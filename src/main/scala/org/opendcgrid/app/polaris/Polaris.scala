package org.opendcgrid.app.polaris

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import org.opendcgrid.app.polaris.server.PolarisServer

import java.io.{BufferedReader, PrintStream}

object Polaris extends App {
  val app = new Polaris()
  val result = app.run(this.args.toIndexedSeq, Console.in, Console.out, Console.err)
  System.exit(result)
}

class Polaris {

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
    val url = Uri("http://localhost:8080")
    implicit def actorSystem: ActorSystem = ActorSystem()
    val server = new PolarisServer(url)
    server.run()
    0
  }


}
