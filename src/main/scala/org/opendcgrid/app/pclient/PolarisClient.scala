package org.opendcgrid.app.pclient

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import org.opendcgrid.app.pclient.device.{DeviceClient, ListDevicesResponse}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object PolarisClient extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher
  //implicit val requester: (HttpRequest, HttpsConnectionContext, ConnectionPoolSettings, LoggingAdapter) => Future[HttpResponse] = Http().singleRequest
  implicit val requester: HttpRequest => Future[HttpResponse] = request => {
    //val context: HttpsConnectionContext = ConnectionContext.httpsClient()
    //val settings: ConnectionPoolSettings = ???
    //val loggingAdapter: LoggingAdapter = ???
    //Http().singleRequest(request, context, settings, loggingAdapter)
    Http().singleRequest(request)
  }
  val deviceClient = DeviceClient()
  val result = deviceClient.listDevices()
  Await.result(result.value, Duration.Inf) match {
    case Right(ListDevicesResponse.OK(value)) => println(value)
    case Right(ListDevicesResponse.BadRequest) => println(s"Bad request.")
    case Left(value) => println(s"Failed: $value")
  }
  Await.result(system.terminate(), Duration.Inf)
  //println("done")
}
