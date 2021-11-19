package org.opendcgrid.app.polaris.command

import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.device.DeviceDescriptor

sealed abstract class CommandResponse(val message: String) {
  override def toString: String = message
}

object CommandResponse {
  case object NullResponse extends CommandResponse("")

  case class ExitResponse(exitCode: Int = 0) extends CommandResponse("")

  case class TextResponse(text: String) extends CommandResponse(text)

  case class MultiResponse(values: Seq[CommandResponse]) extends CommandResponse(values.mkString("\n"))

  case class VersionResponse(version: String) extends CommandResponse(version)

  case class TaskResponse(name: String, descriptor: DeviceDescriptor, uri: Uri) extends CommandResponse(s"$name $descriptor $uri")

  case class HaltResponse(name: String) extends CommandResponse(s"Device halted: $name")

}
