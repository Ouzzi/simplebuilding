"""Simple Maps context menu and waypoint slot picking with the REAL operating-system mouse (prio27 #7).

The client smoke's "context-menu"/"pick-slot" shots showed no menu (it passed raw button numbers; 26.3 counts like SDL). This probe
proves the real path: X11 pointer through XTest (Pointer of Simple Earth's menu_probe.py) -> SDL -> MouseHandler -> WayfinderScreen.
World, chat commands and screenshots come from the inert-unless-requested SortProbe of Simple Interfaces (integration client).

Usage (inside xvfb-run, from the repository root):
    python3.12 modules/simplemaps/tools/maps_menu_probe.py --out /var/tmp/sb/mapsmenu
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

from PIL import Image, ImageChops, ImageStat

REPO = Path(__file__).resolve().parents[3]
_spec = importlib.util.spec_from_file_location('sort_probe', REPO / 'modules/simpleinterfaces/tools/sort_probe.py')
sp = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(sp)
RUN = sp.RUN

# WayfinderLayout (map in hand), plain maths copied from modules/simplemaps/shared/java/com/simplemaps/WayfinderLayout.java.
CELL, FRAME, TITLE_H, FOOTER_H, MARGIN = 16, 8, 12, 12, 4
TAB_MIN, TAB_MAX, TAB_GAP, TAB_HOVER = 12, 32, 2, 2
MIN_COLS, MIN_ROWS, LEFT_TABS, RIGHT_TABS = 8, 6, 8, 9


def layout(width, height):
    rows = max(MIN_ROWS, (height - 2 * MARGIN - TITLE_H - FOOTER_H - 2 * FRAME) // CELL)
    panel_h = TITLE_H + rows * CELL + FOOTER_H + 2 * FRAME
    inner = panel_h - 2 * FRAME
    tab = max(TAB_MIN, min(TAB_MAX, (inner + TAB_GAP) // RIGHT_TABS - TAB_GAP))
    sides = 2 * (tab + TAB_HOVER)
    cols = max(MIN_COLS, (width - 2 * MARGIN - sides - 2 * FRAME) // CELL)
    panel_w = cols * CELL + 2 * FRAME
    left = (width - (panel_w + sides)) // 2 + tab + TAB_HOVER
    top = (height - panel_h) // 2
    map_x, map_y, map_w, map_h = left + FRAME, top + FRAME + TITLE_H, cols * CELL, rows * CELL

    def right_tab(i):
        y = top + FRAME + (inner - (RIGHT_TABS * tab + (RIGHT_TABS - 1) * TAB_GAP)) // 2 + i * (tab + TAB_GAP)
        return left + panel_w + tab // 2, y + tab // 2

    return dict(cx=map_x + map_w // 2, cy=map_y + map_h // 2, panel=(left, top, left + panel_w, top + panel_h), tab=tab, right_tab=right_tab)


def diff(a, b, box):
    """Mean absolute RGB difference of two screenshots inside box (framebuffer pixels)."""
    ia, ib = Image.open(a).convert('RGB').crop(box), Image.open(b).convert('RGB').crop(box)
    return sum(ImageStat.Stat(ImageChops.difference(ia, ib)).mean) / 3


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', required=True)
    ap.add_argument('--scale', type=int, default=2)
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    RUN.mkdir(parents=True, exist_ok=True)
    for f in (sp.STATUS, sp.COMMAND):
        (RUN / f).unlink(missing_ok=True)
    (RUN / sp.REQUEST).write_text(json.dumps({'width': 1280, 'height': 720, 'guiScale': args.scale}))
    started = time.time()
    log = open(out / 'client.log', 'w')
    game = subprocess.Popen(['./gradlew', ':integration:runIntegrationClient'], cwd=REPO, stdout=log, stderr=subprocess.STDOUT,
                            start_new_session=True)
    results, ok = [], True
    shots_dir = RUN / 'screenshots'

    def check(name, good, detail=''):
        nonlocal ok
        ok &= bool(good)
        results.append({'check': name, 'ok': bool(good), 'detail': str(detail)[:600]})
        print(('OK   ' if good else 'FAIL ') + name + ': ' + str(detail)[:400], flush=True)

    def shot(name):
        known = set(shots_dir.glob('*.png')) if shots_dir.exists() else set()
        sp.command('shot')
        for _ in range(50):
            new = [p for p in shots_dir.glob('*.png') if p not in known] if shots_dir.exists() else []
            if new:
                time.sleep(0.5)
                dst = out / f'maps-{name}.png'
                shutil.copy(new[0], dst)
                return dst
            time.sleep(0.1)
        raise SystemExit(f'screenshot {name} not written')

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
        io = sp.Input()
        time.sleep(3)

        def chat(c, wait=0.8):
            sp.command('cmd', value=c)
            time.sleep(wait)
            return sp.fresh()

        st = sp.fresh()
        wx, wy, s = st['windowX'], st['windowY'], st['scale']
        io.move(wx + 640, wy + 360)
        chat('gamemode survival @s')
        chat('time set day')
        chat('item replace entity @s weapon.mainhand with simplemaps:wayfinder_map', 4)  # the map gets its id while held
        io.right_click()
        time.sleep(2.5)
        st = sp.fresh()
        check('real right click with the map opens the wayfinder screen', 'WayfinderScreen' in st['screen'], st['screen'])
        base = shot('0-open')
        w, h = Image.open(base).size
        gw, gh = -(-w // s), -(-h // s)
        L = layout(gw, gh)
        px = lambda gx, gy: (wx + gx * s, wy + gy * s)
        box = lambda x0, y0, x1, y1: (x0 * s, y0 * s, x1 * s, y1 * s)
        print(f'framebuffer {w}x{h}, gui {gw}x{gh}, map centre {L["cx"]},{L["cy"]}, tab {L["tab"]}', flush=True)

        # 1. right click on the map -> context menu "Create waypoint / Center here" at the cursor
        mx, my = L['cx'] + 20, L['cy'] + 10
        io.move(*px(mx, my))
        time.sleep(0.4)
        io.right_click()
        menu = shot('1-context-menu')
        d = diff(base, menu, box(mx, my, mx + 70, my + 30))
        check('(1) real right click on the map opens the context menu (pixels under the cursor change)', d > 25, f'mean diff {d:.1f}')
        # 2. left click on the first row "Create waypoint" -> slot picking (panel greyed, hint text)
        io.click(*px(mx + 6, my + 6))
        pick = shot('2-pick-slot')
        d = diff(base, pick, box(*L['panel']))
        check('(2) real left click on "Create waypoint" starts slot picking (panel greyed)', d > 15, f'mean diff {d:.1f}')
        # 3. left click on right bookmark 1 (empty slot) -> waypoint created, picking ends
        tx, ty = L['right_tab'](1)
        io.click(*px(tx, ty))
        io.move(*px(L['cx'], L['panel'][1] + 4))
        time.sleep(0.8)
        done = shot('3-waypoint-created')
        t = L['tab'] // 2
        d_tab = diff(base, done, box(tx - t + 2, ty - t + 2, tx + t - 2, ty + t - 2))
        d_grey = diff(pick, done, box(*L['panel']))
        check('(3) real left click on bookmark 1 creates the waypoint (bookmark icon changes)', d_tab > 15, f'tab diff {d_tab:.1f}')
        check('(3) slot picking ended (panel no longer grey)', d_grey > 15, f'panel diff vs pick {d_grey:.1f}')
        # 4. right click on the new bookmark -> "Configure / Delete"
        io.move(*px(tx, ty))
        time.sleep(0.4)
        io.right_click()
        wpmenu = shot('4-waypoint-menu')
        d = diff(done, wpmenu, box(tx, ty, min(gw, tx + 70), min(gh, ty + 30)))
        check('(4) real right click on the bookmark opens its menu (configure/delete)', d > 15, f'mean diff {d:.1f}')
        io.key('Escape')
        io.key('Escape')
        sp.command('quit')
    finally:
        if game.poll() is None and not results:
            (RUN / sp.COMMAND).write_text(json.dumps({'op': 'quit'}))
        try:
            game.wait(timeout=120)
        except subprocess.TimeoutExpired:
            os.killpg(game.pid, 15)
        log.close()
        for f in (sp.REQUEST, sp.COMMAND):
            (RUN / f).unlink(missing_ok=True)
        ok = ok and bool(results)
        (out / 'results.json').write_text(json.dumps({'ok': ok, 'results': results}, indent=1))
    sys.exit(0 if ok else 1)


if __name__ == '__main__':
    main()
