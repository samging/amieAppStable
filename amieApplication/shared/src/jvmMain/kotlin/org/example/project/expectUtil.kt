package org.example.project

import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

actual fun getDirectory(): Path {
    val cwd = System.getProperty("user.dir")
    return File(cwd, "AMP").toOkioPath()
}