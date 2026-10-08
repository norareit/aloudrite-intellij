---
kind: feature
status: draft
timestamp: 2026-10-08T16:49:34Z
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

## Amendments
