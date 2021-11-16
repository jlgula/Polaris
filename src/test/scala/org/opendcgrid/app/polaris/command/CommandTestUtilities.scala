package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import org.opendcgrid.app.polaris.PolarisTestFixture
import org.opendcgrid.app.polaris.shell.{Shell, ShellConfiguration, ShellContext}
import org.opendcgrid.lib.task.TaskManager

import scala.concurrent.ExecutionContextExecutor

/**
 * Test utilities for commands.
 */
object CommandTestUtilities {

  class ShellTestFixture(input: String = "", configuration: ShellConfiguration = ShellConfiguration(), context: Option[ShellContext] = None) extends PolarisTestFixture(input, configuration) {
    val shell: Shell = Shell(shellContext)
  }

  class TestCommandContext(val allCommands: Seq[Parsable] = Nil) extends CommandContext {
    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
    override val taskManager: TaskManager = new TaskManager
  }
}


