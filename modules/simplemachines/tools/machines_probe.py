"""Auto Smither, Autonomous Crafter and tier hopper windows with the REAL operating-system mouse (PLAN40 simplemachines-3).

Runs the full integration client (`:integration:runIntegrationClient`) under an X server (xvfb-run). The client-side MachinesProbe
(modules/simplemachines/shared/java/com/simplemachines/client/MachinesProbe.java) creates a flat survival world and, on command,
builds one machine two blocks east of the player, fills the hotbar and opens the machine's window through the block. This script
drives the X11 pointer through XTest (the Pointer class of Simple Earth's menu_probe.py) and reads menu and server state:

- Auto Smither: dirt is refused by the template slot; template, diamond sword and an iron ingot show the recipe error; the netherite
  ingot clears it; a redstone pulse smiths: a netherite sword in the chest in front, template and sword used, one ingot left.
- Autonomous Crafter (powered while filling): planks into two grid slots -> preview 4 sticks; a click on an empty slot switches it
  off and back on; the filter key -> whitelist; power off -> it crafts until one plank stays in every filled slot (filter principle):
  12 sticks in the hopper below.
- Tier hoppers: the filter key cycles off -> whitelist -> same kind -> off; with the whitelist a filled slot refuses another item and
  an empty slot takes it; the netherite and enderite windows open and their key reacts.

Usage (inside xvfb-run, from the repository root):
    python3.12 modules/simplemachines/tools/machines_probe.py --out /var/tmp/sb/machinesprobe --scales 2,4
Exit 0 when every check passed; results in <out>/results.json, screenshots copied there with their step names.
"""
import argparse
import importlib.util
import json
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
RUN = REPO / 'integration/run-fabric-263'
REQUEST, STATUS, COMMAND = 'simplemachines-machines-probe.json', 'simplemachines-machines-status.json', 'simplemachines-machines-command.json'

_spec = importlib.util.spec_from_file_location('menu_probe', REPO / 'modules/simpleearth/tools/menu_probe.py')
_menu = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_menu)


def status():
    try:
        return json.loads((RUN / STATUS).read_text())
    except (OSError, ValueError):
        return None


def fresh(wait=2):
    st = status()
    t0 = st['tick'] if st else -1
    for _ in range(200):
        time.sleep(0.1)
        st = status()
        if st and st['tick'] >= t0 + wait:
            return st
    return st


def command(op, **kw):
    (RUN / COMMAND).write_text(json.dumps(dict(op=op, **kw)))
    for _ in range(100):
        time.sleep(0.05)
        if not (RUN / COMMAND).exists():
            time.sleep(0.6)
            return fresh()
    raise SystemExit(f'command {op} not taken')


def slot(st, i):
    return next(s for s in st['slots'] if s['i'] == i)


def hotbar(st, k):
    """Menu index of hotbar slot k (player inventory index k)."""
    return next(s['i'] for s in st['slots'] if s['inv'] and s['cs'] == k)


def point(st, i):
    s = slot(st, i)
    return st['windowX'] + s['x'], st['windowY'] + s['y']


def filter_key(st):
    for w in st.get('widgets', []):
        if w['class'] == 'UiFilterButton' and w['visible']:
            return st['windowX'] + w['x'] + w['w'] / 2, st['windowY'] + w['y'] + w['h'] / 2
    return None


def output(st, item):
    return sum(e['count'] for e in st.get('output', []) if e['item'] == item)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', required=True)
    ap.add_argument('--scales', default='2,4')
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    scales = [int(s) for s in args.scales.split(',')]
    RUN.mkdir(parents=True, exist_ok=True)
    for f in (STATUS, COMMAND):
        (RUN / f).unlink(missing_ok=True)
    (RUN / REQUEST).write_text(json.dumps({'width': 1920, 'height': 1080, 'guiScale': scales[0]}))
    started = time.time()
    shots_dir = RUN / 'screenshots'
    seen = {p.name for p in shots_dir.glob('*.png')} if shots_dir.is_dir() else set()
    log = open(out / 'client.log', 'w')
    game = subprocess.Popen(['./gradlew', ':integration:runIntegrationClient'], cwd=REPO, stdout=log, stderr=subprocess.STDOUT,
                            start_new_session=True)
    results, ok = [], True
    shot_no = [0]

    def check(name, good, detail=''):
        nonlocal ok
        ok &= bool(good)
        results.append({'check': name, 'ok': bool(good), 'detail': str(detail)[:400]})
        print(('OK   ' if good else 'FAIL ') + name + ': ' + str(detail)[:300], flush=True)

    def snap(name):
        """Screenshot.grab names by time: the new file is copied under the step name right away."""
        command('shot')
        for _ in range(30):
            time.sleep(0.2)
            new = sorted(p for p in shots_dir.glob('*.png') if p.name not in seen) if shots_dir.is_dir() else []
            if new:
                for p in new:
                    seen.add(p.name)
                shot_no[0] += 1
                shutil.copy(new[-1], out / f'{shot_no[0]:02d}-{name}.png')
                return
        check(f'screenshot {name} written', False, 'no new file in ' + str(shots_dir))

    def open_menu(machine, menu, tier='reinforced'):
        command('open', machine=machine, tier=tier)
        for _ in range(60):
            st = fresh()
            if st and st.get('menu') == menu and len(st.get('slots', [])) > 9:
                return st
        check(f'{machine} window opens', False, st and st.get('screen'))
        return None

    def move(st, src, dst, put_back=True):
        """Picks up the stack of slot src and clicks it down on slot dst; what stays on the cursor goes back to src."""
        ptr.click(*point(st, src))
        st = fresh()
        ptr.click(*point(st, dst))
        st = fresh(4)
        if put_back and st['carried']['item']:
            ptr.click(*point(st, src))
            st = fresh(4)
        return st

    try:
        deadline = time.time() + 1200
        while time.time() < deadline:
            st = status()
            if st and st.get('ready') and st.get('screen') == 'none':
                break
            if game.poll() is not None:
                raise SystemExit('client ended before the world opened (see client.log)')
            time.sleep(1)
        else:
            raise SystemExit('world never opened')
        print(f'world open after {time.time() - started:.0f} s', flush=True)
        ptr = _menu.Pointer()
        time.sleep(2)
        for scale in scales:
            command('guiScale', value=scale)
            tag = f'GUI {scale}'

            # ---------------- Auto Smither
            st = open_menu('smither', 'AutoSmitherMenu')
            if st:
                st = fresh(4)
                check(f'{tag} smither: window open, inputs empty', all(slot(st, i)['item'] == '' for i in range(4)), st.get('menu'))
                snap(f'gui{scale}-smither-empty')
                h = [hotbar(st, k) for k in range(5)]
                ptr.click(*point(st, h[3]))
                st = fresh()
                ptr.click(*point(st, 0))
                st = fresh(4)
                check(f'{tag} smither: dirt refused by the template slot', slot(st, 0)['item'] == '' and st['carried']['item'] == 'minecraft:dirt',
                      (slot(st, 0), st['carried']))
                if st['carried']['item']:
                    ptr.click(*point(st, h[3]))
                    st = fresh(4)
                for src, dst, item in ((h[0], 0, 'minecraft:netherite_upgrade_smithing_template'), (h[1], 1, 'minecraft:diamond_sword'),
                                       (h[4], 2, 'minecraft:iron_ingot')):
                    st = move(st, src, dst)
                    check(f'{tag} smither: {item} clicked into slot {dst}', slot(st, dst)['item'] == item, slot(st, dst))
                st = fresh(4)
                check(f'{tag} smither: wrong addition shows the recipe error', st.get('recipeError') is True, st.get('recipeError'))
                snap(f'gui{scale}-smither-error')
                st = move(st, 2, h[4])
                check(f'{tag} smither: iron ingot taken back', slot(st, 2)['item'] == '' and slot(st, h[4])['item'] == 'minecraft:iron_ingot',
                      (slot(st, 2), slot(st, h[4])))
                st = move(st, h[2], 2)
                st = fresh(4)
                check(f'{tag} smither: netherite ingots in, no recipe error', slot(st, 2)['item'] == 'minecraft:netherite_ingot'
                      and slot(st, 2)['count'] == 2 and st.get('recipeError') is False, (slot(st, 2), st.get('recipeError')))
                snap(f'gui{scale}-smither-filled')
                command('power', on=True)
                time.sleep(1.5)
                st = fresh(4)
                check(f'{tag} smither: a pulse smiths a netherite sword into the chest in front', output(st, 'minecraft:netherite_sword') == 1,
                      st.get('output'))
                check(f'{tag} smither: template and sword used, one ingot left', slot(st, 0)['item'] == '' and slot(st, 1)['item'] == ''
                      and slot(st, 2)['count'] == 1, [slot(st, i) for i in range(4)])
                snap(f'gui{scale}-smither-smithed')
                command('power', on=False)

            # ---------------- Autonomous Crafter
            st = open_menu('crafter', 'AutonomousCrafterMenu')
            if st:
                st = fresh(4)
                check(f'{tag} crafter: window open, powered (does not craft yet)', st.get('powered') is True and st.get('filter') == 'NONE',
                      (st.get('powered'), st.get('filter')))
                snap(f'gui{scale}-crafter-empty')
                h = [hotbar(st, k) for k in range(2)]
                st = move(st, h[0], 1)
                st = move(st, h[1], 4)
                check(f'{tag} crafter: planks in grid slots 1 and 4', slot(st, 1)['item'] == 'minecraft:oak_planks' and slot(st, 4)['count'] == 4,
                      (slot(st, 1), slot(st, 4)))
                res = next((s for s in st['slots'] if not s['inv'] and s['i'] >= 9), None)
                check(f'{tag} crafter: preview shows 4 sticks', res is not None and res['item'] == 'minecraft:stick' and res['count'] == 4, res)
                ptr.click(*point(st, 8))
                st = fresh(4)
                check(f'{tag} crafter: a click on the empty slot 8 switches it off', 8 in st.get('disabled', []), st.get('disabled'))
                snap(f'gui{scale}-crafter-slot-off')
                ptr.click(*point(st, 8))
                st = fresh(4)
                check(f'{tag} crafter: a second click switches it on again', 8 not in st.get('disabled', []), st.get('disabled'))
                key = filter_key(st)
                check(f'{tag} crafter: filter key on screen', key is not None, [w['class'] for w in st.get('widgets', [])])
                if key:
                    ptr.click(*key)
                    st = fresh(4)
                    check(f'{tag} crafter: filter key -> whitelist', st.get('filter') == 'WHITELIST', st.get('filter'))
                    ptr.move(*key)
                    time.sleep(1.2)
                    snap(f'gui{scale}-crafter-filter')
                command('power', on=False)
                time.sleep(2.0)
                st = fresh(4)
                check(f'{tag} crafter: without power it crafts until one plank stays per slot', output(st, 'minecraft:stick') == 12
                      and slot(st, 1)['count'] == 1 and slot(st, 4)['count'] == 1, (st.get('output'), slot(st, 1), slot(st, 4)))
                snap(f'gui{scale}-crafter-crafted')
                if key:
                    for want in ('TYPE', 'NONE'):
                        ptr.click(*key)
                        st = fresh(4)
                        check(f'{tag} crafter: filter key -> {want.lower()}', st.get('filter') == want, st.get('filter'))

            # ---------------- tier hoppers
            for tier in ('reinforced', 'netherite', 'enderite'):
                st = open_menu('hopper', 'TierHopperMenu', tier)
                if not st:
                    continue
                st = fresh(4)
                check(f'{tag} {tier} hopper: window open, filter off', st.get('tier') == tier.upper() and st.get('filter') == 'NONE',
                      (st.get('tier'), st.get('filter')))
                key = filter_key(st)
                check(f'{tag} {tier} hopper: filter key on screen', key is not None, [w['class'] for w in st.get('widgets', [])])
                if not key:
                    continue
                ptr.click(*key)
                st = fresh(4)
                check(f'{tag} {tier} hopper: filter key -> whitelist', st.get('filter') == 'WHITELIST', st.get('filter'))
                if tier == 'reinforced':
                    h = [hotbar(st, k) for k in range(2)]
                    st = move(st, h[0], 0)
                    check(f'{tag} hopper: cobblestone in slot 0', slot(st, 0)['item'] == 'minecraft:cobblestone', slot(st, 0))
                    ptr.click(*point(st, h[1]))
                    st = fresh()
                    ptr.click(*point(st, 0))
                    st = fresh(4)
                    check(f'{tag} hopper: whitelist - slot 0 refuses dirt', slot(st, 0)['item'] == 'minecraft:cobblestone'
                          and st['carried']['item'] == 'minecraft:dirt', (slot(st, 0), st['carried']))
                    ptr.click(*point(st, 1))
                    st = fresh(4)
                    check(f'{tag} hopper: whitelist - the empty slot 1 takes dirt (it becomes its filter item)',
                          slot(st, 1)['item'] == 'minecraft:dirt', slot(st, 1))
                    if st['carried']['item']:
                        ptr.click(*point(st, h[1]))
                        st = fresh(4)
                ptr.move(*key)
                time.sleep(1.2)
                snap(f'gui{scale}-{tier}-hopper-whitelist')
                for want in ('TYPE', 'NONE'):
                    ptr.click(*key)
                    st = fresh(4)
                    check(f'{tag} {tier} hopper: filter key -> {want.lower()}', st.get('filter') == want, st.get('filter'))
            command('close')
        command('quit')
    finally:
        if game.poll() is None and not results:
            (RUN / COMMAND).write_text(json.dumps({'op': 'quit'}))
        try:
            game.wait(timeout=180)
        except subprocess.TimeoutExpired:
            # only our own process group (this gradle and the client it started), never a pattern kill
            os.killpg(game.pid, 15)
        log.close()
        for f in (REQUEST, COMMAND):
            (RUN / f).unlink(missing_ok=True)
        ok = ok and bool(results)
        (out / 'results.json').write_text(json.dumps({'ok': ok, 'results': results}, indent=1))
    sys.exit(0 if ok else 1)


if __name__ == '__main__':
    main()
