package org.example.project.components.terminal.window.controller

/**
 * The automatic formatting of the sandbox editor: what Enter inserts, what Tab and Shift+Tab do to the lines of a
 * selection, and Format, which re-indents the whole script by its braces. Pure text functions, so they are unit-tested.
 */
object EditorFormatting {
    const val INDENT = "    "

    private const val OPENERS = "{(["
    private const val CLOSERS = "})]"

    /**
     * Every line re-indented to its depth in braces, parentheses and brackets (one [INDENT] per level), a line that
     * starts with a closer one level out, and a line that continues an expression (`.map`, `?.let`, `?:`, `&&`,
     * `||`) one level in. The inside of a multi-line string or comment is left exactly as it is.
     */
    fun format(text: String): String {
        val code = EditorFolding.codeMask(text)
        val out = StringBuilder(text.length + 64)
        var depth = 0
        var pos = 0
        val lines = text.split('\n')
        lines.forEachIndexed { index, line ->
            val first = line.indexOfFirst { !it.isWhitespace() }
            val startsInside = first >= 0 && !code[pos + first] && pos > 0 && !code[pos - 1]
            when {
                first < 0 -> out.append("")
                startsInside -> out.append(line)
                else -> {
                    var closers = 0
                    var k = first
                    while (k < line.length && code[pos + k] && line[k] in CLOSERS) { closers++; k++ }
                    val continuation = line.startsWith(".", first) || line.startsWith("?.", first) || line.startsWith("?:", first) ||
                        line.startsWith("&&", first) || line.startsWith("||", first)
                    val level = (depth - closers).coerceAtLeast(0) + (if (continuation) 1 else 0)
                    out.append(INDENT.repeat(level)).append(line.substring(first).trimEnd())
                }
            }
            for (k in line.indices) {
                if (!code[pos + k]) continue
                when (line[k]) {
                    in OPENERS -> depth++
                    in CLOSERS -> depth = (depth - 1).coerceAtLeast(0)
                }
            }
            pos += line.length + 1
            if (index < lines.lastIndex) out.append('\n')
        }
        return out.toString()
    }

    /** What Enter inserts and where the caret lands in it */
    data class Insert(val text: String, val caret: Int)

    /**
     * Enter at [caret]: a new line with the indentation of the current line; one level more after an opening
     * brace; and when the caret sits between a `{` and its `}`, the `}` is moved to its own line one level out,
     * the caret staying on the empty line between them.
     */
    fun newline(text: String, caret: Int): Insert {
        val at = caret.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', at - 1) + 1
        val line = text.substring(lineStart, at)
        val indent = line.takeWhile { it == ' ' }
        val code = EditorFolding.codeMask(text)
        val before = line.trimEnd()
        val lastIndex = lineStart + before.length - 1
        val opens = before.isNotEmpty() && before.last() in OPENERS && code.getOrElse(lastIndex) { true }
        val next = text.getOrNull(at)
        val closesNext = next != null && next in CLOSERS && code.getOrElse(at) { true }
        return when {
            opens && closesNext -> {
                val inner = "\n$indent$INDENT"
                Insert("$inner\n$indent", inner.length)
            }
            opens -> "\n$indent$INDENT".let { Insert(it, it.length) }
            else -> "\n$indent".let { Insert(it, it.length) }
        }
    }

    /** The text with one [INDENT] added at the start of every line the range [from, to) touches, and the new range */
    fun indentLines(text: String, from: Int, to: Int): Triple<String, Int, Int> = shiftLines(text, from, to, +1)

    /** The text with one [INDENT] (or fewer leading spaces) taken from every line the range touches */
    fun dedentLines(text: String, from: Int, to: Int): Triple<String, Int, Int> = shiftLines(text, from, to, -1)

    private fun shiftLines(text: String, from: Int, to: Int, direction: Int): Triple<String, Int, Int> {
        val start = text.lastIndexOf('\n', (minOf(from, to) - 1).coerceAtLeast(0)).let { if (minOf(from, to) == 0) -1 else it } + 1
        val endExclusive = maxOf(from, to).let { e -> if (e > start && text.getOrNull(e - 1) == '\n') e - 1 else e }
        val end = text.indexOf('\n', endExclusive).let { if (it < 0) text.length else it }
        val block = text.substring(start, end)
        var firstDelta = 0
        var total = 0
        val shifted = block.split('\n').mapIndexed { i, line ->
            val delta = if (direction > 0) INDENT.length else -minOf(line.takeWhile { it == ' ' }.length, INDENT.length)
            if (i == 0) firstDelta = delta
            total += delta
            if (delta >= 0) INDENT + line else line.substring(-delta)
        }.joinToString("\n")
        val result = text.substring(0, start) + shifted + text.substring(end)
        val newFrom = (minOf(from, to) + firstDelta).coerceAtLeast(start)
        val newTo = (maxOf(from, to) + total).coerceAtLeast(newFrom)
        return Triple(result, newFrom, newTo)
    }
}
