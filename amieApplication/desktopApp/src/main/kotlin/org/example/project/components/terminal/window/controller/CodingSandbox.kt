package org.example.project.components.terminal.window.controller

import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import org.example.project.getDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Path
import org.example.project.components.terminal.window.controller.envBuilder.BuildSettings
import org.example.project.components.terminal.window.controller.envBuilder.SettingsEnv
import org.example.project.components.terminal.window.controller.modelLoaders.ModelBuilder
import org.example.project.components.terminal.window.controller.envBuilder.EnvPlan
import org.example.project.components.terminal.window.controller.envBuilder.ScriptEnvironments
import org.example.project.data.remote.parser.DeviceManagerFactory
import org.example.project.components.terminal.window.controller.ScriptLogger
import org.example.project.components.terminal.window.controller.TerminalWindow
import org.example.project.components.terminal.window.controller.ResponseDto
import org.example.project.components.terminal.window.controller.logToConsole
import org.example.project.components.terminal.window.controller.logInternal
import java.time.Instant
import org.example.project.components.terminal.integrations.amiePilot.*
import org.slf4j.LoggerFactory
import org.example.project.ui.theme.ConsoleBackground
import org.example.project.ui.theme.ConsoleBorderColor
import org.example.project.ui.theme.GutterBackground
import org.example.project.ui.theme.LineNumberColor
import org.example.project.ui.theme.PanelDividerColor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TextFieldDefaults
import androidx.compose.ui.text.style.TextAlign
import org.example.project.ui.theme.ConsoleInputBackground
import org.example.project.ui.theme.EditorBackground
import org.example.project.ui.theme.TabActiveBackground
import org.example.project.ui.theme.TabActiveIndicator
import org.example.project.ui.theme.TabBarBackground
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.UnifiedBorderColor
import org.example.project.ui.theme.SuccessColor
import org.example.project.ui.theme.DangerColor
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.StrokeCap
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.ModelBuild
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.ModelBuildProgress
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.short
import java.time.Duration



object ScriptEngineCache {
    private val appClassLoader: ClassLoader = ScriptEngineCache::class.java.classLoader

    private val manager by lazy {
        // ClassLoader lookup avoids ClassLoader contention with the UI thread
        ScriptEngineManager(appClassLoader)
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

    private val envEngines = mutableMapOf<ClassLoader, ScriptEngine>()

    // The engine compiles against the context classloader it was created under, so each env classpath gets its own
    @Synchronized
    fun engineFor(loader: ClassLoader): ScriptEngine {
        if (loader === appClassLoader) return engine
        return envEngines.getOrPut(loader) {
            withContextClassLoader(loader) {
                ScriptEngineManager(loader).getEngineByExtension("kts") ?: error("Couldn't find Kotlin script engine")
            }
        }
    }

    val classLoader: ClassLoader get() = appClassLoader
}

fun <T> withContextClassLoader(loader: ClassLoader, block: () -> T): T {
    val thread = Thread.currentThread()
    val previous = thread.contextClassLoader
    thread.contextClassLoader = loader
    try {
        return block()
    } finally {
        thread.contextClassLoader = previous
    }
}

fun prewarmScriptEngine() {
    CoroutineScope(Dispatchers.Default).launch {
        ScriptEngineCache.engine // Triggers lazy load early so click is instantaneous
    }
}

private const val SANDBOX_SOURCE = "sandbox"
private const val SCRIPT_SOURCE = "$SANDBOX_SOURCE/main.kts"

@Composable
fun CodingSandbox(navController: NavController) {
    var selectedIndex by remember { mutableStateOf(1) }
    val codeField = remember { mutableStateOf("") }
    var constraintSpecified by remember { mutableStateOf(true) }
    var goalSpecified by remember { mutableStateOf(true) }
    val loop = rememberCoroutineScope()
    var runJob by remember { mutableStateOf<Job?>(null) }
    // Observable mirror of runJob: isActive isn't state, so the UI couldn't follow it
    var running by remember { mutableStateOf(false) }
    var runStartedAt by remember { mutableStateOf<Instant?>(null) }
    var lastRun by remember { mutableStateOf<RunOutcome?>(null) }
    // What modelBuilder is doing right now, and what it did last; reported from the build thread
    val build by ModelBuildProgress.current.collectAsState()
    val buildHistory by ModelBuildProgress.history.collectAsState()
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

    // Open the main.kts the user already wrote; the default script is only for a first start.
    // The editor keeps its own text state, so it is recreated (editorGeneration) when the file replaces its code
    var editorGeneration by remember { mutableStateOf(0) }
    var outputLevels by remember { mutableStateOf(LogLevel.entries.toSet()) }
    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) {
            runCatching { File(getDirectory().toString(), "pluginDir/main.kts").takeIf { it.isFile }?.readText() }.getOrNull()
        }
        if (!saved.isNullOrBlank() && codeField.value.isEmpty()) {
            codeField.value = saved
            highlights = Highlights.Builder()
                .code(saved)
                .language(highlights.getLanguage())
                .theme(highlights.getTheme())
                .build()
            editorGeneration++
        }
    }

    // Save main.kts once typing pauses; a new keystroke cancels the pending write so saves can't land out of order
    LaunchedEffect(codeField.value) {
        if (codeField.value.isEmpty()) return@LaunchedEffect
        delay(300)
        withContext(Dispatchers.IO) {
            runCatching {
                val withTargetFile = File(getDirectory().toString(), "pluginDir/main.kts")
                withTargetFile.parentFile?.mkdirs()
                withTargetFile.writeText(codeField.value)
            }.onFailure { exception ->
                logger.error("Failed to save main.kts: ${exception.message}")
                logToConsole(ResponseDto(time = Instant.now(), message = "Failed to save main.kts: ${exception.message}", level = LogLevel.ERROR, source = SANDBOX_SOURCE))
            }
        }
    }

    LaunchedEffect(codeField.value, constraintSpecified, goalSpecified) {
        val constraintFind = Regex("""val\s+constraints\s+\=\s+mutableListOf<String>(.*)""")
        val goalFind = Regex("""val\s+goals\s+\=\s+mutableListOf<String>(.*)""")

        constraintSpecified = constraintFind.containsMatchIn(codeField.value)
        goalSpecified = goalFind.containsMatchIn(codeField.value)
    }

    IntUiTheme(isDark = true) {
        val backgroundColor = JewelTheme.globalColors.panelBackground

        Row(modifier = Modifier.fillMaxSize().background(EditorBackground)) {

            NavigationSidebar(
                selectedIndex = selectedIndex,
                onIndexSelected = {
                    selectedIndex = it
                    if (it == 0) navController.navigate("home1/")
                },
                showPlay = true,
                modelsSelected = false,
                onModels = { navController.navigate("models") },
                running = running,
                runProgress = build?.fraction,

                showPlayCallback = {
                    if (runJob?.isActive == true) {
                        val busy = build?.let { " (modelBuilder '${it.modelName}' ${it.describe()})" } ?: ""
                        logToConsole(ResponseDto(time = Instant.now(), message = "Script is already running$busy", source = SANDBOX_SOURCE))
                        return@NavigationSidebar
                    }
                    // Snapshot the editor now: the debounced save may not have written main.kts yet
                    val code = highlights.getCode()
                    // Fresh output per run; previous runs stay in the log file
                    clearConsole(SANDBOX_SOURCE)
                    val startedAt = Instant.now()
                    running = true
                    runStartedAt = startedAt
                    runJob = loop.launch(Dispatchers.IO) {
                        logToConsole(ResponseDto(time = Instant.now(), message = "Running pluginDir/main.kts", source = SANDBOX_SOURCE))
                        val pluginPath = File(getDirectory().toString(), "pluginDir/main.kts")
                        val assemblyPath = File(getDirectory().toString(), "pluginDir/assembly.kts")
                        var failure: String? = null

                        try {
                            // Configuration phase: source the env with the script's imports before it compiles, as Gradle resolves before it runs
                            val plan = EnvPlan.scan(code)
                            val env = ScriptEnvironments.open(plan.envName, plan.activeProfile)
                            env.source(plan)
                            val envLoader = env.classLoader(ScriptEngineCache.classLoader)
                            val engine = ScriptEngineCache.engineFor(envLoader)
                            val gradleEngine = ScriptEngineCache.gradleEngine
                            val scriptScope = CoroutineScope(Dispatchers.IO)

                            val loggerObj = object : ScriptLogger {

                                override fun log(message: String, level: LogLevel) {
                                    logToConsole(ResponseDto(time = Instant.now(), message = message, level = level, source = SCRIPT_SOURCE))
                                }

                                override fun initAmie(text: String) {
                                    val result = org.example.project.components.terminal.integrations.amiePilot.initAmie(text)
                                    logToConsole(ResponseDto(time = Instant.now(), message = result.joinToString("\n"), source = SCRIPT_SOURCE))
                                }
                                override  fun initMod() : InteractionModel {
                                    return org.example.project.components.terminal.integrations.amiePilot.InitMod().create()
                                }
                                override fun importModelTensorflow(it: Path) {
                                    val o = ModelBuilder(it).load()
                                }

                                override fun forwardContext(text: String, tokens: (matrix: LongArray) -> Unit): Array<Array<FloatArray>>? =
                                    org.example.project.components.terminal.integrations.amiePilot.initContext(text, tokens)

                                override fun validateMetrics(
                                    modelPath: Path,
                                    train: Path,
                                    test: Path) {
                                    logToConsole(ResponseDto(time = Instant.now(), message = "validateMetrics", source = SCRIPT_SOURCE))
                                }

                                override fun implementation(it: BuildSettings) {
                                    scriptScope.launch {
                                        try {
                                            gradleEngine.eval(it.toString())
                                        } catch (e: Exception) {
                                            logToConsole(
                                                ResponseDto(
                                                    time = Instant.now(),
                                                    message = "Error: ${e.message}",
                                                    level = LogLevel.ERROR,
                                                    source = SCRIPT_SOURCE
                                                )
                                            )
                                        }
                                    }
                                    logToConsole(ResponseDto(time = Instant.now(), message = "implementation $it ", source = SCRIPT_SOURCE))
                                }

                                // Selecting an env is read before the run; at runtime it only reports what was sourced
                                override fun import(it: SettingsEnv) {
                                    if (ScriptEnvironments.current?.name == it.envName) {
                                        logInternal("env '${it.envName}' (profile ${it.activeProfile}) is the sourced environment")
                                    } else {
                                        logInternal("env '${it.envName}' must be selected with a literal amie.import(SettingsEnv(\"${it.envName}\", ...)) to be sourced", LogLevel.ERROR)
                                    }
                                }

                                override fun import(notation: String) {
                                    ScriptEnvironments.current?.import(notation)
                                    logToConsole(ResponseDto(time = Instant.now(), message = "import $notation", source = SCRIPT_SOURCE))
                                }

                                override fun internalEnv(): List<String> = env.print()

                                override fun importPlugin(plugin: String) {
                                    val file = File("/Users/samuel/Documents/GitHub/amieAppStable/amieApplication/desktopApp/AMP/pluginDir/corePlugins.json")
                                    val reader = DeviceManagerFactory.create(file)
                                    reader.getPlugins()
                                    logToConsole(ResponseDto(time = Instant.now(), message = "importPlugin $plugin", source = SCRIPT_SOURCE))
                                    val res = jars.run(plugin)
                                    logToConsole(ResponseDto( time = Instant.now(), message = res.toString(), source = SCRIPT_SOURCE))
                                }


                                override fun modelBuilder(modelName: String, it: (Any) -> Any): ModelCreationalInterface {
                                    // The lambda supplies labeled data: sourceFile rows of [text, label], List<Pair<text, label>> or Map<label, texts>
                                    // or amie.source("train.csv"), whose related test file becomes the validation set
                                    val produced = it(Pair(0, emptyList<String>()))
                                    val samples = org.example.project.components.terminal.window.controller.jetbrainDLBuilder.parseLabeledData(produced)
                                    val test = (produced as? org.example.project.components.terminal.window.controller.sourceFile.DataSource)?.related
                                    val exported = try {
                                        org.example.project.components.terminal.window.controller.jetbrainDLBuilder.buildClassifier(
                                            samples, modelName, validation = test?.samples(), validationName = test?.name
                                        )
                                    } catch (e: java.lang.reflect.InvocationTargetException) {
                                        e.cause?.printStackTrace()
                                        logInternal("modelBuilder failed: ${e.cause?.message}", LogLevel.ERROR)
                                        null
                                    } catch (e: Throwable) {
                                        e.printStackTrace()
                                        logInternal("modelBuilder failed: ${e.message}", LogLevel.ERROR)
                                        null
                                    }
                                    if (exported != null) {
                                        logToConsole(ResponseDto(time = Instant.now(), message = "model '$modelName' saved, load it with amie.initMod(\"$modelName\")", source = SCRIPT_SOURCE))
                                    }
                                    return object : ModelCreationalInterface() {
                                        override val modelName: String? = if (exported != null) modelName else null
                                    }
                                }

                            }

                            val scriptBindings = engine.createBindings().apply {
                                put("logger", loggerObj)
                            }

                            // Rebuild assembly.kts from the editor's current code on every run so it never evaluates a stale copy
                            pluginPath.parentFile?.mkdirs()
                            pluginPath.writeText(code)
                            assemblyPath.writeText(
                                "import org.example.project.components.terminal.window.controller.ScriptLogger\n" +
                                "val amie = bindings[\"logger\"] as ScriptLogger\n" +
                                code
                            )
                            try {
                                // Execution phase: env packages are loaded through the env classloader
                                withContextClassLoader(envLoader) {
                                    engine.eval(assemblyPath.readText(), scriptBindings)
                                }
                            } finally {
                                assemblyPath.writeText("")
                            }
                        } catch (e: Exception) {
                            failure = e.message ?: e.javaClass.simpleName
                            logger.error("Error running script: ${pluginPath}| ${e.message}")
                            logToConsole(ResponseDto(time = Instant.now(), message = "Error: ${e.message}", level = LogLevel.ERROR, source = SANDBOX_SOURCE))
                        } finally {
                            val took = Duration.between(startedAt, Instant.now())
                            lastRun = RunOutcome(ok = failure == null, took = took, detail = failure)
                            running = false
                            logToConsole(ResponseDto(
                                time = Instant.now(),
                                message = if (failure == null) "Finished in ${took.short()}" else "Failed after ${took.short()}",
                                level = if (failure == null) LogLevel.INFO else LogLevel.ERROR,
                                source = SANDBOX_SOURCE
                            ))
                        }
                    }
                    return@NavigationSidebar
                }
            )

            Column(modifier = Modifier.fillMaxWidth(0.6f)) {
                PanelHeader(title = "main.kts", subtitle = "Kotlin script")
                val editorTextStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = FontColor
                )
                // Gutter and editor share one vertical scroll so numbers stay aligned with their lines;
                // the editor scrolls sideways instead of wrapping, which would break that alignment
                BoxWithConstraints(modifier = Modifier.fillMaxSize().background(EditorBackground)) {
                    val viewportWidth = maxWidth
                    val viewportHeight = maxHeight
                    Row(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        LineNumberGutter(
                            lineCount = highlights.getCode().count { it == '\n' } + 1,
                            textStyle = editorTextStyle
                        )
                        Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                            key(editorGeneration) {
                            CodeEditText(
                                highlights = highlights,
                                onValueChange = { newCode ->
                                    codeField.value = newCode
                                    highlights = Highlights.Builder()
                                        .code(newCode)
                                        .language(highlights.getLanguage())
                                        .theme(highlights.getTheme())
                                        .build()
                                },
                                modifier = Modifier
                                    .widthIn(min = viewportWidth - GutterWidth)
                                    .heightIn(min = viewportHeight),
                                textStyle = editorTextStyle,
                                colors = TextFieldDefaults.textFieldColors(
                                    backgroundColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = FontColor
                                )
                            )
                            }
                        }
                    }
                }
            }


            Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(PanelDividerColor))

            Column(modifier = Modifier.fillMaxWidth().background(ConsoleBackground)) {
                ConsoleHeader(
                    levels = outputLevels,
                    counts = consoleEntries(SANDBOX_SOURCE).groupingBy { it.level }.eachCount(),
                    onToggle = { level -> outputLevels = if (level in outputLevels) outputLevels - level else outputLevels + level },
                    onShowAll = { outputLevels = LogLevel.entries.toSet() },
                    onClear = { clearConsole(SANDBOX_SOURCE) },
                )
                RunStatusBar(
                    running = running,
                    startedAt = runStartedAt,
                    lastRun = lastRun,
                    build = build,
                    lastBuild = buildHistory.firstOrNull(),
                )
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
                TerminalWindow(
                    source = SANDBOX_SOURCE,
                    loadHistory = false,
                    levels = outputLevels,
                    containerColor = ConsoleBackground,
                    inputColor = ConsoleInputBackground,
                    inputBorderColor = ConsoleBorderColor
                )
            }
        }
    }
}

/** How the last Run ended */
private data class RunOutcome(val ok: Boolean, val took: Duration, val detail: String?)

/**
 * One line under the console header that follows the Run button: the script's state and elapsed time,
 * and while amie.modelBuilder works, its phase, step and a progress bar.
 */
@Composable
private fun RunStatusBar(
    running: Boolean,
    startedAt: Instant?,
    lastRun: RunOutcome?,
    build: ModelBuild?,
    lastBuild: ModelBuild?,
) {
    // Ticks once a second while running so the elapsed time moves
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(running) {
        while (running) {
            now = Instant.now()
            delay(1000)
        }
    }
    val elapsed = startedAt?.takeIf { running }?.let { Duration.between(it, now).short() }

    val statusColor = when {
        running -> SuccessColor
        lastRun?.ok == false -> DangerColor
        lastBuild?.phase?.finished == true && !lastBuild.succeeded && lastRun?.ok != true -> DangerColor
        else -> MutedFontColor
    }
    val status = when {
        running && build != null -> "Running · ${elapsed ?: "0s"}"
        running -> "Running main.kts · ${elapsed ?: "0s"}"
        lastRun == null -> "Ready"
        lastRun.ok -> "Finished in ${lastRun.took.short()}"
        else -> "Failed after ${lastRun.took.short()}"
    }
    val buildLine = when {
        build != null -> "modelBuilder '${build.modelName}' · ${build.describe()}"
        lastBuild != null -> "last build '${lastBuild.modelName}' ${lastBuild.phase.label} in ${lastBuild.elapsed.short()}" +
                lastBuild.detail.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
        else -> null
    }

    Column(modifier = Modifier.fillMaxWidth().background(ConsoleBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulsingDot(color = statusColor, pulsing = running)
            Spacer(Modifier.width(8.dp))
            Text(
                text = status,
                style = TextStyle(color = if (running) FontColor else MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
            )
            if (buildLine != null) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = buildLine,
                    maxLines = 1,
                    style = TextStyle(color = if (build != null) FontColor else MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
            }
            if (build != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(build.fraction * 100).toInt()}%",
                    style = TextStyle(color = SuccessColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                )
            }
        }
        if (build != null) {
            LinearProgressIndicator(
                progress = build.fraction,
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = SuccessColor,
                backgroundColor = SuccessColor.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Square,
            )
        } else if (running) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = SuccessColor.copy(alpha = 0.7f),
                backgroundColor = SuccessColor.copy(alpha = 0.12f),
                strokeCap = StrokeCap.Square,
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ConsoleBorderColor))
        }
    }
}

@Composable
private fun PulsingDot(color: Color, pulsing: Boolean) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition()
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse)
        ).value
    } else 1f
    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color.copy(alpha = alpha)))
}

@Composable
private fun PanelHeader(title: String, subtitle: String) {
    // Light tab strip sitting above the darker working panel
    Column(modifier = Modifier.fillMaxWidth().background(TabBarBackground)) {
        Row(modifier = Modifier.fillMaxWidth().height(34.dp)) {
            Column(modifier = Modifier.fillMaxHeight().width(IntrinsicSize.Max).background(TabActiveBackground)) {
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(TabActiveIndicator))
                Row(
                    modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = TextStyle(color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subtitle,
                        style = TextStyle(color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(UnifiedBorderColor))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConsoleHeader(
    levels: Set<LogLevel>,
    counts: Map<LogLevel, Int>,
    onToggle: (LogLevel) -> Unit,
    onShowAll: () -> Unit,
    onClear: () -> Unit,
) {
    // Plain section label: the console is a docked panel, not an editor tab; level chips filter what it shows
    Column(modifier = Modifier.fillMaxWidth().background(ConsoleBackground)) {
        // Chips wrap onto a second line when the console is narrow
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "OUTPUT",
                modifier = Modifier.padding(end = 8.dp, top = 2.dp),
                style = TextStyle(
                    color = MutedFontColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
            )
            for (level in listOf(LogLevel.INTERNAL, LogLevel.ERROR, LogLevel.DATA, LogLevel.INFO)) {
                LevelChip(level, on = level in levels, count = counts[level] ?: 0, onClick = { onToggle(level) })
            }
            if (levels.size < LogLevel.entries.size) HeaderAction("Show all", onShowAll)
            HeaderAction("Clear", onClear)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ConsoleBorderColor))
    }
}

@Composable
private fun LevelChip(level: LogLevel, on: Boolean, count: Int, onClick: () -> Unit) {
    val color = level.color()
    Text(
        text = "${level.name} $count",
        style = TextStyle(
            color = if (on) color else MutedFontColor.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (on) color.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (on) color.copy(alpha = 0.35f) else ConsoleBorderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun HeaderAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = TextStyle(color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif),
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onClick() }.padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

private val GutterWidth = 48.dp

@Composable
private fun LineNumberGutter(lineCount: Int, textStyle: TextStyle) {
    // Top padding matches the TextField's built-in 16dp content padding so line 1 lines up
    Text(
        text = (1..lineCount).joinToString("\n"),
        style = textStyle.copy(color = LineNumberColor, textAlign = TextAlign.End),
        modifier = Modifier
            .width(GutterWidth)
            .background(GutterBackground)
            .padding(top = 16.dp, end = 12.dp)
    )
}
