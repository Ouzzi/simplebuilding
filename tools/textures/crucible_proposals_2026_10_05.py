"""Preview-only P6 proposals; never writes mod resources.

Plan: place installed placeholders beside A (riveted bands), B (vertical ribs)
and C (stepped fittings); retain each material's palette and Vanilla silhouettes.
Lava gets distinct pool/channel geometry, buckets distinct inset metal fittings.
Usage: python tools/textures/crucible_proposals_2026_10_05.py [preview-directory]
"""
from pathlib import Path
import sys
import numpy as np
from PIL import Image, ImageDraw
import crucible_placeholders_2026_10_05 as p


def fittings(img, variant, accent, part, family, tier):
    out = img.copy()
    d = ImageDraw.Draw(out)
    dark, light = accent[0], accent[-1]
    mid = accent[len(accent)//2]
    colors = [(*c, 255) for c in (dark, mid, light)]
    # All marks are inset. Top and side drawings are coordinated, not identical.
    if part == 'top':
        if variant == 'A':
            for x, y in ((3, 2), (12, 2), (3, 13), (12, 13)):
                d.line((x, y, x+1, y), fill=colors[2])
        elif variant == 'B':
            for x in (4, 11):
                d.line((x, 1, x, 3), fill=colors[1])
                d.line((x, 12, x, 14), fill=colors[2])
        else:
            for points in (((2, 5), (2, 2), (5, 2)), ((10, 2), (13, 2), (13, 5)),
                           ((2, 10), (2, 13), (5, 13)), ((10, 13), (13, 13), (13, 10))):
                d.line(points, fill=colors[1])
    elif variant == 'A':
        for y in ((4, 11) if family == 'barrel' else (8,)):
            d.line((1, y, 14, y), fill=colors[1])
            d.line((1, y+1, 14, y+1), fill=colors[0])
            for x in (3, 7, 12):
                d.point((x, y), fill=colors[2])
    elif variant == 'B':
        for x in (4, 11):
            d.line((x, 3, x, 12), fill=colors[1])
            d.point((x, 4), fill=colors[2])
            d.point((x, 11), fill=colors[2])
        d.line((5, 12, 10, 12), fill=colors[0])
    else:
        d.line(((2, 5), (4, 5), (4, 7), (6, 7)), fill=colors[1])
        d.line(((13, 5), (11, 5), (11, 7), (9, 7)), fill=colors[1])
        d.line(((2, 11), (5, 11), (5, 12), (10, 12), (10, 11), (13, 11)), fill=colors[1])
        for x in (3, 12):
            d.point((x, 5), fill=colors[2])
    # Tier-specific construction details remain visible within every proposal.
    if tier == 'reinforced':
        if part == 'side':
            d.line(((7, 5), (8, 6), (7, 7)), fill=colors[1])
            d.point((8, 5), fill=colors[2])
        else:
            d.line((6, 2, 9, 2), fill=colors[1])
            d.point((7, 2), fill=colors[0])
    elif tier == 'netherite':
        if part == 'side':
            d.line(((5, 3), (5, 4), (10, 4), (10, 3)), fill=colors[1])
            d.line((7, 3, 8, 3), fill=colors[2])
        else:
            d.line((1, 6, 1, 9), fill=colors[1])
            d.line((14, 6, 14, 9), fill=colors[2])
    elif tier == 'enderite':
        p.crystal(out, 7 if part == 'side' else 2, 5 if part == 'side' else 7)
    elif tier == 'copper' and part == 'side':
        d.line((7, 7, 8, 7), fill=colors[2])
    out.putalpha(img.getchannel('A'))
    return out


def blocks(family):
    tiers = ('iron', 'reinforced', 'netherite', 'enderite') if family == 'crucible' else ('copper', 'reinforced', 'enderite')
    labels = {'iron': 'Eisen', 'reinforced': 'Verstaerkt', 'netherite': 'Netherit', 'enderite': 'Enderit', 'copper': 'Kupfer'}
    rows = []
    for variant in ('alt', 'A', 'B', 'C'):
        cells = []
        for tier in tiers:
            for part in ('side', 'top'):
                path = (p.SB / 'block' if tier == 'enderite' else p.LIB) / f'{tier}_{family}_{part}.png'
                old = p.load(path)
                if variant == 'alt':
                    cells.append(old)
                    continue
                if family == 'crucible':
                    base = p.vanilla('block/cauldron_' + part)
                    if tier in ('netherite', 'enderite'):
                        base = p.remap(base, p.ENDERITE if tier == 'enderite' else p.crucibles.NETHERITE)
                else:
                    base = p.barrels.copperize(p.vanilla('block/barrel_' + part), p.ENDERITE if tier == 'enderite' else p.ramp(p.vanilla('block/copper_block')))
                accent = (p.crucibles.DIAMOND if tier == 'reinforced' else p.crucibles.GOLD if tier == 'netherite' else
                          [p.ENDERITE[0], p.ENDERITE[-2], p.CRYSTAL] if tier == 'enderite' else p.ramp(base))
                cells.append(fittings(base, variant, accent, part, family, tier))
        rows.append((variant, cells))
    columns = [labels[t] + ' / ' + part for t in tiers for part in ('Seite', 'Oben')]
    return columns, rows


def lava_variant(source, variant):
    """Synthesize different tileable channel shapes, retaining Vanilla microtexture."""
    a = np.array(source)
    h, w = a.shape[:2]
    y, x = np.mgrid[:h, :w]
    luminance = a[:, :, :3] @ np.array([.299, .587, .114])
    detail = (luminance - luminance.min()) / max(float(np.ptp(luminance)), 1)
    if variant == 'A':
        # Broad cellular pools separated by narrow bright seams.
        shape = (np.cos(2*np.pi*x/w*2) + np.cos(2*np.pi*y/h*2)) / 4 + .5
        field = .65*shape + .35*detail
    elif variant == 'B':
        # Meandering diagonal channels; periodic at both tile boundaries.
        wave = np.sin(2*np.pi*(x/w + y/h) + .8*np.sin(2*np.pi*y/h))
        field = .65*(1-np.abs(wave)) + .35*detail
    else:
        # Small eddies around scattered hot cores, no random seam discontinuities.
        shape = np.cos(2*np.pi*x/w*3 + np.sin(2*np.pi*y/h)) * np.cos(2*np.pi*y/h*2)
        field = .6*(shape*.5+.5) + .4*detail
    ranks = np.rint(field.clip(0, 1)*(len(p.SOUL)-1)).astype(int)
    a[:, :, :3] = np.array(p.SOUL)[ranks]
    return Image.fromarray(a)


def buckets():
    names = [f'copper_bucket_{n}' for n in range(4)] + ['enderite_bucket', 'soul_lava_bucket']
    rows = []
    for variant in ('alt', 'A', 'B', 'C'):
        cells = []
        for name in names:
            img = p.load(p.SB / f'item/{name}.png')
            original = np.array(img)
            if variant != 'alt':
                source = p.vanilla('item/lava_bucket' if name == 'soul_lava_bucket' else 'item/bucket')
                metal = ~p.liquid_mask(source) & (np.array(source)[:, :, 3] > 0)
                colors = sorted(set(map(tuple, original[metal, :3])), key=p.crucibles.lum)
                d = ImageDraw.Draw(img)
                dark, mid, light = [(*colors[i], 255) for i in (1, len(colors)//2, -1)]
                if variant == 'A':
                    d.rectangle((3, 9, 12, 11), fill=dark)
                    d.line((3, 10, 12, 10), fill=mid)
                    for x in (4, 7, 10):
                        d.point((x, 10), fill=light)
                elif variant == 'B':
                    for x in (4, 7, 10):
                        d.line((x, 7, x, 13), fill=dark)
                        d.line((x+1, 7, x+1, 13), fill=light)
                else:
                    # Thicken the front rim downward, leaving the opening intact.
                    d.line((3, 7, 12, 7), fill=light)
                    d.line((3, 8, 12, 8), fill=dark)
                    for x in (3, 11):
                        d.rectangle((x, 8, x+1, 10), fill=dark)
                        d.point((x, 9), fill=light)
                base = np.array(source)
                protected = (base[:, :, 3] == 0) | (base[:, :, 0] == 53) | p.liquid_mask(source)
                protected[:7] = True
                changed = np.array(img)
                changed[protected] = original[protected]
                img = Image.fromarray(changed)
                assert np.count_nonzero(np.any(changed != original, axis=2)) >= 18, name
            p.validate(p.SB / f'item/{name}.png', img)
            cells.append(img)
        rows.append((variant, cells))
    for col in range(len(names)):
        for a in range(1, 4):
            for b in range(a+1, 4):
                assert np.count_nonzero(np.any(np.array(rows[a][1][col]) != np.array(rows[b][1][col]), axis=2)) >= 20
    return ['Kupfer', 'Angelaufen', 'Verwittert', 'Oxidiert', 'Enderit', 'Seelenlava'], rows


def main():
    destination = Path(sys.argv[1]) if len(sys.argv) > 1 else p.ROOT / 'build/previews'
    # A proposal destination must be a previews directory, never a resource tree.
    assert destination.name == 'previews' and 'resources' not in destination.parts, destination
    before = {path: path.read_bytes() for path, _, _ in p.resources()}
    for family, filename in (('crucible', 'crucible-stufen'), ('barrel', 'fass-stufen')):
        columns, rows = blocks(family)
        comparison(destination / f'{filename}-vorschlaege-vorschau.png',
                   'A Nietenband | B Rippen | C Stufenbeschlag - nur Vorschlaege, 16x', columns, rows)
    rows = []
    for variant in ('alt', 'A', 'B', 'C'):
        cells = []
        for kind, size in (('still', 16), ('flow', 32)):
            old = p.load(p.SB / f'block/soul_lava_{kind}.png').crop((0, 0, size, size))
            source = p.vanilla('block/lava_' + kind).crop((0, 0, size, size))
            cells.append(old if variant == 'alt' else lava_variant(source, variant))
        rows.append((variant, cells))
    comparison(destination / 'seelenlava-vorschlaege-vorschau.png',
               'A Becken | B Kanaele | C Wirbel - nur Vorschlaege, 16x', ['Stand / Frame 0', 'Fluss / Frame 0'], rows)
    columns, rows = buckets()
    comparison(destination / 'eimer-vorschlaege-vorschau.png',
               'A Nietenband | B Rippen | C breiter Rand + Henkelbeschlag - nur Vorschlaege, 16x', columns, rows)
    # Ensure alternatives differ in geometry even when color is ignored.
    for family in ('crucible', 'barrel'):
        _, rows = blocks(family)
        for col in range(len(rows[0][1])):
            assert len({row[col].convert('L').tobytes() for _, row in rows}) == 4
    for path, data in before.items():
        assert path.read_bytes() == data, f'Preview changed resource: {path}'
    print('4 preview sheets: alt/A/B/C, nearest-neighbor 16x; resource writes: 0')


def comparison(path, title, columns, rows):
    p.sheet(path, title, [label for label, _ in rows],
            [(label, [row[col] for _, row in rows]) for col, label in enumerate(columns)])


if __name__ == '__main__':
    main()
