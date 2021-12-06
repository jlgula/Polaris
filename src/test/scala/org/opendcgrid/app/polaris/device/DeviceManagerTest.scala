package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.command.CommandTestUtilities.TestCommandContext
import org.opendcgrid.app.polaris.client.definitions.{Device => DeviceProperties}


import java.util.concurrent.Semaphore
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.{Failure, Success, Try}

class DeviceManagerTest extends org.scalatest.funsuite.AnyFunSuite {
  test("selectName") {
    implicit val actorSystem: ActorSystem = ActorSystem()
    val manager = new DeviceManager()
    val descriptor = DeviceDescriptor.GC
    assertResult(descriptor.name)(manager.selectName(descriptor))
    assertResult(descriptor.name + "1")(manager.selectName(descriptor, Some(1)))
  }

  test("start and terminate") {
    val context = new TestCommandContext()
    implicit val ec: ExecutionContextExecutor = context.executionContext
    val manager = context.deviceManager
    val uri1 = Uri("http://localhost").withPort(PolarisTestUtilities.getUnusedPort)
    val properties1 = DeviceProperties("ID1", "name1")
    val result = for {
      binding <- manager.startDevice(DeviceDescriptor.GC, properties1, uri1)
      termination <- manager.terminateDevice(binding.name)
    } yield termination
    Try(Await.result(result, Duration.Inf)) match {
      case Success(_) => // pass
      case other => fail(s"unexpected result: $other")
    }
    val properties2 = DeviceProperties("ID2", "name2")
    val result2 = for {
      binding <- manager.startDevice(DeviceDescriptor.GC, properties2, uri1)
      termination <- manager.terminateDevice(binding.name)
    } yield termination
    Try(Await.result(result2, Duration.Inf)) match {
      case Success(_) => // pass
      case other => fail(s"unexpected result: $other")
    }
  }

  test("list tasks then terminate all") {
    val context = new TestCommandContext()
    implicit val ec: ExecutionContextExecutor = context.executionContext
    val manager = context.deviceManager
    val properties1 = DeviceProperties("ID1", "name1")
    val uri1 = Uri("http://localhost").withPort(PolarisTestUtilities.getUnusedPort)
    val properties2 = DeviceProperties("ID2", "name2")
    val uri2 = Uri("http://localhost").withPort(PolarisTestUtilities.getUnusedPort)
    val result = for {
      _ <- manager.startDevice(DeviceDescriptor.GC, properties1, uri1)
      _ <- manager.startDevice(DeviceDescriptor.GC, properties2, uri2)
    } yield ()
    Await.result(result, Duration.Inf)
    val listResult = manager.listTasks.toSeq
    val expected1 = (properties1.name, DeviceDescriptor.GC, uri1)
    val expected2 = (properties2.name, DeviceDescriptor.GC, uri2)
    assertResult(Seq(expected1, expected2))(listResult)
    Await.result(manager.terminateAll(), Duration.Inf)
    val listResult2 = manager.listTasks.toSeq
    assert(listResult2.isEmpty)
  }


  test("terminate invalid taskID") {
    val context = new TestCommandContext()
    implicit val ec: ExecutionContextExecutor = context.executionContext
    val manager = context.deviceManager
    val badID = "bad"
    val future = manager.terminateDevice(badID)
    val futureResult = Try(Await.result(future, Duration.Inf))
    futureResult match {
      case Success(_) => fail("expected failure not delivered")
      case Failure(DeviceError.NotFound(_)) => // Pass
      case Failure(error) => fail(s"unexpected error: $error")
    }
  }

  test("empty terminate all") {
    val semaphore = new Semaphore(0)
    val context = new TestCommandContext()
    implicit val ec: ExecutionContextExecutor = context.executionContext
    val manager = context.deviceManager
    manager.terminateAll().onComplete(_ => semaphore.release())
    semaphore.acquire()
  }
}


