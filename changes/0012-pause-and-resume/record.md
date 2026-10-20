---
kind: feature
status: approved
timestamp: 2026-10-20T20:14:29Z
---

# Pause and resume

## Log

- 2026-10-19T14:09:41Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `37e4b25`, at the human's request, wrote `spec.md` of a new unit: a reading can be paused
  and resumed. It is the first of the two features the human asked for when approving unit 0006, from its "Beyond the
  request", and was written once unit 0006 was implemented, as its record asked. Read `process.md` and
  `roles/specifier.md` of the installed kit. Made it a feature and numbered it 0012, after 0011. Asked before
  writing, the human chose: the button pauses and resumes, and a second button that shows only while there is a
  reading stops it; a resume starts at the beginning of the piece that was playing. Decided by me and open to the
  human: the first sound after a resume within one second, so that a resume does not wait for the engine; a pause
  without a time limit; a press on another file ends a paused reading. Left beyond the request: pausing on a keyboard
  shortcut, and resuming one piece earlier. The status is `draft`
- 2026-10-20T12:01:29Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, revised `spec.md` for the tool window
  of unit 0015, on the human's choice that the controls of a reading go there: requirement 4 no longer puts a second
  button on the tab, but a "Pause" or "Resume" button beside the "Stop" of the tool window, which also ends a paused
  reading; requirements 7, 8 and 9 speak of the tab button and the tool window in place of both buttons; the decision
  on the buttons says why and rejects the second tab button; the context says the unit is built after unit 0015; and
  a non-goal names the speed, unit 0016. The status stays `draft`
- 2026-10-20T12:03:05Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `478d82b`, at the human's request, revised `spec.md`: the tab of the file that is read shows
  a stop button beside the pause button from the moment the reading starts, so that stopping is one click on the tab
  as pausing is, and the tool window of unit 0015 keeps its own pause, resume and stop. Asked, the human chose the
  stop on the tab and in the tool window over the stop in the tool window only and over a tab button that stops with
  pausing only in the tool window. Requirement 4 is the stop button on the tab, the tool window moved to
  requirement 5, and the requirements after it are renumbered. The status is `draft`
- 2026-10-20T12:51:28Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `c3d2b82`, at the human's request, added the look of the buttons to `spec.md`. The human chose
  from drawings at their real size beside the IDE's own Run and Stop, in a light and a dark theme: "Read Aloud" is a
  person speaking, in the green of the IDE's Run icon, in place of the loudspeaker, which a first user found
  confusing; "Resume Reading" is that person with a thin line in front; the pause and the stop are the IDE's own; no
  icon is a play triangle; and the plugin's buttons stand on a background of their own in every state, as a group,
  not as a sign that a reading is going on. Rejected by the human: a head in profile, text or a page with sound waves,
  a speech bubble, a grey person while nothing is read, a wide bar in front of the person, and the IDE's look of a
  toggle that is on. Decided by me and open to the human: the tool window's icon is the person in grey. Added
  requirements 1, 6, 7 and 15, put the icons in requirements 2, 3, 5 and 8, renumbered the rest, and named as a risk
  for the plan that the tab row may not allow a background, which goes back to the human if so. The status is `draft`
- 2026-10-20T13:35:37Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `c4f7f9a`, at the human's request, added `icons.html` to the unit: the page of drawings from
  which the human chose the icons, opening on those choices, so that the planner has the drawings to start from. The
  context of `spec.md` says that it is there. The status is `draft`
- 2026-10-20T20:14:29Z [human] approved the spec as it stands, in the main session on the Linux host, and with it the
  choices the specifier left open: the tool window's icon is the person in grey, and a pause has no time limit. Nothing
  of "Beyond the request" is taken here: pausing on a keyboard shortcut is unit 0017. The status is `approved`. The
  entry was written by Claude Code, anthropic/claude-opus-5-5, on the human's word
- 2026-10-20T20:24:04Z [Claude Code, anthropic/claude-opus-5-5] as the planner, on `feature-0012-pause-and-resume` at
  `4d1ba00`, read the spec, the record and the tree, and stopped before writing `plan.md`: the spec leaves open where
  a resume begins when the reading was paused during a silence. Requirement 3 resumes at the beginning of the piece
  that was being played, as the plugin cut the text, but the silences of unit 0010 are no piece of the text: two
  seconds after "code example", half a second between two blocks and in place of a horizontal rule, and a quarter
  of a second around inline code. The context names the two seconds after "code example" as the moment a listener is
  meant to pause, so it is the main case and not an edge. Two answers fit requirements 3 and 4 alike: the resume
  begins at the piece after the silence, and the rest of the silence is not played; or it begins again at the piece
  before the silence, so that "code example" is heard again. A third, playing the rest of the silence and then the
  next piece, puts up to two seconds before the first sound and breaks requirement 4. The unit goes back to the
  specifier and the human. Found meanwhile, and not a question for the spec: the tab row allows the background of
  requirement 6. In IntelliJ IDEA 2026.2.3 it draws `EditorTabsToolbarActions` in an `ActionToolbar` made by
  `ActionManager.createActionToolbar`, which shows the component of an action that implements
  `CustomComponentAction`, so the plugin's buttons can stand in a panel the plugin paints. This was read from the
  classes and not seen running. The status stays `approved`

## Amendments
