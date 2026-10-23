"""W23-4 (owner 2026-10-23 evening, branch a-misctex23b, plan docs/ai/PLAN-A-MISCTEX23B-2026-10-23.md).

Follow-up of misctex23_2026_10_23.py / tex23_preview_2026_10_23.py. Owner: "everything looks harsh" - softer transitions,
smaller tone steps, no pure white/black peaks. Own pixels in named layers (Basis / Overlay / Details, PRINZIPIEN A8).

Built in (--write):
* Jukebox Amplifier / Note Amplifier (variant A): the jukebox frame once all around (exactly 90 degree rotation symmetric,
  so the block reads the same however it is turned), inside the note block grille (180 degree symmetric) with the
  material radially in its holes; the Note Amplifier weaker. Vanilla-derived palette (the note block's tones from the
  jar), so it is drawn at build time through music_disc_textures.derived() - amplifier_textures() below - and never
  committed. Same for block/note_block_framed (the framed note block, only used with the client option
  noteBlockFrameTexture, NoteBlockFrameSpritesMixin).
* Sales Storage (simpleshops) + Storage Crafting Table (simplestorage): the softer B (B2 chosen over B1, see page).
* Animations A1 style A (breathing glow): dimensional scraps (overworld/nether/end, side + top), deepslate sage ore,
  crimson/warped blazewood planks + bookshelves - accepted texture as resting frame, only glowing pixels move.
* Piggy effect P2 (frontal head, Vanilla-like, pupils outside, funny).
* Loose Ashwood leaves L1 (about 30 % opaque): gen_textures.leaves_loose (written through gen_textures.write).

Candidates only (page): note block frame variants B/C, sales storage/table B1, backpack D/E/F (turned clockwise like
the bundle), butter inner X/Y/Z, Piggy P1/P3.

    python3.12 tools/textures/misctex23b_2026_10_23.py --page /root/sb-tools-a/previews/a-misctex23b-preview.html [--jar JAR]
    python3.12 tools/textures/misctex23b_2026_10_23.py --write       (repo textures, run on sb-test)
    python3.12 tools/textures/misctex23b_2026_10_23.py --check       (written textures up to date)
"""
from __future__ import annotations

import argparse
import html
import io
import json
import math
import os
import random
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
_argv = sys.argv[:]
import layers as L  # noqa: E402
import misctex23_2026_10_23 as m  # noqa: E402
from misctex23_2026_10_23 import H, Tex, mix, put, ramp, rgb, load  # noqa: E402
sys.argv = _argv

SHOPS = m.SHOPS
STORAGE = m.STORAGE
OVERLAY = m.OVERLAY
FUN = ROOT / 'modules/simplefun/shared/resources/assets/simplefun/textures/mob_effect'
SAN = ROOT / 'modules/simplesandwiches/shared/resources/assets/simplesandwiches/textures/block'
END_TOOLS = ROOT / 'modules/simpleend/tools'


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def write_tex(path: Path, tex: Tex, mcmeta=None, check=False):
    """PNG = exact composition of the layer source (layers.py), plus <name>.layers.json (+ .png.mcmeta)."""
    doc = tex.doc()
    data = L.png_write(tex.w, tex.h, bytes(L.compose(doc)))
    if check:
        ok = path.exists() and bytes(L.png_read(path.read_bytes())[2]) == bytes(L.compose(doc))
        if mcmeta is not None:
            mp = Path(str(path) + '.mcmeta')
            ok = ok and mp.exists() and json.loads(mp.read_text(encoding='utf-8')) == mcmeta
        return ok
    path.write_bytes(data)
    L.write_beside(path, doc)
    if mcmeta is not None:
        Path(str(path) + '.mcmeta').write_text(json.dumps(mcmeta, indent=2) + '\n', encoding='utf-8')
    return True


# ---------------------------------------------------------------- 1) amplifiers / note block (90 degree symmetric frame)

def rot(x, y):
    """One quarter turn of a 16 x 16 face."""
    return 15 - y, x


def side_of(x, y):
    """(ring, position along its side) - the ring pixel turned onto the top side; each ring pixel belongs to exactly one
    of the four sides (top: x = r .. 14 - r), so anything that depends only on this pair is 90 degree symmetric."""
    r = min(x, y, 15 - x, 15 - y)
    for _ in range(4):
        if y == r and r <= x <= 14 - r:
            return r, x - r
        x, y = rot(x, y)
    raise AssertionError((x, y))


def wood_ramp(note):
    """The note block's own tones (dark -> light) as a continuous ramp; its near-black border tone is left out (soft)."""
    cols = sorted({rgb(note, x, y) for y in range(16) for x in range(16)}, key=lum)
    cols = cols[1:] if len(cols) > 3 else cols
    top = tuple(min(255, round(v * 1.12)) for v in cols[-1])   # one soft highlight above the lightest tone
    return cols + [top]


FRAMES_T10 = {
    # rings from the edge: (tone, seam tone) along each side; then the center size
    'A': dict(rings=[(0.72, 0.56), (0.88, 0.74), (0.30, 0.30), (0.56, 0.46)], center=8, studs=False),
    'B': dict(rings=[(0.80, 0.62), (0.30, 0.30), (0.56, 0.46)], center=10, studs=False),
    'C': dict(rings=[(0.72, 0.56), (0.88, 0.74), (0.30, 0.30), (0.56, 0.46)], center=8, studs=True),
}


def framed(note, variant='A', mat=None, strength=1.0, mat_back=0.25):
    """Jukebox frame once all around + note block grille. strength < 1 pulls the frame's relief towards its mean tone
    (Note Amplifier / note block weaker); mat = material colours (dark -> light) radially in the grille's holes."""
    spec = FRAMES_T10[variant]
    wood = wood_ramp(note)
    mean = 0.55

    def tone(t):
        return ramp(wood, mean + (t - mean) * strength)
    t = Tex()
    base = t.layer('Basis: Rahmen rundum (90°-drehsymmetrisch)')
    nr = len(spec['rings'])
    for y in range(16):
        for x in range(16):
            r, p = side_of(x, y)
            if r >= nr:
                continue
            body, seam = spec['rings'][r]
            n = 15 - 2 * r                       # pixels on this side of the ring
            tv = body
            if r <= 1 and p in (5, 10):          # plank joints of the frame boards
                tv = seam
            elif (r, p) in ((1, 2), (1, 3), (1, 12), (0, 8), (0, 13)):
                tv = (body + seam) / 2           # soft grain dashes
            if r == nr - 1 and p == 0:           # inner rim corner a touch darker (mitre)
                tv = seam
            if r == 0 and p == 0:                # outer corner: the board ends meet
                tv = (body + seam) / 2
            del n
            put(base, x, y, tone(tv))
    mid = t.layer('Overlay: Notenblock-Gitter (180°-symmetrisch)')
    c0 = nr
    c1 = 15 - nr
    half = (c1 - c0 + 1) / 2
    for y in range(c0, c1 + 1):
        for x in range(c0, c1 + 1):
            d = max(abs(x - 7.5), abs(y - 7.5)) / half      # 0 centre .. ~1 at the rim
            hole = (x + y) % 2 == 0
            if hole:
                tv = 0.26 - 0.08 * d                         # holes darker towards the rim (shadow of the rim)
                if (x - y) % 4 == 2:
                    tv += 0.05                               # diagonal rows like the note block (180 degree safe)
            else:
                tv = 0.70 + 0.06 * (1 - d) - (0.06 if d > 0.85 else 0)
                if (x + y) % 4 == 3:
                    tv -= 0.06
            put(mid, x, y, tone(tv))
    if mat:
        det = t.layer('Details: Material in den Löchern')
        for y in range(c0, c1 + 1):
            for x in range(c0, c1 + 1):
                if (x + y) % 2:
                    continue
                dn = min(1.0, math.hypot(x - 7.5, y - 7.5) / (half * 1.25))
                c = ramp(mat, 0.25 + 0.45 * dn ** 1.6)
                put(det, x, y, mix(c, rgb(mid, x, y), mat_back))
    if spec['studs']:
        st = t.layer('Details: Nieten + Membran-Kern')
        stud = mat[-2] if mat else ramp(wood, mean + (0.95 - mean) * strength)
        for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
            put(st, x, y, mix(stud, tone(0.8), 0.35))
        core = mat[-1] if mat else ramp(wood, mean + (0.9 - mean) * strength)
        for x, y in ((7, 7), (8, 8)):
            put(st, x, y, mix(core, tone(0.6), 0.3))
        for x, y in ((8, 7), (7, 8)):
            put(st, x, y, mix(core, tone(0.6), 0.55))
    return t


def check_symmetry(im, center_from):
    """Frame exactly 90 degree symmetric, the whole face 180 degree symmetric."""
    for y in range(16):
        for x in range(16):
            a = im.getpixel((x, y))
            assert a == im.getpixel((15 - x, 15 - y)), ('180', x, y)
            if min(x, y, 15 - x, 15 - y) < center_from:
                assert a == im.getpixel(rot(x, y)), ('90', x, y)


def materials():
    saved = sys.argv[:]
    try:
        import speaker_proposals_2026_10_04 as sp  # noqa: E402 (reads the repo's material items)
    finally:
        sys.argv = saved
    out = {}
    for name, item in (('astralit', 'astralit_dust'), ('nihilith', 'nihilith_shard')):
        cols = sp.material_colours(item)
        n = len(cols)
        out[name] = [cols[min(n - 1, int(n * q))] for q in (0.2, 0.35, 0.5, 0.65, 0.8)]
    return out


AMP_VARIANT = 'A'
NOTE_STRENGTH = 0.6  # Note Amplifier and the framed note block: 60 % of the frame relief of the Jukebox Amplifier


def amplifier_textures(note, variant=AMP_VARIANT):
    """Build-time textures (music_disc_textures.derived()): one texture for all six faces of each amplifier, plus the
    framed note block for the client option noteBlockFrameTexture."""
    mats = materials()
    juke = framed(note, variant, mats['astralit'], 1.0, 0.25).image()
    noteamp = framed(note, variant, mats['nihilith'], NOTE_STRENGTH, 0.4).image()
    plain = framed(note, variant, None, NOTE_STRENGTH).image()
    nr = len(FRAMES_T10[variant]['rings'])
    for im in (juke, noteamp, plain):
        check_symmetry(im, nr)
    return {'jukebox_amplifier_side': juke, 'jukebox_amplifier_top': juke,
            'note_amplifier_side': noteamp, 'note_amplifier_top': noteamp, 'note_block_framed': plain}


# ---------------------------------------------------------------- 2) Sales Storage + Storage Crafting Table (B, softer)

# B was dark spruce + near-white iron + black mouth: the steps were too hard. Both refinements keep the spruce/barrel wood
# but with even steps, iron without white (B1) or only small fittings (B2), the mouth a warm dark gradient, softer gold.
SPRUCE = [H('3b2a18'), H('47331e'), H('533c24'), H('5f452a'), H('6c4f31'), H('7a5a39'), H('886542')]
IRON_SOFT = [H('4c4e54'), H('5f626a'), H('73767e'), H('878a92'), H('9a9ea5'), H('abafb5')]
GOLD_SOFT = [H('8a6424'), H('a77d32'), H('c49a46'), H('dab85e'), H('e8cc7a')]
MOUTH = [H('241810'), H('2e2015'), H('3a291a'), H('47331e')]


def soft_planks(im, seed, vertical=False, board=4):
    """Boards with a soft lit edge and seam (one ramp step, not three), sparse grain and a knot per board."""
    rnd = random.Random(seed)
    w = SPRUCE
    for y in range(16):
        for x in range(16):
            u, v = (x, y) if vertical else (y, x)
            k = u % board
            off = (u // board) * 5
            if k == board - 1:
                c = w[2]
            elif k == 0:
                c = w[5]
            else:
                c = w[4]
                if (v + off) % 7 in (2, 3) and (u // board + k) % 2 == 0:
                    c = w[3]
                if rnd.random() < 0.05:
                    c = w[3]
            if (v + off) % 16 == 11 and 0 < k < board - 1:
                c = w[2] if k == 1 else w[3]
            put(im, x, y, c)


def soft_coin(im, x, y):
    put(im, x, y, GOLD_SOFT[4])
    put(im, x + 1, y, GOLD_SOFT[3])
    put(im, x, y + 1, GOLD_SOFT[2])
    put(im, x + 1, y + 1, GOLD_SOFT[1])


def sales_storage_soft(v):
    """v 'B1': the B layout (iron collar, coin chute, banded lid) in soft tones. 'B2': wood fittings instead of iron
    bands (darker boards as beams), only small iron corner plates, the chute a recessed groove with a gradient."""
    res = {}
    beam = [SPRUCE[1], SPRUCE[2], SPRUCE[3], SPRUCE[4]]
    met = IRON_SOFT
    # side
    t = Tex()
    soft_planks(t.layer('Basis: Bretter'), 11, vertical=True)
    ov = t.layer('Overlay: Kragen')
    for x in range(16):
        if v == 'B1':
            put(ov, x, 0, met[4] if x % 5 else met[5])
            put(ov, x, 1, met[3])
            put(ov, x, 2, met[2])
            put(ov, x, 3, mix(met[0], SPRUCE[1], 0.5))
        else:
            put(ov, x, 0, beam[3])
            put(ov, x, 1, beam[2])
            put(ov, x, 2, beam[1])
        put(ov, x, 15, beam[1] if v == 'B2' else met[1])
    for y in range(16):
        put(ov, 0, y, (beam[3] if v == 'B2' else met[3]) if y > 2 else (beam[3] if v == 'B2' else met[4]))
        put(ov, 15, y, (beam[1] if v == 'B2' else met[1]))
    if v == 'B2':
        for x, y in ((0, 0), (1, 0), (0, 1), (15, 0), (14, 0), (15, 1)):
            put(ov, x, y, met[3] if (x, y) in ((0, 0), (15, 0)) else met[2])
    det = t.layer('Details: Münzschacht')
    for y in range(4 if v == 'B1' else 3, 15):
        half = 2 if y < 9 else 1
        for x in range(8 - half - 1, 8 + half + 1):
            dd = abs(x - 7.5) / (half + 1)
            put(det, x, y, ramp(MOUTH, 0.15 + 0.6 * dd + 0.25 * (y - 4) / 11))
        put(det, 8 - half - 2, y, met[3] if v == 'B1' else SPRUCE[5])
        put(det, 8 + half + 1, y, met[1] if v == 'B1' else SPRUCE[2])
    for x, y in ((6, 4), (8, 6), (7, 9), (7, 12)) if v == 'B1' else ((7, 5), (7, 10)):
        soft_coin(det, x, y)
    res['sales_storage_side'] = t
    # intake
    t = Tex()
    soft_planks(t.layer('Basis: Bretter'), 12)
    ov = t.layer('Overlay: Kragen + Einlass')
    for y in range(16):
        for x in range(16):
            r = max(abs(x - 7.5), abs(y - 7.5))
            lit = x + y < 15
            if r > 6.5:
                if v == 'B1':
                    put(ov, x, y, met[4] if lit else met[2])
                else:
                    put(ov, x, y, beam[3] if lit else beam[1])
            elif r > 5.5:
                if v == 'B1':
                    put(ov, x, y, met[3] if lit else met[1])
                else:
                    put(ov, x, y, beam[2] if lit else beam[0])
            elif r > 4.5:
                put(ov, x, y, mix(MOUTH[3], SPRUCE[2], 0.4) if lit else MOUTH[2])
            else:
                rr = math.hypot(x - 7.5, y - 7.5) / 6.4
                put(ov, x, y, ramp(MOUTH, 0.1 + 0.8 * rr))
    if v == 'B2':
        for cx, cy in ((0, 0), (15, 0), (0, 15), (15, 15)):
            sx, sy = (1 if cx == 0 else -1), (1 if cy == 0 else -1)
            put(ov, cx, cy, met[4] if cx + cy < 15 else met[2])
            put(ov, cx + sx, cy, met[3] if cy == 0 else met[2])
            put(ov, cx, cy + sy, met[3] if cx == 0 else met[2])
    else:
        for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
            put(ov, x, y, GOLD_SOFT[2])
    det = t.layer('Details: Münzen')
    for x, y in ((5, 5), (8, 6), (6, 8), (9, 9)) if v == 'B1' else ((6, 6), (8, 8)):
        soft_coin(det, x, y)
    res['sales_storage_intake'] = t
    # back
    t = Tex()
    soft_planks(t.layer('Basis: Bretter'), 13)
    ov = t.layer('Overlay: Bänder')
    for i in range(16):
        if v == 'B1':
            put(ov, i, 0, met[4])
            put(ov, 0, i, met[3])
            put(ov, i, 15, met[1])
            put(ov, 15, i, met[1])
        else:
            put(ov, i, 0, beam[3])
            put(ov, 0, i, beam[3])
            put(ov, i, 15, beam[0])
            put(ov, 15, i, beam[0])
    for y in (4, 11):
        for x in range(1, 15):
            if v == 'B1':
                put(ov, x, y, met[3])
                put(ov, x, y + 1, met[1])
            else:
                put(ov, x, y, beam[2])
                put(ov, x, y + 1, beam[1])
    det = t.layer('Details: Schloss')
    lock = GOLD_SOFT if v == 'B1' else met
    for y in range(6, 11):
        for x in range(6, 10):
            put(det, x, y, lock[3] if (x == 6 or y == 6) else lock[2] if (x < 9 and y < 10) else lock[1])
    put(det, 7, 8, MOUTH[1])
    put(det, 8, 8, MOUTH[1])
    put(det, 7, 9, MOUTH[2])
    res['sales_storage_back'] = t
    return res


def storage_table_soft(v):
    """Storage Crafting Table in the same soft spruce: B1 the B layout (3 x 3 top grid, drawers, hoop + saw) soft;
    B2 the top like the Vanilla crafting table (a frame and a single cross of grooves, tools-free), brass knobs."""
    res = {}
    w = SPRUCE
    met = IRON_SOFT
    t = Tex()
    soft_planks(t.layer('Basis: Bretter'), 21)
    ov = t.layer('Overlay: Raster')
    for i in range(16):
        put(ov, i, 0, w[6])
        put(ov, 0, i, w[5])
        put(ov, i, 15, w[1])
        put(ov, 15, i, w[1])
    for y in range(1, 15):
        for x in range(1, 15):
            if v == 'B1':
                if x in (5, 10) or y in (5, 10):
                    put(ov, x, y, w[2])
                elif x in (6, 11) or y in (6, 11):
                    put(ov, x, y, w[5]) if (x in (6, 11) and y not in (5, 10)) or (y in (6, 11) and x not in (5, 10)) else None
            else:
                if x in (2, 13) or y in (2, 13):
                    if 2 <= x <= 13 and 2 <= y <= 13:
                        put(ov, x, y, w[2] if (x == 13 or y == 13) else w[3])
                elif x in (7, 8) and 2 < y < 13:
                    put(ov, x, y, w[3] if x == 7 else w[5])
                elif y in (7, 8) and 2 < x < 13:
                    put(ov, x, y, w[3] if y == 7 else w[5])
    det = t.layer('Details: Ecken')
    for cx, cy, sx, sy in ((0, 0, 1, 1), (15, 0, -1, 1), (0, 15, 1, -1), (15, 15, -1, -1)):
        put(det, cx, cy, met[4] if cx + cy < 15 else met[2])
        put(det, cx + sx, cy, met[3] if cy == 0 else met[1])
        put(det, cx, cy + sy, met[3] if cx == 0 else met[1])
    res['storage_crafting_table_top'] = t
    # front: table lip, two drawers
    t = Tex()
    soft_planks(t.layer('Basis: Bretter'), 22)
    ov = t.layer('Overlay: Schubladen')
    for x in range(16):
        put(ov, x, 0, w[6])
        put(ov, x, 1, w[5])
        put(ov, x, 2, w[4])
        put(ov, x, 3, w[1])
    for y0 in (5, 10):
        for y in range(y0, y0 + 4):
            for x in range(2, 14):
                edge = x in (2, 13) or y in (y0, y0 + 3)
                if edge:
                    c = w[5] if (x == 2 or y == y0) else w[2]
                else:
                    c = w[4] if v == 'B1' else ramp(w, 0.62 - 0.12 * (y - y0) / 3)
                put(ov, x, y, c)
    det = t.layer('Details: Griffe')
    knob = GOLD_SOFT
    for y0 in (5, 10):
        put(det, 7, y0 + 1, knob[3])
        put(det, 8, y0 + 1, knob[2])
        put(det, 7, y0 + 2, knob[2])
        put(det, 8, y0 + 2, knob[1])
    for y in range(16):
        put(det, 0, y, w[5] if v == 'B2' else (met[3] if y > 3 else met[4]))
        put(det, 15, y, w[1] if v == 'B2' else met[1])
    res['storage_crafting_table_front'] = t
    # side: lip, staves, hoop, saw (B1) / hanging hand saw + square (B2)
    t = Tex()
    soft_planks(t.layer('Basis: Dauben'), 23, vertical=True)
    ov = t.layer('Overlay: Lippe + Reifen')
    for x in range(16):
        put(ov, x, 0, w[6])
        put(ov, x, 1, w[5])
        put(ov, x, 2, w[4])
        put(ov, x, 3, w[1])
        put(ov, x, 11, met[3] if v == 'B1' else w[2])
        put(ov, x, 12, met[1] if v == 'B1' else w[1])
    det = t.layer('Details: Säge')
    for x in range(3, 10):
        put(det, x, 5, met[5] if x == 3 else met[4])
        put(det, x, 6, met[3])
        if x % 2 == 1:
            put(det, x, 7, met[2])
    for x, y in ((10, 5), (11, 5), (12, 5), (11, 6), (12, 6), (11, 7), (12, 7)):
        put(det, x, y, w[1] if y == 7 or x == 12 else w[2])
    res['storage_crafting_table_side'] = t
    return res


# ---------------------------------------------------------------- 3) backpack item, turned clockwise like the bundle

LEATHER_SOFT = [H('4a2414'), H('5e2f1b'), H('733b22'), H('88492a'), H('9b5832'), H('ad683d'), H('bd7a4b'), H('cb8d5c')]
BUCKLE = [H('6e6a62'), H('8c877d'), H('aaa599'), H('c4bfb2')]

BACKPACKS = {
    # front width, side width, lean steps (rows from the top that move one pixel to the right), extras
    'D': dict(fw=8, sw=3, lean=(), pocket='front', title='D – Dreiviertel, aufrecht',
              note='um die senkrechte Achse im Uhrzeigersinn gedreht: Front links, dunkleres Seitenteil rechts mit Riemen; '
                   'Klappe mit Schnalle, Riemen bis zur Fronttasche, große Griffschlaufe.'),
    'E': dict(fw=8, sw=3, lean=(4, 5, 6, 7), pocket='front', title='E – Dreiviertel, schräg wie das Bündel (Empfehlung)',
              note='wie D, die obere Hälfte einen Pixel nach rechts geneigt wie das Vanilla-Bündel, Schlaufe schräg.'),
    'F': dict(fw=7, sw=4, lean=(3, 4, 5, 6, 7, 8), pocket='side', title='F – stärker gedreht, Seitentasche',
              note='breiteres Seitenteil mit eigener Tasche, schmalere Front mit zwei Klappenriemen, stärker geneigt.'),
}


def backpack(v):
    spec = BACKPACKS[v]
    fw, sw = spec['fw'], spec['sw']
    top, bot = 4, 14
    fx0 = 8 - (fw + sw) // 2 - 1

    def shift(y):
        return 1 if y in spec['lean'] else 0
    front, side = {}, {}
    for y in range(top, bot + 1):
        s = shift(y)
        a, b = fx0 + s, fx0 + fw - 1 + s
        if y in (top, bot):
            a += 1
        for x in range(a, b + 1):
            front[(x, y)] = ((x - a) / max(1, b - a), (y - top) / (bot - top))
        if top < y < bot:
            sa, sb = b + 1, b + sw
            if y in (top + 1, bot - 1):
                sb -= 1
            for x in range(sa, sb + 1):
                side[(x, y)] = ((x - sa) / max(1, sb - sa), (y - top) / (bot - top))
    body = set(front) | set(side)
    t = Tex()
    base = t.layer('Basis: Leder (Front + Seitenteil)')
    out = mix(LEATHER_SOFT[0], LEATHER_SOFT[1], 0.3)
    for (x, y), (u, w) in front.items():
        c = ramp(LEATHER_SOFT[3:], 0.8 - 0.45 * w - 0.2 * u)
        if (x + 1, y) in side:
            c = mix(c, LEATHER_SOFT[7], 0.35)                     # the turned corner catches the light
        put(base, x, y, c)
    for (x, y), (u, w) in side.items():
        put(base, x, y, ramp(LEATHER_SOFT[1:5], 0.6 - 0.35 * w - 0.25 * u))
    ring = set()
    for (x, y) in body:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if q not in body:
                ring.add(q)
    for (x, y) in ring:
        put(base, x, y, out)
    ov = t.layer('Overlay: Klappe + Tasche')
    flap_bot = top + 4
    for (x, y), (u, w) in front.items():
        if y < flap_bot or (y == flap_bot and 0.1 < u < 0.9):
            c = ramp(LEATHER_SOFT[4:], 0.95 - 0.55 * (y - top) / 4 - 0.15 * u)
            if y == flap_bot or (y == flap_bot - 1 and not 0.1 < u < 0.9):
                c = LEATHER_SOFT[3]
            put(ov, x, y, c)
        elif y == flap_bot + 1 and 0.1 < u < 0.9:
            put(ov, x, y, mix(rgb(base, x, y), LEATHER_SOFT[2], 0.5))     # soft shadow under the flap
    for (x, y), (u, w) in side.items():
        if y < flap_bot:
            put(ov, x, y, ramp(LEATHER_SOFT[2:6], 0.7 - 0.45 * (y - top) / 4))
    if spec['pocket'] == 'front':
        py = bot - 4
        for (x, y), (u, w) in front.items():
            if py <= y <= bot - 1 and 0.15 <= u <= 0.85:
                c = LEATHER_SOFT[6] if y == py else ramp(LEATHER_SOFT[3:6], 0.75 - 0.35 * (y - py) / 3)
                if y == bot - 1 or u > 0.8:
                    c = LEATHER_SOFT[3]
                put(ov, x, y, c)
    else:
        py = bot - 5
        for (x, y), (u, w) in side.items():
            if py <= y <= bot - 2 and u >= 0.2:
                put(ov, x, y, LEATHER_SOFT[5] if y == py else ramp(LEATHER_SOFT[2:5], 0.7 - 0.3 * (y - py) / 3))
    det = t.layer('Details: Riemen, Schnalle, Griff')
    hx = fx0 + fw // 2 - 2 + shift(top)
    for x, y in ((hx, top - 1), (hx, top - 2), (hx + 1, top - 3), (hx + 2, top - 3), (hx + 3, top - 2), (hx + 3, top - 1)):
        put(det, x + (1 if y == top - 3 and spec['lean'] else 0), y, LEATHER_SOFT[4] if y == top - 3 else LEATHER_SOFT[2])
    straps = [0.5] if spec['pocket'] == 'front' else [0.2, 0.8]
    for su in straps:
        row = sorted(x for (x, y) in front if y == flap_bot + 2)
        bx = row[min(len(row) - 1, round(su * (len(row) - 1)))]
        for y in range(flap_bot - 1, bot - (4 if spec['pocket'] == 'front' else 1)):
            if (bx - shift(flap_bot + 2) + shift(y), y) in front:
                put(det, bx - shift(flap_bot + 2) + shift(y), y, LEATHER_SOFT[2])
        bxx = bx - shift(flap_bot + 2) + shift(flap_bot + 1)
        put(det, bxx, flap_bot + 1, BUCKLE[3])
        put(det, bxx, flap_bot + 2, BUCKLE[1])
    if side:
        cols = sorted({x for (x, y) in side if y == bot - 3})
        sx0 = cols[len(cols) // 2]
        for y in range(top + 1, bot - 1):
            sx = sx0 - shift(bot - 3) + shift(y)
            if (sx, y) in side and not (spec['pocket'] == 'side' and y >= bot - 5):
                put(det, sx, y, LEATHER_SOFT[1])
        sx = sx0 - shift(bot - 3) + shift(top + 5)
        if (sx, top + 5) in side:
            put(det, sx, top + 5, BUCKLE[2])
    return t


# ---------------------------------------------------------------- 4) butter inner (candidates only)

BUTTER = [H('ecd06a'), H('f2da78'), H('f6e286'), H('f9e894'), H('fbeea6'), H('fdf3b8'), H('fff7ca')]


def smooth_noise(seed, cell=4):
    """Tileable value noise on a 16 px period, smoothstep-interpolated (no hard steps)."""
    rnd = random.Random(seed)
    n = 16 // cell
    g = [[rnd.random() for _ in range(n)] for _ in range(n)]

    def f(x, y):
        gx, gy = x / cell, y / cell
        x0, y0 = int(gx) % n, int(gy) % n
        x1, y1 = (x0 + 1) % n, (y0 + 1) % n
        tx, ty = gx - int(gx), gy - int(gy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = g[y0][x0] + (g[y0][x1] - g[y0][x0]) * tx
        b = g[y1][x0] + (g[y1][x1] - g[y1][x0]) * tx
        return a + (b - a) * ty
    return f


def butter_inner(v):
    t = Tex()
    base = t.layer('Basis: Butter (weicher Verlauf)')
    f1, f2 = smooth_noise(41, 8), smooth_noise(42, 4)
    for y in range(16):
        for x in range(16):
            n = 0.65 * f1(x, y) + 0.35 * f2(x, y)
            if v == 'Z':
                n = 0.5 + (n - 0.5) * 0.35                   # nearly flat like the cake inside
            put(base, x, y, ramp(BUTTER[1:6], n))
    det = t.layer('Details')
    if v == 'X':      # creamy: a few soft light flecks where the noise peaks
        for x, y in ((3, 2), (4, 2), (11, 5), (6, 10), (7, 10), (13, 12), (1, 13)):
            put(det, x, y, mix(BUTTER[6], rgb(base, x, y), 0.4))
    elif v == 'Y':    # a soft knife pass: two gently waving streaks one tone lighter, their lower edge one tone darker
        for y0, x0, ln, ph in ((4, 1, 10, 0.0), (11, 6, 9, 1.7)):
            for k in range(ln):
                x = (x0 + k) % 16
                y = y0 + round(0.6 * math.sin(ph + k * 0.7))
                put(det, x, y, mix(BUTTER[6], rgb(base, x, y), 0.35))
                if k % 3:
                    put(det, x, y + 1, mix(BUTTER[1], rgb(base, x, y + 1), 0.6))
    else:             # Z: like Vanilla's cake inside - flat, sparse small crumb-like clusters one step up/down
        rnd = random.Random(7)
        for _ in range(9):
            x, y = rnd.randrange(16), rnd.randrange(16)
            light = rnd.random() < 0.5
            c = BUTTER[5] if light else BUTTER[2]
            put(det, x, y, c)
            if rnd.random() < 0.6:
                put(det, (x + 1) % 16, y, mix(c, rgb(base, (x + 1) % 16, y), 0.5))
    return t


BUTTERS = {'X': 'cremig – weicher Verlauf, einzelne helle Flocken', 'Y': 'weicher Messerzug – zwei sanfte Streifen',
           'Z': 'Vanilla-nah – fast flach wie das Kucheninnere, wenige Krümel'}


# ---------------------------------------------------------------- 5) Piggy effect (frontal head, pupils outside)

PIG = {'K': H('6a3a3c'), 'd': H('c06e72'), 'p': H('dc8c8e'), 'L': H('eea4a2'), 'H': H('f7c0bc'), 'S': H('e4878c'),
       's': H('cc7078'), 'n': H('8a4a50'), 'W': H('f2eeee'), 'B': H('2a2022'), 'r': H('ef8f94'), 'm': H('9a5056')}
PIGGIES = {
    'P1': ('Vanilla-nah, frontal', ['................',
                                     '................',
                                     '..KKKKKKKKKKKK..',
                                     '.KLHLLLLLLLLLpK.',
                                     '.KHLLLLLLLLLLpK.',
                                     '.KLLLLLLLLLLLpK.',
                                     '.KBWLLLLLLLLWBK.',
                                     '.KLLLLLLLLLLLpK.',
                                     '.KLLLSSSSSSLLpK.',
                                     '.KLLLSnSSnSLLpK.',
                                     '.KLLLSnSSnSLLpK.',
                                     '.KpLLssssssLppK.',
                                     '.KppLLLLLLLLpdK.',
                                     '.KdpppppppppddK.',
                                     '..KKKKKKKKKKKK..',
                                     '................']),
    'P2': ('lustig: Schlappohren, Riesenrüssel, Grinsen (Empfehlung)', ['................',
                                                                         '.KK..........KK.',
                                                                         'KdpK........KpdK',
                                                                         'KppdKKKKKKKKdppK',
                                                                         '.KKLHLLLLLLLLKK.',
                                                                         '.KLHLLLLLLLLLpK.',
                                                                         '.KBWLLLLLLLLWBK.',
                                                                         '.KBWLLLLLLLLWBK.',
                                                                         '.KrLSSSSSSSSLrK.',
                                                                         '.KLSHSSSSSSSSpK.',
                                                                         '.KLSSnnSSnnSSpK.',
                                                                         '.KpSSnnSSnnSSpK.',
                                                                         '.KppssssssssppK.',
                                                                         '.KdpmpppppmppdK.',
                                                                         '..KKdmmmmmmdKK..',
                                                                         '....KKKKKKKK....']),
    'P3': ('lustig: schielend, Zunge', ['................',
                                       '..KK........KK..',
                                       '.KdpK......KpdK.',
                                       '.KppKKKKKKKKppK.',
                                       '.KLHLLLLLLLLLpK.',
                                       '.KHLLLLLLLLLLpK.',
                                       '.KLBWLLLLLLWBpK.',
                                       '.KLLLLLLLLLLLpK.',
                                       '.KLLSSSSSSSSLpK.',
                                       '.KLLSnSSSSnSLpK.',
                                       '.KLLSnSSSSnSLpK.',
                                       '.KpLLssssssLppK.',
                                       '.KpppmmmmmppddK.',
                                       '.KdppppprrpdddK.',
                                       '..KKKKKKrrKKKK..',
                                       '................']),
}


def piggy(v):
    rows = PIGGIES[v][1]
    t = Tex()
    lay = {'Basis: Kopf': t.layer('Basis: Kopf'), 'Overlay: Rüssel': t.layer('Overlay: Rüssel'),
           'Details: Augen, Mund': t.layer('Details: Augen, Mund')}
    for y, r in enumerate(rows):
        assert len(r) == 16, (v, y, r)
        for x, ch in enumerate(r):
            if ch == '.':
                continue
            name = 'Overlay: Rüssel' if ch in 'Ssn' else 'Details: Augen, Mund' if ch in 'WBmr' else 'Basis: Kopf'
            put(lay[name], x, y, PIG[ch])
    return t


# ---------------------------------------------------------------- 6) animations A1 (style A, built in)

ANIM_MCMETA = m.ANIM
A1 = {
    # texture: (glow kind, glow colour at the peak, amount) - the amounts of the reviewed preview (misctex23)
    'dimensional_scrap_side': ('cyan', H('e8fffd'), 0.45), 'dimensional_scrap_top': ('cyan', H('e8fffd'), 0.45),
    'nether_dimensional_scrap_side': ('pink', H('f2e0ff'), 0.45), 'nether_dimensional_scrap_top': ('pink', H('f2e0ff'), 0.45),
    'end_dimensional_scrap_side': ('pink', H('f2e0ff'), 0.45), 'end_dimensional_scrap_top': ('pink', H('f2e0ff'), 0.45),
    'deepslate_sage_ore': ('green', H('f4ffc8'), 0.35),
    'crimson_blazewood_planks': ('ember', H('ffb04a'), 0.28), 'warped_blazewood_planks': ('ember', H('ffb04a'), 0.28),
    'crimson_blazewood_bookshelf': ('crack', H('a1511d'), 0.55), 'warped_blazewood_bookshelf': ('crack', H('a1511d'), 0.55),
}
A1_BASE = 'Basis: abgenommene Textur'


def a1_source(name):
    """The accepted still texture: the PNG itself while it is 16 x 16, afterwards the first frame of the strip's base
    layer (the base layer repeats the accepted texture in every frame), so --write stays idempotent."""
    path = OVERLAY / 'block' / f'{name}.png'
    im = load(path)
    if im.height == 16:
        return im
    doc = json.loads(L.layers_path(path).read_text(encoding='utf-8'))
    layer = next(lay for lay in doc['layers'] if lay['name'] == A1_BASE)
    px = L.layer_pixels(doc, layer)
    out = Image.new('RGBA', (16, 16))
    for i in range(256):
        out.putpixel((i % 16, i // 16), tuple(px[i]) if px[i] else (0, 0, 0, 0))
    return out


def a1_strip(name):
    kind, hot, amount = A1[name]
    tex, strip = m.animate(a1_source(name), kind, 'A', amount, hot)
    return tex, strip


# ---------------------------------------------------------------- write / check

SHOP_TABLE_CHOICE = 'B2'
PIGGY_CHOICE = 'P2'


def built():
    """repo path -> (Tex, mcmeta or None) for everything this generator ships (the leaves go through gen_textures)."""
    out = {}
    for n, t in sales_storage_soft(SHOP_TABLE_CHOICE).items():
        out[SHOPS / f'{n}.png'] = (t, None)
    for n, t in storage_table_soft(SHOP_TABLE_CHOICE).items():
        out[STORAGE / 'block' / f'{n}.png'] = (t, None)
    out[FUN / 'piggy_effect.png'] = (piggy(PIGGY_CHOICE), None)
    for n in A1:
        out[OVERLAY / 'block' / f'{n}.png'] = (a1_strip(n)[0], ANIM_MCMETA)
    return out


def leaves_gen():
    sys.path.insert(0, str(END_TOOLS))
    import gen_textures as g  # noqa: E402
    s = next(w for w in g.WOODS if w['name'] == 'ashwood')
    return g, g.leaves_loose(s), g.TEX / 'block' / 'ashwood_leaves_loose.png'


def write_all(check=False):
    stale = []
    for path, (tex, meta) in built().items():
        if not write_tex(path, tex, meta, check):
            stale.append(path)
    g, im, path = leaves_gen()
    if check:
        if not path.exists() or load(path).tobytes() != im.tobytes():
            stale.append(path)
    else:
        g.write(im, path, [])
    return stale


# ---------------------------------------------------------------- preview page

def fig(im, scale, cap, cls=''):
    src = m.b64(im, scale) if not isinstance(im, str) else im
    return (f'<figure class="{cls}"><img src="{src}" alt="{html.escape(cap)}">'
            f'<figcaption>{html.escape(cap)}</figcaption></figure>')


def card(title, figs, note='', key=None, value=None, rec=False):
    head = html.escape(title)
    if key:
        head = (f'<label><input type="radio" name="{key}" value="{value}"{" checked" if rec else ""}> {head}'
                f'{" <b>(Empfehlung)</b>" if rec else ""}</label>')
    n = f'<p class="note">{html.escape(note)}</p>' if note else ''
    return f'<div class="card"><h3>{head}</h3>{n}<div class="row">{"".join(figs)}</div></div>'


def section(title, tag, note, cards, extra=''):
    cls = 'in' if tag == 'eingebaut' else 'cand'
    return (f'<section><h2>{html.escape(title)} <span class="tag {cls}">{tag}</span></h2>'
            f'<p class="note">{html.escape(note)}</p><div class="grid">{"".join(cards)}</div>{extra}</section>')


def on_grey(im):
    o = Image.new('RGBA', im.size, (139, 139, 139, 255))
    o.alpha_composite(im)
    return o


def rotations(im):
    o = Image.new('RGBA', (16 * 4 + 6 * 3, 16), (139, 139, 139, 255))
    r = im
    for i in range(4):
        o.alpha_composite(r, (i * 22, 0))
        r = r.rotate(90)
    return o


def page(path, jar_path, j264):
    jar = zipfile.ZipFile(jar_path)

    def van(p):
        try:
            return Image.open(io.BytesIO(jar.read(f'assets/minecraft/textures/{p}.png'))).convert('RGBA')
        except KeyError:
            return None
    note = van('block/note_block')
    j_side = load(j264 / 'jukebox_side.png') if (j264 / 'jukebox_side.png').exists() else None
    mats = materials()
    vfig = [fig(note, 8, 'Vanilla-Notenblock 26.3')] + ([fig(j_side, 8, 'Jukebox 26.4 (Seite)')] if j_side else [])
    built_s, open_s = [], []

    # 1) amplifiers
    amps = amplifier_textures(note)
    old = m.t10(van, j264)[1] if j_side else None
    figs = [fig(amps['jukebox_amplifier_side'], 8, 'Jukebox-Verstärker'), fig(amps['note_amplifier_side'], 8, 'Noten-Verstärker (60 %)'),
            fig(amps['note_block_framed'], 8, 'Notenblock (nur mit Option)')]
    ctx = [fig(m.tile(amps['jukebox_amplifier_side']), 4, '3×3 Jukebox-Verst.'), fig(m.tile(amps['note_block_framed']), 4, '3×3 Notenblock'),
           fig(m.cube(amps['jukebox_amplifier_side'], amps['jukebox_amplifier_side']), 1, 'Block'),
           fig(m.cube(amps['note_amplifier_side'], amps['note_amplifier_side']), 1, 'Noten-Verst. Block'),
           fig(rotations(amps['jukebox_amplifier_side']), 5, '0° / 90° / 180° / 270° – gleich')]
    olds = []
    if old:
        olds = [fig(old['jukebox_amplifier_side'], 6, 'Jukebox-Verst. alt'), fig(old['note_amplifier_side'], 6, 'Noten-Verst. alt')]
    built_s.append(section('Notenblock + Verstärker: Rahmen rundum (Variante A)', 'eingebaut',
                           'Jukebox-Rahmen einmal rundum (Bretter, Fuge, Innenkante), exakt 90°-drehsymmetrisch; innen das Notenblock-Gitter '
                           '(180°-symmetrisch, diagonale Reihen wie Vanilla), Material radial in den Löchern: Astralit (Jukebox-Verstärker), '
                           'Nihilit schwächer (Noten-Verstärker: 60 % Relief, Material 40 % zurück zum Holz). Eine Textur für alle 6 Seiten. '
                           'Der Vanilla-Notenblock ändert sich nur mit der neuen Option „Notenblock mit Rahmen“ (Standard aus) – '
                           'Vanilla-Blöcke nicht ungefragt ändern, Ressourcenpakete gewinnen.',
                           [card('eingebaut', figs), card('Kontext', ctx), card('vorher / Vanilla', olds + vfig)]))
    cards = []
    for v, d in (('A', '4 Ringe, 8×8-Gitter'), ('B', '3 Ringe, 10×10-Gitter (mehr Notenblock)'), ('C', 'A + Nieten in den Ecken + Membran-Kern')):
        ims = [framed(note, v, mats['astralit'], 1.0).image(), framed(note, v, mats['nihilith'], NOTE_STRENGTH, 0.4).image(),
               framed(note, v, None, NOTE_STRENGTH).image()]
        cards.append(card(f'{v} – {d}', [fig(ims[0], 6, 'Jukebox-V.'), fig(ims[1], 6, 'Noten-V.'), fig(ims[2], 6, 'Notenblock'),
                                          fig(m.tile(ims[0]), 3, '3×3')], key='Rahmen', value=v, rec=v == 'A'))
    open_s.append(section('Rahmen-Varianten (A eingebaut, B/C zur Wahl)', 'Wahl offen', 'Alle drei 90°-drehsymmetrisch; Notenblock und Noten-Verstärker schwächer.', cards))

    # 2) sales storage + table
    faces = ('sales_storage_side', 'sales_storage_intake', 'sales_storage_back', 'storage_crafting_table_top',
             'storage_crafting_table_front', 'storage_crafting_table_side')
    names = {'sales_storage_side': 'Lager Seite', 'sales_storage_intake': 'Lager Einlass', 'sales_storage_back': 'Lager hinten',
             'storage_crafting_table_top': 'Werkbank oben', 'storage_crafting_table_front': 'Werkbank vorn', 'storage_crafting_table_side': 'Werkbank Seite'}
    prevB = Path('/root/sb-tools-a/previews/misctex23')
    sets = {v: {**sales_storage_soft(v), **storage_table_soft(v)} for v in ('B1', 'B2')}

    def setfigs(s, scale=6):
        ims = {n: s[n].image() for n in faces}
        return [fig(ims[n], scale, names[n]) for n in faces] + [
            fig(m.cube(ims['sales_storage_side'], ims['sales_storage_intake']), 1, 'Verkaufslager'),
            fig(m.cube(ims['storage_crafting_table_side'], ims['storage_crafting_table_top'], ims['storage_crafting_table_front']), 1, 'Lager-Werkbank')]
    old_b = [fig(load(prevB / f'{n}_B.png'), 4, f'B (23.10.) {names[n]}') for n in faces if (prevB / f'{n}_B.png').exists()]
    vv = [fig(van(f'block/{n}'), 4, f'Vanilla {n}') for n in ('crafting_table_top', 'crafting_table_front', 'barrel_side') if van(f'block/{n}')]
    built_s.append(section('Verkaufslager + Lager-Werkbank: B2 (weicher)', 'eingebaut',
                           'Fichte/Fass-Holz mit gleichmäßigen Stufen; statt heller Eisenbänder dunklere Holzleisten, nur kleine Eisen-Eckbleche; '
                           'Einlass/Münzschacht als warmer dunkler Verlauf statt Schwarz; weiches Gold. Werkbank oben wie die Vanilla-Werkbank '
                           '(Rahmen + ein Kreuz), Schubladen mit Verlauf, Messingknöpfe.',
                           [card('B2 eingebaut', setfigs(sets['B2'])),
                            card('3×3', [fig(m.tile(sets['B2']['sales_storage_side'].image()), 3, 'Lager Seite'),
                                         fig(m.tile(sets['B2']['storage_crafting_table_front'].image()), 3, 'Werkbank vorn')]),
                            card('vorher: B (23.10.) / Vanilla', old_b + vv)]))
    open_s.append(section('Verkaufslager + Lager-Werkbank: B1 statt B2?', 'Wahl offen',
                          'B1 = Form von B (Eisenkragen, Bänder, Säge mit Reifen) nur in weichen Tönen – falls die Holzleisten von B2 zu schlicht sind.',
                          [card('B1', setfigs(sets['B1']), key='Lager', value='B1'),
                           card('B2 (eingebaut)', setfigs(sets['B2'], 4), key='Lager', value='B2', rec=True)]))

    # 3) A1 animations
    cards = []
    for n in A1:
        tex, strip = a1_strip(n)
        cards.append(card(n.replace('_', ' '), [fig(a1_source(n), 6, 'Ruhebild (abgenommen)'), fig(m.gif(strip, 6), 1, 'animiert'),
                                                 fig(m.gif(strip, 2, wall=True), 1, '3×3')]))
    built_s.append(section('Animationen A1 (Stil A „atmen“)', 'eingebaut',
                           '16 Bilder, interpoliert, 3,2 s je Runde (.png.mcmeta). Die abgenommene Textur bleibt das Ruhebild, nur die Leuchtpixel '
                           '(Risse/Kristalle/Erz/Glutfugen) werden je Bereich sanft heller und dunkler. Regale: die Unterkanten der Regalbretter '
                           'glühen kurz auf. Alle Geschwister: Scraps (Ober-/Nether-/End-Welt, Seite + oben), Tiefenschiefer-Salbei-Erz, '
                           'Karmesin-/Wirr-Lohenholz-Bretter und -Regale (Treppen/Stufen nutzen die Bretter mit).', cards))

    # 4) Piggy
    pig_old = load(FUN / 'piggy_effect.png') if load(FUN / 'piggy_effect.png').height == 16 else None
    cards = []
    for v, (d, _) in PIGGIES.items():
        im = piggy(v).image()
        cards.append(card(f'{v} – {d}', [fig(im, 8, '16×'), fig(on_grey(im), 2, 'Effektleiste 2×'), fig(on_grey(im), 1, '1×')],
                          key='Piggy', value=v, rec=v == PIGGY_CHOICE))
    vp = [fig(van(f'mob_effect/{n}'), 4, f'Vanilla {n}') for n in ('speed', 'hunger', 'regeneration') if van(f'mob_effect/{n}')]
    pig_face = van('entity/pig/pig_temperate') or van('entity/pig/temperate_pig') or van('entity/pig/pig')
    if pig_face:
        vp.append(fig(pig_face.crop((8, 8, 16, 16)), 8, 'Vanilla-Schwein Gesicht'))
    built_s.append(section(f'Piggy-Effekt: {PIGGY_CHOICE} (vorläufig)', 'eingebaut',
                           'Kopf frontal, Vanilla-näher (eckiger Kopf, Rüssel unten mittig, Nasenlöcher). Fehler behoben: Pupillen (schwarz) außen, '
                           'Weiß innen – wie beim Vanilla-Schwein. Lustiger: Schlappohren, großer Rüssel, Grinsen, rote Bäckchen. Weiche Kontur (Dunkelrosa statt Schwarz).',
                           [card(f'{PIGGY_CHOICE} eingebaut', [fig(piggy(PIGGY_CHOICE).image(), 8, '16×')]),
                            card('vorher / Vanilla', ([fig(pig_old, 6, 'heute (Branch-Basis)')] if pig_old else []) + vp)]))
    open_s.append(section('Piggy-Effekt: Varianten', 'Wahl offen', 'P2 ist vorläufig eingebaut.', cards))

    # 5) leaves L1
    sys.path.insert(0, str(END_TOOLS))
    import gen_textures as g  # noqa: E402
    import tx22_textures as tx  # noqa: E402
    s = next(w for w in g.WOODS if w['name'] == 'ashwood')
    l1 = g.leaves_loose(s)
    old_l = g.leaf_clumps(s, 55, 0.62, 1.3, False, 2)
    med, dense = g.leaves(s), g.leaves_inner(s)
    share = sum(1 for y in range(16) for x in range(16) if l1.getpixel((x, y))[3]) / 256
    built_s.append(section('Aschholz-Laub locker: L1', 'eingebaut', f'Lockerste Laubstufe nur für Aschholz: {share:.0%} deckend (vorher ~46 %); Dämmerholz bleibt.',
                           [card('L1 eingebaut', [fig(g.tinted(l1, tx.TINT), 8, 'getönt'), fig(tx.leaf_wall(l1, med, dense), 3, 'Kronen-Schnitt')]),
                            card('vorher', [fig(g.tinted(old_l, tx.TINT), 6, '~46 %'), fig(tx.leaf_wall(old_l, med, dense), 2, 'Kronen-Schnitt')])]))

    # 6) backpack
    cards = []
    for v, spec in BACKPACKS.items():
        im = backpack(v).image()
        slot = Image.new('RGBA', (52, 18), (139, 139, 139, 255))
        bun = van('item/bundle')
        if bun:
            slot.alpha_composite(bun, (1, 1))
        slot.alpha_composite(im, (18, 1))
        slot.alpha_composite(load(STORAGE / 'item/backpack.png'), (35, 1))
        cards.append(card(spec['title'], [fig(im, 8, '16×'), fig(slot, 3, 'Slot: Bündel | neu | heute')], spec['note'],
                          key='Rucksack', value=v, rec=v == 'E'))
    refs = [fig(load(prevB / 'backpack_B.png'), 6, 'B (23.10.)')] if (prevB / 'backpack_B.png').exists() else []
    if van('item/bundle'):
        refs.append(fig(van('item/bundle'), 6, 'Vanilla-Bündel'))
    cards.append(card('Bezug', refs))
    open_s.append(section('Rucksack: B verfeinert, gedreht wie das Bündel', 'Wahl offen',
                          'Motiv um die senkrechte Achse im Uhrzeigersinn gedreht (Front links, Seitenteil rechts sichtbar), weicheres Leder '
                          '(8 Töne, Kontur Dunkelbraun statt Fast-Schwarz), Schnalle gedämpft. Nur Vorschau; nach der Wahl folgen verstärkt/Netherit/'
                          'Enderit + gefärbte Fassung in derselben Form.', cards))

    # 7) butter
    cur = load(SAN / 'butter_block_inner.png')
    side = load(SAN / 'butter_block_side.png')
    cards = [card('heute (bleibt bis zur Wahl)', [fig(cur, 6, 'innen'), fig(m.cube(side, cur), 1, 'Schnitt oben')])]
    for v, d in BUTTERS.items():
        im = butter_inner(v).image()
        cards.append(card(f'{v} – {d}', [fig(im, 6, '16×'), fig(m.cube(side, im), 1, 'Schnitt oben'), fig(m.tile(im), 2, '3×3')],
                          key='Butter', value=v, rec=v == 'X'))
    vb = [fig(van(f'block/{n}'), 4, f'Vanilla {n}') for n in ('cake_inner', 'cake_top') if van(f'block/{n}')]
    cards.append(card('Vanilla-Bezug', vb))
    open_s.append(section('Butter innen: drei weiche Versuche', 'Wahl offen',
                          'Vanilla-nah, cremig, weich: weicher Verlauf aus 5 nahen Buttertönen statt Rauschen, keine harten Streifen.', cards))

    css = '''
:root{--bg:#f6f5f2;--fg:#1d1d1f;--mut:#5c5c62;--card:#fff;--line:#d9d7d2;--in:#1f7a3a;--cand:#9a5b00}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:#18181b;--fg:#ececee;--mut:#a3a3ab;--card:#232327;--line:#3a3a40;--in:#5fd38a;--cand:#f0b45a}}
:root[data-theme="dark"]{--bg:#18181b;--fg:#ececee;--mut:#a3a3ab;--card:#232327;--line:#3a3a40;--in:#5fd38a;--cand:#f0b45a}
body{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif}
main{max-width:1240px;margin:auto} h1{font-size:1.4rem} h2{font-size:1.15rem;margin:0 0 6px} h3{font-size:1rem;margin:0 0 6px}
.part{font-size:1.25rem;margin:28px 0 4px;border-bottom:2px solid var(--line)}
section{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:12px;margin:14px 0;min-width:0}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:12px}
.card{border:1px solid var(--line);border-radius:8px;padding:10px;min-width:0}
.row{display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;margin:6px 0}
figure{margin:0;text-align:center;max-width:100%} figcaption{font-size:12px;color:var(--mut)}
figure img{image-rendering:pixelated;max-width:100%;height:auto;background:#8b8b8b;display:block;margin:auto}
.note{color:var(--mut);margin:4px 0 8px}
.tag{font-size:12px;padding:2px 6px;border-radius:6px;border:1px solid} .in{color:var(--in)} .cand{color:var(--cand)}
#code{position:sticky;top:0;z-index:2;background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px;display:flex;gap:8px;flex-wrap:wrap;align-items:center}
button{font:inherit;padding:4px 10px} code{font-size:14px;overflow-wrap:anywhere}
'''
    js = '''
function upd(){const k=['Rahmen','Lager','Piggy','Rucksack','Butter'];const p=k.map(n=>{const r=document.querySelector('input[name="'+n+'"]:checked');return n+'='+(r?r.value:'?')});
document.getElementById('c').textContent='W23-4 '+p.join(' ')}
document.addEventListener('change',upd);upd();
document.getElementById('cp').onclick=()=>{const t=document.getElementById('c').textContent;try{navigator.clipboard.writeText(t)}catch(e){}};
'''
    body = (f'<h1>W23-4 Texturen – Notenblock/Verstärker, Lager, Animationen, Piggy, Laub, Rucksack, Butter</h1>'
            '<p class="note">Besitzer 23.10. abends. Grundsatz „alles wirkt harsch“: weichere Übergänge, kleinere Tonsprünge, keine reinen '
            'Weiß-/Schwarz-Spitzen. Oben das Eingebaute (vorläufig, Branch a-misctex23b), unten die offenen Wahlen mit Antwortcode. '
            'Eigene Pixel in Ebenen; Vanilla-Bilder nur auf dieser Seite. Money: verworfen, eigene Texturen bleiben (nichts geändert).</p>'
            '<div id="code"><span>Antwort:</span><code id="c"></code><button id="cp">Kopieren</button></div>'
            '<h2 class="part">Eingebaut</h2>' + ''.join(built_s) + '<h2 class="part">Wahl offen</h2>' + ''.join(open_s))
    Path(path).write_text('<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
                          f'<title>Texturen W23-4</title><style>{css}</style></head><body><main>{body}</main><script>{js}</script></body></html>',
                          encoding='utf-8')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--write', action='store_true')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--page')
    ap.add_argument('--jar', default=os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar'))
    ap.add_argument('--j264', default=os.path.expanduser('~/.gradle/caches/simplebuilding/jukebox264/26.4-snapshot-4'))
    a = ap.parse_args()
    if a.page:
        page(a.page, a.jar, Path(a.j264))
        print('page', a.page)
    if a.write or a.check:
        stale = write_all(check=a.check)
        if a.check and stale:
            raise SystemExit('stale: ' + ', '.join(str(p) for p in stale))
        print('misctex23b textures: ' + ('up to date' if a.check else 'written'))


if __name__ == '__main__':
    main()
