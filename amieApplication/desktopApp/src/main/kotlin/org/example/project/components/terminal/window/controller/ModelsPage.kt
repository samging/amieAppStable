package org.example.project.components.terminal.window.controller

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
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
import org.example.project.components.terminal.integrations.amiePilot.LoadedEncoder
import org.example.project.components.terminal.integrations.amiePilot.SharedEncoders
import org.example.project.components.terminal.window.controller.envBuilder.DEFAULT_ENV
import org.example.project.components.terminal.window.controller.envBuilder.ScriptEnvironments
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.ModelBuild
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.ModelBuildProgress
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.buildClassifier
import org.example.project.components.terminal.window.controller.modelLoaders.OrtSessions
import org.example.project.components.terminal.window.controller.modules.MetricsImages
import org.example.project.components.terminal.window.controller.modules.MetricsPlot
import org.example.project.components.terminal.window.controller.modules.MetricsRun
import org.example.project.components.terminal.window.controller.modules.LoggedRun
import org.example.project.components.terminal.window.controller.modules.ModelRequirements
import org.example.project.components.terminal.window.controller.modules.Requirement
import org.example.project.components.terminal.window.controller.modules.SessionRuns
import org.example.project.components.terminal.window.controller.modules.SystemStats
import org.example.project.components.terminal.window.controller.modules.bytes
import org.example.project.components.terminal.window.controller.modules.ModelServer
import org.example.project.components.terminal.window.controller.modules.ModelSessions
import org.example.project.components.terminal.window.controller.modules.ModelValidation
import org.example.project.components.terminal.window.controller.modules.ModuleManifest
import org.example.project.components.terminal.window.controller.modules.PackageCollection
import org.example.project.components.terminal.window.controller.modules.PackageCollections
import org.example.project.components.terminal.window.controller.modules.ServingSettings
import org.example.project.components.terminal.window.controller.modules.SessionLogEntry
import org.example.project.components.terminal.window.controller.modules.ValidationProgress
import org.example.project.components.terminal.window.controller.modules.deleteModule
import org.example.project.components.terminal.window.controller.modules.ensureDummyModule
import org.example.project.components.terminal.window.controller.modules.ensureSampleData
import org.example.project.components.terminal.window.controller.modules.defaultGuide
import org.example.project.components.terminal.window.controller.modules.guideFile
import org.example.project.components.terminal.window.controller.modules.modulesDir
import org.example.project.components.terminal.window.controller.modules.readMetricsHistory
import org.example.project.components.terminal.window.controller.modules.readModule
import org.example.project.components.terminal.window.controller.modules.readRegistry
import org.example.project.components.terminal.window.controller.modules.readServing
import org.example.project.components.terminal.window.controller.modules.updateModule
import org.example.project.components.terminal.window.controller.modules.uptime
import org.example.project.components.terminal.window.controller.modules.writeServing
import org.example.project.components.terminal.window.controller.pluginEngine.JarContents
import org.example.project.components.terminal.window.controller.pluginEngine.JarInspector
import org.example.project.components.terminal.window.controller.pluginEngine.JarOrigin
import org.example.project.components.terminal.window.controller.pluginEngine.JarPackages
import org.example.project.components.terminal.window.controller.pluginEngine.ServerJar
import org.example.project.components.terminal.window.controller.sourceFile.DataSource
import org.example.project.components.terminal.window.controller.sourceFile.inferStructure
import org.example.project.components.terminal.window.controller.sourceFile.parseCsv
import org.example.project.components.ui.viewport.NavigationSidebar
import org.example.project.getDirectory
import org.example.project.ui.theme.AccentButtonHover
import org.example.project.ui.theme.ActiveColor
import org.example.project.ui.theme.ChartAccent
import org.example.project.ui.theme.DangerButton
import org.example.project.ui.theme.DangerButtonBorder
import org.example.project.ui.theme.DangerButtonHover
import org.example.project.ui.theme.DangerColor
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
import org.example.project.ui.theme.PanelDividerColor
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

private enum class PageTab(val title: String, val icon: ImageVector) { MODELS("Models", Icons.Default.Memory), PACKAGES("Packages", Icons.Default.Inventory2), SESSIONS("Sessions", Icons.Default.Sensors), GUIDE("Help", Icons.Default.HelpOutline) }

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
    /** AMP/modules/<name>/: the manifest, head, metrics, serving settings and logs */
    val dir: File,
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
    /** The width of the list column, dragged at the rule between it and the detail panel */
    @Volatile var listWidth = 260f
    /** The height of the console box in the model's Console tab, dragged at its lower edge */
    @Volatile var consoleHeight = 260f
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
    // The dummy is written on a fresh checkout and topped up (description, guide) on an old one, so it's always current
    val dummy = ensureDummyModule()?.name
    val names = (if (found.isEmpty()) listOfNotNull(dummy) else found).sorted()
    ensureSampleData()
    val models = names.mapNotNull { name ->
        val (dir, manifest) = readModule(name) ?: return@mapNotNull null
        val runs = readMetricsHistory(dir).runs
        ModelInfo(
            name, dir, manifest, readServing(name), ModelServer.stats(name), runs, MetricsImages.ensure(dir, runs),
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
 * The models route: three tabs in a strip like the sandbox's. MODELS: every intent model grouped by the data it
 * was trained on, the running ones first, and the selected one in a panel with its own tabs: metrics as charts,
 * the data it was trained, validated and run on, what it needs to run, the console lines about it, and its
 * sessions; a new one is built from a csv with the form. PACKAGES: the package jars a script can open, in
 * collections (folders) and by where they live, each with what it contains and the Java it needs. SESSIONS: every
 * open session with its own log, each of which can be terminated here.
 */
@Composable
fun ModelsPage(navController: NavController) {
    val session by AuthState.session.collectAsState()

    CompositionLocalProvider(LocalContextMenuRepresentation provides DarkDefaultContextMenuRepresentation) {
    Row(modifier = Modifier.fillMaxSize().background(EditorBackground)) {
        NavigationSidebar(
            selectedIndex = -1,
            onIndexSelected = { if (it == 1) navController.navigate("codingSandbox") },
            showPlay = false,
            modelsSelected = true,
            onModels = {},
            settingsSelected = false,
            onSettings = { navController.navigate("settings") },
        )
        ModelsContent(user = session?.username, onLogin = { navController.navigate("login") { launchSingleTop = true } })
    }
    }
}

@Composable
private fun ModelsContent(user: String?, onLogin: () -> Unit) {
    var snapshot by remember { mutableStateOf(ModelsCache.snapshot) }
    var refresh by remember { mutableStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }
    var destroying by remember { mutableStateOf<String?>(null) }
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
            onWebsite = { openInBrowser(AmieBackend.config.siteDashboardUrl(user)) },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            notice?.let {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Meta(it)
                    Spacer(Modifier.width(8.dp))
                    SmallButton("Dismiss") { notice = null }
                }
            }
            destroying?.let { name ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Meta("destroy $name? its folder AMP/modules/$name, every run and its logs go; a session or listener on it is closed first")
                    Spacer(Modifier.width(8.dp))
                    DangerButton("Destroy $name", icon = Icons.Default.Delete) {
                        destroying = null
                        if (selectedModel == name) selectedModel = null
                        background { if (!deleteModule(name)) error("$name could not be removed entirely; see the console") }
                    }
                    Spacer(Modifier.width(6.dp))
                    SmallButton("Keep") { destroying = null }
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
                    onBuild = { name, data, test, description ->
                        if (build != null) notice = "a build is already running: ${build?.modelName}"
                        else background {
                            val source = DataSource.read(data.absolutePath) ?: error("${data.name} has no text and label columns the builder can use")
                            test?.let { source.relate(it.absolutePath) }
                            buildClassifier(source.samples(), name, validation = source.related?.samples(), validationName = source.related?.name, dataName = data.name, description = description)
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
                    onDescribe = { model, text -> background { updateModule(model.name) { it.copy(description = text.trim().ifEmpty { null }) } } },
                    onListen = { model, on, settings -> background { if (on) ModelServer.start(model.name, settings) else ModelServer.stop(model.name) } },
                    onOpen = { model -> background { ModelSessions.open(model.name) } },
                    onTerminate = { session -> background { session.terminate() } },
                    onTerminateModel = { model -> background { ModelServer.stop(model.name); model.sessions.forEach { it.terminate() } } },
                    onDestroy = { model -> destroying = model.name },
                    onUnpublish = { model ->
                        val signedIn = user
                        if (signedIn == null) onLogin()
                        else background {
                            val removed = AmieBackend.unpublishModule(model.name, signedIn)
                            notice = if (removed > 0) "${model.name} removed from ${AmieBackend.config.base}" else "${model.name} was not on ${AmieBackend.config.base}"
                        }
                    },
                    onPublish = { model ->
                        val signedIn = user
                        if (signedIn == null) onLogin()
                        else background {
                            val (dir, _) = readModule(model.name) ?: error("module '${model.name}' not found")
                            val published = AmieBackend.publishModule(model.name, dir, signedIn)
                            notice = when (published.onWebsite) {
                                true -> "${model.name} published (${published.files.joinToString(", ")}); the website lists it at ${published.dashboardUrl}"
                                false -> "${model.name} published to ${AmieBackend.config.base}, but the website does not list it yet; see ${published.dashboardUrl}"
                                null -> "${model.name} published to ${AmieBackend.config.base}; the website at ${AmieBackend.config.site} could not be asked, see ${published.dashboardUrl}"
                            }
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
                PageTab.GUIDE -> GuideTab()
            }
        }

        MemoryFooter(openSessions = snapshot.sessions.count { it.kind == KIND_MODEL })
    }
}

/**
 * The strip at the foot of the page: what the process holds of the machine's RAM as a bar, the heap against its
 * limit, and the encoders in memory, the heavy part that lives outside the heap and is shared by the sessions on
 * it. Read every two seconds; the bar turns red near the limit, where the next session or build runs out.
 */
@Composable
private fun MemoryFooter(openSessions: Int) {
    var stats by remember { mutableStateOf<SystemStats?>(null) }
    var encoders by remember { mutableStateOf<List<LoadedEncoder>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (true) {
            stats = withContext(Dispatchers.IO) { runCatching { SystemStats.read() }.getOrNull() }
            encoders = SharedEncoders.loaded
            delay(2000)
        }
    }
    val encoderBytes = encoders.sumOf { it.sizeBytes }
    val s = stats
    val share = s?.residentShare ?: -1.0
    val tight = share > 0.85 || (s?.heapShare ?: 0.0) > 0.9
    Column(modifier = Modifier.fillMaxWidth().background(ConsoleBackground)) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ConsoleBorderColor))
        Row(modifier = Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("RAM", color = if (tight) DangerColor else MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif, letterSpacing = 1.sp)
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.width(140.dp).height(6.dp).clip(RectangleShape).background(PanelBorder)) {
                if (share >= 0) Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(share.toFloat().coerceIn(0f, 1f)).clip(RectangleShape).background(if (tight) DangerColor else ChartAccent))
            }
            Spacer(Modifier.width(12.dp))
            if (s == null) Meta("reading…")
            else {
                Text(
                    listOfNotNull(
                        if (s.residentBytes > 0 && s.physicalBytes > 0) "process ${bytes(s.residentBytes)} of ${bytes(s.physicalBytes)}" else null,
                        "heap ${bytes(s.heapUsedBytes)} of ${bytes(s.heapMaxBytes)} limit",
                        "native ${bytes(s.nonHeapUsedBytes)}",
                        if (encoders.isNotEmpty()) "${encoders.size} encoder${if (encoders.size == 1) "" else "s"} ≈ ${bytes(encoderBytes)} shared by ${encoders.sumOf { it.holders }} holder${if (encoders.sumOf { it.holders } == 1) "" else "s"}" else "no encoder loaded",
                    ).joinToString(" · "),
                    color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(16.dp))
                if (tight) Meta("close sessions and stop listeners you don't need; the heap limit is the JVM's -Xmx")
                else if (openSessions > 0) Meta("sessions share the encoder; close() what a script no longer needs")
            }
        }
    }
}

/** The strip under the window header: one tab per section with its count, and the website when signed in */
@Composable
private fun PageTabs(tab: PageTab, counts: Map<PageTab, Int?>, signedIn: Boolean, onTab: (PageTab) -> Unit, onWebsite: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(TabBarBackground)) {
        Row(modifier = Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
            PageTab.entries.forEach { t -> TabCell(t.title, t.icon, counts[t], selected = t == tab) { onTab(t) } }
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
private fun TabCell(title: String, icon: ImageVector?, count: Int?, selected: Boolean, onClick: () -> Unit) {
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
        Row(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (selected || hovered) FontColor else MutedFontColor, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(title, color = if (selected || hovered) FontColor else MutedFontColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                Text(count.toString(), color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
            }
        }
    }
}

/* ---------- MODELS ---------- */

@Composable
private fun ColumnScope.ModelsTab(
    snapshot: ModelsSnapshot,
    build: ModelBuild?,
    validating: ValidationProgress?,
    selectedName: String?,
    signedIn: Boolean,
    creating: Boolean,
    onCreating: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onBuild: (String, File, File?, String) -> Unit,
    onValidate: (ModelInfo, File) -> Unit,
    onSave: (ModelInfo, ServingSettings) -> Unit,
    onDescribe: (ModelInfo, String) -> Unit,
    onListen: (ModelInfo, Boolean, ServingSettings) -> Unit,
    onOpen: (ModelInfo) -> Unit,
    onTerminate: (SessionInfo) -> Unit,
    onTerminateModel: (ModelInfo) -> Unit,
    onDestroy: (ModelInfo) -> Unit,
    onUnpublish: (ModelInfo) -> Unit,
    onPublish: (ModelInfo) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    SectionTitle("INTENT MODELS", snapshot.models.size) {
        if (build == null) AccentButton(if (creating) "Close" else "New model", icon = if (creating) Icons.Default.Close else Icons.Default.Add) { onCreating(!creating) }
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
    if (snapshot.models.isNotEmpty()) Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        var listWidth by remember { mutableStateOf(ModelsCache.listWidth) }
        Column(modifier = Modifier.width(listWidth.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                icon = Icons.Default.Memory,
                                menu = listOfNotNull(
                                    if (model.sessions.isEmpty()) ContextMenuItem("Open session") { onOpen(model) } else null,
                                    ContextMenuItem(if (model.listening) "Stop listening" else "Listen on :${model.serving.port}") { onListen(model, !model.listening, model.serving) },
                                    ContextMenuItem("Validate on sample data") { onValidate(model, ensureSampleData()) },
                                    ContextMenuItem("Validate on file…") { chooseFile("Validation data", listOf("csv"))?.let { onValidate(model, it) } },
                                    ContextMenuItem(if (signedIn) "Publish" else "Publish (sign in)") { onPublish(model) },
                                    ContextMenuItem(if (signedIn) "Remove from the website" else "Remove from the website (sign in)") { onUnpublish(model) },
                                    ContextMenuItem("Reveal folder") { openInBrowser(model.dir.toURI().toString()) },
                                    ContextMenuItem("Copy initMod(\"${model.name}\")") { clipboard.setText(AnnotatedString("initMod(\"${model.name}\")")) },
                                    if (model.running) ContextMenuItem("Terminate model") { onTerminateModel(model) } else null,
                                    ContextMenuItem("Destroy model…") { onDestroy(model) },
                                ),
                            ) { onSelect(model.name) }
                        }
                    }
                }
            }
        }
        VerticalSeparator { listWidth = it; ModelsCache.listWidth = it }
        if (selected != null) ModelDetail(
            model = selected,
            signedIn = signedIn,
            validating = validating?.takeIf { it.model == selected.name },
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onValidate = { data -> onValidate(selected, data) },
            onSave = { onSave(selected, it) },
            onDescribe = { onDescribe(selected, it) },
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
private fun NewModelForm(onBuild: (String, File, File?, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
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
        SettingField("About", description) { description = it }
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
            AccentButton("Build", enabled = ready) { onBuild(name.trim(), data!!, test, description) }
        }
    }
}

private enum class DetailTab(val title: String, val icon: ImageVector) { ABOUT("About", Icons.Default.Info), GUIDE("Guide", Icons.Default.MenuBook), METRICS("Metrics", Icons.Default.BarChart), DATA("Data", Icons.Default.TableChart), REQUIREMENTS("Requirements", Icons.Default.Checklist), CONSOLE("Console", Icons.Default.Terminal), SESSIONS("Sessions", Icons.Default.Sensors) }

/**
 * The selected model: name, status and actions in the header; the serving settings when Configure is on; then
 * one tab at a time. METRICS: the charts of the last validation and of the session log, and the Validate actions.
 * DATA: the csv files it was trained and validated on, with their label balance and text lengths, and what it has
 * run on since. REQUIREMENTS: what it needs to run, checked on this machine. CONSOLE: the lines about it in the
 * console. SESSIONS: the sessions it owns with their logs.
 */
@Composable
private fun ModelDetail(
    model: ModelInfo,
    signedIn: Boolean,
    validating: ValidationProgress?,
    modifier: Modifier = Modifier,
    onValidate: (File) -> Unit,
    onSave: (ServingSettings) -> Unit,
    onDescribe: (String) -> Unit,
    onListen: (Boolean, ServingSettings) -> Unit,
    onOpen: () -> Unit,
    onTerminate: (SessionInfo) -> Unit,
    onPublish: () -> Unit,
) {
    var tab by remember(model.name) { mutableStateOf(DetailTab.ABOUT) }
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
    val consoleLines = consoleLinesAbout(model.name)

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
            SmallButton("Configure", icon = Icons.Default.Tune) { configuring = !configuring }
            if (model.listening) DangerButton("Stop", icon = Icons.Default.Stop) { onListen(false, settings) }
            else SmallButton("Listen", icon = Icons.Default.Sensors) { onListen(true, settings) }
            if (model.sessions.isEmpty()) AccentButton("Open session", icon = Icons.Default.PlayArrow, onClick = onOpen)
            else DangerButton("Terminate session", icon = Icons.Default.StopCircle) { model.sessions.forEach(onTerminate) }
            SmallButton(if (signedIn) "Publish" else "Publish (sign in)", icon = Icons.Default.CloudUpload, onClick = onPublish)
        }

        if (configuring) Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(top = 6.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingField("Host", host) { host = it }
            SettingField("Port", port) { port = it.filter(Char::isDigit).take(5) }
            SettingField("Path", path) { path = it }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Text("Methods", color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.width(56.dp))
                Toggle("POST", allowPost) { allowPost = it }
                Spacer(Modifier.width(6.dp))
                Toggle("GET", allowGet) { allowGet = it }
                Spacer(Modifier.weight(1f))
                SmallButton("Save", icon = Icons.Default.Save) { onSave(settings) }
            }
            val url = settings.url(model.name)
            if (allowPost) CopyLine("POST", url, curl = "curl -s -X POST '$url' -H 'Content-Type: application/json' -d '{\"text\": \"turn the lights on\"}'")
            if (allowGet) CopyLine("GET", "$url?text=…", copy = "$url?text=", curl = "curl -s '$url?text=turn%20the%20lights%20on'")
            if (model.listening) Hint("applies after Stop and Listen")
        }

        DetailTabs(
            tabs = DetailTab.entries.map { t ->
                t to when (t) {
                    DetailTab.METRICS -> model.metrics.size.takeIf { it > 0 }
                    DetailTab.CONSOLE -> consoleLines.size.takeIf { it > 0 }
                    DetailTab.SESSIONS -> model.sessions.size.takeIf { it > 0 }
                    else -> null
                }
            },
            selected = tab,
            onSelect = { tab = it },
        )

        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 10.dp).padding(top = 6.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (tab) {
                DetailTab.ABOUT -> AboutBlock(model, onDescribe)
                DetailTab.GUIDE -> GuideBlock(model)
                DetailTab.METRICS -> MetricsBlock(model, validating, onValidate)
                DetailTab.DATA -> DataBlock(model)
                DetailTab.REQUIREMENTS -> RequirementsBlock(model)
                DetailTab.CONSOLE -> ConsoleBlock(model.name, consoleLines)
                DetailTab.SESSIONS -> SessionsOf(model.sessions, onTerminate)
            }
        }
    }
}

/** The tab strip of a detail panel: a title with its count per tab, the chosen one underlined, a rule under them all */
@Composable
private fun DetailTabs(tabs: List<Pair<DetailTab, Int?>>, selected: DetailTab, onSelect: (DetailTab) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().height(26.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { (t, count) ->
                val interactionSource = remember { MutableInteractionSource() }
                val hovered by interactionSource.collectIsHoveredAsState()
                val on = t == selected
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(IntrinsicSize.Max)
                        .hoverable(interactionSource)
                        .clickable(interactionSource = interactionSource, indication = null) { onSelect(t) }
                ) {
                    Row(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(t.icon, contentDescription = null, tint = if (on || hovered) FontColor else MutedFontColor, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(t.title, color = if (on || hovered) FontColor else MutedFontColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                        if (count != null) {
                            Spacer(Modifier.width(6.dp))
                            Text(count.toString(), color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(if (on) TabActiveIndicator else Color.Transparent))
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PanelBorder))
    }
}

/**
 * What the model is, from its manifest: the description (editable here, kept in module.json), where it lives,
 * what it was trained on and how. Models built before the training details were kept say so.
 */
@Composable
private fun AboutBlock(model: ModelInfo, onDescribe: (String) -> Unit) {
    val m = model.manifest
    var text by remember(model.name, m.description) { mutableStateOf(m.description ?: "") }
    val dirty = text.trim() != (m.description ?: "")
    Row(verticalAlignment = Alignment.CenterVertically) {
        SubTitle("ABOUT")
        Spacer(Modifier.weight(1f))
        if (dirty) AccentButton("Save description", icon = Icons.Default.Save) { onDescribe(text) }
    }
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        textStyle = TextStyle(color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif, lineHeight = 18.sp),
        cursorBrush = SolidColor(FontColor),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CodeBackground, RectangleShape)
            .border(1.dp, ConsoleBorderColor, RectangleShape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        decorationBox = { inner ->
            if (text.isEmpty()) Text("no description yet: write what this model is for and what it was trained on", color = MutedFontColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
            inner()
        },
    )

    SubTitle("WHERE")
    Row(verticalAlignment = Alignment.CenterVertically) {
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(model.dir.absolutePath, color = LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        SmallButton("Reveal folder", icon = Icons.Default.FolderOpen) { openInBrowser(model.dir.toURI().toString()) }
    }
    KeyValues(
        "created" to m.createdAt.take(19).replace('T', ' '),
        "encoder" to "${File(m.encoder).name}  ·  ${m.encoder}",
        "head" to "${m.model}  ·  ${m.format}  ·  ${m.input} [${m.inputDim}] → ${m.output} [${m.outputDim}]",
        "labels" to m.labels.joinToString().ifEmpty { "—" },
    )

    SubTitle("TRAINING")
    if (m.epochs == null && m.architecture == null) Hint("not recorded: this model was built before the training details were kept; build it again to have them")
    KeyValues(
        *listOfNotNull(
            m.architecture?.let { "architecture" to it },
            "data" to (m.data ?: if (model.name == "dummy") "none, the weights are written, not trained" else "unknown"),
            m.trainRows?.let { "train rows" to it.toString() },
            m.validationRows?.let { "validation" to "$it rows of ${m.validation ?: "a split"}" },
            m.epochs?.let { "epochs" to it.toString() },
            m.batchSize?.let { "batch size" to it.toString() },
            m.learningRate?.let { "learning rate" to it.toString() },
            m.hiddenDim?.let { "hidden units" to it.toString() },
            m.finalLoss?.let { "final loss" to fmt(it) },
            m.trainAccuracy?.let { "train accuracy" to pct(it) },
        ).toTypedArray()
    )
}

/**
 * The guide that ships inside the model, `AMP/modules/<name>/guide.md`: a downloaded model brings its own, a
 * built one starts with the default written from its manifest. Read as light markdown (headings, paragraphs,
 * bullets, fenced code); Edit shows the text itself, Save writes the file, and Publish sends it with the model.
 */
@Composable
private fun GuideBlock(model: ModelInfo) {
    val file = guideFile(model.dir)
    val stamp = file.lastModified()
    val stored = remember(model.name, stamp) { runCatching { file.takeIf { it.isFile }?.readText() }.getOrNull() }
    var editing by remember(model.name) { mutableStateOf(false) }
    var text by remember(model.name, stamp) { mutableStateOf(stored ?: defaultGuide(model.manifest)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SubTitle("GUIDE")
        Spacer(Modifier.width(10.dp))
        Meta(if (stored == null) "none in the model yet; this is the default, Save keeps it" else file.name)
        Spacer(Modifier.weight(1f))
        if (editing) {
            AccentButton("Save", icon = Icons.Default.Save) { runCatching { file.writeText(text) }.onFailure { logInternal("guide: couldn't write ${file.name}: ${it.message}", LogLevel.ERROR) }; editing = false }
            Spacer(Modifier.width(6.dp))
            SmallButton("Cancel") { text = stored ?: defaultGuide(model.manifest); editing = false }
        } else SmallButton(if (stored == null) "Edit and save" else "Edit") { editing = true }
    }
    if (editing) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = TextStyle(color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp),
            cursorBrush = SolidColor(FontColor),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .background(CodeBackground, RectangleShape)
                .border(1.dp, ConsoleBorderColor, RectangleShape)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        )
    } else GuideText(text)
}

/** Light markdown: # and ## headings, "- " bullets, ``` fences, blank-line paragraphs; everything else is a line of prose */
@Composable
private fun GuideText(text: String) {
    val blocks = remember(text) { guideBlocks(text) }
    Column(modifier = Modifier.widthIn(max = 860.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        blocks.forEach { (kind, body) ->
            when (kind) {
                "h1" -> Text(body, color = FontColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
                "h2" -> Text(body, color = FontColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                "code" -> Lines(*body.lines().toTypedArray())
                "bullet" -> Row(verticalAlignment = Alignment.Top) {
                    Text("·", color = MutedFontColor, fontSize = 12.sp, modifier = Modifier.width(14.dp))
                    Text(body, color = LogMessageColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif, lineHeight = 18.sp)
                }
                else -> Para(body)
            }
        }
    }
}

/** Splits markdown into (kind, text) blocks: h1, h2, code, bullet, para */
private fun guideBlocks(text: String): List<Pair<String, String>> {
    val out = ArrayList<Pair<String, String>>()
    val para = StringBuilder()
    fun flush() { if (para.isNotBlank()) out += "para" to para.toString().trim(); para.setLength(0) }
    var code: StringBuilder? = null
    text.lines().forEach { raw ->
        val line = raw.trimEnd()
        when {
            code != null -> if (line.trim().startsWith("```")) { out += "code" to code.toString().trimEnd(); code = null } else code!!.appendLine(line)
            line.trim().startsWith("```") -> { flush(); code = StringBuilder() }
            line.startsWith("## ") -> { flush(); out += "h2" to line.removePrefix("## ").uppercase() }
            line.startsWith("# ") -> { flush(); out += "h1" to line.removePrefix("# ") }
            line.startsWith("- ") || line.startsWith("* ") -> { flush(); out += "bullet" to line.drop(2) }
            line.isBlank() -> flush()
            else -> { if (para.isNotEmpty()) para.append(' '); para.append(line.trim()) }
        }
    }
    code?.let { out += "code" to it.toString().trimEnd() }
    flush()
    return out
}

/**
 * The charts of one validation run at a time, chosen on the run strip (the latest first), each a figure with a
 * title, the run and its data in the subtitle and a caption that says how to read it: the scores per label and
 * the confusion matrix. Under them, with more than one run, how the scores changed over the runs; and from the
 * sessions log how sure the model is and how long it takes. Validate adds a run.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricsBlock(model: ModelInfo, validating: ValidationProgress?, onValidate: (File) -> Unit) {
    val runs = model.metrics
    Row(verticalAlignment = Alignment.CenterVertically) {
        SubTitle("METRICS")
        Spacer(Modifier.weight(1f))
        if (validating != null) Meta("validating ${validating.done}/${validating.total}")
        else {
            AccentButton("Validate on sample data", icon = Icons.Default.FactCheck) { onValidate(ensureSampleData()) }
            Spacer(Modifier.width(6.dp))
            AccentButton("Validate on file", icon = Icons.Default.FactCheck) { chooseFile("Validation data", listOf("csv"))?.let(onValidate) }
        }
    }
    fun plot(name: String) = model.plots.firstOrNull { it.file.name == name }

    if (runs.isEmpty()) Hint("no validation yet: validate on the sample data or on a csv of your own")
    else {
        var index by remember(model.name) { mutableStateOf(runs.lastIndex) }
        if (index > runs.lastIndex) index = runs.lastIndex
        val run = runs[index]
        // The runs as a strip, the latest first; every one says what it was validated on when hovered
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("run", color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(40.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                runs.indices.reversed().forEach { i -> Toggle("${i + 1}", i == index) { index = i } }
            }
        }
        KeyValues(
            "when" to run.createdAt.take(19).replace('T', ' '),
            "on" to "${run.validationSize} rows of ${run.validation}${if (run.trainSize > 0) " · trained on ${run.trainSize}" else ""}",
            "scores" to "accuracy ${pct(run.accuracy)}  ·  macro F1 ${fmt(run.macroF1)}  ·  ${run.labels.size} labels: ${run.labels.keys.joinToString()}",
        )
        run.warnings.forEach { Hint("warning: $it") }
        val metricsStamp = File(model.dir, "metrics.json").lastModified()
        var runPlots by remember(model.name) { mutableStateOf<List<MetricsPlot>>(emptyList()) }
        LaunchedEffect(model.name, index, metricsStamp) {
            runPlots = withContext(Dispatchers.IO) { runCatching { MetricsImages.runPlots(model.dir, runs, index) }.getOrDefault(emptyList()) }
        }
        if (runPlots.isEmpty()) Hint("drawing…")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            runPlots.forEach { PlotImage(it) }
        }
        plot("runs.png")?.let { PlotImage(it) }
    }

    val session = listOfNotNull(plot("confidence.png"), plot("latency.png"))
    if (session.isNotEmpty()) {
        SubTitle("FROM THE SESSIONS")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            session.forEach { PlotImage(it) }
        }
    }
}

/** A labeled csv read once per file version: its rows and which columns hold the text and label */
private class CsvFacts(val table: List<List<String>>, val textColumn: Int, val labelColumn: Int) {
    val rows: List<List<String>> get() = table.drop(1)
    val labels: Map<String, Int> get() = rows.mapNotNull { it.getOrNull(labelColumn)?.trim()?.takeIf(String::isNotEmpty) }.groupingBy { it }.eachCount()
    val wordCounts: List<Int> get() = rows.mapNotNull { it.getOrNull(textColumn)?.trim()?.takeIf(String::isNotEmpty) }.map { it.split(Regex("\\s+")).size }
}

@Composable
private fun rememberCsv(file: File): CsvFacts? = remember(file.path, file.lastModified()) {
    runCatching {
        val table = parseCsv(file.readText()).filter { row -> row.any { it.isNotBlank() } }
        if (table.size < 2) return@runCatching null
        val headers = table.first().map { it.trim() }
        val structure = inferStructure(headers, table.drop(1)) ?: return@runCatching null
        CsvFacts(table, headers.indexOf(structure.textColumn), headers.indexOf(structure.labelColumn))
    }.getOrNull()
}

/**
 * The data behind a model. The csv files it was trained and validated on, each with how many rows per label (so
 * an unbalanced set is visible at once), how long the texts are, and the first rows; then what the model has run
 * on since: the texts its sessions were asked and the requests the server answered.
 */
@Composable
private fun DataBlock(model: ModelInfo) {
    val files = listOfNotNull(model.dataFile?.let { "trained on" to it }, model.validationFile?.takeIf { it != model.dataFile }?.let { "validated on" to it })
    SubTitle("DATA FILES")
    if (files.isEmpty()) Hint(if (model.manifest.data != null) "${model.manifest.data} is not in AMP/ any more" else "no data file recorded; the dummy's head is written, not trained")
    files.forEach { (role, file) ->
        val facts = rememberCsv(file)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(role, color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(92.dp))
            Text(file.name, color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(10.dp))
            Meta(facts?.let { "${it.rows.size} rows · ${it.labels.size} labels" } ?: "no text and label columns")
            Spacer(Modifier.weight(1f))
            AccentButton("Open in viewer", icon = Icons.Default.OpenInNew) { openFile(file) }
        }
        if (facts != null) {
            val known = model.manifest.labels.toSet()
            val balance = facts.labels.entries.sortedByDescending { it.value }
            Fold("rows per label", balance.size, open = true, key = "${model.name}:${file.path}:labels") {
                BarRows(balance.map { (label, n) -> BarRow(label, n, if (label in known || known.isEmpty()) null else "not a label of ${model.name}") })
            }
            val words = facts.wordCounts
            if (words.isNotEmpty()) {
                val buckets = listOf("1–2 words" to 1..2, "3–4" to 3..4, "5–6" to 5..6, "7–9" to 7..9, "10+" to 10..Int.MAX_VALUE)
                Fold("text length", null, open = false, key = "${model.name}:${file.path}:lengths") {
                    BarRows(buckets.map { (name, range) -> BarRow(name, words.count { it in range }) })
                }
            }
        }
        Fold("preview ${file.name}", null, open = false, key = "${model.name}:${file.path}") { CsvPreview(file) }
    }

    val logFile = SessionRuns.logFile(model.dir)
    val logged = remember(model.name, logFile.lastModified()) { SessionRuns.read(model.dir, limit = 2000) }
    val incomingFile = SessionRuns.incomingFile(model.dir)
    val incoming = remember(model.name, incomingFile.lastModified()) { SessionRuns.incoming(model.dir) }
    SubTitle("RAN ON")
    if (logged.isEmpty() && incoming == 0) Hint("nothing yet: open a session or listen and send it text")
    else {
        KeyValues(
            "sessions" to "${logged.size} texts answered, in ${logFile.name}",
            "served" to if (incoming == 0) "no requests yet" else "$incoming requests, in ${incomingFile.name}",
        )
        if (logged.isNotEmpty()) {
            val byLabel = logged.mapNotNull { it.top }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
            Fold("answered with", byLabel.size, open = true, key = "${model.name}:answered") {
                BarRows(byLabel.map { (label, n) -> BarRow(label, n) })
            }
            Fold("last texts", null, open = false, key = "${model.name}:last") {
                KeyValues(*logged.takeLast(12).reversed().map { r -> (r.top ?: "-") to "${r.text.take(70)}  (${r.millis} ms)" }.toTypedArray())
            }
        }
    }
}

private data class BarRow(val label: String, val value: Int, val note: String? = null)

/** Counts as calm horizontal bars of one hue, the longest filling the row, the count and an optional note beside each */
@Composable
private fun BarRows(rows: List<BarRow>) {
    if (rows.isEmpty()) { Hint("none"); return }
    val max = rows.maxOf { it.value }.coerceAtLeast(1)
    val labelWidth = (rows.maxOf { it.label.length } * 7 + 12).coerceIn(60, 180).dp
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.label, color = FontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(labelWidth))
                Box(modifier = Modifier.weight(1f).height(8.dp).clip(RectangleShape).background(PanelBorder)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(row.value.toFloat() / max).clip(RectangleShape).background(ChartAccent))
                }
                Spacer(Modifier.width(10.dp))
                Text(row.value.toString(), color = FontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(40.dp))
                row.note?.let { Meta(it) }
            }
        }
    }
}

/** What the model needs to run, each line checked on this machine: met, not met, or only information */
@Composable
private fun RequirementsBlock(model: ModelInfo) {
    var requirements by remember(model.name) { mutableStateOf<List<Requirement>?>(null) }
    // The port probe binds a socket, so the checks run off the UI thread and again when serving changes
    LaunchedEffect(model.name, model.listening, model.serving) {
        requirements = withContext(Dispatchers.IO) { runCatching { ModelRequirements.check(model.dir, model.manifest, model.serving, model.listening) }.getOrDefault(emptyList()) }
    }
    val checks = requirements
    SubTitle("REQUIREMENTS")
    if (checks == null) { Hint("checking…"); return }
    val unmet = checks.count { it.ok == false }
    Hint(if (unmet == 0) "everything the model needs is here" else "$unmet not met: initMod and Listen will fail until they are")
    Column(modifier = Modifier.fillMaxWidth()) {
        checks.forEachIndexed { i, r ->
            if (i > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PanelBorder))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.width(18.dp).height(16.dp), contentAlignment = Alignment.CenterStart) {
                    when (r.ok) {
                        true -> Icon(Icons.Default.CheckCircle, contentDescription = "met", tint = SuccessColor, modifier = Modifier.size(12.dp))
                        false -> Icon(Icons.Default.Cancel, contentDescription = "not met", tint = DangerColor, modifier = Modifier.size(12.dp))
                        null -> Icon(Icons.Default.Info, contentDescription = "information", tint = MutedFontColor, modifier = Modifier.size(12.dp))
                    }
                }
                Text(r.name, color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(110.dp))
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(r.detail, color = if (r.ok == false) LogErrorTextColor else LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                }
            }
        }
    }
}

/** The console lines that name the model, read off the same list the sandbox console shows */
private fun consoleLinesAbout(name: String): List<ResponseDto> {
    val pattern = Regex("(?i)(^|[^A-Za-z0-9_.-])" + Regex.escape(name) + "([^A-Za-z0-9_.-]|$)")
    return consoleData.filter { it.source == INTERNAL_SOURCE && (it.message ?: "").let(pattern::containsMatchIn) }
}

/**
 * The console of the model: every internal line that names it (what initMod, serve, validate and the builder
 * said), newest last, or with the switch everything the models, packages and sessions log.
 */
@Composable
private fun ConsoleBlock(name: String, mine: List<ResponseDto>) {
    var all by remember(name) { mutableStateOf(false) }
    val pattern = remember { Regex("(?i)model|validat|package|session|collection|metrics|module|env '|jar|sample|build|initMod|ort:|auth:|publish|serve") }
    val lines = (if (all) consoleData.filter { it.source == INTERNAL_SOURCE && (it.message ?: "").let(pattern::containsMatchIn) } else mine).takeLast(400)
    val scroll = rememberScrollState()
    LaunchedEffect(lines.size, all) { scroll.scrollTo(scroll.maxValue) }
    val time = remember { DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SubTitle("CONSOLE")
        Spacer(Modifier.width(10.dp))
        Meta("${lines.size} lines")
        Spacer(Modifier.weight(1f))
        Toggle("this model", !all) { all = false }
        Spacer(Modifier.width(6.dp))
        Toggle("all models", all) { all = true }
    }
    var height by remember { mutableStateOf(ModelsCache.consoleHeight) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RectangleShape)
            .background(CodeBackground)
            .border(1.dp, ConsoleBorderColor, RectangleShape)
            .verticalScroll(scroll)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (lines.isEmpty()) Meta(if (all) "nothing yet" else "nothing about $name yet: open a session, listen or validate")
        SelectionContainer {
            Column {
                lines.forEach { e ->
                    Row {
                        Text(e.time?.let { time.format(it) } ?: "", color = LogTimestampColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(e.message ?: "", color = if (e.level == LogLevel.ERROR) LogErrorTextColor else LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
    HeightHandle(height, min = 100f, max = 900f) { height = it; ModelsCache.consoleHeight = it }
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
        Column(modifier = Modifier.fillMaxWidth().clip(RectangleShape).background(CodeBackground).border(1.dp, ConsoleBorderColor, RectangleShape).padding(8.dp)) {
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
        modifier = Modifier.width(plot.width.dp).height(plot.height.dp).clip(RectangleShape)
    )
}

/* ---------- PACKAGES ---------- */

@Composable
private fun ColumnScope.PackagesTab(
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
        AccentButton(if (newCollection) "Close" else "New collection", icon = if (newCollection) Icons.Default.Close else Icons.Default.Add) { newCollection = !newCollection }
        Spacer(Modifier.width(6.dp))
        AccentButton("Add jar", icon = Icons.Default.Add, onClick = onAdd)
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
    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        var listWidth by remember { mutableStateOf(ModelsCache.listWidth) }
        Column(modifier = Modifier.width(listWidth.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        jars.forEach { jar -> JarRow(jar, selected = jar.name == selected?.name, onFetch = { onFetch(jar) }, onSelect = { onSelect(jar.name) }) }
                    }
                }
            }
        }
        VerticalSeparator { listWidth = it; ModelsCache.listWidth = it }
        if (selected != null) JarDetail(selected, Modifier.weight(1f).fillMaxHeight(), onFetch = { onFetch(selected) }, onTerminate = onTerminate)
    }
}

/** The right-click actions of a package: the same as its detail panel's buttons, plus the copy of the call that opens it */
@Composable
private fun jarMenu(jar: JarInfo, onFetch: (() -> Unit)? = null, onDetach: (() -> Unit)? = null): List<ContextMenuItem> {
    val clipboard = LocalClipboardManager.current
    return listOfNotNull(
        ContextMenuItem("Copy initMod(\"${jar.name}\")") { clipboard.setText(AnnotatedString("initMod(\"${jar.name}\")")) },
        ContextMenuItem("Open page") { openInBrowser(jar.page) },
        if (!jar.local && onFetch != null) ContextMenuItem("Fetch", onFetch) else null,
        jar.file?.let { f -> ContextMenuItem("Reveal file") { openInBrowser(f.parentFile.toURI().toString()) } },
        onDetach?.let { ContextMenuItem("Detach from collection", it) },
    )
}

@Composable
private fun JarRow(jar: JarInfo, selected: Boolean, onDetach: (() -> Unit)? = null, onFetch: (() -> Unit)? = null, onSelect: () -> Unit) {
    ListRow(
        name = jar.name,
        active = jar.open > 0,
        selected = selected,
        meta = listOfNotNull(jar.contents?.minJava?.let { "java $it+" }, jar.version?.let { "v$it" }).joinToString(" · "),
        status = jar.status,
        underline = jar.open > 0,
        icon = Icons.Default.Inventory2,
        onDetach = onDetach,
        menu = jarMenu(jar, onFetch, onDetach),
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
                AccentButton(if (adding) "Close" else "Add", icon = if (adding) Icons.Default.Close else Icons.Default.Add) { adding = !adding }
                SmallButton("Delete folder", icon = Icons.Default.Delete) { onCollection { PackageCollections.delete(collection.name) } }
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
            SmallButton("Open page", icon = Icons.Default.Language) { openInBrowser(jar.page) }
            if (!jar.local) AccentButton("Fetch", icon = Icons.Default.Download, onClick = onFetch)
            jar.file?.let { f -> SmallButton("Reveal file", icon = Icons.Default.FolderOpen) { openInBrowser(f.parentFile.toURI().toString()) } }
        }

        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 10.dp).padding(top = 6.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    SectionTitle("SESSIONS", sessions.size) { if (sessions.isNotEmpty()) DangerButton("Terminate all", icon = Icons.Default.StopCircle, onClick = onTerminateAll) }
    if (sessions.isEmpty()) {
        Hint("no open sessions")
        return
    }
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val shown = sessions.filter { q.isEmpty() || it.owner.lowercase().contains(q) || it.kind.lowercase().contains(q) || "#${it.id}".contains(q) }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        if (sessions.size > 1) DangerButton("Terminate all", icon = Icons.Default.StopCircle) { sessions.forEach(onTerminate) }
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
    val clipboard = LocalClipboardManager.current
    val menu = listOfNotNull(
        ContextMenuItem("Terminate", onTerminate),
        if (session.hasLog) ContextMenuItem(if (showLog) "Hide log" else "Show log") { showLog = !showLog } else null,
        if (session.hasLog) ContextMenuItem("Copy log") { clipboard.setText(AnnotatedString(session.log.joinToString("\n") { "${'$'}{it.time} ${'$'}{it.kind} ${'$'}{it.text}" })) } else null,
        ContextMenuItem("Copy #${session.id}") { clipboard.setText(AnnotatedString("${session.id}")) },
    )
    ContextMenuArea(items = { menu }) { Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(session.running)
            Spacer(Modifier.width(8.dp))
            Text(if (showOwner) "${session.owner}  #${session.id}" else "#${session.id}", color = FontColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(10.dp))
            Meta(listOfNotNull(if (showOwner) session.kind else null, if (session.running) "inferring" else "idle", "${session.runs} runs", uptime(Duration.between(session.openedAt, Instant.now()))).joinToString(" · "))
            Spacer(Modifier.weight(1f))
            if (session.hasLog) {
                SmallButton(if (showLog) "Hide log" else "Log ${session.log.size}", icon = Icons.Default.Notes) { showLog = !showLog }
                Spacer(Modifier.width(6.dp))
            }
            DangerButton("Terminate", icon = Icons.Default.StopCircle, onClick = onTerminate)
        }
        if (showLog) {
            Spacer(Modifier.height(6.dp))
            LogBox(session.log)
        }
    } }
}

/** The last lines of a session's log, console style: time, kind and text */
@Composable
private fun LogBox(entries: List<SessionLogEntry>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .background(CodeBackground)
            .border(1.dp, ConsoleBorderColor, RectangleShape)
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

/* ---------- GUIDE ---------- */

/** One paragraph of the guide */
@Composable
private fun Para(text: String) {
    Text(text, color = LogMessageColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 6.dp))
}

/** A few lines a script would hold, in the console's box so they can be selected and copied */
@Composable
private fun Lines(vararg lines: String) {
    SelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RectangleShape)
                .background(CodeBackground)
                .border(1.dp, ConsoleBorderColor, RectangleShape)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) { lines.forEach { Text(it, color = LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp) } }
    }
    Spacer(Modifier.height(6.dp))
}

/**
 * How to work with this page and the models, packages and sessions behind it, one fold per subject: what an
 * intent model is and how it is built, validated, served and published; the same from a script; packages and
 * collections; sessions and memory; the files under AMP/; how to read the charts.
 */
@Composable
private fun GuideTab() {
    SectionTitle("HELP", null)
    Column(modifier = Modifier.widthIn(max = 860.dp).verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Fold("What an intent model is", null, open = true, key = "guide:model") {
            Para("An intent model turns a sentence into one of a few labels, like greeting, lights or weather. It has two parts: the encoder, a BERT model (bge) that turns the text into 768 numbers, and the head, a small dense network trained on your rows that turns those numbers into a score per label. Only the head is yours: it lives in AMP/modules/<name>/ as model.onnx with module.json (name, labels, encoder, how it was trained), metrics.json (every validation), serving.json and sessions.log. The encoder is shared by every model and loaded once.")
            Para("The dummy model is there from the first start: a head with made-up weights over the same encoder, so the page has something to show and a script can call initMod(\"dummy\") before anything is trained. Its scores mean nothing.")
        }
        Fold("Building one", null, open = false, key = "guide:build") {
            Para("Press + New model. Give it a name, a sentence about what it is for, and a csv with a text column and a label column (the columns are inferred from the header; Sample data writes AMP/sample-intents.csv with 45 rows over three labels). A test csv is optional; without one, 20% of the rows are held out. Build embeds every row through the encoder, fits the head for 150 epochs, validates it and exports it. The card above the list follows the build; the console lines of the model say the rest.")
            Para("The list groups models by the data they were trained on, running ones first, then by accuracy. Search matches names, labels, status and data files. Right-click a row for its actions.")
        }
        Fold("Reading the metrics", null, open = false, key = "guide:metrics") {
            Para("Every validation is a run; Validate on sample data or Validate on file adds one, and a build adds the first. Pick a run on the strip. Scores per label: for every label, precision (of what the model called that label, how much really was), recall (of the rows of that label, how much it found) and F1 (their harmonic mean) on one 0 to 1 axis, with the row count; the macro row averages the labels. Confusion matrix: every actual label in a row, what the model answered in the columns; the diagonal is right, a bright cell off it is the confusion to fix, and the caption names the biggest.")
            Para("Scores by run shows accuracy, macro F1 and every label's F1 across the runs, the latest at the right: a row that lightens is improving. From the sessions: how sure the model is (where every label's scores fall between 0 and 1 across every answer it gave) and how long an answer takes.")
        }
        Fold("Serving it", null, open = false, key = "guide:serve") {
            Para("Listen starts an HTTP server for the model on the host, port and path in Configure (loopback by default), with POST {\"text\": \"…\"} or GET ?text=… returning the scores as JSON. Copy and Copy curl in Configure give the address and a working call. What it answers is appended to incoming.jsonl, so it can be reviewed and labeled later. Listening resumes when the app starts again, until Stop.")
            Para("Publish sends the module to the Amie server under your account (sign in first); Open website opens its page there.")
        }
        Fold("From a script", null, open = false, key = "guide:script") {
            Para("In the sandbox, amie is the API; every name below is also reachable without the prefix. initMod(name) gives the session of an exported model. A model has one session at most: the one already open is returned, so running a script again, or Open session on the page, never opens another. inference(text) returns \"label:score\" strings, best first. close() closes it; the Sessions tab can terminate it too.")
            Lines(
                "val model = initMod(\"dummy\")",
                "model.inference(\"turn the lights on\")   // [\"lights:0.57\", \"weather:0.47\", ...]",
                "model.close()",
            )
            Para("Training from a script: source a csv, optionally relate a test file to it, then build. validateMetrics, metricsChart, metricsHistory and confusionMatrix print the stored runs in the console.")
            Lines(
                "val train = source(\"sample-intents.csv\")",
                "relate(train, \"test.csv\")             // optional",
                "modelBuilder(\"lights\", train)",
                "validateMetrics(\"lights\")",
            )
        }
        Fold("Packages", null, open = false, key = "guide:packages") {
            Para("A package is a jar with a class that has run(String) and close(). The Packages tab lists the jars in AMP/pluginDir and the env, and the ones the Amie server offers; Fetch downloads one, + Add jar copies one in. A collection is a folder of jars that belong together, for example every jar that runs on Java 21, made with + New collection. The detail panel says what a jar needs (Java version, language), where it came from and everything it contains, searchable.")
            Para("Unlike a model, a package is a bottle: every initMod(\"name.jar\") opens a fresh instance in its own class loader, and each is a session of its own.")
            Lines("val plugin = initMod(\"example.jar\")", "plugin.inference(\"hello\")", "plugin.close()")
        }
        Fold("Sessions and memory", null, open = false, key = "guide:memory") {
            Para("Every open model, package or bare ONNX session is listed on the Sessions tab with its log (what it was asked and answered) and can be terminated there. The encoder is the heavy part, hundreds of MB outside the Java heap, and it is loaded once and shared by every session, listener, validation and build on it; the last one to close frees it. The footer shows what the process holds of the machine's memory, the heap against its limit, and the encoders loaded. It turns red near the limit: close sessions and stop listeners you don't need.")
        }
        Fold("Requirements", null, open = false, key = "guide:requirements") {
            Para("The Requirements tab of a model checks, on this machine, everything it needs before initMod or Listen can succeed: the encoder folder with model.onnx, tokenizer.json and a config whose hidden size matches the head, the head's own model.onnx, as many labels as outputs, the ONNX runtime, Java, the heap limit and whether the serving port is free. A red cross is something to fix; a dot is only information.")
        }
        Fold("Files under AMP/", null, open = false, key = "guide:files") {
            Lines(
                "AMP/modules/modules.json          the models by name",
                "AMP/modules/<name>/module.json    name, labels, encoder, description, how it was trained",
                "AMP/modules/<name>/model.onnx     the head",
                "AMP/modules/<name>/metrics.json   every validation run",
                "AMP/modules/<name>/serving.json   host, port, path, methods, whether it listens",
                "AMP/modules/<name>/sessions.log   what its sessions were asked and answered",
                "AMP/modules/<name>/incoming.jsonl what the server answered",
                "AMP/modules/<name>/plots/         the charts, drawn when the metrics or the log change",
                "AMP/sample-intents.csv            the sample data",
                "AMP/pluginDir/                    package jars and collections.json",
            )
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
private fun ListRow(name: String, active: Boolean, selected: Boolean, meta: String, status: String, underline: Boolean = false, icon: ImageVector? = null, onDetach: (() -> Unit)? = null, menu: List<ContextMenuItem> = emptyList(), onSelect: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    ContextMenuArea(items = { menu }) { Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .background(when { selected -> PanelSurfaceSelected; hovered -> PanelSurface; else -> Color.Transparent })
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onSelect() }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (active) FontColor else MutedFontColor, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(7.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = FontColor, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontFamily = FontFamily.SansSerif, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (meta.isNotEmpty()) Meta(meta)
            }
            Spacer(Modifier.width(6.dp))
            if (hovered && onDetach != null) {
                SmallButton("Detach", icon = Icons.Default.LinkOff, onClick = onDetach)
            } else {
                Text(status, color = if (active) FontColor else MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(2.dp).background(if (underline) SuccessColor else Color.Transparent))
    } }
}

/** One URL a model is reached at: the method, the address, and Copy for the address and for a curl call that uses it */
@Composable
private fun CopyLine(label: String, shown: String, copy: String = shown, curl: String? = null) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(shown) { mutableStateOf<String?>(null) }
    LaunchedEffect(copied) { if (copied != null) { delay(1500); copied = null } }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label.padEnd(4), color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
        Spacer(Modifier.width(12.dp))
        SelectionContainer(modifier = Modifier.weight(1f, fill = false)) {
            Text(shown, color = LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        copied?.let { Meta(it); Spacer(Modifier.width(6.dp)) }
        SmallButton("Copy", icon = Icons.Default.ContentCopy) { clipboard.setText(AnnotatedString(copy)); copied = "copied" }
        if (curl != null) {
            Spacer(Modifier.width(4.dp))
            SmallButton("Copy curl", icon = Icons.Default.ContentCopy) { clipboard.setText(AnnotatedString(curl)); copied = "copied curl" }
        }
    }
}

/**
 * The rule between a list and its detail panel: 1dp drawn, a wider strip under the pointer, dragged sideways to
 * resize the list (180 to 640dp); the cursor says so and the rule brightens while it is hovered
 */
@Composable
private fun VerticalSeparator(onWidth: (Float) -> Unit) {
    val density = LocalDensity.current.density
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var width by remember { mutableStateOf(ModelsCache.listWidth) }
    val drag = rememberDraggableState { delta ->
        width = (width + delta / density).coerceIn(180f, 640f)
        onWidth(width)
    }
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(7.dp)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.E_RESIZE_CURSOR)))
            .draggable(drag, Orientation.Horizontal),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(if (hovered) PanelBorderSelected else PanelDividerColor))
    }
}

/** A handle under a box: dragged up or down to change the box's height; the cursor says so */
@Composable
private fun HeightHandle(height: Float, min: Float, max: Float, onHeight: (Float) -> Unit) {
    val density = LocalDensity.current.density
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val drag = rememberDraggableState { delta -> onHeight((height + delta / density).coerceIn(min, max)) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.N_RESIZE_CURSOR)))
            .draggable(drag, Orientation.Vertical),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.width(36.dp).height(2.dp).clip(RectangleShape).background(if (hovered) PanelBorderSelected else PanelBorder))
    }
}

/** The bordered panel of the selected item */
@Composable
private fun DetailPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .clip(RectangleShape)
            .background(PanelSurface)
            .border(1.dp, PanelBorder, RectangleShape)
    ) { content() }
}

/** Name and one status line on the left of a detail panel's header, the actions on the right */
@Composable
private fun DetailHeader(name: String, active: Boolean, status: String, actions: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
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
            .clip(RectangleShape)
            .border(1.dp, if (strong) PanelBorderSelected else PanelBorder, RectangleShape)
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
            modifier = Modifier.fillMaxWidth().clip(RectangleShape).clickable { expanded = !expanded }.padding(vertical = 2.dp),
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
        if (expanded) Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 1.dp, bottom = 3.dp)) { content() }
    }
}

/** A one-line filter over a list: an icon, the text, and a clear button once there is text */
@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CodeBackground, RectangleShape)
            .border(1.dp, if (focused || value.isNotEmpty()) PageAccent else PanelBorderSelected, RectangleShape)
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
            .clip(RectangleShape)
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
                .background(CodeBackground, RectangleShape)
                .border(1.dp, ConsoleBorderColor, RectangleShape)
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
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (count == null) title else "$title  $count", color = FontColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, modifier = Modifier.weight(1f))
        action()
    }
}

@Composable
private fun SubTitle(title: String) {
    Text(title, color = FontColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .background(PanelSurface)
            .border(1.dp, PanelBorder, RectangleShape)
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
                .background(CodeBackground, RectangleShape)
                .border(1.dp, ConsoleBorderColor, RectangleShape)
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
            .clip(RectangleShape)
            .background(if (on) PageAccent.copy(alpha = 0.35f) else CodeBackground)
            .border(1.dp, if (on) PageAccent else ConsoleBorderColor, RectangleShape)
            .clickable { onChange(!on) }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** The accent chip for the actions that create, build, validate, open or add: a dark blue that stays quiet */
@Composable
private fun AccentButton(label: String, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colour = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RectangleShape)
            .background(when { !enabled -> PageAccent.copy(alpha = 0.35f); hovered -> AccentButtonHover; else -> PageAccent })
            .hoverable(interactionSource)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, tint = colour, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(5.dp)) }
        Text(label, color = colour, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** The chip of the actions that end something: Terminate, Destroy. A dark red that brightens on hover */
@Composable
private fun DangerButton(label: String, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colour = if (enabled) FontColor else MutedFontColor.copy(alpha = 0.5f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RectangleShape)
            .background(when { !enabled -> DangerButton.copy(alpha = 0.4f); hovered -> DangerButtonHover; else -> DangerButton })
            .border(1.dp, if (enabled) DangerButtonBorder else PanelBorder, RectangleShape)
            .hoverable(interactionSource)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, tint = colour, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(5.dp)) }
        Text(label, color = colour, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** A gray chip like the editor's tabs; it brightens on hover instead of carrying a colour of its own */
@Composable
private fun SmallButton(label: String, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colour = if (enabled) FontColor else MutedFontColor.copy(alpha = 0.5f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RectangleShape)
            .background(if (enabled && hovered) ChipHoverBackground else ChipBackground)
            .border(1.dp, if (enabled) ChipBorderColor else PanelBorder, RectangleShape)
            .hoverable(interactionSource)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, tint = colour, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(5.dp)) }
        Text(label, color = colour, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
