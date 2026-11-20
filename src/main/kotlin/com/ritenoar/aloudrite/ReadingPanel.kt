package com.ritenoar.aloudrite

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapManagerListener
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.panel.ComponentPanelBuilder
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.InplaceButton
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.NamedColorUtil
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Insets
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import kotlin.math.roundToInt
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.Timer
import javax.swing.event.DocumentEvent
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.View

// What the tool window of a project shows of the one reading of the IDE, the speed, and the model in use, in three groups
class ReadingPanel(
    private val ownProject: Project,
    private val reading: () -> CurrentReading,
    private val setting: () -> ReadingSpeedSetting,
    private val modelInUse: () -> SpeechModel,
    private val openTheSettings: () -> Unit,
) : JPanel(VerticalLayout(JBUI.scale(GROUP_GAP))) {

    internal val nothingReadLabel = JBLabel(NOTHING_READ_TEXT)
    internal val fileLabel = JBLabel()
    // Says what part of the file is read, in a line of its own under the name of the file
    internal val partLabel = JBLabel()
    internal val projectLabel = JBLabel()
    // The time left and the time of the whole, which the timer keeps up to date under the share of the text read. Both
    // are marked as estimates, since the whole moves as the audio is made
    internal val timeLeftLabel = commentLabel()
    internal val wholeTimeLabel = commentLabel()
    internal val progressBar = JProgressBar(0, PROGRESS_STEPS)
    internal val refreshTimer = Timer(REFRESH_DELAY_MILLISECONDS) { showTheTimes() }
    internal val speedSeparator = titledSeparator(SPEED_LABEL_TEXT)
    internal val slowerButton = iconButton(AllIcons.General.Remove) {
        setting().slower()
        showTheSpeed()
    }
    internal val fasterButton = iconButton(AllIcons.General.Add) {
        setting().faster()
        showTheSpeed()
    }

    // The sign is the unit of the number, so it sits inside the field in the grey of a hint and does nothing on a click.
    // The remark on a change of the speed is its tooltip, since it matters at the moment of a change only
    internal val speedField = ExtendableTextField(SPEED_FIELD_COLUMNS).apply {
        addExtension(ExtendableTextComponent.Extension.create(TimesSign(), TimesSign(), null, null))
        toolTipText = SPEED_COMMENT_TEXT
    }

    // The model is chosen, downloaded and removed on the settings page, which the link opens. The name is HTML so that
    // a longer one wraps at the width of the pane and is never cut
    internal val modelSeparator = titledSeparator(MODEL_LABEL_TEXT)
    internal val modelNameLabel = JBLabel()
    internal val settingsLink = ActionLink(SETTINGS_LINK_TEXT) { openTheSettings() }

    // Registered on the field, so that Escape there puts the speed back rather than moving the focus to the editor,
    // as Escape in a tool window does
    internal val escapeInTheField: AnAction = DumbAwareAction.create { showTheSpeed() }

    // The layout gives every child the width of its parent, which the bar needs to span the pane and the labels need to
    // wrap in a narrow one
    internal val timesLine = JPanel(BorderLayout()).apply {
        isOpaque = false
        add(timeLeftLabel, BorderLayout.WEST)
        add(wholeTimeLabel, BorderLayout.EAST)
    }

    internal val timesRow = JPanel(VerticalLayout(JBUI.scale(ROW_GAP))).apply {
        isOpaque = false
        isVisible = false
        add(progressBar)
        add(timesLine)
    }

    internal val speedRow = JPanel(HorizontalLayout(JBUI.scale(GAP))).apply {
        isOpaque = false
        add(slowerButton)
        add(speedField)
        add(fasterButton)
    }

    // The link stays at the top of the row when the name wraps to more lines
    private val settingsLinkColumn = JPanel(BorderLayout()).apply {
        isOpaque = false
        add(settingsLink, BorderLayout.NORTH)
    }

    // A layout asks for the height of a row before it gives the row its width, and a label of HTML reports one line
    // until it has its width. The row is as high as its name is at the width the pane gives it, which is known when the
    // pane lays out, so that the wrapped name is never cut
    internal val modelRow = object : JPanel(BorderLayout(JBUI.scale(GAP), 0)) {
        override fun getPreferredSize(): Dimension =
            withTheHeightOfTheWrappedName(super.getPreferredSize(), modelNameLabel, settingsLinkColumn.preferredSize.width)
    }.apply {
        isOpaque = false
        add(modelNameLabel, BorderLayout.CENTER)
        add(settingsLinkColumn, BorderLayout.EAST)
    }

    internal val readingGroup = groupOf(nothingReadLabel, fileLabel, partLabel, projectLabel, timesRow)
    internal val speedGroup = groupOf(speedSeparator, speedRow)
    internal val modelGroup = groupOf(modelSeparator, modelRow)

    // What the labels show, so that the times are those of the reading the user saw
    private var shownTarget: ReadingTarget? = null
    private var typedSpeedRefused = false

    init {
        border = JBUI.Borders.empty(GAP)
        // The tool window focuses its preferred component when it opens, which is this panel, so that the field of
        // the speed does not light up as the first control that could take the focus
        isFocusable = true
        add(readingGroup)
        add(speedGroup)
        add(modelGroup)
        speedField.addActionListener { setTheTypedSpeed() }
        speedField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(event: FocusEvent) = setTheTypedSpeed()
        })
        escapeInTheField.registerCustomShortcutSet(CommonShortcuts.ESCAPE, speedField)
        showTheSpeed()
        showTheModel()
        installTheTooltips()
    }

    // A reading ends on a pooled thread, so the change is handed to the EDT. Any modality, so that the panel follows
    // also while a dialog is open, such as the message of a failure, since setting a label touches no model of the IDE.
    // The speed and the keymap of the tooltips are followed the same way, and the mark of a refused speed needs the same
    // disposable
    fun followTheReading(parentDisposable: Disposable) {
        refreshTimer.start()
        Disposer.register(parentDisposable) { refreshTimer.stop() }
        val connection = ApplicationManager.getApplication().messageBus.connect(parentDisposable)
        connection.subscribe(
            READING_CHANGED,
            ReadingChangeListener {
                ApplicationManager.getApplication().invokeLater(::showTheCurrentReading, ModalityState.any())
            },
        )
        connection.subscribe(
            READING_SPEED_CHANGED,
            ReadingSpeedChangeListener {
                ApplicationManager.getApplication().invokeLater(::showTheSpeed, ModalityState.any())
            },
        )
        connection.subscribe(
            KeymapManagerListener.TOPIC,
            object : KeymapManagerListener {
                override fun activeKeymapChanged(keymap: Keymap?) = installTheTooltipsLater()

                override fun shortcutsChanged(keymap: Keymap, actionIds: Collection<String>, fromSettings: Boolean) =
                    installTheTooltipsLater()
            },
        )
        connection.subscribe(
            SPEECH_MODEL_CHANGED,
            SpeechModelChangeListener {
                ApplicationManager.getApplication().invokeLater(::showTheModel, ModalityState.any())
            },
        )
        ComponentValidator(parentDisposable)
            .withValidator { if (typedSpeedRefused) ValidationInfo(REFUSED_SPEED_TEXT, speedField) else null }
            .installOn(speedField)
        // While the field is marked, every edit looks again, so that the mark goes once the text is a speed
        speedField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(event: DocumentEvent) {
                if (typedSpeedRefused && parsedSpeed(speedField.text) != null) markTheTypedSpeed(refused = false)
            }
        })
        showTheCurrentReading()
    }

    fun showTheCurrentReading() {
        val target = reading().currentTarget()
        val paused = target != null && reading().isPaused(target)
        shownTarget = target
        val otherProject = target?.project?.takeUnless { it === ownProject }
        nothingReadLabel.isVisible = target == null
        fileLabel.isVisible = target != null
        fileLabel.text = target?.let { if (paused) "Paused reading ${it.file.name}" else "Reading ${it.file.name}" }
        val partText = target?.let { reading().partOfTheFile(it) }?.let(::textOfThePart)
        partLabel.isVisible = partText != null
        partLabel.text = partText
        projectLabel.isVisible = otherProject != null
        projectLabel.text = otherProject?.let { "In the project ${it.name}" }
        showTheTimes()
        installTheTooltips()
        revalidate()
        repaint()
    }

    // Called by the timer, and by the change of the reading, so that the row is there as soon as a reading starts. The
    // bar is the share of the text read, which only grows, so it is not rounded back
    internal fun showTheTimes() {
        val times = shownTarget?.let { reading().timesOf(it) }
        timesRow.isVisible = times != null
        if (times == null) return
        timeLeftLabel.text = "About ${minutesAndSeconds(times.leftSeconds)} left"
        wholeTimeLabel.text = "${minutesAndSeconds(times.wholeSeconds)} in all"
        progressBar.value = (times.shareRead * PROGRESS_STEPS).roundToInt()
    }

    private fun textOfThePart(partOfTheFile: PartOfTheFile): String? = when (partOfTheFile) {
        PartOfTheFile.WHOLE_FILE -> null
        PartOfTheFile.SELECTION -> SELECTION_TEXT
        PartOfTheFile.FROM_THE_CARET -> FROM_THE_CARET_TEXT
    }

    // Each names the shortcut its button shares with an action of the keymap, as the keymap in use has it
    private fun installTheTooltips() {
        shortcutTooltip(SLOWER_TEXT, ReadSlowerAction.ID).installOn(slowerButton)
        shortcutTooltip(FASTER_TEXT, ReadFasterAction.ID).installOn(fasterButton)
    }

    private fun installTheTooltipsLater() {
        ApplicationManager.getApplication().invokeLater(::installTheTooltips, ModalityState.any())
    }

    // Also what Escape in the field does, which drops what was typed and its mark
    private fun showTheSpeed() {
        val speed = setting().speed
        speedField.text = speed.number
        slowerButton.isActive = speed.canGoSlower
        fasterButton.isActive = speed.canGoFaster
        markTheTypedSpeed(refused = false)
    }

    // The height of a row whose name is HTML and wraps at the width the pane leaves it beside another component of the
    // given width, and the base size where the name has no view yet or no width is left
    private fun withTheHeightOfTheWrappedName(base: Dimension, nameLabel: JBLabel, widthBesideTheName: Int): Dimension {
        val paneWidth = width - insets.left - insets.right
        val nameWidth = paneWidth - widthBesideTheName - JBUI.scale(GAP)
        val view = nameLabel.getClientProperty(BasicHTML.propertyKey) as? View
        if (view == null || nameWidth <= 0) return base
        view.setSize(nameWidth.toFloat(), 0f)
        return Dimension(base.width, maxOf(base.height, view.getPreferredSpan(View.Y_AXIS).roundToInt()))
    }

    private fun showTheModel() {
        modelNameLabel.text = "<html>${StringUtil.escapeXmlEntities(modelInUse().displayName)}</html>"
    }

    private fun setTheTypedSpeed() {
        val typed = parsedSpeed(speedField.text) ?: return markTheTypedSpeed(refused = true)
        setting().set(typed, afterAWait = false)
        showTheSpeed()
    }

    private fun markTheTypedSpeed(refused: Boolean) {
        typedSpeedRefused = refused
        ComponentValidator.getInstance(speedField).ifPresent { it.revalidate() }
    }

    // An inactive button shows its icon greyed and ignores a click, as the disabled buttons of the IDE do. It has no
    // plain tooltip, so that only the one of installTheTooltips shows
    private fun iconButton(icon: Icon, onPress: () -> Unit): InplaceButton {
        lateinit var button: InplaceButton
        button = InplaceButton(IconButton(null, icon, icon, IconLoader.getDisabledIcon(icon))) {
            if (button.isActive) onPress()
        }
        // The button centers its icon in a size of its own, which would put the icon right of the left edge of the pane
        button.preferredSize = Dimension(icon.iconWidth, icon.iconHeight)
        return button
    }

    private fun groupOf(vararg rows: JComponent): JPanel = JPanel(VerticalLayout(JBUI.scale(ROW_GAP))).apply {
        isOpaque = false
        for (row in rows) add(row)
    }

    // The IDE's comment under a control: its smaller font and its grey, which follow the theme. Not the wrapping one,
    // which is laid out with no text while nothing is read and keeps that height of 0 once the times are set
    private fun commentLabel(): JLabel =
        ComponentPanelBuilder.createNonWrappingCommentComponent("").apply { toolTipText = ESTIMATE_TOOLTIP_TEXT }

    // The label of the separator keeps no inset on its left, so that the title starts at the edge of the pane
    private fun titledSeparator(title: String): TitledSeparator = TitledSeparator(title).apply {
        val insets = label.border?.getBorderInsets(label) ?: Insets(0, 0, 0, 0)
        label.border = JBUI.Borders.empty(insets.top, 0, insets.bottom, insets.right)
    }

    // The "×" after the number, painted in the grey of a hint with the font of the field
    internal class TimesSign : Icon {
        val text = TIMES_TEXT
        private val font = UIUtil.getLabelFont()
        private val metrics = JBLabel().getFontMetrics(font)

        override fun paintIcon(component: Component?, graphics: Graphics, x: Int, y: Int) {
            graphics.font = font
            graphics.color = NamedColorUtil.getInactiveTextColor()
            graphics.drawString(text, x, y + metrics.ascent)
        }

        override fun getIconWidth(): Int = metrics.stringWidth(text)

        override fun getIconHeight(): Int = metrics.height
    }

    internal companion object {
        const val TIMES_TEXT = "×"
        const val NOTHING_READ_TEXT =
            "<html>Nothing is being read.<br>Start a reading with \"Read Aloud\" on the tab of a Markdown file.</html>"
        const val SELECTION_TEXT = "Selection"
        const val FROM_THE_CARET_TEXT = "From the caret"
        const val ESTIMATE_TOOLTIP_TEXT = "Estimated from the text. It gets more exact as the audio is made."
        const val SLOWER_TEXT = "Slower"
        const val FASTER_TEXT = "Faster"
        const val SPEED_LABEL_TEXT = "Reading speed"
        const val MODEL_LABEL_TEXT = "Speech model"
        const val SPEED_COMMENT_TEXT =
            "A change starts the sentence that is playing again at the new speed. This can take a few seconds."
        const val REFUSED_SPEED_TEXT = "Type a speed from 0.75 to 2.0"
        // Room for "0.75" and for the "×" inside the field, which the columns of a text field do not count. Five
        // columns cut off the first digit of a speed with two decimals
        const val SPEED_FIELD_COLUMNS = 6
        const val SETTINGS_LINK_TEXT = "Settings\u2026"
        const val GAP = 8

        // Between two rows of a group, which is less than the gap between two groups
        const val ROW_GAP = 6
        const val GROUP_GAP = 14
        const val PROGRESS_STEPS = 1000

        // At least once a second, as the times are to be
        const val REFRESH_DELAY_MILLISECONDS = 500
    }
}
