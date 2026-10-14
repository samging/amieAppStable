package org.example.project.components.terminal.window.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.LaunchedEffect
import org.example.project.data.remote.parser.DeviceManagerJson
import org.example.project.data.remote.parser.PackageWrite
import org.example.project.util.readLogFile
import org.slf4j.LoggerFactory
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.selection.SelectionContainer
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.components.render.templates.Client
import org.example.project.components.render.templates.FallbackTypes
import org.example.project.components.render.templates.RestType
import org.example.project.backend.ErrorReporter
import org.example.project.getDirectory
import java.io.File
import java.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.example.project.components.terminal.integrations.amiePilot.InteractionModel
import org.example.project.ui.theme.UnifiedBodyBackground
import org.example.project.ui.theme.UnifiedBoxColor
import org.example.project.ui.theme.UnifiedBorderColor
import org.example.project.ui.theme.AddButton
import org.example.project.ui.theme.LogDataColor
import org.example.project.ui.theme.LogErrorColor
import org.example.project.ui.theme.LogInfoColor
import org.example.project.ui.theme.LogInternalColor
import org.example.project.ui.theme.LogTimestampColor
import org.example.project.ui.theme.LogMessageColor
import org.example.project.ui.theme.LogErrorTextColor
import org.example.project.ui.theme.LogDataTextColor
import org.example.project.ui.theme.LogInternalTextColor
import org.example.project.ui.theme.LogRowStripe
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.script.ScriptEngineManager
import javax.swing.SwingUtilities
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

@Serializable
data class TerminalTask(
    @Serializable(with = InstantSerializer::class)
    val time: Instant? = null,
    val task: String? = null,
    val status: Int? = null,
)

@Serializable
enum class LogLevel { INFO, ERROR, DATA, INTERNAL }

@Serializable
data class ResponseDto(
    @Serializable(with = InstantSerializer::class)
    val time: Instant? = null,
    val status: Int? = null,
    val message: String? = null,
    val color: Long? = null,
    val level: LogLevel = LogLevel.INFO,
    // Who logged it, e.g. "sandbox", "sandbox/main.kts", "terminal/<deviceId>"
    val source: String? = null,
)

@Serializable
data class GithubItemMetadata(
     val name: String? = null,
     val downloadUrl: String? = null,
     val id: String? = null,
    val type: String = "file",
    val endComp: String? = null,
)
val consoleData = mutableStateListOf<ResponseDto>()
val logger = LoggerFactory.getLogger("TerminalWindow")

// Single log file shared by every terminal; entries are told apart by their source tag
val appLogFile: File get() = File(getDirectory().toString(), "logs.txt")

// One writer thread keeps appends in order and off the UI thread
private val logWriter = Executors.newSingleThreadExecutor { Thread(it, "amie-log-writer").apply { isDaemon = true } }

// consoleData is read by composition on the UI thread, so writes from script/IO threads are hopped onto it
fun logToConsole(dto: ResponseDto) {
    if (SwingUtilities.isEventDispatchThread()) consoleData.add(dto)
    else SwingUtilities.invokeLater { consoleData.add(dto) }
    val line = dto.toLogLine()
    logWriter.execute {
        try {
            appLogFile.appendText("$line\n")
        } catch (e: Exception) {
            logger.error("Error writing to log file: ${e.message}")
        }
    }
    // Errors also go to the signed-in user's log on the backend; report() only queues, the request runs elsewhere
    if (dto.level == LogLevel.ERROR) ErrorReporter.report(dto.source ?: "app", dto.message ?: "")
}

// Runtime/model internals; shown in every terminal so they also reach the logging terminal
const val INTERNAL_SOURCE = "internal"
fun logInternal(message: String, level: LogLevel = LogLevel.INTERNAL) = logToConsole(ResponseDto(time = Instant.now(), message = message, level = level, source = INTERNAL_SOURCE))

fun logInfo(message: String, source: String) = logToConsole(ResponseDto(time = Instant.now(), message = message, level = LogLevel.INFO, source = source))
fun logError(message: String, source: String) = logToConsole(ResponseDto(time = Instant.now(), message = message, level = LogLevel.ERROR, source = source))
fun logData(message: String, source: String) = logToConsole(ResponseDto(time = Instant.now(), message = message, level = LogLevel.DATA, source = source))

// Empties the on-screen output for a source and its children; the log file keeps everything
fun clearConsole(source: String) {
    consoleData.removeAll { it.belongsTo(source) }
}

// What a terminal for [source] shows, before level filtering
fun consoleEntries(source: String): List<ResponseDto> = consoleData.filter { it.belongsTo(source) }

// Untagged and internal entries (model loaders, plugins, NER) are shown in every terminal
private fun ResponseDto.belongsTo(source: String): Boolean =
    this.source == null || this.source == INTERNAL_SOURCE || this.source == source || this.source.startsWith("$source/")

private val logTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
private val logLineRegex = Regex("""^> \[(INFO|ERROR|DATA|INTERNAL)] (?:\[([^\]]+)] )?(?:(\S+Z) )?(.*)$""", RegexOption.DOT_MATCHES_ALL)

private fun ResponseDto.text(): String = message ?: "$time $status"

// Persisted as "> [LEVEL] [source] <instant> <message>"; multi-line messages are escaped to keep one entry per line
private fun ResponseDto.toLogLine(): String {
    val escaped = text().replace("\\", "\\\\").replace("\n", "\\n")
    return listOfNotNull("> [${level.name}]", source?.let { "[$it]" }, time?.toString(), escaped).joinToString(" ")
}

private fun parseLogLine(line: String): ResponseDto {
    val match = logLineRegex.find(line) ?: return ResponseDto(message = line.removePrefix("> "))
    val (level, source, time, message) = match.destructured
    return ResponseDto(
        source = source.takeIf { it.isNotEmpty() },
        time = time.takeIf { it.isNotEmpty() }?.let { runCatching { Instant.parse(it) }.getOrNull() },
        message = message.replace(Regex("""\\(\\|n)""")) { if (it.value == "\\n") "\n" else "\\" },
        level = LogLevel.valueOf(level),
    )
}

fun LogLevel.color(): Color = when (this) {
    LogLevel.INFO -> LogInfoColor
    LogLevel.ERROR -> LogErrorColor
    LogLevel.DATA -> LogDataColor
    LogLevel.INTERNAL -> LogInternalColor
}

// Fixed columns keep every entry aligned: time | level | source | message (wraps within its own column)
private val ConsoleTimeWidth = 64.dp
private val ConsoleLevelWidth = 72.dp
private val ConsoleSourceWidth = 104.dp
private val ConsoleLineHeight = 20.sp

// "sandbox/main.kts" -> "main.kts"; the full source stays in the log file
private fun ResponseDto.shortSource(): String = source?.substringAfterLast('/') ?: ""

// Below this width the source column is dropped so the message keeps room to breathe
private val ConsoleCompactWidth = 640.dp

@Composable
private fun ConsoleLine(msg: ResponseDto, compact: Boolean = false, striped: Boolean = false) {
    val levelColor = msg.level.color()
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = ConsoleLineHeight)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (striped) LogRowStripe else Color.Transparent, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = msg.time?.let { logTimeFormat.format(it) } ?: "--:--:--",
            style = mono.copy(color = LogTimestampColor, fontSize = 11.sp),
            modifier = Modifier.width(ConsoleTimeWidth)
        )
        // Same height as one text line so the badge sits centered on the first line
        Box(modifier = Modifier.width(ConsoleLevelWidth).height(20.dp), contentAlignment = Alignment.CenterStart) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(ConsoleLevelWidth - 10.dp)
                    .background(levelColor.copy(alpha = 0.16f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Box(modifier = Modifier.size(5.dp).background(levelColor, RoundedCornerShape(50)))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = msg.level.name.lowercase(),
                    style = mono.copy(color = levelColor, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }
        if (!compact) {
            Text(
                text = msg.shortSource(),
                style = mono.copy(color = LogTimestampColor, fontSize = 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(ConsoleSourceWidth).padding(end = 8.dp)
            )
        }
        Text(
            text = msg.text(),
            style = mono.copy(
                fontSize = 13.sp,
                // Ordinary lines in a neutral light grey; errors and data in a light tint of their level
                color = msg.color?.let { Color(it) } ?: when (msg.level) {
                    LogLevel.INFO -> LogMessageColor
                    LogLevel.ERROR -> LogErrorTextColor
                    LogLevel.DATA -> LogDataTextColor
                    LogLevel.INTERNAL -> LogInternalTextColor
                }
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TerminalWindow(
    title: String = "",
    deviceId: String = "",
    modifier: Modifier = Modifier,
    source: String = if (deviceId.isNotEmpty()) "terminal/$deviceId" else "terminal",
    loadHistory: Boolean = true,
    // Levels shown; the others stay in consoleData and the log file
    levels: Set<LogLevel> = LogLevel.entries.toSet(),
    allowCmd: Boolean? = false,
    containerColor: Color = UnifiedBodyBackground,
    inputColor: Color = UnifiedBoxColor,
    inputBorderColor: Color = UnifiedBorderColor,
) {
    val deviceManagerJson = remember { DeviceManagerJson() }
    var portName by remember { mutableStateOf<String?>(null) }
    val client = remember { Client() }
    var messageOver by remember { mutableStateOf(TerminalTask()) }
    val loop = rememberCoroutineScope()

    // Tags everything this terminal logs with its source unless the caller named a more specific one
    fun logToConsole(dto: ResponseDto) =
        org.example.project.components.terminal.window.controller.logToConsole(dto.copy(source = dto.source ?: source))

    LaunchedEffect(source, loadHistory) {
        if (!loadHistory) return@LaunchedEffect
        try {
            val history = withContext(Dispatchers.IO) {
                val file = appLogFile
                if (!file.exists()) emptyList()
                else file.readLines().filter { it.isNotBlank() }.map(::parseLogLine).filter { it.belongsTo(source) }
            }
            clearConsole(source)
            consoleData.addAll(0, history)
        } catch (e: Exception) {
            logger.error("Error reading log history: ${e.message}")
        }
    }
    val scope = rememberCoroutineScope()

    // Asks the backend to republish the device compatibility endpoints (POST /post-endpoints, no body needed; the
    // backend keeps them in the S3 bucket as `repositoryInformations`) and remembers the task with the answer's status
    suspend fun update(body: TerminalTask ) {
        val response = client.rest<Any>("post-endpoints", restType = RestType.POST, body = body)
        response?.let {
            messageOver = TerminalTask(
                time = Instant.now(),
                task = (it["task"] ?: body.task) as String,
                status = it["status"]?.toString()?.toIntOrNull()
            )
        }
    }

    LaunchedEffect(deviceId) {
        portName = try {
            deviceManagerJson.getDevicePort(deviceId)
        } catch (e: Exception) {
            null
        }
    }



    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(0.dp)),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {


            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val compact = maxWidth < ConsoleCompactWidth
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Column {
                            consoleData.filter { it.belongsTo(source) && it.level in levels }.forEachIndexed { i, msg ->
                                ConsoleLine(msg, compact, striped = i % 2 == 1)
                            }
                        }
                    }
                }
            }

            if (allowCmd == true) {
                var textInput by remember { mutableStateOf("") }

                fun sendCommand() {
                    val rawInput = textInput.trim()
                    if (rawInput.isEmpty()) return
                    val parts = rawInput.split(" ", limit = 2)
                    val command = parts.map{it.lowercase()}
                    val args = if (parts.size > 1) parts[1] else ""

                    if (command.first() == "help") {
                        logToConsole(ResponseDto(time = Instant.now(), message = "hey im here to help you"))
                        textInput = ""
                        return
                    }

                    if (command.first() == "list") {
                        val ampPath = getDirectory().toString()
                        val files = File(ampPath).listFiles()
                        val message = if (files.isNullOrEmpty()) {
                            "AMP directory is empty or not found at $ampPath"
                        } else {
                            files.joinToString("\n") { (if (it.isDirectory) "[DIR] " else "[FILE] ") + it.name }
                        }
                        logToConsole(ResponseDto(time = Instant.now(), message = message, level = LogLevel.DATA))
                        textInput = ""
                        return
                    }

                    if (command.first() == "cat") {
                        if (args.isEmpty()) {
                            logToConsole(ResponseDto(time = Instant.now(), message = "Usage: cat <filename>", level = LogLevel.ERROR))
                        } else {
                            val ampPath = getDirectory().toString()
                            val file = File(ampPath, args)
                            if (file.exists() && file.isFile) {
                                try {
                                    logToConsole(ResponseDto(time = Instant.now(), message = file.readText(), level = LogLevel.DATA))
                                } catch (e: Exception) {
                                    logToConsole(ResponseDto(time = Instant.now(), message = "Error reading file: ${e.message}", level = LogLevel.ERROR))
                                }
                            } else {
                                logToConsole(ResponseDto(time = Instant.now(), message = "File not found: $args", level = LogLevel.ERROR))
                            }
                        }
                        textInput = ""
                        return
                    }

                    if(command.first() == "run"){
                        val manager = ScriptEngineManager(Thread.currentThread().contextClassLoader)

                        val engine = manager.getEngineByExtension("kts")
                            ?: manager.getEngineByName("kotlin")
                            ?: error("Couldn't find Kotlin script engine")
                        
                        val loggerObj = object : ScriptLogger {
                            override fun log(message: String, level: LogLevel) {
                                logToConsole(ResponseDto(time = Instant.now(), message = message, level = level, source = "$source/${command.getOrElse(1) { "script" }}.kts"))
                            }
                            override fun initMod() : InteractionModel {
                                return org.example.project.components.terminal.integrations.amiePilot.InitMod().create()
                            }
                            override fun initAmie(text: String) {
                                val result = org.example.project.components.terminal.integrations.amiePilot.initAmie(text)
                                logToConsole(ResponseDto(time = Instant.now(), message = result.joinToString("\n"), level = LogLevel.DATA))
                            }

                            override fun forwardContext(text: String, tokens: (matrix: LongArray) -> Unit): Array<Array<FloatArray>>? =
                                org.example.project.components.terminal.integrations.amiePilot.initContext(text, tokens)

                            override fun modelBuilder(modelName: String, it: (Any) -> Any): ModelCreationalInterface {
                                it("")
                                try {
                                    val nodes = org.example.project.components.terminal.integrations.amiePilot.getShapeContext()
                                    //org.example.project.components.terminal.integrations.amiePilot.createModel(nodes[0], nodes[1], nodes[2], modelName)
                                } catch (e: java.lang.reflect.InvocationTargetException) {
                                    println("--- ROOT CAUSE OF SCRIPT FAILURE ---")
                                    e.cause?.printStackTrace() // <--- THIS WILL SHOW THE ACTUAL ERROR
                                }

                                return object : ModelCreationalInterface() {

                                }
                            }
                        }
                        
//                        val scriptBindings = engine.createBindings().apply {
//                            put("logger", loggerObj)
//                        }
//
//                        loop.launch {
//                            logToConsole(ResponseDto(time = Instant.now(), message = "Running ${command[1]}"))
//                            val pluginPath = File(getDirectory().toString(), "pluginDir/${command[1]}.kts")
//                            try {
//                                if (pluginPath.exists()) {
//                                    engine.eval(pluginPath.readText(), scriptBindings)
//                                } else {
//                                    logToConsole(ResponseDto(time = Instant.now(), message = "Script not found: ${pluginPath.absolutePath}", level = LogLevel.ERROR))
//                                }
//                            } catch (e: Exception) {
//                                logger.error("Error running script: ${pluginPath}| ${e.message}")
//                                logToConsole(ResponseDto(time = Instant.now(), message = "Error: ${e.message}", level = LogLevel.ERROR))
//                            }
//                        }
                        textInput = ""
                        return
                    }

                    logToConsole(ResponseDto(time = Instant.now(), message = "recognized with $command"))

                    if (command.first() == "check") {
                        val ampPath = getDirectory().toString()
                        val packagesFile = File(ampPath, "packages.json")
                        val fileText: String = packagesFile.readText()
                        println("\uD83D\uDD35\uD83D\uDD35(JSON)[FILE-TEXT] $fileText")
                        val dataC: Map<String, PackageWrite> = Json { ignoreUnknownKeys = true }.decodeFromString(fileText)
                        println("\uD83D\uDD35 ${dataC.mapValues { (_, value) -> value.pkgs }}")
                        println("[\uD83D\uDD35] $dataC")
                        println("[PICKED-BY ID]: ${dataC[deviceId]}")
                        println("\uD83D\uDD35\uD83D\uDD35(JSON)[DATA-FILE] $dataC")
                        println("\uD83D\uDD35\uD83D\uDD35(JSON)[DATA-SELECT] ${dataC[deviceId]}")

                        scope.launch {
                            try {
                                val response = client.rest<GithubItemMetadata>(
                                    "list-github-metadata",
                                    restType = RestType.GET
                                )
                                response?.let { msg ->
                                    val urls: List<String> = msg.values.mapNotNull { it?.downloadUrl?.substringBefore("?token=") }

                                    val resComp = (dataC[deviceId]?.pkgs ?: emptyList()).map {
                                        pkg ->
                                            println("(!!) ${pkg} | ${urls.contains(pkg)} ")
                                            val index = urls.indexOf(pkg)
                                            val isComp = msg[index.toString()]?.endComp
                                            println("[IS-COMP]: $isComp")
                                            isComp
                                    }

                                    val templateComp: ResponseDto = if (resComp.any { it?.contains("false") == true }) {
                                        ResponseDto(time = Instant.now(), message = "\\u001b[31m ${resComp} \\u001b[0m")
                                    } else {
                                        ResponseDto(time = Instant.now(), message = "\\u001b[32m Compiling... \\u001b[0m", color = 0xFF00FF00)
                                    }


                                    logToConsole(ResponseDto(time = Instant.now(), message = Json.encodeToString(templateComp), level = LogLevel.DATA))
                                }
                            } catch (e: Exception) {
                                logger.error(e.toString())
                                logToConsole(ResponseDto(time = Instant.now(), message = "check failed: ${e.message}", level = LogLevel.ERROR))
                            }
                        }
                        textInput = ""
                        return
                    }





                    scope.launch {
                        try {
                            // Any other input is a task for the backend: republish the device compatibility endpoints
                            // (POST /post-endpoints) and show what GET /fetch-endpoints lists afterwards (name -> description)
                            val newTask = TerminalTask(task = rawInput, time = Instant.now(), status = 0)
                            messageOver = newTask
                            val response = client.rest<Any>("post-endpoints", restType = RestType.POST, body = newTask)
                            response?.let {
                                logToConsole(ResponseDto(
                                    time = Instant.now(),
                                    status = it["status"]?.toString()?.toIntOrNull() ?: 0,
                                    level = LogLevel.DATA
                                ))
                            }
                            val endpoints = client.rest<Any>("fetch-endpoints", restType = RestType.GET).orEmpty()
                            logToConsole(ResponseDto(
                                time = Instant.now(),
                                message = if (endpoints.isEmpty()) "no compatible endpoints listed by the backend"
                                else endpoints.entries.joinToString("\n", prefix = "${endpoints.size} compatible endpoints:\n") { (name, description) -> "$name -> ${description ?: ""}" },
                                level = LogLevel.DATA
                            ))
                        } catch (e: Exception) {
                            logger.error(e.toString())
                            logToConsole(ResponseDto(time = Instant.now(), message = "Request failed: ${e.message}", level = LogLevel.ERROR))
                        }
                    }
                    textInput = ""
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        shape = RoundedCornerShape(10.dp),
                        value = textInput,
                        onValueChange = { textInput = it },
                        textStyle = TextStyle(
                            color = Color(0xFFc7cbd4),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        placeholder = {
                            Text("Enter command...", color = Color(0xFF878e9c), fontSize = 14.sp)
                        },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFc7cbd4),
                            unfocusedTextColor = Color(0xFFc7cbd4),
                            focusedContainerColor = inputColor,
                            unfocusedContainerColor = inputColor,
                            focusedBorderColor = Color(0xFF5A5A5A),
                            unfocusedBorderColor = inputBorderColor,
                            cursorColor = Color(0xFFc7cbd4)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendCommand() }),

                        trailingIcon = {
                            if (textInput.isNotEmpty()) {
                                IconButton(onClick = { sendCommand() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send command",
                                        tint = Color(0xFFc7cbd4)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
