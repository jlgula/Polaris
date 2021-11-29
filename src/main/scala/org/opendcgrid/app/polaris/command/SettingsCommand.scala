package org.opendcgrid.app.polaris.command

import com.typesafe.config.{Config, ConfigException}
import org.opendcgrid.app.polaris.{PolarisAppOption, PolarisAppOptionTag}
import org.opendcgrid.app.polaris.command.Command.parseErrors
import org.opendcgrid.lib.commandoption.CommandOptionResult

import scala.jdk.CollectionConverters._


import scala.util.{Failure, Success, Try}

object SettingsCommand extends Parsable {
  val name = "settings"
  val help = "settings <key>... - display configuration settings"

  override def parse(arguments: Seq[String]): Try[Command] = {
    val options = Seq(PolarisAppOptionTag.Origin) //Seq[PolarisAppOptionTag](Nil)
    val result = CommandOptionResult.parse(arguments, options)
    for {
      _ <- parseErrors(result) // Bail out if any errors in find
    } yield SettingsCommand(result.values, result.options.contains(PolarisAppOption.Origin))
  }


}

case class SettingsCommand(requestedPaths: Seq[String], showOrigin: Boolean) extends Command {
  case class SettingsResult()
  def run(context: CommandContext): Try[CommandResponse] = {
    val config = context.actorSystem.settings.config
    val paths = if (requestedPaths.isEmpty) config.entrySet().asScala.map(_.getKey).toSeq else requestedPaths
    val results = paths.map(path => getPath(config, path, showOrigin))
    val errors = results.collect{ case Failure(error: CommandError) => error }
    if (errors.isEmpty) Success(CommandResponse.MultiResponse(results.collect{ case Success(response) => response }))
    else Failure(CommandError.MultiError(errors))
  }

  private def getPath(config: Config, path: String, showOrigin: Boolean): Try[CommandResponse.SettingsResponse] = {
    try {
      if (config.hasPathOrNull(path)) {
        if (config.getIsNull(path)) Success(CommandResponse.SettingsResponse(path, ""))
        else {
          val origin = if (showOrigin) {
            config.getAnyRef(path) match {
              case c: Config => Some(c.origin.description())
              case _ => None
            }
          } else None
          Success(CommandResponse.SettingsResponse(path, config.getAnyRef(path).toString, origin))
        }
      } else Failure(CommandError.MissingSettingsPath(path))
    } catch {
      case _: ConfigException.BadPath => Failure(CommandError.InvalidSettingsPath(path))
    }
  }
}

