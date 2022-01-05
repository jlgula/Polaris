package org.opendcgrid.app.polaris.device

import akka.Done
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.pattern.StatusReply
import org.opendcgrid.app.polaris.client.definitions.{Device => DeviceProperties}
import org.opendcgrid.app.polaris.client.device.{DeviceClient, PutPowerAcceptedResponse, PutPowerGrantedResponse}
import org.opendcgrid.app.polaris.command.CommandError

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object CapacityManagerActor {
  sealed trait Command
  final case class AddDevice(properties: DeviceProperties, replyTo: ActorRef[StatusReply[Done]]) extends Command
  final case class RemoveDevice(id: DeviceID, replyTo: ActorRef[StatusReply[Done]]) extends Command
  private final case class WrappedResult(result: StatusReply[Done], replyTo: ActorRef[StatusReply[Done]]) extends Command

  def apply(deviceClient: DeviceClient): Behavior[Command] = Behaviors.setup[Command](context => new CapacityManagerActor(context, deviceClient))

  class CapacityManagerActor(context: ActorContext[Command], val deviceClient: DeviceClient) extends AbstractBehavior[Command](context) {
    implicit private val ec: ExecutionContext = context.executionContext
    val manager = new CapacityManager(Nil, grantMethod, acceptMethod)
    context.log.info("CapacityManagerActor started")

    override def onMessage(msg: Command): Behavior[Command] = {
      msg match {
        case AddDevice(properties, replyTo) =>
          val futureResult = manager.addDevice(properties)
          context.pipeToSelf(futureResult) {
            case Success(_) => WrappedResult(StatusReply.Ack, replyTo)
            case Failure(error) => WrappedResult(StatusReply.error(error), replyTo)
          }
          this
        case RemoveDevice(id, replyTo) =>
          val futureResult = manager.removeDevice(id)
          context.pipeToSelf(futureResult) {
            case Success(_) => WrappedResult(StatusReply.Ack, replyTo)
            case Failure(error) => WrappedResult(StatusReply.error(error), replyTo)
          }
          this
        case WrappedResult(result, replyTo) =>
          replyTo ! result
          this
      }
    }

    override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
      case PostStop =>
        context.log.info("IoT Application stopped")
        this
    }

    private def grantMethod(id: DeviceID, value: PowerValue): Future[Unit] = {
      deviceClient.putPowerGranted(id, value).value.map {
        case Right(PutPowerGrantedResponse.NoContent) => ()
        case other => CommandError.UnexpectedResponse(other.toString)
      }
    }

    private def acceptMethod(id: DeviceID, value: PowerValue): Future[Unit] = {
      deviceClient.putPowerAccepted(id, value).value.map {
        case Right(PutPowerAcceptedResponse.NoContent) => ()
        case other => CommandError.UnexpectedResponse(other.toString)
      }
    }

  }

}



