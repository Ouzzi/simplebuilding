"""Crucible N12b previews (owner feedback 2026-10-06 on N12, plan docs/ai/PLAN-CRUCIBLE-N12B-2026-10-06.md).

Usage (repository root, Pillow + numpy, Vanilla textures in build/vanilla-textures):
  python tools/textures/crucible_n12b_2026_10_06.py
    -> previews/crucible-n12b-ui.png          references (images 3/4) | before (N12) | after, + corner zoom 1:1 grid
       previews/crucible-n12b-fortschritt.png  slot progress V1 (furnace fill, built in) / V2 (gap bar), + ...-fortschritt.gif
       previews/crucible-n12b-hitze.gif        heat: low (afterglow) / medium / high / extreme
       previews/crucible-n12b-keramik.png      ceramic bucket wear stages x fillings next to the copper bucket's oxidation
Mirrors CrucibleMenu.layout, CrucibleScreen (box, slot, furnaceFill, gapBar) and CrucibleFlames; keep them in step.
Supersedes the screen/flame mirror of crucible_n12_2026_10_06.py (kept for the N12 "before" pictures).
"""
from pathlib import Path
import math
import sys
import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import crucible_n12_2026_10_06 as n12  # noqa: E402  (canvas, sheet, the N12 screen = before)

ROOT = n12.ROOT
OUT = n12.OUT
REFS = OUT / 'refs-n12'
K = n12.K
FONT, LABEL_FONT, SHEET_BG = n12.FONT, n12.LABEL_FONT, n12.SHEET_BG
TIERS, PALETTES, INVENTORY, BARREL, argb, Canvas = n12.TIERS, n12.PALETTES, n12.INVENTORY, n12.BARREL, n12.argb, n12.Canvas
hash_, jround = n12.hash_, n12.jround

# palettes: N12b brighter inner light line
PALETTES = dict(PALETTES)
PALETTES['iron'] = (0xFF9A9DA2, 0xFFC4C7CB) + PALETTES['iron'][2:]
PALETTES['reinforced'] = (0xFF6F9095, 0xFF9DBCC1) + PALETTES['reinforced'][2:]
PALETTES['netherite'] = (0xFF5F524C, 0xFF867870) + PALETTES['netherite'][2:]
PALETTES['enderite'] = (0xFF8E6CB0, 0xFFB99AD6) + PALETTES['enderite'][2:]
INVENTORY = (0xFFE3E6E9, 0xFFF8F9FA) + INVENTORY[2:]
RIM = 0xFF1E1F23
FRAME, FRAME_BOTTOM = 5, 7
GRID_TOP, FIRE_ROOM, BARREL_GAP, MARGIN, BOX_GAP = 18, 12, 10, 8, 2
INVENTORY_LABEL, INVENTORY_TOP, INVENTORY_BOX = 6, 17, 100


def layout(tier, barrel):
    _, grids, rows, _ = TIERS[tier]
    gw = grids * 3 * 18

    def content(b):
        return gw + (BARREL_GAP + 54 if b else 0)

    def pw(b):
        return max(176, content(b) + 2 * MARGIN)

    def sec(b):
        return GRID_TOP + max(rows * 18, 54 if b else 0) + FIRE_ROOM

    def ph(b):
        return sec(b) + BOX_GAP + INVENTORY_BOX

    iw, ih = max(pw(False), pw(True)), max(ph(False), ph(True))
    w, h = pw(barrel), ph(barrel)
    x, y = (iw - w) // 2, (ih - h) // 2
    grid = x + (w - content(barrel)) // 2
    section = sec(barrel)
    return dict(iw=iw, ih=ih, x=x, y=y, w=w, h=h, grid=grid, barrel=grid + gw + BARREL_GAP, section=section,
                inv=(x + (w - 162) // 2 + 1, y + section + BOX_GAP + INVENTORY_TOP), rows=rows, grids=grids)


def scale(color, f):
    r, g, b = (min(255, int(((color >> s) & 255) * f)) for s in (16, 8, 0))
    return 0xFF000000 | r << 16 | g << 8 | b


def rounded(c, x, y, w, h, cuts, color):
    n = len(cuts)
    for k in range(n):
        c.fill(x + cuts[k], y + k, x + w - cuts[k], y + k + 1, color)
        c.fill(x + cuts[k], y + h - 1 - k, x + w - cuts[k], y + h - k, color)
    c.fill(x, y + n, x + w, y + h - n, color)


def box(c, x, y, w, h, p):
    rounded(c, x, y, w, h, (3, 1, 1), RIM)
    rounded(c, x + 1, y + 1, w - 2, h - 2, (2, 1), scale(p[0], 0.40))
    rounded(c, x + 1, y + 1, w - 2, h - 4, (2, 1), scale(p[0], 0.82))
    rounded(c, x + 2, y + 2, w - 4, h - 6, (1,), scale(p[0], 0.64))
    rounded(c, x + 4, y + 4, w - 8, h - 10, (1,), p[1])
    rounded(c, x + 5, y + 5, w - 10, h - 12, (), p[0])


def inset(c, x, y, w, h, p):
    rounded(c, x, y, w, h, (2, 1), RIM)
    rounded(c, x + 1, y + 1, w - 2, h - 2, (1,), p[0])
    c.fill(x + 2, y + 1, x + w - 2, y + 2, p[1])
    c.fill(x + 1, y + 2, x + 2, y + h - 2, p[1])
    c.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, p[2])
    c.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, p[2])


def slot(c, x, y, p):
    c.fill(x + 1, y + 16, x + 16, y + 17, p[1])
    c.fill(x + 16, y + 1, x + 17, y + 16, p[1])
    c.fill(x + 1, y, x + 15, y + 16, p[3])
    c.fill(x, y + 1, x + 1, y + 15, scale(p[4], 1.12))
    c.fill(x + 15, y + 1, x + 16, y + 15, p[3])
    c.fill(x + 1, y, x + 15, y + 1, p[4])


FILL_COOK = (0xFFFFAE1E, 0xFFF26B12, 0xFFFFE34A)
FILL_COLD = (0xFF4A86DA, 0xFF2C5DB0, 0xFFBFE0FF)
FILL_BLOCKED = (0xFFC9503E, 0xFF962A1E, 0xFFFF9A80)
TRACK, COOK, RED, BLUE, GREEN, GREEN_LIGHT, GREY = 0xFF3A3A3A, 0xFFF0901E, 0xFFD8402F, 0xFF4A86DA, 0xFF52B13C, 0xFF9BE07F, 0x80505050


def furnace_fill(c, x, y, level, colors, millis):
    if level <= 0:
        return
    top = y + 16 - level
    c.fill(x, max(top, y + 1), x + 16, y + 15, colors[0])
    if top <= y:
        c.fill(x + 1, y, x + 15, y + 1, colors[0])
    c.fill(x + 1, y + 15, x + 15, y + 16, colors[2])
    flick = millis // 160
    for i in range(3):
        tx = x + 2 + i * 5
        h = min(level - 3, 4 + (hash_(i, flick) & 3))
        for k in range(h):
            dx = 0 if ((k + i + flick) & 2) == 0 else 1
            c.fill(tx + dx, y + 13 - k, tx + dx + 2, y + 14 - k, colors[1])


def gap_bar(c, x, y, level, color):
    c.fill(x + 16, y, x + 18, y + 16, TRACK)
    if level > 0:
        c.fill(x + 16, y + 16 - level, x + 18, y + 16, color)


def slot_state(c, state, pct, x, y, millis, style):
    percent = 100 if state == 'blocked' else pct
    if state in ('cooking', 'cold', 'blocked'):
        level = max(1 if state == 'cooking' else 0, percent * 16 // 100)
        if style == 'FILL':
            furnace_fill(c, x, y, level, {'cooking': FILL_COOK, 'cold': FILL_COLD, 'blocked': FILL_BLOCKED}[state],
                         millis if state == 'cooking' else 0)
        else:
            gap_bar(c, x, y, level, {'cooking': COOK, 'cold': BLUE, 'blocked': RED}[state])
    if state == 'result':
        if style == 'FILL':
            c.fill(x + 1, y, x + 15, y + 1, GREEN)
            c.fill(x, y + 1, x + 1, y + 15, GREEN)
            c.fill(x + 1, y + 16, x + 16, y + 17, GREEN_LIGHT)
            c.fill(x + 16, y + 1, x + 17, y + 16, GREEN_LIGHT)
        else:
            gap_bar(c, x, y, 16, GREEN)
    else:
        n12.state_marks(c, state, x, y)


# ---------------------------------------------------------------- CrucibleFlames (N12b: calm levels)
FIRE, SOUL, SPACING = n12.FIRE, n12.SOUL, 10
LIVELY, MEDIUM, GLOW = 0, 1, 2


def calm_of(heat, glowing):
    return GLOW if glowing else MEDIUM if heat == 'medium' else LIVELY


def target_pixels(heat, glowing, section):
    if glowing:
        return 0 if heat == 'none' else max(3, jround(section / 16.0))
    return jround(section * {'none': 0.0, 'medium': 1 / 6.5, 'high': 1 / 3, 'extreme': 1.1 / 3}[heat])


def flick_ms(calm):
    return 110 if calm == LIVELY else 190 if calm == MEDIUM else 280


def height(col, millis, target, calm):
    t = millis / 1000.0 * (1.0 if calm == LIVELY else 0.55 if calm == MEDIUM else 0.35)
    flick = millis // flick_ms(calm)
    best = 0.0
    first = col // SPACING - 1
    for j in range(first, first + 3):
        center = j * SPACING + 5 + 1.5 * math.sin(t * 1.7 + j * 2.1)
        half = 4.5 + (hash_(j, 5) & 3) * 0.5
        skew = 0.3 if (hash_(j, 3) & 1) == 0 else -0.3
        off = col + 0.5 - center
        d = abs(off) / (half * (1 + skew if off < 0 else 1 - skew))
        if d >= 1:
            continue
        if calm == LIVELY:
            amp = 0.78 + 0.3 * math.sin(t * 2.3 + j * 1.7) + 0.14 * math.sin(t * 5.1 + j * 0.9) + (hash_(j, 9) & 3) * 0.06
        else:
            amp = 0.8 + 0.12 * math.sin(t * 2.3 + j * 1.7) + (hash_(j, 9) & 3) * 0.04
        best = max(best, amp * math.pow(1 - d, 1.5))
    jitter = 1 if calm == LIVELY and (hash_(col, flick) & 3) == 0 else 0
    h = jround(target * (0.3 + 0.75 * best)) + jitter
    return max(2, min(target * 8 // 5, h))


def shade(k, h, edge, ember, calm):
    if k == 0:
        return ember
    if edge <= 1:
        return 0
    if calm == GLOW:
        return 1
    if calm == MEDIUM:
        return 3 if k <= 1 else 1 if edge == 2 else 2
    if k <= 2:
        return 4
    if edge == 2:
        return 1
    return 3 if k < h * 35 // 100 and edge >= 4 else 2


def flames(c, x, bottom, width, section, heat, glowing, millis):
    target = target_pixels(heat, glowing, section)
    if target <= 0 or width <= 0:
        return
    calm = calm_of(heat, glowing)
    colors = SOUL if heat == 'extreme' else FIRE
    flick = millis // flick_ms(calm)
    hs = [height(i - 3, millis, target, calm) for i in range(width + 6)]
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
            c.fill(x + col, bottom - k - 1, x + col + 1, bottom - k, colors[shade(k, h, edge, ember, calm)])
    life = 9
    sparks = max(2, width // 14) if calm == LIVELY else max(1, width // (40 if calm == MEDIUM else 70))
    for s in range(sparks):
        clock = flick + (hash_(s, 13) & 63)
        age, cycle = clock % life, clock // life
        if age > (6 if calm == LIVELY else 3):
            continue
        col = hash_(s, cycle) % width
        sway = jround(math.sin((age + s) * 1.3))
        sx = max(0, min(width - 1, col + sway))
        sy = bottom - hs[col + 3] - 2 - age * 2
        c.fill(x + sx, sy, x + sx + 1, sy + 1, colors[7] if age < 3 and calm != GLOW else colors[1])
        if calm == LIVELY and age == 3 and (hash_(s, cycle + 7) & 1) == 0 and 0 < sx < width - 1:
            c.fill(x + sx - 1, sy, x + sx + 2, sy + 1, colors[6])
            c.fill(x + sx, sy - 1, x + sx + 1, sy + 2, colors[6])
            c.fill(x + sx, sy, x + sx + 1, sy + 1, colors[7])


# ---------------------------------------------------------------- screen
DEMO = n12.DEMO
INV_ITEMS = n12.INV_ITEMS


def screen(tier, barrel=False, heat='high', glowing=False, millis=1000, style='FILL', demo=True, states=None, k=K):
    L = layout(tier, barrel)
    name, grids, rows, mult = TIERS[tier]
    top = PALETTES[tier]
    c = Canvas(L['iw'], L['ih'])
    px, py, w = L['x'], L['y'], L['w']
    box(c, px, py, w, L['section'], top)
    box(c, px, py + L['section'] + BOX_GAP, w, L['h'] - L['section'] - BOX_GAP, INVENTORY)
    flames(c, px + FRAME, py + L['section'] - FRAME_BOTTOM, w - 2 * FRAME, L['section'], heat, glowing, millis)
    if barrel:
        bx, by = L['barrel'], py + GRID_TOP
        inset(c, bx - 2, by - 2, 59, 59, BARREL)
        for i in range(9):
            slot(c, bx + 1 + i % 3 * 18, by + 1 + i // 3 * 18, BARREL)
        c.item('cod', bx + 1, by + 1)
    states = DEMO if states is None else states
    items = []
    for col in range(grids * 3):
        for r in range(rows):
            x, y = L['grid'] + 1 + col * 18, py + GRID_TOP + 1 + r * 18
            slot(c, x, y, top)
            i = (col // 3) * rows * 3 + r * 3 + col % 3
            if demo and i in states:
                item, state, pct = states[i]
                slot_state(c, state, pct, x, y, millis, style)
                items.append((item, x, y))
    ix, iy = L['inv']
    for r in range(4):
        for col in range(9):
            x, y = ix + col * 18, iy + r * 18 + (4 if r == 3 else 0)
            slot(c, x, y, INVENTORY)
            if demo and r * 9 + col in INV_ITEMS:
                c.item(INV_ITEMS[r * 9 + col], x, y)
    for item, x, y in items:
        c.item(item, x, y)
    img = c.image()
    if k == 1:
        return img
    big = img.resize((L['iw'] * k, L['ih'] * k), Image.Resampling.NEAREST)
    d = ImageDraw.Draw(big)
    d.text(((px + 8) * k, (py + 6) * k - 2), f'{name} Schmelztiegel', font=FONT, fill=argb(top[5])[:3])
    if mult > 1:
        bonus = f'x{mult} Stapel'
        d.text(((px + w - 8) * k - d.textlength(bonus, font=FONT), (py + 6) * k - 2), bonus, font=FONT, fill=argb(top[5])[:3])
    d.text((ix * k, (iy - 11) * k - 2), 'Inventar', font=FONT, fill=(64, 64, 64))
    return big


def fit(img, height):
    return img.resize((round(img.width * height / img.height), height), Image.Resampling.LANCZOS)


def ui_sheet():
    ref3 = Image.open(REFS / 'bild3-container-stil.webp').convert('RGB')
    ref4 = Image.open(REFS / 'bild4-ofen-stil.png').convert('RGB')
    refs = [('Bild 3 (Ausschnitt)', fit(ref3.crop((52, 0, 430, 360)), 560)),
            ('Bild 4 (verkleinert)', fit(ref4.crop((0, 60, 1940, 1020)), 420))]
    rows = [('Referenz', refs)]
    for tier in TIERS:
        rows.append((TIERS[tier][0], [('vorher (N12)', n12.screen(tier)), ('nachher (N12b)', screen(tier)),
                                     ('nachher mit Fass', screen(tier, barrel=True))]))
    # 1:1 pixel comparison of a corner: image 4 scaled to GUI pixels (10.9 px each) next to ours, both 16x.
    gui = round(ref4.width / 10.9), round(ref4.height / 10.9)
    ref_px = ref4.resize(gui, Image.Resampling.BOX).crop((0, 6, 14, 20))
    ours = screen('iron', demo=False, k=1)
    L = layout('iron', False)
    ours_c = ours.crop((L['x'], L['y'], L['x'] + 14, L['y'] + 14))
    bottom = ours.crop((L['x'], L['y'] + L['section'] - 14, L['x'] + 14, L['y'] + L['section']))
    ref_bottom = ref4.resize(gui, Image.Resampling.BOX).crop((0, 80, 14, 94))
    z = 16
    rows.append(('Ecke 1:1 (x16)', [('Bild 4 oben links', ref_px.resize((14 * z, 14 * z), Image.Resampling.NEAREST)),
                                    ('neu oben links', ours_c.resize((14 * z, 14 * z), Image.Resampling.NEAREST)),
                                    ('Bild 4 unten links', ref_bottom.resize((14 * z, 14 * z), Image.Resampling.NEAREST)),
                                    ('neu unten links', bottom.resize((14 * z, 14 * z), Image.Resampling.NEAREST))]))
    return n12.sheet(rows, 'Schmelztiegel N12b: Rahmen wie Bild 4 (5 px, unten 7 px mit Schatten, Ecken 2 px rund), '
                           'Slots wie Bild 3, Flammen hinter den Slots', OUT / 'crucible-n12b-ui.png')


def progress_sheet():
    L = layout('reinforced', False)
    crop = ((L['grid'] - 4) * K, (L['y'] + GRID_TOP - 4) * K, (L['grid'] + 58) * K, (L['y'] + GRID_TOP + 58) * K)
    cells = [('vorher N12 (Randbalken)', n12.zoom(n12.screen('reinforced', heat='none'), (
        (n12.layout('reinforced', False, 'BAND')['grid'] - 4) * K, (n12.layout('reinforced', False, 'BAND')['y'] + GRID_TOP - 4) * K,
        (n12.layout('reinforced', False, 'BAND')['grid'] + 58) * K, (n12.layout('reinforced', False, 'BAND')['y'] + GRID_TOP + 58) * K)))]
    cells.append(('V1 Slot fuellt sich (eingebaut)', n12.zoom(screen('reinforced', heat='none', style='FILL'), crop)))
    cells.append(('V2 senkrechter Balken in der Fuge', n12.zoom(screen('reinforced', heat='none', style='GAP_BAR'), crop)))
    legend = Image.new('RGB', cells[0][1].size, SHEET_BG)
    d = ImageDraw.Draw(legend)
    for i, line in enumerate(['oben links: Roheisen kocht 35 %', 'oben Mitte: Rohgold kocht 80 %', 'oben rechts: fertig (gruen)',
                              'Mitte links: zu kalt, 50 % (blau, steht)', 'Mitte: blockiert (rot, voll)', 'Mitte rechts: kein Rezept (grau)',
                              'Konstante: PROGRESS_STYLE']):
        d.text((10, 10 + i * 30), line, font=LABEL_FONT, fill=(220, 225, 232))
    path = n12.sheet([('Verstaerkt', cells), ('Legende', [('', legend)])],
                     'Slot-Fortschritt N12b: V1 wie Ofen (Bild 4) in jedem Slot, V2 Balken in der Fuge', OUT / 'crucible-n12b-fortschritt.png')
    frames = []
    for i in range(24):
        pct = i * 100 // 23
        st = {0: ('raw_iron', 'cooking', pct), 1: ('raw_gold', 'cooking', (pct + 50) % 101), 2: ('iron_ingot', 'result', 0)}
        cells = [n12.zoom(screen('reinforced', heat='none', style=s, states=st, millis=i * 110), crop) for s in ('FILL', 'GAP_BAR')]
        frame = Image.new('RGB', (cells[0].width * 2 + 20, cells[0].height), SHEET_BG)
        frame.paste(cells[0], (0, 0))
        frame.paste(cells[1], (cells[0].width + 20, 0))
        frames.append(frame.resize((frame.width // 2, frame.height // 2), Image.Resampling.NEAREST))
    frames[0].save(OUT / 'crucible-n12b-fortschritt.gif', save_all=True, append_images=frames[1:], duration=160, loop=0)
    return path


def heat_gif():
    L = layout('netherite', False)
    frames = []
    cases = [('niedrig (Nachgluehen)', 'high', True), ('mittel', 'medium', False), ('hoch', 'high', False), ('extrem', 'extreme', False)]
    for i in range(36):
        t = i * 110
        cells = []
        for label, heat, glow in cases:
            img = screen('netherite', heat=heat, glowing=glow, millis=t, demo=False)
            img = img.crop((0, 0, img.width, (L['y'] + L['section']) * K))
            d = ImageDraw.Draw(img)
            d.rectangle((img.width - 270, 4, img.width - 8, 30), fill=SHEET_BG)
            d.text((img.width - 264, 6), label, font=FONT, fill='white')
            cells.append(img)
        frame = Image.new('RGB', (cells[0].width * 2 + 20, cells[0].height * 2 + 20), SHEET_BG)
        for n, cimg in enumerate(cells):
            frame.paste(cimg, ((n % 2) * (cimg.width + 20), (n // 2) * (cimg.height + 20)))
        frames.append(frame.resize((frame.width * 2 // 3, frame.height * 2 // 3), Image.Resampling.NEAREST))
    path = OUT / 'crucible-n12b-hitze.gif'
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=110, loop=0)
    return path


def ceramic_sheet():
    tex = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures/item'

    def tile(path):
        img = Image.open(path).convert('RGBA').crop((0, 0, 16, 16))
        bg = Image.new('RGBA', (16, 16), SHEET_BG + (255,))
        return Image.alpha_composite(bg, img).convert('RGB').resize((192, 192), Image.Resampling.NEAREST)

    names = ['intakt', 'angeschlagen', 'rissig', 'bruechig']
    rows = []
    for fill, label in (('ceramic_bucket', 'Keramik leer'), ('ceramic_water_bucket', 'Keramik Wasser'), ('ceramic_lava_bucket', 'Keramik Lava')):
        rows.append((label, [(f'{names[s]} ({s * 8}-{s * 8 + 7})', tile(tex / f'{p}{fill}.png'))
                             for s, p in enumerate(('', 'chipped_', 'cracked_', 'brittle_'))]))
    rows.append(('Kupfer (Vergleich)', [(f'Oxidation {s}', tile(tex / f'copper_bucket_{s}.png')) for s in range(4)]))
    rows.append(('roh', [('roher Keramik-Eimer', tile(tex / 'raw_ceramic_bucket.png'))]))
    return n12.sheet(rows, 'Keramik-Eimer N12b: Steinzeug statt Kupferorange, 4 Stufen wie Kupfer-Oxidation, '
                           'je 8 Einsaetze (32 gesamt)', OUT / 'crucible-n12b-keramik.png')


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for path in (ui_sheet(), progress_sheet(), heat_gif(), ceramic_sheet()):
        print('preview', path)


if __name__ == '__main__':
    main()
