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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(UnifiedBoxColor.toArgb()))
            .border(
                width = 1.dp,
                color = Color(UnifiedBorderColor.toArgb()),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = name.uppercase(),
                    color = Color(FontColor.toArgb()),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier.width(1.dp).height(30.dp)
                        .background(Color(UnifiedBorderColor.toArgb()))
                )

                BadgeLabel(text = deviceEdnpoint.toString())

                BadgeLabel(text = "COM$endPort")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { onConnectPage() }) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Connect",
                        tint = Color(FontColor.toArgb())
                    )
                }

                IconButton(onClick = { onManage() }) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Manage",
                        tint = Color(FontColor.toArgb())
                    )
                }

                IconButton(onClick = { onDisconnect() }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Remove",
                        tint = Color(FontColor.toArgb())
                    )
                }

                IconButton(onClick = { onConfigure() }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure",
                        tint = Color(FontColor.toArgb())
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeLabel(text: String) {
    Surface(
        color = Color(UnifiedBoxColor.toArgb()),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(UnifiedBorderColor.toArgb()))
    ) {
        Text(
            text = text,
            color = Color(FontColor.toArgb()),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontFamily = FontFamily.Monospace
        )
    }
}

