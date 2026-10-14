package org.example.project.components.terminal.window.controller.modelLoaders

import ai.onnxruntime.NodeInfo
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import java.time.Instant
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities

/**
 * Every ORT session in the app: created and run on background threads, listed by print(), cancelled by cancel()/cancelAll().
 * Cancelling terminates in-flight runs before the native session is closed, so a closing session never races a run.
 */
object OrtSessions {
    val env: OrtEnvironment = OrtEnvironment.getEnvironment()

    private val ids = AtomicInteger()
    private val threads = AtomicInteger()
    private val executor = Executors.newCachedThreadPool { Thread(it, "amie-ort-${threads.incrementAndGet()}").apply { isDaemon = true } }
    private val sessions = ConcurrentHashMap<Int, ManagedSession>()

    val all: List<ManagedSession> get() = sessions.values.sortedBy { it.id }

    /** Opens a session for [path] off the UI thread; [name] is how it shows up and is cancelled */
    fun open(name: String, path: String): ManagedSession {
        val session = background("open $name") { env.createSession(path, OrtSession.SessionOptions()) }
        return ManagedSession(ids.incrementAndGet(), name, path, session).also {
            sessions[it.id] = it
            logInternal("ort: session #${it.id} '$name' opened from $path")
        }
    }

    fun find(name: String): List<ManagedSession> = all.filter { it.name == name || it.id.toString() == name }

    /** Cancels and closes sessions by name (or #id); returns how many */
    fun cancel(name: String): Int = find(name).onEach { it.cancel() }.size.also {
        if (it == 0) logInternal("ort: no session '$name'", LogLevel.ERROR)
    }

    fun cancelAll(): Int = all.onEach { it.cancel() }.size.also { logInternal("ort: cancelled $it sessions") }

    // Internal reader: what each session expects and returns, and whether it is busy
    fun print(): List<String> {
        val lines = buildList {
            add("ort sessions (${sessions.size}):")
            all.forEach { s ->
                add("  #${s.id} ${s.name}  ${if (s.running > 0) "running ${s.running}" else "idle"}  runs ${s.runs}  since ${s.openedAt}")
                add("    path    ${s.path}")
                s.inputs.forEach { (n, info) -> add("    input   $n ${describe(info)}") }
                s.outputs.forEach { (n, info) -> add("    output  $n ${describe(info)}") }
            }
        }
        logInternal(lines.joinToString("\n"))
        return all.map { "#${it.id} ${it.name}" }
    }

    internal fun remove(session: ManagedSession) = sessions.remove(session.id)

    // Runs ORT work on its own threads; the caller (a script or IO coroutine) waits, the UI thread never does
    internal fun <T> background(what: String, block: () -> T): T {
        check(!SwingUtilities.isEventDispatchThread()) { "ort: '$what' called on the UI thread; run it from a background coroutine" }
        val future = executor.submit<T> { block() }
        try {
            return future.get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        } catch (e: InterruptedException) {
            future.cancel(true)
            throw CancellationException("ort: '$what' interrupted")
        }
    }

    private fun describe(info: NodeInfo): String = info.info.toString()
        .replace(Regex("""javaType=\w+,onnxType=ONNX_TENSOR_ELEMENT_DATA_TYPE_"""), "")
}

class ManagedSession internal constructor(val id: Int, val name: String, val path: String, private val session: OrtSession) {
    val openedAt: Instant = Instant.now()
    private val active = ConcurrentHashMap.newKeySet<OrtSession.RunOptions>()
    private val runCount = AtomicInteger()
    private val runNanos = java.util.concurrent.atomic.AtomicLong()
    private val lastRunNanos = java.util.concurrent.atomic.AtomicLong()
    @Volatile private var closed = false

    val running: Int get() = active.size
    val isCancelled: Boolean get() = closed
    val runs: Int get() = runCount.get()
    /** Time spent inside the native session over every run, in milliseconds */
    val runTimeMs: Long get() = runNanos.get() / 1_000_000
    val lastRunMs: Long get() = lastRunNanos.get() / 1_000_000
    val averageRunMs: Double get() = if (runs == 0) 0.0 else runTimeMs.toDouble() / runs
    val inputs: Map<String, NodeInfo> get() = session.inputInfo
    val outputs: Map<String, NodeInfo> get() = session.outputInfo
    val inputNames: Set<String> get() = session.inputNames
    val metadata get() = session.metadata

    /** Runs the model in the background; cancel() terminates it and makes this throw CancellationException */
    fun run(inputs: Map<String, OnnxTensor>): OrtSession.Result {
        if (closed) throw CancellationException("ort: session #$id '$name' was cancelled")
        return OrtSessions.background("run $name") {
            val options = OrtSession.RunOptions()
            active += options
            val begun = System.nanoTime()
            try {
                runCount.incrementAndGet()
                session.run(inputs, options).also {
                    val took = System.nanoTime() - begun
                    runNanos.addAndGet(took)
                    lastRunNanos.set(took)
                }
            } catch (e: Exception) {
                if (closed) throw CancellationException("ort: run on #$id '$name' cancelled") else throw e
            } finally {
                active -= options
                options.close()
            }
        }
    }

    @Synchronized
    fun cancel() {
        if (closed) return
        closed = true
        active.forEach { runCatching { it.setTerminate(true) } }
        // Native close must wait for runs to unwind
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (active.isNotEmpty() && System.nanoTime() < deadline) Thread.sleep(10)
        session.close()
        OrtSessions.remove(this)
        logInternal("ort: session #$id '$name' cancelled")
    }

    fun close() = cancel()
}
