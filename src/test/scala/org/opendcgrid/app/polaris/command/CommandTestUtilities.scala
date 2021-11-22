package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.PolarisTestFixture
import org.opendcgrid.app.polaris.device.DeviceManager
import org.opendcgrid.app.polaris.shell.{Shell, ShellConfiguration}

import scala.concurrent.{ExecutionContextExecutor, Future}

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
    override val deviceManager: DeviceManager = new DeviceManager

    override def locateController: Future[Uri] = CommandUtilities.locateController(deviceManager)
  }
}


