package org.opendcgrid.app.polaris.command

import scala.util.{Success, Try}


// Empty command does nothing
case object NullCommand extends Command {
  def run(context: CommandContext): Try[CommandResponse] = Success(CommandResponse.NullResponse)
}
