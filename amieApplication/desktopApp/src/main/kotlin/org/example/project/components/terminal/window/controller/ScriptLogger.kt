package org.example.project.components.terminal.window.controller

import kotlinx.coroutines.runBlocking
import org.example.project.backend.AmieBackend
import org.example.project.components.terminal.integrations.amiePilot.InteractionModel
import java.nio.file.Path
import org.example.project.components.terminal.window.controller.logToConsole
import org.example.project.components.terminal.window.controller.ResponseDto
import org.example.project.components.terminal.window.controller.envBuilder.BuildSettings
import org.example.project.components.terminal.window.controller.envBuilder.SettingsEnv
import org.example.project.components.terminal.window.controller.sourceFile.readSourceFile
import java.time.Instant
import org.example.project.components.terminal.window.controller.sourceFile.DataSource
import org.example.project.components.terminal.window.controller.modelLoaders.OrtSessions
import org.example.project.components.terminal.window.controller.modelLoaders.builderImplement.openOnnxModel
import org.example.project.components.terminal.window.controller.envBuilder.ScriptEnvironments
import org.example.project.components.terminal.window.controller.pluginEngine.JarInterpreter
import org.example.project.components.terminal.window.controller.pluginEngine.JarPackages
import org.example.project.components.terminal.window.controller.modules.MetricsReport
import org.example.project.components.terminal.window.controller.modules.ModelSessions
import org.example.project.components.terminal.window.controller.modules.ModelServer
import org.example.project.components.terminal.window.controller.modules.readServing
import org.example.project.components.terminal.window.controller.databases.Database
import org.example.project.components.terminal.window.controller.databases.Databases
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.Architecture
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.BuildOptions

/**
 * A stable interface for logging from scripts.
 * Defined in a separate file to ensure it's a top-level class with stable visibility.
 */
interface ScriptLogger {
    fun log(message: String, level: LogLevel = LogLevel.INFO)
    fun info(message: String) = log(message, LogLevel.INFO)
    fun error(message: String) = log(message, LogLevel.ERROR)
    fun data(message: String) = log(message, LogLevel.DATA)
    // Named entities in the text with the bundled BERT NER model: one tag per token (O, B-PER, I-ORG, …), printed
    // and returned. A one-off: the model is opened and closed for the call. For many texts open it once with initMod()
    fun initAmie(text: String): List<String>
    fun initMod() : InteractionModel
    // The session of the ONNX module modelBuilder exported to AMP/modules/<modelName>: the one already open on it
    // when there is one, else a new one. For a name ending in .jar, a new instance of that package jar every
    // call (env, AMP/pluginDir, a path, or fetched from the Amie server)
    // Both kinds are listed on the Models page, where any session can be terminated
    fun initMod(modelName: String): InteractionModel =
        if (modelName.trim().endsWith(".jar", ignoreCase = true)) JarPackages.open(modelName.trim())
        else ModelSessions.open(modelName.trim())
    // Package jar sessions: packages.available(), packages.fetch("x.jar"), packages.print(), packages.closeAll()
    val packages: JarPackages get() = JarPackages
    // Saved states of the signed-in user on the Amie backend (uploads/<user>/state/<name>.json): any JSON text under
    // a name matching [A-Za-z0-9._-]{1,64}. Blocking calls like a package fetch; a failure is one ERROR line
    fun saveState(name: String, json: String) {
        runCatching { runBlocking { AmieBackend.saveState(name, json) } }
            .onSuccess { log("saveState: '$name' stored on the backend (${json.length} chars)") }
            .onFailure { log("saveState: '$name' not stored: ${it.message}", LogLevel.ERROR) }
    }
    fun loadState(name: String): String? =
        runCatching { runBlocking { AmieBackend.loadState(name) } }
            .onSuccess { log(if (it == null) "loadState: no state '$name' on the backend" else "loadState: '$name' read from the backend (${it.length} chars)") }
            .onFailure { log("loadState: '$name' not read: ${it.message}", LogLevel.ERROR) }
            .getOrNull()
    fun listStates(): List<String> =
        runCatching { runBlocking { AmieBackend.listStates() } }
            .onSuccess { log("listStates: ${it.size} saved state(s) on the backend" + (if (it.isEmpty()) "" else ": " + it.joinToString(", "))) }
            .onFailure { log("listStates: not read: ${it.message}", LogLevel.ERROR) }
            .getOrDefault(emptyList())

    // Default no-op implementations for optional script actions
    // A fixed ONNX model with initMod's interface (inference, getStatus, close); its session is managed by OrtSessions
    fun importModelOnnx(it: Path): InteractionModel? = runCatching { openOnnxModel(it) }.getOrNull()
    fun importModelOnnx(path: String): InteractionModel? = importModelOnnx(Path.of(path))
    // Global ORT sessions: sessions.print(), sessions.cancel("name"), sessions.cancelAll()
    val sessions: OrtSessions get() = OrtSessions
    // Internal reader of every open ORT session: inputs, outputs, runs and whether it is busy
    fun ortSessions(): List<String> = OrtSessions.print()
    fun importModelTensorflow(it: Path) {}
    // Runs text through the context model; tokens receives the input ids, returns last_hidden_state [batch][token][hidden]
    fun forwardContext(text: String, tokens: (matrix: LongArray) -> Unit = {}): Array<Array<FloatArray>>? = null
    fun validateMetrics(modelPath: Path, train: Path, test: Path) {}
    // Reads the last stored validation of AMP/modules/<modelName> and prints it; labels narrows the table to those labels
    fun validateMetrics(modelName: String, labels: List<String> = emptyList()): MetricsReport =
        MetricsReport.load(modelName).print(labels)
    // ASCII bar chart of the stored validation: accuracy, macro F1 and the F1 of each label (or the given ones)
    fun metricsChart(modelName: String, labels: List<String> = emptyList()): MetricsReport =
        MetricsReport.load(modelName).chart(labels)
    // ASCII line chart of accuracy and macro F1 across every build of the model
    fun metricsHistory(modelName: String): MetricsReport = MetricsReport.load(modelName).history()
    // The last validation's confusion matrix as an ASCII grid
    fun confusionMatrix(modelName: String): MetricsReport = MetricsReport.load(modelName).confusion()
    fun implementation(it: BuildSettings) {}
    fun import(it: SettingsEnv) {}
    // Gradle-style dependency, "group:module:version" or a jar file name; literal calls are sourced before the script compiles
    fun import(notation: String) { ScriptEnvironments.current?.import(notation) ?: logInternal("import: no environment is active", LogLevel.ERROR) }
    fun implementation(notation: String) = import(notation)
    // Prints the env being sourced, what this run sourced and everything in the environment; returns its packages
    fun internalEnv(): List<String> =
        ScriptEnvironments.current?.print() ?: emptyList<String>().also { logInternal("internalEnv: no environment is active", LogLevel.ERROR) }
    // Plugin jars by file name: jars.run("plugJar.jar"), jars.quit("plugJar.jar")
    val jars: JarInterpreter get() = JarInterpreter.shared
    // Rows of the file keyed by row index (header excluded) with their count; a named column keeps only that cell per row
    fun sourceFile(it: String, column: String = ""): Pair<Int, Map<Int, List<String>>> = readSourceFile(it, column)
    // Labeled data file with its text/label columns inferred: val train = amie.source("train.csv")
    fun source(path: String): DataSource? = DataSource.read(path)
    // Gives the model a test file to validate on after training on train: amie.relate(train, "test.csv")
    fun relate(train: DataSource?, test: String): DataSource? = train?.relate(test)
        ?: null.also { logInternal("relate: no training source to relate $test to", LogLevel.ERROR) }
    fun modelBuilder(modelName: String, data: DataSource?): ModelCreationalInterface = modelBuilder(modelName, BuildOptions()) { data ?: emptyList<String>() }
    // The same build with its own options: which heads to try, how to pick, whether to preprocess, the split
    fun modelBuilder(modelName: String, data: DataSource?, options: BuildOptions): ModelCreationalInterface = modelBuilder(modelName, options) { data ?: emptyList<String>() }
    fun importPlugin(plugin: String) {}
    fun modelBuilder(modelName: String, it: (Any) -> Any): ModelCreationalInterface = modelBuilder(modelName, BuildOptions(), it)
    fun modelBuilder(modelName: String, options: BuildOptions, it: (Any) -> Any): ModelCreationalInterface = object : ModelCreationalInterface() {}
    // One head to try: hidden layer widths (none = linear), epochs, batch size, learning rate, Relu or Tanh
    fun architecture(hidden: List<Int> = listOf(16), epochs: Int = 150, batchSize: Int = 16, learningRate: Double = 0.01, activation: String = "Relu"): Architecture =
        Architecture(hidden, epochs, batchSize, learningRate, activation)
    // How a build goes: the heads to compare (the four defaults unless given), macroF1 or accuracy to pick by, preprocessing on or off, the validation share
    fun buildOptions(candidates: List<Architecture> = Architecture.DEFAULT_CANDIDATES, pickBy: String = "macroF1", preprocess: Boolean = true, validationFraction: Double = 0.2, seed: Int = 42, balanceClasses: Boolean = false): BuildOptions =
        BuildOptions(candidates, pickBy, preprocess, validationFraction, seed, balanceClasses)
    // The preprocessor alone, without training: prints what it would clean, drop and refuse; true when a build could start
    fun checkData(data: Any?): Boolean {
        val samples = org.example.project.components.terminal.window.controller.jetbrainDLBuilder.parseLabeledData(data)
        val result = org.example.project.components.terminal.window.controller.jetbrainDLBuilder.Preprocessor.run(samples)
        result.lines().forEach { log("checkData: $it", if (it.startsWith("error:")) LogLevel.ERROR else LogLevel.DATA) }
        log(if (result.ok) "checkData: a build can start on these ${result.samples.size} rows" else "checkData: a build would stop; fix the ${result.errors.size} ${if (result.errors.size == 1) "reason" else "reasons"} above", if (result.ok) LogLevel.INFO else LogLevel.ERROR)
        return result.ok
    }

    // A database from AMP/databases.json: database("main").query("select …"); the same object while the app runs
    fun database(name: String = "main"): Database = Databases.open(name)
    // The configured databases: databases.print(), databases.names(), databases.closeAll()
    val databases: Databases get() = Databases

    // Serves the exported model over HTTP as the Models page's Listen does; null parameters keep what serving.json
    // says (host 127.0.0.1, port 8765, path /models/<name>, POST and GET). Returns the predict URL, null when it
    // could not start (the console says why: the model can't load, the port is taken, the path is served already)
    fun serve(modelName: String, port: Int? = null, host: String? = null, path: String? = null, methods: List<String>? = null): String? {
        val saved = readServing(modelName)
        val settings = saved.copy(host = host ?: saved.host, port = port ?: saved.port, path = path ?: saved.path, methods = methods ?: saved.methods)
        return if (ModelServer.start(modelName, settings)) settings.url(modelName).also { log("serve: '$modelName' answers ${settings.methods.joinToString("/")} at $it") }
        else null.also { log("serve: '$modelName' could not start; the console has the reason", LogLevel.ERROR) }
    }
    // Stops serving the model and remembers that, so it does not resume on the next start
    fun stopServing(modelName: String) = ModelServer.stop(modelName)
    // The predict URL of a model that is being served right now, null when it is not
    fun servingUrl(modelName: String): String? = if (ModelServer.isListening(modelName)) readServing(modelName).url(modelName) else null
}

abstract class ModelCreationalInterface {
    // Set when modelBuilder exported a module; its status is the last stored validation
    open val modelName: String? get() = null

    fun modelStatus(): Unit {
        val name = modelName
        if (name != null) MetricsReport.load(name).print()
        else logToConsole(ResponseDto(time = Instant.now(), message = "Status of model ${this.javaClass.simpleName}"))
    }

    // The stored validation as ASCII charts: bars per label, the build history and the confusion matrix
    fun modelChart(): Unit {
        val name = modelName ?: return logToConsole(ResponseDto(time = Instant.now(), message = "modelChart: nothing was exported", level = LogLevel.ERROR))
        MetricsReport.load(name).chart().history().confusion()
    }
}
