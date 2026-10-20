# AloudRite

AloudRite is meant to become an IntelliJ IDEA plugin that reads Markdown and agentic coding documents aloud.

It reads a Markdown file aloud. The plugin adds a "Read Aloud" button with a loudspeaker icon to the editor tab row
of a Markdown file, beside the icons that switch between editor and preview. Pressing it reads the file from its
beginning to its end through the default audio output of the machine, as the editor holds it at the moment of the
press, changes that are not saved included. What is read stays the text of that moment: a change made to the file
while it is read does not change what is heard. The speech is made on the machine itself by the Kokoro model, in the
voice `af_heart` at normal speed, and the first sound comes before the whole file is turned into audio. The engine
makes the audio on half the processors the IDE may use, rounded down, at least 1 and at most 6 threads, so that the
number follows from the machine and leaves the rest of it to the IDE, and the IDE's log gives the number each time
the engine is created. The first
press without the model asks whether to download it, and that download is the only network request the plugin
makes.

The text is read as Markdown, without its markers: the `#` of a heading, the asterisks of bold and italic, the markers
of list items, the boxes of a task list and the backslash of an escaped character are left out, and so are struck out
text, the address of a link, the definition of a reference link, front matter at the top of the file and HTML comments.
A code block is read as "code example" followed by two seconds of silence, a table as "see table", and an address
without text of its own as "link". Silence is added where the structure asks for a pause: half a second between two
blocks, such as two paragraphs, and in place of a horizontal rule, and a quarter of a second before and after inline
code. Each item of a list ends as a sentence does, with a full stop added where it has none of its own, and a quarter of
a second of silence comes between two items. Characters above U+FFFF, among them most emoji, are left out of the
reading: the speech engine cannot be handed them intact, and would make a few seconds of babble for each. Emoji and
symbols of three bytes, such as ❤ and ✓, do reach the engine and may be read by their names. How each kind of Markdown
is read, and how it sounded before, is described in [docs/reading-markdown.md](docs/reading-markdown.md). The file
itself is never changed. While a file is read, the button on its tab shows a stop icon and the tooltip "Stop Reading",
and pressing it stops the reading. When the end of the file is reached, the button shows the loudspeaker and "Read
Aloud" again. One file is read at a time: pressing "Read Aloud" on another Markdown file stops the first and reads the
second. The reading goes on when another tab or tool window is selected, and stops when the tab of the file is closed,
when its project is closed, and when the IDE exits. Pressing the button on a file that would read nothing, because it is
empty or holds only white space, or nothing but front matter, HTML comments or struck out text, reads nothing and shows
a notification that there is nothing to read. A file of nothing but a code block reads "code example". When the speech
fails during a reading, the reading ends, a message says so, and the next press works.

Every project has a tool window named "AloudRite", with the loudspeaker icon. When a project is first opened with the
plugin, the tool window is docked on the right and closed, and its button is on the tool window bar on that side; it
is also under View › Tool Windows. It does not open by itself, also not when a reading starts, and it stays where the
user puts it, docked on any side, floating or in a window of its own, as any tool window does. While nothing is read,
it says so, and that a reading is started with "Read Aloud" on the tab of a Markdown file. While a file is read, it
shows the name of the file, the name of its project when that is another one, and a "Stop" button that stops the
reading as the button on the tab does. Since one file is read in the whole IDE, the tool window of every project shows
that reading and can stop it.

Speech works on Linux x64 and on macOS on Apple Silicon, and needs the model described under "The speech model"
below. A press shows a message or a question in place of the reading in two cases: when the model is not in its
folder, where it asks whether to download the model, and when the platform is another one. On another platform the
plugin still loads and shows the button, and says that speech is not supported there. A press while the model is
being downloaded is ignored.

The plugin is written in Kotlin, is built with the [IntelliJ Platform Gradle Plugin][gradle-plugin], and targets
IntelliJ IDEA 2026.2. It requires no other plugin, so it loads in any IDE built on the IntelliJ Platform. The plugin
archive carries sherpa-onnx with its native libraries for the two platforms, about 20 MB, and the Markdown parser
commonmark-java with four of its extensions, about 300 KB, and nothing has to be installed on the machine besides the
model. The button appears only where the IDE's Markdown plugin is enabled.

[gradle-plugin]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html

## Requirements

JDK 25 or newer is the only thing that has to be installed. The Gradle wrapper in the repository downloads Gradle itself.

The first build also downloads IntelliJ IDEA, about 1.5 GB, to compile against and to run the plugin in. It is kept
in the Gradle user home and in `.intellijPlatform/`, which git ignores. It also fetches the three jars of
sherpa-onnx 1.13.8 from GitHub (`github.com`), so the build needs a network connection.

## The speech model

The plugin needs the Kokoro v1.0 model package of sherpa-onnx, which is neither in the plugin archive nor in the
repository. The plugin downloads it the first time "Read Aloud" is pressed and the model is not in place:

* It asks first, and states the size of the download, the disk space the model takes, where it is downloaded from and
  the folder it goes to. Nothing is downloaded before you agree, and when you decline, the question is asked again at
  the next press
* It downloads the package, 132 MB, from the address below over HTTPS, using the proxy settings of the IDE, checks it
  against the SHA-256 below, and unpacks it into the model folder, where it takes 190 MB. About 322 MB have to be free
  for a moment
* The download runs in the background, so the IDE stays usable, and its progress shows in the status bar, where it can
  be cancelled at any moment. When the model is in place, the file the button was pressed on is read by itself, with
  the text it had at the press, unless its tab or its project was closed meanwhile
* The model folder comes into being by one rename of a complete, unpacked and flushed directory. Nothing of a failed
  or cancelled download is left on disk, except one empty lock file, `.kokoro-int8-multi-lang-v1_0.install.lock`, next
  to the model folder, which is kept also after a success. What an IDE that was killed in the middle left behind is
  removed at the next download. Every file and directory is flushed to the disk before that rename, which is the most
  Java offers against a power loss, and a power loss was not tried. One limit is known: on macOS the drive may hold the
  last writes in its own cache, which the plugin cannot make it empty
* One download runs at a time, also across IDE windows: the plugin takes a lock on that file before it asks or touches
  anything. While another IDE window holds it, a press starts no download, asks nothing, and says that a download is
  already running in another IDE window
* A failure shows a notification that says what went wrong and offers to try again. When the checksum does not match,
  the package at the address is not the one this version of the plugin was built for, and putting a model in place by
  hand, as described below, keeps working
* A model folder that exists, is not empty and does not hold a complete model is not touched, since the plugin did
  not make it. The download then fails before any request, and the folder has to be removed by hand
* A model that is in place already, downloaded before or put there by hand, is used as it is: no question is asked
  and no request is made

The package:

* Package: `kokoro-int8-multi-lang-v1_0.tar.bz2`, 132 MB packed and 190 MB unpacked
* Address: https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2
* SHA-256: `4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e`. The publisher has replaced the package
  at that address before, so a different checksum means a different package and not necessarily a damaged one. The
  download then fails until a new version of the plugin, built for the new package, is published

It is unpacked into one folder per user, outside the directories of any IDE, so that the sandbox IDE, an installed
IDE and the tests all use the same copy. The package unpacks to a directory of its own, so the model ends up in:

* Linux: `$XDG_DATA_HOME/aloudrite/kokoro-int8-multi-lang-v1_0`, where `XDG_DATA_HOME` is set to an absolute path,
  and `~/.local/share/aloudrite/kokoro-int8-multi-lang-v1_0` otherwise
* macOS: `~/Library/Application Support/AloudRite/kokoro-int8-multi-lang-v1_0`

To remove the model, delete its folder:

```sh
rm -rf ~/.local/share/aloudrite/kokoro-int8-multi-lang-v1_0                          # Linux
rm -rf ~/Library/Application\ Support/AloudRite/kokoro-int8-multi-lang-v1_0         # macOS
```

Putting the model in place by hand is an alternative to the download, for a machine without network access or for
when the download fails. On Linux, with `XDG_DATA_HOME` not set:

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

The automated tests that run the real engine, without playing anything, run only where the model is in its folder.
Without the model they are reported as skipped.

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

The folder `samples/` holds Markdown files to open there and listen to. `markdown-features.md` first holds every kind of
Markdown the plugin may meet, each in a short section under a heading that names it, from headings and lists to links,
code, a table, emoji and HTML comments, with numbered and bulleted lists both with and without full stops, and then two
long chapters of plain prose, to listen to a long reading and to stop one. It begins with front matter, which is not to
be heard.
`one-sentence.md` and `ten-thousand-words.md`, which starts with the same sentence, are for comparing the time to the
first sound, which the IDE's log gives for each press. `empty.md` shows the notification that there is nothing to read.

## Testing

```sh
./gradlew test
```

This runs the automated tests inside the test environment of the platform, without opening an IDE window.

`EngineThreadsMeasurement` runs only with `ALOUDRITE_MEASURE_ENGINE_THREADS` set and the model in its folder, and is
reported as skipped otherwise. It makes the audio with 1, 2, 4, 6 and 8 threads and writes a table to
`build/engine-threads.txt`:

```sh
ALOUDRITE_MEASURE_ENGINE_THREADS=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.EngineThreadsMeasurement
```

## Installing in an IDE

1. Build the archive with `./gradlew buildPlugin`
2. In the IDE, open Settings, then Plugins
3. Click the gear icon and choose "Install Plugin from Disk..."
4. Select `build/distributions/aloudrite-intellij-0.0.1.zip` and restart the IDE when it asks

The archive installs in IDEs of version 2026.2 and later.

## Development

New features of this project are built with [AgentRite](https://github.com/norareit/agentrite). The details are in
[AGENTS.md](AGENTS.md).
