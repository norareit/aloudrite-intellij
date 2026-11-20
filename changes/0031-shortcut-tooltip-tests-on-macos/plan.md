# The tests of the shortcuts in tooltips pass on macOS

## Approach

The keystrokes that the plugin gives its four actions in the keymaps of Linux, and the rule that the keymap of macOS
has `meta` for `ctrl`, are held today in the companion and in the keymap test of `ReadingShortcutsTest`. They move to a
file of their own in the tests, unchanged, so that the tooltip tests can use them too. That move is a refactoring and
is its own commit, after which the suite behaves as before.

Then that file gains one function, the text of the shortcut that the plugin gives an action in the keymap in use. It
takes the keystroke of Linux from the table, turns `ctrl` into `meta` where the keymap in use is "Mac OS X 10.5+" or
has it among its parents, as the keymap of the user that `withAKeymapOfTheUser` derives from it does, and gives the
text through `shortcutText`. The 8 tests expect that text in place of the `shortcutText("ctrl alt shift ...")` they
call now. The shortcut of the user's own, `ctrl alt shift F11`, keeps going through `shortcutText`, since the tests set
it the same on both platforms (R5).

The function reads the name of the keymap in use and its parents, and never the shortcuts of the keymap or of the
action, so the shortcut a test expects never comes from what the plugin declared (R4).

## Files

- `src/test/kotlin/com/ritenoar/aloudrite/PluginShortcuts.kt`: new, the table of keystrokes, the name of the keymap of
  macOS, the keystroke of an action in a keymap given by its name, and the text of the shortcut of an action in the
  keymap in use
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingShortcutsTest.kt`: the companion loses the table and the name of the
  keymap of macOS, and the keymap test takes the keystroke from `PluginShortcuts.kt`; `READ_ALOUD_KEY` and `STOP_KEY`
  give the text of the keymap in use
- `src/test/kotlin/com/ritenoar/aloudrite/ReadFromTheCaretTest.kt`: `SHORTCUT` gives the text of the keymap in use
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingPanelTest.kt`: the shortcuts of "Slower" and "Faster" of the keymap in
  use
- `src/test/kotlin/com/ritenoar/aloudrite/ReadingTitleActionsTest.kt`: the shortcuts of "Pause Reading", "Resume
  Reading" and "Stop Reading" of the keymap in use

## Tasks

- [x] Run the four test classes on the branch on macOS and confirm that the 8 tests of the spec fail with the messages
  it gives, and that the other tests of those classes pass (R3)
- [x] Move the table of keystrokes, the name of the keymap of macOS and the rule of `meta` for `ctrl` from
  `ReadingShortcutsTest` to `PluginShortcuts.kt`, unchanged, and have the keymap test use them. Run the four classes:
  the same 8 fail and the rest pass, `inTheDefaultKeymapsOfLinuxAndMacOsEachActionHasItsShortcutAloneAndNoOther` and
  `inEveryOtherKeymapTheActionsHaveNoShortcut` among them (R4)
- [x] Commit: the move
- [x] Add to `PluginShortcuts.kt` the text of the shortcut of an action in the keymap in use, from its name and the
  names of its parents only (R4)
- [x] Have the 8 tests expect that text in place of the shortcut of Linux, and leave the shortcut of the user's own as
  it is (R1, R4, R5)
- [x] Run the four classes on macOS: the 8 tests pass. Then run `./gradlew test` on macOS with both models in place
  and record the counts (R1)
- [x] Check by hand on macOS that the tests still fail where a tooltip names another shortcut: with the keystroke of
  "Read Aloud" in the table changed to another key for one run, the tooltip tests of "Read Aloud" fail, and with the
  table back they pass. The change is not committed (R4)
- [x] Check with `git diff` from `master` that nothing under `src/main` changed (R6)
- [x] Commit: the fix
- [ ] Log in `record.md` that the run on Linux x64 is not made on this machine, and ask the developer for it (R2)

## Verification

- [x] The 8 tests fail on the branch before the fix and pass after it on macOS (R1, R3)
- [x] `./gradlew test` on macOS passes with both models in place (R1)
- [ ] `./gradlew test` on Linux x64 passes, run by the developer (R2)
- [x] Nothing under `src/main` changed (R6)
- [ ] `npm run check-changes` passes

## Commit Points

- [x] `bugfix-0031` The plan
- [x] `bugfix-0031` Move the keystrokes of the plugin's shortcuts to a file of the tests
- [x] `bugfix-0031` Expect the shortcut of the keymap in use in the tooltip tests
- [ ] `bugfix-0031` The record of the runs
