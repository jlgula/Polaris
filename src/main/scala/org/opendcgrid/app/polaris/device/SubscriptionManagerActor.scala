package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.Uri
import akka.pattern.StatusReply

object SubscriptionManagerActor {
  //final case class Notification(observed: Uri.Path, action: NotificationAction, value: String)
  final case class Subscription(observed: Uri.Path, action: NotificationAction, observer: ActorRef[NotificationProtocol.Command])

  sealed trait Command // extends NotifierActor.Command
  final case class AddSubscription(subscription: Subscription, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final case class RemoveSubscription(subscription: Subscription, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final case class ListSubscriptions(replyTo: ActorRef[SubscriptionResponse]) extends Command
  final case class Notify(notification: NotificationProtocol.Notification, replyTo: ActorRef[StatusReply[Done]]) extends Command

  final case class SubscriptionResponse(subscriptions: Set[Subscription])

  def apply(subscriptions: Set[Subscription]): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case AddSubscription(subscription, replyTo) =>
          replyTo ! StatusReply.Ack
          this (subscriptions + subscription)
        case RemoveSubscription(subscription, replyTo) =>
          replyTo ! StatusReply.Ack
          this (subscriptions - subscription)
        case ListSubscriptions(replyTo) =>
          replyTo ! SubscriptionResponse(subscriptions)
          this (subscriptions)
        case Notify(notification, replyTo) =>
          val observers = subscriptions.filter(s => s.observed == notification.observed).map(_.observer)
          context.spawn(Notifier(notification, observers.toSeq, replyTo), "notifier")
          //observers.foreach { observer => observer ! NotificationProtocol.Notify(notification, replyTo) }
          //replyTo ! StatusReply.Ack
          this (subscriptions)
      }
    }

  object Notifier {
    def apply(notification: NotificationProtocol.Notification, observers: Seq[ActorRef[NotificationProtocol.Command]], replyTo: ActorRef[StatusReply[Done]]): Behavior[StatusReply[Done]] =
      Behaviors.setup[StatusReply[Done]] { context =>

        def handle(observers: Seq[ActorRef[NotificationProtocol.Command]]): Behavior[StatusReply[Done]] = {
          Behaviors.receiveMessage[StatusReply[Done]] {
            case StatusReply.Ack =>
              notifyNext(observers)
            case StatusReply.Error(message) =>
              context.log.error(message.getMessage)
              Behaviors.stopped
          }

          def notifyNext(observers: Seq[ActorRef[NotificationProtocol.Command]]): Behavior[StatusReply[Done]] = {
            if (observers.isEmpty) {
              replyTo ! StatusReply.Ack
              Behaviors.stopped
            }
            else {
              observers.head ! NotificationProtocol.Notify(notification, replyTo)
              handle(observers.tail)
            }
          }

          notifyNext(observers)
        }

        handle(observers)
      }
  }
}


object NotificationProtocol {
  case class Notification(observed: Uri.Path, action: NotificationAction, value: String)
  trait Command
  final case class Notify(notification: Notification, replyTo: ActorRef[StatusReply[Done]]) extends Command
}

/*
object NotifierActor {
  def apply(): Behaviors.Receive[NotificationProtocol.Command] =
    Behaviors.receiveMessage {
     case NotificationProtocol.Notify(_) =>
        this()
    }

}

 */
