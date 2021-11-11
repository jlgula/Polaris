package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.server.PolarisServer

import scala.util.{Failure, Success, Try}


case object ServerCommand extends Command with Parsable {
  val name = "server"
  val help = "server - start the server"

  def run(context: CommandContext): Try[CommandResponse] = {
    val url = Uri("http://localhost:8080")
    implicit def actorSystem: ActorSystem = ActorSystem()
    val server = new PolarisServer(url)
    server.start() match {
      case Success(_) => Success(CommandResponse.NullResponse)
      case Failure(error) => Failure(CommandError.ServerError(error.getMessage))
    }
  }

  override def parse(arguments: Seq[String]): Try[Command] = {
    if (arguments.isEmpty) Success(ServerCommand)
    else Failure(CommandError.MultiError(arguments.map(argument => CommandError.InvalidOption(argument))))
  }
}
