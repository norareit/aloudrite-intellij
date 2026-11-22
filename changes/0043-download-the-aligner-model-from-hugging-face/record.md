---
kind: feature
status: approved
timestamp: 2026-11-22T14:15:06Z
---

# 0043 Download the aligner model from Hugging Face

## Log

- 2026-11-22T14:15:06Z [human] asked for a unit that downloads the aligner model from Hugging Face, written before
  unit 0041 is finished
- 2026-11-22T14:15:06Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, in a worktree of `master` at `ae26c89`, read `process.md`, the standards and `roles/specifier.md` of the
  installed kit. Made the unit a feature, as moving the weekly check over to the aligner model is a decision, and
  numbered it 0043 after 0042. Found the archive in `csukuangfj/fa-models` through the workflow `upload-fa-models.yaml`
  of sherpa-onnx, and its size and SHA-256 at commit `39914ed` through the API of Hugging Face, earlier the same day.
  The status is `draft`
- 2026-11-22T22:47:35Z [human] approved the spec. The status is `approved`
- 2026-11-22T22:50:20Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in the sandbox on the unit's branch at `1b2ad31`,
  which holds unit 0041 as merged, read `process.md`, the standards and `roles/planner.md` of the installed kit, and wrote
  `plan.md`. Saw that Hugging Face answers the revision of `39914ed` of `csukuangfj/fa-models` with that commit, and the
  HEAD of the address of requirement 1 with a 302 that carries the commit, the size and the SHA-256 the plugin pins. The
  status stays `approved`

## Amendments
