package org.example.project.components.ui.viewport

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.UnifiedBodyBackground
import org.example.project.ui.theme.UnifiedBorderColor
import org.example.project.ui.theme.UnifiedBoxColor
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.text.style.TextOverflow
import org.example.project.ui.theme.AccentSoft
import org.example.project.ui.theme.AddButton
import org.example.project.ui.theme.DangerColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.SurfaceHover

/**
 * A hardware connectivity dashboard utility panel that displays peripheral connection paths and
 * provides an icon-based configuration matrix (Manage, Connect, Configure, Disconnect).
 */
@Composable
fun DevicePanelK(
    name: String,
    modifier: Modifier = Modifier,
    deviceEdnpoint: Any?,
    endPort: Any? = null,
    onManage: () -> Unit,
    onConfigure: () -> Unit,
    onConnectPage: () -> Unit,
    onDisconnect: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val cardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(6.dp)
            .clip(cardShape)
            .background(if (isHovered) SurfaceHover else UnifiedBoxColor)
            .border(
                width = 1.dp,
                color = if (isHovered) AddButton.copy(alpha = 0.5f) else UnifiedBorderColor,
                shape = cardShape
            )
            .hoverable(interactionSource)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = AddButton,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = name,
                        color = FontColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BadgeLabel(text = deviceEdnpoint.toString())
                        BadgeLabel(text = "COM$endPort")
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                CardAction(Icons.Default.Link, "Connect") { onConnectPage() }
                CardAction(Icons.Default.Terminal, "Manage") { onManage() }
                CardAction(Icons.Default.Settings, "Configure") { onConfigure() }
                CardAction(Icons.Default.DeleteSweep, "Remove", hoverTint = DangerColor) { onDisconnect() }
            }
        }
    }
}

@Composable
private fun CardAction(
    icon: ImageVector,
    contentDescription: String,
    hoverTint: Color = FontColor,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp).hoverable(interactionSource)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isHovered) hoverTint else MutedFontColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun BadgeLabel(text: String) {
    Surface(
        color = UnifiedBodyBackground,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, UnifiedBorderColor)
    ) {
        Text(
            text = text,
            color = MutedFontColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontFamily = FontFamily.Monospace
        )
    }
}
