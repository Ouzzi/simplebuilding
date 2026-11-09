#!/usr/bin/env python3
"""Erzeugt docs/FEATURES-ALLE-MODS.md aus modules/modules.json + wiki/data/<mod>.json
(+ Handbuch-Einleitungen aus tools/guides/module_guides.py) und die Übersichtsseite.

Nichts wird geraten: jede Zeile stammt aus den Wiki-Daten und nennt deren Fundstelle (Datei/Klasse).
Aufruf: python3 tools/featuredoc/gen_features.py [--html PFAD]
"""
import html
import importlib.util
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).parent))
from fun_concepts import CONCEPTS  # noqa: E402

MODS = json.loads((ROOT / 'modules/modules.json').read_text(encoding='utf-8'))['modules']
BY_ID = {m['id']: m for m in MODS}


def load_intros():
    p = ROOT / 'tools/guides/module_guides.py'
    s = importlib.util.spec_from_file_location('mg', p)
    mod = importlib.util.module_from_spec(s)
    s.loader.exec_module(mod)
    return {c['module']: c['intro'] for c in mod.MODULES if c.get('intro')}


INTROS = load_intros()


def data(mid):
    p = ROOT / 'wiki/data' / f'{mid}.json'
    return json.loads(p.read_text(encoding='utf-8')) if p.is_file() else {}


def nm(mid):
    return BY_ID[mid].get('displayName') or BY_ID[mid]['name'] if mid in BY_ID else mid


def tr(d, key, lang='de'):
    """Text mit Rückfall auf EN; Titel notfalls aus dem Top-Level-Feld `title` (manual.json-Form)."""
    if key == 'title' and isinstance(d.get('title'), dict):
        t = d['title']
        return t.get(lang) or t.get('en') or ''
    return (d.get(lang) or {}).get(key) or (d.get('en') or {}).get(key) or ''


def norm(s):
    return re.sub(r'[^a-z0-9]', '', s.lower())


FAMILIES = [
    ('Basis', ['simplelib']),
    ('SimpleBuilding und seine Sub-Mods', ['simplebuilding'] + [m['id'] for m in MODS if m.get('parent') == 'simplebuilding']),
    ('Simple Quality of Life (Super-Mod) und Sub-Mods', ['simplequalityoflife'] + [m['id'] for m in MODS if m.get('parent') == 'simplequalityoflife']),
    ('Simple Aesthetics (Super-Mod) und Sub-Mods', ['simpleaesthetics'] + [m['id'] for m in MODS if m.get('parent') == 'simpleaesthetics']),
    ('Simple Optimization (Super-Mod) und Sub-Mods', ['simpleoptimization'] + [m['id'] for m in MODS if m.get('parent') == 'simpleoptimization']),
]
used = {i for _, ids in FAMILIES for i in ids}
FAMILIES.append(('Eigenständige Mods und übrige Sub-Mods', [m['id'] for m in MODS if m['id'] not in used]))
# doppelt vermeiden (Reihenfolge der Familien gewinnt)
seen = set()
for i, (fam, ids) in enumerate(FAMILIES):
    out = []
    for x in ids:
        if x not in seen and x in BY_ID:
            seen.add(x)
            out.append(x)
    FAMILIES[i] = (fam, out)

EXTRA = {
    'simplevisuals': ['Pro-Effekt-Überschreibung (`particles.overrides`, Abbildung Effekt-Name zu Stufe Aus/Dezent/Normal/Stark/Maximum) gilt vor der globalen Stufe; Fundstelle `modules/simplevisuals/shared/java/com/simplevisuals/config/SimplevisualsConfig.java`. Config-Bildschirm: `modules/simplevisuals/shared/java/com/simplevisuals/client/VisualsConfigScreen.java`.'],
    'simplesounds': ['Config-Bildschirm und alle sechs Optionen: `modules/simplesounds/shared/java/com/simplebuilding/modules/simplesounds/SoundConfig.java` (globalLevel, followVisuals, volumeCap, soundsPerTick, soundsPerPlayer, cooldownTicks).'],
    'simplequalityoflife': ['Config-Bildschirm: `modules/simplequalityoflife/shared/java/com/simplequalityoflife/client/QolConfigScreen.java`; die Schalter selbst: `SimplequalityoflifeConfig.java` (sieben `enableSimple…`). Die Funktionen der Sub-Mods stehen in deren eigenen Abschnitten (Simple Weather, Interfaces, Loot, Mob Manners, Farming, Tools, Movement).'],
    'simpleend': ['End-Gemälde (`simpleend:end_painting`): wie ein Gemälde anbringen, 46 End-Bilder (Größen und Anzahl wie Vanilla), jedes Bild hat drei kurze Animationen und spielt etwa alle ein bis zwei Minuten zufällig eine davon. Fundstelle `modules/simpleend/wiki/manual.json` (Eintrag `simpleend:end_painting`).'],
    'simplelib': ['Textur-Editor (Entwickler-Wiki, nur Entwicklung): Ebenen-Modi, Malen mit Deckung, die Seite aktualisiert sich nach dem Speichern. Fundstelle `tools/textures/editor_save.py`, `docs/ai/PLAN-B-TEXED16-2026-11-09.md`. Kein Spielfeature, nicht in den Wiki-Daten.',
                  'Lizenz: Alle Mods stehen unter „Simple Mods License – All Rights Reserved, Source-Available“ (SPDX `LicenseRef-SimpleMods-ARR`) mit Beitragsvereinbarung (CLA). Fundstelle `LICENSE`, `docs/ai/PLAN-B-LIC15-2026-11-09.md`.'],
}
# Module, bei denen alle Detailzeilen (Unterpunkte, Zeitpläne, Modi, Stufen) mitgedruckt werden
FULL_DETAIL = {'simplevisuals', 'simplesounds', 'simplequalityoflife', 'simpletweaks', 'simplemodels', 'simplegadgets',
               'simplelayers', 'simplefun', 'simpleores', 'simplechisels', 'simpleend', 'simplelib'}
# Pflicht-Stichwörter für `--check` (N10c): fehlt eins im erzeugten Dokument, fehlt die Quelle (manual.json) oder EXTRA
REQUIRED = ['Strip', 'Backrooms', 'Wolkenmeer', 'Schrott', 'Gemälde', 'Wetter-Zeitplan', 'Konfetti', 'Metall', 'Attrakt', 'Detektor',
            'Editor', 'Kapitel', 'Textur-Editor', 'Lizenz']


def default_of(text):
    m = re.search(r'(?:Standard|Default):\s*([^\s.][^.]*?(?:\.\d+)?)\s*(?:\.|$)', text or '')
    return m.group(1).strip() if m else ''


def build_mod(mid):
    m = BY_ID[mid]
    d = data(mid)
    feats = d.get('features', [])
    cfg_feats = [f for f in feats if f['id'].startswith('config_')]
    main = [f for f in feats if not f['id'].startswith('config_')]
    collapsed = [f for f in main if re.match(r'(Trade:|Linked Price Table:)', f.get('en', {}).get('title', ''))]
    main = [f for f in main if f not in collapsed]

    cfg_rows = []
    for f in cfg_feats:
        key = f['id'][len('config_'):]
        summ = tr(f, 'summary')
        cfg_rows.append({'key': key, 'title': tr(f, 'title'), 'en': (f.get('en') or {}).get('title', ''),
                         'summary': summ, 'default': default_of(summ) or default_of((f.get('en') or {}).get('summary', '')),
                         'source': (f.get('sources') or [''])[0]})
    for c in d.get('config', []):  # nur SimpleBuilding liefert strukturierte Optionen
        cfg_rows.append({'key': c['name'], 'title': c.get('labelDe') or c.get('label') or c['name'],
                         'en': c.get('label') or '', 'summary': c.get('tooltipDe') or c.get('tooltip') or '',
                         'default': str(c.get('default', '')), 'source': c.get('source', '')})

    rows = []
    for f in main:
        fid = norm(f['id'])
        sw = []
        if len(fid) >= 4:
            for c in cfg_rows:
                k = norm(c['key'])
                if k and (fid in k or (len(k) >= 6 and k in fid)):
                    sw.append(c['key'])
        rows.append({'id': f['id'], 'title': tr(f, 'title'), 'en': (f.get('en') or {}).get('title') or (f.get('title') or {}).get('en', '') if isinstance(f.get('title'), dict) else (f.get('en') or {}).get('title', ''),
                     'summary': tr(f, 'summary'), 'details': [x for x in (f.get('de') or f.get('en') or {}).get('details', []) if x and x != tr(f, 'summary')],
                     'sources': f.get('sources', []), 'switch': sw[:4]})

    def names(lst, n=8):
        out = []
        for x in lst:
            nmx = x.get('name') or {}
            if x['id'].endswith(':guide_book'):
                continue
            out.append(nmx.get('de_de') or nmx.get('en_us') or x['id'])
            if len(out) >= n:
                break
        return out

    c = d.get('counts', {})
    intro = INTROS.get(mid)
    deps_req = [nm(x) if x in BY_ID else x for x in m.get('requires', [])]
    deps_opt = [nm(x) if x in BY_ID else x for x in m.get('optional', [])]
    return {
        'id': mid, 'name': nm(mid), 'version': (d.get('mod') or {}).get('version') or m.get('version'),
        'loaders': m.get('loaders', []), 'desc_en': m.get('description', ''),
        'desc_de': intro[1] if intro else '', 'parent': m.get('parent'), 'bundle': m.get('bundle'),
        'switch': m.get('configSwitch'),
        'counts': {'blocks': c.get('blocks', len(d.get('blocks', []))), 'items': c.get('items', len(d.get('items', []))),
                   'recipes': c.get('recipes', 0), 'loot': c.get('lootTables', 0), 'ench': c.get('enchantments', 0),
                   'adv': c.get('advancements', 0), 'features': len(feats), 'config': len(cfg_rows)},
        'top_blocks': names(d.get('blocks', [])), 'top_items': names(d.get('items', [])),
        'req': deps_req, 'opt': deps_opt, 'rows': rows, 'cfg': cfg_rows, 'collapsed': len(collapsed),
        'collapsed_kind': sorted({f['en']['title'].split(':')[0] for f in collapsed}),
    }


MODDATA = {mid: build_mod(mid) for _, ids in FAMILIES for mid in ids}


def src(s):
    return f'`{s}`'


def md_mod(b):
    o = []
    o.append(f"### {b['name']} (`{b['id']}`)")
    o.append('')
    desc = b['desc_de'] or b['desc_en']
    o.append(f"**Kurzbeschreibung:** {desc}")
    if b['desc_de'] and b['desc_en']:
        o.append(f"*(EN, Manifest: {b['desc_en']})*")
    meta = [f"Version {b['version']}", 'Loader ' + '/'.join(b['loaders'])]
    if b['parent']:
        meta.append(f"Sub-Mod von {nm(b['parent'])} ({'Pflicht' if b['bundle'] == 'required' else 'optional'}), auch allein spielbar")
    o.append('')
    o.append('- ' + ' · '.join(meta))
    c = b['counts']
    o.append(f"- Umfang: {c['blocks']} Blöcke, {c['items']} Items, {c['recipes']} Rezepte, {c['loot']} Beutetabellen, {c['ench']} Verzauberungen, {c['adv']} Fortschritte, {c['features']} Wiki-Features, {len(b['cfg'])} Config-Optionen"
             + (' (Quelle: `wiki/data/%s.json`)' % b['id']))
    if b['top_blocks']:
        o.append('- Wichtigste Blöcke: ' + ', '.join(b['top_blocks']))
    if b['top_items']:
        o.append('- Wichtigste Items: ' + ', '.join(b['top_items']))
    o.append('- Abhängigkeiten: erforderlich: ' + (', '.join(b['req']) or 'keine') + '; optional: ' + (', '.join(b['opt']) or 'keine'))
    if b['switch']:
        o.append(f"- Config-Schalter im Super-Mod: `enable{b['name'].replace(' ', '')}` (Fundstelle `modules/modules.json`, `configSwitch`)")
    o.append('')
    if b['rows']:
        o.append('**Features**')
        o.append('')
        for r in b['rows']:
            t = f"{r['title']} ({r['en']})" if r['en'] and r['en'] != r['title'] else r['title']
            line = f"- **{t}** – {r['summary']}"
            if r['switch']:
                line += ' Schalter: ' + ', '.join(f'`{x}`' for x in r['switch']) + '.'
            if r['sources']:
                line += ' Fundstelle: ' + ', '.join(src(s) for s in r['sources'][:3]) + '.'
            o.append(line)
            if b['id'] in FULL_DETAIL:
                for dtl in r['details']:
                    o.append(f'  - {dtl}')
        for x in EXTRA.get(b['id'], []):
            o.append(f'- {x}')
        if b['collapsed']:
            o.append(f"- *{b['collapsed']} weitere Einträge der Art {', '.join(b['collapsed_kind'])} (Handel/Preislisten) siehe `wiki/data/{b['id']}.json`.*")
        o.append('')
    if b['cfg']:
        o.append('**Config-Optionen** (Name – Beschriftung – Standard – Fundstelle)')
        o.append('')
        for r in b['cfg']:
            e = f" ({r['en']})" if r['en'] and r['en'] != r['title'] else ''
            dflt = f" – Standard `{r['default']}`" if r['default'] else ''
            o.append(f"- `{r['key']}` – {r['title']}{e}{dflt} – {src(r['source'])}")
        o.append('')
    if not b['rows'] and not b['cfg']:
        o.append('*Keine eigenen Wiki-Features erfasst.*')
        o.append('')
    return '\n'.join(o)


def md_fun():
    o = ['## Simple Fun – 10 neue Konzepte', '']
    fun = MODDATA['simplefun']
    o.append(f"Bestand: Simple Fun hat heute {len(fun['rows'])} Wiki-Features und {len(fun['cfg'])} Config-Optionen; der Abschnitt „Simple Fun“ oben listet alle mit Fundstelle. "
             'Kurz: Schleichwurf (Yeet), Wurfziegel samt Ziegel-Schneeball, Schweinverwandlung, Schadlos (Feder und Verzauberung), Rückstoß V, '
             'acht kosmetische Freuden (Blumenschnuppern, Keksbrösel, Apfelfunkeln, Karottenknacken, Melonenspritzer, Honigblasen, Brotkrümel, Beerenröte), Admin-Befehle, Config-Bildschirm, Handbuch.')
    o.append('')
    o.append('Regeln für alle Konzepte: nur Kosmetik oder kleine, abschaltbare Mechanik; jeder Eintrag bekommt einen Serverschalter (Standard an für Kosmetik, aus bei Gameplay-Wirkung); '
             'keine Doppelung mit vorhandenen Features (geprüft gegen alle `wiki/data/*.json`-Titel und -Texte); Wirkungen werden nach PRINZIPIEN A14 sichtbar telegrafiert. '
             'A12 = Nutzen im Vanilla-Spiel / Änderung am Spielgeschehen / Stärke des Eingriffs. Aufwand: S unter 1 h, M ein Agentenlauf, L mehrere Läufe.')
    o.append('')
    for c in CONCEPTS:
        o.append(f"### FUN11-{c['n']}: {c['de']} ({c['en']})")
        o.append('')
        o.append(c['text'])
        o.append('')
        o.append(f"- **A12 Vanilla-Nutzen:** {c['a12']}")
        o.append(f"- **Eingriff:** {c['eingriff']} · **Aufwand:** {c['aufwand']}")
        o.append(f"- **Abgrenzung:** {c['abgrenzung']}")
        o.append('')
    o.append('**Antwortcode:** `FUN11: <nummern> + Anmerkung`, zum Beispiel `FUN11: 1 2 5 + Konfetti nur mit Papier` (Nummern 1–10; „alle“ oder „keine“ ist auch gültig).')
    o.append('')
    return '\n'.join(o)


def gen_md():
    tot = {k: sum(b['counts'][k] for b in MODDATA.values()) for k in ('blocks', 'items', 'recipes', 'features', 'config')}
    o = ['# Feature-Dokument aller Mods', '',
         f'Stand 09.11.2026, Basis `claude-wave1` (MC 26.3). Besitzer-Wünsche W3 und W4. Erzeugt von `python3 tools/featuredoc/gen_features.py` aus `modules/modules.json`, `wiki/data/<mod>.json` (= Wiki-Daten aus `modules/*/wiki/manual.json`, Lang-Dateien, Config-Klassen, Rezepten) und den Handbuch-Einleitungen (`tools/guides/module_guides.py`). Nicht von Hand bearbeiten, sondern Wiki-Daten korrigieren und neu erzeugen.', '',
         f'**{len(MODDATA)} Module**, zusammen {tot["blocks"]} Blöcke, {tot["items"]} Items, {tot["recipes"]} Rezepte, {tot["features"]} Wiki-Features und {tot["config"]} Config-Optionen (Blöcke/Items zählen Kern und Sub-Mods getrennt; der Kern weist seine eigenen aus, die Sub-Mods ihre).', '',
         '## Lesehilfe', '',
         '- Feature-Namen stehen Deutsch, Englisch in Klammern. „Schalter“ ordnet Config-Optionen nach übereinstimmendem Namen zu (Heuristik); fehlt sie, gilt das Feature ohne eigenen Schalter oder der Schalter steht im Text. Alle Config-Optionen des Mods stehen darunter vollständig.',
         '- „Fundstelle“ ist die Quelldatei bzw. Klasse, aus der das Wiki-Feature belegt ist (`sources` im Wiki-Datensatz). Zahlen stammen aus `counts` derselben Datei.',
         '- Erhalt: Rezepte, Handel und Funde stehen in der Beschreibung des Features oder im Mod-Handbuch (Buch im Spiel, `modules/<mod>/shared/.../guide`); Rezept-Details je Item im Wiki (`wiki/data/<mod>.json`, Feld `recipes`).',
         '- Grenzen: Texte sind Wiki-Daten mit Stand der Datei; nicht geprüft wurde Spielverhalten im Client. Manche Sub-Mods haben nur wenige Items in den Daten, weil ihr Inhalt im Kern (SimpleBuilding) registriert wird.', '',
         '## Inhaltsverzeichnis', '']
    for fam, ids in FAMILIES:
        o.append(f'- {fam}: ' + ', '.join(f"[{MODDATA[i]['name']}](#{anchor(i)})" for i in ids))
    o.append('- [Simple Fun – 10 neue Konzepte](#simple-fun--10-neue-konzepte)')
    o.append('')
    for fam, ids in FAMILIES:
        o.append(f'## {fam}')
        o.append('')
        if fam.startswith('Simple Quality'):
            o.append('Simple QoL bündelt sieben Sub-Mods mit je einem Schalter (`enableSimple…`). Alle Features der Sub-Mods stehen in deren Abschnitten unten; Simple QoL selbst trägt Schalter, Handbuch, Startquest und `/simplequalityoflife submod`. Quelle der Familie: `docs/ai/RELEASE-QOL-OPT-AES.md`, `docs/MODSTRUKTUR.md`.')
            o.append('')
        if fam.startswith('Simple Aesthetics'):
            o.append('Simple Visuals (HUD, Tooltips, Immersionspartikel), Simple Sounds (lokale Klänge dazu) und Simple Models (eigene 3D-Modelle) sind drei Sub-Mods; Sounds folgt optional der Visuals-Stufe. Quelle: `docs/ai/RELEASE-QOL-OPT-AES.md`, `modules/simplevisuals/AUDIT.md`.')
            o.append('')
        for i in ids:
            o.append(md_mod(MODDATA[i]))
    o.append(md_fun())
    return '\n'.join(o) + '\n'


def anchor(mid):
    b = MODDATA[mid]
    s = f"{b['name']} ({mid})".lower()
    s = re.sub(r'[^\w\- ]', '', s).replace(' ', '-')
    return s


# ---------------- HTML ----------------
def esc(s):
    return html.escape(s or '')


def gen_html():
    parts = []
    toc = []
    for fam, ids in FAMILIES:
        toc.append(f'<div class="fam"><b>{esc(fam)}</b> ')
        for i in ids:
            toc.append(f'<a href="#m-{i}">{esc(MODDATA[i]["name"])}</a> ')
        toc.append('</div>')
        parts.append(f'<h2 class="famh" data-fam>{esc(fam)}</h2>')
        for i in ids:
            b = MODDATA[i]
            c = b['counts']
            h = [f'<details class="mod" id="m-{i}"><summary><span class="mn">{esc(b["name"])}</span> <code>{i}</code>'
                 f'<span class="cnt">{len(b["rows"])} Features · {len(b["cfg"])} Optionen · {c["blocks"]} Blöcke · {c["items"]} Items</span></summary>']
            h.append(f'<p>{esc(b["desc_de"] or b["desc_en"])}</p>')
            h.append(f'<p class="meta">Version {esc(str(b["version"]))} · {esc("/".join(b["loaders"]))}'
                     + (f' · Sub-Mod von {esc(nm(b["parent"]))}' if b['parent'] else '') + '</p>')
            h.append(f'<p class="meta">Erforderlich: {esc(", ".join(b["req"]) or "keine")} · Optional: {esc(", ".join(b["opt"]) or "keine")}</p>')
            if b['top_blocks']:
                h.append(f'<p class="meta">Blöcke: {esc(", ".join(b["top_blocks"]))}</p>')
            if b['top_items']:
                h.append(f'<p class="meta">Items: {esc(", ".join(b["top_items"]))}</p>')
            if b['rows']:
                h.append('<ul class="fl">')
                for r in b['rows']:
                    t = f'{r["title"]} ({r["en"]})' if r['en'] and r['en'] != r['title'] else r['title']
                    sw = (' <span class="sw">Schalter: ' + esc(', '.join(r['switch'])) + '</span>') if r['switch'] else ''
                    sc = f'<span class="src">{esc(r["sources"][0])}</span>' if r['sources'] else ''
                    dt = ''.join(f'<br><small>{esc(x)}</small>' for x in r['details']) if i in FULL_DETAIL else ''
                    h.append(f'<li><b>{esc(t)}</b> – {esc(r["summary"])}{sw}{dt}<br>{sc}</li>')
                h.extend(f'<li>{esc(x)}</li>' for x in EXTRA.get(i, []))
                h.append('</ul>')
            if b['cfg']:
                h.append(f'<details class="cfg"><summary>{len(b["cfg"])} Config-Optionen</summary><ul class="fl">')
                for r in b['cfg']:
                    e = f' ({r["en"]})' if r['en'] and r['en'] != r['title'] else ''
                    d = f' – Standard <code>{esc(r["default"])}</code>' if r['default'] else ''
                    h.append(f'<li><code>{esc(r["key"])}</code> {esc(r["title"])}{esc(e)}{d}<br><span class="src">{esc(r["source"])}</span></li>')
                h.append('</ul></details>')
            h.append('</details>')
            parts.append('\n'.join(h))
    cards = []
    for c in CONCEPTS:
        cards.append(f'<article class="card" data-n="{c["n"]}"><h3><span class="num">{c["n"]}</span> {esc(c["de"])} <small>({esc(c["en"])})</small></h3>'
                     f'<p>{esc(c["text"])}</p><p><b>A12 Vanilla-Nutzen:</b> {esc(c["a12"])}</p>'
                     f'<p><b>Eingriff:</b> {esc(c["eingriff"])} · <b>Aufwand:</b> {esc(c["aufwand"])}</p>'
                     f'<p class="meta"><b>Abgrenzung:</b> {esc(c["abgrenzung"])}</p>'
                     f'<label class="pick"><input type="checkbox" value="{c["n"]}"> Dabei</label></article>')
    tpl = (Path(__file__).parent / 'page.html').read_text(encoding='utf-8')
    return (tpl.replace('@@TOC@@', '\n'.join(toc)).replace('@@MODS@@', '\n'.join(parts))
            .replace('@@CARDS@@', '\n'.join(cards)).replace('@@NMODS@@', str(len(MODDATA))))


if __name__ == '__main__':
    if '--check' in sys.argv:
        md = gen_md().lower()
        miss = [k for k in REQUIRED if k.lower() not in md]
        print('fehlend:', miss or 'keine')
        sys.exit(1 if miss else 0)
    (ROOT / 'docs/FEATURES-ALLE-MODS.md').write_text(gen_md(), encoding='utf-8')
    if '--html' in sys.argv:
        Path(sys.argv[sys.argv.index('--html') + 1]).write_text(gen_html(), encoding='utf-8')
    print('ok', len(MODDATA), 'Mods')
