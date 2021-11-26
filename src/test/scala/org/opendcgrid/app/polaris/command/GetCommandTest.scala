package org.opendcgrid.app.polaris.command

import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.command.CommandTestUtilities.TestCommandContext

import scala.util.{Failure, Success}

class GetCommandTest extends org.scalatest.funsuite.AnyFunSuite {

  test("parse") {
    assertResult(Success(GetCommand("http://localhost")))(GetCommand.parse(Seq("http://localhost")))
    assertResult(Failure(CommandError.MissingArgument(GetCommand.missingArgumentName)))(GetCommand.parse(Nil))
  }

  test("get command with bad URI") {
    val context = new TestCommandContext()
    val command = GetCommand("foo")
    val result = command.run(context)
    result match {
      case Failure(CommandError.InvalidURL("foo", _)) => // Pass
      case other => fail(s"unexpected response: $other")
    }
  }

  test("get command unreachable URI") {
    /* ClientConnectionSettings doesn't seem to work...
    val sys: ActorSystem = implicitly[ActorSystem]
    val settings = ClientConnectionSettings(sys).withConnectingTimeout(FiniteDuration(1, TimeUnit.SECONDS))
    val s2 = ConnectionPoolSettings(sys).withConnectionSettings(settings)

    val context = new TestCommandContext()
    val urlText = "http://unreachable"
    val command = GetCommand(urlText)
    val result = command.run(context)
    result match {
      case Failure(CommandError.InvalidURL(url, _)) => assertResult(urlText)(url)
      case other => fail(s"unexpected response: $other")
    }

     */
  }

  test("get command with controller") {
    val context = new TestCommandContext()
    val controllerPort = PolarisTestUtilities.getUnusedPort
    val controllerCommand = ControllerCommand(controllerPort)
    val controllerResult = controllerCommand.run(context)
    assert(controllerResult.isSuccess)
    val path = Uri.Path("/v1/devices")
    val uri = controllerResult.get.uri.withPath(path)
    val command = GetCommand(uri.toString())
    val result = command.run(context)
    result match {
      case Success(CommandResponse.TextResponse(_)) => // Pass
      case other => fail(s"unexpected response: $other")
    }
  }
 }