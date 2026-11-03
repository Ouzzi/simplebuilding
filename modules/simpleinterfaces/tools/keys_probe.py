"""Key binding page (Simple Keys) with the REAL operating-system mouse and keyboard (IG31-31).

Runs the full integration client (`:integration:runIntegrationClient`) under an X server (xvfb-run). The client-side KeysProbe
opens the page on the title screen; this script then drives the X11 pointer and keyboard through XTest (libXtst via ctypes,
the pointer class of Simple Earth's menu_probe.py): motion, clicks and key presses go X server -> SDL -> game -> screen, like a
player's. Every button of the page is clicked once and its effect read back from the probe's status file.

Usage (inside xvfb-run, from the repository root):
    python3.12 modules/simpleinterfaces/tools/keys_probe.py --out /var/tmp/sb/keysprobe --scales 2,3
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
REQUEST, STATUS, COMMAND = 'simpleinterfaces-keys-probe.json', 'simpleinterfaces-keys-status.json', 'simpleinterfaces-keys-command.json'
ROW = 'key.simplelib.freelook_toggle'  # unbound by default (open_keys has '+' since QOL34)
G = 298  # GLFW code of F9: free in Vanilla, so its key page lists only the probed action

_spec = importlib.util.spec_from_file_location('menu_probe', REPO / 'modules/simpleearth/tools/menu_probe.py')
_menu = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_menu)


class Input(_menu.Pointer):
    """The pointer of menu_probe plus key presses (XTest)."""

    def key(self, name):
        self.x11.XStringToKeysym.restype = ctypes.c_ulong
        sym = self.x11.XStringToKeysym(name.encode())
        code = self.x11.XKeysymToKeycode(self.d, ctypes.c_ulong(sym))
        self.xtst.XTestFakeKeyEvent(self.d, code, 1, 0)
        self.x11.XFlush(self.d)
        time.sleep(0.12)
        self.xtst.XTestFakeKeyEvent(self.d, code, 0, 0)
        self.x11.XFlush(self.d)
        time.sleep(0.5)


def status():
    try:
        return json.loads((RUN / STATUS).read_text())
    except (OSError, ValueError):
        return None


def fresh():
    """A status written after the last action (two ticks later)."""
    st = status()
    t0 = st['tick'] if st else -1
    for _ in range(100):
        time.sleep(0.1)
        st = status()
        if st and st['tick'] >= t0 + 2:
            return st
    return st


def command(op, **kw):
    (RUN / COMMAND).write_text(json.dumps(dict(op=op, **kw)))
    for _ in range(100):
        time.sleep(0.05)
        if not (RUN / COMMAND).exists():
            time.sleep(0.4)
            return fresh()
    raise SystemExit(f'command {op} not taken')


def centre(st, name):
    x, y, w, h = st['rects'][name]
    return st['windowX'] + x + w / 2, st['windowY'] + y + h / 2


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', required=True)
    ap.add_argument('--scales', default='2,3')
    ap.add_argument('--size', default='1280x720', help='window size WxH')
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    scales = [int(s) for s in args.scales.split(',')]
    RUN.mkdir(parents=True, exist_ok=True)
    for f in (STATUS, COMMAND):
        (RUN / f).unlink(missing_ok=True)
    (RUN / REQUEST).write_text(json.dumps({'width': int(args.size.split('x')[0]), 'height': int(args.size.split('x')[1]), 'guiScale': scales[0]}))
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

    try:
        deadline = time.time() + 900
        while time.time() < deadline:
            st = status()
            if st and st.get('screen') == 'KeysScreen' and st.get('rects'):
                break
            if game.poll() is not None:
                raise SystemExit('client ended before the page opened (see client.log)')
            time.sleep(1)
        else:
            raise SystemExit('page never opened')
        print(f'page open after {time.time() - started:.0f} s', flush=True)
        io = Input()
        command('row', value=ROW)
        command('cell', value=G)
        for scale in scales:
            st = command('guiScale', value=scale)
            tag = f'GUI {st["scale"]}'
            command('open')
            command('shot')

            def click(name, st=None):
                st = st or fresh()
                if name not in st['rects']:
                    check(f'{tag}: {name} visible', False, 'not in rects: ' + ', '.join(st['rects']))
                    return st
                x, y = centre(st, name)
                io.move(x, y)
                time.sleep(0.4)
                seen = fresh()
                gx, gy = seen['windowX'] + seen['mouseX'], seen['windowY'] + seen['mouseY']
                if abs(gx - x) > 3 or abs(gy - y) > 3:
                    print(f'  pointer {name}: root {x:.0f}/{y:.0f}, game sees {gx:.0f}/{gy:.0f} (window {seen["windowX"]}/{seen["windowY"]})', flush=True)
                io.click(x, y)
                return fresh()

            # views
            st = click('tab_function')
            check(f'{tag}: tab "by function"', st['values'].get('view') == 'FUNCTION', st['values'].get('view'))
            command('shot')
            st = click('tab_mod')
            check(f'{tag}: tab "by mod"', st['values'].get('view') == 'MOD', st['values'].get('view'))
            # filters
            for f, key in (('only_conflicts', 'onlyConflicts'), ('only_unbound', 'onlyUnbound')):
                st = click(f)
                on = st['values'].get(key)
                st = click(f)
                check(f'{tag}: filter {f} on/off', on == 'true' and st['values'].get(key) == 'false', f'{on} -> {st["values"].get(key)}')
            # assign a key to the probed action: click its key button, press F8 on the real keyboard, then reset
            st = command('show')
            st = click('key_button', st)
            check(f'{tag}: key button listens', st['values'].get('listening') == ROW, st['values'].get('listening'))
            io.key('F8')
            st = fresh()
            check(f'{tag}: real F8 assigned', st['rowKey'] == 'key.keyboard.f8', st['rowKey'])
            st = command('show')
            st = click('reset_button', st)
            check(f'{tag}: reset button', st['rowKey'] == 'key.keyboard.unknown', st['rowKey'])
            # fold the first group and unfold it
            st = command('show')
            rows = int(st['values'].get('rows', '0'))
            command('open')
            st = fresh()
            c0 = int(st['values'].get('collapsed', '0'))  # the debug sub-group starts folded
            st = click('fold_first')
            folded = int(st['values'].get('collapsed', '0'))
            st = click('fold_first')
            check(f'{tag}: fold/unfold first group', folded == c0 + 1 and int(st['values'].get('collapsed', '0')) == c0, f'{c0} -> {folded} -> {st["values"].get("collapsed")} ({rows} rows)')
            # QOL34: "Collapse all" folds every group and sub-group, then reads "Expand all" and opens all again
            st = click('fold_all')
            rows_folded = int(st['values'].get('rows', '0'))
            check(f'{tag}: collapse all', int(st['values'].get('collapsed', '0')) > c0 + 1 and rows_folded < rows, f'rows {rows} -> {rows_folded}, folded {st["values"].get("collapsed")}')
            command('shot')
            st = click('fold_all')
            check(f'{tag}: expand all', st['values'].get('collapsed') == '0' and int(st['values'].get('rows', '0')) > rows, f'rows {st["values"].get("rows")}, folded {st["values"].get("collapsed")}')
            command('shot')
            # reset all asks first; any other click cancels the question
            st = click('reset_all')
            armed = st['values'].get('armedReset')
            st = click('tab_function')
            check(f'{tag}: reset all asks, other click cancels', armed == 'true' and st['values'].get('armedReset') == 'false', f'{armed} -> {st["values"].get("armedReset")}')
            # keyboard view, sizes, layout
            st = click('tab_keyboard')
            check(f'{tag}: tab keyboard', st['values'].get('view') == 'KEYBOARD', st['values'].get('view'))
            st = click('size_60')
            check(f'{tag}: size 60 %', st['values'].get('size').replace(' ', '') == '60%', st['values'].get('size'))
            command('shot')
            st = click('size_100')
            check(f'{tag}: size 100 %', st['values'].get('size').replace(' ', '') == '100%', st['values'].get('size'))
            # b-keys34: the layout button cycles US -> DE -> UK -> FR -> US
            seen = []
            for _ in range(4):
                st = click('layout')
                seen.append(st['values'].get('layout'))
                if len(seen) == 1:
                    command('shot')  # DE with the L-shaped Enter
            check(f'{tag}: layout button cycles', seen == ['de', 'uk', 'fr', 'us'], seen)
            # editor: remove F9 (select it, Del), press the real F9 key (it comes back, selected), move it, save as own layout
            st = click('edit')
            check(f'{tag}: edit opens the editor', st['screen'] == 'KeysEditorScreen', st['screen'])
            n0 = int(st['values'].get('keys', '0'))
            st = click('cell', st)
            check(f'{tag}: click selects the key', st['values'].get('selected') == str(G), st['values'].get('selected'))
            st = click('remove', st)
            check(f'{tag}: remove key', int(st['values'].get('keys', '0')) == n0 - 1, f'{n0} -> {st["values"].get("keys")}')
            io.key('F9')
            st = fresh()
            check(f'{tag}: real key press re-adds the missing key', int(st['values'].get('keys', '0')) == n0 and st['values'].get('selected') == str(G)
                  and st['values'].get('valid') == 'true', f'{st["values"].get("keys")} selected {st["values"].get("selected")} {st["values"].get("message")}')
            command('shot')
            st = click('right', st)
            st = click('right', st)
            check(f'{tag}: arrow button moves the key', st['values'].get('error') == 'false', st['values'].get('message'))
            for _ in range(8):
                st = click('left', st)
            check(f'{tag}: moving into a neighbour is refused', st['values'].get('error') == 'true', st['values'].get('message'))
            command('shot')
            st = click('save', st)
            check(f'{tag}: save returns to the page with the own layout', st['screen'] == 'KeysScreen' and st['values'].get('layout') == 'My US',
                  f'{st["screen"]} {st["values"].get("layout")}')
            st = click('edit', st)
            st = click('delete', st)
            check(f'{tag}: delete layout', st['screen'] == 'KeysScreen' and st['values'].get('layout') == 'us', f'{st["screen"]} {st["values"].get("layout")}')
            # a key on the picture -> its page -> "assign an action here" -> list in pick mode -> click the action
            st = click('cell')
            check(f'{tag}: keyboard key opens its page', st['screen'] == 'KeyCellScreen', st['screen'])
            command('shot')
            st = click('assign', st)
            check(f'{tag}: assign here -> list in pick mode', st['screen'] == 'KeysScreen' and st['values'].get('pickCode') == str(G),
                  f'{st["screen"]} pick {st["values"].get("pickCode")}')
            st = command('show')
            st = click('key_button', st)
            check(f'{tag}: pick assigns F9 and returns to the keyboard', st['rowKey'] == 'key.keyboard.f9' and st['values'].get('view') == 'KEYBOARD',
                  f'{st["rowKey"]} {st["values"].get("view")}')
            st = click('cell')
            st = click('remove_0', st)
            check(f'{tag}: remove on the key page', st['rowKey'] == 'key.keyboard.unknown', st['rowKey'])
            st = click('back', st)
            check(f'{tag}: back to the page', st['screen'] == 'KeysScreen', st['screen'])
            st = click('done')
            check(f'{tag}: done closes', st['screen'] != 'KeysScreen', st['screen'])
            # the button on Vanilla's controls page
            st = command('controls')
            st = click('simple_keys', st)
            check(f'{tag}: Vanilla controls -> Simple Keys button', st['screen'] == 'KeysScreen', st['screen'])
        command('quit')
    finally:
        if game.poll() is None and not results:
            (RUN / COMMAND).write_text(json.dumps({'op': 'quit'}))
        try:
            game.wait(timeout=120)
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
