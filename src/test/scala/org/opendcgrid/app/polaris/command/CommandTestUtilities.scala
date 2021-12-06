package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.{PolarisTestFixture, PolarisTestUtilities}
import org.opendcgrid.app.polaris.device.DeviceManager
import org.opendcgrid.app.polaris.shell.{Shell, ShellConfiguration}
//import org.opendcgrid.app.polaris.client.definitions.{Device => DeviceProperties}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

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

  class GridContext extends TestCommandContext {
    val controllerPort: Int = PolarisTestUtilities.getUnusedPort
    val controller: CommandResponse.DeviceResponse = ControllerCommand(controllerPort).run(this).get
    val client: CommandResponse.DeviceResponse = ClientCommand().run(this).get
    val clientPathOnServer: Uri = Await.result(CommandUtilities.locateDeviceByName(this, client.name), Duration.Inf)
    val powerGrantedPath: Uri = clientPathOnServer.withPath(clientPathOnServer.path ++ Uri.Path("/powerGranted"))
  }

  def initializeClientAndServer(context: CommandContext): CommandResponse.DeviceResponse = {
    val port = PolarisTestUtilities.getUnusedPort
    val controllerResult = ControllerCommand(port).run(context)
    assert(controllerResult.isSuccess)
    val clientResult = ClientCommand().run(context)
    assert(clientResult.isSuccess)
    clientResult.get
  }

}


