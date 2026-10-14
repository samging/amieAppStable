package org.example.project.components.terminal.window.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
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

/** Paints [highlighting] over whatever text the field holds now; spans that outrun the text (stale analysis) are dropped */
private fun highlightTransformation(highlighting: EditorHighlighting) = VisualTransformation { text ->
    val length = text.length
    val painted = buildAnnotatedString {
        append(text.text)
        highlighting.spans.forEach { span ->
            if (span.start >= 0 && span.end <= length && span.end > span.start) addStyle(span.item, span.start, span.end)
        }
    }
    TransformedText(painted, OffsetMapping.Identity)
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

@Composable
internal fun SandboxEditor(
    initialCode: String,
    highlights: Highlights,
    onCodeChange: (String) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    var value by remember { mutableStateOf(TextFieldValue(initialCode)) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var completion by remember { mutableStateOf<Completion?>(null) }
    val highlighting = remember(highlights) { highlightCode(highlights) }
    val transformation = remember(highlighting) { highlightTransformation(highlighting) }

    fun replace(from: Int, to: Int, insert: String, caretOffset: Int = insert.length) {
        val text = value.text
        val updated = text.substring(0, from) + insert + text.substring(to)
        value = TextFieldValue(updated, TextRange(from + caretOffset))
        completion = null
        onCodeChange(updated)
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

    // The Popup is a child of this inner Box, whose origin is the text field's, so the caret rect maps straight to an offset
    Box(modifier = modifier.padding(start = 12.dp, top = 16.dp, end = 24.dp, bottom = 16.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            BasicTextField(
                value = value,
                onValueChange = { change ->
                    val textChanged = change.text != value.text
                    value = change
                    if (textChanged) {
                        onCodeChange(change.text)
                        // Stale highlighting is fine here: it only decides whether the caret sits in a string or comment
                        completion = if (change.selection.collapsed) completionAt(change.text, change.selection.start, highlighting, forced = false) else null
                    } else if (completion != null && change.selection != value.selection) {
                        completion = null
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { if (!it.isFocused) completion = null }
                    .onPreviewKeyEvent { event ->
                        val down = event.type == KeyEventType.KeyDown
                        val open = completion
                        when {
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
                            // Tab indents with spaces instead of moving focus out of the editor
                            event.key == Key.Tab && !event.isShiftPressed -> {
                                if (down && value.selection.collapsed) replace(value.selection.start, value.selection.start, "    ")
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
                    runCatching { result.getCursorRect(caret.coerceIn(0, result.layoutInput.text.length)) }.getOrNull()
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

@Composable
private fun CompletionList(open: Completion, onPick: (SandboxApi.Entry) -> Unit) {
    val shape = RoundedCornerShape(6.dp)
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
