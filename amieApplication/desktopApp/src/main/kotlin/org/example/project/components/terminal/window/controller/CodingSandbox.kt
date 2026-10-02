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
import javax.script.ScriptEngineManager
import org.example.project.components.terminal.window.controller.ScriptLogger
import org.example.project.components.terminal.window.controller.TerminalWindow
import org.example.project.components.terminal.window.controller.ResponseDto
import org.example.project.components.terminal.window.controller.logToConsole
import java.time.Instant
import org.example.project.components.terminal.integrations.amiePilot.*
import org.slf4j.LoggerFactory

import org.tensorflow.SavedModelBundle
import org.tensorflow.ndarray.NdArrays
import org.tensorflow.types.TFloat32



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
                    val manager = ScriptEngineManager(Thread.currentThread().contextClassLoader)
                    val engine = manager.getEngineByExtension("kts") ?: error("Couldn't find engine")

                    val loggerObj = object : ScriptLogger {

                        override fun log(message: String) {
                            logToConsole(ResponseDto(time = Instant.now(), message = message))
                        }

                        override fun initAmie() {
                            //amie - source bert onnx
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

                        override fun importModelTensorflow(it: Path) {}

                        override fun forwardContext(tokens: (matrix: LongArray) -> Unit) {
                            tokens(LongArray(0))
                            return org.example.project.components.terminal.integrations.amiePilot.initContext()
                        }

                        override fun validateMetrics(train: Path, test: Path) {}
                        override fun implementation(it: Path, entry: Path) {}
                        override fun import(it: String) {}
                        override fun sourceFile(it: String) {}
                        override fun importPlugin(plugin: String) {}

                        override fun modelBuilder(it: () -> Unit): ModelCreationalInterface {
                            it()
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
                                    for (assemblyLine in assemblyPath.readLines())
                                    {
                                        println("[i]> $assemblyLine")
                                    }

                                    println("new one: ${assemblyPath.readText()}")
                                    engine.eval(assemblyPath.readText(), scriptBindings)
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
                    return@NavigationSidebar
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
