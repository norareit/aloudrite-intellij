# Pause and resume

## Approach

A reading gets a third state beside "read" and "not read": paused. `SpeechService` keeps the paused reading as its
current reading, so everything unit 0006 and unit 0015 built on `current` (one reading at a time, the end at closing
the tab or the project, at a press on another file and at a failure) holds for a paused reading without new code.
What is new is in four places.

**The audio knows its pieces.** The engine work puts `AudioRun`s into `AudioAhead` in place of bare sample arrays.
A run carries the number of the segment it belongs to and whether that segment is a silence. A segment is one
non-blank piece of `piecesOf`, whose samples may come from the engine in several runs, or one `Silence` part, which
is put as one run. The engine work numbers the segments in the order it makes them.

**The playing remembers what it may have to play again.** A new class, `PlayedAudio`, holds the runs the playing
work has taken from `AudioAhead` and written to the current player, with the offset in that player's samples at which
each segment begins. It forgets a segment once the player has made all of it audible. At a pause it answers, from
the number of samples the player has made audible, where the resume begins:

* the segment that holds that position is the one being heard. Where the position is at or past the end of what was
  written, which is when the engine had not kept up and the line ran dry, it is the last segment written
* where that segment is a piece, the resume plays that segment's runs and every run after it again (R3)
* where it is a silence, the resume plays the runs from that silence on, but drops silence runs until the first run
  of a piece, also those still to come from `AudioAhead`. So the rest of the silence, and a silence directly after
  it, is not played, and the reading goes on at the piece after it (R3). A pause in the silence at the very end of
  a file, after a closing code block, then resumes into the end of the file, and the reading ends as in unit 0006
  (R11)

The position comes from the player, not from what was written, because the audio line holds up to about half a second
that is written and not heard. Taking it from what was written would, at a pause just after a piece boundary, resume
at the next piece and lose the end of the sentence the listener was hearing. `SamplePlayer` therefore gets
`samplesHeard(): Long`, which `AudioPlayer` answers with `line.longFramePosition`, the frames of the one channel
being the samples.

A resume plays the kept runs before it takes anything new, so it never waits for the engine and the first sound
comes as soon as a new line is open (R4). While a reading is paused the engine work goes on until `AudioAhead` holds
its 30 seconds and then waits in `put`, which is the memory the spec's decision on the time limit counts.

**The playing work ends at a pause and a resume starts a new one.** The `Reading` of the service gets `paused`, the
kept resumption, and whether a playing work is running, all under its own monitor as `stopped` is now. A pause sets
`paused`; the playing work sees it before its next slice, as it sees a stop, or right after `take` returns, `null`
included, so that the end of the text made while paused does not end a paused reading. It then reads
`samplesHeard()`, discards its player, which silences the line at once (R2), stores the resumption from `PlayedAudio`
and ends, unless a resume came in the meantime, in which case it opens a new player and plays the resumption itself.
A resume clears `paused` and hands a new playing work to `runInBackground` only where no playing work is running. A
pause that comes while the last audio plays out in `close` discards the line from the pressing thread, as a stop does,
and takes the position before that, and the playing work then stores the resumption as above instead of ending the
reading. An exception of the player after a pause is the discarded line and not a failure. A failure of the engine
while paused ends the reading and is reported as before (R12). A stop, a closed tab or project, a press on another
file and `dispose` end a paused reading through the existing `stop`, which cancels `AudioAhead` and so lets the
engine work go. The first sound after a resume is logged as such, measured from the press of "Resume", so that the
developer's check of R4 has a number beside what they heard.

Ending the playing work at a pause, rather than having it wait, keeps the stand-ins of unit 0006 usable as they are:
their background works run one after another on the test thread, and a work that waited for a resume there would
never return.

**Two tab buttons on one background.** In IntelliJ IDEA 2026.2.3 the tab row draws `EditorTabsToolbarActions` in an
`ActionToolbar` made by `ActionManager.createActionToolbar`, which shows the component of an action that implements
`CustomComponentAction`. A non-popup action group would be flattened into the row, so the plugin adds one action,
`ReadingButtonsAction`, to `EditorTabsToolbarActions`. It is visible on the tab of a Markdown file only, and its
component, `ReadingButtonsPanel`, paints a rounded background and holds a toolbar of its own over the new group
`com.ritenoar.aloudrite.ReadingButtonGroup`, which holds `ReadAloudAction` and the new `StopReadingAction`. The inner
toolbar's target component is the panel, so that its data context is found up the component tree in the tab row,
which gives the file of the selected tab as it does today. It is made as the tab row makes its own: no border, not
opaque, `ToolbarLayoutStrategy.NOWRAP_STRATEGY`. It updates on `ActivityTracker`, which `SpeechService` already
increments at every change. The background is `JBUI.CurrentTheme.ActionButton.hoverBackground()`, the grey of the
drawings in `icons.html`, looked up at every paint, so it follows a change of theme (R6). Since that is also the
color a button shows on hover, the inner toolbar gets a button look, through `setCustomButtonLook`, that paints the
hover in `JBUI.CurrentTheme.ActionButton.pressedBackground()` and leaves the rest of `ActionButtonLook` as it is.
This was read from the classes of the platform and not seen running; the developer's check of R15 is where it is seen.

`ReadAloudAction` shows "Read Aloud" with the person, "Pause Reading" with `AllIcons.Actions.Pause` or "Resume
Reading" with the person and line, by the state of its file's reading, and a press reads, pauses or resumes.
`StopReadingAction` is visible only on the tab of the file that is read or paused, shows "Stop Reading" with
`AllIcons.Actions.Suspend`, the stop of unit 0006, and stops. The text of each is its tooltip, as today.

The icons are drawn from the functions `person` and `personline` of `icons.html`, with its colors: the green person
strokes `#208A3C` over a fill of `#F2FCF3` in the light theme and `#57965C` over `#253627` in the dark one; the grey
person of the tool window strokes `#6C707E` and `#CED0D6`, with no fill, as the platform's own tool window icons.
They are 16 by 16, as the current icon. An object `AloudRiteIcons` loads the three, so that the code and the tests
refer to one constant each.

**The tool window.** `ReadingPanel` shows the reading as now, with "Reading notes.md" while it plays and "Paused
reading notes.md" while it is paused, and a row of two buttons, "Pause" or "Resume", then "Stop". The two act on the
reading the panel shows, as the stop does today.

`Readings` and `CurrentReading` keep `isReading` and `currentTarget`, which now mean that a reading exists, playing or
paused, and gain `isPaused(target)`, `pause(target)` and `resume(target, pressedAtNanos)`. A pause or resume of a
target that is not the current reading, or of a reading not in the state it needs, does nothing, as `stop` does.
Each change of state calls `onChange`, so the tab buttons and every tool window follow (R8, R11).

There is no end-to-end suite that drives an IDE window, as unit 0003 decided; the developer's checks of R14 and R15
are the end-to-end coverage. The unit changes neither the engine, nor sherpa-onnx, nor the text handed to the engine,
so the tests with the real engine are not required to run here.

## Files

- `src/main/resources/icons/readAloud.svg`, `readAloud_dark.svg`: the green person in place of the loudspeaker
- `src/main/resources/icons/resumeReading.svg`, `resumeReading_dark.svg`: new, the green person with a line
- `src/main/resources/icons/toolWindow.svg`, `toolWindow_dark.svg`: new, the grey person
- `src/main/kotlin/com/ritenoar/aloudrite/AloudRiteIcons.kt`: new, the three icons
- `src/main/kotlin/com/ritenoar/aloudrite/AudioAhead.kt`: holds `AudioRun`s
- `src/main/kotlin/com/ritenoar/aloudrite/PlayedAudio.kt`: new, what a resume plays again
- `src/main/kotlin/com/ritenoar/aloudrite/SamplePlayer.kt`, `AudioPlayer.kt`: `samplesHeard()`
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: segments, pause, resume, the interfaces
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAloudAction.kt`: three states, pause and resume
- `src/main/kotlin/com/ritenoar/aloudrite/StopReadingAction.kt`: new, the stop on the tab
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingButtonsAction.kt`: new, the action of the tab row, its panel with
  the background and the button look
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingPanel.kt`: the paused text and the pause or resume button
- `src/main/resources/META-INF/plugin.xml`: the stop action, the group, the action of the tab row in
  `EditorTabsToolbarActions` in place of `ReadAloud`, the tool window's icon
- `src/test/kotlin/com/ritenoar/aloudrite/IconsTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/PlayedAudioTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/AudioAheadTest.kt`, `AudioPlayerTest.kt`: runs, `samplesHeard()`
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechStandIns.kt`: a player that lags in what it has made audible, and the
  log of the first sound after a resume
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechServiceTest.kt`: pause and resume
- `src/test/kotlin/com/ritenoar/aloudrite/ButtonPressSupport.kt`: the stop action and its presentation
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAloudActionTest.kt`: the states of both tab buttons
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingButtonsTest.kt`: new, the action of the tab row and its background
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingPanelTest.kt`, `ReadingToolWindowTest.kt`: the controls and the icon
- `README.md`: the icons, the pause, the resume, the stop on the tab and the controls in the tool window

## Tasks

### The icons

- [x] Write `IconsTest`: each of the six SVGs is 16 by 16; `readAloud` and `resumeReading` stroke `#208A3C` and fill
  `#F2FCF3`, their `_dark` files stroke `#57965C` and fill `#253627`; `toolWindow` strokes `#6C707E`, its `_dark`
  file `#CED0D6`, and neither fills; and `AloudRiteIcons` loads each from its path. Change the expected icon in
  `ReadingToolWindowTest.thePluginRegistersOneToolWindowDockedOnTheRightThatDoesNotOpenByItself` to
  `/icons/toolWindow.svg` (R1, R3, R8, R13)
- [x] Draw the six SVGs from `person` and `personline` of `icons.html` with the colors above, replacing the
  loudspeaker in `readAloud.svg` and `readAloud_dark.svg`; add `AloudRiteIcons` with `ReadAloud`, `ResumeReading` and
  `ToolWindow`; set the tool window's `icon` in `plugin.xml` to `/icons/toolWindow.svg` (R1, R3, R7, R8)

### The audio of a reading

- [x] Change `AudioAheadTest` to put and take `AudioRun`s, keeping each existing case, and add that the limit counts
  the samples of the runs (R3)
- [x] Add `AudioRun(segment: Int, isSilence: Boolean, samples: FloatArray)` to `AudioAhead.kt` and make `AudioAhead`
  hold runs (R3)
- [x] Write `PlayedAudioTest` with plain runs: a position inside a piece resumes at the first run of that piece and
  keeps every later run; a position inside a later run of a piece of several runs resumes at its first run; a
  position past what was written resumes at the last segment written; a position inside a silence keeps the runs from
  the silence on and says that silence is to be skipped; a segment that the player has made audible in full is
  forgotten, and one it has made audible in part is kept; nothing written gives an empty resumption that skips
  nothing; after a resumption, offsets start again at zero (R3)
- [x] Write `PlayedAudio` to pass it. It does no IO and takes the position as a value (R3)
- [x] Add to `AudioPlayerTest`, with its stand-in line, that `samplesHeard()` is the line's `longFramePosition`; add
  `samplesHeard()` to `SamplePlayer` and `AudioPlayer`, and to `StandInPlayer` as the samples played less a lag the
  test sets, at least zero (R3)

### Pause and resume in the service

- [x] Write the tests in `SpeechServiceTest`, each with the stand-ins and no model, audio device or network:
    - a pause while playing plays no further slice, discards the player, keeps the reading current and paused, and
      runs no further work until a resume (R2, R10, R13)
    - a resume after a pause in the second of three pieces plays the second piece from its first sample, then the
      third, closes the new player and ends the reading; `textsMade` holds each piece once, so nothing was made
      again (R3, R4, R13)
    - with the player lagging, a pause just after the third piece began to be written, while the second is still
      being heard, resumes at the second (R3, R13)
    - a pause in the silence after "code example" resumes at the piece after it, and `heard()` holds no more of the
      silence after the pause; a pause in a silence at the end of the parts resumes into the end and ends the
      reading (R3, R11, R13)
    - a pause before the playing work has run, and one before the first slice, resume at the first piece (R3)
    - a pause while the engine has made the end of the text does not end the reading (R10)
    - a pause during the play-out at the end discards the player and leaves the reading paused, and the resume plays
      the last piece again and ends (R3, R11)
    - three pauses and resumes in one reading reach the end of the text, close the last player and end the reading
      (R11, R13)
    - a stop, closing the file, closing the project and a read of another target each end a paused reading and call
      `onChange`, and the read of the other target reads it (R5, R10, R13)
    - a failure of the engine while paused ends the reading and reports it; a failure of opening the player at a
      resume ends the reading and reports it; neither a pause nor a stop reports the exception of a discarded
      player (R12)
    - a pause or resume of another target, a pause of a paused reading and a resume of one that plays do nothing (R12)
    - the first sound is reported once for the press, and the first sound after a resume is reported as after a
      resume, measured from the resume's press (R4)
    - `dispose` ends a paused reading (R10)
- [x] Number the segments in the engine work and put `AudioRun`s; give `Reading` `paused`, the resumption and the
  running playing work; make the playing work take through `PlayedAudio`, play a resumption before new runs, drop
  silence runs while the resumption says so, and end at a pause as the approach says; add `isPaused`, `pause` and
  `resume` to the service and to `Readings` and `CurrentReading`; change `reportFirstSound` to say whether it follows
  a resume, logging "First sound N ms after Resume Reading was pressed" for one (R2, R3, R4, R10, R11, R12)
- [x] Run `./gradlew test` and see the whole suite pass (R13)

Commit point 2.

### The tab buttons

- [ ] Write the tests in `ReadAloudActionTest`, with `presentationFor` and a `presentationFor` of the stop action in
  `ButtonPressSupport`:
    - while nothing is read the first button shows "Read Aloud" and `AloudRiteIcons.ReadAloud`, also on the tab of a
      Markdown file while another file is read, and the stop button is not visible on any tab (R1, R5, R13)
    - from the press on, before any work has run, the first button shows "Pause Reading" and
      `AllIcons.Actions.Pause`, and the stop button shows "Stop Reading" and `AllIcons.Actions.Suspend` on that file's
      tab and on no other (R2, R5, R13)
    - a press while it reads pauses, and the first button then shows "Resume Reading" and
      `AloudRiteIcons.ResumeReading` while the stop button stays (R2, R3, R5, R13)
    - a press while paused resumes, and the first button shows "Pause Reading" again (R3, R13)
    - a press on the stop button, while it reads and while it is paused, ends the reading: the stop button goes and
      the first button shows "Read Aloud" (R5, R13)
    - a change of the document while paused does not reach what is heard after the resume (R9, R13)
    - closing the tab of a paused reading and a press on another Markdown file end it, and the other file is read
      (R10, R13)
    - a reading paused and resumed several times ends at the end of the file with the stop button gone and "Read
      Aloud" shown (R11, R13)
    - a failure while paused puts both buttons back (R12)
    - no icon of either button in any state is `AllIcons.Actions.Execute` or `AllIcons.Actions.Resume` (R7)
- [ ] Change `actionIsInTheEditorTabRowWithTextAndIcon` and `thePluginRegistersNoActionButTheButton` to the new
  registration: `EditorTabsToolbarActions` holds `com.ritenoar.aloudrite.ReadingButtons`, the group
  `com.ritenoar.aloudrite.ReadingButtonGroup` holds `com.ritenoar.aloudrite.ReadAloud` and
  `com.ritenoar.aloudrite.StopReading` in that order, and the plugin registers those four ids and no other (R5, R6)
- [ ] Write `ReadingButtonsTest`: the action of the tab row is visible for a Markdown file only; its custom component
  is a `ReadingButtonsPanel` whose background color is `JBUI.CurrentTheme.ActionButton.hoverBackground()`; it holds
  an action toolbar over `ReadingButtonGroup` whose target component is the panel; and the hover color of its button
  look is `pressedBackground()` and not the background (R6)
- [ ] Make `ReadAloudAction` show and act by the three states; write `StopReadingAction`; write
  `ReadingButtonsAction` with `ReadingButtonsPanel`, which paints a rounded rectangle with an arc of `JBUI.scale(6)`
  in the background color looked up at paint time, and the button look; register them in `plugin.xml` (R1, R2, R3,
  R5, R6, R7)
- [ ] Run `./gradlew test` and see the whole suite pass (R13)

Commit point 3.

### The tool window

- [ ] Write the tests in `ReadingPanelTest`: while a file plays the panel shows "Reading notes.md", "Pause" and "Stop";
  while paused "Paused reading notes.md", "Resume" and "Stop"; "Pause" pauses and "Resume" resumes as the tab button
  does, and the tab button follows; "Stop" ends a paused reading and the panel shows that nothing is read; the panel
  follows a pause and a resume on the tab, and the end of the file after a resume; the two buttons keep their own
  width in a wide pane (R8, R11, R13)
- [ ] Add the paused text and the pause or resume button to `ReadingPanel`, in a row with "Stop" that replaces
  `stopRow`, acting on the reading the panel shows (R8)
- [ ] Run `./gradlew test` and see the whole suite pass (R13)

Commit point 4.

### The README

- [ ] Describe in `README.md` the person speaking in place of the loudspeaker, on the tab and in the tool window; the
  pause and the resume on the tab, where a resume begins, also after a pause in a silence; the stop button on the
  tab; the "Pause", "Resume" and "Stop" of the tool window; and that a paused reading ends as a reading does and at a
  press on another file. Keep every other statement of the paragraphs it touches (R16)

Commit point 5.

### The developer's checks

- [ ] Hand the developer the checks of R14 and R15 on Linux x64 and on macOS on Apple Silicon, and record what they
  found in `record.md`: for R14, pausing and resuming `samples/markdown-features.md` several times in the middle of a
  sentence and between two, also in the silence after "code example", whether the sound stopped within a second,
  whether the first sound after a resume came within a second, with the logged number, and whether the sentence heard
  again was the one expected, and stopping a reading that plays and one that is paused from the tab; for R15, each
  state of the tab buttons and the tool window's icon at their real size in a light and a dark theme, with a run
  configuration running, and whether any icon was taken for Run or Stop (R14, R15)

Commit point 6.

## Verification

- [ ] `./gradlew test` passes, with no test of this unit skipped
- [ ] `./gradlew buildPlugin` builds the archive
- [ ] `npm run check-changes` passes on the committed branch
- [ ] The record holds the developer's findings for R14 and R15 on both machines

## Commit Points

- [x] 1. `feature-0012 Plan pause and resume`: this plan and its log entry
- [x] 2. `feature-0012 Pause and resume a reading`: the icons, the runs, `PlayedAudio`, `samplesHeard()` and the
  service, with their tests
- [ ] 3. `feature-0012 Pause, resume and stop from the tab`: the tab buttons, their background and `plugin.xml`, with
  their tests
- [ ] 4. `feature-0012 Pause and resume in the tool window`: the panel, with its tests
- [ ] 5. `feature-0012 Describe pause and resume`: `README.md`
- [ ] 6. `feature-0012 Record the developer's checks of pause and resume`: `record.md`
