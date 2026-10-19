---
kind: feature
status: draft
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

## Amendments
