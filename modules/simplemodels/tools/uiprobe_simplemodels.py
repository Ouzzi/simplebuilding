"""simplemodels-2: Model Browser and Model Editor with the REAL mouse (and keyboard).

Scenario for modules/simpleinterfaces/tools/ui_probe.py. Opening: survival, key E -> InventoryScreen (the "Models" bookmark - class
ModelsTab, a real AbstractButton added by ModelScreenMixin - sits at its left edge) -> real click on it -> ModelBrowser (Screen).
In the browser the "Editor" button opens ModelEditorScreen (loaded with the selected model, otherwise the built-in stool).
Both screens are built from REAL widgets (Buttons, CycleButtons, EditBoxes: label/active/visible come from the status), so almost
everything is clicked by label. Not in the status (drawn by the screens, read from screenshots with Pillow where a check is possible):
the 3D view (orbit by right drag, zoom by wheel -> changed pixels in the view area; ASSUMPTION gui = 1280 x 720 / scale and
ModelEditorLayout.of, see _view_rect: at gui width 640 the view is (122, 29) 371 x (guiH - 49)), the cube list on the left (rows 12 px from y = 29 + 14), the
selected-model preview and the six example models' look (the items are given with item_model, see the 'six-examples' shot) - those need a
human. The editor state is read from the toolbar: the active flag of "G/R/S" (mode), of the undo/redo arrows (history), the number
fields (3 x 3 EditBoxes "From/To/Pivot", visible only while a cube is selected).
"""
import time

try:
    from PIL import Image, ImageChops
except ImportError:  # the pixel checks then fail with a clear message instead of an exception
    Image = ImageChops = None

EXAMPLES = (('Stool', 'minecraft:oak_slab', 'stool'), ('Table Lantern', 'minecraft:lantern', 'table_lantern'), ('Mailbox', 'minecraft:barrel', 'mailbox'),
            ('Birdhouse', 'minecraft:oak_fence', 'birdhouse'), ('Weather Vane', 'minecraft:lightning_rod', 'weather_vane'),
            ('Crystal Cluster', 'minecraft:amethyst_cluster', 'crystal_cluster'))
NAMES = [e[0] for e in EXAMPLES]


def _img(s, name):
    s.shot(name)
    if Image is None:
        return None
    try:
        return Image.open(s.out / 'shots' / f'{s.scenario}-{name}.png').convert('RGB')
    except Exception:  # noqa: BLE001 - a read problem is a failed check, not an exception
        return None


def _changed(a, b, box=None):
    if a is None or b is None or a.size != b.size:
        return -1
    if box:
        a, b = a.crop(box), b.crop(box)
    return sum(ImageChops.difference(a, b).convert('L').histogram()[25:])


def _rows(s, st=None):
    """Labels of the model rows (Buttons named like an example) in the order of the screen."""
    return [w['label'] for w in s.widgets(st) if w['class'] == 'Button' and w['label'] in NAMES]


def _wait_rows(s, count, seconds=6):
    end = time.time() + seconds
    while time.time() < end:
        st = s.st()
        if len(_rows(s, st)) == count:
            return st
        time.sleep(0.4)
    return s.st()


def _view_rect(gw, gh):
    """ModelEditorLayout.of(gw, gh, false): the 3D view (x, y, w, h) and the width of the cube list (0 = folded away) in gui pixels."""
    inner = gw - 12
    list_w = max(84, min(124, inner * 18 // 100))
    props_w = max(104, min(140, inner * 22 // 100))
    if inner - list_w - props_w - 6 < 120:
        props_w = 0
    if inner - list_w - (props_w + 3 if props_w else 0) - 3 < 120:
        list_w = 0
    view_w = inner - (list_w + 3 if list_w else 0) - (props_w + 3 if props_w else 0)
    return 6 + (list_w + 3 if list_w else 0), 29, view_w, gh - 49, list_w


def _active(s, label, cls='Button', st=None):
    w = s.find_widget(label, cls, st, contains=False)
    return None if w is None else w['active']


def browser(s):
    s.chat('gamemode survival @s', 0.8)
    s.chat('clear @s', 0.4)
    for name, base, model in EXAMPLES:
        s.chat(f'give @s {base}[item_model="simplemodels:example/{model}"]', 0.4)
    s.key('1', 0.4)
    # ---- inventory with the six examples (look: human)
    s.key('e', 1.0)
    st = s.wait_screen('InventoryScreen', 10)
    s.check('E opens the survival inventory', 'InventoryScreen' in st.get('screen', ''), st.get('screen'))
    s.shot('six-examples-in-hotbar')
    tab = s.find_widget(cls='ModelsTab', st=st)
    s.check('the Models bookmark is a widget of the inventory', tab is not None, s.labels(st))
    if not tab:
        return
    s.click_widget(cls='ModelsTab', name='Models bookmark')
    st = s.wait_screen('ModelBrowser', 10)
    s.check('real click on the Models bookmark opens the browser', 'ModelBrowser' in st.get('screen', ''), st.get('screen'))
    if 'ModelBrowser' not in st.get('screen', ''):
        return
    time.sleep(1.0)
    st = s.st()
    s.check('tabs Models / Server Settings / Import Help and Editor + Back are there', all(s.find_widget(t, 'Button', st, contains=False) for t in
            ('Models', 'Server Settings', 'Import Help', 'Editor', 'Back')), s.labels(st))
    s.check('current tab "Models" is greyed out, the others are active', _active(s, 'Models', st=st) is False and _active(s, 'Server Settings', st=st) is True,
            (_active(s, 'Models', st=st), _active(s, 'Server Settings', st=st)))
    st = _wait_rows(s, 6)
    rows = _rows(s, st)
    s.check('the 6 example models are listed (A-Z)', rows == sorted(NAMES), rows)
    s.shot('browser-models')
    # ---- select a row with the real mouse (preview + names at the bottom: human)
    if s.click_widget('Mailbox', 'Button', 'row Mailbox'):
        time.sleep(1.0)
        s.shot('browser-selected-mailbox')
        # hover another row: tooltip with the preview image
        st = s.st()
        w = s.find_widget('Weather Vane', 'Button', st)
        if w:
            s.io.move(*s.abs(st, w['x'], w['y']))
            time.sleep(1.5)
            s.shot('browser-row-tooltip')
    # ---- search box: type, rows shrink, clear
    st = s.st()
    box = s.find_widget(cls='EditBox', st=st)
    s.check('search EditBox is there', box is not None, s.labels(st))
    if box:
        s.click_xy(box['x'], box['y'], st)
        s.type_text('lantern')
        st = _wait_rows(s, 1)
        s.check('typing "lantern" leaves only the Table Lantern row', _rows(s, st) == ['Table Lantern'], _rows(s, st))
        s.shot('browser-search-lantern')
        for _ in range(7):
            s.io.key('BackSpace', 0.1)
        st = _wait_rows(s, 6)
        s.check('clearing the search lists the 6 models again', len(_rows(s, st)) == 6, _rows(s, st))
    # ---- sort cycle button: Name A-Z -> Name Z-A (first row Weather Vane) -> Base Item -> A-Z
    st = s.st()
    sort = s.find_widget('Sort', 'CycleButton', st)
    s.check('sort cycle button present', sort is not None, s.labels(st))
    if sort:
        s.click_widget('Sort', 'CycleButton', 'sort button')
        st = _wait_rows(s, 6)
        s.check('sort click 1: Name Z-A, Weather Vane first', _rows(s, st)[:1] == ['Weather Vane'], (_rows(s, st), s.find_widget('Sort', 'CycleButton', st)))
        s.click_widget('Sort', 'CycleButton', 'sort button')
        time.sleep(1.0)
        st = s.st()
        s.check('sort click 2: Base Item order', len(_rows(s, st)) == 6 and _rows(s, st) != sorted(NAMES), _rows(s, st))
        s.shot('browser-sort-base-item')
        s.click_widget('Sort', 'CycleButton', 'sort button')
        st = _wait_rows(s, 6)
        s.check('sort click 3: back to Name A-Z', _rows(s, st) == sorted(NAMES), _rows(s, st))
    # ---- item filter cycle: first base item -> exactly one example
    item = s.find_widget('Item', 'CycleButton', s.st())
    if item:
        s.click_widget('Item', 'CycleButton', 'item filter')
        st = _wait_rows(s, 1)
        s.check('item filter click 1 narrows the list to one model', len(_rows(s, st)) == 1, _rows(s, st))
        s.shot('browser-filter-one-item')
        for _ in range(8):  # the filter wraps back to "All"
            if len(_rows(s, s.st())) == 6:
                break
            s.click_widget('Item', 'CycleButton', 'item filter')
            time.sleep(0.8)
        s.check('the filter cycles back to All (6 rows)', len(_rows(s, s.st())) == 6, _rows(s, s.st()))
    else:
        s.check('item filter cycle button present', False, s.labels(s.st()))
    # ---- the other tabs
    if s.click_widget('Server Settings', 'Button', 'tab Server Settings'):
        time.sleep(0.8)
        st = s.st()
        s.check('Server Settings tab: Models tab is active again, rows are gone', _active(s, 'Models', st=st) is True and not _rows(s, st), s.labels(st))
        s.shot('browser-server-settings')
    if s.click_widget('Import Help', 'Button', 'tab Import Help'):
        time.sleep(0.8)
        s.shot('browser-import-help')
    s.click_widget('Models', 'Button', 'tab Models')
    time.sleep(1.0)
    s.click_widget('Mailbox', 'Button', 'row Mailbox')
    time.sleep(0.8)

    # ---- editor
    if not s.click_widget('Editor', 'Button', 'Editor button'):
        return
    st = s.wait_screen('ModelEditorScreen', 10)
    s.check('real click on Editor opens the model editor', 'ModelEditorScreen' in st.get('screen', ''), st.get('screen'))
    if 'ModelEditorScreen' not in st.get('screen', ''):
        s.close()
        return
    time.sleep(1.5)
    st = s.st()
    toolbar = ('G', 'R', 'S', '↶', '↷', '+', '⧉', '✕', '?', '</>', 'File', 'Back')
    s.check('toolbar buttons G R S undo redo + duplicate delete ? </> File Back', all(s.find_widget(t, 'Button', st, contains=False) for t in toolbar),
            [t for t in toolbar if not s.find_widget(t, 'Button', st, contains=False)])
    s.shot('editor-open')
    sc = st['scale']
    gw, gh = 1280 // sc, 720 // sc
    vx, vy, vw, vh, list_w = _view_rect(gw, gh)
    view = (vx * sc, vy * sc, (vx + vw) * sc, (vy + vh) * sc)
    cx, cy = (vx + vw // 2) * sc, (vy + vh // 2) * sc
    s.check('mode G is the active mode (button inactive), R is clickable', _active(s, 'G', st=st) is False and _active(s, 'R', st=st) is True,
            (_active(s, 'G', st=st), _active(s, 'R', st=st)))
    s.click_widget('R', 'Button', 'mode R')
    time.sleep(0.6)
    st = s.st()
    s.check('click on R switches the mode (R inactive, G active)', _active(s, 'R', st=st) is False and _active(s, 'G', st=st) is True, (_active(s, 'R', st=st), _active(s, 'G', st=st)))
    s.click_widget('G', 'Button', 'mode G')
    s.check('undo arrow starts inactive (nothing changed yet)', _active(s, '↶', st=s.st()) is False, _active(s, '↶', st=s.st()))
    # view: wheel zoom and right-drag orbit change the picture of the view area
    before = _img(s, 'editor-view-before')
    s.wheel(cx, cy, clicks=3, down=False, st=st)
    zoomed = _img(s, 'editor-view-zoomed')
    n = _changed(before, zoomed, view)
    s.check('mouse wheel zooms the view (view area changes)', n > 1500, n)
    s.wheel(cx, cy, clicks=3, down=True, st=st)
    s.io.move(*s.abs(st, cx, cy))
    time.sleep(0.4)
    s.io.button(3, True)
    time.sleep(0.2)
    for k in range(1, 9):  # a hand's pace: the game applies the cursor once a frame
        s.io.move(*s.abs(st, cx + k * 12, cy + k * 4))
        time.sleep(0.25)
    time.sleep(0.5)
    s.io.button(3, False)
    time.sleep(0.5)
    turned = _img(s, 'editor-view-orbited')
    n = _changed(before, turned, view)
    s.check('right-drag turns the model (view area changes)', n > 1500, n)
    s.key('Home', 0.8)  # reset camera
    # selecting: click the view (best effort, the exact hit depends on the model) or the first row of the cube list
    selected = False
    for dx, dy in ((0, 0), (-50, 0), (50, 0), (0, -50), (0, 50)):  # window pixels around the view centre
        s.click_xy(cx + dx, cy + dy, st)
        time.sleep(0.6)
        if s.find_widget(cls='EditBox', st=s.st()):
            selected = True
            break
    s.check('left click in the view selects a cube (number fields appear)', selected, s.labels(s.st()))
    if not selected:
        s.click_xy((6 + max(20, list_w // 2)) * sc, (29 + 14 + 6) * sc, st)  # first row of the cube list (rows 12 px high from y = 29 + 14)
        time.sleep(0.6)
        s.check('click on the first row of the cube list selects a cube', s.find_widget(cls='EditBox', st=s.st()) is not None, s.labels(s.st()))
    s.shot('editor-cube-selected')
    boxes = [w for w in s.widgets() if w['class'] == 'EditBox']
    s.check('9 number fields (From / To / Pivot x 3) are shown for the cube', len(boxes) == 9, [(w['label'], w.get('value')) for w in boxes])
    # + new cube (selected automatically), undo, redo
    s.click_widget('+', 'Button', 'new cube', )
    time.sleep(0.6)
    st = s.st()
    s.check('"+" makes the undo arrow clickable', _active(s, '↶', st=st) is True, _active(s, '↶', st=st))
    s.shot('editor-new-cube')
    s.click_widget('↶', 'Button', 'undo')
    time.sleep(0.6)
    st = s.st()
    s.check('undo: redo arrow becomes active', _active(s, '↷', st=st) is True, _active(s, '↷', st=st))
    # face tab
    s.click_widget('Face', 'Button', 'tab Face')
    time.sleep(0.8)
    st = s.st()
    s.check('click on the Face tab: Face inactive, Cube active', _active(s, 'Face', st=st, ) is False and _active(s, 'Cube', st=st) is True, (_active(s, 'Face', st=st), _active(s, 'Cube', st=st)))
    s.shot('editor-face-tab')
    s.click_widget('Cube', 'Button', 'tab Cube')
    # help (F1), code only (</> <-> 3D)
    s.key('F1', 0.8)
    s.shot('editor-help')
    s.key('Escape', 0.8)
    st = s.st()
    s.check('Escape closes the help but not the editor', 'ModelEditorScreen' in st.get('screen', ''), st.get('screen'))
    if s.click_widget('</>', 'Button', 'code only'):
        time.sleep(1.2)
        st = s.st()
        s.check('code-only view: the button now reads 3D and the cube list/properties are gone', s.find_widget('3D', 'Button', st, contains=False) is not None, s.labels(st))
        s.shot('editor-code-only')
        s.click_widget('3D', 'Button', 'back to 3D')
        time.sleep(1.0)
    # file dialog
    if s.click_widget('File', 'Button', 'File button'):
        st = s.wait_screen('ModelFileScreen', 8)
        s.check('File opens the model file screen', 'ModelFileScreen' in st.get('screen', ''), st.get('screen'))
        s.shot('editor-file-screen')
        s.click_widget('Back', 'Button', 'file Back')
        st = s.wait_screen('ModelEditorScreen', 8)
        s.check('Back of the file screen returns to the editor', 'ModelEditorScreen' in st.get('screen', ''), st.get('screen'))
    # leave: editor Back -> browser, browser Back -> inventory
    s.click_widget('Back', 'Button', 'editor Back')
    st = s.wait_screen('ModelBrowser', 8)
    s.check('editor Back returns to the browser', 'ModelBrowser' in st.get('screen', ''), st.get('screen'))
    s.click_widget('Back', 'Button', 'browser Back')
    st = s.wait_screen('InventoryScreen', 8)
    s.check('browser Back returns to the inventory', 'InventoryScreen' in st.get('screen', ''), st.get('screen'))
    s.close()


SCENARIOS = {'simplemodels': browser}
