package org.example.project.components.terminal.window.controller.modules

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.components.terminal.window.controller.LogLevel
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
)

@Serializable
data class ModuleRegistry(val modules: Map<String, String> = emptyMap())

private val registryFile: File get() = File(modulesDir, "modules.json")

fun moduleDir(name: String): File = File(modulesDir, name.replace(Regex("""[^A-Za-z0-9._-]"""), "_"))

fun readRegistry(): ModuleRegistry =
    runCatching { moduleJson.decodeFromString<ModuleRegistry>(registryFile.readText()) }.getOrElse { ModuleRegistry() }

fun writeModule(manifest: ModuleManifest, onnxBytes: ByteArray): File {
    val dir = moduleDir(manifest.name).apply { mkdirs() }
    val modelFile = File(dir, manifest.model)
    modelFile.writeBytes(onnxBytes)
    File(dir, "module.json").writeText(moduleJson.encodeToString(manifest))
    val registry = readRegistry()
    registryFile.writeText(moduleJson.encodeToString(registry.copy(modules = registry.modules + (manifest.name to dir.name))))
    logInternal("module '${manifest.name}' exported to ${modelFile.absolutePath}")
    return modelFile
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
