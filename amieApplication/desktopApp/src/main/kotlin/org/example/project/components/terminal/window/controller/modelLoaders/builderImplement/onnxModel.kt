package org.example.project.components.terminal.window.controller.modelLoaders.builderImplement

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.TensorInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.example.project.components.terminal.integrations.amiePilot.InteractionModel
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import org.example.project.components.terminal.window.controller.modelLoaders.ManagedSession
import org.example.project.components.terminal.window.controller.modelLoaders.OrtSessions
import org.example.project.components.terminal.window.controller.modelLoaders.modelProps
import java.io.File
import java.nio.LongBuffer
import java.nio.file.Path
import kotlin.math.exp

/**
 * A fixed, untrainable ONNX model with the same interface as initMod: inference(text), getStatus(), close().
 * Transformer models (an input_ids input) tokenize with the tokenizer.json next to the model;
 * other models read the text as comma-separated numbers for a [batch, n] float input.
 * Labels come from a Hugging Face config.json (id2label) next to the model when there is one.
 */
class onnxModel(private val path: Path) : modelProps, InteractionModel {
    private val dir: File = path.toFile().absoluteFile.parentFile
    private var managed: ManagedSession? = null
    private var tokenizer: HuggingFaceTokenizer? = null
    private val labels: Map<Int, String> by lazy { readLabels() }

    val name: String get() = path.toFile().nameWithoutExtension

    @Synchronized
    override fun load() {
        if (managed != null) return
        require(path.toFile().isFile) { "ONNX model not found: $path" }
        val session = OrtSessions.open("onnx:$name", path.toFile().absolutePath)
        managed = session
        if ("input_ids" in session.inputNames) {
            val tokenizerFile = File(dir, "tokenizer.json")
            require(tokenizerFile.isFile) { "$name takes input_ids but there is no tokenizer.json in $dir" }
            tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile.toPath())
        }
        logInternal("importModelOnnx: '$name' read, ${session.inputNames} -> ${session.outputs.keys}, labels ${labels.size}")
    }

    private fun session(): ManagedSession {
        managed?.let { if (it.isCancelled) throw java.util.concurrent.CancellationException("'$name' session #${it.id} was cancelled; import the model again") else return it }
        load()
        return managed!!
    }

    override fun inference(text: String): List<String> {
        val session = session()
        val res = if (tokenizer != null) textInference(session, text) else vectorInference(session, text)
        logInternal("importModelOnnx: '$name' -> ${res.take(10).joinToString()}${if (res.size > 10) " …" else ""}")
        return res
    }

    private fun textInference(session: ManagedSession, text: String): List<String> {
        val encoded = tokenizer!!.encode(text)
        val ids = encoded.ids
        val feeds = session.inputNames.associateWith { input ->
            val values = when (input) {
                "input_ids" -> ids
                "attention_mask" -> encoded.attentionMask
                "token_type_ids" -> encoded.typeIds ?: LongArray(ids.size)
                else -> throw IllegalArgumentException("$name has an input '$input' that text can't fill")
            }
            OnnxTensor.createTensor(OrtSessions.env, LongBuffer.wrap(values), longArrayOf(1, values.size.toLong()))
        }
        try {
            session.run(feeds).use { result ->
                val out = result.get(0).value
                return when {
                    // [1, tokens, classes]: one label per token
                    out is Array<*> && out.firstOrNull() is Array<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val perToken = (out as Array<Array<FloatArray>>)[0]
                        if (labels.isEmpty()) listOf("hidden state [1, ${perToken.size}, ${perToken.firstOrNull()?.size ?: 0}]")
                        else perToken.mapIndexed { i, logits -> "${encoded.tokens[i]}:${labels[argmax(logits)] ?: argmax(logits)}" }
                    }
                    // [1, classes]: one label for the text
                    out is Array<*> -> classScores(out[0] as FloatArray)
                    else -> listOf(out.toString())
                }
            }
        } finally {
            feeds.values.forEach { it.close() }
        }
    }

    private fun vectorInference(session: ManagedSession, text: String): List<String> {
        val (input, info) = session.inputs.entries.singleOrNull()
            ?: throw IllegalArgumentException("$name has inputs ${session.inputNames}; vector inference needs exactly one")
        val values = text.split(',', ' ', '\n').filter { it.isNotBlank() }.map { it.trim().toFloat() }.toFloatArray()
        val expected = (info.info as? TensorInfo)?.shape?.lastOrNull() ?: -1
        require(expected <= 0 || expected.toInt() == values.size) { "$name input '$input' takes $expected values, got ${values.size}" }
        OnnxTensor.createTensor(OrtSessions.env, arrayOf(values)).use { tensor ->
            session.run(mapOf(input to tensor)).use { result ->
                @Suppress("UNCHECKED_CAST")
                return classScores((result.get(0).value as Array<FloatArray>)[0])
            }
        }
    }

    // "label:probability", most likely first
    private fun classScores(scores: FloatArray): List<String> {
        // Logits are turned into probabilities; outputs that already sum to 1 are kept
        val probabilities = if (scores.all { it in 0f..1f } && scores.sum() in 0.99f..1.01f) scores else softmax(scores)
        return probabilities.mapIndexed { i, p -> (labels[i] ?: i.toString()) to p }
            .sortedByDescending { it.second }
            .map { (label, p) -> "$label:$p" }
    }

    override fun getStatus(): Any {
        val session = session()
        return mapOf(
            "name" to name,
            "path" to path.toString(),
            "trainable" to false,
            "session" to "#${session.id}",
            "inputs" to session.inputs.mapValues { it.value.info.toString() },
            "outputs" to session.outputs.mapValues { it.value.info.toString() },
            "labels" to labels.toSortedMap().values.toList(),
            "producer" to runCatching { session.metadata.producerName }.getOrNull(),
        )
    }

    @Synchronized
    override fun close() {
        managed?.cancel()
        managed = null
        tokenizer?.close()
        tokenizer = null
    }

    private fun readLabels(): Map<Int, String> = runCatching {
        val config = Json.parseToJsonElement(File(dir, "config.json").readText()).jsonObject
        config["id2label"]?.jsonObject?.entries?.associate { (k, v) -> k.toInt() to v.jsonPrimitive.content } ?: emptyMap()
    }.getOrElse { emptyMap() }
}

private fun argmax(values: FloatArray): Int = values.indices.maxByOrNull { values[it] } ?: 0

private fun softmax(values: FloatArray): FloatArray {
    val max = values.maxOrNull() ?: 0f
    val e = values.map { exp((it - max).toDouble()) }
    val sum = e.sum()
    return FloatArray(values.size) { (e[it] / sum).toFloat() }
}

// Kept for importModel's dispatch; an unreadable path surfaces when the session is opened
fun openOnnxModel(path: Path): onnxModel = onnxModel(path).also {
    try {
        it.load()
    } catch (e: Exception) {
        logInternal("importModelOnnx: ${e.message}", LogLevel.ERROR)
        throw e
    }
}
