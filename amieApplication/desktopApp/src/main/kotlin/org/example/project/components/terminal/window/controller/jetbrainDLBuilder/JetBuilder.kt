package org.example.project.components.terminal.window.controller.jetbrainDLBuilder

import ai.onnxruntime.OnnxTensor
import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.initializer.GlorotUniform
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import org.jetbrains.kotlinx.dl.impl.preprocessing.mean
import java.util.Locale


//sentence -> mean pooling -> dense -> softmax
//down casting the 12 vectors into a 768 vector

fun meanPooling(tensor: Array<Array<FloatArray>>) : FloatArray {
    val contextualSize = tensor.size //batches
    val seqLength = tensor[0].size //sequences
    val hiddenSize = tensor[0][0].size //size of last layer
    val sentenceVector = FloatArray(hiddenSize)
    for (sequenceIdx in 0 until seqLength) {
        for (dim in 0 until hiddenSize) {
            sentenceVector[dim] += tensor[0][sequenceIdx][dim]
        }
    }
    for (dim in 0 until hiddenSize) {
        sentenceVector[dim] = sentenceVector[dim] / seqLength
    }

    println("sentenceVector: ${sentenceVector.contentToString()}")
    return sentenceVector
}

fun build(tensor: Array<Array<FloatArray>>, dim1: Long, dim2: Long, dim3: Long, modelName: String) {
    val pooledSentence = meanPooling(tensor) //loops over sequences and generates pooled data
    intentBuilder(pooledSentence, dim1, dim2, dim3)
}

fun intentBuilder(pooledSentence: FloatArray, dim1: Long, dim2: Long, dim3: Long) {
    val x = arrayOf(pooledSentence)
    val y = floatArrayOf(1f)
    val dataset = OnHeapDataset.create(x, y)

    val model = Sequential.of(
        Input(dim3),
        Dense(8, activation = Activations.Relu, kernelInitializer = GlorotUniform()),
        Dense(1, activation = Activations.Sigmoid, kernelInitializer = GlorotUniform())
    )

    model.use {
        it.compile(
            optimizer = Adam(learningRate = 0.05f),
            loss = Losses.MSE,
            metric = Metrics.MAE
        )

        println("--- Starting Training ---")

        it.fit(
            dataset = dataset,
            epochs = 150,
            batchSize = minOf(4, x.size)
        )

        println("\n--- Predictions ---")

        x.forEach { input ->
            val prediction = it.predict(input)
            println("Input shape: [${input.size}] -> Output: ${String.format(Locale.US, "%.4f", prediction)}")
        }
    }
}
