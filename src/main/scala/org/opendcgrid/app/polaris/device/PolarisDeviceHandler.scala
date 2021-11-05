package org.opendcgrid.app.polaris.device

import org.opendcgrid.app.polaris.{HTTPError, PolarisHandler}
import org.opendcgrid.app.polaris.definitions.{Device, Notification}
import org.opendcgrid.app.polaris.subscription.{NotificationAction, PolarisSubscriptionHandler}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import Device._
import akka.http.scaladsl.model.Uri
import io.circe.syntax._


class PolarisDeviceHandler(val uri: Uri, val subscriptionHandler: PolarisSubscriptionHandler, context: ExecutionContext) extends DeviceHandler with PolarisHandler {
  private val devices = mutable.HashMap[String, Device]()
  private val powerGranted = mutable.HashMap[String, BigDecimal]()


  override def addDevice(respond: DeviceResource.AddDeviceResponse.type)(body: Device): Future[DeviceResource.AddDeviceResponse] = {
    if (devices.contains(body.id)) {
      Future.successful(respond.BadRequest("Device exists"))
    } else {
      devices.put(body.id, body)
      subscriptionHandler.notify(Notification("v1/devices", NotificationAction.Post.value, body.asJson.toString()))
      Future.successful(respond.Created(body.id))
    }
  }

  override def listDevices(respond: DeviceResource.ListDevicesResponse.type)(): Future[DeviceResource.ListDevicesResponse] = {
    Future.successful(respond.OK(devices.values.toVector))
  }

  override def getDevice(respond: DeviceResource.GetDeviceResponse.type)(id: String): Future[DeviceResource.GetDeviceResponse] = {
    val response = devices.get(id).map(respond.OK).getOrElse(respond.NotFound(HTTPError.NotFound(id).message))
    Future.successful(response)
  }

  override def putDevice(respond: DeviceResource.PutDeviceResponse.type)(id: String, body: Device): Future[DeviceResource.PutDeviceResponse] = {
    if (devices.contains(id)) {
      devices.put(id, body)
      subscriptionHandler.notify(Notification(s"v1/devices/$id", NotificationAction.Post.value, body.asJson.toString()))
      Future.successful(respond.NoContent)
    } else Future.successful(respond.NotFound(HTTPError.NotFound(id).message))
  }

  override def getPowerGranted(respond: DeviceResource.GetPowerGrantedResponse.type)(id: String): Future[DeviceResource.GetPowerGrantedResponse] = {
    if (devices.contains(id)) {
      Future.successful(respond.OK(powerGranted.getOrElse(id, BigDecimal(0))))
    } else Future.successful(respond.NotFound(HTTPError.NotFound(id).message))
  }

  override def putPowerGranted(respond: DeviceResource.PutPowerGrantedResponse.type)(id: String, body: BigDecimal): Future[DeviceResource.PutPowerGrantedResponse] = {
    if (devices.contains(id)) {
      powerGranted.put(id, body)
      val xx = subscriptionHandler.notify(Notification(s"$uri/v1/devices/$id/powerGranted", NotificationAction.Post.value, body.asJson.toString()))
      xx.map(_ => respond.NoContent)(context)
      //Future.successful(respond.NoContent)
    } else Future.successful(respond.NotFound(HTTPError.NotFound(id).message))
  }

  override def reset(): Unit = {
    devices.clear()
    powerGranted.clear()
  }
}
