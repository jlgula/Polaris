package org.opendcgrid.app.polaris.device

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.opendcgrid.app.pclient.definitions.{Device => ClientDevice}
import org.opendcgrid.app.pclient.device.{AddDeviceResponse, DeviceClient, GetDeviceResponse, GetPowerGrantedResponse, ListDevicesResponse, PutDeviceResponse, PutPowerGrantedResponse, ResetResponse}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class PolarisDeviceHandlerTest extends AnyFunSuite with ScalatestRouteTest {
  val routes: Route = DeviceResource.routes(new PolarisDeviceHandler())
  implicit val routeFunction: HttpRequest => Future[HttpResponse] = Route.toFunction(routes)
  val deviceClient: DeviceClient = DeviceClient()
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
    validateReset()
  }

  test("listDevices") {
    validateReset()
    validateListDevices(Vector[ClientDevice]())
  }

  test("addDevice") {
    validateReset()
    val device = ClientDevice("123", "test")
    validateAddDevice(device)
    validateListDevices(Vector(device))
  }

  test("getDevice") {
    validateReset()
    val device = ClientDevice("123", "test")
    validateAddDevice(device)
    validateGetDevice(device.id, device)
  }

  test("getDevice not found") {
    val result2 = deviceClient.getDevice("bad")
    Await.result(result2.value, Duration.Inf) match {
      case Right(GetDeviceResponse.NotFound(_)) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
  }

  test("putDevice") {
    validateReset()
    val device = ClientDevice("123", "test")
    validateAddDevice(device)
    val update = ClientDevice("123", "changed", Some(10.0))
    validatePutDevice(update)
    validateGetDevice(device.id, update)
  }

  test("put/get PowerGranted") {
    validateReset()
    val device = ClientDevice("123", "test", Some(10.0))
    validateAddDevice(device)
    val powerGranted = BigDecimal(10.0)
    val result = deviceClient.putPowerGranted(device.id, powerGranted)
    Await.result(result.value, Duration.Inf) match {
      case Right(PutPowerGrantedResponse.NoContent) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
    val result2 = deviceClient.getPowerGranted(device.id)
    Await.result(result2.value, Duration.Inf) match {
      case Right(GetPowerGrantedResponse.OK(value)) => assertResult(powerGranted)(value)
      case other => fail(s"unexpected response: $other")
    }

  }

  def validateReset(): Unit = {
    val result2 = deviceClient.reset()
    Await.result(result2.value, Duration.Inf) match {
      case Right(ResetResponse.Created(_)) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
  }

  def validateListDevices(expected: Vector[ClientDevice]): Unit = {
    val result2 = deviceClient.listDevices()
    Await.result(result2.value, Duration.Inf) match {
      case Right(ListDevicesResponse.OK(value)) => assertResult(expected)(value)
      case Left(Left(throwable)) => fail(s"Failed: ${throwable.getMessage}")
      case Left(Right(response)) => fail(s"Failed: unexpected response $response")
    }
  }

  def validateAddDevice(device: ClientDevice): Unit = {
    val result = deviceClient.addDevice(device)
    Await.result(result.value, Duration.Inf) match {
      case Right(AddDeviceResponse.Created(location)) => assertResult(device.id)(location)
      case Left(Left(throwable)) => fail(s"Failed: ${throwable.getMessage}")
      case Left(Right(response)) => fail(s"Failed: unexpected response $response")
    }
  }

  def validateGetDevice(id: String, expected: ClientDevice): Unit = {
    val result2 = deviceClient.getDevice(id)
    Await.result(result2.value, Duration.Inf) match {
      case Right(GetDeviceResponse.OK(value)) => assertResult(expected)(value)
      case other => fail(s"unexpected response: $other")
    }
  }

  def validatePutDevice(updatedDevice: ClientDevice): Unit = {
    val result2 = deviceClient.putDevice(updatedDevice.id, updatedDevice)
    Await.result(result2.value, Duration.Inf) match {
      case Right(PutDeviceResponse.NoContent) => // Succeed
      case other => fail(s"unexpected response: $other")
    }
  }

}
