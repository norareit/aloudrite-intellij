package com.ritenoar.aloudrite.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// Reads a Markdown text through the reading from its parts to the end of its audio, with a pause and a resume, on a
// classpath that holds no class of the IntelliJ Platform, which is what the module is for
class ReadingAloudWithoutThePlatformTest {

    private val standIns = SpeechStandIns<String>()
    private val reading = standIns.reading
    private val target = "notes.md"
    private val markdown = "# Title\n\nFirst paragraph.\n\nSecond paragraph.\n"

    @Test
    fun aMarkdownTextIsReadThroughAPauseAndAResumeToItsEndOnAClasspathWithoutThePlatform() {
        assertThrows(ClassNotFoundException::class.java) {
            Class.forName("com.intellij.openapi.application.ApplicationManager")
        }
        val whatToRead = whatToRead(markdown, PlaceInTheText.Caret(0))
        val expectedPieces = whatToRead.parts.filterIsInstance<Words>().flatMap { piecesOf(it.text) }.filter { it.isNotBlank() }
        reading.read(target, whatToRead, INT8_MODEL, System.nanoTime())
        pauseOnceInTheFirstPiece()

        standIns.runKeptWorks()
        assertTrue(reading.isPaused(target))
        assertTrue(reading.isReading(target))

        reading.resume(target, System.nanoTime())
        standIns.runKeptWorks()

        assertFalse(reading.isReading(target))
        assertEquals(expectedPieces, standIns.textsMade)
        assertTrue(standIns.failures.isEmpty())
    }

    private fun pauseOnceInTheFirstPiece() {
        var pausedOnce = false
        standIns.whilePlaying = { slicesPlayed ->
            if (slicesPlayed == 1 && !pausedOnce) {
                pausedOnce = true
                reading.pause(target)
            }
        }
    }
}
