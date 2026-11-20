"""N118 (release polish, offen-vor-release.md section 3, mods 1 + 2): the "Szenen" lines of SimpleBuilding and SimpleLib with the REAL
mouse/keys, one scenario per scene. Main session only (B runs no client):

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe118 --scale 2 \\
        --only sb118-creative,sb118-guide,sb118-config,sb118-vault,sb118-headslot,sb118-smithing,lib118-guide,lib118-freelook

SimpleBuilding
  sb118-creative   creative inventory: every simplebuilding:* tab opens with a real click (selectedTab), shows items, scrolls.
  sb118-guide      Beginner's Guide (simplebuilding:guide_book used in the air): SimpleBuilding's own book, top bookmarks of the Simple
                   Modpack guide, page turn (spread +2); "More Simple mods" -> SimpleLib's guide screen at the simplebuilding section:
                   the side bookmarks carry the chapter numbers -1 (contents), 0, 1, ... in order, a chapter bookmark opens exactly
                   that chapter, the next arrow turns the page (guide_probe "Kapitelnummern" in the world).
  sb118-config     SimpleBuilding's settings through Mod Menu (Escape -> Mods -> search -> Configure...), in English AND German:
                   title, all nine category tabs clicked, every option name a lang text, every option has a tooltip, no raw keys;
                   the German pass must show German names/tooltips (pause menu "Zurück zum Spiel" proves the switch).
  sb118-vault      Astral Vault: title, 54 container slots in 6 rows + 36 inventory slots, then the full shared/astral round trip
                   (uiprobe_core: shared with the ender chest, astral part only in the vault, contents persist).
  sb118-headslot   survival inventory: extra head slot (menu 46) on the helmet row right of the armor column, refuses dirt, takes a
                   carved pumpkin, shift-click of a mob head lands in a head slot (uiprobe_core).
  sb118-smithing   smithing table = RecipeBookSmithingScreen, the recipe book button opens the book (search field appears), then a real
                   Netherite upgrade (uiprobe_core).
SimpleLib
  lib118-guide     Simple Modpack guide in SURVIVAL (creative opens every chapter): grey (locked) and unlocked bookmarks side by side,
                   a locked bookmark keeps the page; the chapter's advancement granted -> after reopening the same bookmark is unlocked
                   and opens its chapter; page turn with the next arrow and the Right/Left keys. The advancement is revoked again.
  lib118-freelook  free look crosshair: client log names the mods asking for free_look; real left Alt held -> free look on, the grey
                   body-direction mark at the centre; a fixed camera offset (probe op "freelook", the key stays held) of 30 degrees
                   right puts the mark ~148 GUI px left of the centre on the same height, 20 degrees down puts it above; the body yaw
                   stays; Alt released -> off, the mark is gone after the ease back.

Positions use `[int(float(v)) for v in pos.split()]`; blocks are opened with a right click only after the crosshair was re-checked on
the block (tp ... facing turns the camera; no pointer move follows it). Only a human judges: look of the vault rows, guide pages,
creative tab layout, config wording, the grey mark.
"""
import importlib.util
import json
import math
import re
import time
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
REPO = TOOLS.parents[2]


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


rp = _load('release_probe', REPO / 'modules/simplelib/tools/release_probe.py')
core = _load('uiprobe_core_n118', REPO / 'modules/simplebuildingtools/tools/uiprobe_core.py')
sp = _load('sort_probe_n118', TOOLS / 'sort_probe.py')


# ---------------------------------------------------------------- helpers
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _setup(s, mode='creative'):
    s.close()
    s.chat(f'gamemode {mode} @s', 0.4)
    s.chat('time set noon', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('gamerule advance_time false', 0.3)
    s.chat('gamerule spawn_mobs false', 0.3)
    s.chat('clear @s', 0.3)


def _lang(loc, sb=True, modules=()):
    """Lang values of SimpleBuilding (common source + 26.3 overlay) and/or modules for one locale."""
    files = []
    if sb:
        files += [REPO / f'src/main/resources/assets/simplebuilding/lang/{loc}.json', REPO / f'mc26_3/overlay/resources/assets/simplebuilding/lang/{loc}.json']
    for m in modules:
        files += [p for p in (REPO / 'modules' / m).glob(f'**/assets/*/lang/{loc}.json') if 'build' not in p.parts]
    out = {}
    for f in files:
        try:
            out.update(json.loads(f.read_text(encoding='utf-8')))
        except (OSError, ValueError):
            pass
    return out


def _all_modules():
    return sorted(p.name for p in (REPO / 'modules').iterdir() if p.is_dir())


def _centre(rect):
    x, y, w, h = rect
    return x + w / 2, y + h / 2


def _look_up(s):
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 -50', 0.8)
    time.sleep(0.4)


def _use_in_air(s, item, fragment='Guide', seconds=8):
    s.chat(f'item replace entity @s weapon.mainhand with {item}', 0.5)
    _look_up(s)
    s.io.right_click()
    return s.wait_screen(fragment, seconds)


def _open_block(s, block, fragment, dx=2, dy=0, dz=0):
    """setblock + crosshair on it (re-checked, re-aimed once) + real right click. Returns the screen status or None."""
    s.close()
    x, y, z = _pos(s)
    s.chat(f'setblock {x + dx} {y + dy} {z + dz} minecraft:air', 0.4)
    bx, by, bz = s.stage(block, dx, dy, dz)
    time.sleep(0.6)
    st = s.st()
    if st.get('hitBlock') != block:
        st = s.face(bx, by, bz)
        time.sleep(0.6)
        st = s.st()
    s.check(f'crosshair on {block}', st.get('hitBlock') == block, st.get('hitBlock'))
    s.io.right_click()
    st = s.wait_screen(fragment, 10)
    ok = fragment in st.get('screen', '')
    s.check(f'real right click on {block} opens {fragment}', ok, st.get('screen'))
    return st if ok else None


def _guide(st):
    return st.get('guide') or {}


# ---------------------------------------------------------------- SimpleBuilding: creative tab
def sb_creative(s):
    _setup(s)
    s.key('e', 1.2)
    st = s.wait_screen('CreativeModeInventoryScreen', 8)
    if 'CreativeModeInventoryScreen' not in st.get('screen', ''):
        s.check('creative: E opens the creative inventory', False, st.get('screen'))
        return
    tabs = [t for t in st.get('tabs', []) if t['id'].startswith('simplebuilding:')]
    s.check('creative: SimpleBuilding tab(s) displayed', bool(tabs), st.get('tabsError') or [t['id'] for t in st.get('tabs', [])][:40])
    for tab in tabs:
        st = s.st()
        for _ in range(12):   # Fabric pages the tabs: turn with the real '<' / '>' buttons
            if 'page' not in tab or st.get('tabPage', tab['page']) == tab['page']:
                break
            arrow = '>' if st.get('tabPage', 0) < tab['page'] else '<'
            w = next((w for w in s.widgets(st) if rp.plain(w['label']) == arrow), None)
            if not w:
                break
            s.click_xy(w['x'], w['y'], st)
            time.sleep(0.6)
            st = s.st()
        cur = next((t for t in st.get('tabs', []) if t['id'] == tab['id']), tab)
        s.click_xy(cur['x'], cur['y'], st)
        time.sleep(0.9)
        st = s.st()
        name = re.sub('[^a-z0-9]+', '-', tab['id'].lower())
        s.check(f'creative: a real click selects {tab["id"]}', st.get('selectedTab') == tab['id'], st.get('selectedTab'))
        shown = [x['stack'] for x in st.get('slots', []) if not x['own'] and x['stack']]
        s.check(f'creative: {tab["id"]} shows items', len(shown) >= 9, f'{len(shown)} filled slots, first {shown[:6]}')
        foreign = sorted({x.split(':')[0] for x in shown} - {'simplebuilding', 'minecraft'})
        s.check(f'creative: {tab["id"]} item namespaces (informational)', True, foreign or 'simplebuilding/minecraft only')
        rp.park_pointer(s)
        time.sleep(0.3)
        s.shot(f'tab-{name}')
        s.wheel(640, 330, 4, down=True)
        s.shot(f'tab-{name}-scrolled')
    s.close()


# ---------------------------------------------------------------- SimpleBuilding: beginner / modpack guide, chapter numbers
def sb_guide(s):
    _setup(s)
    st = _use_in_air(s, 'simplebuilding:guide_book')
    ok = st.get('screen', '').endswith('GuideBookScreen')
    s.check("beginner's guide: use in the air opens SimpleBuilding's own book (GuideBookScreen)", ok, st.get('screen'))
    if not ok:
        s.close()
        return
    time.sleep(0.6)
    st = s.st()
    s.shot('beginner-p0')
    top = [t['id'] for t in _guide(st).get('top', [])]
    s.check("beginner's guide: top bookmarks of the Simple Modpack guide incl. simplebuilding", 'simplebuilding' in top and len(top) >= 2, top)
    sp0 = st.get('fields', {}).get('spread')
    st = s.key('Right', 0.9)
    sp1 = st.get('fields', {}).get('spread')
    s.check("beginner's guide: Right turns the spread by 2", sp0 is not None and sp1 == sp0 + 2, f'{sp0} -> {sp1}')
    s.shot('beginner-p1')
    st = s.key('Left', 0.9)
    s.check("beginner's guide: Left turns back", st.get('fields', {}).get('spread') == sp0, st.get('fields', {}).get('spread'))
    more = _guide(st).get('more')
    s.check('beginner\'s guide: "More Simple mods" bookmark reported', bool(more), _guide(st).keys())
    if not more:
        s.close()
        return
    s.click_xy(*_centre(more), st)
    st = s.wait_screen('ModpackGuideScreen', 8)
    g = _guide(st)
    s.check('"More Simple mods" opens SimpleLib\'s guide screen at the simplebuilding section',
            st.get('screen', '').endswith('ModpackGuideScreen') and g.get('section') == 'simplebuilding', f"{st.get('screen')} {g.get('section')}")
    tabs = g.get('tabs', [])
    chapters = [t['chapter'] for t in tabs]
    s.check('modpack guide: side bookmarks numbered -1 (contents), 0, 1, ... in order (chapter numbers)',
            bool(chapters) and chapters == list(range(-1, len(chapters) - 1)), chapters)
    s.check('modpack guide (creative): every chapter bookmark open', all(t.get('open', True) for t in tabs if t['chapter'] >= 0),
            [t['chapter'] for t in tabs if t.get('open') is False])
    s.check('modpack guide: every chapter has pages', all(t.get('pages', 0) > 0 for t in tabs if t['chapter'] >= 0),
            [(t['chapter'], t.get('pages')) for t in tabs if t['chapter'] >= 0 and not t.get('pages')])
    s.shot('modpack-sb-contents')
    for pick in [t for t in tabs if t['chapter'] >= 0 and t.get('pages', 0) > 0][:3:2]:   # first and third chapter
        want = pick['chapter']
        s.click_xy(*_centre(pick['rect']), s.st())
        st = s.st()
        time.sleep(0.6)
        st = s.st()
        s.check(f'modpack guide: bookmark of chapter {want + 1} opens chapter {want + 1}', _guide(st).get('chapter') == want,
                f"chapter {_guide(st).get('chapter')}, spread {_guide(st).get('spread')}")
        s.shot(f'modpack-sb-chapter-{want + 1}')
    st = s.st()
    g = _guide(st)
    spread = g.get('spread')
    if g.get('next'):
        s.click_xy(*_centre(g['next']), st)
        time.sleep(0.6)
        st = s.st()
        s.check('modpack guide: the next arrow turns the page (spread +2)', _guide(st).get('spread') == (spread or 0) + 2,
                f"{spread} -> {_guide(st).get('spread')}")
        s.shot('modpack-sb-next')
    s.close()
    st = s.st()
    s.check('guide: Escape closes it', st.get('screen') == 'none', st.get('screen'))


# ---------------------------------------------------------------- SimpleBuilding: config window via Mod Menu, EN + DE
CATS = ['building', 'modules', 'equipment', 'pistons', 'tweaks', 'world', 'visuals', 'advanced', 'server']


def _set_lang(s, code):
    s.close()
    sp.command('lang', value=code)
    time.sleep(25)          # resource reload of the full integration client
    s.close()
    s.key('Escape', 1.5)
    st = s.wait_screen('PauseScreen', 8)
    labels = [rp.plain(w['label']) for w in s.widgets(st)]
    s.close()
    return labels


# Mod Menu labels per language (modmenu.search / modmenu.configure, substring match of click_widget)
MODMENU = {'en_us': ('Search', 'Configure'), 'de_de': ('suchen', 'Konfigur')}


def _modmenu_open(s, query, tag, loc):
    """release_probe.modmenu_open with the labels of the game language: pause menu -> Mods -> search -> first row -> Configure."""
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
        s.check(f'{tag}: the Mods button opens the Mod Menu list', False, st.get('screen'))
        return None
    if not s.click_widget(search, name=f'{tag}: Mod Menu search field'):
        return None
    s.type_text(query)
    time.sleep(1.0)
    st = s.st()
    lw = s.find_widget(cls='ModListWidget', st=st)
    if not lw:
        s.check(f'{tag}: Mod Menu list widget present', False, '; '.join(s.labels(st))[:400])
        return None
    s.click_xy(lw['x'] - lw['w'] / 2 + lw['w'] * 0.45, lw['y'] - lw['h'] / 2 + (4 + 18) * st['scale'], st)   # row 0
    time.sleep(0.8)
    s.shot(f'config-{loc}-modmenu')
    if not s.click_widget(configure, name=f'{tag}: Mod Menu button "{configure}..."'):
        return None
    st = rp.wait(s, lambda t: t.get('screen', 'none') != 'none' and 'ModsScreen' not in t.get('screen', ''), 12)
    ok = st.get('screen', 'none') != 'none' and 'ModsScreen' not in st.get('screen', '')
    s.check(f'{tag}: Configure opens a config screen', ok, st.get('screen'))
    return st if ok else None


def _tab_widget(s, label, st):
    return next((w for w in s.widgets(st) if 'Tab' in w['class'] and rp.plain(w['label']) == label), None)


def _config_pass(s, loc, en_tips):
    lang = _lang(loc)
    tag = f'core config {loc}'
    st = _modmenu_open(s, 'super mod', tag, loc)
    if not st:
        s.close()
        return
    time.sleep(0.8)
    st = s.st()
    title = rp.plain(st.get('title', ''))
    want_title = lang.get('text.autoconfig.simplebuilding.title', '')
    s.check(f'{tag}: title "{want_title}"', bool(want_title) and want_title in title, f"title={title!r} screen={st.get('screen')}")
    s.shot(f'config-{loc}-first')
    texts = set(rp.plain(v) for v in lang.values() if isinstance(v, str))
    other = set(rp.plain(v) for v in _lang('de_de' if loc == 'en_us' else 'en_us').values() if isinstance(v, str))
    modtexts = rp.lang_texts(*_all_modules())
    reached, names_all, bad, no_tip, raw_tip, tips = [], [], [], [], [], {}
    for cat in CATS:
        label = rp.plain(lang.get(f'text.autoconfig.simplebuilding.category.{cat}', cat))
        st = s.st()
        w = _tab_widget(s, label, st)
        for _ in range(8):     # Cloth scrolls the tab bar: wheel over it until the tab lies inside the window
            if w and 0 < w['x'] - w['w'] / 2 and w['x'] + w['w'] / 2 < 1280:
                break
            anchor = next((x for x in s.widgets(st) if 'Tab' in x['class']), None)
            if not anchor:
                break
            st = s.wheel(640, anchor['y'], 2, down=True, st=st)
            w = _tab_widget(s, label, st)
        if not w:
            s.check(f'{tag}: category tab "{label}" present', False, [rp.plain(x['label']) for x in s.widgets(st) if 'Tab' in x['class']])
            continue
        s.click_xy(w['x'], w['y'], st)
        time.sleep(0.9)
        st = s.st()
        reached.append(label)
        entries = [e for e in st.get('entries', []) if rp.plain(e['label']).strip() and not rp.UUID.fullmatch(rp.plain(e['label']))]
        for i, e in enumerate(entries):
            n = rp.plain(e['label'])
            names_all.append(n)
            if rp.raw_looking(n) or (n not in texts and n not in modtexts):
                bad.append(f'{cat}: {n}')
            tip = [rp.plain(x) for x in e.get('tooltip', []) if rp.plain(x).strip()]
            if not tip:
                no_tip.append(f'{cat}: {n}')
            raw_tip += [f'{cat}: {n}: {x}' for x in tip if 'autoconfig' in x or '@Tooltip' in x or re.fullmatch(r'[a-z0-9_.]+\.[a-z0-9_.]+', x)]
            tips[(cat, i)] = ' '.join(tip)
        s.shot(f'config-{loc}-{cat}')
        if entries:   # rest the pointer on the first row so its tooltip shows in a second shot
            rp.hover(s, st, 380, 120, f'config-{loc}-{cat}-tooltip')
    s.check(f'{tag}: all nine categories reached by a real click', len(reached) == len(CATS), reached)
    s.check(f'{tag}: option names listed', bool(names_all), f'{len(names_all)} names')
    s.check(f'{tag}: every option name is a lang text (no raw keys)', bool(names_all) and not bad, bad[:20])
    s.check(f'{tag}: every option has a tooltip', bool(names_all) and not no_tip, no_tip[:20])
    s.check(f'{tag}: no tooltip line is a raw key', not raw_tip, raw_tip[:10])
    if loc != 'en_us':
        native = sum(1 for n in names_all if n in texts and n not in other)
        s.check(f'{tag}: names are German (more than half only in de_de)', names_all and native * 2 > len(names_all), f'{native}/{len(names_all)}')
        same = sum(1 for k, v in tips.items() if v and en_tips.get(k) == v)
        s.check(f'{tag}: tooltips differ from the English pass (at most 20 % equal)', tips and same * 5 <= len(tips), f'{same}/{len(tips)} equal')
    s.close()
    st = s.st()
    s.check(f'{tag}: Escape leaves the config screens', st.get('screen') == 'none', st.get('screen'))
    return tips


def sb_config(s):
    _setup(s)
    en = _config_pass(s, 'en_us', {}) or {}
    try:
        labels = _set_lang(s, 'de_de')
        s.check('language switched to German (pause menu "Zurück zum Spiel")', any('Zurück zum Spiel' in l for l in labels), labels[:8])
        _config_pass(s, 'de_de', en)
    finally:
        labels = _set_lang(s, 'en_us')
        s.check('language back to English (pause menu "Back to Game")', any('Back to Game' in l for l in labels), labels[:8])


# ---------------------------------------------------------------- SimpleBuilding: Astral Vault, extra head slot, smithing recipe book
def sb_vault(s):
    _setup(s)
    names = {_lang(l).get('block.simplebuilding.astral_vault') for l in ('en_us', 'de_de')} - {None}
    st = _open_block(s, 'simplebuilding:astral_vault', 'ContainerScreen')
    if st:
        cont = [x for x in st['slots'] if not x['own']]
        rows = sorted({x['y'] for x in cont})
        s.check('vault: titled with the block name (EN/DE)', rp.plain(st.get('title', '')) in names, f"{st.get('title')!r} vs {names}")
        s.check('vault: 54 container slots in 6 rows of 9', len(cont) == 54 and len(rows) == 6, f'{len(cont)} slots, {len(rows)} rows')
        s.check('vault: 36 inventory slots', sum(1 for x in st['slots'] if x['own']) == 36, sum(1 for x in st['slots'] if x['own']))
        rp.park_pointer(s)
        s.shot('vault-54-slots')
    s.close()
    core._vault(s)


def sb_headslot(s):
    _setup(s, 'survival')
    s.close()
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 6)
    if 'InventoryScreen' in st.get('screen', ''):
        extra = s.slot_xy(st, index=46)
        helmet = s.slot_xy(st, index=5)
        s.check('inventory: extra head slot (46) on the helmet row, right of the armor column',
                bool(extra and helmet) and abs(extra['y'] - helmet['y']) <= 2 and extra['x'] > helmet['x'], (extra, helmet))
        s.close()
        s.chat('item replace entity @s hotbar.0 with minecraft:dirt 3', 0.5)
        s.key('e', 1.2)
        st = s.wait_screen('InventoryScreen', 6)
        s.click_slot(cslot=0, own=True)
        s.click_slot(index=46)
        time.sleep(0.5)
        st = s.st()
        s.check('inventory: the extra head slot refuses dirt', not core._stack(st, 46), core._stack(st, 46))
        s.shot('headslot-refuses-dirt')
        s.click_slot(cslot=0, own=True)      # dirt back
        s.close()
    else:
        s.check('survival: E opens the inventory', False, st.get('screen'))
    s.chat('clear @s', 0.3)
    core._inventory(s)


def sb_smithing(s):
    _setup(s)
    st = _open_block(s, 'minecraft:smithing_table', 'SmithingScreen', dx=0, dz=2)
    if st:
        s.check('smithing: the screen is RecipeBookSmithingScreen', st.get('screen', '').endswith('RecipeBookSmithingScreen'), st.get('screen'))
        before = sum(1 for w in s.widgets(st) if w['class'] == 'EditBox')
        if s.click_widget(cls='ImageButton', name='smithing recipe book button'):
            time.sleep(1.0)
            st = s.st()
            after = sum(1 for w in s.widgets(st) if w['class'] == 'EditBox')
            s.check('smithing: the recipe book opens (its search field appears)', after > before, f'edit boxes {before} -> {after}')
            s.shot('smithing-recipe-book-open')
            s.click_widget(cls='ImageButton', name='smithing recipe book button (close)')
            time.sleep(0.6)
    s.close()
    core._smithing(s)


# ---------------------------------------------------------------- SimpleLib: modpack guide bookmarks grey / unlocked, page turn
def _to_sb_chapters(s, st):
    """From whatever guide screen is open to SimpleLib's guide screen at the simplebuilding section (top bookmark / "More")."""
    for _ in range(3):
        g = _guide(st)
        if st.get('screen', '').endswith('ModpackGuideScreen') and g.get('section') == 'simplebuilding':
            return st
        if g.get('more'):
            s.click_xy(*_centre(g['more']), st)
        else:
            t = next((t for t in g.get('top', []) if t['id'] == 'simplebuilding'), None)
            if not t:
                break
            s.click_xy(*_centre(t['rect']), st)
        time.sleep(1.2)
        st = s.st()
    return st


def lib_guide(s):
    _setup(s, 'survival')
    granted = None
    try:
        st = _use_in_air(s, 'simplelib:modpack_guide')
        s.check('modpack guide: use in the air opens a guide screen', 'Guide' in st.get('screen', ''), st.get('screen'))
        st = _to_sb_chapters(s, st)
        time.sleep(1.5)                  # the server's answer to the unlock query (sent when the screen opens)
        st = s.st()
        g = _guide(st)
        s.check('modpack guide: SimpleLib guide screen at the simplebuilding chapters', g.get('section') == 'simplebuilding', f"{st.get('screen')} {g.get('section')}")
        tabs = [t for t in g.get('tabs', []) if t['chapter'] >= 0]
        locked = [t for t in tabs if t.get('open') is False and t.get('advancement')]
        unlocked = [t for t in tabs if t.get('open') is True]
        s.check('modpack guide (survival): grey (locked) and unlocked bookmarks side by side', bool(locked) and bool(unlocked),
                f'{len(locked)} locked, {len(unlocked)} unlocked of {len(tabs)}')
        rp.park_pointer(s)
        s.shot('bookmarks-grey-unlocked')
        if not locked:
            return
        pick = locked[0]
        before = (g.get('chapter'), g.get('spread'))
        rp.hover(s, st, *_centre(pick['rect']), 'locked-bookmark-tooltip')
        st = s.click_xy(*_centre(pick['rect']), st)
        time.sleep(0.6)
        st = s.st()
        s.check(f'modpack guide: the locked bookmark (chapter {pick["chapter"] + 1}) keeps the page', (_guide(st).get('chapter'), _guide(st).get('spread')) == before,
                f"{before} -> {(_guide(st).get('chapter'), _guide(st).get('spread'))}")
        granted = pick['advancement']
        s.close()
        s.chat(f'advancement grant @s only {granted}', 1.5)
        st = _use_in_air(s, 'simplelib:modpack_guide')
        st = _to_sb_chapters(s, st)
        time.sleep(1.5)                  # the server's answer to the unlock query relayouts the screen
        st = s.st()
        again = next((t for t in _guide(st).get('tabs', []) if t['chapter'] == pick['chapter']), None)
        s.check(f'modpack guide: after the advancement the bookmark of chapter {pick["chapter"] + 1} is unlocked', bool(again) and again.get('open') is True, again)
        rp.park_pointer(s)
        s.shot('bookmark-unlocked-after-advancement')
        if not again or not again.get('open'):
            return
        s.click_xy(*_centre(again['rect']), st)
        time.sleep(0.8)
        st = s.st()
        s.check(f'modpack guide: the unlocked bookmark opens chapter {pick["chapter"] + 1}', _guide(st).get('chapter') == pick['chapter'],
                f"chapter {_guide(st).get('chapter')}")
        s.shot('unlocked-chapter-open')
        spread = _guide(st).get('spread')
        nxt = _guide(st).get('next')
        if nxt:
            s.click_xy(*_centre(nxt), st)
            time.sleep(0.6)
            st = s.st()
            s.check('modpack guide: the next arrow turns the page (spread +2)', _guide(st).get('spread') == spread + 2, f"{spread} -> {_guide(st).get('spread')}")
            s.shot('page-next')
        st = s.key('Right', 0.8)
        s.check('modpack guide: Right turns another page', _guide(st).get('spread') == spread + 4, f"{spread} -> {_guide(st).get('spread')}")
        st = s.key('Left', 0.8)
        s.check('modpack guide: Left turns back', _guide(st).get('spread') == spread + 2, f"-> {_guide(st).get('spread')}")
        s.shot('page-back')
        s.close()
    finally:
        s.close()
        if granted:
            s.chat(f'advancement revoke @s only {granted}', 0.8)
        s.chat('gamemode creative @s', 0.4)


# ---------------------------------------------------------------- SimpleLib: free-look crosshair
def lib_freelook(s):
    _setup(s)
    try:
        log = (s.out / 'client.log').read_text(errors='replace')
    except OSError:
        log = ''
    line = next((ln for ln in log.splitlines() if 'SimpleLib feature free_look:' in ln), '')
    s.check('client log: free_look on, asked for by Simple QoL or Simple Earth (left Alt is the default key)',
            'free_look: on, asked for by [' in line and ('simplequalityoflife' in line or 'simpleearth' in line), line[-300:] or 'no line')
    x, y, z = _pos(s)
    s.chat(f'setblock {x + 4} {y + 1} {z} minecraft:gold_block', 0.3)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} -90 0', 1.0)
    time.sleep(0.5)
    st = s.st()
    fl = st.get('freeLook', {})
    body = st.get('yaw')
    s.check('free look off at the start: no grey mark', fl.get('active') is False and 'crosshair' not in fl, fl)
    s.check('free look crosshair option on (default)', fl.get('crosshairOption') is True, fl)
    s.shot('freelook-off')
    scale = st.get('scale', 2)
    cx, cy = 640, 360
    s.io.key_down('Alt_L')
    try:
        time.sleep(0.8)
        st = s.st()
        fl = st.get('freeLook', {})
        mark = fl.get('crosshair')
        s.check('left Alt held: free look on, grey mark at the centre', fl.get('active') is True and bool(mark)
                and abs(mark[0] - cx) <= 2 * scale and abs(mark[1] - cy) <= 2 * scale, fl)
        sp.command('freelook', yaw=30.0, pitch=0.0, held=True)
        time.sleep(0.6)
        st = s.st()
        fl = st.get('freeLook', {})
        mark = fl.get('crosshair')
        # 70 degrees FOV, GUI 1280/scale x 720/scale: focal = (360/scale)/tan(35) -> 30 degrees right = focal*tan(30) GUI px left of the centre
        want = cx - (360 / scale) / math.tan(math.radians(35)) * math.tan(math.radians(30)) * scale
        s.check('camera 30 degrees right: the grey mark ~148 GUI px left of the centre, same height', bool(mark) and abs(mark[0] - want) <= 4 * scale
                and abs(mark[1] - cy) <= 2 * scale, f'mark {mark}, expected x {want:.0f}; {fl}')
        s.check('free look: the body did not turn', st.get('yaw') is not None and body is not None and abs(st['yaw'] - body) < 0.5, f"{body} -> {st.get('yaw')}")
        s.shot('freelook-camera-30-right-mark-left')
        sp.command('freelook', yaw=0.0, pitch=20.0, held=True)
        time.sleep(0.6)
        st = s.st()
        mark = st.get('freeLook', {}).get('crosshair')
        want_y = cy - (360 / scale) / math.tan(math.radians(35)) * math.tan(math.radians(20)) * scale
        s.check('camera 20 degrees down: the grey mark ~94 GUI px above the centre', bool(mark) and abs(mark[1] - want_y) <= 4 * scale
                and abs(mark[0] - cx) <= 2 * scale, f'mark {mark}, expected y {want_y:.0f}')
        s.shot('freelook-camera-20-down-mark-up')
    finally:
        s.io.key_up('Alt_L')
    time.sleep(1.0)
    st = s.st()
    fl = st.get('freeLook', {})
    s.check('Alt released: free look off, the mark is gone after the ease back', fl.get('active') is False and 'crosshair' not in fl, fl)
    s.check('Alt released (release mode camera): the body yaw is unchanged', abs((st.get('yaw') or 0) - (body or 0)) < 0.5, f"{body} -> {st.get('yaw')}")
    s.shot('freelook-released')


SCENARIOS = {
    'sb118-creative': sb_creative,
    'sb118-guide': sb_guide,
    'sb118-config': sb_config,
    'sb118-vault': sb_vault,
    'sb118-headslot': sb_headslot,
    'sb118-smithing': sb_smithing,
    'lib118-guide': lib_guide,
    'lib118-freelook': lib_freelook,
}
