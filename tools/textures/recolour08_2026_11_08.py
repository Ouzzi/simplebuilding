"""Recoloured Vanilla sprites as build-time textures (a-mojang08, release blocker 1, 2026-11-08).

The textures listed in recolour08_2026_11_08.json were committed as PNGs although their pixels come from a Mojang
sprite (a palette swap, or the sprite with a few own pixels). The repository keeps only the recipe:

  - src: the Vanilla sprite (path under assets/minecraft/textures/ in the 26.3 client jar),
  - map: palette index of the sprite (its distinct RGBA colours, sorted) -> own colour, only where it differs,
  - fix: pixel index -> own colour (None: the sprite's pixel), where a pixel does not follow the map.

No Mojang colour or pixel is stored. tools/textures/vanilla_derived.py calls derived(jar) for the simplebuilding
namespace and derived_module(jar, module) for the modules; the outputs are in .gitignore.

    python3.12 tools/textures/recolour08_2026_11_08.py --capture FILE...   (once: recipe from the committed PNG)
    python3.12 tools/textures/recolour08_2026_11_08.py --verify            (rebuild every entry and compare)
"""
from __future__ import annotations

import json
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
DATA = HERE / 'recolour08_2026_11_08.json'
NS = 'simplebuilding'


def _codes(im):
    import numpy as np
    a = np.asarray(im.convert('RGBA'), dtype=np.uint8).copy()
    a[a[..., 3] == 0] = 0
    return a.reshape(-1, 4)


def _hex(px) -> str:
    return '%02x%02x%02x%02x' % tuple(int(v) for v in px)


def _sprite(z: zipfile.ZipFile, src: str):
    import io
    from PIL import Image
    return Image.open(io.BytesIO(z.read('assets/minecraft/textures/' + src)))


def recipe(own, sprite) -> dict:
    """map + fix that turn sprite into own (both PIL images of one size)."""
    import numpy as np
    v, r = _codes(sprite), _codes(own)
    assert v.shape == r.shape, 'size differs'
    palette, inv = np.unique(v, axis=0, return_inverse=True)
    inv = inv.ravel()
    mapping, fix = {}, []
    for i in range(len(palette)):
        px = r[inv == i]
        cols, cnt = np.unique(px, axis=0, return_counts=True)
        best = cols[cnt.argmax()]
        if not (best == palette[i]).all():
            mapping[str(i)] = _hex(best)
    for p in range(len(r)):
        want = r[p]
        got = bytes.fromhex(mapping[str(inv[p])]) if str(inv[p]) in mapping else bytes(palette[inv[p]])
        if bytes(want) != got:
            # None: the sprite's own pixel stays (no Mojang colour is stored)
            fix.append([p, None if (want == v[p]).all() else _hex(want)])
    return {'map': mapping, 'fix': fix}


def build(entry: dict, sprite):
    import numpy as np
    from PIL import Image
    v = _codes(sprite)
    palette, inv = np.unique(v, axis=0, return_inverse=True)
    inv = inv.ravel()
    lut = palette.copy()
    for i, col in entry['map'].items():
        lut[int(i)] = list(bytes.fromhex(col))
    out = lut[inv]
    for p, col in entry['fix']:
        out[p] = v[p] if col is None else list(bytes.fromhex(col))
    w, h = sprite.size
    return Image.fromarray(out.reshape(h, w, 4).astype(np.uint8), 'RGBA')


def entries(ns: str | None = None) -> list:
    data = json.loads(DATA.read_text(encoding='utf-8'))
    return [e for e in data if ns is None or e['ns'] == ns]


def _draw(jar, ns: str) -> dict:
    with zipfile.ZipFile(jar) as z:
        return {e['path']: build(e, _sprite(z, e['src'])) for e in entries(ns)}


def derived(jar) -> dict:
    """simplebuilding namespace: texture path under assets/simplebuilding/textures/ -> image."""
    return _draw(jar, NS)


def derived_module(jar, module: str) -> dict:
    """One module's textures: path under assets/<module>/textures/ -> image."""
    return _draw(jar, module)


# Former byte copies of Vanilla sprites whose references now point at the Vanilla sprite itself.
REDIRECTS = {('simplecrucibles', 'block/iron_crucible_inner.png'): 'block/cauldron_inner.png',
             ('simplecrucibles', 'block/reinforced_crucible_inner.png'): 'block/cauldron_inner.png',
             ('simplemachines', 'block/auto_smither_bottom.png'): 'block/crafter_bottom.png',
             ('simplefun', 'entity/pig/pig.png'): 'entity/pig/pig_temperate.png'}


def open_own(path):
    """An own texture by its repository path; for a path that became a recipe here (git-ignored, absent in a fresh
    checkout) the image is drawn from the client jar. For generators that read sibling textures (ramps, masks)."""
    from PIL import Image
    path = Path(path)
    if path.exists():
        return Image.open(path)
    ns, rel = path.as_posix().split('/assets/', 1)[1].split('/textures/', 1)
    if (ns, rel) in REDIRECTS:
        with zipfile.ZipFile(_jar()) as z:
            return _sprite(z, REDIRECTS[(ns, rel)])
    for e in entries(ns):
        if e['path'] == rel:
            with zipfile.ZipFile(_jar()) as z:
                return build(e, _sprite(z, e['src']))
    return Image.open(path)  # raises FileNotFoundError as before


def _jar():
    if str(HERE) not in sys.path:
        sys.path.insert(0, str(HERE))
    import vanilla_derived
    jar = vanilla_derived.find_jar()
    if jar is None:
        raise SystemExit('recolour08: no 26.3 client jar')
    return jar


def main(argv: list) -> int:
    from PIL import Image
    if argv[:1] == ['--capture']:
        # FILE=SRC pairs: repository PNG = Vanilla sprite path; appends/replaces the recipe in the JSON.
        data = json.loads(DATA.read_text(encoding='utf-8')) if DATA.exists() else []
        with zipfile.ZipFile(_jar()) as z:
            for arg in argv[1:]:
                f, src = arg.split('=')
                ns, path = f.split('/assets/', 1)[1].split('/textures/', 1)
                e = {'ns': ns, 'path': path, 'src': src, **recipe(Image.open(ROOT / f), _sprite(z, src))}
                data = [d for d in data if (d['ns'], d['path']) != (ns, path)] + [e]
        data.sort(key=lambda d: (d['ns'], d['path']))
        DATA.write_text('[\n' + ',\n'.join(json.dumps(e, separators=(',', ':')) for e in data) + '\n]\n', encoding='utf-8')
        return 0
    if argv[:1] == ['--verify']:
        # Every entry rebuilt from the jar equals the given repository files (FILE...), pixel for pixel.
        import numpy as np
        bad = 0
        with zipfile.ZipFile(_jar()) as z:
            index = {(e['ns'], e['path']): e for e in entries()}
            for f in argv[1:]:
                ns, path = f.split('/assets/', 1)[1].split('/textures/', 1)
                e = index[(ns, path)]
                if not (_codes(build(e, _sprite(z, e['src']))) == _codes(Image.open(ROOT / f))).all():
                    print('DIFF', f)
                    bad += 1
        print(f'recolour08 verify: {len(argv) - 1 - bad}/{len(argv) - 1} identical')
        return 1 if bad else 0
    print(__doc__)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
