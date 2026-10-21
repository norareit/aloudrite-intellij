package com.ritenoar.aloudrite

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.messages.Topic

// Told of every change of the speed, on whichever thread it was set
fun interface ReadingSpeedChangeListener {
    fun speedChanged(speed: ReadingSpeed)
}

val READING_SPEED_CHANGED: Topic<ReadingSpeedChangeListener> =
    Topic.create("AloudRite reading speed changed", ReadingSpeedChangeListener::class.java)

// The one speed of the IDE, kept across restarts. The collaborator is handed in so that a test can set the speed
// without the service of the reading and without the bus of the application
@Service(Service.Level.APP)
@State(name = "AloudRiteReadingSpeed", storages = [Storage("aloudrite.xml")])
class ReadingSpeedSetting(
    private val onChange: (speed: ReadingSpeed, afterAWait: Boolean) -> Unit,
) : PersistentStateComponent<ReadingSpeedSetting.StoredSpeed> {

    constructor() : this({ speed, afterAWait ->
        service<SpeechService>().changeSpeed(speed, afterAWait)
        ApplicationManager.getApplication().messageBus.syncPublisher(READING_SPEED_CHANGED).speedChanged(speed)
    })

    class StoredSpeed {
        var hundredths: Int = ReadingSpeed.NORMAL.hundredths
    }

    @Volatile
    var speed: ReadingSpeed = ReadingSpeed.NORMAL
        private set

    // A set to the speed that is current already is no change, so that it does not make the audio of a reading again
    fun set(newSpeed: ReadingSpeed, afterAWait: Boolean) {
        synchronized(this) {
            if (newSpeed == speed) return
            speed = newSpeed
        }
        onChange(newSpeed, afterAWait)
    }

    override fun getState(): StoredSpeed = StoredSpeed().also { it.hundredths = speed.hundredths }

    // The file can be edited by hand, so a value outside the range is taken as the speed before one was chosen
    override fun loadState(state: StoredSpeed) {
        speed = ReadingSpeed.ofHundredths(state.hundredths) ?: ReadingSpeed.NORMAL
    }
}
