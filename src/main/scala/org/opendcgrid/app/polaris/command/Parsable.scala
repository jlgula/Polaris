package org.opendcgrid.app.polaris.command

import scala.util.Try

// The Command objects must implement this trait.
trait Parsable {
  def name: String // Name of the command
  def help: String // HelpCommand string for command
  def parse(arguments: Seq[String]): Try[Command]
}
