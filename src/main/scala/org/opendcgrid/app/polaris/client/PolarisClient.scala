package org.opendcgrid.app.polaris.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.opendcgrid.app.polaris.client.definitions.Device
import org.opendcgrid.app.polaris.client.device.{DeviceClient, ListDevicesResponse}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}
/*
object PolarisClient extends App {
  val defaultHost = "http://localhost:8080"
  val client = new PolarisClient(defaultHost)
  println(client.listDevices())
  client.terminate()
}

class PolarisClient(val host: String) {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
  private val deviceClient = DeviceClient(host)

  def listDevices(): Try[Vector[Device]] = {
    val result = deviceClient.listDevices()

    Await.result(result.value, Duration.Inf) match {
      case Right(ListDevicesResponse.OK(value)) => Success(value)
      case Left(Left(throwable)) => Failure(throwable)
      case Left(Right(response)) => Failure(new IllegalStateException(s"Failed: unexpected response $response"))
    }
  }

  def terminate(): Unit = Await.result(system.terminate(), Duration.Inf)
}

 */
