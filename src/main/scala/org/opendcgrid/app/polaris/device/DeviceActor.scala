package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import org.opendcgrid.app.polaris.client.definitions.{Device => DeviceProperties}

object DeviceActor {
  sealed trait Command
  final case class GetProperties(replyTo: ActorRef[PropertiesResponse]) extends Command
  final case class PutProperties(properties: DeviceProperties, replyTo: ActorRef[StatusReply[Done]]) extends Command

  final case class PropertiesResponse(properties: DeviceProperties)


  def apply(properties: DeviceProperties): Behaviors.Receive[Command] =
    Behaviors.receiveMessage {
      case GetProperties(replyTo) =>
        replyTo ! PropertiesResponse(properties)
        this (properties)
      case PutProperties(updatedProperties, replyTo) =>
        replyTo ! StatusReply.Ack
        this (updatedProperties)
    }

}
