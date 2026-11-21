"""N127 release polish, mods 26-28 (offen-vor-release.md section 3, "Szenen" of Simple Lights, Simple Gadgets, Simple Music), word for word:

Simple Lights   "Optik der Scheiben (W4C-24)."
  n127-lights-panes     A stone wall with all 37 panes hung on it (setblock facing=north = looking away from the wall): row 1 the 18 plain
                        panes (normal, transparent, 16 colors) + the Redstone Light Pane, row 2 the 17 copper panes + the Copper Redstone
                        Light Pane; one more stone block with a pane on top, on its north and on its west side. Order as in the lamp lesson:
                        panes first, power after (redstone blocks next to each pane). Checks per pane (server, say tags): placed dark;
                        powered -> plain lit=true, redstone pane color != 16, copper flipped to lit=true; power gone -> plain dark, redstone
                        pane off, copper STAYS lit (Copper Bulb behaviour); power again -> copper flips dark. Day + night shots of each
                        step, a close-up at night. The look itself stays with the owner (W4C-24).
Simple Gadgets  "Messuhr-HUD, Resonanzstab, Detektor, Echolot, Rotator, Magnet/Transposer (W4C gadgets-4)."
  n127-gadgets-gauge    Velocity Gauge in the main hand (weapon.mainhand read back): HUD standing, while sprinting with the REAL keys
                        (Ctrl+W, moved >= 8 blocks) and 21 blocks up (altimeter line); shots.
  n127-gadgets-rod      Amethyst Lens (Resonance Rod) with Constructor's Touch: crosshair read back on an ice block 6 blocks away, REAL right
                        button held 4 s -> beam + measuring HUD shot, the ice melts.
  n127-gadgets-detector Detector in survival: iron ore 8 blocks ahead, pings cost durability (damage component), inventory tooltip shot.
  n127-gadgets-echo     Echo Sounder: REAL click on a lodestone (crosshair read back) links it, 20 blocks away the held right button
                        (3.8 s) jumps back to the lodestone.
  n127-gadgets-rotator  Transposer (registry id rotator): REAL click on the top face of a piston (crosshair read back first - the W4C run
                        clicked while the camera still turned) turns it, four clicks bring it back to north; a furnace: a plain click opens
                        its menu (known defect, RotatorTests "Known defect"), Shift held >= 0.8 s + click turns it.
  n127-gadgets-magnet   Attractor (registry id magnet): REAL Shift held >= 0.8 s (sneaking read back) -> three item stacks stay; Shift
                        released -> they fly into the inventory (12 cobblestone). Death part with Simple Tweaks T27 (keeps hotbar, armor,
                        offhand; main inventory drops): attractor in the hotbar, diamonds + cobblestone in the main inventory, /kill, REAL
                        click on Respawn -> attractor kept, the two stacks lie at the death spot (own death drops, DeathDropMarkMixin);
                        3.5 blocks away with an empty hand they stay, with the attractor selected they come back into the inventory.
Simple Music    "Notenblatt-Editor, Plattenpresse, Taschen-Jukebox (Rechtsklick-Abspielen/Platzieren)."
  n127-music-editor     modules/simplemusic/tools/uiprobe_simplemusic.py editor (typing, grid, import, example/listen, upload window,
                        sign) with the sheet read from Inventory[{Slot:0b}] (26.3 player data has no SelectedItem).
  n127-music-press      the same file's Disc Press scenario (real slot clicks, pressing, shift-click out).
  n127-music-pocket     Pocket Jukebox: discs in/out by real inventory clicks; in the hand (weapon.mainhand read back) REAL right click in
                        the air plays (simplemusic:pocket id on the stack), again pauses (position stored); REAL Shift held >= 0.8 s +
                        right click on the floor (crosshair read back) places it; empty-hand right click on the block plays
                        (playing=true); Shift + REAL wheel on it moves the shown disc (block entity Shown changes). The W4C reds were the
                        probe (SelectedItem, Shift without hold time).
  Sounds: all Simple Music discs are ElevenLabs takes (CREDITS.md) - no synthetic sound, no listening page; W4C-35 stays the owner's.

Patterns (uiprobe_n122-n125 lessons): position via int(float()) of status 'pos'; 'tp ... facing' with y - 1.62; real clicks only after the
crosshair target was read back (status hitBlock); main hand = selected hotbar slot; Shift held >= 0.8 s; chat commands ~1-1.5 s each;
counts from "Test passed. Count: N"; random_tick_speed 0 while a scene must not change (restored to 3). No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe127 --scale 2 \\
        --only n127-lights-panes,n127-gadgets-gauge,n127-gadgets-rod,n127-gadgets-detector,n127-gadgets-echo,n127-gadgets-rotator,n127-gadgets-magnet,n127-music-editor,n127-music-press,n127-music-pocket
"""
import importlib.util
import re
import time
from pathlib import Path

TAG = 'N127'
REPO = Path(__file__).resolve().parents[3]
FLOOR = 'minecraft:smooth_stone'


def _load(name, rel):
    spec = importlib.util.spec_from_file_location(name, REPO / rel)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


# ---------------------------------------------------------------- helpers
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _log(s):
    p = s.out / 'client.log'
    return p.read_text(errors='replace') if p.exists() else ''


def _say(s, cond, tag):
    s.chat(f'execute {cond} run say {TAG} {tag}', 0.35)


def _said(s, tags, settle=1.5):
    time.sleep(settle)
    log = _log(s)
    return {t: bool(re.search(TAG + ' ' + re.escape(t) + r'(?![\w-])', log)) for t in tags}


def _check_said(s, name, tags):
    got = _said(s, tags)
    miss = [t for t, ok in got.items() if not ok]
    s.check(name, not miss, f'{len(tags) - len(miss)}/{len(tags)}' + (f' missing {miss[:12]}' if miss else ''))
    return not miss


def _count(s, tag, selector):
    """'execute if entity <selector>': the number from the feedback line "Test passed. Count: N" after a marker line (None = no line)."""
    s.chat(f'say {TAG} COUNT {tag}', 0.3)
    s.chat(f'execute if entity {selector}', 0.35)
    time.sleep(1.5)
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} COUNT {tag}')), default=-1)
    for ln in lines[idx + 1: idx + 8] if idx >= 0 else []:
        m = re.search(r'Test passed[.,] [Cc]ount: (\d+)', ln)
        if m:
            return int(m.group(1))
        if 'Test failed' in ln:
            return 0
    return None


def _inv_count(st, item):
    n = 0
    for text in st.get('inventory', []):
        m = re.fullmatch(re.escape(item) + r' x(\d+)', text or '')
        if m:
            n += int(m.group(1))
    return n


def _prep(s, mode='creative', night_vision=True):
    s.close()
    s.io.button(3, False)
    s.io.key_up('Shift_L')
    s.chat('ride @s dismount', 0.3)
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true', 'keep_inventory false'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('effect clear @s', 0.3)
    if night_vision:
        s.chat('effect give @s night_vision 99999 0 true', 0.3)
    s.chat('clear @s', 0.3)


def _arena(s, dx, x0=-3, z0=-3, x1=14, z1=24, height=6):
    """Moves dx blocks east, lays a smooth stone floor under fresh air (x0..x1, z0..z1 around the feet) and returns the feet position."""
    x, y, z = _pos(s)
    x += dx
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.2)
    s.chat(f'fill {x + x0} {y} {z + z0} {x + x1} {y + height} {z + z1} minecraft:air', 0.6)
    s.chat(f'fill {x + x0} {y - 1} {z + z0} {x + x1} {y - 1} {z + z1} {FLOOR}', 0.6)
    s.chat('kill @e[type=minecraft:item,distance=..40]', 0.3)
    time.sleep(1.0)
    return x, y, z


def _aim(s, at, target, want, tag, tries=4):
    """Feet at `at`, eyes onto the world point `target` (tp facing from the feet: y - 1.62); waits until the crosshair reads back block
    `want` (None = no block). True when it does."""
    hit = None
    for _ in range(tries):
        s.chat(f'tp @s {at[0]:.2f} {at[1]} {at[2]:.2f} facing {target[0]:.3f} {target[1] - 1.62:.3f} {target[2]:.3f}', 0.8)
        for _ in range(6):
            hit = s.st().get('hitBlock')
            if hit == want:
                return True
            time.sleep(0.3)
    s.check(f'{tag}: crosshair on {want}', False, hit)
    return False


def _hand(s, slot, stack, tag):
    """Puts the stack into hotbar slot `slot`, selects it with the REAL number key and reads the main hand back."""
    s.chat(f'item replace entity @s hotbar.{slot} with {stack}', 0.4)
    s.io.key(str(slot + 1), 0.5)
    item = stack.split('[', 1)[0].split(' ', 1)[0]
    _say(s, f'if items entity @s weapon.mainhand {item}', f'{tag}-mainhand')
    return _check_said(s, f'{tag}: {item} is in the main hand (selected hotbar slot {slot + 1})', [f'{tag}-mainhand'])


def _click(s, after=0.7, button=3):
    s.io.button(button, True)
    time.sleep(0.12)
    s.io.button(button, False)
    time.sleep(after)


def _sneak_click(s, tag, button=3):
    """REAL Shift held >= 0.8 s (sneaking read back), then the click."""
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.9)
        s.check(f'{tag}: the client sneaks before the click (real Shift held)', bool(s.st().get('sneaking')), s.last.get('sneaking'))
        _click(s, 0.6, button)
    finally:
        s.io.key_up('Shift_L')
    time.sleep(0.4)


def _center_pointer(s):
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)
    time.sleep(0.3)


# ---------------------------------------------------------------- Simple Lights
COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue',
          'brown', 'green', 'red', 'black']
ROW_PLAIN = ['light_pane', 'transparent_light_pane'] + [f'{c}_light_pane' for c in COLORS] + ['redstone_light_pane']
ROW_COPPER = ['copper_light_pane'] + [f'{c}_copper_light_pane' for c in COLORS] + ['copper_redstone_light_pane']


def _pane_state(name, on):
    """Block state predicate of a pane in the plain (on = powered) or flipped (copper) sense."""
    if name == 'redstone_light_pane':
        return f'simplelights:{name}', ('unless', '[color=16]') if on else ('if', '[color=16]')
    return f'simplelights:{name}', ('if', f'[lit={"true" if on else "false"}]')


def _panes_are(s, cells, phase, want):
    """cells: [(name, x, y, z)], want(name) -> True for lit; one say tag per pane."""
    tags = []
    for name, px, py, pz in cells:
        block, (mode, props) = _pane_state(name, want(name))
        tag = f'{phase}-{name}'
        _say(s, f'{mode} block {px} {py} {pz} {block}{props}', tag)
        tags.append(tag)
    return tags


def lights_panes(s):
    _prep(s, night_vision=False)
    s.chat('gamerule random_tick_speed 0', 0.3)
    try:
        x, y, z = _arena(s, 40, x0=-4, z0=-8, x1=26, z1=8)
        wz = z + 6  # wall
        pz = z + 5  # panes in front of it, facing north (away from the wall, toward the player)
        s.chat(f'fill {x} {y} {wz} {x + 19} {y + 3} {wz} minecraft:stone', 0.5)
        cells = []
        for i, name in enumerate(ROW_PLAIN):
            s.chat(f'setblock {x + i} {y + 1} {pz} simplelights:{name}[facing=north]', 0.3)
            cells.append((name, x + i, y + 1, pz))
        for i, name in enumerate(ROW_COPPER):
            s.chat(f'setblock {x + i} {y + 2} {pz} simplelights:{name}[facing=north]', 0.3)
            cells.append((name, x + i, y + 2, pz))
        # one more block: panes on top, north and west (every side of a block)
        bx, bz = x + 22, z + 3
        s.chat(f'setblock {bx} {y} {bz} minecraft:stone', 0.3)
        s.chat(f'setblock {bx} {y + 1} {bz} simplelights:light_pane[facing=up]', 0.3)
        s.chat(f'setblock {bx} {y} {bz - 1} simplelights:orange_light_pane[facing=north]', 0.3)
        s.chat(f'setblock {bx - 1} {y} {bz} simplelights:transparent_light_pane[facing=west]', 0.3)
        time.sleep(1.0)
        _check_said(s, 'all 37 wall panes placed and dark (plain lit=false, redstone pane color 16, copper lit=false)',
                    _panes_are(s, cells, 'placed', lambda n: False))
        _say(s, f'if block {bx} {y + 1} {bz} simplelights:light_pane[facing=up]', 'side-up')
        _say(s, f'if block {bx} {y} {bz - 1} simplelights:orange_light_pane[facing=north]', 'side-north')
        _say(s, f'if block {bx - 1} {y} {bz} simplelights:transparent_light_pane[facing=west]', 'side-west')
        _check_said(s, 'panes stay on the top, north and west side of a single block', ['side-up', 'side-north', 'side-west'])
        view = (x + 10.5, y, z - 6.5)
        look = (x + 10.5, y + 1.5, wz)
        s.chat(f'tp @s {view[0]} {view[1]} {view[2]} facing {look[0]} {look[1] - 1.62:.2f} {look[2]}', 1.5)
        s.shot('1-day-dark')
        s.chat('time set midnight', 1.5)
        s.shot('2-night-dark')
        # power AFTER the panes (lamp lesson): a redstone block under each row-1 pane and above each row-2 pane
        s.chat(f'fill {x} {y} {pz} {x + len(ROW_PLAIN) - 1} {y} {pz} minecraft:redstone_block', 0.5)
        s.chat(f'fill {x} {y + 3} {pz} {x + len(ROW_COPPER) - 1} {y + 3} {pz} minecraft:redstone_block', 0.5)
        time.sleep(1.0)
        _check_said(s, 'powered: plain panes lit, redstone pane colored, copper panes flipped on',
                    _panes_are(s, cells, 'powered', lambda n: True))
        s.shot('3-night-powered')
        s.chat('time set noon', 1.5)
        s.shot('4-day-powered')
        s.chat('time set midnight', 1.0)
        s.chat(f'tp @s {x + 4.5} {y} {z + 1.5} facing {x + 4.5} {y + 1.5 - 1.62:.2f} {wz}', 1.5)
        s.shot('5-night-close-plain')
        s.chat(f'tp @s {x + 13.5} {y} {z + 1.5} facing {x + 13.5} {y + 1.5 - 1.62:.2f} {wz}', 1.5)
        s.shot('6-night-close-colors')
        # power gone: plain dark, copper keeps its light (Copper Bulb), then a second rising edge flips copper off
        s.chat(f'fill {x} {y} {pz} {x + len(ROW_PLAIN) - 1} {y} {pz} minecraft:air', 0.5)
        s.chat(f'fill {x} {y + 3} {pz} {x + len(ROW_COPPER) - 1} {y + 3} {pz} minecraft:air', 0.5)
        time.sleep(1.0)
        _check_said(s, 'power gone: plain and redstone panes dark, copper panes STAY lit',
                    _panes_are(s, cells, 'unpowered', lambda n: 'copper' in n))
        s.chat(f'tp @s {view[0]} {view[1]} {view[2]} facing {look[0]} {look[1] - 1.62:.2f} {look[2]}', 1.5)
        s.shot('7-night-copper-stays')
        s.chat(f'fill {x} {y + 3} {pz} {x + len(ROW_COPPER) - 1} {y + 3} {pz} minecraft:redstone_block', 0.5)
        time.sleep(1.0)
        _check_said(s, 'second rising edge: copper panes flip dark', _panes_are(s, [c for c in cells if 'copper' in c[0]], 'flipped', lambda n: False))
        s.shot('8-night-copper-flipped')
    finally:
        s.chat('gamerule random_tick_speed 3', 0.3)
        s.chat('time set noon', 0.3)


# ---------------------------------------------------------------- Simple Gadgets
def gadgets_gauge(s):
    _prep(s)
    x, y, z = _arena(s, 40)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 10', 1.0)
    if not _hand(s, 0, 'simplegadgets:velocity_gauge', 'gauge'):
        return
    _center_pointer(s)
    time.sleep(1.0)
    s.shot('1-hud-standing')
    start = _pos(s)
    s.io.key_down('Control_L')
    s.io.key_down('w')
    try:
        time.sleep(1.8)
        s.shot('2-hud-sprinting')
        time.sleep(0.8)
    finally:
        s.io.key_up('w')
        s.io.key_up('Control_L')
    end = _pos(s)
    s.check('gauge: REAL Ctrl+W sprints the player >= 8 blocks', abs(end[2] - start[2]) >= 8, f'{start} -> {end}')
    s.chat('effect give @s minecraft:slow_falling 30 0 true', 0.3)
    s.chat(f'tp @s {end[0] + 0.5} {end[1] + 21} {end[2] + 0.5} 0 35', 0.6)
    time.sleep(1.0)
    s.shot('3-hud-altimeter')
    s.chat('effect clear @s minecraft:slow_falling', 0.3)
    s.chat(f'tp @s {end[0] + 0.5} {end[1]} {end[2] + 0.5}', 0.5)


def gadgets_rod(s):
    _prep(s)
    x, y, z = _arena(s, 40)
    me = (x + 0.5, y, z + 0.5)
    s.chat(f'setblock {x} {y} {z + 6} minecraft:ice', 0.4)
    if not _hand(s, 1, 'simplegadgets:amethyst_lens[enchantments={"simplebuilding:constructors_touch":1}]', 'rod'):
        return
    if not _aim(s, me, (x + 0.5, y + 0.5, z + 6.5), 'minecraft:ice', 'rod'):
        return
    s.io.button(3, True)
    try:
        time.sleep(1.5)
        s.shot('1-beam')
        time.sleep(2.5)
    finally:
        s.io.button(3, False)
    time.sleep(0.6)
    _say(s, f'unless block {x} {y} {z + 6} minecraft:ice', 'ice-gone')
    _check_said(s, 'rod: the held beam melts the ice block 6 blocks away', ['ice-gone'])
    s.shot('2-after')


def gadgets_detector(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 40)
    s.chat(f'setblock {x} {y} {z + 8} minecraft:iron_ore', 0.3)
    s.chat(f'setblock {x + 3} {y} {z + 8} minecraft:diamond_ore', 0.3)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {x + 0.5} {y + 0.5 - 1.62:.2f} {z + 8.5}', 1.0)
    if not _hand(s, 5, 'simplegadgets:detector', 'detector'):
        return
    time.sleep(3.0)  # a ping every second finds the iron ore and costs one durability point each
    s.shot('1-hand')
    _say(s, 'if data entity @s Inventory[{Slot:5b}].components."minecraft:damage"', 'worn')
    _check_said(s, 'detector: pings cost durability (damage component after a few seconds)', ['worn'])
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 8)
    slot = s.slot_xy(st, cslot=5, own=True) if 'InventoryScreen' in st.get('screen', '') else None
    s.check('inventory shows the detector in hotbar slot 6', bool(slot) and 'detector' in slot.get('stack', ''), slot)
    if slot:
        s.io.move(*s.abs(st, slot['x'] + 3, slot['y'] + 3))
        time.sleep(0.4)
        s.io.move(*s.abs(st, slot['x'], slot['y']))
        time.sleep(0.9)
        s.shot('2-inventory-tooltip')
    s.close()
    _center_pointer(s)
    s.chat('gamemode creative @s', 0.3)


def gadgets_echo(s):
    _prep(s)
    x, y, z = _arena(s, 40, x0=-14)
    lx, lz = x + 10, z + 10
    s.chat(f'setblock {lx} {y} {lz} minecraft:lodestone', 0.4)
    if not _hand(s, 4, 'simplegadgets:echo_sounder', 'echo'):
        return
    if not _aim(s, (lx - 1.5, y, lz + 0.5), (lx + 0.5, y + 0.9, lz + 0.5), 'minecraft:lodestone', 'echo'):
        return
    _click(s, after=1.0)
    _say(s, 'if data entity @s Inventory[{Slot:4b}].components."minecraft:lodestone_tracker"', 'linked')
    _check_said(s, 'echo sounder: a REAL click on the lodestone links it (lodestone_tracker)', ['linked'])
    time.sleep(5.0)  # the attempt lock after linking
    s.chat(f'tp @s {lx - 20.5} {y} {lz + 0.5} -90 0', 1.6)
    s.io.button(3, True)
    try:
        time.sleep(2.0)
        s.shot('1-charging')
        time.sleep(1.8)
    finally:
        s.io.button(3, False)
    time.sleep(1.5)
    end = _pos(s)
    s.check('echo sounder: the charged jump lands at the linked lodestone', abs(end[0] - lx) <= 2 and abs(end[2] - lz) <= 2,
            f'now {end}, lodestone ({lx}, {y}, {lz})')
    s.shot('2-arrived')


def gadgets_rotator(s):
    _prep(s)
    x, y, z = _arena(s, 40)
    me = (x + 0.5, y, z + 0.5)
    s.chat(f'setblock {x} {y} {z + 3} minecraft:piston[facing=north]', 0.4)
    s.chat(f'setblock {x + 2} {y} {z + 3} minecraft:furnace[facing=north]', 0.4)
    if not _hand(s, 3, 'simplegadgets:rotator', 'rotator'):
        return
    if _aim(s, me, (x + 0.5, y + 0.99, z + 3.5), 'minecraft:piston', 'rotator piston'):
        _click(s)
        _say(s, f'unless block {x} {y} {z + 3} minecraft:piston[facing=north]', 'piston-turned')
        _check_said(s, 'transposer: a REAL click on the top face turns the piston', ['piston-turned'])
        s.shot('1-piston-turned')
        for _ in range(3):
            _click(s)
        _say(s, f'if block {x} {y} {z + 3} minecraft:piston[facing=north]', 'piston-round')
        _check_said(s, 'transposer: four clicks around the vertical axis bring the piston back to north', ['piston-round'])
        s.shot('2-piston-4x')
    if _aim(s, me, (x + 2.5, y + 0.99, z + 3.5), 'minecraft:furnace', 'rotator furnace'):
        _click(s, after=1.0)
        plain = s.st().get('screen', 'none')
        s.check('furnace: a plain click opens its menu (known defect, documented in RotatorTests)', 'Furnace' in plain, plain)
        s.close()
        time.sleep(0.5)
        if _aim(s, me, (x + 2.5, y + 0.99, z + 3.5), 'minecraft:furnace', 'rotator furnace again'):
            _sneak_click(s, 'furnace')
            _say(s, f'unless block {x + 2} {y} {z + 3} minecraft:furnace[facing=north]', 'furnace-turned')
            _check_said(s, 'transposer: Shift (held) + click turns the furnace', ['furnace-turned'])
            s.shot('3-furnace-sneak')


def gadgets_magnet(s):
    _prep(s)
    x, y, z = _arena(s, 40)
    me = (x + 0.5, y, z + 0.5)
    s.chat(f'tp @s {me[0]} {y} {me[2]} 0 20', 1.2)
    if not _hand(s, 2, 'simplegadgets:magnet', 'magnet'):
        return
    # ---- sneaking pauses it
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.9)
        s.check('magnet: the client sneaks (real Shift held >= 0.8 s)', bool(s.st().get('sneaking')), s.last.get('sneaking'))
        for dx, dz in ((0.5, 3.0), (-0.6, 2.8), (1.4, 3.0)):
            # PickupDelay 32767 = Vanilla's "never" mark, which the attractor leaves alone by design; 600 ticks keeps them lying
            s.chat(f'summon minecraft:item {x + 0.5 + dx} {y + 0.2} {z + 0.5 + dz} '
                   f'{{Item:{{id:"minecraft:cobblestone",count:4}},PickupDelay:600s,Tags:["n127item"]}}', 0.3)
        time.sleep(1.5)
        n = _count(s, 'lying', '@e[type=minecraft:item,tag=n127item]')
        s.check('magnet: while sneaking the three stacks stay lying', n == 3, n)
        s.shot('1-sneaking-items-stay')
    finally:
        s.io.key_up('Shift_L')
    time.sleep(0.4)
    s.shot('2-pulling')
    time.sleep(2.0)
    n = _count(s, 'pulled', '@e[type=minecraft:item,tag=n127item]')
    got = _inv_count(s.st(), 'minecraft:cobblestone')
    s.check('magnet: Shift released - all three stacks fly into the inventory (12 cobblestone)', n == 0 and got == 12, f'{n} lying, {got} in the inventory')

    # ---- death with Simple Tweaks T27: hotbar kept, main inventory drops; the own attractor pulls the own death drops back
    s.chat('clear @s', 0.3)
    s.chat('gamemode survival @s', 0.4)
    s.chat('item replace entity @s hotbar.2 with simplegadgets:magnet', 0.3)
    s.chat('item replace entity @s inventory.5 with minecraft:diamond 3', 0.3)
    s.chat('item replace entity @s inventory.6 with minecraft:cobblestone 16', 0.3)
    s.io.key('1', 0.4)  # empty hand: the attractor must not work before the respawn part wants it
    dx, dy, dz = x + 0.5, y, z + 8.5
    s.chat(f'tp @s {dx} {dy} {dz} 0 0', 1.2)
    s.chat('kill @s', 1.5)
    st = s.wait_screen('DeathScreen', 6)
    if 'DeathScreen' not in st.get('screen', ''):
        s.check('death screen after /kill', False, st.get('screen'))
        return
    time.sleep(1.5)
    s.click_widget(label='Respawn')
    time.sleep(2.0)
    if 'DeathScreen' in s.screen():
        s.check('REAL click on Respawn', False, s.last.get('screen'))
        return
    _say(s, 'if items entity @s hotbar.2 simplegadgets:magnet', 'magnet-kept')
    _say(s, 'unless items entity @s inventory.* minecraft:diamond', 'diamond-dropped')
    _check_said(s, 'T27: the attractor in the hotbar is kept, the main-inventory diamonds dropped', ['magnet-kept', 'diamond-dropped'])
    s.chat(f'tp @s {dx} {dy} {dz - 3.5} 0 20', 2.0)
    s.io.key('1', 0.4)
    time.sleep(1.5)
    near = f'@e[type=minecraft:item,x={dx},y={dy},z={dz},distance=..6]'
    before = _count(s, 'death-drops', near)
    s.check('the two death-drop stacks lie at the death spot (empty hand 3.5 blocks away leaves them)', before == 2, before)
    s.shot('3-death-drops')
    s.io.key('3', 0.5)
    time.sleep(2.0)
    s.chat(f'tp @s {dx} {dy} {dz - 2.5} 0 20', 2.0)
    after = _count(s, 'death-drops-after', near)
    st = s.st()
    dia, cob = _inv_count(st, 'minecraft:diamond'), _inv_count(st, 'minecraft:cobblestone')
    s.check('the own attractor pulls the own death drops back (3 diamonds + 16 cobblestone in the inventory)',
            after == 0 and dia == 3 and cob == 16, f'{after} lying, {dia} diamonds, {cob} cobblestone')
    s.shot('4-death-drops-back')
    s.chat('clear @s', 0.3)
    s.chat('gamemode creative @s', 0.3)


# ---------------------------------------------------------------- Simple Music
_music = None


def _music_mod():
    """modules/simplemusic/tools/uiprobe_simplemusic.py with the 26.3 fixes: sheet read from hotbar slot 1 (Inventory[{Slot:0b}]; the
    player data has no SelectedItem since 1.21.5) and float-safe positions."""
    global _music
    if _music is None:
        _music = _load('uiprobe_simplemusic_n127', 'modules/simplemusic/tools/uiprobe_simplemusic.py')
        _music.SHEET = 'Inventory[{Slot:0b}].components."simplemusic:sheet"'
        _music._xyz = lambda st: [int(float(v)) for v in st['pos'].replace(',', ' ').split()]
    return _music


def music_editor(s):
    _prep(s)
    _music_mod().editor(s)


def music_press(s):
    _prep(s)
    _music_mod().press(s)


POCKET = 'Inventory[{Slot:0b}].components."simplemusic:pocket"'


def music_pocket(s):
    m = _music_mod()
    _prep(s, 'survival')
    x, y, z = _arena(s, 40)
    s.chat('item replace entity @s hotbar.0 with simplemusic:pocket_jukebox', 0.4)
    for i, disc in enumerate(('cat', '13', 'blocks')):
        s.chat(f'item replace entity @s inventory.{i} with minecraft:music_disc_{disc}', 0.3)
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 8)
    if 'InventoryScreen' not in st.get('screen', ''):
        s.check('E opens the survival inventory', False, st.get('screen'))
        return
    jukebox = m.own_index(st, 'simplemusic:pocket_jukebox x1')
    s.check('the pocket jukebox is in the hotbar', jukebox is not None, [sl['stack'] for sl in st['slots'] if sl['own'] and sl['stack']])
    if jukebox is None:
        return
    for disc in ('minecraft:music_disc_cat x1', 'minecraft:music_disc_13 x1', 'minecraft:music_disc_blocks x1'):
        m.click_own(s, disc)
        s.click_slot(index=jukebox)
        st = s.st()
        s.check(f'{disc.split(":")[1].split(" ")[0]}: cursor empty after dropping it on the jukebox', not st.get('carried'), st.get('carried'))
    s.close()
    time.sleep(0.5)
    _say(s, 'if data entity @s Inventory[{Slot:0b}].components."minecraft:bundle_contents"[2]', 'three-discs')
    _say(s, 'unless data entity @s Inventory[{Slot:0b}].components."minecraft:bundle_contents"[3]', 'no-fourth')
    _check_said(s, 'three discs are in the jukebox (bundle_contents has exactly three entries)', ['three-discs', 'no-fourth'])

    # ---- in the hand: right click plays, again pauses
    s.io.key('1', 0.5)
    _say(s, 'if items entity @s weapon.mainhand simplemusic:pocket_jukebox', 'pocket-mainhand')
    _check_said(s, 'pocket jukebox in the main hand (hotbar slot 1 selected)', ['pocket-mainhand'])
    if not _aim(s, (x + 0.5, y, z + 0.5), (x + 0.5, y + 1.62 + 8, z + 30.5), None, 'pocket air'):
        return
    _click(s, after=1.0)
    _say(s, f'if data entity @s {POCKET}.id', 'playing-id')
    _say(s, f'unless data entity @s {POCKET}.ticks', 'playing-pos0')
    _check_said(s, 'REAL right click in the air plays (session id on the stack, position 0)', ['playing-id', 'playing-pos0'])
    s.shot('1-playing')
    time.sleep(1.5)
    _click(s, after=1.0)
    _say(s, f'if data entity @s {POCKET}.ticks', 'paused-pos')
    _check_said(s, 'a second REAL right click pauses it (the position reached is stored)', ['paused-pos'])

    # ---- placing with Shift held + right click on the floor
    px, pz = x, z + 3
    if not _aim(s, (x + 0.5, y, z + 0.5), (px + 0.5, y - 0.05, pz + 0.5), FLOOR, 'pocket floor'):
        return
    _sneak_click(s, 'pocket place')
    _say(s, f'if block {px} {y} {pz} simplemusic:pocket_jukebox', 'placed')
    _say(s, 'unless items entity @s hotbar.0 simplemusic:pocket_jukebox', 'hand-empty')
    if not _check_said(s, 'Shift (held) + REAL right click put the jukebox down in front (hand empty)', ['placed', 'hand-empty']):
        return
    s.io.key('2', 0.4)  # an empty slot: the next right click is an empty-hand click on the block
    if _aim(s, (x + 0.5, y, z + 0.5), (px + 0.5, y + 0.3, pz + 0.5), 'simplemusic:pocket_jukebox', 'pocket block'):
        s.shot('2-placed')
        _click(s, after=1.0)
        _say(s, f'if block {px} {y} {pz} simplemusic:pocket_jukebox[playing=true]', 'block-playing')
        _check_said(s, 'empty-hand REAL right click on the placed jukebox plays it (playing=true)', ['block-playing'])
        s.shot('3-placed-playing')
        shown = []
        for phase in ('before', 'after'):
            for i in range(-1, 3):  # -1 = never scrolled (the current disc is shown)
                _say(s, f'if data block {px} {y} {pz} {{Shown:{i}}}', f'shown-{phase}-{i}')
            got = _said(s, [f'shown-{phase}-{i}' for i in range(-1, 3)])
            shown.append(next((i for i in range(-1, 3) if got[f'shown-{phase}-{i}']), None))
            if phase == 'before':
                s.io.key_down('Shift_L')
                try:
                    time.sleep(0.9)
                    s.check('pocket wheel: the client sneaks (real Shift held)', bool(s.st().get('sneaking')), s.last.get('sneaking'))
                    s.io.button(5, True)
                    s.io.button(5, False)
                    time.sleep(0.8)
                finally:
                    s.io.key_up('Shift_L')
                time.sleep(0.5)
        s.check('Shift + REAL wheel on the placed jukebox moves the shown disc (block entity Shown)',
                None not in shown and shown[0] != shown[1], f'Shown {shown[0]} -> {shown[1]}')
        s.shot('4-placed-scrolled')
    s.chat(f'setblock {px} {y} {pz} minecraft:air', 0.3)
    s.chat('kill @e[type=minecraft:item,distance=..10]', 0.3)
    s.chat('clear @s', 0.3)
    s.chat('gamemode creative @s', 0.3)


SCENARIOS = {
    'n127-lights-panes': lights_panes,
    'n127-gadgets-gauge': gadgets_gauge,
    'n127-gadgets-rod': gadgets_rod,
    'n127-gadgets-detector': gadgets_detector,
    'n127-gadgets-echo': gadgets_echo,
    'n127-gadgets-rotator': gadgets_rotator,
    'n127-gadgets-magnet': gadgets_magnet,
    'n127-music-editor': music_editor,
    'n127-music-press': music_press,
    'n127-music-pocket': music_pocket,
}
