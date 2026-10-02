package org.example.project.components.system.config.handler

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.example.project.components.render.templates.Client
import org.example.project.components.render.templates.FallbackTypes
import org.example.project.components.render.templates.FileItem
import org.example.project.components.render.templates.RestType
import org.example.project.data.remote.parser.DeviceManagerFactory
import org.example.project.data.remote.parser.JSONACTIONS
import org.example.project.getDirectory
import org.example.project.ui.theme.FontColor
import java.io.File
import java.util.UUID

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

@Serializable
data class DevicePlugin(
    val name: String,
    @SerialName("downloadUrl") val url: String,
    @Serializable(with = UUIDSerializer::class) val id: UUID?,
    val type: String,
)

@Composable
fun IndexedSelectionList(
    name: String,
    deviceId: String,
    modifier: Modifier
) {
    var response by remember { mutableStateOf(mapOf<String, FileItem?>()) }
    var doPrefetch by remember { mutableStateOf(true) }
    val client = remember { Client() }
    val logger = remember(name) { org.slf4j.LoggerFactory.getLogger("IndexedSelectionList:$name") }

    // Configured JSON parser to avoid crashes on unexpected server fields
    val jsonParser = remember { Json { ignoreUnknownKeys = true } }

    LaunchedEffect(client, doPrefetch) {
        if (doPrefetch) {
            println("[DEBUG] IndexedSelectionList: Starting prefetch...")
            val result = try {
                client.rest<FileItem>("list-github", restType = RestType.GET, fallbackType = FallbackTypes.FILE_ITEM_LIST)
            } catch (e: Exception) {
                logger.error("Exception in IndexedSelectionList during rest call: ${e.message}")
                println("[DEBUG] IndexedSelectionList: Rest call failed: ${e.message}")
                null
            }
            println("[DEBUG] ALL OF ATTRS: $result")
            if (result == null) {
                println("[DEBUG] IndexedSelectionList: Result is null, handling login error.")
                client.handleLoginError("Failed to get doPrefetch")
            } else {
                response = result.entries.mapNotNull { (key, value) ->
                    value?.let { key to it }
                }.toMap().filterValues { it.type == "file" }
                println("[DEBUG] IndexedSelectionList: Response map updated with ${response.size} items.")
            }
            doPrefetch = false
            println("[DEBUG] IndexedSelectionList: Prefetch completed.")
        }
    }

    //JsonSerializer:
    val packagesFile: File = File(getDirectory().toString(), "packages.json")
    val jsonBridge = remember { DeviceManagerFactory.create(packagesFile) }

    val selectedUrls = remember {
        val initialPackages = jsonBridge.getPackages(deviceId)
        mutableStateListOf<String>().apply { addAll(initialPackages.map { it.substringBefore("?token=") }) }
    }

    val verticalScroller = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(verticalScroller)
            .padding(top = 10.dp)
            .fillMaxSize()
    ) {
        if (doPrefetch && response.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFFc7cbd4)
                )
            }
        } else {
            for ((id, fieldName) in response) {
                val downloadUrl = fieldName?.downloadUrl ?: ""
                val cleanUrl = downloadUrl.substringBefore("?token=")
                val isAdded = selectedUrls.any { it.substringBefore("?token=") == cleanUrl }
                println("${cleanUrl} (S)->(D) ${selectedUrls} ISADDED: ${isAdded}")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF171b23))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = fieldName!!.name,
                            color = Color(FontColor.toArgb()),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Button(
                        modifier = Modifier
                            .height(32.dp)
                            .width(80.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF262b36),
                            contentColor = Color(0xFFc7cbd4)
                        ),
                        onClick = {
                            response[id]?.let {
                                logger.info("Plugin URL (Raw): ${it.downloadUrl}")
                            }

                            if (isAdded) {
                                selectedUrls.removeIf { it.substringBefore("?token=") == cleanUrl }
                                jsonBridge.writePackage(deviceId, mutableListOf(cleanUrl), JSONACTIONS.DELETE)
                            } else {
                                if (!selectedUrls.contains(cleanUrl)) {
                                    selectedUrls.add(cleanUrl)
                                }
                                jsonBridge.writePackage(deviceId, mutableListOf(cleanUrl), JSONACTIONS.WRITE)
                                println("[INFO]: Adding plugin with ID: $deviceId of URL: ${cleanUrl}")
                            }
                        }
                    ) {
                        Text(
                            text = if (isAdded) "DROP" else "ADD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
