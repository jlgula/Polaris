package org.opendcgrid.app.polaris.device
import org.opendcgrid.app.polaris.client.definitions.{Device => DefinedDevice}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class CapacityManagerTest extends org.scalatest.funsuite.AnyFunSuite {
  private val device1 = DefinedDevice("1", "device1")
  private val device2 = DefinedDevice("2", "device2")
  private val device3 = DefinedDevice("3", "device3")

  val futureMethod: (DeviceID, PowerValue) => Future[Unit] = (_, _)=> Future.successful(())
  test("addDevice") {
    val sample = new CapacityManager(futureMethod, futureMethod)
    val result = Await.result(sample.addDevice(device1), Duration.Inf)
    assertResult(Nil)(result)
  }

  test("updateDevice") {
    val sample = createSample()
    // Offer power with no requests
    val result = Await.result(sample.updateDevice(device1.copy(powerOffered = Some(PowerValue(20)))), Duration.Inf)
    assertResult(Nil)(result)

    // Make a 10W request on device2
    val result2 = Await.result(sample.updateDevice(device2.copy(powerRequested = Some(PowerValue(10)))), Duration.Inf).sortBy(_.id)
    assertResult(Seq(PowerAssignment("1", PowerValue(0), PowerValue(10)), PowerAssignment("2", PowerValue(10), PowerValue(0))))(result2)

    // Make a 10W request on device 3
    val x = sample.updateDevice(device3.copy(powerRequested = Some(PowerValue(10))))
    val result3 = Await.result(x, Duration.Inf).sortBy(_.id)
    assertResult(Seq(PowerAssignment("1", PowerValue(0), PowerValue(20)), PowerAssignment("3", PowerValue(10), PowerValue(0))))(result3)

    // Make a 30W request on device 2. This can't be accommodated so give him nothing.
    val x1 = sample.updateDevice(device2.copy(powerRequested = Some(PowerValue(30))))
    val result4 = Await.result(x1, Duration.Inf).sortBy(_.id)
    assertResult(Seq(PowerAssignment("1", PowerValue(0), PowerValue(10)), PowerAssignment("2", PowerValue(0), PowerValue(0))))(result4)

    // Reduce power available to 0.
    val x2 = sample.updateDevice(device1.copy(powerOffered = Some(PowerValue(0))))
    val result5 = Await.result(x2, Duration.Inf).sortBy(_.id)
    assertResult(Seq(PowerAssignment("1", PowerValue(0), PowerValue(0)), PowerAssignment("3", PowerValue(0), PowerValue(0))))(result5)
  }

  test("remove device") {
    val sample = createSample()
    // Offer power with no requests
    val result = Await.result(sample.updateDevice(device1.copy(powerOffered = Some(PowerValue(20)))), Duration.Inf)
    assertResult(Nil)(result)

    // Make a 10W request on device2
    val result2 = Await.result(sample.updateDevice(device2.copy(powerRequested = Some(PowerValue(10)))), Duration.Inf).sortBy(_.id)
    assertResult(Seq(PowerAssignment("1", PowerValue(0), PowerValue(10)), PowerAssignment("2", PowerValue(10), PowerValue(0))))(result2)

    val result3 = Await.result(sample.removeDevice(device2.id), Duration.Inf).sortBy(_.id)
    assertResult(Seq(PowerAssignment("1", PowerValue(0), PowerValue(0))))(result3)
  }

  test("getPowerAssignment") {
    val sample = createSample()
    // Offer power with no requests
    val result = Await.result(sample.updateDevice(device1.copy(powerOffered = Some(PowerValue(20)))), Duration.Inf)
    assertResult(Nil)(result)

    // Make a 10W request on device2
    val result2 = Await.result(sample.updateDevice(device2.copy(powerRequested = Some(PowerValue(10)))), Duration.Inf).sortBy(_.id)
    assertResult(Seq(PowerAssignment("1", PowerValue(0), PowerValue(10)), PowerAssignment("2", PowerValue(10), PowerValue(0))))(result2)

    assertResult(PowerAssignment(device2.id, PowerValue(10)))(sample.getPowerAssignment(device2.id))
  }

  test("futures") {
    val defaultAssignment = PowerAssignment("Bad", PowerValue(-1), PowerValue(-1))
    var grantAssignment = defaultAssignment
    var acceptAssignment = defaultAssignment

    def grantMethod(id: DeviceID, value: PowerValue): Future[Unit] = {
      grantAssignment = PowerAssignment(id, value, grantAssignment.powerAccepted)
      Future.successful(())
    }

    def acceptMethod(id: DeviceID, value: PowerValue): Future[Unit] = {
      acceptAssignment = PowerAssignment(id, acceptAssignment.powerGranted, value)
      Future.successful(())
    }

    val sample = new CapacityManager(grantMethod, acceptMethod)
    Await.result(sample.addDevice(device1), Duration.Inf)
    assertResult(defaultAssignment)(grantAssignment)
    assertResult(defaultAssignment)(acceptAssignment)
    Await.result(sample.addDevice(device2), Duration.Inf)
    assertResult(defaultAssignment)(grantAssignment)
    assertResult(defaultAssignment)(acceptAssignment)

    // Setting an offer with no loads should not trigger any actions.
    Await.result(sample.updateDevice(device1.copy(powerOffered = Some(PowerValue(20)))), Duration.Inf)
    assertResult(defaultAssignment)(grantAssignment)
    assertResult(defaultAssignment)(acceptAssignment)

    // Assign a load should trigger both grant and accept
    Await.result(sample.updateDevice(device2.copy(powerRequested = Some(PowerValue(10)))), Duration.Inf)
    assertResult(PowerAssignment("2", PowerValue(10), PowerValue(-1)))(grantAssignment)
    assertResult(PowerAssignment("1", PowerValue(-1), PowerValue(10)))(acceptAssignment)
  }

  def createSample(): CapacityManager = {
    val sample = new CapacityManager(futureMethod, futureMethod)
    val devices = Seq(device1, device2, device3)
    devices.foreach { device =>
      Await.result(sample.addDevice(device), Duration.Inf)
    }
    sample
  }

  //def updateDevice(manager: CapacityManager, id: DeviceID, requested: PowerValue, offered: PowerValue): Unit =
}
