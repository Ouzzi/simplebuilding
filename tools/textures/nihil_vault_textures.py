"""Nihil Vault (2026-10-04): the Astral Vault skin (violet) shifted to nihilith teal, value and shading kept; the
astral diamond is painted over from the neighbouring front panel and an inset nihil ring (the nihilith lamp motif)
takes its place.

Writes only the nihil_vault textures/models and the labeled preview; deterministic --check.
"""
import argparse
import colorsys
import io
import json
import zipfile
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "mc26_3/overlay/resources/assets/simplebuilding"
RING = (179, 220, 221)  # light nihilith ramp colour (end signal family)
CORE = (232, 250, 246)


def nihil_tint(image):
    """Hue-rotates violet enderite toward nihilith teal (about -75 degrees), keeping value."""
    out = image.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            nr, ng, nb = colorsys.hsv_to_rgb((h - 75 / 360) % 1.0, s * 0.85, v)
            px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return out


def images():
    base = Image.open(ASSETS / "textures/entity/chest/astral_vault.png").convert("RGBA")
    px = base.load()
    # The astral diamond (end_system_textures.py): copy the plain panel one front face to the right over it.
    for x, y in [(18, 38), (19, 37), (20, 36), (21, 35), (22, 36), (23, 37), (24, 38), (23, 39), (22, 40),
                 (21, 41), (20, 40), (19, 39), (21, 38)]:
        px[x, y] = px[x + 14, y]
    chest = nihil_tint(base)
    d = ImageDraw.Draw(chest)
    # Chest front UV, same spot as the Astral Vault glyph: inset, never on the rim.
    d.ellipse((18, 35, 24, 41), outline=RING)
    d.point((21, 38), fill=CORE)
    block = Image.open(ASSETS / "textures/block/astral_vault.png").convert("RGBA")
    px = block.load()
    for x, y in [(5, 7), (6, 6), (7, 5), (8, 6), (9, 7), (8, 8), (7, 9), (6, 8)]:
        px[x, y] = px[x, y + 5] if y + 5 < 16 else px[x, y - 5]
    block = nihil_tint(block)
    d = ImageDraw.Draw(block)
    d.ellipse((5, 5, 9, 9), outline=RING)
    return {"entity/chest/nihil_vault.png": chest, "block/nihil_vault.png": block}


def models():
    return {
        "blockstates/nihil_vault.json": {"variants": {"": {"model": "simplebuilding:block/nihil_vault"}}},
        "models/block/nihil_vault.json": {"textures": {"particle": "simplebuilding:block/nihil_vault"}},
        "models/item/nihil_vault.json": {"parent": "minecraft:item/chest"},
        "items/nihil_vault.json": {"model": {"type": "minecraft:special", "base": "simplebuilding:item/nihil_vault",
                                             "model": {"type": "minecraft:chest", "texture": "simplebuilding:nihil_vault"}}},
    }


def preview(art):
    """previews/nihil-gewoelbe-vorschau.png: A vanilla ender chest, B Astral Vault, C Nihil Vault (12x, labeled)."""
    jar = Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client.jar"
    with zipfile.ZipFile(jar) as z:
        ender = Image.open(io.BytesIO(z.read("assets/minecraft/textures/entity/chest/ender.png"))).convert("RGBA")
    astral = Image.open(ASSETS / "textures/entity/chest/astral_vault.png").convert("RGBA")
    sheets = [("A Endertruhe (Vanilla)", ender), ("B Astralgewoelbe", astral),
              ("C Nihil-Gewoelbe (neu)", art["entity/chest/nihil_vault.png"])]
    scale = 12
    canvas = Image.new("RGBA", (len(sheets) * (64 * scale + 24), 64 * scale + 60), (53, 53, 53))
    d = ImageDraw.Draw(canvas)
    for i, (label, sheet) in enumerate(sheets):
        x = i * (64 * scale + 24)
        canvas.alpha_composite(sheet.resize((64 * scale, 64 * scale), Image.Resampling.NEAREST), (x, 50))
        d.text((x + 8, 12), label, fill=(240, 240, 240), font_size=28)
    target = Path("C:/Users/o_o/code/minecraft-mods/previews")
    if not target.exists():
        target = ROOT / "docs/previews"
    target.mkdir(parents=True, exist_ok=True)
    canvas.save(target / "nihil-gewoelbe-vorschau.png")
    return target / "nihil-gewoelbe-vorschau.png"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    art = images()
    failures = []
    for rel, image in art.items():
        p = ASSETS / "textures" / rel
        if args.check:
            if not p.exists() or Image.open(p).convert("RGBA").tobytes() != image.tobytes():
                failures.append(rel)
        else:
            p.parent.mkdir(parents=True, exist_ok=True)
            image.save(p)
    for rel, data in models().items():
        p = ASSETS / rel
        text = json.dumps(data, indent=2) + "\n"
        if args.check:
            if not p.exists() or p.read_text(encoding="utf-8") != text:
                failures.append(rel)
        else:
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(text, encoding="utf-8")
    if args.check:
        if failures:
            raise SystemExit("Outdated: " + ", ".join(failures))
        print(f"Nihil Vault: {len(art)} textures and models current")
        return
    print(f"Nihil Vault: {len(art)} textures, models and preview {preview(art)} generated")


if __name__ == "__main__":
    main()
