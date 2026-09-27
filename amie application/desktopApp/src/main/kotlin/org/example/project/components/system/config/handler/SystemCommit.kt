package org.example.project.components.system.config.handler

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.data.remote.parser.DeviceManager
import org.example.project.data.remote.parser.DeviceManagerCsv
import org.example.project.data.remote.parser.DeviceManagerJson
import java.io.File
import org.example.project.getDirectory
import org.example.project.ui.theme.AddButton
import org.example.project.ui.theme.FontColor

@Composable
fun SystemCommit(
    modifier: Modifier = Modifier,
    indexDevice: String,
    keyValues: List<String>,
    valuesOf: List<String>,
    deviceManager: DeviceManager,
    redirectOnOk: () -> Unit
) {
    val appDir = File(org.example.project.getDirectory().toString())
    if (!appDir.exists()) appDir.mkdirs()
    val configFile = File(appDir, "componentSettings.json")

    Column(modifier = modifier) {
        Button(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            onClick = {
                deviceManager.writeConfig(
                    id = indexDevice,
                    keys = keyValues,
                    values = valuesOf,
                    configFile = configFile
                )
                redirectOnOk()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(AddButton.toArgb()),
                contentColor = Color(FontColor.toArgb())
            )
        ) {
            Text("Connect", fontWeight = FontWeight.Bold)
        }
    }
}
