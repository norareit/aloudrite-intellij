package com.ritenoar.aloudrite

import com.intellij.openapi.progress.ProgressIndicator
import java.nio.file.Path

// What the service does around the installer: claim the lock, install, close the attempt
fun ModelInstaller.installClaiming(modelFolder: Path, indicator: ProgressIndicator) {
    val attempt = checkNotNull(InstallAttempt.claim(modelFolder.toAbsolutePath().parent)) { "The lock is held" }
    attempt.use { install(modelFolder, it, indicator) }
}
