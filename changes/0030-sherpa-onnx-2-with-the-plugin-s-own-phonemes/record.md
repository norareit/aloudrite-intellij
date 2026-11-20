---
kind: feature
status: in-progress
timestamp: 2026-11-20T00:33:58Z
supersedes: 0014-sherpa-onnx-with-the-fixed-binding
---

# sherpa-onnx 2 with the plugin's own phonemes

## Log

- 2026-11-20T00:33:58Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `537afd2`, at the human's request, wrote `spec.md`. Read `process.md`, the standards and
  `roles/specifier.md` of the installed kit, 8.4.0. Made it a feature and numbered it 0030, after 0029. Read on GitHub
  why sherpa-onnx 2.0 dropped eSpeak NG (issue 3731 and pull request 4055: its GPL license), the maintainer's answer in
  issue 4139, the phonemes that `GenerationConfig` of 2.0 takes, and the Java binding and releases of
  `piper-phonemize`. Found that pull request 4062, the fix of unit 0014, was merged after 4055 and is in 2.0. Decided
  with the human: the plugin's own fallback to eSpeak NG, this unit before units 0028 and 0029, and the work of unit
  0014 taken in, which the human cancelled for it. Decided by me and open to the human: the data of eSpeak NG from the
  model folder, and 10% of the length under 1.13.8 as the test that every word is spoken. Left beyond the request: a
  note for other users of sherpa-onnx. The status is `draft`
- 2026-11-20T00:43:23Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `13d6a1f`, at the human's request, added requirement 15: before the unit is implemented, its
  record names what of the specs of units 0028 and 0029 it has made untrue, unneeded or open to a simpler way, so that
  the human is reminded to have them corrected. Also wrote in the context that the human set unit 0029 back to
  `draft`, and that correcting the two specs is no part of this unit. The status is `draft`
- 2026-11-20T00:54:07Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `master` at `4ccf5c1`, at the human's request, tested the Java binding of `piper-phonemize` 1.5.9 for
  characters above U+FFFF and found the fault of pull request 4062 of sherpa-onnx in it: its `nativeText` reads the
  text with `GetStringUTFChars`, and eSpeak NG leaves such a character out, where the C library of the same release,
  given standard UTF-8, reads 😀 as "grinning face". Wrote the finding into the context. Decided with the human: the
  unit does not wait for a fixed release, which the human pursues upstream; the workaround of unit 0006 stays, so
  that requirements 5 to 7, 11 and the parts of 12 to 14 on emoji that came from unit 0014 are replaced by a check
  that such characters leave nothing in their place and a test that tells whether the release in use has the fix;
  reading them is left to a later unit, whose test checks that 😀 is read with "grinning face"; and eSpeak NG reads
  the data that `piper-phonemize` holds itself, in place of the copy in the model folder. The status is `draft`
- 2026-11-20T01:03:27Z [human] approved the spec. The status is `approved`
- 2026-11-20T01:09:56Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a unit run of the sandbox, on
  `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` at `fdbc721`, read `process.md`, the standards and
  `roles/planner.md` of the installed kit, and stopped before writing `plan.md`, because the tree and the releases
  the spec names leave open two things the plan needs and may not decide. The unit goes back to the specifier and to
  the human. Found from the release assets of sherpa-onnx `v2.0.2` and of `piper-phonemize` `v1.5.9`, which are
  published under `csukuangfj/piper-phonemize` and not under `k2-fsa`. First, the archive outgrows the limit of 30 MB
  that requirement 2 of unit 0004 set and that requirement 8 keeps: the Java binding and the two native libraries of
  sherpa-onnx 2.0.2 are 20,750,113 bytes against 20,164,193 for 1.13.8, and those of `piper-phonemize` 1.5.9 are
  9,631,992 more, 9,042,593 of them the Java archive with all of `espeak-ng-data`, of which `ru_dict` alone is 8.5 MB
  unpacked and `en_dict` 167 KB. With the archive of 20.7 MB that unit 0026 built, that gives about 30.9 MB, and
  `./gradlew buildPlugin` fails at its limit of 30,000,000 bytes. The spec takes the whole Java archive as given, in
  its decision on the data of eSpeak NG, and says nothing of the limit. Second, the native library of
  `piper-phonemize` 1.5.9 for Linux x64 needs glibc 2.38, for `__isoc23_strtoul`, where those of sherpa-onnx 1.13.8
  and 2.0.2 need 2.16. Loading it in the sandbox, Debian 12 with glibc 2.36, failed with ``GLIBC_2.38' not found``,
  so the plugin would no longer speak on Debian 12, Ubuntu 22.04 or RHEL 9, which it does now, and the tests of
  requirements 3, 6, 7 and 9 cannot run in the sandbox. The library for macOS needs macOS 11.0, as sherpa-onnx does.
  Also found: sherpa-onnx 2.0.2 warns that `data_dir` of Kokoro is ignored, and `GenerationConfig` takes the
  phonemes as `int[][]`, one row per sentence, which `PiperPhonemize.Result.getPhonemes` gives in that shape. The
  status is `approved`
- 2026-11-20T01:20:34Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux
  host, on `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` at `138b3e8`, at the human's request, corrected
  `spec.md` for what the planner found. Confirmed on the host that the native library for Linux x64 in
  `piper-phonemize-native-lib-linux-x64-1.5.9.jar` needs glibc 2.38, 2.36, 2.35 and 2.34 for `__isoc23_strtoul`,
  `arc4random`, `_dl_find_object` and `pthread_once`, that `libpiper_phonemize_core.so` of the same release needs
  2.14, and that the libraries of sherpa-onnx 1.13.8 need 2.16, and read that `jar.yaml` of `piper-phonemize` builds
  the library on `ubuntu-latest`, where `linux-jni.yaml` of sherpa-onnx uses `quay.io/pypa/manylinux2014_x86_64`.
  Decided with the human: the Java archive goes in without `espeak-ng-data` and eSpeak NG reads the copy in the
  model folder, which reverses the earlier choice, and the project builds the Linux library itself in that image
  until a release needs no newer glibc, without waiting for the maintainer. Added requirements 16 to 18 and changed
  requirements 1, 2 and 10 and the decisions to match. Decided by me and open to the human: the library is committed
  with its checksum rather than built at each build of the plugin, and the developer runs the script on the host
  where the sandbox has no Docker. The status is `approved`
- 2026-11-20T01:26:14Z [Claude Code, anthropic/claude-opus-5-5] as the specifier, in the main session on the Linux host, on
  `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` at `6c9ed76`, at the human's request, tried the build of
  requirement 16 on the host before planning, outside the project. In `quay.io/pypa/manylinux2014_x86_64`, CentOS 7.9
  with glibc 2.17, gcc 10.2.1 and CMake 4.4.4, `yum install -y java-11-openjdk-devel` gave the headers of JNI, with
  `JAVA_HOME` at `/usr/lib/jvm/java-11-openjdk-11.0.23.0.9-2.el7_9.x86_64`, and the source of `piper-phonemize` at the
  tag `v1.5.9`, commit `dae4d2d`, built with the options of `jar.yaml` and `-DCMAKE_BUILD_TYPE=Release`. The library
  `libpiper-phonemize-jni.so` is 1,048,032 bytes, needs no symbol version of glibc newer than 2.14, and links
  `libm`, `libpthread`, `libc` and `ld-linux-x86-64` only. Packed as the release packs it, under
  `piper-phonemize/native/linux-x64/`, it is 340,718 bytes. With `piper-phonemize-jvm-1.5.9.jar`, it loaded and
  phonemized in `eclipse-temurin:21-jdk-jammy`, Ubuntu 22.04 with glibc 2.35, where the library of the release does
  not load, and gave the same phonemes with the `espeak-ng-data` of the fp32 model folder as with the data of the
  Java archive, for "I have 3 cats in 2026.", "It builds for x86_64 and ARM64 with v1.13.8." and "Before the sign, ❤
  and after the sign.". The files the container wrote belong to root, so the script is to run Docker as the user of
  the host. The status is `approved`
- 2026-11-20T01:41:16Z [Claude Code, anthropic/claude-opus-5-5] as the planner, in a unit run on the Linux host, on
  `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` at `7085beb`, read `process.md`, the standards and
  `roles/planner.md` of the installed kit, and wrote `plan.md`. Read the Kokoro front end of sherpa-onnx at
  `v1.13.8`, the handling of `phonemeCodepoints` at `v2.0.2`, and the Java binding and C library of
  `piper-phonemize` at `v1.5.9`, and the plan has the plugin's front end do what that of 1.13.8 did. Found that
  `piper-phonemize` initializes eSpeak NG once per process and reads its data folder again at each call: tried on
  the host with the release's Linux library, a data folder deleted after the initialization made every call give
  only `.`. A reading, a change of the model and the removal of the first model in one session would then drop
  every word not in the lexicon. To keep eSpeak NG reading the data of the model folder in use, as requirement 17
  says, the plan initializes it with a folder of the plugin's own that holds a link to the `espeak-ng-data` of the
  model in use, pointed again at each creation of the engine. Decided in the plan as well: words are lowercased before
  eSpeak NG is given them, as 1.13.8 did; characters from U+4E00 to U+9FFF, which 1.13.8 dropped, go to eSpeak NG as
  any other word not in the lexicon; and the script runs the container as root, for `yum`, and gives the one file it
  copies out to the user of the host, rather than running Docker as that user. The status is `approved`
- 2026-11-20T01:45:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, in a unit run on the Linux host, on
  `feature-0030-sherpa-onnx-2-with-the-plugin-s-own-phonemes` at `4d2f9be`, read `process.md`, the standards, `roles/implementer.md`,
  `spec.md`, `plan.md` and this record, and began the work. The status is `in-progress`
- 2026-11-20T01:58:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch at `4d2f9be`
  plus the two new measurement classes, measured under sherpa-onnx 1.13.8 on the Linux host (AMD Ryzen 9 9950X3D, 6
  engine threads, Linux amd64). `EngineLengthsMeasurement` with the int8 model at the normal speed, through
  `withCharactersAboveFfffAsSpaces`, in samples at 24 kHz: "I have 3 cats in 2026." 60,305; "Version 4.2 is 5% faster &
  smaller." 80,202; "Read the README of the API with npm." 65,916; "It builds for x86_64 and ARM64 with v1.13.8." 138,351;
  "AloudRite reads Markdown aloud." 47,194; "Before the sign, and after the sign." 45,037; with a heart U+2764 56,273; with
  😀 45,256; with U+20000 45,191; with U+10330 45,032; with U+1D400 45,252. `FirstAudioMeasurement` with the fp32 model,
  for the piece "Markdown features": creation of the engine to the first audio 641, 621, 649, 640 and 647 ms, median
  641 ms, and the first creation in the process 644 ms; `speak` to the first audio of an engine made beforehand 168,
  135, 131, 131 and 135 ms, median 135 ms. `./gradlew buildPlugin` gave an archive of 20,727,090 bytes. The measured
  values vary a little from run to run (the emoji sentences differ by up to 219 samples), so the 10% of requirement 3 is
  wide against that. The status is `in-progress`
- 2026-11-20T02:50:00Z [Claude Code, anthropic/claude-sonnet-5-5] as the implementer, on the unit's branch, wrote
  `scripts/build-piper-phonemize-linux-x64.sh` as planned and ran it with `v1.5.9` on the host, where Docker runs. The
  source is at commit `dae4d2d831f9f0817355f7c0e8abd98c2fb0a49d`. The library in the jar needs glibc 2.14 at most, links
  `libm`, `libpthread`, `libc` and `ld-linux-x86-64` only (`objdump -p`), and the jar
  `native/piper-phonemize-native-lib-linux-x64-1.5.9.jar` is 342,963 bytes with the SHA-256
  `74c2c9f052836906b973d868cfdbfefe38e59681a7c6b02fa0f1acd78d771310`. The jar is owned by the user of the host. Moved
  `build.gradle.kts` to sherpa-onnx 2.0.2 and `piper-phonemize` 1.5.9, with the stripped Java archive (17 entries and
  11,529 bytes, the manifest of the release as it is), the macOS library of the release, the committed Linux jar behind
  `verifyPiperPhonemizeLinuxX64` and the system property of the published jar for the tests. Checked that
  `./gradlew buildPlugin` fails with the checksum expected and the one found when one byte is appended to the jar, and
  passes with the jar as made, with an archive of 21,879,994 bytes. The status is `in-progress`

