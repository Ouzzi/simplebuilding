"""Horseshoe smithing template, round 3: the ten round-2 motifs (A-J) redrawn smaller (proposals only).

Owner 2026-10-04 on round 2 (horseshoe_template_motif_2026_10_04.py): "the horseshoe motifs are all a bit too big".
Each letter is redrawn pixel by pixel (no scaling) with the same character, one to two pixels narrower and lower.
Reference: the Vanilla 26.3 smithing templates' motif pixels (cyan) measure 5x6 (netherite upgrade, 7x7 with its dark
outline) and mostly 5..8 x 5..8 for the armour trims (see vanilla_motif_sizes()). The iron of every new motif stays
within 8x6, most at 6x6 like the netherite arrow with outline. Where the smaller area makes details unreadable, details
are dropped (one nail hole per arm instead of two, no toe holes).

Background, palette and field rules are round 2's (render/check imported from there): every pixel outside the motif
and the old horseshoe box stays exactly as installed.

Usage (Pillow): python tools/textures/horseshoe_template_motif_round3_2026_10_04.py [preview png]
Not installed - the owner picks one."""
import os
import sys
import textwrap
from io import BytesIO
from zipfile import ZipFile

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import horseshoe_template_motif_2026_10_04 as r2  # noqa: E402

PREVIEW = r'C:\Users\o_o\code\minecraft-mods\previews\hufeisen-vorlage-runde3-vorschau.png'
JAR = os.path.join(os.path.expanduser('~'), '.gradle', 'caches', 'fabric-loom', '26.3', 'minecraft-client.jar')
IRON = {r2.PAL[k] for k in 'HLMSDK'}

# 9 x 7 grids over the same motif field as round 2; '.' = plate.
VARIANTS = [
    ('A', 'Klassisch rund', 'gleichmaessiger U-Bogen, ein Nagelloch je Arm, runde Zehe', [
        '.HL..LM..',
        '.HD..DMp.',
        '.HL..LMp.',
        '.LL..MSp.',
        '.LLMMMSp.',
        '..SSSSp..',
        '...ppp...',
    ]),
    ('B', 'Breit mit Stollen', 'niedrig, nach aussen gezogene Stollen, ein Loch je Arm', [
        '.........',
        'HHL..LMS.',
        '.HL..LMp.',
        '.HD..DMp.',
        '.LLMMMSp.',
        '..SSSSp..',
        '...ppp...',
    ]),
    ('C', 'Offener Bogen', 'weite Enden, die zur Zehe hin eng zusammenlaufen', [
        '.HL...LM.',
        '.HL...LMp',
        '..HL.LMp.',
        '..LL.MSp.',
        '..LMMMSp.',
        '...SSSp..',
        '....pp...',
    ]),
    ('D', 'Eckig', 'kastenfoermiges U mit scharfen Ecken und schwerer Zehe', [
        '.........',
        '.HL..LM..',
        '.HL..LMp.',
        '.HD..DMp.',
        '.LLLMMSp.',
        '.MSSSSSp.',
        '..pppppp.',
    ]),
    ('E', 'Schraeg gekippt', 'diagonal wie Vanilla-Werkzeugsymbole, oeffnet nach oben rechts', [
        '.........',
        '...HL....',
        '..HL..LM.',
        '.HL..LMp.',
        '.L..LMp..',
        '.LMMSp...',
        '..SSp....',
    ]),
    ('F', 'Schlank mit Umriss', 'duennes Eisenband, aussen dunkler, innen weicher Kupfer-Umriss', [
        '.........',
        '.pHq.qLp.',
        '.pHq.qMp.',
        '.pHq.qMp.',
        '.pLqqqMp.',
        '..pLMSp..',
        '...ppp...',
    ]),
    ('G', 'Spitze Zehe', 'Arme laufen unten spitz zusammen (V-Zehe)', [
        '.........',
        '.HL..LM..',
        '.HD..DMp.',
        '.LL..LMp.',
        '..LLMSp..',
        '...MSp...',
        '....p....',
    ]),
    ('H', 'Gefalzt (Nut)', 'dunkle Nut rundherum auf der Bandinnenseite, Loecher in der Nut', [
        '.HS...SM.',
        '.HD...DMp',
        '.HS...SMp',
        '.HD...DMp',
        '.LDSDSDMp',
        '..MMMMMp.',
        '...ppppp.',
    ]),
    ('I', 'Keilform', 'duenne Enden, nach unten breiter, schwere Zehe', [
        '.H....M..',
        '.H....Mp.',
        '.HL..LMp.',
        '.LL..MSp.',
        '.LMMMMSp.',
        '..SSSSp..',
        '...ppp...',
    ]),
    ('J', 'Kraeftig mit Relief', 'dicke dreifarbige Arme, Loch in der Bandmitte, schwere Zehe', [
        'HLL..LMS.',
        'HDM..LDSp',
        'HLM..LMSp',
        'LLM..MSSp',
        '.LMMMMSp.',
        '..SSSSp..',
        '...ppp...',
    ]),
]


def vanilla_motif_sizes():
    """Bounding boxes of the cyan motif pixels in the Vanilla smithing templates."""
    out = {}
    with ZipFile(JAR) as z:
        for n in sorted(z.namelist()):
            if n.startswith('assets/minecraft/textures/item/') and n.endswith('smithing_template.png'):
                im = Image.open(BytesIO(z.read(n))).convert('RGBA')
                pts = [(x, y) for y in range(16) for x in range(16)
                       if (c := im.getpixel((x, y)))[3] and c[2] > c[0] + 60 and c[1] > c[0] + 60]
                if pts:
                    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
                    out[n.rsplit('/', 1)[1][:-4]] = (max(xs) - min(xs) + 1, max(ys) - min(ys) + 1)
    return out


def iron_box(im, motif):
    pts = [p for p in motif if im.getpixel(p)[:3] in IRON]
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    return max(xs) - min(xs) + 1, max(ys) - min(ys) + 1


def tile(d, img, im, x, y, s=16):
    big = im.resize((16 * s, 16 * s), Image.Resampling.NEAREST)
    d.rectangle((x - 1, y - 1, x + 16 * s, y + 16 * s), outline=(120, 120, 120))
    img.paste(big, (x, y), big)
    sy = y + 16 * s + 6
    d.rectangle((x, sy, x + 17, sy + 17), fill=(139, 139, 139))
    d.line((x, sy, x + 17, sy), fill=(55, 55, 55))
    d.line((x, sy, x, sy + 17), fill=(55, 55, 55))
    d.line((x, sy + 17, x + 17, sy + 17), fill=(255, 255, 255))
    d.line((x + 17, sy, x + 17, sy + 17), fill=(255, 255, 255))
    img.paste(im, (x + 1, sy + 1), im)


def sheet(rows, path):
    s, pad, gap = 16, 20, 60
    t = 16 * s
    group = 3 * t + 2 * 12
    w = pad + 2 * (group + gap)
    row_h = t + 90
    h = 76 + 5 * row_h
    img = Image.new('RGB', (w, h), (198, 198, 198))
    d = ImageDraw.Draw(img)
    d.text((pad, 12), 'Hufeisen-Vorlage Runde 3 - Motive kleiner neu gezeichnet (Hintergrund pixelgleich, innen Eisen, '
                      'aussen Kupfer). Je Buchstabe: jetzt eingebaut | Runde 2 | Runde 3 (neu)', fill=(20, 20, 20))
    d.text((pad, 30), '16x gross, darunter 1x im Inventar-Slot. Vanilla-Motive: Netherit-Pfeil 5x6 (7x7 mit Umriss), '
                      'Besatzvorlagen meist 5-8 x 5-8 px. NICHT eingebaut - bitte A-J waehlen.', fill=(60, 60, 60))
    for i, (letter, title, desc, cur, old, new, osz, nsz) in enumerate(rows):
        gx = pad + (i % 2) * (group + gap)
        gy = 60 + (i // 2) * row_h
        for k, (im, cap) in enumerate(((cur, 'jetzt'), (old, f'R2 {osz[0]}x{osz[1]}'), (new, f'R3 {nsz[0]}x{nsz[1]}'))):
            x = gx + k * (t + 12)
            tile(d, img, im, x, gy + 16, s)
            d.text((x + 26, gy + 16 + t + 10), cap, fill=(30, 30, 30))
        d.text((gx, gy), f'{letter} - {title}', fill=(0, 0, 0))
        d.text((gx + t + 12 + 110, gy + 16 + t + 10), '', fill=(0, 0, 0))
        for k, line in enumerate(textwrap.wrap(desc, 70)[:2]):
            d.text((gx + 2 * (t + 12) - 250, gy + 16 + t + 30 + 14 * k), line, fill=(50, 50, 50))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else PREVIEW
    sizes = vanilla_motif_sizes()
    print('Vanilla-Motive (Cyan-Pixel):', ', '.join(f'{n.split("_")[0]} {w}x{h}' for n, (w, h) in sizes.items()))
    cur = r2.installed()
    old = {v[0]: v for v in r2.VARIANTS}
    rows = []
    for letter, title, desc, grid in VARIANTS:
        new_im, new_motif = r2.render(grid)
        r2.check(new_im, new_motif)
        old_im, old_motif = r2.render(old[letter][3])
        osz, nsz = iron_box(old_im, old_motif), iron_box(new_im, new_motif)
        assert nsz[0] < osz[0] and nsz[1] < osz[1], (letter, 'not smaller', osz, nsz)
        assert nsz[0] <= 8 and nsz[1] <= 6, (letter, 'larger than the Vanilla motifs', nsz)
        print(f'{letter}: Eisen {osz[0]}x{osz[1]} -> {nsz[0]}x{nsz[1]}')
        rows.append((letter, title, desc, cur, old_im, new_im, osz, nsz))
    sheet(rows, path)
    print('ok: Hintergrund pixelgleich ->', path)


if __name__ == '__main__':
    main()
