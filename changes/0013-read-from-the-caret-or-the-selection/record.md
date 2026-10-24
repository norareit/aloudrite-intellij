---
kind: feature
status: approved
timestamp: 2026-10-19T14:09:41Z
---

# Read from the caret or the selection

## Log

- 2026-10-19T14:09:41Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `37e4b25`, at the human's request, wrote `spec.md` of a new unit: a press reads the
  selection, or else from the sentence the caret is in to the end of the file. It is the second of the two features
  the human asked for when approving unit 0006, from its "Beyond the request", and was written once unit 0006 was
  implemented, as its record asked. Read `process.md` and `roles/specifier.md` of the installed kit. Made it a
  feature and numbered it 0013, after 0012. Asked before writing, the human chose: the selection wins, and without
  one the reading starts at the beginning of the sentence the caret is in. Decided by me and open to the human: a
  sentence is the one of unit 0006, without the cut at a comma, colon or semicolon; a caret with nothing but white
  space after it reads the whole file; a preview without the text reads the whole file; only the primary caret
  counts; the tooltip says which of the three a press will read, with the same icon. Left beyond the request: reading
  from the paragraph, and reading from a click in a preview. The status is `draft`
- 2026-10-24T13:11:55Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `7b3e128`, at the human's request, checked the spec against the units implemented since it was
  written and rewrote it. It chose the part in the text of the file and read it as a file of its own, which unit 0010
  breaks: a piece of Markdown read alone loses what it means in the whole file, as a caret in a fenced code block or a
  selection of rows of a table shows. Its tooltips predated the icon of unit 0012 and the shortcut in the tooltips of
  unit 0017, its notification of nothing to read predated unit 0010, its non-goal of a shortcut was overtaken by
  unit 0017, and its check on both machines predated the rule in `AGENTS.md`. The human chose: the part is chosen in
  the Markdown of the whole file; a code block or a table that the selection touches is read whole; the number of an
  item is read when the part begins after it; the shortcut reads what the button would read; the tooltips name the
  shortcut as "Read Aloud" does; the check is on Linux x64, with one selection and one caret read on macOS on Apple
  Silicon. Decided by me and open to the human: a caret in text that is not read starts at the sentence of the next
  word read; a word that the selection cuts is read whole. Added beyond the request: the tool window saying what is
  read. The status is `draft`
- 2026-10-24T13:24:10Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `00a2a84`, at the human's request, added to the spec that a press and the shortcut with several
  carets or a block selection read by the primary caret and leave the plugin working, and that the tests and the
  developer's check on Linux x64 try both. Reading them as they mean stays a non-goal. The status is `draft`
- 2026-10-24T13:31:40Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `00a2a84`, at the human's request, took item 3 of "Beyond the request" into the spec as
  requirement 10: the tool window shows "Selection" or "From the caret" under the name of the file for the whole of
  such a reading, and nothing new for a whole file. Renumbered the requirements after it. Decided by me and open to
  the human: the two words. The status is `draft`
- 2026-10-24T13:22:35Z [human] approved the spec. The status is `approved`

## Amendments
