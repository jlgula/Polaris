package org.opendcgrid.app.polaris.shell

/**
 * Configuration options for the [[Shell]].
 *
 * @param enablePrompt true iff the shell should display prompts
 */
case class ShellConfiguration(enablePrompt: Boolean = false)
