import org.example.project.components.terminal.window.controller.envBuilder.GenerateBuildSettingsEnv
import org.example.project.components.terminal.window.controller.envBuilder.SettingsEnv
import org.example.project.components.terminal.window.controller.envBuilder.BuildSettings
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure

val settingsEnv = SettingsEnv(
    envName = providers.gradleProperty("envName").getOrElse("staging"),
    activeProfile = providers.gradleProperty("activeProfile").getOrElse("cloud")
)

val buildSettings = BuildSettings(
    customKeys = mapOf(
        "timeout" to "5000",
        "region" to "us-east-1"
    )
)
val generateEnvTask = GenerateBuildSettingsEnv().generateEnv(project, settingsEnv, buildSettings)

configure<SourceSetContainer> {
    named("main") {
        resources.srcDir(layout.buildDirectory.dir("generated/env"))
    }
}

tasks.named("processResources") {
    dependsOn(generateEnvTask)
}