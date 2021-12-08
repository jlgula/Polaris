package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.command.CommandTestUtilities.TestCommandContext

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class CapacityManagerCommandTest extends org.scalatest.funsuite.AnyFunSuite {

  test("parse") {
    assertResult(Success(CapacityManagerCommand(4096)))(CapacityManagerCommand.parse(Seq("--port", "4096")))
    assertResult(Failure(CommandError.InvalidPortValue("foo")))(CapacityManagerCommand.parse(Seq("--port", "foo")))
  }

  test("command with no controller") {
    val context = new TestCommandContext()
    val command = CapacityManagerCommand()
    val result = command.run(context)
    assertResult(Failure(CommandError.NoController))(result)
  }

  test("command with controller and client") {
    val context = new TestCommandContext()
    val controllerPort = CommandUtilities.getUnusedPort
    val controllerCommand = ControllerCommand(controllerPort)
    val controllerResult = controllerCommand.run(context)
    assert(controllerResult.isSuccess)
    val command = CapacityManagerCommand()
    val result = command.run(context)
    assert(result.isSuccess)
    val result2 = ClientCommand().run(context)
    assert(result2.isSuccess)
    Await.result(context.deviceManager.terminateDevice(result.get.name), Duration.Inf)
    Await.result(context.deviceManager.terminateDevice(controllerResult.get.name), Duration.Inf)
  }
}