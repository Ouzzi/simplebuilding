"""simpletweaks-5: the generated settings screen (4 config files, ~518 texts) with the REAL mouse - the way a player gets there.

Scenario for modules/simpleinterfaces/tools/ui_probe.py. There is NO command or key for the screen: SimpleLib's
GenericConfigScreen is only offered to Mod Menu (SimpleLibModMenu.getProvidedConfigScreenFactories). The title screen is not
reachable from the probe world, but Mod Menu (a dev mod of the integration client, enabled-mods.json) adds a "Mods" button to the PAUSE
menu, so the route is: Escape -> PauseScreen -> real click "Mods" -> ModsScreen -> search box "tweaks" -> real click on the first
list entry -> "Configure" -> the screen (with Cloth Config loaded: Cloth's ClothConfigScreen; its categories are the groups of
ConfigScreenModel.categories = the four files play / admin / protect / claims and their sub-groups).
Real widgets (label/class from the status) are used wherever Mod Menu / Cloth have them; the Mod Menu mod LIST is a selection list whose
entries are NOT widgets, so the first entry is clicked by position: list widget = class containing "ModList" (its centre/size come from the
status; first row = 17 gui px below the list top, rows 26 px - a Mod Menu layout ASSUMPTION; the scenario tries the first 3 rows until a
"Configure" button becomes active) - with the search filter "tweaks" the first rows are Simple Tweaks.
Tabs of the Cloth screen: every widget whose class name contains "Tab" is clicked once with a screenshot (Cloth's tab buttons; if its
class names differ the check lists the labels, nothing else is guessed). Entries (toggles, number fields, lists) are drawn by Cloth and
are not asserted: a human looks at the screenshots (518 texts: truncated names, tooltips, wrong categories, raw keys with dots).
"""
import re
import time


def _wait_for(s, cond, seconds):
    end = time.time() + seconds
    while time.time() < end:
        st = s.st()
        if cond(st):
            return st
        time.sleep(0.5)
    return s.st()


def _slug(text):
    return re.sub(r'[^a-z0-9]+', '-', text.lower()).strip('-')[:24] or 'tab'


def tweaks(s):
    # ---- Escape -> pause menu -> "Mods" (Mod Menu)
    s.close()
    st = s.key('Escape', 1.2)
    s.check('Escape opens the pause menu', 'PauseScreen' in st.get('screen', '') or 'Pause' in st.get('screen', ''), st.get('screen'))
    mods = s.find_widget('Mods', st=st)
    s.check('Mod Menu adds a "Mods" button to the pause menu', mods is not None, s.labels(st))
    if not mods:
        s.close()
        return
    s.click_widget('Mods', name='Mods button')
    st = s.wait_screen('ModsScreen', 12)
    s.check('real click on "Mods" opens the Mod Menu screen', 'ModsScreen' in st.get('screen', ''), st.get('screen'))
    if 'ModsScreen' not in st.get('screen', ''):
        s.close()
        return
    time.sleep(1.0)
    # ---- search "tweaks", first row, Configure
    st = s.st()
    box = s.find_widget(cls='EditBox', st=st)
    s.check('Mod Menu search box is a widget', box is not None, s.labels(st))
    if box:
        s.click_xy(box['x'], box['y'], st)
        s.type_text('tweaks')
        time.sleep(1.2)
    st = s.st()
    lst = next((w for w in s.widgets(st) if 'ModList' in w['class']), None)
    s.check('Mod Menu list widget found', lst is not None, s.labels(st))
    sc = st['scale']
    if lst:
        left, top = lst['x'] - lst['w'] / 2, lst['y'] - lst['h'] / 2
        x_click = left + min(80 * sc, lst['w'] / 2)
        rows_y = [top + (17 + 26 * i) * sc for i in range(3)]
    else:  # fallback: left half of the window, below the search box (gui y 48 + 17)
        x_click = 1280 / 4
        rows_y = [(48 + 17 + 26 * i) * sc for i in range(3)]
    configure = None
    for y in rows_y:
        s.click_xy(x_click, y, st)
        time.sleep(0.9)
        st = s.st()
        configure = s.find_widget('Configure', st=st)
        if configure and configure['active']:
            break
    s.check('a list row selects a mod and "Configure" becomes active', configure is not None and configure['active'], s.labels(st))
    s.shot('mod-menu-tweaks-selected')  # shows which mod the row click selected
    if not (configure and configure['active']):
        s.close()
        return
    s.click_xy(configure['x'], configure['y'], st)
    st = _wait_for(s, lambda t: 'ModsScreen' not in t.get('screen', ''), 12)
    s.check('Configure opens a config screen', 'ModsScreen' not in st.get('screen', '') and st.get('screen') != 'none', st.get('screen'))
    if 'ModsScreen' in st.get('screen', '') or st.get('screen') == 'none':
        s.close()
        return
    time.sleep(1.5)
    st = s.st()
    s.check('the config screen is Cloth Config (GenericConfigScreen with ClothForm)', 'clothconfig' in st['screen'].lower() or 'cloth' in st['screen'].lower(), st['screen'])
    s.shot('config-first-tab')
    # ---- tabs
    tabs = [w for w in s.widgets(st) if 'Tab' in w['class']]
    s.check('category tabs found (4 files: play, admin, protect, claims)', len(tabs) >= 4, [(w['class'], w['label']) for w in s.widgets(st)][:40])
    for i, tab in enumerate(tabs[:12]):
        st = s.st()
        cur = next((w for w in s.widgets(st) if 'Tab' in w['class'] and w['label'] == tab['label']), None)
        if not cur:
            continue
        s.click_xy(cur['x'], cur['y'], st)
        time.sleep(1.0)
        s.shot(f'tab-{i:02d}-{_slug(tab["label"])}')
        # scroll the entry list to the end of the tab and look at it again (long lists: tooltips, number fields, string lists)
        st = s.st()
        s.wheel(1280 // 2, 720 // 2, clicks=8, down=True, st=st)
        s.shot(f'tab-{i:02d}-{_slug(tab["label"])}-end')
    # ---- buttons of the screen (Cloth: Cancel / Save) and leaving without saving
    st = s.st()
    s.shot('config-buttons')
    s.check('screen still open after all tab clicks', st.get('screen') != 'none' and 'ModsScreen' not in st.get('screen', ''), st.get('screen'))
    s.key('Escape', 1.0)
    st = s.st()
    s.check('Escape leaves the config screen (no edits, no confirmation) back to Mod Menu', 'ModsScreen' in st.get('screen', ''), st.get('screen'))
    s.close()


SCENARIOS = {'simpletweaks': tweaks}
