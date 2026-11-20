"""Usage: python tools/textures/storage_dye_palette.py <vanilla textures dir> [preview.png] [--write]
       python tools/textures/storage_dye_palette.py --write-only   (26.3 overlay neutral layers, no Vanilla needed)
       python tools/textures/storage_dye_palette.py --check        (exit 1 when the overlay layers are stale)

Dye palette for the mod's bundles, backpacks and quivers (owner 2026-10-02: dye like vanilla bundles - one
fixed colour per dye, distinguishable, vanilla friendly, true to the own textures). The colour of each dye
is read from vanilla's own <colour>_bundle item sprite: the leather tone at the 80th brightness percentile,
i.e. what the leather of a vanilla bundle of that colour looks like in its lit part. The mod's *_dyed layers
are greyscale and get multiplied with it (item model tint source minecraft:dye), so a red reinforced bundle
reads as red as a vanilla red bundle. Prints the palette as Java/JSON and optionally writes a preview sheet."""
from PIL import Image, ImageDraw
import os
import sys

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
T = os.path.join(ROOT, 'src/main/resources/assets/simplebuilding/textures/item/')
COLOURS = ['white', 'light_gray', 'gray', 'black', 'brown', 'red', 'orange', 'yellow', 'lime', 'green', 'cyan',
           'light_blue', 'blue', 'purple', 'magenta', 'pink']


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def sat(p):
    return max(p) - min(p)


def palette():
    base = Image.open(os.path.join(V, 'item/bundle.png')).convert('RGBA')
    out = {}
    for c in COLOURS:
        im = Image.open(os.path.join(V, f'item/{c}_bundle.png')).convert('RGBA')
        # leather pixels: those that differ from the plain bundle (the string and outline stay the same)
        px = [im.getpixel((x, y))[:3] for y in range(16) for x in range(16)
              if im.getpixel((x, y))[3] > 200 and im.getpixel((x, y))[:3] != base.getpixel((x, y))[:3]]
        px.sort(key=lum)
        pick = px[int(0.8 * (len(px) - 1))]
        out[c] = pick
    return out


def tint(layer, rgb):
    out = layer.copy()
    for y in range(layer.height):
        for x in range(layer.width):
            p = layer.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), (p[0] * rgb[0] // 255, p[1] * rgb[1] // 255, p[2] * rgb[2] // 255, p[3]))
    return out


def brightest(layer):
    vals = [lum(layer.getpixel((x, y))) for y in range(layer.height) for x in range(layer.width) if layer.getpixel((x, y))[3]]
    return max(vals) if vals else 1


def neutral(layer, top=None):
    """The dyed layer as neutral grey, its brightest pixel lifted to 245 and the shading kept (gamma 0.8):
    the old layers carried the tier's brown or dark tone, which made every dye muddy and white unreadable.
    `top` = brightest value of the whole group (all faces of one backpack), so the faces stay consistent."""
    top = top or brightest(layer)
    out = layer.copy()
    for y in range(layer.height):
        for x in range(layer.width):
            p = layer.getpixel((x, y))
            if p[3]:
                v = round(245 * (lum(p) / top) ** 0.8)
                out.putpixel((x, y), (v, v, v, p[3]))
    return out


def item(name, rgb):
    if rgb is None:
        return Image.open(T + name + '.png').convert('RGBA')
    im = tint(neutral(Image.open(T + name + '_dyed.png').convert('RGBA')), rgb)
    im.alpha_composite(Image.open(T + name + '_dyed_overlay.png').convert('RGBA'))
    return im


def neutral_layers(src_root):
    """{relative path: neutral image} for every *_dyed.png of the worn backpack (entity/backpack, BackpackLayer).
    entity/backpack was missing until 2026-10-14 (oc-missingtex): the worn dyed backpack kept the tier's
    brown/violet tone, so every dye came out muddy there and differed from the item and the placed block.
    item/ (N115) and block/ (N116) are not written: their *_dyed layers belong to backpacks, bundles and quivers and
    to the placed bundle/backpack blocks, which 26.3 registers in Simple Storage with its own neutral layers;
    SimpleBuilding's copies are not shipped on 26.3 (tools/release/dead_assets_263.py lists them in
    mc26_3/not-shipped-on-26.3.txt). The block/ layers had gone stale too: their 26.2 source was reshaded on
    2026-10-09 (rounder placed bundles), a week after the overlay was generated, and nothing checked it."""
    import re
    out = {}
    for sub in ('entity/backpack',):
        names = sorted(n for n in os.listdir(os.path.join(src_root, sub)) if n.endswith('_dyed.png'))
        group = lambda n: re.sub(r'_(front|back|side|top|open_back|open_front)(?=_dyed)', '', n)
        tops = {}
        for n in names:
            im = Image.open(os.path.join(src_root, sub, n)).convert('RGBA')
            tops[group(n)] = max(tops.get(group(n), 0), brightest(im))
        for n in names:
            out[f'{sub}/{n}'] = neutral(Image.open(os.path.join(src_root, sub, n)).convert('RGBA'), tops[group(n)])
    return out


def same_pixels(im, path):
    if not os.path.isfile(path):
        return False
    old = Image.open(path).convert('RGBA')
    return old.size == im.size and old.tobytes() == im.tobytes()


def write_neutral_layers(src_root, out_root):
    """Writes the neutral layers into the 26.3 overlay (same relative path). A file whose pixels already match is
    left alone (N116): the committed PNGs may be encoded differently (other zlib/optimizer), only pixels count."""
    count = 0
    for rel, im in neutral_layers(src_root).items():
        path = os.path.join(out_root, rel)
        if same_pixels(im, path):
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        im.save(path)
        count += 1
    return count


def check(src_root, out_root):
    """Stale overlay layers (N116, gradle checkStorageDyeLayers): every generated layer must exist in the overlay with
    exactly these pixels (PNG encoding does not count)."""
    return [rel for rel, im in neutral_layers(src_root).items() if not same_pixels(im, os.path.join(out_root, rel))]


if __name__ == '__main__':
    SRC = os.path.join(ROOT, 'src/main/resources/assets/simplebuilding/textures')
    OVERLAY = os.path.join(ROOT, 'mc26_3/overlay/resources/assets/simplebuilding/textures')
    if '--check' in sys.argv:
        stale = check(SRC, OVERLAY)
        for rel in stale:
            print('stale 26.3 overlay layer:', rel)
        print(f'STORAGE_DYE_LAYERS FAIL: {len(stale)} stale; run python3 tools/textures/storage_dye_palette.py --write-only'
              if stale else f'STORAGE_DYE_LAYERS_OK: {len(neutral_layers(SRC))} neutral 26.3 overlay layers up to date')
        sys.exit(1 if stale else 0)
    if '--write-only' in sys.argv:  # no Vanilla textures needed: python3 tools/textures/storage_dye_palette.py --write-only
        n = write_neutral_layers(SRC, OVERLAY)
        print(n, 'neutral dyed layers written (changed pixels only)')
        sys.exit(0)
    if len(sys.argv) > 3 and sys.argv[3] == '--write':
        n = write_neutral_layers(SRC, OVERLAY)
        print(n, 'neutral dyed layers written (changed pixels only)')
    pal = palette()
    for c, rgb in pal.items():
        print(f'{c:11s} 0x{rgb[0]:02X}{rgb[1]:02X}{rgb[2]:02X}  {rgb}')
    if PREVIEW:
        names = ['reinforced_bundle', 'netherite_bundle', 'enderite_bundle', 'backpack', 'reinforced_backpack',
                 'netherite_backpack', 'enderite_backpack', 'quiver', 'reinforced_quiver', 'netherite_quiver', 'enderite_quiver']
        s, cell = 3, 52
        sheet = Image.new('RGBA', (170 + (len(COLOURS) + 2) * cell, (len(names) + 1) * cell + 10), (139, 139, 139, 255))
        d = ImageDraw.Draw(sheet)
        for i, c in enumerate(COLOURS):
            d.text((170 + (i + 2) * cell, 6), c[:8], fill=(255, 255, 255))
        d.text((170 + cell, 6), 'vanilla', fill=(255, 255, 255))
        for r, n in enumerate(names):
            y = 22 + r * cell
            d.text((6, y + 18), n, fill=(255, 255, 255))
            sheet.alpha_composite(item(n, None).resize((16 * s, 16 * s), Image.NEAREST), (170, y))
            for i, c in enumerate(COLOURS):
                sheet.alpha_composite(item(n, pal[c]).resize((16 * s, 16 * s), Image.NEAREST), (170 + (i + 2) * cell, y))
        y = 22 + len(names) * cell
        d.text((6, y + 18), 'vanilla bundle', fill=(255, 255, 255))
        for i, c in enumerate(COLOURS):
            sheet.alpha_composite(Image.open(os.path.join(V, f'item/{c}_bundle.png')).convert('RGBA').resize((48, 48), Image.NEAREST), (170 + (i + 2) * cell, y))
        sheet.save(PREVIEW)
