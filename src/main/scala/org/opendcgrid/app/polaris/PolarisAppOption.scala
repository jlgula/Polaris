package org.opendcgrid.app.polaris

import org.opendcgrid.lib.commandoption.{CommandOption, CommandOptionResult, OneArgumentTag, ZeroArgumentTag}
/**
 * ODG application specific command line options.
 */
object PolarisAppOptionTag {
  case object Client extends OneArgumentTag(PolarisAppOption.Client.apply, "client")
  case object DevicesOption extends ZeroArgumentTag(PolarisAppOption.Devices, "devices")
  case object Log extends OneArgumentTag(PolarisAppOption.Log.apply, "log")
  case object Server extends ZeroArgumentTag(PolarisAppOption.Server, "server")
  case object Shell extends ZeroArgumentTag(PolarisAppOption.Shell, "shell")
}

object PolarisAppOption {
  case object Client {
    def unapply(result: CommandOptionResult): Option[Client] = result.options.collectFirst { case o: Client => o }
  }

  /**
   * [[CommandOption]] that starts a client of the designated type.
   * @param clientType  the type of client to start
   */
  case class Client(clientType: String) extends CommandOption

  /**
   * [[CommandOption]] that indicates a list of devices should be displayed on standard output.
   */
  case object Devices extends CommandOption

  case class Log(level: String) extends CommandOption


  /**
   * [[CommandOption]] that indicates that a server should be started.
   */
  case object Server extends CommandOption

  /**
   * [[CommandOption]] that indicates that a shell should be started after loading files.
   */
  case object Shell extends CommandOption
}
