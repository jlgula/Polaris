package org.opendcgrid.app.polaris

import akka.actor.ActorSystem
import org.opendcgrid.app.polaris.PolarisAppOptionTag.{Client, DevicesOption, Log, Server}
import org.opendcgrid.app.polaris.command.{Command, CommandError, DevicesCommand, ExitCommand, HelpCommand, Parsable, ServerCommand, VersionCommand}
import org.opendcgrid.app.polaris.shell.{Shell, ShellConfiguration, ShellContext}
import org.opendcgrid.lib.commandoption.StandardCommandOptionTag.{Help, Output, Version}
import org.opendcgrid.lib.commandoption.{CommandOptionError, CommandOptionResult, StandardCommandOption}
import org.opendcgrid.lib.task.TaskManager

import java.io.{BufferedReader, PrintStream}
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success, Try}

object Polaris extends App {
  val app = new Polaris(new JVMAppContext())
  val result = app.run(this.args.toIndexedSeq, Console.in, Console.out, Console.err)
  System.exit(result)
}

class Polaris(context: AppContext) {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
  def options = Seq(Client, DevicesOption, Help, Log, Output, Server, PolarisAppOptionTag.Shell, Version)
  private val tm = new TaskManager

  /**
   * Runs the application.
   *
   * @param args command line arguments that are parsed and passed to the application
   * @param in   [[BufferedReader]] that provides input
   * @param out  [[PrintStream]] used to display normal output
   * @param err  [[PrintStream]] used to display error messages
   * @return The exit code for the application. An exit code of 0 means normal exit. Non zero values mean various errors. See TBD for specifics.
   */
  def run(args: Seq[String], in: BufferedReader, out: PrintStream, err: PrintStream): Int = {
    val result = CommandOptionResult.parse(args, options)
    val commandErrors = result.errors.collect { case e: CommandOptionError => mapError(e) }
    if (commandErrors.nonEmpty) {
      commandErrors.foreach(reportAppError(err, _))
      commandErrors.head.exitCode // Value is first error exit code
    } else runConfiguration(result, in, out, err)
  }

  private def runConfiguration(result: CommandOptionResult, in: BufferedReader, out: PrintStream, err: PrintStream): Int = {
    assert(result.errors.isEmpty)
    result match {
      case _ if result.options.contains(StandardCommandOption.Help) => runShellCommand(HelpCommand(Nil), in, out, err)
      case _ if result.options.contains(StandardCommandOption.Version) => runShellCommand(VersionCommand(), in, out, err)
      case _ if result.options.contains(PolarisAppOption.Devices) => runShellCommand(DevicesCommand, in, out, err)
      case _ if result.options.contains(PolarisAppOption.Server) => runShellCommand(ServerCommand, in, out, err)
      case _ if result.options.contains(PolarisAppOption.Shell) => runShell(in, out, err)
      //case _ if result.values.isEmpty => runShell(in, out, err)
      //case _ => runShellFiles(result.values, result.options, in, out, err)
      case _ => 0 // Do nothing throw new IllegalStateException("not yet")
    }
  }


  private def runShell(in: BufferedReader, out: PrintStream, err: PrintStream): Int = {
    //val isConsole: Boolean = context.isConsole // System.console() fails while running under Intellij
    val shell = new Shell(makeShellContext(), in, out, err)
    shell.run()
    0
  }

  private def runShellCommand(command: Command, in: BufferedReader, out: PrintStream, err: PrintStream): Int = {
     val shell = new Shell(makeShellContext(), in, out, err)
    shell.runCommandAndDisplay(command) match {
      case Success(_) => 0
      case Failure(error: CommandError) => error.exitCode
      case Failure(e) => throw new IllegalStateException(s"Unexpected error: $e")
    }
  }

  private def reportAppError(stream: PrintStream, error: CommandError): Int = {
    error match {
      case CommandError.MultiError(errors) => errors.foreach(reportAppError(stream, _)); errors.head.exitCode
      case e => stream.print("TBD"); e.exitCode
    }
  }

  private def mapError(error: CommandOptionError): CommandError = error match {
    case e: CommandOptionError.MultiError => CommandError.MultiError(e.errors.map(mapError))
    case e: CommandOptionError.UnrecognizedOption => CommandError.UnsupportedOption(e.optionName)
    case e: CommandOptionError.MissingOptionArgument => CommandError.MissingArgument(e.optionName)
  }

  private def makeShellContext(): ShellContext = new ShellContext {
    override def allCommands: Seq[Parsable] = Seq[Parsable](
      DevicesCommand,
      ExitCommand,
      HelpCommand,
      ServerCommand,
      VersionCommand
    )


    override def taskManager: TaskManager = tm

    override def writeFile(fileName: String, data: Array[Byte]): Try[Unit] = context.writeFile(fileName, data)

    override def readFile(fileName: String): Try[Array[Byte]] = context.readFile(fileName)

    override def configuration: ShellConfiguration = ShellConfiguration(enablePrompt = true)
  }
}
