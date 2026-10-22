package com.ritenoar.aloudrite

import java.nio.file.Path

private const val BYTES_PER_MEGABYTE = 1_000_000L

fun megabytes(bytes: Long): Long = (bytes + BYTES_PER_MEGABYTE / 2) / BYTES_PER_MEGABYTE

const val DOWNLOAD_RUNNING_ELSEWHERE_MESSAGE =
    "A download of the AloudRite speech model is already running in another IDE window."

fun downloadQuestion(model: SpeechModel, modelFolder: Path): String {
    val modelPackage = model.modelPackage
    return "AloudRite needs its speech model (${model.displayName}) to speak, and the model is not on this machine yet. " +
        "Download it now?\n\n" +
        "The download is ${megabytes(modelPackage.sizeInBytes)} MB, from ${modelPackage.address}. " +
        "Once unpacked, the model takes ${megabytes(modelPackage.unpackedSizeInBytes)} MB in " +
        "${modelFolder.toAbsolutePath()}."
}

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
    is InstallFailure.OutOfDiskSpace ->
        "There is not enough free disk space in ${failure.folder}. The download and the unpacking need about " +
            "${megabytes(failure.modelPackage.sizeInBytes + failure.modelPackage.unpackedSizeInBytes)} MB for a " +
            "moment, and the model takes ${megabytes(failure.modelPackage.unpackedSizeInBytes)} MB after."
    is InstallFailure.CannotUnpack -> "The package could not be unpacked: ${failure.detail}."
    is InstallFailure.ChecksumMismatch ->
        "The package at ${failure.address} is not the one this version of AloudRite was built for: its SHA-256 " +
            "checksum does not match. The publisher has replaced the package at that address before. The README " +
            "of AloudRite says how to put a model in place by hand."
    is InstallFailure.FolderInTheWay ->
        "The folder ${failure.folder} exists and does not hold a complete model. Remove it, then try again."
    InstallFailure.RunningElsewhere -> DOWNLOAD_RUNNING_ELSEWHERE_MESSAGE
    is InstallFailure.Unexpected -> failure.detail
}
