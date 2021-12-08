package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import org.opendcgrid.app.polaris.client.definitions.{Notification, Subscription, Device => DeviceProperties}
import org.opendcgrid.app.polaris.client.device.AddDeviceResponse._
import org.opendcgrid.app.polaris.client.device.{AddDeviceResponse, DeleteDeviceResponse, DeviceClient, GetPowerGrantedResponse}
import org.opendcgrid.app.polaris.client.notification.NotificationHandler
import org.opendcgrid.app.polaris.client.subscription.{AddSubscriptionResponse, SubscriptionClient}
import org.opendcgrid.app.polaris.device.ClientDevice.{ClientNotificationReflector, ClientSubscriptions}

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}
//import akka.http.scaladsl.server.Directives._
import io.circe._
import io.circe.parser._
import org.opendcgrid.app.polaris.client.notification.NotificationResource

import scala.concurrent.{ExecutionContext, Future}

object ClientDevice {
  val powerGrantedSegment = "powerGranted"
  val powerGrantedPath = s"/self/device/$powerGrantedSegment"
  val powerAcceptedSegment = "powerAccepted"
  val powerAcceptedPath = s"/self/device/$powerAcceptedSegment"
  type AddDeviceFutureResponse = Either[Either[Throwable, HttpResponse], AddDeviceResponse]
  type AddSubscriptionFutureResponse = Either[Either[Throwable, HttpResponse], AddSubscriptionResponse]
  type GetPowerGrantedFutureResponse = Either[Either[Throwable, HttpResponse], GetPowerGrantedResponse]

  class ClientNotificationReflector extends NotificationHandler {
    private var binding: Option[NotificationHandler] = None

    def bind(handler: NotificationHandler): Unit = binding = Some(handler)

    override def postNotification(respond: NotificationResource.PostNotificationResponse.type)(body: Notification): Future[NotificationResource.PostNotificationResponse] = binding.get.postNotification(respond)(body)
  }

  /**
   * Container for the subscription IDs used by the client
   *
   * @param powerGranted  ID of powerGranted
   * @param powerAccepted ID of powerAccepted
   */
  case class ClientSubscriptions(powerGranted: String, powerAccepted: String)

  def apply(clientURI: Uri, properties: DeviceProperties, serverURI: Uri)(implicit actorSystem: ActorSystem): Future[ClientDevice] = {
    implicit val ec: ExecutionContext = actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
    val reflector = new ClientNotificationReflector
    val deviceClient = DeviceClient(serverURI.toString())
    val subscriptionClient = SubscriptionClient(serverURI.toString())
    val notificationRoutes = NotificationResource.routes(reflector)
    val routes = notificationRoutes // ~ gcRoutes ~ subscriptionRoutes
    for {
      serverBinding <- Http().newServerAt(clientURI.authority.host.toString(), clientURI.authority.port).bindFlow(routes)
      addResponse <- deviceClient.addDevice(properties).value
      deviceID <- mapAddResponse(addResponse) // The ID of the client on the GC
      powerGrantedID <- subscribeToPowerGranted(clientURI, subscriptionClient, serverURI, deviceID)
      powerAcceptedID <- subscribeToPowerAccepted(clientURI, subscriptionClient, serverURI, deviceID)
    } yield new ClientDevice(
      clientURI,
      serverURI,
      properties,
      reflector,
      serverBinding,
      deviceClient,
      subscriptionClient,
      ClientSubscriptions(powerGrantedID, powerAcceptedID),
      deviceID)

  }

  private def mapAddResponse(response: AddDeviceFutureResponse): Future[String] = response match {
    case Right(Created(id)) => Future.successful(id)
    case Right(BadRequest(message)) => throw new IllegalStateException(s"badrequest: $message")
    case other => throw new IllegalStateException(s"unexpected response: $other")
  }

  private def subscribeToPowerGranted(observerURI: Uri, subscriptionClient: SubscriptionClient, gcURI: Uri, deviceID: String)(implicit ec: ExecutionContext): Future[String] = {
    val subscriptionPath = Uri.Path(s"${GCDevice.devicesPath}/$deviceID/powerGranted")
    val observedURI = gcURI.withPath(subscriptionPath)
    val observerURIWithPath = observerURI.withPath(Uri.Path(ClientDevice.powerGrantedPath))
    subscriptionClient.addSubscription(Subscription(observedURI.toString(), observerURIWithPath.toString())).value.flatMap {
      case Right(AddSubscriptionResponse.Created(id)) => Future.successful(id)
      case other => Future.failed(DeviceError.UnexpectedResponse(other.toString))
    }
  }

  private def subscribeToPowerAccepted(observerURI: Uri, subscriptionClient: SubscriptionClient, gcURI: Uri, deviceID: String)(implicit ec: ExecutionContext): Future[String] = {
    val subscriptionPath = Uri.Path(s"${GCDevice.devicesPath}/$deviceID/powerAccepted")
    val observedURI = gcURI.withPath(subscriptionPath)
    val observerURIWithPath = observerURI.withPath(Uri.Path(ClientDevice.powerAcceptedPath))
    subscriptionClient.addSubscription(Subscription(observedURI.toString(), observerURIWithPath.toString())).value.flatMap {
      case Right(AddSubscriptionResponse.Created(id)) => Future.successful(id)
      case other => Future.failed(DeviceError.UnexpectedResponse(other.toString))
    }
  }
}

class ClientDevice(
                    val uri: Uri,
                    val serverURI: Uri,
                    val properties: DeviceProperties,
                    val reflector: ClientNotificationReflector,
                    val serverBinding: Http.ServerBinding,
                    val deviceClient: DeviceClient,
                    val subscriptionClient: SubscriptionClient,
                    val subscriptions: ClientSubscriptions,
                    val serverID: String)
                  (implicit actorSystem: ActorSystem) extends Device with NotificationHandler {
  implicit val context: ExecutionContext = actorSystem.dispatcher
  var powerGranted: PowerValue = 0  // TODO: protect these in actor
  var powerAccepted: PowerValue = 0

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
    case Notification(path, NotificationAction.Put.value, value) if matchPath(path, ClientDevice.powerGrantedSegment) => updatePowerGranted(respond, value)
    case Notification(path, NotificationAction.Put.value, value) if matchPath(path, ClientDevice.powerAcceptedSegment) => updatePowerAccepted(respond, value)
    case _ => throw new IllegalStateException(s"Unexpected notification: $body")
  }

  private def matchPath(observedPath: String, expected: String): Boolean = {
    val expectedPath = serverURI.withPath(Uri.Path(s"${GCDevice.devicesPath}/${properties.id}/$expected")).toString()
    observedPath == expectedPath
  }


  private def updatePowerGranted(respond: NotificationResource.PostNotificationResponse.type, valueAsString: String): Future[NotificationResource.PostNotificationResponse] = {
    //import io.circe.syntax._
    parse(valueAsString) match {
      case Right(jsonValue) =>
        jsonValue.as[PowerValue] match {
          case Right(value) => putPowerGranted(value) match {
            case Right(_) => Future.successful(respond.NoContent)
            case Left(error) => Future.successful(respond.BadRequest(error.getMessage))
          }
          case Left(error) => Future.successful(respond.BadRequest(error.getMessage))
        }
      case Left(ParsingFailure(message, underlying)) => Future.successful(respond.BadRequest(s"Invalid JSON value: $underlying details: $message"))
    }
  }

  private def updatePowerAccepted(respond: NotificationResource.PostNotificationResponse.type, valueAsString: String): Future[NotificationResource.PostNotificationResponse] = {
    //import io.circe.syntax._
    parse(valueAsString) match {
      case Right(jsonValue) =>
        jsonValue.as[PowerValue] match {
          case Right(value) => putPowerAccepted(value) match {
            case Right(_) => Future.successful(respond.NoContent)
            case Left(error) => Future.successful(respond.BadRequest(error.getMessage))
          }
          case Left(error) => Future.successful(respond.BadRequest(error.getMessage))
        }
      case Left(ParsingFailure(message, underlying)) => Future.successful(respond.BadRequest(s"Invalid JSON value: $underlying details: $message"))
    }
  }


  private def putPowerGranted(value: PowerValue): Either[DeviceError, Unit] = {
    println(s"device: ${properties.name}@$uri power granted: $value")
    this.powerGranted = value
    Right(())
  }

  private def putPowerAccepted(value: PowerValue): Either[DeviceError, Unit] = {
    println(s"device: ${properties.name}@$uri power accepted: $value")
    this.powerAccepted = value
    Right(())
  }

}
