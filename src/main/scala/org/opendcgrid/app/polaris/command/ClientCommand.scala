package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.{PolarisAppOption, PolarisAppOptionTag}
import org.opendcgrid.app.polaris.command.Command.parseErrors
import org.opendcgrid.app.polaris.command.CommandUtilities.parsePort
import org.opendcgrid.app.polaris.device.DeviceDescriptor
import org.opendcgrid.app.polaris.server.ServerError
import org.opendcgrid.lib.commandoption.CommandOptionResult

import java.net.BindException
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, TimeoutException}
import scala.util.{Failure, Success, Try}

case object ClientCommand extends Parsable {
  val name = "client"
  val help = "client - start a client device"
  val defaultPort: Int = 0


  override def parse(arguments: Seq[String]): Try[Command] = {
    val options = Seq(PolarisAppOptionTag.Port)
    val result = CommandOptionResult.parse(arguments, options)
    for {
      _ <- parseErrors(result) // Bail out if any errors in find
      port <- parsePort(result, defaultPort)
    } yield ClientCommand(port)
  }
}

case class ClientCommand(port: Int = 0) extends Command {
  val uri: Uri = Uri("http://localhost").withPort(port)
  def run(context: CommandContext): Try[CommandResponse.DeviceResponse] = {
    implicit def actorSystem: ActorSystem = context.actorSystem
    val nameFuture = context.taskManager.startTask(DeviceDescriptor.Client, None, uri)
    Try(Await.ready(nameFuture, Duration.Inf)) match {
      case Success(f) => f.value.get match {
        case Success(name) => Success(CommandResponse.DeviceResponse(name, DeviceDescriptor.GC, uri))
        case Failure(_: TimeoutException) => Failure(CommandError.ServerError(ServerError.Timeout))
        case Failure(_: InterruptedException) => Failure(CommandError.ServerError(ServerError.Interrupted))
        case Failure(error) if error.getCause.isInstanceOf[BindException] => Failure(CommandError.ServerError(ServerError.BindingError(error.getCause.getMessage)))
        case Failure(error: ServerError.DuplicateUri) => Failure(CommandError.ServerError(error))
        case Failure(error) =>
          println(error)
          throw new IllegalStateException(s"unexpected server error: $error")
      }
      case Failure(_) => Failure(CommandError.ServerError(ServerError.Timeout))
    }
  }
}
