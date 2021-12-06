package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.client.definitions.{Device => DeviceProperties}
import org.opendcgrid.app.polaris.command.CommandTestUtilities
import org.opendcgrid.app.polaris.command.CommandTestUtilities.GridContext

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

  test("manage devices") {
    val context = new CommandTestUtilities.GridContext()
    implicit val ec: ExecutionContext = context.executionContext
    val deviceManager = context.deviceManager
    val startFuture = for {
      binding1 <- deviceManager.startDevice(DeviceDescriptor.GC, makeProperties(context, DeviceDescriptor.Client), makeURI())
      binding2 <- deviceManager.startDevice(DeviceDescriptor.CapacityManager, makeProperties(context, DeviceDescriptor.CapacityManager), makeURI(), Some(binding1.uri))
      binding3 <- deviceManager.startDevice(DeviceDescriptor.CapacityManager, makeProperties(context, DeviceDescriptor.Client, Some(PowerValue(30))), makeURI(), Some(binding1.uri))
      binding4 <- deviceManager.startDevice(DeviceDescriptor.CapacityManager, makeProperties(context, DeviceDescriptor.Client, None, Some(PowerValue(20))), makeURI(), Some(binding1.uri))
      binding5 <- deviceManager.startDevice(DeviceDescriptor.CapacityManager, makeProperties(context, DeviceDescriptor.Client, None, Some(PowerValue(10))), makeURI(), Some(binding1.uri))
    } yield (binding1, binding2, binding3, binding4, binding5)
    Await.result(startFuture, Duration.Inf)
    //val bindings = Await.result(startFuture, Duration.Inf)
    //val powerAccepted = Await.result(bindings._3.
  }

  def makeProperties(context: GridContext, descriptor: DeviceDescriptor, offered: Option[PowerValue] = None, requested: Option[PowerValue] = None): DeviceProperties = {
    DeviceProperties(context.deviceManager.selectID(), context.deviceManager.selectName(descriptor), offered, requested)
  }

  def makeURI(): Uri = Uri("http://localhost").withPort(PolarisTestUtilities.getUnusedPort)
}


