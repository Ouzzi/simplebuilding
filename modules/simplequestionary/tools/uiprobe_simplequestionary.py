"""simplequestionary-2: the Questionary table's book screen (QuestionaryScreen) with the REAL mouse and keyboard.

Scenario for modules/simpleinterfaces/tools/ui_probe.py. Opening: Questionary Table block (simplequestionary:questionary_table),
real right click on it (Use opens the menu). The screen has NO Vanilla widgets (bookmarks, page arrows and the text scroll are
drawn by the screen and hit-tested in mouseClicked / mouseScrolled / keyPressed), so every click position is computed from the
Java layout (QuestionaryScreen.init, QuestionarySheet.bookGrowth: EDGE 9, GROW_X 40, GROW_Y 32; QuestionaryMenu 424 x 238 window):
  leftPos = (guiW - 424) / 2, topPos = (guiH - 238) / 2, grow = min(40 | 32, (gui - 18 - 424 | 238) / 2),
  bookX = -growX, bookY = 14 - growY, bookW = 424 + 2 growX, bookH = 224 + 2 growY,
  bookmark i (0..2): x = leftPos + 318 + 28 i .. + 24, active ribbon top = topPos + bookY - 14, click zone down to topPos + bookY + 3,
  arrows: y = topPos + bookY + bookH - 20 (+10), left x = leftPos + bookX + 13 (+18), right x = leftPos + bookX + bookW - 13 - 18 (+18).
ASSUMPTION: the window is the 1280 x 720 the driver requests (gui = 1280 / scale); the page that is open is NOT in the status, so it is
read from a screenshot (PIL): the active bookmark's ribbon stands 3 gui px higher, so a pixel in the row above the resting ribbons
shows the ribbon colour only for the active page. Everything the screenshots cannot decide (look of the book, text, runes) is for a human.
"""
import time

try:
    from PIL import Image
except ImportError:  # the page checks then fail with a clear message instead of an exception
    Image = None

TABLE = 'simplequestionary:questionary_table'
PAGE_NAMES = ('lore', 'use', 'secret')


def _aim(s, block, dx, dy, dz, state=''):
    """Sets the block dx/dy/dz from the player's feet and aims the crosshair at its centre.
    `tp ... facing` looks from the FEET anchor, so the target is lowered by the eye height (1.62) - same as sort_probe's chest."""
    st = s.st()
    x, y, z = [int(v) for v in st['pos'].replace(',', ' ').split()]
    bx, by, bz = x + dx, y + dy, z + dz
    s.chat(f'setblock {bx} {by} {bz} {block}{state}', 0.6)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {bx + 0.5} {by + 0.5 - 1.62:.2f} {bz + 0.5}', 0.8)
    time.sleep(0.6)
    return (bx, by, bz), s.st()


def _geometry(st):
    scale = st['scale']
    gw, gh = 1280 // scale, 720 // scale
    left, top = (gw - 424) // 2, (gh - 238) // 2
    gx, gy = min(40, max(0, (gw - 18 - 424) // 2)), min(32, max(0, (gh - 18 - 238) // 2))
    book_x, book_y, book_w, book_h = -gx, 14 - gy, 424 + 2 * gx, 224 + 2 * gy
    return dict(scale=scale, left=left, top=top, bx=book_x, by=book_y, bw=book_w, bh=book_h)


def _tab_xy(g, i):
    """Window-pixel centre of bookmark i (inside the click zone for both the resting and the raised ribbon)."""
    return (g['left'] + 318 + 28 * i + 12) * g['scale'], (g['top'] + g['by'] - 4) * g['scale']


def _arrow_xy(g, forward):
    """Centre of a page arrow (18 x 10 gui px, click zone +-2) in window pixels."""
    y = g['top'] + g['by'] + g['bh'] - 20 + 5
    x = g['left'] + g['bx'] + g['bw'] - 13 - 18 + 9 if forward else g['left'] + g['bx'] + 13 + 9
    return x * g['scale'], y * g['scale']


def _right_page_xy(g):
    """A point on the right page text area (not the column, not the left page): the wheel there turns pages."""
    right_x = 124 + 176 + 6
    right_w = g['bx'] + g['bw'] - 13 - right_x
    return (g['left'] + right_x + right_w // 2) * g['scale'], (g['top'] + g['by'] + 12 + 18 + 60) * g['scale']


def _active_tab(s, name):
    """Screenshot named <name>, then which bookmark is raised. Returns (index or None, detail)."""
    s.shot(name)
    page = s.st().get('fields', {}).get('page')  # SortProbe reads the screen's private page field by reflection
    if page is not None:
        return int(page), f'page field = {page}'
    if Image is None:
        return None, 'PIL missing'
    path = s.out / 'shots' / f'{s.scenario}-{name}.png'
    try:
        img = Image.open(path).convert('RGB')
    except Exception as e:  # noqa: BLE001 - any read problem is a failed check, not an exception
        return None, f'cannot read {path.name}: {e}'
    g = _geometry(s.last)
    sc = g['scale']
    row = (g['top'] + g['by'] - 12) * sc + 1  # one row above where a resting ribbon starts (top + by - 11)
    ref = img.getpixel(((g['left'] + 318 - 8) * sc, row))
    diffs = []
    for i in range(3):
        px = img.getpixel(((g['left'] + 318 + 28 * i + 12) * sc, row))
        diffs.append(sum(abs(a - b) for a, b in zip(px, ref)))
    best = max(range(3), key=lambda i: diffs[i])
    return (best if diffs[best] > 40 else None), f'ribbon diffs {diffs} (ref {ref})'


def questionary(s):
    s.chat('gamemode creative @s', 0.5)
    s.chat('tp @s ~ ~ ~40', 1.0)  # own area, away from the other scenarios
    time.sleep(1.5)
    s.clear_and_give('minecraft:iron_pickaxe', 'minecraft:diamond 5')
    # ---- open: real right click on the table
    pos, st = _aim(s, TABLE, 3, 0, 0)
    s.check('crosshair on the questionary table', st.get('hitBlock') == TABLE, st.get('hitBlock'))
    s.io.right_click()
    st = s.wait_screen('QuestionaryScreen', 12)
    s.check('real right click opens the Questionary screen', 'QuestionaryScreen' in st.get('screen', ''), st.get('screen'))
    if 'QuestionaryScreen' not in st.get('screen', ''):
        return
    own = [x for x in st['slots'] if x['own']]
    desk = [x for x in st['slots'] if not x['own']]
    s.check('menu has 1 item slot + 36 inventory slots', len(desk) == 1 and len(own) == 36, f'{len(desk)} + {len(own)}')
    s.shot('welcome')
    g = _geometry(st)

    # ---- real clicks: iron pickaxe from the hotbar into the item slot
    st = s.click_slot(cslot=0, own=True)
    s.check('click on hotbar slot 0 lifts the pickaxe', st and st.get('carried', '').startswith('minecraft:iron_pickaxe'), st and st.get('carried'))
    st = s.click_slot(cslot=0, own=False)
    if st:
        item = s.slot_xy(st, 0, False)
        s.check('click on the item slot puts the pickaxe in', item and item['stack'] == 'minecraft:iron_pickaxe x1', item and item['stack'])
        s.check('cursor is empty again', not st.get('carried'), st.get('carried'))
    time.sleep(1.0)  # the server sends the facts for the Use page
    s.shot('pickaxe-in-slot')

    # ---- bookmarks 0 / 1 / 2 with the real mouse (page read from the ribbon height)
    for i in (2, 0, 1):
        s.click_xy(*_tab_xy(g, i), st=st)
        time.sleep(0.6)
        tab, detail = _active_tab(s, f'bookmark-{PAGE_NAMES[i]}')
        s.check(f'bookmark {i} ({PAGE_NAMES[i]}) raises its ribbon', tab == i, detail)
    # ---- arrows: page 1 -> right arrow -> 2 -> right arrow (stays 2, last page) -> left arrow -> 1 -> left -> 0 -> left (stays 0)
    st = s.st()
    steps = ((True, 2, 'right arrow 1 -> 2'), (True, 2, 'right arrow on the last page stays 2'),
             (False, 1, 'left arrow 2 -> 1'), (False, 0, 'left arrow 1 -> 0'), (False, 0, 'left arrow on the first page stays 0'))
    for n, (forward, want, label) in enumerate(steps, 1):
        ax, ay = _arrow_xy(g, forward)
        s.click_xy(ax, ay, st=st)
        time.sleep(0.6)
        tab, detail = _active_tab(s, f'arrow-{n}-page{want}')
        s.check(label, tab == want, detail)
    # ---- mouse wheel over the right page: down = next page, up = previous
    wx, wy = _right_page_xy(g)
    for down, want, label in ((True, 1, 'wheel down 0 -> 1'), (True, 2, 'wheel down 1 -> 2'), (False, 1, 'wheel up 2 -> 1')):
        s.wheel(wx, wy, 1, down=down, st=st)
        tab, detail = _active_tab(s, 'wheel-' + str(want))
        s.check(label, tab == want, detail)
    # ---- keyboard: Right / D next page, Left / A previous
    for key, want in (('Left', 0), ('d', 1), ('Right', 2), ('a', 1)):
        s.key(key, 0.6)
        tab, detail = _active_tab(s, f'key-{key}')
        s.check(f'key {key} -> page {want}', tab == want, detail)

    # ---- shift click (quickMoveStack): occupied item slot refuses, item slot -> inventory, diamond -> item slot (1 piece)
    st = s.st()
    if not s.slot_xy(st, 1, True) or not s.slot_xy(st, 0, False):
        s.check('Questionary screen still open for the shift-click part', False, f"{st.get('screen')} / {len(st.get('slots', []))} slots")
        return
    before = s.slot_xy(st, 1, True)['stack']
    st = s.click_slot(cslot=1, own=True, shift=True)
    s.check('shift click on diamonds while the slot is occupied does nothing', st and s.slot_xy(st, 1, True)['stack'] == before and
            s.slot_xy(st, 0, False)['stack'] == 'minecraft:iron_pickaxe x1', st and (s.slot_xy(st, 1, True)['stack'], s.slot_xy(st, 0, False)['stack']))
    st = s.click_slot(cslot=0, own=False, shift=True)
    if st:
        s.check('shift click on the item slot sends the pickaxe back to the inventory', not s.slot_xy(st, 0, False)['stack'] and
                any(x['stack'] == 'minecraft:iron_pickaxe x1' for x in st['slots'] if x['own']), s.slot_xy(st, 0, False)['stack'])
    st = s.click_slot(cslot=1, own=True, shift=True)
    if st:
        slot = s.slot_xy(st, 0, False)['stack']
        s.check('shift click on a diamond stack moves ONE diamond into the item slot', slot == 'minecraft:diamond x1', slot)
        s.check('4 diamonds stay in the inventory', s.slot_xy(st, 1, True)['stack'] == 'minecraft:diamond x4', s.slot_xy(st, 1, True)['stack'])
    time.sleep(1.0)
    s.shot('diamond-in-slot')
    # ---- closing returns the item (removed -> clearContainer)
    s.close()
    st = s.st()
    s.check('closing the screen gives the item back', sum(1 for x in st['inventory'] if x.startswith('minecraft:diamond')) >= 1 and
            sum(int(x.split(' x')[1]) for x in st['inventory'] if x.startswith('minecraft:diamond')) == 5, st['inventory'][:9])


SCENARIOS = {'simplequestionary': questionary}
