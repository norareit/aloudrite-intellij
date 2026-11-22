package com.ritenoar.aloudrite

import com.intellij.ide.ActivityTracker
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.concurrency.AppExecutorUtil
import com.ritenoar.aloudrite.core.AudioPlayer
import com.ritenoar.aloudrite.core.ENGINE_DEBUG_PROPERTY
import com.ritenoar.aloudrite.core.FirstSoundAfter
import com.ritenoar.aloudrite.core.KokoroSpeechEngine
import com.ritenoar.aloudrite.core.MILLISECONDS_PER_SECOND
import com.ritenoar.aloudrite.core.NANOSECONDS_PER_MILLISECOND
import com.ritenoar.aloudrite.core.ReadingAloud
import com.ritenoar.aloudrite.core.SpeechEngine
import com.ritenoar.aloudrite.core.SpeechModel
import com.ritenoar.aloudrite.core.WORD_DELAY_PROPERTY
import com.ritenoar.aloudrite.core.engineThreadCount
import com.ritenoar.aloudrite.core.isEngineDebugOutputOn
import com.ritenoar.aloudrite.core.minutesAndSeconds
import com.ritenoar.aloudrite.core.userModelsFolder
import com.ritenoar.aloudrite.core.wordDelayMilliseconds
import java.util.concurrent.TimeUnit

// The reading, with the parts of the IDE that it is handed: the thread pool, the scheduler, the settings, the reports of
// failures and the notice of a change
@Service(Service.Level.APP)
class SpeechService(val reading: ReadingAloud<ReadingTarget>) : Disposable {

    constructor() : this(ReadingAloud(
        { work -> ApplicationManager.getApplication().executeOnPooledThread(work) },
        ::createKokoroEngine,
        { sampleRate, onFirstWrite -> AudioPlayer.open(sampleRate, onFirstWrite) },
        ::logFirstSound,
        ::logReadingTime,
        ::logAndShowFailure,
        {
            ActivityTracker.getInstance().inc()
            publishReadingChange()
        },
        { service<ReadingSpeedSetting>().speed },
        { service<VoiceSetting>().voice },
        { service<SpeechModelSetting>().modelInUse },
        { System.nanoTime() / NANOSECONDS_PER_MILLISECOND },
        { delayMillis, work ->
            AppExecutorUtil.getAppScheduledExecutorService().schedule(Runnable { work() }, delayMillis, TimeUnit.MILLISECONDS)
        },
        pieceAligningOfTheIde(),
        { wordDelayMilliseconds(System.getProperty(WORD_DELAY_PROPERTY)) },
    ))

    fun projectClosed(project: Project?) {
        reading.forgetWhere { it.project == project }
    }

    override fun dispose() {
        reading.close()
    }

    private companion object {
        val LOG = Logger.getInstance(SpeechService::class.java)

        fun logReadingTime(target: ReadingTarget, firstEstimateSeconds: Double?, tookMillis: Long) {
            val estimated = firstEstimateSeconds?.let(::minutesAndSeconds) ?: "not at all"
            LOG.info("Read ${target.file.name} to the end in ${minutesAndSeconds(tookMillis / MILLISECONDS_PER_SECOND.toDouble())}, " +
                "first estimated at $estimated")
        }

        fun logFirstSound(milliseconds: Long, after: FirstSoundAfter) {
            val cause = when (after) {
                FirstSoundAfter.READ_ALOUD -> "the button was pressed"
                FirstSoundAfter.RESUME -> "Resume Reading was pressed"
                FirstSoundAfter.SPEED_CHANGE -> "the speed was changed"
            }
            LOG.info("First sound $milliseconds ms after $cause")
        }

        // The processors are asked at each creation, so that a limit on the IDE's process, such as that of a
        // container, is followed
        fun createKokoroEngine(model: SpeechModel): SpeechEngine {
            val processors = Runtime.getRuntime().availableProcessors()
            val threads = engineThreadCount(processors)
            val engine = KokoroSpeechEngine(
                model.folderIn(userModelsFolder()), model.modelFileName, threads,
                isEngineDebugOutputOn(System.getProperty(ENGINE_DEBUG_PROPERTY)))
            LOG.info("Created the speech engine of ${model.displayName} with $threads threads, of $processors processors available")
            return engine
        }

        // A warning is not an error report of the IDE, and an error would be one
        fun logAndShowFailure(target: ReadingTarget, failure: Throwable) {
            LOG.warn("AloudRite could not read ${target.file.name} aloud", failure)
            ApplicationManager.getApplication().invokeLater {
                val project = target.project?.takeUnless { it.isDisposed }
                Messages.showErrorDialog(
                    project, "AloudRite could not read ${target.file.name} aloud: ${failure.message}", "AloudRite")
            }
        }
    }
}
