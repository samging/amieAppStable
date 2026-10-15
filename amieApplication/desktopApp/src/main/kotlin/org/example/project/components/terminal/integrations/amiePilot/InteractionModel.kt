package org.example.project.components.terminal.integrations.amiePilot

/**
 * One answer of a model. A classifier answers `label:score` lines, the score a probability, which parse into
 * the label and its score; any other answer (a package's `run` result, a NER tag, a token with a class index
 * such as `the:3`) is kept whole with no score.
 */
data class Prediction(val label: String, val score: Float?) {
    override fun toString(): String = if (score == null) label else "$label:$score"

    companion object {
        fun parse(answer: String): Prediction {
            val score = answer.substringAfterLast(':', "").toFloatOrNull()?.takeIf { it in 0f..1f }
            return if (score == null) Prediction(answer, null) else Prediction(answer.substringBeforeLast(':'), score)
        }
    }
}

/**
 * What a call of the model is for, so the model logs its answer in that shape: `predict` is answered with the one
 * label, `scores` and `inference` with every label and its score.
 */
enum class Ask { INFERENCE, PREDICT, SCORES }

interface InteractionModel : AutoCloseable {
    override fun close()
    fun inference(text: String): List<String>
    fun getStatus(): Any

    /** [inference] asked for a reason; a model that logs its answers logs only what was asked for */
    fun inference(text: String, ask: Ask): List<String> = inference(text)

    /** The answers of [inference] parsed: a classifier's `label:score` lines become label and score, best first */
    fun predictions(text: String): List<Prediction> = inference(text).map(Prediction::parse)

    /** The predicted label: the best answer of [inference] without its score (the argmax of a classifier) */
    fun predict(text: String): String {
        val best = inference(text, Ask.PREDICT).firstOrNull() ?: throw IllegalStateException("the model answered nothing for \"$text\"")
        return Prediction.parse(best).label
    }

    /** Every label with its score, best first; answers that carry no score are left out */
    fun scores(text: String): Map<String, Float> =
        inference(text, Ask.SCORES).map(Prediction::parse).mapNotNull { p -> p.score?.let { p.label to it } }.toMap()
}
