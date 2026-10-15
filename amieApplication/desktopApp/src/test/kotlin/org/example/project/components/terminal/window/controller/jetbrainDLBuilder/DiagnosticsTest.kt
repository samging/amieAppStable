package org.example.project.components.terminal.window.controller.jetbrainDLBuilder

import org.example.project.components.terminal.window.controller.modules.computeMetrics
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class DiagnosticsTest {
    private val labels = listOf("greeting", "lights", "weather")

    @Test
    fun `a tiny validation and labels with one row are flagged`() {
        val tiny = Diagnostics.aboutValidation(labels, intArrayOf(0, 1, 2, 0))
        assertTrue(tiny.any { it.contains("only 4 rows") }, tiny.toString())
        val oneRow = Diagnostics.aboutValidation(labels, IntArray(12) { if (it == 0) 2 else it % 2 })
        assertTrue(oneRow.any { it.contains("fewer than 2 validation rows: weather") }, oneRow.toString())
        assertTrue(Diagnostics.aboutValidation(labels, IntArray(30) { it % 3 }).isEmpty())
    }

    @Test
    fun `memorising, a weak head and a confused label are named`() {
        // 30 rows, the head answers 'lights' for almost everything
        val truth = IntArray(30) { it % 3 }
        val prediction = IntArray(30) { if (it % 3 == 1 || it % 5 == 0) 1 else if (it % 3 == 0) 0 else 1 }
        val metrics = computeMetrics(labels, truth, prediction, trainSize = 100, warnings = emptyList())
        val lines = Diagnostics.aboutModel(metrics, labels, trainAccuracy = 0.99)
        assertTrue(lines.any { it.contains("memorises") }, lines.toString())
        assertTrue(lines.any { it.contains("label 'weather' is weak") && it.contains("taken for 'lights'") }, lines.toString())
    }

    @Test
    fun `a head that barely beats the majority label is flagged, a good one is not`() {
        val truth = IntArray(40) { if (it < 30) 1 else it % 2 * 2 }   // 30 lights, 5 greeting, 5 weather
        val lazy = IntArray(40) { 1 }
        val lazyMetrics = computeMetrics(labels, truth, lazy, 100, emptyList())
        assertTrue(Diagnostics.aboutModel(lazyMetrics, labels, 0.75).any { it.contains("barely above always answering 'lights'") })
        val goodMetrics = computeMetrics(labels, truth, truth.copyOf(), 100, emptyList())
        assertTrue(Diagnostics.aboutModel(goodMetrics, labels, 1.0).isEmpty())
    }

    @Test
    fun `class weights lift a rare label`() {
        val random = Random(3)
        // 90 rows of class 0 near (1,0), 10 rows of class 1 near (0,1): heavy imbalance
        val x = Array(100) { i -> if (i < 90) floatArrayOf(1f + (random.nextFloat() - 0.5f) * 0.6f, (random.nextFloat() - 0.5f) * 0.6f) else floatArrayOf((random.nextFloat() - 0.5f) * 0.6f, 1f + (random.nextFloat() - 0.5f) * 0.6f) }
        val y = IntArray(100) { if (it < 90) 0 else 1 }
        val weighted = DenseNet(2, emptyList(), 2).apply { fit(x, y, epochs = 5, batchSize = 10, learningRate = 0.01f, classWeights = floatArrayOf(100f / (2 * 90), 100f / (2 * 10))) }
        val plain = DenseNet(2, emptyList(), 2).apply { fit(x, y, epochs = 5, batchSize = 10, learningRate = 0.01f) }
        val rare = (90 until 100).toList()
        val recallWeighted = rare.count { weighted.predict(x[it]) == 1 }
        val recallPlain = rare.count { plain.predict(x[it]) == 1 }
        assertTrue(recallWeighted >= recallPlain, "weighted $recallWeighted vs plain $recallPlain")
        assertTrue(recallWeighted >= 8, "weighted recall $recallWeighted of 10")
    }
}
