"""simpleshops-5: owner, buyer and trade-terminal windows with the REAL mouse (scenarios for modules/simpleinterfaces/tools/ui_probe.py).

SCENARIOS
  simpleshops           real placement of a Sales Block + Display Case (owner = the player), owner screen (goods row, price row by real
                        left/right/empty-hand clicks, music disc slot), the developer buyer view of the own shop (real pay + Trade +
                        shift-Trade), back to the owner (earnings), Sales Storage (sneak + right click) and its screen.
  simpleshops/terminal  Trade Terminal: tabs, own shop (Public/Private, Collect), Browse (sort, stock filter, search by real typing),
                        buying from an operator server shop through the terminal (pay slots + Trade).

How the screens open (all with the real right click on the crosshair block, empty hand):
  Sales Block / Display Case (owner, case on top)  -> ShopScreens$Owner  (combined "Shop" menu, 222 px high)
  Sales Block with the case of a stranger / server shop -> ShopScreens$Buyer (here: the owner's developer button "Developer: Buyer view")
  Sales Storage (own)                              -> ShopScreens$Storage
  Trade Terminal block                             -> TradeTerminalScreen
The sales block must have an owner, so it is PLACED with a real right click (setblock would leave it ownerless); the Display Case too.
Server shop (operator command /simpleshops servershop ...) is the foreign shop of the terminal scenario: ids are s001, s002 ... counted
per run (the world is fresh), module counter _SERVER_SHOPS assumes this scenario file is the only creator.

Widgets (Vanilla AbstractButton subclasses, found by label): "Developer: Buyer view" (owner), "Trade", "Dev: Owner view" (buyer);
terminal: "My shops", "Browse shops", "Collect", "Public"/"Private", "Trade", "<", ">", the sort button ("Price"/"Near"/"Name"), the
stock filter ("Stock"/"All"), an EditBox (search). Slots are real container slots; ONLY the menu index addresses the rows reliably
(goods/price/earnings/disc share cslot numbers): combined owner menu goods 0-4, price 5-9 (middle 7), earnings 10-36, disc 37, own
inventory 38+; buyer goods 0-4, price 5-9, pay 10-14, own 15+; terminal list 0-17, row A 18-22, row B 23-27, pay 28-32, own 33+.

Only a human can judge: look/feel of the boxes, text contrast, icons, the maintenance sign (needs two players: owner works while a
buyer waits, not reachable here), shop music sound, the particles/sounds of a trade.
"""
import re
import time

DZ = 10  # own area of this module: walk 10 blocks south first (every module uses a different offset)
_SERVER_SHOPS = {'n': 0}


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _xyz(st):
    return [int(v) for v in st['pos'].replace(',', ' ').split()]


def aim(s, px, py, pz, wait=0.8):
    """Puts the crosshair on the world point (the EYES look at it). 'tp @s ... facing' turns from the feet, so the eye height is taken off."""
    x, y, z = _xyz(s.st())
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {px:.3f} {py - 1.62:.3f} {pz:.3f}', wait)
    time.sleep(0.4)
    return s.st()


def count_of(st, item):
    n = 0
    for text in st.get('inventory', []):
        m = re.fullmatch(re.escape(item) + r' x(\d+)', text or '')
        if m:
            n += int(m.group(1))
    return n


def stack_at(st, index):
    for sl in st.get('slots', []):
        if sl['index'] == index:
            return sl['stack']
    return None


def wait_stack(s, index, want, seconds=5):
    """Waits until the slot shows exactly 'want' ('' = empty). Returns (status, stack)."""
    end = time.time() + seconds
    st = s.st()
    while True:
        got = stack_at(st, index)
        if got is not None and (got or '') == want:
            return st, got
        if time.time() > end:
            return st, got
        time.sleep(0.4)
        st = s.st()


def wait_until(s, pred, seconds=5):
    end = time.time() + seconds
    st = s.st()
    while not pred(st) and time.time() < end:
        time.sleep(0.4)
        st = s.st()
    return st


def own_index(st, text):
    for sl in st.get('slots', []):
        if sl['own'] and sl['stack'] == text:
            return sl['index']
    return None


def click_own(s, text, **kw):
    """Real click on the first own-inventory slot whose stack reads exactly 'text'."""
    st = s.st()
    idx = own_index(st, text)
    if idx is None:
        s.check(f'own slot with {text} present', False, [sl['stack'] for sl in st.get('slots', []) if sl['own'] and sl['stack']][:12])
        return None
    return s.click_slot(index=idx, **kw)


def widget(s, st, label):
    return s.find_widget(label, None, st, contains=False)


def right_click_empty(s, point):
    """Empty hand (hotbar 9, emptied first: shift-click collecting the earnings fills the hotbar from slot 9 - a-w4c's Display Case click then
    held the emeralds and quick-stocked them into the case instead of opening the screen), crosshair on the point, one real right click."""
    s.chat('item replace entity @s hotbar.8 with minecraft:air', 0.3)
    s.key('9', 0.4)
    aim(s, *point)
    s.io.right_click()


# ---------------------------------------------------------------- shop building with real placement
def build_shop(s, stock=None):
    """Places Sales Block and Display Case with the real right click (owner = player). Returns (sx, sy, sz) of the Sales Block or None."""
    s.chat('clear @s', 0.3)
    s.chat('item replace entity @s hotbar.0 with simpleshops:sales_block', 0.3)
    s.chat('item replace entity @s hotbar.1 with simpleshops:display_case', 0.3)
    s.chat('item replace entity @s hotbar.2 with simpleshops:sales_storage', 0.3)
    x, y, z = _xyz(s.st())
    sx = x + 3
    s.key('1', 0.4)
    st = aim(s, sx + 0.5, y - 0.1, z + 0.5)  # a point just under the top of the ground block: the ray hits its top face
    s.check('crosshair on the ground to place the Sales Block', bool(st.get('hitBlock')), st.get('hitBlock'))
    s.io.right_click()
    st = aim(s, sx + 0.5, y + 0.5, z + 0.5)
    s.check('real right click placed the Sales Block', st.get('hitBlock') == 'simpleshops:sales_block', st.get('hitBlock'))
    if st.get('hitBlock') != 'simpleshops:sales_block':
        return None
    s.key('2', 0.4)
    aim(s, sx + 0.5, y + 0.97, z + 0.5)  # a point just under the top face of the Sales Block: the ray from the eyes enters it through the top (0.3 block inside)
    s.io.right_click()
    st = aim(s, sx + 0.5, y + 1.5, z + 0.5)
    s.check('real right click put the Display Case on top', st.get('hitBlock') == 'simpleshops:display_case', st.get('hitBlock'))
    return sx, y, z


# ---------------------------------------------------------------- scenario 1: owner / buyer / storage
def owner_buyer(s):
    s.chat('tp @s ~ ~ ~' + str(DZ), 1.0)
    shop = build_shop(s)
    if not shop:
        return
    sx, y, z = shop
    centre = (sx + 0.5, y + 0.5, z + 0.5)
    s.chat('item replace entity @s hotbar.3 with minecraft:diamond 16', 0.3)
    s.chat('item replace entity @s hotbar.4 with minecraft:emerald 2', 0.3)
    s.chat('item replace entity @s inventory.0 with minecraft:music_disc_cat', 0.3)
    s.chat('item replace entity @s inventory.1 with minecraft:emerald 20', 0.3)

    # ---- owner screen
    right_click_empty(s, centre)
    st = s.wait_screen('ShopScreens$Owner', 10)
    s.check('empty-hand right click on the Sales Block opens the owner screen', 'ShopScreens$Owner' in st.get('screen', ''), st.get('screen'))
    if 'ShopScreens$Owner' not in st.get('screen', ''):
        return
    s.check('combined layout: 5 goods + 5 price + 27 earnings + disc + 36 own slots', len(st.get('slots', [])) == 74, len(st.get('slots', [])))
    s.check('developer buyer button is there (dev environment)', widget(s, st, 'Developer: Buyer view') is not None, s.labels(st))
    s.shot('owner-empty')

    # goods: pick the stack up from the hotbar and put it into goods slot 0 (real clicks)
    click_own(s, 'minecraft:diamond x16')
    s.click_slot(index=0)
    st, got = wait_stack(s, 0, 'minecraft:diamond x16')
    s.check('goods slot 0 holds the diamonds after a real click', got == 'minecraft:diamond x16', got)

    # price: the template slots never take a real item - right click adds the cursor stack size, left +1, empty cursor removes
    st = s.st()
    click_own(s, 'minecraft:emerald x2')
    st = s.click_slot(index=7, right=True) or s.st()
    st, got = wait_stack(s, 7, 'minecraft:emerald x2')
    s.check('price: right click on the empty middle slot = template with the cursor stack size (x2)', got == 'minecraft:emerald x2', got)
    s.click_slot(index=7)
    st, got = wait_stack(s, 7, 'minecraft:emerald x3')
    s.check('price: left click adds 1 (x3)', got == 'minecraft:emerald x3', got)
    st = s.st()
    s.check('price: the cursor stack is untouched (still x2)', st.get('carried') == 'minecraft:emerald x2', st.get('carried'))
    s.shot('owner-price')
    # put the cursor stack back (its old slot is empty), then an empty-cursor click removes the price
    st = s.st()
    back = next((sl['index'] for sl in st['slots'] if sl['own'] and not sl['stack']), None)
    if back is not None:
        s.click_slot(index=back)
    s.click_slot(index=7)
    st, got = wait_stack(s, 7, '')
    s.check('price: empty-cursor click removes the price', got == '', got)
    st = s.st()
    click_own(s, 'minecraft:emerald x2')
    s.click_slot(index=7, right=True)
    st, got = wait_stack(s, 7, 'minecraft:emerald x2')
    s.check('price: set again to x2 (right click)', got == 'minecraft:emerald x2', got)
    st = s.st()
    back = next((sl['index'] for sl in st['slots'] if sl['own'] and not sl['stack']), None)
    if back is not None:
        s.click_slot(index=back)
    # price slots on both sides of the run appear (next free positions)
    st = s.st()
    s.check('carried item is back in the inventory', not st.get('carried'), st.get('carried'))

    # shop music: a disc goes into the disc slot (37), shift-click takes it out again
    click_own(s, 'minecraft:music_disc_cat x1')
    s.click_slot(index=37)
    st, got = wait_stack(s, 37, 'minecraft:music_disc_cat x1')
    s.check('disc slot takes a music disc', got == 'minecraft:music_disc_cat x1', got)
    s.shot('owner-stocked')
    s.click_slot(index=37, shift=True)
    st, got = wait_stack(s, 37, '')
    s.check('shift-click empties the disc slot again', got == '', got)

    # ---- buyer view of the own shop (developer button): pay with shift-click, Trade, shift-Trade
    if s.click_widget('Developer: Buyer view'):
        st = s.wait_screen('ShopScreens$Buyer', 10)
        s.check('developer button opens the buyer screen', 'ShopScreens$Buyer' in st.get('screen', ''), st.get('screen'))
        if 'ShopScreens$Buyer' in st.get('screen', ''):
            st, got = wait_stack(s, 0, 'minecraft:diamond x16')
            s.check('buyer sees the goods (slot 0 diamond x16)', got == 'minecraft:diamond x16', got)
            st, got = wait_stack(s, 7, 'minecraft:emerald x2')
            s.check('buyer sees the price (slot 7 emerald x2)', got == 'minecraft:emerald x2', got)
            trade = widget(s, st, 'Trade')
            s.check('Trade button is off before paying', trade is not None and not trade['active'], trade)
            s.check('"Dev: Owner view" button back to the owner is shown', widget(s, st, 'Dev: Owner view') is not None, s.labels(st))
            s.shot('buyer-before')
            click_own(s, 'minecraft:emerald x20', shift=True)  # quick-move into the pay slot of the price position
            st = wait_until(s, lambda t: (widget(s, t, 'Trade') or {}).get('active'), 5)
            s.check('Trade button is on once the pay slot holds the price', bool((widget(s, st, 'Trade') or {}).get('active')),
                    [(sl['index'], sl['stack']) for sl in st['slots'][10:15]])
            s.shot('buyer-paid')
            before = count_of(st, 'minecraft:diamond')
            s.click_widget('Trade')
            st = wait_until(s, lambda t: count_of(t, 'minecraft:diamond') > before, 5)
            s.check('real click Trade: one diamond arrived', count_of(st, 'minecraft:diamond') == before + 1, count_of(st, 'minecraft:diamond'))
            s.shot('buyer-after-trade')
            once = count_of(st, 'minecraft:diamond')
            s.click_widget('Trade', shift=True)
            st = wait_until(s, lambda t: count_of(t, 'minecraft:diamond') > once + 1, 6)
            s.check('shift-Trade repeats while the payment lasts (several more diamonds)', count_of(st, 'minecraft:diamond') > once + 1,
                    count_of(st, 'minecraft:diamond'))
            if s.click_widget('Dev: Owner view'):
                st = s.wait_screen('ShopScreens$Owner', 10)
                s.check('"Dev: Owner view" returns to the owner screen', 'ShopScreens$Owner' in st.get('screen', ''), st.get('screen'))
                st = wait_until(s, lambda t: (stack_at(t, 10) or '').startswith('minecraft:emerald'), 5)
                s.check('earnings slot 10 shows the paid emeralds', (stack_at(st, 10) or '').startswith('minecraft:emerald'), stack_at(st, 10))
                s.shot('owner-earnings')
                s.click_slot(index=10, shift=True)
                st, got = wait_stack(s, 10, '')
                s.check('shift-click collects the earnings slot', got == '', got)
    s.close()

    # ---- Sales Storage: sneak + right click on the own Sales Block with the storage in hand
    s.key('3', 0.4)
    aim(s, *centre)
    s.io.key_down('Shift_L')
    try:
        s.io.right_click()
    finally:
        s.io.key_up('Shift_L')
    st = aim(s, sx - 0.5, y + 0.5, z + 0.5)
    s.check('sneak + right click attached a Sales Storage next to the Sales Block', st.get('hitBlock') == 'simpleshops:sales_storage', st.get('hitBlock'))
    if st.get('hitBlock') == 'simpleshops:sales_storage':
        s.key('9', 0.4)
        s.io.right_click()
        st = s.wait_screen('ShopScreens$Storage', 10)
        s.check('empty-hand right click opens the Sales Storage screen', 'ShopScreens$Storage' in st.get('screen', ''), st.get('screen'))
        s.check('27 storage slots', len([sl for sl in st.get('slots', []) if not sl['own']]) == 27, len(st.get('slots', [])))
        s.shot('storage')
    s.close()
    # the case on its own (owner): right click on the Display Case also opens the combined owner screen
    right_click_empty(s, (sx + 0.5, y + 1.5, z + 0.5))
    st = s.wait_screen('ShopScreens$', 8)
    s.check('right click on the Display Case (owner) opens the same owner screen', 'ShopScreens$Owner' in st.get('screen', ''), st.get('screen'))
    s.close()


# ---------------------------------------------------------------- scenario 2: trade terminal
def terminal(s):
    s.chat('tp @s ~ ~ ~' + str(DZ), 1.0)
    shop = build_shop(s)
    if not shop:
        return
    sx, y, z = shop
    x = _xyz(s.st())[0]
    s.chat('item replace entity @s hotbar.3 with minecraft:diamond 16', 0.3)
    s.chat('item replace entity @s hotbar.4 with minecraft:emerald 2', 0.3)
    s.chat('item replace entity @s inventory.2 with minecraft:iron_ingot 9', 0.3)
    # stock: the owner's right click on the case with the stack in hand puts it into the next free slot
    s.key('4', 0.4)
    aim(s, sx + 0.5, y + 1.5, z + 0.5)
    s.io.right_click()
    # price: owner screen
    right_click_empty(s, (sx + 0.5, y + 0.5, z + 0.5))
    st = s.wait_screen('ShopScreens$Owner', 10)
    if 'ShopScreens$Owner' in st.get('screen', ''):
        st, got = wait_stack(s, 0, 'minecraft:diamond x16')
        s.check('quick stock: the right click with the diamonds in hand filled goods slot 0', got == 'minecraft:diamond x16', got)
        click_own(s, 'minecraft:emerald x2')
        s.click_slot(index=7, right=True)
        st, got = wait_stack(s, 7, 'minecraft:emerald x2')
        s.check('price emerald x2 set by a real right click', got == 'minecraft:emerald x2', got)
        st = s.st()
        back = next((sl['index'] for sl in st['slots'] if sl['own'] and not sl['stack']), None)
        if back is not None:
            s.click_slot(index=back)
    else:
        s.check('owner screen of the terminal scenario opened', False, st.get('screen'))
    s.close()
    # an operator server shop with another good, the foreign shop to buy from
    _SERVER_SHOPS['n'] += 1
    sid = 's%03d' % _SERVER_SHOPS['n']
    s.chat(f'simpleshops servershop create {x - 3} {y} {z}', 0.8)
    s.chat(f'simpleshops servershop price {sid} add minecraft:iron_ingot 3', 0.6)
    s.chat(f'simpleshops servershop goods {sid} add minecraft:golden_apple 5', 0.6)
    # the terminal block (no owner needed)
    s.chat(f'setblock {x} {y} {z + 3} simpleshops:trade_terminal', 0.6)
    right_click_empty(s, (x + 0.5, y + 0.5, z + 3.5))
    st = s.wait_screen('TradeTerminalScreen', 10)
    s.check('empty-hand right click on the terminal opens its screen', 'TradeTerminalScreen' in st.get('screen', ''), st.get('screen'))
    if 'TradeTerminalScreen' not in st.get('screen', ''):
        return
    labels = s.labels(st)
    s.check('terminal widgets: both tabs, Collect, <, >', all(any(t in l for l in labels) for t in ('My shops', 'Browse shops', 'Collect', '<', '>')), labels)
    mine_tab = widget(s, st, 'My shops')
    s.check('starts on "My shops" (its tab is the inactive one)', mine_tab is not None and not mine_tab['active'], mine_tab)
    s.shot('terminal-open')

    # ---- My shops: pick the own shop (list slot 0), Public/Private toggle, Collect
    s.click_slot(index=0)
    st = wait_until(s, lambda t: (widget(s, t, 'Public') or {}).get('active'), 6)
    pub = widget(s, st, 'Public')
    s.check('own shop selected: the Public toggle is on and active', bool(pub and pub['active']), s.labels(st))
    # (a run with the 'simpleshops' scenario before leaves an older own shop with the same look: only the kinds are compared)
    s.check('row B shows the price (slot 23+2: emerald x2)', (stack_at(st, 25) or '') == 'minecraft:emerald x2', stack_at(st, 25))
    s.check('row A shows the goods (slot 18: diamonds)', (stack_at(st, 18) or '').startswith('minecraft:diamond x'), stack_at(st, 18))
    s.shot('terminal-mine-selected')
    s.click_widget('Public')
    st = wait_until(s, lambda t: widget(s, t, 'Private') is not None, 6)
    s.check('Public toggle switched the shop to Private', widget(s, st, 'Private') is not None, s.labels(st))
    s.click_widget('Private')
    st = wait_until(s, lambda t: widget(s, t, 'Public') is not None, 6)
    s.check('toggled back to Public', widget(s, st, 'Public') is not None, s.labels(st))
    s.click_widget('Collect')
    s.shot('terminal-collect')

    # ---- Browse: the own shop and the server shop are listed
    s.click_widget('Browse shops')
    st = wait_until(s, lambda t: (widget(s, t, 'Browse shops') or {}).get('active') is False, 6)
    s.check('Browse tab is active now', (widget(s, st, 'Browse shops') or {}).get('active') is False, s.labels(st))
    st = wait_until(s, lambda t: any((sl['index'] < 18 and sl['stack'].startswith('minecraft:golden_apple')) for sl in t.get('slots', [])), 6)
    listed = [sl['stack'] for sl in st.get('slots', []) if sl['index'] < 18 and sl['stack']]
    s.check('the list offers the server shop goods (golden apple)', any(t.startswith('minecraft:golden_apple') for t in listed), listed)
    s.check('the list offers the own shop goods (diamond)', any(t.startswith('minecraft:diamond') for t in listed), listed)
    s.shot('terminal-browse')
    # sort and stock buttons change their label
    sort0 = [w['label'] for w in s.widgets(st) if w['label'] in ('Price', 'Near', 'Name')]
    s.click_widget(sort0[0] if sort0 else 'Price')
    st = wait_until(s, lambda t: [w['label'] for w in s.widgets(t) if w['label'] in ('Price', 'Near', 'Name')] != sort0, 6)
    sort1 = [w['label'] for w in s.widgets(st) if w['label'] in ('Price', 'Near', 'Name')]
    s.check('sort button cycles its label', sort1 != sort0 and bool(sort1), f'{sort0} -> {sort1}')
    stock0 = 'Stock' if widget(s, st, 'Stock') else 'All'
    s.click_widget(stock0)
    st = wait_until(s, lambda t: widget(s, t, 'All' if stock0 == 'Stock' else 'Stock') is not None, 6)
    s.check('stock filter toggles Stock/All', widget(s, st, 'All' if stock0 == 'Stock' else 'Stock') is not None, s.labels(st))

    # ---- search by real typing
    if s.click_widget('Search', name='search box'):
        s.type_text('gold')
        st = wait_until(s, lambda t: len([sl for sl in t['slots'] if sl['index'] < 18 and sl['stack']]) == 1, 8)
        rows = [sl['stack'] for sl in st['slots'] if sl['index'] < 18 and sl['stack']]
        s.check('typing "gold" narrows the list to the golden apple offer', len(rows) == 1 and rows[0].startswith('minecraft:golden_apple'), rows)
        s.shot('terminal-search')
        for _ in range(4):
            s.key('BackSpace', 0.2)
        st = wait_until(s, lambda t: len([sl for sl in t['slots'] if sl['index'] < 18 and sl['stack']]) >= 2, 8)
        s.check('clearing the search shows both offers again', len([sl for sl in st['slots'] if sl['index'] < 18 and sl['stack']]) >= 2,
                [sl['stack'] for sl in st['slots'] if sl['index'] < 18 and sl['stack']])

    # ---- buy the golden apple from the server shop through the terminal
    st = s.st()
    row = next((sl['index'] for sl in st['slots'] if sl['index'] < 18 and sl['stack'].startswith('minecraft:golden_apple')), None)
    if row is None:
        s.check('golden apple row to click', False, [sl['stack'] for sl in st['slots'] if sl['index'] < 18])
    else:
        s.click_slot(index=row)
        st = wait_until(s, lambda t: (stack_at(t, 20) or '') == 'minecraft:iron_ingot x3', 6)
        s.check('offer selected: row A shows the price (iron ingot x3)', (stack_at(st, 20) or '') == 'minecraft:iron_ingot x3', stack_at(st, 20))
        click_own(s, 'minecraft:iron_ingot x9', shift=True)
        st = wait_until(s, lambda t: (widget(s, t, 'Trade') or {}).get('active'), 6)
        s.check('Trade is on once the pay slot holds the price', bool((widget(s, st, 'Trade') or {}).get('active')), stack_at(st, 30))
        s.shot('terminal-paid')
        s.click_widget('Trade')
        st = wait_until(s, lambda t: count_of(t, 'minecraft:golden_apple') >= 1, 6)
        s.check('real click Trade: a golden apple arrived in the inventory', count_of(st, 'minecraft:golden_apple') >= 1, count_of(st, 'minecraft:golden_apple'))
        s.shot('terminal-bought')
    s.close()
    time.sleep(0.5)


SCENARIOS = {'simpleshops': owner_buyer, 'simpleshops/terminal': terminal}
