package org.example.project.components.terminal.window.controller.jetbrainDLBuilder

import kotlinx.serialization.Serializable

/**
 * One head to try on top of the encoder: the hidden layers (empty = a linear classifier), and how to fit it.
 * `architecture(hidden = listOf(32, 16))` in a script makes one; [BuildOptions.candidates] lists what a build tries.
 */
@Serializable
data class Architecture(
    val hidden: List<Int> = listOf(16),
    val epochs: Int = 150,
    val batchSize: Int = 16,
    val learningRate: Double = 0.01,
    /** The activation between the hidden layers: Relu or Tanh */
    val activation: String = "Relu",
) {
    init {
        require(hidden.all { it > 0 }) { "every hidden layer needs at least one unit, got $hidden" }
        require(epochs in 1..10_000) { "epochs must be between 1 and 10000, got $epochs" }
        require(batchSize >= 1) { "batchSize must be at least 1, got $batchSize" }
        require(learningRate > 0.0 && learningRate < 1.0) { "learningRate must be between 0 and 1, got $learningRate" }
        require(activation.equals("Relu", true) || activation.equals("Tanh", true)) { "activation must be Relu or Tanh, got $activation" }
    }

    /** `linear`, `dense[16]`, `dense[64, 16]` */
    val label: String get() = if (hidden.isEmpty()) "linear" else "dense${hidden.joinToString(", ", "[", "]")}"

    /** The head as written in the manifest, with the input and output sizes filled in */
    fun describe(inputDim: Long, labels: Int): String {
        val dims = listOf(inputDim.toInt()) + hidden + labels
        return dims.zipWithNext().mapIndexed { i, (a, b) -> "dense[$a -> $b]" + if (i < hidden.size) " ${activation.lowercase()}" else " softmax" }.joinToString(" -> ")
    }

    companion object {
        /** What a build tries unless told otherwise: from a linear head to two hidden layers */
        val DEFAULT_CANDIDATES: List<Architecture> = listOf(
            Architecture(hidden = emptyList()),
            Architecture(hidden = listOf(16)),
            Architecture(hidden = listOf(32)),
            Architecture(hidden = listOf(64, 16)),
        )
    }
}

/**
 * How modelBuilder goes about a build: whether the preprocessor runs, which heads it tries and how the best one is
 * picked. `buildOptions(...)` in a script makes one; the Models page's New model uses the defaults.
 */
data class BuildOptions(
    /** The heads to train; with one, nothing is compared */
    val candidates: List<Architecture> = Architecture.DEFAULT_CANDIDATES,
    /** `macroF1` (every label counts the same) or `accuracy` (every row counts the same) on the validation set */
    val pickBy: String = "macroF1",
    /** Clean and check the data first; off, rows go to the encoder as they are and only the hard checks run */
    val preprocess: Boolean = true,
    /** The share of the rows held out when there is no related test file */
    val validationFraction: Double = 0.2,
    /** The seed of the split, the weights and the shuffles: the same data and seed give the same model */
    val seed: Int = 42,
    /** Weight the loss so every label counts the same, for imbalanced data (a rare label is otherwise learned last) */
    val balanceClasses: Boolean = false,
) {
    init {
        require(candidates.isNotEmpty()) { "at least one architecture is needed" }
        require(pickBy == "macroF1" || pickBy == "accuracy") { "pickBy must be macroF1 or accuracy, got $pickBy" }
        require(validationFraction > 0.0 && validationFraction < 0.5) { "validationFraction must be between 0 and 0.5, got $validationFraction" }
    }
}

/** What one candidate came to on the validation set; the list of them is kept with the model's metrics */
@Serializable
data class CandidateResult(
    val architecture: String,
    val hidden: List<Int>,
    val epochs: Int,
    val parameters: Long,
    val accuracy: Double,
    val macroF1: Double,
    val trainAccuracy: Double,
    val finalLoss: Double,
    val chosen: Boolean = false,
    /** F1 per label on the validation rows, so the heads can be compared label by label */
    val perLabelF1: Map<String, Double> = emptyMap(),
    /** What the diagnostics say about this head: memorising, barely above the majority label, weak labels */
    val insights: List<String> = emptyList(),
)

/** The best candidate: by the chosen score, then the other score, then the fewest parameters */
fun pickBest(results: List<CandidateResult>, pickBy: String): CandidateResult =
    results.sortedWith(
        compareByDescending<CandidateResult> { if (pickBy == "accuracy") it.accuracy else it.macroF1 }
            .thenByDescending { if (pickBy == "accuracy") it.macroF1 else it.accuracy }
            .thenBy { it.parameters }
    ).first()
