package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.client.definitions.{Device => DeviceProperties}
import org.opendcgrid.app.polaris.device.DeviceDescriptor.{CapacityManager, Client, GC}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DeviceManager(implicit actorSystem: ActorSystem) {
  implicit val context: ExecutionContext = actorSystem.dispatcher
  case class Binding(name: String, descriptor: DeviceDescriptor, uri: Uri, device: Device)
  private val devices = scala.collection.concurrent.TrieMap[String, Binding]()

  /**
   * Activates a device and adds it to the list of devices known to the manager.
   *
   * @param descriptor the enumeration of device types
   * @param properties  the initial properties of the device such as name, ID etc
   * @param deviceURI the [[Uri]] of for the device where it receives notifications
   * @param controllerURI the [[Uri]] microgrid controller that manages the device
   * @return a [[Binding]] that describes the device in this manager, wrapped in a future
   */
  def startDevice(descriptor: DeviceDescriptor, properties: DeviceProperties, deviceURI: Uri, controllerURI: Option[Uri] = None): Future[Binding] = {
    if (devices.contains(properties.name)) return Future.failed(DeviceError.DuplicateName(properties.name))
    if (devices.exists { case (_, binding) => binding.uri == deviceURI }) return Future.failed(DeviceError.DuplicateUri(deviceURI))
    descriptor match {
      case GC => GCDevice(deviceURI, properties).map(device => Binding(properties.name, descriptor, deviceURI, device)).map(bt => addBinding(bt))
      case Client => ClientDevice(deviceURI, properties, controllerURI.get).map(device => Binding(properties.name, descriptor, deviceURI, device)).map(bt => addBinding(bt))
      case CapacityManager => CapacityManagerDevice(deviceURI, properties, controllerURI.get).map(device => Binding(properties.name, descriptor, deviceURI, device)).map(bt => addBinding(bt))
    }
  }

  /**
   * Removes a device from the list of devices known to this manager.
   *
   * @param name  the string name of the device on the manager
   * @return  a Unit future that captures any errors
   */
  def terminateDevice(name: String): Future[Unit] = {
    if (devices.contains(name)) {
      devices(name).device.terminate().andThen(_ => removeBinding(name)).map(_ => ())
    }
    else Future.failed(DeviceError.NotFound(name))
  }

  /**
   * Terminates all devices known to the manager.
   *
   * Client devices are removed before the controller.
   * @return a Unit future that captures any errors
   */
  def terminateAll(): Future[Unit] = {
    val clients = devices.values.filter(binding => binding.descriptor == DeviceDescriptor.Client)
    val capacityManagers = devices.values.filter(binding => binding.descriptor == DeviceDescriptor.CapacityManager)
    val controllers = devices.values.filter(binding => binding.descriptor == DeviceDescriptor.GC)
    // Need to terminate clients before servers.
    for {
      _ <- Future.sequence(clients.map(device => terminateDevice(device.name)))
      _ <- Future.sequence(capacityManagers.map(device => terminateDevice(device.name)))
      _ <- Future.sequence(controllers.map(device => terminateDevice(device.name)))
    } yield ()
  }

  /**
   * Generates an Iterable that enumerates all known devices.
   *
   * @return an [[Iterable]] of a tuple of the devices
   */
  def listTasks: Iterable[(String, DeviceDescriptor, Uri)] = devices.values.map(binding => (binding.name, binding.descriptor, binding.uri))

  /**
   * Chooses a name for the device.
   *
   * The name is normally taken from the device descriptor.
   *
   * @param descriptor  The enumeration of device types
   * @param index a suggested index to distinguish from similarly named device
   * @return  the [[String]] name of the device
   */
  def selectName(descriptor: DeviceDescriptor, index: Option[Int] = None): String = {
    if (index.isEmpty) {
      if (!devices.contains(descriptor.name)) descriptor.name
      else selectName(descriptor, Some(1))
    } else {
      val candidate = s"${descriptor.name}${index.get}"
      if (!devices.contains(candidate)) candidate
      else selectName(descriptor, Some(index.get + 1))
    }
  }

  /**
   * Generates a unique ID for a device.
   *
   * @return the ID of the device.
   */
  def selectID(): DeviceID = UUID.randomUUID().toString

  // Adds a device to the database of known devices
  private def addBinding(binding: Binding): Binding = {
    val boundUri = binding.uri.withPort(binding.device.serverBinding.localAddress.getPort)
    val updatedBinding = binding.copy(uri = boundUri)
    devices.put(binding.name, updatedBinding)
    updatedBinding
  }

  // Removes a device from the table of known devices.
  private def removeBinding(name: String): Unit = {
    val removeResult = devices.remove(name)
    assert(removeResult.nonEmpty)
  }


}

