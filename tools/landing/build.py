#!/usr/bin/env python3
"""Baut die Landing Page simplemods.de (Entwurf A, Besitzer-Wahl 06.11.) als eine eigenstaendige index.html.
Struktur (Super-Mod -> Sub-Mods, auch zweistufig) kommt aus modules/modules.json ueber tools/modtree/modtree.py;
die Gruppierung steht in CATS. Nur eigene Mod-Icons, keine Vanilla-Texturen. Aufruf: build.py [ausgabeordner]
Die ersten Entwuerfe A/B/C liegen als Archiv in archiv/."""
import base64, glob, os, sys, html
R = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
sys.path.insert(0, os.path.join(R, 'tools', 'modtree'))
import modtree
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__))
E_ = html.escape
LINKS = dict(dl='/setup/', wiki='https://wiki.simplemods.de')

# Gruppen der Mod-Galerie: (Titel, Akzentfarbe, Kurztext, [Wurzel-Mods]). Eine Familie steht komplett in einer Gruppe.
CATS = [
 ('Kern: Bauen, Technik und Welt', '#c8893a', 'SimpleBuilding und seine Module: Baublöcke, Maschinen, Erze, Kreaturen und Lager in einer Familie.', ['simplebuilding']),
 ('Welt und Dimensionen', '#3b8d99', 'Neue Orte, Karten und das Ende.', ['simpleearth', 'simpledimensions', 'simpleend', 'simplemaps']),
 ('Kreaturen und Tiere', '#3d8f6a', 'Neues Getier, Urzeitwesen und alles ums Reiten.', ['simpleanimals', 'simpledinos', 'simpleriding']),
 ('Abenteuer und Spielmodi', '#2f7fb5', 'Herausforderungen, Wiederbelebung, Statistik und Spielerei.', ['simplechallenges', 'simplerespawn', 'simplequestionary', 'simplefun', 'simplestats']),
 ('Handel und Wirtschaft', '#d4af37', 'Münzen, Spielerläden und fahrende Händler.', ['simplemoney', 'simpleshops', 'simplewanderers']),
 ('Handwerk und Essen', '#d9a441', 'Leuchtfeuer und Sandwiches.', ['simplebeacons', 'simplesandwiches']),
 ('Komfort und QoL', '#3a86c8', 'Viele kleine Verbesserungen für den Alltag.', ['simplequalityoflife', 'simpletweaks']),
 ('Optik', '#c25b9a', 'Effekte, Klänge, Modelle und Rüstungsbesätze.', ['simpleaesthetics', 'simpletrims']),
 ('Musik und Klang', '#8e5bbd', 'Noten, Schallplatten und Instrumente.', ['simplemusic']),
 ('Leistung und Server', '#4a9d4a', 'Schneller laufen und die Welt im Browser ansehen.', ['simpleoptimization', 'simplelivemap']),
]
# Fachchip der Sub-Mods von SimpleBuilding (Hierarchie bleibt, der Chip zeigt nur das Thema)
CHIP = {'simplearrows': 'Waffen', 'simpleblocks': 'Bauen', 'simpleenchanting': 'Verzauberung', 'simplegadgets': 'Werkzeug',
        'simplehammers': 'Werkzeug', 'simplelights': 'Bauen', 'simplemachines': 'Technik', 'simplemobheads': 'Kreaturen',
        'simplemobs': 'Kreaturen', 'simpleoctets': 'Bauen', 'simpleores': 'Erze', 'simpleovens': 'Technik',
        'simplecrucibles': 'Technik', 'simplefoundry': 'Technik', 'simplepads': 'Technik', 'simpleresonance': 'Welt',
        'simplestorage': 'Lager'}
DE = {
 'simplebuilding': 'Das Kernmod: praktisches Bauen, Graben und Werkzeuge, auf dem die Familie aufbaut.',
 'simplearrows': 'Der Pfeilmacher baut vier Pfeilsorten, die Vanilla nicht kennt.',
 'simpleblocks': 'Alle Baublöcke an einem Ort: Tiefenschiefer-Fliesen, Dekoration und Muster.',
 'simpleenchanting': 'Zusätzliche Verzauberer über dem Vanilla-Tisch, darunter der Dunkle Verzauberer.',
 'simplegadgets': 'Dinge für die Hand, etwa das Messgerät mit Tempo, Höhe und Fallschaden-Warnung.',
 'simplehammers': 'Sieben Vorschlaghämmer von Stein bis Enderit: bauen 3x3 ab und formen Blöcke um.',
 'simplelights': 'Leuchtpaneele: flache Lampen an jeder Blockseite in sechzehn Farben.',
 'simplemachines': 'Maschinenteile wie der Doppelkolben, in einem eigenen Kreativ-Tab.',
 'simplemobheads': 'Ein Kopf für jeden Mob als Deko und Trophäe, nach Creeper-Explosionen.',
 'simplemobs': 'Neue Kreaturen und Bosse: Hohler Heuler, Hohler Wächter, Panzerling und mehr.',
 'simpleoctets': 'Halbblock-Würfel aus jedem Holz und Block, samt Treppen und Stufen.',
 'simpleores': 'Silber und Roségold für Werkzeuge, Rüstung und Blöcke, in Oberwelt und Ende.',
 'simpleovens': 'Verstärkte, Netherit- und Enderit-Öfen: doppelt bis achtfach so schnell.',
 'simplecrucibles': 'Tiegel kochen ohne Brennstoff alle Slots gleichzeitig, geheizt vom Block darunter.',
 'simplefoundry': 'Eine Mehrblock-Gießerei mit Schmelzkammern in einer Ziegelhülle.',
 'simplepads': 'Plattformen und Druckplatten: Startrampe, Elytra-Pad, Flypad und mehr.',
 'simpleresonance': 'Spielerwert und Gefahrenzonen: wie erfahren ein Spieler ist und wo Mobs spawnen.',
 'simplestorage': 'Kisten, Regale und Vorratskisten, die viel Platz auf wenig Raum bieten.',
 'simpleanimals': 'Hirsch, Wildschwein, Igel, Krabbe und Zebra mit eigenem Verhalten.',
 'simplebeacons': 'Starkes Leuchtfeuer mit sechs Pyramidenstufen und zehn weiteren Effekten.',
 'simplechallenges': 'Wähle eine Herausforderung; ohne Wahl ändert das Mod nichts.',
 'simpledimensions': 'Einstellbare Dimensionen, Lichtbögen und sichere verknüpfte Portale.',
 'simpledinos': 'Fossilien, Dinosaurier-Eier zum Freipinseln und Ausbrüten, und der erste Dino.',
 'simpleearth': 'Die Oberwelt aus der Umlaufbahn: ab großer Höhe wird die Welt zur Scheibe.',
 'simpleend': 'Zwei End-Hölzer mit vollem Holzset, End-Gras, Aschetal und neue Dinge fürs Ende.',
 'simpleenddragonfight': 'Der Drachenkampf wird härter: mehr Lebensbalken je Spieler, schnellerer Drache.',
 'simplefun': 'Spielerische Mechaniken, Wurfziegel und kosmetische Verwandlungen.',
 'simplemaps': 'Endlose Karten je Dimension mit Zoom und Wegpunkten.',
 'simplemoney': 'Münzen, Geldbörse, gewichtete Dorfbewohner-Handel und Schatzbeute.',
 'simplemusic': 'Noten per Textcode, Schallplatten, Instrumente und Musik für die Welt.',
 'simplequestionary': 'Der Fragetisch: Gegenstand hineinlegen und Wissen, Nutzen und ein Geheimnis lesen.',
 'simplerespawn': 'Niedergestreckt statt tot: ein Mitspieler kann dich innerhalb der Blutzeit wiederbeleben.',
 'simpleriding': 'Sattel-Tempo, Pferdesprünge, Nautilus-Sprints, Reittier-Rüstung und Handel.',
 'simplesandwiches': 'Ein Brot, bis zu fünf Zutaten, optional Butter: eigene Sandwiches auf dem Brett.',
 'simpleshops': 'Spielerläden: Verkaufsblock mit Preis und Vitrine zum Ausstellen.',
 'simpletrims': 'Leuchtende und pulsierende Rüstungsbesätze, platzierbare Vorlagen und Upgrades.',
 'simpletweaks': 'Kompatibilität mit älteren Welten und serverseitige Grundstücke, standardmäßig an.',
 'simplewanderers': 'Ein fahrender Händler je Dorf mit angeleintem Pferd.',
 'simplequalityoflife': 'Das Komfort-Paket: bündelt Wetter, Oberflächen, Beute, Bewegung und mehr.',
 'simplefarming': 'Federfall schützt Ackerland, die Hacke erntet und pflanzt neu, und mehr.',
 'simpleinterfaces': 'Neu gestaltete Behälter-Oberflächen mit farbigem Rahmen je Block.',
 'simpleloot': 'Tresor-Abklingzeit in Tagen statt einmal pro Spieler.',
 'simplemobmanners': 'Mobs stummschalten, benannte Babys jung halten, Piglins ignorieren Gold.',
 'simplemovement': 'Manuelles Kriechen, Autolauf, schnellere Leitern und Frostläufer auf Pulverschnee.',
 'simpletools': 'Volle-Haltbarkeits-Bonus, Verzauberungen Sparsam und Seelengebunden, Todestresor.',
 'simpleweather': 'Wetter-Schalter für den Server und einstellbare Regendichte.',
 'simpleaesthetics': 'Das Optik-Paket: bündelt Effekte, Klänge und Modelle.',
 'simplemodels': 'Vom Server freigegebene Itemmodelle mit durchsuchbarem Modell-Browser.',
 'simplesounds': 'Passende Klänge zu den zwölf Stimmungseffekten von Simple Visuals.',
 'simplevisuals': 'Anzeige, Tooltips und Partikel für mehr Stimmung.',
 'simpleoptimization': 'Das Leistungs-Paket: bündelt Speicher-, Render- und Server-Optimierungen.',
 'simpleoptmemory': 'Weniger Speicherverbrauch bei Blockzuständen und Modellen.',
 'simpleoptrender': 'Schnelleres Rendern: Laub-Culling, Licht-Cache, schneller Server-Ping.',
 'simpleoptserver': 'Sichtweite folgt der Serverlast, innerhalb der Grenzen der server.properties.',
 'simplelivemap': 'Eine Live-Karte der Welt im Browser, so wie die Spieler sie erkundet haben.',
 'simplestats': 'Jedes Werkzeug, jede Waffe und Rüstung führt eigene Statistiken.',
}
FEATS = [('Eine Familie, ein Stil', 'Alle Mods teilen Optik und Bedienung und lassen sich einzeln oder zusammen spielen.'),
         ('Jedes Modul allein spielbar', 'Nur installieren, was du willst. Fremdes Mod-Zeug erscheint nur, wenn es da ist.'),
         ('Eingebaute Anleitung', 'SimpleLib bringt ein Handbuch für alle installierten Simple-Mods mit.'),
         ('Server-Konfiguration', 'Alles per Config und Befehl einstellbar, sinnvolle Standardwerte.')]

def uri(p):
    return 'data:image/png;base64,' + base64.b64encode(open(p, 'rb').read()).decode()
def icon(mid):
    g = glob.glob(f'{R}/modules/{mid}/shared/resources/assets/*/icon_128.png')
    if not g and mid == 'simplebuilding':
        g = glob.glob(f'{R}/**/assets/simplebuilding/icon_128.png', recursive=True)
    return uri(g[0]) if g else ''

mods = modtree.load()
for k in mods: mods[k]['de'] = DE.get(k)

def card(m, sub, chip=None):
    d = m['de'] or m['desc']
    tag = f'<i class="chip">{E_(chip)}</i>' if chip else ''
    img = f'<img src="{icon(m["id"])}" alt="">' if icon(m['id']) else ''
    soon = '<i class="chip soon">in Arbeit</i>' if m['status'] == 'planned' else ''
    return f'<div class="c{" sub" if sub else ""}">{img}<b>{E_(m["name"])}</b>{tag}{soon}<p>{E_(d)}</p></div>'

def family(m, depth=0, chips=False):
    kids = modtree.children(mods, m['id'])
    out = card(m, depth > 0, CHIP.get(m['id']) if chips and depth else None)
    if kids:
        out += '<div class="kids">' + ''.join(family(k, depth + 1, chips) for k in kids) + '</div>'
    return f'<div class="fam">{out}</div>' if depth == 0 else out

CSS = '''*{box-sizing:border-box}html{scroll-behavior:smooth}body{margin:0;background:#1d1a16;color:#f0e6d2;font:16px/1.5 "Courier New",monospace;image-rendering:pixelated}
a{color:inherit}.w{max-width:1100px;margin:auto;padding:0 16px}
.hero{padding:64px 0 40px;text-align:center;background:repeating-linear-gradient(90deg,#2a2520 0 32px,#26211c 32px 64px);border-bottom:8px solid #55a630}
h1{font-size:clamp(34px,8vw,72px);margin:0 0 8px;letter-spacing:2px;text-shadow:4px 4px 0 #000;color:#fff}
.sub1{max-width:620px;margin:0 auto 24px;color:#cbbf9f}
.btn{display:inline-block;margin:6px;padding:12px 22px;background:#55a630;color:#fff;text-decoration:none;font-weight:bold;border:3px solid #000;box-shadow:inset -4px -4px 0 #3a7a1f,inset 4px 4px 0 #7cd24f}
.btn.s{background:#6b6b6b;box-shadow:inset -4px -4px 0 #4a4a4a,inset 4px 4px 0 #9a9a9a}
h2{font-size:28px;margin:48px 0 16px;text-shadow:3px 3px 0 #000}
.nav{display:flex;flex-wrap:wrap;gap:8px;margin:0 0 8px}.nav a{padding:6px 12px;background:#3b3329;border:3px solid #000;text-decoration:none;font-size:14px;box-shadow:inset -3px -3px 0 #28221a,inset 3px 3px 0 #54493a}
.cat{margin:40px 0 0;scroll-margin-top:12px}.cat h3{margin:0;font-size:22px;padding:6px 12px;background:#2a2520;border-left:10px solid var(--a);text-shadow:2px 2px 0 #000}
.cat>p{margin:6px 0 14px;color:#cbbf9f;font-size:14px}
.g{display:grid;grid-template-columns:repeat(auto-fill,minmax(270px,1fr));gap:14px;align-items:start}
.fam{border:3px solid #000;border-top:6px solid var(--a);background:#2a2520;padding:8px}
.fam>.c{box-shadow:none}
.kids{margin:8px 0 0 14px;padding-left:12px;border-left:4px solid var(--a);display:grid;gap:8px}
.c{background:#3b3329;border:3px solid #000;padding:12px;box-shadow:inset -3px -3px 0 #28221a,inset 3px 3px 0 #54493a;overflow:hidden}
.c.sub{background:#342d24;padding:8px 10px}.c.sub img{width:32px;height:32px}
.c img{width:48px;height:48px;float:left;margin:0 12px 4px 0;background:#000}.c b{color:#ffd75e;font-size:17px}.c p{margin:4px 0 0;font-size:14px;color:#d7ccb2;clear:none}
.c.sub p{font-size:13px}
.chip{font-style:normal;font-size:11px;margin-left:8px;padding:1px 6px;background:var(--a);color:#fff;border:2px solid #000;vertical-align:2px}.chip.soon{background:#6b6b6b}
.f{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px}
footer{margin:56px 0 24px;text-align:center;color:#8a7f69;font-size:13px}
@media(max-width:520px){.g{grid-template-columns:1fr}.hero{padding:40px 0 28px}.kids{margin-left:6px;padding-left:8px}}'''

def page():
    seen, body = set(), ''
    nav = ''.join(f'<a href="#k{i}">{E_(t)}</a>' for i, (t, *_ ) in enumerate(CATS))
    for i, (t, col, txt, roots) in enumerate(CATS):
        body += f'<section class="cat" id="k{i}" style="--a:{col}"><h3>{E_(t)}</h3><p>{E_(txt)}</p><div class="g">'
        for r in roots:
            body += family(mods[r], 0, chips=(r == 'simplebuilding'))
            stack = [r]
            while stack:
                x = stack.pop(); seen.add(x); stack += [c['id'] for c in modtree.children(mods, x)]
        body += '</div></section>'
    miss = [k for k in mods if k not in seen and k not in ('simplelib', 'wiringexample')]
    assert not miss, f'Mods ohne Gruppe: {miss}'
    n = len(seen)
    h = f'<div class="hero"><div class="w"><h1>SIMPLE MODS</h1><p class="sub1">Eine Familie von Minecraft-Mods für Bauen, Handwerk, Welt und Komfort. Einzeln spielbar, zusammen stark.</p><a class="btn" href="{LINKS["dl"]}">Installer laden</a><a class="btn s" href="{LINKS["wiki"]}">Zum Wiki</a></div></div>'
    h += f'<div class="w"><h2>Die Mods ({n})</h2><nav class="nav">{nav}</nav>{body}'
    h += '<h2>Highlights</h2><div class="f">' + ''.join(f'<div class="c"><b>{E_(a)}</b><p>{E_(b)}</p></div>' for a, b in FEATS) + '</div>'
    h += '<footer>Simple Mods - wiki.simplemods.de - Minecraft ist eine Marke von Mojang/Microsoft, dieses Projekt ist nicht offiziell.</footer></div>'
    return f'<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Simple Mods</title><style>{CSS}</style></head><body>{h}</body></html>'

open(f'{OUT}/index.html', 'w', encoding='utf-8').write(page())
print('index.html', os.path.getsize(f'{OUT}/index.html') // 1024, 'KB')
