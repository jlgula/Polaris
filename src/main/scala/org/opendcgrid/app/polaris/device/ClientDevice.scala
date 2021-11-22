package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import org.opendcgrid.app.polaris.client.definitions.{Device => DefinedDevice, Notification}
import org.opendcgrid.app.polaris.client.device.{AddDeviceResponse, DeleteDeviceResponse, DeviceClient}
import org.opendcgrid.app.polaris.client.notification.NotificationHandler
import org.opendcgrid.app.polaris.device.ClientDevice.ClientNotificationReflector

import java.util.UUID
import scala.concurrent.duration.FiniteDuration
//import akka.http.scaladsl.server.Directives._
import org.opendcgrid.app.polaris.client.notification.NotificationResource

import scala.concurrent.{ExecutionContext, Future}

object ClientDevice {
  class ClientNotificationReflector extends NotificationHandler {
    private var binding: Option[NotificationHandler] = None

    def bind(handler: NotificationHandler): Unit = binding = Some(handler)

    override def postNotification(respond: NotificationResource.PostNotificationResponse.type)(body: Notification): Future[NotificationResource.PostNotificationResponse] = binding.get.postNotification(respond)(body)
  }

  def apply(uri: Uri, name: String, serverURI: Uri)(implicit actorSystem: ActorSystem): Future[ClientDevice] = {
    implicit val context: ExecutionContext = actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
    val id = UUID.randomUUID().toString
    val reflector = new ClientNotificationReflector
    val deviceClient = DeviceClient(serverURI.toString())
    val deviceProperties = DefinedDevice(id, name)
    val notificationRoutes = NotificationResource.routes(reflector)
    val routes = notificationRoutes // ~ gcRoutes ~ subscriptionRoutes
    for {
      serverBinding <- Http().newServerAt(uri.authority.host.toString(), uri.authority.port).bindFlow(routes)
      addResponse <- deviceClient.addDevice(deviceProperties).value
      serverID <- mapAddResponse(addResponse)
    } yield new ClientDevice(uri, serverURI, deviceProperties, reflector, serverBinding, deviceClient, serverID)

  }

  def mapAddResponse(response: Either[Either[Throwable, HttpResponse], AddDeviceResponse]): Future[String] = response match {
    case Right(AddDeviceResponse.Created(id)) => Future.successful(id)
    case Right(AddDeviceResponse.BadRequest(message)) => throw new IllegalStateException(s"badrequest: $message")
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
                    val serverID: String)
                  (implicit actorSystem: ActorSystem) extends Device with NotificationHandler {
  implicit val context: ExecutionContext = actorSystem.dispatcher
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

  override def postNotification(respond: NotificationResource.PostNotificationResponse.type)(body: Notification): Future[NotificationResource.PostNotificationResponse] = {
    Future.successful(respond.NoContent)
  }
}
