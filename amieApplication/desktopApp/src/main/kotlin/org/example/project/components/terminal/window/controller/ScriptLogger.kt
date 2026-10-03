package org.example.project.components.terminal.window.controller

import java.nio.file.Path
import org.example.project.components.terminal.window.controller.logToConsole
import org.example.project.components.terminal.window.controller.ResponseDto
import org.example.project.components.terminal.window.controller.envBuilder.BuildSettings
import org.example.project.components.terminal.window.controller.envBuilder.SettingsEnv
import java.time.Instant

/**
 * A stable interface for logging from scripts.
 * Defined in a separate file to ensure it's a top-level class with stable visibility.
 */
interface ScriptLogger {
    fun log(message: String)
    fun initAmie()
    
    // Default no-op implementations for optional script actions
    fun importModelOnnx(it: Path) {}
    fun importModelTensorflow(it: Path) {}
    fun forwardContext(tokens: (matrix: LongArray) -> Unit) {}
    fun validateMetrics(modelPath: Path, train: Path, test: Path) {}
    fun implementation(it: BuildSettings) {}
    fun import(it: SettingsEnv) {}
    fun sourceFile(it: String, column: String) {}
    fun importPlugin(plugin: String) {}
    fun modelBuilder(modelName: String, it: () -> Unit): ModelCreationalInterface = object : ModelCreationalInterface() {}
}

abstract class ModelCreationalInterface {
    fun modelStatus(): Unit {
        logToConsole(ResponseDto(time = Instant.now(), message = "Status of model ${this.javaClass.simpleName}"))
    }
}
