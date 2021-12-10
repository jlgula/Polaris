package org.opendcgrid.app.polaris.device

import org.opendcgrid.app.polaris.PolarisHandler
import org.opendcgrid.app.polaris.server.definitions.Notification

import scala.concurrent.Future

trait Notifier extends PolarisHandler {
  /**
   * Notifies all observers that a resource has changed.
   *
   * @param notification a [[Notification]] that contains the observed resource and the new value.
   * @return a [[Future]] that contains any errors when it completes
   */

  def notify(notification: Notification): Future[Unit]
}
