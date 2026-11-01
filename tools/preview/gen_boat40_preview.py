"""IG31-40 Ofen-Boot: Vorschau-Seite (3 Optik-Varianten, aus/an) + Konzeptfragen mit Antwortcode.
Aufruf: python3 gen_boat40_preview.py [ausgabe.html]   (nur Pillow)"""
import base64, io, random, sys
from PIL import Image

W, H, S = 72, 48, 8
OAK = [(122, 92, 52), (134, 102, 58), (146, 113, 66), (158, 124, 74), (110, 82, 46), (98, 72, 40)]
DARK = (62, 44, 26)
STONE = [(118, 118, 118), (128, 128, 128), (104, 104, 104), (138, 138, 138), (92, 92, 92)]
BRICK = [(102, 74, 70), (116, 86, 80), (88, 62, 58)]

def plank(rng, x, y):
    base = OAK[(y // 3) % 3]  # Bretter-Baender, weiche Toene
    n = rng.choice(OAK[:4]) if rng.random() < .3 else base
    return n

def hull(img, rng):
    # Rumpf: flache Wanne, Seitenansicht leicht von oben; 8 Pixel hoch
    for y in range(26, 38):
        inset = max(0, (y - 26 - 7)) if y > 33 else 0
        top = 26
        for x in range(8 + inset, 64 - inset):
            c = plank(rng, x, y)
            if y == 26 or y == 37: c = OAK[4]
            if x in (8 + inset, 63 - inset): c = OAK[5]
            img.putpixel((x, y), c)
    # Bug (spitz nach rechts): zwei Stufen
    for y in range(22, 27):
        for x in (60, 61, 62, 63):
            if x >= 60 + (y - 22) // 2: img.putpixel((x, y), OAK[(y + x) % 3])
    # Innenraum (dunkler) als Streifen oben
    for x in range(11, 61):
        for y in (27, 28):
            img.putpixel((x, y), OAK[5] if y == 27 else OAK[4])
    # Wasserlinie
    for x in range(9, 63): img.putpixel((x, 39), (52, 92, 150)); img.putpixel((x, 40), (46, 80, 134))

def block(img, rng, x0, y0, w, h, lit, pal=STONE, face=True):
    for y in range(h):
        for x in range(w):
            c = rng.choice(pal)
            if y == 0: c = tuple(min(255, v + 18) for v in c)
            if y == h - 1 or x == w - 1: c = tuple(v - 22 for v in c)
            img.putpixel((x0 + x, y0 + y), c)
    if face:  # Ofenoeffnung
        ox, oy, ow, oh = x0 + w // 4, y0 + h // 2 - 1, w // 2, h // 2 - 1
        for y in range(oh):
            for x in range(ow):
                img.putpixel((ox + x, oy + y), (30, 28, 28) if not lit else ((250, 170, 40) if y > 0 else (200, 100, 20)))
        if lit:
            for x in range(ow): img.putpixel((ox + x, oy + oh - 1), (255, 215, 90))

def chimney(img, rng, x0, y0, h):
    for y in range(h):
        for x in range(3):
            img.putpixel((x0 + x, y0 + y), BRICK[0 if x == 0 else (1 if y % 2 else 2)] if x < 3 else BRICK[2])
    for x in range(5): img.putpixel((x0 - 1 + x, y0), (70, 50, 48))

def smoke(img, x0, y0):
    for i, (dx, dy, r, a) in enumerate([(0, 0, 2, 190), (2, -4, 2, 150), (-1, -8, 3, 110), (2, -13, 3, 70)]):
        for yy in range(-r, r + 1):
            for xx in range(-r, r + 1):
                if xx * xx + yy * yy <= r * r + 1:
                    px, py = x0 + dx + xx, y0 + dy + yy
                    if 0 <= px < W and 0 <= py < H:
                        img.putpixel((px, py), (200 - i * 8, 200 - i * 8, 200 - i * 8))

def seat(img):  # Steve-Platzhalter: nur Sitzmarke (Kopf+Rumpf)
    for y in range(17, 27):
        for x in range(24, 30):
            img.putpixel((x, y), (60, 100, 170) if y > 21 else (190, 140, 105))

def render(variant, lit):
    rng = random.Random(40)
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    hull(img, rng)
    seat(img)
    if variant == 'A':      # Ofen mittig, kein Schornstein
        block(img, rng, 34, 14, 13, 13, lit)
    elif variant == 'B':    # Ofen hinten (Heck), Schornstein
        block(img, rng, 12, 14, 12, 13, lit)
        chimney(img, rng, 19, 7, 7)
        if lit: smoke(img, 20, 5)
    else:                   # C: Ofen vorn (Bug) mit Rohr, Fahrer hinten
        block(img, rng, 44, 14, 13, 13, lit)
        chimney(img, rng, 52, 6, 8)
        if lit: smoke(img, 53, 4)
    if variant == 'A' and lit:  # Rauchfahne ueber dem Ofen (Partikel), ohne Rohr
        smoke(img, 40, 9)
    return img

def uri(img):
    b = io.BytesIO(); img.resize((W * S, H * S), Image.NEAREST).save(b, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()

def hardshare(img):
    px = img.load(); n = h = 0
    for y in range(H):
        for x in range(W - 1):
            a, b = px[x, y], px[x + 1, y]
            if a[3] and b[3]:
                n += 1; h += abs(sum(a[:3]) - sum(b[:3])) > 90
    return 100 * h / max(n, 1)

LOOKS = {'A': ('Ofen mittig, ohne Schornstein', 'Wie die Truhen-Boote: Block mittig. Rauch-Partikel direkt ueber dem Ofen. Schlichtest, gut zur Vanilla-Optik.'),
         'B': ('Ofen hinten (Heck) + Schornstein', 'Schornstein aus Ziegel, Rauch steigt hinten auf. Fahrer sitzt vorn und sieht frei nach vorn.'),
         'C': ('Ofen vorn (Bug) + Schornstein', 'Wie eine kleine Dampflok. Fahrer sitzt hinten, Ofen verdeckt etwas die Sicht.')}

QUESTIONS = [
 ('steer', 'Steuerung', [('a', 'Spieler lenkt wie im Boot, Ofen liefert nur den Vorwaertsschub (Empfehlung)'), ('b', 'Autopilot: faehrt geradeaus, Spieler lenkt nur seitlich'), ('c', 'Spieler lenkt, Ofen gibt Dauer-Tempo ohne W-Taste')]),
 ('fuel', 'Brennstoff', [('a', 'Rechtsklick mit Brennstoff (wie Ofenlore), Dauer = Brenndauer des Items, max. 32000 Ticks (Empfehlung)'), ('b', 'GUI mit 1 Brennstoff-Slot'), ('c', 'Nur Kohle/Holzkohle')]),
 ('wood', 'Holzarten', [('a', 'Alle Vanilla-Bootarten + Bambusfloss, je ein Rezept: beliebiges Boot + Ofen formlos (Empfehlung)'), ('b', 'Nur Eiche')]),
 ('seat', 'Sitzplaetze', [('a', '1 Spieler (Empfehlung)'), ('b', '2 Spieler, nur der Erste lenkt')]),
 ('land', 'Land und Eis', [('a', 'Wie Vanilla-Boot: Land bremst, Eis schnell, Ofen aendert nichts (Empfehlung)'), ('b', 'Ofen-Boot faehrt auch langsam ueber Land')]),
 ('mod', 'Mod-Zuordnung', [('a', 'Simple Ovens (steht schon die Ofen-Lore), standalone (Empfehlung)'), ('b', 'Simple Gadgets')]),
]

def main(out):
    cards, stats = [], []
    for v, (t, d) in LOOKS.items():
        off, on = render(v, False), render(v, True)
        stats.append(f'{v}: hart aus {hardshare(off):.1f}% / an {hardshare(on):.1f}%')
        cards.append(f'<label class="card"><input type="radio" name="look" value="{v}"{" checked" if v=="A" else ""}> <b>Variante {v}: {t}</b>'
                     f'<p>{d}</p><div class="pair"><figure><img src="{uri(off)}" alt="{v} aus"><figcaption>ohne Brennstoff</figcaption></figure>'
                     f'<figure><img src="{uri(on)}" alt="{v} an"><figcaption>faehrt (Feuer, Rauch)</figcaption></figure></div></label>')
    qs = ''.join(f'<fieldset><legend><b>{t}</b></legend>' + ''.join(
        f'<label class="opt"><input type="radio" name="{k}" value="{o}"{" checked" if o=="a" else ""}> {o.upper()}: {txt}</label><br>' for o, txt in opts) + '</fieldset>'
        for k, t, opts in QUESTIONS)
    keys = ['look'] + [k for k, _, _ in QUESTIONS]
    html = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Ofen-Boot IG31-40</title>
<style>:root{{--bg:#f5f4f0;--fg:#1d1d1b;--mut:#5b5b55;--card:#fff;--bd:#cfccc2;--acc:#1f6f43}}
@media(prefers-color-scheme:dark){{:root{{--bg:#16181b;--fg:#e8e7e2;--mut:#a5a59d;--card:#202327;--bd:#3a3e44;--acc:#5fc08a}}}}
*{{box-sizing:border-box}}body{{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}}main{{max-width:1000px;margin:auto;padding:18px 14px 60px}}
.card{{display:block;background:var(--card);border:1px solid var(--bd);border-radius:8px;padding:10px 12px;margin:10px 0}}.pair{{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:8px}}
figure{{margin:0}}img{{width:100%;height:auto;image-rendering:pixelated;background:linear-gradient(#bfe0ff,#e8f4ff);border-radius:6px}}figcaption,p{{color:var(--mut);font-size:.88rem;margin:2px 0}}
fieldset{{border:1px solid var(--bd);border-radius:8px;margin:10px 0;background:var(--card)}}.opt{{display:block;margin:4px 0}}
output{{display:block;font-family:ui-monospace,monospace;background:var(--card);border:1px solid var(--bd);border-radius:6px;padding:8px;margin-top:8px;overflow-wrap:anywhere;user-select:all}}</style></head><body><main>
<h1>Ofen-Boot (IG31-40)</h1>
<p><b>Vorbestand:</b> kein Konzept/Code. Vorhanden: Ofen-Lore (Simple Ovens, gestufte Furnace-Minecarts) als Vorbild. <b>Platzhalter-Grafik</b> (Seitenansicht, Vanilla-Raster-nah); die endgueltige Textur folgt nach Wahl im Endholz-Prozess (A8b).</p>
<h2>Optik</h2>{''.join(cards)}
<h2>Konzeptfragen (Empfehlung vorgewaehlt)</h2>{qs}
<h2>A12 Vanilla-Nutzen und Eingriff</h2><div class="card"><ul><li><b>Nutzen:</b> Wasserreisen ohne Rudern/Dauertasten, Brennstoff (Kohle, Lava-Eimer, Holz) bekommt eine Transport-Verwendung.</li>
<li><b>Aendert:</b> nur ein neues Fahrzeug und ein formloses Rezept; Vanilla-Boote, -Tempo und -Regeln bleiben unveraendert.</li><li><b>Eingriff: gering</b> (keine Mixins in Vanilla-Boote; eigene Entity). Schub moderat, unter dem Eisboot-Tempo, mit Config-Obergrenze.</li></ul></div>
<h2>Antwort</h2><output id="o"></output>
<script>const K={keys};function u(){{const p=['BOAT40'];for(const k of K){{const e=document.querySelector('input[name='+k+']:checked');p.push(k+'='+(e?e.value:'?'))}}document.getElementById('o').textContent=p.join('|')}}
document.addEventListener('change',u);u();</script></main></body></html>'''
    open(out, 'w').write(html)
    print('\n'.join(stats))
    for v in LOOKS:
        for lit in (False, True):
            render(v, lit).resize((W * S, H * S), Image.NEAREST).save(f'/tmp/boat40_{v}_{int(lit)}.png')

if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else '/root/sb-tools-b/previews/b-boat40-preview.html')
