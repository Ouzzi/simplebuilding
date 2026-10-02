"""Usage: python tools/textures/sage_ore_smaller_2026_10_02.py <vanilla textures dir> [preview png]

Owner 2026-10-02: the Sage Ore texture "a little smaller and animated more slowly".
- Sage Ore / Deepslate Sage Ore: the settled patterns (sage_ore_textures.ore and proposals_v3.sage_ore_a) with
  their darkest speck tier removed (the stone of the vanilla base shows there again, ~1/6 less ore area); the
  remaining speck pixels are re-shaded by brightness rank onto the full green ramp, so every speck keeps its dark edge.
- Sage Orb (the only animated sage texture): the same 12 pulse frames, frametime 1 -> 2 (pulse twice as slow).
Writes into the 26.3 overlay and saves a labelled before/after preview (built from the settled generators, so a
second run shows the same comparison)."""
import json
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build', 'weisheitserz-vorher-nachher.png')
sys.argv = [sys.argv[0], 'unused', V]
import sage_ore_textures as sage  # noqa: E402
import proposals_v3_2026_10_02 as v3  # noqa: E402

v3.V = V
T = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures')
ORB_FRAMETIME = 2


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def is_speck(p):
    return max(p[:3]) - min(p[:3]) > 30


def smaller(ore, base):
    """Drop the darkest speck tier, re-shade the rest by rank onto the original tiers (dark -> light)."""
    pts = [(x, y) for y in range(16) for x in range(16) if is_speck(ore.getpixel((x, y)))]
    tiers = sorted({ore.getpixel(p)[:3] for p in pts}, key=lum)
    darkest = tiers[0]
    out = ore.copy()
    keep = []
    for p in pts:
        if ore.getpixel(p)[:3] == darkest:
            out.putpixel(p, base.getpixel(p))
        else:
            keep.append(p)
    keep.sort(key=lambda p: (lum(ore.getpixel(p)), p[1], p[0]))
    # proportions of the original tiers, applied to the smaller set
    counts = [sum(1 for p in pts if ore.getpixel(p)[:3] == t) for t in tiers]
    bounds, acc = [], 0
    for c in counts:
        acc += c
        bounds.append(acc / len(pts))
    for i, p in enumerate(keep):
        q = (i + 0.5) / len(keep)
        tier = next(k for k, b in enumerate(bounds) if q <= b)
        out.putpixel(p, tiers[tier] + (255,))
    return out


def orb_mcmeta():
    with open(os.path.join(T, 'item', 'sage_orb.png.mcmeta'), 'w', encoding='utf-8') as f:
        json.dump({'animation': {'frametime': ORB_FRAMETIME, 'interpolate': True}}, f, indent=2)
        f.write('\n')


def preview(pairs, path):
    s = 10
    cell = 16 * s + 14
    im = Image.new('RGBA', (len(pairs) * 2 * cell + 20, 16 * s + 70), (44, 44, 44, 255))
    d = ImageDraw.Draw(im)
    x = 10
    for name, before, after, note in pairs:
        for tag, tex in (('A vorher', before), ('B nachher', after)):
            d.text((x, 8), f'{name} - {tag}', fill=(235, 235, 235, 255))
            im.alpha_composite(tex.resize((16 * s, 16 * s), Image.NEAREST), (x, 26))
            x += cell
        d.text((x - 2 * cell, 16 * s + 34), note, fill=(200, 200, 200, 255))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    im.save(path)


def main():
    stone_old = sage.ore('stone', 'diamond_ore')
    deep_old = v3.sage_ore_a()
    stone_base = Image.open(os.path.join(V, 'block', 'diamond_ore.png')).convert('RGBA')
    plain_stone = Image.open(os.path.join(V, 'block', 'stone.png')).convert('RGBA')
    for y in range(16):  # where diamond ore has its own specks, fall back to plain stone
        for x in range(16):
            if is_speck(stone_base.getpixel((x, y))):
                stone_base.putpixel((x, y), plain_stone.getpixel((x, y)))
    deep_base = Image.open(os.path.join(V, 'block', 'deepslate.png')).convert('RGBA')
    stone_new, deep_new = smaller(stone_old, stone_base), smaller(deep_old, deep_base)
    stone_new.save(os.path.join(T, 'block', 'sage_ore.png'))
    deep_new.save(os.path.join(T, 'block', 'deepslate_sage_ore.png'))
    orb_mcmeta()
    orb = Image.open(os.path.join(T, 'item', 'sage_orb.png')).convert('RGBA').crop((0, 0, 16, 16))
    n = lambda im: sum(1 for y in range(16) for x in range(16) if is_speck(im.getpixel((x, y))))
    preview([('Weisheitserz', stone_old, stone_new, f'Erzpixel {n(stone_old)} -> {n(stone_new)}'),
             ('Tiefenschiefer', deep_old, deep_new, f'Erzpixel {n(deep_old)} -> {n(deep_new)}'),
             ('Weisheitskugel', orb, orb, f'Puls: frametime 1 -> {ORB_FRAMETIME} (12 Frames, {12 * ORB_FRAMETIME} Ticks)')],
            PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
