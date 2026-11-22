package com.ritenoar.aloudrite.core

data class ModelPackage(
    val address: String,
    val sizeInBytes: Long,
    val unpackedSizeInBytes: Long,
    val sha256: String,
)

