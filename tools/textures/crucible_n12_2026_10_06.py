"""Crucible N12 previews (owner feedback 2026-10-06, plan docs/ai/PLAN-CRUCIBLE-N12-2026-10-06.md).

Usage (repository root, Pillow + numpy, Vanilla textures in build/vanilla-textures):
  python tools/textures/crucible_n12_2026_10_06.py
    -> previews/crucible-n12-ui.png           before (v3) / A flame strip (built in) / B heat slot like image 4
       previews/crucible-n12-fortschritt.png  slot state bar: before / A over the item (built in) / B behind / C in the gap
       previews/crucible-n12-flammen.png      flames before / after per heat, + crucible-n12-flammen.gif (animation)
       previews/crucible-n12-fass.png         docked barrel before / after (1 px smaller, closer)
       previews/crucible-n12-keramik-lavaeimer.png  the new ceramic lava bucket next to its siblings
Mirrors CrucibleMenu.layout, CrucibleScreen (boxes, slots, state bars) and CrucibleFlames; keep them in step.
"""
from pathlib import Path
import json
import math
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).resolve().parent))
import crucible_ui_v3_preview_2026_10_06 as v3  # noqa: E402  (the screen before)

ROOT = Path(__file__).resolve().parents[2]
OUT = Path('C:/Users/o_o/code/minecraft-mods/previews')
ITEMS = ROOT / 'build/vanilla-textures/item'
K = 3
FONT = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 8 * K)
LABEL_FONT = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 22)
SHEET_BG = (41, 45, 53)

# ---------------------------------------------------------------- CrucibleMenu.layout
TIERS = {'iron': ('Eisen', 1, 2, 1), 'reinforced': ('Verstaerkter', 1, 3, 1), 'netherite': ('Netherit', 2, 3, 1),
         'enderite': ('Enderit', 3, 3, 2)}
GRID_TOP, FIRE_ROOM, SLOT_ROOM, BARREL_GAP, MARGIN, BOX_GAP, HEAT_CELL, HEAT_GAP = 18, 8, 6, 10, 8, 2, 18, 6


def layout(tier, barrel, style):
    _, grids, rows, _ = TIERS[tier]
    gw = grids * 3 * 18
    heat_w = HEAT_CELL + HEAT_GAP if style == 'SLOT' else 0

    def content(b):
        return heat_w + gw + (BARREL_GAP + 54 if b else 0)

    def pw(b):
        return max(176, content(b) + 2 * MARGIN)

    def sec(b):
        return GRID_TOP + max(rows * 18, 54 if b else 0) + (SLOT_ROOM if style == 'SLOT' else FIRE_ROOM)

    def ph(b):
        return sec(b) + BOX_GAP + 13 + 76 + 7

    iw, ih = max(pw(False), pw(True)), max(ph(False), ph(True))
    w, h = pw(barrel), ph(barrel)
    x, y = (iw - w) // 2, (ih - h) // 2
    heat_left = x + (w - content(barrel)) // 2
    grid = heat_left + heat_w
    section = sec(barrel)
    return dict(iw=iw, ih=ih, x=x, y=y, w=w, h=h, grid=grid, barrel=grid + gw + BARREL_GAP, section=section,
                inv=(x + (w - 162) // 2 + 1, y + section + BOX_GAP + 13), heat=(heat_left + 1, y + GRID_TOP + 1 + (rows - 1) * 18),
                rows=rows, grids=grids)


# ---------------------------------------------------------------- CrucibleScreen palettes
def argb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255, (v >> 24) & 255)


PALETTES = {
    'iron': (0xFF9A9DA2, 0xFFB5B8BC, 0xFF7E8186, 0xFF7B7E83, 0xFF64676C, 0xFF2E3034),
    'reinforced': (0xFF6F9095, 0xFF8AAAAF, 0xFF587378, 0xFF55737A, 0xFF425C61, 0xFFF0F6F6),
    'netherite': (0xFF5F524C, 0xFF766860, 0xFF4A3F3A, 0xFF473C37, 0xFF352C28, 0xFFEFE4DA),
    'enderite': (0xFF8E6CB0, 0xFFA888C7, 0xFF735693, 0xFF70538E, 0xFF594073, 0xFFF7F0FF),
}
INVENTORY = (0xFFE3E6E9, 0xFFF6F7F8, 0xFFC5CACE, 0xFFB4BABF, 0xFF979DA3, 0xFF404040)
BARREL = (0xFFB9774F, 0xFFD08F68, 0xFF955839, 0xFF94573A, 0xFF74412B, 0xFF404040)
RIM = 0xFF2B2D31
TRACK, COOK, RED, BLUE, GREEN, GREY = 0xB0262626, 0xFFF0901E, 0xFFD8402F, 0xFF4A86DA, 0xFF52B13C, 0x80505050


class Canvas:
    def __init__(self, w, h):
        self.a = np.zeros((h, w, 3), dtype=np.float64)
        self.a[:] = SHEET_BG

    def fill(self, x0, y0, x1, y1, color):
        h, w = self.a.shape[:2]
        x0, x1, y0, y1 = max(0, x0), min(w, x1), max(0, y0), min(h, y1)
        if x1 <= x0 or y1 <= y0:
            return
        r, g, b, a = argb(color)
        k = a / 255.0
        self.a[y0:y1, x0:x1] = self.a[y0:y1, x0:x1] * (1 - k) + np.array([r, g, b]) * k

    def item(self, name, x, y, veil=None):
        img = Image.open(ITEMS / f'{name}.png').convert('RGBA')
        px = np.asarray(img, dtype=np.float64)
        al = px[:, :, 3:4] / 255.0
        region = self.a[y:y + 16, x:x + 16]
        region[:] = region * (1 - al) + px[:, :, :3] * al

    def image(self):
        return Image.fromarray(np.clip(self.a, 0, 255).astype(np.uint8), 'RGB')


def box(c, x, y, w, h, p):
    fill, light, shade = p[0], p[1], p[2]
    for args in ((x + 2, y, x + w - 2, y + 1), (x + 2, y + h - 1, x + w - 2, y + h), (x, y + 2, x + 1, y + h - 2),
                 (x + w - 1, y + 2, x + w, y + h - 2), (x + 1, y + 1, x + 2, y + 2), (x + w - 2, y + 1, x + w - 1, y + 2),
                 (x + 1, y + h - 2, x + 2, y + h - 1), (x + w - 2, y + h - 2, x + w - 1, y + h - 1)):
        c.fill(*args, RIM)
    c.fill(x + 2, y + 1, x + w - 2, y + h - 1, fill)
    c.fill(x + 1, y + 2, x + 2, y + h - 2, fill)
    c.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, fill)
    c.fill(x + 2, y + 1, x + w - 2, y + 2, light)
    c.fill(x + 1, y + 2, x + 2, y + h - 2, light)
    c.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, shade)
    c.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, shade)


def slot(c, x, y, p):
    c.fill(x + 1, y + 16, x + 16, y + 17, p[1])
    c.fill(x + 16, y + 1, x + 17, y + 16, p[1])
    c.fill(x + 1, y, x + 15, y + 16, p[3])
    c.fill(x, y + 1, x + 1, y + 15, p[3])
    c.fill(x + 15, y + 1, x + 16, y + 15, p[3])
    c.fill(x + 1, y, x + 15, y + 1, p[4])


def state_marks(c, state, x, y):
    if state == 'cooking':
        c.fill(x + 13, y + 1, x + 15, y + 2, 0xFFFFC040)
        c.fill(x + 12, y + 2, x + 15, y + 4, 0xFFFFC040)
    elif state == 'blocked':
        c.fill(x + 12, y + 1, x + 13, y + 5, 0xFFFFFFFF)
        c.fill(x + 13, y + 2, x + 14, y + 4, 0xFFFFFFFF)
        c.fill(x + 14, y + 1, x + 15, y + 5, 0xFFFFFFFF)
    elif state == 'cold':
        c.fill(x + 13, y + 1, x + 14, y + 6, 0xFFFFFFFF)
        c.fill(x + 11, y + 3, x + 16, y + 4, 0xFFFFFFFF)
    elif state == 'no_recipe':
        c.fill(x, y + 1, x + 16, y + 15, GREY)


def state_bar(c, state, pct, x, y):
    x1, x2 = x + 1, x + 15
    if state in ('cooking', 'cold'):
        w = pct * 14 // 100
        if state == 'cooking':
            w = max(1, w)
        c.fill(x1, y, x2, y + 2, TRACK)
        if w > 0:
            c.fill(x1, y, x1 + w, y + 2, COOK if state == 'cooking' else BLUE)
    elif state == 'blocked':
        c.fill(x1, y, x2, y + 2, RED)
    elif state == 'result':
        c.fill(x1, y, x2, y + 2, GREEN)


# ---------------------------------------------------------------- CrucibleFlames
FIRE = [0xFFC81E12, 0xFFE8451A, 0xFFF7921C, 0xFFFFC832, 0xFFFFE98A, 0xFF7A1808, 0xFFFF7A1E, 0xFFFFE070]
SOUL = [0xFF1A3FA8, 0xFF2370D8, 0xFF3FA9F5, 0xFF8EE0FF, 0xFFDDF8FF, 0xFF0E2A6A, 0xFF3F9CF0, 0xFFC8F4FF]
SPACING = 10
PART = {'none': 0.0, 'medium': 1 / 4.5, 'high': 1 / 3, 'extreme': 1.1 / 3}
hash_ = v3.hash_


def jround(v):
    return int(math.floor(v + 0.5))


def target_pixels(heat, section):
    return jround(section * PART[heat])


def height(c, millis, target):
    t = millis / 1000.0
    flick = millis // 110
    best = 0.0
    first = c // SPACING - 1
    for j in range(first, first + 3):
        center = j * SPACING + 5 + 1.5 * math.sin(t * 1.7 + j * 2.1)
        half = 4.5 + (hash_(j, 5) & 3) * 0.5
        skew = 0.3 if (hash_(j, 3) & 1) == 0 else -0.3
        off = c + 0.5 - center
        d = abs(off) / (half * (1 + skew if off < 0 else 1 - skew))
        if d >= 1:
            continue
        amp = 0.78 + 0.3 * math.sin(t * 2.3 + j * 1.7) + 0.14 * math.sin(t * 5.1 + j * 0.9) + (hash_(j, 9) & 3) * 0.06
        best = max(best, amp * math.pow(1 - d, 1.5))
    jitter = 1 if (hash_(c, flick) & 3) == 0 else 0
    h = jround(target * (0.3 + 0.75 * best)) + jitter
    return max(2, min(target * 8 // 5, h))


def shade(k, h, edge, ember):
    if k == 0:
        return ember
    if edge <= 1:
        return 0
    if k <= 2:
        return 4
    if edge == 2:
        return 1
    return 3 if k < h * 35 // 100 and edge >= 4 else 2


def flames(c, x, bottom, width, section, heat, millis):
    target = target_pixels(heat, section)
    if target <= 0 or width <= 0:
        return
    colors = SOUL if heat == 'extreme' else FIRE
    flick = millis // 110
    hs = [height(i - 3, millis, target) for i in range(width + 6)]
    for col in range(width):
        h = hs[col + 3]
        ember = 6 if (hash_(col, flick // 3) & 3) == 0 else 5
        for k in range(h):
            edge = (h - k + 1) // 2
            for d in range(1, 4):
                if d >= edge:
                    break
                if hs[col + 3 - d] <= k or hs[col + 3 + d] <= k:
                    edge = d
                    break
            c.fill(x + col, bottom - k - 1, x + col + 1, bottom - k, colors[shade(k, h, edge, ember)])
    life = 9
    for s in range(max(2, width // 14)):
        clock = flick + (hash_(s, 13) & 63)
        age, cycle = clock % life, clock // life
        if age > 6:
            continue
        col = hash_(s, cycle) % width
        sway = jround(math.sin((age + s) * 1.3))
        sx = max(0, min(width - 1, col + sway))
        sy = bottom - hs[col + 3] - 2 - age * 2
        c.fill(x + sx, sy, x + sx + 1, sy + 1, colors[7] if age < 3 else colors[1])
        if age == 3 and (hash_(s, cycle + 7) & 1) == 0 and 0 < sx < width - 1:
            c.fill(x + sx - 1, sy, x + sx + 2, sy + 1, colors[6])
            c.fill(x + sx, sy - 1, x + sx + 1, sy + 2, colors[6])
            c.fill(x + sx, sy, x + sx + 1, sy + 1, colors[7])


SLOT_LEVEL = {'none': 0, 'medium': 6, 'high': 11, 'extreme': 16}


def heat_slot(c, x, y, heat, millis):
    level = SLOT_LEVEL[heat]
    if level <= 0:
        return
    soul = heat == 'extreme'
    body, tongue, base = (0xFF3FA9F5, 0xFF1E5FD0, 0xFFC8F4FF) if soul else (0xFFFFAE1E, 0xFFF26B12, 0xFFFFE34A)
    top = y + 16 - level
    c.fill(x, top, x + 16, y + 15, body)
    c.fill(x + 1, y + 15, x + 15, y + 16, base)
    flick = millis // 160
    for i in range(3):
        tx = x + 2 + i * 5
        h = min(level - 3, 4 + (hash_(i, flick) & 3))
        for k in range(h):
            dx = 0 if ((k + i + flick) & 2) == 0 else 1
            c.fill(tx + dx, y + 13 - k, tx + dx + 2, y + 14 - k, tongue)
    room = 16 - level - 2
    for i in range(3):
        if room <= 0:
            break
        wx = x + 3 + i * 4
        for k in range(min(5, room)):
            dx = 0 if ((k + flick + i) & 2) == 0 else 1
            c.fill(wx + dx, top - 2 - k, wx + dx + 1, top - 1 - k, 0xFF5E5E5E)


# ---------------------------------------------------------------- the screen
DEMO = {0: ('raw_iron', 'cooking', 35), 1: ('raw_gold', 'cooking', 80), 2: ('iron_ingot', 'result', 0),
        3: ('raw_copper', 'cold', 50), 4: ('beef', 'blocked', 0), 5: ('kelp', 'no_recipe', 0)}
INV_ITEMS = {0: 'clay_ball', 3: 'potato', 9: 'cod', 13: 'raw_iron', 27: 'gold_ingot', 31: 'copper_ingot'}


def screen(tier, barrel=False, heat='high', millis=1000, style='BAND', progress='A', demo=True):
    L = layout(tier, barrel, style)
    name, grids, rows, mult = TIERS[tier]
    top = PALETTES[tier]
    c = Canvas(L['iw'], L['ih'])
    px, py, w = L['x'], L['y'], L['w']
    box(c, px, py, w, L['section'], top)
    box(c, px, py + L['section'] + BOX_GAP, w, L['h'] - L['section'] - BOX_GAP, INVENTORY)
    if style == 'BAND':
        flames(c, px + 2, py + L['section'] - 1, w - 4, L['section'], heat, millis)
    else:
        hx, hy = L['heat']
        slot(c, hx, hy, top)
        heat_slot(c, hx, hy, heat, millis)
    if barrel:
        bx, by = L['barrel'], py + GRID_TOP
        box(c, bx - 2, by - 2, 59, 59, BARREL)
        for i in range(9):
            slot(c, bx + 1 + i % 3 * 18, by + 1 + i // 3 * 18, BARREL)
        c.item('cod', bx + 1, by + 1)
    bars = []
    for col in range(grids * 3):
        for r in range(rows):
            x, y = L['grid'] + 1 + col * 18, py + GRID_TOP + 1 + r * 18
            slot(c, x, y, top)
            i = (col // 3) * rows * 3 + r * 3 + col % 3
            if demo and i in DEMO:
                item, state, pct = DEMO[i]
                state_marks(c, state, x, y)
                if progress == 'B':
                    state_bar(c, state, pct, x, y + 14)
                elif progress == 'C':
                    state_bar(c, state, pct, x, y + 16)
                bars.append((item, state, pct, x, y))
    ix, iy = L['inv']
    for r in range(4):
        for col in range(9):
            x, y = ix + col * 18, iy + r * 18 + (4 if r == 3 else 0)
            slot(c, x, y, INVENTORY)
            idx = r * 9 + col
            if demo and idx in INV_ITEMS:
                c.item(INV_ITEMS[idx], x, y)
    for item, state, pct, x, y in bars:
        c.item(item, x, y)
        if progress == 'A':
            state_bar(c, state, pct, x, y + 14)
    big = c.image().resize((L['iw'] * K, L['ih'] * K), Image.Resampling.NEAREST)
    d = ImageDraw.Draw(big)
    d.text(((px + 8) * K, (py + 6) * K - 2), f'{name} Schmelztiegel', font=FONT, fill=argb(top[5])[:3])
    if mult > 1:
        bonus = f'x{mult} Stapel'
        d.text(((px + w - 8) * K - d.textlength(bonus, font=FONT), (py + 6) * K - 2), bonus, font=FONT, fill=argb(top[5])[:3])
    d.text((ix * K, (iy - 11) * K - 2), 'Inventar', font=FONT, fill=(64, 64, 64))
    return big


def before(tier, barrel=False, heat='high', millis=1000):
    name, grids, rows, _ = TIERS[tier]
    return v3.draw(name, grids, rows, barrel, heat, millis)


def sheet(rows, title, path):
    cw = max(img.width for _, cells in rows for _, img in cells) + 30
    ncol = max(len(c) for _, c in rows)
    rh = [max(img.height for _, img in cells) + 50 for _, cells in rows]
    out = Image.new('RGB', (240 + ncol * cw, 70 + sum(rh)), SHEET_BG)
    d = ImageDraw.Draw(out)
    d.text((16, 16), title, font=LABEL_FONT, fill='white')
    y = 70
    for (name, cells), hgt in zip(rows, rh):
        d.text((16, y + 10), name, font=LABEL_FONT, fill='white')
        x = 240
        for cap, img in cells:
            d.text((x, y), cap, font=LABEL_FONT, fill=(208, 215, 226))
            out.paste(img, (x, y + 36))
            x += cw
        y += hgt
    out.save(path)
    return path


def ui_sheet():
    rows = []
    for tier in TIERS:
        rows.append((TIERS[tier][0], [('vorher (v3)', before(tier)),
                                      ('A Flammenstreifen (eingebaut)', screen(tier)),
                                      ('A mit Fass', screen(tier, barrel=True)),
                                      ('B Hitze-Slot (Bild 4)', screen(tier, style='SLOT'))]))
    return sheet(rows, 'Schmelztiegel N12: zwei Kaesten wie Bild 3 (Tiegel farbig, Inventar hell), runde eingelassene Slots; '
                       'Hitze A = Flammenstreifen, B = Slot wie Bild 4', OUT / 'crucible-n12-ui.png')


def zoom(img, box_, k=2):
    crop = img.crop(box_)
    return crop.resize((crop.width * k, crop.height * k), Image.Resampling.NEAREST)


def progress_sheet():
    L = layout('reinforced', False, 'BAND')
    crop = ((L['grid'] - 4) * K, (L['y'] + GRID_TOP - 4) * K, (L['grid'] + 58) * K, (L['y'] + GRID_TOP + 58) * K)
    old = v3_states()
    cells = [('vorher (Fuellung)', zoom(old, crop))]
    for key, cap in (('A', 'A unten 2 Reihen, ueber Item (eingebaut)'), ('B', 'B unten 2 Reihen, hinter Item'),
                     ('C', 'C 2-px-Fuge unter dem Slot')):
        cells.append((cap, zoom(screen('reinforced', heat='none', progress=key), crop)))
    legend = Image.new('RGB', (cells[0][1].width, cells[0][1].height), SHEET_BG)
    d = ImageDraw.Draw(legend)
    lines = ['oben links: Roheisen kocht 35 %', 'oben Mitte: Rohgold kocht 80 %', 'oben rechts: fertig (gruen)',
             'Mitte links: zu kalt, 50 % (blau)', 'Mitte: blockiert (rot)', 'Mitte rechts: kein Rezept (grau)',
             'Eckzeichen bleiben (Farbenblinde)']
    for i, line in enumerate(lines):
        d.text((10, 10 + i * 30), line, font=LABEL_FONT, fill=(220, 225, 232))
    return sheet([('Verstaerkt', cells), ('Legende', [('', legend)])],
                 'Slot-Fortschritt: untere 2 Pixelreihen des Slots statt ganzer Fuellung (A eingebaut)', OUT / 'crucible-n12-fortschritt.png')


def v3_states():
    """The v3 screen with the old full-slot fills for the demo states (before)."""
    img = before('reinforced', heat='none')
    L = v3.layout(1, 3, False)
    c = Canvas(img.width // K, img.height // K)
    c.a[:] = np.asarray(img.resize((img.width // K, img.height // K), Image.Resampling.NEAREST), dtype=np.float64)
    for i, (item, state, pct) in DEMO.items():
        x, y = L['grid'] + 1 + (i % 3) * 18, GRID_TOP + 1 + (i // 3) * 18
        c.fill(x, y, x + 16, y + 16, 0xFF8B8B8B)
        if state == 'cooking':
            c.fill(x, y, x + 16, y + 16, 0xFFB0A090)
            hh = max(1, pct * 16 // 100)
            c.fill(x, y + 16 - hh, x + 16, y + 16, 0xC0E8892A)
        elif state == 'blocked':
            c.fill(x, y, x + 16, y + 16, 0xC0C83C32)
        elif state == 'cold':
            c.fill(x, y, x + 16, y + 16, 0xC0467FD2)
            c.fill(x, y + 8, x + 16, y + 16, 0x60E8892A)
        elif state == 'result':
            for a in ((x, y, x + 16, y + 1), (x, y + 15, x + 16, y + 16), (x, y, x + 1, y + 16), (x + 15, y, x + 16, y + 16)):
                c.fill(*a, 0xFF4FA13B)
        elif state == 'no_recipe':
            c.fill(x, y, x + 16, y + 16, 0x80505050)
        c.item(item, x, y)
    # the v3 layout has no y offset: the reinforced box starts at 0; shift the crop by the n12 offset
    L12 = layout('reinforced', False, 'BAND')
    shifted = Canvas(L12['iw'], L12['ih'])
    src = c.a
    dx, dy = L12['grid'] - L['grid'], L12['y']
    hh, ww = min(src.shape[0], shifted.a.shape[0] - dy), min(src.shape[1], shifted.a.shape[1] - max(dx, 0))
    shifted.a[dy:dy + hh, max(dx, 0):max(dx, 0) + ww] = src[:hh, max(-dx, 0):max(-dx, 0) + ww]
    return shifted.image().resize((L12['iw'] * K, L12['ih'] * K), Image.Resampling.NEAREST)


def flames_sheet():
    rows = []
    for heat in ('medium', 'high', 'extreme'):
        rows.append((heat, [('vorher (v3)', before('netherite', heat=heat)), ('nachher', screen('netherite', heat=heat, demo=False))]))
    strip = [(f't={t} ms', zoom(screen('iron', heat='high', millis=t, demo=False), (0, 0, 176 * K, 66 * K), 1)) for t in (0, 330, 660, 990)]
    rows.append(('Animation', strip))
    path = sheet(rows, 'Flammen N12 (Bild 1/2): spitze Zungen, roter Rand, gelber Kern unten, Glutreihe, Funken (+)',
                 OUT / 'crucible-n12-flammen.png')
    frames = []
    for i in range(30):
        t = i * 110
        cells = [screen('netherite', heat=heat, millis=t, demo=False) for heat in ('medium', 'high', 'extreme')]
        cells = [c.crop((0, 0, c.width, (layout('netherite', False, 'BAND')['section'] + 2) * K)) for c in cells]
        fw = sum(c.width for c in cells) + 20 * 2
        frame = Image.new('RGB', (fw, cells[0].height), SHEET_BG)
        x = 0
        for cimg in cells:
            frame.paste(cimg, (x, 0))
            x += cimg.width + 20
        frames.append(frame.resize((fw * 2 // 3, frame.height * 2 // 3), Image.Resampling.NEAREST))
    frames[0].save(OUT / 'crucible-n12-flammen.gif', save_all=True, append_images=frames[1:], duration=110, loop=0)
    return path


# ---------------------------------------------------------------- docked barrel
def barrel_sheet():
    import crucible_art_v2_2026_10_05 as v2
    import crucible_n11_2026_10_06 as n11
    sys.path.insert(0, str(ROOT / 'modules/simplelib/tools'))
    import gen_resources as gen
    old_dock = {'elements': [json.loads(json.dumps(e)) for e in OLD_DOCK]}
    new_dock = {'elements': [gen.DOCKED_BODY, *gen.DOCK]}
    rows = []
    for label, model in (('vorher', old_dock), ('nachher', new_dock)):
        cells = []
        for tier, yaw, pitch in (('copper', 225, 30), ('netherite', 135, 30), ('enderite', 270, 10), ('copper', 180, 60)):
            crucible = v2.kettle_model()
            m = {'elements': n11.shifted(crucible, -8, 'c_') + n11.shifted(model, 8, 'b_')}
            tex = {'c_' + k: v for k, v in v2.crucible_textures('iron', v2.CHOICE['crucible']).items()}
            for k, v in v2.barrel_textures(tier).items():
                tex['b_' + k] = v
            cells.append((f'{tier} {yaw}/{pitch}', v2.render(m, tex, yaw=yaw, pitch=pitch, scale=7, size=300)))
        rows.append((label, cells))
    img = v2.label_sheet(rows, 'Angedocktes Fass N12: je Achse 1 px kleiner (13 x 11 x 13), 1 px naeher am Tiegel, Flansch/Rinne angepasst')
    path = OUT / 'crucible-n12-fass.png'
    img.save(path)
    return path


def _old_dock():
    """The docked barrel of v3 (before N12) for the comparison."""
    sys.path.insert(0, str(ROOT / 'modules/simplelib/tools'))
    import gen_resources as gen
    body = gen.full_uv(gen.cuboid((1, 0, 1), (15, 12, 15), {'down': '#bottom', 'up': '#top', 'north': '#side', 'south': '#side',
                                                          'west': '#side', 'east': '#side'}))
    flange = '#flange'
    dock = [gen.inside_uv(e) for e in (
        gen.cuboid((3, 3, -1), (13, 10, 1), {'north': flange, 'up': flange, 'down': flange, 'west': flange, 'east': flange}),
        gen.cuboid((6, 14, -4), (10, 15, 2), {'north': flange, 'up': flange, 'down': flange, 'west': flange, 'east': flange, 'south': flange}),
        gen.cuboid((6, 12, 1), (10, 14, 2), {'north': flange, 'west': flange, 'east': flange, 'south': flange}),
    )]
    return [body, *dock]


OLD_DOCK = _old_dock()


def bucket_sheet():
    """The ceramic lava bucket (texture from texture_round7_2026_10_06, ceramic B) beside the other buckets."""
    tex = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures/item'
    names = [('Keramik', 'ceramic_bucket'), ('Keramik Wasser', 'ceramic_water_bucket'), ('Keramik Lava (neu)', 'ceramic_lava_bucket'),
             ('Kupfer Lava', 'copper_lava_bucket_0'), ('Vanilla Lava', None)]
    cells = []
    for cap, name in names:
        img = Image.open(tex / f'{name}.png' if name else ITEMS / 'lava_bucket.png').convert('RGBA')
        img = img.crop((0, 0, 16, 16))
        bg = Image.new('RGBA', (16, 16), SHEET_BG + (255,))
        cells.append((cap, Image.alpha_composite(bg, img).convert('RGB').resize((160, 160), Image.Resampling.NEAREST)))
    return sheet([('Eimer', cells)], 'Keramik-Lavaeimer N12: Stil der Keramik-Eimer, Lava wie Vanilla', OUT / 'crucible-n12-keramik-lavaeimer.png')


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for path in (ui_sheet(), progress_sheet(), flames_sheet(), barrel_sheet(), bucket_sheet()):
        print('preview', path)


if __name__ == '__main__':
    main()
