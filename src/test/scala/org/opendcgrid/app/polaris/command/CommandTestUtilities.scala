package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import org.opendcgrid.app.polaris.PolarisTestFixture
import org.opendcgrid.app.polaris.device.DeviceManager
import org.opendcgrid.app.polaris.shell.{Shell, ShellConfiguration}

import scala.concurrent.ExecutionContextExecutor

/**
 * Test utilities for commands.
 */
object CommandTestUtilities {

  class ShellTestFixture(input: String = "", configuration: ShellConfiguration = ShellConfiguration()) extends PolarisTestFixture(input, configuration) {
    val shell: Shell = Shell(this.polaris)
  }

  class TestCommandContext(val allCommands: Seq[Parsable] = Nil) extends CommandContext {
    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
    override val taskManager: DeviceManager = new DeviceManager
  }
}


