package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.project.components.render.templates.AppBaseTemplate
import org.example.project.components.ui.viewport.AppChrome
import org.example.project.components.ui.viewport.LocalAppChrome
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition
import java.awt.Dimension
import java.awt.Toolkit
import org.example.project.ui.theme.ChromeBackground
import org.example.project.ui.theme.DangerColor
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.SurfaceHover
import org.example.project.ui.theme.UnifiedBorderColor

fun main() = application {
    // Open at 85% of the screen, centered, but never smaller than the layouts need
    val screen = Toolkit.getDefaultToolkit().screenSize
    val windowState = rememberWindowState(
        size = DpSize(
            (screen.width * 0.85f).coerceAtLeast(1200f).dp,
            (screen.height * 0.85f).coerceAtLeast(780f).dp
        ),
        position = WindowPosition(Alignment.Center)
    )
    val windowShape = RoundedCornerShape(12.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "amieMultiplatform",
        undecorated = true,
        transparent = true,
        state = windowState
    ) {
        window.minimumSize = Dimension(960, 640)
        // Each page's WindowHeader is the top bar; it gets dragging and the window controls from here
        val chrome = AppChrome(
            dragArea = { content -> WindowDraggableArea { content() } },
            onMinimize = { windowState.isMinimized = true },
            onToggleMaximize = {
                windowState.placement =
                    if (windowState.placement == WindowPlacement.Maximized) WindowPlacement.Floating
                    else WindowPlacement.Maximized
            },
            onClose = ::exitApplication,
        )
        CompositionLocalProvider(LocalAppChrome provides chrome) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(windowShape)
                    .background(ChromeBackground)
                    .border(1.dp, UnifiedBorderColor, windowShape)
            ) {
                AppBaseTemplate()
            }
        }
    }
}
