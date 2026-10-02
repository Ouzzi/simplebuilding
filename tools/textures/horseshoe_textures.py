"""Simple Riding R1 - Hufeisen, Schmiedevorlage, Huf-Ebene, Slot-Symbol und Inventarleiste.

Neue Pixelkunst im Vanilla-Stil: Paletten aus Vanillas Barren/Edelsteinen (Kupfer, Eisen, Gold,
Diamant, Netherit) und SimpleBuildings Enderit-Ausruestungsrampe; 1 px Kontur im dunkelsten
Materialton (4er-Nachbarschaft, also keine dunklen Eckpixel in diagonalen Stufen), Licht von links
oben, nie bis an den Rand gemalt. Die historischen Varianten nutzen die Paletten hier im Code;
die aktuelle Vorlage D liest ihre Eisen- und Kupferfarben aus dem Vanilla-26.3-Jar.

Drei Vorschlaege je neuer Textur (Vorschau ``previews/hufeisen-vorschau.png``):
  Hufeisen   A = aufrecht, offen nach oben ("Glueckshufeisen"), Nagelloecher
             B = schmales Renneisen mit Stollen, helle Nagelkoepfe statt Loechern
             C = offen nach unten wie am Huf, mit Zehenkappe
  Vorlage    A = Kupferplatte mit Eisen-Hufeisen-Gravur
             B = oxidierte Kupferplatte mit Kupfer-Gravur
             C = dunkle Steinplatte (wie Rand-Vorlagen) mit Kupfer-Gravur
Ausgeliefert werden ITEM_VARIANT (A) und TEMPLATE_VARIANT (D: Basic-Upgrade-Stil,
Vanilla-Paletten aus dem 26.3-Jar; horseshoe_template_2026_10_02.py).

    python tools/textures/horseshoe_textures.py            # schreiben + Vorschau
    python tools/textures/horseshoe_textures.py --check    # nur pruefen, ob Dateien aktuell sind
    python tools/textures/horseshoe_textures.py --item B --template C
"""
import argparse
import io
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'modules/simpleriding/shared/resources/assets/simpleriding/textures'
# Vorschau liegt ausserhalb des Repos im gemeinsamen Vorschauordner des Besitzers (falls vorhanden).
_OWNER_PREVIEWS = Path.home() / 'code/minecraft-mods/previews'
PREVIEW = (_OWNER_PREVIEWS if _OWNER_PREVIEWS.is_dir() else ROOT / 'build/previews') / 'hufeisen-vorschau.png'
ITEM_VARIANT = 'A'
TEMPLATE_VARIANT = 'D'


def _hex(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# Rampen dunkel -> hell: [Kontur, Schatten, Mitte dunkel, Mitte, Licht, Glanz]
MATERIALS = {
    'copper': ['#6d3421', '#8a4129', '#9c4e31', '#c15a36', '#e77c56', '#fc9982'],
    'iron': ['#353535', '#585858', '#727272', '#a8a8a8', '#d8d8d8', '#ffffff'],
    'golden': ['#752802', '#b26411', '#dc9613', '#e9b115', '#fad64a', '#fdf55f'],
    'diamond': ['#145e53', '#11727a', '#1c919a', '#20c5b5', '#4aedd9', '#a1fbe8'],
    'netherite': ['#111111', '#271c1d', '#31292a', '#3c3232', '#4c4143', '#737173'],
    'enderite': ['#1c0a33', '#2d1656', '#4a2888', '#6d45b8', '#8e63dc', '#a57de9'],
}
TIERS = list(MATERIALS)
ENDER_VEIN = '#f4d2ff'

# ---------------------------------------------------------------------------
# Hufeisen-Masken: '#' Metall, 'n' Nagelloch (Metall in Schattenton), '.' leer.
# Die Kontur entsteht automatisch um alle Metallpixel.
# ---------------------------------------------------------------------------
SHOE_A = [
    '................',
    '................',
    '...###....###...',
    '...###....###...',
    '..####....####..',
    '..#n#......#n#..',
    '..###......###..',
    '..#n#......#n#..',
    '..###......###..',
    '..#n#......#n#..',
    '..####....####..',
    '...####..####...',
    '....#n####n#....',
    '.....######.....',
    '................',
    '................',
]

SHOE_B = [
    '................',
    '................',
    '..###......###..',
    '..###......###..',
    '..#c........c#..',
    '..##........##..',
    '..#c........c#..',
    '..##........##..',
    '..##........##..',
    '..#c#......#c#..',
    '...##......##...',
    '...###....###...',
    '....##c##c##....',
    '.....######.....',
    '................',
    '................',
]

SHOE_C = [
    '................',
    '................',
    '......####......',
    '....########....',
    '...####cc####...',
    '..####....####..',
    '..#n#......#n#..',
    '..###......###..',
    '..#n#......#n#..',
    '..###......###..',
    '..#n#......#n#..',
    '..###......###..',
    '..###......###..',
    '...##......##...',
    '................',
    '................',
]
SHOES = {'A': SHOE_A, 'B': SHOE_B, 'C': SHOE_C}


def _outline(mask):
    """Pixels outside the shape that touch it orthogonally (no diagonal corner fill)."""
    h, w = len(mask), len(mask[0])
    out = set()
    for y in range(h):
        for x in range(w):
            if mask[y][x] != '.':
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and mask[ny][nx] != '.':
                    out.add((x, y))
    return out


def _solid(mask, x, y):
    return 0 <= y < len(mask) and 0 <= x < len(mask[0]) and mask[y][x] != '.'


def shade_mask(mask, ramp, vein=None):
    """Vanilla-like shading: light from the top left, shadow bottom right, outline darkest tone."""
    pal = [_hex(c) for c in ramp]
    img = Image.new('RGBA', (len(mask[0]), len(mask)), (0, 0, 0, 0))
    for (x, y) in _outline(mask):
        img.putpixel((x, y), pal[0])
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            lit = not _solid(mask, x, y - 1) or not _solid(mask, x - 1, y)
            dark = not _solid(mask, x, y + 1) or not _solid(mask, x + 1, y)
            if ch == 'n':
                tone = 1
            elif ch == 'c':
                tone = 5
            elif lit and not dark:
                tone = 5 if (not _solid(mask, x - 1, y) and not _solid(mask, x, y - 1)) else 4
            elif dark and not lit:
                tone = 2
            else:
                tone = 3
            img.putpixel((x, y), pal[tone])
    if vein:
        # Enderit: eine helle Ader entlang der Mitte wie die Enderit-Ausruestung.
        cells = [(x, y) for y, row in enumerate(mask) for x, ch in enumerate(row) if ch == '#'
                 and _solid(mask, x - 1, y) and _solid(mask, x + 1, y) and _solid(mask, x, y - 1) and _solid(mask, x, y + 1)]
        for (x, y) in cells[::3]:
            img.putpixel((x, y), _hex(vein))
    return img


def horseshoe_item(material, variant):
    return shade_mask(SHOES[variant], MATERIALS[material], ENDER_VEIN if material == 'enderite' else None)


# ---------------------------------------------------------------------------
# Schmiedevorlage: Platte 10x13 mit Fase, Gravur (kleines Hufeisen) in Akzentfarbe.
# ---------------------------------------------------------------------------
SLAB = [
    '................',
    '...#########....',
    '..###########...',
    '..###########...',
    '..###########...',
    '..###########...',
    '..###########...',
    '..###########...',
    '..###########...',
    '..###########...',
    '..###########...',
    '..###########...',
    '..###########...',
    '..###########...',
    '...#########....',
    '................',
]
ENGRAVING = [  # relative to the slab interior, 7x8, '#' accent, 'd' engraved shadow
    '#d...#d',
    '#d...#d',
    '#d...#d',
    '#d...#d',
    '##d.##d',
    '.####d.',
    '..ddd..',
]
TEMPLATES = {
    # slab ramp (Kontur, Schatten, dunkel, Mitte, Licht, Glanz), accent ramp
    'A': (MATERIALS['copper'], MATERIALS['iron']),
    'B': (['#2b4a3f', '#356b5a', '#427b6a', '#4fab90', '#6ec59f', '#8fd8b8'], MATERIALS['copper']),
    'C': (['#2a1d17', '#3d2b22', '#4f382c', '#5f4637', '#755a48', '#8b6e5a'], MATERIALS['copper']),
}


def template_item(variant):
    if variant == 'D':
        from horseshoe_template_2026_10_02 import render
        return render()
    slab, accent = TEMPLATES[variant]
    sp, ap = [_hex(c) for c in slab], [_hex(c) for c in accent]
    img = shade_mask(SLAB, slab)
    # Flaechenstruktur: einzelne dunklere Punkte wie Vanillas Netherit-Vorlage.
    for (x, y) in ((4, 4), (10, 3), (5, 10), (11, 12), (3, 8), (9, 9)):
        img.putpixel((x, y), sp[2])
    ox, oy = 4, 4
    for dy, row in enumerate(ENGRAVING):
        for dx, ch in enumerate(row):
            if ch == '#':
                img.putpixel((ox + dx, oy + dy), ap[4] if dy < 2 or dx == 0 else ap[3])
            elif ch == 'd':
                img.putpixel((ox + dx, oy + dy), sp[1])
    img.putpixel((ox, oy), ap[5])
    img.putpixel((ox + 4, oy), ap[5])
    return img


# ---------------------------------------------------------------------------
# Huf-Ebene (64x64, UV wie AbstractEquineModel-Beine: texOffs 48,21, Kasten 4x11x4).
# Seiten: v=25..35, u 48-51 west, 52-55 vorn (north), 56-59 east, 60-63 hinten; Sohle u 56-59, v 21-24.
# ---------------------------------------------------------------------------
def hoof_layer(material):
    pal = [_hex(c) for c in MATERIALS[material]]
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    faces = {48: 3, 52: 4, 56: 2, 60: 2}  # side -> tone (front lit, back/right in shadow)
    for start, tone in faces.items():
        for u in range(start, start + 4):
            img.putpixel((u, 35), pal[tone])
    for u in (49, 58):  # Nagelkoepfe an den Flanken
        img.putpixel((u, 35), pal[1])
    img.putpixel((53, 34), pal[5])  # Zehenkappe vorn
    img.putpixel((54, 34), pal[4])
    for u in range(56, 60):  # Sohle: Ring
        for v in range(21, 25):
            if u in (56, 59) or v in (21, 24):
                img.putpixel((u, v), pal[2] if v != 21 else pal[3])
    if material == 'enderite':
        img.putpixel((54, 35), _hex(ENDER_VEIN))
    return img


# ---------------------------------------------------------------------------
# Leeres-Slot-Symbol (16x16, 1-px-Linie in Vanillas Slot-Grau) und Inventarleiste (24x82).
# ---------------------------------------------------------------------------
SLOT_ICON = [
    '................',
    '................',
    '................',
    '...##......##...',
    '..#..#....#..#..',
    '..#..#....#..#..',
    '..#..#....#..#..',
    '..#..#....#..#..',
    '..#...#..#...#..',
    '...#...##...#...',
    '....#......#....',
    '.....######.....',
    '................',
    '................',
    '................',
    '................',
]


def slot_icon():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(SLOT_ICON):
        for x, ch in enumerate(row):
            if ch == '#':
                img.putpixel((x, y), (124, 124, 124, 255))
    return img


PANEL_W, PANEL_H = 24, 82


def panel():
    """Vanilla container frame: black rim with rounded corners, white/#555 bevel, #c6c6c6 fill."""
    w, h = PANEL_W, PANEL_H
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    black, white, grey, dark = (0, 0, 0, 255), (255, 255, 255, 255), (198, 198, 198, 255), (85, 85, 85, 255)
    for y in range(h):
        for x in range(w):
            img.putpixel((x, y), grey)
    for x in range(w):
        for y in (0, h - 1):
            img.putpixel((x, y), black)
    for y in range(h):
        for x in (0, w - 1):
            img.putpixel((x, y), black)
    for x in range(1, w - 1):
        img.putpixel((x, 1), white); img.putpixel((x, 2), white)
        img.putpixel((x, h - 2), dark); img.putpixel((x, h - 3), dark)
    for y in range(1, h - 1):
        img.putpixel((1, y), white); img.putpixel((2, y), white)
        img.putpixel((w - 2, y), dark); img.putpixel((w - 3, y), dark)
    # Vanilla-Ecken: aussen transparent, innen Uebergangspixel
    for (x, y) in ((0, 0), (1, 0), (0, 1), (w - 1, 0), (w - 2, 0), (w - 1, 1),
                   (0, h - 1), (1, h - 1), (0, h - 2), (w - 1, h - 1), (w - 2, h - 1), (w - 1, h - 2)):
        img.putpixel((x, y), (0, 0, 0, 0))
    for (x, y) in ((1, 1), (w - 2, h - 2), (w - 3, 1), (1, h - 3)):
        img.putpixel((x, y), black if (x, y) in ((1, 1), (w - 2, h - 2)) else grey)
    img.putpixel((w - 2, 1), black); img.putpixel((1, h - 2), black)
    return img


# ---------------------------------------------------------------------------
def outputs(item_variant=ITEM_VARIANT, template_variant=TEMPLATE_VARIANT):
    files = {}
    for m in TIERS:
        files[f'item/{m}_horseshoe.png'] = horseshoe_item(m, item_variant)
        files[f'entity/horseshoe/{m}.png'] = hoof_layer(m)
    files['item/horseshoe_smithing_template.png'] = template_item(template_variant)
    files['gui/sprites/container/slot/horseshoe.png'] = slot_icon()
    files['gui/container/horseshoe_panel.png'] = panel()
    return files


def _png(img):
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return buf.getvalue()


def preview(path):
    s = 8
    cols, gap = 7, 6
    rows = []
    for v in 'ABC':
        rows.append((f'Hufeisen {v}', [horseshoe_item(m, v) for m in TIERS]))
    rows.append(('Vorlage A/B/C', [template_item(v) for v in 'ABC']))
    rows.append(('Slot + Huf', [slot_icon()]))
    width = 140 + cols * (16 * s + gap)
    height = len(rows) * (16 * s + gap) + 64 * 4 + 2 * gap
    sheet = Image.new('RGBA', (width, height), (139, 139, 139, 255))
    try:
        from PIL import ImageDraw
        draw = ImageDraw.Draw(sheet)
    except Exception:  # pragma: no cover
        draw = None
    y = gap
    for label, imgs in rows:
        if draw:
            draw.text((6, y + 56), label, fill=(0, 0, 0, 255))
        for i, im in enumerate(imgs):
            bg = Image.new('RGBA', (16 * s, 16 * s), (198, 198, 198, 255))
            bg.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST))
            sheet.alpha_composite(bg, (140 + i * (16 * s + gap), y))
        y += 16 * s + gap
    # Huf-Ebenen (Ausschnitt der Beinflaechen, 4-fach) und Inventarleiste
    for i, m in enumerate(TIERS):
        crop = hoof_layer(m).crop((48, 20, 64, 36)).resize((64 * 2, 64 * 2), Image.NEAREST)
        bg = Image.new('RGBA', crop.size, (90, 60, 40, 255))
        bg.alpha_composite(crop)
        sheet.alpha_composite(bg, (140 + i * (128 + gap), y))
    p = panel().resize((PANEL_W * 3, PANEL_H * 3), Image.NEAREST)
    sheet.alpha_composite(p, (6, y))
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--item', default=ITEM_VARIANT, choices='ABC')
    ap.add_argument('--template', default=TEMPLATE_VARIANT, choices='ABCD')
    ap.add_argument('--no-preview', action='store_true')
    args = ap.parse_args()
    stale = []
    for rel, img in outputs(args.item, args.template).items():
        target = ASSETS / rel
        data = _png(img)
        if args.check:
            if not target.exists() or Image.open(target).convert('RGBA').tobytes() != img.tobytes():
                stale.append(rel)
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
    if args.check:
        for rel in stale:
            print('STALE:', rel)
        sys.exit(1 if stale else 0)
    if not args.no_preview:
        preview(PREVIEW)
        print('preview:', PREVIEW)
    print('horseshoe textures written:', len(outputs()))


if __name__ == '__main__':
    main()
