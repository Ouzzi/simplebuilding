"""Horseshoe Upgrade: the owner's Basic Upgrade plate with a Vanilla iron horseshoe.

Run with Pillow: --check verifies the installed texture; --preview PATH writes
the old/new/reference comparison at 16x nearest-neighbor scale.
"""
import argparse
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile

from PIL import Image, ImageDraw

from generate_textures import BASIC_UPGRADE_TEMPLATE_PAL, UPGRADE_TEMPLATE

ROOT = Path(__file__).resolve().parents[2]
JAR = Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
BASE = ROOT / 'src/main/resources/assets/simplebuilding/textures/item/basic_upgrade_template.png'
TARGET = ROOT / 'modules/simpleriding/shared/resources/assets/simpleriding/textures/item/horseshoe_smithing_template.png'
# Open at the top; diagonal corners stay copper instead of acquiring dark fills.
HORSESHOE = ('HH...HH', 'HL...LM', 'HL...LM', 'LM...MS', 'LM...MS', '.LM.MS.', '..MSS..')


def ramp(jar, material):
    with ZipFile(jar) as archive:
        ingot = Image.open(BytesIO(archive.read(f'assets/minecraft/textures/item/{material}_ingot.png'))).convert('RGBA')
    colors = {ingot.getpixel((x, y)) for y in range(ingot.height) for x in range(ingot.width)}
    return sorted((c for c in colors if c[3]), key=lambda c: sum(c[:3]))


def render(jar=JAR):
    base = Image.open(BASE).convert('RGBA')
    copper, iron = ramp(jar, 'copper'), ramp(jar, 'iron')
    plate = dict(zip('450132', [copper[i] for i in (0, 1, 3, 4, 6, 7)]))
    metal = dict(zip('SMLH', [iron[i] for i in (3, 5, 6, 7)]))
    result = base.copy()
    for y, row in enumerate(UPGRADE_TEMPLATE):
        for x, symbol in enumerate(row):
            if symbol != '.':
                expected = tuple(bytes.fromhex(BASIC_UPGRADE_TEMPLATE_PAL[symbol][1:])) + (255,)
                assert base.getpixel((x, y)) == expected, 'Basic texture changed; review the plate mapping'
                result.putpixel((x, y), plate.get(symbol, plate['1']))
    for y, row in enumerate(HORSESHOE, 4):
        for x, symbol in enumerate(row, 5):
            if symbol != '.':
                result.putpixel((x, y), metal[symbol])
    assert result.size == (16, 16)
    assert result.getchannel('A').tobytes() == base.getchannel('A').tobytes()
    assert all(result.getpixel((x, y))[3] == 0 for x in range(16) for y in range(16) if x in (0, 15) or y in (0, 15))
    assert all(result.getpixel((x, y)) in copper + iron for y in range(16) for x in range(16) if result.getpixel((x, y))[3])
    return result


def preview(path, result):
    from horseshoe_textures import template_item
    sheet = Image.new('RGB', (832, 304), '#c6c6c6')
    draw = ImageDraw.Draw(sheet)
    for i, (label, texture) in enumerate((('A - Vorher', template_item('A')), ('B - Nachher', result), ('C - Basic-Referenz', Image.open(BASE).convert('RGBA')))):
        x = 16 + i * 272
        draw.text((x, 10), label, fill='#202020')
        large = texture.resize((256, 256), Image.Resampling.NEAREST)
        sheet.paste(large, (x, 32), large)
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
    print('Vorschau:', path)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, default=JAR)
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--preview', type=Path)
    args = parser.parse_args()
    # Since 2026-10-05 render() is only the plate (round-1 texture) under the round-5 motif; the item texture is
    # written and checked by horseshoe_template_motif_round5_2026_10_05.py (--install / --check).
    result = render(args.jar)
    if args.check:
        from horseshoe_template_motif_round5_2026_10_05 import INSTALLED, texture
        assert Image.open(TARGET).convert('RGBA').tobytes() == texture(INSTALLED).tobytes(),             'STALE: horseshoe_smithing_template.png'
    if args.preview:
        preview(args.preview, result)
    print('Horseshoe template plate: OK (Basic silhouette, Vanilla palettes, transparent borders)')


if __name__ == '__main__':
    main()
