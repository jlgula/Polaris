package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.device.DeviceDescriptor.{Client, GC}

import scala.concurrent.{ExecutionContext, Future}

class DeviceManager(implicit actorSystem: ActorSystem) {
  implicit val context: ExecutionContext = actorSystem.dispatcher
  case class Binding(name: String, descriptor: DeviceDescriptor, uri: Uri, device: Device)
  private val devices = scala.collection.concurrent.TrieMap[String, Binding]()

  def startDevice(descriptor: DeviceDescriptor, nameOption: Option[String], deviceURI: Uri, serverURI: Option[Uri] = None): Future[Binding] = {
    val name = nameOption.getOrElse(selectName(descriptor))
    if (devices.contains(name)) return Future.failed(DeviceError.DuplicateName(name))
    if (devices.exists { case (_, binding) => binding.uri == deviceURI }) return Future.failed(DeviceError.DuplicateUri(deviceURI))
    descriptor match {
      case GC => GCDevice(deviceURI, name).map(device => Binding(name, descriptor, deviceURI, device)).map(bt => addBinding(bt))
      case Client => ClientDevice(deviceURI, name, serverURI.get).map(device => Binding(name, descriptor, deviceURI, device)).map(bt => addBinding(bt))
    }
  }

  def terminateDevice(name: String): Future[Unit] = {
    if (devices.contains(name)) {
      devices(name).device.terminate().andThen(_ => removeBinding(name)).map(_ => ())
    }
    else Future.failed(DeviceError.NotFound(name))
  }

  def terminateAll(): Future[Unit] = {
    val clients = devices.values.filter(binding => binding.descriptor == DeviceDescriptor.Client)
    val servers = devices.values.filter(binding => binding.descriptor == DeviceDescriptor.GC)
    // Need to terminate clients before servers.
    for {
      _ <- Future.sequence(clients.map(device => terminateDevice(device.name)))
      _ <- Future.sequence(servers.map(device => terminateDevice(device.name)))
    } yield ()
  }

  private def addBinding(binding: Binding): Binding = {
    val boundUri = binding.uri.withPort(binding.device.serverBinding.localAddress.getPort)
    val updatedBinding = binding.copy(uri = boundUri)
    devices.put(binding.name, updatedBinding)
    updatedBinding
  }

  private def removeBinding(name: String): Unit = {
    val removeResult = devices.remove(name)
    assert(removeResult.nonEmpty)
  }

  def listTasks: Iterable[(String, DeviceDescriptor, Uri)] = devices.values.map(binding => (binding.name, binding.descriptor, binding.uri))

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
}

