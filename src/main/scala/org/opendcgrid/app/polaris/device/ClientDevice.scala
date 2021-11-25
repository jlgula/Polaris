package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import org.opendcgrid.app.polaris.client.definitions.{Notification, Subscription, Device => DefinedDevice}
import org.opendcgrid.app.polaris.client.device.AddDeviceResponse._
import org.opendcgrid.app.polaris.client.device.GetPowerGrantedResponse.OK
import org.opendcgrid.app.polaris.client.device.{AddDeviceResponse, DeleteDeviceResponse, DeviceClient, GetPowerGrantedResponse}
import org.opendcgrid.app.polaris.client.notification.NotificationHandler
import org.opendcgrid.app.polaris.client.subscription.{AddSubscriptionResponse, SubscriptionClient}
import org.opendcgrid.app.polaris.device.ClientDevice.ClientNotificationReflector

import java.util.UUID
import scala.concurrent.duration.FiniteDuration
//import akka.http.scaladsl.server.Directives._
import org.opendcgrid.app.polaris.client.notification.NotificationResource

import scala.concurrent.{ExecutionContext, Future}

object ClientDevice {
  val powerGrantedPath: Uri.Path = Uri.Path("/self/device/powerGranted")
  val powerAcceptedPath: Uri.Path = Uri.Path("/self/device/powerAccepted")
  type AddDeviceFutureResponse = Either[Either[Throwable, HttpResponse], AddDeviceResponse]
  type AddSubscriptionFutureResponse = Either[Either[Throwable, HttpResponse], AddSubscriptionResponse]
  type GetPowerGrantedFutureResponse = Either[Either[Throwable, HttpResponse], GetPowerGrantedResponse]

  class ClientNotificationReflector extends NotificationHandler {
    private var binding: Option[NotificationHandler] = None

    def bind(handler: NotificationHandler): Unit = binding = Some(handler)

    override def postNotification(respond: NotificationResource.PostNotificationResponse.type)(body: Notification): Future[NotificationResource.PostNotificationResponse] = binding.get.postNotification(respond)(body)
  }

  def apply(clientURI: Uri, name: String, serverURI: Uri)(implicit actorSystem: ActorSystem): Future[ClientDevice] = {
    implicit val context: ExecutionContext = actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
    val id = UUID.randomUUID().toString
    val reflector = new ClientNotificationReflector
    val deviceClient = DeviceClient(serverURI.toString())
    val subscriptionClient = SubscriptionClient(serverURI.toString())
    val deviceProperties = DefinedDevice(id, name)
    val notificationRoutes = NotificationResource.routes(reflector)
    val routes = notificationRoutes // ~ gcRoutes ~ subscriptionRoutes
    for {
      serverBinding <- Http().newServerAt(clientURI.authority.host.toString(), clientURI.authority.port).bindFlow(routes)
      addResponse <- deviceClient.addDevice(deviceProperties).value
      deviceID <- mapAddResponse(addResponse) // The ID of the client on the GC
      subscribeResponse <- subscribeToPowerGranted(clientURI, subscriptionClient, serverURI, deviceID)
      powerGrantedSubscriptionID <- mapSubscriptionResponse(subscribeResponse)
      powerGrantedResponse <- deviceClient.getPowerGranted(deviceID).value
      initialPowerGranted <- mapPowerGrantedResponse(powerGrantedResponse)
    } yield new ClientDevice(
      clientURI,
      serverURI,
      deviceProperties,
      reflector,
      serverBinding,
      deviceClient,
      subscriptionClient,
      powerGrantedSubscriptionID,
      initialPowerGranted,
      deviceID)

  }

  private def mapAddResponse(response: AddDeviceFutureResponse): Future[String] = response match {
    case Right(Created(id)) => Future.successful(id)
    case Right(BadRequest(message)) => throw new IllegalStateException(s"badrequest: $message")
    case other => throw new IllegalStateException(s"unexpected response: $other")
  }

  private def subscribeToPowerGranted(observerURI: Uri, subscriptionClient: SubscriptionClient, gcURI: Uri, deviceID: String): Future[AddSubscriptionFutureResponse] = {
    val subscriptionPath = Uri.Path(s"/devices/$deviceID/powerGranted")
    val observedURI = gcURI.withPath(subscriptionPath)
    val observerURIWithPath = observerURI.withPath(ClientDevice.powerGrantedPath)
    subscriptionClient.addSubscription(Subscription(observedURI.toString(), observerURIWithPath.toString())).value
  }

  private def mapSubscriptionResponse(response: AddSubscriptionFutureResponse): Future[String] = response match {
    case Right(AddSubscriptionResponse.Created(id)) => Future.successful(id)
    case Right(AddSubscriptionResponse.BadRequest(message)) => throw new IllegalStateException(s"bad request: $message")
    case other => throw new IllegalStateException(s"unexpected response: $other")
  }

  /**
   * Converts the response from a get power granted operation to the actual power granted value.
   *
   * @param response the response from the client operation
   * @return the value wrapped in a [[Future]] or an error
   */
  private def mapPowerGrantedResponse(response: GetPowerGrantedFutureResponse): Future[BigDecimal] = response match {
    case Right(OK(value)) => Future.successful(value)
    case other => throw new IllegalStateException(s"unexpected response: $other")
  }
}

class ClientDevice(
                    val uri: Uri,
                    val serverURI: Uri,
                    val properties: DefinedDevice,
                    val reflector: ClientNotificationReflector,
                    val serverBinding: Http.ServerBinding,
                    val deviceClient: DeviceClient,
                    val subscriptionClient: SubscriptionClient,
                    val powerGrantedSubscriptionID: String,
                    val initialPowerGranted: BigDecimal,
                    val serverID: String)
                  (implicit actorSystem: ActorSystem) extends Device with NotificationHandler {
  implicit val context: ExecutionContext = actorSystem.dispatcher
  private var powerGranted: BigDecimal = 0
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
    case Notification(_, _, value) => updatePowerGranted(respond, value)
    case _ => throw new IllegalStateException(s"Unexpected notification: $body")
  }

  private def updatePowerGranted(respond: NotificationResource.PostNotificationResponse.type, valueAsString: String): Future[NotificationResource.PostNotificationResponse] = {
    //import io.circe.syntax._
    import io.circe._
    import io.circe.parser._

    parse(valueAsString) match {
      case Right(jsonValue) =>
        jsonValue.as[BigDecimal] match {
          case Right(value) => putPowerGranted(value) match {
            case Right(_) => Future.successful(respond.NoContent)
            case Left(error) => Future.successful(respond.BadRequest(error.getMessage))
          }
          case Left(error) => Future.successful(respond.BadRequest(error.getMessage))
        }
      case Left(ParsingFailure(message, underlying)) => Future.successful(respond.BadRequest(s"Invalid JSON value: $underlying details: $message"))
    }
  }

  def putPowerGranted(value: BigDecimal): Either[DeviceError, Unit] = {
    //println(s"device: $uri power granted: $value")
    this.powerGranted = value
    Right(())
  }
}
