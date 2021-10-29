package org.opendcgrid.app.polaris.device

import org.opendcgrid.app.polaris.definitions.Device

import scala.collection.mutable
import scala.concurrent.Future

class PolarisDeviceHandler extends DeviceHandler {
  private val devices = mutable.HashMap[String, Device]()
  override def addDevice(respond: DeviceResource.AddDeviceResponse.type)(body: Device): Future[DeviceResource.AddDeviceResponse] = {
    devices.put(body.id, body)
    Future.successful(respond.Created(body))
  }

  override def listDevices(respond: DeviceResource.ListDevicesResponse.type)(): Future[DeviceResource.ListDevicesResponse] = {
    Future.successful(respond.OK(devices.values.toVector))
  }

  override def getDevice(respond: DeviceResource.GetDeviceResponse.type)(id: String): Future[DeviceResource.GetDeviceResponse] = {
    Future.successful(respond.OK(devices(id)))
  }

  override def updateDevice(respond: DeviceResource.UpdateDeviceResponse.type)(id: String, body: Device): Future[DeviceResource.UpdateDeviceResponse] = {
    devices.put(id, body)
    Future.successful(respond.OK(body))
  }
}
