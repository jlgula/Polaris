package org.opendcgrid.app.polaris.device

import akka.http.scaladsl.Http
import org.opendcgrid.app.polaris.client.definitions.{Notification, Subscription, Device => DeviceProperties}
import org.opendcgrid.app.polaris.client.device.{DeviceClient, GetPowerAcceptedResponse, GetPowerGrantedResponse}

import scala.concurrent.{ExecutionContext, Future}

trait Device {
  def properties: DeviceProperties

  val deviceClient: DeviceClient

  def serverBinding: Http.ServerBinding

  def terminate(): Future[Http.HttpTerminated]

  def getPowerGranted(implicit ec: ExecutionContext): Future[PowerValue] = {
    deviceClient.getPowerGranted(properties.id).value.flatMap {
      case Right(GetPowerGrantedResponse.OK(value)) => Future.successful(value)
      case other => Future.failed(DeviceError.UnexpectedResponse(other.toString))
    }
  }

  def getPowerAccepted(implicit ec: ExecutionContext): Future[PowerValue] = {
    deviceClient.getPowerAccepted(properties.id).value.flatMap {
      case Right(GetPowerAcceptedResponse.OK(value)) => Future.successful(value)
      case other => Future.failed(DeviceError.UnexpectedResponse(other.toString))
    }
  }

}
