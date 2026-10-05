"""Owner textures 2026-10-05: yarn ball + diamond pebble (owner drawings, Resprite), Raw Enderite Scrap contour.

Usage (Pillow): python tools/textures/owner_round_2026_10_05.py [--yarn-shot PNG] [--pebble-shot PNG] [--preview PNG]

1. Reconstruction: with --yarn-shot / --pebble-shot the iPad screenshots are sampled at the cell centres of the
   calibrated 16x16 canvas (canvas bounds measured from the dark canvas frame) and stored as
   tools/textures/hand/owner/{yarn_ball,diamond_pebble}_owner.png. Without them the stored reconstructions are used.
   Yarn: cream background = transparent (flood fill from the border); background-coloured cells enclosed by the
   outline are the drawing's white wool. Pebble: grey checker = transparent, only the turquoise cells are kept.
2. Colour fix, shapes 1:1: every owner tone keeps its brightness step.
   Yarn -> vanilla white wool (block/white_wool.png): light steps take the wool colour of nearest luminance; steps
   darker than any wool pixel take the wool's darkest colour scaled to the owner's luminance (same hue).
   Pebble -> vanilla diamond (item/diamond.png): owner luminance range stretched onto the diamond ramp, nearest step.
   The mod has a single yarn ball (no colour variants).
3. Raw Enderite Scrap (item/layered_raw_enderite): outline like the Raw Enderite Fragment (item/raw_enderite) -
   every silhouette pixel with a transparent 4-neighbour becomes the fragment's outline colour; shape and inner
   pixels stay. Idempotent.
Installs into all trees (main, 1.21.11 copy, 26.3 overlay for the yarn ball; the wiki copies come from
wiki/generate.py --all). tools/textures/hand/yarn_ball.png is what silent_dandelion_2026_10_03.py uses;
generate_textures.py takes diamond_pebble and layered_raw_enderite from the main tree (hand_drawn)."""
import argparse
import functools
import io
import os
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
OWNER = HERE / 'hand' / 'owner'
JAR = Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
TEX = 'assets/simplebuilding/textures/item/'
MAIN = ROOT / 'src/main/resources' / TEX
OLD = ROOT / 'mc1_21_11/fabric/src/main/resources' / TEX
OVERLAY = ROOT / 'mc26_3/overlay/resources' / TEX
YARN_BOX = (931, 336, 1599, 1004)    # inclusive canvas pixels inside the frame (screenshot 2420x1668)
PEBBLE_BOX = (1052, 449, 1367, 764)
FRAGMENT_OUTLINE = (0x1c, 0x0a, 0x33)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def sample(path, box):
    im = Image.open(path).convert('RGB')
    x0, y0, x1, y1 = box
    cw, ch = (x1 - x0 + 1) / 16, (y1 - y0 + 1) / 16
    grid = []
    for j in range(16):
        row = []
        for i in range(16):
            cx, cy = x0 + (i + .5) * cw, y0 + (j + .5) * ch
            px = [im.getpixel((int(cx + dx), int(cy + dy))) for dx in range(-int(cw / 4), int(cw / 4) + 1)
                  for dy in range(-int(ch / 4), int(ch / 4) + 1)]
            row.append(tuple(round(sum(p[k] for p in px) / len(px)) for k in range(3)))
        grid.append(row)
    return grid


def cluster(values, gap=3):
    """Group luminances separated by more than `gap` (sampling jitter) -> mean per group."""
    groups = []
    for v in sorted(values):
        if groups and v - groups[-1][-1] <= gap:
            groups[-1].append(v)
        else:
            groups.append([v])
    return groups


def reconstruct_yarn(shot):
    grid = sample(shot, YARN_BOX)
    bg = lambda c: min(c) > 235 and c[0] - c[2] > 6  # cream canvas (255,255,241)
    outside, todo = set(), [(x, y) for x in range(16) for y in (0, 15)] + [(x, y) for y in range(16) for x in (0, 15)]
    while todo:
        x, y = todo.pop()
        if 0 <= x < 16 and 0 <= y < 16 and (x, y) not in outside and bg(grid[y][x]):
            outside.add((x, y))
            todo += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
    lums = {(x, y): lum(grid[y][x]) for x in range(16) for y in range(16) if (x, y) not in outside}
    groups = cluster(set(round(v) for v in lums.values()))
    level = {v: round(sum(g) / len(g)) for g in groups for v in g}
    im = Image.new('RGBA', (16, 16))
    for (x, y), v in lums.items():
        g = 255 if bg(grid[y][x]) else level[round(v)]
        im.putpixel((x, y), (g, g, g, 255))
    return im


def reconstruct_pebble(shot):
    grid = sample(shot, PEBBLE_BOX)
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = grid[y][x]
            if max(c) - min(c) > 12:  # turquoise; the checker is neutral grey
                im.putpixel((x, y), c + (255,))
    return im


def opaque(im):
    return [(x, y) for y in range(16) for x in range(16) if im.getpixel((x, y))[3]]


@functools.cache
def vanilla(name):
    with zipfile.ZipFile(JAR) as jar:
        return Image.open(io.BytesIO(jar.read(f'assets/minecraft/textures/{name}.png'))).convert('RGBA')


def recolour_yarn(owner):
    wool = sorted({vanilla('block/white_wool').getpixel(p)[:3] for p in opaque(vanilla('block/white_wool'))}, key=lum)
    darkest = wool[0]
    out = owner.copy()
    for p in opaque(owner):
        v = owner.getpixel(p)[0]
        if v >= lum(darkest):
            c = min(wool, key=lambda w: abs(lum(w) - v))
        else:
            c = tuple(round(k * v / lum(darkest)) for k in darkest)
        out.putpixel(p, c + (255,))
    return out


def recolour_pebble(owner):
    ramp = sorted({vanilla('item/diamond').getpixel(p)[:3] for p in opaque(vanilla('item/diamond'))}, key=lum)
    lo, hi = lum(ramp[0]), lum(ramp[-1])
    ols = [lum(owner.getpixel(p)) for p in opaque(owner)]
    olo, ohi = min(ols), max(ols)
    out = owner.copy()
    for p in opaque(owner):
        t = lo + (lum(owner.getpixel(p)) - olo) / (ohi - olo) * (hi - lo)
        out.putpixel(p, min(ramp, key=lambda c: abs(lum(c) - t)) + (255,))
    return out


def outline_scrap(scrap):
    out = scrap.copy()
    for x, y in opaque(scrap):
        edge = any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or not scrap.getpixel((x + dx, y + dy))[3]
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            out.putpixel((x, y), FRAGMENT_OUTLINE + (255,))
    return out


def save(im, *paths):
    for p in paths:
        p.parent.mkdir(parents=True, exist_ok=True)
        im.save(p)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--yarn-shot')
    ap.add_argument('--pebble-shot')
    ap.add_argument('--preview', default=str(ROOT / '.ai-runs/besitzer-wolle-kiesel-scrap-vorschau.png'))
    a = ap.parse_args()
    if a.yarn_shot:
        save(reconstruct_yarn(a.yarn_shot), OWNER / 'yarn_ball_owner.png')
    if a.pebble_shot:
        save(reconstruct_pebble(a.pebble_shot), OWNER / 'diamond_pebble_owner.png')
    yarn_o = Image.open(OWNER / 'yarn_ball_owner.png').convert('RGBA')
    peb_o = Image.open(OWNER / 'diamond_pebble_owner.png').convert('RGBA')
    yarn, peb = recolour_yarn(yarn_o), recolour_pebble(peb_o)
    scrap_before = Image.open(MAIN / 'layered_raw_enderite.png').convert('RGBA')
    scrap = outline_scrap(scrap_before)
    save(yarn, OVERLAY / 'yarn_ball.png', HERE / 'hand' / 'yarn_ball.png')
    save(peb, MAIN / 'diamond_pebble.png', OLD / 'diamond_pebble.png')
    save(scrap, MAIN / 'layered_raw_enderite.png', OLD / 'layered_raw_enderite.png')

    # preview: screenshot crop | reconstruction | installed, 16x + 1x; scrap before | after
    rows = [('Wollknaeuel', a.yarn_shot, YARN_BOX, yarn_o, yarn), ('Diamant-Kiesel', a.pebble_shot, PEBBLE_BOX, peb_o, peb),
            ('Raw Enderite Scrap', None, None, scrap_before, scrap)]
    s, cell = 16, 16 * 16 + 60
    sheet = Image.new('RGB', (20 + 3 * cell, 30 + len(rows) * (cell + 40)), (139, 139, 139))
    d = ImageDraw.Draw(sheet)
    d.text((10, 8), 'Besitzer-Runde 2026-10-05: Formen 1:1, Farben auf Vanilla-Paletten (Wolle / Diamant); Scrap mit Fragment-Kontur', fill='black')
    for r, (name, shot, box, a_im, b_im) in enumerate(rows):
        y = 30 + r * (cell + 40)
        labels = ['Screenshot-Ausschnitt', 'Rekonstruktion', 'farbkorrigiert (eingebaut)'] if shot else ['', 'vorher', 'nachher (eingebaut)']
        if shot:
            crop = Image.open(shot).convert('RGB').crop((box[0], box[1], box[2] + 1, box[3] + 1)).resize((256, 256))
            sheet.paste(crop, (10, y + 16))
        for k, im in ((1, a_im), (2, b_im)):
            x = 10 + k * cell
            for yy in range(16):
                for xx in range(16):
                    d.rectangle((x + xx * s, y + 16 + yy * s, x + xx * s + s - 1, y + 16 + yy * s + s - 1),
                                fill=(198, 198, 198) if (xx + yy) % 2 else (170, 170, 170))
            sheet.paste(im.resize((256, 256), Image.Resampling.NEAREST), (x, y + 16), im.resize((256, 256), Image.Resampling.NEAREST))
            sheet.paste(im, (x + 264, y + 16), im)
        for k, t in enumerate(labels):
            d.text((10 + k * cell, y + 2), f'{name}: {t}' if t else name, fill='black')
    Path(a.preview).parent.mkdir(parents=True, exist_ok=True)
    sheet.save(a.preview)
    print(a.preview)


if __name__ == '__main__':
    main()
