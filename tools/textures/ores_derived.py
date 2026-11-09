"""vanilla_derived hook (ABN30-10): the Vanilla-derived textures of simpleores, see ores_2026_10_30.py and (N4, b-ores13) ores13_2026_11_09.py."""
import ores13_2026_11_09
from ores_2026_10_30 import derived as _ores_derived


def derived(jar) -> dict:
    out = _ores_derived(jar)
    out.update(ores13_2026_11_09.derived(jar))
    return out
