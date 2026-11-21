package com.ritenoar.aloudrite

import java.nio.file.Files
import java.nio.file.Path

// The model of the forced aligner of sherpa-onnx that finds the times of the words of a piece in its audio. Its package name
// is also the name of its folder, beside those of the speech models
data class AlignerModel(val displayName: String, val packageName: String, val modelPackage: ModelPackage) {

    fun folderIn(modelsFolder: Path): Path = modelsFolder.resolve(packageName)

    // Only looks whether the entries exist, as for a speech model, since the native code may end the whole IDE process when
    // it is given a model that is not there
    fun isInPlace(modelFolder: Path): Boolean = ALIGNER_FILE_NAMES.all { Files.isRegularFile(modelFolder.resolve(it)) }
}

const val ALIGNER_LEXICON_FILE_NAME = "lexicon.txt"

private val ALIGNER_FILE_NAMES = listOf("final.alimdl", "lda.mat", "tree", "phones.txt", ALIGNER_LEXICON_FILE_NAME, "meta.json")

val ALIGNER_MODEL = AlignerModel(
    displayName = "Word timing (english-mfa-v3.1.0)",
    packageName = "english-mfa-v3.1.0",
    modelPackage = ModelPackage(
        address = "https://github.com/k2-fsa/sherpa-onnx/releases/download/fa-models/english-mfa-v3.1.0.tar.bz2",
        sizeInBytes = 47_567_475L,
        unpackedSizeInBytes = 51_887_069L,
        sha256 = "86235e50fc9a8d9351c5622756c2cacd5164e3211fcbdf84bedbd00c98e916da",
    ),
)
