# Measure how well the word highlight follows the voice

## Context

Unit 0025 highlights the word being heard. The engine gives the times of whole pieces only, so the plugin estimates
the word from the share of the piece heard, weighing each word by its letters. Unit 0028 would replace that estimate
by forced alignment, with a 45 MB aligner model downloaded beside the speech models.

The spec of unit 0028 rests on a trial outside the project, whose script the human had found in the transcript of its
session. The trial ran `model.onnx` of the fp32 package itself with ONNX Runtime, not through sherpa-onnx, on 11
sentences that it made into phonemes one word at a time with the `espeak-ng` command, joined by the token of a space.
It took the true times of the words from the durations that the model gives for its tokens, and compared them with
its own copy of the estimate, the share of the audio of the whole sentence weighed by letters, every 10 ms from the
start of the first word to the end of the last, leaving out the gaps between words. The word shown was the word heard
54% of the time at 1.0× and 52% at 1.5× by the estimate, and 96% and 93% by forced alignment shown 70 ms later. Since
sherpa-onnx played no part in it, those shares do not depend on its release, as the spec of unit 0028 implies. The 11
sentences are:

- The lighthouse stood at the end of a narrow spit of rock, and for most of the year nobody went near it.
- The keeper came twice a month with a boat full of supplies, checked the lamp, and wrote a few lines in a book that
  nobody else would ever read.
- In the spring the birds returned.
- They nested in the cracks of the old stone wall, and their noise filled the mornings until the keeper could hardly
  hear the sea.
- He did not mind.
- He had grown used to the company of things that did not ask questions, and he liked the way the gulls argued over
  the scraps he threw from the landing.
- The tower itself was older than anyone could say for certain.
- Two candles and a box of matches.
- Climb the stairs and unlock the lamp room.
- Call record found boat once for every boat, and never change the date by hand.
- The answer was in the old register, which he had read twice.

Since unit 0030, the plugin reads with sherpa-onnx 2.0.2 and makes the phonemes of each piece itself, for a whole
sentence at a time. The human has the impression, on Linux x64 and on macOS, that the word highlight follows the voice
much better since then, which unit 0030 recorded as not measured. If the estimate is now close to what forced alignment
reached, unit 0028 may not be worth its download and its code. The human wants that measured before unit 0028 is
built, in a way that unit 0028 can measure again after it is built, so that the two can be compared and unit 0028
dropped if it brings too little.

The first version of this spec took the durations from the Kokoro Python package, on the specifier's finding that
`model.onnx` has the audio as its one output. That finding was wrong: the file has a second output,
`onnx::Shape_3411`, the durations of its tokens in frames of 600 samples. Run in the developer's working copy, that
version left out every piece of the sample. The Kokoro Python package gives each piece some 0.6 s more of silence at
its ends than the engine, and eSpeak NG joins short words into one phoneme word, as "of the" in "A heading of the third
level", which that version could not pair with the words of the plugin.

The specifier then ran `model.onnx` with ONNX Runtime for Java 1.22.0 on the tokens that the plugin hands the engine
for "Markdown features", with Heart at 1.0×. The durations added up to the 45,000 samples of its audio exactly. The
engine gave 30,447 samples for the same tokens, and they lie within that audio 6,524 samples, 0.272 s, after its start,
with 0.335 s after their end, where the two differ by 7% of the level of the audio on average.

The measurement built on that version placed the engine's audio of a piece by the one place where the two differ
least, and left out a piece where they differed by more than 20% there. It left out 48 of the 130 pieces of the sample
at 1.0×, most of them long sentences. The specifier found why on "Each kind of Markdown comes first, in a short
section of its own under a heading that names it.":

- Two runs of `model.onnx` on the same tokens give the same durations, but audio that differs by 7% of its level,
  since the decoder of Kokoro adds noise and phases of its own at every run.
- The engine's audio of that piece lines up with the model's 250 ms after its start for its first 1.5 s, and 550 ms
  after it from then on: the engine has 300 ms less in the pause of the comma after "first".
- sherpa-onnx 2.0.2 shortens the silences of the audio it makes, by `GeneratedAudio::ScaleSilence` in
  `offline-tts.cc`, which `offline-tts-kokoro-impl.h` calls on the audio of every run of the model, the run on phonemes
  among them. A run of samples whose magnitude is at most 0.01, at least 0.2 s long, at the start, inside or at the
  end, keeps only its first part, its length times the silence scale, which is 0.2 unless it is set otherwise; the
  plugin does not set it. A shorter run of silence is kept whole.
- The model's audio with that rule applied has 113,667 samples for that piece against the engine's 113,701, and 30,446
  for "Markdown features" against 30,447, and in steps of 10 ms the two differ by 1.3% and 1.1% of the level, with no
  shift between them.

So the engine's audio is the model's with its silences shortened by that rule, and the durations describe the engine's
audio once their times are carried through the same rule.

## Requirements

1. A measurement, run on demand as the other measurements of `README.md` are, finds for every piece of words of
   `samples/markdown-features.md`, as the plugin cuts the file into pieces for a reading from its start, read in Heart
   with the fp32 model at 1.0× and at 1.5×, the share of the time the word heard is the word that the highlight shows.
2. The word that the highlight shows at a moment is the one that the plugin's own estimate gives for that moment of
   the piece, from the audio that sherpa-onnx makes for the piece, taken from the code of the plugin and not written
   again for the measurement, so that a change of the estimate, as unit 0028 would make, is measured by running the
   measurement again.
3. The word heard at a moment is the word whose phoneme tokens the model is saying then, by the durations that
   `model.onnx` of the fp32 package gives for the tokens, the voice and the speed that the plugin hands sherpa-onnx for
   the piece, run with ONNX Runtime for Java in the tests. The times of the durations are carried into the engine's
   audio by the rule by which sherpa-onnx 2.0.2 shortens silences, applied to the audio that `model.onnx` gives with
   them: a run of samples of magnitude at most 0.01 that lasts at least 0.2 s keeps its first part, of its length times
   the silence scale of 0.2, and a moment in the part that is cut lies in no word.
4. The moments are taken every 10 ms of each piece, and only those within the tokens of a word count: the pauses
   between words, the punctuation and the silence at the start and the end of a piece are not counted.
5. Where eSpeak NG joins several words of the plugin into one phoneme word, those words count as one group: a moment
   within the tokens of the group is right when the word shown is any word of the group. Where the phoneme words cannot
   be paired with the words of the plugin in this way, as where eSpeak NG reads one written word as several, the piece
   is left out of the shares and named in the output with the reason.
6. A piece is also left out, and named with the reason, where the model's audio with its silences shortened by that
   rule does not stand for the engine's: where their lengths differ by more than 1%, or where, in steps of 10 ms, their
   levels differ by more than 10% of the level on average.
7. The measurement does the same, as a second set, for the 11 sentences of the trial of unit 0028, each read as a file
   of its own, so that its share can be put beside the trial's.
8. The measurement writes to a file under `build/`, as the other measurements do: for each set and each speed, the
   share over all pieces counted, weighted by their time, the share of that time that fell in groups of requirement 5,
   the number of pieces counted and left out, and for each piece its share or the reason it was left out.
9. Automated tests cover the logic of the measurement that is new, the pairing of phoneme words with the words of the
   plugin and its groups, the shortening of silences and the carrying of times through it, the check of requirement 6,
   the word heard at a moment by the durations, the moments that count and the share, with plain values and without
   a model or an audio device.
10. `README.md` names the measurement under "Testing", with its command, and says that it is skipped where the fp32
    model is not in its folder. The measurement needs nothing but the model and what Gradle fetches.
11. The record holds the shares of both sets at 1.0× and at 1.5× of a run with the fp32 model in place, the pieces
    counted and left out, and beside them the 54% and 52% of the trial of unit 0028, with the note that the trial made
    its phonemes a word at a time and counted with its own copy of the estimate.
12. The plugin reads, sounds and highlights as before: the unit changes nothing outside the tests, the measurement,
    the build's dependencies of the tests and the documents.

## Decisions

- **The durations of the model as the truth.** They are what the model itself decided each sound lasts, so they
  hold for every piece and every speed without listening, as in the trial of unit 0028. Timing words by hand would be
  nearer to what is heard, but slow and on a few sentences only. Decided with the human.
- **The durations of `model.onnx` itself, by ONNX Runtime for Java.** They come from the same model file the plugin
  plays, run on the same tokens, so they describe the engine's audio up to the silences that sherpa-onnx shortens, which
  requirement 3 carries them through. ONNX Runtime for Java comes from Maven Central as a dependency of the tests only,
  so the measurement needs no Python and no other weights, and runs in the sandbox as on the host. Rejected: the Kokoro
  Python package, which runs the original weights, gives other silences at the ends, and needs a Python environment and
  a download that the sandbox cannot reach. Decided with the human.
- **Heart, the fp32 model, 1.0× and 1.5×.** Heart is the voice in use until another is chosen, and the trial used it,
  so the result can be put beside the trial's. The British voices of unit 0029 are left out. 1.5× is where the human
  heard the estimate skip the most. Decided with the human for the voice, and by the specifier for the rest.
- **The plugin's own pieces and estimate.** The measurement runs the plugin's code from the text to the estimate, so
  it measures what a listener sees, and unit 0028 measures its own way by running it again. A copy of the estimate in
  the measurement would measure the copy. Decided by the specifier.
- **Every piece of the sample, and the trial's sentences besides.** The whole sample holds headings, lists, links and
  code as well as prose, so a share over all of it says more about real reading. The trial's 11 sentences give a share
  that sits nearer to the trial's 54% and 52%, though the trial made its phonemes a word at a time and used a copy of
  the estimate over whole sentences. Decided with the human.
- **Only the time of the words counts.** In a pause no word is being said, so the word shown then can be neither
  right nor wrong. The trial counted speech only as well. Decided by the specifier.
- **A group of joined words is right for any word in it.** The durations give the time of the group and not of each
  word in it, so no moment within it can be given to one word rather than another. The joined words are short, as "of
  the", and the share of the time in groups is written out, so their weight in the result is seen. Rejected: sharing
  the time of a group among its words by their letters, which would score the estimate against a guess of the same
  kind; and leaving such pieces out, which leaves out nearly every sentence. Decided by the specifier and open to the
  human.
- **sherpa-onnx's own rule for silences, not a search for the best place.** The engine's audio is the model's with
  some silences shortened, inside a piece as well as at its ends, so no single place lines the two up where a piece
  has a long pause. The noise of each run makes the samples differ even where the place is right, so the difference of
  the samples cannot tell a right place from a wrong one. Applying the rule that sherpa-onnx applies gives the timing
  of the engine's audio from the durations directly. Rejected: one place per piece, which left out most long pieces;
  and placing by the level in steps of 10 ms alone, which put the long piece 300 ms off before its pause. Decided by
  the specifier, after the human chose to place the engine's audio by its level and its silences, and open to the
  human.
- **1% for the lengths and 10% for the levels.** On the two pieces tried, the lengths differed by 0.03% or less and the
  levels by 1.3% or less. The noise can move a silence across the 0.01 that the rule tests, and make a run of silence
  that is cut in one audio and kept in the other, which changes the length by tenths of a second; the check leaves such
  a piece out rather than score it on wrong times. Decided by the specifier and open to the human.
- **The silence scale is the one the plugin plays with.** The plugin does not set it, so it is sherpa-onnx's 0.2. The
  measurement takes it from the `GenerationConfig` that the plugin makes, so that a later change of it is followed.
  Decided by the specifier.

## Non-goals

- Making the estimate better. The unit measures it only.
- The delay between the position the plugin reads from the audio line and what comes out of the speakers, which may
  differ between Linux x64 and macOS. The measurement works on the audio itself, as unit 0028 would.
- Other voices, other speeds and the int8 model.
- Changing the spec of unit 0028, such as adding what it must improve to be kept, or correcting what it says of the
  trial. That is decided once this unit has measured.
- Shipping ONNX Runtime for Java with the plugin, or naming it in `THIRD-PARTY.md`, since only the tests use it.

## Beyond the request

1. Also counting the pauses, with the word before as the right one, as the plugin shows it. It costs a second share
   in the output and its tests. Without it, a highlight that moves on too early within a pause goes unnoticed, which is
   rare since a pause is short.
2. Repeating the trial's forced alignment in this measurement, so that both are known on the same pieces before unit
   0028 is built. It costs the aligner model in the measurement and a part of the work of unit 0028 done twice. Without
   it, the comparison waits until unit 0028 is built.
