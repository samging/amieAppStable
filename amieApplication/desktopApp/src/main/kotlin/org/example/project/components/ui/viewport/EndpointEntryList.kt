package org.example.project.components.ui.viewport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.components.render.templates.Client
import org.example.project.components.render.templates.RestType
import org.example.project.data.remote.parser.Device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fazecast.jSerialComm.*
import kotlinx.coroutines.delay
import org.example.project.components.render.templates.FallbackTypes
import org.example.project.components.render.templates.FileItem
import org.example.project.data.remote.parser.DeviceManager
import org.example.project.getDirectory
import org.example.project.ui.theme.AddButton
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.UnifiedBorderColor
import org.example.project.ui.theme.UnifiedBoxColor
import org.slf4j.LoggerFactory
import java.io.File

@Serializable
data class DeviceEndpoint (
    val consumerName: String,
    val deviceName: String,
    val mcu: String,
)


@Serializable
data class DeviceEndpointBeta(
    @SerialName("device_retail") val deviceRetail: String,
    @SerialName("device_name") val deviceName: String,
)
/**
 * A scrollable container that renders a structured list of key-value item pairs (IDs and descriptions).
 */
@Composable
fun EndpointEntryList(name: String,
                  username: String = "",
                  modifier: Modifier,
                  activeFields: Map<Int,String> = mapOf(1 to "this"),
                  currentlyActive: List<Int?> = listOf(-1),
                  onPortsLoaded: (Map<Int, String>) -> Unit = {},
                  onPortSelected: (Int, String) -> Unit = { _, _ -> },
                  deviceManager: DeviceManager? = null,
                  writeId: String? = null,
                  keyQuery: String = "deviceEndpoint"){

    val verticalScroller = rememberScrollState()
    var displayPorts by remember(activeFields) { mutableStateOf<Map<Int, String>>(activeFields) }
    var inputBuffer by remember { mutableStateOf("") }
    var fetchedEndpoints by remember { mutableStateOf(false) }

    val client = remember { Client() }
    var endpointJson by remember { mutableStateOf(emptyMap<String, DeviceEndpointBeta>()) }
    val logger = remember { LoggerFactory.getLogger("EndpointEntryList") }

    LaunchedEffect(Unit) {
        if (!fetchedEndpoints) {
            val response = client.rest<Any>("fetch-endpoints", restType = RestType.GET)
            logger.info("[!!!] Received ${response}\n\n\n")
            endpointJson = response?.mapValues { (key, value) ->
                val stringValue = value?.toString() ?: ""
                try {
                    Json.decodeFromString<DeviceEndpointBeta>(stringValue)
                } catch (e: Exception) {
                    logger.error("Serialization error for $key: ${e.message}")
                    DeviceEndpointBeta(deviceRetail = key, deviceName = stringValue)
                }
            } ?: emptyMap()
            fetchedEndpoints = true
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(verticalScroller)
            .padding(24.dp)
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = inputBuffer,
                onValueChange = {
                    inputBuffer = it
                    onPortSelected(-1, it)
                },
                label = { Text("Port entry", color = Color(0xFF878e9c), fontFamily = FontFamily.SansSerif) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(FontColor.toArgb()),
                    unfocusedTextColor = Color(FontColor.toArgb()).copy(alpha = 0.7f),
                    focusedContainerColor = Color(UnifiedBoxColor.toArgb()),
                    unfocusedContainerColor = Color(UnifiedBoxColor.toArgb()),
                    cursorColor = Color(0xFFc7cbd4),
                    focusedBorderColor = Color(UnifiedBorderColor.toArgb()),
                    unfocusedBorderColor = Color(0xFF262b36).copy(alpha = 0.5f)
                )
            )

            Button(
                onClick = {
                    if (inputBuffer.isNotEmpty() && deviceManager != null && writeId != null) {
                        logger.info("Writing config for button click: id=$writeId, key=$keyQuery, value=$inputBuffer")
                        val appDir = File(getDirectory().toString())
                        val configFile = File(appDir, "componentSettings.json")

                        deviceManager.writeConfig(
                            writeId,
                            listOf(keyQuery),
                            listOf(inputBuffer),
                            configFile = configFile
                        )

                        val matchingId = displayPorts.entries.find {
                            val systemName = it.value.split(":").firstOrNull()?.trim() ?: ""
                            systemName.equals(inputBuffer.trim(), ignoreCase = true)
                        }?.key ?: -1

                        onPortSelected(matchingId, inputBuffer)

                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(AddButton.toArgb()),
                    contentColor = Color(FontColor.toArgb())
                )
            ) {
                Text("Set", fontWeight = FontWeight.Medium)
            }
        }

        endpointJson.values.forEachIndexed { id, endpoint ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onPortSelected(id, endpoint.deviceName)
                    inputBuffer = endpoint.deviceName

                    if (deviceManager != null && writeId != null) {
                        logger.info("Writing config for click: id=$writeId, key=$keyQuery, value=${endpoint.deviceName}")
                        val appDir = File(getDirectory().toString())
                        val configFile = File(appDir, "componentSettings.json")
                        deviceManager.writeConfig(
                            writeId,
                            listOf(keyQuery),
                            listOf(endpoint.deviceName),
                            configFile = configFile
                        )
                    }
                }
                .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween)
            {
                Row {
                    val isHighlighted = currentlyActive.any { it == id }
                    val itemColor = when {
                        isHighlighted -> Color.Green
                        id == 0 && currentlyActive.isEmpty() -> Color.Yellow
                        else -> Color(0xFF878e9c)
                    }

                    Text(
                        color = itemColor,
                        text = "[$id] ",
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = endpoint.deviceRetail,
                        modifier = Modifier.padding(end = 8.dp),
                        color = itemColor
                    )
                }
            }
        }
    }
}
