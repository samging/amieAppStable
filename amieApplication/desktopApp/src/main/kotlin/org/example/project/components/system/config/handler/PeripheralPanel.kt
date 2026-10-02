package org.example.project.components.system.config.handler

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.project.data.remote.parser.DeviceManagerJson
import org.example.project.data.remote.parser.DeviceManager
import org.example.project.data.remote.parser.DeviceManagerCsv

import java.io.File
import org.example.project.getDirectory
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import org.example.project.ui.theme.AddButton
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.UnifiedBorderColor
import org.example.project.ui.theme.UnifiedBoxColor

/**
 * A device discovery and management panel that provides an input field for querying peripheral profiles
 * and an optional trigger action to persist hardware configuration profiles.
 */
@Composable
fun PeripheralPanel(name: String,
                    modifier: Modifier = Modifier,
                    hideButton: Boolean = false,
                    valueOf: String = "",
                    onValueChange: (String) -> Unit = {},
                    customText: String = "Connect",
                    keyQuery: String = "",
                    writeId: String? = null,
                    deviceManager: DeviceManager? = null,
                    username: String? = null) {
    
    val appDir = File(org.example.project.getDirectory().toString())
    if (!appDir.exists()) appDir.mkdirs()
    val configFile = File(appDir, "componentSettings.json")

    var buffer by remember { mutableStateOf(valueOf) }

    LaunchedEffect(valueOf) {
        if (valueOf.isNotEmpty()) {
            buffer = valueOf
        }
    }
    var selectedDevice by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Input for: ${name}",
            color = Color(FontColor.toArgb()),
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = buffer,
                onValueChange = { line ->
                    buffer = line
                    onValueChange(line)
                },
                label = { Text(text = name, color = if (isFocused) Color(FontColor.toArgb()) else Color(0xFF878e9c), fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                interactionSource = interactionSource,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(FontColor.toArgb()),
                    unfocusedTextColor = Color(FontColor.toArgb()),
                    focusedContainerColor = Color(UnifiedBoxColor.toArgb()),
                    unfocusedContainerColor = Color(UnifiedBoxColor.toArgb()),
                    cursorColor = Color(0xFFc7cbd4),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(UnifiedBorderColor.toArgb())
                )
            )

            if (!hideButton) {
                Button(
                    onClick = {
                        if (buffer.isNotEmpty()) {
                            val finalValue = if (keyQuery == "deviceEndpoint" && !username.isNullOrEmpty()) {
                                if (buffer.startsWith(username)) buffer else "${username}${buffer}"
                            } else {
                                buffer
                            }

                            selectedDevice = finalValue
                            errorMessage = ""
                            onValueChange(finalValue)

                            if (deviceManager is DeviceManagerJson) {
                                deviceManager.writeConfig(
                                    writeId.toString(),
                                    listOf(keyQuery),
                                    listOf(finalValue),
                                    configFile = configFile
                                )
                            } else if (deviceManager is DeviceManagerCsv) {
                                deviceManager.writeConfig(
                                    writeId.toString(),
                                    listOf(keyQuery),
                                    listOf(finalValue),
                                    configFile = configFile
                                )
                            }
                        } else {
                            selectedDevice = null
                            errorMessage = "Couldn't find device name"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(AddButton.toArgb()),
                        contentColor = Color(FontColor.toArgb())
                    )
                ) {
                    Text(text = customText, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (selectedDevice != null) {
            Text(
                text = "Connected safely to: $selectedDevice",
                color = Color(0xFFc7cbd4),
                fontSize = 13.sp
            )

        } else if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color(FontColor.toArgb()),
                fontSize = 13.sp
            )
        }
    }
}
