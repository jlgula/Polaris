package org.opendcgrid.app.polaris.command

import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.command.CommandTestUtilities.TestCommandContext
import org.opendcgrid.lib.task.{Task, TaskID, TestTask}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class DevicesCommandTest extends org.scalatest.funsuite.AnyFunSuite {
  test("parse") {
    val parseResult = DevicesCommand.parse(Seq(DevicesCommand.name))
    assertResult(Success(DevicesCommand))(parseResult)
  }

  test("devices command empty") {
    val context = new TestCommandContext()
    val command = DevicesCommand
    val result = command.run(context)
    result match {
      case Failure(error) => fail(error.getMessage)
      case Success(CommandResponse.MultiResponse(responses)) => assert(responses.isEmpty)
      case Success(other) => fail(s"Unexpected response: $other")
    }
  }

  test("devices command with values") {
    val context = new TestCommandContext()
    val manager = context.taskManager
    val uri1 = Uri("http://localhost:8080")
    val task1 = new TestTask("test1", uri1, manager)
    val startFuture1 = task1.start()
    val taskID1 = Await.result(startFuture1, Duration.Inf)
    val uri2 = Uri("http://localhost:8081")
    val task2 = new TestTask("test2", uri2, manager)
    val startFuture2 = task2.start()
    val taskID2 = Await.result(startFuture2, Duration.Inf)
    val command = DevicesCommand
    val result = command.run(context)
    result match {
      case Failure(error) => fail(error.getMessage)
      case Success(CommandResponse.MultiResponse(responses)) =>
        assertResult(2)(responses.size)
        compareResponseToTask(responses.head.asInstanceOf[CommandResponse.TaskResponse], taskID1, task1)
        compareResponseToTask(responses(1).asInstanceOf[CommandResponse.TaskResponse], taskID2, task2)
      case Success(other) => fail(s"Unexpected response: $other")
    }
  }

  def compareResponseToTask(response: CommandResponse.TaskResponse, id: TaskID, task: Task): Unit = {
    assertResult(id)(response.id)
    assertResult(task.name)(response.name)
    assertResult(task.uri)(response.uri)
  }

}