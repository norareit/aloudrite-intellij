# Model download on first use

## Context

Unit 0004 makes the plugin speak, with a Kokoro model that the developer puts into a folder by hand. A user who
installs the plugin will not do that. The model is too large to ship inside the plugin: the package unit 0004
chose is 132 MB, where the plugin itself is to stay under 30 MB. So the plugin has to fetch the model itself, the
first time it is needed.

This spec was written together with the specs of units 0004 and 0006, on 2026-10-08, before unit 0004 was built.
It rests on what was known then: which model package is used, that it lives in one folder per user, and that
speech is asked for through an action. Unit 0004 ends by revising this spec against what it found, so what is
written here is the intent and may change in its details.

## Requirements

1. When speech is asked for and the model is not in its folder, the plugin asks the user whether to download it.
   The question states the size of the download and where it comes from.
2. Nothing is downloaded before the user has agreed. When the user declines, nothing is downloaded, nothing is
   spoken, and the question is asked again the next time speech is asked for.
3. The download runs in the background. The IDE stays usable, and a progress indication shows how far the
   download is.
4. The user can cancel the download from the progress indication.
5. When the download has finished, the speech that was asked for starts without the user asking again.
6. The downloaded model is checked against a checksum that is fixed in the plugin. A download that does not match
   is discarded and reported as a failure.
7. A download that fails, is cancelled or does not match its checksum leaves nothing in the model folder that a
   later attempt would take for a complete model.
8. A failure is reported in a notification that says what went wrong in words a user understands, at the least
   telling apart no connection, an interrupted download, a full disk and a checksum that does not match, and that
   offers to try again.
9. Asking for speech while a download is running does not start a second download.
10. The model is stored in the folder of unit 0004. A model that is already there, put by hand or downloaded
    before, is used as it is: no question is asked and no network request is made.
11. The download is the only network request the plugin makes. It goes over HTTPS to one address that is fixed in
    the plugin, and it uses the proxy settings of the IDE.
12. Automated tests cover a download that succeeds, one that is cancelled, one that is interrupted, one whose
    checksum does not match, a declined question, and a model that is already present. They run against a
    stand-in for the server with a small stand-in for the model, and need no network.
13. `README.md` describes the download: when it happens, how large it is, where the model is stored and how to
    remove it. Putting the model in place by hand is described as an alternative and no longer as a step.
14. Before this unit is set to `implemented`, the spec of unit 0006 has been revised by the specifier against what
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
- **The model comes from the place sherpa-onnx publishes it.** It is the package unit 0004 names, and fetching it
  from its publisher means the plugin distributes no model itself. Hosting a copy was rejected for now: it costs a
  place to host 132 MB and a duty to keep it there. The risk is that the address changes, which the checksum turns
  into a clear failure and not into a wrong model. See "Beyond the request".
- **A fixed checksum.** The plugin runs what it downloads through a native library, so it has to know that it got
  the file it was built for. A checksum in the plugin ties each version of the plugin to one exact model.
- **Failures leave nothing behind.** A half-written model that is later loaded as if it were whole fails in the
  engine, far from its cause. Requirement 7 makes every failure end in the same state as before the attempt.
- **Speech follows the download on its own.** The user pressed a button to hear something. Making them press it
  again after the download is one step more for nothing.
- **Putting the model in place by hand keeps working.** It is how unit 0004 was built and checked, it serves a
  machine without network access, and it costs nothing to keep: the plugin only looks whether the model is there.

## Non-goals

- A choice between models, or between voices.
- Updating a model that is already there, and noticing that a newer one exists.
- Continuing an interrupted download where it stopped.
- A settings page, and a way to remove the model from inside the IDE.
- Downloading anything else than the model, and the native libraries in particular: they stay in the plugin.
- Anonymous statistics or any other network request.

## Beyond the request

- **A second address to fall back on.** If sherpa-onnx moves or removes the package, every new installation fails
  to download until a new version of the plugin is published. A mirror costs hosting 132 MB and logic to try it.
  Without it, the failure is a clear notification, and a user can still put the model in place by hand.
- **Continuing an interrupted download.** On a poor connection a download of 132 MB may fail more than once, and
  each new attempt starts from the beginning. Continuing costs range requests, a check that the partial file
  still belongs to the same package, and tests for both. Without it, a retry costs the user the time of a full
  download.
- **Removing the model from inside the IDE.** A user who uninstalls the plugin keeps 132 MB or more on disk
  without knowing. An action that removes it costs a place in the interface and a confirmation. Without it, the
  README says where the folder is.
