package org.example.project.components.ui.viewport

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import org.example.project.data.remote.parser.DeviceManager
import org.example.project.getDirectory
import org.example.project.ui.theme.AddButton
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.UnifiedBorderColor
import org.example.project.ui.theme.UnifiedBoxColor
import java.io.File

/**
 * A scrollable container that renders a structured list of key-value item pairs (IDs and descriptions).
 */
@Composable
fun DataEntryList(name: String,
                  username: String = "",
                  modifier: Modifier,
                  activeFields: Map<Int,String> = mapOf(1 to "this"),
                  currentlyActive: List<Int?> = listOf(-1),
                  onPortsLoaded: (Map<Int, String>) -> Unit = {},
                  onPortSelected: (Int, String) -> Unit = { _, _ -> },
                  deviceManager: DeviceManager? = null,
                  writeId: String? = null,
                  keyQuery: String = "port"){

    val verticalScroller = rememberScrollState()
    var displayPorts by remember(activeFields) { mutableStateOf<Map<Int, String>>(activeFields) }
    var inputBuffer by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val ports = SerialPort.getCommPorts()
        if (ports.isNotEmpty()) {
            val map = ports.withIndex().associate { it.index to "${it.value.systemPortName}: ${it.value.descriptivePortName}" }
            displayPorts = map
            onPortsLoaded(map)
        }
        
        // ... existing port opening logic ...

        val port = ports.firstOrNull() ?: return@LaunchedEffect

        // Configure port parameters
        port.baudRate = 9600
        port.numDataBits = 8
        port.numStopBits = SerialPort.ONE_STOP_BIT
        port.parity = SerialPort.NO_PARITY

        if (port.openPort()) {
            println("Successfully opened ${port.systemPortName}")
            
            port.addDataListener(object : SerialPortDataListener {
                override fun getListeningEvents(): Int = SerialPort.LISTENING_EVENT_DATA_AVAILABLE

                override fun serialEvent(event: SerialPortEvent) {
                    if (event.eventType == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                        val bytesAvailable = port.bytesAvailable()
                        if (bytesAvailable > 0) {
                            val newData = ByteArray(bytesAvailable)
                            val numRead = port.inputStream.read(newData)
                            if (numRead > 0) {
                                print(String(newData, 0, numRead))
                            }
                        }
                    }
                }
            })
            
            delay(20000)
            port.removeDataListener()
            port.closePort()
            println("\nPort closed.")
        } else {
            println("Failed to open port.")
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
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(UnifiedBorderColor.toArgb())
                )
            )

            Button(
                onClick = {
                    if (inputBuffer.isNotEmpty() && deviceManager != null && writeId != null) {
                        val appDir = File(getDirectory().toString())
                        val configFile = File(appDir, "componentSettings.json")
                        
                        deviceManager.writeConfig(
                            writeId,
                            listOf(keyQuery),
                            listOf(inputBuffer),
                            configFile = configFile
                        )
                        
                        // Find if the typed port matches an ID in our list
                        val matchingId = displayPorts.entries.find { 
                            val systemName = it.value.split(":").firstOrNull()?.trim() ?: ""
                            systemName.equals(inputBuffer.trim(), ignoreCase = true)
                        }?.key ?: -1
                        
                        // Trigger a selection to update highlights
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

        for ((id, description) in displayPorts) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val portName = description.split(":").firstOrNull()?.trim() ?: description
                    onPortSelected(id, portName)
                    inputBuffer = portName
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
                        text = description,
                        modifier = Modifier.padding(end = 8.dp),
                        color = itemColor
                    )
                }
            }
        }
    }
}
