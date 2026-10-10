"""Before/after 3D preview of the placed bone (owner N12: symmetric top/bottom), claude-q-texrest 2026-10-10.

Usage (repository root): python tools/textures/standing_bone_preview_2026_10_10.py <out.png>
Draws standing_bone (plain and stacked twice, up=true below) from the branch base and the working tree with the
renderer of standing_rods_2026_10_04.py.
"""
import io
import json
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
saved, sys.argv = sys.argv, sys.argv[:1]
import standing_rods_2026_10_04 as rods  # noqa: E402
sys.argv = saved
BASE = '9cb965759'
A = 'mc26_3/overlay/resources/assets/simplebuilding/'


def read(rel, before):
    if before:
        return subprocess.run(['git', 'show', f'{BASE}:{A}{rel}'], cwd=ROOT, capture_output=True, check=True).stdout
    return (ROOT / A / rel).read_bytes()


def column(d, mdl_up, mdl, tex, origin, yaw):
    qs = []
    for k, m in ((0, mdl_up), (16, mdl)):
        for el in m['elements']:
            qs += rods.quads(el, {'rod': tex}, lambda p, k=k: (p[0], p[1] + k, p[2]))
    rods.render(d, qs, lambda p: rods.iso(p, yaw), origin, 12)


def main(out):
    sheet = Image.new('RGBA', (760, 560), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    d.text((16, 8), 'Platzierter Knochen: vorher | nachher (oben/unten symmetrisch), je einzeln und zwei gestapelt', fill=(0, 0, 0))
    for i, before in enumerate((True, False)):
        tex = Image.open(io.BytesIO(read('textures/block/standing_bone.png', before))).convert('RGBA')
        mdl = json.loads(read('models/block/standing_bone.json', before))
        up = json.loads(read('models/block/standing_bone_up.json', before))
        x = 20 + i * 380
        d.text((x, 30), 'vorher' if before else 'nachher', fill=(0, 0, 0))
        sheet.alpha_composite(tex.resize((96, 96), Image.NEAREST), (x, 50))
        qs = []
        for el in mdl['elements']:
            qs += rods.quads(el, {'rod': tex}, lambda p: p)
        rods.render(d, qs, lambda p: rods.iso(p, 225), (x + 170, 330), 12)
        column(d, up, mdl, tex, (x + 290, 520), 45)
    sheet.save(out)
    return out


if __name__ == '__main__':
    print(main(sys.argv[1]))
