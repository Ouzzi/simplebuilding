#!/usr/bin/env python3
"""Octant manager / HUD overlay concept previews (N64, docs/ai/KONZEPT-OKTANT-UI-2026-10-15.md).

Rebuilds UiBoxes.box (modules/simplelib/.../UiBoxes.java), UiPalette.derived and HudPanel layout pixel for pixel,
draws the before state (fill 0xA0642E, vanilla tooltip box) and 3 soft variants with the corner fix, 3x scaled,
nearest neighbour. Output: docs/ai/previews/claude-octantui/. No game assets are used (synthetic background).
Run: python3 tools/textures/octant_ui_preview.py [outdir]
"""
import os
import random
import sys
from PIL import Image, ImageDraw, ImageFont

OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "..", "docs", "ai", "previews", "claude-octantui")
RIM = (0x1E, 0x1F, 0x23)
OLD_OUTER = [3, 1, 1]      # CUT_OUTER today (rim silhouette 3,1,1,0 = not a staircase)
NEW_OUTER = [3, 2, 1]      # proposed: 3,2,1,0 = clean 45 degree staircase
BEVEL, INNER = [2, 1], [1]


def scale(c, f):
    return tuple(min(255, int(v * f)) for v in c)


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def lum(c):
    return c[0] * 0.299 + c[1] * 0.587 + c[2] * 0.114


def derived(fill):
    l = lum(fill)
    light = mix(fill, (255, 255, 255), 0.26) if l < 110 else scale(fill, 1.27) if l < 190 else mix(fill, (255, 255, 255), 0.6)
    return dict(fill=fill, light=light, label=(0xF2, 0xEE, 0xE8) if l < 125 else (0x2E, 0x30, 0x34))


def rounded(d, x, y, w, h, cuts, color, bottom=None):
    """Filled rect whose corner rows are shortened by cuts[k] (top) / bottom[k] (bottom, default = cuts)."""
    bottom = cuts if bottom is None else bottom
    for k in range(len(cuts)):
        d.rectangle([x + cuts[k], y + k, x + w - cuts[k] - 1, y + k], fill=color)
    for k in range(len(bottom)):
        d.rectangle([x + bottom[k], y + h - 1 - k, x + w - bottom[k] - 1, y + h - 1 - k], fill=color)
    d.rectangle([x, y + len(cuts), x + w - 1, y + h - len(bottom) - 1], fill=color)


def box(d, x, y, w, h, p, outer):
    s = 2
    fixed = outer is NEW_OUTER  # fix: clean diagonal also where the 2 px shadow sits at the bottom
    rounded(d, x, y, w, h, outer, RIM)
    rounded(d, x + 1, y + 1, w - 2, h - 2, BEVEL, scale(p["fill"], 0.40))
    rounded(d, x + 1, y + 1, w - 2, h - 2 - s, BEVEL, scale(p["fill"], 0.82), [3, 2, 1] if fixed else None)
    rounded(d, x + 2, y + 2, w - 4, h - 4 - s, INNER, scale(p["fill"], 0.64), [2, 1] if fixed else None)
    rounded(d, x + 4, y + 4, w - 8, h - 8 - s, INNER, p["light"])
    rounded(d, x + 5, y + 5, w - 10, h - 10 - s, [], p["fill"])


# ---- font: DejaVu Sans Mono, 1-bit, stands in for Minecraft's 8 px font (widths differ slightly)
def font(size=8):
    for f in ("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"):
        if os.path.exists(f):
            return ImageFont.truetype(f, size)
    return ImageFont.load_default()


F = font(8)


def text(d, xy, s, color, shadow=False):
    x, y = xy
    d.fontmode = "1"
    if shadow:
        d.text((x + 1, y + 1), s, fill=scale(color, 0.25), font=F)
    d.text((x, y), s, fill=color, font=F)


def twidth(s):
    return int(F.getlength(s))


# ---- synthetic game background (grass hill, sky, stone), 16 px blocks with noise, deterministic
def background(w, h, seed=7):
    rnd = random.Random(seed)
    im = Image.new("RGB", (w, h))
    px = im.load()
    for y in range(h):
        for x in range(w):
            if y < h * 0.42:
                base = (0x78, 0xA7, 0xFF) if y < h * 0.3 else (0x8F, 0xB9, 0xFF)
            elif y < h * 0.52:
                base = (0x5B, 0x8F, 0x3C)
            else:
                base = (0x84, 0x84, 0x84) if (x // 16 + y // 16) % 3 else (0x7A, 0x7A, 0x7A)
            n = rnd.randint(-9, 9)
            px[x, y] = tuple(max(0, min(255, v + n)) for v in base)
    return im


# ---- widgets in vanilla look
def button(d, x, y, w, h, label):
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=(0, 0, 0))
    d.rectangle([x + 1, y + 1, x + w - 2, y + h - 2], fill=(0x6F, 0x6F, 0x6F))
    d.rectangle([x + 1, y + 1, x + w - 2, y + 1], fill=(0xA0, 0xA0, 0xA0))
    d.rectangle([x + 1, y + 1, x + 1, y + h - 2], fill=(0xA0, 0xA0, 0xA0))
    d.rectangle([x + 1, y + h - 2, x + w - 2, y + h - 2], fill=(0x3A, 0x3A, 0x3A))
    text(d, (x + (w - twidth(label)) // 2, y + (h - 8) // 2 - 1), label, (255, 255, 255), True)


def field(d, x, y, w, h, label):
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=(0xA0, 0xA0, 0xA0))
    d.rectangle([x + 1, y + 1, x + w - 2, y + h - 2], fill=(0, 0, 0))
    text(d, (x + 4, y + (h - 8) // 2 - 1), label, (0xE0, 0xE0, 0xE0))


# ---- Octant screen (layout data of OctantScreen: padding 8, widgets 20, row step 24, content 214-16=198, summary 44)
PW, PH = 214, 190


def draw_screen(im, ox, oy, p, outer, inset_fill=(0xC6, 0xC6, 0xC6), summary_color=(0x40, 0x40, 0x40)):
    d = ImageDraw.Draw(im)
    box(d, ox, oy, PW, PH, p, outer)
    text(d, (ox + 8, oy + 6), "Octant Manager", p["label"])
    cl = ox + 8
    rows = [("P1", ("120", "64", "-33")), ("P2", ("131", "70", "-20")), ("Size", ("12", "7", "14"))]
    for i, (lab, vals) in enumerate(rows):
        y = oy + 20 + 24 * i
        text(d, (cl, y + 5), lab, p["label"])
        x = cl + 20
        for v in vals:
            button(d, x, y, 12, 20, "-")
            field(d, x + 13, y, 36, 20, v)
            button(d, x + 50, y, 12, 20, "+")
            x += 62 + 4
    y = oy + 20 + 72
    button(d, cl, y, 198 - 60 - 4, 20, "Shape: Cuboid")
    button(d, cl + 198 - 60, y, 60, 20, "Axis: +Y")
    sy = oy + 20 + 96
    d.rectangle([cl, sy, cl + 196, sy + 41], fill=scale(p["fill"], 0.82))  # inset rim
    d.rectangle([cl + 1, sy + 1, cl + 197, sy + 42], fill=(0xFF, 0xFF, 0xFF))
    d.rectangle([cl + 1, sy + 1, cl + 196, sy + 41], fill=inset_fill)
    for i, s in enumerate(["Pos 1: 120, 64, -33", "Pos 2: 131, 70, -20", "Volume: 1176 blocks³", "(12 x 7 x 14)"]):
        text(d, (cl + 5, sy + 4 + 10 * i), s, summary_color)
    by = oy + PH - 8 - 20
    button(d, cl, by, 98, 20, "Settings >>")
    button(d, cl + 100, by, 98, 20, "Done")


# ---- HUD overlay (HudPanel: padding 6 + vanilla tooltip frame; line gap 2, title gap 4)
LINES_OLD = [("Octant", (255, 255, 255)), ("Pos 1: 120, 64, -33", (0xFF, 0xAD, 0x33)), ("Pos 2: 131, 70, -20", (0xFF, 0x7F, 0x00)),
             ("Volume: 1176 blocks³", (0xFF, 0xAD, 0x33)), ("(12 x 7 x 14)", (0xAA, 0xAA, 0xAA))]


def overlay_size(lines, pad, title_gap):
    cw = max(twidth(s) for s, _ in lines)
    ch = 8 + title_gap + (len(lines) - 1) * 10 - 2
    return cw, ch


def draw_overlay_old(im, ox, oy):
    d = ImageDraw.Draw(im, "RGBA")
    cw, ch = overlay_size(LINES_OLD, 6, 4)
    x, y = ox + 6, oy + 6  # content origin; tooltip frame = 3 px outside content, padding 6 modelled by caller
    # vanilla tooltip: background 0xF0100010, frame gradient 0x505000FF -> 0x5028007F, 3 px beyond content
    bx0, by0, bx1, by1 = x - 3, y - 3, x + cw + 3, y + ch + 3
    d.rectangle([bx0, by0 - 1, bx1 - 1, by1], fill=(0x10, 0, 0x10, 0xF0))
    d.rectangle([bx0 - 1, by0, bx1, by1 - 1], fill=(0x10, 0, 0x10, 0xF0))
    d.rectangle([bx0, by0, bx1 - 1, by1 - 1], fill=(0x10, 0, 0x10, 0xF0))
    for yy in range(by0, by1):
        t = (yy - by0) / max(1, by1 - by0 - 1)
        c = (int(0x50 + (0x28 - 0x50) * t), 0, int(0xFF + (0x7F - 0xFF) * t), 0x50)
        d.point((bx0, yy), fill=c)
        d.point((bx1 - 1, yy), fill=c)
    d.rectangle([bx0, by0, bx1 - 1, by0], fill=(0x50, 0, 0xFF, 0x50))
    d.rectangle([bx0, by1 - 1, bx1 - 1, by1 - 1], fill=(0x28, 0, 0x7F, 0x50))
    yy = y
    for i, (s, c) in enumerate(LINES_OLD):
        text(d, (x, yy), s, c, True)
        yy += 8 + (4 if i == 0 else 2) - (0 if i == 0 else 0)
        if i > 0:
            yy += 0
    return (bx1 - bx0, by1 - by0)


def new_colors(p):
    f = p["fill"]
    return dict(bg=scale(f, 0.20), border=scale(f, 0.64), title=(255, 255, 255), v1=mix(f, (255, 255, 255), 0.8),
                v2=mix(f, (255, 255, 255), 0.6), sec=(0xB8, 0xB8, 0xB8))


def draw_overlay_new(im, ox, oy, p, alpha=0xC8):
    c = new_colors(p)
    lines = [("Octant", c["title"]), ("Pos 1: 120, 64, -33", c["v1"]), ("Pos 2: 131, 70, -20", c["v2"]),
             ("Volume: 1176 blocks³", c["v1"]), ("(12 x 7 x 14)", c["sec"])]
    cw, ch = overlay_size(lines, 4, 3)
    pad = 4
    w, h = cw + 2 * pad, ch + 2 * pad
    lay = Image.new("RGBA", im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    # 1 px frame in band colour + fill; corners cut 1 px (single stair step); alpha = overlay opacity
    rounded(d, ox, oy, w, h, [1], c["border"] + (alpha,))
    rounded(d, ox + 1, oy + 1, w - 2, h - 2, [0], c["bg"] + (alpha,))
    d.rectangle([ox + 1, oy + 1, ox + w - 2, oy + 1], fill=scale(p["fill"], 0.50) + (alpha,))  # faint top line
    im.paste(Image.alpha_composite(im.convert("RGBA"), lay).convert("RGB"))
    d2 = ImageDraw.Draw(im)
    yy = oy + pad
    for i, (s, col) in enumerate(lines):
        text(d2, (ox + pad, yy), s, col, True)
        yy += 8 + (3 if i == 0 else 2)
    return (w, h)


def contrast(fg, bg):
    def rl(c):
        v = []
        for x in c:
            x /= 255
            v.append(x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4)
        return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2]
    a, b = rl(fg), rl(bg)
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)


VARIANTS = {
    "A-sand": (0xB9, 0xA2, 0x7E),
    "B-bronze": (0x6F, 0x5F, 0x4B),
    "C-salbei": (0x7F, 0x8F, 0x8A),
}
OLD_FILL = (0xA0, 0x64, 0x2E)
S = 3


def up(im, s):
    return im.resize((im.width * s, im.height * s), Image.NEAREST)


def sheet(before, after, names):
    w = before.width + after.width + 30
    h = max(before.height, after.height) + 24
    out = Image.new("RGB", (w, h), (24, 24, 24))
    out.paste(before, (6, 18))
    out.paste(after, (before.width + 24, 18))
    d = ImageDraw.Draw(out)
    d.text((8, 4), names[0], fill=(255, 255, 255), font=ImageFont.load_default())
    d.text((before.width + 26, 4), names[1], fill=(255, 255, 255), font=ImageFont.load_default())
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    cw_, ch_ = PW + 24, PH + 24
    bg = background(cw_, ch_)
    old = bg.copy()
    draw_screen(old, 12, 12, derived(OLD_FILL), OLD_OUTER)
    # corner detail 1:1 gui px, before / after, shown at 12x
    det = []
    for outer, fill in ((OLD_OUTER, OLD_FILL),):
        pass
    for name, fill in VARIANTS.items():
        p = derived(fill)
        new = bg.copy()
        draw_screen(new, 12, 12, p, NEW_OUTER)
        sheet(up(old, S), up(new, S), ("vorher: #A0642E, Ecken 3/1/1", "nachher %s #%02X%02X%02X, Ecken 3/2/1" % (name, *fill))).save(os.path.join(OUT, "screen-%s.png" % name))
    # overlay: world background crop, crosshair on right of it
    obg = background(150, 90, seed=11)
    o_old = obg.copy()
    draw_overlay_old(o_old, 10, 8)
    for name, fill in VARIANTS.items():
        p = derived(fill)
        o_new = obg.copy()
        draw_overlay_new(o_new, 13, 11, p)
        sheet(up(o_old, 4), up(o_new, 4), ("vorher: Vanilla-Tooltip, orange", "nachher %s, ~78 %% Deckkraft" % name)).save(os.path.join(OUT, "overlay-%s.png" % name))
    # corner detail: bottom-left, 10x10 gui px at 16x, before | after
    c_old = Image.new("RGB", (14, 14), (0x50, 0x4C, 0x7A))
    ImageDraw.Draw(c_old)
    c_new = c_old.copy()
    tmp_o = Image.new("RGB", (40, 30), (0x50, 0x4C, 0x7A))
    tmp_n = tmp_o.copy()
    box(ImageDraw.Draw(tmp_o), 6, -20, 60, 46, derived(OLD_FILL), OLD_OUTER)
    box(ImageDraw.Draw(tmp_n), 6, -20, 60, 46, derived(OLD_FILL), NEW_OUTER)
    sheet(up(tmp_o.crop((0, 8, 22, 30)), 14), up(tmp_n.crop((0, 8, 22, 30)), 14), ("Ecke unten links vorher (3/1/1)", "nachher (3/2/1)")).save(os.path.join(OUT, "ecke-detail.png"))
    # contrast numbers (text on overlay background blended over worst-case sky/white/black backgrounds)
    print("variant  bg-alpha  white-on-bg(dark world)  white-on-bg(bright #FFFFFF world)  value-colour-on-bright")
    for name, fill in VARIANTS.items():
        p = derived(fill)
        c = new_colors(p)
        a = 0xC8 / 255
        for wl, wc in (("dark", (0, 0, 0)), ("bright", (255, 255, 255))):
            eff = mix(wc, c["bg"], a)
            print(name, wl, "eff-bg=%s" % (eff,), "title %.1f" % contrast(c["title"], eff), "v1 %.1f" % contrast(c["v1"], eff),
                  "v2 %.1f" % contrast(c["v2"], eff), "sec %.1f" % contrast(c["sec"], eff))
        print(name, "screen label on fill %.1f" % contrast(p["label"], p["fill"]), "light", p["light"])
    p = derived(OLD_FILL)
    print("OLD label on fill %.1f" % contrast(p["label"], p["fill"]), "FF7F00 on F0100010 %.1f" % contrast((0xFF, 0x7F, 0), (0x10, 0, 0x10)))


if __name__ == "__main__":
    main()
