package org.example.project.components.ui.viewport

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.AccentButtonHover
import org.example.project.ui.theme.ChipHoverBackground
import org.example.project.ui.theme.DangerButton
import org.example.project.ui.theme.DangerButtonHover
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.LogMessageColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.PageAccent
import org.example.project.ui.theme.TabActiveBackground

/*
 * The app's buttons, the same on every page, so the eye finds the one that matters: ONE filled dark-blue
 * AccentButton per place for the action that creates, builds, runs, validates, opens or fetches; a dark-red
 * DangerButton for what ends or removes something; bare text (SmallButton) for everything else, with a faint
 * square under the pointer; Toggle for what is on or off; IconChip for a small square with only an icon. No
 * borders, no rounded corners. Every one takes a tip, the sentence its tooltip shows: what it does, or, while it
 * is disabled, what is missing; with [arrow] a button ends in a small triangle because it opens a menu.
 */

@Composable
internal fun AccentButton(label: String, enabled: Boolean = true, icon: ImageVector? = null, tip: String? = null, arrow: Boolean = false, fill: Color = PageAccent, fillHover: Color = AccentButtonHover, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colour = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
    AppTooltip(tip) { Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(when { !enabled -> fill.copy(alpha = 0.35f); hovered -> fillHover; else -> fill }, RectangleShape)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) { onClick() }
            .padding(start = 8.dp, end = if (arrow) 4.dp else 8.dp, top = 3.dp, bottom = 3.dp)
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, tint = colour, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(5.dp)) }
        Text(label, color = colour, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (arrow) Icon(Icons.Default.ArrowDropDown, contentDescription = "menu", tint = colour, modifier = Modifier.size(14.dp))
    } }
}

@Composable
internal fun DangerButton(label: String, enabled: Boolean = true, icon: ImageVector? = null, tip: String? = null, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colour = if (enabled) FontColor else MutedFontColor.copy(alpha = 0.5f)
    AppTooltip(tip) { Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(when { !enabled -> DangerButton.copy(alpha = 0.4f); hovered -> DangerButtonHover; else -> DangerButton }, RectangleShape)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, tint = colour, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(5.dp)) }
        Text(label, color = colour, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    } }
}

@Composable
internal fun SmallButton(label: String, enabled: Boolean = true, icon: ImageVector? = null, tip: String? = null, arrow: Boolean = false, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colour = if (!enabled) MutedFontColor.copy(alpha = 0.5f) else if (hovered) FontColor else LogMessageColor
    AppTooltip(tip) { Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(if (enabled && hovered) ChipHoverBackground else Color.Transparent, RectangleShape)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) { onClick() }
            .padding(start = 7.dp, end = if (arrow) 3.dp else 7.dp, top = 3.dp, bottom = 3.dp)
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, tint = colour, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(5.dp)) }
        Text(label, color = colour, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        if (arrow) Icon(Icons.Default.ArrowDropDown, contentDescription = "menu", tint = colour, modifier = Modifier.size(14.dp))
    } }
}

/** A square chip that is on (the tab strip's active gray, white text) or off (bare, gray text); the tip says what it stands for */
@Composable
internal fun Toggle(label: String, on: Boolean, tip: String? = null, onChange: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    AppTooltip(tip) {
        Text(
            label,
            color = if (on || hovered) FontColor else MutedFontColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            modifier = Modifier
                .background(when { on -> TabActiveBackground; hovered -> ChipHoverBackground; else -> Color.Transparent }, RectangleShape)
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(interactionSource = interactionSource, indication = null) { onChange(!on) }
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** A 20dp square with an icon and no label, for a setting that is on or off: kept lit while [active] */
@Composable
internal fun IconChip(icon: ImageVector, tip: String, active: Boolean = false, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    AppTooltip(tip) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (active || hovered) ChipHoverBackground else Color.Transparent, RectangleShape)
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(interactionSource = interactionSource, indication = null) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = tip, tint = if (active || hovered) FontColor else MutedFontColor, modifier = Modifier.size(14.dp))
        }
    }
}
