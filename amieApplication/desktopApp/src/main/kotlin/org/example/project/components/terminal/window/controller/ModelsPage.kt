package org.example.project.components.terminal.window.controller

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
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
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberCursorPositionProvider
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
import org.example.project.components.terminal.window.controller.envBuilder.MavenCentral
import org.example.project.components.terminal.window.controller.envBuilder.MavenHit
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.ModelBuild
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.ModelBuildProgress
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.buildClassifier
import org.example.project.components.terminal.window.controller.modelLoaders.OrtSessions
import org.example.project.components.terminal.window.controller.modules.MetricsImages
import org.example.project.components.terminal.window.controller.modules.MetricsPlot
import org.example.project.components.terminal.window.controller.modules.MetricsRun
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
import org.example.project.components.terminal.window.controller.modules.renameModule
import org.example.project.components.terminal.window.controller.modules.ModelGroup
import org.example.project.components.terminal.window.controller.modules.ModelGroups
import org.example.project.components.terminal.window.controller.modules.GROUP_NAME
import org.example.project.components.terminal.window.controller.pluginEngine.PackageMeta
import org.example.project.components.terminal.window.controller.pluginEngine.PackageFlag
import org.example.project.components.terminal.window.controller.pluginEngine.PackageMetadata
import org.example.project.components.terminal.window.controller.pluginEngine.PackageTestReport
import org.example.project.components.terminal.window.controller.modules.MODULE_NAME
import org.example.project.settings.AppSettings
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
import org.example.project.components.ui.viewport.AccentButton
import org.example.project.components.ui.viewport.BelowButton
import org.example.project.components.ui.viewport.DropButton
import org.example.project.components.ui.viewport.MenuBox
import org.example.project.components.ui.viewport.AppTooltip
import org.example.project.components.ui.viewport.DangerButton
import org.example.project.components.ui.viewport.IconChip
import org.example.project.components.ui.viewport.SmallButton
import org.example.project.components.ui.viewport.Toggle
import org.example.project.components.ui.viewport.NavigationSidebar
import org.example.project.getDirectory
import org.example.project.ui.theme.ActiveColor
import org.example.project.ui.theme.ChartAccent
import org.example.project.ui.theme.AccentSoft
import org.example.project.ui.theme.DangerColor
import org.example.project.ui.theme.ChipBackground
import org.example.project.ui.theme.ChipBorderColor
import org.example.project.ui.theme.ChipHoverBackground
import org.example.project.ui.theme.CodeBackground
import org.example.project.ui.theme.CompletionBackground
import org.example.project.ui.theme.CompletionBorder
import org.example.project.ui.theme.CompletionSelected
import org.example.project.ui.theme.ConsoleBackground
import org.example.project.ui.theme.ConsoleBorderColor
import org.example.project.ui.theme.EditorBackground
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.IdleColor
import org.example.project.ui.theme.LogErrorColor
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

/** The sections of the page; the tip is what the tab's tooltip says */
private enum class PageTab(val title: String, val icon: ImageVector, val tip: String) {
    MODELS("Models", Icons.Default.Memory, "Intent models: build one from a csv, then validate, serve and publish it"),
    GROUPS("Groups", Icons.Default.Hub, "Model groups: sets of models a script asks together with group(\"name\"), each kept in its env"),
    PACKAGES("Packages", Icons.Default.Inventory2, "Package jars a script opens with initMod(\"name.jar\"), in collections and by where they live"),
    SESSIONS("Sessions", Icons.Default.Sensors, "Everything open right now: model, package and ONNX sessions, each with its log and Terminate"),
}

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

/**
 * The icons a model or a group can carry, by the name kept in module.json or groups.json: a small catalog of
 * plain Material glyphs, nothing branded. The chip (memory) is the default for a model, the hub for a group.
 */
private val iconCatalog: List<Pair<String, ImageVector>> = listOf(
    "memory" to Icons.Default.Memory, "hub" to Icons.Default.Hub, "lightbulb" to Icons.Default.Lightbulb, "cloud" to Icons.Default.Cloud,
    "chat" to Icons.Default.Chat, "home" to Icons.Default.Home, "music" to Icons.Default.MusicNote, "shield" to Icons.Default.Shield,
    "star" to Icons.Default.Star, "bolt" to Icons.Default.Bolt, "heart" to Icons.Default.Favorite, "language" to Icons.Default.Language,
    "science" to Icons.Default.Science, "work" to Icons.Default.Work, "pets" to Icons.Default.Pets, "school" to Icons.Default.School,
    "cart" to Icons.Default.ShoppingCart, "groups" to Icons.Default.Groups,
)

private fun iconNamed(name: String?, default: ImageVector): ImageVector = iconCatalog.firstOrNull { it.first == name }?.second ?: default

private val iconExtensions = listOf("png", "jpg", "jpeg", "svg")

/** The painter of an uploaded icon file, decoded once per file version; null when it does not decode */
@Composable
private fun rememberIconPainter(file: File?): Painter? {
    val density = LocalDensity.current
    return remember(file?.path, file?.lastModified()) {
        file ?: return@remember null
        runCatching {
            if (file.extension.equals("svg", ignoreCase = true)) file.inputStream().use { loadSvgPainter(it, density) }
            else BitmapPainter(file.inputStream().use { loadImageBitmap(it) })
        }.getOrNull()
    }
}

/** Copies the chosen image into the module folder as icon.<ext>, replacing any icon file there; null clears it */
private fun setIconFile(dir: File, image: File?) {
    iconExtensions.forEach { File(dir, "icon.$it").delete() }
    if (image != null) image.copyTo(File(dir, "icon.${image.extension.lowercase()}"), overwrite = true)
}

/** The status words the rows do not show: the green underline says running, its absence idle; only the other states are worth a word */
private val quietStatus = setOf("idle", "running", "stopped", "open", "closed")

/** A coloured tag: text and its colour; the colour names are what a flag in the metadata may say */
private data class Flag(val text: String, val color: Color, val tip: String? = null)

private val flagColors: List<Pair<String, Color>> = listOf(
    "slate" to ChartAccent, "green" to SuccessColor, "amber" to Color(0xFFB08A3E), "red" to DangerColor, "violet" to Color(0xFF8A6FB5), "blue" to PageAccent,
)

private fun flagColor(name: String?): Color = flagColors.firstOrNull { it.first == name }?.second ?: ChartAccent

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
    /** The groups the model is in, as "name" or "name (env)" */
    val groups: List<String> = emptyList(),
) {
    val icon: ImageVector get() = iconNamed(manifest.icon, Icons.Default.Memory)
    /** An image of the model's own, AMP/modules/<name>/icon.(png|jpg|jpeg|svg), which wins over the catalog icon */
    val iconFile: File? get() = iconExtensions.map { File(dir, "icon.$it") }.firstOrNull { it.isFile }
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
            status.lowercase().contains(q) || dataName.lowercase().contains(q) || metrics.any { it.validation.lowercase().contains(q) } ||
            groups.any { it.lowercase().contains(q) }
    }
}

/** A model group as the page shows it: the group, the swarm open on it (if any) and the models it names that exist */
private data class GroupInfo(val group: ModelGroup, val swarm: SessionInfo?, val present: List<String>) {
    val name: String get() = group.name
    val env: String get() = group.env
    val icon: ImageVector get() = iconNamed(group.icon, Icons.Default.Hub)
    val open: Boolean get() = swarm != null
    val missing: List<String> get() = group.models - present.toSet()
    /** "name" in the default env, "name (env)" elsewhere: how a group is told apart across envs */
    val label: String get() = if (env == org.example.project.components.terminal.window.controller.envBuilder.DEFAULT_ENV) name else "$name ($env)"
    fun matches(q: String) = q.isEmpty() || name.lowercase().contains(q) || env.lowercase().contains(q) || group.models.any { it.lowercase().contains(q) } || (if (open) "open" else "closed").contains(q)
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
    /** group:artifact:version for a library added from Maven Central, null for a package jar */
    val coordinate: String? = null,
    /** The version the env actually holds for it, when a dependency asked for a newer one than declared */
    val resolvedCoordinate: String? = null,
    /** What the jar says it needs: inside it (META-INF/amie/package.json) or beside it (<jar>.package.json) */
    val meta: PackageMeta? = null,
    val metaSource: String? = null,
    /** AMP/pluginDir/config/<jar>/, where its configuration files are read from */
    val configDir: File? = null,
    val requirements: List<Requirement> = emptyList(),
    /** The last test run, from <jar>.tests.json */
    val report: PackageTestReport? = null,
    /** Who published it to the website's store, for a jar that is there */
    val author: String? = null,
) {
    val local: Boolean get() = file != null
    val unmet: Int get() = requirements.count { it.ok == false }

    /**
     * What the jar works with, as coloured tags: its version (blue), the Java it needs (slate, red when this app
     * is too old for it), its language (violet), how its tests went (green or red), then the custom flags from
     * its metadata and the ones added on the page.
     */
    val flags: List<Flag> get() = listOfNotNull(
        version?.let { Flag("v$it", flagColor("blue"), "version $it" + (meta?.version?.let { " (from the metadata)" } ?: " (from the manifest)")) },
        (meta?.java ?: contents?.minJava)?.let { j -> Flag("java $j+", if (contents?.runsHere == false || j > Runtime.version().feature()) DangerColor else flagColor("slate"), "needs Java $j or newer; this app runs Java ${Runtime.version().feature()}") },
        contents?.let { Flag(if (it.kotlin) "kotlin" else "java", flagColor("violet"), "written in ${if (it.kotlin) "Kotlin" else "Java"}") },
        report?.let { r -> Flag(if (r.ok) "tests pass" else "tests fail", if (r.ok) SuccessColor else DangerColor, r.summary()) },
    ) + (meta?.flags ?: emptyList()).map { Flag(it.text, flagColor(it.color), "a flag of the jar: ${it.text}") }
    val maven: Boolean get() = coordinate != null
    val open: Int get() = sessions.size
    val status: String get() = when {
        sessions.any { it.running } -> "inferring"
        open > 0 -> "$open open"
        local -> "idle"
        else -> "on server"
    }

    /** Where the jar lives, as a group of the list */
    val location: String get() = when (source) { "server" -> "On the server"; "store" -> "In the store"; "env" -> "In the env"; "maven" -> "From Maven Central"; else -> "In AMP/pluginDir" }

    /** Name, version, Java, collection, entry class, a class or a resource name contain the query */
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        if (name.lowercase().contains(q) || version?.lowercase()?.contains(q) == true || status.lowercase().contains(q) || collections.any { it.lowercase().contains(q) } || coordinate?.lowercase()?.contains(q) == true) return true
        val c = contents ?: return false
        return c.javaSupport.lowercase().contains(q) || c.entryClass?.lowercase()?.contains(q) == true ||
            c.classes.any { it.lowercase().contains(q) } || c.resources.any { it.lowercase().contains(q) }
    }
}

private data class ModelsSnapshot(
    val models: List<ModelInfo> = emptyList(),
    val groups: List<GroupInfo> = emptyList(),
    val jars: List<JarInfo> = emptyList(),
    val sessions: List<SessionInfo> = emptyList(),
    val collections: List<PackageCollection> = emptyList(),
    /** What the env declares from Maven Central, jar or not (a BOM has no jar and so no row) */
    val mavenDeclared: Set<String> = emptySet(),
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
private const val KIND_GROUP = "model group"

/** Reads the local state; with [remote] the server's jar listing too (cached a minute by JarPackages) */
private fun readSnapshot(remote: Boolean): ModelsSnapshot {
    // ORT sessions named module:<name> belong to a model session and are terminated through it
    val sessions = ModelSessions.all.map { s ->
        SessionInfo("model:${s.id}", s.name, s.id, KIND_MODEL, s.running > 0, s.runs, s.openedAt, s.log.lines, true) { ModelSessions.close(s.id) }
    } + JarPackages.open.map { s ->
        SessionInfo("package:${s.id}", s.jarName, s.id, KIND_PACKAGE, s.running > 0, s.runs, s.openedAt, s.log.lines, true) { JarPackages.close(s.id) }
    } + OrtSessions.all.filter { !it.name.startsWith("module:") }.map { s ->
        SessionInfo("ort:${s.id}", s.name, s.id, KIND_ONNX, s.running > 0, s.runs, s.openedAt, emptyList(), false) { OrtSessions.cancel(s.id.toString()) }
    } + ModelGroups.openSwarms.map { s ->
        SessionInfo("group:${s.id}", s.group.name, s.id, KIND_GROUP, s.running > 0, s.runs, s.openedAt, s.log.lines, true) { ModelGroups.closeSwarm(s.id) }
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
            groups = ModelGroups.ofModel(name),
        )
    }
    val groups = ModelGroups.all().map { g ->
        GroupInfo(g, sessions.firstOrNull { it.kind == KIND_GROUP && it.owner == g.name && ModelGroups.openSwarms.any { s -> s.id == it.id && s.group.env == g.env } }, g.models.filter { m -> names.contains(m) })
    }

    // The page reads the default env when no script has opened one yet, so Maven libraries are listed before the first run
    val env = ScriptEnvironments.current ?: ScriptEnvironments.open(DEFAULT_ENV)
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
    // Libraries the env declares from Maven Central: listed by their jar so they can be inspected like any other
    val mavenJars = env.mavenPackages().mapNotNull { pkg ->
        val file = pkg.jar ?: return@mapNotNull null
        if (local.any { it.first.name == file.name }) return@mapNotNull null
        val shown = pkg.resolved ?: pkg.declared
        JarInfo(file.name, "maven", null, MavenCentral.page(shown), MavenCentral.page(shown) ?: page, pkg.resolvedVersion, file, JarInspector.inspect(file), collectionsOf(file.name), sessions.filter { it.kind == KIND_PACKAGE && it.owner == file.name }, coordinate = pkg.declared, resolvedCoordinate = pkg.resolved)
    }
    val jars = (local.map { (file, source) ->
        val origin = JarPackages.origin(file.name)
        val contents = JarInspector.inspect(file)
        val meta = PackageMetadata.read(file)
        JarInfo(
            file.name, source, origin, origin?.url, origin?.page ?: page, meta?.version ?: origin?.version ?: contents?.implementationVersion, file, contents, collectionsOf(file.name),
            sessions.filter { it.kind == KIND_PACKAGE && it.owner == file.name },
            meta = meta, metaSource = PackageMetadata.source(file), configDir = PackageMetadata.configDir(file.name),
            requirements = PackageMetadata.requirements(file, contents, meta, contents?.entryClass), report = PackageMetadata.lastReport(file.name),
        )
    } + mavenJars + remoteJars.map {
        JarInfo(it.name, if (it.where == "store") "store" else "server", null, it.downloadUrl ?: JarPackages.downloadUrl(it.name), it.page ?: page, it.version, null, null, collectionsOf(it.name), sessions.filter { s -> s.kind == KIND_PACKAGE && s.owner == it.name }, author = it.author)
    }).sortedBy { it.name }

    return ModelsSnapshot(models, groups, jars, sessions, collections, env.mavenPackages().map { it.declared }.toSet())
}

/* ---------- page ---------- */

/**
 * The models route: three tabs in a strip like the sandbox's. MODELS: every intent model grouped by the data it
 * was trained on, the running ones first, and the selected one in a panel with its own tabs: metrics as charts,
 * the data it was trained, validated and run on, what it needs to run, the console lines about it, and its
 * sessions; a new one is built from a csv with the form. PACKAGES: the package jars a script can open, in
 * collections (folders) and by where they live, each with what it contains and the Java it needs. SESSIONS: every
 * open session with its own log, each of which can be terminated here. Every button, tab and status word says
 * what it means when the pointer rests on it; every row and panel header has a ⋯ button with the same actions
 * the right-click menu has, so nothing is reachable only by right-click.
 */
@Composable
fun ModelsPage(navController: NavController) {
    val session by AuthState.session.collectAsState()
    // The sidebar can be put away from the tab strip, as the sandbox's can; the choice is kept in AMP/settings.json
    val settings by AppSettings.current.collectAsState()

    CompositionLocalProvider(LocalContextMenuRepresentation provides SquareContextMenu) {
    Row(modifier = Modifier.fillMaxSize().background(EditorBackground)) {
        if (settings.modelsSidebar) NavigationSidebar(
            selectedIndex = -1,
            onIndexSelected = { if (it == 1) navController.navigate("codingSandbox") },
            modelsSelected = true,
            onModels = {},
            settingsSelected = false,
            onSettings = { navController.navigate("settings") },
        )
        ModelsContent(
            user = session?.username,
            onLogin = { navController.navigate("login") { launchSingleTop = true } },
            sidebarShown = settings.modelsSidebar,
            onSidebar = { AppSettings.setModelsSidebar(!settings.modelsSidebar) },
        )
    }
    }
}

@Composable
private fun ModelsContent(user: String?, onLogin: () -> Unit, sidebarShown: Boolean, onSidebar: () -> Unit) {
    var snapshot by remember { mutableStateOf(ModelsCache.snapshot) }
    var refresh by remember { mutableStateOf(0) }
    var notice by remember { mutableStateOf<Notice?>(null) }
    var destroying by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<String?>(null) }
    // The keys of the actions running right now (open:<model>, publish:<jar>, …), so their buttons can say so
    var pending by remember { mutableStateOf(setOf<String>()) }
    var tab by remember { mutableStateOf(PageTab.MODELS) }
    var selectedModel by remember { mutableStateOf<String?>(null) }
    // A group's row selected instead of a model's: "name@env"
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    // The models a new group is being made for (the strip at the top of the page asks for the name and env)
    var grouping by remember { mutableStateOf<List<String>?>(null) }
    // The jar a flag is being added to
    var flagging by remember { mutableStateOf<String?>(null) }
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

    // Runs an action off the UI thread. With a key the action is pending until it ends, so the button that started
    // it can say so (Opening…, Publishing…); a failure becomes a red notice and a console line
    fun background(key: String? = null, block: suspend () -> Unit) = scope.launch {
        if (key != null) pending = pending + key
        withContext(Dispatchers.IO) {
            runCatching { block() }.onFailure {
                logInternal("models: ${it.message}", LogLevel.ERROR)
                notice = Notice(it.message ?: "something went wrong; the Console tab has the details", error = true)
            }
        }
        if (key != null) pending = pending - key
        refresh++
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PageTabs(
            tab = tab,
            counts = mapOf(PageTab.MODELS to snapshot.models.size, PageTab.GROUPS to snapshot.groups.size, PageTab.PACKAGES to snapshot.jars.size, PageTab.SESSIONS to snapshot.sessions.size),
            onTab = { tab = it },
            sidebarShown = sidebarShown,
            onSidebar = onSidebar,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            notice?.let { NoticeBar(it) { notice = null } }
            flagging?.let { jar ->
                FlagBar(
                    jar = jar,
                    onAdd = { text, color ->
                        flagging = null
                        background("flag:$jar") { PackageMetadata.addFlag(jar, PackageFlag(text, color)) }
                    },
                    onCancel = { flagging = null },
                )
            }
            grouping?.let { models ->
                NewGroupBar(
                    models = models,
                    envs = ModelGroups.envs(),
                    taken = snapshot.groups.map { it.name to it.env },
                    onCreate = { name, env ->
                        grouping = null
                        background("group:$name") {
                            val g = ModelGroups.create(name, models, env)
                            selectedGroup = "${g.name}@${g.env}"
                            notice = Notice("${models.joinToString()} ${if (models.size == 1) "is" else "are"} in the group $name of env $env; a script opens the group with group(\"$name\")")
                        }
                    },
                    onCancel = { grouping = null },
                )
            }
            renaming?.let { name ->
                RenameBar(
                    name = name,
                    taken = snapshot.models.map { it.name },
                    onRename = { newName ->
                        renaming = null
                        background("rename:$name") {
                            renameModule(name, newName)
                            selectedModel = newName
                            notice = Notice("$name is now $newName; scripts open it with initMod(\"$newName\")")
                        }
                    },
                    onCancel = { renaming = null },
                )
            }
            destroying?.let { name ->
                ConfirmBar(
                    text = "Destroy $name? Its folder AMP/modules/$name goes, with every run and its logs. A session or listener on it is closed first. This cannot be undone.",
                    confirm = "Destroy $name",
                    onConfirm = {
                        destroying = null
                        if (selectedModel == name) selectedModel = null
                        background("destroy:$name") { if (!deleteModule(name)) error("$name could not be removed entirely; see the console") }
                    },
                    onKeep = { destroying = null },
                )
            }

            when (tab) {
                PageTab.MODELS -> ModelsTab(
                    snapshot = snapshot,
                    build = build,
                    validating = validating,
                    pending = pending,
                    selectedName = selectedModel,
                    signedIn = user != null,
                    creating = creating || (snapshot.models.isEmpty() && build == null),
                    onCreating = { creating = it },
                    onSelect = { selectedModel = it },
                    onIcon = { model, icon -> background("icon:${model.name}") { updateModule(model.name) { it.copy(icon = icon) } } },
                    onIconFile = { model, image -> background("icon:${model.name}") { setIconFile(model.dir, image) } },
                    onAddToGroup = { model, group -> background("group:${group.name}") { ModelGroups.add(group.name, model.name, group.env) } },
                    onNewGroup = { models -> grouping = models },
                    onBuild = { name, data, test, description ->
                        if (build != null) notice = Notice("a build is already running: ${build?.modelName}; wait for it to end")
                        else background("build") {
                            // A csv picked from anywhere else lands in AMP/ first, where the model finds it again by its name
                            val file = DataSource.importIntoAmp(data)
                            val source = DataSource.read(file.absolutePath) ?: error("${file.name} has no text and label columns the builder can use")
                            test?.let { source.relate(DataSource.importIntoAmp(it).absolutePath) }
                            buildClassifier(source.samples(), name, validation = source.related?.samples(), validationName = source.related?.name, dataName = file.name, description = description)
                                ?: error("model '$name' was not built; its Console tab says why")
                            selectedModel = name
                            creating = false
                        }
                    },
                    onRetrain = { model, data ->
                        if (build != null) notice = Notice("a build is already running: ${build?.modelName}; wait for it to end")
                        else background("build") {
                            val picked = data ?: model.dataFile ?: error("${model.name} has no data file in AMP/ to train on again; pick one with Retrain on file…")
                            val file = DataSource.importIntoAmp(picked)
                            val source = DataSource.read(file.absolutePath) ?: error("${file.name} has no text and label columns the builder can use")
                            // The same name: the head is rebuilt in place, the sessions on it reopen on the new one, the
                            // metrics keep every earlier run and the description and guide stay
                            buildClassifier(source.samples(), model.name, validation = source.related?.samples(), validationName = source.related?.name, dataName = file.name, description = model.manifest.description)
                                ?: error("model '${model.name}' was not trained again; its Console tab says why")
                            selectedModel = model.name
                            notice = Notice("${model.name} was trained again on ${file.name}; the Metrics tab has the new run beside the earlier ones")
                        }
                    },
                    onRename = { model -> renaming = model.name },
                    onValidate = { model, data ->
                        if (validating != null) notice = Notice("a validation is already running: ${validating?.model}; wait for it to end")
                        else background("validate:${model.name}") { ModelValidation.validate(model.name, DataSource.importIntoAmp(data)) }
                    },
                    onSave = { model, settings -> background("save:${model.name}") { writeServing(model.name, settings.copy(enabled = model.listening)) } },
                    onDescribe = { model, text -> background("describe:${model.name}") { updateModule(model.name) { it.copy(description = text.trim().ifEmpty { null }) } } },
                    onListen = { model, on, settings -> background("listen:${model.name}") { if (on) ModelServer.start(model.name, settings) else ModelServer.stop(model.name) } },
                    onOpen = { model -> background("open:${model.name}") { ModelSessions.open(model.name) } },
                    onTerminate = { session -> background("terminate:${session.key}") { session.terminate() } },
                    onTerminateModel = { model -> background("terminate:${model.name}") { ModelServer.stop(model.name); model.sessions.forEach { it.terminate() } } },
                    onDestroy = { model -> destroying = model.name },
                    onUnpublish = { model ->
                        val signedIn = user
                        if (signedIn == null) onLogin()
                        else background("unpublish:${model.name}") {
                            val removed = AmieBackend.unpublishModule(model.name, signedIn)
                            notice = Notice(if (removed > 0) "${model.name} was removed from ${AmieBackend.config.base}" else "${model.name} was not on ${AmieBackend.config.base}")
                        }
                    },
                    onPublish = { model ->
                        val signedIn = user
                        if (signedIn == null) onLogin()
                        else background("publish:${model.name}") {
                            val (dir, _) = readModule(model.name) ?: error("module '${model.name}' not found")
                            val published = AmieBackend.publishModule(model.name, dir, signedIn)
                            notice = Notice(when (published.onWebsite) {
                                true -> "${model.name} is on the website (${published.files.joinToString(", ")}); it is listed at ${published.dashboardUrl}"
                                false -> "${model.name} was stored by ${AmieBackend.config.base}, but the website does not list it yet; refresh ${published.dashboardUrl} in a minute"
                                null -> "${model.name} was stored by ${AmieBackend.config.base}; the website at ${AmieBackend.config.site} could not be asked, see ${published.dashboardUrl}"
                            })
                        }
                    },
                )
                PageTab.GROUPS -> GroupsTab(
                    snapshot = snapshot,
                    pending = pending,
                    selectedKey = selectedGroup,
                    onSelect = { selectedGroup = it },
                    onShowModel = { selectedModel = it; tab = PageTab.MODELS },
                    onAddToGroup = { model, group -> background("group:${group.name}") { ModelGroups.add(group.name, model.name, group.env) } },
                    onGroupOpen = { g -> background("open:group:${g.name}") { ModelGroups.open(g.name, g.env) } },
                    onGroupTerminate = { g -> background("terminate:group:${g.name}") { g.swarm?.terminate() } },
                    onGroupRemove = { g, model -> background("group:${g.name}") { ModelGroups.remove(g.name, model, g.env) } },
                    onGroupIcon = { g, icon -> background("group:${g.name}") { ModelGroups.setIcon(g.name, icon, g.env) } },
                    onGroupDelete = { g ->
                        if (selectedGroup == "${g.name}@${g.env}") selectedGroup = null
                        background("group:${g.name}") { ModelGroups.delete(g.name, g.env); notice = Notice("the group ${g.name} is gone; its models stay") }
                    },
                    onTerminate = { session -> background("terminate:${session.key}") { session.terminate() } },
                    onNewGroup = { models -> grouping = models },
                )
                PageTab.PACKAGES -> PackagesTab(
                    snapshot = snapshot,
                    pending = pending,
                    selectedName = selectedJar,
                    onSelect = { selectedJar = it },
                    onAdd = {
                        val chosen = chooseFile("Add jar", listOf("jar")) ?: return@PackagesTab
                        background("addjar") { addJar(chosen); selectedJar = chosen.name }
                    },
                    onFetch = { jar -> background("fetch:${jar.name}") { JarPackages.fetch(jar.name) ?: error("${jar.name} is not on the server") } },
                    onTest = { jar ->
                        background("test:${jar.name}") {
                            val report = JarPackages.test(jar.name)
                            notice = Notice(report.summary() + "; the TESTS block of the jar has every answer", error = !report.ok)
                        }
                    },
                    onFlag = { jar -> flagging = jar.name },
                    onRemoveFlag = { jar, text -> background("flag:${jar.name}") { PackageMetadata.removeFlag(jar.name, text) } },
                    onTemplate = { jar ->
                        background("template:${jar.name}") {
                            val file = PackageMetadata.writeTemplate(jar.name, jar.contents?.entryClass, jar.contents?.minJava)
                            notice = Notice("${file.name} written beside the jar: say there which config files, variables and Java it needs and what its test runs expect")
                        }
                    },
                    onMaven = { coordinate ->
                        background("maven:$coordinate") {
                            val env = ScriptEnvironments.current ?: ScriptEnvironments.open(DEFAULT_ENV)
                            env.import(coordinate)
                            val pkg = env.mavenPackages().firstOrNull { it.declared == coordinate }
                            val added = pkg?.jar
                            if (added == null) {
                                // Not on Maven Central: it must not stay declared, or every run would try it again. A POM-only
                                // artifact (a BOM) did resolve, it just has no jar to list
                                if (pkg?.resolved == null) env.remove(coordinate)
                                error(if (pkg?.resolved != null) "$coordinate is in the env but has no jar (a POM-only artifact, such as a BOM)" else "$coordinate could not be resolved from Maven Central; the Console tab has the details")
                            }
                            selectedJar = added.name
                            notice = Notice(
                                if (pkg.resolved != null && pkg.resolved != coordinate) "$coordinate is in the env as ${pkg.resolvedVersion}: a dependency already asks for that newer version"
                                else "$coordinate is in the env; a script can import its classes from the next run"
                            )
                        }
                    },
                    onRemoveMaven = { jar ->
                        val coordinate = jar.coordinate ?: return@PackagesTab
                        background("maven-remove:$coordinate") {
                            val env = ScriptEnvironments.current ?: ScriptEnvironments.open(DEFAULT_ENV)
                            notice = Notice(if (env.remove(coordinate)) "$coordinate was removed from the env; its files stay in AMP/env/caches" else "$coordinate was not declared in the env")
                        }
                    },
                    onPublish = { jar ->
                        val signedIn = user
                        val file = jar.file
                        when {
                            signedIn == null -> onLogin()
                            file == null -> notice = Notice("${jar.name} is on the server already; Fetch it first to publish a copy")
                            else -> background("publish:${jar.name}") {
                                val published = AmieBackend.publishJar(file, signedIn)
                                JarPackages.available(refresh = true)
                                notice = Notice(when (published.onWebsite) {
                                    true -> "${jar.name} is on the website; it is listed at ${published.dashboardUrl}"
                                    false -> "${jar.name} was stored by ${AmieBackend.config.base}, but the website does not list it yet; refresh ${published.dashboardUrl} in a minute"
                                    null -> "${jar.name} was stored by ${AmieBackend.config.base}; the website at ${AmieBackend.config.site} could not be asked, see ${published.dashboardUrl}"
                                })
                            }
                        }
                    },
                    signedIn = user != null,
                    onTerminate = { session -> background("terminate:${session.key}") { session.terminate() } },
                    onCollection = { action -> background { action() } },
                )
                PageTab.SESSIONS -> SessionsTab(
                    sessions = snapshot.sessions,
                    onTerminate = { session -> background("terminate:${session.key}") { session.terminate() } },
                    onTerminateAll = { background("terminate:all") { ModelSessions.closeAll(); JarPackages.closeAll(); OrtSessions.cancelAll() } },
                )
            }
        }

        MemoryFooter()
    }
}

/**
 * The strip at the foot of the page: what the process holds of the machine's RAM as a bar, the heap against its
 * limit, and the encoders in memory, the heavy part that lives outside the heap and is shared by the sessions on
 * it. Read every two seconds; the bar turns red near the limit, where the next session or build runs out.
 */
@Composable
private fun MemoryFooter() {
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
        AppTooltip(
            "What this process holds of the machine's RAM (the bar), the Java heap against its -Xmx limit, the native memory " +
                "outside the heap, and the BERT encoders loaded: the heavy part, shared by every session, listener, validation " +
                "and build on them. It turns red near the limit; then close sessions and stop listeners you don't need."
        ) {
        Row(modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("RAM", color = if (tight) DangerColor else MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif, letterSpacing = 1.sp)
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.width(100.dp).height(5.dp).background(PanelBorder, RectangleShape)) {
                if (share >= 0) Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(share.toFloat().coerceIn(0f, 1f)).background(if (tight) DangerColor else ChartAccent, RectangleShape))
            }
            Spacer(Modifier.width(10.dp))
            if (s == null) Meta("…")
            else {
                Meta(
                    listOfNotNull(
                        if (s.residentBytes > 0 && s.physicalBytes > 0) "${bytes(s.residentBytes)} of ${bytes(s.physicalBytes)}" else null,
                        "heap ${bytes(s.heapUsedBytes)} / ${bytes(s.heapMaxBytes)}",
                        "native ${bytes(s.nonHeapUsedBytes)}",
                        if (encoders.isNotEmpty()) "${encoders.size} encoder${if (encoders.size == 1) "" else "s"} ${bytes(encoderBytes)}" else null,
                    ).joinToString(" · ")
                )
                if (tight) {
                    Spacer(Modifier.width(12.dp))
                    Text("near the limit", color = DangerColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
                }
            }
        }
        }
    }
}

/** The strip under the window header: the sidebar's chip right after the tabs, one tab per section with its count */
@Composable
private fun PageTabs(tab: PageTab, counts: Map<PageTab, Int?>, onTab: (PageTab) -> Unit, sidebarShown: Boolean, onSidebar: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(TabBarBackground)) {
        Row(modifier = Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
            // The first thing on the strip, right where the sidebar it hides and shows meets it
            Spacer(Modifier.width(6.dp))
            IconChip(Icons.Default.ViewSidebar, tip = if (sidebarShown) "Hide the navigation sidebar" else "Show the navigation sidebar", active = sidebarShown) { onSidebar() }
            Spacer(Modifier.width(6.dp))
            PageTab.entries.forEach { t -> TabCell(t.title, t.icon, counts[t], selected = t == tab, tip = t.tip) { onTab(t) } }
            Spacer(Modifier.weight(1f))
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(UnifiedBorderColor))
    }
}

@Composable
private fun TabCell(title: String, icon: ImageVector?, count: Int?, selected: Boolean, tip: String? = null, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    AppTooltip(tip) { Column(
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
    } }
}

/* ---------- MODELS ---------- */

@Composable
private fun ColumnScope.ModelsTab(
    snapshot: ModelsSnapshot,
    build: ModelBuild?,
    validating: ValidationProgress?,
    pending: Set<String>,
    selectedName: String?,
    signedIn: Boolean,
    creating: Boolean,
    onCreating: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    /** Sets the model's icon (a catalog name, null for the default) in module.json */
    onIcon: (ModelInfo, String?) -> Unit,
    onIconFile: (ModelInfo, File?) -> Unit,
    onAddToGroup: (ModelInfo, GroupInfo) -> Unit,
    /** Opens the strip that makes a new group with these models */
    onNewGroup: (List<String>) -> Unit,
    onBuild: (String, File, File?, String) -> Unit,
    /** Trains the model again under the same name: on the given csv, or on the one it was trained on */
    onRetrain: (ModelInfo, File?) -> Unit,
    onRename: (ModelInfo) -> Unit,
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

    // The actions of a model: one list for its row's right-click, its ⋯ button and its panel header
    fun menuOf(model: ModelInfo): List<ContextMenuItem> = listOfNotNull(
        if (model.sessions.isEmpty()) ContextMenuItem("Open session") { onOpen(model) } else null,
        ContextMenuItem(if (model.listening) "Stop listening" else "Listen on :${model.serving.port}") { onListen(model, !model.listening, model.serving) },
        ContextMenuItem("Validate on sample data") { onValidate(model, ensureSampleData()) },
        ContextMenuItem("Validate on file…") { chooseFile("Validation data", listOf("csv"))?.let { onValidate(model, it) } },
        if (model.dataFile != null && build == null) ContextMenuItem("Retrain on ${model.dataFile.name}") { onRetrain(model, null) } else null,
        if (build == null) ContextMenuItem("Retrain on file…") { chooseFile("Training data", listOf("csv"))?.let { onRetrain(model, it) } } else null,
        ContextMenuItem("Rename…") { onRename(model) },
    ) + snapshot.groups.filter { model.name !in it.group.models }.map { g -> ContextMenuItem("Add to group ${g.label}") { onAddToGroup(model, g) } } + listOfNotNull(
        ContextMenuItem("New group with ${model.name}…") { onNewGroup(listOf(model.name)) },
        ContextMenuItem(if (signedIn) "Publish to the website" else "Publish to the website (sign in)") { onPublish(model) },
        ContextMenuItem(if (signedIn) "Remove from the website" else "Remove from the website (sign in)") { onUnpublish(model) },
        ContextMenuItem("Reveal folder") { openInBrowser(model.dir.toURI().toString()) },
        ContextMenuItem("Copy initMod(\"${model.name}\")") { clipboard.setText(AnnotatedString("initMod(\"${model.name}\")")) },
        if (model.running) ContextMenuItem("Terminate model") { onTerminateModel(model) } else null,
        ContextMenuItem("Destroy model…") { onDestroy(model) },
    )

    // The list is grouped by the data the models were trained on, so the same data trained twice sits together;
    // groups with a running model come first, and inside a group the running ones, then the most accurate
    var query by remember { mutableStateOf("") }
    val shown = snapshot.models.filter { it.matches(query) }
    val ordered = shown.sortedWith(compareByDescending<ModelInfo> { it.running }.thenByDescending { it.accuracy }.thenBy { it.name })
    // A model dragged onto another model: the two make a new group; onto the zone over the list, a new group with it (the Groups tab has the groups)
    val drag = remember { DragState { model, target ->
        when {
            target == "new-group" -> onNewGroup(listOf(model))
            target.startsWith("model:") -> onNewGroup(listOf(model, target.removePrefix("model:")))
        }
    } }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(modifier = Modifier.fillMaxWidth().weight(1f).onGloballyPositioned { overlayOrigin = it.positionInRoot() }) {
    val selected = snapshot.models.firstOrNull { it.name == selectedName } ?: ordered.firstOrNull()
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        var listWidth by remember { mutableStateOf(ModelsCache.listWidth) }
        Column(modifier = Modifier.width(listWidth.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // The filter and the one action of the list on a single line; the form opens in the panel on the right
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.weight(1f)) { SearchField(query, tip = "Filter the models by name, label, status, training data or validation file") { query = it } }
                if (build == null) AccentButton(
                    if (creating) "Close" else "New model",
                    icon = if (creating) Icons.Default.Close else Icons.Default.Add,
                    tip = if (creating) "Close the form" else "Build a model from a csv with a text column and a label column; the sample data works too",
                ) { onCreating(!creating) }
            }
            build?.let { BuildLine(it) }
            if (drag.dragging != null) NewGroupDropZone(drag)
            if (shown.isEmpty()) { if (query.isNotBlank()) NoMatches(query) { query = "" } else Hint("no models yet") }
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
                                    if (model.listening) "serving :${model.serving.port}" else null,
                                    when (model.sessions.size) { 0 -> null; 1 -> "1 session"; else -> "${model.sessions.size} sessions" },
                                    if (model.groups.isNotEmpty()) "in ${model.groups.joinToString()}" else null,
                                ).joinToString(" · "),
                                status = model.status,
                                statusTip = statusTip(model),
                                underline = model.running,
                                icon = model.icon,
                                painter = rememberIconPainter(model.iconFile),
                                menu = menuOf(model),
                                drag = drag,
                                dragKey = model.name,
                                dropKey = "model:${model.name}",
                            ) { onSelect(model.name) }
                        }
                    }
                }
            }
        }
        VerticalSeparator { listWidth = it; ModelsCache.listWidth = it }
        when {
            creating && build == null -> NewModelPanel(Modifier.weight(1f).fillMaxHeight(), onBuild = onBuild, onClose = { onCreating(false) })
            selected != null -> ModelDetail(
                model = selected,
                signedIn = signedIn,
                validating = validating?.takeIf { it.model == selected.name },
                pending = pending,
                menu = menuOf(selected),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onValidate = { data -> onValidate(selected, data) },
                onRetrain = { data -> onRetrain(selected, data) },
                onIcon = { onIcon(selected, it) },
                onIconFile = { onIconFile(selected, it) },
                building = build != null,
                onSave = { onSave(selected, it) },
                onDescribe = { onDescribe(selected, it) },
                onListen = { on, settings -> onListen(selected, on, settings) },
                onOpen = { onOpen(selected) },
                onTerminate = onTerminate,
                onPublish = { onPublish(selected) },
            )
            else -> Spacer(Modifier.weight(1f))
        }
    }
    // The ghost: the dragged model's name beside the pointer, and what dropping would do
    drag.dragging?.let { name ->
        val action = when (val t = drag.over) {
            null -> "drop on a model to pair them, or on the zone over the list"
            "new-group" -> "new group with $name"
            else -> "new group with ${t.removePrefix("model:")}"
        }
        Text(
            "$name · $action",
            color = FontColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
            modifier = Modifier
                .offset { (drag.position - overlayOrigin).round() + IntOffset(14, 10) }
                .background(PanelSurfaceSelected, RectangleShape)
                .border(1.dp, PageAccent, RectangleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
    }
}

/** What a model's status word means right now, for the tooltip on it */
private fun statusTip(model: ModelInfo): String = when {
    model.busy -> "inferring: answering a request right now"
    model.running -> "running: " + listOfNotNull(
        if (model.listening) "serving on ${model.serving.host}:${model.serving.port}" else null,
        when (model.sessions.size) { 0 -> null; 1 -> "one session open"; else -> "${model.sessions.size} sessions open" },
    ).joinToString(" and ")
    else -> "stopped: not serving and no session open. Open session or Listen starts it"
}

/**
 * Builds a model from a data file, in the place of the detail panel: name, a sentence about it, the csv with text
 * and label columns (or the sample data), an optional test file. What the file holds is read as soon as it is
 * chosen so the labels are visible before Build; while Build is disabled the line beside it says what is missing.
 * The build itself is the same `buildClassifier` the sandbox's modelBuilder runs; the line at the top of the
 * list follows it.
 */
@Composable
private fun NewModelPanel(modifier: Modifier, onBuild: (String, File, File?, String) -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var data by remember { mutableStateOf<File?>(null) }
    var test by remember { mutableStateOf<File?>(null) }
    var preview by remember { mutableStateOf<String?>(null) }
    // The preprocessor's report on the chosen file, on request
    var report by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(data) {
        report = null
        preview = data?.let { f ->
            withContext(Dispatchers.IO) {
                runCatching { DataSource.read(f.absolutePath) }.getOrNull()?.let { s ->
                    val samples = s.samples()
                    "${samples.size} samples · ${s.labels.size} labels: ${s.labels.joinToString()}"
                } ?: "no text and label columns in ${f.name}"
            }
        }
    }
    // What still keeps Build from running, so the disabled button never has to be guessed at
    val chosen = data
    val read = preview
    val missing = when {
        chosen == null -> "choose a data file, or take the sample data"
        read == null -> "reading ${chosen.name}…"
        read.startsWith("no ") -> read
        name.isBlank() -> "give the model a name"
        else -> null
    }
    val ready = missing == null

    DetailPanel(modifier) {
        DetailHeader(name = "New model", active = false, status = "a csv with a text column and a label column", mark = false) {
            SmallButton("Close", icon = Icons.Default.Close, tip = "Close the form; nothing is kept", onClick = onClose)
        }
        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(top = 12.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingField("Name", name) { name = it.filter { c -> c.isLetterOrDigit() || c in "._-" } }
            SettingField("About", description) { description = it }
            fun pick(f: File) {
                data = f
                if (name.isBlank()) name = f.nameWithoutExtension.filter { c -> c.isLetterOrDigit() || c in "._-" }
            }
            FileField("Data", data, "csv with text and label columns", sample = { pick(ensureSampleData()) }, chooseTip = "Pick the csv the model learns from", sampleTip = "Use AMP/sample-intents.csv: 45 rows over greeting, lights and weather") {
                chooseFile("Training data", listOf("csv"))?.let { pick(it) }
            }
            FileField("Test", test, "optional, else a 20% split", onClear = { test = null }, chooseTip = "Pick a csv to score the model on instead of holding out 20% of the data") {
                chooseFile("Test data", listOf("csv"))?.let { test = it }
            }
            Spacer(Modifier.height(6.dp))
            report?.let { lines ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    lines.forEach { line -> Text(line, color = if (line.startsWith("error:")) DangerColor else if (line.startsWith("warning:")) FontColor else MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Hint(missing ?: read ?: "")
                Spacer(Modifier.weight(1f))
                if (chosen != null && read != null && !read.startsWith("no ")) SmallButton("Check data", icon = Icons.Default.FactCheck, tip = "Run the preprocessor on the file without training: what it would clean, drop and refuse") {
                    report = runCatching { DataSource.read(chosen.absolutePath) }.getOrNull()?.let { s ->
                        val r = org.example.project.components.terminal.window.controller.jetbrainDLBuilder.Preprocessor.run(s.samples())
                        r.lines() + (if (r.ok) "a build can start on ${r.samples.size} rows" else "a build would stop here")
                    } ?: listOf("error: ${chosen.name} could not be read")
                }
                Spacer(Modifier.width(6.dp))
                AccentButton(
                    "Build",
                    enabled = ready,
                    icon = Icons.Default.PlayArrow,
                    tip = missing ?: "Embed every row with the encoder, fit the head, validate it and export it; runs in the background, the line at the top of the list follows it",
                ) { onBuild(name.trim(), chosen!!, test, description) }
            }
        }
    }
}

/** The tabs of a model's panel; the tip is what the tab's tooltip says */
private enum class DetailTab(val title: String, val icon: ImageVector, val tip: String) {
    ABOUT("About", Icons.Default.Info, "What the model is: its description, where it lives, its encoder and head, and how it was trained"),
    GUIDE("Guide", Icons.Default.MenuBook, "The guide shipped inside the model (guide.md); it can be edited here and goes along when published"),
    METRICS("Metrics", Icons.Default.BarChart, "The scores of every validation run as charts: per label, the confusion matrix, and how they changed over the runs"),
    DATA("Data", Icons.Default.TableChart, "The csv files it was trained and validated on, and what it has run on since"),
    REQUIREMENTS("Requirements", Icons.Default.Checklist, "What it needs to run, checked on this machine: encoder, head, labels, runtime, Java, memory and port"),
    CONSOLE("Console", Icons.Default.Terminal, "The console lines that name this model"),
    SESSIONS("Sessions", Icons.Default.Sensors, "The sessions this model has open, with their logs"),
}

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
    pending: Set<String>,
    menu: List<ContextMenuItem>,
    modifier: Modifier = Modifier,
    onValidate: (File) -> Unit,
    onRetrain: (File?) -> Unit,
    onIcon: (String?) -> Unit,
    /** An image of the model's own as its icon, copied into its folder; null removes it */
    onIconFile: (File?) -> Unit,
    /** A build is running, so Retrain waits */
    building: Boolean,
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
    val url = settings.url(model.name)
    // The slow actions say so on their button while they run
    val opening = "open:${model.name}" in pending
    val switching = "listen:${model.name}" in pending
    val publishing = "publish:${model.name}" in pending
    val terminating = "terminate:${model.name}" in pending || model.sessions.any { "terminate:${it.key}" in pending }
    val saving = "save:${model.name}" in pending
    var justSaved by remember(model.name) { mutableStateOf(false) }
    var wasSaving by remember(model.name) { mutableStateOf(false) }
    LaunchedEffect(saving) {
        val was = wasSaving
        wasSaving = saving
        if (was && !saving) { justSaved = true; delay(1500); justSaved = false }
    }

    DetailPanel(modifier) {
        DetailHeader(
            name = model.name,
            icon = model.icon,
            painter = rememberIconPainter(model.iconFile),
            active = model.running,
            status = listOfNotNull(
                model.status.takeIf { it !in quietStatus },
                if (model.listening) "listening ${model.serving.host}:${model.serving.port}" else "not serving",
                "trained on ${model.dataName}",
                if (model.groups.isNotEmpty()) "in ${model.groups.joinToString()}" else null,
            ).joinToString(" · "),
            statusTip = statusTip(model),
            menu = menu,
        ) {
            if (model.sessions.isEmpty()) AccentButton(if (opening) "Opening…" else "Open session", enabled = !opening, icon = Icons.Default.PlayArrow, tip = "Load the model into memory as a session, the same as initMod(\"${model.name}\") in a script. A model has one session at most; Sessions lists it", onClick = onOpen)
            else DangerButton(if (terminating) "Terminating…" else "Terminate session", enabled = !terminating, icon = Icons.Default.StopCircle, tip = "Close the model's session; its memory is freed once nothing else uses the encoder") { model.sessions.forEach(onTerminate) }
            if (model.listening) DangerButton(if (switching) "Stopping…" else "Stop", enabled = !switching, icon = Icons.Default.Stop, tip = "Stop serving the model at ${model.serving.host}:${model.serving.port}") { onListen(false, settings) }
            else SmallButton(if (switching) "Starting…" else "Listen on :${settings.port}", enabled = !switching, icon = Icons.Default.Sensors, tip = "Serve the model over HTTP at $url: POST {\"text\": \"…\"} or GET ?text=… answers with the scores. The gear sets the address") { onListen(true, settings) }
            IconChip(Icons.Default.Tune, tip = "Where Listen serves the model: host, port, path and methods", active = configuring) { configuring = !configuring }
            SmallButton(
                when { publishing -> "Publishing…"; signedIn -> "Publish"; else -> "Publish (sign in)" },
                enabled = !publishing,
                icon = Icons.Default.CloudUpload,
                tip = if (signedIn) "Upload the model to the Amie server under your account as <name>.onnx, <name>.module.json and <name>.guide.md; the website lists it" else "Publish uploads the model under your account, so it opens the sign-in page first",
                onClick = onPublish,
            )
        }

        if (configuring) Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(top = 8.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingField("Host", host) { host = it }
            SettingField("Port", port) { port = it.filter(Char::isDigit).take(5) }
            val portOk = port.trim().toIntOrNull()?.let { it in 1..65535 } == true
            if (!portOk) Text("the port must be a number from 1 to 65535", color = LogErrorTextColor, fontSize = 11.sp, modifier = Modifier.padding(start = 56.dp))
            SettingField("Path", path) { path = it }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Text("Methods", color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.width(56.dp))
                Toggle("POST", allowPost, tip = "Accept POST with the JSON body {\"text\": \"…\"}") { allowPost = it }
                Spacer(Modifier.width(6.dp))
                Toggle("GET", allowGet, tip = "Accept GET with ?text=…") { allowGet = it }
                Spacer(Modifier.weight(1f))
                if (justSaved) { Meta("saved"); Spacer(Modifier.width(6.dp)) }
                SmallButton(if (saving) "Saving…" else "Save", enabled = portOk && !saving, icon = Icons.Default.Save, tip = if (portOk) "Write host, port, path and methods to serving.json" else "Fix the port first") { onSave(settings) }
            }
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

        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(top = 10.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (tab) {
                DetailTab.ABOUT -> AboutBlock(model, onDescribe, onIcon, onIconFile)
                DetailTab.GUIDE -> GuideBlock(model)
                DetailTab.METRICS -> MetricsBlock(model, validating, onValidate)
                DetailTab.DATA -> DataBlock(model, building, onRetrain)
                DetailTab.REQUIREMENTS -> RequirementsBlock(model)
                DetailTab.CONSOLE -> ConsoleBlock(model.name, consoleLines)
                DetailTab.SESSIONS -> SessionsOf(model.sessions, onTerminate, title = false)
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
                AppTooltip(t.tip) { Column(
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
                } }
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
private fun AboutBlock(model: ModelInfo, onDescribe: (String) -> Unit, onIcon: (String?) -> Unit, onIconFile: (File?) -> Unit) {
    val m = model.manifest
    var text by remember(model.name, m.description) { mutableStateOf(m.description ?: "") }
    val dirty = text.trim() != (m.description ?: "")
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
            if (text.isEmpty()) Text("what this model is for…", color = MutedFontColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
            inner()
        },
    )
    if (dirty) Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1f))
        AccentButton("Save description", icon = Icons.Default.Save, tip = "Write the description to module.json") { onDescribe(text) }
    }

    Section("ICON", key = "${model.name}:icon", actions = {
        SmallButton("Upload image", icon = Icons.Default.Image, tip = "Use an image of your own (png, jpg or svg); it is copied into the model's folder as icon.<ext> and shown in the list, on this header and in groups") {
            chooseFile("Icon image", iconExtensions)?.let { onIconFile(it) }
        }
        if (model.iconFile != null) { Spacer(Modifier.width(4.dp)); SmallButton("Remove image", icon = Icons.Default.Delete, tip = "Back to the catalog icon; ${model.iconFile!!.name} is deleted from the model's folder") { onIconFile(null) } }
    }) {
        model.iconFile?.let { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                rememberIconPainter(f)?.let { IconImage(it, 36.dp) }
                Spacer(Modifier.width(8.dp))
                Meta("${f.name} · ${f.length() / 1024} KB · in the model's folder, goes along when published")
            }
        }
        Hint(if (model.iconFile != null) "the catalog icon below is used where the image cannot be drawn" else "the glyph of the model in the list and on this header, kept in module.json; or upload an image")
        IconPicker(chosen = m.icon, default = "memory") { onIcon(it) }
    }

    Section("WHERE", key = "${model.name}:where") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(model.dir.absolutePath, color = LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            SmallButton("Reveal folder", icon = Icons.Default.FolderOpen, tip = "Show the model's folder in the file browser") { openInBrowser(model.dir.toURI().toString()) }
        }
        KeyValues(
            "created" to m.createdAt.take(19).replace('T', ' '),
            "encoder" to "${File(m.encoder).name}  ·  ${m.encoder}",
            "head" to "${m.model}  ·  ${m.format}  ·  ${m.input} [${m.inputDim}] → ${m.output} [${m.outputDim}]",
            "labels" to m.labels.joinToString().ifEmpty { "—" },
        )
    }

    Section("TRAINING", key = "${model.name}:training") {
        if (m.epochs == null && m.architecture == null) Hint("not recorded (built before the training details were kept)")
        KeyValues(
            *listOfNotNull(
                m.architecture?.let { "architecture" to it },
                "data" to (m.data ?: if (model.name == "dummy") "none: written, not trained" else "unknown"),
                m.trainRows?.let { "train rows" to it.toString() },
                m.validationRows?.let { "validation" to "$it rows of ${m.validation ?: "a split"}" },
                m.epochs?.let { "epochs" to it.toString() },
                m.batchSize?.let { "batch size" to it.toString() },
                m.learningRate?.let { "learning rate" to it.toString() },
                m.hiddenDim?.let { "hidden units" to it.toString() },
                m.finalLoss?.let { "final loss" to fmt(it) },
                m.trainAccuracy?.let { "train accuracy" to pct(it) },
                m.search?.let { "heads tried" to it },
            ).toTypedArray()
        )
        val lastRun = model.lastRun
        if (lastRun != null && lastRun.preprocessing.isNotEmpty()) {
            Hint("What the preprocessor did to the rows before the last build:")
            KeyValues(*lastRun.preprocessing.mapIndexed { n, line -> (if (n == 0) "rows" else "") to line }.toTypedArray())
        }
    }
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
        Meta(if (stored == null) "default guide, not saved yet" else file.name)
        Spacer(Modifier.weight(1f))
        if (editing) {
            AccentButton("Save", icon = Icons.Default.Save, tip = "Write the guide to ${file.name}; it goes along when the model is published") { runCatching { file.writeText(text) }.onFailure { logInternal("guide: couldn't write ${file.name}: ${it.message}", LogLevel.ERROR) }; editing = false }
            Spacer(Modifier.width(6.dp))
            SmallButton("Cancel", tip = "Drop the changes") { text = stored ?: defaultGuide(model.manifest); editing = false }
        } else SmallButton(if (stored == null) "Edit and save" else "Edit", tip = "Edit the guide as markdown: # headings, - bullets, ``` code") { editing = true }
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
    var index by remember(model.name) { mutableStateOf(runs.lastIndex) }
    // A new run (after Validate) is shown at once; otherwise the chosen one stays
    var seen by remember(model.name) { mutableStateOf(runs.size) }
    if (runs.size != seen || index !in runs.indices) { seen = runs.size; index = runs.lastIndex }
    fun plot(name: String) = model.plots.firstOrNull { it.file.name == name }

    // One line: the runs as chips (the latest first, each saying what it was validated on when hovered) and Validate
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (runs.isEmpty()) {
            Hint("no validation yet")
            Spacer(Modifier.weight(1f))
        } else {
            Text("run", color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(32.dp))
            FlowRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                runs.indices.reversed().forEach { i ->
                    val r = runs[i]
                    Toggle("${i + 1}", i == index, tip = "run ${i + 1}: ${r.validationSize} rows of ${r.validation}, accuracy ${pct(r.accuracy)}, ${r.createdAt.take(16).replace('T', ' ')}") { index = i }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        if (validating != null) Meta("validating ${validating.done}/${validating.total}")
        else DropButton(
            "Validate",
            icon = Icons.Default.FactCheck,
            tip = "Score the model on a csv and add a run with its charts: the sample data (45 rows: greeting, lights, weather) or a file of yours",
            items = listOf(
                ContextMenuItem("On the sample data") { onValidate(ensureSampleData()) },
                ContextMenuItem("On a file…") { chooseFile("Validation data", listOf("csv"))?.let(onValidate) },
            ),
        )
    }

    if (runs.isNotEmpty()) {
        val run = runs[index]
        KeyValues(
            "when" to run.createdAt.take(19).replace('T', ' '),
            "on" to "${run.validationSize} rows of ${run.validation}${if (run.trainSize > 0) " · trained on ${run.trainSize}" else ""}",
            "scores" to "accuracy ${pct(run.accuracy)}  ·  macro F1 ${fmt(run.macroF1)}  ·  ${run.labels.size} labels: ${run.labels.keys.joinToString()}",
        )
        run.warnings.forEach { Hint("warning: $it") }
        if (run.candidates.isNotEmpty()) Section("HEADS COMPARED", key = "${model.name}:candidates:$index") {
            Hint("every head trained on the same rows and scored on the same ${run.validationSize} validation rows; the chosen one was validated once more and is the model")
            CandidatesTable(run.candidates)
        }
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
    if (session.isNotEmpty()) Section("FROM THE SESSIONS", key = "${model.name}:session-plots") {
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
private fun DataBlock(model: ModelInfo, building: Boolean, onRetrain: (File?) -> Unit) {
    val files = listOfNotNull(model.dataFile?.let { "trained on" to it }, model.validationFile?.takeIf { it != model.dataFile }?.let { "validated on" to it })
    Section("FILES", key = "${model.name}:files", actions = {
        // The model trained again: on its data as it is now (edited rows, new rows), or on another csv, under the same
        // name, so nothing that opens it has to change
        if (model.dataFile == null) SmallButton(if (building) "Building…" else "Retrain on file…", enabled = !building, icon = Icons.Default.Refresh, tip = "Pick a csv and train ${model.name} again on it under the same name; the metrics keep every earlier run") {
            chooseFile("Training data", listOf("csv"))?.let { onRetrain(it) }
        }
    }) {
        if (files.isEmpty()) Hint(if (model.manifest.data != null) "${model.manifest.data} is not in AMP/ any more" else "no data file (the dummy is written, not trained)")
        files.forEach { (role, file) ->
            val facts = rememberCsv(file)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(role, color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(92.dp))
                Text(file.name, color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(10.dp))
                Meta(facts?.let { "${it.rows.size} rows · ${it.labels.size} labels" } ?: "no text and label columns")
                Spacer(Modifier.weight(1f))
                if (role == "trained on") {
                    SmallButton(if (building) "Building…" else "Retrain", enabled = !building, icon = Icons.Default.Refresh, tip = "Train ${model.name} again on ${file.name} as it is now: edit or add rows to the file, then press this. The head is rebuilt under the same name, its sessions reopen on the new one, and the Metrics tab keeps every earlier run beside the new one") { onRetrain(file) }
                    Spacer(Modifier.width(4.dp))
                    SmallButton("Other file…", enabled = !building, icon = Icons.Default.FolderOpen, tip = "Train ${model.name} again on another csv; it is copied into AMP/ and becomes the model's data") { chooseFile("Training data", listOf("csv"))?.let { onRetrain(it) } }
                    Spacer(Modifier.width(4.dp))
                }
                SmallButton("Open in viewer", icon = Icons.Default.OpenInNew, tip = "Open ${file.name} in whatever this machine uses for csv files") { openFile(file) }
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

    }
    val logFile = SessionRuns.logFile(model.dir)
    val logged = remember(model.name, logFile.lastModified()) { SessionRuns.read(model.dir, limit = 2000) }
    val incomingFile = SessionRuns.incomingFile(model.dir)
    val incoming = remember(model.name, incomingFile.lastModified()) { SessionRuns.incoming(model.dir) }
    Section("RAN ON", key = "${model.name}:ran-on") {
        if (logged.isEmpty() && incoming == 0) Hint("nothing yet")
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
    if (checks == null) { Hint("checking…"); return }
    val unmet = checks.count { it.ok == false }
    Hint(if (unmet == 0) "all met" else "$unmet not met")
    RequirementRows(checks)
}

/** Requirement lines: a mark (met, not met, information), the name and the detail */
@Composable
private fun RequirementRows(checks: List<Requirement>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        checks.forEachIndexed { i, r ->
            if (i > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PanelBorder))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                AppTooltip(when (r.ok) { true -> "met"; false -> "not met"; null -> "information, nothing to fix" }) {
                    Box(modifier = Modifier.width(18.dp).height(16.dp), contentAlignment = Alignment.CenterStart) {
                        when (r.ok) {
                            true -> Icon(Icons.Default.CheckCircle, contentDescription = "met", tint = SuccessColor, modifier = Modifier.size(12.dp))
                            false -> Icon(Icons.Default.Cancel, contentDescription = "not met", tint = DangerColor, modifier = Modifier.size(12.dp))
                            null -> Icon(Icons.Default.Info, contentDescription = "information", tint = MutedFontColor, modifier = Modifier.size(12.dp))
                        }
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
        Meta("${lines.size} lines")
        Spacer(Modifier.weight(1f))
        Toggle("this model", !all, tip = "Only the lines that name $name") { all = false }
        Spacer(Modifier.width(6.dp))
        Toggle("all models", all, tip = "Everything the models, packages, sessions, builds and validations logged") { all = true }
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
        if (lines.isEmpty()) Meta(if (all) "nothing yet" else "nothing about $name yet")
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
            if (table.size > 21) Meta("… ${table.size - 21} more rows")
        }
    }
}

/** A model while modelBuilder builds it, at the top of the list: its name, the phase, and a thin progress bar */
@Composable
private fun BuildLine(build: ModelBuild) {
    Column(modifier = Modifier.fillMaxWidth().background(PanelSurface, RectangleShape).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusMark(true, "building: the model appears in the list when the build ends")
            Spacer(Modifier.width(8.dp))
            Text("building ${build.modelName}", color = FontColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Meta("${(build.fraction * 100).toInt()}%")
        }
        Meta(build.describe())
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = build.fraction,
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = ActiveColor,
            backgroundColor = PanelBorder,
        )
        // Every head scored so far, as it finishes: the comparison is visible while the next one trains
        if (build.candidates.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            CandidatesTable(build.candidates, compact = true)
        }
    }
}

/**
 * The heads a build compared, one line each: architecture, accuracy and macro F1 on the validation rows, train
 * accuracy and final loss, the chosen one marked; under each, F1 per label and what the diagnostics said of it.
 * Compact leaves out the per-label line for the build card.
 */
@Composable
private fun CandidatesTable(candidates: List<org.example.project.components.terminal.window.controller.jetbrainDLBuilder.CandidateResult>, compact: Boolean = false) {
    val width = (candidates.maxOf { it.architecture.length } * 7 + 10).coerceIn(60, 160).dp
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("head", color = MutedFontColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(width))
            listOf("accuracy", "macro F1", "train", "loss").forEach { Text(it, color = MutedFontColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(62.dp)) }
        }
        candidates.forEach { c ->
            val strong = c.chosen
            AppTooltip(
                "${c.architecture}: ${c.parameters} parameters, ${c.epochs} epochs" +
                    (if (c.perLabelF1.isNotEmpty()) "\nF1 per label: " + c.perLabelF1.entries.joinToString(" · ") { "${it.key} ${fmt(it.value)}" } else "") +
                    (if (c.chosen) "\nchosen: the best score on the validation rows; it is the head that was validated once more and exported" else "")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.architecture, color = if (strong) FontColor else LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.width(width))
                    listOf(fmt(c.accuracy), fmt(c.macroF1), fmt(c.trainAccuracy), fmt(c.finalLoss)).forEach {
                        Text(it, color = if (strong) FontColor else LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(62.dp))
                    }
                    if (c.chosen) BadgeLabel("chosen", strong = true)
                }
            }
            if (!compact && c.perLabelF1.isNotEmpty()) {
                Text("F1 per label: " + c.perLabelF1.entries.joinToString(" · ") { "${it.key} ${fmt(it.value)}" }, color = MutedFontColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 12.dp))
            }
            c.insights.forEach { Text(it, color = MutedFontColor, fontSize = 10.sp, fontFamily = FontFamily.SansSerif, lineHeight = 14.sp, modifier = Modifier.padding(start = 12.dp)) }
        }
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
    pending: Set<String>,
    selectedName: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onFetch: (JarInfo) -> Unit,
    /** Runs the tests the jar declares (or one call with "hello") on a fresh session */
    onTest: (JarInfo) -> Unit,
    /** Writes <jar>.package.json beside a jar that ships no metadata, to fill in */
    onTemplate: (JarInfo) -> Unit,
    /** Opens the strip that adds a coloured flag to the jar */
    onFlag: (JarInfo) -> Unit,
    onRemoveFlag: (JarInfo, String) -> Unit,
    /** Uploads a jar from this machine to the website under the signed-in account */
    onPublish: (JarInfo) -> Unit,
    signedIn: Boolean,
    onTerminate: (SessionInfo) -> Unit,
    /** Runs a change to the collections in the background */
    onCollection: (suspend () -> Unit) -> Unit,
    /** Adds a Maven Central coordinate to the env (resolved with its dependencies) */
    onMaven: (String) -> Unit,
    /** Takes a Maven library out of the env */
    onRemoveMaven: (JarInfo) -> Unit,
) {
    var newCollection by remember { mutableStateOf(false) }
    var fromMaven by remember { mutableStateOf(false) }
    // The list is searched by name, Java, collection, class or resource. Collections come first as folders a jar
    // can be added to or detached from; then every jar by where it lives
    var query by remember { mutableStateOf("") }
    val shown = snapshot.jars.filter { it.matches(query) }
    val selected = snapshot.jars.firstOrNull { it.name == selectedName } ?: shown.firstOrNull()
    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        var listWidth by remember { mutableStateOf(ModelsCache.listWidth) }
        Column(modifier = Modifier.width(listWidth.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // The filter and one Add button that offers both: a jar from this machine, or a new collection
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.weight(1f)) { SearchField(query, tip = "Filter the jars by name, version, Java, collection, the class a session opens on, or any class or resource inside") { query = it } }
                DropButton(
                    "Add",
                    icon = Icons.Default.Add,
                    enabled = "addjar" !in pending,
                    tip = if ("addjar" in pending) "copying the jar…" else "Add a jar from this machine (copied into AMP/pluginDir and imported into the env), a library from Maven Central (resolved into the env with its dependencies), or make a collection: a folder of jars that belong together",
                    items = listOf(
                        ContextMenuItem("Jar from this machine…") { onAdd() },
                        ContextMenuItem("From Maven Central…") { fromMaven = true; newCollection = false },
                        ContextMenuItem("New collection…") { newCollection = true; fromMaven = false },
                    ),
                )
            }
            if (snapshot.jars.isEmpty()) Hint("no packages yet: Add a jar or a Maven library, or put a jar in AMP/pluginDir")
            else if (shown.isEmpty()) NoMatches(query) { query = "" }
            snapshot.collections.forEach { collection ->
                val members = shown.filter { it.name in collection.jars }
                if (members.isEmpty() && query.isNotBlank()) return@forEach
                CollectionFolder(collection, members, snapshot.jars, selected?.name, onSelect, onCollection)
            }
            val groups = shown.groupBy { it.location }
            listOf("In AMP/pluginDir", "In the env", "From Maven Central", "On the server", "In the store").forEach { location ->
                val jars = groups[location] ?: return@forEach
                Fold(location, jars.size, open = (location != "On the server" && location != "In the store") || query.isNotBlank() || jars.any { it.name == selected?.name }, key = location) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        jars.forEach { jar -> JarRow(jar, selected = jar.name == selected?.name, onFetch = { onFetch(jar) }, onPublish = { onPublish(jar) }, onRemove = if (jar.maven) ({ onRemoveMaven(jar) }) else null, signedIn = signedIn, onSelect = { onSelect(jar.name) }) }
                    }
                }
            }
            if (!ModelsCache.remoteLoaded) Hint("asking the server…")
        }
        VerticalSeparator { listWidth = it; ModelsCache.listWidth = it }
        when {
            newCollection -> NewCollectionPanel(snapshot.jars, Modifier.weight(1f).fillMaxHeight(), onCreate = { name, jars -> onCollection { PackageCollections.create(name, jars) }; newCollection = false }, onClose = { newCollection = false })
            fromMaven -> MavenPanel(pending, Modifier.weight(1f).fillMaxHeight(), declared = snapshot.mavenDeclared, onAdd = onMaven, onClose = { fromMaven = false })
            selected != null -> JarDetail(selected, pending, Modifier.weight(1f).fillMaxHeight(), onFetch = { onFetch(selected) }, onPublish = { onPublish(selected) }, onRemove = { onRemoveMaven(selected) }, onTest = { onTest(selected) }, onTemplate = { onTemplate(selected) }, onFlag = { onFlag(selected) }, onRemoveFlag = { onRemoveFlag(selected, it) }, signedIn = signedIn, onTerminate = onTerminate)
            else -> Spacer(Modifier.weight(1f))
        }
    }
}

/** The right-click actions of a package: the same as its detail panel's buttons, plus the copy of the call that opens it */
@Composable
private fun jarMenu(jar: JarInfo, onFetch: (() -> Unit)? = null, onDetach: (() -> Unit)? = null, onPublish: (() -> Unit)? = null, onRemove: (() -> Unit)? = null, onTest: (() -> Unit)? = null, onTemplate: (() -> Unit)? = null, onFlag: (() -> Unit)? = null, onRemoveFlag: ((String) -> Unit)? = null, signedIn: Boolean = true): List<ContextMenuItem> {
    val clipboard = LocalClipboardManager.current
    return listOfNotNull(
        if (jar.local && !jar.maven && onFlag != null) ContextMenuItem("Add flag…", onFlag) else null,
        if (jar.local && !jar.maven && onTest != null) ContextMenuItem(if (jar.meta?.tests.isNullOrEmpty()) "Test run (a call with \"hello\")" else "Test run (${jar.meta!!.tests.size} tests)", onTest) else null,
        if (jar.local && !jar.maven && jar.meta == null && onTemplate != null) ContextMenuItem("Write metadata template", onTemplate) else null,
        jar.configDir?.takeIf { jar.local && !jar.maven }?.let { d -> ContextMenuItem("Reveal config folder") { d.mkdirs(); openInBrowser(d.toURI().toString()) } },
        if (jar.maven) ContextMenuItem("Copy implementation(\"${jar.coordinate}\")") { clipboard.setText(AnnotatedString("implementation(\"${jar.coordinate}\")")) }
        else ContextMenuItem("Copy initMod(\"${jar.name}\")") { clipboard.setText(AnnotatedString("initMod(\"${jar.name}\")")) },
        ContextMenuItem(if (jar.maven) "Open its page on Maven Central" else "Open its page on the website") { openInBrowser(jar.page) },
        if (jar.maven && onRemove != null) ContextMenuItem("Remove from the env", onRemove) else null,
        if (!jar.local && onFetch != null) ContextMenuItem("Fetch from the server", onFetch) else null,
        if (jar.local && !jar.maven && onPublish != null) ContextMenuItem(if (signedIn) "Publish to the website" else "Publish to the website (sign in)", onPublish) else null,
        jar.file?.let { f -> ContextMenuItem("Reveal file") { openInBrowser(f.parentFile.toURI().toString()) } },
        onDetach?.let { ContextMenuItem("Detach from collection", it) },
    ) + (if (onRemoveFlag != null) (PackageMetadata.sidecar(jar.name)?.flags ?: emptyList()).map { f -> ContextMenuItem("Remove flag ${f.text}") { onRemoveFlag(f.text) } } else emptyList())
}

@Composable
private fun JarRow(jar: JarInfo, selected: Boolean, onDetach: (() -> Unit)? = null, onFetch: (() -> Unit)? = null, onPublish: (() -> Unit)? = null, onRemove: (() -> Unit)? = null, signedIn: Boolean = true, onSelect: () -> Unit) {
    ListRow(
        name = jar.name,
        active = jar.open > 0,
        selected = selected,
        meta = listOfNotNull(
            jar.contents?.minJava?.let { "java $it+" },
            jar.version?.let { "v$it" },
            jar.author?.let { "by $it" },
            jar.resolvedCoordinate?.takeIf { it != jar.coordinate }?.let { "declared ${jar.coordinate?.substringAfterLast(':')}" },
        ).joinToString(" · "),
        status = jar.status,
        statusTip = when {
            jar.sessions.any { it.running } -> "inferring: a session on it is answering right now"
            jar.open > 0 -> "${jar.open} open: sessions a script opened with initMod(\"${jar.name}\"); each is its own instance"
            jar.local -> "idle: on this machine, no session open"
            jar.source == "store" -> "in the website's store, published by ${jar.author ?: "someone"}: not on this machine yet; Fetch downloads it, and the first initMod does too"
            else -> "on the server: not on this machine yet; Fetch downloads it, and the first initMod does too"
        },
        underline = jar.open > 0,
        icon = Icons.Default.Inventory2,
        onDetach = onDetach,
        menu = jarMenu(jar, onFetch, onDetach, onPublish, onRemove, signedIn = signedIn),
        flags = jar.flags,
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
                SmallButton(if (adding) "Close" else "Add jar", icon = if (adding) Icons.Default.Close else Icons.Default.Add, tip = if (adding) "Close the list" else "Put another jar into this collection") { adding = !adding }
                SmallButton("Delete folder", icon = Icons.Default.Delete, tip = "Remove the collection; its jars stay where they are") { onCollection { PackageCollections.delete(collection.name) } }
            }
            if (adding) {
                val candidates = all.filter { it.name !in collection.jars }
                if (candidates.isEmpty()) Hint("every jar is in it")
                candidates.forEach { jar ->
                    SmallButton("+ ${jar.name}", tip = "Add ${jar.name} to ${collection.name}") { onCollection { PackageCollections.add(collection.name, jar.name) } }
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

/**
 * A new collection, in the place of the detail panel: a name, the jars to put in it, and a shortcut per Java
 * version that picks every jar that runs on it
 */
@Composable
private fun NewCollectionPanel(jars: List<JarInfo>, modifier: Modifier, onCreate: (String, List<String>) -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(setOf<String>()) }
    val versions = jars.mapNotNull { it.contents?.minJava }.distinct().sorted()
    DetailPanel(modifier) {
        DetailHeader(name = "New collection", active = false, status = "a folder of jars that belong together", mark = false) {
            SmallButton("Close", icon = Icons.Default.Close, tip = "Close the form; nothing is kept", onClick = onClose)
        }
        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(top = 12.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingField("Name", name) { name = it }
            if (versions.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Text("By Java", color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.width(56.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    versions.forEach { v ->
                        // Every jar that runs on Java v: the ones that need v or older
                        SmallButton("jvm$v", tip = "Name it jvm$v and pick every jar that runs on Java $v") {
                            name = "jvm$v"
                            chosen = jars.filter { (it.contents?.minJava ?: Int.MAX_VALUE) <= v }.map { it.name }.toSet()
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                Text("Jars", color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.width(56.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (jars.isEmpty()) Hint("no jars yet")
                    jars.forEach { jar -> Toggle(jar.name, jar.name in chosen, tip = if (jar.name in chosen) "in the collection; click to leave it out" else "click to put it in") { on -> chosen = if (on) chosen + jar.name else chosen - jar.name } }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Meta("${chosen.size} jars")
                Spacer(Modifier.weight(1f))
                AccentButton("Create", enabled = name.isNotBlank() && chosen.isNotEmpty(), tip = when { name.isBlank() -> "give the collection a name"; chosen.isEmpty() -> "pick at least one jar"; else -> "Make the collection; it is kept in AMP/pluginDir/collections.json" }) { onCreate(name.trim(), chosen.toList()) }
            }
        }
    }
}

/**
 * The "From Maven Central…" form, in the place of the detail panel. One box: type a name (gson), a class name,
 * `group:artifact` for the versions of one artifact, or paste a full `group:artifact:version`; Maven Central is
 * asked as you type. Every hit has its own Add, which resolves that version with its dependencies into the env
 * (the same as implementation("g:a:v") in a script). The form closes itself once the library is in the env.
 */
@Composable
private fun MavenPanel(pending: Set<String>, modifier: Modifier, declared: Set<String>, onAdd: (String) -> Unit, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<MavenHit>>(emptyList()) }
    var searched by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var requested by remember { mutableStateOf<String?>(null) }
    val typed = query.trim()
    val exact = MavenCentral.parse(typed)?.let { typed }
    val adding = pending.firstOrNull { it.startsWith("maven:") }?.removePrefix("maven:")

    // Asked as you type, a moment after the last key; a pasted full coordinate is also searched so its versions show
    LaunchedEffect(typed) {
        val q = typed.substringBeforeLast(':').takeIf { exact != null } ?: typed
        if (q.length < 2) { hits = emptyList(); searched = ""; searchError = null; searching = false; return@LaunchedEffect }
        delay(350)
        searching = true
        searchError = null
        try {
            val result = withContext(Dispatchers.IO) { runCatching { MavenCentral.search(q) } }
            result.onSuccess { hits = it; searched = q }.onFailure { e ->
                val cause = (e as? java.util.concurrent.CompletionException)?.cause ?: e
                searchError = "Maven Central could not be searched: ${cause.message ?: cause.javaClass.simpleName}"
            }
        } finally {
            searching = false
        }
    }
    // Done: the library is in the list now, so the form gives way to its detail panel
    LaunchedEffect(declared, requested) { if (requested != null && requested in declared) onClose() }

    DetailPanel(modifier) {
        DetailHeader(name = "From Maven Central", active = false, status = "a library resolved into the env with its dependencies", mark = false) {
            SmallButton("Close", icon = Icons.Default.Close, tip = "Close the form; nothing is kept", onClick = onClose)
        }
        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(top = 12.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)) {
                SearchField(query, tip = "A name (gson, okhttp), a class name, group:artifact for one artifact's versions, or a full group:artifact:version") { query = it }
            }
            Text(
                when {
                    adding != null -> "resolving $adding and its dependencies from Maven Central…"
                    typed.isEmpty() -> "type a name to search Maven Central, or paste a coordinate (group:artifact:version)"
                    searchError != null -> searchError!!
                    searching -> "asking Maven Central…"
                    exact != null -> "Add puts this exact version in the env; the list below has the other versions"
                    hits.isEmpty() && searched.isNotEmpty() -> "nothing on Maven Central matches \"$searched\""
                    else -> "${hits.size} hits · Add puts that version in the env; a script can import its classes from the next run"
                },
                color = if (searchError != null) DangerColor else MutedFontColor, fontSize = 11.sp, lineHeight = 16.sp,
            )
            if (adding != null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).height(2.dp), color = ActiveColor, backgroundColor = ActiveColor.copy(alpha = 0.15f), strokeCap = StrokeCap.Square)
            Spacer(Modifier.height(2.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)) {
                // The pasted coordinate itself, first
                if (exact != null) MavenRow(
                    name = exact.substringBeforeLast(':'), version = exact.substringAfterLast(':'), meta = "as typed",
                    page = MavenCentral.page(exact), inEnv = exact in declared, adding = adding == exact, enabled = adding == null,
                ) { requested = exact; onAdd(exact) }
                hits.filter { it.coordinate != exact }.forEach { hit ->
                    MavenRow(
                        name = "${hit.group}:${hit.artifact}", version = hit.latestVersion,
                        meta = listOfNotNull(hit.updated?.toString()?.take(10)?.let { "published $it" }, hit.packaging.takeIf { it != "jar" }).joinToString(" · "),
                        page = hit.page, inEnv = hit.coordinate in declared, adding = adding == hit.coordinate, enabled = adding == null,
                    ) { requested = hit.coordinate; onAdd(hit.coordinate) }
                }
            }
        }
    }
}

/** One artifact Maven Central found: its coordinates, the version Add would put in the env, and Add itself */
@Composable
private fun MavenRow(name: String, version: String, meta: String, page: String?, inEnv: Boolean, adding: Boolean, enabled: Boolean, onAdd: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val clipboard = LocalClipboardManager.current
    val coordinate = "$name:$version"
    ContextMenuArea(items = { listOfNotNull(
        ContextMenuItem("Copy implementation(\"$coordinate\")") { clipboard.setText(AnnotatedString("implementation(\"$coordinate\")")) },
        page?.let { ContextMenuItem("Open its page on Maven Central") { openInBrowser(it) } },
    ) }) {
        Row(
            modifier = Modifier.fillMaxWidth().background(if (hovered) PanelSurface else Color.Transparent).hoverable(interactionSource).padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf("v$version", meta).filter { it.isNotBlank() }.joinToString(" · "), color = MutedFontColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            when {
                inEnv -> Meta("in the env")
                adding -> SmallButton("Adding…", enabled = false, tip = "being resolved; the Console tab follows it") {}
                else -> AccentButton("Add", enabled = enabled, icon = Icons.Default.Add, tip = "Resolve $coordinate and its dependencies into the env", onClick = onAdd)
            }
        }
    }
}

/**
 * The selected package: what it is (status, Java it needs, language), the collections it is in, where it came
 * from, what built it, what it contains, and its sessions. A script opens it with initMod("name.jar").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JarDetail(jar: JarInfo, pending: Set<String>, modifier: Modifier = Modifier, onFetch: () -> Unit, onPublish: () -> Unit, onRemove: () -> Unit = {}, onTest: () -> Unit = {}, onTemplate: () -> Unit = {}, onFlag: () -> Unit = {}, onRemoveFlag: (String) -> Unit = {}, signedIn: Boolean, onTerminate: (SessionInfo) -> Unit) {
    val contents = jar.contents
    val fetching = "fetch:${jar.name}" in pending
    val publishing = "publish:${jar.name}" in pending
    val removing = "maven-remove:${jar.coordinate}" in pending
    val testing = "test:${jar.name}" in pending
    val package_ = jar.local && !jar.maven
    DetailPanel(modifier) {
        DetailHeader(
            name = jar.name,
            active = jar.open > 0,
            status = listOfNotNull(
                jar.status.takeIf { it !in quietStatus },
                contents?.let { it.minJava?.let { v -> "java $v+" } ?: "any java" },
                contents?.let { if (it.kotlin) "kotlin" else "java" },
                jar.version?.let { "v$it" },
                jar.collections.takeIf { it.isNotEmpty() }?.let { "in ${it.joinToString()}" },
                if (package_ && jar.unmet > 0) "${jar.unmet} not met" else null,
                jar.report?.let { r -> if (r.ok) "tests pass" else "tests fail" },
            ).joinToString(" · "),
            statusTip = if (jar.maven) "a library from Maven Central in the env: a script imports its classes, no initMod needed" else "a script opens it with initMod(\"${jar.name}\"); every call is a fresh instance and its own session",
            menu = jarMenu(jar, onFetch = onFetch, onPublish = onPublish, onRemove = onRemove, onTest = onTest, onTemplate = onTemplate, onFlag = onFlag, onRemoveFlag = onRemoveFlag, signedIn = signedIn),
        ) {
            if (package_) SmallButton(
                if (testing) "Testing…" else "Test run",
                enabled = !testing,
                icon = Icons.Default.FactCheck,
                tip = if (jar.meta?.tests.isNullOrEmpty()) "Open a fresh session as initMod would (configuration included), call run(\"hello\") and close it; no tests are declared, so the call must only answer" else "Open a fresh session as initMod would (configuration included), run the ${jar.meta!!.tests.size} test${if (jar.meta.tests.size == 1) "" else "s"} the jar declares and close it; the TESTS block shows every answer",
                onClick = onTest,
            )
            if (jar.maven) SmallButton(if (removing) "Removing…" else "Remove", enabled = !removing, icon = Icons.Default.Delete, tip = "Take the library out of the env; its files stay in AMP/env/caches", onClick = onRemove)
            if (!jar.local && !jar.maven) AccentButton(if (fetching) "Fetching…" else "Fetch", enabled = !fetching, icon = Icons.Default.Download, tip = "Download the jar from the server into AMP/pluginDir", onClick = onFetch)
            if (jar.local && !jar.maven) SmallButton(
                when { publishing -> "Publishing…"; signedIn -> "Publish"; else -> "Publish (sign in)" },
                enabled = !publishing,
                icon = Icons.Default.CloudUpload,
                tip = if (signedIn) "Upload the jar to the website under your account; the server takes only a jar with run(String) and close()" else "Publish uploads the jar under your account, so it opens the sign-in page first",
                onClick = onPublish,
            )
        }

        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(top = 10.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // What it works with, as flags: version, Java, language, tests, and the custom ones; Add flag puts one more on
            if (package_ || jar.flags.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 2.dp)) {
                jar.flags.forEach { FlagBadge(it) }
                if (package_) SmallButton("Add flag", icon = Icons.Default.Flag, tip = "Tag the jar with a word in a colour of your choice (stable, needs key, v2 api…); kept in ${PackageMetadata.sidecarFile(jar.name).name}", onClick = onFlag)
            }
            if (jar.maven) KeyValues("used with" to "implementation(\"${jar.coordinate}\") is in the env: the script's import lines can name its classes from the next run")
            else KeyValues("opens with" to "initMod(\"${jar.name}\"), a fresh instance per call")

            Fold("SOURCE", null, open = false, key = "${jar.name}:source") { KeyValues(
                "where" to when (jar.source) {
                    "maven" -> "AMP/env/caches, resolved from Maven Central with its dependencies"
                    "server" -> "on the server; fetched into AMP/pluginDir on the first open"
                    "store" -> "in the website's store, published by ${jar.author ?: "someone"} under their account; fetched into AMP/pluginDir on the first open"
                    else -> if (jar.origin != null) "AMP/${if (jar.source == "env") "env/<env>/libs" else "pluginDir"}, downloaded from the server" else "AMP/${if (jar.source == "env") "env/<env>/libs" else "pluginDir"}, added on this machine"
                },
                "page" to jar.page,
                "download" to (jar.url ?: "none, this jar was added on this machine"),
                *listOfNotNull(
                    jar.coordinate?.let { "declared" to it },
                    jar.resolvedCoordinate?.takeIf { it != jar.coordinate }?.let { "in the env as" to "$it, because a dependency asks for that newer version" },
                ).toTypedArray(),
                *listOfNotNull(
                    jar.origin?.let { o -> "fetched" to "${o.fetchedAt.take(19).replace('T', ' ')}${o.lastModified?.let { " · published $it" } ?: ""}" },
                    jar.file?.let { "file" to it.absolutePath },
                ).toTypedArray(),
            ) }

            if (package_) {
                val meta = jar.meta
                Fold("REQUIREMENTS", jar.unmet.takeIf { it > 0 }, open = jar.unmet > 0 || meta != null, key = "${jar.name}:requirements") {
                    Hint(if (jar.unmet == 0) "all met" else "${jar.unmet} not met: initMod opens the jar, but it may not find what it needs")
                    RequirementRows(jar.requirements)
                }
                Fold("CONFIG", meta?.config?.size, open = !meta?.config.isNullOrEmpty(), key = "${jar.name}:config") {
                    val dir = jar.configDir!!
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SelectionContainer(modifier = Modifier.weight(1f)) {
                            Text(dir.absolutePath, color = LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(8.dp))
                        SmallButton("Reveal folder", icon = Icons.Default.FolderOpen, tip = "Show the config folder in the file browser; what the package reads is put here") { dir.mkdirs(); openInBrowser(dir.toURI().toString()) }
                    }
                    Hint("the package finds this folder as the first root of its class loader (getResource), as the amie.package.config system property, and through a configure(File | Path | String | Map) method when it has one")
                    if (meta == null) Hint("no metadata, so no config files are declared; ⋯ > Write metadata template makes ${PackageMetadata.sidecarFile(jar.name).name} to say what the jar needs")
                    else if (meta.config.isEmpty()) Hint("the metadata declares no config files")
                    else KeyValues(*meta.config.map { c ->
                        val f = File(dir, c.file)
                        c.file to listOfNotNull(
                            if (f.isFile) "present" else if (c.template != null) "missing; the template is written on the next open" else "missing",
                            if (c.required) "required" else "optional",
                            c.description,
                        ).joinToString(" · ")
                    }.toTypedArray())
                    if (meta != null && meta.env.isNotEmpty()) KeyValues(*meta.env.map { v -> "\$${v.name}" to listOfNotNull(if (System.getenv(v.name).isNullOrEmpty()) "not set" else "set", if (v.required) "required" else "optional", v.description).joinToString(" · ") }.toTypedArray())
                    if (meta?.notes != null) Hint(meta.notes)
                }
                Fold("TESTS", meta?.tests?.size, open = jar.report != null, key = "${jar.name}:tests") {
                    if (meta == null || meta.tests.isEmpty()) Hint("no tests declared in the metadata; Test run makes one call with \"hello\" that must answer")
                    else KeyValues(*meta.tests.map { t -> t.title to listOfNotNull("run(\"${t.input.take(60)}\")", t.expect?.let { "expect \"$it\"" }, t.contains?.let { "contains \"$it\"" }, t.lines?.let { "$it line${if (it == 1) "" else "s"}" }).joinToString(" · ") }.toTypedArray())
                    val report = jar.report
                    if (report == null) Hint("not run yet")
                    else {
                        Hint("last run ${report.ranAt.take(19).replace('T', ' ')}: ${report.summary().substringAfter(": ")}" + (report.entryClass?.let { " · on $it" } ?: ""))
                        if (report.error != null) Text(report.error, color = LogErrorTextColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                        // Every test with what it was asked and the whole answer, line by line, so what the jar actually said is in view
                        report.results.forEachIndexed { i, r ->
                            if (i > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PanelBorder))
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppTooltip(if (r.passed) "passed" else "failed: ${r.reason}") {
                                        Icon(if (r.passed) Icons.Default.CheckCircle else Icons.Default.Cancel, contentDescription = if (r.passed) "passed" else "failed", tint = if (r.passed) SuccessColor else DangerColor, modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(r.name, color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.width(10.dp))
                                    Meta("${r.millis} ms")
                                }
                                KeyValues("asked" to "run(\"${r.input}\")")
                                Text("answered" + if (r.answer.isEmpty()) ": nothing" else " ${r.answer.size} line${if (r.answer.size == 1) "" else "s"}", color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                if (r.answer.isNotEmpty()) SelectionContainer {
                                    Column(modifier = Modifier.fillMaxWidth().background(CodeBackground, RectangleShape).border(1.dp, ConsoleBorderColor, RectangleShape).padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        r.answer.forEach { line -> Text(line, color = LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp) }
                                    }
                                }
                                r.reason?.let { Text(it, color = LogErrorTextColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp) }
                            }
                        }
                    }
                }
            }

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
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val shown = sessions.filter { q.isEmpty() || it.owner.lowercase().contains(q) || it.kind.lowercase().contains(q) || "#${it.id}".contains(q) }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (sessions.isEmpty()) Hint("no open sessions · Open session on a model, or initMod(\"name\") in a script, opens one")
        else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (sessions.size > 4) Box(modifier = Modifier.weight(1f).widthIn(max = 420.dp)) { SearchField(query, tip = "Filter by owner, kind or #id") { query = it } }
                else Spacer(Modifier.weight(1f))
                DangerButton("Terminate all", icon = Icons.Default.StopCircle, tip = "Close every open session: models, packages and bare ONNX files", onClick = onTerminateAll)
            }
            if (shown.isEmpty()) NoMatches(query) { query = "" }
            SessionList(shown, showOwner = true, onTerminate)
        }
    }
}

/** The sessions one owner has open, inside its detail panel; without [title] where a tab already names them */
@Composable
private fun SessionsOf(sessions: List<SessionInfo>, onTerminate: (SessionInfo) -> Unit, title: Boolean = true) {
    val terminateAll: (@Composable () -> Unit)? = if (sessions.size > 1) ({ DangerButton("Terminate all", icon = Icons.Default.StopCircle, tip = "Close every session of it") { sessions.forEach(onTerminate) } }) else null
    if (title) Section("SESSIONS  ${sessions.size}", key = "sessions:${sessions.firstOrNull()?.owner ?: ""}", actions = terminateAll) {
        if (sessions.isEmpty()) Hint("none open") else SessionList(sessions, showOwner = false, onTerminate)
    } else {
        terminateAll?.let { Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) { Spacer(Modifier.weight(1f)); it() } }
        if (sessions.isEmpty()) Hint("none open") else SessionList(sessions, showOwner = false, onTerminate)
    }
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
            StatusMark(session.running, if (session.running) "inferring: answering right now" else "idle: open and waiting for the next inference")
            Spacer(Modifier.width(8.dp))
            Text(if (showOwner) "${session.owner}  #${session.id}" else "#${session.id}", color = FontColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(10.dp))
            Meta(listOfNotNull(if (showOwner) session.kind else null, if (session.running) "inferring" else null, "${session.runs} runs", uptime(Duration.between(session.openedAt, Instant.now()))).joinToString(" · "))
            Spacer(Modifier.weight(1f))
            if (session.hasLog) {
                SmallButton(if (showLog) "Hide log" else "Log ${session.log.size}", icon = Icons.Default.Notes, tip = "What this session was asked and answered, with the time of each") { showLog = !showLog }
                Spacer(Modifier.width(6.dp))
            }
            DangerButton("Terminate", icon = Icons.Default.StopCircle, tip = "Close this session; a script holding it gets an error on its next call", onClick = onTerminate)
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
        if (shown.size < entries.size) Meta("… ${entries.size - shown.size} earlier lines in sessions.log")
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
    Column(modifier = Modifier.widthIn(max = 860.dp).verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Fold("What an intent model is", null, open = true, key = "guide:model") {
            Para("An intent model turns a sentence into one of a few labels, like greeting, lights or weather. It has two parts: the encoder, a BERT model (bge) that turns the text into 768 numbers, and the head, a small dense network trained on your rows that turns those numbers into a score per label. Only the head is yours: it lives in AMP/modules/<name>/ as model.onnx with module.json (name, labels, encoder, how it was trained), metrics.json (every validation), serving.json and sessions.log. The encoder is shared by every model and loaded once.")
            Para("The dummy model is there from the first start: a head with made-up weights over the same encoder, so the page has something to show and a script can call initMod(\"dummy\") before anything is trained. Its scores mean nothing.")
        }
        Fold("Building one", null, open = false, key = "guide:build") {
            Para("Press + New model. Give it a name, a sentence about what it is for, and a csv with a text column and a label column (the columns are inferred from the header; Sample data writes AMP/sample-intents.csv with 45 rows over three labels). A test csv is optional; without one, 20% of the rows are held out. Build embeds every row through the encoder, fits the head for 150 epochs, validates it and exports it. The card above the list follows the build; the console lines of the model say the rest.")
            Para("The list groups models by the data they were trained on, running ones first, then by accuracy. Search matches names, labels, status and data files. Right-click a row, or press its ⋯ button, for its actions; the panel header has the same button. Rest the pointer on any button, tab or status word and it says what it does.")
        }
        Fold("Reading the metrics", null, open = false, key = "guide:metrics") {
            Para("Every validation is a run; Validate on sample data or Validate on file adds one, and a build adds the first. Pick a run on the strip. Scores per label: for every label, precision (of what the model called that label, how much really was), recall (of the rows of that label, how much it found) and F1 (their harmonic mean) on one 0 to 1 axis, with the row count; the macro row averages the labels. Confusion matrix: every actual label in a row, what the model answered in the columns; the diagonal is right, a bright cell off it is the confusion to fix, and the caption names the biggest.")
            Para("Scores by run shows accuracy, macro F1 and every label's F1 across the runs, the latest at the right: a row that lightens is improving. From the sessions: how sure the model is (where every label's scores fall between 0 and 1 across every answer it gave) and how long an answer takes.")
        }
        Fold("Serving it", null, open = false, key = "guide:serve") {
            Para("Listen starts an HTTP server for the model on the host, port and path in Configure (loopback by default), with POST {\"text\": \"…\"} or GET ?text=… returning the scores as JSON. Copy and Copy curl in Configure give the address and a working call. What it answers is appended to incoming.jsonl, so it can be reviewed and labeled later. Listening resumes when the app starts again, until Stop.")
            Para("Publish sends the module to the Amie server under your account (sign in first); the website lists it. A jar on the Packages tab has Publish too; the server only takes a jar with run(String): List and close().")
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
 * One line of a list: name, a short meta line, the status word at the right (its tooltip says what it means) and
 * a ⋯ button with the row's actions, the same ones right-click opens; click to select. A running one is
 * underlined in green; a stopped one is not. With onDetach, a Detach button appears while the pointer is over it.
 */
@Composable
private fun ListRow(name: String, active: Boolean, selected: Boolean, meta: String, status: String, statusTip: String? = null, underline: Boolean = false, icon: ImageVector? = null, painter: Painter? = null, onDetach: (() -> Unit)? = null, menu: List<ContextMenuItem> = emptyList(), flags: List<Flag> = emptyList(), drag: DragState? = null, dragKey: String? = null, dropKey: String? = null, onSelect: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    // Where the row is on the window, for the drag that starts on it and the drop that lands on it
    var rowOrigin by remember { mutableStateOf(Offset.Zero) }
    val over = drag != null && dropKey != null && drag.dragging != null && drag.over == dropKey
    if (drag != null && dropKey != null) DisposableEffect(dropKey) { onDispose { drag.targets.remove(dropKey) } }
    ContextMenuArea(items = { menu }) { Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { c ->
                rowOrigin = c.positionInRoot()
                if (drag != null && dropKey != null) drag.targets[dropKey] = c.boundsInRoot()
            }
            .background(when { over -> AccentSoft; selected -> PanelSurfaceSelected; hovered -> PanelSurface; else -> Color.Transparent })
            .then(if (over) Modifier.border(1.dp, PageAccent, RectangleShape) else Modifier)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onSelect() }
            .then(
                if (drag != null && dragKey != null) Modifier.pointerInput(dragKey) {
                    detectDragGestures(
                        onDragStart = { start -> drag.start(dragKey, rowOrigin + start) },
                        onDrag = { change, delta -> change.consume(); drag.position += delta },
                        onDragEnd = { drag.drop() },
                        onDragCancel = { drag.cancel() },
                    )
                } else Modifier
            )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (painter != null) {
                IconImage(painter, 18.dp)
                Spacer(Modifier.width(7.dp))
            } else if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (active) FontColor else MutedFontColor, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(7.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                // The name has the whole line: the flags and the meta go under it, so a jar with several flags still shows its name
                Text(name, color = FontColor, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontFamily = FontFamily.SansSerif, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (flags.isNotEmpty()) FlagsLine(flags)
                if (meta.isNotEmpty()) Meta(meta)
            }
            Spacer(Modifier.width(6.dp))
            if (hovered && onDetach != null) {
                SmallButton("Detach", icon = Icons.Default.LinkOff, tip = "Take it out of this collection; the file stays", onClick = onDetach)
            } else if (status !in quietStatus) {
                AppTooltip(statusTip) {
                    Text(status, color = if (active) FontColor else MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                }
            }
            if (menu.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                MoreButton(menu, shown = hovered || selected)
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

/** Name and one status line on the left of a detail panel's header, the actions on the right, then the ⋯ menu */
@Composable
private fun DetailHeader(name: String, active: Boolean, status: String, statusTip: String? = null, menu: List<ContextMenuItem> = emptyList(), mark: Boolean = true, icon: ImageVector? = null, painter: Painter? = null, actions: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (mark) {
            StatusMark(active, statusTip)
            Spacer(Modifier.width(2.dp))
        }
        if (painter != null) IconImage(painter, 22.dp)
        else if (icon != null) Icon(icon, contentDescription = null, tint = if (active) FontColor else MutedFontColor, modifier = Modifier.size(15.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = FontColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif, maxLines = 1, overflow = TextOverflow.Ellipsis)
            AppTooltip(statusTip) { Meta(status) }
        }
        actions()
        if (menu.isNotEmpty()) MoreButton(menu, tip = "Every action of $name, the same list the right-click on its row opens")
    }
    Box(modifier = Modifier.fillMaxWidth().height(if (active) 2.dp else 1.dp).background(if (active) SuccessColor else PanelBorder))
}

/**
 * An uploaded icon drawn as a square of [side]: the image fills it edge to edge (cropped to the centre when it is
 * not square), so no padding or letterbox shrinks it in a row or on a header
 */
@Composable
private fun IconImage(painter: Painter, side: Dp) {
    Image(painter, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(side).clip(RectangleShape))
}

/** The flags of a row under its name, wrapping onto more lines when they do not fit on one */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlagsLine(flags: List<Flag>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp, bottom = 1.dp)) {
        flags.forEach { FlagBadge(it) }
    }
}

/** A coloured tag: a square of its colour with the text on it, the tooltip saying what it stands for */
@Composable
private fun FlagBadge(flag: Flag) {
    AppTooltip(flag.tip) {
        Text(
            text = flag.text,
            color = FontColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            modifier = Modifier
                .background(flag.color.copy(alpha = 0.22f), RectangleShape)
                .border(1.dp, flag.color.copy(alpha = 0.7f), RectangleShape)
                .padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

/**
 * A model row being dragged over the list: dropped on a group it joins it, on another model the two make a new
 * group, on the drop zone over the list a new group with it. The rows register where they are; the ghost label
 * follows the pointer; the row under it is lit.
 */
private class DragState(private val onDrop: (model: String, target: String) -> Unit) {
    var dragging by mutableStateOf<String?>(null)
    var position by mutableStateOf(Offset.Zero)
    val targets = mutableStateMapOf<String, Rect>()

    /** The drop key under the pointer, never the row being dragged */
    val over: String? get() = dragging?.let { d -> targets.entries.firstOrNull { (key, bounds) -> key != "model:$d" && bounds.contains(position) }?.key }

    fun start(key: String, at: Offset) { dragging = key; position = at }
    fun drop() { val model = dragging; val target = over; dragging = null; if (model != null && target != null) onDrop(model, target) }
    fun cancel() { dragging = null }
}

/** The zone over the list that takes a dragged model for a new group; only there while something is dragged */
@Composable
private fun NewGroupDropZone(drag: DragState) {
    val over = drag.over == "new-group"
    DisposableEffect(Unit) { onDispose { drag.targets.remove("new-group") } }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { drag.targets["new-group"] = it.boundsInRoot() }
            .background(if (over) AccentSoft else PanelSurface, RectangleShape)
            .border(1.dp, if (over) PageAccent else PanelBorder, RectangleShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text("drop here for a new group · on another model to pair them · the Groups tab adds to a group", color = if (over) FontColor else MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
    }
}

/** A small label on a faint square; strong is white text, the rest gray */
@Composable
private fun BadgeLabel(text: String, strong: Boolean = false) {
    Text(
        text = text,
        color = if (strong) FontColor else MutedFontColor,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        modifier = Modifier
            .background(ChipBackground, RectangleShape)
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
                    Text(key.padEnd(width), color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 19.sp)
                    Spacer(Modifier.width(14.dp))
                    Text(value, color = LogMessageColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 19.sp)
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
            modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand).clickable { expanded = !expanded }.padding(vertical = 4.dp),
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
        if (expanded) Column(modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 2.dp, bottom = 8.dp)) { content() }
    }
}

/** A one-line filter over a list: an icon, the text, and a clear button once there is text; the tip says what it matches */
@Composable
private fun SearchField(value: String, tip: String? = null, onChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    AppTooltip(tip) { Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CodeBackground, RectangleShape)
            .border(1.dp, when { focused -> PageAccent; value.isNotEmpty() -> PanelBorderSelected; else -> PanelBorder }, RectangleShape)
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
        if (value.isNotEmpty()) SmallButton("×", tip = "Clear the filter") { onChange("") }
    } }
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
private fun FileField(label: String, file: File?, hint: String, onClear: (() -> Unit)? = null, sample: (() -> Unit)? = null, chooseTip: String? = null, sampleTip: String? = null, onChoose: () -> Unit) {
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
        SmallButton("Choose", icon = Icons.Default.FolderOpen, tip = chooseTip, onClick = onChoose)
        if (sample != null) {
            Spacer(Modifier.width(6.dp))
            SmallButton("Sample data", icon = Icons.Default.TableChart, tip = sampleTip, onClick = sample)
        }
        if (file != null && onClear != null) {
            Spacer(Modifier.width(6.dp))
            SmallButton("Clear", tip = "Use no $label file", onClick = onClear)
        }
    }
}

/**
 * One block of a detail tab: the uppercase title on a line that folds the block away (open by default, so nothing
 * is hidden until it is asked to be), a rule under it, and the content with a little air below the line.
 * [actions] sit at the right of the title line and stay visible while the block is closed.
 */
@Composable
private fun Section(title: String, key: Any, actions: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    var expanded by remember(key) { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand).clickable { expanded = !expanded }.padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = if (expanded) "Fold $title away" else "Open $title",
                tint = MutedFontColor,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(title, color = FontColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            actions?.invoke()
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PanelBorder))
        if (expanded) Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = MutedFontColor, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 1.dp))
}

@Composable
private fun Meta(text: String) {
    Text(text, color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** A 7dp square: the running green when active, gray when not; the tip says what that means where it stands */
@Composable
private fun StatusMark(active: Boolean, tip: String? = null) {
    AppTooltip(tip) { Box(modifier = Modifier.size(7.dp).background(if (active) SuccessColor else IdleColor, RectangleShape)) }
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

/* ---------- notices ---------- */

/** A message for the user at the top of the page: what an action did, or (in red) why it failed */
private data class Notice(val text: String, val error: Boolean = false)

/** The notice as a strip under the tabs: a bar at its left edge (red for a failure), the whole text (it wraps, it can be selected) and Dismiss */
@Composable
private fun NoticeBar(notice: Notice, onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .height(IntrinsicSize.Min)
            .background(PanelSurface, RectangleShape)
    ) {
        Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(if (notice.error) LogErrorColor else PanelBorderSelected))
        Spacer(Modifier.width(10.dp))
        SelectionContainer(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(notice.text, color = if (notice.error) LogErrorTextColor else LogMessageColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif, lineHeight = 17.sp)
        }
        Spacer(Modifier.width(8.dp))
        SmallButton("Dismiss", tip = "Hide this message", onClick = onDismiss)
        Spacer(Modifier.width(4.dp))
    }
}

/** A question before something that cannot be undone: a red bar, the whole text, a red button that does it, and Keep */
@Composable
private fun ConfirmBar(text: String, confirm: String, onConfirm: () -> Unit, onKeep: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .height(IntrinsicSize.Min)
            .background(PanelSurface, RectangleShape)
    ) {
        Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(LogErrorColor))
        Spacer(Modifier.width(10.dp))
        Text(text, color = LogMessageColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif, lineHeight = 17.sp, modifier = Modifier.weight(1f).padding(vertical = 6.dp))
        Spacer(Modifier.width(8.dp))
        DangerButton(confirm, icon = Icons.Default.Delete, tip = "Do it; this cannot be undone", onClick = onConfirm)
        Spacer(Modifier.width(4.dp))
        SmallButton("Keep", tip = "Change nothing", onClick = onKeep)
        Spacer(Modifier.width(4.dp))
    }
}

/* ---------- GROUPS ---------- */

/**
 * The groups tab: every group of every env in a list (its icon, how many models, its env, the green underline
 * while its swarm is open) and the selected one in the panel. A group is made from a model's ⋯ menu on the Models
 * tab, by dragging models together there, or with + New group here (empty, to fill with Add model).
 */
@Composable
private fun ColumnScope.GroupsTab(
    snapshot: ModelsSnapshot,
    pending: Set<String>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    onShowModel: (String) -> Unit,
    onAddToGroup: (ModelInfo, GroupInfo) -> Unit,
    onGroupOpen: (GroupInfo) -> Unit,
    onGroupTerminate: (GroupInfo) -> Unit,
    onGroupRemove: (GroupInfo, String) -> Unit,
    onGroupIcon: (GroupInfo, String?) -> Unit,
    onGroupDelete: (GroupInfo) -> Unit,
    onTerminate: (SessionInfo) -> Unit,
    onNewGroup: (List<String>) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    fun menuOfGroup(g: GroupInfo): List<ContextMenuItem> = listOfNotNull(
        if (g.open) ContextMenuItem("Terminate swarm") { onGroupTerminate(g) } else ContextMenuItem("Open swarm") { onGroupOpen(g) },
        ContextMenuItem("Copy group(\"${g.name}\")") { clipboard.setText(AnnotatedString("group(\"${g.name}\")")) },
    ) + g.group.models.map { m -> ContextMenuItem("Take $m out") { onGroupRemove(g, m) } } + listOf(
        ContextMenuItem("Remove group") { onGroupDelete(g) },
    )
    var query by remember { mutableStateOf("") }
    val shown = snapshot.groups.filter { it.matches(query.trim().lowercase()) }.sortedWith(compareByDescending<GroupInfo> { it.open }.thenBy { it.env }.thenBy { it.name })
    val selected = selectedKey?.let { key -> snapshot.groups.firstOrNull { "${it.name}@${it.env}" == key } } ?: shown.firstOrNull()
    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        var listWidth by remember { mutableStateOf(ModelsCache.listWidth) }
        Column(modifier = Modifier.width(listWidth.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.weight(1f)) { SearchField(query, tip = "Filter the groups by name, env, model or open/closed") { query = it } }
                AccentButton("New group", icon = Icons.Default.Add, tip = "Make an empty group in an env, then Add model to it; a model's ⋯ menu on the Models tab and dragging models together there make one too") { onNewGroup(emptyList()) }
            }
            if (shown.isEmpty()) { if (query.isNotBlank()) NoMatches(query) { query = "" } else Hint("no groups yet: New group, or on the Models tab a model's ⋯ menu > New group with…, or drag one model onto another") }
            shown.groupBy { it.env }.toSortedMap().forEach { (env, groups) ->
                Fold("env $env", groups.size, open = true, key = "groups:$env") {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        groups.forEach { g ->
                            ListRow(
                                name = g.name,
                                active = g.open,
                                selected = selected?.let { it.name == g.name && it.env == g.env } == true,
                                meta = listOfNotNull(
                                    when (g.group.models.size) { 0 -> "no models"; 1 -> "1 model"; else -> "${g.group.models.size} models" },
                                    if (g.missing.isNotEmpty()) "${g.missing.size} missing" else null,
                                    g.group.models.takeIf { it.isNotEmpty() }?.joinToString(),
                                ).joinToString(" · "),
                                status = if (g.open) "open" else "closed",
                                statusTip = if (g.open) "the swarm #${g.swarm?.id} answers group(\"${g.name}\") with ${g.present.size} models" else "no swarm on it; Open swarm, or group(\"${g.name}\") in a script, opens one",
                                underline = g.open,
                                icon = g.icon,
                                menu = menuOfGroup(g),
                            ) { onSelect("${g.name}@${g.env}") }
                        }
                    }
                }
            }
        }
        VerticalSeparator { listWidth = it; ModelsCache.listWidth = it }
        if (selected != null) GroupDetail(
            group = selected,
            models = snapshot.models,
            pending = pending,
            menu = menuOfGroup(selected),
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onOpen = { onGroupOpen(selected) },
            onTerminate = { onGroupTerminate(selected) },
            onAdd = { model -> onAddToGroup(model, selected) },
            onRemove = { onGroupRemove(selected, it) },
            onIcon = { onGroupIcon(selected, it) },
            onSelectModel = onShowModel,
            onTerminateSession = onTerminate,
        ) else Spacer(Modifier.weight(1f))
    }
}

/**
 * The selected group: its icon and status in the header, Open swarm / Terminate swarm, then its models (each a
 * row that selects the model, with Detach to take it out), the models that can still be added, the icon picker,
 * where the group lives (its env's groups.json) and the swarm's log while it is open.
 */
@Composable
private fun GroupDetail(
    group: GroupInfo,
    models: List<ModelInfo>,
    pending: Set<String>,
    menu: List<ContextMenuItem>,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onTerminate: () -> Unit,
    onAdd: (ModelInfo) -> Unit,
    onRemove: (String) -> Unit,
    onIcon: (String?) -> Unit,
    onSelectModel: (String) -> Unit,
    onTerminateSession: (SessionInfo) -> Unit,
) {
    val opening = "open:group:${group.name}" in pending
    val terminating = "terminate:group:${group.name}" in pending
    val members = group.group.models.map { name -> name to models.firstOrNull { it.name == name } }
    val others = models.filter { it.name !in group.group.models }
    DetailPanel(modifier) {
        DetailHeader(
            name = group.name,
            icon = group.icon,
            active = group.open,
            status = listOfNotNull(
                if (group.open) "swarm #${group.swarm?.id}" else null,
                "env ${group.env}",
                when (group.group.models.size) { 0 -> "no models"; 1 -> "1 model"; else -> "${group.group.models.size} models" },
            ).joinToString(" · "),
            statusTip = if (group.open) "open: group(\"${group.name}\") in a script gets this swarm; ${group.present.size} models answer together" else "closed: Open swarm, or group(\"${group.name}\") in a script, opens a session per model and asks them together",
            menu = menu,
        ) {
            if (group.open) DangerButton(if (terminating) "Terminating…" else "Terminate swarm", enabled = !terminating, icon = Icons.Default.StopCircle, tip = "Close the swarm and the model sessions it opened; a script holding it gets an error on its next call", onClick = onTerminate)
            else AccentButton(if (opening) "Opening…" else "Open swarm", enabled = !opening && group.present.isNotEmpty(), icon = Icons.Default.PlayArrow, tip = if (group.present.isEmpty()) "no model of the group is exported" else "Open a session on every model of the group and ask them together, the same as group(\"${group.name}\") in a script", onClick = onOpen)
        }
        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(top = 10.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Section("MODELS", key = "group:${group.name}:models", actions = {
                if (others.isNotEmpty()) DropButton("Add model", icon = Icons.Default.PlaylistAdd, tip = "Put one more model in the group; a swarm already open does not change until it is opened again", items = others.map { m -> ContextMenuItem(m.name) { onAdd(m) } })
            }) {
                if (members.isEmpty()) Hint("no models yet: Add model, or Add to group on a model's ⋯ menu")
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    members.forEach { (name, model) ->
                        ListRow(
                            name = name,
                            active = model?.running == true,
                            selected = false,
                            meta = model?.let { m -> listOfNotNull(m.lastRun?.let { "accuracy ${pct(it.accuracy)}" } ?: "not validated", "${m.manifest.labels.size} labels").joinToString(" · ") } ?: "not in AMP/modules any more",
                            status = model?.status ?: "missing",
                            statusTip = model?.let { statusTip(it) } ?: "missing: the model was destroyed or renamed; take it out, or build one with this name",
                            underline = model?.running == true,
                            icon = model?.icon ?: Icons.Default.Memory,
                            painter = rememberIconPainter(model?.iconFile),
                            onDetach = { onRemove(name) },
                            menu = listOfNotNull(model?.let { ContextMenuItem("Show $name") { onSelectModel(name) } }, ContextMenuItem("Take $name out") { onRemove(name) }),
                        ) { if (model != null) onSelectModel(name) }
                    }
                }
                if (members.size > 1) Hint("asked together: predict is the best score across them, vote the label most of them give, each every model's own label")
            }
            Section("ICON", key = "group:${group.name}:icon") {
                IconPicker(chosen = group.group.icon, default = "hub") { onIcon(it) }
            }
            Section("WHERE", key = "group:${group.name}:where") {
                KeyValues(
                    "env" to group.env,
                    "file" to File(ModelGroups.dirOf(group.env), "groups.json").absolutePath,
                    "created" to group.group.createdAt.take(19).replace('T', ' '),
                    "script" to "group(\"${group.name}\")" + (if (group.env != org.example.project.components.terminal.window.controller.envBuilder.DEFAULT_ENV) "  in a script sourced with environment(SettingsEnv(\"${group.env}\"))" else ""),
                )
            }
            group.swarm?.let { SessionsOf(listOf(it), onTerminateSession) }
        }
    }
}

/** The catalog as a row of chips, the chosen one lit; the default is chosen when nothing is kept, and choosing it clears the field */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPicker(chosen: String?, default: String, onChoose: (String?) -> Unit) {
    val current = chosen ?: default
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        iconCatalog.forEach { (name, icon) ->
            IconChip(icon, tip = if (name == default) "$name (the default)" else name, active = name == current) { onChoose(if (name == default) null else name) }
        }
    }
}

/**
 * A new group around a model: its name and the env it belongs to (the default one, or any env that has a folder;
 * a script sourced in that env finds it with group("name")). Create is enabled once the name is a group name no
 * group of that env has.
 */
@Composable
private fun NewGroupBar(models: List<String>, envs: List<String>, taken: List<Pair<String, String>>, onCreate: (String, String) -> Unit, onCancel: () -> Unit) {
    val model = models.joinToString(" + ").ifEmpty { "no models yet" }
    var text by remember(model) { mutableStateOf("") }
    var env by remember(model) { mutableStateOf(ModelGroups.currentEnv) }
    val value = text.trim()
    val problem = when {
        value.isEmpty() -> "name the group"
        !GROUP_NAME.matches(value) -> "letters, digits, dots, dashes, underscores and spaces, up to 64"
        (value to env) in taken -> "a group named $value exists in env $env already"
        else -> null
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .height(IntrinsicSize.Min)
            .background(PanelSurface, RectangleShape)
    ) {
        Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(PanelBorderSelected))
        Spacer(Modifier.width(10.dp))
        Text(if (models.isEmpty()) "New group" else "New group with $model", color = LogMessageColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(vertical = 6.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it.filter { c -> c.isLetterOrDigit() || c in "._- " } },
            singleLine = true,
            textStyle = TextStyle(color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(FontColor),
            modifier = Modifier
                .width(220.dp)
                .background(CodeBackground, RectangleShape)
                .border(1.dp, ConsoleBorderColor, RectangleShape)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            decorationBox = { inner -> if (text.isEmpty()) Text("group name", color = MutedFontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace); inner() },
        )
        Spacer(Modifier.width(8.dp))
        DropButton("env $env", accent = false, tip = "The env the group belongs to: a script sourced in it finds the group with group(\"name\"); groups.json of that env keeps it", items = envs.map { e -> ContextMenuItem(e) { env = e } })
        Spacer(Modifier.width(10.dp))
        Text(problem ?: "kept in AMP/env/$env/groups.json; more models join from their ⋯ menu", color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        AccentButton("Create", enabled = problem == null, icon = Icons.Default.Add, tip = problem ?: if (models.isEmpty()) "Make the empty group $value in env $env; Add model fills it" else "Make the group $value in env $env with $model in it") { onCreate(value, env) }
        Spacer(Modifier.width(4.dp))
        SmallButton("Cancel", tip = "Make no group", onClick = onCancel)
        Spacer(Modifier.width(4.dp))
    }
}

/**
 * A flag for a jar: a word and a colour from the six; it goes beside the version flags of the jar and is kept
 * in the sidecar metadata next to the jar.
 */
@Composable
private fun FlagBar(jar: String, onAdd: (String, String) -> Unit, onCancel: () -> Unit) {
    var text by remember(jar) { mutableStateOf("") }
    var color by remember(jar) { mutableStateOf("slate") }
    val value = text.trim()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .height(IntrinsicSize.Min)
            .background(PanelSurface, RectangleShape)
    ) {
        Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(flagColor(color)))
        Spacer(Modifier.width(10.dp))
        Text("Flag $jar", color = LogMessageColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(vertical = 6.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it.take(24) },
            singleLine = true,
            textStyle = TextStyle(color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(FontColor),
            modifier = Modifier
                .width(180.dp)
                .background(CodeBackground, RectangleShape)
                .border(1.dp, ConsoleBorderColor, RectangleShape)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            decorationBox = { inner -> if (text.isEmpty()) Text("stable, needs key, v2 api…", color = MutedFontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace); inner() },
        )
        Spacer(Modifier.width(10.dp))
        flagColors.forEach { (name, c) ->
            AppTooltip(name) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(c.copy(alpha = if (name == color) 0.9f else 0.35f), RectangleShape)
                        .border(1.dp, if (name == color) FontColor else c, RectangleShape)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { color = name }
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(6.dp))
        if (value.isNotEmpty()) FlagBadge(Flag(value, flagColor(color)))
        Spacer(Modifier.weight(1f))
        AccentButton("Add flag", enabled = value.isNotEmpty(), icon = Icons.Default.Flag, tip = if (value.isEmpty()) "type the flag" else "Put the flag $value on $jar") { onAdd(value, color) }
        Spacer(Modifier.width(4.dp))
        SmallButton("Cancel", tip = "No flag", onClick = onCancel)
        Spacer(Modifier.width(4.dp))
    }
}

/**
 * A new name for a model: the field starts with the current one; Rename is enabled once the name is a module name
 * that no other model has. The folder moves, the sessions on it are closed, and a listener comes back under the new name.
 */
@Composable
private fun RenameBar(name: String, taken: List<String>, onRename: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember(name) { mutableStateOf(name) }
    val value = text.trim()
    val problem = when {
        value == name -> "type the new name"
        !MODULE_NAME.matches(value) -> "letters, digits, dots, dashes and underscores, up to 64"
        value in taken -> "a model named $value exists already"
        else -> null
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .height(IntrinsicSize.Min)
            .background(PanelSurface, RectangleShape)
    ) {
        Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(PanelBorderSelected))
        Spacer(Modifier.width(10.dp))
        Text("Rename $name", color = LogMessageColor, fontSize = 12.sp, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(vertical = 6.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it.filter { c -> c.isLetterOrDigit() || c in "._-" } },
            singleLine = true,
            textStyle = TextStyle(color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(FontColor),
            modifier = Modifier
                .width(260.dp)
                .background(CodeBackground, RectangleShape)
                .border(1.dp, ConsoleBorderColor, RectangleShape)
                .padding(horizontal = 8.dp, vertical = 5.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(problem ?: "its sessions close and a listener comes back under the new name; scripts then open it with initMod(\"$value\")", color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.SansSerif, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        AccentButton("Rename", enabled = problem == null, icon = Icons.Default.DriveFileRenameOutline, tip = problem ?: "Rename $name to $value") { onRename(value) }
        Spacer(Modifier.width(4.dp))
        SmallButton("Cancel", tip = "Keep the name", onClick = onCancel)
        Spacer(Modifier.width(4.dp))
    }
}

/** When a filter leaves nothing: says so, with the query, and clears it */
@Composable
private fun NoMatches(query: String, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Hint("nothing matches “${query.trim()}”")
        Spacer(Modifier.width(8.dp))
        SmallButton("Clear", tip = "Show everything again", onClick = onClear)
    }
}

/* ---------- menus ---------- */

/**
 * The page's menus: a square dark box with one line per action, the ones that end or remove something in red.
 * Right-click opens it under the pointer on every row; the ⋯ button on a row or a panel header opens the same
 * one under itself, so nothing is reachable only by right-click. Esc or a click elsewhere closes it.
 */
private object SquareContextMenu : ContextMenuRepresentation {
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        if (state.status !is ContextMenuState.Status.Open) return
        Popup(
            popupPositionProvider = rememberCursorPositionProvider(),
            onDismissRequest = { state.status = ContextMenuState.Status.Closed },
            properties = PopupProperties(focusable = true),
        ) {
            MenuBox(items()) { state.status = ContextMenuState.Status.Closed }
        }
    }
}

/**
 * The ⋯ button: opens the same menu the right-click opens, under itself. With shown = false it is invisible but
 * keeps its place, so a row's layout doesn't move when the pointer arrives; an open menu keeps it visible.
 */
@Composable
private fun MoreButton(items: List<ContextMenuItem>, shown: Boolean = true, tip: String = "More actions, the same ones a right-click opens") {
    var open by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val visible = shown || open
    Box {
        AppTooltip(if (visible && !open) tip else null) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(if (open || (hovered && visible)) ChipHoverBackground else Color.Transparent, RectangleShape)
                    .border(1.dp, if (open || (hovered && visible)) ChipBorderColor else Color.Transparent, RectangleShape)
                    .hoverable(interactionSource)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(enabled = visible, interactionSource = interactionSource, indication = null) { open = !open },
                contentAlignment = Alignment.Center,
            ) {
                if (visible) Icon(Icons.Default.MoreHoriz, contentDescription = "more actions", tint = if (hovered || open) FontColor else MutedFontColor, modifier = Modifier.size(14.dp))
            }
        }
        if (open) Popup(popupPositionProvider = BelowButton, onDismissRequest = { open = false }, properties = PopupProperties(focusable = true)) {
            MenuBox(items) { open = false }
        }
    }
}
