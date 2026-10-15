"""Strong Beacon background: Vanilla's beacon GUI stretched by two effect rows (levels 5 and 6).

The pixels come from the Mojang texture gui/container/beacon.png, so the result is Vanilla-derived: never committed,
built from the local 26.3 client jar (tools/textures/vanilla_derived.py, MODULE_OUTPUTS['simplebeacons']).

Layout (relative to the image origin; the screen shifts everything below the fields by EXTRA):
  rows 0-96    Vanilla top: frame, both fields, markers 1-3 (left) and 4 (right)
  rows 97-146  EXTRA = 50 rows: the Vanilla 25-row marker period (rows 72-96) twice, i.e. markers for levels 5 and 6
  rows 147-268 Vanilla rows 97-218: field bottom, payment slot, inventory
"""
import io
from zipfile import ZipFile

from PIL import Image

EXTRA = 50
WIDTH, HEIGHT = 230, 219
SHEET = (256, 288)  # texture sheet size of the output (the screen blits with these numbers)
PERIOD_FROM, PERIOD_ROWS, SPLIT = 72, 25, 97


def stretch(vanilla: Image.Image) -> Image.Image:
    """Vanilla beacon.png (256x256) -> the extended sheet."""
    v = vanilla.convert('RGBA').crop((0, 0, WIDTH, HEIGHT))
    out = Image.new('RGBA', SHEET, (0, 0, 0, 0))
    out.paste(v.crop((0, 0, WIDTH, SPLIT)), (0, 0))
    period = v.crop((0, PERIOD_FROM, WIDTH, PERIOD_FROM + PERIOD_ROWS))
    for i in range(EXTRA // PERIOD_ROWS):
        out.paste(period, (0, SPLIT + i * PERIOD_ROWS))
    out.paste(v.crop((0, SPLIT, WIDTH, HEIGHT)), (0, SPLIT + EXTRA))
    return out


def derived(jar):
    """Vanilla-derived output for tools/textures/vanilla_derived.py: texture path -> image."""
    with ZipFile(jar) as z:
        beacon = Image.open(io.BytesIO(z.read('assets/minecraft/textures/gui/container/beacon.png')))
    return {'gui/container/strong_beacon.png': stretch(beacon)}
