package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.BuildInfo
import org.opendcgrid.lib.commandoption.CommandOptionResult

import scala.util.{Failure, Success, Try}


case object VersionCommand extends Parsable {
  val name = "version"
  val help = "version - display the release version of the application"

  def parse(arguments: Seq[String]): Try[Command] = {
    import org.opendcgrid.lib.commandoption.StandardCommandOptionTag.Verbose
    CommandOptionResult.parse(arguments, Seq(Verbose)) match {
      case CommandOptionResult(_, _, errors) if errors.nonEmpty => Failure(CommandError.MultiError(errors.map(e => CommandError.InvalidOption(e.message))))
      case CommandOptionResult(values, _, _) if values.nonEmpty => Failure(CommandError.UnexpectedParameters(values))
      case Verbose(_) => Success(VersionCommand(true))
      case _ => Success(VersionCommand())
    }
  }
}

case class VersionCommand(verbose: Boolean = false) extends Command {
  def run(context: CommandContext): Try[CommandResponse] = {
    val text = if (verbose) BuildInfo.toString else BuildInfo.version
    Success(CommandResponse.TextResponse(text))
  }
}
