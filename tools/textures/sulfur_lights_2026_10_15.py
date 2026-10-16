"""Sulfur torch, sulfur lantern and sulfur campfire textures (queue N58 / N67).

Owner N67: stay very close to Vanilla (torch, lantern, campfire), only the flame changes: the orange fire becomes the
yellow-green sulfur flame (the ramp of the sulfur fire, fires_2026_10_11.py). The PNGs are Vanilla-derived: every
flame pixel keeps the brightness rank it has in the Vanilla sprite, the iron frame / wood / stone stay Vanilla. They
are built from the client jar by tools/textures/vanilla_derived.py and never committed (AGENTS.md rule 7). Small
details of our own: sulfur crust specks on the torch stick, a sulfur-stained lantern base and campfire logs.
Committed here: only the .mcmeta files (Vanilla timing).

Run: python3.12 tools/textures/sulfur_lights_2026_10_15.py [--check] [--preview DIR]
  --check    compares the .mcmeta files only (the PNGs are built)
  --preview  8x sheet "Vanilla | new" (needs the client jar; do not commit, it contains Vanilla pixels)
"""
from __future__ import annotations

import io
import sys
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures'
sys.path.insert(0, str(Path(__file__).resolve().parent))

# Sulfur flame ramp (position by Vanilla brightness) = the sulfur fire ramp.
RAMP = [(0.00, (86, 84, 6)), (0.15, (104, 120, 10)), (0.40, (146, 180, 16)), (0.70, (190, 224, 34)),
        (0.92, (240, 246, 90)), (1.00, (252, 252, 168))]
LUM_RANGE = (0.30, 1.0)  # Vanilla flame brightness range (dark orange .. white)
EMBER = (151, 77, 45)  # glowing crack edge of the Vanilla lit campfire log


def vanilla(jar, name: str) -> Image.Image:
    with zipfile.ZipFile(jar) as z:
        return Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/{name}.png'))).convert('RGBA')


def sulfur(rgb) -> tuple:
    """Vanilla flame colour -> sulfur ramp colour with the same brightness rank."""
    lum = (0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]) / 255
    t = min(1.0, max(0.0, (lum - LUM_RANGE[0]) / (LUM_RANGE[1] - LUM_RANGE[0])))
    xs = [x for x, _ in RAMP]
    return tuple(int(round(float(np.interp(t, xs, [c[k] for _, c in RAMP])))) for k in range(3))


def recolor(im: Image.Image, is_flame) -> Image.Image:
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] and is_flame(x, y, p):
                out.putpixel((x, y), sulfur(p[:3]) + (255,))
    return out


def warm(x, y, p) -> bool:
    """Lantern glow: orange/yellow (the iron frame is blue-grey, blue > red)."""
    return p[0] > p[2] + 20


def blend(im: Image.Image, pts, t: float = 0.5) -> None:
    """Sulfur-stain a few pixels of the sprite: mix the Vanilla colour with the mid sulfur tone."""
    tone = sulfur((223, 162, 27))
    for x, y in pts:
        p = im.getpixel((x, y))
        im.putpixel((x, y), tuple(int(round(p[k] * (1 - t) + tone[k] * t)) for k in range(3)) + (255,))


def derived(jar) -> dict:
    """Vanilla-derived output for tools/textures/vanilla_derived.py: texture path -> image (never committed)."""
    torch = recolor(vanilla(jar, 'block/torch'), lambda x, y, p: y <= 7)
    blend(torch, [(8, 9), (7, 12), (8, 14)])  # sulfur crust on the stick
    lantern = recolor(vanilla(jar, 'block/lantern'), warm)
    for f in range(3):  # sulfur-stained iron rails (a few pixels of the frame rows of each frame)
        blend(lantern, [(1, 16 * f + 8), (4, 16 * f + 8), (2, 16 * f + 2)], 0.35)
    item_lantern = recolor(vanilla(jar, 'item/lantern'), warm)
    cf = vanilla(jar, 'block/campfire_fire')
    fire_rgb = {cf.getpixel((x, y))[:3] for x in range(cf.width) for y in range(cf.height) if cf.getpixel((x, y))[3]}
    in_fire = lambda x, y, p: p[:3] in fire_rgb or p[:3] == EMBER  # noqa: E731
    campfire_fire = recolor(vanilla(jar, 'block/campfire_fire'), in_fire)
    log = recolor(vanilla(jar, 'block/campfire_log_lit'), in_fire)
    item_campfire = recolor(vanilla(jar, 'item/campfire'), in_fire)
    return {'block/sulfur_torch.png': torch, 'block/sulfur_lantern.png': lantern, 'item/sulfur_lantern.png': item_lantern,
            'item/sulfur_campfire.png': item_campfire, 'block/sulfur_campfire_fire.png': campfire_fire,
            'block/sulfur_campfire_log_lit.png': log}


# Vanilla timing: lantern frametime 8, campfire fire frametime 2, lit log interpolated frametime 20.
MCMETA = {
    'block/sulfur_lantern.png': '{\n  "animation": {\n    "frametime": 8\n  }\n}\n',
    'block/sulfur_campfire_fire.png': '{\n  "animation": {\n    "frametime": 2\n  }\n}\n',
    'block/sulfur_campfire_log_lit.png': '{\n  "animation": {\n    "interpolate": true,\n    "frametime": 20\n  }\n}\n',
}

# Vanilla sprite shown next to each new one in the preview (the torch flame, lantern and campfire pairs).
PAIRS = {'block/sulfur_torch.png': 'block/torch', 'block/sulfur_lantern.png': 'block/lantern',
         'item/sulfur_lantern.png': 'item/lantern', 'item/sulfur_campfire.png': 'item/campfire',
         'block/sulfur_campfire_fire.png': 'block/campfire_fire', 'block/sulfur_campfire_log_lit.png': 'block/campfire_log_lit'}


def preview(dst: Path, jar) -> None:
    """8x sheet, one row per sprite: Vanilla | new (first frame, then the other frames). Contains Vanilla pixels."""
    dst.mkdir(parents=True, exist_ok=True)
    new = derived(jar)
    k = 8
    rows = []
    for rel, van in PAIRS.items():
        v = vanilla(jar, van)
        frames = min(v.height // 16, 4)
        rows.append((v, new[rel], frames))
    w = max(2 * 16 * k * f + 16 * k for _, _, f in rows) + 40
    sheet = Image.new('RGBA', (w, sum(16 * k + 16 for _ in rows) + 16), (60, 60, 60, 255))
    y = 8
    for v, n, frames in rows:
        x = 8
        for im in (v, n):
            for f in range(frames):
                c = im.crop((0, 16 * f, 16, 16 * f + 16)).resize((16 * k, 16 * k), Image.NEAREST)
                sheet.paste(c, (x, y), c)
                x += 16 * k + 4
            x += 16 * k // 2
        y += 16 * k + 16
    sheet.save(dst / 'sulfur_lights_vanilla_vs_new_8x.png')
    for rel, im in new.items():
        im.resize((im.width * k, im.height * k), Image.NEAREST).save(dst / ('new_' + rel.replace('/', '_')))


def main(argv: list[str]) -> int:
    check = '--check' in argv
    bad = 0
    for rel, text in MCMETA.items():
        p = TEX / (rel + '.mcmeta')
        if check:
            if not p.exists() or p.read_text() != text:
                print('stale:', rel + '.mcmeta')
                bad += 1
        else:
            p.write_text(text)
    if '--preview' in argv:
        import vanilla_derived
        jar = vanilla_derived.find_jar()
        if jar is None:
            sys.exit('26.3 client jar not found')
        preview(Path(argv[argv.index('--preview') + 1]), jar)
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
