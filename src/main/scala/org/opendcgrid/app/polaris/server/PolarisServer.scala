package org.opendcgrid.app.polaris.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import org.opendcgrid.app.polaris.server.device.{DeviceResource, PolarisDeviceHandler}
import org.opendcgrid.app.polaris.server.gc.{GcResource, PolarisGCHandler}
import org.opendcgrid.app.polaris.server.subscription.{PolarisSubscriptionHandler, SubscriptionResource}

import java.util.concurrent.Semaphore
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class PolarisServer(uri: Uri) (implicit actorSystem: ActorSystem) extends Runnable {
  private val haltSemaphore = new Semaphore(0)
  override def run(): Unit = {
    implicit val context: ExecutionContext = actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
    val subscriptionHandler = new PolarisSubscriptionHandler
    val subscriptionRoutes = SubscriptionResource.routes(subscriptionHandler)
    val deviceHandler = new PolarisDeviceHandler(uri, subscriptionHandler)
    val deviceRoutes = DeviceResource.routes(deviceHandler)
    val gcRoutes = GcResource.routes(new PolarisGCHandler(deviceHandler, subscriptionHandler))
    val routes = deviceRoutes ~ gcRoutes ~ subscriptionRoutes
    Await.result(Http().newServerAt(uri.authority.host.toString(), uri.authority.port).bindFlow(routes), Duration.Inf)
    println(s"Running at ${uri.toString()}!")
    haltSemaphore.acquire()
  }

  def halt(): Unit = haltSemaphore.release()
}
