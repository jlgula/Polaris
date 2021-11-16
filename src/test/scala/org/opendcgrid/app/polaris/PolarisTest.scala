package org.opendcgrid.app.polaris

import org.opendcgrid.app.polaris.shell.ShellContext

import java.io.{BufferedReader, ByteArrayInputStream, ByteArrayOutputStream, InputStreamReader, PrintStream}

class PolarisTest extends org.scalatest.funsuite.AnyFunSuite {
  test("minimal") {
    val app = new Polaris(new ShellContext())
    val result = app.run(Nil)
    assertResult(0)(result)
  }

  test("help option") {
    val fixture = new PolarisTestFixture()
    fixture.run(Seq("--help"))
    assert(fixture.output.nonEmpty)
  }

  test("version option") {
    val fixture = new PolarisTestFixture()
    fixture.run(Seq("--version"))
    assert(fixture.output.nonEmpty)
  }
}
