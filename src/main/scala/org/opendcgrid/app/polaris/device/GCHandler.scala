package org.opendcgrid.app.polaris.device

import io.circe.syntax.EncoderOps
import org.opendcgrid.app.polaris.PolarisHandler
import org.opendcgrid.app.polaris.server.definitions.Notification
import org.opendcgrid.app.polaris.server.gc.{GcHandler, GcResource}

import java.time.OffsetDateTime
import scala.concurrent.{ExecutionContext, Future}

object GCHandler {
  val resetResponse = "reset complete"
  val defaultPrice: Price = Price(0)
}

class GCHandler(val notifier: Notifier, handlers: PolarisHandler*)(implicit ec: ExecutionContext) extends GcHandler {
  private var dateTime: OffsetDateTime = OffsetDateTime.now() // TODO: convert to Actor
  private var powerPrice: Price = GCHandler.defaultPrice

  override def reset(respond: GcResource.ResetResponse.type)(): Future[GcResource.ResetResponse] = {
    notifier.reset()
    handlers.foreach(_.reset())
    Future.successful(respond.Created(GCHandler.resetResponse))
  }

  override def getDateTime(respond: GcResource.GetDateTimeResponse.type)(): Future[GcResource.GetDateTimeResponse] = {
    Future.successful(GcResource.GetDateTimeResponse.OK(dateTime))
  }

  override def getPrice(respond: GcResource.GetPriceResponse.type)(): Future[GcResource.GetPriceResponse] = {
    Future.successful(GcResource.GetPriceResponse.OK(powerPrice))
  }

  override def putDateTime(respond: GcResource.PutDateTimeResponse.type)(body: OffsetDateTime): Future[GcResource.PutDateTimeResponse] = {
    dateTime = body
    val notification = Notification(GCDevice.dateTimePath.toString(), NotificationAction.Put.value, body.asJson.toString())
    notifier.notify(notification).map(_ => respond.NoContent)
  }

  override def putPrice(respond: GcResource.PutPriceResponse.type)(body: BigDecimal): Future[GcResource.PutPriceResponse] = {
    powerPrice = body
    val notification = Notification(GCDevice.powerPricePath.toString(), NotificationAction.Put.value, body.asJson.toString())
    notifier.notify(notification).map(_ => respond.NoContent)
  }
}
