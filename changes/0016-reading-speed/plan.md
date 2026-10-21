# Reading speed

## Approach

The speed is a value of its own, kept by an application service that the IDE stores, and read by the speech service
at the start of a reading and at each change. The tool window and a new settings page set it. What is new is in five
places.

**The value.** A new class `ReadingSpeed` holds the speed as a whole number of tenths, from 5 to 20, so that no
floating point comparison decides a limit or a step. It gives the speed for the engine as a `Float`, its display as
"1.0×", `slower()` and `faster()`, which stay within the range, whether a step is possible either way, all sixteen
values in order, and the length of a silence at that speed: its milliseconds at 1.0 times ten divided by the tenths,
rounded to the nearest millisecond. A companion function `parsedSpeed(text: String): ReadingSpeed?` reads what was
typed in the field, after trimming white space: one or more digits, optionally followed by a point and exactly one
digit, with a value from 0.5 to 2.0. So "2", "1.5", "2.0" and " 0.5 " are accepted, and "1.55", "1,5", "2.1", "0.4",
"", "1." and ".5" are not. The last two are read as not "a number with one decimal at the most" in the sense of
requirement 3, and nothing in the spec asks for them (R1, R3).

**The setting.** A new application service `ReadingSpeedSetting` is a `PersistentStateComponent` with `@State(name =
"AloudRiteReadingSpeed", storages = [Storage("aloudrite.xml")])`, whose state holds the tenths, 10 by default. A
stored value outside the range, as from a file edited by hand, is taken as 1.0, since the file is a boundary. Its
`speed` is the current speed, and `set(speed, afterAWait: Boolean)` stores a speed that differs from the current one
and hands it to the collaborator `onChange(speed, afterAWait)`. Its no-argument constructor makes that collaborator
hand the change to `SpeechService.changeSpeed` and publish it on a new topic, `READING_SPEED_CHANGED`, which every
panel and the settings page follow. As with `SpeechService`, a test hands in its own collaborator. A set to the speed
that is already current changes nothing, so a typed "1" while the speed is 1.0 is no change of the reading (R4, R5).

**The engine and the silences.** `SpeechEngine.speak` takes the speed as a `Float` beside the text, and
`KokoroSpeechEngine` hands it to `generateWithCallback` in place of `NORMAL_SPEED`, which goes. The speaker, the model
and everything else handed to the engine stay as they are (R12). The engine work of the service makes every piece and
every silence of one making at that making's speed: the speed goes to `speak`, and a silence gets the samples of
`ReadingSpeed`'s length of it (R11).

**A change during a reading makes the audio again.** `SpeechService` gets three collaborators more:
`currentSpeed: () -> ReadingSpeed`, the speed of the setting; `nowMillis: () -> Long`, a clock; and `runLater:
(delayMillis: Long, work: () -> Unit) -> Unit`, which runs a work after a delay. In the IDE they are the setting,
`System.nanoTime()` in milliseconds and the scheduled executor of `AppExecutorUtil`. A reading takes `currentSpeed()`
when `read` is called (R6).

The audio of a reading is made in *makings*. A making is the audio made from one segment on, at one speed, into an
`AudioAhead` of its own. The first making starts at segment 0 when the reading starts. The segments are numbered once
per reading, as now, so the numbers of a later making go on where the earlier left off and `PlayedAudio` and the
resumption of unit 0012 work across makings as they are. `Reading` holds the current making in place of its
`Playback`, and the sample rate once the engine is made. Each engine work and each playing work is bound to the making
it was started for: an engine work makes nothing more, and a playing work plays nothing more, once its making is no
longer the current one. The engine work looks at that before each segment, so that a making replaced before its
engine work ran gives no text to the engine at all.

`changeSpeed(speed, afterAWait: Boolean)` acts on the current reading, and does nothing where there is none. The
reading takes the new speed and a time before which no new making starts: the press plus 500 milliseconds after a
press on a button, and the moment of the call otherwise, which also ends a wait still running from a press (R8). Then,
by the state the reading is in:

* where no making has started yet, nothing more is done, and the first making starts at the new speed
* where a change is already pending, that is a change seen by the playing work whose next making has not started,
  nothing more is done either: the pending change takes the newer speed and the newer time, so presses in a row, and
  changes in quick succession by any means, give one making, at the last speed (R8, R9)
* where the reading plays, the reading marks a change not yet seen, as the pause of unit 0012 marks a pause not yet
  seen, cancels the `AudioAhead` of the making, so that the engine stops after the sentence it is making and the audio
  made ahead is dropped, wakes the playing work, and discards the line where the work is playing out, taking the
  position before that, as a pause does (R7)
* where the reading is paused and the playing work has parked, the reading cancels the making and turns its stored
  resumption into a restart from the first segment of that resumption, with its skip of silence, which the resume then
  starts (R10)

A change made by a button also hands `runLater` a check at 500 milliseconds; a later press hands a later one, and a
check that comes before the time the reading holds does nothing, so no check is ever cancelled.

The playing work sees a change not yet seen where it now sees a pause: between two slices, after each take, and after
the play-out. It takes the position the line has made audible, discards the player, so the sound stops within a slice
of a tenth of a second (R7), and asks `PlayedAudio` for the resumption as at a pause. The restart is the first
segment of that resumption with its skip of silence, so the piece that was playing is made again from its beginning
and a change during a silence goes on at the piece after it (R7). Where the resumption is empty, because nothing was
taken on this player yet, the restart is that of the current making. The work hands the restart to the reading and
ends. A pause and a change both seen at once end the same way, and the reading then stays paused with the restart
stored (R10).

A new making starts once the reading has a restart, is not paused and the time it holds has come. Three places call
the one function that checks this: the playing work when it hands over the restart, the check of `runLater`, and a
resume. The function starts the making under the monitor of the reading, so that two of them cannot both start it, and
hands `runInBackground` an engine work and a playing work. The playing work plays the new `AudioAhead` with the
restart's skip of silence, through the `RunsToPlay` of unit 0012 with no runs to play again. The engine work takes
the semaphore of the engine as now, so it begins once the engine work of the cancelled making has let go. A resume of
a reading with a restart is a resume that starts a making, and may take as long as the engine takes for the piece
(R10).

The first sound of a making that follows a change is reported as after a change of the speed, measured from the last
change, so that the developer's check of R15 has a logged number beside what was heard: `reportFirstSound` takes a
`FirstSoundAfter` of `READ_ALOUD`, `RESUME` or `SPEED_CHANGE` in place of its `Boolean`, and the IDE logs "First sound
N ms after the speed was changed" for the last.

The stand-ins of unit 0006 keep running the background works one after another on the test thread. They get the
speed of the setting as a variable, a clock of their own that a test moves on with `passTime(milliseconds)`, which runs
the works of `runLater` that come due, and the speed of each text the engine was given, in `speedsMade`, beside
`textsMade`. No test of the wait waits for real time (R14).

**The controls.** `ReadingPanel` gets a row of its own under the reading, shown always: a "Slower" button, a field
holding the speed with one decimal, a label "×", and a "Faster" button. It takes the setting as a third constructor
argument, `() -> ReadingSpeedSetting`, and follows `READING_SPEED_CHANGED` in `followTheReading` as it follows the
reading, so that every panel shows the speed and the limits of the buttons of the moment (R2, R4). "Slower" and
"Faster" set the speed one step down or up with `afterAWait = true`, and are disabled at 0.5 and 2.0 (R2, R8). The
field sets the speed with `afterAWait = false` when Enter is pressed and when it loses focus, and then shows it with
one decimal (R3). A value `parsedSpeed` does not accept leaves the speed as it was, and the field gets the error of a
`ComponentValidator` with the message "Type a speed from 0.5 to 2.0", which sets the outline of the field to the IDE's
error mark. While it has the error, each edit validates again and the error goes once the text is accepted. Escape in
the field puts the current speed back in it and removes the error, and is consumed there, so that it does not move the
focus to the editor as Escape in a tool window does. A change of the speed from elsewhere puts the new speed in the
field and removes an error it has (R3, R4).

The settings page is a new `ReadingSpeedConfigurable`, registered as an `applicationConfigurable` under `tools` with
the display name "AloudRite". It shows the label "Reading speed:" and a combo box of the sixteen values, each shown
as "1.0×". Reset selects the speed of the setting, `isModified` compares the two, and apply sets the selection with
`afterAWait = false` (R4, R8). It is built with plain Swing, as the panel is.

There is no end-to-end suite that drives an IDE window, as unit 0003 decided; the developer's checks of R15 and R16
are the end-to-end coverage. The unit changes the speed handed to the engine, so the tests with the real engine run
with the model in place, and a skipped run does not count as a pass.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSpeed.kt`: new, the value, its display, its steps, the length of a
  silence and `parsedSpeed`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSpeedSetting.kt`: new, the stored setting and the topic
  `READING_SPEED_CHANGED`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingSpeedConfigurable.kt`: new, the settings page
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechEngine.kt`, `KokoroSpeechEngine.kt`: the speed with each piece
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: makings, `changeSpeed`, the wait, the new collaborators
  and `FirstSoundAfter`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingPanel.kt`: the row of the speed
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingToolWindowFactory.kt`: hands the panel the setting
- `src/main/resources/META-INF/plugin.xml`: the settings page
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingSpeedTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingSpeedSettingTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingSpeedConfigurableTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechStandIns.kt`: the speed, the clock, `runLater`, `speedsMade` and
  `FirstSoundAfter`
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechServiceTest.kt`: the speed of a reading and the changes
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingPanelTest.kt`: the row of the speed
- `src/test/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngineTest.kt`, `EngineThreadsMeasurement.kt`: the speed
  handed to `speak`, and a case at 2.0
- `README.md`: the speed
- `docs/reading-markdown.md`: the lengths of the silences as those at 1.0×

## Tasks

### The value and the setting

- [x] Write `ReadingSpeedTest`: the sixteen values from 0.5 to 2.0 in order, 1.0 among them; the display "0.5×",
  "1.0×" and "2.0×"; the engine speed 1.0f for 1.0 and 2.0f for 2.0; `slower()` and `faster()` by one step, with no
  step possible below 0.5 or above 2.0; the length of a silence of 500 ms as 250 at 2.0 and 1000 at 0.5, of 2000 ms as
  1000 at 2.0 and 4000 at 0.5, and of 250 ms as 357 at 0.7; `parsedSpeed` accepting "2", "1.5", "2.0", "0.5" and
  " 1.5 ", and refusing "1.55", "1,5", "2.1", "0.4", "", "1.", ".5", "abc" and "-1" (R1, R3, R11, R14)
- [x] Write `ReadingSpeed` to pass it. It does no IO (R1, R3, R11)
- [x] Write `ReadingSpeedSettingTest`: a new setting gives 1.0; a setting whose state was taken after a set to 1.7
  gives 1.7 once that state is loaded into a new setting; a loaded state of 3 or of 25 tenths gives 1.0; `set` hands
  the collaborator the speed and whether to wait, and a set to the current speed hands it nothing; the setting the
  IDE registers is one application service, and is a `PersistentStateComponent` stored in `aloudrite.xml` (R4, R5,
  R14)
- [x] Write `ReadingSpeedSetting` and `READING_SPEED_CHANGED` to pass it (R4, R5)
- [x] Run `./gradlew test` and see the whole suite pass (R14)

Commit point 2.

### The speed in the engine and the service

- [ ] Add to `SpeechStandIns` the variable `speed`, 1.0 by default, the clock `clockMillis` and `passTime`, the works
  of `runLater` kept with their due time, and `speedsMade`; change `firstSounds` to `FirstSound(milliseconds, after:
  FirstSoundAfter)` and the six places in the tests that use `afterResume` to it (R14)
- [ ] Write the tests in `SpeechServiceTest`, each with the stand-ins and no model, audio device or network:
    - a reading started at 1.5 hands the engine 1.5 with every piece, and one started after the speed was set to 0.7
      hands it 0.7 (R6, R14)
    - at 2.0 a silence of 500 ms is heard as 250 ms and one of 2000 ms as 1000 ms, and at 0.5 as 1000 ms and
      4000 ms (R11, R14)
    - a change with no reading does nothing, and the next reading reads at the new speed (R6)
    - a change by the field while the second of three pieces plays plays no further slice, discards the player, and
      `heard()` then holds the second piece again and the third, made at the new speed, with no audio of the old speed
      after the change; the reading ends at the end of the text (R7, R14)
    - with the player lagging, a change just after the third piece began to be written, while the second is still
      being heard, goes on at the second (R7, R14)
    - a change during the silence after "code example" goes on at the piece after it, and `heard()` holds no more of
      that silence (R7, R14)
    - a change during the play-out at the end discards the player and makes the last piece again at the new speed
      (R7)
    - a change before the playing work has run makes the first piece at the new speed and nothing at the old one
      (R6, R7)
    - a change by a button makes nothing before the clock has passed 500 ms and makes the piece at 500 ms; presses at
      0, 300 and 600 ms make the piece once, at 1100 ms, at the speed of the last press; two presses 700 ms apart
      make it twice, once at each speed (R8, R14)
    - a change by the field 200 ms after a press on a button makes the piece at once, at the speed of the field (R8)
    - a typed change from 1.0 to 2.0 makes the piece once, at 2.0 (R3, R9, R14)
    - three changes in a row by the field, the next made before the works of the one before have run, make the piece
      once, at the last speed, and `speedsMade` holds no other speed for it (R9, R14)
    - a change while paused plays nothing and runs no work that plays until the resume; the resume makes the piece
      the pause was in at the new speed, also where the pause was in a silence, where it goes on at the piece after
      it; a pause after a change and before its making started stays paused, and its resume starts that making
      (R10, R14)
    - a pause and a resume after a change go on at the new speed with the audio already made, and make nothing again
      (R7, R10)
    - a stop, closing the file and a read of another target during a wait end the reading, and the check that comes
      due afterwards starts nothing (R7, R8)
    - the first sound after a change is reported as after a change of the speed, measured from the last change (R15)
- [ ] Add the speed to `SpeechEngine.speak`, hand it on in `KokoroSpeechEngine` and remove `NORMAL_SPEED`; pass 1.0f
  in `EngineThreadsMeasurement` and in the existing cases of `KokoroSpeechEngineTest` (R1, R12)
- [ ] Add to `KokoroSpeechEngineTest` that a sentence made at 2.0 lasts between 40 and 65 percent of its length at
  1.0, and one at 0.5 between 160 and 250 percent (R1, R12)
- [ ] Make `SpeechService` read in makings as the approach says: the new collaborators, the speed taken at `read`,
  the making bound to each work, the silences of a making at its speed, `changeSpeed`, the change not yet seen, the
  restart, the one function that starts a making, the resume that starts one, and `FirstSoundAfter` in place of the
  `Boolean` of `reportFirstSound`. Its no-argument constructor takes the speed from `ReadingSpeedSetting` (R6, R7,
  R8, R9, R10, R11)
- [ ] Run `./gradlew test` with the model in its folder, and see the whole suite pass with none of the tests of the
  real engine skipped (R12, R13, R14)

Commit point 3.

### The tool window

- [ ] Write the tests in `ReadingPanelTest`, with a `ReadingSpeedSetting` whose collaborator hands each change to the
  stand-in service and publishes it:
    - while nothing is read and while a file is read, the panel shows "Slower", "1.0", "×" and "Faster" (R2, R14)
    - "Faster" from 1.0 sets 1.1 after a wait, and "Slower" sets 0.9 after a wait; at 0.5 "Slower" is disabled and
      at 2.0 "Faster" is, and the other is enabled (R2, R8, R14)
    - "2" typed and Enter pressed sets 2.0 with no wait, in one change, and the field shows "2.0"; the same set by
      the loss of focus; "1.5" sets 1.5 (R3, R14)
    - "2.5", "1,5" and "1.55" typed and Enter pressed leave the speed as it was, and the field has a validation
      error whose message names 0.5 and 2.0 and the outline "error"; the error stays at the loss of focus, goes once
      the text is "1.5", and Escape puts the current speed in the field and removes it (R3, R14)
    - a press during a reading changes the speed of the reading after the wait, and a typed speed at once (R7, R8)
    - a change in the panel of one project shows in the panel of another, with the limits of its buttons, and a set
      of the setting shows in both (R4, R14)
- [ ] Add the row of the speed to `ReadingPanel` as the approach says, and hand it the setting in
  `ReadingToolWindowFactory` (R2, R3, R4, R8)
- [ ] Write `ReadingSpeedConfigurableTest`: the plugin registers an application configurable with the display name
  "AloudRite" under `tools`; its page shows the label "Reading speed:" and the sixteen values as "0.5×" to "2.0×";
  reset selects the speed of the setting; choosing another value makes it modified, and apply sets it with no wait;
  a set in the panel shows on the page after a reset (R4, R8, R14)
- [ ] Write `ReadingSpeedConfigurable` and register it in `plugin.xml` (R4)
- [ ] Run `./gradlew test` and see the whole suite pass (R14)

Commit point 4.

### The documentation

- [ ] Describe in `README.md` the speed from 0.5× to 2.0× in steps of 0.1, 1.0× until it is chosen and kept across
  restarts for the whole IDE; the row of the speed in the tool window, its buttons and their limits, and the field
  with what it accepts, Enter, the loss of focus, the error mark and Escape; the page under Settings, Tools,
  AloudRite; that the silences follow the speed, with the half second and the two seconds at 2.0 and at 0.5; that a
  change during a reading stops the sound and goes on from the beginning of the piece that was playing, or after a
  silence, and that it may take as long as the engine takes for the piece; the wait of half a second after a press on
  a button and none after a typed or chosen speed; and a change while paused. Replace "at normal speed" in the
  paragraph on the voice, and keep every other statement of the paragraphs it touches (R17)
- [ ] Say in `docs/reading-markdown.md`, above the table of the silences, that the lengths are those at 1.0× and are
  divided by the speed, keeping the table as it is (R11, R17)

Commit point 5.

### The developer's checks

- [ ] Hand the developer the check of R15 on Linux x64 and record what they found in `record.md`: the first five
  paragraphs of `samples/ten-thousand-words.md` listened to at each of the sixteen speeds, with how understandable
  each was and whether a silence came in the middle of a sentence; the speed changed during a reading with a single
  press, with presses in a row, with a typed speed, several times in a row, and while paused, with the time from the
  change to the sound at the new speed for the first three, taken from the logged "First sound N ms after the speed
  was changed" beside what was heard. Where a speed is not understandable, stop and log it for the human, who sets
  the range (R12, R13, R15)
- [ ] Hand the developer the check of R16 on Linux x64 and on macOS on Apple Silicon and record what they found:
  `samples/markdown-features.md` to its end at 0.5, 1.5 and 2.0, with, for each machine and speed, whether a silence
  came in the middle of a sentence and how the silences sounded (R11, R13, R16)

Commit point 6.

## Verification

- [ ] `./gradlew test` passes with the model in its folder, with no test of the real engine skipped
- [ ] `./gradlew buildPlugin` builds the archive and passes its size check
- [ ] `npm run check-changes` passes on the committed branch
- [ ] The record holds the developer's findings for R15 on Linux x64 and for R16 on both machines

## Trace

- R1: the value, the engine
- R2: the tool window
- R3: the value, the service (a typed change as one), the tool window
- R4: the setting, the tool window, the settings page
- R5: the setting
- R6: the service
- R7: the service, the tool window
- R8: the service, the tool window, the settings page
- R9: the service
- R10: the service
- R11: the value, the service, `docs/reading-markdown.md`, the check of R16
- R12: the engine, the tests of the real engine, the check of R15
- R13: the tests of the real engine, the checks of R15 and R16
- R14: the tests of every section but the documentation and the checks
- R15: the check of R15, and the logged first sound after a change
- R16: the check of R16
- R17: the documentation

## Commit Points

- [x] 1. `feature-0016 Plan the reading speed`: this plan and its log entry
- [x] 2. `feature-0016 Keep a reading speed`: `ReadingSpeed` and `ReadingSpeedSetting`, with their tests
- [ ] 3. `feature-0016 Read at the speed and make the audio again at a change`: the engine, the service and the
  stand-ins, with their tests
- [ ] 4. `feature-0016 Set the speed in the tool window and in the settings`: the panel, the settings page and
  `plugin.xml`, with their tests
- [ ] 5. `feature-0016 Describe the reading speed`: `README.md` and `docs/reading-markdown.md`
- [ ] 6. `feature-0016 Record the developer's checks of the reading speed`: `record.md`
