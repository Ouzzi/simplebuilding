#!/usr/bin/env python3
"""CLOUDS17/20 (docs/ai/PLAN-B-CLOUDS17-2026-11-10.md): alpha for the approved cloud textures, colours unchanged.

In MC 26.3 the chunk layer of a block face follows its sprite: alpha only 255 = solid, any 0 = cutout (Glass), any 1..254 =
translucent (Ice). Cloud Stone gets Ice-like alpha per palette tone, the Cloud Block gets Glass-like holes (alpha 0) in its
puff gaps (variant A, 15 px in three rounded clusters) or milk-glass alpha (variant B, preview only).

    python3 modules/simplelayers/tools/gen_clouds17_alpha.py            write cloud_stone + cloud_block (variant A) in place
    python3 modules/simplelayers/tools/gen_clouds17_alpha.py --out DIR  write cloud_block_a/_b + cloud_stone to DIR (preview)

Idempotent: the source tones are read from the layer files with their alpha reset to 255. The layers stay ("Grundton" below,
the detail layer above), made disjoint so source-over composes the exact alpha.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools" / "textures"))
import layers  # noqa: E402

TEX = ROOT / "modules/simplelayers/shared/resources/assets/simplelayers/textures/block"

# Ice is alpha 190 everywhere; the darker grain stays a little denser so the stone structure still reads.
STONE = {"dee3ec": 185, "cfd5e1": 195, "bec6d4": 205, "aab2c2": 220}
# A (Glass): the cloud stays opaque, a few rounded gaps between the puffs are clear (alpha 0) like the panes of Glass.
CLOUD_A = {"fafcff": 255, "eef2fa": 255, "e0e6f2": 255, "ced6e6": 255}
# B (milk glass): puff highlights densest, creases most see-through.
CLOUD_B = {"fafcff": 230, "eef2fa": 200, "e0e6f2": 210, "ced6e6": 160}
# Gaps for A as 3-6 px clusters away from the tile edges (no stripes when tiled, no crumbs) - (x, y). Their rim gets the
# shade tones: top/left edge of a gap = the darkest tone (the puff above casts it), bottom/right = the mid shade.
CLOUD_A_HOLES = [(3, 3), (4, 3), (3, 4), (4, 4), (5, 4),
                 (10, 8), (11, 8), (10, 9), (11, 9), (12, 9), (11, 10),
                 (6, 12), (7, 12), (6, 13), (7, 13)]
DARK, MID = (0xce, 0xd6, 0xe6), (0xe0, 0xe6, 0xf2)


def load(name: str) -> dict:
    doc = json.loads((TEX / f"{name}.layers.json").read_text(encoding="utf-8"))
    doc["palette"] = [c[:7] + "ff" for c in doc["palette"]]
    return doc


def rim(holes, w: int, h: int) -> dict[int, tuple[int, int, int]]:
    hs, out = set(holes), {}
    for x, y in hs:
        for dx, dy, tone in ((0, -1, DARK), (-1, 0, DARK), (0, 1, MID), (1, 0, MID)):
            q = (x + dx, y + dy)
            if q not in hs and 0 <= q[0] < w and 0 <= q[1] < h:
                out.setdefault(q[1] * w + q[0], tone)
    return out


def build(doc: dict, alpha: dict[str, int], extra_holes=()) -> dict:
    w, h = doc["width"], doc["height"]
    per_layer = [layers.layer_pixels(doc, layer) for layer in doc["layers"]]
    top = [None] * (w * h)
    for i in range(w * h):
        for li, pix in enumerate(per_layer):
            if pix[i] is not None:
                top[i] = li
    holes = {y * w + x for x, y in extra_holes}
    tones = rim(extra_holes, w, h)
    out = []
    for li, layer in enumerate(doc["layers"]):
        rgba = bytearray(w * h * 4)
        for i, px in enumerate(per_layer[li]):
            if px is None or top[i] != li:
                continue
            rgb = tones.get(i, px[:3])
            a = 0 if i in holes else alpha["%02x%02x%02x" % tuple(rgb)]
            if a:
                rgba[i * 4:i * 4 + 4] = bytes((*rgb, a))
        out.append((layer["name"], bytes(rgba)))
    return layers.from_layers(w, h, out)


def write(doc: dict, png: Path) -> None:
    png.write_bytes(layers.png_write(doc["width"], doc["height"], bytes(layers.compose(doc))))
    layers.layers_path(png).write_text(layers.dumps(doc), encoding="utf-8")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", type=Path)
    ap.add_argument("--variant", choices="ab", default="b", help="Cloud Block in place: a = glass holes, b = milk glass (CLOUDS20, default)")
    args = ap.parse_args()
    stone = build(load("cloud_stone"), STONE)
    cloud = load("cloud_block")
    if args.out:
        args.out.mkdir(parents=True, exist_ok=True)
        write(stone, args.out / "cloud_stone.png")
        write(build(cloud, CLOUD_A, CLOUD_A_HOLES), args.out / "cloud_block_a.png")
        write(build(cloud, CLOUD_B), args.out / "cloud_block_b.png")
    else:
        write(stone, TEX / "cloud_stone.png")
        write(build(cloud, CLOUD_A, CLOUD_A_HOLES) if args.variant == "a" else build(cloud, CLOUD_B), TEX / "cloud_block.png")


if __name__ == "__main__":
    main()
