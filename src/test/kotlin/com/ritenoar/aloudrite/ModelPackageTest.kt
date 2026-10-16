package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelPackageTest {

    @Test
    fun theKokoroPackageIsTheOneTheReadmeNames() {
        assertEquals(
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2",
            KOKORO_MODEL_PACKAGE.address)
        assertEquals(132_303_094L, KOKORO_MODEL_PACKAGE.sizeInBytes)
        assertEquals(189_882_497L, KOKORO_MODEL_PACKAGE.unpackedSizeInBytes)
        assertEquals("4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e", KOKORO_MODEL_PACKAGE.sha256)
    }

    @Test
    fun theKokoroPackageIsFetchedOverHttps() {
        assertTrue(KOKORO_MODEL_PACKAGE.address.startsWith("https://"))
    }
}
