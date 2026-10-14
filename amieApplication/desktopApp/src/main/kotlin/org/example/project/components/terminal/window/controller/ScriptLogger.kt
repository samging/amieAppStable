package org.example.project.components.terminal.window.controller

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
import org.example.project.components.terminal.window.controller.modules.MetricsReport

/**
 * A stable interface for logging from scripts.
 * Defined in a separate file to ensure it's a top-level class with stable visibility.
 */
interface ScriptLogger {
    fun log(message: String, level: LogLevel = LogLevel.INFO)
    fun info(message: String) = log(message, LogLevel.INFO)
    fun error(message: String) = log(message, LogLevel.ERROR)
    fun data(message: String) = log(message, LogLevel.DATA)
    fun initAmie(text: String)
    fun initMod() : InteractionModel
    // Opens the ONNX session of a module exported by modelBuilder (AMP/modules/<modelName>)
    fun initMod(modelName: String): InteractionModel =
        org.example.project.components.terminal.integrations.amiePilot.InitMod(modelName).create()
    
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
    fun modelBuilder(modelName: String, data: DataSource?): ModelCreationalInterface = modelBuilder(modelName) { data ?: emptyList<String>() }
    fun importPlugin(plugin: String) {}
    fun modelBuilder(modelName: String, it: (Any) -> Any): ModelCreationalInterface = object : ModelCreationalInterface() {}
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
