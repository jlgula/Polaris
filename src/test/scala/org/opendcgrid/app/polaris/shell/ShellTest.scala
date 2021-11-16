package org.opendcgrid.app.polaris.shell

import org.opendcgrid.app.polaris.command.CommandTestUtilities.ShellTestFixture
import org.opendcgrid.app.polaris.command.{Command, CommandError, CommandResponse}

import scala.util.Try


class ShellTest extends org.scalatest.funsuite.AnyFunSuite {
  test("basic shell tests") {
    runTest("")
    runTest("#this is a comment")
    runTest("", Shell.prompt, configuration = ShellConfiguration(enablePrompt = true))
    runTest("\n", Shell.prompt + Shell.prompt, configuration = ShellConfiguration(enablePrompt = true))
    runTest("exit\n")
  }

  test("invalid command") {
    runTest("foo\n", expectedError = s"${Shell.appTag}: ${CommandError.InvalidCommand("foo").message}\n")
  }

  def runShellCommand(shell: Shell, commandLine: String): Try[CommandResponse] = {
    val command = shell.parse(commandLine)
    assert(command.isSuccess)
    val result = shell.runCommand(command.get)
    assert(result.isSuccess)
    result
  }

  def runCommand(command: Command, input: String = "", expectedOutput: String = "", expectedError: String = "", configuration: ShellConfiguration = ShellConfiguration()): Try[CommandResponse] = {
    val fixture = new ShellTestFixture(input, configuration)
    val result = fixture.shell.runCommandAndDisplay(command)
    assertResult(expectedOutput)(fixture.output)
    assertResult(expectedError)(fixture.error)
    result
  }


  def runTest(input: String, expectedOutput: String = "", expectedError: String = "", expectedExitCode: Int = 0, configuration: ShellConfiguration = ShellConfiguration()): Unit = {
    val fixture = new ShellTestFixture(input, configuration)
    val result = fixture.shell.run()
    assertResult(expectedExitCode)(result)
    assertResult(expectedOutput)(fixture.output)
    assertResult(expectedError)(fixture.error)
  }


}