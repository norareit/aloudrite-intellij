// Draws the sentence and the word that AloudRite is reading in the preview of the Markdown plugin. The page describes its
// text to the IDE after each patch, the IDE finds the words in it and sends back where to draw them, and this script
// draws them with the highlights of the browser, which change no element and no text of the page. Where the page cannot
// do that, it does nothing and throws nothing, and the IDE goes on without highlights in the preview.
(function () {
  "use strict";

  const PAGE_TAG = "aloudrite.page";
  const NO_RANGES_TAG = "aloudrite.noRanges";
  const PLACES_TAG = "aloudrite.places";
  const SENTENCE_HIGHLIGHT = "aloudrite-sentence";
  const WORD_HIGHLIGHT = "aloudrite-word";
  const SCROLL_KEYS = new Set(["PageUp", "PageDown", "Home", "End", "ArrowUp", "ArrowDown", " ", "Spacebar"]);
  // The colours are written by the IDE, and only a colour of that form goes into a rule of the page
  const COLOUR_PATTERN = /^rgba\(\d{1,3}, \d{1,3}, \d{1,3}, [0-9.]+\)$/;
  const RANGE_PATTERN = /^(\d+)\.\.(\d+)$/;
  const SKIPPED_TAGS = new Set(["script", "style", "template"]);

  const pipe = window.__IntelliJTools && window.__IntelliJTools.messagePipe;
  const canDraw = typeof IncrementalDOM !== "undefined" && IncrementalDOM.notifications && pipe
    && window.CSS && CSS.highlights && typeof Highlight === "function" && typeof CSSStyleSheet === "function"
    && "replaceSync" in CSSStyleSheet.prototype && "adoptedStyleSheets" in document;
  if (!canDraw || window.__aloudriteReadAlong) return;
  window.__aloudriteReadAlong = true;

  const sheet = new CSSStyleSheet();
  document.adoptedStyleSheets = [...document.adoptedStyleSheets, sheet];

  // The text nodes of the last description, in the order the IDE numbers them, and its version
  let textNodes = [];
  let version = 0;
  let noRangesPosted = false;

  let following = true;
  let reading = null;
  let resumes = null;
  let pointerIsDown = false;

  function positionAttribute() {
    const meta = document.querySelector('meta[name="markdown-position-attribute-name"]');
    return meta ? meta.content : null;
  }

  // The text nodes of the page and the elements that carry a range of the file, in document order. An element with a range
  // opens and closes, any other is passed through, and every text node is sent, white space only ones included, so that
  // the numbering is the same on both sides
  function describe() {
    clearDrawing();
    const attribute = positionAttribute();
    const nodes = [];
    const found = [];
    let ranged = 0;

    function walk(node) {
      if (node.nodeType === Node.TEXT_NODE) {
        nodes.push(["t", node.data]);
        found.push(node);
        return;
      }
      if (node.nodeType !== Node.ELEMENT_NODE || SKIPPED_TAGS.has(node.localName)) return;
      const range = attribute ? RANGE_PATTERN.exec(node.getAttribute(attribute) || "") : null;
      if (range) {
        ranged++;
        nodes.push(["o", Number(range[1]), Number(range[2])]);
      }
      node.childNodes.forEach(walk);
      if (range) nodes.push(["c"]);
    }

    document.body.childNodes.forEach(walk);
    textNodes = found;
    version++;
    pipe.post(PAGE_TAG, JSON.stringify({version, nodes}));
    if (ranged === 0 && !noRangesPosted) {
      noRangesPosted = true;
      pipe.post(NO_RANGES_TAG, "");
    }
  }

  function rangesOf(pieces) {
    const ranges = [];
    for (const [index, start, end] of pieces) {
      const node = textNodes[index];
      if (!node || start > end || end > node.length) continue;
      const range = new Range();
      range.setStart(node, start);
      range.setEnd(node, end);
      ranges.push(range);
    }
    return ranges;
  }

  function colourOrNothing(colour) {
    return typeof colour === "string" && COLOUR_PATTERN.test(colour) ? colour : null;
  }

  function showHighlight(name, ranges, priority) {
    if (ranges.length === 0) {
      CSS.highlights.delete(name);
      return;
    }
    const highlight = new Highlight(...ranges);
    highlight.priority = priority;
    CSS.highlights.set(name, highlight);
  }

  function rulesOf(message, attribute) {
    const rules = [];
    const sentenceColour = colourOrNothing(message.sentenceColour);
    const wordColour = colourOrNothing(message.wordColour);
    if (sentenceColour) rules.push(`::highlight(${SENTENCE_HIGHLIGHT}) { background-color: ${sentenceColour}; }`);
    if (wordColour) rules.push(`::highlight(${WORD_HIGHLIGHT}) { background-color: ${wordColour}; }`);
    const element = message.wordElement;
    if (attribute && Array.isArray(element) && element.length === 2) {
      const outline = wordColour || "currentColor";
      rules.push(`[${attribute}="${Number(element[0])}..${Number(element[1])}"] { outline: 2px solid ${outline}; outline-offset: 2px; }`);
    }
    return rules.join("\n");
  }

  function clearDrawing() {
    CSS.highlights.delete(SENTENCE_HIGHLIGHT);
    CSS.highlights.delete(WORD_HIGHLIGHT);
    sheet.replaceSync("");
  }

  function elementOutlinedBy(message, attribute) {
    const range = message.wordElement;
    if (!attribute || !Array.isArray(range) || range.length !== 2) return null;
    return document.querySelector(`[${attribute}="${Number(range[0])}..${Number(range[1])}"]`);
  }

  // The rectangle from the top of the first range to the bottom of the last
  function rectOfRanges(ranges) {
    const first = ranges[0].getBoundingClientRect();
    const last = ranges[ranges.length - 1].getBoundingClientRect();
    const top = Math.min(first.top, last.top);
    const bottom = Math.max(first.bottom, last.bottom);
    return {top, bottom, height: bottom - top, width: Math.max(first.width, last.width)};
  }

  // Puts what is heard in the middle of the window where it is out of view, without the animation that would take it out of
  // view again before it arrived. What is taller than the window has its top put at the top of the window, so that it does
  // not begin above it. The scroll event this makes is no scroll of the user, since no pointer is down for it
  function keepInView(rect) {
    if (rect.height === 0 && rect.width === 0) return;
    if (rect.top >= 0 && rect.bottom <= window.innerHeight) return;
    const middle = rect.height > window.innerHeight ? 0 : (window.innerHeight - rect.height) / 2;
    window.scrollTo({top: window.scrollY + rect.top - middle, behavior: "instant"});
  }

  function places(message) {
    if (message.clear) {
      clearDrawing();
      return;
    }
    if (message.version !== version) return;
    if (message.reading !== reading || message.resumes !== resumes) {
      reading = message.reading;
      resumes = message.resumes;
      following = true;
    }
    const attribute = positionAttribute();
    const wordRanges = rangesOf(message.word || []);
    showHighlight(SENTENCE_HIGHLIGHT, rangesOf(message.sentence || []), 0);
    showHighlight(WORD_HIGHLIGHT, wordRanges, 1);
    sheet.replaceSync(rulesOf(message, attribute));
    if (!following) return;
    if (wordRanges.length > 0) {
      keepInView(wordRanges[0].getBoundingClientRect());
      return;
    }
    const outlined = elementOutlinedBy(message, attribute);
    if (outlined) {
      keepInView(outlined.getBoundingClientRect());
      return;
    }
    const sentenceRanges = rangesOf(message.sentence || []);
    if (sentenceRanges.length > 0) keepInView(rectOfRanges(sentenceRanges));
  }

  function stopFollowing() {
    following = false;
  }

  window.addEventListener("wheel", stopFollowing, {passive: true, capture: true});
  window.addEventListener("keydown", event => {
    if (SCROLL_KEYS.has(event.key)) stopFollowing();
  }, true);
  for (const down of ["pointerdown", "mousedown"]) window.addEventListener(down, () => { pointerIsDown = true; }, true);
  for (const up of ["pointerup", "pointercancel", "mouseup"]) window.addEventListener(up, () => { pointerIsDown = false; }, true);
  // A scroll while a button is held down is the user dragging the scroll bar. Any other scroll, such as the one that the
  // editor beside the preview causes, leaves the preview following
  window.addEventListener("scroll", () => {
    if (pointerIsDown) stopFollowing();
  }, {passive: true});

  pipe.subscribe(PLACES_TAG, data => places(JSON.parse(data)));

  IncrementalDOM.notifications.afterPatchListeners = IncrementalDOM.notifications.afterPatchListeners || [];
  IncrementalDOM.notifications.afterPatchListeners.push(describe);
  // Where the page was patched before this script was loaded
  if (document.body && document.body.childNodes.length > 0) describe();
})();
