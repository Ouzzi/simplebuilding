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
  gadgets   messingbraun, Zahnrad                          (Geraete, 2026-09-29)
  enchantments tiefviolett, Funkeln                        (Verzauberungen, 2026-09-29)

Das Vanilla-Regal entfiel 2026-10-11.
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
    "gadgets": ("#33240c", "#4a3614", "#b08a3e", "#8c6a2a", "#6e521f"),
    "enchantments": ("#1f0f2e", "#2f1745", "#8a4fc0", "#6a3796", "#522a76"),
}
GOLD = {"g": "#e8b93a"}
SILVER = {"g": "#d8dde2"}

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
    "gadgets": ([
        "......",
        "G.GG.G",
        ".GGGG.",
        "GGkkGG",
        "GGkkGG",
        ".GGGG.",
        "G.GG.G",
        "......",
        "......",
    ], {"G": "#e0bd5a", "k": "#3a2a10"}),
    "enchantments": ([
        "......",
        "..W...",
        ".WLW..",
        "WLLLW.",
        ".WLW..",
        "..W...",
        "....W.",
        "...WLW",
        "....W.",
    ], {"W": "#f3e0ff", "L": "#c08af0"}),
}

ORDER = ["guide", "tools", "enchantments", "building", "storage", "machines", "end", "tweaks", "gadgets", "trims", "admin"]
# Neu seit 2026-09-29 und nur auf der Hauptlinie 26.3 (generate_textures schreibt sie nicht in den 1.21.11-Baum).
MAIN_LINE_ONLY = {f"item/guide_book_{t}.png" for t in ORDER if t in ("enchantments", "gadgets")}


def guide_book(topic):
    o, s, l, c, d = COVERS[topic]
    pal = {"O": o, "S": s, "L": l, "C": c, "D": d}
    pal.update(PAGE)
    pal.update(SILVER if topic.startswith("vanilla_") else GOLD)
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


# 26.3-Regale (Textur-Audit Q1, 2026-10-02): alle Themenbuecher in der Form der vom Besitzer abgenommenen
# guide_book.png / guide_book_vanilla_start.png (liegendes Buch wie Vanilla, 1 px dunkle Kontur, Licht von oben
# links). O Kontur, D/C/L Deckel dunkel/mittel/hell (aus COVERS), A Buende auf dem Ruecken, P/p Seiten,
# R/r Lesezeichen hell/dunkel.
# Besitzerwahl 2026-10-02 abends: Vorschlag J "Prachtband" (guide_books_10_proposals_2026_10_02.py) fuer ALLE
# Buecher: Metallecken (B), Edelstein in der Themenfarbe mit Fassung (E), Gold-/Silberschnitt (I). Die beiden
# frueher von Hand abgenommenen Bilder haben exakt diese Form; ihre Farben stehen in MEGA_HAND und werden mit
# demselben Prachtband gezeichnet.
MEGA_BOOK = [
    "................",
    "........OOO.....",
    "......OOCCCO....",
    "....OOCCCCCCO...",
    "..OOCCCACCCCCO..",
    "OOCCCCCCACCCCDO.",
    "OCCCCCCCCACCCDDO",
    "OOCCCACCCCCCDDP.",
    "OLDCCCCCCCDDPpPR",
    "OpLDCCCCCDDPppPr",
    ".OpLDCCDDPppPDr.",
    "..OpLDDPppPDD.Rr",
    "...OpLPpPDD...r.",
    "....OLPDD.......",
    ".....OOO........",
    "................",
]
MEGA_PAGES = {"p": "#a8a8a8", "P": "#d6d6d6"}
MEGA_MOD = {"A": "#fad64c", "r": "#8c1814", "R": "#d6342c"}  # wie guide_book.png: Goldbuende, rotes Band
MEGA_VANILLA = {"A": "#e6e6e6", "r": "#28681a", "R": "#56aa34"}  # Silberbuende, gruenes Band wie vanilla_start
# Farben der Handtexturen (Besitzer, abgenommen 2026-10-02), Form = MEGA_BOOK
MEGA_HAND = {
    "guide": dict(O="#121a42", C="#385cba", A="#fad64c", D="#243a84", P="#d6d6d6", L="#608ade", p="#a8a8a8", R="#d6342c", r="#8c1814"),
    "vanilla_start": dict(O="#2e1c0a", C="#784e1e", A="#6eba3c", D="#543412", P="#d6d6d6", L="#9e7034", p="#a8a8a8", R="#56aa34", r="#28681a"),
}
# Prachtband J: Metall hell/dunkel je Regal, Edelstein je Thema
MEGA_GOLD = ("#fad64c", "#b8860b")
MEGA_SILVER = ("#eef1f4", "#9aa3ab")
MEGA_GEM = {
    "guide": "#f2c94a", "tools": "#d8dde2", "building": "#f0a070", "storage": "#e0a050", "machines": "#e85a4a",
    "end": "#4fd1a8", "tweaks": "#8fe06a", "trims": "#7fb8ff", "admin": "#62d662", "gadgets": "#ffb347",
    "enchantments": "#d08aff", "vanilla_start": "#e0c080", "vanilla_overworld": "#8fd46a", "vanilla_caves": "#ffb030",
    "vanilla_ocean": "#9fe6ff", "vanilla_nether": "#ff9a3a", "vanilla_end": "#c88aff", "vanilla_redstone": "#ff3a2a",
    "vanilla_gear": "#5ad0e0", "vanilla_farming": "#f0cc5a",
}
MEGA_CORNERS = [(9, 2), (2, 5), (12, 5), (5, 9)]
MEGA_CORNERS_LO = [(10, 3), (3, 6), (11, 6), (6, 9)]
MEGA_GEM_POS = (7, 6)


def mega_guide_book(topic):
    if topic in MEGA_HAND:
        pal = dict(MEGA_HAND[topic])
    else:
        o, s, l, c, d = COVERS[topic]
        pal = dict(O=o, D=d, C=c, L=l, **MEGA_PAGES)
        pal.update(MEGA_VANILLA if topic.startswith("vanilla_") else MEGA_MOD)
    hi, lo = MEGA_SILVER if topic.startswith("vanilla_") else MEGA_GOLD
    pal["P"], pal["p"] = hi, lo  # I: Gold-/Silberschnitt
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(MEGA_BOOK):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), _hex(pal[ch]))

    def cover(x, y, colour):  # nur auf dem Deckel, nie auf Kontur oder Seiten
        if MEGA_BOOK[y][x] in "CDLA":
            img.putpixel((x, y), _hex(colour))

    for x, y in MEGA_CORNERS:  # B: Beschlaege
        cover(x, y, hi)
    for x, y in MEGA_CORNERS_LO:
        cover(x, y, lo)
    gx, gy = MEGA_GEM_POS  # E: Edelstein mit Fassung
    for x, y in [(gx, gy - 1), (gx - 1, gy), (gx + 1, gy), (gx, gy + 1)]:
        cover(x, y, lo)
    cover(gx, gy, MEGA_GEM[topic])
    cover(gx - 1, gy - 1, hi)
    return img


def mega_guide_textures(check=False):
    """26.3-only shelf-guide covers (mc26_3 overlay), Prachtband J, with a 16x old/new preview."""
    from pathlib import Path
    root = Path(__file__).resolve().parents[2]
    target = root / "mc26_3/overlay/resources/assets/simplebuilding/textures/item"
    target.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGBA", (256 * 3, 256 * len(ORDER)), (198, 198, 198, 255))
    failures = []
    for i, topic in enumerate(ORDER):
        name = "guide_book.png" if topic == "guide" else f"guide_book_{topic}.png"
        path = target / name
        img = mega_guide_book(topic)
        if check:
            if not path.exists() or Image.open(path).convert("RGBA").tobytes() != img.tobytes():
                failures.append(name)
        else:
            img.save(path)
        sheet.alpha_composite(guide_book(topic).resize((256, 256), Image.Resampling.NEAREST), (0, i * 256))
        sheet.alpha_composite(img.resize((256, 256), Image.Resampling.NEAREST), (256, i * 256))
        insert = img.resize((10, 10), Image.Resampling.NEAREST)
        sheet.alpha_composite(insert.resize((160, 160), Image.Resampling.NEAREST), (560, i * 256 + 48))
    if failures:
        raise SystemExit("Stale mega guide textures: " + ", ".join(failures))
    if not check:
        preview = root / "docs/previews/mega-guides-16x.png"
        preview.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(preview)
    print(f"Mega guides: {len(ORDER)} Prachtband covers")


if __name__ == "__main__":
    # Vorschau (8-fach, Inventar-Grau) nach argv[1]; die Texturen selbst schreibt generate_textures.py.
    import sys
    if "--mega" in sys.argv:
        mega_guide_textures("--check" in sys.argv)
        raise SystemExit(0)
    tex = guide_book_textures()
    sheet = Image.new("RGBA", (len(tex) * 144 + 8, 144), (198, 198, 198, 255))
    for i, img in enumerate(tex.values()):
        sheet.alpha_composite(img.resize((128, 128), Image.NEAREST), (i * 144 + 8, 8))
    sheet.save(sys.argv[1])
