"""Einsteiger-Handbuch und Themenbuecher (2026-09-28) fuer generate_textures.py.

Neue Pixelkunst, keine Umfaerbung des Vanilla-Buchs: ein stehendes, gebundenes Buch von vorn, der
Buchblock schaut rechts einen Pixel hervor (leicht von rechts gesehen). Linker Rand der Ruecken mit
zwei Goldbuenden, Licht von oben links, Kontur nie schwarz, Konturecken frei (keine dunkle
Eckfuellung), eine Spalte Abstand zu jedem Bildrand. Auf dem Deckel ein kleines Zeichen je Thema:

  guide     blau, goldener Stern ueber einem Titelstreifen (Einsteiger-Handbuch)
  tools     schiefergrau, Spitzhacke                       (Werkzeuge & Aufwertungen)
  building  terrakotta, Ziegelmauer                        (Bauen)
  storage   eichenbraun, Truhe mit Goldschloss             (Lagerung)
  machines  redstonerot, Kolbenkopf                        (Maschinen & Kolben)
  end       violett, Enderauge                             (Ende & Enderit)
  tweaks    gruen, Druckplatte mit Pfeil nach oben         (Pads & Geraete)
  trims     nachtblau, Brustpanzer mit goldenem Besatz     (Besaetze & Strahlkraft)
  admin     schiefergrau-dunkel, Konsole mit gruener Eingabe (Server & Admin, 2026-09-28)
"""
from PIL import Image


def _hex(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# Buch (16x16). O Kontur, S Ruecken, g Goldbund, L/C/D Deckel hell/mittel/dunkel,
# P/p Buchblock hell/Schatten, o Kontur des Buchblocks, '.' frei. Das Zeichen liegt in der
# Deckelflaeche (Spalten 5..10, Zeilen 4..12) und ersetzt dort C/D-Pixel.
BOOK = [
    "................",
    "................",
    "...OOOOOOOOO....",
    "..OSLLLLLLLLOo..",
    "..OgLLCCCCCCOPo.",
    "..OSLCCCCCCCOPo.",
    "..OSLCCCCCCCOPo.",
    "..OSLCCCCCCCOPo.",
    "..OSCCCCCCCCOPo.",
    "..OSCCCCCCCCOPo.",
    "..OSCCCCCCCCOPo.",
    "..OSCCCCCCCDOPo.",
    "..OgCCCCCCCDOPo.",
    "..OSDDDDDDDDOpo.",
    "...OOOOOOOOOOo..",
    "................",
]

PAGE = {"P": "#e9dfc4", "p": "#c9bb98", "o": "#5a4a36"}

# Deckelfarben je Thema: O, S, L, C, D (dunkel -> hell sortiert: O < S < D < C < L)
COVERS = {
    "guide": ("#16264a", "#223a6b", "#5f86c9", "#3d62a6", "#2d4c86"),
    "tools": ("#22282e", "#343c44", "#7d8a96", "#5a6570", "#46505a"),
    "building": ("#4a2012", "#6a2e1a", "#c97a4e", "#a65a36", "#86462a"),
    "storage": ("#35220f", "#4d3217", "#a57a47", "#82592f", "#684623"),
    "machines": ("#3a0c0c", "#561414", "#b54343", "#8e2a2a", "#721f1f"),
    "end": ("#24122f", "#361b47", "#8a5cb0", "#6a3f8f", "#532f72"),
    "tweaks": ("#18301a", "#244826", "#6fa35e", "#4e8040", "#3c6532"),
    "trims": ("#10142a", "#1b2342", "#4f5f99", "#343f73", "#28315a"),
    "admin": ("#1a1c1f", "#2a2d31", "#8b9199", "#636870", "#4c5057"),
}
GOLD = {"g": "#e8b93a"}

# Zeichen, 6 breit x 9 hoch, an Spalte 5 / Zeile 4. '.' = Deckel bleibt. Buchstaben eigene Palette.
EMBLEMS = {
    "guide": ([
        "......",
        "..Y...",
        ".YWY..",
        "YYWYY.",
        ".YYY..",
        ".Y.Y..",
        "......",
        "YYYYY.",
        "......",
    ], {"Y": "#e8b93a", "W": "#fff3b0"}),
    "tools": ([
        "......",
        ".IIII.",
        "I..wI.",
        "...w.I",
        "..w...",
        ".w....",
        "w.....",
        "......",
        "......",
    ], {"I": "#d8dde2", "w": "#9a6b3a"}),
    "building": ([
        "......",
        "BBbBBb",
        "mmmmmm",
        "BbBBbB",
        "mmmmmm",
        "BBbBBb",
        "mmmmmm",
        "BbBBbB",
        "......",
    ], {"B": "#d98a5f", "b": "#b86a44", "m": "#e6d6be"}),
    "storage": ([
        "......",
        "WWWWWW",
        "WwwwwW",
        "KKGGKK",
        "WwGgwW",
        "WwwwwW",
        "WWWWWW",
        "......",
        "......",
    ], {"W": "#c79553", "w": "#a0733b", "K": "#3e2a14", "G": "#e8b93a", "g": "#a8801f"}),
    "machines": ([
        "......",
        "TTTTTT",
        "TttttT",
        "..ss..",
        "..ss..",
        "..ss..",
        "RRRRRR",
        "RrrrrR",
        "......",
    ], {"T": "#c7a26a", "t": "#9c7c4a", "s": "#bfbfbf", "R": "#9a9a9a", "r": "#6f6f6f"}),
    "end": ([
        "......",
        "......",
        ".eEEe.",
        "eEGGEe",
        "EGkkGE",
        "eEGGEe",
        ".eEEe.",
        "......",
        "......",
    ], {"e": "#2f6f5a", "E": "#3f9c7c", "G": "#9be0c4", "k": "#0e1f1a"}),
    "tweaks": ([
        "......",
        "..A...",
        ".AAA..",
        "AAAAA.",
        "..A...",
        "......",
        "PPPPPP",
        "pppppp",
        "......",
    ], {"A": "#f2f2f2", "P": "#b8b8b8", "p": "#7c7c7c"}),
    "trims": ([
        "......",
        "Aa..aA",
        "AAaaAA",
        ".AyyA.",
        ".AAyA.",
        ".AyAA.",
        ".AAAA.",
        "......",
        "......",
    ], {"A": "#c8ced6", "a": "#8d96a3", "y": "#f0c84a"}),
    "admin": ([
        "......",
        ".FFFF.",
        "FkkkkF",
        "FgkkkF",
        "FkgkkF",
        "FgkggF",
        ".FFFF.",
        "......",
        "......",
    ], {"F": "#c8ced6", "k": "#16191d", "g": "#62d662"}),
}

ORDER = ["guide", "tools", "building", "storage", "machines", "end", "tweaks", "trims", "admin"]


def guide_book(topic):
    o, s, l, c, d = COVERS[topic]
    pal = {"O": o, "S": s, "L": l, "C": c, "D": d}
    pal.update(PAGE)
    pal.update(GOLD)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(BOOK):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), _hex(pal[ch]))
    rows, epal = EMBLEMS[topic]
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch != ".":
                img.putpixel((5 + dx, 4 + dy), _hex(epal[ch]))
    return img


def guide_book_textures():
    return {("item/guide_book.png" if t == "guide" else f"item/guide_book_{t}.png"): guide_book(t) for t in ORDER}


if __name__ == "__main__":
    # Vorschau (8-fach, Inventar-Grau) nach argv[1]; die Texturen selbst schreibt generate_textures.py.
    import sys
    tex = guide_book_textures()
    sheet = Image.new("RGBA", (len(tex) * 144 + 8, 144), (198, 198, 198, 255))
    for i, img in enumerate(tex.values()):
        sheet.alpha_composite(img.resize((128, 128), Image.NEAREST), (i * 144 + 8, 8))
    sheet.save(sys.argv[1])
