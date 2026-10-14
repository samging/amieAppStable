package org.example.project.components.ui.viewport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What the window and session give every WindowHeader: it is the window's top bar, so it carries
 * the drag area, the window controls and the account button.
 */
data class AppChrome(
    val dragArea: @Composable (content: @Composable () -> Unit) -> Unit = { it() },
    val onMinimize: () -> Unit = {},
    val onToggleMaximize: () -> Unit = {},
    val onClose: () -> Unit = {},
    val user: String = "",
    val onLogin: (() -> Unit)? = null,
    val onLogout: (() -> Unit)? = null,
)

val LocalAppChrome = staticCompositionLocalOf { AppChrome() }
