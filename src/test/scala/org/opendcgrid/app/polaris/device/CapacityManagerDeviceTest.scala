package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.client.definitions.{Device => DeviceProperties}
import org.opendcgrid.app.polaris.command.CommandTestUtilities
import org.opendcgrid.app.polaris.command.CommandTestUtilities.{GridContext, TestCommandContext}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

class CapacityManagerDeviceTest extends org.scalatest.funsuite.AnyFunSuite {
  test("selectName") {
  }

  test("start and terminate") {
    val context = new CommandTestUtilities.GridContext()
    implicit val system: ActorSystem = context.actorSystem
    val capacityManagerPort: Int = PolarisTestUtilities.getUnusedPort
    val capacityManagerURI = context.controller.uri.withPort(capacityManagerPort)
    val capacityManagerName = "capacityManager"
    val properties = DeviceProperties("testID", capacityManagerName)
    val builder = new CapacityManagerDevice.DeviceBuilder(capacityManagerURI, properties, context.controller.uri)
    val capacityManagerDevice = Await.result(builder.build(), Duration.Inf)
    val powerAssignmentResult = Await.result(capacityManagerDevice.assignPower(), Duration.Inf)
    assertResult(Nil)(powerAssignmentResult)  // No devices have power to allocate so far.
    val terminationResult = Await.result(capacityManagerDevice.terminate(), Duration.Inf)
    assertResult(Http.HttpServerTerminated)(terminationResult)
  }

  test("manage devices cm first") {
    val context = new TestCommandContext()
    implicit val ec: ExecutionContext = context.executionContext
    val deviceManager = context.deviceManager
    val powerOffered = PowerValue(30)
    val powerGranted1 = PowerValue(20)
    val powerGranted2 = PowerValue(10)
    val startFuture = for {
      binding1 <- deviceManager.startDevice(DeviceDescriptor.GC, makeProperties(context, DeviceDescriptor.GC), makeURI())
      binding2 <- deviceManager.startDevice(DeviceDescriptor.CapacityManager, makeProperties(context, DeviceDescriptor.CapacityManager), makeURI(), Some(binding1.uri))
      binding3 <- deviceManager.startDevice(DeviceDescriptor.Client, makeProperties(context, DeviceDescriptor.Client, Some(powerOffered)), makeURI(), Some(binding1.uri))
      binding4 <- deviceManager.startDevice(DeviceDescriptor.Client, makeProperties(context, DeviceDescriptor.Client, None, Some(PowerValue(20))), makeURI(), Some(binding1.uri))
      binding5 <- deviceManager.startDevice(DeviceDescriptor.Client, makeProperties(context, DeviceDescriptor.Client, None, Some(PowerValue(10))), makeURI(), Some(binding1.uri))
    } yield (binding1, binding2, binding3, binding4, binding5)
    val bindings = Await.result(startFuture, Duration.Inf)
    val powerAcceptedResult = Await.result(bindings._3.device.getPowerAccepted, Duration.Inf)
    assertResult(powerOffered)(powerAcceptedResult)
    val powerGrantedResult1 = Await.result(bindings._4.device.getPowerGranted, Duration.Inf)
    assertResult(powerGranted1)(powerGrantedResult1)
    val powerGrantedResult2 = Await.result(bindings._5.device.getPowerGranted, Duration.Inf)
    assertResult(powerGranted2)(powerGrantedResult2)
  }

  test("manage devices cm last") {
    val context = new TestCommandContext()
    implicit val ec: ExecutionContext = context.executionContext
    val deviceManager = context.deviceManager
    val powerOffered = PowerValue(30)
    val powerGranted1 = PowerValue(20)
    val powerGranted2 = PowerValue(10)
    val startFuture = for {
      binding1 <- deviceManager.startDevice(DeviceDescriptor.GC, makeProperties(context, DeviceDescriptor.GC), makeURI())
      binding3 <- deviceManager.startDevice(DeviceDescriptor.Client, makeProperties(context, DeviceDescriptor.Client, Some(powerOffered)), makeURI(), Some(binding1.uri))
      binding4 <- deviceManager.startDevice(DeviceDescriptor.Client, makeProperties(context, DeviceDescriptor.Client, None, Some(PowerValue(20))), makeURI(), Some(binding1.uri))
      binding5 <- deviceManager.startDevice(DeviceDescriptor.Client, makeProperties(context, DeviceDescriptor.Client, None, Some(PowerValue(10))), makeURI(), Some(binding1.uri))
      binding2 <- deviceManager.startDevice(DeviceDescriptor.CapacityManager, makeProperties(context, DeviceDescriptor.CapacityManager), makeURI(), Some(binding1.uri))
    } yield (binding1, binding2, binding3, binding4, binding5)
    val bindings = Await.result(startFuture, Duration.Inf)
    val powerAcceptedResult = Await.result(bindings._3.device.getPowerAccepted, Duration.Inf)
    assertResult(powerOffered)(powerAcceptedResult)
    val powerGrantedResult1 = Await.result(bindings._4.device.getPowerGranted, Duration.Inf)
    assertResult(powerGranted1)(powerGrantedResult1)
    val powerGrantedResult2 = Await.result(bindings._5.device.getPowerGranted, Duration.Inf)
    assertResult(powerGranted2)(powerGrantedResult2)
  }


  def makeProperties(context: TestCommandContext, descriptor: DeviceDescriptor, offered: Option[PowerValue] = None, requested: Option[PowerValue] = None): DeviceProperties = {
    DeviceProperties(context.deviceManager.selectID(), context.deviceManager.selectName(descriptor), requested, offered)
  }

  def makeURI(): Uri = Uri("http://localhost").withPort(PolarisTestUtilities.getUnusedPort)
}


