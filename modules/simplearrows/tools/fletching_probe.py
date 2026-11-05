"""Fletching Table potion slot (WE25-9, b-arrows02) with the REAL operating-system mouse.

Runs the full integration client (`:integration:runIntegrationClient`) under an X server (xvfb-run). The client-side FletchingProbe
(modules/simplearrows/.../client/FletchingProbe.java) creates a flat survival world, puts flint, stick, feather and a potion of
swiftness into the hotbar and opens a Vanilla fletching table through the server (Simple Arrows' table menu, from SimpleBuilding since 04.11.). This script drives
the X11 pointer through XTest (the Pointer class of Simple Earth's menu_probe.py): pick up each part and put it into its slot, the
potion into the potion slot, read the result (4 arrows with the potion at 1/8 duration), take the result and put it into the
inventory, check the potion was used. Screenshots: empty, filled, tooltip of the result, taken.

Usage (inside xvfb-run, from the repository root):
    python3.12 modules/simplearrows/tools/fletching_probe.py --out /var/tmp/sb/fletchprobe --scales 2,4
Exit 0 when every check passed; results in <out>/results.json, screenshots copied there.
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
REQUEST, STATUS, COMMAND = 'simplearrows-fletching-probe.json', 'simplearrows-fletching-status.json', 'simplearrows-fletching-command.json'
TIP, SHAFT, FLETCHING, RESULT, POTION = 0, 1, 2, 3, 40
HOTBAR0 = 31   # menu index of hotbar slot 0

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


def point(st, i):
    s = slot(st, i)
    return st['windowX'] + s['x'], st['windowY'] + s['y']


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
    log = open(out / 'client.log', 'w')
    game = subprocess.Popen(['./gradlew', ':integration:runIntegrationClient'], cwd=REPO, stdout=log, stderr=subprocess.STDOUT,
                            start_new_session=True)
    results, ok = [], True

    def check(name, good, detail):
        nonlocal ok
        ok &= bool(good)
        results.append({'check': name, 'ok': bool(good), 'detail': detail})
        print(('OK   ' if good else 'FAIL ') + name + ': ' + str(detail), flush=True)

    def menu_open(st):
        return st and st.get('menu') == 'FletchingMenu' and len(st.get('slots', [])) > POTION

    try:
        deadline = time.time() + 1200
        while time.time() < deadline:
            st = status()
            if menu_open(st):
                break
            if game.poll() is not None:
                raise SystemExit('client ended before the table opened (see client.log)')
            time.sleep(1)
        else:
            raise SystemExit('fletching table never opened')
        print(f'table open after {time.time() - started:.0f} s', flush=True)
        ptr = _menu.Pointer()
        time.sleep(2)
        for n, scale in enumerate(scales):
            if n:
                command('reset')
                for _ in range(60):
                    st = fresh()
                    if menu_open(st) and slot(st, HOTBAR0 + 3)['potion']:
                        break
            st = command('guiScale', value=scale)
            time.sleep(1)
            st = fresh(4)
            tag = f'GUI {st["scale"]}'
            check(f'{tag}: the table menu has the potion slot', menu_open(st) and slot(st, POTION)['item'] == '', st.get('menu'))
            command('shot', name='empty')
            # the three parts and the potion, each picked up from the hotbar and put down into its slot
            for src, dst, item in ((HOTBAR0, TIP, 'minecraft:flint'), (HOTBAR0 + 1, SHAFT, 'minecraft:stick'),
                                   (HOTBAR0 + 2, FLETCHING, 'minecraft:feather'), (HOTBAR0 + 3, POTION, 'minecraft:potion')):
                ptr.click(*point(st, src))
                st = fresh()
                ptr.click(*point(st, dst))
                st = fresh(4)
                # parts: put the rest back (one click on the source slot puts the carried stack down)
                if st['carried']['item']:
                    ptr.click(*point(st, src))
                    st = fresh(4)
                check(f'{tag}: {item} clicked into slot {dst}', slot(st, dst)['item'] == item, slot(st, dst))
            res = slot(st, RESULT)
            check(f'{tag}: result = 4 crafted arrows with the potion at 1/8',
                  res['item'] == 'simplearrows:crafted_arrow' and res['count'] == 4 and res['potion'] and abs(res['durationScale'] - 0.125) < 1e-6, res)
            ptr.move(*point(st, RESULT))
            time.sleep(1.2)
            command('shot', name='tooltip')
            # take the result and put it into an empty inventory slot (menu index 4 = first main inventory slot)
            ptr.click(*point(st, RESULT))
            st = fresh(4)
            check(f'{tag}: the result is on the cursor', st['carried']['item'] == 'simplearrows:crafted_arrow' and st['carried']['potion'], st['carried'])
            ptr.click(*point(st, 4))
            st = fresh(4)
            check(f'{tag}: arrows with the potion in the inventory', slot(st, 4)['item'] == 'simplearrows:crafted_arrow'
                  and slot(st, 4)['count'] == 4 and slot(st, 4)['potion'], slot(st, 4))
            check(f'{tag}: the potion was used up', slot(st, POTION)['item'] == '', slot(st, POTION))
            check(f'{tag}: without a potion the result is plain arrows', slot(st, RESULT)['item'] == 'simplearrows:crafted_arrow'
                  and not slot(st, RESULT)['potion'], slot(st, RESULT))
            command('shot', name='taken')
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
        shots = sorted(p for p in (RUN / 'screenshots').glob('*.png') if p.stat().st_mtime >= started)
        for i, p in enumerate(shots):
            shutil.copy(p, out / f'{i:02d}-{p.name}')
        ok = ok and bool(results)
        (out / 'results.json').write_text(json.dumps({'ok': ok, 'results': results}, indent=1))
    sys.exit(0 if ok else 1)


if __name__ == '__main__':
    main()
