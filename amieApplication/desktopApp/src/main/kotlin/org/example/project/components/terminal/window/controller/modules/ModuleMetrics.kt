package org.example.project.components.terminal.window.controller.modules

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import java.io.File
import java.time.Instant
import java.util.Locale

@Serializable
data class LabelMetrics(val precision: Double, val recall: Double, val f1: Double, val support: Int)

// One validation pass, recorded every time modelBuilder trains a module
@Serializable
data class MetricsRun(
    val createdAt: String = Instant.now().toString(),
    val samples: Int,
    val trainSize: Int,
    val validationSize: Int,
    val accuracy: Double,
    val macroF1: Double,
    val labels: Map<String, LabelMetrics>,
    // confusion[truth][prediction], indexed like ModuleManifest.labels
    val confusion: List<List<Int>>,
    val warnings: List<String> = emptyList(),
    // Related test file the run was validated on, or "split 20%"
    val validation: String = "split 20%",
)

@Serializable
data class MetricsHistory(val runs: List<MetricsRun> = emptyList())

private val metricsJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

private fun metricsFile(dir: File) = File(dir, "metrics.json")

fun readMetricsHistory(dir: File): MetricsHistory =
    runCatching { metricsJson.decodeFromString<MetricsHistory>(metricsFile(dir).readText()) }.getOrElse { MetricsHistory() }

// Appends so earlier builds stay comparable; the last run is the current model
fun appendMetrics(dir: File, run: MetricsRun) {
    val history = readMetricsHistory(dir)
    metricsFile(dir).writeText(metricsJson.encodeToString(history.copy(runs = history.runs + run)))
}

// Per-label precision/recall/F1 from validation predictions; labels index both truth and prediction
fun computeMetrics(labels: List<String>, truth: IntArray, prediction: IntArray, trainSize: Int, warnings: List<String>): MetricsRun {
    val n = labels.size
    val confusion = Array(n) { IntArray(n) }
    truth.indices.forEach { confusion[truth[it]][prediction[it]]++ }

    val perLabel = labels.mapIndexed { i, label ->
        val tp = confusion[i][i]
        val fp = (0 until n).sumOf { if (it != i) confusion[it][i] else 0 }
        val fn = (0 until n).sumOf { if (it != i) confusion[i][it] else 0 }
        val precision = if (tp + fp == 0) 0.0 else tp.toDouble() / (tp + fp)
        val recall = if (tp + fn == 0) 0.0 else tp.toDouble() / (tp + fn)
        val f1 = if (precision + recall == 0.0) 0.0 else 2 * precision * recall / (precision + recall)
        label to LabelMetrics(precision, recall, f1, tp + fn)
    }.toMap()

    return MetricsRun(
        samples = trainSize + truth.size,
        trainSize = trainSize,
        validationSize = truth.size,
        accuracy = if (truth.isEmpty()) 0.0 else truth.indices.count { truth[it] == prediction[it] }.toDouble() / truth.size,
        macroF1 = perLabel.values.map { it.f1 }.average(),
        labels = perLabel,
        confusion = confusion.map { it.toList() },
        warnings = warnings,
    )
}

/** Last stored validation of a module in AMP/modules/<name>/metrics.json; null fields when it was never built */
class MetricsReport(val modelName: String, val run: MetricsRun?) {

    fun precision(label: String): Double? = run?.labels?.get(label)?.precision
    fun recall(label: String): Double? = run?.labels?.get(label)?.recall
    fun f1(label: String): Double? = run?.labels?.get(label)?.f1
    val accuracy: Double? get() = run?.accuracy
    val macroF1: Double? get() = run?.macroF1

    // Prints the chosen labels (all when empty) as a table in the console
    fun print(labels: List<String> = emptyList()): MetricsReport {
        val run = run ?: run {
            logInternal("validateMetrics: no metrics stored for '$modelName', build it with modelBuilder first", LogLevel.ERROR)
            return this
        }
        val missing = labels.filter { it !in run.labels }
        if (missing.isNotEmpty()) logInternal("validateMetrics: '$modelName' has no labels $missing (known: ${run.labels.keys})", LogLevel.ERROR)
        val shown = if (labels.isEmpty()) run.labels else run.labels.filterKeys { it in labels }
        val width = maxOf(5, shown.keys.maxOfOrNull { it.length } ?: 0)

        val lines = buildList {
            add("metrics '$modelName' @ ${run.createdAt}: ${run.samples} samples (${run.trainSize} train / ${run.validationSize} validation on ${run.validation})")
            add("accuracy ${fmt(run.accuracy)}  macro F1 ${fmt(run.macroF1)}")
            add("${"label".padEnd(width)}  precision  recall  f1     support")
            shown.forEach { (label, m) ->
                add("${label.padEnd(width)}  ${fmt(m.precision).padEnd(9)}  ${fmt(m.recall).padEnd(6)}  ${fmt(m.f1).padEnd(5)}  ${m.support}")
            }
            run.warnings.forEach { add("warning: $it") }
        }
        logInternal(lines.joinToString("\n"))
        return this
    }

    companion object {
        fun load(modelName: String): MetricsReport {
            val dir = readModule(modelName)?.first
            return MetricsReport(modelName, dir?.let { readMetricsHistory(it).runs.lastOrNull() })
        }
    }
}

private fun fmt(value: Double) = String.format(Locale.US, "%.3f", value)
