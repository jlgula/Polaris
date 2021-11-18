package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.command.CommandTestUtilities.TestCommandContext

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class ServerCommandTest extends org.scalatest.funsuite.AnyFunSuite {

  test("parse") {
    assertResult(Success(ServerCommand(ServerCommand.defaultPort)))(ServerCommand.parse(Nil))
    assertResult(Success(ServerCommand(4096)))(ServerCommand.parse(Seq("--port", "4096")))
    assertResult(Failure(CommandError.InvalidPortValue("foo")))(ServerCommand.parse(Seq("--port", "foo")))
  }

  test("server command") {
    val context = new TestCommandContext()
    val port = PolarisTestUtilities.getUnusedPort
    val command = ServerCommand(port)
    val result = command.run(context)
    result match {
      case Failure(error) => fail(error.getMessage)
      case Success(CommandResponse.TaskResponse(name, id, uri)) =>
        assertResult(ServerCommand.name)(name)
        assertResult(command.uri)(uri)
        Await.result(context.taskManager.terminateTask(id), Duration.Inf)
      case Success(other) => fail(s"Unexpected response: $other")
    }
    val result2 = DevicesCommand.run(context)
    result2 match {
      case Success(CommandResponse.MultiResponse(devices)) => validateDevices(devices)
      case other => fail(s"unexpected response $other")
    }
  }

  def validateDevices(devices: Seq[CommandResponse]): Unit = {
    val device = devices.collectFirst{ case d: CommandResponse.TaskResponse => d}
    assert(device.nonEmpty)
    assertResult(ServerCommand.name)(device.get.name)
  }
}