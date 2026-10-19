# Engine threads from the machine

## Context

The speech engine, sherpa-onnx on ONNX Runtime, splits the work of making audio over a number of threads that the
plugin gives it when it creates the engine. Since unit 0004 that number is fixed at 2. It was set in the plan of unit
0004, with the reason that it leaves the rest of the machine to the IDE, and never in a spec, so it was never agreed.

Two observations during the checks of unit 0006 bring it up:

- The test suite kept one processor core busy and took 208 seconds, of which 200 went to one test that makes long
  pieces of audio with the real engine. That test has since been taken out of the normal run, but it showed that the
  engine works on very little of a machine of 16 cores and 32 threads.
- While the plugin read a file aloud on Linux, the fans of the machine went up and the processor went from about 50 to
  75 degrees Celsius. Using more threads makes the audio sooner, but also uses more of the machine while it does.

A fixed number is wrong at both ends. On a machine of 4 logical processors, 2 threads is half of it; on one of 32, it
is a sixteenth. Programs of this kind usually work the number out from the machine and cap it: ONNX Runtime, left to
choose, takes the number of physical cores; llama.cpp does the same; whisper.cpp, a small local speech model like
this one, takes the number of hardware threads with a maximum of 4.

The measurements of the plan of unit 0004, on a plain JVM, already show that more threads help less and less. The
second piece of the test sentence arrived after 2.14 s with 1 thread, 1.66 s with 2 and 1.50 s with 4.

The developer's machines are a Linux x64 machine with an AMD Ryzen 9 9950X3D, 16 cores and 32 threads, and a Mac on
Apple Silicon.

## Requirements

1. The engine is created with a number of threads equal to the number of processors available to the JVM, divided by
   two and rounded down, but never fewer than 1 and never more than 6.
2. The number of processors is the one the JVM reports at the moment the engine is created, so that a limit on the
   processors of the IDE's process, such as that of a container, is respected.
3. Each time the engine is created, the IDE's log holds one line at level INFO that gives the number of processors
   available and the number of threads the engine was created with.
4. Automated tests cover the rule of requirement 1 with plain numbers, at the least for 1, 2, 3, 8, 9, 12, 13 and 32
   processors, giving 1, 1, 1, 4, 4, 6, 6 and 6 threads. They need no model, no audio device and no network.
5. The developer has measured, on each of the two machines, with the engine created with 1, 2, 4, 6 and 8 threads:
   the time from asking for a short sentence to its first piece of audio, and, for a long passage of prose, the
   seconds of audio made per second of work. The record holds the measurements, with the number of processors each
   machine reports to the JVM.
6. The developer has listened to a reading on each of the two machines with the plugin built from this unit, and the
   record holds the number of threads the log line of requirement 3 gave and what the developer found of the time to
   the first sound and of the load on the machine, compared with 2 threads.
7. `README.md` says how many threads the engine uses while it makes audio, and that the number follows from the
   machine.
8. Everything else of the engine is as it was: the voice, the speed, and the sound of what is read.

## Decisions

- **The number follows from the machine.** A fixed number gives a small machine too large a part and a large machine
  too small a part. Rejected: a fixed number higher than 2, which has the same problem at a different place.
- **Half the logical processors stands for the physical cores.** The work of the engine is matrix arithmetic, which
  gains little from a second hardware thread on the same core, and ONNX Runtime itself chooses physical cores for that
  reason. Java has no portable way to read the number of physical cores, and on a machine with two threads per core,
  as the developer's Linux machine, half the logical processors is that number. Apple Silicon has one thread per
  core, so there it gives half the cores, which leaves the other half to the IDE. Rejected: all logical processors,
  which runs threads that compete with each other and with the IDE for no gain; reading the physical cores from the
  operating system, which is code per platform for a number the cap of 6 mostly overrides anyway (see "Beyond the
  request").
- **A cap of 6.** The human set it from the measurements of requirement 5, in place of the 4 that the specifier had
  proposed. The plugin is a guest in the IDE, and a reading is not a batch job: once the engine makes audio faster
  than it is played, which unit 0006 relies on, more threads only shorten the time to the first sound and make the
  load on the machine heavier while the engine works ahead. On the Linux machine, of 32 processors, the gain stops at
  about 6 threads: from 4 to 6 the first audio came 62 ms sooner, from 6 to 8 only 4 ms. On the Mac, of 16
  processors, the first audio came 112 ms sooner from 4 to 6 and no sooner from 6 to 8. Both machines get 6 threads
  from the rule. Rejected: a cap of 4, as whisper.cpp has, which leaves the gain from 4 to 6 on both machines; and a
  cap of 8, whose small rise in audio per second of work on the Mac lies within what one run per number can show.
- **A floor of 1.** A machine of 1 to 3 processors still reads, on 1 thread. Its audio comes later, as it did with 1
  thread in unit 0004, and that is better than taking the whole machine from the IDE.
- **The number is logged and not shown.** The log line is what tells, on any machine, what the plugin chose, without
  a setting or a dialog that a user has to understand.

## Non-goals

- Making the test suite faster. The slow test that brought this up is already out of the normal run, and the tests
  that use the real engine get the number of requirement 1 like the plugin does.
- Running test classes in parallel.
- A setting to choose the number of threads. See "Beyond the request".
- The priority of the engine's threads. They are native threads of ONNX Runtime, which the plugin cannot reprioritise
  from Java.
- How far ahead of the playing the engine works, and how hard it works while it does. That is the 30 seconds of unit
  0006, and the fans the human heard are partly that: the engine runs at full speed until it is that far ahead.

## Beyond the request

- **A setting for the number of threads.** It would let a user on an unusual machine choose more or fewer. It costs a
  settings page, its storage and its tests. Without it, a user lives with the number of requirement 1, which on any
  machine is between 1 and 6 and never more than half of it, and the log line shows what it is.
- **A system property that overrides the number.** It would let the developer try another number in the sandbox IDE
  without building the plugin again. It costs a property, its parsing and a test. Without it, the measurements of
  requirement 5 are made with the engine created directly, which needs no plugin build either.
- **Reading the number of physical cores from the operating system.** It would be exact where the halving is not,
  such as on a machine of one thread per core with more than 12 cores. It costs code per platform, from
  `/proc/cpuinfo` on Linux to `sysctl` on macOS, with a fallback for each failure. Without it, such a machine gets 6
  threads where it could have had 6 anyway, because the cap holds.
