package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import org.opendcgrid.app.polaris.client.definitions.{Notification, Subscription, Device => DeviceProperties}
import org.opendcgrid.app.polaris.client.device.AddDeviceResponse.{BadRequest, Created}
import org.opendcgrid.app.polaris.client.device.{AddDeviceResponse, DeleteDeviceResponse, DeviceClient, GetPowerGrantedResponse, ListDevicesResponse, PutPowerAcceptedResponse, PutPowerGrantedResponse}
import org.opendcgrid.app.polaris.client.notification.NotificationHandler
import org.opendcgrid.app.polaris.client.subscription.{AddSubscriptionResponse, SubscriptionClient}
import org.opendcgrid.app.polaris.command.CommandError
import org.opendcgrid.app.polaris.device.CapacityManagerDevice.{DeviceBuilder, DeviceSubscription, PowerManagerNotificationReflector}

import scala.concurrent.duration.FiniteDuration
import org.opendcgrid.app.polaris.client.notification.NotificationResource

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
object CapacityManagerDevice {
  val powerGrantedPath: Uri.Path = Uri.Path("/self/device/powerGranted")
  val powerAcceptedPath: Uri.Path = Uri.Path("/self/device/powerAccepted")
  val deviceAddedPath: Uri.Path = Uri.Path("/self/devices/deviceAdded")
  val deviceRemovedPath: Uri.Path = Uri.Path("/self/devices/deviceRemoved")
  type AddDeviceFutureResponse = Either[Either[Throwable, HttpResponse], AddDeviceResponse]
  type AddSubscriptionFutureResponse = Either[Either[Throwable, HttpResponse], AddSubscriptionResponse]
  type GetPowerGrantedFutureResponse = Either[Either[Throwable, HttpResponse], GetPowerGrantedResponse]
  type ListDevicesFutureResponse = Either[Either[Throwable, HttpResponse], ListDevicesResponse]

  class PowerManagerNotificationReflector extends NotificationHandler {
    private var binding: Option[NotificationHandler] = None

    def bind(handler: NotificationHandler): Unit = binding = Some(handler)

    override def postNotification(respond: NotificationResource.PostNotificationResponse.type)(body: Notification): Future[NotificationResource.PostNotificationResponse] = binding.get.postNotification(respond)(body)
  }

  case class DeviceSubscription(device: DeviceProperties, subscriptionID: String)

  def apply(deviceURI: Uri, properties: DeviceProperties, serverURI: Uri)(implicit actorSystem: ActorSystem): Future[CapacityManagerDevice] = {
    new DeviceBuilder(deviceURI, properties, serverURI).build()
  }

  class DeviceBuilder(val deviceURI: Uri, properties: DeviceProperties, val serverURI: Uri)(implicit actorSystem: ActorSystem) {
    implicit val context: ExecutionContext = actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
    private val reflector = new PowerManagerNotificationReflector
    private val deviceClient = DeviceClient(serverURI.toString())
    private val subscriptionClient = SubscriptionClient(serverURI.toString())
    private val notificationRoutes = NotificationResource.routes(reflector)
    private val routes = notificationRoutes // ~ gcRoutes ~ subscriptionRoutes

    def build(): Future[CapacityManagerDevice] = {
      for {
        serverBinding <- Http().newServerAt(deviceURI.authority.host.toString(), deviceURI.authority.port).bindFlow(routes)
        devices <- listDevices()  // List before we add the CapacityManager so we don't pick that up too.
        deviceID <- addDevice(deviceClient, properties)
        tableSubscription <- subscribe(Uri.Path(GCDevice.devicesPath), Uri.Path(GCDevice.devicesPath))
        deviceSubscriptions <- subscribeToDevices(devices)
        cmDevice = new CapacityManagerDevice(
          deviceURI,
          serverURI,
          properties,
          reflector,
          serverBinding,
          deviceClient,
          subscriptionClient,
          deviceSubscriptions,
          tableSubscription,
          this,
          deviceID)
        _ <- cmDevice.assignPower()
      } yield cmDevice
    }

    private def addDevice(deviceClient: DeviceClient, properties: DeviceProperties): Future[String] = {
      for {
        addResponse <- deviceClient.addDevice(properties).value
        deviceID <- mapAddResponse(addResponse) // The ID of the client on the GC
      } yield deviceID
    }

    private def mapAddResponse(response: AddDeviceFutureResponse): Future[String] = response match {
      case Right(Created(id)) => Future.successful(id)
      case Right(BadRequest(message)) => throw new IllegalStateException(s"badrequest: $message")
      case other => throw new IllegalStateException(s"unexpected response: $other")
    }

    private def listDevices(): Future[Seq[DeviceProperties]] = deviceClient.listDevices().value.flatMap {
      case Right(ListDevicesResponse.OK(value)) => Future.successful(value)
      case other => Future.failed(CommandError.UnexpectedResponse(other.toString))
    }

    private def subscribe(observedPath: Uri.Path, observerPath: Uri.Path): Future[String] = {
      val observedURI = serverURI.withPath(observedPath)
      val observerURIWithPath = deviceURI.withPath(observerPath)
      val subscription = Subscription(observedURI.toString(), observerURIWithPath.toString())
      subscriptionClient.addSubscription(subscription).value.flatMap {
        case Right(AddSubscriptionResponse.Created(id)) => Future.successful(id)
        case other => Future.failed(CommandError.UnexpectedResponse(other.toString))
      }
    }

    private def subscribeToDevices(devices: Seq[DeviceProperties]): Future[Seq[DeviceSubscription]] = {
      for {
        subscriptions <- Future.sequence(devices.map(subscribeToDevice))
      } yield subscriptions
    }

    // Subscribe to the value of a device containing PowerRequested and PowerOffered.
    // This will update the power distribution if these change for any device.
    // The subscription is also watching deletes in case the device goes away.
    def subscribeToDevice(device: DeviceProperties): Future[DeviceSubscription] = {
      val path = Uri.Path(s"${GCDevice.devicesPath}/${device.id}")
      subscribe(path, path).map(subscriptionID => DeviceSubscription(device, subscriptionID))
    }
  }
}


class CapacityManagerDevice(
                    val uri: Uri,
                    val serverURI: Uri,
                    val properties: DeviceProperties,
                    val reflector: PowerManagerNotificationReflector,
                    val serverBinding: Http.ServerBinding,
                    val deviceClient: DeviceClient,
                    val subscriptionClient: SubscriptionClient,
                    val initialSubscriptions: Seq[DeviceSubscription],
                    val tableSubscription: String,
                    val deviceBuilder: DeviceBuilder,
                    val serverID: String)
                           (implicit actorSystem: ActorSystem) extends Device with NotificationHandler {
  implicit val context: ExecutionContext = actorSystem.dispatcher
  private val manager = new CapacityManager(initialSubscriptions.map(_.device), grantMethod, acceptMethod)
  private val deviceSubscriptions = mutable.Map[DeviceID, DeviceSubscription](initialSubscriptions.map(subscription => (subscription.device.id, subscription)): _*)
  reflector.bind(this)


  private def grantMethod(id: DeviceID, value: PowerValue): Future[Unit] = {
    deviceClient.putPowerGranted(id, value).value.map {
      case Right(PutPowerGrantedResponse.NoContent) => ()
      case other => CommandError.UnexpectedResponse(other.toString)
    }
  }

  private def acceptMethod(id: DeviceID, value: PowerValue): Future[Unit] = {
    deviceClient.putPowerAccepted(id, value).value.map {
      case Right(PutPowerAcceptedResponse.NoContent) => ()
      case other => CommandError.UnexpectedResponse(other.toString)
    }
  }

  /**
   * Assigns power to all devices.
   *
   * This should be invoked once to initialize the manager.
   * After that, activity will follow the notifications of power changes.
   *
   * @return  a sequence of all devices that had power assignments wrapped in a Future
   */
  def assignPower(): Future[Seq[PowerAssignment]] = manager.assignPower()

  def terminate(): Future[Http.HttpTerminated] = for {  // TODO: stop subscriptions?
    deleteResponse <- deviceClient.deleteDevice(serverID).value
    _ <- mapDeleteResponse(deleteResponse)
    termination <- serverBinding.terminate(FiniteDuration(1, "seconds"))
  } yield termination

  private def mapDeleteResponse(response: Either[Either[Throwable, HttpResponse], DeleteDeviceResponse]): Future[Unit] = response match {
    case Right(DeleteDeviceResponse.NoContent) => Future.successful(())
    case other => throw new IllegalStateException(s"unexpected response: $other")
  }

  override def postNotification(respond: NotificationResource.PostNotificationResponse.type)(body: Notification): Future[NotificationResource.PostNotificationResponse] = body match {
    case Notification(path, NotificationAction.Post.value, encodedDevice) if matchPath(path, GCDevice.devicesPath) => handleDeviceAdded(respond, encodedDevice)
    case Notification(path, NotificationAction.Delete.value, encodedDevice)  if matchPath(path, GCDevice.devicesPath) => handleDeviceRemoved(respond, encodedDevice)
    case Notification(path, NotificationAction.Put.value, encodedDevice) if matchPath(path, GCDevice.devicesPath) => handleDeviceUpdate(respond, encodedDevice)
    case _ => throw new IllegalStateException(s"Unexpected notification: $body")
  }

  private def matchPath(observed: String, expected: String): Boolean = {
    val expectedPath = Uri.Path(expected)
    Try(Uri(observed)) match {
      case Success(uri) => uri.path.startsWith(expectedPath)
      case Failure(error) => throw new IllegalStateException(s"unexpected observed value: $error")
    }
  }

  private def handleDeviceAdded(respond: NotificationResource.PostNotificationResponse.type, encodedDevice: String): Future[NotificationResource.PostNotificationResponse] = {
    for {
      device <- parseDevice(encodedDevice)
      subscription <- deviceBuilder.subscribeToDevice(device)
      _ <- addDeviceAndSubscription(device, subscription)
    } yield respond.NoContent
  }

  private def addDeviceAndSubscription(device: DeviceProperties, subscription: DeviceSubscription): Future[Seq[PowerAssignment]] = {
    manager.addDevice(device).andThen{ case Success(_) => deviceSubscriptions.put(device.id, subscription)}
  }

  private def handleDeviceRemoved(respond: NotificationResource.PostNotificationResponse.type, encodedDevice: String): Future[NotificationResource.PostNotificationResponse] = {
    for {
      device <- parseDevice(encodedDevice)
      _ <- manager.removeDevice(device.id)
    } yield respond.NoContent
  }

  private def handleDeviceUpdate(respond: NotificationResource.PostNotificationResponse.type, encodedDevice: String): Future[NotificationResource.PostNotificationResponse] = {
    for {
      device <- parseDevice(encodedDevice)
      _ <- manager.updateDevice(device)
    } yield respond.NoContent
  }

  private def parseDevice(value: String): Future[DeviceProperties] = {
    import io.circe._
    import io.circe.parser._
    parse(value) match {
      case Right(jsonValue) =>
        jsonValue.as[DeviceProperties] match {
          case Right(device) => Future.successful(device)
          case Left(error) => Future.failed(CommandError.UnexpectedResponse(error.toString()))
        }
      case Left(ParsingFailure(message, underlying)) => Future.failed(CommandError.UnexpectedResponse(s"Invalid JSON value: $underlying details: $message"))
    }
  }
}
