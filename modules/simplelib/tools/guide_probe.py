"""Simple Modpack guide with the REAL operating-system mouse (b-guide01, after modules/simpleearth/tools/menu_probe.py).

Runs the full integration client (`:integration:runIntegrationClient`, every module plus the dev mods) under an X server
(xvfb-run). SimpleLib's client-side GuideProbe opens the guide on the title screen (every second chapter marked unlocked)
and reports the bookmark rectangles; this script moves and clicks the X11 pointer through XTest (libXtst via ctypes), so
the clicks take the player's path X server -> SDL -> MouseHandler -> screen. Checks per GUI scale:
  side bookmark opens its chapter, a locked one does not, a top bookmark switches the section (SimpleBuilding's own
  screen), "More Simple mods" leads to the sub-mod chapters, the next arrow turns the page, another top bookmark.

Usage (inside xvfb-run, from the repository root):
    python3.12 modules/simplelib/tools/guide_probe.py --out /var/tmp/sb/guideprobe --scales 4,2
Exit 0 when every check passed; results in <out>/results.json, screenshots copied there.
"""
import argparse
import ctypes
import json
import shutil
import subprocess
import sys
import time
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
RUN = REPO / 'integration/run-fabric-263'
REQUEST, STATUS, COMMAND = 'simplelib-guide-probe.json', 'simplelib-guide-status.json', 'simplelib-guide-command.json'


class Pointer:
    """The X11 pointer through XTest (button 1 left)."""

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
        time.sleep(0.3)
        self.button(1, True)
        time.sleep(0.12)
        self.button(1, False)
        time.sleep(1.2)


def status():
    try:
        return json.loads((RUN / STATUS).read_text())
    except (OSError, ValueError):
        return None


def command(op, **kw):
    (RUN / COMMAND).write_text(json.dumps(dict(op=op, **kw)))
    for _ in range(200):
        time.sleep(0.05)
        if not (RUN / COMMAND).exists():
            time.sleep(1.0)
            return
    raise SystemExit(f'command {op} not taken')


def centre(st, rect):
    x, y, w, h = rect
    return st['windowX'] + x + w / 2, st['windowY'] + y + h / 2


def wait(pred, seconds=15):
    end = time.time() + seconds
    st = status()
    while time.time() < end:
        st = status()
        if st and pred(st):
            return st
        time.sleep(0.3)
    return st


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', required=True)
    ap.add_argument('--scales', default='4,2')
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    scales = [int(s) for s in args.scales.split(',')]
    RUN.mkdir(parents=True, exist_ok=True)
    for f in (STATUS, COMMAND):
        (RUN / f).unlink(missing_ok=True)
    (RUN / REQUEST).write_text(json.dumps({'width': 1920, 'height': 1080, 'guiScale': scales[0], 'section': 'simplequalityoflife'}))
    started = time.time()
    log = open(out / 'client.log', 'w')
    game = subprocess.Popen(['./gradlew', ':integration:runIntegrationClient', '-Pforge263=false'], cwd=REPO, stdout=log, stderr=subprocess.STDOUT,
                            start_new_session=True)
    results, ok = [], True

    def check(name, good, detail):
        nonlocal ok
        ok &= bool(good)
        results.append({'check': name, 'ok': bool(good), 'detail': detail})
        print(('OK   ' if good else 'FAIL ') + name + ': ' + str(detail), flush=True)

    def top(st, section):
        for t in st.get('top', []):
            if t['id'] == section:
                return t['rect']
        return None

    try:
        deadline = time.time() + 1200
        while time.time() < deadline:
            st = status()
            if st and st.get('top'):
                break
            if game.poll() is not None:
                raise SystemExit('client ended before the guide opened (see client.log)')
            time.sleep(1)
        else:
            raise SystemExit('guide never opened')
        print(f'guide open after {time.time() - started:.0f} s: {[t["id"] for t in st["top"]]}', flush=True)
        ptr = Pointer()
        for scale in scales:
            command('guiScale', value=scale)
            command('open', section='simplequalityoflife')
            st = wait(lambda s: s.get('section') == 'simplequalityoflife' and s.get('tabs'))
            tag = f'GUI {st["scale"]}'
            check(f'{tag}: guide opens at Simple Quality of Life', st.get('screen') == 'ModpackGuideScreen', st.get('screen'))
            command('shot')
            tabs = st.get('tabs', [])
            first = next((t for t in tabs if t['chapter'] == 0), None)
            if first:
                ptr.click(*centre(st, first['rect']))
                st = wait(lambda s: s.get('chapter') == 0, 5)
                check(f'{tag}: side bookmark opens chapter 1', st.get('chapter') == 0, f'chapter {st.get("chapter")}')
                command('shot')
            locked = next((t for t in tabs if t['chapter'] == 1), None)
            if locked:
                before = st.get('chapter')
                ptr.click(*centre(st, locked['rect']))
                st = status()
                check(f'{tag}: locked bookmark keeps the page', st.get('chapter') == before, f'{before} -> {st.get("chapter")}')
                command('shot')
            rect = top(st, 'simplebuilding')
            if rect:
                ptr.click(*centre(st, rect))
                st = wait(lambda s: s.get('screen') == 'GuideBookScreen', 8)
                check(f'{tag}: top bookmark SimpleBuilding opens its own book', st.get('screen') == 'GuideBookScreen', st.get('screen'))
                command('shot')
                st = wait(lambda s: 'more' in s, 5)
                if 'more' in st:
                    ptr.click(*centre(st, st['more']))
                    st = wait(lambda s: s.get('section') == 'simplebuilding' and s.get('screen') == 'ModpackGuideScreen', 8)
                    check(f'{tag}: "More Simple mods" opens the sub-mod chapters', st.get('section') == 'simplebuilding'
                          and st.get('screen') == 'ModpackGuideScreen', f'{st.get("screen")} {st.get("section")}')
                    command('shot')
                    target = next((t for t in st.get('tabs', []) if t['chapter'] == 2), None)
                    if target:
                        ptr.click(*centre(st, target['rect']))
                        st = wait(lambda s: s.get('chapter') == 2, 5)
                        check(f'{tag}: sub-mod chapter bookmark opens it', st.get('chapter') == 2, f'chapter {st.get("chapter")}')
                        spread = st.get('spread')
                        ptr.click(*centre(st, st['next']))
                        st = status()
                        check(f'{tag}: next arrow turns the page', st.get('spread', 0) > spread, f'{spread} -> {st.get("spread")}')
                        command('shot')
                else:
                    check(f'{tag}: "More Simple mods" bookmark shown', False, 'no rectangle reported')
            else:
                check(f'{tag}: SimpleBuilding top bookmark shown', False, [t['id'] for t in st.get('top', [])])
            other = next((t for t in st.get('top', []) if t['id'] not in ('simplebuilding', 'simplequalityoflife')), None)
            if other:
                ptr.click(*centre(st, other['rect']))
                st = wait(lambda s: s.get('section') == other['id'], 8)
                check(f'{tag}: top bookmark {other["id"]} switches the section', st.get('section') == other['id'], st.get('section'))
                command('shot')
        command('quit')
    finally:
        if game.poll() is None and not results:
            (RUN / COMMAND).write_text(json.dumps({'op': 'quit'}))
        try:
            game.wait(timeout=600)
        except subprocess.TimeoutExpired:
            # sb-test rule: never kill anything, not even an own run - report it and let the client time out by itself
            print('client did not quit within 600 s (left running)', flush=True)
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
