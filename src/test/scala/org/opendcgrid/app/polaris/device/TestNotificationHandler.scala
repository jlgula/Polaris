package org.opendcgrid.app.polaris.device

import org.opendcgrid.app.polaris.client.definitions.{Notification => ClientNotification}
import org.opendcgrid.app.polaris.client.notification.{NotificationHandler, NotificationResource}

import java.util.concurrent.ConcurrentLinkedQueue
import scala.concurrent.Future

class TestNotificationHandler extends NotificationHandler() {
  import io.circe.parser.decode
  val observations = new ConcurrentLinkedQueue[BigDecimal]()
  override def postNotification(respond: NotificationResource.PostNotificationResponse.type)(body: ClientNotification): Future[NotificationResource.PostNotificationResponse] = {
    decode[BigDecimal](body.value) match {
      case Right(bigNum) => observations.add(bigNum)
      case Left(error) => throw new IllegalStateException(s"postNotification - unexpected result: $error")
    }
    println(s"TestNotificationHandler: $this, $body, $observations")
    Future.successful(respond.NoContent)
  }
}
