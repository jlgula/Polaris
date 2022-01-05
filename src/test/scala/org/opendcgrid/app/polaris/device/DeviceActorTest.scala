package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import org.opendcgrid.app.polaris.device.DeviceActor.PropertiesResponse
import org.scalatest.funsuite.AnyFunSuiteLike

class DeviceActorTest extends ScalaTestWithActorTestKit with AnyFunSuiteLike {
  test("get/put") {
    val properties = DeviceProperties("testID", "test")
    val probe = createTestProbe[PropertiesResponse]()

    val deviceActor = spawn(DeviceActor(properties))

    deviceActor ! DeviceActor.GetProperties(probe.ref)
    val response = probe.receiveMessage()
    assertResult(properties.id)(response.properties.id)

    // Now change and verify the change.
    val properties2 = DeviceProperties("testID", "test", Some(PowerValue(10)))
    val probe2 = createTestProbe[StatusReply[Done]]()
    deviceActor ! DeviceActor.PutProperties(properties2, probe2.ref)
    val putResponse = probe2.receiveMessage()
    assert(putResponse.isSuccess)
    deviceActor ! DeviceActor.GetProperties(probe.ref)
    val response2 = probe.receiveMessage()
    assertResult(properties2.powerRequested)(response2.properties.powerRequested)
  }
}
