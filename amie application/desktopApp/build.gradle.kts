import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

//repositories {
//    intellijPlatform {
//        defaultRepositories()
//    }
//}

//kotlin {
//    compilerOptions {
//        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
//        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
//        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
//    }
//}
dependencies {

    implementation(project(":shared"))
    implementation(libs.kotlin.scripting.jsr223)
    implementation(libs.kotlin.compiler.embeddable)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.java)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(compose.materialIconsExtended)
    implementation(libs.jserialcomm)
    implementation(libs.slf4j.simple)
    implementation(libs.jewel.ui)
    implementation(libs.jewel.int.ui.standalone)
    implementation(libs.jewel.int.ui.decorated.window)
    implementation(libs.kodeview)
    implementation("com.microsoft.onnxruntime:onnxruntime:1.19.2")
    implementation("ai.djl.huggingface:tokenizers:0.31.0")
    kotlin("plugin.serialization")
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.example.project"
            packageVersion = "1.0.0"
        }
    }
}
