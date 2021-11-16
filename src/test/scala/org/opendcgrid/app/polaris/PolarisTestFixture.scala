package org.opendcgrid.app.polaris

import org.opendcgrid.app.polaris.shell.{ShellConfiguration, ShellContext}

import java.io.{BufferedReader, ByteArrayInputStream, ByteArrayOutputStream, InputStreamReader, PrintStream}


class PolarisTestFixture(input: String = "", configuration: ShellConfiguration = ShellConfiguration()) {
  val inputStream = new ByteArrayInputStream(input.getBytes)
  val outputStream = new ByteArrayOutputStream()
  val errorStream = new ByteArrayOutputStream()
  val reader = new BufferedReader(new InputStreamReader(inputStream))
  val shellContext = new ShellContext(configuration, reader, new PrintStream(outputStream), new PrintStream(errorStream))
  val polaris = new Polaris(shellContext)

  def run(arguments: Seq[String] = Nil): Int = polaris.run(arguments)

  def output: String = outputStream.toString()
  def error: String = errorStream.toString
}
