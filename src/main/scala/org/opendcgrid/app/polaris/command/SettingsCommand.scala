package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.command.Command.parseErrors
import org.opendcgrid.app.polaris.command.GetCommand.parseTarget
import org.opendcgrid.lib.commandoption.CommandOptionResult

import scala.util.{Failure, Success, Try}

object SettingsCommand extends Parsable {
  val name = "settings"
  val help = "settings - display configuration settings"

  override def parse(arguments: Seq[String]): Try[Command] = {
    val options = Nil //Seq[PolarisAppOptionTag](Nil)
    val result = CommandOptionResult.parse(arguments, options)
    for {
      _ <- parseErrors(result) // Bail out if any errors in find
    } yield SettingsCommand(result.values)
  }

}

case class SettingsCommand(names: Seq[String]) extends Command {
  def run(context: CommandContext): Try[CommandResponse.TextResponse] = {
    val settings = context.actorSystem.settings
    Success(CommandResponse.TextResponse(settings.toString()))
  }
}

