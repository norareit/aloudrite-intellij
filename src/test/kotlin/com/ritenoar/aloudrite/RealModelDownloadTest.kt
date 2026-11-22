package com.ritenoar.aloudrite

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.testFramework.ApplicationRule
import com.ritenoar.aloudrite.core.RepositoryFiles
import com.ritenoar.aloudrite.core.SPEECH_MODELS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.ClassRule
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.relativeTo

// Runs only where ALOUDRITE_REAL_DOWNLOAD_FOLDER names a folder. It downloads both speech models from Hugging Face, over
// the network and through the real installer, into the folder `aloudrite` in it, which is also how a machine gets the
// models for the tests that run the real engine: they look in the folder that XDG_DATA_HOME names
class RealModelDownloadTest {

    @Test
    fun bothSpeechModelsAreDownloadedFromHuggingFaceAsExactlyTheFilesOfTheirLists() {
        val folder = System.getenv("ALOUDRITE_REAL_DOWNLOAD_FOLDER")?.let { Paths.get(it) }
        assumeTrue("ALOUDRITE_REAL_DOWNLOAD_FOLDER does not name a folder", folder != null && Files.isDirectory(folder))
        val modelsFolder = folder!!.resolve("aloudrite")

        SPEECH_MODELS.forEach { model ->
            val modelFolder = model.folderIn(modelsFolder)
            val startedAtNanos = System.nanoTime()

            ModelInstaller(model).installClaiming(modelFolder, EmptyProgressIndicator())

            println("Downloading ${model.packageName} took ${(System.nanoTime() - startedAtNanos) / 1_000_000} ms")
            assertTrue(model.isInPlace(modelFolder))
            val expected = (model.modelPackage as RepositoryFiles).files.map { it.path }.sorted()
            val actual = Files.walk(modelFolder).use { paths ->
                paths.filter { Files.isRegularFile(it) }.map { it.relativeTo(modelFolder).toString() }.toList().sorted()
            }
            assertEquals(expected, actual)
        }
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
