"""
Vanilla-Naehe der eigenen Texturen (PRINZIPIEN.md A8 a, Queue b-layers1 Teil 2): eine Heuristik, die meldet, wo eine
Textur deutlich vom Minecraft-Stil abweicht. Aendert keine Pixel; Ausgabe Markdown fuer den Besitzer.

    python tools/textures/vanilla_nearness_audit.py [--out docs/ai/AUDIT-VANILLA-NAEHE-2026-10-21.md] [--sheet X.png]

Merkmale (Vanilla-Massstab: Items 16x16 mit 1-px-Kontur, Licht von oben links, keine Halbtransparenz, nie reines
Schwarz, je Material 4-6 Stufen; Bloecke deckend, leicht verrauscht):
  halbtransparent  Pixel mit 0 < Alpha < 255 (Vanilla nur bei Glas/Eis/Portalen)
  schwarz          reines #000000 (Vanilla nie)
  kontur           Item: Randpixel (an Transparenz) im Mittel nicht dunkler als das Innere
  licht            unten rechts deutlich heller als oben links (Vanilla: Licht von oben links)
  farben           16x16 mit mehr als 64 Farben (Verlauf/Weichzeichnung) bzw. hoechstens 3 (flach)
  grell            mittlere Saettigung > 0.75 bei mittlerer Helligkeit > 0.5
  kontrast         Helligkeitsspanne < 0.12 (flach, ohne Schattierung)
"""
from __future__ import annotations

import argparse
import colorsys
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402

ROOT = layers.ROOT


def luma(r, g, b):
    return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255


# Masks tinted at run time (dye/trim palettes) and GUI sheets are grey or flat on purpose: not judged.
MASKS = ("_dyed", "overlay", "tint", "mask", "color_palettes", "/trims/", "/gui/", "/font/", "/colormap/",
         "_glow", "_eyes", "emissive")


def features(w: int, h: int, rgba: bytes, rel: str) -> list[str]:
    if any(k in rel for k in MASKS):
        return []
    px = [tuple(rgba[i * 4:i * 4 + 4]) for i in range(w * h)]
    opaque = [i for i, p in enumerate(px) if p[3]]
    if not opaque:
        return []
    flags = []
    semi = sum(1 for i in opaque if px[i][3] < 255)
    if semi and not any(k in rel for k in ("glass", "ice", "portal", "overlay", "glint", "effect", "particle", "shimmer")):
        flags.append(f"halbtransparent ({semi} px)")
    if any(px[i][:3] == (0, 0, 0) and px[i][3] == 255 for i in opaque):
        flags.append("schwarz")
    lum = {i: luma(*px[i][:3]) for i in opaque}
    is_item = "/item/" in rel and w == h == 16
    if is_item:
        def edge(i):
            x, y = i % w, i // w
            return any(not (0 <= x + dx < w and 0 <= y + dy < h) or px[(y + dy) * w + x + dx][3] == 0
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        rim = [lum[i] for i in opaque if edge(i)]
        inner = [lum[i] for i in opaque if not edge(i)]
        if rim and inner and len(inner) >= 8 and sum(rim) / len(rim) >= sum(inner) / len(inner):
            flags.append("kontur")
    half_w, half_h = w // 2, h // 2
    tl = [lum[i] for i in opaque if i % w < half_w and i // w < half_h]
    br = [lum[i] for i in opaque if i % w >= half_w and i // w >= half_h]
    if len(tl) >= 8 and len(br) >= 8 and sum(br) / len(br) - sum(tl) / len(tl) > 0.12:
        flags.append("licht")
    colours = len({px[i] for i in opaque})
    if w == h == 16 and colours > 64:
        flags.append(f"farben ({colours})")
    elif w * h >= 64 and len(opaque) >= 32 and colours <= 3:
        flags.append(f"flach ({colours} Farben)")
    sats = [colorsys.rgb_to_hsv(*(c / 255 for c in px[i][:3]))[1] for i in opaque]
    if sum(sats) / len(sats) > 0.75 and sum(lum.values()) / len(lum) > 0.5:
        flags.append("grell")
    if len(opaque) >= 32 and max(lum.values()) - min(lum.values()) < 0.12:
        flags.append("kontrast")
    return flags


def scan() -> list[tuple[str, int, int, list[str]]]:
    out = []
    for png in layers.own_textures():
        rel = str(png.relative_to(ROOT))
        try:
            w, h, rgba = layers.png_read(png.read_bytes())
        except (ValueError, KeyError) as e:
            out.append((rel, 0, 0, [f"unlesbar ({e})"]))
            continue
        flags = features(w, h, rgba, rel)
        if flags:
            out.append((rel, w, h, flags))
    return out


def sheet(rels: list[str], path: Path, scale: int = 6, cols: int = 12) -> None:
    """Contact sheet of 16x16-ish textures (first frame), nearest-neighbour, grey checker behind."""
    cell = 16 * scale + 4
    rows = (len(rels) + cols - 1) // cols
    W, H = cols * cell, rows * cell
    img = bytearray()
    canvas = [[(60, 60, 60, 255) if ((x // 8 + y // 8) % 2) else (90, 90, 90, 255) for x in range(W)] for y in range(H)]
    for n, rel in enumerate(rels):
        w, h, rgba = layers.png_read((ROOT / rel).read_bytes())
        s = max(1, 16 * scale // max(w, min(h, w)))
        ox, oy = (n % cols) * cell + 2, (n // cols) * cell + 2
        for y in range(min(h, w) * s):
            for x in range(w * s):
                if x >= 16 * scale or y >= 16 * scale:
                    continue
                i = ((y // s) * w + x // s) * 4
                r, g, b, a = rgba[i:i + 4]
                if a:
                    br, bg, bb, _ = canvas[oy + y][ox + x]
                    canvas[oy + y][ox + x] = (r * a // 255 + br * (255 - a) // 255, g * a // 255 + bg * (255 - a) // 255,
                                              b * a // 255 + bb * (255 - a) // 255, 255)
    for row in canvas:
        for p in row:
            img += bytes(p)
    path.write_bytes(layers.png_write(W, H, bytes(img)))


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--out")
    ap.add_argument("--sheet", help="contact sheet of the textures with two or more flags")
    a = ap.parse_args()
    hits = scan()
    total = len(layers.own_textures())
    hits.sort(key=lambda t: (-len(t[3]), t[0]))
    lines = [f"Gepruefte eigene Texturen: {total}, auffaellig: {len(hits)}", ""]
    by_flag: dict[str, int] = {}
    for _, _, _, flags in hits:
        for f in flags:
            by_flag[f.split(" ")[0]] = by_flag.get(f.split(" ")[0], 0) + 1
    lines += ["| Merkmal | Anzahl |", "|---|---|"] + [f"| {k} | {v} |" for k, v in sorted(by_flag.items(), key=lambda kv: -kv[1])]
    lines += ["", "| Textur | Groesse | Merkmale |", "|---|---|---|"]
    lines += [f"| `{rel}` | {w}x{h} | {', '.join(flags)} |" for rel, w, h, flags in hits]
    text = "\n".join(lines) + "\n"
    if a.out:
        Path(a.out).write_text(text, encoding="utf-8", newline="\n")
    else:
        sys.stdout.write(text)
    if a.sheet:
        sheet([rel for rel, _, _, flags in hits if len(flags) >= 2], Path(a.sheet))
    return 0


if __name__ == "__main__":
    sys.exit(main())
