"""Simple Dinosaurs: textures, models, block states and item models of the fossil block, the five eggs, the large saddle and the
three T. rex armors, from ONE spec (concept docs/ai/KONZEPT-A-WP18-2026-10-29.md section 3.2/3.3). Own pixel art drawn from code.

    python3 modules/simpledinos/tools/gen_blocks.py            write
    python3 modules/simpledinos/tools/gen_blocks.py --check    fail when a file is stale

Fossil block: ten stages (the bones come out of the sediment as the progress rises). Eggs (a-dino36b, owner DINO36): drawn in
eggs.py like the Vanilla turtle/sniffer eggs - six stacked boxes (dragon egg profile), a side and a lid texture per dinosaur, the
twelve brooding steps add a translucent warm sheen in four steps, the three crack steps add hairline cracks (side and lid).
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
sys.path.insert(0, str(ROOT / 'tools/textures'))
import layers as L  # noqa: E402
import texrate16_rework_2026_11_09 as RW  # noqa: E402  A8d rework of the texrate16/17 targets (b-texrate17)

NS = 'simpledinos'
ASSETS = MODULE / 'shared/resources/assets' / NS
KINDS = ('trex', 'triceratops', 'stegosaurus', 'utahraptor', 'parasaurolophus')
STAGES = 10
PHASES = 15


def H(s):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def hsh(x, y, s):
    n = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


# ---------------------------------------------------------------- fossil block
BONES = ["................",
         ".....xxxxxx.....",
         "....xxxxxxxx....",
         "....xx.xx.xx....",
         "....xxxxxxxx....",
         ".....xx..xx.....",
         "..x...xxxx...x..",
         ".xxx.xx..xx.xxx.",
         "..x.xx.xx.xx.x..",
         "....xx.xx.xx....",
         "...xx..xx..xx...",
         "...x...xx...x...",
         "......xxxx......",
         "................",
         "................",
         "................"]


def fossil_stage(stage: int):
    """Sediment (sandstone-like) with the fossil: the bones are covered at stage 0 and fully free at stage 9."""
    sed, sed_d, sed_l = H('b39c6e'), H('7a6642'), H('c9b588')
    bone, bone_d, bone_l = H('efe6cc'), H('b8aa84'), H('fbf6e4')
    pixels = [(x, y) for y, row in enumerate(BONES) for x, ch in enumerate(row) if ch == 'x']
    # reveal in a stable random order
    pixels.sort(key=lambda p: hsh(p[0], p[1], 77))
    shown = set(pixels[: round(len(pixels) * stage / (STAGES - 1))])
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = hsh(x, y, 5)
            c = sed
            if n > 0.78:
                c = sed_d
            elif n < 0.14:
                c = sed_l
            # the dust thins out around the revealed bones, so the layer lifts step by step
            if (x, y) in shown:
                c = bone if hsh(x, y, 9) > 0.18 else bone_d
                if y > 0 and (x, y - 1) not in shown and hsh(x, y, 11) > 0.4:
                    c = bone_l
            elif (x, y) in set(pixels) and stage > 0:
                c = mix(c, bone_d, 0.18 * stage / (STAGES - 1))
            px[x, y] = c + (255,)
    return img


# ---------------------------------------------------------------- eggs (a-dino36b: eggs.py, variant eggs.CHOSEN)
sys.path.insert(0, str(Path(__file__).resolve().parent))
import eggs as EG  # noqa: E402


def egg_base(kind: str):
    return EG.egg_side(kind, snap=L.snap)


def egg_lid(kind: str):
    return EG.egg_top(kind, snap=L.snap)


def egg_overlay(phase: int):
    return EG.overlay_side(phase)


def lid_overlay(phase: int):
    """Lid overlay of a phase: clear while brooding, cracks 1..3 in the crack phases."""
    return EG.overlay_top(max(0, phase - (PHASES - 4)))


# ---------------------------------------------------------------- items
# Owner choice 31.10. (IG31-23, preview b-optik32): large saddle = variant C "Hochlehne", T. rex armor = variant C (all three tiers).
# Drawn by tools/textures/optik32_items.py (Endwood process A8b); one layer source per sprite.
sys.path.insert(0, str(ROOT / 'tools/textures'))
import optik32_items as OI  # noqa: E402

ARMOR_TIERS = ('leather', 'iron', 'diamond')


def chosen(img, name):
    """A copy of a chosen proposal with its layer source (the proposal sprite is shared, so it is copied first)."""
    out = img.copy()
    return L.snap(out, name)


def saddle_image():
    return chosen(OI.saddle('C'), 'Sattel')


def armor_image(tier):
    return chosen(OI.armor('C', tier), 'Platte')


def grid_image(rows, colors, groups, base='Grundfarbe'):
    cols = {k: H(v) for k, v in colors.items()}
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in cols:
                px[x, y] = cols[ch] + (255,)
    return L.snap_grid(img, rows, {k: v + (255,) for k, v in cols.items()}, groups, base)


# ---------------------------------------------------------------- JSON
def block_model_cube(texture: str):
    return {'parent': 'minecraft:block/cube_all', 'textures': {'all': texture}}


def egg_parent_model():
    return EG.egg_parent_model()


def dumps(obj):
    return json.dumps(obj, indent=2, ensure_ascii=False) + '\n'


def outputs():
    """{path: PIL image | str} of everything this script owns."""
    files = {}
    tb, ti, mb, mi, bs, it = (ASSETS / 'textures/block', ASSETS / 'textures/item', ASSETS / 'models/block', ASSETS / 'models/item',
                              ASSETS / 'blockstates', ASSETS / 'items')
    for s in range(STAGES):
        files[tb / f'fossil_{s}.png'] = fossil_stage(s)
        files[mb / f'fossil_{s}.json'] = dumps(block_model_cube(f'{NS}:block/fossil_{s}'))
    files[bs / 'fossil_block.json'] = dumps({'variants': {f'stage={s}': {'model': f'{NS}:block/fossil_{s}'} for s in range(STAGES)}})
    files[it / 'fossil_block.json'] = dumps({'model': {'type': 'minecraft:model', 'model': f'{NS}:block/fossil_9'}})
    for kind in KINDS:
        files[tb / f'egg_{kind}.png'] = egg_base(kind)
        files[tb / f'egg_{kind}_top.png'] = egg_lid(kind)
    for p in range(PHASES):
        files[tb / f'egg_overlay_{p}.png'] = egg_overlay(p)
    for c in range(4):
        files[tb / f'egg_overlay_top_{c}.png'] = EG.overlay_top(c)
    files[mb / 'dino_egg.json'] = dumps(egg_parent_model())
    for kind in KINDS:
        for p in range(PHASES):
            files[mb / f'{kind}_egg_{p}.json'] = dumps({'parent': f'{NS}:block/dino_egg', 'textures': {
                'side': f'{NS}:block/egg_{kind}', 'top': f'{NS}:block/egg_{kind}_top', 'overlay': f'{NS}:block/egg_overlay_{p}',
                'overlay_top': f'{NS}:block/egg_overlay_top_{max(0, p - (PHASES - 4))}'}})
        files[bs / f'{kind}_egg.json'] = dumps({'variants': {f'phase={p}': {'model': f'{NS}:block/{kind}_egg_{p}'} for p in range(PHASES)}})
        # the item shows the fresh egg (phase 0), not a brooded one
        files[it / f'{kind}_egg.json'] = dumps({'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{kind}_egg_0'}})
    files[ti / 'large_saddle.png'] = saddle_image()
    for tier in ARMOR_TIERS:
        files[ti / f'trex_armor_{tier}.png'] = armor_image(tier)
    for name in ('large_saddle', 'trex_armor_leather', 'trex_armor_iron', 'trex_armor_diamond'):
        files[mi / f'{name}.json'] = dumps({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})
        files[it / f'{name}.json'] = dumps({'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}'}})
    return files


def preview_images(image_uri):
    """Preview cards for the HTML page: fossil stages, egg kinds, egg brooding and crack steps, items (data URIs via image_uri(path))."""
    import tempfile
    cards = []
    with tempfile.TemporaryDirectory() as tmp:
        def uri(img, scale=6):
            p = Path(tmp) / 'x.png'
            img.save(p)
            return image_uri(p, scale)

        cards.append(dict(title='Fossilienblock: 10 Stufen beim Pinseln', text='Die Knochen kommen Stufe für Stufe aus dem Sediment (etwa 60 s Pinseln).',
                          items=[dict(src=uri(fossil_stage(s)), label=f'Stufe {s}') for s in range(STAGES)]))
        cards.append(dict(title='Die fünf Eier (Seite)', text='Je Dinosaurier ein eigenes Muster (a-dino36b, Variante ' + EG.CHOSEN + '). In Stufe 1 schlüpft nur der T-Rex.',
                          items=[dict(src=uri(egg_base(k)), label=k) for k in KINDS]))
        cards.append(dict(title='Die fünf Eier (Deckel)', text='Spitze und Ringe von oben.', items=[dict(src=uri(egg_lid(k)), label=k) for k in KINDS]))
        base = egg_base('trex')
        glow = []
        for p in (0, 3, 6, 9, 12, 13, 14):
            im = base.copy()
            im.alpha_composite(egg_overlay(p))
            glow.append(dict(src=uri(im), label=('Brüten ' + str(p + 1) + '/12') if p < 12 else f'Risse {p - 11}/3'))
        cards.append(dict(title='Ei: Brütstufen und Risse (T-Rex-Ei)', text='Warmer Schimmer in vier Stufen von unten, danach drei Rissstufen bis zum Schlüpfen.', items=glow))
        gear = [dict(src=uri(saddle_image()), label='Großer Sattel')]
        for tier in ARMOR_TIERS:
            gear.append(dict(src=uri(armor_image(tier)), label='T-Rex-Rüstung ' + tier))
        cards.append(dict(title='Sattel und Rüstungen', text='Items; am T-Rex erscheinen sie als Sattel bzw. Platten auf Kopf und Rücken.', items=gear))
    return cards


def main(argv):
    check = '--check' in argv
    stale = []
    for path, content in outputs().items():
        if isinstance(content, str):
            if not path.exists() or path.read_text(encoding='utf-8') != content:
                stale.append(path)
                if not check:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_text(content, encoding='utf-8')
        else:
            content = RW.finish(content, path)  # b-texrate17: egg_utahraptor etc. keep the A8d rework on re-runs and in --check
            same = path.exists() and Image.open(path).convert('RGBA').tobytes() == content.convert('RGBA').tobytes()
            if not same:
                stale.append(path)
                if not check:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    L.snap(content, 'Grundform') if not L.steps_of(content) else None
                    L.save(content, path)
    if check and stale:
        print('stale: ' + ', '.join(str(p.relative_to(ROOT)) for p in stale))
        return 1
    print('simpledinos blocks/items: %d files %s' % (len(outputs()), 'current' if not stale else 'written'))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
