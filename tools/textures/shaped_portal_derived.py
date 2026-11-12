"""Shaped nether portal (Simple Dimensions, N33): Vanilla's animated nether portal 20 % darker.

The pixels come from the Mojang texture block/nether_portal.png, so the result is Vanilla-derived: never committed,
built from the local 26.3 client jar (tools/textures/vanilla_derived.py, MODULE_OUTPUTS['simpledimensions']).
Owner rule (concept PORTAL33, 12.11.2026): a portal Vanilla would not allow glows 20 % darker; the alpha channel and
the 32 animation frames stay as they are (the .png.mcmeta next to it is the module's own file).
"""
import io
from zipfile import ZipFile

from PIL import Image

FACTOR = 0.8
SOURCE = 'assets/minecraft/textures/block/nether_portal.png'


def darken(image: Image.Image, factor: float = FACTOR) -> Image.Image:
    """RGB times factor (rounded), alpha unchanged."""
    rgba = image.convert('RGBA')
    r, g, b, a = rgba.split()
    scale = [min(255, int(round(v * factor))) for v in range(256)]
    return Image.merge('RGBA', (r.point(scale), g.point(scale), b.point(scale), a))


def derived(jar):
    """Vanilla-derived output for tools/textures/vanilla_derived.py: texture path -> image."""
    with ZipFile(jar) as z:
        portal = Image.open(io.BytesIO(z.read(SOURCE)))
        portal.load()
    return {'block/shaped_nether_portal.png': darken(portal)}
