package org.example.project.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.getDirectory
import java.io.File

/** Everything the Settings page and the sandbox's own switches can change */
@Serializable
data class Settings(
    // How large the whole UI is drawn, 1.0 being the system's own size
    val uiScale: Float = 1f,
    // The sandbox's navigation sidebar, hidden or shown from the strip over the editor (the console is not kept:
    // it starts hidden and opens by itself at the first output)
    val sandboxSidebar: Boolean = true,
)

/**
 * The app's settings, kept in `AMP/settings.json` and read once at start. Changing a value applies at once
 * (the window reads [current]) and writes the file, so it holds on the next start.
 */
object AppSettings {
    const val MIN_SCALE = 0.7f
    const val MAX_SCALE = 2f
    val presets = listOf(0.8f, 0.9f, 1f, 1.1f, 1.25f, 1.5f)

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val file: File get() = File(getDirectory().toString(), "settings.json")

    private val _current = MutableStateFlow(load())
    val current: StateFlow<Settings> = _current.asStateFlow()

    private fun load(): Settings = runCatching { json.decodeFromString<Settings>(file.readText()) }.getOrDefault(Settings())
        .let { it.copy(uiScale = it.uiScale.coerceIn(MIN_SCALE, MAX_SCALE)) }

    fun setUiScale(scale: Float) = update { it.copy(uiScale = scale.coerceIn(MIN_SCALE, MAX_SCALE)) }
    fun setSandboxSidebar(shown: Boolean) = update { it.copy(sandboxSidebar = shown) }

    private fun update(change: (Settings) -> Settings) {
        val next = change(_current.value)
        _current.value = next
        runCatching { file.parentFile?.mkdirs(); file.writeText(json.encodeToString(next)) }
    }
}
