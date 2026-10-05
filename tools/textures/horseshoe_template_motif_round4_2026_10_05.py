"""Horseshoe smithing template, round 4: the ten motifs (A-J) redrawn smaller again (proposals only).

Owner 2026-10-05 on round 3 (horseshoe_template_motif_round3_2026_10_04.py): "most were still too big".
Each letter redrawn pixel by pixel with its round-3 character: iron box at most 5x5 (like the small Vanilla motifs,
snout 5x5 / shaper 5x6), one-pixel bands where possible, no nail holes. Centred: iron on x 6..10 / y 5..9, the copper
drop shadow one pixel right and below. Background rules and checks are round 2's (pixel-identical plate).

Usage (Pillow): python tools/textures/horseshoe_template_motif_round4_2026_10_05.py [preview png]
Not installed - the owner picks one."""
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import horseshoe_template_motif_2026_10_04 as r2  # noqa: E402
import horseshoe_template_motif_round3_2026_10_04 as r3  # noqa: E402

PREVIEW = r'C:\Users\o_o\code\minecraft-mods\previews\hufeisen-vorlage-runde4-vorschau.png'

VARIANTS = [
    ('A', 'Klassisch rund', '1-px-Band, runde Zehe', [
        '.........',
        '..H...M..',
        '..H...Mp.',
        '..H...Mp.',
        '..L...Sp.',
        '...LMSp..',
        '....pp...',
    ]),
    ('B', 'Breit mit Stollen', 'niedrig und breit, dunkle Stollen-Enden', [
        '.........',
        '..S...S..',
        '..H...Mp.',
        '..L...Mp.',
        '..LMMMSp.',
        '...pppp..',
        '.........',
    ]),
    ('C', 'Offener Bogen', 'oben weit, zur flachen Zehe hin enger', [
        '.........',
        '..H...M..',
        '..H...Mp.',
        '...L.Mp..',
        '...LMSp..',
        '....pp...',
        '.........',
    ]),
    ('D', 'Eckig', 'scharfe Ecken, schwere zweireihige Zehe', [
        '.........',
        '..H...M..',
        '..H...Mp.',
        '..H...Mp.',
        '..LLMMSp.',
        '..SSSSSp.',
        '...ppppp.',
    ]),
    ('E', 'Schraeg gekippt', 'schraeg geneigt (kursives U), Arme parallel', [
        '.........',
        '....H.M..',
        '...H..Mp.',
        '..H..Mp..',
        '..L.Mp...',
        '..LSp....',
        '...p.....',
    ]),
    ('F', 'Schlank mit Umriss', '1-px-Eisen mit Kupfer-Umriss', [
        '.........',
        '.pHq.qMp.',
        '.pHq.qMp.',
        '.pLqqqMp.',
        '..pLMSp..',
        '...ppp...',
        '.........',
    ]),
    ('G', 'Spitze Zehe', 'Arme laufen zu einer Spitze zusammen', [
        '.........',
        '..H...M..',
        '..H...Mp.',
        '...L.Mp..',
        '....Sp...',
        '.....p...',
        '.........',
    ]),
    ('H', 'Gefalzt (Nut)', 'dunkle Nut auf der Bandinnenseite', [
        '.........',
        '..HS.SM..',
        '..HD.DMp.',
        '..HS.SMp.',
        '..LDDDMp.',
        '...MMMp..',
        '....ppp..',
    ]),
    ('I', 'Keilform', 'duenne Enden, unten breiter', [
        '.........',
        '..H...M..',
        '..H...Mp.',
        '..LL.MSp.',
        '..LMMMSp.',
        '...SSSp..',
        '....ppp..',
    ]),
    ('J', 'Kraeftig mit Relief', 'durchgehend dicke Arme, Lichtkante links', [
        '.........',
        '..HL.LM..',
        '..HL.LMp.',
        '..LL.MSp.',
        '..LMMMSp.',
        '...SSSp..',
        '....ppp..',
    ]),
]


def sheet(rows, path):
    s, pad, gap = 16, 20, 50
    t = 16 * s
    group = 2 * t + 12
    cols = 3
    w = pad + cols * (group + gap)
    row_h = t + 70
    h = 60 + ((len(rows) + cols - 1) // cols) * row_h
    img = Image.new('RGB', (w, h), (198, 198, 198))
    d = ImageDraw.Draw(img)
    d.text((pad, 12), 'Hufeisen-Vorlage Runde 4 - nochmals kleiner (Eisen max. 5x5 wie Vanilla snout/shaper), Hintergrund '
                      'pixelgleich. Je Buchstabe: Runde 3 | Runde 4', fill=(20, 20, 20))
    d.text((pad, 28), '16x gross, darunter 1x im Inventar-Slot. NICHT eingebaut - bitte A-J waehlen.', fill=(60, 60, 60))
    for i, (letter, title, desc, old, new, osz, nsz) in enumerate(rows):
        gx = pad + (i % cols) * (group + gap)
        gy = 50 + (i // cols) * row_h
        d.text((gx, gy), f'{letter} - {title}: {desc}', fill=(0, 0, 0))
        for k, (im, cap) in enumerate(((old, f'R3 {osz[0]}x{osz[1]}'), (new, f'R4 {nsz[0]}x{nsz[1]}'))):
            x = gx + k * (t + 12)
            r3.tile(d, img, im, x, gy + 16, s)
            d.text((x + 26, gy + 16 + t + 10), cap, fill=(30, 30, 30))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else PREVIEW
    old = {v[0]: v for v in r3.VARIANTS}
    rows = []
    for letter, title, desc, grid in VARIANTS:
        new_im, new_motif = r2.render(grid)
        r2.check(new_im, new_motif)
        old_im, old_motif = r2.render(old[letter][3])
        osz, nsz = r3.iron_box(old_im, old_motif), r3.iron_box(new_im, new_motif)
        assert nsz[0] <= 5 and nsz[1] <= 5, (letter, 'iron box above 5x5', nsz)
        assert nsz[0] * nsz[1] < osz[0] * osz[1], (letter, 'not smaller', osz, nsz)
        print(f'{letter}: Eisen {osz[0]}x{osz[1]} -> {nsz[0]}x{nsz[1]}')
        rows.append((letter, title, desc, old_im, new_im, osz, nsz))
    sheet(rows, path)
    print('ok: Hintergrund pixelgleich ->', path)


if __name__ == '__main__':
    main()
