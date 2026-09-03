"""Erzeugt zu jeder Mod-Textur einen Alternativvorschlag und eine Galerie.

Nichts davon wird ins Spiel eingebunden - die Dateien liegen unter
art/alt-textures/ und sind zum Anschauen da.

Grundsatz: Form und Schattierung bleiben unangetastet, nur die Farbidentitaet
wechselt. Deshalb wird in HSV gerechnet und ausschliesslich der Farbton (und
massvoll die Saettigung) veraendert; Helligkeit und Alpha bleiben Pixel fuer
Pixel gleich. Ein Pixel-Art-Bild bleibt so lesbar - ein neu gemaltes Motiv
koennte ich nicht seriös als "Vorschlag" ausgeben.
"""
import colorsys, hashlib, io, json, pathlib, shutil, sys

from PIL import Image

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

REPO = pathlib.Path(r"C:/Users/oussa/Downloads/Minecraft/Mine/custom created mods/simplebuilding")
SRC = REPO / "src/main/resources/assets/simplebuilding/textures"
OUT = REPO / "art/alt-textures"
ALT = OUT / "alternativ"
ORIG = OUT / "original"

# Vier Bloecke, deren Textur byte-gleich mit Vanilla ist - man kann sie im Spiel
# nicht von gewoehnlichem Sand und Kies unterscheiden. Hier hat der Vorschlag
# eine Aufgabe statt nur Geschmack, deshalb bekommen sie ein eigenes Motiv.
GRAVITY = {
    "block/levitating_sand.png":    ("aufsteigend", (0.58, 0.55), "up"),
    "block/levitating_gravel.png":  ("aufsteigend", (0.58, 0.55), "up"),
    "block/suspended_sand.png":     ("schwebend",   (0.47, 0.30), "hold"),
    "block/suspended_gravel.png":   ("schwebend",   (0.47, 0.30), "hold"),
}

SAT_THRESHOLD = 0.18      # darunter gilt ein Bild als grau: Farbdrehung waere unsichtbar
HUE_TURN = 0.42           # ~150 Grad - deutlich anders, aber nicht die exakte Komplementaerfarbe


def load(path: pathlib.Path) -> Image.Image:
    return Image.open(path).convert("RGBA")


def mean_saturation(img: Image.Image) -> float:
    px = [p for p in img.getdata() if p[3] > 8]
    if not px:
        return 0.0
    return sum(colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)[1] for r, g, b, _ in px) / len(px)


def stable_hue(name: str) -> float:
    """Ein fester, aber je Datei anderer Farbton - damit graue Texturen nicht alle gleich getoent werden."""
    return int(hashlib.sha256(name.encode()).hexdigest()[:8], 16) / 0xFFFFFFFF


def recolour(img, fn):
    out = Image.new("RGBA", img.size)
    out.putdata([
        (*(round(c * 255) for c in fn(*colorsys.rgb_to_hsv(r / 255, g / 255, b / 255))), a) if a > 0 else (0, 0, 0, 0)
        for r, g, b, a in img.getdata()
    ])
    return out


def rule_hue_turn(img):
    return recolour(img, lambda h, s, v: colorsys.hsv_to_rgb((h + HUE_TURN) % 1.0, min(1.0, s * 1.08), v))


def rule_tint(img, hue):
    """Fuer graue Texturen: Helligkeit behalten, einen Farbton unterlegen."""
    return recolour(img, lambda h, s, v: colorsys.hsv_to_rgb(hue, max(s, 0.30), v))


def rule_checker_swap(img):
    """Zweifarbige Muster: nur die hellere Haelfte umfaerben - ergibt eine andere Paarung."""
    vals = sorted(colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)[2] for r, g, b, a in img.getdata() if a > 8)
    if not vals:
        return rule_hue_turn(img)
    median = vals[len(vals) // 2]
    return recolour(img, lambda h, s, v: colorsys.hsv_to_rgb(
        (h + 0.5) % 1.0 if v > median else h, s if v <= median else min(1.0, s * 1.15), v))


def rule_gravity(img, hue, sat, motif):
    """Getoent plus ein sparsames Muster, damit der Block im Spiel erkennbar wird."""
    tinted = recolour(img, lambda h, s, v: colorsys.hsv_to_rgb(hue, max(s, sat), v))
    px = tinted.load()
    w, h_ = tinted.size
    for x in range(w):
        for y in range(h_):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if motif == "up":
                # nach oben zeigende Pfeilspitzen im 8er-Raster: heller Akzent
                mark = (y % 8) == ((x % 8) // 2) and (x % 4) in (1, 2)
            else:
                # schwebend: ruhendes Punktraster
                mark = (x % 4 == 2) and (y % 4 == 2)
            if mark:
                px[x, y] = (min(255, r + 70), min(255, g + 70), min(255, b + 70), a)
    return tinted


def choose(rel: str, img: Image.Image):
    if rel in GRAVITY:
        label, (hue, sat), motif = GRAVITY[rel]
        return rule_gravity(img, hue, sat, motif), f"eigenes Motiv ({label}) statt Vanilla-Kopie"
    if "checker" in rel:
        return rule_checker_swap(img), "Zweiklang getauscht: nur die helle Haelfte umgefaerbt"
    if mean_saturation(img) < SAT_THRESHOLD:
        hue = stable_hue(rel)
        return rule_tint(img, hue), f"grau -> getoent (Farbton {hue:.2f})"
    return rule_hue_turn(img), "Farbton um ~150 Grad gedreht, Helligkeit unveraendert"


def main() -> int:
    if OUT.exists():
        shutil.rmtree(OUT)
    rows = []
    for src in sorted(SRC.rglob("*.png")):
        rel = src.relative_to(SRC).as_posix()
        img = load(src)
        alt, rule = choose(rel, img)

        (ALT / rel).parent.mkdir(parents=True, exist_ok=True)
        (ORIG / rel).parent.mkdir(parents=True, exist_ok=True)
        alt.save(ALT / rel)
        shutil.copyfile(src, ORIG / rel)

        w, h = img.size
        rows.append({"rel": rel, "gruppe": rel.split("/")[0], "w": w, "h": h, "regel": rule})

    # Als .js statt .json: per Doppelklick geoeffnet blockiert der Browser
    # fetch() auf lokale Dateien, ein <script> laedt dagegen problemlos.
    header = "// Erzeugt von art/alt-textures/generate.py - nicht von Hand aendern.\n"
    (OUT / "gallery-data.js").write_text(
        header + "window.ALT_DATA = " + json.dumps(rows, ensure_ascii=False, indent=1) + ";\n",
        encoding="utf-8")
    print(f"{len(rows)} Alternativen erzeugt")
    for g in sorted({r['gruppe'] for r in rows}):
        print(f"   {g}: {sum(1 for r in rows if r['gruppe'] == g)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
