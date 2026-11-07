"""TEX07B preview (06.11.2026): sage crystal / shard / block and the pressings in variants a/b, as a clickable page with the answer code TEX07B.

    python3.12 tools/textures/tex07b_preview.py OUT.html [--jar J]     (needs the 26.3 client jar: run it on sb-test)
"""
from __future__ import annotations

import argparse
import base64
import io
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import ores_2026_10_30 as o  # noqa: E402
from vanilla_derived import find_jar  # noqa: E402


def uri(im, scale=10):
    buf = io.BytesIO()
    im.resize((im.width * scale, im.height * scale), o.Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def fig(im, cap):
    return f'<figure><img src="{uri(im)}"><figcaption>{cap}</figcaption></figure>'


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('out', type=Path)
    ap.add_argument('--jar')
    a = ap.parse_args()
    j = o.Jar(a.jar or find_jar())
    body = ''
    for v in 'ab':
        s = o.sage_images(j, v)
        body += (f'<h2>Sage Crystal {v.upper()}</h2><p>{"Ein sechskantiger Kristall, Splitter schräg, Block aus vier Facettenzellen" if v == "a" else "Kristall-Gruppe aus drei Spitzen, gebrochene Spitze als Splitter, Block mit großer Raute"}</p><div class="row">'
                 + fig(s['item/sage_crystal.png'], 'sage_crystal') + fig(s['item/sage_crystal_shard.png'], 'sage_crystal_shard')
                 + fig(s['block/sage_crystal_block.png'], 'sage_crystal_block') + '</div>')
    for v in 'ab':
        body += (f'<h2>Presslinge {v.upper()}</h2><p>{"Drei Materialien als kleine Körner (2x1)" if v == "a" else "Drei Materialien als 2x2-Flecken"}; Anteile wie im Rezept (Roségold 1 Gold : 2 Kupfer : 1 Silber, Rosenkupfer 2 Gold : 1 Roségold : 1 Silber), dunkle Naht</p><div class="row">')
        for alloy, name in (('rose_gold', 'Roségold'), ('rose_bronze', 'Rosenkupfer')):
            body += fig(o.press_item(j, alloy, v), f'{name} Pressling') + fig(o.press_block(j, alloy, v), f'{name} Presslingblock')
        body += '</div>'
    body += '<h2>Zum Vergleich</h2><div class="row">' + fig(j.img('item/amethyst_shard'), 'Vanilla Amethystsplitter (nur Vergleich)') + fig(o.recolor(j.img('item/gold_ingot'), 'soft'), 'Roségold-Barren') + '</div>'
    html = '''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Sage und Presslinge</title><style>
:root{--bg:#f4f4f2;--fg:#1c1c1c;--card:#fff}@media(prefers-color-scheme:dark){:root{--bg:#181818;--fg:#eee;--card:#242424}}
body{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:14px system-ui,sans-serif}h1{font-size:20px}h2{font-size:16px;margin:18px 0 6px}
.row{display:flex;flex-wrap:wrap;gap:10px}figure{margin:0;background:var(--card);padding:8px;border-radius:8px;text-align:center;font-size:12px}img{image-rendering:pixelated;display:block;margin:0 auto 4px;max-width:100%}
.opt{background:var(--card);padding:10px;border-radius:8px;margin:8px 0}label{margin-right:14px;font-weight:600}code{display:block;padding:8px;background:rgba(128,128,128,.2);border-radius:5px;word-break:break-all;margin:8px 0}textarea{width:100%;box-sizing:border-box}
</style></head><body><h1>Sage Crystal und Presslinge (TEX07B)</h1><p>Eigene Formen statt Umfärbung; Sage-Grün aus Sage-Erz und Sage-Orb. Variante A wird eingebaut, wenn nichts anderes gewählt wird.</p>''' + body + '''
<div class="opt"><b>Antwort</b><p>Sage Crystal (Kristall, Splitter, Block): <label><input type="radio" name="c" value="A" checked>A</label><label><input type="radio" name="c" value="B">B</label></p>
<p>Presslinge und Blöcke: <label><input type="radio" name="p" value="A" checked>A</label><label><input type="radio" name="p" value="B">B</label></p>
<p>Anmerkung:</p><textarea id="anm" rows="2"></textarea><code id="code"></code><button id="cp">Code kopieren</button></div>
<script>function upd(){const g=n=>document.querySelector('input[name='+n+']:checked').value;const a=document.getElementById('anm').value.replace(/[|]/g,'/').trim();document.getElementById('code').textContent='TEX07B|crystal='+g('c')+'|press='+g('p')+(a?'|anm='+a:'')}
document.querySelectorAll('input').forEach(e=>e.onchange=upd);document.getElementById('anm').oninput=upd;document.getElementById('cp').onclick=()=>navigator.clipboard&&navigator.clipboard.writeText(document.getElementById('code').textContent);upd();</script></body></html>'''
    a.out.write_text(html, encoding='utf-8')


if __name__ == '__main__':
    main()
