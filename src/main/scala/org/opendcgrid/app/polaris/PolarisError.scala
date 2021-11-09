package org.opendcgrid.app.polaris


sealed abstract class PolarisError(val message: String) extends Throwable(message)
object PolarisError {
  case class MultiError(errors: Seq[PolarisError]) extends PolarisError(s"Multiple errors: ${errors.map(_.toString).mkString(",")}")
  case class NotFound(url: String) extends PolarisError(s"Not found: $url")
}
