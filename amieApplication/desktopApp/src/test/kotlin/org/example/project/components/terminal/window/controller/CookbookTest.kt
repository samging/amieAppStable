package org.example.project.components.terminal.window.controller

import java.io.File
import javax.script.Compilable
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every Kotlin block of docs/cookbook.md must compile as a sandbox script, assembled exactly as the sandbox
 * assembles main.kts. Blocks that declare a Maven dependency are skipped: the library is not on this classpath.
 */
class CookbookTest {
    private fun cookbook(): File = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "docs/cookbook.md") }.firstOrNull { it.isFile } ?: fail("docs/cookbook.md not found above ${File("").absolutePath}")

    private fun blocks(markdown: String): List<Pair<Int, String>> {
        val out = mutableListOf<Pair<Int, String>>()
        var inside = false
        var start = 0
        val current = StringBuilder()
        markdown.lines().forEachIndexed { i, line ->
            when {
                !inside && line.trim() == "```kotlin" -> { inside = true; start = i + 2; current.clear() }
                inside && line.trim() == "```" -> { inside = false; out += start to current.toString() }
                inside -> current.append(line).append('\n')
            }
        }
        return out
    }

    @Test
    fun `every Examples menu script compiles`() {
        val engine = ScriptEngineCache.engine as Compilable
        val failures = SandboxExamples.all.mapNotNull { example ->
            val assembled = assembleScript(example.code)
            runCatching { engine.compile(assembled) }.exceptionOrNull()?.let { e ->
                val message = e.message?.lines()?.firstOrNull { l -> "error" in l.lowercase() } ?: e.message
                val at = Regex("kts:(\\d+):").find(message ?: "")?.groupValues?.get(1)?.toIntOrNull()
                "'${example.title}': $message" + (at?.let { assembled.lines().getOrNull(it - 1) }?.let { "\n    >> $it" } ?: "")
            }
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }

    @Test
    fun `every cookbook script compiles`() {
        val file = cookbook()
        val scripts = blocks(file.readText())
        assertTrue(scripts.size >= 10, "expected the cookbook's scripts, found ${scripts.size}")
        val engine = ScriptEngineCache.engine as Compilable
        val failures = scripts.mapNotNull { (line, code) ->
            if ("implementation(" in code || "dependency(" in code) return@mapNotNull null
            val assembled = assembleScript(code)
            runCatching { engine.compile(assembled) }.exceptionOrNull()?.let { e ->
                val message = e.message?.lines()?.firstOrNull { l -> "error" in l.lowercase() } ?: e.message
                // The engine names the line of the assembled script; show that line so the doc can be fixed
                val at = Regex("kts:(\\d+):").find(message ?: "")?.groupValues?.get(1)?.toIntOrNull()
                val offending = at?.let { assembled.lines().getOrNull(it - 1) }
                "cookbook.md:$line: $message" + (offending?.let { "\n    >> $it" } ?: "")
            }
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }
}
