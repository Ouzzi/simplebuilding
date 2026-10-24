"""Set-down bags on floor and wall (B23-M11/M12, docs/ai/PLAN-A-MECH23B-2026-10-23.md), Simple Storage.

Writes into modules/simplestorage/shared/resources/assets/simplestorage:
  - models/block/template_placed_quiver.json and textures/block/placed_<tier>quiver.png (+ .layers.json): the placed
    quiver - an upright leather tube with rim and strap, arrows sticking out (own pixels; the tones are sampled from the
    tier's quiver item texture, so every tier keeps its colours);
  - the wall copies of every template (``template_placed_bundle[_dyed]``, ``template_placed_quiver``,
    ``template_backpack[_dyed]`` -> ``..._wall``): the same elements moved back against the wall and a little up
    (PlacedBundleBlock.WALL_SHIFT, BackpackBlock.WALL_SHIFT/WALL_LIFT - the hit shapes in Java must match), cullfaces
    dropped (the faces no longer lie on the block border), and a ``_wall`` child of every tier model;
  - blockstates/placed_bundle.json and the four backpack blockstates with the ``wall`` property.

W24-2 (a-tex24b): the placed quiver shows its fill level (owner: "5 Pfeile drin und den Fuellstand zeigen, dann je nach
dem immer weniger"): template_placed_quiver_<n> for n = 0..5 arrows (the first n of the five arrow spots), a model per tier
and count (``placed_<tier>_<n>``, PlacedBundleBlock.ARROWS) and the ``arrows`` property in the blockstate.

Run from the repo root: python3.12 tools/textures/placed_storage_wall_models.py
"""
from __future__ import annotations

import argparse
import copy
import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))
from layers import save, snap  # noqa: E402

ASSETS = ROOT / 'modules/simplestorage/shared/resources/assets/simplestorage'
MODELS = ASSETS / 'models/block'
NS = 'simplestorage'

BUNDLE_TIERS = ['bundle', 'reinforced', 'netherite', 'enderite']
QUIVER_TIERS = ['quiver', 'reinforced_quiver', 'netherite_quiver', 'enderite_quiver']
FACINGS = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
# (dz towards the wall, dy up) per template family; must match the Java shapes.
SHIFT = {'placed': (3, 3), 'backpack': (5, 1)}


def dump(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, 'w', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


# ------------------------------------------------------------------------------------------------- placed quiver

def ramp_from_item(tier: str):
    """Five leather tones (dark -> light), the fletching pair and an accent, sampled from the quiver item sprite."""
    im = Image.open(ASSETS / f'textures/item/{tier}.png').convert('RGBA')
    px = [im.getpixel((x, y)) for y in range(16) for x in range(16)]
    px = [p for p in px if p[3] > 200]

    def lum(p):
        return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]

    def sat(p):
        return max(p[:3]) - min(p[:3])

    import colorsys
    grey = [p for p in px if sat(p) < 18 and lum(p) > 150]

    def hue(p):
        return colorsys.rgb_to_hsv(*(c / 255 for c in p[:3]))[0] * 360

    # The leather is the most common hue of the sprite (the strap and fittings in other hues stay out).
    body = [p for p in px if p not in grey and lum(p) > 22]
    if len({p for p in body if sat(p) >= 16}) >= 5:
        body = [p for p in body if sat(p) >= 16]      # greys have no hue (they would count as red)
    buckets = {}
    for p in body:
        buckets[int(hue(p) // 30)] = buckets.get(int(hue(p) // 30), 0) + 1
    main = max(buckets, key=buckets.get)
    leather = sorted({p for p in body if int(hue(p) // 30) in (main, (main + 1) % 12, (main - 1) % 12)}, key=lum)
    pick = [leather[round(i * (len(leather) - 1) / 4)] for i in range(5)]
    feather = sorted(set(grey), key=lum) or [(220, 220, 220, 255)]
    accent = max(px, key=sat) if tier.startswith('enderite') else None
    return pick, (feather[0], feather[-1]), accent


SHAFT = [(92, 66, 38, 255), (124, 92, 54, 255)]
TIP = [(110, 110, 116, 255), (170, 170, 178, 255)]
INSIDE = (30, 22, 18, 255)

# Sheet layout (32x32, one texel per model pixel): name -> (x, y, w, h)
Q = {
    'side_n': (0, 0, 6, 11), 'side_e': (6, 0, 6, 11), 'side_s': (12, 0, 6, 11), 'side_w': (18, 0, 6, 11),
    'top': (0, 12, 6, 6), 'bottom': (6, 12, 6, 6),
    'shaft': (24, 0, 1, 4), 'feather': (26, 0, 3, 4),
}


def quiver_sheet(tier: str, arrows: int) -> Image.Image:
    leather, (fl_dark, fl_light), accent = ramp_from_item(tier)
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    # Base: the tube walls - a round tube, lit from the front-left: per column a tone of the cylinder.
    cyl = [1, 2, 3, 3, 2, 1]
    for name in ('side_n', 'side_e', 'side_s', 'side_w'):
        x0, y0, w, h = Q[name]
        for x in range(w):
            for y in range(h):
                im.putpixel((x0 + x, y0 + y), leather[cyl[x]])
    snap(im, 'Leder')
    for name in ('side_n', 'side_e', 'side_s', 'side_w'):
        x0, y0, w, h = Q[name]
        for x in range(w):
            im.putpixel((x0 + x, y0), leather[4 if 1 <= x <= 4 else 3])      # rolled rim, catches the light
            im.putpixel((x0 + x, y0 + 1), leather[0])                         # shadow under the rim
            im.putpixel((x0 + x, y0 + h - 1), leather[0])                     # sole seam
            im.putpixel((x0 + x, y0 + 7), leather[1] if cyl[x] > 1 else leather[0])  # strap band
        if name == 'side_n':
            im.putpixel((x0 + 2, y0 + 7), leather[4])                         # buckle
            im.putpixel((x0 + 3, y0 + 7), leather[3])
            for y in (3, 5):                                                  # stitches down the front
                im.putpixel((x0 + 2, y0 + y), leather[cyl[2] - 1])
        if name == 'side_s':
            for y in range(2, 10):                                            # back seam
                im.putpixel((x0 + 3, y0 + y), leather[0] if y % 2 else leather[1])
    snap(im, 'Rand, Riemen, Naehte')
    if accent:
        x0, y0, _, _ = Q['side_n']
        for (x, y) in ((3, 4), (2, 9)):
            im.putpixel((x0 + x, y0 + y), accent)
        snap(im, 'Kristalle')
    x0, y0, w, h = Q['top']
    for x in range(w):
        for y in range(h):
            edge = x in (0, w - 1) or y in (0, h - 1)
            im.putpixel((x0 + x, y0 + y), leather[3] if edge else INSIDE)
    x0, y0, w, h = Q['bottom']
    for x in range(w):
        for y in range(h):
            im.putpixel((x0 + x, y0 + y), leather[0] if x in (0, w - 1) or y in (0, h - 1) else leather[1])
    x0, y0, _, _ = Q['shaft']
    for y in range(4):
        im.putpixel((x0, y0 + y), SHAFT[y % 2])
    x0, y0, w, h = Q['feather']
    for y in range(h):
        for x in range(w):
            if y == 0 and x != 1:
                continue                                                      # pointed feather tips
            im.putpixel((x0 + x, y0 + y), fl_light if x == 1 or y == 1 else fl_dark)
    im.putpixel((x0 + 1, y0 + 3), SHAFT[0])                                   # nock
    return snap(im, 'Oeffnung, Boden, Pfeile')


def uvq(name, dx=0, dy=0, w=None, h=None):
    x0, y0, W, H = Q[name]
    w = W if w is None else w
    h = H if h is None else h
    return [(x0 + dx) / 2, (y0 + dy) / 2, (x0 + dx + w) / 2, (y0 + dy + h) / 2]


# Arrow spots (x, z) in the tube opening and their height (variant B, five arrows), in the order they appear while the
# quiver fills: one arrow stands in the middle, the full quiver shows all five.
ARROW_SPOTS = [(8.0, 7.6, 16), (6.3, 6.6, 15), (9.4, 8.2, 15.5), (8.7, 6.4, 14.5), (6.6, 8.9, 15.5)]
MAX_ARROWS = len(ARROW_SPOTS)


def quiver_model(arrows: int):
    E = []
    tex = '#quiver'
    # The tube: a 6x6 footprint with one stepped corner per side (no coplanar overlaps): middle band + front/back rows.
    for (x0, x1, z0, z1, inner) in [(5, 11, 6, 10, None), (6, 10, 5, 6, 'south'), (6, 10, 10, 11, 'north')]:
        faces = {}
        w, d = x1 - x0, z1 - z0
        sl = {'north': ('side_n', 11 - x1, w), 'south': ('side_s', x0 - 5, w), 'west': ('side_w', z0 - 5, d), 'east': ('side_e', 11 - z1, d)}
        for side, (region, dx, fw) in sl.items():
            if side == inner:
                continue
            faces[side] = {"uv": uvq(region, dx, 0, fw, 11), "texture": tex}
        faces['up'] = {"uv": uvq('top', x0 - 5, z0 - 5, w, d), "texture": tex}
        faces['down'] = {"uv": uvq('bottom', x0 - 5, 11 - z1, w, d), "texture": tex, "cullface": "down"}
        E.append({"from": [x0, 0, z0], "to": [x1, 11, z1], "faces": faces})
    for (x, z, top) in ARROW_SPOTS[:arrows]:
        # shaft out of the opening, then the fletching as two crossed planes (like Vanilla's arrow)
        E.append({"from": [x - 0.5, 10, z - 0.5], "to": [x + 0.5, top - 2.5, z + 0.5], "faces": {
            s: {"uv": uvq('shaft'), "texture": tex} for s in ('north', 'south', 'east', 'west')}})
        for rot_axis, f, t in (('x', [x - 1.5, top - 3, z], [x + 1.5, top, z]), ('z', [x, top - 3, z - 1.5], [x, top, z + 1.5])):
            sides = ('north', 'south') if rot_axis == 'x' else ('east', 'west')
            E.append({"from": f, "to": t, "shade": False, "faces": {s: {"uv": uvq('feather'), "texture": tex} for s in sides}})
    return {"__comment": "Abgestellter Koecher (PlacedBundleBlock, Stufe *_quiver, B23-M11): Lederrohr 6x11x6 mit gestuften Ecken, "
                        f"Rand, Riemen mit Schnalle, {arrows} Pfeile mit gekreuzten Federn (Fuellstand, W24-2). Textur 32x32, ein Texel "
                        "je Modellpixel; erzeugt von tools/textures/placed_storage_wall_models.py. Trefferform PlacedBundleBlock#QUIVER_NORTH.",
            "parent": "block/block", "ambientocclusion": False,
            "textures": {"particle": "#quiver"}, "elements": E}


# ------------------------------------------------------------------------------------------------- wall copies

def wall_copy(model: dict, dz: float, dy: float) -> dict:
    m = copy.deepcopy(model)
    for e in m.get('elements', []):
        e['from'] = [e['from'][0], e['from'][1] + dy, e['from'][2] + dz]
        e['to'] = [e['to'][0], e['to'][1] + dy, e['to'][2] + dz]
        if 'rotation' in e:
            o = e['rotation']['origin']
            e['rotation']['origin'] = [o[0], o[1] + dy, o[2] + dz]
        for f in e['faces'].values():
            f.pop('cullface', None)
    if '__comment' in m:
        m['__comment'] = ("An der Wand (B23-M12): " + m['__comment'] if not m['__comment'].startswith('An der Wand')
                          else m['__comment'])
    return m


def child_wall(child: str, template: str) -> None:
    m = json.loads((MODELS / f'{child}.json').read_text())
    parent = m['parent']
    assert parent.endswith(template), (child, parent)
    m['parent'] = parent + '_wall'
    dump(MODELS / f'{child}_wall.json', m)


def blockstate_placed_bundle() -> dict:
    variants = {}
    for arrows in range(MAX_ARROWS + 1):
        for dyed in (False, True):
            for facing, y in sorted(FACINGS.items()):
                for tier in BUNDLE_TIERS + QUIVER_TIERS:
                    for wall in (False, True):
                        if tier in QUIVER_TIERS:
                            model = f'placed_{tier}_{arrows}'                     # quivers stand undyed for now
                        else:
                            model = 'placed_bundle' if tier == 'bundle' else f'placed_{tier}_bundle'
                            if dyed:
                                model += '_dyed'
                        if wall:
                            model += '_wall'
                        v = {"model": f"{NS}:block/{model}"}
                        if y:
                            v["y"] = y
                        variants[f"arrows={arrows},dyed={str(dyed).lower()},facing={facing},tier={tier},wall={str(wall).lower()}"] = v
    return {"variants": variants}


def blockstate_backpack(name: str) -> dict:
    variants = {}
    for dyed in (False, True):
        for facing, y in sorted(FACINGS.items()):
            for wall in (False, True):
                model = name + ('_dyed' if dyed else '') + ('_wall' if wall else '')
                v = {"model": f"{NS}:block/{model}"}
                if y:
                    v["y"] = y
                variants[f"dyed={str(dyed).lower()},facing={facing},wall={str(wall).lower()}"] = v
    return {"variants": variants}


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('--out', type=Path, default=ASSETS, help='asset root to write (default: the module)')
    args = ap.parse_args(argv)
    out = args.out
    models, tex = out / 'models/block', out / 'textures/block'
    # placed quiver: one template per arrow count, one sheet per tier, one model per tier and count (W24-2)
    for arrows in range(MAX_ARROWS + 1):
        dump(models / f'template_placed_quiver_{arrows}.json', quiver_model(arrows))
    for tier in QUIVER_TIERS:
        tex.mkdir(parents=True, exist_ok=True)
        save(quiver_sheet(tier, MAX_ARROWS), tex / f'placed_{tier}.png')
        for arrows in range(MAX_ARROWS + 1):
            dump(models / f'placed_{tier}_{arrows}.json', {"parent": f"{NS}:block/template_placed_quiver_{arrows}",
                                                           "textures": {"quiver": f"{NS}:block/placed_{tier}", "particle": f"{NS}:block/placed_{tier}"}})
    # wall templates and children
    for template, fam in (('template_placed_bundle', 'placed'), ('template_placed_bundle_dyed', 'placed'),
                          ('template_backpack', 'backpack'), ('template_backpack_dyed', 'backpack')) + tuple(
                             (f'template_placed_quiver_{n}', 'placed') for n in range(MAX_ARROWS + 1)):
        src = models / f'{template}.json'
        if not src.exists():
            src = MODELS / f'{template}.json'
        dz, dy = SHIFT[fam]
        dump(models / f'{template}_wall.json', wall_copy(json.loads(src.read_text()), dz, dy))
    if out == ASSETS:
        for tier in BUNDLE_TIERS:
            base = 'placed_bundle' if tier == 'bundle' else f'placed_{tier}_bundle'
            child_wall(base, 'template_placed_bundle')
            child_wall(base + '_dyed', 'template_placed_bundle_dyed')
        for tier in QUIVER_TIERS:
            for n in range(MAX_ARROWS + 1):
                child_wall(f'placed_{tier}_{n}', f'template_placed_quiver_{n}')
        for name in ('backpack', 'reinforced_backpack', 'netherite_backpack', 'enderite_backpack'):
            child_wall(name, 'template_backpack')
            child_wall(name + '_dyed', 'template_backpack_dyed')
            dump(out / f'blockstates/{name}.json', blockstate_backpack(name))
        dump(out / 'blockstates/placed_bundle.json', blockstate_placed_bundle())
    print('placed storage models written to', out)


if __name__ == '__main__':
    main()
