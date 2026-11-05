"""Item texture of the Simple Mobs trumpet (TROMP-3): own pixel shape, copper tube with a gold mouthpiece, drawn from a character grid
(no Mojang pixels). Writes textures/item/trumpet.png and its layer source trumpet.layers.json.  Run: python3 tools/textures/trumpet_2026_11_05.py"""
import json
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parents[2] / 'modules/simplemobs/shared/resources/assets/simplemobs/textures/item'
PALETTE = {'o': '#3b1d12ff', 'd': '#8a3f26ff', 'm': '#c4673aff', 'l': '#e9a06dff', 'h': '#f7d3a5ff', 'g': '#d8a82aff', 'G': '#f3df7aff'}
GRID = [
    "................",
    "................",
    "............oooo",
    "...........odmlo",
    "..ooooooooodmlho",
    ".oGgoddmmmmmmmdo",
    ".oggommllllllldo",
    "..ooodddddddddo.",
    "....o.o.........",
    "....odo.........",
    "....odo.oooooo..",
    "....ommooddddmo.",
    "....oddmmmmmlmo.",
    ".....oodddddmoo.",
    ".......oooooo...",
    "................",
]
GRID = [r.ljust(16, '.')[:16] for r in GRID]
keys = list(PALETTE)
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
rows = []
for y, row in enumerate(GRID):
    cells = []
    for x, c in enumerate(row):
        if c == '.':
            cells.append('..')
            continue
        h = PALETTE[c]
        img.putpixel((x, y), tuple(int(h[i:i + 2], 16) for i in (1, 3, 5, 7)))
        cells.append('%02d' % keys.index(c))
    rows.append(''.join(cells))
layers = {'format': 'sb-layers', 'version': 1, 'width': 16, 'height': 16, 'digits': 2, 'palette': [PALETTE[k] for k in keys],
          'layers': [{'name': 'Grundform', 'visible': True, 'opacity': 100, 'blend': 'normal', 'rows': rows}]}
img.save(OUT / 'trumpet.png')
(OUT / 'trumpet.layers.json').write_text(json.dumps(layers, indent=2) + '\n', encoding='utf-8')
img.resize((128, 128), Image.NEAREST).save('/tmp/claude-0/trp/trumpet_preview.png')
