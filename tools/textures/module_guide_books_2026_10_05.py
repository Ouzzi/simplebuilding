"""Modul-Handbuecher (2026-10-05): ein Guide-Buch je Modul im Stil der SB-Handbuecher (Besitzerwahl J "Prachtband").

Gleiche Form und Zeichnung wie guide_book_textures.mega_guide_book (Goldbuende, rotes Band, Metallecken, Edelstein
mit Fassung); je Modul eigene Deckelfarbe und ein Edelstein in der Modulfarbe. Nur Module mit Buch-Item (Visuals und
Sounds sind Client-Mods ohne Item, siehe docs/ai/PLAN-MODUL-GUIDES-2026-10-05.md).

Aufruf (Pillow): python tools/textures/module_guide_books_2026_10_05.py [--check] [vorschau.png]
Schreibt modules/<modul>/shared/resources/assets/<ns>/textures/item/guide_book.png und eine Vorschau (A-H, 16-fach).
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import guide_book_textures as gbt  # noqa: E402
import texture_round6_2026_10_05 as round6  # noqa: E402

# Besitzer 2026-10-05: "etwas kontrastreicher" - die Deckeltoene S, L, C, D je Modul um ihren Mittelwert gespreizt
# (Kontur O bleibt; bei 1.25 war der Unterschied kaum sichtbar, weil C und D fast gleich sind).
COVER_CONTRAST = 1.6
ICON_CONTRAST = 1.2  # dazu das ganze Icon (Deckel gegen Gold/Kontur) +20 % um den Mittelwert

# Modul -> (Namespace, Deckel O, S, L, C, D, Edelstein, Beschreibung)
BOOKS = {
    "simpleriding": ("simpleriding", ("#2e1a0c", "#45281a", "#a8764a", "#86592f", "#6a4524"), "#f0cc4a", "Sattelleder, Heu-Edelstein"),
    "simplefun": ("simplefun", ("#3e170e", "#5a2416", "#d0704a", "#ac5434", "#8a4128"), "#ff9ec0", "Ziegelrot, Schweinchen-Rosa"),
    "simplemoney": ("simplemoney", ("#12301a", "#1d4626", "#62a85e", "#448a44", "#356e36"), "#ffd84a", "Geldscheingruen, Gold"),
    "simplesandwiches": ("simplesandwiches", ("#3c2a10", "#56401a", "#d2a860", "#b08840", "#8e6c30"), "#ffe070", "Brotkruste, Kaesegelb"),
    "simplequalityoflife": ("simplequalityoflife", ("#0e2a2a", "#164040", "#5ab0a6", "#3c8e86", "#2e706a"), "#8ff6e8", "Petrol, Tuerkis"),
    "simplemodels": ("simplemodels", ("#2c0f2a", "#44183f", "#c05cb0", "#9c3f8e", "#7c3070"), "#ffb0f0", "Magenta, Rosa"),
    "simpledimensions": ("simpledimension", ("#0e2640", "#163c62", "#68b0e4", "#4890c4", "#3874a2"), "#ffd36a", "Himmelblau, Glowstone"),
    "simpletweaks": ("simpletweaks", ("#1c2026", "#2c3239", "#8a96a2", "#68737e", "#525b64"), "#f05a4a", "Eisengrau, Claim-Rot"),
    "simplerespawn": ("simplerespawn", ("#300a10", "#4a1018", "#d0485a", "#a8303f", "#862430"), "#f6f6f6", "Karmesin, Verbandsweiss"),
    "simplebeacons": ("simplebeacons", ("#0a2a2c", "#124044", "#5cc4bc", "#3a9e98", "#2c7e7a"), "#fff2b0", "Prismarin, Netherstern"),
    # 2026-10-14 (oc-missingtex): these four had a guide_book item but never a texture (purple/black in the inventory).
    "simplemobheads": ("simplemobheads", ("#26241f", "#3c3a33", "#d8d2bc", "#b4ae98", "#8e8a76"), "#9cf0a0", "Knochenweiss, Creeper-Gruen"),
    "simplemobs": ("simplemobs", ("#2a0e1c", "#421830", "#a8506e", "#86385a", "#682a46"), "#7ee8ff", "Dunkelrot, Seelenfeuer-Cyan"),
    "simpleovens": ("simpleovens", ("#22201e", "#363230", "#9a9692", "#787470", "#5e5a56"), "#ff8a2a", "Ofenstein, Glutorange"),
    "simplecrucibles": ("simplecrucibles", ("#1c2123", "#2f373b", "#8a98a0", "#66747c", "#4c5860"), "#5ef0e0", "Gusseisen, Diamant-Tuerkis"),
    "simpletrims": ("simpletrims", ("#141838", "#20285a", "#6a7ed0", "#4a5eae", "#3a4a8a"), "#ffd860", "Nachtblau, Besatzgold"),
    "simplewanderers": ("simplewanderers", ("#2e1a0a", "#4a2c14", "#c08a48", "#a06c34", "#7a4e24"), "#5aa04a", "Reisemantel-Ocker, Schalgruen"),
    "simpleshops": ("simpleshops", ("#2a1a0c", "#40281a", "#b0824c", "#8e6234", "#6e4a26"), "#ffd23a", "Kassenholz, Muenzgold"),
    # 2026-10-15 (oc-simplestats)
    "simplestats": ("simplestats", ("#2e1608", "#4a2610", "#d68a50", "#b46a34", "#8e5026"), "#7ee8d0", "Kupfer, Patina-Tuerkis"),
}


def book(module):
    _, cover, gem, _ = BOOKS[module]
    key = "module_" + module
    gbt.COVERS[key] = (cover[0],) + round6.spread(cover[1:], COVER_CONTRAST)
    gbt.MEGA_GEM[key] = gem
    return round6.more_contrast(gbt.mega_guide_book(key), ICON_CONTRAST)


def target(module):
    ns = BOOKS[module][0]
    return ROOT / f"modules/{module}/shared/resources/assets/{ns}/textures/item/guide_book.png"


def main():
    args = sys.argv[1:]
    check = "--check" in args
    args = [a for a in args if a != "--check"]
    stale = []
    for module in BOOKS:
        img, path = book(module), target(module)
        if check:
            if not path.exists() or Image.open(path).convert("RGBA").tobytes() != img.tobytes():
                stale.append(str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            img.save(path)
    if check:
        if stale:
            raise SystemExit("Stale module guide textures: " + ", ".join(stale))
        print("module guide textures: up to date")
        return
    out = Path(args[0] if args else "C:/Users/o_o/code/minecraft-mods/previews/modul-buecher-vorschau.png")
    scale, cell, label_h = 16, 16 * 16 + 24, 52
    rows = (len(BOOKS) + 3) // 4
    sheet = Image.new("RGBA", (4 * cell + 16, rows * (cell + label_h) + 16), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    sb = gbt.mega_guide_book("guide").resize((64, 64), Image.Resampling.NEAREST)
    for i, module in enumerate(BOOKS):
        x, y = 8 + (i % 4) * cell, 8 + (i // 4) * (cell + label_h)
        sheet.alpha_composite(book(module).resize((16 * scale, 16 * scale), Image.Resampling.NEAREST), (x + 12, y + 4))
        sheet.alpha_composite(sb, (x + 12 + 16 * scale - 64, y + 4 + 16 * scale - 64))  # SB-Handbuch zum Vergleich
        d.text((x + 12, y + 16 * scale + 10), f"{chr(65 + i)}  {module}", fill=(20, 20, 20, 255))
        d.text((x + 12, y + 16 * scale + 26), BOOKS[module][3], fill=(60, 60, 60, 255))
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print("Vorschau:", out)


if __name__ == "__main__":
    main()
