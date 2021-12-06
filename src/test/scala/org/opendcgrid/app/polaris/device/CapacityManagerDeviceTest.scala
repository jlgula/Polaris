package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import org.opendcgrid.app.polaris.PolarisTestUtilities
import org.opendcgrid.app.polaris.client.definitions.{Device => DeviceProperties}
import org.opendcgrid.app.polaris.command.CommandTestUtilities

import scala.concurrent.Await
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

  test("list tasks then terminate all") {
  }


}


