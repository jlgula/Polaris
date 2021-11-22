package org.opendcgrid.app.polaris.device

import akka.http.scaladsl.Http

import scala.concurrent.Future

trait Device {
  def serverBinding: Http.ServerBinding
  def terminate(): Future[Http.HttpTerminated]
}
