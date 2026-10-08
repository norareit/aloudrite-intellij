---
kind: feature
status: approved
timestamp: 2026-10-08T16:49:34Z
---

# Speech engine proof of concept

## Log

- 2026-10-08T16:49:34Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, wrote `spec.md`. The human's request, to get Kokoro working in the plugin, was split with the human into
  three units: this one, 0005 for the download of the model and 0006 for reading a file. At the human's
  instruction all three specs were written now, and this one requires that 0005 and 0006 are revised when it is
  done. Decided with the human: sherpa-onnx as the engine, GPL-3.0 as the license and part of this unit, the
  source on GitHub next to Bitbucket as the human's own step, and Linux x64 and macOS on Apple Silicon as the
  platforms. Looked up on the day of writing and not run: the sizes of the model, of the ONNX Runtime Java library
  and of sherpa-onnx 1.13.8 with its Kokoro packages, and that the build of sherpa-onnx compiles espeak-ng in as
  a static library. Not established: that sherpa-onnx loads inside an IDE plugin, how the 8-bit model sounds, how
  fast it is, and that its v1.0 package holds the voice `af_heart`. My own choices, which the human has not
  stated: "or later" on the license, the 8-bit multi-language v1.0 package, the voice, the test sentence, the
  30 MB limit on the archive, and the model folder being one per user. Status left at `draft` for the human to
  approve.
- 2026-10-08T19:14:26Z [human] approved `spec.md` as it stands. Status set to `approved`.

## Amendments
