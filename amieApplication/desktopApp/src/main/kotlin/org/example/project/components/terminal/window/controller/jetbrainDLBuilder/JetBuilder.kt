package org.example.project.components.terminal.window.controller.jetbrainDLBuilder

import java.util.Locale
import java.io.File
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import org.example.project.components.terminal.window.controller.modules.DenseExport
import org.example.project.components.terminal.window.controller.modules.ModuleManifest
import org.example.project.components.terminal.window.controller.modules.denseStackToOnnx
import org.example.project.components.terminal.window.controller.modules.writeModule
import org.example.project.components.terminal.window.controller.modules.MetricsReport
import org.example.project.components.terminal.window.controller.modules.appendMetrics
import org.example.project.components.terminal.window.controller.modules.computeMetrics
import org.example.project.components.terminal.integrations.amiePilot.embedTexts


//sentence -> mean pooling -> dense -> softmax
//down casting the 12 vectors into a 768 vector

fun meanPooling(tensor: Array<Array<FloatArray>>) : FloatArray {
    val contextualSize = tensor.size //batches
    val seqLength = tensor[0].size //sequences
    val hiddenSize = tensor[0][0].size //size of last layer
    val sentenceVector = FloatArray(hiddenSize)
    for (sequenceIdx in 0 until seqLength) {
        for (dim in 0 until hiddenSize) {
            sentenceVector[dim] += tensor[0][sequenceIdx][dim]
        }
    }
    for (dim in 0 until hiddenSize) {
        sentenceVector[dim] = sentenceVector[dim] / seqLength
    }

    return sentenceVector
}

const val DEFAULT_ENCODER = "/Users/samuel/Documents/onnx/bert/bge"
const val EPOCHS = 150
private const val EPOCH_LOG_EVERY = 25

/**
 * Trains a text classifier on labeled samples, validates it on a stratified hold-out and exports it to
 * AMP/modules/<modelName>/ (model.onnx, module.json, metrics.json). Returns the exported model file.
 * With [validation] (a related test file) the model trains on all of [samples] and is validated on it instead of a split.
 * Every phase is reported to [ModelBuildProgress], which the sandbox UI shows while the build runs.
 */
fun buildClassifier(
    samples: List<LabeledText>,
    modelName: String,
    encoder: String = DEFAULT_ENCODER,
    validation: List<LabeledText>? = null,
    validationName: String? = null,
): File? {
    ModelBuildProgress.start(modelName)
    return try {
        buildClassifierPhases(samples, modelName, encoder, validation, validationName)
            .also { if (it == null && ModelBuildProgress.current.value != null) ModelBuildProgress.failed("not built") }
    } catch (e: Throwable) {
        ModelBuildProgress.failed(e.message ?: e.javaClass.simpleName)
        throw e
    }
}

private fun buildClassifierPhases(
    samples: List<LabeledText>,
    modelName: String,
    encoder: String,
    validation: List<LabeledText>?,
    validationName: String?,
): File? {
    ModelBuildProgress.phase(BuildPhase.CHECKING, detail = "${samples.size} samples")
    val check = checkVariability(samples)
    check.warnings.forEach { logInternal("modelBuilder: $it") }
    if (!check.ok) {
        check.errors.forEach { logInternal("modelBuilder: $it", LogLevel.ERROR) }
        logInternal("modelBuilder: '$modelName' not built, the data doesn't have enough variability", LogLevel.ERROR)
        ModelBuildProgress.failed("not enough variability in the data")
        return null
    }

    val labels = samples.map { it.label }.distinct().sorted()
    val warnings = check.warnings.toMutableList()

    // Test rows can only be scored on labels the model trains on
    val testSamples = validation?.let { test ->
        val known = test.filter { it.label in labels }
        if (known.size < test.size) warnings += "${test.size - known.size} rows of $validationName have labels the training data doesn't have"
        known.ifEmpty {
            warnings += "$validationName has no rows with trained labels, falling back to a 20% split"
            null
        }
    }
    warnings.drop(check.warnings.size).forEach { logInternal("modelBuilder: $it") }

    val all = samples + testSamples.orEmpty()
    val labelIndex = all.map { labels.indexOf(it.label) }.toIntArray()
    logInternal("modelBuilder: embedding ${all.size} samples over ${labels.size} labels $labels")

    // sentence -> encoder hidden state -> mean pooling
    ModelBuildProgress.phase(BuildPhase.EMBEDDING, steps = all.size, detail = File(encoder).name)
    val pooled = embedTexts(all.map { it.text }, encoder) { done, total ->
        ModelBuildProgress.step(done, "$done of $total texts through ${File(encoder).name}")
    }.map { meanPooling(it) }
    checkEmbeddingSpread(pooled)?.let {
        logInternal("modelBuilder: $it", LogLevel.ERROR)
        ModelBuildProgress.failed("texts are too similar to learn from")
        return null
    }
    val inputDim = pooled[0].size.toLong()

    val (trainIdx, validationIdx) =
        if (testSamples != null) samples.indices.toList() to (samples.size until all.size).toList()
        else stratifiedSplit(labelIndex)
    val trainX = trainIdx.map { pooled[it] }.toTypedArray()
    logInternal("modelBuilder: training '$modelName' on ${trainIdx.size} samples, validating on ${validationIdx.size}")

    val hiddenDim = 16
    val model = DenseNet(inputDim.toInt(), hiddenDim, labels.size)
    logInternal("modelBuilder: fitting dense[$hiddenDim relu] -> dense[${labels.size}] for $EPOCHS epochs")
    ModelBuildProgress.phase(BuildPhase.TRAINING, steps = EPOCHS, detail = "${trainIdx.size} samples")
    val last = model.fit(
        trainX, IntArray(trainIdx.size) { labelIndex[trainIdx[it]] },
        epochs = EPOCHS, batchSize = minOf(16, trainX.size), learningRate = 0.01f,
    ) { epoch ->
        val summary = "epoch ${epoch.epoch} loss ${fmt4(epoch.loss)} accuracy ${fmt3(epoch.accuracy)}"
        ModelBuildProgress.step(epoch.epoch, summary)
        if (epoch.epoch % EPOCH_LOG_EVERY == 0 && epoch.epoch < EPOCHS) logInternal("modelBuilder: $summary")
    }
    logInternal("modelBuilder: trained, epoch ${last.epoch} loss ${fmt4(last.loss)} accuracy ${fmt3(last.accuracy)}")

    ModelBuildProgress.phase(BuildPhase.VALIDATING, steps = validationIdx.size, detail = "${validationIdx.size} samples")
    val truth = validationIdx.map { i -> labelIndex[i] }.toIntArray()
    val prediction = validationIdx.mapIndexed { n, i -> model.predict(pooled[i]).also { ModelBuildProgress.step(n + 1) } }.toIntArray()
    val metrics = computeMetrics(labels, truth, prediction, trainIdx.size, warnings)
        .copy(validation = if (testSamples != null) validationName ?: "test set" else "split 20%")

    ModelBuildProgress.phase(BuildPhase.EXPORTING, detail = "AMP/modules/$modelName")
    return try {
        val layers = exportDenseLayers(model)
        val manifest = ModuleManifest(
            name = modelName,
            encoder = encoder,
            inputDim = inputDim,
            outputDim = layers.last().outDim,
            labels = labels,
        )
        val file = writeModule(manifest, denseStackToOnnx(layers, manifest.input, manifest.output, modelName))
        appendMetrics(file.parentFile, metrics)
        MetricsReport(modelName, metrics).print()
        ModelBuildProgress.done("accuracy ${fmt3(metrics.accuracy)} · macro F1 ${fmt3(metrics.macroF1)} on ${metrics.validation}")
        file
    } catch (e: Exception) {
        logInternal("modelBuilder: export of '$modelName' failed: ${e.message}", LogLevel.ERROR)
        ModelBuildProgress.failed("export failed: ${e.message}")
        null
    }
}

private fun fmt3(value: Double) = "%.3f".format(Locale.US, value)
private fun fmt4(value: Double) = "%.4f".format(Locale.US, value)

// The trained layers in ONNX form; the classifier's logits are exported behind a Softmax so callers get probabilities
private fun exportDenseLayers(model: DenseNet): List<DenseExport> = listOf(
    DenseExport(model.w1.copyOf(), model.b1.copyOf(), model.inputDim.toLong(), model.hiddenDim.toLong(), "Relu"),
    DenseExport(model.w2.copyOf(), model.b2.copyOf(), model.hiddenDim.toLong(), model.outputDim.toLong(), "Softmax"),
)
