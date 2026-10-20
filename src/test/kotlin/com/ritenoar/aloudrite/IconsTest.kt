package com.ritenoar.aloudrite

import com.intellij.openapi.util.IconLoader
import com.intellij.testFramework.ApplicationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test
import javax.swing.Icon

class IconsTest {

    @Test
    fun everyIconIsSixteenBySixteen() {
        for (path in ALL_PATHS) {
            val svg = svgAt(path)
            assertEquals("The width of $path", "16", attribute(svg, "width"))
            assertEquals("The height of $path", "16", attribute(svg, "height"))
            assertEquals("The view box of $path", "0 0 16 16", attribute(svg, "viewBox"))
        }
    }

    @Test
    fun theGreenPersonStrokesAndFillsInTheGreenOfRunInTheLightTheme() {
        for (path in listOf(READ_ALOUD, RESUME_READING)) {
            assertEquals(path, setOf("#208A3C"), colorsOf(svgAt(path), "stroke"))
            assertEquals(path, setOf("#F2FCF3"), colorsOf(svgAt(path), "fill"))
        }
    }

    @Test
    fun theGreenPersonStrokesAndFillsInTheGreenOfRunInTheDarkTheme() {
        for (path in listOf(READ_ALOUD, RESUME_READING).map(::darkOf)) {
            assertEquals(path, setOf("#57965C"), colorsOf(svgAt(path), "stroke"))
            assertEquals(path, setOf("#253627"), colorsOf(svgAt(path), "fill"))
        }
    }

    @Test
    fun theToolWindowIconIsTheGreyOfTheOtherToolWindowIconsWithoutAFill() {
        assertEquals(setOf("#6C707E"), colorsOf(svgAt(TOOL_WINDOW), "stroke"))
        assertEquals(setOf("#CED0D6"), colorsOf(svgAt(darkOf(TOOL_WINDOW)), "stroke"))
        assertTrue(colorsOf(svgAt(TOOL_WINDOW), "fill").isEmpty())
        assertTrue(colorsOf(svgAt(darkOf(TOOL_WINDOW)), "fill").isEmpty())
    }

    @Test
    fun theResumeIconIsThePersonWithALineInFront() {
        val resume = svgAt(RESUME_READING)

        assertTrue("The line in front is missing", "M1.25 3V13" in resume)
        assertTrue("The person is missing", personHead in resume)
        assertTrue("The person is missing", personHead in svgAt(READ_ALOUD))
        assertTrue("The person is missing", personHead in svgAt(TOOL_WINDOW))
    }

    @Test
    fun theIconsAreLoadedFromTheirPaths() {
        assertSameIcon(READ_ALOUD, AloudRiteIcons.ReadAloud)
        assertSameIcon(RESUME_READING, AloudRiteIcons.ResumeReading)
        assertSameIcon(TOOL_WINDOW, AloudRiteIcons.ToolWindow)
    }

    private fun assertSameIcon(path: String, icon: Icon) {
        // A loaded icon names its path, and its size is not drawn in the headless tests, so the size is that of the SVG
        assertEquals(IconLoader.getIcon(path, AloudRiteIcons::class.java).toString(), icon.toString())
        assertTrue("$icon is not loaded from $path", path in icon.toString())
    }

    private fun svgAt(path: String): String {
        val resource = IconsTest::class.java.getResource(path)
        assertNotNull("There is no $path", resource)
        return resource!!.readText()
    }

    private fun attribute(svg: String, name: String): String? =
        Regex("""<svg[^>]*\s$name="([^"]*)"""").find(svg)?.groupValues?.get(1)

    // The colors given to the attribute in the drawing, leaving out "none", which the outer element uses
    private fun colorsOf(svg: String, name: String): Set<String> =
        Regex("""\s$name="([^"]*)"""").findAll(svg).map { it.groupValues[1] }.filter { it != "none" }.toSet()

    private fun darkOf(path: String) = path.removeSuffix(".svg") + "_dark.svg"

    private val personHead = """<circle cx="5.5" cy="5.5" r="2.5""""

    companion object {
        private const val READ_ALOUD = "/icons/readAloud.svg"
        private const val RESUME_READING = "/icons/resumeReading.svg"
        private const val TOOL_WINDOW = "/icons/toolWindow.svg"
        private val ALL_PATHS = listOf(READ_ALOUD, RESUME_READING, TOOL_WINDOW).flatMap { listOf(it, it.removeSuffix(".svg") + "_dark.svg") }

        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
