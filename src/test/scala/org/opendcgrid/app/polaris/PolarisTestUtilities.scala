package org.opendcgrid.app.polaris

import java.net.ServerSocket

object PolarisTestUtilities {
  def getUnusedPort: Int = {
    val socket = new ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    port
  }
}
