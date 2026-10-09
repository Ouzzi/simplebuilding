"""Soul lava, round 2 (owner 2026-10-09: "Animation vom Seelenlava ist nicht gut, Textur und Animation ueberarbeiten").

The textures follow Vanilla lava frame by frame: every pixel keeps the brightness rank it has in the Vanilla
lava_still / lava_flow animation (read at run time from the 26.3 client jar in the Gradle cache, never checked
in), so the motion is exactly as calm as Vanilla's. Only the colours change: the rank is mapped onto a soul fire
ramp (soul soil browns for the coolest crust, soul lantern teal for the body, pale soul fire cyan for the hottest
streaks). Single highlights brighter than their neighbourhood are flattened, so no lone bright pixel flickers.

* still: 20 frames, ping-pong 0..19..1, frametime 2 (as Vanilla);
* flow: 16 frames, forward loop 0..15, frametime 3 (as Vanilla; N29: the ping-pong made it run back and forth);
* iron soul lava bucket: the liquid tones of the Vanilla lava bucket are swapped 1:1 for tones of the same ramp
  (SOUL_FOR_LAVA, shared with crucible_art_v2_2026_10_05.py and enderite_bucket_fill_2026_10_09.py, which writes
  the Enderite soul lava buckets).

Run from the repository root (Pillow + numpy):
  python3.12 tools/textures/soul_lava_2026_10_09.py              write textures + .mcmeta
  python3.12 tools/textures/soul_lava_2026_10_09.py --check      compare only
  python3.12 tools/textures/soul_lava_2026_10_09.py --preview DIR [--old DIR]   GIF + contact sheet (old vs new)
"""
from __future__ import annotations

import argparse
import glob
import io
import json
import os
import sys
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures'

# Colour stops over the brightness rank (0 = coolest pixel of the whole animation, 1 = hottest). Round 3 (owner
# 2026-10-09 evening: "geht ins Roetliche", keep it blue like soul fire): body and streaks from the Vanilla soul_fire
# palette (1,128,133)..(1,198,204)..pale cyan, the coolest crust dark blue / petrol; no brown, red or orange, no white.
STOPS = [
    (0.00, (10, 38, 66)),     # dark blue, only the very coolest crust spots
    (0.025, (6, 62, 90)),     # petrol crust cooling into the liquid
    (0.09, (1, 104, 120)),    # deep soul teal
    (0.35, (1, 130, 136)),    # soul fire, darkest tone
    (0.65, (1, 152, 158)),
    (0.86, (1, 188, 195)),    # soul fire body
    (0.96, (40, 226, 233)),
    (1.00, (110, 248, 252)),  # soul fire highlight (not white)
]
SPIKE = 0.06  # max rank a pixel may stand above the mean of its 8 neighbours (no lone bright pixels)
DIP = 0.15    # max rank a pixel may fall below it (no lone dark crust specks)

# Vanilla lava bucket liquid tones -> soul tones of the same rank (the two greyish rim tones -> cool blue-grey rim).
SOUL_FOR_LAVA = {
    (127, 62, 44): (6, 62, 90),
    (204, 70, 40): (1, 130, 136),
    (227, 140, 63): (1, 188, 195),
    (228, 210, 92): (110, 248, 252),
    (159, 127, 120): (108, 138, 158),
    (182, 140, 123): (126, 162, 182),
}

KINDS = {'still': (16, 2), 'flow': (32, 3)}  # frame size, frametime


def client_jar() -> Path:
    home = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle'))
    jars = sorted(glob.glob(str(home / 'caches/neoformruntime/artifacts/minecraft_26.3_client.jar')))
    jars += sorted(glob.glob(str(home / 'caches/fabric-loom/26.3/minecraft-client.jar')))
    jars += sorted(glob.glob(str(home / 'caches/minecraftforge/**/26.3/client.jar'), recursive=True))
    if not jars:
        sys.exit('26.3 client jar not found in the Gradle cache (run any 26.3 build once)')
    return Path(jars[0])


def vanilla(name: str) -> Image.Image:
    with zipfile.ZipFile(client_jar()) as z:
        data = z.read(f'assets/minecraft/textures/{name}.png')
    return Image.open(io.BytesIO(data)).convert('RGBA')


def ramp(t: np.ndarray) -> np.ndarray:
    xs = [s for s, _ in STOPS]
    cols = np.array([c for _, c in STOPS], dtype=float)
    return np.stack([np.interp(t, xs, cols[:, ch]) for ch in range(3)], axis=-1)


def soul_lava(kind: str) -> Image.Image:
    size, _ = KINDS[kind]
    src = np.array(vanilla('block/lava_' + kind)).astype(float)
    frames = src.shape[0] // size
    lu = src[..., :3] @ np.array([.299, .587, .114])
    # Brightness rank over the whole animation (not per frame), so a pixel only changes colour when Vanilla's does.
    order = lu.ravel().argsort(kind='stable')
    rank = np.empty(order.size)
    rank[order] = np.arange(order.size)
    t = (rank / (order.size - 1)).reshape(lu.shape)
    # Ties share a rank (same Vanilla brightness -> same colour).
    flat = lu.ravel()
    for v in np.unique(flat):
        m = lu == v
        t[m] = t[m].mean()
    out = np.zeros((*lu.shape, 4), dtype=np.uint8)
    for f in range(frames):
        tf = t[f * size:(f + 1) * size]
        nb = sum(np.roll(np.roll(tf, dy, 0), dx, 1) for dy in (-1, 0, 1) for dx in (-1, 0, 1) if dy or dx) / 8
        tf = np.clip(tf, nb - DIP, nb + SPIKE)
        out[f * size:(f + 1) * size, :, :3] = np.rint(ramp(tf)).astype(np.uint8)
    out[..., 3] = 255
    return Image.fromarray(out)


def mcmeta(kind: str, frames: int) -> str:
    _, frametime = KINDS[kind]
    if kind == 'flow':  # N29: one direction only, a plain forward loop like Vanilla lava_flow (seamless 15 -> 0)
        return json.dumps({'animation': {'frametime': frametime}}, indent=2) + '\n'
    order = list(range(frames)) + list(range(frames - 2, 0, -1))
    return json.dumps({'animation': {'frametime': frametime, 'frames': order}}, indent=2) + '\n'


def swap(img: Image.Image, table: dict) -> Image.Image:
    a = np.array(img.convert('RGBA'))
    hits = [((a[..., 0] == s[0]) & (a[..., 1] == s[1]) & (a[..., 2] == s[2]) & (a[..., 3] > 0), d) for s, d in table.items()]
    for hit, dst in hits:
        a[hit, :3] = dst
    return Image.fromarray(a)


def outputs() -> dict:
    out = {}
    for kind in KINDS:
        img = soul_lava(kind)
        out[TEX / f'block/soul_lava_{kind}.png'] = img
        out[TEX / f'block/soul_lava_{kind}.png.mcmeta'] = mcmeta(kind, img.height // KINDS[kind][0])
    out[TEX / 'item/soul_lava_bucket.png'] = swap(vanilla('item/lava_bucket'), SOUL_FOR_LAVA)
    return out


def same(path: Path, value) -> bool:
    if not path.exists():
        return False
    if isinstance(value, str):
        return path.read_text(encoding='utf-8').replace('\r\n', '\n') == value
    with Image.open(path) as cur:
        return cur.convert('RGBA').tobytes() == value.tobytes() and cur.size == value.size


# ------------------------------------------------------------------------------------------------- preview

def frames_of(img: Image.Image, size: int) -> list:
    return [img.crop((0, i * size, size, (i + 1) * size)) for i in range(img.height // size)]


def preview(dest: Path, old: Path | None) -> None:
    dest.mkdir(parents=True, exist_ok=True)
    new = {k: Image.open(TEX / f'block/soul_lava_{k}.png').convert('RGBA') for k in KINDS}
    van = {k: vanilla('block/lava_' + k) for k in KINDS}
    rows = [('Vanilla', van)]
    if old:
        rows.insert(0, ('alt', {k: Image.open(old / f'soul_lava_{k}.png').convert('RGBA') for k in KINDS}))
    rows.append(('neu', new))
    # Animated GIF: per row still (2x2 tiled, 8x) and flow (8x), in-game timing; still ping-pong, flow forward
    # (as Vanilla and the .mcmeta).
    scale, pad = 8, 12
    cell = 32 * scale
    w = pad + 2 * (cell + pad)
    h = pad + len(rows) * (cell + pad + 14)
    gif = []
    for t in range(0, 228, 2):  # 2 game ticks per GIF frame = 100 ms, 11.4 s (three still loops)
        canvas = Image.new('RGB', (w, h), (24, 24, 24))
        d = ImageDraw.Draw(canvas)
        for r, (label, imgs) in enumerate(rows):
            y = pad + r * (cell + pad + 14)
            d.text((pad, y), f'{label}: still | flow', fill=(230, 230, 230))
            for c, kind in enumerate(('still', 'flow')):
                size, ft = KINDS[kind]
                fr = frames_of(imgs[kind], size)
                n = len(fr)
                pingpong = kind == 'still'
                seq = list(range(n)) + list(range(n - 2, 0, -1)) if pingpong else list(range(n))
                f = fr[seq[(t // ft) % len(seq)]]
                if kind == 'still':
                    tile = Image.new('RGBA', (32, 32))
                    for ty in (0, 16):
                        for tx in (0, 16):
                            tile.paste(f, (tx, ty))
                    f = tile
                canvas.paste(f.resize((cell, cell), Image.NEAREST).convert('RGB'), (pad + c * (cell + pad), y + 14))
        gif.append(canvas)
    gif[0].save(dest / 'soullava-flow.gif', save_all=True, append_images=gif[1:], duration=100, loop=0)
    # Contact sheet: every frame of still and flow, old / Vanilla / new.
    sheets = []
    for label, imgs in rows:
        for kind, sc in (('still', 6), ('flow', 3)):
            size, _ = KINDS[kind]
            fr = frames_of(imgs[kind], size)
            strip = Image.new('RGB', (len(fr) * (size * sc + 2) + 90, size * sc), (24, 24, 24))
            ImageDraw.Draw(strip).text((2, 2), f'{label}\n{kind}', fill=(230, 230, 230))
            for i, f in enumerate(fr):
                strip.paste(f.resize((size * sc, size * sc), Image.NEAREST).convert('RGB'), (90 + i * (size * sc + 2), 0))
            sheets.append(strip)
    buckets = []
    for name in ('soul_lava_bucket', 'enderite_soul_lava_bucket'):
        for label, base in (('alt', old), ('neu', TEX / 'item')):
            if base and (base / f'{name}.png').exists():
                im = Image.open(base / f'{name}.png').convert('RGBA').crop((0, 0, 16, 16))
                bg = Image.new('RGBA', (16, 16), (60, 60, 60, 255))
                buckets.append(Image.alpha_composite(bg, im).resize((128, 128), Image.NEAREST).convert('RGB'))
    if buckets:
        strip = Image.new('RGB', (90 + len(buckets) * 132, 128), (24, 24, 24))
        ImageDraw.Draw(strip).text((2, 2), 'Eimer\nalt|neu\neisen,\nenderit', fill=(230, 230, 230))
        for i, b in enumerate(buckets):
            strip.paste(b, (90 + i * 132, 0))
        sheets.append(strip)
    sw = max(s.width for s in sheets)
    sheet = Image.new('RGB', (sw, sum(s.height + 6 for s in sheets)), (12, 12, 12))
    y = 0
    for s in sheets:
        sheet.paste(s, (0, y))
        y += s.height + 6
    sheet.save(dest / 'soullava-frames.png')


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--preview', type=Path)
    ap.add_argument('--old', type=Path)
    args = ap.parse_args()
    if args.preview:
        preview(args.preview, args.old)
        return 0
    bad = []
    for path, value in outputs().items():
        if args.check:
            if not same(path, value):
                bad.append(path)
        elif isinstance(value, str):
            path.write_text(value, encoding='utf-8', newline='\n')
        else:
            value.save(path)
    for path in bad:
        print('out of date:', path.relative_to(ROOT))
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
