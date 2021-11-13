package org.opendcgrid.app.polaris

import java.io.{BufferedReader, ByteArrayInputStream, ByteArrayOutputStream, InputStreamReader, PrintStream}

class PolarisTest extends org.scalatest.funsuite.AnyFunSuite {
  test("minimal") {
    val app = new Polaris(new JVMAppContext())
    val result = app.run(Nil, Console.in, Console.out, Console.err)
    assertResult(0)(result)
  }

  test("help option") {
    val fixture = new TextFixture()
    fixture.run(Seq("--help"))
    assert(fixture.output.nonEmpty)
  }

  test("version option") {
    val fixture = new TextFixture()
    fixture.run(Seq("--version"))
    assert(fixture.output.nonEmpty)
  }


  class TextFixture(input: String = "") {
    val polaris = new Polaris(new JVMAppContext())
    val inputStream = new ByteArrayInputStream(input.getBytes)
    val outputStream = new ByteArrayOutputStream()
    val errorStream = new ByteArrayOutputStream()
    val reader = new BufferedReader(new InputStreamReader(inputStream))
    def run(arguments: Seq[String] = Nil): Int = polaris.run(arguments, reader, new PrintStream(outputStream), new PrintStream(errorStream))

    def output: String = outputStream.toString()
    def error: String = errorStream.toString
  }

}
