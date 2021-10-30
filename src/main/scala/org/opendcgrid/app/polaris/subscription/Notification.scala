package org.opendcgrid.app.polaris.subscription

sealed abstract class NotificationAction(val value: Int)
object NotificationAction {
  case object Put extends NotificationAction(1)
  case object Post extends NotificationAction(2)
  case object Delete extends NotificationAction(3)

  def find(value: Int): Option[NotificationAction] = all.find(_.value == value)
  val all = Seq(Put, Put, Delete)
}

case class Notification(observed: String, action: Int, value: String)
