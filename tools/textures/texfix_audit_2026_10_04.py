"""Usage: python tools/textures/texfix_audit_2026_10_04.py [preview dir]

Owner 2026-10-04: textures were changed in the texture-audit wave ("texfix", 2026-10-02) that he had not asked for.
This script lists every texture that wave touched - commits b82a84ce (Q1 items), 760d103e (pads, teleporters,
chunk loaders, launchpads, copper plates, shelf/mod guide books), dc018029 (mod enchanted books, signed blueprint),
b37bf91f (removed pixel-identical vanilla copies) - with its state now:
  reverted   back to the pre-texfix picture (owner's order 2026-10-04: spawn elytra, brick snowball, all Simple Money
             items, the mod's enchanted books)
  texfix     still the texfix picture - waiting for the owner's decision (shown in texfix-revert-uebersicht.png)
  later      changed again afterwards on purpose (e.g. guide books J, blueprint, raw enderite owner texture) - left alone
  removed    deleted vanilla copy (the game uses vanilla's own texture)
It writes texfix-revert-uebersicht.png (numbered, before | now, only the 'texfix' entries), texfix-ruecksetzung.png
(the reverted ones: texfix | old = now | a colour-reduced proposal of the old one) and prints the full list."""
import io
import os
import subprocess
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, '..', '..'))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(REPO, 'build')
COMMITS = ['b82a84ce', '760d103e', 'dc018029', 'b37bf91f']
REVERTED = ('enchanted_book_', '/simplemoney/', 'brick_snowball', 'spawn_elytra')


def git(*args):
    return subprocess.run(['git', *args], cwd=REPO, capture_output=True).stdout


def blob(rev, path):
    data = git('show', f'{rev}:{path}')
    if not data:
        return None
    try:
        return Image.open(io.BytesIO(data)).convert('RGBA')
    except OSError:
        return None


def current(path):
    p = os.path.join(REPO, path)
    return Image.open(p).convert('RGBA') if os.path.exists(p) else None


def same(a, b):
    return a is not None and b is not None and a.size == b.size and a.tobytes() == b.tobytes()


def entries():
    out = []
    for c in COMMITS:
        for line in git('show', '--name-status', '--format=', c).decode().splitlines():
            parts = line.split('\t')
            status, path = parts[0], parts[-1]
            if not path.endswith('.png') or path.startswith('wiki/') or '/hand/' in path or 'preview' in path:
                continue
            before, texfix, now = blob(f'{c}^', path), blob(c, path), current(path)
            if status.startswith('D'):
                state = 'removed'
            elif same(now, texfix):
                state = 'texfix'
            elif any(k in path for k in REVERTED) and same(now, before):
                state = 'reverted'
            else:
                state = 'later'
            out.append(dict(commit=c, path=path, status=status[0], state=state, before=before, texfix=texfix, now=now))
    return out


def frame(im, s):
    """First animation frame, scaled."""
    if im is None:
        return Image.new('RGBA', (16 * s, 16 * s), (0, 0, 0, 0))
    side = min(im.width, im.height)
    return im.crop((0, 0, side, side)).resize((16 * s, 16 * s), Image.NEAREST)


def reduce_colours(im, n=10):
    """Fewer colours, same drawing: the colours of the visible pixels quantised to n (median cut); every pixel keeps
    its own alpha (several old pictures are semi-transparent on purpose)."""
    if im is None:
        return None
    pts = [(x, y) for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3] > 0]
    strip = Image.new('RGB', (len(pts), 1))
    for i, p in enumerate(pts):
        strip.putpixel((i, 0), im.getpixel(p)[:3])
    q = strip.quantize(colors=n, method=Image.Quantize.MEDIANCUT).convert('RGB')
    out = Image.new('RGBA', im.size, (0, 0, 0, 0))
    for i, p in enumerate(pts):
        out.putpixel(p, q.getpixel((i, 0)) + (im.getpixel(p)[3],))
    return out


def sheet(rows, columns, path, title, s=6):
    cell = 16 * s + 8
    per_row = 4
    block = columns * cell + 30
    h = 30 + ((len(rows) + per_row - 1) // per_row) * (cell + 28)
    im = Image.new('RGBA', (20 + per_row * block, h), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((10, 8), title, fill=(0, 0, 0, 255))
    for k, (label, images) in enumerate(rows):
        x = 10 + (k % per_row) * block
        y = 30 + (k // per_row) * (cell + 28)
        d.text((x, y), label[:46], fill=(0, 0, 0, 255))
        for j, img in enumerate(images):
            im.alpha_composite(frame(img, s), (x + j * cell, y + 14))
    im.save(path)


def main():
    es = entries()
    for state in ('reverted', 'texfix', 'later', 'removed'):
        group = [e for e in es if e['state'] == state]
        print(f'== {state}: {len(group)}')
        for e in group:
            print(f"   {e['commit']} {e['status']} {e['path']}")
    open_ = [e for e in es if e['state'] == 'texfix']
    sheet([(f"{i + 1}. {os.path.basename(e['path'])}", [e['before'], e['now']]) for i, e in enumerate(open_)],
          2, os.path.join(OUT, 'texfix-revert-uebersicht.png'),
          'Texfix (2026-10-02), noch aktiv - je Nummer: vorher (vor Texfix) | jetzt. Besitzer entscheidet, was zurueck soll.')
    rev = [e for e in es if e['state'] == 'reverted']
    sheet([(os.path.basename(e['path']), [e['texfix'], e['now'], reduce_colours(e['now'])]) for e in rev],
          3, os.path.join(OUT, 'texfix-ruecksetzung.png'),
          'Zurueckgesetzt (Besitzer): Texfix-Fassung | alte Fassung = jetzt eingebaut | Vorschlag farbreduziert (10 Farben)')
    print('ok')


if __name__ == '__main__':
    main()
