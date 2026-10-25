# AloudRite

AloudRite is an IntelliJ IDEA plugin that reads Markdown and agentic coding documents aloud.

It reads a Markdown file aloud. The plugin adds a "Read Aloud" button to the editor tab row of a Markdown file, beside
the icons that switch between editor and preview. Its icon is a person speaking, a head and shoulders seen from the
front with two sound waves beside the head, in the green of the IDE's Run icon. The plugin's buttons on the tab stand
together on a grey background of their own, a color of the IDE's theme, which sets them apart from the icons beside
them. Pressing "Read Aloud" reads the file from its
beginning to its end, or the part of it that the selection or the caret chooses as described below, through the default audio output of the machine, as the editor holds it at the moment of the
press, changes that are not saved included. What is read stays the text of that moment: a change made to the file
while it is read does not change what is heard. The speech is made on the machine itself by the Kokoro model, in the
voice `af_heart`, at the speed described below, and the first sound comes before the whole file is turned into
audio. The engine
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
a second of silence comes between two items. These lengths are those at the speed of 1.0×, and follow the speed.
Characters above U+FFFF, among them most emoji, are left out of the
reading: the speech engine cannot be handed them intact, and would make a few seconds of babble for each. Emoji and
symbols of three bytes, such as ❤ and ✓, do reach the engine and may be read by their names. How each kind of Markdown
is read, and how it sounded before, is described in [docs/reading-markdown.md](docs/reading-markdown.md). The file
itself is never changed.

What a press reads depends on the text of the file in the tab. With a selection that is not empty, a press reads what the
selection covers and nothing else. With no selection, it reads from the beginning of the sentence the caret is in to the
end of the file, a sentence being one as the plugin cuts the words that are read, without the cut at a comma, colon or
semicolon. A caret in text that is not read, such as the `#` of a heading, the address of a link or the white space
between two sentences, starts the reading at the sentence of the next words that are read, and a caret after the last
word, at the start of the file, or in front matter before the first sentence, reads the whole file. A file that is shown
only as a preview, without its text, has no caret and is read whole. The part is chosen in the Markdown of the whole file, so
that it is read as the whole file would read it there: a code block or a table that the selection touches, wholly or in
part, is read whole as "code example" or "see table", and a caret in one starts the reading at those words; a part that
begins in an item of a numbered list after its number is read with the number first; a word that the selection cuts is
read whole; a link by reference is read by its text also when its definition lies outside the part; and a selection of
front matter or of nothing but white space reads nothing and shows the notification that there is nothing to read. Only
the primary caret and its selection count: with several carets, or a block selection over several lines, a press reads by the
primary caret and its selection as it does with one, which may sound odd, and the plugin works as before. The caret and the
selection are not changed. The key "Read Aloud, Pause or Resume", while nothing is read, reads what a press on the
"Read Aloud" button of the selected editor would read. Like the text, the part is fixed at the press, also while the
reading is paused and resumed. While nothing of the file is read, the tooltip of the button is "Read Aloud" whichever of
these a press will read, with the key in the IDE's grey where the tooltip names one, as described below, and the icon the
person speaking. While a selection is read or paused, the tool window shows
"Selection" in a line of its own under the name of the file, while a reading from the caret is read or paused "From the caret",
and while a whole file is read neither. That line stays the same for the whole reading, also when the selection or the caret
changes meanwhile.

While a file is read, the button on its tab shows the IDE's pause icon and the tooltip "Pause Reading", and pressing
it pauses the reading: the sound stops within a second. While the reading is paused, the button shows the person
speaking with an upright line in front of it and the tooltip "Resume Reading", and pressing it goes on with the
reading. A resume begins again at the beginning of the piece that was being played at the pause, which is the
sentence, or the part of a long sentence that the plugin cut at a comma, colon or semicolon, so that the listener
hears it whole. A reading paused during a silence the plugin adds, such as the two seconds after "code example",
goes on at the piece after that silence, and the rest of the silence is not played. A resume plays audio that was
already made, so the first sound comes without waiting for the speech engine. What is heard after a resume is the
text of the press, also when the file was changed while the reading was paused. A reading stays paused for as long
as the IDE runs, and can be paused and resumed any number of times. From the moment a file starts to be read, and as
long as it is read or paused, a second button beside the first, with the IDE's red stop icon and the tooltip "Stop
Reading", ends the reading. When the end of the file is reached, or the reading is stopped, the stop button goes and
the first button shows the person speaking and "Read Aloud" again. One file is read at a time: pressing "Read Aloud"
on another Markdown file stops the first, also a paused one, and reads the second. The reading goes on when another
tab or tool window is selected, and stops when the tab of the file is closed, when its project is closed, and when
the IDE exits, whether it plays or is paused. Pressing "Read Aloud" on a file that would read nothing, because it is
empty or holds only white space, or nothing but front matter, HTML comments or struck out text, reads nothing and shows
a notification that there is nothing to read. A file of nothing but a code block reads "code example". When the speech
fails during a reading, also a paused one, the reading ends, a message says so, and the next press works.

Every project has a tool window named "AloudRite", whose icon is the person speaking in the grey of the IDE's other
tool window icons. When a project is first opened with the
plugin, the tool window is docked on the right and closed, and its button is on the tool window bar on that side; it
is also under View › Tool Windows. It does not open by itself, also not when a reading starts, and it stays where the
user puts it, docked on any side, floating or in a window of its own, as any tool window does. While nothing is read,
it says so, and that a reading is started with "Read Aloud" on the tab of a Markdown file. While a file is read, it
shows the name of the file, the name of its project when that is another one, a "Pause" button that pauses the
reading as the button on the tab does, and a "Stop" button that stops it as the stop button on the tab does. While
the reading is paused, it says so, and shows a "Resume" button in place of "Pause", which goes on with the reading as
the button on the tab does; "Stop" then ends the paused reading. Since one file is read in the whole IDE, the tool
window of every project shows that reading and can pause, resume and stop it.

While a file is read, the tool window also shows a row of its own under the name of the file, the part and the project,
and above the buttons: the time left and the time of the whole reading, as `9:28 left of 12:40`, and a bar of progress
beside them. Both times are for the speed in use, rounded up to the second, with the minutes not capped at 59, as
`64:05`. The whole is an estimate: the engine makes the audio of a few pieces ahead of the playing, so the plugin counts
the audio it has for the part that is made, and for the rest the length of its text at the pace the audio made so far
shows, or at a default pace before any is made. The estimate therefore gets more exact as the reading goes on, and the
time left is updated every half second, falls while the audio plays and stands still while the reading is paused. A
change of the speed changes both times within that half second. A resume and a change of the speed start the piece that
was playing again from its beginning, so the time left then rises by what of that piece is heard again. The bar shows the
share of the text of the file that has been read, not of the time, so that it only grows: it is empty at the start,
moves with the audio of the piece that plays, stands still while the reading is paused, and never moves back, also
after a change of the speed or of the estimate. While nothing is read, the row is not shown. When a reading reaches its
end by itself, the IDE's log holds one line, `Read <file> to the end in <time>, first estimated at <time>`, with the time
from the first sound to the end and the whole as it was estimated when the first piece was made.

The reading speed can be set from 0.75× to 2.0×, where 1.0× is the normal speed of the voice and the speed until one is
chosen. Its steps go by 0.05 from 0.75× to 1.3× and by 0.1 from 1.3× to 2.0×, nineteen in all. One speed holds for the
whole IDE, in every project, and it is kept when the IDE is closed and opened again. The speed is made by the speech
engine, so the voice keeps its pitch, and what is read stays the same, at every speed. Slowed down, the voice of the
model sounds less clean, and below 1.0× it may crack now and then, which is why the range starts at 0.75×. The tool
window shows the speed at all times, also while nothing is read, in a row of its own under the label "Reading speed": a
field that holds it, with a grey "×" inside it at its end, between a minus and a plus button, whose tooltips are
"Slower" and "Faster" and which set the step below and the step above. "Slower" is greyed out at 0.75× and "Faster" at
2.0×. A grey text under the row says that a change starts the sentence that is playing again at the new speed, and that
this can take a few seconds. A speed can be typed in the field, as any number from 0.75 to 2.0 with two decimals at the
most and a point as the decimal sign, such as "2", "1.5" or "1.37", also one between two steps. It is set when Enter is
pressed or the field loses focus, and is then shown with two decimals where the second is not 0 and with one otherwise.
A value the field does not accept leaves the speed as it was, and the field shows the IDE's error mark with the message
"Type a speed from 0.75 to 2.0" until the value is corrected, or until Escape puts the current speed back in it. The
same speed can be chosen from its steps under Settings › Tools › AloudRite, where a typed speed between two steps is
shown among them, and that page and the tool window of every open project show the same speed. The silences the plugin
adds follow the speed: their lengths above are divided by it, so that at 2.0× the half second between two paragraphs is
a quarter of a second and the two seconds after "code example" are one second, and at 0.75× they are two thirds of a
second and two and two thirds seconds. Under the speed, the tool window shows the speech model in use, in a row of its
own under the label "Speech model", at all times and in every project. It is read-only: the model is chosen,
downloaded and removed only on the page "AloudRite" under Settings › Tools. A model chosen there that is not in place
yet shows in the tool window once it is in use, and not before.

A reading reads at the speed set when it starts. A change of the speed during a reading stops the sound within a
second and goes on from the beginning of the piece that was playing, at the new speed, so that what was heard of that
piece is heard again. A change during a silence the plugin adds goes on at the piece after it, as a resume does. The
audio the engine made ahead at the old speed is dropped and the piece is made again, so the new speed may take as long
to be heard as the engine takes for the piece, which is several seconds for a long one. After a press on "Slower" or
"Faster", the engine begins to make the piece again only once half a second has passed without another press on
either, so that presses in a row, each within half a second of the one before, make the piece once, at the speed of
the last press. A speed typed in the field or chosen on the settings page is one change and is made at once, with no
such wait. A change while the reading is paused is heard at the resume, which starts where it would have started, at
the new speed, and may take longer than a resume otherwise does, since the piece has to be made again.

The reading can also be controlled from the keyboard, with four actions of the IDE:

| Action                      | Linux                 | macOS    |
|-----------------------------|-----------------------|----------|
| Read Aloud, Pause or Resume | Ctrl+Alt+Shift+Space  | ⌘⌥⇧Space |
| Stop Reading                | Ctrl+Alt+Shift+O      | ⌘⌥⇧O     |
| Read Slower                 | Ctrl+Alt+Shift+Comma  | ⌘⌥⇧,     |
| Read Faster                 | Ctrl+Alt+Shift+Period | ⌘⌥⇧.     |

These are their keys in the IDE's default keymaps on Linux, "Default for XWin", "Default for GNOME" and "Default for
KDE", and in its default keymap on macOS, "macOS", where they are the same keys with ⌘ for Ctrl and ⌥ for Alt. In other
keymaps the actions have no key until the user gives them one. Since one file is read in the whole IDE, each key acts on
that reading wherever the focus is: in the editor of the file that is read, in that of another file, in a tool window,
and in the window of another project. While nothing is read, "Read Aloud, Pause or Resume" reads the Markdown file of
the selected editor, as a press on its "Read Aloud" button does, the question about the download included; with another
file selected, or no editor, it does nothing. While a file is read, it pauses that reading, also from the editor of
another Markdown file, and while the reading is paused, it resumes it; to read another file then, press "Read Aloud" on
its tab. "Stop Reading" stops the reading, also a paused one, and does nothing while nothing is read. "Read Slower" and
"Read Faster" set the step below and above, as "Slower" and "Faster" in the tool window do, also while nothing is read
and while the tool window is closed. They share the half second of those buttons, so that presses of the keys and of the
buttons in a row make the piece once, and they do nothing at 0.75× and at 2.0×. After each key, the buttons on the tab,
the tool window and the settings page show the reading and the speed as after a press on their buttons. The tooltips of
the buttons name the key that does what the button does, in the IDE's grey after the text: "Read Aloud", "Pause Reading"
and "Resume Reading" on the tab that of "Read Aloud, Pause or Resume", and "Stop Reading" that of "Stop Reading". On the
tab of another Markdown file while a file is read, "Read Aloud" names no key, since there the button reads its own file
and the key pauses the other. In the tool window, "Pause" and "Resume" have the tooltips "Pause Reading" and "Resume
Reading" with the key of "Read Aloud, Pause or Resume", "Stop" has "Stop Reading" with its key, and "Slower" and
"Faster" name those of "Read Slower" and "Read Faster". The keys are changed or removed under Settings › Keymap, where
the actions are found by their names, and a tooltip then names the first key the action has in the keymap in use, or
none. The actions can also be run from Find Action. A key that the desktop or another application takes for itself, or
that a component such as the terminal takes before the IDE's keymap, does not reach the plugin.

Speech works on Linux x64 and on macOS on Apple Silicon, and needs the model described under "The speech model"
below. A press shows a message or a question in place of the reading in two cases: when the model in use is not in
its folder, where it asks whether to download that model, and when the platform is another one. On another platform the
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

The plugin speaks with a Kokoro v1.0 model package of sherpa-onnx, which is neither in the plugin archive nor in the
repository. It knows two packages, which have the same voices and differ in how the network is stored:

| Model on the page | Package | Download | On disk | Model file |
|---|---|---|---|---|
| Kokoro 1.0, int8 | `kokoro-int8-multi-lang-v1_0` | 132 MB | 190 MB | `model.int8.onnx` |
| Kokoro 1.0, fp32 | `kokoro-multi-lang-v1_0` | 350 MB | 401 MB | `model.onnx` |

The model in use is the same for the whole IDE and is kept when the IDE is closed. Before a model is chosen, it is the
fp32 model, on Linux x64 and on macOS on Apple Silicon alike, because the fp32 model makes its audio much faster than
the int8 model on both. On a Linux x64 machine with an AMD Ryzen 9 9950X3D, with the 6 threads the plugin uses there,
the fp32 model made 11.5 seconds of audio per second of work and its first audio after 0.3 seconds, and the int8 model
2.8 seconds and 1.3 seconds; the fp32 model also loads in half the time. It is the larger download, 350 MB against
132 MB, once. The int8 model also loses pieces of a reading on macOS on Apple Silicon, which the plugin plays as
silence: of the 136 pieces of `samples/markdown-features.md`, with 6 threads, it lost 1 at the normal speed, 30 at 1.5
and 56 at 2.0. The fp32 model lost none there at any speed, and the int8 model lost none on Linux x64. The measurement
of the loss is in unit 0016 under `changes/`.
The default is not stored as a choice, so a version of the plugin with another default moves the listeners who never
chose, and a listener who chose keeps the choice.

The models are chosen, downloaded and removed on the page "AloudRite" under Tools in the settings, under "Speech
model". The page lists both models with the size to download, the space they take and whether they are on the machine,
and marks the model in use:

* A model that is not on the machine has a button "Download". It asks first, and states the size of the download, the
  disk space the model takes, where it is downloaded from and the folder it goes to
* Choosing a model that is not on the machine and applying the page asks whether to download it. The model is in use
  once it is in place, and the model before stays in use until then. Declining leaves the model in use as it was
* A model on the machine that is not in use has a button "Remove", which asks first and names the space it frees. The
  model in use, and the model of a reading that goes on, cannot be removed
* A choice made during a reading takes effect at the next reading, and the reading goes on with the model it began
  with. The plugin keeps one engine in memory, that of the model in use, and releases the engine of another model
  once no reading uses it

A press on "Read Aloud" when the model in use is not in place asks whether to download that model:

* It asks first, and states the size of the download, the disk space the model takes, where it is downloaded from and
  the folder it goes to. Nothing is downloaded before you agree, and when you decline, the question is asked again at
  the next press
* It downloads the package from the address below over HTTPS, using the proxy settings of the IDE, checks it against
  the SHA-256 below, and unpacks it into the model folder. About 322 MB for the int8 model
  and 751 MB for the fp32 model have to be free for a moment
* The download runs in the background, so the IDE stays usable, and its progress shows in the status bar, where it can
  be cancelled at any moment. When the model is in place, the file the button was pressed on is read by itself, with
  the text it had at the press, unless its tab or its project was closed meanwhile
* The model folder comes into being by one rename of a complete, unpacked and flushed directory. Nothing of a failed
  or cancelled download is left on disk, except one empty lock file, `.kokoro-int8-multi-lang-v1_0.install.lock`, in
  the folder that holds the model folders, which is kept also after a success. It has the name from when the int8
  model was the only one, and is one lock for both models. What an IDE that was killed in the middle left behind is
  removed at the next download. Every file and directory is flushed to the disk before that rename, which is the most
  Java offers against a power loss, and a power loss was not tried. One limit is known: on macOS the drive may hold the
  last writes in its own cache, which the plugin cannot make it empty
* One download runs at a time, across both models and across IDE windows: the plugin takes a lock on that file before
  it asks or touches anything. While another IDE window holds it, a press starts no download, asks nothing, and says
  that a download is already running in another IDE window. A removal takes the same lock
* A failure shows a notification that says what went wrong and offers to try again. When the checksum does not match,
  the package at the address is not the one this version of the plugin was built for, and putting a model in place by
  hand, as described below, keeps working
* A model folder that exists, is not empty and does not hold a complete model is not touched, since the plugin did
  not make it. The download then fails before any request, and the folder has to be removed by hand. The page does not
  remove such a folder either
* A model that is in place already, downloaded before or put there by hand, is used as it is: no question is asked
  and no request is made

The packages:

* int8: `kokoro-int8-multi-lang-v1_0.tar.bz2`, 132 MB packed and 190 MB unpacked
    * Address: https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2
    * SHA-256: `4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e`
* fp32: `kokoro-multi-lang-v1_0.tar.bz2`, 350 MB packed and 401 MB unpacked (the folder takes 384 MB on the disk by
  `du`)
    * Address: https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2
    * SHA-256: `c5f7e2d2caf082bc1d20fb70334a61d99d20b484500aad32e7cf84c128ea3298`

The publisher has replaced the package at an address before, so a different checksum means a different package and not
necessarily a damaged one. The download then fails until a new version of the plugin, built for the new package, is
published.

The models are unpacked into one folder per user, outside the directories of any IDE, so that the sandbox IDE, an
installed IDE and the tests all use the same copy. Each package unpacks to a directory of its own, so the models lie
side by side in:

* Linux: `$XDG_DATA_HOME/aloudrite`, where `XDG_DATA_HOME` is set to an absolute path, and `~/.local/share/aloudrite`
  otherwise
* macOS: `~/Library/Application Support/AloudRite`

To remove a model by hand, delete its folder, as `kokoro-int8-multi-lang-v1_0` or `kokoro-multi-lang-v1_0` in that
folder:

```sh
rm -rf ~/.local/share/aloudrite/kokoro-multi-lang-v1_0                          # Linux
rm -rf ~/Library/Application\ Support/AloudRite/kokoro-multi-lang-v1_0         # macOS
```

Putting a model in place by hand is an alternative to the download, for a machine without network access or for when
the download fails. On Linux, with `XDG_DATA_HOME` not set, for the int8 model:

```sh
mkdir -p ~/.local/share/aloudrite
cd "$(mktemp -d)"
curl -fLO https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2
echo "4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e  kokoro-int8-multi-lang-v1_0.tar.bz2" | sha256sum -c -
tar -xjf kokoro-int8-multi-lang-v1_0.tar.bz2 -C ~/.local/share/aloudrite
```

and for the fp32 model:

```sh
mkdir -p ~/.local/share/aloudrite
cd "$(mktemp -d)"
curl -fLO https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2
echo "c5f7e2d2caf082bc1d20fb70334a61d99d20b484500aad32e7cf84c128ea3298  kokoro-multi-lang-v1_0.tar.bz2" | sha256sum -c -
tar -xjf kokoro-multi-lang-v1_0.tar.bz2 -C ~/.local/share/aloudrite
```

On macOS, for the int8 model:

```sh
mkdir -p ~/Library/Application\ Support/AloudRite
cd "$(mktemp -d)"
curl -fLO https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2
echo "4c3052abaa60943a341f193888cf6abd68787dae6ab8ae5c925a706caa247e4e  kokoro-int8-multi-lang-v1_0.tar.bz2" | shasum -a 256 -c -
tar -xjf kokoro-int8-multi-lang-v1_0.tar.bz2 -C ~/Library/Application\ Support/AloudRite
```

and for the fp32 model:

```sh
mkdir -p ~/Library/Application\ Support/AloudRite
cd "$(mktemp -d)"
curl -fLO https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2
echo "c5f7e2d2caf082bc1d20fb70334a61d99d20b484500aad32e7cf84c128ea3298  kokoro-multi-lang-v1_0.tar.bz2" | shasum -a 256 -c -
tar -xjf kokoro-multi-lang-v1_0.tar.bz2 -C ~/Library/Application\ Support/AloudRite
```

The plugin counts a model as in place when its folder holds the model file of the table, `voices.bin`, `tokens.txt`,
`lexicon-us-en.txt` and the directory `espeak-ng-data`. It looks at nothing more, so a folder that holds those but
is damaged or from another package is not noticed until the engine fails.

The automated tests that run the real engine, without playing anything, run only where the int8 model is in its folder.
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

The native code of sherpa-onnx writes a line of debug output per piece of a reading to the error output. The sandbox
IDE is started with the system property `aloudrite.engine.debug` set to `true` by `tasks.runIde`, so these lines show in
the output of `./gradlew runIde`, where they help to see what the engine did with each piece. An installed plugin is
not started with it, and writes none.

The folder `samples/` holds Markdown files to open there and listen to. `markdown-features.md` first holds every kind of
Markdown the plugin may meet, each in a short section under a heading that names it, from headings and lists to links,
code, a table, emoji and HTML comments, with numbered and bulleted lists both with and without full stops, and then two
long chapters of plain prose, to listen to a long reading and to stop one. It begins with front matter, which is not to
be heard.
`one-sentence.md` and `ten-thousand-words.md`, which starts with the same sentence, are for comparing the time to the
first sound, which the IDE's log gives for each press, and also after each change of the speed during a reading.
`empty.md` shows the notification that there is nothing to read.

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

`EngineSilenceMeasurement` runs only with `ALOUDRITE_MEASURE_ENGINE_SILENCE` set and the model in its folder, and is
reported as skipped otherwise. It makes every piece of `samples/markdown-features.md` with the real engine at 1.0×, 1.5×
and 2.0×, with the threads the plugin uses and with one thread, and writes a table to `build/engine-silence.txt` of the
pieces that came out as silence or as samples that are not a number, and whether a second try of such a piece gave
sound. It takes several minutes, most of them with one thread. On macOS on Apple Silicon it found such pieces, where on
Linux x64 it found none:

```sh
ALOUDRITE_MEASURE_ENGINE_SILENCE=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.EngineSilenceMeasurement
```

With `ALOUDRITE_MEASURE_MODEL_FOLDER` and `ALOUDRITE_MEASURE_MODEL_FILE` it measures another model in place of the
plugin's own, such as the fp32 `model.onnx` of `kokoro-multi-lang-v1_0`. Without them it measures the int8 model.

`ReadingTimeMeasurement` runs only with `ALOUDRITE_MEASURE_READING_TIME` set, on a platform with speech, and with both
models in place, and is reported as skipped otherwise. For `samples/markdown-features.md` and
`samples/ten-thousand-words.md`, each model and 1.0×, 1.5× and 2.0×, it makes the audio with the real engine and writes
to `build/reading-time.txt` the length of the whole audio and the estimate of the whole reading that the tool window
would show after the first piece and when a quarter, a half and three quarters of the audio have played, each with its
error in seconds and in percent of the length of the audio. The six setups run side by side, each with its own engine,
and it took 35 minutes on a machine with 6 processors:

```sh
ALOUDRITE_MEASURE_READING_TIME=1 ./gradlew cleanTest test --tests com.ritenoar.aloudrite.ReadingTimeMeasurement
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

The sandbox of AgentRite, started with `./sandbox.sh`, holds JDK 25, Amazon Corretto, so `./gradlew buildPlugin` and
`./gradlew test` run there as they do on the host. `./gradlew runIde` does not, as it opens a window and the sandbox
has no display.
