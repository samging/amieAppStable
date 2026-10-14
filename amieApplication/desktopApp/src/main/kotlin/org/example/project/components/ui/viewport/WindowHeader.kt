package org.example.project.components.ui.viewport

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.AddButton
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.ChromeBackground
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.UnifiedBorderColor
import org.example.project.ui.theme.UnifiedBoxColor
import org.example.project.ui.theme.DangerColor
import org.example.project.ui.theme.SurfaceHover
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

/**
 * The window's top bar on every page: back button, title, account (Log in or the signed-in user),
 * an optional add-component button and the window controls. Dragging it moves the window.
 */
@Composable
fun WindowHeader(name:String,
                 showUser: Boolean = true,
                 user: String = "",
                 modifier: Modifier = Modifier,
                 showOnBack: Boolean = true,
                 onBack: () -> Unit,
                 addComponent: Boolean = false,
                 addComponentNav: () -> Unit = {}) {
    val chrome = LocalAppChrome.current
    val account = user.ifEmpty { chrome.user }

    chrome.dragArea {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(ChromeBackground)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (showOnBack) {
                    IconButton(
                        onClick = { onBack() },
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Go back",
                            tint = MutedFontColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                } else {
                    Spacer(modifier = Modifier.size(4.dp))
                }

                // Title
                Text(
                    text = name,
                    color = FontColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.2.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Add Component Button
                if (addComponent) {
                    IconButton(
                        onClick = { addComponentNav() },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AddButton)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create component",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                }

                if (showUser) {
                    if (account.isNotEmpty()) {
                        AccountChip(account, onLogout = chrome.onLogout)
                        Spacer(modifier = Modifier.size(10.dp))
                    } else if (chrome.onLogin != null) {
                        LoginButton(onClick = chrome.onLogin)
                        Spacer(modifier = Modifier.size(10.dp))
                    }
                }

                Box(modifier = Modifier.width(1.dp).height(20.dp).background(UnifiedBorderColor))
                WindowControlButton(symbol = "—", onClick = chrome.onMinimize)
                WindowControlButton(symbol = "□", onClick = chrome.onToggleMaximize)
                WindowControlButton(symbol = "✕", hoverColor = DangerColor, onClick = chrome.onClose)
            }

            // Bottom subtle border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(UnifiedBorderColor)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun LoginButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHovered) AddButton else UnifiedBoxColor)
            .border(1.dp, UnifiedBorderColor, RoundedCornerShape(8.dp))
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = FontColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "Log in",
            color = FontColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun AccountChip(user: String, onLogout: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(UnifiedBoxColor)
            .border(1.dp, UnifiedBorderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "User icon",
            tint = MutedFontColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = user,
            color = FontColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(start = 6.dp)
        )
        if (onLogout != null) {
            Text(
                text = "Log out",
                color = MutedFontColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(start = 10.dp).clickable { onLogout() }
            )
        }
    }
}

@Composable
private fun WindowControlButton(
    symbol: String,
    hoverColor: Color = SurfaceHover,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(if (isHovered) hoverColor else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (isHovered) FontColor else MutedFontColor,
            fontSize = 13.sp
        )
    }
}
