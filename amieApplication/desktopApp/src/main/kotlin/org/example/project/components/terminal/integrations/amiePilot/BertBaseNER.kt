package org.example.project.components.terminal.integrations.amiePilot

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer
import java.nio.file.Paths
import java.time.Instant
import org.example.project.components.terminal.window.controller.logToConsole
import org.example.project.components.terminal.window.controller.ResponseDto

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

    private val env = OrtEnvironment.getEnvironment()
    private val modelDir = modelPath
    private val _tensorList = mutableListOf<Array<Array<FloatArray>>>()
    private var lastHiddenStateTensor: OnnxTensor? = null

    val outputTensor: List<Array<Array<FloatArray>>>
        get() = _tensorList

    private fun addOutputTensor(tensor: OnnxTensor) {
        @Suppress("UNCHECKED_CAST")
        val matrix = tensor.value as Array<Array<FloatArray>>
        _tensorList.add(matrix)
    }

    val session: OrtSession
    val tokenizer: HuggingFaceTokenizer
    init {
        session = env.createSession("$modelDir/model.onnx")
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
        session: OrtSession,
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

        session.outputNames.forEach { name ->
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
            logToConsole(ResponseDto(time = Instant.now(), message = "Text is empty"))
            return emptyList()
        }
    }

    override fun getContextTensor() : OnnxTensor? {
        if (lastHiddenStateTensor != null)  {
            return lastHiddenStateTensor
        } else {
            logToConsole(ResponseDto(time = Instant.now(), message = "TensorList is empty | ${_tensorList.size}"))
            return null
        }
        return null
    }

    override fun close() {
        session.close()
        env.close()
    }
}
fun initAmie(): List<String> {
    val inst = BertOnnxModel("/Users/samuel/Documents/onnx/bert")
    try {
        return inst.sayHello("hello world")
    } finally {
        inst.close() //[H]AutoCloseable, check it out!
    }
}

fun initContext() {
    val inst = BertOnnxModel("/Users/samuel/Documents/onnx/bert/bge")
    try {
        inst.sayHello("hello world")
        val contextTensors = inst.getContextTensor()
        logToConsole(ResponseDto(time = Instant.now(), message = "${contextTensors}"))
    } finally {
        inst.close() //[H]AutoCloseable, check it out!
    }
}

fun getTensor(text: String = ""): Array<Array<FloatArray>>? {
    val inst = BertOnnxModel("/Users/samuel/Documents/onnx/bert/bge")
    try {
        inst.sayHello(text)
        val contextTensors = inst.getContextTensor()
        return contextTensors?.value as Array<Array<FloatArray>>
    } catch (e: Exception) { logToConsole(ResponseDto(time = Instant.now(), message = e.message.toString())) }
    return null
}
fun getShapeContext() : List<Long> {
    val inst = BertOnnxModel("/Users/samuel/Documents/onnx/bert/bge")
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