package org.opendcgrid.app.polaris.command

import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.PolarisAppOption
import org.opendcgrid.app.polaris.device.{DeviceDescriptor, DeviceManager}
import org.opendcgrid.lib.commandoption.CommandOptionResult

import scala.concurrent.Future
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

}
