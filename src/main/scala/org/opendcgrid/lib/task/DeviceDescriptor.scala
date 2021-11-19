package org.opendcgrid.lib.task

sealed abstract class DeviceDescriptor(val name: String)
object DeviceDescriptor {
  object GC extends DeviceDescriptor("gc")
  object Client extends DeviceDescriptor("client")

  val all: Seq[DeviceDescriptor] = Seq(GC, Client)
  def find(name: String): Option[DeviceDescriptor] = all.find(_.name == name)
}
