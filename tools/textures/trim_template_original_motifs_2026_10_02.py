"""Usage: python tools/textures/trim_template_original_motifs_2026_10_02.py <vanilla textures dir> [preview png]

Owner 2026-10-02, round 3 for the Glowing / Pulsating / Emitting smithing templates: background A
(trim_template_bg_motif_2026_10_02, the plain Sentry plate) for all three, and the motifs of the ORIGINAL textures
carried over onto it. Proposals only (nothing is written into the mod).

Where the motifs come from (git history of textures/item/*_trim_template.png):
- Glowing (owner, 2026-01-11 "trim update"): a dark glow-ink pool over the plate, in its middle a five-pixel checker
  cross in pale beige, grey glints scattered round it.
- Emitting (owner, 2026-01-13 "trim update"): a sun - bright core, rays running out to the plate's edges - over a
  faint grey lattice (the glowing pattern it upgrades).
- Pulsating (2026-09-29, the current texture): a cross with four bright tips and dark eye pixels, dim cyan specks.
  Owner: keep it more subtle, with more glimmer.
Each motif is read pixel by pixel from the original (classified by brightness/saturation, see the masks below) and
re-coloured onto background A in the template's colours by brightness rank: A = the faithful imitation, B and C two
small variations."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build', 'besatz-original-motive-vorschau.png')
sys.argv = [sys.argv[0], V]
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import trim_template_bg_motif_2026_10_02 as bgm  # noqa: E402
import trim_template_proposals_2026_10_02 as r1  # noqa: E402

bgm.V = r1.V = V
ORIGINAL = {n: ps.load(os.path.join(ps.SB_ITEM, f'{n}_trim_template.png')) for n in ('glowing', 'pulsating', 'emitting')}


def lum(c):
    return ps.lum(c)


def sat(c):
    return max(c[:3]) - min(c[:3])


def classify(name):
    """{(x, y): class} of the original's motif pixels."""
    src = ORIGINAL[name]
    out = {}
    for (x, y) in ps.opaque(src):
        c = src.getpixel((x, y))
        if name == 'glowing':
            if sat(c) <= 35 and lum(c) >= 100:
                out[(x, y)] = 'core'          # the beige checker cross (five pixels)
            elif sat(c) <= 20 and lum(c) >= 48:
                out[(x, y)] = 'glint'         # grey glints round it
            elif c[2] > c[0] + 12:
                out[(x, y)] = 'pool'          # the navy glow-ink pool
        elif name == 'emitting':
            if lum(c) >= 150:
                out[(x, y)] = 'core'          # the sun
            elif lum(c) >= 118:
                out[(x, y)] = 'ray'
            elif sat(c) <= 40 and lum(c) >= 70:
                out[(x, y)] = 'lattice'       # faint glowing lattice
        else:
            if lum(c) >= 180:
                out[(x, y)] = 'tip'
            elif c[2] - c[0] > 60 and lum(c) >= 90:
                out[(x, y)] = 'cross'
            elif c[2] - c[0] > 60 and lum(c) >= 60:
                out[(x, y)] = 'speck'
            elif lum(c) < 5:
                out[(x, y)] = 'eye'
    return out


def ranked(name, cls, ramp):
    """Map the class's pixels onto ramp by their brightness rank in the original."""
    src = ORIGINAL[name]
    pts = [p for p, k in classify(name).items() if k == cls]
    levels = sorted({round(lum(src.getpixel(p))) for p in pts})
    return {p: ramp[round(levels.index(round(lum(src.getpixel(p)))) * (len(ramp) - 1) / max(1, len(levels) - 1))]
            for p in pts}


def paint(plate, colours):
    im = plate.copy()
    for (x, y), c in colours.items():
        if im.getpixel((x, y))[3]:
            im.putpixel((x, y), tuple(c) + (255,))
    return im


def tint(plate, pts, colour, t):
    return {p: ps.mix(plate.getpixel(p)[:3], colour, t) for p in pts if plate.getpixel(p)[3]}


WHITE = (255, 255, 255)


def glowing(plate, acc, variant):
    """Pool darker than the plate, grey glints -> the glow ink's light teal, the checker cross pale like the
    original's beige (glow ink's lightest tone towards white), so it reads as clearly as on the original."""
    cls = classify('glowing')
    pool = [p for p, k in cls.items() if k == 'pool']
    out = tint(plate, pool, ps.mix(acc[0], (0, 0, 0), 0.4), 0.5 if variant != 'C' else 0.3)
    # the original's glints are quiet grey: here half way between the plate and the glow ink's teal
    glints = [p for p, k in cls.items() if k == 'glint']
    out.update(tint(plate, glints, acc[4], 0.5 if variant != 'B' else 0.7))
    pale = ps.mix(acc[-1], WHITE, 0.35 if variant != 'B' else 0.6)
    out.update(ranked('glowing', 'core', [acc[-2], pale]))
    if variant == 'B':  # one white glint in the middle of the checker cross
        out[(7, 7)] = WHITE
    return paint(plate, out)


def emitting(plate, acc, body, variant):
    """The sun: core in glowstone's lightest tones, rays one step below - lighter than the plate's brown so they read."""
    cls = classify('emitting')
    out = {}
    if variant != 'B':  # A, C keep the faint lattice of the glowing pattern underneath
        out.update(tint(plate, [p for p, k in cls.items() if k == 'lattice'], (150, 150, 150), 0.45))
    out.update(ranked('emitting', 'ray', acc[2:5]))
    core = [acc[4], acc[5], ps.mix(acc[5], WHITE, 0.4)]
    out.update(ranked('emitting', 'core', core if variant != 'C' else core[1:]))
    if variant == 'C':  # rays reach one pixel further on the diagonals
        for p in ((3, 4), (12, 4), (3, 12), (12, 12)):
            if plate.getpixel(p)[3]:
                out[p] = acc[3]
    return paint(plate, out)


def pulsating(plate, acc, body, variant):
    """More subtle than the original: the cross in the echo ramp's middle tones (not its glowing top), the dark eyes
    filled in; more glimmer: the four tips and the specks become single bright glints, plus extra glints."""
    cls = classify('pulsating')
    out = {}
    # the echo shard's glowing cyan only at half strength (its dark tones are darker than the plate)
    cross_ramp = [(20, 110, 118), (0, 146, 149)] if variant != 'B' else [(12, 92, 100), (20, 118, 126)]
    cross = ranked('pulsating', 'cross', cross_ramp)
    if variant == 'C':  # only the cross's two axes, the fill dropped
        cross = {p: c for p, c in cross.items() if p[0] == 8 or p[1] == 8}
    out.update(cross)
    for p, k in cls.items():
        if k == 'eye':
            out[p] = cross_ramp[0]
        elif k == 'tip':
            out[p] = ps.mix(acc[-1], WHITE, 0.4)
        elif k == 'speck':
            out[p] = acc[-1]
    extra = [(5, 3), (11, 5), (3, 10), (10, 12), (12, 10), (6, 13)]
    if variant == 'B':
        extra += [(4, 6), (9, 2), (13, 7), (7, 11)]
    for p in extra:
        if plate.getpixel(p)[3] and p not in out:
            out[p] = acc[-1] if (p[0] + p[1]) % 2 else acc[-2]
    return paint(plate, out)


def main():
    th = r1.themes()
    plates = {n: bgm.backgrounds(th[n]['body'])[0][1] for n in ('Glowing', 'Pulsating', 'Emitting')}
    rows = []
    for name in ('Glowing', 'Pulsating', 'Emitting'):
        acc, body = th[name]['accent'], th[name]['body']
        if name == 'Glowing':
            new = [glowing(plates[name], acc, v) for v in 'ABC']
        elif name == 'Pulsating':
            new = [pulsating(plates[name], acc, body, v) for v in 'ABC']
        else:
            new = [emitting(plates[name], acc, body, v) for v in 'ABC']
        rows.append((name, [ORIGINAL[name.lower()], plates[name]] + new))
    refs = [(n, bgm.vtex(f'item/{n}_armor_trim_smithing_template')) for n in
            ('sentry', 'eye', 'ward', 'silence', 'wayfinder', 'rib', 'spire')]
    s, cell, left = 12, 16 * 12 + 12, 120
    labels = ['Original', 'Hintergrund A', 'A Imitation', 'B Abwandlung', 'C Abwandlung']
    im = Image.new('RGBA', (left + max(len(refs), 5) * cell, 30 + (len(rows) + 1) * (cell + 24) + 60), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((10, 8), 'Besatzvorlagen Runde 3: Original-Motive auf Hintergrund A (Pulsating dezenter, mehr Glimmer)',
           fill=(0, 0, 0, 255))
    y = 30
    d.text((10, y + cell // 2), 'Vanilla', fill=(0, 0, 0, 255))
    for k, (n, ref) in enumerate(refs):
        d.text((left + k * cell, y), n, fill=(0, 0, 0, 255))
        im.alpha_composite(ref.resize((16 * s, 16 * s), Image.NEAREST), (left + k * cell, y + 14))
    for name, imgs in rows:
        y += cell + 24
        d.text((10, y + cell // 2), name, fill=(0, 0, 0, 255))
        for k, sprite in enumerate(imgs):
            d.text((left + k * cell, y), labels[k], fill=(0, 0, 0, 255))
            im.alpha_composite(sprite.resize((16 * s, 16 * s), Image.NEAREST), (left + k * cell, y + 14))
    y += cell + 30
    for line in ('Glowing: A Original-Motiv (dunkle Tintenlache, helles Schachbrett-Kreuz, Glanzpunkte); B heller, weisser Mittelglanz; C Lache schwaecher.',
                 'Pulsating: A Kreuz in Mitteltoenen, Augen gefuellt, Spitzen/Sprenkel als Glimmer + 6 Glimmer; B noch dezenter, 10 Glimmer; C nur Kreuzachsen.',
                 'Emitting: A Original-Sonne mit Strahlen ueber feinem Gitter; B ohne Gitter; C Kern kleiner, Diagonalstrahlen laenger.'):
        d.text((10, y), line, fill=(0, 0, 0, 255))
        y += 16
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    im.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
