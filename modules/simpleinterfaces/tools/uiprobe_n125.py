"""N125 release polish, mods 20-22 (offen-vor-release.md section 3, "Szenen" of Simple Maps, Simple Livemap, Simple Shops), word for word:

Simple Maps       "Config-Bildschirm Simple Maps (Namen nach Fix), Wegfinder, Bildkarte; Web-Export im Browser (W4C-27)."
  n125-maps-config     Config screen through the real Mod Menu (SimpleLib release_probe.config_screen, as N111): title "Simple Maps",
                       every tab, every option name a lang text (no raw paths); one shot per page.
  n125-maps-wayfinder  Wayfinder Map in the main hand (read back with 'execute if items ... weapon.mainhand'), a walk explores the
                       ground, REAL right click opens WayfinderScreen (title "Wayfinder Map"); REAL mouse wheel zooms and a REAL
                       left-button drag moves the map (each judged by the screenshot changing); then the Mega Wayfinder Map opens the
                       same screen (dimension buttons in the title bar: shot).
  n125-maps-picture    Picture map: a 256 x 256 PNG written into the client's config/simplemaps/images/, /simplemaps picture opens
                       PictureMapScreen (the probe's screen route only if the command did not), File/Size labels read back, survival
                       with 4 empty maps, REAL click "Make maps" -> 4 locked filled maps in hotbar 1-4 and no empty map left (one used
                       per map); the four maps hung into a 2 x 2 item-frame wall (shot of the picture).
  n125-maps-web        Web export: wayfinder walk, webEnabled on (/simplemaps config, reset at the end), /simplemaps web export full,
                       /simplemaps web status names the folder; markers.json lists the Overworld with tiles; the exported viewer page in
                       a headless Chromium (served from the folder on 127.0.0.1): dimensions read, tiles loaded, shot. W4C-27 (owner)
                       judges the look.
Simple Livemap    "Chat-Link, Ziehen/Zoomen, 3 Stile (W4C-25)."
  n125-livemap-chat    /simplelivemap link, chat opened (T), REAL click on the link line -> Vanilla's ConfirmLinkScreen (N125 made the
                       line clickable; before it was text only). The address/key is never written into results.json.
  n125-livemap-browser The built-in web map of the running game in a headless Chromium driven over the DevTools protocol (real
                       pointer/wheel events, no Playwright needed): live.json read (dimensions), tiles drawn; DRAG 200 px moves the view
                       by 200/scale blocks; WHEEL zooms x1.25; the +/- buttons x2/x0.5; the gear opens "View settings" and the three
                       map styles Blocks / Shaded relief / Cartographer are clicked one after another (OPTS.style read back, one shot
                       each, the paper style differs visibly). Firefox/Safari stay with the owner (N106).
Simple Shops      "Besitzer-/Käufer-/Terminal-/Lager-Fenster, Vitrine, Shop-Musik, Wartungsschild (2 Spieler) (W4C-43)."
  n125-shops-windows   modules/simpleshops/tools/uiprobe_simpleshops.py owner_buyer: owner, buyer (developer view), storage windows
                       with real placement/clicks (a-w4c, 52/53; its last Display Case check is the "shops (Vitrine)" probe fix).
  n125-shops-terminal  the same file's terminal scenario (tabs, own shop, browse/sort/search, buying from a server shop). Do not run it
                       together with simpleshops/terminal in one client: both count server shop ids s001... from a fresh world.
  n125-shops-case      Vitrine: a free Display Case and a Glow Display Case placed with REAL right clicks, quick-stocked by a REAL right
                       click with diamonds / golden apples in hand (hand empty afterwards), empty-hand click opens the case's own stock
                       window (ShopScreens$Stock) showing the stack; REAL Shift (held >= 0.8 s) + right click with red dye tints the
                       case (tint=red); night shot (the glow case draws its goods fully bright).
  n125-shops-music     Shop music: a shop placed for real, a disc put into the owner screen's disc slot (real clicks), the sales block
                       holds the disc (server data); shot near the shop (music notes rise above it). Hearing it is the owner's (W4C-43).
                       Maintenance sign: needs a second player (owner works while a customer waits) - not reachable with one client;
                       gametests owner_first_maintenance (ShopsTests) + StorageTests cover the logic, the look stays W4C-43.

Patterns (uiprobe_n121/n123 + N122/N123 lessons): position via int(float()) of status 'pos'; 'tp ... facing' with y - 1.62; real clicks
only after the crosshair target was read back; main hand = selected hotbar slot; Shift held >= 0.8 s; chat commands ~1-1.5 s each;
gamerules advance_time/spawn_mobs/random_tick_speed. No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe125 --scale 2 \
        --only n125-maps-config,n125-maps-wayfinder,n125-maps-picture,n125-maps-web,n125-livemap-chat,n125-livemap-browser,n125-shops-windows,n125-shops-terminal,n125-shops-case,n125-shops-music
"""
import base64
import functools
import glob
import http.server
import importlib.util
import json
import os
import re
import shutil
import socket
import struct
import subprocess
import threading
import time
import urllib.request
from pathlib import Path

TAG = 'N125'
REPO = Path(__file__).resolve().parents[3]
RUN = REPO / 'integration/run-fabric-263'


def _load(name, rel):
    spec = importlib.util.spec_from_file_location(name, REPO / rel)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


rp = _load('release_probe_n125', 'modules/simplelib/tools/release_probe.py')
shops = _load('uiprobe_simpleshops_n125', 'modules/simpleshops/tools/uiprobe_simpleshops.py')


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _log(s):
    p = s.out / 'client.log'
    return p.read_text(errors='replace') if p.exists() else ''


def _say(s, cond, tag):
    """cond = the part between 'execute' and 'run', e.g. 'if block 1 2 3 minecraft:stone'."""
    s.chat(f'execute {cond} run say {TAG} {tag}', 0.35)


def _said(s, tags, settle=1.5):
    time.sleep(settle)
    log = _log(s)
    return {t: bool(re.search(TAG + ' ' + re.escape(t) + r'(?![\w-])', log)) for t in tags}


def _check_said(s, name, tags):
    got = _said(s, tags)
    miss = [t for t, ok in got.items() if not ok]
    s.check(name, not miss, f'{len(tags) - len(miss)}/{len(tags)}' + (f' missing {miss[:12]}' if miss else ''))
    return not miss


def _mark(s, marker):
    s.chat(f'say {TAG} MARK {marker}', 0.35)


def _after(s, marker, pattern, lines_after=30):
    """Last regex match of pattern in the log lines after the last 'N125 MARK <marker>' line."""
    time.sleep(1.0)
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} MARK {marker}')), default=-1)
    found = None
    for ln in lines[idx + 1: idx + 1 + lines_after] if idx >= 0 else []:
        m = re.search(pattern, ln)
        if m:
            found = m
    return found


def _prep(s, mode='creative'):
    s.close()
    s.chat('ride @s dismount', 0.3)
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true', 'keep_inventory false'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    s.chat('clear @s', 0.3)


def _move(s, dx):
    """Moves dx blocks east onto fresh flat ground and returns the new feet position."""
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + dx + 0.5} {y} {z + 0.5} 0 0', 1.0)
    time.sleep(1.5)
    return _pos(s)


def _wait_screen(s, fragment, seconds=6.0):
    st = s.wait_screen(fragment, seconds)
    return st if fragment in st.get('screen', '') else None


def _aim(s, px, py, pz, want, tag, tries=4):
    """Eyes onto the world point (tp facing from the feet: y - 1.62) and waits until the crosshair reads back block `want`."""
    hit = None
    for _ in range(tries):
        x, y, z = _pos(s)
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {px:.3f} {py - 1.62:.3f} {pz:.3f}', 0.8)
        for _ in range(6):
            hit = s.st().get('hitBlock')
            if hit == want:
                return True
            time.sleep(0.3)
    s.check(f'{tag}: crosshair on {want}', False, hit)
    return False


def _sneak_click(s):
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.8)
        s.check('client sneaks at the click (real Shift held)', bool(s.st().get('sneaking')), s.last.get('sneaking'))
        s.io.right_click()
    finally:
        s.io.key_up('Shift_L')
    time.sleep(0.6)


def _shot_path(s, name):
    return s.out / 'shots' / f"{s.scenario.replace('/', '-')}-{name}.png"


def _differs(a, b, min_rms=2.0):
    """(changed, rms) of two screenshots (PIL); a missing file counts as unchanged."""
    try:
        from PIL import Image, ImageChops, ImageStat
        ia, ib = Image.open(a).convert('RGB'), Image.open(b).convert('RGB')
        if ia.size != ib.size:
            return True, 999.0
        rms = sum(ImageStat.Stat(ImageChops.difference(ia, ib)).rms) / 3
        return rms >= min_rms, round(rms, 2)
    except Exception as e:  # noqa: BLE001
        return False, f'{type(e).__name__}: {e}'


def _walk(s, x, y, z, points, wait=1.5):
    for dx, dz in points:
        s.chat(f'tp @s {x + dx + 0.5} {y} {z + dz + 0.5}', wait)


# ---------------------------------------------------------------- headless Chromium over the DevTools protocol (stdlib only)
def _chromium():
    for pat in ('/root/.cache/ms-playwright/chromium_headless_shell-*/chrome-headless-shell-linux64/chrome-headless-shell',
                '/root/.cache/ms-playwright/chromium-*/chrome-linux*/chrome'):
        found = sorted(glob.glob(pat))
        if found:
            return found[-1]
    return shutil.which('chromium') or shutil.which('chromium-browser') or shutil.which('google-chrome')


def _free_port():
    with socket.socket() as so:
        so.bind(('127.0.0.1', 0))
        return so.getsockname()[1]


class Cdp:
    """One headless Chromium page, driven through the DevTools websocket (minimal RFC 6455 client: masked text frames out)."""

    def __init__(self, chrome, profile, width=1280, height=800):
        self.port = _free_port()
        self.profile = Path(profile)
        shutil.rmtree(self.profile, ignore_errors=True)
        self.proc = subprocess.Popen([chrome, '--headless=new', '--no-sandbox', '--disable-gpu', '--hide-scrollbars',
                                      f'--remote-debugging-port={self.port}', '--remote-allow-origins=*', f'--user-data-dir={self.profile}',
                                      f'--window-size={width},{height}', 'about:blank'],
                                     stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, start_new_session=True)
        ws = None
        for _ in range(60):
            time.sleep(0.25)
            try:
                with urllib.request.urlopen(f'http://127.0.0.1:{self.port}/json/list', timeout=2) as r:
                    pages = [t for t in json.loads(r.read()) if t.get('type') == 'page']
                if pages:
                    ws = pages[0]['webSocketDebuggerUrl']
                    break
            except OSError:
                pass
        if not ws:
            self.close()
            raise RuntimeError('no DevTools page target')
        m = re.match(r'ws://([^/:]+):(\d+)(/.*)', ws)
        self.sock = socket.create_connection((m.group(1), int(m.group(2))), timeout=20)
        key = base64.b64encode(os.urandom(16)).decode()
        self.sock.sendall((f'GET {m.group(3)} HTTP/1.1\r\nHost: {m.group(1)}:{m.group(2)}\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n'
                           f'Sec-WebSocket-Key: {key}\r\nSec-WebSocket-Version: 13\r\n\r\n').encode())
        head = b''
        while b'\r\n\r\n' not in head:
            chunk = self.sock.recv(1)
            if not chunk:
                raise RuntimeError('websocket handshake closed')
            head += chunk
        if b' 101 ' not in head.split(b'\r\n', 1)[0]:
            raise RuntimeError('websocket handshake refused')
        self.next_id = 0
        self.call('Page.enable')
        self.call('Runtime.enable')

    def _recv_exact(self, n):
        buf = b''
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise RuntimeError('websocket closed')
            buf += chunk
        return buf

    def _send(self, text):
        data = text.encode()
        head = bytes([0x81])
        n = len(data)
        if n < 126:
            head += bytes([0x80 | n])
        elif n < 65536:
            head += bytes([0x80 | 126]) + struct.pack('>H', n)
        else:
            head += bytes([0x80 | 127]) + struct.pack('>Q', n)
        mask = os.urandom(4)
        self.sock.sendall(head + mask + bytes(b ^ mask[i % 4] for i, b in enumerate(data)))

    def _recv(self):
        parts = b''
        while True:
            b0, b1 = self._recv_exact(2)
            n = b1 & 0x7F
            if n == 126:
                n = struct.unpack('>H', self._recv_exact(2))[0]
            elif n == 127:
                n = struct.unpack('>Q', self._recv_exact(8))[0]
            payload = self._recv_exact(n)
            op = b0 & 0x0F
            if op == 0x8:
                raise RuntimeError('websocket closed by the browser')
            if op in (0x9, 0xA):  # ping/pong: not used by Chromium here
                continue
            parts += payload
            if b0 & 0x80:
                return json.loads(parts.decode('utf-8', 'replace'))

    def call(self, method, **params):
        self.next_id += 1
        mid = self.next_id
        self._send(json.dumps({'id': mid, 'method': method, 'params': params}))
        while True:
            msg = self._recv()
            if msg.get('id') == mid:
                if 'error' in msg:
                    raise RuntimeError(f"{method}: {msg['error'].get('message')}")
                return msg.get('result', {})

    def goto(self, url, settle=4.0):
        self.call('Page.navigate', url=url)
        time.sleep(settle)

    def js(self, expression):
        r = self.call('Runtime.evaluate', expression=expression, returnByValue=True, awaitPromise=True)
        if r.get('exceptionDetails'):
            raise RuntimeError('js: ' + str(r['exceptionDetails'].get('text')) + ' ' + str((r['exceptionDetails'].get('exception') or {}).get('description', ''))[:200])
        return (r.get('result') or {}).get('value')

    def center(self, selector):
        return self.js(f'(() => {{ const e = document.querySelector({json.dumps(selector)}); if (!e) return null; '
                       'const r = e.getBoundingClientRect(); return [r.x + r.width / 2, r.y + r.height / 2]; })()')

    def mouse(self, kind, x, y, **kw):
        self.call('Input.dispatchMouseEvent', type=kind, x=x, y=y, **kw)

    def click(self, x, y):
        self.mouse('mouseMoved', x, y)
        self.mouse('mousePressed', x, y, button='left', buttons=1, clickCount=1)
        self.mouse('mouseReleased', x, y, button='left', buttons=0, clickCount=1)
        time.sleep(0.4)

    def click_el(self, selector):
        c = self.center(selector)
        if c:
            self.click(*c)
        return c is not None

    def drag(self, x0, y0, x1, y1, steps=10):
        self.mouse('mouseMoved', x0, y0)
        self.mouse('mousePressed', x0, y0, button='left', buttons=1, clickCount=1)
        for i in range(1, steps + 1):
            self.mouse('mouseMoved', x0 + (x1 - x0) * i / steps, y0 + (y1 - y0) * i / steps, button='left', buttons=1)
            time.sleep(0.03)
        self.mouse('mouseReleased', x1, y1, button='left', buttons=0, clickCount=1)
        time.sleep(0.5)

    def wheel(self, x, y, dy):
        self.mouse('mouseWheel', x, y, deltaX=0, deltaY=dy)
        time.sleep(0.5)

    def shot(self, path):
        data = self.call('Page.captureScreenshot', format='png').get('data', '')
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        Path(path).write_bytes(base64.b64decode(data))
        return Path(path).stat().st_size

    def close(self):
        try:
            self.sock.close()
        except Exception:  # noqa: BLE001
            pass
        if self.proc.poll() is None:
            self.proc.terminate()
            try:
                self.proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                self.proc.kill()
        shutil.rmtree(self.profile, ignore_errors=True)


def _serve(folder):
    """A quiet static web server for `folder` on 127.0.0.1 (thread). Returns (server, port)."""
    class Quiet(http.server.SimpleHTTPRequestHandler):
        def log_message(self, *a):
            pass
    srv = http.server.ThreadingHTTPServer(('127.0.0.1', 0), functools.partial(Quiet, directory=str(folder)))
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    return srv, srv.server_address[1]


# ---------------------------------------------------------------- Simple Maps
def maps_config(s):
    s.chat('gamemode creative @s', 0.5)
    rp.config_screen(s, 'simplemaps', 'simple maps', 'Simple Maps')


def _open_map(s, item, tag):
    """Item into the selected hotbar slot 1, read back as the main hand, look up, REAL right click -> WayfinderScreen."""
    s.key('1', 0.4)
    s.chat(f'item replace entity @s hotbar.0 with {item}', 0.4)
    _say(s, f'if items entity @s weapon.mainhand {item}', f'{tag}-in-hand')
    _check_said(s, f'{tag}: {item} in the main hand (selected hotbar slot)', [f'{tag}-in-hand'])
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 -60', 1.0)
    s.io.right_click()
    st = _wait_screen(s, 'WayfinderScreen', 8)
    s.check(f'{tag}: REAL right click with the map opens WayfinderScreen', st is not None, s.last.get('screen'))
    return st


def maps_wayfinder(s):
    _prep(s)
    x, y, z = _move(s, 10)
    s.key('1', 0.4)
    s.chat('item replace entity @s hotbar.0 with simplemaps:wayfinder_map', 0.4)
    _walk(s, x, y, z, [(0, 0), (32, 0), (32, 32), (0, 32), (0, 0)], 2.0)   # held in the hand: the map explores
    st = _open_map(s, 'simplemaps:wayfinder_map', 'wayfinder')
    if not st:
        return
    title = rp.plain(st.get('title', ''))
    s.check('wayfinder: screen title "Wayfinder Map"', 'Wayfinder Map' in title, title)
    time.sleep(2.0)   # tiles arrive from the server
    s.shot('open')
    # REAL wheel over the map centre: one way is the zoom limit, the other must change the picture
    s.wheel(640, 380, clicks=2, down=False)
    time.sleep(1.0)
    s.shot('wheel-in')
    changed, rms = _differs(_shot_path(s, 'open'), _shot_path(s, 'wheel-in'))
    if not changed:
        s.wheel(640, 380, clicks=2, down=True)
        time.sleep(1.0)
        s.shot('wheel-out')
        changed, rms = _differs(_shot_path(s, 'open'), _shot_path(s, 'wheel-out'))
    s.check('wayfinder: REAL mouse wheel zooms the map (picture changes)', changed, f'rms {rms}')
    # REAL left-button drag over the map
    st = s.st()
    s.shot('before-drag')
    px, py = s.abs(st, 560, 380)
    s.io.move(px, py)
    time.sleep(0.3)
    s.io.button(1, True)
    for i in range(1, 11):
        s.io.move(px + 16 * i, py + 6 * i)
        time.sleep(0.05)
    s.io.button(1, False)
    time.sleep(1.0)
    s.shot('after-drag')
    changed, rms = _differs(_shot_path(s, 'before-drag'), _shot_path(s, 'after-drag'))
    s.check('wayfinder: REAL left-button drag moves the map (picture changes)', changed, f'rms {rms}')
    s.check('wayfinder: still the map screen after wheel + drag (no context menu / no close)', 'WayfinderScreen' in s.screen(), s.last.get('screen'))
    s.close()
    # the held map in first person (FirstPersonMapMixin)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 30', 1.0)
    time.sleep(1.0)
    s.shot('held-first-person')
    # Mega Wayfinder Map: same screen, dimension buttons in the title bar
    st = _open_map(s, 'simplemaps:mega_wayfinder_map', 'mega')
    if st:
        time.sleep(2.0)
        s.shot('mega-open')
    s.close()


def _write_png(path, size=256):
    from PIL import Image, ImageDraw
    im = Image.new('RGB', (size, size))
    d = ImageDraw.Draw(im)
    for yy in range(size):
        d.line([(0, yy), (size, yy)], fill=(int(255 * yy / size), 120, 255 - int(255 * yy / size)))
    d.ellipse([size // 4, size // 4, 3 * size // 4, 3 * size // 4], fill=(250, 220, 40), outline=(0, 0, 0), width=6)
    d.rectangle([8, 8, size // 3, size // 3], fill=(20, 160, 60))
    d.text((size // 2 - 24, size - 30), 'N125', fill=(255, 255, 255))
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


def maps_picture(s):
    _prep(s, 'survival')
    x, y, z = _move(s, 10)
    name = 'aaa_n125_probe.png'
    _write_png(RUN / 'config/simplemaps/images' / name)
    s.chat('item replace entity @s inventory.26 with minecraft:map 4', 0.4)
    s.chat('simplemaps picture', 1.5)
    st = _wait_screen(s, 'PictureMapScreen', 5)
    s.check('/simplemaps picture opens the picture map dialog', st is not None, s.last.get('screen'))
    if not st:
        _screen_route(s, 'com.simplemaps.client.PictureMapScreen')
        st = _wait_screen(s, 'PictureMapScreen', 5)
        s.check('picture map dialog opened through the probe route (command route failed above)', st is not None, s.last.get('screen'))
        if not st:
            return
    title = rp.plain(st.get('title', ''))
    s.check('picture: title "Picture Map"', 'Picture Map' in title, title)
    for _ in range(12):   # the file button cycles through the folder's PNGs
        w = s.find_widget('File: ', None, s.st())
        if not w or name in rp.plain(w['label']):
            break
        s.click_xy(w['x'], w['y'])
        time.sleep(0.6)
    w = s.find_widget('File: ', None, s.st())
    s.check(f'picture: File label shows {name}', bool(w) and name in rp.plain(w['label']), w and w['label'])
    size = s.find_widget('Size: ', None, s.last)
    s.check('picture: Size label "Size: 2 map(s)" (default: 2 maps on the longer edge = 2 x 2)', bool(size) and '2 map' in rp.plain(size['label']),
            size and size['label'])
    make = s.find_widget('Make maps', None, s.last)
    s.check('picture: "Make maps" is active (picture loaded and converted)', bool(make) and make.get('active'), make)
    time.sleep(1.0)
    s.shot('dialog')
    if not make:
        return
    s.click_xy(make['x'], make['y'])
    time.sleep(3.0)
    s.check('picture: the dialog closes after "Make maps"', s.screen() == 'none', s.last.get('screen'))
    tags = []
    for i in range(4):
        _say(s, f'if items entity @s hotbar.{i} minecraft:filled_map', f'pic-map-{i}')
        tags.append(f'pic-map-{i}')
    _say(s, 'unless items entity @s container.* minecraft:map', 'pic-empty-used')
    tags.append('pic-empty-used')
    _check_said(s, 'picture: REAL click "Make maps" gives 4 filled maps (hotbar 1-4) and uses up the 4 empty maps (survival)', tags)
    # 2 x 2 wall of item frames: looking south, +x is on the left -> column c at x+1-c, row r at y+2-r
    wz = z + 3
    s.chat(f'fill {x - 2} {y} {wz} {x + 3} {y + 3} {wz} minecraft:smooth_stone', 0.4)
    s.chat(f'fill {x - 2} {y} {wz - 1} {x + 3} {y + 3} {wz - 1} minecraft:air', 0.4)
    order = {(0, 0): 0, (1, 0): 1, (0, 1): 2, (1, 1): 3}   # payload order row by row -> inventory order
    tags = []
    for (c, r), slot in order.items():
        fx, fy = x + 1 - c, y + 2 - r
        s.chat(f'summon minecraft:item_frame {fx} {fy} {wz - 1} {{Facing:2b,Fixed:1b,Tags:["n125pic","n125pic{c}{r}"]}}', 0.4)
        s.chat(f'item replace entity @e[type=minecraft:item_frame,tag=n125pic{c}{r},limit=1] contents from entity @s hotbar.{slot}', 0.4)
        _say(s, f'if entity @e[type=minecraft:item_frame,tag=n125pic{c}{r},nbt={{Item:{{id:"minecraft:filled_map"}}}}]', f'pic-frame-{c}{r}')
        tags.append(f'pic-frame-{c}{r}')
    _check_said(s, 'picture: the four maps hang in the 2 x 2 frame wall', tags)
    s.chat(f'tp @s {x + 1.0} {y} {z - 1.5} facing {x + 1.0} {y + 2.0 - 1.62:.2f} {wz}', 1.0)
    time.sleep(2.0)
    s.shot('wall')
    s.chat('kill @e[type=minecraft:item_frame,tag=n125pic]', 0.4)
    s.chat('gamemode creative @s', 0.3)


def _screen_route(s, cls):
    sp = _load('sort_probe_n125', 'modules/simpleinterfaces/tools/sort_probe.py')
    sp.command('screen', value=cls)
    time.sleep(1.5)


def maps_web(s):
    _prep(s)
    x, y, z = _move(s, 10)
    s.key('1', 0.4)
    s.chat('item replace entity @s hotbar.0 with simplemaps:wayfinder_map', 0.4)
    _walk(s, x, y, z, [(0, 0), (48, 0), (48, 48), (0, 48), (0, 0)], 2.0)
    srv = cdp = None
    try:
        s.chat('simplemaps config set webEnabled true', 1.5)
        _mark(s, 'webexport')
        s.chat('simplemaps web export full', 1.5)
        m = None
        for _ in range(10):
            time.sleep(3.0)
            _mark(s, 'webstatus')
            s.chat('simplemaps web status', 1.5)
            m = _after(s, 'webstatus', r'Last web map export .*?: (\d+) tiles in (\d+) dimensions, folder (.+?)\.?\s*$')
            if m:
                break
        s.check('web: /simplemaps web status reports a finished export with tiles', bool(m) and int(m.group(1)) > 0,
                m.group(0)[-160:] if m else 'no "Last web map export" line')
        if not m:
            return
        folder = Path(m.group(3).strip())
        if not folder.is_absolute():
            folder = (RUN / folder).resolve()
        markers = folder / 'markers.json'
        try:
            meta = json.loads(markers.read_text())
        except (OSError, ValueError) as e:
            s.check('web: markers.json readable', False, f'{type(e).__name__} {markers}')
            return
        dims = meta.get('dimensions') or []
        over = [d for d in dims if 'overworld' in str(d.get('id', ''))]
        s.check('web: markers.json lists the Overworld with tiles', bool(over) and over[0].get('tiles', 0) > 0, json.dumps(dims)[:300])
        s.check('web: the viewer page index.html was exported', (folder / 'index.html').is_file(), str(folder)[-120:])
        chrome = _chromium()
        s.check('web: headless Chromium found', chrome is not None, chrome or 'none')
        if not chrome:
            return
        srv, port = _serve(folder)
        cdp = Cdp(chrome, s.out / 'chrome-n125-web')
        cdp.goto(f'http://127.0.0.1:{port}/index.html', 5.0)
        info = cdp.js('JSON.stringify({dims: meta ? (meta.dimensions || []).length : -1, dim: dim ? dim.id : null, cached: cache.size, '
                      'info: document.getElementById("info").textContent})')
        info = json.loads(info or '{}')
        s.check('web: the exported viewer reads markers.json and loads tiles in the browser', info.get('dims', 0) > 0 and info.get('cached', 0) > 0, info)
        size = cdp.shot(_shot_path(s, 'browser-viewer'))
        s.check('web: browser shot of the exported web map', size > 10_000, size)
    finally:
        if cdp:
            cdp.close()
        if srv:
            srv.shutdown()
        s.chat('simplemaps config reset webEnabled', 1.0)


# ---------------------------------------------------------------- Simple Livemap
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


def livemap_chat(s):
    _prep(s)
    s.chat('simplelivemap link', 1.5)
    log = _log(s)
    s.check('livemap: /simplelivemap link answered with the web map address', bool(re.search(r'(Private|Public) web map.*: http://', log)),
            'line found' if re.search(r'web map.*: http://', log) else 'no link line in the log')
    st = s.key('t', 1.0)
    st = _wait_screen(s, 'ChatScreen', 4) or st
    s.check('chat opened with T', 'ChatScreen' in st.get('screen', ''), st.get('screen'))
    time.sleep(0.5)
    s.shot('chat-link')
    scale = st.get('scale', 2) or 2
    gui_h = 720 // scale
    opened = None
    # newest chat line: text centre at (gui height - 40) - 4, older lines 9 gui px higher; text starts at x 4 (ChatComponent)
    for line in range(3):
        cur = s.st()
        if 'ChatScreen' not in cur.get('screen', ''):
            break
        yy = (gui_h - 40 - 4 - 9 * line) * scale
        s.click_xy(20 * scale, yy, cur)
        st = _wait_screen(s, 'ConfirmLinkScreen', 2.5)
        if st:
            opened = line
            break
    s.check('livemap: REAL click on the link line opens Vanilla\'s link confirmation (ConfirmLinkScreen)', opened is not None,
            f'line {opened}' if opened is not None else s.last.get('screen'))
    if opened is not None:
        s.shot('confirm-link')
        s.key('Escape', 0.8)
    s.close()


def livemap_browser(s):
    _prep(s)
    x, y, z = _pos(s)
    # loaded chunks around the player are drawn (render.chunksPerTick 2 per tick): a short walk, then time for the tiles
    _walk(s, x, y, z, [(0, 0), (48, 0), (48, 48), (0, 48), (0, 0)], 2.5)
    time.sleep(8.0)
    port, token = _web(RUN)
    s.check('livemap: web map config readable', port is not None, f'port {port}')
    if port is None:
        return
    chrome = _chromium()
    s.check('livemap: headless Chromium found', chrome is not None, chrome or 'none')
    if not chrome:
        return
    cdp = None
    try:
        cdp = Cdp(chrome, s.out / 'chrome-n125-livemap')
        # start position #x=&z=&s= (N106): the viewer opens at 0/0 otherwise, far from the probe's area
        cdp.goto(f'http://127.0.0.1:{port}/#' + (f't={token}&' if token else '') + f'x={x + 24}&z={z + 24}&s=2', 6.0)
        state = lambda: json.loads(cdp.js('JSON.stringify({x: view.x, z: view.z, s: view.s, style: OPTS.style, dims: dims.length, '
                                          'tiles: tiles.size, denied: denied, panel: panel.classList.contains("open")})') or '{}')
        st0 = state()
        s.check('livemap: the page read live.json (dimensions) and drew tiles', st0.get('dims', 0) > 0 and st0.get('tiles', 0) > 0 and not st0.get('denied'),
                {k: st0.get(k) for k in ('dims', 'tiles', 'denied', 's')})
        s.check('livemap: start position from #x=&z=&s= (N106)', abs(st0.get('x', 0) - (x + 24)) < 2 and abs(st0.get('z', 0) - (z + 24)) < 2,
                {k: st0.get(k) for k in ('x', 'z', 's')})
        cdp.shot(_shot_path(s, 'style-blocks'))
        cx, cy = 640, 460
        cdp.drag(cx, cy, cx + 200, cy, 10)
        st1 = state()
        want = st0['x'] - 200 / st0['s']
        s.check('livemap: DRAG 200 px to the right moves the view 200/scale blocks west', abs(st1['x'] - want) < 1.5 and abs(st1['z'] - st0['z']) < 1.5,
                f"x {st0['x']:.1f} -> {st1['x']:.1f} (want {want:.1f}), z {st0['z']:.1f} -> {st1['z']:.1f}")
        cdp.wheel(cx, cy, -100)
        st2 = state()
        s.check('livemap: WHEEL up zooms in x1.25', abs(st2['s'] - st1['s'] * 1.25) < 1e-6 or (st1['s'] >= 8 and st2['s'] == 8), f"s {st1['s']} -> {st2['s']}")
        cdp.click_el('#zout')
        st3 = state()
        cdp.click_el('#zin')
        st4 = state()
        s.check('livemap: "-" halves and "+" doubles the scale', abs(st3['s'] - st2['s'] / 2) < 1e-6 and abs(st4['s'] - st3['s'] * 2) < 1e-6,
                f"s {st2['s']} -> {st3['s']} -> {st4['s']}")
        cdp.click_el('#gear')
        s.check('livemap: the gear opens "View settings"', state().get('panel') is True, 'panel open' if state().get('panel') else 'closed')
        shots = {}
        for style in ('shaded', 'paper', 'blocks'):
            cdp.click_el(f'#styles input[value="{style}"]')
            time.sleep(2.5)
            got = state().get('style')
            s.check(f'livemap: style "{style}" chosen with a click', got == style, got)
            shots[style] = _shot_path(s, f'style-{style}')
            cdp.shot(shots[style])
        changed, rms = _differs(shots['blocks'], shots['paper'], 5.0)
        s.check('livemap: the Cartographer style looks different from Blocks (paper background)', changed, f'rms {rms}')
        changed, rms = _differs(shots['blocks'], shots['shaded'], 0.5)
        s.check('livemap: Shaded relief differs from Blocks', changed, f'rms {rms}')
    finally:
        if cdp:
            cdp.close()


# ---------------------------------------------------------------- Simple Shops
def shops_windows(s):
    s.chat('gamemode creative @s', 0.5)
    shops.owner_buyer(s)


def shops_terminal(s):
    s.chat('gamemode creative @s', 0.5)
    shops.terminal(s)


def _place(s, slot_key, gx, gy, gz, want, tag):
    """Selected hotbar slot `slot_key`, crosshair on the top face of the ground block, REAL right click; reads the block back."""
    s.key(slot_key, 0.4)
    if not _aim(s, gx + 0.5, gy - 0.1, gz + 0.5, 'minecraft:grass_block', f'{tag} ground'):
        return False
    s.io.right_click()
    time.sleep(0.8)
    _say(s, f'if block {gx} {gy} {gz} {want}', f'{tag}-placed')
    return _check_said(s, f'{tag}: REAL right click placed {want}', [f'{tag}-placed'])


def shops_case(s):
    _prep(s)
    x, y, z = _move(s, 10)
    s.chat(f'fill {x - 3} {y} {z - 3} {x + 4} {y + 3} {z + 4} minecraft:air', 0.4)
    s.chat('item replace entity @s hotbar.0 with simpleshops:display_case', 0.3)
    s.chat('item replace entity @s hotbar.1 with simpleshops:glow_display_case', 0.3)
    s.chat('item replace entity @s hotbar.2 with minecraft:diamond 8', 0.3)
    s.chat('item replace entity @s hotbar.3 with minecraft:golden_apple 2', 0.3)
    s.chat('item replace entity @s hotbar.4 with minecraft:red_dye 1', 0.3)
    case, glow = (x + 2, y, z), (x + 2, y, z + 2)
    if not _place(s, '1', *case, 'simpleshops:display_case', 'case'):
        return
    if not _place(s, '2', *glow, 'simpleshops:glow_display_case', 'glow case'):
        return
    for key, pos, item, block, tag in (('3', case, 'minecraft:diamond', 'simpleshops:display_case', 'case'),
                                       ('4', glow, 'minecraft:golden_apple', 'simpleshops:glow_display_case', 'glow case')):
        s.key(key, 0.4)
        if not _aim(s, pos[0] + 0.5, pos[1] + 0.5, pos[2] + 0.5, block, f'{tag} stock'):
            continue
        s.io.right_click()
        time.sleep(0.8)
        _say(s, f'unless items entity @s weapon.mainhand {item}', f'{tag}-hand-empty')
        _check_said(s, f'{tag}: REAL right click with {item} in hand quick-stocks the free case (hand empty)', [f'{tag}-hand-empty'])
        shops.right_click_empty(s, (pos[0] + 0.5, pos[1] + 0.5, pos[2] + 0.5))
        st = _wait_screen(s, 'ShopScreens$Stock', 6)
        s.check(f'{tag}: empty-hand right click opens the case\'s stock window (ShopScreens$Stock)', st is not None, s.last.get('screen'))
        if st:
            goods = [sl['stack'] for sl in st.get('slots', []) if not sl['own'] and sl['stack']]
            s.check(f'{tag}: the stock window shows the stocked {item}', any(g.startswith(item) for g in goods), goods[:6])
            s.shot(f'{tag.replace(" ", "-")}-stock')
        s.close()
    # REAL Shift (>= 0.8 s) + right click with red dye: the owner tints glass and frame
    s.key('5', 0.4)
    if _aim(s, case[0] + 0.5, case[1] + 0.5, case[2] + 0.5, 'simpleshops:display_case', 'case tint'):
        _sneak_click(s)
        _say(s, f'if block {case[0]} {case[1]} {case[2]} simpleshops:display_case[tint=red]', 'case-tint-red')
        _check_said(s, 'case: Shift + right click with red dye tints the case (tint=red)', ['case-tint-red'])
    s.chat(f'tp @s {x - 1.5} {y} {z + 1.0} facing {x + 2.5} {y + 0.5 - 1.62:.2f} {z + 1.5}', 1.0)
    time.sleep(1.5)
    s.shot('cases-day')
    s.chat('effect clear @s night_vision', 0.3)
    s.chat('time set midnight', 1.0)
    time.sleep(1.5)
    s.shot('cases-night')
    s.chat('time set noon', 0.3)


def shops_music(s):
    _prep(s)
    s.chat('gamerule random_tick_speed 0', 0.3)
    try:
        s.chat('tp @s ~ ~ ~10', 1.0)
        shop = shops.build_shop(s)
        if not shop:
            return
        sx, sy, sz = shop
        centre = (sx + 0.5, sy + 0.5, sz + 0.5)
        s.chat('item replace entity @s inventory.0 with minecraft:music_disc_cat', 0.3)
        shops.right_click_empty(s, centre)
        st = _wait_screen(s, 'ShopScreens$Owner', 8)
        s.check('music: empty-hand right click opens the owner screen', st is not None, s.last.get('screen'))
        if not st:
            return
        shops.click_own(s, 'minecraft:music_disc_cat x1')
        s.click_slot(index=37)
        st, got = shops.wait_stack(s, 37, 'minecraft:music_disc_cat x1')
        s.check('music: the disc slot takes the disc (real clicks)', got == 'minecraft:music_disc_cat x1', got)
        s.shot('owner-disc')
        s.close()
        _say(s, f'if block {sx} {sy} {sz} simpleshops:sales_block{{disc:{{id:"minecraft:music_disc_cat"}}}}', 'music-disc-saved')
        _check_said(s, 'music: the sales block holds the disc (server data)', ['music-disc-saved'])
        x, y, z = _pos(s)
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {centre[0]:.2f} {sy + 1.6 - 1.62:.2f} {centre[2]:.2f}', 1.0)
        time.sleep(4.0)   # the client rescans every few ticks and starts the music; notes rise above the playing shop
        s.shot('music-notes-1')
        time.sleep(1.5)
        s.shot('music-notes-2')
    finally:
        s.chat('gamerule random_tick_speed 3', 0.3)


SCENARIOS = {
    'n125-maps-config': maps_config,
    'n125-maps-wayfinder': maps_wayfinder,
    'n125-maps-picture': maps_picture,
    'n125-maps-web': maps_web,
    'n125-livemap-chat': livemap_chat,
    'n125-livemap-browser': livemap_browser,
    'n125-shops-windows': shops_windows,
    'n125-shops-terminal': shops_terminal,
    'n125-shops-case': shops_case,
    'n125-shops-music': shops_music,
}
