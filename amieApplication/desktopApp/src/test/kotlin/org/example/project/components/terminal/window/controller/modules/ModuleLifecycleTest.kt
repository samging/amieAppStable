package org.example.project.components.terminal.window.controller.modules

import org.example.project.components.terminal.integrations.amiePilot.Ask
import org.example.project.components.terminal.integrations.amiePilot.InteractionModel
import org.example.project.components.terminal.window.controller.sourceFile.DataSource
import org.example.project.getDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Modules written, trained again, renamed; data copied into AMP; what predict asks of a model */
class ModuleLifecycleTest {
    private fun manifest(name: String, description: String? = null) = ModuleManifest(
        name = name, encoder = "/nowhere/bge", inputDim = 4, outputDim = 2, labels = listOf("a", "b"), description = description,
    )

    private fun head(): ByteArray =
        denseStackToOnnx(listOf(DenseExport(FloatArray(8) { 0.1f }, FloatArray(2), 4, 2, "Softmax")), "embedding", "score", "t")

    @Test
    fun `a module trained again keeps its description and guide, and its earlier metrics`() {
        val name = "lifecycle-retrain-${System.nanoTime()}"
        val dir = writeModule(manifest(name, "the first description"), head()).parentFile
        val guide = guideFile(dir).also { it.writeText("# $name\n\nedited by hand\n") }
        appendMetrics(dir, computeMetrics(listOf("a", "b"), intArrayOf(0, 1), intArrayOf(0, 1), 2, emptyList()))

        writeModule(manifest(name), head())

        val (_, again) = readModule(name)!!
        assertEquals("the first description", again.description)
        assertTrue(guide.readText().contains("edited by hand"))
        assertEquals(1, readMetricsHistory(dir).runs.size)
        assertTrue(deleteModule(name))
    }

    @Test
    fun `a module is renamed with its folder, manifest, guide title and registry entry`() {
        val name = "lifecycle-old-${System.nanoTime()}"
        val target = "$name-new"
        val dir = writeModule(manifest(name, "about it"), head()).parentFile
        writeServing(name, ServingSettings(port = 9999, path = "/models/$name"))

        assertTrue(renameModule(name, target))

        assertFalse(dir.exists())
        assertFalse(moduleExists(name))
        val (newDir, m) = readModule(target)!!
        assertEquals(target, m.name)
        assertEquals("about it", m.description)
        assertEquals("# $target", guideFile(newDir).readLines().first())
        assertEquals(newDir.name, readRegistry().modules[target])
        assertFalse(name in readRegistry().modules)
        // The default path follows the name, the port stays
        val serving = readServing(target)
        assertEquals(9999, serving.port)
        assertEquals("/models/$target/predict", serving.pathFor(target))
        assertTrue(deleteModule(target))
    }

    @Test
    fun `a rename refuses a bad name and a taken one`() {
        val a = "lifecycle-a-${System.nanoTime()}"
        val b = "lifecycle-b-${System.nanoTime()}"
        writeModule(manifest(a), head())
        writeModule(manifest(b), head())
        assertFailsWith<IllegalArgumentException> { renameModule(a, "has space") }
        assertFailsWith<IllegalArgumentException> { renameModule(a, b) }
        assertTrue(moduleExists(a))
        deleteModule(a); deleteModule(b)
    }

    @Test
    fun `a csv from outside AMP is copied in, one inside is used where it is`() {
        val outside = File.createTempFile("lifecycle-data", ".csv").apply { writeText("text,label\nhello,a\nbye,b\n"); deleteOnExit() }
        val copied = DataSource.importIntoAmp(outside)
        assertEquals(File(getDirectory().toString()).canonicalPath, copied.parentFile.canonicalPath)
        assertEquals(outside.readText(), copied.readText())
        assertEquals(copied.name, DataSource.locate(outside.name)?.name)

        val inside = File(getDirectory().toString(), "lifecycle-inside-${System.nanoTime()}.csv").apply { writeText("text,label\nhi,a\n") }
        assertEquals(inside.canonicalPath, DataSource.importIntoAmp(inside).canonicalPath)
        copied.delete(); inside.delete()
    }

    @Test
    fun `predict is the best label and says so to the model, scores keep every label`() {
        val asked = mutableListOf<Ask>()
        val model = object : InteractionModel {
            override fun close() {}
            override fun getStatus(): Any = "ok"
            override fun inference(text: String): List<String> = inference(text, Ask.INFERENCE)
            override fun inference(text: String, ask: Ask): List<String> { asked += ask; return listOf("lights:0.9", "weather:0.1") }
        }
        assertEquals("lights", model.predict("turn the lights on"))
        assertEquals(mapOf("lights" to 0.9f, "weather" to 0.1f), model.scores("turn the lights on"))
        assertEquals(listOf("lights:0.9", "weather:0.1"), model.inference("turn the lights on"))
        assertEquals(listOf(Ask.PREDICT, Ask.SCORES, Ask.INFERENCE), asked)
    }
}
