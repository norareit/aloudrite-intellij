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
- 2026-10-19T14:16:12Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `28bbe26`, corrected `spec.md` at the human's request. The human listened to a spec of their own
  with AloudRite and found its numbered items, which had no full stops, running together, "very awkward"; the items
  of the sample's numbered list all end with a full stop, which gave the pause that sounded good there, so "numbered
  lists sound fine" holds only for items with their own punctuation. The human asked for a pause before the next item
  in numbered and bulleted lists alike, without a double pause for items that do end with punctuation. Changed: the
  context says so; requirement 4 gives every item of a list, numbered ones too, the pause and fall of the end of a
  sentence, once, and keeps the numbers read; requirement 16 keeps only quotes as in unit 0006; the decision on the
  lengths of the pauses no longer says that numbered lists have the pause already; a new decision: an item without
  its own end is handed to the engine as if it ended with a full stop, so that all items end alike and none gets a
  second pause, the one place where a pause is made with punctuation, with the reasons and the alternatives
  rejected. Decided by me and open to the human: that way of making the pause. No requirement is renumbered. Not
  corrected here, since this commit holds only this unit's documents: the sentence on numbered lists in
  `docs/reading-markdown.md` and the record of unit 0006, which say the voice pauses at each new number; the
  first is for requirement 21 of this unit, the second for an amendment of unit 0006 if the human wants one. The
  status stays `draft`
- 2026-10-19T19:30:37Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, reworded the non-goal on emoji in
  `spec.md`, which said that characters above U+FFFF stay left out and so read as for good: this unit leaves them out
  as unit 0006 does, and unit 0014 has them read once a release of sherpa-onnx can hand them over intact. On the
  human's word that they are left out until unit 0014 is done, and read after it. The status stays `draft`
- 2026-10-19T19:41:17Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, changed requirement 3 of `spec.md` and
  its decision so that struck out text is not read, neither its tildes nor its words, on the human's word that
  reading struck out words next to the words that replace them makes a sentence the author never meant. A file whose
  only words are struck out reads nothing, under requirement 17. The status stays `draft`

## Amendments
