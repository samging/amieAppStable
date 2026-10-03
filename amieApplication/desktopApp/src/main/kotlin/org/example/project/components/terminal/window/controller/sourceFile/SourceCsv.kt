package org.example.project.components.terminal.window.controller.sourceFile

import okio.Path.Companion.toPath
import java.io.File
import java.nio.file.Path

class SourceCsv(
    private val path: String
) {
    private val toPath = path.toPath()
    private val data = mutableListOf<List<String>>()

    init {
        if (!toPath.toFile().exists()) {
            throw IllegalArgumentException("File does not exist: $path")
        }
    }
    //read["column name"]
    operator fun get(column: String = ""): List<String> {
        val lines = File(path).readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val headers = lines[0].split(",")
        val columnIndex = headers.indexOf(column)

        return lines.drop(1).mapNotNull { line ->
            val row = line.split(",").map { it.trim() }
            row.getOrNull(columnIndex)
        }
    }
}