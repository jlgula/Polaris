package org.opendcgrid.app.polaris.shell

import akka.actor.ActorSystem
import org.opendcgrid.app.polaris.AppContext
import org.opendcgrid.app.polaris.command.{CommandContext, CommandError, ExitCommand, HelpCommand, Parsable, VersionCommand }
import org.opendcgrid.lib.task.TaskManager

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Try}

trait ShellContext extends AppContext with CommandContext {
  def configuration: ShellConfiguration
}

class GenericShellContext(val configuration: ShellConfiguration = ShellConfiguration()) extends ShellContext {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  override def writeFile(fileName: String, data: Array[Byte]): Try[Unit] = Failure(CommandError.UnsupportedOperation("file write"))

  override def readFile(fileName: String): Try[Array[Byte]] = Failure(CommandError.UnsupportedOperation("file read"))

  override def allCommands: Seq[Parsable] = Seq[Parsable](
    ExitCommand,  // These are used in basic shell tests.
    HelpCommand,
    VersionCommand
  )

  override def taskManager: TaskManager = new TaskManager
}
