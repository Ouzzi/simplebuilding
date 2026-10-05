"""Generate P5/P6 resources. Run from the repository root; optional --check.

Plan: reuse the cauldron/barrel generators and owner's Enderite ramp; keep
Vanilla luminance and silhouettes, add crystal rivets and cracked diamond bands.
Validate every output and show Vanilla/current references beside new pixels at 16x.
"""
from pathlib import Path
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
V = ROOT / 'build/vanilla-textures'
# The older generators read argv at import time.
saved_argv = sys.argv
sys.argv = [sys.argv[0], str(V)]
import crucibles_2026_10_04 as crucibles
import copper_barrels_2026_10_05 as barrels
sys.argv = saved_argv

SB = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures'
LIB = ROOT / 'modules/simplelib/shared/resources/assets/simplelib/textures/block'
PREVIEWS = ROOT / 'build/crucible-previews'


def load(path):
    with Image.open(path) as img:
        return img.convert('RGBA')


def vanilla(name):
    return load(V / (name + '.png'))


def ramp(img):
    return sorted({p[:3] for p in img.get_flattened_data() if p[3]}, key=crucibles.lum)


ENDERITE = ramp(load(ROOT / 'src/main/resources/assets/simplebuilding/textures/block/enderite_block.png'))
SOUL = ramp(vanilla('block/soul_fire_0'))
CRYSTAL = (224, 180, 255)


def remap(img, colors, mask=None):
    a = np.array(img)
    selected = a[:, :, 3] > 0
    if mask is not None:
        selected &= mask
    if not selected.any():
        return img.copy()
    lum = a[:, :, :3] @ np.array([.299, .587, .114])
    lo, hi = lum[selected].min(), lum[selected].max()
    ranks = np.rint((lum - lo) / max(hi - lo, 1) * (len(colors) - 1)).astype(int)
    a[selected, :3] = np.array(colors)[ranks.clip(0, len(colors) - 1)[selected]]
    return Image.fromarray(a)


def crystal(img, x, y):
    d = ImageDraw.Draw(img)
    d.point((x, y - 1), fill=(*CRYSTAL, 255))
    d.line((x - 1, y, x + 1, y), fill=(*ENDERITE[-2], 255))
    d.point((x, y + 1), fill=(*ENDERITE[len(ENDERITE)//2], 255))


def enderite_crucible():
    tex = {part: remap(img, ENDERITE) for part, img in crucibles.tier('iron').items()}
    top_alpha = tex['top'].getchannel('A')
    for x in (3, 12):
        crystal(tex['side'], x, 9)
        crystal(tex['top'], x, 2)
    tex['top'].putalpha(top_alpha)
    crystal(tex['handle'], 7, 7)
    return tex


def enderite_barrel():
    tex = {part: barrels.copperize(barrels.load('barrel_' + part), ENDERITE)
           for part in ('side', 'top', 'bottom', 'top_open')}
    tex['flange'] = barrels.flange(ENDERITE)
    for x in (3, 12):
        crystal(tex['side'], x, 3)
        crystal(tex['side'], x, 12)
    for part in ('top', 'bottom', 'top_open', 'flange'):
        crystal(tex[part], 7, 2)
    return tex


def liquid_mask(img):
    a = np.array(img).astype(int)
    return (a[:, :, :3].max(2) - a[:, :, :3].min(2) > 25) & (a[:, :, 3] > 0)


def bucket(material=None, content=None):
    source = vanilla('item/' + (content + '_bucket' if content in ('water', 'lava') else
                               'lava_bucket' if content == 'soul_lava' else 'bucket'))
    liquid = liquid_mask(source) if content else np.zeros((16, 16), dtype=bool)
    out = source.copy()
    if material:
        # Block palettes lack the dark outline/interior tones of Vanilla items.
        # Anchor those below the block ramp; interpolate to retain every shade.
        dark = tuple(round(c * min(.55, 32 / crucibles.lum(material[0]))) for c in material[0])
        colors = np.array([dark, *material])
        stops = np.r_[53, np.linspace(114, 255, len(material))]
        if material is ENDERITE:
            # Enderite already has item-dark tones; spread its full ramp evenly.
            colors = np.array(material)
            stops = np.linspace(53, 255, len(material))
        a = np.array(source)
        metal = (a[:, :, 3] > 0) & ~liquid
        luminance = a[:, :, :3] @ np.array([.299, .587, .114])
        for channel in range(3):
            a[:, :, channel][metal] = np.rint(np.interp(luminance[metal], stops, colors[:, channel]))
        out = Image.fromarray(a)
    if content == 'soul_lava':
        out = remap(out, SOUL, liquid)
    return out


def reinforced_cauldron():
    tex = {p: vanilla('block/cauldron_' + p) for p in ('side', 'top', 'bottom', 'inner')}
    side = tex['side']
    crucibles.band(side, 9, [crucibles.DIAMOND[0], crucibles.DIAMOND[2]], (2, 7, 8, 13), crucibles.DIAMOND[3])
    d = ImageDraw.Draw(side)
    for x in (5, 10):
        d.point((x, 9), fill=(*crucibles.DIAMOND[0], 255))
        d.point((x + 1, 10), fill=(*crucibles.DIAMOND[1], 255))
    # Respect the cauldron's existing alpha mask (especially the leg cutout).
    for part in ('top', 'inner', 'bottom'):
        original = tex[part].getchannel('A')
        d = ImageDraw.Draw(tex[part])
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            d.point((x, y), fill=(*crucibles.DIAMOND[2], 255))
        tex[part].putalpha(original)
    return tex


def soul_burn():
    img = Image.new('RGBA', (18, 18))
    d = ImageDraw.Draw(img)
    d.polygon([(8, 1), (10, 4), (10, 7), (12, 5), (14, 8), (15, 11),
               (14, 14), (12, 16), (5, 16), (3, 14), (2, 11), (4, 7),
               (5, 10), (7, 8), (6, 5)], fill=(*SOUL[len(SOUL)//3], 255))
    d.polygon([(8, 6), (9, 10), (11, 9), (12, 12), (11, 14), (6, 14),
               (5, 12), (7, 10)], fill=(*SOUL[2*len(SOUL)//3], 255))
    d.line((8, 11, 8, 14), fill=(*SOUL[-1], 255), width=2)
    return img


def resources():
    """Return (destination, generated image, reference image) without writing."""
    rows = []
    for family, textures in (('crucible', enderite_crucible()), ('barrel', enderite_barrel())):
        for part, img in textures.items():
            old = load(LIB / f'{"netherite" if family == "crucible" else "reinforced"}_{family}_{part}.png')
            rows.append((SB / f'block/enderite_{family}_{part}.png', img, old))
    for kind in ('still', 'flow'):
        old = vanilla('block/lava_' + kind)
        rows.append((SB / f'block/soul_lava_{kind}.png', remap(old, SOUL), old))
    rows.append((SB / 'item/soul_lava_bucket.png', bucket(content='soul_lava'), vanilla('item/lava_bucket')))
    for stage, name in enumerate(('copper_block', 'exposed_copper', 'weathered_copper', 'oxidized_copper')):
        colors = ramp(vanilla('block/' + name))
        for content in (None, 'water', 'lava'):
            suffix = (content + '_' if content else '') + 'bucket'
            rows.append((SB / f'item/copper_{suffix}_{stage}.png', bucket(colors, content), vanilla('item/' + suffix)))
    for content in (None, 'water', 'lava', 'soul_lava'):
        suffix = (content + '_' if content else '') + 'bucket'
        old = vanilla('item/' + ('lava_bucket' if content == 'soul_lava' else suffix))
        rows.append((SB / f'item/enderite_{suffix}.png', bucket(ENDERITE, content), old))
    burn_reference = Image.new('RGBA', (18, 18))
    burn_reference.paste(vanilla('block/soul_fire_0').crop((0, 0, 16, 16)), (1, 1))
    rows.append((SB / 'mob_effect/soul_burn.png', soul_burn(), burn_reference))
    for part, img in reinforced_cauldron().items():
        rows.append((LIB / f'reinforced_cauldron_{part}.png', img, vanilla('block/cauldron_' + part)))
    return rows


def validate(path, img):
    size = (16, 320) if path.name == 'soul_lava_still.png' else (32, 512) if path.name == 'soul_lava_flow.png' else (18, 18) if path.name == 'soul_burn.png' else (16, 16)
    assert img.size == size and img.mode == 'RGBA', (path, img.size, img.mode)
    a = np.array(img)[:, :, 3]
    assert set(np.unique(a)) <= {0, 255}, path
    if path.parent.name in ('item', 'mob_effect'):
        assert not (a[0].any() or a[-1].any() or a[:, 0].any() or a[:, -1].any()), path


def sheet(path, title, labels, rows):
    """Each cell is native pixels enlarged exactly 16x, with readable captions."""
    font = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 19)
    widths = [max(row[col].width for _, row in rows) * 16 + 24 for col in range(len(labels))]
    heights = [max(img.height for img in row) * 16 + 42 for _, row in rows]
    canvas = Image.new('RGB', (240 + sum(widths), 90 + sum(heights)), '#292d35')
    d = ImageDraw.Draw(canvas)
    d.text((16, 10), title, font=font, fill='white')
    x = 240
    for label, width in zip(labels, widths):
        d.text((x, 51), label, font=font, fill='#d0d7e2')
        x += width
    y = 90
    for (label, row), height in zip(rows, heights):
        d.text((12, y + 10), label, font=font, fill='white')
        x = 240
        for img, width in zip(row, widths):
            big = img.resize((img.width * 16, img.height * 16), Image.Resampling.NEAREST)
            for cy in range(0, big.height, 16):
                for cx in range(0, big.width, 16):
                    d.rectangle((x+cx, y+cy, x+cx+15, y+cy+15), fill='#414650' if (cx+cy)//16 % 2 else '#373c45')
            canvas.paste(big, (x, y), big)
            x += width
        y += height
    path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(path)


def main():
    check = '--check' in sys.argv
    rows = resources()
    assert len({path for path, _, _ in rows}) == 34
    for path, img, old in rows:
        validate(path, img)
        if path.parent.name != 'mob_effect':
            assert img.getchannel('A').tobytes() == old.getchannel('A').tobytes(), path
        if path.parent.name == 'item' and 'soul' not in path.name:
            mask = liquid_mask(old)
            assert np.array_equal(np.array(img)[mask], np.array(old)[mask]), path
        if path.parent.name == 'item' and path.name.startswith(('copper_', 'enderite_')):
            source, target = np.array(old), np.array(img)
            metal = (source[:, :, 3] > 0) & ~liquid_mask(old)
            outline = metal & (source[:, :, 0] == 53)
            brightness = target[:, :, :3] @ np.array([.299, .587, .114])
            assert np.array_equal(brightness == brightness[outline][0], outline), path
            # No merged shades: the empty interior and rim retain their relief.
            tones = sorted(set(source[:, :, 0][metal]))
            levels = [brightness[metal & (source[:, :, 0] == tone)][0] for tone in tones]
            assert all(a < b for a, b in zip(levels, levels[1:])), path
            assert brightness[outline][0] <= 33, path
        if check:
            with Image.open(path) as actual:
                assert actual.mode == 'RGBA' and actual.size == img.size, path
                assert actual.tobytes() == img.tobytes(), path
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            img.save(path)
    for kind in ('still', 'flow'):
        data = (V / f'block/lava_{kind}.png.mcmeta').read_bytes()
        path = SB / f'block/soul_lava_{kind}.png.mcmeta'
        if check:
            # Git can check out text resources with CRLF on Windows.
            assert path.read_bytes().replace(b'\r\n', b'\n') == data.replace(b'\r\n', b'\n'), path
        else:
            path.write_bytes(data)
    if not check:
        for group in ('block', 'item', 'mob_effect'):
            entries = [(p.stem.replace('_', '\n', 1), [old.crop((0, 0, old.width, old.width)), img.crop((0, 0, img.width, img.width))])
                       for p, img, old in rows if p.parent.name == group]
            sheet(PREVIEWS / f'platzhalter-{group}.png', 'P5/P6: Referenz alt / eingebaut neu (16x)', ['alt / Quelle', 'neu'], entries)
    print(f'P5/P6: {len(rows)} RGBA textures + 2 exact Vanilla metadata files: OK')


if __name__ == '__main__':
    main()
