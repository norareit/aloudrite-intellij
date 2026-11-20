# The tests of the shortcuts in tooltips pass on macOS

## Context

The plugin gives its four actions, "Read Aloud, Pause or Resume", "Stop Reading", "Read Slower" and "Read Faster", a
shortcut in the default keymaps of Linux and macOS. On Linux it is Ctrl+Alt+Shift with a key, and in the keymap
"Mac OS X 10.5+", which the IDE names "macOS", it is Cmd+Alt+Shift with the same key, so that each platform has the
shortcut its users expect. Several buttons name the shortcut of their action in their tooltip, and the tests check
that text.

The tests had only been run on Linux x64. The suite on macOS on Apple Silicon was first run for unit 0030, on
2026-11-20, and 8 of these tests failed there. They fail in the same way on `master`, which holds nothing of unit
0030, so they are not a fault of that unit. Units 0013, 0017 and 0024 added them, and all three are implemented.

Only the tests are affected. The tooltips on macOS name the Cmd shortcut that the plugin gives the keymap of macOS,
which is what the plugin is meant to do, and the test that checks the shortcuts of the default keymaps themselves
passes on macOS.

## Observed behavior

On macOS on Apple Silicon, `./gradlew test` reports these 8 tests as failed:

* `ReadFromTheCaretTest.theTooltipStaysReadAloudWhenTheCaretMovesOrASelectionIsMade`, from unit 0013
* `ReadFromTheCaretTest.theTooltipIsReadAloudWhateverAPressWillReadAndNamesTheShortcut`, from unit 0013
* `ReadingPanelTest.theTooltipsFollowAShortcutTheUserGaveAndOneTheUserRemoved`, from unit 0017
* `ReadingShortcutsTest.theTooltipNamesAShortcutTheUserGaveTheFirstOfSeveralAndNoneWhereTheShortcutsAreRemoved`,
  from unit 0017
* `ReadingShortcutsTest.theTooltipsOfTheTabButtonsNameTheShortcutsOfTheirActions`, from unit 0017
* `ReadingPanelTest.theTooltipsOfTheSpeedButtonsNameTheShortcutsOfTheirActionsAndTheyHaveNoPlainTooltip`, from unit
  0024
* `ReadingTitleActionsTest.theTooltipsFollowAShortcutTheUserRemoved`, from unit 0024
* `ReadingTitleActionsTest.theTooltipsNameTheShortcutsOfTheActionsTheyStandFor`, from unit 0024

Each fails with an assertion such as `expected:<(Read Aloud, ⌃⌥⇧Space)> but was:<(Read Aloud, ⌥⇧⌘Space)>`, or the
same for "Stop Reading", "Pause Reading", "Slower" and "Faster" with their keys.

## Expected behavior

The 8 tests pass on macOS on Apple Silicon, as they pass on Linux x64, and each still fails when a tooltip names
another shortcut than the one the plugin gives its action in the keymap in use.

## Steps to reproduce

On macOS on Apple Silicon, on `master` at `8e716ff`:

```sh
./gradlew test --tests '*ReadingShortcutsTest' --tests '*ReadingTitleActionsTest' --tests '*ReadingPanelTest' --tests '*ReadFromTheCaretTest'
```

This reports 104 tests completed and the 8 above failed, with the messages above. It was run on an Apple M3 Max with
macOS 15.1.1.

## Root cause

Reproduced on the Mac above, on `8e716ff`, earlier on `b4d5317`, and on the branch of unit 0030 at `f1aedc3`.

The tests build the text they expect from the keystroke of Linux, written in the tests as `ctrl alt shift SPACE`,
`ctrl alt shift O`, `ctrl alt shift COMMA` and `ctrl alt shift PERIOD`, and turn it into the text the IDE shows. On
macOS the tooltip names `⌥⇧⌘`, which is Cmd+Alt+Shift, the shortcut the plugin gives the keymap "Mac OS X 10.5+" and
no other keymap. So the keymap in use in the tests on macOS is that of macOS, or one derived from it, and the tooltip
is right while the expectation is the shortcut of Linux. Which keymap the test environment makes active was read
from that text, and was not printed by a test.

The tests that give the action a shortcut of the user's own, Ctrl+Alt+Shift+F11, expect that shortcut on both
platforms and are right. They fail only at their other assertions, which expect the shortcut of Linux.

## Fix

Each of the 8 tests expects the shortcut that the plugin gives its action in the keymap in use: Cmd+Alt+Shift with the
key where that keymap is the keymap of macOS or derives from it, and Ctrl+Alt+Shift with the key otherwise. The
keystrokes the plugin gives each action, and the rule that the keymap of macOS has Cmd for Ctrl, are written in the
tests once and used by these tests and by the test of the default keymaps, which already holds both.

## Requirements

1. On macOS on Apple Silicon, `./gradlew test` runs the 8 tests named under "Observed behavior", and they pass
2. On Linux x64, the 8 tests pass as before
3. The 8 tests are the regression test: they fail on `master` at `8e716ff` on macOS on Apple Silicon with the
   messages above, and pass with the fix
4. The shortcut each of the 8 tests expects comes from the keystrokes written in the tests for each action, and not
   from the keymap or the action the test checks, so that a tooltip naming another shortcut than the plugin gives
   still fails the test on both platforms
5. The 8 tests assert what they asserted before, apart from the shortcut of the keymap in use. The shortcut of the
   user's own that some of them give is the same on both platforms
6. The plugin's own code does not change

## Decisions

- **The expected shortcut follows the keymap in use, from the keystrokes written in the tests.** The tests then check on
  each platform what its users see. Rejected: reading the expected shortcut from the active keymap at the time of the
  test, which takes it from the state the plugin set up and would let a wrong shortcut that the plugin declares pass;
  skipping the tests on macOS, which leaves the tooltips of macOS untested; and making the keymap of Linux active in
  these tests on macOS, which tests a setup no user of macOS has
- **The keystrokes and the rule of Cmd for Ctrl are written once.** The test of the default keymaps already holds the
  keystroke of each action and the rule, so the tooltip tests take them from the same place rather than holding a
  second copy that could drift from the first
- **A bug of its own, not part of unit 0030.** The human chose it. The fault is in tests of units 0013, 0017 and 0024,
  and unit 0030 changes nothing in them. Those units are implemented and frozen, and the fix changes no requirement of
  them, so this unit corrects their tests without superseding them

## Non-goals

- Any change to the shortcuts, to the keymaps or to the tooltips of the plugin
- Keymaps other than the default keymaps of Linux and macOS, which the plugin gives no shortcut
- Windows, which the plugin does not support
- Any other test that fails, or any other check, on macOS

## Beyond the request

1. **Running the whole suite on macOS when a unit is checked there.** The 8 tests failed unseen since units 0013, 0017
   and 0024, because the checks on macOS of those units were made by hand in the sandbox IDE and the suite was not run
   there. A line in the project's instructions for agents asking that `./gradlew test` be run whenever a unit is checked
   on macOS would cost one line and a few minutes of each such check. Without it, a test that depends on the platform is
   found the next time a unit happens to run the suite on macOS, as here, and is fixed then
