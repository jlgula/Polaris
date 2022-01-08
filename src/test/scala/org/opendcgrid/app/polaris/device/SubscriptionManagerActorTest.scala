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

    val observed = Uri("http://localhost/test")
    val id = "test"
    //val value = "testValue"
    val manager = spawn(SubscriptionManagerActor())
    //val notifier = spawn(Notifier(manager))
    val observer = createTestProbe[NotificationProtocol.Command]()
    val observerUri = Uri(observer.ref.path.toString)
    val subscription = Subscription(id, observed, NotificationAction.Put, observerUri, observer.ref)

    val probe = createTestProbe[StatusReply[String]]()
    manager ! SubscriptionManagerActor.AddSubscription(subscription, probe.ref)
    val response1 = probe.receiveMessage()
    assert(response1.isSuccess)
    assertResult(id)(response1.getValue)


    val probe2 = createTestProbe[SubscriptionManagerActor.SubscriptionResponse]()
    manager ! SubscriptionManagerActor.ListSubscriptions(probe2.ref)
    val response2 = probe2.receiveMessage()
    assert(response2.subscriptions.toSeq.contains(subscription))

    val probe3 = createTestProbe[StatusReply[Done]]()
    manager ! SubscriptionManagerActor.RemoveSubscription(id, probe3.ref)
    val response3 = probe3.receiveMessage()
    assert(response3.isSuccess)

    manager ! SubscriptionManagerActor.ListSubscriptions(probe2.ref)
    val response4 = probe2.receiveMessage()
    assert(!response4.subscriptions.toSeq.contains(subscription))
  }

  test("notifier") {
    val probe = createTestProbe[StatusReply[Done]]()
    val path = Uri.Path("/")
    val action = NotificationAction.Put
    val value = "test"
    val notification = NotificationProtocol.Notification(path, action, value)
    val observer = spawn(Observer2())
    spawn(NotifierActor(notification, Seq(observer.ref), probe.ref))
    val response = probe.receiveMessage()
    assert(response.isSuccess)
  }

  test("add/notify") {
    val value = "testValue"
    val manager = spawn(SubscriptionManagerActor())
    val observer = spawn(Observer(manager))

    val probe = createTestProbe[StatusReply[String]]()
    observer ! Observer.InitializeSubscriptions(probe.ref)
    val response = probe.receiveMessage()
    assert(response.isSuccess)

    val probe2 = createTestProbe[StatusReply[Done]]()
    manager ! SubscriptionManagerActor.Notify(Notification(Observer.observed.path, Observer.action, value), probe2.ref)
    val response2 = probe2.receiveMessage()
    assert(response2.isSuccess)

    val probe3 = createTestProbe[Seq[Notification]]()
    observer ! Observer.List(probe3.ref)
    val response3 = probe3.receiveMessage()
    assertResult(Seq(Notification(Observer.observed.path, Observer.action, value)))(response3)
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
  val observed: Uri = Uri("http://localhost/test")
  val id = "test"
  val action: NotificationAction = NotificationAction.Put
  sealed trait Command
  case class List(replyTo: ActorRef[Seq[Notification]]) extends Command
  case class WrappedNotification(notification: Notification, replyTo: ActorRef[StatusReply[Done]]) extends Command
  case class InitializeSubscriptions(replyTo: ActorRef[StatusReply[String]]) extends Command
  def apply(manager: ActorRef[SubscriptionManagerActor.Command]): Behavior[Command] =
    Behaviors.setup(context => new Observer(manager, context))
}

class Observer(val manager: ActorRef[SubscriptionManagerActor.Command], context: ActorContext[Observer.Command]) extends AbstractBehavior[Observer.Command](context)  {
  import Observer._
  private val observer: Uri = Uri("http://localhost")
  private var notifications = Seq[Notification]()
  private val notificationAdapter: ActorRef[NotificationProtocol.Command] = {
    context.messageAdapter {
      case Notify(notification, replyTo) => WrappedNotification(notification, replyTo)
    }
  }

  override def onMessage(msg: Command): Behavior[Command] = msg match {
    case InitializeSubscriptions(replyTo) =>
      manager ! SubscriptionManagerActor.AddSubscription(Subscription(id, observed, action, observer, notificationAdapter), replyTo)
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
