---
kind: feature
status: in-progress
timestamp: 2026-11-21T23:40:00Z
supersedes: 0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes
---

# piper-phonemize 1.6 and emoji read by their names

## Log

- 2026-11-21T11:48:00Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `13b0427`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0035, after 0034, superseding
  unit 0030, whose requirements on the Linux library and on characters above U+FFFF it makes untrue. Read the changes
  of `piper-phonemize` from `v1.5.9` to `v1.6.1` on GitHub and tried the Java archive and the native library for Linux
  x64 of 1.6.1 outside the project, on the host and in `eclipse-temurin:21-jdk-jammy`, as the context of the spec
  says. Decided with the human: the move to the fixed release, characters above U+FFFF read by their names, and a unit
  of its own. Decided by me and open to the human: the release's own Linux library in place of the project's build,
  and characters without a name read as eSpeak NG reads them. Left beyond the request: leaving out the characters
  without a name, and a note on what this unit makes untrue in the specs of units 0028 and 0032. The status is
  `draft`
- 2026-11-21T12:52:05Z [human] approved the spec. The status is `approved`
- 2026-11-21T23:21:02Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a unit run of the orchestrator in
  the sandbox, on the branch `feature-0035-piper-phonemize-1-6-and-emoji-read-by-their-names` at `e725b7b`, wrote
  `plan.md`. Read `process.md`, the standards and `roles/planner.md` of the installed kit. Fetched the three jars of
  `piper-phonemize` 1.6.1 outside the project: their names and layout are those of 1.5.9, and the Linux library needs
  no glibc symbol version newer than 2.14. Tried the forced aligner of unit 0028 on the audio of a sentence with the
  name of an emoji spoken: it gives no interval for an emoji, so a piece that holds one would lose its alignment, and
  a word its lexicon lacks, given in its place, is placed where the name is heard. The plan therefore makes each
  character above U+FFFF a word of its own and gives it to the aligner as the word `emoji`, so that the highlights of
  the words around it stay where they are heard, as requirement 8 asks. It found that a piece holding ❤ already has no
  alignment before this unit, which stays as it is, since how ❤ is read is a non-goal. The status stays `approved`
- 2026-11-21T23:40:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in a unit run of the orchestrator
  in the sandbox, on the branch `feature-0035-piper-phonemize-1-6-and-emoji-read-by-their-names`, began the work. Read
  `process.md`, the standards, `roles/implementer.md` and the three documents of the unit. The status is `in-progress`
- 2026-11-21T23:41:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, added
  `thePhonemesHandedToTheEngineAreThoseOf1_5_9` to `EspeakPhonemesTest` and made the companion of `EspeakDataTest`
  internal so that it shares `SENTENCES`. It failed with no constants and printed what was found, which were written
  into the test, and it then passed. Found under `piper-phonemize` 1.5.9 at `a80af4a` (the tree before any change of
  this unit), with the int8 model, with the voice `DEFAULT_VOICE` (`en-us`), on Linux x64 in the sandbox; the rows of
  a sentence are joined by ` | `, and each of these sentences is one row. Each ends with a space after the full stop:
    * `I have 3 cats in 2026.`: `aɪ hæv θɹˈiː kˈæts ɪn tˈuː θˈaʊzənd twˈɛnti sˈɪks. `
    * `Version 4.2 is 5% faster & smaller.`: `vˈɜːʒən fˈoːɹ pɔɪnt tˈuː ɪz fˈaɪv pɚsˈɛnt fˈæstɚ ænd smˈɔːlɚ. `
    * `Read the README of the API with npm.`: `ɹˈiːd ðə ɹˈiːdmiː ʌvðɪ ˌeɪpˌiːˈaɪ wɪð ˌɛnpˌiːˈɛm. `
    * `It builds for x86_64 and ARM64 with v1.13.8.`: `ɪt bˈɪldz fɔːɹ ˈɛks ˈeɪɾi sˈɪks sˈɪksti fˈoːɹ ænd ˌeɪˌɑːɹɹˈɛm sˈɪksti fˈoːɹ wɪð vˈiː wˈʌn pɔɪnt wˈʌn θɹˈiːpɔɪnt ˈeɪt. `
    * `AloudRite reads Markdown aloud.`: `ɐlˈaʊd ɹˈaɪt ɹˈiːdz mˈɑːɹkdaʊn ɐlˈaʊd. `
    * `Before the sign, and after the sign.`: `bᵻfˌoːɹ ðə sˈaɪn, ænd ˈæftɚ ðə sˈaɪn. `
    * `Before the sign, ❤ and after the sign.`: `bᵻfˌoːɹ ðə sˈaɪn, ɹˈɛd hˈɑːɹt ænd ˈæftɚ ðə sˈaɪn. `

## Amendments
