package org.example.project.components.terminal.window.controller.modules

import org.example.project.components.terminal.integrations.amiePilot.InitMod
import org.example.project.components.terminal.integrations.amiePilot.InteractionModel
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * One open intent model: the encoder and the exported head behind the `inference / getStatus / close` interface.
 * Every `initMod("name")` makes one, and the Models page lists them so any of them can be terminated there.
 */
class ModelSession internal constructor(
    val id: Int,
    val name: String,
    private val model: InteractionModel,
    /** "intent model", or "collection" when the name is a collection of models opened together */
    val kind: String = "intent model",
    logFile: File = File(moduleDir(name), "sessions.log"),
) : InteractionModel {
    val openedAt: Instant = Instant.now()
    private val runningCount = AtomicInteger()
    private val runCount = AtomicInteger()

    /** What this session was asked and answered; also appended to AMP/modules/<name>/sessions.log */
    val log = SessionLog(name, id, logFile)

    /** Calls of inference in flight right now */
    val running: Int get() = runningCount.get()
    val runs: Int get() = runCount.get()

    @Volatile
    var closed: Boolean = false
        private set

    override fun inference(text: String): List<String> {
        check(!closed) { "model $name (#$id) is closed" }
        runningCount.incrementAndGet()
        val begun = System.nanoTime()
        try {
            return model.inference(text).also { log.run(text, it, (System.nanoTime() - begun) / 1_000_000) }
        } catch (e: Throwable) {
            log.error(text, e)
            throw e
        } finally {
            runningCount.decrementAndGet()
            runCount.incrementAndGet()
        }
    }

    override fun getStatus(): Any = model.getStatus()

    /** Closes the head session and the encoder; a second call does nothing */
    override fun close() {
        if (closed) return
        closed = true
        runCatching { model.close() }.onFailure { logInternal("model $name (#$id): close failed: ${it.message}", LogLevel.ERROR) }
        ModelSessions.unregister(this)
        log.add("close", "closed after $runs runs")
        logInternal("model $name (#$id): closed after $runs runs")
    }

    override fun toString(): String = "#$id $name · ${if (running > 0) "running $running" else "idle"} · runs $runs"
}

/** The open intent model sessions, so the Models page can show and terminate them */
object ModelSessions {
    private val ids = AtomicInteger()
    private val sessions = linkedMapOf<Int, ModelSession>()

    val all: List<ModelSession> @Synchronized get() = sessions.values.toList()

    /**
     * The session of the module exported to AMP/modules/<name>. A model has one session at most: the one already
     * open is returned (so a script run again, two scripts, or the page's button don't pile up sessions), else a
     * new one is opened.
     */
    fun open(name: String): ModelSession = acquire(name).first

    /** The session of the model and whether this call opened it, for a caller that closes only what it opened */
    @Synchronized
    fun acquire(name: String): Pair<ModelSession, Boolean> {
        val existing = sessions.values.firstOrNull { it.name == name && !it.closed }
        if (existing != null) {
            existing.log.add("open", "reused: a model has one session")
            logInternal("initMod: '$name' is already open as session #${existing.id}, that one is used")
            return existing to false
        }
        val session = ModelSession(ids.incrementAndGet(), name, InitMod(name).create())
        session.log.add("open", "opened as session #${session.id}")
        sessions[session.id] = session
        return session to true
    }

    fun close(id: Int): Boolean {
        val session = synchronized(this) { sessions[id] } ?: return false
        session.close()
        return true
    }

    @Synchronized
    fun closeAll() = sessions.values.toList().forEach { it.close() }

    internal fun unregister(session: ModelSession) {
        synchronized(this) { sessions.remove(session.id) }
    }
}
