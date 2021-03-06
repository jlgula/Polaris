package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.command.CommandTestUtilities.TestCommandContext
import org.opendcgrid.app.polaris.device.DeviceError

import scala.util.{Failure, Success}

class HaltCommandTest extends org.scalatest.funsuite.AnyFunSuite {

  test("parse") {
    assertResult(Success(HaltCommand("GC1")))(HaltCommand.parse(Seq("GC1")))
    assertResult(Success(HaltCommand("GC1", "GC2")))(HaltCommand.parse(Seq("GC1", "GC2")))
    assertResult(Failure(CommandError.MissingArgument("deviceName")))(HaltCommand.parse(Nil))
  }

  test("halt command") {
    val context = new TestCommandContext()
    val port = CommandUtilities.getUnusedPort
    val serverCommand = ControllerCommand(port)
    val result = for {
      serverResult <- serverCommand.run(context)
      haltResult <- HaltCommand(serverResult.name).run(context)
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
    val badID = "Bad"
    val haltResult =  HaltCommand(badID).run(context)
    val expected = CommandError.ServerError(DeviceError.NotFound(badID))
    haltResult match {
      case Success(_) => fail(s"Unexpected success returned")
      case Failure(error) => assertResult(expected)(error)// Pass
    }
  }


}