package org.opendcgrid.app.polaris.device

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.Materializer
import org.opendcgrid.app.pclient.device.ListDevicesResponse
import org.scalatest.funsuite.AnyFunSuite
import org.opendcgrid.app.polaris.definitions.Device

import scala.concurrent.{ExecutionContext, Future}

class PolarisDeviceHandlerTest extends AnyFunSuite with ScalatestRouteTest {
  val routes = DeviceResource.routes(new PolarisDeviceHandler())
  test("listDevices") {
    type FromResponseUnmarshaller[T] = Unmarshaller[HttpResponse, T]
    implicit val unmarshaller: FromResponseUnmarshaller[Vector[Device]] = new Unmarshaller[HttpResponse, Vector[Device]] {
      override def apply(value: HttpResponse)(implicit ec: ExecutionContext, materializer: Materializer): Future[Vector[Device]] = Future.successful(Vector[Device]())
    }//??? //Unmarshal(resp.entity).to[Vector[_root_.org.opendcgrid.app.pclient.definitions.Device]](listDevicesOKDecoder, implicitly, implicitly).map(x => Right(ListDevicesResponse.OK(x)))
    Get("/v1/devices") -> routes -> check {
      assertResult(Vector[Device]())(responseAs[Vector[Device]])
    }
    //implicit val system = ActorSystem()
    // needed for the future flatMap/onComplete in the end
    //implicit val executionContext = system.dispatcher
    /*
    val deviceHttpClient: HttpRequest => Future[HttpResponse] = Route.toFunction(routes)
    val deviceClient: DeviceClient = DeviceClient.httpClient(deviceHttpClient)
    //val getUserResponse: EitherT[Future, Either[Throwable, HttpResponse], Vector[Device]] = deviceClient.listDevices().map(_.fold(d => d))
    val listDeviceResponse = deviceClient.listDevices().value
    val x = Await.result(listDeviceResponse, Duration.Inf)
    val yy = x.getOrElse(throw new IllegalStateException("failed"))
    val devices = yy match {
      case ListDevicesResponse.OK(value) => value
      case ListDevicesResponse.BadRequest => throw new IllegalStateException("failed")
    }
    assert(devices.isEmpty)

     */
  }
}
