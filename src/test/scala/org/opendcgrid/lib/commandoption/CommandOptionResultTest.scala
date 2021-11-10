package org.opendcgrid.lib.commandoption

import org.opendcgrid.lib.commandoption.CommandOptionError.{MissingOptionArgument, UnrecognizedOption}

class CommandOptionResultTest extends org.scalatest.funsuite.AnyFunSuite {
  import org.opendcgrid.lib.commandoption.CommandOptionResult.parse
  import org.opendcgrid.lib.commandoption.StandardCommandOptionTag._

  test("CommandOptionResult") {
    assertResult(CommandOptionResult())(parse(Nil))
    val name1 = "foo"
    val name2 = "bar"
    assertResult(CommandOptionResult(Seq(name1)))(parse(Seq(name1)))
    assertResult(CommandOptionResult(Seq(name1, name2)))(parse(Seq(name1, name2)))
  }

  test("CommandOptionResult options") {
    val options = Seq(Help, Version)
    val name1 = "foo"
    val nameList1 = Seq(name1)
    assertResult(CommandOptionResult())(parse(Nil, options))
    assertResult(CommandOptionResult(options = Seq(StandardCommandOption.Help)))(parse(Seq("--help"), options))
    assertResult(CommandOptionResult(options = Seq(StandardCommandOption.Help)))(parse(Seq("-h"), options))
    assertResult(CommandOptionResult(nameList1, options = Seq(StandardCommandOption.Help)))(parse(Seq("-h", name1), options))
    assertResult(CommandOptionResult(options = Seq(StandardCommandOption.Help, StandardCommandOption.Version)))(parse(Seq("-h", "-v"), options))
  }

  test("CommandOptionResult errors") {
    val options = Seq(Help, Version)
    val name1 = "foo"
    val nameList1 = Seq(name1)
    val expectedErrors = Seq(UnrecognizedOption("z"))
    val result = parse(Seq("-z"), options)
    assertResult(CommandOptionResult(errors = expectedErrors))(result)
  }

  test("CommandOptionResult option with parameter") {
    val options = Seq(Output)
    val name1 = "foo"
    //val nameList1 = Seq(name1)
    val result = parse(Seq("-o", name1), options)
    val expectedOptions = Seq(StandardCommandOption.Output(name1))
    val expected = CommandOptionResult(Nil, expectedOptions)
    assertResult(expected)(result)
    val result2 = parse(Seq("-o"), options)
    val expected2 = CommandOptionResult(Nil, Nil, Seq(MissingOptionArgument("output")))
    assertResult(expected2)(result2)
  }

  test("CommandOptionResult multishort") {
    val options = Seq(Help, Version)
    val name1 = "foo"
    val result = parse(Seq("-hv", name1), options)
    val expectedOptions = Seq(StandardCommandOption.Help, StandardCommandOption.Version)
    val expected = CommandOptionResult(Seq(name1), expectedOptions)
    assertResult(expected)(result)
    val options2 = Seq(Help, Output)
    val result2 = parse(Seq("-oh", name1), options2)
    val expectedOptions2 = Seq(StandardCommandOption.Output(name1), StandardCommandOption.Help)
    val expected2 = CommandOptionResult(Nil, expectedOptions2)
    assertResult(expected2)(result2)
  }

  test("CommandOptionResult zero argument flag match") {
    val options = Seq(Verbose)
    val result = parse(Seq("-V"), options)
    result match {
      case Verbose(flag) => assert(flag == StandardCommandOption.Verbose)
      case _ => fail("option not matched")
    }
  }

}