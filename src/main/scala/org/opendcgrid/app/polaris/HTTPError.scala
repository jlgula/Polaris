package org.opendcgrid.app.polaris

import java.io.IOException


sealed abstract class HTTPError(val message: String) extends Throwable(message)
object HTTPError {
  case class BodyTooShort(expected: Int, actual: Int) extends HTTPError(s"Body too short. Expected: $expected. Actual: $actual")
  case object Closed extends HTTPError(s"IO error: reader is closed")
  case object EOF extends HTTPError(s"IO error: unexpected End of File")
  case class HeaderMissingColon(line: String) extends HTTPError(s"No colon separator in header: $line")
  case class InvalidAccept(value: String, details: Seq[Throwable]) extends HTTPError(s"Invalid accept header: $value")
  case class InvalidChunkHeading(value: String) extends HTTPError(s"Invalid chunk heading: $value")
  case class InvalidCommandLine(line: String) extends HTTPError(s"Invalid command line: $line")
  case class InvalidConnectionParameter(parameter: String) extends HTTPError(s"Invalid connection parameter: $parameter")
  case class InvalidContentLength(value: String) extends HTTPError(s"Invalid content length: $value")
  case class InvalidMediaRange(value: String) extends HTTPError(s"Invalid media range: $value")
  case class InvalidMediaRangeParameter(value: String) extends HTTPError(s"Invalid media range parameter: $value")
  case class InvalidMethod(method: String) extends HTTPError(s"Invalid method: $method")
  case class InvalidQualityValue(value: String) extends HTTPError(s"Invalid quality: $value")
  case class InvalidResponse(details: Throwable) extends HTTPError(s"Invalid response. ${details.getMessage}.")
  case class InvalidStatusCode(code: String) extends HTTPError(s"Invalid status code: $code")
  case class InvalidStatusLine(line: String) extends HTTPError(s"Invalid status line: $line")
  case class InvalidTransferEncoding(encoding: String) extends HTTPError(s"Invalid transfer encoding: $encoding")
  case class InvalidURL(url: String) extends HTTPError(s"Invalid url: $url")
  case class InvalidVersion(version: String) extends HTTPError(s"Invalid version: $version")
  case class IOError(error: IOException) extends HTTPError(s"IO error: ${error.getMessage}")
  case class MalformedHeadName(name: String) extends HTTPError(s"Malformed header name: $name")
  case class MalformedMediaType(value: String) extends HTTPError(s"Malformed media type: $value")
  case object MissingCommandLine extends HTTPError(s"No command line in request")
  case object MissingMediaType extends HTTPError("No media type in request")
  case object MissingMessageTerminator extends HTTPError(s"Final \r\n terminator not found")
  case object MissingStatusLine extends HTTPError(s"No status line in request")
  case class MultiError(errors: Seq[HTTPError]) extends HTTPError(s"Multiple errors: ${errors.map(_.toString).mkString(",")}")
  case class NotFound(url: String) extends HTTPError(s"Not found: $url")
  case object SocketClosed extends HTTPError(s"IO error: port closed")
  case object ReadTimeout extends HTTPError(s"IO error: read timeout")
  case class UnsupportedEncodingFormat(format: String) extends HTTPError(s"Unsupported encoding format: $format")
  case object UnsupportedMessageFormat extends HTTPError(s"Unsupported message format")
}