package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import org.opendcgrid.app.polaris.device.DeviceActor.{GetProperties, PropertiesResponse}
import org.opendcgrid.app.polaris.device.DeviceManagerActor.{AddDevice, Locate, RemoveDevice}
import org.scalatest.funsuite.AnyFunSuiteLike

class DeviceManagerActorTest extends ScalaTestWithActorTestKit with AnyFunSuiteLike {
  test("locate") {
    val descriptor = DeviceDescriptor.Client
    val properties = DeviceProperties("testID", "test")
    val probe = createTestProbe[StatusReply[ActorRef[DeviceActor.Command]]]()
    val manager = spawn(DeviceManagerActor())

    // Verify locate fails initially
    manager ! Locate(properties.id, probe.ref)
    val response = probe.receiveMessage()
    assert(response.isError)

    // Add a device and verify locate.
    val probe3 = createTestProbe[StatusReply[Done]]()
    manager ! AddDevice(descriptor, properties, probe3.ref)
    val addResponse = probe3.receiveMessage()
    assert(addResponse.isSuccess)
    manager ! Locate(properties.id, probe.ref)
    val response2 = probe.receiveMessage()
    assert(response2.isSuccess)

    // Validate response is correct actor.
    val probe2 = createTestProbe[PropertiesResponse]()
    response2.getValue ! GetProperties(probe2.ref)
    val response3 = probe2.receiveMessage()
    assertResult(properties.id)(response3.properties.id)
  }

  test("add/remove") {
    val descriptor = DeviceDescriptor.Client
    val properties = DeviceProperties("testID", "test")
    val probe = createTestProbe[StatusReply[ActorRef[DeviceActor.Command]]]()
    val manager = spawn(DeviceManagerActor())

    // Verify non-existent remove failed
    val probe3 = createTestProbe[StatusReply[Done]]()
    manager ! RemoveDevice(properties.id, probe3.ref)
    val failResponse = probe3.receiveMessage()
    assert(failResponse.isError)

    // Add a device
    manager ! AddDevice(descriptor, properties, probe3.ref)
    val addResponse = probe3.receiveMessage()
    assert(addResponse.isSuccess)

    // Remove the device
    manager ! RemoveDevice(properties.id, probe3.ref)
    val successResponse = probe3.receiveMessage()
    assert(successResponse.isSuccess)

    // Verify gone
    manager ! Locate(properties.id, probe.ref)
    val locateResponse = probe.receiveMessage()
    assert(locateResponse.isError)
  }
}
