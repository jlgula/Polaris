package org.opendcgrid.app.polaris.command

import scala.util.{Failure, Success, Try}


object HelpCommand extends Parsable {
  val name = "help"
  val help = "help [<commandName>] - list all commands"

  def parse(arguments: Seq[String]): Try[Command] = Success(HelpCommand(arguments))
}

case class HelpCommand(names: Seq[String]) extends Command {
  def run(context: CommandContext): Try[CommandResponse] = {
    val nameList = if (names.isEmpty) context.allCommands.map(_.name).sorted else names
    val helpTries = nameList.map { name =>
      context.allCommands.find(_.name == name) match {
        case None => Failure(CommandError.InvalidCommand(name))
        case Some(command) => Success(command.help)
      }
    }

    val errors = helpTries.collect { case Failure(e: CommandError) => e }
    if (errors.isEmpty) Success(CommandResponse.MultiResponse(helpTries.map(h => CommandResponse.TextResponse(h.get))))
    else Failure(CommandError.MultiError(errors))
  }
}

