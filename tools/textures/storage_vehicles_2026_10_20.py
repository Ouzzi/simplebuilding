"""Simple Storage (split stage 8b-2, docs/ai/PLAN-B-S8C-STORAGE-2026-10-20.md): the chest cart and chest boat/raft overlay
item textures, drawn by vehicles_2026_10_10 (the art did not change, only its namespace). Vanilla-derived (Vanilla
minecart / chest boat masks): built, never committed."""
from __future__ import annotations

import vehicles_2026_10_10 as vehicles


def derived(jar):
    """Texture path under assets/simplestorage/textures/ -> image, for tools/textures/vanilla_derived.py --module."""
    return vehicles.derived_storage(jar)
