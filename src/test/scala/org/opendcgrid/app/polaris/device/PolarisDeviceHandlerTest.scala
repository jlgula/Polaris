package org.opendcgrid.app.polaris.device

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest

import org.opendcgrid.app.pclient.definitions.{Device => ClientDevice}
import org.opendcgrid.app.pclient.device.{AddDeviceResponse, DeviceClient, GetDeviceResponse, ListDevicesResponse}

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

  test("listDevices") {
    validateListDevices(Vector[ClientDevice]())
  }

  test("addDevice") {
    val device = ClientDevice("123", "test")
    validateAddDevice(device)
    validateListDevices(Vector(device))
  }

  test("getDevice") {
    val device = ClientDevice("123", "test")
    validateAddDevice(device)
    val result2 = deviceClient.getDevice(device.id)
    Await.result(result2.value, Duration.Inf) match {
      case Right(GetDeviceResponse.OK(value)) => assertResult(device)(value)
      case Left(Left(throwable)) => fail(s"Failed: ${throwable.getMessage}")
      case Left(Right(response)) => fail(s"Failed: unexpected response $response")
    }
  }

  def validateListDevices(expected: Vector[ClientDevice]): Unit = {
    val result2 = deviceClient.listDevices()
    Await.result(result2.value, Duration.Inf) match {
      case Right(ListDevicesResponse.OK(value)) => assertResult(expected)(value)
      case Right(ListDevicesResponse.BadRequest) => fail("Bad request.")
      case Left(Left(throwable)) => fail(s"Failed: ${throwable.getMessage}")
      case Left(Right(response)) => fail(s"Failed: unexpected response $response")
    }
  }

  def validateAddDevice(device: ClientDevice): Unit = {
    val result = deviceClient.addDevice(device)
    Await.result(result.value, Duration.Inf) match {
      case Right(AddDeviceResponse.Created(createdDevice)) => assertResult(device)(createdDevice)
      case Left(Left(throwable)) => fail(s"Failed: ${throwable.getMessage}")
      case Left(Right(response)) => fail(s"Failed: unexpected response $response")
    }
  }

}
