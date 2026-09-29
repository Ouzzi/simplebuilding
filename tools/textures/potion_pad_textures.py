"""Trank-Pad I-III (Besitzer 2026-09-28) fuer generate_textures.py.

Trank-Pads: der Besitzer wollte die alten Flypad-Bilder (block/flypad.png, reinforced_flypad.png,
stellar_flypad.png - seit den Enderit-Flypads unbenutzt und dafuer aufgehoben) in der Netherit-Palette
wiederverwenden. Die grauen Steinpixel gehen nach ihrer Helligkeit auf die Netherit-Rampe der anderen
Netherit-Pads (NETHERITE_TWEAK_PAL), die blauen Adern auf eine Glut-Rampe je Stufe (I Lohen-Orange,
II Enderit-Violett, III Gold wie das stellare Flypad), die hellen Funkelsterne des stellaren Bildes
bleiben hell. Kein Trank-Symbol auf dem Pad (Besitzer 2026-09-28: nur das Pad zeigen; frueher stand
eine kleine Trankflasche in der Mitte); die Farbe des gespeicherten Tranks zeigen im Spiel die Partikel.

Abklingzeit (Besitzer 2026-09-28): je Stufe ein Animationsstreifen <id>_cooling.png (COOLING_FRAMES
Bilder, .mcmeta ueber POTION_PAD_ANIMATIONS): die Adern verlieren ihre Glut und pulsieren langsam zwischen
erkaltet und halb gluehend - das Pad laedt nach.

Mob-Koepfe (Lohenkopf, Endermankopf): keine eigenen Texturen mehr (Besitzer 2026-09-29) - die Koepfe
zeigen per Resource-Location die echten Vanilla-Mob-Texturen (entity/blaze/blaze.png,
entity/enderman/enderman.png + enderman_eyes.png), siehe ModSkullModels.java.
"""
import colorsys
import math
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
OLD_FLYPADS = os.path.join(REPO, "src", "main", "resources", "assets", "simplebuilding", "textures", "block")


def _hex(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# Netherit-Rampe (dunkel -> hell) wie NETHERITE_TWEAK_PAL in generate_textures.py
NETHERITE_RAMP = [_hex(c) for c in ("#161213", "#2c2627", "#3b3536", "#433d3f", "#4a4547", "#524d50",
                                    "#5a565a", "#625e62", "#7a7579", "#8e898d")]

# Stufen: Quelle (altes Flypad), Adern-Rampe dunkel -> hell
STAR = "#f7e2a0"  # Funkelsterne des stellaren Bildes: warmes Weissgold

TIERS = {
    "potion_pad": {
        "source": "flypad.png",
        "veins": ["#2e1a14", "#44231a", "#5e2e1c", "#7a3c20", "#9a4c24"],
    },
    "reinforced_potion_pad": {
        "source": "reinforced_flypad.png",
        "veins": ["#241a30", "#31213f", "#402a55", "#53366e", "#6a4690"],
    },
    "infused_potion_pad": {
        "source": "stellar_flypad.png",
        "veins": ["#2e2618", "#43361e", "#5c4a24", "#7a632c", "#9c8036"],
    },
}

def _luma(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def _ramp(ramp, t):
    t = max(0.0, min(1.0, t))
    return ramp[min(len(ramp) - 1, int(t * len(ramp)))]


def _recolour(src, veins):
    """Steinpixel -> Netherit-Rampe, blaue Adern -> Glut-Rampe, sehr helle Sternpixel bleiben hell."""
    out = Image.new("RGBA", src.size)
    vein_ramp = [c if isinstance(c, tuple) else _hex(c) for c in veins]
    for y in range(src.height):
        for x in range(src.width):
            p = src.getpixel((x, y))
            if p[3] == 0:
                out.putpixel((x, y), p)
                continue
            h, s, v = colorsys.rgb_to_hsv(p[0] / 255, p[1] / 255, p[2] / 255)
            lum = _luma(p) / 255
            if v > 0.62 and s < 0.25:
                # Funkelstern (stellar): hell, leicht in die Adernfarbe getoent
                out.putpixel((x, y), tuple(int(0.35 * 235 + 0.65 * c) for c in _hex(STAR)[:3]) + (255,))
            elif s > 0.22 and 0.45 < h < 0.70:
                # blaue Ader
                out.putpixel((x, y), _ramp(vein_ramp, (lum - 0.16) / 0.24))
            else:
                out.putpixel((x, y), _ramp(NETHERITE_RAMP, (lum - 0.12) / 0.26))
    return out


# ---------------------------------------------------------------------------------------------
# Abklingzeit: animierter Streifen je Stufe
# ---------------------------------------------------------------------------------------------
COOLING_FRAMES = 12
POTION_PAD_ANIMATIONS = {f"block/{name}_cooling.png": {"frametime": 8, "interpolate": False} for name in TIERS}


def _blend(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def _cooled_veins(veins, glow):
    """Adern-Rampe ohne Glut: Richtung dunkles Netherit gezogen; glow 0 = kalt, 1 = halbe Glut."""
    cold = NETHERITE_RAMP[1]
    return [_blend(_blend(_hex(c), cold, 0.62), _hex(c), 0.5 * glow) for c in veins]


def cooling_strip(src, tier):
    """COOLING_FRAMES Bilder untereinander: die erkalteten Adern pulsieren langsam."""
    strip = Image.new("RGBA", (16, 16 * COOLING_FRAMES))
    for i in range(COOLING_FRAMES):
        glow = 0.5 - 0.5 * math.cos(2 * math.pi * i / COOLING_FRAMES)
        frame = _recolour(src, _cooled_veins(tier["veins"], glow))
        strip.paste(frame, (0, 16 * i))
    return strip


def potion_pad_textures():
    tex = {}
    for name, tier in TIERS.items():
        src = Image.open(os.path.join(OLD_FLYPADS, tier["source"])).convert("RGBA")
        tex[f"block/{name}.png"] = _recolour(src, tier["veins"])
        tex[f"block/{name}_cooling.png"] = cooling_strip(src, tier)
    return tex
