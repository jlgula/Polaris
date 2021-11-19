package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.{PolarisAppOption, PolarisAppOptionTag}
import org.opendcgrid.app.polaris.command.Command.parseErrors
import org.opendcgrid.app.polaris.command.ServerCommand.name
import org.opendcgrid.app.polaris.server.{PolarisServer, ServerError}
import org.opendcgrid.lib.commandoption.CommandOptionResult

import java.net.BindException
import scala.concurrent.{Await, TimeoutException}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}


case object ServerCommand extends Parsable {
  val name = "server"
  val help = "server - start the server"
  val defaultPort: Int = 8080


  override def parse(arguments: Seq[String]): Try[Command] = {
    /*
    if (arguments.isEmpty) Success(ServerCommand)
    else Failure(CommandError.MultiError(arguments.map(argument => CommandError.InvalidOption(argument))))

     */
    val options = Seq(PolarisAppOptionTag.Port)
    val result = CommandOptionResult.parse(arguments, options)
    for {
      _ <- parseErrors(result) // Bail out if any errors in find
      port <- parsePort(result)
    } yield ServerCommand(port)

  }

  def parsePort(result: CommandOptionResult): Try[Int] = {
    result.options.collectFirst { case port: PolarisAppOption.Port => port } match {
      case None => Success(ServerCommand.defaultPort)
      case Some(PolarisAppOption.Port(value)) => try {
        val intValue = value.toInt
        if (intValue >= 0 && intValue <= 65535) Success(intValue)
        else Failure(CommandError.InvalidPortValue(value))
      } catch {
        case _: NumberFormatException => Failure(CommandError.InvalidPortValue(value))
      }
    }
  }
}

case class ServerCommand(port: Int = 8080) extends Command {
  val uri: Uri = Uri("http://localhost").withPort(port)
  def run(context: CommandContext): Try[CommandResponse] = {
    val taskName = name
    implicit def actorSystem: ActorSystem = ActorSystem()
    val server = new PolarisServer(uri, taskName, context.taskManager)
    try {
      val taskID = Await.result(server.start(), Duration.Inf)
      Success(CommandResponse.TaskResponse(taskName, taskID, uri))
    } catch {
      case _: TimeoutException => Failure(CommandError.ServerError(ServerError.Timeout))
      case _: InterruptedException => Failure(CommandError.ServerError(ServerError.Interrupted))
      //case e: BindException => Failure(CommandError.ServerError(ServerError.BindingError(e.getMessage)))
      // akka errors are gross..
      case error: Throwable if error.getCause.isInstanceOf[BindException] => Failure(CommandError.ServerError(ServerError.BindingError(error.getCause.getMessage)))
      case error: Throwable =>
        println(error)
        throw new IllegalStateException(s"unexpected server error: $error")
    }
  }

}
