---
kind: task
status: approved
timestamp: 2026-11-21T11:36:00Z
---

# 0033 Shorten the pause after a code block

## Description

A code block is read as "code example" followed by two seconds of silence at 1.0×, the length the developer asked
for in unit 0010 as long enough to pause the reading by hand. Heard in use, the developer finds it too long. Make it
one second at 1.0×, twice the pause between two blocks:

* `PAUSE_AFTER_A_CODE_BLOCK_MILLISECONDS` in `SpokenMarkdown.kt` becomes 1,000. It stays divided by the speed of the
  reading, as every pause the plugin adds is, so that it is half a second at 2.0× and a third longer at 0.75×
* Where two pauses meet, the longest is still heard once, so a code block followed by the next block gives one second
* The tests that assert the parts of a code block expect a silence of 1,000 ms after "code example". The tests of
  `SpeechServiceTest` and `ReadingSpeedTest` that use 2,000 as an arbitrary length of a silence, not as the pause after
  a code block, are left as they are
* `README.md` and the table of pauses in `docs/reading-markdown.md` say one second where they say two seconds for
  the pause after "code example". The table drops "long enough to pause the reading by hand", which no longer holds.
  The "Wanted" line of the code block in `docs/reading-markdown.md` records what was asked in unit 0010 and stays as
  it is, and its "Now" line says one second

Not part of this task: a setting for the length of the pause, and any change to the other pauses.

## Log

- 2026-11-21T11:36:00Z [human] asked for a shorter pause after a code block, and chose one second from the options
  given
- 2026-11-21T11:36:00Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `815ebe9`, read `process.md`, the standards and `roles/specifier.md` of the installed kit,
  8.4.0. Made the unit a task: one constant, its tests and the two documents that state its length, with the length
  decided by the human. Numbered it 0033, after 0032. The status is `draft`
- 2026-11-21T12:34:20Z [human] approved the record. The status is `approved`

## Amendments
