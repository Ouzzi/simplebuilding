"""simpleresonance-1 (offen-vor-release B5): the resonance panel in the player inventory and the player value, for the owner card.
Scenario for modules/simpleinterfaces/tools/ui_probe.py (survival: the panel sits in the survival inventory, InventoryScreenResonanceMixin):

  simpleresonance/release
    1. Some experience first (xp add), so the value is not the bare minimum; E opens the inventory: the 19x19 button with the heart
       right of the player model (ResonanceLayout.INVENTORY_X/Y = 76/34 from leftPos/topPos; leftPos/topPos from the crafting result
       slot, menu index 0, centre 162/36). Shots: inventory, pointer on the button (blue frame + "Resonance: x (max y)" = the player
       value), pointer on the button with Shift held (every factor line). With PIL the button's pixels are compared: hovered != not hovered.
    2. /resonance: the player value in the chat ("Your resonance value: ... (level ... of 5)"), shot.
    3. Config screen through Mod Menu ("simple resonance"), option names from the lang file.

Only a human can judge: the heart tier sprite, the frame colours, the wording and colours of the tooltip lines.
"""
import importlib.util
import time
from pathlib import Path

try:
    from PIL import Image
except ImportError:  # the pixel check is then skipped with a note
    Image = None

_spec = importlib.util.spec_from_file_location('release_probe', Path(__file__).resolve().parents[2] / 'simplelib/tools/release_probe.py')
rp = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(rp)


def _button(st):
    """Centre of the resonance button and its half size in window pixels, from the crafting result slot (menu index 0)."""
    slot = next((x for x in st.get('slots', []) if x['index'] == 0), None)
    if not slot:
        return None
    sc = st['scale']
    left, top = slot['x'] / sc - 162, slot['y'] / sc - 36
    return (left + 76 + 9.5) * sc, (top + 34 + 9.5) * sc, 9 * sc


def _crop_mean(s, name, box):
    if Image is None:
        return None
    try:
        img = Image.open(s.out / 'shots' / f"{s.scenario.replace('/', '-')}-{name}.png").convert('RGB')
    except OSError:
        return None
    px = list(img.crop(box).getdata())
    return tuple(sum(p[i] for p in px) / max(1, len(px)) for i in range(3))


def release(s):
    s.chat('time set day', 0.3)
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.3)
    s.chat('xp add @s 300 points', 0.5)
    time.sleep(1.5)  # the server sends simpleresonance:stats on its own schedule
    rp.park_pointer(s)
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 8)
    ok = 'InventoryScreen' in st.get('screen', '')
    s.check('E opens the survival inventory', ok, st.get('screen'))
    if not ok:
        return
    b = _button(st)
    s.check('crafting result slot found (button position)', b is not None, len(st.get('slots', [])))
    if not b:
        return
    bx, by, half = b
    # pointer away from the button first: plain button
    s.io.move(*s.abs(st, bx - 120, by + 120))
    time.sleep(0.8)
    s.shot('inventory-panel')
    rp.hover(s, st, bx, by, 'panel-hover-value')
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.8)
        s.shot('panel-hover-shift-all-factors')
    finally:
        s.io.key_up('Shift_L')
    box = (int(bx - half + 2), int(by - half + 2), int(bx + half - 2), int(by + half - 2))
    plain, hovered = _crop_mean(s, 'inventory-panel', box), _crop_mean(s, 'panel-hover-value', box)
    if plain is None or hovered is None:
        s.check('(info) button pixel check skipped (PIL or shots missing)', True, (plain, hovered))
    else:
        s.check('the resonance button is drawn and lights up under the pointer (pixels differ)', max(abs(a - c) for a, c in zip(plain, hovered)) > 4,
                (plain, hovered))
    s.close()
    rp.park_pointer(s)
    # ---- the player value in the chat
    s.chat('resonance', 1.2)
    s.shot('chat-player-value')
    s.chat('gamemode creative @s', 0.5)
    rp.config_screen(s, 'simpleresonance', 'simple resonance', 'Simple Resonance')


SCENARIOS = {'simpleresonance/release': release}
