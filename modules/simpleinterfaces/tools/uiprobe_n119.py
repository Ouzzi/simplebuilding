"""N119 release polish, mods 3 + 4 (offen-vor-release.md section 3, "Szenen" of Simple Ores and Simple Blocks), word for word:

Simple Ores  "Erze/Platten/Türen/Spiegel im Kreativ-Tab, Redstone-Linien an/aus (Helligkeit 1/7/15), Platten-Lagen, Worldgen-Schnitte."
  n119-ores-tab       creative inventory (E) -> tab simpleores:ores opened with the REAL mouse (pages turned with the real arrows), the
                      grid scrolled with the real wheel to the end; every item seen is collected from the probe status: ores, plates
                      (lead plate, silver sheet), all eight metal doors + trapdoors and the silver mirror must be there; tooltips of
                      a door and the mirror hovered with the real pointer. Shots of every page of the tab.
  n119-ores-lines     the three lines side by side (Astral fed by an Astral Redstone block, Nihil by a Nihil Redstone block, Bridge by a
                      redstone block), 15 dust each, a lamp of the line at the end: signal 15 / 7 / 1 at piece 1 / 9 / 15 read from
                      the block state, lamps lit; day + night shots, close-ups at 15, 7 and 1; then the sources taken away: every piece
                      0, lamps dark, shots "aus".
  n119-ores-plates    lead plates and silver sheets with 1..8 layers lying on the floor, plates on a wall, the eight metal doors (closed
                      and every second one open), trapdoors, the silver mirror on floor / wall / ceiling; every state read back.
  n119-ores-worldgen  the real configured features placed with /place feature into blocks of host rock, then the front half cut away
                      (cross-section): overworld (stone over deepslate: tin, lead, silver, deepslate nickel, deepslate dimensional
                      scrap, zinc geode), Nether (netherrack: Nether dimensional scrap), End (end stone: Astralit, Nihilith, End
                      silver, End lead, End dimensional scrap). After the shots each ore is counted with a fill-replace in the
                      remaining half (the count line in the chat log; > 0 for every feature that has to hit). Also the Client-Smoke
                      look of the three dimensional scrap blocks side by side.
Simple Blocks "Formsäge mit und ohne Simple Interfaces (Margin, Titel), Zauntor nach N89D, Schnee-/Pale-/Kollabiert-Blöcke."
  n119-blocks-saw     a Shapecutter opened with a real right click (crosshair checked first; up to 3 tries), title "Shapecutter",
                      16 stone bricks moved into the input with real slot clicks; shot in the Simple Interfaces box look, the real
                      click on the style toggle above the frame -> Vanilla stonecutter look, shot, toggle back. Same for the Pulse
                      Shapecutter (machine screen).
  n119-blocks-scenes  nether brick fence gate between fences and between walls, closed and open (states read back); Snowlogged fence
                      (1 and 3 layers, Held = oak fence), snow caps on a fence, a wall and stairs, frost grass under snow next to
                      grass; the five Pale blocks over their Vanilla blocks; all 19 Collapsed blocks in a row. Every block read back.

Patterns (uiprobe_n89c/n100b): position via int(float()) of status 'pos'; 26.x gamerules advance_time / spawn_mobs; blocks set and read
with commands (no real clicks after 'tp ... facing': the camera turns), real clicks only inside screens and for the two right clicks that
open a screen. Results of '/execute if block ... run say N119 <tag>' and fill counts are read from client.log ([CHAT] lines).

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe119 --scale 2 \
        --only n119-ores-tab,n119-ores-lines,n119-ores-plates,n119-ores-worldgen,n119-blocks-saw,n119-blocks-scenes
"""
import re
import time

METALS = ['silver', 'lead', 'zinc', 'nickel', 'tin', 'brass', 'bronze', 'stainless_steel']


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _plain(label):
    return re.sub('§.', '', label or '')


def _look(s, px, py, pz, tx, ty, tz, wait=1.0):
    s.chat(f'tp @s {px} {py} {pz} facing {tx} {ty} {tz}', wait)
    time.sleep(0.6)


def _log(s):
    p = s.out / 'client.log'
    return p.read_text(errors='replace') if p.exists() else ''


def _say_if(s, x, y, z, state, tag):
    s.chat(f'execute if block {x} {y} {z} {state} run say N119 {tag}', 0.35)


def _said(s, tags, settle=1.5):
    """Which of the tags came back as '[CHAT] ... N119 <tag>' (exact word end)."""
    time.sleep(settle)
    log = _log(s)
    return {t: bool(re.search(r'N119 ' + re.escape(t) + r'(?![\w-])', log)) for t in tags}


def _check_said(s, name, tags):
    got = _said(s, tags)
    miss = [t for t, ok in got.items() if not ok]
    s.check(name, not miss, f'{len(tags) - len(miss)}/{len(tags)}' + (f' missing {miss[:12]}' if miss else ''))


def _prep(s):
    s.close()
    s.chat('gamemode creative @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('kill @e[type=minecraft:slime]', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    s.chat('clear @s', 0.3)


def _wait_screen(s, fragment, seconds=6.0):
    st = s.wait_screen(fragment, seconds)
    return st if fragment in st.get('screen', '') else None


def _hover(s, st, x, y, name):
    s.io.move(*s.abs(st, x + 3, y + 3))
    time.sleep(0.3)
    s.io.move(*s.abs(st, x, y))
    time.sleep(0.9)
    s.shot(name)


def _park(s, st):
    s.io.move(*s.abs(st, 4, 4))
    time.sleep(0.3)


# ---------------------------------------------------------------- Simple Ores: creative tab
def _open_tab(s, tab_id):
    s.close()
    s.key('e', 1.2)
    st = _wait_screen(s, 'CreativeModeInventoryScreen', 8)
    s.check('creative: E opens the creative inventory', st is not None, s.last.get('screen'))
    if not st:
        return None
    tab = next((t for t in st.get('tabs', []) if t['id'] == tab_id), None)
    s.check(f'creative: tab {tab_id} is listed', tab is not None, st.get('tabsError') or [t['id'] for t in st.get('tabs', [])][:40])
    if not tab:
        return None
    for _ in range(12):  # Fabric pages the tabs: turn with the real '<' / '>' buttons
        if 'page' not in tab or st.get('tabPage', tab['page']) == tab['page']:
            break
        arrow = '>' if st.get('tabPage', 0) < tab['page'] else '<'
        w = next((w for w in s.widgets(st) if _plain(w['label']) == arrow), None)
        if not w:
            break
        s.click_xy(w['x'], w['y'], st)
        time.sleep(0.6)
        st = s.st()
    cur = next((t for t in st.get('tabs', []) if t['id'] == tab_id), tab)
    s.click_xy(cur['x'], cur['y'], st)
    time.sleep(0.8)
    st = s.st()
    s.check(f'creative: real click selects {tab_id}', st.get('selectedTab') == tab_id, st.get('selectedTab'))
    return st if st.get('selectedTab') == tab_id else None


def _grid(st):
    return [sl for sl in st.get('slots', []) if not sl['own'] and sl['stack']]


def _collect_tab(s, st, tag, rounds=60):
    """Scrolls the creative grid with the real wheel to the end; returns (ordered item ids, {id: (slot, page)})."""
    seen, where, still = [], {}, 0
    page = 0
    _park(s, st)
    s.shot(f'{tag}-page-{page:02d}')
    for _ in range(rounds):
        new = 0
        for sl in _grid(st):
            iid = sl['stack'].split(' x')[0]
            if iid not in where:
                seen.append(iid)
                new += 1
            where[iid] = (sl, page)
        if new == 0:
            still += 1
            if still >= 2:
                break
        else:
            still = 0
        st = s.wheel(640, 300, 5, down=True, st=st)
        page += 1
        if new and page % 2 == 0:
            _park(s, st)
            s.shot(f'{tag}-page-{page:02d}')
    return seen, where


def ores_tab(s):
    _prep(s)
    st = _open_tab(s, 'simpleores:ores')
    if not st:
        s.close()
        return
    seen, where = _collect_tab(s, st, 'ores-tab')
    ids = set(seen)
    s.check('Ores tab: items collected while scrolling', len(ids) > 45, f'{len(ids)} items, first {seen[:8]}')
    want = {
        'ores': ['simpleores:' + o for o in ('tin_ore', 'silver_ore', 'deepslate_silver_ore', 'lead_ore', 'zinc_ore', 'deepslate_nickel_ore',
                                             'astralit_ore', 'nihilith_ore', 'end_silver_ore', 'end_lead_ore', 'nether_rose_gold_ore',
                                             'dimensional_scrap')],
        'plates': ['simpleores:lead_plate', 'simpleores:silver_sheet'],
        'doors': [f'simpleores:{m}_door' for m in METALS] + [f'simpleores:{m}_trapdoor' for m in METALS],
        'mirror': ['simpleores:silver_mirror'],
    }
    for group, items in want.items():
        miss = [i for i in items if i not in ids]
        s.check(f'Ores tab shows the {group} ({len(items)})', not miss, f'missing {miss}' if miss else 'all there')
    # tooltips of a door and the mirror: scroll back to the top, then down to the page that showed it, hover it
    for iid, name in (('simpleores:lead_door', 'tooltip-lead-door'), ('simpleores:silver_mirror', 'tooltip-silver-mirror'),
                      ('simpleores:lead_plate', 'tooltip-lead-plate')):
        if iid not in where:
            continue
        st = s.wheel(640, 300, 120, down=False)
        _, page = where[iid]
        for _ in range(page):
            st = s.wheel(640, 300, 5, down=True, st=st)
        sl = next((x for x in _grid(st) if x['stack'].startswith(iid + ' ')), None)
        s.check(f'{iid} found again on its page', sl is not None, f'page {page}')
        if sl:
            _hover(s, st, sl['x'], sl['y'], name)
    s.close()


# ---------------------------------------------------------------- Simple Ores: redstone lines
LANES = [('astral', 'simpleores:astral_redstone', 'simpleores:astral_redstone_block', 'simpleores:astralit_lamp'),
         ('nihil', 'simpleores:nihil_redstone', 'simpleores:nihil_redstone_block', 'simpleores:nihilith_lamp'),
         ('bridge', 'simpleores:bridge_redstone', 'minecraft:redstone_block', 'minecraft:redstone_lamp')]


def ores_lines(s):
    _prep(s)
    x, y, z = _pos(s)
    s.chat(f'fill {x - 2} {y - 1} {z + 2} {x + 20} {y - 1} {z + 10} minecraft:smooth_stone', 0.4)
    s.chat(f'fill {x - 2} {y} {z + 2} {x + 20} {y + 3} {z + 10} minecraft:air', 0.4)
    for i, (name, dust, source, lamp) in enumerate(LANES):
        lz = z + 4 + 2 * i
        s.chat(f'setblock {x + 1} {y} {lz} {source}', 0.25)
        for k in range(15):
            s.chat(f'setblock {x + 2 + k} {y} {lz} {dust}[east=side,west=side]', 0.12)
        s.chat(f'setblock {x + 17} {y} {lz} {lamp}', 0.25)
    time.sleep(2.0)
    tags = []
    for i, (name, dust, source, lamp) in enumerate(LANES):
        lz = z + 4 + 2 * i
        for k, pw in ((0, 15), (8, 7), (14, 1)):
            _say_if(s, x + 2 + k, y, lz, f'{dust}[power={pw}]', f'{name}-p{pw}')
            tags.append(f'{name}-p{pw}')
        _say_if(s, x + 17, y, lz, f'{lamp}[lit=true]', f'{name}-lamp-on')
        tags.append(f'{name}-lamp-on')
    _check_said(s, 'lines on: signal 15 / 7 / 1 at piece 1 / 9 / 15 on every line, lamps lit', tags)
    _look(s, x + 9.5, y + 6, z - 1.5, x + 9.5, y, z + 6)
    s.shot('an-tag-uebersicht')
    for k, pw in ((0, 15), (8, 7), (14, 1)):
        _look(s, x + 2.5 + k, y + 1.6, z + 1.8, x + 2.5 + k, y, z + 6)
        s.shot(f'an-helligkeit-{pw:02d}')
    s.chat('effect clear @s night_vision', 0.3)
    s.chat('time set midnight', 0.6)
    _look(s, x + 9.5, y + 6, z - 1.5, x + 9.5, y, z + 6, 1.5)
    s.shot('an-nacht-uebersicht')
    for k, pw in ((0, 15), (8, 7), (14, 1)):
        _look(s, x + 2.5 + k, y + 1.6, z + 1.8, x + 2.5 + k, y, z + 6)
        s.shot(f'an-nacht-helligkeit-{pw:02d}')
    # off: sources taken away
    for i in range(len(LANES)):
        s.chat(f'setblock {x + 1} {y} {z + 4 + 2 * i} minecraft:smooth_stone', 0.25)
    time.sleep(2.5)
    tags = []
    for i, (name, dust, source, lamp) in enumerate(LANES):
        lz = z + 4 + 2 * i
        for k in (0, 8, 14):
            _say_if(s, x + 2 + k, y, lz, f'{dust}[power=0]', f'{name}-off{k}')
            tags.append(f'{name}-off{k}')
        _say_if(s, x + 17, y, lz, f'{lamp}[lit=false]', f'{name}-lamp-off')
        tags.append(f'{name}-lamp-off')
    _check_said(s, 'lines off: sources removed, every piece 0, lamps dark', tags)
    _look(s, x + 9.5, y + 6, z - 1.5, x + 9.5, y, z + 6, 1.5)
    s.shot('aus-nacht-uebersicht')
    s.chat('time set noon', 0.4)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    _look(s, x + 9.5, y + 6, z - 1.5, x + 9.5, y, z + 6, 1.0)
    s.shot('aus-tag-uebersicht')


# ---------------------------------------------------------------- Simple Ores: plates, doors, mirrors
def ores_plates(s):
    _prep(s)
    x, y, z = _pos(s)
    s.chat(f'fill {x - 2} {y - 1} {z + 2} {x + 18} {y - 1} {z + 14} minecraft:smooth_stone', 0.4)
    s.chat(f'fill {x - 2} {y} {z + 2} {x + 18} {y + 4} {z + 14} minecraft:air', 0.4)
    tags = []
    # layers 1..8 lying on the floor: lead plate row z+4, silver sheet row z+6
    for row, block in ((z + 4, 'simpleores:lead_plate'), (z + 6, 'simpleores:silver_sheet')):
        for n in range(1, 9):
            st = f'{block}[facing=up,layers={n}]'
            s.chat(f'setblock {x + 1 + n} {y} {row} {st}', 0.15)
            _say_if(s, x + 1 + n, y, row, st, f'{block.split(":")[1]}-l{n}')
            tags.append(f'{block.split(":")[1]}-l{n}')
    # plates on a wall (stone wall at z+9, plates on its south face, pointing north away from it)
    s.chat(f'fill {x + 2} {y} {z + 9} {x + 9} {y + 2} {z + 9} minecraft:stone_bricks', 0.3)
    for n, block in ((1, 'simpleores:lead_plate'), (4, 'simpleores:lead_plate'), (8, 'simpleores:lead_plate'),
                     (1, 'simpleores:silver_sheet'), (4, 'simpleores:silver_sheet'), (8, 'simpleores:silver_sheet')):
        bx = x + 2 + (0 if block.endswith('plate') else 4) + [1, 4, 8].index(n)
        st = f'{block}[facing=north,layers={n}]'
        s.chat(f'setblock {bx} {y + 1} {z + 8} {st}', 0.15)
        _say_if(s, bx, y + 1, z + 8, st, f'wall-{block.split(":")[1]}-l{n}')
        tags.append(f'wall-{block.split(":")[1]}-l{n}')
    _check_said(s, 'plate layers: lead plate + silver sheet 1..8 on the floor and 1/4/8 on a wall', tags)
    _look(s, x + 6.0, y + 2.6, z + 1.0, x + 6.0, y, z + 5.5)
    s.shot('platten-lagen-boden')
    _look(s, x + 6.0, y + 1.6, z + 5.5, x + 6.0, y + 1, z + 8.5)
    s.shot('platten-an-der-wand')
    # doors (row z+12, every second open) and trapdoors (z+11 floor, open/closed alternately)
    tags = []
    for i, m in enumerate(METALS):
        dx, op = x + 1 + 2 * i, 'true' if i % 2 else 'false'
        for half in ('lower', 'upper'):
            s.chat(f'setblock {dx} {y + (half == "upper")} {z + 13} simpleores:{m}_door[facing=north,half={half},hinge=left,open={op}]', 0.12)
        s.chat(f'setblock {dx} {y} {z + 11} simpleores:{m}_trapdoor[facing=north,half=bottom,open={op}]', 0.12)
        _say_if(s, dx, y + 1, z + 13, f'simpleores:{m}_door[half=upper,open={op}]', f'door-{m}')
        _say_if(s, dx, y, z + 11, f'simpleores:{m}_trapdoor[open={op}]', f'trapdoor-{m}')
        tags += [f'door-{m}', f'trapdoor-{m}']
    _check_said(s, 'eight metal doors (both halves) and trapdoors placed, every second open', tags)
    _look(s, x + 8.5, y + 2.2, z + 7.0, x + 8.5, y + 0.8, z + 13)
    s.shot('tueren-und-falltueren')
    # silver mirror on floor / wall / ceiling (east of the doors)
    mx = x + 20
    s.chat(f'fill {mx - 1} {y - 1} {z + 2} {mx + 7} {y + 4} {z + 8} minecraft:air', 0.3)
    s.chat(f'fill {mx - 1} {y - 1} {z + 2} {mx + 7} {y - 1} {z + 8} minecraft:smooth_stone', 0.3)
    s.chat(f'fill {mx - 1} {y + 3} {z + 2} {mx + 7} {y + 3} {z + 8} minecraft:smooth_stone', 0.3)
    s.chat(f'fill {mx - 1} {y} {z + 7} {mx + 7} {y + 2} {z + 7} minecraft:stone_bricks', 0.3)
    mirrors = [(mx + 1, y, z + 5, 'face=floor,facing=north', 'floor'), (mx + 3, y + 1, z + 6, 'face=wall,facing=north', 'wall'),
               (mx + 5, y + 2, z + 5, 'face=ceiling,facing=north', 'ceiling')]
    tags = []
    for bx, by, bz, props, tag in mirrors:
        s.chat(f'setblock {bx} {by} {bz} simpleores:silver_mirror[{props}]', 0.2)
        _say_if(s, bx, by, bz, f'simpleores:silver_mirror[{props}]', f'mirror-{tag}')
        tags.append(f'mirror-{tag}')
    _check_said(s, 'silver mirror on the floor, a wall and the ceiling', tags)
    _look(s, mx + 3.5, y + 1.6, z + 1.5, mx + 3.5, y + 1, z + 6)
    s.shot('spiegel-boden-wand-decke')
    s.chat(f'item replace entity @s weapon.mainhand with simpleores:silver_mirror', 0.3)
    _look(s, mx + 3.5, y + 1.0, z + 3.0, mx + 3.5, y + 1.6, z + 7)
    s.shot('spiegel-item-in-der-hand')
    s.chat('clear @s', 0.3)


# ---------------------------------------------------------------- Simple Ores: worldgen cuts
def _count(s, tag, box, block):
    s.chat(f'say N119 COUNT {tag}', 0.35)
    s.chat(f'fill {box} minecraft:air replace {block}', 0.5)


def _counts(s, tags):
    """Count after 'N119 COUNT <tag>': the next fill feedback line in client.log."""
    time.sleep(1.5)
    lines = _log(s).splitlines()
    out = {}
    for t in tags:
        idx = max((i for i, ln in enumerate(lines) if re.search(r'N119 COUNT ' + re.escape(t) + r'$', ln.strip())), default=-1)
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


def ores_worldgen(s):
    _prep(s)
    x, y, z = _pos(s)
    z0, zc, z1 = z + 6, z + 11, z + 16       # cube depth; cut plane at zc
    cubes = [
        ('overworld', x + 4, [('minecraft:deepslate', 0, 4), ('minecraft:stone', 5, 10)],
         [('tin_ore', 3, 8), ('lead_ore', 7, 8), ('silver_ore', 5, 6), ('nickel_ore', 3, 2), ('dimensional_scrap', 7, 2),
          ('zinc_geode', 5, 4)],
         ['tin_ore', 'lead_ore', 'silver_ore', 'deepslate_nickel_ore', 'dimensional_scrap', 'zinc_ore', 'geode_crust']),
        ('nether', x + 20, [('minecraft:netherrack', 0, 10)],
         [('nether_dimensional_scrap', 3, 3), ('nether_dimensional_scrap', 5, 6), ('nether_dimensional_scrap', 7, 8)],
         ['nether_dimensional_scrap']),
        ('end', x + 36, [('minecraft:end_stone', 0, 10)],
         [('astralit_ore', 3, 7), ('nihilith_ore', 7, 7), ('end_silver_ore', 3, 3), ('end_lead_ore', 7, 3),
          ('end_dimensional_scrap', 5, 5)],
         ['astralit_ore', 'dull_astralit_ore', 'nihilith_ore', 'dull_nihilith_ore', 'end_silver_ore', 'end_lead_ore', 'end_dimensional_scrap']),
    ]
    s.chat(f'fill {x - 2} {y} {z + 1} {x + 48} {y + 12} {z + 18} minecraft:air', 0.6)
    s.chat('say N119 PLACE-START', 0.4)
    placed = []
    for name, cx, layers, feats, _ in cubes:
        for block, a, b in layers:
            s.chat(f'fill {cx} {y + a} {z0} {cx + 10} {y + b} {z1} {block}', 0.4)
        for feat, fx, fy in feats:
            s.chat(f'place feature simpleores:{feat} {cx + fx} {y + fy} {zc}', 0.5)
            placed.append(feat)
    time.sleep(1.0)
    after = _log(s).split('N119 PLACE-START')[-1].splitlines()
    ok_n = sum(1 for ln in after if '[CHAT]' in ln and 'Placed' in ln)
    bad = [ln.split('[CHAT]')[-1].strip() for ln in after if '[CHAT]' in ln and ('Unknown' in ln or 'Incorrect' in ln or 'Failed' in ln)]
    s.check('/place feature: every overworld/End vein feature placed (scattered scrap may fail by chance)',
            ok_n >= len(placed) - 4 and not any('Unknown' in b or 'Incorrect' in b for b in bad), f'{ok_n}/{len(placed)} placed; {bad[:6]}')
    # the cut: front half away
    for name, cx, *_ in cubes:
        s.chat(f'fill {cx} {y} {z0} {cx + 10} {y + 10} {zc - 1} minecraft:air', 0.4)
    time.sleep(1.0)
    for name, cx, *_ in cubes:
        _look(s, cx + 5.5, y + 5.5, z - 2.5, cx + 5.5, y + 5.0, zc)
        s.shot(f'schnitt-{name}')
        _look(s, cx + 5.5, y + 6.5, zc - 4.0, cx + 5.5, y + 4.0, zc)
        s.shot(f'schnitt-{name}-nah')
    # Client-Smoke look of the three dimensional scrap blocks
    s.chat(f'setblock {x + 1} {y} {z + 3} simpleores:dimensional_scrap', 0.2)
    s.chat(f'setblock {x + 2} {y} {z + 3} simpleores:nether_dimensional_scrap', 0.2)
    s.chat(f'setblock {x + 3} {y} {z + 3} simpleores:end_dimensional_scrap', 0.2)
    _look(s, x + 2.5, y + 1.4, z + 1.2, x + 2.5, y + 0.4, z + 3.5)
    s.shot('dimensional-scrap-drei')
    # counts in the remaining half (after the shots: the fill takes the ores away)
    tags = []
    for name, cx, _, _, ores in cubes:
        box = f'{cx} {y} {zc} {cx + 10} {y + 10} {z1}'
        for ore in ores:
            _count(s, f'{name}-{ore}', box, f'simpleores:{ore}')
            tags.append(f'{name}-{ore}')
    got = _counts(s, tags)
    s.check('cut counts read for every ore', all(v is not None for v in got.values()), got)
    must = ['overworld-tin_ore', 'overworld-lead_ore', 'overworld-deepslate_nickel_ore', 'overworld-geode_crust', 'end-end_silver_ore',
            'end-end_lead_ore']
    s.check('veins/geode visible in the cut: tin, lead, deepslate nickel, geode crust, End silver, End lead',
            all((got.get(t) or 0) > 0 for t in must), {t: got.get(t) for t in must})
    s.check('overworld silver (size 5), Astralit + Nihilith (with dull variants) and the three scraps: counted (may be 0 by chance)',
            True, {t: v for t, v in got.items() if t not in must})


# ---------------------------------------------------------------- Simple Blocks: Shapecutter with / without Simple Interfaces
def _open_block(s, bx, by, bz, fragment, tag):
    for attempt in range(3):
        st = s.face(bx, by, bz)
        hit = st.get('hitBlock')
        if attempt == 0:
            s.check(f'{tag}: crosshair on the block', hit and hit.startswith('simpleblocks:'), hit)
        s.io.right_click()
        got = _wait_screen(s, fragment, 4)
        if got:
            return got
        s.close()
    s.check(f'{tag}: real right click opens the screen', False, s.last.get('screen'))
    return None


def _saw(s, block, bx, by, bz, tag, title):
    st = _open_block(s, bx, by, bz, 'Shapecutter', tag)
    if not st:
        return
    s.check(f'{tag}: screen title "{title}"', _plain(st.get('title', '')) == title, st.get('title'))
    # 16 stone bricks from the inventory (cslot 9) into the input (menu slot index 0) with two real clicks
    s.click_slot(cslot=9, own=True)
    time.sleep(0.4)
    s.click_slot(index=0)
    time.sleep(0.8)
    st = s.st()
    inp = next((x for x in st.get('slots', []) if x['index'] == 0), {})
    s.check(f'{tag}: stone bricks in the input', inp.get('stack', '').startswith('minecraft:stone_bricks'), inp.get('stack'))
    _park(s, st)
    s.shot(f'{tag}-simple-interfaces')
    tog = s.find_widget(cls='StyleToggleButton', st=st)
    s.check(f'{tag}: style toggle above the frame', tog is not None, '; '.join(s.labels(st))[:300])
    if tog:
        s.click_xy(tog['x'], tog['y'], st)
        time.sleep(0.8)
        st = s.st()
        _park(s, st)
        s.shot(f'{tag}-vanilla-steinsaege')
        s.check(f'{tag}: screen still open after the toggle', 'Shapecutter' in st.get('screen', ''), st.get('screen'))
        tog = s.find_widget(cls='StyleToggleButton', st=st)
        if tog:
            s.click_xy(tog['x'], tog['y'], st)
            time.sleep(0.6)
    s.close()


def blocks_saw(s):
    _prep(s)
    x, y, z = _pos(s)
    s.chat(f'fill {x - 1} {y} {z + 1} {x + 5} {y + 2} {z + 4} minecraft:air', 0.3)
    s.chat('item replace entity @s inventory.0 with minecraft:stone_bricks 16', 0.3)   # cslot 9; the hand stays empty
    s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.3)
    s.chat(f'setblock {x} {y} {z + 2} simpleblocks:shapecutter[facing=north]', 0.4)
    _saw(s, 'simpleblocks:shapecutter', x, y, z + 2, 'formsaege', 'Shapecutter')
    s.chat('clear @s', 0.3)
    s.chat('item replace entity @s inventory.0 with minecraft:stone_bricks 16', 0.3)
    s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.3)
    s.chat(f'setblock {x} {y} {z + 2} minecraft:air', 0.3)
    s.chat(f'setblock {x} {y} {z + 2} simpleblocks:redstone_shapecutter[facing=north]', 0.4)
    _saw(s, 'simpleblocks:redstone_shapecutter', x, y, z + 2, 'takt-formsaege', 'Pulse Shapecutter')
    s.chat('clear @s', 0.3)


# ---------------------------------------------------------------- Simple Blocks: gate, snow, pale, collapsed
GATE = 'simpleblocks:nether_brick_fence_gate'
COLLAPSED = ['dirt', 'mud', 'cobblestone', 'stone', 'sand', 'red_sand', 'gravel', 'clay', 'netherrack', 'cobbled_deepslate', 'andesite',
             'diorite', 'granite', 'tuff', 'sandstone', 'red_sandstone', 'end_stone', 'blackstone', 'soul_sand']


def blocks_scenes(s):
    _prep(s)
    x, y, z = _pos(s)
    s.chat(f'fill {x - 3} {y - 1} {z + 2} {x + 24} {y - 1} {z + 16} minecraft:smooth_stone', 0.4)
    s.chat(f'fill {x - 3} {y} {z + 2} {x + 24} {y + 4} {z + 16} minecraft:air', 0.4)
    # gate: fence row z+4 (x..x+4), wall row z+4 (x+8..x+12)
    tags = []
    for gx, side, wall in ((x + 2, 'minecraft:nether_brick_fence', 'false'), (x + 10, 'minecraft:nether_brick_wall', 'true')):
        s.chat(f'setblock {gx - 2} {y} {z + 4} minecraft:nether_bricks', 0.15)
        s.chat(f'setblock {gx + 2} {y} {z + 4} minecraft:nether_bricks', 0.15)
        s.chat(f'setblock {gx - 1} {y} {z + 4} {side}', 0.15)
        s.chat(f'setblock {gx + 1} {y} {z + 4} {side}', 0.15)
        s.chat(f'setblock {gx} {y} {z + 4} {GATE}[facing=south,in_wall={wall},open=false]', 0.3)
        _say_if(s, gx, y, z + 4, f'{GATE}[open=false,in_wall={wall}]', f'gate-closed-{wall}')
        tags.append(f'gate-closed-{wall}')
    _check_said(s, 'nether brick fence gate between fences and between walls (in_wall), closed', tags)
    _look(s, x + 6.5, y + 2.0, z + 0.5, x + 6.5, y + 0.5, z + 4.5)
    s.shot('zauntor-zu-zaun-und-mauer')
    _look(s, x + 4.6, y + 1.8, z + 2.0, x + 2.5, y + 0.5, z + 4.5)
    s.shot('zauntor-zu-zaun-nah')
    tags = []
    for gx, wall in ((x + 2, 'false'), (x + 10, 'true')):
        s.chat(f'setblock {gx} {y} {z + 4} {GATE}[facing=south,in_wall={wall},open=true]', 0.3)
        _say_if(s, gx, y, z + 4, f'{GATE}[open=true,in_wall={wall}]', f'gate-open-{wall}')
        tags.append(f'gate-open-{wall}')
    _check_said(s, 'gate open between fences and walls', tags)
    _look(s, x + 6.5, y + 2.0, z + 0.5, x + 6.5, y + 0.5, z + 4.5)
    s.shot('zauntor-offen-zaun-und-mauer')
    # snow: snowlogged fence 1 + 3 layers, snow caps on fence / wall / stairs, frost grass under snow beside grass (row z+8)
    tags = []
    held = '{Held:{Name:"minecraft:oak_fence",Properties:{east:"false",north:"false",south:"false",west:"false",waterlogged:"false"}}}'
    for bx, n in ((x + 1, 1), (x + 3, 3)):
        s.chat(f'setblock {bx} {y} {z + 8} simpleblocks:snowlogged[layers={n}]{held}', 0.3)
        _say_if(s, bx, y, z + 8, f'simpleblocks:snowlogged[layers={n}]', f'snowlogged-{n}')
        tags.append(f'snowlogged-{n}')
    for bx, base in ((x + 5, 'minecraft:oak_fence'), (x + 7, 'minecraft:cobblestone_wall'), (x + 9, 'minecraft:stone_brick_stairs[facing=north]')):
        s.chat(f'setblock {bx} {y} {z + 8} {base}', 0.2)
        s.chat(f'setblock {bx} {y + 1} {z + 8} simpleblocks:snow_cap', 0.25)
        _say_if(s, bx, y + 1, z + 8, 'simpleblocks:snow_cap', f'snowcap-{bx - x}')
        tags.append(f'snowcap-{bx - x}')
    s.chat(f'setblock {x + 11} {y - 1} {z + 8} minecraft:grass_block[snowy=true]', 0.2)
    s.chat(f'setblock {x + 11} {y} {z + 8} minecraft:snow', 0.2)
    s.chat(f'setblock {x + 12} {y - 1} {z + 8} simpleblocks:frost_grass[snowy=true]', 0.2)
    s.chat(f'setblock {x + 12} {y} {z + 8} minecraft:snow', 0.2)
    s.chat(f'setblock {x + 13} {y - 1} {z + 8} simpleblocks:frost_grass[snowy=false]', 0.2)
    _say_if(s, x + 12, y - 1, z + 8, 'simpleblocks:frost_grass', 'frost-grass')
    tags.append('frost-grass')
    _check_said(s, 'snow: Snowlogged fence 1 + 3 layers, snow caps on fence / wall / stairs, frost grass', tags)
    _look(s, x + 7.0, y + 2.2, z + 5.0, x + 7.0, y + 0.3, z + 8.5)
    s.shot('schnee-bloecke')
    _look(s, x + 2.5, y + 1.4, z + 6.4, x + 2.5, y + 0.3, z + 8.5)
    s.shot('schnee-verschneiter-zaun-nah')
    # pale: five Pale blocks over their Vanilla blocks (row z+12)
    vanilla = ['minecraft:pumpkin', 'minecraft:carved_pumpkin[facing=north]', 'minecraft:jack_o_lantern[facing=north]', 'minecraft:melon',
               'minecraft:hay_block']
    pale = ['pale_pumpkin', 'pale_carved_pumpkin[facing=north]', 'pale_jack_o_lantern[facing=north]', 'pale_melon', 'pale_hay_block']
    tags = []
    for i, (v, p) in enumerate(zip(vanilla, pale)):
        s.chat(f'setblock {x + 1 + 2 * i} {y} {z + 12} {v}', 0.2)
        s.chat(f'setblock {x + 1 + 2 * i} {y + 1} {z + 12} simpleblocks:{p}', 0.2)
        pid = p.split('[')[0]
        _say_if(s, x + 1 + 2 * i, y + 1, z + 12, f'simpleblocks:{pid}', f'pale-{pid}')
        tags.append(f'pale-{pid}')
    _check_said(s, 'the five Pale blocks over their Vanilla blocks', tags)
    _look(s, x + 5.5, y + 2.0, z + 8.6, x + 5.5, y + 0.8, z + 12.5)
    s.shot('pale-ueber-vanilla')
    # collapsed: all 19 in a row (z+15)
    tags = []
    for i, c in enumerate(COLLAPSED):
        s.chat(f'setblock {x - 2 + i} {y} {z + 15} simpleblocks:collapsed_{c}', 0.12)
        _say_if(s, x - 2 + i, y, z + 15, f'simpleblocks:collapsed_{c}', f'collapsed-{c}')
        tags.append(f'collapsed-{c}')
    _check_said(s, 'all 19 Collapsed blocks placed', tags)
    _look(s, x + 7.5, y + 3.0, z + 9.0, x + 7.5, y + 0.4, z + 15.5)
    s.shot('kollabiert-alle')
    _look(s, x + 1.5, y + 1.4, z + 12.8, x + 1.5, y + 0.4, z + 15.5)
    s.shot('kollabiert-nah')


SCENARIOS = {
    'n119-ores-tab': ores_tab,
    'n119-ores-lines': ores_lines,
    'n119-ores-plates': ores_plates,
    'n119-ores-worldgen': ores_worldgen,
    'n119-blocks-saw': blocks_saw,
    'n119-blocks-scenes': blocks_scenes,
}
