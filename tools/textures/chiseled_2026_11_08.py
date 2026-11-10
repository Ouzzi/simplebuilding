"""Chiseled Polished Andesite/Diorite/Granite, Chiseled Prismarine, Chiseled Red Nether Bricks (a-pal09, owner 08.11.: missing blocks).
Every pixel is Vanilla-derived (the Vanilla sprite of the base block, only brightness moves along a carved pattern), so the textures are
built from the client jar at build time and never committed (AGENTS.md rule 7; tools/textures/vanilla_derived.py, MODULE_OUTPUTS['simpleblocks']).

Two looks (A is built in, B only in the preview; the owner picks, answer code PAL09):
  A "Rahmen": a carved panel like Vanilla's chiseled stone bricks: dark outer edge, a lighter step inside it, a thin groove, the face
              slightly lifted inside the groove.
  B "Band":   two horizontal grooves with a lit lower edge (the chiseled quartz / sandstone band), a darker rim.
All edits are brightness factors of the Vanilla pixels (A8b: soft steps; the share of hard transitions is printed in the preview).

    python3.12 tools/textures/chiseled_2026_11_08.py --out DIR [--jar J]            write the textures of variant A
    python3.12 tools/textures/chiseled_2026_11_08.py --preview OUT.html [--jar J]   owner page (5 blocks x 2 looks)
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

# New block -> Vanilla sprite under textures/block/ (the animated prismarine strip: first frame).
SPRITES = {
    'chiseled_polished_andesite': 'polished_andesite', 'chiseled_polished_diorite': 'polished_diorite',
    'chiseled_polished_granite': 'polished_granite', 'chiseled_prismarine': 'prismarine', 'chiseled_red_nether_bricks': 'red_nether_bricks',
    'chiseled_dark_prismarine': 'dark_prismarine',  # b-pal14 (owner 09.11.), look A like the others
}
DESC = 'chiseled_2026_11_08.derived() - Vanilla sprite with a carved panel / band (a-pal09)'


def outputs() -> dict:
    """Texture path (under assets/simpleblocks/textures/) -> (size, description), for vanilla_derived.MODULE_OUTPUTS."""
    return {f'block/{n}.png': ((16, 16), DESC) for n in SPRITES}


def _scale(a: np.ndarray, f: np.ndarray) -> np.ndarray:
    out = a.astype(float)
    out[..., :3] = np.clip(out[..., :3] * f[..., None], 0, 255)
    return out.astype(np.uint8)


def look_a(a: np.ndarray) -> np.ndarray:
    i = np.arange(16)
    d = np.minimum(i, 15 - i)
    ring = np.minimum(d[:, None], d[None, :])
    f = np.ones((16, 16))
    f[ring == 0] = 0.80
    f[ring == 1] = 1.10
    f[ring == 3] = 0.84  # groove
    f[ring >= 4] = 1.04
    # the lower right edge of the groove stays a touch darker than the upper left (light from the top left, like the Vanilla bricks)
    lit = (ring == 3) & (i[:, None] + i[None, :] < 15)
    f[lit] = 0.90
    return _scale(a, f)


def look_b(a: np.ndarray) -> np.ndarray:
    f = np.ones((16, 16))
    f[[0, 15], :] = 0.82
    f[:, [0, 15]] = 0.82
    f[[4, 11], 1:15] = 0.80   # groove
    f[[5, 12], 1:15] = 1.10   # lit lower edge
    f[[3, 10], 1:15] = 0.94   # soft shoulder above the groove
    return _scale(a, f)


LOOKS = {'A': look_a, 'B': look_b}


def vanilla(jar: Path, sprite: str) -> np.ndarray:
    with zipfile.ZipFile(jar) as z:
        im = Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{sprite}.png'))).convert('RGBA')
    return np.array(im.crop((0, 0, 16, 16)))


def derived(jar, look: str = 'M') -> dict:
    """The build hook: texture path -> RGBA image. Since b-pal15 (owner 09.11., N13a) the built look is 'M': frame + one carved
    motif per family (tools/textures/pal15_2026_11_09.py, since b-pal16 pal16_2026_11_10.py); the frame looks A/B stay for the PAL09 history."""
    if look == 'M':
        import pal16_2026_11_10 as pal16   # b-pal16 (owner PAL15 10.11.): symmetric motifs, prismarine keeps its PAL15 eye
        return {k: Image.fromarray(v, 'RGBA') for k, v in pal16.simpleblocks(Path(jar)).items() if k[len('block/'):-4] in SPRITES}
    return {f'block/{name}.png': Image.fromarray(LOOKS[look](vanilla(Path(jar), sprite)), 'RGBA') for name, sprite in SPRITES.items()}


def hard_share(a: np.ndarray, thr: int = 40) -> float:
    lum = 0.299 * a[..., 0] + 0.587 * a[..., 1] + 0.114 * a[..., 2]
    h = (np.abs(np.diff(lum, axis=0)) > thr).sum() + (np.abs(np.diff(lum, axis=1)) > thr).sum()
    return float(h) / (lum.shape[0] * (lum.shape[1] - 1) * 2)


def find_jar(explicit):
    for c in (explicit, os.environ.get('SB_CLIENT_JAR'), Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'):
        if c and Path(c).exists():
            return Path(c)
    sys.exit('no Minecraft client jar found (--jar)')


def _data(a: np.ndarray, k: int = 10) -> str:
    buf = io.BytesIO()
    Image.fromarray(a, 'RGBA').resize((16 * k, 16 * k), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def preview(jar: Path, path: Path) -> None:
    names = {'A': 'Rahmen', 'B': 'Band'}
    cards = []
    for key, fn in LOOKS.items():
        rows = []
        for name, sprite in SPRITES.items():
            a = vanilla(jar, sprite)
            b = fn(a)
            rows.append(f'<figure><img src="{_data(a)}" alt=""><figcaption>Vanilla {sprite}</figcaption></figure>'
                        f'<figure><img src="{_data(b)}" alt=""><figcaption>{name.replace("_", " ")}<br><small>hart {hard_share(b):.0%}</small></figcaption></figure>')
        cards.append(f'<section><h2>Variante {key}: {names[key]}</h2><div class=row>{"".join(rows)}</div>'
                     f'<button onclick="pick(\'{key}\')">Variante {key} wählen</button></section>')
    html = f"""<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>a-pal09 Vorschau: Gemeißelte Blöcke</title>
<style>:root{{--bg:#f2efe6;--fg:#1c1e21;--card:#fff;--ac:#2f6b3a}}@media(prefers-color-scheme:dark){{:root{{--bg:#1d1f22;--fg:#e8e6df;--card:#2a2d31;--ac:#7fc08a}}}}
body{{font:16px/1.5 system-ui,sans-serif;margin:0;padding:1rem;background:var(--bg);color:var(--fg)}}h1{{font-size:1.4rem}}
section{{background:var(--card);padding:1rem;border-radius:8px;margin-bottom:1rem}}.row{{display:flex;flex-wrap:wrap;gap:.6rem}}
figure{{margin:0;text-align:center}}img{{width:96px;height:96px;image-rendering:pixelated;display:block}}figcaption{{font-size:.75rem;max-width:96px}}
button{{margin-top:.6rem;padding:.6rem 1rem;font-size:1rem;border:0;border-radius:6px;background:var(--ac);color:var(--bg);cursor:pointer}}</style>
<h1>a-pal09: gemeißelte Muster</h1>
<p>Fünf neue Blöcke (Chiseled Polished Andesite/Diorite/Granite, Prismarine, Red Nether Bricks). Je Paar: Vanilla links, neu rechts.
Pixel aus Vanilla, per Skript beim Bauen (<code>tools/textures/chiseled_2026_11_08.py</code>), nichts eingecheckt. „hart“ = Anteil harter Übergänge (Richtwert unter ~12 %, A8b).
Gebaut ist vorerst <b>A</b>; die Wahl gilt für alle fünf.</p>
{''.join(cards)}
<p id=out></p><script>function pick(k){{var c='PAL09-'+k;document.getElementById('out').innerHTML='Antwortcode: <code>'+c+'</code>';try{{navigator.clipboard.writeText(c)}}catch(e){{}}}}</script></html>"""
    path.write_text(html, encoding='utf-8')
    print(f'wrote {path}')


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--out')
    ap.add_argument('--preview')
    ap.add_argument('--jar')
    ap.add_argument('--look', default='M')
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
