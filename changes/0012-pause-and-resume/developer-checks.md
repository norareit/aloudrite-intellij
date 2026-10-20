# The developer's checks of pause and resume

The checks of requirements 14 and 15 of [spec.md](spec.md), which the automated tests cannot make. The check of the
sound, requirement 14, is made on Linux x64 and on macOS on Apple Silicon. The check of the look, requirement 15, is
made on Linux x64 only. The spec's decision on the look says why.

What the developer finds goes into [record.md](record.md), by the implementer on the developer's word.

## Setup, on both machines

1. Check out `feature-0012-pause-and-resume` and run `./gradlew runIde`. A separate sandbox IDE opens with the
   plugin.
2. In that IDE, open the project and then `samples/markdown-features.md`. Where the speech model is not on the machine
   yet, the first press of "Read Aloud" offers to download it, 132 MB.
3. Open the log with Help › Show Log in Files. It is `idea.log` under `.intellijPlatform/sandbox/.../log/`. Search it
   for "First sound".

## Requirement 14, the sound, on both machines

4. Press "Read Aloud" and let it read into the first sections.
5. **Pause in the middle of a sentence.** Press the pause button partway through a sentence. Note whether the sound
   stopped within about a second.
6. **Resume.** Press the button again, which now shows the person with a line in front, and note:
    * whether sound is heard within about a second, and the number in the log line "First sound N ms after Resume
      Reading was pressed"
    * whether the reading starts again at the beginning of the sentence it was paused in
7. **Pause between two sentences.** Repeat steps 5 and 6, pausing just after a sentence ends. The resume begins with
   the sentence that was being heard, or with the next one where the pause fell right at the boundary. Note which
   sentence was heard.
8. **Pause in the silence after a code block.** Wait for the section "Code block". Right after "code example", during
   the two seconds of silence, pause and then resume. The reading goes on with what follows the code block, with no
   rest of the silence and no "code example" again.
9. **Stop a reading that plays.** Press the red stop button on the tab. The sound stops within a second, the stop
   button goes, and the first button shows the person and "Read Aloud" again.
10. **Stop a reading that is paused.** Start the reading again, pause it, then press the red stop button. The result
    is the same as in step 9.
11. **Note the results**, a few lines per step.

## Requirement 15, the look, on Linux x64 only

Steps 12 to 17 are made once in the light theme and once in the dark theme.

12. **Show Run and Stop in the toolbar.** In the sandbox IDE, choose Run › Edit Configurations › + › Shell Script,
    set Script text to `sleep 600`, and run it. Run and Stop now show as active in the main toolbar.
13. **Look at the tab buttons at their real size**, beside Run and Stop, in each state:
    * nothing is read: the green person
    * a file is read: the pause and the red stop
    * the reading is paused: the person with a line, and the red stop
    * another Markdown tab while a file is read: the green person, and no stop button
14. **Look at the background.** Note whether the grey background behind the plugin's buttons shows in every state,
    and whether it sets them apart from the icons beside them that switch between editor and preview.
15. **Look at the icon of the tool window.** The AloudRite icon on the tool window bar on the right is the person in
    grey, as the other tool window icons are.
16. **Switch the theme** with Settings › Appearance › Theme, and repeat steps 13 to 15.
17. **Note whether any icon of the plugin was taken for Run or Stop.**
