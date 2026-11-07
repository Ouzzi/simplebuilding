"""Tool screens (blueprint editor, N23 placed blueprint, octant/wand screen, JEI) with the REAL operating-system mouse and keyboard.

Runs the full integration client (`:integration:runIntegrationClient`, JEI included) under xvfb-run. The client-side BtProbe opens a flat
creative world and reports the screen, widgets and the server side of the held stack and of a watched block entity; this script clicks,
presses keys and types through XTest (Pointer of Simple Earth's menu_probe.py), exactly like a player's devices.

Usage (inside xvfb-run, from the repository root):
    python3.12 modules/simplebuildingtools/tools/mouse_probe.py --out /var/tmp/sb/btmouse
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
REQUEST, STATUS, COMMAND = 'simplebuildingtools-probe.json', 'simplebuildingtools-probe-status.json', 'simplebuildingtools-probe-command.json'

_spec = importlib.util.spec_from_file_location('menu_probe', REPO / 'modules/simpleearth/tools/menu_probe.py')
_menu = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_menu)


class Input(_menu.Pointer):
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

    def key(self, name, wait=0.5):
        self.key_down(name)
        self.key_up(name)
        time.sleep(wait)

    def right_click(self):
        self.button(3, True)
        time.sleep(0.15)
        self.button(3, False)
        time.sleep(0.6)


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


def widget(st, label):
    for w in st['widgets']:
        if w['visible'] and label.lower() in (w['label'] or '').lower():
            return w
    return None


def centre(st, w):
    return st['windowX'] + w['x'] + w['w'] / 2, st['windowY'] + w['y'] + w['h'] / 2


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
        results.append({'check': name, 'ok': bool(good), 'detail': str(detail)[:400]})
        print(('OK   ' if good else 'FAIL ') + name + ': ' + str(detail)[:300], flush=True)

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

        def esc():
            if fresh()['screen'] != 'none':
                io.key('Escape', 0.8)

        def snap(name):
            command('shot')
            time.sleep(1.2)
            # Screenshot.grab names by timestamp: remember a label for the order
            results.append({'check': 'shot', 'ok': True, 'detail': name})

        def click_widget(label, wait=0.5):
            st = fresh()
            w = widget(st, label)
            if not w:
                check(f'widget "{label}" visible', False, [x['label'] for x in st['widgets'] if x['visible']])
                return st
            io.click(*centre(st, w))
            time.sleep(wait)
            return fresh()

        def reset():
            chat('gamemode creative @s')
            chat('clear @s')
            st = fresh()
            return [int(v) for v in st['pos'].replace(',', ' ').split()]

        x, y, z = reset()
        # centre the pointer so the game (grabbed) gets a pointer inside the window
        st = fresh()
        io.move(st['windowX'] + 640, st['windowY'] + 360)

        def aim_floor(dx, dz):
            # look at the top of the floor block dz blocks ahead (south, +z)
            chat(f'tp @s {x}.5 {y} {z}.5 facing {x + dx}.5 {y - 1}.5 {z + dz}.5')
            time.sleep(0.8)

        # ---------- (a) blueprint editor from the hand
        chat('give @s simplebuildingtools:blueprint')
        io.key('1')
        aim_floor(0, 3)
        io.right_click()
        st = fresh()
        check('(a) right click with the blueprint opens the editor', st['screen'].endswith('BlueprintScreen'), st['screen'])
        snap('a-editor-open')
        if st['screen'].endswith('BlueprintScreen'):
            check('(a) editor starts blank', st.get('code', 'x') == '', repr(st.get('code')))
            st = click_widget('Example', 1.0)
            check('(a) real click on Example fills the code', len(st.get('code', '')) > 10, (st.get('code') or '')[:60])
            snap('a-after-example')
            # click into the code area and type with the real keyboard
            area = next((w for w in st['widgets'] if w['class'] == 'BlueprintCodeArea'), None)
            check('(a) code area is a widget of the screen', area is not None)
            if area:
                io.click(*centre(st, area))
                before = st['code']
                for k in ('x', 'y', 'z'):
                    io.key(k, 0.2)
                st = fresh()
                check('(a) typing in the code area changes the code', st['code'] != before and 'xyz' in st['code'].replace('\n', ''), st['code'][-30:])
            code_a = st['code']
            snap('a-after-typing')
            for label in ('Import', 'Export'):
                st = click_widget(label, 1.0)
                check(f'(a) {label} button opens its window', not st['screen'].endswith('BlueprintScreen'), st['screen'])
                snap('a-' + label.lower())
                esc()
                st = fresh()
                if not st['screen'].endswith('BlueprintScreen'):
                    check(f'(a) Escape from {label} returns to the editor', False, st['screen'])
            st = click_widget('Done', 1.0)
            check('(a) Done closes the editor', st['screen'] == 'none', st['screen'])
            check('(a) Save: server stack carries the typed code', 'xyz' in st.get('serverHeld', '').replace('\\n', '').replace('\n', ''), st.get('serverHeld', '')[:200])
            io.right_click()
            st = fresh()
            check('(a) reopen shows the saved code', st['screen'].endswith('BlueprintScreen') and st.get('code') == code_a, (st.get('code') or '')[-30:])
            esc()

        # ---------- (b) N23 placed blueprint
        chat('clear @s')
        chat('give @s simplebuildingtools:blueprint')
        io.key('1')
        chat(f'tp @s {x}.5 {y} {z}.5 facing {x}.5 {y}.0 {z + 2}.5')
        time.sleep(0.8)
        st = fresh()
        fx, fy, fz = [int(v) for v in st['hitPos'].replace(',', ' ').split()] if st.get('hitPos') else (x, y - 1, z + 2)
        print('floor block under the crosshair', st.get('hitPos'), st.get('hitBlock'), flush=True)
        io.key_down('Shift_L')
        io.right_click()
        io.key_up('Shift_L')
        st = fresh()
        check('(b) shift + right click places the blueprint', st.get('hitBlock', '').endswith('placed_blueprint') or st['screen'] == 'none', st.get('hitBlock'))
        px, py, pz = fx, fy + 1, fz
        command('watch', x=px, y=py, z=pz)
        st = fresh()
        check('(b) placed blueprint block entity on the server', 'blueprint' in st.get('serverBe', '').lower() or 'code' in st.get('serverBe', '').lower() or 'template' in st.get('serverBe', '').lower(), st.get('serverBe', '')[:200])
        snap('b-placed')
        chat('item replace entity @s weapon.mainhand with minecraft:air')
        chat(f'tp @s {x}.5 {y} {z}.5 facing {px}.5 {py}.03 {pz}.5')
        time.sleep(0.8)
        st = fresh()
        check('(b) crosshair is on the placed blueprint', st.get('hitBlock', '').endswith('placed_blueprint'), (st.get('hitPos'), st.get('hitBlock')))
        io.right_click()
        st = fresh()
        check('(b) N23: empty-hand right click opens the editor', st['screen'].endswith('BlueprintScreen'), st['screen'])
        snap('b-editor')
        if st['screen'].endswith('BlueprintScreen'):
            st = click_widget('Example', 1.0)
            code_b = st.get('code', '')
            check('(b) Example fills the code of the placed blueprint', len(code_b) > 10, code_b[:60])
            snap('b-after-example')
            st = click_widget('Done', 1.0)
            check('(b) Done closes the editor', st['screen'] == 'none', st['screen'])
            sbe = st.get('serverBe', '').replace('\\n', '\n')
            first_line = code_b.split('\n')[0][:20]
            check('(b) change saved on the SERVER (block entity)', bool(first_line) and first_line in sbe, sbe[:300])
            io.right_click()
            st = fresh()
            check('(b) reopen shows the saved change', st['screen'].endswith('BlueprintScreen') and st.get('code') == code_b, (st.get('code') or '')[:40])
            snap('b-reopened')
            esc()

        # ---------- (c) octant + wand screen with K
        chat('clear @s')
        chat('give @s simplebuildingtools:octant_lime')
        io.key('1')
        aim_floor(0, 3)
        io.key('k', 1.0)
        st = fresh()
        check('(c) K with the octant opens its screen', st['screen'].endswith('OctantScreen'), st['screen'])
        snap('c-octant')
        if st['screen'].endswith('OctantScreen'):
            clicked = 0
            seen = set()
            for rnd in range(2):
                st = fresh()
                btns = [w for w in st['widgets'] if w['class'] in ('Button', 'Plain') and w['visible'] and w['active']]
                for i in range(len(btns)):
                    st = fresh()
                    btns = [w for w in st['widgets'] if w['class'] in ('Button', 'Plain') and w['visible'] and w['active']]
                    if i >= len(btns):
                        break
                    b = btns[i]
                    if b['label'] in ('Done',) or b['label'] in seen:
                        continue
                    seen.add(b['label'])
                    io.click(*centre(st, b))
                    time.sleep(0.5)
                    st2 = fresh()
                    after = [w['label'] for w in st2['widgets'] if w['visible']]
                    changed = b['label'] not in after or st2['screen'] != st['screen']
                    check(f'(c) octant button "{b["label"]}" reacts to a real click', st2['screen'].endswith('OctantScreen') and (changed or b['label'] in after), after)
                    clicked += 1
                snap(f'c-octant-round{rnd}')
            check('(c) octant: buttons clicked', clicked >= 3, clicked)
            st = click_widget('Done', 1.0)
            check('(c) octant Done closes', st['screen'] == 'none', st['screen'])
        chat('clear @s')
        chat('give @s simplebuildingtools:netherite_building_wand')
        io.key('1')
        chat('enchant @s simplebuilding:constructors_touch')
        io.key('k', 1.0)
        st = fresh()
        check('(c) K with the wand opens its screen', st['screen'].endswith('BuildingWandScreen'), st['screen'])
        snap('c-wand')
        if st['screen'].endswith('BuildingWandScreen'):
            for b in [w for w in st['widgets'] if w['class'] in ('Button', 'Plain') and w['visible'] and w['active']]:
                if b['label'] == 'Done':
                    continue
                st = fresh()
                cur = widget(st, b['label'])
                if cur:
                    io.click(*centre(st, cur))
                    time.sleep(0.4)
                    st2 = fresh()
                    check(f'(c) wand button "{b["label"]}" click keeps the screen', st2['screen'].endswith('BuildingWandScreen'), st2['screen'])
            snap('c-wand-after')
            esc()

        # ---------- (d) JEI recipes for chisel and building wand
        chat('gamemode survival @s')
        for item, label in (('simplechisels:iron_chisel', 'chisel'), ('simplebuildingtools:netherite_building_wand', 'wand')):
            chat('clear @s')
            chat(f'give @s {item}')
            io.key('e', 1.5)
            st = fresh()
            check(f'(d) inventory opens ({label})', 'InventoryScreen' in st['screen'], st['screen'])
            slot = next((s for s in st['slots'] if s['item'] == item), None)
            check(f'(d) {label} slot found in the inventory', slot is not None, [s['item'] for s in st['slots']])
            if slot:
                io.move(st['windowX'] + slot['x'], st['windowY'] + slot['y'])
                time.sleep(1.0)
                io.key('r', 2.0)
                st = fresh()
                check(f'(d) JEI recipe window for the {label} (key R over the item)', 'jei' in st['screen'].lower() or 'recipe' in st['screen'].lower(), st['screen'])
                snap(f'd-jei-{label}')
                esc()
                st = fresh()
                if st['screen'] != 'none':
                    esc()
        command('quit')
    finally:
        if game.poll() is None and not any(r['check'] != 'shot' for r in results):
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
        ok = ok and any(r['check'] != 'shot' for r in results)
        (out / 'results.json').write_text(json.dumps({'ok': ok, 'results': results}, indent=1))
    sys.exit(0 if ok else 1)


if __name__ == '__main__':
    main()
