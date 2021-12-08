package org.opendcgrid.app.polaris.command

import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.command.CommandTestUtilities.TestCommandContext
import org.opendcgrid.app.polaris.device.DeviceDescriptor
import org.opendcgrid.app.polaris.client.definitions.{Device => DeviceProperties}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class DevicesCommandTest extends org.scalatest.funsuite.AnyFunSuite {
  test("parse") {
    val parseResult = DevicesCommand.parse(Seq(DevicesCommand.name))
    assertResult(Success(DevicesCommand))(parseResult)
  }

  test("devices command empty") {
    val context = new TestCommandContext()
    val command = DevicesCommand
    val result = command.run(context)
    result match {
      case Failure(error) => fail(error.getMessage)
      case Success(CommandResponse.MultiResponse(responses)) => assert(responses.isEmpty)
      case Success(other) => fail(s"Unexpected response: $other")
    }
  }

  test("devices command with values") {
    val context = new TestCommandContext()
    implicit val ec: ExecutionContext = context.executionContext
    val manager = context.deviceManager
    val uri1 = Uri("http://localhost").withPort(CommandUtilities.getUnusedPort)
    val uri2 = Uri("http://localhost").withPort(CommandUtilities.getUnusedPort)
    val properties1 = DeviceProperties("1", "Device1")
    val properties2 = DeviceProperties("2", "Device2")
    val result = for {
      _ <- manager.startDevice(DeviceDescriptor.GC, properties1, uri1)
      _ <- manager.startDevice(DeviceDescriptor.Client, properties2, uri2, Some(uri1))
    } yield ()
    Await.result(result, Duration.Inf)
    val listResult = DevicesCommand.run(context)
    val expected1 = CommandResponse.DeviceResponse(properties1.name, DeviceDescriptor.GC, uri1)
    val expected2 = CommandResponse.DeviceResponse(properties2.name, DeviceDescriptor.Client, uri2)
    assertResult(Success(CommandResponse.MultiResponse(Seq(expected1, expected2))))(listResult)
    Await.result(manager.terminateAll(), Duration.Inf)
    val listResult2 = manager.listTasks.toSeq
    assert(listResult2.isEmpty)
  }
}