package org.example.project.components.terminal.window.controller

import androidx.compose.ui.text.input.OffsetMapping

/**
 * A block of the script that can be folded away: a `fun`, `class`, `object` or `interface` whose line ends with
 * `{`. Folded, the header line keeps its `{`, the lines down to the matching `}` are hidden behind ` … `, and the
 * `}` joins the header line. [key] survives edits elsewhere in the file: the header's text plus its rank among
 * identical headers, so a fold stays where it was while other lines are typed.
 */
internal data class FoldRegion(val key: String, val headerLine: Int, val endLine: Int, val hiddenStart: Int, val hiddenEnd: Int) {
    /** Whether typing at [offset] (an offset in the original text) would land in the hidden part */
    fun covers(offset: Int) = offset in hiddenStart..hiddenEnd
    fun coversLine(line: Int) = line in headerLine..endLine
}

/** One run of the original text that stays visible, and where it starts in the transformed text */
internal data class FoldSegment(val from: Int, val to: Int, val at: Int)

/** The ` … ` standing for a folded region in the transformed text */
internal data class FoldPlaceholder(val region: FoldRegion, val at: Int, val length: Int)

/**
 * The script as the editor shows it with some regions folded: the transformed text, the visible segments with
 * their place in it, the placeholders, the original lines that are hidden, and the mapping between the two texts
 * that BasicTextField needs. A hidden offset maps onto the end of its placeholder; the placeholder maps back to
 * the start of the hidden part, so a click on it puts the caret there (and the editor then opens the fold).
 */
internal class FoldLayout(
    val original: String,
    val transformed: String,
    val segments: List<FoldSegment>,
    val placeholders: List<FoldPlaceholder>,
    val hiddenLines: Set<Int>,
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        for (seg in segments) if (offset >= seg.from && offset <= seg.to) return seg.at + (offset - seg.from)
        for (ph in placeholders) if (offset > ph.region.hiddenStart && offset < ph.region.hiddenEnd) return ph.at + ph.length
        return offset.coerceIn(0, transformed.length)
    }

    override fun transformedToOriginal(offset: Int): Int {
        for (seg in segments) if (offset >= seg.at && offset <= seg.at + (seg.to - seg.from)) return seg.from + (offset - seg.at)
        for (ph in placeholders) if (offset >= ph.at && offset <= ph.at + ph.length) return ph.region.hiddenStart
        return offset.coerceIn(0, original.length)
    }
}

/** Finds what can be folded in a script and lays the text out with some of it folded */
internal object EditorFolding {
    const val PLACEHOLDER = " … "
    private val declaration = Regex("""(?<![\w.])(fun|class|object|interface)\b""")

    /** The foldable blocks of [text], in order of their header line */
    fun regions(text: String): List<FoldRegion> {
        if (text.isEmpty()) return emptyList()
        val code = codeMask(text)
        val out = ArrayList<FoldRegion>()
        val seen = HashMap<String, Int>()
        var lineStart = 0
        var line = 0
        while (true) {
            val lineEnd = text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }
            val lineText = text.substring(lineStart, lineEnd)
            val match = declaration.find(lineText)
            if (match != null && code[lineStart + match.range.first]) {
                // The last character of the line that is code (not a comment) must be the opening brace
                var i = lineEnd - 1
                while (i >= lineStart && (text[i].isWhitespace() || !code[i])) i--
                if (i >= lineStart && text[i] == '{') {
                    val close = matchingBrace(text, code, i)
                    if (close > 0) {
                        val endLine = line + text.substring(lineStart, close).count { it == '\n' }
                        if (endLine > line) {
                            val header = lineText.trim()
                            val rank = seen.getOrDefault(header, 0)
                            seen[header] = rank + 1
                            out += FoldRegion("$header#$rank", line, endLine, i + 1, close)
                        }
                    }
                }
            }
            if (lineEnd >= text.length) break
            lineStart = lineEnd + 1
            line++
        }
        return out
    }

    /** [text] with the regions in [folded] hidden; a region inside another folded one is simply part of it */
    fun layout(text: String, regions: List<FoldRegion>, folded: Set<String>): FoldLayout {
        val active = regions.filter { it.key in folded }.sortedBy { it.hiddenStart }
        val segments = ArrayList<FoldSegment>()
        val placeholders = ArrayList<FoldPlaceholder>()
        val hidden = HashSet<Int>()
        val sb = StringBuilder(text.length)
        var o = 0
        for (r in active) {
            if (r.hiddenStart < o) continue
            segments += FoldSegment(o, r.hiddenStart, sb.length)
            sb.append(text, o, r.hiddenStart)
            placeholders += FoldPlaceholder(r, sb.length, PLACEHOLDER.length)
            sb.append(PLACEHOLDER)
            for (l in r.headerLine + 1..r.endLine) hidden += l
            o = r.hiddenEnd
        }
        segments += FoldSegment(o, text.length, sb.length)
        sb.append(text, o, text.length)
        return FoldLayout(text, sb.toString(), segments, placeholders, hidden)
    }

    /** The line (0-based) that holds [offset] */
    fun lineOf(text: String, offset: Int): Int {
        var line = 0
        val end = offset.coerceIn(0, text.length)
        for (i in 0 until end) if (text[i] == '\n') line++
        return line
    }

    private fun matchingBrace(text: String, code: BooleanArray, open: Int): Int {
        var depth = 0
        for (i in open until text.length) {
            if (!code[i]) continue
            when (text[i]) {
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) return i }
            }
        }
        return -1
    }

    /** Which characters are code rather than part of a string, a character literal or a comment */
    private fun codeMask(text: String): BooleanArray {
        val n = text.length
        val code = BooleanArray(n) { true }
        fun hide(from: Int, to: Int) { for (k in from until minOf(to, n)) code[k] = false }
        var i = 0
        while (i < n) {
            val c = text[i]
            when {
                c == '/' && i + 1 < n && text[i + 1] == '/' -> {
                    val end = text.indexOf('\n', i).let { if (it < 0) n else it }
                    hide(i, end); i = end
                }
                c == '/' && i + 1 < n && text[i + 1] == '*' -> {
                    val end = text.indexOf("*/", i + 2).let { if (it < 0) n else it + 2 }
                    hide(i, end); i = end
                }
                c == '"' && text.startsWith("\"\"\"", i) -> {
                    val end = text.indexOf("\"\"\"", i + 3).let { if (it < 0) n else it + 3 }
                    hide(i, end); i = end
                }
                c == '"' || c == '\'' -> {
                    var k = i + 1
                    while (k < n && text[k] != c && text[k] != '\n') { if (text[k] == '\\') k++; k++ }
                    val end = minOf(k + 1, n)
                    hide(i, end); i = end
                }
                else -> i++
            }
        }
        return code
    }
}
