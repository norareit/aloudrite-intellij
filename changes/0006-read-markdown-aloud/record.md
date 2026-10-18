---
kind: feature
status: planned
timestamp: 2026-10-18T21:20:13Z
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

## Amendments
