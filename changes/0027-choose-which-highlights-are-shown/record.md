---
kind: feature
status: approved
timestamp: 2026-11-19T16:10:49Z
---

# Choose which highlights are shown

## Log

- 2026-11-19T16:10:49Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host, on
  `master` at `d1ceece`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0027, after 0026. Decided with
  the human: four checkboxes replacing the one of unit 0025, the two of the views first, the two of the highlights
  greyed out while neither view is checked, and the unit built after unit 0026. Decided by me and open to the human:
  the wording of the boxes, all four checked until cleared, the check marks kept while greyed out, and the sentence
  followed by the scrolling when the word is not shown. Left beyond the request: carrying over a cleared checkbox of
  unit 0025. The status is `draft`
- 2026-11-19T21:10:30Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host, on
  `master`, changed the defaults of requirement 2 as the human decided: the two views and the sentence start checked
  and the word starts cleared, since the estimate of the word skipped short words in the human's check of unit 0025
  at 1.0×. Replaced the decision that all four start checked, and reworded item 1 of "Beyond the request" to match.
  The status stays `draft`
- 2026-11-19T22:36:36Z [human] approved the spec. The status is `approved`
- 2026-11-19T22:44:05Z [Claude Code, anthropic/claude-opus-5-5] as the planner, run by AgentRite's orchestrator on the
  Linux host, cut `feature-0027-choose-which-highlights-are-shown` from `master` at `4b2bf38` and wrote `plan.md`.
  Read `process.md`, the standards and `roles/planner.md` of the installed kit, 8.4.0. Unit 0026 is implemented, so
  the preview has the choice of requirement 3 to act on. Decided in the plan: the four choices held as one immutable
  value read once per refresh; the state of unit 0025 left to load as the defaults, since carrying over its box is
  beyond the request; the word starting cleared met in the existing tests by choosing it in their setup; a view that
  keeps a range in view while its start and end are both visible, the sentence followed in the page when the message
  holds no word, and a sentence taller than the preview put at its top; and the boxes of the highlights not
  indented. The status is `approved`

## Amendments
