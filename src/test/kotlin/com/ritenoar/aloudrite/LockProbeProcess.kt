package com.ritenoar.aloudrite

import java.nio.channels.FileChannel
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

class LockProbe private constructor(private val process: Process) : AutoCloseable {

    val firstLine: String = process.inputStream.bufferedReader().readLine().orEmpty()

    // Ends the process, which is how the operating system releases its lock
    fun kill() {
        process.destroyForcibly().waitFor()
    }

    override fun close() = kill()

    companion object {
        fun start(mode: String, lockFile: java.nio.file.Path): LockProbe {
            val java = Paths.get(System.getProperty("java.home"), "bin", "java").toString()
            val process = ProcessBuilder(
                java, "-cp", System.getProperty("java.class.path"), "com.ritenoar.aloudrite.LockProbeProcessKt",
                mode, lockFile.toString(),
            ).redirectErrorStream(true).start()
            return LockProbe(process)
        }
    }
}
