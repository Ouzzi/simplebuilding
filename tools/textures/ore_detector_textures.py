"""Erzdetektor als Kompass (2026-09-28) fuer generate_textures.py.

- item/ore_detector_dial: das Gehaeuse ohne Nadel (Ebene 0 der Nadelmodelle), runde Aufsicht wie der
  alte Detektor: Sculk-Gehaeuse, ein Ring Echo-Metall, Goldmarke oben (Goldkern im Rezept) und
  goldener Zapfen in der Mitte, dunkles Sonarfeld.
- item/ore_detector_needle_00..31: nur die Nadel (Ebene 1), gleiche Zaehlung wie Vanillas
  compass_XX (16 = Nadel nach oben, im Uhrzeigersinn weiter). Die Nadel ist hell gemalt; das
  Modell toent sie mit der Farbe aus custom_model_data (OreDetectorItem.RESONANCE_COLORS), sie
  wird also amethystlila und heller, je naeher das Erz ist. Mit ihr getoent wird ein schwacher
  Resonanzring im Sonarfeld - er glueht mit.
- item/ore_detector: Ruhebild ohne Ziel (Nadel nach oben, gedaempft).
"""
import math

from PIL import Image


def _hex(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# Aufsicht: 13er-Scheibe um den Zapfen (7, 7), Spalten 1..13 / Zeilen 1..13 - rechts und unten bleibt
# Luft, das Item reicht nirgends an den Rand. DISC ist der Umriss einer Vanilla-artigen Pixelscheibe; die
# Kreisecken sind nur ueber Eck verbunden und bleiben frei (Konturregel des Besitzers). Daraus werden
# Umriss (o hell oben links / O dunkel unten rechts), ein Ring Echo-Metall (B / b / d) und das Sonarfeld
# (F, Glasschimmer f oben links) abgeleitet; die Goldmarke G sitzt oben im Ring (Goldkern im Rezept).
DISC = [
    "....#####....",
    "..##.....##..",
    ".#.........#.",
    ".#.........#.",
    "#...........#",
    "#...........#",
    "#...........#",
    "#...........#",
    "#...........#",
    ".#.........#.",
    ".#.........#.",
    "..##.....##..",
    "....#####....",
]
DISC_ORIGIN = (1, 1)
GOLD_MARK = [(7, 2)]
GLASS = [(5, 4), (4, 5), (6, 4)]
# Echo-Schimmer am unteren Rand des Sonarfelds (wie beim Bergungskompass).
ECHO = [(6, 11), (8, 11), (10, 10), (11, 9)]

PAL = {
    "o": "#1c2c30",  # Umriss oben/links
    "O": "#070b0e",  # Umriss unten/rechts
    "B": "#4f7178",  # Metallring hell
    "b": "#2a454b",  # Metallring
    "d": "#15262c",  # Metallring dunkel
    "G": "#fad64a",  # Goldmarke Glanz
    "g": "#b26411",  # Goldmarke Schatten
    "F": "#08161a",  # Sonarfeld
    "f": "#143139",  # Glasschimmer
    "s": "#0f4a52",  # Echo-Schimmer
}

PIVOT = (7, 7)
PIVOT_COL = _hex("#e9b115")
NEEDLE_LEN = 4.2
NEEDLE_TAIL = 1.0

NEEDLE_TIP = _hex("#ffffff")
NEEDLE_HEAD = _hex("#e4d8f2")
NEEDLE_TAIL_COL = _hex("#5c5664")
RING_COL = _hex("#8a8294")  # Resonanzmarken, getoent: fern kaum sichtbar, nah gluehend
# Resonanzmarken: vier Striche in den Himmelsrichtungen am Rand des Sonarfelds (wie die Marken eines
# Kompasses); sie tragen dieselbe Toenung wie die Nadel und gluehen mit, je naeher das Erz ist.
RING_MARKS = [(7, 3), (11, 7), (7, 11), (3, 7)]

# Gedaempfte Ruhefarbe (Nadel ohne Ziel) - dunkler und grauer als die fernste Resonanzstufe.
IDLE_TINT = (0x4E, 0x3F, 0x63)


def _line(x0, y0, x1, y1):
    """Bresenham von (x0, y0) nach (x1, y1), ohne den Startpunkt - gerade Treppen, keine Haken."""
    out = []
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x1 > x0 else -1), (1 if y1 > y0 else -1)
    err = dx + dy
    x, y = x0, y0
    while (x, y) != (x1, y1):
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x += sx
        if e2 <= dx:
            err += dx
            y += sy
        out.append((x, y))
    return out


def needle_pixels(frame):
    """Kopf- und Schwanzpixel der Nadel fuer Bild ``frame`` (0..31, 16 = oben)."""
    a = 2 * math.pi * (frame - 16) / 32
    dx, dy = math.sin(a), -math.cos(a)
    px, py = PIVOT
    head = _line(px, py, int(round(px + dx * NEEDLE_LEN)), int(round(py + dy * NEEDLE_LEN)))
    tail = [p for p in _line(px, py, int(round(px - dx * NEEDLE_TAIL)), int(round(py - dy * NEEDLE_TAIL)))
            if p not in head]
    return head, tail


def _cells():
    """Zelle -> Ebene ('outline', 'ring', 'field') der Scheibe."""
    ox, oy = DISC_ORIGIN
    outline = set()
    inside = set()
    for j, row in enumerate(DISC):
        xs = [i for i, c in enumerate(row) if c == "#"]
        for i in xs:
            outline.add((ox + i, oy + j))
    for j, row in enumerate(DISC):
        xs = [i for i, c in enumerate(row) if c == "#"]
        if len(xs) < 2:
            continue
        for i in range(xs[0], xs[-1] + 1):
            if (ox + i, oy + j) not in outline:
                inside.add((ox + i, oy + j))
    # Zeile 1 und 13: der Umriss ist dort ein Balken; darunter/darueber liegt Innenflaeche.
    for j in (1, len(DISC) - 2):
        row = DISC[j]
        xs = [i for i, c in enumerate(row) if c == "#"]
        for i in range(xs[1] + 1, xs[2]):
            inside.add((ox + i, oy + j))
    cells = {p: "outline" for p in outline}
    for (x, y) in inside:
        n4 = [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
        cells[(x, y)] = "ring" if any(q in outline for q in n4) else "field"
    return cells


def dial():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cx, cy = PIVOT
    for (x, y), kind in _cells().items():
        side = (x - cx) + (y - cy)
        if kind == "outline":
            ch = "o" if side < 1 else "O"
        elif kind == "ring":
            ch = "B" if side <= -3 else ("d" if side >= 3 else "b")
        else:
            ch = "F"
        px[x, y] = _hex(PAL[ch])
    for p in GLASS:
        px[p] = _hex(PAL["f"])
    for p in ECHO:
        px[p] = _hex(PAL["s"])
    for p in GOLD_MARK:
        px[p] = _hex(PAL["G"])
    px[PIVOT] = PIVOT_COL
    return img


def _field(x, y):
    return _cells().get((x, y)) == "field" and (x, y) not in GLASS and (x, y) not in ECHO


def ring_pixels():
    return [p for p in RING_MARKS if _field(*p)]


def needle_image(frame):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    head, tail = needle_pixels(frame)
    for p in ring_pixels():
        px[p] = RING_COL
    for p in tail:
        px[p] = NEEDLE_TAIL_COL
    for p in head[:-1]:
        px[p] = NEEDLE_HEAD
    px[head[-1]] = NEEDLE_TIP
    return img


def tinted(img, rgb):
    out = img.copy()
    px = out.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (r * rgb[0] // 255, g * rgb[1] // 255, b * rgb[2] // 255, a)
    return out


def idle_image():
    img = dial()
    img.alpha_composite(tinted(needle_image(16), IDLE_TINT))
    return img


def ore_detector_textures():
    tex = {"item/ore_detector_dial.png": dial(), "item/ore_detector.png": idle_image()}
    for f in range(32):
        tex[f"item/ore_detector_needle_{f:02d}.png"] = needle_image(f)
    return tex
