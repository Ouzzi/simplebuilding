#!/usr/bin/env python3
"""Vorher/nachher-Vorschau des Textur-Audits Q1 (2026-10-02), gruppiert, 4-fach.

    python tools/textures/texfix_preview_2026_10_02.py <ziel.png> [<git-rev vorher>]

Vorher = Stand von <git-rev> (Standard 923c7d44, vor dem Audit-Umbau), nachher = Arbeitsbaum.
"""
import io
import os
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
SB = "src/main/resources/assets/simplebuilding/textures"
OV = "mc26_3/overlay/resources/assets/simplebuilding/textures"
MONEY = "modules/simplemoney/shared/resources/assets/simplemoney/textures/item"

PADS = ["chunk_loader", "netherite_chunk_loader", "enderite_chunk_loader", "elytra_pad", "reinforced_elytra_pad",
        "netherite_elytra_pad", "enderite_elytra_pad", "fine_elytra_pad", "spawn_teleporter", "spawn_teleporter_tier_2",
        "spawn_teleporter_tier_3", "spawn_teleporter_tier_4", "enderite_spawn_teleporter", "launchpad",
        "netherite_launchpad", "enderite_launchpad", "flypad_ender", "reinforced_flypad_ender", "stellar_flypad_ender",
        "netherite_flypad"]
PLATES = ["copper_pressure_plate", "exposed_copper_pressure_plate", "weathered_copper_pressure_plate",
          "oxidized_copper_pressure_plate"]
BOOKS = ["break_through", "bridge", "color_palette", "constructors_touch", "cover", "deep_pockets", "double_jump",
         "drawer", "fast_chiseling", "funnel", "kinetic_protection", "linear", "master_builder", "override", "radius",
         "range", "strip_miner", "vein_miner", "versatility"]
GUIDES = ["admin", "building", "enchantments", "end", "gadgets", "machines", "storage", "tools", "trims", "tweaks",
          "vanilla_caves", "vanilla_end", "vanilla_farming", "vanilla_gear", "vanilla_nether", "vanilla_ocean",
          "vanilla_overworld", "vanilla_redstone"]


def groups():
    pad_states = []
    for n in PADS:
        for s in ("", "_active", "_charge_1", "_charge_2", "_charge_3"):
            if os.path.exists(os.path.join(REPO, SB, "block", f"{n}{s}.png")):
                pad_states.append(f"{SB}/block/{n}{s}.png")
    return [
        ("simplemoney", [f"{MONEY}/{n}.png" for n in ("blank_note", "refined_blank_note", "special_paper", "money_bill",
                                                     "raw_bill", "resin_fiber", "special_fiber")]),
        ("Einzel-Items", ["modules/simplefun/shared/resources/assets/simplefun/textures/item/brick_snowball.png",
                          f"{SB}/item/raw_enderite.png", f"{SB}/item/spawn_elytra.png", f"{SB}/item/blueprint_signed.png"]),
        ("Pads, Teleporter, Chunk-Loader, Launchpads (mit Zustaenden)", pad_states),
        ("Kupfer-Druckplatten", [f"{SB}/block/{n}{s}.png" for n in PLATES for s in ("", "_active")]),
        ("Verzauberte Buecher (Mod)", [f"{SB}/item/enchanted_book_{n}.png" for n in BOOKS]),
        ("Regal-Handbuecher 26.3 (guide_book und vanilla_start unveraendert)", [f"{OV}/item/guide_book_{n}.png" for n in GUIDES]),
    ]


def old_image(rev, rel):
    data = subprocess.run(["git", "-C", REPO, "show", f"{rev}:{rel}"], capture_output=True).stdout
    return Image.open(io.BytesIO(data)).convert("RGBA") if data else None


def colors(img):
    return len({p for p in img.getdata() if p[3]}) if img else 0


def main():
    out = sys.argv[1]
    rev = sys.argv[2] if len(sys.argv) > 2 else "923c7d44"
    scale, cols = 4, 8
    cell_w, cell_h = 2 * 16 * scale + 14, 16 * scale + 26
    try:
        font = ImageFont.truetype("consola.ttf", 11)
        big = ImageFont.truetype("consola.ttf", 15)
    except OSError:
        font = big = ImageFont.load_default()
    gs = groups()
    height = 34 + sum(22 + ((len(f) + cols - 1) // cols) * cell_h for _, f in gs)
    sheet = Image.new("RGBA", (cols * cell_w + 8, height), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    d.text((6, 6), f"Textur-Audit Q1: vorher (links, {rev}) / nachher (rechts), 4x, Zahl = Farben", fill=(0, 0, 0), font=big)
    y = 34
    for title, files in gs:
        d.text((6, y), title, fill=(30, 30, 30), font=big)
        y += 22
        for i, rel in enumerate(files):
            x0, y0 = 6 + (i % cols) * cell_w, y + (i // cols) * cell_h
            old = old_image(rev, rel)
            new = Image.open(os.path.join(REPO, rel)).convert("RGBA")
            for j, img in enumerate((old, new)):
                bx = x0 + j * (16 * scale + 4)
                d.rectangle([bx, y0, bx + 16 * scale - 1, y0 + 16 * scale - 1], fill=(139, 139, 139, 255))
                if img is not None:
                    img = img.crop((0, 0, 16, 16))
                    sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (bx, y0))
            name = os.path.basename(rel)[:-4]
            d.text((x0, y0 + 16 * scale + 1), f"{name[:20]}", fill=(0, 0, 0), font=font)
            d.text((x0, y0 + 16 * scale + 12), f"{colors(old)} -> {colors(new)}", fill=(0, 0, 90), font=font)
        y += ((len(files) + cols - 1) // cols) * cell_h
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    sheet.save(out)
    print(out)


if __name__ == "__main__":
    main()
