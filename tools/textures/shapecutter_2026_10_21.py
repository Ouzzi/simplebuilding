"""Shapecutter textures (Simple Blocks, principle A4b; owner 21.10.): the Vanilla stonecutter with a polished metal base.

Owner 21.10.: "die textur einfach nur die stein base aus metall. also die blade wieder vanilla und dann etwas
glenzender/polierter". So, from the Vanilla stonecutter (docs/ai/PLAN-B-SHAPECUTTER-2026-10-21.md, decision 4):
  - every stone grey of top, side and bottom becomes polished iron: a cool, light steel ramp whose mid tones lie closer
    together than the stone's (less noise = polished);
  - the brass corner caps of the top become copper (the recipe's two copper ingots); the wooden frame stays Vanilla;
  - on top of that the own gloss layers (tools/textures/shapecutter/gloss_<face>.layers.json: "Glanzkanten",
    "Schattenkante", "Highlights"; own pixels, editable in the wiki pixel editor) add the shiny edges and highlights;
  - the blade (owner 21.10. evening: "sägeblatt etwas dunkler in einer variante und in der anderen heller bzw polierter")
    is Vanilla's stonecutter_saw strip, regreyed per frame: S1 a little darker, S2 lighter/polished (brighter glints,
    more contrast at the teeth). BLADE picks the built-in one (S2); the animation (.mcmeta, own text) stays Vanilla's.

Vanilla-derived (AGENTS.md rule 7): built from the client jar, never committed. Listed in
vanilla_derived.MODULE_OUTPUTS['simpleblocks'] (generator derived(jar)) and in .gitignore.

  python3.12 tools/textures/shapecutter_2026_10_21.py --jar <client.jar>   # previews to SB_PREVIEW_DIR (never committed)
  python3.12 tools/textures/shapecutter_2026_10_21.py --jar <client.jar> --blade s1   # blade variant S1 (preview)
"""
import argparse
import io
import json
import os
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402

PREVIEW = Path(os.environ.get('SB_PREVIEW_DIR', '/tmp/claude-0/shapecutter'))
SRC = 'assets/minecraft/textures/block/'
GLOSS = HERE / 'shapecutter'
# Polished iron (Vanilla iron block family, a touch of blue): stone grey -> steel, anchors (stone value, steel colour).
# 0x53/0x5f are the blade slot, 0x68..0x8f the stone; 0x74 and 0x7f move together (smoother = polished).
IRON = [(0x40, (0x2c, 0x30, 0x36)), (0x53, (0x3c, 0x41, 0x48)), (0x5f, (0x4e, 0x53, 0x5b)), (0x68, (0x94, 0x9a, 0xa2)),
        (0x74, (0xa6, 0xac, 0xb3)), (0x7f, (0xad, 0xb3, 0xba)), (0x8f, (0xbb, 0xc0, 0xc6)), (0xb0, (0xe0, 0xe3, 0xe7))]
# Blade variants (grey value of the Vanilla strip -> new grey, a touch of the base's cool steel). Vanilla uses
# 0x72/0x82 (hub), 0xa8 (teeth), 0xd8 (body), 0xea/0xff (glints).
BLADES = {
    's1': [(0x72, (0x5c, 0x60, 0x66)), (0x82, (0x6a, 0x6e, 0x75)), (0xa8, (0x8c, 0x90, 0x97)), (0xd8, (0xb4, 0xb8, 0xbe)),
           (0xea, (0xc6, 0xca, 0xd0)), (0xff, (0xda, 0xde, 0xe3))],
    's2': [(0x72, (0x56, 0x5a, 0x61)), (0x82, (0x68, 0x6c, 0x74)), (0xa8, (0x84, 0x89, 0x91)), (0xd8, (0xdc, 0xe0, 0xe5)),
           (0xea, (0xf6, 0xf8, 0xfa)), (0xff, (0xff, 0xff, 0xff))],
}
# Built-in blade variant (owner choice pending: S1 darker, S2 polished; coordinator 21.10.: S2 built in).
BLADE = 's2'
# Corner caps: brass -> copper (dark -> light), Vanilla copper block family.
COPPER = [(0x6d, 0x34, 0x22), (0x8c, 0x46, 0x30), (0xa8, 0x58, 0x3e), (0xc2, 0x6b, 0x4c), (0xd6, 0x7b, 0x5b)]


def _lum(p):
    return 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]


def _is_grey(p):
    return p[3] and abs(p[0] - p[1]) <= 6 and abs(p[1] - p[2]) <= 6


def _is_brass(p):
    """The yellowish brass of the stonecutter's corner caps (red > green > blue clearly)."""
    return p[3] and p[0] - p[2] > 60 and p[1] - p[2] > 25


def _ramp(v, anchors):
    """Piecewise linear: grey value v -> colour between the anchors (clamped at the ends)."""
    if v <= anchors[0][0]:
        return anchors[0][1]
    for (v0, c0), (v1, c1) in zip(anchors, anchors[1:]):
        if v <= v1:
            t = (v - v0) / (v1 - v0)
            return tuple(round(c0[i] + (c1[i] - c0[i]) * t) for i in range(3))
    return anchors[-1][1]


def metal(src, anchors=IRON):
    """Every grey (stone) pixel of a Vanilla texture onto the metal ramp; caps to copper; everything else unchanged."""
    im = src.convert('RGBA').copy()
    brass = [im.getpixel((x, y)) for y in range(im.height) for x in range(im.width) if _is_brass(im.getpixel((x, y)))]
    lo, hi = (min(map(_lum, brass)), max(map(_lum, brass))) if brass else (0, 1)
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if _is_grey(p):
                im.putpixel((x, y), _ramp(p[0], anchors) + (p[3],))
            elif _is_brass(p):
                t = 0 if hi <= lo else (_lum(p) - lo) / (hi - lo)
                im.putpixel((x, y), COPPER[max(0, min(len(COPPER) - 1, round(t * (len(COPPER) - 1))))] + (p[3],))
    return im


def gloss(face):
    """The own gloss layers of a face (tools/textures/shapecutter/gloss_<face>.layers.json), composed, or None."""
    path = GLOSS / f'gloss_{face}.layers.json'
    if not path.exists():
        return None
    doc = json.loads(path.read_text(encoding='utf-8'))
    return Image.frombytes('RGBA', (doc['width'], doc['height']), bytes(layers.compose(doc)))


def polish(im, face):
    """Metal face + its gloss overlay (alpha over)."""
    over = gloss(face)
    return im if over is None else Image.alpha_composite(im, over)


def blade(src, variant=BLADE):
    """The Vanilla blade strip (all animation frames) regreyed onto the variant's ramp; alpha unchanged."""
    im = src.convert('RGBA').copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3]:
                im.putpixel((x, y), _ramp(p[0], BLADES[variant]) + (p[3],))
    return im


def build(read, blade_variant=BLADE):
    """read(name) -> Image of a Vanilla block texture. Returns texture path -> image."""
    out = {f'block/shapecutter_{face}.png': polish(metal(read(f'stonecutter_{face}.png'), IRON), face)
           for face in ('top', 'side', 'bottom')}
    out['block/shapecutter_saw.png'] = blade(read('stonecutter_saw.png'), blade_variant)
    return out


def derived(jar):
    """Vanilla-derived output (vanilla_derived.py, MODULE_OUTPUTS['simpleblocks']): path -> image."""
    with zipfile.ZipFile(jar) as z:
        return build(lambda name: Image.open(io.BytesIO(z.read(SRC + name))).convert('RGBA'))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--jar', default=os.environ.get('SB_CLIENT_JAR'))
    ap.add_argument('--dir', help='folder with the Vanilla block textures instead of a jar')
    ap.add_argument('--blade', choices=sorted(BLADES), default=BLADE, help='blade variant (preview)')
    a = ap.parse_args()
    if a.dir:
        out = build(lambda name: Image.open(Path(a.dir) / name).convert('RGBA'), a.blade)
    else:
        with zipfile.ZipFile(a.jar) as z:
            out = build(lambda name: Image.open(io.BytesIO(z.read(SRC + name))).convert('RGBA'), a.blade)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    for path, im in out.items():
        im.save(PREVIEW / Path(path).name)
    print(f'shapecutter previews (Vanilla-derived, never commit) -> {PREVIEW}')


if __name__ == '__main__':
    main()
