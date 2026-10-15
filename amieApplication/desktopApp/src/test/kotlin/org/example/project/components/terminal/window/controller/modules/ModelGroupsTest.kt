package org.example.project.components.terminal.window.controller.modules

import org.example.project.components.terminal.window.controller.envBuilder.DEFAULT_ENV
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Groups kept per env, what create/add/remove/delete do, and how a destroyed or renamed model leaves them */
class ModelGroupsTest {
    private fun manifest(name: String) = ModuleManifest(name = name, encoder = "/nowhere/bge", inputDim = 4, outputDim = 2, labels = listOf("a", "b"))
    private fun head(): ByteArray = denseStackToOnnx(listOf(DenseExport(FloatArray(8) { 0.1f }, FloatArray(2), 4, 2, "Softmax")), "embedding", "score", "t")
    private fun module(name: String): String { writeModule(manifest(name), head()); return name }

    @Test
    fun `a group is kept in its env with its models and icon`() {
        val a = module("groups-a-${System.nanoTime()}")
        val b = module("groups-b-${System.nanoTime()}")
        val env = "groups-env-${System.nanoTime()}"
        val name = "house ${System.nanoTime()}"
        val group = ModelGroups.create(name, listOf(a), env, icon = "home")
        assertEquals(listOf(a), group.models)
        assertEquals("home", group.icon)
        assertEquals(env, group.env)
        assertTrue(java.io.File(ModelGroups.dirOf(env), "groups.json").isFile)

        ModelGroups.add(name, b, env)
        assertEquals(listOf(a, b), ModelGroups.find(name, env)!!.models)
        assertNull(ModelGroups.find(name, DEFAULT_ENV))
        assertNotNull(ModelGroups.find(name))                       // any env when the current one has none
        assertEquals(listOf("$name ($env)"), ModelGroups.ofModel(a))
        assertTrue(env in ModelGroups.envs())

        assertTrue(ModelGroups.remove(name, a, env))
        assertEquals(listOf(b), ModelGroups.find(name, env)!!.models)
        assertTrue(ModelGroups.setIcon(name, "star", env))
        assertEquals("star", ModelGroups.find(name, env)!!.icon)
        assertTrue(ModelGroups.delete(name, env))
        assertNull(ModelGroups.find(name, env))
        assertTrue(moduleExists(b))
        deleteModule(a); deleteModule(b)
        ModelGroups.dirOf(env).deleteRecursively()
    }

    @Test
    fun `a group refuses a bad name and a model that is not exported`() {
        val env = "groups-env-${System.nanoTime()}"
        assertFailsWith<IllegalArgumentException> { ModelGroups.create("bad/name", emptyList(), env) }
        assertFailsWith<IllegalArgumentException> { ModelGroups.create("fine", listOf("no-such-model-${System.nanoTime()}"), env) }
        assertFalse(java.io.File(ModelGroups.dirOf(env), "groups.json").exists())
    }

    @Test
    fun `a destroyed model leaves its groups and a renamed one follows its name`() {
        val a = module("groups-c-${System.nanoTime()}")
        val b = module("groups-d-${System.nanoTime()}")
        val env = "groups-env-${System.nanoTime()}"
        val name = "pair"
        ModelGroups.create(name, listOf(a, b), env)
        assertTrue(renameModule(a, "$a-new"))
        assertEquals(listOf("$a-new", b), ModelGroups.find(name, env)!!.models)
        assertTrue(deleteModule(b))
        assertEquals(listOf("$a-new"), ModelGroups.find(name, env)!!.models)
        deleteModule("$a-new")
        ModelGroups.dirOf(env).deleteRecursively()
    }

    @Test
    fun `opening a group with no openable model is an error that names the groups`() {
        val env = "groups-env-${System.nanoTime()}"
        val e = assertFailsWith<IllegalArgumentException> { ModelGroups.open("nowhere", env) }
        assertTrue(e.message!!.contains("no model group 'nowhere'"), e.message)
        ModelGroups.create("empty", emptyList(), env)
        assertFailsWith<IllegalStateException> { ModelGroups.open("empty", env) }
        ModelGroups.dirOf(env).deleteRecursively()
    }
}
