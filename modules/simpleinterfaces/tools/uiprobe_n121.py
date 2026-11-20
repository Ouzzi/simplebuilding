"""N121 release polish, mods 8-10 (offen-vor-release.md section 3, "Szenen" of Simple Machines, Simple Building Tools, Simple Chisels),
word for word:

Simple Machines  "Schmied-, Crafter-, Trichter-Fenster (machines_probe 76/76 lief), Doppelkolben ausfahren."
  n121-machines-windows  Smither, Auto Crafter and the Reinforced / Netherite / Enderite Hopper set by command, each opened with a REAL right
                         click (crosshair on the block checked first, up to 3 tries): screen class (AutoSmitherScreen,
                         AutonomousCrafterScreen, TierHopperScreen), title ("Smither", "Auto Crafter", hopper name) and the number of the
                         machine's own slots (Smither 4 = template/base/addition/result, Auto Crafter 10 = 3x3 grid + preview, hopper 5);
                         hopper filter key present (widget). Shot of each window. The deep interaction (refused items, recipe error, real
                         smithing / crafting by redstone, filter modes) is machines_probe.py, last run 76/76:
                         python3.12 modules/simplemachines/tools/machines_probe.py --out /var/tmp/sb/machinesprobe --scales 2,4
  n121-machines-piston   two Double Pistons facing up: one fed by a dust line whose last piece carries 7 (stage 1: ext=1, head one block up),
                         one next to a redstone block (15 = stage 2: ext=2, arm + head two blocks up); every state read back with
                         '/execute if block'. Built piston first, wire second, source last (the source's neighbour update starts the move).
                         Shots stage 0 / out; then both sources removed: both back to ext=0, heads gone. Texture judgement = owner (W4C-26).
Simple Building Tools  "Baustäbe, Bauplan-Editor, Oktant (mouse_probe 53/53 lief), Gefühl + Audit-Fragen F2/F6 (W4C-11)."
  n121-bt-wand           Copper Building Wand in hand, stone in the hotbar, crosshair on the top face of a floor block (checked), REAL right
                         click: the 3x3 stone area appears on the floor (counted with a fill-replace in the chat log = 9). Undo: look into
                         the sky, sneak + REAL right click: the 9 stones are gone. K with the wand opens the wand settings
                         (BuildingWandScreen); shots.
  n121-bt-screens        Octant in hand + K -> OctantScreen ("Octant Manager"), Blueprint in hand looking into the sky + REAL right click ->
                         BlueprintScreen (editor); labels and widgets listed, shots. Deep checks (typing, buttons, placed blueprint, JEI):
                         mouse_probe.py, last run 53/53:
                         python3.12 modules/simplebuildingtools/tools/mouse_probe.py --out /var/tmp/sb/btmouse
                         "Gefühl + Audit-Fragen F2/F6" (W4C-11) is the owner's judgement, not a probe check.
Simple Chisels   "Meißel/Spachtel in der Hand + Ketten."
  n121-chisels-hand      all nine chisels (stone, copper, rose copper, iron, gold, rose gold, diamond, netherite, enderite) in the hotbar,
                         each selected with its number key: first-person shot, the netherite one also third person (F5). The six old
                         spatulas ARE these chisels on 26.3 (SimpleChisels.SPATULA_TIERS -> <tier>_chisel, gametest
                         migration_from_simplebuilding): no own item to show.
  n121-chisels-chains    whole chains with REAL right clicks, crosshair checked before every click: Stone Chisel through red sandstone
                         (smooth -> cut -> red -> chiseled), Diamond Chisel through quartz (smooth -> pillar -> bricks -> chiseled -> block);
                         each step read from the crosshair block; then sneak + click walks one step back. Shots per step.

Patterns (uiprobe_n119): position via int(float()) of status 'pos'; gamerules advance_time / spawn_mobs; blocks set and read with commands,
real clicks only after the crosshair block was read back (the tp-facing turn has settled) and inside screens. No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe121 --scale 2 \
        --only n121-machines-windows,n121-machines-piston,n121-bt-wand,n121-bt-screens,n121-chisels-hand,n121-chisels-chains
"""
import re
import time


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _plain(label):
    return re.sub('§.', '', label or '')


def _log(s):
    p = s.out / 'client.log'
    return p.read_text(errors='replace') if p.exists() else ''


def _say_if(s, x, y, z, state, tag):
    s.chat(f'execute if block {x} {y} {z} {state} run say N121 {tag}', 0.35)


def _said(s, tags, settle=1.5):
    time.sleep(settle)
    log = _log(s)
    return {t: bool(re.search(r'N121 ' + re.escape(t) + r'(?![\w-])', log)) for t in tags}


def _check_said(s, name, tags):
    got = _said(s, tags)
    miss = [t for t, ok in got.items() if not ok]
    s.check(name, not miss, f'{len(tags) - len(miss)}/{len(tags)}' + (f' missing {miss[:12]}' if miss else ''))


def _count(s, tag, box, block):
    s.chat(f'say N121 COUNT {tag}', 0.35)
    s.chat(f'fill {box} minecraft:air replace {block}', 0.5)


def _counts(s, tags):
    """Count after 'N121 COUNT <tag>': the next fill feedback line in client.log."""
    time.sleep(1.5)
    lines = _log(s).splitlines()
    out = {}
    for t in tags:
        idx = max((i for i, ln in enumerate(lines) if re.search(r'N121 COUNT ' + re.escape(t) + r'$', ln.strip())), default=-1)
        n = None
        for ln in lines[idx + 1: idx + 8] if idx >= 0 else []:
            m = re.search(r'(?:Successfully filled|filled) (\d+) block', ln)
            if m:
                n = int(m.group(1))
                break
            if 'No blocks were filled' in ln:
                n = 0
                break
        out[t] = n
    return out


def _prep(s, mode='creative'):
    s.close()
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    s.chat('clear @s', 0.3)


def _wait_screen(s, fragment, seconds=6.0):
    st = s.wait_screen(fragment, seconds)
    return st if fragment in st.get('screen', '') else None


def _park(s, st):
    s.io.move(*s.abs(st, 4, 4))
    time.sleep(0.3)


def _aim(s, bx, by, bz, want, tag, tries=4):
    """Turns onto the block and waits until the crosshair reads it back (the turn has settled). True when it does."""
    hit = None
    for _ in range(tries):
        st = s.face(bx, by, bz)
        for _ in range(6):
            hit = st.get('hitBlock')
            if hit == want or (want.endswith(':') and hit and hit.startswith(want)):
                return True
            time.sleep(0.3)
            st = s.st()
    s.check(f'{tag}: crosshair on {want}', False, hit)
    return False


def _sky(s):
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {x + 0.5} {y + 40} {z + 3.5}', 0.8)
    time.sleep(0.6)


def _open_block(s, bx, by, bz, fragment, tag, want):
    for attempt in range(3):
        if not _aim(s, bx, by, bz, want, tag):
            continue
        s.io.right_click()
        got = _wait_screen(s, fragment, 4)
        if got:
            return got
        s.close()
    s.check(f'{tag}: real right click opens {fragment}', False, s.last.get('screen'))
    return None


def _menu_slots(st):
    return [sl for sl in st.get('slots', []) if not sl.get('own')]


# ---------------------------------------------------------------- Simple Machines: windows
MACHINES = [
    ('simplemachines:auto_smither', 'AutoSmitherScreen', 'Smither', 4, 'smither'),
    ('simplemachines:autonomous_crafter', 'AutonomousCrafterScreen', 'Auto Crafter', 10, 'crafter'),
    ('simplemachines:reinforced_hopper', 'TierHopperScreen', 'Reinforced Hopper', 5, 'hopper-reinforced'),
    ('simplemachines:netherite_hopper', 'TierHopperScreen', 'Netherite Hopper', 5, 'hopper-netherite'),
    ('simplemachines:enderite_hopper', 'TierHopperScreen', 'Enderite Hopper', 5, 'hopper-enderite'),
]


def machines_windows(s):
    _prep(s)
    x, y, z = _pos(s)
    s.chat(f'fill {x - 2} {y} {z + 1} {x + 2} {y + 2} {z + 4} minecraft:air', 0.3)
    for block, screen, title, own, tag in MACHINES:
        s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.3)
        s.chat(f'setblock {x} {y} {z + 2} minecraft:air', 0.3)
        s.chat(f'setblock {x} {y} {z + 2} {block}', 0.5)
        st = _open_block(s, x, y, z + 2, screen, tag, block)
        if not st:
            continue
        s.check(f'{tag}: screen {screen}', screen in st.get('screen', ''), st.get('screen'))
        s.check(f'{tag}: title "{title}"', _plain(st.get('title', '')) == title, st.get('title'))
        n = len(_menu_slots(st))
        s.check(f'{tag}: {own} own slots', n == own, n)
        if screen == 'TierHopperScreen':
            key = s.find_widget(cls='UiFilterButton', st=st)
            s.check(f'{tag}: filter key (UiFilterButton) present', key is not None, '; '.join(s.labels(st))[:200])
        _park(s, st)
        s.shot(tag)
        s.close()
    s.chat(f'setblock {x} {y} {z + 2} minecraft:air', 0.3)


# ---------------------------------------------------------------- Simple Machines: double piston
def machines_piston(s):
    _prep(s)
    x, y, z = _pos(s)
    s.chat(f'fill {x - 13} {y} {z + 3} {x + 5} {y + 4} {z + 7} minecraft:air', 0.4)
    s.chat(f'fill {x - 13} {y - 1} {z + 3} {x + 5} {y - 1} {z + 7} minecraft:smooth_stone', 0.4)
    p1 = (x - 2, y, z + 5)   # dust line, last piece 7 -> stage 1
    p2 = (x + 2, y, z + 5)   # redstone block, 15 -> stage 2
    for p in (p1, p2):
        s.chat(f'setblock {p[0]} {p[1]} {p[2]} simplemachines:double_piston[facing=up,ext=0]', 0.4)
    s.chat(f'tp @s {x + 0.5} {y + 1} {z - 2.5} facing {x + 0.5} {y + 1} {z + 5.5}', 0.8)
    time.sleep(0.6)
    _say_if(s, *p1, 'simplemachines:double_piston[ext=0]', 'p1-in')
    _say_if(s, *p2, 'simplemachines:double_piston[ext=0]', 'p2-in')
    _check_said(s, 'Double Pistons placed retracted (ext=0)', ['p1-in', 'p2-in'])
    s.shot('stage-0')
    # wire first (9 dust west of p1: 15 at the block end, 7 at the piece next to the piston), sources last
    s.chat(f'fill {p1[0] - 9} {y} {p1[2]} {p1[0] - 1} {y} {p1[2]} minecraft:redstone_wire', 0.5)
    s.chat(f'setblock {p1[0] - 10} {y} {p1[2]} minecraft:redstone_block', 0.5)
    s.chat(f'setblock {p2[0] + 1} {y} {p2[2]} minecraft:redstone_block', 0.5)
    time.sleep(2.5)
    _say_if(s, p1[0] - 1, y, p1[2], 'minecraft:redstone_wire[power=7]', 'wire-7')
    _say_if(s, *p1, 'simplemachines:double_piston[ext=1]', 'p1-ext1')
    _say_if(s, p1[0], y + 1, p1[2], 'simplemachines:double_piston_head', 'p1-head-1up')
    _say_if(s, *p2, 'simplemachines:double_piston[ext=2]', 'p2-ext2')
    _say_if(s, p2[0], y + 1, p2[2], 'simplemachines:double_piston_arm', 'p2-arm-1up')
    _say_if(s, p2[0], y + 2, p2[2], 'simplemachines:double_piston_head', 'p2-head-2up')
    _check_said(s, 'signal 7 -> stage 1 (head one up), signal 15 -> stage 2 (arm + head two up)',
                ['wire-7', 'p1-ext1', 'p1-head-1up', 'p2-ext2', 'p2-arm-1up', 'p2-head-2up'])
    s.shot('stage-1-and-2')
    s.chat(f'tp @s {x + 4.5} {y + 1} {z + 2.5} facing {x + 0.5} {y + 1.5} {z + 5.5}', 0.8)
    time.sleep(0.6)
    s.shot('stage-1-and-2-side')
    # sources away: both retract
    s.chat(f'setblock {p1[0] - 10} {y} {p1[2]} minecraft:air', 0.5)
    s.chat(f'setblock {p2[0] + 1} {y} {p2[2]} minecraft:air', 0.5)
    time.sleep(2.5)
    _say_if(s, *p1, 'simplemachines:double_piston[ext=0]', 'p1-back')
    _say_if(s, *p2, 'simplemachines:double_piston[ext=0]', 'p2-back')
    _say_if(s, p2[0], y + 2, p2[2], 'minecraft:air', 'p2-head-gone')
    _check_said(s, 'sources removed: both Double Pistons back to ext=0, head gone', ['p1-back', 'p2-back', 'p2-head-gone'])
    s.shot('retracted')


# ---------------------------------------------------------------- Simple Building Tools: wand
def bt_wand(s):
    _prep(s)
    x, y, z = _pos(s)
    s.chat(f'fill {x - 3} {y} {z - 3} {x + 3} {y + 3} {z + 4} minecraft:air', 0.4)
    s.chat(f'fill {x - 3} {y - 1} {z - 3} {x + 3} {y - 1} {z + 4} minecraft:smooth_stone', 0.4)
    s.chat('item replace entity @s hotbar.0 with simplebuildingtools:copper_building_wand', 0.3)
    s.chat('item replace entity @s hotbar.1 with minecraft:stone 64', 0.3)
    s.key('1', 0.5)
    st = s.st()
    s.check('copper wand in hotbar 1, stone in hotbar 2',
            (st.get('inventory') or [''])[0].startswith('simplebuildingtools:copper_building_wand')
            and (st.get('inventory') or ['', ''])[1].startswith('minecraft:stone'), st.get('inventory', [])[:2])
    s.shot('wand-in-hand')
    tx, ty, tz = x, y - 1, z + 2
    if _aim(s, tx, ty, tz, 'minecraft:smooth_stone', 'wand'):
        s.io.right_click()
        time.sleep(2.0)
        s.shot('wand-built')
        _count(s, 'wand', f'{x - 1} {y} {z + 1} {x + 1} {y} {z + 3}', 'minecraft:stone')
        n = _counts(s, ['wand'])['wand']
        s.check('real right click with the Copper Building Wand builds the 3x3 stone area', n == 9, n)
        # build again, then undo: sneak + right click into the air
        if _aim(s, tx, ty, tz, 'minecraft:smooth_stone', 'wand-again'):
            s.io.right_click()
            time.sleep(2.0)
            _sky(s)
            s.io.key_down('Shift_L')
            try:
                time.sleep(0.4)
                s.io.right_click()
            finally:
                s.io.key_up('Shift_L')
            time.sleep(1.5)
            _count(s, 'undo', f'{x - 1} {y} {z + 1} {x + 1} {y} {z + 3}', 'minecraft:stone')
            n = _counts(s, ['undo'])['undo']
            s.check('sneak + real right click into the air undoes the area (0 stones left)', n == 0, n)
    s.key('k', 1.2)
    st = _wait_screen(s, 'BuildingWandScreen', 6)
    s.check('K with the wand opens the wand settings (BuildingWandScreen)', st is not None, s.last.get('screen'))
    if st:
        s.check('wand settings show widgets', len(s.widgets(st)) >= 2, '; '.join(s.labels(st))[:300])
        _park(s, st)
        s.shot('wand-settings')
    s.close()


# ---------------------------------------------------------------- Simple Building Tools: octant + blueprint editor
def bt_screens(s):
    _prep(s)
    s.chat('item replace entity @s hotbar.0 with simplebuildingtools:octant', 0.3)
    s.chat('item replace entity @s hotbar.1 with simplebuildingtools:blueprint', 0.3)
    s.key('1', 0.5)
    _sky(s)
    s.key('k', 1.2)
    st = _wait_screen(s, 'OctantScreen', 6)
    s.check('K with the octant opens the Octant Manager (OctantScreen)', st is not None, s.last.get('screen'))
    if st:
        s.check('Octant Manager title', _plain(st.get('title', '')) == 'Octant Manager', st.get('title'))
        s.check('Octant Manager shows widgets', len(s.widgets(st)) >= 4, '; '.join(s.labels(st))[:300])
        _park(s, st)
        s.shot('octant-manager')
    s.close()
    s.key('2', 0.5)
    _sky(s)
    s.io.right_click()
    st = _wait_screen(s, 'BlueprintScreen', 6)
    s.check('real right click into the air with the Blueprint opens the editor (BlueprintScreen)', st is not None, s.last.get('screen'))
    if st:
        s.check('Blueprint editor shows widgets', len(s.widgets(st)) >= 2, '; '.join(s.labels(st))[:300])
        _park(s, st)
        s.shot('blueprint-editor')
    s.close()


# ---------------------------------------------------------------- Simple Chisels: in hand
CHISELS = ['stone', 'copper', 'rose_bronze', 'iron', 'gold', 'rose_gold', 'diamond', 'netherite', 'enderite']


def chisels_hand(s):
    _prep(s)
    for i, tier in enumerate(CHISELS):
        s.chat(f'item replace entity @s hotbar.{i} with simplechisels:{tier}_chisel', 0.3)
    st = s.st()
    inv = st.get('inventory', [])
    miss = [t for i, t in enumerate(CHISELS) if not (len(inv) > i and inv[i].startswith(f'simplechisels:{t}_chisel'))]
    s.check('all nine chisels in the hotbar', not miss, f'missing {miss}' if miss else inv[:9])
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {x + 0.5} {y + 1.2} {z + 5.5}', 0.8)
    for i, tier in enumerate(CHISELS):
        s.key(str(i + 1), 0.6)
        s.shot(f'hand-{tier}')
    s.key('8', 0.5)
    s.key('F5', 0.8)
    s.shot('third-person-netherite')
    s.key('F5', 0.5)
    s.key('F5', 0.5)


# ---------------------------------------------------------------- Simple Chisels: chains
RED = ['minecraft:smooth_red_sandstone', 'minecraft:cut_red_sandstone', 'minecraft:red_sandstone', 'minecraft:chiseled_red_sandstone']
QUARTZ = ['minecraft:smooth_quartz', 'minecraft:quartz_pillar', 'minecraft:quartz_bricks', 'minecraft:chiseled_quartz_block',
          'minecraft:quartz_block']


def _tap(s):
    s.io.button(3, True)
    time.sleep(0.08)
    s.io.button(3, False)


def _walk(s, chain, slot, tag, cooldown):
    s.key(str(slot), 0.5)
    x, y, z = _pos(s)
    b = (x + 2, y, z)
    s.chat(f'setblock {b[0]} {b[1]} {b[2]} {chain[0]}', 0.5)
    steps = 0
    for i in range(len(chain) - 1):
        if not _aim(s, *b, chain[i], f'{tag} step {i + 1}'):
            break
        _tap(s)
        time.sleep(0.8)
        got = s.st().get('hitBlock')
        s.check(f'{tag}: real click {chain[i]} -> {chain[i + 1]}', got == chain[i + 1], got)
        s.shot(f'{tag}-{i + 1}')
        if got != chain[i + 1]:
            break
        steps += 1
        time.sleep(cooldown)
    if steps == len(chain) - 1 and _aim(s, *b, chain[-1], f'{tag} back'):
        s.io.key_down('Shift_L')
        try:
            time.sleep(0.4)
            _tap(s)
        finally:
            s.io.key_up('Shift_L')
        time.sleep(0.8)
        got = s.st().get('hitBlock')
        s.check(f'{tag}: sneak + real click walks one step back to {chain[-2]}', got == chain[-2], got)
        s.shot(f'{tag}-back')
    s.chat(f'setblock {b[0]} {b[1]} {b[2]} minecraft:air', 0.3)
    time.sleep(cooldown)


def chisels_chains(s):
    _prep(s, 'survival')
    x, y, z = _pos(s)
    s.chat(f'fill {x - 1} {y} {z - 1} {x + 3} {y + 2} {z + 1} minecraft:air', 0.4)
    s.chat('item replace entity @s hotbar.0 with simplechisels:stone_chisel', 0.3)
    s.chat('item replace entity @s hotbar.1 with simplechisels:diamond_chisel', 0.3)
    _walk(s, RED, 1, 'stone-red-sandstone', 1.6)    # stone chisel: 30 ticks cooldown
    _walk(s, QUARTZ, 2, 'diamond-quartz', 0.6)      # diamond chisel: 10 ticks
    s.chat('gamemode creative @s', 0.3)


SCENARIOS = {
    'n121-machines-windows': machines_windows,
    'n121-machines-piston': machines_piston,
    'n121-bt-wand': bt_wand,
    'n121-bt-screens': bt_screens,
    'n121-chisels-hand': chisels_hand,
    'n121-chisels-chains': chisels_chains,
}
