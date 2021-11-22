package org.opendcgrid.app.polaris.device

import org.opendcgrid.app.polaris.PolarisHandler
import org.opendcgrid.app.polaris.server.gc.{GcHandler, GcResource}

import scala.concurrent.Future

class GCHandler(handlers: PolarisHandler*) extends GcHandler {

  override def reset(respond: GcResource.ResetResponse.type)(): Future[GcResource.ResetResponse] = {
    handlers.foreach(_.reset())
    Future.successful(respond.Created("reset complete"))
  }
 }
