package org.example.project.components.terminal.window.controller

import java.nio.file.Path
import org.example.project.components.terminal.window.controller.logToConsole
import org.example.project.components.terminal.window.controller.ResponseDto
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
    fun validateMetrics(train: Path, test: Path) {}
    fun implementation(it: Path, entry: Path) {}
    fun import(it: String) {}
    fun sourceFile(it: String) {}
    fun importPlugin(plugin: String) {}
    fun modelBuilder(modelName: String, it: () -> Unit): ModelCreationalInterface = object : ModelCreationalInterface() {}
}

abstract class ModelCreationalInterface {
    fun modelStatus(): Unit {
        logToConsole(ResponseDto(time = Instant.now(), message = "Status of model ${this.javaClass.simpleName}"))
    }
}
