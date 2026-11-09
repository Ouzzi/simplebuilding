"""Shared blend test vectors (b-texed16): tools/textures/layers.py and wiki/assets/pixeleditor-core.js compute the
same cases (wiki/tests/pixeleditor_core.cjs reads the same JSON) and must hit every byte.

    python3 tools/textures/tests/test_blend_vectors.py --write    regenerate blend_vectors.json (after a reviewed change)
"""
import json
import random
import sys
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
import layers as ly  # noqa: E402

VECTORS = HERE / "blend_vectors.json"

# hand-computed fix points (W3C formulas, rnd = floor(x + 0.5)): mode, backdrop, source, opacity, result
FIXED = [
    ("normal", [255, 0, 0, 255], [0, 0, 0, 255], 50, [127, 0, 0, 255]),
    ("multiply", [255, 128, 0, 255], [128, 128, 128, 255], 100, [128, 64, 0, 255]),
    ("screen", [0, 128, 255, 255], [128, 128, 128, 255], 100, [128, 192, 255, 255]),
    ("difference", [200, 50, 10, 255], [50, 200, 10, 255], 100, [150, 150, 0, 255]),
    ("add", [200, 100, 0, 255], [100, 100, 0, 255], 100, [255, 200, 0, 255]),
    ("subtract", [200, 100, 0, 255], [100, 150, 0, 255], 100, [100, 0, 0, 255]),
    ("darken", [200, 100, 0, 255], [100, 150, 0, 255], 100, [100, 100, 0, 255]),
    ("lighten", [200, 100, 0, 255], [100, 150, 0, 255], 100, [200, 150, 0, 255]),
    ("multiply", [0, 0, 0, 0], [10, 20, 30, 255], 100, [10, 20, 30, 255]),        # nothing below: source as is
    ("multiply", [255, 255, 255, 255], [0, 0, 0, 255], 50, [127, 127, 127, 255]),  # sa = rnd(127.5) = 128, 255*127/255
    ("luminosity", [255, 0, 0, 255], [128, 128, 128, 255], 100, [255, 74, 74, 255]),  # SetLum + ClipColor: .502+(.202-.502)*.498/.698
]


def one(mode, back, src, op):
    pal = sorted({"#%02x%02x%02x%02x" % tuple(back), "#%02x%02x%02x%02x" % tuple(src)})
    cell = lambda c: "%02x" % pal.index("#%02x%02x%02x%02x" % tuple(c)) if c[3] else ".."
    doc = {"format": "sb-layers", "version": 1, "width": 1, "height": 1, "digits": 2, "palette": pal, "layers": [
        {"name": "b", "visible": True, "opacity": 100, "blend": "normal", "rows": [cell(back)]},
        {"name": "s", "visible": True, "opacity": op, "blend": mode, "rows": [cell(src)]}]}
    return list(ly.compose(doc))


def make():
    rng = random.Random(16)
    edge = [0, 1, 63, 64, 127, 128, 191, 254, 255]
    cases = []
    for mode in ly.BLENDS:
        for _ in range(14):  # edge values
            b = [rng.choice(edge) for _ in range(3)] + [rng.choice([0, 1, 128, 255, 255])]
            s = [rng.choice(edge) for _ in range(3)] + [rng.choice([1, 128, 254, 255, 255])]
            cases.append((mode, b, s, rng.choice([1, 50, 99, 100, 100])))
        for _ in range(30):  # random
            b = [rng.randrange(256) for _ in range(3)] + [rng.choice([rng.randrange(256), 255])]
            s = [rng.randrange(256) for _ in range(3)] + [rng.choice([rng.randrange(1, 256), 255])]
            cases.append((mode, b, s, rng.choice([rng.randrange(1, 101), 100])))
    out = [{"mode": m, "back": b, "src": s, "opacity": op, "out": r} for m, b, s, op, r in FIXED]
    out += [{"mode": m, "back": b, "src": s, "opacity": op, "out": one(m, b, s, op)} for m, b, s, op in cases]
    return out


def dumps(vectors):
    return "[\n" + ",\n".join(json.dumps(v, separators=(",", ":")) for v in vectors) + "\n]\n"


class BlendVectors(unittest.TestCase):
    def test_fixed_points(self):
        for mode, b, s, op, want in FIXED:
            self.assertEqual(one(mode, b, s, op), want, (mode, b, s, op))

    def test_vectors_file_is_current(self):
        self.assertEqual(VECTORS.read_text(encoding="utf-8"), dumps(make()),
                         "blend_vectors.json differs; regenerate with --write after review")

    def test_every_vector(self):
        for v in json.loads(VECTORS.read_text(encoding="utf-8")):
            self.assertEqual(one(v["mode"], v["back"], v["src"], v["opacity"]), v["out"], v)

    def test_all_modes_covered_and_validate(self):
        modes = {v["mode"] for v in json.loads(VECTORS.read_text(encoding="utf-8"))}
        self.assertEqual(modes, set(ly.BLENDS))
        doc = {"format": "sb-layers", "version": 1, "width": 1, "height": 1, "digits": 2, "palette": ["#ff0000ff"],
               "layers": [{"name": "a", "visible": True, "opacity": 100, "blend": "dodge", "rows": ["00"]}]}
        self.assertTrue(any("unknown blend" in p for p in ly.validate(doc)))
        del doc["layers"][0]["blend"]   # missing = normal (old files)
        self.assertEqual(ly.validate(doc), [])


if __name__ == "__main__":
    if "--write" in sys.argv:
        VECTORS.write_text(dumps(make()), encoding="utf-8", newline="\n")
        print(f"{VECTORS}: {len(make())} vectors")
    else:
        unittest.main()
