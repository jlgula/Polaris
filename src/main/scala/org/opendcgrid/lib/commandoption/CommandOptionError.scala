package org.opendcgrid.lib.commandoption

sealed abstract class CommandOptionError(val message: String, val exitCode: Int = 1) extends Throwable(message)
object CommandOptionError {
  case class MultiError(errors: Seq[CommandOptionError]) extends CommandOptionError(s"Errors: ${errors.mkString(",")}.")
  case class UnrecognizedOption(optionName: String) extends CommandOptionError(s"Unrecognized option: ${q(optionName)}")
  case class MissingOptionArgument(optionName: String) extends CommandOptionError(s"Option $optionName: missing argument")

  def q(value: String): String = "\"" + value + "\""
}