"""Usage: python3.12 tools/textures/wb24_gems/wb24_gems.py

WB24-3 (Besitzer 24.10.): NUR Vorschlaege, nichts in assets.
  Nether-Juwel A1-A3  - Seelenglut (WA24-5 A) ueberarbeitet: kuehlere Karmesin-Rampe, Seelenfeuer-Tuerkis, langsam.
  Ender-Juwel  E1-E3  - aus Sternenschliff (WA24-5 C): eigener hoher Schliff, End-Palette (Enderperle/Enderauge/
                        Endstein/Purpur), Lichtband + Glitzer bzw. Kern/Partikel.
  Ender-Herz   H1-H3  - eigene Herz-/Kern-Silhouette (kein Netherherz-Recolor), Stil wie Netherherz (dunkle Kontur,
                        Licht oben links), Herzschlag.
Je Variante: variants/<name>.png (16x16*N Streifen), .png.mcmeta, .layers.json (Ebenen Kontur/Grundform/Effekt,
PRINZIPIEN A8 b). Alle Raster und Farben sind eigene Pixel (Farben an Vanilla-Paletten angelehnt), keine Vanilla-Pixel.
Braucht Pillow.
"""
import json, math, os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(HERE)))
sys.path.insert(0, os.path.join(REPO, 'tools', 'textures'))
import layers  # noqa: E402

OUT = os.path.join(HERE, 'variants')


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def ramp(*cs):
    return [hexc(c) for c in cs]


def pick(r, v):
    """Farbe aus Rampe r fuer Wert v (0..1) bzw. Index (int)."""
    i = v if isinstance(v, int) else int(round(v * (len(r) - 1)))
    return r[max(0, min(len(r) - 1, i))]


# ------------------------------------------------------------------ Raster
# o Kontur, sonst Facetten-Zonen; ZONE gibt die Grundhelligkeit (Index in die Rampe, 0 = Kontur).
NETHER_GEM = [
    "................",
    "................",
    "................",
    "....oooooooo....",
    "...oHHTTTTTRo...",
    "..oLHLkTTTkRRo..",
    ".oLLLkTTTTTkRRo.",
    ".oGGGGGggggggGo.",
    "..ollklmmmkrro..",
    "...ollklmkrro...",
    "....olkmkrro....",
    ".....olmkro.....",
    "......ommo......",
    ".......oo.......",
    "................",
    "................",
]
NETHER_ZONE = {'o': 0, 'H': 7, 'L': 6, 'T': 5, 'R': 4, 'k': 3, 'G': 5, 'g': 3, 'l': 4, 'm': 3, 'r': 2}

ENDER_GEM = [  # hoher Sternenschliff: oben breit-spitz, unten lang
    "................",
    ".......oo.......",
    "......oHRo......",
    ".....oLhkRo.....",
    "....oLLhkRRo....",
    "...oLLLhkRRRo...",
    "..oLLLLhkRRRRo..",
    "..oGGGGGgggggo..",
    "..ollllKmrrrro..",
    "...olllKmrrro...",
    "....ollKmrro....",
    ".....olKmro.....",
    "......oKmo......",
    "......oKmo......",
    ".......oo.......",
    "................",
]
ENDER_ZONE = {'o': 0, 'H': 7, 'L': 5, 'h': 6, 'k': 3, 'R': 4, 'G': 5, 'g': 3, 'l': 4, 'K': 4, 'm': 3, 'r': 2}

HEART = [  # eigene Herz-Silhouette (zwei runde Kuppeln, Spitze unten)
    "................",
    "................",
    "...oooo..oooo...",
    "..oHLLLooLLLLo..",
    ".oHLLLLLLLLLLMo.",
    ".oLLLLLLLLLLMMo.",
    ".oLLLLLLLLLMMMo.",
    ".oLLLLLLLLLMMMo.",
    "..oLLLLLLLMMMo..",
    "...oLLLLLMMMo...",
    "....oLLLMMMo....",
    ".....oLMMMo.....",
    "......oMMo......",
    ".......oo.......",
    "................",
    "................",
]
HEART_CHORUS = [  # wie HEART, mit zwei Chorus-Stielen statt Adern
    "................",
    "....oo....oo....",
    "...oSSo..oSSo...",
    "..oLSSLooLSSLo..",
] + HEART[4:]


def cells(grid):
    return {(x, y): ch for y, row in enumerate(grid) for x, ch in enumerate(row) if ch != '.'}


def base_index(grid, zone):
    """Grundhelligkeit je Pixel; Pixel an der rechten/unteren Kontur eine Stufe dunkler (Vanilla-Tiefe)."""
    c = cells(grid)
    out = {}
    for (x, y), ch in c.items():
        v = zone.get(ch, 4)
        if ch != 'o' and v > 1 and (c.get((x + 1, y)) == 'o' or c.get((x, y + 1)) == 'o') and ch not in 'H':
            v -= 1
        out[(x, y)] = v
    return c, out


def sphere(c, cx, cy, lx, ly, rad):
    """Kugelige Helligkeit 0..1 (Licht bei lx,ly) fuer Herz-Koerper."""
    return {k: max(0.0, 1 - math.hypot(k[0] - lx, k[1] - ly) / rad) for k in c}


# ------------------------------------------------------------------ Abspielhelfer
def heartbeat(rest=60, step=3):
    """Frames 0 Ruhe, 1-2 'lub', 3 zwischen, 4-5 'dub' (kleiner); interpolate glaettet."""
    return {'interpolate': True, 'frames': [
        {'index': 0, 'time': rest}, {'index': 1, 'time': step}, {'index': 2, 'time': step + 1},
        {'index': 3, 'time': step}, {'index': 4, 'time': step}, {'index': 5, 'time': step + 3}]}


BEAT = [0.0, 0.7, 1.0, 0.35, 0.65, 0.2]  # Staerke je Herzschlag-Frame


# ------------------------------------------------------------------ Nether-Juwel A1-A3
CRIMSON = ramp('#14040e', '#2c0818', '#4a0c22', '#6c1230', '#92203c', '#b8344a', '#dc6270', '#ffb0b4')
SOUL = ramp('#015a5e', '#01a2a7', '#23e0e6', '#85fbff', '#dbfeff')


def nether_base():
    c, b = base_index(NETHER_GEM, NETHER_ZONE)
    return c, {k: CRIMSON[v] for k, v in b.items()}, b


def a1_seelenkern():
    """Seelenkern: Tuerkiser Kern atmet langsam auf und ab, bei voller Glut leuchtet die Rundiste mit."""
    c, base, b = nether_base()
    frames = []
    for t in range(16):
        s = 0.5 - 0.5 * math.cos(2 * math.pi * t / 16)  # 0..1..0
        f = dict(base)
        r = 1.2 + 1.3 * s
        for (x, y), ch in c.items():
            if ch == 'o':
                continue
            d = math.hypot(x - 7.5, y - 8.0)
            if d < r:
                f[(x, y)] = pick(SOUL, (1 - d / r) * (0.5 + 0.5 * s) + 0.15)
            elif d < r + 0.6 and s > 0.6 and y >= 7:
                f[(x, y)] = CRIMSON[2]  # dunkler Hof um den Kern
            elif ch == 'k' and s > 0.6:
                f[(x, y)] = SOUL[0] if s < 0.85 else SOUL[1]
        frames.append(f)
    return frames, {'frametime': 10, 'interpolate': True}


def a2_seelenwirbel():
    """Seelenwirbel: ein Seelenfunke kreist langsam um den Kern (Ellipse), Kern glimmt staerker, wenn er vorn ist."""
    c, base, b = nether_base()
    frames = []
    n = 24
    for t in range(n):
        f = dict(base)
        a = 2 * math.pi * t / n
        front = 0.5 + 0.5 * math.sin(a)  # 1 = unten/vorn
        for (x, y), ch in c.items():
            if ch != 'o' and math.hypot(x - 7.5, y - 8.3) < 1.6:
                f[(x, y)] = pick(SOUL, 0.25 + 0.5 * front)
        for k, tail in enumerate((0, 0.35, 0.7)):
            px = round(7.5 + 4.6 * math.cos(a - tail) - 0.5)
            py = round(8.0 + 2.4 * math.sin(a - tail) - 0.5)
            if c.get((px, py), 'o') != 'o':
                f[(px, py)] = SOUL[4 - k]
        frames.append(f)
    return frames, {'frametime': 5}


def a3_seelenflamme():
    """Seelenflamme: Seelenfeuer-Zungen steigen langsam von der Spitze in den Stein, die Krone bleibt Karmesin."""
    c, base, b = nether_base()
    frames = []
    for t in range(16):
        f = dict(base)
        for (x, y), ch in c.items():
            if ch == 'o' or y < 7:
                continue
            a = 2 * math.pi * t / 16
            hx = 2.6 + 1.6 * math.sin(a + x * 1.1) + 0.7 * math.sin(2 * a + x * 2.3)  # Zungenhoehe je Spalte
            dy = 12.5 - y
            if dy < hx:
                f[(x, y)] = pick(SOUL, 0.15 + 0.85 * (1 - dy / hx))
        if t in (6, 7):  # kurzer Glanz oben links
            f[(4, 4)] = CRIMSON[7]
            f[(5, 4)] = SOUL[4] if t == 6 else CRIMSON[7]
        frames.append(f)
    return frames, {'frametime': 8, 'interpolate': True}


# ------------------------------------------------------------------ Ender-Juwel E1-E3
PEARL = ramp('#06141a', '#0b3a36', '#105e51', '#1b7b6b', '#349988', '#2ccdb1', '#8cf4e2', '#e6fff6')
VOIDP = ramp('#0b0614', '#1a0c2c', '#2d0a48', '#4a1470', '#6a2a98', '#8f5cc0', '#c7a8e7', '#f2e8ff')
PURPUR = ramp('#2a1430', '#4f2a4f', '#6e466e', '#906590', '#ac7bac', '#c7a8c7', '#e4d2e4', '#fbf3fb')
ENDSTONE = hexc('#eef6b4')
EYE = ramp('#1b2b10', '#3c6a2a', '#71ac49', '#97d16b', '#cbfcdd')


def ender_base(r, outline=None):
    c, b = base_index(ENDER_GEM, ENDER_ZONE)
    base = {k: r[v] for k, v in b.items()}
    if outline:
        for k, ch in c.items():
            if ch == 'o':
                base[k] = outline
    return c, base


def star(f, c, x, y, col, arm):
    f[(x, y)] = col
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if c.get((x + dx, y + dy), 'o') != 'o':
            f[(x + dx, y + dy)] = arm


def e1_perlschliff():
    """Perlschliff (C-Ableitung): Enderperl-Tuerkis, Purpur-Kontur; Endstein-Lichtband gleitet diagonal, dann Glitzerstern, lange Ruhe."""
    c, base = ender_base(PEARL, hexc('#2a0e3a'))
    frames = [dict(base)]
    for t in range(8):  # Lichtband
        f = dict(base)
        pos = -6 + t * 3.2
        for (x, y), ch in c.items():
            if ch == 'o':
                continue
            d = (x + y) - 8 - pos
            if abs(d) < 0.8:
                f[(x, y)] = ENDSTONE
            elif abs(d) < 1.8:
                f[(x, y)] = PEARL[min(7, ENDER_ZONE.get(ch, 4) + 2)]
        frames.append(f)
    for k in range(3):  # Glitzerstern
        f = dict(base)
        star(f, c, 6, 4, PEARL[7] if k == 1 else ENDSTONE, PEARL[6] if k == 1 else base[(5, 4)])
        frames.append(f)
    meta = {'frames': [{'index': 0, 'time': 120}] + [{'index': i, 'time': 3} for i in range(1, 9)]
            + [{'index': 0, 'time': 30}, {'index': 9, 'time': 3}, {'index': 10, 'time': 5}, {'index': 11, 'time': 3}]}
    return frames, meta


def e2_leerenschliff():
    """Leerenschliff: Leere-Violett (Drachenei/weinender Obsidian), innen pulsiert langsam ein Enderperl-Kern; Purpur-Glanz an der Kante."""
    c, base = ender_base(VOIDP)
    frames = []
    for t in range(16):
        s = 0.5 - 0.5 * math.cos(2 * math.pi * t / 16)
        f = dict(base)
        for (x, y), ch in c.items():
            if ch == 'o':
                continue
            d = math.hypot((x - 7.5) * 1.4, y - 7.8)
            r = 1.8 + 1.8 * s
            if d < r:
                f[(x, y)] = pick(PEARL, 0.45 + 0.55 * (1 - d / r) * (0.4 + 0.6 * s))
        g = int(round(t / 15 * 5))  # Glanz wandert die linke obere Kante hinab
        for i, (x, y) in enumerate(((7, 2), (6, 3), (5, 4), (4, 5), (3, 6), (3, 7))):
            if i == g:
                f[(x, y)] = VOIDP[7] if (x, y) != (7, 2) else VOIDP[6]
        frames.append(f)
    return frames, {'frametime': 8, 'interpolate': True}


def e3_purpurstern():
    """Purpurstern: Perl-Koerper mit Purpur-Krone; Ender-Partikel (violett) treiben langsam zur Mitte, dort Glitzerstern."""
    c, b = base_index(ENDER_GEM, ENDER_ZONE)
    base = {}
    for k, v in b.items():
        ch = c[k]
        base[k] = PURPUR[max(1, v - (1 if v > 5 else 0))] if ch != 'o' else hexc('#2a0e3a')
    frames = []
    starts = [(3, 7), (12, 7), (7, 1), (8, 13), (4, 9), (11, 5)]
    n = 24
    for t in range(n):
        f = dict(base)
        for i, (sx, sy) in enumerate(starts):
            ph = ((t + i * 4) % n) / n
            if ph > 0.8:
                continue
            q = ph / 0.8
            x = round(sx + (7.5 - sx) * q - 0.5 * q)
            y = round(sy + (7.5 - sy) * q - 0.5 * q)
            if c.get((x, y), 'o') != 'o':
                f[(x, y)] = PEARL[6] if q > 0.5 else PEARL[4]
        if t % 12 in (10, 11):
            star(f, c, 7, 7, ENDSTONE if t % 12 == 10 else PEARL[7], PEARL[5])
        frames.append(f)
    return frames, {'frametime': 5}


# ------------------------------------------------------------------ Ender-Herz H1-H3
def heart_body(grid, r, light=(4.5, 4.0), rad=11.0, outline=None):
    c = cells(grid)
    s = sphere(c, 7.5, 7.5, *light, rad)
    base = {}
    for (x, y), ch in c.items():
        if ch == 'o':
            base[(x, y)] = outline or r[0]
            continue
        v = 1 + s[(x, y)] * (len(r) - 2.2)
        if ch == 'H':
            v = len(r) - 1
        if c.get((x + 1, y)) == 'o' or c.get((x, y + 1)) == 'o':
            v -= 0.8
        base[(x, y)] = r[max(1, min(len(r) - 1, int(round(v))))]
    return c, base


def rays(c, cx, cy, angles, r0):
    out = []
    for a in angles:
        line = []
        for k in range(int(r0 * 4), 40):
            d = k / 4
            p = (round(cx + d * math.cos(math.radians(a)) - 0.5), round(cy - d * math.sin(math.radians(a)) - 0.5))
            if c.get(p, 'o') == 'o':
                break
            if p not in line:
                line.append(p)
        out.append(line)
    return out


def h1_leerenkern():
    """Leerenkern: Herz aus Leere-Violett, in der Mitte ein Enderperl-Kern; Herzschlag (lub-dub) laesst Kern und Risse aufleuchten."""
    c, base = heart_body(HEART, VOIDP[:7])
    cracks = rays(c, 7.5, 7.0, (130, 50, 205, 335, 270), 2.8)
    frames = []
    for s in BEAT:
        f = dict(base)
        for (x, y), ch in c.items():
            d = math.hypot(x - 7.5, y - 7.0)
            if ch != 'o' and d < 2.6 + 0.5 * s:
                sh = max(0, 1 - math.hypot(x - 6.5, y - 6.0) / 3.4)
                f[(x, y)] = pick(PEARL, 0.3 + 0.4 * sh + 0.3 * s)
        f[(6, 5)] = PEARL[6 if s < 0.5 else 7]  # Glanzpunkt auf dem Kern
        for line in cracks:
            for i, p in enumerate(line):
                glow = s - i * 0.18
                f[p] = VOIDP[6] if glow > 0.55 else VOIDP[5] if glow > 0.2 else VOIDP[3]
        frames.append(f)
    return frames, heartbeat(60)


def h2_perlherz():
    """Perlherz: Koerper wie eine Enderperle (Tuerkis, Kugel-Licht), in der Mitte ein Enderauge, dessen Iris im Herzschlag gluehen."""
    c, base = heart_body(HEART, PEARL[:7], outline=PEARL[0])
    frames = []
    for s in BEAT:
        f = dict(base)
        for (x, y), ch in c.items():
            if ch == 'o':
                continue
            d = math.hypot(x - 7.5, (y - 7.5) * 0.8)
            if d < 3.0:
                f[(x, y)] = pick(EYE, 0.25 + 0.45 * (1 - d / 3.0) + 0.35 * s)
        pupil = [(x, y) for x in (7, 8) for y in (range(5, 10) if s < 0.6 else range(6, 9))]
        for p in pupil:
            f[p] = EYE[0]
        f[(6, 5)] = EYE[4]
        frames.append(f)
    return frames, heartbeat(70)


def h3_chorusherz():
    """Chorusherz: Purpur-Fleisch mit zwei Chorus-Stielen; tuerkise Adern verzweigen sich von der Spitze nach oben, ein Lichtpuls steigt langsam auf."""
    c, base = heart_body(HEART_CHORUS, PURPUR[:6], light=(4.5, 4.5), rad=13.0, outline=hexc('#24102a'))
    for (x, y), ch in c.items():
        if ch == 'S':
            base[(x, y)] = PURPUR[5] if y <= 2 else PURPUR[3] if x in (4, 10) else PURPUR[2]
    veins = [(7, 12), (7, 11), (7, 10), (8, 9), (8, 8), (7, 7), (7, 6), (6, 5), (5, 4), (9, 6), (10, 5), (10, 4)]
    frames = []
    for t in range(16):
        f = dict(base)
        for (x, y) in veins:
            v = 0.5 + 0.5 * math.cos(2 * math.pi * (t / 16 - (13 - y) / 11))
            f[(x, y)] = pick(PEARL, 0.35 + 0.65 * v)
        tip = 0.5 + 0.5 * math.cos(2 * math.pi * (t / 16 - 11 / 11))
        for p in ((4, 1), (5, 1), (10, 1), (11, 1)):  # Stiel-Kappen leuchten, wenn der Puls oben ankommt
            f[p] = PEARL[6] if tip > 0.7 else base[p]
        frames.append(f)
    return frames, {'frametime': 6, 'interpolate': True}


# ------------------------------------------------------------------ WC24-1 Endfassungen (Besitzer-Wahl 24.10.)
def nether_final():
    """A3 mit hoeherer Seelenflamme: Zungen steigen bis in die Krone, dunkler Karmesin-Saum ueber den Spitzen,
    die oberste Kronenreihe bleibt Karmesin (Stein bleibt als Nether-Juwel lesbar)."""
    c, base, b = nether_base()
    frames = []
    for t in range(16):
        a = 2 * math.pi * t / 16
        f = dict(base)
        for (x, y), ch in c.items():
            if ch == 'o' or y < 5:
                continue
            hx = 4.6 + 2.4 * math.sin(a + x * 1.7) + 0.8 * math.sin(2 * a + x * 2.9)  # Zungenhoehe je Spalte (2-8 px)
            dy = 12.5 - y
            if dy < hx:
                f[(x, y)] = pick(SOUL, 0.1 + 0.9 * (1 - dy / hx) ** 0.8)
            elif dy < hx + 1:
                f[(x, y)] = CRIMSON[2]  # Saum: Flamme hebt sich vom Stein ab
        if t in (6, 7):  # kurzer Glanz oben links
            f[(4, 4)] = CRIMSON[7]
            f[(5, 4)] = SOUL[4] if t == 6 else CRIMSON[7]
        frames.append(f)
    return frames, {'frametime': 8, 'interpolate': True}


ENDER_GEM_FINAL = [  # E2-Schliff mit klaren Facetten: aeussere/innere Facette je Seite, Grat H|h bzw. K|m
    "................",
    ".......oo.......",
    "......oHho......",
    ".....oAHhRo.....",
    "....oALHhRro....",
    "...oAALHhRrro...",
    "..oAALLHhRRrro..",
    "..oGGGGGgggggo..",
    "..olnnnKmsssro..",
    "...olnnKmssro...",
    "....olnKmsro....",
    ".....olKmro.....",
    "......oKmo......",
    "......oKmo......",
    ".......oo.......",
    "................",
]
ENDER_FINAL_ZONE = {'o': 0, 'H': 7, 'A': 6, 'L': 5, 'h': 4, 'R': 3, 'r': 2, 'G': 6, 'g': 4,
                    'n': 5, 'l': 4, 'K': 5, 'm': 3, 's': 3}


def ender_final():
    """E2 verfeinert: Leere-Violett, Facetten in klaren Flaechen; der Enderperl-Kern faerbt die Facetten nur ein
    (gleiche Stufe in der Perl-Rampe, Linien bleiben), ruhiger Puls, einmal je Runde ein Glanz oben links."""
    c, b = base_index(ENDER_GEM_FINAL, ENDER_FINAL_ZONE)
    base = {k: (VOIDP[v] if c[k] != 'o' else VOIDP[0]) for k, v in b.items()}
    frames = []
    for t in range(16):
        s = 0.5 - 0.5 * math.cos(2 * math.pi * t / 16)
        f = dict(base)
        r = 1.4 + 1.2 * s
        for (x, y), ch in c.items():
            if ch in 'oH':
                continue
            d = math.hypot((x - 7.5) * 1.2, y - 8.0)
            if d < r:
                f[(x, y)] = PEARL[max(2, min(7, b[(x, y)] + (1 if d < r * 0.5 and s > 0.5 else 0)))]
        if t in (3, 4):
            f[(5, 4)] = VOIDP[7]
            if t == 3:
                star(f, c, 5, 5, VOIDP[7], VOIDP[6])
        frames.append(f)
    return frames, {'frametime': 10, 'interpolate': True}


HEART_FINAL = [  # kantiges Herz: flache Kuppeln mit Chorus-Stielen, gerade Seiten, 45-Grad-Spitze, Facettenflaechen
    "................",
    "....oo....oo....",
    "...oSSo..oSSo...",
    "..oASSAooBSSCo..",
    ".oAAAAAKKBBBCCo.",
    ".oAAAAKKKKBBCCo.",
    ".oDAAKKKKKKBCCo.",
    ".oDDKKKKKKKKCCo.",
    "..oDDKKKKKKCCo..",
    "...oDEKKKKFCo...",
    "....oEEKKFFo....",
    ".....oEEFFo.....",
    "......oEFo......",
    ".......oo.......",
    "................",
    "................",
]
HEART_RABBIT = [  # Alternativ-Lesart "Kaninchen": Stiele werden zu langen Ohren (nur Vorschau)
    "...oo......oo...",
    "..oSSo....oSSo..",
    "..oSSo....oSSo..",
    "..oASSAooBSSCo..",
] + HEART_FINAL[4:]
HEART_FINAL_ZONE = {'A': 5, 'B': 4, 'D': 3, 'K': 3, 'C': 2, 'E': 2, 'F': 1}
HEART_OUTLINE = hexc('#24102a')


def _heart_final(grid, veins, caps):
    c = cells(grid)
    base = {}
    for (x, y), ch in c.items():
        if ch == 'o':
            base[(x, y)] = HEART_OUTLINE
        elif ch == 'S':
            base[(x, y)] = PURPUR[5] if c.get((x, y - 1)) == 'o' else PURPUR[4] if x in (3, 4, 10) else PURPUR[3]
        else:
            base[(x, y)] = PURPUR[HEART_FINAL_ZONE[ch]]
    base[(3, 4)] = base[(4, 4)] = PURPUR[6]  # Glanzkante oben links
    frames = []
    for s in BEAT:
        f = dict(base)
        for (x, y), ch in c.items():
            if ch != 'K':
                continue
            d = abs(x - 7.5) + abs(y - 7.0)  # rautenfoermiger Kern (kantig wie die Mittelfacette)
            if d < 2.1 + 1.1 * s:
                left = 1 if x < 8 and y < 8 else 0  # Facette oben links etwas heller
                f[(x, y)] = PEARL[max(3, min(7, int(round(4 + left + 2.5 * s * (1 - d / 3.2)))))]
        f[(6, 6)] = PEARL[6 if s < 0.5 else 7]  # Glanzpunkt auf dem Kern
        for line in veins:
            for i, p in enumerate(line):
                glow = s - i * 0.2
                f[p] = PEARL[6] if glow > 0.55 else PEARL[4] if glow > 0.2 else PEARL[2]
        if s > 0.6:
            for p in caps:
                f[p] = PEARL[6]
        frames.append(f)
    return frames, heartbeat(60)


def heart_final():
    """H3 + H1: Chorus-Purpur, Chorus-Stiele, tuerkiser Enderperl-Kern; im Herzschlag laufen Adern zu den Stielen."""
    veins = [[(6, 4), (5, 3), (5, 2)], [(9, 4), (10, 3), (10, 2)], [(7, 11), (7, 12)]]
    return _heart_final(HEART_FINAL, veins, ((4, 1), (5, 1), (10, 1), (11, 1)))


def heart_rabbit():
    veins = [[(6, 4), (5, 3), (4, 2), (4, 1)], [(9, 4), (10, 3), (11, 2), (11, 1)], [(7, 11), (7, 12)]]
    return _heart_final(HEART_RABBIT, veins, ((3, 0), (4, 0), (11, 0), (12, 0)))


VARIANTS = {
    'nether_gem_a1_seelenkern': a1_seelenkern,
    'nether_gem_a2_seelenwirbel': a2_seelenwirbel,
    'nether_gem_a3_seelenflamme': a3_seelenflamme,
    'ender_gem_e1_perlschliff': e1_perlschliff,
    'ender_gem_e2_leerenschliff': e2_leerenschliff,
    'ender_gem_e3_purpurstern': e3_purpurstern,
    'ender_heart_h1_leerenkern': h1_leerenkern,
    'ender_heart_h2_perlherz': h2_perlherz,
    'ender_heart_h3_chorusherz': h3_chorusherz,
    # WC24-1 Endfassungen (Besitzer-Wahl 24.10. nachmittags)
    'nether_gem_final': nether_final,
    'ender_gem_final': ender_final,
    'ender_heart_final': heart_final,
    'ender_heart_alt_kaninchen': heart_rabbit,  # nur Vorschau
}
INSTALL = {'nether_gem_final': 'nether_gem', 'ender_gem_final': 'ender_gem', 'ender_heart_final': 'ender_heart'}
MOBS_ITEM = os.path.join(REPO, 'modules/simplemobs/shared/resources/assets/simplemobs/textures/item')
GRIDS = {'nether_gem_final': NETHER_GEM, 'ender_gem_final': ENDER_GEM_FINAL, 'ender_heart_final': HEART_FINAL,
         'ender_heart_alt_kaninchen': HEART_RABBIT}


def render(name, frames, meta):
    """-> (PIL-Bild, mcmeta-Text, Ebenen-Text); Ebenen Kontur/Grundform aus Frame 0, Effekt = Abweichungen."""
    n = len(frames)
    first = frames[0]
    kontur = Image.new('RGBA', (16, 16 * n))
    grund = Image.new('RGBA', (16, 16 * n))
    eff = Image.new('RGBA', (16, 16 * n))
    full = Image.new('RGBA', (16, 16 * n))
    for i, f in enumerate(frames):
        for (x, y), col in f.items():
            full.putpixel((x, y + 16 * i), col)
            base = first.get((x, y))
            tgt = kontur if base is not None and _is_outline(name, x, y) else grund
            if base is not None:
                tgt.putpixel((x, y + 16 * i), base)
            if col != base:
                eff.putpixel((x, y + 16 * i), col)
    doc = layers.from_layers(16, 16 * n, [('Kontur', kontur.tobytes()), ('Grundform', grund.tobytes()), ('Effekt', eff.tobytes())])
    assert bytes(layers.compose(doc)) == full.tobytes(), name + ': Ebenen ergeben nicht das PNG'
    return full, json.dumps({'animation': meta}, indent=2) + '\n', layers.dumps(doc)


def write(png, img, meta_text, layers_text):
    img.save(png)
    with open(png + '.mcmeta', 'w', newline='\n') as fh:
        fh.write(meta_text)
    with open(layers.layers_path(__import__('pathlib').Path(png)), 'w', newline='\n', encoding='utf-8') as fh:
        fh.write(layers_text)


def stale(png, img, meta_text, layers_text):
    """True, wenn PNG/.mcmeta/.layers.json nicht dem Generator entsprechen."""
    lp = png[:-4] + '.layers.json'
    if not all(os.path.isfile(p) for p in (png, png + '.mcmeta', lp)):
        return True
    with Image.open(png) as cur:
        same = cur.convert('RGBA').tobytes() == img.tobytes() and cur.size == img.size
    return not same or open(png + '.mcmeta', encoding='utf-8').read() != meta_text \
        or open(lp, encoding='utf-8').read() != layers_text


def _is_outline(name, x, y):
    grid = GRIDS.get(name) or (NETHER_GEM if name.startswith('nether_gem') else ENDER_GEM if name.startswith('ender_gem') else
                               HEART_CHORUS if 'chorus' in name else HEART)
    return grid[y][x] == 'o'


def main(argv):
    """ohne Argument: Varianten nach variants/ schreiben; --install: zusaetzlich Endfassungen als echte Texturen;
    --check: nur pruefen, ob die eingebauten Texturen dem Generator entsprechen."""
    if '--check' in argv:
        bad = [t for v, t in INSTALL.items() if stale(os.path.join(MOBS_ITEM, t + '.png'), *render(v, *VARIANTS[v]()))]
        print('veraltet: ' + ', '.join(bad) if bad else 'wb24_gems: eingebaute Texturen aktuell')
        return 1 if bad else 0
    os.makedirs(OUT, exist_ok=True)
    for name, fn in VARIANTS.items():
        frames, meta = fn()
        out = render(name, frames, meta)
        write(os.path.join(OUT, name + '.png'), *out)
        print(name, len(frames), 'Frames', meta.get('frametime', 'frames-Liste'))
        if '--install' in argv and name in INSTALL:
            write(os.path.join(MOBS_ITEM, INSTALL[name] + '.png'), *out)
            print('  -> simplemobs/textures/item/' + INSTALL[name] + '.png')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
