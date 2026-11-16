"""N77 (owner 16.11.2026, "eindeutig fehlt"): textures of simpleores:emerald_pole and the terranite/infernite/zenithite nuggets.

Own pixels only: the shapes are Simple Ores' own textures (silver_pole, enderite_nugget, both with their layer sources), the colours
an own emerald ramp resp. the ramps of the own rift-metal ingots (terranite/infernite/zenithite_ingot), each shade mapped by
brightness rank; the nuggets carry one fleck of their ingot's vein colour. Writes PNG + one-layer sb-layers source next to it.

    python3 tools/textures/n77_ores_2026_11_16.py [out_root]   (default: the module's texture folder)
"""
import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "modules/simpleores/shared/resources/assets/simpleores/textures"
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else TEX

EMERALD = ["#0a3a1e", "#0f5229", "#156b35", "#1c8442", "#249d4f", "#33b55f", "#52cc78", "#7de09a", "#b4f0c4", "#e6fcec"]
# vein colour of each ingot (the accent its own texture carries)
VEIN = {"terranite": "#8768d3", "infernite": "#4ba999", "zenithite": "#e9bc8f"}


def lum(c):
    return 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]


def hexrgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def ramp_of(path, skip=()):
    im = Image.open(path).convert("RGBA")
    cols = sorted({p for p in im.getdata() if p[3] and "#%02x%02x%02x" % p[:3] not in skip}, key=lum)
    return cols


def remap(shape_path, ramp):
    shape = Image.open(shape_path).convert("RGBA")
    ranks = sorted({p for p in shape.getdata() if p[3]}, key=lum)
    out = Image.new("RGBA", shape.size, (0, 0, 0, 0))
    for y in range(shape.height):
        for x in range(shape.width):
            p = shape.getpixel((x, y))
            if p[3]:
                i = ranks.index(p)
                out.putpixel((x, y), ramp[round(i * (len(ramp) - 1) / max(1, len(ranks) - 1))])
    return out


def save(im, rel):
    path = OUT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)
    pal, rows = [], []
    for y in range(im.height):
        row = ""
        for x in range(im.width):
            c = im.getpixel((x, y))
            c = c if c[3] else (0, 0, 0, 0)
            if c not in pal:
                pal.append(c)
            row += f"{pal.index(c):02x}"
        rows.append(row.replace("00" if pal[0] == (0, 0, 0, 0) else "~~", ".."))
    # layers.py: "." = transparent; index 0 is the transparent colour when the corner is empty
    if pal[0] == (0, 0, 0, 0):
        pal = pal[1:]
        rows = ["".join(".." if row[i:i + 2] == ".." else f"{int(row[i:i + 2], 16) - 1:02x}" for i in range(0, len(row), 2)) for row in rows]
    doc = {"format": "sb-layers", "version": 1, "width": im.width, "height": im.height, "digits": 2,
           "palette": ["#%02x%02x%02x%02x" % c for c in pal],
           "layers": [{"name": "Grundform", "visible": True, "opacity": 100, "blend": "normal", "rows": rows}]}
    path.with_name(path.stem + ".layers.json").write_text(json.dumps(doc, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    save(remap(TEX / "block/silver_pole.png", [hexrgb(h) for h in EMERALD]), "block/emerald_pole.png")
    for m, vein in VEIN.items():
        ramp = ramp_of(TEX / f"item/{m}_ingot.png", skip=(vein,))
        # nine shades evenly from the ingot ramp (dark outline .. highlight)
        nine = [ramp[round(i * (len(ramp) - 1) / 8)] for i in range(9)]
        im = remap(TEX / "item/enderite_nugget.png", nine)
        im.putpixel((7, 8), hexrgb(vein))   # one fleck of the vein, like the ingot
        save(im, f"item/{m}_nugget.png")


if __name__ == "__main__":
    main()
