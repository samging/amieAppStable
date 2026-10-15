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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.components.ui.viewport.DangerButton
import org.example.project.components.ui.viewport.SmallButton
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

            Text("FILES", color = FontColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            AmpFileTree(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth().weight(1f))
        }
    }
}

/**
 * The AMP folder as a tree: every folder opens and closes, every file shows its size, and a file or folder can
 * be deleted (asked once, on the row). A folder that is left empty deletes itself: after a delete the empty
 * parents go, and every read of the tree prunes the empty folders, since the app makes what it needs again.
 */
@Composable
private fun AmpFileTree(modifier: Modifier = Modifier) {
    val root = remember { java.io.File(org.example.project.getDirectory().toString()) }
    var version by remember { mutableStateOf(0) }
    var pruned by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    var confirming by remember { mutableStateOf<String?>(null) }
    // Read off the UI thread; the empty folders are pruned on each read
    var tree by remember { mutableStateOf<List<TreeRow>?>(null) }
    LaunchedEffect(version) {
        tree = withContext(Dispatchers.IO) {
            pruned = AmpFiles.pruneEmptyFolders(root)
            readTree(root)
        }
    }
    Column(
        modifier = modifier
            .background(PanelSurface, RectangleShape)
            .border(1.dp, PanelBorder, RectangleShape)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(root.absolutePath, color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "everything the app writes; a folder left empty deletes itself" + (if (pruned.isNotEmpty()) " · pruned ${pruned.joinToString { it.name }}" else ""),
                    color = MutedFontColor, fontSize = 11.sp, lineHeight = 16.sp,
                )
            }
            SmallButton("Refresh", icon = Icons.Default.Refresh, tip = "Read the folder again; empty folders are pruned") { version++ }
            Spacer(Modifier.width(6.dp))
            SmallButton("Reveal", icon = Icons.Default.FolderOpen, tip = "Show the AMP folder in the file browser") {
                runCatching { java.awt.Desktop.getDesktop().browse(root.toURI()) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PanelBorder))
        val rows = tree
        if (rows == null) { Text("reading…", color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)); return@Column }
        val shown = rows.filter { row -> row.parents.all { expanded[it] ?: (it == root.path) } }
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (shown.isEmpty()) Text("empty", color = MutedFontColor, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            shown.forEach { row ->
                val open = expanded[row.file.path] ?: false
                val interactionSource = remember(row.file.path) { MutableInteractionSource() }
                val hovered by interactionSource.collectIsHoveredAsState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (hovered) ChipBackground else Color.Transparent)
                        .hoverable(interactionSource)
                        .clickable(interactionSource = interactionSource, indication = null) { if (row.directory) expanded[row.file.path] = !open }
                        .padding(start = (row.depth * 16).dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = when { !row.directory -> Icons.Default.InsertDriveFile; open -> Icons.Default.KeyboardArrowDown; else -> Icons.Default.KeyboardArrowRight },
                        contentDescription = null,
                        tint = MutedFontColor,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(row.file.name, color = FontColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(10.dp))
                    Text(if (row.directory) "${row.count} item${if (row.count == 1) "" else "s"}" else AmpFiles.size(row.file.length()), color = MutedFontColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.weight(1f))
                    if (confirming == row.file.path) {
                        DangerButton("Delete ${row.file.name}", icon = Icons.Default.Delete, tip = "Gone for good; folders left empty above it go too") {
                            confirming = null
                            AmpFiles.delete(root, row.file)
                            version++
                        }
                        Spacer(Modifier.width(4.dp))
                        SmallButton("Keep", tip = "Change nothing") { confirming = null }
                    } else if (hovered) {
                        SmallButton("Delete", icon = Icons.Default.Delete, tip = if (row.directory) "Delete the folder and everything in it (asks first)" else "Delete the file (asks first)") { confirming = row.file.path }
                    }
                }
            }
        }
    }
}

/** One line of the tree: the file, how deep it is, the folders above it (for folding) and how many things a folder holds */
private class TreeRow(val file: java.io.File, val depth: Int, val parents: List<String>, val directory: Boolean, val count: Int)

private fun readTree(root: java.io.File): List<TreeRow> {
    val out = mutableListOf<TreeRow>()
    fun walk(dir: java.io.File, depth: Int, parents: List<String>) {
        val children = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: return
        children.forEach { child ->
            val count = if (child.isDirectory) child.listFiles()?.size ?: 0 else 0
            out += TreeRow(child, depth, parents, child.isDirectory, count)
            if (child.isDirectory) walk(child, depth + 1, parents + child.path)
        }
    }
    walk(root, 0, listOf(root.path))
    return out
}
