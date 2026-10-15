package org.example.project.components.terminal.window.controller.jetbrainDLBuilder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreprocessorTest {
    private fun rows(label: String, vararg texts: String) = texts.map { LabeledText(it, label) }
    private val good = rows("greeting", "hello there", "hi, how are you", "good morning", "hey amie", "good evening", "nice to meet you") +
        rows("lights", "turn the lights on", "switch off the kitchen light", "dim the lights", "lights off please", "lamp on", "brighten the desk lamp") +
        rows("weather", "what's the weather", "will it rain", "is it cold outside", "how hot is it", "do i need an umbrella", "forecast for tonight")

    @Test
    fun `clean data passes with a summary and no changes`() {
        val r = Preprocessor.run(good)
        assertTrue(r.ok, r.errors.toString())
        assertEquals(18, r.samples.size)
        assertTrue(r.dropped.isEmpty())
        assertTrue(r.notes.first().startsWith("18 rows in, 18 kept; 3 labels:"), r.notes.first())
    }

    @Test
    fun `whitespace and label spelling are normalised, repeats and junk are dropped with a reason`() {
        val messy = good + listOf(
            LabeledText("  hello   there ", "Greeting"),      // a repeat under another spelling of the label
            LabeledText("", "lights"),                        // empty
            LabeledText("???", "lights"),                     // no letters
            LabeledText("x", "weather"),                      // one character
            LabeledText("lamp on", "lights"),                 // exact repeat
        )
        val r = Preprocessor.run(messy)
        assertTrue(r.ok, r.errors.toString())
        assertEquals(18, r.samples.size)
        assertEquals(setOf("greeting", "lights", "weather"), r.samples.map { it.label }.toSet())
        assertTrue(r.notes.any { it.contains("'Greeting' read as 'greeting'") }, r.notes.toString())
        val reasons = r.dropped.map { it.reason }
        assertTrue("repeated row" in reasons && "empty text" in reasons && "no letters or digits in the text" in reasons && "a single character" in reasons, reasons.toString())
    }

    @Test
    fun `the same text under two labels keeps the majority and says so, with no majority both go`() {
        val data = good + listOf(LabeledText("hello there", "lights"), LabeledText("hello there", "greeting"), LabeledText("will it rain", "lights"))
        val r = Preprocessor.run(data)
        assertTrue(r.ok, r.errors.toString())
        assertEquals("greeting", r.samples.first { it.text == "hello there" }.label)
        assertTrue(r.dropped.any { it.text == "hello there" && it.reason.contains("kept as 'greeting'") })
        assertTrue(r.samples.none { it.text == "will it rain" }, "a tie drops both rows")
        assertTrue(r.dropped.any { it.text == "will it rain" && it.reason.contains("no majority") })
    }

    @Test
    fun `too few labels or rows, swapped columns and labels without variety are refused with a reason`() {
        assertTrue(Preprocessor.run(emptyList()).errors.first().startsWith("no labeled rows"))
        assertTrue(Preprocessor.run(rows("only", "a b", "c d", "e f", "g h", "i j")).errors.any { it.contains("at least 2 labels") })
        assertTrue(Preprocessor.run(good + rows("rare", "one", "two")).errors.any { it.contains("label 'rare' has 2 rows") })
        val swapped = good.map { LabeledText(it.label, it.text) }
        assertTrue(Preprocessor.run(swapped).errors.any { it.contains("probably swapped") }, Preprocessor.run(swapped).errors.toString())
        val flat = good + rows("echo", "same text", "same text!", "same text.", "same, text", "same text?", "(same text)")
        val r = Preprocessor.run(flat)
        assertFalse(r.ok)
        assertTrue(r.errors.any { it.contains("label 'echo' has only 1 distinct text") }, r.errors.toString())
    }

    @Test
    fun `imbalance and very short texts are warnings, not errors`() {
        val skewed = good + (1..60).map { LabeledText("turn light number $it on", "lights") }
        val r = Preprocessor.run(skewed)
        assertTrue(r.ok)
        assertTrue(r.warnings.any { it.contains("imbalanced") }, r.warnings.toString())
        val short = rows("a", "x1", "y1", "z1", "w1", "v1") + rows("b", "x2", "y2", "z2", "w2", "v2")
        assertTrue(Preprocessor.run(short).warnings.any { it.contains("very short") })
    }
}
