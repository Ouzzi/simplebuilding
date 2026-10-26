"""Simple Earth menu with the REAL operating-system mouse (WF25-2 / WG26-3).

Runs the full integration client (`:integration:runIntegrationClient`, every module plus the dev mods) under an X server
(xvfb-run), lets the client-side MenuProbe open the Simple Earth menu on the title screen and then drives the X11 pointer
through the XTest extension (the mechanism xdotool uses; libXtst via ctypes because xdotool is not installed on sb-test):
motion, button presses, drags and wheel clicks go X server -> SDL -> MouseHandler -> screen, exactly like a player's mouse.
Fabric's client game tests cannot do this - they block real input.

Usage (inside xvfb-run, from the repository root):
    python3.12 modules/simpleearth/tools/menu_probe.py --out /var/tmp/sb/menuprobe --scales 2,4
Exit 0 when every check passed; results in <out>/results.json, screenshots copied there.
"""
import argparse
import ctypes
import json
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
RUN = REPO / 'integration/run-fabric-263'
REQUEST, STATUS, COMMAND = 'simpleearth-menu-probe.json', 'simpleearth-probe-status.json', 'simpleearth-probe-command.json'


class Pointer:
    """The X11 pointer through XTest (button 1 left, 4/5 wheel up/down)."""

    def __init__(self):
        self.x11 = ctypes.cdll.LoadLibrary('libX11.so.6')
        self.xtst = ctypes.cdll.LoadLibrary('libXtst.so.6')
        self.x11.XOpenDisplay.restype = ctypes.c_void_p
        self.display = self.x11.XOpenDisplay(None)
        if not self.display:
            raise SystemExit('no X display (run inside xvfb-run)')
        self.d = ctypes.c_void_p(self.display)

    def move(self, x, y):
        self.xtst.XTestFakeMotionEvent(self.d, -1, int(x), int(y), 0)
        self.x11.XFlush(self.d)

    def button(self, b, down):
        self.xtst.XTestFakeButtonEvent(self.d, b, 1 if down else 0, 0)
        self.x11.XFlush(self.d)

    def click(self, x, y):
        self.move(x, y)
        time.sleep(0.25)
        self.button(1, True)
        time.sleep(0.12)
        self.button(1, False)
        time.sleep(0.4)

    def drag(self, x0, y, x1, steps=12):
        self.move(x0, y)
        time.sleep(0.25)
        self.button(1, True)
        time.sleep(0.15)
        # a hand's pace: under Xvfb the full pack draws few frames a second, and the game applies the cursor once a frame
        for k in range(1, steps + 1):
            self.move(x0 + (x1 - x0) * k / steps, y)
            time.sleep(0.25)
        time.sleep(1.0)
        self.button(1, False)
        time.sleep(0.4)

    def wheel_down(self, x, y, clicks=3):
        self.move(x, y)
        time.sleep(0.25)
        for _ in range(clicks):
            self.button(5, True)
            self.button(5, False)
            time.sleep(0.12)
        time.sleep(0.4)


def status():
    try:
        return json.loads((RUN / STATUS).read_text())
    except (OSError, ValueError):
        return None


def command(op, **kw):
    (RUN / COMMAND).write_text(json.dumps(dict(op=op, **kw)))
    for _ in range(100):
        time.sleep(0.05)
        if not (RUN / COMMAND).exists():
            time.sleep(0.4)
            return
    raise SystemExit(f'command {op} not taken')


def centre(st, rect):
    x, y, w, h = st['rects'][rect]
    return st['windowX'] + x + w / 2, st['windowY'] + y + h / 2


def at(st, rect, fraction):
    x, y, w, h = st['rects'][rect]
    return st['windowX'] + x + w * fraction, st['windowY'] + y + h / 2


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', required=True)
    ap.add_argument('--scales', default='2,4')
    ap.add_argument('--label', default='')
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

    try:
        st = None
        deadline = time.time() + 900
        while time.time() < deadline:
            st = status()
            if st and st.get('screen') == 'EarthScreen' and st.get('rects'):
                break
            if game.poll() is not None:
                raise SystemExit('client ended before the menu opened (see client.log)')
            time.sleep(1)
        else:
            raise SystemExit('menu never opened')
        print(f'menu open after {time.time() - started:.0f} s', flush=True)
        ptr = Pointer()
        for scale in scales:
            command('guiScale', value=scale)
            command('version', value=6)
            command('set', key='detail', value=3)
            command('set', key='faceEdge', value=0.28)
            command('set', key='moonPhases', value=1)
            time.sleep(1)
            st = status()
            tag = f'GUI {st["scale"]} ({st["width"]}x{st["height"]})'
            command('shot', name='before')
            # 1 version button (a Vanilla Button)
            ptr.click(*centre(st, 'v10'))
            st = status()
            check(f'{tag}: click V10', st['version'] == 10, f'version {st["version"]}')
            if st['version'] != 10:
                command('version', value=10)
            # 2 slider click at 10 % of the detail slider -> the lowest level
            command('scrollTo', key='detail')
            st = status()
            if 'detail' in st['rects']:
                ptr.click(*at(st, 'detail', 0.08))
                st = status()
                check(f'{tag}: click detail slider near the left end', st['values']['detail'] == 1, f'detail {st["values"]["detail"]}')
            else:
                check(f'{tag}: detail slider visible', False, 'not in view')
            # calibration: where the game sees the pointer (window pixels) after a real move
            cx, cy = centre(st, 'list')
            ptr.move(cx, cy)
            time.sleep(0.8)
            st = status()
            print(f'{tag}: pointer at root {cx:.0f}/{cy:.0f}, game sees {st["mouseX"]:.0f}/{st["mouseY"]:.0f} '
                  f'(window at {st["windowX"]}/{st["windowY"]})', flush=True)
            # 3 drag the tile border slider from left to right
            command('scrollTo', key='faceEdge')
            st = status()
            if 'faceEdge' in st['rects']:
                x0, y = at(st, 'faceEdge', 0.04)
                x1, _ = at(st, 'faceEdge', 0.98)
                ptr.drag(x0, y, x1)
                st = status()
                print(f'{tag}: after drag the game sees the pointer at {st["mouseX"]:.0f}, slider {st["rects"].get("faceEdge")}', flush=True)
                check(f'{tag}: drag tile border slider to the right', st['values']['faceEdge'] >= 0.9, f'faceEdge {st["values"]["faceEdge"]}')
            else:
                check(f'{tag}: tile border slider visible', False, 'not in view')
            # 4 toggle
            command('scrollTo', key='moonPhases')
            st = status()
            before = st['values']['moonPhases']
            if 'moonPhases' in st['rects']:
                ptr.click(*centre(st, 'moonPhases'))
                st = status()
                check(f'{tag}: click moon phases toggle', st['values']['moonPhases'] != before, f'{before} -> {st["values"]["moonPhases"]}')
            else:
                check(f'{tag}: moon phases toggle visible', False, 'not in view')
            # 5 wheel over the list
            command('scrollTo', key='detail')
            st = status()
            s0 = st['scroll']
            ptr.wheel_down(*centre(st, 'list'), clicks=3)
            st = status()
            check(f'{tag}: mouse wheel scrolls the list', st['scroll'] > s0, f'{s0} -> {st["scroll"]}')
            command('shot', name='after')
        command('quit')
    finally:
        if game.poll() is None and not results:
            command_quiet = RUN / COMMAND
            command_quiet.write_text(json.dumps({'op': 'quit'}))
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
            shutil.copy(p, out / f'{args.label}{i:02d}-{p.name}')
        ok = ok and bool(results)
        (out / 'results.json').write_text(json.dumps({'ok': ok, 'results': results}, indent=1))
    sys.exit(0 if ok else 1)


if __name__ == '__main__':
    main()
