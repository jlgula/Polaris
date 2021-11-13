package org.opendcgrid.lib.task

import scala.util.{Failure, Success, Try}

object TaskID {
  def parse(value: String): Try[TaskID] = {
    try {
      Success(TaskID(value.toInt))
    } catch {
      case e: NumberFormatException => Failure(e)
    }
  }
}

case class TaskID(value: Int) {
  override def toString: String = s"$value"
}
