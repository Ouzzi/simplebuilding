"""Crucible window preview for SP22-6 (owner 22.10.): before/after of the fire zone.

After: one slot pitch more room under the grid (CrucibleMenu.FIRE_ROOM 12 -> 30), a dark hearth trapezoid in that zone
(CrucibleFlames.hearth) and the flames over it, sized to the zone (CrucibleFlames.targetPixels/room) so the tallest
tongue stays CrucibleMenu.FIRE_GAP px below the lowest slot row. Before: the layout of crucible_fire_2026_10_12.py.
Pure Python + PIL, own pixels only (placeholder items are plain blobs). Mirrors the Java code - keep in step.
Usage: python3.12 tools/textures/crucible_hearth_2026_10_22.py [OUT_HTML]
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

argv = sys.argv[1:]
sys.argv = sys.argv[:1]
sys.path.insert(0, str(Path(__file__).parent))
import crucible_fire_2026_10_12 as F

C = F.C
OUT = Path(argv[0]) if argv else Path("/tmp/claude-0/sbx/crucui22-preview.html")
FIRE_ROOM_OLD, FIRE_ROOM, FIRE_GAP, FRAME_BOTTOM = 12, 30, 3, 7
old_target = F.target_pixels


def target_new(heat, glowing, room):
    """CrucibleFlames.targetPixels (SP22-6): from the room the flames may rise in."""
    if glowing:
        return 0 if heat == "none" else max(3, C.jround(room / 5.0))
    return C.jround(room * {"none": 0.0, "medium": 0.35, "high": 0.65, "extreme": 0.70}[heat])


def hearth(d, x, top, width, bottom, key):
    """CrucibleFlames.hearth: back edge 0.40, slanted edges 0.50, floor 0.52 -> 0.66 front, 1 px narrower per 2 rows."""
    fill = F.H(F.PAL[key][0])
    h = bottom - top
    for r in range(h):
        inset = (h - 1 - r) // 2
        l, rt = x + inset, x + width - inset - 1
        if r == 0:
            d.rectangle((l, top, rt, top), fill=F.sc(fill, 0.40))
            continue
        d.rectangle((l, top + r, rt, top + r), fill=F.sc(fill, 0.52 + 0.14 * r / max(1, h - 1)))
        if inset > 0:
            d.point((l, top + r), fill=F.sc(fill, 0.50))
            d.point((rt, top + r), fill=F.sc(fill, 0.50))


def item(d, x, y, n):
    """Placeholder item (own pixels): a small raw-ore blob, tinted per slot."""
    tint = [(0xD8, 0xAF, 0x93), (0xE3, 0xC0, 0x5A), (0xC8, 0x7C, 0x4E)][n % 3]
    dark = F.sc(tint, 0.6)
    d.rectangle((x + 4, y + 5, x + 11, y + 12), fill=tint)
    d.rectangle((x + 3, y + 7, x + 12, y + 10), fill=tint)
    d.rectangle((x + 5, y + 4, x + 9, y + 4), fill=tint)
    d.rectangle((x + 5, y + 12, x + 12, y + 12), fill=dark)
    d.rectangle((x + 12, y + 7, x + 12, y + 11), fill=dark)
    d.point((x + 6, y + 6), fill=F.sc(tint, 1.25))


def screen(tier, barrel, heat, glowing, millis, after):
    g, rows = F.TIERS[tier]
    gw = g * 3 * 18
    cw = bw = gw + 14
    group = cw + (2 + bw if barrel else 0)
    room = FIRE_ROOM if after else FIRE_ROOM_OLD
    sec = 18 + rows * 18 + room
    W = max(176, group)
    img = Image.new("RGB", (W, sec + 2 + 100), (31, 34, 40))
    d = ImageDraw.Draw(img)
    left = (W - group) // 2
    F.box(d, left, 0, cw, sec, tier)
    if barrel:
        F.box(d, left + cw + 2, 0, bw, sec, "barrel")
    F.box(d, (W - 176) // 2, sec + 2, 176, 100, "inv")
    fire_top, fire_bottom = 18 + rows * 18, sec - FRAME_BOTTOM
    if after:
        hearth(d, left + 5, fire_top + FIRE_GAP, cw - 10, fire_bottom, tier)
        C.target_pixels = lambda h, gl, zone: target_new(h, gl, zone - FRAME_BOTTOM - FIRE_GAP)
        size = sec - fire_top
    else:
        C.target_pixels = old_target
        size = sec
    if heat != "none":
        C.draw_flames(d, left + 5, fire_bottom, cw - 10, size, heat, glowing, millis, "MITTEL")
    n = 0
    for r in range(rows):
        for c in range(g * 3):
            x, y = left + 8 + c * 18, 19 + r * 18
            F.slot(d, x, y, tier)
            if (r + c) % 2 == 0 or r == rows - 1:
                item(d, x, y, n); n += 1
            if barrel:
                F.slot(d, left + cw + 2 + 8 + c * 18, y, "barrel")
    ix = (W - 176) // 2 + 8
    for r in range(3):
        for c in range(9):
            F.slot(d, ix + c * 18, sec + 2 + 17 + r * 18, "inv")
    for c in range(9):
        F.slot(d, ix + c * 18, sec + 2 + 17 + 58, "inv")
    return img


def main():
    rows = []
    for tier in F.TIERS:
        for barrel in (False, True):
            b = F.uri(F.up(screen(tier, barrel, "high", False, 700, False), 3))
            a = F.uri(F.up(screen(tier, barrel, "high", False, 700, True), 3))
            rows.append(f'<h3>{tier} {"mit Fass" if barrel else "ohne Fass"} (Hitze hoch)</h3><div class="g">'
                        f'<figure><img src="{b}" alt=""><figcaption>vorher</figcaption></figure>'
                        f'<figure><img src="{a}" alt=""><figcaption>nachher</figcaption></figure></div>')
    heats = []
    for tier in ("iron", "enderite"):
        for after in (False, True):
            ims = [screen(tier, False, h, gl, 700, after) for _, h, gl in C.HEATS]
            sec = 18 + F.TIERS[tier][1] * 18 + (FIRE_ROOM if after else FIRE_ROOM_OLD)
            ims = [i.crop((0, 0, i.width, sec)) for i in ims]
            sh = Image.new("RGB", (sum(i.width for i in ims) + 6 * (len(ims) - 1), sec), (31, 34, 40))
            x = 0
            for i in ims:
                sh.paste(i, (x, 0)); x += i.width + 6
            heats.append(f'<figure><img src="{F.uri(F.up(sh, 3))}" alt=""><figcaption>{tier} {"nachher" if after else "vorher"}: '
                         f'mittel, hoch, extrem (blau), Nachglühen</figcaption></figure>')
    html = f"""<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Tiegel-Feuerzone</title><style>
:root{{--bg:#fff;--fg:#1d2026;--mut:#5b6270;--card:#f1f3f6}}
@media(prefers-color-scheme:dark){{:root:not([data-theme=light]){{--bg:#16181d;--fg:#e8eaee;--mut:#9aa1ae;--card:#22252c}}}}
:root[data-theme=dark]{{--bg:#16181d;--fg:#e8eaee;--mut:#9aa1ae;--card:#22252c}}
body{{background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif;margin:0 auto;padding:16px;max-width:1200px}}
img{{image-rendering:pixelated;max-width:100%;height:auto}}figure{{margin:0;background:var(--card);padding:8px;border-radius:8px}}
figcaption{{color:var(--mut);font-size:13px}}.g{{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:12px;align-items:start}}
p{{color:var(--mut)}}</style></head><body>
<h1>Tiegel-Fenster: Feuerzone (SP22-6)</h1>
<p>Nachher: unten 18 px (eine Slot-Teilung) mehr Platz, darin ein dunkles Herd-Trapez in der Farbe der Box (hinten dunkler, vorne heller,
Pixel-Treppe wie die Box-Ecken). Das Feuer steht darauf, ist auf die Zone bemessen und bleibt 3 px unter der untersten Slot-Reihe.
Alle Stufen gleich; die Fass-Box wächst bündig mit (ohne Trapez, dort brennt nichts). Items sind Platzhalter. Dreifach vergrößert.</p>
<h2>Hitzestufen</h2><div class="g" style="grid-template-columns:1fr">{''.join(heats)}</div>
<h2>Alle Stufen vorher / nachher</h2>{''.join(rows)}</body></html>"""
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(html)
    print(OUT, len(html))


if __name__ == "__main__":
    main()
