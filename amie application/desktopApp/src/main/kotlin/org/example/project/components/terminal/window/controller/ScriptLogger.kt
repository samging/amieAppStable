package org.example.project.components.terminal.window.controller

/**
 * A stable interface for logging from scripts.
 * Defined in a separate file to ensure it's a top-level class with stable visibility.
 */
interface ScriptLogger {
    fun log(message: String)
    fun initAmie()
}
