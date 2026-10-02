"""Usage: python tools/textures/round3_settled_2026_10_02.py <vanilla textures dir>

Writes the textures the owner settled in round 3 (2026-10-02) into the 26.3 overlay:
guide books (round-2 B with a bookmark ribbon at the side), the sage orb (the vanilla orb, animated with the colour
pulse of vanilla's ExperienceOrbRenderer), the Velocity Gauge (round-3 C: round like the clock, dial above, gold body
with an amethyst below; 17 needle frames over the upper half), the Deepslate Sage Ore (round-3 A) and the new Iron
Rod. The drawing lives in proposals_v3_2026_10_02.py, so preview and game use the same pixels."""
import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposals_v3_2026_10_02 as v3  # noqa: E402

if len(sys.argv) > 1:
    v3.V = sys.argv[1]
T = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures')

GAUGE_FRAMES = 17          # velocity_gauge_needle_00..16, as before (the item model picks the frame)
GAUGE_PIVOT = (7, 8)       # on the gold band under the dial
GAUGE_SWEEP = (170, 10)    # frame 0 = rest (left), 16 = full speed (right), over the top
GAUGE_LENGTH = 5
NEEDLE_BODY = (40, 34, 30)
NEEDLE_TIP = (200, 30, 30)
HUB = (117, 40, 2)
ORB_FRAMES = 12            # vanilla pulse: sin(age / 2) -> one cycle in 4 pi ~ 12.6 ticks


def save(image, path):
    target = os.path.join(T, path + '.png')
    os.makedirs(os.path.dirname(target), exist_ok=True)
    image.save(target)


def gauge_needle(frame):
    a = math.radians(GAUGE_SWEEP[0] + (GAUGE_SWEEP[1] - GAUGE_SWEEP[0]) * frame / (GAUGE_FRAMES - 1))
    im = v3.blank()
    cx, cy = GAUGE_PIVOT
    for r in range(1, GAUGE_LENGTH + 1):
        v3.px(im, round(cx + math.cos(a) * r), round(cy - math.sin(a) * r), NEEDLE_TIP if r == GAUGE_LENGTH else NEEDLE_BODY)
    v3.px(im, cx, cy, HUB)
    return im


def orb_strip():
    """Vanilla orb (icon 4) as an item, tinted per frame like ExperienceOrbRenderer: r = (sin+1)/2, g = 1,
    b = (sin(+4pi/3)+1)/10; 12 frames of one tick, interpolated."""
    sheet = v3.Image.open(os.path.join(v3.V, 'entity/experience/experience_orb.png')).convert('RGBA')
    base = sheet.crop((0, 16, 16, 32))
    strip = v3.Image.new('RGBA', (16, 16 * ORB_FRAMES), (0, 0, 0, 0))
    for f in range(ORB_FRAMES):
        rr = f / ORB_FRAMES * 4 * math.pi / 2
        r, b = (math.sin(rr) + 1) * 0.5, (math.sin(rr + math.pi * 4 / 3) + 1) * 0.1
        for y in range(16):
            for x in range(16):
                p = base.getpixel((x, y))
                if p[3]:
                    strip.putpixel((x, f * 16 + y), (round(p[0] * r), p[1], round(p[2] * b), p[3]))
    return strip


def main():
    mod, vanilla = v3.guide_books()
    save(mod, 'item/guide_book')
    save(vanilla, 'item/guide_book_vanilla_start')
    save(orb_strip(), 'item/sage_orb')
    with open(os.path.join(T, 'item', 'sage_orb.png.mcmeta'), 'w', encoding='utf-8') as f:
        json.dump({'animation': {'frametime': 1, 'interpolate': True}}, f, indent=2)
        f.write('\n')
    dial, _ = v3.gauge_c()
    save(dial, 'item/velocity_gauge_dial')
    for frame in range(GAUGE_FRAMES):
        save(gauge_needle(frame), f'item/velocity_gauge_needle_{frame:02d}')
    save(v3.over(dial, gauge_needle(0)), 'item/velocity_gauge')
    save(v3.sage_ore_a(), 'block/deepslate_sage_ore')
    save(v3.iron_rod_textures(), 'block/iron_rod')
    print('ok')


if __name__ == '__main__':
    main()
