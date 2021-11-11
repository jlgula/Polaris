package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.{AppContext, GenericAppContext}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

// Test utilities for commands.

object CommandTestUtilities {
  def makeShell(input: String = "", configuration: ShellConfiguration = ShellConfiguration()): Shell = {
    val in = new ByteArrayInputStream(input.getBytes)
    val output = new ByteArrayOutputStream()
    val error = new ByteArrayOutputStream()
    Shell(new GenericAppContext(configuration), in, output, error)
  }

  class ShellTextFixture(input: String = "", configuration: ShellConfiguration = ShellConfiguration(), context: Option[AppContext] = None) {
    val inputStream = new ByteArrayInputStream(input.getBytes)
    val outputStream = new ByteArrayOutputStream()
    val errorStream = new ByteArrayOutputStream()
    val appContext: AppContext = context.getOrElse(new GenericAppContext(configuration))
    val shell: Shell = Shell(appContext, inputStream, outputStream, errorStream)

    def output: String = outputStream.toString()
    def error: String = errorStream.toString
  }
}


