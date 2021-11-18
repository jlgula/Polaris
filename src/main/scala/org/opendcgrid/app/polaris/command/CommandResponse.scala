package org.opendcgrid.app.polaris.command

import akka.http.scaladsl.model.Uri
import org.opendcgrid.lib.task.TaskID

sealed abstract class CommandResponse(val message: String) {
  override def toString: String = message
}

object CommandResponse {
  case object NullResponse extends CommandResponse("")

  case class ExitResponse(exitCode: Int = 0) extends CommandResponse("")

  case class TextResponse(text: String) extends CommandResponse(text)

  case class MultiResponse(values: Seq[CommandResponse]) extends CommandResponse(values.mkString("\n"))

  case class VersionResponse(version: String) extends CommandResponse(version)

  case class TaskResponse(name: String, id: TaskID, uri: Uri) extends CommandResponse(s"$name running at $uri as task $id")

  case class HaltResponse(id: TaskID) extends CommandResponse(s"Device halted: $id")

}
