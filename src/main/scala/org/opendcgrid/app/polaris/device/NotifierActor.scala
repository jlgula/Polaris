package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply

import scala.concurrent.duration.FiniteDuration

object NotifierActor {
  sealed trait Command
  private case object RequestTimeout extends Command
  private case class WrappedReply(reply: StatusReply[Done]) extends Command
  val defaultTimeout: FiniteDuration = FiniteDuration(3, "seconds")

  def apply(
             notification: NotificationProtocol.Notification,
             observers: Seq[ActorRef[NotificationProtocol.Command]],
             replyTo: ActorRef[StatusReply[Done]],
             timeout: FiniteDuration = defaultTimeout): Behavior[Command] = {
    if (observers.isEmpty) {
      replyTo ! StatusReply.Ack
      Behaviors.stopped
    } else {
      Behaviors.setup[Command] { context =>
        Behaviors.withTimers { timers =>
          val replyAdapter = context.messageAdapter(WrappedReply)

          // Send the notification to each observer and start a timer to make sure everyone replies.
          // TODO: return the actorRef in the response so we can keep track of who failed.
          observers.foreach { observer => observer ! NotificationProtocol.Notify(notification, replyAdapter) }
          timers.startSingleTimer(RequestTimeout, timeout)

          def active(count: Int): Behavior[Command] = {
            Behaviors.receiveMessage {
              case WrappedReply(_) =>
                if (count == 1) {   // Last one?
                  replyTo ! StatusReply.Ack
                  Behaviors.stopped
                }
                else active(count - 1)
              case RequestTimeout =>
                replyTo ! StatusReply.error("Notifier: request timeout")
                Behaviors.same
            }
          }

          active(observers.size)
        }
      }
    }
  }
}
