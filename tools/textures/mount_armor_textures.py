"""Enderit-Pferde- und -Nautilusruestung (Besitzer 2026-09-28) fuer generate_textures.py.

Zwei Items eine Stufe ueber Vanillas Netherit-Pferde-/-Nautilusruestung (Schmiedetisch: Enderit-
Aufwertung + Enderit-Barren). Neue Pixelkunst in der Enderit-Rampe der Ausruestung (Variante B von
ENDERITE_GEAR_VARIANTS: Violett von #1c0a33 bis #a57de9, Adern #f4d2ff, Glimmen #c77dff):

- Item-Symbole: Silhouette und Lage wie Vanillas Netherit-Symbole (Pferd: Rumpf mit Kopf rechts oben;
  Nautilus: isometrische Schalenkappe, aber einen Pixel vom rechten Rand weg), Kontur oben/links
  #2d1656, unten/rechts #1c0a33, diagonale Konturstufen ohne dunkles Eckpixel. Der Sattel der
  Pferderuestung ist Netherit (das Material, aus dem sie geschmiedet wird) mit Enderit-Saum.
- Getragene Ebenen: exakt Vanillas UV-Maske (dieselben Pixel deckend), aber selbst gemalt: jede
  Modellflaeche (UV-Rechteck aus AbstractEquineModel / NautilusArmorModel) bekommt Licht von links
  oben, Plattenkanten, Plattenfugen alle vier Pixel und die diagonalen Ender-Adern wie die getragene
  Enderit-Ruestung (humanoid/enderite.png). Pferd: Netherit-Satteldecke mit Glimm-Saum, Ender-Perle
  auf den Seitenlappen, Glimm-Schnallen, Stirnkamm mit Perle. Nautilus: Schalenkiel auf Ober- und
  Vorderseite, Ender-Perlen auf den Schalenseiten, Nietenband unten, dunkles Schaleninneres.

Die Karten unten sind nur Materialkarten (welches Pixel Platte, Sattel, Riemen, Schnalle ist); sie
wurden einmal aus der Deckung von Vanillas netherite.png abgeleitet, damit das Modell passt. Alle
Farben entstehen hier im Code - das Modul liest zur Laufzeit keine Vanilla-Dateien.
"""
from PIL import Image


def _hex(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# Enderit-Rampe der Ausruestung (ENDERITE_GEAR_VARIANTS["B"]["head"]), Index 0 dunkel -> 7 hell
ENDER = ["#1c0a33", "#2d1656", "#3e2173", "#4a2888", "#55309a", "#6d45b8", "#8e63dc", "#a57de9"]
VEIN = "#f4d2ff"   # Ender-Ader (ENDERITE_GEAR_VARIANTS["B"]["vein"])
GLOW = "#c77dff"   # Glimmen (ENDERITE_GEAR_VARIANTS["B"]["glow"])
# Netherit (Sattel, Riemen) wie NETHERITE_RAMP in potion_pad_textures.py, dunkel -> hell
NETHER = ["#161213", "#2c2627", "#3b3536", "#4a4547", "#5a565a", "#7a7579"]


# ---------------------------------------------------------------------------
# Item-Symbole (16x16): o/O Kontur oben-links/unten-rechts, 2..7 Enderit-Rampe, v Ader, g Glimmen,
# n a b c d Netherit (Sattel) dunkel -> hell
# ---------------------------------------------------------------------------
ICON_PAL = {
    "o": ENDER[1], "O": ENDER[0],
    "1": ENDER[1], "2": ENDER[2], "3": ENDER[3], "4": ENDER[4], "5": ENDER[5], "6": ENDER[6], "7": ENDER[7],
    "v": VEIN, "g": GLOW,
    "n": NETHER[1], "a": NETHER[2], "b": NETHER[3], "c": NETHER[4], "d": NETHER[5],
}

HORSE_ICON = [
    "................",
    "................",
    "................",
    "...........o....",
    "...........o7O..",
    "..........o6.5O.",
    ".........o6g54O.",
    ".........o54OOO.",
    "..ooonnnn65O....",
    ".o766ddcb54O....",
    ".o665dccb4g4O...",
    ".o554ccbb4v3O...",
    ".o443vggg332O...",
    ".OOOOOOOOOOOO...",
    "................",
    "................",
]

NAUTILUS_ICON = [
    "................",
    ".......oo.......",
    ".....oo76oo.....",
    "...oo762265oo...",
    ".oo6662vg2655oO.",
    ".o56662g625543O.",
    ".o554662254332O.",
    ".o554446533322O.",
    ".o554g4db33322O.",
    ".o54v44gb22222O.",
    ".o3g333da11111O.",
    ".OO5444db3322OO.",
    "...OO44gb32OO...",
    ".....OOdaOO.....",
    ".......OO.......",
    "................",
]


def render_icon(rows, pal):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), "Symbolkarte muss 16x16 sein"
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), _hex(pal[ch]))
    return img


# Materialkarten der getragenen Ebenen. Pferd (ab Zeile 13, 64 breit): a Platte, s Satteldecke,
# k Riemen, b Schnalle. Nautilus (Zeile 0..53 der 128x128-Flaeche, 64 breit): # deckend; das Material
# ergibt sich dort aus der Modellflaeche.
HORSE_MAP = [
    ".......aaaaaa...................................................",
    ".......aaaaaa...................................................",
    ".......aaaaaa...................................................",
    ".......aaaaaa...................................................",
    ".......aaaaaa...................................................",
    ".......aaaaaa......aaaa.........................................",
    ".......aaaaaa......aaaa.........................................",
    "aaaaaaaaaaaaaaaaaaaaaaaaaa......................................",
    "aaa...aaaaaaaa...aaaaaaaaa..........................aaaa........",
    "aaaaaaaaaaaaaaaaaaaaaaaaaa..........................aaaa........",
    "aaaaaa........aaaaaaaaaaaa..........................aaaa........",
    "a.................aaaaaaaa..........................aaaa........",
    ".....aaaa.......................................aaaaaaaaaaaaaaaa",
    ".....aaaa.......................................aaaaaaaaaaaaaaaa",
    ".....aaaa.......................................aaaaaaaaaaaaaaaa",
    ".....aaaa.......................................aaaaaaaaaaaaaaaa",
    ".....aaaa.......................................aaaaaaaaaaaaa..a",
    "aaaaa....aaaaa..................................aaaaaaaaaaaa....",
    "aaaa......aaaa..................................................",
    "aaa........aaa........aaaaaaaaaa................................",
    "......................aaaaaaaaaa................................",
    "......................aaaaaaaaaa................................",
    ".......aaaa...........aaaaaaaaaa................................",
    ".......aaaa...........aaaaaaaaaa................................",
    ".......aaaa...........kkkkkkkkkkkkbbbkkkkk......................",
    ".......aaaa...........sssssssssskkbbbkkkkk......................",
    ".......aaaa...........sssssssssskkbbbkkkkk...............a..a...",
    ".......aaaa...........ssssssssss.........................a..a...",
    ".......aaaa...........ssssssssss.........................a..a...",
    "aaaa..........aaaaaaaassssssssss.........................a..a...",
    "aaaa..........aaaaaaaassssssssss.........................a..a...",
    "aaaa..........aaaaaaaasssssssssskkbbbkkkkk...............a..a...",
    "aaaa..........aaaaaaaasssssssssskkbbbkkkkk...............a..a...",
    "aaaa..........aaaaaaaasssssssssskkbbbkkkkk...............a..a...",
    "aaaa..........aaaaaaaakkkkkkkkkk.........................a..a...",
    "aaaa..........aaaaaaaa...................................a..a...",
    "aaaa..........aaaaaaaa...................................a..a...",
    "aaaa..........aaaaaaaa...................................a..a...",
    "aaaa..........aaaaaaaa...................................a..a...",
    ".........................................................a..a...",
    "................................................................",
    "aaaaaksssssssssk......................kssssssssskaaaaaaaaaaaaaaa",
    "aaaaaksssssssssk......................kssssssssskaaaaaaaaaaaaaaa",
    "aaaaakssssssssskkaaaaaaa......aaaaaaakkssssssssskaaaaaaaaaaaaaaa",
    "aaaaakssssssssskkaaaaaaa......aaaaaaakkssssssssskaaaaaaaaaaaaaaa",
    "aaaaakssssssssskaaaaaaaaa....aaaaaaaaakssssssssskaaaaaaaaaaaaaaa",
    "aaaaakssssssssskaaaaaaaaaaaaaaaaaaaaaakssssssssskaaaaaaaaaaaaaaa",
    "aaaaakkkksssssskaaaaaaaaaaaaaaaaaaaaaaksssssskkkkaaaaaaaaaaaaaaa",
    "aaaaakkk.kkkkkkkaaaaaaaaaaaaaaaaaaaaaakkkkkkk.kkkaaaaaaaaaaaaaaa",
    ".....kkk....kkk..aaaaaaaaaaaaaaaaaaaa..kkk....kkk...............",
    ".....kkk....kkk...aaaaaaaaaaaaaaaaaa...kkk....kkk...............",
]
NAUTILUS_MAP = [
    "................############################....................",
    "................############################....................",
    "................############################....................",
    "................############################....................",
    "................##############..................................",
    "................##############..................................",
    "................##############..................................",
    "................##############..................................",
    "................##############..................................",
    "................##############..................................",
    "................##############..................................",
    "................##############..................................",
    "................##############..................................",
    "................##############..................................",
    "................##############..................................",
    "................##############..................................",
    "############################################################....",
    "############################################################....",
    "############################################################....",
    "############################################################....",
    "############################################################....",
    "############################################################....",
    "############################################################....",
    "############################################################....",
    "############################################################....",
    "############################################################....",
    "..................................##..........################..",
    "..................................##..........################..",
    "..................................##..........################..",
    "..................................###........#################..",
    "..................................#####....###################..",
    "..................................############################..",
    "..................................############################..",
    "..................................############################..",
    "..................................##############................",
    "..................................##############................",
    "..................................##############................",
    "..................................##############................",
    "..................................##############................",
    "..................................##############................",
    "..................................##############................",
    "..................................##############................",
    "..................................##############................",
    "..................................##############................",
    "..................................##############................",
    "..................................##############................",
    "......##########################################................",
    ".......########################################.................",
    ".......########################################.................",
    ".......########################################.................",
    "......##########################################................",
    "....##############################################..............",
    "######################################################..........",
    "######################################################..........",
]


# ---------------------------------------------------------------------------
# Getragene Ebenen
# ---------------------------------------------------------------------------
# Modellflaechen als UV-Rechtecke (u, v, Breite, Hoehe, Art). Art: "side" senkrechte Flaeche (v nach unten
# = im Modell nach unten, Plattenfugen quer), "top" Oberseite (Fugen quer zur Laenge), "inner" dunkle
# Innen-/Unterseite. Kastenschema: texOffs(u, v), Groesse (w, h, d) -> Oberseite (u+d, v, w, d),
# Unterseite (u+d+w, v, w, d), Seiten in Zeile v+d: (u, d), (u+d, w), (u+d+w, d), (u+2d+w, w).
def _box(u, v, w, h, d, top="top", bottom="inner", side="side"):
    return [(u + d, v, w, d, top), (u + d + w, v, w, d, bottom),
            (u, v + d, d, h, side), (u + d, v + d, w, h, side),
            (u + d + w, v + d, d, h, side), (u + 2 * d + w, v + d, w, h, side)]


# AbstractEquineModel.createBodyMesh: Rumpf, Hals, Kopf, Maehne, Maul, Beine, Ohren
HORSE_FACES = (_box(0, 32, 10, 10, 22, bottom="top") + _box(0, 35, 4, 12, 7) + _box(0, 13, 6, 5, 7)
               + _box(56, 36, 2, 16, 2) + _box(0, 25, 4, 5, 5) + _box(48, 21, 4, 11, 4) + _box(19, 16, 2, 3, 1))
# NautilusArmorModel: Schale oben (0,0) 14x10x16, Schale unten (0,26) 14x8x20, Innenwand (48,26) 14x8x0;
# der Streifen rechts neben der Schalenunterseite (44..61, Zeile 0..3) ist bei Vanilla ebenfalls dunkel.
NAUTILUS_FACES = (_box(0, 0, 14, 10, 16) + _box(0, 26, 14, 8, 20) + [(48, 26, 14, 8, "inner"), (44, 0, 18, 4, "inner")])

# Ender-Perle (Stempel): Ziffern = ENDER-Index, v Ader, g Glimmen, '.' laesst die Platte stehen
PEARL_6 = [
    ".1111.",
    "1v7651",
    "176g41",
    "16g541",
    "154331",
    ".1111.",
]
PEARL_4 = [
    ".11.",
    "1v61",
    "16g1",
    ".11.",
]


def _hash(x, y, salt=0):
    n = (x * 374761393 + y * 668265263 + salt * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return n ^ (n >> 16)


class _Layer:
    def __init__(self, rows, size, faces):
        self.rows, self.faces = rows, faces
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    def mat(self, x, y):
        if 0 <= y < len(self.rows) and 0 <= x < len(self.rows[y]):
            return self.rows[y][x]
        return "."

    def face(self, x, y):
        for f in self.faces:
            u, v, w, h, _ = f
            if u <= x < u + w and v <= y < v + h:
                return f
        return (x, y, 1, 1, "side")

    def put(self, x, y, col):
        self.img.putpixel((x, y), _hex(col))

    def edges(self, x, y, same):
        """(oben, links, unten, rechts) offen: Flaechenrand oder Nachbar nicht in 'same'."""
        u, v, w, h, _ = self.face(x, y)
        return (y == v or self.mat(x, y - 1) not in same, x == u or self.mat(x - 1, y) not in same,
                y == v + h - 1 or self.mat(x, y + 1) not in same, x == u + w - 1 or self.mat(x + 1, y) not in same)

    def plate(self, x, y, keel=None):
        """Enderit-Platte: Licht von links oben je Flaeche, Kanten, Fugen, Adern wie humanoid/enderite.png."""
        u0, v0, w, h, kind = self.face(x, y)
        u, v = x - u0, y - v0
        top, left, bottom, right = self.edges(x, y, "a#")
        if kind == "inner":
            t = 1.4 + (0.7 * (0.5 - v / (h - 1)) if h > 1 else 0) + (0.5 * (0.5 - u / (w - 1)) if w > 1 else 0)
            t += 0.8 if top else (-0.7 if bottom else 0)
            r = _hash(x, y) % 16
            t += 0.6 if r == 0 else (-0.6 if r == 1 else 0)
            tone = max(0, min(3, round(t)))
            if tone >= 1 and (x + 2 * y) % 23 == 0:
                tone += 1   # schwache Aderspur im Inneren
            return ENDER[tone]
        t = 4.0
        if w > 1:
            t += 0.9 * (0.5 - u / (w - 1))
        if h > 1:
            t += 0.9 * (0.5 - v / (h - 1))
        plain = True
        if bottom:
            t -= 1.6; plain = False
        elif right:
            t -= 1.0; plain = False
        elif top:
            t += 1.4; plain = False
        elif left:
            t += 0.8; plain = False
        period = 4 if kind == "side" else 5
        if plain and h >= 6 and v < h - 1:
            if v % period == period - 1:
                t -= 1.7; plain = False
            elif v % period == 0 and v > 0:
                t += 0.9; plain = False
        if keel is not None and u in keel:
            t += 1.6 if u == keel[0] else -0.9
            plain = False
        r = _hash(x, y, 1) % 11
        t += 0.6 if r == 0 else (-0.6 if r == 1 else 0)
        tone = max(1, min(7, round(t)))
        if plain and tone >= 3:
            k = (x + 2 * y) % 17
            if k == 0:
                return VEIN
            if k in (1, 16) and (x - y) % 2 == 0:
                return GLOW
        if keel is not None and u == keel[0] and v % 3 == 1 and not (top or bottom):
            return VEIN
        return ENDER[tone]

    def saddle(self, x, y):
        """Netherit-Satteldecke: Rautensteppung, Glimm-Saum rundum (oben/links hell, unten/rechts dunkler)."""
        top, left, bottom, right = self.edges(x, y, "s")
        if top or left:
            return VEIN if _hash(x, y, 2) % 5 == 0 else GLOW
        if bottom or right:
            return ENDER[5]
        u0, v0, w, h, _ = self.face(x, y)
        t = 3.0 + 0.9 * (0.5 - (x - u0) / max(1, w - 1)) + 0.9 * (0.5 - (y - v0) / max(1, h - 1))
        if (x + y) % 4 == 0 or (x - y) % 4 == 0:
            t -= 1.6   # Steppnaht
        elif (x + y) % 4 == 2 and (x - y) % 4 == 2:
            t += 1.4   # Kissen zwischen den Naehten
        return NETHER[max(1, min(5, round(t)))]

    def strap(self, x, y):
        top, left, bottom, right = self.edges(x, y, "kb")
        if top:
            return NETHER[2]
        if bottom:
            return NETHER[0]
        return NETHER[1] if (x + y) % 5 else NETHER[2]

    def buckle(self, x, y):
        # 3x3 Schnalle: Glimm-Ring, Mitte zeigt den Riemen
        u = next(i for i in range(3) if self.mat(x - i - 1, y) != "b")
        v = next(i for i in range(3) if self.mat(x, y - i - 1) != "b")
        if (u, v) == (1, 1):
            return NETHER[0]
        if (u, v) == (0, 0):
            return VEIN
        if u == 2 or v == 2:
            return ENDER[6]
        return GLOW

    def stamp(self, x0, y0, rows):
        for dy, row in enumerate(rows):
            for dx, ch in enumerate(row):
                if ch == "." or self.mat(x0 + dx, y0 + dy) == ".":
                    continue
                self.put(x0 + dx, y0 + dy, {"v": VEIN, "g": GLOW}.get(ch) or ENDER[int(ch)])


def horse_body():
    layer = _Layer(HORSE_MAP_FULL, 64, HORSE_FACES)
    for y, row in enumerate(layer.rows):
        for x, m in enumerate(row):
            if m == ".":
                continue
            if m == "a":
                u0, v0, w, h, _ = layer.face(x, y)
                keel = (2, 3) if (u0, v0) == (7, 13) or (u0, v0) == (7, 20) else None   # Stirnkamm
                col = layer.plate(x, y, keel)
            elif m == "s":
                col = layer.saddle(x, y)
            elif m == "k":
                col = layer.strap(x, y)
            else:
                col = layer.buckle(x, y)
            layer.put(x, y, col)
    layer.stamp(25, 41, PEARL_4)   # Sattel oben
    layer.stamp(9, 55, PEARL_4)    # Seitenlappen rechts
    layer.stamp(42, 55, PEARL_4)   # Seitenlappen links
    layer.stamp(9, 15, [".g.", "gvg", ".g."])  # Stirnjuwel
    return layer.img


def nautilus_body():
    layer = _Layer(NAUTILUS_MAP, 128, NAUTILUS_FACES)
    keel_faces = {(16, 0), (16, 16), (20, 46)}   # Oberseite, Vorderseite, Vorderseite unten: Schalenkiel
    rivet_faces = {(0, 46), (34, 46)}            # Nietenband der unteren Schale
    for y, row in enumerate(layer.rows):
        for x, m in enumerate(row):
            if m == ".":
                continue
            u0, v0, w, h, kind = layer.face(x, y)
            col = layer.plate(x, y, (6, 7) if (u0, v0) in keel_faces else None)
            if (u0, v0) in rivet_faces and (x - u0) % 4 == 2:
                if y - v0 == 1:
                    col = GLOW
                elif y - v0 == 2:
                    col = ENDER[1]
            layer.put(x, y, col)
    layer.stamp(5, 18, PEARL_6)    # Schalenseite rechts
    layer.stamp(35, 18, PEARL_6)   # Schalenseite links
    return layer.img


HORSE_MAP_FULL = ["." * 64] * 13 + HORSE_MAP


def mount_armor_textures():
    """{relativer Texturpfad: RGBA-Bild} fuer generate_textures.py."""
    return {
        "item/enderite_horse_armor.png": render_icon(HORSE_ICON, ICON_PAL),
        "item/enderite_nautilus_armor.png": render_icon(NAUTILUS_ICON, ICON_PAL),
        "entity/equipment/horse_body/enderite.png": horse_body(),
        "entity/equipment/nautilus_body/enderite.png": nautilus_body(),
    }
