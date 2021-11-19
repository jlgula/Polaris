package org.opendcgrid.app.polaris.device

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.device.DeviceDescriptor.GC
import org.opendcgrid.app.polaris.server.{PolarisServer, ServerError}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class DeviceManager(implicit actorSystem: ActorSystem) {
  implicit val context: ExecutionContext = actorSystem.dispatcher
  case class Binding(name: String, descriptor: DeviceDescriptor, uri: Uri, binding: Http.ServerBinding)
  private val tasks = scala.collection.concurrent.TrieMap[String, Binding]()
  
  def startTask(descriptor: DeviceDescriptor, nameOption: Option[String], uri: Uri): Future[String] = {
    val name = nameOption.getOrElse(selectName(descriptor))
    if (tasks.contains(name)) return Future.failed(ServerError.DuplicateName(name))
    if (tasks.exists { case (_, binding) => binding.uri == uri }) return Future.failed(ServerError.DuplicateUri(uri))
    descriptor match {
      case GC => PolarisServer(uri, name).map(binding => Binding(name, descriptor, uri, binding)).andThen(bt => addBinding(bt)).map(_.name)
      case other => throw new IllegalStateException(s"unexpected descriptor $other")
    }
  }

  private def addBinding(bindingTry: Try[Binding]): Unit = bindingTry match {
    case Success(binding) => tasks.put(binding.name, binding)
    case Failure(_) => // Ignore
  }

  def terminateTask(name: String): Future[Unit] = {
    if (tasks.contains(name)) {
      tasks(name).binding.terminate(FiniteDuration(1, "seconds")).andThen(_ => removeBinding(name)).map(_ => ())
    }
    else Future.failed(ServerError.NotFound(name))
  }

  private def removeBinding(name: String): Unit = {
    val removeResult = tasks.remove(name)
    assert(removeResult.nonEmpty)
  }

  def listTasks: Iterable[(String, DeviceDescriptor, Uri)] = tasks.values.map(binding => (binding.name, binding.descriptor, binding.uri))

  def terminateAll(): Future[Unit] = {
    val futures = tasks.map{case (name, _) => terminateTask(name) }.toSeq
    Future.sequence(futures).map(_ => ())
  }

  def selectName(descriptor: DeviceDescriptor, index: Option[Int] = None): String = {
    if (index.isEmpty) {
      if (!tasks.contains(descriptor.name)) descriptor.name
      else selectName(descriptor, Some(1))
    } else {
      val candidate = s"${descriptor.name}${index.get}"
      if (!tasks.contains(candidate)) candidate
      else selectName(descriptor, Some(index.get + 1))
    }
  }
}

