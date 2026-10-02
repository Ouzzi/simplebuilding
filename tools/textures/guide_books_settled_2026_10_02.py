"""Besitzerwahl 2026-10-02 abends: Handbuch-Texturen Vorschlag J "Prachtband" fuer alle 20 Buecher (26.3-Overlay).

Die Zeichnung selbst steht in guide_book_textures.mega_guide_book (dieselbe Quelle, die generate_textures.py
schreibt und mit --check prueft). Dieser Schritt schreibt die Texturen und eine Vorher/Nachher-Vorschau;
"vorher" kommt aus `git show <rev>:<pfad>` (Standard `be4176b4`, der letzte Stand vor dem Einbau).

Aufruf (Pillow): python tools/textures/guide_books_settled_2026_10_02.py [vorschau.png] [--rev REV]
"""
import io
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
from guide_book_textures import ORDER, mega_guide_book, mega_guide_textures  # noqa: E402

ITEM = "mc26_3/overlay/resources/assets/simplebuilding/textures/item/"


def old_image(name, rev):
    data = subprocess.run(["git", "show", f"{rev}:{ITEM}{name}"], cwd=ROOT, capture_output=True, check=True).stdout
    return Image.open(io.BytesIO(data)).convert("RGBA")


def main():
    args = [a for a in sys.argv[1:]]
    rev = "be4176b4"
    if "--rev" in args:
        i = args.index("--rev")
        rev = args[i + 1]
        del args[i:i + 2]
    out = Path(args[0] if args else "C:/Users/o_o/code/minecraft-mods/previews/guide-buecher-J-eingebaut.png")
    mega_guide_textures(check=False)

    scale, cell, label_w, head = 8, 136, 170, 26
    # two topics per row: [label | vorher | nachher] x 2
    sheet = Image.new("RGBA", (2 * (label_w + 2 * cell) + 16, head + (len(ORDER) + 1) // 2 * cell + 8), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    for half in range(2):
        x0 = half * (label_w + 2 * cell + 8)
        d.text((x0 + label_w + 8, 6), "vorher", fill=(30, 30, 30, 255))
        d.text((x0 + label_w + cell + 8, 6), "nachher (J)", fill=(30, 30, 30, 255))
    for i, topic in enumerate(ORDER):
        name = "guide_book.png" if topic == "guide" else f"guide_book_{topic}.png"
        x0 = (i % 2) * (label_w + 2 * cell + 8)
        y = head + (i // 2) * cell
        d.text((x0 + 8, y + cell // 2 - 6), topic, fill=(20, 20, 20, 255))
        before = old_image(name, rev)
        after = mega_guide_book(topic)
        sheet.alpha_composite(before.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST), (x0 + label_w + 4, y + 4))
        sheet.alpha_composite(after.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST), (x0 + label_w + cell + 4, y + 4))
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print("Vorschau:", out)


if __name__ == "__main__":
    main()
