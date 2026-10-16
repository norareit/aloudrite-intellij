package com.ritenoar.aloudrite

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.ApplicationRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

// The press of the button, through the question, the download from a stand-in server and the speech
class ModelDownloadFlowTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val standIn = ModelPackageStandIn.create()
    private val server = PackageServerStandIn()
    private val disposable = Disposer.newDisposable()
    private val keptWorks = mutableListOf<() -> Unit>()
    private val spoken = mutableListOf<Pair<String, Path>>()
    private val notifications = mutableListOf<Notification>()
    private lateinit var modelFolder: Path

    @Before
    fun prepare() {
        modelFolder = temporaryFolder.newFolder().toPath().resolve("aloudrite").resolve(MODEL_PACKAGE_NAME)
        ApplicationManager.getApplication().messageBus.connect(disposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                notifications += notification
            }
        })
    }

    @After
    fun cleanUp() {
        Disposer.dispose(disposable)
        server.close()
    }

    @Test
    fun theDefaultActionFindsTheDownloadService() {
        assertFalse(service<ModelDownloadService>().isRunning)
    }

    @Test
    fun aDownloadThatSucceedsIsFollowedByTheSpeech() {
        server.serve("/package", standIn.bytes)
        val action = actionDownloading(standIn.modelPackage(server.address("/package")))

        val messages = pressTheButtonOn(action, markdownFile("# Title"), answerTo = { Messages.YES })

        assertEquals(listOf(downloadQuestion(KOKORO_MODEL_PACKAGE, modelFolder)), messages)
        assertTrue(spoken.isEmpty())
        assertEquals(1, keptWorks.size)

        runKeptWork()

        assertTrue(isModelInPlace(modelFolder))
        assertEquals(listOf(TEST_SENTENCE to modelFolder), spoken)
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun aDeclinedQuestionDownloadsAndSpeaksNothingAndIsAskedAgain() {
        server.serve("/package", standIn.bytes)
        val action = actionDownloading(standIn.modelPackage(server.address("/package")))

        val messages = pressTheButtonOn(action, markdownFile("# Title"), answerTo = { Messages.NO }) +
            pressTheButtonOn(action, markdownFile("# Title"), answerTo = { Messages.NO })

        assertEquals(2, messages.size)
        assertTrue(keptWorks.isEmpty())
        assertEquals(0, server.requestCount("/package"))
        assertTrue(spoken.isEmpty())
        assertFalse(isModelInPlace(modelFolder))
    }

    @Test
    fun cancellingDuringTheDownloadSpeaksNothingAndReportsNothing() {
        server.serve("/package", standIn.bytes)
        val indicator = indicatorThatCancels { fraction, text -> text == "Downloading the speech model" && fraction >= 0.5 }

        pressAgreeAndRun(standIn.modelPackage(server.address("/package")), indicator)

        assertEndedWithoutModel()
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun cancellingDuringTheUnpackingSpeaksNothingAndReportsNothing() {
        server.serve("/package", standIn.bytes)
        val indicator = indicatorThatCancels { fraction, text -> text == "Unpacking the speech model" && fraction > 0.0 }

        pressAgreeAndRun(standIn.modelPackage(server.address("/package")), indicator)

        assertEndedWithoutModel()
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun aDownloadThatBreaksOffIsReportedWithAnOfferToTryAgain() {
        server.serveCutOff("/package", standIn.bytes, standIn.bytes.size / 2)

        pressAgreeAndRun(standIn.modelPackage(server.address("/package")))

        assertEndedWithoutModel()
        assertSingleNotificationWithText("The download broke off after 0 of 0 MB.")
    }

    @Test
    fun aChecksumThatDoesNotMatchIsReported() {
        server.serve("/package", standIn.bytes)
        val modelPackage = ModelPackage(server.address("/package"), standIn.bytes.size.toLong(), 1L, "0".repeat(64))

        pressAgreeAndRun(modelPackage)

        assertEndedWithoutModel()
        assertSingleNotificationWithText(failureText(InstallFailure.ChecksumMismatch(modelPackage.address)))
    }

    @Test
    fun aPackageThatCannotBeUnpackedIsReported() {
        val cutOff = standIn.cutOffAtHalf()
        server.serve("/package", cutOff.bytes)

        pressAgreeAndRun(cutOff.modelPackage(server.address("/package")))

        assertEndedWithoutModel()
        val notification = notifications.single()
        assertTrue(notification.content, notification.content.startsWith("The package could not be unpacked: "))
    }

    @Test
    fun aModelAlreadyInPlaceAsksNothingRequestsNothingAndSpeaksAtThePress() {
        server.serve("/package", standIn.bytes)
        fillWithModelStandIn(modelFolder)
        val action = actionDownloading(standIn.modelPackage(server.address("/package")))

        val messages = pressTheButtonOn(action, markdownFile("# Title"))

        assertTrue(messages.isEmpty())
        assertEquals(0, server.requestCount("/package"))
        assertTrue(keptWorks.isEmpty())
        assertEquals(listOf(TEST_SENTENCE to modelFolder), spoken)
    }

    @Test
    fun aPressWhileADownloadIsRunningAsksNothingAndStartsNothing() {
        server.serve("/package", standIn.bytes)
        val action = actionDownloading(standIn.modelPackage(server.address("/package")))
        val file = markdownFile("# Title")

        val firstMessages = pressTheButtonOn(action, file, answerTo = { Messages.YES })
        val secondMessages = pressTheButtonOn(action, file, answerTo = { Messages.YES })

        assertEquals(1, firstMessages.size)
        assertTrue(secondMessages.isEmpty())
        assertEquals(1, keptWorks.size)
        assertEquals(0, server.requestCount("/package"))
    }

    @Test
    fun aPressWhileThisProcessHoldsTheLockAsksNothingStartsNothingAndSaysSo() {
        server.serve("/package", standIn.bytes)
        val action = actionDownloading(standIn.modelPackage(server.address("/package")))

        val messages = InstallAttempt.claim(modelFolder.parent)!!.use {
            pressTheButtonOn(action, markdownFile("# Title"), answerTo = { Messages.YES })
        }

        assertEquals(listOf(DOWNLOAD_RUNNING_ELSEWHERE_MESSAGE), messages)
        assertTrue(keptWorks.isEmpty())
        assertEquals(0, server.requestCount("/package"))
        assertEndedWithoutModel()
    }

    @Test
    fun aPressWhileAnotherProcessHoldsTheLockAsksNothingStartsNothingAndSaysSo() {
        server.serve("/package", standIn.bytes)
        val action = actionDownloading(standIn.modelPackage(server.address("/package")))
        Files.createDirectories(modelFolder.parent)
        val lockFile = modelFolder.parent.toRealPath().resolve(InstallAttempt.LOCK_FILE_NAME)

        val messages = LockProbe.start("hold", lockFile).use { otherIde ->
            assertEquals("locked", otherIde.firstLine)
            pressTheButtonOn(action, markdownFile("# Title"), answerTo = { Messages.YES })
        }

        assertEquals(listOf(DOWNLOAD_RUNNING_ELSEWHERE_MESSAGE), messages)
        assertTrue(keptWorks.isEmpty())
        assertEquals(0, server.requestCount("/package"))
        assertEndedWithoutModel()
        assertEquals(listOf(InstallAttempt.LOCK_FILE_NAME), namesIn(modelFolder.parent))
    }

    @Test
    fun theLockIsHeldDuringTheDownloadAndFreeAfterEachWayToEnd() {
        server.serve("/package", standIn.bytes)
        val action = actionDownloading(standIn.modelPackage(server.address("/package")))

        pressTheButtonOn(action, markdownFile("# Title"), answerTo = { Messages.NO })
        InstallAttempt.claim(modelFolder.parent)!!.close()

        pressTheButtonOn(action, markdownFile("# Title"), answerTo = { Messages.YES })
        assertNull(InstallAttempt.claim(modelFolder.parent))
        runKeptWork(indicatorThatCancels { fraction, text -> text == "Downloading the speech model" && fraction >= 0.5 })
        InstallAttempt.claim(modelFolder.parent)!!.close()
        assertEquals(listOf(InstallAttempt.LOCK_FILE_NAME), namesIn(modelFolder.parent))

        pressTheButtonOn(action, markdownFile("# Title"), answerTo = { Messages.YES })
        runKeptWork()
        InstallAttempt.claim(modelFolder.parent)!!.close()
        assertEquals(listOf(InstallAttempt.LOCK_FILE_NAME, MODEL_PACKAGE_NAME), namesIn(modelFolder.parent))
    }

    private fun namesIn(directory: Path): List<String> =
        Files.list(directory).use { entries -> entries.map { it.fileName.toString() }.toList().sorted() }

    @Test
    fun anUnsupportedPlatformSaysSoAndAsksOrDownloadsNothing() {
        server.serve("/package", standIn.bytes)
        val action = actionDownloading(standIn.modelPackage(server.address("/package")), "Windows 11", "amd64")
        val file = markdownFile("# Title")

        val offered = isOfferedFor(action, file)
        val messages = pressTheButtonOn(action, file, answerTo = { Messages.YES })

        assertTrue(offered)
        assertEquals(
            listOf("Speech is not supported on this platform. AloudRite speaks on Linux x64 and on macOS on Apple Silicon."),
            messages)
        assertTrue(keptWorks.isEmpty())
        assertEquals(0, server.requestCount("/package"))
        assertEndedWithoutModel()
    }

    private fun actionDownloading(
        modelPackage: ModelPackage,
        operatingSystemName: String = "Linux",
        architectureName: String = "amd64",
    ): ReadAloudAction {
        val downloads = ModelDownloadService(
            runInBackground = { work -> keptWorks += work },
            install = { folder, attempt, indicator -> ModelInstaller(modelPackage).install(folder, attempt, indicator) },
            reportFailure = ::notifyAboutInstallFailure,
        )
        return ReadAloudAction(
            operatingSystemName, architectureName, modelFolder, { text, folder, _ -> spoken += text to folder }, { downloads })
    }

    private fun pressAgreeAndRun(modelPackage: ModelPackage, indicator: ProgressIndicator = EmptyProgressIndicator()) {
        pressTheButtonOn(actionDownloading(modelPackage), markdownFile("# Title"), answerTo = { Messages.YES })
        runKeptWork(indicator)
    }

    private fun runKeptWork(indicator: ProgressIndicator = EmptyProgressIndicator()) {
        val work = keptWorks.last()
        try {
            ProgressManager.getInstance().runProcess(Runnable { work() }, indicator)
        } catch (_: ProcessCanceledException) {
        }
    }

    private fun indicatorThatCancels(cancelAt: (fraction: Double, text: String) -> Boolean) = object : ProgressIndicatorBase() {
        override fun setFraction(fraction: Double) {
            super.setFraction(fraction)
            if (cancelAt(fraction, text)) cancel()
        }
    }

    private fun assertEndedWithoutModel() {
        assertFalse(isModelInPlace(modelFolder))
        assertTrue(spoken.isEmpty())
    }

    private fun assertSingleNotificationWithText(text: String) {
        val notification = notifications.single()
        assertEquals("AloudRite", notification.groupId)
        assertEquals(text, notification.content)
        assertEquals(listOf("Try Again"), notification.actions.map { it.templateText })
    }

    private fun isOfferedFor(action: ReadAloudAction, file: com.intellij.openapi.vfs.VirtualFile): Boolean {
        val event = eventFor(action, file)
        ActionUtil.updateAction(action, event)
        return event.presentation.isEnabledAndVisible
    }

    companion object {
        @JvmField
        @ClassRule
        val application = ApplicationRule()

        private const val TEST_SENTENCE = "AloudRite can speak. This sentence was made on your own machine."
    }
}
