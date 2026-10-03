package org.example.project.components.ui.viewport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import org.example.project.ui.theme.AddButton
import org.example.project.ui.theme.FontColor
import org.jetbrains.compose.resources.painterResource
import amiemultiplatform.shared.generated.resources.Res
import amiemultiplatform.shared.generated.resources.libs_icon
import amiemultiplatform.shared.generated.resources.console_icon
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NavigationSidebar(
    selectedIndex: Int = 1,
    onIndexSelected: (Int) -> Unit,
    showPlay: Boolean = true,
    showPlayCallback: () -> Unit = {},
    showTools: Boolean = false,
) {
    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .width(45.dp)
            .background(Color.Black),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavigationRail(
            modifier = Modifier.width(70.dp),
            backgroundColor = Color.Transparent,
            contentColor = Color(FontColor.toArgb()),
            elevation = 0.dp
        ) {
            NavigationRailItem(
                selected = selectedIndex == 1,
                onClick = {
                    onIndexSelected(1)
                    println("clicked 1 | index: 1")
                },
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.console_icon),
                        contentDescription = "Packages"
                    )
                },
                modifier = Modifier
                    .padding(
                        vertical = 4.dp,
                        horizontal = 8.dp
                    ) // Gives it margin around the pill
                    .drawWithContent {
                        drawContent()
                        if (selectedIndex == 1) {
                            val strokeWidthPx = 2.dp.toPx()
                            drawLine(
                                color = Color.White,
                                start = Offset(size.width - strokeWidthPx / 2, 0f),
                                end = Offset(size.width - strokeWidthPx / 2, size.height),
                                strokeWidth = strokeWidthPx
                            )
                        }
                    }
                    .clip(RoundedCornerShape(8.dp)) // Rounded background corners
                    .background(if (selectedIndex == 1) Color(AddButton.toArgb()).copy(alpha = 0.2f) else Color.Transparent),
                selectedContentColor = Color(AddButton.toArgb()),
                unselectedContentColor = Color(FontColor.toArgb()),
                alwaysShowLabel = false
            )

            if (showTools) {
                NavigationRailItem(
                    selected = selectedIndex == 0,
                    onClick = {
                        onIndexSelected(0)
                        println("clicked 0 | index: 0")
                    },
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.libs_icon),
                            contentDescription = "Agent"
                        )
                    },
                    modifier = Modifier
                        .padding(
                            vertical = 4.dp,
                            horizontal = 8.dp
                        )
                        .drawWithContent {
                            drawContent()
                            if (selectedIndex == 0) {
                                val strokeWidthPx = 2.dp.toPx()
                                drawLine(
                                    color = Color.White,
                                    start = Offset(size.width - strokeWidthPx / 2, 0f),
                                    end = Offset(size.width - strokeWidthPx / 2, size.height),
                                    strokeWidth = strokeWidthPx
                                )
                            }
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedIndex == 0) Color(AddButton.toArgb()).copy(alpha = 0.2f) else Color.Transparent),
                    selectedContentColor = Color(AddButton.toArgb()),
                    unselectedContentColor = Color(FontColor.toArgb()),
                    alwaysShowLabel = false
                )
            }
                if (showPlay) {
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        selected = selectedIndex == 2,
                        onClick = {
                            onIndexSelected(2)
                            println("clicked 2 | index: 2")
                            coroutineScope.launch(Dispatchers.IO) {
                                showPlayCallback()
                            }
                                  },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Agent"
                            )
                        },
                        modifier = Modifier
                            .padding(
                                vertical = 4.dp,
                                horizontal = 8.dp
                            )
                            .drawWithContent {
                                drawContent()
                                if (selectedIndex == 2) {
                                    val strokeWidthPx = 2.dp.toPx()
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(size.width - strokeWidthPx / 2, 0f),
                                        end = Offset(size.width - strokeWidthPx / 2, size.height),
                                        strokeWidth = strokeWidthPx
                                    )
                                }
                            }
                            .clip(RoundedCornerShape(8.dp)) // Rounded background corners
                            .background(if (selectedIndex == 2) Color.Green.copy(alpha = 0.2f) else Color.Transparent),
                        selectedContentColor = Color.Green,
                        unselectedContentColor = Color(FontColor.toArgb()),
                        alwaysShowLabel = false
                    )
            }
        }
    }
}
