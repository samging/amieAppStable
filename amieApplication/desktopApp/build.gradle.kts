import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}
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
    implementation("org.tensorflow:tensorflow-core-platform:0.5.0")
    implementation("org.tensorflow:tensorflow-framework:0.5.0")
    kotlin("plugin.serialization")
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            packageName = "AmieApp"
            packageVersion = "1.0.0"

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            modules("java.net.http", "java.scripting", "java.sql")
            packageName = "amie pilot"

            macOS {
                iconFile.set(project.file("/Users/samuel/Documents/GitHub/amieAppStable/amie application/shared/src/commonMain/composeResources/drawable/amieDistributionIconNew.icns"))
                bundleID = "com.amie.app"
            }
        }
    }
}
