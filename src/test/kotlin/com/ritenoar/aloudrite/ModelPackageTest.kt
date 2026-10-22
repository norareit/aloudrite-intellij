package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelPackageTest {

    @Test
    fun theKokoroPackageIsTheOneTheReadmeNames() {
        assertEquals(
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2",
            INT8_MODEL.modelPackage.address)
        assertEquals(132_303_094L, INT8_MODEL.modelPackage.sizeInBytes)
        assertEquals(189_882_497L, INT8_MODEL.modelPackage.unpackedSizeInBytes)
        assertEquals("4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e", INT8_MODEL.modelPackage.sha256)
    }

    @Test
    fun theKokoroPackageIsFetchedOverHttps() {
        assertTrue(INT8_MODEL.modelPackage.address.startsWith("https://"))
    }
}
