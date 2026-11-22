package com.ritenoar.aloudrite.core

import java.nio.file.Files
import java.nio.file.Path

// The model of the forced aligner of sherpa-onnx that finds the times of the words of a piece in its audio. Its package name
// is also the name of its folder, beside those of the speech models
data class AlignerModel(
    override val displayName: String,
    override val packageName: String,
    override val modelPackage: ArchivePackage,
) : InstallablePackage {
    override val progressName: String get() = "the word timing model"

    override fun isInPlace(modelFolder: Path): Boolean = ALIGNER_FILE_NAMES.all { Files.isRegularFile(modelFolder.resolve(it)) }
}

const val ALIGNER_LEXICON_FILE_NAME = "lexicon.txt"

private val ALIGNER_FILE_NAMES = listOf("final.alimdl", "lda.mat", "tree", "phones.txt", ALIGNER_LEXICON_FILE_NAME, "meta.json")

// Where the archive of the aligner model is: the download builds its package from this, and the check of the sources
// compares Hugging Face with it, so that both read the commit, the size and the checksum from one place. Nothing installs it
val ALIGNER_MODEL_SOURCE = RepositoryFiles(
    repository = "csukuangfj/fa-models",
    commit = "39914ed8514a568e2d24a5e785608e1abb429133",
    files = listOf(
        RepositoryFile(
            path = "english-mfa-v3.1.0.tar.bz2",
            sizeInBytes = 47_567_475L,
            sha256 = "86235e50fc9a8d9351c5622756c2cacd5164e3211fcbdf84bedbd00c98e916da",
        ),
    ),
)

private val ALIGNER_ARCHIVE = ALIGNER_MODEL_SOURCE.files.single()

val ALIGNER_MODEL = AlignerModel(
    displayName = "Word timing (english-mfa-v3.1.0)",
    packageName = "english-mfa-v3.1.0",
    modelPackage = ArchivePackage(
        address = ALIGNER_MODEL_SOURCE.fileAddress(ALIGNER_ARCHIVE),
        sizeInBytes = ALIGNER_ARCHIVE.sizeInBytes,
        unpackedSizeInBytes = 51_887_069L,
        sha256 = ALIGNER_ARCHIVE.sha256,
    ),
)
