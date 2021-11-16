package org.opendcgrid.app.polaris

import org.opendcgrid.app.polaris.command.CommandError
import org.opendcgrid.app.polaris.shell.ShellConfiguration

import java.io.{BufferedReader, PrintStream}
import scala.util.{Failure, Try}
/**
 * Top level functions that may or may not be supplied by the platform.
 */
trait AppContext {
   /**
   * Writes data to a file, creating the file if necessary or over-writing any existing content.
   *
   * If the write cannot be performed, the method will return a Failure of
   *
   * @param fileName the name of the file as a pathname on the platform.
   * @param data  the byte array containing the data.
   * @return  a Success of Unit or [[Failure]] that should be a [[CommandError]]
   */
  def writeFile(fileName: String, data: Array[Byte]): Try[Unit]

  /**
   * Reads a file into a byte array.
   *
   * @param fileName  the path name of the file to read.
   * @return a [[Try]] of the contents of the file as a byte array or a [[Failure]] wrapping a [[CommandError]]
   */
  def readFile(fileName: String): Try[Array[Byte]]

  /**
   * @return the [[BufferedReader]] used by the shell, normally stdin
   */
  def in: BufferedReader

  /**
    * @return the [[PrintStream]] used for normal messages, normally stdout
   */
  def out: PrintStream

  /**
   * @return the [[PrintStream]] used for error messages, normally stderr
   */
  def err: PrintStream
}

class GenericAppContext(val configuration: ShellConfiguration = ShellConfiguration()) extends AppContext {

  override def writeFile(fileName: String, data: Array[Byte]): Try[Unit] = Failure(CommandError.UnsupportedOperation("file write"))

  override def readFile(fileName: String): Try[Array[Byte]] = Failure(CommandError.UnsupportedOperation("file read"))

  override def in: BufferedReader = Console.in

  override def out: PrintStream = Console.out

  override def err: PrintStream = Console.err
}
