package org.example.project.components.system.config.handler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.UnifiedBorderColor
import org.example.project.ui.theme.UnifiedBoxColor
import org.example.project.util.fetchFilesList

@Serializable
data class GithubPackages (
    val name: String?,
    val downloadUrl: String?,
    val id: String?,
    val type: String?
)
@Composable
fun RemotePackageConsole(name: String, modifier: Modifier = Modifier, customText: String = "Connect") {
    var buffer by remember { mutableStateOf("") }
    var packages by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        packages = fetchFilesList()
        isLoading = false
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Search for package:",
            color = Color(FontColor.toArgb()),
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.height(8.dp))

        var searchResult by remember { mutableStateOf<Boolean?>(null) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = buffer,
                onValueChange = { buffer = it },
                label = { Text("Dependency Name", color = Color(0xFF878e9c)) },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = Color(0xFFc7cbd4)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(FontColor.toArgb()),
                    unfocusedTextColor = Color(FontColor.toArgb()),
                    focusedContainerColor = Color(UnifiedBoxColor.toArgb()),
                    unfocusedContainerColor = Color(UnifiedBoxColor.toArgb()),
                    cursorColor = Color(0xFFc7cbd4),
                    focusedBorderColor = Color(FontColor.toArgb()),
                    unfocusedBorderColor = Color(UnifiedBorderColor.toArgb())
                )
            )

            IconButton(
                onClick = { searchResult = buffer in packages },
                modifier = Modifier
                    .width(48.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search packages",
                    tint = Color(0xFFc7cbd4)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        when (searchResult) {
            true -> Text(text = "Adding package", color = Color(0xFFc7cbd4), fontSize = 13.sp)
            false -> Text(text = "Package not found", color = Color(0xFF878e9c), fontSize = 13.sp)
            null -> {}
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Text(
                "Loading packages from server...",
                color = Color(0xFF878e9c),
                fontSize = 13.sp
            )
        } else {
            if (packages.isNotEmpty()) {
                val serialized = Json.decodeFromString<List<GithubPackages>>(packages)
                val pkgs = mutableListOf<String>().apply{ this.addAll(serialized.filter{it.type != "dir"}.map{ it -> it.name ?: "" } )}
                for (pkg in pkgs) {
                    ActionCard(title = pkg, modifier = Modifier)
                }
            }
            Text(
                text = "---",
                color = Color(0xFFc7cbd4),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
@Composable
fun ActionCard(title: String, modifier: Modifier){
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(onClick = {}) {
                Text(text = "Click me")
            }
        }
    }
}
