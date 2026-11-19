// Runs the real script of the page against a small stand-in for the browser and the pipe of the Markdown plugin, so that
// its lifecycle and its decisions to follow are tested without an embedded browser. Run by ReadAlongScriptTest
import assert from "node:assert/strict";
import fs from "node:fs";
import test from "node:test";
import vm from "node:vm";

const SCRIPT = fs.readFileSync(process.env.READ_ALONG_SCRIPT, "utf8");
const ATTRIBUTE = "md-pos";

class TextNode {
  constructor(data) { this.nodeType = 3; this.data = data; }
  get length() { return this.data.length; }
}

class ElementNode {
  constructor(localName, attributes = {}, children = []) {
    this.nodeType = 1;
    this.localName = localName;
    this.attributes = attributes;
    this.childNodes = children;
  }
  getAttribute(name) { return this.attributes[name] ?? null; }
  getBoundingClientRect() { return ElementNode.rect; }
}

const text = data => new TextNode(data);
const element = (name, attributes, ...children) => new ElementNode(name, attributes, children);

// A fresh page with the script loaded in it. Options leave out what an old preview would lack
function loadPage({body, withMeta = true, withHighlights = true, withPipe = true} = {}) {
  const posted = [];
  const subscribers = {};
  const listeners = {};
  const patchListeners = [];
  const highlights = new Map();
  const sheets = [];
  const scrolls = [];
  const page = {posted, highlights, sheets, scrolls, rect: {top: 5000, bottom: 5020, height: 20, width: 40}};

  class Range {
    setStart(node, offset) { this.startNode = node; this.start = offset; }
    setEnd(node, offset) { this.endNode = node; this.end = offset; }
    getBoundingClientRect() { return page.rect; }
  }
  class Highlight {
    constructor(...ranges) { this.ranges = ranges; this.priority = 0; }
  }
  class CSSStyleSheet {
    replaceSync(rules) { this.rules = rules; }
    constructor() { sheets.push(this); }
  }

  const document = {
    body: body ?? element("body", {}),
    adoptedStyleSheets: [],
    querySelector(selector) {
      if (selector.startsWith("meta")) return withMeta ? {content: ATTRIBUTE} : null;
      const wanted = /^\[(.+)="(.+)"\]$/.exec(selector);
      const find = node => {
        if (node.nodeType !== 1) return null;
        if (wanted && node.getAttribute(wanted[1]) === wanted[2]) return node;
        return node.childNodes.map(find).find(Boolean) ?? null;
      };
      return find(document.body);
    },
  };

  const window = {
    document,
    innerHeight: 800,
    scrollY: 0,
    scrollTo(options) { scrolls.push(options); },
    addEventListener(type, handler) { (listeners[type] ??= []).push(handler); },
    __IntelliJTools: withPipe ? {
      messagePipe: {
        post(tag, data) { posted.push({tag, data}); },
        subscribe(tag, callback) { subscribers[tag] = callback; },
      },
    } : undefined,
  };
  window.window = window;
  window.CSS = withHighlights ? {highlights} : {};

  const context = vm.createContext({
    window, document, Node: {TEXT_NODE: 3, ELEMENT_NODE: 1}, Range, Highlight, CSSStyleSheet,
    CSS: window.CSS,
    IncrementalDOM: {notifications: {afterPatchListeners: patchListeners}},
    console,
  });
  context.CSSStyleSheet = CSSStyleSheet;
  vm.runInContext(SCRIPT, context);

  page.fire = (type, event = {}) => (listeners[type] ?? []).forEach(handler => handler(event));
  page.patch = () => patchListeners.forEach(listener => listener());
  page.send = message => subscribers["aloudrite.places"](JSON.stringify(message));
  page.description = () => JSON.parse(posted.filter(entry => entry.tag === "aloudrite.page").at(-1).data);
  page.subscribed = Object.keys(subscribers);
  page.loaded = Boolean(context.window.__aloudriteReadAlong);
  page.document = document;
  return page;
}

const paragraph = () => element("body", {},
  element("div", {[ATTRIBUTE]: "0..30"},
    text("One "),
    element("strong", {[ATTRIBUTE]: "4..11"}, text("two")),
    element("span", {}, text(" three.")),
    element("script", {}, text("ignored")),
    text("  ")));

const places = (extra = {}) => ({
  version: 1, reading: 1, resumes: 0,
  sentenceColour: "rgba(1, 2, 3, 0.5)", wordColour: "rgba(4, 5, 6, 1)",
  sentence: [[0, 0, 4], [1, 0, 3]], word: [[1, 0, 3]], wordElement: null, ...extra,
});

test("it describes the page at load, walking ranged elements, passing others through and skipping scripts", () => {
  const page = loadPage({body: paragraph()});

  assert.deepEqual(page.description(), {
    version: 1,
    nodes: [["o", 0, 30], ["t", "One "], ["o", 4, 11], ["t", "two"], ["c"], ["t", " three."], ["t", "  "], ["c"]],
  });
  assert.deepEqual(page.subscribed, ["aloudrite.places"]);
});

test("it describes an empty body only after a patch", () => {
  const page = loadPage();

  assert.equal(page.posted.length, 0);
  page.document.body.childNodes.push(element("p", {[ATTRIBUTE]: "0..3"}, text("abc")));
  page.patch();
  assert.equal(page.description().version, 1);
});

test("each patch describes the page again with the next version and drops what was drawn", () => {
  const page = loadPage({body: paragraph()});
  page.send(places());
  assert.equal(page.highlights.size, 2);

  page.patch();

  assert.equal(page.description().version, 2);
  assert.equal(page.highlights.size, 0);
  assert.equal(page.sheets[0].rules, "");
});

test("it posts that the page holds no ranges once, and not for a page with ranges", () => {
  const without = loadPage({body: element("body", {}, element("p", {}, text("abc")))});
  without.patch();
  assert.equal(without.posted.filter(entry => entry.tag === "aloudrite.noRanges").length, 1);

  const noMeta = loadPage({body: paragraph(), withMeta: false});
  assert.equal(noMeta.posted.filter(entry => entry.tag === "aloudrite.noRanges").length, 1);

  const ranged = loadPage({body: paragraph()});
  ranged.patch();
  assert.equal(ranged.posted.filter(entry => entry.tag === "aloudrite.noRanges").length, 0);
});

test("a message of another version is dropped and one of the version drawn on the text nodes with the colours", () => {
  const page = loadPage({body: paragraph()});

  page.send(places({version: 7}));
  assert.equal(page.highlights.size, 0);

  page.send(places());
  const sentence = page.highlights.get("aloudrite-sentence");
  const word = page.highlights.get("aloudrite-word");
  assert.deepEqual(sentence.ranges.map(range => [range.startNode.data, range.start, range.end]), [["One ", 0, 4], ["two", 0, 3]]);
  assert.deepEqual(word.ranges.map(range => [range.startNode.data, range.start, range.end]), [["two", 0, 3]]);
  assert.ok(word.priority > sentence.priority);
  assert.match(page.sheets[0].rules, /::highlight\(aloudrite-sentence\) \{ background-color: rgba\(1, 2, 3, 0\.5\); \}/);
  assert.match(page.sheets[0].rules, /::highlight\(aloudrite-word\) \{ background-color: rgba\(4, 5, 6, 1\); \}/);
});

test("a colour that is not of the form the IDE writes gets no rule", () => {
  const page = loadPage({body: paragraph()});

  page.send(places({sentenceColour: "red; } body { display: none", wordColour: null}));

  assert.equal(page.sheets[0].rules, "");
});

test("a piece that the text node cannot hold is not drawn", () => {
  const page = loadPage({body: paragraph()});

  page.send(places({sentence: [[1, 0, 99], [42, 0, 1]], word: []}));

  assert.equal(page.highlights.has("aloudrite-sentence"), false);
});

test("the clearing message removes the highlights and the rules whatever the version", () => {
  const page = loadPage({body: paragraph()});
  page.send(places());

  page.send({clear: true});

  assert.equal(page.highlights.size, 0);
  assert.equal(page.sheets[0].rules, "");
});

test("a word shown in no text outlines its element, in the colour of the word", () => {
  const page = loadPage({body: element("body", {}, element("p", {[ATTRIBUTE]: "0..16"}, element("img", {[ATTRIBUTE]: "0..16"})))});
  ElementNode.rect = {top: 100, bottom: 120, height: 20, width: 40};

  page.send(places({sentence: [], word: [], wordElement: [0, 16]}));

  assert.match(page.sheets[0].rules, /\[md-pos="0\.\.16"\] \{ outline: 2px solid rgba\(4, 5, 6, 1\); outline-offset: 2px; \}/);
});

test("it draws nothing in the page: no element is added, removed or given an attribute, and no text changes", () => {
  const body = paragraph();
  const snapshot = JSON.stringify(body);
  const page = loadPage({body});

  page.send(places());
  page.send(places({wordElement: [0, 30]}));
  page.send({clear: true});

  assert.equal(JSON.stringify(body), snapshot);
});

test("it follows from the load and scrolls the word that is out of view to the middle of the window", () => {
  const page = loadPage({body: paragraph()});

  page.send(places());

  assert.equal(page.scrolls.length, 1);
  assert.equal(page.scrolls[0].behavior, "instant");
  assert.equal(page.scrolls[0].top, 0 + 5000 - (800 - 20) / 2);
});

test("it does not scroll a word that is in view", () => {
  const page = loadPage({body: paragraph()});
  page.rect = {top: 100, bottom: 120, height: 20, width: 40};

  page.send(places());

  assert.equal(page.scrolls.length, 0);
});

const sentenceOnly = (extra = {}) => places({word: [], wordElement: null, ...extra});

test("a message with the sentence and no word scrolls the sentence that is out of view once to the middle of the window", () => {
  const page = loadPage({body: paragraph()});

  page.send(sentenceOnly());

  assert.equal(page.scrolls.length, 1);
  assert.equal(page.scrolls[0].behavior, "instant");
  assert.equal(page.scrolls[0].top, 0 + 5000 - (800 - 20) / 2);
});

test("a message with the sentence and no word does not scroll a sentence that is in view", () => {
  const page = loadPage({body: paragraph()});
  page.rect = {top: 100, bottom: 120, height: 20, width: 40};

  page.send(sentenceOnly());

  assert.equal(page.scrolls.length, 0);
});

test("a sentence taller than the window is scrolled to have its top at the top of the window", () => {
  const page = loadPage({body: paragraph()});
  page.rect = {top: 5000, bottom: 6000, height: 1000, width: 40};

  page.send(sentenceOnly());

  assert.equal(page.scrolls.length, 1);
  assert.equal(page.scrolls[0].top, 5000);
});

test("a message with a word and a sentence scrolls to the word and not to the sentence", () => {
  const page = loadPage({body: paragraph()});

  page.send(places());

  assert.equal(page.scrolls.length, 1);
});

test("a message with neither the word nor the sentence does not scroll", () => {
  const page = loadPage({body: paragraph()});

  page.send(places({sentence: [], word: []}));

  assert.equal(page.scrolls.length, 0);
});

test("a turn of the wheel stops the following of the sentence as it stops that of the word", () => {
  const page = loadPage({body: paragraph()});
  page.send(sentenceOnly());
  page.fire("wheel");

  page.send(sentenceOnly({sentence: [[0, 0, 4]]}));

  assert.equal(page.scrolls.length, 1);
});

test("a turn of the wheel stops the following until the next resume or the next reading", () => {
  const page = loadPage({body: paragraph()});
  page.send(places());
  page.fire("wheel");

  page.send(places({word: [[0, 0, 3]]}));
  assert.equal(page.scrolls.length, 1);

  page.send(places({resumes: 1}));
  assert.equal(page.scrolls.length, 2);

  page.fire("wheel");
  page.send(places({resumes: 1, reading: 2}));
  assert.equal(page.scrolls.length, 3);
});

test("the keys that scroll stop the following, and the others do not", () => {
  for (const key of ["PageUp", "PageDown", "Home", "End", "ArrowUp", "ArrowDown", " "]) {
    const page = loadPage({body: paragraph()});
    page.send(places());
    page.fire("keydown", {key});
    page.send(places({word: [[0, 0, 3]]}));
    assert.equal(page.scrolls.length, 1, key);
  }
  const page = loadPage({body: paragraph()});
  page.send(places());
  page.fire("keydown", {key: "a"});
  page.send(places({word: [[0, 0, 3]]}));
  assert.equal(page.scrolls.length, 2);
});

test("a scroll while a pointer is down stops the following, and one without a pointer does not", () => {
  const page = loadPage({body: paragraph()});
  page.fire("scroll");
  page.send(places());
  assert.equal(page.scrolls.length, 1);

  page.fire("pointerdown");
  page.fire("scroll");
  page.send(places({word: [[0, 0, 3]]}));
  assert.equal(page.scrolls.length, 1);

  page.fire("pointerup");
  page.send(places({resumes: 1}));
  assert.equal(page.scrolls.length, 2);
});

test("it does nothing and throws nothing where the pipe or the highlights of the browser are missing", () => {
  const withoutPipe = loadPage({body: paragraph(), withPipe: false});
  assert.equal(withoutPipe.loaded, false);
  assert.deepEqual(withoutPipe.subscribed, []);

  const withoutHighlights = loadPage({body: paragraph(), withHighlights: false});
  assert.equal(withoutHighlights.loaded, false);
  assert.equal(withoutHighlights.posted.length, 0);
});
