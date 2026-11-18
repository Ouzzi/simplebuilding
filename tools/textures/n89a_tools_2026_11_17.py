"""N89A (b-red89a, 17.11.2026): the rose alloy horseshoes, chisels and building wands - own art only.

Owner N77 ("all rose gold variants ... everything that is missing"); plan /root/sb-tools-b/n89/PLAN-N89A.md.
Nothing here reads a Mojang texture:

- Horseshoes: the shipped horseshoe process (horseshoe_textures.horseshoe_item variant A, then the owner pick N41 E5 =
  n41_round2_2026_10_12.horseshoe(v=1)) and the worn hoof layer (horseshoe_textures.hoof_layer), both on six-tone rose
  ramps taken from the alloys' own 10-tone ramps (ores_2026_10_30.RAMPS: 'soft' = Rose Gold, 'hard' = Rose Copper).
- Chisels and building wands: the committed own gold_chisel.png / gold_building_wand.png; every head pixel (a pixel that
  differs from the iron sibling, so the wooden handle stays) recoloured onto the rose ramp - the same process as the
  optional rose gold sledgehammers (ores_2026_10_30.derived_hammers).

    python3 tools/textures/n89a_tools_2026_11_17.py          write the PNGs + layer sources
    python3 tools/textures/n89a_tools_2026_11_17.py --check  fail when a file is stale
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import horseshoe_textures as HT  # noqa: E402
import layers as L  # noqa: E402
import n41_round2_2026_10_12 as N41R2  # noqa: E402
import ores_2026_10_30 as ORES  # noqa: E402

RIDE = ROOT / 'modules/simpleriding/shared/resources/assets/simpleriding/textures'
CHISEL = ROOT / 'modules/simplechisels/shared/resources/assets/simplechisels/textures/item'
WAND = ROOT / 'modules/simplebuildingtools/shared/resources/assets/simplebuildingtools/textures/item'
ALLOYS = {'rose_bronze': 'hard', 'rose_gold': 'soft'}


def six(ramp: str) -> list[str]:
    """Six tones dark -> light (outline, shadow, mid dark, mid, light, highlight) out of the 10-tone ramp (brightest first)."""
    r = ORES.RAMPS[ramp]
    return [r[9], r[7], r[6], r[4], r[2], r[0]]


for _alloy, _ramp in ALLOYS.items():
    HT.MATERIALS[_alloy] = six(_ramp)
    N41R2.HS[_alloy] = tuple(c.lstrip('#') for c in six(_ramp))


def horseshoe_icon(alloy: str) -> Image.Image:
    base = HT.horseshoe_item(alloy, HT.ITEM_VARIANT).convert('RGBA')
    return N41R2.horseshoe(alloy, 1, base)


def head_recolour(gold: Path, iron: Path, ramp: str) -> Image.Image:
    g, i = Image.open(gold).convert('RGBA'), Image.open(iron).convert('RGBA')
    return ORES.recolor(g, ramp, ORES.differs_from(i))


def outputs() -> dict:
    out = {}
    for alloy, ramp in ALLOYS.items():
        out[RIDE / f'item/{alloy}_horseshoe.png'] = L.snap(horseshoe_icon(alloy), 'Grundform')
        out[RIDE / f'entity/horseshoe/{alloy}.png'] = L.snap(HT.hoof_layer(alloy), 'Grundform')
        out[CHISEL / f'{alloy}_chisel.png'] = L.snap(head_recolour(CHISEL / 'gold_chisel.png', CHISEL / 'iron_chisel.png', ramp), 'Grundform')
        out[WAND / f'{alloy}_building_wand.png'] = L.snap(
            head_recolour(WAND / 'gold_building_wand.png', WAND / 'iron_building_wand.png', ramp), 'Grundform')
    return out


def main(argv) -> int:
    check = '--check' in argv
    stale = []
    for path, img in outputs().items():
        same = path.exists() and Image.open(path).convert('RGBA').tobytes() == img.convert('RGBA').tobytes()
        if not same:
            stale.append(path)
            if not check:
                path.parent.mkdir(parents=True, exist_ok=True)
                L.save(img, path)
    if check and stale:
        print('stale: ' + ', '.join(str(p.relative_to(ROOT)) for p in stale))
        return 1
    print(f'n89a tools: {len(outputs())} textures {"current" if not stale else ("stale" if check else "written")}')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
