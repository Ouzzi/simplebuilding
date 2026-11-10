#!/usr/bin/env python3
"""b-texrate17 (10.11., N19c): Runde 17 - the red textures outside the TEXRATE16 review and the orange ones Runde 16 could
not improve, rounds R1-R3 (A8c) with the A8d rework engine of texrate16_rework_2026_11_09.py.

Candidates: every red texture plus the `not_improved` of texrate16_targets.json, minus the TEXRATE16 review (464 targets
of c8ece50d6, the owner answers them separately) and minus EXCLUDE (category + reason). Per candidate:
  R1  biggest traffic-light gain: every Runde-16 method, best light, least hard
  R2  gentlest gain: also the Runde-17 recipes (texrate16_rework.R17); among the best light the least mean luminance change
  R3  motif guard (built): like R2, but only recipes that keep >= 80 % of the contrast span (p5-p95 luminance) and >= 85 %
      of the strongest saturation (R1/R2 look: sparkles, gold, furnace mouths, book colours, black joints were lost);
      then OVERRIDE (look check 1x/8x next to Vanilla).
Only targets whose light improves are built (red -> orange/green, orange -> green), as in Runde 16. The targets go to
texrate17_targets.json; texrate16_rework.load_targets() reads it too, so layers.save(), generate_textures.py and the
other hooked generators re-apply the rework (finish()).

    python3 tools/textures/texrate15_2026_11_09.py --base HEAD --json /tmp/r.json   # rating first
    python3 tools/textures/texrate17_2026_11_10.py --select /tmp/r.json              # write texrate17_targets.json
    python3 tools/textures/texrate17_2026_11_10.py --apply                           # rework the files on disk
    python3 tools/textures/texrate17_2026_11_10.py --check                           # every target = finish(base)
"""
from __future__ import annotations

import argparse
import colorsys
import json
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import texrate as tr  # noqa: E402
import texrate16_rework_2026_11_09 as rw  # noqa: E402

ROOT = HERE.parents[1]
OUT = HERE / 'texrate17_targets.json'
REVIEW16 = 'c8ece50d6'  # b-texrate16 head the TEXRATE16 review page was built from (464 targets)
PADS = r'(elytra_pad|launchpad|spawn_teleporter|flypad|chunk_loader|potion_pad)'
# (category, reason, name patterns) - first match wins
EXCLUDE = [
    ('Besitzer-Textur', 'Besitzer-Zeichnung bzw. vom Besitzer gemacht/abgenommen - nie aendern',
     ('bridge_redstone', 'extra_head', 'yarn_ball', 'diamond_pebble', 'astralit_dust', 'trim_template', 'layered_raw_enderite',
      'nihilith_shard', 'cracked_diamond', 'guide_book', 'money_bill')),
    ('Vanilla-Umfaerbung', 'pixelweise Umfaerbung von Vanilla-Texturen -> vanilla_derived.py (Runde 17), nicht bewertbar',
     ('veined_astralit', 'crystalline_astralit', 'layered_astralit', 'veined_nihilith', 'crystalline_nihilith', 'frosted_nihilith')),
    ('leuchtendes Motiv', 'der helle Schein ist das Motiv (Runde 16: Stufung daempfte ihn); Runde 17: kein Gewinn ohne Daempfung',
     ('furnace_front_on', 'enchanter_top', '_lamp_on', '_lamp_top_on', 'dimensional_scrap', 'paint_box', 'glitched_bedrock')),
    ('Vanilla-artiges Glas', 'wie Vanilla-Glas/-Buntglas (selbst rot): grosse ruhige Glasflaeche ist richtig',
     ('display_case_glass', 'transparent_')),
    ('Teilebene', 'wird zur Laufzeit/im Generator zusammengesetzt bzw. Partikel; 2-3 Farben gewollt',
     ('arrow/shaft_', 'arrow/fletching_', 'sandwich/layer_', 'custom_disc_label', 'custom_disc_composed_label', 'confetti')),
    ('andere Abnahme', 'liegt in einer anderen offenen Abnahme (BACK18) - nicht vermischen', ('caution_', 'level_door')),
    ('Backrooms BACK22d', 'b-tex22d (10.11.): eigene Silhouetten je Textur (Besitzer: unique), Generator layers22d - nicht nacharbeiten',
     ('/simplelayers/textures/block/', '/simplelayers/textures/item/', '/simplelayers/textures/mob_effect/')),
    ('Fossil-Stufen', 'zehn Pinsel-Stufen eines Blocks: nur Stufe 0-2 gewinnen, die Knochenkante ist Motiv; einzelne Stufen '
                      'zu aendern liesse das Sediment beim Pinseln springen', ('fossil_',)),
    ('nur messbar', 'Gewinn nur messtechnisch (ein Ton auf wenigen Pixeln, Aenderung <= 1,3 Helligkeit); der eigene '
                    'Modul-Generator (check_data --check) muesste dafuer umgebaut werden', ('questionary_table_gem', 'strip_station_bars')),
]
TINT = (re.compile(r'(ashwood|duskwood)_leaves(_inner|_loose)?$|end_grass$'),
        'Toenungsmaske (BlockColorsMixin faerbt grau ein) - nicht bewertbar')
# Look check after R3 (1x/8x next to Vanilla froglights, birch/cherry door, bookshelf, lectern):
#  froglights: quant recipes snapped the bright centre to a grey member colour (scarlet side), step-limited breakers
#  (<= 6-10 luminance) found only grey ramp tones (grey blobs), grain drew horizontal strokes across Vanilla's vertical
#  side streaks -> only the plain area breaker or the diagonal stripe recipe (tones of the own ramp);
#  ashwood_door_bottom: R2 stripe keeps the planks and frame (span 0.79, just below the guard) -> taken.
ONLY = {'froglight': ('areas', 'stripe')}
FORCE = {'ashwood_door_bottom': 'stripe'}
GROUPS = [('Esche/Daemmerholz + Kristall-Laub', r'(ashwood|duskwood)'),
          ('Froschlichter', r'froglight'),
          ('Baubloecke + Modellflaechen', r'.')]
ORDER = {'rot': 0, 'orange': 1, 'gruen': 2}
SPAN, SAT = 0.8, 0.85


def review16() -> set:
    out = subprocess.run(['git', 'show', f'{REVIEW16}:tools/textures/texrate16_targets.json'], cwd=ROOT, capture_output=True,
                         text=True)
    tg = set(json.loads(out.stdout)['targets']) if out.returncode == 0 else set()
    cur = json.loads((HERE / 'texrate16_targets.json').read_text(encoding='utf-8'))
    return tg | set(cur['targets'])


def excluded(name: str):
    for cat, why, pats in EXCLUDE:
        if any(p in name for p in pats):
            return cat, why
    if TINT[0].search(name):
        return 'Toenungsmaske', TINT[1]
    return None


def guard(im):
    """(contrast span p5-p95 luminance, strongest saturation*value) of the opaque pixels."""
    a = im.convert('RGBA').load(); w = im.width
    lum, sat = [], []
    for y in range(w):
        for x in range(w):
            p = a[x, y]
            if p[3] == 255:
                lum.append(tr.lum(p))
                sat.append(colorsys.rgb_to_hsv(*[c / 255 for c in p[:3]])[1] * max(p[:3]) / 255)
    if not lum:
        return 0, 0
    lum.sort()
    n = len(lum)
    return lum[int(n * .95) - 1] - lum[int(n * .05)], max(sat)


def change(a, b):
    """Mean absolute luminance change over the opaque pixels of the first frame."""
    a, b = a.convert('RGBA').load(), b.convert('RGBA').load()
    s = n = 0
    for y in range(16):
        for x in range(16):
            if a[x, y][3] == 255:
                s += abs(tr.lum(a[x, y]) - tr.lum(b[x, y])); n += 1
    return s / max(1, n)


def trials(base, name):
    """Every recipe at R3: dict(m, light, why, hard, ch, guard_ok, span, sat)."""
    sp0, sa0 = guard(base)
    out = []
    for m in rw.ROUNDS:
        im = rw.rework(base, name, m, 3)
        mm = tr.metrics(im); light, why = tr.rate(mm); sp, sa = guard(im)
        out.append(dict(m=m, light=light, why=', '.join(why), hard=mm['hard'], ch=round(change(base, im), 2),
                        guard_ok=sp >= SPAN * sp0 and sa >= SAT * sa0, span=round(sp / max(1, sp0), 2),
                        sat=round(sa / max(.01, sa0), 2), r17=m in rw.R17))
    return out


def pick(ts, stem):
    """R1, R2, R3 of one texture (see module doc)."""
    best = lambda xs: max(ORDER[t['light']] for t in xs)
    r16 = [t for t in ts if not t['r17']]
    b1 = best(r16)
    r1 = min((t for t in r16 if ORDER[t['light']] == b1), key=lambda t: t['hard'])
    b2 = best(ts)
    r2 = min((t for t in ts if ORDER[t['light']] == b2), key=lambda t: t['ch'])
    only = next((ms for pat, ms in ONLY.items() if pat in stem), None)
    pool = [t for t in ts if t['guard_ok'] and (only is None or t['m'] in only)]
    if stem in FORCE:
        r3 = next(t for t in ts if t['m'] == FORCE[stem])
    else:
        b3 = best(pool)
        r3 = min((t for t in pool if ORDER[t['light']] == b3), key=lambda t: t['ch'])
    return r1, r2, r3


def group_of(stem):
    return next(g for g, pat in GROUPS if re.search(pat, stem))


def select(rows):
    tabu = review16()
    t16 = json.loads((HERE / 'texrate16_targets.json').read_text(encoding='utf-8'))
    targets, excl, not_imp = {}, {}, {}
    for r in rows:
        if r['path'] in tabu or re.search(PADS, r['key']):
            continue
        if not (r['light'] == 'rot' or r['path'] in t16['not_improved']):
            continue
        name = r['key'].split(':')[1]
        ex = excluded(name)
        if ex:
            excl[r['path']] = dict(category=ex[0], why=ex[1], light=r['light'])
            continue
        base = rw.base_of(ROOT / r['path'])
        stem = Path(r['path']).stem
        r1, r2, r3 = pick(trials(base, Path(r['path']).name), stem)
        rounds = {k: {q: v[q] for q in ('m', 'light', 'why', 'ch', 'span', 'sat')} for k, v in (('R1', r1), ('R2', r2), ('R3', r3))}
        if ORDER[r3['light']] > ORDER[r['light']]:
            targets[r['path']] = dict(group=group_of(stem), method=r3['m'], light=r['light'], why=', '.join(r['why']),
                                      after=r3['light'], after_why=r3['why'], rounds=rounds)
        else:
            not_imp[r['path']] = dict(light=r['light'], why=', '.join(r['why']), rounds=rounds,
                                      reason='kein Ampel-Gewinn mit Motiv-Schutz (Kontrast/Saettigung)')
    return targets, excl, not_imp


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('--select', help='rating JSON (texrate15_2026_11_09.py --base HEAD --json)')
    ap.add_argument('--apply', action='store_true')
    ap.add_argument('--check', action='store_true')
    a = ap.parse_args(argv)
    if a.select:
        rows = json.loads(Path(a.select).read_text(encoding='utf-8'))
        tg, ex, ni = select(rows)
        doc = {'note': 'b-texrate17 (10.11., N19c): Runde 17 targets (method = texrate16_rework recipe, rounds R1-R3 with '
                       'rating), exclusions with category/reason, candidates without gain; see texrate17_2026_11_10.py',
               'groups': {g: '' for g, _ in GROUPS}, 'targets': dict(sorted(tg.items())), 'excluded': dict(sorted(ex.items())),
               'not_improved': dict(sorted(ni.items()))}
        OUT.write_text(json.dumps(doc, indent=1, ensure_ascii=False) + '\n', encoding='utf-8')
        from collections import Counter
        print(len(tg), 'targets', Counter(v['group'] for v in tg.values()), '| excluded', len(ex),
              Counter(v['category'] for v in ex.values()), '| not improved', len(ni))
    doc = json.loads(OUT.read_text(encoding='utf-8')) if OUT.exists() else {'targets': {}}
    rw._TARGETS = None  # re-read both target files
    if a.apply:
        rw.apply(list(doc['targets']))
        print(f"applied {len(doc['targets'])}")
    if a.check:
        bad = rw.apply(list(doc['targets']), check=True)
        print('\n'.join('nicht aktuell: ' + b for b in bad) or f"OK: {len(doc['targets'])} Ziele aktuell")
        return 1 if bad else 0
    return 0


if __name__ == '__main__':
    sys.exit(main())
