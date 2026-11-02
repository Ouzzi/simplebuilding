"""FX-1 (b-mobfx02): 18x18 icons for the five weak Simple Mobs effects (leech, frostbite, magnetic, tracer, decay).

Own pixel art (outline, 3 tones, highlight), written to modules/simplemobs/shared/resources/assets/simplemobs/textures/mob_effect.
Run `python3 tools/textures/layers.py sync` afterwards for the .layers.json sources.
"""
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parents[2] / 'modules/simplemobs/shared/resources/assets/simplemobs/textures/mob_effect'
OUTLINE = (5, 4, 18, 255)

# '.' empty, 'o' dark, 'm' mid, 'l' light, 'h' highlight
ART = {
    'leech': (  # blood drop with a bite mark
        ['.........m........', '........mmm.......', '.......mmmm.......', '......mmmmmm......', '......mmmmmm......', '.....mmmmmmmm.....',
         '....mmmmmmmmmm....', '....mmmmmmmmmm....', '...ooommmmmmmmo...', '...mmmmmmmmmmmm...', '...mmmmmmmmmmmm...', '...mmmmmmmmmmmm...',
         '....mmmmmmmmmm....', '....mmmmmmmmmm....', '.....oommmmmo.....', '......oooooo......'][:16],
        {'o': (110, 14, 28), 'm': (176, 28, 44), 'l': (226, 70, 84), 'h': (255, 170, 170)}),
    'frostbite': (  # ice crystal / snowflake
        ['........ll........', '........ll........', '..l.....ll.....l..', '...l....ll....l...', '....l...ll...l....', '.....l..ll..l.....',
         '......l.ll.l......', '.......lllll.......', 'llllllllllllllllll', '.......lllll.......', '......l.ll.l......',
         '.....l..ll..l.....', '....l...ll...l....', '...l....ll....l...', '..l.....ll.....l..', '........ll........'],
        {'l': (170, 224, 248), 'm': (110, 180, 230), 'o': (40, 90, 160), 'h': (255, 255, 255)}),
    'magnetic': (  # horseshoe magnet, red body, silver tips
        ['..mmmmmmmmmmmm....', '.mmmmmmmmmmmmmm...', 'mmmmmmmmmmmmmmmm..', 'mmmmm......mmmmm..', 'mmmm........mmmm..',
         'mmmm........mmmm..', 'mmmm........mmmm..', 'mmmm........mmmm..', 'mmmm........mmmm..', 'mmmm........mmmm..',
         'llll........llll..', 'llll........llll..', 'hhhh........hhhh..'][:13],
        {'m': (196, 40, 48), 'l': (214, 220, 228), 'h': (255, 255, 255), 'o': (90, 14, 24)}),
    'tracer': (  # glowing arrow streak with a dot
        ['..............lll.', '.............llhl.', '............llhhl.', '...........llhhl..', '..........llhhl...',
         '.........llhhl....', '........llhhl.....', '.......llhhl......', '......llhhl.......', '.....llhhl........',
         '....llhhl.........', '...llhhl..........', '..llhhl...........', '.mmhhl............', '.mhhl.............'],
        {'l': (242, 217, 90), 'h': (255, 250, 200), 'm': (200, 150, 30), 'o': (120, 80, 10)}),
    'decay': (  # rotting leaf / sickly blob with holes
        ['......mmmm........', '....mmmmmmmm......', '...mmmmmmmmmm.....', '..mmmo.mmmmmmm....', '..mmmmmmmmoommm...',
         '.mmmmmmmmmmmmmmm..', '.mmoommmmmmmmmmm..', '.mmoommmmlmmmmm...', '.mmmmmmmmmmmoomm..', '..mmmmmmmmmmoomm..',
         '..mmmoommmmmmmm...', '...mmmoommmmmmm...', '....mmmmmmmmmm....', '.....mmmmmmm......', '......mmmm........'],
        {'m': (110, 130, 46), 'l': (160, 180, 70), 'o': (60, 66, 24), 'h': (200, 210, 120)}),
}


def render(rows, pal):
    w = h = 18
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    ox, oy = (w - max(len(r) for r in rows)) // 2, (h - len(rows)) // 2
    mask = set()
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != '.':
                mask.add((x + ox, y + oy))
                img.putpixel((x + ox, y + oy), pal[c] + (255,) if c in pal else pal['m'] + (255,))
    # outline in the dark of the effect (only where a neighbour is filled)
    for (x, y) in list(mask):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, y + dy)
            if p not in mask and 0 <= p[0] < w and 0 <= p[1] < h:
                img.putpixel(p, OUTLINE)
    return img


if __name__ == '__main__':
    for name, (rows, pal) in ART.items():
        render(rows, pal).save(OUT / f'{name}.png')
        print('wrote', name)
