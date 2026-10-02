"""10 Vorschlaege fuer die Handbuch-Texturen (Besitzer-Queue 2026-10-02 abends), NICHT eingebaut.

Basis ist die 26.3-Form (liegendes Buch wie Vanilla, `MEGA_BOOK` aus guide_book_textures.py, abgenommene
Form von guide_book.png). Heute unterscheiden sich die Themenbuecher nur in der Farbe; jeder Vorschlag A-J
gibt ihnen mehr Wiedererkennung, ohne die Form zu verlassen. Kontur nie schwarz, keine Pixel am Bildrand
ausser denen der abgenommenen Form, Licht von oben links.

  A  Zeichen      kleines 3x3-Themenzeichen mitten auf dem Deckel
  B  Beschlaege   Metallecken an den vier Deckelecken (Gold Mod-Regal, Silber Vanilla-Regal)
  C  Titelschild  helles Pergamentschild auf dem Deckel, Themenpunkt in der Mitte
  D  Schliesse    Lederriemen ueber den Buchblock mit Metallschnalle
  E  Edelstein    Edelstein in der Themen-Akzentfarbe mit Metallfassung
  F  Praegerand   gepraegte helle Randlinie eine Reihe innerhalb der Kontur
  G  Halbleder    dunkler Lederruecken (linkes Drittel) + Themenfarbe, Buende bleiben
  H  Zwei Baender Lesebaendchen in Themenfarbe + Metallfarbe statt eines roten
  I  Goldschnitt  vergoldeter (Mod) bzw. versilberter (Vanilla) Seitenschnitt
  J  Prachtband   B + E + I zusammen (Ecken, Stein, Schnitt)

Aufruf (Pillow): python tools/textures/guide_books_10_proposals_2026_10_02.py [ausgabe.png]
Standard-Ausgabe: C:/Users/o_o/code/minecraft-mods/previews/guide-buecher-10-vorschau.png
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from guide_book_textures import COVERS, MEGA_BOOK, MEGA_MOD, MEGA_PAGES, MEGA_VANILLA, _hex  # noqa: E402

TOPICS = ["guide", "tools", "end", "machines", "building", "vanilla_start", "vanilla_nether", "vanilla_ocean"]
COVER = set("CDL")

# Themen-Akzent (Zeichen/Stein/Band) je Buch
ACCENT = {
    "guide": "#f2c94a", "tools": "#d8dde2", "end": "#4fd1a8", "machines": "#e85a4a", "building": "#f0a070",
    "vanilla_start": "#e0c080", "vanilla_nether": "#ff9a3a", "vanilla_ocean": "#9fe6ff",
}
# 3x3-Zeichen (Vorschlag A), '#' = Akzent, '+' = hell
GLYPHS = {
    "guide": [".#.", "###", ".#."],
    "tools": ["##+", ".+.", "+.."],
    "end": [".#.", "#+#", ".#."],
    "machines": ["###", ".+.", ".+."],
    "building": ["#+#", "+#+", "#+#"],
    "vanilla_start": ["#+#", "+++", "#+#"],
    "vanilla_nether": [".#.", "##.", "#+#"],
    "vanilla_ocean": ["...", "#+#", "+#+"],
}
GOLD = ("#fad64c", "#b8860b")
SILVER = ("#eef1f4", "#9aa3ab")


def metal(topic):
    return SILVER if topic.startswith("vanilla_") else GOLD


def shade(h, f):
    r, g, b, _ = _hex(h)
    return "#%02x%02x%02x" % (min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)))


class Book:
    def __init__(self, topic):
        self.topic = topic
        o, s, l, c, d = COVERS[topic]
        self.pal = dict(O=o, D=d, C=c, L=l, **MEGA_PAGES)
        self.pal.update(MEGA_VANILLA if topic.startswith("vanilla_") else MEGA_MOD)
        self.grid = [list(r) for r in MEGA_BOOK]
        self.extra = {}  # (x, y) -> hex

    def ch(self, x, y):
        return self.grid[y][x] if 0 <= y < 16 and 0 <= x < 16 else "."

    def cover(self, x, y, colour):
        """Paint only where the cover is (never over the outline or the pages)."""
        if self.ch(x, y) in COVER or self.ch(x, y) == "A":
            self.extra[(x, y)] = colour

    def put(self, x, y, colour):
        self.extra[(x, y)] = colour

    def image(self):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y, row in enumerate(self.grid):
            for x, c in enumerate(row):
                if c != ".":
                    img.putpixel((x, y), _hex(self.pal[c]))
        for (x, y), col in self.extra.items():
            img.putpixel((x, y), _hex(col))
        return img


# Deckelmitte und -ecken der MEGA_BOOK-Form
CX, CY = 7, 6
CORNERS = [(9, 2), (2, 5), (12, 5), (5, 9)]


def glyph(b, x0=CX - 1, y0=CY - 1):
    acc = ACCENT[b.topic]
    for dy, row in enumerate(GLYPHS[b.topic]):
        for dx, c in enumerate(row):
            if c == "#":
                b.cover(x0 + dx, y0 + dy, acc)
            elif c == "+":
                b.cover(x0 + dx, y0 + dy, shade(acc, 1.35) if acc != "#d8dde2" else "#ffffff")


def corners(b):
    hi, lo = metal(b.topic)
    for x, y in CORNERS:
        b.cover(x, y, hi)
    # each fitting two pixels long along the edge
    b.cover(10, 3, lo); b.cover(3, 6, lo); b.cover(11, 6, lo); b.cover(6, 9, lo)


def gem(b):
    hi, lo = metal(b.topic)
    acc = ACCENT[b.topic]
    for x, y in [(CX, CY - 1), (CX - 1, CY), (CX + 1, CY), (CX, CY + 1)]:
        b.cover(x, y, lo)
    b.cover(CX, CY, acc)
    b.cover(CX - 1, CY - 1, hi)


def gilt(b):
    hi, lo = metal(b.topic)
    b.pal["P"] = hi
    b.pal["p"] = lo


def variant_a(b):
    glyph(b)


def variant_b(b):
    corners(b)


def variant_c(b):
    plate = "#e9dfc4"
    edge = "#c9bb98"
    for x, y in [(6, 5), (7, 5), (8, 5), (5, 6), (6, 6), (7, 6), (8, 6), (5, 7), (6, 7), (7, 7)]:
        b.cover(x, y, plate)
    for x, y in [(9, 5), (9, 6), (8, 7), (4, 7), (5, 8), (6, 8)]:
        b.cover(x, y, edge)
    b.cover(7, 6, ACCENT[b.topic] if b.topic != "tools" else "#5a6570")


def variant_d(b):
    hi, lo = metal(b.topic)
    strap, strap_dark = "#7a5230", "#4e3219"
    # Lederriemen vom Deckel ueber die Buchblock-Kante, Schnalle auf der Deckelkante
    for x, y in [(10, 6), (11, 6), (11, 7), (12, 7), (12, 8), (13, 8)]:
        b.put(x, y, strap)
    for x, y in [(12, 9), (13, 9), (12, 10)]:
        b.put(x, y, strap_dark)
    b.put(11, 7, hi)
    b.put(12, 7, lo)


def variant_e(b):
    gem(b)


def variant_f(b):
    rim = shade(COVERS[b.topic][2], 1.15)
    for x, y in [(8, 2), (9, 2), (10, 2), (6, 3), (7, 3), (4, 4), (5, 4), (11, 3), (12, 4), (2, 5), (3, 5),
                 (12, 5), (2, 6), (12, 6), (3, 7), (11, 7), (4, 8), (10, 8), (5, 9), (9, 9), (6, 10), (7, 10)]:
        b.cover(x, y, rim)


def variant_g(b):
    dark = COVERS[b.topic][1]
    for y, row in enumerate(b.grid):
        for x, c in enumerate(row):
            if c in "CD" and x + (y - 4) * 0.0 < 5 and y >= 3:
                b.cover(x, y, dark)
            if c == "L":
                b.cover(x, y, shade(dark, 1.4))
    for x, y in [(4, 4), (3, 5), (5, 6), (4, 7)]:
        b.cover(x, y, metal(b.topic)[0])


def variant_h(b):
    acc = ACCENT[b.topic]
    hi, lo = metal(b.topic)
    b.pal["R"] = acc
    b.pal["r"] = shade(acc, 0.6)
    b.put(13, 10, hi)
    b.put(13, 11, lo)
    b.put(12, 12, hi)


def variant_i(b):
    gilt(b)


def variant_j(b):
    corners(b)
    gem(b)
    gilt(b)


VARIANTS = [
    ("A", "Zeichen", variant_a), ("B", "Beschlaege", variant_b), ("C", "Titelschild", variant_c),
    ("D", "Schliesse", variant_d), ("E", "Edelstein", variant_e), ("F", "Praegerand", variant_f),
    ("G", "Halbleder", variant_g), ("H", "Zwei Baender", variant_h), ("I", "Goldschnitt", variant_i),
    ("J", "Prachtband", variant_j),
]


def proposals():
    out = {}
    for letter, _name, fn in VARIANTS:
        for topic in TOPICS:
            b = Book(topic)
            fn(b)
            out[(letter, topic)] = b.image()
    return out


def preview(path):
    scale, cell, pad, label_w, head = 8, 136, 8, 150, 28
    rows = [("heute", None)] + [(f"{l} {n}", l) for l, n, _ in VARIANTS]
    sheet = Image.new("RGBA", (label_w + len(TOPICS) * cell + pad, head + len(rows) * cell + pad), (198, 198, 198, 255))
    draw = ImageDraw.Draw(sheet)
    for i, topic in enumerate(TOPICS):
        draw.text((label_w + i * cell + 4, 8), topic.replace("vanilla_", "v_"), fill=(40, 40, 40, 255))
    props = proposals()
    for r, (label, letter) in enumerate(rows):
        y = head + r * cell
        draw.text((8, y + cell // 2 - 6), label, fill=(20, 20, 20, 255))
        for i, topic in enumerate(TOPICS):
            img = Book(topic).image() if letter is None else props[(letter, topic)]
            sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST), (label_w + i * cell + 4, y + 4))
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
    print("Vorschau:", path)


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else "C:/Users/o_o/code/minecraft-mods/previews/guide-buecher-10-vorschau.png")
