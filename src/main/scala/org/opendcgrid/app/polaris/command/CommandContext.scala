package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.device.DeviceManager

import scala.concurrent.{ExecutionContext, Future}

trait CommandContext {
  def allCommands: Seq[Parsable]
  def deviceManager: DeviceManager
  def actorSystem: ActorSystem

  /**
   * Gets the Uri of the microgrid controller server.
   * @return the [[Uri]] of the controller wrapped in a [[Future]]
   */
  def locateController: Future[Uri]
}
