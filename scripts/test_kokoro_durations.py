# Tests of kokoro-durations.py with a stand-in model, so that they run without torch, kokoro or the weights:
# python3 -I -m unittest discover -s scripts -p 'test_*.py'
import importlib.util
import sys
import types
import unittest
from pathlib import Path

sys.modules.setdefault("torch", types.ModuleType("torch"))
kokoro_stand_in = types.ModuleType("kokoro")
kokoro_stand_in.KModel = object
sys.modules.setdefault("kokoro", kokoro_stand_in)

specification = importlib.util.spec_from_file_location("kokoro_durations", Path(__file__).with_name("kokoro-durations.py"))
kokoro_durations = importlib.util.module_from_spec(specification)
specification.loader.exec_module(kokoro_durations)


class Frames:
    def __init__(self, frames):
        self.frames = frames

    def tolist(self):
        return self.frames


class StandInModel:
    def __init__(self):
        self.calls = []

    def __call__(self, phonemes, style, speed, return_output):
        self.calls.append((phonemes, style, speed, return_output))
        return types.SimpleNamespace(pred_dur=Frames([1, len(phonemes), 3.0]))


class DurationsTest(unittest.TestCase):
    def test_a_line_gives_its_durations_as_integers_separated_by_spaces(self):
        self.assertEqual("1 2 3", kokoro_durations.durations_of(StandInModel(), ["a", "b", "c"], "1.0\tab\n"))

    def test_the_model_is_given_the_phonemes_the_speed_and_the_style_of_their_length(self):
        model = StandInModel()
        voice_pack = ["style0", "style1", "style2"]

        kokoro_durations.durations_of(model, voice_pack, "1.5\tabc")
        kokoro_durations.durations_of(model, voice_pack, "1.0\ta")

        self.assertEqual([("abc", "style2", 1.5, True), ("a", "style0", 1.0, True)], model.calls)

    def test_the_phonemes_keep_their_spaces_and_only_the_first_tab_splits_the_line(self):
        model = StandInModel()

        kokoro_durations.durations_of(model, ["x"] * 10, "1.0\ta b\tc")

        self.assertEqual("a b\tc", model.calls[0][0])


if __name__ == "__main__":
    unittest.main()
