"""Sculk-Kiefer, geschlossene Form, Runde 2 (Besitzer: "aktuell ist der beste"): Feinschliff NAH an der N34-Form.

Alle Varianten behalten die zwei Sculk-Lippen und die verzahnten Fangzaehne; Zeile 1 ist die eingebaute Form.
  E Palette: gleiche Form, Vanilla-Sculk-Palette, Lippen oben mit hellerer Kante und Shrieker-Leuchtpunkten.
  F Spitze Zaehne: mehr, schmalere Fangzaehne (4 + 4) mit dreistufig zulaufender Spitze.
  G Runde Kontur: Lippen in vier flacheren Stufen (weichere Kuppel), Naht leuchtet schwach.
  H Hakenzaehne: Fangzaehne greifen ueber die Naht auf die andere Lippe, darunter leuchtender Spalt.
  I Zahnfleischrand: schmaler Knochenrand entlang der Naht, Zaehne kuerzer, Adern auf der Lippe.

  python3 tools/textures/sculk_jaw_variants2_2026_10_11.py   # schreibt ~/previews/n34/jaw-vorschlaege2.png
Renderer, Palette und Texturbaukasten kommen aus sculk_jaw_variants_2026_10_11.py (Runde 1).
"""
import importlib.util
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent


def _load(name, file):
    spec = importlib.util.spec_from_file_location(name, HERE / file)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


v1 = _load("jaw_variants1", "sculk_jaw_variants_2026_10_11.py")
base = v1.base
box, GUM, TOOTH, FLOOR = v1.box, v1.GUM, v1.TOOTH, v1.FLOOR
S = v1.S
OUT = v1.OUT.with_name("jaw-vorschlaege2.png")
SCULK, GLOW, BONE = v1.SCULK, v1.GLOW, v1.BONE


def lips(layers):
    """Zwei Lippen aus (x-Innenkante-Abstand vom Rand, y0, y1, z-Einzug) je Stufe; obere Flaechen mit Top-Textur."""
    els = [box([0, 0, 0], [16, 2, 16], GUM, FLOOR)]
    for half in (0, 1):
        for x0, y0, y1, zi in layers:
            a, b = (x0, 8) if half == 0 else (8, 16 - x0)
            els.append(box([a, y0, zi], [b, y1, 16 - zi], GUM, FLOOR))
    return els


N34_LIPS = [(0, 2, 4, 0), (1, 4, 6, 1), (3, 6, 7, 2.5)]


def fangs(zl, zr, w=2.0, h=1.5, tip=1.5, taper=False):
    els = []
    for z in zl:
        els.append(box([5.5, 7, z], [8.5, 7 + h, z + w], TOOTH))
        els.append(box([7, 7 + h, z + 0.25 * w], [9, 7 + h + tip, z + 0.75 * w], TOOTH))
        if taper:
            els.append(box([7.5, 7 + h + tip, z + 0.375 * w], [8.5, 7 + h + tip + 1, z + 0.625 * w], TOOTH))
    for z in zr:
        els.append(box([7.5, 7, z], [10.5, 7 + h, z + w], TOOTH))
        els.append(box([7, 7 + h, z + 0.25 * w], [9, 7 + h + tip, z + 0.75 * w], TOOTH))
        if taper:
            els.append(box([7.5, 7 + h + tip, z + 0.375 * w], [8.5, 7 + h + tip + 1, z + 0.625 * w], TOOTH))
    return els


def tex_lip(seed, veins, dots, bone_style="fang", rim=True):
    """Runde-1-Baukasten plus Lippenkante: oberste Zeile des Top-Felds heller, Rand unten dunkel."""
    img = v1.tex(seed, veins, bone_style, dots)
    px = img.load()
    if rim:
        for x in range(16):
            px[x, 8] = SCULK[4]
            px[x, 15] = SCULK[0]
        for y in range(8):
            px[0, y] = SCULK[4] if y < 2 else px[0, y]
    return img


def var_e():
    return lips(N34_LIPS) + fangs((1.5, 5.5, 9.5), (3.5, 7.5, 11.5))


def var_f():
    return lips(N34_LIPS) + fangs((1.5, 4.75, 8.0, 11.25), (3.125, 6.375, 9.625, 12.75), w=1.5, h=1.0, tip=1.5, taper=True)


def var_g():
    return lips([(0, 2, 3.5, 0), (0.5, 3.5, 5, 0.5), (1.5, 5, 6, 1.5), (3, 6, 7, 2.5)]) + \
        [box([7.75, 6.9, 3], [8.25, 7.05, 13], GUM, FLOOR)] + fangs((1.5, 5.5, 9.5), (3.5, 7.5, 11.5))


def var_h():
    els = lips(N34_LIPS) + [box([7.5, 6.8, 2.5], [8.5, 7, 13.5], GUM, FLOOR)]
    for z in (2, 6, 10):  # links verwurzelt, Spitze haengt rechts ueber die Naht
        els += [box([6, 7, z], [8, 8.5, z + 1.5], TOOTH), box([8, 7.5, z + 0.25], [9.5, 8.5, z + 1.25], TOOTH),
                box([9, 7, z + 0.5], [9.75, 7.5, z + 1], TOOTH)]
    for z in (4, 8, 12):
        els += [box([8, 7, z], [10, 8.5, z + 1.5], TOOTH), box([6.5, 7.5, z + 0.25], [8, 8.5, z + 1.25], TOOTH),
                box([6.25, 7, z + 0.5], [7, 7.5, z + 1], TOOTH)]
    return els


def var_i():
    els = lips(N34_LIPS)
    for half in (0, 1):
        a, b = (6.5, 7.5) if half == 0 else (8.5, 9.5)
        els.append(box([a, 7, 2.5], [b, 7.5, 13.5], TOOTH))
    return els + fangs((2, 6, 10), (4, 8, 12), w=1.5, h=1.0, tip=1.0)


def main():
    rows = [
        ("aktuell (N34)", base.jaw(True)["elements"], base.texture()),
        ("E Palette + Lippenkante", var_e(), tex_lip(51, [], [(2, 2), (5, 5), (6, 1), (3, 3), (12, 5), (7, 6)])),
        ("F spitze Zaehne 4+4", var_f(), tex_lip(53, [], [(1, 4), (6, 2), (4, 6), (5, 4), (11, 2)])),
        ("G runde Kontur", var_g(), tex_lip(57, [[(4, 2), (5, 3), (6, 3), (7, 4), (8, 4), (9, 4), (10, 3), (11, 3), (12, 2)]],
                                            [(2, 5), (5, 1), (6, 6), (2, 6), (14, 6)])),
        ("H Hakenzaehne", var_h(), tex_lip(61, [[(7, 1), (7, 2), (8, 3), (8, 4), (8, 5), (7, 6)]], [(1, 2), (6, 5), (3, 7), (3, 2), (12, 5)])),
        ("I Zahnfleischrand", var_i(), tex_lip(67, [[(1, 2), (2, 3), (3, 3), (4, 4)], [(14, 2), (13, 3), (12, 3), (11, 4)]],
                                               [(2, 2), (6, 4), (4, 6), (7, 5), (8, 6)], bone_style="shrieker")),
    ]
    iw, ih = 30 * S, 24 * S
    out = Image.new("RGB", (iw + 32 * S + 60, len(rows) * (ih + 10)), (30, 30, 34))
    d = ImageDraw.Draw(out)
    for i, (name, els, img) in enumerate(rows):
        y = i * (ih + 10)
        out.paste(v1.iso(els, img), (0, y))
        out.paste(v1.top(els, img), (iw + 20, y + 30))
        out.paste(img.convert("RGB").resize((16 * S, 16 * S), Image.NEAREST), (iw + 16 * S + 40, y + 30))
        d.text((8, y + 6), name, fill=(235, 235, 235))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    out.save(OUT)
    print("preview", OUT)


if __name__ == "__main__":
    main()
