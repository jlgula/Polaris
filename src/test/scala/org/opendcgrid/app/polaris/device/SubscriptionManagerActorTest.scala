package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.Uri
import akka.pattern.StatusReply
import org.opendcgrid.app.polaris.device.NotificationProtocol.{Notification, Notify}
import org.opendcgrid.app.polaris.device.SubscriptionManagerActor.Subscription
import org.scalatest.funsuite.AnyFunSuiteLike

class SubscriptionManagerActorTest extends ScalaTestWithActorTestKit with AnyFunSuiteLike {
  test("add/remove/list") {

    val path = Uri.Path("/")
    //val value = "testValue"
    val manager = spawn(SubscriptionManagerActor(Set[Subscription]()))
    //val notifier = spawn(Notifier(manager))
    val observer = createTestProbe[NotificationProtocol.Command]()

    val subscription = Subscription(path, NotificationAction.Put, observer.ref)

    val probe = createTestProbe[StatusReply[Done]]()
    manager ! SubscriptionManagerActor.AddSubscription(subscription, probe.ref)
    val response1 = probe.receiveMessage()
    assert(response1.isSuccess)


    val probe2 = createTestProbe[SubscriptionManagerActor.SubscriptionResponse]()
    manager ! SubscriptionManagerActor.ListSubscriptions(probe2.ref)
    val response2 = probe2.receiveMessage()
    assert(response2.subscriptions.contains(subscription))

    manager ! SubscriptionManagerActor.RemoveSubscription(subscription, probe.ref)
    val response3 = probe.receiveMessage()
    assert(response3.isSuccess)

    manager ! SubscriptionManagerActor.ListSubscriptions(probe2.ref)
    val response4 = probe2.receiveMessage()
    assert(!response4.subscriptions.contains(subscription))
  }

  test("notifier") {
    val probe = createTestProbe[StatusReply[Done]]()
    val path = Uri.Path("/")
    val action = NotificationAction.Put
    val value = "test"
    val notification = NotificationProtocol.Notification(path, action, value)
    val observer = spawn(Observer2())
    spawn(SubscriptionManagerActor.Notifier(notification, Seq(observer.ref), probe.ref))
    val response = probe.receiveMessage()
    assert(response.isSuccess)
  }

  test("add/notify") {
    val path = Uri.Path("/")
    val value = "testValue"
    val action = NotificationAction.Put
    val manager = spawn(SubscriptionManagerActor(Set[Subscription]()))
    val observer = spawn(Observer(manager))

    val probe = createTestProbe[StatusReply[Done]]()
    observer ! Observer.InitializeSubscriptions(probe.ref)
    val response = probe.receiveMessage()
    assert(response.isSuccess)

    manager ! SubscriptionManagerActor.Notify(Notification(path, action, value), probe.ref)
    val response2 = probe.receiveMessage()
    assert(response2.isSuccess)

    val probe3 = createTestProbe[Seq[Notification]]()
    observer ! Observer.List(probe3.ref)
    val response3 = probe3.receiveMessage()
    assertResult(Seq(Notification(path, action, value)))(response3)
  }

}

object Observer2 {
  def apply(): Behavior[NotificationProtocol.Command] = {
    Behaviors.receiveMessage {
      case Notify(_, replyTo) =>
        replyTo ! StatusReply.Ack
        this ()
    }

  }
}


object Observer {
  private val path = Uri.Path("/")
  private val action = NotificationAction.Put
  sealed trait Command
  case class List(replyTo: ActorRef[Seq[Notification]]) extends Command
  case class WrappedNotification(notification: Notification, replyTo: ActorRef[StatusReply[Done]]) extends Command
  case class InitializeSubscriptions(replyTo: ActorRef[StatusReply[Done]]) extends Command
  def apply(manager: ActorRef[SubscriptionManagerActor.Command]): Behavior[Command] =
    Behaviors.setup(context => new Observer(manager, context))
}

class Observer(val manager: ActorRef[SubscriptionManagerActor.Command], context: ActorContext[Observer.Command]) extends AbstractBehavior[Observer.Command](context)  {
  import Observer._
  private var notifications = Seq[Notification]()
  private val notificationAdapter: ActorRef[NotificationProtocol.Command] = {
    context.messageAdapter {
      case Notify(notification, replyTo) => WrappedNotification(notification, replyTo)
    }
  }

  override def onMessage(msg: Command): Behavior[Command] = msg match {
    case InitializeSubscriptions(replyTo) =>
      manager ! SubscriptionManagerActor.AddSubscription(Subscription(path, action, notificationAdapter), replyTo)
      this
    case WrappedNotification(notification, replyTo) =>
      notifications = notifications :+ notification
      replyTo ! StatusReply.ack()
      this
    case List(replyTo) =>
      replyTo ! notifications
      this
  }
}
