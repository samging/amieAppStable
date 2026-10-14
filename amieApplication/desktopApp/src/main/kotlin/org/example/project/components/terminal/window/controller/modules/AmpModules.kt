package org.example.project.components.terminal.window.controller.modules

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.DEFAULT_ENCODER
import org.example.project.components.terminal.window.controller.logInternal
import org.example.project.getDirectory
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant

// Exported models live in AMP/modules/<name>/ as model.onnx + module.json, indexed by AMP/modules/modules.json
val modulesDir: File get() = File(getDirectory().toString(), "modules")

private val moduleJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

@Serializable
data class ModuleManifest(
    val name: String,
    val format: String = "onnx",
    val model: String = "model.onnx",
    // Encoder that turns text into the hidden state the head was trained on
    val encoder: String,
    val pooling: String = "mean",
    val input: String = "embedding",
    val output: String = "score",
    val inputDim: Long,
    val outputDim: Long,
    // Class names in output order
    val labels: List<String> = emptyList(),
    val createdAt: String = Instant.now().toString(),
    // The data file the head was trained on, when known (a csv name); models trained on the same data group together
    val data: String? = null,
    // What the model is for, written by whoever built it and editable on the Models page
    val description: String? = null,
    // How it was trained: the head's shape and the fit; null in modules built before these were kept
    val architecture: String? = null,
    val epochs: Int? = null,
    val batchSize: Int? = null,
    val learningRate: Double? = null,
    val hiddenDim: Int? = null,
    val trainRows: Int? = null,
    val validationRows: Int? = null,
    val validation: String? = null,
    val finalLoss: Double? = null,
    val trainAccuracy: Double? = null,
)

@Serializable
data class ModuleRegistry(val modules: Map<String, String> = emptyMap())

private val registryFile: File get() = File(modulesDir, "modules.json")

fun moduleDir(name: String): File = File(modulesDir, name.replace(Regex("""[^A-Za-z0-9._-]"""), "_"))

fun readRegistry(): ModuleRegistry =
    runCatching { moduleJson.decodeFromString<ModuleRegistry>(registryFile.readText()) }.getOrElse { ModuleRegistry() }

/** The guide that ships inside a module: `guide.md` next to its manifest, what a downloaded model comes with */
fun guideFile(dir: File): File = File(dir, "guide.md")

/**
 * The guide a freshly built module starts with, from its manifest: what it is, its labels, how to open and ask
 * it from a script and over HTTP, how to validate it, and its files. Whoever builds the model edits it on the page.
 */
fun defaultGuide(m: ModuleManifest): String = buildString {
    appendLine("# ${m.name}")
    appendLine()
    appendLine(m.description ?: "An intent model: it turns a sentence into one of its labels.")
    appendLine()
    appendLine("## Labels")
    appendLine()
    m.labels.forEach { appendLine("- $it") }
    appendLine()
    appendLine("## From a script")
    appendLine()
    appendLine("```")
    appendLine("val model = initMod(\"${m.name}\")")
    appendLine("model.inference(\"your sentence\")   // [\"label:score\", ...], best first")
    appendLine("model.close()")
    appendLine("```")
    appendLine()
    appendLine("A model has one session: initMod gives the open one when there is one. Terminate it on the Models page or close() it.")
    appendLine()
    appendLine("## Over HTTP")
    appendLine()
    appendLine("Listen on the Models page, then POST {\"text\": \"...\"} or GET ?text=... to its /predict address; Configure shows the address and a curl call.")
    appendLine()
    appendLine("## Validating")
    appendLine()
    appendLine("Validate on a csv with a text and a label column (the labels above). Every validation is a run on the Metrics tab.")
    appendLine()
    appendLine("## How it works")
    appendLine()
    appendLine("The text goes through the ${File(m.encoder).name} encoder (${m.inputDim} numbers, ${m.pooling} pooled), then through this model's head" +
        (m.architecture?.let { " ($it)" } ?: "") + ", which gives one score per label.")
    m.epochs?.let { appendLine("Trained for $it epochs on ${m.trainRows ?: "?"} rows of ${m.data ?: "its data"}; validated on ${m.validationRows ?: "?"} rows of ${m.validation ?: "a split"}.") }
    appendLine()
    appendLine("## Files")
    appendLine()
    appendLine("- module.json: name, labels, encoder, how it was trained")
    appendLine("- ${m.model}: the head")
    appendLine("- metrics.json: every validation run")
    appendLine("- guide.md: this guide")
}

fun writeModule(manifest: ModuleManifest, onnxBytes: ByteArray): File {
    val dir = moduleDir(manifest.name).apply { mkdirs() }
    val modelFile = File(dir, manifest.model)
    modelFile.writeBytes(onnxBytes)
    File(dir, "module.json").writeText(moduleJson.encodeToString(manifest))
    guideFile(dir).takeIf { !it.isFile }?.writeText(defaultGuide(manifest))
    val registry = readRegistry()
    registryFile.writeText(moduleJson.encodeToString(registry.copy(modules = registry.modules + (manifest.name to dir.name))))
    logInternal("module '${manifest.name}' exported to ${modelFile.absolutePath}")
    return modelFile
}

/** Rewrites module.json with [change] applied, e.g. a new description; false when the module isn't there */
fun updateModule(name: String, change: (ModuleManifest) -> ModuleManifest): Boolean {
    val (dir, manifest) = readModule(name) ?: return false
    File(dir, "module.json").writeText(moduleJson.encodeToString(change(manifest)))
    return true
}

/**
 * Removes a module for good: its sessions are closed and its listener stopped first, then AMP/modules/<name>/
 * is deleted and the registry forgets it. The dummy comes back on the next start of the page.
 */
fun deleteModule(name: String): Boolean {
    val (dir, _) = readModule(name) ?: return false
    ModelServer.stop(name)
    ModelSessions.all.filter { it.name == name }.forEach { it.close() }
    val gone = dir.deleteRecursively()
    val registry = readRegistry()
    registryFile.writeText(moduleJson.encodeToString(registry.copy(modules = registry.modules - name)))
    logInternal(if (gone) "module '$name' destroyed: ${dir.absolutePath} removed" else "module '$name' forgotten, but ${dir.absolutePath} could not be removed entirely", if (gone) LogLevel.INTERNAL else LogLevel.ERROR)
    return gone
}

/** Whether a module of that name is exported, without logging when it isn't */
fun moduleExists(name: String): Boolean {
    val dir = readRegistry().modules[name]?.let { File(modulesDir, it) } ?: moduleDir(name)
    return File(dir, "module.json").isFile
}

// Resolves a module by its registered name, falling back to its folder name
fun readModule(name: String): Pair<File, ModuleManifest>? {
    val dir = readRegistry().modules[name]?.let { File(modulesDir, it) } ?: moduleDir(name)
    val manifestFile = File(dir, "module.json")
    if (!manifestFile.exists()) {
        logInternal("module '$name' not found in ${modulesDir.absolutePath}", LogLevel.ERROR)
        return null
    }
    return dir to moduleJson.decodeFromString<ModuleManifest>(manifestFile.readText())
}

const val DUMMY_MODEL = "dummy"
const val SAMPLE_DATA = "sample-intents.csv"

/**
 * Writes `AMP/sample-intents.csv` (text,label rows over greeting / lights / weather, the dummy's labels) unless it
 * exists, so a model can be built and validated from the page before any data of one's own is at hand.
 */
fun ensureSampleData(): File {
    val file = File(getDirectory().toString(), SAMPLE_DATA)
    if (file.isFile) return file
    val rows = mapOf(
        "greeting" to listOf(
            "hello there", "hi, how are you", "good morning", "hey amie", "good evening to you", "hi there, nice to see you",
            "hello, anyone home", "greetings", "hey, what's up", "morning", "hello again", "hi amie, you awake",
            "good afternoon", "howdy", "nice to meet you",
        ),
        "lights" to listOf(
            "turn the lights on", "switch off the kitchen light", "dim the living room lights", "lights off please",
            "make the bedroom brighter", "turn on the lamp", "set the lights to 50 percent", "kill the lights in the hall",
            "can you turn the light on in the office", "switch the garden lights on", "lights on", "turn off every light",
            "brighten the desk lamp", "lower the lights a bit", "turn the hallway light off",
        ),
        "weather" to listOf(
            "what's the weather like today", "will it rain tomorrow", "is it cold outside", "how hot is it right now",
            "do i need an umbrella", "what's the forecast for the weekend", "is it going to snow", "how windy is it",
            "temperature outside please", "weather for tomorrow morning", "is it sunny", "any storms coming",
            "what's the humidity today", "will it be warm this afternoon", "forecast for tonight",
        ),
    )
    val text = buildString {
        appendLine("text,label")
        rows.forEach { (label, texts) -> texts.forEach { appendLine("\"${it.replace("\"", "\"\"")}\",$label") } }
    }
    runCatching { file.writeText(text) }.onFailure { logInternal("sample data: could not be written: ${it.message}", LogLevel.ERROR) }
    return file
}

/**
 * Writes a small fixed intent model named "dummy" (three labels, deterministic weights, one dense layer over the
 * encoder's embedding) unless it already exists, so the Models page always has a component to configure and a
 * script can try `initMod("dummy")` before training anything. Returns the module folder, or null when it could
 * not be written.
 */
fun ensureDummyModule(encoder: String = DEFAULT_ENCODER): File? {
    val dir = moduleDir(DUMMY_MODEL)
    if (File(dir, "module.json").isFile) {
        // A dummy written before the description and the guide were kept gets them now
        runCatching { updateModule(DUMMY_MODEL) { m -> if (m.description != null) m else m.copy(description = dummyDescription(m.encoder), architecture = m.architecture ?: "dense[${m.inputDim} -> ${m.labels.size}] sigmoid over mean-pooled ${File(m.encoder).name}") } }
        runCatching { readModule(DUMMY_MODEL)?.let { (d, m) -> guideFile(d).takeIf { !it.isFile }?.writeText(defaultGuide(m)) } }
        return dir
    }
    return runCatching {
        val dim = hiddenSize(encoder) ?: 768
        val labels = listOf("greeting", "lights", "weather")
        val kernel = FloatArray(dim * labels.size) { i ->
            val row = i / labels.size
            val col = i % labels.size
            (kotlin.math.sin((row + 1) * 0.37 + col * 2.1) * 0.05).toFloat()
        }
        val layer = DenseExport(kernel, FloatArray(labels.size), dim.toLong(), labels.size.toLong(), "Sigmoid")
        val manifest = ModuleManifest(
            name = DUMMY_MODEL, encoder = encoder, inputDim = dim.toLong(), outputDim = labels.size.toLong(), labels = labels,
            description = dummyDescription(encoder),
            architecture = "dense[$dim -> ${labels.size}] sigmoid over mean-pooled ${File(encoder).name}",
        )
        writeModule(manifest, denseStackToOnnx(listOf(layer), manifest.input, manifest.output, DUMMY_MODEL))
        dir
    }.onFailure { logInternal("dummy model: could not be written: ${it.message}", LogLevel.ERROR) }.getOrNull()
}

private fun dummyDescription(encoder: String) =
    "A fixed three-label head with made-up weights over the ${File(encoder).name} encoder, so the page has a model to configure, " +
        "serve and validate and a script can try initMod(\"dummy\") before training anything. Its scores are not meaningful; " +
        "build a model from sample-intents.csv to see real ones."

// The encoder's hidden size from its config.json, which is the width of the embedding a head is trained on
private fun hiddenSize(encoder: String): Int? = runCatching {
    Regex(""""hidden_size"\s*:\s*(\d+)""").find(File(encoder, "config.json").readText())?.groupValues?.get(1)?.toInt()
}.getOrNull()

/** One dense layer: kernel is row-major [inDim][outDim]; activation is an ONNX op type or null for linear */
class DenseExport(val kernel: FloatArray, val bias: FloatArray?, val inDim: Long, val outDim: Long, val activation: String?)

/**
 * Writes a feed-forward stack as an ONNX model (opset 13): input [batch, inDim] -> MatMul/Add/activation per layer -> output.
 * The protobuf is encoded by hand so no ONNX library is needed.
 */
fun denseStackToOnnx(layers: List<DenseExport>, inputName: String, outputName: String, graphName: String): ByteArray {
    require(layers.isNotEmpty()) { "No layers to export" }
    val nodes = mutableListOf<ByteArray>()
    val initializers = mutableListOf<ByteArray>()
    var current = inputName
    layers.forEachIndexed { i, layer ->
        val isLast = i == layers.lastIndex
        val w = "dense_${i}_kernel"
        initializers += tensorProto(w, longArrayOf(layer.inDim, layer.outDim), layer.kernel)
        var out = "dense_${i}_matmul"
        nodes += nodeProto("MatMul", listOf(current, w), out, "dense_${i}_MatMul")
        if (layer.bias != null) {
            val b = "dense_${i}_bias"
            initializers += tensorProto(b, longArrayOf(layer.outDim), layer.bias)
            val added = "dense_${i}_add"
            nodes += nodeProto("Add", listOf(out, b), added, "dense_${i}_Add")
            out = added
        }
        if (layer.activation != null) {
            val act = "dense_${i}_${layer.activation.lowercase()}"
            nodes += nodeProto(layer.activation, listOf(out), act, "dense_${i}_${layer.activation}")
            out = act
        }
        if (isLast) {
            nodes += nodeProto("Identity", listOf(out), outputName, "output")
        }
        current = out
    }

    val graph = Proto().apply {
        nodes.forEach { bytes(1, it) }
        string(2, graphName)
        initializers.forEach { bytes(5, it) }
        bytes(11, valueInfo(inputName, layers.first().inDim))
        bytes(12, valueInfo(outputName, layers.last().outDim))
    }.toByteArray()

    return Proto().apply {
        varint(1, 8) // ir_version
        string(2, "amie")
        bytes(7, graph)
        bytes(8, Proto().apply { string(1, ""); varint(2, 13) }.toByteArray()) // opset_import
    }.toByteArray()
}

private fun nodeProto(op: String, inputs: List<String>, output: String, name: String): ByteArray = Proto().apply {
    inputs.forEach { string(1, it) }
    string(2, output)
    string(3, name)
    string(4, op)
}.toByteArray()

private fun tensorProto(name: String, dims: LongArray, values: FloatArray): ByteArray {
    val raw = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    values.forEach { raw.putFloat(it) }
    return Proto().apply {
        dims.forEach { varint(1, it) }
        varint(2, 1) // FLOAT
        string(8, name)
        bytes(9, raw.array())
    }.toByteArray()
}

// Float tensor of shape [batch, dim] with a symbolic batch dimension
private fun valueInfo(name: String, dim: Long): ByteArray {
    val shape = Proto().apply {
        bytes(1, Proto().apply { string(2, "batch") }.toByteArray())
        bytes(1, Proto().apply { varint(1, dim) }.toByteArray())
    }.toByteArray()
    val tensorType = Proto().apply { varint(1, 1); bytes(2, shape) }.toByteArray()
    return Proto().apply {
        string(1, name)
        bytes(2, Proto().apply { bytes(1, tensorType) }.toByteArray())
    }.toByteArray()
}

// Minimal protobuf writer: varint (wire type 0) and length-delimited (wire type 2) fields
private class Proto {
    private val out = ByteArrayOutputStream()

    private fun rawVarint(value: Long) {
        var v = value
        while (v and 0x7FL.inv() != 0L) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write(v.toInt())
    }

    fun varint(field: Int, value: Long) { rawVarint((field shl 3).toLong()); rawVarint(value) }
    fun bytes(field: Int, value: ByteArray) { rawVarint(((field shl 3) or 2).toLong()); rawVarint(value.size.toLong()); out.write(value) }
    fun string(field: Int, value: String) = bytes(field, value.toByteArray(Charsets.UTF_8))
    fun toByteArray(): ByteArray = out.toByteArray()
}
