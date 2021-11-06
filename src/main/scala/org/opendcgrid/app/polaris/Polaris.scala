package org.opendcgrid.app.polaris

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import org.opendcgrid.app.polaris.device.{DeviceResource, PolarisDeviceHandler}
import org.opendcgrid.app.polaris.gc.{GcResource, PolarisGCHandler}
import org.opendcgrid.app.polaris.subscription.{PolarisSubscriptionHandler, SubscriptionResource}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

object Polaris extends App {
  implicit def actorSystem: ActorSystem = ActorSystem()
  implicit val context: ExecutionContext = actorSystem.dispatcher
  implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
  private val subscriptionHandler = new PolarisSubscriptionHandler
  private val subscriptionRoutes = SubscriptionResource.routes(subscriptionHandler)
  private val url = Uri("http://localhost:8080")
  private val deviceHandler = new PolarisDeviceHandler(url, subscriptionHandler)
  private val deviceRoutes = DeviceResource.routes(deviceHandler)
  private val gcRoutes = GcResource.routes(new PolarisGCHandler(deviceHandler, subscriptionHandler))
  private val routes = deviceRoutes ~ gcRoutes ~ subscriptionRoutes
  Await.result(Http().newServerAt(url.authority.host.toString(), url.authority.port).bindFlow(routes), Duration.Inf)
  println("Running at http://localhost:8080!")
}
