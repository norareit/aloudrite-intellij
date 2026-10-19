---
kind: feature
status: draft
timestamp: 2026-10-19T11:50:23Z
---

# Read Markdown without its markers

## Log

- 2026-10-19T11:50:23Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `67e2777`, at the human's request, wrote `spec.md` of a new unit: the plugin reads Markdown
  without its markers, with words in place of code blocks and tables and with pauses where the structure asks for
  them. It starts from what the human heard on Linux during the checks of unit 0006, gathered in `record.md` of unit
  0006 at 2026-10-19T11:41:58Z and in `docs/reading-markdown.md` on the branch `feature-0006-read-markdown-aloud`,
  which is not on `master` yet. Read `process.md` and `roles/specifier.md` of the installed kit. Made it a feature
  and numbered it 0010, after the 0009 of the human. It needs unit 0006 implemented. Decided by me and open to the
  human: that the plugin reads the structure of the Markdown and does not only take out characters; that a pause is
  silence the plugin adds and not punctuation; the lengths of the pauses, half a second between paragraphs and for a
  horizontal rule, a quarter of a second around inline code, two seconds after "code example"; a bare address read as
  "link" and not as its domain; the struck out words read; the state of a task not said; an escaped character handed
  to the engine; a file of nothing but a code block not counted as empty. Left beyond the request: naming the
  symbols of code, front matter and HTML comments. Nothing of it was heard yet. The status is `draft`
