package org.opendcgrid.app.polaris.device

import org.opendcgrid.app.polaris.PolarisHandler
import org.opendcgrid.app.polaris.server.gc.{GcHandler, GcResource}

import java.time.OffsetDateTime
import scala.concurrent.Future

class GCHandler(val subscriptionHandler: GCSubscriptionHandler, handlers: PolarisHandler*) extends GcHandler {
  private var dateTime: OffsetDateTime = OffsetDateTime.now() // TODO: convert to Actor
  private var powerPrice: BigDecimal = BigDecimal(0)

  override def reset(respond: GcResource.ResetResponse.type)(): Future[GcResource.ResetResponse] = {
    handlers.foreach(_.reset())
    Future.successful(respond.Created("reset complete"))
  }

  override def getDateTime(respond: GcResource.GetDateTimeResponse.type)(): Future[GcResource.GetDateTimeResponse] = {
    Future.successful(GcResource.GetDateTimeResponse.OK(dateTime))
  }

  override def getPrice(respond: GcResource.GetPriceResponse.type)(): Future[GcResource.GetPriceResponse] = {
    Future.successful(GcResource.GetPriceResponse.OK(powerPrice))
  }

  override def putDateTime(respond: GcResource.PutDateTimeResponse.type)(body: OffsetDateTime): Future[GcResource.PutDateTimeResponse] = {
    dateTime = body
    Future.successful(GcResource.PutDateTimeResponse.NoContent)
  }

  override def putPrice(respond: GcResource.PutPriceResponse.type)(body: BigDecimal): Future[GcResource.PutPriceResponse] = {
    powerPrice = body
    Future.successful(GcResource.PutPriceResponse.NoContent)
  }
}
