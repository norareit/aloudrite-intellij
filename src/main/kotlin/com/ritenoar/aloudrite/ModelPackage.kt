package com.ritenoar.aloudrite

class ModelPackage(
    val address: String,
    val sizeInBytes: Long,
    val unpackedSizeInBytes: Long,
    val sha256: String,
)

val KOKORO_MODEL_PACKAGE = ModelPackage(
    address = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$MODEL_PACKAGE_NAME.tar.bz2",
    sizeInBytes = 132_303_094L,
    unpackedSizeInBytes = 189_882_497L,
    sha256 = "4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e",
)
