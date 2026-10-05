"""Horseshoe smithing template, round 5: the round-4 shapes one pixel lower, engraved like the Vanilla arrow.

Owner 2026-10-05: "R4 please, but one pixel lower and notched into the plate like the arrow on the other templates".
Pixel analysis of netherite_upgrade_smithing_template (26.3): the cyan arrow sits in a recess. Plate pixels directly
ABOVE the motif (and beside its upper half) are the darkest plate tone (81,21,21 against body 114,50,50), plate pixels
directly BELOW it (and beside its lower half) the light plate tone (133,66,66); inside the arrow the upper-left faces
are the dark cyan, the bottom row too, the right faces mid, the interior bright. Same rule here with the copper/iron convention: shadow rim = copper (138,65,41),
light rim = copper (231,124,86) - both from the Vanilla copper_ingot ramp, the plate's own palette family - and iron
from the iron_ingot ramp: top/left/bottom faces 168, right faces 216, interior 255. Plate pixels enclosed by the
motif (two or more iron neighbours) take the shadow rim.
Shapes are round 4's iron cells (H keeps its dark groove, B its dark calkins), moved down one pixel.
Background rules and checks are round 2's (pixel-identical plate).

Usage (Pillow): python tools/textures/horseshoe_template_motif_round5_2026_10_05.py [preview png]
Not installed - the owner picks one."""
import os
import sys
from io import BytesIO
from zipfile import ZipFile

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import horseshoe_template_motif_2026_10_04 as r2  # noqa: E402
import horseshoe_template_motif_round3_2026_10_04 as r3  # noqa: E402
import horseshoe_template_motif_round4_2026_10_05 as r4  # noqa: E402

PREVIEW = r'C:\Users\o_o\code\minecraft-mods\previews\hufeisen-vorlage-runde5-vorschau.png'
SHADOW, LIGHT = 'p', 'c'          # rim colours (copper ramp)
DARK, MID, BRIGHT = 'M', 'L', 'H'  # iron faces


def shape(letter, grid):
    """Round-4 iron cells moved down one row; fixed tokens survive (groove D, B's calkins)."""
    cells = {}
    for r, row in enumerate(grid):
        for c, s in enumerate(row):
            if s in 'HLMSDK':
                keep = s == 'D' or (letter == 'H' and s == 'S') or (letter == 'B' and r == 1)
                cells[(c, r + 1)] = s if keep else None
    assert max(r for _, r in cells) < r2.FIELD_H, letter
    return cells


def engrave(letter, cells):
    rows = [r for _, r in cells]
    cols = [c for c, _ in cells]
    cy = (min(rows) + max(rows)) / 2
    cx = (min(cols) + max(cols)) / 2
    grid = [['.'] * r2.FIELD_W for _ in range(r2.FIELD_H)]
    for (c, r), fixed in cells.items():
        up, down = (c, r - 1) not in cells, (c, r + 1) not in cells
        left, right = (c - 1, r) not in cells and c < cx, (c + 1, r) not in cells and c > cx
        grid[r][c] = fixed or (DARK if up or down or left else MID if right else BRIGHT)
    deep = letter == 'F'   # F: the "outline" variant, deeper notch on all eight neighbours
    for r in range(r2.FIELD_H):
        for c in range(r2.FIELD_W):
            if (c, r) in cells:
                continue
            below, above = (c, r + 1) in cells, (c, r - 1) in cells
            side = (c - 1, r) in cells or (c + 1, r) in cells
            diag = any((c + dc, r + dr) in cells for dc in (-1, 1) for dr in (-1, 1))
            if sum(((c, r + 1) in cells, (c, r - 1) in cells, (c - 1, r) in cells, (c + 1, r) in cells)) >= 2:
                grid[r][c] = 'o' if deep else SHADOW      # enclosed (inside the U / the slot): in the recess shadow
            elif below:
                grid[r][c] = 'o' if deep else SHADOW
            elif above:
                grid[r][c] = LIGHT
            elif side:
                grid[r][c] = ('o' if deep else SHADOW) if r <= cy else LIGHT
            elif deep and diag:
                grid[r][c] = SHADOW if r <= cy else LIGHT
    return [''.join(row) for row in grid]


def vanilla(name):
    with ZipFile(r3.JAR) as z:
        return Image.open(BytesIO(z.read(f'assets/minecraft/textures/item/{name}.png'))).convert('RGBA')


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else PREVIEW
    rows = []
    for letter, title, desc, grid in r4.VARIANTS:
        cells = shape(letter, grid)
        g5 = engrave(letter, cells)
        new_im, new_motif = r2.render(g5)
        r2.check(new_im, new_motif)
        old_im, old_motif = r2.render(grid)
        osz, nsz = r3.iron_box(old_im, old_motif), r3.iron_box(new_im, new_motif)
        assert osz == nsz, (letter, osz, nsz)
        top = min(y for x, y in new_motif if new_im.getpixel((x, y))[:3] in r3.IRON)
        print(f'{letter}: Eisen {nsz[0]}x{nsz[1]}, oben y={top}  ' + ' | '.join(g5))
        rows.append((letter, title, old_im, new_im, nsz))

    s, pad, gap = 16, 20, 50
    t = 16 * s
    group = 2 * t + 12
    cols = 3
    w = pad + cols * (group + gap)
    row_h = t + 60
    h = 60 + row_h + ((len(rows) + cols - 1) // cols) * row_h
    img = Image.new('RGB', (w, h), (198, 198, 198))
    d = ImageDraw.Draw(img)
    d.text((pad, 12), 'Hufeisen-Vorlage Runde 5 - Formen aus Runde 4, 1 px tiefer, eingekerbt wie der Vanilla-Pfeil '
                      '(Schattenkante oben, Lichtkante unten). Hintergrund pixelgleich.', fill=(20, 20, 20))
    d.text((pad, 28), '16x gross, darunter 1x im Inventar-Slot. Je Buchstabe: Runde 4 | Runde 5. NICHT eingebaut.',
           fill=(60, 60, 60))
    for k, (im, cap) in enumerate(((vanilla('netherite_upgrade_smithing_template'), 'Vanilla Netherit-Vorlage'),
                                   (r2.installed(), 'jetzt eingebaut'))):
        x = pad + k * (t + 12)
        r3.tile(d, img, im, x, 60, s)
        d.text((x + 26, 60 + t + 10), cap, fill=(30, 30, 30))
    for i, (letter, title, old, new, nsz) in enumerate(rows):
        gx = pad + (i % cols) * (group + gap)
        gy = 60 + row_h + (i // cols) * row_h
        d.text((gx, gy), f'{letter} - {title}', fill=(0, 0, 0))
        for k, (im, cap) in enumerate(((old, 'R4'), (new, f'R5 {nsz[0]}x{nsz[1]}'))):
            x = gx + k * (t + 12)
            r3.tile(d, img, im, x, gy + 16, s)
            d.text((x + 26, gy + 16 + t + 10), cap, fill=(30, 30, 30))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print('ok: Hintergrund pixelgleich ->', path)


if __name__ == '__main__':
    main()
