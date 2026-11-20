"""N111 config screen shots (release list, Config column): Simple Maps, Simple Shops and - found by tools/check_config_lang.py -
Simple Wanderers and Simple Sandwiches had options without a lang name, so the generic config screen (Mod Menu -> Configure)
showed raw paths. Each scenario opens the screen through the real Mod Menu and checks: title, every option name is a lang text
(release_probe.config_screen), plus one shot per page. Main session, real mouse:

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe111 --scale 2 \\
        --only simplemaps/config111,simpleshops/config111,simplewanderers/config111,simplesandwiches/config111
"""
import importlib.util
from pathlib import Path

_spec = importlib.util.spec_from_file_location('release_probe', Path(__file__).resolve().parents[2] / 'simplelib/tools/release_probe.py')
rp = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(rp)


def _screen(module, query, title):
    def run(s):
        s.chat('gamemode creative @s', 0.5)
        rp.config_screen(s, module, query, title)
    return run


SCENARIOS = {
    'simplemaps/config111': _screen('simplemaps', 'simple maps', 'Simple Maps'),
    'simpleshops/config111': _screen('simpleshops', 'simple shops', 'Simple Shops'),
    'simplewanderers/config111': _screen('simplewanderers', 'simple wanderers', 'Simple Wanderers'),
    'simplesandwiches/config111': _screen('simplesandwiches', 'simple sandwiches', 'Simple Sandwiches'),
}
