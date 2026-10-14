package org.example.project.components.terminal.integrations.amiePilot

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer
import java.nio.file.Paths
import java.time.Instant
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import org.example.project.components.terminal.window.controller.modelLoaders.ManagedSession
import org.example.project.components.terminal.window.controller.modelLoaders.OrtSessions
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.meanPooling
import org.example.project.components.terminal.window.controller.modules.modulesDir
import org.example.project.components.terminal.window.controller.modules.readModule
import java.io.File
import org.jetbrains.kotlin.fir.scopes.impl.overrides

//open fun requirements(req: listOf<String>?) {
//    if(req.isEmptyOrNull()) { return IllegalArgumentException("Can't be null or empty - program crash") }
//
//
//}
//
//open fun reactOn(req: ListOf<String>? = null,
//                 reactWith: (Any?) -> Unit,
//                 ) {}

interface ModelInterface {
    fun sayHello(text: String): List<String>
    fun getContextTensor(): OnnxTensor?
    fun close()
}


//@param: modelPath - path to the model directory
class BertOnnxModel(val modelPath: String) : ModelInterface {

    private val env = OrtSessions.env
    private val modelDir = modelPath
    private val _tensorList = mutableListOf<Array<Array<FloatArray>>>()
    private var lastHiddenStateTensor: OnnxTensor? = null
    var lastTokenIds: LongArray = LongArray(0)
        private set

    val outputTensor: List<Array<Array<FloatArray>>>
        get() = _tensorList

    // Only the last run's matrix is kept: every run used to add one more, which grew without bound while a model served
    private fun addOutputTensor(tensor: OnnxTensor) {
        @Suppress("UNCHECKED_CAST")
        val matrix = tensor.value as Array<Array<FloatArray>>
        _tensorList.clear()
        _tensorList.add(matrix)
    }

    // Managed so it runs in the background and shows up in / can be cancelled from OrtSessions
    val session: ManagedSession
    val tokenizer: HuggingFaceTokenizer
    init {
        session = OrtSessions.open("bert:${File(modelDir).name}", "$modelDir/model.onnx")
        tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("$modelDir/tokenizer.json"))
    }

    private val id2label = mapOf(
        0 to "O",
        1 to "B-PER", 2 to "I-PER",
        3 to "B-ORG", 4 to "I-ORG",
        5 to "B-LOC", 6 to "I-LOC",
        7 to "B-MISC", 8 to "I-MISC"
    )

    private fun runModel(
        ids: LongArray,
        attentionMask: LongArray,
        tokenTypeIds: LongArray,
        session: ManagedSession,
        env: OrtEnvironment
    ): List<String> {

        val idTensors = OnnxTensor.createTensor(env, LongBuffer.wrap(ids), longArrayOf(1, ids.size.toLong()))
        val attentionMaskTensors = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), longArrayOf(1, attentionMask.size.toLong()))
        val tokenTypeTensors = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), longArrayOf(1, tokenTypeIds.size.toLong()))


        val inputs = mapOf(
            "input_ids" to idTensors,
            "attention_mask" to attentionMaskTensors,
            "token_type_ids" to tokenTypeTensors
        )

        val results = idTensors.use {
            attentionMaskTensors.use {
                tokenTypeTensors.use {
                    session.run(inputs)
                }
            }
        }
//        println("---- raw results----")
//            val tensor = results.get("last_hidden_state").orElse(null) as OnnxTensor
//            @Suppress("UNCHECKED_CAST")
//            val floatData = tensor.value as Array<Array<FloatArray>>
//            val cls = floatData[0][0]
//            println("Maybe [CLS]: " + cls.contentToString())
//        println("---- E raw results----")

        session.outputs.keys.forEach { name ->
            println("Output: $name")
        }

        lastHiddenStateTensor = results.get("last_hidden_state").orElse(null) as? OnnxTensor

        val hiddenStateTensor = results.get("last_hidden_state").orElse(null) as? OnnxTensor
        println("HIDDEN TENSOR: $hiddenStateTensor")
        val tensorOutput = results.get("last_hidden_state").orElse(null) as? OnnxTensor
        if (tensorOutput != null) {
            addOutputTensor(tensorOutput)
        }
        println(results)
        println("tensorOutput: ${results.get("last_hidden_state").orElse(null)}")

        @Suppress("UNCHECKED_CAST")
        val logitBatch = results.get(0).value as Array<Array<FloatArray>>

        val labels = logitBatch[0].map { tokenLogits ->
            val maxIndex = tokenLogits.indices.maxByOrNull { i -> tokenLogits[i] } ?: 0
            id2label[maxIndex] ?: "O"
        }
        return labels
    }

    override fun sayHello(text: String): List<String>{
        if (text.isNotEmpty()) {
            val encoded = tokenizer.encode(text)
            lastTokenIds = encoded.ids

            for ((index, i) in encoded.tokens.indices.withIndex()) {
                val token = encoded.tokens[i]
                val id = encoded.ids[i]
                println("TOKEN: [$index] $token - $id")
            }

            val tokenTypeIds = encoded.typeIds ?: LongArray(encoded.ids.size) { 0L }
            val outputModel =
                runModel(encoded.ids, encoded.attentionMask, tokenTypeIds, session, env)

            outputModel.forEachIndexed { index, label ->
                println("OUTPUT: $index - $label")
            }

            return outputModel
        }
        else {
            logInternal("Text is empty")
            return emptyList()
        }
    }

    override fun getContextTensor() : OnnxTensor? {
        if (lastHiddenStateTensor != null)  {
            return lastHiddenStateTensor
        } else {
            logInternal("TensorList is empty | ${_tensorList.size}")
            return null
        }
        return null
    }

    override fun close() {
        session.close()
        tokenizer.close()
    }

    fun metadata() : Any {
        return session.metadata
    }
}
fun initAmie(text: String): List<String> {
    val inst = BertOnnxModel(BundledModels.ner)
    try {
        return inst.sayHello(text)
    } finally {
        inst.close() //[H]AutoCloseable, check it out!
    }
}

// No name: the bundled BERT NER model. With a name: the module modelBuilder exported to AMP/modules/<name>
class InitMod(private val moduleName: String? = null) {

    fun create() : InteractionModel = if (moduleName == null) nerModel() else moduleModel(moduleName)

    private fun nerModel(): InteractionModel {
        val inst = BertOnnxModel(BundledModels.ner)
        return object : InteractionModel {

            override fun getStatus(): Any {
                return inst.metadata()
            }

            override fun inference(text: String): List<String> {
                val res = inst.sayHello(text)
                logInternal("amie ${quoteForLog(text)} -> ${res.joinToString(" ")}", LogLevel.DATA)
                return res
            }

            override fun close() {
                inst.close()
            }

        }
    }

    private fun moduleModel(name: String): InteractionModel {
        val (dir, manifest) = readModule(name) ?: throw IllegalArgumentException("Module '$name' not found in ${modulesDir.absolutePath}")
        require(manifest.format == "onnx") { "Module '$name' has unsupported format ${manifest.format}" }

        val env = OrtSessions.env
        val head = OrtSessions.open("module:$name", File(dir, manifest.model).absolutePath)
        // The encoder is shared with every other session on the same folder; only the small head is this session's own
        val encoder = try {
            SharedEncoders.acquire(manifest.encoder)
        } catch (e: Throwable) {
            head.close()
            throw e
        }
        logInternal("initMod: '$name' session ready (${manifest.input} [${manifest.inputDim}] -> ${manifest.output} [${manifest.outputDim}])")

        return object : InteractionModel {

            override fun getStatus(): Any = manifest

            // text -> encoder hidden state -> mean pooling -> exported head
            override fun inference(text: String): List<String> {
                // The encoder keeps its last hidden state, so one text at a time goes through it
                val pooled = synchronized(encoder) {
                    encoder.sayHello(text)
                    val hidden = encoder.getContextTensor() ?: return emptyList<String>().also {
                        logInternal("initMod: '$name' encoder returned no hidden state", LogLevel.ERROR)
                    }
                    @Suppress("UNCHECKED_CAST")
                    meanPooling(hidden.value as Array<Array<FloatArray>>)
                }
                val scores = OnnxTensor.createTensor(env, arrayOf(pooled)).use { input ->
                    head.run(mapOf(manifest.input to input)).use { result ->
                        @Suppress("UNCHECKED_CAST")
                        (result.get(0).value as Array<FloatArray>)[0]
                    }
                }
                // "label:probability", most likely first
                val res = scores.mapIndexed { i, score -> (manifest.labels.getOrNull(i) ?: i.toString()) to score }
                    .sortedByDescending { it.second }
                    .map { (label, score) -> "$label:$score" }
                // The answer is data the script asked for, so it shows as a DATA line: the text, then every label with its score
                logInternal(
                    "$name ${quoteForLog(text)} -> " + res.joinToString { entry ->
                        val label = entry.substringBeforeLast(':')
                        val score = entry.substringAfterLast(':').toDoubleOrNull()
                        if (score == null) entry else "$label ${"%.3f".format(java.util.Locale.US, score)}"
                    },
                    LogLevel.DATA,
                )
                return res
            }

            override fun close() {
                head.close()
                SharedEncoders.release(manifest.encoder)
            }
        }
    }

}

/** The text of an inference as it is logged: one line, quoted, cut at 60 characters */
private fun quoteForLog(text: String): String {
    val one = text.replace('\n', ' ').trim()
    return "\"" + (if (one.length > 60) one.take(57) + "..." else one) + "\""
}

fun initContext(text: String, tokens: (matrix: LongArray) -> Unit = {}): Array<Array<FloatArray>>? {
    if (text.isBlank()) {
        logInternal("forwardContext: text is empty", LogLevel.ERROR)
        return null
    }
    val encoderPath = BundledModels.encoder
    val inst = SharedEncoders.acquire(encoderPath)
    try { synchronized(inst) {
        logInternal("forwardContext: running ${text.length} chars through bge")
        inst.sayHello(text)
        logInternal("forwardContext: ${inst.lastTokenIds.size} tokens ${inst.lastTokenIds.contentToString()}")
        tokens(inst.lastTokenIds)

        val contextTensor = inst.getContextTensor() ?: run {
            logInternal("forwardContext: model returned no last_hidden_state", LogLevel.ERROR)
            return null
        }
        // Copy out of the ORT tensor before the session is closed
        @Suppress("UNCHECKED_CAST")
        val matrix = contextTensor.value as Array<Array<FloatArray>>
        logInternal("forwardContext: last_hidden_state shape ${contextTensor.info.shape.contentToString()}")
        return matrix
    } } catch (e: Exception) {
        logInternal("forwardContext failed: ${e.message}", LogLevel.ERROR)
        return null
    } finally {
        SharedEncoders.release(encoderPath)
    }
}

// One encoder session for many texts; each result is that text's last_hidden_state
// Runs every text through the encoder; onProgress gets (done, total) after each one
fun embedTexts(texts: List<String>, encoder: String, onProgress: (Int, Int) -> Unit = { _, _ -> }): List<Array<Array<FloatArray>>> {
    // Shared with any open session on the same encoder, so a build never loads a second copy
    return SharedEncoders.use(encoder) { inst ->
        texts.mapIndexed { index, text ->
            synchronized(inst) {
                inst.sayHello(text)
                val hidden = inst.getContextTensor() ?: throw IllegalStateException("encoder returned no hidden state for '$text'")
                @Suppress("UNCHECKED_CAST")
                (hidden.value as Array<Array<FloatArray>>)
            }.also { onProgress(index + 1, texts.size) }
        }
    }
}

fun getTensor(text: String = ""): Array<Array<FloatArray>>? {
    val inst = BertOnnxModel(BundledModels.encoder)
    try {
        inst.sayHello(text)
        val contextTensors = inst.getContextTensor()
        @Suppress("UNCHECKED_CAST")
        return contextTensors?.value as Array<Array<FloatArray>>
    } catch (e: Exception) { logInternal(e.message.toString(), LogLevel.ERROR) }
    return null
}
fun getShapeContext() : List<Long> {
    val inst = BertOnnxModel(BundledModels.encoder)
    try {
        inst.sayHello("hello world")
        println("--- context tensors debug: ---")
        val contextTensors = inst.getContextTensor()
        println("printing informations gotten: " + contextTensors?.info?.shape?.contentToString())
        return listOf<Long>(contextTensors?.info?.shape?.get(0) ?: 0, contextTensors?.info?.shape?.get(1) ?: 0, contextTensors?.info?.shape?.get(2) ?: 0)
    } finally {
        inst.close() //[H]AutoCloseable, check it out!
    }
}