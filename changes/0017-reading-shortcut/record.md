---
kind: feature
status: approved
timestamp: 2026-10-22T18:27:48Z
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

## Amendments
