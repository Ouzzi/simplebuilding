"""N124 release polish, mods 17-19 (offen-vor-release.md section 3, "Szenen" of Simple Tweaks, Simple Interfaces, Simple Quality of
Life), word for word:

Simple Tweaks          "Einstellungs-Bildschirm Texte lesen (W4C simpletweaks-5), Claim-Urkunde, Invsee."
  n124-tweaks-config   Settings screen the player's way (Escape -> Mods -> search -> row -> Configure...), in English AND German: title,
                       the four tabs (Players & World, Administration, Protection, Claims) each clicked with the real mouse and
                       photographed (top and scrolled to the end), every option name is a Simple Tweaks lang text (N124 named all 182
                       options; before, ~166 showed humanized English paths, also in German), every option has its own tooltip text;
                       German pass: names mostly German, title and tabs German. Reading the wording stays with the owner (shots).
  n124-tweaks-deed     Claim Deed: REAL right click in the air -> claim sheet (DeedScreen), "Claim" -> closed, right click again -> map
                       card (DeedMapScreen) with the side tabs Map/Buy/Rights, Buy -> DeedTradeScreen, Rights -> DeedScreen; shots.
  n124-tweaks-invsee   Invsee screen family (InvseeScreen): one player only in the probe world (no second target), so the DEATHS view
                       of the same screen class: death in survival with a diamond in the main inventory and an apple in the hotbar,
                       REAL click on Respawn; T27 keeps the apple; "/simpletweaks deaths <me>" opens InvseeScreen, title "Death backups
                       of <me>", the backup holds the diamond; "/simpletweaks invsee <me>" answers "Use your own inventory for that.".
Simple Interfaces      "Zoom/Tooltip-Bilder, Tastenseite GUI 3 (W4C-23), Händlerfenster."
  n124-si-zoom         Spyglass held with the REAL right button: zoom on at factor 0.1, three REAL wheel steps up zoom closer (factor
                       smaller), HUD hidden; release -> off. Zoom key: zoomHotkey switched on in memory (probe op siclient), REAL V held
                       -> key zoom at 0.25, wheel closer, V released -> off; shots.
  n124-si-tooltips     Survival inventory: shulker box with contents, bundle with 16 kinds, ender chest item (own ender chest filled and
                       synced by opening one). REAL pointer on each: shulker without Shift = note "Hold Shift..." (no grid), with REAL
                       Shift held = grid with the 2 stacks; ender chest with Shift = grid; bundle hover shot (all 16 entries). Shots.
  n124-si-keys3        Key page at GUI scale 3 (probe op guiscale): U opens KeysScreen; every visible widget lies inside the 1280 x 720
                       window; the three view tabs (By mod / By function / Keyboard) clicked, each then the inactive (chosen) one; the
                       Vanilla route Escape -> Options... -> Controls... -> Key Binds... -> "Simple Keys" opens it too. Shots; scale 2 again.
  n124-si-merchant     Librarian with two fixed trades (NoAI): crosshair on it (read back), REAL right click -> MerchantScreen, two offer
                       tiles (TradeOfferButton), REAL click on the first offer -> the emerald moves into the payment slot and the book
                       shows in the result slot; shots before/after (offer tile style, title spacing).
Simple Quality of Life "QoL-Gesamtpaket, Config EN/DE (W4C-40)."
  n124-qol-config      Super-mod settings via Mod Menu in English AND German: title, the Sub-mods tab, exactly the seven sub-mod
                       switches (Weather, Interfaces, Movement, Tools, Farming, Mob Manners, Loot) with their lang names and tooltips;
                       German pass German. Shots (the owner looks at the package view).

Patterns (uiprobe_n118/n123): position via int(float()) of status 'pos'; real clicks only after the crosshair target was read back, or
inside screens; chat lines read from client.log. Probe-only changes in SortProbe (status zoom/hover, ops guiscale/siclient) - additions,
nothing existing changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe124 --scale 2 \\
        --only n124-tweaks-config,n124-tweaks-deed,n124-tweaks-invsee,n124-si-zoom,n124-si-tooltips,n124-si-keys3,n124-si-merchant,n124-qol-config
"""
import importlib.util
import json
import re
import time
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
REPO = TOOLS.parents[2]
TAG = 'N124'


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


rp = _load('release_probe_n124', REPO / 'modules/simplelib/tools/release_probe.py')
sp = _load('sort_probe_n124', TOOLS / 'sort_probe.py')


# ---------------------------------------------------------------- helpers
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _plain(label):
    return re.sub('§.', '', label or '')


def _log(s):
    p = s.out / 'client.log'
    return p.read_text(errors='replace') if p.exists() else ''


def _say(s, cond, tag):
    s.chat(f'execute {cond} run say {TAG} {tag}', 0.35)


def _said(s, tags, settle=1.5):
    time.sleep(settle)
    log = _log(s)
    return {t: bool(re.search(TAG + ' ' + re.escape(t) + r'(?![\w-])', log)) for t in tags}


def _check_said(s, name, tags):
    got = _said(s, tags)
    miss = [t for t, ok in got.items() if not ok]
    s.check(name, not miss, f'{len(tags) - len(miss)}/{len(tags)}' + (f' missing {miss[:12]}' if miss else ''))
    return not miss


def _mark(s, marker):
    s.chat(f'say {TAG} MARK {marker}', 0.35)


def _after(s, marker, pattern, lines_after=14):
    time.sleep(1.5)
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} MARK {marker}')), default=-1)
    for ln in lines[idx + 1: idx + lines_after] if idx >= 0 else []:
        m = re.search(pattern, ln)
        if m:
            return m
    return None


def _prep(s, mode='creative'):
    s.close()
    s.chat('ride @s dismount', 0.3)
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true', 'keep_inventory false'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('clear @s', 0.3)


def _wait_screen(s, fragment, seconds=6.0):
    st = s.wait_screen(fragment, seconds)
    return st if fragment in st.get('screen', '') else None


def _park(s, st):
    s.io.move(*s.abs(st, 4, 4))
    time.sleep(0.3)


def _floor(s, x0, z0, x1, z1, y, block='minecraft:smooth_stone', clear=4):
    s.chat(f'fill {x0} {y} {z0} {x1} {y + clear} {z1} minecraft:air', 0.4)
    s.chat(f'fill {x0} {y - 1} {z0} {x1} {y - 1} {z1} {block}', 0.4)


def _lang(module, loc):
    out = {}
    for p in (REPO / 'modules' / module).glob(f'**/assets/*/lang/{loc}.json'):
        if 'build' in p.parts:
            continue
        try:
            out.update(json.loads(p.read_text(encoding='utf-8')))
        except (OSError, ValueError):
            pass
    return out


def _player_name(s):
    """The probe player's name from its own chat line ('[Name] N124 NAME')."""
    s.chat(f'say {TAG} NAME', 0.6)
    time.sleep(1.0)
    m = None
    for m in re.finditer(r'\[CHAT\] \[([A-Za-z0-9_]{1,16})\] ' + TAG + r' NAME', _log(s)):
        pass
    return m.group(1) if m else None


def _set_lang(s, code):
    """Switches the game language (probe op, resource reload ~25 s); returns the pause menu labels as proof."""
    s.close()
    sp.command('lang', value=code)
    time.sleep(25)
    s.close()
    s.key('Escape', 1.5)
    st = s.wait_screen('PauseScreen', 8)
    labels = [rp.plain(w['label']) for w in s.widgets(st)]
    s.close()
    return labels


# Mod Menu labels per language (modmenu.search / modmenu.configure, substring match)
MODMENU = {'en_us': ('Search', 'Configure'), 'de_de': ('suchen', 'Konfigur')}


def _modmenu_config(s, query, tag, loc, want_title):
    """Pause menu -> Mods -> search -> rows 0..2 until Configure... opens a screen titled want_title. Returns its status or None."""
    search, configure = MODMENU[loc]
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
        s.check(f'{tag}: "Mods" opens the Mod Menu list', False, st.get('screen'))
        return None
    if not s.click_widget(search, name=f'{tag}: Mod Menu search field'):
        return None
    s.type_text(query)
    time.sleep(1.2)
    titles = []
    for row in range(3):
        st = s.st()
        lw = s.find_widget(cls='ModListWidget', st=st)
        if not lw:
            s.check(f'{tag}: Mod Menu list widget present', False, '; '.join(s.labels(st))[:400])
            return None
        scale = st['scale']
        s.click_xy(lw['x'] - lw['w'] / 2 + lw['w'] * 0.45, lw['y'] - lw['h'] / 2 + (4 + 18 + 36 * row) * scale, st)
        time.sleep(0.9)
        st = s.st()
        btn = s.find_widget(configure, st=st)
        if not (btn and btn.get('active')):
            continue
        if row == 0:
            s.shot(f'config-{loc}-modmenu')
        s.click_xy(btn['x'], btn['y'], st)
        st = rp.wait(s, lambda t: t.get('screen', 'none') != 'none' and 'ModsScreen' not in t.get('screen', ''), 12)
        time.sleep(0.8)
        st = s.st()
        title = rp.plain(st.get('title', ''))
        titles.append(title)
        if want_title and want_title in title:
            s.check(f'{tag}: Configure... opens the screen titled "{want_title}"', True, f"row {row}, {st.get('screen')}")
            return st
        s.key('Escape', 1.0)
        s.wait_screen('ModsScreen', 6)
    s.check(f'{tag}: Configure... opens the screen titled "{want_title}"', False, f'titles seen {titles}')
    s.close()
    return None


def _config_pass(s, module, query, loc, title_key, want_tabs, other_names=None):
    """One language: title, every tab (real click + shots), names = lang texts of the module, every option with its own tooltip.
    Returns the list of option names seen."""
    tag = f'{module} config {loc}'
    lang = _lang(module, loc)
    other = _lang(module, 'de_de' if loc == 'en_us' else 'en_us')
    want_title = rp.plain(lang.get(title_key, ''))
    st = _modmenu_config(s, query, tag, loc, want_title)
    if not st:
        return []
    s.shot(f'config-{loc}-first')
    texts = {rp.plain(v) for v in lang.values() if isinstance(v, str)}
    lib = {rp.plain(v) for v in _lang('simplelib', loc).values() if isinstance(v, str)}
    tips = [' '.join(rp.plain(v).split()) for k, v in lang.items() if k.endswith('.@Tooltip') and isinstance(v, str)]
    tabs_want = [rp.plain(lang.get(k, '?')) for k in want_tabs]
    tabs_have = [rp.plain(w['label']) for w in s.widgets(st) if 'Tab' in w['class'] and rp.plain(w['label'])]
    if len(tabs_want) > 1:
        s.check(f'{tag}: tabs {tabs_want}', all(t in tabs_have for t in tabs_want), tabs_have)
    names, bad, no_tip = [], [], []
    for i, tab in enumerate(tabs_want):
        st = s.st()
        w = next((x for x in s.widgets(st) if 'Tab' in x['class'] and rp.plain(x['label']) == tab), None)
        if len(tabs_want) > 1:   # Cloth draws no tab bar for a single category: that page is the tab
            if not w:
                continue
            s.click_xy(w['x'], w['y'], st)
            time.sleep(1.0)
        st = s.st()
        slug = re.sub('[^a-z0-9]+', '-', tab.lower()).strip('-') or 'tab'
        entries = [e for e in st.get('entries', []) if rp.plain(e['label']).strip() and not rp.UUID.fullmatch(rp.plain(e['label']))]
        for e in entries:
            n = rp.plain(e['label'])
            names.append(n)
            if rp.raw_looking(n) or (n not in texts and n not in lib):
                bad.append(f'{tab}: {n}')
            joined = ' '.join(' '.join(rp.plain(x).split()) for x in e.get('tooltip', []))
            if 'SubCategory' not in e.get('class', '') and not any(t and t in joined for t in tips):
                no_tip.append(f'{tab}: {n}')
        s.shot(f'config-{loc}-{slug}')
        if entries:
            rp.hover(s, st, 380, 120, f'config-{loc}-{slug}-tooltip')
            s.wheel(640, 400, clicks=40, down=True, st=s.st())
            s.shot(f'config-{loc}-{slug}-end')
            s.wheel(640, 400, clicks=40, down=False, st=s.st())
    s.check(f'{tag}: option names listed', bool(names), f'{len(names)} names')
    s.check(f'{tag}: every option name is a lang text of the mod (no raw or humanized path)', bool(names) and not bad, bad[:20])
    s.check(f'{tag}: every option has its own tooltip text', bool(names) and not no_tip, no_tip[:20])
    if loc != 'en_us':
        o = {rp.plain(v) for v in other.values() if isinstance(v, str)}
        native = sum(1 for n in names if n in texts and n not in o)
        s.check(f'{tag}: names are German (more than half only in de_de)', names and native * 2 > len(names), f'{native}/{len(names)}')
    s.close()
    st = s.st()
    s.check(f'{tag}: Escape leaves the config screens', st.get('screen') == 'none', st.get('screen'))
    return names


def _both_languages(s, run):
    run('en_us')
    try:
        labels = _set_lang(s, 'de_de')
        s.check('language switched to German (pause menu "Zurück zum Spiel")', any('Zurück zum Spiel' in l for l in labels), labels[:8])
        run('de_de')
    finally:
        labels = _set_lang(s, 'en_us')
        s.check('language back to English (pause menu "Back to Game")', any('Back to Game' in l for l in labels), labels[:8])


# ---------------------------------------------------------------- Simple Tweaks
TWEAKS_TABS = [f'text.autoconfig.simpletweaks.category.{c}' for c in ('play', 'admin', 'protect', 'claims')]


def tweaks_config(s):
    _prep(s)
    seen = {}

    def run(loc):
        seen[loc] = _config_pass(s, 'simpletweaks', 'simpletweaks', loc, 'text.autoconfig.simpletweaks.title', TWEAKS_TABS)
    _both_languages(s, run)
    s.check('both passes list the same number of options (182 expected)', len(seen.get('en_us', [])) == len(seen.get('de_de', [])) >= 170,
            {k: len(v) for k, v in seen.items()})


def tweaks_deed(s):
    _prep(s)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 30', 1.5)
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)
    s.chat('item replace entity @s hotbar.0 with simpletweaks:claim_deed', 0.5)
    s.key('1', 0.5)
    s.io.right_click()
    st = _wait_screen(s, 'DeedScreen', 6)
    s.check('REAL right click with a fresh deed opens the claim sheet (DeedScreen)', st is not None, s.last.get('screen'))
    if not st:
        return
    _park(s, st)
    s.shot('1-claim-sheet')
    s.click_widget('Claim', name='button "Claim"')
    time.sleep(1.5)
    st = s.st()
    s.check('"Claim" closes the sheet', st.get('screen') == 'none', st.get('screen'))
    s.io.right_click()
    st = _wait_screen(s, 'DeedMapScreen', 6)
    s.check('REAL right click again opens the map card (DeedMapScreen)', st is not None, s.last.get('screen'))
    if not st:
        return
    tabs = [rp.plain(w['label']) for w in s.widgets(st) if w['class'] == 'SideTab']
    s.check('side tabs Map, Buy, Rights', all(t in tabs for t in ('Map', 'Buy', 'Rights')), tabs)
    _park(s, st)
    s.shot('2-map')
    if s.click_widget('Buy', cls='SideTab', name='side tab "Buy"'):
        st = _wait_screen(s, 'DeedTradeScreen', 4)
        s.check('Buy opens the trade screen (DeedTradeScreen)', st is not None, s.last.get('screen'))
        if st:
            _park(s, st)
            s.shot('3-buy')
    if s.click_widget('Rights', cls='SideTab', name='side tab "Rights"'):
        st = _wait_screen(s, 'DeedScreen', 4)
        s.check('Rights opens the rights editor (DeedScreen)', st is not None and 'DeedMapScreen' not in st.get('screen', ''), s.last.get('screen'))
        if st:
            _park(s, st)
            s.shot('4-rights')
    s.close()
    s.chat('clear @s', 0.3)


def tweaks_invsee(s):
    _prep(s, 'survival')
    name = _player_name(s)
    s.check('player name read from chat', bool(name), name)
    if not name:
        return
    s.chat('item replace entity @s inventory.5 with minecraft:diamond 7', 0.3)
    s.chat('item replace entity @s hotbar.0 with minecraft:apple 3', 0.3)
    s.chat('kill @s', 1.5)
    st = _wait_screen(s, 'DeathScreen', 6)
    s.check('death screen after /kill', st is not None, s.last.get('screen'))
    if st:
        time.sleep(1.5)
        s.click_widget(label='Respawn')
        time.sleep(2.0)
    if 'DeathScreen' in s.screen():
        s.check('REAL click on Respawn', False, s.last.get('screen'))
        return
    _say(s, 'if items entity @s hotbar.* minecraft:apple', 'apple-kept')
    _say(s, 'unless items entity @s inventory.* minecraft:diamond', 'diamond-lost')
    _check_said(s, 'T27 partial keep inventory: hotbar apple kept, main-inventory diamond dropped', ['apple-kept', 'diamond-lost'])
    s.chat(f'simpletweaks deaths {name}', 1.2)
    st = _wait_screen(s, 'InvseeScreen', 6)
    s.check(f'"/simpletweaks deaths {name}" opens InvseeScreen (death backup view)', st is not None, s.last.get('screen'))
    if st:
        t = rp.plain(st.get('title', ''))  # the screen shows the heading text "Death backups: <name>"
        s.check(f'title names the death backups of {name}', t.startswith('Death backups') and name in t, st.get('title'))
        other = [sl.get('stack', '') for sl in st.get('slots', []) if not sl.get('own')]
        s.check('the backup holds the 7 diamonds', any(str(v).startswith('minecraft:diamond x7') for v in other), [v for v in other if v][:8])
        _park(s, st)
        s.shot('death-backup')
        s.close()
    _mark(s, 'self')
    s.chat(f'simpletweaks invsee {name}', 1.0)
    s.check('"/simpletweaks invsee <me>" refuses: "Use your own inventory for that."',
            _after(s, 'self', r'Use your own inventory for that\.') is not None, 'see client.log')
    s.chat('gamemode creative @s', 0.3)
    s.chat('clear @s', 0.3)
    s.chat('kill @e[type=minecraft:item]', 0.3)


# ---------------------------------------------------------------- Simple Interfaces
def _zoom(s):
    return s.st().get('zoom') or {}


def _wheel_up(s, clicks=3):
    """REAL wheel steps up without moving the pointer (in the game a pointer move turns the camera)."""
    for _ in range(clicks):
        s.io.button(4, True)
        s.io.button(4, False)
        time.sleep(0.15)
    time.sleep(0.5)


def si_zoom(s):
    _prep(s)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.0)
    s.chat('item replace entity @s weapon.mainhand with minecraft:spyglass', 0.5)
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)
    s.shot('0-without')
    s.io.button(3, True)
    try:
        time.sleep(0.8)
        z0 = _zoom(s)
        s.check('spyglass held (REAL right button): zoom on, factor 0.1', z0.get('scoping') and z0.get('zooming') and abs(z0.get('factor', 0) - 0.1) < 1e-6, z0)
        s.check('HUD hidden while zooming (crosshair stays)', bool(z0.get('hideHud')), z0)
        s.shot('1-spyglass')
        _wheel_up(s)
        z1 = _zoom(s)
        s.check('three REAL wheel steps up zoom closer (factor < 0.1)', z1.get('zooming') and z1.get('factor', 1) < 0.095, z1)
        s.shot('2-spyglass-closer')
    finally:
        s.io.button(3, False)
    time.sleep(0.8)
    s.check('released: no zoom', not _zoom(s).get('zooming'), _zoom(s))
    s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.4)
    sp.command('siclient', field='zoomHotkey', value=True)
    time.sleep(0.5)
    s.io.key_down('v')
    try:
        time.sleep(0.8)
        z2 = _zoom(s)
        s.check('zoom key V held (zoomHotkey on): key zoom at factor 0.25', z2.get('key') and abs(z2.get('factor', 0) - 0.25) < 1e-6, z2)
        s.shot('3-key-zoom')
        _wheel_up(s)
        z3 = _zoom(s)
        s.check('wheel while the key is held zooms closer (factor < 0.25)', z3.get('key') and z3.get('factor', 1) < 0.24, z3)
        s.shot('4-key-zoom-closer')
    finally:
        s.io.key_up('v')
        sp.command('siclient', field='zoomHotkey', value=False)
    time.sleep(0.8)
    s.check('V released: no zoom', not _zoom(s).get('zooming'), _zoom(s))


BUNDLE_ITEMS = ['apple', 'bread', 'carrot', 'potato', 'beetroot', 'cookie', 'melon_slice', 'sweet_berries', 'glow_berries', 'egg',
                'feather', 'string', 'bone', 'arrow', 'flint', 'stick']


def _hover(s, cslot, name, shift=False):
    st = s.st()
    sl = s.slot_xy(st, cslot=cslot, own=True)
    if not sl:
        s.check(f'{name}: slot {cslot} present', False, f'{len(st.get("slots", []))} slots')
        return {}
    if shift:
        s.io.key_down('Shift_L')
    try:
        if shift:
            time.sleep(0.9)
        s.io.move(*s.abs(st, sl['x'] + 3, sl['y'] + 3))
        time.sleep(0.3)
        s.io.move(*s.abs(st, sl['x'], sl['y']))
        time.sleep(0.9)
        hv = s.st().get('hover') or {}
        s.shot(name)
    finally:
        if shift:
            s.io.key_up('Shift_L')
    time.sleep(0.4)
    return hv


def si_tooltips(s):
    _prep(s)
    x, y, z = _pos(s)
    _floor(s, x - 3, z - 3, x + 3, z + 3, y)
    # own ender chest: fill it, then open and close a real ender chest so the client gets the snapshot (EnderChestSnapshotMixin)
    s.chat('item replace entity @s enderchest.0 with minecraft:diamond 9', 0.3)
    s.chat('item replace entity @s enderchest.4 with minecraft:emerald 3', 0.3)
    s.chat(f'setblock {x + 2} {y} {z} minecraft:ender_chest', 0.5)
    s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.3)
    st = s.face(x + 2, y, z)
    for _ in range(6):
        if st.get('hitBlock') == 'minecraft:ender_chest':
            break
        time.sleep(0.3)
        st = s.st()
    s.check('crosshair on the ender chest', st.get('hitBlock') == 'minecraft:ender_chest', st.get('hitBlock'))
    s.io.right_click()
    st = rp.wait(s, lambda t: t.get('screen', 'none') != 'none', 5)
    s.check('REAL right click opens the ender chest', st.get('screen', 'none') != 'none', st.get('screen'))
    s.close()
    s.chat(f'setblock {x + 2} {y} {z} minecraft:air', 0.3)
    s.chat('gamemode survival @s', 0.4)
    s.chat('item replace entity @s inventory.0 with minecraft:shulker_box[minecraft:container=[{slot:0,item:{id:"minecraft:diamond",count:5}},'
           '{slot:1,item:{id:"minecraft:apple",count:3}}]]', 0.5)
    contents = ','.join(f'{{id:"minecraft:{i}",count:1}}' for i in BUNDLE_ITEMS)
    s.chat(f'item replace entity @s inventory.1 with minecraft:bundle[minecraft:bundle_contents=[{contents}]]', 0.6)
    s.chat('item replace entity @s inventory.2 with minecraft:ender_chest', 0.4)
    s.key('e', 1.2)
    st = _wait_screen(s, 'InventoryScreen', 5)
    s.check('survival inventory open (InventoryScreen)', st is not None, s.last.get('screen'))
    if not st:
        s.chat('gamemode creative @s', 0.3)
        return
    hv = _hover(s, 9, 'shulker-no-shift')
    s.check('shulker box without Shift: the note line, no grid', hv.get('stack', '').startswith('minecraft:shulker_box') and hv.get('grid') is False
            and 'Shift' in hv.get('note', ''), hv)
    hv = _hover(s, 9, 'shulker-shift', shift=True)
    s.check('shulker box with REAL Shift held: the slot grid with the 2 stacks', hv.get('shift') and hv.get('grid') and hv.get('filled') == 2, hv)
    hv = _hover(s, 10, 'bundle-16')
    s.check('bundle under the pointer (shot shows all 16 entries, Vanilla shows 12)', hv.get('stack', '').startswith('minecraft:bundle'), hv)
    hv = _hover(s, 11, 'ender-chest-shift', shift=True)
    s.check('ender chest item with REAL Shift held: grid of the own ender chest (2 stacks)', hv.get('grid') and hv.get('filled') == 2, hv)
    s.close()
    s.chat('gamemode creative @s', 0.3)
    s.chat('clear @s', 0.3)
    s.chat('item replace entity @s enderchest.0 with minecraft:air', 0.3)
    s.chat('item replace entity @s enderchest.4 with minecraft:air', 0.3)


def _inside(s, st):
    out = []
    for w in s.widgets(st):
        l, r = w['x'] - w['w'] / 2, w['x'] + w['w'] / 2
        t, b = w['y'] - w['h'] / 2, w['y'] + w['h'] / 2
        if l < -1 or t < -1 or r > 1281 or b > 721:
            out.append(f"{w['class']}:{rp.plain(w['label'])} [{l:.0f},{t:.0f},{r:.0f},{b:.0f}]")
    return out


def si_keys3(s):
    _prep(s)
    lang = _lang('simpleinterfaces', 'en_us')
    sp.command('guiscale', value=3)
    time.sleep(1.5)
    try:
        st = s.st()
        s.check('GUI scale 3 set', st.get('scale') == 3, st.get('scale'))
        s.key('u', 1.2)
        st = _wait_screen(s, 'KeysScreen', 5)
        s.check('U opens the key page (KeysScreen) at GUI 3', st is not None, s.last.get('screen'))
        if st:
            _park(s, st)
            s.shot('1-page')
            out = _inside(s, st)
            s.check('every visible widget lies inside the 1280 x 720 window', not out, out[:10])
            for view in ('mod', 'function', 'keyboard'):
                label = rp.plain(lang.get(f'simpleinterfaces.keys.tab.{view}', view))
                if not s.click_widget(label, cls='Button', name=f'view tab "{label}"'):
                    continue
                time.sleep(1.0)
                st = s.st()
                w = s.find_widget(label, cls='Button', st=st)
                s.check(f'view tab "{label}" is the chosen one (inactive) after the click', w is not None and not w.get('active'), w)
                out = _inside(s, st)
                s.check(f'{label}: every visible widget inside the window', not out, out[:10])
                _park(s, st)
                s.shot(f'2-{view}')
            s.close()
        # the Vanilla route: Options... -> Controls... -> Key Binds... -> "Simple Keys"
        s.key('Escape', 1.5)
        if _wait_screen(s, 'PauseScreen', 6):
            ok = s.click_widget('Options', name='pause menu "Options..."') and _wait_screen(s, 'OptionsScreen', 5)
            ok = ok and s.click_widget('Controls', name='"Controls..."') and _wait_screen(s, 'ControlsScreen', 5)
            ok = ok and s.click_widget('Key Binds', name='"Key Binds..."') and _wait_screen(s, 'KeyBindsScreen', 5)
            if ok:
                st = s.st()
                _park(s, st)
                s.shot('3-key-binds')
                s.click_widget(rp.plain(lang.get('simpleinterfaces.keys.open', 'Simple Keys')), name='button "Simple Keys"')
                st = _wait_screen(s, 'KeysScreen', 5)
                s.check('"Simple Keys" on the Vanilla key binds page opens KeysScreen', st is not None, s.last.get('screen'))
            else:
                s.check('Vanilla route to the key binds page', False, s.last.get('screen'))
        s.close()
    finally:
        s.close()
        sp.command('guiscale', value=2)
        time.sleep(1.5)


def si_merchant(s):
    _prep(s)
    x, y, z = _pos(s)
    _floor(s, x - 3, z - 3, x + 5, z + 3, y)
    s.chat('kill @e[type=minecraft:villager,distance=..20]', 0.4)
    offers = ('{Recipes:[{buy:{id:"minecraft:emerald",count:1},sell:{id:"minecraft:book",count:1},maxUses:12},'
              '{buy:{id:"minecraft:emerald",count:3},sell:{id:"minecraft:lantern",count:1},maxUses:12}]}')
    s.chat(f'execute positioned {x + 2.5} {y} {z + 0.5} summon minecraft:villager run data merge entity @s {{NoAI:1b,Invulnerable:1b,'
           f'PersistenceRequired:1b,Xp:10,VillagerData:{{profession:"minecraft:librarian",level:2,type:"minecraft:plains"}},Offers:{offers}}}', 1.0)
    # empty hand first: give fills the selected slot, and clearing the main hand afterwards would take the emeralds away
    s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.3)
    s.chat('item replace entity @s inventory.0 with minecraft:emerald 8', 0.5)
    hit = None
    for _ in range(4):
        st = rp.aim(s, x + 2.5, y + 1.2, z + 0.5)
        for _ in range(6):
            hit = st.get('hitEntity')
            if hit == 'minecraft:villager':
                break
            time.sleep(0.3)
            st = s.st()
        if hit == 'minecraft:villager':
            break
    s.check('crosshair on the villager', hit == 'minecraft:villager', hit)
    s.io.right_click()
    st = _wait_screen(s, 'MerchantScreen', 6)
    s.check('REAL right click opens the trading screen (MerchantScreen)', st is not None, s.last.get('screen'))
    if not st:
        return
    tiles = [w for w in s.widgets(st) if w['class'] == 'TradeOfferButton']
    s.check('two offer tiles (TradeOfferButton)', len(tiles) == 2, [w['class'] for w in s.widgets(st)])
    _park(s, st)
    s.shot('1-offers')
    if tiles:
        first = min(tiles, key=lambda w: w['y'])
        s.click_xy(first['x'], first['y'], st)
        time.sleep(1.0)
        st = s.st()
        merch = {sl['cslot']: sl.get('stack', '') for sl in st.get('slots', []) if not sl.get('own')}
        s.check('REAL click on the first offer: emerald in the payment slot, book in the result slot',
                str(merch.get(0, '')).startswith('minecraft:emerald') and str(merch.get(2, '')).startswith('minecraft:book'), merch)
        _park(s, st)
        s.shot('2-chosen-offer')
    s.close()
    s.chat('kill @e[type=minecraft:villager,distance=..20]', 0.4)
    s.chat('clear @s', 0.3)


# ---------------------------------------------------------------- Simple Quality of Life
QOL_SUBS = ['Weather', 'Interfaces', 'Movement', 'Tools', 'Farming', 'MobManners', 'Loot']


def qol_config(s):
    _prep(s)

    def run(loc):
        names = _config_pass(s, 'simplequalityoflife', 'simplequalityoflife', loc, 'text.autoconfig.simplequalityoflife.serverTitle',
                             ['text.autoconfig.simplequalityoflife.category.submods'])
        lang = _lang('simplequalityoflife', loc)
        want = [rp.plain(lang.get(f'text.autoconfig.simplequalityoflife.option.enableSimple{m}', m)) for m in QOL_SUBS]
        s.check(f'QoL {loc}: exactly the seven sub-mod switches', sorted(names) == sorted(want), {'seen': names, 'want': want})
    _both_languages(s, run)


SCENARIOS = {
    'n124-tweaks-config': tweaks_config,
    'n124-tweaks-deed': tweaks_deed,
    'n124-tweaks-invsee': tweaks_invsee,
    'n124-si-zoom': si_zoom,
    'n124-si-tooltips': si_tooltips,
    'n124-si-keys3': si_keys3,
    'n124-si-merchant': si_merchant,
    'n124-qol-config': qol_config,
}
