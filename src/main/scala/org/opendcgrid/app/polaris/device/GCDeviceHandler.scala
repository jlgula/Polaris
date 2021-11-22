package org.opendcgrid.app.polaris.device

import akka.http.scaladsl.model.Uri
import io.circe.syntax._
import org.opendcgrid.app.polaris.server.definitions.{Device, Notification}
import org.opendcgrid.app.polaris.server.device.{DeviceHandler, DeviceResource}
import org.opendcgrid.app.polaris.{PolarisError, PolarisHandler}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}


class GCDeviceHandler(val uri: Uri, val subscriptionHandler: GCSubscriptionHandler)(implicit context: ExecutionContext) extends DeviceHandler with PolarisHandler {
  private val devices = mutable.HashMap[String, Device]()
  private val powerGranted = mutable.HashMap[String, BigDecimal]()
  private val powerAccepted = mutable.HashMap[String, BigDecimal]()


  override def addDevice(respond: DeviceResource.AddDeviceResponse.type)(body: Device): Future[DeviceResource.AddDeviceResponse] = {
    if (devices.contains(body.id)) {
      Future.successful(respond.BadRequest("Device exists"))
    } else {
      devices.put(body.id, body)
      val notificationsFuture = subscriptionHandler.notify(Notification("v1/devices", NotificationAction.Post.value, body.asJson.toString()))
      notificationsFuture.map(_ => respond.Created(body.id))
    }
  }

  override def listDevices(respond: DeviceResource.ListDevicesResponse.type)(): Future[DeviceResource.ListDevicesResponse] = {
    Future.successful(respond.OK(devices.values.toVector))
  }

  override def getDevice(respond: DeviceResource.GetDeviceResponse.type)(id: String): Future[DeviceResource.GetDeviceResponse] = {
    val response = devices.get(id).map(respond.OK).getOrElse(respond.NotFound(PolarisError.NotFound(id).message))
    Future.successful(response)
  }

  override def putDevice(respond: DeviceResource.PutDeviceResponse.type)(id: String, body: Device): Future[DeviceResource.PutDeviceResponse] = {
    if (devices.contains(id)) {
      devices.put(id, body)
      val notificationsFuture = subscriptionHandler.notify(Notification(s"v1/devices/$id", NotificationAction.Post.value, body.asJson.toString()))
      notificationsFuture.map(_ => respond.NoContent)
    } else Future.successful(respond.NotFound(PolarisError.NotFound(id).message))
  }

  override def getPowerGranted(respond: DeviceResource.GetPowerGrantedResponse.type)(id: String): Future[DeviceResource.GetPowerGrantedResponse] = {
    if (devices.contains(id)) {
      Future.successful(respond.OK(powerGranted.getOrElse(id, BigDecimal(0))))
    } else Future.successful(respond.NotFound(PolarisError.NotFound(id).message))
  }

  override def putPowerGranted(respond: DeviceResource.PutPowerGrantedResponse.type)(id: String, body: BigDecimal): Future[DeviceResource.PutPowerGrantedResponse] = {
    if (devices.contains(id)) {
      powerGranted.put(id, body)
      val notificationResult = subscriptionHandler.notify(Notification(s"$uri/v1/devices/$id/powerGranted", NotificationAction.Post.value, body.asJson.toString()))
      notificationResult.map(_ => respond.NoContent)(context)
    } else Future.successful(respond.NotFound(PolarisError.NotFound(id).message))
  }

  override def reset(): Unit = {
    devices.clear()
    powerGranted.clear()
    powerAccepted.clear()
  }

  override def deleteDevice(respond: DeviceResource.DeleteDeviceResponse.type)(id: String): Future[DeviceResource.DeleteDeviceResponse] = {
    if (devices.contains(id)) {
      devices.remove(id)
      powerGranted.remove(id)
      powerAccepted.remove(id)
      Future.successful(respond.NoContent)
    } else Future.successful(respond.NotFound(PolarisError.NotFound(id).message))
  }

  override def getPowerAccepted(respond: DeviceResource.GetPowerAcceptedResponse.type)(id: String): Future[DeviceResource.GetPowerAcceptedResponse] = {
    if (devices.contains(id)) {
      Future.successful(respond.OK(powerAccepted.getOrElse(id, BigDecimal(0))))
    } else Future.successful(respond.NotFound(PolarisError.NotFound(id).message))
  }

  override def putPowerAccepted(respond: DeviceResource.PutPowerAcceptedResponse.type)(id: String, body: BigDecimal): Future[DeviceResource.PutPowerAcceptedResponse] = {
    if (devices.contains(id)) {
      powerAccepted.put(id, body)
      val notificationResult = subscriptionHandler.notify(Notification(s"$uri/v1/devices/$id/powerAccepted", NotificationAction.Post.value, body.asJson.toString()))
      notificationResult.map(_ => respond.NoContent)(context)
    } else Future.successful(respond.NotFound(PolarisError.NotFound(id).message))
  }
}
