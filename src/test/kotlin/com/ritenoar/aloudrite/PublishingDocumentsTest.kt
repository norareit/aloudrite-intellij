package com.ritenoar.aloudrite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// The facts that the documents for a first publication on the JetBrains Marketplace state, which drift from the code and from each other
class PublishingDocumentsTest {

    private val eula = File("EULA.md").readText()
    private val privacy = File("PRIVACY.md").readText()
    private val thirdParty = File("THIRD-PARTY.md").readText()
    private val readme = File("README.md").readText()

    @Test
    fun theEulaNamesTheLicenseTheLicensorAndTheThirdPartyListAndSaysJetBrainsIsNoParty() {
        listOf("GPL-3.0-or-later", "Ritenoar CommV", "Belgium", "licensor", "THIRD-PARTY.md", "JetBrains", "warranty")
            .forEach { fact -> assertTrue(fact, eula.contains(fact)) }
    }

    @Test
    fun thePrivacyNoticeNamesThePublisherTheAddressAndWhatTheDownloadShowsToHuggingFace() {
        listOf("Ritenoar CommV", "nora+aloud@ritenoar.com", "Hugging Face", "https://huggingface.co/privacy", "IP address")
            .forEach { fact -> assertTrue(fact, privacy.contains(fact)) }
    }

    @Test
    fun theReadmeLinksTheChecklistTheEulaAndThePrivacyNotice() {
        listOf("docs/publishing.md", "EULA.md", "PRIVACY.md")
            .forEach { document -> assertTrue(document, readme.contains("]($document)")) }
    }

    @Test
    fun theArchiveHoldsTheFourDocumentsBesideTheLicense() {
        val build = File("build.gradle.kts").readText()

        assertTrue(build.contains("""files("LICENSE", "THIRD-PARTY.md", "EULA.md", "PRIVACY.md")"""))
        assertTrue(readme.contains("holds all four"))
    }

    @Test
    fun eachSourceArchiveHasAFileNameAGitHubAddressAndASha256AndNoneIsOfOnnxRuntime() {
        val rows = File("scripts/third-party-sources.tsv").readLines().filter { it.isNotBlank() }.map { it.split("\t") }

        assertEquals(16, rows.size)
        rows.forEach { row ->
            assertEquals(row.toString(), 3, row.size)
            assertTrue(row[1], row[1].startsWith("https://github.com/"))
            assertTrue(row[2], Regex("[0-9a-f]{64}").matches(row[2]))
            assertFalse(row[0], row[0].contains("onnxruntime", ignoreCase = true))
            assertTrue(row[0], thirdParty.contains("`${row[0]}`"))
        }
        assertEquals(rows.size, rows.map { it[0] }.toSet().size)
    }

    @Test
    fun theThirdPartyListNamesTheReleaseThatHoldsTheSourceAndTheSourceOfOnnxRuntime() {
        listOf("release `v1.0.0`", "https://github.com/microsoft/onnxruntime", "`v1.28.2`", "`33ca962`")
            .forEach { fact -> assertTrue(fact, thirdParty.contains(fact)) }
    }
}
