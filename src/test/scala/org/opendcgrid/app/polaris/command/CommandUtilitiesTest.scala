package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.command.CommandTestUtilities.{TestCommandContext, initializeClientAndServer}
import org.opendcgrid.app.polaris.command.CommandUtilities.parsePort
import org.opendcgrid.app.polaris.{PolarisAppOptionTag, PolarisTestUtilities}
import org.opendcgrid.lib.commandoption.CommandOptionResult

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class CommandUtilitiesTest extends org.scalatest.funsuite.AnyFunSuite {
  val defaultPort = 666
  test("parsePort") {
    assertResult(Success(defaultPort))(runParsePort(Nil))
    assertResult(Success(0))(runParsePort(Seq("--port", "0")))
    assertResult(Failure(CommandError.InvalidPortValue("foo")))(runParsePort(Seq("--port", "foo")))
    assertResult(Failure(CommandError.InvalidPortValue(Integer.MAX_VALUE.toString)))(runParsePort(Seq("--port", Integer.MAX_VALUE.toString)))
  }

  test("locateController") {
    val context = new TestCommandContext()
    val port = PolarisTestUtilities.getUnusedPort
    val controllerResult = ControllerCommand(port).run(context)
    val result = CommandUtilities.locateController(context.deviceManager)
    val uri = Await.result(result, Duration.Inf)
    assertResult(controllerResult.get.uri)(uri)
  }

  test("listDevicesOnController") {
    val context = new TestCommandContext()
    val port = PolarisTestUtilities.getUnusedPort
    val controllerResult = ControllerCommand(port).run(context)
    assert(controllerResult.isSuccess)
    val clientResult = ClientCommand().run(context)
    assert(clientResult.isSuccess)
    val listResult = CommandUtilities.listDevicesOnController(context, controllerResult.get.uri)
    val list = Await.result(listResult, Duration.Inf)
    assert(list.exists(_.name == clientResult.get.name))
  }

  test("locateDeviceByName") {
    val context = new TestCommandContext()
    val clientResult = initializeClientAndServer(context)
    val locateFuture = CommandUtilities.locateDeviceByName(context, clientResult.name)
    val uri = Await.result(locateFuture, Duration.Inf)
    val getResult = GetCommand(uri.toString()).run(context)
    assert(getResult.isSuccess)
  }

  test("getURIByName and getURI") {
    val context = new TestCommandContext()
    val clientResult = initializeClientAndServer(context)
    val getFuture = CommandUtilities.getURIByName(context, clientResult.name)
    val uri = Await.result(getFuture, Duration.Inf)
    val getResult = GetCommand(uri.toString()).run(context)
    assert(getResult.isSuccess)
    val getFuture2 = CommandUtilities.getURIByName(context, clientResult.name + "/powerGranted")
    val uri2 = Await.result(getFuture2, Duration.Inf)
    val getResult2 = GetCommand(uri2.toString()).run(context) // this tests getURI
    assert(getResult2.isSuccess)
  }

  def runParsePort(arguments: Seq[String]): Try[Int] = {
    val options = Seq(PolarisAppOptionTag.Port)
    val result = CommandOptionResult.parse(arguments, options)
    parsePort(result, defaultPort)
  }


}