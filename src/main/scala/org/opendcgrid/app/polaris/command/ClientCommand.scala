package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.PolarisAppOptionTag
import org.opendcgrid.app.polaris.command.Command.parseErrors
import org.opendcgrid.app.polaris.command.CommandUtilities.parsePort
import org.opendcgrid.app.polaris.device.{DeviceDescriptor, DeviceError}
import org.opendcgrid.lib.commandoption.CommandOptionResult

import java.net.BindException
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, TimeoutException}
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
    implicit def ec: ExecutionContext = actorSystem.dispatcher
    val binding = for {
      controllerURI <- context.locateController
      binding <- context.deviceManager.startDevice(DeviceDescriptor.Client, None, uri, Some(controllerURI))
    } yield binding
    //val nameFuture = context.taskManager.startTask(DeviceDescriptor.Client, None, uri)
    Try(Await.ready(binding, Duration.Inf)) match {
      case Success(f) => f.value.get match {
        case Success(binding) => Success(CommandResponse.DeviceResponse(binding.name, DeviceDescriptor.GC, binding.uri))
        case Failure(_: TimeoutException) => Failure(CommandError.ServerError(DeviceError.Timeout))
        case Failure(_: InterruptedException) => Failure(CommandError.ServerError(DeviceError.Interrupted))
        case Failure(error) if error.getCause.isInstanceOf[BindException] => Failure(CommandError.ServerError(DeviceError.BindingError(error.getCause.getMessage)))
        case Failure(error: DeviceError.DuplicateUri) => Failure(CommandError.ServerError(error))
        case Failure(CommandError.NoController) => Failure(CommandError.NoController)
        case Failure(error) =>
          println(error)
          throw new IllegalStateException(s"unexpected server error: $error")
      }
      case Failure(_) => Failure(CommandError.ServerError(DeviceError.Timeout))
    }
  }
}
