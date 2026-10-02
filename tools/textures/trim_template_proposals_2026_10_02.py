"""Usage: python tools/textures/trim_template_proposals_2026_10_02.py <vanilla textures dir> [preview png] [png dir]

Owner 2026-10-02: ten alternative proposals for the mod's own smithing templates Glowing (Glow Ink Sac), Pulsating
(Echo Shard) and Emitting (Glowstone Dust). Proposals only. Each set takes the silhouette and pattern of one vanilla
armor-trim template: the cyan diamond pattern becomes the ingredient's colour (ramp taken from the vanilla ingredient
item), the stone body becomes a matching body ramp (themed, or deepslate-dark for the 'dunkel' sets)."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'besatzvorlagen-vorschau.png')
PNG_DIR = sys.argv[3] if len(sys.argv) > 3 else None


def vtex(path):
    return ps.load(os.path.join(V, path + '.png'))


def is_accent(p):
    return p[2] - p[0] > 50 and p[1] - p[0] > 40


def themes():
    glow_ink = ps.ramp_of(vtex('item/glow_ink_sac'))
    echo = ps.ramp_of(vtex('item/echo_shard'), 6)
    dust = [c for c in ps.ramp_of(vtex('item/glowstone_dust')) if ps.saturated(c + (255,), 30) or ps.lum(c) > 200]
    dust = [dust[round(i * (len(dust) - 1) / 5)] for i in range(6)]
    return {
        'Glowing': {'accent': [c for c in glow_ink if c[1] > c[0] + 30],
                    'body': ps.ramp((8, 20, 28), (20, 48, 58), (40, 82, 92), (74, 120, 124), n=7)},
        'Pulsating': {'accent': echo, 'body': ps.ramp_of(vtex('block/sculk'), 7)},
        'Emitting': {'accent': dust, 'body': ps.ramp((48, 26, 12), (92, 56, 26), (140, 92, 44), (182, 134, 74), n=7)},
    }


DEEP = None

# (vanilla template, body mode) - 'thema' = themed body, 'dunkel' = deepslate body
SETS = [('sentry', 'thema'), ('vex', 'thema'), ('ward', 'thema'), ('eye', 'thema'), ('spire', 'thema'),
        ('silence', 'thema'), ('wayfinder', 'thema'), ('snout', 'dunkel'), ('rib', 'dunkel'), ('coast', 'dunkel')]


def make(template, mode, theme):
    src = vtex(f'item/{template}_armor_trim_smithing_template')
    body = theme['body'] if mode == 'thema' else DEEP
    accent = ps.recolor(src, theme['accent'], mask=is_accent)
    themed = ps.recolor(src, body, mask=lambda p: not is_accent(p))
    out = themed.copy()
    for p in ps.opaque(src):  # the mask is decided on the source, so a teal body is never taken for pattern
        if is_accent(src.getpixel(p)):
            out.putpixel(p, accent.getpixel(p))
    return out


def main():
    global DEEP
    DEEP = ps.ramp_of(vtex('block/deepslate'), 7)
    th = themes()
    rows = []
    named = {}
    for name in ('Glowing', 'Pulsating', 'Emitting'):
        imgs = []
        for k, (tpl, mode) in enumerate(SETS):
            im = make(tpl, mode, th[name])
            imgs.append(im)
            named[f'{name.lower()}_trim_template_{ps.LETTERS[k].lower()}'] = im
        rows.append((name, imgs))
    item = lambda n: ps.load(os.path.join(ps.SB_ITEM, n + '.png'))
    refs = [('Glowing jetzt', item('glowing_trim_template')), ('Pulsating jetzt', item('pulsating_trim_template')),
            ('Emitting jetzt', item('emitting_trim_template')), ('Leuchttintenbeutel', vtex('item/glow_ink_sac')),
            ('Echoscherbe', vtex('item/echo_shard')), ('Glowstonestaub', vtex('item/glowstone_dust')),
            ('Vanilla: Rippen', vtex('item/rib_armor_trim_smithing_template'))]
    notes = ['Form je Spalte aus der Vanilla-Vorlage: ' + ', '.join(f'{ps.LETTERS[k]} {t}{" (dunkel)" if m == "dunkel" else ""}'
                                                                     for k, (t, m) in enumerate(SETS)),
             'Muster = Farbe der Zutat, Koerper = passende Grundfarbe; "dunkel" = Tiefenschiefer-Koerper.']
    ps.sheet('Besatzvorlagen Glowing / Pulsating / Emitting - 10 Alternativen (je Spalte ein Satz)', refs, rows,
             PREVIEW, notes=notes)
    if PNG_DIR:
        ps.save_pngs(PNG_DIR, named)
    print('ok')


if __name__ == '__main__':
    main()
