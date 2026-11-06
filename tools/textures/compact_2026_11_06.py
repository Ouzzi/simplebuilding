"""Compact and Dense blocks (WH26-10, a-wh26-10, 06.11.2026): stage 1 "Compact" = 9 blocks, stage 2 "Dense" = 9 Compact.
Every pixel is Vanilla-derived (the Vanilla sprite of the base block, only tone and raster move), so the textures are built from
the client jar at build time and never committed (AGENTS.md rule 7; tools/textures/vanilla_derived.py, MODULE_OUTPUTS['simpleblocks']).

Three looks (A is built in, B and C only in the preview; the owner picks, then the others are dropped):
  A "Frame":   the Vanilla sprite inside a pressed edge: Compact = 1 px darker rim (corners darkest), Dense = rim + a second, lighter
               inner step. Raster and tones of the block stay recognisable.
  B "Grain":   the sprite shrunk to half (Compact, 2x2 tiles) or a quarter (Dense, 4x4 tiles) by taking every 2nd / 4th pixel, so the
               Vanilla pixels stay but the grain is finer (denser); a slight darkening.
  C "Straps":  a cross of two darker 2 px straps over the sprite (Compact); Dense adds the rim of A.
All edits are brightness factors of the Vanilla pixels (A8b: soft steps, the share of hard transitions is printed in the preview).

    python3.12 tools/textures/compact_2026_11_06.py --out DIR [--jar J]                  write the textures of variant A
    python3.12 tools/textures/compact_2026_11_06.py --preview OUT.html [--jar J]         owner page (cobblestone, 3 looks x 2 stages)
"""
from __future__ import annotations

import argparse
import base64
import io
import os
import sys
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

# Block name (without prefix) -> Vanilla sprite under textures/block/ (side sprite; sandstones use the plain top).
SPRITES = {
    'dirt': 'dirt', 'mud': 'mud', 'cobblestone': 'cobblestone', 'stone': 'stone', 'sand': 'sand', 'red_sand': 'red_sand',
    'gravel': 'gravel', 'clay': 'clay', 'netherrack': 'netherrack', 'cobbled_deepslate': 'cobbled_deepslate',
    'andesite': 'andesite', 'diorite': 'diorite', 'granite': 'granite', 'tuff': 'tuff', 'sandstone': 'sandstone_top',
    'red_sandstone': 'red_sandstone_top', 'end_stone': 'end_stone', 'blackstone': 'blackstone', 'soul_sand': 'soul_sand',
}
STAGES = ('compact', 'dense')
DESC = 'compact_2026_11_06.derived() - Vanilla sprite in a pressed rim, Compact/Dense (WH26-10)'


def outputs() -> dict:
    """Texture path (under assets/simpleblocks/textures/) -> (size, description), for vanilla_derived.MODULE_OUTPUTS."""
    return {f'block/{s}_{n}.png': ((16, 16), DESC) for n in SPRITES for s in STAGES}


def _scale(a: np.ndarray, f: np.ndarray) -> np.ndarray:
    out = a.astype(float)
    out[..., :3] = np.clip(out[..., :3] * f[..., None], 0, 255)
    return out.astype(np.uint8)


def _rim(depth: int) -> np.ndarray:
    """Distance from the edge (0 = outermost ring) for a 16x16 grid."""
    i = np.arange(16)
    d = np.minimum(i, 15 - i)
    return np.minimum(d[:, None], d[None, :]) if depth else np.zeros((16, 16))


def look_a(a: np.ndarray, stage: int) -> np.ndarray:
    d = _rim(1)
    f = np.ones((16, 16))
    f[d == 0] = 0.72 if stage == 1 else 0.60
    if stage == 2:
        f[d == 1] = 0.84
    corner = np.zeros((16, 16), bool)
    corner[[0, 0, 15, 15], [0, 15, 0, 15]] = True
    f[corner] *= 0.9
    return _scale(a, f)


def look_b(a: np.ndarray, stage: int) -> np.ndarray:
    k = 2 if stage == 1 else 4
    small = a[::k, ::k]
    out = np.tile(small, (k, k, 1))
    return _scale(out, np.full((16, 16), 0.94 if stage == 1 else 0.88))


def look_c(a: np.ndarray, stage: int) -> np.ndarray:
    f = np.ones((16, 16))
    band = [7, 8]
    f[band, :] = 0.78
    f[:, band] = 0.78
    out = _scale(a, f)
    return look_a(out, 2) if stage == 2 else out


LOOKS = {'A': look_a, 'B': look_b, 'C': look_c}


def vanilla(jar: Path, sprite: str) -> np.ndarray:
    with zipfile.ZipFile(jar) as z:
        im = Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{sprite}.png'))).convert('RGBA')
    return np.array(im.crop((0, 0, 16, 16)))


def derived(jar, look: str = 'A') -> dict:
    """The build hook: texture path -> RGBA image (variant A unless a preview asks otherwise)."""
    out = {}
    for name, sprite in SPRITES.items():
        a = vanilla(Path(jar), sprite)
        for i, stage in enumerate(STAGES, 1):
            out[f'block/{stage}_{name}.png'] = Image.fromarray(LOOKS[look](a, i), 'RGBA')
    return out


def hard_share(a: np.ndarray, thr: int = 40) -> float:
    lum = 0.299 * a[..., 0] + 0.587 * a[..., 1] + 0.114 * a[..., 2]
    h = (np.abs(np.diff(lum, axis=0)) > thr).sum() + (np.abs(np.diff(lum, axis=1)) > thr).sum()
    return h / (lum[1:].size + lum[:, 1:].size)


def find_jar(explicit):
    for c in (explicit, os.environ.get('SB_CLIENT_JAR'), Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'):
        if c and Path(c).exists():
            return Path(c)
    sys.exit('no Minecraft client jar found (--jar)')


def _data(a: np.ndarray, k: int = 12) -> str:
    buf = io.BytesIO()
    Image.fromarray(a, 'RGBA').resize((16 * k, 16 * k), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def preview(jar: Path, path: Path) -> None:
    sample = ('cobblestone', 'dirt', 'netherrack')
    names = {'A': 'Rahmen', 'B': 'Körnung', 'C': 'Gurte'}
    cards = []
    for key, fn in LOOKS.items():
        cells = []
        for name in sample:
            a = vanilla(jar, SPRITES[name])
            tiles = [(f'{name} (Vanilla)', a, hard_share(a))] + [(f'{s.capitalize()}', fn(a, i), hard_share(fn(a, i))) for i, s in enumerate(STAGES, 1)]
            cells.append('<div class=row>' + ''.join(
                f'<figure><img src="{_data(t)}" alt=""><figcaption>{lbl}<br><small>hart {h:.0%}</small></figcaption></figure>' for lbl, t, h in tiles) + '</div>')
        cards.append(f'<section><h2>Variante {key}: {names[key]}</h2>{"".join(cells)}'
                     f'<button onclick="pick(\'{key}\')">Variante {key} wählen</button></section>')
    html = f"""<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>a-wh26-10 Vorschau: Kompakte Blöcke</title>
<style>:root{{--bg:#f2efe6;--fg:#1c1e21;--card:#fff;--ac:#2f6b3a}}@media(prefers-color-scheme:dark){{:root{{--bg:#1d1f22;--fg:#e8e6df;--card:#2a2d31;--ac:#7fc08a}}}}
body{{font:16px/1.5 system-ui,sans-serif;margin:0;padding:1rem;background:var(--bg);color:var(--fg)}}h1{{font-size:1.4rem}}
.g{{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:1rem}}section{{background:var(--card);padding:1rem;border-radius:8px}}
.row{{display:flex;gap:.5rem;margin:.4rem 0}}figure{{margin:0;text-align:center}}img{{width:92px;height:92px;image-rendering:pixelated;display:block}}figcaption{{font-size:.75rem}}
button{{margin-top:.6rem;padding:.6rem 1rem;font-size:1rem;border:0;border-radius:6px;background:var(--ac);color:var(--bg);cursor:pointer}}
#out{{margin-top:1rem;font-size:1.1rem}}</style>
<h1>a-wh26-10 (WH26-10): Kompakte Blöcke, Look am Beispiel Bruchstein</h1>
<p>Stufe 1 <b>Compact</b> = 9 Blöcke, Stufe 2 <b>Dense</b> = 9 Compact (umkehrbar). Pixel aus Vanilla, per Skript beim Bauen
(<code>tools/textures/compact_2026_11_06.py</code>), nichts eingecheckt. Je Reihe: Vanilla, Compact, Dense. „hart“ = Anteil harter Übergänge (Richtwert unter ~12 %, A8b).
Wahl in allen 19 Blöcken; gebaut ist vorerst <b>A</b>.</p>
<div class=g>{''.join(cards)}</div>
<p id=out></p><script>function pick(k){{var c='WH2610-'+k;document.getElementById('out').innerHTML='Antwortcode: <code>'+c+'</code>';try{{navigator.clipboard.writeText(c)}}catch(e){{}}}}</script></html>"""
    path.write_text(html, encoding='utf-8')
    print(f'wrote {path}')


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--out')
    ap.add_argument('--preview')
    ap.add_argument('--jar')
    ap.add_argument('--look', default='A')
    a = ap.parse_args()
    jar = find_jar(a.jar)
    if a.preview:
        preview(jar, Path(a.preview))
    if a.out:
        for rel, im in derived(jar, a.look).items():
            p = Path(a.out) / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            im.save(p)


if __name__ == '__main__':
    main()
