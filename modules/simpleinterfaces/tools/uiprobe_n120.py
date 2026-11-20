"""N120 release polish, mods 5 + 7 (offen-vor-release.md section 3, "Szenen"), word for word. Simple Slabs (mod 6) needs no
new scene: its scenes are the N86C/N86S probes (uiprobe_n86.py / uiprobe_n86s.py, shots in previews/n86c + n86s).

Simple Storage "Rucksack alle 4 Stufen (Margin messen), Crafting Storage, getragene Rucksäcke/Köcher von vorn/hinten (W4C-46)."
  n120-storage-backpacks  the four backpacks worn in the chest slot (survival), each opened with the backpack key B: screen class
                          BackpackScreen, backpack slot count 9 / 18 / 33 / 50, 36 player slots; shot + probe status json next
                          to it (shots/<scenario>-<tier>.json, for /root/sb-tools-b/ig57u/mock/measure_shots.py). The margins
                          between the box frame (SimpleLib UiBoxes FRAME 5 / FRAME_BOTTOM 7) and the outermost slots are
                          computed from the real slot positions and the tier's image size (BackpackLayout) and written to
                          shots/n120-backpack-margins.json - measured only: the margin rule for the backpack is an owner
                          decision (N64-F), so these checks report numbers and do not judge them.
  n120-storage-crafting   Storage Crafting Table opened with a real right click: StorageCraftingScreen, 46 slots (result, 3x3
                          grid, 36 inventory) = variant A (variant B with 27 chest slots under the seam is the open owner
                          question IG57U); shot + status json.
  n120-storage-worn       the four backpacks and the four quivers worn in the chest slot (checked in the survival inventory,
                          armor slot index 6), each shot in third person from behind (F5) and from the front (F5 twice).
Simple Octets "uiprobe_n100b (Blasseiche/Eiche/Karmesin/entrindet), Axt strippt einzelnes Achtel."
  (uiprobe_n100b: already run 1/1, not repeated here)
  n120-octets-strip       a 7-of-8 cell of oak log octets (o111 empty); survival, iron axe, real right click on the top face
                          over the octet x1 y1 z0: the cell turns mixed_octet, its block entity holds exactly Octets
                          [0,0,0,1,0,0,0,-1] (palette 1 = the stripped oak log octet), the axe has damage 1; a second click
                          on the same octet changes nothing (damage stays 1); a click over the octet x0 y1 z1 strips that one
                          too (Octets [0,0,0,1,0,0,1,-1], damage 2). Shots before / after each click and a close-up.

Patterns (uiprobe_n100/n119): position via int(float()) of status 'pos'; 26.x gamerules advance_time / spawn_mobs; block
states read with '/execute if block|data|items ... run say N120 <tag>' from client.log ([CHAT] lines); before every real
right click the crosshair block is checked (tp ... facing again, up to 3 tries) - after 'tp ... facing' the camera can turn.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe120 --scale 2 \
        --only n120-storage-backpacks,n120-storage-crafting,n120-storage-worn,n120-octets-strip
"""
import json
import re
import time

# BackpackTier: rows, extra columns; BackpackLayout: image 176 + 18 * columns wide, 166 + 18 * rows high, vanilla part
# 18 px to the right when the tier has a left column (Enderite)
TIERS = [
    ('basic', 'simplestorage:backpack', 9, 176, 184, 0),
    ('reinforced', 'simplestorage:reinforced_backpack', 18, 176, 202, 0),
    ('netherite', 'simplestorage:netherite_backpack', 33, 194, 220, 0),
    ('enderite', 'simplestorage:enderite_backpack', 50, 212, 238, 18),
]
FRAME, FRAME_BOTTOM, SLOT = 5, 7, 17
WORN = [
    ('backpack', 'simplestorage:backpack'),
    ('reinforced-backpack', 'simplestorage:reinforced_backpack'),
    ('netherite-backpack', 'simplestorage:netherite_backpack'),
    ('enderite-backpack', 'simplestorage:enderite_backpack'),
    ('quiver', 'simplestorage:quiver'),
    ('reinforced-quiver', 'simplestorage:reinforced_quiver'),
    ('netherite-quiver', 'simplestorage:netherite_quiver'),
    ('enderite-quiver', 'simplestorage:enderite_quiver'),
]


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _log(s):
    p = s.out / 'client.log'
    return p.read_text(errors='replace') if p.exists() else ''


def _said(s, tags, settle=1.5):
    """Which of the tags came back as '[CHAT] ... N120 <tag>' (exact word end)."""
    time.sleep(settle)
    log = _log(s)
    return {t: bool(re.search(r'N120 ' + re.escape(t) + r'(?![\w-])', log)) for t in tags}


def _prep(s, mode='creative'):
    s.close()
    s.chat(f'gamemode {mode} @s', 0.4)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('clear @s', 0.4)


def _save(s, name, st=None):
    time.sleep(0.8)
    s.shot(name)
    dest = s.out / 'shots'
    dest.mkdir(parents=True, exist_ok=True)
    (dest / f"{s.scenario.replace('/', '-')}-{name}.json").write_text(json.dumps(st or s.st(), indent=1))


def _bp_slots(st, n):
    """Backpack slots: menu index 46 .. 46+n-1 (BackpackMenu.BACKPACK_SLOT_START = 46)."""
    return [x for x in st.get('slots', []) if 46 <= x['index'] < 46 + n and not x['own']]


# ---------------------------------------------------------------- Simple Storage
def _margins(st, width, height, vx):
    """Margins between the box frame and the outermost slots, from the real slot positions (gui px). The head armor slot
    (menu index 5, layout x vx + 8, y 8) anchors the window."""
    scale = st.get('scale') or 2
    pts = {x['index']: (round(x['x'] / scale) - 8, round(x['y'] / scale) - 8) for x in st.get('slots', [])}
    if 5 not in pts:
        return {'error': 'no head armor slot (index 5)', 'slots': len(pts)}
    left, top = pts[5][0] - (vx + 8), pts[5][1] - 8
    rel = [(x - left, y - top) for x, y in pts.values()]
    x0, x1 = min(x for x, _ in rel), max(x for x, _ in rel)
    y0, y1 = min(y for _, y in rel), max(y for _, y in rel)
    return {'links': x0 - FRAME, 'rechts': width - FRAME - (x1 + SLOT), 'oben': y0 - FRAME,
            'unten': height - FRAME_BOTTOM - (y1 + SLOT), 'slots': len(pts), 'span': [x0, y0, x1, y1]}


def storage_backpacks(s):
    _prep(s, 'survival')
    measured = {}
    for tier, item, n, width, height, vx in TIERS:
        s.close()
        s.chat('clear @s', 0.3)
        s.chat(f'item replace entity @s armor.chest with {item}', 0.8)
        st = s.key('b', 1.2)
        st = s.wait_screen('BackpackScreen', 6)
        ok = 'BackpackScreen' in st.get('screen', '')
        s.check(f'{tier}: key B opens the worn backpack (BackpackScreen)', ok, st.get('screen'))
        if not ok:
            continue
        got = len(_bp_slots(st, n))
        s.check(f'{tier}: {n} backpack slots', got == n, got)
        own = sum(1 for x in st['slots'] if x['own'])
        s.check(f'{tier}: the player inventory is in the window (>= 36 own slots)', own >= 36, own)
        m = _margins(st, width, height, vx)
        measured[tier] = m
        s.check(f'{tier}: margins measured (N64-F, owner decision - no target)', 'error' not in m,
                m.get('error') or f"l{m['links']} r{m['rechts']} o{m['oben']} u{m['unten']} (image {width}x{height})")
        _save(s, tier, st)
    s.close()
    dest = s.out / 'shots'
    dest.mkdir(parents=True, exist_ok=True)
    (dest / 'n120-backpack-margins.json').write_text(json.dumps(measured, indent=1))
    s.chat('clear @s', 0.3)
    s.chat('gamemode creative @s', 0.5)


def storage_crafting(s):
    _prep(s)
    x, y, z = _pos(s)
    s.chat(f'fill {x + 1} {y} {z - 1} {x + 3} {y + 2} {z + 1} minecraft:air', 0.4)
    st = None
    for attempt in range(3):
        s.close()
        s.stage('simplestorage:storage_crafting_table', 2, 0, 0)
        st = s.st()
        if st.get('hitBlock') != 'simplestorage:storage_crafting_table':
            continue
        s.io.right_click()
        st = s.wait_screen('StorageCraftingScreen', 8)
        if 'StorageCraftingScreen' in st.get('screen', ''):
            break
    ok = 'StorageCraftingScreen' in (st or {}).get('screen', '')
    s.check('real right click opens the Storage Crafting Table (StorageCraftingScreen)', ok, (st or {}).get('screen'))
    if not ok:
        return
    n = len(st['slots'])
    own = sum(1 for x in st['slots'] if x['own'])
    s.check('46 slots: result + 3x3 grid + 36 inventory (variant A; variant B = owner question IG57U)', n == 46 and own == 36,
            f'{n} slots, {own} own')
    _save(s, 'crafting-storage', st)
    s.close()


def storage_worn(s):
    _prep(s, 'survival')
    x, y, z = _pos(s)
    s.chat(f'fill {x - 2} {y} {z - 4} {x + 2} {y + 3} {z + 2} minecraft:air', 0.4)
    look = f'tp @s {x + 0.5} {y} {z + 0.5} 180 0'
    s.chat(look, 0.8)
    st = s.key('F5', 1.5)  # first person -> third person, back view
    s.check('F5 (back view) leaves no screen open', st.get('screen') == 'none', st.get('screen'))
    worn_ok = []
    for name, item in WORN:
        s.chat(f'item replace entity @s armor.chest with {item}', 0.6)
        st = s.key('e', 1.0)
        st = s.wait_screen('InventoryScreen', 5)
        sl = next((v for v in st.get('slots', []) if v['index'] == 6), None)
        if sl and (sl.get('stack') or '').startswith(item.split('[')[0]):
            worn_ok.append(name)
        s.close()
        s.chat(look, 0.8)
        time.sleep(0.6)
        s.shot(f'hinten-{name}')
    s.check('every backpack and quiver sits in the chest slot (inventory slot 6)', len(worn_ok) == len(WORN),
            f'{len(worn_ok)}/{len(WORN)} {sorted(set(n for n, _ in WORN) - set(worn_ok))}')
    st = s.key('F5', 1.5)  # front view
    for name, item in WORN:
        s.chat(f'item replace entity @s armor.chest with {item}', 0.6)
        s.chat(look, 0.6)
        time.sleep(0.6)
        s.shot(f'vorn-{name}')
    st = s.key('F5', 1.2)  # back to first person
    s.check('F5 cycle ends in first person without a screen', st.get('screen') == 'none', st.get('screen'))
    s.chat('clear @s', 0.3)
    s.chat('gamemode creative @s', 0.5)


# ---------------------------------------------------------------- Simple Octets
NOTCH = 'o000=true,o100=true,o001=true,o101=true,o010=true,o110=true,o011=true,o111=false'


def _aim_and_click(s, eye, target, want_block, tag):
    """tp to eye position facing the target on the cell's top face; real right click only with the crosshair on want_block."""
    for attempt in range(3):
        s.chat(f'tp @s {eye[0]} {eye[1]} {eye[2]} facing {target[0]} {target[1]} {target[2]}', 0.8)
        time.sleep(0.6)
        st = s.st()
        if st.get('hitBlock') == want_block:
            s.io.right_click()
            time.sleep(0.8)
            return True
    s.check(f'{tag}: crosshair on {want_block}', False, s.st().get('hitBlock'))
    return False


def octets_strip(s):
    _prep(s, 'survival')
    x, y, z = _pos(s)
    cx, cy, cz = x + 3, y, z
    s.chat(f'fill {x - 1} {y} {z - 3} {x + 6} {y + 3} {z + 3} minecraft:air', 0.4)
    s.chat(f'setblock {cx} {cy} {cz} simpleoctets:oak_log_octet[{NOTCH}]', 0.5)
    s.chat('item replace entity @s weapon.mainhand with minecraft:iron_axe', 0.5)
    eye = (cx - 0.5, cy, cz + 0.25)                # feet; the eye is 1.62 higher, so the ray comes down onto the top face
    first = (cx + 0.75, cy + 1.0, cz + 0.25)       # over the octet x1 y1 z0 (index 3, property o110)
    second = (cx + 0.25, cy + 1.0, cz + 0.75)      # over the octet x0 y1 z1 (index 6, property o011)
    s.chat(f'tp @s {eye[0]} {eye[1]} {eye[2]} facing {first[0]} {first[1]} {first[2]}', 1.0)
    time.sleep(0.6)
    st = s.st()
    s.check('crosshair on the oak log octet cell before the click', st.get('hitBlock') == 'simpleoctets:oak_log_octet', st.get('hitBlock'))
    s.shot('1-vorher')

    def state(tag, octets, palette_stripped=True, damage=None):
        s.chat(f'execute if block {cx} {cy} {cz} simpleoctets:mixed_octet run say N120 {tag}-mixed', 0.35)
        s.chat(f'execute if data block {cx} {cy} {cz} {{Octets:[I;{",".join(str(v) for v in octets)}]}} run say N120 {tag}-octets', 0.35)
        if palette_stripped:
            for name in ('simpleoctets:stripped_oak_log_octet', 'minecraft:stripped_oak_log'):
                s.chat(f'execute if data block {cx} {cy} {cz} Palette[{{Name:"{name}"}}] run say N120 {tag}-stripped', 0.35)
        if damage is not None:
            s.chat(f'execute if items entity @s weapon.mainhand minecraft:iron_axe[minecraft:damage={damage}] run say N120 {tag}-damage', 0.35)

    # 1) first click: only the octet x1 y1 z0 is stripped
    if _aim_and_click(s, eye, first, 'simpleoctets:oak_log_octet', 'click 1'):
        st = s.st()
        state('a', [0, 0, 0, 1, 0, 0, 0, -1], damage=1)
        got = _said(s, ['a-mixed', 'a-octets', 'a-stripped', 'a-damage'])
        s.check('click 1: the cell turns mixed_octet', got['a-mixed'], st.get('hitBlock'))
        s.check('click 1: only the clicked octet is stripped (Octets [0,0,0,1,0,0,0,-1], palette stripped oak log)',
                got['a-octets'] and got['a-stripped'], got)
        s.check('click 1: the axe lost one durability (damage 1)', got['a-damage'], st.get('mainhandComponents'))
        s.shot('2-nach-klick-1')
        # 2) the same octet again: nothing changes
        if _aim_and_click(s, eye, first, 'simpleoctets:mixed_octet', 'click 2'):
            state('b', [0, 0, 0, 1, 0, 0, 0, -1], palette_stripped=False, damage=1)
            got = _said(s, ['b-octets', 'b-damage'])
            s.check('click 2 on the stripped octet: nothing changes (same Octets, damage still 1)', got['b-octets'] and got['b-damage'], got)
        # 3) another octet: stripped too, damage 2
        if _aim_and_click(s, eye, second, 'simpleoctets:mixed_octet', 'click 3'):
            state('c', [0, 0, 0, 1, 0, 0, 1, -1], palette_stripped=False, damage=2)
            got = _said(s, ['c-octets', 'c-damage'])
            s.check('click 3 on the octet x0 y1 z1: that one is stripped too (Octets [0,0,0,1,0,0,1,-1], damage 2)',
                    got['c-octets'] and got['c-damage'], got)
            s.shot('3-nach-klick-3')
    # close-up from above the notch (commands only; spectator: no fall, no hand in the picture)
    s.chat('gamemode spectator @s', 0.5)
    s.chat(f'tp @s {cx - 0.6} {cy + 0.9} {cz - 0.9} facing {cx + 0.5} {cy + 0.75} {cz + 0.5}', 1.0)
    time.sleep(0.8)
    s.shot('4-nah')
    s.chat('clear @s', 0.3)
    s.chat('gamemode creative @s', 0.5)


SCENARIOS = {
    'n120-storage-backpacks': storage_backpacks,
    'n120-storage-crafting': storage_crafting,
    'n120-storage-worn': storage_worn,
    'n120-octets-strip': octets_strip,
}
