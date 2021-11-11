package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.GenericAppContext

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
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


/*
  test("Command.parseDeviceType") {
    //val result = Command.parseDeviceType("foo")
    //assertResult(Success(Command.DeviceTypeParse("foo", Protocol.SimProtocol)))(result)
    Command.parseDeviceType("foo:80") match {
      case Success(DeviceTypeParse("foo", HTTPProtocol(80))) =>
      case other => fail(s"failed parse with $other")
    }
    Command.parseDeviceType("foo:http:80") match {
      case Success(DeviceTypeParse("foo", HTTPProtocol(80))) =>
      case other => fail(s"failed parse with $other")
    }
    Command.parseDeviceType("foo:http:bar") match {
      case Success(value) => fail(s"expected failure but got Success($value)")
      case other => // Silently succeed
    }
  }

 */

  def runShellCommand(shell: Shell, commandLine: String): Try[CommandResponse] = {
    val command = shell.parse(commandLine)
    assert(command.isSuccess)
    val result = shell.runCommand(command.get)
    assert(result.isSuccess)
    result
  }

  def runCommand(command: Command, input: String = "", expectedOutput: String = "", expectedError: String = "", configuration: ShellConfiguration = ShellConfiguration()): Try[CommandResponse] = {
    val in = new ByteArrayInputStream(input.getBytes)
    val output = new ByteArrayOutputStream()
    val error = new ByteArrayOutputStream()
    val shell = Shell(new GenericAppContext(configuration), in, output, error)
    val result = shell.runCommandAndDisplay(command)
    val actualOutput = new String(output.toByteArray)
    val actualError = new String(error.toByteArray)
    assertResult(expectedOutput)(actualOutput)
    assertResult(expectedError)(actualError)
    result
  }

  def runTest(input: String, expectedOutput: String = "", expectedError: String = "", expectedExitCode: Int = 0, configuration: ShellConfiguration = ShellConfiguration()): Unit = {
    val in = new ByteArrayInputStream(input.getBytes)
    val output = new ByteArrayOutputStream()
    val error = new ByteArrayOutputStream()
    val sample = Shell(new GenericAppContext(configuration), in, output, error)
    val result = sample.run()
    assertResult(expectedExitCode)(result)
    val actualOutput = new String(output.toByteArray)
    val actualError = new String(error.toByteArray)
    assertResult(expectedOutput)(actualOutput)
    assertResult(expectedError)(actualError)
  }
}