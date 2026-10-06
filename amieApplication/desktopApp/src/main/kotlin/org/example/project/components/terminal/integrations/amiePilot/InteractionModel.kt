package org.example.project.components.terminal.integrations.amiePilot

interface InteractionModel : AutoCloseable {
    override fun close()
    fun inference(text: String): List<String>
    fun getStatus(): Any
}