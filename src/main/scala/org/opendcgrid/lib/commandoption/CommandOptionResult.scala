package org.opendcgrid.lib.commandoption

import org.opendcgrid.lib.commandoption.CommandOptionError.{MissingOptionArgument, MultiError, UnrecognizedOption}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

case class CommandOptionResult(values: Seq[String] = Nil, options: Seq[CommandOption] = Nil, errors: Seq[CommandOptionError] = Nil)

object CommandOptionResult {
  def parse(arguments: Seq[String], parameters: Seq[CommandOptionTag] = Nil): CommandOptionResult = {
    val options = mutable.ArrayBuffer[CommandOption]()
    val values = mutable.ArrayBuffer[String]()
    val errors = mutable.ArrayBuffer[CommandOptionError]()
    var index: Int = 0

    // Handler for single character options that can be grouped as in "-xyz" == "-x -y -z"
    def processShortOption(text: Character): Unit = {
      findOptionShort(text.toString, parameters) match {
        case Success(tag) => processOption(tag, arguments.slice(index + 1, arguments.size)) match {
          case Failure(e: MultiError) => errors.addAll(e.errors)
          case Failure(e: CommandOptionError) => errors.append(e)
          case Failure(e) => throw new IllegalStateException(s"Unexpected error: $e")
          case Success(option) => options.append(option); index += tag.parameterCount
        }
        case Failure(e: CommandOptionError) => errors.append(e)
        case Failure(e) => throw new IllegalStateException(s"Unexpected error: $e")
      }
    }

    while (index < arguments.size) {
      val next = arguments(index)
      next match {
        case _ if next.startsWith("--") => findOptionName(next.substring(2), parameters) match {
          case Success(tag) => processOption(tag, arguments.slice(index + 1, arguments.size)) match {
            case Failure(e: MultiError) => errors.addAll(e.errors)
            case Failure(e: CommandOptionError) => errors.append(e)
            case Failure(e) => throw new IllegalStateException(s"Unexpected error: $e")
            case Success(option) => options.append(option); index += tag.parameterCount
          }
          case Failure(e: CommandOptionError) => errors.append(e)
          case Failure(e) => throw new IllegalStateException(s"Unexpected error: $e")
        }
        case _ if next.startsWith("-") => next.substring(1).foreach(processShortOption(_))
        case _ => values.append(next)
      }

      index += 1
    }

    CommandOptionResult(values.toSeq, options.toSeq, errors.toSeq)
  }

  def findOptionName(value: String, parameters: Seq[CommandOptionTag]): Try[CommandOptionTag] = {
    parameters.find(p => p.name == value) match {
      case Some(parameter) => Success(parameter)
      case None => Failure(UnrecognizedOption(value))
    }
  }

  def findOptionShort(value: String, parameters: Seq[CommandOptionTag]): Try[CommandOptionTag] = {
    parameters.find(p => p.shortForm.getOrElse("") == value) match {
      case Some(parameter) => Success(parameter)
      case None => Failure(UnrecognizedOption(value))
    }
  }

  def processOption(option: CommandOptionTag, arguments: Seq[String]): Try[CommandOption] = {
    if (option.parameterCount > arguments.size) Failure(MissingOptionArgument(option.name))
    else option.parse(arguments)
  }

}

