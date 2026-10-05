"""Astral rail (boosts) / Nihil rail (brakes): three texture proposals A/B/C, each off/on.

docs/ai/PLAN-ASTRAL-NIHIL-SCHIENEN-2026-10-02.md. Base is vanilla's powered rail (same sleepers, iron and layout):
  A "channel gold" - the three gold shades of the rail become the channel's gem ramp (switch/lamp/piston family),
                     the redstone line becomes the channel colour: dim when off, bright when on.
  B "line only"    - vanilla gold stays, only the redstone line takes the channel colour.
  C "polished"     - the gold becomes the polished Astralit/Nihilith stone ramp, the line the gem colour.

  python tools/textures/end_rails_2026_10_04.py              # preview + install the chosen variant
  python tools/textures/end_rails_2026_10_04.py --install B  # switch the built-in variant
  python tools/textures/end_rails_2026_10_04.py --check      # textures match the chosen variant
"""
import argparse
import io
import json
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

CHOSEN = "B"
ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mc26_3/overlay/resources/assets/simplebuilding"
SRC_BLOCK = ROOT / "src/main/resources/assets/simplebuilding/textures/block"
CLIENT_JAR = Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client.jar"
PREVIEW = Path("C:/Users/o_o/code/minecraft-mods/previews/astral-nihil-schienen-vorschau.png")

# Gem ramps of the End signal family (end_system_textures.py / end_pistons_2026_10_03.py): dark, mid, light.
GEMS = {"astral": [(98, 48, 81), (177, 99, 141), (241, 175, 211)],
        "nihil": [(38, 65, 82), (83, 145, 158), (179, 220, 221)]}
POLISHED = {"astral": "polished_astralit.png", "nihil": "polished_nihilith.png"}
# Vanilla powered rail: gold dark/mid/light, redstone line off (dark..light) and on (dark..light).
GOLD = [(201, 136, 29), (220, 174, 41), (243, 193, 42)]
LINE_OFF = [(49, 0, 0), (74, 3, 3), (98, 2, 2)]
LINE_ON = [(136, 3, 0), (171, 3, 1), (212, 1, 2)]


def vanilla(name):
    with zipfile.ZipFile(CLIENT_JAR) as jar:
        return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def stone_ramp(channel):
    tile = Image.open(SRC_BLOCK / POLISHED[channel]).convert("RGBA")
    shades = sorted({p[:3] for p in tile.get_flattened_data() if p[3]}, key=lambda c: sum(c))
    return [shades[len(shades) // 4], shades[len(shades) // 2], shades[-1]]


def gold_ramp(channel, variant):
    dark, mid, light = GEMS[channel]
    if variant == "A":
        return [mid, mix(mid, light, 0.5), light]
    if variant == "C":
        return stone_ramp(channel)
    return GOLD


def line_ramp(channel, on):
    dark, mid, light = GEMS[channel]
    if on:
        return [mid, light, mix(light, (255, 255, 255), 0.45)]
    return [mix(dark, (0, 0, 0), 0.45), mix(dark, (0, 0, 0), 0.2), dark]


def recolour(channel, on, variant):
    src = vanilla("powered_rail_on" if on else "powered_rail")
    swap = dict(zip(GOLD, gold_ramp(channel, variant)))
    swap.update(zip(LINE_ON if on else LINE_OFF, line_ramp(channel, on)))
    img = src.copy()
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if p[3] and p[:3] in swap:
                img.putpixel((x, y), swap[p[:3]] + (p[3],))
    return img


VARIANTS = ["A", "B", "C"]


def textures(choice):
    return {f"block/{ch}_rail{'_on' if on else ''}.png": recolour(ch, on, choice)
            for ch in ["astral", "nihil"] for on in [False, True]}


def write_models():
    def write(rel, data):
        p = ASSETS / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    shapes = {"north_south": ("", None), "east_west": ("", 90), "ascending_north": ("_raised_ne", None),
              "ascending_east": ("_raised_ne", 90), "ascending_south": ("_raised_sw", None), "ascending_west": ("_raised_sw", 90)}
    parents = {"": "minecraft:block/rail_flat", "_raised_ne": "minecraft:block/template_rail_raised_ne",
               "_raised_sw": "minecraft:block/template_rail_raised_sw"}
    for ch in ["astral", "nihil"]:
        name = f"{ch}_rail"
        variants = {}
        for powered in ["false", "true"]:
            on = "_on" if powered == "true" else ""
            for shape, (suffix, y) in shapes.items():
                entry = {"model": f"simplebuilding:block/{name}{on}{suffix}"}
                if y:
                    entry["y"] = y
                variants[f"powered={powered},shape={shape}"] = entry
        write(f"blockstates/{name}.json", {"variants": variants})
        for on in ["", "_on"]:
            for suffix, parent in parents.items():
                write(f"models/block/{name}{on}{suffix}.json",
                      {"parent": parent, "textures": {"rail": f"simplebuilding:block/{name}{on}"}})
        write(f"models/item/{name}.json", {"parent": "minecraft:item/generated",
                                           "textures": {"layer0": f"simplebuilding:block/{name}"}})
        write(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"simplebuilding:item/{name}"}})


def preview():
    scale, pad = 8, 12
    try:
        font = ImageFont.truetype("arial.ttf", 22)
    except OSError:
        font = ImageFont.load_default()
    refs = [vanilla("powered_rail"), vanilla("powered_rail_on"),
            Image.open(ASSETS / "textures/block/astralit_switch.png").convert("RGBA"),
            Image.open(ASSETS / "textures/block/nihilith_switch.png").convert("RGBA")]
    rows = [("Vorlagen", refs)] + [(k, [recolour(ch, on, k) for ch in ["astral", "nihil"] for on in [False, True]])
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
                caption = ["Antriebsschiene", "Antriebsschiene an", "Astralit-Schalter", "Nihilith-Schalter"][i]
                d.text((140 + i * (cell + pad), y + cell + 1), caption, fill=(200, 200, 200), font=ImageFont.load_default())
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(PREVIEW)
    return PREVIEW


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--install", choices=VARIANTS, default=CHOSEN)
    args = parser.parse_args()
    art = textures(args.install)
    if args.check:
        bad = [rel for rel, im in art.items() if not (ASSETS / "textures" / rel).exists()
               or Image.open(ASSETS / "textures" / rel).convert("RGBA").tobytes() != im.tobytes()]
        if bad:
            raise SystemExit("Outdated: " + ", ".join(bad))
        print(f"End rails: {len(art)} textures current (variant {args.install})")
        return
    for rel, im in art.items():
        p = ASSETS / "textures" / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        im.save(p)
    write_models()
    print(f"End rails: variant {args.install} installed, preview {preview()}")


if __name__ == "__main__":
    main()
