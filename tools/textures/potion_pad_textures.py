"""Trank-Pad I-III und Lohenkopf (Besitzer 2026-09-28) fuer generate_textures.py.

Trank-Pads: der Besitzer wollte die alten Flypad-Bilder (block/flypad.png, reinforced_flypad.png,
stellar_flypad.png - seit den Enderit-Flypads unbenutzt und dafuer aufgehoben) in der Netherit-Palette
wiederverwenden. Die grauen Steinpixel gehen nach ihrer Helligkeit auf die Netherit-Rampe der anderen
Netherit-Pads (NETHERITE_TWEAK_PAL), die blauen Adern auf eine Glut-Rampe je Stufe (I Lohen-Orange,
II Enderit-Violett, III Gold wie das stellare Flypad), die hellen Funkelsterne des stellaren Bildes
bleiben hell. In der Mitte steht eine kleine Trankflasche (Korken, Glas, Fluessigkeit in der
Stufenfarbe); die Farbe des gespeicherten Tranks zeigen im Spiel die Partikel.

Lohenkopf: neue Pixelkunst im Mob-Kopf-Raster (64x32, Kopfwuerfel 8x8x8 bei UV 0,0 wie Vanillas
Creeper-/Skelettkopf): Glutgesicht von Weissgelb oben nach Rostbraun unten, zwei Augenpaare wie die
Lohe (weisser Rand aussen, schwarze Pupille innen), dunkle Brauenkante darueber, Oberseite heiss und
fleckig, Unterseite verkohlt.
"""
import colorsys
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
OLD_FLYPADS = os.path.join(REPO, "src", "main", "resources", "assets", "simplebuilding", "textures", "block")


def _hex(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# Netherit-Rampe (dunkel -> hell) wie NETHERITE_TWEAK_PAL in generate_textures.py
NETHERITE_RAMP = [_hex(c) for c in ("#161213", "#2c2627", "#3b3536", "#433d3f", "#4a4547", "#524d50",
                                    "#5a565a", "#625e62", "#7a7579", "#8e898d")]

# Stufen: Quelle (altes Flypad), Adern-Rampe dunkel -> hell, Flaschen-Fluessigkeit (L Grund, l Glanz, d Schatten)
STAR = "#f7e2a0"  # Funkelsterne des stellaren Bildes: warmes Weissgold

TIERS = {
    "potion_pad": {
        "source": "flypad.png",
        "veins": ["#2e1a14", "#44231a", "#5e2e1c", "#7a3c20", "#9a4c24"],
        "liquid": {"L": "#f0761c", "l": "#ffd35a", "d": "#b3470f"},
    },
    "reinforced_potion_pad": {
        "source": "reinforced_flypad.png",
        "veins": ["#241a30", "#31213f", "#402a55", "#53366e", "#6a4690"],
        "liquid": {"L": "#a454e0", "l": "#e2b8ff", "d": "#6a2fa3"},
    },
    "infused_potion_pad": {
        "source": "stellar_flypad.png",
        "veins": ["#2e2618", "#43361e", "#5c4a24", "#7a632c", "#9c8036"],
        "liquid": {"L": "#f2b53a", "l": "#fff3b0", "d": "#b57c1c"},
    },
}

# Trankflasche, 6x8, Spalten 5..10, Zeilen 4..11 ('.' = Pad bleibt sichtbar). c/C Korken, g/G Glas
# (Schatten/Licht), L/l/d Fluessigkeit. Die Ecken der Schulter bleiben frei (keine dunkle Eckfuellung).
FLASK_AT = (5, 4)
FLASK = [
    "..Cc..",
    "..Gg..",
    ".GlLg.",
    "GlLLLg",
    "GLLLLg",
    "GLLLdg",
    "gLLddg",
    ".gggg.",
]
FLASK_PAL = {"C": "#a8744a", "c": "#6e4526", "G": "#d9e3ee", "g": "#8f9db3"}


def _luma(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def _ramp(ramp, t):
    t = max(0.0, min(1.0, t))
    return ramp[min(len(ramp) - 1, int(t * len(ramp)))]


def _recolour(src, veins):
    """Steinpixel -> Netherit-Rampe, blaue Adern -> Glut-Rampe, sehr helle Sternpixel bleiben hell."""
    out = Image.new("RGBA", src.size)
    vein_ramp = [_hex(c) for c in veins]
    for y in range(src.height):
        for x in range(src.width):
            p = src.getpixel((x, y))
            if p[3] == 0:
                out.putpixel((x, y), p)
                continue
            h, s, v = colorsys.rgb_to_hsv(p[0] / 255, p[1] / 255, p[2] / 255)
            lum = _luma(p) / 255
            if v > 0.62 and s < 0.25:
                # Funkelstern (stellar): hell, leicht in die Adernfarbe getoent
                out.putpixel((x, y), tuple(int(0.35 * 235 + 0.65 * c) for c in _hex(STAR)[:3]) + (255,))
            elif s > 0.22 and 0.45 < h < 0.70:
                # blaue Ader
                out.putpixel((x, y), _ramp(vein_ramp, (lum - 0.16) / 0.24))
            else:
                out.putpixel((x, y), _ramp(NETHERITE_RAMP, (lum - 0.12) / 0.26))
    return out


def _flask(img, liquid):
    pal = dict(FLASK_PAL)
    pal.update(liquid)
    ox, oy = FLASK_AT
    for dy, row in enumerate(FLASK):
        for dx, ch in enumerate(row):
            if ch != ".":
                img.putpixel((ox + dx, oy + dy), _hex(pal[ch]))
    return img


def potion_pad_textures():
    tex = {}
    for name, tier in TIERS.items():
        src = Image.open(os.path.join(OLD_FLYPADS, tier["source"])).convert("RGBA")
        tex[f"block/{name}.png"] = _flask(_recolour(src, tier["veins"]), tier["liquid"])
    tex["entity/blaze_head.png"] = blaze_head_texture()
    return tex


# ---------------------------------------------------------------------------------------------
# Lohenkopf
# ---------------------------------------------------------------------------------------------
BLAZE_PAL = {
    "W": "#fffbd8", "Y": "#fff04a", "y": "#ffd02e", "O": "#fca51c", "o": "#e8820e",
    "R": "#c35d08", "r": "#94400a", "B": "#6b2a06", "D": "#4a1a05", "E": "#2a0c06", "e": "#fff8e0",
}
# Gesicht (vorne): heiss oben, Brauenkante in Zeile 2, Augen in Zeile 3 (Pupillen innen), Glut unten
BLAZE_FRONT = [
    "YWYyYWYy",
    "yYyOyyYO",
    "OrrOOrrO",
    "oeEoOEeo",
    "oOooRoOR",
    "RoRrRRor",
    "rRrBrRBr",
    "BrBDBrBD",
]
BLAZE_SIDE = [
    "yYWyYyWY",
    "OyyOyYyO",
    "oOyoOOyo",
    "RoOooRoO",
    "oRRoRoRR",
    "rRrRRrRr",
    "BrRBrBrB",
    "DBrDBDBD",
]
BLAZE_BACK = [
    "yOyYyOyY",
    "OoOyOoOy",
    "oRoOoRoO",
    "RoRRoRRo",
    "rRrRrRrR",
    "BrBrRBrB",
    "DBDBrDBD",
    "DDBDDBDD",
]
BLAZE_TOP = [
    "yYWYyYWy",
    "YWYyYWYY",
    "yYYWYyYO",
    "OyWYYWyy",
    "yYyWYYyO",
    "YWYyWYyy",
    "yyYYyWYO",
    "OyOyyOyo",
]
BLAZE_BOTTOM = [
    "rBrDBrBr",
    "BDBBrDBD",
    "rBDrBBDr",
    "DBrDDBrB",
    "BrBDBrDD",
    "DBDrBDBr",
    "rDBBDrBD",
    "BrDBrBDB",
]
# (Karte, UV-Ecke) wie Vanillas Mob-Kopf-Raster
BLAZE_FACES = [
    (BLAZE_TOP, (8, 0)), (BLAZE_BOTTOM, (16, 0)),
    (BLAZE_SIDE, (0, 8)), (BLAZE_FRONT, (8, 8)), (BLAZE_SIDE, (16, 8)), (BLAZE_BACK, (24, 8)),
]


def blaze_head_texture():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for rows, (ox, oy) in BLAZE_FACES:
        if len(rows) != 8 or any(len(r) != 8 for r in rows):
            raise ValueError("Lohenkopf: jede Seite muss 8x8 sein")
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                img.putpixel((ox + x, oy + y), _hex(BLAZE_PAL[ch]))
    return img
