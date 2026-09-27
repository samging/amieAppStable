package org.example.project

import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

actual fun getDirectory(): Path {
    val homeDir = System.getProperty("user.home")
    val downloadsDir = File(homeDir, "Downloads/amieMultiplatform")

    return File(downloadsDir, "AMP").toOkioPath()
}