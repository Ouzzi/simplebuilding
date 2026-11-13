"""Shapecutter window with the REAL operating-system mouse and keyboard (PLAN40 simpleblocks-5).

Runs the full integration client (`:integration:runIntegrationClient`) under an X server (xvfb-run). The client-side SawProbe
(modules/simpleblocks/shared/java/com/simpleblocks/saw/client/SawProbe.java) creates a flat survival world and, on command, puts
16 stone bricks, 16 oak planks and 16 cobbled deepslate into the hotbar and opens a Shapecutter through the block. This script moves
and clicks the X11 pointer and presses Shift and Escape through XTest (the Pointer of Simple Earth's menu_probe.py, keys as in
Simple Octets' octets_probe.py) and reads the window and the server's inventory. Per GUI scale:

- stone bricks clicked into the input: the cut list holds stairs, slab (x2) and wall, and the octet (x8) when Simple Octets'
  stone brick octet is registered; a click on the stairs tile shows one stair in the result slot, a click on the result puts it on
  the cursor (input 16 -> 15), a click on an empty inventory slot puts it down; with Simple Octets the octet tile, taken by click
  (8 octets); the slab tile, taken with Shift + click: the whole rest of the input is cut at once into the inventory;
- cobbled deepslate (16 cuts, more than the 12 tiles): the mouse wheel over the list scrolls down and back up;
- oak planks: the slab tile, taken by click (2 slabs); Escape closes the window and the server gives the planks in the input back
  (15 planks, still 2 slabs: the shown result is no extra item);
- UI03: the same window in Vanilla's stonecutter look and in SimpleLib's box look (screenshots of both).

Usage (inside xvfb-run, from the repository root):
    python3.12 modules/simpleblocks/tools/saw_probe.py --out /var/tmp/sb/sawprobe --scales 2,4
Exit 0 when every check passed; results in <out>/results.json, screenshots copied there with their step names.
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
REQUEST, STATUS, COMMAND = 'simpleblocks-saw-probe.json', 'simpleblocks-saw-status.json', 'simpleblocks-saw-command.json'
INPUT, RESULT = 0, 1

_spec = importlib.util.spec_from_file_location('menu_probe', REPO / 'modules/simpleearth/tools/menu_probe.py')
_menu = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_menu)


class Input(_menu.Pointer):
    """Pointer plus keyboard and the wheel up (the pattern of octets_probe.py)."""

    def _code(self, name):
        self.x11.XStringToKeysym.restype = ctypes.c_ulong
        sym = self.x11.XStringToKeysym(name.encode())
        return self.x11.XKeysymToKeycode(self.d, ctypes.c_ulong(sym))

    def key_down(self, name):
        self.xtst.XTestFakeKeyEvent(self.d, self._code(name), 1, 0)
        self.x11.XFlush(self.d)
        time.sleep(0.15)

    def key_up(self, name):
        self.xtst.XTestFakeKeyEvent(self.d, self._code(name), 0, 0)
        self.x11.XFlush(self.d)
        time.sleep(0.15)

    def key(self, name):
        self.key_down(name)
        time.sleep(0.1)
        self.key_up(name)
        time.sleep(0.4)

    def wheel_up(self, x, y, clicks=3):
        self.move(x, y)
        time.sleep(0.25)
        for _ in range(clicks):
            self.button(4, True)
            self.button(4, False)
            time.sleep(0.12)
        time.sleep(0.4)


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


def inv_slot(st, k):
    """Menu index of player inventory slot k (0-8 hotbar, 9-35 main)."""
    return next(s['i'] for s in st['slots'] if s['inv'] and s['cs'] == k)


def point(st, i):
    s = slot(st, i)
    return st['windowX'] + s['x'], st['windowY'] + s['y']


def cut(st, item, count=None):
    return next((c for c in st.get('cuts', []) if c['item'] == item and (count is None or c['count'] == count)), None)


def tile(st, c):
    return st['windowX'] + c['x'], st['windowY'] + c['y']


def server_items(st):
    return (st.get('server') or {}).get('items', {})


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

    def open_saw():
        command('open')
        st = None
        for _ in range(60):
            st = fresh()
            if st and st.get('menu') == 'ShapecutterMenu' and len(st.get('slots', [])) > 30:
                return fresh(4)
        check('shapecutter window opens', False, st and st.get('screen'))
        return None

    def put(st, src, dst):
        """Picks up the stack of slot src and clicks it down on slot dst."""
        ptr.click(*point(st, src))
        st = fresh()
        ptr.click(*point(st, dst))
        return fresh(4)

    def choose(st, item, count=None):
        """Clicks the tile of a cut, scrolling down with the wheel first when it is not shown."""
        c = cut(st, item, count)
        for _ in range(6):
            if c is None or c['visible']:
                break
            ptr.wheel_down(st['windowX'] + st['listX'], st['windowY'] + st['listY'], clicks=1)
            st = fresh(4)
            c = cut(st, item, count)
        if c is None or not c['visible']:
            return st, None
        ptr.click(*tile(st, c))
        return fresh(4), c

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
        ptr = Input()
        time.sleep(2)
        for scale in scales:
            command('guiScale', value=scale)
            command('style', on=True)
            tag = f'GUI {scale}'
            st = open_saw()
            if not st:
                continue
            check(f'{tag}: window open, input and result empty, no cuts', slot(st, INPUT)['item'] == '' and slot(st, RESULT)['item'] == ''
                  and not st['cuts'], (slot(st, INPUT), slot(st, RESULT), len(st['cuts'])))
            snap(f'gui{scale}-empty')
            h = [inv_slot(st, k) for k in range(3)]

            # ---------------- stone bricks: stairs by click, octet by click, slab by shift-click
            st = put(st, h[0], INPUT)
            check(f'{tag}: 16 stone bricks clicked into the input', slot(st, INPUT)['item'] == 'minecraft:stone_bricks'
                  and slot(st, INPUT)['count'] == 16 and st['carried']['item'] == '', (slot(st, INPUT), st['carried']))
            items = sorted({c['item'] for c in st['cuts']})
            check(f'{tag}: stone brick cuts list stairs, slab x2 and wall', cut(st, 'minecraft:stone_brick_stairs', 1)
                  and cut(st, 'minecraft:stone_brick_slab', 2) and cut(st, 'minecraft:stone_brick_wall', 1), items)
            octet = st.get('octetRegistered')
            if octet:
                check(f'{tag}: Simple Octets loaded - the stone brick octet (x8) is listed', cut(st, 'simpleoctets:stone_bricks_octet', 8), items)
            else:
                print(f'INFO {tag}: Simple Octets not loaded, no octet cut expected', flush=True)
            snap(f'gui{scale}-stone-bricks-cuts')
            st, c = choose(st, 'minecraft:stone_brick_stairs', 1)
            check(f'{tag}: a click on the stairs tile chooses it, one stair in the result slot', c is not None
                  and st['selected'] == c['i'] and slot(st, RESULT)['item'] == 'minecraft:stone_brick_stairs' and slot(st, RESULT)['count'] == 1,
                  (st.get('selected'), c, slot(st, RESULT)))
            ptr.move(*point(st, RESULT))
            time.sleep(1.0)
            snap(f'gui{scale}-stairs-chosen')
            ptr.click(*point(st, RESULT))
            st = fresh(4)
            check(f'{tag}: a click on the result takes one stair, input 16 -> 15', st['carried']['item'] == 'minecraft:stone_brick_stairs'
                  and st['carried']['count'] == 1 and slot(st, INPUT)['count'] == 15, (st['carried'], slot(st, INPUT)))
            free = inv_slot(st, 9)
            ptr.click(*point(st, free))
            st = fresh(4)
            check(f'{tag}: the stair is put down in the inventory', slot(st, free)['item'] == 'minecraft:stone_brick_stairs'
                  and st['carried']['item'] == '', (slot(st, free), st['carried']))
            left = 15
            if octet:
                st, c = choose(st, 'simpleoctets:stone_bricks_octet', 8)
                check(f'{tag}: the octet tile gives 8 octets in the result slot', c is not None and slot(st, RESULT)['item'] == 'simpleoctets:stone_bricks_octet'
                      and slot(st, RESULT)['count'] == 8, (c, slot(st, RESULT)))
                ptr.click(*point(st, RESULT))
                st = fresh(4)
                spot = inv_slot(st, 10)
                ptr.click(*point(st, spot))
                st = fresh(4)
                check(f'{tag}: 8 octets taken by click and put down, input 15 -> 14', slot(st, spot)['item'] == 'simpleoctets:stone_bricks_octet'
                      and slot(st, spot)['count'] == 8 and slot(st, INPUT)['count'] == 14, (slot(st, spot), slot(st, INPUT)))
                left = 14
            st, c = choose(st, 'minecraft:stone_brick_slab', 2)
            check(f'{tag}: the slab tile gives 2 slabs in the result slot', c is not None and slot(st, RESULT)['item'] == 'minecraft:stone_brick_slab'
                  and slot(st, RESULT)['count'] == 2, (c, slot(st, RESULT)))
            ptr.move(*point(st, RESULT))
            time.sleep(0.4)
            ptr.key_down('Shift_L')
            try:
                ptr.click(*point(st, RESULT))
            finally:
                ptr.key_up('Shift_L')
            time.sleep(0.8)
            st = fresh(6)
            inv = server_items(st)
            check(f'{tag}: shift + click cuts the whole rest of the input into the inventory', slot(st, INPUT)['item'] == ''
                  and inv.get('minecraft:stone_brick_slab') == 2 * left and inv.get('minecraft:stone_bricks') is None
                  and inv.get('minecraft:stone_brick_stairs') == 1 and st['carried']['item'] == '',
                  (slot(st, INPUT), {k: v for k, v in inv.items() if 'stone_brick' in k}, st['carried']))
            if octet:
                check(f'{tag}: the server has the 8 octets', inv.get('simpleoctets:stone_bricks_octet') == 8, inv)
            snap(f'gui{scale}-shift-taken')

            # ---------------- cobbled deepslate: the wheel scrolls the long list
            st = put(st, h[2], INPUT)
            n = len(st['cuts'])
            check(f'{tag}: cobbled deepslate lists more cuts than the 12 tiles', n > 12 and st['startIndex'] == 0, (n, st.get('startIndex')))
            snap(f'gui{scale}-deepslate-top')
            ptr.wheel_down(st['windowX'] + st['listX'], st['windowY'] + st['listY'], clicks=3)
            st = fresh(4)
            down = st['startIndex']
            check(f'{tag}: the wheel down over the list shows later cuts', down > 0 and down % 4 == 0
                  and any(c['visible'] for c in st['cuts'][12:]), (down, n))
            snap(f'gui{scale}-deepslate-scrolled')
            ptr.wheel_up(st['windowX'] + st['listX'], st['windowY'] + st['listY'], clicks=3)
            st = fresh(4)
            check(f'{tag}: the wheel up shows the first cuts again', st['startIndex'] == 0, st.get('startIndex'))
            # the deepslate goes back to its hotbar slot by hand
            st = put(st, INPUT, h[2])
            check(f'{tag}: deepslate taken out of the input again', slot(st, INPUT)['item'] == '' and slot(st, h[2])['count'] == 16,
                  (slot(st, INPUT), slot(st, h[2])))

            # ---------------- oak planks: slab by click, Escape gives the input back
            st = put(st, h[1], INPUT)
            st, c = choose(st, 'minecraft:oak_slab', 2)
            check(f'{tag}: planks - the slab tile gives 2 oak slabs', c is not None and slot(st, RESULT)['item'] == 'minecraft:oak_slab'
                  and slot(st, RESULT)['count'] == 2, (c, slot(st, RESULT), sorted({x['item'] for x in st['cuts']})))
            snap(f'gui{scale}-planks-slab')
            ptr.click(*point(st, RESULT))
            st = fresh(4)
            spot = inv_slot(st, 11)
            ptr.click(*point(st, spot))
            st = fresh(4)
            check(f'{tag}: 2 oak slabs taken by click, input 16 -> 15', slot(st, spot)['item'] == 'minecraft:oak_slab'
                  and slot(st, INPUT)['count'] == 15 and st['carried']['item'] == '', (slot(st, spot), slot(st, INPUT)))
            check(f'{tag}: planks - the next 2 slabs are shown again', slot(st, RESULT)['item'] == 'minecraft:oak_slab', slot(st, RESULT))
            ptr.move(st['windowX'] + 10, st['windowY'] + 10)
            ptr.key('Escape')
            st = fresh(10)
            inv = server_items(st)
            check(f'{tag}: Escape closes the window', st['screen'] == 'none' and (st.get('server') or {}).get('menu') == 'InventoryMenu',
                  (st['screen'], (st.get('server') or {}).get('menu')))
            check(f'{tag}: closing gives the 15 planks of the input back, the shown result is no extra item',
                  inv.get('minecraft:oak_planks') == 15 and inv.get('minecraft:oak_slab') == 2, inv)

            # ---------------- UI03: Vanilla's stonecutter look and the box look
            for on, name in ((False, 'ui03-old'), (True, 'ui03-box')):
                command('style', on=on)
                st = open_saw()
                if not st:
                    continue
                check(f'{tag}: look {name} switched', st['style'] is on, st.get('style'))
                st = put(st, inv_slot(st, 0), INPUT)
                st, c = choose(st, 'minecraft:stone_brick_stairs', 1)
                check(f'{tag} {name}: stairs chosen by click', c is not None and slot(st, RESULT)['item'] == 'minecraft:stone_brick_stairs',
                      slot(st, RESULT))
                ptr.move(st['windowX'] + 10, st['windowY'] + 10)
                time.sleep(0.6)
                snap(f'gui{scale}-{name}')
                ptr.key('Escape')
                st = fresh(10)
                check(f'{tag} {name}: closed, the 16 stone bricks are back', st['screen'] == 'none'
                      and server_items(st).get('minecraft:stone_bricks') == 16, server_items(st))
            command('style', on=True)
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
