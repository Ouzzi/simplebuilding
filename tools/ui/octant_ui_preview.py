#!/usr/bin/env python3
"""Octant manager (key K) contrast preview, queue CL-1 part, plan docs/ai/PLAN-B-OCTUI12-2026-11-08.md.

Renders the screen as it is now ("Ist") and three variants A/B/C at 3x GUI scale with the geometry of OctantScreen.java
(panel 230 wide, 20 high widgets, 24 grid) and measures every text/background pair with WCAG 2.x (UiContrast.ratio).
Usage: python3 tools/ui/octant_ui_preview.py [--vanilla DIR] [--out HTML]
Reuses the block/font code of simpleinterfaces_preview.py.
"""
import argparse, base64, io, os, sys
from pathlib import Path
import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import simpleinterfaces_preview as sp

K = 3
BRASS = 0xFF6F5F4B
INV = 0xFFC6C6C6
BTN, BTN_HOVER, BTN_OFF = 0xFF707070, 0xFF767676, 0xFF2D2D2D
INK_SOFT, INK_ERROR = 0xFF554C37, 0xFF8F1E18


def lumi(c):
    f = lambda v: v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4
    return 0.2126 * f((c >> 16 & 255) / 255) + 0.7152 * f((c >> 8 & 255) / 255) + 0.0722 * f((c & 255) / 255)


def ratio(a, b):
    la, lb = lumi(a), lumi(b)
    return (max(la, lb) + .05) / (min(la, lb) + .05)


def tint_face(rgb):
    return 0xFF000000 | rgb


def nine(name, w, h, border, tint=None):
    src = Image.open(sp.TEX / 'gui/sprites/widget' / f'{name}.png').convert('RGBA')
    sw, sh = src.size
    out = Image.new('RGBA', (w, h))
    b = border
    for sx, sy, dx, dy, cw, ch in [(0, 0, 0, 0, b, b), (sw - b, 0, w - b, 0, b, b), (0, sh - b, 0, h - b, b, b), (sw - b, sh - b, w - b, h - b, b, b)]:
        out.paste(src.crop((sx, sy, sx + cw, sy + ch)), (dx, dy))
    top = src.crop((b, 0, sw - b, b)); bot = src.crop((b, sh - b, sw - b, sh))
    lef = src.crop((0, b, b, sh - b)); rig = src.crop((sw - b, b, sw, sh - b))
    mid = src.crop((b, b, sw - b, sh - b))
    out.paste(top.resize((w - 2 * b, b), Image.NEAREST), (b, 0)); out.paste(bot.resize((w - 2 * b, b), Image.NEAREST), (b, h - b))
    out.paste(lef.resize((b, h - 2 * b), Image.NEAREST), (0, b)); out.paste(rig.resize((b, h - 2 * b), Image.NEAREST), (w - b, b))
    out.paste(mid.resize((w - 2 * b, h - 2 * b), Image.NEAREST), (b, b))
    if tint:
        a = np.asarray(out, dtype=np.float64)
        t = np.array([(tint >> 16) & 255, (tint >> 8) & 255, tint & 255]) / 112.0
        a[:, :, :3] = np.clip(a[:, :, :3] * t, 0, 255)
        out = Image.fromarray(a.astype(np.uint8), 'RGBA')
    return out


def txt(c, s, x, y, color, shadow=False):
    if shadow:
        sp.text(c, s, x + 1, y + 1, sp.scale(color, 0.25))
    sp.text(c, s, x, y, color)


def ctext(c, s, x, w, y, color, shadow=True):
    txt(c, s, x + (w - sp.text_width(s)) // 2, y, color, shadow)


LOCK_OPEN = ["..XXX..", ".X...X.", ".X.....", "XXXXXXX", "XXXXXXX", "XXX.XXX", "XXXXXXX", "XXXXXXX"]
LOCK_SHUT = ["..XXX..", ".X...X.", ".X...X.", "XXXXXXX", "XXXXXXX", "XXX.XXX", "XXXXXXX", "XXXXXXX"]


def icon(c, rows, x, y, color, shadow=True):
    for dx, dy, col in ([(1, 1, sp.scale(color, 0.25))] if shadow else []) + [(0, 0, color)]:
        for r, row in enumerate(rows):
            for k, ch in enumerate(row):
                if ch == 'X':
                    c.px(x + k + dx, y + r + dy, col)


def button(c, x, y, w, h, label, state='n', face=None, color=0xFFFFFFFF, lock=None, chip=None):
    name = {'n': 'button', 'h': 'button_highlighted', 'd': 'button_disabled'}[state]
    c.rgba(nine(name, w, h, 3, face), x, y)
    if lock is not None:
        icon(c, LOCK_SHUT if lock[0] else LOCK_OPEN, x + (w - 7) // 2, y + 6, lock[1])
    elif label:
        ctext(c, label, x, w, y + (h - 8) // 2, color)
    if chip:
        pass


def editbox(c, x, y, w, h, val, focus=False):
    c.fill(x, y, x + w, y + h, 0xFFFFFFFF if focus else 0xFFA0A0A0)
    c.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000)
    txt(c, val, x + 4, y + (h - 8) // 2, 0xFFE0E0E0)


def chip(c, x, y, letter, rgb, w=11, h=9):
    c.fill(x, y, x + w, y + h, 0xFF1E1F23); c.fill(x + 1, y + 1, x + w - 1, y + h - 1, rgb)
    sp.text(c, letter, x + (w - sp.text_width(letter)) // 2, y + 1, 0xFFFFFFFF)


AXIS = {'X': 0xFF9B2D2D, 'Y': 0xFF2F6B2F, 'Z': 0xFF2F4F9B}   # white letters >= 6:1 on the chip face


def render(variant, page, locked=False, hollow=True, layer=False, fig=True):
    """variant 'ist','A','B','C'; returns (PIL image, [(name, fg, bg)] measurements)."""
    m = []
    pal = sp.derive('brass', BRASS & 0xFFFFFF | 0xFF000000)
    PW = 230
    header = 10 if variant == 'C' and page == 0 else 0
    ph = 188 + header if page == 0 else 120
    W, H = PW + 16, ph + 16
    c = sp.Canvas(W, H, (0x5E, 0x72, 0x56))
    c.ox = c.oy = 8
    sp.box(c, 0, 0, PW, ph, pal, shadow=0)
    lab = pal[5]
    txt(c, 'Octant Manager', 8, 6, lab)
    m.append(('Titel/Beschriftung auf Messing', lab, BRASS))
    pad, cw = 8, 214
    top = 20 + header
    if page == 0:
        if header:
            for i, a in enumerate('XYZ'):
                chip(c, 28 + i * 66 + 31 - 5, 14, a, AXIS[a])
        for r, name in enumerate(['P1', 'P2', 'Size']):
            y = top + 24 * r
            txt(c, name, pad, y + 6, lab)
            for g in range(3):
                gx = 28 + g * 66
                val = [['1', '-58', '4'], ['0', '0', '0'], ['2', '59', '5']][r][g]
                button(c, gx, y, 12, 20, '-')
                editbox(c, gx + 13, y, 36, 20, val)
                button(c, gx + 50, y, 12, 20, '+')
        y = top + 72
        button(c, pad, y, 150, 20, 'Cuboid', 'h')
        ax_x = pad + 154
        if variant in ('B', 'C'):
            button(c, ax_x, y, 60, 20, '')
            txt(c, 'Axis', ax_x + 6, y + 6, 0xFFFFFFFF, True)
            chip(c, ax_x + 33, y + 5, '+Y', AXIS['Y'], 21, 10)
        else:
            button(c, ax_x, y, 60, 20, 'Axis: +Y')
        # summary
        sy = top + 96
        dark = variant == 'C'
        fx, fy, fw, fh = pad, sy - 3, cw - 1, 42
        if dark:
            sp.inset(c, fx, fy, fw, fh, pal)
            c.fill(fx + 1, fy + 1, fx + fw - 1, fy + fh - 1, 0xFF2B241C)
            fg, sec, num, bgc = 0xFFF2EEE8, 0xFFC9BFB0, 0xFFFFD87A, 0xFF2B241C
        else:
            sp.inset(c, fx, fy, fw, fh, sp.INVENTORY)
            c.fill(fx + 1, fy + 1, fx + fw - 1, fy + fh - 1, INV)
            fg, sec, bgc = 0xFF404040, (0xFF707070 if variant == 'ist' else INK_SOFT), INV
            num = fg
        bx = pad + 4
        txt(c, 'Pos 1: ', bx, sy, fg); sp.text(c, '1, -58, 4', bx + sp.text_width('Pos 1: ') + 1, sy, num)
        txt(c, 'Pos 2: ', bx, sy + 10, fg); sp.text(c, '0, 0, 0', bx + sp.text_width('Pos 2: ') + 1, sy + 10, num)
        txt(c, 'Volume: ', bx, sy + 20, fg); sp.text(c, '590 blocks', bx + sp.text_width('Volume: ') + 1, sy + 20, num)
        sp.text(c, '3', bx + sp.text_width('Volume: 590 blocks') + 2, sy + 18, num)
        txt(c, '(2 x 59 x 5)', bx, sy + 30, sec)
        m.append(('Zusammenfassung Haupttext', fg, bgc))
        m.append(('Zusammenfassung Masse (2 x 59 x 5)', sec, bgc))
        m.append(('Warnung "Corner too far"', (0xFFAA0000 if variant == 'ist' else (0xFFFF9A8C if dark else INK_ERROR)), bgc))
        fy2 = top + 24 * 3 + 24 + 44
    else:
        fy2 = top + 96
    # page 2 widgets / footer
    if page == 1:
        half = 105
        def tog(x, y, w, label, on):
            if variant in ('B', 'C') and on:
                button(c, x, y, w, 20, label + ': ON', face=tint_face(0x2F6F34))
                m.append((label + ' ON (Knopffläche grün, weisse Schrift)', 0xFFFFFFFF, 0xFF2F6F34))
            else:
                button(c, x, y, w, 20, f'{label}: {"ON" if on else "OFF"}')
        tog(pad, top, half, 'Hollow', hollow)
        tog(pad + cw - half, top, half, 'Layer Mode', layer)
        button(c, pad, top + 24, cw, 20, 'Order: Default')
        tog(pad, top + 48, cw, 'Figure', fig)
    # footer
    fy = fy2 if page == 0 else top + 72
    lock_col = 0xFF55FF55 if not locked else 0xFFFF5555
    face = None
    if variant == 'ist':
        button(c, pad, fy, 20, 20, '', lock=(locked, lock_col))
        m.append(('Schloss offen (§a #55FF55) auf Knopf #707070' if not locked else 'Schloss zu (§c #FF5555) auf Knopf #707070', lock_col, BTN))
    elif variant == 'A':
        button(c, pad, fy, 20, 20, '', lock=(locked, 0xFFFFFFFF))
    else:
        button(c, pad, fy, 20, 20, '', face=tint_face(0x9A3A32) if locked else None, lock=(locked, 0xFFFFFFFF))
    pw_ = (cw - 20 - 8) // 2
    button(c, pad + 24, fy, pw_, 20, 'Settings >>' if page == 0 else '<< Selection')
    button(c, pad + cw - (cw - 20 - 8 - pw_), fy, cw - 20 - 8 - pw_, 20, 'Done')
    return c.image(), m


def contrast_rows(m):
    return [(n, ratio(f, b)) for n, f, b in m]


def b64(img):
    img = img.resize((img.width * K, img.height * K), Image.NEAREST)
    bio = io.BytesIO(); img.save(bio, 'PNG')
    return base64.b64encode(bio.getvalue()).decode()


INFO = {
    'ist': ('Ist-Stand', 'Wie im Spiel (Build 08.11.): helles Feld mit grauer Zweitschrift, rote/grüne Schlosssymbole auf grauem Knopf, Zustand nur im Text.'),
    'A': ('A - Zahlen reparieren (kleinste Änderung)', 'Nur Farben: Zweitschrift und Warnung auf die UiContrast-Tinten (INK_SOFT/INK_ERROR), Schloss weiss (Form trägt den Zustand). Keine neuen Texturen.'),
    'B': ('B - Zustand auf dem Knopf', 'A plus: Schalter AN = grüne Knopffläche, Schloss zu = rote Fläche, Achse als farbige Kachel X rot / Y grün / Z blau (weisse Buchstaben). Zustand ohne Lesen erkennbar.'),
    'C': ('C - B plus dunkle Anzeige und Spaltenköpfe', 'B plus: Zusammenfassung dunkel eingelassen mit hellem Text (Zahlen gold), Kopfzeile X/Y/Z über den Feldern. Panel 10 px höher.'),
}


def extra_measures(variant):
    """Button / symbol pairs common to all variants."""
    rows = [('Knopfschrift weiss auf Knopf normal #707070', 0xFFFFFFFF, BTN), ('Knopfschrift weiss auf Knopf hover #767676', 0xFFFFFFFF, BTN_HOVER),
            ('Knopfschrift deaktiviert #A0A0A0 auf #2D2D2D', 0xFFA0A0A0, BTN_OFF), ('Eingabefeld Text #E0E0E0 auf schwarz', 0xFFE0E0E0, 0xFF000000)]
    if variant == 'ist':
        rows += [('Schloss offen §a #55FF55 auf Knopf', 0xFF55FF55, BTN), ('Schloss zu §c #FF5555 auf Knopf', 0xFFFF5555, BTN),
                 ('Zustand an/aus nur als Text ON/OFF (kein Farbunterschied)', 0xFFFFFFFF, BTN)]
    else:
        rows += [('Schloss weiss auf Knopf', 0xFFFFFFFF, BTN)]
    if variant in ('B', 'C'):
        rows += [('Schloss weiss auf roter Fläche #9A3A32', 0xFFFFFFFF, 0xFF9A3A32), ('Achse X weiss auf rot', 0xFFFFFFFF, AXIS['X']),
                 ('Achse Y weiss auf grün', 0xFFFFFFFF, AXIS['Y']), ('Achse Z weiss auf blau', 0xFFFFFFFF, AXIS['Z'])]
    return rows


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--vanilla', default=os.path.expanduser('~/vanilla263/assets/minecraft/textures'))
    ap.add_argument('--out', default='/root/sb-tools-b/previews/b-octui12-preview.html')
    a = ap.parse_args()
    sp.TEX = Path(a.vanilla)
    sp.FONT = sp.Font()
    h = ['<!doctype html><meta charset=utf-8><title>OCTUI12 Oktant-Manager Kontrast</title><style>body{font:15px sans-serif;background:#1f2229;color:#e6e8ec;margin:20px;max-width:1500px}'
         '.card{background:#2a2e37;border-radius:8px;padding:14px;margin:18px 0;border:3px solid #2a2e37}.card.sel{border-color:#7fd36b}'
         'img{image-rendering:pixelated;max-width:100%}table{border-collapse:collapse;margin:8px 0}td,th{padding:2px 10px;border-bottom:1px solid #444;text-align:left}'
         '.ok{color:#8fe07a}.bad{color:#ff8a7a;font-weight:bold}.row{display:flex;gap:14px;flex-wrap:wrap}.row>div{flex:1;min-width:360px}label{font-size:18px;cursor:pointer}'
         'code{background:#111;padding:3px 8px;border-radius:4px;font-size:18px}</style>',
         '<h1>Oktant-Manager (Taste K): Kontrast, Ist und Varianten</h1>',
         '<p>Mockups 3x GUI-Massstab (Geometrie aus OctantScreen.java, Palette Messing #6F5F4B, Vanilla-Knopf-Texturen 26.3). Kontrast nach WCAG 2.x wie UiContrast.ratio, Grenze 4,5:1. Rot = unter 4,5.</p>',
         '<h2>Mängel im Ist-Stand</h2><ol><li><b>Zweitschrift (2 x 59 x 5)</b> #707070 auf Feld #C6C6C6: nur 2,9:1.</li>'
         '<li><b>Schloss-Symbole</b> §a/§c auf Knopf #707070: rot 1,6:1 (fast unsichtbar), grün 3,7:1; der Zustand hängt an der Farbe.</li>'
         '<li><b>Warnung</b> #AA0000 auf #C6C6C6: 4,5:1, knapp an der Grenze (Rundung).</li>'
         '<li><b>An/aus</b> (Hollow, Layer, Figure) nur als Text ON/OFF, gleiche Knopffläche, Zustand erst beim Lesen erkennbar.</li>'
         '<li><b>Achse</b> nur Text, keine Zuordnung X/Y/Z zu den Feldspalten; Summary-Zahlen nicht abgehoben.</li>'
         '<li>Summary nutzt feste Farben (LABEL_COLOR), nicht die Helligkeit des UI-Kits (UiBrightness): im dunklen Modus bleibt die Schrift dunkel auf dem Feld.</li></ol>']
    for v in ('ist', 'A', 'B', 'C'):
        title, desc = INFO[v]
        im0, m0 = render(v, 0, locked=False)
        im1, m1 = render(v, 1, locked=True)
        imL, _ = render(v, 0, locked=True)
        meas = m0 + [x for x in m1 if 'ON (' in x[0]][:1] + extra_measures(v)
        seen, rows = set(), []
        for n, f, b in meas:
            if n in seen: continue
            seen.add(n); rows.append((n, ratio(f, b)))
        pick = '' if v == 'ist' else f'<label><input type=radio name=w value={v} onchange="pick(this.value)"> Variante {v} wählen</label>'
        h.append(f'<div class=card id=c{v}><h2>{title}</h2><p>{desc}</p>{pick}<div class=row>'
                 f'<div>Seite Auswahl (Schloss offen)<br><img src="data:image/png;base64,{b64(im0)}"></div>'
                 f'<div>Seite Einstellungen (Schloss zu im Fuss)<br><img src="data:image/png;base64,{b64(im1)}"></div></div>'
                 f'<table><tr><th>Paar</th><th>Kontrast</th></tr>' +
                 ''.join(f'<tr><td>{n}</td><td class={"ok" if r >= 4.5 else "bad"}>{r:.2f}:1</td></tr>' for n, r in rows) + '</table></div>')
        print(v, [(n, round(r, 2)) for n, r in rows if r < 4.5])
    h.append('<h2>Antwort</h2><p>Gewählt: <code id=ans>OCTUI12=?</code> <button onclick="navigator.clipboard.writeText(document.getElementById(\'ans\').textContent)">kopieren</button></p>'
             '<p>Variante bestimmt nur Farben/Knopfflächen im UI-Kit (siehe Plan). Nichts ist eingebaut, bis gewählt ist.</p>'
             '<script>function pick(v){document.getElementById("ans").textContent="OCTUI12="+v;for(const k of "ABC")document.getElementById("c"+k).classList.toggle("sel",k==v)}</script>')
    Path(a.out).write_text(''.join(h))
    print('written', a.out)


if __name__ == '__main__':
    main()
