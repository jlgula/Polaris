package org.opendcgrid.lib.task

import java.util.concurrent.Semaphore
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

class TaskTest extends org.scalatest.funsuite.AnyFunSuite {
  test("minimal") {
    val manager = new TaskManager
    val task = new TestTask("test", manager)
    val startFuture = task.start()
    val taskID = Await.result(startFuture, Duration.Inf)
    val taskMap = manager.listTasks
    assert(taskMap.contains(taskID))
    val terminateFuture = task.terminate()
    Await.result(terminateFuture, Duration.Inf)
    assert(manager.listTasks.isEmpty)
  }


  class TestTask(val name: String, taskManager: TaskManager) extends Task {
    private val semaphore = new Semaphore(0)
    private val completionPromise = Promise[Unit]
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

}
