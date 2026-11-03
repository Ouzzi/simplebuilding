"""vanilla_derived hook (b-arrows02, 03.11.): the Vanilla-derived textures of simplearrows.

Silver Arrow: unchanged from Simple Ores (ores_2026_10_30.arrow_images, variant A, approved ORES32-PFEIL). Light Arrow: the
Vanilla arrow with its flint head on a ten-tone glowstone ramp (A8b: Vanilla sprite, soft tones, light top right) and one
pale hot pixel at the very tip, item sprite and entity sheet alike.
"""
from ores_2026_10_30 import ARROW_VARIANT, Jar, RAMPS, arrow_images, hexrgb, recolor

# Glowstone tones, brightest first (Vanilla glowstone dust: pale yellow to amber-brown).
RAMPS['glow'] = ['#fffbe2', '#fff2aa', '#ffe47c', '#f6cf5a', '#e9b546', '#d69a39', '#bb7e2e', '#9a6226', '#77481d', '#523113']


def light_arrow_images(j: Jar) -> dict:
    item = j.img('item/arrow')
    ent = j.img('entity/projectiles/arrow')

    def grey(p):
        return abs(p[0] - p[1]) < 8 and abs(p[1] - p[2]) < 8

    item_out = recolor(item, 'glow', lambda x, y, p: x >= 9 and y <= 5 and grey(p), shift=-0.12)
    ent_out = recolor(ent, 'glow', lambda x, y, p: 12 <= x <= 20 and y <= 4 and grey(p), shift=-0.12)
    tip = item_out.getpixel((13, 2))
    if tip[3]:
        item_out.putpixel((13, 2), hexrgb(RAMPS['glow'][0]) + (255,))
    return {'item/light_arrow.png': item_out, 'entity/projectiles/light_arrow.png': ent_out}


def derived(jar) -> dict:
    """assets/simplearrows/textures/<key>: path -> image. Order as MODULE_OUTPUTS['simplearrows']."""
    j = jar if isinstance(jar, Jar) else Jar(jar)
    out = dict(arrow_images(j, ARROW_VARIANT))
    out.update(light_arrow_images(j))
    return out
