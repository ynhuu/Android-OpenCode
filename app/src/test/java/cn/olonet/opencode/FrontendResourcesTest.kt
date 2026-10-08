package cn.olonet.opencode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FrontendResourcesTest {
    private fun version(root: File, name: String): File = File(root, name).apply {
        mkdirs()
        File(this, "index.html").writeText("frontend:$name")
        File(this, "_assets").mkdirs()
        File(this, "_assets/app.js").writeText("script")
    }

    @Test fun removesAllOldVersionsAndKeepsActiveResourcesAndOtherFiles() {
        val root = Files.createTempDirectory("frontend-cleanup").toFile()
        try {
            val active = version(root, "a".repeat(64))
            val old = version(root, "b".repeat(64))
            val older = version(root, "c".repeat(64))
            val staging = version(root, "staging-download")
            val archive = File(root, "download.zip").apply { writeText("zip") }
            val settings = File(root, "settings.json").apply { writeText("connections and theme") }
            assertTrue(FrontendResources.cleanup(root, active))
            assertFalse(old.exists())
            assertFalse(older.exists())
            assertEquals("frontend:${active.name}", File(active, "index.html").readText())
            assertEquals("script", File(active, "_assets/app.js").readText())
            assertTrue(staging.exists())
            assertTrue(archive.exists())
            assertEquals("connections and theme", settings.readText())
            assertTrue(FrontendResources.cleanup(root, active))
        } finally { root.deleteRecursively() }
    }

    @Test fun preservesOldVersionsWhenNewActiveVersionIsMissing() {
        val root = Files.createTempDirectory("frontend-cleanup").toFile()
        try {
            val old = version(root, "b".repeat(64))
            assertThrows(IllegalArgumentException::class.java) {
                FrontendResources.cleanup(root, File(root, "a".repeat(64)))
            }
            assertTrue(File(old, "index.html").isFile)
        } finally { root.deleteRecursively() }
    }

    @Test fun rejectsActiveVersionOutsideTheResourceDirectory() {
        val root = Files.createTempDirectory("frontend-cleanup").toFile()
        try {
            val resources = File(root, "frontends").apply { mkdirs() }
            val old = version(resources, "b".repeat(64))
            val outside = version(root, "a".repeat(64))
            assertThrows(IllegalArgumentException::class.java) { FrontendResources.cleanup(resources, outside) }
            assertTrue(File(old, "index.html").isFile)
            assertTrue(File(outside, "index.html").isFile)
        } finally { root.deleteRecursively() }
    }
}
