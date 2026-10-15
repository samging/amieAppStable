package org.example.project.components.ui.viewport

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.CompletionBackground
import org.example.project.ui.theme.CompletionBorder
import org.example.project.ui.theme.FontColor

/**
 * A short explanation that appears under the pointer after it rests on the content for half a second: what a
 * button does, or what a tab holds. The same dark box everywhere, square like the rest of the chrome. With no
 * text the content is shown as it is, so a tip can be optional.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTooltip(text: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (text.isNullOrBlank()) {
        content()
        return
    }
    TooltipArea(
        tooltip = {
            Text(
                text = text,
                color = FontColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 15.sp,
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .background(CompletionBackground, RectangleShape)
                    .border(1.dp, CompletionBorder, RectangleShape)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        },
        modifier = modifier,
        delayMillis = 500,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 18.dp)),
        content = content,
    )
}
