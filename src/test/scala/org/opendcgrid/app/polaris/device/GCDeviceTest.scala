package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import org.opendcgrid.app.polaris.command.CommandTestUtilities
import org.opendcgrid.app.polaris.device.DeviceUtilities.DeviceTestContext

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class GCDeviceTest extends org.scalatest.funsuite.AnyFunSuite {
  test("selectName") {
  }

  test("create and terminate") {
    val context = new DeviceTestContext
    val gcDevice = Await.result(context.createController(), Duration.Inf)
    Await.result(gcDevice.terminate(), Duration.Inf)
  }

}


