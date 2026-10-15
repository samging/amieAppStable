package org.example.project.components.ui.viewport

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import org.example.project.ui.theme.SidebarBackground
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.SuccessColor
import org.example.project.ui.theme.SurfaceHover
import org.example.project.ui.theme.UnifiedBorderColor
import org.jetbrains.compose.resources.painterResource
import amiemultiplatform.shared.generated.resources.Res
import amiemultiplatform.shared.generated.resources.libs_icon
import amiemultiplatform.shared.generated.resources.console_icon

/**
 * The column of icons at the left of every working page: the sandbox, the models page, Run (while a script runs,
 * a square that fills with the build's progress and stops the run when clicked) and the settings gear. Every
 * icon says what it is when the pointer rests on it. Squares only; the open page is marked with a bar at the edge.
 */
@Composable
fun NavigationSidebar(
    selectedIndex: Int = 1,
    onIndexSelected: (Int) -> Unit,
    showPlay: Boolean = true,
    showPlayCallback: () -> Unit = {},
    // True while the Run button's script is executing: the play icon becomes a running mark
    running: Boolean = false,
    // 0..1 of the model being built, null for an indeterminate mark
    runProgress: Float? = null,
    showTools: Boolean = false,
    // Non-null shows the Models button; true while the models page is open
    modelsSelected: Boolean? = null,
    onModels: () -> Unit = {},
    // Non-null shows the Settings gear at the bottom; true while the settings page is open
    settingsSelected: Boolean? = null,
    onSettings: (() -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(52.dp)
                .background(SidebarBackground)
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SidebarItem(
                painter = painterResource(Res.drawable.console_icon),
                contentDescription = "Sandbox",
                tip = "Sandbox: the script editor, its console and the devices",
                selected = selectedIndex == 1,
                onClick = { onIndexSelected(1) }
            )

            if (modelsSelected != null) {
                SidebarItem(
                    painter = painterResource(Res.drawable.libs_icon),
                    contentDescription = "Models",
                    tip = "Models: intent models, package jars and open sessions",
                    selected = modelsSelected,
                    onClick = onModels
                )
            }

            if (showTools) {
                SidebarItem(
                    painter = painterResource(Res.drawable.libs_icon),
                    contentDescription = "Agent",
                    tip = "Agent",
                    selected = selectedIndex == 0,
                    onClick = { onIndexSelected(0) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            if (showPlay) {
                if (running) {
                    RunningItem(progress = runProgress, onClick = showPlayCallback)
                } else {
                    SidebarItem(
                        painter = rememberVectorPainter(Icons.Default.PlayArrow),
                        contentDescription = "Run",
                        tip = "Run main.kts",
                        selected = selectedIndex == 2,
                        accent = SuccessColor,
                        idleTint = SuccessColor,
                        onClick = {
                            onIndexSelected(2)
                            showPlayCallback()
                        }
                    )
                }
            }
            if (onSettings != null) {
                SidebarItem(
                    painter = rememberVectorPainter(Icons.Default.Settings),
                    contentDescription = "Settings",
                    tip = "Settings: the UI scale",
                    selected = settingsSelected == true,
                    onClick = onSettings
                )
            }
        }

        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(UnifiedBorderColor))
    }
}

@Composable
private fun SidebarItem(
    painter: Painter,
    contentDescription: String,
    tip: String,
    selected: Boolean,
    accent: Color = FontColor,
    idleTint: Color = MutedFontColor,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    AppTooltip(tip) {
        Box(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            // The open page: a bar at the leading edge
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(3.dp)
                        .height(18.dp)
                        .background(accent)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        when {
                            selected -> accent.copy(alpha = 0.16f)
                            isHovered -> SurfaceHover
                            else -> Color.Transparent
                        }
                    )
                    .hoverable(interactionSource)
                    .clickable(interactionSource = interactionSource, indication = null) { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painter,
                    contentDescription = contentDescription,
                    tint = when {
                        selected -> accent
                        isHovered -> FontColor
                        else -> idleTint
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * The Run button while its script runs: a square frame that fills from the bottom with the model build's
 * progress (or breathes when there is no fraction to show), with a stop square in the middle: clicking it stops
 * the run.
 */
@Composable
private fun RunningItem(progress: Float?, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val fill = progress?.coerceIn(0f, 1f) ?: rememberInfiniteTransition().animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
    ).value

    AppTooltip(if (progress != null) "Building a model: ${(progress * 100).toInt()}% done; the console follows it" else "Running main.kts; the console follows it") {
        Box(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(18.dp)
                    .background(SuccessColor)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (isHovered) SurfaceHover else SuccessColor.copy(alpha = 0.16f))
                    .hoverable(interactionSource)
                    .clickable(interactionSource = interactionSource, indication = null) { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(20.dp).border(1.5.dp, SuccessColor, RectangleShape),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(fill).background(SuccessColor.copy(alpha = 0.45f)))
                    // The stop square: the mark reads as "running, click to stop", not as loading
                    Box(modifier = Modifier.align(Alignment.Center).size(7.dp).background(SuccessColor))
                }
            }
        }
    }
}
