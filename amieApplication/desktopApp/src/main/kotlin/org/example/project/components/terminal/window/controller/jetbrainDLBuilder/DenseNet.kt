package org.example.project.components.terminal.window.controller.jetbrainDLBuilder

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * A feed-forward classifier (input -> hidden layers with relu or tanh -> softmax) trained with Adam on plain Kotlin
 * arrays; no native library, so it runs everywhere the app does. Any number of hidden layers, none included: a
 * linear classifier. Weights are row-major [in][out], the layout denseStackToOnnx exports.
 */
class DenseNet(val inputDim: Int, val hidden: List<Int>, val outputDim: Int, val activation: String = "Relu", seed: Int = 42) {
    /** For the one-hidden-layer callers that still ask: the first hidden width, 0 for a linear head */
    constructor(inputDim: Int, hiddenDim: Int, outputDim: Int, seed: Int = 42) : this(inputDim, if (hiddenDim > 0) listOf(hiddenDim) else emptyList(), outputDim, "Relu", seed)

    class Layer(val inDim: Int, val outDim: Int) {
        val w = FloatArray(inDim * outDim)
        val b = FloatArray(outDim)
    }

    val layers: List<Layer> = (listOf(inputDim) + hidden + outputDim).zipWithNext { a, b -> Layer(a, b) }
    val hiddenDim: Int get() = hidden.firstOrNull() ?: 0
    val parameters: Long get() = layers.sumOf { (it.w.size + it.b.size).toLong() }
    private val useTanh = activation.equals("Tanh", ignoreCase = true)

    init {
        val random = Random(seed)
        layers.forEach { glorot(it.w, it.inDim, it.outDim, random) }
    }

    class EpochResult(val epoch: Int, val loss: Double, val accuracy: Double)

    /**
     * Trains on [x] with class indices [y]; returns the last epoch's mean loss and accuracy.
     * [onEpoch] is called after every epoch so callers can report progress.
     */
    fun fit(
        x: Array<FloatArray>,
        y: IntArray,
        epochs: Int = 150,
        batchSize: Int = 16,
        learningRate: Float = 0.01f,
        seed: Int = 42,
        /** One weight per class; the loss and gradient of a row are multiplied by its class's weight */
        classWeights: FloatArray? = null,
        onEpoch: (EpochResult) -> Unit = {},
    ): EpochResult {
        require(x.size == y.size && x.isNotEmpty()) { "fit needs the same number of inputs and labels" }
        require(classWeights == null || classWeights.size == outputDim) { "classWeights needs one weight per class" }
        val params = layers.flatMap { listOf(it.w, it.b) }
        val adam = Adam(params, learningRate)
        val random = Random(seed)
        var last = EpochResult(0, 0.0, 0.0)
        val order = x.indices.toMutableList()
        for (epoch in 1..epochs) {
            order.shuffle(random)
            var lossSum = 0.0
            var correct = 0
            for (start in order.indices step batchSize) {
                val batch = order.subList(start, minOf(start + batchSize, order.size))
                val grads = layers.map { Layer(it.inDim, it.outDim) }
                for (i in batch) {
                    val pass = forward(x[i])
                    val p = pass.activations.last()
                    val weight = classWeights?.get(y[i]) ?: 1f
                    lossSum += -ln(p[y[i]].toDouble().coerceAtLeast(1e-9)) * weight
                    if (argmax(p) == y[i]) correct++
                    backward(pass, y[i], grads, weight / batch.size)
                }
                adam.step(grads.flatMap { listOf(it.w, it.b) })
            }
            last = EpochResult(epoch, lossSum / x.size, correct.toDouble() / x.size)
            onEpoch(last)
        }
        return last
    }

    fun predictProbabilities(x: FloatArray): FloatArray = forward(x).activations.last()

    fun predict(x: FloatArray): Int = argmax(predictProbabilities(x))

    /** activations[0] is the input, activations[l + 1] the output of layer l (softmax for the last); preActivations[l] its z */
    private class Pass(val activations: List<FloatArray>, val preActivations: List<FloatArray>)

    private fun forward(x: FloatArray): Pass {
        val activations = mutableListOf(x)
        val pre = mutableListOf<FloatArray>()
        layers.forEachIndexed { l, layer ->
            val input = activations.last()
            val z = FloatArray(layer.outDim) { j ->
                var s = layer.b[j]
                for (i in 0 until layer.inDim) s += input[i] * layer.w[i * layer.outDim + j]
                s
            }
            pre += z
            activations += if (l == layers.lastIndex) softmax(z) else FloatArray(z.size) { activate(z[it]) }
        }
        return Pass(activations, pre)
    }

    private fun activate(z: Float): Float = if (useTanh) tanh(z) else if (z > 0f) z else 0f
    private fun activateGradient(z: Float, a: Float): Float = if (useTanh) 1f - a * a else if (z > 0f) 1f else 0f

    // Cross-entropy gradients, scaled so a batch contributes its mean
    private fun backward(pass: Pass, label: Int, grads: List<Layer>, scale: Float) {
        val output = pass.activations.last()
        var delta = FloatArray(outputDim) { k -> (output[k] - if (k == label) 1f else 0f) * scale }
        for (l in layers.indices.reversed()) {
            val layer = layers[l]
            val input = pass.activations[l]
            val g = grads[l]
            for (k in 0 until layer.outDim) g.b[k] += delta[k]
            for (i in 0 until layer.inDim) {
                val xi = input[i]
                if (xi == 0f) continue
                for (k in 0 until layer.outDim) g.w[i * layer.outDim + k] += xi * delta[k]
            }
            if (l == 0) break
            val z = pass.preActivations[l - 1]
            val next = FloatArray(layer.inDim) { i ->
                var s = 0f
                for (k in 0 until layer.outDim) s += layer.w[i * layer.outDim + k] * delta[k]
                s * activateGradient(z[i], input[i])
            }
            delta = next
        }
    }

    private class Adam(private val params: List<FloatArray>, private val lr: Float, private val beta1: Float = 0.9f, private val beta2: Float = 0.999f, private val eps: Float = 1e-7f) {
        private val m = params.map { FloatArray(it.size) }
        private val v = params.map { FloatArray(it.size) }
        private var t = 0

        fun step(grads: List<FloatArray>) {
            t++
            val correction1 = 1f - Math.pow(beta1.toDouble(), t.toDouble()).toFloat()
            val correction2 = 1f - Math.pow(beta2.toDouble(), t.toDouble()).toFloat()
            for (p in params.indices) {
                val param = params[p]; val g = grads[p]; val mp = m[p]; val vp = v[p]
                for (i in param.indices) {
                    mp[i] = beta1 * mp[i] + (1f - beta1) * g[i]
                    vp[i] = beta2 * vp[i] + (1f - beta2) * g[i] * g[i]
                    val mHat = mp[i] / correction1
                    val vHat = vp[i] / correction2
                    param[i] -= lr * mHat / (sqrt(vHat) + eps)
                }
            }
        }
    }

    companion object {
        private fun glorot(w: FloatArray, fanIn: Int, fanOut: Int, random: Random) {
            val limit = sqrt(6.0 / (fanIn + fanOut)).toFloat()
            for (i in w.indices) w[i] = (random.nextFloat() * 2f - 1f) * limit
        }

        fun softmax(z: FloatArray): FloatArray {
            val max = z.maxOrNull() ?: 0f
            val e = FloatArray(z.size) { exp((z[it] - max).toDouble()).toFloat() }
            val sum = e.sum()
            return FloatArray(z.size) { e[it] / sum }
        }

        fun argmax(values: FloatArray): Int = values.indices.maxByOrNull { values[it] } ?: 0
    }
}
