package org.example.project.components.terminal.window.controller.modules

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.components.terminal.window.controller.logInternal
import org.example.project.components.terminal.window.controller.sourceFile.DataSource
import java.io.File

/** A validation in progress: which model, how many rows are done */
data class ValidationProgress(val model: String, val done: Int, val total: Int)

/**
 * Validates an exported model on a labeled csv: every row goes through a session of the model, the top label is
 * compared with the row's, and the result is stored as one more run in `AMP/modules/<name>/metrics.json`, the
 * same file and shape `modelBuilder` writes, so the Models page and the console charts show it. Rows whose label
 * the model doesn't know are skipped and counted as a warning.
 */
object ModelValidation {
    private val _current = MutableStateFlow<ValidationProgress?>(null)
    val current: StateFlow<ValidationProgress?> = _current.asStateFlow()

    fun validate(name: String, data: File): MetricsRun {
        val (dir, manifest) = readModule(name) ?: throw IllegalArgumentException("model '$name' not found")
        val source = DataSource.read(data.absolutePath) ?: throw IllegalArgumentException("${data.name} has no text and label columns")
        val labels = manifest.labels
        val samples = source.samples()
        val known = samples.filter { it.label in labels }
        val warnings = mutableListOf<String>()
        if (known.size < samples.size) warnings += "${samples.size - known.size} rows of ${data.name} have labels $name doesn't know (${(samples.map { it.label }.toSet() - labels.toSet()).joinToString()})"
        if (known.isEmpty()) throw IllegalArgumentException("${data.name} has no rows with the labels of $name (${labels.joinToString()})")

        val session = ModelSessions.open(name)
        val truth = IntArray(known.size)
        val prediction = IntArray(known.size)
        try {
            known.forEachIndexed { i, sample ->
                _current.value = ValidationProgress(name, i, known.size)
                truth[i] = labels.indexOf(sample.label)
                val top = session.inference(sample.text).firstOrNull()?.substringBeforeLast(':') ?: ""
                prediction[i] = labels.indexOf(top).coerceAtLeast(0)
            }
        } finally {
            session.close()
            _current.value = null
        }
        val run = computeMetrics(labels, truth, prediction, trainSize = 0, warnings = warnings).copy(validation = data.name)
        appendMetrics(dir, run)
        logInternal("validate: '$name' on ${data.name}: ${known.size} rows, accuracy ${"%.3f".format(run.accuracy)}, macro F1 ${"%.3f".format(run.macroF1)}")
        return run
    }
}
