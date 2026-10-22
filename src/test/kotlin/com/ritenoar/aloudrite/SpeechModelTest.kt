package com.ritenoar.aloudrite

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
        assertEquals(2, SPEECH_MODELS.size)
        assertEquals(
            SpeechModel(
                "Kokoro 1.0, int8", "kokoro-int8-multi-lang-v1_0", "model.int8.onnx",
                ModelPackage(
                    "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2",
                    132_303_094L, 189_882_497L, "4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e")),
            int8)
        assertEquals(
            SpeechModel(
                "Kokoro 1.0, fp32", "kokoro-multi-lang-v1_0", "model.onnx",
                ModelPackage(
                    "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2",
                    349_906_910L, 401_239_297L, "c5f7e2d2caf082bc1d20fb70334a61d99d20b484500aad32e7cf84c128ea3298")),
            fp32)
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
    fun theDefaultIsTheFp32ModelOnMacAndTheInt8ModelOnLinux() {
        assertSame(fp32, defaultSpeechModel("Mac OS X"))
        assertSame(int8, defaultSpeechModel("Linux"))
    }

    @Test
    fun theDefaultOfAPlatformWithoutSpeechIsTheInt8Model() {
        assertSame(int8, defaultSpeechModel("Windows 11"))
    }
}
