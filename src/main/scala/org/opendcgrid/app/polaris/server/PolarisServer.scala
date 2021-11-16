package org.opendcgrid.app.polaris.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import org.opendcgrid.app.polaris.server.device.{DeviceResource, PolarisDeviceHandler}
import org.opendcgrid.app.polaris.server.gc.{GcResource, PolarisGCHandler}
import org.opendcgrid.app.polaris.server.subscription.{PolarisSubscriptionHandler, SubscriptionResource}
import org.opendcgrid.lib.task.{Task, TaskID, TaskManager}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

class PolarisServer(val uri: Uri, val name: String, taskManager: TaskManager) (implicit actorSystem: ActorSystem) extends Task {
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  @volatile private var binding: Option[Http.ServerBinding] = None
  def start(): Future[TaskID] = {
    implicit val context: ExecutionContext = actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
    val subscriptionHandler = new PolarisSubscriptionHandler
    val subscriptionRoutes = SubscriptionResource.routes(subscriptionHandler)
    val deviceHandler = new PolarisDeviceHandler(uri, subscriptionHandler)
    val deviceRoutes = DeviceResource.routes(deviceHandler)
    val gcRoutes = GcResource.routes(new PolarisGCHandler(deviceHandler, subscriptionHandler))
    val routes = deviceRoutes ~ gcRoutes ~ subscriptionRoutes
    val bindingFuture = Http().newServerAt(uri.authority.host.toString(), uri.authority.port).bindFlow(routes)
    /*
    bindingFuture.map { binding =>
      this.binding = Some(binding)
      taskManager.startTask(this)
    }

     */
    bindingFuture.transform[TaskID] { b: Try[Http.ServerBinding] => b match {
      case Success(binding) =>  this.binding = Some(binding); Success(taskManager.startTask(this))
      case Failure(error) => Failure(error)
    }}
  }

  def terminate(): Future[Unit] = {
    if (binding.isDefined) binding.get.terminate(FiniteDuration(1, "seconds")).map(_ => ())
    else Future.failed(ServerError.NotStarted)
  }
}
