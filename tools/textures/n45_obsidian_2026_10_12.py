"""Blazing obsidian, owner pick N45 (2026-10-12): B1 (obsidian ground + ember veins), animated and seamless.

Run: python3.12 tools/textures/n45_obsidian_2026_10_12.py [--html out.html]
- blazing_strip(): 16 x (16*FRAMES) animation strip. The veins are random walks on the 16 px torus, so they run
  over every tile edge and continue in the neighbouring block; the ground is tileable value noise (same obsidian
  ground as B1 / Vanilla crying obsidian). A glow wave travels along each vein (dark rim -> glow -> hot core), the
  strip loops after FRAMES frames; mcmeta: interpolate, frametime 8 (like B3 / Vanilla magma).
Own pixel art drawn from code; no Vanilla pixels are used.
"""
from __future__ import annotations

import base64
import io
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import n41_proposals_2026_10_12 as n41  # noqa: E402
from n41_proposals_2026_10_12 import H, mix, new  # noqa: E402
import n45_2026_10_12 as n45  # noqa: E402

FRAMES = 8
ANIMATION = {'interpolate': True, 'frametime': 8}
RIM, GLOW, HOT, WHITE = H('4a1203'), H('b04606'), H('ff8a1c'), H('ffd070')
WAVE = 10.0   # pixels per glow wave along a vein


def veins(seed=4551, count=3):
    """Long 8-connected walks on the torus (flow mostly downward/diagonal), each a list of (x, y), no self touching."""
    rnd = random.Random(seed)
    taken = set()
    out = []
    while len(out) < count:
        x, y = rnd.randrange(16), rnd.randrange(16)
        length = rnd.randint(10, 13)
        dx, dy = rnd.choice(((1, 1), (-1, 1), (0, 1), (1, 0)))
        path = []
        for _ in range(length):
            if (x, y) in taken or any(((x + dx) % 16, (y + dy) % 16) in taken
                                      for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                                      if (x, y) not in path and ((x + dx) % 16, (y + dy) % 16) not in path[-2:]):
                break
            path.append((x, y))
            taken.add((x, y))
            if rnd.random() < 0.4:   # mostly keep the heading, sometimes bend
                dx, dy = rnd.choice(((0, 1), (1, 1), (-1, 1), (1, 0), (-1, 0), (1, -1)))
            x, y = (x + dx) % 16, (y + dy) % 16
        if len(path) >= 8:
            out.append(path)
        else:
            taken -= set(path)
    return out


def ramp(t):
    """glow level 0..1 -> colour (vein core)."""
    if t < 0.5:
        return mix(RIM, GLOW, t * 2)
    if t < 0.85:
        return mix(GLOW, HOT, (t - 0.5) / 0.35)
    return mix(HOT, WHITE, (t - 0.85) / 0.15 * 0.6)


def blazing_frame(f, paths=None):
    im = n45.obsidian_ground(450)
    paths = paths or veins()
    cells = {}
    for pi, path in enumerate(paths):
        for s, (x, y) in enumerate(path):
            phase = s / WAVE - f / FRAMES + pi * 0.37
            wave = 0.5 + 0.5 * math.cos(2 * math.pi * phase)
            cells[(x, y)] = 0.2 + 0.45 * wave
    for (x, y) in cells:   # dark ember rim on the free neighbours, like the dark edge of the tears
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = ((x + dx) % 16, (y + dy) % 16)
            if p not in cells and n41.hsh(*p, 5) < 0.2:
                im.putpixel(p, RIM)
    for (x, y), t in cells.items():
        im.putpixel((x, y), ramp(t))
    return im


def blazing_strip():
    paths = veins()
    strip = Image.new('RGBA', (16, 16 * FRAMES))
    for f in range(FRAMES):
        strip.paste(blazing_frame(f, paths), (0, 16 * f))
    return strip


# ------------------------------------------------------------------ preview page
def frames_of(strip):
    return [strip.crop((0, 16 * f, 16, 16 * f + 16)) for f in range(strip.height // 16)]


def interpolated(strip, steps=4):
    fr = frames_of(strip)
    return [Image.blend(fr[i], fr[(i + 1) % len(fr)], k / steps) for i in range(len(fr)) for k in range(steps)]


def gif(images, scale, tiles=1):
    out = []
    for im in images:
        rgb = im.convert('RGB')
        if tiles > 1:
            big = Image.new('RGB', (16 * tiles, 16 * tiles))
            for ty in range(tiles):
                for tx in range(tiles):
                    big.paste(rgb, (16 * tx, 16 * ty))
            rgb = big
        out.append(rgb.resize((rgb.width * scale, rgb.height * scale), Image.NEAREST))
    b = io.BytesIO()
    out[0].save(b, 'GIF', save_all=True, append_images=out[1:], duration=100, loop=0)   # 8 ticks = 0.4 s / 4 steps
    return 'data:image/gif;base64,' + base64.b64encode(b.getvalue()).decode()


def png(im, scale):
    b = io.BytesIO()
    im.resize((im.width * scale, im.height * scale), Image.NEAREST).save(b, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()


def page(path, vanilla_png=None):
    strip = blazing_strip()
    seq = interpolated(strip)
    fr = frames_of(strip)
    van = ''
    if vanilla_png:
        vc = Image.open(vanilla_png).convert('RGBA')
        van = (f'<figure><figcaption><b>Vanilla crying_obsidian</b><span>Referenz (statisch), 3x3</span></figcaption>'
               f'<img src="{png(Image.new("RGBA", (48, 48)) if False else _tile(vc, 3), 5)}" width="240" height="240" alt="Vanilla 3x3"></figure>')
    html = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Lohen-Obsidian N45</title><style>
:root{{--bg:#f4f4f2;--fg:#1d1f23;--card:#fff;--mut:#5d636c;--line:#d9dadd}}
@media (prefers-color-scheme:dark){{:root:not([data-theme="light"]){{--bg:#16181c;--fg:#e8e9ec;--card:#20232a;--mut:#9aa1ab;--line:#30343c}}}}
:root[data-theme="dark"]{{--bg:#16181c;--fg:#e8e9ec;--card:#20232a;--mut:#9aa1ab;--line:#30343c}}
body{{background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif;margin:0;padding:16px}}
main{{max-width:1000px;margin:auto}}
figure{{background:var(--card);border:1px solid var(--line);border-radius:8px;margin:0 0 12px;padding:12px}}
figcaption{{display:flex;flex-wrap:wrap;gap:4px 12px;margin-bottom:8px}} figcaption span{{color:var(--mut)}}
.row{{display:flex;flex-wrap:wrap;align-items:flex-end;gap:12px}}
img{{image-rendering:pixelated;max-width:100%;height:auto}} li{{color:var(--mut)}} a{{color:inherit}}
</style></head><body><main>
<h1>Lohen-Obsidian: B1 animiert und nahtlos</h1>
<p>Obsidian-Grund wie Crying Obsidian, Glut-Adern als Wege auf dem 16-px-Torus: sie laufen ueber jede Kante weiter. Eine Gluthelligkeits-Welle wandert entlang der Adern ({FRAMES} Frames, interpolate, frametime 8). Orange statt Violett, kein Vanilla-Pixel.</p>
<figure><figcaption><b>3x3-Flaeche</b><span>animiert (GIF, 4 Zwischenschritte je Frame)</span></figcaption>
<div class="row"><img src="{gif(seq, 6, 3)}" width="288" height="288" alt="3x3 animiert">
<img src="{png(_tile(fr[0], 3), 6)}" width="288" height="288" alt="3x3 Frame 0 statisch"></div></figure>
<figure><figcaption><b>Einzelblock</b><span>1x und 8x, animiert</span></figcaption>
<div class="row"><img src="{gif(seq, 1)}" width="16" height="16" alt="1x"><img src="{gif(seq, 8)}" width="128" height="128" alt="8x"></div></figure>
<figure><figcaption><b>Alle {FRAMES} Frames</b><span>8x, nebeneinander</span></figcaption>
<div class="row">{''.join(f'<img src="{png(f, 4)}" width="64" height="64" alt="Frame {i}">' for i, f in enumerate(fr))}</div></figure>
{van}
{_before_after_html()}
{_astral_html()}
<h2>Referenzen</h2><ul>
<li>Vanilla 26.3 Client-Jar: block/crying_obsidian (nur gelesen, Grund und Aderprinzip)</li>
<li><a href="https://www.tynker.com/minecraft/blocks/view/crying_obsidian/">Tynker: Crying Obsidian</a></li>
<li><a href="https://docs.faithfulpack.net/pages/textures/f32-texturing-guidelines">Faithful f32 Texturing Guidelines</a></li>
</ul></main></body></html>'''
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_text(html)


def _tile(im, n):
    big = Image.new('RGBA', (16 * n, 16 * n))
    for ty in range(n):
        for tx in range(n):
            big.paste(im, (16 * tx, 16 * ty))
    return big




def _astral_html():
    """Astral enchanting table before (N41 A3 / N45 side) and after (animated), top and side as GIFs + render."""
    import n45_astral_2026_10_12 as ast
    top0, _old_side, bottom = n41.astral_table(2, round_top=True)
    old_side = frames_of(_prev('astral_enchanting_table_side'))[0]
    new = ast.textures()
    t = frames_of(new['block/astral_enchanting_table_top'])
    sd = frames_of(new['block/astral_enchanting_table_side'])
    seqt, seqs = interpolated(new['block/astral_enchanting_table_top'], 3), interpolated(new['block/astral_enchanting_table_side'], 3)

    def render(top, side):
        return png(n45.table_render(top, side, bottom), 1)

    def rgif(k):   # animated block render, 3 steps per frame (frametime 6 = 0.3 s)
        frs = [n45.table_render(seqt[i], seqs[i], bottom) for i in range(len(seqt))]
        b = io.BytesIO()
        frs = [f.convert('RGB') for f in frs]
        frs[0].save(b, 'GIF', save_all=True, append_images=frs[1:], duration=100, loop=0)
        return 'data:image/gif;base64,' + base64.b64encode(b.getvalue()).decode()
    return f'''<h2>Astral-Verzauberungstisch: vorher / nachher</h2>
<p>Feedback-Tweak: Tischdecke haengt an den Seiten 2 Zeilen weiter herab.</p>
<p>Oberseite: Nebel-Wirbel dreht sich, Glanzpunkt laeuft ueber den Goldring, Eckkristalle funkeln. Seiten (Modell 12 px hoch, Zeilen 0-3 leer): Nebel-Muster jetzt UNTER dem Tuch, das Tuch hat die Obsidian-Farbe des Oberseiten-Rands; Saum glitzert, Sterne funkeln. Beide 8 Frames, interpolate, frametime 6.</p>
<figure><figcaption><b>vorher</b><span>vorherige Fassung, Frame 0 (Tuch kuerzer)</span></figcaption>
<div class="row"><img src="{png(top0, 8)}" width="128" height="128" alt="Oberseite vorher"><img src="{png(old_side, 8)}" width="128" height="128" alt="Seite vorher"><img src="{render(top0, old_side)}" width="128" height="128" alt="Render vorher"></div></figure>
<figure><figcaption><b>nachher</b><span>animiert (GIF)</span></figcaption>
<div class="row"><img src="{gif(seqt, 8)}" width="128" height="128" alt="Oberseite nachher"><img src="{gif(seqs, 8)}" width="128" height="128" alt="Seite nachher"><img src="{rgif(0)}" width="128" height="128" alt="Render nachher"></div></figure>'''


def _prev(name):
    """Strip as committed in HEAD (the version before the owner's feedback tweak)."""
    import subprocess
    root = Path(__file__).resolve().parents[2]
    d = subprocess.run(['git', 'show', f'HEAD:mc26_3/overlay/resources/assets/simplebuilding/textures/block/{name}.png'],
                       cwd=root, capture_output=True, check=True).stdout
    return Image.open(io.BytesIO(d)).convert('RGBA')


def _before_after_html():
    so, sn = interpolated(_prev('blazing_obsidian')), interpolated(blazing_strip())
    return f'''<h2>Feedback-Tweak: Lohen-Obsidian dezenter</h2>
<p>3 statt 4 Adern, kuerzer und dunkler, kaum Rand, ruhigere Welle (laengere Wellenlaenge, kleinere Helligkeitsspanne, kein heisser Kern).</p>
<figure><figcaption><b>vorher / nachher</b><span>3x3 animiert</span></figcaption><div class="row">
<img src="{gif(so, 6, 3)}" width="288" height="288" alt="vorher 3x3"><img src="{gif(sn, 6, 3)}" width="288" height="288" alt="nachher 3x3"></div></figure>'''


if __name__ == '__main__':
    a = sys.argv[1:]
    if '--html' in a:
        page(a[a.index('--html') + 1], a[a.index('--vanilla') + 1] if '--vanilla' in a else None)
