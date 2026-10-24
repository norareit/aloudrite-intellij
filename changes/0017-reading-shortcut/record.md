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
- 2026-10-22T19:03:39Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, ran the checks of the plan's verification on `6b98e3e`: `./gradlew test` passed with 509 tests, of which the
  same 3 skipped, and `KokoroSpeechEngineTest` ran with the model in place; `./gradlew buildPlugin` built
  `aloudrite-intellij-0.0.1.zip` of 20563644 bytes and passed its size check; `npm run check-changes` passed. Open are
  the look at the tooltips in the sandbox IDE and the developer's checks of `developer-checks.md` on both machines, and
  with them the last item of the verification. The status stays `in-progress`
- 2026-10-22T19:12:41Z [human] asked, after seeing the actions in Settings › Keymap, that "Read Slower" and "Read
  Faster" get the minus and plus icons, as "Read Aloud, Pause or Resume" and "Stop Reading" have icons there. The entry
  was written by Claude Code, anthropic/claude-opus-5-5, on the human's word. The status stays `in-progress`
- 2026-10-22T19:12:41Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on `feature-0017-reading-shortcut` at `6de009a`, added to `spec.md` the human's decision that the two speed actions
  have the minus and plus icons of the buttons in the tool window. The status stays `in-progress`
- 2026-10-22T19:12:41Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux host,
  added to `plan.md` the section "The icons of the speed actions" with its commit point 8, made before commit point 7,
  which waits for the developer's checks. The setup of `developer-checks.md` now names the icons. The status stays
  `in-progress`
- 2026-10-22T19:12:48Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, did the section "The icons of the speed actions". The test of the icons failed before `plugin.xml` named them,
  with no icon on "Read Slower", and passed after. `./gradlew test` passed on the tree of this commit: 510 tests, of
  which the same 3 skipped
- 2026-10-22T19:25:12Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, found why the developer saw no icons on "Read Slower" and "Read Faster" in Settings › Keymap of the sandbox IDE
  started after `7ff47b0`: the page draws the template presentation of what `ActionManager.getActionOrStub` gives, and
  an `ActionStub` keeps only the path of its icon, which reaches the presentation when the action is created. The two
  tab actions are created by the toolbar of the tab, and the speed actions only by their first use. A test that was not
  kept showed all four stubs without an icon in the test environment. The developer pressed the two speed keys once, and
  the icons then showed on the page. The test of the icons reads the created action, which is why it passed. Whether to
  leave it so or to create the two actions at startup is asked of the human
- 2026-10-22T19:25:37Z [human] decided to leave the icons of "Read Slower" and "Read Faster" as the platform shows them:
  in Settings › Keymap once each action has been used in a session, and in Find Action when it lists them, with no
  startup activity that creates the actions. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the
  human's word. The status stays `in-progress`
- 2026-10-22T19:26:44Z [human] checked the unit on Linux x64 in the sandbox IDE, on the branch at `9150725` or later, in
  a way of their own rather than by the steps of `developer-checks.md`: they used all four shortcuts, looked at the
  actions in Settings › Keymap, and looked at the tooltips. All of it worked as it should, and the tooltips looked good
  to them. Not checked on Linux x64: giving an action a key or removing one in Settings › Keymap, which the developer
  chose not to do, the places of the focus and the speed keys with no file read one by one, the light and the dark theme
  each, and for each key whether the desktop took it first beyond that every key reached the IDE. Not checked at all:
  macOS on Apple Silicon, which the developer put off while debugging another issue on that machine. The entry was
  written by Claude Code, anthropic/claude-opus-5-5, on the human's word. The look at the tooltips in the sandbox IDE,
  the open task of the section "The tooltips", is ticked on the developer's look. The task of the developer's checks
  stays open for macOS, and the unit stays `in-progress` until requirement 14 holds or the human changes it
- 2026-10-22T19:35:51Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a full review of unit 0017 at
  `31fc9e198c5e9f97c83733c99f2291f7217c71ae`, against the merge base `d50b48ec3790768141baf70d737b8f4c514abb54` with
  `master`. Returned S1, S2, S3 and P1, saved in `.reviews/0017-reading-shortcut/2026-10-22-193423-opencode.md`.
  `./gradlew cleanTest test buildPlugin` passed: 510 tests, 3 opt-in tests skipped, no failures or errors; all 5
  `KokoroSpeechEngineTest` tests ran with the model in place. The archive was 20563667 bytes and passed its size check.
  `npm run check-changes` and `git diff --check master...HEAD` passed. Inspected all 1802 distribution jars for the
  shortcut conflicts and checked the compiled actions' indexing awareness against the platform gate. Did not
  independently verify the interactive tooltips or physical keys, including the incomplete Linux and macOS checks
  of R14, or key reassignment and removal through Settings. The status stays `in-progress`
- 2026-10-22T19:53:02Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, took the findings of `.reviews/0017-reading-shortcut/2026-10-22-193423-opencode.md`, the review of this branch,
  and added the section "The findings of the review" to `plan.md` with commit point 9
- 2026-10-22T19:53:02Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, S1, accepted and applied:
  `ReadAloudAction`, `StopReadingAction` and `ReadingSpeedAction` extend `DumbAwareAction`, so that the keymap and
  `ActionUtil` no longer refuse them while the project of the key indexes. The test asks each of the four registered
  actions whether it is dumb aware, which is what the platform's check reads, and failed before the change. The review
  asked for a test that goes through the platform's check with a project that indexes; that test was tried and is not
  possible in this test environment, which opens no project: `DumbModeTestUtils.runInDumbModeSynchronously` on the
  default project hung, and starting dumb mode on it failed with "dumb mode is going to be entered without exiting", and
  the `MockProject` has no `DumbService`. As a side effect the two tab buttons also work while their project indexes,
  where the IDE greyed them out before; `ReadingButtonsAction`, which holds them on the tab, was not made dumb aware, as
  no finding asked it, and whether the tab row shows it while indexing was not checked
- 2026-10-22T19:53:02Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, S2, open for the human: the
  developer's checks of requirement 14 are not complete, with macOS not checked and the places of the focus on Linux not
  checked one by one. Only the human can complete them or change the requirement
- 2026-10-22T19:53:02Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, S3, accepted and applied: the
  shortcuts are given in "Default for XWin" and "Mac OS X 10.5+" in place of `$default`, and removed in the four keymaps
  that derive from "Mac OS X 10.5+", "macOS System Shortcuts" among them. A new test checks every keymap the tests load
  but those of requirement 8, `$default`, Emacs, Sublime Text, Visual Studio, Eclipse and NetBeans 6.5 among them, for
  both platforms; it failed before the change, naming all of them, and passes after. The test of the four keymaps of
  requirement 8 passes unchanged, so R8 and R9 still hold; the macOS entries no longer need `replace-all`
- 2026-10-22T19:53:02Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, P1, accepted and applied: the files
  of `plan.md` name `ReadingTarget.kt`, and its paragraph on the shortcuts says how they are given now and how they were
  given first
- 2026-10-22T19:53:02Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, ran `./gradlew test` on the tree of
  this commit: 512 tests passed, of which the same 3 skipped. The status stays `in-progress`, with S2 open
- 2026-10-22T20:17:29Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of unit 0017 at
  `09ff2d90841fe630633278b7d13c9e3c4ae769d4`, compared against the previously reviewed commit
  `31fc9e198c5e9f97c83733c99f2291f7217c71ae`. Returned S2 and no new findings, saved in
  `.reviews/0017-reading-shortcut/2026-10-22-201643-opencode.md`. `./gradlew cleanTest test buildPlugin` passed:
  512 tests, 3 opt-in tests skipped, no failures or errors; all 5 `KokoroSpeechEngineTest` tests ran with the model in
  place. The archive was 20563933 bytes and passed its size check. `npm run check-changes`,
  `git diff --check 31fc9e1..HEAD` and `git diff --check master...HEAD` passed. Probed the compiled actions' indexing
  awareness and inspected the distribution's keymap parent declarations. Did not independently verify interactive
  tooltips, physical keys, a live indexing workflow, or key reassignment and removal through Settings; the R14 checks
  could not be verified. The status stays `in-progress`
- 2026-10-22T20:25:20Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, took the delta review `.reviews/0017-reading-shortcut/2026-10-22-201643-opencode.md` of `09ff2d9`: it found S1,
  S3 and P1 corrected and no new findings, and S2 still open. Nothing was changed. The status stays `in-progress`, with
  S2 open for the human
- 2026-10-22T22:15:49Z [human] checked the shortcuts again on Linux x64 in the sandbox IDE, on the branch at `ff64ac9`,
  with the fp32 model: a reading started in a file of project A, and all four shortcuts used from another file of
  project A and then from project B, the same way. All of them worked as they should, and no key was taken by the
  desktop or another application before the IDE. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the
  human's word. Not named in this check: the keys with the focus in a tool window, and the speed keys with no file read.
  macOS on Apple Silicon is still not checked. The status stays `in-progress`
- 2026-10-22T22:19:30Z [human] completed the check on Linux x64 in the sandbox IDE: with the focus in the Project tool
  window, all four shortcuts worked on a reading, and with no file read, "Read Slower" and "Read Faster" changed the
  speed with the focus in the Project tool window and in another tool window than AloudRite's. With the entry of
  `2e42cfd`, requirement 14 holds on Linux x64 for each shortcut, from the file of another project, another file and a
  tool window, and with no file read, with no key taken before the IDE. The entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word. macOS on Apple Silicon is still not checked, so S2 stays open for that
  part. The status stays `in-progress`
- 2026-10-22T22:48:43Z [human] checked the shortcuts on macOS on Apple Silicon in the sandbox IDE, on the branch at
  `d621a7d`: ⌃⌥⇧Space, the key of "Read Aloud, Pause or Resume", does not work there, and the other three keys do. The
  developer takes it that something else on the Mac takes the key before the IDE. Which one is not known yet. The entry
  was written by Claude Code, anthropic/claude-opus-5-5, on the human's word. As the plan says for a key that is taken,
  the work stops for the human, who chooses the keys. The status stays `in-progress`
- 2026-10-22T22:55:35Z [human] found on macOS on Apple Silicon that the IDE does not receive ⌃⌥⇧Space: Find Actions by
  Shortcut in Settings › Keymap showed nothing for it, so something outside the IDE takes the key first; what that is
  was not looked for. The developer gave "Read Aloud, Pause or Resume" ⌃⌥⇧⌘Space in Settings › Keymap, and that key
  worked, which checks on macOS that a key the user gives an action works. The entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word. The status stays `in-progress`
- 2026-10-22T23:13:16Z [human] chose for macOS ⌘⌥⇧ with Space, O, Comma and Period, in place of ⌃⌥⇧, and kept the keys
  of Linux, and asked that the spec and the plan be changed and the work done. The entry was written by Claude Code,
  anthropic/claude-opus-5-5, on the human's word. The status stays `in-progress`
- 2026-10-22T23:13:16Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on `feature-0017-reading-shortcut` at `68dab91`, at the human's request, changed `spec.md`: requirement 8 gives macOS
  ⌘⌥⇧ with the four keys, and the decision on macOS now says why, with ⌃⌥⇧ and another key on both machines rejected.
  ⌘⌥⇧ with the four keys was looked up beforehand in the 1802 jars of the distribution and found in no keymap and no
  plugin descriptor; Ctrl+Alt+Shift+O is only in "NetBeans 6.5", which the shortcuts no longer reach. The status stays
  `in-progress`
- 2026-10-22T23:13:16Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux host,
  changed `plan.md`: the approach gives `Mac OS X 10.5+` `meta alt shift` with the four keys, the done task of the test
  of the keymaps no longer says that macOS has no ⌘ shortcut, and the new section "The keys of macOS" with commit point
  10 holds the work. The status stays `in-progress`
- 2026-10-22T23:14:39Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, did the section "The keys of macOS". The test of the default keymaps failed for the four actions in "Mac OS X
  10.5+" before `plugin.xml` gave them `meta alt shift`, and passes after; the test that no other keymap has the
  actions' keys passes as well, so the entries that remove them in the keymaps derived from "Mac OS X 10.5+" match the
  new keys. The look-up of the distribution was the one made as the specifier just before, with the same script, for
  `meta alt shift` and for `ctrl alt shift` with the four keys: nothing in a keymap of requirement 8, in `$default` or
  in a plugin descriptor. `README.md` and `developer-checks.md` name the new keys of macOS. `./gradlew test` passed on
  the tree of this commit: 512 tests, of which the same 3 skipped. On macOS, the four keys are still to be tried by the
  developer. The status stays `in-progress`
- 2026-10-24T12:20:11Z [human] checked the shortcuts on macOS on Apple Silicon in the sandbox IDE, on the branch at
  `05e27d0`, with the keymap "macOS": Settings › Keymap showed ⌘⌥⇧Space, ⌘⌥⇧O, ⌘⌥⇧, and ⌘⌥⇧. on the four actions; with
  the focus in the file that is read, ⌘⌥⇧Space read the file, paused and resumed it, ⌘⌥⇧. and ⌘⌥⇧, made it faster and
  slower, and ⌘⌥⇧O stopped it; all four did the same with the focus in another file and in the Project tool window; with
  no file read, the speed keys changed the speed with the focus in the Project tool window; and nothing outside the IDE
  reacted to any of the keys. The entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word. With
  the entries on Linux x64 of `7008a66` and `b911117`, requirement 14 holds on both machines, and with
  it S2 of the review is done. The task of the developer's checks and the last item of the verification are ticked, and
  commit point 7 is this commit
- 2026-10-24T12:21:15Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, ran the suites on the tree of `a6a7ddc` with the correction of the hashes in the entry before it, after the
  human's rebase of the branch onto `master` at `a8653fe`, which brought AgentRite 8.1.0: `./gradlew test` passed with
  512 tests, of which the same 3 skipped, and the 5 tests of `KokoroSpeechEngineTest` ran with the model in place;
  `./gradlew buildPlugin` built the archive of 20563971 bytes and passed its size check. The commits named in the
  entries before the rebase have other hashes since it. Every task of `plan.md` is ticked, and no finding of the reviews
  is open. The changes for the keys of macOS, `d5df966` and `a2badaf`, came after the last review. The status stays
  `in-progress` until the human decides whether they are reviewed first
- 2026-10-24T12:33:34Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, made a delta review of unit 0017 at
  `7906de233932a794b8a047c6d7ec19294d69c33d`, compared against the previously reviewed commit
  `09ff2d90841fe630633278b7d13c9e3c4ae769d4`, which remains available after the rebase. Returned S4, saved in
  `.reviews/0017-reading-shortcut/2026-10-24-123202-opencode.md`. `./gradlew cleanTest test buildPlugin` passed:
  512 tests, 509 passed and 3 opt-in tests skipped, no failures or errors; all 5 `KokoroSpeechEngineTest` tests ran
  with the int8 model in place. `./gradlew buildPlugin --rerun` executed the archive task and its size check; the
  archive was 20563971 bytes. `npm run check-changes`, `git diff --check 09ff2d9 HEAD` and
  `git diff --check master...HEAD` passed. Inspected all 1802 distribution jars for shortcut conflicts and the 15
  keymap parent declarations. Checked the recorded developer results for R14; did not independently repeat physical
  keys, interactive tooltips, a live indexing workflow, or key reassignment and removal through Settings, or reproduce
  the README's model-performance measurements. The status stays `in-progress`
- 2026-10-24T12:37:53Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host,
  on `feature-0017-reading-shortcut` at `989abdf`, at the human's request, added to `spec.md` the decision that the fp32
  model is the default on Linux as well, outside the reading shortcuts: what the human asked, why, what it costs a
  listener who never chose, and that it is no requirement of this unit. Read `roles/specifier.md` of the installed kit
  again
- 2026-10-24T12:37:53Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the main session on the Linux host,
  added to `plan.md` the section "The default model on Linux", with its one task ticked, since the commit `9f8e096` did
  it before the plan named it
- 2026-10-24T12:37:53Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, in the main session on the Linux
  host, took the delta review `.reviews/0017-reading-shortcut/2026-10-24-123202-opencode.md` of `7906de2`: S2 corrected,
  with the developer's checks on both machines recorded; S4, which did not block, accepted and done by the decision in
  `spec.md` and the section in `plan.md` above. No finding of the reviews is open

## Amendments
