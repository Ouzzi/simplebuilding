"""Deterministic vanilla-based 16px art; run with Pillow, optionally --preview PATH.

The yarn sprite also supplies the existing placed_small_parts block renderer.
No separate block texture is needed: placement retains the item's exact pixels.
"""
import argparse
import io
import json
from pathlib import Path
import zipfile
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--jar', type=Path, default=Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar')
    parser.add_argument('--preview', type=Path, default=ROOT / '.ai-runs/stiller-loewenzahn-vorschau.png')
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    with zipfile.ZipFile(args.jar) as jar:
        def sprite(name):
            return Image.open(io.BytesIO(jar.read(f'assets/minecraft/textures/{name}.png'))).convert('RGBA')
        golden = sprite('block/golden_dandelion')
        vanilla = sprite('block/dandelion')
        string = sprite('item/string')
    silent = golden.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = silent.getpixel((x, y))
            if a:
                if r > b * 1.25 and r >= g * 0.9:
                    # Keep vanilla's shading and silhouette; turn gold petals into cool ivory.
                    shade = round(0.30 * r + 0.59 * g + 0.11 * b)
                    value = min(245, 110 + round(shade * 0.55))
                    silent.putpixel((x, y), (value - 5, value - 2, value, a))
                else:
                    # Quiet sage stem, still recognizably a dandelion.
                    gray = round((r + g + b) / 3)
                    silent.putpixel((x, y), ((r + gray) // 2, (g + gray) // 2, (b + gray) // 2, a))
    # Keep a transparent border around new artwork, including below the vanilla stem.
    inset = Image.new('RGBA', (16, 16))
    inset.paste(silent, (0, -1))
    silent = inset
    # 2026-10-05: the owner's own drawing, colours on the vanilla white wool (owner_round_2026_10_05.py).
    yarn = Image.open(Path(__file__).resolve().parent / 'hand' / 'yarn_ball.png').convert('RGBA')

    for name, im in [('block/silent_dandelion', silent), ('item/yarn_ball', yarn)]:
        path = ASSETS / f'textures/{name}.png'
        if args.check:
            assert path.exists() and Image.open(path).convert('RGBA').tobytes() == im.tobytes(), f'Stale texture: {path}'
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            im.save(path)

    def write(name, data):
        path = ASSETS / f'{name}.json'
        if args.check:
            assert path.exists() and json.loads(path.read_text(encoding='utf-8')) == data, f'Stale model: {path}'
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')

    for name in ('silent_dandelion', 'potted_silent_dandelion'):
        write(f'blockstates/{name}', {'variants': {'': {'model': f'simplebuilding:block/{name}'}}})
    write('models/block/silent_dandelion', {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout',
          'textures': {'cross': 'simplebuilding:block/silent_dandelion'}})
    write('models/block/potted_silent_dandelion', {'parent': 'minecraft:block/flower_pot_cross', 'render_type': 'minecraft:cutout',
          'textures': {'plant': 'simplebuilding:block/silent_dandelion'}})
    for name, texture in [('silent_dandelion', 'block/silent_dandelion'), ('yarn_ball', 'item/yarn_ball')]:
        write(f'models/item/{name}', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'simplebuilding:{texture}'}})
        write(f'items/{name}', {'model': {'type': 'minecraft:model', 'model': f'simplebuilding:item/{name}'}})

    if args.check:
        print('Silent Dandelion textures/models: OK')
        return
    preview = Image.new('RGB', (5 * 280, 320), '#34383e')
    pen = ImageDraw.Draw(preview)
    for i, (label, im) in enumerate([('A Vanilla-Loewenzahn', vanilla), ('B Goldener Loewenzahn', golden),
                                    ('C Stiller Loewenzahn', silent), ('D Vanilla-Faden', string), ('E Wollknaeuel / abgelegt', yarn)]):
        for y in range(16):
            for x in range(16):
                color = '#656970' if (x + y) % 2 else '#777b82'
                pen.rectangle((i * 280 + 12 + x * 16, 32 + y * 16, i * 280 + 27 + x * 16, 47 + y * 16), fill=color)
        preview.paste(im.resize((256, 256), Image.Resampling.NEAREST), (i * 280 + 12, 32), im.resize((256, 256), Image.Resampling.NEAREST))
        pen.text((i * 280 + 12, 300), label, fill='white')
    args.preview.parent.mkdir(parents=True, exist_ok=True)
    preview.save(args.preview)
    print(args.preview)


if __name__ == '__main__':
    main()
