---
title: Markdown features
date: 2026-10-19
tags:
  - listening
  - markdown
---

# Markdown features

This file is for listening to AloudRite. Each kind of Markdown comes first, in a short section of its own under a
heading that names it. Two long chapters of plain prose follow at the end, to listen to a long reading and to stop one.
The front matter above the first heading, its title, date and tags, is not to be heard.

## Heading levels

### A heading of the third level

#### A heading of the fourth level

## Bold and italic

This sentence has a **bold** word, an *italic* word, a __bold__ word with underscores, an _italic_ word with
underscores, and a ***bold and italic*** word.

## Strikethrough

This sentence has a ~~struck out~~ word.

## Bulleted list

- A brass key for the lamp room
- Two candles and a box of matches
- The logbook, wrapped in oilcloth

## Bulleted list with full stops

- The door was locked from the inside.
- The lamp was still warm.
- Nobody answered the bell.

## Numbered list

1. Climb the stairs and unlock the lamp room.
2. Check the oil and trim the wick.
3. Light the lamp and wind the clockwork.

## Numbered list without full stops

1. Climb the stairs and unlock the lamp room
2. Check the oil and trim the wick
3. Light the lamp and wind the clockwork

## Nested list

- Food
  - Bread for two days
  - Three tins of soup
- Light
  - Oil for a week
  - The candles, unused

## Task list

- [x] Light the lamp
- [ ] Write in the logbook

## Link

The history of the station is in [the harbor archive](https://example.com/archive/lost-boats?coast=west&years=1900-1950).

## Bare address

The records office is at https://example.org/records and nowhere else.

## Link in angle brackets

Write to <https://example.org/letters> for a copy.

## Reference link

The answer was in [the old register][register], which he had read twice.

[register]: https://example.net/register/volume-3

## Image

![A drawing of the boat on the shingle](boat.png)

## Inline code

Call `recordFoundBoat` once for every boat, and never change the `date` by hand.

## Inline code that is code

Set `config.timeout = 30` before you call `boats.map { it.name }`, and check that `count >= 1 && name != null`. The
logbook is kept in `src/main/kotlin/Logbook.kt`, and `Logbook.write("lamp lit")` adds a line to it.

## Code block

```kotlin
fun recordFoundBoat(name: String, place: String): String {
    val date = java.time.LocalDate.now()
    return "Found the boat $name at $place on $date"
}
```

## Quote

> The sea does not care whether you are watching it. That is its kindness.

## Table

| Part | What he found                     | State |
|------|-----------------------------------|-------|
| Hull | Painted blue, with a white stripe | Sound |
| Oars | Two, tied along the seats         | Dry   |

## Horizontal rule

Before the rule.

---

After the rule.

## Emoji of four bytes

The sun went down 🌅 behind the tower 🗼 and the waves 🌊 came in.

## Symbols of three bytes

The anchor ⚓ held, the list was done ✓, and he loved ❤ the sea.

## Escaped characters

This sentence has a \*star\* that is not bold, and a \# that is not a heading.

## HTML comments

The paragraph before the comment.

<!-- A note for whoever edits this file, which a listener does not hear. -->

The paragraph after the comment, which has <!-- another note --> a comment within it.

## Chapter 1: The tower

The lighthouse stood at the end of a narrow spit of rock, and for most of the year nobody went near it. The keeper
came twice a month with a boat full of supplies, checked the lamp, and wrote a few lines in a book that nobody else
would ever read.

In the spring the birds returned. They nested in the cracks of the old stone wall, and their noise filled the mornings
until the keeper could hardly hear the sea. He did not mind. He had grown used to the company of things that did not
ask questions, and he liked the way the gulls argued over the scraps he threw from the landing.

The tower itself was older than anyone could say for certain. The harbor office kept a drawing of it from the year it
was rebuilt, but the stones at its foot were darker and rougher than the rest, and the keeper believed that an older
tower had stood there before, lit by a fire of driftwood on the top of the rock.

One evening a storm came in from the west, faster than the radio had said it would. The waves climbed the rocks and
threw white spray against the windows, and the lamp turned through the dark as it had done for a hundred years.

The keeper sat at the small table in the room below the lamp and listened to the wind. He had seen many storms, and
most of them had passed in a night. This one did not. By morning the landing was under water, and the boat that should
have come for him that day stayed in the harbor, as it was wise to do.

## Chapter 2: The boat

When the weather cleared, there was a small wooden boat on the shingle below the tower. It was empty, its oars tied
neatly along the seats, and on the bow someone had painted a name that the keeper did not recognize.

He searched the island that afternoon, calling out now and then, though he expected no answer. By the time the sun went
down he had found nothing but a single glove, wet and heavy, caught between two stones near the landing.

Over the following weeks he wrote to the harbor office, to the coast guard, and to a museum that kept records of old
fishing boats. Each letter took days to arrive and longer to be answered, and the answers said very little.

It was the birds who solved it, in a way. In the autumn they left the tower, and in the quiet that followed the keeper
noticed that the paint on the bow of the boat had begun to flake. Under the name he could not read, there was an older
one, and that one he knew.

It was the name of the boat that had brought the first keeper to the island, a hundred years before. Someone had kept
it all that time, repainted it, renamed it, and in the end sent it back to the rock where it had started.

The keeper did not write to the museum about it. He wrote one line in the logbook, under the date and the time he had
lit the lamp, and then he went up the stairs to wind the clockwork, as he did every evening at sunset.
