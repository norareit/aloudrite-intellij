package com.ritenoar.aloudrite.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Paths

class SpeechModelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val int8 = SPEECH_MODELS[0]
    private val fp32 = SPEECH_MODELS[1]

    @Test
    fun theListHoldsTheInt8AndTheFp32ModelInThatOrder() {
        assertEquals(listOf(int8, fp32), SPEECH_MODELS)
        assertEquals(listOf("Kokoro 1.0, int8", "Kokoro 1.0, fp32"), SPEECH_MODELS.map { it.displayName })
        assertEquals(listOf("kokoro-int8-multi-lang-v1_0", "kokoro-multi-lang-v1_0"), SPEECH_MODELS.map { it.packageName })
        assertEquals(listOf("model.int8.onnx", "model.onnx"), SPEECH_MODELS.map { it.modelFileName })
    }

    @Test
    fun theSizesAreTheSumOfTheSizesOfTheFilesOfTheList() {
        assertEquals(189_881_734L, int8.modelPackage.sizeInBytes)
        assertEquals(189_881_734L, int8.modelPackage.unpackedSizeInBytes)
        assertEquals(401_239_297L, fp32.modelPackage.sizeInBytes)
        assertEquals(401_239_297L, fp32.modelPackage.unpackedSizeInBytes)
    }

    @Test
    fun everyAddressIsHttps() {
        SPEECH_MODELS.forEach { assertTrue(it.modelPackage.address.startsWith("https://")) }
    }

    @Test
    fun theFoldersOfTheModelsLieSideBySideInTheFolderOfTheModels() {
        val modelsFolder = userModelsFolder("Linux", "/home/someone", null)

        assertEquals(Paths.get("/home/someone/.local/share/aloudrite/kokoro-int8-multi-lang-v1_0"), int8.folderIn(modelsFolder))
        assertEquals(Paths.get("/home/someone/.local/share/aloudrite/kokoro-multi-lang-v1_0"), fp32.folderIn(modelsFolder))
    }

    @Test
    fun aModelNeedsItsOwnModelFileAndNotTheOthers() {
        val folder = temporaryFolder.newFolder().toPath()
        fillWithModelStandIn(folder, int8)

        assertTrue(int8.isInPlace(folder))
        assertFalse(fp32.isInPlace(folder))

        val other = temporaryFolder.newFolder().toPath()
        fillWithModelStandIn(other, fp32)

        assertTrue(fp32.isInPlace(other))
        assertFalse(int8.isInPlace(other))
    }

    @Test
    fun theDefaultIsTheFp32ModelOnMacAndOnLinux() {
        assertSame(fp32, defaultSpeechModel("Mac OS X"))
        assertSame(fp32, defaultSpeechModel("Linux"))
    }

    @Test
    fun theDefaultOfAPlatformWithoutSpeechIsTheInt8Model() {
        assertSame(int8, defaultSpeechModel("Windows 11"))
    }
}
