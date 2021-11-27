package org.opendcgrid.app.polaris.command

import akka.actor.ActorSystem
import akka.http.scaladsl.coding.Coders
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.{Http, model}
import akka.stream.StreamTcpException
import org.opendcgrid.app.polaris.command.Command.parseErrors
import org.opendcgrid.lib.commandoption.CommandOptionResult

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case object GetCommand extends Parsable {
  val name = "get"
  val help = "get <uri> - get a value from network"
  val defaultPort: Int = 0
  val missingArgumentName = "target URI"


  override def parse(arguments: Seq[String]): Try[Command] = {
    val options = Nil //Seq[PolarisAppOptionTag](Nil)
    val result = CommandOptionResult.parse(arguments, options)
    for {
      _ <- parseErrors(result) // Bail out if any errors in find
      target <- parseTarget(result)
    } yield GetCommand(target)
  }

  def parseTarget(results: CommandOptionResult): Try[String] = {
    results.values.size match {
      case 0 => Failure(CommandError.MissingArgument(missingArgumentName))
      case 1 => Success(results.values.head)
      case _ => Failure(CommandError.UnexpectedParameters(results.values.tail))
    }
  }
}

case class GetCommand(target: String) extends Command {
  import akka.http.scaladsl.model._
  import HttpMethods._

  import scala.concurrent.duration._

  def run(context: CommandContext): Try[CommandResponse.TextResponse] = {
    implicit def system: ActorSystem = context.actorSystem
    implicit def ec: ExecutionContext = system.dispatcher
    val bodyFuture = for {
      uri <- CommandUtilities.getURI(context, target)
      response <- Http().singleRequest(model.HttpRequest(GET, uri))
      text <- validateResponse(uri, context, response)
    } yield text
    Try(Await.ready(bodyFuture, Duration.Inf)) match {
      case Success(f) => f.value.get match {
        case Success(text) => Success(CommandResponse.TextResponse(text))
        case Failure(error: IllegalUriException) => Failure(CommandError.InvalidURL(target, error))
        case Failure(error: StreamTcpException) => Failure(CommandError.InvalidURL(target, error))
        case Failure(error: CommandError) => Failure(error)
        case Failure(error) => throw new IllegalStateException(s"unexpected error: $error")
      }
      case Failure(error) => throw new IllegalStateException(s"unexpected error: $error")
    }
  }

  def validateResponse(uri: Uri, context: CommandContext, response: HttpResponse): Future[String] =  response.status match {
    case StatusCodes.OK => decodeResponse(context, response)
    case StatusCodes.NoContent => FastFuture.successful("")
    case StatusCodes.NotFound => FastFuture.failed(CommandError.NotFound(uri.toString()))
    case _ =>
      if (response.status.isFailure()) FastFuture.failed(CommandError.UnexpectedResponse(response.status.reason()))
      else FastFuture.successful("")
  }

  def decodeResponse(context: CommandContext, response: HttpResponse): Future[String] = {
    implicit def system: ActorSystem = context.actorSystem
    implicit def ec: ExecutionContext = system.dispatcher
    val decoder = response.encoding match {
      case HttpEncodings.gzip =>
        Coders.Gzip
      case HttpEncodings.deflate =>
        Coders.Deflate
      case HttpEncodings.identity =>
        Coders.NoCoding
      case _ =>
        Coders.NoCoding
    }

    val decodedResponse = decoder.decodeMessage(response)
    Unmarshal(decodedResponse).to[String]
  }
}
