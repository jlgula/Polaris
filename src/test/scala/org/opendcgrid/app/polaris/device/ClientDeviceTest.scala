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

  test("get/put powerGranted with subscription") {
    val context = new DeviceTestContext
    implicit val ec: ExecutionContext = context.executionContext
    val controller = Await.result(context.createController(), Duration.Inf)
    val client = Await.result(context.createClient("test", controller), Duration.Inf)
    val power = PowerValue(10)
    Await.result(client.putPowerGranted(power), Duration.Inf)
    val getResult = Await.result(client.getPowerGranted, Duration.Inf)
    assertResult(power)(getResult)
    assertResult(power)(client.powerGranted)
  }

  test("get/put powerAccepted with subscription") {
    val context = new DeviceTestContext
    implicit val ec: ExecutionContext = context.executionContext
    val controller = Await.result(context.createController(), Duration.Inf)
    val client = Await.result(context.createClient("test", controller), Duration.Inf)
    val power = PowerValue(10)
    Await.result(client.putPowerPowerAccepted(power), Duration.Inf)
    val getResult = Await.result(client.getPowerAccepted, Duration.Inf)
    assertResult(power)(getResult)
    assertResult(power)(client.powerAccepted)
  }

  test("gridPrice with subscription") {
    val context = new DeviceTestContext
    implicit val ec: ExecutionContext = context.executionContext
    val controller = Await.result(context.createController(), Duration.Inf)
    val clientPrice = Price(10)
    val powerRequested = PowerValue(20)
    val powerOffered = PowerValue(30)
    val client = Await.result(context.createClient("test", controller, Some(powerRequested), Some(PowerValue(30)), Some(clientPrice)), Duration.Inf)
    val price = Price(10)
    Await.result(controller.gcClient.putPrice(price).value, Duration.Inf)
    assertResult(price)(client.powerPrice)

    // Get the properties from the server and make sure that offered and accepted still match defaults.
    val properties1 = Await.result(client.getProperties, Duration.Inf)
    assertResult(Some(powerRequested))(properties1.powerRequested)
    assertResult(Some(powerOffered))(properties1.powerOffered)

    // Set the price higher than he is willing to pay to receive power.
    val price2 = Price(20)
    Await.result(controller.gcClient.putPrice(price2).value, Duration.Inf)
    assertResult(price2)(client.powerPrice)

    // Get the properties from the server and make sure that requested is now 0 but offered is still default.
    val properties2 = Await.result(client.getProperties, Duration.Inf)
    assertResult(None)(properties2.powerRequested)
    assertResult(Some(powerOffered))(properties2.powerOffered)

    // Set the price lower than he is willing to receive to offer power.
    val price3 = Price(5)
    Await.result(controller.gcClient.putPrice(price3).value, Duration.Inf)
    assertResult(price3)(client.powerPrice)

    // Get the properties from the server and make sure that requested is now default but offered is 0.
    val properties = Await.result(client.getProperties, Duration.Inf)
    assertResult(Some(powerRequested))(properties.powerRequested)
    assertResult(None)(properties.powerOffered)
  }


}


