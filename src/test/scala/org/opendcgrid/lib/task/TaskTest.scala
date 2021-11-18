package org.opendcgrid.lib.task

import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.server.ServerError

import java.util.concurrent.Semaphore
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class TaskTest extends org.scalatest.funsuite.AnyFunSuite {
  test("minimal") {
    val manager = new TaskManager
    val uri = Uri("http://localhost:8080")
    val task = new TestTask("test", uri, manager)
    val startFuture = task.start()
    val taskID = Await.result(startFuture, Duration.Inf)
    val taskMap = manager.listTasks
    assert(taskMap.contains(taskID))
    val getResult = manager.getTask(taskID)
    assert(getResult.nonEmpty)
    assertResult(task.name)(getResult.get.name)
    assertResult(task.uri)(getResult.get.uri)
    val terminateFuture = task.terminate()
    Await.result(terminateFuture, Duration.Inf)
    assert(manager.listTasks.isEmpty)
    val getResult2 = manager.getTask(taskID)
    assert(getResult2.isEmpty)
  }

  test("terminate invalid taskID") {
    val manager = new TaskManager
    val badID = TaskID(0)
    val future = manager.terminateTask(badID)
    val futureResult = Try(Await.result(future, Duration.Inf))
    futureResult match {
      case Success(_) => fail("expected failure not delivered")
      case Failure(ServerError.NotFound(badID)) => // Pass
      case Failure(error) => fail(s"unexpected error: $error")
    }
  }
}



class TestTask(val name: String, val uri: Uri, taskManager: TaskManager) extends Task {
  private val semaphore = new Semaphore(0)
  private val completionPromise = Promise[Unit]()
  private val task = this
  def start(): Future[TaskID] = {
    val taskID = taskManager.startTask(task)
    val thread = new Thread {
      override def run(): Unit = {
        semaphore.acquire()
        taskManager.endTask(taskID)
        completionPromise.success(())
      }
    }
    thread.start()
    Future.successful(taskID)
  }

  override def terminate(): Future[Unit] = {
    semaphore.release()
    completionPromise.future
  }
}

