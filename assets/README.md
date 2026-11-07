# AloudRite brand assets

## Logo

| File | Use |
|------|-----|
| `logo.svg` | The speaker, on dark backgrounds |
| `logo-light.svg` | The speaker, on light backgrounds |
| `icon.svg` | The speaker at 32 pixels and below, such as the browser tab icon: thicker lines |
| `lockup.svg` | The speaker with the name, on dark backgrounds |
| `lockup-light.svg` | The speaker with the name, on light backgrounds |

The lockups carry their font inside the file, so they render the same wherever they are shown. To pick the version
that fits the reader's theme, such as in a README on GitHub:

```html
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="lockup.svg">
  <img src="lockup-light.svg" alt="AloudRite" width="228">
</picture>
```

## Colours

AloudRite shares the palette of AgentRite (agentrite.dev), so the two read as one family and are told apart by the
shape of their marks. The speaker is drawn in grey-blue and its voice in gold.

| Role | Dark background | Light background |
|------|-----------------|------------------|
| Ground, graphite | `#1B1D21` | `#FFFFFF` |
| Raised surfaces, such as panels and code blocks | `#24272C` | |
| Borders | `#33373E` | |
| Text | `#E4E7EB` | `#1B1D21` |
| Secondary text | `#9AA1AB` | |
| The speaker and the name's chip | `#8FA6BF` | `#4F6680` |
| The voice: the sound waves, the primary button | `#E8B84A` | `#E8B84A` |
| Errors, cautions and findings as text, and accents that are not buttons | `#D98A9F` | |
| Errors and cautions as a filled background | `#8C2A44` | |

Text on the chip is the ground colour on dark backgrounds and white on light ones. Text meets a contrast ratio of at
least 4.5:1 against its background.

## Fonts

* [JetBrains Mono](https://www.jetbrains.com/lp/mono/), weights 500 and 800: headings, the name, code and terminal
  text. In the name, "aloud" is 500 and "rite" is 800
* [IBM Plex Sans](https://github.com/IBM/plex), weights 400 and 600: running text

Both are under the SIL Open Font License.
