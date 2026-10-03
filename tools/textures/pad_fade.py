"""Generate 26.3 pad fade steps from the existing owner textures, without redrawing pixels."""
import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'src/main/resources/assets/simplebuilding/textures/block'
TARGET = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures/block'
PADS = (
    'elytra_pad', 'reinforced_elytra_pad', 'netherite_elytra_pad', 'enderite_elytra_pad', 'fine_elytra_pad',
    'flypad_ender', 'reinforced_flypad_ender', 'stellar_flypad_ender',
    'spawn_teleporter', 'spawn_teleporter_tier_2', 'enderite_spawn_teleporter',
    'potion_pad', 'reinforced_potion_pad', 'infused_potion_pad',
)


def images():
    for name in PADS:
        base = Image.open(SOURCE / f'{name}.png').convert('RGBA')
        effects = ('active', 'cooling') if 'potion' in name else ('active',)
        for effect in effects:
            source = SOURCE / f'{name}_{effect}.png'
            active = Image.open(source).convert('RGBA')
            assert base.size == (16, 16) and active.width == 16 and active.height % 16 == 0
            tiled = Image.new('RGBA', active.size)
            for y in range(0, active.height, 16):
                tiled.paste(base, (0, y))
            metadata = source.with_suffix('.png.mcmeta')
            for step in (1, 2):
                yield f'{name}_{effect}_fade_{step}.png', Image.blend(tiled, active, step / 3), metadata


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    problems = []
    for name, image, metadata in images():
        path = TARGET / name
        if args.check:
            if not path.exists() or Image.open(path).convert('RGBA').tobytes() != image.tobytes():
                problems.append(str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            image.save(path)
        if metadata.exists():
            output = path.with_suffix('.png.mcmeta')
            expected = json.loads(metadata.read_text())
            if args.check:
                if not output.exists() or json.loads(output.read_text()) != expected:
                    problems.append(str(output.relative_to(ROOT)))
            else:
                output.write_text(json.dumps(expected, indent=2) + '\n', encoding='utf-8')
    if problems:
        raise SystemExit('Pad fade mismatch: ' + ', '.join(problems))
    if not args.check:
        preview()
    print('Pad fade: 34 textures and 6 animation metadata files OK')


def preview():
    # Sixteenfold nearest-neighbor view: original off/on, then the complete new fade.
    rows = [(name, 'active') for name in PADS]
    rows += [(name, 'cooling') for name in PADS if 'potion' in name]
    sheet = Image.new('RGB', (6 * 256, len(rows) * 286 + 28), '#20252a')
    draw = ImageDraw.Draw(sheet)
    for col, label in enumerate(('A Alt: aus', 'B Alt: sofort an', 'C Neu: 0%', 'D Neu: 33%', 'E Neu: 67%', 'F Neu: 100%')):
        draw.text((col * 256 + 8, 8), label, fill='white')
    for row, (name, effect) in enumerate(rows):
        y = 28 + row * 286
        draw.text((8, y), f'{name} ({effect})', fill='white')
        paths = [SOURCE / f'{name}.png', SOURCE / f'{name}_{effect}.png', SOURCE / f'{name}.png',
                 TARGET / f'{name}_{effect}_fade_1.png', TARGET / f'{name}_{effect}_fade_2.png',
                 SOURCE / f'{name}_{effect}.png']
        for col, path in enumerate(paths):
            frame = Image.open(path).convert('RGBA').crop((0, 0, 16, 16))
            sheet.paste(frame.resize((256, 256), Image.Resampling.NEAREST), (col * 256, y + 22))
    output = ROOT / 'previews/pad-fade-vorschau.png'
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output)


if __name__ == '__main__':
    main()
