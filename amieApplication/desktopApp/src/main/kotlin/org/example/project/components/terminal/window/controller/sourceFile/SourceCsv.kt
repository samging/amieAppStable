package org.example.project.components.terminal.window.controller.sourceFile

import okio.Path.Companion.toPath
import java.io.File

// [+Up] Apache commons csv parser
class SourceCsv(
    private val path: String
) {
    private val toPath = path.toPath()
    private val data = mutableMapOf<Int, List<String>>()

    init {
        if (!toPath.toFile().exists()) {
            throw IllegalArgumentException("File does not exist: $path")
        }
    }

    // read["column name"]
    operator fun get(column: String = ""): Pair<Int, Map<Int, List<String>>> {
        val lines = File(path).readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return Pair(0, emptyMap())

        val headers = lines[0].split(",").map { it.trim() }

        val columnIndex = if (column.isEmpty()) 0 else headers.indexOf(column)
            if (columnIndex < 0) {
                return Pair(lines.size, emptyMap())
            }

        val numberOfLines = lines.size

        lines.drop(1).forEachIndexed {  index, string ->
            val row = string.split(",").map { it.trim() }
            data.put(index,row)
        }



        return Pair(numberOfLines, data)
    }
}