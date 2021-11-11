package org.opendcgrid.app.polaris.command

trait CommandContext {
  def allCommands: Seq[Parsable]

}
