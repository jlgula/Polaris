package org.opendcgrid.app.polaris.device

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.client.definitions.{Device => DeviceProperties}
import org.opendcgrid.app.polaris.client.device.{DeviceClient, GetDeviceResponse, GetPowerAcceptedResponse, GetPowerGrantedResponse, PutDeviceResponse, PutPowerAcceptedResponse, PutPowerGrantedResponse}

import scala.concurrent.{ExecutionContext, Future}

trait Device {
  def uri: Uri

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

  def putPowerGranted(value: PowerValue)(implicit ec: ExecutionContext): Future[Unit] = {
    deviceClient.putPowerGranted(properties.id, value).value.flatMap {
      case Right(PutPowerGrantedResponse.NoContent) => Future.successful(())
      case other => Future.failed(DeviceError.UnexpectedResponse(other.toString))
    }
  }

  def getPowerAccepted(implicit ec: ExecutionContext): Future[PowerValue] = {
    deviceClient.getPowerAccepted(properties.id).value.flatMap {
      case Right(GetPowerAcceptedResponse.OK(value)) => Future.successful(value)
      case other => Future.failed(DeviceError.UnexpectedResponse(other.toString))
    }
  }

  def putPowerPowerAccepted(value: PowerValue)(implicit ec: ExecutionContext): Future[Unit] = {
    deviceClient.putPowerAccepted(properties.id, value).value.flatMap {
      case Right(PutPowerAcceptedResponse.NoContent) => Future.successful(())
      case other => Future.failed(DeviceError.UnexpectedResponse(other.toString))
    }
  }

  def getProperties(implicit ec: ExecutionContext): Future[DeviceProperties] = {
    deviceClient.getDevice(properties.id).value.flatMap {
      case Right(GetDeviceResponse.OK(value)) => Future.successful(value)
      case other => Future.failed(DeviceError.UnexpectedResponse(other.toString))
    }
  }

  def putProperties(properties: DeviceProperties)(implicit ec: ExecutionContext): Future[Unit] = {
    deviceClient.putDevice(properties.id, properties).value.flatMap {
      case Right(PutDeviceResponse.NoContent) => Future.successful(())
      case other => Future.failed(DeviceError.UnexpectedResponse(other.toString))
    }
  }
}
