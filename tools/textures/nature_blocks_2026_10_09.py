"""Naturvarianten (QUEUE N24/N25, docs/ai/PLAN-Q-BLOCKS-2026-10-09.md): eigene 16x16-Texturen.

- gemeisseltes Packeis / gemeisseltes Blaueis: Rahmen mit Fase und Rille wie gemeisselte Steinziegel, im Feld ein
  eingemeisselter Eiskristall (Packeis: achtarmiger Stern mit Seitenzweigen, Blaueis: gestufte Raute).
  Paletten aus den Vanilla-Eisfarben, die Formen sind eigene.
- Nautilusschalen-Block (Saeule): Stirnseite = Schnitt durch die Schale (Spirale mit Kammerwaenden, aussen die
  gestreifte Wohnkammer), Seiten = Aussenseite mit welligen Tigerstreifen auf Perlmutt.
- Froschlichter Scharlach/Aqua/Azur: Aufbau wie Vanillas Froschlicht (helles Inneres, Rand dunkler, an den Seiten
  herablaufende Tropfen), aber eigenes Tropfen-/Blasenmuster und eigene Farbrampen.

Rissiges Eis braucht keine Textur (Modelle zeigen auf minecraft:block/frosted_ice_0..3), die Stufen ebenso.

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
PREVIEW = Path(os.environ.get("SB_PREVIEW_DIR", "/root/previews/blocks")) / "nature_blocks.png"
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
    # Eisschlieren im Feld: kurze helle Diagonalen wie Packeis (eigene Lage)
    for _ in range(5):
        x, y = 3 + int(r() * 10), 3 + int(r() * 10)
        for k in range(1 + int(r() * 3)):
            if 3 <= x + k <= 12 and 3 <= y - k <= 12:
                g[y - k][x + k] = 4
    return g


def carve(g, cells):
    """Eingemeisselte Linie: Rille dunkel (0), rechts unten daneben ein Lichtpixel (5), falls Feld."""
    cells = set(cells)
    for (x, y) in cells:
        g[y][x] = 0
    for (x, y) in cells:
        hx, hy = x + 1, y + 1
        if (hx, hy) not in cells and 3 <= hx <= 12 and 3 <= hy <= 12 and g[hy][hx] not in (0,):
            g[hy][hx] = 5


def chiseled_packed_ice():
    g = chiseled_frame(11)
    c = 7
    cells = []
    for d in range(-4, 5):
        cells += [(c + d, c), (c, c + d)]
    for d in range(-3, 4):
        cells += [(c + d, c + d), (c + d, c - d)]
    # Seitenzweige an den geraden Armen (V-Form kurz vor der Spitze)
    for sx, sy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        px, py = c + 3 * sx, c + 3 * sy
        if sx:
            cells += [(px + sx, py - 1), (px + sx, py + 1)]
        else:
            cells += [(px - 1, py + sy), (px + 1, py + sy)]
    carve(g, cells)
    g[c][c] = 6  # Glanzpunkt in der Mitte
    return paint(g, PACKED)


def chiseled_blue_ice():
    g = chiseled_frame(23)
    c = 7
    outer = [(x, y) for y in range(3, 13) for x in range(3, 13) if abs(x - c) + abs(y - c) == 4]
    inner = [(x, y) for y in range(3, 13) for x in range(3, 13) if abs(x - c) + abs(y - c) == 2]
    carve(g, outer)
    for (x, y) in [(x, y) for y in range(3, 13) for x in range(3, 13) if abs(x - c) + abs(y - c) < 2]:
        g[y][x] = 6 if (x, y) == (c - 1, c) or (x, y) == (c, c - 1) else 4
    for (x, y) in inner:
        g[y][x] = 1 if x >= c and y >= c else 5
    g[c][c] = 6
    # Ecken des Feldes: kleine Facetten
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        g[y][x] = 4 if x + y < 15 else 2
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
    """Seite: Kern hell, an Ober- und Unterkante laufen ungleich lange Tropfen ein, Seitenkanten weich dunkler."""
    r = rng(seed)
    top = [1 + int(r() * 6) for _ in range(16)]
    bottom = [1 + int(r() * 6) for _ in range(16)]
    g = [[7] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = min(x, 15 - x)
            a = 1 + int(6 * y / top[x]) if y < top[x] else 7
            b = 1 + int(6 * (15 - y) / bottom[x]) if 15 - y < bottom[x] else 7
            c = 2 + 2 * d + (1 if r() < 0.35 else 0)
            tone = min(7, a, b, c)
            if 3 <= tone <= 6 and r() < 0.22:
                tone -= 1
            g[y][x] = max(0, tone)
    return paint(g, ramp)


def froglight_top(ramp, seed):
    """Stirnseite: grosser heller Kern, zackiger Rand, Ecken am dunkelsten."""
    r = rng(seed)
    depth = [[1 + int(r() * 3) for _ in range(16)] for _ in range(4)]  # Randtiefe je Kante und Position
    g = [[7] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dist = [y, 15 - y, x, 15 - x]
            pos = [x, x, y, y]
            tone = 7
            for edge in range(4):
                reach = depth[edge][pos[edge]]
                if dist[edge] < reach + 1:
                    tone = min(tone, 2 + int(5 * dist[edge] / (reach + 1)))
            if min(x, 15 - x) + min(y, 15 - y) <= 1:
                tone = min(tone, 1)
            if 3 <= tone <= 6 and r() < 0.18:
                tone -= 1
            g[y][x] = tone
    return paint(g, ramp)


# --------------------------------------------------------------------------------------------------

def build():
    tex = {
        "chiseled_packed_ice": chiseled_packed_ice(),
        "chiseled_blue_ice": chiseled_blue_ice(),
        "nautilus_shell_block_side": nautilus_side(),
        "nautilus_shell_block_top": nautilus_top(),
    }
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
