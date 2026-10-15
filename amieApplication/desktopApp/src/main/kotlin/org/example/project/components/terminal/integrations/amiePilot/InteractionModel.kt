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

interface InteractionModel : AutoCloseable {
    override fun close()
    fun inference(text: String): List<String>
    fun getStatus(): Any

    /** The answers of [inference] parsed: a classifier's `label:score` lines become label and score, best first */
    fun predictions(text: String): List<Prediction> = inference(text).map(Prediction::parse)

    /** The predicted label: the best answer of [inference] without its score (the argmax of a classifier) */
    fun predict(text: String): String {
        val best = inference(text).firstOrNull() ?: throw IllegalStateException("the model answered nothing for \"$text\"")
        return Prediction.parse(best).label
    }

    /** Every label with its score, best first; answers that carry no score are left out */
    fun scores(text: String): Map<String, Float> =
        predictions(text).mapNotNull { p -> p.score?.let { p.label to it } }.toMap()
}
