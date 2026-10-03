"""Astral piston (pushes) / Nihil piston (pulls): three texture proposals A/B/C, each off/on.

docs/ai/PLAN-ASTRAL-KOLBEN-2026-10-02.md, section 7:
  A "gem cube"     - all six faces alike: the polished Astralit/Nihilith tile as frame, a dark
                     socket with the 4x4 gem of the switch/lamp family in the middle; on = brighter gem.
  B "arrows"       - A plus small corner brackets: Astral points outward (pushes), Nihil inward (pulls);
                     on = the brackets light up.
  C "piston plate" - vanilla piston head plate recoloured to the channel ramp, small gem hole in the
                     middle; on = a 1 px brighter rim ("extended").

  python tools/textures/end_pistons_2026_10_03.py              # preview + install the chosen variant
  python tools/textures/end_pistons_2026_10_03.py --install B  # switch the built-in variant
  python tools/textures/end_pistons_2026_10_03.py --check      # textures match the chosen variant

The built-in variant is CHOSEN below (owner can re-pick after the preview).
"""
import argparse
import io
import json
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

CHOSEN = "A"
ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mc26_3/overlay/resources/assets/simplebuilding"
SRC_BLOCK = ROOT / "src/main/resources/assets/simplebuilding/textures/block"
CLIENT_JAR = Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client.jar"
PREVIEW = Path("C:/Users/o_o/code/minecraft-mods/previews/astral-nihil-kolben-vorschau.png")

# Gem ramps of the End signal family (end_system_textures.py): dark, mid, light.
GEMS = {"astral": [(98, 48, 81), (177, 99, 141), (241, 175, 211)],
        "nihil": [(38, 65, 82), (83, 145, 158), (179, 220, 221)]}
POLISHED = {"astral": "polished_astralit.png", "nihil": "polished_nihilith.png"}
SOCKET = (34, 29, 47)
SOCKET_RIM = (58, 52, 66)
SOCKET_SHADE = (24, 20, 32)
WHITE = (250, 240, 248)


def ramp(channel):
    """The polished tile's own colours, dark to light."""
    tile = Image.open(SRC_BLOCK / POLISHED[channel]).convert("RGBA")
    return sorted({p[:3] for p in tile.get_flattened_data() if p[3]}, key=lambda c: sum(c))


def gem(d, x, y, channel, on):
    dark, mid, light = GEMS[channel]
    edge, body, shine = (mid, light, WHITE) if on else (dark, mid, light)
    for gx, gy in [(1, 0), (2, 0), (0, 1), (3, 1), (0, 2), (3, 2), (1, 3), (2, 3)]:
        d.point((x + gx, y + gy), fill=edge)
    for gx, gy in [(1, 1), (2, 1), (1, 2), (2, 2)]:
        d.point((x + gx, y + gy), fill=body)
    d.point((x + 1, y + 1), fill=shine)


def socket(d, x0, y0, x1, y1):
    """Inset hole: dark fill, shaded top/left, lit bottom/right."""
    d.rectangle((x0, y0, x1, y1), fill=SOCKET)
    d.line([(x0, y0), (x1, y0)], fill=SOCKET_SHADE)
    d.line([(x0, y0), (x0, y1)], fill=SOCKET_SHADE)
    d.line([(x0 + 1, y1), (x1, y1)], fill=SOCKET_RIM)
    d.line([(x1, y0 + 1), (x1, y1)], fill=SOCKET_RIM)


def variant_a(channel, on):
    img = Image.open(SRC_BLOCK / POLISHED[channel]).convert("RGBA")
    d = ImageDraw.Draw(img)
    socket(d, 5, 5, 10, 10)
    gem(d, 6, 6, channel, on)
    return img


def variant_b(channel, on):
    img = variant_a(channel, on)
    d = ImageDraw.Draw(img)
    colour = GEMS[channel][2] if on else GEMS[channel][0]
    for sx, sy in [(1, 1), (-1, 1), (1, -1), (-1, -1)]:
        cx = 2 if sx > 0 else 13
        cy = 2 if sy > 0 else 13
        if channel == "astral":  # vertex at the corner: points outward
            pts = [(cx, cy), (cx + sx, cy), (cx + 2 * sx, cy), (cx, cy + sy), (cx, cy + 2 * sy)]
        else:  # vertex towards the middle: points inward
            vx, vy = cx + 2 * sx, cy + 2 * sy
            pts = [(vx, vy), (vx - sx, vy), (vx - 2 * sx, vy), (vx, vy - sy), (vx, vy - 2 * sy)]
        for p in pts:
            d.point(p, fill=colour)
    return img


def piston_top():
    with zipfile.ZipFile(CLIENT_JAR) as jar:
        return Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/piston_top.png"))).convert("RGBA")


def variant_c(channel, on):
    src = piston_top()
    shades = ramp(channel)
    img = Image.new("RGBA", src.size)
    lum = sorted({sum(p[:3]) for p in src.get_flattened_data() if p[3]})
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if not p[3]:
                continue
            i = lum.index(sum(p[:3])) * (len(shades) - 1) // max(1, len(lum) - 1)
            img.putpixel((x, y), shades[i] + (255,))
    d = ImageDraw.Draw(img)
    socket(d, 6, 6, 9, 9)
    dark, mid, light = GEMS[channel]
    d.rectangle((7, 7, 8, 8), fill=light if on else mid)
    d.point((7, 7), fill=WHITE if on else light)
    if on:
        d.rectangle((0, 0, 15, 15), outline=shades[-1] + (255,))
    return img


VARIANTS = {"A": variant_a, "B": variant_b, "C": variant_c}


def textures(choice):
    return {f"block/{ch}_piston{'_active' if on else ''}.png": VARIANTS[choice](ch, on)
            for ch in ["astral", "nihil"] for on in [False, True]}


def write_models():
    def write(rel, data):
        p = ASSETS / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    for ch in ["astral", "nihil"]:
        name = f"{ch}_piston"
        write(f"blockstates/{name}.json", {"variants": {
            f"power={power}": {"model": f"simplebuilding:block/{name}" + ("_active" if power else "")} for power in range(16)}})
        for suffix in ["", "_active"]:
            write(f"models/block/{name}{suffix}.json",
                  {"parent": "minecraft:block/cube_all", "textures": {"all": f"simplebuilding:block/{name}{suffix}"}})
        write(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"simplebuilding:block/{name}"}})


def preview():
    scale, pad = 8, 12
    try:
        font = ImageFont.truetype("arial.ttf", 22)
    except OSError:
        font = ImageFont.load_default()
    refs = [Image.open(SRC_BLOCK / POLISHED[c]).convert("RGBA") for c in ["astral", "nihil"]]
    refs += [Image.open(ASSETS / "textures/block" / f"{c}_switch.png").convert("RGBA") for c in ["astralit", "nihilith"]]
    rows = [("Vorlagen", refs)] + [(f"{k}", [VARIANTS[k](ch, on) for ch in ["astral", "nihil"] for on in [False, True]])
                                   for k in VARIANTS]
    cell = 16 * scale
    width = 140 + 4 * (cell + pad)
    height = 40 + len(rows) * (cell + pad + 10)
    canvas = Image.new("RGBA", (width, height), (53, 53, 53, 255))
    d = ImageDraw.Draw(canvas)
    for i, head in enumerate(["Astral aus", "Astral an", "Nihil aus", "Nihil an"]):
        d.text((140 + i * (cell + pad), 8), head, fill=(230, 230, 230), font=font)
    for r, (label, images) in enumerate(rows):
        y = 40 + r * (cell + pad + 10)
        d.text((10, y + cell // 2 - 12), label, fill=(255, 220, 120) if label == CHOSEN else (230, 230, 230), font=font)
        if label == CHOSEN:
            d.text((10, y + cell // 2 + 14), "eingebaut", fill=(255, 220, 120), font=ImageFont.load_default())
        for i, im in enumerate(images):
            canvas.alpha_composite(im.resize((cell, cell), Image.Resampling.NEAREST), (140 + i * (cell + pad), y))
            if r == 0:
                caption = ["pol. Astralit", "pol. Nihilith", "Astralit-Schalter", "Nihilith-Schalter"][i]
                d.text((140 + i * (cell + pad), y + cell + 1), caption, fill=(200, 200, 200), font=ImageFont.load_default())
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(PREVIEW)
    return PREVIEW


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--install", choices=sorted(VARIANTS), default=CHOSEN)
    args = parser.parse_args()
    art = textures(args.install)
    if args.check:
        bad = [rel for rel, im in art.items() if not (ASSETS / "textures" / rel).exists()
               or Image.open(ASSETS / "textures" / rel).convert("RGBA").tobytes() != im.tobytes()]
        if bad:
            raise SystemExit("Outdated: " + ", ".join(bad))
        print(f"End pistons: {len(art)} textures current (variant {args.install})")
        return
    for rel, im in art.items():
        p = ASSETS / "textures" / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        im.save(p)
    write_models()
    print(f"End pistons: variant {args.install} installed, preview {preview()}")


if __name__ == "__main__":
    main()
