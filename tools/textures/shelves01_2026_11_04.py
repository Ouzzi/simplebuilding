"""b-shelves01 (04.11., DW04C-3, docs/ai/PLAN-B-SHELVES01-2026-11-04.md): End bookshelves, chiseled bookshelves, Nihilit Book.

Own pixels (committed with layer sources, `--write`):
  - simpleend:block/{ashwood,duskwood}_bookshelf: frame = the wood's own planks texture (top/bottom board, middle board, side
    posts), two shelves of books in Vanilla's arrangement (1-2 px spines, 4-6 px high) in muted, cool End tones; soft spine shading.
Vanilla-derived (built from the client jar, never committed; vanilla_derived.py calls derived_core/derived_end/derived_enchanting):
  - chiseled bookshelves of every mod wood (crimson/warped blazewood in SB and Simple Enchanting, ashwood/duskwood in Simple End):
    Vanilla's chiseled front/side/top with the frame recoloured to the wood's planks ramp; the books stay Vanilla's (owner 04.11.:
    "variation der books ... wie in vanilla" - every slot another colour, like Vanilla).
  - simplebuilding:item/nihilith_book: the Blaze Book recipe (Vanilla written book layout) on a Nihilit ramp (navy cover, teal-cyan
    highlights from the Nihilit Shard), a shard embossed on the cover.
  - simplebuilding:block/chiseled_books_{blaze,nihilith}: only the book pixels of Vanilla's occupied front, each slot on its own
    ramp of the book's colours (Vanilla's variation); drawn per slot by ChiseledBooksRenderer over a slot that holds such a book.

    python3.12 tools/textures/shelves01_2026_11_04.py --write
    python3.12 tools/textures/shelves01_2026_11_04.py --check
    python3.12 tools/textures/shelves01_2026_11_04.py --preview OUT.jpg [--shots DIR]
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import tex29_2026_10_29 as t  # noqa: E402
from layers import save  # noqa: E402

ROOT = t.ROOT
END_TEX = t.END_TEX
SB_TEX = t.SB_TEX
ENCH_TEX = ROOT / 'modules/simpleenchanting/shared/resources/assets/simpleenchanting/textures'
END_WOODS = ('ashwood', 'duskwood')
BLAZE_WOODS = ('crimson', 'warped')
lum = t.lum
mix = t.mixc


def H(h):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def jar_of(jar):
    return jar if isinstance(jar, t.Jar) else t.Jar(jar)


def ramp_of(path: Path):
    im = Image.open(path).convert('RGBA').crop((0, 0, 16, 16))
    return sorted({im.getpixel((x, y)) for y in range(16) for x in range(16) if im.getpixel((x, y))[3]}, key=lum), im


def end_ramp(wood):
    return ramp_of(END_TEX / f'block/{wood}_planks.png')


def blaze_ramp(wood):
    return t.blaze_wood_ramp(wood)


# ---------------------------------------------------------------- chiseled (derived)

def chiseled(jar, pal):
    """Vanilla's chiseled bookshelf: frame/cavity recoloured to the wood ramp, the books left as Vanilla's (variant C of a-tex29)."""
    jar = jar_of(jar)
    emp, occ = jar.img('block/chiseled_bookshelf_empty'), jar.img('block/chiseled_bookshelf_occupied')
    frame_cols = sorted({emp.getpixel((x, y)) for y in range(16) for x in range(16) if lum(emp.getpixel((x, y))) > 90}, key=lum)
    cav_cols = sorted({emp.getpixel((x, y)) for y in range(16) for x in range(16) if lum(emp.getpixel((x, y))) <= 90}, key=lum)
    to = {c: pal[min(len(pal) - 1, int(round(i * (len(pal) - 1) / max(1, len(frame_cols) - 1))))] for i, c in enumerate(frame_cols)}
    for i, c in enumerate(cav_cols):
        to[c] = tuple(int(pal[0][k] * (0.28 + 0.14 * i)) for k in range(3)) + (255,)

    def recolor(src):
        out = src.copy()
        for y in range(16):
            for x in range(16):
                p = src.getpixel((x, y))
                if p == emp.getpixel((x, y)) and p in to:
                    out.putpixel((x, y), to[p])
        return out

    def whole(name):
        src = jar.img('block/' + name)
        cols = sorted({src.getpixel((x, y)) for y in range(16) for x in range(16)}, key=lum)
        m = {c: pal[min(len(pal) - 1, int(round(i * (len(pal) - 1) / max(1, len(cols) - 1))))] for i, c in enumerate(cols)}
        out = src.copy()
        for y in range(16):
            for x in range(16):
                out.putpixel((x, y), m[src.getpixel((x, y))])
        return out
    return {'empty': recolor(emp), 'occupied': recolor(occ), 'side': whole('chiseled_bookshelf_side'), 'top': whole('chiseled_bookshelf_top')}


# Slot rectangles on the front texture (u0, v0, u1, v1), Vanilla's template_chiseled_bookshelf_slot_* order 0..5.
SLOTS = ((0, 0, 6, 8), (6, 0, 11, 8), (11, 0, 16, 8), (0, 8, 6, 16), (6, 8, 11, 16), (11, 8, 16, 16))
BOOK_RAMPS = {
    'blaze': [[H('3a1206'), H('7a2a0c'), H('b8460e'), H('e8781e'), H('ffb43c')],    # ember
              [H('40160a'), H('8c3008'), H('d0661a'), H('ee9a30'), H('f6cc5a')],    # Blaze Book cover
              [H('4a1a08'), H('9a4a10'), H('d08a1c'), H('f2c040'), H('fff0a0')]],   # gold
    'nihilith': [[H('08161f'), H('19323f'), H('264d62'), H('3f8094'), H('5eb5bf')],  # teal shard
                 [H('0e1430'), H('1a3160'), H('1e40a1'), H('3e68a2'), H('4a9dc5')],  # navy
                 [H('140c2a'), H('2a1c58'), H('42308a'), H('6650b0'), H('9a88d6')]],  # void violet
}


def books_overlay(jar, kind):
    """Only the book pixels of Vanilla's occupied front; every slot on its own ramp, the shading of Vanilla's books kept."""
    jar = jar_of(jar)
    emp, occ = jar.img('block/chiseled_bookshelf_empty'), jar.img('block/chiseled_bookshelf_occupied')
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for i, (u0, v0, u1, v1) in enumerate(SLOTS):
        r = BOOK_RAMPS[kind][(i * 2 + i // 3) % 3]
        px = [(x, y) for y in range(v0, v1) for x in range(u0, u1) if occ.getpixel((x, y)) != emp.getpixel((x, y))]
        if not px:
            continue
        ls = [lum(occ.getpixel(p)) for p in px]
        lo, hi = min(ls), max(ls)
        for p, value in zip(px, ls):
            k = 0 if hi == lo else (value - lo) / (hi - lo)
            out.putpixel(p, r[min(len(r) - 1, int(round(0.5 + k * (len(r) - 2))))])
    return out


# ---------------------------------------------------------------- Nihilit Book (derived)

NIHIL_COVER = {'out': H('0a1424'), 'edge': H('1a3160'), 'face': H('1e40a1'), 'hi': H('3e68a2'), 'emb': H('4a9dc5'), 'dent': H('193177')}


def nihilith_book(jar):
    import os
    os.environ['SB_CLIENT_JAR'] = str(jar_of(jar).path)
    import n41_round3_2026_10_12 as n41
    keep = dict(n41.COVER), n41.DARK
    try:
        n41.COVER.clear()
        n41.COVER.update(NIHIL_COVER)
        n41.DARK = H('050a12')
        im = n41.blaze_book(2)
    finally:
        n41.COVER.clear()
        n41.COVER.update(keep[0])
        n41.DARK = keep[1]
    for y in range(16):     # the blaze rods' glints -> shard glints
        for x in range(16):
            if im.getpixel((x, y)) == H('ffe27a'):
                im.putpixel((x, y), H('8fe0e6'))
    return im


# ---------------------------------------------------------------- hooks for vanilla_derived

def derived_core(jar) -> dict:
    jar = jar_of(jar)
    out = {'item/nihilith_book.png': nihilith_book(jar),
           'block/chiseled_books_blaze.png': books_overlay(jar, 'blaze'),
           'block/chiseled_books_nihilith.png': books_overlay(jar, 'nihilith')}
    return out


def blaze_chiseled(jar, wood):
    pal, _ = blaze_ramp(wood)
    return chiseled(jar, pal)


def derived_end(jar) -> dict:
    out = {}
    for wood in END_WOODS:
        pal, _ = end_ramp(wood)
        for k, im in chiseled(jar, pal).items():
            out[f'block/chiseled_{wood}_bookshelf_{k}.png'] = im
    return out


def derived_enchanting(jar) -> dict:
    out = {}
    for wood in BLAZE_WOODS:
        for k, im in blaze_chiseled(jar, wood).items():
            if k != 'top':
                out[f'block/chiseled_{wood}_blazewood_bookshelf_{k}.png'] = im
    return out


# ---------------------------------------------------------------- End bookshelf (own pixels)

# Spine colours (dark, mid, light): Vanilla's variety (red, blue, green, ochre, brown) pulled towards the cool, dusty End palette.
SPINES = [(H('3c1c26'), H('6a3442'), H('8c4a58')),   # plum
          (H('1c2a40'), H('34507a'), H('4c6e9a')),   # slate blue
          (H('1e3430'), H('3a5c50'), H('547a6a')),   # sage
          (H('4a3a1c'), H('86683a'), H('a88a52')),   # ochre
          (H('2a2036'), H('4a3c62'), H('665684')),   # dusk violet
          (H('3a2a22'), H('5e463a'), H('7a5e50'))]   # leather


def end_bookshelf(wood):
    pal, planks = end_ramp(wood)
    im = planks.copy()
    seed = {'ashwood': 3, 'duskwood': 11}[wood]
    gap = pal[0]
    for row0 in (1, 9):
        for y in range(row0, row0 + 6):           # back wall: deep wood, a little lighter at the bottom
            for x in range(1, 15):
                im.putpixel((x, y), mix(tuple(int(c * 0.42) for c in gap[:3]) + (255,), gap, 0.35 * (y - row0) / 5))
        x, k = 1, row0 * 5 + seed
        while x < 15:
            w = 2 if t.hsh(x, row0, k) > 0.4 else 1
            w = min(w, 15 - x)
            h = 6 - (1 if t.hsh(x, row0, k + 1) > 0.55 else 0) - (1 if t.hsh(x, row0, k + 2) > 0.85 else 0)
            sp = SPINES[int(t.hsh(x, row0, k + 3) * len(SPINES)) % len(SPINES)]
            for i in range(w):
                for j in range(h):
                    y = row0 + 5 - j
                    c = sp[2] if (w == 2 and i == 0) else sp[1]
                    if j == 0:
                        c = mix(c, sp[0], 0.6)        # spine foot in shadow
                    if j == h - 1:
                        c = mix(c, sp[2], 0.35)       # top edge catches the light
                    if j == h - 3 and w == 2:
                        c = mix(c, H('c8b88a'), 0.35)  # a faded title band
                    im.putpixel((x + i, y), c)
            x += w + (1 if t.hsh(x, row0, k + 5) > 0.8 else 0)
        for x in range(1, 15):                        # shadow under the board above
            im.putpixel((x, row0), mix(im.getpixel((x, row0)), gap, 0.45))
    return im


def own_textures() -> dict:
    return {END_TEX / f'block/{w}_bookshelf.png': end_bookshelf(w) for w in END_WOODS}


# ---------------------------------------------------------------- preview

def iso_cube(top, left, right, s=8):
    """Isometric cube (top, south-west face, south-east face), s pixels per texel."""
    n = 16 * s
    W, Hh = 2 * n, 2 * n
    out = Image.new('RGBA', (W, Hh), (0, 0, 0, 0))

    def face(img, coeffs, shade):
        big = img.resize((n, n), Image.NEAREST)
        if shade != 1:
            big = Image.eval(big.convert('RGB'), lambda v: int(v * shade)).convert('RGBA')
        layer = big.transform((W, Hh), Image.AFFINE, coeffs, Image.NEAREST, fillcolor=(0, 0, 0, 0))
        out.alpha_composite(layer)
    # top: (u,v) -> (n + u - v, (u + v)/2)
    face(top, _inv((1, -1, n, 0.5, 0.5, 0)), 1.0)
    # left: (u,v) -> (u, n/2 + u/2 + v)
    face(left, _inv((1, 0, 0, 0.5, 1, n / 2)), 0.8)
    # right: (u,v) -> (n + u, n - u/2 + v)
    face(right, _inv((1, 0, n, -0.5, 1, n)), 0.62)
    return out


def _inv(m):
    a, b, c, d, e, f = m
    det = a * e - b * d
    ia, ib, id_, ie = e / det, -b / det, -d / det, a / det
    return (ia, ib, -(ia * c + ib * f), id_, ie, -(id_ * c + ie * f))


def preview(out_path: Path, jar, shots: Path | None) -> None:
    from PIL import ImageFont
    jar = jar_of(jar)
    try:
        font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 13)
    except OSError:
        font = ImageFont.load_default()
    cells = []
    vanilla_occ = jar.img('block/chiseled_bookshelf_occupied')
    blaze_ov, nihil_ov = books_overlay(jar, 'blaze'), books_overlay(jar, 'nihilith')

    def with_books(front, ov, slots):
        im = front.copy()
        for i in slots:
            u0, v0, u1, v1 = SLOTS[i]
            im.alpha_composite(ov.crop((u0, v0, u1, v1)), (u0, v0))
        return im
    rows = []
    for wood, kind in (('ashwood', 'end'), ('duskwood', 'end'), ('crimson', 'blaze'), ('warped', 'blaze')):
        if kind == 'end':
            pal, planks = end_ramp(wood)
            shelf = end_bookshelf(wood)
            title = {'ashwood': 'Ascheholz (Simple End)', 'duskwood': 'Dämmerholz (Simple End)'}[wood]
        else:
            pal, planks = blaze_ramp(wood)
            shelf = Image.open(SB_TEX / f'block/{wood}_blazewood_bookshelf.png').convert('RGBA').crop((0, 0, 16, 16))
            title = {'crimson': 'Karmesin-Lohenholz (SB + Simple Enchanting)', 'warped': 'Wirr-Lohenholz (SB + Simple Enchanting)'}[wood]
        ch = chiseled(jar, pal)
        mixed = with_books(with_books(ch['occupied'], blaze_ov, (1, 5)), nihil_ov, (3,))
        top = ch['top'] if kind == 'end' else ch['side']
        rows.append((title, [
            ('Bücherregal', iso_cube(planks, shelf, shelf, 6)),
            ('gemeißelt leer', iso_cube(top, ch['empty'], ch['side'], 6)),
            ('gemeißelt voll (Vanilla-Bücher)', iso_cube(top, ch['occupied'], ch['side'], 6)),
            ('voll gemischt: Lohen 2x, Nihilit 1x', iso_cube(top, mixed, ch['side'], 6)),
            ('Front 8x', mixed.resize((128, 128), Image.NEAREST)),
        ]))
    books = [('Vanilla-Buch', jar.img('item/book')), ('Lohenbuch', Image.open(ROOT / 'build/vanilla-derived/resources/assets/simplebuilding/textures/item/blaze_book.png').convert('RGBA')
                                                   if (ROOT / 'build/vanilla-derived/resources/assets/simplebuilding/textures/item/blaze_book.png').exists() else None),
             ('Nihilit-Buch (neu)', nihilith_book(jar)), ('Vanilla gemeißelt voll', vanilla_occ),
             ('Lohen-Rücken je Fach', blaze_ov), ('Nihilit-Rücken je Fach', nihil_ov)]
    if books[1][1] is None:
        import n41_round3_2026_10_12 as n41
        books[1] = ('Lohenbuch', n41.blaze_book(2))
    cw, chh = 230, 240
    shot_list = sorted(shots.glob('*.png')) if shots and shots.is_dir() else []
    W = cw * 5 + 20
    Hh = 60 + len(rows) * (chh + 30) + 200 + (len(shot_list) * 560 if shot_list else 0)
    canvas = Image.new('RGB', (W, Hh), (34, 34, 38))
    d = ImageDraw.Draw(canvas)
    d.text((10, 10), 'SHELVES01 (DW04C-3): End-Bücherregale, gemeißelte Regale mit Vanilla-Buchvariation, Nihilit-Buch, Mod-Bücher je Fach',
           fill=(240, 240, 240), font=font)
    d.text((10, 28), 'Ohne Einwand = abgenommen. Gemeißelt zählt wie Vanilla nicht als Verzauberungskraft; End-/Lohen-Regale = 2 Punkte am Enchanter.',
           fill=(200, 200, 200), font=font)
    y = 50
    for title, items in rows:
        d.text((10, y), title, fill=(255, 220, 140), font=font)
        y += 16
        for i, (cap, im) in enumerate(items):
            x = 10 + i * cw
            canvas.paste(im, (x + (cw - im.width) // 2, y), im)
            d.text((x + 6, y + 200), cap, fill=(220, 220, 220), font=font)
        y += chh
    d.text((10, y), 'Bücher (16x16 je 8x)', fill=(255, 220, 140), font=font)
    y += 16
    for i, (cap, im) in enumerate(books):
        x = 10 + i * 200
        canvas.paste(im.resize((128, 128), Image.NEAREST), (x, y), im.resize((128, 128), Image.NEAREST))
        d.text((x, y + 134), cap, fill=(220, 220, 220), font=font)
    y += 180
    for p in shot_list:
        shot = Image.open(p).convert('RGB')
        shot.thumbnail((W - 20, 540))
        d.text((10, y), 'Ingame: ' + p.stem, fill=(255, 220, 140), font=font)
        canvas.paste(shot, (10, y + 14))
        y += 560
    canvas.save(out_path, quality=90)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--write', action='store_true')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--preview', type=Path)
    ap.add_argument('--shots', type=Path)
    ap.add_argument('--jar', default=str(t.JAR))
    a = ap.parse_args()
    if a.write:
        for path, im in own_textures().items():
            save(im, path)
            print('wrote', path.relative_to(ROOT), 'hard', round(100 * t.smooth_pairs(im), 1), '%')
    if a.check:
        bad = [p for p, im in own_textures().items() if not p.exists() or list(Image.open(p).convert('RGBA').getdata()) != list(im.getdata())]
        if bad:
            sys.exit('out of date: ' + ', '.join(str(p) for p in bad))
        print('shelves01 textures up to date')
    if a.preview:
        preview(a.preview, a.jar, a.shots)
        print('preview', a.preview)


if __name__ == '__main__':
    main()
