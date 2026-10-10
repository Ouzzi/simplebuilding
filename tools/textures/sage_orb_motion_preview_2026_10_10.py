"""Preview of the sage orb's hand motion (owner N21: the XP orb moves like the cores on use), 2026-10-10.

Usage (repository root, Pillow): python tools/textures/sage_orb_motion_preview_2026_10_10.py <out-dir>
Writes sage-orb-motion.png (the 8-tick motion in 9 steps, with the old bow-pose-free rest for reference) and
sage-orb-motion.gif. Pose math = CoreHandMotion.pose case ORB, ported 1:1; drawing as core_motions_preview_2026_10_06.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import core_motions_preview_2026_10_06 as cores  # noqa: E402

ORB_TICKS = 8
ORB = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures/item/sage_orb.png'


def orb_pose(t):
    p = [0, 0, 0, 0, 0, 0, 1]
    bell = math.sin(math.pi * t)
    loop = 2 * math.pi * t
    p[0] = 0.07 * math.sin(loop)
    p[1] = 0.2 * bell + 0.04 * (1 - math.cos(loop))
    p[2] = -0.08 * bell
    p[5] = 360 * cores.smooth(t)
    p[6] = 1 + 0.25 * bell * bell
    return p


def frame(orb, t, size=200):
    img = Image.new('RGB', (size, size + 22), '#2b3038')
    p = orb_pose(t)
    depth = 1 / (1 - p[2] * 0.6)
    s = 64 * p[6] * depth
    icon = orb.resize((max(2, int(s)), max(2, int(s))), Image.Resampling.NEAREST).rotate(-p[5], expand=True)
    cx, cy = size / 2 + p[0] * 160, size * 0.62 - p[1] * 160
    img.paste(icon, (int(cx - icon.width / 2), int(cy - icon.height / 2)), icon)
    ImageDraw.Draw(img).text((8, size + 4), 't = %.2f' % t, fill='white')
    return img


def main(out):
    out = Path(out)
    out.mkdir(parents=True, exist_ok=True)
    path = ORB if ORB.exists() else next(ROOT.glob('**/textures/item/sage_orb.png'))
    with Image.open(path) as o:
        orb = o.convert('RGBA').crop((0, 0, o.width, o.width))
    steps = [i / 8 for i in range(9)]
    strip = Image.new('RGB', (200 * len(steps), 222))
    for i, t in enumerate(steps):
        strip.paste(frame(orb, t), (200 * i, 0))
    strip.save(out / 'sage-orb-motion.png')
    frames = [frame(orb, min(1.0, sub / (ORB_TICKS * 4))) for sub in range(ORB_TICKS * 4 + 20)]
    frames[0].save(out / 'sage-orb-motion.gif', save_all=True, append_images=frames[1:], duration=12, loop=0)
    return out


if __name__ == '__main__':
    print(main(sys.argv[1]))
