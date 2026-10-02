package org.example.project.components.terminal.window.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.UnifiedBodyBackground

@Composable
fun ConfigurationPanel(deviceId: String,
                       modifier: Modifier = Modifier,
                       endPointNumber: Int? = null,
                       allowCmd: Boolean? = null,
                       logFilePath: String = "logs.txt") {

    Column(modifier = modifier.fillMaxSize().background(Color(UnifiedBodyBackground.toArgb())).padding(16.dp)) {
        Text(
            text = "Device Configuration",
            color = Color(FontColor.toArgb()),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF262b36), RoundedCornerShape(8.dp))) {
            TerminalWindow(
                title = if (endPointNumber != null) "Endpoint configuration $endPointNumber" else "",
                deviceId = deviceId,
                modifier = Modifier.fillMaxSize(),
                allowCmd = allowCmd,
                logFilePath = logFilePath
            )
        }
    }
}
