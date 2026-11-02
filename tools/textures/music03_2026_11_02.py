"""b-music03 (02.11.): Dämmerholz (Stamm weicher, Schilder B mit Lila), inverser Besatz (Stufenlogik) und die Vorschau zu allem.

Schallplatten, Rohling, Notenblatt und Taschen-Jukebox entstehen in modules/simplemusic/tools/gen_textures.py;
dieses Skript schreibt nur die Dämmerholz-Texturen nach modules/simpleend (aus dem Stand der Basis, damit ein erneuter
Lauf dasselbe ergibt) und baut die Vorher/Nachher-Seite. Eigene Pixel; Vanilla nur im Speicher (Besatz-Vorschau).

    python3 tools/textures/music03_2026_11_02.py [--base <git-ref>] [--jar <client.jar>] [--write] [--preview <html>]
"""
from __future__ import annotations

import argparse
import base64
import importlib.util
import io
import math
import os
import subprocess
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import layers  # noqa: E402

spec = importlib.util.spec_from_file_location('ig3114', HERE / 'ig3114_2026_10_31.py')
ig = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ig)

END = ROOT / 'modules/simpleend/shared/resources/assets/simpleend/textures'
MUSIC = ROOT / 'modules/simplemusic/shared/resources/assets/simplemusic/textures/item'
BASE_DEFAULT = '8d395b8e7'  # the branch point: what the textures looked like before this work
LILAC_ACCENT = [(0x5a, 0x3f, 0x55), (0x7f, 0x5a, 0x78), (0xa9, 0x84, 0x9f), (0xd6, 0xb9, 0xcd)]  # simpleend's Duskwood accent ramp


def base_img(ref, rel):
    data = subprocess.run(['git', '-C', str(ROOT), 'show', f'{ref}:{rel}'], check=True, capture_output=True).stdout
    return Image.open(io.BytesIO(data)).convert('RGBA')


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def lilac(p):
    """The purple 'dusk' pixels of the Duskwood log (centre of the top, glints on the side) - they must stay."""
    return p[3] and p[2] - p[1] >= 10 and p[0] >= p[1]


# ---------------------------------------------------------------- Dämmerholz

def soften(im, k, wrap):
    """Pull every non-lilac pixel k of the way to the mean of its four neighbours: the same shapes, a little less hard."""
    out = im.copy()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            p = im.getpixel((x, y))
            if not p[3] or lilac(p):
                continue
            ns = []
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if wrap:
                    nx, ny = nx % w, ny % h
                if 0 <= nx < w and 0 <= ny < h:
                    q = im.getpixel((nx, ny))
                    if q[3]:
                        ns.append(q)
            if not ns:
                continue
            mean = [sum(q[i] for q in ns) / len(ns) for i in range(3)]
            out.putpixel((x, y), tuple(round(p[i] + (mean[i] - p[i]) * k) for i in range(3)) + (255,))
    return out


def log_side_new(ref):
    old = base_img(ref, 'modules/simpleend/shared/resources/assets/simpleend/textures/block/duskwood_log.png')
    layers.snap(old, 'Ist (Basis)')
    return soften(old, 0.30, wrap=True), old


def log_top_new(ref):
    old = base_img(ref, 'modules/simpleend/shared/resources/assets/simpleend/textures/block/duskwood_log_top.png')
    layers.snap(old, 'Ist (Basis)')
    new = soften(old, 0.55, wrap=False)
    return new, old


def sign_b(hanging):
    """Variant B (owner 02.11.): two boards with a seam, Vanilla proportions; the hanging sign's chains in the Duskwood lilac."""
    im = ig.sign_item('B', hanging)
    if hanging:
        for cx in (4, 11):  # replace the grey chain by the lilac accent
            for (dx, y, c) in [(0, 3, LILAC_ACCENT[1]), (-1, 4, LILAC_ACCENT[0]), (1, 4, LILAC_ACCENT[0]), (0, 4, LILAC_ACCENT[2]),
                               (-1, 5, LILAC_ACCENT[0]), (1, 5, LILAC_ACCENT[0])]:
                ig.put(im, cx + dx, y, c)
        layers.snap(im, 'Lila Ketten')
    return im


def write_png(im, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)
    steps = layers.steps_of(im)
    final = im.convert('RGBA').tobytes()
    if not steps:
        steps = [('Grundform', final)]
    elif steps[-1][1] != final:
        steps.append(('Nachbearbeitung', final))
    layers.write_beside(path, layers.from_steps(im.width, im.height, steps))


# ---------------------------------------------------------------- inverser Besatz (Spiegel von TrimInversion.java)

RIM_SUBTRACT = 80
BORDER = {1: 1, 2: 2, 3: 3}


def tone_for(armor_px, palette):
    tones = sorted(palette, key=lambda c: -lum(c))
    t = min(1.0, max(0.0, (lum(armor_px) - 60) / 170))
    idx = 6 - math.floor(t * 5 + 0.5)
    return tones[max(1, min(min(6, len(tones) - 2), idx))]


def inverted_new(pattern, armor, palette, stage):
    out = ig.new(pattern.width, pattern.height)
    border = BORDER[stage]
    for y in range(pattern.height):
        for x in range(pattern.width):
            if pattern.getpixel((x, y))[3]:
                continue
            a = armor.getpixel((x, y))
            if not a[3]:
                continue
            d = ig.dist(pattern, x, y, border)
            if d != 0 and d != border:
                continue  # the cut-out grows 1 px per stage: the armor shows through
            f = tone_for(a, palette)
            out.putpixel((x, y), (ig.subtract(f, RIM_SUBTRACT) if d else f) + (255,))
    return out


def besatz(jar):
    armor = jar.img('entity/equipment/humanoid/iron')
    keys = [jar.img('palettes/trim_base').getpixel((i, 0))[:3] for i in range(8)]
    res = {}
    for pat, mat in [('coast', 'amethyst'), ('sentry', 'gold'), ('wild', 'redstone')]:
        pattern = jar.img('trims/entity/humanoid/' + pat)
        pal = [jar.img('palettes/trim/' + mat).getpixel((i, 0))[:3] for i in range(8)]
        normal = armor.copy()
        normal.alpha_composite(ig.trim_colored(pattern, keys, pal))
        res[(pat, 'vanilla', 0)] = ig.front(normal)
        for stage in (1, 2, 3):
            for v in ('ist', 'neu'):
                im = armor.copy()
                im.alpha_composite(ig.inverted(pattern, armor, pal, 'ist', stage) if v == 'ist' else inverted_new(pattern, armor, pal, stage))
                res[(pat, v, stage)] = ig.front(im)
    return res


# ---------------------------------------------------------------- Vorschau

def uri(im, s):
    buf = io.BytesIO()
    im.resize((im.width * s, im.height * s), Image.NEAREST).save(buf, 'PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


CSS = (':root{--bg:#f4f2f6;--fg:#1d1b20;--card:#fff;--muted:#4a4452;--line:#8886;--slot:#8b8b8b}'
       '@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--muted:#c9c2d4}}'
       'body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif}main{max-width:1200px;margin:auto;padding:16px}'
       'h1{font-size:22px}h2{font-size:19px;margin-top:30px;border-top:2px solid var(--line);padding-top:12px}h3{font-size:15px;margin:14px 0 6px}'
       'img{image-rendering:pixelated;display:block;max-width:100%;height:auto;cursor:zoom-in}img.big{cursor:zoom-out}'
       '.row{display:flex;flex-wrap:wrap;gap:10px;align-items:flex-start;margin:6px 0}'
       '.cell{background:var(--card);padding:6px;border-radius:6px;max-width:100%}.cap{font-size:12px;color:var(--muted);margin-top:3px;max-width:220px}'
       '.slot{background:var(--slot);padding:2px;display:inline-block;margin-top:4px}.slot img{width:32px;height:32px;cursor:default}'
       '.note{max-width:920px}.wrap{overflow-x:auto;max-width:100%}.hard{font-weight:600}.warn{color:#c0392b}'
       'pre{background:var(--card);padding:10px;border-radius:6px;white-space:pre-wrap;word-break:break-all;user-select:all}'
       'button{font:inherit;padding:6px 12px;border-radius:6px;border:1px solid var(--line);background:var(--card);color:var(--fg);cursor:pointer}')


def cell(im, cap, s=10, hard=True):
    h = ig.hard_share(im) if hard else None
    tag = '' if h is None else f' · <span class="hard{" warn" if h > 0.12 else ""}">{h * 100:.0f} % hart</span>'
    small = f'<div class="slot"><img src="{uri(im, 2)}" alt=""></div>' if im.width <= 16 and im.height <= 16 else ''
    return (f'<div class="cell"><img src="{uri(im, s)}" data-s="{s}" alt="{cap}" width="{im.width * s}" height="{im.height * s}">{small}'
            f'<div class="cap">{cap}{tag}</div></div>')


def load(p):
    return Image.open(p).convert('RGBA')


def page(jar, ref, bz):
    h = ['<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">',
         '<title>b-music03 Vorschau</title><style>', CSS, '</style></head><body><main>',
         '<h1>b-music03 – Schallplatten, Notenblatt, Taschen-Jukebox, Dämmerholz, inverser Besatz</h1>',
         '<p class="note"><b>Gebaut ist gleich die beste Fassung</b> (Grundregel des Besitzers: ohne Einwand = abgenommen). Je Motiv Vorher '
         '(Stand vor dieser Arbeit) und Nachher (jetzt im Branch). Bild anklicken = größer/kleiner. Unter jedem Bild: 2-fach im grauen Slot, '
         'Anteil harter Übergänge (Helligkeitssprung &gt; 40 zwischen Nachbarn; Ziel ~7 %, über 12 % rot – bei Platten zählen Etikett und '
         'Rillen mit, Vanilla-Platten liegen selbst bei 20–30 %).</p>']

    h.append('<h2>1 · Schallplatten (Vanilla-Form)</h2><p class="note">Alle acht Platten und der Rohling jetzt in Vanilla-Form (15×10, linksbündig, '
             'Kappen 5 px, Etikett-Ring um das Loch, zwei Schattenreihen). <b>Deep Pulse</b> bleibt (Ring). <b>Ember Forge</b>: voller Ring, Flamme '
             '(helle Spitze oben, dunkler Fuß) statt der Flecken. <b>End Departure</b>: Sichel (hell links, dunkel rechts, voller Ring). <b>Fortress March</b>: '
             'Zinnen (Kerbe oben), helle Seiten. <b>Obsidian Throne</b>: Krone (zwei helle Spitzen, goldenes Band, Purpur-Sockel). <b>Sky Forge</b>: '
             'helles Kreuz auf dunklem Blau. <b>Etikett</b> (custom_disc) und <b>Composed</b> wie gefallen. <b>Rohling</b>: Variante C (grau, Rillen, Papier-Etikett). '
             'Die runden platzierten Platten bleiben unverändert. Gewählt ist durchgehend der getönte Körper (Variante B der IG3114-Vorschau), weil '
             'die heutigen Platten schon getönt sind; die Platten-Variante war in der Antwort nicht genannt.</p>')
    h.append('<h3>Vanilla zum Vergleich</h3><div class="row">')
    for n in ('13', 'cat', 'pigstep', 'creator', 'otherside'):
        h.append(cell(jar.img('item/music_disc_' + n), 'music_disc_' + n))
    h.append('</div>')
    names = [('music_disc_deep_pulse', 'deep_pulse'), ('music_disc_ember_forge', 'ember_forge'), ('music_disc_end_departure', 'end_departure'),
             ('music_disc_fortress_march', 'fortress_march'), ('music_disc_obsidian_throne', 'obsidian_throne'), ('music_disc_sky_forge', 'sky_forge'),
             ('custom_disc', 'custom_disc'), ('custom_disc_composed', 'custom_disc_composed'), ('blank_disc', None)]
    for item, ig_name in names:
        h.append(f'<h3>{item}</h3><div class="row">')
        h.append(cell(base_img(ref, f'modules/simplemusic/shared/resources/assets/simplemusic/textures/item/{item}.png'), 'Vorher'))
        if ig_name:
            h.append(cell(load(HERE / 'ig3114' / f'platten_B_{ig_name}.png'), 'IG3114-Vorschlag B'))
        else:
            h.append(cell(load(HERE / 'ig3114' / 'music_C_blank_disc.png'), 'IG3114 Variante C'))
        h.append(cell(load(MUSIC / f'{item}.png'), 'Nachher'))
        h.append('</div>')

    h.append('<h2>2 · Notenblatt</h2><p class="note">Der Stand war „fast optimal“: Form, Noten und Rand bleiben. Nur die Hintergrundlinien: '
             'vorher (140,128,110) – ein harter Sprung zum Papier –, jetzt der eigene Schattenton des Papiers (208,198,170), wie die weichen Linien von Vanilla-Papier und Karte.</p><div class="row">')
    for rel in ('item/paper', 'item/map'):
        h.append(cell(jar.img(rel), 'Vanilla ' + rel))
    h.append(cell(base_img(ref, 'modules/simplemusic/shared/resources/assets/simplemusic/textures/item/sheet_music.png'), 'Vorher'))
    h.append(cell(load(MUSIC / 'sheet_music.png'), 'Nachher'))
    h.append('</div>')

    h.append('<h2>3 · Taschen-Jukebox</h2><p class="note">Heutiges Aussehen bleibt (Kupferkasten, Deckel mit Platte, Tonarm). Behutsam: das fast Schwarze der Platte '
             'und das Weiß des Tonarms jeweils ein paar Stufen weicher; Form und Farben unverändert, alle 16 Farben und die Abspiel-Animation ziehen mit.</p><div class="row">')
    for n, cap in (('pocket_jukebox', 'geschlossen'), ('red_pocket_jukebox', 'rot'), ('blue_pocket_jukebox', 'blau')):
        h.append(cell(base_img(ref, f'modules/simplemusic/shared/resources/assets/simplemusic/textures/item/{n}.png'), f'Vorher {cap}'))
        h.append(cell(load(MUSIC / f'{n}.png'), f'Nachher {cap}'))
    h.append('</div>')

    h.append('<h2>4 · Dämmerholz</h2><p class="note"><b>Stamm</b>: der Ist-Zustand bleibt – unregelmäßige Rinde und das Lila in der Mitte der Stirnseite '
             '(Pixel unverändert, geschützt); nur etwas weniger hart (jeder Pixel 30 % Seite / 55 % oben zum Mittel seiner Nachbarn). '
             '<b>Schilder</b>: Variante B (zwei Bretter mit Fuge, Vanilla-Proportionen); beim <b>Hängeschild</b> sind die Ketten jetzt lila (Dämmer-Akzent).</p>')
    h.append('<h3>Vanilla</h3><div class="row">')
    for rel in ('block/dark_oak_log', 'block/dark_oak_log_top', 'item/oak_sign', 'item/oak_hanging_sign'):
        h.append(cell(jar.img(rel), rel))
    h.append('</div>')
    for item, new in (('block/duskwood_log', log_side_new(ref)[0]), ('block/duskwood_log_top', log_top_new(ref)[0]),
                      ('item/duskwood_sign', sign_b(False)), ('item/duskwood_hanging_sign', sign_b(True))):
        h.append(f'<h3>{item}</h3><div class="row">')
        h.append(cell(base_img(ref, f'modules/simpleend/shared/resources/assets/simpleend/textures/{item}.png'), 'Vorher'))
        h.append(cell(new, 'Nachher'))
        h.append('</div>')
    h.append('<h3>Stamm 3×3 (Kacheln, Seite)</h3><div class="row">')
    for label, im in (('Vorher', log_side_new(ref)[1]), ('Nachher', log_side_new(ref)[0])):
        t = ig.new(48, 48)
        for i in range(3):
            for k in range(3):
                t.alpha_composite(im, (i * 16, k * 16))
        h.append(cell(t, label, s=4, hard=False))
    h.append('</div>')

    h.append('<h2>5 · Inverser Besatz (Variante A mit Stufenlogik)</h2><p class="note">Eisenrüstung von vorn, Vanilla-Muster mit Amethyst/Gold/Redstone. '
             '<b>Regel:</b> der Übergang ist immer genau <b>1 Pixel dunkellila Außenlinie</b> (Füllung minus 80). <b>Stufe 1</b> wie bisher (Muster + 1 px Linie). '
             '<b>Stufe 2</b>: das ausgeschnittene Muster ist 1 px dicker (die Rüstung scheint durch, „silber“), nur die äußere Linie bleibt dunkel. '
             '<b>Stufe 3</b>: 2 px dicker, das meiste silber, nur die Außenlinie dunkel. Die Füllung folgt der Helligkeit der Rüstung über die Besatz-Palette '
             '(helle Rüstung, helles Lila; Variante A), und sie liegt nur dort, wo die Rüstung Pixel hat (keine Füllung in der Helmöffnung). '
             '<b>Farben geprüft (Coast/Amethyst):</b> Füllung = Amethyst-Töne (lila), ausgeschnittenes Muster = Rüstungsfarbe (silber) – die Inversion '
             'vertauscht Muster und Fläche, nicht die Farben; im Vorher füllte eine einzige flache Farbe auch Lücken.</p>')
    for pat, mat in (('coast', 'amethyst'), ('sentry', 'gold'), ('wild', 'redstone')):
        h.append(f'<h3>{pat} · {mat}</h3><div class="row wrap">')
        h.append(cell(bz[(pat, 'vanilla', 0)], 'Vanilla-Besatz (normal)', s=5, hard=False))
        for v, lab in (('ist', 'Vorher'), ('neu', 'Nachher')):
            for st in (1, 2, 3):
                h.append(cell(bz[(pat, v, st)], f'{lab} Stufe {st}', s=5, hard=False))
        h.append('</div>')

    h.append('<h2>Einwand?</h2><p class="note">Ohne Einwand gilt alles als abgenommen. Sonst unten kurz schreiben, was anders soll, und den Code zurückgeben.</p>'
             '<label>Einwand: <input id="free" style="width:100%;max-width:560px" placeholder="z. B. Sky Forge Kreuz kleiner; Besatz Stufe 3 weniger Silber"></label>'
             '<pre id="code">MUSIC03|ok</pre><button id="copy" type="button">Code kopieren</button>')
    h.append('<script>\n' + JS + '\n</script></main></body></html>')
    return ''.join(h)


JS = """(function () {
  function code() {
    var free = document.getElementById('free').value.trim();
    return free ? 'MUSIC03|einwand=' + free.replace(/\\|/g, '/') : 'MUSIC03|ok';
  }
  function update() { document.getElementById('code').textContent = code(); }
  document.getElementById('free').addEventListener('input', update);
  document.getElementById('copy').addEventListener('click', function () {
    if (navigator.clipboard) { navigator.clipboard.writeText(code()).catch(function () {}); }
  });
  document.addEventListener('click', function (e) {
    var t = e.target;
    if (t.tagName === 'IMG' && t.dataset.s) {
      var big = t.classList.toggle('big');
      var s = +t.dataset.s, w = +t.getAttribute('width');
      t.style.width = big ? (w * 2) + 'px' : '';
      t.style.maxWidth = big ? 'none' : '';
    }
  });
  update();
})();"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--base', default=BASE_DEFAULT)
    ap.add_argument('--jar', default=os.environ.get('SB_CLIENT_JAR') or ig.JAR_DEFAULT)
    ap.add_argument('--write', action='store_true', help='write the Duskwood textures into modules/simpleend')
    ap.add_argument('--preview')
    a = ap.parse_args()
    if a.write:
        side, _ = log_side_new(a.base)
        top, _ = log_top_new(a.base)
        for name, im in (('block/duskwood_log', side), ('block/duskwood_log_top', top), ('item/duskwood_sign', sign_b(False)),
                         ('item/duskwood_hanging_sign', sign_b(True))):
            write_png(im, END / f'{name}.png')
            print('wrote', name, f'{ig.hard_share(im) * 100:.1f} % hard')
    if a.preview:
        jar = ig.Jar(a.jar)
        if not jar.z:
            sys.exit('client jar not found: ' + str(a.jar))
        html = page(jar, a.base, besatz(jar))
        Path(a.preview).parent.mkdir(parents=True, exist_ok=True)
        Path(a.preview).write_text(html, encoding='utf-8')
        print('preview', a.preview, len(html.encode()) // 1024, 'KiB')


if __name__ == '__main__':
    main()
