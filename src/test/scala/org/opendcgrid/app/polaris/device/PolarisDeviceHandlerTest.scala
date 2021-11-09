package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.opendcgrid.app.pclient.definitions.{Device => ClientDevice}
import org.opendcgrid.app.pclient.device.{AddDeviceResponse, DeleteDeviceResponse, DeviceClient, GetDeviceResponse, GetPowerAcceptedResponse, GetPowerGrantedResponse, ListDevicesResponse, PutDeviceResponse, PutPowerAcceptedResponse, PutPowerGrantedResponse}
import org.opendcgrid.app.pclient.gc.{GcClient, ResetResponse}
import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.gc.{GcResource, PolarisGCHandler}
import org.opendcgrid.app.polaris.subscription.PolarisSubscriptionHandler
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class PolarisDeviceHandlerTest extends AnyFunSuite with ScalatestRouteTest {
  private val actorSystem = implicitly[ActorSystem]
  val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
  private val subscriptionHandler = new PolarisSubscriptionHandler()(actorSystem, requester)
  private val gcURL = Uri("http://localhost").withPort(PolarisTestUtilities.getUnusedPort)
  private val deviceHandler = new PolarisDeviceHandler(gcURL, subscriptionHandler)
  private val deviceRoutes = DeviceResource.routes(deviceHandler)
  private val gcRoutes = GcResource.routes(new PolarisGCHandler(deviceHandler, subscriptionHandler))
  private val routes = deviceRoutes ~ gcRoutes

  implicit val routeFunction: HttpRequest => Future[HttpResponse] = Route.toFunction(routes)
  private val deviceClient = DeviceClient()
  private val gcClient = GcClient()
  /*
   test("listDevices") {
     type FromResponseUnmarshaller[T] = Unmarshaller[HttpResponse, T]
     implicit val unmarshaller: FromResponseUnmarshaller[Vector[Device]] = new Unmarshaller[HttpResponse, Vector[Device]] {
       override def apply(value: HttpResponse)(implicit ec: ExecutionContext, materializer: Materializer): Future[Vector[Device]] = Future.successful(Vector[Device]())
     }
     Get("/v1/devices") -> routes -> check {
       assertResult(Vector[Device]())(responseAs[Vector[Device]])
     }
    }

    */
  test("reset") {
    reset()
  }

  test("listDevices") {
    reset()
    assertResult(Vector[ClientDevice]())(listDevices())
  }

  test("addDevice") {
    reset()
    val device = ClientDevice("123", "test")
    addDevice(device)
    assertResult(Vector(device))(listDevices())
  }

  test("getDevice") {
    reset()
    val device = ClientDevice("123", "test")
    addDevice(device)
    assertResult(device)(getDevice(device.id))
  }

  test("getDevice not found") {
    verifyNotFound("bad")
  }

  test("putDevice") {
    reset()
    val device = ClientDevice("123", "test")
    addDevice(device)
    val update = ClientDevice("123", "changed", Some(10.0))
    putDevice(update)
    assertResult(update)(getDevice(device.id))
  }

  test("deleteDevice") {
    reset()
    val device = ClientDevice("123", "test")
    addDevice(device)
    deleteDevice(device.id)
    verifyNotFound(device.id)
    assertResult(Vector[ClientDevice]())(listDevices())
  }

  test("put/get PowerGranted") {
    reset()
    val id = "123"
    val power = BigDecimal(10.0)
    val device = ClientDevice(id, "test", powerRequested = Some(power))
    addDevice(device)
    assertResult(BigDecimal(0.0))(this.getPowerGranted(id))
    putPowerGranted(id, power)
    assertResult(power)(this.getPowerGranted(id))
  }

  test("put/get PowerAccepted") {
    reset()
    val id = "123"
    val power = BigDecimal(10.0)
    val device = ClientDevice(id, "test", powerOffered = Some(power))
    addDevice(device)
    assertResult(BigDecimal(0.0))(this.getPowerAccepted(id))
    putPowerAccepted(id, power)
    assertResult(power)(this.getPowerAccepted(id))
  }

  def reset(): Unit = {
    val result = gcClient.reset()
    Await.result(result.value, Duration.Inf) match {
      case Right(ResetResponse.Created(_)) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
  }

  def listDevices(): Vector[ClientDevice] = {
    val result = deviceClient.listDevices()
    Await.result(result.value, Duration.Inf) match {
      case Right(ListDevicesResponse.OK(value)) => value
      case other => fail(s"unexpected response: $other")
    }
  }

  def addDevice(device: ClientDevice): Unit = {
    val result = deviceClient.addDevice(device)
    Await.result(result.value, Duration.Inf) match {
      case Right(AddDeviceResponse.Created(location)) => assertResult(device.id)(location)
      case other => fail(s"unexpected response: $other")
    }
  }

  def getDevice(id: String): ClientDevice = {
    val result = deviceClient.getDevice(id)
    Await.result(result.value, Duration.Inf) match {
      case Right(GetDeviceResponse.OK(value)) => value
      case other => fail(s"unexpected response: $other")
    }
  }

  def putDevice(updatedDevice: ClientDevice): Unit = {
    val result = deviceClient.putDevice(updatedDevice.id, updatedDevice)
    Await.result(result.value, Duration.Inf) match {
      case Right(PutDeviceResponse.NoContent) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
  }

  def deleteDevice(id: String): Unit = {
    val result = deviceClient.deleteDevice(id)
    Await.result(result.value, Duration.Inf) match {
      case Right(DeleteDeviceResponse.NoContent) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
  }

  def verifyNotFound(id: String): Unit = {
    val result = deviceClient.getDevice(id)
    Await.result(result.value, Duration.Inf) match {
      case Right(GetDeviceResponse.NotFound(_)) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
  }

  def putPowerGranted(id: String, powerGranted: BigDecimal): Unit = {
    val result = deviceClient.putPowerGranted(id, powerGranted)
    Await.result(result.value, Duration.Inf) match {
      case Right(PutPowerGrantedResponse.NoContent) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
  }

  def getPowerGranted(id: String): BigDecimal = {
    val result = deviceClient.getPowerGranted(id)
    Await.result(result.value, Duration.Inf) match {
      case Right(GetPowerGrantedResponse.OK(value)) => value
      case other => fail(s"unexpected response: $other")
    }
  }

  def putPowerAccepted(id: String, powerGranted: BigDecimal): Unit = {
    val result = deviceClient.putPowerAccepted(id, powerGranted)
    Await.result(result.value, Duration.Inf) match {
      case Right(PutPowerAcceptedResponse.NoContent) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
  }

  def getPowerAccepted(id: String): BigDecimal = {
    val result = deviceClient.getPowerAccepted(id)
    Await.result(result.value, Duration.Inf) match {
      case Right(GetPowerAcceptedResponse.OK(value)) => value
      case other => fail(s"unexpected response: $other")
    }
  }
}
