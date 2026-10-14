#!/usr/bin/env python3
"""Preview (and check) of the dyed backpacks and bundles: every tier x the 16 Vanilla dye colors.

Renders what the item models do: layer0 (*_dyed.png) MULTIPLIED by the dye color (minecraft:dye tint source,
tint index 0), layer1 (*_dyed_overlay.png) on top, untinted. 16x enlarged cells plus the undyed item in front.
Dye colors are StorageDyes.COLOURS (what a dye recipe puts into minecraft:dyed_color on 26.3).

Usage: python3 tools/textures/dyed_storage_preview.py [out.png] [--check]
  --check  exit 1 when a dye would not show as its color (see problems()): every effective *_dyed layer (item, placed
           block, worn backpack entity) must be neutral grey, has an _overlay twin, and the 16 dye recipes must give
           the StorageDyes color of their dye.
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
# 26.3 ships the neutral grey *_dyed layers from the overlay (tools/textures/storage_dye_palette.py --write); the
# 26.2 line keeps the tier-toned layers in src/main. The overlay wins, as in mergeResources263.
ROOTS = (ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures',
         ROOT / 'src/main/resources/assets/simplebuilding/textures')
TEX_DIRS = tuple(r / 'item' for r in ROOTS)
def _storage_dyes() -> dict[str, int]:
    """The 26.3 dye table: StorageDyes.COLOURS (measured leather tones of the Vanilla bundle sprites)."""
    import re
    text = (ROOT / 'common/src/shared/java/com/simplebuilding/util/StorageDyes.java').read_text('utf-8')
    return {m.group(1).lower(): int(m.group(2), 16)
            for m in re.finditer(r'Map\.entry\(DyeColor\.([A-Z_]+), 0x([0-9A-Fa-f]{6})\)', text)}


DYES = _storage_dyes()
TIERS = ('', 'reinforced_', 'netherite_', 'enderite_')
KINDS = ('backpack', 'bundle', 'quiver')  # the basic bundle is Vanilla's minecraft:bundle (16 Vanilla colors), not ours


def tiers(kind: str):
    return TIERS[1:] if kind == 'bundle' else TIERS
NEUTRAL_TOL = 3  # max channel spread of a dyed layer pixel (grey layers: 0)


def path(name: str) -> Path:
    return next((d / f'{name}.png' for d in TEX_DIRS if (d / f'{name}.png').is_file()), TEX_DIRS[-1] / f'{name}.png')


def load(name: str) -> Image.Image:
    return Image.open(path(name)).convert('RGBA')


def tint(img: Image.Image, rgb: int) -> Image.Image:
    r, g, b = rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            p = px[x, y]
            px[x, y] = (p[0] * r // 255, p[1] * g // 255, p[2] * b // 255, p[3])
    return out


def render(prefix: str, kind: str, rgb: int | None) -> Image.Image:
    base = f'{prefix}{kind}'
    if rgb is None:
        return load(base)
    img = tint(load(f'{base}_dyed'), rgb)
    img.alpha_composite(load(f'{base}_dyed_overlay'))
    return img


def layers() -> dict[str, Path]:
    """Effective (overlay wins) *_dyed.png of items, placed blocks and the worn backpack: name -> path."""
    out = {}
    for sub in ('item', 'block', 'entity/backpack'):
        for root in reversed(ROOTS):
            d = root / sub
            if d.is_dir():
                for f in d.glob('*_dyed.png'):
                    out[f'{sub}/{f.stem}'] = f
    return out


def problems() -> list[str]:
    """What must hold for a dye to show as ITS color (26.3: tint = StorageDyes color on a neutral grey layer)."""
    import json
    import re
    out = []
    for name, f in sorted(layers().items()):
        img = Image.open(f).convert('RGBA')
        px = [p for p in img.getdata() if p[3] > 0]  # noqa
        if not px:
            out.append(f'{name}: empty')
            continue
        sat = max(max(p[:3]) - min(p[:3]) for p in px)
        if sat > NEUTRAL_TOL:
            out.append(f'{name}: dyed layer is not neutral grey (saturation {sat}) - a dye turns muddy; '
                       f'run tools/textures/storage_dye_palette.py --write-only')
    # every worn/placed/item layer pair must exist: layer + _overlay, same mask union as the plain texture
    for name, f in sorted(layers().items()):
        if not (f.parent / (f.stem + '_overlay.png')).is_file() and not any(
                (r / name).with_name(f.stem + '_overlay.png').is_file() for r in ROOTS):
            out.append(f'{name}: no _overlay twin')
    # recipes: <color>_dyed_storage -> minecraft:<color>_dye -> dyed_color == StorageDyes
    rdir = ROOT / 'mc26_3/overlay/resources/data/simplebuilding/recipe'
    for dye, rgb in DYES.items():
        f = rdir / f'{dye}_dyed_storage.json'
        if not f.is_file():
            out.append(f'recipe {dye}_dyed_storage.json missing')
            continue
        r = json.loads(f.read_text('utf-8'))
        if r.get('material') != f'minecraft:{dye}_dye':
            out.append(f'recipe {dye}_dyed_storage: material {r.get("material")} is not minecraft:{dye}_dye')
        got = r.get('result', {}).get('components', {}).get('minecraft:dyed_color')
        if got != rgb:
            out.append(f'recipe {dye}_dyed_storage: dyed_color {got} != StorageDyes {rgb} (0x{rgb:06X})')
    # item definitions: every *_dyed model carries exactly the dye tint (tint index 0 = layer0); its model puts
    # <x>_dyed on layer0 (tinted) and <x>_dyed_overlay on layer1 (untinted)
    gen = ROOT / 'src/main/generated/assets/simplebuilding'

    def walk(node, who):
        if isinstance(node, dict):
            m = node.get('model')
            if node.get('type') == 'minecraft:model' and isinstance(m, str) and m.endswith('_dyed'):
                tints = node.get('tints') or []
                if [t.get('type') for t in tints] != ['minecraft:dye']:
                    out.append(f'{who}: {m} needs exactly one minecraft:dye tint, has {tints}')
                mf = gen / 'models' / (m.split(':', 1)[1] + '.json')
                tex = json.loads(mf.read_text('utf-8')).get('textures', {}) if mf.is_file() else {}
                if not (tex.get('layer0', '').endswith('_dyed') and tex.get('layer1', '') == tex.get('layer0', '') + '_overlay'):
                    out.append(f'{who}: model {m} layer0/layer1 are not <x>_dyed / <x>_dyed_overlay ({tex})')
            for v in node.values():
                walk(v, who)
        elif isinstance(node, list):
            for v in node:
                walk(v, who)

    for f in sorted((gen / 'items').glob('*.json')):
        walk(json.loads(f.read_text('utf-8')), f'item {f.stem}')
    if len(DYES) != 16:
        out.append(f'StorageDyes has {len(DYES)} colors, expected 16')
    return out


def entity_sheet(out: Path) -> None:
    """Worn backpack (textures/entity/backpack, 64x32 atlas): the old tier-toned layer (src/main, what 26.3 shipped
    before) above the neutral grey one (26.3 overlay) for every dye of the backpack tiers, Blue and Red first."""
    cell_w, cell_h, scale, pad = 64 * 2, 32 * 2, 2, 4
    order = list(DYES)
    sheet = Image.new('RGBA', (pad + len(order) * (cell_w + pad), pad + len(TIERS) * 2 * (cell_h + pad)), (198, 198, 198, 255))
    ImageDraw.Draw(sheet)
    for ti, prefix in enumerate(TIERS):
        for variant, root in enumerate((ROOTS[1], ROOTS[0])):
            base = f'{prefix}backpack'
            for ci, dye in enumerate(order):
                def ld(n):
                    f = root / 'entity/backpack' / f'{n}.png'
                    return Image.open(f if f.is_file() else ROOTS[1] / 'entity/backpack' / f'{n}.png').convert('RGBA')
                img = tint(ld(f'{base}_dyed'), DYES[dye])
                img.alpha_composite(ld(f'{base}_dyed_overlay'))
                x, y = pad + ci * (cell_w + pad), pad + (ti * 2 + variant) * (cell_h + pad)
                sheet.alpha_composite(img.resize((cell_w, cell_h), Image.NEAREST), (x, y))
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print('preview:', out)


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    if '--check' in sys.argv:
        probs = problems()
        print('\n'.join(probs) or 'dyed storage tints: ok')
        return 1 if probs else 0
    out = Path(args[0] if args else ROOT / 'docs/ai/previews/oc-missingtex/dyed-storage-colors.png')
    scale, cell, pad, label = 4, 64, 6, 14
    cols = 1 + len(DYES)
    rows = sum(len(tiers(k)) for k in KINDS)
    w, h = pad + cols * (cell + pad) + 90, pad + rows * (cell + pad + label)
    sheet = Image.new('RGBA', (w, h), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    r = 0
    for kind in KINDS:
        for prefix in tiers(kind):
            y = pad + r * (cell + pad + label)
            d.text((w - 86, y + cell // 2), f'{prefix}{kind}', fill=(0, 0, 0, 255))
            for c, (name, rgb) in enumerate([('undyed', None)] + list(DYES.items())):
                x = pad + c * (cell + pad)
                cellimg = render(prefix, kind, rgb).resize((cell, cell), Image.NEAREST)
                sheet.alpha_composite(cellimg, (x, y))
                d.text((x, y + cell + 1), name[:11], fill=(40, 40, 40, 255))
            r += 1
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print('preview:', out)
    entity_sheet(out.with_name('worn-backpack-before-after.png'))
    return 0


if __name__ == '__main__':
    sys.exit(main())
