"""Crafter-style Auto Smither background from the installed 26.3 client jar.

--check verifies pixels and all 40 menu slot coordinates without starting a client.
--preview writes a labeled 16x nearest-neighbor before/after layout montage.
"""
import argparse
import io
import re
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
        crafter = asset('gui/container/crafter.png')
        after = crafter.copy()
        draw = ImageDraw.Draw(after)
        # Keep the middle input row, the large output frame, and all inventory slots.
        background = crafter.getpixel((85, 20))
        draw.rectangle((25, 16, 78, 33), fill=background)
        draw.rectangle((25, 52, 78, 69), fill=background)
        after.alpha_composite(asset('gui/sprites/container/crafter/unpowered_redstone.png'), (97, 35))
        menu = (ROOT / 'common/src/shared/java/com/simplebuilding/screen/AutoSmitherMenu.java').read_text(encoding='utf-8')
        inputs = [(int(x), int(y)) for x, y in re.findall(r'new InputSlot\(container, AutoSmitherBlockEntity\.\w+, (\d+), (\d+),', menu)]
        output = re.search(r'new Slot\(container, RESULT_SLOT, (\d+), (\d+)\)', menu)
        assert inputs == [(26, 35), (44, 35), (62, 35)]
        assert output and tuple(map(int, output.groups())) == (134, 35)
        assert 'addStandardInventorySlots(inventory, 8, 84)' in menu
        for x, y in [*inputs,
                     *[(8 + 18 * col, 84 + 18 * row) for row in range(3) for col in range(9)],
                     *[(8 + 18 * col, 142) for col in range(9)]]:
            box = (x - 1, y - 1, x + 17, y + 17)
            assert crafter.crop(box).tobytes() == after.crop(box).tobytes(), (x, y)
        # The 16x16 item is centered within Crafter's 26x26 result frame.
        assert crafter.crop((129, 30, 155, 56)).tobytes() == after.crop((129, 30, 155, 56)).tobytes()
        if args.check:
            assert TARGET.exists() and Image.open(TARGET).convert('RGBA').tobytes() == after.tobytes(), TARGET
        else:
            TARGET.parent.mkdir(parents=True, exist_ok=True)
            after.save(TARGET)
        if args.preview:
            before = asset('gui/container/smithing.png')
            ImageDraw.Draw(before).rectangle((6, 6, 40, 40), fill=before.getpixel((42, 40)))
            old = before.crop((0, 0, 176, 166))
            new = after.crop((0, 0, 176, 166))
            for panel, positions, title_pos in [(old, [(8, 48), (26, 48), (44, 48)], (44, 15)),
                                                (new, inputs, None)]:
                for (x, y), icon in zip(positions, ['smithing_template_netherite_upgrade', 'sword', 'ingot']):
                    panel.alpha_composite(asset('gui/sprites/container/slot/' + icon + '.png'), (x, y))
                draw = ImageDraw.Draw(panel)
                if title_pos is None:
                    width = draw.textlength('Auto Smither', font_size=9)
                    title_pos = ((176 - width) / 2, 6)
                draw.text(title_pos, 'Auto Smither', fill='#404040', font_size=9)
                draw.text((8, 72), 'Inventory', fill='#404040', font_size=9)
            preview = Image.new('RGB', (176 * 32 + 48, 166 * 16 + 72), '#20242b')
            draw = ImageDraw.Draw(preview)
            draw.text((16, 12), 'A: VORHER - Schmiedetisch', fill='white', font_size=32)
            draw.text((176 * 16 + 32, 12), 'B: NACHHER - Crafter (Layoutmontage)', fill='white', font_size=32)
            for x, panel in [(16, old), (176 * 16 + 32, new)]:
                preview.paste(panel.resize((176 * 16, 166 * 16), Image.Resampling.NEAREST), (x, 56))
            args.preview.parent.mkdir(parents=True, exist_ok=True)
            preview.save(args.preview)
    print('Auto Smither GUI: Crafter pixels and 40 menu slot fields OK')


if __name__ == '__main__':
    main()
