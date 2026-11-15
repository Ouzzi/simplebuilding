"""simplerespawn-3: the Downed screen (Spectate / Inventory / Give up) and the view-only inventory with the REAL mouse.

Scenario for modules/simpleinterfaces/tools/ui_probe.py (reference example for the other uiprobe_<module>.py files).
Flow: survival, '/simplerespawn down @s' (op command, the player is alone) -> DownedScreen -> real click 'Spectate' (screen closes)
-> key E (screen back) -> real click 'Inventory' (view-only inventory, Q drops nothing) -> E -> real click 'Give up' (death screen).
"""
import time


def downed(s):
    s.chat('gamemode survival @s', 0.8)
    s.clear_and_give('minecraft:bread 12', 'minecraft:iron_sword')
    s.chat('simplerespawn down @s', 1.5)
    st = s.wait_screen('DownedScreen', 12)
    s.check('Downed screen opens after /simplerespawn down', 'DownedScreen' in st.get('screen', ''), st.get('screen'))
    labels = s.labels(st)
    s.check('three buttons Spectate / Inventory / Give up', all(any(t in l for l in labels) for t in ('Spectate', 'Inventory', 'Give up')), labels)
    s.shot('downed-screen')
    # real click: Spectate closes the screen and frees the camera
    if s.click_widget('Spectate'):
        st = s.wait_screen('none', 6)
        s.check('real click Spectate closes the screen', st.get('screen') == 'none', st.get('screen'))
        s.shot('spectate')
    # E brings the screen back
    s.key('e', 1.5)
    st = s.wait_screen('DownedScreen', 8)
    s.check('E opens the Downed screen again', 'DownedScreen' in st.get('screen', ''), st.get('screen'))
    # Inventory: view only
    if s.click_widget('Inventory'):
        st = s.wait_screen('ViewOnlyInventoryScreen', 8)
        s.check('real click Inventory opens the view-only inventory', 'ViewOnlyInventoryScreen' in st.get('screen', ''), st.get('screen'))
        before = st.get('inventory', [])[:]
        s.shot('view-only-inventory')
        s.key('q', 0.8)
        st = s.st()
        s.check('Q drops nothing from the view', st.get('inventory', []) == before, st.get('inventory', [])[:9])
        # a real click on a slot must not move anything either
        own = [x for x in st.get('slots', []) if x['own'] and x['stack']]
        if own:
            s.click_xy(own[0]['x'], own[0]['y'], st)
            st = s.st()
            s.check('real click on an item changes nothing (view only)', st.get('inventory', []) == before and not st.get('carried'), st.get('carried'))
    s.key('e', 1.5)
    st = s.wait_screen('DownedScreen', 8)
    s.check('E goes back to the Downed screen', 'DownedScreen' in st.get('screen', ''), st.get('screen'))
    # Give up: Vanilla death screen
    if s.click_widget('Give up'):
        st = s.wait_screen('DeathScreen', 10)
        s.check('real click Give up shows the Vanilla death screen', 'DeathScreen' in st.get('screen', ''), st.get('screen'))
        s.shot('gave-up')
        # Respawn button of the death screen with the real mouse
        if s.click_widget('Respawn'):
            st = s.wait_screen('none', 10)
            s.check('real click Respawn leaves the death screen', st.get('screen') == 'none', st.get('screen'))
    time.sleep(1)


SCENARIOS = {'simplerespawn': downed}
