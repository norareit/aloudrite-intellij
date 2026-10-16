package com.ritenoar.aloudrite

import java.nio.file.Path

sealed class InstallFailure {
    data class NoConnection(val host: String) : InstallFailure()
    data class ServerAnswer(val status: Int) : InstallFailure()
    data class RedirectRefused(val location: String) : InstallFailure()
    data object TooManyRedirects : InstallFailure()
    data class BrokeOff(val receivedBytes: Long, val totalBytes: Long) : InstallFailure()
    data class OutOfDiskSpace(val folder: Path) : InstallFailure()
    data class CannotUnpack(val detail: String) : InstallFailure()
    data class ChecksumMismatch(val address: String) : InstallFailure()
    data class FolderInTheWay(val folder: Path) : InstallFailure()
    data object RunningElsewhere : InstallFailure()
    data class Unexpected(val detail: String) : InstallFailure()
}

class ModelInstallException(val failure: InstallFailure, cause: Throwable? = null) :
    Exception(failure.toString(), cause)
