package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.Uri
import akka.pattern.StatusReply
import org.opendcgrid.app.polaris.device.NotificationProtocol.Notify
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.duration.FiniteDuration

class NotifierActorTest extends ScalaTestWithActorTestKit with AnyFunSuiteLike {
  private val path = Uri.Path("/")
  private val action = NotificationAction.Put
  private val notificationValue = "test"

  test("no observers") {
    val probe = createTestProbe[StatusReply[Done]]()
    val value = "test"
    val notification = NotificationProtocol.Notification(path, action, value)
    spawn(NotifierActor(notification, Nil, probe.ref))
    val response = probe.receiveMessage()
    assert(response.isSuccess)
  }

  test("one observer") {
    val probe = createTestProbe[StatusReply[Done]]()
    val notification = NotificationProtocol.Notification(path, action, notificationValue)
    val observer = spawn(Observer2())
    spawn(NotifierActor(notification, Seq(observer.ref), probe.ref))
    val response = probe.receiveMessage()
    assert(response.isSuccess)
  }

  test("N observers") {
    val n = 3
    val probe = createTestProbe[StatusReply[Done]]()
    val notification = NotificationProtocol.Notification(path, action, notificationValue)
    val observers = (0 until n).map(_ => spawn(Observer2()))
    spawn(NotifierActor(notification, observers, probe.ref))
    val response = probe.receiveMessage()
    assert(response.isSuccess)
  }

  test("timeout") {
    val probe = createTestProbe[StatusReply[Done]]()
    val timeout = FiniteDuration(10, scala.concurrent.duration.MILLISECONDS)
    val notification = NotificationProtocol.Notification(path, action, notificationValue)
    val observer = spawn(BadObserver())
    spawn(NotifierActor(notification, Seq(observer.ref), probe.ref, timeout))
    val response = probe.receiveMessage()
    assert(response.isError)
  }

  object BadObserver {
    def apply(): Behavior[NotificationProtocol.Command] = {
      Behaviors.receiveMessage {
        case Notify(_, _) =>
          //replyTo ! StatusReply.Ack
          this ()
      }
    }
  }
}
