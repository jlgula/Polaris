package org.opendcgrid.app.polaris.device

import akka.http.scaladsl.model.Uri

sealed abstract class DeviceError(val message: String) extends Throwable(message)
object DeviceError {
  case object Timeout extends DeviceError("timeout")
  case object Interrupted extends DeviceError("timeout")
  case object NotStarted extends DeviceError("server not started")
  case class BindingError(details: String) extends DeviceError(s"binding failed: $details")
  case class NotFound(name: String) extends DeviceError(s"Not found $name")
  case class DuplicateName(name: String) extends DeviceError(s"Name already in use: $name")
  case class DuplicateUri(uri: Uri) extends DeviceError(s"URI already in use: $uri")
  case class InvalidPowerValue(value: BigDecimal, details: String) extends DeviceError(s"Invalid power value: $value details: $details")
}