package org.example.project.components.terminal.integrations.amiePilot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** predict / predictions / scores on top of inference's "label:score" lines */
class PredictionTest {
    private fun model(vararg answers: String) = object : InteractionModel {
        override fun inference(text: String): List<String> = answers.toList()
        override fun getStatus(): Any = "test"
        override fun close() {}
    }

    @Test
    fun `predict is the label of the best answer`() {
        assertEquals("lights", model("lights:0.67", "weather:0.49", "greeting:0.33").predict("sunny"))
    }

    @Test
    fun `predictions split label and score and keep the order`() {
        val p = model("lights:0.67", "weather:0.49").predictions("sunny")
        assertEquals(listOf("lights", "weather"), p.map { it.label })
        assertEquals(0.67f, p[0].score)
    }

    @Test
    fun `scores map label to score, best first`() {
        val s = model("lights:0.67", "weather:0.49", "greeting:0.33").scores("sunny")
        assertEquals(listOf("lights", "weather", "greeting"), s.keys.toList())
        assertEquals(0.49f, s["weather"])
    }

    @Test
    fun `answers without a score stay whole`() {
        assertEquals(Prediction("echo: hello", null), Prediction.parse("echo: hello"))
        assertEquals(Prediction("B-PER", null), Prediction.parse("B-PER"))
        assertEquals(Prediction("the:B-PER", null), Prediction.parse("the:B-PER"))
        assertEquals(Prediction("a:b", 0.5f), Prediction.parse("a:b:0.5"))
        // A number outside 0..1 is not a probability: a token with its class index stays whole
        assertEquals(Prediction("the:3", null), Prediction.parse("the:3"))
        assertEquals(Prediction("x:1.5", null), Prediction.parse("x:1.5"))
        assertNull(model("echo: hello").scores("hi")["echo: hello"])
        assertEquals("echo: hello", model("echo: hello").predict("hi"))
    }

    @Test
    fun `predict on no answer fails instead of guessing`() {
        assertFailsWith<IllegalStateException> { model().predict("hi") }
    }
}
