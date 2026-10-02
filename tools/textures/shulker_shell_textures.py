"""Usage: python tools/textures/shulker_shell_textures.py [--preview <png>]

Tier shulker shells (owner 2026-10-02): Reinforced, Netherite and Enderite Shulker Shell.

Built on vanilla's item/shulker_shell.png (read from the 26.3 client jar): the shell keeps its purple body and
grain, the tier shows the way the tier shulker boxes show it (tools/textures/tiered_shulker_box_textures.py) -
the outline becomes the tier's plating, the lower rim a plating band, and three studs of the tier's accent sit
on the lid. Same plating colors as the boxes, so shell, box and the rare shulker's hull match.

Output: mc26_3/overlay/resources/assets/simplebuilding/textures/item/<tier>_shulker_shell.png (26.3-only items).
--preview also writes a labeled, 16x upscaled preview: A/B/C the shells next to vanilla, D/E the hull of the
reinforced and enderite shulker: the existing tier box entity texture with vanilla's shulker head, written to
textures/entity/rare_shulker/<tier>.png (outside entity/shulker, so the shulker box atlas does not pick it up).
"""
import io
import os
import sys
import zipfile

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
from tiered_shulker_box_textures import PLATING  # noqa: E402

JAR = os.path.expanduser(r"~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar")
OUT = "mc26_3/overlay/resources/assets/simplebuilding/textures/item"
HULL_OUT = "mc26_3/overlay/resources/assets/simplebuilding/textures/entity/rare_shulker"
ENTITY = "src/main/resources/assets/simplebuilding/textures/entity/shulker"
TIERS = ("reinforced", "netherite", "enderite")

# Vanilla shell colors (item/shulker_shell.png) by role.
OUTLINE_LIGHT = (82, 52, 96)   # upper/left outline
OUTLINE_DARK = (67, 38, 80)    # lower/right outline
# Studs on the lid (x, y): on the mid-purple field, away from the outline.
STUDS = ((7, 4), (10, 4), (5, 6))


def vanilla_entity():
    with zipfile.ZipFile(JAR) as z:
        return Image.open(io.BytesIO(z.read("assets/minecraft/textures/entity/shulker/shulker.png"))).convert("RGBA")


def hull(tier, vanilla):
    """The rare shulker's texture: the undyed tier box (shell, plating) with vanilla's head (x < 24, y >= 52)."""
    img = Image.open(os.path.join(ENTITY, tier + ".png")).convert("RGBA")
    img.paste(vanilla.crop((0, 52, 24, 64)), (0, 52))
    return img


def vanilla_shell():
    with zipfile.ZipFile(JAR) as z:
        return Image.open(io.BytesIO(z.read("assets/minecraft/textures/item/shulker_shell.png"))).convert("RGBA")


def paint(base, tier):
    pal = PLATING[tier]
    img = base.copy()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                continue
            c = (r, g, b)
            if c == OUTLINE_LIGHT:
                # Upper outline: plating mid tone; the rim below row 8 a shade lighter (the band).
                img.putpixel((x, y), pal["c"] + (255,) if y < 8 else pal["d"] + (255,))
            elif c == OUTLINE_DARK:
                img.putpixel((x, y), pal["a"] + (255,))
    for x, y in STUDS:
        img.putpixel((x, y), pal["y"] + (255,))
        img.putpixel((x, y - 1), pal["z"] + (255,))
    return img


def preview(path, base, shells):
    scale = 16
    cells = [("vanilla", base)] + [(t, shells[t]) for t in TIERS]
    entity = []
    for t in TIERS:
        p = os.path.join(HULL_OUT, t + ".png")
        if os.path.exists(p):
            entity.append((t, Image.open(p).convert("RGBA")))
    width = max(len(cells) * (16 * scale + 20) + 20, 3 * (64 * 6 + 20) + 20)
    height = 16 * scale + 60 + (64 * 6 + 60 if entity else 0)
    sheet = Image.new("RGBA", (max(width, 2 * (64 * 6 + 20) + 20), height), (40, 40, 46, 255))
    draw = ImageDraw.Draw(sheet)
    x = 20
    for i, (name, img) in enumerate(cells):
        label = "" if i == 0 else "ABC"[i - 1] + " "
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (x, 40))
        draw.text((x, 12), label + name + " shell", fill=(235, 235, 235, 255))
        x += 16 * scale + 20
    y = 16 * scale + 60
    x = 20
    for i, (name, img) in enumerate(entity):
        sheet.alpha_composite(img.resize((64 * 6, 64 * 6), Image.NEAREST), (x, y + 30))
        draw.text((x, y + 6), "DEF"[i] + " " + name + " shulker hull (entity texture)", fill=(235, 235, 235, 255))
        x += 64 * 6 + 20
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sheet.save(path)


def main():
    base = vanilla_shell()
    shells = {t: paint(base, t) for t in TIERS}
    os.makedirs(OUT, exist_ok=True)
    for t, img in shells.items():
        img.save(os.path.join(OUT, t + "_shulker_shell.png"))
    os.makedirs(HULL_OUT, exist_ok=True)
    vanilla = vanilla_entity()
    for t in TIERS:
        hull(t, vanilla).save(os.path.join(HULL_OUT, t + ".png"))
    if "--preview" in sys.argv:
        preview(sys.argv[sys.argv.index("--preview") + 1], base, shells)


if __name__ == "__main__":
    main()
