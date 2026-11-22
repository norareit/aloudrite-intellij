package com.ritenoar.aloudrite

import com.intellij.openapi.project.ProjectManager
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.LightVirtualFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test

class SpeechServiceTest {

    private val standIns = SpeechStandIns<ReadingTarget>()
    private val service = SpeechService(standIns.reading)
    private val target = ReadingTarget(null, LightVirtualFile("notes.md", "text"))

    @Test
    fun closingTheProjectStopsItsReadingAndNoOther() {
        val project = ProjectManager.getInstance().defaultProject
        val projectTarget = ReadingTarget(project, target.file)
        standIns.reading.read(projectTarget, wholeFile(listOf(Words("One sentence."))), INT8_MODEL, System.nanoTime())

        service.projectClosed(null)
        assertTrue(standIns.reading.isReading(projectTarget))

        service.projectClosed(project)
        assertFalse(standIns.reading.isReading(projectTarget))

        standIns.reading.read(target, wholeFile(listOf(Words("One sentence."))), INT8_MODEL, System.nanoTime())
        service.projectClosed(project)
        assertTrue(standIns.reading.isReading(target))
        service.projectClosed(null)
        assertFalse(standIns.reading.isReading(target))
    }

    companion object {
        @ClassRule
        @JvmField
        val application = ApplicationRule()
    }
}
