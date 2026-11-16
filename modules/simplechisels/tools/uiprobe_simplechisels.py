"""simplechisels-3 (offen-vor-release B5): chisel, Fast Chiseling and the chisel at an item frame with the REAL mouse, for the owner card.
Scenario for modules/simpleinterfaces/tools/ui_probe.py (survival, so the chisel cooldown and durability apply like for a player):

  simplechisels/release
    1. Stone Chisel in hand facing smooth sandstone (stone table: smooth -> cut -> sandstone -> chiseled), real right click: the block
       turns into the next one of its chain (shots before / after; hand hint and cooldown sweep on the hotbar).
    2. Fast Chiseling (Schnelles Meisseln): two right clicks 1.0 s apart. The plain Stone Chisel waits 30 ticks (1.5 s) after a step,
       so only ONE step happens; with Fast Chiseling II the wait is 30 x 0.4 = 12 ticks (0.6 s), so BOTH steps happen
       (ChiselItem.tryChiselBlock). Tooltip of the enchanted chisel in the inventory.
    3. Sneak + right click walks one step back (costs 2 durability).
    4. Chisel at an item frame (simplevisuals FrameChisel, BS25-7): item frame placed and filled by real right clicks, then sneak + right
       click with the chisel tilts the framed item one 15-degree step per click (three shots). The tilt is a synced entity datum the probe
       cannot read: only the pictures show it.
    5. Config screen through Mod Menu ("simple chisels"): option names are lang texts (shared step, simplelib/tools/release_probe.py).

Only a human can judge: the chisel models in the hand, particles and sound, the cooldown sweep, the tilt angles in the frame.
"""
import importlib.util
import time
from pathlib import Path

_spec = importlib.util.spec_from_file_location('release_probe', Path(__file__).resolve().parents[2] / 'simplelib/tools/release_probe.py')
rp = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(rp)

CHAIN = ['minecraft:smooth_sandstone', 'minecraft:cut_sandstone', 'minecraft:sandstone', 'minecraft:chiseled_sandstone']


def _tap(s, gap):
    """One short real right click (shorter than Vanilla's 4-tick repeat), then wait gap seconds."""
    s.io.button(3, True)
    time.sleep(0.08)
    s.io.button(3, False)
    time.sleep(gap)


def _hit(s, want=None, seconds=3.0):
    st = rp.wait(s, lambda t: want is None or t.get('hitBlock') == want, seconds)
    return st.get('hitBlock')


def _count(st, item):
    n = 0
    for text in st.get('inventory', []):
        if text and text.split(' x')[0] == item:
            n += int(text.rsplit('x', 1)[1])
    return n


def release(s):
    s.chat('time set day', 0.3)
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.3)
    s.chat('item replace entity @s hotbar.0 with simplechisels:stone_chisel', 0.3)
    s.chat('item replace entity @s hotbar.1 with simplechisels:stone_chisel[enchantments={"simplechisels:fast_chiseling":2}]', 0.3)
    s.chat('item replace entity @s hotbar.2 with minecraft:item_frame 4', 0.3)
    s.chat('item replace entity @s hotbar.3 with minecraft:diamond_sword', 0.3)
    s.key('1', 0.5)
    st = s.st()
    s.check('stone chisel and the Fast Chiseling II chisel are in the hotbar',
            (st.get('inventory') or [''])[0].startswith('simplechisels:stone_chisel') and (st.get('inventory') or ['', ''])[1].startswith('simplechisels:stone_chisel'),
            st.get('inventory', [])[:4])

    # ---- 1: one step forward with the plain chisel
    block = s.stage(CHAIN[0], 2, 0, 0)
    hit = _hit(s, CHAIN[0])
    s.check('crosshair on smooth sandstone', hit == CHAIN[0], hit)
    s.shot('chisel-before')
    _tap(s, 0.9)
    hit = _hit(s, CHAIN[1])
    s.check('real right click with the Stone Chisel: smooth sandstone -> cut sandstone', hit == CHAIN[1], hit)
    s.shot('chisel-after-cooldown')
    time.sleep(1.5)  # the 30-tick cooldown runs out

    # ---- 2: Fast Chiseling: two clicks 1.0 s apart, plain chisel = one step, Fast Chiseling II = two steps
    s.chat(f'setblock {block[0]} {block[1]} {block[2]} {CHAIN[0]}', 0.6)
    _tap(s, 1.0)
    _tap(s, 0.8)
    plain_hit = _hit(s, None, 1.0)
    s.check('plain Stone Chisel: the second click inside the 1.5 s cooldown does nothing (one step only)', plain_hit == CHAIN[1], plain_hit)
    time.sleep(1.6)
    s.key('2', 0.5)
    s.chat(f'setblock {block[0]} {block[1]} {block[2]} {CHAIN[0]}', 0.6)
    _tap(s, 1.0)
    _tap(s, 0.8)
    fast_hit = _hit(s, CHAIN[2], 2.0)
    s.check('Fast Chiseling II: the cooldown drops to 0.6 s, both clicks reshape (two steps)', fast_hit == CHAIN[2], fast_hit)
    s.shot('fast-chiseling-two-steps')

    # ---- 3: sneak + right click one step back
    time.sleep(1.0)
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.4)
        _tap(s, 0.8)
    finally:
        s.io.key_up('Shift_L')
    back = _hit(s, CHAIN[1], 2.0)
    s.check('sneak + right click walks one step back (sandstone -> cut sandstone)', back == CHAIN[1], back)
    s.shot('chisel-step-back')

    # tooltip of the enchanted chisel (inventory, real pointer)
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 8)
    if 'InventoryScreen' in st.get('screen', ''):
        rp.hover_slot(s, st, s.slot_xy(st, cslot=1, own=True), 'tooltip-fast-chiseling')
        rp.hover_slot(s, st, s.slot_xy(st, cslot=0, own=True), 'tooltip-stone-chisel')
    else:
        s.check('E opens the inventory', False, st.get('screen'))
    s.close()
    rp.park_pointer(s)

    # ---- 4: chisel at an item frame
    x, y, z = rp.xyz(s.st())
    s.chat(f'setblock {x} {y} {z + 3} minecraft:stone_bricks', 0.4)
    s.chat(f'setblock {x} {y + 1} {z + 3} minecraft:stone_bricks', 0.4)
    s.key('3', 0.5)
    frames_before = _count(s.st(), 'minecraft:item_frame')
    rp.aim(s, x + 0.5, y + 1.5, z + 3.0)  # north face of the upper block, seen from the south side of it
    s.io.right_click()
    time.sleep(0.6)
    st = rp.wait(s, lambda t: t.get('hitEntity') == 'minecraft:item_frame', 3)
    frames_after = _count(st, 'minecraft:item_frame')
    s.check('real right click places an item frame on the wall', st.get('hitEntity') == 'minecraft:item_frame' and frames_after == frames_before - 1,
            f"hitEntity={st.get('hitEntity')} frames {frames_before}->{frames_after}")
    s.key('4', 0.5)
    s.io.right_click()
    time.sleep(0.8)
    st = s.st()
    s.check('real right click puts the diamond sword into the frame', _count(st, 'minecraft:diamond_sword') == 0, st.get('inventory', [])[:5])
    s.key('1', 0.6)
    s.shot('frame-flat')
    for n in (1, 2, 3):
        s.io.key_down('Shift_L')
        try:
            time.sleep(0.4)
            s.io.right_click()
        finally:
            s.io.key_up('Shift_L')
        time.sleep(0.6)
        s.shot(f'frame-tilt-{n * 15}deg')
    st = s.st()
    s.check('the frame (with the sword) is still there after three chisel clicks', st.get('hitEntity') == 'minecraft:item_frame', st.get('hitEntity'))

    # ---- 5: config screen
    s.chat('gamemode creative @s', 0.5)
    rp.config_screen(s, 'simplechisels', 'simple chisels', 'Simple Chisels')


SCENARIOS = {'simplechisels/release': release}
