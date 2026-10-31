"""b-rohr33 (31.10.): builds the choice page /root/sb-tools-b/previews/b-rohr33-preview.html (preview only).

Ist-Zustand (repo models + textures) and variants A/B/C (rohr33_2026_10_31.py) rendered as scenes: straight run, bend, cross into
a hopper, item; the flat sheets 16x; the A8b hard share. Vanilla chest/hopper are read at runtime from /root/mc263src (never
written into the repo; the page lives outside it).

    python3 tools/textures/rohr33_preview.py [--out PATH]
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import math
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import rohr33_2026_10_31 as gen  # noqa: E402
import rohr33_render as rr  # noqa: E402
import storage31_2026_10_30 as s31  # noqa: E402

ROOT = HERE.parents[1]
ASSETS = ROOT / 'modules/simplestorage/shared/resources/assets/simplestorage'
ROT = {'north': (0, 0), 'east': (0, 90), 'south': (0, 180), 'west': (0, 270), 'up': (270, 0), 'down': (90, 0)}
OUT = Path('/root/sb-tools-b/previews/b-rohr33-preview.html')


def model(name):
    return json.loads((ASSETS / f'models/block/{name}.json').read_text())['elements']


IST = {'core': model('template_pipe_core'), 'arm': model('template_pipe_arm'), 'out': model('template_pipe_out')}


def pipe(tex, pos, facing, sides, variant):
    """Parts of one pipe block like the multipart block state: core, an arm per connected side (not the output), the output."""
    t = rr.load_textures({'pipe': tex}, None)
    if variant == 'ist':
        parts = [rr.part(IST['core'], t, pos)]
        arm, out = IST['arm'], IST['out']
    else:
        parts = [rr.part(gen.core_elements(), t, pos, *ROT[facing])]   # concept: the core turns with the output
        arm, out = gen.arm_elements(variant == 'B'), gen.out_elements()
    for s in sides:
        if s != facing:
            parts.append(rr.part(arm, t, pos, *ROT[s]))
    parts.append(rr.part(out, t, pos, *ROT[facing]))
    return parts


def hopper(pos, facing='down'):
    els, tex = rr.vanilla_model('hopper' if facing == 'down' else 'hopper_side')
    return [rr.part(els, tex, pos, *({'down': (0, 0)}.get(facing, ROT[facing])))]


def scenes(tex, variant):
    v = variant
    straight = [rr.chest_parts((0, 0, 0), 90), *pipe(tex, (1, 0, 0), 'east', ['west', 'east'], v),
                *pipe(tex, (2, 0, 0), 'east', ['west', 'east'], v), rr.chest_parts((3, 0, 0), 270)]
    bend = [rr.chest_parts((0, 0, 0), 90), *pipe(tex, (1, 0, 0), 'east', ['west', 'east'], v),
            *pipe(tex, (2, 0, 0), 'south', ['west', 'south'], v), *hopper((2, 0, 1), 'down')]
    cross = [*pipe(tex, (0, 0, 1), 'east', ['east'], v), *pipe(tex, (1, 0, 0), 'south', ['south'], v),
             *pipe(tex, (2, 0, 1), 'west', ['west'], v), rr.chest_parts((1, 1, 1), 0),
             *pipe(tex, (1, 0, 1), 'down', ['west', 'north', 'east', 'up', 'down'], v), *hopper((1, -1, 1))]
    item = pipe(tex, (0, 0, 0), 'west', ['east'], v)
    return [('Nah: ein Rohr, Eingang rechts, Ausgang links', item, (0.5, 0.5, 0.5), 24, 520, 330),
            ('Gerade: Truhe → Rohr → Rohr → Truhe', straight, (2.0, 0.5, 0.5), 12, 760, 380),
            ('Kurve: Truhe → Rohr → Rohr (Knick) → Trichter', bend, (1.5, 0.45, 1.0), 13, 760, 440),
            ('Kreuzung: 3 Rohre + Truhe oben → Rohr → Trichter', cross, (1.5, 0.3, 1.5), 12, 760, 560)]


def uri(img: Image.Image) -> str:
    buf = io.BytesIO()
    img.save(buf, 'PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def render_variant(sheets, variant):
    out = []
    for title, parts, target, scale, w, h in scenes(sheets['pipe'], variant):
        img = rr.render(parts, w, h, scale, math.radians(-35), math.radians(30), target)
        out.append((title, uri(img)))
    # all four steps side by side (straight piece)
    row = Image.new('RGBA', (4 * 150, 170), (0, 0, 0, 0))
    for i, step in enumerate(gen.STEPS):
        img = rr.render(pipe(sheets[step], (0, 0, 0), 'east', ['west'], variant), 150, 170, 9, math.radians(-35), math.radians(30), (0.5, 0.5, 0.5))
        row.paste(img, (i * 150, 0), img)
    out.append(('Alle 4 Stufen (Eisen, Verstärkt, Netherit, Enderit), Ausgang rechts', uri(row)))
    return out


def flat(sheets):
    row = Image.new('RGBA', (4 * 272 - 16, 256), (0, 0, 0, 0))
    for i, step in enumerate(gen.STEPS):
        im = sheets[step].resize((256, 256), Image.NEAREST)
        row.paste(im, (i * 272, 0), im)
    return uri(row)


def ist_sheets():
    return {s: Image.open(ASSETS / f'textures/block/{s}.png').convert('RGBA') for s in gen.STEPS}


def build_page(path: Path):
    built = gen.build()
    data = {}
    ist = ist_sheets()
    data['ist'] = dict(renders=render_variant(ist, 'ist'), flat=flat(ist),
                       hard={s: s31.hard_cells(ist[s], 'pipe') for s in gen.STEPS})
    for v in gen.VARIANTS:
        sh = {s: built[(v, s)] for s in gen.STEPS}
        data[v] = dict(renders=render_variant(sh, v), flat=flat(sh), hard={s: gen.hard(sh[s]) for s in gen.STEPS})
    html = (HERE / 'rohr33_page.html').read_text()
    html = html.replace('/*DATA*/null', json.dumps(data))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(html)
    print(path, f'{path.stat().st_size / 1e6:.2f} MB')


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', type=Path, default=OUT)
    build_page(ap.parse_args().out)
