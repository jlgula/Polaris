package org.opendcgrid.app.polaris

import org.opendcgrid.lib.commandoption.{CommandOption, CommandOptionResult, OneArgumentTag, ZeroArgumentTag}
/**
 * ODG application specific command line options.
 */
object PolarisAppOptionTag {
  case object Client extends OneArgumentTag(PolarisAppOption.Client.apply, "client")
  case object DevicesOption extends ZeroArgumentTag(PolarisAppOption.Devices, "devices")
  case object HaltOption extends ZeroArgumentTag(PolarisAppOption.Halt, "halt")
  case object Log extends OneArgumentTag(PolarisAppOption.Log.apply, "log")
  case object Port extends OneArgumentTag(PolarisAppOption.Port.apply, "port")
  case object Server extends ZeroArgumentTag(PolarisAppOption.Server, "server")
  case object Settings extends ZeroArgumentTag(PolarisAppOption.Settings, "settings")
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

  /**
   * [[CommandOption]] that tells polaris to halt any running devices - used in testing.
   */
  case object Halt extends CommandOption

  case class Log(level: String) extends CommandOption

  case object Port {
    def unapply(result: CommandOptionResult): Option[Port] = result.options.collectFirst { case o: Port => o }
  }

  /**
   * [[CommandOption]] for the port used by the server command.
   * @param value the localhost port number that the server will use
   */
  case class Port(value: String) extends CommandOption

  /**
   * [[CommandOption]] that indicates that a server should be started.
   */
  case object Server extends CommandOption

  /**
   * [[CommandOption]] that indicates that configuration settings should be displayed.
   */
  case object Settings extends CommandOption

  /**
   * [[CommandOption]] that indicates that a shell should be started after loading files.
   */
  case object Shell extends CommandOption
}
