#!/usr/bin/env python3
"""Bucket of Frog (Simple Mob Manners, queue N62): own pixel art, a metal pail with a frog peeking out of the water.

    python tools/textures/frog_bucket_2026_10_15.py            # writes the three item textures (variant C, 31.10.)
    python tools/textures/frog_bucket_2026_10_15.py --check    # fails if a texture is out of date

Since 31.10. variant C of the b-optik32 preview (one sprite per frog variant). No Vanilla pixels are used.
"""
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEXTURE = ROOT / "modules/simplemobmanners/shared/resources/assets/simplemobmanners/textures/item/frog_bucket.png"


# Owner choice 31.10. (IG31-23, preview b-optik32): variant C, one sprite per frog variant, drawn by optik32_items.py (Endwood
# process A8b). frog_bucket.png is the temperate frog (also the fallback without a variant), _warm/_cold the other two; the item
# definition items/frog_bucket.json selects them by the minecraft:frog/variant component.
TEX_DIR = TEXTURE.parent
CHOSEN = {"frog_bucket": "temperate", "frog_bucket_warm": "warm", "frog_bucket_cold": "cold"}


def chosen():
    sys.path.insert(0, str(ROOT / "tools/textures"))
    import layers as L
    import optik32_items as OI
    out = {}
    for name, kind in CHOSEN.items():
        img = OI.frog("C", kind).copy()
        out[TEX_DIR / f"{name}.png"] = L.snap(img, "Frosch im Eimer")
    return out, L


def main():
    files, L = chosen()
    if "--check" in sys.argv:
        stale = [p.name for p, img in files.items() if not p.is_file() or Image.open(p).convert("RGBA").tobytes() != img.convert("RGBA").tobytes()]
        if stale:
            sys.exit(f"out of date: {', '.join(stale)}; run this script")
        print("frog_bucket: textures up to date")
        return
    for path, img in files.items():
        L.save(img, path)
        print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
    main()
