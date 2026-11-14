"""simplebuilding-9: the windows of the SimpleBuilding CORE with the REAL mouse (scenarios for modules/simpleinterfaces/tools/ui_probe.py).

The core has no modules/simplebuilding directory, so its scenario file lives here (key 'simplebuilding-core'). The plan point "Echt-Maus-Probe fuer
18 Kern-Fenster (Gewoelbe, Handbuch, Config, HUD ...)" has no list anywhere in the repo; the windows below are the ones the CORE itself owns and a
player can reach on 26.3 (the other old core windows moved out: octant / wand / blueprint -> modules/simplebuildingtools/tools/mouse_probe.py,
hoppers / smither / crafter -> simplemachines (machines_probe.py), chests / backpack -> simplestorage (uiprobe_simplestorage.py), shops, trims, ...).

Scenario keys use a hyphen (simplestorage-chest), not a slash: ui_probe's shot() writes shots/<scenario>-<name>.png without creating sub folders.
SCENARIOS
  simplebuilding-core           Astral Vault (54 slots: first 27 shared with the ender chest) incl. the ender chest check, Smithing Table with the
                                recipe book button (RecipeBookSmithingScreen) and a real smithing (Netherite upgrade), Survival Inventory with the
                                extra head slot (index 46) incl. a trimmed armor piece for the resonance panel (shot).
  simplebuilding-core-guide     Beginner's Guide (item use) and Simple Modpack guide: page turn with Right/D keys, Home, mouse wheel, Backspace, right
                                click = back. The page content is drawn (no widgets): checks compare the screenshots of the steps.
  simplebuilding-core-creative  Creative inventory (E in creative): tab page buttons "<" / ">" when present, shots of the first pages.
  simplebuilding-core-config    SimpleBuilding's own settings window (Cloth AutoConfig) through Mod Menu: Escape -> Mods -> search "simple building" ->
                                first row -> Configure... -> every tab clicked (Cloth tab buttons), nothing saved.

NOT reachable with the real mouse (needs a Java hook, see the report): the Mod HUD toggle (key.simplebuilding.toggle_hud has no default key, the
Controls screen would have to be driven first), the octant / wand windows (key K, now Simple Building Tools: mouse_probe.py), the trim reference screen
(on the main line the Smithing screen carries the recipe book instead of the cycling trim button: McVersion.SMITHING_RECIPE_BOOK).

Assumptions to check on the first run: ids simplebuilding:astral_vault / guide_book, simplelib:modpack_guide; slot order of the SmithingMenu (0 template,
1 base, 2 material, 3 result); the extra head slot is menu index 46 of the inventory; Cloth tab buttons are widgets whose class name contains "Tab".
Only a human can judge: look of the vault (dark astral rows), the guide pages (text, pictures, bookmarks), the creative tabs layout (CL-2), the resonance
panel, the trimmed armor, contrast and wording of the config tabs.
"""
import hashlib
import re
import time


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _xyz(st):
    return [int(v) for v in st['pos'].replace(',', ' ').split()]


def _plain(label):
    return re.sub('§.', '', label or '')


def _stack(st, index):
    for sl in st.get('slots', []):
        if sl['index'] == index:
            return sl['stack']
    return None


def _cont(st):
    return [x for x in st.get('slots', []) if not x['own']]


def _own(st, cslot):
    return next((x for x in st.get('slots', []) if x['own'] and x['cslot'] == cslot), None)


def _wait(s, pred, seconds=4.0):
    end = time.time() + seconds
    st = s.st()
    while not pred(st) and time.time() < end:
        time.sleep(0.4)
        st = s.st()
    return st


def _wait_other(s, fragment, seconds=10.0):
    return _wait(s, lambda t: t.get('screen', 'none') != 'none' and fragment not in t.get('screen', ''), seconds)


def _put(s, slot, stack, wait=0.3):
    s.chat(f'item replace entity @s {slot} with {stack}', wait)


_LAST = {}


def _open(s, block, fragment, dx=2, dy=0, dz=0, seconds=10):
    s.close()
    x, y, z = _xyz(s.st())
    s.chat(f'setblock {x + dx} {y + dy} {z + dz} minecraft:air', 0.4)  # a fresh block entity (no contents of an older run)
    _LAST['pos'] = s.stage(block, dx, dy, dz)
    st = s.st()
    s.check(f'crosshair on {block}', st.get('hitBlock') == block, st.get('hitBlock'))
    s.io.right_click()
    st = s.wait_screen(fragment, seconds)
    ok = fragment in st.get('screen', '')
    s.check(f'real right click on {block} opens {fragment}', ok, st.get('screen'))
    return st if ok else None


def _reopen(s, fragment, seconds=10):
    s.close()
    if _LAST.get('pos'):
        s.face(*_LAST['pos'])
    time.sleep(0.4)
    s.io.right_click()
    st = s.wait_screen(fragment, seconds)
    ok = fragment in st.get('screen', '')
    s.check(f'right click again reopens {fragment}', ok, st.get('screen'))
    return st if ok else None


def _png(s, name):
    p = s.out / 'shots' / f'{s.scenario}-{name}.png'
    return hashlib.sha1(p.read_bytes()).hexdigest() if p.is_file() else None


# ---------------------------------------------------------------- Astral Vault, Smithing Table, Inventory
def _vault(s):
    s.chat('clear @s', 0.4)
    st = _open(s, 'simplebuilding:astral_vault', 'ContainerScreen', dx=2, dz=0)
    if not st:
        return
    s.check('vault: 54 container slots (6 rows: 27 shared with the ender chest + 27 astral)', len(_cont(st)) == 54, len(_cont(st)))
    s.check('vault: 36 inventory slots', sum(1 for x in st['slots'] if x['own']) == 36, sum(1 for x in st['slots'] if x['own']))
    s.shot('vault-open')
    _put(s, 'hotbar.0', 'minecraft:dirt 5')
    _put(s, 'hotbar.1', 'minecraft:apple 10')
    _wait(s, lambda t: (_own(t, 1) or {}).get('stack') == 'minecraft:apple x10')
    s.click_slot(cslot=0, own=True)
    s.click_slot(cslot=0, own=False)          # shared part
    s.click_slot(cslot=1, own=True)
    s.click_slot(cslot=30, own=False)         # astral part (only the vault has it)
    st = _wait(s, lambda t: _stack(t, 0) == 'minecraft:dirt x5' and _stack(t, 30) == 'minecraft:apple x10')
    s.check('vault: dirt in a shared slot, apples in an astral slot', _stack(st, 0) == 'minecraft:dirt x5' and _stack(st, 30) == 'minecraft:apple x10', (_stack(st, 0), _stack(st, 30)))
    s.shot('vault-filled')
    st = _reopen(s, 'ContainerScreen')
    if st:
        s.check('vault: contents persist after closing and reopening', _stack(st, 0) == 'minecraft:dirt x5' and _stack(st, 30) == 'minecraft:apple x10', (_stack(st, 0), _stack(st, 30)))
    s.close()
    # the first 27 slots are the ender chest: open a real one beside it
    st = _open(s, 'minecraft:ender_chest', 'ContainerScreen', dx=-2, dz=0)
    if st:
        s.check('ender chest: 27 slots, slot 0 holds the shared dirt, the astral apples are not there',
                len(_cont(st)) == 27 and _stack(st, 0) == 'minecraft:dirt x5' and not any(x['stack'] == 'minecraft:apple x10' for x in _cont(st)),
                (len(_cont(st)), _stack(st, 0)))
        s.shot('ender-chest-shared')
        s.click_slot(cslot=0, own=False, shift=True)   # take the dirt back: leaves no stock in the player's ender chest
    s.close()
    st = _open(s, 'simplebuilding:astral_vault', 'ContainerScreen', dx=2, dz=0)
    if st:
        s.click_slot(cslot=30, own=False, shift=True)  # clean up the astral part too (real shift-click)
        st = _wait(s, lambda t: not _stack(t, 30) and not _stack(t, 0))
        s.check('vault: shared slot 0 is empty (taken through the ender chest) and the astral apples are taken out', not _stack(st, 0) and not _stack(st, 30),
                (_stack(st, 0), _stack(st, 30)))
    s.close()


def _smithing(s):
    s.chat('clear @s', 0.4)
    st = _open(s, 'minecraft:smithing_table', 'SmithingScreen', dx=0, dz=2)
    if not st:
        return
    labels = s.labels(st)
    s.check('smithing: the screen is the recipe-book version (an ImageButton above the material slot)', s.find_widget(cls='ImageButton', st=st) is not None, labels)
    s.shot('smithing-open')
    if s.click_widget(cls='ImageButton', name='smithing recipe book button'):
        time.sleep(1.0)
        s.shot('smithing-recipe-book')
        s.click_widget(cls='ImageButton', name='smithing recipe book button (close)')
        time.sleep(0.6)
    # a real smithing: template, base, material by real clicks; result by a real click
    _put(s, 'hotbar.0', 'minecraft:netherite_upgrade_smithing_template')
    _put(s, 'hotbar.1', 'minecraft:diamond_pickaxe')
    _put(s, 'hotbar.2', 'minecraft:netherite_ingot')
    _wait(s, lambda t: (_own(t, 2) or {}).get('stack') == 'minecraft:netherite_ingot x1')
    for cslot, target in ((0, 0), (1, 1), (2, 2)):
        s.click_slot(cslot=cslot, own=True)
        s.click_slot(index=target)
    st = _wait(s, lambda t: _stack(t, 3) == 'minecraft:netherite_pickaxe x1', 5)
    s.check('smithing: template + diamond pickaxe + ingot give a Netherite pickaxe in the result slot (menu 0-2 inputs, 3 result)',
            _stack(st, 3) == 'minecraft:netherite_pickaxe x1', [_stack(st, i) for i in range(4)])
    s.shot('smithing-result')
    s.click_slot(index=3)
    st = s.st()
    s.check('smithing: a click on the result takes the pickaxe onto the cursor', st.get('carried') == 'minecraft:netherite_pickaxe x1', st.get('carried'))
    s.click_slot(cslot=8, own=True)
    st = _wait(s, lambda t: 'minecraft:netherite_pickaxe x1' in t.get('inventory', []))
    s.check('smithing: the pickaxe lies in the inventory', 'minecraft:netherite_pickaxe x1' in st.get('inventory', []), st.get('inventory', [])[:9])
    s.close()
    s.chat('clear @s', 0.3)


def _inventory(s):
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.4)
    _put(s, 'hotbar.0', 'minecraft:carved_pumpkin')
    _put(s, 'hotbar.1', 'minecraft:zombie_head')
    _put(s, 'armor.chest', 'minecraft:iron_chestplate[trim={material:"minecraft:gold",pattern:"minecraft:wild"}]', 0.6)
    s.close()
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 6)
    s.check('survival: E opens the inventory', 'InventoryScreen' in st.get('screen', ''), st.get('screen'))
    if 'InventoryScreen' not in st.get('screen', ''):
        s.chat('gamemode creative @s', 0.5)
        return
    s.shot('inventory-open')
    extra = next((x for x in st['slots'] if x['index'] == 46), None)
    s.check('inventory: the extra head slot is menu slot 46 (after the off hand)', extra is not None, len(st['slots']))
    if extra:
        s.click_slot(cslot=0, own=True)
        s.click_slot(index=46)
        st = _wait(s, lambda t: _stack(t, 46) == 'minecraft:carved_pumpkin x1')
        s.check('inventory: a carved pumpkin can be put into the extra head slot with real clicks', _stack(st, 46) == 'minecraft:carved_pumpkin x1', _stack(st, 46))
        s.shot('inventory-extra-head-pumpkin')
        s.click_slot(index=46, shift=True)
        st = _wait(s, lambda t: not _stack(t, 46))
        s.check('inventory: shift-click takes it out again', not _stack(st, 46), _stack(st, 46))
        s.click_slot(cslot=1, own=True, shift=True)
        st = _wait(s, lambda t: bool(_stack(t, 46)) or bool(_stack(t, 5)))
        where = 'extra head slot (46)' if _stack(st, 46) else 'armor head slot (5)' if _stack(st, 5) else 'nowhere'
        s.check('inventory: shift-click of a mob head from the hotbar lands in a head slot', where != 'nowhere', where)
        s.shot('inventory-extra-head-mobhead')
    s.close()
    s.chat('clear @s', 0.3)
    s.chat('gamemode creative @s', 0.5)


def core(s):
    _vault(s)
    _smithing(s)
    _inventory(s)


# ---------------------------------------------------------------- guides
def _guide_walk(s, tag):
    """Keys and wheel in an open guide screen: only 'still a guide screen' is readable, the pictures are compared."""
    st = s.st()
    s.check(f'{tag}: opened', 'Guide' in st.get('screen', ''), st.get('screen'))
    if 'Guide' not in st.get('screen', ''):
        return
    s.shot(f'{tag}-p0')
    s.key('Right', 0.9)
    s.shot(f'{tag}-p1')
    st = s.key('d', 0.9)
    s.shot(f'{tag}-p2')
    s.check(f'{tag}: Right and D keep the screen open', 'Guide' in st.get('screen', ''), st.get('screen'))
    a, b, c = _png(s, f'{tag}-p0'), _png(s, f'{tag}-p1'), _png(s, f'{tag}-p2')
    s.check(f'{tag}: the Right key turned the page (picture changed)', bool(a) and bool(b) and a != b, (a, b))
    s.check(f'{tag}: D turned another page (picture changed again)', bool(b) and bool(c) and b != c, (b, c))
    st = s.wheel(640, 360, 2, down=False)
    s.shot(f'{tag}-wheel-up')
    s.check(f'{tag}: wheel up turns pages back (picture differs from the page before)', _png(s, f'{tag}-wheel-up') != c, (_png(s, f'{tag}-wheel-up'), c))
    s.key('Home', 0.9)
    s.shot(f'{tag}-home')
    s.check(f'{tag}: Home keeps the screen open (contents page: look at the shot)', 'Guide' in s.st().get('screen', ''), s.st().get('screen'))
    s.key('Right', 0.9)
    s.key('Right', 0.9)
    s.key('BackSpace', 0.9)
    s.shot(f'{tag}-backspace')
    s.right_click_xy(640, 360)   # right mouse button = back
    time.sleep(0.8)
    s.check(f'{tag}: right click (back) keeps the screen open', 'Guide' in s.st().get('screen', ''), s.st().get('screen'))
    s.close()
    st = s.st()
    s.check(f'{tag}: Escape closes the guide', st.get('screen') == 'none', st.get('screen'))


def _look_up(s):
    x, y, z = _xyz(s.st())
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 -50', 0.8)
    time.sleep(0.4)


def guide(s):
    s.chat('clear @s', 0.4)
    # ---- Beginner's Guide (SimpleBuilding): item use in the air
    s.chat('item replace entity @s weapon.mainhand with simplebuilding:guide_book', 0.5)
    _look_up(s)
    s.io.right_click()
    st = s.wait_screen('Guide', 8)
    _guide_walk(s, 'beginner')
    # ---- Simple Modpack guide (SimpleLib): item use in the air, SimpleBuilding section among its bookmarks
    s.chat('clear @s', 0.4)
    s.chat('item replace entity @s weapon.mainhand with simplelib:modpack_guide', 0.5)
    _look_up(s)
    s.io.right_click()
    st = s.wait_screen('Guide', 8)
    _guide_walk(s, 'modpack')
    s.chat('clear @s', 0.3)


# ---------------------------------------------------------------- creative inventory
def creative(s):
    s.close()
    s.chat('gamemode creative @s', 0.5)
    st = s.key('e', 1.2)
    st = s.wait_screen('CreativeModeInventoryScreen', 6)
    s.check('creative: E opens the creative inventory', 'CreativeModeInventoryScreen' in st.get('screen', ''), st.get('screen'))
    if 'CreativeModeInventoryScreen' not in st.get('screen', ''):
        return
    s.shot('creative-page-1')
    pages = [w for w in s.widgets(st) if _plain(w['label']) in ('<', '>')]
    s.check('creative: tab page buttons (informational: absent when all tabs fit one page)', True, [(w['class'], _plain(w['label'])) for w in pages] or 'none')
    nxt = next((w for w in pages if _plain(w['label']) == '>'), None)
    for n in (2, 3, 4):
        if not nxt:
            break
        s.click_xy(nxt['x'], nxt['y'], st)
        time.sleep(0.9)
        st = s.st()
        s.shot(f'creative-page-{n}')
        nxt = next((w for w in s.widgets(st) if _plain(w['label']) == '>' and w.get('active', True)), None)
    # the survival inventory tab and back (real click at the reported tab position)
    st = s.st()
    if 'invTabX' in st:
        s.click_xy(st['invTabX'], st['invTabY'], st)
        time.sleep(0.8)
        st = s.st()
        s.check('creative: a real click on the Survival Inventory tab opens it', st.get('inventoryTab') is True, st.get('inventoryTab'))
        s.shot('creative-survival-tab')
    s.close()


# ---------------------------------------------------------------- SimpleBuilding's own config window
def _open_config(s, query, tag):
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
    st = _open_config(s, 'simple building', 'core config')
    if not st:
        return
    time.sleep(0.8)
    st = s.st()
    s.check('core config: the Cloth Config screen', 'ClothConfig' in st.get('screen', ''), st.get('screen'))
    s.shot('config-first-tab')
    labels = [_plain(l) for l in s.labels(st)]
    s.check('core config: Save and Cancel buttons', any('Save' in l for l in labels) and any(('Cancel' in l or 'Discard' in l) for l in labels), labels[:12])
    tabs = [w for w in s.widgets(st) if 'Tab' in w['class']]
    s.check('core config: category tabs found (class name contains "Tab")', len(tabs) >= 1, [(w['class'], _plain(w['label'])) for w in s.widgets(st)][:14])
    for i, tab in enumerate(tabs[:5]):
        s.click_xy(tab['x'], tab['y'], st)
        time.sleep(0.9)
        st = s.st()
        s.shot(f'config-tab-{i + 1}-{re.sub("[^a-z0-9]+", "-", _plain(tab["label"]).lower()).strip("-")}')
        s.check(f'core config: tab "{_plain(tab["label"])}" clicked, screen still open', 'ClothConfig' in st.get('screen', ''), st.get('screen'))
    s.close()
    st = s.st()
    s.check('core config: Escape leaves the config screens back to the game', st.get('screen') == 'none', st.get('screen'))


SCENARIOS = {
    'simplebuilding-core': core,
    'simplebuilding-core-guide': guide,
    'simplebuilding-core-creative': creative,
    'simplebuilding-core-config': config,
}
