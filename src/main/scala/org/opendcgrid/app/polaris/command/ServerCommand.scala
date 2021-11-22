package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.command.CommandUtilities.parsePort
import org.opendcgrid.app.polaris.command.Command.parseErrors
import org.opendcgrid.app.polaris.device.DeviceDescriptor
import org.opendcgrid.app.polaris.server.ServerError
import org.opendcgrid.app.polaris.{PolarisAppOption, PolarisAppOptionTag}
import org.opendcgrid.lib.commandoption.CommandOptionResult

import java.net.BindException
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, TimeoutException}
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
      port <- parsePort(result, defaultPort)
    } yield ServerCommand(port)
  }
}

case class ServerCommand(port: Int = 8080) extends Command {
  val uri: Uri = Uri("http://localhost").withPort(port)
  def run(context: CommandContext): Try[CommandResponse.DeviceResponse] = {
    implicit def actorSystem: ActorSystem = context.actorSystem
    val nameFuture = context.deviceManager.startTask(DeviceDescriptor.GC, None, uri)
    Try(Await.ready(nameFuture, Duration.Inf)) match {
      case Success(f) => f.value.get match {
        case Success(binding) => Success(CommandResponse.DeviceResponse(binding.name, DeviceDescriptor.GC, binding.uri))
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
