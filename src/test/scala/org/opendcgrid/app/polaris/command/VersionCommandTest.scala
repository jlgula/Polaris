package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.command.CommandTestUtilities.makeShell

import scala.util.{Failure, Success}

class VersionCommandTest extends org.scalatest.funsuite.AnyFunSuite {
  test("version command") {
    val shell = makeShell()
    val command = VersionCommand.parse(Nil)
    val result = shell.runCommand(command.get)
    result match {
      case Success(_) => // TODO: verify format
      case Failure(e) => fail(e.getMessage)
    }
  }

  test("version command verbose") {
    val shell = makeShell()
    val command = VersionCommand.parse(Seq("-V"))
    val result = shell.runCommand(command.get)
    result match {
      case Success(_) => // TODO: verify format
      case Failure(e) => fail(e.getMessage)
    }
  }


}