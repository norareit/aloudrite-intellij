# Highlight the words being read in the preview

## Approach

Unit 0025 already works out the sentence and the word being heard as ranges of the file, maps them through the edits
made since the press, and draws them in every text editor of the file from `ReadAlongHighlighter`. This unit adds the
previews of the file as a second kind of view that the same refresh draws in. Nothing about the estimate changes.

The work falls into five layers. Only the last two know about the Markdown plugin or the browser.

1. **The text of a page.** A page of the preview describes itself to the IDE: after each patch, it sends the text of
   each of its text nodes in document order, interleaved with the opening and closing of every element that carries a
   range of the file. A pure `PreviewText` turns that description into text nodes, each with the span of the file it
   was made from, and ranged elements, each with the text nodes inside it.
2. **The places in a page.** A pure `PreviewPlaces` aligns the text of a text node with the characters of its span of
   the file, and finds, for a range of the file, the pieces of text nodes that show it, or the smallest element that
   holds it where no text shows it. It is tested on plain values and on the HTML that the Markdown library of the
   platform makes, so that the finding of the words needs no browser (R2, R3, R4, R11).
3. **The pages and the messages.** `PreviewPage` is one page in the IDE: its file, the last description it sent, and
   a function that sends a message to it. `PreviewPages` is the application service that holds the pages of every
   file and writes the one line to the log. `PreviewMessage` builds the JSON that tells a page what to draw. None of
   them names a class of the Markdown plugin.
4. **The highlighter.** `ReadAlongHighlighter` sends each page of the file the places at every refresh it draws the
   editors at, and clears them wherever it removes the editors' highlights. So the preview and the editor show the
   same places at the same moments, follow the same pause, resume, silence, speed and end, and obey the same checkbox
   (R1, R6, R7).
5. **The Markdown side.** Loaded only through an optional dependency on the Markdown plugin, a
   `MarkdownBrowserPreviewExtension.Provider` gives every preview panel an extension that adds the plugin's script to
   the page, connects the page's messages to a `PreviewPage` and registers it for the panel's file. The script
   describes the page, draws the places with the highlights of the browser, and scrolls (R5, R8, R9, R10).

What the Markdown plugin of IntelliJ IDEA 2026.2.3 does, as read from its classes and scripts for this plan:

* The extension point is `org.intellij.markdown.browserPreviewExtensionProvider`, with the interface
  `org.intellij.plugins.markdown.extensions.MarkdownBrowserPreviewExtension.Provider`, whose
  `createBrowserExtension(panel: MarkdownHtmlPanel)` is called for each panel each time the panel loads its
  extensions. The extension gives `scripts` by name and a `resourceProvider` that serves them, as the plugin's own
  `ProcessLinksExtension` does. The panel disposes it when it loads its extensions again and when it is disposed. The
  panel reads the extensions when it is made and listens to no change of the extension point, so the provider has to
  be registered in the descriptor and not later from code
* `MarkdownHtmlPanel.getBrowserPipe()` gives a `BrowserPipe`, or null by its default. `BrowserPipe.send(tag, data)`
  runs a script in the page, and `subscribe(tag, handler)` calls the handler on a thread of the browser, not the event
  thread. In the page, the pipe is `window.__IntelliJTools.messagePipe`, with `subscribe(tag, callback)` and
  `post(tag, data)`
* The page names the attribute of the ranges in `<meta name="markdown-position-attribute-name">`, and its value is
  `<start>..<end>` in offsets of `document.getText()`, which `HtmlSourceTextPreprocessor` hands to the Markdown library
  unchanged. Elements the plugin adds without a range, such as the spans of the highlighting of a code block, sit
  between them
* The page is patched with `IncrementalDOM.patch`, and every function in
  `IncrementalDOM.notifications.afterPatchListeners` is called after each patch
* The preview follows the editor beside it: a scroll of the text editor of the split editor scrolls the preview to it.
  Nothing listens to the scroll of the preview to scroll the editor in this version, so a scroll of the preview moves
  no editor

How the parts are decided, so that the implementer does not re-derive it:

* **The description (R2).** The script walks `document.body` depth first. An element with the attribute sends
  `["o", start, end]` before its children and `["c"]` after them. Any other element sends nothing of its own, and its
  children are walked as if they were the children of its parent. `script`, `style` and `template` elements are
  skipped with their children. Each text node sends `["t", text]`, white space only ones included, so that the
  numbering of the text nodes is the same on both sides. The message is
  `aloudrite.page` with `{"version": n, "nodes": [...]}`, where the version counts up in the page at each description
* **The span of a text node.** Its start is the last boundary passed before it: the start of the element whose
  `"o"` came last, or the end of the element whose `"c"` came last, whichever is later in the walk, and 0 before any.
  Its end is the next boundary after it, by the same rule, or its start where there is none. A ranged element knows
  the index of its first text node and the index after its last
* **The alignment.** The characters of a text node are matched in order with the characters of its span. For each
  one, the first character of the span at or after the place reached that is the same, or white space for white
  space, is its offset, and the place moves past it. A character that has no match, such as the `©` of `&copy;`, gets
  the place reached as its offset, and the place does not move. So the asterisks of bold text, the backslash of an
  escape and the address of a link are passed over, and an entity counts as the place where it starts. The offsets of
  a text node are worked out the first time they are needed for a description and kept with it
* **The pieces of a range.** For a range of the file, every text node whose span overlaps it gives each run of its
  characters whose offsets lie within the range as a piece `[text node, start, end]` in UTF-16 units, which are those
  of the page as of the file
* **Where no text shows the word (R3, R4).** Where the word gives no piece, its element is the smallest ranged element
  whose range holds the word, the last one in the walk of those of equal size. Where that element holds text, the word
  is all of its text, so that "link", which is read for the address, is drawn on the text of the link. Where it holds
  none, as an image or a diagram, the message names the element by its range, and the script outlines it. "code
  example" and "see table" need no rule of their own, since their range is the whole block, whose text shows it. A
  word that `TextEdits` gives as null, because all its characters were deleted, is not drawn. The sentence has no
  fallback: where no text shows it, it is not drawn
* **The message (R1, R7, R9).** `aloudrite.places` with
  `{"version": n, "reading": number, "resumes": count, "sentenceColour": css, "wordColour": css, "sentence": [pieces],
  "word": [pieces], "wordElement": [start, end] or null}`. The version is that of the description the pieces were
  found in, and the script drops a message of another version. The colours are the background colours of
  `HEARD_SENTENCE_KEY` and `HEARD_WORD_KEY` in `EditorColorsManager.getInstance().globalScheme`, which is the scheme
  the preview's own colours come from, written as `rgba(r, g, b, alpha / 255)`, or null where the key has no
  background, as in the editor. Clearing is `aloudrite.places` with `{"clear": true}`, which applies to every version.
  A page is sent a message only when it differs from the last one it was sent, and a new description makes it be sent
  again
* **Drawing (R5, R9).** The script draws with `CSS.highlights`, under the names `aloudrite-sentence` and
  `aloudrite-word`, the word with the higher `priority`. Their colours, and the outline of a word element, are rules of
  a `CSSStyleSheet` of the script's own in `document.adoptedStyleSheets`: `::highlight(aloudrite-sentence)` and
  `::highlight(aloudrite-word)` with only `background-color`, and `[<attribute>="<start>..<end>"]` with an
  `outline` of 2 pixels in the word's colour and an `outline-offset` of 2 pixels. No element is added, removed, moved
  or given an attribute, and no text changes. At each patch the script drops what it drew, since the text nodes may be
  new, and describes the page again
* **Scrolling (R8).** The script follows from its load, and again whenever a message brings another reading number
  or another count of resumes. It stops at a `wheel` event, at a `keydown` of PageUp, PageDown, Home, End, ArrowUp,
  ArrowDown or Space, and at a `scroll` event while a pointer button is held down, as in dragging the scroll bar. Other
  scrolls, such as the one the editor beside it causes, leave it following. While it follows and the first piece of
  the word, or the word element, lies outside the window, it scrolls with
  `window.scrollTo({top: ..., behavior: "instant"})` to put it in the middle. The editors keep the rule of unit 0025
  unchanged, since no scroll of the preview moves them
* **When the preview cannot show them (R10).** The one line goes to the IDE's log through
  `PreviewPages.noHighlightsInThePreview(reason)`, once per IDE session whatever the reason, at level info, reading
  `AloudRite: the preview shows no highlights, as <reason>`. The reasons are:
    * the extension point is missing, which is also the case without the Markdown plugin. The highlighter asks
      `ApplicationManager.getApplication().extensionArea.hasExtensionPoint("org.intellij.markdown.browserPreviewExtensionProvider")`
      when it first shows a reading
    * a panel gives no pipe. The extension then gives no script
    * the page holds no ranges: after a patch, the script finds no meta element or no element with the attribute, and
      posts `aloudrite.noRanges` once for the load of the page
  With the extension point missing, the descriptor's extension names an extension point that does not exist. Whether
  the platform then reports an error to the user is checked in the sandbox IDE, as a task below says
* **The boundary.** Only the files in `com.ritenoar.aloudrite.preview` name classes of the Markdown plugin, and only
  `aloudrite-markdown.xml`, which `plugin.xml` loads with `<depends optional="true">`, names those files. A test reads
  the sources to keep it so. The JSON is read and written with Gson, which the platform carries

## Files

- `build.gradle.kts`: `bundledPlugin("org.intellij.plugins.markdown")` in place of `testBundledPlugin`, so that the
  Markdown side compiles against it
- `src/main/resources/META-INF/plugin.xml`: `<depends optional="true" config-file="aloudrite-markdown.xml">`
  `org.intellij.plugins.markdown</depends>`
- `src/main/resources/META-INF/aloudrite-markdown.xml`: new. Registers the provider under
  `<extensions defaultExtensionNs="org.intellij.markdown"><browserPreviewExtensionProvider .../></extensions>`
- `src/main/kotlin/com/ritenoar/aloudrite/PreviewText.kt`: new. `PreviewText`, `PreviewTextNode`, `PreviewElement`,
  and the reading of the description
- `src/main/kotlin/com/ritenoar/aloudrite/PreviewPlaces.kt`: new. The alignment, the pieces of a range and the word's
  element
- `src/main/kotlin/com/ritenoar/aloudrite/PreviewMessage.kt`: new. The JSON of `aloudrite.places` and of the clearing,
  and the colour as CSS
- `src/main/kotlin/com/ritenoar/aloudrite/PreviewPages.kt`: new. `PreviewPage`, the application service
  `PreviewPages`, and the one line to the log
- `src/main/kotlin/com/ritenoar/aloudrite/ReadAlongHighlighter.kt`: sends to the pages and clears them, and checks the
  extension point once
- `src/main/kotlin/com/ritenoar/aloudrite/preview/ReadAlongPreviewExtension.kt`: new. The provider and the extension
- `src/main/resources/com/ritenoar/aloudrite/preview/aloudrite-read-along.js`: new. The script of the page
- `src/test/kotlin/com/ritenoar/aloudrite/PreviewHtml.kt`: new. `describedPage(markdown)`, which makes the HTML with
  `org.intellij.markdown.html.HtmlGenerator` and `includeSrcPositions = true`, parses it with jsoup and walks it by
  the rule of the script, to give the description a page would send
- `src/test/kotlin/com/ritenoar/aloudrite/PreviewTextTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/PreviewPlacesTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/PreviewMessageTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/PreviewPagesTest.kt`: new
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAlongPreviewTest.kt`: new. The highlighter with pages that record what
  they are sent, on the stand-ins of unit 0006
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAlongHighlighterTest.kt`: builds the highlighter with its new
  parameters
- `src/test/kotlin/com/ritenoar/aloudrite/ReadAlongPreviewExtensionTest.kt`: new. The Markdown side with a panel and a
  pipe of the test's own, the descriptors, and the boundary
- `README.md`: the highlights in the preview, and what happens where the preview cannot show them
- `changes/0026-highlight-the-words-being-read-in-the-preview/record.md`: the captures, the checks in the sandbox IDE
  and the developer's check

## Tasks

- [x] Write `PreviewTextTest` on descriptions written by hand. It covers:
    * a paragraph `One **two** three.` gives three text nodes with the spans of the paragraph's start to the bold's
      start, the bold's range, and the bold's end to the paragraph's end
    * an element without a range is passed through, its text nodes taking the spans of its ranged parent
    * a text node before any boundary starts at 0, and one after the last boundary ends at its start
    * a ranged element knows its first text node and the one after its last, also when it holds none
    * text nodes of white space only are counted
    * a description that is not JSON, or whose nodes are not of the three kinds, gives null

  Then write `PreviewText` and its reading of the description until it passes (R2)
- [x] Write `PreviewHtml.kt` in the tests. Where jsoup is not on the test classpath through the platform, add
  `testImplementation("org.jsoup:jsoup:1.21.2")`
- [x] Write `PreviewPlacesTest`. The ranges are given as offsets found in the Markdown, so that each check reads as
  the characters it means. On plain values it covers:
    * a word of plain text gives one piece over its letters
    * a word in bold, in italic, in inline code and in the text of a link gives its letters, without the marks
    * a word with formatting inside it, as `foo**bar**`, gives a piece in each of the two text nodes
    * an escaped character and an entity are in the word they are read in
    * a sentence over two paragraphs' worth of text nodes, and one over bold text, gives a piece in each text node
    * a range that no text node overlaps gives no pieces

  On `describedPage` of Markdown texts it covers:
    * "code example", as the range of a fenced code block, gives the text of the block
    * "see table", as the range of a table, gives the text of its cells
    * "link", as the range of the address, gives no pieces, and its element is the link, whose text is the word
    * an image, as the range of `![alt](boat.png)`, gives no pieces and names the image's element by its range
    * the word's element is the smallest one holding it, and of equal ones the last
    * the offsets of a text node are worked out once for a description

  Then write `PreviewPlaces` until it passes (R2, R3, R4, R11)
- [ ] Commit: `feature-0026 Find the words heard in the text of the preview`
- [ ] Write `PreviewMessageTest`. It covers:
    * the places message holds the version, the reading's number and resumes, both colours, the pieces of the
      sentence and of the word, and the word's element or null
    * a colour with an alpha is written as `rgba` with the alpha as a share of 1, and a missing one as null
    * the clearing message is `{"clear":true}`

  Then write `PreviewMessage` (R1, R3, R7, R9)
- [ ] Write `PreviewPagesTest`. It covers:
    * a page registered for a file is among the pages of that file and not of another, and is gone once the disposable
      it was registered with is disposed
    * a page that receives a description keeps it, and one that receives one that cannot be read keeps the last one
    * a page sends a message only when it differs from the last one sent, and again after a new description
    * `aloudrite.noRanges` from a page writes the line to the log
    * the line is written once per instance of `PreviewPages`, whatever the reasons and however often they come, and
      reads as decided above

  The log is a function given to `PreviewPages`, which defaults to `Logger.info`. Then write `PreviewPage` and
  `PreviewPages`, registered as an application service through `@Service` (R10)
- [ ] Commit: `feature-0026 Hold the pages of the preview and the messages to them`
- [ ] Write `ReadAlongPreviewTest` with `ApplicationRule`, `EditorsOfATest` and `SpeechStandIns`, as
  `ReadAlongHighlighterTest` is written. Its pages are `PreviewPage`s registered in a `PreviewPages` of the test for
  the file read, which record the messages they are sent and are given the description of `describedPage` of the
  file's text. It covers:
    * at each slice, the last message to each of two pages of the file shows as the sentence and the word the same
      characters as the editor's two highlighters of that refresh, and a page of another file is sent nothing
    * a page with no description is sent nothing, and once it receives one it is sent the places at the next refresh
    * "code example" heard gives the text of the code block as the word, and "link" the text of the link
    * the message is not sent again while the places stay, and so not while paused
    * a resume sends the new count of resumes, and the next reading its new number
    * the end of the reading, a stop, another reading and a failure of the engine send the clearing message to every
      page of the file once the events of the EDT are dispatched after `readingChanged()`
    * the checkbox cleared during the reading sends the clearing message at the next refresh while the reading goes
      on, and checked again sends the places at the next refresh
    * a reading after a download, whose document changed, sends the clearing message and no places
    * with the extension point reported missing, the editors show their highlights as before and the log has the one
      line, and with it present the log has none (R1, R3, R6, R7, R10, R11)
- [ ] In `ReadAlongHighlighter`, take `previews: () -> PreviewPages` and `hasPreviewExtensionPoint: () -> Boolean`
  after `setting`, with the services and the check of the extension area in the secondary constructor. In
  `ShownReading.show`, after the editors, send each page of the file the places message, with the ranges mapped
  through the edits as for the editors. In `removeHighlights` and `end`, send the clearing message to the pages of the
  file. Check the extension point when a reading is first shown. Update the construction in
  `ReadAlongHighlighterTest`. Run until `ReadAlongPreviewTest` and `ReadAlongHighlighterTest` pass (R1, R6, R7, R10)
- [ ] Commit: `feature-0026 Send the places heard to the previews of the file`
- [ ] Write `ReadAlongPreviewExtensionTest` with `ApplicationRule`, a `MarkdownHtmlPanel` of the test's own that gives
  a file and a pipe of the test's own, and a `PreviewPages` of the test. It covers:
    * the extension of a panel with a pipe gives the script `aloudrite-read-along.js`, its resource provider serves
      the file's bytes as `text/javascript` and serves no other name
    * it subscribes to `aloudrite.page` and `aloudrite.noRanges`, and a description posted through the pipe, from
      another thread, reaches the page registered for the panel's file on the EDT
    * a message sent to that page goes out through the pipe as `aloudrite.places`
    * disposing the extension removes the page and both subscriptions
    * a panel without a pipe gives an extension with no script, registers no page, throws nothing and writes the line
      to the log
    * a panel without a file registers no page
    * the descriptor of the plugin depends on `org.intellij.plugins.markdown` optionally with `aloudrite-markdown.xml`,
      and the provider is among the extensions of `org.intellij.markdown.browserPreviewExtensionProvider`
    * no source file under `src/main/kotlin` outside `com/ritenoar/aloudrite/preview` imports or names
      `org.intellij.plugins.markdown`, and `plugin.xml` does not name the package `preview` (R10, R11)
- [ ] Change `build.gradle.kts`, `plugin.xml` and add `aloudrite-markdown.xml`. Write `ReadAlongPreviewProvider` and
  `ReadAlongPreviewExtension`: the handlers hand what they receive to the EDT with `invokeLater` in
  `ModalityState.any()`, and the extension unregisters and unsubscribes in `dispose`. Run until
  `ReadAlongPreviewExtensionTest` passes (R10)
- [ ] Write `aloudrite-read-along.js` by the rules above: describe at load where the body already holds the page and
  after each patch, post `aloudrite.noRanges` once where the page holds no ranges, draw and clear, follow and scroll.
  It does nothing, and throws nothing, where `IncrementalDOM`, the pipe or `CSS.highlights` is missing (R2, R4, R5, R8,
  R9, R10)
- [ ] Run the whole `./gradlew test`, and `./gradlew buildPlugin`, and see the archive within its size limit
- [ ] Commit: `feature-0026 Draw the places heard in the preview of the Markdown plugin`
- [ ] Start the sandbox IDE with `./gradlew runIde` and open `samples/markdown-features.md`. Read it, and capture only
  the window whose title ends in `[aloudrite-intellij]` with `xwd -id <window id>`, as `AGENTS.md` says, in Islands
  Light and in Islands Dark, with the preview alone and with the editor and the preview side by side. Take these
  captures:
    * a paragraph with bold or a link being read
    * the code block being read
    * the table being read
    * the image being read, with its outline
    * the preview after it scrolled by itself

  Check that the word stands out from the sentence, that the text stays readable, that a selection of text in the
  preview copies the same text as with no reading, that the preview stops following at a scroll of the mouse wheel
  in it and follows again at a resume, and that side by side, a scroll of the editor by itself neither stops the
  preview from following nor the preview's scroll stops the editor's, and that the two do not scroll each other back
  and forth. Write in `record.md` what the captures showed (R2, R3, R4, R5, R8, R9, R12)
- [ ] In the sandbox IDE, check the cases of R10 and write in `record.md` what each showed:
    * with the Markdown plugin disabled under Settings › Plugins and the IDE restarted, the plugin loads, a reading
      highlights the editor, no error is shown, and `idea.log` of the sandbox has the one line
    * with the name of the extension point changed by hand in `aloudrite-markdown.xml` for the check alone, and the
      change reverted afterwards, the same. Where the platform shows an error for the unknown extension point, log it
      in `record.md` as a finding for the human, since R10 then cannot be met with the provider registered in the
      descriptor (R10)
- [ ] Commit: `feature-0026 Record the look of the highlights in the preview`
- [ ] Describe in `README.md`, in place of the sentence that says the preview shows no highlights in this version, the
  highlights in every preview of the file. Say:
    * that the preview shows the same sentence and word as the editor, at the same moments, on the text as the preview
      shows it, in each element a sentence runs through
    * that "code example" and "see table" highlight the block, "link" the text of the link, and that an image or a
      diagram, which holds no text, is outlined
    * that the page is not changed, so copying gives the same text
    * that they come back after the preview is updated or shown again, and appear in a preview opened during the
      reading from its next update on
    * that the preview scrolls by itself, stops at a scroll of the user in it, follows again from the next resume or
      reading, and that with the editor beside it each follows by itself
    * that the colours are those of the two keys, in the theme of the preview
    * that the checkbox turns them off in the preview as well
    * that the plugin depends on the Markdown plugin optionally, and that without it, or where its preview cannot show
      them, the plugin reads as before, shows the highlights in the editor, and writes one line to the IDE's log
      (R14)
- [ ] Commit: `feature-0026 Describe the highlights in the preview in the README`

## Verification

- [ ] `./gradlew test` passes. The unit does not change the speech engine, sherpa-onnx or the text handed to the
  engine, so the tests with the real engine need not run
- [ ] `./gradlew buildPlugin` builds the archive within its size limit
- [ ] `npm run check-changes` passes on the committed branch
- [ ] Every requirement has a task:
    * R1: `ReadAlongPreviewTest` and the sending from the highlighter
    * R2: `PreviewText`, `PreviewPlaces` and the description of the script
    * R3: `PreviewPlacesTest` and `ReadAlongPreviewTest` on "code example", "see table" and "link"
    * R4: the word's element and its outline
    * R5: drawing with `CSS.highlights` and an adopted sheet, and the check of copying in the sandbox IDE
    * R6: the description at each patch and at load, and the sending after a new description
    * R7: the clearing and the resumes in `ReadAlongPreviewTest`
    * R8: the following of the script and the checks side by side in the sandbox IDE
    * R9: the colours of the global scheme and the captures in both themes
    * R10: the optional dependency, the line to the log, `PreviewPagesTest`, `ReadAlongPreviewTest`,
      `ReadAlongPreviewExtensionTest` and the checks in the sandbox IDE
    * R11: the tests above, none of which needs a browser
    * R12: the captures
    * R13: the developer's check
    * R14: the README
- [ ] Requirement 13 is left to the developer: reading `samples/markdown-features.md` on Linux x64 with the preview
  alone in view, and recording in `record.md` whether the highlights followed the voice through its headings, lists,
  formatting, code blocks and tables

## Commit Points

- [ ] `feature-0026 Find the words heard in the text of the preview`
- [ ] `feature-0026 Hold the pages of the preview and the messages to them`
- [ ] `feature-0026 Send the places heard to the previews of the file`
- [ ] `feature-0026 Draw the places heard in the preview of the Markdown plugin`
- [ ] `feature-0026 Record the look of the highlights in the preview`
- [ ] `feature-0026 Describe the highlights in the preview in the README`
