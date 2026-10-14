package org.example.project.components.terminal.window.controller

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.components.terminal.window.controller.modules.MetricsRun
import org.example.project.ui.theme.ChartAccent
import org.example.project.ui.theme.ChartGrid
import org.example.project.ui.theme.ChartSecond
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.PanelBorder
import org.example.project.ui.theme.PanelSurface
import org.example.project.ui.theme.PanelSurfaceSelected
import java.util.Locale

/*
 * The plots of the metrics block on the Models page, drawn on a Canvas: the F1 of every label as thin bars, the
 * confusion matrix as a one-hue heatmap, accuracy and macro F1 over the runs as two lines. Marks carry the colour,
 * text stays in the text tokens, grid and axes stay recessive, and hovering a mark names it under the plot.
 */

private fun f3(v: Double) = String.format(Locale.US, "%.3f", v)

/** A headline number: the value large, what it is under it */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PanelSurfaceSelected)
            .border(1.dp, PanelBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(value, color = FontColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
        Text(label, color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
    }
}

/** F1 of every label as horizontal bars on a faint grid; hovering a bar lists its precision, recall and support */
@Composable
fun LabelF1Chart(run: MetricsRun, modifier: Modifier = Modifier) {
    val labels = run.labels.entries.toList()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var hovered by remember(run) { mutableStateOf<Int?>(null) }
    val rowHeight = 22.dp
    val labelStyle = TextStyle(color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    val valueStyle = TextStyle(color = FontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight * labels.size + 8.dp)
                .pointerInput(run) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val rowPx = with(density) { rowHeight.toPx() }
                            hovered = when (event.type) {
                                PointerEventType.Exit -> null
                                else -> (event.changes.first().position.y / rowPx).toInt().takeIf { it in labels.indices }
                            }
                        }
                    }
                }
        ) {
            val rowPx = rowHeight.toPx()
            val labelWidth = minOf(size.width * 0.3f, labels.maxOf { measurer.measure(it.key, labelStyle).size.width }.toFloat() + 12.dp.toPx())
            val valueWidth = 44.dp.toPx()
            val plotLeft = labelWidth
            val plotWidth = size.width - labelWidth - valueWidth
            val barHeight = 8.dp.toPx()

            // Grid at every quarter, recessive
            for (q in 0..4) {
                val x = plotLeft + plotWidth * q / 4f
                drawLine(ChartGrid, Offset(x, 0f), Offset(x, rowPx * labels.size), strokeWidth = 1f)
            }
            labels.forEachIndexed { i, (label, m) ->
                val y = i * rowPx
                val centre = y + rowPx / 2
                if (hovered == i) drawRect(PanelSurfaceSelected, Offset(0f, y), Size(size.width, rowPx))
                drawText(measurer, label, Offset(0f, centre - 8.sp.toPx()), labelStyle, overflow = TextOverflow.Ellipsis, maxLines = 1, size = Size(labelWidth - 6.dp.toPx(), rowPx))
                val w = (plotWidth * m.f1.coerceIn(0.0, 1.0)).toFloat()
                drawRoundRect(if (hovered == i) ChartAccent.copy(alpha = 1f) else ChartAccent.copy(alpha = 0.85f), Offset(plotLeft, centre - barHeight / 2), Size(w, barHeight), CornerRadius(4.dp.toPx()))
                drawText(measurer, f3(m.f1), Offset(plotLeft + plotWidth + 8.dp.toPx(), centre - 8.sp.toPx()), valueStyle)
            }
        }
        val h = hovered?.let { labels.getOrNull(it) }
        Text(
            if (h == null) "F1 per label · hover a bar" else "${h.key} · F1 ${f3(h.value.f1)} · precision ${f3(h.value.precision)} · recall ${f3(h.value.recall)} · ${h.value.support} rows",
            color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

/** The confusion matrix as a one-hue heatmap: truth in rows, prediction in columns, the count in every cell */
@Composable
fun ConfusionHeatmap(run: MetricsRun, modifier: Modifier = Modifier) {
    val labels = run.labels.keys.toList()
    val n = labels.size
    val matrix = run.confusion
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var hovered by remember(run) { mutableStateOf<Pair<Int, Int>?>(null) }
    val cell = 26.dp
    val head = 54.dp
    val labelStyle = TextStyle(color = MutedFontColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    val countStyle = TextStyle(color = FontColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    val rowMax = matrix.map { row -> row.maxOrNull() ?: 0 }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .width(head + cell * n)
                .height(head + cell * n)
                .pointerInput(run) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val cellPx = with(density) { cell.toPx() }
                            val headPx = with(density) { head.toPx() }
                            hovered = if (event.type == PointerEventType.Exit) null else {
                                val p = event.changes.first().position
                                val r = ((p.y - headPx) / cellPx).toInt()
                                val c = ((p.x - headPx) / cellPx).toInt()
                                if (p.y >= headPx && p.x >= headPx && r in 0 until n && c in 0 until n) r to c else null
                            }
                        }
                    }
                }
        ) {
            val cellPx = cell.toPx()
            val headPx = head.toPx()
            val gap = 2.dp.toPx()
            labels.forEachIndexed { i, label ->
                val short = if (label.length > 7) label.take(6) + "…" else label
                // Column heads along the top, row heads down the left
                drawText(measurer, short, Offset(headPx + i * cellPx + 2.dp.toPx(), headPx - 14.sp.toPx()), labelStyle, maxLines = 1, size = Size(cellPx, 14.sp.toPx()))
                drawText(measurer, short, Offset(0f, headPx + i * cellPx + cellPx / 2 - 7.sp.toPx()), labelStyle, maxLines = 1, size = Size(headPx - 4.dp.toPx(), cellPx))
            }
            for (r in 0 until n) for (c in 0 until n) {
                val count = matrix.getOrNull(r)?.getOrNull(c) ?: 0
                val share = if (rowMax[r] == 0) 0f else count.toFloat() / rowMax[r]
                val fill = lerp(PanelSurface, ChartAccent, 0.12f + 0.88f * share)
                val x = headPx + c * cellPx
                val y = headPx + r * cellPx
                drawRoundRect(fill, Offset(x + gap / 2, y + gap / 2), Size(cellPx - gap, cellPx - gap), CornerRadius(3.dp.toPx()))
                if (r == c) drawRoundRect(FontColor.copy(alpha = 0.35f), Offset(x + gap / 2, y + gap / 2), Size(cellPx - gap, cellPx - gap), CornerRadius(3.dp.toPx()), style = Stroke(1f))
                if (hovered == r to c) drawRoundRect(FontColor, Offset(x + gap / 2, y + gap / 2), Size(cellPx - gap, cellPx - gap), CornerRadius(3.dp.toPx()), style = Stroke(2f))
                val text = count.toString()
                val tw = measurer.measure(text, countStyle).size.width
                drawText(measurer, text, Offset(x + cellPx / 2 - tw / 2, y + cellPx / 2 - 7.sp.toPx()), countStyle)
            }
        }
        val h = hovered
        Text(
            if (h == null) "truth ↓ · predicted → · hover a cell" else "${labels[h.first]} predicted as ${labels[h.second]}: ${matrix.getOrNull(h.first)?.getOrNull(h.second) ?: 0}",
            color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

/** Accuracy and macro F1 over every run as two lines with markers; hovering a run gives its date and values */
@Composable
fun RunsHistoryChart(runs: List<MetricsRun>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    var hovered by remember(runs.size) { mutableStateOf<Int?>(null) }
    val axisStyle = TextStyle(color = MutedFontColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    val height = 120.dp

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LegendDot(ChartAccent); Text("accuracy", color = MutedFontColor, fontSize = 11.sp)
            Spacer(Modifier.width(8.dp))
            LegendDot(ChartSecond); Text("macro F1", color = MutedFontColor, fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(runs.size) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            hovered = if (event.type == PointerEventType.Exit || runs.size < 2) null else {
                                val left = 36.dp.toPx(); val right = size.width - 12.dp.toPx()
                                val x = event.changes.first().position.x
                                ((x - left) / (right - left) * (runs.size - 1)).toInt().let { i -> (i + if ((x - left) / (right - left) * (runs.size - 1) - i > 0.5f) 1 else 0).coerceIn(0, runs.size - 1) }
                            }
                        }
                    }
                }
        ) {
            val left = 36.dp.toPx()
            val right = size.width - 12.dp.toPx()
            val top = 6.dp.toPx()
            val bottom = size.height - 18.dp.toPx()
            fun x(i: Int) = left + (right - left) * i / maxOf(1, runs.size - 1)
            fun y(v: Double) = bottom - (bottom - top) * v.coerceIn(0.0, 1.0).toFloat()

            for (q in 0..4) {
                val yy = y(q / 4.0)
                drawLine(ChartGrid, Offset(left, yy), Offset(right, yy), strokeWidth = 1f)
                drawText(measurer, String.format(Locale.US, "%.2f", q / 4.0), Offset(0f, yy - 6.sp.toPx()), axisStyle)
            }
            runs.indices.forEach { i -> drawText(measurer, "${i + 1}", Offset(x(i) - 3.sp.toPx(), bottom + 4.dp.toPx()), axisStyle) }
            hovered?.let { i -> drawLine(FontColor.copy(alpha = 0.35f), Offset(x(i), top), Offset(x(i), bottom), strokeWidth = 1f) }

            fun series(colour: Color, value: (MetricsRun) -> Double) {
                val path = Path()
                runs.forEachIndexed { i, r -> if (i == 0) path.moveTo(x(i), y(value(r))) else path.lineTo(x(i), y(value(r))) }
                drawPath(path, colour, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                runs.forEachIndexed { i, r ->
                    drawCircle(PanelSurface, 5.dp.toPx(), Offset(x(i), y(value(r))))
                    drawCircle(colour, if (hovered == i) 4.5.dp.toPx() else 3.5.dp.toPx(), Offset(x(i), y(value(r))))
                }
            }
            series(ChartAccent) { it.accuracy }
            series(ChartSecond) { it.macroF1 }
        }
        val h = hovered?.let { runs.getOrNull(it) }
        Text(
            if (h == null) "run →  · hover a point" else "run ${hovered!! + 1} · ${h.createdAt.take(16).replace('T', ' ')} · accuracy ${f3(h.accuracy)} · macro F1 ${f3(h.macroF1)} · ${h.validation}",
            color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LegendDot(colour: Color) {
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colour))
}
