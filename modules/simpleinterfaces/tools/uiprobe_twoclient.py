"""O8 audit 1: two real clients on ONE dedicated server (driver: tools/testrunner/two_client_probe.py, not part of the normal ui_probe run).

ProbeA (op, creative) changes the world through commands, ProbeB (no op, survival) only looks: every check reads B's CLIENT state from the
probe status (crosshair block/entity, slots of the open menu, inventory, creative flag), so it proves that the server synced it to the
second client. The two sides meet at barriers (files in SB_TWOCLIENT_DIR); each waits at most BARRIER_S seconds.
SCENARIOS
  twoclient-a   ProbeA: places a Simple Storage chest with items, changes a slot while B has it open, summons a mod mob, removes both,
                hands B a stone for the protection check.
  twoclient-b   ProbeB: block sync (crosshair), menu sync (open chest: slot 0, then the live change of slot 1), entity sync (crosshair
                entity), removal sync, permissions (gamemode refused, block placing inside spawn protection refused).
Not covered (only a shot for a human): config sync to the client (no client-readable config value in the probe status).
"""
import os
import time
from pathlib import Path

BARRIER_S = 180
CHEST = 'simplestorage:reinforced_chest'
MOB = 'simpleanimals:crab'
# flat world: grass top at y=-61, players stand at y=-60; spawn protection (server.properties) covers x/z 0..16 around the spawn
CX, CY, CZ = 5, -60, 0
FACE_CHEST = '2.5 -60 0.5 -90 25'      # B two blocks west of the chest, looking at it
FACE_MOB = '5.5 -60 1.5 180 35'        # B south of the mob at 5 -60 -1, looking north/down at it
FACE_GROUND = '2.5 -60 0.5 -90 50'     # B looking at the grass east of it (place target, inside spawn protection)


def _dir():
    d = Path(os.environ.get('SB_TWOCLIENT_DIR', '/var/tmp/sb/twoclient'))
    d.mkdir(parents=True, exist_ok=True)
    return d


def signal(name):
    (_dir() / name).write_text(str(time.time()))


def wait(s, name):
    end = time.time() + BARRIER_S
    while time.time() < end:
        if (_dir() / name).exists():
            return True
        time.sleep(0.5)
    s.check(f'barrier {name} reached', False, f'the other client did not arrive in {BARRIER_S} s')
    return False


def _poll(s, pred, seconds=5.0):
    end = time.time() + seconds
    st = s.st()
    while not pred(st) and time.time() < end:
        time.sleep(0.3)
        st = s.st()
    return st


def _slot(st, cslot):
    return next((x['stack'] for x in st.get('slots', []) if not x['own'] and x['cslot'] == cslot), None)


def side_a(s):
    s.chat('gamemode creative @s', 0.5)
    s.chat('tp @s 0.5 -60 4.5', 1.0)
    if not wait(s, 'b-joined'):
        return
    s.chat('gamemode survival ProbeB', 0.5)
    s.chat('clear ProbeB', 0.3)
    s.chat(f'setblock {CX} {CY} {CZ} {CHEST}', 0.5)
    s.chat(f'item replace block {CX} {CY} {CZ} container.0 with minecraft:diamond 7', 0.5)
    s.chat(f'tp ProbeB {FACE_CHEST}', 1.0)
    signal('placed')
    if not wait(s, 'b-opened'):
        return
    s.chat(f'item replace block {CX} {CY} {CZ} container.1 with minecraft:emerald 3', 0.5)
    signal('changed')
    if not wait(s, 'b-closed'):
        return
    s.chat(f'summon {MOB} {CX} {CY} -1 {{NoAI:1b,PersistenceRequired:1b,Silent:1b,Tags:["o8"]}}', 1.0)
    s.chat(f'tp ProbeB {FACE_MOB}', 1.5)
    signal('mob')
    if not wait(s, 'b-mob'):
        return
    s.chat('kill @e[tag=o8]', 0.5)
    s.chat(f'setblock {CX} {CY} {CZ} minecraft:air', 0.5)
    s.chat(f'tp ProbeB {FACE_CHEST}', 1.5)
    signal('removed')
    if not wait(s, 'b-removed'):
        return
    s.chat('give ProbeB minecraft:stone 1', 0.5)
    s.chat(f'tp ProbeB {FACE_GROUND}', 1.5)
    signal('protect')
    if not wait(s, 'b-done'):
        return
    s.check('side A ran to the end', True)


def side_b(s):
    st = s.st()
    s.check('B is no operator (survival after join)', not st.get('creative', True), f"creative={st.get('creative')}")
    signal('b-joined')
    if not wait(s, 'placed'):
        return
    st = _poll(s, lambda t: t.get('hitBlock') == CHEST)
    s.check('block sync: B sees the chest A placed', st.get('hitBlock') == CHEST, f"hitBlock={st.get('hitBlock')} pos={st.get('pos')}")
    s.io.right_click()
    st = _poll(s, lambda t: t.get('screen', 'none') != 'none' and t.get('slots'), 6)
    s.check('menu: B opens the chest', bool(st.get('slots')), f"screen={st.get('screen')}")
    s.check('menu sync: slot 0 holds the 7 diamonds A put there', _slot(st, 0) == 'minecraft:diamond x7', f'slot0={_slot(st, 0)}')
    s.shot('chest-open')
    signal('b-opened')
    if not wait(s, 'changed'):
        return
    st = _poll(s, lambda t: _slot(t, 1) == 'minecraft:emerald x3')
    s.check('live menu sync: slot 1 changed by A while B looks', _slot(st, 1) == 'minecraft:emerald x3', f'slot1={_slot(st, 1)}')
    s.close()
    signal('b-closed')
    if not wait(s, 'mob'):
        return
    st = _poll(s, lambda t: t.get('hitEntity') == MOB)
    s.check('entity sync: B sees the mod mob A summoned', st.get('hitEntity') == MOB, f"hitEntity={st.get('hitEntity')} hitBlock={st.get('hitBlock')}")
    s.shot('mob')
    signal('b-mob')
    if not wait(s, 'removed'):
        return
    st = _poll(s, lambda t: t.get('hitBlock') != CHEST)
    s.check('removal sync: the chest is gone for B too', st.get('hitBlock') != CHEST, f"hitBlock={st.get('hitBlock')}")
    signal('b-removed')
    if not wait(s, 'protect'):
        return
    s.chat('gamemode creative @s', 1.0)
    st = s.st()
    s.check('permission: B (no op) cannot switch to creative', not st.get('creative', True), f"creative={st.get('creative')}")
    st = _poll(s, lambda t: 'minecraft:stone x1' in t.get('inventory', []), 4)
    before = st.get('hitBlock')
    s.io.right_click()
    time.sleep(1.5)
    st = s.st()
    s.check('permission: spawn protection refuses B placing a block', 'minecraft:stone x1' in st.get('inventory', [])
            and st.get('hitBlock') == before, f"before={before} after={st.get('hitBlock')} inv={[x for x in st.get('inventory', []) if x]}")
    s.open_config('simplepads')
    s.shot('config-simplepads')
    s.close()
    signal('b-done')


SCENARIOS = {'twoclient-a': side_a, 'twoclient-b': side_b}
