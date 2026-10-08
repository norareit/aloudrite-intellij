# IntelliJ IDEA Plugin: Kokoro TTS Reader

## Project Vision
An automated, lightweight, high-quality Text-to-Speech (TTS) IntelliJ IDEA plugin that reads agentic coding documents and Markdown files aloud. It provides natural voice playback, interactive "karaoke-style" text highlighting in the active editor, and intelligent Markdown pre-processing to skip non-speakable elements like syntax markers and code blocks.

---

## Technical Architecture & Model Strategy

### Core Engine: Kokoro-82M (v1.0)
* **Model Choice:** Kokoro-82M for its exceptionally high-quality, natural neural voices at a fraction of typical model sizes.
* **Inference Runtime:** **ONNX Runtime (C++/Java API via JNI)** embedded directly within the Kotlin plugin.
* **Why Embedded ONNX instead of PyTorch / Python:**
  * **Lightweight:** Entire footprint is ~100 MB (ONNX weights + voice matrices) vs. 1.5 GB–3 GB for PyTorch/Python dependencies.
  * **Performance:** Sub-100ms startup latency and minimal RAM overhead (~150 MB).
  * **Zero-Setup UX:** Single-click plugin installation from the JetBrains Marketplace without requiring users to configure local Python virtual environments, system `espeak-ng`, or `PYTHONPATH`.

---

## Voice System & Multi-Voice Support

### 1. Vector/Embedding-Based Multi-Voice
* The core ONNX model (`kokoro-v1.0.onnx`) uses external voice tensors (e.g., stored in `voices.bin` or individual matrix files).
* Voices are dynamically selected by passing a voice matrix tensor into the ONNX session.

### 2. Supported Voice Presets
* **American English (`lang_code='a'`):**
  * `af_heart` (Warm, highly expressive — primary recommendation)
  * `af_sarah` (Clear, natural default)
  * `af_nicole` / `af_bella`
  * `am_michael` / `am_adam`
* **British English (`lang_code='b'`):**
  * `bf_emma` / `bm_george` / `bm_fable`

### 3. Voice Blending & Configuration
* Supports mathematical voice blending (e.g., $0.7 \times \text{Voice}_A + 0.3 \times \text{Voice}_B$) to create custom voice timbres.
* User-configurable via plugin settings (Voice ID, Speed multiplier, Fallback logic).

---

## Intelligent Markdown Pre-Processing & AST Parsing

To avoid robotic recitation of syntax markers, the plugin uses IntelliJ’s native AST parser (`com.intellij.markdown`) or a dedicated pre-processor before passing text to the TTS engine.

### 1. Header Handling
* **Problem:** Raw phonemizers pronounce `# Title` literally as "hash Title" or "number sign Title".
* **Solution:** Strip leading `#` header markers and feed only clean text strings to the speech generator.

### 2. Code Block Skipping
* **Problem:** Reading raw code aloud is noisy and unhelpful.
* **Fenced Code Blocks (` ``` `):** Skipped entirely and replaced with concise vocal cues like `"[see code block]"` or `"[see Kotlin code block]"`.
* **Inline Code (` `code` `):** Cleaned of backticks and read naturally if short, or replaced with `"[see inline code]"` if complex.

### 3. General Formatting Sanitization
* **Links (`[text](url)`):** Pronounces only the anchor `text`, dropping the URL.
* **Images / HTML:** Stripped entirely.
* **Bold / Italics / Blockquotes:** Formatting characters removed; text preserved.

---

## Visual UX: "Karaoke" Line Highlighting

Using the IntelliJ Platform SDK `MarkupModel` API, the editor provides real-time visual synchronization as audio plays:

### Implementation Details
* **Highlighting:** Uses `editor.markupModel.addRangeHighlighter()` with a subtle, translucent background overlay (e.g., soft semi-transparent gold) across the line or character offset currently being spoken.
* **Sync & Progression:** As the audio stream finishes a chunk/line, the active `RangeHighlighter` is removed and shifted to the next offset.
* **Threading:** Audio playback runs on background threads; UI highlight updates are strictly dispatched via `ApplicationManager.getApplication().invokeLater`.
* **Auto-Scroll & Caret Follow:** Automatically scrolls the editor viewport to keep the currently spoken line visible.
* **Gutter Indicator (Optional):** Displays a subtle play icon in the editor's left gutter next to the line being read.

---

## Development Roadmap
1. **Phase 1 (Proof-of-Concept):** Standalone Kotlin test invoking ONNX Runtime with `kokoro-v1.0.onnx` and `voices.bin` to play synthesized audio via Java Sound API.
2. **Phase 2 (AST & Cleaning):** Build `MarkdownASTVisitor` in Kotlin to extract speakable text nodes, strip headers, and insert `"[see code block]"` placeholders.
3. **Phase 3 (IDE UI & Highlighting):** Implement `MarkupModel` range highlighting synchronized with audio playback timestamps/events.
4. **Phase 4 (Packaging):** Bundle ONNX native libraries and model weights for JetBrains Marketplace distribution.
