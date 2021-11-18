package org.opendcgrid.app.polaris.shell

import akka.actor.ActorSystem
import org.opendcgrid.app.polaris.JVMAppContext
import org.opendcgrid.app.polaris.command.{CommandContext, CommandError, DevicesCommand, ExitCommand, HaltCommand, HelpCommand, Parsable, ServerCommand, VersionCommand}
import org.opendcgrid.lib.task.TaskManager

import java.io.{BufferedReader, PrintStream}
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Try}

class ShellContext(
                           val configuration: ShellConfiguration = ShellConfiguration(),
                           in: BufferedReader = Console.in,
                           out: PrintStream = Console.out,
                           err: PrintStream = Console.err,
                         ) extends JVMAppContext(in, out, err) with CommandContext {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
  override val taskManager: TaskManager = new TaskManager

  override def writeFile(fileName: String, data: Array[Byte]): Try[Unit] = Failure(CommandError.UnsupportedOperation("file write"))

  override def readFile(fileName: String): Try[Array[Byte]] = Failure(CommandError.UnsupportedOperation("file read"))

  override def allCommands: Seq[Parsable] = Seq[Parsable](
    DevicesCommand,
    ExitCommand,
    HaltCommand,
    HelpCommand,
    ServerCommand,
    VersionCommand
  )


}
