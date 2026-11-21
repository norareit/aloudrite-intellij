# The durations that the Kokoro Python package gives for rows of phonemes, for HighlightFollowingMeasurement (unit 0032).
#
# Usage: kokoro-env/bin/python scripts/kokoro-durations.py <weights folder> <voice name>
#
# The weights folder holds config.json, kokoro-v1_0.pth and voices/<voice name>.pt of the Hugging Face repository
# hexgrad/Kokoro-82M, so nothing is downloaded. Each line of the input is a speed, a tab and the phonemes of one row, and
# each line of the output is the durations of that row as integers separated by spaces, one per token with the padding token
# at both ends, in frames of 600 samples at 24 kHz
import sys
from pathlib import Path

import torch
from kokoro import KModel


def main(weights_folder: Path, voice_name: str) -> None:
    model = KModel(
        config=str(weights_folder / "config.json"),
        model=str(weights_folder / "kokoro-v1_0.pth"),
    ).eval()
    voice_pack = torch.load(weights_folder / "voices" / f"{voice_name}.pt", weights_only=True)
    with torch.no_grad():
        for line in sys.stdin:
            speed_text, phonemes = line.rstrip("\n").split("\t", 1)
            # The style is the one that KPipeline takes, by the length of the phonemes
            style = voice_pack[len(phonemes) - 1]
            output = model(phonemes, style, float(speed_text), return_output=True)
            print(" ".join(str(int(frames)) for frames in output.pred_dur.tolist()), flush=True)


if __name__ == "__main__":
    main(Path(sys.argv[1]), sys.argv[2])
