package org.opendcgrid.app.polaris.device

import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.server.definitions.Notification

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
  case class UnexpectedResponse(response: String) extends DeviceError(s"Unexpected response: $response")
  case class InvalidNotification(notification: Notification, details: String) extends DeviceError(s"Invalid notification: $notification. Details: $details")
  case class NotificationFailed(notification: Notification, details: String) extends DeviceError(s"Notification failed: $notification. Details: $details")
}