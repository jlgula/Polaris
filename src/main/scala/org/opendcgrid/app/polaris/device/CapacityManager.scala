package org.opendcgrid.app.polaris.device

import org.opendcgrid.app.polaris.client.definitions.{Device => DefinedDevice}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

case class PowerAssignment(id: DeviceID, powerGranted: PowerValue = PowerValue(0), powerAccepted: PowerValue = PowerValue(0))

class CapacityManager(
                           sendGrant: (DeviceID, PowerValue) => Future[Unit],
                           sendAccept: (DeviceID, PowerValue) => Future[Unit])(implicit ec: ExecutionContext) {
  private val deviceMap = mutable.Map[DeviceID, DefinedDevice]()
  private val currentAssignments = mutable.Map[DeviceID, PowerAssignment]()
  case class Assignment(id: DeviceID, power: PowerValue)

  def addDevice(device: DefinedDevice): Future[Seq[PowerAssignment]] = {
    deviceMap.put(device.id, device)
    currentAssignments.put(device.id, PowerAssignment(device.id))
    reassignPower()
  }

  def removeDevice(id: DeviceID): Future[Seq[PowerAssignment]] = {
    deviceMap.remove(id)
    currentAssignments.remove(id)
    reassignPower()
  }

  def updateDevice(device: DefinedDevice): Future[Seq[PowerAssignment]] = {
    deviceMap.put(device.id, device)
    reassignPower()
  }

  def getPowerAssignment(id: String): PowerAssignment = currentAssignments(id)

  private def reassignPower(): Future[Seq[PowerAssignment]] = {

    val powerValues =  deviceMap.values.map(device => (device.powerRequested.getOrElse(PowerValue(0)), device.powerOffered.getOrElse(PowerValue(0))))

    val powerRequested = powerValues.map(_._1).sum  // Compute total of power requested.
    val powerOffered = powerValues.map(_._2).sum  // Compute total of power available.

    val powerDistributed = powerRequested.min(powerOffered)
    val ids = deviceMap.keys.toSeq.sorted // TODO: sort by priority
    // Distribute the power offered to devices in order and convert the result to a map.
    val grantAssignments = distributeGrantedPower(ids, powerDistributed).map(assignment => (assignment.id, assignment.power)).toMap
    val totalGrantsAssigned = grantAssignments.values.sum
    val devicesNeedingGrants = ids.filter(id => grantAssignments(id) != currentAssignments(id).powerGranted)

    val acceptAssignments = distributeAcceptedPower(ids, totalGrantsAssigned).map(assignment => (assignment.id, assignment.power)).toMap
    val devicesNeedingAccepts = ids.filter(id => acceptAssignments(id) != currentAssignments(id).powerAccepted)

    for {
      accepts <- sendAccepts(devicesNeedingAccepts, acceptAssignments)
      grants <- sendGrants(devicesNeedingGrants, grantAssignments)
    } yield accepts ++ grants
  }

  private def sendGrants(devicesNeedingGrants: Seq[DeviceID], grantAssignments: Map[DeviceID, PowerValue]): Future[Seq[PowerAssignment]] = {
    val grants = devicesNeedingGrants.map { id =>
      val powerGranted = grantAssignments(id)
      val assignment = PowerAssignment(id, powerGranted, currentAssignments(id).powerAccepted)
      currentAssignments.put(id, assignment)
      sendGrant(id, powerGranted).map(_ => assignment)
    }
    Future.sequence(grants)   // Send grants in any order.
  }

  private def sendAccepts(devicesNeedingAccepts: Seq[DeviceID], acceptAssignments: Map[DeviceID, PowerValue]): Future[Seq[PowerAssignment]] = {
    val accepts = devicesNeedingAccepts.map{id: DeviceID =>
      val powerAccepted = acceptAssignments(id)
      val assignment = PowerAssignment(id, currentAssignments(id).powerGranted, powerAccepted)
      currentAssignments.put(id, assignment)
      sendAccept(id, powerAccepted).map(_ => assignment)
    }
    Future.sequence(accepts)   // Send grants in any order.
  }

  // The grant distribution algorithm assigns available power in order of IDs.
  // In order for a grant to be made, the requested power must be less than or equal to the unassigned available power.
  // If the requested power is greater than the unassigned power, the device is assigned 0 power.
  // This means that the remaining power may not be 0 after all distributions have been made.
  @tailrec
  private def distributeGrantedPower(ids: Seq[DeviceID], remaining: PowerValue, accumulator: Seq[Assignment] = Nil): Seq[Assignment] = ids match {
    case Nil =>
      assert(remaining.signum >= 0)
      accumulator
    case _ =>
      val nextID = ids.head
      val powerRequested = this.deviceMap(nextID).powerRequested.getOrElse(PowerValue(0))
      if (remaining >= powerRequested) {
        //val assignedValue = source(nextID).min(remaining)
        distributeGrantedPower(ids.tail, remaining - powerRequested, accumulator :+ Assignment(nextID, powerRequested))
      }
      else distributeGrantedPower(ids.tail, remaining, accumulator :+ Assignment(nextID, PowerValue(0)))
  }

  // The accept distribution algorithm accepts power in order of IDs.
  // It will accept until all the remaining power is assigned.
  @tailrec
  private def distributeAcceptedPower(ids: Seq[DeviceID], remaining: PowerValue, accumulator: Seq[Assignment] = Nil): Seq[Assignment] = ids match {
    case Nil =>
      assert(remaining.equals(PowerValue(0)))
      accumulator
    case _ =>
      val nextID = ids.head
      val powerOffered = this.deviceMap(nextID).powerOffered.getOrElse(PowerValue(0))
      if (remaining >= PowerValue(0)) {
        val acceptedPower = remaining.min(powerOffered)
        distributeAcceptedPower(ids.tail, remaining - acceptedPower, accumulator :+ Assignment(nextID, acceptedPower))
      }
      else distributeAcceptedPower(ids.tail, remaining, accumulator :+ Assignment(nextID, PowerValue(0)))
  }
}
