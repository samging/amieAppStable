package org.example.project

import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

actual fun getDirectory(): Path {
    return File(System.getProperty("java.io.tmpdir"), ".amieMultiplatform").toOkioPath()
}
