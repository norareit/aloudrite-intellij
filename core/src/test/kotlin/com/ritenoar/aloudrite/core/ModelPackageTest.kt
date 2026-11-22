package com.ritenoar.aloudrite.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelPackageTest {

    private val commit = "57adb37fc0e8d4bd430cfc1b8f8c2294945b9af2"
    private val sha256 = "0".repeat(64)

    @Test
    fun theKokoroModelsComeFromHuggingFaceOverHttps() {
        assertEquals(
            "https://huggingface.co/csukuangfj/kokoro-int8-multi-lang-v1_0/tree/$commit", INT8_MODEL.modelPackage.address)
        assertTrue(SPEECH_MODELS.all { it.modelPackage.address.startsWith("https://") })
    }

    @Test
    fun theAddressOfAFileEncodesEverySegmentOfItsPath() {
        val files = RepositoryFiles("owner/model", commit, listOf(RepositoryFile("espeak-ng-data/voices/!v/Mr serious", 1, sha256)))

        assertEquals(
            "https://huggingface.co/owner/model/resolve/$commit/espeak-ng-data/voices/%21v/Mr%20serious",
            files.fileAddress(files.files.single()))
    }

    @Test
    fun aPathOfUnreservedCharactersIsLeftAsItIs() {
        assertEquals("espeak-ng-data/en_dict-1.2~x/model.onnx", encodePath("espeak-ng-data/en_dict-1.2~x/model.onnx"))
    }

    @Test
    fun aCharacterOutsideAsciiIsEncodedAsItsUtf8Bytes() {
        assertEquals("caf%C3%A9/%E2%82%AC", encodePath("café/€"))
    }

    @Test
    fun theHostIsTheOneTheTestsHandIn() {
        val files = RepositoryFiles("owner/model", commit, listOf(RepositoryFile("a", 1, sha256)), host = "http://localhost:1")

        assertEquals("http://localhost:1/owner/model/resolve/$commit/a", files.fileAddress(files.files.single()))
    }

    @Test
    fun theSizeToDownloadAndTheSizeOnDiskAreTheSumOfTheSizesOfTheFiles() {
        val files = RepositoryFiles(
            "owner/model", commit, listOf(RepositoryFile("a", 10, sha256), RepositoryFile("b/c", 32, sha256)))

        assertEquals(42L, files.sizeInBytes)
        assertEquals(42L, files.unpackedSizeInBytes)
    }
}
