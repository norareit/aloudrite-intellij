# Third-party software

AloudRite itself is licensed under GPL-3.0-or-later, see [LICENSE](LICENSE). This file lists the third-party software
that the plugin archive ships or that the plugin uses at run time, with the license of each.

Every license below was read at the source named in its entry. The native libraries of sherpa-onnx in the archive are
the ones sherpa-onnx 2.0.2 publishes. The libraries linked into them were taken from the build files of sherpa-onnx at
the tag `v2.0.2` (`CMakeLists.txt`, `sherpa-onnx/csrc/CMakeLists.txt`, `sherpa-onnx/jni/CMakeLists.txt` and `cmake/`),
which the build of the JNI library with text-to-speech follows. The native libraries of piper-phonemize in the archive
are the ones release 1.6.1 publishes, for Linux x64 and for macOS on Apple Silicon, and the project builds none of them.
What they hold was taken from `CMakeLists.txt` of piper-phonemize at the tag `v1.6.1` (commit `5f92f0b`) and from the
files of that tag that it names. That the published binaries were built exactly like that was not checked.

## In the plugin archive

| Software                        | Version                 | License                | Where it comes from                                                                                |
|---------------------------------|-------------------------|------------------------|----------------------------------------------------------------------------------------------------|
| sherpa-onnx (Java binding)      | 2.0.2                   | Apache-2.0             | https://github.com/k2-fsa/sherpa-onnx, release `v2.0.2`, `sherpa-onnx-jvm-2.0.2.jar`               |
| sherpa-onnx (native libraries)  | 2.0.2                   | Apache-2.0             | the same release, `sherpa-onnx-native-lib-linux-x64-2.0.2.jar` and `...-osx-aarch64-2.0.2.jar`   |
| ONNX Runtime                    | 1.28.2                  | MIT                    | https://github.com/microsoft/onnxruntime, in the two native jars as `libonnxruntime`              |
| piper-phonemize (Java binding)  | 1.6.1                   | GPL-3.0                | https://github.com/csukuangfj/piper-phonemize, release `v1.6.1`, `piper-phonemize-jvm-1.6.1.jar`, carried without its `espeak-ng-data` |
| piper-phonemize (native library for macOS) | 1.6.1        | GPL-3.0                | the same release, `piper-phonemize-native-lib-osx-aarch64-1.6.1.jar`                              |
| piper-phonemize (native library for Linux x64) | 1.6.1    | GPL-3.0                | the same release, `piper-phonemize-native-lib-linux-x64-1.6.1.jar`                                |
| espeak-ng                       | commit `ed530aa` of the fork https://github.com/csukuangfj/espeak-ng | GPL-3.0-or-later | linked into `libpiper-phonemize-jni`; its `README.md` says GPL version 3 or later |
| uni-algo                        | as in piper-phonemize   | Unlicense (public domain) | `src/uni_algo.h` and `licenses/uni-algo/LICENSE.md` of piper-phonemize                          |
| kaldi-native-fbank              | 1.23.1                  | Apache-2.0             | https://github.com/csukuangfj/kaldi-native-fbank, linked into `libsherpa-onnx-jni`                 |
| kaldi-native-io                 | 1.23.2                  | Apache-2.0             | https://github.com/csukuangfj/kaldi_native_io, named by `cmake/` of sherpa-onnx                    |
| kaldi-decoder                   | 0.4.0                   | Apache-2.0             | https://github.com/k2-fsa/kaldi-decoder, linked into `libsherpa-onnx-jni`                          |
| kaldi-hmm-gmm                   | 1.2.1                   | Apache-2.0             | https://github.com/csukuangfj/kaldi-hmm-gmm, linked into `libsherpa-onnx-jni`                      |
| kaldifst                        | 1.9.0                   | Apache-2.0             | https://github.com/k2-fsa/kaldifst, named by `cmake/` of sherpa-onnx                               |
| OpenFst                         | 1.8.5-2026-07-09 (fork) | Apache-2.0             | https://github.com/csukuangfj/openfst, named by `cmake/` of sherpa-onnx                            |
| kissfft                         | commit `febd4ca`        | BSD-3-Clause           | https://github.com/mborgerding/kissfft, named by `cmake/` of sherpa-onnx                           |
| utf8proc                        | 2.12.0                  | MIT                    | https://github.com/JuliaStrings/utf8proc, linked into `libsherpa-onnx-jni`; its `LICENSE.md` also covers Unicode data |
| simple-sentencepiece            | 0.7                     | Apache-2.0             | https://github.com/pkufool/simple-sentencepiece, linked into `libsherpa-onnx-jni`                  |
| hclust-cpp                      | 2026-02-25              | BSD-2-Clause           | https://github.com/csukuangfj/hclust-cpp, built into `libsherpa-onnx-jni` for speaker diarization |
| nlohmann/json                   | 3.12.0                  | MIT                    | https://github.com/nlohmann/json, header-only, compiled into `libsherpa-onnx-jni`                  |
| commonmark-java                 | 0.30.1                  | BSD-2-Clause           | https://github.com/commonmark/commonmark-java, `commonmark-0.30.1.jar` from Maven Central          |
| commonmark-java extensions      | 0.30.1                  | BSD-2-Clause           | the same repository, `commonmark-ext-gfm-strikethrough`, `-gfm-tables`, `-task-list-items` and `-autolink` |
| autolink-java                   | 0.12.0                  | MIT                    | https://github.com/robinst/autolink-java, `autolink-0.12.0.jar` from Maven Central, used by `commonmark-ext-autolink` |

Notes:

* espeak-ng and piper-phonemize are the reason the plugin is licensed under GPL-3.0-or-later: the native library of
  piper-phonemize contains espeak-ng, and the repository of piper-phonemize holds a GPL-3.0 license in `LICENSE.md` at
  the tag `v1.6.1`. A plugin that ships them has to be offered under the GPL as a whole. sherpa-onnx 2.0 dropped
  espeak-ng for that reason, and the plugin hands the engine the phonemes that piper-phonemize makes.
* The Java archive of piper-phonemize is the one the release publishes, without the entries under
  `piper-phonemize/espeak-ng-data/`, which would carry the data of every language of espeak-ng. The plugin reads the
  `espeak-ng-data` of the model package instead, which is under the license of espeak-ng above.
* The native library of piper-phonemize for Linux x64 is the one the release publishes. It needs no symbol version of
  glibc newer than 2.14, which was read with `objdump -T` from the library in the jar and is recorded in unit 0035 under
  `changes/`. The release builds it in the manylinux2014 container images from the source at its tag.
* `CMakeLists.txt` of piper-phonemize at the tag `v1.6.1` fetches eSpeak NG as the archive of commit `ed530aa` of the
  fork, builds it as a static library and links it, with its `ucd-tools`, into the library, and names no other
  software. `src/uni_algo.h` and `licenses/uni-algo/` are in the tree of that tag.
* Which of the libraries that `cmake/` of sherpa-onnx names are in the binaries of 2.0.2, and which of those the JNI
  library links, was not checked beyond the libraries that `sherpa-onnx/csrc/CMakeLists.txt` lists: the entries marked
  as named by `cmake/` only are named there. The licenses of kaldi-native-io, kaldifst and kaldi-hmm-gmm were read
  in their `LICENSE` files, which are the Apache License 2.0 with a notice of legal notices on top, and that of kissfft
  in its `COPYING`, which names BSD-3-Clause.
* The espeak-ng fork also carries code under the 2-clause BSD license (`COPYING.BSD2`, a `getopt_long` implementation
  from NetBSD) and Unicode data files (`COPYING.UCD`), and a file `COPYING.APACHE` with the text of the Apache License 2.0. Which part of the fork each of these three files covers, and whether the binary uses them, was not checked.
* hclust-cpp is listed with the license its `LICENSE` file states: a notice of copyright by Daniel Muellner and
  Christoph Dalitz and the three clauses and the disclaimer of the 2-clause BSD license. Whether the text matches
  BSD-2-Clause word for word was not checked.
* ONNX Runtime is the build of https://github.com/csukuangfj/onnxruntime-libs, release `v1.28.2`, named by the build
  files of sherpa-onnx. Its license was read in the repository of Microsoft at the tag `v1.28.2`.
* commonmark-java was read in the `META-INF/LICENSE.txt` of each of its five jars, which holds the 2-clause BSD license
  with a notice of copyright by Atlassian Pty Ltd, and in `LICENSE.txt` of the repository at the tag
  `commonmark-parent-0.30.1`, which holds the same license with a notice of copyright by Robin Stocker. Whether the
  texts match BSD-2-Clause word for word was not checked.
* autolink-java carries no license file in its jar. Its license was read in `LICENSE` of the repository at the tag
  `autolink-0.12.0`, the MIT license with a notice of copyright by Robin Stocker, and its Maven POM names the MIT
  license as well.
* The libraries are not fetched at run time. The only things the plugin fetches are the models, described below,
  and only after the user agreed.

## Downloaded at the request of the user

| Software                 | Version | License    | Where it comes from                                                                              |
|--------------------------|---------|------------|--------------------------------------------------------------------------------------------------|
| Kokoro v1.0, int8 model | `kokoro-int8-multi-lang-v1_0` | Apache-2.0 | https://huggingface.co/csukuangfj/kokoro-int8-multi-lang-v1_0 at commit `57adb37fc0e8d4bd430cfc1b8f8c2294945b9af2` |
| Kokoro v1.0, fp32 model | `kokoro-multi-lang-v1_0` | Apache-2.0 | https://huggingface.co/csukuangfj/kokoro-multi-lang-v1_0 at commit `f7b96bb6bef5c5da4d3aa4f4e0498fbbf62dc78b` |
| Aligner model `english-mfa-v3.1.0` | `english-mfa-v3.1.0.tar.bz2` | CC BY 4.0 | https://huggingface.co/csukuangfj/fa-models at commit `39914ed8514a568e2d24a5e785608e1abb429133` |

The license of a Kokoro model is the one in the `LICENSE` file among its files, read at the commit named above. The files
of a model also hold the data of espeak-ng (`espeak-ng-data`), which is under the license of espeak-ng above and which the plugin reads, and the voices and dictionaries
of the Kokoro model. The files are not part of the plugin archive and are not in the repository. The plugin
downloads them at the request of the user, or the user fetches them. The README describes the download and how to put the model in place by hand.

The aligner model is the `english_mfa` acoustic model, version v3.1.0, of the Montreal Forced Aligner project
(https://github.com/MontrealCorpusTools/mfa-models), which sherpa-onnx re-packages, keeping the files that its forced
aligner needs, and publishes in the repository `csukuangfj/fa-models` on Hugging Face. Its license, CC BY 4.0, requires attribution, which this entry
gives: the model is by the Montreal Forced Aligner project, and the plugin uses the re-packaged files unchanged. The
license was read in the `README.md` inside the package, after unpacking it, which says "License: CC BY 4.0 (as declared by
the upstream model card)" and refers to the source repository for the terms. The upstream model card itself was not
read. The package holds no other license file. The model is not part of the plugin archive and is not in the repository.
The plugin downloads it with a speech model, or on its own, at the request of the user, or the user fetches it. The README
describes the download and how to put the model in place by hand.
