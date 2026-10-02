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
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.components.render.templates.Client
import org.example.project.components.render.templates.FallbackTypes
import org.example.project.components.render.templates.RestType
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
import org.example.project.ui.theme.UnifiedBodyBackground
import javax.script.ScriptEngineManager


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
data class ResponseDto(
    @Serializable(with = InstantSerializer::class)
    val time: Instant? = null,
    val status: Int? = null,
    val message: String? = null,
    val color: Long? = null,
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

fun logToConsole(dto: ResponseDto) {
    consoleData.add(dto)
}

@Composable
fun TerminalWindow(
    title: String = "",
    deviceId: String = "",
    modifier: Modifier = Modifier,
    logFilePath: String = "logs.txt",
    allowCmd: Boolean? = false
) {
    val deviceManagerJson = remember { DeviceManagerJson() }
    var portName by remember { mutableStateOf<String?>(null) }
    val client = remember { Client() }
    var messageOver by remember { mutableStateOf(TerminalTask()) }
    val loop = rememberCoroutineScope()

    fun logToConsole(dto: ResponseDto) {
        consoleData.add(dto)
        try {
            val file = File(logFilePath)
            val logText = if (dto.message != null) {
                "> ${dto.message}"
            } else {
                "> ${dto.time} ${dto.status}"
            }
            file.appendText("$logText\n")
        } catch (e: Exception) {
            logger.error("Error writing to log file: ${e.message}")
        }
    }

    LaunchedEffect(logFilePath) {
        val file = File(logFilePath)
        if (file.exists()) {
            try {
                val lines = file.readLines()
                consoleData.clear()
                for (line in lines) {
                    if (line.isBlank()) continue
                    consoleData.add(ResponseDto(message = line.removePrefix("> ")))
                }
            } catch (e: Exception) {
                logger.error("Error reading log history: ${e.message}")
            }
        }
    }
    val scope = rememberCoroutineScope()

    suspend fun update(body: TerminalTask ) {
        val repoOwner = "samging"
        val repoName = "codeRepository"
        val path = "repositoryInformations"
        val url = "https://api.github.com/repos/$repoOwner/$repoName/contents/$path"
        val response = client.rest<Any>(url, restType = RestType.POST, body = body)
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
            .clip(RoundedCornerShape(0.dp))
            .border(1.dp, Color.White),
        colors = CardDefaults.cardColors(containerColor = Color(UnifiedBodyBackground.toArgb())),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                for (msg in consoleData) {
                    val displayText = if (msg.message != null) {
                        "> ${msg.message}"
                    } else {
                        "> ${msg.time} ${msg.status}"
                    }
                    Text(
                        text = displayText,
                        color = msg.color?.let { Color(it) } ?: Color(0xFFc7cbd4),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
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
                        logToConsole(ResponseDto(time = Instant.now(), message = message))
                        textInput = ""
                        return
                    }

                    if (command.first() == "cat") {
                        if (args.isEmpty()) {
                            logToConsole(ResponseDto(time = Instant.now(), message = "Usage: cat <filename>"))
                        } else {
                            val ampPath = getDirectory().toString()
                            val file = File(ampPath, args)
                            if (file.exists() && file.isFile) {
                                val contentStr = try {
                                    file.readText()
                                } catch (e: Exception) {
                                    "Error reading file: ${e.message}"
                                }
                                logToConsole(ResponseDto(time = Instant.now(), message = contentStr))
                            } else {
                                logToConsole(ResponseDto(time = Instant.now(), message = "File not found: $args"))
                            }
                        }
                        textInput = ""
                        return
                    }

                    if(command.first() == "run"){
                        val manager = ScriptEngineManager(Thread.currentThread().contextClassLoader)
                        val engine = manager.getEngineByExtension("kts") ?: error("Couldn't find engine")
                        
                        val loggerObj = object : ScriptLogger {
                            override fun log(message: String) {
                                logToConsole(ResponseDto(time = Instant.now(), message = message))
                            }
                            override fun initAmie() {
                                val result = org.example.project.components.terminal.integrations.amiePilot.initAmie()
                                logToConsole(ResponseDto(time = Instant.now(), message = result.joinToString("\n")))
                            }


                            override fun forwardContext(tokens: (matrix: LongArray) -> Unit) {
                                tokens(LongArray(0))
                                return org.example.project.components.terminal.integrations.amiePilot.initContext()
                            }

                        }
                        
                        val scriptBindings = engine.createBindings().apply {
                            put("logger", loggerObj)
                        }

                        loop.launch {
                            logToConsole(ResponseDto(time = Instant.now(), message = "Running ${command[1]}"))
                            val pluginPath = File(getDirectory().toString(), "pluginDir/${command[1]}.kts")
                            try {
                                if (pluginPath.exists()) {
                                    engine.eval(pluginPath.readText(), scriptBindings)
                                } else {
                                    logToConsole(ResponseDto(time = Instant.now(), message = "Script not found: ${pluginPath.absolutePath}"))
                                }
                            } catch (e: Exception) {
                                logger.error("Error running script: ${pluginPath}| ${e.message}")
                                logToConsole(ResponseDto(time = Instant.now(), message = "Error: ${e.message}"))
                            }
                        }
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


                                    logToConsole(ResponseDto(time = Instant.now(), message = Json.encodeToString(templateComp), color = 0xFFFF0000))
                                }
                            } catch (e: Exception) {
                                logger.error(e.toString())
                            }
                        }
                        textInput = ""
                        return
                    }





                    scope.launch {
                        try {
                            val repoOwner = "samging"
                            val repoName = "codeRepository"
                            val path = "repositoryInformations"
                            val url = "https://api.github.com/repos/$repoOwner/$repoName/contents/$path"

                            val newTask = TerminalTask(task = rawInput, time = Instant.now(), status = 0)
                            messageOver = newTask
                            val response = client.rest<Any>(url, restType = RestType.POST, body = newTask)
                            response?.let {
                                logToConsole(ResponseDto(
                                    time = Instant.now(),
                                    status = it["status"]?.toString()?.toIntOrNull() ?: 0
                                ))
                            }
                        } catch (e: Exception) {
                            logger.error(e.toString())
                        }
                    }
                    textInput = ""
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
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
                            focusedContainerColor = Color(0xFF171b23),
                            unfocusedContainerColor = Color(0xFF171b23),
                            focusedBorderColor = Color(0xFF262b36),
                            unfocusedBorderColor = Color(0xFF262b36).copy(alpha = 0.5f),
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
