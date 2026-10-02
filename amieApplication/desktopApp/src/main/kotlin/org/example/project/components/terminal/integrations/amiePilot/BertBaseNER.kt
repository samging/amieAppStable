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
    fun sayHello(): List<String>
    fun getContextTensor(): MutableList<Array<Array<FloatArray>>>
    fun close()
}


//@param: modelPath - path to the model directory
class BertOnnxModel(val modelPath: String) : ModelInterface {

    private val env = OrtEnvironment.getEnvironment()
    private val modelDir = modelPath
    private val _tensorList = mutableListOf<Array<Array<FloatArray>>>()

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

        session.outputNames.forEach { name ->
            println("Output: $name") 
        }

        println(" last hidden state ${results.get("last_hidden_state")} ")
        val tensorOutput = results.get("logits").orElse(null) as? OnnxTensor
        if (tensorOutput != null) {
            addOutputTensor(tensorOutput)
        }
        println(results)
        println("tensorOutput: ${results.get("logits")}")

        val logitBatch = results.get(0).value as Array<Array<FloatArray>>

        val labels = logitBatch[0].map { tokenLogits ->
            val maxIndex = tokenLogits.indices.maxByOrNull { i -> tokenLogits[i] } ?: 0
            id2label[maxIndex] ?: "O"
        }
        return labels
    }

    override fun sayHello(): List<String>{
        val encoded = tokenizer.encode("This is Daniel, how's your day?")

        for((index, i) in encoded.tokens.indices.withIndex() ) {
            val token = encoded.tokens[i]
            val id = encoded.ids[i]
            println("TOKEN: [$index] $token - $id")
        }

        val tokenTypeIds = encoded.typeIds ?: LongArray(encoded.ids.size) { 0L }
        val outputModel = runModel(encoded.ids, encoded.attentionMask, tokenTypeIds, session, env)

        outputModel.forEachIndexed { index, label ->
            println("OUTPUT: $index - $label")
        }

        return outputModel
    }

    override fun getContextTensor() : MutableList<Array<Array<FloatArray>>> {
        if (_tensorList.isNotEmpty()) {
            return _tensorList
        } else {
            logToConsole(ResponseDto(time = Instant.now(), message = "TensorList is empty | ${_tensorList.size}"))
            return emptyList<Array<Array<FloatArray>>>().toMutableList()
        }
        return emptyList<Array<Array<FloatArray>>>().toMutableList()
    }

    override fun close() {
        session.close()
        env.close()
    }
}
fun initAmie(): List<String> {
    val inst = BertOnnxModel("/Users/samuel/Documents/onnx/bert")
    try {
        return inst.sayHello()
    } finally {
        inst.close() //[H]AutoCloseable, check it out!
    }
}

fun initContext() {
    val inst = BertOnnxModel("/Users/samuel/Documents/onnx/bert/contextualizedBert")
    try {
        inst.sayHello()
        val contextTensors = inst.getContextTensor()
        logToConsole(ResponseDto(time = Instant.now(), message = "${contextTensors.size}"))
    } finally {
        inst.close() //[H]AutoCloseable, check it out!
    }
}