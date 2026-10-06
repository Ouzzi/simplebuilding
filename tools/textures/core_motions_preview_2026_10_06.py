"""Preview GIF of the building core hand motions (owner addition 11, CoreHandMotion.java).

Usage (repository root, Pillow): python tools/textures/core_motions_preview_2026_10_06.py
Writes C:/Users/o_o/code/minecraft-mods/previews/kern-animationen-vorschau.gif: one panel per motion, the diamond
core moved by the same pose math as CoreHandMotion#pose (ported 1:1 below), seen roughly like the first-person
hand (x right, y up, -z away from the camera; rotations about y/x shown as squash, about z as a turn).
"""
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
CORE = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures/item/diamond_core.png'
OUT = Path('C:/Users/o_o/code/minecraft-mods/previews/kern-animationen-vorschau.gif')
MOTIONS = [('PULSE', 45, 14), ('SPIN', 30, 16), ('RISE', 15, 24), ('BOOMERANG', 10, 26), ('FORGE', 0, 40)]
LABEL = {'PULSE': 'Pulsieren 45 %', 'SPIN': 'Drehen 30 %', 'RISE': 'Aufsteigen 15 %', 'BOOMERANG': 'Bumerang 10 %',
         'FORGE': 'Erz (Schmieden)'}


def smooth(x):
    x = max(0.0, min(1.0, x))
    return x * x * (3 - 2 * x)


def ease_in_out(x):
    return 4 * x ** 3 if x < 0.5 else 1 - (-2 * x + 2) ** 3 / 2


def pose(motion, t):
    """[tx, ty, tz, rx, ry, rz, scale] exactly as CoreHandMotion.pose."""
    p = [0, 0, 0, 0, 0, 0, 1]
    bell = math.sin(math.pi * t)
    if motion == 'PULSE':
        beat = math.sin(2 * math.pi * t)
        p[6] = 1 + 0.3 * beat * beat * (1 - 0.4 * t)
        p[1] = 0.03 * bell
    elif motion == 'SPIN':
        p[4] = 360 * ease_in_out(t)
        p[1] = 0.08 * bell
    elif motion == 'RISE':
        hover = smooth(t / 0.35) if t < 0.35 else 1 if t < 0.7 else 1 - smooth((t - 0.7) / 0.3)
        bob = 0.025 * math.sin(2 * math.pi * (t - 0.35) / 0.35) if 0.35 <= t < 0.7 else 0
        p[1] = 0.42 * hover + bob
        p[2] = -0.12 * hover
        p[4] = 360 * smooth(t)
        p[6] = 1 + 0.1 * hover
    elif motion == 'BOOMERANG':
        p[2] = -1.3 * bell
        p[0] = -0.35 * math.sin(2 * math.pi * t)
        p[1] = 0.12 * bell
        p[3] = -60 * bell
        p[5] = 720 * smooth(t)
    else:
        if t < 0.3:
            up = smooth(t / 0.3)
            p[1], p[2] = 0.35 * up, -0.1 * up
        elif t < 0.75:
            k = (t - 0.3) / 0.45
            p[1] = 0.35 + 0.02 * math.sin(6 * math.pi * k)
            p[2] = -0.1
            p[4] = 1080 * k * k
            p[6] = 1 + 0.25 * smooth(k)
        elif t < 0.85:
            k = (t - 0.75) / 0.1
            fall = k * k
            p[1] = 0.35 - 0.47 * fall
            p[2] = -0.1 * (1 - fall)
            p[3] = -30 * fall
            p[6] = 1.25 - 0.1 * fall
        else:
            k = (t - 0.85) / 0.15
            spring = math.cos(3 * math.pi * k) * (1 - k)
            p[1] = -0.12 * spring * (1 - k)
            p[3] = -30 * (1 - smooth(k))
            p[6] = 1 + 0.15 * (1 - smooth(k))
    return p


def panel(core, motion, ticks, frame_tick, size=200):
    img = Image.new('RGB', (size, size + 26), '#2b3038')
    d = ImageDraw.Draw(img)
    t = (frame_tick % (ticks + 10)) / ticks  # 10 ticks of rest between loops
    p = pose(motion, t) if t < 1 else pose(motion, 0.0)
    depth = 1 / (1 - p[2] * 0.6)                       # further away = smaller
    s = 64 * p[6] * depth
    sx = s * abs(math.cos(math.radians(p[4]))) + 2      # turn about y: squash in x
    sy = s * abs(math.cos(math.radians(p[3])))          # tilt about x: squash in y
    icon = core.resize((max(2, int(sx)), max(2, int(sy))), Image.Resampling.NEAREST).rotate(-p[5], expand=True)
    cx = size / 2 + p[0] * 160
    cy = size * 0.62 - p[1] * 160
    img.paste(icon, (int(cx - icon.width / 2), int(cy - icon.height / 2)), icon)
    d.text((8, size + 4), LABEL[motion], fill='white', font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 15))
    return img


def main():
    with Image.open(CORE) as c:
        core = c.convert('RGBA').crop((0, 0, 16, 16))
    frames = []
    for sub in range(0, 50 * 2):           # 50 ticks at 2 frames per tick (50 ms per frame)
        tick = sub / 2
        row = Image.new('RGB', (200 * len(MOTIONS), 226))
        for i, (m, _, ticks) in enumerate(MOTIONS):
            row.paste(panel(core, m, ticks, tick), (200 * i, 0))
        frames.append(row)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    frames[0].save(OUT, save_all=True, append_images=frames[1:], duration=25, loop=0)
    print(OUT)


if __name__ == '__main__':
    main()
