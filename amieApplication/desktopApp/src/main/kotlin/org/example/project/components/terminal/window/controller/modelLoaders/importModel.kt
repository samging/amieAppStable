package org.example.project.components.terminal.window.controller.modelLoaders

import org.example.project.components.terminal.window.controller.modelLoaders.builderImplement.onnxModel
import org.example.project.components.terminal.window.controller.modelLoaders.builderImplement.tensorflowModel
import org.example.project.components.terminal.window.controller.modelLoaders.builderImplement.tfLite
import java.nio.file.Path
import kotlin.io.path.extension


class ModelBuilder {
    companion object {
        fun builder(modelPath: Path): modelProps {
            return when (modelPath.extension.lowercase()) {
                "tflite" -> tfLite(modelPath)
                "onnx"   -> onnxModel(modelPath)
                "pb"     -> tensorflowModel(modelPath)
                else     -> throw IllegalArgumentException("Invalid model type: .${modelPath.extension}")
            }
        }

        operator fun invoke(modelPath: Path): modelProps = builder(modelPath)
    }
}