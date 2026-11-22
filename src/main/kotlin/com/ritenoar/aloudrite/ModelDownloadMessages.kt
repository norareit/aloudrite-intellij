package com.ritenoar.aloudrite

import com.ritenoar.aloudrite.core.AlignerModel
import com.ritenoar.aloudrite.core.ModelPackage
import com.ritenoar.aloudrite.core.RepositoryFiles
import com.ritenoar.aloudrite.core.SpeechModel
import java.nio.file.Path

private const val BYTES_PER_MEGABYTE = 1_000_000L

fun megabytes(bytes: Long): Long = (bytes + BYTES_PER_MEGABYTE / 2) / BYTES_PER_MEGABYTE

const val DOWNLOAD_RUNNING_ELSEWHERE_MESSAGE =
    "A download of the AloudRite speech model is already running in another IDE window."

const val DOWNLOAD_RUNNING_HERE_MESSAGE = "A download of an AloudRite speech model is already running in this IDE."

// The files of a repository arrive as they are, and an archive is unpacked after it arrives
private fun onceInPlace(modelPackage: ModelPackage): String =
    if (modelPackage is RepositoryFiles) "Once downloaded" else "Once unpacked"

private fun downloadFacts(model: SpeechModel, modelFolder: Path, alignerModel: AlignerModel?): String {
    val modelPackage = model.modelPackage
    if (alignerModel == null) {
        return "The download is ${megabytes(modelPackage.sizeInBytes)} MB, from ${modelPackage.address}. " +
            "${onceInPlace(modelPackage)}, the model takes ${megabytes(modelPackage.unpackedSizeInBytes)} MB in " +
            "${modelFolder.toAbsolutePath()}."
    }
    val alignerPackage = alignerModel.modelPackage
    return "The download is ${megabytes(modelPackage.sizeInBytes + alignerPackage.sizeInBytes)} MB: " +
        "${megabytes(modelPackage.sizeInBytes)} MB from ${modelPackage.address}, and ${megabytes(alignerPackage.sizeInBytes)} MB " +
        "from ${alignerPackage.address} for ${alignerModel.displayName}, which finds the word being read. " +
        "${onceInPlace(modelPackage)}, the model takes ${megabytes(modelPackage.unpackedSizeInBytes)} MB in " +
        "${modelFolder.toAbsolutePath()}, and ${alignerModel.displayName} takes " +
        "${megabytes(alignerPackage.unpackedSizeInBytes)} MB in ${alignerModel.folderIn(modelFolder.toAbsolutePath().parent)}."
}

// The aligner model is given where it is not on the machine, and is then downloaded with the speech model
fun downloadQuestion(model: SpeechModel, modelFolder: Path, alignerModel: AlignerModel? = null): String =
    "AloudRite needs its speech model (${model.displayName}) to speak, and the model is not on this machine yet. " +
        "Download it now?\n\n${downloadFacts(model, modelFolder, alignerModel)}"

fun downloadQuestionFromThePage(model: SpeechModel, modelFolder: Path, alignerModel: AlignerModel? = null): String =
    "Download ${model.displayName}?\n\n${downloadFacts(model, modelFolder, alignerModel)}"

fun chooseQuestion(model: SpeechModel, modelFolder: Path, alignerModel: AlignerModel? = null): String =
    "${model.displayName} is not on this machine yet. Download it, and use it once it is in place?\n\n" +
        downloadFacts(model, modelFolder, alignerModel)

fun downloadAlignerQuestion(alignerModel: AlignerModel, alignerFolder: Path): String {
    val alignerPackage = alignerModel.modelPackage
    return "Download ${alignerModel.displayName}?\n\nIt finds the word being read, so that the highlight of the word " +
        "follows the voice. Until it is in place, the highlight estimates the word. " +
        "The download is ${megabytes(alignerPackage.sizeInBytes)} MB, from ${alignerPackage.address}. " +
        "Once unpacked, it takes ${megabytes(alignerPackage.unpackedSizeInBytes)} MB in ${alignerFolder.toAbsolutePath()}."
}

// The aligner model is given where the model is the last speech model in place, and is then removed with it
fun removeQuestion(model: SpeechModel, modelFolder: Path, alignerModel: AlignerModel? = null): String {
    val alignerRemoval = alignerModel?.let {
        " As no other speech model is in place, it also deletes ${it.displayName} in " +
            "${it.folderIn(modelFolder.toAbsolutePath().parent)} and frees ${megabytes(it.modelPackage.unpackedSizeInBytes)} MB more."
    } ?: ""
    return "Remove ${model.displayName}?\n\nThis deletes ${modelFolder.toAbsolutePath()} and frees " +
        "${megabytes(model.modelPackage.unpackedSizeInBytes)} MB.$alignerRemoval It can be downloaded again from the settings page."
}

fun removeRefusedMessage(model: SpeechModel): String =
    "${model.displayName} is in use, or the reading that goes on uses it, so it cannot be removed. " +
        "Choose another model, and remove this one once no reading uses it."

fun removeRefusedNotThePluginsMessage(modelFolder: Path): String =
    "The folder ${modelFolder.toAbsolutePath()} does not hold a complete model that AloudRite put there, " +
        "so AloudRite did not remove it."

fun failureText(failure: InstallFailure): String = when (failure) {
    is InstallFailure.NoConnection ->
        "There is no connection to ${failure.host}. Check the network and the proxy settings of the IDE."
    is InstallFailure.ServerAnswer -> "The download address answered with HTTP ${failure.status}."
    is InstallFailure.RedirectRefused ->
        "The download address sent AloudRite on to ${failure.location}, which is not an HTTPS address, " +
            "so AloudRite did not follow it."
    InstallFailure.TooManyRedirects -> "The download address redirected AloudRite more than 10 times."
    is InstallFailure.BrokeOff ->
        "The download broke off after ${megabytes(failure.receivedBytes)} of ${megabytes(failure.totalBytes)} MB."
    is InstallFailure.OutOfDiskSpace -> outOfDiskSpaceText(failure)
    is InstallFailure.CannotUnpack -> "The package could not be unpacked: ${failure.detail}."
    is InstallFailure.ChecksumMismatch ->
        "The package at ${failure.address} is not the one this version of AloudRite was built for: its SHA-256 " +
            "checksum does not match. The publisher has replaced the package at that address before. The README " +
            "of AloudRite says how to put a model in place by hand."
    InstallFailure.RateLimited ->
        "Hugging Face limits the downloads from this address for the moment. Press \"Try Again\" later to download the model."
    is InstallFailure.FolderInTheWay ->
        "The folder ${failure.folder} exists and does not hold a complete model. Remove it, then try again."
    InstallFailure.RunningElsewhere -> DOWNLOAD_RUNNING_ELSEWHERE_MESSAGE
    is InstallFailure.Unexpected -> failure.detail
}

// An archive needs room for itself and for what is unpacked from it for a moment, and the files of a repository only for
// themselves, which the model then takes
private fun outOfDiskSpaceText(failure: InstallFailure.OutOfDiskSpace): String {
    val modelPackage = failure.modelPackage
    val taken = "the model takes ${megabytes(modelPackage.unpackedSizeInBytes)} MB"
    val needs = if (modelPackage is RepositoryFiles) {
        "The download needs about ${megabytes(modelPackage.sizeInBytes)} MB, which $taken after."
    } else {
        "The download and the unpacking need about " +
            "${megabytes(modelPackage.sizeInBytes + modelPackage.unpackedSizeInBytes)} MB for a moment, and $taken after."
    }
    return "There is not enough free disk space in ${failure.folder}. $needs"
}
