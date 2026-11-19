---
kind: feature
status: in-progress
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
- 2026-11-19T23:00:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, run by AgentRite's orchestrator on the
  Linux host, on `feature-0027-choose-which-highlights-are-shown`, began the work. Read `process.md`, the standards and
  `roles/implementer.md`. No review findings exist yet. The status is `in-progress`
- 2026-11-19T23:58:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on `feature-0027-choose-which-highlights-are-shown`,
  built the unit in the commit `52e6484` and the README. Tasks 1 to 5 and 7 of the plan are ticked. Deviation: the
  intermediate step of task 2, with the two old tests marked `@Ignore`, was skipped, since the tests of the four boxes
  were written in the same sitting and no intermediate state was committed. The tests of the editor and the preview
  that change a box at slice 2 and back at slice 4 see the change at the refresh of the next slice, since the callback
  runs after the refresh of its own slice, as the tests of unit 0025 already did. `./gradlew test buildPlugin` passed
  on `52e6484` with the README as the only later change: `HighlightSettingTest` 5, `ReadAlongHighlighterTest` 27,
  `ReadAlongPreviewTest` 23, `ReadingSpeedConfigurableTest` 33 and `ReadAlongScriptTest` 1 tests, none skipped. The
  tests with the real engine were not run, since the unit does not touch the engine
- 2026-11-19T23:59:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, did not complete task 6, the look
  at the sandbox IDE (R9). Started `./gradlew runIde` and found its window by the process id of the sandbox IDE, apart
  from the developer's own IDE window with the same title. The capture of that window with `xwd` was entirely white at
  two tries, so nothing about the page, the greying out or the scrolling by the sentence was seen. Not checked: how the
  four boxes look in a light and a dark theme, the greyed out boxes, and a reading with only the sentence highlighted.
  The task stays unticked and the sandbox IDE was stopped. The status stays `in-progress`
- 2026-11-20T00:20:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, resumed on `feature-0027-choose-which-highlights-are-shown`
  at `beef70e`, and tried task 6 again. The earlier white captures were an artefact of the conversion: `xwd` writes an
  alpha channel that `ffmpeg` read as transparent, and `-vf format=rgb0` gives the real image. The capture of the
  window of the sandbox IDE, found by its process id, showed the Markdown file and its preview. The settings page was
  not opened, since the IDE ignores synthetic key events sent to its window, and real input with `xdotool` goes to the
  window that has the focus, which on this display may be the developer's own IDE. So the look of the page in a light
  and a dark theme, the greyed out boxes and the reading with only the sentence highlighted were not seen. Task 6 stays
  unticked, R9 is not met, and the sandbox IDE was stopped. `./gradlew test buildPlugin` and `npm run check-changes`
  passed on `beef70e`. The second and third commit points of the plan were not made as planned: the settings page
  went in with the first commit, and the README and record in one commit, `beef70e`, so those two boxes stay
  unticked. The status stays `in-progress`
- 2026-11-19T23:21:16Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0027 at
  `09e43bea5511ba5e24093695f218b25b1babc605` in a full review against the merge base `4b2bf38` with `master`.
  Returned S1, P1 and C1, saved in `.reviews/0027-choose-which-highlights-are-shown/review-20261119T232116Z.md`.
  Independently ran `./gradlew test buildPlugin` (successful, tasks up to date), `./gradlew cleanTest test`
  (successful, affected tests and real engine tests ran), `npm run check-changes` and `git diff --check`
  (successful). The opt-in measurement suites and real package test were skipped. Could not verify the sandbox
  observations required by R9. The status stays `in-progress`
- 2026-11-20T00:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, accepted by the definition of
  the orchestrator, not as a decision of the human, findings P1 and C1 of the review of `09e43be`. P1: replaced the
  commit points of `plan.md` with the two commits that were made, and removed the third planned one, since its work
  went into the second. C1: renamed the one-character lambda parameters of `ReadingSpeedConfigurableTest` to
  `configurable` and `choices`. S1 stays open: the look in the sandbox IDE (R9) cannot be made safely by an agent on
  this display, so it is left to the human, and task 6 stays unticked. The status stays `in-progress`
- 2026-11-19T23:25:34Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0027 at
  `f9173ddddf5d5af5c29dfe8a8dc03c643997f25a` in a delta review from `09e43bea5511ba5e24093695f218b25b1babc605`.
  Verified the corrections for P1 and C1; S1 remains open because R9's sandbox observations could not be verified.
  Returned no new findings. Saved the review in
  `.reviews/0027-choose-which-highlights-are-shown/review-20261119T232534Z.md`. Independently ran
  `./gradlew cleanTest test buildPlugin` (successful, fresh tests and up-to-date build), `npm run check-changes`
  and the working-tree and delta `git diff --check` checks (successful). The status stays `in-progress`

## Amendments
