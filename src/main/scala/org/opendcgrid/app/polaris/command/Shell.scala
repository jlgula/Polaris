package org.opendcgrid.app.polaris.command

import org.opendcgrid.app.polaris.AppContext

import java.io.{BufferedReader, InputStream, InputStreamReader, OutputStream, PrintStream}
import scala.util.{Failure, Success, Try}

object Shell {
  val rootURL = "/"
  val prompt = ">>"
  val appTag = "polaris"
  val defaultScheme = "sim"

  def apply(context: AppContext, input: InputStream, output: OutputStream, error: OutputStream): Shell = {
    val reader = new BufferedReader(new InputStreamReader(input))
    val writer = new PrintStream(output)
    val errorWriter = new PrintStream(error)
    new Shell(context, reader, writer, errorWriter)
  }

  def formatError(error: CommandError): String = s"$appTag: ${error.getMessage}\n"
}

class Shell(val context: AppContext, val reader: BufferedReader, val writer: PrintStream, val errorWriter: PrintStream) extends CommandContext {
  import org.opendcgrid.app.polaris.command.CommandResponse._
  val allCommands: Seq[Parsable] = Seq[Parsable](
    DevicesCommand,
    ExitCommand,
    HelpCommand,
    ServerCommand,
    VersionCommand
  )

  def configuration: ShellConfiguration = context.configuration

  def run(reader: BufferedReader = this.reader, prompt: Boolean = configuration.enablePrompt): Int = {
    var running: Boolean = true
    var exitCode: Int = 0
    while(running) {
      runReader(reader, prompt) match {
        case Success(CommandResponse.ExitResponse(code)) => exitCode = code; running = false
        case Success(CommandResponse.NullResponse) => exitCode = 0; running = false
        case other => throw new IllegalStateException(s"Unexpected runReader response: $other")
      }
    }

    // Clean up.
    exitCode
  }

  def runReader(reader: BufferedReader = this.reader, prompt: Boolean): Try[CommandResponse] = {
    var readerRunning: Boolean = true
    while (readerRunning) {
      if (prompt) {
        writer.print(Shell.prompt)
        writer.flush()
      }
      val line = reader.readLine()
      if (line != null) {
        parse(line) match {
          case Success(ExitCommand(exitCode)) => return Success(CommandResponse.ExitResponse(exitCode))
          case Success(command) => runCommandAndDisplay(command)
          case Failure(e: CommandError) => showResult(Failure(e))
          case Failure(error) => return Failure(CommandError.InternalError(error))
        }
      } else {
        readerRunning = false
      }
    }
    Success(CommandResponse.NullResponse)
  }

  def parse(line: String): Try[Command] = {
    val parts = line.split(' ').filter(_.nonEmpty).toSeq
    if (parts.isEmpty || parts.head.startsWith("#")) Success(NullCommand)
    else {
      val commandName = parts.head
      allCommands.find(_.name == commandName) match {
        case Some(parsable) => parsable.parse(parts.tail)
        case None => Failure(CommandError.InvalidCommand(commandName))
      }
    }
  }


  def showResult(result: Try[CommandResponse]): Unit = result match {
    case Success(NullResponse) => // Silently ignore
    case Success(ExitResponse(_)) => // Silently ignore
    case Success(MultiResponse(values)) => values.foreach(value => showResult(Success(value)))  // use showResult to ignore nulls
    //case Success(TraceResponse(response, trace)) => showTrace(trace); showResult(Success(response))
    case Success(response) => showResponse(response.toString)
    case Failure(CommandError.MultiError(values)) => values.foreach(showError)
    case Failure(e: CommandError.TracedError) => showTrace(e.trace); showError(e.error)
    case Failure(e: CommandError) => showError(e)
    case Failure(error) => throw new IllegalStateException(s"Unexpected error: $error")
  }

  def showTrace(trace: Seq[String]): Unit = {
    trace.foreach(errorWriter.println)
    errorWriter.flush()
  }

  def showError(error: CommandError): Unit = {
    errorWriter.print(Shell.formatError(error))
    errorWriter.flush()
  }

  def showResponse(response: String): Unit = {
    writer.print(s"$response\n")
    writer.flush()
  }

  def runCommand(command: Command): Try[CommandResponse] = command.run(this)

  def runCommandAndDisplay(command: Command): Try[CommandResponse] = {
    val result = runCommand(command)
    showResult(result)
    result
  }

  //override def readFile(fileName: String): Try[Array[Byte]] = context.readFile(fileName)

  //def writeFile(fileName: String, data: Array[Byte]): Try[Unit] = context.writeFile(fileName, data)

}



