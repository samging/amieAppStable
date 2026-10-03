package org.example.project.components.terminal.window.controller.envBuilder

import org.gradle.api.Project
import org.gradle.api.Task // <--- MUST IMPORT TASK
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.util.Properties

data class SettingsEnv(
    val envName: String,
    val activeProfile: String
)

data class BuildSettings(
    val customKeys: Map<String, String>
)

class GenerateBuildSettingsEnv {
    fun generateEnv(
        project: Project,
        settingsEnv: SettingsEnv,
        buildSettings: BuildSettings
    ): TaskProvider<*> {

        // Explicitly type the parameter as 'task: Task'
        return project.tasks.register("generateEnvBuildSettings") { task: Task ->
            val outputDirectory = project.layout.buildDirectory.file("generated/env")

            // Now 'task.outputs' and 'task.doLast' resolve cleanly!
            task.outputs.dir(outputDirectory)

            task.doLast {
                val properties = Properties().apply {
                    setProperty("PROJECT_NAME", project.name)
                    setProperty("PROJECT_VERSION", project.version.toString())
                    setProperty("PROJECT_GROUP", project.group.toString())

                    setProperty("APP_ENV", settingsEnv.envName)
                    setProperty("ACTIVE_PROFILE", settingsEnv.activeProfile)

                    buildSettings.customKeys.forEach { (key: String, value: String) ->
                        setProperty("build.$key", value)
                    }

                    val extraProps = project.extensions.extraProperties
                    extraProps.properties.forEach { entry: Map.Entry<String, Any?> ->
                        val key = entry.key
                        val value = entry.value
                        if (value is String || value is Number || value is Boolean) {
                            setProperty("ext.$key", value.toString())
                        }
                    }
                }

                val file: File = outputDirectory.get().asFile
                file.parentFile.mkdirs()

                file.outputStream().use { stream ->
                    properties.store(stream, "Generated from Gradle Build and Settings File")
                }
            }
        }
    }
}