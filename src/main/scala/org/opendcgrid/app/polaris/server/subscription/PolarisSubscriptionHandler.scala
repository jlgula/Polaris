package org.opendcgrid.app.polaris.server.subscription

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.opendcgrid.app.polaris.server.notification.PostNotificationResponse
//import org.opendcgrid.app.polaris.client.definitions.Notification
//import org.opendcgrid.app.polaris.client.notification.NotificationResource.PostNotificationResponse
import org.opendcgrid.app.polaris.server.definitions.Subscription
import org.opendcgrid.app.polaris.server.definitions.Notification
import org.opendcgrid.app.polaris.server.notification.NotificationClient
import org.opendcgrid.app.polaris.{PolarisError, PolarisHandler}

import java.util.UUID
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

class PolarisSubscriptionHandler(implicit system: ActorSystem, requester: HttpRequest => Future[HttpResponse]) extends SubscriptionHandler with PolarisHandler {
  //implicit val system: ActorSystem = sys
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  //implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
  private val subscriptions = mutable.HashMap[String, Subscription]()
  private val clients = mutable.HashMap[String, NotificationClient]()

  /**
   * Notifies all observers that a resource has changed
   * @param notification a [[Notification]] that contains the observed resource and the new value.
   * @return a [[Future]] that contains all the responses when it completes
   */
  def notify(notification: Notification): Future[Iterable[Either[Either[Throwable, HttpResponse], PostNotificationResponse]]] = {
    // Find all the subscriptions that match the observed resource.
    val matchingSubscriptions = subscriptions.values.filter(_.observedUrl == notification.observed)
    // Post a notification to all observers and convert the resulting list of futures into a single future of the results.
    // Note that Future.sequence actually runs all the futures in parallel, not in series.
    val x = Future.sequence(matchingSubscriptions.map(subscription => clients(subscription.observerUrl).postNotification(notification)).map(_.value))
    x
  }


  override def addSubscription(respond: SubscriptionResource.AddSubscriptionResponse.type)(body: Subscription): Future[SubscriptionResource.AddSubscriptionResponse] = {
    val id = UUID.randomUUID().toString
    subscriptions.put(id, body)
    if (!clients.contains(body.observerUrl)) {
      clients.put(body.observerUrl, NotificationClient(body.observerUrl))
    }
    Future.successful(respond.Created(id))
  }

  override def listSubscriptions(respond: SubscriptionResource.ListSubscriptionsResponse.type)(): Future[SubscriptionResource.ListSubscriptionsResponse] = {
    Future.successful(respond.OK(subscriptions.values.toVector))
  }

  override def deleteSubscription(respond: SubscriptionResource.DeleteSubscriptionResponse.type)(id: String): Future[SubscriptionResource.DeleteSubscriptionResponse] = {
    if (subscriptions.contains(id)) {
      subscriptions.remove(id)
      Future.successful(respond.NoContent)
    } else Future.successful(respond.NotFound(PolarisError.NotFound(id).message))

  }

  override def reset(): Unit = subscriptions.clear()
}
