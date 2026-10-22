---
kind: feature
status: in-progress
timestamp: 2026-10-22T18:39:44Z
---

# Reading shortcuts

## Log

- 2026-10-20T13:33:16Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `4002e5d`, at the human's request, wrote `spec.md` of a new unit: a keyboard shortcut for
  reading, from the item "Pausing on a keyboard shortcut" under "Beyond the request" of unit 0012. Read `process.md`
  and `roles/specifier.md` of the installed kit. Made it a feature and numbered it 0017, after 0016, to be built after
  unit 0012. Asked before writing, the human chose: the shortcut does what the tab button does, starting, pausing and
  resuming; it has a default key; and it acts on the reading wherever the focus is. Checked candidate keys against the
  default keymaps and the plugin shortcuts of IntelliJ IDEA 2026.2.3, and found Ctrl+Alt+Shift+Space free on Linux and
  ⌘⌥⇧Space free on macOS, where shorter combinations with Space and Ctrl+Alt+Shift with P, R, S and A are taken.
  Decided by me and open to the human: that key; the shortcut pauses a reading also from another Markdown file; it
  does nothing outside a Markdown file; and the action's name "Read Aloud, Pause or Resume". Left beyond the request:
  the shortcut in the tooltips of the tab buttons, and a second key that starts the selected file during a reading.
  The status is `draft`
- 2026-10-22T18:22:03Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `e61647f`, at the human's request, revised `spec.md`. The human asked for shortcuts that stop
  and that make the reading slower and faster beside the one that reads, pauses and resumes, proposed Ctrl+Alt with A,
  S, Q and E for them, took in the shortcut in the tooltips from what the spec left beyond the request, and asked that
  the tooltips name the shortcuts. Read `roles/specifier.md` of the installed kit again. Checked the keys against the
  default keymaps and the bundled plugins of IntelliJ IDEA 2026.2.3, reading every keymap and plugin descriptor of the
  distribution: Ctrl+Alt with A, S, Q and E is taken on Linux, and so is Ctrl+Alt+Shift with A, S and E. The spec now
  has four actions, a stop and two for the speed that act wherever the focus is and do what the buttons do, and the
  tooltips of the tab and tool window buttons name the shortcut in use. Updated the context to units 0012, 0015 and
  0016 as they were built. Decided by me and open to the human: Ctrl+Alt+Shift with Space, Q, Comma and Period; ⌃⌥⇧
  with the same keys on macOS in place of ⌘⌥⇧, because ⌘⌥⇧Q logs out of macOS at once; the names "Stop Reading",
  "Read Slower" and "Read Faster"; the speed shortcuts with no file read; and tooltips for the text buttons of the tool
  window. Left beyond the request: the new speed shown after a speed shortcut, and the shortcut in the tool window's
  text when nothing is read. The status stays `draft`
- 2026-10-22T18:27:48Z [human] approved the spec, with the shortcuts of requirement 8 and without items 2, 3 and 4 under
  "Beyond the request". The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word. The
  status is `approved`
- 2026-10-22T18:36:59Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux host,
  on `feature-0017-reading-shortcut` at `d50b48e`, cut from `master`, wrote `plan.md`. Read `roles/planner.md` of the
  installed kit. The two tab actions become the keymap actions "Read Aloud, Pause or Resume" and "Stop Reading",
  keeping their ids, and act on the one reading when used by key or from Find Action, so that each name appears once
  in Settings › Keymap; two new actions change the speed through steps of the setting that the panel shares. Checked
  in the classes of IntelliJ IDEA 2026.2.3 that a toolbar button takes a tooltip handed to it in its presentation,
  which lets "Read Aloud" name no shortcut on the tab of another file, and that its default tooltip names the shortcut
  of the active keymap. The macOS shortcuts replace those the IDE would derive with ⌘ from the Linux ones. The status
  stays `approved`
- 2026-10-22T18:39:44Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, on `feature-0017-reading-shortcut` at `7e4f1c6`, began the work. Read `roles/implementer.md` of the installed
  kit, the spec, the plan and this record. Commit point 1 was made by the planner as `7e4f1c6`. The status is
  `in-progress`
- 2026-10-22T18:44:30Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, did the section "The actions by key" of the plan. The test of the names in `ReadingShortcutsTest` checks
  "Read Aloud, Pause or Resume" and "Stop Reading" and that the texts of the plugin's actions are all different, and
  the names of "Read Slower" and "Read Faster" are added to it in the next section, where those actions are
  registered, so that the suite passes at each commit point. A press of a key in the tests calls the `update` and
  `actionPerformed` of the action itself, rather than `ActionUtil`, which asks the project for the dumb mode service
  that the `MockProject` standing for another project has not. `isOnTheTab` and `targetOfTheTab`, shared by the two
  tab actions, are in `ReadingTarget.kt`, a file the plan does not list. `./gradlew test` passed on the tree of this
  commit: 494 tests, of which 3 skipped, the two engine measurements and `RealModelPackageTest`, which run only when
  their environment variable is set; `KokoroSpeechEngineTest` ran with the model in place
- 2026-10-22T18:47:46Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, did the section "The speed by key" of the plan. The names of "Read Slower" and "Read Faster" joined the test
  of the names, and the test in `ReadAloudActionTest` of the actions the plugin registers, which the plan does not
  name, now lists the two new ids. The tests of the speed by key read a file of one sentence, so that what the engine
  makes after the wait is that piece again. `./gradlew test` passed on the tree of this commit: 503 tests, of which
  the same 3 skipped
- 2026-10-22T18:50:10Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, found that Ctrl+Alt+Shift+Q, the key of "Stop Reading" in requirement 8, is taken, and stopped the work for the
  specifier and the human, as the plan says. The test of the keymaps fails in "Default for XWin", "Default for GNOME"
  and "Default for KDE": the shortcut belongs there both to `com.ritenoar.aloudrite.StopReading` and to
  `Jdbc.OpenConsole.New.InPath`, "Query File…" of the bundled plugin Database Tools, which gives it Ctrl+Alt+Shift+Q in
  `$default` in `plugins/DatabaseTools/lib/modules/intellij.database.impl.jar`, and ⇧⌘J in "Mac OS X" with
  `replace-all`. The test passes for the other three keys in all four keymaps, and for all four keys in "Mac OS X
  10.5+". The look-up of the plan, a script that read every `keymaps/*.xml`, `plugin.xml` and module descriptor in the
  1802 jars of the IntelliJ IDEA 2026.2.3 distribution the build uses, found the same and nothing else: Ctrl+Alt+Shift
  with Space, Comma and Period is in no keymap and no descriptor. It also listed the keys taken with Ctrl+Alt+Shift: in
  `$default` 1, every letter but O and Z, Q among them by Database Tools, Back Space, Down, Enter, F7, F8, Insert, Left,
  Right and Up; in "Default for XWin" 0, 2, 7, B, Equals and Minus; in "Default for GNOME" and "Default for KDE" 0,
  Equals and Minus; in "Mac OS X" Down, Up and Y; and in "Mac OS X 10.5+" B, F, Down, Up and Y. O and Z are free in all
  of them. The shortcuts are in `plugin.xml` and the test of the keymaps is written; neither is committed, and
  `./gradlew test` fails on that tree with this one test. The status stays `in-progress`
- 2026-10-22T18:55:21Z [human] chose Ctrl+Alt+Shift+O and ⌃⌥⇧O for "Stop Reading", because Ctrl+Alt+Shift+Z is too
  awkward to press with one hand, and asked that the spec and the plan be corrected and the work go on, which approves
  the corrected spec. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word. The status
  stays `in-progress`
- 2026-10-22T18:55:21Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on `feature-0017-reading-shortcut` at `ea4ca5c`, at the human's request, corrected `spec.md`. Read
  `roles/specifier.md` of the installed kit again. The stop is on O in place of Q in requirement 8; the context says
  that Database Tools takes Ctrl+Alt+Shift+Q on Linux and that every letter but O and Z is taken with Ctrl+Alt+Shift;
  the decision on the keys gives the reason for O and rejects Q and Z; and the decision on macOS keeps its reason for
  ⌃⌥⇧ without resting on the stop being on Q. The status stays `in-progress`
- 2026-10-22T18:55:21Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux host,
  corrected `plan.md` to the corrected spec. Read `roles/planner.md` of the installed kit again. The approach gives the
  stop `ctrl alt shift O`, and says why the entries for macOS keep `replace-all`. The three tasks of the section "The
  shortcuts in the keymaps" that were done with Q are open again, to be done with O. The status stays `in-progress`
- 2026-10-22T18:56:18Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, did the section "The shortcuts in the keymaps" with the stop on O. The test of the keymaps reports every keymap
  and shortcut where something else is found in one run, rather than the first. The look-up script, run again over the
  1802 jars of the distribution, finds none of the four keystrokes in a keymap of requirement 8 or in a plugin
  descriptor; Ctrl+Alt+Shift+O is only in the keymap "NetBeans 6.5", for Go to Symbol, which requirement 9 does not
  cover and the spec names among the non-goals. The comment in `plugin.xml` on `replace-all` no longer rests on Q. This
  commit also mends a comment line of `ReadingShortcutsTest` that the commit of the previous section had doubled.
  `./gradlew test` passed on the tree of this commit: 504 tests, of which the same 3 skipped
- 2026-10-22T19:02:06Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, did the section "The tooltips" of the plan but its last task, the look at the tooltips in the sandbox IDE. The
  four actions name their ids in a constant `ID` of their own, which the tooltips use, so that an action made in a test
  names the shortcut of the registered one. A tooltip handed to the tab button at every update does not install again
  while it is equal to the one installed, which `HelpTooltip.installOn` checks. `./gradlew test` passed on the tree of
  this commit: 509 tests, of which the same 3 skipped. The look was not done: the sandbox IDE started, and a capture of
  its frame alone showed the buttons on the tab, but the IDE paints its content at about three quarters of the size of
  its frame on the developer's screen, so that the pointer moved over "Read Aloud" did not reach it and no tooltip
  showed, and the attempt moved the developer's pointer and focus each time. One of the attempts captured the whole
  screen against `AGENTS.md`; that capture was deleted without being looked at. The sandbox IDE was stopped, and the
  task is open for the developer, who looks at the same tooltips on Linux x64 under requirement 14
- 2026-10-22T19:03:13Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, did the section "The documentation and the checks" of the plan. `README.md` has a paragraph of its own on the
  four keys, after the one on a change of the speed during a reading, and no other statement of it changed.
  `developer-checks.md` holds the look of the tooltips on Linux x64, which also stands for the open task of the look in
  the sandbox IDE, the four keys from the three places of the focus and the speed keys with no file read on both
  machines, and a key given and one removed in Settings › Keymap. Commit point 5 is ticked as made, with the task of the
  look still open

## Amendments
