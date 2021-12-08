package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import org.opendcgrid.app.polaris.PolarisAppOption
import org.opendcgrid.app.polaris.client.definitions.Device
import org.opendcgrid.app.polaris.client.device.DeviceClient
import org.opendcgrid.app.polaris.client.device.ListDevicesResponse.OK
import org.opendcgrid.app.polaris.device.{DeviceDescriptor, DeviceManager}
import org.opendcgrid.lib.commandoption.CommandOptionResult

import java.net.ServerSocket
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

object CommandUtilities {
  def parsePort(result: CommandOptionResult, defaultPort: Int): Try[Int] = {
    result.options.collectFirst { case port: PolarisAppOption.Port => port } match {
      case None => Success(defaultPort)
      case Some(PolarisAppOption.Port(value)) => try {
        val intValue = value.toInt
        if (intValue >= 0 && intValue <= 65535) Success(intValue)
        else Failure(CommandError.InvalidPortValue(value))
      } catch {
        case _: NumberFormatException => Failure(CommandError.InvalidPortValue(value))
      }
    }
  }

  def locateController(controller: DeviceManager): Future[Uri] = {
    controller.listTasks.find{ case (_, descriptor, _) => descriptor == DeviceDescriptor.GC }.map{ case (_, _, uri) => uri } match {
      case Some(uri) => Future.successful(uri)
      case None => Future.failed(CommandError.NoController)
    }
  }

  def listDevicesOnController(context: CommandContext, controllerURI: Uri): Future[Seq[Device]] = {
    implicit val system: ActorSystem = context.actorSystem
    implicit val ec: ExecutionContextExecutor = context.actorSystem.dispatcher
    implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)

    DeviceClient(controllerURI.toString()).listDevices().value.map {
      case Right(OK(response)) => response
      case other => throw new IllegalStateException(s"unexpected response: $other")
    }
  }

  /**
   * Locates a device on the server by name and returns its uri on the server.
   *
   * The resulting URI includes the "/v1/devices/<ID>" path such that doing
   * a GET on this URI will return the device information.
   *
   * @param context the [[CommandContext]] to run the method
   * @param name  the human readable name of the device
   * @return the [[Uri]] of the device on the server.
   */
  def locateDeviceByName(context: CommandContext, name: String): Future[Uri] = {
    implicit val ec: ExecutionContextExecutor = context.actorSystem.dispatcher
    val findFuture = for {
      controllerURI <- locateController(context.deviceManager)
      devices <- listDevicesOnController(context, controllerURI)
    } yield devices.find(device => device.name == name).map(device => controllerURI.withPath(Uri.Path(s"/v1/devices/${device.id}")))
    findFuture.transform ({
      case Success(Some(uri)) => Success(uri)
      case Success(None) => Failure(CommandError.NotFound(name))
      case Failure(error) => Failure(error)
    })
  }

  /**
   * Parses the target as a URI or a named path like "name/path".
   *
   * The path can be empty and be just a name or it can contain additional path
   * components that selected entities under "/v1/devices/<ID>/... on the controller.
   *
   * @param context the [[CommandContext]] to run the method
   * @param target the string to parse
   * @return the [[Uri]] of the selected entity on the server.
   */
  def getURI(context: CommandContext, target: String): Future[Uri] = {
    Try(Uri.parseAbsolute(target)) match {
      case Success(uri) => Future.successful(uri)
      case Failure(_) => getURIByName(context, target)
    }
  }

  /**
   * Parses the target named path like "name/path".
   *
   * The path can be empty and be just a name or it can contain additional path
   * components that selected entities under "/v1/devices/<ID>/... on the controller.
   *
   * @param context the [[CommandContext]] to run the method
   * @param target the string to parse
   * @return the [[Uri]] of the selected entity on the server.
   */
  def getURIByName(context: CommandContext, target: String): Future[Uri] = {
    implicit val ec: ExecutionContextExecutor = context.actorSystem.dispatcher
    val parts = target.split("/")
    val name = parts.head
    val pathRemainder = Uri.Path(if (parts.tail.isEmpty) "" else parts.tail.mkString("/", "/", ""))
    CommandUtilities.locateDeviceByName(context, name).map(uri => uri.withPath(uri.path ++ pathRemainder))
  }

  /**
   * @return a port on the local host that is not in use
   */
  def getUnusedPort: Int = {
    val socket = new ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    port
  }
}
