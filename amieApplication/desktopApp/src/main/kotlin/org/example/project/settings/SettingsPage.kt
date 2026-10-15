package org.example.project.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.example.project.components.ui.viewport.NavigationSidebar
import org.example.project.ui.theme.ChipBackground
import org.example.project.ui.theme.ChipBorderColor
import org.example.project.ui.theme.ConsoleBorderColor
import org.example.project.ui.theme.EditorBackground
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.PageAccent
import org.example.project.ui.theme.PanelBorder
import org.example.project.ui.theme.PanelSurface
import kotlin.math.roundToInt

/**
 * The settings route: the one setting there is, the UI scale. The presets and the slider apply as they are
 * moved, the whole window redraws at the new size, and the value is kept in AMP/settings.json.
 */
@Composable
fun SettingsPage(navController: NavController) {
    val settings by AppSettings.current.collectAsState()
    Row(modifier = Modifier.fillMaxSize().background(EditorBackground)) {
        NavigationSidebar(
            selectedIndex = -1,
            onIndexSelected = { if (it == 1) navController.navigate("codingSandbox") },
            modelsSelected = false,
            onModels = { navController.navigate("models") },
            settingsSelected = true,
            onSettings = {},
        )
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("SETTINGS", color = FontColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .background(PanelSurface, RectangleShape)
                    .border(1.dp, PanelBorder, RectangleShape)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("UI scale", color = FontColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
                Text("How large everything is drawn. It applies as you move it and is kept for the next start.", color = MutedFontColor, fontSize = 11.sp, lineHeight = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppSettings.presets.forEach { p ->
                        val on = (settings.uiScale * 100).roundToInt() == (p * 100).roundToInt()
                        Text(
                            "${(p * 100).roundToInt()}%",
                            color = if (on) FontColor else MutedFontColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .background(if (on) PageAccent.copy(alpha = 0.35f) else ChipBackground, RectangleShape)
                                .border(1.dp, if (on) PageAccent else ChipBorderColor, RectangleShape)
                                .clickable { AppSettings.setUiScale(p) }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                // The slider holds its own value while dragged, so the window doesn't redraw at every pixel
                var dragging by remember { mutableStateOf<Float?>(null) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = dragging ?: settings.uiScale,
                        onValueChange = { dragging = (it * 20).roundToInt() / 20f },
                        onValueChangeFinished = { dragging?.let { AppSettings.setUiScale(it) }; dragging = null },
                        valueRange = AppSettings.MIN_SCALE..AppSettings.MAX_SCALE,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = FontColor, activeTrackColor = PageAccent, inactiveTrackColor = ConsoleBorderColor),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("${((dragging ?: settings.uiScale) * 100).roundToInt()}%", color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                }
                Spacer(Modifier.height(2.dp))
                Text("stored in AMP/settings.json", color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
