#!/usr/bin/env python3
"""Every mod of modules/modules.json has its 16x16 pixel-art icon in every loader (docs/ai/PRINZIPIEN.md A8 c,
docs/ai/PLAN-B-MODICONS-2026-10-21.md).

Fabric `icon` and NeoForge `iconFile` name assets/<modid>/icon.png (16x16, layer source icon.layers.json next to
it); Forge `logoFile` names assets/<modid>/icon_128.png (8x nearest-neighbour copy) with `logoBlur=false`.
The files themselves are kept in sync with tools/textures/mod_icons.py (`--check`, run here too).
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools/textures"))
import layers as L  # noqa: E402
import mod_icons  # noqa: E402


def resources(entry: dict, loader: str) -> list[Path]:
    """Resource roots that end up in the loader jar of this mod."""
    if entry["paths"]["root"] == ".":
        own = {"fabric": "src/main/resources", "neoforge": "neoforge/src/main/resources",
               "forge": "forge/src/main/resources"}[loader]
        return [ROOT / own, ROOT / "src/main/resources"]
    base = ROOT / entry["paths"]["root"]
    return [ROOT / entry["paths"][loader] / "src/main/resources", base / "shared/resources"]


def descriptor(entry: dict, loader: str) -> Path:
    if loader == "fabric":
        return resources(entry, loader)[0] / "fabric.mod.json"
    return resources(entry, loader)[0] / "META-INF" / ("neoforge.mods.toml" if loader == "neoforge" else "mods.toml")


def problems(entries: list[dict]) -> list[str]:
    out = []
    for entry in entries:
        mid = entry.get("modId", entry["id"])
        icon, logo = f"assets/{mid}/icon.png", f"assets/{mid}/icon_128.png"
        for loader in entry["loaders"]:
            path = descriptor(entry, loader)
            rel = path.relative_to(ROOT).as_posix()
            if not path.is_file():
                out.append(f"{rel}: missing descriptor")
                continue
            text = path.read_text(encoding="utf-8")
            if loader == "fabric":
                value, want = json.loads(text).get("icon"), icon
                key = "icon"
            else:
                # plain line match: the root descriptors carry ${...} placeholders that are no valid TOML
                key = "iconFile" if loader == "neoforge" else "logoFile"
                m = re.search(rf'^{key}\s*=\s*"([^"]*)"', text, re.M)
                value, want = m and m.group(1), icon if loader == "neoforge" else logo
                if loader == "forge" and not re.search(r"^logoBlur\s*=\s*false\s*$", text, re.M):
                    out.append(f"{rel}: logoBlur=false missing (pixel art must not be filtered)")
            if value != want:
                out.append(f"{rel}: {key} is {value!r}, expected {want!r}")
                continue
            if not any((r / want).is_file() for r in resources(entry, loader)):
                out.append(f"{rel}: {want} not found in the {loader} resources")
        base = mod_icons.asset_dir(entry)
        png = base / "icon.png"
        if not png.is_file():
            continue
        w, h, rgba = L.png_read(png.read_bytes())
        if (w, h) != (16, 16):
            out.append(f"{png.relative_to(ROOT).as_posix()}: {w}x{h}, must be 16x16 pixel art")
            continue
        if not (base / "icon.layers.json").is_file():
            out.append(f"{png.relative_to(ROOT).as_posix()}: layer source icon.layers.json missing (A8 b)")
        big = base / "icon_128.png"
        if big.is_file():
            bw, bh, brgba = L.png_read(big.read_bytes())
            if (bw, bh) != (128, 128) or bytes(brgba) != mod_icons.upscale(bytes(rgba), 16, 16, 8):
                out.append(f"{big.relative_to(ROOT).as_posix()}: not the 8x nearest-neighbour copy of icon.png")
    return out


def main() -> int:
    found = problems(mod_icons.modules())
    if found:
        print("Mod icons:", *found, sep="\n  ")
        print("Fix: draw the icon in tools/textures/mod_icons.py, run it, add the descriptor lines.")
        return 1
    status = mod_icons.main(["--check"])
    if status == 0:
        print(f"Mod icons OK ({len(mod_icons.modules())} mods).")
    return status


if __name__ == "__main__":
    raise SystemExit(main())
