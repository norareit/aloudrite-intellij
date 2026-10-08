# Model download on first use

## Context

Unit 0004 makes the "Read Aloud" button speak, with a Kokoro model that the developer puts into a folder by hand,
following commands in the README. A user who installs the plugin will not do that. The model is too large to ship
inside the plugin: the plugin archive is 20 MB and is to stay under 30 MB, and the model package is 132 MB. So the
plugin has to fetch the model itself, the first time it is needed.

This spec was first written on 2026-10-08, together with the specs of units 0004 and 0006 and before unit 0004 was
built. It was revised on 2026-10-09 against what unit 0004 established, which is this:

- The model is the package `kokoro-int8-multi-lang-v1_0.tar.bz2` of sherpa-onnx, 132,303,094 bytes, published as
  a release asset on GitHub. It is a packed archive and not a model file: it unpacks to one directory of 190 MB.
- The README of unit 0004 holds its address and its SHA-256 checksum, and notes that the publisher has replaced
  the package at that address before.
- The model folder is `~/Library/Application Support/AloudRite/kokoro-int8-multi-lang-v1_0` on macOS, and on
  Linux `$XDG_DATA_HOME/aloudrite/kokoro-int8-multi-lang-v1_0`, or under `~/.local/share` where that variable is
  not set.
- The plugin counts the model as in place when that folder holds five entries: four files and one directory. It
  looks at nothing more.
- A model folder that is not what the engine expects is not always an error that the plugin can catch. With a
  missing or a cut-off model file the engine throws, and the plugin reports it. With the lexicon missing from what
  the engine is given, the native code ended the whole process, which in an IDE means the IDE closes without a
  word. Unit 0004 guards against that case. What other damage to a model folder has that effect is not known.
- When the model is not in place, pressing the button shows a message that names the folder and points to the
  README. On a platform that is not supported, it shows another message.
- The plugin makes no network request at all.

## Requirements

1. On a supported platform, when the "Read Aloud" button is pressed and the model is not in place, the plugin asks
   the user whether to download it. This question takes the place of the message of unit 0004 that the model is
   missing.
2. The question states the size of the download, the disk space the model takes once it is in place, where it is
   downloaded from and the folder it goes to.
3. Nothing is downloaded before the user has agreed. When the user declines, nothing is downloaded, nothing is
   spoken, and the question is asked again the next time the button is pressed.
4. On a platform that unit 0004 does not support, no question is asked and nothing is downloaded. The message of
   unit 0004 for that case stays.
5. The download and the unpacking run in the background. The IDE stays usable, and a progress indication shows
   how far the work is.
6. The user can cancel from the progress indication, at any moment until the model is in place.
7. The downloaded package is checked against a SHA-256 checksum that is fixed in the plugin, before anything of it
   is unpacked. A package that does not match is discarded and reported as a failure.
8. The package is unpacked by the plugin, and what ends up in the model folder is what the commands of the README
   put there, so that a model downloaded by the plugin and one put in place by hand are the same on disk.
9. The model folder holds a complete model or it does not count as in place. At no moment does the plugin count a
   model as in place that it downloaded and that is not complete, and that holds when the download fails, when
   the unpacking fails, when the user cancels, and when the IDE is killed or the machine loses power in the middle
   of either.
10. After a failure or a cancellation, nothing of the attempt is left on disk. What an attempt left behind that
    was cut off by the IDE being killed is removed at the next attempt.
11. A failure is reported in a notification that says what went wrong in words a user understands, and that
    offers to try again. It tells apart, at the least: no connection, a download that broke off, not enough disk
    space, a package that could not be unpacked, and a checksum that does not match. For the last it says that
    the package at the address is not the one this version of the plugin was built for, and points to the
    README.
12. When the model is in place after a download, the speech that was asked for starts without the user pressing
    again.
13. Pressing the button while a download or the unpacking is running starts no second download and asks no
    second question.
14. A model that is already in place, put there by hand or downloaded before, is used as it is: no question is
    asked and no network request is made.
15. The download is the only network request the plugin makes, and it is made only after the user agreed. It
    starts at one HTTPS address that is fixed in the plugin, follows redirects only to HTTPS addresses, and uses
    the proxy settings of the IDE. This takes the place of requirement 10 of unit 0004, which says that the
    plugin makes no network request.
16. Automated tests cover: a download that succeeds and is followed by speech, a declined question, a
    cancellation during the download and one during the unpacking, a download that breaks off, a checksum that
    does not match, a package that cannot be unpacked, a model that is already in place, a press while a download
    is running, and a platform that is not supported. For each case that ends without a model, they assert that
    the plugin does not count the model as in place. They run against a stand-in for the server with a small
    stand-in for the package, need no network, and hand nothing to the real engine.
17. The developer has, on Linux x64 and on macOS on Apple Silicon, with the model folder removed: pressed the
    button, declined, and seen that nothing was downloaded; pressed again, agreed, cancelled halfway, and seen
    that no model folder was left; and pressed again, agreed, waited, and heard the speech start by itself. These
    are the human's checks, made at the review.
18. `README.md` describes the download: when it happens, how large it is, where the model is stored and how to
    remove it. Putting the model in place by hand is described as an alternative and no longer as a step, and the
    README no longer says that the plugin makes no network request.
19. Before this unit is set to `implemented`, the spec of unit 0006 has been revised by the specifier against what
    this unit established, where unit 0006 is not implemented yet, and the human has seen the revision. Where it
    needed no change, or unit 0006 was implemented already, the record of this unit says so.

## Decisions

- **The model is downloaded on first use and not shipped in the plugin.** The developer asked for a small plugin.
  With the model inside, the archive would be about 150 MB and every update of the plugin would carry the model
  again.
- **The user is asked first.** A download of 132 MB is a cost in time and, for some, in money, and a plugin that
  starts one unasked at the press of a button is a surprise. Asking once costs one click. Starting the download
  without asking was rejected for that reason, and downloading when the plugin is installed was rejected because
  it spends the bandwidth of users who never press the button.
- **The question replaces the message that the model is missing.** That message sends the user to the README to
  run commands by hand. Once the plugin can fetch the model, an offer to do so is the better answer to the same
  situation.
- **The plugin unpacks the package itself.** What the publisher offers is a packed archive, so downloading alone
  does not give a model. Asking the user to unpack it was rejected because it is the manual step this unit exists
  to remove.
- **A complete model or none.** Unit 0004 found that a model folder the engine does not expect can close the IDE
  without a word, and it is not known which kinds of damage do that. A download that is cut off is the most
  likely way for a damaged folder to come into being. Requirement 9 therefore asks for more than cleaning up
  after a failure that the plugin sees: the state on disk has to be safe also when the plugin never gets the
  chance to clean up. How that is done is for the plan.
- **A fixed checksum, checked before unpacking.** The plugin hands what it downloads to native code, so it has to
  know that it got the package it was built for. The checksum ties each version of the plugin to one exact
  package. It is checked first because unpacking an archive that is not the expected one is itself a risk.
- **The model comes from the place sherpa-onnx publishes it.** It is the package unit 0004 names, and fetching it
  from its publisher means the plugin distributes no model itself. Unit 0004 found that the publisher has
  replaced the package at that address before. When that happens again, the checksum no longer matches and every
  new installation fails to download until a new version of the plugin is published. That is a clear failure and
  not a wrong model, and putting the model in place by hand keeps working with whatever package the user finds.
  Hosting a copy of our own avoids it and was not chosen here, because it is the human's to decide: see "Beyond
  the request".
- **Speech follows the download on its own.** The user pressed a button to hear something. Making them press it
  again after the download is one step more for nothing.
- **Putting the model in place by hand keeps working, and is not checked more than before.** It is how unit 0004
  was built and checked, it serves a machine without network access, and it costs nothing to keep: the plugin
  looks whether the five entries are there, as in unit 0004. A stricter check of a hand-placed model is under
  "Beyond the request".
- **The developer checks the download on both platforms.** The automated tests use a stand-in for the server and
  for the package. That the real package arrives from the real address, unpacks on a Mac as on Linux and gives a
  model that speaks is only shown by doing it.

## Non-goals

- A choice between models, or between voices.
- Updating a model that is already there, and noticing that a newer one exists.
- Continuing an interrupted download where it stopped.
- A settings page, and a way to remove the model from inside the IDE.
- Downloading anything else than the model, and the native libraries in particular: they stay in the plugin.
- Anonymous statistics or any other network request.
- Removing what the binding of sherpa-onnx leaves in the temporary folder of the machine. Unit 0004 found that it
  copies its native libraries there at each start. That has nothing to do with the download.

## Beyond the request

- **A copy of the package that the project hosts itself.** The publisher has replaced the package before, and
  each time it does, every new installation fails to download until a new version of the plugin is out. A copy
  as a release asset of the project's own GitHub repository would stay as it is. It costs keeping 132 MB there,
  and it makes the project a distributor of the model and of the espeak-ng data inside the package, which the
  license notices then have to cover. Without it, a replaced package gives the clear notification of requirement
  11, and a user can still put a model in place by hand.
- **A stricter check of a model that was put in place by hand.** The plugin takes any folder with the five
  entries for a model, and unit 0004 found that a folder the engine does not expect can close the IDE. A check
  of the sizes or checksums of the files costs a list of them in the plugin, a message of its own, and time at
  the first press if checksums are used. Without it, a damaged hand-placed model fails at the press, in the worst
  case by closing the IDE, on the machine of someone who chose to place the files themselves.
- **A check for free disk space before the download starts.** Package and unpacked model need about 325 MB
  together for a moment. A check costs a way to ask the file system and a message. Without it, a full disk shows
  as the failure of requirement 11 after part of the download was made.
- **Continuing an interrupted download.** On a poor connection a download of 132 MB may fail more than once, and
  each new attempt starts from the beginning. Continuing costs range requests, a check that the partial file
  still belongs to the same package, and tests for both. Without it, a retry costs the user the time of a full
  download.
- **Removing the model from inside the IDE.** A user who uninstalls the plugin keeps 190 MB on disk without
  knowing. An action that removes it costs a place in the interface and a confirmation. Without it, the README
  says where the folder is.
