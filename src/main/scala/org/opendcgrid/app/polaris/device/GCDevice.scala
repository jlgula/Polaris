package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import org.opendcgrid.app.polaris.client.definitions.{Device => DeviceProperties}
import org.opendcgrid.app.polaris.client.device.DeviceClient
import org.opendcgrid.app.polaris.client.gc.GcClient
import org.opendcgrid.app.polaris.server.device.DeviceResource
import org.opendcgrid.app.polaris.server.gc.GcResource
import org.opendcgrid.app.polaris.server.subscription.SubscriptionResource

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object GCDevice {
  val devicesPath = "/v1/devices"
  val powerPricePath: Uri.Path = Uri.Path("/v1/gc/powerPrice")
  val dateTimePath: Uri.Path = Uri.Path("/v1/gc/dateTime")
  val powerGrantedProperty = "powerGranted" // Used with makeDeviceSubscriptionPath
  val powerAcceptedProperty = "powerAccepted" // Used with makeDeviceSubscriptionPath

  def apply(uri: Uri, properties: DeviceProperties)(implicit actorSystem: ActorSystem): Future[GCDevice] = {
    implicit val context: ExecutionContext = actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
    val deviceClient = DeviceClient(uri.toString()) // Not used but needed for Device
    val subscriptionHandler = new GCSubscriptionHandler
    val subscriptionRoutes = SubscriptionResource.routes(subscriptionHandler)
    val deviceHandler = new GCDeviceHandler(uri, subscriptionHandler)
    val deviceRoutes = DeviceResource.routes(deviceHandler)
    val gcRoutes = GcResource.routes(new GCHandler(uri, subscriptionHandler, deviceHandler))
    val routes = deviceRoutes ~ gcRoutes ~ subscriptionRoutes
    val gcClient = GcClient(uri.toString())
    Http().newServerAt(uri.authority.host.toString(), uri.authority.port).bindFlow(routes).map(binding => new GCDevice(uri, properties, deviceClient, gcClient, binding))
  }

  /**
   * Creates the observed URI for device properties on the GC.
   *
   * @param gcURI  the base [[Uri]] of the GC device
   * @param deviceID  the [[DeviceID]] of the subscribing device
   * @param property  the name of the property being observered
   * @return  the full [[Uri]] of the observed property
   */
  def makeDeviceSubscriptionURI(gcURI: Uri, deviceID: DeviceID, property: String): Uri = {
    gcURI.withPath(Uri.Path(s"$devicesPath/$deviceID/$property"))
  }
}

class GCDevice(
                val uri: Uri,
                val properties: DeviceProperties,
                val deviceClient: DeviceClient,
                val gcClient: GcClient,
                val serverBinding: Http.ServerBinding) extends Device {
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
