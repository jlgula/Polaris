package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Route
import akka.pattern.StatusReply
import akka.stream.Materializer
import org.opendcgrid.app.polaris.client.notification.NotificationResource
import org.opendcgrid.app.polaris.command.CommandUtilities
import org.opendcgrid.app.polaris.server.notification.NotificationClient
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.{ExecutionContext, Future}

class NotificationAdapterActorTest extends ScalaTestWithActorTestKit with AnyFunSuiteLike {
  implicit val sys: ActorSystem[Nothing] = system // Use the ActorSystem provided by ScalaTestWithActorTestKit
  test("notification") {
    val fixture = new NotificationAdapterFixture
    val probe = createTestProbe[StatusReply[Done]]()

    val adapterActor = spawn(NotificationAdapterActor(fixture.notificationClient))
    val notification = NotificationProtocol.Notification(fixture.path, fixture.action, fixture.value)
    adapterActor ! NotificationAdapterActor.WrappedNotification(notification, probe.ref)
    val response = probe.receiveMessage()
    assert(response.isSuccess)
    assertResult(BigDecimal(fixture.value))(fixture.testHandler.observations.peek())
  }
}

/**
 * Fixture to validate http routing of notifications.
 *
 * Note: this fixture does not actually put messages on the wire. It uses Route.toFunction
 * to shortcut http transmissions locally.
 *
 * @param system the [[ActorSystem]] in use
 */
class NotificationAdapterFixture(implicit system: ActorSystem[Nothing]) {
  implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
  implicit def context: ExecutionContext = system.executionContext
  private val localHost = Uri("http://localhost")
  private val gcPort = CommandUtilities.getUnusedPort
  private val gcURI = localHost.withPort(gcPort)
  val testHandler = new TestNotificationHandler
  private val notificationRoutes = NotificationResource.routes(testHandler)
  val deviceID = "123"
  private val routeFunction: HttpRequest => Future[HttpResponse] = Route.toFunction(notificationRoutes)
  private val materializer = Materializer(system)
  val notificationClient: NotificationClient = NotificationClient(gcURI.toString())(routeFunction, context, materializer)
  val path: Uri.Path = gcURI.path
  val action: NotificationAction = NotificationAction.Put
  val value = "1.0"   // Note this is a JSON value convertable to a BigNum.
}

