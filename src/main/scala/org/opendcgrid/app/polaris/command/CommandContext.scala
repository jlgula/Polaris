package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import org.opendcgrid.app.polaris.device.DeviceManager

import scala.concurrent.ExecutionContext

trait CommandContext {
  def allCommands: Seq[Parsable]
  def taskManager: DeviceManager
  def actorSystem: ActorSystem
}
