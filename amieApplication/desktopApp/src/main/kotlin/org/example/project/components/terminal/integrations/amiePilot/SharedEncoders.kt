package org.example.project.components.terminal.integrations.amiePilot

import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import org.example.project.components.terminal.window.controller.modules.bytes
import java.io.File

/** One loaded encoder: its folder, how many holders share it and what its ONNX file weighs on disk */
data class LoadedEncoder(val path: String, val holders: Int, val sizeBytes: Long)

/**
 * The encoders in memory, one per folder however many sessions use it. An encoder is the heavy part of a model
 * (hundreds of MB loaded by ORT outside the heap, the head is a few KB), so every `initMod`, Listen, validation
 * and build that used to load its own copy acquires the shared one here and releases it when it closes; the
 * last release closes it. Callers synchronize on the encoder while they run a text through it, as it keeps the
 * last hidden state between `sayHello` and `getContextTensor`.
 */
object SharedEncoders {
    private class Entry(val encoder: BertOnnxModel) { var holders = 0 }
    private val entries = HashMap<String, Entry>()

    // The folder a caller names may be another machine's; the bundled encoder of that name stands in for it
    private fun key(path: String) = runCatching { File(BundledModels.resolveEncoder(path)).canonicalPath }.getOrDefault(path)

    /** The encoder of that folder, loaded on the first call and shared after it */
    @Synchronized
    fun acquire(path: String): BertOnnxModel {
        val k = key(path)
        val entry = entries[k] ?: Entry(BertOnnxModel(k)).also {
            entries[k] = it
            logInternal("encoder: loaded ${File(path).name} (${bytes(File(path, "model.onnx").length())} on disk)")
        }
        entry.holders++
        if (entry.holders > 1) logInternal("encoder: ${File(path).name} shared by ${entry.holders} holders, not loaded again")
        return entry.encoder
    }

    /** Gives the encoder back; the last holder's release closes it and frees its memory */
    @Synchronized
    fun release(path: String) {
        val k = key(path)
        val entry = entries[k] ?: return
        entry.holders--
        if (entry.holders <= 0) {
            entries.remove(k)
            runCatching { entry.encoder.close() }.onFailure { logInternal("encoder: close of ${File(path).name} failed: ${it.message}", LogLevel.ERROR) }
            logInternal("encoder: ${File(path).name} released and closed")
        }
    }

    /** Runs [block] with the encoder of [path], acquired for just that long */
    inline fun <T> use(path: String, block: (BertOnnxModel) -> T): T {
        val encoder = acquire(path)
        try {
            return block(encoder)
        } finally {
            release(path)
        }
    }

    /** What is loaded right now, for the memory footer */
    val loaded: List<LoadedEncoder>
        @Synchronized get() = entries.map { (k, e) -> LoadedEncoder(k, e.holders, File(k, "model.onnx").length()) }
}
