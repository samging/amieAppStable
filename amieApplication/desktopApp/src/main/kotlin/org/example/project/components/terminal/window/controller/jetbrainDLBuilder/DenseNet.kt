package org.example.project.components.terminal.window.controller.jetbrainDLBuilder

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A two-layer classifier (input -> relu hidden -> softmax) trained with Adam on plain Kotlin arrays.
 * It replaces the TensorFlow-backed KotlinDL model, which has no native library on Apple Silicon.
 * Weights are row-major [in][out], the layout denseStackToOnnx exports.
 */
class DenseNet(val inputDim: Int, val hiddenDim: Int, val outputDim: Int, seed: Int = 42) {
    val w1 = FloatArray(inputDim * hiddenDim)
    val b1 = FloatArray(hiddenDim)
    val w2 = FloatArray(hiddenDim * outputDim)
    val b2 = FloatArray(outputDim)

    init {
        val random = Random(seed)
        glorot(w1, inputDim, hiddenDim, random)
        glorot(w2, hiddenDim, outputDim, random)
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
        onEpoch: (EpochResult) -> Unit = {},
    ): EpochResult {
        require(x.size == y.size && x.isNotEmpty()) { "fit needs the same number of inputs and labels" }
        val adam = Adam(listOf(w1, b1, w2, b2), learningRate)
        val random = Random(seed)
        var last = EpochResult(0, 0.0, 0.0)
        val order = x.indices.toMutableList()
        for (epoch in 1..epochs) {
            order.shuffle(random)
            var lossSum = 0.0
            var correct = 0
            for (start in order.indices step batchSize) {
                val batch = order.subList(start, minOf(start + batchSize, order.size))
                val grads = Gradients(this)
                for (i in batch) {
                    val (z1, h, p) = forward(x[i])
                    lossSum += -ln(p[y[i]].toDouble().coerceAtLeast(1e-9))
                    if (argmax(p) == y[i]) correct++
                    backward(x[i], z1, h, p, y[i], grads, 1f / batch.size)
                }
                adam.step(listOf(grads.w1, grads.b1, grads.w2, grads.b2))
            }
            last = EpochResult(epoch, lossSum / x.size, correct.toDouble() / x.size)
            onEpoch(last)
        }
        return last
    }

    fun predictProbabilities(x: FloatArray): FloatArray = forward(x).third

    fun predict(x: FloatArray): Int = argmax(predictProbabilities(x))

    private fun forward(x: FloatArray): Triple<FloatArray, FloatArray, FloatArray> {
        val z1 = FloatArray(hiddenDim) { j ->
            var s = b1[j]
            for (i in 0 until inputDim) s += x[i] * w1[i * hiddenDim + j]
            s
        }
        val h = FloatArray(hiddenDim) { if (z1[it] > 0f) z1[it] else 0f }
        val z2 = FloatArray(outputDim) { k ->
            var s = b2[k]
            for (j in 0 until hiddenDim) s += h[j] * w2[j * outputDim + k]
            s
        }
        return Triple(z1, h, softmax(z2))
    }

    // Cross-entropy gradients, scaled so a batch contributes its mean
    private fun backward(x: FloatArray, z1: FloatArray, h: FloatArray, p: FloatArray, label: Int, g: Gradients, scale: Float) {
        val dz2 = FloatArray(outputDim) { k -> (p[k] - if (k == label) 1f else 0f) * scale }
        val dh = FloatArray(hiddenDim)
        for (j in 0 until hiddenDim) {
            var s = 0f
            for (k in 0 until outputDim) {
                g.w2[j * outputDim + k] += h[j] * dz2[k]
                s += w2[j * outputDim + k] * dz2[k]
            }
            dh[j] = if (z1[j] > 0f) s else 0f
        }
        for (k in 0 until outputDim) g.b2[k] += dz2[k]
        for (i in 0 until inputDim) {
            val xi = x[i]
            if (xi == 0f) continue
            for (j in 0 until hiddenDim) g.w1[i * hiddenDim + j] += xi * dh[j]
        }
        for (j in 0 until hiddenDim) g.b1[j] += dh[j]
    }

    private class Gradients(net: DenseNet) {
        val w1 = FloatArray(net.w1.size)
        val b1 = FloatArray(net.b1.size)
        val w2 = FloatArray(net.w2.size)
        val b2 = FloatArray(net.b2.size)
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
