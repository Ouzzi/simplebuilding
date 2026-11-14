"""N48c (b-circ46, 14.11.2026): textures of simplelib:emerald_pressure_plate, derived from Vanilla's emerald block (palette snapped, no blur).

Usage: python3.12 n48_emerald_plate_2026_11_14.py <emerald_block.png> <out_dir> [round]
Round 1: every pixel one ramp step lighter.  Round 2: lighter in a diamond shaped centre only.
Round 3 (used): like 2, plus the centre two steps and the outer ring one step, so the pressed plate "glows" like the diamond one.
The resting texture is the emerald block itself with a one pixel lighter top-left edge (a plate lying flat catches the light).
"""
import sys
from pathlib import Path
from PIL import Image

src, out = Path(sys.argv[1]), Path(sys.argv[2])
rnd = int(sys.argv[3]) if len(sys.argv) > 3 else 3
base = Image.open(src).convert("RGBA")
lum = lambda c: 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]
ramp = sorted({p for p in base.getdata()}, key=lum)
idx = {c: i for i, c in enumerate(ramp)}
step = lambda c, n: ramp[max(0, min(len(ramp) - 1, idx[c] + n))]


def active(shift_fn):
    im = base.copy()
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            im.putpixel((x, y), step(base.getpixel((x, y)), shift_fn(d)))
    return im


def resting():
    im = base.copy()
    for i in range(1, 15):
        for (x, y) in ((i, 0), (0, i)):
            im.putpixel((x, y), step(base.getpixel((x, y)), 1))
    return im


fn = {1: lambda d: 1, 2: lambda d: 1 if d <= 8 else 0, 3: lambda d: 2 if d <= 5 else (1 if d <= 10 else 0)}[rnd]
out.mkdir(parents=True, exist_ok=True)
resting().save(out / "emerald_pressure_plate.png")
active(fn).save(out / "emerald_pressure_plate_active.png")

# Layer sources (sb-layers, one base layer) next to the PNGs, like every texture of this repo.
import json
for name in ("emerald_pressure_plate", "emerald_pressure_plate_active"):
    im = Image.open(out / f"{name}.png").convert("RGBA")
    pal = []
    rows = []
    for y in range(16):
        row = ""
        for x in range(16):
            c = im.getpixel((x, y))
            if c not in pal:
                pal.append(c)
            row += f"{pal.index(c):02x}"
        rows.append(row)
    doc = {"format": "sb-layers", "version": 1, "width": 16, "height": 16, "digits": 2,
           "palette": ["#%02x%02x%02x%02x" % c for c in pal],
           "layers": [{"name": "Grundform", "visible": True, "opacity": 100, "blend": "normal", "rows": rows}]}
    (out / f"{name}.layers.json").write_text(json.dumps(doc, ensure_ascii=False) + "\n", encoding="utf-8")
