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
import scala.concurrent.{Await, ExecutionContext, Future, TimeoutException}
import scala.util.{Failure, Success, Try}

class PolarisServer(uri: Uri) (implicit actorSystem: ActorSystem){
  private var binding: Option[Http.ServerBinding] = None
  def start(): Try[Unit] = {
    implicit val context: ExecutionContext = actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
    val subscriptionHandler = new PolarisSubscriptionHandler
    val subscriptionRoutes = SubscriptionResource.routes(subscriptionHandler)
    val deviceHandler = new PolarisDeviceHandler(uri, subscriptionHandler)
    val deviceRoutes = DeviceResource.routes(deviceHandler)
    val gcRoutes = GcResource.routes(new PolarisGCHandler(deviceHandler, subscriptionHandler))
    val routes = deviceRoutes ~ gcRoutes ~ subscriptionRoutes
    try {
      val result = Await.result(Http().newServerAt(uri.authority.host.toString(), uri.authority.port).bindFlow(routes), Duration.Inf)
      binding = Some(result)
      Success(())
    } catch {
      case _: TimeoutException => Failure(ServerError.Timeout)
      case _: InterruptedException => Failure(ServerError.Interrupted)
    }

  }

  def terminate(): Try[Unit] = {
    if (binding.isDefined) Success(())
    else Failure(ServerError.NotStarted)
  }
}
