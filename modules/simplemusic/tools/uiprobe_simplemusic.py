"""simplemusic-2: sheet music editor (code + grid), import, examples, listen, upload window, signing, the disc press and the Pocket Jukebox in the hand,
with the REAL mouse and keyboard (scenarios for modules/simpleinterfaces/tools/ui_probe.py).  Single-player only: the multiplayer part of the plan
(upload -> operator approval -> streaming at a second player) cannot run here.

SCENARIOS
  simplemusic          sheet editor: real typing into the code field, grid left/right click, Import of a .txt, Add example, Listen/Stop,
                       Upload window opens, title + Sign.
  simplemusic/press    Disc Press block: sheet + blank + dye by real clicks, pressing, shift-click of the pressed custom disc.
  simplemusic/pocket   Pocket Jukebox: discs in/out by real inventory clicks, wheel over the slot, play/pause with the real right click in the hand,
                       placing with Shift + right click, Shift + wheel on the placed block (pictures).

How the screens open: sheet editor = right click with the sheet in the main hand into the air (Item.use -> SimpleMusic.openSheet -> SheetMusicScreen);
disc press = empty-hand right click on the block (DiscPressScreen); pocket jukebox in the inventory = key E, survival (InventoryScreen).

STATUS vs. EFFECT: the code field (UiCodeArea) and the grid cells are drawn by the screen itself and are not in the status (the UiCodeArea is listed as a
widget but its text is not).  What a click/typing really did is therefore read from the SERVER after the screen is closed (the editor saves on close)
with a command side channel: `execute <condition> run item replace entity @s hotbar.8 with minecraft:stick` and a look at hotbar slot 8 in the status
(probe(), conditions on `SelectedItem.components."simplemusic:sheet"` / `Inventory[...]`).  Where a condition cannot be asked that way the check is a screenshot.
GRID GEOMETRY (assumption, from SheetMusicScreen.init at gui scale 2, 640 x 360 gui px, one button row): cell (step, note) centre in gui px =
(40 + 12 * step, 55 + 10 * (24 - note) + 5); the 21 px label column assumes the font width of "f#5" = 17 + 4.  Cells are 12 px wide, so a 3 px error still hits.
The import scenario writes <game dir>/config/simplemusic/sheets/probe-song.txt (the folder the Import button lists).

Only a human can judge: the editor's look (box colours, syntax colours, error line, grid cells/ghosts), what Listen sounds like, the pressed disc's look and
the dye tint, the pocket jukebox models (open bundle model with a selected disc, the placed block's shown disc), sounds of insert/remove/play/pause, the
Redstone/comparator behaviour of the placed jukebox and the whole multiplayer path.
"""
import re
import time
from pathlib import Path

DZ = 40  # own area of this module (south of the riding area)
REPO = Path(__file__).resolve().parents[3]
SONG = 'tempo 4\\nharp: c4 e4 g4 - c5 . . g4\\nbass: c3 . c3 . [c3 g3] .'  # as written in a command string (\n = newline)
SONG_FILE = 'tempo 4\nharp: c4 e4 g4 - c5 . . g4\nbass: c3 . c3 . [c3 g3] .'


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _xyz(st):
    return [int(v) for v in st['pos'].replace(',', ' ').split()]


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


def wait_stack(s, index, want, seconds=5, prefix=False):
    end = time.time() + seconds
    st = s.st()
    while True:
        got = stack_at(st, index) or ''
        if (got.startswith(want) and bool(want)) if prefix else got == want:
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
    st = s.st()
    idx = own_index(st, text)
    if idx is None:
        s.check(f'own slot with {text} present', False, [sl['stack'] for sl in st.get('slots', []) if sl['own'] and sl['stack']][:12])
        return None
    return s.click_slot(index=idx, **kw)


def look(s, at, target, wait=0.9):
    """The player stands at 'at' and the EYES look at 'target' (tp ... facing turns from the feet: eye height 1.62 taken off)."""
    s.chat(f'tp @s {at[0]:.2f} {at[1]} {at[2]:.2f} facing {target[0]:.3f} {target[1] - 1.62:.3f} {target[2]:.3f}', wait)
    time.sleep(0.5)
    return s.st()


def look_at_air(s):
    """Crosshair on the open sky/horizon straight ahead (no block in the way): a right click then only reaches the item in hand."""
    x, y, z = _xyz(s.st())
    return look(s, (x + 0.5, y, z + 0.5), (x + 40.5, y + 1.62, z + 0.5))


def probe(s, condition, slot=8):
    """True when `execute <condition>` succeeds on the server: it puts a stick in hotbar slot 8, which the status shows."""
    s.chat(f'execute {condition} run item replace entity @s hotbar.{slot} with minecraft:stick', 0.9)
    ok = (s.st().get('inventory', [''] * 9)[slot] or '').startswith('minecraft:stick')
    if ok:
        s.chat(f'item replace entity @s hotbar.{slot} with minecraft:air', 0.4)
    return ok


SHEET = 'SelectedItem.components."simplemusic:sheet"'


def sheet_has(s, extra=''):
    """The sheet in the main hand has note code (not blank), optionally with more conditions ('unless data entity ...')."""
    return probe(s, f'if data entity @s {SHEET}.code {extra}')


def sheet_is(s, snbt):
    return probe(s, f'if data entity @s {SHEET}{{{snbt}}}')


def gui_cell(st, step, note):
    """Window-pixel centre of a grid cell (see the docstring for the layout assumptions)."""
    scale = st['scale']
    return (40 + 12 * step) * scale, (55 + 10 * (24 - note) + 5) * scale


def open_sheet(s, expect=True):
    """Real right click into the air with the sheet in the main hand (hotbar 1)."""
    s.key('1', 0.4)
    look_at_air(s)
    s.io.right_click()
    st = s.wait_screen('SheetMusicScreen', 10)
    if expect:
        s.check('right click with the sheet opens the editor', 'SheetMusicScreen' in st.get('screen', ''), f"{st.get('screen')}")
    return st


# ---------------------------------------------------------------- scenario 1: the editor
def editor(s):
    s.chat('tp @s ~ ~ ~' + str(DZ), 1.0)
    s.chat('clear @s', 0.3)
    s.chat('item replace entity @s hotbar.0 with simplemusic:sheet_music', 0.4)
    # the Import list shows what is in config/simplemusic/sheets: write the song file the import step picks
    folder = REPO / 'integration/run-fabric-263/config/simplemusic/sheets'
    try:
        folder.mkdir(parents=True, exist_ok=True)
        (folder / 'probe-song.txt').write_text(SONG_FILE, encoding='utf-8')
    except OSError as e:
        s.check('import test file written', False, e)

    # ---- (a) open, widgets, real typing into the code field
    st = open_sheet(s)
    if 'SheetMusicScreen' not in st.get('screen', ''):
        return
    labels = s.labels(st)
    want = ('Listen', 'Import', 'Add example', 'Copy', 'Sign', 'Upload', 'Done')
    s.check('editor buttons: Listen, Import, Add example, Copy, Sign, Upload, Done', all(any(w == l.split(':', 1)[1] for l in labels) for w in want), labels)
    s.check('editor has the code field ("Note code") and the title box ("Title")', s.find_widget('Note code', None, st) is not None and s.find_widget('Title', None, st) is not None, labels)
    s.check('grid layout: the instrument button is there (window >= 360 gui px wide)', s.find_widget('Instrument:', None, st) is not None, labels)
    s.shot('editor-empty')
    s.click_widget('Note code')
    s.type_text('tempo 6')
    s.shot('editor-typed')
    s.click_widget('Done')
    st = s.wait_screen('none', 6)
    s.check('Done closes the editor', st.get('screen') == 'none', st.get('screen'))
    time.sleep(0.8)
    s.check('typed "tempo 6" was saved to the sheet (server)', sheet_is(s, 'code:"tempo 6"'), 'probe')

    # ---- (b) grid: left click sets a note, instrument button, right click removes it
    st = open_sheet(s)
    inst = s.find_widget('Instrument:', None, st)
    before = inst['label'] if inst else ''
    s.click_widget('Instrument:')
    st = s.st()
    after = (s.find_widget('Instrument:', None, st) or {}).get('label', '')
    s.check('the instrument button cycles (label changes)', bool(after) and after != before, f'{before} -> {after}')
    s.click_widget('Instrument:', shift=True)
    st = s.st()
    s.check('shift-click on it goes back', (s.find_widget('Instrument:', None, st) or {}).get('label', '') == before, (s.find_widget('Instrument:', None, st) or {}).get('label'))
    gx, gy = gui_cell(st, 2, 12)
    s.click_xy(gx, gy, st)
    s.shot('grid-note')
    s.click_widget('Done')
    s.wait_screen('none', 6)
    time.sleep(0.8)
    s.check('a left click on a grid cell wrote a note (code is more than the tempo line)',
            sheet_has(s, 'unless data entity @s ' + SHEET + '{code:"tempo 6"}'), 'probe')
    st = open_sheet(s)
    gx, gy = gui_cell(st, 2, 12)
    s.right_click_xy(gx, gy, st)
    s.key('Escape', 0.8)  # Esc closes and saves like Done
    st = s.wait_screen('none', 6)
    time.sleep(0.8)
    # N60P: NoteGrid.toCode strips the text - with no note left the code is exactly "tempo 6" (no newline; NoteGridTest: empty -> "tempo 4")
    s.check('a right click on the same cell removed the note again (tempo line only)', sheet_is(s, 'code:"tempo 6"'), 'probe')

    # ---- (c) import of the .txt file
    st = open_sheet(s)
    if s.click_widget('Import'):
        st = s.st()
        s.shot('import-list')
        if s.click_widget('probe-song.txt'):
            st = s.st()
            s.check('importing returns to the editor (Listen is back)', s.find_widget('Listen', None, st) is not None, s.labels(st))
            s.click_widget('Done')
            s.wait_screen('none', 6)
            time.sleep(0.8)
            s.check('the imported song is the sheet\'s code (exact text)', sheet_is(s, f'code:"{SONG}"'), 'probe')

    # ---- (d) example, listen / stop, upload window
    st = open_sheet(s)
    if s.click_widget('Add example'):
        st = s.st()
        s.shot('examples')
        picks = [w for w in s.widgets(st) if ' - ' in w['label']]
        s.check('the example list offers songs ("Title - Author")', bool(picks), s.labels(st))
        if picks:
            s.click_xy(picks[0]['x'], picks[0]['y'], st)
            st = s.st()
            s.check('picking an example returns to the editor', s.find_widget('Listen', None, st) is not None, s.labels(st))
            s.click_widget('Listen')
            st = wait_until(s, lambda t: s.find_widget('Stop', None, t) is not None, 4)
            s.check('Listen plays the example: the button reads Stop', s.find_widget('Stop', None, st) is not None, s.labels(st))
            s.shot('listening')
            s.click_widget('Stop')
            st = wait_until(s, lambda t: s.find_widget('Listen', None, t) is not None, 4)
            s.check('Stop ends it: the button reads Listen again', s.find_widget('Listen', None, st) is not None, s.labels(st))
    if s.click_widget('Upload'):
        st = s.wait_screen('BurnScreen', 8)
        s.check('Upload opens the upload window (BurnScreen)', 'BurnScreen' in st.get('screen', ''), st.get('screen'))
        s.shot('upload-window')
        s.key('Escape', 0.8)
    s.close()

    # ---- (e) title + Sign
    time.sleep(0.5)
    st = open_sheet(s)
    if s.click_widget('Title'):
        # N60P: picking the example in (d) also set the empty title to the song's ("First Steps", SheetMusicScreen example pick) and Upload
        # saved it; typing appended -> "First Stepsprobe". Select all and delete first, like a player replacing a title.
        s.io.key_down('Control_L')
        s.key('a', 0.3)
        s.io.key_up('Control_L')
        s.key('BackSpace', 0.3)
        s.type_text('probe')
        s.click_widget('Sign')
        st = s.wait_screen('none', 6)
        s.check('Sign closes the editor', st.get('screen') == 'none', st.get('screen'))
        time.sleep(0.8)
        s.check('the sheet is signed with the typed title', sheet_is(s, 'signed:1b,title:"probe"'), 'probe')
    s.close()


# ---------------------------------------------------------------- scenario 2: the disc press
def press(s):
    s.chat('tp @s ~ ~ ~' + str(DZ), 1.0)
    x, y, z = _xyz(s.st())
    s.chat('clear @s', 0.3)
    s.chat('item replace entity @s hotbar.0 with simplemusic:sheet_music[simplemusic:sheet={code:"harp: c4 e4 g4",title:"Probe",author:"Probe",signed:1b}]', 0.4)
    s.chat('item replace entity @s hotbar.1 with simplemusic:blank_disc 2', 0.4)
    s.chat('item replace entity @s hotbar.2 with minecraft:red_dye 2', 0.4)
    px, pz = x + 3, z
    s.chat(f'setblock {px} {y} {pz} simplemusic:disc_press', 0.6)
    s.key('9', 0.4)
    look(s, (x + 0.5, y, z + 0.5), (px + 0.5, y + 0.5, pz + 0.5))
    s.io.right_click()
    st = s.wait_screen('DiscPressScreen', 10)
    s.check('empty-hand right click on the Disc Press opens its screen', 'DiscPressScreen' in st.get('screen', ''), f"{st.get('screen')} hit={st.get('hitBlock')}")
    if 'DiscPressScreen' not in st.get('screen', ''):
        return
    s.check('press menu: 4 own slots + 36 inventory slots', len(st.get('slots', [])) == 40, len(st.get('slots', [])))
    s.shot('press-empty')
    # sheet (slot 0), blank (slot 1), dye (slot 3) by real clicks; the dye slot takes only a dye
    click_own(s, 'minecraft:red_dye x2')
    s.click_slot(index=3)
    st, got = wait_stack(s, 3, 'minecraft:red_dye x2')
    s.check('real clicks: the dye slot (3) holds the red dye', got == 'minecraft:red_dye x2', got)
    click_own(s, 'simplemusic:blank_disc x2')
    s.click_slot(index=1)
    st, got = wait_stack(s, 1, 'simplemusic:blank_disc x2')
    s.check('real clicks: the blank disc slot (1) holds the blank discs', got == 'simplemusic:blank_disc x2', got)
    click_own(s, 'simplemusic:sheet_music x1', shift=True)  # shift-click goes to the sheet slot
    st, got = wait_stack(s, 0, 'simplemusic:sheet_music x1')
    s.check('shift-click sends the sheet into slot 0', got == 'simplemusic:sheet_music x1', got)
    s.shot('press-loaded')
    st, got = wait_stack(s, 2, 'simplemusic:custom_disc', 12, prefix=True)
    s.check('after the pressing time the output slot (2) shows the custom disc', got.startswith('simplemusic:custom_disc'), got)
    s.shot('press-done')
    s.click_slot(index=2, shift=True)
    st = wait_until(s, lambda t: count_of(t, 'simplemusic:custom_disc') >= 1, 5)
    s.check('shift-click on the output moves the pressed disc into the inventory', count_of(st, 'simplemusic:custom_disc') >= 1, count_of(st, 'simplemusic:custom_disc'))
    s.close()


# ---------------------------------------------------------------- scenario 3: the pocket jukebox
INV = 'Inventory[{Slot:0b}]'
BUNDLE = 'components."minecraft:bundle_contents"'


def pocket(s):
    s.chat('tp @s ~ ~ ~' + str(DZ), 1.0)
    x, y, z = _xyz(s.st())
    s.chat('gamemode survival @s', 0.8)
    s.chat('clear @s', 0.3)
    s.chat('item replace entity @s hotbar.0 with simplemusic:pocket_jukebox', 0.4)
    s.chat('item replace entity @s inventory.0 with minecraft:music_disc_cat', 0.3)
    s.chat('item replace entity @s inventory.1 with minecraft:music_disc_13', 0.3)
    s.chat('item replace entity @s inventory.2 with minecraft:music_disc_blocks', 0.3)
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 8)
    s.check('E opens the survival inventory', 'InventoryScreen' in st.get('screen', ''), st.get('screen'))
    if 'InventoryScreen' not in st.get('screen', ''):
        return
    s.shot('pocket-inventory')
    jukebox = own_index(st, 'simplemusic:pocket_jukebox x1')
    s.check('the pocket jukebox is in the hotbar', jukebox is not None, [sl['stack'] for sl in st['slots'] if sl['own'] and sl['stack']])
    if jukebox is None:
        return
    # discs go in with a left click on the jukebox while the cursor holds one (like a bundle)
    for disc in ('minecraft:music_disc_cat x1', 'minecraft:music_disc_13 x1', 'minecraft:music_disc_blocks x1'):
        click_own(s, disc)
        s.click_slot(index=jukebox)
        st = s.st()
        s.check(f'{disc.split(":")[1].split(" ")[0]}: the cursor is empty after dropping it on the jukebox (one disc went in)', not st.get('carried'), st.get('carried'))
    s.check('three discs are in the jukebox (bundle contents has a third entry)', probe(s, f'if data entity @s {INV}.{BUNDLE}[2]'), 'probe')
    s.check('and no fourth entry', not probe(s, f'if data entity @s {INV}.{BUNDLE}[3]'), 'probe')
    # right click with an empty cursor takes the (selected / newest) disc out
    s.click_slot(index=jukebox, right=True)
    st = s.st()
    s.check('right click with an empty cursor takes a disc onto the cursor', bool(st.get('carried', '')) and 'music_disc' in st.get('carried', ''), st.get('carried'))
    s.click_slot(index=jukebox)  # and puts it back
    st = s.st()
    s.check('left click with that disc puts it back in', not st.get('carried'), st.get('carried'))
    # the wheel over the slot selects a disc (Vanilla bundle behaviour); the tooltip shows it
    sl = next(x for x in st['slots'] if x['index'] == jukebox)
    s.wheel(sl['x'], sl['y'], clicks=1, down=True, st=st)
    s.shot('pocket-wheel-tooltip')
    s.key('e', 1.0)
    s.close()
    s.chat('gamemode survival @s', 0.6)

    # ---- in the hand: right click plays, again pauses
    s.key('1', 0.4)
    look_at_air(s)
    s.io.right_click()
    time.sleep(0.8)
    # N60P: simplemusic:playing is transient (no persistent codec) and the item predicate parser refuses it - run 1 (and a-w4c) showed
    # "Expected literal ~ ...ic:playing] run item replace ..." in the chat, the probe command never ran. The persistent
    # simplemusic:pocket state tells the same: PocketPlayback.begin writes {id, ticks: start position = 0 for a fresh jukebox},
    # pause writes the position reached (> 0 after 0.8 s). ticks 0 may be left out (optional field with default 0).
    pocket = 'SelectedItem.components."simplemusic:pocket"'
    started = probe(s, f'if data entity @s {pocket}.id') and (not probe(s, f'if data entity @s {pocket}.ticks') or probe(s, f'if data entity @s {pocket}{{ticks:0}}'))
    s.check('right click in the air plays (a session id on the stack, position 0)', started, f'data {pocket}: id set, ticks 0/absent')
    s.shot('pocket-playing')
    s.io.right_click()
    time.sleep(0.8)
    paused = probe(s, f'if data entity @s {pocket}.ticks') and not probe(s, f'if data entity @s {pocket}{{ticks:0}}')
    s.check('a second right click pauses it (the position reached is stored)', paused, f'data {pocket}.ticks > 0')

    # ---- placing: Shift + right click on the ground, Shift + wheel on the placed block
    look(s, (x + 0.5, y, z + 0.5), (x + 3.5, y - 0.1, z + 0.5))
    s.io.key_down('Shift_L')
    try:
        s.io.right_click()
    finally:
        s.io.key_up('Shift_L')
    # where the block landed is asked on the server (the ray of the second look passes over the low block, a-w4c read the grass behind it)
    placed = next(((dx, dy) for dx in range(1, 5) for dy in (0, 1)
                   if probe(s, f'if block {x + dx} {y + dy} {z} simplemusic:pocket_jukebox')), None)
    hand = (s.st().get('inventory', [''])[0] or '')
    s.check('Shift + right click put the jukebox down as a block (hand empty, block in front)', placed is not None and not hand.startswith('simplemusic:pocket_jukebox'),
            f'block at dx/dy {placed}, hand {hand!r}')
    st = look(s, (x + 0.5, y, z + 0.5), (x + (placed[0] if placed else 3) + 0.5, y + (placed[1] if placed else 0) + 0.25, z + 0.5))
    s.shot('pocket-placed')
    if placed:
        s.io.key_down('Shift_L')
        try:
            for _ in range(2):
                s.io.button(5, True)
                s.io.button(5, False)
                time.sleep(0.4)
        finally:
            s.io.key_up('Shift_L')
        time.sleep(0.8)
        s.shot('pocket-placed-scrolled')
    s.chat('gamemode creative @s', 0.5)


SCENARIOS = {'simplemusic': editor, 'simplemusic/press': press, 'simplemusic/pocket': pocket}
