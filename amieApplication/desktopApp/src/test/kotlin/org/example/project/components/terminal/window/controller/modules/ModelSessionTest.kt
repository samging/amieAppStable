package org.example.project.components.terminal.window.controller.modules

import org.example.project.components.terminal.integrations.amiePilot.Ask
import org.example.project.components.terminal.integrations.amiePilot.InteractionModel
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** A model session over a Mockito model: what it counts, what it logs, and what a closed session still allows */
class ModelSessionTest {
    private fun session(model: InteractionModel): Pair<ModelSession, File> {
        val log = File.createTempFile("sessions", ".log").apply { deleteOnExit() }
        return ModelSession(id = 42, name = "mocked", model = model, logFile = log) to log
    }

    @Test
    fun `inference goes to the model, is counted and logged with the answer`() {
        val model = mock<InteractionModel>()
        // predict and scores tell the model what they are for (Ask), so the model logs only that; a plain
        // inference goes through the one-argument call. A mock answers nothing on the default method unless told
        whenever(model.inference(eq("turn the lights on"), any())).thenReturn(listOf("lights:0.9", "weather:0.1"))
        whenever(model.inference("turn the lights on")).thenReturn(listOf("lights:0.9", "weather:0.1"))
        val (s, log) = session(model)

        assertEquals("lights", s.predict("turn the lights on"))
        assertEquals(mapOf("lights" to 0.9f, "weather" to 0.1f), s.scores("turn the lights on"))

        verify(model).inference("turn the lights on", Ask.PREDICT)
        verify(model).inference("turn the lights on", Ask.SCORES)
        verify(model, never()).inference("turn the lights on")
        assertEquals(2, s.runs)
        assertEquals(0, s.running)
        val lines = log.readText()
        assertTrue(lines.contains("mocked #42 run") && lines.contains("\"turn the lights on\"") && lines.contains("lights:0.9"), lines)
    }

    @Test
    fun `a model that throws is logged as an error and the exception reaches the script`() {
        val model = mock<InteractionModel>()
        whenever(model.inference(any())).thenThrow(IllegalStateException("encoder gone"))
        val (s, log) = session(model)

        assertFailsWith<IllegalStateException> { s.inference("hi") }
        assertEquals(1, s.runs)
        assertTrue(log.readText().contains("error"), log.readText())
    }

    @Test
    fun `close closes the model once and a closed session refuses inference`() {
        val model = mock<InteractionModel>()
        val (s, log) = session(model)

        s.close()
        s.close()

        verify(model, times(1)).close()
        assertTrue(s.closed)
        assertFailsWith<IllegalStateException> { s.inference("hi") }
        verify(model, never()).inference(any())
        assertTrue(log.readText().contains("closed after 0 runs"), log.readText())
    }

    @Test
    fun `getStatus is the model's own`() {
        val model = mock<InteractionModel>()
        whenever(model.getStatus()).thenReturn(mapOf("labels" to listOf("a", "b")))
        val (s, _) = session(model)
        assertEquals(mapOf("labels" to listOf("a", "b")), s.getStatus())
    }
}
