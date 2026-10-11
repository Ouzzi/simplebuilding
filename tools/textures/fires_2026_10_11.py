"""Sulfur fire and copper fire (queue N33): animated fire textures plus their blockstates and models.

Like the soul lava script, the flames follow Vanilla frame by frame: every opaque pixel keeps the brightness rank it
has in the Vanilla fire_0 / fire_1 animation (read at run time from the 26.3 client jar in the Gradle cache, never
checked in); only the colours change. The ramps sit between orange fire and blue soul fire:
* sulfur fire: olive -> lemon -> yellow-green -> pale yellow (yellow-greenish);
* copper fire: dark green -> leaf green -> mint (the green flame of copper).
The frame order (.mcmeta) is Vanilla soul_fire's (fire_1 is a plain loop). Blockstates and models are the Vanilla
soul fire ones pointed at the new textures, with render_type cutout.

Run from the repository root (Pillow + numpy):
  python3.12 tools/textures/fires_2026_10_11.py              write textures, .mcmeta, blockstates, models
  python3.12 tools/textures/fires_2026_10_11.py --check      compare only
  python3.12 tools/textures/fires_2026_10_11.py --preview DIR  contact sheet (Vanilla fire, soul fire, new fires, 16x)
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
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding'

RAMPS = {
    'sulfur_fire': [
        (0.00, (86, 84, 6)),
        (0.15, (104, 120, 10)),
        (0.40, (146, 180, 16)),
        (0.70, (190, 224, 34)),
        (0.92, (240, 246, 90)),
        (1.00, (252, 252, 168)),
    ],
    'copper_fire': [
        (0.00, (6, 74, 36)),
        (0.15, (12, 112, 52)),
        (0.40, (24, 158, 74)),
        (0.70, (58, 206, 104)),
        (0.92, (130, 240, 152)),
        (1.00, (206, 252, 204)),
    ],
}
VARIANTS = ('0', '1')


def client_jar() -> Path:
    home = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle'))
    jars = sorted(glob.glob(str(home / 'caches/fabric-loom/26.3/minecraft-client.jar')))
    jars += sorted(glob.glob(str(home / 'caches/neoformruntime/artifacts/minecraft_26.3_client.jar')))
    jars += sorted(glob.glob(str(home / 'caches/minecraftforge/**/26.3/client.jar'), recursive=True))
    if not jars:
        sys.exit('26.3 client jar not found in the Gradle cache (run any 26.3 build once)')
    return Path(jars[0])


def vanilla(path: str) -> bytes:
    with zipfile.ZipFile(client_jar()) as z:
        return z.read('assets/minecraft/' + path)


def vanilla_img(name: str) -> Image.Image:
    return Image.open(io.BytesIO(vanilla(f'textures/block/{name}.png'))).convert('RGBA')


def ramp(t: np.ndarray, stops) -> np.ndarray:
    xs = [s for s, _ in stops]
    cols = np.array([c for _, c in stops], dtype=float)
    return np.stack([np.interp(t, xs, cols[:, ch]) for ch in range(3)], axis=-1)


def recolor(variant: str, stops) -> Image.Image:
    src = np.array(vanilla_img('fire_' + variant)).astype(float)
    lu = src[..., :3] @ np.array([.299, .587, .114])
    opaque = src[..., 3] > 0
    vals = np.unique(lu[opaque])  # equal Vanilla brightness -> equal colour
    t = np.zeros(lu.shape)
    t[opaque] = np.searchsorted(vals, lu[opaque]) / max(len(vals) - 1, 1)
    out = np.zeros(src.shape, dtype=np.uint8)
    out[..., :3] = np.rint(ramp(t, stops)).astype(np.uint8)
    out[..., 3] = src[..., 3].astype(np.uint8)
    out[~opaque, :3] = 0
    return Image.fromarray(out)


def png_bytes(img: Image.Image) -> bytes:
    buf = io.BytesIO()
    img.save(buf, 'PNG', optimize=True)
    return buf.getvalue()


def models(name: str) -> dict:
    out = {}
    for v in VARIANTS:
        for kind, parent in (('floor', 'template_fire_floor'), ('side', 'template_fire_side'),
                             ('side_alt', 'template_fire_side_alt')):
            out[RES / f'models/block/{name}_{kind}{v}.json'] = json.dumps({
                'parent': f'minecraft:block/{parent}',
                'render_type': 'minecraft:cutout',
                'textures': {'fire': f'simplebuilding:block/{name}_{v}'},
            }, indent=2) + '\n'
    return out


def outputs() -> dict:
    out = {}
    state = vanilla('blockstates/soul_fire.json').decode()
    for name, stops in RAMPS.items():
        for v in VARIANTS:
            out[RES / f'textures/block/{name}_{v}.png'] = recolor(v, stops)
            meta = vanilla(f'textures/block/soul_fire_{v}.png.mcmeta').decode()
            out[RES / f'textures/block/{name}_{v}.png.mcmeta'] = meta if meta.endswith('\n') else meta + '\n'
        out[RES / f'blockstates/{name}.json'] = state.replace('minecraft:block/soul_fire', f'simplebuilding:block/{name}')
        out.update(models(name))
    return out


def same(path: Path, value) -> bool:
    if not path.exists():
        return False
    if isinstance(value, Image.Image):
        return path.read_bytes() == png_bytes(value)
    return path.read_text() == value


def preview(dest: Path) -> None:
    dest.mkdir(parents=True, exist_ok=True)
    rows = [('fire_0', vanilla_img('fire_0')), ('soul_fire_0', vanilla_img('soul_fire_0'))]
    rows += [(n + '_0', recolor('0', s)) for n, s in RAMPS.items()]
    frames = (0, 4, 8, 12, 16, 20, 24, 28)
    scale = 8
    sheet = Image.new('RGBA', (len(frames) * 16 * scale + 8 * (len(frames) + 1), len(rows) * (16 * scale + 8) + 8), (40, 40, 48, 255))
    for r, (_, img) in enumerate(rows):
        for c, f in enumerate(frames):
            tile = img.crop((0, f * 16, 16, f * 16 + 16)).resize((16 * scale, 16 * scale), Image.NEAREST)
            sheet.alpha_composite(tile, (8 + c * (16 * scale + 8), 8 + r * (16 * scale + 8)))
    sheet.save(dest / 'fires_sheet.png')
    for name, stops in RAMPS.items():
        img = recolor('0', stops)
        gif = [Image.alpha_composite(Image.new('RGBA', (16, 16), (30, 30, 36, 255)), img.crop((0, f * 16, 16, f * 16 + 16)))
               .resize((128, 128), Image.NEAREST).convert('P') for f in range(img.height // 16)]
        gif[0].save(dest / f'{name}.gif', save_all=True, append_images=gif[1:], duration=50, loop=0)
    print('preview written to', dest)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--preview', metavar='DIR')
    args = ap.parse_args()
    if args.preview:
        preview(Path(args.preview))
        return 0
    stale = []
    for path, value in outputs().items():
        if same(path, value):
            continue
        stale.append(path)
        if not args.check:
            path.parent.mkdir(parents=True, exist_ok=True)
            if isinstance(value, Image.Image):
                path.write_bytes(png_bytes(value))
            else:
                path.write_text(value)
    if args.check:
        for p in stale:
            print('stale:', p.relative_to(ROOT))
        print('fires textures: ' + ('OK' if not stale else f'{len(stale)} stale'))
        return 1 if stale else 0
    print(f'wrote {len(stale)} files')
    return 0


if __name__ == '__main__':
    sys.exit(main())
