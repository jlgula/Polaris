package org.opendcgrid.lib.task
import org.opendcgrid.app.polaris.server.ServerError

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ExecutionContext, Future}

class TaskManager(implicit ec: ExecutionContext) {
  class ForceableSemaphore(available: Int) extends java.util.concurrent.Semaphore(available) {
    def forceAcquire(): Unit = reducePermits(1)
  }

  private val nextID = new AtomicInteger(0)
  private val waitSemaphore = new ForceableSemaphore(1)
  private val tasks = scala.collection.concurrent.TrieMap[TaskID, Task]()


  def startTask(task: Task): TaskID = {
    val id = TaskID(nextID.incrementAndGet())
    tasks.put(id, task)
    waitSemaphore.forceAcquire()
    id
  }

  def endTask(id: TaskID): Unit = {
    tasks.remove(id)
    waitSemaphore.release()
  }

  def terminateTask(id: TaskID): Future[Unit] = {
    //tasks(id).terminate()

    if (tasks.contains(id)) tasks(id).terminate()
    else Future.failed(ServerError.NotFound(id))


  }

  def getTask(id: TaskID): Option[Task] = tasks.get(id)

  def listTasks: Map[TaskID, Task] = tasks.toMap

  def waitForComplete(): Unit = waitSemaphore.acquire()

  def terminateAll(): Future[Unit] = {
    val futures = tasks.map{case (id, _) => terminateTask(id) }.toSeq
    Future.sequence(futures).map(_ => ())
  }
}

