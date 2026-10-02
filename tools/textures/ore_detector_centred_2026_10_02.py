"""Usage: python tools/textures/ore_detector_centred_2026_10_02.py <vanilla textures dir> [preview png] [old texture dir]
       [v2 preview png] [v2 old texture dir]

Successor of round5_settled for the Ore Detector only (owner 2026-10-02: the compass needle did not look centred).
The round-5 dial is symmetric about x = 7.5 but the needle turned about pixel 7, so it reached one pixel closer to
the left rim than to the right one and the side marks sat at 2 / 12. proposals_v5 now draws a two-pixel hub and
turns the needle about the point between columns 7 and 8 (frames 17..31 mirror 15..1). This script writes only the
detector files into the 26.3 overlay (dial, 32 needle frames, resting item), checks the mirror symmetry and saves a
labelled before/after preview.

Second pass the same day (owner): the needle is wider like the recovery compass's (4-connected stroke, two-pixel
tail), and the selection glimmer of a calibrated detector runs along the needle instead of round the slot edge. The
head pixels of every frame (hub -> tip) are written to OreDetectorNeedlePath.java for OreDetectorGlint; the preview
shows the glimmer steps on the resting needle and on a diagonal one (old texture dir = the textures before this
change, e.g. from git, for the "vorher" row)."""
import json
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build', 'erzdetektor-vorher-nachher.png')
OLD = sys.argv[3] if len(sys.argv) > 3 else None
PREVIEW_V2 = sys.argv[4] if len(sys.argv) > 4 else None    # vertical-centring round: frames 0, 8, 16, 24 large
OLD_V2 = sys.argv[5] if len(sys.argv) > 5 else None
sys.argv = [sys.argv[0], V]
import proposals_v3_2026_10_02 as v3  # noqa: E402
import proposals_v5_2026_10_02 as v5  # noqa: E402

T = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'item')
JAVA = os.path.join(HERE, '..', '..', 'common', 'src', 'shared', 'java', 'com', 'simplebuilding', 'client', 'render',
                    'OreDetectorNeedlePath.java')
# OreDetectorGlint: 150 ms per step, head + two fading tail pixels, four steps pause
TRAIL_ALPHA = (0xD0, 0x80, 0x40)
PAUSE_STEPS = 4
GLINT_RGB = (0x5D, 0xEC, 0xF5)   # diamond ore, OreDetectorItem#targetColor


def write_java():
    rows = []
    for f in range(32):
        steps, _ = v5.needle_steps(f)
        rows.append('            {' + ', '.join(f'{x}, {y}, {k}' for k, step in enumerate(steps) for x, y in step)
                    + '},  // ' + f'{f:02d}')
    src = """package com.simplebuilding.client.render;

/**
 * Kopfpixel der 32 Erzdetektor-Nadelbilder ({@code item/detector_needle_NN}), je Bild von der Nabe
 * zur Spitze als x, y, Schritt im 16x16-Feld (senkrecht ist die Nadel zwei Pixel breit: zwei Pixel je
 * Schritt). Geschrieben von {@code tools/textures/ore_detector_centred_2026_10_02.py} aus denselben
 * Zahlen wie die Texturen - nicht von Hand aendern. {@link OreDetectorGlint} laesst den
 * Auswahl-Schimmer Schritt fuer Schritt darauf laufen.
 */
public final class OreDetectorNeedlePath {
    private static final int[][] HEAD = {
ROWS
    };

    private OreDetectorNeedlePath() {
    }

    /** Anzahl der Schritte (Nabe bis Spitze) von Bild {@code frame} (0..31). */
    public static int steps(int frame) {
        int[] head = HEAD[Math.floorMod(frame, 32)];
        return head[head.length - 1] + 1;
    }

    /** Die Pixel von Schritt {@code step} (0 = an der Nabe) in Bild {@code frame}, je {x, y}. */
    public static int[][] pixels(int frame, int step) {
        int[] head = HEAD[Math.floorMod(frame, 32)];
        int n = 0;
        for (int i = 2; i < head.length; i += 3) {
            if (head[i] == step) n++;
        }
        int[][] out = new int[n][];
        n = 0;
        for (int i = 0; i < head.length; i += 3) {
            if (head[i + 2] == step) out[n++] = new int[]{head[i], head[i + 1]};
        }
        return out;
    }
}
""".replace('ROWS', chr(10).join(rows))
    with open(JAVA, 'w', encoding='utf-8', newline=chr(10)) as f:
        f.write(src)


# Resting needle (no find) pulses gently - third owner note 2026-10-02. Two frames, interpolated like vanilla's
# animated items: dim (as before, 0.6) <-> a little brighter (0.85); 2 x 16 ticks = 1.6 s per breath.
PULSE_DIM = (0.6, 0.85)
PULSE_FRAMETIME = 16


def idle_pulse(dial):
    strip = Image.new('RGBA', (16, 16 * len(PULSE_DIM)), (0, 0, 0, 0))
    for k, factor in enumerate(PULSE_DIM):
        strip.alpha_composite(v3.over(dial, dim(v5.detector_needle(16), factor)), (0, 16 * k))
    return strip


def save_pulse_previews(strip):
    """Frame strip (as Minecraft interpolates it, 8 steps per breath) and a GIF next to the preview."""
    a, b = strip.crop((0, 0, 16, 16)), strip.crop((0, 16, 16, 32))
    steps = []
    n = 8
    for i in range(n):
        t = (1 - math.cos(2 * math.pi * i / n)) / 2   # 0 -> 1 -> 0 over one breath
        steps.append(Image.blend(a, b, t))
    s = 8
    sheet = Image.new('RGBA', (len(steps) * (16 * s + 8) + 8, 16 * s + 30), (139, 139, 139, 255))
    d = ImageDraw.Draw(sheet)
    for k, im in enumerate(steps):
        x = 8 + k * (16 * s + 8)
        sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (x, 22))
        d.text((x, 6), f'{k * 200} ms', fill=(0, 0, 0, 255))
    base = os.path.join(os.path.dirname(os.path.abspath(PREVIEW)), 'erzdetektor')
    sheet.save(base + '-ruhepuls.png')
    frames = []
    for im in steps:
        bg = Image.new('RGBA', (16 * s, 16 * s), (139, 139, 139, 255))
        bg.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST))
        frames.append(bg.convert('P', palette=Image.ADAPTIVE))
    frames[0].save(base + '-ruhepuls.gif', save_all=True, append_images=frames[1:], duration=200, loop=0)


def glint_steps(frame, step):
    """The glimmer pixels {(x, y): alpha} at step `step`, same rule as OreDetectorGlint.sparks."""
    steps, _ = v5.needle_steps(frame)
    lap = len(steps) + len(TRAIL_ALPHA) + PAUSE_STEPS
    h = step % lap
    return {c: a for i, a in enumerate(TRAIL_ALPHA) if 0 <= h - i < len(steps) for c in steps[h - i]}


def with_glint(base, frame, step):
    im = base.copy()
    for (x, y), a in glint_steps(frame, step).items():
        p = im.getpixel((x, y))
        t = a / 255
        im.putpixel((x, y), tuple(round(p[i] * (1 - t) + GLINT_RGB[i] * t) for i in range(3)) + (255,))
    return im


def dim(image, factor=0.6):
    out = image.copy()
    for y in range(16):
        for x in range(16):
            p = out.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), tuple(round(c * factor) for c in p[:3]) + (p[3],))
    return out


def check_symmetry():
    for f in range(1, 16):
        a = v5.detector_needle(f)
        b = v5.detector_needle(32 - f).transpose(Image.FLIP_LEFT_RIGHT)
        assert a.tobytes() == b.tobytes(), f'frame {f} and {32 - f} are not mirror images'
    for f in range(32):
        pts = [(x, y) for y in range(16) for x in range(16) if v5.detector_needle(f).getpixel((x, y))[3]]
        assert all(2 <= x <= 13 and 4 <= y <= 10 for x, y in pts), f'frame {f} leaves the dial face'
    for f in v5.VERTICAL_FRAMES:  # straight up/down: the needle's pixels sit symmetric about the dial centre x = 7.5
        im = v5.detector_needle(f)
        cells = {(x, y) for y in range(16) for x in range(16) if im.getpixel((x, y))[3]}
        assert cells == {(15 - x, y) for x, y in cells}, f'frame {f} is not centred on x = 7.5'


def sheet(before, after, old_idle, new_idle):
    s = 6
    frames = list(range(0, 32, 2))
    cell = 16 * s + 6
    rows = 4
    im = Image.new('RGBA', (130 + len(frames) * cell, rows * (cell + 18) + 40), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((8, 6), 'Erzdetektor-Nadel: Frames 0, 2, ... 30 (0 = unten/Sued, 8 = links, 16 = oben, 24 = rechts); '
                   'rote Linie = Mitte des Ziffernblatts', fill=(0, 0, 0, 255))

    def put(row, k, label, image, centre=True):
        y = 24 + row * (cell + 18)
        x = 130 + k * cell
        im.alpha_composite(image.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
        d.text((x + 2, y), label, fill=(0, 0, 0, 255))
        if centre:
            d.line([(x + 8 * s, y + 14), (x + 8 * s, y + 14 + 16 * s)], fill=(255, 0, 0, 110))

    for row, (label, frame_of) in enumerate((('A vorher', before), ('B nachher', after))):
        d.text((8, 24 + row * (cell + 18) + 8 * s), label, fill=(0, 0, 0, 255))
        for k, f in enumerate(frames):
            put(row, k, str(f), frame_of(f))
    # glimmer: before = round the slot edge (old OreDetectorGlint, 60 edge pixels), after = along the needle
    d.text((8, 24 + 2 * (cell + 18) + 6 * s), 'C Auswahl-\nSchimmer\nvorher (Rand)', fill=(0, 0, 0, 255))
    edge = [(i, 0) for i in range(15)] + [(15, i) for i in range(15)] + [(15 - i, 15) for i in range(15)] + [(0, 15 - i) for i in range(15)]
    for k in range(8):
        step = k * 8
        img = old_idle.copy()
        for i, a in enumerate(TRAIL_ALPHA):
            x, y = edge[(step - i) % 60]
            p = img.getpixel((x, y))
            t = a / 255
            img.putpixel((x, y), tuple(round(p[j] * (1 - t) + GLINT_RGB[j] * t) for j in range(3)) + (255,))
        put(2, k, f'{step * 50} ms', img, centre=False)
    d.text((8, 24 + 3 * (cell + 18) + 6 * s), 'D Auswahl-\nSchimmer\nnachher (Nadel)', fill=(0, 0, 0, 255))
    k = 0
    for frame, base in ((16, new_idle), (4, after(4))):
        steps, _ = v5.needle_steps(frame)
        for step in range(len(steps) + 2):
            put(3, k, f'F{frame} {step * 150} ms', with_glint(base, frame, step), centre=False)
            k += 1
    return im


def sheet_v2(old_dir, dial):
    """Frames 0, 8, 16, 24 large: textures before the vertical-centring round against now, centre line in red."""
    s = 14
    cell = 16 * s + 12
    im = Image.new('RGBA', (120 + 4 * cell, 2 * (cell + 22) + 30), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((8, 6), 'Erzdetektor-Nadel senkrecht mittig: Frames 0 (6 Uhr), 8, 16 (12 Uhr), 24; rote Linie = Mitte x = 7,5',
           fill=(0, 0, 0, 255))
    old_dial = Image.open(os.path.join(old_dir, 'detector_dial.png')).convert('RGBA')
    for row, (label, frame_of) in enumerate((
            ('A vorher', lambda f: v3.over(old_dial, Image.open(os.path.join(old_dir, f'detector_needle_{f:02d}.png')).convert('RGBA'))),
            ('B nachher', lambda f: v3.over(dial, v5.detector_needle(f))))):
        y = 28 + row * (cell + 22)
        d.text((8, y + cell // 2), label, fill=(0, 0, 0, 255))
        for k, f in enumerate((0, 8, 16, 24)):
            x = 120 + k * cell
            d.text((x, y), f'Frame {f}', fill=(0, 0, 0, 255))
            im.alpha_composite(frame_of(f).resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
            d.line([(x + 8 * s, y + 14), (x + 8 * s, y + 14 + 16 * s)], fill=(255, 0, 0, 160))
    return im


def main():
    check_symmetry()
    src = OLD or T
    old = {f: Image.open(os.path.join(src, f'detector_needle_{f:02d}.png')).convert('RGBA') for f in range(32)}
    old_dial = Image.open(os.path.join(src, 'detector_dial.png')).convert('RGBA')
    old_idle = Image.open(os.path.join(src, 'detector.png')).convert('RGBA')
    dial = v5.detector_dial()
    dial.save(os.path.join(T, 'detector_dial.png'))
    for f in range(32):
        v5.detector_needle(f).save(os.path.join(T, f'detector_needle_{f:02d}.png'))
    idle = v3.over(dial, dim(v5.detector_needle(16)))
    strip = idle_pulse(dial)
    strip.save(os.path.join(T, 'detector.png'))
    with open(os.path.join(T, 'detector.png.mcmeta'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump({'animation': {'frametime': PULSE_FRAMETIME, 'interpolate': True}}, f, indent=2)
        f.write('\n')
    save_pulse_previews(strip)
    write_java()
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    sheet(lambda f: v3.over(old_dial, old[f]), lambda f: v3.over(dial, v5.detector_needle(f)), old_idle, idle).save(PREVIEW)
    if PREVIEW_V2 and OLD_V2:
        sheet_v2(OLD_V2, dial).save(PREVIEW_V2)
    print('ok')


if __name__ == '__main__':
    main()
