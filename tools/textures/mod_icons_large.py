"""Large listing icons for Modrinth / CurseForge: nearest-neighbour 128 and 512 px copies of the 16x16 mod icons.

Source: <assets>/<modid>/icon.png (written by tools/textures/mod_icons.py, own pixel art; no Mojang pixels:
none of the ITEM_MOTIFS textures is listed in tools/textures/vanilla_derived.py, and this script refuses to run
when one is). Output: docs/release/icons/<modid>-128.png and <modid>-512.png.

    python3.12 tools/textures/mod_icons_large.py          write
    python3.12 tools/textures/mod_icons_large.py --check  files match the 16x16 icons

Run by wiki/generate.py --all (so every bundle's gen step refreshes them).
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers as L  # noqa: E402
import mod_icons as M  # noqa: E402

OUT = M.ROOT / "docs/release/icons"
SIZES = (128, 512)


def outputs() -> dict[Path, bytes]:
    files = {}
    for entry in M.modules():
        src = M.asset_dir(entry) / "icon.png"
        w, h, px = L.png_read(src.read_bytes())
        if (w, h) != (16, 16):
            raise SystemExit(f"{src}: expected 16x16, got {w}x{h}")
        mod_id = entry.get("modId", entry["id"])
        for s in SIZES:
            files[OUT / f"{mod_id}-{s}.png"] = L.png_write(s, s, M.upscale(bytes(px), 16, 16, s // 16))
    return files


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args(argv)
    bad = []
    files = outputs()
    for path, data in files.items():
        if M.same_pixels(path, data):
            continue
        if args.check:
            bad.append(path.relative_to(M.ROOT).as_posix())
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
    if args.check and OUT.is_dir():
        bad += [p.relative_to(M.ROOT).as_posix() for p in OUT.glob("*.png") if p not in files]
    if bad:
        print("large mod icons out of date (run python3.12 tools/textures/mod_icons_large.py):", *bad, sep="\n  ")
        return 1
    print(f"{len(files)} large icons ({len(files) // len(SIZES)} mods x {SIZES})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
