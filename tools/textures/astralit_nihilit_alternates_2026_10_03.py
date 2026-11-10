"""Astralit / Nihilith alternative blocks (owner 2026-10-03): the new textures A, B, C of
astralit_nihilit_proposals_2026_10_03.py, three alternative blocks per material (the base block stays as it is):
- Astralit: veined_astralit (A, calcite pattern), crystalline_astralit (B, amethyst-block pattern),
  layered_astralit (C, dripstone pattern);
- Nihilith: veined_nihilith (A, calcite), crystalline_nihilith (B, amethyst block), frosted_nihilith (C, packed ice).
All tile (the vanilla patterns tile; the recolouring is per pixel).

Vanilla-derived (b-texrate17, 10.11.): the pattern is a Vanilla block's pixel layout recoloured by brightness rank, so the
textures are built from the 26.3 client jar by tools/textures/vanilla_derived.py (derived(jar)) and never committed.
The body colours and accent specks come from the own Astralit/Nihilith block textures as they were on 2026-10-03
(tools/textures/hand/alternates_2026_10_03/, commit 259a382d1): the textures stay pixel-identical to the approved ones
although the base blocks were reworked since (texrate15/16).

    python3 tools/textures/vanilla_derived.py --out DIR      # builds them with every other Vanilla-derived texture
    python3 tools/textures/astralit_nihilit_alternates_2026_10_03.py [--jar client.jar] OUTDIR   # only these six
"""
import io
import os
import sys
import zipfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, 'hand', 'alternates_2026_10_03')
NAMES = {
    'astralit_block': ('pink', ('calcite', 'amethyst_block', 'dripstone_block'),
                       ('veined_astralit', 'crystalline_astralit', 'layered_astralit')),
    'nihilith_block': ('blue', ('calcite', 'amethyst_block', 'packed_ice'),
                       ('veined_nihilith', 'crystalline_nihilith', 'frosted_nihilith')),
}


def _prop():
    """astralit_nihilit_proposals_2026_10_03 reads sys.argv at import time (its CLI): import it with a neutral argv."""
    if HERE not in sys.path:
        sys.path.insert(0, HERE)
    argv, sys.argv = sys.argv, [sys.argv[0]]
    try:
        import astralit_nihilit_proposals_2026_10_03 as prop
    finally:
        sys.argv = argv
    return prop


def derived(jar):
    """Vanilla-derived output for tools/textures/vanilla_derived.py: texture path -> image."""
    prop = _prop()
    out = {}
    with zipfile.ZipFile(jar) as z:
        def vblock(name):
            data = z.read(f'assets/minecraft/textures/block/{name}.png')
            return Image.open(io.BytesIO(data)).convert('RGBA').crop((0, 0, 16, 16))
        for base, (hue, sources, names) in NAMES.items():
            cur = Image.open(os.path.join(SRC, base + '.png')).convert('RGBA').crop((0, 0, 16, 16))
            cols = prop.body_colours(cur, hue)
            for src, name in zip(sources, names):
                out[f'block/{name}.png'] = prop.keep_accents(prop.matched(vblock(src), cols), cur, hue)
    return out


def main():
    args = sys.argv[1:]
    jar = None
    if args[:1] == ['--jar']:
        jar, args = args[1], args[2:]
    if not args:
        raise SystemExit(__doc__)
    if jar is None:
        import vanilla_derived
        jar = vanilla_derived.find_jar()
        if jar is None:
            raise SystemExit('26.3 client jar not found (pass --jar)')
    for rel, im in derived(jar).items():
        path = os.path.join(args[0], rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        im.save(path)
        print(rel)
    print('ok')


if __name__ == '__main__':
    main()
