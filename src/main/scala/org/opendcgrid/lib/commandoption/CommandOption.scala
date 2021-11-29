package org.opendcgrid.lib.commandoption

import scala.util.{Success, Try}

abstract class CommandOption {
  //def unapply(result: CommandOptionResult): Option[CommandOption[T]] = result.options.collectFirst{case o: T => o}
}

/**
 * Defines a command line option.
 *
 * Usage:
 * {{{
 *  val result = CommandOptionResult.parse(args, options)
    result match {
      case StandardCommandOption.input(option) => doSomething(option.inputName)
    }

    run(result.values)  // values contains all the arguments not captured by options

 * }}}
 *
 * @param name  the long form of the option as in "--version"
 * @param shortForm the single character form of the option, if any as in "-v"
 * @param parameterCount  the number of parameters expected after the option
 */
abstract class CommandOptionTag(val name: String, val shortForm: Option[String] = None, val parameterCount: Int = 0) {
  def parse(arguments: Seq[String]): Try[CommandOption]
}

abstract class ZeroArgumentTag(val value: CommandOption, override val name: String, override val shortForm: Option[String] = None) extends CommandOptionTag(name, shortForm, 0) {
  def parse(arguments: Seq[String]): Try[CommandOption] = Success(value)

  def unapply(result: CommandOptionResult): Option[CommandOption] = result.options.find(_ == this.value)
}

abstract class OneArgumentTag(val constructor: String => CommandOption, override val name: String, override val shortForm: Option[String] = None) extends CommandOptionTag(name, shortForm, 1) {
  def parse(arguments: Seq[String]): Try[CommandOption] = Success(constructor(arguments.head))
}


object StandardCommandOptionTag {
  case object Input extends OneArgumentTag(StandardCommandOption.Input.apply, "input", Some("i"))
  case object Help extends ZeroArgumentTag(StandardCommandOption.Help, "help", Some("h"))
  case object List extends ZeroArgumentTag(StandardCommandOption.List, "list", Some("l"))
  case object Output extends OneArgumentTag(StandardCommandOption.Output.apply, "output", Some("o"))
  case object Remove extends OneArgumentTag(StandardCommandOption.Output.apply, "remove", Some("r"))
  case object Verbose extends ZeroArgumentTag(StandardCommandOption.Verbose, "verbose", Some("V"))
  case object Version extends ZeroArgumentTag(StandardCommandOption.Version, "version", Some("v"))
}

object StandardCommandOption {
  case object Help extends CommandOption
  case object List extends CommandOption
  case class Input(inputName: String) extends CommandOption
  case class Output(outputName: String) extends CommandOption
  case class Remove(removeName: String) extends CommandOption
  case object Verbose extends CommandOption
  case object Version extends CommandOption
}



