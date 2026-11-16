"""N79 spyglass frame (copper, 6 / 10 GUI px, slides in from the screen edge): real right click held with a spyglass, shots
while it slides in (about 0.25 s), fully open, and while it slides back out after letting go; for both dev variants.

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe79 --scale 2 --only simpleinterfaces/n79
"""
import importlib.util
import time
from pathlib import Path

_spec = importlib.util.spec_from_file_location('sort_probe', Path(__file__).with_name('sort_probe.py'))
sp = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(sp)


def scope(s):
    s.close()
    s.clear_and_give('spyglass 1')
    s.chat('time set noon', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    for variant, label in (('a', '6px'), ('b', '10px')):
        sp.command('variant', id='simpleinterfaces:scope_frame', value=variant)
        time.sleep(0.6)
        s.shot(f'{label}-0-ohne')
        s.io.button(3, True)
        time.sleep(0.08)
        s.shot(f'{label}-1-baut-auf')
        time.sleep(0.9)
        s.shot(f'{label}-2-offen')
        s.io.button(3, False)
        time.sleep(0.07)
        s.shot(f'{label}-3-baut-ab')
        time.sleep(0.9)
        s.shot(f'{label}-4-zu')
    s.check('spyglass shots for both frame widths taken', True, 'see shots/')


SCENARIOS = {'simpleinterfaces/n79': scope}
