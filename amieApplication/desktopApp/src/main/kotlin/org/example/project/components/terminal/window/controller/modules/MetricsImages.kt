package org.example.project.components.terminal.window.controller.modules

import androidx.compose.ui.graphics.toArgb
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import org.example.project.ui.theme.ChartAccent
import org.example.project.ui.theme.ChartGrid
import org.example.project.ui.theme.ChartSecond
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.PanelBorder
import org.example.project.ui.theme.PanelSurface
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO

/** One rendered plot: the file and the size it is shown at (the image is drawn at twice that for sharpness) */
data class MetricsPlot(val title: String, val file: File, val width: Int, val height: Int)

/**
 * Draws the metrics of a model as PNG files in `AMP/modules/<name>/plots/`: the F1 of every label as bars with
 * precision, recall and support beside them, the confusion matrix as a one-hue heatmap, and accuracy with macro
 * F1 over every run as two lines. They are drawn again only when `metrics.json` is newer than they are, so the
 * Models page shows them at once. Same colours as the page: marks in the accent, text in the text tokens.
 */
object MetricsImages {
    private const val SCALE = 2
    private val surface = awt(PanelSurface)
    private val border = awt(PanelBorder)
    private val grid = awt(ChartGrid)
    private val text = awt(FontColor)
    private val muted = awt(MutedFontColor)
    private val accent = awt(ChartAccent)
    private val second = awt(ChartSecond)

    private fun awt(c: androidx.compose.ui.graphics.Color) = Color(c.toArgb(), true)
    private fun f3(v: Double) = String.format(Locale.US, "%.3f", v)

    /** The plots of a model, drawn when missing or stale; empty when there is no run yet */
    fun ensure(dir: File, runs: List<MetricsRun>): List<MetricsPlot> {
        val run = runs.lastOrNull() ?: return emptyList()
        val metricsFile = File(dir, "metrics.json")
        val plotsDir = File(dir, "plots")
        val labels = run.labels.keys.toList()
        val labelsPlot = MetricsPlot("F1 per label", File(plotsDir, "labels.png"), 560, 26 + 22 * labels.size + 20)
        val headLeft = minOf(96, 16 + 7 * labels.maxOf { it.length })
        val confusionPlot = MetricsPlot("Confusion matrix", File(plotsDir, "confusion.png"), 12 + headLeft + 30 * labels.size + 56, 30 + 64 + 30 * labels.size + 12)
        val historyPlot = MetricsPlot("Accuracy and macro F1 over runs", File(plotsDir, "history.png"), 560, 180)
        val summaryPlot = MetricsPlot("Summary", File(plotsDir, "summary.png"), 560, 132)
        val plots = listOfNotNull(summaryPlot, labelsPlot, confusionPlot, historyPlot.takeIf { runs.size > 1 })
        val stale = plots.any { !it.file.isFile || it.file.lastModified() < metricsFile.lastModified() }
        if (stale) {
            runCatching {
                plotsDir.mkdirs()
                draw(summaryPlot) { g -> summary(g, runs, summaryPlot) }
                draw(labelsPlot) { g -> labels(g, run, labelsPlot) }
                draw(confusionPlot) { g -> confusion(g, run, confusionPlot) }
                if (runs.size > 1) draw(historyPlot) { g -> history(g, runs, historyPlot) }
            }.onFailure { logInternal("metrics plots: couldn't draw for ${dir.name}: ${it.message}", LogLevel.ERROR); return emptyList() }
        }
        return plots.filter { it.file.isFile }
    }

    private fun draw(plot: MetricsPlot, body: (Graphics2D) -> Unit) {
        val image = BufferedImage(plot.width * SCALE, plot.height * SCALE, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.scale(SCALE.toDouble(), SCALE.toDouble())
        g.color = surface
        g.fillRoundRect(0, 0, plot.width, plot.height, 8, 8)
        g.color = border
        g.drawRoundRect(0, 0, plot.width - 1, plot.height - 1, 8, 8)
        body(g)
        g.dispose()
        ImageIO.write(image, "png", plot.file)
    }

    private val mono = Font(Font.MONOSPACED, Font.PLAIN, 11)
    private val sans = Font(Font.SANS_SERIF, Font.PLAIN, 11)

    private fun Graphics2D.label(s: String, x: Float, y: Float, colour: Color = muted, font: Font = mono) {
        this.font = font
        color = colour
        drawString(s, x, y)
    }

    private fun Graphics2D.width(s: String, font: Font = mono): Int = getFontMetrics(font).stringWidth(s)

    private fun Graphics2D.roundBar(x: Float, y: Float, w: Float, h: Float, colour: Color) {
        color = colour
        fill(RoundRectangle2D.Float(x, y, maxOf(w, 2f), h, 4f, 4f))
    }

    /** Two gauges, accuracy and macro F1, and what the run says in words: the data, the best and weakest label, the trend */
    private fun summary(g: Graphics2D, runs: List<MetricsRun>, plot: MetricsPlot) {
        val run = runs.last()
        fun gauge(cx: Float, cy: Float, value: Double, name: String, colour: Color) {
            val r = 36f
            g.stroke = BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.color = grid
            g.draw(java.awt.geom.Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, 225f, -270f, java.awt.geom.Arc2D.OPEN))
            g.color = colour
            g.draw(java.awt.geom.Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, 225f, (-270f * value.coerceIn(0.0, 1.0)).toFloat(), java.awt.geom.Arc2D.OPEN))
            val big = Font(Font.SANS_SERIF, Font.BOLD, 15)
            val v = String.format(Locale.US, "%.0f%%", value * 100)
            g.label(v, cx - g.width(v, big) / 2f, cy + 6f, text, big)
            g.label(name, cx - g.width(name, sans) / 2f, cy + r + 14f, muted, sans)
        }
        gauge(58f, 58f, run.accuracy, "accuracy", accent)
        gauge(150f, 58f, run.macroF1, "macro F1", second)

        val best = run.labels.maxByOrNull { it.value.f1 }
        val worst = run.labels.minByOrNull { it.value.f1 }
        val lines = buildList {
            add(if (run.trainSize > 0) "${run.validationSize} of ${run.samples} rows held out from ${run.validation}" else "${run.validationSize} rows of ${run.validation}")
            add("${run.labels.size} labels: ${run.labels.keys.joinToString().let { if (it.length > 48) it.take(47) + "…" else it }}")
            if (best != null && worst != null && best.key != worst.key) {
                add("best     ${best.key}  F1 ${f3(best.value.f1)}")
                add("weakest  ${worst.key}  F1 ${f3(worst.value.f1)}")
            }
            if (runs.size > 1) {
                val first = runs.first()
                val delta = run.accuracy - first.accuracy
                add("${runs.size} runs · accuracy ${f3(first.accuracy)} → ${f3(run.accuracy)} (${if (delta >= 0) "+" else ""}${f3(delta)})")
            }
            if (run.warnings.isNotEmpty()) add("${run.warnings.size} warning${if (run.warnings.size == 1) "" else "s"}: ${run.warnings.first().let { if (it.length > 44) it.take(43) + "…" else it }}")
        }
        val maxWidth = plot.width - 212 - 12
        fun fit(s: String): String {
            if (g.width(s) <= maxWidth) return s
            var t = s
            while (t.isNotEmpty() && g.width("$t…") > maxWidth) t = t.dropLast(1)
            return "$t…"
        }
        lines.forEachIndexed { i, line -> g.label(fit(line), 212f, 30f + i * 17f, if (i == 0) text else muted) }
    }

    /** Horizontal bars of F1, the value and precision / recall / support beside each */
    private fun labels(g: Graphics2D, run: MetricsRun, plot: MetricsPlot) {
        val entries = run.labels.entries.toList()
        g.label("F1 per label", 12f, 16f, text, sans)
        val top = 26f
        val rowH = 22f
        val labelW = minOf(plot.width * 0.28f, entries.maxOf { g.width(it.key) } + 20f)
        val noteW = 150f
        val valueW = 44f
        val left = 12f + labelW
        val plotW = plot.width - left - valueW - noteW - 12f
        for (q in 0..4) {
            val x = left + plotW * q / 4f
            g.color = grid
            g.stroke = BasicStroke(1f)
            g.drawLine(x.toInt(), top.toInt(), x.toInt(), (top + rowH * entries.size).toInt())
        }
        for (q in 0..4) {
            val t = String.format(Locale.US, "%.2f", q / 4.0)
            g.label(t, left + plotW * q / 4f - g.width(t) / 2f, top + rowH * entries.size + 12f)
        }
        entries.forEachIndexed { i, (label, m) ->
            val cy = top + i * rowH + rowH / 2
            g.label(if (g.width(label) > labelW - 20) label.take(maxOf(3, (labelW / 7).toInt() - 1)) + "…" else label, 12f, cy + 4f)
            g.roundBar(left, cy - 4f, (plotW * m.f1.coerceIn(0.0, 1.0)).toFloat(), 8f, accent)
            g.label(f3(m.f1), left + plotW + 8f, cy + 4f, text)
            g.label("p ${f3(m.precision)} r ${f3(m.recall)} n ${m.support}", left + plotW + valueW + 8f, cy + 4f)
        }
    }

    /** Truth in rows, prediction in columns, one hue by share of the row, the count in every cell */
    private fun confusion(g: Graphics2D, run: MetricsRun, plot: MetricsPlot) {
        val labels = run.labels.keys.toList()
        val n = labels.size
        g.label("Confusion · truth ↓  predicted →", 12f, 16f, text, sans)
        val headLeft = minOf(96f, 16f + 7f * labels.maxOf { it.length })
        val headTop = 64f
        val cell = 30f
        val ox = 12f
        val oy = 30f
        val rowMax = run.confusion.map { it.maxOrNull() ?: 0 }
        fun short(label: String) = if (label.length > 12) label.take(11) + "…" else label
        labels.forEachIndexed { i, label ->
            // Column heads slanted so long names don't run into each other; row heads straight
            val cx = ox + headLeft + i * cell + cell / 2 + 4f
            val cy = oy + headTop - 6f
            val t = g.transform
            g.rotate(Math.toRadians(-55.0), cx.toDouble(), cy.toDouble())
            g.label(short(label), cx, cy)
            g.transform = t
            g.label(short(label), ox, oy + headTop + i * cell + cell / 2 + 4f)
        }
        for (r in 0 until n) for (c in 0 until n) {
            val count = run.confusion.getOrNull(r)?.getOrNull(c) ?: 0
            val share = if (rowMax[r] == 0) 0f else count.toFloat() / rowMax[r]
            val t = 0.12f + 0.88f * share
            val fill = Color(
                (surface.red + (accent.red - surface.red) * t).toInt(),
                (surface.green + (accent.green - surface.green) * t).toInt(),
                (surface.blue + (accent.blue - surface.blue) * t).toInt(),
            )
            val x = ox + headLeft + c * cell + 1f
            val y = oy + headTop + r * cell + 1f
            g.color = fill
            g.fill(RoundRectangle2D.Float(x, y, cell - 2f, cell - 2f, 3f, 3f))
            if (r == c) {
                g.color = Color(text.red, text.green, text.blue, 90)
                g.stroke = BasicStroke(1f)
                g.draw(RoundRectangle2D.Float(x, y, cell - 2f, cell - 2f, 3f, 3f))
            }
            val s = count.toString()
            g.label(s, x + (cell - 2f) / 2 - g.width(s) / 2f, y + cell / 2 + 3f, text)
        }
    }

    /** Two lines over the runs with markers and a legend, y from 0 to 1 */
    private fun history(g: Graphics2D, runs: List<MetricsRun>, plot: MetricsPlot) {
        g.label("Accuracy and macro F1 over runs", 12f, 16f, text, sans)
        // legend
        g.color = accent; g.fill(Ellipse2D.Float(plot.width - 190f, 9f, 8f, 8f)); g.label("accuracy", plot.width - 178f, 17f, muted, sans)
        g.color = second; g.fill(Ellipse2D.Float(plot.width - 110f, 9f, 8f, 8f)); g.label("macro F1", plot.width - 98f, 17f, muted, sans)
        val left = 44f
        val right = plot.width - 16f
        val top = 32f
        val bottom = plot.height - 24f
        fun x(i: Int) = left + (right - left) * i / maxOf(1, runs.size - 1)
        fun y(v: Double) = bottom - (bottom - top) * v.coerceIn(0.0, 1.0).toFloat()
        g.stroke = BasicStroke(1f)
        for (q in 0..4) {
            val yy = y(q / 4.0)
            g.color = grid
            g.drawLine(left.toInt(), yy.toInt(), right.toInt(), yy.toInt())
            g.label(String.format(Locale.US, "%.2f", q / 4.0), 8f, yy + 4f)
        }
        runs.indices.forEach { i -> g.label("${i + 1}", x(i) - 3f, bottom + 14f) }
        fun series(colour: Color, value: (MetricsRun) -> Double) {
            val path = Path2D.Float()
            runs.forEachIndexed { i, r -> if (i == 0) path.moveTo(x(i), y(value(r))) else path.lineTo(x(i), y(value(r))) }
            g.color = colour
            g.stroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.draw(path)
            runs.forEachIndexed { i, r ->
                g.color = surface; g.fill(Ellipse2D.Float(x(i) - 5f, y(value(r)) - 5f, 10f, 10f))
                g.color = colour; g.fill(Ellipse2D.Float(x(i) - 3.5f, y(value(r)) - 3.5f, 7f, 7f))
            }
        }
        series(accent) { it.accuracy }
        series(second) { it.macroF1 }
        // last values, direct-labelled
        runs.lastOrNull()?.let { last ->
            g.label(f3(last.accuracy), x(runs.lastIndex) - 34f, y(last.accuracy) - 8f, text)
            g.label(f3(last.macroF1), x(runs.lastIndex) - 34f, y(last.macroF1) + 16f, text)
        }
    }
}
