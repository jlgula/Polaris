package org.opendcgrid.app.polaris.server.subscription

sealed abstract class NotificationAction(val value: String)
object NotificationAction {
  case object Put extends NotificationAction("Put")
  case object Post extends NotificationAction("Post")
  case object Delete extends NotificationAction("Delete")

  def find(value: String): Option[NotificationAction] = all.find(_.value == value)
  val all = Seq(Put, Put, Delete)
}
