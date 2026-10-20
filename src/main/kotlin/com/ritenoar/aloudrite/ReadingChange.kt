package com.ritenoar.aloudrite

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

// Told of every start and every end of the one reading of the IDE, on whichever thread the change happened
fun interface ReadingChangeListener {
    fun readingChanged()
}

val READING_CHANGED: Topic<ReadingChangeListener> =
    Topic.create("AloudRite reading changed", ReadingChangeListener::class.java)

fun publishReadingChange() {
    ApplicationManager.getApplication().messageBus.syncPublisher(READING_CHANGED).readingChanged()
}
