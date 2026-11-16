"""Shared steps of the release scenarios (b-probe5, offen-vor-release B5: simplechisels-3, simpleenchanting-4, simplelib-7,
simpleresonance-1; config screens of Chisels, Enchanting, Resonance, Crucibles, SimpleLib). Not a scenario file itself (ui_probe.py
loads only uiprobe_*.py); the uiprobe_<module>.py files load it by path:

    _rp = importlib.util.spec_from_file_location('release_probe', Path(__file__).resolve().parents[2] / 'simplelib/tools/release_probe.py')

config_screen(s, module, query, expect_title): the way a player goes (Escape -> "Mods" -> Mod Menu search -> first row -> "Configure...")
to the mod's config screen, every Cloth tab clicked with the real mouse and photographed; the option names (SortProbe status
"entries" = Cloth's getFieldName) must all be lang texts of the mod (or SimpleLib), never a raw / humanized option path.
"""
import json
import re
import time
from pathlib import Path

MODULES = Path(__file__).resolve().parents[2]


def plain(label):
    return re.sub('§.', '', label or '')


def wait(s, pred, seconds=4.0):
    end = time.time() + seconds
    st = s.st()
    while not pred(st) and time.time() < end:
        time.sleep(0.4)
        st = s.st()
    return st


def xyz(st):
    return [int(v) for v in st['pos'].replace(',', ' ').split()]


def aim(s, px, py, pz, wait_s=0.8):
    """Crosshair on the world point: 'tp @s ... facing' turns from the feet, so the eye height (1.62) is taken off."""
    x, y, z = xyz(s.st())
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {px:.3f} {py - 1.62:.3f} {pz:.3f}', wait_s)
    time.sleep(0.4)
    return s.st()


def hover(s, st, x, y, name):
    """Rests the real pointer on window point (x, y) (a small move first so the screen sees a motion) and takes a shot."""
    s.io.move(*s.abs(st, x + 3, y + 3))
    time.sleep(0.3)
    s.io.move(*s.abs(st, x, y))
    time.sleep(0.9)
    s.shot(name)


def hover_slot(s, st, slot, name):
    if not slot:
        s.check(f'{name}: slot present', False, 'slot missing')
        return
    hover(s, st, slot['x'], slot['y'], name)


def park_pointer(s):
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)


def lang_texts(*modules):
    """Every en_us and de_de lang value of these modules (the game may run in either language)."""
    out = set()
    for m in modules:
        for lang in ('en_us', 'de_de'):
            for p in (MODULES / m).glob(f'**/assets/*/lang/{lang}.json'):
                if 'build' in p.parts:
                    continue
                try:
                    out.update(plain(v) for v in json.loads(p.read_text(encoding='utf-8')).values() if isinstance(v, str))
                except (OSError, ValueError):
                    pass
    return out


RAW = (re.compile(r'[A-Za-z0-9_]+([._][A-Za-z0-9_]+)+'), re.compile(r'[a-z]+[A-Z][A-Za-z0-9]*'))


def raw_looking(label):
    return any(r.fullmatch(label) for r in RAW) or 'autoconfig' in label or '@Tooltip' in label


def modmenu_open(s, query, tag):
    """Pause menu -> Mods -> Mod Menu search -> first row -> Configure... Returns the status of the config screen or None."""
    s.close()
    s.key('Escape', 1.5)
    st = s.wait_screen('PauseScreen', 8)
    if 'PauseScreen' not in st.get('screen', ''):
        s.check(f'{tag}: Escape opens the pause menu', False, st.get('screen'))
        return None
    if not s.click_widget('Mods', name=f'{tag}: pause menu button "Mods" (Mod Menu)'):
        return None
    st = s.wait_screen('ModsScreen', 12)
    if 'ModsScreen' not in st.get('screen', ''):
        s.check(f'{tag}: the Mods button opens the Mod Menu list', False, st.get('screen'))
        return None
    if not s.click_widget('Search', name=f'{tag}: Mod Menu search field'):
        return None
    s.type_text(query)
    time.sleep(1.0)
    st = s.st()
    lw = s.find_widget(cls='ModListWidget', st=st)
    if not lw:
        s.check(f'{tag}: Mod Menu list widget present', False, '; '.join(s.labels(st))[:400])
        return None
    scale = st['scale']
    s.click_xy(lw['x'] - lw['w'] / 2 + lw['w'] * 0.45, lw['y'] - lw['h'] / 2 + (4 + 18) * scale, st)  # row 0: top + 4 px + half of 36 px
    time.sleep(0.8)
    s.shot('config-modmenu')
    if not s.click_widget('Configure', name=f'{tag}: Mod Menu button "Configure..."'):
        return None
    st = wait(s, lambda t: t.get('screen', 'none') != 'none' and 'ModsScreen' not in t.get('screen', ''), 12)
    ok = st.get('screen', 'none') != 'none' and 'ModsScreen' not in st.get('screen', '')
    s.check(f'{tag}: Configure... opens a config screen', ok, st.get('screen'))
    return st if ok else None


def config_screen(s, module, query, expect_title):
    """The config screen of `module` through Mod Menu: title, every tab, option names only from the lang files."""
    tag = f'{module} config'
    st = modmenu_open(s, query, tag)
    if not st:
        s.close()
        return
    time.sleep(0.8)
    st = s.st()
    title = plain(st.get('title', ''))
    s.check(f'{tag}: the screen is titled "{expect_title}"', expect_title.lower() in title.lower(), f"title={title!r} screen={st.get('screen')}")
    texts = lang_texts(module, 'simplelib')
    tabs = [plain(w['label']) for w in s.widgets(st) if 'TabButton' in w['class'] and plain(w['label'])]
    s.check(f'{tag}: tabs (informational)', True, tabs or 'one page, no tabs')
    seen, bad = [], []

    def collect(name):
        cur = s.st()
        names = [plain(e['label']) for e in cur.get('entries', []) if plain(e['label']).strip()]
        seen.extend(names)
        bad.extend(f'{name}: {n}' for n in names if raw_looking(n) or n not in texts)
        s.shot(f'config-{re.sub("[^a-z0-9]+", "-", name.lower()).strip("-") or "page"}')
        return names

    if not tabs:
        collect('page')
    for i, tab in enumerate(tabs):
        if i > 0:
            if not s.click_widget(tab, name=f'{tag}: tab "{tab}"'):
                continue
            time.sleep(0.8)
        collect(tab)
    s.check(f'{tag}: the screen lists option names (SortProbe entries)', bool(seen), f'{len(seen)} names')
    s.check(f'{tag}: every option name is a lang text (no raw path names)', bool(seen) and not bad, bad[:20] or seen[:12])
    s.close()
    st = s.st()
    s.check(f'{tag}: Escape leaves the config screens', st.get('screen') == 'none', st.get('screen'))
