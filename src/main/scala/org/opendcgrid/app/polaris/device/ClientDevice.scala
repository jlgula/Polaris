package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import org.opendcgrid.app.polaris.client.definitions.Notification
import org.opendcgrid.app.polaris.client.notification.NotificationHandler
//import akka.http.scaladsl.server.Directives._
import org.opendcgrid.app.polaris.client.notification.NotificationResource

import scala.concurrent.{ExecutionContext, Future}

object ClientDevice {
  def apply(uri: Uri, name: String, serverURI: Uri)(implicit actorSystem: ActorSystem): Future[Http.ServerBinding] = {
    implicit val context: ExecutionContext = actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
    val notificationHandler = new ClientDevice
    val notificationRoutes = NotificationResource.routes(notificationHandler)
    val routes = notificationRoutes // ~ gcRoutes ~ subscriptionRoutes
    Http().newServerAt(uri.authority.host.toString(), uri.authority.port).bindFlow(routes)
  }
}

class ClientDevice extends NotificationHandler {
  override def postNotification(respond: NotificationResource.PostNotificationResponse.type)(body: Notification): Future[NotificationResource.PostNotificationResponse] = ???
}
