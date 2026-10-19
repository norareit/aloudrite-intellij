---
kind: feature
status: in-progress
timestamp: 2026-10-18T21:47:44Z
---

# Read the Markdown file aloud

## Log

- 2026-10-08T16:49:34Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, wrote `spec.md`, as the third of the three units the Kokoro work was split into with the human. It was
  written before units 0004 and 0005 were built, at the human's instruction, and both require that it is revised
  when they are done. It needs units 0003 and 0004 implemented. Unit 0003 is implemented on its branch and not on
  `master` at the time of writing. My own choices, which the human has not stated: the same button stops the
  reading, one reading at a time, reading goes on when another tab is selected and stops when the file's tab is
  closed, the text is fixed at the moment of pressing and read raw, no pausing, and the 2 seconds of requirement
  3, which are a guess. Status left at `draft` for the human to approve.
- 2026-10-08T19:32:06Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, brought `spec.md` in line with a change the human asked for in unit 0004: the test sentence of that unit
  is started with the "Read Aloud" button, in place of the dialog, and not with an action in the Tools menu. The
  context, requirements 2, 12, 13 and 16 and two decisions are reworded, and no requirement is renumbered. Status
  still `draft`.
- 2026-10-08T22:51:21Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, revised `spec.md` against what unit 0004 established, as requirement 19 of that unit asks, reading its
  record, plan and README on its branch at `f21f947`. The context now states what was measured and heard. The 2
  seconds of requirement 3 stay, and are no longer called a guess: the first sound of one sentence came in 640 to
  927 ms with the model loaded. New requirements: reading does not fall silent in the middle because the engine
  is behind, stated for the developer's two machines (4), and a failure during a reading puts the button back
  (13). The tests also cover a reading that stops when its tab is closed (16), and the human's check records how
  the Markdown markers sounded (17). The requirements are renumbered, from 16 to 18. New non-goal: languages other
  than English. New under "Beyond the request": loading the model before the first press. Not established by
  unit 0004 and still open here: how the engine speaks Markdown markers, and how it behaves on a text of several
  pages. The human has not read the revision yet. Status still `draft`.
- 2026-10-18T20:32:43Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the macOS
  host, on `master` at `545af1f`, revised `spec.md` against what unit 0005 established, as requirement 19 of that
  unit asks, at the human's request. Unit 0005 is `in-progress` and not merged, so I read its `spec.md`, `record.md`,
  `README.md` and `src/main` on its branch at `64d1b33`: the record says its implementation, reviews and the human's
  checks on both platforms are done. Changed because of what was found: the context says that this unit needs unit
  0005, which changes the same button, and states what that unit established, in place of the line that this
  spec would change once more; requirement 14 names the question and download of unit 0005 in place of "where that
  unit is implemented"; new requirement 15, the file is read by itself after a download, since unit 0005 starts the
  speech the press asked for without a second press, and in this unit that speech is the file; requirement 16 says
  the test sentence is gone also after a download; the tests (17) cover the reading after a download; the README
  (19) describes what happens after a download. Decided by me and open to the human: what is read after a download
  is the text of the moment of pressing (15); nothing is read when the file's tab or its project was closed during
  the download (15); a press on an empty file asks no question and downloads nothing (12); and a press during a
  download stays ignored, named as a non-goal. The requirements are renumbered from 18 to 19, and the reference to
  the human's checks in the decision on raw text follows. New under "Beyond the request": the developer's check of
  a reading after a real download. The human has not read the revision yet. Status still `draft`
- 2026-10-18T20:57:20Z [human] approved the spec as it stands, in the main session on the Linux host, after reading
  it. None of the items under "Beyond the request" is taken into this unit. The human wants the first two, pausing
  and resuming and reading from the caret or the selection, to become features of their own, and asked to be
  reminded to have their specs written once this unit is implemented. The status is `approved`. The status change
  and this entry were written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-18T21:20:13Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux host, on
  the branch `feature-0006-read-markdown-aloud`, cut from `master` at `1f8ed00`, wrote `plan.md`. A trial with the
  real engine, in a copy of the tree outside the repository that was deleted after, found that one call of the engine with
  more than about 1,500 characters ends the process with SIGSEGV, whatever the text, so the plan has the plugin cut
  the text into pieces of at most 400 bytes; the trial is described in the plan. Decided by me and open to the
  human: a press on an empty file while another file is read leaves that reading going, since the spec says only that
  the press plays nothing; and a file open in two split editors is read on until its last tab is closed. Found
  before any change: on this host `./gradlew test` on `1f8ed00` fails four tests of unit 0005, those that start a
  second Java process, with `error=7, Argument list too long`. They are not this unit's, the plan leaves them alone,
  and the human decides whether a bug unit is written for them. The status is `planned`
- 2026-10-18T21:47:44Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, on the branch `feature-0006-read-markdown-aloud` at `972907f`, began the implementation. The status is
  `in-progress`
- 2026-10-18T22:05:39Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, changed the fourth task of `plan.md`: the case of `KokoroSpeechEngineTest` that makes every piece of four
  long texts now makes each distinct piece once. Run with every piece, it took over 18 minutes, because a piece of
  100 emoji, of which the text of 600 emoji has six that are the same, is spoken as the names of the emoji: one
  such piece gave 467 seconds of audio and took 185 seconds to make. The pieces of the other three texts, measured
  in the same run, each took 3.1 to 8.7 seconds for 8.1 to 22.1 seconds of audio, and none crashed. Making each
  distinct piece once still makes every kind of piece the case is about. That emoji are read by their names is
  worth knowing for the unit that cleans the Markdown
- 2026-10-18T22:14:45Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, did the tasks of `plan.md` up to the README, in four commits from `08d875c` to `d016840`. On the tree of
  `d016840`, `./gradlew clean test` ran 166 tests: one skipped (`RealModelPackageTest`) and four failed, the four
  named in the plan, each with `error: 7 (Argument list too long)` when it starts a second Java process.
  `KokoroSpeechEngineTest` ran its three cases with the real engine and passed. On the tree of `7ea81f7`, built in a
  separate worktree with the interim change to the `SpeechService` of unit 0004 that the fourth task asks for, the
  suite ran 136 tests with the same one skipped and the same four failed. `SpeechServiceTest` and `AudioAheadTest`,
  which use real threads, passed five runs in a row on `d016840`'s tree, while another build ran on the host. The
  greps of the verification find no `AloudRite can speak` in `src/main` and `README.md` and no `TEST_SENTENCE` in
  `src`, `git ls-files` shows no model file, and the model folder was only read. What differs from the plan, besides
  the change logged at 2026-10-18T22:05:39Z:
  * The tests were written before the code they cover, but the runs that the plan asks for to see
    `SpeechServiceTest` and `ReadAloudActionTest` fail to compile were not made on their own: the first compile came
    after the code was written. Two cases failed at their first run, because of mistakes in the tests, and were
    corrected: a `LightVirtualFile` never counts as an unsaved document, so the case of the unsaved change checks
    that the file still holds the saved text; and a case of `SpeechServiceTest` looked at the players after the
    retry had made one
  * `NOTIFICATION_GROUP_ID` of `ModelDownloadService.kt` is no longer private, and the notification of nothing to
    read uses it, so that the group is not named a second time. Added to the files of the plan
  * A press on a file that has no document, whose text is then read from disk, shows the error dialog
    `AloudRite could not read <file name> aloud: <message>` when that read fails. The plan did not say what happens
    then. Added to the verification of the plan, since its grep for `Messages.show` now finds this dialog as well.
    No case tests it
  * For the case of a download that fails and is tried again, the failure is a folder in the way of the model,
    since the server stand-in cannot serve a second answer at the same address
  * A carriage return between two line breaks still makes a blank line in `piecesOf`, so that a file read from disk
    with Windows line ends is cut like any other. Not tested with such a file
  * A stop pressed while the audio line plays out the end of a file, after the last piece was written, puts the
    button back at once but lets the sound already in the line play out, since `close` drains the line. How long
    that is depends on the buffer of the line, which was not measured. Open to the human
  Not done yet: the checks in the sandbox IDE, the timing of the first sound and the human's checks of requirement
  18, which need the human at the speakers. The four failing tests are for the human to decide on, as the plan says
- 2026-10-18T22:50:44Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit
  `0006-read-markdown-aloud` at `5a7d18180bc84a2013c79b17d7c154c4140ddb8f`. This was its first/full review,
  against the merge base with `master`, `1f8ed00d79199c19e02005405d14cb78c01f8313`, with no prior review to
  compare. Returned finding labels S1, S2, P1, C1 and C2. Saved the complete findings in
  `.reviews/0006-read-markdown-aloud/2026-10-18T224909Z-opencode-gpt-6.1-sol-full.md`.
  Independently ran the full suite twice: each reported 166 tests, four failures launching LockProbe with
  `Argument list too long`, and one skipped RealModelPackageTest; all three real-engine cases ran and passed.
  `./gradlew clean buildPlugin`, `npm run check-changes` and the merge-base diff whitespace check passed;
  SpeechServiceTest and AudioAheadTest passed five consecutive targeted runs. Also ran two in-memory lifecycle
  probes without changing source or tests. Not verified: sandbox IDE interactions, actual default-output stop
  latency, the six first-sound timings, the human's listening checks on either platform, macOS execution, a real
  download, the historical baseline/interim suite runs and deleted trial measurements, uncommitted test-writing
  order, or the model folder's earlier contents. Details and limits are in the saved findings. No fix or commit
  was made
- 2026-10-18T23:13:43Z [human] accepted the findings C1, C2, S2 and P1 of the review of 2026-10-18T22:50:44Z,
  to be applied in this unit, after the implementer's assessment of each; S1 is answered by the open tasks of the
  sandbox IDE, the first sound and requirement 18. The four tests that fail with `error: 7` are to get a bug unit of
  their own: the implementer measured the classpath that `LockProbe` passes as one argument at 268,909 characters
  in 1,696 entries on this host, above the 131,072 bytes that Linux allows for one argument, and unit 0005 ran its
  last full suite on macOS only. `RealModelPackageTest`, which is skipped without `ALOUDRITE_MODEL_PACKAGE`, is to be
  run once with a fresh download of the real package. This entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word
- 2026-10-18T23:20:43Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, applied the findings of the review of 2026-10-18T22:50:44Z that the human accepted at 2026-10-18T23:13:43Z,
  each with its test written first, and added that work to `plan.md` as a task:
  * C1, applied: a failure of the player when it is closed at the end of the text, which is where the line is
    drained, now ends the reading as any other failure does: the reading ends, the button is put back, the failure
    is reported once with its target, and the next press reads. The new case of `SpeechServiceTest` failed before
    the fix with the `drain failed` it throws, and passes after it
  * S2, applied: the reading keeps the player that plays out the end of the text, and a stop then discards it, so
    that a stop at the end ends the sound as it does anywhere else. The new case of `SpeechServiceTest` failed
    before the fix, since nothing discarded the player, and passes after it. That a real line returns from `drain`
    when another thread stops it was not checked; the sandbox IDE check of a stop at the end of a file shows it
  * C2, applied: a new case of `ReadAloudActionTest` presses on a file that has no document and whose text cannot
    be loaded, with the model in place and without it. Each press shows `AloudRite could not read notes.md aloud:
    The disk is gone`, and nothing is read, asked, downloaded or notified. It passed as soon as the stand-in file
    itself was right, since it covers code that was there already
  * P1, applied: the two tasks of `plan.md` that ask to see a test fail to compile now say that the run was not made,
    and refer to this record. No run is claimed after the fact
  * S1, open: it is answered by the three tasks still open in `plan.md`, which are the human's and need the human at
    the speakers. By the human's decision they are done after the review, not before it: the checks in the sandbox
    IDE, the six times of the first sound, and the human's listening checks of requirement 18 on Linux x64 and on
    macOS on Apple Silicon. Until they are done and recorded, the unit is not `implemented`
  `RealModelPackageTest`, run once with `ALOUDRITE_MODEL_PACKAGE` naming the real package, downloaded with `curl`
  from the address of the README into a scratch directory outside the repository, with its SHA-256 matching, ran and
  was not skipped, and passed: installing it took 8,744 ms. On the final tree, `./gradlew clean buildPlugin`
  succeeded and `./gradlew test` ran 169 tests: one skipped (`RealModelPackageTest`, without its environment
  variable) and four failed, the four of unit 0009, each with `error: 7 (Argument list too long)`. Of the 208
  seconds the tests took, 200 went to the case of `KokoroSpeechEngineTest` that makes the long pieces with the real
  engine, most of it to the piece of emoji. The status stays `in-progress`
- 2026-10-18T23:42:40Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, corrected a claim of the entry of 2026-10-18T22:05:39Z and of a comment in `KokoroSpeechEngineTest`, which
  said that a piece of emoji is spoken as the names of the emoji. That was inferred and not heard, and it is wrong.
  Measured with the real engine: a piece of 100 characters of four bytes gave 467.0 seconds of audio, made in 181 to
  185 seconds, alike for U+20000 (a CJK character), U+10330 (a Gothic letter), U+1D400 (a mathematical bold A) and
  the emoji U+1F600. In the sentence "Before the sign, and after the sign.", which gives 1.9 seconds of audio alone,
  one such character in the middle gave 7.4 to 7.6 seconds, three emoji 18.6 seconds, and one emoji alone 5.8
  seconds. The human listened to the sentence alone, with one emoji and with the CJK character, on this host: between
  the two halves of the sentence the engine says something like "ayo I to have", about five times, and it sounds
  exactly the same for the emoji and for the CJK character. So the engine reads each character of four bytes that
  it does not know as the same babble of about 5.7 seconds. This unit reads the text as it is (requirement 11), so
  it keeps that babble; it is for the unit that cleans the Markdown. The samples were made by a throwaway test,
  deleted after, into a scratch directory outside the repository
- 2026-10-19T00:03:22Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, found the cause of the babble of the entry before, with the human. Requirement 1 does not hold for a file
  that holds a character above U+FFFF, a character of four bytes in UTF-8, among them most emoji, and the human
  decided that this is fixed in this unit, which needs a correction of requirement 11 first. Established:
  * The model is not the cause. The same texts through the Python pipeline of `kokoro` in `kokoro-env/`, the one
    `speak.py` uses, offline and with the voice `af_sarah` since `af_heart` is not in the local cache, gave the
    phonemes of "grinning face" for U+1F600, "letter two zero zero zero zero" for U+20000, "letter one zero three
    three zero" for U+10330 and "A" for U+1D400, in 3.4 to 5.2 seconds of audio against 3.0 for the sentence alone.
    The human listened to them: "just fine". `espeak-ng` alone, with the data of the model and the voice `en-us`,
    reads the same four characters the same way, apart from U+1D400, which it reads as "letter one D four zero zero"
  * The cause is the hand-over in the Java binding of sherpa-onnx. `offline-tts.cc` under `sherpa-onnx/jni/` of
    sherpa-onnx, at the tag `v1.13.8` that the plugin uses and on `master` on this date, converts the text with
    `GetStringUTFChars`, which gives modified UTF-8. In it, a character above U+FFFF becomes the two halves of its
    surrogate pair, each written as three bytes, which is not valid UTF-8, so the native code never gets the
    character. That every such character gives the same babble of the same length fits this. No Java string makes
    `GetStringUTFChars` give a valid four-byte UTF-8 sequence, and a newer sherpa-onnx does not change it, so no
    change in the plugin can hand such a character to the engine intact
  * Characters of two and three bytes come through: with the real engine, the sentence with "café" gave 2.3 seconds
    of audio, with "€" 2.3 seconds and with "→" 2.5 seconds, against 1.9 seconds alone and 7.4 to 7.6 seconds with
    one character above U+FFFF. Those three samples were measured and not listened to
  Every fix in the plugin therefore changes the text the engine is given, which requirement 11 forbids as it stands.
  The human rejects reading such a character by its name and replacing it with a fixed word, and prefers leaving it
  out or a pause. The implementer recommends replacing each such character with a space: it leaves it out without
  joining the words on its two sides, while a pause takes either a comma, which changes the melody, or silence that
  the plugin cuts the text for and inserts, which is more code for little effect. To be stated in the spec as a
  consequence: characters of three bytes, among them older emoji and symbols such as U+2764 and U+2713, reach the
  engine and may be read by their names, so some emoji would be silent and others named. Also seen: once such
  characters no longer reach the engine, the piece of 100 emoji of `KokoroSpeechEngineTest`, which takes about 3 of
  the 3.5 minutes of the suite, has no reason to stay. The human gives this to the specifier, who corrects
  requirement 11 once the human has decided between leaving out and a pause. Nothing of the fix is implemented
  until the corrected spec is approved. The samples and the Python script were made in a scratch directory outside
  the repository, and the throwaway tests that made the samples of the plugin were deleted. The status stays
  `in-progress`
- 2026-10-19T00:05:10Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on the branch `feature-0006-read-markdown-aloud` at `007a792`, corrected `spec.md` against the two entries
  before, at the human's request, so that the characters above U+FFFF are handled in this unit. The human chose
  between leaving them out and a pause, when asked: left out, as a space. Changed: the context states what the
  implementation found about the binding of sherpa-onnx; requirement 11 makes each character above U+FFFF one space
  and nothing else; the tests (17) cover requirement 11, with the cases named there; the README (19) says that such
  characters are left out; a new decision gives the reasons and the alternatives rejected, among them the human's
  rejection of a name and of a fixed word, and states the consequence that emoji of three bytes may be named; the
  decision on raw text points to it; two new non-goals, making all emoji alike and reading such a character by its
  name or as a word. No requirement is renumbered. Decided by me and open to the human: characters of two and three
  bytes are left alone, and a surrogate without its other half is not handled, named under "Beyond the request".
  For the plan, not the spec: whether the piece of 100 emoji of `KokoroSpeechEngineTest` stays. The status stays
  `in-progress`, and nothing of the fix is implemented until the human has approved the correction
- 2026-10-19T00:12:12Z [human] approved the correction of `spec.md` of 2026-10-19T00:05:10Z as it stands, in the
  main session on the Linux host, when asked by the planner. Asked by the planner about a case the correction leaves
  open, a file that holds nothing but characters above U+FFFF and white space, the human decided that it counts as
  empty for requirement 12: the notification that there is nothing to read, no question and no download. The spec
  does not say so yet, and the human gives it to the specifier to correct requirement 12. The status stays
  `in-progress`. This entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-19T00:12:12Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux
  host, on the branch `feature-0006-read-markdown-aloud` at `4295d1a`, changed `plan.md` for the correction of the
  spec. New: the pure function `withCharactersAboveFfffAsSpaces` in `EngineText.kt`, applied once by the action to
  the text of the press, before the check for an empty file, so that the service and `piecesOf` do not change; its
  test, and the cases of requirement 17 in `ReadAloudActionTest` with the stand-in engine; in
  `KokoroSpeechEngineTest`, the text of 600 emoji goes from the case of long texts, as the specifier left to the
  plan, and 700 of `→` take its place, with a new case that an emoji in a sentence no longer adds the babble; the
  README sentence of requirement 19; and a commit point. The task for a file of nothing but emoji waits for the
  correction of requirement 12, and goes in a commit of its own. Decided by me: a half of a surrogate pair alone is
  left as it is, as the spec names it beyond the request. The new tasks are unticked and the earlier ones stay as
  they were. The status stays `in-progress`
- 2026-10-19T00:13:30Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on the branch `feature-0006-read-markdown-aloud` at `73b7624`, corrected requirement 12 of `spec.md` as the
  human decided in the entry of 2026-10-19T00:12:12Z, at the human's request: a file that holds only white space and
  characters above U+FFFF counts as empty, with the notification, no question and no download. The decision on the
  notification for an empty file says why. Nothing else changes, and no requirement is renumbered. The status stays
  `in-progress`, and the task of a file of nothing but emoji waits until the human has approved the correction
- 2026-10-19T00:14:41Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux host, on the
  branch `feature-0006-read-markdown-aloud` at `d7f0633`, brought `plan.md` in line with the correction of
  requirement 12 of 2026-10-19T00:13:30Z, which says what the plan already planned for a file of nothing but
  characters above U+FFFF and white space. Only the wording that waited for the correction changed: the task of such
  a file, and its commit, now wait for the human's approval of it. No task was added or removed. The status stays
  `in-progress`
- 2026-10-19T00:15:19Z [human] approved the correction of requirement 12 of 2026-10-19T00:13:30Z, in the main session on the
  Linux host, when asked by the planner. The task of a file of nothing but characters above U+FFFF and white space
  may now be built. The status stays `in-progress`. This entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word
- 2026-10-19T00:19:11Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, implemented the correction of requirements 11 and 12 of 2026-10-19, with the five tasks the plan added for
  it, in the commits `1517467` and `328af81`, as its commit points name them. In order, with the runs the plan asks
  for:
  * `EngineTextTest` was written first and seen failing to compile, since `withCharactersAboveFfffAsSpaces` did not
    exist; with `EngineText.kt` written, its seven cases pass
  * The four cases of requirement 11 in `ReadAloudActionTest` were seen failing before the action changed, the
    three with emoji with the emoji still in the text given to the engine; the case of `café`, `€` and `→` passed
    already, since nothing changes for those characters. With the action applying the function to the text it took,
    before the check for an empty file, all pass
  * The two cases of a file of nothing but emoji and white space, with the model and without it, were written after
    the human approved the correction of requirement 12 at 2026-10-19T00:15:19Z, and passed at once, as the plan
    expected, since the change of the task before already makes such a file empty
  * `KokoroSpeechEngineTest` makes 700 `→` in place of 600 emoji, and has a new case: the sentence `Before the sign,
    ` U+1F600 ` and after the sign.`, through the function, gives less than 4 seconds of audio with the real engine.
    Its four cases ran on this host, none skipped, and passed in 26.1 seconds, where the class took 203.1 seconds
    before
  * `README.md` says that characters above U+FFFF, among them most emoji, are left out of the reading, and that
    emoji and symbols of three bytes, such as ❤ and ✓, reach the engine and may be read by their names
  `grep -rn 'withCharactersAboveFfffAsSpaces' src/main` finds the function and its one call, in `ReadAloudAction.kt`.
  On the final tree, `./gradlew clean buildPlugin` succeeded and `./gradlew test` ran 183 tests in 31 seconds of test
  time, where the suite took 208 before: one skipped (`RealModelPackageTest`, without its environment variable) and
  four failed, the four of unit 0009, each with `error: 7 (Argument list too long)`. The first of the two commits
  holds `ReadAloudActionTest` without the two cases of a file of nothing but emoji, so that they are a commit of their
  own; with them taken out, `ReadAloudActionTest` ran its 21 cases and passed. Not checked: what a reading with emoji
  sounds like in the IDE, which the human's checks will show.
  Still to do, as decided by the human, after the review of this work and not before: the checks in the sandbox IDE,
  the six times of the first sound, and the human's listening checks of requirement 18 on Linux x64 and on macOS on
  Apple Silicon. Until they are done and recorded, the unit is not `implemented`. The status stays `in-progress`
- 2026-10-19T00:30:33Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit
  `0006-read-markdown-aloud` at `a9b718e0a4973786d3ac80369ae027305d758263`. This was a delta review from
  `5a7d18180bc84a2013c79b17d7c154c4140ddb8f`, covering the accepted findings of the first review and the
  intervening changes on all three axes. Returned no new finding labels. Saved the complete delta report in
  `.reviews/0006-read-markdown-aloud/2026-10-19T002913Z-opencode-gpt-6.1-sol-delta.md`.
  Independently ran `./gradlew clean buildPlugin`, which passed, and `./gradlew test`, which reported 183 tests:
  178 passed, four failed launching LockProbe with `error: 7 (Argument list too long)`, and one skipped
  RealModelPackageTest because its package file was not supplied. All four real-engine cases ran and passed.
  `npm run check-changes` and the delta and merge-base whitespace checks passed. Additional in-memory probes
  exercised drain failure, cross-thread cancellation during drain and the new character transformation; details
  are in the report. Not verified firsthand: physical audio output and concurrent drain/discard on a real provider,
  sandbox IDE interactions, the six first-sound timings, Linux/macOS R18 listening checks, earlier approval
  conversations, the historical real-package run, deleted diagnostic experiments or intermediate red-test runs.
  The three manual tasks remain scheduled after review as recorded. No fix, status change or commit was made
- 2026-10-19T09:33:36Z [Claude Code, anthropic/claude-opus-5-5] in the main session on the Linux host, at the human's
  request, recorded the report upstream of the cause behind requirement 11. The human reported it to sherpa-onnx as
  https://github.com/k2-fsa/sherpa-onnx/issues/4060 and submitted a fix as
  https://github.com/k2-fsa/sherpa-onnx/pull/4062, which reads the text of the TTS functions of the Java binding as
  standard UTF-8. The pull request is open and not merged on this date. Before it, the human confirmed by listening
  that the babble comes from the Java binding only: the same sentence with U+1F600 through the Python API of
  sherpa-onnx 1.13.8 with the same model was read without it. The human asked to be reminded, once this unit is
  implemented, to have a spec written for moving the plugin to the first release of sherpa-onnx that holds the fix
  and removing the workarounds for characters above U+FFFF; the latest release on this date is 1.13.8 of
  2026-09-10. Nothing else changes, and the status stays `in-progress`
- 2026-10-19T09:51:39Z [human] made the checks of the sandbox IDE of `plan.md` on the Linux host, with the model in place and
  the speakers on, in the sandbox IDE of `./gradlew runIde` on `1b77664`, with a project of four Markdown files: one
  of several pages with headings, bold and italic, a bulleted and a numbered list, links, a code block and a quote
  with an emoji, of 1,253 words; one of one sentence; one of 10,026 words that starts with that sentence; and an
  empty one. What the human found:
  * The file of several pages was read, and the IDE stayed usable while it read: typing in another editor and the
    menus worked. The reading went on when another tab and a tool window were selected. The log of the sandbox gives
    `First sound 1288 ms after the button was pressed` for that press, the first after the IDE started, which loaded
    the model
  * While it read, the button on its tab showed the stop icon and the tooltip "Stop Reading". Pressing it stopped the
    reading, and the button showed the loudspeaker again
  * A press on the file of one sentence while the other was read stopped the first and read the second
  * Closing the tab of the file that was read stopped the sound at once, and so did closing its project. Closing the
    project, the only one open, also ended the sandbox IDE, which is what the platform does then, and the IDE was
    started again for the remaining checks
  * A press on the empty file showed the notification that there is nothing to read
  * While it read, the human heard the fans of the machine go up, and saw the processor go from about 50 to 70 to
    75 degrees Celsius. Noted for the question of the number of threads of the engine, which the human may want as a
    unit of its own
  Heard of the Markdown, for requirement 18 and the unit that cleans the Markdown, which the human asked to collect
  here as a list:
  * Headings: the `#` is read as "hashtag"
  * Bold and italic: the asterisks are read out, as "asterisk asterisk bold asterisk asterisk". Strikethrough was not
    in the file and is likely read out alike
  * Bulleted lists: the items run together as one sentence. The human wants a pause between the items
  * Numbered lists: they sound good, since the voice pauses at each new number as at the end of a sentence
  * Code blocks: the language of the block, `kotlin`, is read out, and then the code as one sentence. The human wants
    the code skipped, with something like "code example" in its place and a pause after it long enough to pause the
    reading by hand, which touches the feature of pausing the human wants as a unit of its own
  * Links: the whole address is read out. The human asked between leaving the address out and putting something like
    "see link" in its place; the implementer proposed reading only the text of a link and leaving the address out,
    since the text is part of the sentence, with a short word such as "link" or the domain only for an address that
    has no text of its own, the alternative text of an image read or left out, and the definitions of reference
    links left out. Not yet decided by the human
  * The quote with an emoji at the end of the file was not heard yet
  Written by Claude Code, anthropic/claude-opus-5-5, as the implementer, on the human's word

## Amendments
