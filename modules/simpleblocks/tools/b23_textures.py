"""W23-3 (owner 23.10. evening): Simple Blocks texture preview of branch a-blockstex23b, one page.

Built in (tools/textures/blocks_b23_2026_10_23.py, nothing to choose): the cracks of all six cracked bricks (deeper, running
through several bricks like Vanilla), sculk deepslate B (organic veins), hushed deepslate tiles B (sculk cushions instead of
sculk along the grout), chiseled packed ice B + chiseled blue ice the same way, the new block Sculk Tiles (animated).
Choice open: the moss, three new variants A/B/C drawn after a pixel analysis of Vanilla's mossy stone bricks (only here,
nothing built; the chosen one replaces missing_blocks_2026_10_16.mossy for all seven mossy bricks).

    python3.12 modules/simpleblocks/tools/b23_textures.py --preview <out.html> [--jar <minecraft-client.jar>]
"""
from __future__ import annotations

import argparse
import base64
import importlib.util
import io
import math
import random
import subprocess
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(ROOT / 'tools' / 'textures'))
import blocks_b23_2026_10_23 as b23  # noqa: E402
from blocks_b23_2026_10_23 import joints, layered, lum, overlay, pix, scale, vanilla  # noqa: E402
from layers import steps_of  # noqa: E402
from n41_proposals_2026_10_12 import hsh, ramp, vnoise  # noqa: E402

BLOCK = 'modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block'
DEFAULT_JAR = Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'


# ---------------------------------------------------------------- T1 moss (W23-3: new round, closer to Vanilla)

# Own moss ramp near Vanilla's five olive tones (Vanilla mossy stone bricks / mossy cobblestone: lum 86-130, darker than
# the stone's middle; the darkest tone sits in the joints): six tones, dark to light.
MOSS6 = ramp('4a5530', '57653a', '637443', '70834a', '7d9353', '8aa15c')


def moss_pick(base_px, t, adapt):
    """Moss tone for a pixel: t = 0 (deep in the joint) .. 1 (on the brick's lit top); a light brick lifts it one step.
    adapt > 0 pulls the tone that far towards the brick's own brightness (the moss sits in the texture's light)."""
    k = max(0, min(5, int(round(t * 5))))
    c = MOSS6[k]
    if adapt:
        target = lum(c) * (1 - adapt) + lum(base_px) * 0.78 * adapt
        c = scale(c, target / max(1, lum(c)))
    return c


def mossy(base_im, variant, seed=3):
    """W23-3 moss, the structure of Vanilla's mossy stone bricks:
    A  Vanilla structure: moss runs along each horizontal joint in clumps (darkest tone in the joint), a lighter cap on
       the brick row below with drips of 1-5 px (tips darker), a few pixels climbing up; the stone beside the moss one
       step darker (its shadow).
    B  the same, quieter (the owner's nearest pick was the subtle C): fewer clumps, drips up to 2 px, the tone a quarter
       of the way to the brick's brightness.
    C  cushions like mossy cobblestone: round clumps on the joint crossings, thin moss along part of the joints."""
    p = pix(base_im)
    j = joints(p)
    rows = [y for y in range(16) if sum(j[y]) >= 12]
    lums = sorted(lum(c) for row in p for c in row)
    lo, hi = lums[3], lums[-4]
    bright = lambda px: min(1.0, max(0.0, (lum(px) - lo) / max(1, hi - lo)))
    adapt = {'A': 0.0, 'B': 0.3, 'C': 0.15}[variant]
    cover, drip_max, climb_max = {'A': (0.62, 5, 4), 'B': (0.45, 2, 2), 'C': (0.32, 2, 1)}[variant]
    joint_l = [[None] * 16 for _ in range(16)]
    face_l = [[None] * 16 for _ in range(16)]
    r = random.Random(seed)
    dith = lambda x, y: (hsh(x, y, seed + 9) - 0.5) * 0.45                     # mottled, two tones side by side

    def put(x, y, t):
        x, y = x % 16, y % 16
        if j[y][x]:
            joint_l[y][x] = moss_pick(p[y][x], min(0.25, max(0.0, t * 0.3 + dith(x, y) * 0.5)), adapt)
        else:
            face_l[y][x] = moss_pick(p[y][x], min(1.0, max(0.0, t + 0.25 * bright(p[y][x]) + dith(x, y))), adapt)

    for row in rows:
        for x in range(16):
            n = vnoise(x, row, 5, seed + row) * 0.75 + hsh(x, row, seed) * 0.25  # clumps of 3-6 columns, ragged
            if n > cover:
                continue
            strength = 1 - n / cover
            if strength > 0.25 or hsh(x, row, seed + 6) < 0.5:
                put(x, row, 0.0)
            down = int(round(strength * drip_max * (0.3 + 0.9 * hsh(x, row, seed + 1))))
            up = int(round(strength * climb_max * (0.3 + 0.9 * hsh(x, row, seed + 2))))
            for k in range(1, down + 1):                                       # hanging below the joint
                if j[(row + k) % 16][x]:
                    break
                put(x, row + k, 0.62 - 0.1 * k if k < down else 0.25)
            for k in range(1, up + 1):                                         # climbing above it
                if j[(row - k) % 16][x]:
                    break
                put(x, row - k, 0.5 - 0.08 * k if k < up else 0.25)
    for y in range(16):                                                        # ragged fringe: single moss pixels
        for x in range(16):
            if face_l[y][x] is None and not j[y][x]:
                nb = sum(face_l[(y + dy) % 16][(x + dx) % 16] is not None for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if nb >= 2 and hsh(x, y, seed + 8) < 0.35 * (1 if variant == 'A' else 0.6):
                    put(x, y, 0.35)
    if variant == 'C':                                                         # cushions on the joint crossings
        crossings = [(x, y) for y in rows for x in range(16) if j[(y + 1) % 16][x] or j[(y - 1) % 16][x]]
        r.shuffle(crossings)
        for cx, cy in crossings[:4]:
            rad = r.uniform(1.8, 2.8)
            for dy in range(-3, 4):
                for dx in range(-3, 4):
                    d = math.hypot(dx, dy) + (vnoise(cx + dx, cy + dy, 2, seed) - 0.5) * 1.4
                    if d < rad:
                        x, y = (cx + dx) % 16, (cy + dy) % 16
                        put(x, y, 0.2 + 0.5 * (1 - d / rad))
    shadow = [[None] * 16 for _ in range(16)]
    for y in range(16):                                                        # stone beside the moss: a step darker
        for x in range(16):
            if joint_l[y][x] is None and face_l[y][x] is None and not j[y][x]:
                nb = [(x, (y - 1) % 16), ((x - 1) % 16, y), ((x + 1) % 16, y)]
                if any(face_l[b][a] is not None for a, b in nb) and hsh(x, y, seed + 5) < 0.55:
                    shadow[y][x] = scale(p[y][x], 0.9)
    s1 = overlay(p, shadow)
    s2 = overlay(s1, joint_l)
    return layered([('Basis (Vanilla)', p), ('Mooschatten', s1), ('Moos in den Fugen', s2), ('Moos auf den Ziegeln', overlay(s2, face_l))])


# ---------------------------------------------------------------- old state (for the "before" column)

BEFORE = 'e2eece951'   # branch point of a-blockstex23b: the textures before W23-3


def at_head(rel):
    """A file as it was before W23-3."""
    return subprocess.run(['git', '-C', str(ROOT), 'show', f'{BEFORE}:{rel}'], capture_output=True, check=True).stdout


def old_png(name):
    return Image.open(io.BytesIO(at_head(f'{BLOCK}/{name}.png'))).convert('RGBA')


def old_aged_module():
    """missing_blocks_2026_10_16 as it was (the old cracked/mossy), loaded from Git."""
    spec = importlib.util.spec_from_loader('old_missing_blocks', loader=None)
    mod = importlib.util.module_from_spec(spec)
    mod.__dict__['__file__'] = str(ROOT / 'tools/textures/missing_blocks_2026_10_16.py')
    exec(at_head('tools/textures/missing_blocks_2026_10_16.py').decode('utf-8'), mod.__dict__)
    return mod


# ---------------------------------------------------------------- preview

def colours(im):
    b = im.convert('RGBA').tobytes()
    return len({b[i:i + 4] for i in range(0, len(b), 4) if b[i + 3]})


def uri(im, s):
    buf = io.BytesIO()
    im.resize((im.width * s, im.height * s), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def gif_uri(frames, s, ms):
    buf = io.BytesIO()
    big = [f.convert('RGB').resize((f.width * s, f.height * s), Image.NEAREST) for f in frames]
    big[0].save(buf, 'GIF', save_all=True, append_images=big[1:], duration=ms, loop=0)
    return 'data:image/gif;base64,' + base64.b64encode(buf.getvalue()).decode()


def tile(im, n=3):
    o = Image.new('RGBA', (16 * n, 16 * n))
    for i in range(n):
        for k in range(n):
            o.alpha_composite(im, (i * 16, k * 16))
    return o


def context(im, neighbour):
    """A 5x3 wall: the block as 3x3 in the middle, framed by its plain neighbour (how it sits in a build)."""
    o = Image.new('RGBA', (80, 48))
    for i in range(5):
        for k in range(3):
            o.alpha_composite(im if 1 <= i <= 3 else neighbour, (i * 16, k * 16))
    return o


def anim_frames(strip, sub=4):
    """The strip's frames with the in-game interpolation (sub steps between two frames)."""
    n = strip.height // 16
    fr = [strip.crop((0, 16 * i, 16, 16 * i + 16)) for i in range(n)]
    return [Image.blend(fr[i], fr[(i + 1) % n], k / sub) for i in range(n) for k in range(sub)]


CRACKED = [('cracked_end_stone_bricks', 'end_stone_bricks', 'Rissige Endsteinziegel'), ('cracked_mud_bricks', 'mud_bricks', 'Rissige Lehmziegel'),
           ('cracked_red_nether_bricks', 'red_nether_bricks', 'Rissige Rote Netherziegel'), ('cracked_tuff_bricks', 'tuff_bricks', 'Rissige Tuffziegel'),
           ('cracked_cinnabar_bricks', 'cinnabar_bricks', 'Rissige Zinnoberziegel'), ('cracked_sulfur_bricks', 'sulfur_bricks', 'Rissige Schwefelziegel')]
MOSS_EX = [('mossy_deepslate_tiles', 'deepslate_tiles', 'Bemooste Tiefenschieferfliesen'), ('mossy_end_stone_bricks', 'end_stone_bricks', 'Bemooste Endsteinziegel')]
MOSS_NOTES = {
    'A': 'Vanilla-Struktur: Moos läuft in Büscheln an den waagerechten Fugen entlang (dunkelster Ton in der Fuge), hängt 1–5 px '
         'in die Ziegelreihe darunter (Spitzen dunkler) und klettert 1–4 px nach oben; gefleckt aus zwei Nachbartönen; der Stein '
         'neben dem Moos eine Stufe dunkler (Schatten) – wie bei Vanilla-Moossteinziegeln.',
    'B': 'Wie A, aber dezenter (Richtung der früheren Var. C, die am nächsten dran war): weniger Büschel, höchstens 2 px hängend/kletternd, '
         'Moos-Ton zu 30 % an die Helligkeit des Ziegels angeglichen.',
    'C': 'Polster wie Vanilla-Moosbruchstein: runde Moosbüschel an Fugenkreuzungen, dazu wenig Moos entlang der Fugen.',
}
MOSS_REC = 'B'


def preview(out, jar):
    css = (':root{--bg:#f4f2f6;--fg:#1d1b20;--card:#fff;--muted:#4a4452;--acc:#8a5a00;--ok:#1d6b3a}'
           '@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--muted:#c9c2d4;--acc:#ffd27a;--ok:#7fd69a}}'
           ':root[data-theme="dark"]{--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--muted:#c9c2d4;--acc:#ffd27a;--ok:#7fd69a}'
           'body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif}main{max-width:1180px;margin:auto;padding:16px}'
           'h1{font-size:22px}h2{font-size:19px;margin-top:30px;border-top:2px solid #8886;padding-top:14px}h3{font-size:15px;margin:16px 0 6px}'
           'img{image-rendering:pixelated;display:block;max-width:100%;height:auto}.row{display:flex;flex-wrap:wrap;gap:12px;align-items:flex-start}'
           '.cell{background:var(--card);padding:8px;border-radius:6px;max-width:100%}.cap{font-size:12px;color:var(--muted);margin-top:4px;max-width:300px}'
           '.note{max-width:900px}.rec{color:var(--acc);font-weight:600}.done{color:var(--ok);font-weight:600}'
           'pre{background:var(--card);padding:10px;border-radius:6px;white-space:pre-wrap;user-select:all}')
    h = ['<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
         f'<title>Simple Blocks W23</title><style>{css}</style><main><h1>Simple Blocks W23-3 (Branch a-blockstex23b)</h1>'
         '<p class="note">Oben: <span class="done">eingebaut</span> nach deiner Wahl (nichts mehr zu entscheiden, nur ansehen). '
         'Unten: <span class="rec">Wahl offen</span> – das Moos, drei neue Varianten. Bilder: 16-fach, 3×3-Wand, Wand-Kontext; '
         'Vanilla-Bilder nur in dieser Seite.</p>']

    def cell(im, s, cap, layers=True):
        st = steps_of(im) if layers else []
        return (f'<div class="cell"><img src="{uri(im, s)}" alt=""><div class="cap">{cap} · {colours(im)} Töne'
                + (f' · {len(st)} Ebenen' if len(st) > 1 else '') + '</div></div>')

    def trio(im, neighbour, cap):
        return cell(im, 12, cap) + cell(tile(im), 5, '3×3') + cell(context(im, neighbour), 3, 'Wand-Kontext')

    def layers_row(im):
        st = steps_of(im)
        if len(st) < 2:
            return ''
        return '<p class="cap">Ebenen:</p><div class="row">' + ''.join(
            cell(Image.frombytes('RGBA', (im.width, im.height), data).crop((0, 0, 16, 16)), 4, name, False) for name, data in st) + '</div>'

    def before_after(title, old, new, neighbour, note, refs):
        h.append(f'<h3>{title}</h3><p class="note">{note}</p>')
        h.append('<div class="row">' + cell(old, 8, 'vorher') + cell(tile(old), 4, 'vorher 3×3') + '</div>')
        h.append('<div class="row">' + trio(new, neighbour, '<span class="done">jetzt eingebaut</span>') + '</div>' + layers_row(new))
        if refs:
            h.append('<div class="row">' + ''.join(cell(vanilla(jar, n), 6, 'Vanilla ' + n, False) for n in refs) + '</div>')

    # ------------------------------------------------ built in
    h.append('<h2><span class="done">Eingebaut</span></h2>')
    old_mod = old_aged_module()
    h.append('<h3>Risse – alle 6 rissigen Ziegel</h3><p class="note">Wie Vanilla-rissige Stein-/Tiefenschieferziegel: drei lange Risse '
             '(13–18 px) laufen über die Fugen hinweg durch 2–3 Ziegel, je 1–2 Abzweige; Kern im dunklen Fugenton, in der Mitte '
             'eine Stufe tiefer (Halbton daneben), Spitzen laufen aus, Lichtkante darüber. Gleiche Vanilla-Basis + Riss-Ebenen. '
             'Ausnahmen unverändert: rissiger Diamantblock, rissiges Eis.</p>')
    for i, (name, base, label) in enumerate(CRACKED):
        idx = [a[0] for a in old_mod.AGED].index(name)
        src = vanilla(jar, base)
        new = b23.cracked(src, 1000 + idx)
        old = old_mod.cracked(src, 1000 + idx)
        h.append(f'<p class="cap"><b>{label}</b></p><div class="row">' + cell(old, 6, 'vorher') + cell(tile(old), 3, 'vorher 3×3')
                 + cell(new, 6, '<span class="done">jetzt</span>') + cell(tile(new), 3, 'jetzt 3×3') + cell(context(new, src), 2, 'Kontext') + '</div>')
    h.append(layers_row(b23.cracked(vanilla(jar, 'end_stone_bricks'), 1000 + [a[0] for a in old_mod.AGED].index('cracked_end_stone_bricks'))))
    h.append('<div class="row">' + ''.join(cell(vanilla(jar, n), 6, 'Vanilla ' + n, False) for n in ('cracked_stone_bricks', 'cracked_deepslate_bricks')) + '</div>')
    deep = vanilla(jar, 'deepslate')
    before_after('Sculk-Tiefenschiefer (B, Muster organischer)', old_png('sculk_deepslate'), b23.sculk_deepslate(jar), deep,
                 'Vanilla-Tiefenschiefer + eigene Sculk-Adern, die wie die Vanilla-Sculk-Ader wachsen: ein Trieb kriecht pixelweise, dreht '
                 'ständig ein wenig und wechselt die Drehrichtung (keine geraden Linien), gabelt sich, schwillt an Knoten auf 2 px an; '
                 'dunkler Saum rundum, drei Leuchtpunkte. Beim Build erzeugt (Vanilla-Basis).', ['sculk_vein', 'sculk'])
    before_after('Gehüllte Tiefenschiefer-Fliesen (B, weniger gerade Linien)', old_png('hushed_deepslate_tiles'), b23.hushed_tiles(jar),
                 vanilla(jar, 'deepslate_tiles'),
                 'Vanilla-Tiefenschiefer-Fliesen + Sculk: statt Sculk entlang der Fugen (gab gerade Linien) vier unregelmäßige Sculk-Polster '
                 'über Fliesen und Fugen hinweg, dünne gewundene Triebe dazwischen, drei leuchtende Augen. Beim Build erzeugt.', ['deepslate_tiles'])
    for kind, name, label, vn in (('packed', 'chiseled_packed_ice', 'Gemeißeltes Packeis (B)', 'packed_ice'),
                                  ('blue', 'chiseled_blue_ice', 'Gemeißeltes Blaueis (analog B)', 'blue_ice')):
        before_after(label, old_png(name), b23.chiseled_ice(jar, kind), vanilla(jar, vn),
                     'Vanilla-' + ('Packeis' if kind == 'packed' else 'Blaueis') + ' als Basis, das bisherige Motiv ('
                     + ('Kristall' if kind == 'packed' else 'gestufte Raute') + ') als Relief eingemeißelt: jede Motivrolle hellt bzw. dunkelt '
                     'das Eis darunter (Licht oben/links, Schatten unten/rechts) statt flacher Farbringe. Beim Build erzeugt.', [vn])
    strip = b23.sculk_tiles()
    first = strip.crop((0, 0, 16, 16))
    frames = anim_frames(strip)
    h.append('<h3>Neuer Block: Sculk-Fliesen (C)</h3><p class="note">Eigene Textur (eingecheckt, Ebenen): Fliesen in Sculk-Farben, '
             'jede Fliese eigene Helligkeit wie die Tiefenschiefer-Fliesen; vier Reihen (4/3/3/2 px) mit je einer hohen Fliese, die eine '
             'Fugenreihe überbrückt, und versetzten Stoßfugen – wenig durchgehende Linien. Animiert (.mcmeta, 8 Bilder, frametime 6, '
             'interpolate): sechs Sculk-Funken atmen langsam auf und ab. Rezept 2×2: Tiefenschieferfliesen + Sculk über Kreuz → 2. '
             'Würfel ohne Treppe/Stufe → laut Octet-Regel (A4) kein Octet. Im Tab „Simple Blocks“ hinter Sculk-Tiefenschiefer, '
             'in Vanillas Baublock-Tab hinter den Gehüllten Fliesen.</p><div class="row">'
             f'<div class="cell"><img src="{gif_uri(frames, 12, 75)}" alt=""><div class="cap">animiert (wie im Spiel)</div></div>'
             f'<div class="cell"><img src="{gif_uri([tile(f) for f in frames], 5, 75)}" alt=""><div class="cap">3×3 animiert</div></div>'
             + cell(context(first, vanilla(jar, 'deepslate_tiles')), 3, 'Kontext (Bild 1)') + '</div>' + layers_row(strip))

    # ------------------------------------------------ choice open
    h.append('<h2><span class="rec">Wahl offen</span>: Moos (3 neue Varianten)</h2>'
             '<p class="note">Analyse Vanilla-Moossteinziegel (26.3, pixelgenau): 98 von 256 Pixeln geändert, 5 gedämpfte Oliv-Töne '
             '(Helligkeit 86–130, dunkler als die Steinmitte). Das Moos sitzt auf den waagerechten Fugen: dunkelster Ton <i>in</i> der Fuge, '
             'hellere Töne als Band darüber und darunter, von dort Streifen 1–5 px nach unten <i>und</i> oben (beides gleich stark), '
             'gefleckt aus zwei Nachbartönen; Steinpixel neben dem Moos werden eine Stufe dunkler. Moosbruchstein: runde Polster, '
             'meist der dunkelste Ton in den Ritzen. Danach gezeichnet, eigene Töne (6 Oliv-Töne nahe Vanilla), in Ebenen. '
             'Nach der Wahl auf alle 7 bemoosten Ziegel übertragen.</p>')
    h.append('<h3>Kalibrierung: Variante A auf Vanilla-Steinziegeln neben dem echten Vanilla-Moos</h3><div class="row">'
             + cell(tile(mossy(vanilla(jar, 'stone_bricks'), 'A', seed=23)), 5, 'Variante A auf stone_bricks', False)
             + cell(tile(vanilla(jar, 'mossy_stone_bricks')), 5, 'Vanilla mossy_stone_bricks', False)
             + cell(tile(vanilla(jar, 'mossy_cobblestone')), 5, 'Vanilla mossy_cobblestone', False) + '</div>')
    for i, (name, base, label) in enumerate(MOSS_EX):
        van = vanilla(jar, base)
        idx = [a[0] for a in old_mod.AGED].index(name)
        old = old_mod.mossy(van, 1000 + idx)
        h.append(f'<h3>{label}</h3><div class="row">' + cell(old, 8, 'heute eingebaut') + cell(tile(old), 4, 'heute 3×3') + '</div>')
        for v in 'ABC':
            im = mossy(van, v, seed=3 + 10 * i)
            rec = ' <span class="rec">Empfehlung</span>' if v == MOSS_REC else ''
            h.append(f'<p class="cap"><b>Variante {v}</b>{rec}: {MOSS_NOTES[v]}</p><div class="row">' + trio(im, van, f'Moos={v}') + '</div>'
                     + (layers_row(im) if i == 0 else ''))
    h.append('<h2>Antwort</h2><p class="note">Antwortcode kopieren und anpassen (Empfehlung B vorausgefüllt: dezent wie die '
             'frühere Tiefenschiefer-Variante C, aber mit der Vanilla-Struktur).</p>'
             f'<pre>W23-3-Moos={MOSS_REC}</pre><p class="cap">Referenzen: Vanilla 26.3 mossy_stone_bricks, mossy_cobblestone, '
             'cracked_stone_bricks, cracked_deepslate_bricks, sculk, sculk_vein, deepslate(_tiles), packed_ice, blue_ice; '
             'Vanilla Tweaks/Faithful als Stilrichtung (weiche Übergänge, Relief statt Farbringe). Eigene Pixel; Vanilla-Pixel nur als Basis '
             'der beim Build erzeugten Texturen und in dieser Seite.</p></main></html>')
    Path(out).write_text('\n'.join(h), encoding='utf-8')


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview', required=True)
    ap.add_argument('--jar', default=str(DEFAULT_JAR))
    a = ap.parse_args(argv)
    preview(a.preview, a.jar)
    print('preview ->', a.preview)


if __name__ == '__main__':
    main()
