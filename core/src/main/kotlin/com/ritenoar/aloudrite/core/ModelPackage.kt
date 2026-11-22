package com.ritenoar.aloudrite.core

// What the plugin downloads to put a model in place: an archive that is unpacked, or the files of a repository
sealed interface ModelPackage {
    val address: String
    val sizeInBytes: Long
    val unpackedSizeInBytes: Long
}

data class ArchivePackage(
    override val address: String,
    override val sizeInBytes: Long,
    override val unpackedSizeInBytes: Long,
    val sha256: String,
) : ModelPackage
