package org.opendcgrid.app.polaris.subscription

import org.opendcgrid.app.polaris.{HTTPError, PolarisHandler}
import org.opendcgrid.app.polaris.definitions.Subscription

import java.util.UUID
import scala.collection.mutable
import scala.concurrent.Future

class PolarisSubscriptionHandler extends SubscriptionHandler with PolarisHandler {
  private val subscriptions = mutable.HashMap[String, Subscription]()
  def notify(notification: Notification): Unit = {
    subscriptions.values.filter(_.observedUrl == notification.observed).foreach(println(_)) // TODO: send message
  }

  override def addSubscription(respond: SubscriptionResource.AddSubscriptionResponse.type)(body: Subscription): Future[SubscriptionResource.AddSubscriptionResponse] = {
    val id = UUID.randomUUID().toString
    subscriptions.put(id, body)
    Future.successful(respond.Created(id))
  }

  override def listSubscriptions(respond: SubscriptionResource.ListSubscriptionsResponse.type)(): Future[SubscriptionResource.ListSubscriptionsResponse] = {
    Future.successful(respond.OK(subscriptions.values.toVector))
  }

  override def deleteSubscription(respond: SubscriptionResource.DeleteSubscriptionResponse.type)(id: String): Future[SubscriptionResource.DeleteSubscriptionResponse] = {
    if (subscriptions.contains(id)) {
      subscriptions.remove(id)
      Future.successful(respond.NoContent)
    } else Future.successful(respond.NotFound(HTTPError.NotFound(id).message))

  }

  override def reset(): Unit = subscriptions.clear()
}
