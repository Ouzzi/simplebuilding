"""Astralit and Nihilith switch (Queue N35, owner 2026-10-11: "Nihilith-Signalschalter ueberarbeiten").

A slide switch both channels share, only the gem colour differs: a 1 px plate (12 x 12) with a bevelled frame and a
slot along it, and a 4 x 4 x 2 slider. Off: the slider sits at the north end, the slot is dark, the slider's gem
dull. On: the slider sits at the south end, the slot glows in the channel colour and the gem lights up. The
blockstate follows ``enabled`` (the switch's own lever state, immediate) instead of the delayed ``power``.

Texture layout (16 x 16 per state): plate top uv 0..12 x 0..12, plate side uv 0..12 x 12..13, slider top
uv 12..16 x 0..4, slider side uv 12..16 x 4..6.

  python3 tools/textures/end_switches_2026_10_11.py           # writes textures, models, blockstates + preview
  python3 tools/textures/end_switches_2026_10_11.py --check   # checks they are current
"""
import argparse
import json
import os
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mc26_3/overlay/resources/assets/simplebuilding"
PREVIEW = Path(os.path.expanduser("~/previews/n35/switches.png"))

# Plate: the dark End stone grey of the signal family.
RIM_LIGHT, RIM_DARK, FIELD, FLECK_LIGHT, FLECK_DARK, SLOT = (
    (86, 80, 96), (34, 30, 40), (58, 52, 66), (72, 66, 82), (46, 41, 53), (18, 16, 22))
# Gem ramps (dark, mid, light, glint) of the two channels, as in end_pistons/end_rails.
GEMS = {"nihilith": [(24, 52, 62), (48, 104, 116), (83, 160, 172), (214, 250, 250)],
        "astralit": [(66, 30, 56), (128, 60, 104), (196, 104, 160), (255, 220, 240)]}
FLECKS = [(2, 3, FLECK_LIGHT), (9, 2, FLECK_LIGHT), (3, 9, FLECK_DARK), (8, 10, FLECK_DARK), (2, 7, FLECK_DARK), (9, 7, FLECK_LIGHT)]


def texture(channel, on):
    dark, mid, light, glint = GEMS[channel]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Plate top: field, flecks, frame (light top/left, dark bottom/right, like a pressure plate).
    d.rectangle((0, 0, 11, 11), fill=FIELD)
    for x, y, c in FLECKS:
        img.putpixel((x, y), c)
    d.line((0, 0, 11, 0), fill=RIM_LIGHT)
    d.line((0, 0, 0, 11), fill=RIM_LIGHT)
    d.line((0, 11, 11, 11), fill=RIM_DARK)
    d.line((11, 0, 11, 11), fill=RIM_DARK)
    # Slot (world x 7..9, z 4..12): dark, glowing when on; a dark lip on its north edge, light on its south edge.
    d.rectangle((5, 2, 6, 9), fill=mid if on else SLOT)
    if on:
        d.line((5, 3, 5, 8), fill=light)
    d.line((5, 1, 6, 1), fill=RIM_DARK)
    d.line((5, 10, 6, 10), fill=RIM_LIGHT)
    # Marks beside the slot at the "on" end: two gem dots, lit when on.
    for x in (3, 8):
        img.putpixel((x, 8), light if on else dark)
    # Plate side (1 px).
    d.line((0, 12, 11, 12), fill=RIM_DARK)
    # Slider top 4 x 4 at (12, 0): grey rim, gem in the middle.
    d.rectangle((12, 0, 15, 3), fill=RIM_LIGHT)
    d.line((12, 3, 15, 3), fill=FIELD)
    d.line((15, 0, 15, 3), fill=FIELD)
    d.rectangle((13, 1, 14, 2), fill=light if on else dark)
    img.putpixel((13, 1), glint if on else mid)
    # Slider sides 4 x 2 at (12, 4): grey with a gem line.
    d.line((12, 4, 15, 4), fill=FIELD)
    d.line((12, 5, 15, 5), fill=mid if on else dark)
    return img


def face(uv, cull=None):
    f = {"uv": uv, "texture": "#texture"}
    if cull:
        f["cullface"] = cull
    return f


def model(channel, on):
    tex = f"simplebuilding:block/{channel}_switch{'_active' if on else ''}"
    side = face([0, 12, 12, 13])
    plate = {"from": [2, 0, 2], "to": [14, 1, 14],
             "faces": {"down": face([0, 0, 12, 12], "down"), "up": face([0, 0, 12, 12]),
                       "north": side, "south": side, "west": side, "east": side}}
    z = 9 if on else 3
    knob_side = face([12, 4, 16, 6])
    knob = {"from": [6, 1, z], "to": [10, 3, z + 4],
            "faces": {"up": face([12, 0, 16, 4]), "north": knob_side, "south": knob_side,
                      "west": knob_side, "east": knob_side}}
    return {"parent": "minecraft:block/thin_block", "textures": {"texture": tex, "particle": tex},
            "elements": [plate, knob]}


def blockstate(channel):
    return {"variants": {"enabled=false": {"model": f"simplebuilding:block/{channel}_switch"},
                         "enabled=true": {"model": f"simplebuilding:block/{channel}_switch_active"}}}


def outputs():
    out = {}
    for channel in GEMS:
        for on in (False, True):
            name = f"{channel}_switch{'_active' if on else ''}"
            out[f"textures/block/{name}.png"] = texture(channel, on)
            out[f"models/block/{name}.json"] = json.dumps(model(channel, on), indent=2) + "\n"
        out[f"blockstates/{channel}_switch.json"] = json.dumps(blockstate(channel), indent=2) + "\n"
    return out


def same(path, value):
    try:
        if isinstance(value, str):
            return path.read_text(encoding="utf-8") == value
        cur = Image.open(path).convert("RGBA")
        return cur.size == value.size and cur.tobytes() == value.tobytes()
    except OSError:
        return False


def preview(out):
    s = 10
    sheet = Image.new("RGBA", (4 * (16 * s + 20) + 20, 16 * s + 50), (139, 139, 139, 255))
    d = ImageDraw.Draw(sheet)
    for i, channel in enumerate(GEMS):
        for j, on in enumerate((False, True)):
            name = f"{channel}_switch{'_active' if on else ''}"
            x = 20 + (2 * i + j) * (16 * s + 20)
            d.text((x, 8), name, fill=(0, 0, 0, 255))
            sheet.alpha_composite(out[f"textures/block/{name}.png"].resize((16 * s, 16 * s), Image.NEAREST), (x, 30))
    return sheet


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    out = outputs()
    if args.check:
        stale = [rel for rel, value in out.items() if not same(ASSETS / rel, value)]
        if stale:
            print("Outdated:\n  " + "\n  ".join(stale))
            return 1
        print(f"OK: {len(out)} switch files current")
        return 0
    for rel, value in out.items():
        path = ASSETS / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        if isinstance(value, str):
            path.write_text(value, encoding="utf-8")
        else:
            value.save(path)
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    preview(out).save(PREVIEW)
    print(f"{len(out)} files written, preview {PREVIEW}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
