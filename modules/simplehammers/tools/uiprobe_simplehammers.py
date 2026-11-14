"""simplehammers-3: settings window and the hammer scenes with the REAL mouse (scenarios for modules/simpleinterfaces/tools/ui_probe.py).

Simple Hammers has NO window of its own. Its "Einstellungsfenster" is SimpleLib's generic config screen (L13, GenericConfigScreen; the options of
HammerConfig are announced with ConfigCommands, so the screen is built automatically). A player reaches it ONLY through Mod Menu:
  Escape (pause menu) -> button "Mods" -> Mod Menu list -> search "hammers" -> click the mod -> button "Configure..." -> Cloth Config screen
(the integration client has Mod Menu and Cloth Config; without Cloth the screen would be SimpleLib's Vanilla form instead). There is no command
and no key. If a step of this path cannot be found the scenario says which widget was missing (labels are in the check detail).

Scenario keys use a hyphen (simplestorage-chest), not a slash: ui_probe's shot() writes shots/<scenario>-<name>.png without creating sub folders.
SCENARIOS
  simplehammers          the settings window: pause menu -> Mods -> Mod Menu search -> mod -> Configure... -> Cloth screen with the six options
                         (1 switch + 5 numbers 5/2/4/10/81): switch clicked twice (edit marker appears and disappears), nothing is saved.
  simplehammers-area     area outline (3x3) with a hammer in the hand, sneak = no outline (shots), extra cracks while mining an Obsidian wall,
                         then a stone wall is mined in survival: exactly the 3x3 around the aimed block disappears (crosshair probing).
  simplehammers-reshape  "Umformen": sneak + hold right click on a stone block, shots at 1 s and at the end, the block must change (stairs/slab/octets).
  simplehammers-crush    "Stein zerkleinern" (HS29-3): right clicks on a quartz block: it breaks after a few strikes and 4 quartz drop; picked up.

Assumptions to check on the first run: label "Mods" of the pause menu button (Mod Menu "replace bugs" style), the first row of the Mod Menu list is the
searched mod (row centre = list top + 22 GUI px), the "Configure..." sprite button carries its tooltip text as label, Cloth toggles read
"Yes"/"No" (with colour codes). Only a human can judge: look of the outline and the extra cracks, arm pose, the progress/charge look while reshaping,
the swing between the strikes (shots show single frames only), the sounds.
"""
import re
import time


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _xyz(st):
    return [int(v) for v in st['pos'].replace(',', ' ').split()]


def _plain(label):
    return re.sub('§.', '', label or '')


def _wait(s, pred, seconds=4.0):
    end = time.time() + seconds
    st = s.st()
    while not pred(st) and time.time() < end:
        time.sleep(0.4)
        st = s.st()
    return st


def _wait_other(s, fragment, seconds=10.0):
    """Waits for a screen that is open and does NOT contain the fragment."""
    return _wait(s, lambda t: t.get('screen', 'none') != 'none' and fragment not in t.get('screen', ''), seconds)


def _toggles(s, st):
    return [w for w in s.widgets(st) if _plain(w['label']) in ('Yes', 'No')]


def _open_config(s, query, tag):
    """Pause menu -> Mods -> Mod Menu search -> first row -> Configure... Returns the status of the config screen or None."""
    s.close()
    s.key('Escape', 1.5)
    st = s.wait_screen('PauseScreen', 8)
    if 'PauseScreen' not in st.get('screen', ''):
        s.check(f'{tag}: Escape opens the pause menu', False, st.get('screen'))
        return None
    s.shot('config-pause-menu')
    if not s.click_widget('Mods', name='pause menu button "Mods" (Mod Menu)'):
        return None
    st = s.wait_screen('ModsScreen', 12)
    if 'ModsScreen' not in st.get('screen', ''):
        s.check(f'{tag}: the Mods button opens the Mod Menu list', False, st.get('screen'))
        return None
    s.check(f'{tag}: the Mods button opens the Mod Menu list', True, st.get('screen'))
    if not s.click_widget('Search', name='Mod Menu search field'):
        return None
    s.type_text(query)
    time.sleep(1.0)
    st = s.st()
    lw = s.find_widget(cls='ModListWidget', st=st)
    if not lw:
        s.check(f'{tag}: Mod Menu list widget present', False, '; '.join(s.labels(st))[:400])
        return None
    # first row: list top + 4 px padding + half of the 36 px row (all in GUI px); x: inside the row, right of the icon
    scale = st['scale']
    s.click_xy(lw['x'] - lw['w'] / 2 + lw['w'] * 0.45, lw['y'] - lw['h'] / 2 + (4 + 18) * scale, st)
    time.sleep(0.8)
    s.shot('config-modmenu-selected')
    if not s.click_widget('Configure', name='Mod Menu button "Configure..."'):
        return None
    st = _wait_other(s, 'ModsScreen', 12)
    ok = st.get('screen', 'none') != 'none' and 'ModsScreen' not in st.get('screen', '')
    s.check(f'{tag}: Configure... opens a config screen', ok, st.get('screen'))
    return st if ok else None


def config(s):
    st = _open_config(s, 'hammers', 'hammers config')
    if not st:
        return
    time.sleep(0.8)
    st = s.st()
    s.check('config screen is the Cloth Config screen', 'ClothConfig' in st.get('screen', ''), st.get('screen'))
    s.shot('config-screen')
    labels = [_plain(l) for l in s.labels(st)]
    s.check('config screen has Save and a Cancel/Discard button', any('Save' in l for l in labels) and any(('Cancel' in l or 'Discard' in l) for l in labels), labels)
    values = sorted(w['value'] for w in s.widgets(st) if w.get('value') is not None)
    s.check('config screen shows the five numbers of HammerConfig (5, 2, 4, 10, 81)', values == sorted(['5', '2', '4', '10', '81']), values)
    toggles = _toggles(s, st)
    s.check('config screen has the one switch (enabled), shown "Yes"', len(toggles) == 1 and _plain(toggles[0]['label']) == 'Yes', [_plain(w['label']) for w in toggles])
    if toggles:
        s.click_xy(toggles[0]['x'], toggles[0]['y'], st)
        time.sleep(0.6)
        st = s.st()
        now = _toggles(s, st)
        s.check('real click flips the switch to "No"', len(now) == 1 and _plain(now[0]['label']) == 'No', [_plain(w['label']) for w in now])
        s.check('an edit marker appears: the Cancel button now reads "Discard Changes"', any('Discard' in _plain(l) for l in s.labels(st)), [_plain(l) for l in s.labels(st)])
        s.shot('config-edited')
        if now:
            s.click_xy(now[0]['x'], now[0]['y'], st)
            time.sleep(0.6)
            st = s.st()
            back = _toggles(s, st)
            s.check('second click flips it back to "Yes" (nothing is saved)', len(back) == 1 and _plain(back[0]['label']) == 'Yes', [_plain(w['label']) for w in back])
    # leave without saving: Escape x3 (config -> Mod Menu -> pause menu -> game)
    s.close()
    st = s.st()
    s.check('Escape leaves the config screens back to the game', st.get('screen') == 'none', st.get('screen'))


# ---------------------------------------------------------------- scenes with the hammer in the hand
def _wall(s, block, dx=2, up=4, half=2):
    """A wall of block in the plane dx east of the player, 2*half+1 wide, from the feet level up. Returns the player's block position."""
    x, y, z = _xyz(s.st())
    s.chat(f'fill {x + dx} {y} {z - half} {x + dx} {y + up} {z + half} {block}', 0.8)
    return x, y, z


def _hit(s, bx, by, bz):
    s.face(bx, by, bz)
    return s.st().get('hitBlock')


def area(s):
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.4)
    s.chat('item replace entity @s weapon.mainhand with simplehammers:diamond_sledgehammer', 0.5)
    # ---- outline on a stone wall, centre block (x+2, y+1, z)
    x, y, z = _wall(s, 'minecraft:stone')
    hit = _hit(s, x + 2, y + 1, z)
    s.check('crosshair on the stone wall', hit == 'minecraft:stone', hit)
    time.sleep(1.0)
    s.shot('outline-3x3')
    # sneaking: single block, no outline (the hammer mines one block)
    s.io.key_down('Shift_L')
    time.sleep(1.0)
    s.shot('outline-sneak')
    s.io.key_up('Shift_L')
    # ---- extra cracks while mining a hard wall (Obsidian, hold the left button)
    s.chat(f'fill {x + 2} {y} {z - 2} {x + 2} {y + 4} {z + 2} minecraft:obsidian', 0.8)
    hit = _hit(s, x + 2, y + 1, z)
    s.check('crosshair on the obsidian wall', hit == 'minecraft:obsidian', hit)
    s.io.button(1, True)
    time.sleep(2.5)
    s.shot('cracks-area-mining')
    time.sleep(1.5)
    s.io.button(1, False)
    time.sleep(0.5)
    # ---- real area break on stone: exactly the 3x3 around the aimed block goes (diamond hammer: a few seconds at most)
    s.chat(f'fill {x + 2} {y} {z - 2} {x + 2} {y + 4} {z + 2} minecraft:stone', 0.8)
    hit = _hit(s, x + 2, y + 1, z)
    s.check('crosshair on the stone wall again', hit == 'minecraft:stone', hit)
    s.io.button(1, True)
    st = _wait(s, lambda t: t.get('hitBlock') != 'minecraft:stone', 10)
    s.io.button(1, False)
    time.sleep(0.8)
    s.check('the aimed block broke within 10 s of holding the real left button', st.get('hitBlock') != 'minecraft:stone', st.get('hitBlock'))
    s.shot('area-broken')
    corner = _hit(s, x + 2, y + 2, z + 1)
    s.check('area: the diagonal neighbour (x+2, y+2, z+1) broke too (3x3)', corner is None, corner)
    outside = _hit(s, x + 2, y + 3, z)
    s.check('area: a block two rows above the centre did NOT break (only 3x3)', outside == 'minecraft:stone', outside)
    s.chat(f'fill {x + 2} {y} {z - 2} {x + 2} {y + 4} {z + 2} minecraft:air', 0.6)
    s.chat('gamemode creative @s', 0.5)


def reshape(s):
    """Sneak + hold right click: carve an eighth off a block (needs the hammer in the hand, empty off hand)."""
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.4)
    s.chat('item replace entity @s weapon.mainhand with simplehammers:diamond_sledgehammer', 0.5)
    s.stage('minecraft:stone', 2, 0, 0)
    hit = s.st().get('hitBlock')
    s.check('crosshair on the stone block to reshape', hit == 'minecraft:stone', hit)
    s.io.key_down('Shift_L')
    s.io.button(3, True)
    time.sleep(1.0)
    s.shot('reshape-1s')
    st = _wait(s, lambda t: t.get('hitBlock') != 'minecraft:stone', 8)
    s.shot('reshape-end')
    s.io.button(3, False)
    s.io.key_up('Shift_L')
    s.check('holding sneak + right click reshaped the stone block (crosshair block changed)', st.get('hitBlock') != 'minecraft:stone', st.get('hitBlock'))
    s.chat('gamemode creative @s', 0.5)


def crush(s):
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.4)
    s.chat('item replace entity @s weapon.mainhand with simplehammers:iron_sledgehammer', 0.5)
    bx, by, bz = s.stage('minecraft:quartz_block', 2, 0, -3)
    hit = s.st().get('hitBlock')
    s.check('crosshair on the quartz block', hit == 'minecraft:quartz_block', hit)
    strikes = 0
    for _ in range(10):
        s.io.right_click()
        strikes += 1
        time.sleep(1.4)
        hit = s.st().get('hitBlock')
        if hit != 'minecraft:quartz_block':
            break
        if strikes == 2:
            s.shot('crush-after-2-strikes')
    s.check('the quartz block broke after some real right clicks (HS29-3: 4 strikes)', hit != 'minecraft:quartz_block', (strikes, hit))
    s.shot('crush-broken')
    # the drops lie at the block: step on them and look at the inventory
    s.chat(f'tp @s {bx + 0.5} {by} {bz + 0.5}', 1.5)
    st = s.st()
    got = [v for v in st.get('inventory', []) if v and v.startswith('minecraft:quartz x')]
    s.check('four quartz were collected', got == ['minecraft:quartz x4'], got)
    s.chat('gamemode creative @s', 0.5)


SCENARIOS = {
    'simplehammers': config,
    'simplehammers-area': area,
    'simplehammers-reshape': reshape,
    'simplehammers-crush': crush,
}
