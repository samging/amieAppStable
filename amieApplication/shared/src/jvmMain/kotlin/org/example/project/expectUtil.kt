package org.example.project

import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * The AMP folder, where everything the app writes lives. A Gradle run keeps it at `AMP/` in the working directory
 * (`amieApplication/desktopApp/AMP/`). A packaged app has no useful working directory, so it keeps AMP in the
 * user's application-data folder (`~/Library/Application Support/amie pilot/AMP` on macOS, `%APPDATA%\amie pilot\AMP`
 * on Windows, `~/.local/share/amie-pilot/AMP` elsewhere), seeded on the first start from the `AMP/` folder
 * shipped in the app's resources. `AMIE_AMP_DIR` names another folder instead.
 */
actual fun getDirectory(): Path = ampDirectory.toOkioPath()

private val ampDirectory: File by lazy {
    val override = System.getenv("AMIE_AMP_DIR")?.takeIf { it.isNotBlank() }?.let(::File)
    val resources = System.getProperty("compose.application.resources.dir")?.let(::File)
    val dir = when {
        override != null -> override
        resources == null -> File(System.getProperty("user.dir"), "AMP")
        else -> {
            val home = System.getProperty("user.home")
            val os = System.getProperty("os.name").lowercase()
            val base = when {
                os.contains("mac") -> File(home, "Library/Application Support/amie pilot")
                os.contains("win") -> File(System.getenv("APPDATA") ?: "$home\\AppData\\Roaming", "amie pilot")
                else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "amie-pilot")
            }
            File(base, "AMP")
        }
    }
    if (!dir.isDirectory) {
        dir.mkdirs()
        // First start of a packaged app: the shipped AMP (backend address, plugin dir, sample data) is copied in once
        val seed = resources?.let { File(it, "AMP") }
        if (seed != null && seed.isDirectory) runCatching { seed.copyRecursively(dir, overwrite = false) }
    }
    dir
}
