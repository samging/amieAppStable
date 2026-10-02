package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.project.components.render.templates.AppBaseTemplate
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import org.example.project.ui.theme.FontColor
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState()

    Window(
        onCloseRequest = ::exitApplication,
        title = "amieMultiplatform",
        undecorated = true,
        transparent = true,
        state = windowState
    ) {
        Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
            WindowDraggableArea {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color.Black),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title text (draggable area covers this)
                    Text(
                        text = "",
                        color = Color(FontColor.toArgb()),
                        modifier = Modifier.padding(start = 16.dp)
                    )

                    // Window control buttons
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minimize Button
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .fillMaxHeight()
                                .clickable { windowState.isMinimized = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("—", color = Color(FontColor.toArgb()))
                        }

                        // Maximize / Restore Button
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .fillMaxHeight()
                                .clickable {
                                    windowState.isMinimized = false
                                    // Toggle maximization state if needed, or check current placement
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("□", color = Color(FontColor.toArgb()))
                        }

                        // Close Button
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .fillMaxHeight()
                                .clickable { exitApplication() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = Color(FontColor.toArgb()))
                        }
                    }
                }
            }

            // Your main app content
            Box(modifier = Modifier.fillMaxSize()) {
                AppBaseTemplate()
            }
        }
    }
}