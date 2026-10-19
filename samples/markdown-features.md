# The keeper of the light

This is a short story for listening to AloudRite. It is written to hold every kind of Markdown the plugin may meet:
headings, **bold** and *italic* words, ~~struck out~~ words, lists, links, code, quotes, a table and a few emoji. Read
it from the beginning to the end, and listen to how each of them sounds.

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

### What the keeper carried

- A brass key for the lamp room
- Two candles and a box of matches
- The logbook, wrapped in oilcloth
- A tin of tea that was always nearly empty

### Lighting the lamp

1. Climb the stairs and unlock the lamp room.
2. Check the oil and trim the wick.
3. Light the lamp and wind the clockwork.
4. Write the time in the logbook.

## Chapter 2: The storm

One evening a storm came in from the west, faster than the radio had said it would. The waves climbed the rocks and
threw white spray against the windows, and the lamp turned through the dark as it had done for a hundred years.

The keeper sat at the small table in the room below the lamp and listened to the wind. He had seen many storms, and
most of them had passed in a night. This one did not. By morning the landing was under water, and the boat that should
have come for him that day stayed in the harbor, as it was ~~supposed~~ *wise* to do.

He made a list of what he had left, because that was what keepers did when the weather turned:

- Food
  - Bread for two days
  - Three tins of soup
  - The last of the tea
- Light
  - Oil for a week
  - The candles, unused
- Company
  - The birds, who did not seem to mind the storm at all

> The sea does not care whether you are watching it. That is its kindness. 🌊
>
> — written in the margin of the logbook, in a hand that was not the keeper's

## Chapter 3: The boat

When the weather cleared, there was a small wooden boat on the shingle below the tower. It was empty, its oars tied
neatly along the seats, and on the bow someone had painted a name that the keeper did not recognize. ⚓

He searched the island that afternoon, calling out now and then, though he expected no answer. By the time the sun went
down he had found nothing but a single glove, wet and heavy, caught between two stones near the landing.

The keeper wrote down everything he could see of the boat, the way he had been taught to write down a ship that passed
in the night:

| Part      | What he found                        | State   |
|-----------|--------------------------------------|---------|
| Hull      | Painted blue, with a white stripe    | Sound   |
| Oars      | Two, tied along the seats            | Dry     |
| Name      | Painted on the bow, half worn away   | Faded   |
| Contents  | One coil of rope and a tin cup       | Wet     |

He drew the name as well as he could, letter by letter, so that someone else might read it where he could not.

![A drawing of the boat on the shingle](boat.png)

## Chapter 4: The letters

Over the following weeks he wrote to the harbor office, to the coast guard, and to a museum that kept records of old
fishing boats. Each letter took days to arrive and longer to be answered, and the answers said very little.

The museum sent him a printed list of boats that had been lost along that coast, and a note that more could be found
in [the harbor archive](https://example.com/archive/lost-boats?coast=west&years=1900-1950). The coast guard wrote that
the boat was not one of theirs, and gave the address of their records office, which was simply
https://example.org/records. The harbor office sent nothing at all for a month, and then a short note that pointed him
to [the old register][register], which he had already read twice.

[register]: https://example.net/register/volume-3

In the end he copied the procedure for recording a found boat into the margin of the logbook, as the coast guard had
asked. They had written it as a small program, which he did not understand, but he copied it carefully anyway:

```kotlin
fun recordFoundBoat(name: String, place: String): String {
    val date = java.time.LocalDate.now()
    return "Found the boat $name at $place on $date"
}
```

The note that came with it said to call `recordFoundBoat` once for every boat, and never to change the `date` by hand.

## Chapter 5: The name

It was the birds who solved it, in a way. In the autumn they left the tower, and in the quiet that followed the keeper
noticed that the paint on the bow of the boat had begun to flake. Under the name he could not read, there was an older
one, and that one he knew. ✓

It was the name of the boat that had brought the first keeper to the island, a hundred years before. Someone had kept
it all that time, repainted it, renamed it, and in the end sent it back to the rock where it had started. ❤

The keeper did not write to the museum about it. He wrote one line in the logbook, under the date and the time he had
lit the lamp, and then he went up the stairs to wind the clockwork, as he did every evening at sunset. 🌅 🗼

**The end.**
