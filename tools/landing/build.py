#!/usr/bin/env python3
"""Baut die drei Landing-Page-Entwuerfe (A/B/C) als eigenstaendige HTML-Dateien (Bilder als data-URI).
Nur eigene Mod-Icons/-Texturen aus dem Repo. Aufruf: build.py [ausgabeordner]"""
import base64, glob, os, sys, html
R = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__)) + '/dist'
os.makedirs(OUT, exist_ok=True)

# (id, Name, Kurzbeschreibung, Akzentfarbe)
FEATURED = [
 ('simplebuilding','SimpleBuilding','Das Kernmod: praktisches Bauen, Graben und Werkzeuge, auf dem die Familie aufbaut.','#c8893a'),
 ('simplehammers','Simple Hammers','Sieben Vorschlaghaemmer von Stein bis Enderit. Jeder baut ab wie eine Spitzhacke, nur flaechig.','#7a8ea3'),
 ('simplecrucibles','Simple Crucibles','Tiegel kochen ohne Brennstoff alle Slots gleichzeitig, geheizt vom Block darunter.','#d8602f'),
 ('simpleovens','Simple Ovens','Drei verbesserte Stufen fuer Ofen, Raeucherofen und Schmelzofen.','#b5472a'),
 ('simpleores','Simple Ores','Silber, Rosegold und weitere Metalle fuer Werkzeuge, Ruestung und Bloecke.','#c9a227'),
 ('simplemobs','Simple Mobs','Neue Kreaturen und ein selbst beschwoerbarer Boss in der Deep Dark.','#3d8f6a'),
 ('simplestorage','Simple Storage','Kisten und Regale, die viel Platz auf wenig Raum bieten.','#8a6a43'),
 ('simplemoney','Simple Money','Muenzen, Geldboerse und Handel fuer eigene Wirtschaftssysteme.','#d4af37'),
 ('simpleblocks','Simple Blocks','Alle Baublocke an einem Ort: Steinmetz-Varianten, Dekoration, Muster.','#6c7a89'),
 ('simplemusic','Simple Music','Schallplatten, Instrumente und Musik fuer die Welt.','#8e5bbd'),
 ('simplemobheads','Simple Mobheads','97 Koepfe: jeder Mob ohne Vanilla-Kopf als Deko und Trophaee.','#c0504d'),
 ('simplechallenges','Simple Challenges','Waehle eine Herausforderung - ohne Wahl aendert das Mod nichts.','#2f7fb5'),
 ('simpleend','Simple End','Zwei End-Hoelzer mit vollem Holzset und neue Dinge fuers Ende.','#7b5ea7'),
 ('simplesandwiches','Simple Sandwiches','Ein Brot, bis zu fuenf Zutaten, optional Butter: eigene Sandwiches bauen.','#d9a441'),
 ('simplemaps','Simple Maps','Endlose Karten je Dimension.','#3b8d99'),
 ('simpleoptimization','Simple Optimization','Mehr Leistung durch Speicher-, Render- und Server-Optimierungen.','#4a9d4a'),
 ('simplequalityoflife','Simple Quality of Life','Viele kleine Komfortverbesserungen fuer den Alltag.','#3a86c8'),
 ('simpleaesthetics','Simple Aesthetics','Optik-Paket: Effekte, Klaenge und Stimmung.','#c25b9a'),
]
SHOW = ['simplehammers/item/diamond_sledgehammer.png','simplehammers/item/gold_sledgehammer.png',
        'simpleores/item/enderite_ingot.png','simplecrucibles/item/ceramic_lava_bucket.png',
        'simplehammers/item/copper_sledgehammer.png','simpleores/item/enderite_upgrade_template.png']

def uri(p):
    return 'data:image/png;base64,' + base64.b64encode(open(p, 'rb').read()).decode()
def icon(mid):
    g = glob.glob(f'{R}/modules/{mid}/shared/resources/assets/*/icon_128.png')
    if not g and mid == 'simplebuilding':
        g = glob.glob(f'{R}/**/assets/simplebuilding/icon_128.png', recursive=True)
    return uri(g[0]) if g else None
mods = [(i, n, d, c, icon(i)) for i, n, d, c in FEATURED]
mods = [m for m in mods if m[4]]
allicons = []
for p in sorted(glob.glob(f'{R}/modules/*/shared/resources/assets/*/icon_128.png')):
    mid = p.split('/modules/')[1].split('/')[0]
    if mid != 'wiringexample': allicons.append((mid, uri(p)))
show = [uri(f'{R}/wiki/assets/textures/{s}') for s in SHOW if os.path.exists(f'{R}/wiki/assets/textures/{s}')]
E_ = html.escape
HEAD = '<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Simple Mods</title><style>'
LINKS = dict(dl='/setup/', wiki='https://wiki.simplemods.de')
FEATS = [('Eine Familie, ein Stil','Alle Mods teilen Optik und Bedienung und lassen sich einzeln oder zusammen spielen.'),
         ('Jedes Modul allein spielbar','Nur installieren, was du willst. Fremdes Mod-Zeug erscheint nur, wenn es da ist.'),
         ('Eingebaute Anleitung','SimpleLib bringt ein Handbuch fuer alle installierten Simple-Mods mit.'),
         ('Server-Konfiguration','Alles per Config und Befehl einstellbar, sinnvolle Standardwerte.')]

def A():
    css = '''*{box-sizing:border-box}body{margin:0;background:#1d1a16;color:#f0e6d2;font:16px/1.5 "Courier New",monospace;image-rendering:pixelated}
a{color:inherit}.w{max-width:1100px;margin:auto;padding:0 16px}
.hero{padding:64px 0 40px;text-align:center;background:repeating-linear-gradient(90deg,#2a2520 0 32px,#26211c 32px 64px);border-bottom:8px solid #55a630}
h1{font-size:clamp(34px,8vw,72px);margin:0 0 8px;letter-spacing:2px;text-shadow:4px 4px 0 #000;color:#fff}
.sub{max-width:620px;margin:0 auto 24px;color:#cbbf9f}
.btn{display:inline-block;margin:6px;padding:12px 22px;background:#55a630;color:#fff;text-decoration:none;font-weight:bold;border:3px solid #000;box-shadow:inset -4px -4px 0 #3a7a1f,inset 4px 4px 0 #7cd24f}
.btn.s{background:#6b6b6b;box-shadow:inset -4px -4px 0 #4a4a4a,inset 4px 4px 0 #9a9a9a}
h2{font-size:28px;margin:48px 0 16px;text-shadow:3px 3px 0 #000}
.g{display:grid;grid-template-columns:repeat(auto-fill,minmax(250px,1fr));gap:12px}
.c{background:#3b3329;border:3px solid #000;padding:12px;box-shadow:inset -3px -3px 0 #28221a,inset 3px 3px 0 #54493a}
.c img{width:48px;height:48px;float:left;margin:0 12px 4px 0;background:#000}.c b{display:block;font-size:17px;color:#ffd75e}.c p{margin:4px 0 0;font-size:14px;color:#d7ccb2}
.f{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px}
.ic{display:flex;flex-wrap:wrap;gap:6px}.ic img{width:40px;height:40px;border:2px solid #000}
footer{margin:56px 0 24px;text-align:center;color:#8a7f69;font-size:13px}'''
    b = '<div class="hero"><div class="w"><h1>SIMPLE MODS</h1><p class="sub">Eine Familie von Minecraft-Mods fuer Bauen, Handwerk, Welt und Komfort. Einzeln spielbar, zusammen stark.</p><a class="btn" href="%s">Installer laden</a><a class="btn s" href="%s">Zum Wiki</a></div></div><div class="w"><h2>Die Mods</h2><div class="g">' % (LINKS['dl'], LINKS['wiki'])
    for i, n, d, c, ic in mods:
        b += f'<div class="c"><img src="{ic}" alt=""><b>{E_(n)}</b><p>{E_(d)}</p></div>'
    b += '</div><h2>Highlights</h2><div class="f">' + ''.join(f'<div class="c"><b>{E_(t)}</b><p>{E_(d)}</p></div>' for t, d in FEATS)
    b += f'</div><h2>Alle {len(allicons)} Module</h2><div class="ic">' + ''.join(f'<img src="{u}" title="{m}" alt="{m}">' for m, u in allicons)
    b += '</div><footer>Simple Mods - wiki.simplemods.de - Minecraft ist eine Marke von Mojang/Microsoft, dieses Projekt ist nicht offiziell.</footer></div>'
    return HEAD + css + '</style></head><body>' + b + '</body></html>'

def B():
    css = '''*{box-sizing:border-box}body{margin:0;font:17px/1.6 system-ui,sans-serif;background:#0b0d12;color:#e8ebf2}
a{color:inherit}.w{max-width:1200px;margin:auto;padding:0 20px}
nav{display:flex;justify-content:space-between;padding:20px 0;align-items:center}nav b{letter-spacing:.1em}nav a{margin-left:18px;text-decoration:none;opacity:.8}
.hero{min-height:78vh;display:grid;align-items:center;background:radial-gradient(1000px 500px at 75% 30%,#27406e55,transparent),radial-gradient(800px 500px at 10% 90%,#6b2f7a44,transparent)}
.hero .w{display:grid;grid-template-columns:1.2fr 1fr;gap:40px;align-items:center}
h1{font-size:clamp(40px,7vw,88px);line-height:1.02;margin:0 0 20px;font-weight:800;letter-spacing:-.03em;background:linear-gradient(90deg,#fff,#9db8ff);-webkit-background-clip:text;color:transparent}
.hero p{font-size:20px;color:#aab3c5;max-width:520px}
.btn{display:inline-block;margin:8px 10px 0 0;padding:14px 26px;border-radius:99px;background:#fff;color:#0b0d12;text-decoration:none;font-weight:600}.btn.s{background:transparent;color:#fff;border:1px solid #fff5}
.stage{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}.stage img{width:100%;aspect-ratio:1;object-fit:contain;image-rendering:pixelated;background:#ffffff0a;border-radius:20px;padding:14px;border:1px solid #ffffff14}
section{padding:70px 0}h2{font-size:clamp(28px,4vw,44px);margin:0 0 30px;letter-spacing:-.02em}
.row{display:grid;grid-template-columns:140px 1fr;gap:28px;align-items:center;padding:26px 0;border-top:1px solid #ffffff14}
.row img{width:140px;height:140px;border-radius:26px;image-rendering:pixelated;background:#ffffff0a;padding:10px}
.row h3{margin:0 0 6px;font-size:26px}.row p{margin:0;color:#aab3c5;max-width:640px}
.f{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:18px}.f div{padding:24px;border-radius:20px;background:#ffffff08;border:1px solid #ffffff14}.f b{display:block;margin-bottom:6px}.f span{color:#aab3c5;font-size:15px}
.cta{text-align:center;padding:90px 0;background:linear-gradient(#0b0d12,#14203a)}footer{text-align:center;color:#6c7488;font-size:13px;padding:24px}
@media(max-width:760px){.hero .w{grid-template-columns:1fr}.row{grid-template-columns:80px 1fr}.row img{width:80px;height:80px}}'''
    st = ''.join(f'<img src="{u}" alt="">' for u in show[:6])
    b = f'<div class="hero"><div class="w" style="display:block"><nav><b>SIMPLE MODS</b><span><a href="{LINKS["wiki"]}">Wiki</a><a href="{LINKS["dl"]}">Download</a></span></nav></div><div class="w"><div><h1>Minecraft, einfach besser.</h1><p>Eine Familie aufeinander abgestimmter Mods: bauen, schmieden, entdecken. Jedes Modul funktioniert allein.</p><a class="btn" href="{LINKS["dl"]}">Installer laden</a><a class="btn s" href="{LINKS["wiki"]}">Wiki ansehen</a></div><div class="stage">{st}</div></div></div>'
    b += '<section><div class="w"><h2>Die Mods</h2>' + ''.join(f'<div class="row"><img src="{ic}" alt=""><div><h3>{E_(n)}</h3><p>{E_(d)}</p></div></div>' for i, n, d, c, ic in mods) + '</div></section>'
    b += '<section><div class="w"><h2>Warum Simple Mods</h2><div class="f">' + ''.join(f'<div><b>{E_(t)}</b><span>{E_(d)}</span></div>' for t, d in FEATS) + '</div></div></section>'
    b += f'<div class="cta"><div class="w"><h2>Bereit?</h2><a class="btn" href="{LINKS["dl"]}">Installer laden</a><a class="btn s" href="{LINKS["wiki"]}">Dokumentation</a></div></div><footer>wiki.simplemods.de - nicht von Mojang/Microsoft</footer>'
    return HEAD + css + '</style></head><body>' + b + '</body></html>'

def C():
    css = '''*{box-sizing:border-box}body{margin:0;font:16px/1.5 "Trebuchet MS",system-ui,sans-serif;background:#fff4dc;color:#2b2140}
a{color:inherit}.w{max-width:1150px;margin:auto;padding:0 16px}
.hero{text-align:center;padding:56px 0 30px;position:relative;overflow:hidden}
h1{font-size:clamp(38px,8vw,80px);margin:0;transform:rotate(-2deg);color:#ff5d8f;text-shadow:4px 4px 0 #2b2140}
.sub{font-size:19px;max-width:560px;margin:14px auto 22px}
.btn{display:inline-block;margin:6px;padding:13px 24px;border-radius:16px;background:#ffd23f;border:3px solid #2b2140;box-shadow:4px 4px 0 #2b2140;text-decoration:none;font-weight:800;transition:transform .15s}.btn:hover{transform:translate(-2px,-2px) rotate(-1deg)}.btn.s{background:#7bdff2}
.float{position:absolute;width:64px;image-rendering:pixelated;opacity:.9;animation:fl 5s ease-in-out infinite}@keyframes fl{50%{transform:translateY(-14px) rotate(8deg)}}
h2{font-size:32px;margin:40px 0 18px;transform:rotate(-1deg)}
.g{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:18px}
.c{border-radius:22px;border:3px solid #2b2140;padding:16px;text-align:center;box-shadow:5px 5px 0 #2b2140;transition:transform .2s;color:#2b2140}
.c:nth-child(odd){transform:rotate(-1.2deg)}.c:nth-child(even){transform:rotate(1.2deg)}.c:hover{transform:rotate(0) scale(1.04)}
.c img{width:84px;height:84px;image-rendering:pixelated;background:#fff9;border-radius:18px;padding:6px;border:2px solid #2b2140}.c b{display:block;margin:8px 0 4px;font-size:18px}.c p{margin:0;font-size:14px}
.f{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:16px}.f div{background:#fff;border:3px dashed #2b2140;border-radius:20px;padding:16px}
.ic{display:flex;flex-wrap:wrap;gap:8px;justify-content:center}.ic img{width:44px;height:44px;border-radius:10px;border:2px solid #2b2140;transition:transform .2s}.ic img:hover{transform:scale(1.5) rotate(10deg)}
footer{text-align:center;margin:50px 0 20px;font-size:13px}'''
    pals = ['#ffb5c8', '#b8f2e6', '#ffe08a', '#c9b6ff', '#ffc9a3', '#a8e0ff']
    fl = ''.join(f'<img class="float" style="left:{p}%;top:{t}px;animation-delay:{d}s" src="{u}" alt="">' for (p, t, d), u in zip([(6, 40, 0), (86, 70, 1), (14, 250, 2), (80, 270, 3)], show))
    b = f'<div class="hero">{fl}<div class="w"><h1>Simple Mods!</h1><p class="sub">Eine bunte Familie von Minecraft-Mods. Sammle sie alle - oder nimm nur die, die du magst.</p><a class="btn" href="{LINKS["dl"]}">Installer laden</a><a class="btn s" href="{LINKS["wiki"]}">Zum Wiki</a></div></div><div class="w"><h2>Triff die Familie</h2><div class="g">'
    for k, (i, n, d, c, ic) in enumerate(mods):
        b += f'<div class="c" style="background:{pals[k % 6]}"><img src="{ic}" alt=""><b>{E_(n)}</b><p>{E_(d)}</p></div>'
    b += '</div><h2>Darum Simple</h2><div class="f">' + ''.join(f'<div><b>{E_(t)}</b><br>{E_(d)}</div>' for t, d in FEATS) + '</div>'
    b += f'<h2>Die ganze Galerie ({len(allicons)})</h2><div class="ic">' + ''.join(f'<img src="{u}" title="{m}" alt="{m}">' for m, u in allicons) + '</div><footer>wiki.simplemods.de - nicht von Mojang/Microsoft</footer></div>'
    return HEAD + css + '</style></head><body>' + b + '</body></html>'

for k, f in (('A', A), ('B', B), ('C', C)):
    open(f'{OUT}/{k}.html', 'w').write(f())
    print(k, os.path.getsize(f'{OUT}/{k}.html') // 1024, 'KB')
