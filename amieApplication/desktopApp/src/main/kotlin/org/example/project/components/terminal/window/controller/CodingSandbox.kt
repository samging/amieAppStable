package org.example.project.components.terminal.window.controller

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import org.example.project.components.ui.viewport.AccentButton
import org.example.project.components.ui.viewport.AppTooltip
import org.example.project.components.ui.viewport.IconChip
import org.example.project.components.ui.viewport.SmallButton
import org.example.project.components.ui.viewport.Toggle
import org.example.project.ui.theme.PanelBorderSelected
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
import org.example.project.ui.theme.PanelDividerColor
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

private val importDirective = Regex("""^\s*import\s+[\w.]+(\.\*)?(\s+as\s+\w+)?\s*$""")

/**
 * What the engine evaluates: the user's import directives first (they must lead the file), then the ScriptLogger
 * import and `val amie`, then the SandboxApi prelude so the API is callable without the prefix, then the rest of
 * the user's code with its import lines blanked so its line numbers keep their spacing. main.kts stays the raw code.
 */
internal fun assembleScript(code: String): String {
    val lines = code.lines()
    val imports = lines.filter { importDirective.matches(it) }
    val body = lines.joinToString("\n") { if (importDirective.matches(it)) "" else it }
    return buildString {
        imports.forEach { append(it.trim()).append('\n') }
        append("import org.example.project.components.terminal.window.controller.ScriptLogger\n")
        append("val amie = bindings[\"logger\"] as ScriptLogger\n")
        append(SandboxApi.prelude())
        append(body)
    }
}

@Composable
fun CodingSandbox(navController: NavController) {
    var selectedIndex by remember { mutableStateOf(1) }
    val codeField = remember { mutableStateOf("") }
    // The console shows the time, level and source columns only on request
    var details by remember { mutableStateOf(false) }
    // "saved" beside the file name for a moment after main.kts was written
    var savedFlash by remember { mutableStateOf(false) }
    val runShortcut = remember { if (System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)) "⌘↵" else "Ctrl+Enter" }
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
                        "// The script API is in scope: log(), source(), modelBuilder(), ... (amie.name works too)\n" +
                        "log(\"hello from main.kts\")\n" +
                        "val train = source(\"intents.csv\")\n"
                )
                .language(SyntaxLanguage.KOTLIN)
                .theme(sandboxSyntaxTheme)
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
        savedFlash = false
        delay(300)
        val written = withContext(Dispatchers.IO) {
            runCatching {
                val withTargetFile = File(getDirectory().toString(), "pluginDir/main.kts")
                withTargetFile.parentFile?.mkdirs()
                withTargetFile.writeText(codeField.value)
            }.onFailure { exception ->
                logger.error("Failed to save main.kts: ${exception.message}")
                logToConsole(ResponseDto(time = Instant.now(), message = "Failed to save main.kts: ${exception.message}", level = LogLevel.ERROR, source = SANDBOX_SOURCE))
            }.isSuccess
        }
        if (written) { savedFlash = true; delay(1500); savedFlash = false }
    }

    /**
     * Runs main.kts: the sidebar's play button, the Run button over the editor and Cmd/Ctrl+Enter all end here. A
     * second press while it runs only says so. The console is emptied first; the log file keeps the earlier runs.
     */
    fun run() {
        if (runJob?.isActive == true) {
            val busy = build?.let { " (modelBuilder '${it.modelName}' ${it.describe()})" } ?: ""
            logToConsole(ResponseDto(time = Instant.now(), message = "Script is already running$busy", source = SANDBOX_SOURCE))
            return
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
                            logInternal("env '${it.envName}' must be selected with a literal environment(SettingsEnv(\"${it.envName}\", ...)) or amie.import(SettingsEnv(...)) to be sourced", LogLevel.ERROR)
                        }
                    }

                    override fun import(notation: String) {
                        ScriptEnvironments.current?.import(notation)
                        logToConsole(ResponseDto(time = Instant.now(), message = "import $notation", source = SCRIPT_SOURCE))
                    }

                    override fun internalEnv(): List<String> = env.print()

                    override fun importPlugin(plugin: String) {
                        // AMP/pluginDir/corePlugins.json names the core plugin set; absent on a fresh checkout
                        val file = File(getDirectory().toString(), "pluginDir/corePlugins.json")
                        if (file.isFile) DeviceManagerFactory.create(file).getPlugins()
                        else logToConsole(ResponseDto(time = Instant.now(), message = "importPlugin: no ${file.path}", source = SCRIPT_SOURCE))
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
                                samples, modelName, validation = test?.samples(), validationName = test?.name,
                                dataName = (produced as? org.example.project.components.terminal.window.controller.sourceFile.DataSource)?.name,
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
                assemblyPath.writeText(assembleScript(code))
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
                settingsSelected = false,
                onSettings = { navController.navigate("settings") },
                running = running,
                runProgress = build?.fraction,

                showPlayCallback = { run() },
            )

            // The editor and the console share the width at a rule that can be dragged; the split lasts while the app runs
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val total = maxWidth
                val minPane = 320f
                var editorWidth by remember { mutableStateOf(SandboxSplit.editorWidth.takeIf { it > 0f } ?: (total.value * 0.58f)) }
                val width = editorWidth.coerceIn(minPane, (total.value - minPane).coerceAtLeast(minPane)).dp
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.width(width).fillMaxHeight()) {
                        EditorHeader(saved = savedFlash, running = running, shortcut = runShortcut, onRun = { run() })
                        val editorTextStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = FontColor
                        )
                        // The editor draws its own gutter (numbers, fold markers) and scrolls sideways instead of wrapping
                        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(EditorBackground)) {
                            key(editorGeneration) {
                                SandboxEditor(
                                    initialCode = highlights.getCode(),
                                    highlights = highlights,
                                    onCodeChange = { newCode ->
                                        codeField.value = newCode
                                        highlights = Highlights.Builder()
                                            .code(newCode)
                                            .language(highlights.getLanguage())
                                            .theme(highlights.getTheme())
                                            .build()
                                    },
                                    textStyle = editorTextStyle,
                                    viewportWidth = maxWidth,
                                    viewportHeight = maxHeight,
                                    onRun = { run() },
                                )
                            }
                        }
                    }

                    SplitHandle { delta ->
                        editorWidth = (width.value + delta).coerceIn(minPane, (total.value - minPane).coerceAtLeast(minPane))
                        SandboxSplit.editorWidth = editorWidth
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight().background(ConsoleBackground)) {
                        ConsoleStrip(
                            running = running,
                            startedAt = runStartedAt,
                            lastRun = lastRun,
                            build = build,
                            lastBuild = buildHistory.firstOrNull(),
                            shortcut = runShortcut,
                            levels = outputLevels,
                            counts = consoleEntries(SANDBOX_SOURCE).groupingBy { it.level }.eachCount(),
                            onToggle = { level -> outputLevels = if (level in outputLevels) outputLevels - level else outputLevels + level },
                            onShowAll = { outputLevels = LogLevel.entries.toSet() },
                            onClear = { clearConsole(SANDBOX_SOURCE) },
                            details = details,
                            onDetails = { details = it },
                        )
                        TerminalWindow(
                            source = SANDBOX_SOURCE,
                            loadHistory = false,
                            levels = outputLevels,
                            details = details,
                            emptyHint = "run main.kts ($runShortcut) and what it logs appears here",
                            containerColor = ConsoleBackground,
                            inputColor = ConsoleInputBackground,
                            inputBorderColor = ConsoleBorderColor
                        )
                    }
                }
            }
        }
    }
}

/** The editor's share of the width, in dp, kept while the app runs; below zero until the user drags the rule */
/** How the last Run ended */
private data class RunOutcome(val ok: Boolean, val took: Duration, val detail: String?)

private object SandboxSplit {
    @Volatile var editorWidth = -1f
}

/**
 * The strip over the editor: the one file as a tab, "saved" beside it for a moment after each write, and Run at
 * the right end (Running… while the script runs; the console strip says how long).
 */
@Composable
private fun EditorHeader(saved: Boolean, running: Boolean, shortcut: String, onRun: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(TabBarBackground)) {
        Row(modifier = Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.fillMaxHeight().width(IntrinsicSize.Max).background(TabActiveBackground)) {
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(TabActiveIndicator))
                Row(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "main.kts", style = TextStyle(color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (saved) "saved" else "Kotlin script", style = TextStyle(color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (running) SmallButton("Running…", enabled = false, icon = Icons.Default.PlayArrow, tip = "main.kts is running; the console follows it") {}
            else AccentButton("Run", icon = Icons.Default.PlayArrow, tip = "Run main.kts ($shortcut). The file is saved as you type") { onRun() }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(UnifiedBorderColor))
    }
}

/** The rule between the editor and the console: dragged sideways to share the width; the cursor says so */
@Composable
private fun SplitHandle(onDrag: (Float) -> Unit) {
    val density = LocalDensity.current.density
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val drag = rememberDraggableState { delta -> onDrag(delta / density) }
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(7.dp)
            .background(EditorBackground)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.E_RESIZE_CURSOR)))
            .draggable(drag, Orientation.Horizontal),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(if (hovered) PanelBorderSelected else PanelDividerColor))
    }
}

/**
 * The one strip over the console. At the left what the script is doing: ready (and how to run it), running with
 * the elapsed time and, while modelBuilder works, its phase and progress; finished or failed with the time it
 * took, the detail in the tooltip. At the right the four levels as toggles with their counts, the details switch
 * (time, level and source columns) and Clear. A thin bar under it moves while a run or a build is on.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConsoleStrip(
    running: Boolean,
    startedAt: Instant?,
    lastRun: RunOutcome?,
    build: ModelBuild?,
    lastBuild: ModelBuild?,
    shortcut: String,
    levels: Set<LogLevel>,
    counts: Map<LogLevel, Int>,
    onToggle: (LogLevel) -> Unit,
    onShowAll: () -> Unit,
    onClear: () -> Unit,
    details: Boolean,
    onDetails: (Boolean) -> Unit,
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
    val failed = lastRun?.ok == false || (lastBuild?.phase?.finished == true && !lastBuild.succeeded && lastRun?.ok != true)
    val statusColor = when {
        running -> SuccessColor
        failed -> DangerColor
        else -> MutedFontColor
    }
    val status = when {
        running && build != null -> "running ${elapsed ?: "0s"} · modelBuilder '${build.modelName}' ${build.describe()} · ${(build.fraction * 100).toInt()}%"
        running -> "running ${elapsed ?: "0s"}"
        lastRun == null -> "ready · $shortcut runs main.kts"
        lastRun.ok -> "finished in ${lastRun.took.short()}"
        else -> "failed after ${lastRun.took.short()}"
    }
    val statusTip = listOfNotNull(
        lastRun?.detail?.let { "last error: $it" },
        lastBuild?.let { "last build '${it.modelName}' ${it.phase.label} in ${it.elapsed.short()}" + it.detail.takeIf { d -> d.isNotBlank() }?.let { d -> " · $d" }.orEmpty() },
    ).joinToString("\n").ifEmpty { null }

    Column(modifier = Modifier.fillMaxWidth().background(TabBarBackground)) {
        // The toggles move under the status when the console is narrow instead of squeezing it
        FlowRow(
            modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp).padding(start = 12.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(26.dp)) {
                PulsingDot(color = statusColor, pulsing = running)
                Spacer(modifier = Modifier.width(8.dp))
                AppTooltip(statusTip) {
                    Text(
                        text = status,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(color = if (running) FontColor else MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            for (level in listOf(LogLevel.INTERNAL, LogLevel.ERROR, LogLevel.DATA, LogLevel.INFO)) {
                Box(modifier = Modifier.height(26.dp), contentAlignment = Alignment.CenterStart) {
                    Toggle("${level.name.lowercase()} ${counts[level] ?: 0}", on = level in levels, tip = levelTip(level)) { onToggle(level) }
                }
            }
            if (levels.size < LogLevel.entries.size) Box(modifier = Modifier.height(26.dp), contentAlignment = Alignment.CenterStart) {
                SmallButton("all", tip = "Show every level again") { onShowAll() }
            }
            Box(modifier = Modifier.height(26.dp).padding(start = 4.dp), contentAlignment = Alignment.Center) {
                IconChip(Icons.Default.ViewColumn, tip = "Show the time, the level and the source of every line, as the log file has them", active = details) { onDetails(!details) }
            }
            Box(modifier = Modifier.height(26.dp), contentAlignment = Alignment.CenterStart) {
                SmallButton("Clear", tip = "Empty the console; AMP/logs.txt keeps everything") { onClear() }
            }
        }
        if (build != null) {
            LinearProgressIndicator(
                progress = build.fraction,
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = SuccessColor,
                backgroundColor = SuccessColor.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Square,
            )
        } else if (running) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = SuccessColor.copy(alpha = 0.7f),
                backgroundColor = SuccessColor.copy(alpha = 0.12f),
                strokeCap = StrokeCap.Square,
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(UnifiedBorderColor))
        }
    }
}

/** What a level's toggle says when the pointer rests on it */
private fun levelTip(level: LogLevel): String = when (level) {
    LogLevel.INTERNAL -> "Lines the app itself writes: build phases, metrics tables, charts, session messages. Click to hide or show them"
    LogLevel.ERROR -> "Failures: script exceptions, backend and sign-in errors. Click to hide or show them"
    LogLevel.DATA -> "What the script logs with data(...), and file dumps. Click to hide or show them"
    LogLevel.INFO -> "log(...) and info(...) from the script, and the start and end of a run. Click to hide or show them"
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
    Box(modifier = Modifier.size(7.dp).background(color.copy(alpha = alpha), RectangleShape))
}
