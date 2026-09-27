package org.example.project.components.terminal.integrations.amiePilot

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer
import java.nio.file.Paths

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
    fun close()
}



class BertOnnxModel(val modelPath: String) : ModelInterface {
    private val env = OrtEnvironment.getEnvironment()
    private val modelDir = "/Users/samuel/Downloads/bertNER"
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
        session: OrtSession,
        env: OrtEnvironment
    ): List<String> {
        val idTensors = OnnxTensor.createTensor(env, LongBuffer.wrap(ids), longArrayOf(1, ids.size.toLong()))
        val attentionMaskTensors = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), longArrayOf(1, attentionMask.size.toLong()))

        val inputs = mapOf(
            "input_ids" to idTensors,
            "attention_mask" to attentionMaskTensors
        )

        val results = idTensors.use {
            attentionMaskTensors.use {
                session.run(inputs)
            }
        }
        val logitBatch = results.get(0).value as Array<Array<FloatArray>>

        val labels = logitBatch[0].map { tokenLogits ->
            val maxIndex = tokenLogits.indices.maxByOrNull { i -> tokenLogits[i] } ?: 0
            id2label[maxIndex] ?: "O"
        }
        return labels
    }

    override fun sayHello(): List<String>{
        val encoded = tokenizer.encode("This is Samuel Fux, how's your day? Johnyyy I missyou")
        for((index, i) in encoded.tokens.indices.withIndex() ) {
            val token = encoded.tokens[i]
            val id = encoded.ids[i]
            println("TOKEN: [$index] $token - $id")
        }
        val outputModel = runModel(encoded.ids, encoded.attentionMask, session, env)
        outputModel.forEachIndexed { index, label ->
            println("OUTPUT: $index - $label")
        }
        return outputModel
    }

    override fun close() {
        session.close()
        env.close()
    }
}
fun initAmie(): List<String> {
    val inst = BertOnnxModel("/Users/samuel/Downloads/bertNER/model.onnx")
    try {
        return inst.sayHello()
    } finally {
        inst.close() //AutoCloseable, check it out!
    }
}