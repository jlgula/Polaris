package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply

object XNotifier {
  def apply(notification: NotificationProtocol.Notification, observers: Seq[ActorRef[NotificationProtocol.Command]], replyTo: ActorRef[StatusReply[Done]]): Behavior[StatusReply[Done]] =
    Behaviors.setup[StatusReply[Done]] { context =>
      observers.foreach{ observer => observer ! NotificationProtocol.Notify(notification, replyTo) }
      def active(count: Int): Behavior[StatusReply[Done]] = {
        Behaviors.receiveMessage {
          case StatusReply.Ack =>
            if (count == 0) Behaviors.stopped
            else active(count = 1)
        }
      }
      active(observers.size)
    }
}
