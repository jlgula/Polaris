package org.opendcgrid.app.polaris.command

import org.opendcgrid.lib.commandoption.{CommandOptionResult, StandardCommandOption}

import scala.util.{Failure, Success, Try}

object Command {
  /*
  def parseURL(value: Option[String]): Try[Url] = value match {
    case None => Success(Url.parse(Shell.rootURL)) // TODO: replace with relativeRoot?
    case Some(urlText) =>
      Url.parseTry(urlText) match {
        case Success(url) => Success(url)
        case Failure(error) => Failure(CommandError.InvalidURL(urlText, error))
      }
  }



  def parseAccept(result: CommandOptionResult): Try[MediaRange] = {
    result.options.collectFirst { case option: ODGAppOption.Accept => option } match {
      case None => Success(http.MediaRange(MediaType.ApplicationJSON))
      case Some(ODGAppOption.Accept(range)) => MediaRange.parseMediaRange(range)
    }
  }

  def parseContentType(result: CommandOptionResult): Try[MediaType] = {
    result match {
      case ODGAppOption.ContentType(value) => MediaType.parse(value.mediaType)
      case _ => Success(MediaType.ApplicationJSON)
    }
  }

   */

  def parseVerbose(result: CommandOptionResult): Try[Boolean] = Success(result.options.contains(StandardCommandOption.Verbose))

  def parseErrors(result: CommandOptionResult): Try[Unit] = result.errors.size match {
    case 0 => Success(())
    case 1 => Failure(CommandError.InvalidOption(result.errors.head.message))
    case _ => Failure(CommandError.MultiError(result.errors.map(error => CommandError.InvalidOption(error.message))))
  }

}

abstract class Command {
  def run(context: CommandContext): Try[CommandResponse]
}
