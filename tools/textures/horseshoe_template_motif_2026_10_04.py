"""Horseshoe smithing template, round 2: ten nicer horseshoe motifs on the UNCHANGED plate (proposals only).

Owner 2026-10-04 on round 1 (horseshoe_template_and_riding_books_2026_10_04.py, A-J): "keep the background the same and
make the horseshoe motif a bit nicer, ten proposals again".

- Background: the installed texture, pixel for pixel. Only the motif field may change. The field is the Vanilla
  smithing-template motif area (x 4..12, y 4..10, where the netherite template draws its arrow). Inside the old
  horseshoe box (x 5..11, y 4..10) the leftover arrow pixels of the Basic plate are cleared to the plate's body copper;
  every pixel outside the box that a motif does not paint stays exactly as installed (checked below).
- Convention kept: a horseshoe instead of the arrow, iron inside, copper outside. Colours come only from the Vanilla
  26.3 iron_ingot / copper_ingot ramps (the installed texture's palettes): light edge top-left, shade bottom-right,
  dark copper drop shadow like the Vanilla templates' motif outline.

Usage (Pillow): python tools/textures/horseshoe_template_motif_2026_10_04.py [preview png]
Not installed - the owner picks one."""
import os
import sys
import textwrap

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
TARGET = os.path.join(ROOT, 'modules', 'simpleriding', 'shared', 'resources', 'assets', 'simpleriding', 'textures',
                      'item', 'horseshoe_smithing_template.png')
PREVIEW = r'C:\Users\o_o\code\minecraft-mods\previews\hufeisen-vorlage-runde2-vorschau.png'

FIELD_X, FIELD_Y, FIELD_W, FIELD_H = 4, 4, 9, 7      # Vanilla motif area
OLD_BOX = (5, 4, 11, 10)                             # installed horseshoe's bounding box (inclusive)
BODY = (193, 90, 54)                                 # plate body copper

# Vanilla 26.3 ramps (iron_ingot / copper_ingot), the same colours the installed texture uses.
PAL = {
    'H': (255, 255, 255), 'L': (216, 216, 216), 'M': (168, 168, 168), 'S': (114, 114, 114),
    'D': (88, 88, 88), 'K': (53, 53, 53),                       # iron: highlight .. nail hole
    'o': (109, 52, 33), 'p': (138, 65, 41), 'q': (156, 78, 49),  # copper shadows (plate's own darks)
    'c': (231, 124, 86), 'l': (252, 153, 130),                   # copper lights
}

# 9 x 7 grids over the motif field; '.' = plate.
VARIANTS = [
    ('A', 'Klassisch rund', 'gleichmaessiger U-Bogen, je Arm zwei Nagelloecher, runde Zehe, Schatten unten rechts', [
        '.HL...LM.',
        '.HD...DMp',
        '.HL...LMp',
        '.HD...DMp',
        '.LL...MSp',
        '..LMMMSp.',
        '...SSSp..',
    ]),
    ('B', 'Breit mit Stollen', 'flach und breit, kantige Stollen an den Enden, Nagelloecher in der Zehe', [
        'HHL...LMS',
        '.HL...LMp',
        '.HD...DMp',
        '.LL...LMp',
        '.LMDMDMSp',
        '..SSSSSpp',
        '...pppp..',
    ]),
    ('C', 'Offener Bogen', 'weit geoeffnete Enden, die zur Zehe hin zusammenlaufen (Parabel-Form)', [
        'HL.....LM',
        'HD.....DM',
        '.HL...LMp',
        '.HD...DMp',
        '.LL...MSp',
        '..LMMMSp.',
        '...SSSp..',
    ]),
    ('D', 'Eckig', 'kastenfoermiges U mit scharfen Ecken und schwerer Zehe', [
        '.HL...LM.',
        '.HL...LMp',
        '.HD...DMp',
        '.HL...LMp',
        '.LLLMMMSp',
        '.MSSSSSSp',
        '..ppppppp',
    ]),
    ('E', 'Schraeg gekippt', 'diagonal wie Vanilla-Werkzeugsymbole, oeffnet nach oben rechts, gleich lange Arme', [
        '....HL...',
        '...HL..LM',
        '..HD..LDp',
        '.HL..LMp.',
        '.L..LMp..',
        '.LMMSp...',
        '..SSp....',
    ]),
    ('F', 'Schlank mit Umriss', 'duennes Eisenband mit dunklem Kupfer-Umriss wie die Linien der Vanilla-Besatzvorlagen', [
        'pHp...pLp',
        'pHp...pMp',
        'pHp...pMp',
        'pLp...pMp',
        'pLpppppMp',
        '.pLMMMSp.',
        '..ppppp..',
    ]),
    ('G', 'Spitze Zehe', 'Arme laufen unten spitz zusammen (V-Zehe)', [
        '.HL...LM.',
        '.HD...DMp',
        '.HL...LMp',
        '.LD...DMp',
        '..LL.MSp.',
        '...LMSp..',
        '....Sp...',
    ]),
    ('H', 'Gefalzt (Nut)', 'dickes Eisen mit dunkler Nut, in der die Nagelloecher sitzen', [
        'HLL...LLM',
        'HSL...LSM',
        'HDL...LDM',
        'HSL...LSM',
        'LDL...MDS',
        '.MSDSDSSp',
        '..SSSSSp.',
    ]),
    ('I', 'Keilform', 'duenne Enden, nach unten breiter werdend, schwere Zehe mit Loechern', [
        '.H.....M.',
        '.H.....Mp',
        '.HL...LMp',
        '.HL...LMp',
        '.LLD.DMSp',
        '.LMMMMMSp',
        '..SSSSSp.',
    ]),
    ('J', 'Kraeftig mit Relief', 'breites Eisen, runde Enden, Loecher in der Bandmitte, Lichtkante links', [
        '.HL...LM.',
        'HLL...LMS',
        'HDM...LDS',
        'HLM...LMS',
        'HDM...LDS',
        '.LMMMMMSp',
        '..SSSSSp.',
    ]),
]


def installed():
    """The plate as installed before these rounds (round-1 texture of horseshoe_template_2026_10_02.py), rendered
    rather than read from TARGET so the background reference stays fixed once a round-5 motif is installed."""
    from horseshoe_template_2026_10_02 import render
    return render()


def base():
    """Installed texture with the old horseshoe box cleared to the plate body colour."""
    im = installed()
    x0, y0, x1, y1 = OLD_BOX
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), BODY + (255,))
    return im


def render(grid):
    assert len(grid) == FIELD_H and all(len(r) == FIELD_W for r in grid), grid
    im = base()
    motif = set()
    for r, row in enumerate(grid):
        for c, s in enumerate(row):
            if s != '.':
                im.putpixel((FIELD_X + c, FIELD_Y + r), PAL[s] + (255,))
                motif.add((FIELD_X + c, FIELD_Y + r))
    return im, motif


def check(im, motif):
    ref = installed()
    x0, y0, x1, y1 = OLD_BOX
    allowed = {tuple(PAL[k]) for k in PAL} | {BODY}
    for y in range(16):
        for x in range(16):
            in_box = x0 <= x <= x1 and y0 <= y <= y1
            if (x, y) in motif:
                assert FIELD_X <= x < FIELD_X + FIELD_W and FIELD_Y <= y < FIELD_Y + FIELD_H
            elif not in_box:
                assert im.getpixel((x, y)) == ref.getpixel((x, y)), ('background changed', x, y)
            if in_box or (x, y) in motif:
                assert im.getpixel((x, y))[:3] in allowed and im.getpixel((x, y))[3] == 255
    assert im.getchannel('A').tobytes() == ref.getchannel('A').tobytes()
    return sum(1 for p in motif if im.getpixel(p)[:3] in {PAL[k] for k in 'HLMSDK'})


def sheet(entries, path):
    s = 16
    tile = 16 * s
    cols = 4
    pad = 24
    label_h = 54
    w = cols * (tile + pad) + pad
    rows = (len(entries) + cols - 1) // cols
    h = 70 + rows * (tile + label_h + 70) + 20
    img = Image.new('RGB', (w, h), (198, 198, 198))
    d = ImageDraw.Draw(img)
    d.text((pad, 14), 'Hufeisen-Vorlage Runde 2 - Hintergrund pixelgleich zur eingebauten Fassung, nur das Hufeisen neu '
                      '(innen Eisen, aussen Kupfer)', fill=(20, 20, 20))
    d.text((pad, 32), 'je Feld: 16x gross, darunter 1x und 2x auf Inventar-Grau. NICHT eingebaut - bitte A-J waehlen.',
           fill=(60, 60, 60))
    for i, (name, desc, im) in enumerate(entries):
        cx = pad + (i % cols) * (tile + pad)
        cy = 70 + (i // cols) * (tile + label_h + 70)
        big = im.resize((tile, tile), Image.Resampling.NEAREST)
        d.rectangle((cx - 1, cy - 1, cx + tile, cy + tile), outline=(120, 120, 120))
        img.paste(big, (cx, cy), big)
        d.text((cx, cy + tile + 6), name, fill=(0, 0, 0))
        for k, line in enumerate(textwrap.wrap(desc, 46)[:2]):
            d.text((cx, cy + tile + 22 + 14 * k), line, fill=(50, 50, 50))
        # inventory slots: 1x and 2x
        sy = cy + tile + label_h
        for scale, ox in ((1, 0), (2, 30)):
            slot = 18 * scale
            sx = cx + ox
            d.rectangle((sx, sy, sx + slot - 1, sy + slot - 1), fill=(139, 139, 139))
            d.line((sx, sy, sx + slot - 1, sy), fill=(55, 55, 55))
            d.line((sx, sy, sx, sy + slot - 1), fill=(55, 55, 55))
            d.line((sx, sy + slot - 1, sx + slot - 1, sy + slot - 1), fill=(255, 255, 255))
            d.line((sx + slot - 1, sy, sx + slot - 1, sy + slot - 1), fill=(255, 255, 255))
            small = im.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST)
            img.paste(small, (sx + scale, sy + scale), small)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else PREVIEW
    entries = [('jetzt eingebaut', 'aktuelle Fassung (Referenz)', installed())]
    for letter, title, desc, grid in VARIANTS:
        im, motif = render(grid)
        n = check(im, motif)
        assert n >= 14, (letter, 'too little iron', n)
        entries.append((f'{letter} - {title}', desc, im))
    sheet(entries, path)
    print('ok:', len(VARIANTS), 'Varianten, Hintergrund pixelgleich ->', path)


if __name__ == '__main__':
    main()
