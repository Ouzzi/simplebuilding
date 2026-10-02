"""Usage: python tools/textures/round5_settled_2026_10_02.py <vanilla textures dir> [core variant a|b|c]

Writes what the owner settled after rounds 4 and 5 (2026-10-02) into the 26.3 overlay:
- Ore Detector: round-5 dial (round-4 B, top-left rim fixed) and its 32 needle frames in the tilted view; the resting
  item (no find) shows the needle north, dimmed.
- Attractor: round-5 A (redstone red, lapis blue, iron tips).
- Echo Sounder: round-4 B sonar on the clean frame - 32 direction frames (same numbering as vanilla compass_XX:
  16 = up, clockwise), each animated (the inner ring pulses, 4 frames of 2 ticks), and the three cracked stages.
- Resonance Rod: redrawn (resonance_rod_2026_10_02.py).
- Cores: round-4 C with the softer star transition (cores_2026_10_02.py), variant b unless given."""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
CORE_VARIANT = sys.argv[2] if len(sys.argv) > 2 else 'b'
sys.argv = [sys.argv[0], V]
import proposals_v3_2026_10_02 as v3  # noqa: E402
import proposals_v4_2026_10_02 as v4  # noqa: E402
import proposals_v5_2026_10_02 as v5  # noqa: E402
import echo_sounder_textures as old_echo  # noqa: E402
import resonance_rod_2026_10_02 as rod  # noqa: E402
import cores_2026_10_02 as cores  # noqa: E402
from PIL import Image  # noqa: E402

v3.V = V
T = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'item')
PULSE_FRAMES = 4
PULSE_TICKS = 2


def save(image, name):
    os.makedirs(T, exist_ok=True)
    image.save(os.path.join(T, name + '.png'))


def animate(name):
    with open(os.path.join(T, name + '.png.mcmeta'), 'w', encoding='utf-8') as f:
        json.dump({'animation': {'frametime': PULSE_TICKS}}, f, indent=2)
        f.write('\n')


def dim(image, factor=0.6):
    out = image.copy()
    for y in range(16):
        for x in range(16):
            p = out.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), tuple(round(c * factor) for c in p[:3]) + (p[3],))
    return out


def echo_cracked(stage):
    c = old_echo.CRACKED_COLOURS[stage]
    hexc = {k: tuple(int(v[i:i + 2], 16) for i in (1, 3, 5)) for k, v in c.items()}
    saved = dict(v4.E)
    v4.E.update({'g': hexc['g'], 'G': hexc['G'], 'F': hexc['F'], 'f': hexc['f'], 't': hexc['tip']})
    try:
        im = v4.echo_b_frame(16, 3)  # arc resting at the bottom, no pulse
    finally:
        v4.E.clear()
        v4.E.update(saved)
    for x, y, light in old_echo.CRACK_STAGES[stage]:
        im.putpixel((x, y), old_echo.CRACK_LIGHT if light else old_echo.CRACK_DARK)
    return im


def main():
    # Ore Detector
    dial = v5.detector_dial()
    save(dial, 'detector_dial')
    for f in range(32):
        save(v5.detector_needle(f), f'detector_needle_{f:02d}')
    save(v3.over(dial, dim(v5.detector_needle(16))), 'detector')
    # Attractor
    save(v5.attractor_a(), 'magnet')
    # Echo Sounder
    for f in range(32):
        strip = Image.new('RGBA', (16, 16 * PULSE_FRAMES), (0, 0, 0, 0))
        for tick in range(PULSE_FRAMES):
            strip.alpha_composite(v4.echo_b_frame((f - 16) % 32, tick), (0, 16 * tick))
        save(strip, f'echo_sounder_{f:02d}')
        animate(f'echo_sounder_{f:02d}')
    for stage in old_echo.CRACK_STAGES:
        save(echo_cracked(stage), f'echo_sounder_cracked_{stage}')
    # Resonance Rod
    for name, im in rod.textures().items():
        save(im, name)
    # Cores
    for name in cores.CORES:
        save(cores.make(name, CORE_VARIANT), f'{name}_core')
    print('ok')


if __name__ == '__main__':
    main()
