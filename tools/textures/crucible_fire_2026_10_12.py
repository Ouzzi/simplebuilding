"""Crucible fire preview (queue N14 "Tiegel-Feuer"): tongues swing between POINTED and BROAD, bigger heat levels,
plus the GUI backgrounds of all tiers without/with barrel and no rendered items.

References (shape variation in place, not upward drift; 4-6 key shapes looping; temperature ramp white/yellow/orange/red):
  https://www.sprite-ai.art/gallery/fire , https://pixie.haus/pixelart/154800/fiery-flame-sprite-animation-frames ,
  https://tips.clip-studio.com/en-us/articles/8096 ; motion only from the owner's anime-fire references (uppbeat.io).
Mirrors UiFlames.height/morph and CrucibleFlames.targetPixels (keep in step), CrucibleMenu.layout, UiBoxes.box/slot.
Usage: python3.12 tools/textures/crucible_fire_2026_10_12.py [OUT_DIR]   (default /tmp/claude-0/sbx/crucible-fire)
"""
import base64, io, math, sys
from pathlib import Path
from PIL import Image, ImageDraw

sys.argv, argv = sys.argv[:1], sys.argv[1:]
sys.path.insert(0, str(Path(__file__).parent))
import crucible_flames_compare_2026_10_09 as C

OUT = Path(argv[0]) if argv else Path("/tmp/claude-0/sbx/crucible-fire")
P = C.SHAPE_PARAMS
lerp = lambda a, b, m: a + (b - a) * m


def morph(j, t, calm):
    s = math.sin(t * (2.4 if calm == 0 else 1.5) + j * 2.7)
    return 0.5 + 0.5 * math.copysign(abs(s) ** 0.7, s)


def flame_height(col, millis, target, calm, shape=None):
    t = millis / 1000.0 * (1.0 if calm == 0 else 0.55 if calm == 1 else 0.35)
    flick = millis // C.flick_ms(calm)
    sp = P["MITTEL"]["spacing"]
    best, first = 0.0, col // sp - 1
    for j in range(first, first + 3):
        center = j * sp + sp // 2 + 1.5 * math.sin(t * 1.7 + j * 2.1)
        m = morph(j, t, calm)
        h = j
        half = lerp(4.5, 6.5, m) + (C.java_hash(j, 5) & 3) * lerp(0.5, 0.6, m)
        skew = 0.3 if (C.java_hash(j, 3) & 1) == 0 else -0.3
        off = col + 0.5 - center
        d = abs(off) / (half * (1 + skew if off < 0 else 1 - skew))
        if d >= 1:
            continue
        amp = (0.78 + 0.3 * math.sin(t * 2.3 + j * 1.7) + 0.14 * math.sin(t * 5.1 + j * 0.9)
               + (C.java_hash(j, 9) & 3) * 0.06) if calm == 0 else (
               0.8 + 0.12 * math.sin(t * 2.3 + j * 1.7) + (C.java_hash(j, 9) & 3) * 0.04)
        best = max(best, lerp(0.75, 0.5, m) * amp * (1 - d) ** lerp(1.5, 0.6, m))
    jitter = 1 if calm == 0 and (C.java_hash(col, flick) & 3) == 0 else 0
    return max(2, min(target * 7 // 5, C.jround(target * (0.375 + best)) + jitter))


def target_pixels(heat, glowing, section):
    if glowing:
        return 0 if heat == "none" else max(3, C.jround(section / 12.0))
    return C.jround(section * {"none": 0.0, "medium": 1 / 4.8, "high": 1 / 2.7, "extreme": 1 / 2.4}[heat])


C.flame_height, C.target_pixels = flame_height, target_pixels

# --- GUI backgrounds -------------------------------------------------------------------------------------------
def sc(c, f): return tuple(min(255, int(v * f)) for v in c)
def mix(a, b, t): return tuple(int(x * (1 - t) + y * t) for x, y in zip(a, b))
H = lambda v: ((v >> 16) & 255, (v >> 8) & 255, v & 255)
PAL = {  # fill, light, slot, slotTop
    "iron": (0x9A9DA2, 0xC4C7CB, 0x7B7E83, 0x64676C), "reinforced": (0x6F9095, 0x9DBCC1, 0x55737A, 0x425C61),
    "netherite": (0x5F524C, 0x867870, 0x473C37, 0x352C28), "enderite": (0x8E6CB0, 0xB99AD6, 0x70538E, 0x594073),
    "barrel": (0xB9774F, 0xD08F68, 0x94573A, 0x74412B), "inv": (0xC6C6C6, 0xFFFFFF, 0x8B8B8B, 0x373737)}
RIM = (0x1E, 0x1F, 0x23)
TIERS = {"iron": (1, 2), "reinforced": (2, 2), "netherite": (2, 3), "enderite": (2, 5)}


def rounded(d, x, y, w, h, cuts, col):
    n = len(cuts)
    for k, c in enumerate(cuts):
        d.rectangle((x + c, y + k, x + w - c - 1, y + k), fill=col)
        d.rectangle((x + c, y + h - 1 - k, x + w - c - 1, y + h - 1 - k), fill=col)
    d.rectangle((x, y + n, x + w - 1, y + h - n - 1), fill=col)


def box(d, x, y, w, h, key):
    fill, light = H(PAL[key][0]), H(PAL[key][1])
    rounded(d, x, y, w, h, [3, 1, 1], RIM)
    rounded(d, x + 1, y + 1, w - 2, h - 2, [2, 1], sc(fill, .40))
    rounded(d, x + 1, y + 1, w - 2, h - 4, [2, 1], sc(fill, .82))
    rounded(d, x + 2, y + 2, w - 4, h - 6, [1], sc(fill, .64))
    rounded(d, x + 4, y + 4, w - 8, h - 10, [1], light)
    rounded(d, x + 5, y + 5, w - 10, h - 12, [], fill)


def slot(d, x, y, key):
    s, sl, top, lt = 16, H(PAL[key][2]), H(PAL[key][3]), H(PAL[key][1])
    d.rectangle((x + 1, y + s, x + s - 1, y + s), fill=lt)
    d.rectangle((x + s, y + 1, x + s, y + s - 1), fill=lt)
    d.rectangle((x + 1, y, x + s - 2, y + s - 1), fill=sl)
    d.rectangle((x, y + 1, x, y + s - 2), fill=sc(top, 1.12))
    d.rectangle((x + s - 1, y + 1, x + s - 1, y + s - 2), fill=sl)
    d.rectangle((x + 1, y, x + s - 2, y), fill=top)


def screen(tier, barrel, heat, glowing, millis, flames=True, slots=True):
    g, rows = TIERS[tier]
    gw = g * 3 * 18
    cw = bw = gw + 14
    group = cw + (2 + bw if barrel else 0)
    sec = 18 + rows * 18 + 12
    W = max(176, group)
    Ht = sec + 2 + 100
    img = Image.new("RGB", (W, Ht), (31, 34, 40))
    d = ImageDraw.Draw(img)
    left = (W - group) // 2
    box(d, left, 0, cw, sec, tier)
    if barrel:
        box(d, left + cw + 2, 0, bw, sec, "barrel")
    box(d, (W - 176) // 2, sec + 2, 176, 100, "inv")
    if flames and heat != "none":
        C.draw_flames(d, left + 5, sec - 7, cw - 10, sec, heat, glowing, millis, "MITTEL")
    for r in (range(rows) if slots else ()):
        for c in range(g * 3):
            slot(d, left + 7 + 1 + c * 18, 18 + 1 + r * 18, tier)
            if barrel:
                slot(d, left + cw + 2 + 7 + 1 + c * 18, 18 + 1 + r * 18, "barrel")
    ix = (W - 176) // 2 + 8
    for r in range(3):
        for c in range(9): slot(d, ix + c * 18, sec + 2 + 17 + r * 18, "inv")
    for c in range(9): slot(d, ix + c * 18, sec + 2 + 17 + 58, "inv")
    return img


def up(im, k=3): return im.resize((im.width * k, im.height * k), Image.Resampling.NEAREST)
def uri(im, fmt="PNG", **kw):
    b = io.BytesIO(); im.save(b, fmt, **kw)
    return f"data:image/{fmt.lower()};base64," + base64.b64encode(b.getvalue()).decode()


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    N, MS = 24, 80
    heats = C.HEATS
    # animation: iron tier panel (crucible box only) per heat, stacked; 24 frames
    def strip_frame(f, tier="netherite"):
        panels = [screen(tier, False, h, gl, f * MS, slots=False) for _, h, gl in heats]
        cw = panels[0].width
        crop = [p.crop((0, 0, p.width, 18 + TIERS[tier][1] * 18 + 12)) for p in panels]
        sheet = Image.new("RGB", (cw * len(crop), crop[0].height), (31, 34, 40))
        for i, p in enumerate(crop): sheet.paste(p, (i * cw, 0))
        return sheet
    frames = [up(strip_frame(f)) for f in range(N)]
    frames[0].save(OUT / "tiegel-feuer.gif", save_all=True, append_images=frames[1:], duration=MS, loop=0)
    # PNG strip: 8 key frames, one per row, heats side by side
    keys = [up(strip_frame(f * 3), 2) for f in range(8)]
    st = Image.new("RGB", (keys[0].width, keys[0].height * 8))
    for i, k in enumerate(keys): st.paste(k, (0, i * k.height))
    st.save(OUT / "tiegel-feuer-streifen.png")
    # backgrounds
    bgs = []
    for tier in TIERS:
        for barrel in (False, True):
            im = screen(tier, barrel, "high", False, 700, flames=False)
            name = f"tiegel-{tier}-{'fass' if barrel else 'ohne-fass'}"
            up(im, 2).save(OUT / f"{name}.png")
            fl = screen(tier, barrel, "high", False, 700)
            bgs.append((tier, barrel, uri(up(im, 2)), uri(up(fl, 2))))
    heat_rows = []
    for tier in ("iron", "enderite"):
        row = [screen(tier, False, h, gl, 700).crop((0, 0, 9999, 18 + TIERS[tier][1] * 18 + 12)) for _, h, gl in heats]
        sh = Image.new("RGB", (sum(r.width for r in row), row[0].height)); x = 0
        for r in row: sh.paste(r, (x, 0)); x += r.width
        heat_rows.append((tier, uri(up(sh, 2))))
    cards = "".join(f'<figure><img src="{b}" alt=""><figcaption>{t} {"mit Fass" if bar else "ohne Fass"} (leer)</figcaption></figure>'
                    f'<figure><img src="{f}" alt=""><figcaption>{t} {"mit Fass" if bar else "ohne Fass"} + Feuer hoch</figcaption></figure>'
                    for t, bar, b, f in bgs)
    hr = "".join(f'<figure><img src="{u}" alt=""><figcaption>{t}: mittel, hoch, extrem (blau), Nachglühen</figcaption></figure>' for t, u in heat_rows)
    html = f"""<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Tiegel-Feuer</title><style>
:root{{--bg:#fff;--fg:#1d2026;--mut:#5b6270;--card:#f1f3f6}}
@media(prefers-color-scheme:dark){{:root:not([data-theme=light]){{--bg:#16181d;--fg:#e8eaee;--mut:#9aa1ae;--card:#22252c}}}}
:root[data-theme=dark]{{--bg:#16181d;--fg:#e8eaee;--mut:#9aa1ae;--card:#22252c}}
body{{background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif;margin:0;padding:16px;max-width:1100px;margin-inline:auto}}
img{{image-rendering:pixelated;max-width:100%;height:auto}}figure{{margin:0;background:var(--card);padding:8px;border-radius:8px}}
figcaption{{color:var(--mut);font-size:13px}}.g{{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:12px}}
h2{{margin-top:28px}}p{{color:var(--mut)}}</style></head><body>
<h1>Tiegel-Feuer</h1>
<p>Zungen wechseln einzeln und phasenversetzt zwischen spitz und breit (scharfer Sinus: verweilt an den Formen, geht schnell durch die stumpfe Mitte). Jede Hitzestufe ist groesser (mittel 1/4,8 statt 1/6,5; hoch 1/2,7 statt 1/3; extrem 1/2,4 statt 1,1/3; Nachgluehen 1/12 statt 1/16 der Boxhoehe).</p>
<p>Referenzen: <a href="https://www.sprite-ai.art/gallery/fire">sprite-ai fire</a> (Form wechselt am Ort, Spitzen schnueren sich ab), <a href="https://pixie.haus/pixelart/154800/fiery-flame-sprite-animation-frames">pixie.haus Flammen-Frames</a>, <a href="https://tips.clip-studio.com/en-us/articles/8096">Clip Studio Flammen-Zyklus</a>; Bewegung der Anime-Feuer von uppbeat.io nur als Rhythmus.</p>
<h2>Animation (Netherit-Box ohne Slots, damit die Zungen sichtbar sind; 4 Hitzestufen)</h2><figure><img src="{uri(frames[0].copy(), 'GIF', save_all=True, append_images=frames[1:], duration=MS, loop=0)}" alt=""></figure>
<h2>Streifen (8 Schluesselbilder)</h2><figure><img src="{uri(st)}" alt=""></figure>
<h2>Hitzestufen (mit Slots, wie im Spiel) Eisen / Enderit</h2><div class="g" style="grid-template-columns:1fr">{hr}</div>
<h2>Hintergruende aller Stufen, ohne Items</h2><div class="g">{cards}</div></body></html>"""
    Path(OUT.parent / "crucible-fire.html").write_text(html)
    print("html bytes", len(html))

main()
