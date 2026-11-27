"""Generic real-mouse UI probe (a-w4c, PLAN40 'Echt-Maus-Probe' packages): many screens, ONE client start.

Boots the full integration client (every module) under xvfb-run once, lets the client-side SortProbe of Simple Interfaces build a flat
creative world (inert unless the request file exists) and then drives the X11 pointer and keyboard through XTest (pattern of
Simple Earth's menu_probe.py / Simple Interfaces' sort_probe.py): X server -> SDL -> MouseHandler -> screen, like a player's mouse.
SortProbe reports the open screen, every container slot AND every real widget of the screen (label, class, centre and size in window
pixels), so a scenario clicks a Vanilla widget by its label and needs no layout maths.

Scenarios live next to their module: modules/<module>/tools/uiprobe_<module>.py defines

    SCENARIOS = {'<name>': function(session)}        # name = the module id (or module-id/topic)

and use the Session API below. Usage (inside xvfb-run, from the repository root):

    python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe [--only simplerespawn,simplequestionary] [--scale 2]

The X screen must hold the whole 1280 x 720 window (xvfb-run -s '-screen 0 2560x1440x24'); on a smaller display (xvfb-run's default
1280 x 1024) the probe starts its own 2560 x 1440 Xvfb (N60P) and stops if the window still does not fit.

Exit 0 when every check of every scenario passed; <out>/results.json lists all checks, <out>/shots holds the screenshots named
<scenario>-<name>.png. A scenario that raises counts as one failed check, the next scenario still runs (same world).
"""
import argparse
import importlib.util
import json
import os
import shutil
import signal
import subprocess
import sys
import time
import traceback
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
_spec = importlib.util.spec_from_file_location('sort_probe', REPO / 'modules/simpleinterfaces/tools/sort_probe.py')
sp = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(sp)
RUN = sp.RUN


class Session:
    """What a scenario sees: chat, status, real clicks/keys, checks and named screenshots."""

    def __init__(self, io, out, check_sink):
        self.io = io
        self.out = out
        self._sink = check_sink
        self.scenario = '?'
        self.last = {}

    # ---- status
    def st(self):
        self.last = sp.fresh() or self.last
        return self.last

    def widgets(self, st=None):
        st = st or self.last
        return [w for w in st.get('widgets', []) if w.get('visible', True)]

    def labels(self, st=None):
        return [f"{w['class']}:{w['label']}" for w in self.widgets(st)]

    def screen(self):
        return self.st().get('screen', 'none')

    def wait_screen(self, fragment, seconds=15):
        """Waits until the open screen's class name contains fragment ('none' = no screen)."""
        end = time.time() + seconds
        while time.time() < end:
            st = self.st()
            if fragment in st.get('screen', ''):
                return st
            time.sleep(0.3)
        return self.last

    # ---- commands
    def chat(self, command, wait=0.8):
        """A slash command as the player ('/' is added by the client)."""
        sp.command('cmd', value=command)
        time.sleep(wait)
        return self.st()

    def clear_and_give(self, *stacks):
        """clear + one /give per 'item[components] count' string."""
        self.chat('clear @s', 0.3)
        for s in stacks:
            self.chat(f'give @s {s}', 0.3)

    def stage(self, block, dx=2, dy=0, dz=0, face=True, state=''):
        """Puts a block dx/dy/dz from the player's feet and (face) turns the crosshair onto its centre. Returns (x, y, z)."""
        st = self.st()
        x, y, z = [int(v) for v in st['pos'].replace(',', ' ').split()]
        bx, by, bz = x + dx, y + dy, z + dz
        self.chat(f'setblock {bx} {by} {bz} {block}{state}', 0.5)
        if face:
            self.face(bx, by, bz)
        return bx, by, bz

    def face(self, bx, by, bz):
        st = self.st()
        x, y, z = [int(v) for v in st['pos'].replace(',', ' ').split()]
        self.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {bx + 0.5} {by + 0.5 - 1.62:.2f} {bz + 0.5}', 0.8)
        time.sleep(0.5)
        return self.st()

    def shot(self, name):
        """Screenshot of the framebuffer, copied to <out>/shots/<scenario>-<name>.png."""
        shots = RUN / 'screenshots'
        before = {p.name for p in shots.glob('*.png')} if shots.is_dir() else set()
        sp.command('shot')
        for _ in range(60):
            time.sleep(0.25)
            new = [p for p in shots.glob('*.png') if p.name not in before] if shots.is_dir() else []
            if new:
                dest = self.out / 'shots'
                dest.mkdir(parents=True, exist_ok=True)
                time.sleep(0.4)
                shutil.copy(sorted(new)[-1], dest / f"{self.scenario.replace('/', '-')}-{name}.png")
                return
        self.check(f'shot {name} written', False, 'no new screenshot')

    # ---- real input
    def abs(self, st, x, y):
        return st['windowX'] + x, st['windowY'] + y

    def click_xy(self, x, y, st=None):
        st = st or self.last
        self.io.click(*self.abs(st, x, y))
        return self.st()

    def right_click_xy(self, x, y, st=None):
        st = st or self.last
        self.io.move(*self.abs(st, x, y))
        time.sleep(0.4)
        self.io.right_click()
        return self.st()

    def find_widget(self, label=None, cls=None, st=None, contains=True):
        for w in self.widgets(st):
            if label is not None and not ((label in w['label']) if contains else (label == w['label'])):
                continue
            if cls is not None and cls != w['class']:
                continue
            return w
        return None

    def click_widget(self, label=None, cls=None, name=None, shift=False):
        """Real left click on the first visible widget with this label (substring) / class. Returns the new status or None if absent."""
        st = self.st()
        w = self.find_widget(label, cls, st)
        what = name or f'widget {cls or ""}:{label}'
        if not w:
            self.check(f'{what} present', False, 'widgets: ' + '; '.join(self.labels(st))[:500])
            return None
        if shift:
            self.io.key_down('Shift_L')
        try:
            st = self.click_xy(w['x'], w['y'], st)
        finally:
            if shift:
                self.io.key_up('Shift_L')
        return st

    def slot_xy(self, st, cslot=None, own=True, index=None):
        for s in st.get('slots', []):
            if index is not None and s['index'] == index:
                return s
            if index is None and s['own'] == own and s['cslot'] == cslot:
                return s
        return None

    def click_slot(self, cslot=None, own=True, index=None, shift=False, right=False):
        st = self.st()
        s = self.slot_xy(st, cslot, own, index)
        if not s:
            self.check(f'slot {index if index is not None else (own, cslot)} present', False, f'{len(st.get("slots", []))} slots')
            return None
        if shift:
            self.io.key_down('Shift_L')
        try:
            if right:
                st = self.right_click_xy(s['x'], s['y'], st)
            else:
                st = self.click_xy(s['x'], s['y'], st)
        finally:
            if shift:
                self.io.key_up('Shift_L')
        return st

    def wheel(self, x, y, clicks=2, down=True, st=None):
        st = st or self.last
        px, py = self.abs(st, x, y)
        self.io.move(px, py)
        time.sleep(0.3)
        for _ in range(clicks):
            self.io.button(5 if down else 4, True)
            self.io.button(5 if down else 4, False)
            time.sleep(0.15)
        time.sleep(0.5)
        return self.st()

    def key(self, name, wait=0.8):
        self.io.key(name, wait)
        return self.st()

    def type_text(self, text):
        for ch in text:
            self.io.key({' ': 'space', '-': 'minus', '_': 'underscore', '.': 'period', ',': 'comma', '/': 'slash'}.get(ch, ch), 0.15)
        return self.st()

    def open_config(self, mod_id, wait=2.0):
        """Opens SimpleLib's generic config screen of a mod through the probe (no Mod Menu route needed)."""
        sp.command('config', value=mod_id)
        time.sleep(wait)
        return self.st()

    def close(self):
        """Escape until no screen is open (max 3)."""
        for _ in range(3):
            if self.st().get('screen', 'none') == 'none':
                break
            self.io.key('Escape', 0.6)
        return self.st()

    # ---- checks
    def check(self, name, good, detail=''):
        self._sink(self.scenario, name, bool(good), str(detail))


def load_scenarios(only):
    found = {}
    for path in sorted((REPO / 'modules').glob('*/tools/uiprobe_*.py')):
        spec = importlib.util.spec_from_file_location(path.stem, path)
        mod = importlib.util.module_from_spec(spec)
        try:
            spec.loader.exec_module(mod)
        except Exception as e:  # a broken scenario file must not stop the others
            print(f'cannot load {path}: {e}', flush=True)
            continue
        for name, fn in getattr(mod, 'SCENARIOS', {}).items():
            found[name] = fn
    if only:
        wanted = [n for n in only if n]
        missing = [n for n in wanted if n not in found]
        if missing:
            print('unknown scenarios (ignored): ' + ', '.join(missing), flush=True)
        found = {n: found[n] for n in wanted if n in found}
    else:
        # O8 audit 1: the two-client sides wait for each other and only run under tools/testrunner/two_client_probe.py
        found = {n: fn for n, fn in found.items() if not n.startswith('twoclient-')}
    return found


def _screen_size():
    """(width, height) of the X screen in DISPLAY, or None without a display."""
    import ctypes
    if not os.environ.get('DISPLAY'):
        return None
    x11 = ctypes.cdll.LoadLibrary('libX11.so.6')
    x11.XOpenDisplay.restype = ctypes.c_void_p
    d = x11.XOpenDisplay(None)
    if not d:
        return None
    d = ctypes.c_void_p(d)
    try:
        return x11.XDisplayWidth(d, 0), x11.XDisplayHeight(d, 0)
    finally:
        x11.XCloseDisplay(d)


NEED_SCREEN = (2560, 1440)


def _own_xvfb():
    """N60P: the client window opens at 854 x 480 centred and SortProbe then grows it to 1280 x 720 from that corner, so on xvfb-run's
    default 1280 x 1024 screen its right part (window x > ~1066) lies OUTSIDE the screen - XTest clamps the pointer to the screen edge and
    every click there hit something else (run 1: Mod Menu "Configure...", the editor's top-right buttons, the questionary's right arrow).
    When the display is smaller than NEED_SCREEN, start our own Xvfb with a big screen and use it. Returns the process or None."""
    size = _screen_size()
    if size and size[0] >= NEED_SCREEN[0] and size[1] >= NEED_SCREEN[1]:
        return None
    for n in range(90, 140):
        if Path(f'/tmp/.X{n}-lock').exists() or Path(f'/tmp/.X11-unix/X{n}').exists():
            continue
        proc = subprocess.Popen(['Xvfb', f':{n}', '-screen', '0', f'{NEED_SCREEN[0]}x{NEED_SCREEN[1]}x24', '-nolisten', 'tcp'],
                                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, start_new_session=True)
        os.environ['DISPLAY'] = f':{n}'
        for _ in range(50):
            time.sleep(0.2)
            if proc.poll() is not None:
                break
            if _screen_size():
                print(f'display {size} too small for the 1280 x 720 window: own Xvfb :{n} {NEED_SCREEN[0]}x{NEED_SCREEN[1]}', flush=True)
                return proc
        proc.kill()
    raise SystemExit(f'display {size} too small and no own Xvfb could start (run under xvfb-run -s "-screen 0 2560x1440x24")')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', required=True)
    ap.add_argument('--scale', type=int, default=2)
    ap.add_argument('--only', default='')
    ap.add_argument('--list', action='store_true')
    # O8 audit 1 (tools/testrunner/two_client_probe.py): join a dedicated server as this player; the game directory is SB_PROBE_RUN
    ap.add_argument('--server', default='', help='host:port to join instead of creating a flat world')
    ap.add_argument('--username', default='', help='offline player name (needs --server)')
    ap.add_argument('--bare', action='store_true', help='no per-scenario reset (gamemode/clear/tp): scenarios own the player state')
    args = ap.parse_args()
    scenarios = load_scenarios([x.strip() for x in args.only.split(',')] if args.only else [])
    if args.list:
        print('\n'.join(scenarios))
        return
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    xvfb = _own_xvfb()
    RUN.mkdir(parents=True, exist_ok=True)
    for f in (sp.STATUS, sp.COMMAND):
        (RUN / f).unlink(missing_ok=True)
    request = {'width': 1280, 'height': 720, 'guiScale': args.scale}
    if args.server:
        request['server'] = args.server
    (RUN / sp.REQUEST).write_text(json.dumps(request))
    started = time.time()
    log = open(out / 'client.log', 'w')
    # one client per heavy lock (A 15.11.): no Gradle daemon outliving the run, and SIGTERM/SIGHUP from sbt end the client too
    gradle = ['./gradlew', '--no-daemon', ':integration:runIntegrationClient']
    if args.username:
        gradle.append(f'--args=--username {args.username} --gameDir {RUN}')
    game = subprocess.Popen(gradle, cwd=REPO, stdout=log,
                            stderr=subprocess.STDOUT, start_new_session=True)

    def stop(signum, frame):
        raise SystemExit(f'signal {signum}')
    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGHUP, stop)
    results = []

    def sink(scenario, name, good, detail):
        results.append({'scenario': scenario, 'check': name, 'ok': good, 'detail': detail[:700]})
        print(('OK   ' if good else 'FAIL ') + f'[{scenario}] {name}: ' + detail[:300], flush=True)

    try:
        deadline = time.time() + 1800
        while time.time() < deadline:
            st = sp.status()
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
        s = Session(io, out, sink)
        st = s.st()
        size = _screen_size()
        if size and (st['windowX'] < 0 or st['windowY'] < 0 or st['windowX'] + 1280 > size[0] or st['windowY'] + 720 > size[1]):
            raise SystemExit(f"client window at {st['windowX']},{st['windowY']} (1280 x 720) does not fit the {size[0]} x {size[1]} screen: "
                             'clicks near its edge would land elsewhere')
        print(f"window at {st['windowX']},{st['windowY']} on a {size} screen", flush=True)
        io.move(st['windowX'] + 640, st['windowY'] + 360)
        for idx, (name, fn) in enumerate(scenarios.items()):
            s.scenario = name
            print(f'=== {name}', flush=True)
            t0 = time.time()
            try:
                # a dead player (a scenario that ended inside a wall) must not spoil every later scenario
                if 'DeathScreen' in s.st().get('screen', ''):
                    w = s.find_widget('Respawn')
                    if w:
                        s.click_xy(w['x'], w['y'])
                    s.wait_screen('none', 10)
                if args.bare:
                    fn(s)
                    continue
                s.chat('gamemode creative @s', 0.5)
                s.close()
                s.chat('clear @s', 0.3)
                s.chat('effect clear @s', 0.3)
                s.chat('weather clear', 0.3)
                # every scenario starts in its own fresh stretch of the flat world, high enough to land on the grass
                s.chat(f'tp @s {20000 + 4000 * idx} -58 20000', 3.0)
                time.sleep(1.5)
                fn(s)
            except SystemExit:
                raise
            except Exception:
                sink(name, 'scenario ran without exception', False, traceback.format_exc()[-600:])
            try:
                s.close()
            except Exception:
                pass
            print(f'=== {name} took {time.time() - t0:.0f} s', flush=True)
        sp.command('quit')
    finally:
        if game.poll() is None and not results:
            sp._write_command({'op': 'quit'})
        try:
            game.wait(timeout=120)
        except subprocess.TimeoutExpired:
            os.killpg(game.pid, signal.SIGTERM)
            try:
                game.wait(timeout=30)
            except subprocess.TimeoutExpired:
                os.killpg(game.pid, signal.SIGKILL)
                game.wait(timeout=30)
        log.close()
        for f in (sp.REQUEST, sp.COMMAND):
            (RUN / f).unlink(missing_ok=True)
        if xvfb is not None:
            xvfb.terminate()
        ok = bool(results) and all(r['ok'] for r in results)
        (out / 'results.json').write_text(json.dumps({'ok': ok, 'results': results}, indent=1))
    print(f"{sum(r['ok'] for r in results)}/{len(results)} checks ok")
    sys.exit(0 if ok else 1)


class Input(sp.Input):
    """sort_probe's Input plus held keys (Shift-click)."""

    def _code(self, name):
        import ctypes
        self.x11.XStringToKeysym.restype = ctypes.c_ulong
        return self.x11.XKeysymToKeycode(self.d, ctypes.c_ulong(self.x11.XStringToKeysym(name.encode())))

    def key_down(self, name):
        self.xtst.XTestFakeKeyEvent(self.d, self._code(name), 1, 0)
        self.x11.XFlush(self.d)
        time.sleep(0.2)

    def key_up(self, name):
        self.xtst.XTestFakeKeyEvent(self.d, self._code(name), 0, 0)
        self.x11.XFlush(self.d)
        time.sleep(0.2)


if __name__ == '__main__':
    main()
