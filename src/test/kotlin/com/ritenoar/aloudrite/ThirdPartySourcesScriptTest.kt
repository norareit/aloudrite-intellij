package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

// scripts/prepare-third-party-sources.sh, run on local files through file:// addresses
class ThirdPartySourcesScriptTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val script = File("scripts/prepare-third-party-sources.sh").absolutePath

    @Test
    fun aFileWithTheListedChecksumArrivesAndIsInTheSumsFile() {
        val source = temporaryFolder.newFile("good.tar.gz").apply { writeText("good source") }
        val list = listOf("good-1.0.tar.gz", source.toURI().toString(), sha256Of(source))
        val destination = File(temporaryFolder.root, "out")

        val result = run(list, destination)

        assertEquals(result.output, 0, result.exitCode)
        assertEquals("good source", File(destination, "good-1.0.tar.gz").readText())
        assertEquals("${sha256Of(source)}  good-1.0.tar.gz\n", File(destination, "SHA256SUMS").readText())
    }

    @Test
    fun aFileWithAnotherChecksumMakesItExitWithAnErrorNameTheFileAndLeaveItOut() {
        val source = temporaryFolder.newFile("bad.tar.gz").apply { writeText("changed source") }
        val wrongChecksum = "0".repeat(64)
        val list = listOf("bad-1.0.tar.gz", source.toURI().toString(), wrongChecksum)
        val destination = File(temporaryFolder.root, "out")

        val result = run(list, destination)

        assertNotEquals(0, result.exitCode)
        assertTrue(result.output, result.output.contains("bad-1.0.tar.gz"))
        assertTrue(result.output, result.output.contains(wrongChecksum))
        assertTrue(result.output, result.output.contains(sha256Of(source)))
        assertFalse(File(destination, "bad-1.0.tar.gz").exists())
    }

    @Test
    fun theFolderIsEmptiedBeforeTheDownloads() {
        val source = temporaryFolder.newFile("good.tar.gz").apply { writeText("good source") }
        val destination = temporaryFolder.newFolder("out").also { File(it, "stale.zip").writeText("old") }

        run(listOf("good-1.0.tar.gz", source.toURI().toString(), sha256Of(source)), destination)

        assertFalse(File(destination, "stale.zip").exists())
    }

    private class Result(val exitCode: Int, val output: String)

    private fun run(row: List<String>, destination: File): Result {
        val list = temporaryFolder.newFile().apply { writeText(row.joinToString("\t") + "\n") }
        val process = ProcessBuilder("bash", script, list.absolutePath, destination.absolutePath)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        return Result(process.waitFor(), output)
    }

    private fun sha256Of(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
}
