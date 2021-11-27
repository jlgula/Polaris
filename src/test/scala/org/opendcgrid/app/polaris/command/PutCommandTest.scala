package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.command.CommandTestUtilities.{GridContext, TestCommandContext}

import scala.util.{Failure, Success}

class PutCommandTest extends org.scalatest.funsuite.AnyFunSuite {

  test("parse") {
    assertResult(Success(PutCommand("http://localhost", "test")))(PutCommand.parse(Seq("http://localhost", "test")))
    assertResult(Failure(CommandError.MissingArgument(GetCommand.missingArgumentName)))(PutCommand.parse(Nil))
  }

  test("put command with bad URI") {
    val context = new TestCommandContext()

    // Start a controller so the name parsing won't fail with No Controller.
    val controllerPort = PolarisTestUtilities.getUnusedPort
    val controllerCommand = ControllerCommand(controllerPort)
    val controllerResult = controllerCommand.run(context)
    assert(controllerResult.isSuccess)

    val command = PutCommand("foo", "bar")
    val result = command.run(context)
    result match {
      case Failure(CommandError.InvalidURL("foo", _)) => // Pass
      case Failure(CommandError.NotFound("foo")) => // Pass
      case other => fail(s"unexpected response: $other")
    }
  }

  test("put command with controller and client") {
    val context = new GridContext()
    val powerAsJSON = "5.0"
    val command = PutCommand(context.powerGrantedPath.toString(), powerAsJSON)
    val result = command.run(context)
    result match {
      case Success(CommandResponse.NullResponse) => // Pass
      case other => fail(s"unexpected put response: $other")
    }
    val getResult = GetCommand(context.powerGrantedPath.toString()).run(context)
    getResult match {
      case Success(CommandResponse.TextResponse(text)) => assertResult(powerAsJSON)(text)
      case other => fail(s"unexpected get response: $other")
    }
  }
 }