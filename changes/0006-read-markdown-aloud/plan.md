# Read the Markdown file aloud

## Approach

A press of "Read Aloud" takes the text of the file from its document, as the editor holds it, and hands it to the
`SpeechService` of unit 0004, which becomes a reader of one file at a time. The service cuts the text into short
pieces, has the engine turn them into audio one after the other on one background thread, and plays that audio on a
second background thread as it arrives, so that the engine works ahead of the playing. A reading can be stopped at
any moment: the playing thread writes the audio to the line in slices of 100 ms and looks at the stop between two
slices, and a stop discards what the line still holds. The button asks the service whether its file is being read
and shows the stop icon and "Stop Reading" while it is. Two listeners stop the reading when the file's tab is closed
and when its project is closed, and disposing the service stops it when the IDE exits.

The press, in this order:

1. When the file of the event is being read, stop that reading and return. This comes first, so the stop button
   always works.
2. When the platform is not supported, show the message of unit 0004 and return.
3. Take the text of the file and give each character above U+FFFF as one space (R11). When what is left is empty or
   white space only, show the notification that there is nothing to read and return. Nothing is asked and nothing
   is downloaded (R12).
4. When the model is in place, read the text and return. A reading of another file stops (R8).
5. Otherwise the flow of unit 0005, unchanged, with one difference: what follows the download is the reading of the
   text taken in step 3, through a callback that the service hands out and forgets once the file's tab or project is
   closed (R15).

This plan is written against the branch `feature-0006-read-markdown-aloud`, cut from `master` at `1f8ed00`, which
holds unit 0005 as implemented. `ReadAloudAction` speaks the constant `TEST_SENTENCE` through a function
`speak(text, modelFolder, pressedAtNanos)` that the constructor without arguments fills with the `SpeechService`.
`SpeechService` runs one sentence at a time through `OneAtATime`, plays each piece of the engine inside the engine's
callback, and ignores a press while it runs. `./gradlew test` on that tree on 2026-10-18 on the Linux host ran 116
tests, one skipped (`RealModelPackageTest`) and four failed, see "Where the work is done".

The plan was changed on 2026-10-19 for the correction of the spec of that date, made while the unit was being built:
requirement 11 now gives each character above U+FFFF to the engine as one space, and requirements 17 and 19 follow.
The tasks for it are the ones after the ticked tasks of the first implementation. Requirement 12 was corrected
after that, at 2026-10-19T00:13:30Z, as the human decided when the plan was changed: a file holding nothing but such
characters and white space counts as empty. The task that rests on it waits for the human's approval of that
correction.

### What a trial established

Made on 2026-10-18 on the Linux host (x64, the model of the human in its folder), in a copy of the tree at `1f8ed00`
outside the repository with one throwaway test calling `OfflineTts` of sherpa-onnx 1.13.8 directly, with two threads
and the voice of the plugin, under `./gradlew test`. The copy was deleted afterwards. Use these as established, and
log it in `record.md` where one of them turns out not to hold:

* `generateWithCallback` calls the callback once per sentence, where a sentence ends at `.`, `!` or `?`. A line
  break does not end one: a heading, the blank line after it and the first sentence came as one piece. Within a long
  sentence it cuts by itself at about 80 to 100 words.
* It took 0.42 seconds for each second of audio, with the model loaded, over a paragraph of 16 seconds of audio. The
  first piece, a heading and a sentence of 13 words, 4.7 seconds of audio, came after 1.9 seconds.
* A callback that returns `0` stops the generation after the sentence it was given; the call then returns. The
  generation of a sentence cannot be stopped inside it.
* The call returns all the audio it made as one array, besides handing it to the callback. A file read in one call
  would therefore keep the whole audio in memory, about 350 MB for an hour.
* **One call with more than about 1,500 characters ends the process with SIGSEGV**, whatever the text is: 1,501
  characters passed and 1,801 crashed for words without punctuation and for one run of `x` without spaces, and an
  earlier text of 40 sentences without full stops, about 3,300 characters, crashed as well. Words with commas passed
  at 1,430 characters. Where the limit counts bytes or phonemes and not characters was not found out. In the IDE this
  crash would end the IDE.

From the jar of sherpa-onnx 1.13.8, read with `javap`: `OfflineTtsCallback.invoke(float[])` returns an `Integer`, and
`OfflineTtsConfig` has `maxNumSentences`, which the plugin leaves at its default.

From the jars of IntelliJ IDEA 2026.2.3, read with `javap`: `ProjectCloseListener` has `TOPIC` and
`projectClosing(Project)`, `FileEditorManagerListener` has `fileClosed(FileEditorManager, VirtualFile)`,
`ActivityTracker.getInstance().inc()` exists, and `AllIcons.Actions.Suspend` is the stop icon of the platform.

### Decisions the spec leaves to the plan

* **The text is cut by the plugin, into pieces of at most 400 bytes in UTF-8**, by a pure function
  `piecesOf(text): List<String>`. That keeps every call far below the crash of the trial, also for text of
  characters that take four bytes, keeps the memory of one call small, and lets a stop or a switch wait for at most
  one short piece. The rules, in order:
    * A piece ends after the white space that follows a sentence end: `.`, `!` or `?`, followed by any of `"`, `'`,
      `)`, `]` and `*`, followed by white space. The white space belongs to the piece before
    * A piece also ends after the white space that follows a `,`, `:` or `;`, once the piece so far is at least 60
      bytes, added on 2026-10-19 after the human heard a silence of about 3 seconds before a long sentence that
      followed a short one, see `record.md` of 2026-10-19T10:18:06Z
    * A piece also ends after white space that holds a blank line, that is two line breaks with only spaces or tabs
      between them, so a heading or a list without full stops ends where its block ends
    * A piece still longer than 400 bytes is cut after the last white space that keeps it within 400 bytes, and where
      it has none, at 400 bytes, never inside a surrogate pair
    * The pieces joined give exactly the text handed to the reading, which is the text of the file with each
      character above U+FFFF as a space. `piecesOf` takes nothing out and puts nothing in (R11). A piece of white
      space only is not handed to the engine, which has nothing to say for it
  A sentence longer than 400 bytes, about 65 words, is therefore made in two calls, and an abbreviation such as
  `e.g.` followed by a space ends a piece. Both can change the melody at that point, but neither leaves a silence for
  the engine, which by then is ahead (R4).
* **Characters above U+FFFF become spaces at the press.** A pure function `withCharactersAboveFfffAsSpaces(text)`
  in `EngineText.kt` gives the text with each code point above U+FFFF, a valid surrogate pair in the `String`,
  replaced by one space, and every other character as it is: a run of three such characters gives three spaces, and
  characters of two and three bytes in UTF-8, among them U+2764 and U+2713, stay (R11). A half of a surrogate pair
  without its other half is left as it is, as the spec names it under "Beyond the request". The action applies it
  once, to the text it took from the document, before the check for an empty file, so that the check, the reading
  and the reading after a download all see the same text, and the service and `piecesOf` do not change. The comment
  of the function says why: the binding hands the text over in modified UTF-8, in which such a character never
  arrives intact, and the engine makes about 5.7 seconds of babble for each one.
* **The engine works ahead, at most 30 seconds of audio.** A class `AudioAhead` holds the audio that is made and not
  yet played, counted in samples. The engine thread hands each piece of audio to it and waits while 30 seconds or
  more are waiting, which keeps the memory at about 3 MB; the playing thread takes from it and waits while it is
  empty. A stop wakes both. At a rate of 0.42 the engine is ahead after its first piece, and the rest of a file is
  made while the first pieces play (R3, R4).
* **The first sound** comes after the first piece is made, which is the first sentence, or the first block, of the
  file, whatever follows it. A file of 10,000 words whose first sentence is the sentence of a one-sentence file
  therefore starts after the same time (R3). The line `First sound <n> ms after the button was pressed` of unit 0004
  stays, once per reading, and is what the implementer measures with.
* **One engine, used by one thread at a time.** The engine is not made for two callers, and loading a second one
  costs its memory. The engine thread of a reading holds a lock on the engine for the whole reading. A reading
  that was stopped lets it go after the sentence the engine is making, since that cannot be cut short, and the
  callback answers "stop" at the next one. A switch to another file (R8) therefore starts its sound up to the time
  of one piece later, typically one or two seconds, at most about 8 seconds for a piece of 400 bytes on the Linux
  host. The sound of the first file stops at once, since the stop does not wait for the engine.
* **A stop within one second (R6).** The playing thread writes each piece in slices of at most a tenth of a second
  of audio and looks at the stop before each slice. On a stop it calls `stop`, `flush` and `close` on the line, in
  place of `drain` and `close` at the end of a reading, so that what the line still holds is discarded. The sound
  ends after at most one slice that is being written.
* **The state the button shows** is whether the service's current reading is of the file of the button. A press on
  the stop button, a switch, the end of the file and a failure each change the current reading at once, and then
  call `ActivityTracker.getInstance().inc()`, which makes the toolbars of the IDE update their buttons (R6, R7, R13).
  The button shows `AllIcons.Actions.Suspend` and the text `Stop Reading`, which is its tooltip, while its file is
  read, and the icon of `plugin.xml` and `Read Aloud` otherwise.
* **What is read is identified** by a `ReadingTarget` of the project of the event, which may be null, and the
  `VirtualFile` of the event. The same file in another project is another target.
* **The tab that stops the reading.** A reading stops when its file is no longer open in any editor of its project,
  which is when the platform calls `fileClosed` (R9). A file open in two split editors keeps being read while one of
  them is closed, since the stop button is still on the other. Selecting another tab or a tool window is not
  listened to at all, so it changes nothing.
* **The text at the press** is the text of the document of the file, `FileDocumentManager.getDocument(file)`, which
  holds the changes that are not saved (R1), copied as a `String` on the event dispatch thread, so a later change
  of the document does not reach the reading (R10). For a file that has no document, the content on disk is read
  with `VfsUtilCore.loadText`.
* **An empty file** is one whose text, after the characters above U+FFFF became spaces, is empty or white space
  only, so a file of nothing but emoji is one. The notification is of the group `AloudRite`, of
  the type `INFORMATION`, without a title, with the content `There is nothing to read in <file name>.` and no action.
  A press on an empty file while another file is read leaves that reading alone: the press asked for nothing that
  can be heard, and the spec says only that it plays nothing. This is decided by the plan and open to the human.
* **A failure** of the engine, of the line or of anything else of a reading ends it, at once and once: the reading
  stops, the button is put back, and the failure is logged with `warn` and shown with
  `Messages.showErrorDialog(project, "AloudRite could not read <file name> aloud: <message>", "AloudRite")` on the
  event dispatch thread, as unit 0004 did (R13).
* **The reading after a download.** Before it hands the download to `ModelDownloads.start`, the action asks the
  service for the callback with `readOnceTheModelIsInPlace(target, text, modelFolder, pressedAtNanos)`. The service
  remembers that one waiting reading, replacing any earlier one, and the callback reads it only while it is still the
  one remembered. Closing the file's tab or its project forgets it, so a callback that runs after that, also the one
  of a "Try Again", reads nothing, even where the tab has been opened again meanwhile (R15). A cancelled download
  leaves the waiting reading remembered until the next one replaces it, which costs the memory of one text.
* **The IDE exits** by disposing the service, which stops the reading and forgets the waiting one. It closes the
  engine only where no engine thread holds it, as unit 0004 decided, and leaves it to the process otherwise.
* **The test sentence goes** from `ReadAloudAction.kt` and from the README (R16, R19). `KokoroSpeechEngineTest` keeps
  a sentence of its own as the input of the real engine, which is test data and not something the plugin speaks.
* **What the action needs of the service** is a narrow contract `Readings`, in the manner of `ModelDownloads`:
  `isReading(target)`, `read(target, text, modelFolder, pressedAtNanos)`, `stop(target)` and
  `readOnceTheModelIsInPlace(target, text, modelFolder, pressedAtNanos)`. The listeners call two further methods of
  the service itself, `fileClosed(target)` and `projectClosed(project)`, which stop the reading and forget the
  waiting reading of that file or project.
* **The two listeners** are registered in `plugin.xml`: `ReadingTabListener`, a `FileEditorManagerListener` under
  `<projectListeners>` whose constructor takes the `Project`, and `ReadingProjectListener`, a `ProjectCloseListener`
  under `<applicationListeners>` that acts in `projectClosing`. Each is one call into the service. They are not
  tested on their own, since the test environment opens no project; what they call is, and the implementer's check
  in the sandbox IDE shows the wiring.

### Where the work is done

* **On the Linux host**, where the model of the human is in its folder, so that `KokoroSpeechEngineTest` runs and
  the timing check can be made. No test or check writes to the model folder.
* **Four tests fail on this host before this unit**, all of them those that start a second Java process
  (`LockProbe`): `ModelDownloadFlowTest.aPressWhileAnotherProcessHoldsTheLockAsksNothingStartsNothingAndSaysSo` and
  three of `InstallAttemptTest`, with `Cannot run program ".../jbr/bin/java": error=7, Argument list too long`. They
  are not this unit's, and are not to be changed in it. The implementer tells the human, who decides whether a bug
  unit is written. Every verification below that says the suite passes means: apart from these four, where they
  still fail for that reason alone.

## Files

- `src/main/kotlin/com/ritenoar/aloudrite/TextPieces.kt`: new, the pure function `piecesOf` and the limit of 400 bytes
- `src/main/kotlin/com/ritenoar/aloudrite/EngineText.kt`: new, `withCharactersAboveFfffAsSpaces`, added for the
  correction of the spec of 2026-10-19
- `src/main/kotlin/com/ritenoar/aloudrite/AudioAhead.kt`: new, the audio made and not yet played, bounded by a number
  of samples, which a stop empties and wakes
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingTarget.kt`: new, the project and the file of a reading
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechEngine.kt`: changed, `onSamples` returns whether to go on
- `src/main/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngine.kt`: changed, the callback answers `1` or `0` from that
- `src/main/kotlin/com/ritenoar/aloudrite/SamplePlayer.kt`: changed, a `discard()` beside `close()`
- `src/main/kotlin/com/ritenoar/aloudrite/AudioPlayer.kt`: changed, `discard()` stops, flushes and closes the line
- `src/main/kotlin/com/ritenoar/aloudrite/SpeechService.kt`: changed, the contract `Readings` and the service that
  reads one file at a time, in place of the one sentence
- `src/main/kotlin/com/ritenoar/aloudrite/ReadingListeners.kt`: new, `ReadingTabListener` and `ReadingProjectListener`
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAloudAction.kt`: changed, the press of the Approach and the state of the
  button; `TEST_SENTENCE` goes
- `src/main/resources/META-INF/plugin.xml`: changed, the two listeners
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechStandIns.kt`: new, the stand-in engine and player and a
  `SpeechService` made with them and with a background function that keeps the work, moved out of
  `SpeechServiceTest` and extended, because the action tests and the flow test need them too
- `src/test/kotlin/com/ritenoar/aloudrite/TextPiecesTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/EngineTextTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/AudioAheadTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/AudioPlayerTest.kt`: changed, the cases of `discard`
- `src/test/kotlin/com/ritenoar/aloudrite/SpeechServiceTest.kt`: rewritten for readings
- `src/test/kotlin/com/ritenoar/aloudrite/KokoroSpeechEngineTest.kt`: changed, the cases with the real engine, and
  for the correction of 2026-10-19 the emoji out of the case of long texts and a case of a sentence with an emoji
- `src/test/kotlin/com/ritenoar/aloudrite/ButtonPressSupport.kt`: changed, the event carries a project where one is
  given, and a helper reads the presentation of the button for a file
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAloudActionTest.kt`: changed, the test of the sentence goes, the cases of
  reading come
- `src/test/kotlin/com/ritenoar/aloudrite/ModelDownloadFlowTest.kt`: changed, the reading after a download in place of
  the sentence
- `src/main/kotlin/com/ritenoar/aloudrite/ModelDownloadService.kt`: changed, its constant `NOTIFICATION_GROUP_ID` is no
  longer private, so that the notification of nothing to read names the group through it and not a second time,
  see `record.md`
- `README.md`: what the button does
- `samples/markdown-features.md`, `samples/one-sentence.md`, `samples/ten-thousand-words.md` and `samples/empty.md`:
  new, files for the checks by ear, added at the human's request
- `changes/0006-read-markdown-aloud/record.md`: the log

Not changed, and named because the plan depends on it: `start(modelFolder, attempt, onModelInPlace)` of
`ModelDownloadService.kt`, which stays as it is, `OneAtATime.kt`, which the download service keeps using and `SpeechService` no
longer does, `KokoroModel.kt`, `SpeechPlatform.kt`, `build.gradle.kts` and `THIRD-PARTY.md`.

## Tasks

- [x] Write `TextPiecesTest`, a plain JUnit 4 test of `piecesOf`. For every case it also asserts that the pieces
      joined are the text and that none is longer than 400 bytes in UTF-8. The cases: one sentence is one piece;
      `One. Two! Three? Four` gives four pieces, each with the white space after it; `He said "yes." Then` and
      `(See this.) Then` end after the closing mark; `3.14 is` and `file.md is` stay one piece; `# Title\n\nText.`
      gives `# Title\n\n` and `Text.`; a list of lines without full stops separated by single line breaks stays one
      piece; a blank line of spaces between two line breaks ends a piece; 10,000 words of prose give only pieces of
      at most 400 bytes; one sentence of 200 words without punctuation is cut at white space; a run of 2,000 `x` is
      cut at 400 bytes; a run of 500 emoji, each a surrogate pair, is cut between two pairs; the empty text gives no
      piece; a text of white space only gives one piece. Then write `TextPieces.kt` and see the test pass (R3, R4,
      R11)
- [x] Write `AudioAheadTest`, a plain JUnit 4 test with real threads and timeouts of at most 5 seconds: what is put
      is taken in order; `take` waits while it is empty and gives the next piece once one is put; after `finish`,
      `take` gives what is left and then null; `put` waits while the samples waiting reach the limit and goes on once
      enough was taken; `cancel` wakes a `put` and a `take` that wait, after which `put` drops what it is given and
      `take` gives null. Then write `AudioAhead.kt` with a limit in samples given to its constructor, and see the
      test pass (R4, R6)
- [x] In `AudioPlayerTest`, add: `discard` calls `stop`, `flush` and `close` on the line in that order and never
      `drain`; `discard` still closes the line when `flush` throws. Then add `discard()` to `SamplePlayer` and
      `AudioPlayer`, and see the test pass. The cases of `close` stay as they are (R6)
- [x] Change `SpeechEngine.speak` so that `onSamples` returns `true` to go on and `false` to stop, and
      `KokoroSpeechEngine` so that its callback answers `1` and `0` from that. In `KokoroSpeechEngineTest`, which
      runs only where the model is in place, keep the case of the sentence with a callback that goes on, and add:
      a callback that answers `false` at once gets one call for a text of three sentences; and every piece that
      `piecesOf` gives for a text of 2,000 characters without punctuation, 2,000 `x`, 2,000 digits and 600 emoji is
      made without a crash; a piece that is the same as an earlier one is made once, see `record.md` of
      2026-10-18T22:05:39Z. In the `SpeechService` of unit 0004, which this task does not rewrite yet, let the
      callback answer `true` after playing, so that the tree builds and the suite passes at this commit. Run the
      test on this host and see it pass and not be skipped (R6, safety of R1)
- [x] Write `ReadingTarget.kt`, a data class of a `Project?` and a `VirtualFile` (R8, R9)
- [x] Move the stand-in engine and player of `SpeechServiceTest` into `SpeechStandIns.kt` and extend them: the engine
      records each text it is given, gives for each a few pieces of samples whose values say which text they came
      from, can be told to fail at a given text, can run a function of the test while it makes a given text, and
      stops when `onSamples` answers `false`; the player records what it plays, whether it was closed or discarded,
      and can be told to fail. A `SpeechService` is made with a background function that keeps the work, these
      stand-ins, a recorder of the first sounds, a recorder of the failures and a counter of the changes. A helper
      runs the kept works in the order they were handed over until none is left (R17)
- [x] Rewrite `SpeechServiceTest` against the stand-ins, each case as a test of its own. Run it and see it fail to
      compile, because the service has no `read`. That run was not made: the test was written before the service,
      but first compiled after it, see `record.md` of 2026-10-18T22:14:45Z and finding P1. The cases:
    - `read` returns at once and only hands work over; `isReading` of its target is true at once (R5, R6)
    - the engine is given the pieces of `piecesOf` in order, without the pieces of white space only, and the player
      plays all their samples in order, each in slices of at most a tenth of a second of audio (R1, R11)
    - when the engine is given the second piece, the audio of the first has been handed to `AudioAhead`, so playing
      can start before the rest is made (R3)
    - the end of the text closes the player without a discard, `isReading` is false after, and the change is counted
      (R7)
    - `stop` makes `isReading` false at once and counts a change; the player plays no further slice, is discarded and
      not closed; the engine is answered `false` at its next piece and is given no further text (R6)
    - `read` of another target while one is read stops the first as `stop` does and reads the second, whose engine
      work starts only once the first has let the engine go (R8)
    - the engine is given exactly the text handed to `read`; that a document changed after the press does not
      reach it is shown in the action test (R10)
    - a failure of the engine at its first piece and at a later one, a failure of making the engine and a failure of
      the player each end the reading: one failure reported, with the target, `isReading` false, the player
      discarded where there was one, a change counted, and the next `read` works (R13)
    - `fileClosed` of the target stops its reading, and of another file does not; `projectClosed` of its project
      stops it, and of another project does not, with `ProjectManager.getInstance().defaultProject` and null as the
      two projects (R9)
    - the callback of `readOnceTheModelIsInPlace` reads the text it was given with the target it was given, once;
      after `fileClosed` or `projectClosed` of that target it reads nothing, also when it is called twice, as a
      "Try Again" would; a second `readOnceTheModelIsInPlace` replaces the first, whose callback then reads nothing
      (R15)
    - the first sound is reported once per reading (R3)
    - the engine is made at the first reading and kept for the next
    - `dispose` stops the reading, forgets the waiting reading, and closes the engine when no engine work holds it,
      and leaves it alone while one does (R9)
- [x] Rewrite `SpeechService.kt`: the contract `Readings` of the Approach and the `@Service(Service.Level.APP)` class,
      whose primary constructor takes the background function, the making of the engine, the making of the player,
      the report of the first sound, the report of a failure with its target, and the function called on a change,
      and whose constructor without arguments passes the pooled threads of the application, `KokoroSpeechEngine`,
      `AudioPlayer.open`, the log line of the first sound, the error dialog of the Approach after a `warn`, and
      `ActivityTracker.getInstance().inc()`. A reading hands over one work, which takes the lock of the engine, makes
      the engine where there is none, hands over the playing work with the sample rate, and makes the pieces into an
      `AudioAhead` of 30 seconds at that rate. The current reading and the waiting reading are changed only under the
      monitor of the service. `OneAtATime` is no longer used here. See `SpeechServiceTest` pass (R1, R3 to R10, R13,
      R15)
- [x] Write `ReadingListeners.kt` with the two listeners of the Approach, each calling `service<SpeechService>()`,
      and register them in `plugin.xml` (R9, R15)
- [x] In `ButtonPressSupport.kt`, let `eventFor` and `pressTheButtonOn` take a project, null by default, that goes
      into the event as `CommonDataKeys.PROJECT`, and add `presentationFor(action, file, project)`, which updates the
      action for that event and returns its text and icon. In `ReadAloudActionTest`, delete
      `pressingTheButtonSpeaksTheSameSentenceWhateverTheFileHolds` and add these cases, with an action made with
      `Linux`, `amd64`, a folder that holds the model stand-in, the stand-in service of `SpeechStandIns.kt`, and the
      idle download service; a notification is caught as in `ModelDownloadFlowTest`. Run them and see them fail to
      compile, because the action has no such constructor. That run was not made: the cases were written before the
      action, but first compiled after it, see `record.md` of 2026-10-18T22:14:45Z and finding P1. The cases:
    - a press on a file with text shows no message, and the engine is given the pieces of that text (R1, R2)
    - a press on a file whose document was changed on the event dispatch thread in a write command, and not saved,
      reads the changed text; a change of the document after the press does not change the text the engine is given
      (R1, R10)
    - before a press the button shows `Read Aloud` and the icon of the action; after it, `Stop Reading` and
      `AllIcons.Actions.Suspend` for that file and `Read Aloud` for another file; a second press on it stops the
      reading, shows `Read Aloud` again and reads nothing more (R6)
    - after the works ran to the end of the text, the button shows `Read Aloud` again (R7)
    - a press on a second file while the first is read: the first is stopped, the second read, and only the second
      shows `Stop Reading` (R8)
    - closing the tab of the file, as `fileClosed` of its target, puts the button back and plays no more (R9, R17)
    - a press on an empty file and on one of white space only: no message, nothing handed to the engine, and one
      notification each of the group `AloudRite` with `There is nothing to read in notes.md.`; with an empty model
      folder, the same, and nothing handed to the download service, which keeps no work (R12)
    - a press on an empty file while another file is read leaves that reading going (decision of the Approach)
    - a failure of the engine during a reading: the button shows `Read Aloud` again, one failure is reported, and the
      next press reads (R13)
- [x] Change `ReadAloudAction.kt`: its primary constructor takes, in place of `speak`, a function that gives the
      `Readings`, which the constructor without arguments fills with `{ service<SpeechService>() }`; `update` sets
      the text and icon of the Approach for the target of the event; `actionPerformed` follows the five steps of the
      Approach, and the retry of a lock that cannot be taken presses again with the text of the first press;
      `TEST_SENTENCE` goes. See `ReadAloudActionTest` pass. `ModelDownloadFlowTest` does not compile until the next
      task, which belongs to the same commit (R1, R2, R6 to R8, R10, R12 to R14, R16)
- [x] Change `ModelDownloadFlowTest` to the stand-in service in place of the list `spoken`, and its constant
      `TEST_SENTENCE` goes. Each case that spoke the sentence now asserts that the engine was given the text of the
      file pressed, `# Title`, and nothing else; each that spoke nothing asserts that the engine was given nothing.
      Its cases of the unsupported platform, the question and the press during a download stay, and show that the
      download of unit 0005 is unchanged (R14). Add, for a file of a text of its own (R15, R16, R17):
    - a download after which the file is read by itself: after the press the document is changed, and after the
      download work and the reading works ran, the engine was given the text of the press and no other
    - a download during which the file's tab was closed, as `fileClosed` of its target: nothing is read after it
    - the same with the project closed, as `projectClosed`
    - a download that fails and is tried again from its notification after the tab was closed: nothing is read
    - a press on an empty file without the model: no question, nothing requested, nothing handed over, and the
      notification of nothing to read
      See the test pass, and the whole suite with it
- [x] Rewrite the parts of `README.md` that this unit makes untrue. The introduction says that the plugin reads a
      Markdown file aloud: pressing the button reads the file from its beginning to its end as the editor holds it,
      unsaved changes included, with the text fixed at the press; the text is read as it is, Markdown markers
      included, which the engine may pronounce; while it reads, the button shows a stop icon and "Stop Reading" and
      stops the reading; one file is read at a time and a press on another file switches to it; the reading goes on
      on another tab and stops when the file's tab or project is closed or the IDE exits; an empty file gives a
      notification. The paragraph on presses names the stop in place of the ignored second press, and keeps that a
      press during a download is ignored. In the section on the model, "the sentence starts by itself" becomes that
      the file is read by itself, with the text of the press, unless its tab or project was closed meanwhile. The
      sentence on the automated test of the test sentence says that the tests that run the real engine, without
      playing anything, run only where the model is in place. No sentence describes the test sentence any more.
      Everything else stays (R19)
- [x] Apply the findings C1, C2 and S2 of the review of 2026-10-18T22:50:44Z, which the human accepted, see
      `record.md` of 2026-10-18T23:13:43Z, each test first. C1: in `SpeechServiceTest`, a failure of the player when
      it is closed at the end of the text ends the reading as any other failure does; seen failing, then
      `SpeechService` catches it. S2: in `SpeechServiceTest`, a stop while the player plays out the end of the text
      discards it; seen failing, then the reading keeps the player that plays out, and a stop discards it. C2: in
      `ReadAloudActionTest`, a press on a file that has no document and whose text cannot be loaded shows the error
      dialog and reads, asks and downloads nothing, with the model and without it; it passed at once, since it covers
      code that was there. Run `RealModelPackageTest` once with a fresh download of the real package
- [x] Write `EngineTextTest`, a plain JUnit 4 test of `withCharactersAboveFfffAsSpaces`, and see it fail to compile.
      The cases: the emoji U+1F600 alone gives one space; three emoji in a row give three spaces; `a`, U+1F600, `b`
      gives `a b`; U+20000, U+10330 and U+1D400 each give one space; `café`, `€`, `→`, U+2764 and U+2713 stay as
      they are; a text without such characters is returned equal; a half of a surrogate pair alone stays as it is.
      Then write `EngineText.kt` and see the test pass (R11)
- [x] In `ReadAloudActionTest`, add these cases with the stand-in service, and see them fail. The engine is given,
      for a file of `Before ` U+1F600 ` after.`, the text with three spaces in a row; for a file of three emoji in a
      row between two words, `one ` and ` two` around them, `one` and `two` with five spaces between; for a file of
      one emoji alone between two sentences, the sentences with a space in its place; and for a
      file of `café`, `€` and `→`, its text as it is (R11, R17). Then change `ReadAloudAction.kt` so that the press
      applies `withCharactersAboveFfffAsSpaces` to the text it took, before the check for an empty file, and hands
      that text on to `read` and to `readOnceTheModelIsInPlace`, and see them pass
- [x] Once the human has approved the correction of requirement 12 of 2026-10-19T00:13:30Z, and not before: in
      `ReadAloudActionTest`, a press on a file of nothing but emoji and white space shows the notification of nothing
      to read and hands nothing to the engine, with the model in place; and with an empty model folder, the same,
      and no question and no work kept by the download service. See it pass with the change of the task before,
      which already makes it so (R12)
- [x] In `KokoroSpeechEngineTest`, take the text of 600 emoji out of the case of long texts, since such characters no
      longer reach the engine and that text took about 3 of the 3.5 minutes of the suite, and put in its place 700
      of `→`, a character of three bytes that reaches the engine, so that the cut at 400 bytes is still made with
      characters of more than one byte. Add a case: `Before the sign, ` U+1F600 ` and after the sign.` through
      `withCharactersAboveFfffAsSpaces` gives less than 4 seconds of audio with the real engine, where the
      implementation measured 1.9 seconds for the sentence alone and 7.4 to 7.6 with such a character in it. Run it
      on this host and see it pass and not be skipped, and log the time the suite took (R11)
- [x] In `README.md`, where it says that the text is read as it is, add that characters above U+FFFF, among them most
      emoji, are left out of the reading, and that emoji and symbols of three bytes, such as ❤ and ✓, reach the
      engine and may be read by their names. Nothing else changes (R19)
- [x] In the sandbox IDE of `./gradlew runIde` on this host, with the model in place and the speakers on: press on a
      Markdown file of several pages and hear it start; type in another editor and open a menu while it reads; select
      another tab and a tool window and hear it go on; see the stop icon and "Stop Reading" on its tab and stop it;
      press on a second file during a reading and hear the switch; close the tab of a file that is read and hear it
      stop; close the project while a file is read and hear it stop; press on an empty file. Log what was seen and
      heard in `record.md`, and anything that differed (R5 to R9, R12)
- [x] Measure the first sound in the same sandbox IDE: a Markdown file of one sentence, and one of 10,000 words whose
      first sentence is that sentence, each pressed three times after a first press that loaded the model, each time
      read from the line `First sound <n> ms after the button was pressed` in the `idea.log` of the sandbox. Log the
      six times in `record.md`. The difference of the medians is at most 2,000 ms; where it is more, log it and tell
      the human before going on (R3)
- [x] Add the folder `samples/` with Markdown files for the checks by ear, added at the human's request, see
      `record.md` of 2026-10-19T10:09:33Z: `markdown-features.md`, prose of several pages with headings, bold, italic and
      strikethrough, bulleted, numbered and nested lists, a link, a bare address, a reference link and an image, inline
      code and a code block, a quote, a table, emoji of four bytes and symbols of three bytes; `one-sentence.md`;
      `ten-thousand-words.md`, which starts with the sentence of `one-sentence.md`; and `empty.md`. `README.md` says
      under "Running in a sandbox IDE" what the folder is for. No code changes
- [ ] In `TextPiecesTest`, add the cases of the cut at a clause, and see them fail: a sentence of more than 60 bytes
      before a comma, a colon and a semicolon each ends a piece after the white space that follows it; a comma that
      comes before 60 bytes ends nothing, also in a list of short words; a comma not followed by white space, as in
      `1,000`, ends nothing. Then change `piecesOf` and see the test pass. Measure the first six pieces of
      `samples/markdown-features.md` with the real engine again, as the trial of `record.md` of 2026-10-19T10:18:06Z did, run the
      suite, and log both. The human listens to the sample again to judge the melody at those cuts (R4)
- [ ] Hand the human what their checks of requirement 18 need, and write their answers into `record.md` as they give
      them: the archive of `./gradlew buildPlugin` to install on Linux x64 and on macOS on Apple Silicon; the steps,
      which are to press on a Markdown file of several pages with headings, lists, links and code, and listen to it
      from beginning to end, noting any silence in the middle of a sentence, and to press again and stop it halfway;
      and the questions the record has to answer for each machine: was there a silence in the middle of a sentence
      (R4), did the stop come at once, and how did the Markdown markers sound (R18)

## Verification

- [ ] `./gradlew clean buildPlugin` succeeds and `./gradlew test` passes, apart from the four tests named under
      "Where the work is done", with every test of the tasks above; `KokoroSpeechEngineTest` runs on this host and is
      not skipped (R17)
- [ ] `grep -rn 'AloudRite can speak' src/main README.md` finds nothing, and `grep -rn 'TEST_SENTENCE' src` finds
      nothing (R16, R19)
- [ ] `grep -rn 'Messages.show' src/main/kotlin/com/ritenoar/aloudrite/ReadAloudAction.kt` finds only the message of
      an unsupported platform, the sentence about another IDE window, the question of the download and the error of
      a file whose text cannot be loaded from disk, added during the implementation, see `record.md`: no dialog
      shows the content of the file (R2)
- [ ] `grep -rn 'withCharactersAboveFfffAsSpaces' src/main` finds the function and its one call, in
      `ReadAloudAction.kt` (R11)
- [ ] `git ls-files` shows no file ending in `.onnx`, `.bin` or `.tar.bz2`, and the model folder on this host holds
      what it held before
- [ ] `record.md` holds the checks of the sandbox IDE, the six times of the first sound, and the human's answers of
      requirement 18 for both platforms (R3, R18)
- [ ] `npm run check-changes` passes on the committed branch

## Commit Points

- [ ] `feature-0006 Cut the text into pieces the engine can take`: `TextPieces.kt` and `TextPiecesTest.kt`
- [ ] `feature-0006 Keep the audio ahead of the playing and let it be stopped`: `AudioAhead.kt`, the changes of
      `SamplePlayer.kt`, `AudioPlayer.kt`, `SpeechEngine.kt` and `KokoroSpeechEngine.kt`, and their tests
- [ ] `feature-0006 Read the file when the button is pressed, one file at a time`: `ReadingTarget.kt`,
      `SpeechService.kt`, `ReadingListeners.kt`, `plugin.xml`, `ReadAloudAction.kt`, `SpeechStandIns.kt`,
      `SpeechServiceTest.kt`, `ButtonPressSupport.kt`, `ReadAloudActionTest.kt` and `ModelDownloadFlowTest.kt`. One
      commit, because the new contract of the service changes the action and its tests at once, and the tree builds
      only with all of them
- [ ] `feature-0006 Describe the reading in the README`: `README.md`
- [ ] `feature-0006 Leave characters above U+FFFF out of the reading, as a space`: `EngineText.kt`,
      `EngineTextTest.kt`, `ReadAloudAction.kt`, `ReadAloudActionTest.kt`, `KokoroSpeechEngineTest.kt` and
      `README.md`. The case of a file of nothing but emoji goes in a commit of its own,
      `feature-0006 Count a file of nothing but emoji as empty`, once the correction of requirement 12 is approved
- [ ] `feature-0006 Add Markdown files for the checks by ear`: the folder `samples/` and `README.md`
- [ ] `feature-0006 Cut a long sentence at a comma, a colon or a semicolon`: `TextPieces.kt` and `TextPiecesTest.kt`
- [ ] `feature-0006 Record the implementation`: `record.md` and the ticked boxes of this plan
