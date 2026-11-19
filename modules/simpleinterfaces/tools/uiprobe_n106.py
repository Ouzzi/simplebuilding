"""N106 (b-livemap106) in-game shots for the Simple Livemap release check: the livemap link in chat, the config screen
("map screen" of the mod), a held filled Vanilla map (the mod has no minimap of its own; a held map is what it reads as
explored), and - if a Chromium is found on the test machine - the web map of the running game in a real headless browser
in the default style (AUDIT C9 "browser from the game"). Not run by B (main session only, no xvfb in B).

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe106 --scale 2 --only simpleinterfaces/n106

The key of the private web map is read from the client's own config/simplelivemap.json and only put into the browser's URL
fragment; it is never printed or written into results.json.
"""
import glob
import json
import shutil
import subprocess
import time
import urllib.request

from pathlib import Path


def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _setup(s):
    s.close()
    s.chat('gamemode creative @s', 0.3)
    s.chat('time set noon', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('gamerule advance_time false', 0.3)
    s.chat('gamerule spawn_mobs false', 0.3)


def _run_dir(s):
    # the client run folder of the probe (sort_probe.RUN): config/ and logs/ of the running game
    return Path(__file__).resolve().parents[3] / 'integration/run-fabric-263'


def _web(run):
    """(port, token) of the built-in web map, or (None, None). Port: config value, the log says when a fallback port is used."""
    try:
        w = json.loads((run / 'config/simplelivemap.json').read_text()).get('web', {})
    except (OSError, ValueError):
        return None, None
    port = int(w.get('port', 8125))
    try:
        log = (run / 'logs/latest.log').read_text(errors='replace')
        for line in log.splitlines()[::-1]:
            if 'Simple Livemap' in line and 'port' in line and 'instead' in line:
                port = int(''.join(ch for ch in line.split('port')[-1] if ch.isdigit())[:5] or port)
                break
    except OSError:
        pass
    return port, (w.get('token') or None)


def _chromium():
    for pat in ('/root/.cache/ms-playwright/chromium_headless_shell-*/chrome-headless-shell-linux64/chrome-headless-shell',
                '/root/.cache/ms-playwright/chromium-*/chrome-linux*/chrome'):
        found = sorted(glob.glob(pat))
        if found:
            return found[-1]
    return shutil.which('chromium') or shutil.which('chromium-browser') or shutil.which('google-chrome')


def scenario(s):
    _setup(s)
    x, y, z = _pos(s)
    run = _run_dir(s)

    # 1) livemap link + status in chat (the host gets the clickable link; the key is shown as *** in status)
    s.chat('simplelivemap status', 1.0)
    s.chat('simplelivemap link', 1.0)
    s.key('t', 0.8)                       # open chat so the lines stay visible for the shot
    s.shot('1-chat-livemap-link-status')
    s.close()

    # 2) the mod's config screen (Simple Livemap settings, SimpleLib) - the "map screen" of the mod
    s.chat('simplelivemap config', 1.5)
    s.shot('2-config-bildschirm')
    s.check('config screen opened', s.screen() != 'none', s.screen())
    s.close()

    # 3) a held filled map: walk a little so chunks are explored, then hold a map (counts as explored, drawn into the tiles)
    s.chat('give @s minecraft:map 1', 0.4)
    s.key('1', 0.4)
    s.io.right_click()                    # create the map at the player's place
    time.sleep(2.0)
    for dx in (0, 48, 96, 48, 0):
        s.chat(f'tp @s {x + dx} {y + 20} {z + dx // 2}', 1.2)
    s.chat(f'tp @s {x} {y} {z} facing {x} {y} {z + 8}', 1.0)
    time.sleep(1.5)
    s.shot('3-gehaltene-karte')
    s.chat('simplelivemap status', 1.5)   # queued/drawn chunks after the walk
    s.key('t', 0.8)
    s.shot('4-status-nach-rundgang')
    s.close()

    # 4) the web map of this game in a real browser (headless Chromium): page + live.json + a tile, three styles
    port, token = _web(run)
    s.check('web map config readable', port is not None, f'port {port}')
    if port is None:
        return
    base = f'http://127.0.0.1:{port}'
    try:
        req = urllib.request.Request(base + '/live.json', headers={'Authorization': 'Bearer ' + token} if token else {})
        with urllib.request.urlopen(req, timeout=5) as r:
            live = json.loads(r.read())
        s.check('live.json answers with dimensions', bool(live.get('dimensions')), f"{len(live.get('dimensions', []))} dimension(s)")
    except Exception as e:  # noqa: BLE001 - one failed check, the shots still run
        s.check('live.json answers with dimensions', False, type(e).__name__)
    chrome = _chromium()
    s.check('headless Chromium found for the browser shots', chrome is not None, chrome or 'none')
    if chrome is None:
        return
    shots = Path(s.out) / 'shots'
    shots.mkdir(parents=True, exist_ok=True)
    out = shots / 'simpleinterfaces-n106-5-browser-standardstil.png'
    prof = Path(s.out) / 'chrome-profile'
    shutil.rmtree(prof, ignore_errors=True)
    # default style "blocks"; the other two styles need the gear panel (click test on the dev stage / Playwright shots)
    args = [chrome, '--headless=new', '--no-sandbox', '--disable-gpu', f'--user-data-dir={prof}', '--window-size=1280,800',
            '--virtual-time-budget=6000', f'--screenshot={out}', base + '/' + (f'#t={token}' if token else '')]
    try:
        subprocess.run(args, timeout=40, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False)
    except (OSError, subprocess.TimeoutExpired):
        pass
    shutil.rmtree(prof, ignore_errors=True)
    s.check('browser shot of the web map', out.exists() and out.stat().st_size > 10_000, out.name)


SCENARIOS = {'simpleinterfaces/n106': scenario}
