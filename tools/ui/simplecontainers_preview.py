"""simplecontainers W0-B: previews of every container screen in the shared N12 box style.

Plan: docs/ai/PLAN-SIMPLECONTAINERS-2026-10-08.md (W0 part B), measurements docs/ai/PLAN-CRUCIBLE-N12B-2026-10-06.md.
References: previews/refs-n12/bild3-container-stil.webp (one coloured box per block, light inventory box below, sunk
rounded slots, symbols instead of text) and bild4-ofen-stil.png (fuel slot shows the flames in itself, big result slot,
arrow, faint light/dark motifs in the box).

Usage (Pillow + numpy, vanilla 26.3 textures unpacked from the client jar):
  python3.12 tools/ui/simplecontainers_preview.py [--vanilla DIR] [--out DIR]
    DIR defaults: /root/vanilla263/assets/minecraft/textures, /root/previews/simplecontainers
  -> <out>/<group>-<ui>.png   one file per screen: vanilla before (where there is one) | new style
     <out>/kontakt-g1..g4.png contact sheet per group, <out>/kontakt-alle.png everything small,
     <out>/vergleich-bild3-bild4.png  references next to the matching new screens.
1 GUI pixel = 3 image pixels (K), like the crucible previews.

Python port of the style blocks; the numbers are exactly CrucibleScreen's (box, slot, furnaceFill) and
CrucibleFlames'. New blocks (bigSlot, arrow, plus, symbols, motifs, box split) are specified here first; the Java
port (simplelib com.simplelib.client.ui) must take the same numbers - see README in the output folder.
Slot positions: vanilla menu classes (checked against the slot frames in the vanilla container textures) and the mod
menus (TieredChestMenu, BackpackLayout, AutoSmitherMenu, FletchingMenu, NetheriteHopperScreen, HorseshoePanel,
CrucibleMenu).
"""
from pathlib import Path
import argparse
import json
import math
import numpy as np
from PIL import Image, ImageDraw

K = 3
SHEET_BG = (41, 45, 53)
CAPTION = (226, 230, 236)
CAPTION_DIM = (150, 158, 170)

# ------------------------------------------------------------------ CrucibleScreen constants (keep in step)
RIM = 0xFF1E1F23
FRAME, FRAME_BOTTOM, BOX_GAP = 5, 7, 2
CUT_OUTER, CUT_BEVEL, CUT_INNER, CUT_NONE = (3, 1, 1), (2, 1), (1,), ()
FILL_COOK = (0xFFFFAE1E, 0xFFF26B12, 0xFFFFE34A)
FILL_COLD = (0xFF4A86DA, 0xFF2C5DB0, 0xFFBFE0FF)
FILL_BLAST = (0xFFFFD04A, 0xFFF58A1C, 0xFFFFF6B0)
FILL_BLAZE = (0xFFFFC21E, 0xFFE0700E, 0xFFFFF08A)
FIRE = (0xFFC81E12, 0xFFE8451A, 0xFFF7921C, 0xFFFFC832, 0xFFFFE98A, 0xFF7A1808, 0xFFFF7A1E, 0xFFFFE070)
GREEN, GREEN_LIGHT = 0xFF52B13C, 0xFF9BE07F
PROGRESS = 0xFFFFFFFF          # arrow progress (image 4: white arrow)
PROGRESS_SHADOW = 0xFF3A3A3A


def scale(color, f):
    r, g, b = (min(255, int(((color >> s) & 255) * f)) for s in (16, 8, 0))
    return 0xFF000000 | r << 16 | g << 8 | b


def mix(a, b, t):
    r = [int(((a >> s) & 255) * (1 - t) + ((b >> s) & 255) * t) for s in (16, 8, 0)]
    return 0xFF000000 | r[0] << 16 | r[1] << 8 | r[2]


def lum(c):
    return 0.299 * ((c >> 16) & 255) + 0.587 * ((c >> 8) & 255) + 0.114 * (c & 255)


def argb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255, (v >> 24) & 255)


def hexs(c):
    return '#%06X' % (c & 0xFFFFFF)


# ------------------------------------------------------------------ palettes
class Pal(tuple):
    """fill, light, shade, slot, slotTop, label (CrucibleScreen.Palette order) + motif kind."""
    def __new__(cls, fill, light, shade, slot, slot_top, label, motif='none', name=''):
        p = super().__new__(cls, (fill, light, shade, slot, slot_top, label))
        p.motif, p.name = motif, name
        return p


def derive(name, fill, motif='none', label=None, light=None, slot=None):
    """Palette from one fill colour with the crucible ratios (light ~1.27, shade 0.82, slot 0.78, slotTop 0.64)."""
    dark = lum(fill) < 110
    light = light or (mix(fill, 0xFFFFFFFF, 0.26) if dark else scale(fill, 1.27) if lum(fill) < 190 else mix(fill, 0xFFFFFFFF, 0.6))
    label = label or (0xFFF2EEE8 if lum(fill) < 125 else 0xFF2E3034)
    slot = slot or scale(fill, 0.78)
    return Pal(fill, light, scale(fill, 0.82), slot, scale(fill, 0.64), label, motif, name)


# The crucible's palettes exactly (CrucibleScreen.palette / INVENTORY / BARREL).
INVENTORY = Pal(0xFFE3E6E9, 0xFFF8F9FA, 0xFFC5CACE, 0xFFB4BABF, 0xFF979DA3, 0xFF404040, 'none', 'Inventar')
BARREL = Pal(0xFFB9774F, 0xFFD08F68, 0xFF955839, 0xFF94573A, 0xFF74412B, 0xFF404040, 'wood', 'Fass')
IRON = Pal(0xFF9A9DA2, 0xFFC4C7CB, 0xFF7E8186, 0xFF7B7E83, 0xFF64676C, 0xFF2E3034, 'metal', 'Eisen')
REINFORCED = Pal(0xFF6F9095, 0xFF9DBCC1, 0xFF587378, 0xFF55737A, 0xFF425C61, 0xFFF0F6F6, 'metal', 'Verstaerkt')
NETHERITE = Pal(0xFF5F524C, 0xFF867870, 0xFF4A3F3A, 0xFF473C37, 0xFF352C28, 0xFFEFE4DA, 'nether', 'Netherit')
ENDERITE = Pal(0xFF8E6CB0, 0xFFB99AD6, 0xFF735693, 0xFF70538E, 0xFF594073, 0xFFF7F0FF, 'ender', 'Enderit')

P = {
    # G1 storage (fills from image 3 where it shows the block, else the block texture's mean colour, muted)
    'chest_oak': derive('Truhe (Eiche)', 0xFFCE9148, 'wood'),
    'barrel': BARREL,
    'ender_chest': derive('Endertruhe', 0xFF597880, 'ender'),
    'shulker_purple': derive('Shulkerkiste lila', 0xFF876C99, 'shulker'),
    'shulker_light_blue': derive('Shulkerkiste hellblau', 0xFF5C97B8, 'shulker'),
    'hopper': derive('Trichter', 0xFF5A5C63, 'metal'),
    'dispenser': derive('Spender/Werfer', 0xFF878787, 'stone'),
    'crafter': derive('Crafter', 0xFF7A736A, 'redstone'),
    'horse': derive('Reittier (Sattelleder)', 0xFF8B5E3C, 'leather'),
    # G2
    'crafting': derive('Werkbank', 0xFFB7935B, 'wood'),
    'furnace': derive('Ofen', 0xFF929699, 'stone'),
    'blast_furnace': derive('Schmelzofen', 0xFF6E7179, 'metal'),
    'smoker': derive('Raeucherofen', 0xFF7D6B57, 'smoke'),
    'brewing': derive('Braustand', 0xFF847D7D, 'stone'),
    'beacon': derive('Leuchtfeuer', 0xFF6FB4B1, 'glass'),
    'enchanting': derive('Zaubertisch', 0xFFA1282B, 'rune'),
    # G3
    'anvil': derive('Amboss', 0xFF666666, 'metal'),
    'grindstone': derive('Schleifstein', 0xFF9E9A92, 'stone'),
    'stonecutter': derive('Steinsaege', 0xFF857A72, 'stone'),
    'loom': derive('Webstuhl', 0xFF9C8262, 'wool'),
    'cartography': derive('Kartentisch', 0xFF6B5A45, 'paper'),
    'smithing': derive('Schmiedetisch', 0xFF4B1E19, 'metal'),
    'merchant': derive('Handel', 0xFF3F8A55, 'leather'),
    'player': INVENTORY,
    # G4 mod
    'chest_reinforced': REINFORCED,
    'chest_netherite': NETHERITE,
    'chest_enderite': ENDERITE,
    'backpack': derive('Rucksack (Leder)', 0xFF8E6440, 'leather'),
    'backpack_enderite': ENDERITE,
    'auto_smither': derive('Auto-Schmied', 0xFF4F5560, 'redstone'),
    'fletching': derive('Befiederungstisch', 0xFFC5B485, 'wood'),
    'hopper_netherite': NETHERITE,
    'horseshoe': IRON,
    'crucible_iron': IRON,
}


# ------------------------------------------------------------------ canvas
class Canvas:
    def __init__(self, w, h, bg=SHEET_BG):
        self.w, self.h = w, h
        self.a = np.zeros((h, w, 3), dtype=np.float64)
        self.a[:] = bg
        self.ox = self.oy = 0          # GUI origin (lets screens draw at negative x, e.g. the hoof panel)

    def fill(self, x0, y0, x1, y1, color):
        x0, x1, y0, y1 = x0 + self.ox, x1 + self.ox, y0 + self.oy, y1 + self.oy
        x0, x1, y0, y1 = max(0, x0), min(self.w, x1), max(0, y0), min(self.h, y1)
        if x1 <= x0 or y1 <= y0:
            return
        r, g, b, a = argb(color)
        k = a / 255.0
        self.a[y0:y1, x0:x1] = self.a[y0:y1, x0:x1] * (1 - k) + np.array([r, g, b]) * k

    def px(self, x, y, color):
        self.fill(x, y, x + 1, y + 1, color)

    def rgba(self, img, x, y, tint=None, alpha=1.0):
        px = np.asarray(img.convert('RGBA'), dtype=np.float64)
        h, w = px.shape[:2]
        x, y = x + self.ox, y + self.oy
        sx0, sy0 = max(0, -x), max(0, -y)
        x0, y0, x1, y1 = max(0, x), max(0, y), min(self.w, x + w), min(self.h, y + h)
        if x1 <= x0 or y1 <= y0:
            return
        src = px[sy0:sy0 + y1 - y0, sx0:sx0 + x1 - x0]
        al = src[:, :, 3:4] / 255.0 * alpha
        col = src[:, :, :3] if tint is None else np.array(argb(tint)[:3], dtype=np.float64)
        region = self.a[y0:y1, x0:x1]
        region[:] = region * (1 - al) + col * al

    def image(self):
        return Image.fromarray(np.clip(self.a, 0, 255).astype(np.uint8), 'RGB')


TEX = None   # set in main


def tex(rel):
    return Image.open(TEX / rel)


def item_img(name):
    for d, suffix in (('item', ''), ('block', ''), ('block', '_side'), ('block', '_front'), ('block', '_top')):
        p = TEX / d / f'{name}{suffix}.png'
        if p.exists():
            im = Image.open(p).convert('RGBA')
            return im.crop((0, 0, 16, 16))
    raise FileNotFoundError(name)


def item(c, name, x, y, count=None, veil=None):
    c.rgba(item_img(name), x, y)
    if veil is not None:
        c.fill(x, y, x + 16, y + 16, veil)
    if count:
        s = str(count)
        w = text_width(s)
        text(c, s, x + 17 - w + 1, y + 9 + 1, 0xFF3F3F3F)
        text(c, s, x + 17 - w, y + 9, 0xFFFFFFFF)


def ghost(c, name, x, y, pal):
    """A filter/reserved ghost: the item faint behind a slot-coloured veil (CrucibleScreen.veil)."""
    item(c, name, x, y)
    color = 0xA8000000 | (pal[3] & 0xFFFFFF)
    c.fill(x + 1, y, x + 15, y + 16, color)
    c.fill(x, y + 1, x + 1, y + 15, color)
    c.fill(x + 15, y + 1, x + 16, y + 15, color)


# ------------------------------------------------------------------ vanilla bitmap font (default.json providers)
class Font:
    def __init__(self):
        self.glyphs = {}
        d = json.loads((TEX.parent / 'font/include/default.json').read_text())
        for p in d['providers']:
            if p.get('type') != 'bitmap' or not p['file'].endswith(('ascii.png', 'accented.png', 'nonlatin_european.png')):
                continue
            img = np.asarray(tex('font/' + p['file'].split('/')[-1]).convert('RGBA'))
            rows = p['chars']
            cw, ch = img.shape[1] // len(rows[0]), img.shape[0] // len(rows)
            ascent = p.get('ascent', 7)
            for r, row in enumerate(rows):
                for col, char in enumerate(row):
                    if char == '\x00' or char in self.glyphs:
                        continue
                    cell = img[r * ch:(r + 1) * ch, col * cw:(col + 1) * cw, 3] > 0
                    cols = np.where(cell.any(0))[0]
                    width = cols.max() + 1 if len(cols) else 0
                    self.glyphs[char] = (cell, width, 7 - ascent)


FONT = None


def text_width(s):
    return sum(4 if ch == ' ' else FONT.glyphs.get(ch, FONT.glyphs['?'])[1] + 1 for ch in s) - 1


def text(c, s, x, y, color):
    for ch in s:
        if ch == ' ':
            x += 4
            continue
        cell, width, dy = FONT.glyphs.get(ch, FONT.glyphs['?'])
        ys, xs = np.nonzero(cell)
        for yy, xx in zip(ys, xs):
            c.px(x + xx, y + yy + dy, color)
        x += width + 1


# ------------------------------------------------------------------ style blocks (CrucibleScreen port)
def rounded(c, x, y, w, h, cuts, color):
    n = len(cuts)
    for k in range(n):
        c.fill(x + cuts[k], y + k, x + w - cuts[k], y + k + 1, color)
        c.fill(x + cuts[k], y + h - 1 - k, x + w - cuts[k], y + h - k, color)
    c.fill(x, y + n, x + w, y + h - n, color)


def box(c, x, y, w, h, p, shadow=2):
    """CrucibleScreen.box (shadow=2: 5 px frame, 7 px at the bottom). shadow=0 = the tight variant (5 px all round)."""
    rounded(c, x, y, w, h, CUT_OUTER, RIM)
    if shadow:
        rounded(c, x + 1, y + 1, w - 2, h - 2, CUT_BEVEL, scale(p[0], 0.40))
    rounded(c, x + 1, y + 1, w - 2, h - 2 - shadow, CUT_BEVEL, scale(p[0], 0.82))
    rounded(c, x + 2, y + 2, w - 4, h - 4 - shadow, CUT_INNER, scale(p[0], 0.64))
    rounded(c, x + 4, y + 4, w - 8, h - 8 - shadow, CUT_INNER, p[1])
    rounded(c, x + 5, y + 5, w - 10, h - 10 - shadow, CUT_NONE, p[0])


def slot_rect(c, x, y, w, h, p):
    """CrucibleScreen.slot generalised to w x h (16x16 = the slot; light edge below/right outside the area)."""
    c.fill(x + 1, y + h, x + w, y + h + 1, p[1])
    c.fill(x + w, y + 1, x + w + 1, y + h, p[1])
    c.fill(x + 1, y, x + w - 1, y + h, p[3])
    c.fill(x, y + 1, x + 1, y + h - 1, scale(p[4], 1.12))
    c.fill(x + w - 1, y + 1, x + w, y + h - 1, p[3])
    c.fill(x + 1, y, x + w - 1, y + 1, p[4])


def slot(c, x, y, p):
    slot_rect(c, x, y, 16, 16, p)


def big_slot(c, x, y, p):
    """Result slot like image 4 / the vanilla 26x26 frame: 24x24 around the 16x16 slot at (x, y)."""
    slot_rect(c, x - 4, y - 4, 24, 24, p)


def inset(c, x, y, w, h, p):
    """Sunk field (name bar, option rows, previews): slot look, any size, slot colour a little lighter."""
    q = (p[0], p[1], p[2], mix(p[3], p[0], 0.35), p[4], p[5])
    slot_rect(c, x, y, w, h, q)


def raised(c, x, y, w, h, p, color=None):
    """A raised button/tile: light top/left, dark bottom/right (opposite of a slot)."""
    color = color or p[0]
    c.fill(x + 1, y, x + w - 1, y + h, color)
    c.fill(x, y + 1, x + w, y + h - 1, color)
    c.fill(x + 1, y, x + w - 1, y + 1, mix(color, 0xFFFFFFFF, 0.35))
    c.fill(x, y + 1, x + 1, y + h - 1, mix(color, 0xFFFFFFFF, 0.2))
    c.fill(x + 1, y + h - 1, x + w - 1, y + h, scale(color, 0.62))
    c.fill(x + w - 1, y + 1, x + w, y + h - 1, scale(color, 0.72))


def furnace_fill(c, x, y, level, colors, millis):
    """CrucibleScreen.furnaceFill (image 4's fuel slot)."""
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


def fuel_slot(c, x, y, p, level, colors=FILL_COOK, millis=0):
    """NEW (image 4): the fuel slot is its own burn display - dim tongue silhouettes over the whole slot (so the slot
    reads as 'fire' even when cold), the burn time fills it from below (furnace_fill)."""
    slot(c, x, y, p)
    sil = scale(p[3], 0.86)
    for i in range(3):
        tx = x + 2 + i * 5
        for k in range(11):
            dx = 0 if ((k + i) & 2) == 0 else 1
            c.fill(tx + dx, y + 13 - k, tx + dx + 2, y + 14 - k, sil)
    furnace_fill(c, x, y, level, colors, millis)


# ------------------------------------------------------------------ symbols (own pixel art; '#' = engraved stroke)
def bitmap(rows):
    return [(x, y) for y, row in enumerate(rows) for x, ch in enumerate(row) if ch == '#']


ARROW = bitmap([          # 22x15, image 3's chunky arrow
    '..............#.......',
    '..............##......',
    '..............###.....',
    '..............####....',
    '..............#####...',
    '###################...',
    '####################..',
    '#####################.',
    '####################..',
    '###################...',
    '..............#####...',
    '..............####....',
    '..............###.....',
    '..............##......',
    '..............#.......',
])
ARROW_SMALL = bitmap([    # 16x11 (2x2 crafting in the inventory/backpack)
    '..........#.....',
    '..........##....',
    '..........###...',
    '#############...',
    '##############..',
    '###############.',
    '##############..',
    '#############...',
    '..........###...',
    '..........##....',
    '..........#.....',
])
ARROW_DOWN = bitmap([     # 9x26, brewing progress (vanilla 9x28 spot)
    '...###...', '...###...', '...###...', '...###...', '...###...', '...###...', '...###...', '...###...',
    '...###...', '...###...', '...###...', '...###...', '...###...', '...###...', '...###...', '...###...',
    '...###...', '...###...', '#########', '.#######.', '..#####..', '..#####..', '...###...', '...###...',
    '....#....', '....#....',
])
PLUS = bitmap([
    '....###....', '....###....', '....###....', '....###....',
    '###########', '###########', '###########',
    '....###....', '....###....', '....###....', '....###....',
])
HEAT = bitmap([           # 14x13: three rising heat waves (image 3, furnace tile)
    '.#....#....#..',
    '#....#....#...',
    '#....#....#...',
    '.#....#....#..',
    '..#....#....#.',
    '..#....#....#.',
    '.#....#....#..',
    '#....#....#...',
    '#....#....#...',
    '.#....#....#..',
    '..#....#....#.',
    '..#....#....#.',
    '.#....#....#..',
])
SMOKE = bitmap([          # 14x13: smoker - soft curls instead of sharp waves
    '..##....##....',
    '.#..#..#..#...',
    '....#.....#...',
    '...#.....#....',
    '..#.....#.....',
    '..#.....#.##..',
    '...#.....#..#.',
    '....#......#..',
    '....#.....#...',
    '...#.....#....',
    '..#.....#.....',
    '..#.....#.....',
    '...#.....#....',
])
CROSS = bitmap(['#.....#', '##...##', '.##.##.', '..###..', '.##.##.', '##...##', '#.....#'])
CHECK = bitmap(['......#', '.....##', '#...##.', '##.##..', '.###...', '..#....'])
WHEEL = bitmap([          # 19x19 grindstone wheel
    '......#######......', '....##.......##....', '...#...........#...', '..#.....###.....#..', '.#....#######....#.',
    '.#...###...###...#.', '#...##.......##...#', '#...##..###..##...#', '#..##..#####..##..#', '#..##..#####..##..#',
    '#..##..#####..##..#', '#...##..###..##...#', '#...##.......##...#', '.#...###...###...#.', '.#....#######....#.',
    '..#.....###.....#..', '...#...........#...', '....##.......##....', '......#######......',
])
BOOK = bitmap([           # 26x18 open book (enchanting)
    '.####.............####....', '#....###.........##...#...', '#.......##.....##......#..', '#.##......#.#.#..##....#..',
    '#...##.....#.#.....##..#..', '#.##........#.....##....#..', '#....###....#...##......#.', '#.##........#........##.#.',
    '#....##.....#......##...#.', '#.###.......#.....##.....#.', '#......##...#...........#.', '#.##........#....###....#.',
    '##.....##...#.........###.', '..###.....#.#.#....###....', '.....####..###..###.......', '.........####.###.........',
    '..........................', '..........................',
])
FUNNEL = bitmap([         # 12x12 filter funnel
    '############', '#..........#', '.#........#.', '..#......#..', '...#....#...', '....#..#....',
    '....#..#....', '....#..#....', '....#..#....', '....#..#....', '.....##.....', '............',
])
STACK = bitmap(['..#####', '..#...#', '#####.#', '#...#.#', '#...###', '#...#..', '#####..'])
REDSTONE = bitmap([       # 12x12 dust blob (crafter powered sign)
    '....#..#....', '.#.###.##...', '..#####.#.#.', '.########...', '#.#######.#.', '.#########..',
    '..########.#', '.#.######...', '...##.###.#.', '..#..#.#....', '.....#......', '............',
])
ANVIL_HAMMER = bitmap(['.#####....', '.#####....', '.#####....', '...#......', '...#......', '...#......',
                       '...#......', '...#......'])
XP = bitmap(['..###..', '.#####.', '###.###', '##...##', '###.###', '.#####.', '..###..'])


def symbol(c, bm, x, y, p, color=None, light=True):
    """Engraved like image 3: stroke in the slot colour, a 1 px light edge under it (sunk, like the slots)."""
    color = color or p[3]
    if light:
        s = set(bm)
        for (dx, dy) in bm:
            if (dx, dy + 1) not in s:
                c.px(x + dx, y + dy + 1, p[1])
    for (dx, dy) in bm:
        c.px(x + dx, y + dy, color)


def symbol_progress(c, bm, x, y, p, frac, color=PROGRESS, vertical=False):
    """Engraved symbol, filled with the progress colour up to frac (left to right, or top to bottom)."""
    symbol(c, bm, x, y, p)
    if frac <= 0:
        return
    w = max(dx for dx, _ in bm) + 1
    h = max(dy for _, dy in bm) + 1
    for (dx, dy) in bm:
        if (dy if vertical else dx) < frac * (h if vertical else w):
            c.px(x + dx, y + dy, color)


def bm_size(bm):
    return max(dx for dx, _ in bm) + 1, max(dy for _, dy in bm) + 1


def sprite_mask(c, rel, x, y, color):
    """A vanilla empty-slot sprite (saddle, potion, lapis ...) as an engraved silhouette inside the slot."""
    c.rgba(tex('gui/sprites/' + rel), x, y, tint=color, alpha=0.8)


# ------------------------------------------------------------------ motifs (image 4: faint marks in the box)
def hash_(a, b):
    h = (a * 374761393 + b * 668265263) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return h ^ (h >> 16)


MOTIFS = {
    # list of shapes; each shape = (dark pixels, light pixels) as offsets
    'wood': [([(0, 0), (1, 0), (2, 0), (3, 0), (4, 0), (4, 1)], []), ([(0, 1), (0, 0), (1, 0), (2, 0)], []),
             ([(0, 0), (1, 0), (2, 0), (3, 0), (4, 0), (5, 0), (6, 0)], []), ([(1, 0), (2, 0), (3, 0), (0, 1), (4, 1)], [])],
    'stone': [  # image 4: branched cracks, dark stroke with a light lip below
        ([(0, 1), (1, 1), (2, 2), (3, 2), (4, 3), (5, 3), (6, 2), (7, 1), (4, 4), (4, 5), (3, 6)],
         [(0, 2), (1, 2), (2, 3), (3, 3), (5, 4), (6, 3), (5, 5)]),
        ([(0, 4), (1, 3), (2, 3), (3, 2), (4, 1), (4, 0), (5, 2), (6, 2), (7, 3), (2, 4), (2, 5)],
         [(1, 4), (3, 3), (5, 3), (6, 3), (3, 5)]),
        ([(0, 0), (1, 0), (2, 1), (3, 1), (3, 2), (4, 3)], [(0, 1), (1, 1), (2, 2), (4, 4)])],
    'smoke': [([(0, 2), (1, 1), (2, 1), (3, 2), (4, 2), (5, 1)], []), ([(0, 0), (1, 1), (2, 1), (3, 0)], [])],
    'metal': [([(1, 1)], [(0, 0), (1, 0), (0, 1)]), ([(0, 0), (1, 1), (2, 2)], [(1, 0), (2, 1)])],
    'nether': [([(0, 2), (1, 1), (2, 0)], [(1, 2), (2, 1)]), ([(0, 0), (1, 0)], [(0, 1), (1, 1)])],
    'ender': [([], [(1, 0), (0, 1), (1, 1), (2, 1), (1, 2)]), ([(0, 0)], []), ([], [(0, 0)])],
    'shulker': [([(0, 0), (1, 1), (2, 1), (3, 0)], [(1, 0), (2, 0)]), ([(0, 1), (1, 0), (2, 0), (3, 1)], [])],
    'leather': [],      # stitch seam, see motif()
    'redstone': [([(0, 0), (1, 0), (2, 1), (3, 1), (4, 1)], []), ([(0, 0), (1, 1), (1, 2)], [])],
    'wool': [([(0, 0), (2, 0), (1, 1), (0, 2), (2, 2)], []), ([(0, 0), (1, 0), (2, 0)], [(0, 1), (2, 1)])],
    'paper': [([(0, 0), (2, 0), (4, 0), (6, 0)], []), ([(0, 0), (0, 2), (0, 4)], [])],
    'glass': [([], [(0, 2), (1, 1), (2, 0)]), ([], [(0, 3), (1, 2), (2, 1), (3, 0)])],
    'rune': [([(0, 0), (1, 0), (1, 1), (1, 2), (2, 2)], []), ([(0, 0), (0, 1), (1, 1), (2, 1), (2, 0)], []),
             ([(1, 0), (0, 1), (2, 1), (1, 2)], [])],
    'none': [],
}


def overlaps(r, rects, pad=2):
    x0, y0, x1, y1 = r
    return any(x0 < b[2] + pad and x1 > b[0] - pad and y0 < b[3] + pad and y1 > b[1] - pad for b in rects)


def motif(c, x, y, w, h, p, avoid, seed=1):
    """Faint marks inside a box interior (x, y, w, h = the fill area). Dark = 0.9 x fill, light = halfway to light."""
    kind = p.motif
    dark, light = scale(p[0], 0.90), mix(p[0], p[1], 0.5)
    if kind == 'leather':          # stitched seam 2 px inside the fill, 2 on / 2 off
        for i in range(x + 2, x + w - 2):
            if (i - x) % 4 < 2:
                for yy in (y + 1, y + h - 2):
                    if not overlaps((i, yy, i + 1, yy + 1), avoid, 0):
                        c.px(i, yy, light)
        for j in range(y + 3, y + h - 3):
            if (j - y) % 4 < 2:
                for xx in (x + 1, x + w - 2):
                    if not overlaps((xx, j, xx + 1, j + 1), avoid, 0):
                        c.px(xx, j, light)
        return
    if kind == 'metal':            # rivets in the inner corners + a few scratches
        for (cx, cy) in ((x + 1, y + 1), (x + w - 3, y + 1), (x + 1, y + h - 3), (x + w - 3, y + h - 3)):
            if not overlaps((cx, cy, cx + 2, cy + 2), avoid, 0):
                c.fill(cx, cy, cx + 2, cy + 2, scale(p[0], 0.78))
                c.px(cx, cy, p[1])
    shapes = MOTIFS.get(kind, [])
    if not shapes:
        return
    n = max(2, w * h // 650)
    placed = []
    for i in range(n * 6):
        if len(placed) >= n:
            break
        hsh = hash_(i + seed * 97, w * 31 + h)
        sh = shapes[hsh % len(shapes)]
        pts = sh[0] + sh[1]
        sw = max(dx for dx, _ in pts) + 1
        sh_h = max(dy for _, dy in pts) + 1
        sx = x + 2 + (hsh >> 4) % max(1, w - sw - 4)
        sy = y + 2 + (hsh >> 12) % max(1, h - sh_h - 4)
        r = (sx, sy, sx + sw, sy + sh_h)
        if overlaps(r, avoid) or overlaps(r, placed, 10):
            continue
        placed.append(r)
        for (dx, dy) in sh[0]:
            c.px(sx + dx, sy + dy, dark)
        for (dx, dy) in sh[1]:
            c.px(sx + dx, sy + dy, light)


# ------------------------------------------------------------------ flames (CrucibleFlames port, POINTED)
def flame_height(col, millis, target, calm):
    t = millis / 1000.0 * (1.0 if calm == 0 else 0.55 if calm == 1 else 0.35)
    flick = millis // (110 if calm == 0 else 190 if calm == 1 else 280)
    best, spacing = 0.0, 10
    first = math.floor(col / spacing) - 1
    for j in range(first, first + 3):
        center = j * spacing + spacing // 2 + 1.5 * math.sin(t * 1.7 + j * 2.1)
        half = 4.5 + (hash_(j, 5) & 3) * 0.5
        skew = 0.3 if (hash_(j, 3) & 1) == 0 else -0.3
        off = col + 0.5 - center
        d = abs(off) / (half * ((1 + skew) if off < 0 else (1 - skew)))
        if d >= 1:
            continue
        amp = (0.78 + 0.3 * math.sin(t * 2.3 + j * 1.7) + 0.14 * math.sin(t * 5.1 + j * 0.9) + (hash_(j, 9) & 3) * 0.06
               if calm == 0 else 0.8 + 0.12 * math.sin(t * 2.3 + j * 1.7) + (hash_(j, 9) & 3) * 0.04)
        best = max(best, amp * (1 - d) ** 1.5)
    jitter = 1 if calm == 0 and (hash_(col, flick) & 3) == 0 else 0
    h = int(round(target * (0.3 + 0.75 * best))) + jitter
    return max(2, min(target * 8 // 5, h))


def flame_shade(k, h, edge, ember, calm):
    if k == 0:
        return ember
    if edge <= 1:
        return 0
    if calm == 2:
        return 1
    if calm == 1:
        return 3 if k <= 1 else 1 if edge == 2 else 2
    if k <= 2:
        return 3
    if edge == 2:
        return 1
    return 3 if k < h * 35 // 100 and edge >= 4 else 2


def flames(c, x, bottom, width, section, millis=1234, part=1.0 / 3.0, calm=0, colors=FIRE):
    target = int(round(section * part))
    flick = millis // (110 if calm == 0 else 190)
    hs = [flame_height(i - 3, millis, target, calm) for i in range(width + 6)]
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
            c.px(x + col, bottom - 1 - k, colors[flame_shade(k, h, edge, ember, calm)])


# ------------------------------------------------------------------ box split (NEW rule, see README "Kasten-Fuge")
def split(top_bottom, inv_top):
    """How the container box and the inventory box share the rows between the lowest container element
    (top_bottom = first free row under its light edge) and the first inventory slot row (inv_top).
    Returns (mode, container box bottom (exclusive), shadow): 'two' boxes, or 'one' box with a seam."""
    free = inv_top - top_bottom
    if free >= FRAME_BOTTOM + FRAME:              # >= 12: full frames, gap up to 2, the rest pads the container box
        gap = min(BOX_GAP, free - FRAME_BOTTOM - FRAME)
        return 'two', inv_top - FRAME - gap, 2
    if free >= 2 * FRAME:                          # 10..11: container box without its 2 px shadow
        return 'two', inv_top - FRAME - (free - 2 * FRAME), 0
    return 'one', None, 2                          # < 10: one box, inventory part as a light panel behind a seam


class Screen:
    """One screen: image size, container palette, container elements (rects for the split + motif avoidance)."""
    def __init__(self, key, title, w, h, pal, inv=(8, 84), hotbar_gap=4, inv_pal=INVENTORY, top=0):
        self.key, self.title, self.w, self.h, self.p = key, title, w, h, pal
        self.inv, self.hotbar_gap, self.inv_pal, self.top = inv, hotbar_gap, inv_pal, top
        self.rects = []          # (x0, y0, x1, y1) of container elements incl. light edges
        self.ops = []            # deferred draw calls on top of the box

    def add(self, x0, y0, x1, y1):
        self.rects.append((x0, y0, x1, y1))

    def s(self, x, y):           # slot
        self.add(x, y, x + 17, y + 17)
        return (x, y)

    def b(self, x, y):           # big slot
        self.add(x - 4, y - 4, x + 21, y + 21)
        return (x, y)

    def inv_rows(self):
        ix, iy = self.inv
        hot = iy + 58 + (self.hotbar_gap - 4)
        return ix, iy, hot


def draw_screen(c, sc, draw_top, inv_items=True, box_x=0, box_w=None, inv_box=True, pre=None, panel_x=None):
    """Container box (+ split), inventory box, then the screen's own elements (draw_top)."""
    p = sc.p
    if pre:
        pre(c)
    ix, iy, hot = sc.inv_rows()
    box_w = box_w or sc.w
    top_bottom = max(r[3] for r in sc.rects) if sc.rects else iy - 14
    mode, bottom, shadow = split(top_bottom, iy)
    inv_box_x, inv_box_w = ix - 8, 176
    inv_bottom = hot + 17 + FRAME_BOTTOM
    if mode == 'two':
        box(c, box_x, sc.top, box_w, bottom - sc.top, p, shadow)
        interior = (box_x + FRAME, sc.top + FRAME, box_w - 2 * FRAME, bottom - sc.top - FRAME - (FRAME_BOTTOM if shadow else FRAME))
        if inv_box:
            box(c, inv_box_x, iy - FRAME, inv_box_w, inv_bottom - (iy - FRAME), INVENTORY)
    else:
        box(c, box_x, sc.top, box_w, inv_bottom - sc.top, p)
        interior = (box_x + FRAME, sc.top + FRAME, box_w - 2 * FRAME, iy - 3 - sc.top - FRAME)
        if inv_box:
            px0 = panel_x if panel_x is not None else box_x + FRAME
            seam_panel(c, px0, iy - 3, box_x + box_w - FRAME - px0, inv_bottom - FRAME_BOTTOM - (iy - 3), p)
    motif(c, *interior, p, sc.rects + [(box_x + 4, sc.top + 4, box_x + 12 + text_width(sc.title), sc.top + 15)],
          seed=len(sc.key))
    if sc.title:
        text(c, sc.title, box_x + 8, sc.top + 6, p[5])
    if inv_box:
        for r in range(3):
            for col in range(9):
                slot(c, ix + col * 18, iy + r * 18, INVENTORY)
        for col in range(9):
            slot(c, ix + col * 18, hot, INVENTORY)
        if inv_items:
            item(c, 'cobblestone', ix, iy, 64)
            item(c, 'bread', ix + 3 * 18, iy, 12)
            item(c, 'stick', ix, iy + 18, 23)
            item(c, 'torch', ix + 4 * 18, iy + 18, 40)
            item(c, 'iron_pickaxe', ix, hot)
            item(c, 'oak_log', ix + 4 * 18, hot, 32)
            item(c, 'diamond_sword', ix + 1 * 18, hot)
    draw_top(c)
    return mode


def seam_panel(c, x, y, w, h, p):
    """'one' box: the inventory part as a light panel - a seam (groove in the box's band colour) and a light line,
    then the INVENTORY fill down to the frame (the box's own frame closes it at the sides and below)."""
    c.fill(x, y, x + w, y + 1, scale(p[0], 0.64))
    c.fill(x, y + 1, x + w, y + 2, INVENTORY[1])
    c.fill(x, y + 2, x + w, y + h, INVENTORY[0])


# ------------------------------------------------------------------ screens
SCREENS = []   # (group, key, label, fn, vanilla_fn or None)


def screen(group, key, label, vanilla=None):
    def deco(fn):
        SCREENS.append((group, key, label, fn, vanilla))
        return fn
    return deco


def grid(sc, x, y, cols, rows):
    return [sc.s(x + cc * 18, y + r * 18) for r in range(rows) for cc in range(cols)]


def chest_like(key, title, pal, rows, cols=9, w=176, items=()):
    h = 114 + rows * 18
    left = (w - (14 + 18 * cols)) // 2 + 8 if cols > 9 else 8
    sc = Screen(key, title, w, h, pal, inv=((w - 176) // 2 + 8, 18 + rows * 18 + 13))   # ChestMenu / TieredChestMenu
    slots = grid(sc, left, 18, cols, rows)

    def top(c):
        for (x, y) in slots:
            slot(c, x, y, pal)
        for (i, name, n) in items:
            x, y = slots[i]
            item(c, name, x, y, n)
    return sc, top


def render(sc, top, **kw):
    pad_l = kw.pop('pad_l', 0)
    c = Canvas(sc.w + pad_l + 2, sc.h + 4)
    c.ox, c.oy = pad_l + 1, 1
    draw_screen(c, sc, top, **kw)
    return c


@screen('g1', 'truhe-eiche', 'Truhe 3 Reihen (Eiche)', vanilla=('generic', 3))
def s_chest():
    sc, top = chest_like('truhe', 'Truhe', P['chest_oak'], 3, items=[(0, 'oak_planks', 64), (4, 'apple', 7), (10, 'wheat', 31),
                                                                  (13, 'iron_ingot', 9), (22, 'coal', 18)])
    return render(sc, top)


@screen('g1', 'doppeltruhe', 'Grosse Truhe (6 Reihen)', vanilla=('generic', 6))
def s_double():
    sc, top = chest_like('truhe2', 'Grosse Truhe', P['chest_oak'], 6, items=[(0, 'oak_planks', 64), (8, 'apple', 7), (20, 'wheat', 31),
                                                                          (31, 'iron_ingot', 9), (45, 'coal', 18), (53, 'redstone', 40)])
    return render(sc, top)


@screen('g1', 'fass', 'Fass', vanilla=('generic', 3))
def s_barrel():
    sc, top = chest_like('fass', 'Fass', P['barrel'], 3, items=[(0, 'salmon', 12), (1, 'cod', 20), (11, 'potato', 33), (19, 'carrot', 5)])
    return render(sc, top)


@screen('g1', 'endertruhe', 'Endertruhe', vanilla=('generic', 3))
def s_ender():
    sc, top = chest_like('ender', 'Endertruhe', P['ender_chest'], 3, items=[(0, 'ender_pearl', 16), (5, 'diamond', 12), (14, 'emerald', 3)])
    return render(sc, top)


@screen('g1', 'shulker-lila', 'Shulkerkiste (lila)', vanilla=('shulker', 3))
def s_shulker():
    sc, top = chest_like('shulker1', 'Shulkerkiste', P['shulker_purple'], 3, items=[(0, 'shulker_shell', 2), (6, 'chorus_fruit', 40)])
    sc.inv = (8, 84)
    return render(sc, top)


@screen('g1', 'shulker-hellblau', 'Shulkerkiste (hellblau)', vanilla=('shulker', 3))
def s_shulker2():
    sc, top = chest_like('shulker2', 'Hellblaue Shulkerkiste', P['shulker_light_blue'], 3, items=[(2, 'blue_ice', 64), (3, 'snowball', 16)])
    sc.inv = (8, 84)
    return render(sc, top)


@screen('g1', 'trichter', 'Trichter', vanilla=('hopper',))
def s_hopper():
    p = P['hopper']
    sc = Screen('trichter', 'Trichter', 176, 133, p, inv=(8, 51))
    slots = [sc.s(44 + i * 18, 20) for i in range(5)]

    def top(c):
        for (x, y) in slots:
            slot(c, x, y, p)
        item(c, 'iron_ingot', 44, 20, 5)
        item(c, 'gold_nugget', 62, 20, 17)
    return render(sc, top)


@screen('g1', 'spender', 'Spender / Werfer', vanilla=('dispenser',))
def s_dispenser():
    p = P['dispenser']
    sc = Screen('spender', 'Spender', 176, 166, p)
    slots = grid(sc, 62, 17, 3, 3)

    def top(c):
        for (x, y) in slots:
            slot(c, x, y, p)
        item(c, 'arrow', 62, 17, 64)
        item(c, 'water_bucket', 98, 53)
    return render(sc, top)


@screen('g1', 'crafter', 'Crafter (Selbstbaukasten)', vanilla=('crafter',))
def s_crafter():
    p = P['crafter']
    sc = Screen('crafter', 'Crafter', 176, 166, p)
    slots = grid(sc, 26, 17, 3, 3)
    res = sc.b(134, 35)
    sc.add(84, 22, 100, 34)

    def top(c):
        for i, (x, y) in enumerate(slots):
            slot(c, x, y, p)
            if i == 4:   # disabled slot: engraved cross instead of vanilla's red X sprite
                symbol(c, CROSS, x + 4, y + 4, p, color=p[4], light=False)
        for i, n in ((0, 'iron_ingot'), (1, 'iron_ingot'), (2, 'iron_ingot'), (6, 'iron_ingot'), (7, 'iron_ingot'), (8, 'iron_ingot')):
            item(c, n, *slots[i], 2)
        big_slot(c, *res, p)
        item(c, 'bucket', *res)
        symbol(c, REDSTONE, 86, 22, p, color=0xFFD8261E)          # powered: red dust, else engraved
        symbol_progress(c, ARROW, 103, 36, p, 0.0)
    return render(sc, top)


@screen('g1', 'pferd', 'Reittier-Inventar (Esel mit Truhe) + Hufeisen-Panel', vanilla=('horse',))
def s_horse():
    p = P['horse']
    sc = Screen('pferd', 'Esel', 176, 166, p)
    saddle, armor = sc.s(8, 18), sc.s(8, 36)
    sc.add(26, 18, 78, 70)
    chest = grid(sc, 80, 18, 5, 3)

    def top(c):
        slot(c, *saddle, p)
        slot(c, *armor, p)
        sprite_mask(c, 'container/slot/saddle.png', 8, 18, p[4])
        sprite_mask(c, 'container/slot/horse_armor.png', 8, 36, p[4])
        inset(c, 26, 18, 52, 52, p)
        donkey(c, 30, 24, p)
        for (x, y) in chest:
            slot(c, x, y, p)
        item(c, 'hay_block', *chest[0], 12)
        item(c, 'golden_carrot', *chest[1], 6)
    return render(sc, top, pad_l=30, pre=lambda c: horseshoe_panel(c, P['horseshoe']))


def donkey(c, x, y, p):
    """Placeholder silhouette for the live entity preview (the game draws the animal here)."""
    col = scale(p[3], 0.85)
    for (x0, y0, x1, y1) in ((10, 14, 34, 28), (30, 6, 38, 18), (33, 0, 35, 7), (36, 0, 38, 7), (34, 16, 42, 20),
                             (12, 28, 15, 42), (18, 28, 21, 42), (26, 28, 29, 42), (31, 28, 34, 42), (8, 15, 10, 22)):
        c.fill(x + x0, y + y0, x + x1, y + y1, col)


def horseshoe_panel(c, p):
    """Hoof panel (simpleriding HorseshoePanel X=-24 Y=12, slots X=-20 Y=18+18i): a tab under the horse box's left
    frame - its right edge hides under the box, so slot and frame fit into the 20 px."""
    x, y, w, h = -28, 12, 33, 82
    box(c, x, y, w, h, p)
    for i in range(4):
        slot(c, -20, 18 + i * 18, p)
    item(c, 'iron_ingot', -20, 18)
    for i in range(1, 4):
        symbol(c, HORSESHOE, -17, 21 + i * 18, p, color=p[4], light=False)


HORSESHOE = bitmap(['#.......#', '#.......#', '#.......#', '#.......#', '.#.....#.', '.##...##.', '..#####..', '...###...'][::-1])


@screen('g2', 'werkbank', 'Werkbank', vanilla=('crafting_table',))
def s_crafting():
    p = P['crafting']
    sc = Screen('werkbank', 'Werkbank', 176, 166, p)
    slots = grid(sc, 30, 17, 3, 3)
    res = sc.b(124, 35)

    def top(c):
        for (x, y) in slots:
            slot(c, x, y, p)
        for i in (0, 1, 2, 4, 7):
            item(c, 'oak_planks' if i < 3 else 'stick', *slots[i])
        symbol_progress(c, ARROW, 90, 36, p, 0.0)
        big_slot(c, *res, p)
        item(c, 'wooden_pickaxe', *res)
    return render(sc, top)


def furnace_screen(key, title, pal, fill, waves, items, burn=0.65, cook=0.55):
    sc = Screen(key, title, 176, 166, pal)
    inp, fuel, res = sc.s(56, 17), sc.s(56, 53), sc.b(116, 35)
    sc.add(57, 36, 71, 50)

    def top(c):
        slot(c, *inp, pal)
        fuel_slot(c, *fuel, pal, int(round(16 * burn)), fill, 1234)
        symbol(c, waves, 57, 36, pal, color=mix(pal[3], fill[1], 0.55))   # heat over the fire, warm while lit
        symbol_progress(c, ARROW, 80, 35, pal, cook)
        big_slot(c, *res, pal)
        item(c, items[0], *inp, items[1])
        if items[2]:
            item(c, items[2], *fuel, items[3])
        item(c, items[4], *res, items[5])
    return sc, top


@screen('g2', 'ofen', 'Ofen', vanilla=('furnace',))
def s_furnace():
    return render(*furnace_screen('ofen', 'Ofen', P['furnace'], FILL_COOK, HEAT, ('raw_iron', 12, None, 0, 'iron_ingot', 4)))


@screen('g2', 'schmelzofen', 'Schmelzofen', vanilla=('blast_furnace',))
def s_blast():
    return render(*furnace_screen('schmelz', 'Schmelzofen', P['blast_furnace'], FILL_BLAST, HEAT,
                                  ('raw_gold', 20, None, 0, 'gold_ingot', 7), burn=0.9, cook=0.3))


@screen('g2', 'raeucherofen', 'Raeucherofen', vanilla=('smoker',))
def s_smoker():
    return render(*furnace_screen('raeucher', 'Räucherofen', P['smoker'], FILL_COOK, SMOKE,
                                  ('beef', 8, 'oak_log', 14, 'cooked_beef', 3), burn=0.4, cook=0.8))


@screen('g2', 'braustand', 'Braustand', vanilla=('brewing_stand',))
def s_brewing():
    p = P['brewing']
    sc = Screen('brau', 'Braustand', 176, 166, p)
    fuel, ing = sc.s(17, 17), sc.s(79, 17)
    bottles = [sc.s(56, 51), sc.s(79, 58), sc.s(102, 51)]
    sc.add(60, 14, 76, 44)
    sc.add(97, 16, 106, 44)

    def top(c):
        # pipes (image 3, brewing tile): fuel -> heater, ingredient -> the three bottles
        pipe = scale(p[3], 1.0)
        for (x0, y0, x1, y1) in ((34, 24, 50, 27), (47, 24, 50, 46), (47, 43, 64, 46),      # fuel line to the bubbles
                                 (86, 35, 89, 58), (63, 46, 113, 49), (63, 46, 66, 51), (110, 46, 113, 51)):
            c.fill(x0, y0 + 1, x1, y1 + 1, p[1])
        for (x0, y0, x1, y1) in ((34, 24, 50, 27), (47, 24, 50, 46), (47, 43, 64, 46),
                                 (86, 35, 89, 58), (63, 46, 113, 49), (63, 46, 66, 51), (110, 46, 113, 51)):
            c.fill(x0, y0, x1, y1, pipe)
        slot(c, *fuel, p)
        fuel_slot(c, *fuel, p, 11, FILL_BLAZE, 0)          # blaze powder left: the slot fills like a fuel slot
        slot(c, *ing, p)
        item(c, 'nether_wart', *ing, 5)
        for (x, y) in bottles:
            slot(c, x, y, p)
            sprite_mask(c, 'container/slot/potion.png', x, y, p[4])
        item(c, 'potion', *bottles[0])
        item(c, 'potion', *bottles[2])
        # bubbles rising over the heater (vanilla bubbles spot 63,14 12x29)
        for (bx, by, r) in ((66, 37, 2), (71, 31, 1), (64, 27, 1), (69, 22, 2), (73, 16, 1), (66, 15, 1)):
            bubble(c, bx, by, r, p)
        symbol_progress(c, ARROW_DOWN, 97, 17, p, 0.45, vertical=True)
    return render(sc, top)


def bubble(c, x, y, r, p):
    ring = p[1]
    if r == 1:
        c.fill(x, y - 1, x + 1, y + 2, ring)
        c.fill(x - 1, y, x + 2, y + 1, ring)
        c.px(x, y, p[3])
    else:
        c.fill(x - 1, y - 2, x + 2, y - 1, ring)
        c.fill(x - 1, y + 2, x + 2, y + 3, ring)
        c.fill(x - 2, y - 1, x - 1, y + 2, ring)
        c.fill(x + 2, y - 1, x + 3, y + 2, ring)
        c.fill(x - 1, y - 1, x + 2, y + 2, p[3])
        c.px(x - 1, y - 1, 0xFFFFFFFF)


@screen('g2', 'leuchtfeuer', 'Leuchtfeuer', vanilla=('beacon',))
def s_beacon():
    p = P['beacon']
    sc = Screen('leuchtfeuer', '', 230, 219, p, inv=(36, 137))
    pay = sc.s(136, 110)
    sc.add(164, 107, 186, 129)
    sc.add(190, 107, 212, 129)

    def top(c):
        # two sunk fields: primary (left, 3 tiers) / secondary (right); raised effect buttons, selected = pressed
        inset(c, 18, 8, 110, 92, p)
        inset(c, 140, 8, 72, 92, p)
        symbol(c, PYRAMID, 64, 12, p)
        symbol(c, STAR, 171, 12, p)
        effects = [['speed', 'haste'], ['resistance', 'jump_boost'], ['strength']]
        for tier, row in enumerate(effects):
            n = len(row)
            l = n * 22 + (n - 1) * 2
            for j, e in enumerate(row):
                bx = 76 + j * 24 - l // 2
                by = 22 + tier * 25
                effect_button(c, bx, by, e, p, selected=(e == 'haste'))
        effect_button(c, 155, 47, 'regeneration', p, selected=True)
        effect_button(c, 181, 47, 'haste', p, selected=False, label='II')
        slot(c, *pay, p)
        item(c, 'iron_ingot', *pay)
        for k, n in enumerate(('netherite_ingot', 'emerald', 'diamond', 'gold_ingot', 'iron_ingot')):
            item(c, n, 20 + k * 22, 109)
        raised(c, 164, 107, 22, 22, p, color=mix(p[0], 0xFFFFFFFF, 0.15))
        symbol(c, CHECK, 171, 115, p, color=0xFF2F8F2F, light=False)
        raised(c, 190, 107, 22, 22, p, color=mix(p[0], 0xFFFFFFFF, 0.15))
        symbol(c, CROSS, 197, 114, p, color=0xFFB3322A, light=False)
    return render(sc, top, inv_items=True)


PYRAMID = bitmap(['....#....', '...###...', '..#####..', '.#######.', '#########'])
STAR = bitmap(['...#...', '..###..', '#######', '.#####.', '.##.##.', '#.....#'])


def effect_button(c, x, y, effect, p, selected=False, label=None):
    if selected:
        slot_rect(c, x, y, 22, 22, p)
    else:
        raised(c, x, y, 22, 22, p, color=mix(p[0], 0xFFFFFFFF, 0.15))
    c.rgba(tex(f'mob_effect/{effect}.png').resize((18, 18), Image.NEAREST), x + 2, y + 2)
    if label:
        text(c, label, x + 12, y + 13, 0xFFFFFFFF)


@screen('g2', 'zaubertisch', 'Zaubertisch', vanilla=('enchanting_table',))
def s_enchant():
    p = P['enchanting']
    sc = Screen('zauber', 'Verzaubern', 176, 166, p)
    it, lapis = sc.s(15, 47), sc.s(35, 47)
    for i in range(3):
        sc.add(60, 14 + 19 * i, 169, 14 + 19 * i + 20)

    def top(c):
        symbol(c, BOOK, 17, 18, p)
        slot(c, *it, p)
        slot(c, *lapis, p)
        sprite_mask(c, 'container/slot/lapis_lazuli.png', 35, 47, p[4])
        item(c, 'diamond_pickaxe', *it)
        item(c, 'lapis_lazuli', *lapis, 9)
        sga = Image.open(TEX / 'font/ascii_sga.png') if (TEX / 'font/ascii_sga.png').exists() else None
        for i in range(3):
            x, y = 60, 14 + 19 * i
            inset(c, x, y, 108, 19, p)
            for k in range(i + 1):                      # lapis cost as gems
                gem(c, x + 3 + k * 5, y + 6)
            runes(c, x + 20, y + 3, i)
            num = str((5, 17, 30)[i])
            text(c, num, x + 106 - text_width(num), y + 9, 0xFF80FF20)
    return render(sc, top)


def gem(c, x, y):
    for (dx, dy, col) in ((1, 0, 0xFF6F9BFF), (0, 1, 0xFF2D5FD0), (1, 1, 0xFF4A7BE8), (2, 1, 0xFF2D5FD0), (1, 2, 0xFF1E438C),
                          (0, 2, 0xFF1E438C), (2, 2, 0xFF1E438C), (1, 3, 0xFF14306A)):
        c.px(x + dx, y + dy, col)


def runes(c, x, y, seed):
    """Enchanting-table script (own glyphs, 5x6 each)."""
    col = 0xFFE8D8B0 if seed < 2 else 0xFFB8A888
    for k in range(9 - seed * 2):
        h = hash_(k, seed + 3)
        for gy in range(6):
            for gx in range(4):
                if (h >> (gy * 4 + gx)) & 1 and (gx in (0, 3) or gy in (0, 3, 5)):
                    c.px(x + k * 6 + gx, y + gy + (k % 2) * 7, col)


@screen('g3', 'amboss', 'Amboss', vanilla=('anvil',))
def s_anvil():
    p = P['anvil']
    sc = Screen('amboss', 'Reparieren & Benennen', 176, 166, p)
    a, b, r = sc.s(27, 47), sc.s(76, 47), sc.b(134, 47)
    sc.add(59, 20, 162, 37)
    sc.add(100, 38, 116, 47)

    def top(c):
        inset(c, 59, 20, 103, 16, p)
        text(c, 'Gute Spitzhacke', 63, 24, 0xFFE0E0E0)
        c.fill(63 + text_width('Gute Spitzhacke') + 1, 24, 63 + text_width('Gute Spitzhacke') + 2, 32, 0xFFE0E0E0)
        symbol(c, ANVIL_HAMMER, 47, 22, p)
        slot(c, *a, p)
        slot(c, *b, p)
        symbol(c, PLUS, 54, 50, p)
        symbol_progress(c, ARROW, 98, 48, p, 0.0)
        big_slot(c, *r, p)
        item(c, 'iron_pickaxe', *a)
        item(c, 'enchanted_book', *b)
        item(c, 'iron_pickaxe', *r)
        symbol(c, XP, 100, 39, p, color=0xFF80FF20, light=False)
        text(c, '5', 109, 39, 0xFF80FF20)
    return render(sc, top)


@screen('g3', 'schleifstein', 'Schleifstein', vanilla=('grindstone',))
def s_grind():
    p = P['grindstone']
    sc = Screen('schleif', 'Schleifstein', 176, 166, p)
    a, b, r = sc.s(49, 19), sc.s(49, 40), sc.b(129, 34)

    def top(c):
        slot(c, *a, p)
        slot(c, *b, p)
        # bracket from both inputs into the wheel, wheel, arrow, result
        for (x0, y0, x1, y1) in ((67, 26, 74, 28), (67, 47, 74, 49), (72, 26, 74, 49), (74, 37, 78, 39)):
            c.fill(x0, y0 + 1, x1, y1 + 1, p[1])
        for (x0, y0, x1, y1) in ((67, 26, 74, 28), (67, 47, 74, 49), (72, 26, 74, 49), (74, 37, 78, 39)):
            c.fill(x0, y0, x1, y1, p[3])
        symbol(c, WHEEL, 79, 28, p)
        symbol_progress(c, ARROW, 101, 35, p, 0.0)
        big_slot(c, *r, p)
        item(c, 'enchanted_book', *a)
        item(c, 'book', *r)
        symbol(c, XP, 116, 60, p, color=0xFF80FF20, light=False)
    return render(sc, top)


@screen('g3', 'steinsaege', 'Steinsaege', vanilla=('stonecutter',))
def s_stonecutter():
    p = P['stonecutter']
    sc = Screen('saege', 'Steinsäge', 176, 166, p)
    inp, res = sc.s(20, 33), sc.b(143, 33)
    sc.add(51, 14, 117, 70)
    sc.add(119, 15, 131, 70)

    def top(c):
        slot(c, *inp, p)
        item(c, 'stone', *inp, 32)
        inset(c, 51, 14, 66, 56, p)
        names = ['smooth_stone', 'cobblestone', 'stone_bricks', 'mossy_stone_bricks', 'cracked_stone_bricks',
                 'chiseled_stone_bricks', 'polished_andesite', 'stone', 'andesite']
        for k, n in enumerate(names):
            x, y = 52 + (k % 4) * 16, 15 + (k // 4) * 18
            if k == 2:
                slot_rect(c, x, y, 16, 18, p)
            else:
                raised(c, x, y, 16, 18, p, color=mix(p[0], 0xFFFFFFFF, 0.18))
            try:
                item(c, n, x, y + 1)
            except FileNotFoundError:
                pass
        inset(c, 119, 15, 12, 54, p)
        raised(c, 119, 15, 12, 15, p, color=mix(p[0], 0xFFFFFFFF, 0.3))
        big_slot(c, *res, p)
        item(c, 'stone_bricks', *res)
    return render(sc, top)


@screen('g3', 'webstuhl', 'Webstuhl', vanilla=('loom',))
def s_loom():
    p = P['loom']
    sc = Screen('web', 'Webstuhl', 176, 166, p)
    ban, dye, pat, res = sc.s(13, 26), sc.s(33, 26), sc.s(23, 45), sc.b(143, 57)
    sc.add(59, 13, 117, 71)
    sc.add(119, 13, 131, 70)

    def top(c):
        for s_, spr in ((ban, 'banner'), (dye, 'dye'), (pat, 'banner_pattern')):
            slot(c, *s_, p)
            sprite_mask(c, f'container/slot/{spr}.png', *s_, p[4])
        banner(c, *ban, None)
        item(c, 'red_dye', *dye, 4)
        inset(c, 59, 13, 58, 58, p)
        for k in range(16):
            x, y = 60 + (k % 4) * 14, 14 + (k // 4) * 14
            if k == 5:
                slot_rect(c, x, y, 14, 14, p)
            else:
                raised(c, x, y, 14, 14, p, color=mix(p[0], 0xFFFFFFFF, 0.35))
            pattern_icon(c, x + 3, y + 2, k)
        inset(c, 119, 13, 12, 56, p)
        raised(c, 119, 13, 12, 15, p, color=mix(p[0], 0xFFFFFFFF, 0.3))
        big_slot(c, *res, p)
        banner(c, *res, 5)
    return render(sc, top)


def banner(c, x, y, k):
    """Banner item stand-in (vanilla draws banners from the model, there is no item texture)."""
    c.fill(x + 2, y + 1, x + 14, y + 2, 0xFF6B4A2B)
    c.fill(x + 7, y, x + 9, y + 16, 0xFF6B4A2B)
    c.fill(x + 3, y + 2, x + 13, y + 14, 0xFFE9E4DA)
    c.fill(x + 3, y + 13, x + 13, y + 14, 0xFFC9C2B6)
    if k is not None:
        for yy in range(12):
            for xx in range(10):
                if abs(xx - 4.5) + abs(yy - 6) < 4:
                    c.px(x + 3 + xx, y + 2 + yy, 0xFFB02E26)


def pattern_icon(c, x, y, k):
    """Tiny banner previews 8x10 (white cloth, red pattern)."""
    c.fill(x, y, x + 8, y + 10, 0xFFE9E4DA)
    h = hash_(k, 77)
    for yy in range(10):
        for xx in range(8):
            if ((xx + (h & 3)) % 4 < 2 and (h >> 2) & 1) or ((yy + (h >> 4 & 3)) % 5 < 2 and (h >> 3) & 1) or (k % 3 == 0 and abs(xx - 4) + abs(yy - 5) < 3):
                c.px(x + xx, y + yy, 0xFFB02E26)


@screen('g3', 'kartentisch', 'Kartografietisch', vanilla=('cartography_table',))
def s_carto():
    p = P['cartography']
    sc = Screen('karte', 'Kartentisch', 176, 166, p)
    mp, paper, res = sc.s(15, 15), sc.s(15, 52), sc.b(145, 39)
    sc.add(67, 13, 134, 77)

    def top(c):
        slot(c, *mp, p)
        slot(c, *paper, p)
        sprite_mask(c, 'container/slot/dye.png', 15, 52, p[4])
        item(c, 'filled_map', *mp)
        item(c, 'paper', *paper, 8)
        symbol(c, PLUS, 18, 36, p)
        inset(c, 67, 13, 66, 63, p)
        fake_map(c, 69, 15, 62, 59)
        symbol_progress(c, ARROW_SMALL, 41, 37, p, 0.0)
        big_slot(c, *res, p)
        item(c, 'filled_map', *res, 2)
    return render(sc, top)


def fake_map(c, x, y, w, h):
    c.fill(x, y, x + w, y + h, 0xFFD9C79A)
    for yy in range(h):
        for xx in range(w):
            v = math.sin(xx * 0.21) + math.cos(yy * 0.17 + xx * 0.05) + math.sin((xx + yy) * 0.09)
            if v > 1.1:
                c.px(x + xx, y + yy, 0xFF5C9C4A)
            elif v < -1.2:
                c.px(x + xx, y + yy, 0xFF4E78C8)
    c.fill(x + w // 2, y + h // 2, x + w // 2 + 2, y + h // 2 + 2, 0xFFFFFFFF)


@screen('g3', 'schmiedetisch', 'Schmiedetisch', vanilla=('smithing',))
def s_smithing():
    p = P['smithing']
    sc = Screen('schmied', 'Schmiedetisch', 176, 166, p)
    t, b, a, r = sc.s(8, 48), sc.s(26, 48), sc.s(44, 48), sc.b(98, 48)
    sc.add(120, 8, 169, 77)

    def top(c):
        for s_, spr in ((t, 'smithing_template_netherite_upgrade'), (b, 'chestplate'), (a, 'ingot')):
            slot(c, *s_, p)
            sprite_mask(c, f'container/slot/{spr}.png', *s_, p[4])
        item(c, 'netherite_upgrade_smithing_template', *t)
        item(c, 'diamond_chestplate', *b)
        item(c, 'netherite_ingot', *a)
        symbol_progress(c, ARROW, 68, 49, p, 0.0)
        big_slot(c, *r, p)
        item(c, 'netherite_chestplate', *r)
        inset(c, 121, 8, 48, 68, p)
        stand(c, 133, 14, p)
    return render(sc, top)


def stand(c, x, y, p):
    """Armour-stand preview placeholder (the game renders the 3D stand here)."""
    col = scale(p[3], 1.5)
    for (x0, y0, x1, y1) in ((10, 0, 14, 5), (5, 6, 19, 8), (11, 8, 13, 30), (6, 18, 18, 20), (8, 30, 10, 52), (14, 30, 16, 52),
                             (3, 52, 21, 55)):
        c.fill(x + x0, y + y0, x + x1, y + y1, col)


@screen('g3', 'handel', 'Handel (Dorfbewohner)', vanilla=('villager',))
def s_merchant():
    p = P['merchant']
    sc = Screen('handel', '', 276, 166, p, inv=(108, 84))
    sc.add(5, 18, 100, 160)
    a, b = sc.s(136, 37), sc.s(162, 37)
    r = sc.b(220, 37)
    # list column and the right part are one box with a vertical seam (scroller at 94..100 leaves no room for two
    # frames before the inventory at 108) -> 'one' box: the lower right is the light inventory panel
    sc.rects.append((103, 0, 276, 80))

    def top(c):
        c.fill(101, 5, 102, 159, scale(p[0], 0.64))
        c.fill(102, 5, 103, 159, p[1])
        offers = [('emerald', 'wheat', 20, None, 'emerald', 1), ('emerald', 'iron_ingot', 4, None, 'emerald', 1),
                  ('emerald', 'emerald', 3, 'book', 'enchanted_book', 1), ('emerald', 'emerald', 1, None, 'bread', 6),
                  ('emerald', 'emerald', 15, None, 'bell', 1)]
        for k, (_, cost, n, cost2, out, m) in enumerate(offers):
            x, y = 5, 18 + k * 20
            if k == 1:
                slot_rect(c, x, y, 88, 20, p)
            else:
                raised(c, x, y, 88, 20, p, color=mix(p[0], 0xFFFFFFFF, 0.12))
            item(c, cost, x + 4, y + 2, n)
            if cost2:
                item(c, cost2, x + 30, y + 2)
            symbol(c, TRADE_ARROW, x + 53, y + 6, p)
            item(c, out, x + 66, y + 2, m if m > 1 else None)
        inset(c, 94, 18, 6, 140, p)
        raised(c, 94, 18, 6, 27, p, color=mix(p[0], 0xFFFFFFFF, 0.3))
        text(c, 'Waffenschmied', 107, 6, p[5])
        inset(c, 136, 16, 102, 5, p)
        c.fill(137, 17, 137 + 60, 20, 0xFF80FF20)
        slot(c, *a, p)
        slot(c, *b, p)
        item(c, 'emerald', *a, 4)
        symbol_progress(c, ARROW, 186, 38, p, 0.0)
        big_slot(c, *r, p)
        item(c, 'iron_ingot', *r)
    return render(sc, top, panel_x=103)


TRADE_ARROW = bitmap(['....#...', '....##..', '#######.', '########', '#######.', '....##..', '....#...'])


@screen('g3', 'spieler-inventar', 'Spieler-Inventar', vanilla=('inventory',))
def s_player():
    p = P['player']
    sc = Screen('spieler', '', 176, 166, p)
    armor = [sc.s(8, 8 + i * 18) for i in range(4)]
    off = sc.s(77, 62)
    craft = grid(sc, 98, 18, 2, 2)
    res = sc.s(154, 28)
    sc.add(26, 8, 76, 78)

    def top(c):
        for s_, spr in zip(armor, ('helmet', 'chestplate', 'leggings', 'boots')):
            slot(c, *s_, p)
            sprite_mask(c, f'container/slot/{spr}.png', *s_, p[4])
        item(c, 'iron_helmet', *armor[0])
        item(c, 'iron_chestplate', *armor[1])
        slot(c, *off, p)
        sprite_mask(c, 'container/slot/shield.png', *off, p[4])
        inset(c, 26, 8, 49, 70, p)
        player_fig(c, 38, 14, p)
        text(c, 'Herstellen', 97, 6, p[5])
        for s_ in craft:
            slot(c, *s_, p)
        item(c, 'oak_log', *craft[0])
        symbol_progress(c, ARROW_SMALL, 135, 29, p, 0.0)
        slot(c, *res, p)
        item(c, 'oak_planks', *res, 4)
    return render(sc, top)


def player_fig(c, x, y, p):
    col = scale(p[3], 0.8)
    for (x0, y0, x1, y1) in ((8, 0, 16, 8), (6, 9, 18, 30), (1, 9, 5, 28), (19, 9, 23, 28), (7, 31, 11, 56), (13, 31, 17, 56)):
        c.fill(x + x0, y + y0, x + x1, y + y1, col)


# ------------------------------------------------------------------ G4 mod screens
def tiered(key, title, pal, cols, bonus, items):
    w = max(176, 14 + 18 * cols)
    sc, top = chest_like(key, title, pal, 6, cols=cols, w=w, items=items)

    def top2(c):
        top(c)
        if bonus:
            s = f'x{bonus}'
            x = w - 8 - text_width(s)
            text(c, s, x, 6, pal[5])
            symbol(c, STACK, x - 10, 6, pal, color=pal[5], light=False)
    return render(sc, top2)


@screen('g4', 'mod-truhe-verstaerkt', 'Verstaerkte Doppeltruhe (12 Spalten)')
def s_mod_reinf():
    return tiered('mt1', 'Verstärkte große Truhe', P['chest_reinforced'], 12, None, [(0, 'iron_ingot', 64), (13, 'copper_ingot', 30)])


@screen('g4', 'mod-truhe-netherit', 'Netherit-Doppeltruhe (15 Spalten, x2 Stapel)')
def s_mod_neth():
    return tiered('mt2', 'Netherit-Truhe', P['chest_netherite'], 15, 2, [(0, 'netherrack', 128), (16, 'quartz', 90), (40, 'magma_cream', 3)])


@screen('g4', 'mod-truhe-enderit', 'Enderit-Doppeltruhe (18 Spalten, x4 Stapel)')
def s_mod_end():
    return tiered('mt3', 'Enderit-Truhe', P['chest_enderite'], 18, 4, [(0, 'end_stone', 256), (19, 'chorus_fruit', 77), (50, 'ender_pearl', 60)])


def backpack(key, title, pal, rows, extra, items):
    vx = 18 if extra >= 2 else 0
    w, h = 176 + 18 * extra, 166 + 18 * rows
    sc = Screen(key, '', w, h, pal, inv=(vx + 8, 84 + 18 * rows))
    armor = [sc.s(vx + 8, 8 + i * 18) for i in range(4)]
    off = sc.s(vx + 77, 62)
    craft = grid(sc, vx + 98, 18, 2, 2)
    res = sc.s(vx + 154, 28)
    sc.add(vx + 26, 8, vx + 76, 78)
    bp_rows = [(vx + 8 + cc * 18, 84 + r * 18) for r in range(rows) for cc in range(9)]
    columns = []
    for col in range(extra):
        x = vx + 170 if col == 0 else vx - 10
        columns += [(x, 84 + 18 * i) for i in range(rows + 3)]

    def top(c):
        for s_, spr in zip(armor, ('helmet', 'chestplate', 'leggings', 'boots')):
            slot(c, *s_, pal)
            sprite_mask(c, f'container/slot/{spr}.png', *s_, pal[4])
        slot(c, *off, pal)
        sprite_mask(c, 'container/slot/shield.png', *off, pal[4])
        inset(c, vx + 26, 8, 49, 70, pal)
        player_fig(c, vx + 38, 14, pal)
        for s_ in craft:
            slot(c, *s_, pal)
        symbol_progress(c, ARROW_SMALL, vx + 135, 29, pal, 0.0)
        slot(c, *res, pal)
        # backpack band: rows + extra columns in the backpack's colours on a tinted strip of the light panel
        tint = mix(INVENTORY[0], pal[0], 0.28)
        c.fill(vx + 6, 82, vx + 170, 84 + rows * 18, tint)
        for (x, y) in columns:
            c.fill(x - 2, y - 2 if y == 84 else y, x + 18, y + 18, tint)
        for (x, y) in bp_rows + columns:
            slot(c, x, y, pal)
        for (i, n, k) in items:
            item(c, n, *(bp_rows + columns)[i], k)
    # free rows between the armour/crafting part and the backpack rows = 5 -> 'one' box with a seam (see split())
    c = Canvas(w + 2, h + 4)
    c.ox, c.oy = 1, 1
    box(c, 0, 0, w, h, pal)
    motif(c, FRAME, FRAME, w - 2 * FRAME, 81 - FRAME, pal, sc.rects, seed=7)
    seam_panel(c, FRAME, 81, w - 2 * FRAME, h - FRAME_BOTTOM - 81, pal)
    top(c)
    iy = 84 + 18 * rows
    for r in range(3):
        for col in range(9):
            slot(c, vx + 8 + col * 18, iy + r * 18, INVENTORY)
    for col in range(9):
        slot(c, vx + 8 + col * 18, iy + 58, INVENTORY)
    item(c, 'cobblestone', vx + 8, iy, 64)
    item(c, 'iron_pickaxe', vx + 8, iy + 58)
    return c


@screen('g4', 'rucksack-basis', 'Rucksack (Basis, 1 Reihe)')
def s_bp1():
    return backpack('bp1', 'Rucksack', P['backpack'], 1, 0, [(0, 'torch', 48), (1, 'bread', 9)])


@screen('g4', 'rucksack-enderit', 'Enderit-Rucksack (4 Reihen + 2 Spalten)')
def s_bp4():
    return backpack('bp4', 'Rucksack', P['backpack_enderite'], 4, 2, [(0, 'ender_pearl', 16), (10, 'torch', 64), (36, 'diamond', 5),
                                                                     (44, 'gold_ingot', 20)])


@screen('g4', 'auto-schmied', 'Auto-Schmied')
def s_auto():
    p = P['auto_smither']
    sc = Screen('auto', 'Auto-Schmied', 176, 166, p)
    t, b, a, r = sc.s(26, 35), sc.s(44, 35), sc.s(62, 35), sc.b(134, 35)
    sc.add(84, 22, 96, 34)

    def top(c):
        for s_, spr in ((t, 'smithing_template_netherite_upgrade'), (b, 'chestplate'), (a, 'ingot')):
            slot(c, *s_, p)
            sprite_mask(c, f'container/slot/{spr}.png', *s_, p[4])
        item(c, 'netherite_upgrade_smithing_template', *t, 3)
        item(c, 'diamond_sword', *b)
        item(c, 'netherite_ingot', *a, 3)
        symbol(c, REDSTONE, 98, 22, p, color=0xFFD8261E)
        symbol_progress(c, ARROW, 99, 36, p, 0.4)
        big_slot(c, *r, p)
        item(c, 'netherite_sword', *r)
    return render(sc, top)


@screen('g4', 'befiederungstisch', 'Befiederungstisch')
def s_fletch():
    p = P['fletching']
    sc = Screen('fletch', 'Befiederungstisch', 176, 166, p)
    tip, shaft, feather, r = sc.s(66, 17), sc.s(48, 35), sc.s(30, 53), sc.b(124, 35)

    def top(c):
        # the three part slots sit on a diagonal: an engraved arrow shaft runs through them
        for k in range(-6, 50):
            x, y = 36 + k, 59 - k
            if not overlaps((x, y, x + 1, y + 1), [(66, 17, 83, 34), (48, 35, 65, 52), (30, 53, 47, 70)], 0):
                c.px(x, y + 1, p[1])
                c.px(x, y, p[3])
                c.px(x + 1, y, p[3])
        for s_ in (tip, shaft, feather):
            slot(c, *s_, p)
        item(c, 'flint', *tip, 8)
        item(c, 'stick', *shaft, 8)
        item(c, 'feather', *feather, 8)
        symbol_progress(c, ARROW, 92, 36, p, 0.0)
        big_slot(c, *r, p)
        item(c, 'arrow', *r, 8)
    return render(sc, top)


@screen('g4', 'netherit-trichter', 'Netherit-Trichter mit Filter (Modi: aus / genau / Sorte)')
def s_nhopper():
    p = P['hopper_netherite']
    sc = Screen('nhopper', 'Netherit-Trichter', 176, 133, p, inv=(8, 51))
    slots = [sc.s(44 + i * 18, 20) for i in range(5)]
    sc.add(138, 19, 157, 38)

    def top(c):
        for (x, y) in slots:
            slot(c, x, y, p)
        ghost(c, 'iron_ingot', 62, 20, p)
        ghost(c, 'gold_ingot', 80, 20, p)
        item(c, 'iron_ingot', 44, 20, 12)
        # gap of 4 px, then the filter button as a raised slot-sized key with an engraved funnel + mode badge
        filter_button(c, 138, 19, p, 'exact')
    c = Canvas(176 + 92, 137)
    c.ox, c.oy = 1, 1
    draw_screen(c, sc, top)
    # legend of the three modes (not part of the screen): off / exact item / same kind
    for k, (mode, lab) in enumerate((('none', 'aus'), ('exact', 'genau'), ('type', 'Sorte'))):
        filter_button(c, 186, 6 + k * 24, p, mode)
        text(c, lab, 208, 11 + k * 24, 0xFFE2E6EC)
    return c


def filter_button(c, x, y, p, mode):
    raised(c, x, y, 18, 18, p, color=mix(p[0], 0xFFFFFFFF, 0.18))
    symbol(c, FUNNEL, x + 3, y + 3, p, color=p[4], light=False)
    if mode == 'none':
        for k in range(14):
            c.px(x + 2 + k, y + 15 - k, 0xFFD8402F)
            c.px(x + 3 + k, y + 15 - k, 0xFFD8402F)
    elif mode == 'exact':
        symbol(c, CHECK, x + 10, y + 11, p, color=0xFF55FF55, light=False)
    else:   # type: three small squares = 'same kind'
        for k in range(3):
            c.fill(x + 9 + k * 3, y + 13, x + 11 + k * 3, y + 15, 0xFFFFE055)


@screen('g4', 'tiegel-referenz', 'Schmelztiegel (Eisen) - unveraendert, Referenz')
def s_crucible():
    p = IRON
    # CrucibleMenu.layout(IRON, barrel=false): grids 1, rows 2; panel 176 x (18+36+12) + 2 + 100
    section = 18 + 2 * 18 + 12
    sc = Screen('tiegel', 'Eisen Schmelztiegel', 176, section + 2 + 100, p, inv=(8, section + 2 + 17))
    grid_left = (176 - 54) // 2
    slots = [(grid_left + 1 + cc * 18, 18 + 1 + r * 18) for r in range(2) for cc in range(3)]
    c = Canvas(178, sc.h + 4)
    c.ox, c.oy = 1, 1
    box(c, 0, 0, 176, section, p)
    box(c, 0, section + 2, 176, 100, INVENTORY)
    flames(c, FRAME, section - FRAME_BOTTOM, 176 - 2 * FRAME, section)
    for (x, y) in slots:
        slot(c, x, y, p)
    furnace_fill(c, *slots[0], 9, FILL_COOK, 0)
    item(c, 'raw_iron', *slots[0], 3)
    item(c, 'iron_ingot', *slots[1], 2)
    text(c, 'Eisen Schmelztiegel', 8, 6, p[5])
    text(c, 'Inventar', 8, section + 2 + 6, INVENTORY[5])
    for r in range(3):
        for col in range(9):
            slot(c, 8 + col * 18, sc.inv[1] + r * 18, INVENTORY)
    for col in range(9):
        slot(c, 8 + col * 18, sc.inv[1] + 58, INVENTORY)
    return c


# ------------------------------------------------------------------ vanilla 'before' pictures
def vanilla(kind):
    k = kind[0]
    base = TEX / 'gui/container'
    if k == 'generic':
        rows = kind[1]
        img = Image.open(base / 'generic_54.png').convert('RGBA')
        out = Image.new('RGBA', (176, 114 + rows * 18))
        out.paste(img.crop((0, 0, 176, 17 + rows * 18)), (0, 0))
        out.paste(img.crop((0, 126, 176, 222)), (0, 17 + rows * 18))
        return out
    name = {'shulker': 'shulker_box'}.get(k, k)
    sizes = {'hopper': (176, 133), 'beacon': (230, 219), 'villager': (276, 166)}
    w, h = sizes.get(name, (176, 166))
    img = Image.open(base / f'{name}.png').convert('RGBA')
    out = img.crop((0, 0, w, h))
    if name == 'horse':
        chest = Image.open(TEX / 'gui/sprites/container/horse/chest_slots.png').convert('RGBA')
        out.alpha_composite(chest, (79, 17))
    if name == 'crafter':
        sl = Image.open(TEX / 'gui/sprites/container/slot.png').convert('RGBA')
        for r in range(3):
            for cc in range(3):
                out.alpha_composite(sl.resize((18, 18)), (25 + cc * 18, 16 + r * 18))
    return out


# ------------------------------------------------------------------ sheets
def caption(draw, x, y, s, size_big=True, color=CAPTION):
    draw.text((x, y), s, fill=color, font=LABEL_FONT if size_big else SMALL_FONT)


def scaled(c_or_img):
    img = c_or_img.image() if isinstance(c_or_img, Canvas) else c_or_img
    return img.resize((img.width * K, img.height * K), Image.NEAREST)


def compose(label, new, before=None):
    pad, head = 24, 56
    parts = []
    if before is not None:
        bg = Image.new('RGB', before.size, SHEET_BG)
        bg.paste(before, (0, 0), before)
        parts.append(('Vanilla vorher', scaled(bg)))
    parts.append(('simplecontainers (Vorschau W0-B)', scaled(new)))
    w = sum(p[1].width for p in parts) + pad * (len(parts) + 1)
    h = max(p[1].height for p in parts) + head + 34 + pad
    out = Image.new('RGB', (w, h), SHEET_BG)
    d = ImageDraw.Draw(out)
    caption(d, pad, 14, label)
    x = pad
    for (t, im) in parts:
        caption(d, x, head, t, False, CAPTION_DIM)
        out.paste(im, (x, head + 30))
        x += im.width + pad
    return out


def contact(title, items, cols, cell_scale=1.0):
    pad, head = 24, 60
    ims = []
    for label, im in items:
        if cell_scale != 1.0:
            im = im.resize((int(im.width * cell_scale), int(im.height * cell_scale)), Image.LANCZOS)
        ims.append((label, im))
    rows = [ims[i:i + cols] for i in range(0, len(ims), cols)]
    col_w = [max(r[i][1].width for r in rows if i < len(r)) for i in range(cols)]
    row_h = [max(im.height for _, im in r) + 34 for r in rows]
    w = sum(col_w) + pad * (cols + 1)
    h = head + sum(row_h) + pad * len(rows) + pad
    out = Image.new('RGB', (w, h), SHEET_BG)
    d = ImageDraw.Draw(out)
    caption(d, pad, 16, title)
    y = head
    for r, rh in zip(rows, row_h):
        x = pad
        for i, (label, im) in enumerate(r):
            caption(d, x, y, label, False, CAPTION_DIM)
            out.paste(im, (x, y + 28))
            x += col_w[i] + pad
        y += rh + pad
    return out


GROUPS = {'g1': 'G1 Lager', 'g2': 'G2 Arbeit I (Werkbank, Oefen, Braustand, Leuchtfeuer, Zaubertisch)',
          'g3': 'G3 Arbeit II (Amboss, Schleifstein, Steinsaege, Webstuhl, Kartentisch, Schmiedetisch, Handel, Inventar)',
          'g4': 'G4 Mod-UIs (simplelib-Bausteine)'}


def palette_sheet():
    keys = list(P.keys())
    sw = 34
    c = Canvas(560, 14 + len(keys) * 20)
    for i, k in enumerate(keys):
        p = P[k]
        y = 4 + i * 20
        box(c, 4, y, 60, 18, p, shadow=0)
        for j, col in enumerate(p):
            c.fill(70 + j * (sw + 2), y + 2, 70 + j * (sw + 2) + sw, y + 16, col)
        text(c, f'{k}  {p.name or ""}  [{p.motif}]', 70 + 6 * (sw + 2) + 6, y + 6, 0xFFE2E6EC)
    return c


def main():
    global TEX, FONT, LABEL_FONT, SMALL_FONT
    ap = argparse.ArgumentParser()
    ap.add_argument('--vanilla', default='/root/vanilla263/assets/minecraft/textures')
    ap.add_argument('--out', default='/root/previews/simplecontainers')
    ap.add_argument('--only', default=None)
    a = ap.parse_args()
    TEX = Path(a.vanilla)
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    FONT = Font()
    from PIL import ImageFont
    LABEL_FONT = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 22)
    SMALL_FONT = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 18)
    groups = {}
    for group, key, label, fn, van in SCREENS:
        if a.only and a.only not in key:
            continue
        c = fn()
        before = vanilla(van) if van else None
        compose(label, c, before).save(out / f'{group}-{key}.png')
        groups.setdefault(group, []).append((label, scaled(c)))
        print('ok', group, key)
    if a.only:
        return
    for g, items in groups.items():
        contact(f'simplecontainers W0-B - {GROUPS[g]}', items, 3 if g != 'g4' else 3).save(out / f'kontakt-{g}.png')
    every = [it for g in sorted(groups) for it in groups[g]]
    contact('simplecontainers W0-B - alle UIs (verkleinert 1:2)', every, 6, 0.5).save(out / 'kontakt-alle.png')
    pal = palette_sheet()
    scaled(pal).save(out / 'paletten.png')
    # comparison: image 3 + image 4 next to chest/furnace/crafting
    b3 = Image.open(TEX.parents[2].parent / 'refs' if False else Path('/root/previews/refs-n12/bild3-container-stil.webp')).convert('RGB')
    b4 = Image.open('/root/previews/refs-n12/bild4-ofen-stil.png').convert('RGB')
    b3 = b3.resize((1200, int(b3.height * 1200 / b3.width)), Image.LANCZOS)
    b4 = b4.resize((700, int(b4.height * 700 / b4.width)), Image.LANCZOS)
    mine = [scaled(s_crafting()), scaled(s_chest()), scaled(s_furnace())]
    w = max(b3.width + b4.width + 72, sum(m.width for m in mine) + 96)
    h = 60 + max(b3.height, b4.height) + 60 + max(m.height for m in mine) + 30
    out_img = Image.new('RGB', (w, h), SHEET_BG)
    d = ImageDraw.Draw(out_img)
    caption(d, 24, 16, 'Vergleich: Bild 3 / Bild 4 (Besitzer-Referenz) oben, Vorschau W0-B unten (Werkbank, Truhe, Ofen)')
    out_img.paste(b3, (24, 60))
    out_img.paste(b4, (48 + b3.width, 60))
    x = 24
    yy = 60 + max(b3.height, b4.height) + 40
    for m in mine:
        out_img.paste(m, (x, yy))
        x += m.width + 24
    out_img.save(out / 'vergleich-bild3-bild4.png')
    print('sheets ok')


if __name__ == '__main__':
    main()
