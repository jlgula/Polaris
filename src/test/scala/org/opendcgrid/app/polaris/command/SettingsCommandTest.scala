package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.command.CommandTestUtilities.ShellTestFixture

import scala.util.Success

class SettingsCommandTest extends org.scalatest.funsuite.AnyFunSuite {

  test("settings command") {
    val fixture = new ShellTestFixture()
    val shell = fixture.shell
    val command = SettingsCommand(Nil, showOrigin = false)
    val result = shell.runCommand(command)
    result match {
      case Success(CommandResponse.MultiResponse(_)) => // pass
      case other => fail(s"unexpected result: $other")
    }
  }

}