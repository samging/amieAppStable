package org.example.project.components.terminal.window.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import dev.snipme.kodeview.view.CodeEditText
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.ui.component.Text
import org.example.project.components.ui.viewport.NavigationSidebar
import java.io.File
import java.nio.file.Path
import org.example.project.getDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import org.example.project.components.terminal.window.controller.ScriptLogger
import org.example.project.components.terminal.window.controller.TerminalWindow
import org.example.project.components.terminal.window.controller.ResponseDto
import org.example.project.components.terminal.window.controller.logToConsole
import java.time.Instant
import org.example.project.components.terminal.integrations.amiePilot.*
import org.example.project.components.terminal.window.controller.envBuilder.BuildSettings
import org.example.project.components.terminal.window.controller.envBuilder.SettingsEnv
import org.example.project.components.terminal.window.controller.modelLoaders.ModelBuilder
import org.slf4j.LoggerFactory
import org.tensorflow.SavedModelBundle
import org.tensorflow.TensorFlow
import org.example.project.components.terminal.window.controller.sourceFile.SourceCsv
import org.example.project.data.remote.parser.DeviceManagerFactory


object ScriptEngineCache {
    private val manager by lazy {
        // ClassLoader lookup avoids ClassLoader contention with the UI thread
        ScriptEngineManager(ScriptEngineCache::class.java.classLoader)
    }

    val engine: ScriptEngine by lazy {
        manager.getEngineByExtension("kts")
            ?: manager.getEngineByName("kotlin")
            ?: error("Couldn't find Kotlin script engine")
    }

    // Shares the same cached manager instance
    val gradleEngine: ScriptEngine by lazy {
        manager.getEngineByExtension("kts")
            ?: manager.getEngineByName("kotlin")
            ?: engine
    }
}

fun prewarmScriptEngine() {
    CoroutineScope(Dispatchers.Default).launch {
        ScriptEngineCache.engine // Triggers lazy load early so click is instantaneous
    }
}

@Composable
fun CodingSandbox(navController: NavController) {
    var selectedIndex by remember { mutableStateOf(1) }
    val codeField = remember { mutableStateOf("") }
    var constraintSpecified by remember { mutableStateOf(true) }
    var goalSpecified by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val loop = rememberCoroutineScope()
    val logger = LoggerFactory.getLogger("CodingSandbox")

    var highlights by remember {
        mutableStateOf(
            Highlights.Builder()
                .code("" +
                        "val constraints = mutableListOf<String>()\n" +
                        "val goals = mutableListOf<String>()\n" +
                        "amie.initAmie()\n"
                )
                .language(SyntaxLanguage.KOTLIN)
                .theme(SyntaxThemes.default(darkMode = true))
                .build()
        )
    }

    LaunchedEffect(codeField.value, constraintSpecified, goalSpecified) {
        val constraintFind = Regex("""val\s+constraints\s+\=\s+mutableListOf<String>(.*)""")
        val goalFind = Regex("""val\s+goals\s+\=\s+mutableListOf<String>(.*)""")

        constraintSpecified = constraintFind.containsMatchIn(codeField.value)
        goalSpecified = goalFind.containsMatchIn(codeField.value)
    }

    IntUiTheme(isDark = true) {
        val backgroundColor = JewelTheme.globalColors.panelBackground

        Row(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {

            NavigationSidebar(
                selectedIndex = selectedIndex,
                onIndexSelected = {
                    selectedIndex = it
                    if (it == 0) navController.navigate("home1/")
                },
                showPlay = true,

                showPlayCallback = {
                    val scriptScope = CoroutineScope(Dispatchers.IO)
                    CoroutineScope(Dispatchers.IO).launch {
                    val engine = ScriptEngineCache.engine
                        val gradleEngine = ScriptEngineCache.gradleEngine

                    val loggerObj = object : ScriptLogger {

                        override fun log(message: String) {
                            logToConsole(ResponseDto(time = Instant.now(), message = message))
                        }

                        override fun initAmie() {
                            val result = org.example.project.components.terminal.integrations.amiePilot.initAmie()
                            logToConsole(ResponseDto(time = Instant.now(), message = result.joinToString("\n")))
                        }

                        override fun importModelOnnx(it: Path) {
                            try {
                                require(it.toFile().exists())
                            } catch(e: Exception) {
                                logToConsole(ResponseDto(time = Instant.now(), message = "Error: ${e.message}"))
                            }
                            logToConsole(ResponseDto(time = Instant.now(), message = "importModelOnnx"))
                        }

                        override fun importModelTensorflow(it: Path) {
                            val o = ModelBuilder(it).load()
                        }

                        override fun forwardContext(tokens: (matrix: LongArray) -> Unit) {
                            tokens(LongArray(0))
                            return org.example.project.components.terminal.integrations.amiePilot.initContext()
                        }

                        override fun validateMetrics(
                            modelPath: Path,
                            train: Path,
                            test: Path) {
                            logToConsole(ResponseDto(time = Instant.now(), message = "validateMetrics"))
                        }

                        override fun implementation(it: BuildSettings) {
                            scriptScope.launch {
                                try {
                                    gradleEngine.eval(it.toString())
                                } catch (e: Exception) {
                                    logToConsole(
                                        ResponseDto(
                                            time = Instant.now(),
                                            message = "Error: ${e.message}"
                                        )
                                    )
                                }
                            }
                            logToConsole(ResponseDto(time = Instant.now(), message = "implementation $it "))
                        }

                        override fun import(it: SettingsEnv) {
                            scriptScope.launch {
                                try {
                                    gradleEngine.eval(it.toString())
                                } catch (e: Exception) {
                                    logToConsole(
                                        ResponseDto(
                                            time = Instant.now(),
                                            message = "Error: ${e.message}"
                                        )
                                    )
                                }
                            }
                            logToConsole(ResponseDto(time = Instant.now(), message = "import $it"))
                        }

                        override fun sourceFile(it: String, column: String) {
                                val toFile = File(it)
                                if (toFile.exists()) {
                                    when(toFile.extension) {
                                        "csv" -> SourceCsv(it).get()
                                        else -> logToConsole(ResponseDto(time = Instant.now(), message = "unsupported type"))
                                    }
                                }
                        }

                        override fun importPlugin(plugin: String) {
                            val file = File("/Users/samuel/Documents/GitHub/amieAppStable/amieApplication/desktopApp/AMP/pluginDir/corePlugins.json")
                            val reader = DeviceManagerFactory.create(file)

                            logToConsole(ResponseDto(time = Instant.now(), message = "importPlugin $plugin"))
                        }


                        override fun modelBuilder(modelName: String, it: () -> Unit): ModelCreationalInterface {
                            println("INSIDE MODEL BUILDER")
                            it()

                            try {
                                // Force JavaCPP to extract and load C++ native libraries into process memory
                                //SavedModelBundle.Loader.load(org.tensorflow.internal.c_api.global.tensorflow::class.java)
                                CoroutineScope(Dispatchers.IO).launch {
                                    //println("TensorFlow Native Version preloaded: ${TensorFlow.version()}")
                                }
                            } catch (e: Throwable) {
                                System.err.println("Preload failed: ${e.message}")
                                e.printStackTrace()
                            }
                            try {
                                val nodes = org.example.project.components.terminal.integrations.amiePilot.getShapeContext()
                                //  org.example.project.components.terminal.integrations.amiePilot.createModel(nodes[0], nodes[1], nodes[2], modelName)
                            } catch (e: java.lang.reflect.InvocationTargetException) {
                                println("--- ROOT CAUSE OF SCRIPT FAILURE ---")
                                e.cause?.printStackTrace() // <--- THIS WILL SHOW THE ACTUAL ERROR
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                            return object : ModelCreationalInterface() {
                            }
                        }

                    }

                    val scriptBindings = engine.createBindings().apply {
                        put("logger", loggerObj)
                    }

                    loop.launch {
                        logToConsole(ResponseDto(time = Instant.now(), message = "pluginDir/main.kt"))

                        val pluginPath = File(getDirectory().toString(), "pluginDir/main.kts")
                        val assemblyPath = File(getDirectory().toString(), "pluginDir/assembly.kts")

                        try {
                            if (pluginPath.exists()) {
                                val pluginPathText = pluginPath.readText()

                                val regexImports = Regex(
                                    pattern = """import org.example.project.components.terminal.window.controller.ScriptLogger.*val amie = bindings\["logger"\] as ScriptLogger.*""",
                                    options = setOf(RegexOption.DOT_MATCHES_ALL)
                                )

                                if (assemblyPath.readText().contains(regexImports)) {
                                    scriptScope.launch {
                                        for (assemblyLine in assemblyPath.readLines()) {
                                            println("[i]> $assemblyLine")
                                        }

                                        println("new one: ${assemblyPath.readText()}")
                                        engine.eval(assemblyPath.readText(), scriptBindings)
                                    }
                                }

                                else {
                                    println("AS NOT: ${pluginPathText} | ${assemblyPath.readText().contains(regexImports)}")
                                    assemblyPath.appendText("import org.example.project.components.terminal.window.controller.ScriptLogger\nval amie = bindings[\"logger\"] as ScriptLogger")
                                    assemblyPath.appendText("\n${pluginPathText}")
                                    engine.eval(assemblyPath.readText(), scriptBindings)
                                    assemblyPath.writeText("")
                                }
                            } else {
                                logToConsole(ResponseDto(time = Instant.now(), message = "Script not found: ${pluginPath.absolutePath}"))
                            }
                        } catch (e: Exception) {
                            logger.error("Error running script: ${pluginPath}| ${e.message}")
                            logToConsole(ResponseDto(time = Instant.now(), message = "Error: ${e.message}"))
                        }
                    }
                    return@launch
                }
            }


            )

            Column(modifier = Modifier.fillMaxWidth(0.7f)) {
                CodeEditText(
                    highlights = highlights,
                    onValueChange = { newCode ->
                        codeField.value = newCode
                        highlights = Highlights.Builder()
                            .code(newCode)
                            .language(highlights.getLanguage())
                            .theme(highlights.getTheme())
                            .build()

                        coroutineScope.launch(Dispatchers.IO) {
                            runCatching {
                                val targetDir = File(getDirectory().toString())
                                val withTargetFile = File(targetDir, "pluginDir/main.kts")
                                withTargetFile.parentFile?.mkdirs()

                                println("DEBUG ${withTargetFile.absolutePath} | ${targetDir.absolutePath}")
                                withTargetFile.writeText(newCode)
                                println(withTargetFile.readText())
                            }.onFailure { exception ->
                                println("Failed with exception: ${exception.message}")
                                exception.printStackTrace()
                            }
                        }
                                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                        .padding(8.dp),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                )
            }


            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E))) {
//                Text(
//                    text = "${codeField.value}",
//                    modifier = Modifier.padding(bottom = 16.dp)
//                )
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(10.dp)
//                            .background(
//                                color = if (constraintSpecified) Color.Green else Color.Red,
//                                shape = CircleShape
//                            )
//                            .align(Alignment.CenterVertically)
//                    ) {}
//
//                    Text(
//                        text = " Constraint"
//                    )
//                }
                //Spacer(modifier = Modifier.height(16.dp))
//                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E))) {
//                    Box(
//                        modifier = Modifier
//                            .size(10.dp)
//                            .background(
//                                color = if (goalSpecified) Color.Green else Color.Red,
//                                shape = CircleShape
//                            )
//                            .align(Alignment.CenterVertically)
//                    ) {}
//                    Text(
//                        text = " Goal",
//                    )
//                }
//                Spacer(modifier = Modifier.height(16.dp))
                TerminalWindow()
            }
        }
    }
}
