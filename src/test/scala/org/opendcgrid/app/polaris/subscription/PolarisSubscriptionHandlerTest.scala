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

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class PolarisSubscriptionHandlerTest extends AnyFunSuite {
  /*
  private val actorSystem = implicitly[ActorSystem]
  private val subscriptionHandler = new PolarisSubscriptionHandler(actorSystem)
  private val deviceHandler = new PolarisDeviceHandler(subscriptionHandler)
  private val deviceRoutes = DeviceResource.routes(deviceHandler)
  private val gcRoutes = GcResource.routes(new PolarisGCHandler(deviceHandler, subscriptionHandler))

  private val subscriptionRoutes = SubscriptionResource.routes(subscriptionHandler)
  private val routes = deviceRoutes ~ gcRoutes ~ subscriptionRoutes

  implicit val routeFunction: HttpRequest => Future[HttpResponse] = Route.toFunction(routes)
  private val deviceClient = DeviceClient()
  private val gcClient = GcClient()
  private val subscriptionClient = SubscriptionClient()

   */

  class TestNotificationHandler extends NotificationHandler() {
    val observationSeen = new java.util.concurrent.atomic.AtomicBoolean(false)
    override def postNotification(respond: NotificationResource.PostNotificationResponse.type)(body: Notification): Future[NotificationResource.PostNotificationResponse] = {
      observationSeen.set(true)
      //println(s"TestNotificationHandler: $this, $body, $observationSeen")
      Future.successful(respond.NoContent)
    }
  }


  test("observePowerGranted") {
    /*
    val testHandler = new TestNotificationHandler
    val notificationRoutes = NotificationResource.routes(new TestNotificationHandler)
    implicit val routeFunction: HttpRequest => Future[HttpResponse] = Route.toFunction(notificationRoutes)
    validateReset()
    val device = ClientDevice("123", "test")
    validateAddDevice(device)
    val observedURL = "http://localhost:8080/v1/devices/123/powerGranted"
    val observerURL = "http://localhost:8080/v1/notification"
    val subscription = Subscription(observedURL,observerURL)
    val result = subscriptionClient.addSubscription(subscription)
    Await.result(result.value, Duration.Inf) match {
      case Right(AddSubscriptionResponse.Created(_)) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
    val powerGranted = BigDecimal(10.0)
    val result2 = deviceClient.putPowerGranted(device.id, powerGranted)
    Await.result(result2.value, Duration.Inf) match {
      case Right(PutPowerGrantedResponse.NoContent) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
    assertResult(true)(testHandler.observationSeen)

     */
  }

  test("full") {
    implicit def actorSystem: ActorSystem = ActorSystem()
    implicit def context: ExecutionContext = actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
    val localHost = Uri("http://localhost")
    val gcPort = PolarisTestUtilities.getUnusedPort
    val observerPort = PolarisTestUtilities.getUnusedPort
    val gcURL = localHost.withPort(gcPort)
    val testHandler = new TestNotificationHandler
    val notificationRoutes = NotificationResource.routes(testHandler)
    val deviceID = "123"
    val device = ClientDevice(deviceID, "test")
    val subscriptionHandler = new PolarisSubscriptionHandler
    val subscriptionRoutes = SubscriptionResource.routes(subscriptionHandler)
    val deviceHandler = new PolarisDeviceHandler(gcURL, subscriptionHandler)
    val deviceRoutes = DeviceResource.routes(deviceHandler)
    val gcRoutes = GcResource.routes(new PolarisGCHandler(deviceHandler, subscriptionHandler))
    val serverRoutes = deviceRoutes ~ gcRoutes ~ subscriptionRoutes
    val observedURL = gcURL.withPath(Uri.Path("/v1/devices/123/powerGranted"))
    val observerURL = localHost.withPort(observerPort)
    val subscription = Subscription(observedURL.toString(),observerURL.toString())
    val routeFunction: HttpRequest => Future[HttpResponse] = Route.toFunction(serverRoutes)
    val materializer = Materializer(actorSystem)
    val subscriptionClient = SubscriptionClient(gcURL.toString())(routeFunction, context, materializer)
    val powerGranted = BigDecimal(10.0)
    val deviceClient = DeviceClient(gcURL.toString())(routeFunction, context, materializer)

    val result = for {
      //_ <- Http().newServerAt(gcURL.authority.host.toString(), gcURL.authority.port).bindFlow(serverRoutes)
      _ <- Http().newServerAt(observerURL.authority.host.toString(), observerURL.authority.port).bindFlow(notificationRoutes)
      _ <- deviceClient.addDevice(device).value
      _ <- subscriptionClient.addSubscription(subscription).value
      put <- deviceClient.putPowerGranted(deviceID, powerGranted).value
    } yield put
    Await.result(result, Duration.Inf) match {
      case Right(_) => // println(value)// Succeed
      case other => fail(s"unexpected response: $other")
    }
    //println(s"Testhandler: $testHandler ${testHandler.observationSeen.get}")
    assertResult(true)(testHandler.observationSeen.get())
  }
/*
  def validateReset(): Unit = {
    val result2 = gcClient.reset()
    Await.result(result2.value, Duration.Inf) match {
      case Right(ResetResponse.Created(_)) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
  }

  def validateAddDevice(device: ClientDevice): Unit = {
    val result = deviceClient.addDevice(device)
    Await.result(result.value, Duration.Inf) match {
      case Right(AddDeviceResponse.Created(location)) => assertResult(device.id)(location)
      case other => fail(s"unexpected response: $other")
    }
  }

 */
}
