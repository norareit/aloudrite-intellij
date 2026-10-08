# Third-party software

AloudRite itself is licensed under GPL-3.0-or-later, see [LICENSE](LICENSE). This file lists the third-party software
that the plugin archive ships or that the plugin uses at run time, with the license of each.

Every license below was read at the source named in its entry. The native libraries in the archive are the ones
sherpa-onnx 1.13.8 publishes. The libraries linked into them were taken from the build files of sherpa-onnx at the tag
`v1.13.8` (`CMakeLists.txt`, `sherpa-onnx/csrc/CMakeLists.txt`, `sherpa-onnx/jni/CMakeLists.txt` and `cmake/`), which
the build of the JNI library with text-to-speech follows. That the published binaries were built exactly like that
was not checked.

## In the plugin archive

| Software                        | Version                 | License                | Where it comes from                                                                                |
|---------------------------------|-------------------------|------------------------|----------------------------------------------------------------------------------------------------|
| sherpa-onnx (Java binding)      | 1.13.8                  | Apache-2.0             | https://github.com/k2-fsa/sherpa-onnx, release `v1.13.8`, `sherpa-onnx-jvm-1.13.8.jar`             |
| sherpa-onnx (native libraries)  | 1.13.8                  | Apache-2.0             | the same release, `sherpa-onnx-native-lib-linux-x64-1.13.8.jar` and `...-osx-aarch64-1.13.8.jar`  |
| ONNX Runtime                    | 1.28.2                  | MIT                    | https://github.com/microsoft/onnxruntime, in the two native jars as `libonnxruntime`              |
| espeak-ng                       | commit `ed530aa` of the fork https://github.com/csukuangfj/espeak-ng | GPL-3.0-or-later | linked into `libsherpa-onnx-jni`; its `README.md` says GPL version 3 or later |
| piper-phonemize                 | commit `f3ff95a`        | MIT                    | https://github.com/csukuangfj/piper-phonemize, linked into `libsherpa-onnx-jni`                    |
| uni-algo                        | as in piper-phonemize   | Unlicense (public domain) | `licenses/uni-algo/LICENSE.md` of piper-phonemize                                               |
| kaldi-native-fbank              | 1.22.3                  | Apache-2.0             | https://github.com/csukuangfj/kaldi-native-fbank, linked into `libsherpa-onnx-jni`                 |
| kaldi-decoder                   | 0.3.0                   | Apache-2.0             | https://github.com/k2-fsa/kaldi-decoder, linked into `libsherpa-onnx-jni`                          |
| OpenFst                         | 1.8.5-2026-07-09 (fork) | Apache-2.0             | https://github.com/csukuangfj/openfst, linked into `libsherpa-onnx-jni`                            |
| simple-sentencepiece            | 0.7                     | Apache-2.0             | https://github.com/pkufool/simple-sentencepiece, linked into `libsherpa-onnx-jni`                  |
| hclust-cpp                      | 2026-02-25              | BSD-2-Clause           | https://github.com/csukuangfj/hclust-cpp, built into `libsherpa-onnx-jni` for speaker diarization |
| nlohmann/json                   | 3.12.0                  | MIT                    | https://github.com/nlohmann/json, header-only, compiled into `libsherpa-onnx-jni`                  |

Notes:

* espeak-ng is the reason the plugin is licensed under GPL-3.0-or-later: the native library of sherpa-onnx contains it,
  and a plugin that ships that library has to be offered under the GPL as a whole.
* The espeak-ng fork also carries code under the 2-clause BSD license (`COPYING.BSD2`, a `getopt_long` implementation
  from NetBSD) and Unicode data files (`COPYING.UCD`), and a file `COPYING.APACHE` with the text of the Apache License 2.0. Which part of the fork each of these three files covers, and whether the binary uses them, was not checked.
* hclust-cpp is listed with the license its `LICENSE` file states: a notice of copyright by Daniel Muellner and
  Christoph Dalitz and the three clauses and the disclaimer of the 2-clause BSD license. Whether the text matches
  BSD-2-Clause word for word was not checked.
* ONNX Runtime is the build of https://github.com/csukuangfj/onnxruntime-libs, release `v1.28.2`, named by the build
  files of sherpa-onnx. Its license was read in the repository of Microsoft at the tag `v1.28.2`.
* The plugin makes no network request. The libraries are not fetched at run time.

## Fetched by the user

| Software                 | Version | License    | Where it comes from                                                                              |
|--------------------------|---------|------------|--------------------------------------------------------------------------------------------------|
| Kokoro v1.0 model package | `kokoro-int8-multi-lang-v1_0.tar.bz2` | Apache-2.0 | https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2 |

The license is the one in the `LICENSE` file inside the package, read after unpacking it. The package also holds the
data of espeak-ng (`espeak-ng-data`), which is under the license of espeak-ng above, and the voices and dictionaries
of the Kokoro model. The package is not part of the plugin archive and is not in the repository. The README says how
to get it.
