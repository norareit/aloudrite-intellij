---
kind: feature
status: approved
timestamp: 2026-11-22T12:07:54Z
---

# 0041 Download the speech models from Hugging Face

## Log

- 2026-11-22T12:07:54Z [human] asked for the speech models to be downloaded from Hugging Face at fixed commits, as the
  archives on the GitHub release of sherpa-onnx were replaced, with the fixed int8 model, and for the aligner model to
  stay on GitHub until its maintainer publishes it on Hugging Face. Asked as well that a download limited by Hugging
  Face wait until the limit ends, rather than fail after a few short retries
- 2026-11-22T12:07:54Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `38da9bb`, read `process.md`, the standards and `roles/specifier.md` of the installed kit. Made
  the unit a feature, as the human asked, numbered 0041 after 0040. Downloaded both archives of the release
  `tts-models` and compared their files with the models in `~/.local/share/aloudrite/`, found the commits on Hugging
  Face that serve the old and the fixed int8 model and the installed fp32 model, and downloaded the 377 files of the
  int8 model at commit `57adb37` eight at a time, in 20 seconds, with no answer of 429, every file equal to the
  archive's. Found no aligner model on Hugging Face. The figures are in `spec.md`. The status is `draft`
- 2026-11-22T12:19:33Z [human] took item 2 of "Beyond the request", a check that the commits on Hugging Face still
  serve the files, to be run every week by a workflow of GitHub Actions and by hand
- 2026-11-22T12:19:33Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, wrote it into `spec.md` as
  requirements 15 to 17, with the list of files moved into a resource file that the download and the check share,
  in requirement 2. The status stays `draft`
- 2026-11-22T12:22:55Z [human] approved the spec. The status is `approved`
- 2026-11-22T12:30:09Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a unit run on the branch
  `feature-0041-download-the-speech-models-from-hugging-face` at `edecf6d`, read `process.md`, the standards and
  `roles/planner.md` of the installed kit, and wrote `plan.md`. Checked against Hugging Face that both repositories at
  their commits list 377 files besides `.gitattributes`, with the sums of requirement 9, that the HEAD of the largest
  file carries `x-repo-commit`, `x-linked-size` and `x-linked-etag` and that the revision endpoint answers 404 for a
  commit that does not exist. Found that the address of a file answers with a redirect, to the CDN or to a relative
  address under `/api/resolve-cache/` that carries no limit of the bucket `api`; the plan takes following it as
  requesting the file, which requirement 5 allows. The status stays `approved`

## Amendments
