package org.opendcgrid.app.polaris.command

sealed abstract class CommandResponse(val message: String) {
  override def toString: String = message
}

object CommandResponse {
  case object NullResponse extends CommandResponse("")

  case class ExitResponse(exitCode: Int = 0) extends CommandResponse("")

  case class TextResponse(text: String) extends CommandResponse(text)

  case class MultiResponse(values: Seq[CommandResponse]) extends CommandResponse(values.mkString("\n"))

  case class VersionResponse(version: String) extends CommandResponse(version)
}
