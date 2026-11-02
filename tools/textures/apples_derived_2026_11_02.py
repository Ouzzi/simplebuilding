"""Vanilla-derived Netherite and Enderite apple (B02, 02.11.2026): the Vanilla golden apple on the ingot ramps, built from the client jar.

Owner 02.11.: every apple is a plain recolour of the Vanilla (golden) apple, generated in the build, no Mojang pixels committed.
Recipe unchanged from foods_settled_2026_10_02.py (Netherite = whole sprite on the netherite ramp, stem/leaf in the metal tones;
Enderite = that sprite on the enderite ramp plus the mod's enderite glimmer from generate_textures.ENDERITE_GLIMMER).
The enchanted apples use the same textures. Hook of tools/textures/vanilla_derived.py (derived(jar))."""
from __future__ import annotations

import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))

OUT = ('item/netherite_apple.png', 'item/enderite_apple.png')


def derived(jar) -> dict:
    import zipfile
    saved = sys.argv
    sys.argv = [sys.argv[0]]  # the old proposal scripts read argv at import
    try:
        import food_proposals_2026_10_02 as food
        import proposal_sheet_2026_10_02 as ps
        import generate_textures as gt
    finally:
        sys.argv = saved
    with tempfile.TemporaryDirectory() as tmp:
        (Path(tmp) / 'item').mkdir()
        with zipfile.ZipFile(jar) as z:
            (Path(tmp) / 'item/golden_apple.png').write_bytes(z.read('assets/minecraft/textures/item/golden_apple.png'))
        food.V = tmp
        nether = food.build('golden_apple', 'netherite', food.NETHERITE, 'metal')
        ender = ps.recolor(nether, food.ENDERITE).convert('RGBA')
    for (x, y), colour in gt.glimmer_pixels(ender, 'item/enderite_apple.png').items():
        ender.putpixel((x, y), tuple(colour) + (255,))
    return {'item/netherite_apple.png': nether, 'item/enderite_apple.png': ender}
