package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import org.opendcgrid.app.polaris.shell.{GenericShellContext, Shell, ShellConfiguration, ShellContext}
import org.opendcgrid.lib.task.TaskManager

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import scala.concurrent.ExecutionContextExecutor

// Test utilities for commands.

object CommandTestUtilities {
  def makeShell(input: String = "", configuration: ShellConfiguration = ShellConfiguration()): Shell = {
    val in = new ByteArrayInputStream(input.getBytes)
    val output = new ByteArrayOutputStream()
    val error = new ByteArrayOutputStream()
    Shell(new GenericShellContext(configuration), in, output, error)
  }

  class ShellTextFixture(input: String = "", configuration: ShellConfiguration = ShellConfiguration(), context: Option[ShellContext] = None) {
    val inputStream = new ByteArrayInputStream(input.getBytes)
    val outputStream = new ByteArrayOutputStream()
    val errorStream = new ByteArrayOutputStream()
    val appContext: ShellContext = context.getOrElse(new GenericShellContext(configuration))
    val shell: Shell = Shell(appContext, inputStream, outputStream, errorStream)

    def output: String = outputStream.toString()
    def error: String = errorStream.toString
  }

  class TestCommandContext(val allCommands: Seq[Parsable] = Nil) extends CommandContext {
    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
    override val taskManager: TaskManager = new TaskManager
  }
}


