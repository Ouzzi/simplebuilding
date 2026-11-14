"""simplestorage-6: the five storage windows with the REAL mouse + the worn parts seen from outside
(scenarios for modules/simpleinterfaces/tools/ui_probe.py; pipe window: pipe_probe.py, not repeated here).

Scenario keys use a hyphen (simplestorage-chest), not a slash: ui_probe's shot() writes shots/<scenario>-<name>.png without creating sub folders.
SCENARIOS
  simplestorage            Controller chest (network window): wooden controller + interface + cable + two chests with items; list cells,
                           sort key, search field (real typing, right click clears), take a stack, put it back, shift-click store, shift-click take.
  simplestorage-chest      Mod chest (Reinforced 4 rows; Enderite 6 rows with the x4 stack rule): pick up, place, shift-click, right-click split,
                           close and reopen (contents persist), 128 in one Enderite slot, taking leaves at most one normal stack.
  simplestorage-barrel     Barrel (Copper 3 rows = Vanilla ContainerScreen; Enderite 6 rows x4): same moves.
  simplestorage-table      Storage Crafting Table: grid items STAY on the table when the window closes, recipe 4 planks -> crafting table, recipe book button.
  simplestorage-backpack   Backpack: opens with the backpack KEY (B), worn in the chest slot (Netherite 33 slots, Enderite 50, Reinforced carried 18);
                           put/shift-click items, extra column slot, locked worn slot, recipe book button, B closes, contents persist, Shift tooltip.
  simplestorage-worn       Worn parts from outside: backpacks of every tier, a dyed one, quivers (chest slot) in third person (F5 back + front)
                           and a placed backpack block. Screenshots only (nothing in the status shows the render).

How the windows open (all with the REAL right click on the crosshair block, empty hand, via ui_probe's stage()):
  Controller / Chest / Barrel / Storage Crafting Table: right click on the block. Backpack: key B (BackpackKeyHandler, backpack anywhere on the body
  or in the inventory) with a placed-block fallback (right click on the placed backpack block).
Widgets: Controller: class 'Key' (sort, empty label), EditBox 'Search...'. Backpack/Table: ImageButton (recipe book, empty label). Everything
else is slots; slots are addressed by cslot (container slots) or menu index where cslots clash (backpack: index 46.., table: index 0..9).

Assumptions to check on the first run: wording of the crosshair block id, that `item replace` into the open window syncs within ~1 s, the order the
controller list shows (Amount = descending, Name = ascending), quick-move targets of the backpack (hotbar -> storage in picture order).
Only a human can judge: boxes/colours of the windows, symbols, the render of worn backpack/quiver (shots), tooltip look, the feel of the scrollbar
(drag/wheel needs > 45 kinds: not exercised), double chests (setblock cannot form them: placing is a real-player job).
"""
import time

CARRY = 'carried'


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _xyz(st):
    return [int(v) for v in st['pos'].replace(',', ' ').split()]


def _put(s, slot, stack, wait=0.3):
    s.chat(f'item replace entity @s {slot} with {stack}', wait)


def _stack(st, index):
    for sl in st.get('slots', []):
        if sl['index'] == index:
            return sl['stack']
    return None


def _cont(st):
    """Container (not own) slots in menu order."""
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


def _inv_has(st, text):
    return any(v == text for v in st.get('inventory', []))


def _inv_item(st, prefix):
    return [v for v in st.get('inventory', []) if v and v.startswith(prefix)]


_LAST = {}


def _open(s, block, fragment, dx=2, dy=0, dz=0, state='', seconds=10):
    """Closes whatever is open, places the block next to the player, aims at it, real right click, waits for the screen. Returns the status or None."""
    s.close()
    # an older run's contents must not stay (setblock over the same block keeps its block entity): clear the cell first
    x, y, z = _xyz(s.st())
    s.chat(f'setblock {x + dx} {y + dy} {z + dz} minecraft:air', 0.4)
    _LAST['pos'] = s.stage(block, dx, dy, dz, state=state)
    st = s.st()
    s.check(f'crosshair on {block}', st.get('hitBlock') == block, st.get('hitBlock'))
    s.io.right_click()
    st = s.wait_screen(fragment, seconds)
    ok = fragment in st.get('screen', '')
    s.check(f'real right click on {block} opens {fragment}', ok, st.get('screen'))
    return st if ok else None


def _reopen(s, fragment, seconds=10):
    """Escape, aim at the same block again (closing a window re-grabs the mouse, which can turn the view), real right click."""
    s.close()
    if _LAST.get('pos'):
        s.face(*_LAST['pos'])
    time.sleep(0.4)
    s.io.right_click()
    st = s.wait_screen(fragment, seconds)
    ok = fragment in st.get('screen', '')
    s.check(f'right click again reopens {fragment}', ok, st.get('screen'))
    return st if ok else None


# ---------------------------------------------------------------- chest / barrel (same moves)
def _container_moves(s, tag, shot, slots, mult):
    """Moves in an open container window with slots container slots; mult = stack factor of the tier (1 = Vanilla stacks)."""
    st = s.st()
    cont = _cont(st)
    s.check(f'{tag}: {slots} container slots', len(cont) == slots, len(cont))
    s.check(f'{tag}: 36 inventory slots', sum(1 for x in st['slots'] if x['own']) == 36, sum(1 for x in st['slots'] if x['own']))
    s.shot(f'{shot}-open')
    _put(s, 'hotbar.0', 'minecraft:dirt 20')
    _put(s, 'hotbar.1', 'minecraft:apple 5')
    _put(s, 'hotbar.2', 'minecraft:stone 10')
    st = _wait(s, lambda t: (_own(t, 0) or {}).get('stack') == 'minecraft:dirt x20')
    s.check(f'{tag}: items given while open show in the hotbar slots', (_own(st, 0) or {}).get('stack') == 'minecraft:dirt x20', [x['stack'] for x in st['slots'] if x['own']][27:36])
    # pick up with the real left click, place into container slot 0
    s.click_slot(cslot=0, own=True)
    st = s.st()
    s.check(f'{tag}: left click on the hotbar stack takes it onto the cursor', st.get(CARRY) == 'minecraft:dirt x20', st.get(CARRY))
    s.click_slot(cslot=0, own=False)
    st = _wait(s, lambda t: _stack(t, 0) == 'minecraft:dirt x20')
    s.check(f'{tag}: left click on an empty chest slot places the stack', _stack(st, 0) == 'minecraft:dirt x20' and not st.get(CARRY), (_stack(st, 0), st.get(CARRY)))
    # shift click from the hotbar: the apples go into the container
    s.click_slot(cslot=1, own=True, shift=True)
    st = _wait(s, lambda t: any(x['stack'] == 'minecraft:apple x5' for x in _cont(t)))
    s.check(f'{tag}: shift-click moves the apples into the container', any(x['stack'] == 'minecraft:apple x5' for x in _cont(st)),
            [x['stack'] for x in _cont(st) if x['stack']])
    s.check(f'{tag}: hotbar slot 1 is empty after the shift-click', not (_own(st, 1) or {}).get('stack'), (_own(st, 1) or {}).get('stack'))
    # right click takes half of a stack
    s.click_slot(cslot=2, own=True, right=True)
    st = s.st()
    s.check(f'{tag}: right click takes half (stone x10 -> x5 on the cursor)', st.get(CARRY) == 'minecraft:stone x5', st.get(CARRY))
    s.click_slot(cslot=4, own=False)
    st = _wait(s, lambda t: _stack(t, 4) == 'minecraft:stone x5')
    s.check(f'{tag}: half stack placed into container slot 4', _stack(st, 4) == 'minecraft:stone x5', _stack(st, 4))
    # shift click out of the container back into the inventory
    s.click_slot(cslot=0, own=False, shift=True)
    st = _wait(s, lambda t: not _stack(t, 0))
    s.check(f'{tag}: shift-click on a container slot empties it', not _stack(st, 0), _stack(st, 0))
    s.check(f'{tag}: the dirt arrived in the inventory', len(_inv_item(st, 'minecraft:dirt')) >= 1, st.get('inventory', [])[:12])
    s.shot(f'{shot}-after-moves')


def _stack_rule(s, tag, shot, mult):
    """Enderite x4 rule: two 64 stacks merge to 128 in ONE slot; taking out gives at most one normal stack (64)."""
    _put(s, 'hotbar.3', 'minecraft:cobblestone 64')
    _put(s, 'hotbar.4', 'minecraft:cobblestone 64')
    _wait(s, lambda t: (_own(t, 4) or {}).get('stack') == 'minecraft:cobblestone x64')
    s.click_slot(cslot=3, own=True)
    s.click_slot(cslot=10, own=False)
    s.click_slot(cslot=4, own=True)
    s.click_slot(cslot=10, own=False)
    st = _wait(s, lambda t: _stack(t, 10) == 'minecraft:cobblestone x128', 5)
    s.check(f'{tag}: two stacks of 64 merge to {64 * 2} in one slot (x{mult} tier)', _stack(st, 10) == 'minecraft:cobblestone x128', (_stack(st, 10), st.get(CARRY)))
    s.shot(f'{shot}-merged')
    s.click_slot(cslot=10, own=False)
    st = s.st()
    s.check(f'{tag}: left click takes at most one normal stack (64) onto the cursor', st.get(CARRY) == 'minecraft:cobblestone x64', st.get(CARRY))
    s.click_slot(cslot=10, own=False)
    st = _wait(s, lambda t: not t.get(CARRY))
    s.check(f'{tag}: put back, nothing left on the cursor', not st.get(CARRY), st.get(CARRY))


def chest(s):
    s.chat('clear @s', 0.4)
    # ---- Reinforced chest: 4 rows x 9, Vanilla stacks
    st = _open(s, 'simplestorage:reinforced_chest', 'TieredChestScreen', dx=2)
    if st:
        _container_moves(s, 'reinforced chest', 'chest-reinforced', 36, 1)
        st = _reopen(s, 'TieredChestScreen')
        if st:
            s.check('reinforced chest: apples and stone are still there after closing and reopening',
                    _stack(st, 1) == 'minecraft:apple x5' and _stack(st, 4) == 'minecraft:stone x5', (_stack(st, 1), _stack(st, 4)))
            s.shot('chest-reinforced-reopened')
    s.close()
    # ---- Enderite chest: 6 rows x 9, x4 stacks
    s.chat('clear @s', 0.4)
    st = _open(s, 'simplestorage:enderite_chest', 'TieredChestScreen', dx=-2)
    if st:
        s.check('enderite chest: 54 container slots', len(_cont(st)) == 54, len(_cont(st)))
        s.shot('chest-enderite-open')
        _stack_rule(s, 'enderite chest', 'chest-enderite', 4)
    s.close()


def barrel(s):
    s.chat('clear @s', 0.4)
    st = _open(s, 'simplestorage:copper_barrel', 'ContainerScreen', dx=0, dz=2)
    if st:
        _container_moves(s, 'copper barrel', 'barrel-copper', 27, 1)
        st = _reopen(s, 'ContainerScreen')
        if st:
            s.check('copper barrel: contents persist after reopening', _stack(st, 1) == 'minecraft:apple x5' and _stack(st, 4) == 'minecraft:stone x5',
                    (_stack(st, 1), _stack(st, 4)))
    s.close()
    s.chat('clear @s', 0.4)
    st = _open(s, 'simplestorage:enderite_barrel', 'ContainerScreen', dx=0, dz=-2)
    if st:
        s.check('enderite barrel: 54 container slots', len(_cont(st)) == 54, len(_cont(st)))
        s.shot('barrel-enderite-open')
        _stack_rule(s, 'enderite barrel', 'barrel-enderite', 4)
    s.close()


# ---------------------------------------------------------------- storage crafting table
def table(s):
    s.chat('clear @s', 0.4)
    st = _open(s, 'simplestorage:storage_crafting_table', 'StorageCraftingScreen', dx=2, dz=2)
    if not st:
        return
    # menu: 0 result, 1-9 grid, 10-36 inventory, 37-45 hotbar
    s.check('table: 46 slots (result, 3x3 grid, 36 inventory)', len(st['slots']) == 46, len(st['slots']))
    s.shot('table-open')
    _put(s, 'hotbar.0', 'minecraft:oak_planks 64')
    _wait(s, lambda t: (_own(t, 0) or {}).get('stack') == 'minecraft:oak_planks x64')
    s.click_slot(cslot=0, own=True)
    s.click_slot(index=1, right=True)
    s.click_slot(index=2, right=True)
    st = _wait(s, lambda t: _stack(t, 1) == 'minecraft:oak_planks x1' and _stack(t, 2) == 'minecraft:oak_planks x1')
    s.check('table: right click places one plank per grid slot', _stack(st, 1) == 'minecraft:oak_planks x1' and _stack(st, 2) == 'minecraft:oak_planks x1',
            (_stack(st, 1), _stack(st, 2)))
    s.click_slot(cslot=0, own=True)  # remaining planks back into the hotbar
    s.shot('table-two-planks')
    # the grid belongs to the TABLE: closing returns nothing to the player
    s.close()
    st = s.st()
    s.check('table: closing returned no plank to the inventory (62 left, 2 on the table)',
            sum(int(v.rsplit(' x', 1)[1]) for v in _inv_item(st, 'minecraft:oak_planks')) == 62, _inv_item(st, 'minecraft:oak_planks'))
    st = _reopen(s, 'StorageCraftingScreen')
    if not st:
        return
    s.check('table: the two planks are still in the grid after reopening', _stack(st, 1) == 'minecraft:oak_planks x1' and _stack(st, 2) == 'minecraft:oak_planks x1',
            (_stack(st, 1), _stack(st, 2)))
    s.click_slot(cslot=0, own=True)
    s.click_slot(index=4, right=True)
    s.click_slot(index=5, right=True)
    st = _wait(s, lambda t: _stack(t, 0) == 'minecraft:crafting_table x1', 5)
    s.check('table: 2x2 planks give a crafting table in the result slot', _stack(st, 0) == 'minecraft:crafting_table x1', _stack(st, 0))
    s.click_slot(cslot=0, own=True)  # drop the rest of the planks, the cursor must be empty to take the result
    s.shot('table-recipe')
    s.click_slot(index=0)
    st = s.st()
    s.check('table: clicking the result takes the crafting table', st.get(CARRY) == 'minecraft:crafting_table x1', st.get(CARRY))
    s.click_slot(cslot=8, own=True)
    st = _wait(s, lambda t: _inv_has(t, 'minecraft:crafting_table x1'))
    s.check('table: the crafting table lies in the inventory', _inv_has(st, 'minecraft:crafting_table x1'), st.get('inventory', [])[:9])
    s.check('table: the grid is empty after crafting', not any(_stack(st, i) for i in range(1, 10)), [_stack(st, i) for i in range(1, 10)])
    # recipe book button (ImageButton, empty label): open and close it
    if s.click_widget(cls='ImageButton', name='table recipe book button'):
        time.sleep(0.8)
        s.shot('table-recipe-book')
        s.click_widget(cls='ImageButton', name='table recipe book button (close)')
    s.close()
    s.chat('clear @s', 0.3)


# ---------------------------------------------------------------- controller chest (the network window)
def _controller_net(s):
    """Controller (north of the player) + interface + cable + two interfaces with a chest each. Returns the controller position."""
    x, y, z = _xyz(s.st())
    # fresh area: the contents of an older run must not stay (setblock over the same block keeps its items)
    s.chat(f'fill {x - 3} {y} {z - 6} {x + 3} {y} {z - 1} minecraft:air', 0.6)
    s.chat(f'setblock {x + 2} {y} {z - 4} minecraft:chest', 0.3)
    s.chat(f'setblock {x - 2} {y} {z - 4} minecraft:chest', 0.3)
    s.chat(f'setblock {x} {y} {z - 4} simplestorage:copper_cable', 0.3)
    s.chat(f'setblock {x + 1} {y} {z - 4} simplestorage:chest_interface[facing=east]', 0.3)
    s.chat(f'setblock {x - 1} {y} {z - 4} simplestorage:chest_interface[facing=west]', 0.3)
    s.chat(f'setblock {x} {y} {z - 3} simplestorage:chest_interface[facing=south]', 0.3)
    for slot, stack in ((0, 'minecraft:dirt 64'), (1, 'minecraft:dirt 36'), (2, 'minecraft:cobblestone 30')):
        s.chat(f'item replace block {x + 2} {y} {z - 4} container.{slot} with {stack}', 0.25)
    for slot, stack in ((0, 'minecraft:apple 5'), (1, 'minecraft:oak_log 7')):
        s.chat(f'item replace block {x - 2} {y} {z - 4} container.{slot} with {stack}', 0.25)
    return s.stage('simplestorage:wooden_controller_chest', 0, 0, -2)


def controller(s):
    s.chat('clear @s', 0.4)
    s.chat('time set day', 0.3)
    _controller_net(s)
    st = s.st()
    s.check('crosshair on the controller chest', st.get('hitBlock') == 'simplestorage:wooden_controller_chest', st.get('hitBlock'))
    s.io.right_click()
    st = s.wait_screen('ControllerScreen', 10)
    if 'ControllerScreen' not in st.get('screen', ''):
        s.check('real right click opens the controller window', False, st.get('screen'))
        return
    s.check('real right click opens the controller window', True, st.get('screen'))
    st = _wait(s, lambda t: sum(1 for x in _cont(t) if x['stack']) >= 4, 5)
    cells = _cont(st)
    s.check('controller: 45 list cells + 36 own slots', len(cells) == 45 and len(st['slots']) == 81, (len(cells), len(st['slots'])))
    kinds = [x['stack'] for x in cells if x['stack']]
    want = {'minecraft:dirt x1', 'minecraft:cobblestone x1', 'minecraft:oak_log x1', 'minecraft:apple x1'}
    s.check('controller: the list shows the four kinds of both chests', set(kinds) == want, kinds)
    s.check('controller: default order is by amount (dirt 100 first)', bool(kinds) and kinds[0] == 'minecraft:dirt x1', kinds)
    labels = s.labels(st)
    s.check('controller: sort key (class Key) and search field present', s.find_widget(cls='Key', st=st) is not None and s.find_widget('Search', st=st) is not None, labels)
    s.shot('controller-open')
    # tooltip with the exact total: hover the first cell
    first = next((x for x in cells if x['index'] == 0), None)
    if first:
        s.io.move(*s.abs(st, first['x'], first['y']))
        time.sleep(1.0)
        s.shot('controller-tooltip')
        s.io.move(*s.abs(st, 640, 700))
    # sort key: real click, order switches to by name (apple first)
    if s.click_widget(cls='Key', name='sort key'):
        st = _wait(s, lambda t: (_stack(t, 0) or '') == 'minecraft:apple x1')
        s.check('controller: sort key switches to name order (apple first)', _stack(st, 0) == 'minecraft:apple x1', [x['stack'] for x in _cont(st) if x['stack']])
        s.shot('controller-sort-name')
        s.click_widget(cls='Key', name='sort key (again)')
        st = _wait(s, lambda t: (_stack(t, 0) or '') == 'minecraft:dirt x1')
        s.check('controller: second click is back to amount order', _stack(st, 0) == 'minecraft:dirt x1', [x['stack'] for x in _cont(st) if x['stack']])
    # search: click the field, type, only oak_log stays; right click clears
    if s.click_widget('Search', name='search field'):
        s.type_text('oak')
        st = _wait(s, lambda t: [x['stack'] for x in _cont(t) if x['stack']] == ['minecraft:oak_log x1'])
        shown = [x['stack'] for x in _cont(st) if x['stack']]
        s.check('controller: typing "oak" with the real keyboard filters the list to oak_log', shown == ['minecraft:oak_log x1'], shown)
        w = s.find_widget('Search', st=st)
        s.check('controller: the search field holds the typed text', bool(w) and w.get('value') == 'oak', w and w.get('value'))
        s.shot('controller-search')
        if w:
            s.right_click_xy(w['x'], w['y'], st)
            st = _wait(s, lambda t: sum(1 for x in _cont(t) if x['stack']) == 4)
            shown = [x['stack'] for x in _cont(st) if x['stack']]
            w = s.find_widget('Search', st=st)
            s.check('controller: right click on the search field clears it and shows all four kinds', len(shown) == 4 and bool(w) and w.get('value') == '', (shown, w and w.get('value')))
        # leave the field: Escape would close the window while the field has the focus? keep going by clicking the list instead
    # take a full stack of dirt (cell 0) with the left click, put it into the hotbar
    s.click_slot(index=0)
    st = _wait(s, lambda t: t.get(CARRY) == 'minecraft:dirt x64')
    s.check('controller: left click on the dirt cell takes a full stack (64) onto the cursor', st.get(CARRY) == 'minecraft:dirt x64', st.get(CARRY))
    s.click_slot(cslot=0, own=True)
    st = _wait(s, lambda t: (_own(t, 0) or {}).get('stack') == 'minecraft:dirt x64')
    s.check('controller: the stack lies in hotbar slot 0', (_own(st, 0) or {}).get('stack') == 'minecraft:dirt x64', (_own(st, 0) or {}).get('stack'))
    # put it back: pick it up and click any list cell with the item on the cursor
    s.click_slot(cslot=0, own=True)
    s.click_slot(index=0)
    st = _wait(s, lambda t: not t.get(CARRY))
    s.check('controller: a click on the list with an item on the cursor stores it in the network (cursor empty)', not st.get(CARRY), st.get(CARRY))
    # shift-click in the inventory stores
    _put(s, 'hotbar.1', 'minecraft:stone 30')
    _wait(s, lambda t: (_own(t, 1) or {}).get('stack') == 'minecraft:stone x30')
    s.click_slot(cslot=1, own=True, shift=True)
    st = _wait(s, lambda t: not (_own(t, 1) or {}).get('stack'))
    s.check('controller: shift-click in the inventory stores the stack (hotbar slot empty)', not (_own(st, 1) or {}).get('stack'), (_own(st, 1) or {}).get('stack'))
    st = _wait(s, lambda t: any(x['stack'] == 'minecraft:stone x1' for x in _cont(t)), 6)
    stone = next((x for x in _cont(st) if x['stack'] == 'minecraft:stone x1'), None)
    s.check('controller: the list now has a stone cell (the list refreshes a few times a second)', stone is not None, [x['stack'] for x in _cont(st) if x['stack']])
    s.shot('controller-after-store')
    # shift-click a list cell takes a stack into the inventory
    if stone:
        s.click_slot(index=stone['index'], shift=True)
        st = _wait(s, lambda t: _inv_has(t, 'minecraft:stone x30'), 5)
        s.check('controller: shift-click on the stone cell takes the 30 stone into the inventory', _inv_has(st, 'minecraft:stone x30'), st.get('inventory', [])[:9])
    s.close()
    s.chat('clear @s', 0.3)


# ---------------------------------------------------------------- backpack
def _bp_slots(st, n):
    """Backpack slots: menu index 46 .. 46+n-1 (BackpackMenu.BACKPACK_SLOT_START = 46)."""
    return [x for x in st.get('slots', []) if 46 <= x['index'] < 46 + n and not x['own']]


def _open_backpack(s, tier_item, slots, block_fallback=True):
    """Backpack key B with the backpack worn (chest slot) or carried; fallback: placed block + right click. Returns (status, via_key)."""
    s.close()
    st = s.key('b', 1.2)
    st = s.wait_screen('BackpackScreen', 6)
    if 'BackpackScreen' in st.get('screen', ''):
        s.check(f'key B opens the {tier_item} window', True, st.get('screen'))
        return st, True
    s.check(f'key B opens the {tier_item} window', False, st.get('screen'))
    if not block_fallback:
        return None, False
    st = _open(s, f'simplestorage:{tier_item}', 'BackpackScreen', dx=3)
    return st, False


def backpack(s):
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.4)
    _put(s, 'armor.chest', 'simplestorage:netherite_backpack', 0.6)
    st, via_key = _open_backpack(s, 'netherite_backpack', 33)
    if st:
        slots = _bp_slots(st, 33)
        s.check('netherite backpack: 33 backpack slots (3 rows + 1 extra column of 6)', len(slots) == 33, len(slots))
        s.check('window also holds the inventory (36 own slots)', sum(1 for x in st['slots'] if x['own']) >= 36, sum(1 for x in st['slots'] if x['own']))
        s.shot('backpack-netherite-open')
        _put(s, 'hotbar.0', 'minecraft:apple 10')
        _put(s, 'hotbar.1', 'minecraft:cobblestone 40')
        _put(s, 'hotbar.2', 'minecraft:stick 5')
        st = _wait(s, lambda t: (_own(t, 2) or {}).get('stack') == 'minecraft:stick x5')
        s.click_slot(cslot=0, own=True)
        s.click_slot(index=46)
        st = _wait(s, lambda t: _stack(t, 46) == 'minecraft:apple x10')
        s.check('backpack: pick up, place into backpack slot 0', _stack(st, 46) == 'minecraft:apple x10', _stack(st, 46))
        s.click_slot(cslot=1, own=True, shift=True)
        st = _wait(s, lambda t: not (_own(t, 1) or {}).get('stack'))
        placed = [x['index'] for x in st['slots'] if x['stack'] == 'minecraft:cobblestone x40']
        s.check('backpack: shift-click from the hotbar moves the cobblestone away from hotbar slot 1', not (_own(st, 1) or {}).get('stack'), placed)
        s.check('backpack: the cobblestone sits in the backpack rows (index >= 46) or in the main inventory', bool(placed), placed)
        column = 46 + 27  # first slot of the extra column
        s.click_slot(cslot=2, own=True)
        s.click_slot(index=column)
        st = _wait(s, lambda t: _stack(t, column) == 'minecraft:stick x5')
        s.check('backpack: an item fits the extra column slot (violet)', _stack(st, column) == 'minecraft:stick x5', _stack(st, column))
        # the worn slot is locked: a real click does not take the backpack off
        before = _stack(st, 6)
        s.click_slot(index=6)
        st = s.st()
        s.check('backpack: the worn backpack slot (chest) is locked: nothing lands on the cursor', not st.get(CARRY), (before, st.get(CARRY)))
        s.check('backpack: the worn slot still shows the backpack', _stack(st, 6) == before and bool(before), (before, _stack(st, 6)))
        s.shot('backpack-netherite-filled')
        # B closes the window (BackpackKeyHandler.closesOnBackpackKey)
        st = s.key('b', 1.0)
        st = _wait(s, lambda t: t.get('screen') == 'none', 4)
        s.check('backpack: key B closes the window', st.get('screen') == 'none', st.get('screen'))
        # reopen: contents persisted in the item
        st, _ = _open_backpack(s, 'netherite_backpack', 33)
        if st:
            s.check('backpack: contents persist in the worn backpack (apples, sticks)', _stack(st, 46) == 'minecraft:apple x10' and _stack(st, column) == 'minecraft:stick x5',
                    (_stack(st, 46), _stack(st, column)))
            s.shot('backpack-netherite-reopened')
            # recipe book button (ImageButton): real click, last step of this window (an open book would take typed keys)
            if s.click_widget(cls='ImageButton', name='backpack recipe book button'):
                time.sleep(0.8)
                s.shot('backpack-recipe-book')
    s.close()
    # ---- Shift tooltip of the worn backpack in the survival inventory (contents list)
    st = s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 6)
    if 'InventoryScreen' in st.get('screen', ''):
        sl = next((x for x in st['slots'] if x['index'] == 6), None)
        if sl:
            s.io.key_down('Shift_L')
            s.io.move(*s.abs(st, sl['x'], sl['y']))
            time.sleep(1.2)
            s.shot('backpack-tooltip-shift')
            s.io.key_up('Shift_L')
        else:
            s.check('inventory: chest armor slot present (tooltip of the worn backpack)', False, len(st['slots']))
    else:
        s.check('inventory opens with E (survival) for the backpack tooltip', False, st.get('screen'))
    s.close()
    # ---- Enderite backpack worn: 50 slots (2 extra columns)
    _put(s, 'armor.chest', 'simplestorage:enderite_backpack', 0.6)
    st, _ = _open_backpack(s, 'enderite_backpack', 50)
    if st:
        s.check('enderite backpack: 50 backpack slots (4 rows + 2 columns of 7)', len(_bp_slots(st, 50)) == 50, len(_bp_slots(st, 50)))
        s.shot('backpack-enderite-open')
    s.close()
    # ---- Reinforced backpack only CARRIED in the inventory (not worn): B still opens it
    s.chat('clear @s', 0.4)
    _put(s, 'hotbar.5', 'simplestorage:reinforced_backpack', 0.6)
    st, _ = _open_backpack(s, 'reinforced_backpack', 18, block_fallback=False)
    if st:
        s.check('reinforced backpack carried: 18 backpack slots (2 rows, no column)', len(_bp_slots(st, 18)) == 18, len(_bp_slots(st, 18)))
        s.shot('backpack-reinforced-carried')
    s.close()
    s.chat('clear @s', 0.3)
    s.chat('gamemode creative @s', 0.5)


# ---------------------------------------------------------------- worn parts from outside (shots only)
def worn(s):
    """Backpacks and quivers on the body in third person (F5 = back view, F5 again = front view, F5 again = first person).

    Nothing in the status shows a render, so the only checks are 'the key presses did not open a screen'; a human looks at the shots.
    """
    s.chat('clear @s', 0.4)
    s.chat('time set day', 0.3)
    # placed backpack block in first person (the placed model and its tint)
    st = s.stage('simplestorage:netherite_backpack', 2, 0, 0)
    time.sleep(0.6)
    s.shot('placed-backpack')
    s.chat('setblock ~2 ~ ~ minecraft:air', 0.4)
    # look at the horizon, third person from behind
    x, y, z = _xyz(s.st())
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 180 0', 0.8)
    wear = [
        ('backpack', 'simplestorage:backpack'),
        ('reinforced-backpack', 'simplestorage:reinforced_backpack'),
        ('netherite-backpack', 'simplestorage:netherite_backpack'),
        ('enderite-backpack', 'simplestorage:enderite_backpack'),
        ('dyed-backpack', 'simplestorage:backpack[dyed_color=16711680]'),
        ('quiver', 'simplestorage:quiver'),
        ('netherite-quiver', 'simplestorage:netherite_quiver'),
        ('enderite-quiver', 'simplestorage:enderite_quiver'),
    ]
    st = s.key('F5', 1.5)  # first person -> third person, back view
    s.check('F5 leaves no screen open', st.get('screen') == 'none', st.get('screen'))
    for name, item in wear:
        _put(s, 'armor.chest', item, 0.6)
        time.sleep(0.8)
        s.shot(f'back-{name}')
    # front view of the last (quiver) and of a backpack
    st = s.key('F5', 1.5)
    s.shot('front-enderite-quiver')
    _put(s, 'armor.chest', 'simplestorage:netherite_backpack', 0.6)
    time.sleep(0.8)
    s.shot('front-netherite-backpack')
    st = s.key('F5', 1.2)  # back to first person
    s.check('F5 cycle ends in first person without a screen', st.get('screen') == 'none', st.get('screen'))
    s.chat('clear @s', 0.3)


SCENARIOS = {
    'simplestorage': controller,
    'simplestorage-chest': chest,
    'simplestorage-barrel': barrel,
    'simplestorage-table': table,
    'simplestorage-backpack': backpack,
    'simplestorage-worn': worn,
}
