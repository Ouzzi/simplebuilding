"""Preview of the crucible screen layout, old (v1) against new (v2, owner addition 11), for every tier.

Usage: python tools/textures/crucible_ui_v2_preview_2026_10_06.py  -> previews/crucible-ui-v2-vorschau.png
Mirrors the numbers of CrucibleMenu (layout) and CrucibleScreen (colours); keep both in step when the layout changes.
Shown per tier: v1 and v2 without a barrel, v2 with an attached barrel (heat high, a few slot states).
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path('C:/Users/o_o/code/minecraft-mods/previews/crucible-ui-v2-vorschau.png')
TIERS = {'Eisen': (1, 2), 'Verstaerkt': (1, 3), 'Netherit': (2, 3), 'Enderit': (3, 3)}  # grids, rows
BG, OUTLINE, LIGHT, SHADE = (198, 198, 198), (0, 0, 0), (255, 255, 255), (85, 85, 85)
SLOT_DARK, SLOT_FILL, LABEL = (55, 55, 55), (139, 139, 139), (64, 64, 64)
K = 3
FONT = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 8 * K)


def v1(grids, rows):
    gw = grids * 54 + (grids - 1) * 4
    left = 8 + 12 + 6
    w = max(176, left + gw + 10 + 54 + 8)
    h = 18 + rows * 18 + 12 + 14 + 76 + 8
    return dict(w=w, h=h, thermo=(8, 18, 12, rows * 18), grid=left, gw=gw, fire=(left, 18 + rows * 18 + 2, gw, 12),
                barrel=left + gw + 10, inv=((w - 162) // 2 + 1, h - 82), placeholders=False, inset=False)


def v2(grids, rows):
    gw = grids * 54 + (grids - 1) * 4
    content = 10 + 4 + gw + 8 + 54
    w = max(176, content + 16)
    cl = (w - content) // 2
    fire_top = 18 + rows * 18 + 2
    bottom = max(fire_top + 10, 18 + 54)
    inv_top = bottom + 15
    h = inv_top + 83
    return dict(w=w, h=h, thermo=(cl, 18, 10, fire_top + 10 - 18), grid=cl + 14, gw=gw, fire=(cl + 14, fire_top, gw, 10),
                barrel=cl + 14 + gw + 8, inv=((w - 162) // 2 + 1, inv_top), placeholders=True, inset=True)


def draw(spec, grids, rows, attached, title):
    w, h = spec['w'], spec['h']
    img = Image.new('RGB', (w, h), (41, 45, 53))
    d = ImageDraw.Draw(img)

    def fill(x0, y0, x1, y1, c):
        if x1 > x0 and y1 > y0:
            d.rectangle((x0, y0, x1 - 1, y1 - 1), fill=c)

    fill(1, 1, w - 1, h - 1, BG)
    d.rectangle((0, 0, w - 1, h - 1), outline=OUTLINE)
    fill(2, 1, w - 3, 3, LIGHT); fill(1, 2, 3, h - 3, LIGHT)
    fill(3, h - 3, w - 2, h - 1, SHADE); fill(w - 3, 3, w - 1, h - 2, SHADE)

    def slot(x, y):
        fill(x - 1, y - 1, x + 17, y + 17, SLOT_FILL)
        fill(x - 1, y - 1, x + 16, y, SLOT_DARK); fill(x - 1, y - 1, x, y + 16, SLOT_DARK)
        fill(x, y + 16, x + 17, y + 17, LIGHT); fill(x + 16, y, x + 17, y + 17, LIGHT)

    def inset(x, y, ww, hh):
        fill(x, y, x + ww, y + hh, SLOT_FILL)
        fill(x, y, x + ww - 1, y + 1, SLOT_DARK); fill(x, y, x + 1, y + hh - 1, SLOT_DARK)
        fill(x + 1, y + hh - 1, x + ww, y + hh, LIGHT); fill(x + ww - 1, y + 1, x + ww, y + hh, LIGHT)

    tx, ty, tw, th = spec['thermo']
    if spec['inset']:
        inset(tx, ty, tw, th)
        fill(tx + 1, ty + 1, tx + tw - 1, ty + th - 1, (34, 34, 34))
        seg = (th - 3 - 2) // 3
        for l, c in ((1, (224, 123, 34)), (2, (242, 178, 51)), (3, (58, 58, 58))):
            b = ty + th - 2 - (l - 1) * (seg + 1)
            fill(tx + 2, b - seg, tx + tw - 2, b, c)
    else:
        fill(tx, ty, tx + tw, ty + th, SLOT_DARK)
        fill(tx + 1, ty + 1, tx + tw - 1, ty + th - 1, (34, 34, 34))
        seg = (th - 2) // 3
        for l, c in ((1, (224, 123, 34)), (2, (242, 178, 51))):
            b = ty + th - 1 - (l - 1) * seg
            fill(tx + 2, b - seg + 1, tx + tw - 2, b, c)
    fx, fy, fw, fh = spec['fire']
    if spec['inset']:
        inset(fx, fy, fw, fh)
        fx, fy, fw, fh = fx + 1, fy + 1, fw - 2, fh - 2
    fill(fx, fy, fx + fw, fy + fh, (42, 36, 32))
    for col in range(0, fw, 2):
        hh = max(1, fh - 3 - (col * 7) % 4)
        fill(fx + col, fy + fh - 2 - hh, fx + col + 2, fy + fh - 2, (216, 82, 30))
        fill(fx + col, fy + fh - 2 - hh // 2, fx + col + 2, fy + fh - 2, (246, 194, 74))
    states = {0: (232, 137, 42), 2: (79, 161, 59), 4: (70, 127, 210)}
    for g in range(grids):
        for r in range(rows):
            for c in range(3):
                x, y = spec['grid'] + 1 + g * 58 + c * 18, 18 + 1 + r * 18
                slot(x, y)
                i = g * rows * 3 + r * 3 + c
                if i in states:
                    fill(x, y + 8, x + 16, y + 16, states[i])
    bx = spec['barrel']
    if attached:
        fill(bx - 2, 16, bx + 56, 74, (138, 74, 47)); fill(bx - 1, 17, bx + 55, 73, (201, 130, 95))
    if attached or spec['placeholders']:
        for i in range(9):
            x, y = bx + 1 + i % 3 * 18, 19 + i // 3 * 18
            slot(x, y)
            if not attached:
                fill(x, y, x + 16, y + 16, (120, 120, 120))
        if not attached:
            fill(bx + 19, 37, bx + 35, 53, (176, 120, 90))
    ix, iy = spec['inv']
    for r in range(3):
        for c in range(9):
            slot(ix + c * 18, iy + r * 18)
    for c in range(9):
        slot(ix + c * 18, iy + 58)
    big = img.resize((w * K, h * K), Image.Resampling.NEAREST)
    bd = ImageDraw.Draw(big)
    bd.text((8 * K, 6 * K - 2), title, font=FONT, fill=LABEL)
    bd.text((ix * K, (iy - 11) * K - 2), 'Inventar', font=FONT, fill=LABEL)
    if grids == 3:
        bd.text(((w - 8) * K - 50, 6 * K - 2), 'x2 Stapel', font=FONT, fill=LABEL)
    return big


def main():
    label = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 26)
    rows = []
    for name, (g, r) in TIERS.items():
        rows.append((name, [('v1 alt (ohne Fass)', draw(v1(g, r), g, r, False, name + '-Schmelztiegel')),
                            ('v2 neu (ohne Fass)', draw(v2(g, r), g, r, False, name + '-Schmelztiegel')),
                            ('v2 neu (Fass angebracht)', draw(v2(g, r), g, r, True, name + '-Schmelztiegel'))]))
    cw = max(img.width for _, cells in rows for _, img in cells) + 30
    rh = [max(img.height for _, img in cells) + 50 for _, cells in rows]
    sheet = Image.new('RGB', (200 + 3 * cw, 70 + sum(rh)), (41, 45, 53))
    d = ImageDraw.Draw(sheet)
    d.text((16, 16), 'Schmelztiegel-UI v2: Inhalt zentriert, Hitze-Saeule + Feuer eingelassen (Tooltip), Fass-Felder immer sichtbar',
           font=label, fill='white')
    y = 70
    for (name, cells), hgt in zip(rows, rh):
        d.text((16, y + 10), name, font=label, fill='white')
        x = 200
        for cap, img in cells:
            d.text((x, y), cap, font=label, fill=(208, 215, 226))
            sheet.paste(img, (x, y + 36))
            x += cw
        y += hgt
    OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUT)
    print('preview', OUT)


if __name__ == '__main__':
    main()
