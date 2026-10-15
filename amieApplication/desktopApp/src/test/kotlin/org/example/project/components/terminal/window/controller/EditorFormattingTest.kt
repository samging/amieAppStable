package org.example.project.components.terminal.window.controller

import kotlin.test.Test
import kotlin.test.assertEquals

class EditorFormattingTest {
    @Test
    fun `format re-indents by braces and leaves strings and comments alone`() {
        val messy = """
            |fun greet(name: String) {
            |log("hi " + name)   // a { in a comment
            |      if (name.isEmpty()) {
            |  log("{ not a brace }")
            |}
            |    listOf(1, 2)
            |    .map { it * 2 }
            |        .forEach { log("${'$'}it") }
            |}
            |val raw = ""${'"'}
            |   keep   this
            |""${'"'}
            |""".trimMargin()
        val expected = """
            |fun greet(name: String) {
            |    log("hi " + name)   // a { in a comment
            |    if (name.isEmpty()) {
            |        log("{ not a brace }")
            |    }
            |    listOf(1, 2)
            |        .map { it * 2 }
            |        .forEach { log("${'$'}it") }
            |}
            |val raw = ""${'"'}
            |   keep   this
            |""${'"'}
            |""".trimMargin()
        assertEquals(expected, EditorFormatting.format(messy))
        assertEquals(expected, EditorFormatting.format(expected), "formatting is stable")
    }

    @Test
    fun `enter keeps the indentation and opens a block`() {
        val text = "fun f() {\n    val x = 1"
        assertEquals(EditorFormatting.Insert("\n    ", 5), EditorFormatting.newline(text, text.length))
        val open = "fun f() {"
        assertEquals(EditorFormatting.Insert("\n    ", 5), EditorFormatting.newline(open, open.length))
        val between = "fun f() {}"
        assertEquals(EditorFormatting.Insert("\n    \n", 5), EditorFormatting.newline(between, 9))
        val nested = "    if (a) {}"
        assertEquals(EditorFormatting.Insert("\n        \n    ", 9), EditorFormatting.newline(nested, 12))
    }

    @Test
    fun `tab and shift-tab move the touched lines`() {
        val text = "a\nb\nc"
        val (indented, from, to) = EditorFormatting.indentLines(text, 2, 3)
        assertEquals("a\n    b\nc", indented)
        assertEquals(6 to 7, from to to)
        val (back, f2, t2) = EditorFormatting.dedentLines(indented, from, to)
        assertEquals(text, back)
        assertEquals(2 to 3, f2 to t2)
        // A selection across two lines moves both
        val (both, _, _) = EditorFormatting.indentLines(text, 0, 3)
        assertEquals("    a\n    b\nc", both)
    }
}
