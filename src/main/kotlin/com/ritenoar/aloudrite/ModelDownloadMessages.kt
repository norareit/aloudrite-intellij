package com.ritenoar.aloudrite

import java.nio.file.Path

private const val BYTES_PER_MEGABYTE = 1_000_000L

fun megabytes(bytes: Long): Long = (bytes + BYTES_PER_MEGABYTE / 2) / BYTES_PER_MEGABYTE

fun downloadQuestion(modelPackage: ModelPackage, modelFolder: Path): String =
    "AloudRite needs its speech model to speak, and the model is not on this machine yet. Download it now?\n\n" +
        "The download is ${megabytes(modelPackage.sizeInBytes)} MB, from ${modelPackage.address}. " +
        "Once unpacked, the model takes ${megabytes(modelPackage.unpackedSizeInBytes)} MB in " +
        "${modelFolder.toAbsolutePath()}."

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
            "${megabytes(KOKORO_MODEL_PACKAGE.sizeInBytes + KOKORO_MODEL_PACKAGE.unpackedSizeInBytes)} MB for a " +
            "moment, and the model takes ${megabytes(KOKORO_MODEL_PACKAGE.unpackedSizeInBytes)} MB after."
    is InstallFailure.CannotUnpack -> "The package could not be unpacked: ${failure.detail}."
    is InstallFailure.ChecksumMismatch ->
        "The package at ${failure.address} is not the one this version of AloudRite was built for: its SHA-256 " +
            "checksum does not match. The publisher has replaced the package at that address before. The README " +
            "of AloudRite says how to put a model in place by hand."
    is InstallFailure.FolderInTheWay ->
        "The folder ${failure.folder} exists and does not hold a complete model. Remove it, then try again."
    is InstallFailure.Unexpected -> failure.detail
}
