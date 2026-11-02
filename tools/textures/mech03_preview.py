"""b-mech03 (02.11.): one preview page: crab legs before/after, zebra/zorse model B, double piston + dampener textures.

    python3 tools/textures/mech03_preview.py /root/simplebuilding/abnahme-b/b-mech03-preview.html
"""
from __future__ import annotations

import base64
import copy
import io
import json
import math
import subprocess
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/textures'))
sys.path.insert(0, str(ROOT / 'modules/simpleanimals/tools'))
import gen_models as G  # noqa: E402
import preview as P  # noqa: E402
import optik32_preview as OP  # noqa: E402  (parts_of, turntable)

P.W, P.H, P.FRAMES = 440, 300, 8
SA = 'modules/simpleanimals/shared/resources/assets/simpleanimals/textures/entity/'
SM = ROOT / 'modules/simplemachines/shared/resources/assets/simplemachines/textures/block/'


def uri(im, k=1):
    if k != 1:
        im = im.resize((im.width * k, im.height * k), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, 'PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def old_crab(key):
    """The legs as they were before b-mech03 (thigh up, shin long): for the 'vorher' view."""
    spec = copy.deepcopy(G.PALETTES[key])
    fid = key == 'crab_fiddler'
    for p in spec['parts']:
        n = p['name']
        sx = -1 if n.startswith('right') else 1
        if '_thigh_' in n:
            p['rot'] = (0, 0, -(0.35 if fid else 0.45) * sx)
        elif '_shin_' in n:
            p['rot'] = (0, 0, (1.55 if fid else 1.7) * sx)
            for b in p['boxes']:
                L = 5 if fid else 6
                b['size'] = (L, 1, 1)
                b['pos'] = (0, -0.5, -0.5) if sx > 0 else (-L, -0.5, -0.5)
    return spec


def crab_frames(spec, tex):
    parts, tw, th = OP.parts_of(spec)
    return OP.turntable(parts, tw, th, tex, 13, (0, math.pi / 2, 0))


def horse_frames(key, tex):
    parts, tw, th = OP.parts_of(G.PALETTES[key])
    return OP.turntable(parts, tw, th, tex, 7)


def main(out):
    G.register_horses()
    groups = []
    for key, label in (('crab_red', 'Krebs A (rot)'), ('crab_fiddler', 'Krebs B (Winkerkrabbe)')):
        tex = G.TEXTURES[key]()
        old = G.TEXTURES[key]()
        groups.append(dict(code=key, title=label, vorher=crab_frames(old_crab(key), old), nachher=crab_frames(G.PALETTES[key], tex),
                           note='Vorher: Oberschenkel zeigen nach oben, Unterschenkel lang. Nachher: beide Seiten spiegelbildlich, Beine zeigen nach unten.'))
    for coat in ('zebra', 'zorse'):
        new = G.TEXTURES[coat]()
        groups.append(dict(code=coat, title=f'{coat.capitalize()} (Pferd B kompakt)', vorher=None, nachher=horse_frames(coat, new),
                           note='Eigenes Modell B: tieferer Rumpf, kürzere Beine mit Hufen, dicker Hals, Kopfhaltung wie Vanilla (Pivot und 30 Grad gleich), Hitbox/Werte unverändert.'))
    stills = []
    for n in ('double_piston_side_overlay', 'double_piston_plate_overlay', 'signal_dampener_top_0', 'signal_dampener_top_1_on', 'signal_dampener_top_2', 'signal_dampener_top_3_on'):
        stills.append((n, uri(Image.open(SM / f'{n}.png').convert('RGBA'), 12)))
    html = ['<!doctype html><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1"><title>b-mech03 Vorschau</title>',
            '<style>body{font:15px system-ui;background:#1d1f22;color:#e6e6e6;max-width:980px;margin:auto;padding:12px}.g{background:#2a2d31;border-radius:8px;padding:12px;margin:14px 0}'
            'img{image-rendering:pixelated;background:#3e5538}.row{display:flex;gap:10px;flex-wrap:wrap}.c{flex:1;min-width:260px}.s img{background:#555;margin:4px}button{padding:6px 12px;margin:3px}</style>',
            '<h1>b-mech03: Krebs-Beine, Pferd B, Doppelkolben, Dämpfer</h1><p>Regler dreht das Modell. Unten steht der Antwortcode.</p>']
    for i, g in enumerate(groups):
        html.append(f'<div class=g><h2>{g["title"]}</h2><p>{g["note"]}</p><input type=range min=0 max=7 value=0 oninput="spin({i},this.value)"><div class=row>')
        for lab, fr in (('Vorher', g['vorher']), ('Nachher', g['nachher'])):
            if fr:
                html.append(f'<div class=c><b>{lab}</b><br><img id="im{i}{lab}" src="{fr[0]}"></div>')
        html.append(f'</div><button onclick="ans(\'{g["code"]}\',\'ok\')">OK</button><button onclick="ans(\'{g["code"]}\',\'nein\')">Nacharbeit</button></div>')
    html.append('<div class="g s"><h2>Texturen (Fugenpaar A, Dämpfer ohne Hebel)</h2>' + ''.join(f'<figure style=display:inline-block><img src="{u}"><figcaption>{n}</figcaption></figure>' for n, u in stills) +
                '<p>Silberspiegel: Variante A (bereits eingebaut, unverändert).</p></div>')
    html.append('<div class=g><b>Antwortcode</b><pre id=out>MECH03|</pre></div>')
    data = [dict(v=g['vorher'], n=g['nachher']) for g in groups]
    html.append('<script>const D=' + json.dumps(data) + ';const A={};function spin(i,k){const a=document.getElementById("im"+i+"Vorher"),b=document.getElementById("im"+i+"Nachher");'
                'if(a)a.src=D[i].v[k];if(b)b.src=D[i].n[k];}function ans(c,v){A[c]=v;document.getElementById("out").textContent="MECH03|"+Object.entries(A).map(e=>e[0]+"="+e[1]).join("|");}</script>')
    Path(out).write_text('\n'.join(html), encoding='utf-8')
    print(out, Path(out).stat().st_size)


if __name__ == '__main__':
    main(sys.argv[1])
