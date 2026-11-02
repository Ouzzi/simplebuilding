"""Pipe window (Simple Storage, ROHR33) with the REAL operating-system mouse.

Runs the full integration client (`:integration:runIntegrationClient`) under an X server (xvfb-run). The client-side PipeProbe opens
the pipe window on the title screen (menu without a server, same cycle as the server); this script drives the X11 pointer through
XTest (the Pointer class of Simple Earth's menu_probe.py): every side key is clicked through in - out - off - in and the result read
back from the status file; hover tooltips and the compass layout are checked.

Usage (inside xvfb-run, from the repository root):
    python3.12 modules/simplestorage/tools/pipe_probe.py --out /var/tmp/sb/pipeprobe --scales 2,3
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
REQUEST, STATUS, COMMAND = 'simplestorage-pipe-probe.json', 'simplestorage-pipe-status.json', 'simplestorage-pipe-command.json'
IN, OUT, OFF = 0, 1, 2

_spec = importlib.util.spec_from_file_location('menu_probe', REPO / 'modules/simpleearth/tools/menu_probe.py')
_menu = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_menu)


def status():
    try:
        return json.loads((RUN / STATUS).read_text())
    except (OSError, ValueError):
        return None


def fresh():
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
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    scales = [int(s) for s in args.scales.split(',')]
    RUN.mkdir(parents=True, exist_ok=True)
    for f in (STATUS, COMMAND):
        (RUN / f).unlink(missing_ok=True)
    (RUN / REQUEST).write_text(json.dumps({'width': 1280, 'height': 720, 'guiScale': scales[0]}))
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
        deadline = time.time() + 1500
        while time.time() < deadline:
            st = status()
            if st and st.get('screen') == 'ProbeScreen' and st.get('rects'):
                break
            if game.poll() is not None:
                raise SystemExit('client ended before the window opened (see client.log)')
            time.sleep(1)
        else:
            raise SystemExit('window never opened')
        print(f'window open after {time.time() - started:.0f} s', flush=True)
        io = _menu.Pointer()
        for scale in scales:
            st = command('guiScale', value=scale)
            tag = f'GUI {st["scale"]}'
            command('open')
            st = fresh()
            r = st['rects']
            cx = lambda n: r[n][0]
            cy = lambda n: r[n][1]
            check(f'{tag}: compass layout', cy('north') < cy('west') == cy('east') < cy('south') and cx('west') < cx('north') == cx('south') < cx('east'),
                  {n: r[n][:2] for n in ('north', 'west', 'east', 'south')})
            check(f'{tag}: up/down column beside the compass', cx('up') > cx('east') and cx('down') == cx('up') and cy('up') < cy('down'),
                  {n: r[n][:2] for n in ('up', 'down')})
            check(f'{tag}: start = one output (down), rest input', st['values']['down'] == OUT and all(st['values'][n] == IN for n in ('north', 'south', 'east', 'west', 'up')), st['values'])
            command('shot')

            def click(name):
                st = fresh()
                x, y = centre(st, name)
                io.move(x, y)
                time.sleep(0.4)
                io.click(x, y)
                return fresh()

            def hover(name):
                st = fresh()
                x, y = centre(st, name)
                io.move(x, y)
                time.sleep(0.6)
                return fresh()

            for name in ('north', 'west', 'east', 'south', 'up', 'down'):
                start = fresh()['values'][name]
                seq = []
                for _ in range(3):
                    seq.append(click(name)['values'][name])
                want = [(start + k) % 3 for k in (1, 2, 3)]
                check(f'{tag}: {name} cycles in-out-off-in with the real mouse', seq == want, f'{start} -> {seq}')
            # several outputs at once
            click('north')
            st = click('east')
            check(f'{tag}: two outputs at once (north + east + down)', st['values']['north'] == OUT and st['values']['east'] == OUT and st['values']['down'] == OUT, st['values'])
            command('shot')
            st = click('west')
            st = click('west')
            command('shot')
            # tooltips
            for name, mode, word in (('north', OUT, 'output'), ('west', OFF, 'off'), ('south', IN, 'input')):
                st = hover(name)
                lines = st['values'].get('tooltip', [])
                check(f'{tag}: tooltip {name}', len(lines) >= 3 and word in lines[0].lower() and all(len(l) <= 48 for l in lines), lines)
                if name == 'west':
                    command('shot')
            # first step: up never an output
            command('tier', value='BASIC')
            command('open')
            seq = [click('up')['values']['up'] for _ in range(3)]
            check(f'{tag}: first step up skips output (off, in, off)', seq == [OFF, IN, OFF], seq)
            command('tier', value='REINFORCED')
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
