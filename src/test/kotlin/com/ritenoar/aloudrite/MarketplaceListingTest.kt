package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// What the JetBrains Marketplace and the plugin manager show of the plugin: its icons, its vendor and its description
class MarketplaceListingTest {

    private val resources = File("src/main/resources/META-INF")
    private val markSource = File("assets/icon.svg").readText()
    private val pluginXml = File(resources, "plugin.xml").readText()

    @Test
    fun eachIconIsFortyByFortyWithTheViewBoxAndTheShapesOfTheMark() {
        listOf("pluginIcon.svg", "pluginIcon_dark.svg").forEach { name ->
            val icon = File(resources, name).readText()

            assertEquals(name, "40", attribute(icon, "width"))
            assertEquals(name, "40", attribute(icon, "height"))
            assertEquals(name, attribute(markSource, "viewBox"), attribute(icon, "viewBox"))
            assertEquals(name, shapesOf(markSource), shapesOf(icon))
        }
    }

    @Test
    fun theSpeakerIsGreyBlueForLightThemesAndLighterForDarkOnesAndTheWavesAreGoldInBoth() {
        assertEquals(listOf("#4F6680", "#4F6680", "#E8B84A", "#E8B84A"), strokesOf("pluginIcon.svg"))
        assertEquals(listOf("#8FA6BF", "#8FA6BF", "#E8B84A", "#E8B84A"), strokesOf("pluginIcon_dark.svg"))
    }

    @Test
    fun theVendorIsRitenoarWithItsWebsiteAndItsEmailAddress() {
        assertTrue(pluginXml.contains("""<vendor url="https://ritenoar.com" email="nora+aloud@ritenoar.com">Ritenoar</vendor>"""))
    }

    @Test
    fun theDescriptionStartsWithAnEnglishSummaryOfAtMostFortyCharacters() {
        val text = description.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
        val summary = "Reads Markdown files aloud in the IDE."

        assertTrue(text.startsWith(summary))
        assertTrue(summary.length <= 40)
    }

    @Test
    fun theDescriptionLinksTheIssueTrackerTheTermsAndThePrivacyNotice() {
        listOf(
            "https://github.com/norareit/aloudrite-intellij/issues",
            "https://github.com/norareit/aloudrite-intellij/blob/master/EULA.md",
            "https://github.com/norareit/aloudrite-intellij/blob/master/PRIVACY.md",
        ).forEach { link -> assertTrue(link, description.contains("""<a href="$link">""")) }
    }

    @Test
    fun theDescriptionLinksAgentRiteAndTheSourceCodeAndNamesTheLicense() {
        assertTrue(description.contains("""<a href="https://agentrite.dev">AgentRite</a>"""))
        assertTrue(description.contains("""<a href="https://github.com/norareit/aloudrite-intellij">"""))
        assertTrue(description.contains("GPL-3.0-or-later"))
    }

    @Test
    fun theDescriptionGivesPauseAndResumeToTheReadAloudButtonAndStopToASecondButton() {
        val text = description.replace(Regex("\\s+"), " ")

        assertTrue(text.contains("the same button pauses and resumes the reading"))
        assertTrue(text.contains("While a reading runs or is paused, a second button beside the first, \"Stop Reading\", ends it."))
    }

    @Test
    fun theDescriptionStatesThePlatformsTheModelsTheirSizesAndTheirSource() {
        listOf("Linux x64", "Apple Silicon", "Kokoro", "Hugging Face", "190 MB", "401 MB", "48 MB", "13 voices", "only network requests")
            .forEach { fact -> assertTrue(fact, description.replace(Regex("\\s+"), " ").contains(fact)) }
    }

    @Test
    fun theDescriptionHoldsTheEightKeysOfTheShortcuts() {
        listOf(
            "Ctrl+Alt+Shift+Space", "Ctrl+Alt+Shift+O", "Ctrl+Alt+Shift+Comma", "Ctrl+Alt+Shift+Period",
            "&#8984;&#8997;&#8679;Space", "&#8984;&#8997;&#8679;O", "&#8984;&#8997;&#8679;,", "&#8984;&#8997;&#8679;.",
        ).forEach { key -> assertTrue(key, description.contains(key)) }
    }

    private val description: String
        get() = Regex("<description><!\\[CDATA\\[(.*?)]]></description>", RegexOption.DOT_MATCHES_ALL)
            .find(pluginXml)!!.groupValues[1]

    private fun strokesOf(name: String): List<String> =
        Regex("stroke=\"(#[0-9A-Fa-f]{6})\"").findAll(File(resources, name).readText()).map { it.groupValues[1] }.toList()

    private fun attribute(svg: String, name: String): String? =
        Regex("<svg[^>]*\\s$name=\"([^\"]*)\"").find(svg)?.groupValues?.get(1)

    // The circle and the paths with everything but their colour, which is what each icon sets for itself
    private fun shapesOf(svg: String): List<String> =
        Regex("<(circle|path)[^>]*>").findAll(svg).map { it.value.replace(Regex("stroke=\"#[0-9A-Fa-f]{6}\""), "stroke=\"\"") }.toList()
}
