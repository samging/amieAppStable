package org.example.project.components.terminal.window.controller.jetbrainDLBuilder

import java.util.Locale
import java.io.File
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import org.example.project.components.terminal.window.controller.logInfo
import org.example.project.components.terminal.window.controller.logError
import org.example.project.components.terminal.window.controller.modules.DenseExport
import org.example.project.components.terminal.window.controller.modules.ModuleManifest
import org.example.project.components.terminal.window.controller.modules.denseStackToOnnx
import org.example.project.components.terminal.window.controller.modules.writeModule
import org.example.project.components.terminal.window.controller.modules.MetricsReport
import org.example.project.components.terminal.window.controller.modules.appendMetrics
import org.example.project.components.terminal.window.controller.modules.computeMetrics
import org.example.project.components.terminal.integrations.amiePilot.embedTexts
import org.example.project.components.terminal.integrations.amiePilot.BundledModels


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

/** The encoder a model is trained on unless the call names one: the bundled bge */
val DEFAULT_ENCODER: String get() = BundledModels.encoder
const val EPOCHS = 150
private const val EPOCH_LOG_EVERY = 25
private const val BUILD_SOURCE = "sandbox/modelBuilder"

/** Lines the person must see: the preprocessing summary, the candidates, the pick, and why a build stopped */
private fun say(message: String) = logInfo("modelBuilder: $message", BUILD_SOURCE)
private fun fail(message: String) = logError("modelBuilder: $message", BUILD_SOURCE)

/**
 * Trains a text classifier on labeled samples and exports it to AMP/modules/<modelName>/ (model.onnx, module.json,
 * metrics.json). Returns the exported model file, or null with the reasons in the console.
 *
 * Phases: the [Preprocessor] cleans and checks the data (and refuses what cannot be learned from), every text goes
 * through the encoder once, every candidate head in [options] is trained on the same split and scored on the same
 * validation rows, the best one by [BuildOptions.pickBy] is kept, validated and exported. With [validation] (a
 * related test file) the heads train on all of [samples] and are validated on it instead of a split. Every phase
 * is reported to [ModelBuildProgress], which the sandbox and the Models page show while the build runs.
 */
fun buildClassifier(
    samples: List<LabeledText>,
    modelName: String,
    encoder: String = DEFAULT_ENCODER,
    validation: List<LabeledText>? = null,
    validationName: String? = null,
    dataName: String? = null,
    description: String? = null,
    options: BuildOptions = BuildOptions(),
): File? {
    ModelBuildProgress.start(modelName)
    return try {
        buildClassifierPhases(samples, modelName, encoder, validation, validationName, dataName, description, options)
            .also { if (it == null && ModelBuildProgress.current.value != null) ModelBuildProgress.failed("not built") }
    } catch (e: Throwable) {
        ModelBuildProgress.failed(e.message ?: e.javaClass.simpleName)
        throw e
    }
}

private fun buildClassifierPhases(
    rawSamples: List<LabeledText>,
    modelName: String,
    encoder: String,
    validation: List<LabeledText>?,
    validationName: String?,
    dataName: String?,
    description: String?,
    options: BuildOptions,
): File? {
    // ---- 1. the data: cleaned, measured, refused when it cannot be learned from
    ModelBuildProgress.phase(BuildPhase.CHECKING, detail = "${rawSamples.size} rows")
    val prep = if (options.preprocess) Preprocessor.run(rawSamples) else {
        val check = checkVariability(rawSamples)
        Preprocessor.Result(rawSamples, emptyList(), listOf("${rawSamples.size} rows, not preprocessed"), check.warnings, check.errors)
    }
    prep.lines().forEach { line -> if (line.startsWith("error:")) fail(line.removePrefix("error: ")) else say(line) }
    if (!prep.ok) {
        fail("'$modelName' was not built: the data cannot give a usable classifier (${prep.errors.size} ${if (prep.errors.size == 1) "reason" else "reasons"} above). Fix the rows and run again")
        ModelBuildProgress.failed(prep.errors.first().take(80))
        return null
    }
    val samples = prep.samples
    val labels = samples.map { it.label }.distinct().sorted()
    val warnings = prep.warnings.toMutableList()
    val preprocessing = prep.notes + prep.dropped.groupBy { it.reason }.map { (reason, rows) -> "dropped ${rows.size}: $reason" }

    // Test rows can only be scored on labels the model trains on; they are cleaned the same way
    val testSamples = validation?.let { test ->
        val cleaned = if (options.preprocess) Preprocessor.run(test).samples else test
        val known = cleaned.filter { it.label in labels }
        if (known.size < cleaned.size) warnings += "${cleaned.size - known.size} rows of $validationName have labels the training data doesn't have"
        known.ifEmpty {
            warnings += "$validationName has no rows with trained labels, falling back to a ${(options.validationFraction * 100).toInt()}% split"
            null
        }
    }
    warnings.drop(prep.warnings.size).forEach { say("warning: $it") }

    // ---- 2. every text through the encoder, once
    val all = samples + testSamples.orEmpty()
    val labelIndex = all.map { labels.indexOf(it.label) }.toIntArray()
    logInternal("modelBuilder: embedding ${all.size} samples over ${labels.size} labels $labels")
    ModelBuildProgress.phase(BuildPhase.EMBEDDING, steps = all.size, detail = File(encoder).name)
    val pooled = embedTexts(all.map { it.text }, encoder) { done, total ->
        ModelBuildProgress.step(done, "$done of $total texts through ${File(encoder).name}")
    }.map { meanPooling(it) }
    checkEmbeddingSpread(pooled)?.let {
        fail(it)
        fail("'$modelName' was not built: the encoder sees no difference between the texts, so no head can tell the labels apart")
        ModelBuildProgress.failed("texts are too similar to learn from")
        return null
    }
    val inputDim = pooled[0].size.toLong()

    val (trainIdx, validationIdx) =
        if (testSamples != null) samples.indices.toList() to (samples.size until all.size).toList()
        else stratifiedSplit(labelIndex, options.validationFraction)
    val trainX = trainIdx.map { pooled[it] }.toTypedArray()
    val trainY = IntArray(trainIdx.size) { labelIndex[trainIdx[it]] }
    val truth = validationIdx.map { i -> labelIndex[i] }.toIntArray()
    say("training on ${trainIdx.size} rows, validating on ${validationIdx.size} (${if (testSamples != null) validationName ?: "test set" else "a ${(options.validationFraction * 100).toInt()}% split"}); ${options.candidates.size} ${if (options.candidates.size == 1) "head" else "heads to compare"}")

    // ---- 3. every candidate head on the same rows, scored on the same validation rows
    val candidates = options.candidates
    val totalEpochs = candidates.sumOf { it.epochs }
    ModelBuildProgress.phase(BuildPhase.TRAINING, steps = totalEpochs, detail = "${trainIdx.size} samples")
    var epochsDone = 0
    val trained = candidates.map { arch ->
        val model = DenseNet(inputDim.toInt(), arch.hidden, labels.size, arch.activation)
        val batchSize = minOf(arch.batchSize, trainX.size)
        logInternal("modelBuilder: fitting ${arch.label} (${model.parameters} parameters) for ${arch.epochs} epochs")
        val last = model.fit(trainX, trainY, epochs = arch.epochs, batchSize = batchSize, learningRate = arch.learningRate.toFloat()) { epoch ->
            val summary = "${arch.label} epoch ${epoch.epoch} loss ${fmt4(epoch.loss)} accuracy ${fmt3(epoch.accuracy)}"
            ModelBuildProgress.step(epochsDone + epoch.epoch, summary)
            if (epoch.epoch % EPOCH_LOG_EVERY == 0 && epoch.epoch < arch.epochs) logInternal("modelBuilder: $summary")
        }
        epochsDone += arch.epochs
        val prediction = validationIdx.map { i -> model.predict(pooled[i]) }.toIntArray()
        val scored = computeMetrics(labels, truth, prediction, trainIdx.size, emptyList())
        val result = CandidateResult(arch.label, arch.hidden, arch.epochs, model.parameters, scored.accuracy, scored.macroF1, last.accuracy, last.loss)
        say("${arch.label}: accuracy ${fmt3(scored.accuracy)} · macro F1 ${fmt3(scored.macroF1)} on validation (train accuracy ${fmt3(last.accuracy)}, loss ${fmt4(last.loss)}, ${model.parameters} parameters)")
        Triple(arch, model, result to (last to batchSize))
    }
    val results = trained.map { it.third.first }
    val best = pickBest(results, options.pickBy)
    val (arch, model, rest) = trained.first { it.third.first === best }
    val (last, batchSize) = rest.second
    val candidateResults = results.map { it.copy(chosen = it === best) }
    if (candidates.size > 1) say("chosen: ${best.architecture}, the best ${if (options.pickBy == "accuracy") "accuracy" else "macro F1"} on the validation rows" +
        (results.filter { it !== best }.maxByOrNull { if (options.pickBy == "accuracy") it.accuracy else it.macroF1 }?.let { " (next: ${it.architecture} ${fmt3(if (options.pickBy == "accuracy") it.accuracy else it.macroF1)})" } ?: ""))

    // ---- 4. the chosen head's validation, kept with the model
    ModelBuildProgress.phase(BuildPhase.VALIDATING, steps = validationIdx.size, detail = "${validationIdx.size} samples")
    val prediction = validationIdx.mapIndexed { n, i -> model.predict(pooled[i]).also { ModelBuildProgress.step(n + 1) } }.toIntArray()
    val metrics = computeMetrics(labels, truth, prediction, trainIdx.size, warnings)
        .copy(validation = if (testSamples != null) validationName ?: "test set" else "split ${(options.validationFraction * 100).toInt()}%", candidates = candidateResults, preprocessing = preprocessing)

    // ---- 5. export
    ModelBuildProgress.phase(BuildPhase.EXPORTING, detail = "AMP/modules/$modelName")
    return try {
        val layers = exportDenseLayers(model, arch)
        val manifest = ModuleManifest(
            name = modelName,
            encoder = encoder,
            inputDim = inputDim,
            outputDim = layers.last().outDim,
            labels = labels,
            data = dataName,
            description = description?.takeIf { it.isNotBlank() },
            architecture = "${arch.describe(inputDim, labels.size)} over mean-pooled ${File(encoder).name}",
            epochs = arch.epochs,
            batchSize = batchSize,
            learningRate = arch.learningRate,
            hiddenDim = arch.hidden.firstOrNull() ?: 0,
            trainRows = trainIdx.size,
            validationRows = validationIdx.size,
            validation = metrics.validation,
            finalLoss = last.loss,
            trainAccuracy = last.accuracy,
            search = if (candidates.size > 1) "${candidates.size} heads compared by ${if (options.pickBy == "accuracy") "accuracy" else "macro F1"}: " +
                candidateResults.joinToString { "${it.architecture} ${fmt3(if (options.pickBy == "accuracy") it.accuracy else it.macroF1)}${if (it.chosen) " (chosen)" else ""}" } else null,
        )
        val file = writeModule(manifest, denseStackToOnnx(layers, manifest.input, manifest.output, modelName))
        appendMetrics(file.parentFile, metrics)
        MetricsReport(modelName, metrics).print()
        ModelBuildProgress.done("${arch.label} · accuracy ${fmt3(metrics.accuracy)} · macro F1 ${fmt3(metrics.macroF1)} on ${metrics.validation}")
        file
    } catch (e: Exception) {
        fail("export of '$modelName' failed: ${e.message}")
        ModelBuildProgress.failed("export failed: ${e.message}")
        null
    }
}

private fun fmt3(value: Double) = "%.3f".format(Locale.US, value)
private fun fmt4(value: Double) = "%.4f".format(Locale.US, value)

// The trained layers in ONNX form; the classifier's logits are exported behind a Softmax so callers get probabilities
private fun exportDenseLayers(model: DenseNet, arch: Architecture): List<DenseExport> = model.layers.mapIndexed { i, layer ->
    DenseExport(layer.w.copyOf(), layer.b.copyOf(), layer.inDim.toLong(), layer.outDim.toLong(), if (i == model.layers.lastIndex) "Softmax" else arch.activation.replaceFirstChar { it.uppercase() })
}
