package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.command.CommandTestUtilities.TestCommandContext

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class ServerCommandTest extends org.scalatest.funsuite.AnyFunSuite {

  test("server command") {
    val context = new TestCommandContext()
    val command = ServerCommand
    val result = command.run(context)
    result match {
      case Failure(error) => fail(error.getMessage)
      case Success(CommandResponse.TaskResponse(name, id, uri)) =>
        assertResult(ServerCommand.name)(name)
        assertResult(ServerCommand.uri)(uri)
        Await.result(context.taskManager.terminateTask(id), Duration.Inf)
      case Success(other) => fail(s"Unexpected response: $other")
    }

  }

}