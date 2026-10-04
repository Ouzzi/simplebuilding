"""Remove the vanilla hammer decoration without changing smithing slot geometry.

Uses the installed 26.3 client jar, no downloads. --check compares pixels.
--preview writes a labeled 16x nearest-neighbor before/after GUI comparison.
"""
import argparse
import io
from pathlib import Path
from zipfile import ZipFile

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
TARGET = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures/gui/container/auto_smither.png'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, default=Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar')
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--preview', type=Path)
    args = parser.parse_args()
    with ZipFile(args.jar) as jar:
        def asset(name):
            return Image.open(io.BytesIO(jar.read('assets/minecraft/textures/' + name))).convert('RGBA')
        before = asset('gui/container/smithing.png')
        after = before.copy()
        # The entire decoration lies above the first slot row; preserve frame and slots.
        ImageDraw.Draw(after).rectangle((6, 6, 40, 40), fill=before.getpixel((42, 40)))
        # Slot borders must remain exactly where the menu puts its item cells.
        for x, y in [(8, 48), (26, 48), (44, 48), (98, 48),
                     *[(8 + 18 * col, 84 + 18 * row) for row in range(3) for col in range(9)],
                     *[(8 + 18 * col, 142) for col in range(9)]]:
            box = (x - 1, y - 1, x + 17, y + 17)
            assert before.crop(box).tobytes() == after.crop(box).tobytes(), (x, y)
        if args.check:
            assert TARGET.exists() and Image.open(TARGET).convert('RGBA').tobytes() == after.tobytes(), TARGET
        else:
            TARGET.parent.mkdir(parents=True, exist_ok=True)
            after.save(TARGET)
        if args.preview:
            old = before.crop((0, 0, 176, 166))
            old.alpha_composite(asset('gui/sprites/container/crafter/unpowered_redstone.png'), (133, 48))
            new = after.crop((0, 0, 176, 166))
            for panel in (old, new):
                panel.alpha_composite(asset('gui/sprites/container/slot/smithing_template_armor_trim.png'), (8, 48))
                draw = ImageDraw.Draw(panel)
                draw.text((44, 15), 'Auto Smither', fill='#404040', font_size=9)
                draw.text((8, 72), 'Inventory', fill='#404040', font_size=9)
            preview = Image.new('RGB', (176 * 32 + 48, 166 * 16 + 72), '#20242b')
            draw = ImageDraw.Draw(preview)
            draw.text((16, 12), 'A: VORHER', fill='white', font_size=32)
            draw.text((176 * 16 + 32, 12), 'B: NACHHER (gueltig / leer)', fill='white', font_size=32)
            for x, panel in [(16, old), (176 * 16 + 32, new)]:
                preview.paste(panel.resize((176 * 16, 166 * 16), Image.Resampling.NEAREST), (x, 56))
            args.preview.parent.mkdir(parents=True, exist_ok=True)
            preview.save(args.preview)
    print('Auto Smither GUI: pixels and 40 slot fields OK')


if __name__ == '__main__':
    main()
