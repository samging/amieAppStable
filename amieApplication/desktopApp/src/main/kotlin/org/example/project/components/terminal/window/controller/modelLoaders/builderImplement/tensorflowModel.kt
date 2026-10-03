package org.example.project.components.terminal.window.controller.modelLoaders.builderImplement

import org.example.project.components.terminal.window.controller.modelLoaders.modelProps
import java.nio.file.Path
import org.tensorflow.SavedModelBundle

class tensorflowModel(
    private val path: Path
) : modelProps {

    override fun load() {
        try {
            SavedModelBundle.load(path.toString(), "serve").use { model ->
                println("Loaded TensorFlow model from $path")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun close() {
        TODO("Create loader model method")
    }
}
