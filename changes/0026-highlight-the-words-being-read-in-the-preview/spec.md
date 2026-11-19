# Highlight the words being read in the preview

## Context

Unit 0025 highlights the sentence and the estimated word being heard in the text editor of the file that is read. The
human reads mostly in the preview of a Markdown file, which unit 0025 leaves out, so this unit brings the same
highlights to the preview. It is built on unit 0025 and right after it.

The preview of the Markdown plugin that comes with the IDE is a page in an embedded Chromium. When the human asked
about it, the specifier checked the Markdown plugin of IntelliJ IDEA 2026.2.3, against which the plugin is built:

- Each element of the page, such as a paragraph, a heading, bold text or inline code, carries the range of the file it
  was made from, in an attribute whose name the page gives in a `meta` element. The preview's own scroll sync with the
  editor relies on these ranges.
- The Markdown plugin has an extension point through which another plugin adds scripts and styles to the page, and a
  pipe through which the IDE and the page send each other messages.
- The page is patched in place each time the file changes, rather than loaded again.

The ranges are on elements and not on words, so the word being heard has to be found in the text of the smallest
element whose range holds it. The extension point and the pipe belong to the Markdown plugin and not to the core of
the IntelliJ Platform, so they may change between versions of the IDE.

The plugin depends today on the platform alone.

## Requirements

1. While a file is read, every preview of the file in an open project, shown alone or beside the text editor, shows
   the two highlights of unit 0025: a light highlight of the sentence being heard and a strong highlight of the word
   being heard. They are the same sentence and word, at the same moments, as unit 0025 shows in the text editor.
2. The highlights cover the text of the preview that the characters of the sentence and the word were read from. A
   word with formatting in it or around it, such as bold, italic, inline code or the text of a link, is highlighted on
   its text as the preview shows it. A sentence that runs through several elements is highlighted in each of them.
3. Words that stand for something that is not read word by word are highlighted on the element the preview makes of
   what they stand for, as unit 0025 decides for the editor: "code example" on the code block, "see table" on the
   table, and "link" on the link whose address it is read for.
4. Where the preview shows the source of a word in a way that holds no text to match, such as a diagram or a formula,
   the strong highlight covers the smallest element whose range holds the word. Where the word is not in the preview
   at all, as when it was deleted, the preview shows no strong highlight while it is heard.
5. The highlights do not change the page: no element is added, removed or moved, and the text of the page is not
   changed. Copying text from the preview gives the same text as without them.
6. When the preview is patched after a change to the file, or shown again after it was hidden, the highlights are
   shown again within half a second. A preview of the file that is opened during a reading shows them from its next
   update on.
7. Pause, resume, a change of the speed, a silence the plugin adds and the end of a reading act on the highlights in
   the preview as they do in the editor, by requirements 7 to 9 of unit 0025.
   The checkbox of requirement 15 of unit 0025 turns the highlights and the scrolling by itself off and on in the
   preview as it does in the editor, within the same half second.
8. The preview scrolls by itself to keep the highlighted word in view. It stops doing so once the user scrolls that
   preview, and does so again from the next resume or the next reading on. With the editor and the preview side by
   side, each follows by itself, and a scroll by the plugin in one does not count as a scroll of the user in the other.
9. The colours of the highlights in the preview follow the theme of the preview, light or dark, and do not hide its
   text.
10. The plugin depends on the Markdown plugin optionally. Without the Markdown plugin, and with a Markdown plugin whose
    page holds no ranges or whose extension point or pipe is missing, the plugin loads and reads as it does without
    this unit, shows the highlights of unit 0025 in the editor, shows no error to the user, and writes one line to the
    IDE's log saying that the preview shows no highlights.
11. Automated tests cover requirement 10, and the messages the IDE sends to the page for requirements 1, 3 and 7. The
    finding of the words in the text of an element, for requirements 2 to 4, is tested on plain values, in Kotlin or
    in the script of the page, without a browser.
12. The implementer looks at the highlights in the preview of the sandbox IDE as `AGENTS.md` says, in a light and a
    dark theme, with the preview alone and beside the editor, and the record holds what the captures showed.
13. The developer reads `samples/markdown-features.md` on Linux x64 with the preview alone in view, and the record holds
    whether the highlights followed the voice through its headings, lists, formatting, code blocks and tables.
14. `README.md` describes the highlights in the preview, and what the plugin does where the preview cannot show them.

## Decisions

- **One estimate, two ways of drawing it.** The sentence and the word being heard are worked out once, as positions in
  the file, by unit 0025. The preview receives those positions and draws them. So the editor and the preview cannot
  disagree, and a better estimate later improves both.
- **Drawn with the highlights of the browser, not by changing the page.** Chromium can paint ranges of text without
  any change to the page. Wrapping the words in elements of the plugin's own would fight the patching of the preview
  after each change, and could break the preview's own scripts and the copying of text. Rejected for those reasons.
- **The words are found within the ranges the preview already carries.** The Markdown plugin puts them there for its
  scroll sync, so they are as dependable as that sync. Matching the words of the reading against the whole text of the
  page was rejected, since the same words come more than once in a file.
- **The optional dependency, and quiet when it fails.** The extension point and the pipe are the Markdown plugin's
  own and may change in a later IDE. A missing highlight in the preview is a smaller loss than a plugin that fails to
  load or reads no more, so the plugin goes on without it and says so in the log only. Decided by the specifier and
  open to the human.
- **Each view follows by itself.** With the editor and the preview side by side, the preview's own scroll sync may
  also scroll the one when the other moves. Each view keeping the word in view by itself, and telling its own scrolls
  from the user's, keeps the two from stopping each other. Decided by the specifier and open to the human.
- **Checked on Linux only.** The preview is the same embedded Chromium on both platforms and nothing here is native
  code, so the check on Linux x64 is enough, as `AGENTS.md` says.

## Non-goals

- Previews other than that of the Markdown plugin that comes with the IDE, such as one of another plugin.
- Choosing to read from a place by clicking a word in the preview.
- A preview shown outside the IDE, such as in an external browser.
- Times of the engine for each word, as for unit 0025.

## Beyond the request

1. A notification, once per IDE session, when the preview cannot show the highlights. It costs a notification and its
   test. Without it, only the IDE's log says why the highlights are missing in the preview, and a user who reads in
   the preview may think the feature is broken.
