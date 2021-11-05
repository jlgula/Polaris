package org.opendcgrid.app.polaris.subscription

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.opendcgrid.app.polaris.definitions.{Notification, Subscription}
import org.opendcgrid.app.polaris.notification.{NotificationClient, PostNotificationResponse}
import org.opendcgrid.app.polaris.{HTTPError, PolarisHandler}

import java.util.UUID
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class PolarisSubscriptionHandler(sys: ActorSystem) extends SubscriptionHandler with PolarisHandler {
  implicit val system: ActorSystem = sys
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val requester: HttpRequest => Future[HttpResponse] = Http().singleRequest(_)
  private val subscriptions = mutable.HashMap[String, Subscription]()
  def notify(notification: Notification): Future[Unit] = {
    /*
    subscriptions.values.filter(_.observedUrl == notification.observed).foreach { subscription =>
      println(s"Notification: $subscription, $notification")
      val result = NotificationClient(subscription.observerUrl).postNotification(notification)
      val xx = Await.result(result.value, Duration(1, "seconds"))
      println(s"notify: $xx")

     */
      /*
      result.value.onComplete{
        case Success(Right(PostNotificationResponse.NoContent(_))) =>  // Done - nothing to do
        case Success(Right(PostNotificationResponse.BadRequest(message))) => throw new IllegalStateException(s"PolarisSubscriptionHandler.notify failed: $message")
        case Success(Left(Left(throwable))) => throw new IllegalStateException(s"PolarisSubscriptionHandler.notify failed: $throwable")
        case Success(Left(Right(response))) => throw new IllegalStateException(s"PolarisSubscriptionHandler.notify failed - unexpected response $response")
        case Failure(other) => throw new IllegalStateException(s"PolarisSubscriptionHandler.notify failed: $other")
      }

       */
    val subs = subscriptions.values.filter(_.observedUrl == notification.observed)
    val xx = subs.map(subscription => NotificationClient(subscription.observerUrl).postNotification(notification)).map(_.value)
    val yy = Future.sequence(xx)
    // PostNotificationResponse.NoContent: PostNotificationResponse
    yy.map(aa => aa.foreach(_ == Right(PostNotificationResponse.NoContent)))
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
