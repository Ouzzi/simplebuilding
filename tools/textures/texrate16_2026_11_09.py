#!/usr/bin/env python3
"""b-texrate16 (09.11., N13d continued): pick the rework targets after a fresh texrate rating and give every target its
problem group + method (texrate16_rework_2026_11_09.py), write texrate16_targets.json.

Selection: every red texture plus the orange ones of the visible problem groups (pads, lamps, ash/duskwood, light panes,
display cases, froglights) and of Simple End / Simple Ores / Simple Blocks (priority 1-3). Never: owner-drawn textures
(OWNER), the Vanilla-recolour alternates (texrate15 VANILLA_OLD), masks/animation parts (grau), glass that is
transparent like Vanilla glass (display_case_glass*, transparent_*).

    python3 tools/textures/texrate16_2026_11_09.py --select BEFORE.json      write texrate16_targets.json
    python3 tools/textures/texrate16_2026_11_09.py --generators              which generator writes each target (hooked?)
    python3 tools/textures/texrate16_2026_11_09.py --preview OUT.html        before/after page (1x + 8x, traffic lights,
                                                                             filter mod/group, rounds R0-R3 per group)
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import texrate15_2026_11_09 as t15  # noqa: E402

ROOT = HERE.parents[1]
OUT = HERE / 'texrate16_targets.json'
# Owner-drawn (Besitzer-Zeichnungen, tools/textures/hand/owner/, b-tex16 bridge_redstone): never changed.
# Also owner-made or owner-approved hand drawings: cracked diamond block (owner texture cleaned, 7b C), guide books
# (approved guide_book.png, hand-made), trim templates (owner textures 1:1). The pads are the owner's originals quantised
# by generate_textures (02.10., hand/pad_res); N13d names them explicitly -> reworked, flagged as Rueckfrage.
OWNER = ('bridge_redstone', 'extra_head', 'yarn_ball', 'diamond_pebble', 'astralit_dust', 'trim_template',
         'layered_raw_enderite', 'nihilith_shard', 'cracked_diamond', 'guide_book', 'money_bill')
VANILLA_GLASS = ('display_case_glass', 'transparent_')
# b-fix169tex (bundle 169): not reworked - textures of module generators that do not call finish() (their check_data would
# report the reworked PNG as stale: Simple Dinos eggs, the octet melon flesh, the questionary table) and the chiseled motifs
# of b-pal16 (owner answer PAL15, symmetric motifs; PAL16 won the merge conflict, so the A8d layer is not on them).
GENERATOR_OWN = ('egg_trex', 'egg_triceratops', 'melon_flesh', 'questionary_table_book', 'questionary_table_side',
                 'chiseled_brass', 'chiseled_lead', 'chiseled_nickel', 'chiseled_silver', 'chiseled_tin', 'chiseled_zinc',
                 'chiseled_astralit_bricks', 'chiseled_ender_quartz_bricks', 'chiseled_nihilith_bricks')
# Glowing faces whose bright motif is the point (lit furnace fire, enchanter glow, lit lamps, sparks on black, rainbow):
# the contrast step-down dulled them in R3 (look check) -> not reworked, like Vanilla furnace_front_on/glowstone.
GLOW = ('furnace_front_on', 'blast_furnace_front_on', 'enchanter_top', '_lamp_on', '_lamp_top_on', 'dimensional_scrap',
        'paint_box', 'glitched_bedrock')  # glitched bedrock: Vanilla bedrock is hard 47 % as well
# (group, name pattern, method, why this Vanilla model) - first match wins
GROUPS = [
    ('Pads', r'(elytra_pad|launchpad|spawn_teleporter|flypad|chunk_loader|potion_pad)',
     'pad', 'Vanilla respawn_anchor_top/lodestone_top: Glut-/Ringmotiv in 2-3 Stufen, Kante bleibt Motiv (Vanilla '
     'end_portal_frame_top hart 43 %) -> Kontrast der Extreme senken bis hart <= 20 %, dann Flaechen brechen'),
    ('Lampen', r'_lamp(_top)?(_on)?$', 'lamp',
     'Vanilla redstone_lamp(_on): Maeander mit harten Kanten ist Motiv (Vanilla hart 18/48 %), aber Felder dort <= 88 px '
     'und mit 2-3 Toenen gebrochen -> nur Felder mit feinen Tonstufen brechen, Linien wechseln den Ton'),
    ('Licht-Scheiben', r'light_pane', 'quant',
     'Vanilla copper_bulb_lit/shroomlight: leuchtende Flaeche aus 6-12 Stufen ohne weichen Verlauf -> Verlauf auf 8 Toene '
     'stufen, Flaeche mit feinen 2-4-px-Haufen brechen (Rahmen/Nieten bleiben)'),
    ('Froschlichter', r'froglight', 'frog',
     'Vanilla ochre/verdant_froglight: 6-8 Toene, Rand in Stufen, Mitte darf ruhig sein (Vanilla-Mitte 132 px) -> weichen '
     'Verlauf auf 8 echte Toene der Textur stufen; die helle Mitte bleibt ruhig wie bei Vanilla (Haufen auf Fast-Weiss '
     'wirkten in R2 wie graue Flecken)'),
    ('Vitrinen', r'display_case_frame', 'stripe',
     'Vanilla glass_pane_top/oak_planks: Glas bleibt wie Vanilla-Glas (nicht bewertet); Rahmen-Schraegstreifen sind Motiv '
     '-> Streifenkontrast in 2 Stufen senken (hart <= 20 %), Toene nur laengs der Streifen wechseln'),
    ('Vitrinen', r'(display_case|sales_storage|shop)', 'areas',
     'Vanilla lectern_top/barrel: Grundflaechen wie Bretter in 2-4-px-Haufen gebrochen'),
    ('Esche/Daemmerholz', r'(ashwood|duskwood|blazewood|_door|trapdoor|stripped_|_planks|_sticks|_sign|cutting_board)',
     'grain', 'Vanilla birch/cherry door+trapdoor, stripped_*_log: Maserung als 2-3-px-Striche laengs der Bretter '
     '(keine runden Haufen)'),
    ('Kanten-Motive', r'(chess/|cracked_|dimensional_scrap|furnace_front_on|enchanter_top|glitched_bedrock)', 'pad',
     'Vanilla glowstone/target/crying_obsidian: Funken/Risse duerfen hart sein, aber Extreme gestuft -> hart <= 20 %'),
    ('Holz-/Lederflaechen (Modell)', r'(backpack|crucible|pocket_jukebox|barrel_flange|crafting_table|claim_table|'
     r'questionary_table|death_vault|strip_station|casting_tray)', 'areas',
     'Vanilla barrel/chest/lectern: Holz/Leder/Metall in 2-4-px-Haufen eines Nachbartons, Drift oben/links hell'),
]
DEFAULT = ('Flaechen (allgemein)', 'areas', 'passende Vanilla-Baustoffe (Tiefenschiefer, Endstein, polished_*): 2-4-px-'
           'Haufen der eigenen Rampe, Drift, Linien-Tonwechsel (texrate15 R3)')
PROBLEM = {'Pads', 'Lampen', 'Licht-Scheiben', 'Froschlichter', 'Vitrinen', 'Esche/Daemmerholz'}


def group_of(key: str, row: dict):
    name = key.split(':')[1]
    for g, pat, meth, why in GROUPS:
        if re.search(pat, name):
            return g, meth, why
    if row['cols'] > 32:
        return 'Weiche Verlaeufe', 'quant', 'Vanilla-Baustoffe haben 6-16 Toene: Verlauf auf 8 Toene stufen, dann fein brechen'
    return DEFAULT


def select(rows):
    out = {}
    for r in rows:
        name = r['key'].split(':')[1]
        if r['light'] not in ('rot', 'orange') or r['lock'].startswith('vanilla'):
            continue
        if any(o in name for o in OWNER + GENERATOR_OWN) or any(v in name for v in VANILLA_GLASS) or any(g in name for g in GLOW):
            continue
        g, meth, why = group_of(r['key'], r)
        if r['light'] == 'orange' and g not in PROBLEM and r['prio'] > 3:
            continue
        out[r['path']] = dict(group=g, method=meth, light=r['light'], why=', '.join(r['why']))
    return out


ORDER = {'rot': 0, 'orange': 1, 'gruen': 2}


def keep_improved(tg):
    """Drop targets whose R3 does not improve the traffic light (orange -> orange only dulls an accepted texture; red ->
    red is a thin/motif shape the method cannot fix) - except the lamps (Vanilla redstone_lamp is red as well; the fields
    are broken, the meander stays). Records the R3 rating per kept target."""
    import texrate as tr
    import texrate16_rework_2026_11_09 as rw
    out, dropped = {}, {}
    for rel, v in tg.items():
        base = rw.base_of(ROOT / rel)
        new = rw.rework(base, Path(rel).name, v['method'])
        light, why = tr.rate(tr.metrics(new))
        if ORDER[light] > ORDER[v['light']] or v['group'] == 'Lampen':
            out[rel] = dict(v, after=light, after_why=', '.join(why))
        else:
            dropped[rel] = dict(v, after=light)
    return out, dropped


def generators(targets):
    """{target: [(script, hooked)]}: scripts under tools/textures that name the target's file stem."""
    scripts = {p: p.read_text(encoding='utf-8', errors='replace') for p in HERE.glob('*.py')
               if not p.name.startswith(('texrate', 'vanilla_nearness', 'abn', 'texfix_preview'))}
    res = {}
    for t in targets:
        stem = Path(t).stem
        hits = []
        for p, src in scripts.items():
            if re.search(r"['\"/_]" + re.escape(stem) + r"['\".]", src):
                hooked = 'layers.save(' in src or p.name == 'generate_textures.py' or 'texrate16_rework' in src
                hits.append((p.name, hooked))
        res[t] = hits
    return res


def _uri(im):
    import base64
    import io
    b = io.BytesIO(); im.save(b, 'PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()


CSS = """body{font:14px system-ui,sans-serif;background:#1d1f21;color:#ddd;margin:16px}table{border-collapse:collapse}
td,th{border-bottom:1px solid #333;padding:3px 6px;vertical-align:middle;text-align:left}th{background:#2a2c2f;position:sticky;top:0}
img{image-rendering:pixelated;background:repeating-conic-gradient(#555 0 25%,#444 0 50%) 0 0/8px 8px}.x1{width:16px;height:16px}
.x8{width:128px;height:128px}.r{width:64px;height:64px}.rot{color:#ff6b6b;font-weight:bold}.orange{color:#ffb347;font-weight:bold}
.gruen{color:#7bd389}.bar{margin:8px 0}select,input{background:#2a2c2f;color:#ddd;border:1px solid #555;padding:3px}code{color:#9cdcfe}
.box{background:#26282b;padding:8px 12px;margin:10px 0;border-left:3px solid #7bd389}small{color:#aaa}"""
JS = """function f(){const m=document.getElementById('fm').value,g=document.getElementById('fg').value,l=document.getElementById('fl').value,q=document.getElementById('fq').value.toLowerCase();
let n=0;for(const r of document.querySelectorAll('#t tbody tr')){const ok=(!m||r.dataset.m===m)&&(!g||r.dataset.g===g)&&(!l||r.dataset.l===l)&&(!q||r.dataset.k.includes(q));r.style.display=ok?'':'none';if(ok)n++}
document.getElementById('n').textContent=n}"""


def preview(out, before_json, after_json):
    import texrate as tr
    import texrate16_rework_2026_11_09 as rw
    from collections import Counter
    doc = json.loads(OUT.read_text(encoding='utf-8'))
    tg = doc['targets']
    bef = json.loads(Path(before_json).read_text(encoding='utf-8'))
    aft = {r['path']: r for r in json.loads(Path(after_json).read_text(encoding='utf-8'))}
    mod = {r['path']: r['mod'] for r in bef}
    fams = sorted({(r['prio'], r['family']) for r in bef})
    cb = Counter((r['family'], r['light']) for r in bef); ca = Counter((r['family'], r['light']) for r in aft.values())
    L = ('rot', 'orange', 'gruen', 'grau')
    h = [f'<!doctype html><meta charset=utf-8><title>b-texrate16 Textur-Ueberarbeitung</title><style>{CSS}</style><script>{JS}</script>',
         '<h1>Textur-Ueberarbeitung Runde 16 nach A8c/A8d (b-texrate16, 09.11.)</h1>',
         '<div class=box>Antwortcode <b>TEXRATE16</b> = <code>ok</code> | <code>einwand &lt;Textur/Gruppe&gt;</code> (Ziel wird aus '
         '<code>tools/textures/texrate16_targets.json</code> genommen, alte Pixel kommen zurueck) | <code>pads alt</code> (Pads beruhen auf '
         'Besitzer-Originalen, 02.10. quantisiert - nur Kontrast gesenkt; bitte bestaetigen) | <code>mehr &lt;Gruppe&gt;</code>. '
         'Jede Zeile: vorher/nachher in Spielgroesse (1x) und 8x nearest-neighbour, Ampel vorher/nachher (Messwerte gr. Flaeche / '
         '&ge;6 px % / Hauptton % / hart % / Farben).</div>',
         '<h2>Zaehlung je Familie (vorher &rarr; nachher)</h2><table><tr><th>Familie</th>' +
         ''.join(f'<th>{l}</th>' for l in L) + '</tr>' +
         ''.join(f"<tr><td>{f}</td>" + ''.join(f"<td class={l}>{cb[(f, l)]} &rarr; {ca[(f, l)]}</td>" for l in L) + '</tr>'
                 for _, f in fams) +
         f"<tr><td><b>gesamt</b></td>" + ''.join(f"<td class={l}><b>{sum(cb[(f, l)] for _, f in fams)} &rarr; "
                                                 f"{sum(ca[(f, l)] for _, f in fams)}</b></td>" for l in L) + '</tr></table>']
    # rounds per group (A8c): three examples each, R0..R3 with rating
    h.append('<h2>Runden je Gruppe (A8c)</h2>')
    groups = {}
    for rel, v in tg.items():
        groups.setdefault(v['group'], []).append(rel)
    for g, rels in groups.items():
        why = doc['groups'].get(g, '') if not g.startswith('Runde 15') else 'texrate15 R3, unveraendert - nur in den Generatoren verankert'
        h.append(f'<h3>{g} ({len(rels)})</h3><p><small>Vanilla-Vorbild/Methode: {why}</small></p><table><tr><th>Textur</th><th>R0</th><th>R1</th><th>R2</th><th>R3 (gebaut)</th></tr>')
        for rel in rels[::max(1, len(rels) // 3)][:3]:
            base = rw.base_of(ROOT / rel)
            cells = []
            for n in range(4):
                im = rw.rework(base, Path(rel).name, tg[rel]['method'], n) if tg[rel]['method'] != 'r15' or n in (0, 3) else None
                if im is None:
                    cells.append('<td>-</td>'); continue
                m = tr.metrics(im); light = tr.rate(m)[0]
                cells.append(f"<td><img class=r src='{_uri(tr.load(im))}'><br><span class={light}>{light}</span> "
                             f"<small>{m['big']:g}/{m['ge6']:g}/{m['top']:g}/{m['hard']:g}/{m['cols']}</small></td>")
            h.append(f"<tr><td><code>{Path(rel).stem}</code></td>{''.join(cells)}</tr>")
        h.append('</table>')
    h.append('<p><small>Runden: R1 erster Wurf (grobe Grenze), R2 enger + Drift/Maserung/Stufen, R3 = R2 + Hauptton &le; 38 % '
             '(gebaut). Blick-Befunde und Abweichungen: docs/ai/PLAN-B-TEXRATE16-2026-11-09.md.</small></p>')
    gl = sorted(groups)
    mods = sorted({mod.get(rel, '?') for rel in tg})
    h.append('<h2>Alle Ziele</h2><div class=bar>Mod <select id=fm onchange=f()><option value="">alle</option>' +
             ''.join(f'<option>{m}</option>' for m in mods) + '</select> Gruppe <select id=fg onchange=f()><option value="">alle</option>' +
             ''.join(f'<option>{g}</option>' for g in gl) + '</select> Ampel nachher <select id=fl onchange=f()><option value="">alle</option>'
             '<option>rot</option><option>orange</option><option>gruen</option></select> Suche <input id=fq oninput=f()> <span id=n></span> Zeilen</div>')
    h.append('<table id=t><thead><tr><th>Textur</th><th>Mod</th><th>Gruppe</th><th>vorher 1x</th><th>nachher 1x</th><th>vorher 8x</th>'
             '<th>nachher 8x</th><th>Ampel vorher</th><th>Ampel nachher</th></tr></thead><tbody>')
    for rel, v in tg.items():
        base = rw.base_of(ROOT / rel); new = tr.load(Image.open(ROOT / rel)); b = tr.load(base)
        mb, mn = tr.metrics(b), tr.metrics(new); lb, wb = tr.rate(mb); ln, wn = tr.rate(mn)
        ub, un = _uri(b), _uri(new)
        h.append(f"<tr data-m=\"{mod.get(rel, '?')}\" data-g=\"{v['group']}\" data-l={ln} data-k=\"{rel.lower()}\"><td><code>{Path(rel).stem}</code>"
                 f"<br><small>{rel.split('/textures/')[-1]}</small></td><td>{mod.get(rel, '?')}</td><td>{v['group']}</td>"
                 f"<td><img class=x1 src='{ub}'></td><td><img class=x1 src='{un}'></td><td><img class=x8 src='{ub}'></td>"
                 f"<td><img class=x8 src='{un}'></td><td class={lb}>{lb}<br><small>{', '.join(wb)}</small></td>"
                 f"<td class={ln}>{ln}<br><small>{', '.join(wn)}</small></td></tr>")
    h.append('</tbody></table><script>f()</script>')
    Path(out).write_text('\n'.join(h), encoding='utf-8')


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('--select', help='rating JSON (texrate15_2026_11_09.py --json)')
    ap.add_argument('--generators', action='store_true')
    ap.add_argument('--preview', nargs=3, metavar=('OUT', 'BEFORE_JSON', 'AFTER_JSON'))
    a = ap.parse_args(argv)
    if a.select:
        rows = json.loads(Path(a.select).read_text(encoding='utf-8'))
        tg, dropped = keep_improved(select(rows))
        import texrate15_rework_2026_11_09 as r15
        for fam, paths in r15.TARGETS.items():  # texrate15 R3 (reworked 09.11.), anchored in the generators as well
            for rel in paths:
                tg.setdefault(rel, dict(group='Runde 15 (' + fam + ')', method='r15', light='rot', why='b-texrate15',
                                        after='siehe Runde 15'))
        doc = {'note': 'b-texrate16 (09.11.): A8d rework targets; method see texrate16_rework_2026_11_09.py, groups and '
                       'Vanilla models see texrate16_2026_11_09.py GROUPS',
               'groups': {g: why for g, _, _, why in reversed(GROUPS)} | {DEFAULT[0]: DEFAULT[2]},
               'targets': dict(sorted(tg.items())),
               'not_improved': {k: v['group'] + ': ' + v['light'] + ' -> ' + v['after'] for k, v in sorted(dropped.items())}}
        OUT.write_text(json.dumps(doc, indent=1, ensure_ascii=False) + '\n', encoding='utf-8')
        from collections import Counter
        print(len(tg), Counter(v['group'] for v in tg.values()), 'not improved', len(dropped))
    if a.generators:
        tg = json.loads(OUT.read_text(encoding='utf-8'))['targets']
        for t, hits in generators(tg).items():
            print(t, hits)
    if a.preview:
        preview(*a.preview)
    return 0


if __name__ == '__main__':
    sys.exit(main())
