package org.opendcgrid.app.polaris.device

import akka.http.scaladsl.model.Uri
import io.circe.syntax.EncoderOps
import org.opendcgrid.app.polaris.device.DeviceUtilities.DeviceTestContext
import org.opendcgrid.app.polaris.server.definitions.Notification
import org.opendcgrid.app.polaris.server.gc.GcResource.{GetDateTimeResponse, GetPriceResponse, PutDateTimeResponse, PutPriceResponse, ResetResponse}

import java.time.OffsetDateTime
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class GCHandlerTest extends org.scalatest.funsuite.AnyFunSuite {
  private val gcURI = Uri("http://localhost") // Needed by GCHandler but not used with TestNotifier.

  test("reset") {
    val context = new DeviceTestContext
    implicit val ec: ExecutionContext = context.executionContext
    val subscriptionHandler = new TestNotifier
    val sample = new GCHandler(gcURI, subscriptionHandler)
    val result = Await.result(sample.reset(ResetResponse)(), Duration.Inf)
    assertResult(ResetResponse.Created(GCHandler.resetResponse))(result)
    assert(subscriptionHandler.isReset)
  }

  test("getPrice/putPrice") {
    val context = new DeviceTestContext
    implicit val ec: ExecutionContext = context.executionContext
    val notifier = new TestNotifier
    val sample = new GCHandler(gcURI, notifier)
    val result = Await.result(sample.getPrice(GetPriceResponse)(), Duration.Inf)
    assertResult(GetPriceResponse.OK(GCHandler.defaultPrice))(result)
    val priceUpdate = Price(666)
    val result2 = Await.result(sample.putPrice(PutPriceResponse)(priceUpdate), Duration.Inf)
    assertResult(PutPriceResponse.NoContent)(result2)
    assertResult(Notification(gcURI.withPath(GCDevice.powerPricePath).toString(), NotificationAction.Put.value, priceUpdate.asJson.toString))(notifier.notification.get)
    val result3 = Await.result(sample.getPrice(GetPriceResponse)(), Duration.Inf)
    assertResult(GetPriceResponse.OK(priceUpdate))(result3)
  }

  test("getDateTime/putDateTime") {
    val context = new DeviceTestContext
    implicit val ec: ExecutionContext = context.executionContext
    val notifier = new TestNotifier
    val sample = new GCHandler(gcURI, notifier)
    val result = Await.result(sample.getDateTime(GetDateTimeResponse)(), Duration.Inf)
    //assertResult(GetDateTimeResponse.OK(GCHandler.defaultPrice))(result) -- initial value is now()
    val timeUpdate = OffsetDateTime.now()
    val result2 = Await.result(sample.putDateTime(PutDateTimeResponse)(timeUpdate), Duration.Inf)
    assertResult(PutDateTimeResponse.NoContent)(result2)
    assertResult(Notification(gcURI.withPath(GCDevice.dateTimePath).toString(), NotificationAction.Put.value, timeUpdate.asJson.toString))(notifier.notification.get)
    val result3 = Await.result(sample.getDateTime(GetDateTimeResponse)(), Duration.Inf)
    assertResult(GetDateTimeResponse.OK(timeUpdate))(result3)
  }


  class TestNotifier extends Notifier {
    var isReset: Boolean = false
    var notification: Option[Notification] = None
    override def notify(notification: Notification): Future[Unit] = {
      this.notification = Some(notification)
      Future.successful(())
    }

    override def reset(): Unit = isReset = true
  }

}


