package org.example.project.components.ui.viewport

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.example.project.ui.theme.CompletionBackground
import org.example.project.ui.theme.CompletionBorder
import org.example.project.ui.theme.CompletionSelected
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.LogErrorTextColor
import org.example.project.ui.theme.MutedFontColor

/**
 * The app's menus: a square dark box with one line per action, the ones that end or remove something in red.
 * Shared by the Models page (right-click, ⋯, Add, Validate) and the sandbox (Examples).
 */

/** Places a menu under its button with their right edges aligned; above the button when the window ends too soon */
internal object BelowButton : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        var x = anchorBounds.right - popupContentSize.width
        if (x < 0) x = anchorBounds.left.coerceAtMost((windowSize.width - popupContentSize.width).coerceAtLeast(0))
        var y = anchorBounds.bottom + 2
        if (y + popupContentSize.height > windowSize.height) y = (anchorBounds.top - popupContentSize.height - 2).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

/** The actions whose menu line is red: they end, remove or detach something */
private val endingActions = listOf("Terminate", "Destroy", "Remove", "Delete", "Stop", "Detach")

/** A line that only names a group of the actions under it; its label starts with this marker */
internal const val MENU_HEADING = "\u0000"

@Composable
internal fun MenuBox(items: List<ContextMenuItem>, close: () -> Unit) {
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = 160.dp)
            .background(CompletionBackground, RectangleShape)
            .border(1.dp, CompletionBorder, RectangleShape)
            .padding(vertical = 3.dp)
    ) {
        items.forEach { item ->
            if (item.label.startsWith(MENU_HEADING)) MenuHeading(item.label.removePrefix(MENU_HEADING))
            else MenuLine(item.label, ending = endingActions.any { item.label.startsWith(it) }) { close(); item.onClick() }
        }
    }
}

@Composable
private fun MenuHeading(label: String) {
    Text(
        label.uppercase(),
        color = MutedFontColor,
        fontSize = 10.sp,
        letterSpacing = 0.6.sp,
        fontFamily = FontFamily.SansSerif,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 7.dp, bottom = 2.dp),
    )
}

@Composable
private fun MenuLine(label: String, ending: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Text(
        label,
        color = if (ending) LogErrorTextColor else FontColor,
        fontSize = 12.sp,
        fontFamily = FontFamily.SansSerif,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (hovered) CompletionSelected else Color.Transparent)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

/** A button that opens a short menu under itself instead of doing one thing: Validate, Add, Examples */
@Composable
internal fun DropButton(label: String, icon: ImageVector? = null, tip: String? = null, enabled: Boolean = true, accent: Boolean = true, items: List<ContextMenuItem>) {
    var open by remember { mutableStateOf(false) }
    Box {
        if (accent) AccentButton(label, enabled = enabled, icon = icon, tip = if (open) null else tip, arrow = true) { open = !open }
        else SmallButton(label, enabled = enabled, icon = icon, tip = if (open) null else tip, arrow = true) { open = !open }
        if (open) Popup(popupPositionProvider = BelowButton, onDismissRequest = { open = false }, properties = PopupProperties(focusable = true)) {
            MenuBox(items) { open = false }
        }
    }
}
