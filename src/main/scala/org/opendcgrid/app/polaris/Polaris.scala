package org.opendcgrid.app.polaris

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import org.opendcgrid.app.polaris.device.{DeviceResource, PolarisDeviceHandler}
import org.opendcgrid.app.polaris.gc.{GcResource, PolarisGCHandler}
import org.opendcgrid.app.polaris.subscription.{PolarisSubscriptionHandler, SubscriptionResource}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Polaris extends App {
  implicit def actorSystem: ActorSystem = ActorSystem()
  private val subscriptionHandler = new PolarisSubscriptionHandler()
  private val subscriptionRoutes = SubscriptionResource.routes(subscriptionHandler)
  private val deviceHandler = new PolarisDeviceHandler(subscriptionHandler)
  private val deviceRoutes = DeviceResource.routes(deviceHandler)
  private val gcRoutes = GcResource.routes(new PolarisGCHandler(deviceHandler, subscriptionHandler))
  private val routes = deviceRoutes ~ gcRoutes ~ subscriptionRoutes
  Await.result(Http().newServerAt("127.0.0.1", 8080).bindFlow(routes), Duration.Inf)
  println("Running at http://localhost:8080!")
}
