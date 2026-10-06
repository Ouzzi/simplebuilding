"""Crucible N12c previews (owner feedback 2026-10-06 on N12b, plan docs/ai/PLAN-CRUCIBLE-N12C-2026-10-06.md).

Usage (repository root, Pillow + numpy, Vanilla textures in build/vanilla-textures):
  python tools/textures/crucible_n12c_2026_10_06.py
    -> previews/crucible-n12c-ui.png     every tier with an attached barrel: before (N12b) | after (own barrel box,
                                          as many barrel fields as crucible slots)
       previews/crucible-n12c-feuer.gif   flames per heat: current (pointed, built in) | broad (wide low tongues)
Mirrors CrucibleMenu.layout (N12c), CrucibleScreen and CrucibleFlames (SHAPE); keep them in step.
"""
from pathlib import Path
import math
import sys
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import crucible_n12b_2026_10_06 as b  # noqa: E402  (N12b screen = before, boxes, slots, flames)

OUT, K, FONT, SHEET_BG = b.OUT, b.K, b.FONT, b.SHEET_BG
TIERS, PALETTES, INVENTORY, BARREL, Canvas = b.TIERS, b.PALETTES, b.INVENTORY, b.BARREL, b.Canvas
GRID_TOP, FIRE_ROOM, MARGIN, BOX_GAP, INVENTORY_TOP, INVENTORY_BOX = 18, 12, 8, 2, 17, 100
hash_, jround = b.hash_, b.jround


def layout(tier, barrel):
    _, grids, rows, _ = TIERS[tier]
    gw = grids * 3 * 18
    bw = gw + 2 * MARGIN

    def cw(br):
        return max(176, gw + 2 * MARGIN)

    def pw(br):
        return cw(br) + (BOX_GAP + bw if br else 0)

    section = GRID_TOP + rows * 18 + FIRE_ROOM
    h = section + BOX_GAP + INVENTORY_BOX
    iw = max(pw(False), pw(True))
    w = pw(barrel)
    x, y = (iw - w) // 2, 0
    c = cw(barrel)
    return dict(iw=iw, ih=h, x=x, y=y, w=w, h=h, cw=c, bw=bw, grid=x + (c - gw) // 2, barrel_box=x + c + BOX_GAP,
                barrel=x + c + BOX_GAP + MARGIN, section=section, inv=(x + (w - 162) // 2 + 1, y + section + BOX_GAP + INVENTORY_TOP),
                rows=rows, grids=grids)


# ---------------------------------------------------------------- flames with the SHAPE switch
def height(col, millis, target, calm, broad):
    t = millis / 1000.0 * (1.0 if calm == b.LIVELY else 0.55 if calm == b.MEDIUM else 0.35)
    flick = millis // b.flick_ms(calm)
    spacing = 14 if broad else 10
    best = 0.0
    first = col // spacing - 1
    for j in range(first, first + 3):
        center = j * spacing + spacing // 2 + 1.5 * math.sin(t * 1.7 + j * 2.1)
        half = 6.5 + (hash_(j, 5) & 3) * 0.6 if broad else 4.5 + (hash_(j, 5) & 3) * 0.5
        skew = 0.3 if (hash_(j, 3) & 1) == 0 else -0.3
        off = col + 0.5 - center
        d = abs(off) / (half * (1 + skew if off < 0 else 1 - skew))
        if d >= 1:
            continue
        if calm == b.LIVELY:
            amp = 0.78 + 0.3 * math.sin(t * 2.3 + j * 1.7) + 0.14 * math.sin(t * 5.1 + j * 0.9) + (hash_(j, 9) & 3) * 0.06
        else:
            amp = 0.8 + 0.12 * math.sin(t * 2.3 + j * 1.7) + (hash_(j, 9) & 3) * 0.04
        best = max(best, amp * math.pow(1 - d, 0.6 if broad else 1.5))
    jitter = 1 if calm == b.LIVELY and (hash_(col, flick) & 3) == 0 else 0
    h = jround(target * ((0.45 + 0.5 * best) if broad else (0.3 + 0.75 * best))) + jitter
    return max(2, min(target * (6 if broad else 8) // 5, h))


def flames(c, x, bottom, width, section, heat, glowing, millis, broad=False):
    orig = b.height
    b.height = lambda col, m, tg, calm: height(col, m, tg, calm, broad)
    try:
        b.flames(c, x, bottom, width, section, heat, glowing, millis)
    finally:
        b.height = orig


def screen(tier, barrel=True, heat='high', glowing=False, millis=1000, broad=False, demo=True):
    L = layout(tier, barrel)
    name, grids, rows, mult = TIERS[tier]
    top = PALETTES[tier]
    c = Canvas(L['iw'], L['ih'])
    px, py, w = L['x'], L['y'], L['w']
    b.box(c, px, py, L['cw'], L['section'], top)
    if barrel:
        b.box(c, L['barrel_box'], py, L['bw'], L['section'], BARREL)
    b.box(c, px, py + L['section'] + BOX_GAP, w, L['h'] - L['section'] - BOX_GAP, INVENTORY)
    flames(c, px + b.FRAME, py + L['section'] - b.FRAME_BOTTOM, L['cw'] - 2 * b.FRAME, L['section'], heat, glowing, millis, broad)
    items = []
    for col in range(grids * 3):
        for r in range(rows):
            x, y = L['grid'] + 1 + col * 18, py + GRID_TOP + 1 + r * 18
            b.slot(c, x, y, top)
            i = (col // 3) * rows * 3 + r * 3 + col % 3
            if demo and i in b.DEMO:
                item, state, pct = b.DEMO[i]
                b.slot_state(c, state, pct, x, y, millis, 'FILL')
                items.append((item, x, y))
            if barrel:
                bx = L['barrel'] + 1 + col * 18
                b.slot(c, bx, y, BARREL)
                if demo and i == 0:
                    items.append(('cod', bx, y))
    ix, iy = L['inv']
    for r in range(4):
        for col in range(9):
            x, y = ix + col * 18, iy + r * 18 + (4 if r == 3 else 0)
            b.slot(c, x, y, INVENTORY)
            if demo and r * 9 + col in b.INV_ITEMS:
                c.item(b.INV_ITEMS[r * 9 + col], x, y)
    for item, x, y in items:
        c.item(item, x, y)
    big = c.image().resize((L['iw'] * K, L['ih'] * K), Image.Resampling.NEAREST)
    d = ImageDraw.Draw(big)
    d.text(((px + 8) * K, (py + 6) * K - 2), f'{name} Schmelztiegel', font=FONT, fill=b.argb(top[5])[:3])
    if mult > 1:
        bonus = f'x{mult} Stapel'
        d.text(((px + L['cw'] - 8) * K - d.textlength(bonus, font=FONT), (py + 6) * K - 2), bonus, font=FONT, fill=b.argb(top[5])[:3])
    d.text((ix * K, (iy - 11) * K - 2), 'Inventar', font=FONT, fill=(64, 64, 64))
    return big


def ui_sheet():
    rows = []
    for tier in TIERS:
        n = TIERS[tier][1] * TIERS[tier][2] * 3
        rows.append((TIERS[tier][0], [('vorher (N12b, 9 Fass-Felder)', b.screen(tier, barrel=True)),
                                     (f'nachher ({n} Fass-Felder, eigener Kasten)', screen(tier))]))
    return b.n12.sheet(rows, 'Schmelztiegel N12c: Fass-Kasten mit demselben Rahmen wie die anderen, so viele Fass-Felder wie Tiegel-Plaetze '
                             '(6/9/18/27, Rest bleibt verwahrt)', OUT / 'crucible-n12c-ui.png')


def fire_gif():
    L = layout('netherite', False)
    cases = [('niedrig', 'high', True), ('mittel', 'medium', False), ('hoch', 'high', False), ('extrem', 'extreme', False)]
    frames = []
    for i in range(36):
        t = i * 110
        grid = []
        for label, heat, glow in cases:
            row = []
            for broad in (False, True):
                img = screen('netherite', barrel=False, heat=heat, glowing=glow, millis=t, broad=broad, demo=False)
                img = img.crop((0, 0, img.width, L['section'] * K))
                d = ImageDraw.Draw(img)
                text = f'{label} - {"breit (Vorschau)" if broad else "aktuell (spitz)"}'
                d.rectangle((img.width - 380, 4, img.width - 8, 30), fill=SHEET_BG)
                d.text((img.width - 374, 6), text, font=FONT, fill='white')
                row.append(img)
            grid.append(row)
        cw, ch = grid[0][0].width, grid[0][0].height
        frame = Image.new('RGB', (cw * 2 + 20, ch * 4 + 60), SHEET_BG)
        for r, row in enumerate(grid):
            for col, img in enumerate(row):
                frame.paste(img, (col * (cw + 20), r * (ch + 20)))
        frames.append(frame.resize((frame.width // 2, frame.height // 2), Image.Resampling.LANCZOS))
    path = OUT / 'crucible-n12c-feuer.gif'
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=110, loop=0)
    return path


def main():
    for path in (ui_sheet(), fire_gif()):
        print('preview', path)


if __name__ == '__main__':
    main()
