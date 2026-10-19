package com.ritenoar.aloudrite

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

// A second process for the tests of the lock. `hold <file>` locks the file, prints `locked` and keeps the lock until
// its standard input ends; `try <file>` prints `free` or `held`, according to whether it could lock the file
fun main(arguments: Array<String>) {
    val channel = FileChannel.open(Paths.get(arguments[1]), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    val lock = channel.tryLock()
    if (arguments[0] == "try") {
        println(if (lock == null) "held" else "free")
        return
    }
    check(lock != null) { "The file is locked already" }
    println("locked")
    System.out.flush()
    System.`in`.readAllBytes()
}

class LockProbe private constructor(private val process: Process, private val argumentFile: Path) : AutoCloseable {

    val firstLine: String = process.inputStream.bufferedReader().readLine().orEmpty()

    // Ends the process, which is how the operating system releases its lock
    fun kill() {
        process.destroyForcibly().waitFor()
    }

    override fun close() {
        kill()
        Files.deleteIfExists(argumentFile)
    }

    companion object {
        fun start(mode: String, lockFile: Path): LockProbe {
            val java = Paths.get(System.getProperty("java.home"), "bin", "java").toString()
            // Linux refuses a single argument of 128 KiB or more, which the classpath of the tests exceeds, so it goes
            // in an argument file that the launcher reads itself. The quotes keep the entries that hold a space whole
            val argumentFile = Files.createTempFile("lock-probe", ".args")
            // A probe that never starts is never closed, so the file is removed here instead
            val process = try {
                Files.writeString(argumentFile, "-cp \"${System.getProperty("java.class.path")}\"")
                ProcessBuilder(
                    java, "@$argumentFile", "com.ritenoar.aloudrite.LockProbeProcessKt", mode, lockFile.toString(),
                ).redirectErrorStream(true).start()
            } catch (failure: Exception) {
                Files.deleteIfExists(argumentFile)
                throw failure
            }
            return LockProbe(process, argumentFile)
        }
    }
}
