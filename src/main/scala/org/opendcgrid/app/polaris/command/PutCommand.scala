package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.{Http, model}
import akka.stream.StreamTcpException
import akka.util.ByteString
import org.opendcgrid.app.polaris.command.Command.parseErrors
import org.opendcgrid.lib.commandoption.CommandOptionResult

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case object PutCommand extends Parsable {
  val name = "put"
  val help = "put <uri> <JSONValue> - put a value to network"
  val missingArgumentName = "target URI"
  val missingArgumentName2 = "JSONValue"


  override def parse(arguments: Seq[String]): Try[Command] = {
    val options = Nil //Seq[PolarisAppOptionTag](Nil)
    val result = CommandOptionResult.parse(arguments, options)
    for {
      _ <- parseErrors(result) // Bail out if any errors in find
      arguments <- parseArguments(result)
    } yield PutCommand(arguments._1, arguments._2)
  }

  def parseArguments(results: CommandOptionResult): Try[(String, String)] = {
    results.values.size match {
      case 0 => Failure(CommandError.MissingArgument(missingArgumentName))
      case 1 => Failure(CommandError.MissingArgument(missingArgumentName2))
      case 2 => Success((results.values.head, results.values(1)))
      case _ => Failure(CommandError.UnexpectedParameters(results.values.drop(2)))
    }
  }
}

case class PutCommand(target: String, value: String) extends Command {
  import akka.http.scaladsl.model._
  import HttpMethods._

  import scala.concurrent.duration._

  def run(context: CommandContext): Try[CommandResponse] = {
    implicit def system: ActorSystem = context.actorSystem
    implicit def ec: ExecutionContext = system.dispatcher
    val responseFuture = for {
      uri <- CommandUtilities.getURI(context, target)
      entity <- parseValue(value)
      response <- Http().singleRequest(model.HttpRequest(PUT, uri, entity = entity))
    } yield response
    Try(Await.ready(responseFuture, Duration.Inf)) match {
      case Success(f) => f.value.get match {
        case Success(_) => Success(CommandResponse.NullResponse)
        case Failure(error: IllegalUriException) => Failure(CommandError.InvalidURL(target, error))
        case Failure(error: StreamTcpException) => Failure(CommandError.InvalidURL(target, error))
        case Failure(error: CommandError) => Failure(error)
        case Failure(error) => throw new IllegalStateException(s"unexpected error: $error")
      }
      case Failure(error) => throw new IllegalStateException(s"unexpected error: $error")
    }
  }

  def parseValue(value: String): Future[HttpEntity.Strict] = {
    import io.circe.parser._

    parse(value) match {
      case Right(json) => FastFuture.successful(HttpEntity.Strict(ContentType(MediaTypes.`application/json`), ByteString(json.toString())))
      case Left(error) => FastFuture.failed(CommandError.InvalidJSONValue(value, error.getMessage()))
    }
  }
}
