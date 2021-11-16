package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.command.Command.parseErrors
import org.opendcgrid.lib.commandoption.CommandOptionResult

import scala.util.{Success, Try}


case object DevicesCommand extends Command with Parsable {
  val name = "devices"
  val help = "devices - display the devices"

  def run(context: CommandContext): Try[CommandResponse] = {
    implicit val ordering: Ordering[CommandResponse.TaskResponse] = Ordering.by(_.id)
    val tasks = context.taskManager.listTasks
    val responses = tasks.map{ case (id, task) => CommandResponse.TaskResponse(task.name, id, task.uri)}.toSeq.sorted
    Success(CommandResponse.MultiResponse(responses))
  }

  override def parse(arguments: Seq[String]): Try[Command] = {
    val options = Nil
    val result = CommandOptionResult.parse(arguments, options)
    for {
      _ <- parseErrors(result) // Bail out if any errors in find
    } yield DevicesCommand
  }
}
