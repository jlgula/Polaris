package org.opendcgrid.app.polaris.device

import org.opendcgrid.app.polaris.device.DeviceUtilities.DeviceTestContext

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class ClientDeviceTest extends org.scalatest.funsuite.AnyFunSuite {

  test("create and terminate") {
    val name = "test"
    val context = new DeviceTestContext
    val controller = Await.result(context.createController(), Duration.Inf)
    val client = Await.result(context.createClient(name, controller), Duration.Inf)
    assertResult(name)(client.properties.name)
    Await.result(client.terminate(), Duration.Inf)
  }

  test("get/put properties") {
    val name = "test"
    val context = new DeviceTestContext
    implicit val ec: ExecutionContext = context.executionContext
    val controller = Await.result(context.createController(), Duration.Inf)
    val client = Await.result(context.createClient(name, controller), Duration.Inf)
    val powerRequested = PowerValue(10)
    val powerOffered = PowerValue(10)
    val updatedProperties = client.properties.copy(powerRequested = Some(powerRequested), powerOffered = Some(powerOffered))
    Await.result(client.putProperties(updatedProperties), Duration.Inf)
    val result = Await.result(client.getProperties, Duration.Inf)
    assertResult(client.properties.name)(result.name)
    assertResult(client.properties.id)(result.id)
    assertResult(updatedProperties.powerRequested)(result.powerRequested)
    assertResult(updatedProperties.powerOffered)(result.powerOffered)
  }

  test("get/put powerGranted") {
    // TODO
  }

  test("get/put powerAccepted") {
    // TODO
  }

  test("powerGrantedSubscription") {
    val context = new DeviceTestContext
    implicit val ec: ExecutionContext = context.executionContext
    val controller = Await.result(context.createController(), Duration.Inf)
    val client = Await.result(context.createClient("test", controller), Duration.Inf)
    val power = PowerValue(10)
    Await.result(client.putPowerGranted(power), Duration.Inf)
    assertResult(power)(client.powerGranted)
  }

  test("powerAcceptedSubscription") {
    val context = new DeviceTestContext
    implicit val ec: ExecutionContext = context.executionContext
    val controller = Await.result(context.createController(), Duration.Inf)
    val client = Await.result(context.createClient("test", controller), Duration.Inf)
    val power = PowerValue(10)
    Await.result(client.putPowerPowerAccepted(power), Duration.Inf)
    assertResult(power)(client.powerAccepted)
  }


}


