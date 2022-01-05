package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.StatusReply
import org.opendcgrid.app.polaris.client.definitions.{Device => DeviceProperties}

object DeviceManagerActor {
  final case class DeviceDefinition(descriptor: DeviceDescriptor, properties: DeviceProperties)

  sealed trait Command
  final case class AddDevice(descriptor: DeviceDescriptor, properties: DeviceProperties, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final case class RemoveDevice(id: DeviceID, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final case class Locate(id: DeviceID, replyTo: ActorRef[StatusReply[ActorRef[DeviceActor.Command]]]) extends Command

  def apply(devices: Seq[DeviceDefinition] = Nil): Behavior[Command] =
    Behaviors.setup { context =>
      val deviceMap = devices.map(device => device.descriptor match {
        case DeviceDescriptor.Client => (device.properties.id, context.spawn(DeviceActor(device.properties), device.properties.id))
        case other => throw new UnsupportedOperationException(s"not yet: $other")
      }).toMap
      buildManager(deviceMap)
    }

  private def buildManager(deviceMap: Map[DeviceID, ActorRef[DeviceActor.Command]]): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case AddDevice(descriptor, properties, replyTo) =>
        val deviceActor = descriptor match {
          case DeviceDescriptor.Client => context.spawn(DeviceActor(properties), properties.id)
          case other => throw new UnsupportedOperationException(s"not yet: $other")
        }
        replyTo ! StatusReply.Ack
        buildManager(deviceMap + (properties.id -> deviceActor))
      case RemoveDevice(id, replyTo) =>
        val response = if (deviceMap.contains(id)) StatusReply.Ack else StatusReply.error("Not found")
        replyTo ! response
        buildManager(deviceMap.removed(id))
      case Locate(id, replyTo) =>
        replyTo ! deviceMap.get(id).map(StatusReply.success).getOrElse(StatusReply.error("Not found"))
        buildManager(deviceMap)
    }
  }
}
