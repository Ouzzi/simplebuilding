"""Messuhr / Gauge als Zeigerinstrument (2026-09-29) fuer generate_textures.py.

- item/velocity_gauge_dial: Gehaeuse ohne Nadel (Ebene 0 der Nadelmodelle). Form wie eine Taschen-
  Stoppuhr: Kupfergehaeuse (Farben der alten Besitzer-Textur), oben ein Kupferknopf, dahinter wie
  bei der alten Textur Amethyst: ein dunkles Amethystglas mit Schimmer oben links; Skala als
  270-Grad-Bogen aus Amethystmarken, unten offen: Nullmarke unten links, Mittelmarke oben, rote Zone
  unten rechts (wie im HUD).
- item/velocity_gauge_needle_00..16: nur die Nadel mit Nabe (Ebene 1). Bild 0 = Stillstand (unten
  links), 16 = Vollausschlag (unten rechts); dazwischen gleichmaessig ueber 270 Grad. Welches Bild
  gezeigt wird, entscheidet simplebuilding:gauge_needle (Tempo des Halters, Wurzelskala).
- item/velocity_gauge: Ruhebild (Bild 0) fuer Rezeptanzeigen und das Wiki.

Nur fuer den Hauptbaum (26.2/26.3); die 1.21.11-Kopie behaelt ihre alte Textur bis zum Port-Run.
"""
import math

from PIL import Image


def _hex(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# Scheibe 13 x 13 (Spalten 1..13, Zeilen 2..14) um die Nabe (7, 8); darueber der Knopf in Zeile 1.
# Rechts bleibt Luft, unten ein Pixel: das Item reicht nirgends an den Rand. Die Kreisecken sind nur
# ueber Eck verbunden und bleiben frei (Konturregel des Besitzers).
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
DISC_ORIGIN = (1, 2)
PIVOT = (7, 8)
# Kupferknopf oben (Krone der Stoppuhr): Umriss und Glanz.
CROWN = {(6, 1): "k", (7, 1): "K", (8, 1): "k"}
GLASS = {(5, 4): "G", (4, 5): "g"}

PAL = {
    "o": "#3f1f13",  # Umriss oben/links (alte Textur)
    "O": "#190c07",  # Umriss unten/rechts
    "C": "#d28366",  # Kupfer hell
    "c": "#a85232",  # Kupfer
    "d": "#6d3520",  # Kupfer dunkel
    "k": "#6d3520",  # Knopf Rand
    "K": "#d28366",  # Knopf Glanz
    "F": "#2e2340",  # Amethystglas
    "f": "#241b33",  # Glas Schatten (unten rechts)
    "g": "#5d4a80",  # Glasschimmer
    "G": "#8e74bf",  # Glasschimmer hell
    "t": "#a58bd6",  # Skalenmarke (Amethyst)
    "r": "#e0503a",  # rote Zone
}

NEEDLE_LEN = 3.6
NEEDLE_COL = _hex("#ff7f3f")
NEEDLE_TIP = _hex("#ffd2a0")
HUB_COL = _hex("#d28366")
FRAMES = 17


def angle(frame):
    """Nadelwinkel in Grad (mathematisch, 0 = rechts): 225 bei Bild 0, 90 oben, -45 bei Bild 16."""
    return 225.0 - 270.0 * frame / (FRAMES - 1)


def _cells():
    ox, oy = DISC_ORIGIN
    outline, inside = set(), set()
    for j, row in enumerate(DISC):
        xs = [i for i, ch in enumerate(row) if ch == "#"]
        for i in xs:
            outline.add((ox + i, oy + j))
    for j, row in enumerate(DISC):
        xs = [i for i, ch in enumerate(row) if ch == "#"]
        if len(xs) < 2:
            continue
        for i in range(xs[0], xs[-1] + 1):
            if (ox + i, oy + j) not in outline:
                inside.add((ox + i, oy + j))
    for j in (1, len(DISC) - 2):
        xs = [i for i, ch in enumerate(DISC[j]) if ch == "#"]
        for i in range(xs[1] + 1, xs[2]):
            inside.add((ox + i, oy + j))
    cells = {p: "outline" for p in outline}
    for (x, y) in inside:
        n4 = [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
        cells[(x, y)] = "ring" if any(q in outline for q in n4) else "face"
    return cells


def _rim(a_deg, r=4.6):
    cx, cy = PIVOT
    a = math.radians(a_deg)
    return (int(round(cx + r * math.cos(a))), int(round(cy - r * math.sin(a))))


def scale_marks():
    """Skalenmarken am Rand des Zifferblatts: {Pixel: 't' | 'r'}."""
    cells = _cells()
    marks = {}
    for a in (225.0, 157.5, 90.0, 22.5):
        p = _rim(a)
        if cells.get(p) == "face":
            marks[p] = "t"
    # rote Zone: die letzten Achtel des Bogens (wie im HUD ab 80 %)
    for a in (-10.0, -30.0, -45.0):
        p = _rim(a)
        if cells.get(p) == "face":
            marks[p] = "r"
    return marks


def dial():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cx, cy = PIVOT
    for (x, y), kind in _cells().items():
        side = (x - cx) + (y - cy)
        if kind == "outline":
            ch = "o" if side < 1 else "O"
        elif kind == "ring":
            ch = "C" if side <= -3 else ("d" if side >= 3 else "c")
        else:
            ch = "f" if side >= 4 else "F"
        px[x, y] = _hex(PAL[ch])
    for p, ch in CROWN.items():
        px[p] = _hex(PAL[ch])
    for p, ch in GLASS.items():
        px[p] = _hex(PAL[ch])
    for p, ch in scale_marks().items():
        px[p] = _hex(PAL[ch])
    return img


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
    """Pixel der Nadel ohne die Nabe, von innen nach aussen (gerade Bresenham-Treppe)."""
    cx, cy = PIVOT
    a = math.radians(angle(frame))
    return _line(cx, cy, int(round(cx + NEEDLE_LEN * math.cos(a))), int(round(cy - NEEDLE_LEN * math.sin(a))))


def needle_image(frame):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    pixels = needle_pixels(frame)
    for p in pixels[:-1]:
        px[p] = NEEDLE_COL
    px[pixels[-1]] = NEEDLE_TIP
    px[PIVOT] = HUB_COL
    return img


def rest_image():
    img = dial()
    img.alpha_composite(needle_image(0))
    return img


def gauge_textures():
    tex = {"item/velocity_gauge_dial.png": dial(), "item/velocity_gauge.png": rest_image()}
    for f in range(FRAMES):
        tex[f"item/velocity_gauge_needle_{f:02d}.png"] = needle_image(f)
    return tex
