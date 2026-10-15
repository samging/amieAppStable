package org.example.project.components.terminal.window.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import org.example.project.components.ui.viewport.AppTooltip
import org.example.project.ui.theme.EditorCurrentLine
import org.example.project.ui.theme.EditorCurrentLineEdge
import org.example.project.ui.theme.EditorFoldBackground
import org.example.project.ui.theme.GutterBackground
import org.example.project.ui.theme.LineNumberColor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.rememberTextMeasurer
import org.example.project.ui.theme.EditorIndentGuide
import org.example.project.ui.theme.EditorIndentGuideActive
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.PhraseLocation
import dev.snipme.highlights.model.SyntaxTheme
import org.example.project.ui.theme.CompletionBackground
import org.example.project.ui.theme.CompletionBorder
import org.example.project.ui.theme.CompletionSelected
import org.example.project.ui.theme.EditorAnnotationColor
import org.example.project.ui.theme.EditorApiColor
import org.example.project.ui.theme.EditorCommentColor
import org.example.project.ui.theme.EditorKeywordColor
import org.example.project.ui.theme.EditorNumberColor
import org.example.project.ui.theme.EditorPunctuationColor
import org.example.project.ui.theme.EditorStringColor
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.MutedFontColor
import kotlin.math.roundToInt

/*
 * The sandbox code editor: a BasicTextField whose visual transformation paints the Highlights analysis
 * (keywords, strings, numbers, comments) plus the SandboxApi names in their own colour, and a completion
 * dropdown under the caret while an identifier is typed (or on Ctrl+Space).
 */

private fun Color.rgb(): Int = toArgb() and 0xFFFFFF

/** Highlights theme in the editor palette; the API colour is added by [highlightCode], not by the library */
val sandboxSyntaxTheme: SyntaxTheme = SyntaxTheme(
    key = "amie",
    code = FontColor.rgb(),
    keyword = EditorKeywordColor.rgb(),
    string = EditorStringColor.rgb(),
    literal = EditorNumberColor.rgb(),
    comment = EditorCommentColor.rgb(),
    metadata = EditorAnnotationColor.rgb(),
    multilineComment = EditorCommentColor.rgb(),
    punctuation = EditorPunctuationColor.rgb(),
    mark = FontColor.rgb(),
)

/** Spans to paint over the code, and the string/comment ranges where neither API colour nor completion apply */
internal class EditorHighlighting(val spans: List<AnnotatedString.Range<SpanStyle>>, val quiet: List<PhraseLocation>) {
    fun isQuiet(offset: Int) = quiet.any { offset >= it.start && offset < it.end }

    companion object {
        val EMPTY = EditorHighlighting(emptyList(), emptyList())
    }
}

// `amie` itself, API functions when called and API properties when not: `source(`, `amie.source(`, `sessions.print()`
private val apiNameRegex: Regex by lazy {
    val functions = SandboxApi.functionNames.sortedByDescending { it.length }.joinToString("|")
    val properties = SandboxApi.propertyNames.sortedByDescending { it.length }.joinToString("|")
    Regex("""(?:(?<=amie\.)|(?<![\w.]))(?:amie\b(?!\s*\()|(?:$functions)(?=\s*\()|(?:$properties)\b(?!\s*\())""")
}

internal fun highlightCode(highlights: Highlights): EditorHighlighting {
    val code = highlights.getCode()
    val structure = runCatching { highlights.getCodeStructure() }.getOrNull() ?: return EditorHighlighting.EMPTY
    val theme = highlights.getTheme()
    val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    fun paint(locations: Set<PhraseLocation>, rgb: Int, weight: FontWeight? = null) {
        locations.forEach { spans += AnnotatedString.Range(SpanStyle(color = Color(rgb).copy(alpha = 1f), fontWeight = weight), it.start, it.end) }
    }
    // Later spans win where they overlap, so comments and strings are painted after keywords
    paint(structure.marks, theme.mark)
    paint(structure.punctuations, theme.punctuation)
    paint(structure.keywords, theme.keyword, FontWeight.SemiBold)
    paint(structure.literals, theme.literal)
    paint(structure.annotations, theme.metadata)
    paint(structure.strings, theme.string)
    paint(structure.comments, theme.comment)
    paint(structure.multilineComments, theme.multilineComment)
    val quiet = (structure.strings + structure.comments + structure.multilineComments).toList()
    val result = EditorHighlighting(spans, quiet)
    apiNameRegex.findAll(code).forEach { match ->
        if (!result.isQuiet(match.range.first)) {
            spans += AnnotatedString.Range(SpanStyle(color = EditorApiColor, fontWeight = FontWeight.SemiBold), match.range.first, match.range.last + 1)
        }
    }
    return result
}

/**
 * Paints [highlighting] over the text as the field shows it: with nothing folded the text itself; with folds, the
 * transformed text of [folds], every span moved into the visible segment it falls in and each placeholder in the
 * muted colour on its square. Spans that outrun the text (analysis a keystroke behind) are clipped or dropped.
 */
private fun foldTransformation(highlighting: EditorHighlighting, folds: FoldLayout) = VisualTransformation { text ->
    if (folds.placeholders.isEmpty() || text.text != folds.original) {
        val length = text.length
        val painted = buildAnnotatedString {
            append(text.text)
            highlighting.spans.forEach { span ->
                if (span.start >= 0 && span.end <= length && span.end > span.start) addStyle(span.item, span.start, span.end)
            }
        }
        return@VisualTransformation TransformedText(painted, OffsetMapping.Identity)
    }
    val painted = buildAnnotatedString {
        append(folds.transformed)
        for (seg in folds.segments) {
            if (seg.to <= seg.from) continue
            for (span in highlighting.spans) {
                val from = maxOf(span.start, seg.from)
                val to = minOf(span.end, seg.to)
                if (to > from) addStyle(span.item, seg.at + (from - seg.from), seg.at + (to - seg.from))
            }
        }
        for (ph in folds.placeholders) addStyle(SpanStyle(color = MutedFontColor, background = EditorFoldBackground), ph.at, ph.at + ph.length)
    }
    TransformedText(painted, folds)
}

/** The dropdown: which identifier is being completed and what matches it */
private data class Completion(
    val prefixStart: Int,
    val prefix: String,
    val afterReceiver: Boolean,
    val items: List<SandboxApi.Entry>,
    val selected: Int = 0,
)

private fun Char.isIdentifierPart() = isLetterOrDigit() || this == '_'

/** Completion for the identifier ending at [caret]; null when nothing is typed (unless [forced]) or nothing matches */
private fun completionAt(text: String, caret: Int, highlighting: EditorHighlighting, forced: Boolean): Completion? {
    if (caret < 0 || caret > text.length) return null
    var start = caret
    while (start > 0 && text[start - 1].isIdentifierPart()) start--
    if (text.getOrNull(caret)?.isIdentifierPart() == true && !forced) return null
    val prefix = text.substring(start, caret)
    val afterReceiver = text.substring(0, start).endsWith("${SandboxApi.RECEIVER}.")
    if (!forced && prefix.isEmpty() && !afterReceiver) return null
    if (prefix.firstOrNull()?.isDigit() == true) return null
    if (highlighting.isQuiet(start) && !forced) return null
    val items = buildList {
        if (!afterReceiver && SandboxApi.RECEIVER.startsWith(prefix, ignoreCase = true)) add(SandboxApi.receiverEntry)
        addAll(SandboxApi.complete(prefix, afterReceiver))
    }.take(8)
    if (items.isEmpty()) return null
    return Completion(start, prefix, afterReceiver, items)
}

/** What Tab inserts and the width of one indentation level, four spaces */
private const val INDENT = EditorFormatting.INDENT

/** The column at the left of the text: line numbers and the fold markers */
internal val GutterWidth = 56.dp

/** One visible line of the gutter: its number in the file, and the block that starts on it, if any */
private data class GutterLine(val number: Int, val region: FoldRegion?, val folded: Boolean)

/**
 * The editor: a gutter with the line numbers and a marker on every line that starts a foldable block, and the
 * text with the current line in a faint box. A BasicTextField whose visual transformation paints the Highlights
 * analysis (keywords, strings, numbers, comments) plus the SandboxApi names, and hides the folded blocks behind
 * " … "; the text behind it is always the whole script, so saving and running never see a fold. A completion
 * dropdown opens under the caret while an identifier is typed (or on Ctrl+Space). Cmd/Ctrl+Enter runs,
 * Cmd/Ctrl+Minus folds the block the caret is in, Cmd/Ctrl+Equals opens it; with Shift, all of them.
 */
@Composable
internal fun SandboxEditor(
    initialCode: String,
    highlights: Highlights,
    onCodeChange: (String) -> Unit,
    textStyle: TextStyle,
    viewportWidth: Dp,
    viewportHeight: Dp,
    onRun: () -> Unit = {},
    /** Goes up by one each time the strip's Format is pressed; the editor formats the whole script then */
    formatRequests: Int = 0,
) {
    var value by remember { mutableStateOf(TextFieldValue(initialCode)) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var completion by remember { mutableStateOf<Completion?>(null) }
    var folded by remember { mutableStateOf(setOf<String>()) }
    val highlighting = remember(highlights) { highlightCode(highlights) }
    val regions = remember(value.text) { EditorFolding.regions(value.text) }
    val folds = remember(value.text, regions, folded) { EditorFolding.layout(value.text, regions, folded) }
    val transformation = remember(highlighting, folds) { foldTransformation(highlighting, folds) }
    val density = LocalDensity.current

    fun replace(from: Int, to: Int, insert: String, caretOffset: Int = insert.length) {
        val text = value.text
        val updated = text.substring(0, from) + insert + text.substring(to)
        value = TextFieldValue(updated, TextRange(from + caretOffset))
        completion = null
        onCodeChange(updated)
    }

    /** The whole script re-indented; the caret stays on its line, at the same place in the line's content */
    fun formatAll() {
        val text = value.text
        val caret = value.selection.start.coerceIn(0, text.length)
        val line = EditorFolding.lineOf(text, caret)
        val lineStart = text.lastIndexOf('\n', caret - 1) + 1
        val inContent = (caret - lineStart - text.substring(lineStart, caret).takeWhile { it == ' ' }.length).coerceAtLeast(0)
        val formatted = EditorFormatting.format(text)
        if (formatted == text) return
        val newLineStart = formatted.split('\n').take(line).sumOf { it.length + 1 }
        val newLine = formatted.substring(newLineStart).substringBefore('\n')
        val newCaret = newLineStart + newLine.takeWhile { it == ' ' }.length + minOf(inContent, newLine.trimStart().length)
        value = TextFieldValue(formatted, TextRange(newCaret.coerceIn(0, formatted.length)))
        completion = null
        onCodeChange(formatted)
    }
    LaunchedEffect(formatRequests) { if (formatRequests > 0) formatAll() }

    /** Tab and Shift+Tab on the lines a selection touches */
    fun shiftSelection(direction: Int) {
        val (text, from, to) = if (direction > 0) EditorFormatting.indentLines(value.text, value.selection.start, value.selection.end)
        else EditorFormatting.dedentLines(value.text, value.selection.start, value.selection.end)
        if (text == value.text) return
        value = TextFieldValue(text, TextRange(from, to))
        completion = null
        onCodeChange(text)
    }

    fun accept(entry: SandboxApi.Entry) {
        val current = completion ?: return
        var insert = entry.insert
        if (current.afterReceiver) insert = insert.removePrefix("${SandboxApi.RECEIVER}.")
        val caretOffset = when {
            insert.endsWith("(\"\")") -> insert.length - 2
            insert.endsWith("()") -> insert.length - 1
            else -> insert.length
        }
        replace(current.prefixStart, value.selection.start, insert, caretOffset)
    }

    // Folding. A caret inside a block that gets folded is moved in front of its brace, so it cannot type out of sight
    fun fold(region: FoldRegion) {
        folded = folded + region.key
        val s = value.selection
        if (region.covers(s.start) || region.covers(s.end)) value = value.copy(selection = TextRange(maxOf(0, region.hiddenStart - 1)))
        completion = null
    }
    fun unfold(region: FoldRegion) { folded = folded - region.key }
    fun caretLineInFile() = EditorFolding.lineOf(value.text, value.selection.start)
    fun foldAtCaret() {
        val line = caretLineInFile()
        regions.filter { it.coversLine(line) && it.key !in folded }.maxByOrNull { it.headerLine }?.let { fold(it) }
    }
    fun unfoldAtCaret() {
        val line = caretLineInFile()
        (regions.firstOrNull { it.headerLine == line && it.key in folded } ?: regions.filter { it.coversLine(line) && it.key in folded }.maxByOrNull { it.headerLine })?.let { unfold(it) }
    }
    fun foldAll() { regions.forEach { fold(it) } }
    fun unfoldAll() { folded = emptySet() }

    // The current line, counted in what is shown (folded blocks take one line)
    val transformedCaret = folds.originalToTransformed(value.selection.start.coerceIn(0, value.text.length))
    val caretLine = layout?.let { l -> runCatching { l.getLineForOffset(transformedCaret.coerceIn(0, l.layoutInput.text.length)) }.getOrNull() } ?: 0
    val lineHeightPx = with(density) { textStyle.lineHeight.toPx() }
    val gutterLines = remember(value.text, regions, folds, folded) {
        val byHeader = regions.associateBy { it.headerLine }
        val total = value.text.count { it == '\n' } + 1
        (0 until total).filter { it !in folds.hiddenLines }.map { i -> GutterLine(i + 1, byHeader[i], byHeader[i]?.let { it.key in folded } == true) }
    }
    // The text's own size decides how wide the current line's box is and how tall the gutter
    val textWidth = layout?.let { with(density) { it.size.width.toDp() } } ?: 0.dp
    val textHeight = layout?.let { with(density) { it.size.height.toDp() } } ?: 0.dp
    val paneWidth = maxOf(viewportWidth - GutterWidth, textWidth + 36.dp)
    val paneHeight = maxOf(viewportHeight, textHeight + 32.dp)

    Row(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Gutter(
            lines = gutterLines,
            caretLine = caretLine,
            layout = layout,
            lineHeightPx = lineHeightPx,
            height = paneHeight,
            textStyle = textStyle,
            onFold = { fold(it) },
            onUnfold = { unfold(it) },
        )
        Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
            Box(modifier = Modifier.width(paneWidth).height(paneHeight)) {
                CurrentLineBox(layout, caretLine, width = paneWidth, topPadding = 16.dp)
                IndentGuides(layout, textStyle, caretLine = caretLine, startPadding = 12.dp, topPadding = 16.dp)
                // The Popup is a child of this inner Box, whose origin is the text field's, so the caret rect maps straight to an offset
                Box(modifier = Modifier.fillMaxSize().padding(start = 12.dp, top = 16.dp, end = 24.dp, bottom = 16.dp)) {
                    BasicTextField(
                        value = value,
                        onValueChange = { change ->
                            val textChanged = change.text != value.text
                            val caretMoved = change.selection != value.selection
                            value = change
                            if (textChanged) {
                                onCodeChange(change.text)
                                // Stale highlighting is fine here: it only decides whether the caret sits in a string or comment
                                completion = if (change.selection.collapsed) completionAt(change.text, change.selection.start, highlighting, forced = false) else null
                            } else if (completion != null && caretMoved) {
                                completion = null
                            }
                            // Typing or moving into a folded block opens it, so nothing happens out of sight
                            val current = if (textChanged) EditorFolding.regions(change.text) else regions
                            val hit = current.filter { it.key in folded && (it.covers(change.selection.start) || it.covers(change.selection.end)) }
                            if (hit.isNotEmpty()) folded = folded - hit.map { it.key }.toSet()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .onFocusChanged { if (!it.isFocused) completion = null }
                            .onPreviewKeyEvent { event ->
                                val down = event.type == KeyEventType.KeyDown
                                val cmd = event.isMetaPressed || event.isCtrlPressed
                                val open = completion
                                when {
                                    cmd && (event.key == Key.Enter || event.key == Key.NumPadEnter) -> {
                                        if (down) onRun()
                                        true
                                    }
                                    cmd && (event.key == Key.Minus || event.key == Key.NumPadSubtract) -> {
                                        if (down) { if (event.isShiftPressed) foldAll() else foldAtCaret() }
                                        true
                                    }
                                    cmd && (event.key == Key.Equals || event.key == Key.Plus || event.key == Key.NumPadAdd) -> {
                                        if (down) { if (event.isShiftPressed) unfoldAll() else unfoldAtCaret() }
                                        true
                                    }
                                    event.isCtrlPressed && event.key == Key.Spacebar -> {
                                        if (down) completion = completionAt(value.text, value.selection.start, highlighting, forced = true)
                                        true
                                    }
                                    open != null && event.key == Key.DirectionUp -> {
                                        if (down) completion = open.copy(selected = (open.selected - 1 + open.items.size) % open.items.size)
                                        true
                                    }
                                    open != null && event.key == Key.DirectionDown -> {
                                        if (down) completion = open.copy(selected = (open.selected + 1) % open.items.size)
                                        true
                                    }
                                    open != null && (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.Tab) -> {
                                        if (down) accept(open.items[open.selected])
                                        true
                                    }
                                    open != null && event.key == Key.Escape -> {
                                        if (down) completion = null
                                        true
                                    }
                                    cmd && event.isShiftPressed && event.key == Key.F -> {
                                        if (down) formatAll()
                                        true
                                    }
                                    // Enter keeps the line's indentation, opens a block after a brace
                                    (event.key == Key.Enter || event.key == Key.NumPadEnter) && !cmd -> {
                                        if (down) {
                                            val from = value.selection.min
                                            val insert = EditorFormatting.newline(value.text, from)
                                            replace(from, value.selection.max, insert.text, insert.caret)
                                        }
                                        true
                                    }
                                    // Tab indents with spaces instead of moving focus out of the editor; on a selection, the lines move
                                    event.key == Key.Tab -> {
                                        if (down) {
                                            if (event.isShiftPressed || !value.selection.collapsed) shiftSelection(if (event.isShiftPressed) -1 else 1)
                                            else replace(value.selection.start, value.selection.start, INDENT)
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            },
                        textStyle = textStyle,
                        visualTransformation = transformation,
                        onTextLayout = { layout = it },
                        cursorBrush = SolidColor(FontColor),
                    )

                    completion?.let { open ->
                        val caret = value.selection.start
                        val rect = layout?.let { result ->
                            runCatching { result.getCursorRect(folds.originalToTransformed(caret).coerceIn(0, result.layoutInput.text.length)) }.getOrNull()
                        }
                        val anchor = rect?.let { IntOffset(it.left.roundToInt(), it.bottom.roundToInt() + 2) } ?: IntOffset(0, 0)
                        Popup(
                            offset = anchor,
                            onDismissRequest = { completion = null },
                            properties = PopupProperties(focusable = false),
                        ) {
                            CompletionList(open, onPick = { accept(it) })
                        }
                    }
                }
            }
        }
    }
}

/**
 * The indentation made visible: a thin vertical line at every four leading spaces of a line, drawn under the
 * text as the field lays it out (so folded blocks and the horizontal scroll are respected). A blank line
 * carries the guides of the next line that has text, so a block's lines stay joined across its empty lines.
 * The guide of the block the caret is in is lit, over the lines that block spans.
 */
@Composable
private fun IndentGuides(layout: TextLayoutResult?, textStyle: TextStyle, caretLine: Int, startPadding: Dp, topPadding: Dp) {
    val l = layout ?: return
    val measurer = rememberTextMeasurer()
    val indentWidth = remember(textStyle, measurer) { measurer.measure(INDENT, textStyle).size.width.toFloat() }
    val text = l.layoutInput.text.text
    // The indentation level of every laid-out line; a blank line takes the next non-blank line's
    val levels = remember(text, l.lineCount) {
        val raw = IntArray(l.lineCount) { line ->
            val start = l.getLineStart(line)
            val end = l.getLineEnd(line, visibleEnd = true)
            val content = text.substring(start, end)
            if (content.isBlank()) -1 else (content.length - content.trimStart(' ').length) / INDENT.length
        }
        var next = 0
        for (i in raw.indices.reversed()) {
            if (raw[i] < 0) raw[i] = next else next = raw[i]
        }
        raw
    }
    if (levels.none { it > 0 }) return
    // The caret's block: its guide is the innermost one of the caret line (a closing line counts one level in),
    // lit from the first to the last line that is at least that deep around the caret
    val active = run {
        if (caretLine !in levels.indices) return@run null
        val lineText = text.substring(l.getLineStart(caretLine), l.getLineEnd(caretLine, visibleEnd = true)).trimStart()
        val depth = levels[caretLine] + (if (lineText.startsWith("}") || lineText.startsWith(")") || lineText.startsWith("]")) 1 else 0)
        if (depth <= 0) return@run null
        var first = caretLine
        while (first > 0 && levels[first - 1] + (if (first - 1 == caretLine) 0 else 0) >= depth) first--
        var last = caretLine
        while (last + 1 < levels.size && levels[last + 1] >= depth) last++
        Triple(depth - 1, first, last)
    }
    val density = LocalDensity.current
    val left = with(density) { startPadding.toPx() }
    val top = with(density) { topPadding.toPx() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (line in 0 until l.lineCount) {
            val y0 = top + l.getLineTop(line)
            val y1 = top + l.getLineBottom(line)
            for (level in 0 until levels[line]) {
                val lit = active != null && level == active.first && line >= active.second && line <= active.third
                val x = left + level * indentWidth + 0.5f
                drawLine(if (lit) EditorIndentGuideActive else EditorIndentGuide, Offset(x, y0), Offset(x, y1), strokeWidth = 1f)
            }
        }
    }
}

/** The faint box on the line the caret is on, across the whole text pane */
@Composable
private fun CurrentLineBox(layout: TextLayoutResult?, line: Int, width: Dp, topPadding: Dp) {
    val l = layout ?: return
    if (line < 0 || line >= l.lineCount) return
    val density = LocalDensity.current
    val top = l.getLineTop(line)
    val height = with(density) { (l.getLineBottom(line) - top).toDp() }
    val topPx = with(density) { topPadding.toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset(0, (top + topPx).roundToInt()) }
            .width(width)
            .height(height)
            .background(EditorCurrentLine, RectangleShape)
            .border(1.dp, EditorCurrentLineEdge, RectangleShape)
    )
}

/**
 * The gutter: one row per visible line at the exact height the text lays that line out, with its number (bright
 * on the current line, which is tinted like the text's box) and, where a block starts, a marker that folds or
 * opens it.
 */
@Composable
private fun Gutter(
    lines: List<GutterLine>,
    caretLine: Int,
    layout: TextLayoutResult?,
    lineHeightPx: Float,
    height: Dp,
    textStyle: TextStyle,
    onFold: (FoldRegion) -> Unit,
    onUnfold: (FoldRegion) -> Unit,
) {
    val density = LocalDensity.current
    Box(modifier = Modifier.width(GutterWidth).height(height).background(GutterBackground).padding(top = 16.dp)) {
        lines.forEachIndexed { v, line ->
            val laid = layout?.takeIf { v < it.lineCount }
            val top = laid?.getLineTop(v) ?: (v * lineHeightPx)
            val rowHeight = laid?.let { it.getLineBottom(v) - it.getLineTop(v) } ?: lineHeightPx
            val current = v == caretLine
            Row(
                modifier = Modifier
                    .offset { IntOffset(0, top.roundToInt()) }
                    .fillMaxWidth()
                    .height(with(density) { rowHeight.toDp() })
                    .background(if (current) EditorCurrentLine else Color.Transparent, RectangleShape),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = line.number.toString(),
                    style = textStyle.copy(color = if (current) FontColor else LineNumberColor, textAlign = TextAlign.End),
                    modifier = Modifier.width(34.dp),
                )
                Box(modifier = Modifier.width(22.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    line.region?.let { region -> FoldMarker(folded = line.folded) { if (line.folded) onUnfold(region) else onFold(region) } }
                }
            }
        }
    }
}

/** The small arrow beside a line that starts a block: down while it is open, right while it is folded */
@Composable
private fun FoldMarker(folded: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    AppTooltip(if (folded) "Open the block (⌘= / Ctrl+=)" else "Fold the block down to its first line (⌘- / Ctrl+-)") {
        Box(
            modifier = Modifier
                .size(16.dp)
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(interactionSource = interactionSource, indication = null) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (folded) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                contentDescription = if (folded) "folded" else "open",
                tint = if (hovered || folded) FontColor else LineNumberColor,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun CompletionList(open: Completion, onPick: (SandboxApi.Entry) -> Unit) {
    val shape = RectangleShape
    Column(
        modifier = Modifier
            .width(480.dp)
            .background(CompletionBackground, shape)
            .border(1.dp, CompletionBorder, shape)
            .padding(vertical = 4.dp)
    ) {
        open.items.forEachIndexed { index, entry ->
            val selected = index == open.selected
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (selected) CompletionSelected else Color.Transparent)
                    .clickable { onPick(entry) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row {
                    BasicText(
                        text = entry.name,
                        style = TextStyle(color = EditorApiColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(2.dp))
                    BasicText(
                        text = entry.signature.removePrefix(entry.name),
                        style = TextStyle(color = FontColor.copy(alpha = 0.85f), fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicText(
                    text = entry.description,
                    style = TextStyle(color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
