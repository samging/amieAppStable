package org.example.project.components.terminal.window.controller.jetbrainDLBuilder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ArchitectureTest {
    @Test
    fun `labels and descriptions name the layers`() {
        assertEquals("linear", Architecture(hidden = emptyList()).label)
        assertEquals("dense[64, 16]", Architecture(hidden = listOf(64, 16)).label)
        assertEquals("dense[768 -> 16] relu -> dense[16 -> 3] softmax", Architecture(hidden = listOf(16)).describe(768, 3))
        assertEquals("dense[768 -> 3] softmax", Architecture(hidden = emptyList()).describe(768, 3))
        assertEquals(4, Architecture.DEFAULT_CANDIDATES.size)
    }

    @Test
    fun `bad settings are refused when they are written, not when the build runs`() {
        assertFailsWith<IllegalArgumentException> { Architecture(hidden = listOf(0)) }
        assertFailsWith<IllegalArgumentException> { Architecture(epochs = 0) }
        assertFailsWith<IllegalArgumentException> { Architecture(learningRate = 2.0) }
        assertFailsWith<IllegalArgumentException> { Architecture(activation = "Sigmoid") }
        assertFailsWith<IllegalArgumentException> { BuildOptions(candidates = emptyList()) }
        assertFailsWith<IllegalArgumentException> { BuildOptions(pickBy = "loss") }
        assertFailsWith<IllegalArgumentException> { BuildOptions(validationFraction = 0.9) }
    }

    @Test
    fun `the best candidate is picked by the chosen score, then the other, then size`() {
        val results = listOf(
            CandidateResult("linear", emptyList(), 150, 2307, accuracy = 0.90, macroF1 = 0.88, trainAccuracy = 0.95, finalLoss = 0.2),
            CandidateResult("dense[16]", listOf(16), 150, 12355, accuracy = 0.92, macroF1 = 0.91, trainAccuracy = 0.99, finalLoss = 0.1),
            CandidateResult("dense[32]", listOf(32), 150, 24700, accuracy = 0.93, macroF1 = 0.91, trainAccuracy = 0.99, finalLoss = 0.1),
            CandidateResult("dense[64, 16]", listOf(64, 16), 150, 50000, accuracy = 0.93, macroF1 = 0.91, trainAccuracy = 1.0, finalLoss = 0.05),
        )
        assertEquals("dense[32]", pickBest(results, "macroF1").architecture, "same F1: the better accuracy, then the smaller head")
        assertEquals("dense[32]", pickBest(results, "accuracy").architecture)
        assertTrue(pickBest(results.take(1), "macroF1").architecture == "linear")
    }
}
