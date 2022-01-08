package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.pattern.StatusReply
import akka.util.Timeout
import org.opendcgrid.app.polaris.PolarisHandler
import org.opendcgrid.app.polaris.server.definitions.{Notification, Subscription}
import org.opendcgrid.app.polaris.server.notification.NotificationClient
import org.opendcgrid.app.polaris.server.subscription.{SubscriptionHandler, SubscriptionResource}

import java.util.UUID
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

/**
 * An actor version of the subscription handler that deals with external HTTP subscriptions.
 */
object GCSubscriptionHandlerActor {
  sealed trait Command
  final case class AddSubscription(subscription: Subscription, replyTo: ActorRef[AddSubscriptionResponse]) extends Command
  final case class RemoveDevice(id: DeviceID, replyTo: ActorRef[StatusReply[Done]]) extends Command
  private final case class WrappedResult(result: StatusReply[Done], replyTo: ActorRef[StatusReply[Done]]) extends Command

  final case class AddSubscriptionResponse(id: DeviceID)

  class Handler(context: ActorContext[NotificationAdapterActor.Command], manager: ActorRef[SubscriptionManagerActor.Command])(implicit requester: HttpRequest => Future[HttpResponse]) extends SubscriptionHandler with PolarisHandler with Notifier {
    implicit private val ec: ExecutionContext = context.executionContext
    implicit private val system: ActorSystem[Nothing] = context.system
    implicit private val timeout: Timeout = FiniteDuration(3, SECONDS)

    override def addSubscription(respond: SubscriptionResource.AddSubscriptionResponse.type)(body: Subscription): Future[SubscriptionResource.AddSubscriptionResponse] = {
      val id = UUID.randomUUID().toString
      val observed = Uri(body.observedUrl) // TODO: error checking!
      val observer = Uri(body.observerUrl) // TODO: error checking!
      val action = NotificationAction.Post
      val client = NotificationClient(observer.toString())
      val clientActor = context.spawn(NotificationAdapterActor(client), s"Adapter for: ${observer.toString()}")
      val wrapper = context.spawn(NotificationAdapterActor.wrapper(clientActor), "Wrapper")
      val result = manager.ask(replyTo => SubscriptionManagerActor.AddSubscription(SubscriptionManagerActor.Subscription(id, observed, action, observer, wrapper), replyTo))
      result.map {
        case StatusReply.Success(_) => respond.Created(id)  // TODO: Don't understand why id is an any
        case StatusReply.Error(error) => respond.BadRequest(error.toString)
      }
    }

    override def listSubscriptions(respond: SubscriptionResource.ListSubscriptionsResponse.type)(): Future[SubscriptionResource.ListSubscriptionsResponse] = {
      val result = manager.ask(replyTo => SubscriptionManagerActor.ListSubscriptions(replyTo))
      result.map(response => respond.OK(response.subscriptions.map(convertSubscription).toVector))
    }

    override def deleteSubscription(respond: SubscriptionResource.DeleteSubscriptionResponse.type)(id: String): Future[SubscriptionResource.DeleteSubscriptionResponse] = {
      manager.ask(replyTo => SubscriptionManagerActor.RemoveSubscription(id, replyTo)).map {
        case StatusReply.Success(_) => respond.NoContent
        case StatusReply.Error(error) => respond.NotFound(error.getMessage)
      }
    }

    override def reset(): Unit = {
      manager ! SubscriptionManagerActor.Reset
    }

    override def notify(notification: Notification): Future[Unit] = {
      val observed = Uri.Path(notification.observed)  // TODO: error check!
      val action = NotificationAction.find(notification.action).get  // TODO: error check!
      val typedNotification = NotificationProtocol.Notification(observed, action, notification.value)
      manager.ask(replyTo => SubscriptionManagerActor.Notify(typedNotification, replyTo)).map {
        case StatusReply.Success(_) => ()
        case StatusReply.Error(error) => Failure(error)
      }
    }

    private def convertSubscription(subscription: SubscriptionManagerActor.Subscription): Subscription = {
      Subscription(subscription.observed.toString(), subscription.observer.toString())
    }
  }
}
