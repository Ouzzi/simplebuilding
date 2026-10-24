"""Usage: python3.12 tools/textures/wa24_gem/wa24_gem.py [--src <nether_gem.png>]

WA24-5 Nether-Juwel: drei Varianten (A Seelenglut, B Aderstein, C Sternenschliff) als 16x(16*N)-Animationsstreifen
plus .mcmeta-Vorschlag (langsam, interpolate). NUR Vorschlaege: schreibt nach tools/textures/wa24_gem/out/,
nichts in assets. Silhouette und Facetten-Schattierung stammen aus der heutigen Textur (Frame 0), nur Farbrampe
und Animation sind neu. Braucht Pillow.
"""
import json, math, os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(HERE)))
SRC = os.path.join(REPO, 'modules/simplemobs/shared/resources/assets/simplemobs/textures/item/nether_gem.png')
OUT = os.path.join(HERE, 'out')
N = 16  # Frames je Variante


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def base_shade(src):
    """Pro Pixel (alpha, Helligkeit 0..1, Randflag) aus Frame 0 der heutigen Textur."""
    im = Image.open(src).convert('RGBA').crop((0, 0, 16, 16))
    px = im.load()
    out = {}
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (0.3 * r + 0.55 * g + 0.15 * b) / 255
            out[(x, y)] = lum
    # Aufhellen/Abdunkeln normalisieren: Sparkle (Frame 0 Mitte) nicht als Facette zaehlen
    vals = sorted(out.values())
    lo, hi = vals[0], vals[int(len(vals) * 0.9)]
    return {k: max(0.0, min(1.0, (v - lo) / (hi - lo))) for k, v in out.items()}


def ramp_pick(ramp, v):
    return ramp[max(0, min(len(ramp) - 1, int(round(v * (len(ramp) - 1)))))]


def is_edge(shade, x, y):
    return any((x + dx, y + dy) not in shade for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def variant_a(shade):
    """Seelenglut: dunkelrote Facetten, in der Mitte pulsiert langsam eine tuerkise Seelenflamme (Ringwelle nach aussen)."""
    ramp = [hexc(c) for c in ('#1a0610', '#3a0a1e', '#6a1230', '#a02038', '#d2423a', '#f0803e', '#ffc070')]
    soul = [hexc(c) for c in ('#0e5a6a', '#1a98a8', '#46d8d0', '#a8f4ee', '#f0ffff')]
    cx, cy = 7.5, 7.5
    frames = []
    for t in range(N):
        ph = t / N
        img = Image.new('RGBA', (16, 16))
        p = img.load()
        for (x, y), v in shade.items():
            d = math.hypot(x - cx, y - cy)
            wave = 0.5 + 0.5 * math.cos(2 * math.pi * (ph - d / 9))  # 0..1, Welle laeuft nach aussen
            col = ramp_pick(ramp, v * 0.85 + 0.15 * wave * (1 - d / 9))
            if is_edge(shade, x, y):
                col = ramp[0] if v < 0.5 else ramp[1]
            glow = wave * max(0, 1 - d / 3.4)
            if glow > 0.3:
                col = soul[min(len(soul) - 1, int(glow * len(soul) * 1.15))]
            p[x, y] = col
        frames.append(img)
    return frames, {'frametime': 8, 'interpolate': True}


def variant_b(shade):
    """Aderstein: fast schwarz-violetter Obsidian-Stein, goldene Magma-Adern, ein Lichtpuls kriecht langsam die Adern entlang."""
    ramp = [hexc(c) for c in ('#0a0614', '#170c2a', '#26103e', '#391a58', '#52277a', '#6e3a98', '#8f56b8')]
    vein = [hexc(c) for c in ('#5a1608', '#a8340c', '#e8701a', '#ffb43a', '#fff0a0')]
    # Adern: ein verzweigter Pfad von links oben nach rechts unten, nur ueber vorhandene Pixel
    path = [(4, 4), (5, 5), (6, 5), (7, 6), (7, 7), (8, 8), (9, 8), (10, 9), (10, 10), (9, 11), (8, 12), (8, 13)]
    branch = [(7, 7), (6, 8), (5, 9), (5, 10), (4, 10)]
    branch2 = [(8, 6), (9, 5), (10, 5), (11, 6)]
    allv = [q for q in path + branch + branch2 if q in shade]
    order = {q: i for i, q in enumerate(path)}
    frames = []
    for t in range(N):
        pos = t / N * (len(path) + 6)
        img = Image.new('RGBA', (16, 16))
        p = img.load()
        for (x, y), v in shade.items():
            col = ramp_pick(ramp, v)
            if is_edge(shade, x, y):
                col = ramp[0] if v < 0.5 else ramp[2]
            p[x, y] = col
        for q in allv:
            i = order.get(q)
            if i is None:  # Seitenader haengt am naechsten Hauptpfad-Punkt
                i = 4 if q in branch else 3
            dist = (pos - i) % (len(path) + 6)
            lvl = 4 if dist < 1 else 3 if dist < 2 else 2 if dist < 3.5 else 1 if dist < 5 else 0
            p[q] = vein[lvl]
        frames.append(img)
    return frames, {'frametime': 8, 'interpolate': True}


def variant_c(shade):
    """Sternenschliff: karmesin-magenta Schliff mit kuehlem Tuerkis-Rand, ein Lichtband gleitet selten diagonal ueber den Stein, dazu ein kleines Glitzerkreuz."""
    ramp = [hexc(c) for c in ('#200610', '#40081e', '#741228', '#a82034', '#d43a40', '#f06a58', '#ffb8a0')]
    rim = [hexc(c) for c in ('#103a58', '#1f6a8c', '#3ea8c8')]
    frames = []
    for t in range(N):
        sweep = (t - 2) / 8 if 2 <= t < 10 else None  # Band nur in Frames 2..9, danach Ruhe
        img = Image.new('RGBA', (16, 16))
        p = img.load()
        for (x, y), v in shade.items():
            col = ramp_pick(ramp, v)
            if is_edge(shade, x, y):
                col = rim[0] if v < 0.5 else rim[1]
            elif any((x + dx, y + dy) not in shade for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                col = ramp[max(0, ramp.index(col) - 1)] if col in ramp else col
            if sweep is not None and not is_edge(shade, x, y):
                dd = (x + y) - (4 + sweep * 24)
                if abs(dd) < 0.9:
                    col = ramp[min(6, ramp.index(col) + 2)] if col in ramp else col
                elif abs(dd) < 1.8:
                    col = ramp[min(6, ramp.index(col) + 1)] if col in ramp else col
            p[x, y] = col
        # Glitzerkreuz oben links, erscheint zum Ende des Bands
        if t in (9, 10, 11, 12):
            s = (255, 255, 255, 255)
            p[6, 5] = s
            if t in (10, 11):
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if (6 + dx, 5 + dy) in shade:
                        p[6 + dx, 5 + dy] = rim[2] if t == 10 else hexc('#a8e8f8')
        frames.append(img)
    return frames, {'frametime': 6, 'interpolate': False}


def strip(frames):
    s = Image.new('RGBA', (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        s.paste(f, (0, i * 16))
    return s


def main():
    src = SRC
    if '--src' in sys.argv:
        src = sys.argv[sys.argv.index('--src') + 1]
    os.makedirs(OUT, exist_ok=True)
    shade = base_shade(src)
    for name, fn in (('a_seelenglut', variant_a), ('b_aderstein', variant_b), ('c_sternenschliff', variant_c)):
        frames, meta = fn(shade)
        strip(frames).save(os.path.join(OUT, f'nether_gem_{name}.png'))
        with open(os.path.join(OUT, f'nether_gem_{name}.png.mcmeta'), 'w') as f:
            json.dump({'animation': meta}, f, indent=2)
        print(name, len(frames), 'frames', meta)


if __name__ == '__main__':
    main()
