"""b-earth82 (simpleearth-2, "few understandable sliders"): the Simple Earth menu in both states with the REAL mouse - the
player view (four version buttons, six sliders, no scrolling) and the hidden developer mode (all values of the version plus
the display values, drop-down of the other versions). The menu key "#" (SDL NONUSHASH) has no Xvfb key, so the screen opens
through Simple Interfaces' probe command 'screen'; the developer mode is switched with the client command.

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe82 --scale 2 --only simpleearth/menu82
"""
import importlib.util
import time
from pathlib import Path

_spec = importlib.util.spec_from_file_location('sort_probe', Path(__file__).resolve().parents[2] / 'simpleinterfaces/tools/sort_probe.py')
sp = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(sp)

SCREEN = 'com.simplebuilding.modules.simpleearth.client.EarthScreen'
SIMPLE = ('Render height', 'Detail level', 'Render distance', 'Atmosphere', 'Moon size', 'Show other players')
EXPERT = ('Edge length', 'Fold band', 'Paint budget')


def open_menu(s, developer):
    s.close()
    s.chat(f'simpleclient simpleearth set developerMode {"true" if developer else "false"}', 1.0)
    sp.command('screen', value=SCREEN)
    time.sleep(1.0)
    return s.wait_screen('EarthScreen', 8)


def settings(s, st):
    """Labels of the setting widgets (sliders and on/off buttons carry 'Name: value')."""
    return [w['label'] for w in s.widgets(st) if ':' in w['label']]


def menu(s):
    s.chat('time set noon', 0.3)
    s.chat('weather clear', 0.3)
    # 1 player view
    st = open_menu(s, False)
    s.check('menu opens (player view)', 'EarthScreen' in st.get('screen', ''), st.get('screen'))
    rows = settings(s, st)
    s.check('player view: exactly the six simple settings', len(rows) == 6 and all(any(r.startswith(n) for r in rows) for n in SIMPLE), rows)
    s.check('player view: no expert values', not any(r.startswith(n) for r in rows for n in EXPERT), rows)
    s.check('player view: no drop-down of other versions', not s.find_widget('More versions', st=st), s.labels(st))
    s.check('player view: four version buttons', all(s.find_widget(f'V{v}', st=st, contains=False) for v in (25, 26, 27, 5)), s.labels(st))
    s.shot('1-spieler')
    # a real click on the on/off button of the simple list
    before = next((r for r in rows if r.startswith('Show other players')), None)
    st = s.click_widget('Show other players', name='Show other players toggle')
    if st:
        after = next((w['label'] for w in s.widgets(st) if w['label'].startswith('Show other players')), None)
        s.check('real click toggles "Show other players"', before != after, f'{before} -> {after}')
        s.click_widget('Show other players', name='Show other players toggle back')
    # 2 developer mode
    st = open_menu(s, True)
    s.check('menu opens (developer mode)', 'EarthScreen' in st.get('screen', ''), st.get('screen'))
    rows = settings(s, st)
    s.check('developer mode: all values (> 20 rows incl. edge, fold band, paint budget)',
            len(rows) > 20 and all(any(r.startswith(n) for r in rows) for n in EXPERT), f'{len(rows)} rows')
    s.check('developer mode: drop-down of the other versions', s.find_widget('More versions', st=st), s.labels(st))
    s.shot('2-entwickler')
    # back to the player state, so the next scenario finds the normal client
    open_menu(s, False)
    s.shot('3-spieler-wieder')
    s.close()


SCENARIOS = {'simpleearth/menu82': menu}
