package org.opendcgrid.app.pclient

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.opendcgrid.app.pclient.device.{DeviceClient, ListDevicesResponse}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object PolarisClient extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
  val deviceClient = DeviceClient()
  val result = deviceClient.listDevices()

  Await.result(result.value, Duration.Inf) match {
    case Right(ListDevicesResponse.OK(value)) => println(value)
    case Right(ListDevicesResponse.BadRequest) => println(s"Bad request.")
    case Left(Left(throwable)) => println(s"Failed: ${throwable.getMessage}")
    case Left(Right(response)) => println(s"Failed: unexpected response $response")
  }

  Await.result(system.terminate(), Duration.Inf)
  //println("done")
}
