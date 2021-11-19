package org.opendcgrid.app.polaris

import akka.actor.ActorSystem
import org.opendcgrid.app.polaris.PolarisAppOptionTag.{Client, DevicesOption, Log, Server}
import org.opendcgrid.app.polaris.command.{Command, CommandError, DevicesCommand, ExitCommand, HaltCommand, HelpCommand, Parsable, ServerCommand, VersionCommand}
import org.opendcgrid.app.polaris.shell.{Shell, ShellConfiguration, ShellContext}
import org.opendcgrid.lib.commandoption.StandardCommandOptionTag.{Help, Output, Version}
import org.opendcgrid.lib.commandoption.{CommandOptionError, CommandOptionResult, StandardCommandOption}
import org.opendcgrid.lib.task.TaskManager

import java.io.{BufferedReader, PrintStream}
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success, Try}

object Polaris extends App {
  val app = new Polaris(new JVMAppContext())
  val result = app.run(this.args.toIndexedSeq)
  System.exit(result)
}

class Polaris(context: AppContext) extends ShellContext {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
  override val taskManager: TaskManager = new TaskManager

  override def allCommands: Seq[Parsable] = Seq[Parsable](
    DevicesCommand,
    ExitCommand,
    HaltCommand,
    HelpCommand,
    ServerCommand,
    VersionCommand
  )

  def options = Seq(Client, DevicesOption, Help, Log, Output, Server, PolarisAppOptionTag.Shell, Version)

  override def writeFile(fileName: String, data: Array[Byte]): Try[Unit] = context.writeFile(fileName, data)

  override def readFile(fileName: String): Try[Array[Byte]] = context.readFile(fileName)

  override def configuration: ShellConfiguration = context.configuration

  override def in: BufferedReader = context.in

  override def out: PrintStream = context.out

  override def err: PrintStream = context.err


  /**
   * Runs the application.
   *
   * @param args command line arguments that are parsed and passed to the application
   * @return The exit code for the application. An exit code of 0 means normal exit. Non zero values mean various errors. See TBD for specifics.
   */
  def run(args: Seq[String]): Int = {
    val result = CommandOptionResult.parse(args, options)
    val commandErrors = result.errors.collect { case e: CommandOptionError => mapError(e) }
    if (commandErrors.nonEmpty) {
      commandErrors.foreach(reportAppError)
      commandErrors.head.exitCode // Value is first error exit code
    } else runConfiguration(result)
  }

  private def runConfiguration(result: CommandOptionResult): Int = {
    // Start server if one is called for on the command line. Otherwise, do nothing.
    val serverResult = startServer(result)
    if (serverResult != 0) return serverResult

    assert(result.errors.isEmpty)
    result match {
      case _ if result.options.contains(StandardCommandOption.Help) => runShellCommand(HelpCommand(Nil))
      case _ if result.options.contains(StandardCommandOption.Version) => runShellCommand(VersionCommand())
      case _ if result.options.contains(PolarisAppOption.Devices) => runShellCommand(DevicesCommand)
      //case _ if result.options.contains(PolarisAppOption.Server) => runShellCommand(ServerCommand())
      case _ if result.options.contains(PolarisAppOption.Shell) => runShell()
      //case _ if result.values.isEmpty => runShell(in, out, err)
      //case _ => runShellFiles(result.values, result.options, in, out, err)
      case _ => 0 // Do nothing throw new IllegalStateException("not yet")
    }
  }


  private def startServer(result: CommandOptionResult): Int = {
    if (!result.options.contains(PolarisAppOption.Server)) 0
    else {
      ServerCommand.parsePort(result) match {
        case Failure(e: CommandError) => reportAppError(e); e.exitCode
        case Success(port) => runShellCommand(ServerCommand(port))
        case Failure(other) => throw new IllegalStateException(s"unexpected error: $other")
      }
    }
  }

  private def runShell(): Int = {
    val shell = new Shell(this)
    shell.run(prompt = true)
    0
  }

  private def runShellCommand(command: Command): Int = {
     val shell = new Shell(this)
    shell.runCommandAndDisplay(command) match {
      case Success(_) => 0
      case Failure(error: CommandError) => error.exitCode
      case Failure(e) => throw new IllegalStateException(s"Unexpected error: $e")
    }
  }

  private def reportAppError(error: CommandError): Int = {
    error match {
      case CommandError.MultiError(errors) => errors.foreach(reportAppError); errors.head.exitCode
      case e => context.err.print(error.getMessage); e.exitCode
    }
  }

  private def mapError(error: CommandOptionError): CommandError = error match {
    case e: CommandOptionError.MultiError => CommandError.MultiError(e.errors.map(mapError))
    case e: CommandOptionError.UnrecognizedOption => CommandError.UnsupportedOption(e.optionName)
    case e: CommandOptionError.MissingOptionArgument => CommandError.MissingArgument(e.optionName)
  }



}
