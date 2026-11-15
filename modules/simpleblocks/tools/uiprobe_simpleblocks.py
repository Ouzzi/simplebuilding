"""simpleblocks-5: settings window and the client scenes of Simple Blocks with the REAL mouse (scenarios for modules/simpleinterfaces/tools/ui_probe.py).
The Shapecutter window has its own probe: saw_probe.py (not repeated here).

Simple Blocks has NO window of its own besides the Shapecutter. Its "Einstellungsfenster" is SimpleLib's generic config screen (L13,
GenericConfigScreen; SnowConfig is announced with ConfigCommands, the groups snow / pale / collapsed become the three tabs). A player reaches
it ONLY through Mod Menu: Escape (pause menu) -> "Mods" -> Mod Menu list -> search "simple blocks" -> click the mod -> "Configure..." ->
Cloth Config screen (the integration client has Mod Menu and Cloth). No command, no key. If a step cannot be found the check detail lists the labels.

Scenario keys use a hyphen (simplestorage-chest), not a slash: ui_probe's shot() writes shots/<scenario>-<name>.png without creating sub folders.
SCENARIOS
  simpleblocks         the settings window: pause menu -> Mods -> search -> mod -> Configure... -> tabs "Snow" / "Pale Blocks" / "Collapsed Blocks" clicked
                       with the real mouse (switch/number counts per tab: 3 switches + 2 numbers / 1 switch + 5 numbers / 1 switch), Frost Grass
                       switch flipped twice (edit marker appears and disappears); nothing is saved.
  simpleblocks-scenes  snowlogging with a REAL right click (snow layer on a fence -> Snowlogged block, second click adds a layer), a row of the five
                       Pale chains over their Vanilla blocks, a frost grass block under snow next to normal grass, and Collapsed blocks carried in
                       1 / 3 / 9 stacks (slot alert on the hotbar and in the inventory screen). Screenshots; checks on the block under the crosshair.

Assumptions to check on the first run: label "Mods" of the pause menu button, row 0 of the Mod Menu list is Simple Blocks (alphabetical, "Simple Blocks"
before "Simple Building"), "Configure..." sprite button label, Cloth tab buttons carry the category names, toggles read "Yes"/"No", the double fields show
their value text (Pale chances 0.0569). Block ids: simpleblocks:snowlogged / frost_grass / pale_* / collapsed_*.
Only a human can judge: the look of snow, frost grass, pale stages and collapsed textures, the slot alert (red beam) and the slowness feel (shots only),
the scene colours; the worldgen of snow cannot be provoked quickly (random ticks) and is not exercised.
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
    return _wait(s, lambda t: t.get('screen', 'none') != 'none' and fragment not in t.get('screen', ''), seconds)


def _toggles(s, st):
    return [w for w in s.widgets(st) if _plain(w['label']) in ('Yes', 'No')]


def _values(s, st):
    return [w['value'] for w in s.widgets(st) if w.get('value') is not None]


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
    scale = st['scale']
    s.click_xy(lw['x'] - lw['w'] / 2 + lw['w'] * 0.45, lw['y'] - lw['h'] / 2 + (4 + 18) * scale, st)  # row 0: top + 4 px + half of 36 px
    time.sleep(0.8)
    s.shot('config-modmenu-selected')
    if not s.click_widget('Configure', name='Mod Menu button "Configure..."'):
        return None
    st = _wait_other(s, 'ModsScreen', 12)
    ok = st.get('screen', 'none') != 'none' and 'ModsScreen' not in st.get('screen', '')
    s.check(f'{tag}: Configure... opens a config screen', ok, st.get('screen'))
    return st if ok else None


def _tab(s, name):
    """Real click on the Cloth category tab whose label contains name; returns the new status or None."""
    st = s.click_widget(name, name=f'config tab "{name}"')
    if st:
        time.sleep(0.8)
        return s.st()
    return None


def config(s):
    st = _open_config(s, 'simple blocks', 'blocks config')
    if not st:
        return
    time.sleep(0.8)
    st = s.st()
    s.check('config screen is the Cloth Config screen', 'ClothConfig' in st.get('screen', ''), st.get('screen'))
    labels = [_plain(l) for l in s.labels(st)]
    s.check('three category tabs: Snow, Pale Blocks, Collapsed Blocks', all(any(t in l for l in labels) for t in ('Snow', 'Pale', 'Collapsed')), labels)
    s.shot('config-snow')
    tg, vals = [_plain(w['label']) for w in _toggles(s, st)], sorted(_values(s, st))
    s.check('Snow tab: three switches (Yes, Yes, No = frost grass off) and the two chances 3 and 6', tg == ['Yes', 'Yes', 'No'] and vals == ['3', '6'], (tg, vals))
    # frost grass switch (third): flip and flip back, nothing saved
    t = _toggles(s, st)
    if len(t) == 3:
        s.click_xy(t[2]['x'], t[2]['y'], st)
        time.sleep(0.6)
        st = s.st()
        now = [_plain(w['label']) for w in _toggles(s, st)]
        s.check('real click flips Frost Grass to "Yes"', now == ['Yes', 'Yes', 'Yes'], now)
        s.check('an edit marker appears: the Cancel button reads "Discard Changes"', any('Discard' in _plain(l) for l in s.labels(st)), [_plain(l) for l in s.labels(st)])
        s.shot('config-snow-edited')
        t = _toggles(s, st)
        s.click_xy(t[2]['x'], t[2]['y'], st)
        time.sleep(0.6)
        back = [_plain(w['label']) for w in _toggles(s, s.st())]
        s.check('second click flips it back to "No" (nothing is saved)', back == ['Yes', 'Yes', 'No'], back)
    # tab Pale Blocks
    st = _tab(s, 'Pale')
    if st:
        tg, vals = [_plain(w['label']) for w in _toggles(s, st)], _values(s, st)
        s.check('Pale tab: one switch (aging on) and five chances (pumpkin, carved, jack o lantern, melon, hay)', tg == ['Yes'] and len(vals) == 5, (tg, vals))
        s.shot('config-pale')
    # tab Collapsed Blocks
    st = _tab(s, 'Collapsed')
    if st:
        tg, vals = [_plain(w['label']) for w in _toggles(s, st)], _values(s, st)
        s.check('Collapsed tab: one switch (slowness on), no numbers', tg == ['Yes'] and vals == [], (tg, vals))
        s.shot('config-collapsed')
    # back to Snow: the tab switch keeps working
    st = _tab(s, 'Snow')
    if st:
        s.check('Snow tab again shows its three switches', len(_toggles(s, st)) == 3, [_plain(w['label']) for w in _toggles(s, st)])
    s.close()
    st = s.st()
    s.check('Escape leaves the config screens back to the game', st.get('screen') == 'none', st.get('screen'))


# ---------------------------------------------------------------- scenes
def _hit(s, bx, by, bz):
    s.face(bx, by, bz)
    return s.st().get('hitBlock')


def scenes(s):
    s.chat('clear @s', 0.4)
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    x, y, z = _xyz(s.st())
    # ---- snowlogging with the real right click: snow layer on a fence
    s.chat(f'setblock {x + 2} {y} {z - 3} minecraft:oak_fence', 0.4)
    s.chat('item replace entity @s weapon.mainhand with minecraft:snow 16', 0.5)
    hit = _hit(s, x + 2, y, z - 3)
    s.check('crosshair on the fence', hit == 'minecraft:oak_fence', hit)
    s.io.right_click()
    time.sleep(0.6)
    st = _wait(s, lambda t: t.get('hitBlock') == 'simpleblocks:snowlogged', 4)
    s.check('real right click with a snow layer on a fence makes a Snowlogged block', st.get('hitBlock') == 'simpleblocks:snowlogged', st.get('hitBlock'))
    s.shot('snowlogged-fence')
    s.io.right_click()
    time.sleep(0.8)
    s.shot('snowlogged-fence-2-layers')
    # ---- the five Pale chains over their Vanilla blocks (column z-2 .. z+2 at x+4: 4 blocks from the player, inside the reach)
    vanilla = ['minecraft:pumpkin', 'minecraft:carved_pumpkin[facing=west]', 'minecraft:jack_o_lantern[facing=west]', 'minecraft:melon', 'minecraft:hay_block']
    pale = ['simpleblocks:pale_pumpkin', 'simpleblocks:pale_carved_pumpkin[facing=west]', 'simpleblocks:pale_jack_o_lantern[facing=west]',
            'simpleblocks:pale_melon', 'simpleblocks:pale_hay_block']
    for i, (v, p) in enumerate(zip(vanilla, pale)):
        s.chat(f'setblock {x + 4} {y} {z - 2 + i} {v}', 0.25)
        s.chat(f'setblock {x + 4} {y + 1} {z - 2 + i} {p}', 0.25)
    hit = _hit(s, x + 4, y + 1, z)
    s.check('crosshair on the middle Pale block (jack o lantern)', hit == 'simpleblocks:pale_jack_o_lantern', hit)
    time.sleep(0.6)
    s.shot('pale-row-over-vanilla')
    # ---- frost grass under snow beside normal grass (placed directly, the default of the switch is off), west of the player
    s.chat(f'setblock {x - 3} {y - 1} {z - 1} minecraft:grass_block', 0.25)
    s.chat(f'setblock {x - 3} {y} {z - 1} minecraft:snow', 0.25)
    s.chat(f'setblock {x - 3} {y - 1} {z + 1} simpleblocks:frost_grass', 0.25)
    s.chat(f'setblock {x - 3} {y} {z + 1} minecraft:snow', 0.25)
    hit = _hit(s, x - 3, y, z + 1)
    s.shot('frost-grass-next-to-grass')
    s.check('frost grass scene placed (crosshair on the snow layer or the frost grass)', hit in ('simpleblocks:frost_grass', 'minecraft:snow'), hit)
    # ---- Collapsed blocks: Slowness I / II / III by stacks (1 / 2 / 9), slot alert on the hotbar; survival so the effect acts
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.4)
    collapsed = ['dirt', 'stone', 'sand', 'gravel', 'clay', 'granite', 'tuff', 'andesite', 'diorite']
    for n, steps in ((1, 'one stack (Slowness I)'), (3, 'three stacks (Slowness II)'), (9, 'nine stacks (Slowness III)')):
        s.chat('clear @s', 0.3)
        for i in range(n):
            s.chat(f'item replace entity @s hotbar.{i} with simpleblocks:collapsed_{collapsed[i]} 64', 0.2)
        time.sleep(2.5)  # the effect is refreshed once a second
        st = s.st()
        have = [v for v in st.get('inventory', [])[:9] if v and 'collapsed_' in v]
        s.check(f'collapsed: {steps} in the hotbar', len(have) == n, have)
        s.shot(f'collapsed-{n}-stacks-hud')
    # inventory screen with 9 stacks: the red beams show on the slots too
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 6)
    s.check('survival inventory opens with E (collapsed slot alert view)', 'InventoryScreen' in st.get('screen', ''), st.get('screen'))
    time.sleep(0.8)
    s.shot('collapsed-9-stacks-inventory')
    s.close()
    s.chat('clear @s', 0.3)
    s.chat('gamemode creative @s', 0.5)


SCENARIOS = {
    'simpleblocks': config,
    'simpleblocks-scenes': scenes,
}
