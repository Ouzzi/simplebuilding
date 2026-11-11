"""Sort key of Simple Interfaces (WE25-3, default middle mouse button) with the REAL operating-system mouse.

Runs the full integration client under xvfb-run. The client-side SortProbe creates a flat world and reports the open container
screen's slots; this script presses E, moves the X11 pointer over a main-inventory slot and clicks the MIDDLE button through XTest
(Pointer of Simple Earth's menu_probe.py): X server -> SDL -> MouseHandler -> screen, like a player's mouse.
Steps: (a) creative - item tab clones, Survival Inventory tab sorts its main rows, its hotbar clones (b-sortprobe27); (b) survival
inventory; (c) hotbar; (d) chest.

Usage (inside xvfb-run, from the repository root):
    python3.12 modules/simpleinterfaces/tools/sort_probe.py --out /var/tmp/sb/sortprobe
Exit 0 when every check passed; results in <out>/results.json, screenshots copied there.
"""
import argparse
import ctypes
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
REQUEST, STATUS, COMMAND = 'simpleinterfaces-sort-probe.json', 'simpleinterfaces-sort-status.json', 'simpleinterfaces-sort-command.json'

_spec = importlib.util.spec_from_file_location('menu_probe', REPO / 'modules/simpleearth/tools/menu_probe.py')
_menu = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_menu)


class Input(_menu.Pointer):
    def key(self, name, wait=0.8):
        self.x11.XStringToKeysym.restype = ctypes.c_ulong
        code = self.x11.XKeysymToKeycode(self.d, ctypes.c_ulong(self.x11.XStringToKeysym(name.encode())))
        for down in (1, 0):
            self.xtst.XTestFakeKeyEvent(self.d, code, down, 0)
            self.x11.XFlush(self.d)
            time.sleep(0.15)
        time.sleep(wait)

    def middle_click(self, x, y):
        self.move(x, y)
        time.sleep(0.5)
        self.button(2, True)
        time.sleep(0.12)
        self.button(2, False)
        time.sleep(0.8)

    def right_click(self):
        self.button(3, True)
        time.sleep(0.15)
        self.button(3, False)
        time.sleep(0.8)


def status():
    try:
        return json.loads((RUN / STATUS).read_text())
    except (OSError, ValueError):
        return None


def fresh():
    st = status()
    t0 = st['tick'] if st else -1
    for _ in range(150):
        time.sleep(0.1)
        st = status()
        if st and st['tick'] >= t0 + 3:
            return st
    return st


def command(op, **kw):
    (RUN / COMMAND).write_text(json.dumps(dict(op=op, **kw)))
    for _ in range(150):
        time.sleep(0.05)
        if not (RUN / COMMAND).exists():
            time.sleep(0.4)
            return fresh()
    raise SystemExit(f'command {op} not taken')


def slot(st, cslot, own=True):
    return next((s for s in st['slots'] if s['own'] == own and s['cslot'] == cslot), None)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', required=True)
    ap.add_argument('--scale', type=int, default=2)
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    RUN.mkdir(parents=True, exist_ok=True)
    for f in (STATUS, COMMAND):
        (RUN / f).unlink(missing_ok=True)
    (RUN / REQUEST).write_text(json.dumps({'width': 1280, 'height': 720, 'guiScale': args.scale}))
    started = time.time()
    log = open(out / 'client.log', 'w')
    game = subprocess.Popen(['./gradlew', ':integration:runIntegrationClient'], cwd=REPO, stdout=log, stderr=subprocess.STDOUT,
                            start_new_session=True)
    results, ok = [], True

    def check(name, good, detail=''):
        nonlocal ok
        ok &= bool(good)
        results.append({'check': name, 'ok': bool(good), 'detail': str(detail)[:600]})
        print(('OK   ' if good else 'FAIL ') + name + ': ' + str(detail)[:400], flush=True)

    try:
        deadline = time.time() + 1800
        while time.time() < deadline:
            st = status()
            if st and st.get('screen') == 'none' and 'pos' in st and st['tick'] > 5:
                break
            if game.poll() is not None:
                raise SystemExit('client ended before the world opened (see client.log)')
            time.sleep(1)
        else:
            raise SystemExit('world never opened')
        print(f'world open after {time.time() - started:.0f} s', flush=True)
        io = Input()
        time.sleep(3)

        def chat(c, wait=0.8):
            command('cmd', value=c)
            time.sleep(wait)
            return fresh()

        st = fresh()
        io.move(st['windowX'] + 640, st['windowY'] + 360)

        def fill(items):
            chat('clear @s')
            for i, (item, n) in enumerate(items):
                chat(f'item replace entity @s inventory.{i} with {item} {n}', 0.3)

        # ---- (a) creative (b-sort26 / b-sortprobe27): the Survival Inventory tab sorts its main rows, item tabs and the hotbar keep
        # Vanilla's middle click (clone a full stack onto the cursor).
        chat('gamemode creative @s')
        scramble_c = [('minecraft:dirt', 20), ('minecraft:apple', 3), ('minecraft:stone', 7), ('minecraft:dirt', 30), ('minecraft:apple', 4)]
        chat('clear @s')
        for i, (item, n) in enumerate(scramble_c):
            chat(f'item replace entity @s inventory.{9 + i * 3} with {item} {n}', 0.3)
        chat('item replace entity @s hotbar.0 with minecraft:torch 5', 0.3)
        io.key('e')
        st = fresh()
        check('(a) creative: E opens the creative screen', 'CreativeModeInventoryScreen' in st['screen'], st['screen'])
        check('(a) creative player', st['creative'] is True, st['creative'])
        # (a1) item tab: middle click on an item clones a full stack (Vanilla), the inventory is untouched
        if st.get('inventoryTab'):
            check('(a1) creative screen opened on an item tab', False, 'inventory tab already open')
        else:
            pre = st['inventory'][:]
            item = next((x for x in st['slots'] if not x['own'] and x['stack']), None)
            check('(a1) item tab shows items', item is not None, len(st['slots']))
            if item:
                io.middle_click(st['windowX'] + item['x'], st['windowY'] + item['y'])
                st = fresh()
                got = st.get('carried', '')
                check('(a1) item tab: real middle click clones a full stack (Vanilla)', got.split(' x')[0] == item['stack'].split(' x')[0] and got.endswith(('x64', 'x16', 'x1')), f'{item["stack"]} -> carried {got}')
                check('(a1) item tab: inventory unchanged', st['inventory'] == pre, st['inventory'][9:36])
            io.key('Escape')
            io.key('e')
            st = fresh()
        # (a2) the real left click on the Survival Inventory tab
        if not st.get('inventoryTab'):
            check('(a2) inventory tab position reported', 'invTabX' in st, st.get('invTabError', st.get('screen')))
            if 'invTabX' in st:
                io.click(st['windowX'] + st['invTabX'], st['windowY'] + st['invTabY'])
                st = fresh()
        check('(a2) real click opens the Survival Inventory tab', st.get('inventoryTab') is True, st.get('inventoryTab'))
        command('expect')
        st = fresh()
        pre = st['inventory'][:]
        command('shot')
        # (a3) middle click on a main-row slot sorts the main rows (server sorts the inventory menu)
        s9 = slot(st, 9)
        check('(a3) main-row slot 9 shown in the inventory tab', s9 is not None, len(st['slots']))
        if s9:
            io.middle_click(st['windowX'] + s9['x'], st['windowY'] + s9['y'])
            time.sleep(0.5)
            st = fresh()
            inv = st['inventory']
            command('shot')
            check('(a3) creative inventory tab: real middle click sorts the main rows as ContainerSort.sorted predicts', inv[9:36] == st['expected'], f'{inv[9:36]} vs {st["expected"]}')
            check('(a3) dirt merged to 50, apple 7', 'minecraft:dirt x50' in inv[9:36] and 'minecraft:apple x7' in inv[9:36], inv[9:36])
            check('(a3) hotbar untouched', inv[0:9] == pre[0:9], inv[0:9])
            check('(a3) nothing on the cursor (sort, not clone)', not st.get('carried'), st.get('carried'))
        # (a4) middle click on the hotbar in the inventory tab keeps Vanilla's clone
        again = st['inventory'][:]
        s0 = slot(st, 0)
        if s0:
            io.middle_click(st['windowX'] + s0['x'], st['windowY'] + s0['y'])
            st = fresh()
            check('(a4) inventory tab hotbar: real middle click clones (torch x64 on the cursor)', st.get('carried') == 'minecraft:torch x64', st.get('carried'))
            check('(a4) inventory tab hotbar: inventory unchanged', st['inventory'] == again, st['inventory'][:9])
        else:
            check('(a4) hotbar slot 0 shown in the inventory tab', False, len(st['slots']))
        io.key('Escape')
        # ---- (b) survival inventory
        chat('gamemode survival @s')
        scramble = [('minecraft:dirt', 20), ('minecraft:apple', 3), ('minecraft:stone', 7), ('minecraft:dirt', 30), ('minecraft:oak_planks', 5),
                    ('minecraft:apple', 4), ('minecraft:cobblestone', 9), ('minecraft:stone', 8), ('minecraft:iron_ingot', 2)]
        chat('clear @s')
        for i, (item, n) in enumerate(scramble):
            chat(f'item replace entity @s inventory.{9 + i * 2} with {item} {n}', 0.3)
        chat('item replace entity @s hotbar.0 with minecraft:torch 5', 0.3)
        command('expect')
        io.key('e')
        st = fresh()
        check('(b) survival: E opens the inventory screen', 'InventoryScreen' in st['screen'], st['screen'])
        check('(b) not creative', st['creative'] is False, st['creative'])
        pre = st['inventory'][:]
        s = slot(st, 9)
        command('shot')
        io.middle_click(st['windowX'] + s['x'], st['windowY'] + s['y'])
        command('shot')
        st = fresh()
        inv = st['inventory']
        check('(b) real middle click sorts the main inventory as ContainerSort.sorted predicts', inv[9:36] == st['expected'], f'{inv[9:36]} vs {st["expected"]}')
        check('(b) equal stacks merged (dirt 50 -> 2 stacks, apple 7 in one)', 'minecraft:apple x7' in inv[9:36], inv[9:36])
        check('(b) empties last', all(not x for x in inv[9 + sum(1 for x in st['expected'] if x):36]), inv[9:36])
        check('(b) hotbar untouched', inv[0:9] == pre[0:9], inv[0:9])
        # ---- (c) middle click over the hotbar does not sort
        again = inv[:]
        sh = slot(st, 0)
        io.middle_click(st['windowX'] + sh['x'], st['windowY'] + sh['y'])
        st = fresh()
        check('(c) middle click on the hotbar changes nothing', st['inventory'] == again, st['inventory'][:9])
        io.key('Escape')
        # ---- (d) chest: aim at it, open with the real right click, middle click its slots
        x, y, z = [int(v) for v in st['pos'].replace(',', ' ').split()]
        chat(f'setblock {x + 2} {y} {z} minecraft:chest')
        for i, (item, n) in enumerate([('minecraft:dirt', 10), ('minecraft:apple', 5), ('minecraft:dirt', 20), ('minecraft:stone', 3)]):
            chat(f'item replace block {x + 2} {y} {z} container.{i * 5 + 1} with {item} {n}', 0.3)
        chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {x + 2.5} {y + 0.5 - 1.62:.2f} {z + 0.5}')
        time.sleep(0.8)
        st = fresh()
        check('(d) crosshair on the chest', st.get('hitBlock', '').endswith('chest'), st.get('hitBlock'))
        io.right_click()
        st = fresh()
        check('(d) real right click opens the chest', 'ContainerScreen' in st['screen'], st['screen'])
        chest = [s for s in st['slots'] if not s['own']]
        before = [s['stack'] for s in chest]
        check('(d) chest slots reported', len(chest) == 27, len(chest))
        if len(chest) == 27:
            io.middle_click(st['windowX'] + chest[0]['x'], st['windowY'] + chest[0]['y'])
            st = fresh()
            after = [s['stack'] for s in st['slots'] if not s['own']]
            check('(d) chest sorted: dirt 30 merged, apple, stone, empties last', after[:3] == ['minecraft:stone x3', 'minecraft:dirt x30', 'minecraft:apple x5'] or
                  (sorted(x for x in after if x) == sorted(x for x in ['minecraft:dirt x30', 'minecraft:apple x5', 'minecraft:stone x3']) and not any(after[3:])), after)
            command('shot')
        io.key('Escape')
        command('quit')
    finally:
        if game.poll() is None and not results:
            (RUN / COMMAND).write_text(json.dumps({'op': 'quit'}))
        try:
            game.wait(timeout=120)
        except subprocess.TimeoutExpired:
            os.killpg(game.pid, 15)
        log.close()
        for f in (REQUEST, COMMAND):
            (RUN / f).unlink(missing_ok=True)
        shots = sorted(p for p in (RUN / 'screenshots').glob('*.png') if p.stat().st_mtime >= started)
        for i, p in enumerate(shots):
            shutil.copy(p, out / f'{i:02d}-{p.name}')
        ok = ok and bool(results)
        (out / 'results.json').write_text(json.dumps({'ok': ok, 'results': results}, indent=1))
    sys.exit(0 if ok else 1)


if __name__ == '__main__':
    main()
