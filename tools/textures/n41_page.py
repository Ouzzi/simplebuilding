"""HTML review page for the Nachtrag 41 texture proposals (all images as data URIs)."""
import base64
import html
import io

from PIL import Image

NAMES = {
    'astral_enchanting_table': 'Astral-Verzauberungstisch (astral_enchanting_table)',
    'warped_blazewood_bookshelf': 'Wirr-Lohenholz-Regal (warped_blazewood_bookshelf)',
    'crimson_blazewood_bookshelf': 'Karmesin-Lohenholz-Regal (crimson_blazewood_bookshelf)',
    'warped_blazewood_planks': 'Wirr-Lohenholzbretter (warped_blazewood_planks)',
    'ring_block': 'Weißer Ring-Block unten links (Id im Repo nicht gefunden)',
    'blazing_obsidian': 'Lohen-Obsidian (blazing_obsidian)',
    'crimson_blazewood_planks': 'Karmesin-Lohenholzbretter (crimson_blazewood_planks)',
    'paint_box': 'Farbkasten (paint_box)',
    'backpack': 'Rucksack Leder (backpack)',
    'backpack (Render 3D-Modell)': 'Rucksack als Render des 3D-Modells',
    'resin_fiber': 'Harzfaser (simplemoney:resin_fiber)',
    'special_fiber': 'Spezialfaser (simplemoney:special_fiber)',
    'ender_quartz': 'Enderquarz (ender_quartz)',
    'blaze_book': 'Lohenbuch (blaze_book)',
    'horseshoes': 'Hufeisen: Kupfer, Eisen, Gold, Diamant, Netherit, Enderit',
}

REFS = {
    'A': [
        ('Astral-Tisch', 'Vanilla enchanting_table (Ecksteine, Tuch über der Kante, Obsidian-Körper) und crying_obsidian '
         '(Adern statt Einzelpunkte). Übernommen: Aufbau Deckel/Seite/Boden; neu: Tuch, Goldsaum, Stern, weiche Violett-Adern.'),
        ('Regale', 'Vanilla bookshelf (Rahmen oben/Mitte/unten, zwei Buchreihen) und Bookshelf-Mods mit Varianten je Holz. '
         'Übernommen: Raster; neu: Rückwand dunkles Holz statt Schwarz (weniger stechend), Lohen-Buchrücken, Goldbänder.'),
        ('Bretter', 'Vanilla crimson/warped_planks (4 Bretter, versetzte Stöße), Faithful-Glossar: lineares Dithering '
         'für Maserung. Übernommen: Brett-Raster; neu: Glut folgt der Maserung bzw. den Fugen statt Zufallsflecken.'),
        ('Lohen-Obsidian', 'Vanilla crying_obsidian und magma (Adern/Zellen mit hellem Kern). Übernommen: '
         'Kern hell → Rand dunkelrot (Kruste); neu: kachelbare, fließende Adern ohne Pixelrauschen.'),
        ('Ring-Block', 'Vanilla bone_block_top (Ringe) und Stamm-Seiten (Längsbänder). Übernommen: Ringe oben, '
         'Bänder seitlich; neu: ruhigere Wellen, Rost-Glut nur in den Bandkanten.'),
        ('Allgemein', 'Faithful-Texturregeln: dunkelste/hellste Töne halten, Zwischentöne nur zum Glätten; '
         'Hue-Shifting (Schatten kühler, Lichter wärmer) gegen matschige Flächen.'),
    ],
    'B': [
        ('Farbkasten', 'Vanilla bundle/brush (Holz-/Lederschattierung, dunkle Kontur), Tuschkasten-Pixelart. '
         'Übernommen: 1-px-Kontur, Licht oben links; neu: fünf eigene Kastenformen.'),
        ('Rucksack', '16x16-Taschen-Assets (itch.io jgiio „bags“), Leder-Rucksack-Sprite (pixie.haus), Vanilla leather. '
         'Übernommen: warme Brauntöne mit hellem Klappenrand; neu: Klappe/Schnallen/Bettrolle je Variante.'),
        ('Fasern', 'Vanilla string und lead (Seil als diagonale Hell-Mittel-Dunkel-Folge). Übernommen: '
         'Seilmuster = zwei verdrillte Stränge; neu: Ring/Spirale/Strang mit Band, Fransen, Knoten, abstehende Fasern.'),
    ],
    'C': [('Enderquarz', 'Vanilla quartz (milchig), amethyst_shard (violette Facetten), ender_pearl (Türkis). '
           'Übernommen: nur Farbwelten; Form = unsere Pixelmaske 1:1, jede Helligkeitsstufe neu belegt.')],
    'D': [('Lohenbuch', 'Vanilla book / enchanted_book (schräg liegendes Buch, Seitenkante links unten, Lesezeichen). '
           'Übernommen: Silhouette und Blickwinkel; neu selbst konstruiert: Lohen-Einband, Flamme/Glutriss/Lohenruten, Goldecken.')],
    'E': [('Hufeisen', 'Vanilla Barren (iron/gold/copper/netherite_ingot, diamond) für Materialrampen. '
           'Übernommen: Farbrampen-Logik; Form = unsere Maske 1:1, neu: Rohr-Licht (E1), Fase + Nagelköpfe (E2), gehämmert (E3).')],
}

SOURCES = [
    ('Minecraft Wiki – History of textures', 'https://minecraft.wiki/w/History_of_textures'),
    ('Faithful – f32 texturing guidelines', 'https://docs.faithfulpack.net/pages/textures/f32-texturing-guidelines'),
    ('Faithful – Glossar (Dithering, Kontrast)', 'https://docs.faithfulpack.net/pages/textures/glossary'),
    ('Better Crimson and Warped Blocks', 'https://mcpedl.com/better-crimson-blocks/'),
    ('Bookshelves (Varianten je Holz)', 'https://www.modpackindex.com/mod/61317/bookshelves'),
    ('16x16 bags (itch.io)', 'https://jgiio.itch.io/free-2d-item-16x16-bags'),
    ('Leder-Rucksack-Sprite', 'https://pixie.haus/pixelart/517880/vintage-leather-travel-pack-sprite'),
    ('Shading & Hue-Shifting', 'https://pixnote.net/en/learn/shading'),
    ('Farbpaletten', 'https://www.wayline.io/learn/pixel-art/2'),
    ('Minecraft Wiki – Enchanted Book', 'https://minecraft.wiki/w/Enchanted_Book'),
]

TITLES = {'A': 'A · Blöcke (Bild 1)', 'B': 'B · Farbkasten, Rucksack, Fasern (Bild 2)', 'C': 'C · Enderquarz',
          'D': 'D · Lohenbuch', 'E': 'E · Hufeisen'}


def uri(im, scale=8):
    if im.size[0] <= 16:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    b = io.BytesIO()
    im.save(b, 'PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()


def slot(im, cap, cls='slot'):
    return (f'<figure><div class="{cls}"><img src="{uri(im)}" alt="{html.escape(cap)}"></div>'
            f'<figcaption>{html.escape(cap)}</figcaption></figure>')


CSS = """
:root{--bg:#f4f1ea;--fg:#1f1d1a;--muted:#6b665c;--card:#fffdf8;--line:#ddd6c8;--accent:#8a4b12;
--slot:#8b8b8b;--slot-hi:#ffffff;--slot-lo:#373737;}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#17161a;--fg:#ece8e0;--muted:#a49e92;
--card:#211f25;--line:#38343d;--accent:#f0a85a;}}
:root[data-theme="dark"]{--bg:#17161a;--fg:#ece8e0;--muted:#a49e92;--card:#211f25;--line:#38343d;--accent:#f0a85a;}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}
main{max-width:1200px;margin:0 auto;padding:16px}
h1{font-size:1.5rem;margin:.5rem 0}h2{margin:2rem 0 .5rem;border-bottom:2px solid var(--line);padding-bottom:4px}
h3{margin:1.2rem 0 .4rem;font-size:1.05rem}
p.lead,.muted{color:var(--muted)}
.refs{background:var(--card);border:1px solid var(--line);border-radius:8px;padding:8px 14px;margin:.5rem 0}
.refs li{margin:.2rem 0}
.row{display:flex;flex-wrap:wrap;gap:12px;align-items:flex-start}
.card{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px;max-width:100%}
.card.before{border-style:dashed}
.lab{font-weight:700;font-size:1.3rem;color:var(--accent);margin-bottom:4px}
.faces{display:flex;flex-wrap:wrap;gap:8px}
figure{margin:0;text-align:center}figcaption{font-size:.75rem;color:var(--muted)}
.slot{background:var(--slot);padding:4px;border:3px solid;border-color:var(--slot-lo) var(--slot-hi) var(--slot-hi) var(--slot-lo);
display:inline-block;line-height:0}
.slot img{image-rendering:pixelated;width:128px;height:128px}
.iso img{width:128px;height:128px;image-rendering:pixelated}
.shot img{width:auto;height:128px;image-rendering:pixelated}
@media (max-width:600px){.slot img,.iso img{width:96px;height:96px}.shot img{height:96px}}
"""


def write(secs, path):
    out = ['<!doctype html><html lang="de"><head><meta charset="utf-8">',
           '<meta name="viewport" content="width=device-width,initial-scale=1">',
           '<title>N41 Texturvorschläge</title>', f'<style>{CSS}</style></head><body><main>',
           '<h1>Nachtrag 41 – Texturvorschläge</h1>',
           '<p class="lead">Nur Vorschläge, nichts ist im Spiel. 16×16, 8× vergrößert auf Inventar-Slot. '
           'Gestrichelt = jetziger Stand. Zum Auswählen die Buchstaben-Nummern nennen (z. B. „A2, B7, E1“). '
           'Blöcke zusätzlich als Würfel-Render.</p>']
    for k, lst in secs.items():
        out.append(f'<h2>{TITLES[k]}</h2><div class="refs"><b>Referenzen &amp; was übernommen wurde</b><ul>')
        out += [f'<li><b>{html.escape(a)}:</b> {html.escape(b)}</li>' for a, b in REFS[k]]
        out.append('</ul></div>')
        cur = None
        for e in lst:
            if e['block'] != cur:
                if cur is not None:
                    out.append('</div>')
                cur = e['block']
                out.append(f'<h3>{html.escape(NAMES.get(cur, cur))}</h3><div class="row">')
            if e.get('before'):
                figs = []
                for cap, im in e['before']:
                    if im.size[0] > 16:
                        figs.append(f'<figure><div class="slot shot"><img src="{uri(im)}" alt="Screenshot"></div>'
                                    f'<figcaption>{html.escape(cap)}</figcaption></figure>')
                    else:
                        figs.append(slot(im, cap))
                out.append(f'<div class="card before"><div class="lab">Vorher</div><div class="faces">{"".join(figs)}</div></div>')
            figs = [slot(im, cap) for cap, im in e['faces']]
            if e.get('iso') is not None:
                figs.append(f'<figure><div class="slot iso"><img src="{uri(e["iso"])}" alt="Render"></div>'
                            f'<figcaption>Render</figcaption></figure>')
            out.append(f'<div class="card"><div class="lab">{e["label"]}</div><div class="faces">{"".join(figs)}</div></div>')
        out.append('</div>')
    out.append('<h2>Quellen</h2><ul>')
    out += [f'<li><a href="{u}">{html.escape(t)}</a></li>' for t, u in SOURCES]
    out.append('</ul><p class="muted">Generator: tools/textures/n41_proposals_2026_10_12.py · PNGs: docs/ai/previews/n41/ '
               '(Branch claude-n41-tex). Vanilla-Texturen nur angesehen, nicht eingecheckt.</p></main></body></html>')
    with open(path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(out))
