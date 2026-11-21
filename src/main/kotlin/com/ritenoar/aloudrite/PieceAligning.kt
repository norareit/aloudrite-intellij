package com.ritenoar.aloudrite

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.RejectedExecutionException

// Aligns the audio of a piece with its words, off the threads of the engine and of the player, so that a reading never waits
// for it. The collaborators are handed in so that a test can run it without the model and without a thread
class PieceAligning(
    private val isModelInPlace: () -> Boolean,
    private val createAligner: () -> WordAligner,
    private val runOnItsThread: (() -> Unit) -> Unit,
    private val logFailure: (Throwable) -> Unit,
) : AutoCloseable {

    constructor() : this(
        { ALIGNER_MODEL.isInPlace(ALIGNER_MODEL.folderIn(userModelsFolder())) },
        { ForcedWordAligner(ALIGNER_MODEL.folderIn(userModelsFolder())) },
        { work -> runOnTheAlignmentThread(work) },
        { failure -> LOG.warn("AloudRite could not align the words of a piece with its audio", failure) },
    )

    // Both are used only while holding the monitor, so that a close never releases an aligner that aligns
    private var aligner: WordAligner? = null
    private var closed = false

    // Calls back with the alignment of the piece, or with nothing where the aligner model is not in place, where there is
    // nothing to align, where the aligner failed and where it did not place the words. It calls back on its own thread
    fun align(samples: FloatArray, sampleRate: Int, partsOfWords: List<List<String>>, onAligned: (PieceAlignment?) -> Unit) {
        if (partsOfWords.all { it.isEmpty() }) return onAligned(null)
        try {
            runOnItsThread { onAligned(alignOnItsThread(samples, sampleRate, partsOfWords)) }
        } catch (exception: RejectedExecutionException) {
            onAligned(null)
        }
    }

    private fun alignOnItsThread(samples: FloatArray, sampleRate: Int, partsOfWords: List<List<String>>): PieceAlignment? =
        synchronized(this) {
            try {
                val wordAligner = alignerWhereTheModelIsInPlace() ?: return null
                val intervals = wordAligner.align(samples, sampleRate, partsOfWords.flatten())
                wordStartsOf(partsOfWords, intervals, sampleRate, samples.size)
            } catch (failure: Throwable) {
                logFailure(failure)
                null
            }
        }

    // Where the model is gone, as after a removal, the aligner that was made from it goes too
    private fun alignerWhereTheModelIsInPlace(): WordAligner? {
        if (closed) return null
        if (!isModelInPlace()) {
            aligner?.close()
            aligner = null
            return null
        }
        return aligner ?: createAligner().also { aligner = it }
    }

    override fun close() = synchronized(this) {
        closed = true
        aligner?.close()
        aligner = null
    }

    private companion object {
        val LOG = Logger.getInstance(PieceAligning::class.java)

        // One thread, which the aligner is made and used on
        val EXECUTOR by lazy { AppExecutorUtil.createBoundedApplicationPoolExecutor("AloudRite alignment", 1) }

        fun runOnTheAlignmentThread(work: () -> Unit) {
            EXECUTOR.execute(work)
        }
    }
}
