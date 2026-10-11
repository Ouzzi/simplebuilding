"""Deceiver stage 2 textures (own pixel art): cloak of deception (item + worn layer), trick mirror, illusion shard.

    python tools/textures/deceiver2_textures.py          # write PNGs + previews
    python tools/textures/deceiver2_textures.py --check  # fail if the committed PNGs differ

Worn layer uses the vanilla humanoid armor UV (64x32): body 8x12x4 at (16,16), arms 4x12x4 at (40,16).
Palette shared with deceiver_textures.py (same cloth as the Deceiver's robe).
"""
import os
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from deceiver_textures import PAL, ROBE, DEEP, TRIM, TRIM_D, EYE, tone  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs'
PREVIEW = Path(os.environ.get('SB_PREVIEW_DIR', os.path.expanduser('~/previews')) + '/deceiver2')

P2 = dict(PAL)
P2.update({'m': (0xa6, 0x8c, 0xe0), 'n': (0x74, 0x5c, 0xb4), 'o': (0x4e, 0x3c, 0x86),
           's': (0x7a, 0x55, 0x30), 'S': (0x4f, 0x36, 0x1e), 'y': (0xe8, 0xcf, 0x7a)})


def draw(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c in P2: img.putpixel((x, y), P2[c] + (255,))
    return img


def cloak_item():
    # A hooded cloak hanging from its clasp: dark hood opening, gold clasp with the watching eye, torn hem.
    return draw([
        "................",
        "......kkkk......",
        ".....kbccdk.....",
        "....kbcaacdk....",
        "....kbaaaacdk...",
        "....kcbaabcdk...",
        "...kbcgyegcbk...",
        "...kbcbhhbccdk..",
        "..kbccbccbcdck..",
        "..kbcbcbbcbcdk..",
        "..kbccbccbccdk..",
        ".kbcbccbbccbcdk.",
        ".kbbcbcbccbccdk.",
        ".kbcbkccbkcbcck.",
        ".kbk.kbk.kck.kk.",
        "..k...k...k.....",
    ])


def mirror_item():
    # Hand mirror: gold oval frame, violet glass with a glint, wooden handle to the lower left.
    return draw([
        "................",
        ".........gggg...",
        "........gmmnng..",
        ".......gmwmnnoh.",
        ".......gmmwnnoh.",
        ".......gmmnnooh.",
        ".......gmnnnooh.",
        "........hnnooh..",
        ".......ghhhhh...",
        "......gh........",
        ".....sS.........",
        "....sS..........",
        "...sS...........",
        "..sS............",
        "..S.............",
        "................",
    ])


def shard_item():
    # A long splinter of violet glass with a gold core vein, a second small chip beside it.
    return draw([
        "................",
        "..........kk....",
        ".........kmwk...",
        "........kmwnk...",
        ".......kmwnok...",
        "......kmmyok....",
        ".....kmnyok.....",
        "....kmnyok......",
        "...kmnyok.......",
        "...knyok..kk....",
        "..knook..kmnk...",
        "..kook...knok...",
        "..kkk.....kk....",
        "................",
        "................",
        "................",
    ])


def worn():
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))

    def face(u, v, w, h, k, hem=True):
        for y in range(h):
            for x in range(w):
                c = tone(u + x, v + y, ROBE, k)
                if hem and y == h - 1 and (x * 7 + k) % 3 == 0: continue          # torn hem
                if hem and y >= h - 2 and (x * 5 + k) % 4 == 0: c = DEEP
                img.putpixel((u + x, v + y), c + (255,))

    # body box 8x12x4 at (16,16): top, bottom, right, front, left, back
    face(20, 16, 8, 4, 1, False); face(28, 16, 8, 4, 2, False)
    face(16, 20, 4, 12, 3); face(20, 20, 8, 12, 4); face(28, 20, 4, 12, 5); face(32, 20, 8, 12, 6)
    # arms 4x12x4 at (40,16): wide sleeves
    face(44, 16, 4, 4, 7, False); face(48, 16, 4, 4, 8, False)
    for i, u in enumerate((40, 44, 48, 52)): face(u, 20, 4, 12, 9 + i)
    # front: gold clasp across the collar with the watching eye, dark seam down the middle
    for x in range(20, 28): img.putpixel((x, 20), TRIM + (255,))
    for x in (22, 25): img.putpixel((x, 21), TRIM_D + (255,))
    img.putpixel((23, 21), EYE[0] + (255,)); img.putpixel((24, 21), EYE[1] + (255,))
    for y in range(22, 31): img.putpixel((23 + (y % 2), y), DEEP + (255,))
    # back: hood hanging down
    for y in range(20, 24):
        for x in range(34 - (y - 20) // 2, 38 + (y - 20) // 2): img.putpixel((x, y), ROBE[2] + (255,))
    return img


def outputs():
    return {
        RES / 'textures/item/deceiver_cloak.png': cloak_item(),
        RES / 'textures/item/trick_mirror.png': mirror_item(),
        RES / 'textures/item/illusion_shard.png': shard_item(),
        RES / 'textures/entity/equipment/humanoid/deceiver_cloak.png': worn(),
    }


def preview(items):
    PREVIEW.mkdir(parents=True, exist_ok=True)
    sheet = Image.new('RGBA', (16 * 8 * 2 + 64 * 8 + 60, 300), (60, 60, 66, 255))
    for i, n in enumerate(['deceiver_cloak', 'trick_mirror', 'illusion_shard']):
        t = items[RES / f'textures/item/{n}.png'].resize((128, 128), Image.NEAREST)
        sheet.paste(t, (10 + (i % 2) * 140, 10 + (i // 2) * 140), t)
    w = items[RES / 'textures/entity/equipment/humanoid/deceiver_cloak.png'].resize((512, 256), Image.NEAREST)
    sheet.paste(w, (300, 10), w)
    sheet.save(PREVIEW / 'textures.png')


def main():
    items = outputs()
    if '--check' in sys.argv:
        bad = [str(p) for p, im in items.items() if not p.is_file() or Image.open(p).convert('RGBA').tobytes() != im.tobytes()]
        if bad:
            print('stale:', *bad, sep='\n  '); sys.exit(1)
        print('deceiver2 textures current'); return
    for p, im in items.items():
        p.parent.mkdir(parents=True, exist_ok=True); im.save(p)
    preview(items)


if __name__ == '__main__':
    main()
