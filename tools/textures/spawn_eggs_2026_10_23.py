"""Spawn eggs in the Vanilla 26.3 style (Queue TX22-2, docs/ai/PLAN-A-EGGS22-2026-10-23.md), exemplary round.

Owner 22.10.: "bei den Spawn-Eiern ist schlechte Arbeit gemacht worden. Das sieht nicht aus wie Vanilla."
Since 1.21.5 every Vanilla spawn egg is its own 16x16 drawing: one egg silhouette (14 rows, 12 wide), lit from the
top left, the outline in the darkest tone of the material under it (bottom/right darker still), 10-25 tones, and the
mob's look painted onto the egg (face, clothes, shell) - sometimes with parts sticking out (vex wings, spider legs).

This round draws two mods' eggs in three variants each (rule "UI/Optik erst exemplarisch"), nothing shipped changes:
  A  Vanilla egg + mob-coloured flecks (the old two-colour idea, but shaded like Vanilla)
  B  Vanilla egg + mob motif (like the new Vanilla eggs: face/shell/clothes on the egg)
  C  Mischform: the motif reduced to its main mark, plus flecks and a part sticking out

Own pixels only: material maps are drawn here as character grids, the shading is computed (light from the top left,
small hash dither), ramps come from the mobs' own entity generators. Vanilla eggs are read from the local client jar
only for the preview page (written outside the repository), never stored.

    python3.12 tools/textures/spawn_eggs_2026_10_23.py [--out DIR] [--html FILE] [--jar client.jar]

Writes candidates <mob>_spawn_egg_<A|B|C>.png + .layers.json (layers Umriss, Grundfarbe, Motiv, Details) to DIR
(default $SB_PREVIEW_DIR/eggs22) and, with --html, a self-contained preview page (16x and 1x, old egg, Vanilla eggs).
"""
from __future__ import annotations

import argparse
import base64
import io
import os
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import layers  # noqa: E402

# Ramps dark -> light (from tools/textures/mobs2_textures.py and village_wanderer_textures.py, so egg and mob match).
BED = [(0x1f, 0x1f, 0x21), (0x34, 0x34, 0x36), (0x52, 0x52, 0x55), (0x64, 0x64, 0x67), (0x8c, 0x8c, 0x8f), (0xa2, 0xa2, 0xa4)]
VIOLET = [(0x2a, 0x12, 0x3a), (0x55, 0x22, 0x77), (0x9a, 0x4c, 0xd8), (0xd9, 0xa8, 0xff), (0xf6, 0xe8, 0xff)]
OCHRE = [(88, 52, 22), (126, 76, 30), (164, 104, 42), (198, 136, 62), (226, 170, 92)]
RUST = [(70, 30, 18), (104, 44, 24), (142, 62, 32), (176, 88, 44)]
GREEN = [(24, 62, 36), (38, 92, 50), (60, 126, 66), (98, 164, 88)]
BRASS = [(122, 80, 18), (190, 140, 34), (240, 200, 80), (255, 240, 164)]
SKIN = [(122, 78, 56), (160, 106, 76), (196, 140, 100), (222, 170, 128)]
LEATHER = [(40, 26, 16), (66, 42, 24), (96, 64, 36), (128, 90, 52)]
BEARD = [(96, 90, 84), (120, 112, 104), (160, 152, 142), (198, 190, 180)]

# The Vanilla-sized egg: rows 1..14, (first, last) column. Small mobs may use SMALL (shifted down, Vanilla endermite).
EGG = {1: (6, 9), 2: (5, 10), 3: (4, 11), 4: (3, 12), 5: (3, 12), 6: (2, 13), 7: (2, 13), 8: (2, 13), 9: (2, 13),
       10: (2, 13), 11: (2, 13), 12: (3, 12), 13: (4, 11), 14: (5, 10)}


def h(x, y, k=0):
    v = (x * 374761393 + y * 668265263 + k * 2147483647) & 0xFFFFFFFF
    v = ((v ^ (v >> 13)) * 1274126177) & 0xFFFFFFFF
    return (v ^ (v >> 16)) & 0xFF


def dark(c, f=0.72):
    return tuple(int(v * f) for v in c[:3])


def egg_rows(material: str) -> list[str]:
    """16 rows of '.' with the egg filled by one material letter."""
    return ["".join(material if y in EGG and EGG[y][0] <= x <= EGG[y][1] else "." for x in range(16)) for y in range(16)]


def paint(rows: list[str], fills: dict) -> list[str]:
    """Overlay: fills maps (x, y) -> letter; only on the egg or where a letter is given explicitly with '!' prefix."""
    g = [list(r) for r in rows]
    for (x, y), ch in fills.items():
        if ch.startswith("!") or g[y][x] != ".":
            g[y][x] = ch[-1]
    return ["".join(r) for r in g]


def overlay(rows: list[str], art: list[str]) -> list[str]:
    """Art grid on top ('.' keeps, ' ' keeps, other letters replace; letters outside the egg add pixels)."""
    return ["".join(a if a not in ". " else r for r, a in zip(row, line.ljust(16, "."))) for row, line in zip(rows, art)]


def shade(rows: list[str], mats: dict, fixed: dict, main: str, light=1.0, dither=True, seed=0):
    """Renders a material grid. mats: letter -> (ramp, base index, noise) shaded; fixed: letter -> RGB as drawn.
    Returns (img, layers) with layers Umriss / Grundfarbe / Motiv / Details as RGBA byte images."""
    filled = {(x, y) for y in range(16) for x in range(16) if rows[y][x] != "."}
    out = {name: bytearray(16 * 16 * 4) for name in ("Umriss", "Grundfarbe", "Motiv", "Details")}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ys = [y for _, y in filled]
    top, bottom = min(ys), max(ys)
    cy = (top + bottom) / 2
    for (x, y) in filled:
        ch = rows[y][x]
        if ch in fixed:
            c, layer = fixed[ch], "Details"
        else:
            ramp, base, noise = mats[ch]
            up = (x, y - 1) not in filled
            left = (x - 1, y) not in filled
            down = (x, y + 1) not in filled
            right = (x + 1, y) not in filled
            if up or left or down or right:
                lit_side = (up or left) and not (down or right)
                c = ramp[0] if lit_side else dark(ramp[0], 0.7)
                if not lit_side and (up or left):
                    c = ramp[0]
                layer = "Umriss"
            else:
                # light from the top left: about +1.5 in the upper left, -1.5 in the lower right
                l = ((7.5 - x) * 0.22 + (cy - y) * 0.26) * light
                if dither:
                    l += ((h(x, y, seed) & 7) - 3.5) / 9.0 * noise
                i = max(1, min(len(ramp) - 1, int(round(base + l))))
                c = ramp[i]
                layer = "Grundfarbe" if ch == main else "Motiv"
        px = tuple(c[:3]) + (255,)
        img.putpixel((x, y), px)
        o = (y * 16 + x) * 4
        out[layer][o:o + 4] = bytes(px)
    # one layer per role (disjoint, opaque): the composition is exact
    names = ["Umriss", "Grundfarbe", "Motiv", "Details"]
    stack = []
    for n in names:
        if any(out[n][3::4]):
            stack.append((n, bytes(out[n])))
    return img, stack


# ---------------------------------------------------------------- Shellker (Simple Mobs)
# The mob: a shulker shell disguised as bedrock (grey noise, black pits, pale flecks), violet head inside.

def shellker(variant):
    mats = {"b": (BED, 3, 2.2), "l": (BED, 4, 1.4), "v": (VIOLET, 2, 0.8), "k": ([BED[0], BED[0], (0x18, 0x0c, 0x22)], 2, 0)}
    fixed = {"E": VIOLET[4], "P": BED[0], "F": BED[5], "G": VIOLET[3]}
    rows = egg_rows("b")
    if variant == "A":
        # flecks: violet blots (2-3 px, like the old spots, but placed on the lit/shade pattern) + bedrock pits
        rows = paint(rows, {(5, 4): "v", (6, 4): "v", (5, 5): "v", (10, 5): "v", (9, 8): "v", (10, 8): "v", (9, 9): "v",
                            (4, 10): "v", (5, 10): "v", (7, 12): "v", (8, 12): "v", (7, 2): "P", (11, 7): "P", (6, 8): "P",
                            (4, 7): "F", (8, 6): "F", (11, 11): "F"})
        main = "b"
    elif variant == "B":
        # shell lid (rows 1-7) with a lighter rim, open slit (rows 8-9) with the violet head and its eye, base below
        art = ["",
               "",
               "",
               "",
               "",
               "",
               "..llllllllllll..",
               "..kkkkkkkkkkkk..",
               "...kvvvvvvvkk...",
               "..kvvvGEvvvvkk..",
               "..llllllllllll..",
               "", "", "", "", ""]
        rows = overlay(rows, art)
        rows = paint(rows, {(7, 3): "P", (10, 4): "P", (5, 12): "P", (10, 12): "F", (4, 5): "F", (8, 13): "P"})
        rows[8] = "." * 3 + rows[8][3:13] + "." * 3  # the shell opens: pinched like a real shulker shell
        main = "b"
    else:
        # C: lid + slit reduced to a dark seam with the eye, violet flecks on the base like the lit inside showing
        art = ["", "", "", "", "", "", "",
               ".llllllllllllll.",
               "..kkkkkGEkkkkk..",
               "", "", "", "", "", "", ""]
        rows = overlay(rows, art)
        rows = paint(rows, {(5, 11): "v", (6, 11): "v", (10, 10): "v", (8, 13): "v", (7, 3): "P", (10, 5): "F", (4, 4): "F",
                            (11, 12): "P"})
        main = "b"
    return shade(rows, mats, fixed, main, seed=11)


# ---------------------------------------------------------------- Village Wanderer (Simple Wanderers)
# The mob: felt hat with a wide brim, grey beard, green eyes, green scarf with a brass bell, ochre travel cloak.

def wanderer(variant):
    mats = {"o": (OCHRE, 2, 0.8), "r": (RUST, 2, 0.6), "g": (GREEN, 2, 0.6), "h": (LEATHER, 2, 0.6),
            "s": (SKIN, 2, 0.4), "n": ([SKIN[0], SKIN[0], SKIN[1], SKIN[1]], 2, 0), "w": (BEARD, 2, 0.5)}
    fixed = {"E": (46, 70, 40), "W": (236, 236, 228), "B": BRASS[2], "Q": BRASS[1], "K": LEATHER[0]}
    rows = egg_rows("o")
    if variant == "A":
        rows = paint(rows, {(5, 3): "g", (6, 3): "g", (5, 4): "g", (9, 5): "r", (10, 5): "r", (10, 6): "r",
                            (4, 8): "g", (5, 8): "g", (4, 9): "g", (8, 9): "r", (9, 9): "r", (11, 10): "g", (11, 11): "g",
                            (6, 12): "r", (7, 12): "r", (8, 2): "B"})
        main = "o"
    elif variant == "B":
        # villager-like face egg: hat (crown rows 1-3, brim row 4 sticking out), face, nose, beard, scarf + bell, cloak
        art = ["",
               "......hhhh......",
               ".....hhhhhh.....",
               "....hhhhhhhh....",
               ".hhhhhhhhhhhhhh.",
               "...ssssssssss...",
               "..sssEWssWEsss..",
               "..ssssnnnnssss..",
               "..swwwnnnnwwws..",
               "..owwwwwwwwwwo..",
               "..ggggggBgggggg.",
               "..ooroooQoooro..",
               "...ooroooooro...",
               "....oroooor.....",
               ".....oooooo.....",
               ""]
        rows = overlay(rows, art)
        main = "o"
    else:
        # C: the hat brim sticks out (like Vanilla's wings/legs), ochre egg, scarf band with the bell, rust hem flecks
        art = ["",
               "......hhhh......",
               ".....hhhhhh.....",
               ".hhhhhhhhhhhhhh.",
               "...oooooooooo...",
               "",
               "",
               "",
               "..gggggggggggg..",
               "..ggggggBggggg..",
               "........Q.......",
               "",
               "....r......r....",
               ".....rr..rr.....",
               "",
               ""]
        rows = overlay(rows, art)
        main = "o"
    return shade(rows, mats, fixed, main, seed=23)


MOBS = {
    "shellker": ("Shellker (Simple Mobs)", shellker, "modules/simplemobs/shared/resources/assets/simplemobs/textures/item/shellker_spawn_egg.png",
                 ["shulker", "endermite", "silverfish"]),
    "village_wanderer": ("Dorf-Wanderer (Simple Wanderers)", wanderer,
                         "modules/simplewanderers/shared/resources/assets/simplewanderers/textures/item/village_wanderer_spawn_egg.png",
                         ["wandering_trader", "villager", "pillager"]),
}
VARIANTS = {"A": "Vanilla-Eiform + Mob-Farbflecken", "B": "Vanilla-Eiform + Mob-Motiv (wie die neuen Vanilla-Eier)",
            "C": "Mischform: Hauptmerkmal + Flecken + abstehendes Teil"}
ALL_OWN = ["shellker", "soulfire_blaze", "dread", "hollow_watcher", "hollow_howler", "deceiver"]


def generate(out: Path) -> dict:
    out.mkdir(parents=True, exist_ok=True)
    result = {}
    for mob, (_, fn, _, _) in MOBS.items():
        for v in VARIANTS:
            img, stack = fn(v)
            p = out / f"{mob}_spawn_egg_{v}.png"
            img.save(p)
            doc = layers.from_layers(16, 16, stack)
            layers.write_beside(p, doc)
            assert bytes(layers.compose(doc)) == img.tobytes(), f"layers of {p.name} do not compose"
            result[(mob, v)] = img
    return result


def b64(img: Image.Image) -> str:
    buf = io.BytesIO()
    img.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def vanilla_eggs(jar: Path | None, names: list[str]) -> dict:
    found = {}
    if jar is None:
        return found
    with zipfile.ZipFile(jar) as z:
        for n in names:
            try:
                found[n] = Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/item/{n}_spawn_egg.png"))).convert("RGBA")
            except KeyError:
                pass
    return found


def html(cands: dict, jar: Path | None) -> str:
    def cell(img, label, big=True):
        s = 160 if big else 16
        return (f'<figure><img src="{b64(img)}" width="{s}" height="{s}" class="px" alt="{label}">'
                f'<figcaption>{label}</figcaption></figure>')

    def strip(imgs):
        # 1x: as in the inventory (with a slot-like background), then 2x
        return "".join(f'<img src="{b64(i)}" width="16" height="16" class="px" title="{t}">' for t, i in imgs)

    parts = []
    for mob, (title, _, old_path, refs) in MOBS.items():
        old = Image.open(ROOT / old_path).convert("RGBA")
        van = vanilla_eggs(jar, refs)
        row = [cell(old, "alt (ausgeliefert)")] + [cell(cands[(mob, v)], f"{v}: {VARIANTS[v]}") for v in VARIANTS]
        row += [cell(i, f"Vanilla {n}") for n, i in van.items()]
        ones = [("alt", old)] + [(v, cands[(mob, v)]) for v in VARIANTS] + [(n, i) for n, i in van.items()]
        parts.append(f'<section><h2>{title}</h2><div class="row">{"".join(row)}</div>'
                     f'<p class="lbl">1&times; (Inventar-Größe) und 2&times;, Reihenfolge: alt, A, B, C, Vanilla</p>'
                     f'<div class="slot">{strip(ones)}</div><div class="slot x2">{strip(ones)}</div></section>')
    # Vanilla overview: the language of the 26.3 eggs (shape, light, outline, motif)
    over = vanilla_eggs(jar, ["zombie", "creeper", "blaze", "shulker", "endermite", "vex", "spider", "wandering_trader",
                              "villager", "enderman", "witch", "breeze"])
    over_html = "".join(cell(i, n) for n, i in over.items()) if over else "<p>Kein Client-Jar gefunden.</p>"
    old_all = []
    for p in sorted(ROOT.glob("modules/*/shared/resources/assets/*/textures/item/*_spawn_egg.png")):
        old_all.append(cell(Image.open(p).convert("RGBA"), p.stem.replace("_spawn_egg", "") + " (alt)"))
    return f"""<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Spawn-Eier TX22-2</title><style>
:root{{--bg:#f4f4f2;--fg:#1d1d1f;--mut:#5a5a60;--card:#fff;--line:#d6d6d8;--slot:#8b8b8b}}
@media (prefers-color-scheme:dark){{:root:not([data-theme=light]){{--bg:#18181a;--fg:#ececee;--mut:#a8a8b0;--card:#232326;--line:#3a3a3e}}}}
:root[data-theme=dark]{{--bg:#18181a;--fg:#ececee;--mut:#a8a8b0;--card:#232326;--line:#3a3a3e}}
body{{background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif;margin:0;padding:16px;max-width:1200px}}
h1{{font-size:22px}} h2{{font-size:18px;margin:0 0 8px}} section{{background:var(--card);border:1px solid var(--line);border-radius:8px;padding:12px;margin:12px 0}}
.row{{display:flex;flex-wrap:wrap;gap:12px}} figure{{margin:0;width:168px}} figcaption{{font-size:12px;color:var(--mut)}}
.px{{image-rendering:pixelated;background:#8b8b8b;border:1px solid var(--line)}}
.slot{{display:inline-flex;gap:2px;background:var(--slot);padding:4px;margin:4px 8px 0 0;vertical-align:middle}} .slot img{{border:0}}
.x2 img{{width:32px;height:32px}} .lbl{{font-size:12px;color:var(--mut);margin:8px 0 0}} ul{{padding-left:20px}}
</style></head><body>
<h1>Spawn-Eier TX22-2 &ndash; exemplarisch (Shellker, Dorf-Wanderer)</h1>
<p>Besitzer 22.10.: &bdquo;Das sieht nicht aus wie Vanilla.&ldquo; Zwei Mods als Beispiel, je drei Varianten; nach der Wahl werden alle 9 eigenen Eier so gezeichnet (Plan <code>docs/ai/PLAN-A-EGGS22-2026-10-23.md</code>). Vanilla-Bilder nur auf dieser Seite, nicht im Repo.</p>
<ul><li><b>A</b> {VARIANTS['A']}</li><li><b>B</b> {VARIANTS['B']}</li><li><b>C</b> {VARIANTS['C']}</li></ul>
<p>Empfehlung: <b>B</b> &ndash; so arbeitet Vanilla seit 1.21.5 (Gesicht/Panzer/Kleidung auf dem Ei, Licht oben links, farbige statt schwarzer Kontur).</p>
{''.join(parts)}
<section><h2>Vanilla 26.3 zum Vergleich (Formensprache)</h2><div class="row">{over_html}</div>
<p class="lbl">Gleiche Eiform (14 Zeilen, 12 breit), Kontur im dunkelsten Ton des Materials, Licht oben links, 10&ndash;25 Töne, Mob-Motiv; kleine Mobs kleiner (Endermite), Flügel/Beine stehen über (Vex, Spinne).</p></section>
<section><h2>Alter Stand aller eigenen Eier</h2><div class="row">{''.join(old_all)}</div></section>
<p class="lbl">Referenzen: Vanilla 26.3 Client-Jar (item/*_spawn_egg.png), Minecraft Wiki &bdquo;Spawn Egg&ldquo; (Texturwechsel 1.21.5 / 25w08a).</p>
</body></html>"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(os.environ.get("SB_PREVIEW_DIR", os.path.expanduser("~/previews")), "eggs22"))
    ap.add_argument("--html")
    ap.add_argument("--jar")
    a = ap.parse_args()
    out = Path(a.out).resolve()
    try:
        out.relative_to(ROOT)
        sys.exit("--out must lie outside the repository (candidates only, nothing shipped changes)")
    except ValueError:
        pass
    cands = generate(out)
    print(f"{len(cands)} candidates -> {out}")
    if a.html:
        sys.path.insert(0, str(HERE))
        import vanilla_derived
        jar = vanilla_derived.find_jar(a.jar)
        Path(a.html).write_text(html(cands, jar), encoding="utf-8")
        print(f"preview -> {a.html} (Vanilla jar: {jar})")


if __name__ == "__main__":
    main()
