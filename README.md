# AloudRite

AloudRite is meant to become an IntelliJ IDEA plugin that reads Markdown and agentic coding documents aloud.

It does not read a file aloud yet. What is here is the proof that the speech works: the plugin adds a "Read Aloud"
button with a loudspeaker icon to the editor tab row of a Markdown file, beside the icons that switch between editor
and preview. Pressing it speaks one fixed sentence, "AloudRite can speak. This sentence was made on your own
machine.", through the default audio output of the machine, whatever the file holds. The sentence is a placeholder
for reading the file. The speech is made on the machine itself by the Kokoro model, in the voice `af_heart` at
normal speed, and the plugin makes no network request.

Speech works on Linux x64 and on macOS on Apple Silicon, and needs the model described under "The speech model"
below. A press shows a message in place of the sentence in two cases: when the model is not in its folder, and
when the platform is another one. On another platform the plugin still loads and shows the button, and says that
speech is not supported there. A second press while the sentence is being made or played is ignored.

The plugin is written in Kotlin, is built with the [IntelliJ Platform Gradle Plugin][gradle-plugin], and targets
IntelliJ IDEA 2026.2. It requires no other plugin, so it loads in any IDE built on the IntelliJ Platform. The plugin
archive carries sherpa-onnx with its native libraries for the two platforms, about 20 MB, and nothing has to be
installed on the machine besides the model. The button appears only where the IDE's Markdown plugin is enabled.

[gradle-plugin]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html

## Requirements

JDK 25 is the only thing that has to be installed. The Gradle wrapper in the repository downloads Gradle itself.

The first build also downloads IntelliJ IDEA, about 1.5 GB, to compile against and to run the plugin in. It is kept
in the Gradle user home and in `.intellijPlatform/`, which git ignores. It also fetches the three jars of
sherpa-onnx 1.13.8 from GitHub (`github.com`), so the build needs a network connection.

## The speech model

The plugin needs the Kokoro v1.0 model package of sherpa-onnx, which is neither in the plugin archive nor in the
repository:

* Package: `kokoro-int8-multi-lang-v1_0.tar.bz2`, 132 MB packed and 190 MB unpacked
* Address: https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2
* SHA-256: `4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e`. The publisher has replaced the package
  at that address before, so a different checksum means a different package and not necessarily a damaged one

It is unpacked into one folder per user, outside the directories of any IDE, so that the sandbox IDE, an installed
IDE and the tests all use the same copy. The package unpacks to a directory of its own, so the model ends up in:

* Linux: `$XDG_DATA_HOME/aloudrite/kokoro-int8-multi-lang-v1_0`, where `XDG_DATA_HOME` is set to an absolute path,
  and `~/.local/share/aloudrite/kokoro-int8-multi-lang-v1_0` otherwise
* macOS: `~/Library/Application Support/AloudRite/kokoro-int8-multi-lang-v1_0`

On Linux, with `XDG_DATA_HOME` not set:

```sh
mkdir -p ~/.local/share/aloudrite
cd "$(mktemp -d)"
curl -fLO https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2
echo "4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e  kokoro-int8-multi-lang-v1_0.tar.bz2" | sha256sum -c -
tar -xjf kokoro-int8-multi-lang-v1_0.tar.bz2 -C ~/.local/share/aloudrite
```

On macOS:

```sh
mkdir -p ~/Library/Application\ Support/AloudRite
cd "$(mktemp -d)"
curl -fLO https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2
echo "4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e  kokoro-int8-multi-lang-v1_0.tar.bz2" | shasum -a 256 -c -
tar -xjf kokoro-int8-multi-lang-v1_0.tar.bz2 -C ~/Library/Application\ Support/AloudRite
```

The plugin counts the model as in place when its folder holds `model.int8.onnx`, `voices.bin`, `tokens.txt`,
`lexicon-us-en.txt` and the directory `espeak-ng-data`. It looks at nothing more, so a folder that holds those but
is damaged or from another package is not noticed until the engine fails.

The automated test that makes the audio of the test sentence, without playing it, runs only where the model is in
its folder. Without the model it is reported as skipped.

## License

Copyright (C) 2026 Stef Klaassen

The plugin is licensed under GPL-3.0-or-later, see [LICENSE](LICENSE). It ships sherpa-onnx, whose native library
contains espeak-ng, which is licensed under GPL-3.0-or-later, so the plugin as a whole has to be offered under the
GPL. The third-party software the plugin ships or uses, with the license of each, is listed in
[THIRD-PARTY.md](THIRD-PARTY.md), and the plugin archive holds both files.

## Building

```sh
./gradlew buildPlugin
```

This builds the installable plugin archive, `build/distributions/aloudrite-intellij-0.0.1.zip`.

## Running in a sandbox IDE

```sh
./gradlew runIde
```

This starts a separate IntelliJ IDEA with the plugin loaded. It keeps its settings, caches and plugins in a sandbox
under `.intellijPlatform/sandbox/`, which git ignores, so the IDE installed on the machine and its settings are left
alone.

## Testing

```sh
./gradlew test
```

This runs the automated tests inside the test environment of the platform, without opening an IDE window.

## Installing in an IDE

1. Build the archive with `./gradlew buildPlugin`
2. In the IDE, open Settings, then Plugins
3. Click the gear icon and choose "Install Plugin from Disk..."
4. Select `build/distributions/aloudrite-intellij-0.0.1.zip` and restart the IDE when it asks

The archive installs in IDEs of version 2026.2 and later.

## Development

New features of this project are built with [AgentRite](https://github.com/norareit/agentrite). The details are in
[AGENTS.md](AGENTS.md).
