"""Simple Ovens (BOAT40, b-boat02): the furnace boat/raft item overlays. Like the chest boat overlays of Simple Storage: they
cover exactly the pixels where Vanilla's chest boat (raft) differs from the plain boat (raft), painted in the shades of
Vanilla's furnace side texture by Vanilla's light, with one fire pixel in the mouth. Vanilla-derived: built, never committed."""
from __future__ import annotations

import io
import zipfile
from pathlib import Path

from PIL import Image

import vehicles_2026_10_10 as vehicles


def furnace_shades(jar):
    """Five shades (dark..light) of Vanilla's furnace side: luminance quantiles of its pixels."""
    with zipfile.ZipFile(jar) as z:
        im = Image.open(io.BytesIO(z.read("assets/minecraft/textures/block/furnace_side.png"))).convert("RGBA")
    px = sorted((c[:3] for c in im.get_flattened_data() if c[3] > 0), key=vehicles.lum)
    return [px[min(len(px) - 1, int(q * (len(px) - 1)))] for q in (0.05, 0.3, 0.55, 0.75, 0.9)]


def overlay(chest, plain, shades):
    region = vehicles.mask(chest, plain)
    out = vehicles.paint(chest, region, shades)
    inner = sorted(vehicles.interior(region), key=lambda q: (q[1], q[0]))
    if inner:
        x, y = inner[len(inner) * 2 // 3]
        out.putpixel((x, y), (255, 150, 40, 255))
    return out


def derived(jar):
    """Texture path under assets/simpleovens/textures/ -> image, for tools/textures/vanilla_derived.py --module."""
    vehicles.CLIENT_JAR = Path(jar)
    shades = furnace_shades(jar)
    return {"item/furnace_boat_overlay.png": overlay(vehicles.vanilla("oak_chest_boat"), vehicles.vanilla("oak_boat"), shades),
            "item/furnace_raft_overlay.png": overlay(vehicles.vanilla("bamboo_chest_raft"), vehicles.vanilla("bamboo_raft"), shades)}
