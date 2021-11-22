package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.command.CommandTestUtilities.TestCommandContext
import org.opendcgrid.app.polaris.device.DeviceDescriptor

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class ClientCommandTest extends org.scalatest.funsuite.AnyFunSuite {

  test("parse") {
    assertResult(Success(ClientCommand(ClientCommand.defaultPort)))(ClientCommand.parse(Nil))
    assertResult(Success(ClientCommand(4096)))(ClientCommand.parse(Seq("--port", "4096")))
    assertResult(Failure(CommandError.InvalidPortValue("foo")))(ClientCommand.parse(Seq("--port", "foo")))
  }

  test("client command with no controller") {
    val context = new TestCommandContext()
    val command = ClientCommand(ClientCommand.defaultPort)
    val result = command.run(context)
    assertResult(Failure(CommandError.NoController))(result)
  }

  test("client command with controller") {
    val context = new TestCommandContext()
    val controllerPort = PolarisTestUtilities.getUnusedPort
    val controllerCommand = ServerCommand(controllerPort)
    val controllerResult = controllerCommand.run(context)
    assert(controllerResult.isSuccess)
    val clientCommand = ClientCommand()
    val clientResult = clientCommand.run(context)
    assert(clientResult.isSuccess)
    Await.result(context.deviceManager.terminateDevice(clientResult.get.name), Duration.Inf)
    Await.result(context.deviceManager.terminateDevice(controllerResult.get.name), Duration.Inf)
  }
}