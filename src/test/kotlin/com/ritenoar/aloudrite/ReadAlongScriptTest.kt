package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path
import java.util.concurrent.TimeUnit

// Runs the tests of the script of the page, which are written for Node against a stand-in for the browser and the pipe, so
// that the lifecycle of the script and its decisions to follow are tested without an embedded browser. Node is a tool of the
// project already, as the change check runs on it
class ReadAlongScriptTest {

    @Test
    fun theScriptOfThePageKeepsItsLifecycleAndItsDecisionsToFollow() {
        val process = ProcessBuilder("node", "--test", Path.of("src/test/javascript").toString())
            .redirectErrorStream(true)
            .also { it.environment()["READ_ALONG_SCRIPT"] = Path.of("src/main/resources/com/ritenoar/aloudrite/preview/aloudrite-read-along.js").toString() }
            .start()
        val output = process.inputStream.bufferedReader().readText()

        assertEquals(output, true, process.waitFor(2, TimeUnit.MINUTES))
        assertEquals(output, 0, process.exitValue())
    }
}
