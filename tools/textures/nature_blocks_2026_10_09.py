"""Naturvarianten (QUEUE N24/N25, docs/ai/PLAN-Q-BLOCKS-2026-10-09.md): eigene 16x16-Texturen.

- gemeisseltes Packeis / gemeisseltes Blaueis: Rahmen mit Fase und Rille wie gemeisselte Steinziegel, im Feld ein
  eingemeisselter Eiskristall (Packeis: Kristall mit Diagonalarmen und V-Zweigen, Blaueis: gestufte Raute); N35: an beiden Achsen um die
  echte Mitte gespiegelt, Licht nur von oben.
  Paletten aus den Vanilla-Eisfarben, die Formen sind eigene.
- Nautilusschalen-Block (Saeule): Stirnseite = Schnitt durch die Schale (Spirale mit Kammerwaenden, aussen die
  gestreifte Wohnkammer), Seiten = Aussenseite mit welligen Tigerstreifen auf Perlmutt.
- Froschlichter Scharlach/Aqua/Azur: Aufbau wie Vanillas Froschlicht (Besitzer N34: "wie Vanilla", nur die Farbe je
  Variante) - Seite ein weiches rundes Leuchten mit feinen senkrechten Schlieren, Stirnseite ein flacher heller Kern
  mit weichem, unregelmaessigem Rand; eigene Farbrampen.

Rissiges Eis: eigene Texturen seit N52 (n52_cracked_ice_2026_10_13.py), vorher Vanillas frosted_ice_0..3.

  python3 tools/textures/nature_blocks_2026_10_09.py            # schreibt die PNGs + Vorschau
  python3 tools/textures/nature_blocks_2026_10_09.py --check    # prueft, ob die PNGs aktuell sind
"""
import argparse
import colorsys
import io
import math
import os
import sys
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "mc26_3/overlay/resources/assets/simplebuilding/textures/block"
PREVIEW = Path(os.environ.get("SB_PREVIEW_DIR", os.path.expanduser("~/previews") + "/blocks")) / "nature_blocks.png"
CLIENT_JAR = Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client.jar"


def hexrgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def rng(seed):
    """Kleiner deterministischer Zufall (LCG), damit --check stabil bleibt."""
    state = [seed & 0xFFFFFFFF]

    def nxt():
        state[0] = (state[0] * 1664525 + 1013904223) & 0xFFFFFFFF
        return state[0] / 0x100000000
    return nxt


# --------------------------------------------------------------------------------------------------
# Gemeisseltes Eis
# --------------------------------------------------------------------------------------------------
PACKED = [hexrgb(h) for h in ("6f98e6", "7ca5f4", "85adf8", "92b9fe", "a1c3ff", "bcd4ff", "c8dcff")]
BLUE = [hexrgb(h) for h in ("5d8ef0", "6b9dfb", "6ca3fd", "74abfe", "8eb8fe", "a9c8ff", "bfd6ff")]


def chiseled_frame(seed):
    """Tonwerte 0..6: Fase aussen (oben/links hell, unten/rechts dunkel), Rille innen (umgekehrt), Feld mit Eisschlieren."""
    r = rng(seed)
    g = [[3] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            tl = x <= y if x + y < 15 else False
            if ring == 0:
                g[y][x] = 5 if (x == 0 or y == 0) and not (x == 15 or y == 15) else 1
                if (x, y) in ((15, 0), (0, 15)):
                    g[y][x] = 2
            elif ring == 1:
                g[y][x] = 4 if (x == 1 or y == 1) and x < 15 and y < 15 else 3
                if x == 14 or y == 14:
                    g[y][x] = 2
            elif ring == 2:
                # Rille: oben/links Schatten, unten/rechts Licht
                g[y][x] = 0 if (x == 2 or y == 2) and x < 13 and y < 13 else 5
            else:
                g[y][x] = 3
            del tl
    # N35 (Besitzer 2026-10-11: symmetrischer, ausgewogener): keine Zufallsschlieren mehr im Feld, nur je eine
    # helle Facette in den vier Feldecken, an beiden Achsen gespiegelt.
    del r
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        g[y][x] = 4
    return g


def mirrored(quarter):
    """Ein Viertelmotiv (Feldpixel x, y <= 7) an beiden Achsen der echten Mitte (7,5) gespiegelt."""
    cells = set()
    for (x, y) in quarter:
        cells |= {(x, y), (15 - x, y), (x, 15 - y), (15 - x, 15 - y)}
    return cells


def carve(g, cells):
    """Eingemeisselte Linie (1 px): Rille dunkel (0), darunter ein Lichtpixel (5) - Licht nur von oben, damit das
    Motiv links/rechts spiegelgleich bleibt."""
    cells = set(cells)
    for (x, y) in cells:
        g[y][x] = 0
    for (x, y) in cells:
        if (x, y + 1) not in cells and 3 <= y + 1 <= 12 and 3 <= x <= 12:
            g[y + 1][x] = 5


def chiseled_packed_ice():
    """Eiskristall: Diagonalkreuz mit je einem V-Zweig pro Arm, kurze Achsenstriche an den Feldkanten, heller Kern;
    an beiden Achsen gespiegelt (N35)."""
    g = chiseled_frame(11)
    quarter = [(4, 4), (5, 5), (6, 6)]           # Diagonalarm
    quarter += [(6, 4), (4, 6)]                   # V-Zweig an der Armspitze
    quarter += [(7, 6), (6, 7)]                   # kleine Raute um den Kern
    carve(g, mirrored(quarter))
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        g[y][x] = 6 if y == 7 else 4               # heller Kern, oben glaenzend
    return paint(g, PACKED)


def chiseled_blue_ice():
    """Gestufte Raute: Rille als Raute um die echte Mitte, innen eine erhabene Facette (oben hell, unten dunkler);
    an beiden Achsen gespiegelt (N35)."""
    g = chiseled_frame(23)
    outer = [(x, y) for y in range(3, 8) for x in range(3, 8) if (7.5 - x) + (7.5 - y) == 4]
    carve(g, mirrored(outer))
    for y in range(3, 13):
        for x in range(3, 13):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d <= 2:
                g[y][x] = (6 if d <= 1 else 5) if y <= 7 else (4 if d <= 1 else 2)
    return paint(g, BLUE)


def paint(g, pal):
    img = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), pal[g[y][x]])
    return img


# --------------------------------------------------------------------------------------------------
# Nautilusschalen-Block
# --------------------------------------------------------------------------------------------------
NACRE = [hexrgb(h) for h in ("bdb4a2", "d2c9b6", "e2dac8", "ece6d6", "f5f1e6")]
STRIPE = [hexrgb(h) for h in ("7a3f22", "9c5530", "b8693c", "cf8650")]
WHORL = hexrgb("5e4634")
SEPTUM = hexrgb("9a7d63")


def nautilus_side():
    r = rng(5)
    img = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            tone = 2 + (1 if (x * 7 + y * 3) % 11 < 4 else 0) - (1 if x in (0, 15) else 0)
            img.putpixel((x, y), NACRE[max(0, min(4, tone))])
    # Wellige Tigerstreifen quer zur Saeulenachse, eigene Lage je Streifen
    bands = [(2, 0.0, 2), (7, 1.9, 1), (11, 3.6, 2)]
    for base, phase, width in bands:
        for x in range(16):
            yc = base + 1.2 * math.sin(x / 2.6 + phase)
            for w in range(width):
                yy = int(round(yc)) + w
                if 0 <= yy < 16:
                    shade = 2 if w == 0 else 3
                    if r() < 0.12:
                        shade = max(0, shade - 1)
                    img.putpixel((x, yy % 16), STRIPE[max(0, min(3, shade))])
    # Kanten der Saeule: oben/unten eine Perlmutt-Linie (Licht oben, Schatten unten)
    for x in range(16):
        img.putpixel((x, 0), NACRE[4])
        img.putpixel((x, 15), NACRE[0])
    return img


def nautilus_top():
    img = Image.new("RGB", (16, 16), NACRE[1])
    cx, cy = 7.5, 7.5
    b = 0.17
    a = 0.55
    pix = {}
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            rr = math.hypot(dx, dy)
            th = math.atan2(dy, dx) % (2 * math.pi)
            # Abstand zur naechsten Windung der logarithmischen Spirale r = a*e^(b*(th + 2pi*k))
            best = 99
            for k in range(-1, 4):
                rs = a * math.exp(b * (th + 2 * math.pi * k))
                best = min(best, abs(rr - rs))
            outer = a * math.exp(b * (th + 2 * math.pi * 2))
            if rr > 7.6:
                col = NACRE[0] if (x in (0, 15) or y in (0, 15)) else NACRE[1]
            elif best < 0.5:
                col = WHORL
            elif rr > outer:
                # Wohnkammer: Perlmutt mit Streifen, die nach aussen laufen
                stripe = int((th * 180 / math.pi) / 24) % 2 == 0 and rr > outer + 1.2
                col = STRIPE[2] if stripe else NACRE[3]
            else:
                # Kammern: Septen alle 40 Grad, dazwischen Perlmutt-Schattierung
                deg = (th * 180 / math.pi + 20 * math.log(max(rr, 0.5))) % 40
                col = SEPTUM if deg < 6 else (NACRE[4] if deg < 20 else NACRE[2])
            pix[(x, y)] = col
    for (x, y), col in pix.items():
        img.putpixel((x, y), col)
    img.putpixel((7, 7), WHORL)
    return img


# --------------------------------------------------------------------------------------------------
# Froschlichter
# --------------------------------------------------------------------------------------------------
FROGLIGHT_HUES = {"scarlet": 356, "aqua": 178, "azure": 214}


def froglight_ramp(hue):
    """8 Toene vom Rand (gesaettigt, dunkler) zum Kern (fast weiss) - Verlauf wie Vanilla, eigene Farben."""
    out = []
    for i in range(8):
        t = i / 7
        s = 0.6 * (1 - t) ** 1.25 + 0.05
        v = 0.74 + 0.26 * t ** 0.8
        h = (hue + 6 * t) % 360 / 360
        out.append(tuple(int(round(c * 255)) for c in colorsys.hsv_to_rgb(h, s, v)))
    return out


def froglight_side(ramp, seed):
    """Seite wie Vanillas Froschlicht (Besitzer N34 2026-10-11: "sieht nicht wie Vanilla aus" - keine Zacken mehr):
    ein weiches, rundes Leuchten - Kern fast weiss, nach aussen gleichmaessig dunkler, Ecken am dunkelsten - und
    darueber feine senkrechte Schlieren (jede Spalte eine Spur heller oder dunkler, pixelweise etwas Rauschen)."""
    r = rng(seed)
    column = [(-1 if r() < 0.45 else 0.25) for _ in range(16)]
    g = [[7] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x - 7.5) / 8, abs(y - 7.5) / 8
            rad = (dx ** 2.6 + dy ** 2.6) ** (1 / 2.6)
            t = max(0.0, (rad - 0.42) / 0.95)
            tone = 7.7 - 7.7 * t ** 1.1
            if tone > 1.5:
                tone += column[x] * (0.7 + 0.5 * r())
            tone += (r() - 0.5) * 0.6
            g[y][x] = max(0, min(7, int(round(tone))))
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        g[y][x] = 0
    return paint(g, ramp)


def froglight_top(ramp, seed):
    """Stirnseite wie Vanilla: grosser, ganz flacher heller Kern, ein zwei bis drei Pixel breiter weicher Rand mit
    unregelmaessig tiefen Einbuchtungen, die Ecken am dunkelsten; nur die hellen sechs Toene der Rampe."""
    r = rng(seed)
    reach = [[1 + int(r() * 2.6) for _ in range(16)] for _ in range(4)]  # Randbreite je Kante und Position
    g = [[5] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dist = (y, 15 - y, x, 15 - x)
            pos = (x, x, y, y)
            tone = 5
            for edge in range(4):
                d, w = dist[edge], reach[edge][pos[edge]]
                if d <= w:
                    tone = min(tone, 1 + int(r() * 2) + int(3 * d / (w + 1)))
            if min(x, 15 - x) + min(y, 15 - y) == 0:
                tone = 0
            elif min(x, 15 - x) + min(y, 15 - y) == 1:
                tone = min(tone, 1)
            g[y][x] = max(0, min(5, tone)) + 2
    return paint(g, ramp)


# --------------------------------------------------------------------------------------------------

def build():
    tex = {
        "chiseled_packed_ice": chiseled_packed_ice(),
        "chiseled_blue_ice": chiseled_blue_ice(),
    }
    # Owner pick N41 (2026-10-12): proposal A15 (ring top, flowing bands with rust-glow edges).
    import n41_proposals_2026_10_12 as n41
    top, side = n41.ring_block(2)
    tex["nautilus_shell_block_side"] = side.convert("RGB")
    tex["nautilus_shell_block_top"] = top.convert("RGB")
    for i, (name, hue) in enumerate(FROGLIGHT_HUES.items()):
        ramp = froglight_ramp(hue)
        tex[f"{name}_froglight_side"] = froglight_side(ramp, 101 + i)
        tex[f"{name}_froglight_top"] = froglight_top(ramp, 201 + i)
    return tex


def vanilla(name):
    try:
        with zipfile.ZipFile(CLIENT_JAR) as jar:
            return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/{name}.png"))).convert("RGBA").crop((0, 0, 16, 16))
    except (OSError, KeyError):
        return None


def preview(tex):
    """16-fach, Vanilla-Vorbild links neben dem neuen Block, dazu 3x3-Kachel gegen Naht-Fehler."""
    pairs = [
        ("block/packed_ice", "chiseled_packed_ice"), ("block/blue_ice", "chiseled_blue_ice"),
        ("item/nautilus_shell", "nautilus_shell_block_side"), ("block/bone_block_top", "nautilus_shell_block_top"),
        ("block/ochre_froglight_side", "scarlet_froglight_side"), ("block/ochre_froglight_top", "scarlet_froglight_top"),
        ("block/verdant_froglight_side", "aqua_froglight_side"), ("block/verdant_froglight_top", "aqua_froglight_top"),
        ("block/pearlescent_froglight_side", "azure_froglight_side"), ("block/pearlescent_froglight_top", "azure_froglight_top"),
    ]
    s = 8
    cell = 16 * s
    w = 3 * cell + 4 * 12 + 48 * 3
    h = len(pairs) * (cell + 12) + 12
    out = Image.new("RGB", (w, h), (40, 40, 44))
    d = ImageDraw.Draw(out)
    for row, (ref, name) in enumerate(pairs):
        y = 12 + row * (cell + 12)
        v = vanilla(ref)
        if v is not None:
            bg = Image.new("RGBA", v.size, (40, 40, 44, 255))
            out.paste(Image.alpha_composite(bg, v).convert("RGB").resize((cell, cell), Image.NEAREST), (12, y))
        out.paste(tex[name].resize((cell, cell), Image.NEAREST), (24 + cell, y))
        tile = Image.new("RGB", (48, 48))
        for ty in range(3):
            for tx in range(3):
                tile.paste(tex[name], (tx * 16, ty * 16))
        out.paste(tile.resize((cell, cell), Image.NEAREST), (36 + 2 * cell, y))
        d.text((48 + 3 * cell, y + cell // 2 - 6), f"{name}\n(vanilla: {ref})", fill=(230, 230, 230))
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    tex = build()
    stale = []
    for name, img in tex.items():
        path = OUT / f"{name}.png"
        if args.check:
            try:
                cur = Image.open(path)
                same = cur.mode == img.mode and cur.size == img.size and cur.tobytes() == img.tobytes()
            except OSError:
                same = False
            if not same:
                stale.append(str(path.relative_to(ROOT)))
        else:
            OUT.mkdir(parents=True, exist_ok=True)
            img.save(path)
    if args.check:
        if stale:
            print("Veraltet oder fehlend:\n  " + "\n  ".join(stale))
            return 1
        print(f"OK: {len(tex)} Naturvarianten-Texturen aktuell")
        return 0
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    preview(tex).save(PREVIEW)
    print(f"{len(tex)} Texturen geschrieben, Vorschau: {PREVIEW}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
