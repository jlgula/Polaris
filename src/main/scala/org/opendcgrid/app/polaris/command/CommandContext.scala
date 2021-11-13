package org.opendcgrid.app.polaris.command

import org.opendcgrid.lib.task.TaskManager

trait CommandContext {
  def allCommands: Seq[Parsable]
  def taskManager: TaskManager
}
