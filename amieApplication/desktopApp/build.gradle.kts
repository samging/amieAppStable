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

afterEvaluate {
    tasks.withType<JavaExec>().configureEach {
        val launcher = javaToolchains.launcherFor(java.toolchain)
        javaLauncher.set(launcher)
        setExecutable(launcher.get().executablePath.asFile.absolutePath)
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
    // Postgres for scripts: database("main") reads AMP/databases.json
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("ai.djl.huggingface:tokenizers:0.31.0")

    implementation(libs.tensorflow.core)
    implementation(libs.tensorflow.platform) {
        exclude(group = "org.bytedeco", module = "javacpp")
    }

    implementation("org.jetbrains.kotlinx:kotlin-deeplearning-api:0.5.0")
    implementation("org.jetbrains.kotlinx:kotlin-deeplearning-tensorflow:0.5.0")

    implementation("org.bytedeco:javacpp:1.5.8")
    //runtimeOnly("org.bytedeco:tensorflow:1.15.5-1.5.8:macosx-arm64")
    //runtimeOnly("org.bytedeco:tensorflow:1.15.5-1.5.8:macosx-x86_64")
    implementation(gradleApi())
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("io.ktor:ktor-client-mock:3.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    kotlin("plugin.serialization")
}

// The BERT models a distribution ships: the NER model in this folder, the bge encoder in its bge/ subfolder.
// -Pamie.modelsDir=<path> or AMIE_MODELS_DIR points at another copy.
val modelsDir: String = (findProperty("amie.modelsDir") as String?)
    ?: System.getenv("AMIE_MODELS_DIR")
    ?: "/Users/samuel/Documents/onnx/bert"
val appResourcesDir = layout.buildDirectory.dir("appResources")

// The AMP folder a packaged app seeds its own from on the first start: the shipped settings, plugin dir, sample
// data and keycloak.json (the amie client is confidential, so the app cannot sign in without its secret), but not
// this machine's logs, signed-in session, UI scale or exported models
val prepareBundledAmp by tasks.registering(Sync::class) {
    from("AMP") {
        exclude("logs.txt", "session.json", "settings.json", "modules/**", "**/.DS_Store")
    }
    into(appResourcesDir.map { it.dir("common/AMP") })
}

// Puts the models into the app image after jpackage built it, so neither Gradle nor jpackage copies 800 MB twice.
// On macOS the files are APFS clones (cp -c) and take no extra disk; elsewhere they are copied.
val bundleModels by tasks.registering {
    dependsOn("createDistributable")
    val appImages = layout.buildDirectory.dir("compose/binaries/main/app").get().asFile
    val source = File(modelsDir)
    inputs.property("modelsDir", modelsDir)
    outputs.upToDateWhen { false }
    doLast {
        require(File(source, "model.onnx").isFile && File(source, "bge/model.onnx").isFile) {
            "No BERT models at ${source.path} (expected model.onnx and bge/model.onnx); pass -Pamie.modelsDir=<folder>"
        }
        val resourcesDirs = appImages.walkTopDown().filter { it.isDirectory && it.name == "resources" && it.parentFile.name == "app" }.toList()
        require(resourcesDirs.isNotEmpty()) { "No app/resources folder under $appImages" }
        resourcesDirs.forEach { resources ->
            val target = File(resources, "models/bert")
            target.deleteRecursively()
            target.mkdirs()
            // The NER model's own files plus the bge folder; sibling folders such as other encoders stay out
            val wanted = source.listFiles { f -> f.isFile && !f.name.startsWith(".") }!!.toList() + File(source, "bge")
            val mac = System.getProperty("os.name").lowercase().contains("mac")
            wanted.forEach { f ->
                if (mac) {
                    val cmd = listOf("cp", "-c", "-R", f.absolutePath, target.absolutePath)
                    val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
                    val out = proc.inputStream.bufferedReader().readText()
                    check(proc.waitFor() == 0) { "cp -c failed for ${f.name}: $out" }
                } else {
                    f.copyRecursively(File(target, f.name), overwrite = true)
                }
            }
            File(target, "bge/.DS_Store").delete()
            val size = target.walk().filter { it.isFile }.sumOf { it.length() } / (1024 * 1024)
            this.logger.lifecycle("Bundled BERT models into ${target.relativeTo(appImages)} ($size MB)")
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            packageName = "amie pilot"
            packageVersion = "1.0.0"
            description = "Amie pilot: a Kotlin sandbox that trains and serves intent models on a BERT encoder"
            vendor = "Amie"

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // ORT, DJL tokenizers, the Kotlin scripting compiler and ktor reach into many JDK modules; the whole runtime goes in
            includeAllModules = true

            // Shipped next to the jars as app/resources: the seeded AMP folder, and models/bert once bundleModels ran
            appResourcesRootDir.set(appResourcesDir)

            macOS {
                iconFile.set(project(":shared").file("src/commonMain/composeResources/drawable/amieDistributionIconNew.icns"))
                bundleID = "com.amie.app"
                // Packages like microphone.jar record through javax.sound inside this process; macOS only shows the
                // microphone permission prompt to a bundle that says why it wants the microphone
                infoPlist {
                    extraKeysRawXml = """
                        <key>NSMicrophoneUsageDescription</key>
                        <string>Amie packages such as microphone.jar record audio from a script when you ask them to.</string>
                    """.trimIndent()
                }
            }
        }
    }
}

// The Compose plugin registers its packaging tasks after evaluation, so they are wired up as they appear
tasks.configureEach {
    if (name == "prepareAppResources") dependsOn(prepareBundledAmp)
    // Every installer is built from the app image, so the models go in before it is packaged
    if (name in setOf("packageDmg", "packageMsi", "packageDeb", "packageDistributionForCurrentOS")) dependsOn(bundleModels)
}

// Tests get their own AMP folder and fake service addresses, so they never touch AMP/session.json or probe the
// real backend and Keycloak; the HTTP clients under test are given a mock engine instead
tasks.withType<Test>().configureEach {
    environment("AMIE_AMP_DIR", layout.buildDirectory.dir("test-amp").get().asFile.path)
    environment("AMIE_BACKEND_URL", "http://backend.test")
    environment("AMIE_SITE_URL", "http://site.test")
    environment("AMIE_S3_URL", "http://s3.test")
    environment("KEYCLOAK_URL", "http://keycloak.test")
}
