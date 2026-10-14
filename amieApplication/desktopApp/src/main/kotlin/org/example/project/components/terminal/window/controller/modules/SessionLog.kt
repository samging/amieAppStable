package org.example.project.components.terminal.window.controller.modules

import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One line of a session's log: when, what kind of event (open, run, error, close) and the text */
data class SessionLogEntry(val at: Instant, val kind: String, val text: String) {
    val time: String get() = TIME.format(at)

    private companion object {
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    }
}

/**
 * The log of one session: what it was asked and what it answered, kept in memory for the Models page and
 * appended to a file next to its owner (`AMP/modules/<name>/sessions.log` for an intent model,
 * `AMP/pluginDir/<jar>.sessions.log` for a package) so the history of a terminated session is not lost.
 */
class SessionLog(private val owner: String, private val id: Int, private val file: File?, private val capacity: Int = 300) {
    private val entries = ArrayDeque<SessionLogEntry>()

    /** The entries still in memory, oldest first */
    val lines: List<SessionLogEntry> @Synchronized get() = entries.toList()

    val size: Int @Synchronized get() = entries.size

    @Synchronized
    fun add(kind: String, text: String) {
        val entry = SessionLogEntry(Instant.now(), kind, text)
        entries.addLast(entry)
        while (entries.size > capacity) entries.removeFirst()
        file?.let { f ->
            runCatching {
                f.parentFile?.mkdirs()
                f.appendText("${entry.at} $owner #$id ${kind.padEnd(5)} $text\n")
            }.onFailure { logInternal("session log $owner #$id: couldn't write ${f.name}: ${it.message}", LogLevel.ERROR) }
        }
    }

    /** A run: the text in, the first answers out and how long it took */
    fun run(input: String, output: List<String>, millis: Long) =
        add("run", "${quote(input)} -> ${output.take(3).joinToString(", ").ifEmpty { "(nothing)" }}${if (output.size > 3) " +${output.size - 3}" else ""} (${millis} ms)")

    fun error(input: String, error: Throwable) = add("error", "${quote(input)} -> ${error::class.simpleName}: ${error.message}")

    private fun quote(text: String): String {
        val oneLine = text.replace('\n', ' ').trim()
        return "\"" + (if (oneLine.length > 80) oneLine.take(77) + "..." else oneLine) + "\""
    }
}
