package org.example.project.util

import java.io.File

fun readLogFile(path: String): List<String> {
    val file = File(path)
    return if (file.exists()) {
        file.readLines()
    } else {
        listOf("Log file not found at $path")
    }
}
