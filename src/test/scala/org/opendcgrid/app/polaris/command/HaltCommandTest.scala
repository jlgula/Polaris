package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.command.CommandTestUtilities.TestCommandContext
import org.opendcgrid.app.polaris.server.ServerError
import org.opendcgrid.lib.task.TaskID

import scala.util.{Failure, Success}

class HaltCommandTest extends org.scalatest.funsuite.AnyFunSuite {

  test("parse") {
    assertResult(Success(HaltCommand(TaskID(1))))(HaltCommand.parse(Seq("1")))
    assertResult(Success(HaltCommand(TaskID(1), TaskID(2))))(HaltCommand.parse(Seq("1", "2")))
    assertResult(Failure(CommandError.MissingArgument("deviceID")))(HaltCommand.parse(Nil))
    assertResult(Failure(CommandError.MultiError(Seq(CommandError.InvalidDevice("foo")))))(HaltCommand.parse(Seq("foo")))
  }

  test("halt command") {
    val context = new TestCommandContext()
    val port = PolarisTestUtilities.getUnusedPort
    val serverCommand = ServerCommand(port)
    val result = for {
      serverResult <- serverCommand.run(context)
      taskResponse = serverResult.asInstanceOf[CommandResponse.TaskResponse]
      haltResult <- HaltCommand(taskResponse.id).run(context)
    } yield haltResult
    result match {
      case Failure(error) => fail(error.getMessage)
      case Success(CommandResponse.MultiResponse(Seq(CommandResponse.HaltResponse(_)))) => // Pass
      case Success(other) => fail(s"Unexpected response: $other")
    }
    val devicesResult = DevicesCommand.run(context)
    assertResult(Success(CommandResponse.MultiResponse(Nil)))(devicesResult)
  }

  test("halt command invalid id") {
    val context = new TestCommandContext()
    val badID = TaskID(0)
    val haltResult =  HaltCommand(badID).run(context)
    val expected = CommandError.ServerError(ServerError.NotFound(badID))
    haltResult match {
      case Success(_) => fail(s"Unexpected success returned")
      case Failure(error) => assertResult(expected)(error)// Pass
    }
  }
}