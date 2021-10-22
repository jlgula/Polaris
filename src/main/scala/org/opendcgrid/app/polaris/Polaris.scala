package org.opendcgrid.app.polaris

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import org.opendcgrid.app.polaris.device.{DeviceResource, PolarisDeviceHandler}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Polaris extends App {
  implicit def actorSystem: ActorSystem = ActorSystem()
  val routes = DeviceResource.routes(new PolarisDeviceHandler())
  Await.result(Http().newServerAt("127.0.0.1", 8080).bindFlow(routes), Duration.Inf)
  println("Running at http://localhost:8080!")
}
