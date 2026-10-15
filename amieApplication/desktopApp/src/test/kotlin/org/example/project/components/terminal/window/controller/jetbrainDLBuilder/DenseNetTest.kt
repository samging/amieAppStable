package org.example.project.components.terminal.window.controller.jetbrainDLBuilder

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DenseNetTest {
    /** Three clusters in 4 dimensions, one per class, with noise */
    private fun clusters(n: Int, seed: Int = 1): Pair<Array<FloatArray>, IntArray> {
        val random = Random(seed)
        val centres = listOf(floatArrayOf(1f, 0f, 0f, 0f), floatArrayOf(0f, 1f, 0f, 0f), floatArrayOf(0f, 0f, 1f, 1f))
        val x = Array(n) { i -> FloatArray(4) { d -> centres[i % 3][d] + (random.nextFloat() - 0.5f) * 0.4f } }
        val y = IntArray(n) { it % 3 }
        return x to y
    }

    private fun accuracy(net: DenseNet, x: Array<FloatArray>, y: IntArray) = x.indices.count { net.predict(x[it]) == y[it] }.toDouble() / x.size

    @Test
    fun `a linear head, one hidden layer and two hidden layers all learn separable clusters`() {
        val (x, y) = clusters(90)
        val (tx, ty) = clusters(30, seed = 7)
        for (hidden in listOf(emptyList(), listOf(8), listOf(16, 8))) {
            val net = DenseNet(4, hidden, 3)
            val last = net.fit(x, y, epochs = 60, batchSize = 8, learningRate = 0.02f)
            assertTrue(last.accuracy > 0.95, "$hidden trained to ${last.accuracy}")
            assertTrue(accuracy(net, tx, ty) > 0.9, "$hidden generalises to ${accuracy(net, tx, ty)}")
            assertEquals(hidden.size + 1, net.layers.size)
        }
    }

    @Test
    fun `tanh works as the hidden activation`() {
        val (x, y) = clusters(90)
        val net = DenseNet(4, listOf(8), 3, activation = "Tanh")
        assertTrue(net.fit(x, y, epochs = 60, batchSize = 8, learningRate = 0.02f).accuracy > 0.95)
    }

    @Test
    fun `parameters are counted and probabilities sum to one`() {
        val net = DenseNet(4, listOf(8), 3)
        assertEquals((4 * 8 + 8 + 8 * 3 + 3).toLong(), net.parameters)
        assertEquals(0, DenseNet(4, emptyList(), 3).hiddenDim)
        val p = net.predictProbabilities(floatArrayOf(1f, 0f, 0f, 0f))
        assertEquals(1.0, p.sum().toDouble(), 1e-4)
        assertFailsWith<IllegalArgumentException> { net.fit(emptyArray(), IntArray(0)) }
    }
}
