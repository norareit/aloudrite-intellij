package com.ritenoar.aloudrite

import com.intellij.openapi.progress.ProgressIndicator
import com.ritenoar.aloudrite.core.RepositoryFile
import com.ritenoar.aloudrite.core.RepositoryFiles
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private const val MOST_FILES_AT_ONCE = 8
private const val LOOK_UP_MILLIS = 250L
private const val MILLIS_PER_SECOND = 1_000L

// What the coordinator knows of time and of waiting, so that a test runs a pause of five minutes in a moment
interface DownloadClock {
    fun nowMillis(): Long

    // Waits for an outcome of a worker for at most the given time, and returns null when none came
    fun <T : Any> poll(queue: BlockingQueue<T>, timeoutMillis: Long): T?
}

object SystemDownloadClock : DownloadClock {
    override fun nowMillis(): Long = System.nanoTime() / 1_000_000

    override fun <T : Any> poll(queue: BlockingQueue<T>, timeoutMillis: Long): T? =
        queue.poll(timeoutMillis, TimeUnit.MILLISECONDS)
}

// Named so that a thread dump says what they are, and daemons so that one that a network read holds never keeps the IDE
// from ending. The tests look for the name to see that no worker outlives a download
const val DOWNLOAD_WORKER_THREAD_PREFIX = "aloudrite-download-"

private object DownloadWorkerThreads : ThreadFactory {
    private val numbers = AtomicInteger()

    override fun newThread(task: Runnable): Thread =
        Thread(task, DOWNLOAD_WORKER_THREAD_PREFIX + numbers.incrementAndGet()).also { it.isDaemon = true }
}

private sealed class FileOutcome {
    abstract val file: RepositoryFile

    data class Arrived(override val file: RepositoryFile) : FileOutcome()
    data class Refused(override val file: RepositoryFile, val answer: RateLimitedAnswer) : FileOutcome()
    data class Failed(override val file: RepositoryFile, val exception: Exception) : FileOutcome()
}

// Ends the one file whose worker was told to stop
private class Stopped : RuntimeException()

// Downloads every file of the repository into the folder, at most eight at a time, each checked against its size and its
// SHA-256. The calling thread only coordinates: it alone looks at the indicator and decides what is requested next. An
// answer that the server limits the requests pauses the whole download, as RateLimitPause says. A cancellation, and a
// failure of any file, stop every worker and wait for it to end before they are thrown, so that none still writes when the
// caller removes the folder
fun downloadRepositoryFiles(
    repositoryFiles: RepositoryFiles,
    targetFolder: Path,
    indicator: ProgressIndicator,
    progressName: String,
    clock: DownloadClock = SystemDownloadClock,
    resolveRedirect: (current: String, location: String?) -> String = ::redirectTarget,
) {
    val download = RepositoryDownload(repositoryFiles, targetFolder, indicator, progressName, clock, resolveRedirect)
    try {
        download.run()
    } finally {
        download.stopWorkers()
    }
}

private class RepositoryDownload(
    private val repositoryFiles: RepositoryFiles,
    private val targetFolder: Path,
    private val indicator: ProgressIndicator,
    private val progressName: String,
    private val clock: DownloadClock,
    private val resolveRedirect: (current: String, location: String?) -> String,
) {
    private val stop = AtomicBoolean(false)
    private val receivedBytes = AtomicLong(0)
    private val outcomes = LinkedBlockingQueue<FileOutcome>()
    private val workers: ExecutorService = Executors.newFixedThreadPool(MOST_FILES_AT_ONCE, DownloadWorkerThreads)

    private val missing = ArrayDeque(repositoryFiles.files)
    private var open = 0
    private var pauseEnd: Long? = null

    // The first request after a pause goes alone: it shows whether the limit is over before the others spend the window
    private var nextIsProbe = false
    private var probe: RepositoryFile? = null

    fun run() {
        indicator.isIndeterminate = false
        while (missing.isNotEmpty() || open > 0) {
            indicator.checkCanceled()
            endPauseIfOver()
            submitMissing()
            showProgress()
            clock.poll(outcomes, waitMillis())?.let(::take)
        }
        showProgress()
    }

    fun stopWorkers() {
        stop.set(true)
        workers.shutdown()
        while (!workers.awaitTermination(1, TimeUnit.SECONDS)) {
            // Each worker ends at its next read, which the network may hold for a while
        }
    }

    private fun endPauseIfOver() {
        val end = pauseEnd ?: return
        if (clock.nowMillis() < end) return
        pauseEnd = null
        nextIsProbe = true
    }

    private fun submitMissing() {
        if (pauseEnd != null || probe != null) return
        if (nextIsProbe) {
            val file = missing.pollFirst() ?: return
            nextIsProbe = false
            probe = file
            submit(file)
            return
        }
        while (open < MOST_FILES_AT_ONCE && missing.isNotEmpty()) submit(missing.removeFirst())
    }

    private fun submit(file: RepositoryFile) {
        open++
        workers.execute { outcomes.put(fetchOutcome(file)) }
    }

    private fun waitMillis(): Long {
        val end = pauseEnd ?: return LOOK_UP_MILLIS
        return (end - clock.nowMillis()).coerceIn(1, LOOK_UP_MILLIS)
    }

    private fun take(outcome: FileOutcome) {
        open--
        val wasProbe = outcome.file == probe
        if (wasProbe) probe = null
        when (outcome) {
            is FileOutcome.Arrived -> Unit
            is FileOutcome.Failed -> throw outcome.exception
            is FileOutcome.Refused -> {
                // A refusal right after a whole pause is not a limit of this download alone, and waiting again would not end it
                if (wasProbe) throw ModelInstallException(InstallFailure.RateLimited)
                pauseFor(outcome)
            }
        }
    }

    // The refused file is requested again after the pause. A second refusal, of a request that was open before the pause
    // began, moves the end of the pause to the later of the two
    private fun pauseFor(outcome: FileOutcome.Refused) {
        missing.addFirst(outcome.file)
        val pauseMillis = rateLimitPause(outcome.answer.retryAfter, outcome.answer.rateLimit) * MILLIS_PER_SECOND
        pauseEnd = maxOf(pauseEnd ?: 0L, clock.nowMillis() + pauseMillis)
    }

    private fun showProgress() {
        val total = repositoryFiles.sizeInBytes
        indicator.fraction = (receivedBytes.get().toDouble() / total).coerceAtMost(1.0)
        val end = pauseEnd
        if (end == null) {
            indicator.text = "Downloading $progressName"
            indicator.text2 = "${megabytes(receivedBytes.get())} of ${megabytes(total)} MB"
            return
        }
        val secondsLeft = (end - clock.nowMillis() + MILLIS_PER_SECOND - 1).coerceAtLeast(0) / MILLIS_PER_SECOND
        indicator.text = RATE_LIMITED_TEXT
        indicator.text2 = rateLimitCountdownText(secondsLeft)
    }

    // Runs on a worker. Whatever goes wrong with the file is handed back as an outcome, and none is thrown on the thread
    private fun fetchOutcome(file: RepositoryFile): FileOutcome = try {
        fetchAndCheck(file)
        FileOutcome.Arrived(file)
    } catch (answer: RateLimitedAnswer) {
        FileOutcome.Refused(file, answer)
    } catch (_: Stopped) {
        // Nobody reads the outcome of a download that was stopped
        FileOutcome.Arrived(file)
    } catch (exception: Exception) {
        FileOutcome.Failed(file, exception)
    }

    private fun fetchAndCheck(file: RepositoryFile) {
        val target = targetFolder.resolve(file.path)
        try {
            Files.createDirectories(target.parent)
        } catch (exception: IOException) {
            throw ioFailure(exception, targetFolder, repositoryFiles)
        }
        val address = repositoryFiles.fileAddress(file)
        val fetch = FileFetch(address, target, file.sizeInBytes, repositoryFiles, ::failIfStopped) { count, _ ->
            receivedBytes.addAndGet(count.toLong())
        }
        val fetched = fetchFile(fetch, resolveRedirect)
        if (fetched.receivedBytes != file.sizeInBytes || fetched.sha256 != file.sha256) {
            throw ModelInstallException(InstallFailure.ChecksumMismatch(address))
        }
    }

    private fun failIfStopped() {
        if (stop.get()) throw Stopped()
    }
}
