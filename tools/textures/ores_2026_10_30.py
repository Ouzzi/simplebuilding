"""Simple Ores textures (ABN30-10, 30.10.2026): every pixel is Vanilla-derived, built, never committed (AGENTS.md rule 7).

Gold sprites of the 26.3 client jar are recoloured by luminance onto a palette ramp (the Vanilla raster and its tone
distribution stay, so the soft transitions of PRINZIPIEN A8b are kept):
  - hard Rose Bronze   palette B "Kupfer-Rose" (owner pick)         - soft Blossom Gold  palette A "Zart-Rosa" (owner pick)
  - silver             cool grey-blue ramp; raw silver warm grey; ores keep the stone and only the gold flecks turn silver
  - tools: only the pixels that differ between the golden and the iron sprite (the head; the stick stays), armor: all pixels
  - pressing: the ingot, matte and darker, with a strap across the middle
  - lead (b-ores32, 31.10.): Vanilla iron sprites onto a dull blue-grey ramp (ores keep the stone, only the iron flecks turn lead); the block gets
    rivets (variant c) or stays plain; variants a/b/c are shown in the preview, LEAD_VARIANT is the one built
  - silver arrow: the Vanilla arrow with its head recoloured silver (item 16 x 16 and entity 32 x 32); silver mirror: silver block frame + glass pane
  - hammers (simplehammers, optional): the committed gold sledgehammer recoloured like the tools (iron sledgehammer as mask)

vanilla_derived.py lists the outputs (MODULE_OUTPUTS['simpleores'] / ['simplehammers']) and calls derived(jar) / derived_hammers(jar).

    python3.12 tools/textures/ores_2026_10_30.py --out DIR [--jar J]    write the textures
    python3.12 tools/textures/ores_2026_10_30.py --preview OUT.html [--jar J]   palette swatches + recoloured sprites
"""
from __future__ import annotations

import argparse
import base64
import io
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
HAMMER_TEX = ROOT / 'modules/simplehammers/shared/resources/assets/simplehammers/textures/item'

# 10-tone ramps, brightest first (palette hexes from the owner preview a-rg30).
RAMPS = {
    'hard': ['#ffeed8', '#fbd5b3', '#f2b88f', '#e69a73', '#d37d5d', '#b5604a', '#924a3c', '#6f372f', '#4d2825', '#331b1c'],     # B Kupfer-Rose
    'soft': ['#fff1ea', '#f7d9cf', '#efc0b4', '#e3a39a', '#d2877f', '#b86a66', '#96504f', '#733a3d', '#512a2f', '#351b22'],     # A Zart-Rosa
    'silver': ['#ffffff', '#f0f4f8', '#dde4ec', '#c6d0dc', '#aebbca', '#93a1b3', '#778699', '#5b6a7e', '#3f4c5f', '#28313f'],
    'raw': ['#ebe8e3', '#d6d2cc', '#bfbab4', '#a8a39d', '#928d87', '#7b7772', '#65625e', '#4f4c49', '#3a3836', '#262524'],
}
RAMPS.update({
    # lead: dull and heavy, clearly darker than silver. a = blue-grey, b = violet-grey, c = graphite
    'lead_a': ['#d4dbe3', '#b7c1cc', '#9aa7b5', '#808e9d', '#677583', '#515e6b', '#3e4954', '#2d363f', '#1f262d', '#14191f'],
    'lead_b': ['#dcd9e3', '#c0bccd', '#a5a0b4', '#8a8499', '#716b80', '#5a5568', '#454153', '#332f3d', '#24212c', '#16141c'],
    'lead_c': ['#b9c0bd', '#9ea6a3', '#848c89', '#6b7371', '#555c5a', '#434948', '#333837', '#262a29', '#191c1c', '#0f1111'],
})
LUM_HI, LUM_LO = 240.0, 50.0   # luminance span of the Vanilla gold sprites

ITEM_TOOLS = ('sword', 'pickaxe', 'axe', 'shovel', 'hoe')
ITEM_ARMOR = ('helmet', 'chestplate', 'leggings', 'boots')
VANILLA_NAME = {'sword': 'sword', 'pickaxe': 'pickaxe', 'axe': 'axe', 'shovel': 'shovel', 'hoe': 'hoe', 'helmet': 'helmet',
                'chestplate': 'chestplate', 'leggings': 'leggings', 'boots': 'boots'}
ALLOYS = {'rose_bronze': 'hard', 'blossom_gold': 'soft'}
LEAD_VARIANT = 'a'      # ramp of lead_<v>; block rivets for 'c'
ARROW_VARIANT = 'a'
MIRROR_VARIANT = 'a'


def hexrgb(h: str) -> tuple[int, int, int]:
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def lum(p) -> float:
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def ramp_at(ramp: list[str], t: float) -> tuple[int, int, int]:
    """Colour at t (0 = brightest ... 1 = darkest), interpolated between the ten tones."""
    t = max(0.0, min(1.0, t)) * (len(ramp) - 1)
    i = min(int(t), len(ramp) - 2)
    f = t - i
    a, b = hexrgb(ramp[i]), hexrgb(ramp[i + 1])
    return tuple(int(round(a[c] * (1 - f) + b[c] * f)) for c in range(3))


def tone(p, ramp: str, shift: float = 0.0, squeeze: float = 1.0) -> tuple:
    """The pixel recoloured onto the ramp; shift > 0 darkens, squeeze < 1 flattens the contrast (matte)."""
    t = (LUM_HI - lum(p)) / (LUM_HI - LUM_LO)
    t = 0.5 + (t - 0.5) * squeeze + shift
    return ramp_at(RAMPS[ramp], t) + (p[3],)


class Jar:
    """Texture reader on the client jar (assets/minecraft/textures/<rel>.png). Without a jar: grey placeholders."""

    def __init__(self, path):
        self.z = zipfile.ZipFile(path) if path and Path(path).exists() else None

    def img(self, rel: str) -> Image.Image:
        if not self.z:
            return Image.new('RGBA', (16, 16), (90, 90, 90, 255))
        return Image.open(io.BytesIO(self.z.read(f'assets/minecraft/textures/{rel}.png'))).convert('RGBA')

    def first(self, *rels: str) -> Image.Image:
        for rel in rels:
            try:
                return self.img(rel)
            except KeyError:
                continue
        raise KeyError(rels)


def recolor(im: Image.Image, ramp: str, mask=None, **kw) -> Image.Image:
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] and (mask is None or mask(x, y, p)):
                out.putpixel((x, y), tone(p, ramp, **kw))
    return out


def goldish(_x, _y, p) -> bool:
    """A yellow-ish pixel (gold flecks); the grey stone around them stays."""
    return p[0] >= p[1] >= p[2] and p[0] - p[2] >= 40


def differs_from(other: Image.Image):
    return lambda x, y, p: other.getpixel((x, y)) != p


def pressing(ingot: Image.Image, ramp: str) -> Image.Image:
    """The ingot as a bound bundle: matte and darker, a dark strap across the middle with a light edge above."""
    out = recolor(ingot, ramp, shift=0.12, squeeze=0.7)
    strap, edge = hexrgb(RAMPS[ramp][8]), hexrgb(RAMPS[ramp][2])
    for x in range(out.width):
        for y, colour in ((6, edge), (7, strap), (8, strap)):
            if out.getpixel((x, y))[3]:
                out.putpixel((x, y), colour + (out.getpixel((x, y))[3],))
    return out


def end_ore(base: Image.Image, gold_ore: Image.Image) -> Image.Image:
    """End stone with the silver flecks of the gold ore laid over it."""
    out = base.copy()
    for y in range(base.height):
        for x in range(base.width):
            p = gold_ore.getpixel((x, y))
            if p[3] and goldish(x, y, p):
                out.putpixel((x, y), tone(p, 'silver'))
    return out


def fleck(_x, _y, p) -> bool:
    """A fleck of the Vanilla iron ore: warmer and lighter than the stone (r > b by 25 or more)."""
    return p[0] - p[2] >= 25


def rivets(im: Image.Image, ramp: str) -> Image.Image:
    """Four rivets in the corners and a darker frame line: the block of lead as a heavy plate."""
    out = im.copy()
    hi, lo, mid = hexrgb(RAMPS[ramp][1]), hexrgb(RAMPS[ramp][7]), hexrgb(RAMPS[ramp][4])
    for x in range(16):
        for y in (0, 15):
            out.putpixel((x, y), lo + (255,))
        for y in (1, 14):
            out.putpixel((x, y), tone(out.getpixel((x, y)), ramp, shift=0.08))
    for y in range(16):
        for x in (0, 15):
            out.putpixel((x, y), lo + (255,))
    for cx, cy in ((3, 3), (12, 3), (3, 12), (12, 12)):
        out.putpixel((cx, cy), hi + (255,))
        out.putpixel((cx + 1, cy + 1), lo + (255,))
        out.putpixel((cx + 1, cy), mid + (255,))
        out.putpixel((cx, cy + 1), mid + (255,))
    return out


def lead_images(j: 'Jar', v: str) -> dict:
    """assets/simpleores/textures/<key> of the lead set in variant v (a, b, c)."""
    ramp = f'lead_{"b" if v == "b" else "a" if v == "a" else "c"}'
    ore, deep = j.img('block/iron_ore'), j.img('block/deepslate_iron_ore')
    out = {
        'block/lead_ore.png': recolor(ore, ramp, fleck),
        'block/deepslate_lead_ore.png': recolor(deep, ramp, fleck),
        'block/raw_lead_block.png': recolor(j.img('block/raw_iron_block'), ramp, shift=0.08),
        'item/raw_lead.png': recolor(j.img('item/raw_iron'), ramp, shift=0.1),
        'item/lead_ingot.png': recolor(j.img('item/iron_ingot'), ramp, shift=0.18),
        'item/lead_nugget.png': recolor(j.img('item/iron_nugget'), ramp, shift=0.18),
    }
    # the Vanilla iron block is almost white: pushed well into the dark half of the ramp so lead looks heavy
    block = recolor(j.img('block/iron_block'), ramp, shift=0.3, squeeze=1.2)
    out['block/lead_block.png'] = rivets(block, ramp) if v == 'c' else (recolor(j.img('block/iron_block'), ramp, shift=0.36, squeeze=0.9) if v == 'b' else block)
    return out


def arrow_images(j: 'Jar', v: str) -> dict:
    """The silver arrow: item sprite (head = grey pixels of the upper right) and entity texture (head = grey pixels around x 12..20, y < 5)."""
    shift = 0.15 if v == 'c' else 0.0
    item = j.img('item/arrow')
    ent = j.img('entity/projectiles/arrow')

    def grey(p):
        return abs(p[0] - p[1]) < 8 and abs(p[1] - p[2]) < 8

    item_out = recolor(item, 'silver', lambda x, y, p: x >= 9 and y <= 5 and grey(p), shift=shift)
    ent_out = recolor(ent, 'silver', lambda x, y, p: 12 <= x <= 20 and y <= 4 and grey(p), shift=shift)
    if v == 'b':   # a pale tip: the foremost head pixel of the sprite lights up
        item_out.putpixel((13, 2), (255, 255, 255, 255))
        item_out.putpixel((12, 3), hexrgb(RAMPS['silver'][1]) + (255,))
    return {'item/silver_arrow.png': item_out, 'entity/projectiles/silver_arrow.png': ent_out}


def mirror_image(j: 'Jar', v: str) -> Image.Image:
    """Silver block frame round a glass pane (variants: a = 2 px frame, b = 1 px frame + brighter pane, c = frame with corner studs)."""
    frame_src = recolor(j.img('block/gold_block'), 'silver')
    glass = j.img('block/glass')
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    width = 1 if v == 'b' else 2
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge < width:
                p = frame_src.getpixel((x, y))
                out.putpixel((x, y), tone(p, 'silver', shift=0.7 if edge == 0 else 0.42) if p[3] else (120, 134, 153, 255))
            else:
                g = glass.getpixel((x, y))
                # the pane: Vanilla glass luminance on a light blue-silver ramp, a diagonal glare band on top
                t = (LUM_HI - lum(g)) / (LUM_HI - LUM_LO) if g[3] else 0.1
                glare = 0.0
                d = (x + y) % 16
                if d in (5, 6) or (v != 'b' and d == 10):
                    glare = -0.12
                base = 0.15 if v == 'b' else 0.32
                out.putpixel((x, y), ramp_at(RAMPS['silver'], base + 0.25 * t + glare + (0.06 if (x + y) % 2 and edge == width else 0)) + (255,))
    if v == 'c':
        for cx, cy in ((2, 2), (13, 2), (2, 13), (13, 13)):
            out.putpixel((cx, cy), hexrgb(RAMPS['silver'][0]) + (255,))
    return out


def hard_share(im: Image.Image) -> float:
    """Share of neighbouring opaque pixel pairs with a luminance jump of 40 or more (PRINZIPIEN A8b: about 7 %, over 12 % is too hard)."""
    px = im.load()
    pairs = hard = 0
    for y in range(im.height):
        for x in range(im.width):
            if not px[x, y][3]:
                continue
            for nx, ny in ((x + 1, y), (x, y + 1)):
                if nx < im.width and ny < im.height and px[nx, ny][3]:
                    pairs += 1
                    hard += abs(lum(px[x, y]) - lum(px[nx, ny])) >= 40
    return hard / pairs if pairs else 0.0


def derived(jar) -> dict:
    """assets/simpleores/textures/<key>: path -> image. Order as MODULE_OUTPUTS['simpleores']."""
    j = jar if isinstance(jar, Jar) else Jar(jar)
    out: dict[str, Image.Image] = {}
    gold_ore, deep = j.img('block/gold_ore'), j.img('block/deepslate_gold_ore')
    out['block/silver_ore.png'] = recolor(gold_ore, 'silver', goldish)
    out['block/deepslate_silver_ore.png'] = recolor(deep, 'silver', goldish)
    out['block/end_silver_ore.png'] = end_ore(j.img('block/end_stone'), gold_ore)
    out['block/raw_silver_block.png'] = recolor(j.img('block/raw_gold_block'), 'raw')
    out['block/silver_block.png'] = recolor(j.img('block/gold_block'), 'silver')
    out['item/raw_silver.png'] = recolor(j.img('item/raw_gold'), 'raw')
    out['item/silver_ingot.png'] = recolor(j.img('item/gold_ingot'), 'silver')
    out['item/silver_nugget.png'] = recolor(j.img('item/gold_nugget'), 'silver')
    for alloy, ramp in ALLOYS.items():
        out[f'block/{alloy}_block.png'] = recolor(j.img('block/gold_block'), ramp)
        out[f'item/{alloy}_ingot.png'] = recolor(j.img('item/gold_ingot'), ramp)
        out[f'item/{alloy}_pressling.png'] = pressing(j.img('item/gold_ingot'), ramp)
        for part in ITEM_TOOLS:
            gold, iron = j.img(f'item/golden_{part}'), j.img(f'item/iron_{part}')
            out[f'item/{alloy}_{part}.png'] = recolor(gold, ramp, differs_from(iron))
        for part in ITEM_ARMOR:
            out[f'item/{alloy}_{part}.png'] = recolor(j.img(f'item/golden_{part}'), ramp)
        for layer in ('humanoid', 'humanoid_leggings'):
            src = j.first(f'entity/equipment/{layer}/gold', f'entity/equipment/{layer}/golden')
            out[f'entity/equipment/{layer}/{alloy}.png'] = recolor(src, ramp)
    out.update(lead_images(j, LEAD_VARIANT))
    out.update(arrow_images(j, ARROW_VARIANT))
    out['block/silver_mirror.png'] = mirror_image(j, MIRROR_VARIANT)
    return out


def derived_hammers(_jar=None) -> dict:
    """assets/simplehammers/textures/<key>: the two optional rose gold sledgehammers from the committed gold hammer."""
    gold, iron = Image.open(HAMMER_TEX / 'gold_sledgehammer.png').convert('RGBA'), Image.open(HAMMER_TEX / 'iron_sledgehammer.png').convert('RGBA')
    return {f'item/{alloy}_sledgehammer.png': recolor(gold, ramp, differs_from(iron)) for alloy, ramp in ALLOYS.items()}


def png(im: Image.Image) -> bytes:
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return buf.getvalue()


def preview(jar, path: Path) -> None:
    images = derived(jar)
    cells = ''.join(f'<figure><img src="data:image/png;base64,{base64.b64encode(png(im.resize((im.width * 6, im.height * 6), Image.NEAREST))).decode()}">'
                    f'<figcaption>{rel}</figcaption></figure>' for rel, im in images.items() if im.width <= 16 and im.height <= 16)
    path.write_text(f'<!doctype html><meta charset=utf-8><style>figure{{display:inline-block;margin:6px;font:11px sans-serif}}img{{image-rendering:pixelated}}</style>{cells}', encoding='utf-8')


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', type=Path)
    ap.add_argument('--jar')
    ap.add_argument('--preview', type=Path)
    a = ap.parse_args()
    if a.preview:
        preview(a.jar, a.preview)
        return
    for rel, im in derived(a.jar).items():
        target = a.out / 'simpleores' / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        im.save(target)
    for rel, im in derived_hammers().items():
        target = a.out / 'simplehammers' / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        im.save(target)


if __name__ == '__main__':
    main()
