"""simpleweather-3: rain density 20 % and 100 % in the real game picture (a-w4c, scenario for modules/simpleinterfaces/tools/ui_probe.py).

Flow: black concrete pad on the flat world (rain splashes show well on it), '/weather rain', then '/simpleweather config set
clientRainParticleDensity <20|100>' (the option is client look only and takes effect at once), GUI hidden with F1, per density one picture
into the sky (pitch up, rain streaks against the sky) and one onto the pad (splashes). Checks: the config file on disk holds the value.
The density option is read live by VisualRainMixin (air rain scaled by the rain level) and ClientWeatherMixin (ground particles/sounds).
# NUR BESITZER: whether 20 % "looks clearly thinner" than 100 % and how the rain sound feels at 20 %.
"""
import json
import math
import time
from pathlib import Path

GAME = Path(__file__).resolve().parents[3] / 'integration/run-fabric-263'


def _pos(s):
    return tuple(int(v) for v in s.st()['pos'].replace(',', ' ').split())


def _aim(s, f, t, wait=0.8):
    """Stand at feet point f and look at point t (eye height 1.62, like a crosshair)."""
    dx, dy, dz = t[0] - f[0], t[1] - (f[1] + 1.62), t[2] - f[2]
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    s.chat(f'tp @s {f[0]:.2f} {f[1]} {f[2]:.2f} {yaw:.1f} {pitch:.1f}', wait)


def _config_value(key):
    """Value of config/simpleweather.json on disk (None when unreadable); the config command writes it at once."""
    for _ in range(10):
        try:
            return json.loads((GAME / 'config' / 'simpleweather.json').read_text()).get(key)
        except (OSError, ValueError):
            time.sleep(0.3)
    return None


def rain_density(s):
    s.chat('time set day', 0.4)
    s.chat('weather clear', 0.4)
    x, y, z = _pos(s)
    # own spot 20 blocks south; a 11x11 black pad one block below the feet
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.6)
    x, y, z = _pos(s)
    s.chat(f'fill {x - 5} {y - 1} {z + 1} {x + 5} {y - 1} {z + 11} minecraft:black_concrete', 1.0)
    feet = (x + 0.5, y, z + 0.5)
    s.chat('weather rain 12000', 0.5)
    time.sleep(8)  # the rain level fades in at 1 % per tick (5 s)
    s.key('F1', 0.6)
    try:
        for density in (20, 100):
            s.chat(f'simpleweather config set clientRainParticleDensity {density}', 1.0)
            value = _config_value('clientRainParticleDensity')
            s.check(f'rain density {density} % saved by /simpleweather config set', value == density, f'config/simpleweather.json: {value}')
            time.sleep(1.5)
            _aim(s, feet, (x + 0.5, y + 14, z + 12.5), 1.2)  # the sky ahead
            s.shot(f'rain-{density}-sky')
            _aim(s, feet, (x + 0.5, y - 1, z + 5.5), 1.2)  # the pad
            s.shot(f'rain-{density}-ground')
    finally:
        s.key('F1', 0.6)
        s.chat('simpleweather config set clientRainParticleDensity 100', 0.6)
        s.chat('weather clear', 0.6)


SCENARIOS = {'simpleweather': rain_density}
