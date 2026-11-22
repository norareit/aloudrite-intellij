package com.ritenoar.aloudrite

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.testFramework.ApplicationRule
import com.ritenoar.aloudrite.core.RepositoryFiles
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class RepositoryDownloadTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val indicator = RecordingIndicator()
    private val clock = ClockThatMovesWhilePaused { indicator.text == RATE_LIMITED_TEXT }
    private val server = PackageServerStandIn(clock::nowMillis)
    private val standIn = RepositoryFilesStandIn()
    private val files: RepositoryFiles = standIn.repositoryFiles(server)
    private lateinit var target: Path

    @After
    fun stopServer() = server.close()

    private fun targetFolder(): Path = temporaryFolder.newFolder().toPath().resolve("model").also { target = it }

    private fun download() = downloadRepositoryFiles(files, targetFolder(), indicator, "the speech model", clock)

    private fun failureOfDownload(): InstallFailure {
        try {
            download()
        } catch (exception: ModelInstallException) {
            return exception.failure
        }
        throw AssertionError("The download did not fail")
    }

    private fun addressOf(path: String) = files.fileAddress(files.files.single { it.path == path })

    private fun aFileThatIsNotTheSpaceOne(): String = standIn.contents.keys.first { it != RepositoryFilesStandIn.SPACE_PATH }

    private fun assertEveryFileArrived() {
        val arrived = Files.walk(target).use { paths -> paths.filter { Files.isRegularFile(it) }.toList() }
        assertEquals(standIn.contents.size, arrived.size)
        standIn.contents.forEach { (path, bytes) -> assertTrue("$path differs", bytes.contentEquals(Files.readAllBytes(target.resolve(path)))) }
    }

    private fun assertNoWorkerRuns() {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (workerThreads().isNotEmpty() && System.nanoTime() < deadline) Thread.sleep(10)
        assertEquals(emptyList<String>(), workerThreads())
    }

    private fun workerThreads() = Thread.getAllStackTraces().keys
        .filter { it.isAlive && it.name.startsWith(DOWNLOAD_WORKER_THREAD_PREFIX) }.map { it.name }

    @Test
    fun theRepositoryArrivesIntoItsFolderWithTheFileThatHasASpaceAmongIt() {
        standIn.serveOn(server)

        download()

        assertEveryFileArrived()
        assertTrue(Files.isRegularFile(target.resolve(RepositoryFilesStandIn.SPACE_PATH)))
    }

    @Test
    fun theStandInReceivesTheAddressesOfTheFilesOfTheListAndNoOtherPath() {
        standIn.serveOn(server)

        download()

        assertEquals(standIn.contents.keys.map(standIn::rawPathOf).sorted(), server.requests.map { it.rawPath }.sorted())
    }

    @Test
    fun aFileOneByteLongerThanItsSizeFailsNamingItsAddress() {
        val path = aFileThatIsNotTheSpaceOne()
        standIn.serveOn(server) { requested, _ -> if (requested == path) StandInAnswer(body = standIn.contents.getValue(path) + 1) else null }

        assertEquals(InstallFailure.ChecksumMismatch(addressOf(path)), failureOfDownload())
        assertNoWorkerRuns()
    }

    @Test
    fun aFileWhoseSha256DiffersFailsNamingItsAddress() {
        standIn.serveOn(server) { requested, _ ->
            if (requested == RepositoryFilesStandIn.SPACE_PATH) {
                StandInAnswer(body = standIn.contents.getValue(requested).also { it[0] = (it[0] + 1).toByte() })
            } else null
        }

        assertEquals(InstallFailure.ChecksumMismatch(addressOf(RepositoryFilesStandIn.SPACE_PATH)), failureOfDownload())
        assertNoWorkerRuns()
    }

    @Test
    fun anAnswerOf429WithRetryAfterPausesForItsSeconds() = assertPause(mapOf("Retry-After" to "30"), 30)

    @Test
    fun anAnswerOf429WithOnlyTheRateLimitHeaderPausesForItsTime() =
        assertPause(mapOf("ratelimit" to "\"resolvers\";r=0;t=120"), 120)

    @Test
    fun anAnswerOf429WithNeitherHeaderPausesForAMinute() = assertPause(emptyMap(), 60)

    @Test
    fun aPauseIsCappedAtFiveMinutes() = assertPause(mapOf("Retry-After" to "900"), 300)

    // The clock moves only while the download is paused, so the stand-in stamps every request of a stretch without a
    // pause with the same time, and a request that came during a pause would carry a time inside it
    private fun assertPause(headers: Map<String, String>, seconds: Long) {
        val refusedPath = aFileThatIsNotTheSpaceOne()
        standIn.serveOn(server) { requested, number ->
            if (requested == refusedPath && number == 1) StandInAnswer(status = 429, headers = headers) else null
        }
        val start = clock.nowMillis()

        download()

        val end = start + seconds * 1_000
        val times = server.requests.map { it.atMillis }
        assertTrue("A request came during the pause: $times", times.none { it > start && it < end })
        assertTrue("No request came after the pause: $times", times.any { it >= end })
        assertEquals(end, clock.nowMillis())
        assertEquals(2, server.requests.count { it.rawPath == standIn.rawPathOf(refusedPath) })
        assertEquals(1, server.requests.count { it.rawPath == standIn.rawPathOf(RepositoryFilesStandIn.SPACE_PATH) })
        assertEveryFileArrived()
    }

    @Test
    fun theFilesThatArrivedBeforeThePauseAreNotRequestedAgain() {
        val refusedPath = standIn.contents.keys.last()
        standIn.serveOn(server) { requested, number ->
            if (requested == refusedPath && number == 1) StandInAnswer(status = 429, delayMillis = 300) else null
        }

        download()

        val requestedPaths = server.requests.map { it.rawPath }
        assertEquals(standIn.contents.size + 1, requestedPaths.size)
        assertEquals(requestedPaths.size - 1, requestedPaths.toSet().size)
    }

    @Test
    fun theSecondLineCountsDownDuringThePauseAndTheFirstNamesTheLimit() {
        val refusedPath = aFileThatIsNotTheSpaceOne()
        standIn.serveOn(server) { requested, number ->
            if (requested == refusedPath && number == 1) StandInAnswer(status = 429, headers = mapOf("Retry-After" to "30")) else null
        }

        download()

        val countdown = indicator.secondLines.filter { it.startsWith("Continuing in") }.distinct()
        assertEquals((30 downTo 1).map { "Continuing in 0:%02d".format(it) }, countdown)
        assertTrue(indicator.firstLinesWhileSecondLineCounts.all { it == RATE_LIMITED_TEXT })
        assertEquals("Downloading the speech model", indicator.firstLines.last())
    }

    @Test
    fun anAnswerOf429ToTheFirstRequestAfterAPauseFailsTheDownload() {
        val refusedPath = aFileThatIsNotTheSpaceOne()
        standIn.serveOn(server) { requested, _ ->
            if (requested == refusedPath) StandInAnswer(status = 429, headers = mapOf("Retry-After" to "30")) else null
        }

        assertEquals(InstallFailure.RateLimited, failureOfDownload())
        assertNoWorkerRuns()
        assertEquals(2, server.requests.count { it.rawPath == standIn.rawPathOf(refusedPath) })
    }

    @Test
    fun cancellingDuringThePauseEndsTheDownloadWithNoWorkerRunning() {
        val refusedPath = aFileThatIsNotTheSpaceOne()
        standIn.serveOn(server) { requested, number ->
            if (requested == refusedPath && number == 1) StandInAnswer(status = 429, headers = mapOf("Retry-After" to "30")) else null
        }
        indicator.onSecondLine = { if (it == "Continuing in 0:20") indicator.cancel() }

        assertCancelled { download() }

        assertNoWorkerRuns()
        assertEquals(1, server.requests.count { it.rawPath == standIn.rawPathOf(refusedPath) })
    }

    @Test
    fun noMoreThanEightRequestsAreOpenAtOnceAndMoreThanOneIs() {
        standIn.serveOn(server) { requested, _ -> StandInAnswer(body = standIn.contents.getValue(requested), delayMillis = 150) }

        download()

        assertEquals(8, server.mostRequestsOpenAtOnce)
        assertEveryFileArrived()
    }

    @Test
    fun cancellingWhileFilesArriveStopsEveryWorkerBeforeTheCallReturns() {
        standIn.serveOn(server) { requested, _ -> StandInAnswer(body = standIn.contents.getValue(requested), delayMillis = 100) }
        indicator.onFraction = { if (it > 0.0) indicator.cancel() }

        assertCancelled { download() }

        assertNoWorkerRuns()
    }

    @Test
    fun theFractionEndsAtOneAndNeverGoesDown() {
        standIn.serveOn(server)

        download()

        assertEquals(1.0, indicator.fractions.last(), 0.0)
        assertEquals(indicator.fractions.sorted(), indicator.fractions.toList())
        assertNotEquals(0, indicator.fractions.size)
        assertFalse(indicator.fractions.any { it > 1.0 })
    }

    private fun assertCancelled(work: () -> Unit) {
        try {
            work()
            fail("The download was not cancelled")
        } catch (_: ProcessCanceledException) {
        }
    }

    private class RecordingIndicator : ProgressIndicatorBase() {
        val fractions = CopyOnWriteArrayList<Double>()
        val firstLines = CopyOnWriteArrayList<String>()
        val secondLines = CopyOnWriteArrayList<String>()
        val firstLinesWhileSecondLineCounts = CopyOnWriteArrayList<String>()
        var onSecondLine: (String) -> Unit = {}
        var onFraction: (Double) -> Unit = {}

        override fun setFraction(fraction: Double) {
            super.setFraction(fraction)
            fractions += fraction
            onFraction(fraction)
        }

        override fun setText(text: String?) {
            super.setText(text)
            firstLines += text.orEmpty()
        }

        override fun setText2(text: String?) {
            super.setText2(text)
            secondLines += text.orEmpty()
            if (text.orEmpty().startsWith("Continuing in")) firstLinesWhileSecondLineCounts += this.text
            onSecondLine(text.orEmpty())
        }
    }

    // A clock that stands still while files arrive and moves only during a pause, by the time the coordinator waits, so that
    // a pause of five minutes takes a moment and the times that the stand-in records say whether a request came during it
    private class ClockThatMovesWhilePaused(private val isPaused: () -> Boolean) : DownloadClock {
        private val now = AtomicLong(1_000_000)

        override fun nowMillis(): Long = now.get()

        override fun <T : Any> poll(queue: BlockingQueue<T>, timeoutMillis: Long): T? {
            queue.poll(WAIT_MILLIS, TimeUnit.MILLISECONDS)?.let { return it }
            if (isPaused()) now.addAndGet(timeoutMillis)
            return null
        }
    }

    companion object {
        private const val WAIT_MILLIS = 2L

        @JvmField
        @ClassRule
        val application = ApplicationRule()
    }
}
