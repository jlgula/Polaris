package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.PolarisAppOptionTag
import org.opendcgrid.app.polaris.command.Command.parseErrors
import org.opendcgrid.app.polaris.server.ServerError
import org.opendcgrid.lib.commandoption.CommandOptionResult
import org.opendcgrid.lib.task.TaskID

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case object HaltCommand extends Parsable {
  val name = "halt"
  val help = "halt <deviceID>... - halt a device"

  override def parse(arguments: Seq[String]): Try[Command] = {
    val options = Seq(PolarisAppOptionTag.Port)
    val result = CommandOptionResult.parse(arguments, options)
    for {
      _ <- parseErrors(result) // Bail out if any errors in find
      _ <- if (result.values.isEmpty) Failure(CommandError.MissingArgument("deviceID")) else Success(())
      tasks <- parseDevices(result.values)
    } yield HaltCommand(tasks: _*)
  }

  def parseDevices(values: Seq[String]): Try[Seq[TaskID]] = {
    val deviceTries = values.map(parseDevice)
    val failures = deviceTries.filter(_.isFailure)
    if (failures.isEmpty) Success(deviceTries.map(_.get))
    else Failure(CommandError.MultiError(failures.collect{ case Failure(e: CommandError) => e}))
  }

  def parseDevice(value: String): Try[TaskID] = {
    TaskID.parse(value).recoverWith{ case _ => Failure(CommandError.InvalidDevice(value))}
  }
}

case class HaltCommand(devices: TaskID*) extends Command {
  def run(context: CommandContext): Try[CommandResponse] = {
    implicit val ec: ExecutionContext = context.executionContext
    val terminationFutures = devices.map(context.taskManager.terminateTask(_))
    val commandFuture = Future.sequence(terminationFutures)
    Try(Await.result(commandFuture, Duration.Inf)) match {
      case Success(_) => Success(CommandResponse.MultiResponse(devices.map(CommandResponse.HaltResponse)))
      case Failure(e: ServerError) => Failure(CommandError.ServerError(e))
      case Failure(error) => throw new IllegalStateException(s"unexpected error: $error")
    }
  }
}
