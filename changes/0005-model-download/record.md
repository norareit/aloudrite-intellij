---
kind: feature
status: draft
timestamp: 2026-10-08T16:49:34Z
---

# Model download on first use

## Log

- 2026-10-08T16:49:34Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, wrote `spec.md`, as the second of the three units the Kokoro work was split into with the human. It was
  written before unit 0004 was built, at the human's instruction, and unit 0004 requires that it is revised when
  that unit is done. At the human's instruction it requires in turn that unit 0006 is revised when this one is
  done. Decided with the human: the model is downloaded on first use and not shipped in the plugin. My own
  choices, which the human has not stated: asking the user before the download, fetching from the place
  sherpa-onnx publishes the package, the fixed checksum, and speech starting by itself after the download. Status
  left at `draft` for the human to approve.
- 2026-10-08T19:32:06Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, reworded one sentence of the context in `spec.md` after a change the human asked for in unit 0004:
  speech is asked for with the "Read Aloud" button and not through an action in the Tools menu. No requirement
  changed. Status still `draft`.
- 2026-10-08T22:51:21Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, revised `spec.md` against what unit 0004 established, as requirement 19 of that unit asks, reading its
  record, plan and README on its branch at `f21f947`. Changed because of what was found: the package is a packed
  archive, so the plugin unpacks it (requirement 8); a model folder the engine does not expect closed the whole
  process in one case, so an incomplete model must never count as in place, also after a kill (requirement 9);
  the checksum is checked before unpacking (7); the question replaces the missing-model message of unit 0004 and
  is not asked on a platform without speech (1, 4); the question also states the 190 MB on disk and the folder
  (2); the failure cases gain a package that cannot be unpacked, and the checksum case says what it means, since
  the publisher has replaced the package before (11); redirects are allowed over HTTPS, as the address is a
  GitHub release asset (15); and requirement 10 of unit 0004, no network request, is named as replaced (15).
  Added as my own choice: the human's checks on both platforms (17). The requirements are renumbered, from 14 to
  19. New under "Beyond the request": a copy of the package hosted by the project, a stricter check of a
  hand-placed model, and a check for free disk space. The human has not read the revision yet. Status still
  `draft`.

## Amendments
