# The developer's checks of the reading speed

These are the checks of requirements 15 and 16 of [spec.md](spec.md), which the automated tests cannot make. The check
of requirement 15 is made on Linux x64 only, and the check of requirement 16 on Linux x64 and on macOS on Apple Silicon.
The spec's decision on the machines says why.

The implementer writes what the developer finds into [record.md](record.md), on the developer's word. The tables below
can be filled in on each machine and handed over as they are.

## Setup, on each machine

1. Check out `feature-0016-reading-speed` and run `./gradlew runIde`. A separate sandbox IDE opens with the plugin.
2. In that IDE, open the project. Open the tool window "AloudRite" with its button on the tool window bar on the
   right, or with View › Tool Windows › AloudRite. Under the label "Reading speed" it shows a minus button, a field
   holding "1.0" with a grey "×" inside it, and a plus button, with a grey text under them, also while nothing is
   read. The tooltips of the minus and plus buttons are "Slower" and "Faster".
3. Open the log with Help › Show Log in Files. It is `idea.log` under `.intellijPlatform/sandbox/.../log/`. Search it
   for "First sound". After a change of the speed during a reading, the line reads "First sound N ms after the speed
   was changed".
4. A speed is set most quickly by typing it in the field and pressing Enter. Settings › Tools › AloudRite shows the
   same speed, and can set it as well.

## Requirement 15, every speed and the changes, on Linux x64 only

### The look of the row

5a. Look at the row of the speed, in the light and the dark theme, and in a narrow tool window. Click the grey "×"
    in the field: nothing happens. Note whether the label, the minus and plus buttons, the "×" and the text under the
    row are clear, and whether anything still looks like it would delete or close something.

| Look of the row | Notes |
|-----------------|-------|
| Clear           |       |

### Every speed

The speeds from 0.5 to 2.0 in steps of 0.1 were heard before the range was set, and `record.md` holds what was found.
What is left is the steps that were not among them, and one typed speed between two steps.

5. Open `samples/ten-thousand-words.md`. For each speed in the table, type it in the field and press Enter, press
   "Read Aloud" and listen to the first five paragraphs, then press "Stop" in the tool window.
6. For each speed, note how understandable it was, and whether a silence came in the middle of a sentence. Where a
   speed is not understandable, say so. The unit then stops there, and the human sets the range.

| Speed          | Understandable | A silence in the middle of a sentence |
|----------------|----------------|---------------------------------------|
| 0.75           |                |                                       |
| 0.85           |                |                                       |
| 0.95           |                |                                       |
| 1.05           |                |                                       |
| 1.15           |                |                                       |
| 1.25           |                |                                       |
| 1.37, typed    |                |                                       |

### Changes during a reading

Set the speed to 1.0, and start each of these on `samples/ten-thousand-words.md` with "Read Aloud", in the middle of a
sentence of the first paragraphs.

7. **A single press.** Press the plus button, "Faster", once. The sound stops within about a second, and the sentence
   that was playing begins again, a little faster, after half a second and the time the engine takes for it. Note the
   number of the log line.
8. **Presses in a row.** Press the plus button three times, each within half a second of the one before. The sound stops
   at the first press, and the sentence is heard once, at the speed of the last press, with no speed in between heard.
   Note the number of the log line.
9. **A typed speed.** Type "2" in the field and press Enter. The field then shows "2.0". The sentence begins again at
   2.0, with no wait of half a second. Note the number of the log line.
10. **Several changes in quick succession.** Type "1.5" and press Enter, then press the minus button, "Slower", twice
    quickly, then type "1" and press Enter, all within about two seconds. The sentence is heard once more, at 1.0, and
    no other speed is heard in between.
11. **A change while paused.** Press "Pause" in the tool window in the middle of a sentence, set the speed to 2.0,
    and wait a few seconds: nothing is heard. Press "Resume". The sentence of the pause begins again at 2.0. This
    resume may take longer than a resume otherwise does.
12. **The limits and the field.** At 0.75, the minus button is greyed out and does nothing, and at 2.0 the plus button
    is. Type "1.37" and press Enter: the field shows "1.37". Press the minus button: the field shows "1.3". Type "1.37"
    again and press the plus button: the field shows "1.4". Type "2.5", "1,5", "0.7" and then "1.555" in the field and
    press Enter after each: the speed stays as it was, and the field shows the IDE's error mark with the message "Type a
    speed from 0.75 to 2.0". Press Escape in the field: the current speed is put back, the mark goes, and the focus
    stays in the field rather than moving to the editor.
13. **The settings page and a second project.** Choose another speed under Settings › Tools › AloudRite and press OK.
    The tool window shows it. Type "1.37" in the field, then open the page: it shows 1.37 among the steps. Where a
    second project is open, its tool window shows the same speed.

| Step | As described | Log line "First sound N ms after the speed was changed" | Notes |
|------|--------------|---------------------------------------------------------|-------|
| 7    |              |                                                         |       |
| 8    |              |                                                         |       |
| 9    |              |                                                         |       |
| 10   |              | not asked for                                           |       |
| 11   |              | not asked for                                           |       |
| 12   |              | not asked for                                           |       |
| 13   |              | not asked for                                           |       |

## Requirement 16, the whole sample at three speeds, on Linux x64 and on macOS on Apple Silicon

14. Open `samples/markdown-features.md`. At each of 0.75, 1.5 and 2.0, press "Read Aloud" and listen to the file to
    its end, through both chapters of prose.
15. For each speed, note whether a silence came in the middle of a sentence, and how the silences the plugin adds
    sounded. At 2.0 the half second between two paragraphs is a quarter of a second and the two seconds after "code
    example", in the section "Code block", are one second. At 0.75 they are two thirds of a second and two and two
    thirds seconds. The silences around inline code, in the section "Inline code", and between the items of a list
    follow the speed the same way.

| Machine                | Speed | A silence in the middle of a sentence | How the silences sounded |
|------------------------|-------|---------------------------------------|--------------------------|
| Linux x64              | 0.75  |                                       |                          |
| Linux x64              | 1.5   |                                       |                          |
| Linux x64              | 2.0   |                                       |                          |
| macOS on Apple Silicon | 0.75  |                                       |                          |
| macOS on Apple Silicon | 1.5   |                                       |                          |
| macOS on Apple Silicon | 2.0   |                                       |                          |
