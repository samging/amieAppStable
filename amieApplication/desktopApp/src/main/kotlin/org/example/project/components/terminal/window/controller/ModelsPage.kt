package org.example.project.components.terminal.window.controller

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.auth.AuthState
import org.example.project.backend.AmieBackend
import org.example.project.components.terminal.window.controller.envBuilder.DEFAULT_ENV
import org.example.project.components.terminal.window.controller.envBuilder.ScriptEnvironments
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.ModelBuild
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.ModelBuildProgress
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.buildClassifier
import org.example.project.components.terminal.window.controller.modelLoaders.OrtSessions
import org.example.project.components.terminal.window.controller.modules.MetricsImages
import org.example.project.components.terminal.window.controller.modules.MetricsPlot
import org.example.project.components.terminal.window.controller.modules.MetricsRun
import org.example.project.components.terminal.window.controller.modules.ModelServer
import org.example.project.components.terminal.window.controller.modules.ModelSessions
import org.example.project.components.terminal.window.controller.modules.ModelValidation
import org.example.project.components.terminal.window.controller.modules.ModuleManifest
import org.example.project.components.terminal.window.controller.modules.PackageCollection
import org.example.project.components.terminal.window.controller.modules.PackageCollections
import org.example.project.components.terminal.window.controller.modules.ServingSettings
import org.example.project.components.terminal.window.controller.modules.SessionLogEntry
import org.example.project.components.terminal.window.controller.modules.ValidationProgress
import org.example.project.components.terminal.window.controller.modules.ensureDummyModule
import org.example.project.components.terminal.window.controller.modules.ensureSampleData
import org.example.project.components.terminal.window.controller.modules.modulesDir
import org.example.project.components.terminal.window.controller.modules.readMetricsHistory
import org.example.project.components.terminal.window.controller.modules.readModule
import org.example.project.components.terminal.window.controller.modules.readRegistry
import org.example.project.components.terminal.window.controller.modules.readServing
import org.example.project.components.terminal.window.controller.modules.uptime
import org.example.project.components.terminal.window.controller.modules.writeServing
import org.example.project.components.terminal.window.controller.pluginEngine.JarContents
import org.example.project.components.terminal.window.controller.pluginEngine.JarInspector
import org.example.project.components.terminal.window.controller.pluginEngine.JarOrigin
import org.example.project.components.terminal.window.controller.pluginEngine.JarPackages
import org.example.project.components.terminal.window.controller.pluginEngine.ServerJar
import org.example.project.components.terminal.window.controller.sourceFile.DataSource
import org.example.project.components.terminal.window.controller.sourceFile.parseCsv
import org.example.project.components.ui.viewport.NavigationSidebar
import org.example.project.getDirectory
import org.example.project.ui.theme.AccentButtonHover
import org.example.project.ui.theme.ActiveColor
import org.example.project.ui.theme.ChipBackground
import org.example.project.ui.theme.ChipBorderColor
import org.example.project.ui.theme.ChipHoverBackground
import org.example.project.ui.theme.CodeBackground
import org.example.project.ui.theme.ConsoleBackground
import org.example.project.ui.theme.ConsoleBorderColor
import org.example.project.ui.theme.EditorBackground
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.IdleColor
import org.example.project.ui.theme.LogErrorTextColor
import org.example.project.ui.theme.LogMessageColor
import org.example.project.ui.theme.LogTimestampColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.PageAccent
import org.example.project.ui.theme.PanelBorder
import org.example.project.ui.theme.PanelBorderSelected
import org.example.project.ui.theme.PanelSurface
import org.example.project.ui.theme.PanelSurfaceSelected
import org.example.project.ui.theme.SuccessColor
import org.example.project.ui.theme.TabActiveBackground
import org.example.project.ui.theme.TabActiveIndicator
import org.example.project.ui.theme.TabBarBackground
import org.example.project.ui.theme.UnifiedBorderColor
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ---------- what the page shows, read off the UI thread ---------- */

private enum class PageTab(val title: String) { MODELS("Models"), PACKAGES("Packages"), SESSIONS("Sessions") }

/** Any open session on this page, named after its owner: an intent model, a package jar or a bare ONNX file */
private data class SessionInfo(
    val key: String,
    val owner: String,
    val id: Int,
    val kind: String,
    val running: Boolean,
    val runs: Int,
    val openedAt: Instant,
    /** What the session was asked and answered; empty with hasLog = false for a bare ONNX session */
    val log: List<SessionLogEntry>,
    val hasLog: Boolean,
    val terminate: () -> Unit,
)

/** An intent model as a component: its manifest, metrics, data, how it is served, whether it listens and its sessions */
private data class ModelInfo(
    val name: String,
    val manifest: ModuleManifest,
    val serving: ServingSettings,
    val stats: ModelServer.ServingStats?,
    val metrics: List<MetricsRun>,
    /** The rendered plots of the last run, PNG files under AMP/modules/<name>/plots */
    val plots: List<MetricsPlot>,
    /** The csv the model was trained on, when it is known and present */
    val dataFile: File?,
    /** The csv of the last validation, when it is a file that is present */
    val validationFile: File?,
    val sessions: List<SessionInfo>,
) {
    val url: String? get() = stats?.url
    val listening: Boolean get() = stats != null
    val busy: Boolean get() = sessions.any { it.running }
    /** Running when it serves or has a session open; stopped otherwise */
    val running: Boolean get() = listening || sessions.isNotEmpty()
    val status: String get() = when {
        busy -> "inferring"
        running -> "running"
        else -> "stopped"
    }
    val lastRun: MetricsRun? get() = metrics.lastOrNull()
    val accuracy: Double get() = lastRun?.accuracy ?: -1.0

    /** The data the model was trained on, as the list groups it: the manifest's file, else the dummy's fixed head */
    val dataName: String get() = manifest.data ?: if (name == "dummy") "written, not trained" else "unknown data"

    /** Name, labels, status, data or validation file contain the query */
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        return q.isEmpty() || name.lowercase().contains(q) || manifest.labels.any { it.lowercase().contains(q) } ||
            status.lowercase().contains(q) || dataName.lowercase().contains(q) || metrics.any { it.validation.lowercase().contains(q) }
    }
}

/**
 * A package jar the sandbox can open with initMod("name.jar"): local (env or AMP/pluginDir) or offered by the
 * server, with where it came from, what it contains, the collections it is in and the sessions open on it.
 */
private data class JarInfo(
    val name: String,
    val source: String,
    val origin: JarOrigin?,
    val url: String?,
    val page: String,
    val version: String?,
    val file: File?,
    val contents: JarContents?,
    val collections: List<String>,
    val sessions: List<SessionInfo>,
) {
    val local: Boolean get() = file != null
    val open: Int get() = sessions.size
    val status: String get() = when {
        sessions.any { it.running } -> "inferring"
        open > 0 -> "$open open"
        local -> "idle"
        else -> "on server"
    }

    /** Where the jar lives, as a group of the list */
    val location: String get() = when (source) { "server" -> "On the server"; "env" -> "In the env"; else -> "In AMP/pluginDir" }

    /** Name, version, Java, collection, entry class, a class or a resource name contain the query */
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        if (name.lowercase().contains(q) || version?.lowercase()?.contains(q) == true || status.lowercase().contains(q) || collections.any { it.lowercase().contains(q) }) return true
        val c = contents ?: return false
        return c.javaSupport.lowercase().contains(q) || c.entryClass?.lowercase()?.contains(q) == true ||
            c.classes.any { it.lowercase().contains(q) } || c.resources.any { it.lowercase().contains(q) }
    }
}

private data class ModelsSnapshot(
    val models: List<ModelInfo> = emptyList(),
    val jars: List<JarInfo> = emptyList(),
    val sessions: List<SessionInfo> = emptyList(),
    val collections: List<PackageCollection> = emptyList(),
)

/**
 * What the page showed last, kept for the life of the app so the page opens on it at once instead of on an empty
 * list, plus the server's jar listing, which is the one slow read and is fetched after the local state is shown.
 */
private object ModelsCache {
    @Volatile var snapshot = ModelsSnapshot()
    @Volatile var remoteJars: List<ServerJar> = emptyList()
    @Volatile var remoteLoaded = false
}

private const val KIND_MODEL = "intent model"
private const val KIND_PACKAGE = "package"
private const val KIND_ONNX = "ONNX session"

/** Reads the local state; with [remote] the server's jar listing too (cached a minute by JarPackages) */
private fun readSnapshot(remote: Boolean): ModelsSnapshot {
    // ORT sessions named module:<name> belong to a model session and are terminated through it
    val sessions = ModelSessions.all.map { s ->
        SessionInfo("model:${s.id}", s.name, s.id, KIND_MODEL, s.running > 0, s.runs, s.openedAt, s.log.lines, true) { ModelSessions.close(s.id) }
    } + JarPackages.open.map { s ->
        SessionInfo("package:${s.id}", s.jarName, s.id, KIND_PACKAGE, s.running > 0, s.runs, s.openedAt, s.log.lines, true) { JarPackages.close(s.id) }
    } + OrtSessions.all.filter { !it.name.startsWith("module:") }.map { s ->
        SessionInfo("ort:${s.id}", s.name, s.id, KIND_ONNX, s.running > 0, s.runs, s.openedAt, emptyList(), false) { OrtSessions.cancel(s.id.toString()) }
    }

    val found = (readRegistry().modules.keys + (modulesDir.listFiles()?.filter { File(it, "module.json").isFile }?.map { it.name } ?: emptyList())).distinct()
    // A fresh checkout gets the dummy model so there is a component to configure right away
    val names = (if (found.isEmpty()) listOfNotNull(ensureDummyModule()?.name) else found).sorted()
    ensureSampleData()
    val models = names.mapNotNull { name ->
        val (dir, manifest) = readModule(name) ?: return@mapNotNull null
        val runs = readMetricsHistory(dir).runs
        ModelInfo(
            name, manifest, readServing(name), ModelServer.stats(name), runs, MetricsImages.ensure(dir, runs),
            manifest.data?.let { DataSource.locate(it) },
            runs.lastOrNull()?.validation?.takeIf { it.endsWith(".csv", ignoreCase = true) }?.let { DataSource.locate(it) },
            sessions.filter { it.kind == KIND_MODEL && it.owner == name },
        )
    }

    val env = ScriptEnvironments.current
    val envJars = env?.libsDir?.listFiles { f -> f.extension == "jar" }?.toList() ?: emptyList()
    val pluginJars = File(getDirectory().toString(), "pluginDir").listFiles { f -> f.extension == "jar" }?.toList() ?: emptyList()
    val local = (envJars.map { it to "env" } + pluginJars.map { it to "pluginDir" }).distinctBy { it.first.name }
    if (remote) {
        ModelsCache.remoteJars = JarPackages.available()
        ModelsCache.remoteLoaded = true
    }
    val remoteJars = ModelsCache.remoteJars.filter { r -> local.none { it.first.name == r.name } }
    val page = JarPackages.downloadsPage()
    val collections = PackageCollections.all()
    fun collectionsOf(jar: String) = collections.filter { jar in it.jars }.map { it.name }
    val jars = (local.map { (file, source) ->
        val origin = JarPackages.origin(file.name)
        val contents = JarInspector.inspect(file)
        JarInfo(file.name, source, origin, origin?.url, origin?.page ?: page, origin?.version ?: contents?.implementationVersion, file, contents, collectionsOf(file.name), sessions.filter { it.kind == KIND_PACKAGE && it.owner == file.name })
    } + remoteJars.map {
        JarInfo(it.name, "server", null, JarPackages.downloadUrl(it.name), page, it.version, null, null, collectionsOf(it.name), sessions.filter { s -> s.kind == KIND_PACKAGE && s.owner == it.name })
    }).sortedBy { it.name }

    return ModelsSnapshot(models, jars, sessions, collections)
}

/* ---------- page ---------- */

/**
 * The models route: three tabs in a strip like the sandbox's, and an output strip at the bottom that shows what
 * the models, packages and sessions log. MODELS: every intent model grouped by the data it was trained on, the
 * running ones first, with its metrics as images, its data file, and the actions to configure, listen, open and
 * publish it; a new one is built from a csv with the form. PACKAGES: the package jars a script can open, in
 * collections (folders) and by where they live, each with what it contains and the Java it needs. SESSIONS: every
 * open session with its own log, each of which can be terminated here.
 */
@Composable
fun ModelsPage(navController: NavController) {
    val session by AuthState.session.collectAsState()

    Row(modifier = Modifier.fillMaxSize().background(EditorBackground)) {
        NavigationSidebar(
            selectedIndex = -1,
            onIndexSelected = { if (it == 1) navController.navigate("codingSandbox") },
            showPlay = false,
            modelsSelected = true,
            onModels = {},
        )
        ModelsContent(user = session?.username, onLogin = { navController.navigate("login") { launchSingleTop = true } })
    }
}

@Composable
private fun ModelsContent(user: String?, onLogin: () -> Unit) {
    var snapshot by remember { mutableStateOf(ModelsCache.snapshot) }
    var refresh by remember { mutableStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(PageTab.MODELS) }
    var selectedModel by remember { mutableStateOf<String?>(null) }
    var selectedJar by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val build by ModelBuildProgress.current.collectAsState()
    val validating by ModelValidation.current.collectAsState()

    // Files, sessions and servers change underneath, so the page re-reads them while it is open. The first read
    // after start-up is local only, so the models show at once; the server's jar listing follows right after.
    LaunchedEffect(refresh) {
        var withRemote = ModelsCache.remoteLoaded
        while (true) {
            snapshot = withContext(Dispatchers.IO) { runCatching { readSnapshot(withRemote) }.getOrDefault(snapshot) }
            ModelsCache.snapshot = snapshot
            if (!withRemote) { withRemote = true; continue }
            delay(1500)
        }
    }

    fun background(block: suspend () -> Unit) = scope.launch {
        withContext(Dispatchers.IO) {
            runCatching { block() }.onFailure {
                logInternal("models: ${it.message}", LogLevel.ERROR)
                notice = it.message
            }
        }
        refresh++
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PageTabs(
            tab = tab,
            counts = mapOf(PageTab.MODELS to snapshot.models.size, PageTab.PACKAGES to snapshot.jars.size, PageTab.SESSIONS to snapshot.sessions.size),
            signedIn = user != null,
            onTab = { tab = it },
            onWebsite = { openInBrowser(AmieBackend.config.siteDashboardUrl) },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            notice?.let {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Meta(it)
                    Spacer(Modifier.width(8.dp))
                    SmallButton("Dismiss") { notice = null }
                }
            }

            when (tab) {
                PageTab.MODELS -> ModelsTab(
                    snapshot = snapshot,
                    build = build,
                    validating = validating,
                    selectedName = selectedModel,
                    signedIn = user != null,
                    creating = creating || (snapshot.models.isEmpty() && build == null),
                    onCreating = { creating = it },
                    onSelect = { selectedModel = it },
                    onBuild = { name, data, test ->
                        if (build != null) notice = "a build is already running: ${build?.modelName}"
                        else background {
                            val source = DataSource.read(data.absolutePath) ?: error("${data.name} has no text and label columns the builder can use")
                            test?.let { source.relate(it.absolutePath) }
                            buildClassifier(source.samples(), name, validation = source.related?.samples(), validationName = source.related?.name, dataName = data.name)
                                ?: error("model '$name' was not built; the output below says why")
                            selectedModel = name
                            creating = false
                        }
                    },
                    onValidate = { model, data ->
                        if (validating != null) notice = "a validation is already running: ${validating?.model}"
                        else background { ModelValidation.validate(model.name, data) }
                    },
                    onSave = { model, settings -> background { writeServing(model.name, settings.copy(enabled = model.listening)) } },
                    onListen = { model, on, settings -> background { if (on) ModelServer.start(model.name, settings) else ModelServer.stop(model.name) } },
                    onOpen = { model -> background { ModelSessions.open(model.name) } },
                    onTerminate = { session -> background { session.terminate() } },
                    onPublish = { model ->
                        val signedIn = user
                        if (signedIn == null) onLogin()
                        else background {
                            val (dir, _) = readModule(model.name) ?: error("module '${model.name}' not found")
                            AmieBackend.publishModule(model.name, dir, signedIn)
                            notice = "${model.name} published to ${AmieBackend.config.base}"
                        }
                    },
                )
                PageTab.PACKAGES -> PackagesTab(
                    snapshot = snapshot,
                    selectedName = selectedJar,
                    onSelect = { selectedJar = it },
                    onAdd = {
                        val chosen = chooseFile("Add jar", listOf("jar")) ?: return@PackagesTab
                        background { addJar(chosen); selectedJar = chosen.name }
                    },
                    onFetch = { jar -> background { JarPackages.fetch(jar.name) ?: error("${jar.name} is not on the server") } },
                    onTerminate = { session -> background { session.terminate() } },
                    onCollection = { action -> background { action() } },
                )
                PageTab.SESSIONS -> SessionsTab(
                    sessions = snapshot.sessions,
                    onTerminate = { session -> background { session.terminate() } },
                    onTerminateAll = { background { ModelSessions.closeAll(); JarPackages.closeAll(); OrtSessions.cancelAll() } },
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        OutputStrip(build, validating)
    }
}

/** The strip under the window header: one tab per section with its count, and the website when signed in */
@Composable
private fun PageTabs(tab: PageTab, counts: Map<PageTab, Int>, signedIn: Boolean, onTab: (PageTab) -> Unit, onWebsite: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(TabBarBackground)) {
        Row(modifier = Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
            PageTab.entries.forEach { t -> TabCell(t.title, counts[t] ?: 0, selected = t == tab) { onTab(t) } }
            Spacer(Modifier.weight(1f))
            if (signedIn) {
                SmallButton("Open website", onClick = onWebsite)
                Spacer(Modifier.width(10.dp))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(UnifiedBorderColor))
    }
}

@Composable
private fun TabCell(title: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(IntrinsicSize.Max)
            .background(if (selected) TabActiveBackground else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(if (selected) TabActiveIndicator else Color.Transparent))
        Row(modifier = Modifier.weight(1f).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = if (selected || hovered) FontColor else MutedFontColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
            Spacer(Modifier.width(8.dp))
            Text(count.toString(), color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
        }
    }
}

/**
 * The output at the bottom of the page: what the models, packages, sessions, builds and validations log, newest
 * last, with the build or validation in progress on its header. The console in the sandbox has the same lines.
 */
@Composable
private fun OutputStrip(build: ModelBuild?, validating: ValidationProgress?) {
    val pattern = remember { Regex("(?i)model|validat|package|session|collection|metrics|module|env '|jar|sample|build|initMod|ort:|auth:|publish") }
    val lines = consoleData.filter { it.source == INTERNAL_SOURCE && (it.message ?: "").let(pattern::containsMatchIn) }.takeLast(200)
    val scroll = rememberScrollState()
    LaunchedEffect(lines.size) { scroll.scrollTo(scroll.maxValue) }
    val time = remember { DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()) }
    Column(modifier = Modifier.fillMaxWidth().height(132.dp).background(ConsoleBackground)) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ConsoleBorderColor))
        Row(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 5.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("OUTPUT", color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif, letterSpacing = 1.sp)
            Spacer(Modifier.width(12.dp))
            when {
                build != null -> { StatusDot(true); Spacer(Modifier.width(6.dp)); Meta("building ${build.modelName} · ${(build.fraction * 100).toInt()}% · ${build.describe()}") }
                validating != null -> { StatusDot(true); Spacer(Modifier.width(6.dp)); Meta("validating ${validating.model} · ${validating.done}/${validating.total}") }
            }
        }
        SelectionContainer {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scroll).padding(horizontal = 14.dp, vertical = 2.dp)) {
                if (lines.isEmpty()) Meta("nothing yet")
                lines.forEach { e ->
                    Row {
                        Text(e.time?.let { time.format(it) } ?: "", color = LogTimestampColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(e.message ?: "", color = if (e.level == LogLevel.ERROR) LogErrorTextColor else LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/* ---------- MODELS ---------- */

@Composable
private fun ModelsTab(
    snapshot: ModelsSnapshot,
    build: ModelBuild?,
    validating: ValidationProgress?,
    selectedName: String?,
    signedIn: Boolean,
    creating: Boolean,
    onCreating: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onBuild: (String, File, File?) -> Unit,
    onValidate: (ModelInfo, File) -> Unit,
    onSave: (ModelInfo, ServingSettings) -> Unit,
    onListen: (ModelInfo, Boolean, ServingSettings) -> Unit,
    onOpen: (ModelInfo) -> Unit,
    onTerminate: (SessionInfo) -> Unit,
    onPublish: (ModelInfo) -> Unit,
) {
    SectionTitle("INTENT MODELS", snapshot.models.size) {
        if (build == null) AccentButton(if (creating) "Close" else "+ New model") { onCreating(!creating) }
    }
    build?.let { BuildCard(it); Spacer(Modifier.height(8.dp)) }
    if (creating && build == null) {
        NewModelForm(onBuild = onBuild)
        Spacer(Modifier.height(8.dp))
    }

    // The list is grouped by the data the models were trained on, so the same data trained twice sits together;
    // groups with a running model come first, and inside a group the running ones, then the most accurate
    var query by remember { mutableStateOf("") }
    val shown = snapshot.models.filter { it.matches(query) }
    val ordered = shown.sortedWith(compareByDescending<ModelInfo> { it.running }.thenByDescending { it.accuracy }.thenBy { it.name })
    val selected = snapshot.models.firstOrNull { it.name == selectedName } ?: ordered.firstOrNull()
    if (snapshot.models.isNotEmpty()) Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SearchField(query) { query = it }
            if (shown.isEmpty()) Hint("no matches")
            val groups = ordered.groupBy { it.dataName }
            val groupOrder = groups.keys.sortedWith(compareByDescending<String> { groups.getValue(it).any { m -> m.running } }.thenByDescending { groups.getValue(it).maxOf { m -> m.accuracy } })
            groupOrder.forEach { data ->
                val models = groups.getValue(data)
                Fold(data, models.size, open = true, key = "data:$data") {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        models.forEach { model ->
                            ListRow(
                                name = model.name,
                                active = model.running,
                                selected = model.name == selected?.name,
                                meta = listOfNotNull(
                                    model.lastRun?.let { "accuracy ${pct(it.accuracy)}" } ?: "not validated",
                                    if (model.listening) ":${model.serving.port}" else null,
                                    when (model.sessions.size) { 0 -> null; 1 -> "1 session"; else -> "${model.sessions.size} sessions" },
                                ).joinToString(" · "),
                                status = model.status,
                                underline = model.running,
                            ) { onSelect(model.name) }
                        }
                    }
                }
            }
        }
        if (selected != null) ModelDetail(
            model = selected,
            signedIn = signedIn,
            validating = validating?.takeIf { it.model == selected.name },
            modifier = Modifier.weight(1f),
            onValidate = { data -> onValidate(selected, data) },
            onSave = { onSave(selected, it) },
            onListen = { on, settings -> onListen(selected, on, settings) },
            onOpen = { onOpen(selected) },
            onTerminate = onTerminate,
            onPublish = { onPublish(selected) },
        )
    }
}

/**
 * Builds a model from a data file: name, the csv with text and label columns, an optional test file. What the
 * file holds is read as soon as it is chosen so the labels are visible before Build; the build itself is the same
 * `buildClassifier` the sandbox's modelBuilder runs, and the BUILDING card and the output strip follow it.
 */
@Composable
private fun NewModelForm(onBuild: (String, File, File?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var data by remember { mutableStateOf<File?>(null) }
    var test by remember { mutableStateOf<File?>(null) }
    var preview by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(data) {
        preview = data?.let { f ->
            withContext(Dispatchers.IO) {
                runCatching { DataSource.read(f.absolutePath) }.getOrNull()?.let { s ->
                    val samples = s.samples()
                    "${samples.size} samples · ${s.labels.size} labels: ${s.labels.joinToString()}"
                } ?: "no text and label columns in ${f.name}"
            }
        }
    }
    val ready = name.isNotBlank() && data != null && preview?.startsWith("no ") == false

    Card {
        SubTitle("NEW MODEL")
        Spacer(Modifier.height(6.dp))
        SettingField("Name", name) { name = it.filter { c -> c.isLetterOrDigit() || c in "._-" } }
        fun pick(f: File) {
            data = f
            if (name.isBlank()) name = f.nameWithoutExtension.filter { c -> c.isLetterOrDigit() || c in "._-" }
        }
        FileField("Data", data, "csv with text and label columns", sample = { pick(ensureSampleData()) }) {
            chooseFile("Training data", listOf("csv"))?.let { pick(it) }
        }
        FileField("Test", test, "optional, else a 20% split", onClear = { test = null }) {
            chooseFile("Test data", listOf("csv"))?.let { test = it }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Meta(preview ?: "")
            Spacer(Modifier.weight(1f))
            AccentButton("Build", enabled = ready) { onBuild(name.trim(), data!!, test) }
        }
    }
}

/**
 * The selected model: name, status and actions in the header; then what matters first, the metrics as images
 * and the data it was trained and validated on (open in a viewer, or preview here); the serving settings when
 * Configure is on; labels, encoder and serving folded under DETAILS; and the sessions it owns with their logs.
 */
@Composable
private fun ModelDetail(
    model: ModelInfo,
    signedIn: Boolean,
    validating: ValidationProgress?,
    modifier: Modifier = Modifier,
    onValidate: (File) -> Unit,
    onSave: (ServingSettings) -> Unit,
    onListen: (Boolean, ServingSettings) -> Unit,
    onOpen: () -> Unit,
    onTerminate: (SessionInfo) -> Unit,
    onPublish: () -> Unit,
) {
    var configuring by remember(model.name) { mutableStateOf(false) }
    var host by remember(model.name) { mutableStateOf(model.serving.host) }
    var port by remember(model.name) { mutableStateOf(model.serving.port.toString()) }
    var path by remember(model.name) { mutableStateOf(model.serving.path.ifBlank { "/models/${model.name}" }) }
    var allowPost by remember(model.name) { mutableStateOf("POST" in model.serving.methods) }
    var allowGet by remember(model.name) { mutableStateOf("GET" in model.serving.methods) }
    val settings = ServingSettings(
        enabled = model.listening,
        host = host.trim().ifEmpty { "127.0.0.1" },
        port = port.trim().toIntOrNull()?.takeIf { it in 1..65535 } ?: model.serving.port,
        path = path.trim(),
        methods = listOfNotNull("POST".takeIf { allowPost }, "GET".takeIf { allowGet }).ifEmpty { listOf("POST") },
    )

    DetailPanel(modifier) {
        DetailHeader(
            name = model.name,
            active = model.running,
            status = listOfNotNull(
                model.status,
                if (model.listening) "listening ${model.serving.host}:${model.serving.port}" else "not serving",
                "trained on ${model.dataName}",
            ).joinToString(" · "),
        ) {
            SmallButton("Configure") { configuring = !configuring }
            SmallButton(if (model.listening) "Stop" else "Listen") { onListen(!model.listening, settings) }
            AccentButton("Open session", onClick = onOpen)
            SmallButton(if (signedIn) "Publish" else "Publish (sign in)", onClick = onPublish)
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (configuring) {
                SettingField("Host", host) { host = it }
                SettingField("Port", port) { port = it.filter(Char::isDigit).take(5) }
                SettingField("Path", path) { path = it }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text("Methods", color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.width(56.dp))
                    Toggle("POST", allowPost) { allowPost = it }
                    Spacer(Modifier.width(6.dp))
                    Toggle("GET", allowGet) { allowGet = it }
                    Spacer(Modifier.weight(1f))
                    SmallButton("Save") { onSave(settings) }
                }
                KeyValues(
                    *listOfNotNull(
                        if (allowPost) "POST" to settings.url(model.name) else null,
                        if (allowGet) "GET" to "${settings.url(model.name)}?text=…" else null,
                    ).toTypedArray()
                )
                if (model.listening) Hint("applies after Stop and Listen")
            }

            MetricsBlock(model.metrics, model.plots, validating, onValidate)

            DataBlock(model)

            Fold("DETAILS", null, open = false, key = "${model.name}:details") {
                KeyValues(
                    "labels" to model.manifest.labels.joinToString().ifEmpty { "—" },
                    "encoder" to File(model.manifest.encoder).name,
                    "files" to "AMP/modules/${model.name}/",
                    *listOfNotNull(model.stats?.let { s -> "serving" to "${s.methods.joinToString("/")} ${s.url} · ${s.requests} requests, ${s.errors} errors" }).toTypedArray(),
                )
            }

            SessionsOf(model.sessions, onTerminate)
        }
    }
}

/**
 * The metrics as images drawn by MetricsImages when the metrics change: the summary (gauges and what the run
 * says), F1 per label, the confusion matrix and, with more than one run, the trend. Validate adds a run.
 */
@Composable
private fun MetricsBlock(runs: List<MetricsRun>, plots: List<MetricsPlot>, validating: ValidationProgress?, onValidate: (File) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SubTitle("METRICS")
        Spacer(Modifier.weight(1f))
        if (validating != null) Meta("validating ${validating.done}/${validating.total}")
        else {
            AccentButton("Validate on sample data") { onValidate(ensureSampleData()) }
            Spacer(Modifier.width(6.dp))
            AccentButton("Validate on file") { chooseFile("Validation data", listOf("csv"))?.let(onValidate) }
        }
    }
    val run = runs.lastOrNull()
    if (run == null) {
        Hint("no validation yet")
        return
    }
    plots.firstOrNull { it.file.name == "summary.png" }?.let { PlotImage(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        plots.filter { it.file.name == "labels.png" || it.file.name == "confusion.png" }.forEach { PlotImage(it) }
    }
    plots.firstOrNull { it.file.name == "history.png" }?.let { PlotImage(it) }
    if (runs.size > 1) {
        Fold("RUNS", runs.size, open = false, key = "runs") {
            KeyValues(*runs.mapIndexed { i, r -> "${i + 1}" to "acc ${fmt(r.accuracy)}  F1 ${fmt(r.macroF1)}  ${r.validation}" }.toTypedArray())
        }
    }
    run.warnings.forEach { Hint("warning: $it") }
}

/** The csv files behind a model, the training data and the last validation set: open in a viewer, or preview here */
@Composable
private fun DataBlock(model: ModelInfo) {
    val files = listOfNotNull(model.dataFile?.let { "trained on" to it }, model.validationFile?.takeIf { it != model.dataFile }?.let { "validated on" to it })
    SubTitle("DATA")
    if (files.isEmpty()) {
        Hint(if (model.manifest.data != null) "${model.manifest.data} is not in AMP/ any more" else "no data file recorded")
        return
    }
    files.forEach { (role, file) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(role, color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(92.dp))
            Text(file.name, color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            AccentButton("Open in viewer") { openFile(file) }
        }
        Fold("preview ${file.name}", null, open = false, key = "${model.name}:${file.path}") { CsvPreview(file) }
    }
}

/** The first rows of a csv as a monospace table */
@Composable
private fun CsvPreview(file: File) {
    val table = remember(file.path, file.lastModified()) {
        runCatching { parseCsv(file.readText()) }.getOrDefault(emptyList())
    }
    if (table.isEmpty()) { Hint("empty or unreadable"); return }
    val widths = (0 until (table.maxOfOrNull { it.size } ?: 0)).map { c -> table.take(21).maxOf { row -> (row.getOrNull(c) ?: "").length }.coerceIn(4, 40) }
    fun line(row: List<String>) = row.mapIndexed { c, v -> v.replace('\n', ' ').take(widths[c]).padEnd(widths[c]) }.joinToString("  ")
    SelectionContainer {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(CodeBackground).border(1.dp, ConsoleBorderColor, RoundedCornerShape(6.dp)).padding(8.dp)) {
            Text(line(table.first()), color = FontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, maxLines = 1)
            table.drop(1).take(20).forEach { Text(line(it), color = LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, maxLines = 1) }
            if (table.size > 21) Meta("… ${table.size - 21} more rows, Open in viewer for all")
        }
    }
}

/** A model while modelBuilder builds it: phase, step and a progress bar */
@Composable
private fun BuildCard(build: ModelBuild) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(true)
            Spacer(Modifier.width(8.dp))
            Text("building ${build.modelName}", color = FontColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Meta("${(build.fraction * 100).toInt()}%")
        }
        Spacer(Modifier.height(4.dp))
        Meta(build.describe())
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = build.fraction,
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = ActiveColor,
            backgroundColor = PanelBorder,
        )
    }
}

/** One rendered plot file, decoded once per file version and shown at the size it was drawn for */
@Composable
private fun PlotImage(plot: MetricsPlot) {
    val stamp = plot.file.lastModified()
    val bitmap = remember(plot.file.path, stamp) {
        runCatching { plot.file.inputStream().use { loadImageBitmap(it) } }.getOrNull()
    }
    if (bitmap == null) {
        Hint("${plot.title}: not drawn yet")
        return
    }
    Image(
        bitmap = bitmap,
        contentDescription = plot.title,
        modifier = Modifier.width(plot.width.dp).height(plot.height.dp).clip(RoundedCornerShape(8.dp))
    )
}

/* ---------- PACKAGES ---------- */

@Composable
private fun PackagesTab(
    snapshot: ModelsSnapshot,
    selectedName: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onFetch: (JarInfo) -> Unit,
    onTerminate: (SessionInfo) -> Unit,
    /** Runs a change to the collections in the background */
    onCollection: (suspend () -> Unit) -> Unit,
) {
    var newCollection by remember { mutableStateOf(false) }
    SectionTitle("PACKAGES", snapshot.jars.size) {
        AccentButton(if (newCollection) "Close" else "+ New collection") { newCollection = !newCollection }
        Spacer(Modifier.width(6.dp))
        AccentButton("+ Add jar", onClick = onAdd)
    }
    if (newCollection) {
        NewCollectionForm(snapshot.jars, onCreate = { name, jars -> onCollection { PackageCollections.create(name, jars) }; newCollection = false })
        Spacer(Modifier.height(8.dp))
    }
    if (snapshot.jars.isEmpty()) {
        Hint("no packages: add a jar, put one in AMP/pluginDir, or upload one on the website")
        return
    }
    // The list is searched by name, Java, collection, class or resource. Collections come first as folders a jar
    // can be added to or detached from; then every jar by where it lives
    var query by remember { mutableStateOf("") }
    val shown = snapshot.jars.filter { it.matches(query) }
    val selected = snapshot.jars.firstOrNull { it.name == selectedName } ?: shown.firstOrNull()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SearchField(query) { query = it }
            if (shown.isEmpty()) Hint("no matches")
            snapshot.collections.forEach { collection ->
                val members = shown.filter { it.name in collection.jars }
                if (members.isEmpty() && query.isNotBlank()) return@forEach
                CollectionFolder(collection, members, snapshot.jars, selected?.name, onSelect, onCollection)
            }
            val groups = shown.groupBy { it.location }
            listOf("In AMP/pluginDir", "In the env", "On the server").forEach { location ->
                val jars = groups[location] ?: return@forEach
                Fold(location, jars.size, open = location != "On the server" || query.isNotBlank() || jars.any { it.name == selected?.name }, key = location) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        jars.forEach { jar -> JarRow(jar, selected = jar.name == selected?.name, onSelect = { onSelect(jar.name) }) }
                    }
                }
            }
        }
        if (selected != null) JarDetail(selected, Modifier.weight(1f), onFetch = { onFetch(selected) }, onTerminate = onTerminate)
    }
}

@Composable
private fun JarRow(jar: JarInfo, selected: Boolean, onDetach: (() -> Unit)? = null, onSelect: () -> Unit) {
    ListRow(
        name = jar.name,
        active = jar.open > 0,
        selected = selected,
        meta = listOfNotNull(jar.contents?.minJava?.let { "java $it+" }, jar.version?.let { "v$it" }).joinToString(" · "),
        status = jar.status,
        underline = jar.open > 0,
        onDetach = onDetach,
        onSelect = onSelect,
    )
}

/**
 * A collection as a folder: its jars as rows, Detach appearing on the row under the pointer, and Add offering
 * the jars that are not in it yet. Delete removes the folder, the jars stay.
 */
@Composable
private fun CollectionFolder(
    collection: PackageCollection,
    members: List<JarInfo>,
    all: List<JarInfo>,
    selectedName: String?,
    onSelect: (String) -> Unit,
    onCollection: (suspend () -> Unit) -> Unit,
) {
    var adding by remember(collection.name) { mutableStateOf(false) }
    Fold(collection.name, collection.jars.size, open = true, key = "collection:${collection.name}") {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 2.dp)) {
                AccentButton(if (adding) "Close" else "+ Add") { adding = !adding }
                SmallButton("Delete folder") { onCollection { PackageCollections.delete(collection.name) } }
            }
            if (adding) {
                val candidates = all.filter { it.name !in collection.jars }
                if (candidates.isEmpty()) Hint("every jar is in it")
                candidates.forEach { jar ->
                    SmallButton("+ ${jar.name}") { onCollection { PackageCollections.add(collection.name, jar.name) } }
                }
                Spacer(Modifier.height(2.dp))
            }
            if (members.isEmpty()) Hint("empty")
            members.forEach { jar ->
                JarRow(jar, selected = jar.name == selectedName, onDetach = { onCollection { PackageCollections.remove(collection.name, jar.name) } }) { onSelect(jar.name) }
            }
        }
    }
}

/** A new collection: a name, the jars to put in it, and a shortcut per Java version that picks every jar it fits */
@Composable
private fun NewCollectionForm(jars: List<JarInfo>, onCreate: (String, List<String>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(setOf<String>()) }
    val versions = jars.mapNotNull { it.contents?.minJava }.distinct().sorted()
    Card {
        SubTitle("NEW COLLECTION")
        Spacer(Modifier.height(6.dp))
        SettingField("Name", name) { name = it }
        if (versions.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
            Text("By Java", color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.width(56.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                versions.forEach { v ->
                    // Every jar that runs on Java v: the ones that need v or older
                    SmallButton("jvm$v") {
                        name = "jvm$v"
                        chosen = jars.filter { (it.contents?.minJava ?: Int.MAX_VALUE) <= v }.map { it.name }.toSet()
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
            Text("Jars", color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.width(56.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                jars.forEach { jar -> Toggle(jar.name, jar.name in chosen) { on -> chosen = if (on) chosen + jar.name else chosen - jar.name } }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Meta("${chosen.size} jars")
            Spacer(Modifier.weight(1f))
            AccentButton("Create", enabled = name.isNotBlank() && chosen.isNotEmpty()) { onCreate(name.trim(), chosen.toList()) }
        }
    }
}

/**
 * The selected package: what it is (status, Java it needs, language), the collections it is in, where it came
 * from, what built it, what it contains, and its sessions. A script opens it with initMod("name.jar").
 */
@Composable
private fun JarDetail(jar: JarInfo, modifier: Modifier = Modifier, onFetch: () -> Unit, onTerminate: (SessionInfo) -> Unit) {
    val contents = jar.contents
    DetailPanel(modifier) {
        DetailHeader(
            name = jar.name,
            active = jar.open > 0,
            status = listOfNotNull(
                jar.status,
                contents?.let { it.minJava?.let { v -> "java $v+" } ?: "any java" },
                contents?.let { if (it.kotlin) "kotlin" else "java" },
                jar.version?.let { "v$it" },
                jar.collections.takeIf { it.isNotEmpty() }?.let { "in ${it.joinToString()}" },
            ).joinToString(" · "),
        ) {
            SmallButton("Open page") { openInBrowser(jar.page) }
            if (!jar.local) AccentButton("Fetch", onClick = onFetch)
            jar.file?.let { f -> SmallButton("Reveal file") { openInBrowser(f.parentFile.toURI().toString()) } }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyValues("opens with" to "initMod(\"${jar.name}\"), a fresh instance per call")

            Fold("SOURCE", null, open = false, key = "${jar.name}:source") { KeyValues(
                "where" to when (jar.source) {
                    "server" -> "on the server; fetched into AMP/pluginDir on the first open"
                    else -> if (jar.origin != null) "AMP/${if (jar.source == "env") "env/<env>/libs" else "pluginDir"}, downloaded from the server" else "AMP/${if (jar.source == "env") "env/<env>/libs" else "pluginDir"}, added on this machine"
                },
                "page" to jar.page,
                "download" to (jar.url ?: "none, this jar was added on this machine"),
                *listOfNotNull(
                    jar.origin?.let { o -> "fetched" to "${o.fetchedAt.take(19).replace('T', ' ')}${o.lastModified?.let { " · published $it" } ?: ""}" },
                    jar.file?.let { "file" to it.absolutePath },
                ).toTypedArray(),
            ) }

            if (contents == null) {
                Fold("CONTENTS", null, open = true, key = "${jar.name}:none") {
                    Hint(if (jar.local) "not readable as a jar" else "not fetched yet")
                }
            } else {
                Fold("BUILD", null, open = false, key = "${jar.name}:build") { KeyValues(
                    "built by" to (contents.createdBy ?: "unknown (no Created-By in the manifest)"),
                    *listOfNotNull(contents.buildJdk?.let { "build jdk" to it }).toTypedArray(),
                    "needs" to (contents.classVersion?.let { "${contents.javaSupport} (class files version $it)" } ?: "any Java (no class files)"),
                    "this app" to "Java ${contents.hostJava}, ${if (contents.runsHere) "can load it" else "too old for it"}",
                    "language" to if (contents.kotlin) "Kotlin" else "Java",
                    "size" to "${contents.size / 1024} KB, ${contents.entries} entries",
                    *listOfNotNull(contents.implementationTitle?.let { "title" to it }).toTypedArray(),
                ) }

                Fold("CONTENTS", contents.classes.size + contents.resources.size, open = false, key = "${jar.name}:contents") {
                    ContentsBrowser(contents)
                }
            }

            SessionsOf(jar.sessions, onTerminate)
        }
    }
}

/* ---------- SESSIONS ---------- */

@Composable
private fun SessionsTab(sessions: List<SessionInfo>, onTerminate: (SessionInfo) -> Unit, onTerminateAll: () -> Unit) {
    SectionTitle("SESSIONS", sessions.size) { if (sessions.isNotEmpty()) SmallButton("Terminate all", onClick = onTerminateAll) }
    if (sessions.isEmpty()) {
        Hint("no open sessions")
        return
    }
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val shown = sessions.filter { q.isEmpty() || it.owner.lowercase().contains(q) || it.kind.lowercase().contains(q) || "#${it.id}".contains(q) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (sessions.size > 4) SearchField(query) { query = it }
        if (shown.isEmpty()) Hint("no matches")
        SessionList(shown, showOwner = true, onTerminate)
    }
}

/** The sessions one owner has open, inside its detail panel */
@Composable
private fun SessionsOf(sessions: List<SessionInfo>, onTerminate: (SessionInfo) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        SubTitle("SESSIONS  ${sessions.size}")
        Spacer(Modifier.weight(1f))
        if (sessions.size > 1) SmallButton("Terminate all") { sessions.forEach(onTerminate) }
    }
    if (sessions.isEmpty()) Hint("none open")
    else SessionList(sessions, showOwner = false, onTerminate)
}

/** Sessions as flat rows with a divider between them, each with its Log toggle and Terminate */
@Composable
private fun SessionList(sessions: List<SessionInfo>, showOwner: Boolean, onTerminate: (SessionInfo) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        sessions.forEachIndexed { i, s ->
            if (i > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PanelBorder))
            SessionRow(s, showOwner, onTerminate = { onTerminate(s) })
        }
    }
}

/** One open session: owner and id, what it is doing, a Log toggle that shows what it was asked, and Terminate */
@Composable
private fun SessionRow(session: SessionInfo, showOwner: Boolean, onTerminate: () -> Unit) {
    var showLog by remember(session.key) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(session.running)
            Spacer(Modifier.width(8.dp))
            Text(if (showOwner) "${session.owner}  #${session.id}" else "#${session.id}", color = FontColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(10.dp))
            Meta(listOfNotNull(if (showOwner) session.kind else null, if (session.running) "inferring" else "idle", "${session.runs} runs", uptime(Duration.between(session.openedAt, Instant.now()))).joinToString(" · "))
            Spacer(Modifier.weight(1f))
            if (session.hasLog) {
                SmallButton(if (showLog) "Hide log" else "Log ${session.log.size}") { showLog = !showLog }
                Spacer(Modifier.width(6.dp))
            }
            SmallButton("Terminate", onClick = onTerminate)
        }
        if (showLog) {
            Spacer(Modifier.height(6.dp))
            LogBox(session.log)
        }
    }
}

/** The last lines of a session's log, console style: time, kind and text */
@Composable
private fun LogBox(entries: List<SessionLogEntry>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CodeBackground)
            .border(1.dp, ConsoleBorderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (entries.isEmpty()) Meta("nothing yet")
        val shown = entries.takeLast(60)
        if (shown.size < entries.size) Meta("… ${entries.size - shown.size} earlier lines in the sessions.log file")
        SelectionContainer {
            Column {
                shown.forEach { e ->
                    Row {
                        Text(e.time, color = LogTimestampColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(e.kind.padEnd(5), color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(e.text, color = LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

/* ---------- actions ---------- */

// Copies the jar into AMP/pluginDir and imports it into the env so a script can open it with initMod("name.jar")
private fun addJar(file: File) {
    val pluginDir = File(getDirectory().toString(), "pluginDir").apply { mkdirs() }
    val target = File(pluginDir, file.name)
    if (file.canonicalPath != target.canonicalPath) file.copyTo(target, overwrite = true)
    val env = ScriptEnvironments.current ?: ScriptEnvironments.open(DEFAULT_ENV)
    env.import(file.name)
}

private fun chooseFile(title: String, extensions: List<String>): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD).apply {
        setFilenameFilter { _, name -> extensions.any { name.endsWith(".$it", ignoreCase = true) } }
        isVisible = true
    }
    return dialog.file?.let { File(dialog.directory, it) }?.takeIf { f -> extensions.any { f.extension.equals(it, ignoreCase = true) } }
}

private fun openInBrowser(url: String) {
    runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) }
        .onFailure { logInternal("models: couldn't open $url: ${it.message}", LogLevel.ERROR) }
}

/** Opens the file in whatever the system uses for it, e.g. a csv in a spreadsheet or text viewer */
private fun openFile(file: File) {
    runCatching { java.awt.Desktop.getDesktop().open(file) }
        .onFailure { logInternal("models: couldn't open ${file.name}: ${it.message}", LogLevel.ERROR) }
}

private fun fmt(value: Double) = String.format(Locale.US, "%.3f", value)
private fun pct(value: Double) = "${(value * 100).toInt()}%"

/* ---------- pieces ---------- */

/**
 * One line of a list: name, a short meta line and the status at the right; click to select. A running one is
 * underlined in green; a stopped one is not. With onDetach, a Detach button appears while the pointer is over it.
 */
@Composable
private fun ListRow(name: String, active: Boolean, selected: Boolean, meta: String, status: String, underline: Boolean = false, onDetach: (() -> Unit)? = null, onSelect: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(when { selected -> PanelSurfaceSelected; hovered -> PanelSurface; else -> Color.Transparent })
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onSelect() }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = FontColor, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontFamily = FontFamily.SansSerif, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (meta.isNotEmpty()) Meta(meta)
            }
            Spacer(Modifier.width(6.dp))
            if (hovered && onDetach != null) {
                SmallButton("Detach", onClick = onDetach)
            } else {
                Text(status, color = if (active) FontColor else MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(2.dp).background(if (underline) SuccessColor else Color.Transparent))
    }
}

/** The bordered panel of the selected item */
@Composable
private fun DetailPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PanelSurface)
            .border(1.dp, PanelBorder, RoundedCornerShape(8.dp))
    ) { content() }
}

/** Name and one status line on the left of a detail panel's header, the actions on the right */
@Composable
private fun DetailHeader(name: String, active: Boolean, status: String, actions: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatusDot(active)
        Spacer(Modifier.width(2.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = FontColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Meta(status)
        }
        actions()
    }
    Box(modifier = Modifier.fillMaxWidth().height(if (active) 2.dp else 1.dp).background(if (active) SuccessColor else PanelBorder))
    Spacer(Modifier.height(4.dp))
}

/** A small bordered label; strong is white text, the rest gray */
@Composable
private fun BadgeLabel(text: String, strong: Boolean = false) {
    Text(
        text = text,
        color = if (strong) FontColor else MutedFontColor,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, if (strong) PanelBorderSelected else PanelBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}

/** Aligned key: value lines in monospace */
@Composable
private fun KeyValues(vararg pairs: Pair<String, String>) {
    if (pairs.isEmpty()) return
    val width = pairs.maxOf { it.first.length }
    SelectionContainer {
        Column {
            pairs.forEach { (key, value) ->
                Row {
                    Text(key.padEnd(width), color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(value, color = LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                }
            }
        }
    }
}

/** A section that opens on click: chevron, title and count in the header, the content under it */
@Composable
private fun Fold(title: String, count: Int?, open: Boolean, key: Any, content: @Composable () -> Unit) {
    var expanded by remember(key) { mutableStateOf(open) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).clickable { expanded = !expanded }.padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MutedFontColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(title, color = FontColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                Text(count.toString(), color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
        if (expanded) Column(modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 2.dp, bottom = 4.dp)) { content() }
    }
}

/** A one-line filter over a list: an icon, the text, and a clear button once there is text */
@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CodeBackground, RoundedCornerShape(6.dp))
            .border(1.dp, if (focused || value.isNotEmpty()) PageAccent else PanelBorderSelected, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = if (focused) PageAccent else MutedFontColor, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = FontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(FontColor),
            modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused }
        )
        if (value.isNotEmpty()) SmallButton("×") { onChange("") }
    }
}

/**
 * The classes and resources of a jar: a filter on top, classes grouped by package and resources by folder, each
 * group folded until opened (or while a filter matches inside it). The class a session opens on and the
 * Main-Class are marked. Clicking a line copies its full name, for a script or a build.
 */
@Composable
private fun ContentsBrowser(contents: JarContents) {
    val clipboard = LocalClipboardManager.current
    var query by remember(contents.name) { mutableStateOf("") }
    var copied by remember(contents.name) { mutableStateOf<String?>(null) }
    LaunchedEffect(copied) { if (copied != null) { delay(1500); copied = null } }
    val q = query.trim().lowercase()
    val filtering = q.isNotEmpty()
    val classes = contents.classes.filter { !filtering || it.lowercase().contains(q) }
    val resources = contents.resources.filter { !filtering || it.lowercase().contains(q) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        KeyValues(
            "opens on" to (contents.entryClass ?: "no class with run(String) and close(): initMod will refuse it"),
            *listOfNotNull(contents.mainClass?.let { "main class" to it }).toTypedArray(),
        )
        SearchField(query) { query = it }
        copied?.let { Meta("copied $it") }
        if (filtering && classes.isEmpty() && resources.isEmpty()) Hint("no matches")

        fun copy(name: String) {
            clipboard.setText(AnnotatedString(name))
            copied = name
        }

        if (classes.isNotEmpty()) Fold("CLASSES", classes.size, open = filtering || classes.size <= 12, key = "classes:$filtering") {
            val byPackage = classes.groupBy { it.substringBeforeLast('.', "") }
            byPackage.toSortedMap().forEach { (pkg, names) ->
                Fold(pkg.ifEmpty { "(default package)" }, names.size, open = filtering || byPackage.size == 1, key = "pkg:$pkg:$filtering") {
                    names.forEach { fqn ->
                        ContentLine(
                            name = fqn.substringAfterLast('.'),
                            mark = when (fqn) {
                                contents.entryClass -> "opens on"
                                contents.mainClass -> "main"
                                else -> null
                            },
                        ) { copy(fqn) }
                    }
                }
            }
        }
        if (resources.isNotEmpty()) Fold("RESOURCES", resources.size, open = filtering || resources.size <= 12, key = "resources:$filtering") {
            val byDir = resources.groupBy { it.substringBeforeLast('/', "") }
            byDir.toSortedMap().forEach { (dir, names) ->
                Fold(dir.ifEmpty { "(root)" }, names.size, open = filtering || byDir.size == 1, key = "dir:$dir:$filtering") {
                    names.forEach { path -> ContentLine(path.substringAfterLast('/'), null) { copy(path) } }
                }
            }
        }
    }
}

/** One class or resource: its short name and a mark when it is special; the hover says it can be copied */
@Composable
private fun ContentLine(name: String, mark: String?, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (hovered) PanelSurfaceSelected else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        mark?.let { Spacer(Modifier.width(8.dp)); BadgeLabel(it, strong = true) }
    }
}

/** A file picker line: label, the chosen file or a hint, Choose and an optional Clear */
@Composable
private fun FileField(label: String, file: File?, hint: String, onClear: (() -> Unit)? = null, sample: (() -> Unit)? = null, onChoose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.width(56.dp))
        Text(
            file?.absolutePath ?: hint,
            color = if (file != null) FontColor else MutedFontColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .weight(1f, fill = false)
                .background(CodeBackground, RoundedCornerShape(6.dp))
                .border(1.dp, ConsoleBorderColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        )
        Spacer(Modifier.width(6.dp))
        SmallButton("Choose", onClick = onChoose)
        if (sample != null) {
            Spacer(Modifier.width(6.dp))
            AccentButton("Sample data", onClick = sample)
        }
        if (file != null && onClear != null) {
            Spacer(Modifier.width(6.dp))
            SmallButton("Clear", onClick = onClear)
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int?, action: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (count == null) title else "$title  $count", color = FontColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, modifier = Modifier.weight(1f))
        action()
    }
}

@Composable
private fun SubTitle(title: String) {
    Text(title, color = FontColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 2.dp))
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelSurface)
            .border(1.dp, PanelBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) { content() }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = MutedFontColor, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 1.dp))
}

@Composable
private fun Meta(text: String) {
    Text(text, color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (active) SuccessColor else IdleColor))
}

@Composable
private fun SettingField(label: String, value: String, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.width(56.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(FontColor),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .background(CodeBackground, RoundedCornerShape(6.dp))
                .border(1.dp, ConsoleBorderColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Text(
        label,
        color = if (on) FontColor else MutedFontColor,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (on) PageAccent.copy(alpha = 0.35f) else CodeBackground)
            .border(1.dp, if (on) PageAccent else ConsoleBorderColor, RoundedCornerShape(6.dp))
            .clickable { onChange(!on) }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** The accent chip for the actions that create, build, validate, open or add: a dark blue that stays quiet */
@Composable
private fun AccentButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Text(
        label,
        color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(when { !enabled -> PageAccent.copy(alpha = 0.35f); hovered -> AccentButtonHover; else -> PageAccent })
            .hoverable(interactionSource)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** A gray chip like the editor's tabs; it brightens on hover instead of carrying a colour of its own */
@Composable
private fun SmallButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Text(
        label,
        color = if (enabled) FontColor else MutedFontColor.copy(alpha = 0.5f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled && hovered) ChipHoverBackground else ChipBackground)
            .border(1.dp, if (enabled) ChipBorderColor else PanelBorder, RoundedCornerShape(6.dp))
            .hoverable(interactionSource)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}
