"""N100 (owner 18.11.2026): the pressed Netherite Pressure Plate only lights up like the diamond one (no orange glow).

Vanilla-derived (the Netherite block's own pixels), so built, never committed (tools/textures/vanilla_derived.py):
    derived(jar)                 -> {'block/netherite_pressure_plate_active.png': image}  (simplebuilding)
    derived_module(jar, module)  -> the same for simplelib
Method = the diamond/emerald plates (N48c): the resting plate is the Netherite block; pressed, three rings around the
centre are blended towards white (RINGS, the same ring shape as the pressed diamond plate), weaker steps than diamond's
0.2/0.4/0.6 because Netherite is dark (rounds: 0.2/0.4/0.6 too pale-white, 0.12/0.24/0.36 a grey disc, 0.07/0.14/0.21 kept; see /root/sb-tools-b/n100/PLAN-N100.md).

Preview: python3.12 n100_netherite_plate_2026_11_18.py <client.jar> <out.png>
"""
import io
import sys
import zipfile

KEY = 'block/netherite_pressure_plate_active.png'
SRC = 'assets/minecraft/textures/block/netherite_block.png'
# Ring level per pixel (0 = unchanged ... 3 = centre), the ring shape of the pressed diamond plate.
RINGS = ['0000000000000000',
         '0000000000000000',
         '0000011111100000',
         '0000111111110000',
         '0001112222111000',
         '0011122222211100',
         '0011223333221100',
         '0011223333221100',
         '0011223333221100',
         '0011223333221100',
         '0011122222211100',
         '0001112222111000',
         '0000111111110000',
         '0000011111100000',
         '0000000000000000',
         '0000000000000000']
# Blend towards white per ring level.
STEPS = (0.0, 0.07, 0.14, 0.21)


def active(block):
    """The pressed plate from the Netherite block image (16x16 RGBA)."""
    im = block.convert('RGBA').copy()
    for y in range(16):
        for x in range(16):
            f = STEPS[int(RINGS[y][x])]
            r, g, b, a = im.getpixel((x, y))
            im.putpixel((x, y), (round(r + (255 - r) * f), round(g + (255 - g) * f), round(b + (255 - b) * f), a))
    return im


def _draw(jar):
    from PIL import Image
    with zipfile.ZipFile(jar) as z:
        return {KEY: active(Image.open(io.BytesIO(z.read(SRC))))}


def derived(jar) -> dict:
    return _draw(jar)


def derived_module(jar, module: str) -> dict:
    return _draw(jar) if module == 'simplelib' else {}


if __name__ == '__main__':
    _draw(sys.argv[1])[KEY].save(sys.argv[2])
