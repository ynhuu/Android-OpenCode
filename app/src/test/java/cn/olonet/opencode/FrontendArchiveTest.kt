package cn.olonet.opencode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FrontendArchiveTest {
    private fun archive(root: File, entries: List<Pair<String, String>>): File {
        val archive = File(root, "frontend.zip")
        ZipOutputStream(archive.outputStream()).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return archive
    }

    @Test fun extractsRuntimeResourcesIncludingUnderscoreDirectory() {
        val root = Files.createTempDirectory("frontend-test").toFile()
        try {
            val target = File(root, "staging").apply { mkdirs() }
            FrontendArchive.extract(archive(root, listOf("index.html" to "frontend", "_assets/app.js" to "script")), target)
            assertEquals("frontend", File(target, "index.html").readText())
            assertEquals("script", File(target, "_assets/app.js").readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun rejectsTraversalAbsoluteAndBackslashPaths() {
        val root = Files.createTempDirectory("frontend-test").toFile()
        try {
            val target = File(root, "staging").apply { mkdirs() }
            listOf("../escape", "_assets/../../escape", "/absolute", "_assets\\escape", "./index.html").forEach { name ->
                assertThrows(IllegalArgumentException::class.java) {
                    FrontendArchive.extract(archive(root, listOf(name to "bad")), target)
                }
            }
            assertFalse(File(root, "escape").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun enforcesFileCountLimit() {
        val root = Files.createTempDirectory("frontend-test").toFile()
        try {
            val target = File(root, "staging").apply { mkdirs() }
            val zip = archive(root, (0..10000).map { "file-$it" to "" })
            assertThrows(IllegalArgumentException::class.java) { FrontendArchive.extract(zip, target) }
        } finally { root.deleteRecursively() }
    }
}
