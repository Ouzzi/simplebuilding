"""Preview of the rose gold apples (IG31-41): two variants per apple next to the Vanilla golden and enchanted golden apple.

Reads the local 26.3 client jar, writes one self-contained HTML page (answer code APPLE41|rose=A|bloom=A). The built variants live in
ores_2026_10_30.py (APPLE_VARIANT); the enchanted apples use the plain texture plus the Vanilla glint (drawn here as a purple shimmer).

    python3.12 tools/textures/apple41_preview.py --jar /root/vanilla263/client.jar --out /root/sb-tools-b/previews/b-apple41-preview.html
"""
from __future__ import annotations

import argparse
import base64
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import ores_2026_10_30 as ores  # noqa: E402

ROWS = [  # name, hunger, effects
    ('Goldapfel (Vanilla)', 4, 'Regeneration II 0:05, Absorption I 2:00'),
    ('Rosenbronze-Apfel', 5, 'Regeneration II 0:07, Absorption I 2:00, Feuerresistenz 1:30'),
    ('Blütengold-Apfel', 6, 'Regeneration II 0:09, Absorption I 3:00, Feuerresistenz 2:30, Resistenz I 0:30'),
    ('Netherit-Apfel', 8, 'Regeneration II 0:10, Absorption II 0:30, Feuerresistenz 4:00, Resistenz I 1:00'),
]
ENCHANTED = [
    ('Verz. Goldapfel (Vanilla)', 'Regeneration II 0:20, Resistenz I 5:00, Feuerres. 5:00, Absorption IV 2:00'),
    ('Verz. Rosenbronze-Apfel', 'Regeneration II 0:25, Resistenz I 5:00, Feuerres. 5:00, Absorption IV 2:00'),
    ('Verz. Blütengold-Apfel', 'Regeneration II 0:32, + Stärke I 1:00'),
    ('Verz. Netherit-Apfel', 'Regeneration II 0:40, + Stärke II 2:00'),
]


def glint(im: Image.Image) -> Image.Image:
    """Cheap stand-in for the enchantment glint: diagonal purple bands over the opaque pixels."""
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] and (x + y) % 6 < 2:
                k = 0.35
                out.putpixel((x, y), (int(p[0] * (1 - k) + 150 * k), int(p[1] * (1 - k) + 60 * k), int(p[2] * (1 - k) + 230 * k), p[3]))
    return out


def uri(im: Image.Image, scale: int = 8) -> str:
    return 'data:image/png;base64,' + base64.b64encode(ores.png(im.resize((im.width * scale, im.height * scale), Image.NEAREST))).decode()


def fig(im: Image.Image, caption: str) -> str:
    return f'<figure><img src="{uri(im)}" width="128" height="128"><figcaption>{caption}</figcaption></figure>'


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--jar', required=True)
    ap.add_argument('--out', type=Path, required=True)
    a = ap.parse_args()
    j = ores.Jar(a.jar)
    gold = j.img('item/golden_apple')
    nether = Image.open(Path(__file__).resolve().parents[2] / 'src/main/resources/assets/simplebuilding/textures/item/netherite_apple.png').convert('RGBA')
    html = ['<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Apple41 Vorschau</title>',
            '<style>:root{--bg:#f5f4f0;--fg:#1c1c1e;--card:#fff;--line:#d0cec6;--acc:#2f5d8a}@media(prefers-color-scheme:dark){:root{--bg:#1b1d21;--fg:#ececec;--card:#262a30;--line:#3d434c;--acc:#7fb0e0}}'
            'body{background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif;margin:0;padding:16px;max-width:1000px;margin-inline:auto}'
            'h1{font-size:22px}h2{font-size:17px;margin-top:28px;border-bottom:1px solid var(--line);padding-bottom:4px}'
            '.opt{display:block;background:var(--card);border:1px solid var(--line);border-radius:8px;padding:10px;margin:8px 0;cursor:pointer}'
            '.opt:has(input:checked){outline:2px solid var(--acc)}.row{display:flex;flex-wrap:wrap;gap:10px;margin-top:6px;align-items:flex-end}'
            'figure{margin:0;text-align:center;font-size:12px;max-width:150px}img{image-rendering:pixelated;background:#555;border-radius:3px}.hs{opacity:.7}'
            '#code{background:var(--card);border:1px solid var(--line);padding:6px 10px;border-radius:6px;font-family:monospace;display:block;margin:10px 0;word-break:break-all;user-select:all}'
            'p.note{opacity:.8;font-size:13px}table{border-collapse:collapse;font-size:13px}td,th{border:1px solid var(--line);padding:3px 8px;text-align:left}</style>',
            '<h1>Simple Ores: Roségold-Äpfel (IG31-41)</h1>',
            '<p class="note">Branch b-apple41. Jeder Apfel ist aus dem Vanilla-Goldapfel abgeleitet (Umfärbung auf die Rampen der Legierungen, Vanilla-Raster und weiche Übergänge bleiben, A8b); gebaut wird beim Build, nichts davon liegt im Repo. '
            'Der verzauberte Apfel nutzt dieselbe Textur plus Glint (hier nur als lila Streifen angedeutet). Je Apfel eine Variante wählen, unten steht der Antwortcode.</p>',
            '<h2>Vergleich: Stufenleiter (Vanilla / Netherit)</h2>',
            '<div class="row">' + fig(gold, 'Goldapfel (Vanilla)') + fig(nether, 'Netherit-Apfel') + fig(glint(gold), 'verz. Goldapfel (Vanilla)') + fig(glint(nether), 'verz. Netherit-Apfel') + '</div>']
    for name, ramp, code in (('Rosenbronze-Apfel (hart, Kupfer-Rosé)', 'hard', 'rose'), ('Blütengold-Apfel (weich, Zartrosa)', 'soft', 'bloom')):
        html.append(f'<h2>{name}</h2>')
        for v, title in (('a', 'A reine Umfärbung des Goldapfels'), ('b', 'B Umfärbung mit grünem Blatt am Stiel')):
            im = ores.apple_image(j, ramp, v)
            html.append(f'<label class="opt"><input type="radio" name="{code}" value="{v.upper()}"{" checked" if v == "a" else ""}><b>{title}</b>'
                        f'<div class="row">{fig(im, f"Gegenstand <span class=hs>{ores.hard_share(im) * 100:.0f} % hart, Vanilla {ores.hard_share(gold) * 100:.0f} %</span>")}{fig(glint(im), "verzaubert (Glint-Andeutung)")}'
                        f'{fig(Image.alpha_composite(Image.new("RGBA", im.size, (60, 60, 60, 255)), im).resize((16, 16)), "auf dunklem Grund")}</div></label>')
    html.append('<h2>Werte (Hunger / Effekte)</h2><table><tr><th>Apfel</th><th>Hunger</th><th>Effekte</th></tr>')
    html += [f'<tr><td>{n}</td><td>{h}</td><td>{e}</td></tr>' for n, h, e in ROWS]
    html += [f'<tr><td>{n}</td><td></td><td>{e}</td></tr>' for n, e in ENCHANTED]
    html.append('</table><p class="note">Zwischenstufe zwischen Gold und Netherit; Begründung und A12 in docs/ai/PLAN-B-APPLE41-2026-11-01.md. '
                'Rezepte: 8 Barren um einen Apfel; verzaubert 8 Blöcke um einen Apfel. Standard im Code: A/A.</p>')
    html.append('<h2>Antwortcode</h2><div id="code">APPLE41|rose=A|bloom=A</div>'
                "<script>function upd(){var c=['APPLE41'];['rose','bloom'].forEach(function(n){var e=document.querySelector('input[name='+n+']:checked');c.push(n+'='+(e?e.value:'A'))});document.getElementById('code').textContent=c.join('|')}"
                "document.querySelectorAll('input').forEach(function(i){i.addEventListener('change',upd)});upd();</script></html>")
    a.out.write_text('\n'.join(html), encoding='utf-8')


if __name__ == '__main__':
    main()
