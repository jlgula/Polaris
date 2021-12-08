package org.opendcgrid.app.polaris

package object device {
  type DeviceID = String
  type PowerValue = BigDecimal

  object PowerValue {
    def apply(value: BigDecimal): PowerValue = value
  }

  object DeviceProperties {
    def apply(id: DeviceID, name: String, powerRequested: Option[PowerValue] = None, powerOffered: Option[PowerValue] = None): org.opendcgrid.app.polaris.client.definitions.Device = {
      org.opendcgrid.app.polaris.client.definitions.Device(id, name, powerRequested, powerOffered)
    }
  }
}
