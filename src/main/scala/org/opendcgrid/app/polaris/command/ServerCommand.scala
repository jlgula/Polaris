package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.server.{PolarisServer, ServerError}

import scala.concurrent.{Await, TimeoutException}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}


case object ServerCommand extends Command with Parsable {
  val name = "server"
  val help = "server - start the server"
  val url: Uri = Uri("http://localhost:8080")

  def run(context: CommandContext): Try[CommandResponse] = {
    val taskName = name
    implicit def actorSystem: ActorSystem = ActorSystem()
    val server = new PolarisServer(url, taskName, context.taskManager)
    try {
      val taskID = Await.result(server.start(), Duration.Inf)
      Success(CommandResponse.TaskResponse(taskName, taskID, url.toString()))
    } catch {
      case _: TimeoutException => Failure(CommandError.ServerError(ServerError.Timeout.getMessage))
      case _: InterruptedException => Failure(CommandError.ServerError(ServerError.Interrupted.getMessage))
    }
  }

  override def parse(arguments: Seq[String]): Try[Command] = {
    if (arguments.isEmpty) Success(ServerCommand)
    else Failure(CommandError.MultiError(arguments.map(argument => CommandError.InvalidOption(argument))))
  }
}
