"""Simple Ovens (split plan stage 4): the furnace minecart item textures, drawn by vehicles_2026_10_10 (the art did
not change, only its namespace). Vanilla-derived (Vanilla minecart overlay recolour): built, never committed."""
from __future__ import annotations

import vehicles_2026_10_10 as vehicles


def derived(jar):
    """Texture path under assets/simpleovens/textures/ -> image, for tools/textures/vanilla_derived.py --module."""
    return {path: img for path, img in vehicles.derived(jar).items() if path.endswith('_furnace_minecart.png')}
