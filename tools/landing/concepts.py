#!/usr/bin/env python3
"""LAND10 (IG31-54): vier Seiten (index/paket/reise/discover) mit Navigation.

  python3 tools/landing/concepts.py [vorschau-ordner]

1. index.html  = vorschlag-2.html (Besitzer 06.11.: V2) + nur groessere Schrift/Abstaende (H1 dominant, H2 kleiner).
2. konzept-1.html  Modpack-Baukasten (interaktiv, Abhaengigkeiten aus modules.json/modtree.py live)
   konzept-2.html  Reise durchs Spiel (Abschnitte Start -> Endgame, Items mit eigenen Texturen und Lang-Namen)
   konzept-3.html  Schaufenster (Hero-Kacheln, Zahlen-Leiste aus echten Daten, Zeitleiste "Was ist neu")
Daten: modules/modules.json (modtree.load), Lang-Dateien de_de/en_us, git-verfolgte Mod-Texturen
(modules/*/shared/..., src/main/resources, mc26_3/overlay; nie build/, nie wiki/, nie assets/minecraft).
Mit Ordnerargument zusaetzlich a-land09-default/-k1/-k2/-k3.html + a-land09-preview.html dorthin.
"""
import base64, glob, html, io, json, os, re, subprocess, sys
from pathlib import Path
from PIL import Image

D = Path(__file__).resolve().parent
R = D.parents[1]
sys.path.insert(0, str(R / 'tools/modtree'))
import modtree  # noqa: E402

E = html.escape
CHECK = '--check' in sys.argv  # wiki/generate.py --check: nur vergleichen, nichts schreiben
ARGS = [a for a in sys.argv[1:] if a != '--check']
OUT = {}  # Dateiname -> Inhalt; am Ende geschrieben (oder bei --check verglichen)

# ---------------------------------------------------------------- Texturen (W12: bei jedem Lauf frisch aus dem Repo)
TRACKED = set(subprocess.run(['git', '-C', str(R), 'ls-files', '*.png'], capture_output=True, text=True).stdout.split())
ROOTS = ['modules/*/shared/resources/assets/{ns}/textures', 'mc26_3/overlay/resources/assets/{ns}/textures',
         'src/main/resources/assets/{ns}/textures']
# Anpassbar: tools/landing/overrides.json (Besitzer setzt je Kachel Bild/Text, siehe README)
OVR = json.loads((D / 'overrides.json').read_text(encoding='utf-8')) if (D / 'overrides.json').exists() else {}
TEXMAP = {k: v for k, v in (OVR.get('textures') or {}).items() if not k.startswith('_')}


def _derived():
    """Vanilla-abgeleitete Texturen (tools/textures/vanilla_derived.py) sind nie oeffentlich: 'assets/ns/textures/rel'."""
    try:
        sys.path.insert(0, str(R / 'tools/textures'))
        import vanilla_derived as vd
        out = {f'assets/simplebuilding/textures/{k}' for k in vd.OUTPUTS}
        for ns, m in vd.MODULE_OUTPUTS.items():
            out |= {f'assets/{ns}/textures/{k}' for k in m}
        return out
    except Exception:
        return set()


DERIVED = _derived()


def _uri(im):
    b = io.BytesIO(); im.save(b, 'PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()


def _own_png(relp):
    return (relp in TRACKED and '/build/' not in relp and '/assets/minecraft/' not in relp
            and ('assets/' + relp.split('/assets/', 1)[-1]) not in DERIVED)


def tex(spec):
    """'ns:block/name' -> data-URI (nur git-verfolgte eigene Mod-Texturen, Animationsstreifen -> erstes Bild).
    overrides.json "textures" kann eine Textur durch eine andere ('ns:pfad') oder eine Datei ('file:bild.png',
    relativ zu tools/landing/) ersetzen."""
    spec = TEXMAP.get(spec, spec)
    if spec.startswith('file:'):
        return _uri(Image.open(D / spec[5:]))
    ns, rel = spec.split(':')
    assert ns != 'minecraft'
    for r in ROOTS:
        for p in glob.glob(str(R / r.format(ns=ns) / f'{rel}.png')):
            relp = os.path.relpath(p, R)
            if _own_png(relp):
                im = Image.open(p)
                if im.height > im.width:
                    im = im.crop((0, 0, im.width, im.width))
                return _uri(im)
    raise SystemExit(f'Textur fehlt: {spec}')


ICONS = {}
for p in glob.glob(f'{R}/modules/*/shared/resources/assets/*/icon_128.png') + [f'{R}/src/main/resources/assets/simplebuilding/icon_128.png']:
    mid = p.split('/modules/')[1].split('/')[0] if '/modules/' in p else 'simplebuilding'
    if mid != 'wiringexample' and _own_png(os.path.relpath(p, R)):
        ICONS[mid] = 'data:image/png;base64,' + base64.b64encode(open(p, 'rb').read()).decode()
for mid, o in (OVR.get('mods') or {}).items():
    if not mid.startswith('_') and o.get('icon'):
        ICONS[mid] = tex(o['icon'])


def fresh_icons(h):
    """Jedes <img data-ico="id"> der Basis bekommt das aktuelle Icon (oder das aus overrides.json)."""
    return re.sub(r'<img data-ico="([a-z0-9_]+)" src="[^"]*"',
                  lambda m: f'<img data-ico="{m.group(1)}" src="{ICONS.get(m.group(1), "")}"', h)

# ---------------------------------------------------------------- Teil 1: neue Basis
SIZES = ('h1{font-size:clamp(52px,13vw,128px);line-height:1;letter-spacing:4px;margin:0 0 22px}'
         'h2{font-size:clamp(28px,4.6vw,40px);line-height:1.15;margin:72px 0 22px}'
         '.hero{padding:88px 0 56px}.sub{font-size:19px;line-height:1.55;margin:0 auto 32px}'
         '.c b{font-size:18px}.c p{font-size:15px;line-height:1.55}'
         '@media(max-width:600px){.hero{padding:60px 0 36px}h2{margin:52px 0 16px}}')
v2 = fresh_icons((D / 'vorschlag-2.html').read_text(encoding='utf-8'))
NAVCSS = ('.tn{display:flex;flex-wrap:wrap;gap:6px;padding:8px 118px 8px 8px;font-family:"Silkscreen","Courier New",monospace}'
          '.tn a{color:#cbbf9f;text-decoration:none;font-size:14px;padding:6px 10px;border:3px solid #000;background:#3b3329;'
          'box-shadow:inset -3px -3px 0 #28221a,inset 3px 3px 0 #54493a}'
          '.tn a:hover{color:#fff}.tn a[aria-current=page]{background:#55a630;color:#fff}'
          '@media(max-width:600px){.tn{padding-right:104px}.tn a{font-size:12px;padding:5px 7px}}')
i = v2.rfind('</style>')
base = v2[:i] + SIZES + NAVCSS + v2[i:]

NAVITEMS = [('index.html', 'Start', 'Start'), ('paket.html', 'Paket zusammenstellen', 'Build your pack'),
            ('reise.html', 'Reise', 'Journey'), ('discover.html', 'Discover', 'Discover')]


def nav(active):
    return '<div class="tn">' + ''.join(
        f'<a href="{h}" data-en="{E(en)}"' + (' aria-current="page"' if h == active else '') + f'>{E(de)}</a>'
        for h, de, en in NAVITEMS) + '</div>'


# Bausteine der Basis fuer die Konzepte
HEAD = base[:base.find('</head>')]
LANGBOX = re.search(r'<div class="lg".*?</div>', base).group(0)
OUT['index.html'] = base.replace(LANGBOX, LANGBOX + nav('index.html'), 1)
HERO = re.search(r'<div class="hero">.*?</div></div>', base).group(0)
ICG_I = base.find('<h2 data-en="All ')
ICG = base[ICG_I:base.find('<footer', ICG_I)]
FOOTER = re.search(r'<footer.*?</footer>', base).group(0)
SCRIPT = re.search(r'<script>.*?</script>', base, re.S).group(0).replace(
    'if(save)try', 'document.dispatchEvent(new CustomEvent("smlang",{detail:x}));if(save)try', 1)
# ---------------------------------------------------------------- Daten
MODS = {k: v for k, v in modtree.load(R).items() if k != 'wiringexample'}
SUP, REST = modtree.roots(MODS)
LANG = {'de': {}, 'en': {}}
for l, f in (('de', 'de_de'), ('en', 'en_us')):
    for p in [R / 'src/main/resources/assets/simplebuilding/lang' / f'{f}.json',
              R / 'mc26_3/overlay/resources/assets/simplebuilding/lang' / f'{f}.json'] + \
             sorted(Path(R).glob(f'modules/*/shared/resources/assets/*/lang/{f}.json')):
        if p.exists():
            LANG[l].update(json.loads(p.read_text(encoding='utf-8')))


def name(spec, l):
    ns, rel = spec.split(':')
    path = rel.split('/')[-1]
    for suf in ('_front_on', '_front', '_side', '_top', '_on'):
        if path.endswith(suf) and not any(k.endswith('.' + path) for k in LANG[l]):
            path = path[:-len(suf)]
    for k in (f'block.{ns}.{path}', f'item.{ns}.{path}'):
        if k in LANG[l]:
            return LANG[l][k]
    for k, v in LANG[l].items():
        if re.fullmatch(r'(block|item)\.[a-z0-9_]+\.' + re.escape(path), k):
            return v
    raise SystemExit(f'Name fehlt: {spec} ({l})')


def langkey(k, l):
    return LANG[l][k]


def counts():
    keys = {}
    for k in LANG['en']:
        m = re.fullmatch(r'(block|item)\.([a-z0-9_]+)\.([a-z0-9_]+)', k)
        if m:
            keys.setdefault(m.group(2), {}).setdefault(m.group(3), set()).add(m.group(1))
    other = {p for ns, d in keys.items() if ns != 'simplebuilding' for p in d}
    blocks = items = 0
    for ns, d in keys.items():
        if ns not in MODS and ns != 'simplebuilding':
            continue
        for p, kinds in d.items():
            if ns == 'simplebuilding' and p in other:
                continue  # verschobene ids nicht doppelt zaehlen
            if 'block' in kinds: blocks += 1
            else: items += 1
    texs = len({p.split('/assets/', 1)[1] for p in TRACKED if '/textures/' in p and '/assets/minecraft/' not in p
               and (p.startswith('modules/') or p.startswith('src/main/') or p.startswith('mc26_3/overlay/'))})
    return dict(mods=len(MODS), sup=len(SUP), blocks=blocks, items=items, tex=texs)


# Kurztexte je Mod (DE, EN); Quelle: modules.json-Beschreibungen, gekuerzt
TXT = {
 'simplebuilding': ('Der Kern: Bauwerkzeuge, Speicher, Maschinen und Verzauberungen; bündelt viele Sub-Mods.', 'The core: building tools, storage, machines and enchantments; bundles many sub mods.'),
 'simplelib': ('Gemeinsame Basis: Materialien, Upgrades in der Welt, Handbuch und UI-Bausteine.', 'Shared base: materials, in-world upgrades, the guide and UI building blocks.'),
 'simplemoney': ('Geldscheine, gewichtete Dorfbewohner-Händel und Schatz-Beute.', 'Bank notes, weighted villager trades and treasure loot.'),
 'simpleriding': ('Schnellere Sättel, Pferdesprünge, Nautilus-Sprints und Reittier-Rüstung.', 'Faster saddles, horse jumps, nautilus dashes and mount armor.'),
 'simplemodels': ('Eigene Item-Modelle, vom Server freigegeben, am Amboss zuweisen.', 'Custom item models approved by the server, assigned at the anvil.'),
 'simplefun': ('Verspielte Mechaniken: werfbare Ziegel und kosmetische Verwandlungen.', 'Playful mechanics: throwable bricks and cosmetic transformations.'),
 'simplevisuals': ('HUD, Tooltips und mehr Vanilla-Stimmung am Client.', 'HUD, tooltips and more Vanilla immersion on the client.'),
 'simplesounds': ('Passende Klänge zu den Effekten von Simple Visuals.', 'Matching sounds for the Simple Visuals effects.'),
 'simplequalityoflife': ('Super-Mod für Komfort: Wetter, Oberflächen, Beute, Mob-Manieren, Farm, Werkzeuge, Bewegung.', 'Super mod for comfort: weather, screens, loot, mob manners, farming, tools, movement.'),
 'simpletweaks': ('Kompatibilität und Grundstücks-Schutz (Claims), serverseitig.', 'Compatibility and server-side land claims.'),
 'simpledimensions': ('Einstellbare Dimensionen, Lichtbögen und sichere verknüpfte Portale.', 'Configurable dimensions, light arches and safe linked portals.'),
 'simplesandwiches': ('Sandwiches auf dem Schneidebrett, Messer, Käse und Butter.', 'Sandwiches on a cutting board, a knife, cheese and butter.'),
 'simpleinterfaces': ('Neuer Look für Vanilla-Behälter: farbiger Kasten je Block.', 'A new look for Vanilla containers: a coloured box per block.'),
 'simplemaps': ('Wegfinder-Karten: endlos je Dimension, Zoom und Wegpunkte.', 'Wayfinder maps: endless per dimension, zoom and waypoints.'),
 'simplelivemap': ('Live-Webkarte der Welt, so wie die Spieler sie erkundet haben.', 'Live web map of the world as players really explored it.'),
 'simpletrims': ('Leuchtende, strahlende und pulsierende Rüstungsbesätze.', 'Glowing, emitting and pulsating armor trims.'),
 'simplemobs': ('Bosse wie Hollow Howler und Hollow Watcher und neue Mobs.', 'Bosses like the Hollow Howler and Hollow Watcher and new mobs.'),
 'simpleweather': ('Wetter-Schalter am Server und Regendichte am Client.', 'Weather switch on the server and rain density on the client.'),
 'simplemovement': ('Free Look, Kriechen, Autowalk und schnellere Leitern.', 'Free Look, crawling, autowalk and faster ladders.'),
 'simpletools': ('Sparsamkeit, Seelenbindung und der Todes-Tresor.', 'Thrift, Soulbound and the Death Vault.'),
 'simplefarming': ('Federfall schützt Ackerboden, Hacke erntet in der Fläche.', 'Feather Falling protects farmland, the hoe harvests an area.'),
 'simplemobmanners': ('Mobs stummschalten, Babys jung halten, friedlichere Piglins.', 'Mute mobs, keep babies young, calmer piglins.'),
 'simpleloot': ('Tresor-Abklingzeit in Tagen statt einmal je Spieler.', 'Vault cooldown in days instead of once per player.'),
 'simplequestionary': ('Der Fragetisch: Item hineinlegen, Herkunft und Nutzen lesen.', 'The Questionary Table: put an item in, read its lore and use.'),
 'simpleaesthetics': ('Super-Mod für Optik: Visuals, Sounds und Models.', 'Super mod for looks: Visuals, Sounds and Models.'),
 'simpleenddragonfight': ('Drachenkampf, der mit der Zahl der Spieler mitwächst.', 'An Ender Dragon fight that scales with the players.'),
 'simpleend': ('Zwei End-Hölzer, End-Gras, Bäume und Früchte.', 'Two End woods, End grass, trees and fruit.'),
 'simplerespawn': ('Niedergeschlagen statt tot: Mitspieler können dich retten.', 'Downed instead of dead: teammates can revive you.'),
 'simplebeacons': ('Starkes Leuchtfeuer mit 6 Stufen und 10 weiteren Effekten.', 'Strong Beacon with 6 levels and 10 more effects.'),
 'simpleovens': ('Verstärkte, Netherit- und Enderit-Öfen, Räucher- und Schmelzöfen.', 'Reinforced, Netherite and Enderite furnaces, smokers and blast furnaces.'),
 'simplemobheads': ('Ein Kopf für jeden Mob, zum Aufstellen und Tragen.', 'A head for every mob, to place and to wear.'),
 'simpleshops': ('Spieler-Läden: Verkaufsblock mit eigenem Preis.', 'Player shops: a sales block with your own price.'),
 'simplewanderers': ('Ein Wanderhändler je Dorf, mit Pferd an der Leine.', 'One wandering trader per village, with a leashed horse.'),
 'simplestats': ('Jedes Werkzeug zählt seine eigene Statistik.', 'Every tool keeps its own statistics.'),
 'simpleblocks': ('Fehlende Baublöcke, Formsäge und kompakte Blöcke.', 'Missing building blocks, the shapecutter and compact blocks.'),
 'simplecrucibles': ('Tiegel kochen alle Plätze zugleich, beheizt vom Block darunter.', 'Crucibles cook every slot at once, heated from below.'),
 'simplebuildingtools': ('Bauwerkzeuge: Muster kopieren, Flächen füllen und Blöcke sauber ersetzen.', 'Building tools: copy patterns, fill areas and replace blocks cleanly.'),
 'simplechisels': ('Meißel in mehreren Materialien, um Blöcke zu formen und zu verzieren.', 'Chisels in several materials to shape and decorate blocks.'),
 'simpleoctets': ('Octets: halbe Würfel aus jedem Holz und Block.', 'Octets: half-block cubes of every wood and block.'),
 'simplehammers': ('Sieben Vorschlaghämmer, die 3x3 abbauen und Blöcke formen.', 'Seven sledgehammers that mine 3x3 and reshape blocks.'),
 'simpleoptimization': ('Super-Mod für Leistung: Render, Speicher, Server.', 'Super mod for performance: render, memory, server.'),
 'simpleoptrender': ('Client-Optimierungen wie Blätter-Culling und Lichtcache.', 'Client optimizations like leaf culling and a light cache.'),
 'simpleoptmemory': ('Speicher-Optimierungen für Blockzustände und Modelle.', 'Memory optimizations for block states and models.'),
 'simpleoptserver': ('Sichtweite folgt der Serverlast.', 'View distance follows the server load.'),
 'simplestorage': ('Kisten, Rucksäcke, Fässer, Rohre und Truhen-Netzwerk.', 'Crates, backpacks, barrels, pipes and a chest network.'),
 'simplemusic': ('Notenblätter, Plattenpresse und eigene Schallplatten.', 'Sheet music, the disc press and your own records.'),
 'simplelights': ('Lichtscheiben: flache Lampen auf jeder Blockseite.', 'Light panes: flat lamps on any block side.'),
 'simplegadgets': ('Handgeräte wie der Tacho mit Fallschaden-Warnung.', 'Handheld gadgets like the gauge with a fall warning.'),
 'simpleearth': ('Die Oberwelt von oben gesehen, aus der Umlaufbahn.', 'The Overworld seen from orbit.'),
 'simplechallenges': ('Spielmodi für eine Welt deiner Wahl.', 'Game modes for a world of your choice.'),
 'simpleanimals': ('Hirsch, Wildschwein, Igel, Krabbe und Zebra.', 'Deer, wild boar, hedgehog, crab and zebra.'),
 'simpledinos': ('Fossilien, Dino-Eier zum Ausbrüten und reitbare Dinos.', 'Fossils, dino eggs to hatch and rideable dinos.'),
 'simpleores': ('Silber, Roségold, Enderit und Sage-Kristalle.', 'Silver, rose gold, enderite and sage crystals.'),
 'simplemachines': ('Doppelkolben, Signaldämpfer, Kolben- und Trichter-Stufen.', 'Double piston, signal dampener, piston and hopper tiers.'),
 'simplearrows': ('Pfeiltisch, Licht- und Silberpfeil.', 'Fletching table, light and silver arrows.'),
 'simpleresonance': ('Spielerwert, Gefahrenzonen und Spawn-Besitzer.', 'Player value, danger zones and spawn owners.'),
 'simplefoundry': ('Gießerei als Multiblock: Schmelzkammern und Gussformen.', 'A multiblock foundry: melting chambers and casts.'),
 'simpleenchanting': ('Dunkler und Astral-Verzauberer über dem Vanilla-Tisch.', 'Dark and Astral Enchanter above the Vanilla table.'),
 'simplepads': ('Startrampen, Elytra-Pads, Druckplatten und Spawn-Teleporter.', 'Launchpads, elytra pads, pressure plates and a spawn teleporter.'),
}
for _k, _o in (OVR.get('mods') or {}).items():  # overrides.json: Kurztext je Mod
    if not _k.startswith('_') and _k in TXT:
        TXT[_k] = (_o.get('de', TXT[_k][0]), _o.get('en', TXT[_k][1]))
missing = [k for k in MODS if k not in TXT]
assert not missing, missing
EXT = {'cloth_config': 'Cloth Config', 'cloth-config': 'Cloth Config'}
VER = re.search(r'^mod_version=(.+)$', (R / 'gradle.properties').read_text(), re.M).group(1).strip()


def family(pid):
    ids = [pid]
    for c in modtree.children(MODS, pid):
        ids += family(c['id'])
    return ids


GROUPS = [(r['id'], family(r['id'])) for r in SUP] + [('', [m['id'] for m in REST] + ['simplelib'])]


def t(de, en, tag='span', cls='', attrs=''):
    c = f' class="{cls}"' if cls else ''
    return f'<{tag}{c}{attrs} data-en="{E(en)}">{E(de)}</{tag}>'


def box(de_t, en_t, de, en):
    return ''  # LAND10: Konzept-Erklaerkasten entfernt
    return (f'<aside class="kz"><b data-en="Concept: {E(en_t)}">Konzept: {E(de_t)}</b>'
            f'<p data-en="{E(en)}">{E(de)}</p></aside>')


KZ_CSS = ('.kz{margin:32px 0 8px;padding:16px 20px;background:#2a2520;border:3px dashed #ffd75e}'
          '.kz b{display:block;font-family:"Silkscreen","Courier New",monospace;font-weight:400;font-size:20px;color:#ffd75e;margin-bottom:6px}'
          '.kz p{margin:0;font-size:15px;color:#d7ccb2;max-width:820px}'
          '.px{image-rendering:pixelated;image-rendering:crisp-edges}'
          '.sk{font-family:"Silkscreen","Courier New",monospace;font-weight:400}')


def page(fname, css, body, js=''):
    h = HEAD.replace('<title>Simple Mods</title>', '<title>Simple Mods</title>', 1)
    h = h[:h.rfind('</style>')] + KZ_CSS + css + h[h.rfind('</style>'):]
    out = (h + '</head><body>' + LANGBOX + nav(fname) + HERO + '<div class="w">' + body + FOOTER + '</div>'
           + (f'<script>{js}</script>' if js else '') + SCRIPT + '</body></html>')
    OUT[fname] = out
    return out


C = counts()

# ---------------------------------------------------------------- K1 Baukasten
def k1():
    data = {}
    for k, m in MODS.items():
        data[k] = dict(n=m['name'], de=TXT[k][0], en=TXT[k][1], p=m['parent'], b=m['bundle'],
                       r=[x for x in m['req'] if x in MODS], o=[x for x in m['opt'] if x in MODS],
                       x=sorted({EXT.get(x, x) for x in m['req'] if x not in MODS}))
    groups = [dict(id=g, n=MODS[g]['name'] if g else None, ids=ids) for g, ids in GROUPS]
    icons = {k: ICONS[k] for k in MODS if k in ICONS}
    J = json.dumps(dict(mods=data, groups=groups, icons=icons), ensure_ascii=False, separators=(',', ':'))
    css = '''.bk{display:grid;grid-template-columns:1fr 340px;gap:20px;align-items:start}
.pre{display:flex;flex-wrap:wrap;gap:8px;margin:0 0 18px}.pre button,.mb,.fh{font:inherit;color:#f0e6d2;cursor:pointer}
.pre button{background:#3b3329;border:3px solid #000;padding:6px 12px;box-shadow:inset -3px -3px 0 #28221a,inset 3px 3px 0 #54493a}
.fam{background:#3b3329;border:3px solid #000;padding:12px;margin:0 0 14px;box-shadow:inset -3px -3px 0 #28221a,inset 3px 3px 0 #54493a}
.fh{display:flex;align-items:center;gap:10px;background:none;border:0;padding:0;margin:0 0 10px;font-size:18px;color:#ffd75e;text-align:left}
.fh img{width:32px;height:32px;border:2px solid #000}.fh small{color:#8a7f69;font-size:12px}
.sub2{font-size:12px;color:#cbbf9f;margin:10px 0 6px}
.ml{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:6px}
.mb{display:flex;align-items:center;gap:8px;background:#26211c;border:2px solid #000;padding:5px 6px;font-size:13px;text-align:left;line-height:1.2}
.mb img{width:28px;height:28px;flex:none}.mb[aria-pressed=true]{background:#55a630;color:#fff}.mb.auto{background:#2f4a22;outline:2px dashed #7cd24f;outline-offset:-4px}
.sum{position:sticky;top:48px;background:#2a2520;border:3px solid #000;padding:16px;max-height:calc(100vh - 64px);overflow:auto}
.big{font-size:56px;line-height:1;color:#fff}.sum h3{font-size:15px;color:#ffd75e;margin:16px 0 6px}.sum ul{margin:0;padding:0 0 0 18px;font-size:13px}
.sum li{margin:2px 0}.why{color:#8a7f69}.sug{font:inherit;font-size:12px;background:none;border:1px solid #8a7f69;color:#cbbf9f;cursor:pointer;margin:2px 2px 0 0;padding:1px 6px}
.ins{background:#3b3329;border:3px solid #000;padding:16px;margin-top:20px}.ins ol{padding-left:22px}.ins code{background:#1d1a16;padding:1px 4px}
.cp{margin-top:8px}.bar{display:none}
.rel{color:#cbbf9f;font-size:14px}.los{display:flex;flex-wrap:wrap;gap:6px;align-items:center;margin:0 0 12px}.los span{color:#cbbf9f;font-size:14px}
.lob{font:inherit;font-size:14px;color:#f0e6d2;cursor:pointer;background:#26211c;border:2px solid #000;padding:5px 10px}.lob[aria-pressed=true]{background:#55a630;color:#fff}
.lob:disabled{cursor:not-allowed;color:#a89c84}.lob small{font-size:11px}.st2{padding-left:22px;font-size:15px}.st2 a,.dnote a{color:#ffd75e}.dnote{color:#cbbf9f;font-size:13px;margin:8px 0 0}
.btn:disabled{opacity:.55;cursor:wait}.dmsg{min-height:1.4em;color:#ffd75e;font-size:14px;margin:4px 0}
.fl{list-style:none;margin:0;padding:0;font-size:13px}.fl li{display:grid;grid-template-columns:minmax(0,1fr) 72px 60px;gap:2px 10px;align-items:baseline;padding:6px 0;border-top:1px solid #54493a}
.fl .fn{overflow-wrap:anywhere}.fl .fz{color:#cbbf9f;text-align:right}.fl a{color:#ffd75e}.fl .sh{grid-column:1/-1;font-size:11px;color:#a89c84;overflow-wrap:anywhere;background:none;padding:0}
@media(max-width:820px){.bk{grid-template-columns:1fr}.sum{position:static;max-height:none}.bar{display:block;position:sticky;bottom:0;background:#55a630;color:#fff;border:3px solid #000;padding:8px 12px;text-align:center;font-weight:bold;z-index:5}}'''
    body = (box('Modpack-Baukasten', 'Modpack builder',
                'Du stellst dir dein eigenes Paket zusammen: Mods oder ganze Familien anklicken, die Seite ergänzt sofort alles, was sie brauchen, und zeigt die Gesamtzahl. Wer sieht, dass „nur Hämmer“ wirklich nur zwei Mods bedeutet, traut sich zu installieren.',
                'You build your own pack: click mods or whole families, the page instantly adds everything they need and shows the total. Seeing that “just hammers” really means two mods makes installing feel safe.')
            + t('Stell dein Paket zusammen', 'Build your pack', 'h2')
            + '<div class="pre">' + ''.join(
                f'<button type="button" data-p="{k}" data-en="{E(en)}">{E(de)}</button>' for k, de, en in [
                    ('all', 'Alles', 'Everything'), ('build', 'Nur Bauen', 'Just building'),
                    ('adv', 'Abenteuer', 'Adventure'), ('comfort', 'Komfort + Leistung', 'Comfort + performance'),
                    ('none', 'Leeren', 'Clear')]) + '</div>'
            + '<div class="bk"><div id="fams"></div><aside class="sum" id="sum" aria-live="polite"></aside></div>'
            + '<div class="ins" id="ins"></div><div class="ins" id="dl"></div><div class="bar" id="bar"></div>'
            + ICG)
    js = 'var D=' + J + ''';
(function(){var M=D.mods,S=new Set(),L=function(){return document.documentElement.lang=="en"?"en":"de"};
var T={de:{req:"Pflicht-Teile",opt:"Optional",more:"Einzelne Mods",fam:"ganze Familie",n:"Mods",picked:"Gewählt",auto:"Automatisch dazu",ext:"Fremd-Mods",fit:"Passt gut dazu",for:"für",empty:"Noch nichts gewählt. Klick auf einen Mod oder eine Familie.",how:"So installierst du genau diese Auswahl",s1:"Fabric Loader für Minecraft 26.3 installieren.",s2:"Unten bei „Herunterladen“ die ZIP laden und die .jar-Dateien in den Ordner mods legen. Enthalten:",s3:"Fremd-Mods zusätzlich selbst laden (Links unten):",s4:"Profil „Fabric 26.3“ im Launcher starten.",cp:"Liste kopieren",ok:"Kopiert",share:"Link zu dieser Auswahl",none:"keine"},
en:{req:"Required parts",opt:"Optional",more:"Single mods",fam:"whole family",n:"mods",picked:"Picked",auto:"Added automatically",ext:"Other mods",fit:"Goes well with",for:"for",empty:"Nothing picked yet. Click a mod or a family.",how:"How to install exactly this selection",s1:"Install Fabric Loader for Minecraft 26.3.",s2:"Download the ZIP below under “Download” and put the .jar files into the mods folder. Included:",s3:"Get these other mods yourself (links below):",s4:"Start the “Fabric 26.3” profile in the launcher.",cp:"Copy list",ok:"Copied",share:"Link to this selection",none:"none"}};
var P={build:["simpleblocks","simpleoctets","simplehammers","simplelights","simpleend"],adv:["simplemobs","simpleanimals","simpledinos","simpleend","simplemobheads","simpleenddragonfight"],comfort:["simplequalityoflife","simpleoptimization"]};
function esc(s){return String(s).replace(/[&<>"]/g,function(c){return{"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c]})}
function closure(){var W={},st=[];S.forEach(function(i){W[i]=null;st.push(i)});while(st.length){var i=st.pop();M[i].r.forEach(function(r){if(!(r in W)){W[r]=i;st.push(r)}})}return W}
function img(i,c){return D.icons[i]?'<img class="px'+(c?' '+c:'')+'" src="'+D.icons[i]+'" alt="">':""}
function btn(i,W){var on=S.has(i),au=!on&&(i in W);return '<button type="button" class="mb'+(au?" auto":"")+'" aria-pressed="'+on+'" data-m="'+i+'" title="'+esc(M[i][L()])+'">'+img(i)+'<span>'+esc(M[i].n)+'</span></button>'}
function render(){var l=L(),t=T[l],W=closure(),h="";
D.groups.forEach(function(g){h+='<div class="fam">';if(g.id){h+='<button type="button" class="fh" data-f="'+g.id+'">'+img(g.id)+'<span>'+esc(g.n)+' <small>'+g.ids.length+' '+t.n+' · '+t.fam+'</small></span></button>';
var rq=g.ids.filter(function(i){return i==g.id||M[i].b=="required"}),op=g.ids.filter(function(i){return i!=g.id&&M[i].b!="required"});
h+='<div class="sub2">'+t.req+'</div><div class="ml">'+rq.map(function(i){return btn(i,W)}).join("")+'</div>';if(op.length)h+='<div class="sub2">'+t.opt+'</div><div class="ml">'+op.map(function(i){return btn(i,W)}).join("")+'</div>'}
else h+='<div class="fh">'+t.more+'</div><div class="ml">'+g.ids.map(function(i){return btn(i,W)}).join("")+'</div>';h+='</div>'});
document.getElementById("fams").innerHTML=h;
var ids=Object.keys(W),au=ids.filter(function(i){return !S.has(i)}),ext={},fit={};ids.forEach(function(i){M[i].x.forEach(function(x){ext[x]=1});M[i].o.forEach(function(o){if(!(o in W))fit[o]=1})});
var s='<div class="big sk">'+ids.length+'</div><div>'+t.n+'</div>';if(!ids.length)s+='<p>'+t.empty+'</p>';
else{s+='<h3>'+t.picked+' ('+S.size+')</h3><ul>'+Array.from(S).map(function(i){return '<li>'+esc(M[i].n)+'</li>'}).join("")+'</ul>';
if(au.length)s+='<h3>'+t.auto+' ('+au.length+')</h3><ul>'+au.map(function(i){return '<li>'+esc(M[i].n)+' <span class="why">'+t.for+' '+esc(M[W[i]].n)+'</span></li>'}).join("")+'</ul>';
s+='<h3>'+t.ext+'</h3><ul>'+(Object.keys(ext).length?Object.keys(ext).map(function(x){return '<li>'+esc(x)+'</li>'}).join(""):'<li>'+t.none+'</li>')+'</ul>';
var f=Object.keys(fit).slice(0,12);if(f.length)s+='<h3>'+t.fit+'</h3>'+f.map(function(i){return '<button type="button" class="sug" data-m="'+i+'">+ '+esc(M[i].n)+'</button>'}).join("")}
document.getElementById("sum").innerHTML=s;document.getElementById("bar").textContent=ids.length+" "+t.n;
var names=ids.map(function(i){return M[i].n}).sort();
document.getElementById("ins").innerHTML='<h2 style="margin-top:0">'+t.how+'</h2><ol><li>'+t.s1+'</li><li>'+t.s2+'<br><code>'+(names.length?esc(names.join(", ")):"-")+'</code></li><li>'+t.s3+' '+(Object.keys(ext).join(", ")||t.none)+'</li><li>'+t.s4+'</li></ol><button type="button" class="btn s cp" id="cp">'+t.cp+'</button> <a class="btn s" href="#sel='+Array.from(S).join(",")+'">'+t.share+'</a>';
document.getElementById("cp").onclick=function(){var b=this;try{navigator.clipboard.writeText(names.join("\\n")).then(function(){b.textContent=t.ok})}catch(e){}};
dlRender(W,M);
try{history.replaceState(null,"",S.size?"#sel="+Array.from(S).join(","):location.pathname+location.search)}catch(e){}}
document.addEventListener("click",function(e){var b=e.target.closest("[data-m],[data-f],[data-p]");if(!b)return;
if(b.dataset.m){var i=b.dataset.m;S.has(i)?S.delete(i):S.add(i)}
else if(b.dataset.f){var g=D.groups.filter(function(x){return x.id==b.dataset.f})[0],all=g.ids.every(function(i){return S.has(i)});g.ids.forEach(function(i){all?S.delete(i):S.add(i)})}
else{var p=b.dataset.p;S.clear();if(p=="all")Object.keys(M).forEach(function(i){S.add(i)});else if(P[p])P[p].forEach(function(i){S.add(i)})}render()});
var m=/sel=([a-z0-9_,]+)/.exec(location.hash);if(m)m[1].split(",").forEach(function(i){if(M[i])S.add(i)});
document.addEventListener("smlang",render);dlInit(render);render()})();'''
    js = (D / 'paket_dl.js').read_text(encoding='utf-8') + js
    return page('paket.html', css, body, js)


# ---------------------------------------------------------------- K2 Reise
STAGES = [
 dict(k='start', de='Start und Überleben', en='Start and survival', c='#55a630', bg='simplestorage:block/copper_barrel_side',
      tde='Die erste Nacht: ein Messer und ein Schneidebrett machen aus Brot ein Sandwich, Krabben und Trüffel füllen den Magen, der Keramikeimer ersetzt den teuren Eisen-Eimer.',
      ten='The first night: a knife and a cutting board turn bread into a sandwich, crabs and truffles fill your belly, the ceramic bucket replaces the costly iron one.',
      items=['simplesandwiches:item/knife', 'simplesandwiches:item/cheese_slice', 'simplesandwiches:item/butter_slice', 'simpleanimals:item/cooked_crab',
             'simpleanimals:item/truffle_soup', 'simpletools:item/ceramic_bucket', 'simplehammers:item/stone_sledgehammer', 'simplelib:item/modpack_guide'],
      mods=['simplesandwiches', 'simpleanimals', 'simplecrucibles', 'simplehammers', 'simplequalityoflife', 'simplerespawn']),
 dict(k='build', de='Bauen', en='Building', c='#c8893a', bg='simpleblocks:block/sculk_tiles',
      tde='Das erste Haus wird zur Burg: fehlende Baublöcke, halbe Würfel als Octets, flache Lichtscheiben und Hämmer, die Treppen und Stufen in der Welt formen.',
      ten='The first house becomes a castle: missing building blocks, half cubes as octets, flat light panes and hammers that shape stairs and slabs in the world.',
      items=['simpleblocks:block/sculk_tiles', 'simpleblocks:block/blue_nether_bricks', 'simpleblocks:block/nautilus_shell_block_side', 'simpleblocks:block/azure_froglight_side',
             'simplelights:block/lime_light_pane', 'simplelights:block/cyan_light_pane', 'simplehammers:item/iron_sledgehammer', 'simplehammers:item/diamond_sledgehammer'],
      mods=['simpleblocks', 'simpleoctets', 'simplelights', 'simplehammers', 'simpletrims']),
 dict(k='tech', de='Technik', en='Technology', c='#7a8ea3', bg='simplefoundry:block/foundry_brick',
      tde='Ab jetzt läuft es von allein: Tiegel kochen alle Plätze zugleich, Rohre verbinden Truhen, verstärkte Trichter und Kolben schieben mehr, die Gießerei schmilzt Erz in Formen.',
      ten='From here on it runs itself: crucibles cook every slot at once, pipes link chests, reinforced hoppers and pistons push more, the foundry melts ore into casts.',
      items=['simplecrucibles:block/iron_crucible_side', 'simpleovens:block/reinforced_furnace_front_on', 'simplemachines:item/guide_book', 'simplemachines:item/signal_dampener',
             'simplestorage:block/pipe', 'simplestorage:block/chest_interface', 'simplefoundry:block/foundry_desk_front', 'simplepads:block/launchpad'],
      mods=['simplecrucibles', 'simpleovens', 'simplemachines', 'simplestorage', 'simplefoundry', 'simplepads', 'simplegadgets']),
 dict(k='nether', de='Nether', en='Nether', c='#b5472a', bg='simpleenchanting:block/crimson_blazewood_planks',
      tde='Hitze und Gefahr: im Nether wartet das Roségold-Erz, der Seelenfeuer-Lohe und ein Netherherz. Netherit-Öfen, -Hämmer und -Rucksäcke sind der Lohn.',
      ten='Heat and danger: the Nether hides rose gold ore, the Soulfire Blaze and a Nether Heart. Netherite furnaces, hammers and backpacks are the reward.',
      items=['simplemobs:item/nether_heart', 'simplemobs:item/nether_gem', 'simplemobs:item/soulfire_blaze_spawn_egg', 'simpleenchanting:block/blazing_obsidian',
             'simpleores:block/nether_dimensional_scrap_side', 'simpleovens:block/netherite_furnace_front_on', 'simplehammers:item/netherite_sledgehammer', 'simplestorage:block/netherite_backpack_front'],
      named=['simpleores:block/sage_ore'],
      mods=['simpleores', 'simplemobs', 'simpleenchanting', 'simpleovens', 'simplestorage']),
 dict(k='end', de='End', en='The End', c='#7b5ea7', bg='simplebuilding:block/astral_end_stone',
      tde='Eine neue Welt aus Stein: Astralit und Nihilit liegen im Endstein, Asch- und Dämmerholz wachsen auf End-Gras, Enderit wird zum stärksten Metall.',
      ten='A new world of stone: Astralit and Nihilit lie in the end stone, Ashwood and Duskwood grow on End grass, Enderite becomes the strongest metal.',
      items=['simplebuilding:block/astralit_block', 'simplebuilding:block/nihilith_block', 'simplebuilding:item/astralit_dust', 'simplebuilding:item/nihilith_shard',
             'simpleend:block/ashwood_log', 'simpleend:block/end_grass', 'simpleores:item/enderite_ingot'],
      mods=['simpleend', 'simpleores', 'simplebuilding', 'simpleenddragonfight', 'simplemobs']),
 dict(k='endgame', de='Endgame', en='Endgame', c='#d4af37', bg='simpleenchanting:block/nihil_obsidian',
      tde='Ganz oben: der Enderit-Hammer, der Astral-Verzauberer auf Nihil-Obsidian, das Mächtige Leuchtfeuer und Bosse wie der Hollow Howler, der dich in der Deep Dark erwartet.',
      ten='At the top: the Enderite hammer, the Astral Enchanter on Nihil Obsidian, the Mighty Beacon and bosses like the Hollow Howler waiting in the Deep Dark.',
      items=['simplehammers:item/enderite_sledgehammer', 'simpleenchanting:block/astral_enchanter_side', 'simplebeacons:block/mighty_beacon', 'simplebeacons:item/mighty_conduit',
             'simplemobs:item/hollow_howler_spawn_egg', 'simplemobs:item/watcher_eye', 'simplepads:block/enderite_spawn_teleporter', 'simplestorage:block/enderite_backpack_front'],
      mods=['simplehammers', 'simpleenchanting', 'simplebeacons', 'simplemobs', 'simplepads', 'simplemobheads']),
]


for _s in STAGES:  # overrides.json "stages": {"<k>": {"de"/"en" (Titel), "tde"/"ten" (Text), "bg", "items", "mods"}}
    _s.update({k: v for k, v in ((OVR.get('stages') or {}).get(_s['k']) or {}).items() if not k.startswith('_')})


def k2():
    css = '''.jn{position:sticky;top:0;z-index:6;display:flex;gap:4px;background:#1d1a16;border-bottom:3px solid #000;padding:8px 0;overflow-x:auto;margin:0 -16px;padding-left:16px;padding-right:96px}
.jn a{flex:none;text-decoration:none;font-size:12px;padding:4px 8px;border:2px solid #000;background:#3b3329;color:#cbbf9f}.jn a.on{background:var(--c);color:#fff}
.st{position:relative;margin:56px 0;padding:28px 22px 24px;border:3px solid #000;background:#26211c;overflow:hidden;opacity:.35;transform:translateY(24px);transition:opacity .5s,transform .5s}
.st.in{opacity:1;transform:none}.st::before{content:"";position:absolute;inset:0;background-image:var(--bg);background-size:64px;image-rendering:pixelated;opacity:.18}
.st>*{position:relative}.st .no{font-size:13px;color:var(--c);letter-spacing:2px}.st h2{margin:4px 0 10px;color:#fff;border-left:8px solid var(--c);padding-left:14px}
.st .lead{max-width:760px;font-size:16px;color:#e6dcc4;margin:0 0 18px}
.it{display:grid;grid-template-columns:repeat(auto-fill,minmax(112px,1fr));gap:10px}
.it figure{margin:0;background:rgba(29,26,22,.85);border:2px solid #000;padding:10px 6px;text-align:center}
.it img{width:64px;height:64px;display:block;margin:0 auto 6px}.it figcaption{font-size:12px;line-height:1.3;color:#f0e6d2}
.it .nm{border-style:dashed}.it .nm i{display:block;width:64px;height:64px;margin:0 auto 6px;border:2px dashed var(--c);font-style:normal;line-height:60px;font-size:28px;color:var(--c)}
.mc{display:flex;flex-wrap:wrap;gap:6px;margin-top:16px}.mc a{display:flex;align-items:center;gap:6px;text-decoration:none;font-size:12px;background:#1d1a16;border:2px solid #000;padding:3px 8px 3px 3px}
.mc img{width:24px;height:24px}.path{height:12px;margin:-44px auto;width:12px;background:repeating-linear-gradient(#55a630 0 6px,transparent 6px 12px)}
@media(max-width:600px){.st{padding:20px 14px}.it{grid-template-columns:repeat(auto-fill,minmax(92px,1fr))}}
@media(prefers-reduced-motion:reduce){.st{opacity:1;transform:none;transition:none}}'''
    nav = '<nav class="jn">' + ''.join(
        f'<a href="#{s["k"]}" style="--c:{s["c"]}" data-en="{n+1}. {E(s["en"])}">{n+1}. {E(s["de"])}</a>' for n, s in enumerate(STAGES)) + '</nav>'
    body = box('Reise durchs Spiel', 'A journey through the game',
               'Statt einer Mod-Liste erzählt die Seite den Weg eines Spielers: von der ersten Nacht über Bauen und Technik bis Nether, End und Endgame. Zu jeder Etappe siehst du die echten Items mit ihren eigenen Texturen, so weiß jeder sofort, wo ihn die Mods im Spiel abholen.',
               'Instead of a mod list the page tells a player’s path: from the first night through building and technology to the Nether, the End and endgame. Each stage shows the real items with their own textures, so everyone sees at once where the mods meet them in the game.') + nav
    for n, s in enumerate(STAGES):
        figs = ''.join(f'<figure><img class="px" src="{tex(x)}" alt=""><figcaption data-en="{E(name(x, "en"))}">{E(name(x, "de"))}</figcaption></figure>' for x in s['items'])
        figs += ''.join(f'<figure class="nm"><i>?</i><figcaption data-en="{E(name(x, "en"))}">{E(name(x, "de"))}</figcaption></figure>' for x in s.get('named', []))
        chips = ''.join(f'<a href="https://wiki.simplemods.de/?mod={m}"><img class="px" src="{ICONS[m]}" alt="">{E(MODS[m]["name"])}</a>' for m in s['mods'] if m in ICONS)
        if n:
            body += '<div class="path" aria-hidden="true"></div>'
        body += (f'<section class="st" id="{s["k"]}" style="--c:{s["c"]};--bg:url({tex(s["bg"])})">'
                 f'<div class="no sk" data-en="Stage {n+1} of {len(STAGES)}">Etappe {n+1} von {len(STAGES)}</div>'
                 + t(s['de'], s['en'], 'h2') + t(s['tde'], s['ten'], 'p', 'lead')
                 + f'<div class="it">{figs}</div>' + t('Mods dieser Etappe', 'Mods of this stage', 'div', 'sub2', ' style="font-size:12px;color:#cbbf9f;margin-top:16px"')
                 + f'<div class="mc">{chips}</div></section>')
    body += ('<section class="st in" style="--c:#55a630;--bg:none;text-align:center">' + t('Deine Reise beginnt hier', 'Your journey starts here', 'h2', '', ' style="border:0;padding:0"')
             + t('Ein Installer, alle Etappen. Jedes Mod läuft auch allein.', 'One installer, every stage. Every mod also runs on its own.', 'p', 'lead', ' style="margin:0 auto 18px"')
             + '<a class="btn" href="/setup/" data-en="Get the installer">Installer laden</a></section>' + ICG)
    js = '''(function(){var S=document.querySelectorAll(".st[id]"),A=document.querySelectorAll(".jn a");
if(!("IntersectionObserver" in window)){S.forEach(function(s){s.classList.add("in")});return}
var o=new IntersectionObserver(function(es){es.forEach(function(e){if(e.isIntersecting){e.target.classList.add("in");
A.forEach(function(a){var on=a.getAttribute("href")=="#"+e.target.id;a.classList.toggle("on",on);if(on&&a.scrollIntoView)a.parentNode.scrollLeft=a.offsetLeft-40})}})},{rootMargin:"-35% 0px -45% 0px"});
S.forEach(function(s){o.observe(s)})})();'''
    return page('reise.html', css, body, js)


# ---------------------------------------------------------------- K3 Schaufenster
def audio():
    src = R / 'modules/simplemusic/shared/resources/assets/simplemusic/sounds/records/ember_forge.ogg'
    tmp = Path(os.environ.get('TMPDIR', '/tmp')) / 'land09-clip.mp3'
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', str(src), '-t', '20', '-af', 'afade=t=out:st=17:d=3',
                    '-ac', '1', '-b:a', '64k', str(tmp)], check=True)
    u = 'data:audio/mpeg;base64,' + base64.b64encode(tmp.read_bytes()).decode()
    tmp.unlink()
    return u


SHOW = [  # (mod, Titel de/en, Satz de/en, Hintergrund, Haupttexturen, wiki)
 ('simplemovement', 'Free Look', 'Free Look', 'Taste halten und frei umsehen, während du weiter geradeaus läufst oder fliegst.',
  'Hold a key and look around freely while you keep walking or flying straight on.', 'simplebuilding:block/astral_end_stone', [], 'simplemovement'),
 ('simplestorage', 'Simple Storage-Netzwerk', 'Simple Storage network', 'Rohre und Truhen-Schnittstellen verbinden Truhen und Fässer zu einem Lager, das sich selbst sortiert.',
  'Pipes and chest interfaces link chests and barrels into storage that sorts itself.', 'simplestorage:block/copper_barrel_side',
  ['simplestorage:block/pipe', 'simplestorage:block/chest_interface', 'simplestorage:block/enderite_pipe'], 'simplestorage'),
 ('simplemusic', 'Plattenpresse', 'Disc press', 'Presse eigene Schallplatten mit farbigem Etikett. Hör rein, so klingt „Ember Forge“:',
  'Press your own records with a tinted label. Have a listen, this is “Ember Forge”:', 'simplemusic:block/disc_press_side',
  ['simplemusic:item/custom_disc', 'simplemusic:item/blank_disc', 'simplemusic:block/disc_press_top'], 'simplemusic'),
 ('simpleblocks', 'Kompakte Blöcke', 'Compact blocks', 'Neun Blöcke werden zu einem kompakten, neun kompakte zu einem dichten Block, fürs Lager und für mutige Bauten.',
  'Nine blocks become one compact block, nine compact ones a dense block, for storage and bold builds.', 'simpleblocks:block/sculk_tiles',
  ['simpleblocks:block/nautilus_shell_block_side', 'simpleblocks:block/blue_nether_bricks'], 'simpleblocks'),
 ('simpledinos', 'Dinos', 'Dinos', 'Fossilien freipinseln, Eier ausbrüten und mit großem Sattel auf dem T-Rex reiten.',
  'Brush fossils free, hatch eggs and ride a T-Rex with a large saddle.', 'simpledinos:block/fossil_0',
  ['simpledinos:block/egg_trex', 'simpledinos:block/egg_triceratops', 'simpledinos:item/large_saddle'], 'simpledinos'),
 ('simplehammers', 'Vorschlaghämmer', 'Sledgehammers', 'Sieben Hämmer von Stein bis Enderit bauen 3x3 ab und formen Blöcke direkt in der Welt.',
  'Seven hammers from stone to enderite mine 3x3 and shape blocks right in the world.', 'simplefoundry:block/foundry_brick',
  ['simplehammers:item/stone_sledgehammer', 'simplehammers:item/diamond_sledgehammer', 'simplehammers:item/enderite_sledgehammer'], 'simplehammers'),
 ('simplecrucibles', 'Tiegel', 'Crucibles', 'Kochen ohne Brennstoff: der Block darunter heizt, alle Plätze garen zugleich.',
  'Cooking without fuel: the block below heats it, every slot cooks at once.', 'simplecrucibles:block/reinforced_cauldron_side',
  ['simplecrucibles:block/iron_crucible_side', 'simpletools:item/ceramic_lava_bucket', 'simpletools:item/enderite_soul_lava_bucket'], 'simplecrucibles'),
 ('simplemobs', 'Bosse', 'Bosses', 'Hollow Howler und Hollow Watcher fordern ganze Gruppen, ihre Herzen und Augen öffnen neue Rezepte.',
  'The Hollow Howler and Hollow Watcher challenge whole groups, their hearts and eyes unlock new recipes.', 'simpleenchanting:block/nihil_obsidian',
  ['simplemobs:item/hollow_howler_spawn_egg', 'simplemobs:item/watcher_eye', 'simplemobs:item/ender_heart'], 'simplemobs'),
]
NEWS = [  # aus git log (Datum, de, en)
 ('06.11.2026', 'Kompakte und dichte Blöcke für 19 Grundblöcke, Plattenpresse mit Farbstoff-Platz, gesprungene End-Ziegel.', 'Compact and dense blocks for 19 base blocks, disc press with a dye slot, cracked End bricks.'),
 ('05.11.2026', 'Free Look: Fadenkreuz am Zielpunkt. Roségold neu benannt, Silberblech und Blei-Pfeilspitze.', 'Free Look: crosshair at the aim point. Rose gold renamed, silver sheet and lead arrow tip.'),
 ('04.11.2026', 'Simple Pads wird eigenes Mod, ein Kreativ-Tab je Mod, End-Bücherregale.', 'Simple Pads becomes its own mod, one creative tab per mod, End bookshelves.'),
 ('03.11.2026', 'Simple Enchanting mit Dunklem und Astral-Verzauberer, Modpack-Handbuch in SimpleLib, Tastatur-Layouts.', 'Simple Enchanting with the Dark and Astral Enchanter, modpack guide in SimpleLib, keyboard layouts.'),
 ('02.11.2026', 'Gießerei mit Brennstofftank, T-Rex in neuer Größe, Rohre mit Kompass-Fenster.', 'Foundry fuel tank, T-Rex at its new size, pipes with a compass window.'),
 ('01.11.2026', 'Simple Machines startet: Doppelkolben und Signaldämpfer.', 'Simple Machines starts: double piston and signal dampener.'),
 ('2.0.0', 'Der große Modul-Split: SimpleBuilding wird Super-Mod, jedes Sub-Mod läuft allein.', 'The big module split: SimpleBuilding becomes a super mod, every sub mod runs on its own.'),
]


_SHOWKEYS = ('mod', 'tde', 'ten', 'pde', 'pen', 'bg', 'items', 'wiki')
for _i, _row in enumerate(SHOW):  # overrides.json "discover": {"<mod>": {"tde"/"ten" (Titel), "pde"/"pen" (Satz), "bg", "items"}}
    _o = (OVR.get('discover') or {}).get(_row[0]) or {}
    SHOW[_i] = tuple(_o.get(k, v) for k, v in zip(_SHOWKEYS, _row))


def k3():
    clip = audio()
    css = '''.nb{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:10px;margin:28px 0 0}
.nb div{background:#3b3329;border:3px solid #000;padding:14px 10px;text-align:center;box-shadow:inset -3px -3px 0 #28221a,inset 3px 3px 0 #54493a}
.nb b{display:block;font-family:"Silkscreen","Courier New",monospace;font-weight:400;font-size:clamp(30px,5vw,44px);line-height:1.1;color:#ffd75e}.nb span{font-size:13px;color:#cbbf9f}
.sw{display:grid;grid-template-columns:repeat(2,1fr);gap:16px}.sw .hx:first-child{grid-column:1/-1}
.hx{display:flex;flex-direction:column;border:3px solid #000;background:#2a2520;box-shadow:inset -3px -3px 0 #1a1612,inset 3px 3px 0 #54493a}
.pic{position:relative;height:200px;background-image:var(--bg);background-size:64px;image-rendering:pixelated;display:flex;align-items:center;justify-content:center;gap:14px;border-bottom:3px solid #000}
.pic::after{content:"";position:absolute;inset:0;background:linear-gradient(transparent 40%,rgba(0,0,0,.55))}
.pic img{position:relative;z-index:1;width:96px;height:96px;filter:drop-shadow(4px 4px 0 rgba(0,0,0,.6))}.pic img.ico{width:128px;height:128px;border:3px solid #000}
.hx:first-child .pic{height:260px}.hx .tx{padding:14px 16px 16px;flex:1;display:flex;flex-direction:column}
.hx h3{font-family:"Silkscreen","Courier New",monospace;font-weight:400;font-size:24px;margin:0 0 6px;color:#fff}.hx p{margin:0 0 12px;color:#d7ccb2;font-size:15px}
.hx .ft{margin-top:auto;display:flex;align-items:center;gap:8px;flex-wrap:wrap;font-size:13px}.hx .ft img{width:24px;height:24px}
.hx .ft a{margin-left:auto;color:#ffd75e}.play{font:inherit;font-weight:bold;cursor:pointer;background:#8e5bbd;color:#fff;border:3px solid #000;padding:8px 14px;margin:0 0 12px;align-self:flex-start;box-shadow:inset -3px -3px 0 #5e3b80,inset 3px 3px 0 #b48ad8}
.tl{list-style:none;margin:0;padding:0 0 0 22px;border-left:4px solid #55a630}.tl li{position:relative;margin:0 0 18px}
.tl li::before{content:"";position:absolute;left:-32px;top:4px;width:12px;height:12px;background:#55a630;border:3px solid #000}
.tl b{display:block;font-family:"Silkscreen","Courier New",monospace;font-weight:400;color:#ffd75e;font-size:15px}.tl span{color:#d7ccb2;font-size:15px}
@media(max-width:700px){.sw{grid-template-columns:1fr}.pic,.hx:first-child .pic{height:170px}.pic img{width:72px;height:72px}.pic img.ico{width:96px;height:96px}}'''
    nums = [(C['mods'], 'Mods', 'mods'), (C['sup'], 'Super-Mods', 'super mods'), (C['blocks'], 'Blöcke', 'blocks'),
            (C['items'], 'Items', 'items'), (C['tex'], 'eigene Texturen', 'own textures')]
    body = box('Schaufenster', 'Showcase',
               'Die Seite zeigt keine Liste, sondern die acht stärksten Momente der Mods als große Kacheln, jede mit eigener Textur, einem Satz und dem Weg ins Wiki. Echte Zahlen und eine Zeitleiste zeigen dazu, wie groß und lebendig das Projekt ist.',
               'The page shows no list but the eight strongest moments of the mods as big tiles, each with its own texture, one sentence and a way into the wiki. Real numbers and a timeline add how big and alive the project is.')
    body += '<div class="nb">' + ''.join(f'<div><b>{n}</b>{t(de, en)}</div>' for n, de, en in nums) + '</div>'
    body += t('Highlights', 'Highlights', 'h2') + '<div class="sw">'
    for mid, tde, ten, pde, pen, bg, texs, wiki in SHOW:
        imgs = ''.join(f'<img class="px" src="{tex(x)}" alt="">' for x in texs) or f'<img class="px ico" src="{ICONS[mid]}" alt="">'
        extra = ''
        if mid == 'simplemusic':
            extra = (f'<audio id="clip" preload="none" src="{clip}"></audio><button type="button" class="play" id="play" '
                     'data-en="&#9654; Play">&#9654; Abspielen</button>')
        body += (f'<article class="hx"><div class="pic" style="--bg:url({tex(bg)})">{imgs}</div><div class="tx">'
                 + t(tde, ten, 'h3') + t(pde, pen, 'p') + extra
                 + f'<div class="ft"><img class="px" src="{ICONS[mid]}" alt=""><span>{E(MODS[mid]["name"])}</span>'
                 f'<a href="https://wiki.simplemods.de/?mod={wiki}" data-en="Open in the wiki">Im Wiki ansehen</a></div></div></article>')
    body += '</div>' + t('Was ist neu', 'What’s new', 'h2') + '<ol class="tl">' + ''.join(
        f'<li><b>{E(d)}</b>{t(de, en)}</li>' for d, de, en in NEWS) + '</ol>'
    body += '<p style="text-align:center;margin:36px 0 0"><a class="btn" href="/setup/" data-en="Get the installer">Installer laden</a><a class="btn s" href="https://wiki.simplemods.de" data-en="Go to the wiki">Zum Wiki</a></p>' + ICG
    js = '''(function(){var a=document.getElementById("clip"),b=document.getElementById("play");if(!a||!b)return;
function lab(){var en=document.documentElement.lang=="en";b.textContent=a.paused?(en?"\\u25B6 Play":"\\u25B6 Abspielen"):(en?"\\u25A0 Stop":"\\u25A0 Stopp")}
b.onclick=function(){if(a.paused){a.currentTime=0;a.play()}else a.pause();lab()};a.onended=a.onpause=lab;document.addEventListener("smlang",lab)})();'''
    out = page('discover.html', css, body, js)
    # Button-Text wird vom Skript gepflegt, nicht vom Sprach-Schalter
    out = out.replace(' data-en="&#9654; Play">', '>', 1)
    OUT['discover.html'] = out
    return out


pages = {'default': OUT['index.html'], 'k1': k1(), 'k2': k2(), 'k3': k3()}
stale = [f for f, v in OUT.items() if not (D / f).exists() or (D / f).read_text(encoding='utf-8') != v]
if CHECK:
    if stale:  # Hinweis, kein Abbruch: die Landing ist kein Gate-Inhalt, der gen-Schritt jedes Buendels schreibt sie neu
        print('landing: veraltet (python3 tools/landing/concepts.py):', ', '.join(stale))
    else:
        print('landing: aktuell')
    sys.exit(0)
for f in stale:
    (D / f).write_text(OUT[f], encoding='utf-8')
print('Zahlen:', C, '| Groessen:', {k: len(v.encode()) for k, v in pages.items()})

# ---------------------------------------------------------------- Vorschau (LAND10)
if ARGS:
    P = Path(ARGS[0]); P.mkdir(parents=True, exist_ok=True)
    names = {'index.html': pages['default'], 'paket.html': pages['k1'], 'reise.html': pages['k2'], 'discover.html': pages['k3']}
    for f, v in names.items():
        for g in names:
            v = v.replace(f'href="{g}"', f'href="a-land10-{g}"')
        (P / f'a-land10-{f}').write_text(v, encoding='utf-8')
    tabs = [('index.html', 'Start (Default, unverändert + Navigation)'), ('paket.html', 'Paket zusammenstellen'),
            ('reise.html', 'Reise durchs Spiel'), ('discover.html', 'Discover (Schaufenster)')]
    pv = ('<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
          '<title>LAND10 Vorschau</title><style>:root{--bg:#f4f1ea;--fg:#1d1a16;--mu:#6b6355;--bd:#d6cfc0;--ac:#3a7a1f}'
          '@media(prefers-color-scheme:dark){:root:not([data-theme=light]){--bg:#1d1a16;--fg:#f0e6d2;--mu:#a89c84;--bd:#3b3329;--ac:#7cd24f}}'
          ':root[data-theme=dark]{--bg:#1d1a16;--fg:#f0e6d2;--mu:#a89c84;--bd:#3b3329;--ac:#7cd24f}'
          'body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}.w{max-width:1280px;margin:auto;padding:16px}'
          'h1{font-size:22px;margin:0 0 4px}.code{background:var(--bd);padding:8px 12px;font-family:monospace;margin:10px 0 18px;word-break:break-word}'
          'section{margin:0 0 28px}h2{font-size:18px;margin:0}p{margin:2px 0 8px;color:var(--mu)}'
          'iframe{width:100%;height:860px;border:2px solid var(--bd);background:#1d1a16}a{color:var(--ac)}</style></head><body><div class="w">'
          '<h1>LAND10: vier Seiten mit Navigation</h1><p>Start = Default (Inhalt unverändert), Paket zusammenstellen = K1, Reise = K2, Discover = K3. Navigationsleiste oben links, DE/EN oben rechts (wird seitenübergreifend gemerkt). Die Seiten sind unten eingebettet und einzeln öffenbar.</p>'
          '<div class="code">Feinschliff als Anmerkung</div>'
          + ''.join(f'<section><h2>{n}</h2><p><a href="a-land10-{f}" target="_blank">einzeln öffnen</a></p>'
                    f'<iframe src="a-land10-{f}" loading="lazy" title="{n}"></iframe></section>' for f, n in tabs)
          + '</div></body></html>')
    (P / 'a-land10-preview.html').write_text(pv, encoding='utf-8')
    print('Vorschau', P, 'gesamt', sum(f.stat().st_size for f in P.glob('a-land10-*')), 'Bytes')
