"""Nachtrag 46 round 3 (owner 2026-10-12): astral / nihil vault chest proposals on the basis of A1/N1, cleaner.

The owner asked to take the Vanilla ender chest sheet (entity/chest/ender.png) pixel for pixel as the structure and
only swap the palette (+ a small motif). The Vanilla sheet is read at run time from the local Minecraft client jar
(never checked in); the resulting proposal sheets are written only outside the repo (--out, default
/root/previews/n46/r3) and into the review page. After the owner's pick the chosen sheet goes into the game.

  1  exact: every Vanilla tone mapped by brightness onto the material ramp, eye -> motif, lock -> material accent
  2  calm: the same, but quantised to 5 tones (less noise, clear edges)
  3  calm + accent: like 2, the light rim / seam tones of the lid in the material accent colour
  4  like 3, plus the motif small on the lid

Run: python3.12 tools/textures/n46_round3_2026_10_12.py [--html page.html] [--jar client.jar] [--out dir]
"""
from __future__ import annotations

import argparse
import io
import sys
import zipfile
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import n41_proposals_2026_10_12 as n41  # noqa: E402
import n46_proposals_2026_10_12 as r1  # noqa: E402
import n46_round2_2026_10_12 as r2  # noqa: E402

H, mix = n41.H, n41.mix
JAR = Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
EYE = (43, 34, 55, 42)          # front panel area of the Vanilla ender eye (x0, y0, x1, y1 exclusive)
LOCK = (0, 0, 6, 5)

RAMP = {
    'astral': ['05020c', '120a22', '1e1236', '2c1c4c', '3e2a66', '56408a', '7458ae', '9a80d0'],
    'nihil': ['020a0b', '08181a', '0f2626', '173634', '22484a', '306462', '46847e', '68a8a0'],
}
ACCENT = {'astral': ['5a3a9c', '8460c8', 'b496ec', 'e4d4ff'], 'nihil': ['2e6e6a', '4a9a94', '86cfc8', 'd0f4ee']}
MOTIF = {'astral': ('2a8c8c', '5ad8d0', 'c8fff6'), 'nihil': ('3e8e8a', 'b3dcdd', 'eafcf8')}


def vanilla(jar):
    with zipfile.ZipFile(jar) as z:
        return Image.open(io.BytesIO(z.read('assets/minecraft/textures/entity/chest/ender.png'))).convert('RGBA')


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def in_box(x, y, b):
    return b[0] <= x < b[2] and b[1] <= y < b[3]


def is_eye(c):
    return c[1] > c[0] + 18 and c[1] > 60   # the green ender eye tones


def ramp_at(r, t):
    t = min(max(t, 0.0), 1.0) * (len(r) - 1)
    i = int(t)
    return mix(H(r[i]), H(r[min(i + 1, len(r) - 1)]), t - i)


def sheet(van, mat, v):
    body = [van.getpixel((x, y)) for y in range(64) for x in range(64)
            if van.getpixel((x, y))[3] and not in_box(x, y, LOCK) and not (in_box(x, y, EYE) and is_eye(
                van.getpixel((x, y))))]
    lo, hi = min(lum(c) for c in body), max(lum(c) for c in body)
    ramp, acc, mot = RAMP[mat], ACCENT[mat], [H(c) for c in MOTIF[mat]]
    out = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    lock = [van.getpixel((x, y)) for y in range(5) for x in range(6) if van.getpixel((x, y))[3]]
    llo, lhi = min(lum(c) for c in lock), max(lum(c) for c in lock)
    for y in range(64):
        for x in range(64):
            c = van.getpixel((x, y))
            if not c[3]:
                continue
            if in_box(x, y, LOCK):
                t = (lum(c) - llo) / max(1, lhi - llo)
                out.putpixel((x, y), ramp_at(acc, t))
                continue
            if in_box(x, y, EYE) and is_eye(c):
                out.putpixel((x, y), H(ramp[0]))     # empty eye socket, the motif follows
                continue
            t = (lum(c) - lo) / max(1, hi - lo)
            if v >= 2:
                t = round(t * 4) / 4                 # 5 calm tones
            col = ramp_at(ramp, t)
            if v >= 3 and t >= 0.5 and (in_box(x, y, (14, 0, 28, 14)) or 14 <= y < 19):   # lid rim dots / side seams
                col = H(acc[1] if t < 0.75 else acc[2])
            out.putpixel((x, y), col)

    def motif(cx, cy):
        for dy, row in enumerate(r2.MOT5[mat]):
            for dx, ch in enumerate(row):
                c = {'o': mot[1], 'M': mot[0], 'L': mot[2]}.get(ch)
                if c:
                    out.putpixel((cx - 2 + dx, cy - 2 + dy), c)
    motif(48, 38)
    if v == 4:
        motif(21, 7)
    return out


def page(path, van, out_dir):
    out_dir.mkdir(parents=True, exist_ok=True)
    u, rc = r1.uri, r1.render_cell
    refs = [('Minecraft Wiki: Ender Chest', 'https://minecraft.wiki/w/Ender_Chest'),
            ('Fancy End – End Aesthetic Overhaul (saubere Vanilla-nahe Endertruhe/End-Paletten)',
             'https://www.curseforge.com/minecraft/texture-packs/fancy-end-end-aesthetic-overhaul'),
            ('Reinforced Chests Retextured (Truhen-Stufen streng im Vanilla-Stil)',
             'https://www.modrinth.com/resourcepack/reinforced-chests-retextured'),
            ('Ender Chest Palette (gefärbte Endertruhen = Palettentausch auf Vanilla-Struktur)',
             'https://modrinth.com/project/gOkXuy64'),
            ('Faithful-Richtlinien (Formen/Farben wie Vanilla halten)',
             'https://docs.faithfulpack.net/pages/textures/cf32-texturing-guidelines')]
    h = ['<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" '
         'content="width=device-width,initial-scale=1"><title>N46 Runde 3</title>'
         f'<style>{r1.CSS}</style></head><body><main>',
         '<h1>Nachtrag 46 · Runde 3: Fragentisch im Spiel, Gewölbe sauberer</h1>',
         '<h2>Web-Referenzen</h2><ul>' + ''.join(f'<li><a href="{x}">{t}</a></li>' for t, x in refs) + '</ul>']
    # questionary: before / after
    import subprocess
    t_dir = 'modules/simplequestionary/shared/resources/assets/simplequestionary/textures/block'
    before = {}
    for k in ('side', 'top', 'bottom', 'gem', 'book'):
        data = subprocess.run(['git', '-C', str(r1.ROOT), 'show', f'{r1.BASE}:{t_dir}/questionary_table_{k}.png'],
                              capture_output=True, check=True).stdout
        before[k] = Image.open(io.BytesIO(data)).convert('RGBA')
    after = r1.qtable_current()
    h.append('<h2>Fragentisch – R3 eingebaut</h2><p>R3 mit voller Bücherreihe an allen Seiten (2-px-Buchrücken wie '
             'das Vanilla-Bücherregal) und niedrigerer Lapis-Kante (nur noch die untere Reihe der Tischplatte).</p>')
    for title, tex in (('Vorher', before), ('Nachher (im Spiel)', after)):
        h.append(f'<div class="card"><h3>{title}</h3><div class="row">' + rc('Render', r1.qtable_render(tex))
                 + ''.join(r1.tex_cell(k, im) for k, im in tex.items()) + '</div></div>')
    names = ['1 · exakt (Vanilla-Töne auf Material-Palette)', '2 · ruhig (5 Töne, weniger Rauschen)',
             '3 · ruhig + Akzent am Deckel', '4 · wie 3, Motiv klein auf dem Deckel']
    for mat, L, title in (('astral', 'A', 'Astral-Gewölbe'), ('nihil', 'N', 'Nihil-Gewölbe')):
        h.append(f'<h2>{title} – 4 Vorschläge auf Basis {L}1</h2><p>Struktur Pixel für Pixel von der '
                 'Vanilla-Endertruhe, nur Palette getauscht, Auge = '
                 f'{"Astral-Raute" if mat == "astral" else "Nihil-Ring"}, Schloss in Material-Akzent.</p>')
        a1 = r2.chest_sheet(0, mat)
        h.append(f'<div class="card"><h3>Vorher ({L}1 aus Runde 2)</h3><div class="row">'
                 + rc('3D-Truhe', r2.chest_render(a1)) + f'<figure><img class="px" src="{u(a1, 4)}" alt="">'
                 '<figcaption>Truhenbild 4×</figcaption></figure></div></div>')
        for v in range(1, 5):
            s = sheet(van, mat, v)
            s.save(out_dir / f'{L}{v}_{mat}_vault_chest.png')
            items = (f'<figure><span class="sizes"><img class="px" src="{u(r2.chest_render(s, 32))}" alt="">'
                     f'<img class="px" src="{u(r2.chest_render(s, 16))}" alt=""></span>'
                     '<figcaption>Item 32/16</figcaption></figure>')
            h.append(f'<div class="card"><h3>{L}{names[v - 1]}</h3><div class="row">'
                     + rc('3D-Truhe', r2.chest_render(s)) + items
                     + f'<figure><img class="px" src="{u(s, 4)}" alt=""><figcaption>Truhenbild 4×</figcaption>'
                     '</figure></div></div>')
    h.append('</main></body></html>')
    path.write_text(''.join(h), encoding='utf-8')
    print(path, path.stat().st_size)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--html', type=Path)
    ap.add_argument('--jar', type=Path, default=JAR)
    ap.add_argument('--out', type=Path, default=Path('/root/previews/n46/r3'))
    a = ap.parse_args()
    if a.html:
        page(a.html, vanilla(a.jar), a.out)
