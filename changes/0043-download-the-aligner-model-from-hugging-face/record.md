---
kind: feature
status: implemented
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
- 2026-11-22T23:05:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in the sandbox on the unit's branch, read `process.md`, the standards, `roles/implementer.md`, the spec, the plan and this record, and set the status to `in-progress`. Built the plan in the three commit points `21aece0`, `e929a95` and the one that holds the documents: `ALIGNER_MODEL_SOURCE` and the package of `ALIGNER_MODEL` built from it, `modelSources()` in the check of the sources, and the texts of `README.md` and `THIRD-PARTY.md`. The tests were written before the code and `./gradlew test` passed after it
- 2026-11-22T23:06:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran `./gradlew checkModelSources` in the sandbox against Hugging Face: it printed "Hugging Face still serves the commits of 3 models." With the commit of `ALIGNER_MODEL_SOURCE` changed in a temporary edit that was not committed, it failed with two lines that name `english-mfa-v3.1.0`: the revision address with "the commit ... is missing from csukuangfj/fa-models", and the address of the archive with "the answer was HTTP 404". The edit was undone
- 2026-11-22T23:07:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, ran the instructions of `README.md` for Linux with a new temporary folder in place of `~/.local/share/aloudrite`: `sha256sum -c` printed `OK` and `english-mfa-v3.1.0` held the six files `final.alimdl`, `lda.mat`, `tree`, `phones.txt`, `lexicon.txt` and `meta.json`. `grep -rn "releases/download/fa-models" README.md THIRD-PARTY.md core src` finds nothing. `./gradlew buildPlugin` passed and no jar of the plugin archive holds a class of `com.ritenoar.aloudrite.sources`
- 2026-11-22T23:08:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, left the developer's check of the plan open: the download of the aligner model through the plugin on a machine where it is not in place, and the word highlighted in a reading (R5), which only the developer can make. Not checked: the macOS instructions, which differ from the Linux ones only in the folder and in `shasum`. The tests with the real engine were not run, as the archive is the same byte for byte and the unit changes no engine code. The unit stays `in-progress` until that check and the review
- 2026-11-22T22:56:13Z [OpenCode, openai/gpt-6.1-sol] as the reviewer, reviewed unit 0043 at `54f1705` in a full review against merge base `1b2ad31` with `origin/master`. Returned no findings on spec conformance, plan fidelity or code quality; saved the review in `.reviews/0043-download-the-aligner-model-from-hugging-face/review-round-1.md`. Independently ran `./gradlew test --rerun-tasks`, `./gradlew buildPlugin`, `./gradlew checkModelSources` and `npm run check-changes`, all successfully; the real aligner tests ran without skips. Could not verify the developer's download through the running plugin and visible word highlight on Linux x64 and macOS on Apple Silicon, which remain an open plan task, or the macOS manual instructions. The status stays `in-progress`
- 2026-11-22T23:36:42Z [human] reported the check of requirement 5 on Linux x64, on the unit's branch: the question
  before the download named the address on Hugging Face, the download of the aligner model through the plugin succeeded,
  and the word was highlighted as it was heard in a reading
- 2026-11-22T23:36:42Z [human] decided that the check on macOS on Apple Silicon is not made for this unit, as the
  developer has checked the download there already under unit 0041 and this unit changes only the address of the
  archive. This is an exception to what `AGENTS.md` asks for a unit that touches the model download
- 2026-11-22T23:36:42Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, on the human's decision reworded the
  task of the developer's check in `plan.md` to Linux x64 and ticked it on the human's result. Every task of `plan.md`
  is ticked. The manual instructions for macOS in `README.md` remain unrun. The status stays `in-progress`
- 2026-11-22T23:36:42Z [Claude Code, anthropic/claude-opus-5-5] as the implementer, at the human's decision, on
  `ea82a48`, with every task of `plan.md` ticked and no finding returned by the review of `54f1705`, which the commits
  after it, of this record and of the plan, do not change in code. The status is `implemented`

## Amendments
