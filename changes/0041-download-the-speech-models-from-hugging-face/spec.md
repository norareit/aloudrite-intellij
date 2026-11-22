# Download the speech models from Hugging Face

## Context

Since unit 0005, the plugin downloads a speech model as one archive from the GitHub release `tts-models` of
sherpa-onnx, and accepts it only when its SHA-256 is the one the plugin was built for. Unit 0005 knew that the
publisher had replaced the archive at that address before, and accepted that each replacement makes every new
installation fail to download until a new version of the plugin is published.

That happened on 2026-10-26. The human's pull request 4087 of sherpa-onnx, which fixes the all-NaN audio of the int8
model on macOS on Apple Silicon (issue 4086), was merged that day, and the maintainer exported both Kokoro 1.0
packages again and uploaded them under the same names. A GitHub release keeps no earlier version of an asset. The
specifier downloaded both packages on 2026-11-22 and compared them, file by file, with the models installed on the
developer's machine:

- `kokoro-int8-multi-lang-v1_0.tar.bz2` is now 132,353,410 bytes with SHA-256 `72c34723…`, where the plugin pins
  132,303,094 bytes and `4c3052ab…`. Its `model.int8.onnx` is now `75635ea3…`, the fixed model that pull request 4087
  names, and every other file is unchanged
- `kokoro-multi-lang-v1_0.tar.bz2` is now 349,886,476 bytes with SHA-256 `60703707…`, where the plugin pins
  349,906,910 bytes and `c5f7e2d2…`. Every file in it is unchanged: only the archive was made again

So neither model can be downloaded by a new installation of the plugin today, and the developer did not notice it
because both models were already in place on their machines.

The maintainer also publishes each model as a repository on Hugging Face, `csukuangfj/kokoro-int8-multi-lang-v1_0` and
`csukuangfj/kokoro-multi-lang-v1_0`, with the history of its commits. A file fetched by the address of a commit,
`https://huggingface.co/<repository>/resolve/<commit>/<path>`, does not change when the repository does. Both versions
of the int8 model are still served that way: the old one at commit `2a36069` and the fixed one at commit `57adb37`.

The specifier tried the download on 2026-11-22 from the developer's Linux host:

- The int8 repository at commit `57adb37` holds the 377 files of the archive, and `.gitattributes`. All 377, fetched
  one by one at that commit, are byte for byte the files of the current archive, 189,881,734 bytes together
- Fetching them eight at a time took 20 seconds
- None was refused. Every answer carried the policy `"resolvers";q=3000;w=300`: an anonymous client may make 3,000
  requests for files per 5 minutes, counted per address. Every answer also carried
  `ratelimit: "resolvers";r=<remaining>;t=<seconds>`, the requests left in the window and the seconds until it starts
  again. The requests left fell by about 450 over the 377 files, so a model costs about 450 of them, and one address
  can download a model about six times in 5 minutes. Many machines behind one address, as behind the gateway of a
  company, share that
- Listing the files of a repository goes to a separate bucket, `"api";q=500;w=300`, which the plugin does not need
  when it carries the list itself
- One file has a space in its name, `espeak-ng-data/voices/!v/Mr serious`, and is only found with its name encoded in
  the address
- The fp32 repository at its latest commit, `f7b96bb` of 2026-09-08, serves the `model.onnx` installed on the
  developer's machine, `b40f62b1…`

The aligner model `english-mfa-v3.1.0` of unit 0028 is not on Hugging Face. It stays on the GitHub release `fa-models`
of sherpa-onnx, where it is today the archive the plugin pins.

The human asked for the speech models to be downloaded from Hugging Face, from commits fixed in the plugin, and for the
int8 model to be the fixed one.

## Requirements

1. Each speech model is downloaded from its repository on Hugging Face, at a commit fixed in the plugin, one file at a
   time: `kokoro-int8-multi-lang-v1_0` from `csukuangfj/kokoro-int8-multi-lang-v1_0` at commit
   `57adb37fc0e8d4bd430cfc1b8f8c2294945b9af2`, and `kokoro-multi-lang-v1_0` from `csukuangfj/kokoro-multi-lang-v1_0`
   at commit `f7b96bb6bef5c5da4d3aa4f4e0498fbbf62dc78b`.
2. The plugin carries, for each model, its repository, its commit and the list of the files it downloads, with the
   size and the SHA-256 of each: the 377 files the archive of that model holds, which are the files of the repository
   at that commit without `.gitattributes`. They are in one resource file of the plugin, which the download and the
   check of requirement 15 both read, so that the two cannot disagree. A model put in place by a download holds those files and nothing else, in the same folder as
   today, so the engine finds it as it does now.
3. Every file is checked against its size and SHA-256 before the model is put in place. A file that does not match
   fails the download with the message the plugin gives today for a package that is not the one it was built for,
   naming the address of that file. Nothing of a failed download is put in place, and the model in use stays in use,
   as today.
4. The address of each file has each segment of its path encoded, so that a file whose name holds a space or another
   character that an address does not allow as it is, such as `espeak-ng-data/voices/!v/Mr serious`, is found.
5. The download makes no request to the API of Hugging Face, such as a listing of the files. It requests the files of
   requirement 2 and nothing else.
6. At most eight files are downloaded at the same time.
7. An answer that the server limits the requests, HTTP status 429, pauses the whole download:
   1. No new file is requested until the pause ends. Files already received in the attempt are kept and are not
      requested again
   2. The pause lasts the seconds of the answer's `Retry-After` header, or else the `t` of its `ratelimit` header, or
      else 60 seconds, and never more than 5 minutes
   3. During the pause the progress shows that Hugging Face limits the downloads for the moment and the time left
      until the download continues, counting down, and cancelling it ends the download as at any other moment
   4. After the pause the download continues with the files still missing, the refused one among them
   5. An answer of 429 to the first request after a pause fails the download with a message that says that Hugging
      Face limits the downloads from this address for the moment and that "Try Again" can be pressed later
8. The progress of a download is the bytes received of the sum of the sizes in the list, as one bar for the model, and
   cancelling it stops every file still being fetched.
9. The size of a download, which the settings page and the question before a download state, is the sum of the sizes
   in the list, since the files arrive unpacked: 189,881,734 bytes for the int8 model and 401,239,297 bytes for the
   fp32 model. The space on disk is the same number.
10. A model that is already in place, downloaded from the archive or put there by hand, is used as it is, and is not
    downloaded again.
11. The aligner model is downloaded as today, from the GitHub release `fa-models` of sherpa-onnx, checked against the
    SHA-256 the plugin pins, together with a speech model where it is not yet on the machine.
12. Automated tests, with a stand-in for the server, cover: a download of a model of several files, among them one
    whose name holds a space, into its folder; a file whose size or SHA-256 does not match, after which nothing is in
    place; an answer of 429 with `Retry-After`, with only `ratelimit` and with neither, each pausing for the time
    requirement 7 gives and capped at 5 minutes; that no file is requested during the pause and that the files
    received before it are not requested again; the countdown in the progress; the download completing after the
    pause; an answer of 429 to the first request after a pause, and its message; cancelling during a pause; that no
    more than eight requests are open at once; and the cancelling of a download.
13. `README.md`, under "The speech model", names the repositories and commits of the two models, their sizes, and
    that the int8 model is the one with the fix of issue 4086. Its instructions for putting a model in place by hand
    give a way that yields the files of requirement 2. `THIRD-PARTY.md` names the repositories on Hugging Face at
    those commits as where the models come from.
14. The developer downloads each model through the plugin, on Linux x64 and on macOS on Apple Silicon, from a machine
    on which it is not in place, and hears it read. The result is recorded in `record.md`.
15. A Gradle task, `checkModelSources`, reads the resource file of requirement 2 and checks, for each model, that
    Hugging Face still serves its commit: that the commit exists in its repository, and that the headers of its
    largest file at that commit give the size and the SHA-256 of the list. It makes a few requests per model and not
    one per file. It fails when a check fails, naming the model, the address and what did not match, and succeeds
    otherwise. It is not part of `./gradlew test` or `./gradlew build`, so that neither needs the network. Its
    comparison is covered by automated tests with stand-in answers: a commit that is missing, a size and a SHA-256
    that do not match, and answers that match.
16. A workflow of GitHub Actions runs `checkModelSources` once a week and when started by hand, with a JDK 25 of
    Temurin as the workflow of unit 0023 has, and with the permission to read the contents of the repository alone.
17. `README.md` says how `checkModelSources` is run, that the workflow runs it every week, that it is run before a
    release is published, and that GitHub turns off a scheduled workflow of a public repository after 60 days without
    activity in it, which the Actions tab of the repository turns on again.

## Decisions

- **Hugging Face at a fixed commit, rather than the GitHub release with a new checksum or a copy of our own.** A new
  checksum mends the download until the next time the maintainer uploads again, which is what broke it now. A copy on
  the project's own releases would not change, but it would make the project a distributor of the packages, among
  them lexicons whose license is an open question in issue 4230 of sherpa-onnx. A file at a commit on Hugging Face
  does not change and is distributed by the maintainer. It stops working only if the maintainer deletes the repository
  or rewrites its history, which is the risk that remains.
- **The list of files is carried by the plugin, not asked of Hugging Face.** It makes the plugin check every file
  against what it was built for, as the checksum of the archive did, and it keeps the download out of the bucket of
  the API, whose limit of 500 requests per 5 minutes is the tighter one.
- **Eight files at a time.** The trial fetched the int8 model in 20 seconds that way. A model costs about 450 of the
  3,000 requests an anonymous address may make per 5 minutes, so both models fit within one window several times
  over.
- **A 429 pauses the whole download until the window starts again, rather than retrying the one file a few times.**
  The server says when its window starts again, and a window lasts up to 5 minutes, so retries of 30 seconds each
  could give up before it does and show an error that waiting would have avoided. Pausing every file, not only the
  refused one, keeps the other seven from spending the window's first requests on refusals. A refusal right after a
  full wait means the limit is not this machine's alone, as when many machines share one address, and a clear message
  with "Try Again" is then better than a pause that does not end. The countdown tells the user that the download is
  waiting and not stuck.
- **The fixed int8 model.** It is the model of the human's own fix, and it is what the archive of sherpa-onnx holds
  today. The old one is still on Hugging Face, at commit `2a36069`, should it be needed for a comparison.
- **The aligner model stays on the GitHub release.** It is not on Hugging Face. The human will ask the maintainer to
  publish it there, and moving it is a unit of its own once that is done. A copy of our own was not chosen, because it
  would be replaced again when it reaches Hugging Face. A failure of its download leaves the speech model in place and
  reading, so its remaining risk is the highlight of the word.
- **The check of the sources asks whether the commits exist, and does not download the models.** A commit of Git
  names its content: while the commit exists, its files are the ones it held when the list was made. What can break
  is that the repository is deleted or its history rewritten, and a few requests per model show that. A download of
  every file would show nothing more, and, made from a runner of GitHub, whose addresses many users share, it could
  meet the limit of Hugging Face.
- **The check runs every week on GitHub, and by hand before a release, not in the tests.** What it watches changes
  outside the repository, without a push, so a check that runs only with a build would not see it when it happens,
  and the tests would fail for a reason that is not in the code whenever Hugging Face cannot be reached. A week finds
  a deleted repository within days, mostly before a user does, and GitHub mails the developer when a scheduled run
  fails. The repository is public, so the runs cost nothing. The repository is also pushed to Bitbucket, where the
  workflow does nothing.
- **The download grows.** The files arrive unpacked, so the int8 model is 190 MB to download where its archive was
  132 MB, and the fp32 model 401 MB where it was 350 MB. The space on disk is unchanged. It is the cost of an address
  that does not change.

## Non-goals

- Moving the aligner model to Hugging Face
- Replacing a model that is already in place, such as the old int8 model on the developer's machines, which the
  developer replaces by hand
- Continuing an interrupted download where it stopped, or keeping the files of a failed attempt for the next one
- Signing in to Hugging Face or using a token of it
- Measuring again how the fixed int8 model behaves on macOS on Apple Silicon and updating what `README.md` says about
  it. That is a unit of its own, after this one, once the fixed model can be downloaded
- Other models than the two Kokoro 1.0 models

## Beyond the request

1. Notice an int8 model in place that is the old one, by the SHA-256 of its `model.int8.onnx`, and offer the fixed one.
   It costs a checksum of 114 MB when the model is first used in a session, a question of its own, and its tests.
   Without it, a machine that downloaded the old model keeps it until the user removes it and downloads it again. As
   the plugin is not yet published, only the developer's machines have it.
2. A check, run by hand or by a workflow, that the two commits on Hugging Face still serve the files of the list.
   Taken into the unit by the human, as requirements 15 to 17.
