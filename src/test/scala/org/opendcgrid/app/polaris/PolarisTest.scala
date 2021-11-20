package org.opendcgrid.app.polaris

class PolarisTest extends org.scalatest.funsuite.AnyFunSuite {
  test("minimal") {
    val app = new Polaris(new PolarisTestFixture())
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

  test("start then halt server") {
    val fixture = new PolarisTestFixture()
    fixture.run(Seq("--server --halt"))
  }
}
