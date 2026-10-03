"""Usage: python tools/textures/astralit_nihilit_alternates_2026_10_03.py <vanilla textures dir>

Owner 2026-10-03: the new textures A, B, C of astralit_nihilit_proposals_2026_10_03.py become three alternative
blocks per material (the base block stays as it is). Writes block textures into src/main/resources (shared by
26.2 and 26.3):
- Astralit: veined_astralit (A, calcite pattern), crystalline_astralit (B, amethyst-block pattern),
  layered_astralit (C, dripstone pattern);
- Nihilith: veined_nihilith (A, calcite), crystalline_nihilith (B, amethyst block), frosted_nihilith (C, packed ice).
All tile (the vanilla patterns tile; the recolouring is per pixel)."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
sys.argv = [sys.argv[0], V]
import astralit_nihilit_proposals_2026_10_03 as prop  # noqa: E402

prop.V = V
NAMES = {
    'astralit_block': ('pink', ('calcite', 'amethyst_block', 'dripstone_block'),
                       ('veined_astralit', 'crystalline_astralit', 'layered_astralit')),
    'nihilith_block': ('blue', ('calcite', 'amethyst_block', 'packed_ice'),
                       ('veined_nihilith', 'crystalline_nihilith', 'frosted_nihilith')),
}


def main():
    for base, (hue, sources, names) in NAMES.items():
        _, variants = prop.variants(base, hue, sources)
        for (label, im), name in zip(variants[:3], names):
            im.save(os.path.join(prop.BLOCK, name + '.png'))
            print(name, '<-', label)
    print('ok')


if __name__ == '__main__':
    main()
