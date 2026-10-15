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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.collectAsState
import org.example.project.settings.AppSettings
import androidx.compose.ui.window.WindowScope
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
    // The window is a plain rectangle, like everything drawn inside it
    val windowShape = RectangleShape

    Window(
        onCloseRequest = ::exitApplication,
        title = "amieMultiplatform",
        undecorated = true,
        transparent = true,
        state = windowState
    ) {
        window.minimumSize = Dimension(960, 640)
        // A backend.json that still points at Keycloak's port is corrected before the first backend call
        androidx.compose.runtime.LaunchedEffect(Unit) { org.example.project.backend.BackendConfig.resolve() }
        // The window is undecorated, so the system's own maximize is not there for it. On macOS the header button
        // asks for native full screen (its own Space, menu bar and dock away; Esc or the button leaves it).
        // Elsewhere, or when that is refused, the window is sized by hand to the screen it is on minus the menu
        // bar and the dock, and brought back to the bounds it had
        var maximized by remember { mutableStateOf(false) }
        var floatingBounds by remember { mutableStateOf<java.awt.Rectangle?>(null) }
        androidx.compose.runtime.LaunchedEffect(Unit) { MacFullScreen.listen(window) { maximized = it } }
        fun toggleMaximize() {
            val frame = window
            if (MacFullScreen.toggle(frame)) return
            if (!maximized) {
                floatingBounds = frame.bounds
                val gc = frame.graphicsConfiguration
                val screenBounds = gc.bounds
                val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
                frame.setBounds(
                    screenBounds.x + insets.left,
                    screenBounds.y + insets.top,
                    screenBounds.width - insets.left - insets.right,
                    screenBounds.height - insets.top - insets.bottom,
                )
                maximized = true
            } else {
                floatingBounds?.let { frame.bounds = it }
                maximized = false
            }
        }
        // Each page's WindowHeader is the top bar; it gets dragging and the window controls from here
        val chrome = AppChrome(
            dragArea = { content -> WindowDraggableArea { content() } },
            onMinimize = { windowState.isMinimized = true },
            onToggleMaximize = { toggleMaximize() },
            maximized = { maximized },
            onClose = ::exitApplication,
        )
        // The UI scale from Settings multiplies the system density, so everything inside draws larger or smaller
        val settings by AppSettings.current.collectAsState()
        val systemDensity = LocalDensity.current
        CompositionLocalProvider(LocalAppChrome provides chrome) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(windowShape)
                    .background(ChromeBackground)
                    .border(1.dp, UnifiedBorderColor, windowShape)
            ) {
                CompositionLocalProvider(LocalDensity provides Density(systemDensity.density * settings.uiScale, systemDensity.fontScale)) {
                    AppBaseTemplate()
                }
                if (!maximized) ResizeEdges()
            }
        }
    }
}

/**
 * The window is undecorated, so the system gives it no resize border; these are its edges and corners instead:
 * thin strips along every side that drag the window's bounds, the cursor saying which way. The minimum size of
 * the window holds. Only while floating; a maximized window has nothing to resize.
 */
@Composable
private fun WindowScope.ResizeEdges() {
    val density = LocalDensity.current.density
    val frame = window
    val edge = 6.dp
    val corner = 14.dp

    @Composable
    fun Handle(modifier: Modifier, cursor: Int, left: Boolean = false, right: Boolean = false, top: Boolean = false, bottom: Boolean = false) {
        Box(
            modifier
                .pointerHoverIcon(PointerIcon(java.awt.Cursor(cursor)))
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        // Pointer deltas are in pixels, the frame's bounds in screen points
                        val dx = (drag.x / density).toInt()
                        val dy = (drag.y / density).toInt()
                        val b = frame.bounds
                        val min = frame.minimumSize
                        var x = b.x; var y = b.y; var w = b.width; var h = b.height
                        if (right) w = (w + dx).coerceAtLeast(min.width)
                        if (bottom) h = (h + dy).coerceAtLeast(min.height)
                        if (left) { val nw = (w - dx).coerceAtLeast(min.width); x += w - nw; w = nw }
                        if (top) { val nh = (h - dy).coerceAtLeast(min.height); y += h - nh; h = nh }
                        frame.setBounds(x, y, w, h)
                    }
                }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Handle(Modifier.align(Alignment.CenterStart).fillMaxHeight().width(edge), java.awt.Cursor.W_RESIZE_CURSOR, left = true)
        Handle(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(edge), java.awt.Cursor.E_RESIZE_CURSOR, right = true)
        Handle(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(edge), java.awt.Cursor.N_RESIZE_CURSOR, top = true)
        Handle(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(edge), java.awt.Cursor.S_RESIZE_CURSOR, bottom = true)
        Handle(Modifier.align(Alignment.TopStart).size(corner), java.awt.Cursor.NW_RESIZE_CURSOR, left = true, top = true)
        Handle(Modifier.align(Alignment.TopEnd).size(corner), java.awt.Cursor.NE_RESIZE_CURSOR, right = true, top = true)
        Handle(Modifier.align(Alignment.BottomStart).size(corner), java.awt.Cursor.SW_RESIZE_CURSOR, left = true, bottom = true)
        Handle(Modifier.align(Alignment.BottomEnd).size(corner), java.awt.Cursor.SE_RESIZE_CURSOR, right = true, bottom = true)
    }
}
