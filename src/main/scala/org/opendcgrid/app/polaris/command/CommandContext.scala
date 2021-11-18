package org.opendcgrid.app.polaris.command

import org.opendcgrid.lib.task.TaskManager

import scala.concurrent.ExecutionContext

trait CommandContext {
  def allCommands: Seq[Parsable]
  def taskManager: TaskManager
  def executionContext: ExecutionContext
}
