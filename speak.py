#!/usr/bin/env python3
import sys
import argparse
import sounddevice as sd
from kokoro import KPipeline

DEFAULT_VOICE = "af_sarah"

# 1. Setup Argument Parser
parser = argparse.ArgumentParser(
    description="Read text aloud using Kokoro TTS."
)
parser.add_argument(
    "-v", "--voice",
    type=str,
    default=DEFAULT_VOICE,
    help=f"Voice ID to use (default: {DEFAULT_VOICE})"
)
parser.add_argument(
    "-s", "--speed",
    type=float,
    default=1.0,
    help="Playback speed multiplier (default: 1.0)"
)
parser.add_argument(
    "text",
    nargs="*",
    help="Text to speak. If omitted, text is read from stdin."
)

args = parser.parse_args()

# 2. Get Text (stdin pipe OR positional arguments)
if args.text:
    input_text = " ".join(args.text).strip()
elif not sys.stdin.isatty():
    input_text = sys.stdin.read().strip()
else:
    input_text = ""

if not input_text:
    print("Error: No text provided to speak.")
    sys.exit(1)

# 3. Determine Language Code based on Voice Prefix ('a' for US, 'b' for UK)
voice_name = args.voice.lower()
lang_code = "b" if voice_name.startswith("b") else "a"

print(f"Using voice: '{args.voice}' (Lang: {lang_code}) | Speed: {args.speed}")

# 4. Initialize Pipeline and Speak
try:
    pipeline = KPipeline(lang_code=lang_code)
    generator = pipeline(input_text, voice=args.voice, speed=args.speed, split_pattern=r"\n+")

    for _, _, audio in generator:
        sd.play(audio, 24000)
        sd.wait()

except Exception as e:
    print(f"Error playing voice '{args.voice}': {e}")
    sys.exit(1)
