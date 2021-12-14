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
  val powerPricePath: Uri.Path = Uri.Path("/self/powerPrice")   // observer path for observing the grid price

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
  case class ClientSubscriptions(powerGranted: String, powerAccepted: String, gridPrice: String)

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
      gridPriceID <- subscribe(serverURI.withPath(GCDevice.powerPricePath), clientURI.withPath(ClientDevice.powerPricePath), subscriptionClient)
    } yield new ClientDevice(
      clientURI,
      serverURI,
      properties,
      reflector,
      serverBinding,
      deviceClient,
      subscriptionClient,
      ClientSubscriptions(powerGrantedID, powerAcceptedID, gridPriceID),
      deviceID)

  }

  private def mapAddResponse(response: AddDeviceFutureResponse): Future[String] = response match {
    case Right(Created(id)) => Future.successful(id)
    case Right(BadRequest(message)) => throw new IllegalStateException(s"badrequest: $message")
    case other => throw new IllegalStateException(s"unexpected response: $other")
  }

  private def subscribe(observedURI: Uri, observerURI: Uri, subscriptionClient: SubscriptionClient)(implicit ec: ExecutionContext): Future[String] = {
    subscriptionClient.addSubscription(Subscription(observedURI.toString(), observerURI.toString())).value.flatMap {
      case Right(AddSubscriptionResponse.Created(id)) => Future.successful(id)
      case other => Future.failed(DeviceError.UnexpectedResponse(other.toString))
    }
  }

  private def subscribeToPowerGranted(observerURI: Uri, subscriptionClient: SubscriptionClient, gcURI: Uri, deviceID: DeviceID)(implicit ec: ExecutionContext): Future[String] = {
    val observedURI = GCDevice.makeDeviceSubscriptionURI(gcURI, deviceID, GCDevice.powerGrantedProperty)
    val observerURIWithPath = observerURI.withPath(Uri.Path(ClientDevice.powerGrantedPath))
    subscribe(observedURI, observerURIWithPath, subscriptionClient)
  }

  private def subscribeToPowerAccepted(observerURI: Uri, subscriptionClient: SubscriptionClient, gcURI: Uri, deviceID: DeviceID)(implicit ec: ExecutionContext): Future[String] = {
    val observedURI = GCDevice.makeDeviceSubscriptionURI(gcURI, deviceID, GCDevice.powerAcceptedProperty)
    val observerURIWithPath = observerURI.withPath(Uri.Path(ClientDevice.powerAcceptedPath))
    subscribe(observedURI, observerURIWithPath, subscriptionClient)
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
  var powerGranted: PowerValue = PowerValue(0)  // TODO: protect these in actor
  var powerAccepted: PowerValue = PowerValue(0)
  var powerPrice: Price = Price(0)

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
    case Notification(path, NotificationAction.Put.value, value) if matchPath(path, ClientDevice.powerGrantedSegment) => update(respond, value, _.as[PowerValue], putPowerGranted)
    case Notification(path, NotificationAction.Put.value, value) if matchPath(path, ClientDevice.powerAcceptedSegment) => update(respond, value, _.as[PowerValue], putPowerAccepted)
    case Notification(path, NotificationAction.Put.value, value) if path == serverURI.withPath(GCDevice.powerPricePath).toString() => update(respond, value, _.as[Price], putGridPrice)
    case _ => throw new IllegalStateException(s"Unexpected notification: $body")
  }

  private def matchPath(observedPath: String, expected: String): Boolean = {
    val expectedPath = serverURI.withPath(Uri.Path(s"${GCDevice.devicesPath}/${properties.id}/$expected")).toString()
    observedPath == expectedPath
  }

  private def update[T](respond: NotificationResource.PostNotificationResponse.type, valueAsString: String, decoder: Json => Decoder.Result[T], handler: T => Option[DeviceError]) : Future[NotificationResource.PostNotificationResponse] = {
    parse(valueAsString) match {
      case Right(jsonValue) =>
        decoder(jsonValue) match {
          case Right(value) => handler(value) match {
            case None => Future.successful(respond.NoContent)
            case Some(error) => Future.successful(respond.BadRequest(error.getMessage))
          }
          case Left(error) => Future.successful(respond.BadRequest(error.getMessage))
        }
      case Left(ParsingFailure(message, underlying)) => Future.successful(respond.BadRequest(s"Invalid JSON value: $underlying details: $message"))
    }
  }

  private def putPowerGranted(value: PowerValue): Option[DeviceError] = {
    //println(s"device: ${properties.name}@$uri power granted: $value")
    this.powerGranted = value
    None
  }

  private def putPowerAccepted(value: PowerValue): Option[DeviceError] = {
    //println(s"device: ${properties.name}@$uri power accepted: $value")
    this.powerAccepted = value
    None
  }

  private def putGridPrice(value: Price): Option[DeviceError] = {
    //println(s"device: ${properties.name}@$uri grid price: $value")
    this.powerPrice = value
    None
  }


}
