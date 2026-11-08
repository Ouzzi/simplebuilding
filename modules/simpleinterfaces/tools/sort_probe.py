"""Sort key of Simple Interfaces (WE25-3, default middle mouse button) with the REAL operating-system mouse.

Runs the full integration client under xvfb-run. The client-side SortProbe creates a flat world and reports the open container
screen's slots; this script presses E, moves the X11 pointer over a main-inventory slot and clicks the MIDDLE button through XTest
(Pointer of Simple Earth's menu_probe.py): X server -> SDL -> MouseHandler -> screen, like a player's mouse.

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

        # ---- (a) creative: the key must do nothing (Vanilla's middle click = pick block stack)
        chat('gamemode creative @s')
        fill([('minecraft:dirt', 3), ('minecraft:apple', 2), ('minecraft:dirt', 4)])
        io.key('e')
        st = fresh()
        check('(a) E opens the inventory screen', 'Inventory' in st['screen'], st['screen'])
        s0 = slot(st, 9)
        before = st['inventory'][:]
        if s0:
            io.middle_click(st['windowX'] + s0['x'], st['windowY'] + s0['y'])
        st = fresh()
        check('(a) creative: middle click does not crash (screen stays)', 'Screen' in st['screen'], st['screen'])
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
        chat(f'tp @s {x}.5 {y} {z}.5 facing {x + 2}.5 {y + 0.5 - 1.62:.2f} {z}.5')
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
