package org.example.project.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Empty folders go, the root stays; a delete takes the empty parents above it with it */
class AmpFilesTest {
    private fun tree(): File {
        val root = File.createTempFile("amp-files", "").apply { delete(); mkdirs() }
        File(root, "a/b/c").mkdirs()                       // empty all the way down
        File(root, "d/keep.txt").apply { parentFile.mkdirs(); writeText("x") }
        File(root, "d/e").mkdirs()                         // empty beside a file
        return root
    }

    @Test
    fun `prune removes the empty folders bottom up and keeps the root and the ones with files`() {
        val root = tree()
        val removed = AmpFiles.pruneEmptyFolders(root).map { it.relativeTo(root).path }.sorted()
        assertEquals(listOf("a", "a/b", "a/b/c", "d/e"), removed)
        assertTrue(root.isDirectory)
        assertTrue(File(root, "d/keep.txt").isFile)
        assertFalse(File(root, "a").exists())
        root.deleteRecursively()
    }

    @Test
    fun `deleting the last file takes its empty parents with it, up to the root`() {
        val root = tree()
        assertTrue(AmpFiles.delete(root, File(root, "d/keep.txt")))
        assertFalse(File(root, "d").exists())
        assertTrue(root.isDirectory)
        root.deleteRecursively()
    }
}
