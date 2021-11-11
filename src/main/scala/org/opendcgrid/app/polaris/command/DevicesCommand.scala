package org.opendcgrid.app.polaris.command

import scala.util.Try


case object DevicesCommand extends Command with Parsable {
  val name = "devices"
  val help = "devices - display the devices"

  def run(context: CommandContext): Try[CommandResponse] = {
    throw new UnsupportedOperationException("Not yet")
  }

  override def parse(arguments: Seq[String]): Try[Command] = throw new IllegalStateException("DevicesCommand - parse unsupported")
}
