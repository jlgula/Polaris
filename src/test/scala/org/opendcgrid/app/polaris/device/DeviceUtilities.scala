package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.command.CommandUtilities

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object DeviceUtilities {

  class DeviceTestContext {
    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    /**
     * Makes a controller for use in testing.
     *
     * @return the [[GCDevice]] created
     */
    def createController(): Future[GCDevice] = GCDevice.apply(makeURI(), DeviceProperties(UUID.randomUUID().toString, "GC"))


    def createClient(name: String, controller: GCDevice, powerRequested: Option[PowerValue] = None, powerOffered: Option[PowerValue] = None): Future[ClientDevice] = {
      ClientDevice.apply(makeURI(), DeviceProperties(UUID.randomUUID().toString, name, powerRequested, powerOffered), controller.uri)
    }
  }

  def makeURI(): Uri = Uri("http://localhost").withPort(CommandUtilities.getUnusedPort)



}
