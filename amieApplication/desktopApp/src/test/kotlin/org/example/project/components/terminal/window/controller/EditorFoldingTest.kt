package org.example.project.components.terminal.window.controller

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The folding behind the sandbox editor: which blocks fold, what the folded text looks like, how offsets map */
class EditorFoldingTest {
    private val script = """
        |fun greet(name: String) {
        |    log("hello " + name)   // a { in a comment
        |    val s = "{ not a brace }"
        |}
        |val oneLiner = fun() { log("x") }
        |class Box {
        |    fun inner() {
        |        log("in")
        |    }
        |}
        |greet("you")
    """.trimMargin()

    @Test
    fun findsDeclarationsThatSpanLines() {
        val regions = EditorFolding.regions(script)
        assertEquals(listOf("fun greet(name: String) {#0", "class Box {#0", "fun inner() {#0"), regions.map { it.key })
        val greet = regions[0]
        assertEquals(0, greet.headerLine)
        assertEquals(3, greet.endLine)
        assertEquals('{', script[greet.hiddenStart - 1])
        assertEquals('}', script[greet.hiddenEnd])
        assertEquals(5, regions[1].headerLine)
        assertEquals(9, regions[1].endLine)
        assertEquals(6, regions[2].headerLine)
        assertEquals(8, regions[2].endLine)
    }

    @Test
    fun foldedLayoutHidesTheBodyAndKeepsTheBraces() {
        val regions = EditorFolding.regions(script)
        val layout = EditorFolding.layout(script, regions, setOf(regions[0].key))
        assertEquals("fun greet(name: String) { … }", layout.transformed.lineSequence().first())
        assertEquals(setOf(1, 2, 3), layout.hiddenLines)
        assertEquals(script.lines().size - 3, layout.transformed.lines().size)
    }

    @Test
    fun nestedFoldInsideAFoldedBlockIsSwallowed() {
        val regions = EditorFolding.regions(script)
        val layout = EditorFolding.layout(script, regions, regions.map { it.key }.toSet())
        assertEquals(2, layout.placeholders.size)
        assertEquals(setOf(1, 2, 3, 6, 7, 8, 9), layout.hiddenLines)
    }

    @Test
    fun offsetsMapBothWaysWithinBoundsAndInOrder() {
        val regions = EditorFolding.regions(script)
        val greet = regions[0]
        val layout = EditorFolding.layout(script, regions, setOf(greet.key))
        var last = -1
        for (o in 0..script.length) {
            val t = layout.originalToTransformed(o)
            assertTrue(t in 0..layout.transformed.length, "o=$o t=$t")
            assertTrue(t >= last, "not monotonic at original $o")
            last = t
        }
        last = -1
        for (t in 0..layout.transformed.length) {
            val o = layout.transformedToOriginal(t)
            assertTrue(o in 0..script.length, "t=$t o=$o")
            assertTrue(o >= last, "not monotonic at transformed $t")
            last = o
        }
        assertEquals(0, layout.transformedToOriginal(0))
        assertEquals(greet.hiddenStart, layout.transformedToOriginal(layout.placeholders[0].at))
        val afterBrace = greet.hiddenEnd + 1
        assertEquals(afterBrace, layout.transformedToOriginal(layout.originalToTransformed(afterBrace)))
        assertEquals(script.length, layout.transformedToOriginal(layout.transformed.length))
    }

    @Test
    fun bracesInStringsAndCommentsDoNotCount() {
        val text = "fun a() {\n    val s = \"}\"  // }\n    log(s)\n}\n"
        val region = EditorFolding.regions(text).single()
        assertEquals(3, region.endLine)
        assertEquals(text.lastIndexOf('}'), region.hiddenEnd)
    }

    @Test
    fun unbalancedBlockIsNotFoldable() {
        assertTrue(EditorFolding.regions("fun a() {\n    log(1)\n").isEmpty())
    }
}
