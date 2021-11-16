package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.command.CommandTestUtilities.ShellTestFixture

import scala.util.Success

class HelpCommandTest extends org.scalatest.funsuite.AnyFunSuite {

  test("help command") {
    val fixture = new ShellTestFixture()
    val shell = fixture.shell
    val command = HelpCommand(Nil)
    val result = shell.runCommand(command)
    assert(result.isSuccess)
    val command2 = HelpCommand(Seq("help"))
    val result2 = shell.runCommand(command2)
    //assert(result2.isSuccess)
    assertResult(Success(CommandResponse.MultiResponse(Seq(CommandResponse.TextResponse(HelpCommand.help)))))(result2)
  }

}