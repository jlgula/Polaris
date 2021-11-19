package org.opendcgrid.app.polaris.shell

import org.opendcgrid.app.polaris.AppContext
import org.opendcgrid.app.polaris.command.{CommandContext, CommandError, Parsable}
import org.opendcgrid.app.polaris.device.DeviceManager

import java.io.{BufferedReader, PrintStream}
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Try}

trait ShellContext extends AppContext with CommandContext {
  def configuration: ShellConfiguration

  def in: BufferedReader

  def out: PrintStream

  def err: PrintStream

  //implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor

  override val taskManager: DeviceManager

  override def writeFile(fileName: String, data: Array[Byte]): Try[Unit] = Failure(CommandError.UnsupportedOperation("file write"))

  override def readFile(fileName: String): Try[Array[Byte]] = Failure(CommandError.UnsupportedOperation("file read"))

  override def allCommands: Seq[Parsable]
}
