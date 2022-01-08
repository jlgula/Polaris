package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.StatusReply
import org.opendcgrid.app.polaris.device.DeviceActor.{GetProperties, PropertiesResponse, PutProperties}
import org.opendcgrid.app.polaris.device.NotificationProtocol.Notification
import org.opendcgrid.app.polaris.server.definitions.{Notification => ServerNotification}
import org.opendcgrid.app.polaris.server.notification.{NotificationClient, PostNotificationResponse}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object NotificationAdapterActor {
  sealed trait Command
  final case class WrappedNotification(notification: NotificationProtocol.Notification, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final case object TransmissionSuccessful extends Command
  final case class TransmissionFailed(error: DeviceError) extends Command
  def apply(client: NotificationClient): Behavior[Command] = Behaviors.setup[Command](context => new NotificationAdapterActor(context, client))

  /**
   * Converts the protocol to [[NotificationAdapterActor.Command]] for use in subscription.
   *
   * Just forwards the notification to the NotificationAdapterActor
   * @param actor the [[NotificationAdapterActor]] being wrapped
   * @return a new actor that performs the conversion
   */
  def wrapper(actor: ActorRef[NotificationAdapterActor.Command]): Behavior[NotificationProtocol.Command] = {
    Behaviors.receiveMessage {
      case NotificationProtocol.Notify(notification: Notification, replyTo: ActorRef[StatusReply[Done]]) =>
        actor ! WrappedNotification(notification, replyTo)
        Behaviors.same
    }
  }
}

/**
 * Forwards a notification to an HTTP client and sends a reply when it hears from the client.
 *
 * @param context the [[ActorContext]] for this actor
 * @param client the generated client to send to the target
 */
class NotificationAdapterActor(context: ActorContext[NotificationAdapterActor.Command], client: NotificationClient)  extends AbstractBehavior[NotificationAdapterActor.Command](context) {
  import NotificationAdapterActor._
  implicit private val ec: ExecutionContext = context.executionContext
  var replyTo: Option[ActorRef[StatusReply[Done]]] = None
  /*
  val notificationAdapter: ActorRef[NotificationProtocol.Notify] = context.messageAdapter[NotificationProtocol.Notify] {
    case NotificationProtocol.Notify(notification, replyTo) => WrappedNotification(notification, replyTo)
  }

   */

  override def onMessage(msg: Command): Behavior[Command] = msg match {
    case WrappedNotification(notification, replyTo) =>
      this.replyTo = Some(replyTo)
      val serializedNotification = ServerNotification(notification.observed.toString(), notification.action.toString, notification.value)
      val futureResult = client.postNotification(serializedNotification).value
      context.pipeToSelf(futureResult) {
        // map the Future value to a message, handled by this actor
        case Success(result) => result match {
          case Right(PostNotificationResponse.NoContent) => TransmissionSuccessful
          case Right(PostNotificationResponse.BadRequest(details)) => TransmissionFailed(DeviceError.InvalidNotification(serializedNotification, details))
          case Left(Left(error)) => TransmissionFailed(DeviceError.NotificationFailed(serializedNotification, error.getMessage))
          case Left(Right(httpResponse)) => TransmissionFailed(DeviceError.NotificationFailed(serializedNotification, httpResponse.toString()))
        }
        case Failure(e) =>TransmissionFailed(DeviceError.NotificationFailed(serializedNotification, e.toString))
      }
      Behaviors.same
    case TransmissionSuccessful =>
      this.replyTo.get ! StatusReply.ack()
      Behaviors.same
    case TransmissionFailed(error) =>
      this.replyTo.get ! StatusReply.error(error)
      Behaviors.same
  }
}
