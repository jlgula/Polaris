package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.PolarisAppOptionTag
import org.opendcgrid.app.polaris.command.CommandUtilities.parsePort
import org.opendcgrid.lib.commandoption.CommandOptionResult

import scala.util.{Failure, Success, Try}

class CommandUtilitiesTest extends org.scalatest.funsuite.AnyFunSuite {
  val defaultPort = 666
  test("parsePort") {
    assertResult(Success(defaultPort))(runParsePort(Nil))
    assertResult(Success(0))(runParsePort(Seq("--port", "0")))
    assertResult(Failure(CommandError.InvalidPortValue("foo")))(runParsePort(Seq("--port", "foo")))
    assertResult(Failure(CommandError.InvalidPortValue(Integer.MAX_VALUE.toString)))(runParsePort(Seq("--port", Integer.MAX_VALUE.toString)))
  }

  def runParsePort(arguments: Seq[String]): Try[Int] = {
    val options = Seq(PolarisAppOptionTag.Port)
    val result = CommandOptionResult.parse(arguments, options)
    parsePort(result, defaultPort)
  }

}