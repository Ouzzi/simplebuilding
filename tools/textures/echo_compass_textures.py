"""Echo-Kompass: 32 Nadelbilder (item/echo_compass_00..31, gleiche Zaehlung wie Vanillas
compass_XX: 16 = Nadel nach oben, im Uhrzeigersinn weiter) und drei Riss-Stufen
(item/echo_compass_cracked_0..2, 0 = leer, 2 = fast repariert) fuer generate_textures.py.

Form: Gehaeuse wie der Kompass (runder Deckel in Aufsicht, darunter das Seitenband), Farben
vom Bergungskompass (Sculk-Schwarz, Tiefenblau, leuchtendes Echo-Cyan). Die Nadel wird je
Winkel gerastert; Kopf cyan mit hellem Spitzenpixel, Schwanz dunkel. Das Gehaeuse ist die
Pixelkarte ECHO_COMPASS_BODY; die Nadel sitzt auf ECHO_PIVOT.
"""
import math

from PIL import Image


def _hex(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# Gehaeuse ohne Nadel. Licht oben links: Deckelrand oben hell (Metall wie beim Kompass),
# unten/rechts der dunkelste Ton. Das Seitenband traegt die Sculk-Adern des Bergungskompasses.
ECHO_COMPASS_BODY = [
    "................",
    ".....oooooo.....",
    "...ooLWLMLmoo...",
    "..oLMdsssssdDO..",
    ".oLdsFfFFFFFsdO.",
    ".oMdFfFFFFFFFdO.",
    ".oMdFFFFFFFFFdO.",
    ".omdFFFFFFFFFdO.",
    ".OmDdFFFFFFFdDO.",
    ".OLWmddFFddmDDO.",
    ".OLLMmDddDmmDkO.",
    "..OgkMmmmmDkgO..",
    "...OOkgGGgkOO...",
    ".....OOOOOO.....",
    "................",
    "................",
]

ECHO_PAL = {
    "o": "#2b3a3d",  # Umriss oben (heller, wie beim Kompass)
    "O": "#0d1417",  # Umriss unten/rechts, Sculk-Schwarz
    "W": "#ffffff",  # Glanzpixel
    "L": "#d8d8d8",  # Deckelrand hell
    "M": "#a8a8a8",
    "m": "#828282",
    "D": "#5e5e5e",
    "d": "#3a4447",  # Innenkante des Deckels
    "F": "#0b1a1d",  # Zifferblatt (tiefes Sculk-Blau)
    "f": "#15343a",  # Spiegelung auf dem Glas oben links
    "s": "#105257",  # Echo-Schimmer am unteren Glasrand
    "k": "#082e36",  # Seitenband
    "K": "#0d4552",  # Seitenband heller
    "g": "#138e99",  # Sculk-Ader
    "G": "#29dfeb",  # Sculk-Ader leuchtend
}

ECHO_PIVOT = (8, 6)
NEEDLE_RX = 4.2
NEEDLE_RY = 2.3
NEEDLE_TAIL = 0.42

NEEDLE_TIP = _hex("#9ffaff")
NEEDLE_HEAD = _hex("#29dfeb")
NEEDLE_PIN = _hex("#a8a8a8")
NEEDLE_TAIL_COL = _hex("#2f6b72")

# Riss-Stufen: Rissfarbe, Pfade (ab Stufe 0 alle), Nadel- und Aderfarben.
CRACK_LIGHT = _hex("#d7f2f5")
CRACK_DARK = _hex("#6f9aa0")
# Einschlag rechts oben im Glas, Hauptriss nach unten, Seitenriss nach links oben, im leeren Zustand
# ein zweiter Riss links und ein Sprung im Seitenband. Tripel (x, y, hell?). Die Nadel haengt nach
# links unten und kreuzt keinen Riss.
CRACK_MAIN = [(11, 4, True), (12, 5, False), (11, 6, True), (12, 7, False)]
CRACK_SIDE = [(10, 4, False), (9, 3, True)]
CRACK_LEFT = [(5, 4, True), (5, 5, False), (6, 5, True)]
CRACK_BAND = [(4, 10, True), (4, 11, False), (5, 12, True)]
CRACK_STAGES = {
    0: CRACK_MAIN + CRACK_SIDE + CRACK_LEFT + CRACK_BAND,
    1: CRACK_MAIN[:3] + CRACK_SIDE[:1],
    2: CRACK_MAIN[:2],
}
# Leer: Nadel haengt kraftlos nach unten (Bild 2), Adern erloschen; dann kehrt das Leuchten zurueck.
CRACKED_NEEDLE_FRAME = 2
CRACKED_COLOURS = {
    0: {"head": "#35565b", "tip": "#4d7479", "g": "#0b3a44", "G": "#0f4d59", "F": "#08141a", "f": "#0f262b"},
    1: {"head": "#177f89", "tip": "#2fb3be", "g": "#0f6d77", "G": "#169aa6", "F": "#0a181b", "f": "#132f35"},
    2: {"head": "#22c3cf", "tip": "#7de9f0", "g": "#128593", "G": "#22c6d2", "F": "#0b1a1d", "f": "#15343a"},
}


def needle_pixels(frame):
    """Kopf- und Schwanzpixel der Nadel fuer Bild ``frame`` (0..31, 16 = oben)."""
    a = 2 * math.pi * (frame - 16) / 32
    dx, dy = math.sin(a), -math.cos(a)
    px, py = ECHO_PIVOT
    head = []
    for i in range(1, 41):
        t = i / 40
        p = (int(math.floor(px + 0.5 + dx * NEEDLE_RX * t)), int(math.floor(py + 0.5 + dy * NEEDLE_RY * t)))
        if p != (px, py) and p not in head:
            head.append(p)
    tail = []
    for i in range(1, 41):
        t = i / 40 * NEEDLE_TAIL
        p = (int(math.floor(px + 0.5 - dx * NEEDLE_RX * t)), int(math.floor(py + 0.5 - dy * NEEDLE_RY * t)))
        if p != (px, py) and p not in tail and p not in head:
            tail.append(p)
    return head, tail


def body(pal=None):
    colours = dict(ECHO_PAL)
    if pal:
        colours.update(pal)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(ECHO_COMPASS_BODY):
        if len(row) != 16:
            raise ValueError(f"echo_compass: Zeile {y} hat {len(row)} Zeichen")
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            px[x, y] = _hex(colours[ch])
    return img


def frame_image(frame, head_col=NEEDLE_HEAD, tip_col=NEEDLE_TIP, pal=None):
    img = body(pal)
    px = img.load()
    head, tail = needle_pixels(frame)
    for p in tail:
        px[p] = NEEDLE_TAIL_COL
    for p in head[:-1]:
        px[p] = head_col
    px[head[-1]] = tip_col
    px[ECHO_PIVOT] = NEEDLE_PIN
    return img


def cracked_image(stage):
    c = CRACKED_COLOURS[stage]
    img = frame_image(CRACKED_NEEDLE_FRAME, _hex(c["head"]), _hex(c["tip"]),
                      {"g": c["g"], "G": c["G"], "F": c["F"], "f": c["f"]})
    px = img.load()
    for x, y, light in CRACK_STAGES[stage]:
        px[x, y] = CRACK_LIGHT if light else CRACK_DARK
    return img


def echo_compass_textures():
    tex = {}
    for f in range(32):
        tex[f"item/echo_compass_{f:02d}.png"] = frame_image(f)
    for stage in CRACK_STAGES:
        tex[f"item/echo_compass_cracked_{stage}.png"] = cracked_image(stage)
    return tex
