package org.opendcgrid.app.polaris.command

import scala.util.{Failure, Success, Try}


object ExitCommand extends Parsable {
  val name = "exit"
  val help = "exit [<exitCode>] - halt all devices and terminate the application"

  def parse(arguments: Seq[String]): Try[Command] = arguments.size match {
    case 0 => Success(ExitCommand(0))
    case 1 => try {
      Success(ExitCommand(arguments.head.toInt))
    } catch {
      case e: NumberFormatException => Failure(CommandError.InvalidExitCode(arguments.head, e))
    }
    case _ => Failure(CommandError.UnexpectedParameters(arguments.drop(1)))
  }
}

case class ExitCommand(exitCode: Int) extends Command {
  def run(context: CommandContext): Try[CommandResponse] = Success(CommandResponse.ExitResponse(exitCode))
}
