"""Usage: python tools/textures/foods_settled_2026_10_02.py <vanilla textures dir> [preview png]

Owner 2026-10-02 settled the netherite/enderite foods from food_proposals_2026_10_02.py:
- Netherite Apple / Netherite Carrot = proposal A (vanilla golden apple / golden carrot, the whole sprite on the
  vanilla netherite-ingot ramp).
- Enderite Apple / Enderite Carrot = that netherite sprite recoloured onto the enderite-ingot ramp (brightness rank),
  written as the hand template tools/textures/hand/enderite_*.png; generate_textures.py then lays the mod's enderite
  glimmer on it (ENDERITE_GLIMMER: cores g with their glow, v) like on every enderite item. Static, like the
  other glimmered enderite items (no animation, so no mcmeta).
The enchanted apples use the same textures (their item models point at these), so they follow.
Writes into src/main/resources (the shared texture place; the 26.3 overlay has no own copies of these four; the
1.21.11 copies wait for the port run) and saves a labelled before/after preview."""
import os
import subprocess
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build', 'netherit-enderit-essen-eingebaut.png')
sys.argv = [sys.argv[0], V]
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import food_proposals_2026_10_02 as food  # noqa: E402

food.V = V
T = ps.SB_ITEM
PINK, PINK_LIGHT = (199, 125, 255), (244, 210, 255)


def netherite(kind):
    return food.build('golden_' + kind, 'netherite', food.NETHERITE, 'metal')


def enderite_base(kind):
    """The netherite sprite on the enderite-ingot ramp, before the glimmer (tools/textures/hand/ template)."""
    return ps.recolor(netherite(kind), food.ENDERITE)


def glimmer_candidates(im):
    """Where the enderite glimmer fits: the brightest pixel (core g) and two middle-tone body pixels of the lower
    right, apart from each other (v and g) - the points listed in generate_textures.ENDERITE_GLIMMER."""
    pts = ps.opaque(im)
    key = lambda p: (ps.lum(im.getpixel(p)), -p[1], -p[0])
    interior = lambda p: all(0 <= p[0] + dx < 16 and 0 <= p[1] + dy < 16 and im.getpixel((p[0] + dx, p[1] + dy))[3]
                             for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    top = max((p for p in pts if interior(p)), key=key)
    mids = sorted((p for p in pts if p[1] >= 7 and p[0] + p[1] >= 14 and interior(p)
                   and abs(p[0] - top[0]) + abs(p[1] - top[1]) > 3), key=key)
    mids = mids[len(mids) * 2 // 5:len(mids) * 3 // 5]
    chosen = []
    for p in reversed(mids):
        if all(abs(p[0] - q[0]) + abs(p[1] - q[1]) > 3 for q in chosen):
            chosen.append(p)
        if len(chosen) == 2:
            break
    return [(top[0], top[1], 'g')] + [(p[0], p[1], k) for p, k in zip(chosen, ('v', 'g'))]


def main():
    names = ['netherite_apple', 'enderite_apple', 'netherite_carrot', 'enderite_carrot']
    before = {n: ps.load(os.path.join(T, n + '.png')) for n in names}
    netherite('apple').save(os.path.join(T, 'netherite_apple.png'))
    netherite('carrot').save(os.path.join(T, 'netherite_carrot.png'))
    for kind in ('apple', 'carrot'):
        base = enderite_base(kind)
        base.save(os.path.join(HERE, 'hand', f'enderite_{kind}.png'))
        print(f'enderite_{kind} glimmer points (generate_textures.ENDERITE_GLIMMER):', glimmer_candidates(base))
    # the glimmer itself is applied by generate_textures.py from the hand templates (main tree only)
    subprocess.run([sys.executable, os.path.join(HERE, 'generate_textures.py')], check=True, stdout=subprocess.DEVNULL)
    after = {n: ps.load(os.path.join(T, n + '.png')) for n in names}
    s, cell = 10, 16 * 10 + 14
    sheet = Image.new('RGBA', (140 + len(names) * cell, 2 * (cell + 20) + 30), (139, 139, 139, 255))
    d = ImageDraw.Draw(sheet)
    d.text((10, 8), 'Netherit-/Enderit-Apfel und -Karotte eingebaut (Satz A; Enderit = Umfaerbung + Enderit-Glimmern); '
                    'gilt auch fuer die verzauberten Aepfel', fill=(0, 0, 0, 255))
    for row, (label, imgs) in enumerate((('A vorher', before), ('B nachher', after))):
        y = 30 + row * (cell + 20)
        d.text((10, y + cell // 2), label, fill=(0, 0, 0, 255))
        for k, n in enumerate(names):
            x = 140 + k * cell
            d.text((x, y), n, fill=(0, 0, 0, 255))
            sheet.alpha_composite(imgs[n].resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    sheet.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
