#!/usr/bin/env python3
"""Erzeugt vorschlag-1..3.html aus index.html (Default bleibt unveraendert). IG31-54 / LAND08."""
import re, sys, pathlib
D = pathlib.Path(__file__).parent
src = (D / "index.html").read_text(encoding="utf-8")
icons = dict(re.findall(r'<img src="(data:image/png;base64,[^"]+)" title="([a-z]+)"', src)[::1])
icons = {v: k for k, v in icons.items()}

def t(de, en, tag="span", cls=""):
    c = f' class="{cls}"' if cls else ""
    return f'<{tag}{c} data-en="{en}">{de}</{tag}>'

NEW = [  # (DE-Titel, EN-Titel, DE-Text, EN-Text, Icon)
 ("Nether-Roségold-Erz", "Nether Rose Gold Ore", "Neues Erz im Nether, höchstens eines je Golderz-Ader.", "New Nether ore, at most one per gold vein.", "simpleores"),
 ("Sage-Kristalle", "Sage crystals", "Eine ganze Kristallfamilie samt Erzen in Nether und End.", "A whole crystal family with ores in the Nether and the End.", "simpleores"),
 ("Presslinge", "Pressling blocks", "Gepresste Blöcke, dazu Schrott-Blöcke und Rosenkarotten.", "Pressed blocks, plus scrap blocks and rose carrots.", "simpleblocks"),
 ("End-Ziegel", "End bricks", "Gesprungene Astralit-, Nihilit- und Ender-Quarz-Ziegel.", "Cracked Astralit, Nihilit and Ender Quartz bricks.", "simpleend"),
 ("Plattenpresse", "Disc press", "Neuer Farbstoff-Platz und Schallplatten-Etiketten in Farbe.", "New dye slot and tinted record labels.", "simplemusic"),
 ("Simple Arrows", "Simple Arrows", "Neues Pfeil-Mod mit Silber- und Lichtpfeil.", "New arrow mod with silver and light arrows.", "simplearrows"),
]
FAQ = [
 ("Welche Minecraft-Version?", "Which Minecraft version?", "Aktuell Minecraft 26.3 mit Fabric. Andere Lader folgen später.", "Currently Minecraft 26.3 with Fabric. Other loaders follow later."),
 ("Brauche ich alle Mods?", "Do I need all mods?", "Nein. Jedes Modul ist allein spielbar. Inhalte anderer Mods erscheinen nur, wenn sie installiert sind.", "No. Every module plays on its own. Content from other mods only appears if they are installed."),
 ("Super- und Sub-Mods?", "Super and sub mods?", "Ein Super-Mod bündelt seine Sub-Mods. Die Sub-Mods bleiben einzeln installierbar und sind in der Galerie darunter gruppiert.", "A super mod bundles its sub mods. Sub mods can still be installed on their own and are grouped in the gallery below."),
]
STEPS = [
 ("Installer laden", "Get the installer", "Auf /setup/ den Installer für dein System holen.", "Grab the installer for your system at /setup/."),
 ("Mods wählen", "Pick your mods", "Alles oder nur das, was du magst. Abhängigkeiten holt der Installer mit.", "Everything or only what you like. The installer pulls in dependencies."),
 ("Spielen", "Play", "Fabric-Profil 26.3 im Launcher starten, fertig.", "Start the Fabric 26.3 profile in the launcher, done."),
]
LINKS = [("Wiki", "Wiki", "https://wiki.simplemods.de"), ("Downloads", "Downloads", "/setup/"), ("Änderungen", "Changelog", "https://wiki.simplemods.de")]

def card(de_t, en_t, de_p, en_p, icon=None):
    im = f'<img src="{icons[icon]}" alt="">' if icon else ""
    return f'<div class="c">{im}{t(de_t,en_t,"b")}{t(de_p,en_p,"p")}</div>'

def sec_new(n=6):
    return '<h2 data-en="New in this version">Neu in dieser Version</h2><div class="g">' + "".join(card(*x) for x in NEW[:n]) + "</div>"
def sec_faq():
    return '<h2 data-en="FAQ">Kurz-FAQ</h2><div class="f">' + "".join(card(a,b,c,d) for a,b,c,d in FAQ) + "</div>"
def sec_steps():
    return '<h2 data-en="Install in 3 steps">Installation in 3 Schritten</h2><div class="f">' + "".join(
        f'<div class="c"><b class="n">{i+1}</b>{t(a,b,"b")}{t(c,d,"p")}</div>' for i,(a,b,c,d) in enumerate(STEPS)) + "</div>"
def sec_gal(names):
    return '<h2 data-en="Mod gallery">Mod-Galerie</h2><div class="gal">' + "".join(
        f'<figure><img src="{icons[n]}" alt="{n}"><figcaption>{n}</figcaption></figure>' for n in names if n in icons) + "</div>"
def sec_links():
    return '<nav class="lk">' + "".join(f'<a class="btn s" href="{h}" data-en="{e}">{d}</a>' for d,e,h in LINKS) + "</nav>"

BASE_CSS = """h2{text-shadow:none}.c b.n{display:inline-block;background:#55a630;color:#fff;padding:0 10px;margin-bottom:6px}
.gal{display:grid;grid-template-columns:repeat(auto-fill,minmax(92px,1fr));gap:10px}.gal figure{margin:0;text-align:center}.gal img{width:64px;height:64px;border:3px solid #000;background:#000}.gal figcaption{font-size:11px;color:#cbbf9f;word-break:break-all}
.lk{text-align:center;margin:40px 0 0}@media(max-width:600px){.hero{padding:48px 0 28px}.btn{display:block}}"""

V = {
 1: dict(font='Archivo+Black', fam='"Archivo Black","Arial Black",Impact,sans-serif', css='h1{font-family:%s;font-weight:400;letter-spacing:1px;text-shadow:none}', secs=lambda: sec_new(6)+sec_links()+"@@MODS@@"+sec_faq()),
 2: dict(font='Silkscreen:wght@400', fam='"Silkscreen","VT323","Courier New",monospace', css='h1{font-family:%s;font-weight:400;letter-spacing:3px;text-shadow:2px 2px 0 #26211c;font-size:clamp(30px,7vw,60px)}', secs=lambda: sec_steps()+"@@MODS@@"+sec_gal(["simpleores","simpleend","simplehammers","simplecrucibles","simplemusic","simplearrows","simpleblocks","simplesandwiches","simplechess" ,"simplestorage","simpleoctets","simpletrims"])+sec_links()),
 3: dict(font='Roboto+Slab:wght@700', fam='"Roboto Slab",Rockwell,Georgia,serif', css='h1{font-family:%s;font-weight:700;letter-spacing:0;text-shadow:none}', secs=lambda: "@@MODS@@"+sec_new(4)+sec_steps()+sec_faq()+sec_links()),
}
for n, v in V.items():
    h = src
    fonts = f'<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin><link href="https://fonts.googleapis.com/css2?family={v["font"]}&display=swap" rel="stylesheet">'
    css = BASE_CSS + v["css"] % v["fam"]
    h = h.replace("</style></head>", css + "</style>" + fonts + "</head>", 1)
    # Inhalt: Abschnitte vor "Alle ... Module" bzw. Highlights einfuegen
    m = re.search(r'<h2 data-en="All \d+ modules">', h)
    assert m
    extra = v["secs"]()
    if "@@MODS@@" in extra:
        a, b = extra.split("@@MODS@@")
        h = h[:m.start()] + b + h[m.start():]
        h = h.replace('<h2>Highlights</h2>', a + '<h2>Highlights</h2>', 1) if a else h
    else:
        h = h[:m.start()] + extra + h[m.start():]
    (D / f"vorschlag-{n}.html").write_text(h, encoding="utf-8")
    print(n, len(h))
