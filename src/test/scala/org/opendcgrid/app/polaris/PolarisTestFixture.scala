package org.opendcgrid.app.polaris

import org.opendcgrid.app.polaris.shell.{ShellConfiguration, ShellContext}

import java.io.{BufferedReader, ByteArrayInputStream, ByteArrayOutputStream, InputStreamReader, PrintStream}
import scala.util.Try


class PolarisTestFixture(input: String = "", val configuration: ShellConfiguration = ShellConfiguration()) extends AppContext {
  val inputStream = new ByteArrayInputStream(input.getBytes)
  val outputStream = new ByteArrayOutputStream()
  val errorStream = new ByteArrayOutputStream()
  val reader = new BufferedReader(new InputStreamReader(inputStream))
  val polaris = new Polaris(this)

  def run(arguments: Seq[String] = Nil): Int = polaris.run(arguments)

  def output: String = outputStream.toString()

  def error: String = errorStream.toString

  override def writeFile(fileName: String, data: Array[Byte]): Try[Unit] = throw new UnsupportedOperationException

  override def readFile(fileName: String): Try[Array[Byte]] = throw new UnsupportedOperationException

  override def in: BufferedReader = new BufferedReader(new InputStreamReader(inputStream))

  override def out: PrintStream = new PrintStream(outputStream)

  override def err: PrintStream = new PrintStream(errorStream)
}
