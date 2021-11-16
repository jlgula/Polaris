package org.opendcgrid.app.polaris.command


import org.opendcgrid.app.polaris.command.CommandTestUtilities.ShellTestFixture

import scala.util.{Failure, Success}

class ExitCommandTest extends org.scalatest.funsuite.AnyFunSuite {
  test("exit command") {
    val fixture = new ShellTestFixture()
    val shell = fixture.shell
    val command = ExitCommand(0)
    val result = shell.runCommand(command)
    result match {
      case Success(CommandResponse.ExitResponse(0)) =>
      case Success(response) => fail(s"Unexpected response $response")
      case Failure(e) => fail(e.getMessage)
    }
  }

}