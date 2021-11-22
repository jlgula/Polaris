package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.device.DeviceError

import java.io.IOException

sealed abstract class CommandError(val message: String, val exitCode: Int = 1) extends Throwable(message)
object CommandError {
  case class FileEmpty(fileName: String) extends CommandError(s"$fileName: file has no content")
  case class FileFormatError(fileName: String, details: String) extends CommandError(s"$fileName: invalid format. $details")
  case class FileNotFound(fileName: String) extends CommandError(s"$fileName: file not found")
  case class FileIOError(fileName: String, details: String) extends CommandError(s"$fileName: IO error: $details")
  case class HTTPRequestFailed(details: String) extends CommandError(s"Request failed. $details")
  case class HTTPErrorResponse(code: Int, reason: Option[String]) extends CommandError(s"Request failed with status: $code. ${reason.getOrElse("No reason provided")}")
  case class InternalError(details: Throwable) extends CommandError(s"Unexpected internal error. ${details.getMessage}")
  case class InvalidCommand(name: String) extends CommandError(s"Command not recognized: $name")
  case class InvalidDevice(deviceID: String) extends CommandError(s"Invalid device identifier: ${q(deviceID)}")
  case class InvalidExitCode(value: String, details: Throwable) extends CommandError(s"Invalid exit code $value. ${details.getMessage}")
  case class InvalidJSONValue(value: String, details: String) extends CommandError(s"Invalid JSON code: ${q(value)}. $details")
  case class InvalidOption(value: String) extends CommandError(s"Invalid option: $value")
  case class InvalidParameter(value: String) extends CommandError(s"Invalid parameter: $value")
  case class InvalidParameterArgument(parameterName: String, argument: String) extends CommandError(s"parameter $parameterName: invalid argument $argument")
  case class InvalidPort(port: Int) extends CommandError(s"Invalid port: $port")
  case class InvalidPortValue(value: String) extends CommandError(s"Invalid port code: ${q(value)}")
  case class InvalidProtocol(value: String) extends CommandError(s"Invalid protocol: $value.")
  case class InvalidURL(value: String, details: Throwable) extends CommandError(s"Invalid url: $value. ${details.getMessage}")
  case class MissingArgument(argumentName: String) extends CommandError(s"Argument $argumentName is required")
  case object MissingHost extends CommandError("Host missing in URL")
  case class MultiError(errors: Seq[CommandError]) extends CommandError(errors.map(_.message).mkString(","))
  case object NonLocalHost extends CommandError(s"Servers can only be started on the local host")
  case object NotAttached extends CommandError(s"Not attached. Use full URL")
  case class NoPermission(thing: String) extends CommandError(s"The user does not have permission to access: $thing")
  case object NoController extends CommandError(s"Controller could not be located")
  case class NotFound(thing: String) extends CommandError(s"Not found: $thing")
  case class PortInUse(port: Int) extends CommandError(s"Port number: $port is already in use")
  case class RequestFailed(details: Throwable) extends CommandError(s"Request failed. ${details.getMessage}")
  case class ServerError(details: DeviceError) extends CommandError(s"Server error: ${details.getMessage}")
  case class SocketIOError(socketUrl: String, error: IOException) extends CommandError(s"IOException on socket $socketUrl: ${error.getMessage}")
  case class TerminationError(details: String) extends CommandError(s"Error terminating devices: $details")
  case class TracedError(error: CommandError, trace: Seq[String]) extends CommandError(error.getMessage)
  case class UnexpectedParameters(parameters: Seq[String]) extends CommandError(s"Unexpected parameters: ${parameters.mkString(",")}")
  case class UnexpectedResponse(response: String) extends CommandError(s"Unexpected response $response")
  case object UnexpectedCreatedResponse extends CommandError(s"Created response did not contain location header")
  case class UnsupportedFileType(fileName: String) extends CommandError(s"File not supported or not recognized: $fileName")
  case class UnsupportedMediaType(mediaType: String) extends CommandError(s"Unsupported media type: $mediaType")
  case class UnsupportedOperation(operation: String) extends CommandError(s"Requested operation is not supported on the platform: $operation")
  case class UnsupportedOption(option: String) extends CommandError(s"Unsupported option: $option")
  case class UnsupportedProtocol(protocol: String) extends CommandError(s"Protocol not supported: $protocol")
  case object Usage extends CommandError("usage - polaris [targetURL | configFile]")

  def q(value: String): String = "\"" + value + "\""
}
