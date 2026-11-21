"""N126 release polish, mods 23-25 (offen-vor-release.md section 3, "Szenen" of Simple Money, Simple Crucibles, Simple Ovens), word for word:

Simple Money        "Händlerfenster mit Geldangebot (W4C-32)."
  n126-money-merchant   Wandering trader (NoAI): REAL right click -> MerchantScreen; its offers are generated then, the server names the
                        offer that costs Money Bills (the money link Simple Money adds as an extra draw); REAL click on that offer tile
                        -> bills in the payment slot, the linked item in the result slot; REAL click on the result -> on the cursor,
                        bills fewer. Then a librarian with a fixed money offer (3 bills -> enchanted book, like librarian/03): REAL
                        right click, tile, payment slot = bills. Shots of both screens (bill tile look, foil, tooltip).
Simple Crucibles    "Seelenlava-Optik, Feldküche, JEI/Jade der Tiegel (W4C-13)."
  n126-crucibles-soullava  A soul lava source on a stone floor spreads (flowing block beside it); shots by day and by night. Player in a
                        3-deep soul lava pool, eyes inside: the fog is turquoise (shot centre blue > red), a Vanilla lava pool beside it
                        orange (red > blue). Survival with Fire Resistance in 1-deep soul lava: the Soul Burn effect is on the player,
                        shot of the burn overlay; restored afterwards.
  n126-crucibles-kitchen   All five field kitchen templates (plains, desert, savanna, snowy, taiga) placed with /place template: crucible
                        on an UNLIT campfire, food in the crucible, the barrel and the seat; overview + close-up shots; REAL empty-hand
                        right click on the plains crucible opens CrucibleScreen with the cooked food in its slots.
  n126-crucibles-jeijade   Jade: iron crucible on a magma block cooking potatoes, crosshair on it -> the Jade box at the top differs
                        from looking at plain floor (crop compare), shot. JEI: survival inventory, REAL pointer on a baked potato + R
                        -> JEI's recipe screen, shot; REAL pointer on the iron crucible + U -> JEI uses (crucible category), shot; the
                        U key must not open Simple Interfaces' key page while JEI is under the pointer.
Simple Ovens        "Ofenboot-Stufen, Stufen-Loren, Rauch (W4C-38)."
  n126-ovens-boats      Plain/Reinforced/Netherite/Enderite oak furnace boats + Enderite bamboo raft on water (overview from above).
                        REAL right click with coal on the plain and the Enderite boat: Fuel 3600 and 28800 (x8 = Enderite speed).
                        Empty hand REAL right click on the Enderite boat -> riding; third person (F5) shots of the smoke, two frames
                        differ; REAL W held 1.5 s -> the boat moves; the fuel burns while ridden.
  n126-ovens-carts      Three parallel rails; Reinforced/Netherite/Enderite furnace minecarts; REAL right click with coal on each:
                        Fuel 7200/14400/28800 and they drive off east; after 3 s every cart moved; the Enderite cart is ahead of the
                        Reinforced one; shot of the smoking carts.

Patterns of N122-N125: position via int(float()) of status 'pos'; crosshair read back before every real world click; entities within
~4 blocks (stand next to each target); 'tp ... facing' with y - 1.62; count feedback "Test passed[.,] Count: N"; chat commands
~1-1.5 s each; random_tick_speed 0 for still scenes; Simple Tweaks T27 keeps hotbar/armor/offhand on death (no death planned here).
Probe-only file, no game code.

    /root/sbt b-mod126 "timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe126 --scale 2 \\
        --only n126-money-merchant,n126-crucibles-soullava,n126-crucibles-kitchen,n126-crucibles-jeijade,n126-ovens-boats,n126-ovens-carts"
"""
import re
import time
from pathlib import Path

TAG = 'N126'
REPO = Path(__file__).resolve().parents[3]


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _log(s):
    p = s.out / 'client.log'
    return p.read_text(errors='replace') if p.exists() else ''


def _say(s, cond, tag):
    """cond = the part between 'execute' and 'run', e.g. 'if block 1 2 3 minecraft:stone'."""
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


def _mark(s, marker):
    s.chat(f'say {TAG} MARK {marker}', 0.35)


def _after(s, marker, pattern, lines_after=30):
    """Last regex match of pattern in the log lines after the last 'N126 MARK <marker>' line."""
    time.sleep(1.2)
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} MARK {marker}')), default=-1)
    found = None
    for ln in lines[idx + 1: idx + 1 + lines_after] if idx >= 0 else []:
        m = re.search(pattern, ln)
        if m:
            found = m
    return found


def _data(s, marker, command):
    """Runs a '/data get ...' and returns the number it reports (None when the log shows none)."""
    _mark(s, marker)
    s.chat(command, 0.5)
    m = _after(s, marker, r'has the following entity data: (-?[\d.]+)')
    return float(m.group(1)) if m else None


def _prep(s, mode='creative'):
    s.close()
    s.chat('ride @s dismount', 0.3)
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true', 'keep_inventory false', 'random_tick_speed 0'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    s.chat('clear @s', 0.3)


def _done(s):
    s.close()
    s.chat('ride @s dismount', 0.3)
    s.chat('gamemode creative @s', 0.3)
    s.chat('gamerule random_tick_speed 3', 0.3)
    s.chat('time set noon', 0.3)
    s.chat('clear @s', 0.3)


def _move(s, dx):
    """Moves dx blocks east onto fresh flat ground (the scenarios do not share space) and returns the new feet position."""
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + dx + 0.5} {y} {z + 0.5} -90 0', 1.0)
    time.sleep(1.5)
    return _pos(s)


def _floor(s, x0, z0, x1, z1, y, block='minecraft:smooth_stone', clear=5):
    s.chat(f'fill {x0} {y} {z0} {x1} {y + clear} {z1} minecraft:air', 0.5)
    s.chat(f'fill {x0} {y - 1} {z0} {x1} {y - 1} {z1} {block}', 0.5)


def _wait_screen(s, fragment, seconds=6.0):
    st = s.wait_screen(fragment, seconds)
    return st if fragment in st.get('screen', '') else None


def _park(s, st):
    s.io.move(*s.abs(st, 4, 4))
    time.sleep(0.3)


def _look(s, fx, fy, fz, px, py, pz, wait=1.0):
    """Stands at feet point (fx, fy, fz) and turns the EYES onto the point (tp facing turns from the feet: y - 1.62)."""
    s.chat(f'tp @s {fx:.2f} {fy} {fz:.2f} facing {px:.3f} {py - 1.62:.3f} {pz:.3f}', wait)


def _aim_block(s, fx, fy, fz, px, py, pz, want, tag, tries=4):
    hit = None
    for _ in range(tries):
        _look(s, fx, fy, fz, px, py, pz, 0.8)
        for _ in range(6):
            hit = s.st().get('hitBlock')
            if hit == want:
                return True
            time.sleep(0.3)
    s.check(f'{tag}: crosshair on {want}', False, hit)
    return False


def _aim_entity(s, fx, fy, fz, px, py, pz, want, tag, tries=4):
    hit = None
    for _ in range(tries):
        _look(s, fx, fy, fz, px, py, pz, 0.8)
        for _ in range(6):
            hit = s.st().get('hitEntity')
            if hit == want:
                return True
            time.sleep(0.3)
    s.check(f'{tag}: crosshair on entity {want}', False, hit)
    return False


def _shot_path(s, name):
    return s.out / 'shots' / f"{s.scenario.replace('/', '-')}-{name}.png"


def _mean_rgb(path, box=(0.35, 0.35, 0.65, 0.65)):
    """Mean (r, g, b) of the box (fractions of the picture) of a screenshot (PIL); None when unreadable."""
    try:
        from PIL import Image, ImageStat
        im = Image.open(path).convert('RGB')
        w, h = im.size
        crop = im.crop((int(box[0] * w), int(box[1] * h), int(box[2] * w), int(box[3] * h)))
        return tuple(round(v, 1) for v in ImageStat.Stat(crop).mean)
    except Exception as e:  # noqa: BLE001
        return None


def _differs(a, b, min_rms=2.0, box=None):
    """(changed, rms) of two screenshots (PIL), optionally only inside box (fractions); a missing file counts as unchanged."""
    try:
        from PIL import Image, ImageChops, ImageStat
        ia, ib = Image.open(a).convert('RGB'), Image.open(b).convert('RGB')
        if ia.size != ib.size:
            return True, 999.0
        if box:
            w, h = ia.size
            rect = (int(box[0] * w), int(box[1] * h), int(box[2] * w), int(box[3] * h))
            ia, ib = ia.crop(rect), ib.crop(rect)
        rms = sum(ImageStat.Stat(ImageChops.difference(ia, ib)).rms) / 3
        return rms >= min_rms, round(rms, 2)
    except Exception as e:  # noqa: BLE001
        return False, f'{type(e).__name__}: {e}'


def _count(st, item):
    n = 0
    for text in st.get('inventory', []):
        m = re.fullmatch(re.escape(item) + r' x(\d+)', text or '')
        if m:
            n += int(m.group(1))
    return n


def _merchant_slots(st):
    return {sl['cslot']: sl.get('stack', '') for sl in st.get('slots', []) if not sl.get('own')}


# ---------------------------------------------------------------- Simple Money
def money_merchant(s):
    _prep(s)
    x, y, z = _move(s, 40)
    _floor(s, x - 2, z - 4, x + 6, z + 4, y)
    s.chat('kill @e[type=minecraft:wandering_trader,distance=..40]', 0.4)
    s.chat('kill @e[type=minecraft:villager,distance=..40]', 0.4)
    tx, tz = x + 2.5, z + 0.5
    s.chat(f'execute positioned {tx} {y} {tz} summon minecraft:wandering_trader run data merge entity @s '
           f'{{NoAI:1b,Invulnerable:1b,PersistenceRequired:1b,Tags:["n126"],Rotation:[90f,0f]}}', 1.0)
    s.chat('item replace entity @s inventory.0 with simplemoney:money_bill 64', 0.4)
    s.chat('item replace entity @s inventory.1 with minecraft:emerald 64', 0.4)
    s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.3)
    if not _aim_entity(s, x + 0.5, y, z + 0.5, tx, y + 1.3, tz, 'minecraft:wandering_trader', 'wandering trader'):
        return _done(s)
    s.io.right_click()
    st = _wait_screen(s, 'MerchantScreen', 6)
    s.check('REAL right click on the wandering trader opens the trading screen (MerchantScreen)', st is not None, s.last.get('screen'))
    if not st:
        return _done(s)
    time.sleep(1.0)
    st = s.st()
    tiles = sorted([w for w in s.widgets(st) if w['class'] == 'TradeOfferButton'], key=lambda w: w['y'])
    # the server names the offers now that they exist: how many, and which one costs Money Bills
    sel = '@e[type=minecraft:wandering_trader,tag=n126,limit=1]'
    for i in range(10):
        _say(s, f'if data entity {sel} Offers.Recipes[{i}]', f'OFFER{i}')
        _say(s, f'if data entity {sel} Offers.Recipes[{i}].buy{{id:"simplemoney:money_bill"}}', f'BILL{i}')
    got = _said(s, [f'OFFER{i}' for i in range(10)] + [f'BILL{i}' for i in range(10)])
    offers = sum(1 for i in range(10) if got[f'OFFER{i}'])
    bills = [i for i in range(10) if got[f'BILL{i}']]
    s.check('the wandering trader has an offer priced in Money Bills (the money link draw)', bool(bills), f'{offers} offers, bill offers {bills}')
    s.check('offer tiles on the screen = offers of the trader (up to 7 visible)', len(tiles) == min(offers, 7), f'{len(tiles)} tiles, {offers} offers')
    _park(s, st)
    s.shot('1-wandering-offers')
    idx = bills[0] if bills else None
    if idx is not None and tiles and idx >= len(tiles):
        # 7 tiles are visible: scroll the offer list (one offer per wheel click) until the money offer is the last visible one
        clicks = idx - len(tiles) + 1
        s.wheel(tiles[0]['x'], tiles[0]['y'], clicks=clicks, down=True, st=st)
        st = s.st()
        tiles = sorted([w for w in s.widgets(st) if w['class'] == 'TradeOfferButton'], key=lambda w: w['y'])
        idx -= clicks
    if idx is not None and idx < len(tiles):
        tile = tiles[idx]
        s.io.move(*s.abs(st, tile['x'], tile['y']))
        time.sleep(1.0)
        s.shot('2-money-offer-hover')
        before = _count(s.st(), 'simplemoney:money_bill')
        s.click_xy(tile['x'], tile['y'], st)
        time.sleep(1.0)
        st = s.st()
        merch = _merchant_slots(st)
        s.check('REAL click on the money offer: Money Bills in the payment slot, the linked item in the result slot',
                str(merch.get(0, '')).startswith('simplemoney:money_bill') and bool(merch.get(2)), merch)
        _park(s, st)
        s.shot('3-money-offer-chosen')
        if merch.get(2):
            res = next((sl for sl in st.get('slots', []) if not sl.get('own') and sl['cslot'] == 2), None)
            if res:
                s.click_xy(res['x'], res['y'], st)
                time.sleep(1.0)
                st = s.st()
                carried = st.get('carried', '')
                pay = str(_merchant_slots(st).get(0, ''))
                left = _count(st, 'simplemoney:money_bill') + (int(pay.rsplit('x', 1)[1]) if pay.startswith('simplemoney:money_bill') else 0)
                s.check('REAL click on the result: the linked item on the cursor, Money Bills paid', bool(carried) and left < before,
                        f'carried {carried!r}, bills before {before}, after {left}')
                _park(s, st)
                s.shot('4-bought')
    else:
        s.check('the money offer is one of the visible tiles', False, f'bill offers {bills}, {len(tiles)} tiles')
    s.close()
    # a villager with a fixed money offer (librarian/03: 3 bills -> enchanted book), level 2 so the XP bar shows
    s.chat('kill @e[type=minecraft:wandering_trader,distance=..40]', 0.4)
    s.chat('clear @s', 0.3)
    s.chat('item replace entity @s inventory.0 with simplemoney:money_bill 16', 0.4)
    s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.3)
    offers_nbt = ('{Recipes:[{buy:{id:"simplemoney:money_bill",count:3},sell:{id:"minecraft:enchanted_book",count:1,'
                  'components:{"minecraft:stored_enchantments":{"minecraft:efficiency":5}}},maxUses:2,xp:2,priceMultiplier:0.2f},'
                  '{buy:{id:"minecraft:emerald",count:1},sell:{id:"minecraft:book",count:1},maxUses:12}]}')
    s.chat(f'execute positioned {tx} {y} {tz} summon minecraft:villager run data merge entity @s {{NoAI:1b,Invulnerable:1b,'
           f'PersistenceRequired:1b,Xp:15,Rotation:[90f,0f],VillagerData:{{profession:"minecraft:librarian",level:2,type:"minecraft:plains"}},'
           f'Offers:{offers_nbt}}}', 1.0)
    if not _aim_entity(s, x + 0.5, y, z + 0.5, tx, y + 1.3, tz, 'minecraft:villager', 'librarian'):
        return _done(s)
    s.io.right_click()
    st = _wait_screen(s, 'MerchantScreen', 6)
    s.check('REAL right click on the librarian opens MerchantScreen', st is not None, s.last.get('screen'))
    if st:
        time.sleep(0.8)
        st = s.st()
        tiles = sorted([w for w in s.widgets(st) if w['class'] == 'TradeOfferButton'], key=lambda w: w['y'])
        s.check('librarian: two offer tiles', len(tiles) == 2, len(tiles))
        if tiles:
            s.click_xy(tiles[0]['x'], tiles[0]['y'], st)
            time.sleep(1.0)
            st = s.st()
            merch = _merchant_slots(st)
            # Vanilla fills the payment slot with more than the price when the inventory has it; the price itself is 3 bills
            s.check('librarian: REAL click on the money offer -> Money Bills paid in, enchanted book in the result',
                    str(merch.get(0, '')).startswith('simplemoney:money_bill') and str(merch.get(2, '')).startswith('minecraft:enchanted_book'), merch)
            s.io.move(*s.abs(st, tiles[0]['x'], tiles[0]['y']))
            time.sleep(1.0)
            s.shot('5-librarian-money-offer')
    s.close()
    s.chat('kill @e[type=minecraft:villager,distance=..40]', 0.4)
    _done(s)


# ---------------------------------------------------------------- Simple Crucibles
def crucibles_soullava(s):
    _prep(s)
    x, y, z = _move(s, 40)
    _floor(s, x - 2, z - 8, x + 14, z + 8, y)
    s.chat(f'fill {x - 2} {y - 5} {z - 8} {x + 14} {y - 2} {z + 8} minecraft:stone', 0.5)
    # 1. a source on the floor spreads (flowing soul lava beside it; the Overworld flow is 2 blocks)
    sx, sz = x + 4, z - 4
    s.chat(f'setblock {sx} {y} {sz} simplecrucibles:soul_lava', 0.5)
    time.sleep(4.0)
    _say(s, f'if block {sx} {y} {sz} simplecrucibles:soul_lava[level=0]', 'SOURCE')
    _say(s, f'if block {sx + 1} {y} {sz} simplecrucibles:soul_lava', 'FLOW')
    _say(s, f'unless block {sx + 1} {y} {sz} simplecrucibles:soul_lava[level=0]', 'FLOWING')
    _check_said(s, 'soul lava source stays and spreads as flowing soul lava', ['SOURCE', 'FLOW', 'FLOWING'])
    _look(s, x + 0.5, y + 1, z - 0.5, sx + 0.5, y + 0.3, sz + 0.5)
    time.sleep(1.0)
    s.shot('1-flow-day')
    s.chat('time set midnight', 0.6)
    s.chat('effect clear @s minecraft:night_vision', 0.4)
    time.sleep(1.0)
    s.shot('2-flow-night')
    s.chat('time set noon', 0.4)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    # 2. fog: eyes inside a 3-deep pool of soul lava, then of Vanilla lava
    px, pz = x + 4, z + 3
    s.chat(f'fill {px - 1} {y - 4} {pz - 1} {px + 1} {y - 1} {pz + 1} simplecrucibles:soul_lava', 0.6)
    s.chat(f'fill {px + 5} {y - 4} {pz - 1} {px + 7} {y - 1} {pz + 1} minecraft:lava', 0.6)
    time.sleep(1.0)
    s.chat(f'tp @s {px + 0.5} {y - 3} {pz + 0.5} -90 0', 1.2)
    time.sleep(1.5)
    s.shot('3-fog-soul-lava')
    s.chat(f'tp @s {px + 6.5} {y - 3} {pz + 0.5} -90 0', 1.2)
    time.sleep(1.5)
    s.shot('4-fog-vanilla-lava')
    soul, lava = _mean_rgb(_shot_path(s, '3-fog-soul-lava')), _mean_rgb(_shot_path(s, '4-fog-vanilla-lava'))
    s.check('eyes in soul lava: turquoise fog (blue > red in the picture centre)', bool(soul) and soul[2] > soul[0], soul)
    s.check('eyes in Vanilla lava: orange fog (red > blue), so the colour is the mod\'s', bool(lava) and lava[0] > lava[2], lava)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} -90 0', 0.8)
    s.chat(f'fill {px - 1} {y - 4} {pz - 1} {px + 7} {y - 1} {pz + 1} minecraft:stone', 0.6)
    # 3. survival, Fire Resistance: one block of soul lava at the feet gives Soul Burn (damage blocked)
    bx, bz = x + 9, z - 4
    s.chat(f'setblock {bx} {y - 1} {bz} simplecrucibles:soul_lava', 0.5)
    s.chat('effect give @s minecraft:fire_resistance 120 0 true', 0.3)
    s.chat('effect give @s minecraft:resistance 120 4 true', 0.3)
    s.chat('effect give @s minecraft:regeneration 120 4 true', 0.3)
    s.chat('gamemode survival @s', 0.4)
    s.chat(f'tp @s {bx + 0.5} {y - 1} {bz + 0.5} -90 30', 1.0)
    time.sleep(2.5)
    _say(s, 'if data entity @s active_effects[{id:"simplecrucibles:soul_burn"}]', 'SOULBURN')
    s.shot('5-soul-burn-overlay')
    _check_said(s, 'standing in soul lava (survival, Fire Resistance) gives the Soul Burn effect', ['SOULBURN'])
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} -90 0', 0.8)
    s.chat('gamemode creative @s', 0.3)
    s.chat('effect clear @s', 0.3)
    s.chat(f'setblock {bx} {y - 1} {bz} minecraft:smooth_stone', 0.3)
    s.chat(f'fill {x - 2} {y} {z - 8} {x + 14} {y + 2} {z} minecraft:air', 0.5)
    _done(s)


KITCHENS = ['plains', 'desert', 'savanna', 'snowy', 'taiga']


def crucibles_kitchen(s):
    _prep(s)
    x, y, z = _move(s, 40)
    _floor(s, x - 2, z - 4, x + 26, z + 10, y)
    s.chat('kill @e[type=item,distance=..40]', 0.3)
    tags = []
    for k, kind in enumerate(KITCHENS):
        ox, oy, oz = x + 2 + 5 * k, y - 1, z + 2
        s.chat(f'place template simplecrucibles:village/{kind}/field_kitchen {ox} {oy} {oz}', 0.8)
        s.chat(f'setblock {ox} {oy} {oz} minecraft:smooth_stone', 0.3)  # the template's jigsaw block
        cx, cy, cz = ox + 1, oy + 1, oz + 2
        _say(s, f'if block {cx} {cy} {cz} simplecrucibles:iron_crucible', f'CRU-{kind}')
        _say(s, f'if block {cx} {cy - 1} {cz} minecraft:campfire[lit=false]', f'FIRE-{kind}')
        _say(s, f'if data block {cx} {cy} {cz} Contents[0]', f'FOOD-{kind}')
        _say(s, f'if block {cx} {cy} {cz - 1} minecraft:barrel', f'BARREL-{kind}')
        _say(s, f'if block {cx} {cy} {cz + 1} #minecraft:stairs', f'SEAT-{kind}')
        tags += [f'CRU-{kind}', f'FIRE-{kind}', f'FOOD-{kind}', f'BARREL-{kind}', f'SEAT-{kind}']
    _check_said(s, 'five field kitchens: crucible on an unlit campfire, food in it, barrel and seat', tags)
    _look(s, x + 12.5, y + 5, z - 4.5, x + 13, y, z + 4, 1.2)
    time.sleep(1.0)
    s.shot('1-five-kitchens')
    for k, kind in enumerate(KITCHENS):
        cx, cy, cz = x + 3 + 5 * k, y, z + 4
        _look(s, cx + 3.5, y, cz - 2.5, cx + 0.5, cy + 0.5, cz + 0.5, 1.0)
        time.sleep(0.6)
        s.shot(f'2-{kind}')
    # REAL empty-hand right click on the plains crucible: its screen shows the cooked food
    cx, cy, cz = x + 3, y, z + 4
    s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.3)
    if _aim_block(s, cx + 2.5, y, cz + 0.5, cx + 0.5, cy + 0.7, cz + 0.5, 'simplecrucibles:iron_crucible', 'plains crucible'):
        s.io.right_click()
        st = _wait_screen(s, 'CrucibleScreen', 6)
        s.check('REAL right click on the kitchen crucible opens CrucibleScreen', st is not None, s.last.get('screen'))
        if st:
            time.sleep(0.6)
            st = s.st()
            food = [sl['stack'] for sl in st.get('slots', []) if not sl.get('own') and sl['index'] < 6 and sl.get('stack')]
            s.check('the crucible slots hold the plains food (bread, cooked beef)',
                    any(f.startswith('minecraft:bread') for f in food) and any(f.startswith('minecraft:cooked_beef') for f in food), food)
            _park(s, st)
            s.shot('3-plains-crucible-screen')
            s.close()
    _done(s)


def crucibles_jeijade(s):
    _prep(s)
    x, y, z = _move(s, 40)
    _floor(s, x - 2, z - 4, x + 8, z + 4, y)
    cx, cy, cz = x + 3, y, z
    s.chat(f'setblock {cx} {cy - 1} {cz} minecraft:magma_block', 0.4)
    s.chat(f'setblock {cx} {cy} {cz} simplecrucibles:iron_crucible{{Contents:[{{Slot:0,Item:{{id:"minecraft:potato",count:1}},Count:8}},'
           f'{{Slot:1,Item:{{id:"minecraft:beef",count:1}},Count:4}}]}}', 0.6)
    time.sleep(1.5)
    # Jade box: crosshair on the crucible vs on bare floor - the top band of the picture differs
    if _aim_block(s, x + 0.5, y, z + 0.5, cx + 0.5, cy + 0.6, cz + 0.5, 'simplecrucibles:iron_crucible', 'Jade crucible'):
        time.sleep(1.5)
        s.shot('1-jade-crucible')
        _look(s, x + 0.5, y, z + 0.5, x + 0.5, y - 1, z - 2.5, 1.0)
        time.sleep(1.2)
        s.shot('2-jade-floor')
        _look(s, x + 0.5, y, z + 0.5, cx + 0.5, cy + 0.6, cz + 0.5, 1.0)
        time.sleep(1.2)
        s.shot('3-jade-crucible-again')
        changed, rms = _differs(_shot_path(s, '1-jade-crucible'), _shot_path(s, '3-jade-crucible-again'), box=(0.3, 0.0, 0.7, 0.18))
        floor, rms2 = _differs(_shot_path(s, '1-jade-crucible'), _shot_path(s, '2-jade-floor'), min_rms=4.0, box=(0.3, 0.0, 0.7, 0.18))
        s.check('Jade box at the top shows for the crucible (top band differs from looking at floor)', floor, f'rms {rms2} (crucible vs crucible {rms})')
    # JEI: survival inventory, pointer on a baked potato + R, pointer on the crucible + U
    s.chat('gamemode survival @s', 0.4)
    s.chat('item replace entity @s inventory.0 with minecraft:baked_potato 4', 0.3)
    s.chat('item replace entity @s inventory.1 with simplecrucibles:iron_crucible 1', 0.3)
    s.key('e', 1.2)
    st = _wait_screen(s, 'InventoryScreen', 5)
    s.check('survival inventory open', st is not None, s.last.get('screen'))
    if st:
        for cslot, key, name in ((9, 'r', 'recipes-baked-potato'), (10, 'u', 'uses-iron-crucible')):
            st = _wait_screen(s, 'InventoryScreen', 5) or s.st()
            sl = s.slot_xy(st, cslot, True)
            if not sl:
                s.check(f'inventory slot {cslot} present', False, len(st.get('slots', [])))
                continue
            s.io.move(*s.abs(st, sl['x'] + 2, sl['y'] + 2))
            time.sleep(0.3)
            s.io.move(*s.abs(st, sl['x'], sl['y']))
            time.sleep(0.8)
            s.key(key, 1.5)
            st = s.st()
            scr = st.get('screen', '')
            s.check(f'JEI: pointer on the {name.split("-", 1)[1].replace("-", " ")} + {key.upper()} opens the JEI recipe screen',
                    'jei' in scr.lower() or 'Recipes' in scr, scr)
            s.check(f'{key.upper()} under JEI does not open Simple Interfaces\' key page', 'KeysScreen' not in scr, scr)
            _park(s, st)
            time.sleep(0.5)
            s.shot(f'4-jei-{name}')
            s.key('Escape', 1.0)
            if 'InventoryScreen' not in s.screen():
                s.close()
                s.key('e', 1.2)
    s.close()
    s.chat('gamemode creative @s', 0.3)
    s.chat(f'setblock {cx} {cy} {cz} minecraft:air', 0.3)
    _done(s)


# ---------------------------------------------------------------- Simple Ovens
BOATS = [('oak_furnace_boat', 'plain'), ('reinforced_oak_furnace_boat', 'reinforced'), ('netherite_oak_furnace_boat', 'netherite'),
         ('enderite_oak_furnace_boat', 'enderite'), ('enderite_bamboo_furnace_raft', 'enderite raft')]


def _kill_boats(s):
    for kind, _ in BOATS:
        s.chat(f'kill @e[type=simpleovens:{kind},distance=..80]', 0.3)


def ovens_boats(s):
    _prep(s)
    x, y, z = _move(s, 40)
    _floor(s, x - 2, z - 8, x + 30, z + 8, y)
    s.chat(f'fill {x + 1} {y - 3} {z - 7} {x + 29} {y - 1} {z + 7} minecraft:water', 0.8)
    _kill_boats(s)
    for i, (kind, _) in enumerate(BOATS):
        s.chat(f'summon simpleovens:{kind} {x + 2.5} {y - 0.6} {z - 4 + 2 * i + 0.5} {{Rotation:[-90f,0f]}}', 0.5)
    time.sleep(1.5)
    for kind, _ in BOATS:
        _say(s, f'if entity @e[type=simpleovens:{kind},distance=..30]', f'BOAT-{kind}')
    _check_said(s, 'all four furnace tiers as boats + the Enderite raft on the water', [f'BOAT-{k}' for k, _ in BOATS])
    _look(s, x + 0.5, y + 6, z + 0.5, x + 2.5, y - 1, z + 0.5, 1.2)
    time.sleep(1.0)
    s.shot('1-boat-tiers')
    # REAL right click with coal on the plain and the Enderite boat: fuel per coal = 3600 x tier factor
    s.chat('item replace entity @s weapon.mainhand with minecraft:coal 8', 0.4)
    for i, (kind, label) in enumerate(BOATS):
        if label not in ('plain', 'enderite'):
            continue
        bz = z - 4 + 2 * i + 0.5
        if not _aim_entity(s, x + 0.5, y, bz, x + 2.5, y + 0.1, bz, f'simpleovens:{kind}', f'{label} boat'):
            continue
        s.io.right_click()
        time.sleep(0.8)
        fuel = _data(s, f'FUEL-{label}', f'data get entity @e[type=simpleovens:{kind},limit=1,sort=nearest] Fuel')
        want = 3600 if label == 'plain' else 28800
        # a held click repeats after 4 ticks (Vanilla): one or two coal per click, each worth `want`
        s.check(f'REAL right click with coal on the {label} boat: Fuel {want} per coal', fuel is not None and fuel >= want and fuel % want == 0, fuel)
    # ride the Enderite boat: empty hand, REAL right click
    i = 3
    kind = BOATS[i][0]
    bz = z - 4 + 2 * i + 0.5
    s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.3)
    if _aim_entity(s, x + 0.5, y, bz, x + 2.5, y + 0.1, bz, f'simpleovens:{kind}', 'Enderite boat (ride)'):
        s.io.right_click()
        time.sleep(1.0)
        _say(s, f'on vehicle if entity @s[type=simpleovens:{kind}]', 'RIDE')
        if _check_said(s, 'empty-hand REAL right click: the player sits in the Enderite furnace boat', ['RIDE']):
            s.key('F5', 1.2)
            s.shot('2-smoke-a')
            time.sleep(0.6)
            s.shot('3-smoke-b')
            changed, rms = _differs(_shot_path(s, '2-smoke-a'), _shot_path(s, '3-smoke-b'), min_rms=0.5)
            s.check('lit boat: the two frames differ (rising smoke)', changed, rms)
            fuel0 = _data(s, 'FUEL-RIDE0', f'data get entity @e[type=simpleovens:{kind},limit=1,sort=nearest] Fuel')
            p0 = _pos(s)
            bsel = f'@e[type=simpleovens:{kind},limit=1,sort=nearest]'
            yaw = _data(s, 'BOAT-YAW', f'data get entity {bsel} Rotation[0]')
            bx0 = _data(s, 'BOAT-X0', f'data get entity {bsel} Pos[0]')
            bz0 = _data(s, 'BOAT-Z0', f'data get entity {bsel} Pos[2]')
            s.io.key_down('w')
            try:
                time.sleep(1.5)
            finally:
                s.io.key_up('w')
            time.sleep(0.5)
            p1 = _pos(s)
            bx1 = _data(s, 'BOAT-X1', f'data get entity {bsel} Pos[0]')
            bz1 = _data(s, 'BOAT-Z1', f'data get entity {bsel} Pos[2]')
            s.shot('4-under-way')
            fuel1 = _data(s, 'FUEL-RIDE1', f'data get entity @e[type=simpleovens:{kind},limit=1,sort=nearest] Fuel')
            # the lit boat already pushes while the rider sits (smoke shots): measure from where it was summoned (x + 2.5)
            dist = (((bx1 - (x + 2.5)) ** 2 + (bz1 - bz) ** 2) ** 0.5) if None not in (bx1, bz1) else None
            s.check('the ridden lit boat travels east from its spot (>= 2 blocks)', dist is not None and dist >= 2 and bx1 > x + 2.5,
                    f'boat yaw {yaw}, ({bx0}, {bz0}) -> ({bx1}, {bz1}), player {p0} -> {p1}')
            s.check('the fuel burns while somebody sits in the boat', fuel0 is not None and fuel1 is not None and fuel1 < fuel0, f'{fuel0} -> {fuel1}')
            s.key('F5', 0.5)
            s.key('F5', 0.8)
    s.chat('ride @s dismount', 0.4)
    _kill_boats(s)
    s.chat(f'fill {x + 1} {y - 3} {z - 7} {x + 29} {y - 1} {z + 7} minecraft:stone', 0.6)
    _done(s)


CARTS = [('reinforced', 7200), ('netherite', 14400), ('enderite', 28800)]


def ovens_carts(s):
    _prep(s)
    x, y, z = _move(s, 40)
    length = 150
    _floor(s, x - 2, z - 4, x + length + 2, z + 4, y)
    s.chat(f'kill @e[type=minecraft:item,distance=..{length}]', 0.3)
    for i, (tier, _) in enumerate(CARTS):
        tz = z - 2 + 2 * i
        s.chat(f'kill @e[type=simpleovens:{tier}_furnace_minecart,distance=..{length + 50}]', 0.3)
        s.chat(f'fill {x + 1} {y} {tz} {x + length} {y} {tz} minecraft:rail', 0.6)
        s.chat(f'summon simpleovens:{tier}_furnace_minecart {x + 3.5} {y} {tz + 0.5}', 0.5)
    time.sleep(1.0)
    start = {t: _data(s, f'X0-{t}', f'data get entity @e[type=simpleovens:{t}_furnace_minecart,limit=1] Pos[0]') for t, _ in CARTS}
    s.chat('item replace entity @s weapon.mainhand with minecraft:coal 8', 0.4)
    lit_at = {}
    for i, (tier, want) in enumerate(CARTS):
        tz = z - 2 + 2 * i + 0.5
        kind = f'simpleovens:{tier}_furnace_minecart'
        if _aim_entity(s, x + 1.5, y, tz, x + 3.5, y + 0.4, tz, kind, f'{tier} cart'):
            s.io.right_click()
            lit_at[tier] = time.time()
            time.sleep(0.3)
    # speed right away (the 150-block rails end after ~10 s and then all carts stand alike): distance since lighting / time since lighting
    early = {}
    for t, _ in CARTS:
        xv = _data(s, f'XE-{t}', f'data get entity @e[type=simpleovens:{t}_furnace_minecart,limit=1] Pos[0]')
        if xv is not None and start.get(t) is not None and t in lit_at:
            early[t] = round((xv - start[t]) / max(0.5, time.time() - lit_at[t]), 2)
    _look(s, x + 0.5, y + 2, z + 4.5, x + 10, y, z, 0.6)
    s.shot('1-carts-smoke')
    # speed along the rails now (blocks per tick): the tiers' top speeds are 0.625 / 0.75 / 1.0 of a plain cart
    # fuel: the Enderite cart burns 1 per tick like every furnace cart, so allow the seconds of the reads
    fuel = {t: _data(s, f'FUEL-{t}', f'data get entity @e[type=simpleovens:{t}_furnace_minecart,limit=1] Fuel') for t, _ in CARTS}
    for tier, want in CARTS:
        f = fuel.get(tier)
        s.check(f'REAL right click with coal on the {tier} furnace cart: Fuel about {want} (one coal x tier)',
                f is not None and want - 800 <= f <= want, f)
    end = {t: _data(s, f'X1-{t}', f'data get entity @e[type=simpleovens:{t}_furnace_minecart,limit=1] Pos[0]') for t, _ in CARTS}
    moved = {t: (round(end[t] - start[t], 1) if end.get(t) is not None and start.get(t) is not None else None) for t, _ in CARTS}
    s.check('every fuelled cart drove off east (> 2 blocks)', all(m is not None and m > 2 for m in moved.values()), moved)
    s.check('the Enderite cart runs faster than the Reinforced one (blocks per second since lighting)',
            early.get('enderite') is not None and early.get('reinforced') is not None and early['enderite'] > early['reinforced'] * 1.15, early)
    _look(s, x + 0.5, y + 2, z + 4.5, x + 25, y, z, 1.0)
    time.sleep(0.5)
    s.shot('2-carts-under-way')
    for tier, _ in CARTS:
        s.chat(f'kill @e[type=simpleovens:{tier}_furnace_minecart,distance=..{length + 50}]', 0.3)
    _done(s)


SCENARIOS = {
    'n126-money-merchant': money_merchant,
    'n126-crucibles-soullava': crucibles_soullava,
    'n126-crucibles-kitchen': crucibles_kitchen,
    'n126-crucibles-jeijade': crucibles_jeijade,
    'n126-ovens-boats': ovens_boats,
    'n126-ovens-carts': ovens_carts,
}
