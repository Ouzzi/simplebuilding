#!/usr/bin/env python3
"""b-texrate15 (09.11., N13 d): rate the own block/item textures with texrate.py (PRINZIPIEN A8d).

Scope (tracked PNGs only, MC 26.3: core src/main + mc26_3 overlay, modules/*/shared; no mc1_21_11 copies, no wiki):
priority 1 Simple End (module simpleend + core End textures), 2 Simple Ores, 3 Simple Blocks + core building blocks,
4 other modules, 5 core items. Vanilla-derived outputs (tools/textures/vanilla_derived.py) are git-ignored and therefore
not rated; textures that b-pal15 (chiseled_*) or b-fun14 (simplefun) are working on are rated but marked "gesperrt".

  python3 tools/textures/texrate15_2026_11_09.py --json /tmp/texrate15.json   # rating table (JSON)
  python3 tools/textures/texrate15_2026_11_09.py --md   docs/ai/TEXTUR-BEWERTUNG-2026-11-09.md.table
  python3 tools/textures/texrate15_2026_11_09.py --preview OUT.html [--before DIR]  # sortable/filterable page
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import subprocess
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import texrate as tr  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
END_WORDS = ('astral', 'nihil', 'ender', 'end_', 'chorus', 'purpur', 'void', 'ashwood', 'duskwood', 'shulker', 'elytra')
BUILD_WORDS = ('bricks', 'planks', 'pillar', 'checker', 'tiles', 'stone', 'obsidian', 'block', 'quartz', 'carved/',
               'chess/', 'slab', 'stairs', 'polished', 'cracked', 'smooth', 'cut_', 'mosaic', 'glass', 'log', 'wood')
# Tint masks / animation parts: one colour or a grey dye mask by design (Vanilla redstone_dust_* is just as flat);
# rated as 'grau' (nicht bewertet).
SPECIAL = ('carved/', 'placed_', 'redstone_line', 'redstone_dot', '_redstone_active', 'redstone.', '_dyed', '_overlay', 'detector_needle', '_needle_',
           'detector_dial')
# Committed before the 12.10. rule but per-pixel recolours of Vanilla textures (astralit_nihilit_alternates_2026_10_03.py):
# rated, marked 'vanilla-abgeleitet', never reworked here (they belong into vanilla_derived.py).
VANILLA_OLD = ('veined_astralit', 'crystalline_astralit', 'layered_astralit', 'veined_nihilith', 'crystalline_nihilith',
               'frosted_nihilith')
# Look-check (Blick-Befund, 8x contact sheets read by the agent, 09.11.) per family: first matching substring wins.
NOTES = [
    ('light_pane', 'Licht-Scheibe (16 Farben x an/aus): grosse einfarbige Glas-/Rahmenflaeche, Familie gemeinsam ueberarbeiten'),
    ('display_ca', 'Vitrine: Grundflaeche einfarbig bzw. diagonale Streifen hart'),
    ('cloud', 'Wolke: weiche helle Flaeche, wenig Struktur'),
    ('elytra_pad', 'Pad: Wirbel/Funken mit harten Kanten 30-48 % (A8b ~7 %), wirkt unruhig; eigene Pad-Runde (Wirbelkontrast senken)'),
    ('launchpad', 'Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde'),
    ('spawn_teleporter', 'Pad: Wirbel mit harten Kanten, unruhig; eigene Pad-Runde'),
    ('flypad', 'Pad: Sterne/Wirbel hart, Rahmenflaeche flach'),
    ('chunk_loader', 'Pad-Familie: Funken hart auf Grund'),
    ('potion_pad', 'Pad-Familie: Wirbel hart'),
    ('backpack', 'Rucksackseite: grosse einfarbige Lederflaechen (Modellseite), Vanilla-Fass/Truhe brechen Holz in Maserung'),
    ('_lamp', 'Lampen-Maeander: grosse einfarbige Felder + harte Kanten; Vanilla-Redstone-Lampe ist ebenso hart, Flaeche brechen'),
    ('froglight', 'weiche Verlaeufe + grosse helle Mitte (gegen A8c: kein weicher Verlauf)'),
    ('pocket_jukebox', 'Item: flache Kastenflaeche, lesbar; mit 2-3 Toenen brechen'),
    ('fossil_', 'Knochen/Sand mit vielen harten Spruengen'),
    ('shaft_', 'duenner Schaft (Pfeil-Teilstueck), wenige Farben gewollt'),
    ('hanging_sign', 'Schild-Item: Holzflaeche flach (Vanilla oak_hanging_sign ebenfalls orange)'),
    ('_sign', 'Schild-Item: Holzflaeche flach (Vanilla oak_sign ebenfalls orange)'),
    ('trapdoor', 'Falltuer: Brettflaechen ohne Maserung'),
    ('door_', 'Tuer: Brettflaechen ohne Maserung'),
    ('stripped_', 'entrindeter Stamm: lange einfarbige Streifen'),
    ('sticks_top', 'Stoecke-Buendel oben: grosse einfarbige Flaeche zwischen den Stoecken'),
    ('crucible', 'Tiegel-Modellteil: dunkle Innenflaeche flach'),
    ('chess/', 'Schachbrett-Feld: polierte Flaeche flach'),
    ('_tiles', 'Fliesen: Fliesenflaechen einfarbig, Fugen durchgehend'),
    ('_pillar_top', 'Saeulenkopf: Ringe einfarbig'),
    ('_pillar', 'Saeule: Riefen ueber die volle Hoehe einfarbig (Vanilla-Quarzsaeule bricht sie)'),
    ('cut_', 'geschnittene Platten: Plattenmitte einfarbig (Vanilla cut_copper gruen)'),
    ('polished_', 'poliert: Plattenflaechen einfarbig (Vanilla polished_* 0-50 %)'),
    ('_block', 'Materialblock: grosse Grundflaeche'),
    ('_bricks', 'Ziegel: Ziegelflaechen teils einfarbig'),
]
LOCKED = (('chiseled', 'b-pal15'), ('simplefun', 'b-fun14'))


def tracked():
    out = subprocess.run(['git', 'ls-files', '*.png'], cwd=ROOT, capture_output=True, text=True, check=True).stdout
    seen = {}
    for p in out.split():
        if p.startswith(('wiki/', 'mc1_21_11/')) or '/assets/' not in p:
            continue
        ns, rest = p.split('/assets/', 1)[1].split('/', 1)
        if not rest.startswith(('textures/block/', 'textures/item/')):
            continue
        key = f'{ns}:{rest[len("textures/"):-4]}'
        if key in seen and not p.startswith('mc26_3/overlay/'):
            continue  # the 26.3 overlay wins over src/main
        seen[key] = p
    return seen


def family(key, path):
    ns, name = key.split(':')
    mod = path.split('/')[1] if path.startswith('modules/') else 'simplebuilding'
    if mod == 'simpleend' or (mod == 'simplebuilding' and any(w in name for w in END_WORDS)):
        return mod, 'Simple End', 1
    if mod == 'simpleores':
        return mod, 'Simple Ores', 2
    if mod == 'simpleblocks' or (mod == 'simplebuilding' and name.startswith('block/')
                                 and any(w in name for w in BUILD_WORDS)):
        return mod, 'Simple Blocks / Baubloecke', 3
    if mod != 'simplebuilding':
        return mod, mod, 4
    return mod, 'Kern (sonstige)', 5


def changed_since(base):
    if not base:
        return set()
    out = subprocess.run(['git', 'diff', '--name-only', base, '--', '*.png'], cwd=ROOT, capture_output=True, text=True,
                         check=True).stdout
    return set(out.split())


def rows(base=None):
    out = []; changed = changed_since(base)
    for key, path in sorted(tracked().items()):
        mod, fam, prio = family(key, path)
        m = tr.metrics(ROOT / path)
        light, why = tr.rate(m)
        if any(w in key + '.' for w in SPECIAL):
            light, why = 'grau', ['Maske/UV-Atlas/Animationsteil (nicht als Flaeche gedacht)']
        lock = next((who for word, who in LOCKED if word in key or word in path), '')
        if key.split('/')[-1] in VANILLA_OLD:
            lock = 'vanilla-abgeleitet, Altbestand'
        note = next((n for w, n in NOTES if w in key), '') if light in ('rot', 'orange') else ''
        before = None
        if path in changed:
            blob = subprocess.run(['git', 'show', f'{base}:{path}'], cwd=ROOT, capture_output=True, check=True).stdout
            bm = tr.metrics(Image.open(io.BytesIO(blob)))
            before = dict(light=tr.rate(bm)[0], why=tr.rate(bm)[1], **bm)
            note = 'ueberarbeitet A8d R3 (b-texrate15), vorher ' + before['light'] + ': ' + ', '.join(before['why'])
        out.append(dict(key=key, path=path, mod=mod, family=fam, prio=prio, light=light, why=why, lock=lock, note=note,
                        before=before,
                        layers=(ROOT / path).with_suffix('.layers.json').exists(), **m))
    order = {'rot': 0, 'orange': 1, 'gruen': 2, 'grau': 3}
    out.sort(key=lambda r: (r['prio'], order[r['light']], r['key']))
    return out


def uri(im, k=1):
    if k != 1:
        im = im.resize((im.width * k, im.height * k), Image.NEAREST)
    b = io.BytesIO(); im.save(b, 'PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()


def overlay(path):
    """8x with flat areas outlined (red >= 10 px, orange 6-9 px), like rift14 --dump."""
    im = tr.load(ROOT / path if not isinstance(path, Image.Image) else path)
    k = 8; big = im.resize((im.width * k, im.height * k), Image.NEAREST); px = big.load()
    sc = 256 / (im.width * im.height)
    for _, r in tr.regions(im):
        if len(r) * sc < 6:
            continue
        col = (255, 40, 40, 255) if len(r) * sc >= 10 else (255, 160, 0, 255); s = set(r)
        for x, y in r:
            for dx, dy in tr.N4:
                if (x + dx, y + dy) in s:
                    continue
                for i in range(k):
                    for w in range(2):
                        if dx == 1: px[x * k + k - 1 - w, y * k + i] = col
                        if dx == -1: px[x * k + w, y * k + i] = col
                        if dy == 1: px[x * k + i, y * k + k - 1 - w] = col
                        if dy == -1: px[x * k + i, y * k + w] = col
    return big


def md_table(rs):
    lines = ['| Prio | Familie | Mod | Textur | Ampel | Messwerte (Fl./>=6/Haupt/hart/Farben) | Grund |', '|---|---|---|---|---|---|---|']
    for r in rs:
        if r['light'] in ('gruen', 'grau') or r['prio'] > 3:
            continue  # further modules: counts in the document, every row in the preview
        lines.append(f"| {r['prio']} | {r['family']} | {r['mod']} | `{r['key'].split(':')[1]}`{' (gesperrt ' + r['lock'] + ')' if r['lock'] else ''} | "
                     f"{r['light']} | {r['big']:g}/{r['ge6']:g}/{r['top']:g}/{r['hard']:g}/{r['cols']} | {', '.join(r['why'])}{'; ' + r['note'] if r.get('note') else ''} |")
    return '\n'.join(lines)


CSS = """body{font:14px system-ui,sans-serif;background:#1d1f21;color:#ddd;margin:16px}table{border-collapse:collapse}
td,th{border-bottom:1px solid #333;padding:4px 8px;vertical-align:middle;text-align:left}th{cursor:pointer;background:#2a2c2f;position:sticky;top:0}
img.t{width:128px;height:128px;image-rendering:pixelated;background:repeating-conic-gradient(#555 0 25%,#444 0 50%) 0 0/16px 16px}
img.s{width:16px;height:16px;image-rendering:pixelated}
.rot{color:#ff6b6b;font-weight:bold}.orange{color:#ffb347;font-weight:bold}.gruen{color:#7bd389}
.bar{margin:8px 0}.bar select,.bar input{background:#2a2c2f;color:#ddd;border:1px solid #555;padding:3px}
code{color:#9cdcfe}.ba td{text-align:center}h2{margin-top:28px}"""
JS = """function f(){const m=document.getElementById('fm').value,l=document.getElementById('fl').value,q=document.getElementById('fq').value.toLowerCase();
let n=0;for(const r of document.querySelectorAll('#t tbody tr')){const ok=(!m||r.dataset.fam===m)&&(!l||r.dataset.l===l)&&(!q||r.dataset.k.includes(q));r.style.display=ok?'':'none';if(ok)n++}
document.getElementById('n').textContent=n}
function s(c){const tb=document.querySelector('#t tbody'),rs=[...tb.rows],num=c>=5&&c<=9;const d=tb.dataset.c==c?-1*(+tb.dataset.d||1):1;tb.dataset.c=c;tb.dataset.d=d;
rs.sort((a,b)=>{const x=a.cells[c].dataset.v??a.cells[c].textContent,y=b.cells[c].dataset.v??b.cells[c].textContent;return (num?(+x)-(+y):x.localeCompare(y))*d});rs.forEach(r=>tb.appendChild(r))}"""


def preview(rs, out, before=None, rounds_html=''):
    fams = sorted({(r['prio'], r['family']) for r in rs})
    cnt = {}
    for r in rs:
        cnt.setdefault(r['family'], {}).setdefault(r['light'], 0); cnt[r['family']][r['light']] += 1
    h = [f'<!doctype html><meta charset=utf-8><title>b-texrate15 Textur-Bewertung</title><style>{CSS}</style><script>{JS}</script>',
         '<h1>Textur-Bewertung nach A8d (b-texrate15, 09.11.)</h1>',
         '<p>Antwortcode <b>TEXRATE15</b> = <code>ok</code> | <code>einwand &lt;Textur/Familie&gt;</code> | '
         '<code>mehr &lt;Familie&gt;</code> (weitere Familie ueberarbeiten). Messung <code>tools/textures/texrate.py</code>; '
         'Ampel-Regel siehe docs/ai/TEXTUR-BEWERTUNG-2026-11-09.md. Thumbnails 8x nearest-neighbour; rote/orange mit '
         'Flaechen-Markierung (rot umrandet = einfarbige Flaeche &ge; 10 px, orange 6-9 px).</p>',
         '<table><tr><th>Familie</th><th>rot</th><th>orange</th><th>gruen</th><th>grau</th></tr>' +
         ''.join(f"<tr><td>{f}</td><td class=rot>{cnt[f].get('rot', 0)}</td><td class=orange>{cnt[f].get('orange', 0)}</td>"
                 f"<td class=gruen>{cnt[f].get('gruen', 0)}</td><td>{cnt[f].get('grau', 0)}</td></tr>" for _, f in fams) + '</table>']
    if rounds_html:
        h.append(rounds_html)
    h.append('<h2>Tabelle</h2><div class=bar>Familie <select id=fm onchange=f()><option value="">alle</option>' +
             ''.join(f'<option>{f}</option>' for _, f in fams) +
             '</select> Ampel <select id=fl onchange=f()><option value="">alle</option><option>rot</option><option>orange</option>'
             '<option>gruen</option><option>grau</option></select> Suche <input id=fq oninput=f()> <span id=n></span> Zeilen. Spaltenkopf klicken = sortieren.</div>')
    h.append('<table id=t><thead><tr>' + ''.join(f'<th onclick=s({i})>{c}</th>' for i, c in enumerate(
        ['Bild', 'Familie', 'Mod', 'Textur', 'Ampel', 'gr. Flaeche', '&ge;6 px %', 'Hauptton %', 'hart %', 'Farben', 'Grund'])) + '</tr></thead><tbody>')
    for r in rs:
        img = tr.load(ROOT / r['path'])
        pic = uri(overlay(r['path'])) if r['light'] in ('rot', 'orange') else uri(img, 8)
        name = r['key'].split(':')[1] + (f" <i>(gesperrt {r['lock']})</i>" if r['lock'] else '') + ('' if r['layers'] else ' <i>(ohne .layers.json)</i>')
        lv = {'rot': 0, 'orange': 1, 'gruen': 2, 'grau': 3}[r['light']]
        h.append(f"<tr data-fam=\"{r['family']}\" data-l={r['light']} data-k=\"{r['key']}\"><td><img class=t loading=lazy src='{pic}'></td>"
                 f"<td data-v='{r['prio']}{r['family']}'>{r['family']}</td><td>{r['mod']}</td><td><code>{name}</code></td>"
                 f"<td class={r['light']} data-v={lv}>{r['light']}</td><td>{r['big']:g}</td><td>{r['ge6']:g}</td><td>{r['top']:g}</td>"
                 f"<td>{r['hard']:g}</td><td>{r['cols']}</td><td>{', '.join(r['why'])}{('; ' + r['note']) if r.get('note') else ''}</td></tr>")
    h.append('</tbody></table><script>f()</script>')
    Path(out).write_text('\n'.join(h), encoding='utf-8')


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('--json'); ap.add_argument('--md'); ap.add_argument('--preview'); ap.add_argument('--notes')
    ap.add_argument('--rounds', help='HTML snippet with the before/after section')
    ap.add_argument('--base', default='hub/b-bundle165', help='commit before the rework (for the "vorher" column)')
    a = ap.parse_args(argv)
    rs = rows(a.base)
    if a.notes:  # look-check notes: {"ns:block/x": "kurzer Blick-Befund"}
        notes = json.loads(Path(a.notes).read_text(encoding='utf-8'))
        for r in rs:
            r['note'] = notes.get(r['key'], r['note'])
    if a.json:
        Path(a.json).write_text(json.dumps(rs, indent=1), encoding='utf-8')
    if a.md:
        Path(a.md).write_text(md_table(rs) + '\n', encoding='utf-8')
    if a.preview:
        preview(rs, a.preview, rounds_html=Path(a.rounds).read_text(encoding='utf-8') if a.rounds else '')
    tot = {}
    for r in rs:
        tot.setdefault(r['family'], {}).setdefault(r['light'], 0); tot[r['family']][r['light']] += 1
    for f, c in tot.items():
        print(f'{f:28s} {c}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
