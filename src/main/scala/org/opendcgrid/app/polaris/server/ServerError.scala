package org.opendcgrid.app.polaris.server

import akka.http.scaladsl.model.Uri

sealed abstract class ServerError(val message: String) extends Throwable(message)
object ServerError {
  case object Timeout extends ServerError("timeout")
  case object Interrupted extends ServerError("timeout")
  case object NotStarted extends ServerError("server not started")
  case class BindingError(details: String) extends ServerError(s"binding failed: $details")
  case class NotFound(name: String) extends ServerError(s"Not found $name")
  case class DuplicateName(name: String) extends ServerError(s"Name already in use: $name")
  case class DuplicateUri(uri: Uri) extends ServerError(s"URI already in use: $uri")
}