package org.opendcgrid.app.polaris.command

import scala.util.{Failure, Success, Try}


abstract class Command {
  def run(context: CommandContext): Try[CommandResponse]
}
