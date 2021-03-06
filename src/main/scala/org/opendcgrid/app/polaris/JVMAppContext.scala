package org.opendcgrid.app.polaris

import org.opendcgrid.app.polaris.command.CommandError
import org.opendcgrid.app.polaris.shell.ShellConfiguration

import java.io.{BufferedReader, IOException, PrintStream}
import java.nio.file.{FileSystems, Files, InvalidPathException}
import scala.util.{Failure, Success, Try}

class JVMAppContext(
                   val in: BufferedReader = Console.in,
                   val out: PrintStream = Console.out,
                   val err: PrintStream = Console.err
                   ) extends AppContext {

  def writeFile(fileName: String, data: Array[Byte]): Try[Unit] = {
    try {
      val path = FileSystems.getDefault.getPath(fileName)
      Files.write(path, data)
      Success(())
    } catch {
      case _: InvalidPathException => Failure(CommandError.FileNotFound(fileName))
      case e: IOException => Failure(CommandError.FileIOError(fileName, e.getMessage))
    }
  }

  def readFile(fileName: String): Try[Array[Byte]] = {
    try {
      val path = FileSystems.getDefault.getPath(fileName)
      if (!Files.isReadable(path)) Failure(CommandError.FileNotFound(fileName))
      else {
        Success(Files.readAllBytes(path))
      }
    } catch {
      case _: InvalidPathException => Failure(CommandError.FileNotFound(fileName))
      case e: IOException => Failure(CommandError.FileIOError(fileName, e.getMessage))
    }
  }

  override def configuration: ShellConfiguration = ShellConfiguration(enablePrompt = true)
}
