"""Sulfur torch, sulfur lantern and sulfur campfire textures (queue N58, follow-up of the sulfur fire).

All pixels are drawn here (no Vanilla input), so the PNGs are committed like the other hand-drawn textures. The look is
the sulfur fire's yellow-green: a crystal lump on the torch, a bronze lantern with a sulfur crystal core, a campfire with
a flickering yellow-green flame over logs whose cracks glow. New files only - no accepted texture changes.

Run: python3.12 tools/textures/sulfur_lights_2026_10_15.py [--check] [--preview DIR]
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures'

# sulfur crystal ramp 1..5 (dark olive -> pale yellow) and bronze frame d,m,l,h; wood w,x,y
PAL = {
    '1': (84, 100, 10), '2': (140, 168, 18), '3': (194, 220, 40), '4': (234, 244, 100), '5': (252, 254, 180),
    'd': (52, 40, 22), 'm': (98, 74, 34), 'l': (146, 112, 52), 'h': (190, 152, 78),
    'w': (84, 58, 30), 'x': (116, 82, 42), 'y': (146, 106, 56),
    'k': (34, 32, 22), 'g': (60, 56, 30),
}


def from_map(rows: list[str]) -> Image.Image:
    im = Image.new('RGBA', (16, len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row.ljust(16, '.')[:16]):
            if ch != '.':
                im.putpixel((x, y), PAL[ch] + (255,))
    return im


def stack(frames: list[Image.Image]) -> Image.Image:
    out = Image.new('RGBA', (16, 16 * len(frames)), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        out.paste(f, (0, 16 * i))
    return out


# ---- torch: the model maps columns 7-8, rows 6-15 (head rows 6-8); crystal lump crusting down a wood stick ----
TORCH = [
    '................', '................', '................', '................', '................', '................',
    '.......45.......',
    '.......34.......',
    '.......23.......',
    '.......x2.......',
    '.......y2.......',
    '.......x3.......',
    '.......yw.......',
    '.......xw.......',
    '.......yw.......',
    '.......xw.......',
]
TORCH[11] = '.......xw.......'
TORCH[12] = '.......y3.......'


# ---- lantern: 3 animation frames, the Vanilla lantern UV layout (side 0-5 x 2-8, caps, top/bottom, chain links) ----
def lantern_frame(shift: int) -> Image.Image:
    core = ['3443', '2432', '3553', '2442', '1231']
    ramp = '12345'

    def tone(c):
        return ramp[max(0, min(4, ramp.index(c) + shift))]

    rows = [
        '.dmmd...........',
        '.mllm......dmd..',
        'dhllhd.....m.m..',
        'd' + ''.join(tone(c) for c in core[0]) + 'd.....m.m..',
        'm' + ''.join(tone(c) for c in core[1]) + 'm.....dmd..',
        'm' + ''.join(tone(c) for c in core[2]) + 'm..........',
        'm' + ''.join(tone(c) for c in core[3]) + 'm.....m.m..',
        'm' + ''.join(tone(c) for c in core[4]) + 'm.....dmd..',
        'dlllld..........',
        'dmllmd..........',
        'mlhhlm.....dmd..',
        'mlhhlm.....m.m..',
        'mlhhlm..........',
        'dmllmd..........',
        '.dmmd...........',
        '................',
    ]
    return from_map(rows)


LANTERN_ITEM = [
    '................', '......dmd.......', '.....m...m......', '.....m...m......', '....dmmmmmd.....', '....mlhhhlm.....',
    '....m34543d.....', '....m24542d.....', '....m35553d.....', '....m24442d.....', '....m12321d.....', '....dlllllm.....',
    '.....dmmmd......', '................', '................', '................',
]

CAMPFIRE_ITEM = [
    '................', '................', '.......3........', '......343.......', '.....24542......', '.....34543......', '....2345432.....', '....3455543.....',
    '...234555432....', '...xyyyyyyyx....', '..xwwwwwwwwwx...', '.dxy2yyyy3yyxd..', '.dxwwwwwwwwwxd..', '..dxyyyyyyyxd...', '...ddddddddd....', '................',
]


def fire_frame(f: int, frames: int = 8) -> Image.Image:
    """One flame frame: a tall centre tongue and two side tongues that sway with the frame."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    t = 2 * math.pi * f / frames
    for x in range(16):
        d = (x - 7.5 + 0.9 * math.sin(t)) / 3.9
        h = 15.0 * math.exp(-d * d) * (0.78 + 0.22 * math.sin(2.1 * x + t * 1.0))
        h += 2.2 * math.sin(1.7 * x + 2 * t) * (1 if abs(d) < 1.6 else 0)
        h = 0 if h < 2.5 else min(15, h)
        for y in range(16):
            up = 15 - y  # height above the bottom row
            if up >= h:
                continue
            rel = up / max(h, 1)
            core = 1 - min(1.0, abs(x - 7.5 + 0.6 * math.sin(t)) / 3.2)
            v = 0.55 * (1 - rel) + 0.55 * core * (1 - rel * 0.7)
            idx = '1' if v < 0.22 else '2' if v < 0.42 else '3' if v < 0.64 else '4' if v < 0.86 else '5'
            im.putpixel((x, y), PAL[idx] + (255,))
    return im


def log_frame(f: int) -> Image.Image:
    """Charred bark (16x16, Vanilla campfire UV uses rows 0-8) with sulfur-glowing cracks that pulse per frame."""
    im = Image.new('RGBA', (16, 16), PAL['k'] + (255,))
    for y in range(16):
        for x in range(16):
            n = (x * 7 + y * 13 + (x // 3) * 5) % 11
            col = PAL['g'] if n < 3 else PAL['k'] if n < 8 else PAL['w']
            im.putpixel((x, y), col + (255,))
    for y in (3, 7, 11, 14):
        for x in range(16):
            if (x + f + y) % 3 != 0 and (x * 5 + y) % 4 != 1:
                tone = '3' if (x + f) % 5 == 0 else '2' if (x + y + f) % 3 else '1'
                im.putpixel((x, y), PAL[tone] + (255,))
    return im


def build() -> dict[str, Image.Image]:
    out = {
        'block/sulfur_torch.png': from_map(TORCH),
        'block/sulfur_lantern.png': stack([lantern_frame(s) for s in (0, 1, 0)]),
        'item/sulfur_lantern.png': from_map(LANTERN_ITEM),
        'item/sulfur_campfire.png': from_map(CAMPFIRE_ITEM),
        'block/sulfur_campfire_fire.png': stack([fire_frame(i) for i in range(8)]),
        'block/sulfur_campfire_log_lit.png': stack([log_frame(i) for i in range(4)]),
    }
    return out


MCMETA = {
    'block/sulfur_lantern.png': '{\n  "animation": {\n    "frametime": 8\n  }\n}\n',
    'block/sulfur_campfire_fire.png': '{\n  "animation": {\n    "frametime": 2\n  }\n}\n',
    'block/sulfur_campfire_log_lit.png': '{\n  "animation": {\n    "frametime": 4\n  }\n}\n',
}


def png_bytes(im: Image.Image) -> bytes:
    import io
    b = io.BytesIO()
    im.save(b, 'PNG')
    return b.getvalue()


def preview(dst: Path, tex: dict[str, Image.Image]) -> None:
    dst.mkdir(parents=True, exist_ok=True)
    s = 12
    sheet = Image.new('RGBA', (16 * s * 8 + 20, 16 * s * 4 + 40), (70, 70, 70, 255))

    def put(im, x, y):
        big = im.resize((im.width * s, im.height * s), Image.NEAREST)
        sheet.paste(big, (x, y), big)

    put(tex['block/sulfur_torch.png'], 4, 4)
    put(tex['item/sulfur_lantern.png'], 16 * s + 12, 4)
    put(tex['item/sulfur_campfire.png'], 32 * s + 20, 4)
    lan = tex['block/sulfur_lantern.png']
    for i in range(3):
        put(lan.crop((0, 16 * i, 16, 16 * i + 16)), (48 + 16 * i) * s + 28 + 8 * i, 4)
    fire = tex['block/sulfur_campfire_fire.png']
    for i in range(8):
        put(fire.crop((0, 16 * i, 16, 16 * i + 16)), 16 * s * i + 4, 16 * s + 16)
    log = tex['block/sulfur_campfire_log_lit.png']
    for i in range(4):
        put(log.crop((0, 16 * i, 16, 16 * i + 16)), 16 * s * i + 4, 32 * s + 28)
    sheet.save(dst / 'sulfur_lights_12x.png')


def main(argv: list[str]) -> int:
    tex = build()
    check = '--check' in argv
    bad = 0
    for rel, im in tex.items():
        p = TEX / rel
        if check:
            if not p.exists() or png_bytes(Image.open(p).convert('RGBA')) != png_bytes(im):
                print('stale:', rel)
                bad += 1
        else:
            p.parent.mkdir(parents=True, exist_ok=True)
            im.save(p)
    for rel, text in MCMETA.items():
        p = TEX / (rel + '.mcmeta')
        if check:
            if not p.exists() or p.read_text() != text:
                print('stale:', rel + '.mcmeta')
                bad += 1
        else:
            p.write_text(text)
    if '--preview' in argv:
        preview(Path(argv[argv.index('--preview') + 1]), tex)
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
