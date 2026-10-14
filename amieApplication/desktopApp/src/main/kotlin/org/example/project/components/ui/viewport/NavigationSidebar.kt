package org.example.project.components.ui.viewport

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@Composable
fun NavigationSidebar(
    selectedIndex: Int = 1,
    onIndexSelected: (Int) -> Unit,
    showPlay: Boolean = true,
    showPlayCallback: () -> Unit = {},
    // True while the Run button's script is executing: the play icon becomes a progress ring
    running: Boolean = false,
    // 0..1 of the model being built, null for an indeterminate ring
    runProgress: Float? = null,
    showTools: Boolean = false,
    // Non-null shows the Models button; true while the models page is open
    modelsSelected: Boolean? = null,
    onModels: () -> Unit = {},
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
                contentDescription = "Packages",
                selected = selectedIndex == 1,
                onClick = {
                    onIndexSelected(1)
                    println("clicked 1 | index: 1")
                }
            )

            if (modelsSelected != null) {
                SidebarItem(
                    painter = painterResource(Res.drawable.libs_icon),
                    contentDescription = "Models",
                    selected = modelsSelected,
                    onClick = onModels
                )
            }

            if (showTools) {
                SidebarItem(
                    painter = painterResource(Res.drawable.libs_icon),
                    contentDescription = "Agent",
                    selected = selectedIndex == 0,
                    onClick = {
                        onIndexSelected(0)
                        println("clicked 0 | index: 0")
                    }
                )
            }

            if (showPlay) {
                Spacer(modifier = Modifier.weight(1f))
                if (running) {
                    RunningItem(progress = runProgress, onClick = showPlayCallback)
                } else {
                    SidebarItem(
                        painter = rememberVectorPainter(Icons.Default.PlayArrow),
                        contentDescription = "Run",
                        selected = selectedIndex == 2,
                        accent = SuccessColor,
                        idleTint = SuccessColor.copy(alpha = 0.8f),
                        onClick = {
                            onIndexSelected(2)
                            println("clicked 2 | index: 2")
                            showPlayCallback()
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(UnifiedBorderColor))
    }
}

@Composable
private fun SidebarItem(
    painter: Painter,
    contentDescription: String,
    selected: Boolean,
    accent: Color = FontColor,
    idleTint: Color = MutedFontColor,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        // Active indicator pill on the leading edge
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(accent)
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
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

/** The Run button while its script runs: a ring that spins, or fills with the model build's progress */
@Composable
private fun RunningItem(progress: Float?, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                .background(SuccessColor)
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isHovered) SurfaceHover else SuccessColor.copy(alpha = 0.16f))
                .hoverable(interactionSource)
                .clickable(interactionSource = interactionSource, indication = null) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (progress != null) {
                CircularProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.size(22.dp),
                    color = SuccessColor,
                    strokeWidth = 2.5.dp,
                    strokeCap = StrokeCap.Round,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = SuccessColor,
                    strokeWidth = 2.5.dp,
                    strokeCap = StrokeCap.Round,
                )
            }
            // A small stop square so the ring reads as "running" and not as loading
            Box(modifier = Modifier.size(7.dp).clip(RoundedCornerShape(1.5.dp)).background(SuccessColor))
        }
    }
}
