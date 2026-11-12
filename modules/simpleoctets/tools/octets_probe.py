"""Octets with the REAL operating-system mouse and keyboard (PLAN40 simpleoctets-5).

Runs the full integration client (`:integration:runIntegrationClient`) under an X server (xvfb-run). The client-side OctetsProbe
(modules/simpleoctets/shared/java/com/simpleoctets/client/OctetsProbe.java) creates a flat creative world, builds a bench of cells at
the player's feet on command and turns the view to exact points (`aim`, from the current eye, so also while sneaking). This script
presses the mouse buttons and keys through XTest (the Pointer of Simple Earth's menu_probe.py; the pointer is never moved while the
game holds it) and reads the cells from the server:

(a) stone octets into the four bottom corners and then the four top ones of an empty cell, each in the aimed corner; the eighth
    turns the cell into a stone block;
(b) a stone and an oak octet in one cell -> a mixed cell with both materials;
(c) sneaking + left click takes out only the aimed octet; sneaking + right click with an empty hand takes the aimed octet into the
    hand; a plain left click takes the whole cell;
(d) sneaking + right click with a melon slice places a melon octet in the aimed corner, without sneaking nothing is placed;
(e) survival, sneaking, the left button held on an oak cell and on a mixed cell: screenshots with the break cracks (the mixed cell's
    are drawn by the module); after letting go the cells are unchanged.

Usage (inside xvfb-run, from the repository root):
    python3.12 modules/simpleoctets/tools/octets_probe.py --out /var/tmp/sb/octetsprobe
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
REQUEST, STATUS, COMMAND = 'simpleoctets-octets-probe.json', 'simpleoctets-octets-status.json', 'simpleoctets-octets-command.json'

_spec = importlib.util.spec_from_file_location('menu_probe', REPO / 'modules/simpleearth/tools/menu_probe.py')
_menu = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_menu)


class Input(_menu.Pointer):
    """Pointer plus keyboard and the right button (the pattern of mouse_probe.py)."""

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

    def press(self, button, hold=0.15):
        """A mouse button pressed and let go WITHOUT moving the pointer (a move would turn the view)."""
        self.button(button, True)
        time.sleep(hold)
        self.button(button, False)
        time.sleep(0.7)


LEFT, RIGHT = 1, 3


def status():
    try:
        return json.loads((RUN / STATUS).read_text())
    except (OSError, ValueError):
        return None


def fresh(wait=3):
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
    for _ in range(150):
        time.sleep(0.05)
        if not (RUN / COMMAND).exists():
            time.sleep(0.5)
            return fresh()
    raise SystemExit(f'command {op} not taken')


def cell(st, name):
    return st['server']['cells'][name]


def bit(ix, iy, iz):
    return 1 << (ix | iy << 1 | iz << 2)


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
    (RUN / REQUEST).write_text(json.dumps({'width': 1920, 'height': 1080, 'guiScale': args.scale}))
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

    def chat(c, wait=0.8):
        command('cmd', value=c)
        time.sleep(wait)
        return fresh()

    def hold(item):
        return chat(f'item replace entity @s weapon.mainhand with {item}')

    def aim(x, y, z):
        command('aim', x=x, y=y, z=z)
        time.sleep(0.6)  # the new view reaches the server with the next movement packets
        return fresh()

    def sneak(on):
        (io.key_down if on else io.key_up)('Shift_L')
        time.sleep(1.5)  # the sneak state is read once per tick; under Xvfb the full pack draws few frames
        return fresh()

    try:
        deadline = time.time() + 1800
        while time.time() < deadline:
            st = status()
            if st and st.get('ready') and st.get('screen') == 'none' and st['tick'] > 5:
                break
            if game.poll() is not None:
                raise SystemExit('client ended before the world opened (see client.log)')
            time.sleep(1)
        else:
            raise SystemExit('world never opened')
        print(f'world open after {time.time() - started:.0f} s', flush=True)
        io = Input()
        time.sleep(3)
        st = fresh()
        # one move to the window centre before the game holds the pointer; afterwards only buttons and keys
        io.move(st['windowX'] + st['windowW'] / 2, st['windowY'] + st['windowH'] / 2)
        time.sleep(1)
        st = command('bench')
        time.sleep(1.0)
        st = fresh()
        check('bench built', 'cells' in st.get('server', {}) and cell(st, 'sneak')['mask'] == 0x33, st.get('server'))

        # ---------- (a) corners of an empty cell, bottom layer then top layer, far ones first
        hold('simpleoctets:stone_octet 64')
        cx, cy, cz = cell(st, 'corner')['pos']
        mask = 0
        order = [(ix, iy, iz) for iy in (0, 1) for iz in (1, 0) for ix in (0, 1)]
        for n, (ix, iy, iz) in enumerate(order):
            st = aim(cx + 0.25 + 0.5 * ix, cy + 0.5 * iy, cz + 0.25 + 0.5 * iz)
            io.press(RIGHT)
            st = fresh()
            c = cell(st, 'corner')
            mask |= bit(ix, iy, iz)
            if n < 7:
                check(f'(a) octet {n + 1} lands in the aimed corner ({ix},{iy},{iz})', c['mask'] == mask, (hex(c['mask']), hex(mask), st.get('hit')))
                if n == 0:
                    snap('a-first-corner')
                if n == 3:
                    snap('a-bottom-layer')
            else:
                check('(a) the eighth octet turns the cell into a stone block', c['block'] == 'minecraft:stone', c)
                snap('a-full-block')

        # ---------- (b) two materials in one cell
        mx, my, mz = cell(st, 'mixed')['pos']
        hold('simpleoctets:stone_octet 64')
        aim(mx + 0.25, my, mz + 0.75)
        io.press(RIGHT)
        hold('simpleoctets:oak_octet 64')
        aim(mx + 0.75, my, mz + 0.75)
        io.press(RIGHT)
        st = fresh()
        c = cell(st, 'mixed')
        check('(b) stone + oak octet make a mixed cell', c['block'] == 'simpleoctets:mixed_octet'
              and 'simpleoctets:stone_octet' in c['parts'] and 'simpleoctets:oak_octet' in c['parts'], c)
        snap('b-mixed-cell')

        # ---------- (c) sneak-break one octet, take one out with the empty hand, then the whole cell
        sx, sy, sz = cell(st, 'sneak')['pos']
        hold('minecraft:air')
        sneak(True)
        aim(sx + 0.75, sy + 0.5, sz + 0.75)
        io.press(LEFT)
        st = fresh()
        c = cell(st, 'sneak')
        check('(c) sneaking + left click takes out only the aimed octet', c['mask'] == 0x33 & ~bit(1, 0, 1), (hex(c['mask']), st.get('hit')))
        snap('c-sneak-break')
        aim(sx + 0.25, sy + 0.5, sz + 0.75)
        io.press(RIGHT)
        st = fresh()
        c = cell(st, 'sneak')
        check('(c) sneaking + right click with an empty hand takes the aimed octet', c['mask'] == 0x33 & ~bit(1, 0, 1) & ~bit(0, 0, 1)
              and st['server']['held'].startswith('simpleoctets:oak_octet'), (hex(c['mask']), st['server']['held']))
        sneak(False)
        hold('minecraft:air')
        aim(sx + 0.25, sy + 0.5, sz + 0.25)
        io.press(LEFT)
        st = fresh()
        check('(c) a plain left click takes the whole cell', cell(st, 'sneak')['block'] == 'minecraft:air', cell(st, 'sneak'))
        snap('c-whole-cell-gone')

        # ---------- (d) melon slice: sneaking places a melon octet, standing does not
        wx, wy, wz = cell(st, 'melon')['pos']
        hold('minecraft:melon_slice 16')
        sneak(True)
        aim(wx + 0.25, wy, wz + 0.75)
        io.press(RIGHT)
        st = fresh()
        c = cell(st, 'melon')
        check('(d) sneaking + right click with a melon slice places a melon octet in the aimed corner',
              c['block'] == 'simpleoctets:melon_octet' and c['mask'] == bit(0, 0, 1), c)
        sneak(False)
        aim(wx + 0.75, wy, wz + 0.75)
        io.press(RIGHT)
        st = fresh()
        c = cell(st, 'melon')
        check('(d) without sneaking the melon slice places nothing', c['mask'] == bit(0, 0, 1), c)
        snap('d-melon-octet')

        # ---------- (e) break cracks on an oak cell and on a mixed cell (survival, sneaking, button held)
        chat('gamemode survival @s')
        hold('minecraft:air')
        sneak(True)
        # the octet nearest to the player: oak cell (east, bottom half) on its top, mixed cell (west, full) on its top
        for name, dx, dy in (('cracks', 0.25, 0.5), ('cracksMixed', 0.75, 1.0)):
            kx, ky, kz = cell(st, name)['pos']
            before = cell(fresh(), name)['mask']
            aim(kx + dx, ky + dy, kz + 0.25)
            io.button(LEFT, True)
            time.sleep(1.4)
            snap(f'e-{name.lower()}-cracks')
            io.button(LEFT, False)
            time.sleep(0.8)
            st = fresh()
            check(f'(e) {name}: letting go before the end leaves the cell whole', cell(st, name)['mask'] == before, cell(st, name))
        sneak(False)
        chat('gamemode creative @s')
        command('quit')
    finally:
        try:
            io.key_up('Shift_L')
        except Exception:
            pass
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
