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

  case class DeviceResponse(name: String, descriptor: DeviceDescriptor, uri: Uri) extends CommandResponse(s"$name running at: $uri")

  case class HaltResponse(name: String) extends CommandResponse(s"Device halted: $name")

  case class SettingsResponse(path: String, value: String, origin: Option[String] = None) extends CommandResponse(s"$path:$value${origin.map(o => s"@$o").getOrElse("")}")
}
