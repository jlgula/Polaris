package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import org.opendcgrid.app.polaris.client.definitions.{Notification, Subscription, Device => DefinedDevice}
import org.opendcgrid.app.polaris.client.device.AddDeviceResponse.{BadRequest, Created}
import org.opendcgrid.app.polaris.client.device.{AddDeviceResponse, DeleteDeviceResponse, DeviceClient, GetPowerGrantedResponse, ListDevicesResponse, PutPowerAcceptedResponse, PutPowerGrantedResponse}
import org.opendcgrid.app.polaris.client.notification.NotificationHandler
import org.opendcgrid.app.polaris.client.subscription.{AddSubscriptionResponse, SubscriptionClient}
import org.opendcgrid.app.polaris.command.CommandError
import org.opendcgrid.app.polaris.device.CapacityManagerDevice.{DeviceSubscription, PowerManagerNotificationReflector}
import java.util.UUID
import scala.concurrent.duration.FiniteDuration
import org.opendcgrid.app.polaris.client.notification.NotificationResource

import scala.concurrent.{ExecutionContext, Future}
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

  case class DeviceSubscription(deviceID: String, subscriptionID: String)

  def apply(deviceURI: Uri, name: String, serverURI: Uri)(implicit actorSystem: ActorSystem): Future[CapacityManagerDevice] = {
    new DeviceBuilder(deviceURI, name, serverURI).build()
  }

  class DeviceBuilder(val deviceURI: Uri, val name: String, val serverURI: Uri)(implicit actorSystem: ActorSystem) {
    implicit val context: ExecutionContext = actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
    private val id = UUID.randomUUID().toString
    private val reflector = new PowerManagerNotificationReflector
    private val deviceClient = DeviceClient(serverURI.toString())
    private val subscriptionClient = SubscriptionClient(serverURI.toString())
    private val deviceProperties = DefinedDevice(id, name)
    private val notificationRoutes = NotificationResource.routes(reflector)
    private val routes = notificationRoutes // ~ gcRoutes ~ subscriptionRoutes

    def build(): Future[CapacityManagerDevice] = {
      for {
        serverBinding <- Http().newServerAt(deviceURI.authority.host.toString(), deviceURI.authority.port).bindFlow(routes)
        deviceID <- addDevice(deviceClient, deviceProperties)
        devices <- subscribeToDevices()
      } yield new CapacityManagerDevice(
        deviceURI,
        serverURI,
        deviceProperties,
        reflector,
        serverBinding,
        deviceClient,
        subscriptionClient,
        devices,
        deviceID)
    }

    private def addDevice(deviceClient: DeviceClient, properties: DefinedDevice): Future[String] = {
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

    private def subscribeToDevices(): Future[Seq[DeviceSubscription]] = {
      for {
        listResponse <- deviceClient.listDevices().value
        deviceIDs <- mapListDevicesResponse(listResponse)
        subscriptions <- Future.sequence(deviceIDs.map(subscribeToDevice))
      } yield deviceIDs.zip(subscriptions).map{ case (deviceID, subscriptionID) => DeviceSubscription(deviceID, subscriptionID)}
    }

    private def mapListDevicesResponse(response: ListDevicesFutureResponse): Future[Seq[String]] = response match {
      case Right(ListDevicesResponse.OK(devices)) => Future.successful(devices.map(_.id))
      case other => throw new IllegalStateException(s"unexpected response: $other")
    }

    private def subscribeToDevice(deviceID: String): Future[String] = {
      val subscriptionPath = Uri.Path(s"/devices/$deviceID")
      val observedURI = serverURI.withPath(subscriptionPath)
      val observerURIWithPath = deviceURI.withPath(Uri.Path(deviceID))
      val subscription = Subscription(observedURI.toString(), observerURIWithPath.toString())
      subscriptionClient.addSubscription(subscription).value.flatMap(mapSubscriptionResponse)
    }

    private def mapSubscriptionResponse(response: AddSubscriptionFutureResponse): Future[String] = response match {
      case Right(AddSubscriptionResponse.Created(id)) => Future.successful(id)
      case Right(AddSubscriptionResponse.BadRequest(message)) => throw new IllegalStateException(s"bad request: $message")
      case other => throw new IllegalStateException(s"unexpected response: $other")
    }
  }
}


class CapacityManagerDevice(
                    val uri: Uri,
                    val serverURI: Uri,
                    val properties: DefinedDevice,
                    val reflector: PowerManagerNotificationReflector,
                    val serverBinding: Http.ServerBinding,
                    val deviceClient: DeviceClient,
                    val subscriptionClient: SubscriptionClient,
                    val devices: Seq[DeviceSubscription],
                    val serverID: String)
                           (implicit actorSystem: ActorSystem) extends Device with NotificationHandler {
  implicit val context: ExecutionContext = actorSystem.dispatcher
  val manager = new CapacityManager(grantMethod, acceptMethod)

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


  reflector.bind(this)

  def terminate(): Future[Http.HttpTerminated] = for {
    deleteResponse <- deviceClient.deleteDevice(serverID).value
    _ <- mapDeleteResponse(deleteResponse)
    termination <- serverBinding.terminate(FiniteDuration(1, "seconds"))
  } yield termination

  private def mapDeleteResponse(response: Either[Either[Throwable, HttpResponse], DeleteDeviceResponse]): Future[Unit] = response match {
    case Right(DeleteDeviceResponse.NoContent) => Future.successful(())
    case other => throw new IllegalStateException(s"unexpected response: $other")
  }

  override def postNotification(respond: NotificationResource.PostNotificationResponse.type)(body: Notification): Future[NotificationResource.PostNotificationResponse] = body match {
    case Notification("/v1/devices", NotificationAction.Post.value, encodedDevice) => handleDeviceAdded(respond, encodedDevice)
    case _ => throw new IllegalStateException(s"Unexpected notification: $body")
  }

  private def handleDeviceAdded(respond: NotificationResource.PostNotificationResponse.type, encodedDevice: String): Future[NotificationResource.PostNotificationResponse] = {
    for {
      device <- parseDevice(encodedDevice)
      _ <- manager.addDevice(device)
    } yield respond.NoContent

  }

  private def parseDevice(value: String): Future[DefinedDevice] = {
    import io.circe._
    import io.circe.parser._
    parse(value) match {
      case Right(jsonValue) =>
        jsonValue.as[DefinedDevice] match {
          case Right(device) => Future.successful(device)
          case Left(error) => Future.failed(CommandError.UnexpectedResponse(error.toString()))
        }
      case Left(ParsingFailure(message, underlying)) => Future.failed(CommandError.UnexpectedResponse(s"Invalid JSON value: $underlying details: $message"))
    }
  }
}
