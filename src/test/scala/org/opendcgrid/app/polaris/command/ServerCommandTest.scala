package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.command.CommandTestUtilities.TestCommandContext
import org.opendcgrid.app.polaris.device.DeviceDescriptor

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class ServerCommandTest extends org.scalatest.funsuite.AnyFunSuite {

  test("parse") {
    assertResult(Success(ControllerCommand(ControllerCommand.defaultPort)))(ControllerCommand.parse(Nil))
    assertResult(Success(ControllerCommand(4096)))(ControllerCommand.parse(Seq("--port", "4096")))
    assertResult(Failure(CommandError.InvalidPortValue("foo")))(ControllerCommand.parse(Seq("--port", "foo")))
  }

  test("server command") {
    val context = new TestCommandContext()
    val port = CommandUtilities.getUnusedPort
    val command = ControllerCommand(port)
    val result = command.run(context)
    val gcDescriptor = DeviceDescriptor.GC
    result match {
      case Failure(error) => fail(error.getMessage)
      case Success(CommandResponse.DeviceResponse(name, descriptor, uri)) =>
        assertResult(gcDescriptor.name)(name)
        assertResult(command.uri)(uri)
        assertResult(gcDescriptor)(descriptor)
        Await.result(context.deviceManager.terminateDevice(descriptor.name), Duration.Inf)
      case Success(other) => fail(s"Unexpected response: $other")
    }
    val result2 = DevicesCommand.run(context)
    result2 match {
      case Success(CommandResponse.MultiResponse(devices)) => assertResult(Nil)(devices) // validateDevices(devices)
      case other => fail(s"unexpected response $other")
    }
  }



  /*

  def validateDevices(devices: Seq[CommandResponse]): Unit = {
    val device = devices.collectFirst{ case d: CommandResponse.TaskResponse => d}
    assert(device.nonEmpty)
    assertResult(ServerCommand.name)(device.get.name)
  }

   */
}