"""a-abn29t (29.10.): owner picks of the a-tex29 round + Strong Gunpowder B / Strong TNT, built into the repo.

Principle A8b (about 7 % hard transitions, measured): every number printed by --report is the share of neighbouring pixel
pairs whose luminance differs by more than 40 (tex29.smooth_pairs).

Own pixels (committed with layer sources, written by `--write`):
  - ashwood_planks, duskwood_planks (planks A: the 4x4 plank grid, seven soft tones)
  - ashwood_door, duskwood_door item (C: the placed door averaged to 10 x 14, the upper window see-through)
  - astral_piston(+_active): old texture mixed with C; nihil_piston(+_active): B, less hard
  - sculk_tiles (animation A, softer and darker; written by blocks_b23_2026_10_23.main through sculk_tiles_strip())
Vanilla-derived (built from the client jar, never committed; vanilla_derived.py calls derived_end/derived_mobs/derived_core):
  - simpleend: ashwood/duskwood boat + chest boat (item and entity sheets), A
  - simplemobs: soulfire_rod A, strong_gunpowder B (blue), strong_tnt_side/top/bottom (only the red pixels turn blue)
  - core: diamond_rod A, the six textures of the chiseled crimson/warped blazewood bookshelves (frame in blazewood, blaze books)
  - simpleblocks (blocks_b23 / shapecutter modules call back here): sculk_deepslate and hushed_deepslate_tiles as 8-frame strips

    python3.12 tools/textures/abn29_2026_10_29.py --write [--jar J]      # own textures
    python3.12 tools/textures/abn29_2026_10_29.py --preview OUT.html [--jar J]
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import tex29_2026_10_29 as t  # noqa: E402
from layers import save  # noqa: E402

ROOT = t.ROOT
END_TEX = t.END_TEX
SB_TEX = t.SB_TEX
BLOCKS_TEX = t.BLOCKS_TEX
MOBS_TEX = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/textures'
SEED = {'ashwood': 7, 'duskwood': 19}
STRONG_TINT = (92, 138, 255)          # Strong Gunpowder B (blue): the tint the owner saw on the Vanilla gunpowder
SOFT_TILE_SCALE = 0.74                # A8b: sculk tiles softer and darker than variant A (17 % hard) -> about 9 %


def jar_of(jar) -> t.Jar:
    return jar if isinstance(jar, t.Jar) else t.Jar(jar)


def soften(im: Image.Image, k: float) -> Image.Image:
    """Every pixel moves k of the way to the mean of its four neighbours (wraps, tiles stay seamless)."""
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            nb = [im.getpixel(((x + dx) % im.width, (y + dy) % im.height)) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            m = [sum(q[c] for q in nb) / 4 for c in range(3)]
            out.putpixel((x, y), tuple(int(round(p[c] * (1 - k) + m[c] * k)) for c in range(3)) + (255,))
    return out


def quant(im: Image.Image, n: int) -> Image.Image:
    """At most n tones (median cut over the opaque pixels), alpha kept: blends and averages must not leave 100+ colours (A8b)."""
    rgb = Image.new('RGB', im.size)
    rgb.paste(im.convert('RGB'))
    q = rgb.quantize(colors=n, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert('RGBA')
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            if im.getpixel((x, y))[3]:
                out.putpixel((x, y), q.getpixel((x, y))[:3] + (im.getpixel((x, y))[3],))
    return out


def mix_img(a: Image.Image, b: Image.Image, f: float) -> Image.Image:
    out = a.copy()
    for y in range(a.height):
        for x in range(a.width):
            out.putpixel((x, y), t.mixc(a.getpixel((x, y)), b.getpixel((x, y)), f))
    return out


# ---------------------------------------------------------------- planks, door items

def planks(wood: str) -> Image.Image:
    return t.planks_a(wood, SEED[wood])[0]


def door_item(wood: str) -> Image.Image:
    """Variant C of the door item (the placed door, alpha-weighted average to 10 x 14, outline left and below) with the see-through
    window of the placed door kept: where the door is mostly open (ashwood: the three pixels of each eye; duskwood: the meander
    channel), the item is see-through, as at the Vanilla door item."""
    top = Image.open(END_TEX / f'block/{wood}_door_top.png').convert('RGBA')
    bot = Image.open(END_TEX / f'block/{wood}_door_bottom.png').convert('RGBA')
    full = Image.new('RGBA', (16, 32))
    full.paste(top, (0, 0))
    full.paste(bot, (0, 16))
    limit = 100 if wood == 'ashwood' else 190             # alpha (0..255) of the PIL box average, as the owner saw the C preview
    alpha = full.getchannel('A').resize((10, 14), Image.BOX)
    r = t.ramp7(t.wood_tints()[wood]['base'], 0.55, 1.12)
    im = t.new()
    for y in range(14):
        for x in range(10):
            x0, x1, y0, y1 = x * 1.6, (x + 1) * 1.6, y * 32 / 14, (y + 1) * 32 / 14
            sa = sr = sg = sb = area = 0.0
            for yy in range(int(y0), min(32, int(y1) + 1)):
                for xx in range(int(x0), min(16, int(x1) + 1)):
                    w = max(0.0, min(x1, xx + 1) - max(x0, xx)) * max(0.0, min(y1, yy + 1) - max(y0, yy))
                    if w <= 0:
                        continue
                    p = full.getpixel((xx, yy))
                    area += w
                    sa += w * p[3] / 255
                    sr += w * p[0] * p[3] / 255
                    sg += w * p[1] * p[3] / 255
                    sb += w * p[2] * p[3] / 255
            cover = sa / area
            if alpha.getpixel((x, y)) <= limit and y < 8:
                continue
            if cover < 0.5:                                  # lower part: a gap would not be a window
                im.putpixel((3 + x, 2 + y), r[0])
            else:
                im.putpixel((3 + x, 2 + y), (int(sr / sa), int(sg / sa), int(sb / sa), 255))
    for x in range(3, 13):
        im.putpixel((x, 15), r[0])
    for y in range(2, 16):
        im.putpixel((3, y), r[0])
    return quant(im, 12)


# ---------------------------------------------------------------- pistons

def piston(kind: str, active: bool) -> Image.Image:
    """Astral: the old texture mixed 50:50 with variant C, softened. Nihil: variant B with the ring pulled towards the resting
    state and softened (owner: B, a little less hard)."""
    if kind == 'astral':
        # the old texture (before this round) is kept as the source: tools/textures/abn29/astral_piston[_active]_old.png
        old = Image.open(HERE / f'abn29/astral_piston{"_active" if active else ""}_old.png').convert('RGBA')
        return quant(soften(mix_img(old, t.piston_soft(kind, 'C', active), 0.5), 0.35), 16)
    on, off = t.piston_soft(kind, 'B', active), t.piston_soft(kind, 'B', False)
    return quant(soften(mix_img(on, off, 0.5 if active else 0.0), 0.45), 16)



# ---------------------------------------------------------------- sculk tiles (own, animated)

def sculk_tiles_strip() -> Image.Image:
    """Variant A softer and darker (the shading scaled by SOFT_TILE_SCALE), eight frames of 10 ticks with six small dots, each in its own phase."""
    base = soft_tiles_scaled()
    return t.strip_of([t.render_spots(base, t.TILE_SPOTS, f, 3.0, 2.6) for f in range(t.FRAMES)])


def soft_tiles_scaled(seed: int = 61) -> Image.Image:
    b23 = t.b23
    R = t.stile_ramp(15)
    g = lambda x, y: b23.LAYOUT[y % 16][x % 16]
    val = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            c = g(x, y)
            n = t.vnoise(x, y, 3, seed + ord(c)) * 0.6 + t.hsh(x, y, seed + 7) * 0.4
            if c == '.':
                val[y][x] = 1.2 + (n - 0.5) * 1.6
                continue
            v = b23.SHADE[c] * 1.35 + (n - 0.5) * 2.4
            up, left, down, right = g(x, y - 1) != c, g(x - 1, y) != c, g(x, y + 1) != c, g(x + 1, y) != c
            if up or left:
                v += 0.7
            elif down or right:
                v -= 1.1
            val[y][x] = v * SOFT_TILE_SCALE
    im = t.new()
    for y in range(16):
        for x in range(16):
            c = g(x, y)
            same = [val[(y + dy) % 16][(x + dx) % 16] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if g(x + dx, y + dy) == c]
            sm = 0.55 * val[y][x] + 0.45 * (sum(same) / len(same) if same else val[y][x])
            t.put(im, x, y, R[max(0, min(14, int(round(sm))))])
    return im


def derived_deepslate_strip(jar, kind: str) -> Image.Image:
    """Sculk deepslate / hushed tiles: Vanilla base + own veins, variant B (five plus-shaped dots, each in another frame), 8 frames."""
    return t.strip_of(t.derived_frames(jar_of(jar), kind, 'B')[1])


# ---------------------------------------------------------------- derived: boats, rods, powder, TNT, bookshelves

def boat_ramp(wood: str):
    return t.boat_ramp(t.wood_tints()[wood]['base'], lo=0.30, hi=1.25)


def derived_end(jar) -> dict:
    jar = jar_of(jar)
    out = {}
    for wood in ('ashwood', 'duskwood'):
        r = boat_ramp(wood)
        out[f'item/{wood}_boat.png'] = t.boat_item(jar, wood, r, False)
        out[f'item/{wood}_chest_boat.png'] = t.boat_item(jar, wood, r, True)
        out[f'entity/boat/{wood}.png'] = t.boat_entity(jar, r, False)
        out[f'entity/chest_boat/{wood}.png'] = t.boat_entity(jar, r, True)
    return out


def tinted(im: Image.Image, tint) -> Image.Image:
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), tuple(p[c] * tint[c] // 255 for c in range(3)) + (p[3],))
    return out


def is_red(p) -> bool:
    return p[3] > 0 and p[0] > p[1] * 1.5 and p[0] > p[2] * 1.5


def blue_of(p) -> tuple:
    """The Strong Gunpowder blue at the brightness of the red pixel it replaces."""
    f = t.lum(p) / t.lum(STRONG_TINT)
    return tuple(max(0, min(255, int(round(STRONG_TINT[c] * f)))) for c in range(3)) + (p[3],)


def strong_tnt(jar, name: str) -> Image.Image:
    """Vanilla TNT face: structure, white middle, grey and fuse pixels unchanged, only the red pixels turn into the blue of the
    Strong Gunpowder (owner 29.10.: no blue cast over the block)."""
    im = jar_of(jar).img('block/' + name)
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if is_red(p):
                out.putpixel((x, y), blue_of(p))
    return out


def derived_mobs(jar) -> dict:
    jar = jar_of(jar)
    return {'item/soulfire_rod.png': t.rod_from(jar.img('item/blaze_rod'), t.SOULFIRE_KEYS, 0.98),
            'item/strong_gunpowder.png': tinted(jar.img('item/gunpowder'), STRONG_TINT),
            'block/strong_tnt_side.png': strong_tnt(jar, 'tnt_side'),
            'block/strong_tnt_top.png': strong_tnt(jar, 'tnt_top'),
            'block/strong_tnt_bottom.png': strong_tnt(jar, 'tnt_bottom')}


def derived_core(jar) -> dict:
    jar = jar_of(jar)
    out = {'item/diamond_rod.png': t.rod_from(jar.img('item/blaze_rod'), t.DIAMOND_KEYS, 0.98)}
    for wood in ('crimson', 'warped'):
        empty, occupied, _ = t.chiseled_front(jar, wood, 'A')
        out[f'block/chiseled_{wood}_blazewood_bookshelf_empty.png'] = empty
        out[f'block/chiseled_{wood}_blazewood_bookshelf_occupied.png'] = occupied
        out[f'block/chiseled_{wood}_blazewood_bookshelf_side.png'] = t.chiseled_side(jar, wood)
    return out


# ---------------------------------------------------------------- writing / report

MCMETA_SCULK = {'animation': {'frametime': 10, 'interpolate': True}}


def write_mcmeta(path: Path, doc: dict) -> None:
    path.write_text(json.dumps(doc, indent=2) + '\n', encoding='utf-8', newline='\n')


def own_textures() -> dict:
    out = {}
    for wood in ('ashwood', 'duskwood'):
        out[END_TEX / f'block/{wood}_planks.png'] = planks(wood)
        out[END_TEX / f'item/{wood}_door.png'] = door_item(wood)
    for kind in ('astral', 'nihil'):
        out[SB_TEX / f'block/{kind}_piston.png'] = piston(kind, False)
        out[SB_TEX / f'block/{kind}_piston_active.png'] = piston(kind, True)
    return out


def write_all() -> None:
    for path, im in own_textures().items():
        save(im, path)
        print('wrote', path.relative_to(ROOT))
    path = BLOCKS_TEX / 'sculk_tiles.png'
    save(sculk_tiles_strip(), path)
    write_mcmeta(path.with_name('sculk_tiles.png.mcmeta'), MCMETA_SCULK)
    print('wrote', path.relative_to(ROOT))


def report(jar_path) -> None:
    for path, im in own_textures().items():
        print(f'{path.name:34s} {t.smooth_pairs(im):5.1%} hard, {t.tones(im)} tones')
    st = sculk_tiles_strip()
    frames = [st.crop((0, 16 * i, 16, 16 * i + 16)) for i in range(t.FRAMES)]
    print(f'sculk_tiles                        {max(t.smooth_pairs(f) for f in frames):5.1%} hard (worst frame)')
    jar = jar_of(jar_path)
    for kind in ('deepslate', 'tiles'):
        fr = t.derived_frames(jar, kind, 'B')[1]
        print(f'sculk {kind:26s} {max(t.smooth_pairs(f) for f in fr):5.1%} hard (worst frame)')


def preview(out: str, jar_path) -> None:
    jar = jar_of(jar_path)
    pg = t.Page()
    pg.add('<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
           f'<title>a-abn29t Nachher</title><style>{t.CSS}</style><main><h1>a-abn29t: eingebaute Abnahmen (ABN29-4, ABN29-5)</h1>'
           '<p class="note">Alle Bilder 16-fach, daneben 3×3 und der Anteil harter Übergänge (Nachbarpixel mit Helligkeitsunterschied über 40; '
           'Maßstab A8b: etwa 7 %, über 12 % zu hart). Vanilla-abgeleitete Texturen (Boote, Ruten, Starkes Schwarzpulver/TNT, Regale, Sculk) erscheinen nur hier, nie im Repo.</p>')

    def show(title, im, s=16, cap=''):
        pg.cell(im, s, f'{title} ({t.smooth_pairs(im):.0%} hart){cap}')

    pg.add('<h2>Bretter A</h2>')
    for wood in ('ashwood', 'duskwood'):
        im = planks(wood)
        pg.row(); show(wood, im); pg.cell(t.tile(im), 5, f'{wood} 3×3'); pg.end()
    pg.add('<h2>Boote A (Item + Entity)</h2>')
    ends = derived_end(jar)
    pg.row()
    for k in ('item/ashwood_boat', 'item/ashwood_chest_boat', 'item/duskwood_boat', 'item/duskwood_chest_boat'):
        pg.cell(ends[k + '.png'], 12, k.split('/')[1])
    pg.end()
    pg.row()
    for k in ('entity/boat/ashwood', 'entity/chest_boat/ashwood', 'entity/boat/duskwood'):
        pg.cell(ends[k + '.png'], 3, k)
    pg.end()
    pg.add('<h2>Ruten A</h2>')
    core, mobs = derived_core(jar), derived_mobs(jar)
    pg.row(); pg.cell(core['item/diamond_rod.png'], 12, 'Diamantrute'); pg.cell(mobs['item/soulfire_rod.png'], 12, 'Seelenfeuerrute')
    pg.cell(jar.img('item/blaze_rod'), 12, 'Vanilla Lohenrute', 'van'); pg.end()
    pg.add('<h2>Starkes Schwarzpulver B + Starkes TNT</h2>')
    pg.row(); pg.cell(mobs['item/strong_gunpowder.png'], 12, 'Starkes Schwarzpulver B'); pg.cell(jar.img('item/gunpowder'), 12, 'Vanilla Schwarzpulver', 'van')
    for n in ('side', 'top', 'bottom'):
        pg.cell(mobs[f'block/strong_tnt_{n}.png'], 10, f'Starkes TNT {n}')
    pg.cell(jar.img('block/tnt_side'), 10, 'Vanilla TNT Seite', 'van'); pg.end()
    pg.add('<h2>Türitems C (oben durchsichtig)</h2>')
    pg.row()
    for wood in ('ashwood', 'duskwood'):
        pg.cell(Image.open(END_TEX / f'item/{wood}_door.png').convert('RGBA'), 12, f'alt {wood}', 'old')
        pg.cell(door_item(wood), 12, f'neu {wood}')
    pg.end()
    pg.add('<h2>Kolben</h2>')
    for kind in ('astral', 'nihil'):
        pg.row()
        for act in (False, True):
            old = Image.open(SB_TEX / f'block/{kind}_piston{"_active" if act else ""}.png').convert('RGBA')
            new = piston(kind, act)
            pg.cell(old, 10, f'alt {kind}{" aktiv" if act else ""} ({t.smooth_pairs(old):.0%} hart)', 'old')
            show(f'neu {kind}{" aktiv" if act else ""}', new, 10)
            pg.cell(t.tile(new), 3, '3×3')
        pg.end()
    pg.add('<h2>Sculk</h2>')
    pg.row()
    st = sculk_tiles_strip()
    frames = [st.crop((0, 16 * i, 16, 16 * i + 16)) for i in range(t.FRAMES)]
    pg.raw(f'<img src="{t.gif_uri(t.interp_frames(frames), 10, 100)}" alt="">', f'Sculk-Fliesen (worst frame {max(t.smooth_pairs(f) for f in frames):.0%} hart)')
    pg.cell(t.tile(frames[3]), 4, '3×3')
    for kind, label in (('tiles', 'Verstummte Tiefenschiefer-Fliesen'), ('deepslate', 'Sculk-Tiefenschiefer')):
        fr = t.derived_frames(jar, kind, 'B')[1]
        pg.raw(f'<img src="{t.gif_uri(t.interp_frames(fr), 10, 100)}" alt="">', f'{label} (worst frame {max(t.smooth_pairs(f) for f in fr):.0%} hart)')
    pg.end()
    pg.add('<h2>Gemeißelte Lohen-Regale A</h2>')
    pg.row()
    for k, im in core.items():
        if 'bookshelf' in k:
            pg.cell(im, 8, k.split('/')[1].replace('chiseled_', '').replace('_blazewood_bookshelf', ''))
    pg.end()
    pg.add('<h2>Formsäge (alter Stand, nur Eckpfeiler aus Holz)</h2>')
    import shapecutter_2026_10_21 as sc
    sh = sc.derived(jar.path)
    pg.row()
    for k in ('top', 'side', 'bottom', 'metal_top'):
        pg.cell(sh[f'block/shapecutter_{k}.png'], 10, f'Formsäge {k}')
    pg.end()
    pg.add('</main></html>')
    Path(out).write_text('\n'.join(pg.h), encoding='utf-8')
    print('preview', out)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--jar', default=str(t.JAR))
    ap.add_argument('--write', action='store_true')
    ap.add_argument('--report', action='store_true')
    ap.add_argument('--preview')
    a = ap.parse_args()
    if a.write:
        write_all()
    if a.report:
        report(a.jar)
    if a.preview:
        preview(a.preview, a.jar)


if __name__ == '__main__':
    main()
