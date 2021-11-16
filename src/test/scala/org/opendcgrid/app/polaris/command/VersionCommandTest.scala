package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.command.CommandTestUtilities.ShellTestFixture

import scala.util.{Failure, Success}

class VersionCommandTest extends org.scalatest.funsuite.AnyFunSuite {
  test("version command") {
    val fixture = new ShellTestFixture()
    val shell = fixture.shell
    val command = VersionCommand.parse(Nil)
    val result = shell.runCommand(command.get)
    result match {
      case Success(_) => // TODO: verify format
      case Failure(e) => fail(e.getMessage)
    }
  }

  test("version command verbose") {
    val fixture = new ShellTestFixture()
    val shell = fixture.shell
    val command = VersionCommand.parse(Seq("-V"))
    val result = shell.runCommand(command.get)
    result match {
      case Success(_) => // TODO: verify format
      case Failure(e) => fail(e.getMessage)
    }
  }


}