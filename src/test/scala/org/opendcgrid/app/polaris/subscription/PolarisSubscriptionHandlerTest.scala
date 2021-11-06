package org.opendcgrid.app.polaris.subscription

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import org.opendcgrid.app.pclient.definitions.{Subscription, Device => ClientDevice}
import org.opendcgrid.app.pclient.device.DeviceClient
import org.opendcgrid.app.pclient.subscription.SubscriptionClient
import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.definitions.Notification
import org.opendcgrid.app.polaris.device.{DeviceResource, PolarisDeviceHandler}
import org.opendcgrid.app.polaris.gc.{GcResource, PolarisGCHandler}
import org.opendcgrid.app.polaris.notification.{NotificationHandler, NotificationResource}
import org.scalatest.funsuite.AnyFunSuite

import java.util.concurrent.ConcurrentLinkedQueue
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class PolarisSubscriptionHandlerTest extends AnyFunSuite {

  test("PowerGranted observed") {
    val fixture = new TestFixture
    implicit val ec: ExecutionContext = fixture.context
    val powerGranted = BigDecimal(10.0)
    val result = fixture.start().flatMap(_ => fixture.deviceClient.putPowerGranted(fixture.deviceID, powerGranted).value)
    Await.result(result, Duration.Inf) match {
      case Right(_) => // println(value)// Succeed
      case other => fail(s"unexpected response: $other")
    }
    val observation = fixture.testHandler.observations.peek()
    assertResult(powerGranted)(observation)
  }
}

class TestNotificationHandler extends NotificationHandler() {
  import io.circe.parser.decode
  val observations = new ConcurrentLinkedQueue[BigDecimal]()
  override def postNotification(respond: NotificationResource.PostNotificationResponse.type)(body: Notification): Future[NotificationResource.PostNotificationResponse] = {
    decode[BigDecimal](body.value) match {
      case Right(bigNum) => observations.add(bigNum)
      case Left(error) => throw new IllegalStateException(s"postNotification - unexpected result: $error")
    }
    //println(s"TestNotificationHandler: $this, $body, $observationSeen")
    Future.successful(respond.NoContent)
  }
}

class TestFixture {
  implicit def actorSystem: ActorSystem = ActorSystem()
  implicit def context: ExecutionContext = actorSystem.dispatcher
  implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
  private val localHost = Uri("http://localhost")
  private val gcPort = PolarisTestUtilities.getUnusedPort
  private val observerPort = PolarisTestUtilities.getUnusedPort
  private val gcURL = localHost.withPort(gcPort)
  val testHandler = new TestNotificationHandler
  private val notificationRoutes = NotificationResource.routes(testHandler)
  val deviceID = "123"
  private val device = ClientDevice(deviceID, "test")
  private val subscriptionHandler = new PolarisSubscriptionHandler
  private val subscriptionRoutes = SubscriptionResource.routes(subscriptionHandler)
  private val deviceHandler = new PolarisDeviceHandler(gcURL, subscriptionHandler)
  private val deviceRoutes = DeviceResource.routes(deviceHandler)
  private val gcRoutes = GcResource.routes(new PolarisGCHandler(deviceHandler, subscriptionHandler))
  private val serverRoutes = deviceRoutes ~ gcRoutes ~ subscriptionRoutes
  private val observedURL = gcURL.withPath(Uri.Path("/v1/devices/123/powerGranted"))
  private val observerURL = localHost.withPort(observerPort)
  private val subscription = Subscription(observedURL.toString(),observerURL.toString())
  private val routeFunction: HttpRequest => Future[HttpResponse] = Route.toFunction(serverRoutes)
  private val materializer = Materializer(actorSystem)
  private val subscriptionClient = SubscriptionClient(gcURL.toString())(routeFunction, context, materializer)
  val deviceClient: DeviceClient = DeviceClient(gcURL.toString())(routeFunction, context, materializer)

  def start(): Future[Unit] = for {
    _ <- Http().newServerAt(observerURL.authority.host.toString(), observerURL.authority.port).bindFlow(notificationRoutes)
    _ <- deviceClient.addDevice(device).value
    _ <- subscriptionClient.addSubscription(subscription).value
  } yield ()

}
