package org.opendcgrid.app.polaris.server.gc

import org.opendcgrid.app.polaris.PolarisHandler

import scala.concurrent.Future

class PolarisGCHandler(handlers: PolarisHandler*) extends GcHandler {

  override def reset(respond: GcResource.ResetResponse.type)(): Future[GcResource.ResetResponse] = {
    handlers.foreach(_.reset())
    Future.successful(respond.Created("reset complete"))
  }
 }
