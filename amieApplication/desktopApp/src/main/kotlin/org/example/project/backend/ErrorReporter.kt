package org.example.project.backend

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.example.project.auth.AuthState
import org.slf4j.LoggerFactory

/**
 * Sends the app's ERROR-level console entries to the backend's error log of the signed-in user, which the
 * website's dashboard shows under "Errors from your app". [report] never blocks: entries go through a channel
 * to one background coroutine on the IO dispatcher. The same `kind|message` is sent at most once a minute,
 * messages are cut to 2000 characters and guests are not reported. A report that fails reaches only the debug
 * log, never the console, so an unreachable backend cannot feed itself new errors.
 */
object ErrorReporter {
    private const val MAX_MESSAGE = 2000
    private const val DEDUPE_MS = 60_000L
    private const val MAX_RECENT = 256

    private val logger = LoggerFactory.getLogger("ErrorReporter")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Channel<Pair<String, String>>(capacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    // When each `kind|message` was last sent; bounded, the least recently seen key goes first
    private val recent = object : LinkedHashMap<String, Long>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean = size > MAX_RECENT
    }

    init {
        scope.launch {
            for ((kind, message) in queue) {
                if (AuthState.user.isEmpty()) continue // signed out in the meantime
                try {
                    AmieBackend.reportError(kind, message)
                } catch (e: Throwable) {
                    logger.debug("couldn't report '{}': {}", kind, e.message)
                }
            }
        }
    }

    /** Queues one error for the backend; returns at once. Nothing happens for a guest or a repeat within a minute */
    fun report(kind: String, message: String) {
        if (AuthState.user.isEmpty()) return
        val text = message.take(MAX_MESSAGE)
        val key = "$kind|$text"
        val now = System.currentTimeMillis()
        synchronized(recent) {
            val last = recent[key]
            if (last != null && now - last < DEDUPE_MS) return
            recent[key] = now
        }
        queue.trySend(kind to text)
    }
}
