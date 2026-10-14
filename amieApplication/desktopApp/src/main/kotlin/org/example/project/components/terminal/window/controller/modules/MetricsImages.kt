package org.example.project.components.terminal.window.controller.modules

import androidx.compose.ui.graphics.toArgb
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import org.example.project.ui.theme.ChartAccent
import org.example.project.ui.theme.ChartGrid
import org.example.project.ui.theme.ChartHeatHigh
import org.example.project.ui.theme.ChartHeatLow
import org.example.project.ui.theme.ChartInkDark
import org.example.project.ui.theme.ChartMarkDark
import org.example.project.ui.theme.ChartMarkLight
import org.example.project.ui.theme.ChartMarkMid
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.PanelBorder
import org.example.project.ui.theme.PanelSurface
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/** One rendered plot: the file and the size it is shown at (the image is drawn at twice that for sharpness) */
data class MetricsPlot(val title: String, val file: File, val width: Int, val height: Int)

/**
 * Draws the charts of a model as PNG files in `AMP/modules/<name>/plots/`, each laid out like a report figure:
 * a title that says what it shows, a subtitle that says which run, which data and which labels, named axes, and
 * a caption that says how to read it. Per run: the scores of every label as a dot plot and the confusion matrix
 * as a heatmap. Across runs: accuracy, macro F1 and every label's F1 as a heatmap, the latest run at the right.
 * From the sessions log: the spread of every label's score and the latency of the answers. Files are drawn again
 * only when `metrics.json` (or `sessions.log`) is newer than they are. One muted slate hue throughout, light for
 * large and dark for small; the words in the text tokens.
 */
object MetricsImages {
    private const val SCALE = 2
    private const val WIDE = 600
    private val surface = awt(PanelSurface)
    private val border = awt(PanelBorder)
    private val grid = awt(ChartGrid)
    private val text = awt(FontColor)
    private val muted = awt(MutedFontColor)
    private val accent = awt(ChartAccent)
    private val markDark = awt(ChartMarkDark)
    private val markMid = awt(ChartMarkMid)
    private val markLight = awt(ChartMarkLight)
    private val heatLow = awt(ChartHeatLow)
    private val heatHigh = awt(ChartHeatHigh)
    private val inkDark = awt(ChartInkDark)

    private fun awt(c: androidx.compose.ui.graphics.Color) = Color(c.toArgb(), true)
    private fun f3(v: Double) = String.format(Locale.US, "%.3f", v)
    private fun f2(v: Double) = String.format(Locale.US, "%.2f", v)
    private fun pct(v: Double) = "${(v * 100).roundToInt()}%"

    private val legacy = listOf("summary.png", "labels.png", "scorecard.png", "history.png", "label-runs.png")

    /** The plots that don't depend on which run is chosen: across the runs and from the sessions, plus the latest run's */
    fun ensure(dir: File, runs: List<MetricsRun>): List<MetricsPlot> {
        val plotsDir = File(dir, "plots")
        legacy.forEach { File(plotsDir, it).delete() }
        val plots = ArrayList<MetricsPlot>()
        val metricsFile = File(dir, "metrics.json")
        val model = dir.name

        if (runs.size > 1) {
            val labels = runs.last().labels.keys.toList()
            val runsPlot = MetricsPlot("Scores by run", File(plotsDir, "runs.png"), WIDE, 76 + 16 + 22 * (labels.size + 2) + 24 + 40)
            if (stale(runsPlot, metricsFile)) runCatching {
                plotsDir.mkdirs()
                draw(runsPlot) { g -> runsHeatmap(g, model, runs, labels, runsPlot) }
            }.onFailure { logInternal("metrics plots: couldn't draw runs for $model: ${it.message}", LogLevel.ERROR) }
            plots += runsPlot
        }
        if (runs.isNotEmpty()) plots += runPlots(dir, runs, runs.lastIndex)

        val logFile = SessionRuns.logFile(dir)
        if (logFile.isFile) {
            val labelCount = (runs.lastOrNull()?.labels?.size ?: readModule(model)?.second?.labels?.size ?: 3).coerceAtLeast(1)
            val confidencePlot = MetricsPlot("How sure the model is", File(plotsDir, "confidence.png"), WIDE, 76 + 16 + 24 * labelCount + 26 + 40)
            val latencyPlot = MetricsPlot("How long an answer takes", File(plotsDir, "latency.png"), WIDE, 222)
            val session = listOf(confidencePlot, latencyPlot)
            if (session.any { stale(it, logFile) }) runCatching {
                val logged = SessionRuns.read(dir)
                if (logged.isNotEmpty()) {
                    plotsDir.mkdirs()
                    val order = runs.lastOrNull()?.labels?.keys?.toList() ?: readModule(model)?.second?.labels ?: emptyList()
                    draw(confidencePlot) { g -> confidence(g, model, logged, order, confidencePlot) }
                    draw(latencyPlot) { g -> latency(g, model, logged, latencyPlot) }
                }
            }.onFailure { logInternal("session plots: couldn't draw for $model: ${it.message}", LogLevel.ERROR) }
            plots += session
        }
        return plots.filter { it.file.isFile }
    }

    /** The two plots of run [index] (0-based), drawn when missing or older than the metrics */
    fun runPlots(dir: File, runs: List<MetricsRun>, index: Int): List<MetricsPlot> {
        val run = runs.getOrNull(index) ?: return emptyList()
        val plotsDir = File(dir, "plots")
        val metricsFile = File(dir, "metrics.json")
        val labels = run.labels.keys.toList()
        val n = labels.size
        val number = index + 1
        val scoresPlot = MetricsPlot("Scores per label", File(plotsDir, "run-$number-scores.png"), WIDE, 76 + 16 + 26 * (n + 1) + 22 + 42)
        val headLeft = minOf(120, 16 + 7 * labels.maxOf { it.length })
        val cell = 46
        val confusionPlot = MetricsPlot("Confusion matrix", File(plotsDir, "run-$number-confusion.png"), maxOf(440, 12 + 22 + headLeft + cell * n + 60 + 12), 76 + 16 + 64 + cell * n + 24 + 28 + 42)
        val plots = listOf(scoresPlot, confusionPlot)
        if (plots.any { stale(it, metricsFile) }) runCatching {
            plotsDir.mkdirs()
            draw(scoresPlot) { g -> scores(g, dir.name, runs, index, scoresPlot) }
            draw(confusionPlot) { g -> confusion(g, dir.name, runs, index, confusionPlot, headLeft.toFloat(), cell.toFloat()) }
        }.onFailure { logInternal("metrics plots: couldn't draw run $number of ${dir.name}: ${it.message}", LogLevel.ERROR) }
        return plots.filter { it.file.isFile }
    }

    private fun stale(plot: MetricsPlot, source: File) = !plot.file.isFile || plot.file.lastModified() < source.lastModified()

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

    private val titleFont = Font(Font.SANS_SERIF, Font.BOLD, 13)
    private val sans = Font(Font.SANS_SERIF, Font.PLAIN, 11)
    private val mono = Font(Font.MONOSPACED, Font.PLAIN, 11)
    private val monoSmall = Font(Font.MONOSPACED, Font.PLAIN, 9)

    private fun Graphics2D.label(s: String, x: Float, y: Float, colour: Color = muted, font: Font = mono) {
        this.font = font
        color = colour
        drawString(s, x, y)
    }

    private fun Graphics2D.width(s: String, font: Font = mono): Int = getFontMetrics(font).stringWidth(s)

    private fun Graphics2D.centred(s: String, cx: Float, y: Float, colour: Color, font: Font = mono) = label(s, cx - width(s, font) / 2f, y, colour, font)

    private fun Graphics2D.fit(s: String, maxWidth: Float, font: Font = sans): String {
        if (width(s, font) <= maxWidth) return s
        var t = s
        while (t.isNotEmpty() && width("$t…", font) > maxWidth) t = t.dropLast(1)
        return "$t…"
    }

    private fun short(label: String, max: Int = 12) = if (label.length > max) label.take(max - 1) + "…" else label

    /** Breaks a sentence at its " · " and spaces into at most two lines that fit [maxWidth] */
    private fun Graphics2D.wrap(s: String, maxWidth: Float, font: Font): List<String> {
        if (width(s, font) <= maxWidth) return listOf(s)
        val words = s.split(" ")
        val first = StringBuilder()
        var i = 0
        while (i < words.size && width((if (first.isEmpty()) "" else "$first ") + words[i], font) <= maxWidth) {
            if (first.isNotEmpty()) first.append(' ')
            first.append(words[i])
            i++
        }
        val rest = words.drop(i).joinToString(" ").removePrefix("· ").trim()
        return listOf(first.toString().removeSuffix(" ·").trim(), fit(rest, maxWidth, font))
    }

    /** Title and subtitle at the top (the subtitle on up to two lines), the caption on up to two lines at the bottom; returns the y where the figure starts */
    private fun Graphics2D.frame(plot: MetricsPlot, title: String, subtitle: String, caption: String?): Float {
        label(fit(title, plot.width - 24f, titleFont), 12f, 20f, text, titleFont)
        val sub = wrap(subtitle, plot.width - 24f, sans)
        sub.forEachIndexed { i, line -> label(line, 12f, 36f + i * 14f, muted, sans) }
        caption?.let {
            val lines = wrap(it, plot.width - 24f, monoSmall)
            lines.forEachIndexed { i, line -> label(line, 12f, plot.height - 9f - (lines.size - 1 - i) * 12f, muted, monoSmall) }
        }
        return 50f + (sub.size - 1) * 14f
    }

    /** The one sequential ramp: dark for nothing, light for everything */
    private fun heat(t: Double): Color {
        val k = t.coerceIn(0.0, 1.0).toFloat()
        return Color(
            (heatLow.red + (heatHigh.red - heatLow.red) * k).toInt(),
            (heatLow.green + (heatHigh.green - heatLow.green) * k).toInt(),
            (heatLow.blue + (heatHigh.blue - heatLow.blue) * k).toInt(),
        )
    }

    private fun inkOn(t: Double): Color = if (t > 0.5) inkDark else text

    /** A heat cell with its text, inset so a 2px surface gap separates neighbours */
    private fun Graphics2D.cell(x: Float, y: Float, w: Float, h: Float, t: Double, big: String?, small: String? = null) {
        color = heat(t)
        fill(RoundRectangle2D.Float(x + 1f, y + 1f, w - 2f, h - 2f, 3f, 3f))
        val ink = inkOn(t)
        when {
            big != null && small != null -> {
                centred(big, x + w / 2, y + h / 2 - 1f, ink)
                centred(small, x + w / 2, y + h / 2 + 10f, if (t > 0.5) inkDark else muted, monoSmall)
            }
            big != null -> centred(big, x + w / 2, y + h / 2 + 4f, ink)
            small != null -> centred(small, x + w / 2, y + h / 2 + 3f, ink, monoSmall)
        }
    }

    /** A gradient strip with its ends named, so the ramp reads as a scale */
    private fun Graphics2D.scaleBar(x: Float, y: Float, w: Float, low: String, high: String) {
        paint = GradientPaint(x, y, heat(0.0), x + w, y, heat(1.0))
        fill(RoundRectangle2D.Float(x, y, w, 7f, 3f, 3f))
        paint = null
        label(low, x, y + 17f, muted, monoSmall)
        label(high, x + w - width(high, monoSmall), y + 17f, muted, monoSmall)
    }

    private fun Graphics2D.mark(kind: Int, cx: Float, cy: Float, colour: Color, r: Float = 4.5f) {
        // A ring of the surface first, so marks that land on each other stay apart
        color = surface
        fill(Ellipse2D.Float(cx - r - 2f, cy - r - 2f, 2 * r + 4f, 2 * r + 4f))
        color = colour
        when (kind) {
            0 -> fill(Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r))
            1 -> fill(Path2D.Float().apply { moveTo(cx, cy - r - 1f); lineTo(cx + r + 1f, cy + r); lineTo(cx - r - 1f, cy + r); closePath() })
            else -> fill(Rectangle2D.Float(cx - r, cy - r, 2 * r, 2 * r))
        }
    }

    private fun runWords(run: MetricsRun, index: Int, total: Int) =
        "run ${index + 1} of $total · ${run.validationSize} rows of ${run.validation} · ${run.labels.size} labels: ${run.labels.keys.joinToString()}"

    /** Every label as a row; its precision, recall and F1 as three marks on one 0–1 axis, the numbers beside; the macro row last */
    private fun scores(g: Graphics2D, model: String, runs: List<MetricsRun>, index: Int, plot: MetricsPlot) {
        val run = runs[index]
        val entries = run.labels.entries.toList()
        val top = g.frame(
            plot,
            "How $model scores on each label · accuracy ${pct(run.accuracy)}, macro F1 ${f3(run.macroF1)}",
            runWords(run, index, runs.size),
            "precision: of what it called the label, how much really was · recall: of the label's rows, how much it found · F1: their harmonic mean",
        )
        // Legend
        val legend = listOf(Triple("precision", 0, markDark), Triple("recall", 1, markMid), Triple("F1", 2, markLight))
        var lx = 12f
        legend.forEach { (name, kind, colour) ->
            g.mark(kind, lx + 4f, top + 6f, colour, 3.5f)
            lx += 14f
            g.label(name, lx, top + 10f, muted, sans)
            lx += g.width(name, sans) + 16f
        }
        val rowTop = top + 16f
        val rowH = 26f
        val numbersW = 168f
        val labelW = (plot.width * 0.26f).coerceAtMost(entries.maxOf { g.width(it.key) } + 56f)
        val left = 12f + labelW
        val right = plot.width - 12f - numbersW
        fun x(v: Double) = left + (right - left) * v.coerceIn(0.0, 1.0).toFloat()
        val rows = entries.map { (label, m) -> listOf(label, m.precision, m.recall, m.f1, m.support) } +
            listOf(listOf("macro", entries.map { it.value.precision }.average(), entries.map { it.value.recall }.average(), run.macroF1, run.validationSize))
        val bottom = rowTop + rowH * rows.size
        g.stroke = BasicStroke(1f)
        for (q in 0..4) {
            val xx = x(q / 4.0)
            g.color = grid
            g.drawLine(xx.toInt(), rowTop.toInt(), xx.toInt(), bottom.toInt())
            g.centred(f2(q / 4.0), xx, bottom + 12f, muted, monoSmall)
        }
        g.label("p", right + 14f, rowTop - 4f, muted, monoSmall)
        g.label("r", right + 62f, rowTop - 4f, muted, monoSmall)
        g.label("F1", right + 110f, rowTop - 4f, muted, monoSmall)
        rows.forEachIndexed { r, row ->
            val label = row[0] as String
            val p = row[1] as Double
            val rc = row[2] as Double
            val f1 = row[3] as Double
            val n = row[4] as Int
            val cy = rowTop + r * rowH + rowH / 2
            val macro = r == rows.lastIndex
            if (macro) {
                g.color = grid
                g.drawLine(12, (cy - rowH / 2).toInt(), plot.width - 12, (cy - rowH / 2).toInt())
            }
            g.label(g.fit(label, labelW - 52f, mono), 12f, cy + 4f, if (macro) muted else text, mono)
            g.label("n $n", 12f + labelW - 48f, cy + 4f, muted, monoSmall)
            // The span from the lowest to the highest of the three, then the marks
            val lo = minOf(p, rc, f1)
            val hi = maxOf(p, rc, f1)
            g.color = grid
            g.stroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.drawLine(x(lo).toInt(), cy.toInt(), x(hi).toInt(), cy.toInt())
            g.mark(0, x(p), cy, markDark)
            g.mark(1, x(rc), cy, markMid)
            g.mark(2, x(f1), cy, markLight)
            g.label(f3(p), right + 8f, cy + 4f, muted)
            g.label(f3(rc), right + 56f, cy + 4f, muted)
            g.label(f3(f1), right + 104f, cy + 4f, text)
        }
    }

    /** Actual label in rows, predicted in columns; each cell the share of its row, with the count; totals at the edges, the scale under */
    private fun confusion(g: Graphics2D, model: String, runs: List<MetricsRun>, index: Int, plot: MetricsPlot, headLeft: Float, cell: Float) {
        val run = runs[index]
        val labels = run.labels.keys.toList()
        val n = labels.size
        val rowTotals = run.confusion.map { it.sum() }
        val colTotals = (0 until n).map { c -> run.confusion.sumOf { it.getOrNull(c) ?: 0 } }
        // The biggest confusion, for the caption
        var worst: Triple<Int, Int, Int>? = null
        for (r in 0 until n) for (c in 0 until n) if (r != c) {
            val count = run.confusion.getOrNull(r)?.getOrNull(c) ?: 0
            if (count > (worst?.third ?: 0)) worst = Triple(r, c, count)
        }
        val caption = worst?.let { (r, c, count) -> "biggest confusion: ${labels[r]} answered as ${labels[c]}, $count of ${rowTotals[r]} · a bright cell off the diagonal is what to fix" }
            ?: "every row answered with its own label · nothing off the diagonal"
        val top = g.frame(plot, "What $model answered for each actual label", runWords(run, index, runs.size), caption)
        val ox = 12f + 22f
        val oy = top + 16f
        val headTop = 64f
        g.label("predicted →", ox + headLeft, oy + 10f, muted, sans)
        // "actual ↓" runs down the left edge
        val t0 = g.transform
        g.rotate(Math.toRadians(-90.0), 20.0, (oy + headTop + n * cell / 2).toDouble())
        g.centred("actual ↓", 20f, oy + headTop + n * cell / 2 + 4f, muted, sans)
        g.transform = t0
        labels.forEachIndexed { i, label ->
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
            val share = if (rowTotals[r] == 0) 0.0 else count.toDouble() / rowTotals[r]
            val x = ox + headLeft + c * cell
            val y = oy + headTop + r * cell
            g.cell(x, y, cell, cell, share, count.toString(), pct(share))
            if (r == c) {
                g.color = Color(text.red, text.green, text.blue, 120)
                g.stroke = BasicStroke(1f)
                g.draw(RoundRectangle2D.Float(x + 1f, y + 1f, cell - 2f, cell - 2f, 3f, 3f))
            }
        }
        val totalsX = ox + headLeft + n * cell + 10f
        g.label("rows", totalsX, oy + headTop - 6f, muted, monoSmall)
        rowTotals.forEachIndexed { r, total -> g.label(total.toString(), totalsX, oy + headTop + r * cell + cell / 2 + 4f, muted) }
        val totalsY = oy + headTop + n * cell + 14f
        g.label("answered", ox, totalsY, muted, monoSmall)
        colTotals.forEachIndexed { c, total -> g.centred(total.toString(), ox + headLeft + c * cell + cell / 2, totalsY, muted) }
        g.scaleBar(ox + headLeft, totalsY + 10f, n * cell, "0% of the row", "100%")
    }

    /** Accuracy, macro F1 and every label's F1 in rows, the runs in columns, the latest at the right */
    private fun runsHeatmap(g: Graphics2D, model: String, runs: List<MetricsRun>, labels: List<String>, plot: MetricsPlot) {
        val shown = runs.takeLast(20)
        val offset = runs.size - shown.size
        val sources = runs.groupingBy { it.validation }.eachCount().entries.sortedByDescending { it.value }
        val top = g.frame(
            plot,
            "How $model's scores changed over ${runs.size} runs",
            (if (offset == 0) "every run, the latest at the right" else "the last ${shown.size} of ${runs.size} runs, the latest at the right") +
                " · validated on " + sources.joinToString { "${it.key} (${it.value})" },
            "a row that lightens towards the right is a score that improved · a validation run adds a column, so does a build",
        )
        val rowTop = top + 16f
        val rowH = 22f
        val names = listOf("accuracy", "macro F1") + labels.map { "F1 · $it" }
        val labelW = (plot.width * 0.3f).coerceAtMost(names.maxOf { g.width(it) } + 16f)
        val left = 12f + labelW
        val cellW = (plot.width - 12f - left) / shown.size
        shown.indices.forEach { c -> g.centred("${offset + c + 1}", left + c * cellW + cellW / 2, rowTop - 4f, muted, monoSmall) }
        names.forEachIndexed { r, name ->
            val y = rowTop + r * rowH
            if (r == 2) {
                g.color = grid
                g.stroke = BasicStroke(1f)
                g.drawLine(12, y.toInt(), plot.width - 12, y.toInt())
            }
            g.label(g.fit(name, labelW - 8f, mono), 12f, y + rowH / 2 + 4f, if (r < 2) text else muted, mono)
            shown.forEachIndexed { c, run ->
                val v = when (r) { 0 -> run.accuracy; 1 -> run.macroF1; else -> run.labels[labels[r - 2]]?.f1 ?: 0.0 }
                g.cell(left + c * cellW, y, cellW, rowH, v, if (cellW >= 40f) f2(v) else null, if (cellW in 22f..40f) f2(v).removePrefix("0") else null)
            }
        }
        g.label("run", 12f, rowTop - 4f, muted, monoSmall)
        g.scaleBar(left, rowTop + names.size * rowH + 8f, plot.width - 12f - left, "0", "1")
    }

    /** Labels in rows, score tenths in columns, every cell how many answers gave that label a score in that tenth */
    private fun confidence(g: Graphics2D, model: String, logged: List<LoggedRun>, order: List<String>, plot: MetricsPlot) {
        val seen = logged.flatMap { r -> r.scores.map { it.first } }.distinct()
        val labels = (order.filter { it in seen } + seen.filter { it !in order }).ifEmpty { order }.take(12)
        val buckets = 10
        val counts = Array(labels.size) { IntArray(buckets) }
        logged.forEach { run ->
            run.scores.forEach { (label, score) ->
                val r = labels.indexOf(label)
                if (r >= 0) counts[r][(score * buckets).toInt().coerceIn(0, buckets - 1)]++
            }
        }
        val top3 = logged.any { it.scores.size >= 3 } && labels.size > 3
        val top = g.frame(
            plot,
            "How sure $model is about each label",
            "${logged.size} answers from its sessions${if (top3) ", the best three scores of each" else ""} · each cell: how many answers gave the label a score in that tenth",
            "a label whose answers sit far right is one the model is confident about; a label that never leaves the left is one it hardly picks",
        )
        val rowTop = top + 16f
        val rowH = 24f
        val totalW = 50f
        val labelW = (plot.width * 0.25f).coerceAtMost((labels.maxOfOrNull { g.width(it) } ?: 40) + 16f)
        val left = 12f + labelW
        val cellW = (plot.width - 12f - totalW - left) / buckets
        val max = counts.maxOfOrNull { it.maxOrNull() ?: 0 }?.coerceAtLeast(1) ?: 1
        (0 until buckets).forEach { c -> g.centred("${String.format(Locale.US, ".%d", c)}–${if (c == 9) "1" else String.format(Locale.US, ".%d", c + 1)}", left + c * cellW + cellW / 2, rowTop - 4f, muted, monoSmall) }
        g.label("answers", left + buckets * cellW + 6f, rowTop - 4f, muted, monoSmall)
        labels.forEachIndexed { r, label ->
            val y = rowTop + r * rowH
            g.label(g.fit(label, labelW - 8f, mono), 12f, y + rowH / 2 + 4f, text, mono)
            counts[r].forEachIndexed { c, count -> g.cell(left + c * cellW, y, cellW, rowH, count.toDouble() / max, if (count == 0) null else count.toString()) }
            g.label(counts[r].sum().toString(), left + buckets * cellW + 6f, y + rowH / 2 + 4f, muted)
        }
        val under = rowTop + labels.size * rowH + 8f
        g.label("score the label was given →", left, under + 10f, muted, monoSmall)
        g.scaleBar(plot.width - 12f - 140f, under + 2f, 140f, "0", "$max answers")
    }

    /** How long the answers took, as a histogram with the median and the slowest in the subtitle */
    private fun latency(g: Graphics2D, model: String, logged: List<LoggedRun>, plot: MetricsPlot) {
        val ms = logged.map { it.millis }.sorted()
        val median = ms[ms.size / 2]
        val p95 = ms[((ms.size - 1) * 0.95).toInt()]
        val top = g.frame(
            plot,
            "How long an answer of $model takes",
            "${ms.size} answers · half within $median ms · 95% within $p95 ms · slowest ${ms.last()} ms",
            "the time from the text going in to the scores coming out: the encoder, then the head · the slowest 5% share the last bar",
        )
        val spanMax = maxOf(p95, 1)
        val step = listOf(1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000).first { spanMax / it < 12 }
        val bars = spanMax / step + 1
        val counts = IntArray(bars)
        ms.forEach { counts[(it / step).coerceAtMost(bars - 1)]++ }
        val left = 48f
        val right = plot.width - 16f
        val plotTop = top + 10f
        val bottom = plot.height - 58f
        val max = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
        g.stroke = BasicStroke(1f)
        for (q in 0..2) {
            val yy = bottom - (bottom - plotTop) * q / 2f
            g.color = grid
            g.drawLine(left.toInt(), yy.toInt(), right.toInt(), yy.toInt())
            g.label("${max * q / 2}", 12f, yy + 4f, muted, monoSmall)
        }
        val t0 = g.transform
        g.rotate(Math.toRadians(-90.0), 20.0, ((plotTop + bottom) / 2).toDouble())
        g.centred("answers", 20f, (plotTop + bottom) / 2 - 8f, muted, monoSmall)
        g.transform = t0
        val barW = (right - left) / bars
        counts.forEachIndexed { i, count ->
            val h = (bottom - plotTop) * count / max
            g.color = accent
            g.fill(RoundRectangle2D.Float(left + i * barW + 2f, bottom - h, barW - 4f, maxOf(h, if (count > 0) 2f else 0f), 4f, 4f))
            val from = i * step
            g.centred(if (i == bars - 1) "$from+" else "$from", left + i * barW + barW / 2, bottom + 12f, muted, monoSmall)
            if (count > 0 && bars <= 12) g.centred(count.toString(), left + i * barW + barW / 2, bottom - h - 4f, muted, monoSmall)
        }
        g.centred("milliseconds", (left + right) / 2, bottom + 24f, muted, monoSmall)
    }
}
