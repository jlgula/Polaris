package org.opendcgrid.app.polaris.server

import org.opendcgrid.lib.task.TaskID

sealed abstract class ServerError(val message: String) extends Throwable(message)
object ServerError {
  case object Timeout extends ServerError("timeout")
  case object Interrupted extends ServerError("timeout")
  case object NotStarted extends ServerError("server not started")
  case class BindingError(details: String) extends ServerError(s"binding failed: $details")
  case class NotFound(taskID: TaskID) extends ServerError(s"Not found - identifier: $taskID")
}