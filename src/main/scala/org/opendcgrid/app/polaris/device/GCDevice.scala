package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import org.opendcgrid.app.polaris.server.device.DeviceResource
import org.opendcgrid.app.polaris.server.gc.GcResource
import org.opendcgrid.app.polaris.server.subscription.SubscriptionResource

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object GCDevice {
  def apply(uri: Uri, name: String)(implicit actorSystem: ActorSystem): Future[Device] = {
    implicit val context: ExecutionContext = actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
    val subscriptionHandler = new GCSubscriptionHandler
    val subscriptionRoutes = SubscriptionResource.routes(subscriptionHandler)
    val deviceHandler = new GCDeviceHandler(uri, subscriptionHandler)
    val deviceRoutes = DeviceResource.routes(deviceHandler)
    val gcRoutes = GcResource.routes(new GCHandler(deviceHandler, subscriptionHandler))
    val routes = deviceRoutes ~ gcRoutes ~ subscriptionRoutes
    Http().newServerAt(uri.authority.host.toString(), uri.authority.port).bindFlow(routes).map(binding => new GCDevice(binding))
  }
}

class GCDevice(val serverBinding: Http.ServerBinding) extends Device {
  override def terminate(): Future[Http.HttpTerminated] = serverBinding.terminate(FiniteDuration(1, "seconds"))
}
/*
class PolarisServer(val uri: Uri, val name: String, taskManager: TaskManager) (implicit actorSystem: ActorSystem) extends Task {
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  @volatile private var binding: Option[Http.ServerBinding] = None
  @volatile private var id: Option[TaskID] = None
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
      case Success(binding) =>
        this.binding = Some(binding)
        val taskID = taskManager.startTask(this)
        this.id = Some(taskID)
        Success(taskID)
      case Failure(error) => Failure(error)
    }}
  }

  def terminate(): Future[Unit] = {
    if (binding.isDefined) binding.get.terminate(FiniteDuration(1, "seconds")).flatMap(endTaskWithFuture)
    else Future.failed(ServerError.NotStarted)
  }

  private def endTaskWithFuture(binding: Http.HttpTerminated): Future[Unit] = {
    if (id.isDefined) {
      taskManager.endTask(id.get)
      Future.successful(())
    } else Future.failed(ServerError.NotStarted)
  }
}

 */
