package org.example.project.components.terminal.window.controller.jetbrainDLBuilder

import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.initializer.GlorotUniform
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.api.core.optimizer.Adam
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import java.util.Locale

fun run() {
    val x = arrayOf(
        floatArrayOf(0f, 0f),
        floatArrayOf(0f, 1f),
        floatArrayOf(1f, 0f),
        floatArrayOf(1f, 1f)
    )
    val y = floatArrayOf(0f, 1f, 1f, 0f)
    val dataset = OnHeapDataset.create(x, y)

    val model = Sequential.of(
        Input(2),
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
            batchSize = 4
        )

        println("\n--- Predictions ---")

        x.forEach { input ->
            val prediction = it.predict(input) // Direct value!
            println("Input: [${input[0]}, ${input[1]}] -> Output: ${String.format(Locale.US, "%.4f", prediction)}")
        }
    }
}