package org.opendcgrid.app.polaris

package object device {
  type DeviceID = String
  type PowerValue = BigDecimal

  object PowerValue {
    def apply(value: BigDecimal): PowerValue = value
  }
}
