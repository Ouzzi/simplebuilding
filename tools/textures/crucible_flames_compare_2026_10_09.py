"""Owner preview for CrucibleFlames POINTED/BROAD/HYBRID and a new MITTEL shape.

The POINTED and BROAD paths copy the numerical model in
``modules/simplelib/.../CrucibleFlames.java`` exactly.  HYBRID deliberately
keeps a broad, low bed while adding a few separated pointed tongues: it reads
as a stable low heat source but still gives the eye unmistakable hot peaks.

MITTEL is not a mix of two tongue kinds.  Every single shape parameter is the
arithmetic mean of the POINTED and BROAD value (spacing 10/14 -> 12, half-width
base 4.5/6.5 -> 5.5 with step 0.5/0.6 -> 0.55, profile exponent 1.5/0.6 -> 1.05,
floor band 0.3/0.45 -> 0.375, tongue scale 0.75/0.5 -> 0.625, height cap
8/5 / 6/5 -> 7/5); the lean/skew stays exactly as in both other shapes.  The
result is a single, uniform mid-width tongue shape.

Owner choice 2026-10-09: MITTEL is built in (``UiFlames.Shape.MIDDLE``, ``UiFlames.SHAPE``). SHAPE_PARAMS mirrors
the Java enum ``UiFlames.Shape`` exactly (MITTEL = MIDDLE); keep both in step.

Run from the repository root:
    python3.12 tools/textures/crucible_flames_compare_2026_10_09.py [OUT_DIR]   (default previews-out/)
"""
from pathlib import Path
import math
import sys
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "previews-out"
SCALE = 4
GUI_W, GUI_H = 190, 94
BOX_X, BOX_Y, BOX_W, BOX_H = 7, 7, 176, 84
FIRE_BOTTOM = BOX_Y + BOX_H - 7
FIRE_X, FIRE_W = BOX_X + 5, BOX_W - 10
FRAMES = 24
FRAME_MS = 110
BG = (31, 34, 40)
BOX = (65, 57, 53)
BOX_EDGE = (22, 21, 24)
FIRE = [(200, 30, 18), (232, 69, 26), (247, 146, 28), (255, 200, 50),
        (255, 233, 138), (122, 24, 8), (255, 122, 30), (255, 224, 112)]
SOUL = [(26, 63, 168), (35, 112, 216), (63, 169, 245), (142, 224, 255),
        (221, 248, 255), (14, 42, 106), (63, 156, 240), (200, 244, 255)]
HEATS = (("MEDIUM", "medium", False), ("HIGH", "high", False),
         ("EXTREME", "extreme", False), ("NACHGLÜHEN", "high", True))
SHAPES = ("POINTED", "BROAD", "HYBRID", "MITTEL")
COMPARE = ("POINTED", "MITTEL", "BROAD")
SHAPE_NAMES = {"POINTED": "spitz", "MITTEL": "mittel", "BROAD": "breit"}
HEAT_NAMES = ("mittlere Hitze", "hohe Hitze", "extrem (blau)", "Nachglühen")
# The exact Java values (UiFlames.Shape); MITTEL (Java MIDDLE, built in) is the mean of every value.
SHAPE_PARAMS = {
    "POINTED": dict(spacing=10, half=4.5, half_step=0.5, exponent=1.5,
                    band=0.30, scale=0.75, cap=8),
    "BROAD": dict(spacing=14, half=6.5, half_step=0.6, exponent=0.6,
                  band=0.45, scale=0.50, cap=6),
    "MITTEL": dict(spacing=12, half=5.5, half_step=0.55, exponent=1.05,
                   band=0.375, scale=0.625, cap=7),
}

try:
    FONT = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 10 * SCALE)
except OSError:
    FONT = ImageFont.load_default()


def i32(value):
    value &= 0xFFFFFFFF
    return value - (1 << 32) if value >= 1 << 31 else value


def java_hash(a, b):
    h = i32(a * 374761393 + b * 668265263)
    h = i32((h ^ ((h & 0xFFFFFFFF) >> 13)) * 1274126177)
    return i32(h ^ ((h & 0xFFFFFFFF) >> 16))


def jround(value):
    return math.floor(value + 0.5)


def calm_of(heat, glowing):
    return 2 if glowing else 1 if heat == "medium" else 0


def target_pixels(heat, glowing, section):
    if glowing:
        return 0 if heat == "none" else max(3, jround(section / 16.0))
    return jround(section * {"none": 0.0, "medium": 1 / 6.5,
                             "high": 1 / 3, "extreme": 1.1 / 3}[heat])


def flick_ms(calm):
    return 110 if calm == 0 else 190 if calm == 1 else 280


def height(col, millis, target, calm, shape):
    if shape == "HYBRID":
        # A lower broad bed plus four sparse, pointed tongues.
        bed = max(2, flame_height(col, millis, target, calm, "BROAD") * 3 // 4)
        spacing = 20
        center = (col // spacing) * spacing + spacing // 2
        distance = abs(col - center)
        if distance <= 5:
            tongue = flame_height(col, millis, target, calm, "POINTED")
            bed = max(bed, tongue)
        return min(target * 8 // 5, bed)
    return flame_height(col, millis, target, calm, shape)


def flame_height(col, millis, target, calm, shape):
    p = SHAPE_PARAMS[shape]
    t = millis / 1000.0 * (1.0 if calm == 0 else 0.55 if calm == 1 else 0.35)
    flick = millis // flick_ms(calm)
    spacing = p["spacing"]
    best = 0.0
    first = col // spacing - 1
    for j in range(first, first + 3):
        center = j * spacing + spacing / 2 + 1.5 * math.sin(t * 1.7 + j * 2.1)
        half = p["half"] + (java_hash(j, 5) & 3) * p["half_step"]
        skew = 0.3 if (java_hash(j, 3) & 1) == 0 else -0.3
        off = col + 0.5 - center
        distance = abs(off) / (half * (1 + skew if off < 0 else 1 - skew))
        if distance >= 1:
            continue
        amp = (0.78 + 0.3 * math.sin(t * 2.3 + j * 1.7)
               + 0.14 * math.sin(t * 5.1 + j * 0.9)
               + (java_hash(j, 9) & 3) * 0.06) if calm == 0 else (
               0.8 + 0.12 * math.sin(t * 2.3 + j * 1.7)
               + (java_hash(j, 9) & 3) * 0.04)
        best = max(best, amp * (1 - distance) ** p["exponent"])
    jitter = 1 if calm == 0 and (java_hash(col, flick) & 3) == 0 else 0
    factor = p["band"] + p["scale"] * best
    limit = target * p["cap"] // 5
    return max(2, min(limit, jround(target * factor) + jitter))


def shade(k, h, edge, ember, calm):
    if k == 0:
        return ember
    if edge <= 1:
        return 0
    if calm == 2:
        return 1
    if calm == 1:
        return 3 if k <= 1 else 1 if edge == 2 else 2
    if k <= 2:
        return 3
    if edge == 2:
        return 1
    return 3 if k < h * 35 // 100 and edge >= 4 else 2


def draw_flames(draw, x, bottom, width, section, heat, glowing, millis, shape):
    target = target_pixels(heat, glowing, section)
    if target <= 0:
        return
    calm = calm_of(heat, glowing)
    colors = SOUL if heat == "extreme" else FIRE
    flick = millis // flick_ms(calm)
    heights = [height(i - 3, millis, target, calm, shape) for i in range(width + 6)]
    for col in range(width):
        h = heights[col + 3]
        ember = 6 if (java_hash(col, flick // 3) & 3) == 0 else 5
        for k in range(h):
            edge = (h - k + 1) // 2
            for distance in range(1, 4):
                if distance >= edge:
                    break
                if heights[col + 3 - distance] <= k or heights[col + 3 + distance] <= k:
                    edge = distance
                    break
            draw.point((x + col, bottom - k - 1), fill=colors[shade(k, h, edge, ember, calm)])
    sparks = max(2, width // 14) if calm == 0 else max(1, width // (40 if calm == 1 else 70))
    for s in range(sparks):
        clock = flick + (java_hash(s, 13) & 63)
        age, cycle = clock % 9, clock // 9
        if age > (6 if calm == 0 else 3):
            continue
        col = (java_hash(s, cycle) % width + width) % width
        sway = jround(math.sin((age + s) * 1.3))
        sx = max(0, min(width - 1, col + sway))
        sy = bottom - heights[col + 3] - 2 - age * 2
        draw.point((x + sx, sy), fill=colors[7] if age < 3 and calm != 2 else colors[1])


def panel(shape, heat, glowing, millis):
    image = Image.new("RGB", (GUI_W, GUI_H), BG)
    draw = ImageDraw.Draw(image)
    draw.rectangle((BOX_X, BOX_Y, BOX_X + BOX_W - 1, BOX_Y + BOX_H - 1), fill=BOX, outline=BOX_EDGE, width=2)
    draw.rectangle((BOX_X + 3, BOX_Y + 3, BOX_X + BOX_W - 4, BOX_Y + 4), fill=(112, 92, 72))
    draw_flames(draw, FIRE_X, FIRE_BOTTOM, FIRE_W, BOX_H, heat, glowing, millis, shape)
    return image.resize((GUI_W * SCALE, GUI_H * SCALE), Image.Resampling.NEAREST)


def make_gif(shape):
    frames = []
    for frame in range(FRAMES):
        sheet = Image.new("RGB", (GUI_W * SCALE, GUI_H * SCALE * 4), BG)
        for row, (_, heat, glowing) in enumerate(HEATS):
            sheet.paste(panel(shape, heat, glowing, frame * FRAME_MS),
                        (0, row * GUI_H * SCALE))
        frames.append(sheet)
    path = OUT / f"flammen-{shape.lower()}.gif"
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=FRAME_MS,
                   loop=0, optimize=False)
    return path


def comparison_gif():
    frames = []
    legacy = ("POINTED", "BROAD", "HYBRID")
    for frame in range(FRAMES):
        sheet = Image.new("RGB", (GUI_W * SCALE * 3, GUI_H * SCALE * 4), BG)
        for col, shape in enumerate(legacy):
            for row, (_, heat, glowing) in enumerate(HEATS):
                sheet.paste(panel(shape, heat, glowing, frame * FRAME_MS),
                            (col * GUI_W * SCALE, row * GUI_H * SCALE))
        frames.append(sheet)
    path = OUT / "flammen-vergleich.gif"
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=FRAME_MS,
                   loop=0, optimize=False)
    return path


def draw_label(draw, cx, cy, text, fill):
    bbox = FONT.getbbox(text)
    draw.text((cx - (bbox[2] - bbox[0]) / 2 - bbox[0],
               cy - (bbox[3] - bbox[1]) / 2 - bbox[1]), text, font=FONT, fill=fill)


def comparison_gif_v2():
    """The new comparison: columns spread | middle | broad, rows = heat levels, labelled."""
    probe = ImageDraw.Draw(Image.new("RGB", (8, 8)))
    lbl = int(max(probe.textlength(t, font=FONT) for t in HEAT_NAMES)) + 48
    hdr = FONT.getbbox("Ag")[3] - FONT.getbbox("Ag")[1] + 44
    pw, ph = GUI_W * SCALE, GUI_H * SCALE
    width, height = lbl + len(COMPARE) * pw, hdr + len(HEATS) * ph
    frames = []
    for frame in range(FRAMES):
        sheet = Image.new("RGB", (width, height), BG)
        draw = ImageDraw.Draw(sheet)
        draw.rectangle((0, 0, width - 1, hdr - 1), fill=(24, 26, 31))
        draw.rectangle((0, 0, lbl - 1, height - 1), fill=(24, 26, 31))
        for col, shape in enumerate(COMPARE):
            draw_label(draw, lbl + col * pw + pw // 2, hdr // 2,
                       SHAPE_NAMES[shape], (255, 214, 96))
        for row, name in enumerate(HEAT_NAMES):
            draw_label(draw, lbl // 2, hdr + row * ph + ph // 2, name, (214, 220, 232))
        for col, shape in enumerate(COMPARE):
            for row, (_, heat, glowing) in enumerate(HEATS):
                sheet.paste(panel(shape, heat, glowing, frame * FRAME_MS),
                            (lbl + col * pw, hdr + row * ph))
        frames.append(sheet)
    path = OUT / "flammen-vergleich-v2.gif"
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=FRAME_MS,
                   loop=0, optimize=False)
    return path


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    paths = [make_gif(shape) for shape in SHAPES] + [comparison_gif(), comparison_gif_v2()]
    for path in paths:
        print(f"preview {path} frames={FRAMES} size={Image.open(path).size}")


if __name__ == "__main__":
    main()
