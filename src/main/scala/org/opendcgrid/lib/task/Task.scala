package org.opendcgrid.lib.task

import scala.concurrent.Future

object Task {

}

trait Task {
  def name: String
  def start(): Future[TaskID]
  def terminate(): Future[Unit]
}
