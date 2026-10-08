package com.ritenoar.aloudrite

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class SayHelloAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        Messages.showInfoMessage(e.project, "Hello, world!", "AloudRite")
    }
}
