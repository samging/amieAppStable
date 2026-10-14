package org.example.project.components.terminal.window.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import org.example.project.backend.BackendStatus
import org.example.project.components.terminal.window.controller.envBuilder.DEFAULT_ENV
import org.example.project.components.terminal.window.controller.envBuilder.ScriptEnvironments
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.ModelBuild
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.ModelBuildProgress
import org.example.project.components.terminal.window.controller.jetbrainDLBuilder.short
import org.example.project.components.terminal.window.controller.modelLoaders.OrtSessions
import org.example.project.components.terminal.window.controller.modules.ModelServer
import org.example.project.components.terminal.window.controller.modules.ModuleManifest
import org.example.project.components.terminal.window.controller.modules.ServingSettings
import org.example.project.components.terminal.window.controller.modules.SystemStats
import org.example.project.components.terminal.window.controller.modules.bytes
import org.example.project.components.terminal.window.controller.modules.modulesDir
import org.example.project.components.terminal.window.controller.modules.ms
import org.example.project.components.terminal.window.controller.modules.percent
import org.example.project.components.terminal.window.controller.modules.readMetricsHistory
import org.example.project.components.terminal.window.controller.modules.readModule
import org.example.project.components.terminal.window.controller.modules.readRegistry
import org.example.project.components.terminal.window.controller.modules.readServing
import org.example.project.components.terminal.window.controller.modules.uptime
import org.example.project.components.terminal.window.controller.modules.writeServing
import org.example.project.components.terminal.window.controller.pluginEngine.JarInterpreter
import org.example.project.components.ui.viewport.NavigationSidebar
import org.example.project.getDirectory
import org.example.project.ui.theme.AddButton
import org.example.project.ui.theme.ConsoleBackground
import org.example.project.ui.theme.DangerColor
import org.example.project.ui.theme.EditorBackground
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.SuccessColor
import org.example.project.ui.theme.SurfaceHover
import org.example.project.ui.theme.UnifiedBorderColor
import org.example.project.ui.theme.UnifiedBoxColor
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ---------- what the page shows, read off the UI thread ---------- */

private data class ModelInfo(
    val name: String,
    val manifest: ModuleManifest,
    val modelBytes: Long,
    val macroF1: Double?,
    val accuracy: Double?,
    val validation: String?,
    val validatedAt: String?,
    val builds: Int,
    val sessions: Int,
    val sessionRuns: Int,
    val sessionRunTimeMs: Long,
    val sessionAverageMs: Double,
    val incoming: Int,
    val serving: ServingSettings,
    val stats: ModelServer.ServingStats?,
) {
    val listening: Boolean get() = stats != null
}

private data class JarInfo(val name: String, val file: File, val bytes: Long, val running: Boolean, val inEnv: Boolean)

private data class SessionInfo(val id: Int, val name: String, val running: Int, val runs: Int, val runTimeMs: Long, val averageMs: Double, val openedAt: Instant)

private data class ModelsSnapshot(
    val system: SystemStats? = null,
    val models: List<ModelInfo> = emptyList(),
    val jars: List<JarInfo> = emptyList(),
    val sessions: List<SessionInfo> = emptyList(),
)

private fun readSnapshot(): ModelsSnapshot {
    val names = (readRegistry().modules.keys + (modulesDir.listFiles()?.filter { File(it, "module.json").isFile }?.map { it.name } ?: emptyList())).distinct().sorted()
    val ortSessions = OrtSessions.all
    val models = names.mapNotNull { name ->
        val (dir, manifest) = readModule(name) ?: return@mapNotNull null
        val history = readMetricsHistory(dir).runs
        val last = history.lastOrNull()
        val own = ortSessions.filter { it.name == "module:$name" }
        val runs = own.sumOf { it.runs }
        val runTime = own.sumOf { it.runTimeMs }
        ModelInfo(
            name = name,
            manifest = manifest,
            modelBytes = File(dir, manifest.model).length(),
            macroF1 = last?.macroF1,
            accuracy = last?.accuracy,
            validation = last?.validation,
            validatedAt = last?.createdAt,
            builds = history.size,
            sessions = own.size,
            sessionRuns = runs,
            sessionRunTimeMs = runTime,
            sessionAverageMs = if (runs == 0) 0.0 else runTime.toDouble() / runs,
            incoming = File(dir, "incoming.jsonl").takeIf { it.isFile }?.useLines { it.count() } ?: 0,
            serving = readServing(name),
            stats = ModelServer.stats(name),
        )
    }

    val env = ScriptEnvironments.current
    val envJars = env?.libsDir?.listFiles { f -> f.extension == "jar" }?.toList() ?: emptyList()
    val pluginJars = File(getDirectory().toString(), "pluginDir").listFiles { f -> f.extension == "jar" }?.toList() ?: emptyList()
    val running = JarInterpreter.shared.running.toSet()
    val jars = (envJars + pluginJars).distinctBy { it.name }.sortedBy { it.name }.map {
        JarInfo(it.name, it, it.length(), it.name in running, envJars.any { e -> e.name == it.name })
    }

    val sessions = ortSessions.map { SessionInfo(it.id, it.name, it.running, it.runs, it.runTimeMs, it.averageRunMs, it.openedAt) }
    return ModelsSnapshot(SystemStats.read(), models, jars, sessions)
}

/* ---------- page ---------- */

/**
 * The models route: what the app process uses, every intent model with its uptime, traffic, latency and
 * validation, the model being built, plugin jars and open ORT sessions. Everything comes from local files under
 * AMP/ (modules/<name>/metrics.json, serving.json, pluginDir), so a guest sees it too; only the Amie backend card
 * and Publish need a Keycloak sign-in.
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
    var snapshot by remember { mutableStateOf(ModelsSnapshot()) }
    var refresh by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val build by ModelBuildProgress.current.collectAsState()
    val buildHistory by ModelBuildProgress.history.collectAsState()

    // Files, sessions and servers change underneath, so the page re-reads them while it is open
    LaunchedEffect(refresh) {
        while (true) {
            snapshot = withContext(Dispatchers.IO) { runCatching { readSnapshot() }.getOrDefault(snapshot) }
            delay(1500)
        }
    }

    fun background(block: suspend () -> Unit) = scope.launch {
        withContext(Dispatchers.IO) { runCatching { block() }.onFailure { logInternal("models: ${it.message}", LogLevel.ERROR) } }
        refresh++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Models", color = FontColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
            Spacer(Modifier.weight(1f))
            Meta(if (user != null) "signed in as $user · refreshes every 1.5s" else "guest · read from AMP/modules · refreshes every 1.5s")
        }
        Spacer(Modifier.height(14.dp))

        snapshot.system?.let { SystemOverview(it, models = snapshot.models) }

        if (user != null) {
            BackendCard(user = user, publishedNames = snapshot.models.map { it.name })
        } else {
            SectionTitle("AMIE BACKEND", 0) { SmallButton("Sign in", AddButton, onClick = onLogin) }
            Hint("Sign in with Keycloak to publish models to the Amie backend. Statistics and metrics stay local and need no account.")
        }

        build?.let {
            SectionTitle("BUILDING", 1)
            BuildCard(it)
        }

        SectionTitle("INTENT MODELS", snapshot.models.size)
        if (snapshot.models.isEmpty() && build == null) Hint("No models yet. Build one in the sandbox with amie.modelBuilder(\"name\", data).")
        snapshot.models.forEach { model ->
            ModelCard(
                model = model,
                lastBuild = buildHistory.firstOrNull { it.modelName == model.name },
                onSave = { settings -> background { writeServing(model.name, settings.copy(enabled = model.listening)) } },
                onListen = { on, settings ->
                    background { if (on) ModelServer.start(model.name, settings) else ModelServer.stop(model.name) }
                },
                onPublish = user?.let { signedIn ->
                    {
                        background {
                            val (dir, _) = readModule(model.name) ?: error("module '${model.name}' not found")
                            AmieBackend.publishModule(model.name, dir, signedIn)
                            BackendCardState.refresh()
                        }
                    }
                },
            )
        }

        SectionTitle("JAR PLUGINS", snapshot.jars.size) {
            SmallButton("+ Add jar", AddButton) {
                val chosen = chooseJar() ?: return@SmallButton
                background { addJar(chosen) }
            }
        }
        if (snapshot.jars.isEmpty()) Hint("Add a jar, or put one in AMP/pluginDir.")
        snapshot.jars.forEach { jar ->
            JarRow(
                jar = jar,
                onRun = { background { JarInterpreter.shared.run(jar.name) } },
                onQuit = { background { JarInterpreter.shared.quit(jar.name) } },
            )
        }

        SectionTitle("ORT SESSIONS", snapshot.sessions.size) {
            if (snapshot.sessions.isNotEmpty()) SmallButton("Cancel all", DangerColor) { background { OrtSessions.cancelAll() } }
        }
        if (snapshot.sessions.isEmpty()) Hint("No open sessions.")
        snapshot.sessions.forEach { s ->
            SessionRow(s, onCancel = { background { OrtSessions.cancel(s.id.toString()) } })
        }
        Spacer(Modifier.height(24.dp))
    }
}

/* ---------- overview ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemOverview(system: SystemStats, models: List<ModelInfo>) {
    val requests = models.sumOf { it.stats?.requests ?: 0 }
    val errors = models.sumOf { it.stats?.errors ?: 0 }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("APP UPTIME", uptime(system.uptime))
        StatTile("MODELS", "${models.size}", "${system.listening} listening")
        StatTile("REQUESTS", "$requests", if (errors > 0) "$errors errors" else "no errors", if (errors > 0) DangerColor else null)
        StatTile("ORT SESSIONS", "${system.ortSessions}", "${system.ortRuns} runs · ${ms(system.ortRunTimeMs.toDouble())} in native")
        StatTile("HEAP", bytes(system.heapUsedBytes), "of ${bytes(system.heapMaxBytes)}", fraction = system.heapUsedBytes.toFloat() / system.heapMaxBytes)
        StatTile("CPU", percent(system.processCpu), "process · system ${percent(system.systemCpu)} · ${system.processors} cores", fraction = system.processCpu.toFloat().takeIf { it >= 0 })
        StatTile("THREADS", "${system.threads}", "non-heap ${bytes(system.nonHeapUsedBytes)}")
    }
}

@Composable
private fun StatTile(label: String, value: String, detail: String? = null, accent: Color? = null, fraction: Float? = null) {
    Column(
        modifier = Modifier
            .widthIn(min = 150.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(UnifiedBoxColor)
            .border(1.dp, UnifiedBorderColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(label, color = MutedFontColor, fontSize = 10.sp, letterSpacing = 1.sp, fontFamily = FontFamily.SansSerif)
        Spacer(Modifier.height(4.dp))
        Text(value, color = accent ?: FontColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
        if (detail != null) Meta(detail)
        if (fraction != null) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = fraction.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = if (fraction > 0.85f) DangerColor else AddButton,
                backgroundColor = UnifiedBorderColor,
            )
        }
    }
}

/* ---------- cards ---------- */

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelCard(
    model: ModelInfo,
    lastBuild: ModelBuild?,
    onSave: (ServingSettings) -> Unit,
    onListen: (Boolean, ServingSettings) -> Unit,
    // Null for a guest: publishing needs the Keycloak session
    onPublish: (() -> Unit)? = null,
) {
    var expanded by remember(model.name) { mutableStateOf(false) }
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
    val stats = model.stats

    Card {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { expanded = !expanded }) {
            Text(if (expanded) "▾" else "▸", color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.width(14.dp))
            Text(model.name, color = FontColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            StatusDot(if (model.listening) SuccessColor else MutedFontColor.copy(alpha = 0.5f))
            Spacer(Modifier.width(6.dp))
            Text(
                if (stats != null) "listening · up ${uptime(stats.uptime)}" else "off",
                color = if (model.listening) SuccessColor else MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(10.dp))

        // Resource and traffic figures; serving ones only while it listens
        FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Figure("uptime", stats?.let { uptime(it.uptime) } ?: "—")
            Figure("requests", stats?.requests?.toString() ?: "—")
            Figure("errors", stats?.errors?.toString() ?: "—", if ((stats?.errors ?: 0) > 0) DangerColor else null)
            Figure("avg latency", stats?.takeIf { it.requests > 0 }?.let { ms(it.averageLatencyMs) } ?: "—")
            Figure("max latency", stats?.takeIf { it.requests > 0 }?.let { ms(it.maxLatencyMs.toDouble()) } ?: "—")
            Figure("last request", stats?.lastRequestAt?.let { timeFormat.format(it) } ?: "—")
            Figure("sessions", "${model.sessions}")
            Figure("session runs", "${model.sessionRuns}")
            Figure("native time", ms(model.sessionRunTimeMs.toDouble()))
            Figure("avg run", if (model.sessionRuns > 0) ms(model.sessionAverageMs) else "—")
            Figure("incoming rows", "${model.incoming}")
            Figure("size", bytes(model.modelBytes))
            Figure("accuracy", model.accuracy?.let { "%.3f".format(Locale.US, it) } ?: "—")
            Figure("macro F1", model.macroF1?.let { "%.3f".format(Locale.US, it) } ?: "—")
            Figure("builds", "${model.builds}")
        }

        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Meta("labels     ${model.manifest.labels.joinToString().ifEmpty { "—" }}")
            Meta("encoder    ${File(model.manifest.encoder).name} · input ${model.manifest.inputDim} → ${model.manifest.outputDim} (${model.manifest.pooling} pooling)")
            Meta("validated  on ${model.validation ?: "—"}${model.validatedAt?.let { " at ${fmtInstant(it)}" } ?: ""}")
            Meta("created    ${fmtInstant(model.manifest.createdAt)} · format ${model.manifest.format}")
            lastBuild?.let { Meta("last build ${it.phase.label} in ${it.elapsed.short()}${it.detail.takeIf { d -> d.isNotBlank() }?.let { d -> " · $d" } ?: ""}") }
            stats?.let { Meta("serving    ${it.methods.joinToString("/")} ${it.url} since ${timeFormat.format(it.startedAt)}") }
            Spacer(Modifier.height(10.dp))

            SettingField("Host", host) { host = it }
            SettingField("Port", port) { port = it.filter(Char::isDigit).take(5) }
            SettingField("Path", path) { path = it }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text("Methods", color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.width(48.dp))
                Toggle("POST", allowPost) { allowPost = it }
                Spacer(Modifier.width(6.dp))
                Toggle("GET", allowGet) { allowGet = it }
                Spacer(Modifier.weight(1f))
                SmallButton("Save", AddButton) { onSave(settings) }
            }

            Spacer(Modifier.height(10.dp))
            SelectionContainer {
                Column {
                    if (allowPost) UrlLine("POST", settings.url(model.name))
                    if (allowGet) UrlLine("GET", settings.url(model.name) + "?text=…")
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Listen for incoming data", color = FontColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = model.listening,
                    onCheckedChange = { onListen(it, settings) },
                    colors = SwitchDefaults.colors(checkedTrackColor = SuccessColor, uncheckedTrackColor = UnifiedBoxColor),
                )
            }
            if (model.listening) Meta("changes to host, port or path apply after switching off and on")

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Publish to Amie", color = FontColor, fontSize = 12.sp)
                    Meta(
                        if (onPublish != null) "uploads model.onnx and module.json to your package folder; the website's dashboard lists them"
                        else "sign in with Keycloak to upload this model to the Amie backend"
                    )
                }
                if (onPublish != null) SmallButton("Publish", AddButton, onClick = onPublish)
            }
        }
    }
}

/* ---------- Amie backend ---------- */

/** The last backend check, shared so a publish can refresh the card */
private object BackendCardState {
    val status = kotlinx.coroutines.flow.MutableStateFlow<BackendStatus?>(null)
    val busy = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    fun refresh() {
        if (busy.value) return
        busy.value = true
        scope.launch {
            status.value = runCatching { AmieBackend.status() }.getOrElse {
                BackendStatus(java.time.Instant.now(), reachable = false, problem = it.message)
            }
            busy.value = false
        }
    }
}

/**
 * Whether the Amie backend accepts this Keycloak session, how it names the user, what the user has
 * published, and a link to the website's dashboard that lists the same files.
 */
@Composable
private fun BackendCard(user: String, publishedNames: List<String>) {
    val status by BackendCardState.status.collectAsState()
    val busy by BackendCardState.busy.collectAsState()
    val config = AmieBackend.config
    LaunchedEffect(user) { BackendCardState.refresh() }

    SectionTitle("AMIE BACKEND", status?.packages?.size ?: 0) {
        SmallButton(if (busy) "Checking…" else "Check again", AddButton) { BackendCardState.refresh() }
        Spacer(Modifier.width(6.dp))
        SmallButton("Open website", AddButton) {
            runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(config.siteDashboardUrl)) }
                .onFailure { logInternal("models: couldn't open ${config.siteDashboardUrl}: ${it.message}", LogLevel.ERROR) }
        }
    }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(
                when {
                    status == null -> MutedFontColor.copy(alpha = 0.5f)
                    status!!.profile != null -> SuccessColor
                    status!!.reachable -> DangerColor
                    else -> DangerColor
                }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    status == null -> "checking ${config.base}"
                    status!!.profile != null -> "${config.base} · signed in as ${status!!.profile!!.username}"
                    status!!.reachable -> "${config.base} · answered but refused the session"
                    else -> "${config.base} · unreachable"
                },
                color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace
            )
        }
        status?.problem?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = DangerColor, fontSize = 11.sp)
        }
        status?.profile?.let { p ->
            Spacer(Modifier.height(6.dp))
            Meta("profile    ${listOf(p.fullName, p.email).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { "no name or email on the backend yet" }} · edit at ${config.siteProfileUrl}")
        }
        status?.let { st ->
            if (st.profile != null) {
                Spacer(Modifier.height(6.dp))
                if (st.packages.isEmpty()) Meta("no packages on the backend yet; expand a model and press Publish")
                st.packages.take(12).forEach { pkg ->
                    val isModel = publishedNames.any { pkg.path.contains("/onnx/") }
                    Meta("${if (isModel) "model  " else "file   "} ${pkg.path.substringAfter("uploads/").ifEmpty { pkg.name }} · ${bytes(pkg.size)}")
                }
                if (st.packages.size > 12) Meta("… ${st.packages.size - 12} more on the website")
            }
        }
        Spacer(Modifier.height(6.dp))
        Meta("backend and website addresses live in AMP/backend.json (AMIE_BACKEND_URL / AMIE_SITE_URL override)")
    }
    Spacer(Modifier.height(10.dp))
}

private fun fmtInstant(iso: String): String = runCatching { dateFormat.format(Instant.parse(iso)) }.getOrDefault(iso)

/** A model while modelBuilder builds it: phase, step and a progress bar */
@Composable
private fun BuildCard(build: ModelBuild) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(SuccessColor)
            Spacer(Modifier.width(8.dp))
            Text(build.modelName, color = FontColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("building ${(build.fraction * 100).toInt()}%", color = SuccessColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(6.dp))
        Meta(build.describe())
        Meta("started ${build.elapsed.short()} ago")
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = build.fraction,
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = SuccessColor,
            backgroundColor = SuccessColor.copy(alpha = 0.15f),
        )
    }
}

@Composable
private fun JarRow(jar: JarInfo, onRun: () -> Unit, onQuit: () -> Unit) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(if (jar.running) SuccessColor else MutedFontColor.copy(alpha = 0.5f))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(jar.name, color = FontColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Meta("${bytes(jar.bytes)}${if (jar.inEnv) " · in env" else ""}${if (jar.running) " · running" else ""}")
            }
            SmallButton("Run", SuccessColor, onClick = onRun)
            Spacer(Modifier.width(6.dp))
            SmallButton("Quit", DangerColor, enabled = jar.running, onClick = onQuit)
        }
    }
}

@Composable
private fun SessionRow(session: SessionInfo, onCancel: () -> Unit) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(if (session.running > 0) SuccessColor else MutedFontColor.copy(alpha = 0.5f))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("#${session.id} ${session.name}", color = FontColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Meta(
                    "${if (session.running > 0) "running ${session.running}" else "idle"} · runs ${session.runs} · " +
                        "native ${ms(session.runTimeMs.toDouble())}${if (session.runs > 0) " · avg ${ms(session.averageMs)}" else ""} · " +
                        "open ${uptime(Duration.between(session.openedAt, Instant.now()))}"
                )
            }
            SmallButton("Cancel", DangerColor, onClick = onCancel)
        }
    }
}

// Copies the jar into AMP/pluginDir and imports it into the env so scripts and the page can run it
private fun addJar(file: File) {
    val pluginDir = File(getDirectory().toString(), "pluginDir").apply { mkdirs() }
    val target = File(pluginDir, file.name)
    if (file.canonicalPath != target.canonicalPath) file.copyTo(target, overwrite = true)
    val env = ScriptEnvironments.current ?: ScriptEnvironments.open(DEFAULT_ENV)
    env.import(file.name)
}

private fun chooseJar(): File? {
    val dialog = FileDialog(null as Frame?, "Add jar", FileDialog.LOAD).apply {
        setFilenameFilter { _, name -> name.endsWith(".jar") }
        isVisible = true
    }
    return dialog.file?.let { File(dialog.directory, it) }?.takeIf { it.extension == "jar" }
}

/* ---------- pieces ---------- */

@Composable
private fun SectionTitle(title: String, count: Int, action: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$title  $count", color = FontColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, modifier = Modifier.weight(1f))
        action()
    }
}

@Composable
private fun Figure(label: String, value: String, accent: Color? = null) {
    Column {
        Text(label, color = MutedFontColor, fontSize = 10.sp, fontFamily = FontFamily.SansSerif)
        Text(value, color = accent ?: FontColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(UnifiedBoxColor)
            .border(1.dp, UnifiedBorderColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) { content() }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun Meta(text: String) {
    Text(text, color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
}

@Composable
private fun StatusDot(color: Color) {
    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
}

@Composable
private fun UrlLine(method: String, url: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            method,
            color = FontColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(40.dp).background(SurfaceHover, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(url, color = FontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SettingField(label: String, value: String, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.width(48.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(FontColor),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .background(ConsoleBackground, RoundedCornerShape(6.dp))
                .border(1.dp, UnifiedBorderColor, RoundedCornerShape(6.dp))
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
            .background(if (on) AddButton.copy(alpha = 0.25f) else ConsoleBackground)
            .border(1.dp, if (on) AddButton.copy(alpha = 0.6f) else UnifiedBorderColor, RoundedCornerShape(6.dp))
            .clickable { onChange(!on) }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun SmallButton(label: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Text(
        label,
        color = if (enabled) color else MutedFontColor.copy(alpha = 0.5f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled && hovered) color.copy(alpha = 0.18f) else color.copy(alpha = if (enabled) 0.08f else 0.03f))
            .hoverable(interactionSource)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}
