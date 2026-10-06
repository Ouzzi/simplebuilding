"""Preview of the crucible screen v3 (owner feedback 2026-10-06) for every tier, without and with barrel, all heats.

Usage: python tools/textures/crucible_ui_v3_preview_2026_10_06.py
  -> previews/crucible-ui-v3-vorschau.png (sheet) and previews/crucible-ui-v3-flammen.gif (animation, Netherit, all heats)
Mirrors CrucibleMenu.layout (numbers) and CrucibleFlames (flame algorithm); keep all three in step.
"""
from pathlib import Path
import math
from PIL import Image, ImageDraw, ImageFont

OUT = Path('C:/Users/o_o/code/minecraft-mods/previews/crucible-ui-v3-vorschau.png')
GIF = Path('C:/Users/o_o/code/minecraft-mods/previews/crucible-ui-v3-flammen.gif')
TIERS = {'Eisen': (1, 2), 'Verstaerkt': (1, 3), 'Netherit': (2, 3), 'Enderit': (3, 3)}  # grids, rows
BG, OUTLINE, LIGHT, SHADE = (198, 198, 198), (0, 0, 0), (255, 255, 255), (85, 85, 85)
SLOT_DARK, SLOT_FILL, LABEL = (55, 55, 55), (139, 139, 139), (64, 64, 64)
K = 3
FONT = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 8 * K)
GRID_TOP, FIRE_ROOM, BARREL_GAP, MARGIN, CELL = 18, 8, 10, 8, 2
FIRE = [(255, 212, 59), (247, 148, 29), (232, 64, 28), (242, 107, 33)]
SOUL = [(184, 242, 255), (63, 169, 245), (30, 95, 208), (108, 200, 255)]
PART = {'none': 0.0, 'medium': 1 / 4.5, 'high': 1 / 3, 'extreme': 1.1 / 3}


def i32(v):
    v &= 0xFFFFFFFF
    return v - (1 << 32) if v >= 1 << 31 else v


def hash_(a, b):
    h = i32(a * 374761393 + b * 668265263)
    h = i32((h ^ ((h & 0xFFFFFFFF) >> 13)) * 1274126177)
    return i32(h ^ ((h & 0xFFFFFFFF) >> 16))


def target_cells(heat, section):
    return round(section * PART[heat] / CELL)


def height(c, millis, target):
    t = millis / 1000.0
    flick = millis // 110
    wave = 0.55 + 0.22 * math.sin(c * 0.3 + t * 2.1) + 0.15 * math.sin(c * 0.71 - t * 3.3) + 0.08 * math.sin(c * 1.6 + t * 5.0)
    jitter = (hash_(c // 2, flick) & 3) - 1
    return max(1, min(target * 8 // 5, int(math.floor(wave / 0.55 * target + 0.5)) + jitter))


def flames(fill, x, bottom, width, section, heat, millis):
    target = target_cells(heat, section)
    if target <= 0:
        return
    colors = SOUL if heat == 'extreme' else FIRE
    flick = millis // 110
    cols = (width + CELL - 1) // CELL
    for c in range(cols):
        h = height(c, millis, target)
        orange = h * 62 // 100 + (hash_(c, flick + 77) & 1)
        yellow = h * 30 // 100 + (hash_(c, flick + 151) & 1)
        x0, x1 = x + c * CELL, min(x + width, x + c * CELL + CELL)
        for k in range(h):
            color = colors[0] if k < yellow else colors[1] if k < orange else colors[2]
            fill(x0, bottom - (k + 1) * CELL, x1, bottom - k * CELL, color)
    life = 7
    for s in range(max(2, cols // 6)):
        clock = flick + (hash_(s, 13) & 63)
        age, cycle = clock % life, clock // life
        if age > 4:
            continue
        c = hash_(s, cycle) % cols
        k = height(c, millis, target) + 1 + age
        x0 = x + c * CELL
        fill(x0, bottom - (k + 1) * CELL, min(x + width, x0 + CELL), bottom - k * CELL, colors[2] if age < 2 else colors[3])


def layout(grids, rows, barrel):
    gw = grids * 3 * 18
    w = max(176, gw + (BARREL_GAP + 54 if barrel else 0) + 2 * MARGIN)
    section = GRID_TOP + max(rows * 18, 54 if barrel else 0) + FIRE_ROOM
    h = section + 13 + 76 + 7
    content = gw + (BARREL_GAP + 54 if barrel else 0)
    grid = (w - content) // 2
    return dict(w=w, h=h, section=section, grid=grid, barrel=grid + gw + BARREL_GAP, inv=((w - 162) // 2 + 1, section + 13))


def draw(name, grids, rows, barrel, heat, millis=1000):
    L = layout(grids, rows, barrel)
    w, h = L['w'], L['h']
    img = Image.new('RGB', (w, h), (41, 45, 53))
    d = ImageDraw.Draw(img)

    def fill(x0, y0, x1, y1, c):
        if x1 > x0 and y1 > y0:
            d.rectangle((x0, y0, x1 - 1, y1 - 1), fill=c)

    fill(1, 1, w - 1, h - 1, BG)
    d.rectangle((0, 0, w - 1, h - 1), outline=OUTLINE)
    fill(2, 1, w - 3, 3, LIGHT); fill(1, 2, 3, h - 3, LIGHT)
    fill(3, h - 3, w - 2, h - 1, SHADE); fill(w - 3, 3, w - 1, h - 2, SHADE)
    flames(fill, 3, L['section'], w - 6, L['section'], heat, millis)

    def slot(x, y):
        fill(x - 1, y - 1, x + 17, y + 17, SLOT_FILL)
        fill(x - 1, y - 1, x + 16, y, SLOT_DARK); fill(x - 1, y - 1, x, y + 16, SLOT_DARK)
        fill(x, y + 16, x + 17, y + 17, LIGHT); fill(x + 16, y, x + 17, y + 17, LIGHT)

    if barrel:
        bx = L['barrel']
        fill(bx - 2, GRID_TOP - 2, bx + 56, GRID_TOP + 56, (138, 74, 47)); fill(bx - 1, GRID_TOP - 1, bx + 55, GRID_TOP + 55, (201, 130, 95))
        for i in range(9):
            slot(bx + 1 + i % 3 * 18, GRID_TOP + 1 + i // 3 * 18)
    states = {0: (232, 137, 42), 2: (79, 161, 59), 4: (70, 127, 210)}
    for col in range(grids * 3):
        for r in range(rows):
            x, y = L['grid'] + 1 + col * 18, GRID_TOP + 1 + r * 18
            slot(x, y)
            i = (col // 3) * rows * 3 + r * 3 + col % 3
            if i in states:
                fill(x, y + 8, x + 16, y + 16, states[i])
    ix, iy = L['inv']
    for r in range(3):
        for c in range(9):
            slot(ix + c * 18, iy + r * 18)
    for c in range(9):
        slot(ix + c * 18, iy + 58)
    big = img.resize((w * K, h * K), Image.Resampling.NEAREST)
    bd = ImageDraw.Draw(big)
    bd.text((8 * K, 6 * K - 2), name + '-Schmelztiegel', font=FONT, fill=LABEL)
    bd.text((ix * K, (iy - 11) * K - 2), 'Inventar', font=FONT, fill=LABEL)
    return big


def main():
    label = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 26)
    rows = []
    for name, (g, r) in TIERS.items():
        rows.append((name, [('ohne Fass, Hitze hoch', draw(name, g, r, False, 'high')),
                            ('mit Fass, Hitze hoch', draw(name, g, r, True, 'high'))]))
    rows.append(('Hitzestufen', [(f'{heat}', draw('Verstaerkt', 1, 3, False, heat)) for heat in ('none', 'medium', 'high', 'extreme')]))
    strip = [(f't={t} ms', draw('Netherit', 2, 3, False, 'high', t).crop((0, 0, 0, 0)) if False else
              draw('Eisen', 1, 2, False, 'high', t)) for t in (0, 220, 440, 660)]
    rows.append(('Animation (hoch)', strip))
    cw = max(img.width for _, cells in rows for _, img in cells) + 30
    ncol = max(len(c) for _, c in rows)
    rh = [max(img.height for _, img in cells) + 50 for _, cells in rows]
    sheet = Image.new('RGB', (230 + ncol * cw, 70 + sum(rh)), (41, 45, 53))
    d = ImageDraw.Draw(sheet)
    d.text((16, 16), 'Schmelztiegel-UI v3: zusammenhaengendes Raster, Fass-Felder nur angebracht, Pixel-Flammen statt Hitzebalken',
           font=label, fill='white')
    y = 70
    for (name, cells), hgt in zip(rows, rh):
        d.text((16, y + 10), name, font=label, fill='white')
        x = 230
        for cap, img in cells:
            d.text((x, y), cap, font=label, fill=(208, 215, 226))
            sheet.paste(img, (x, y + 36))
            x += cw
        y += hgt
    OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUT)
    frames = []
    for i in range(24):
        t = i * 110
        cells = [draw('Netherit', 2, 3, False, heat, t) for heat in ('medium', 'high', 'extreme')]
        fw = sum(c.width for c in cells) + 20 * 2
        frame = Image.new('RGB', (fw, cells[0].height), (41, 45, 53))
        x = 0
        for c in cells:
            frame.paste(c, (x, 0))
            x += c.width + 20
        frames.append(frame.resize((fw // 2, frame.height // 2), Image.Resampling.NEAREST))
    frames[0].save(GIF, save_all=True, append_images=frames[1:], duration=110, loop=0)
    print('preview', OUT, GIF)


if __name__ == '__main__':
    main()
