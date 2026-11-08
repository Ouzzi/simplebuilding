"""Claim deed (WH26-9, b-deed14) with the REAL operating-system mouse and keyboard.

Runs the full integration client (`:integration:runIntegrationClient`) under xvfb-run and drives it through the probe of
Simple Building Tools (BtProbe: status, chat commands, screenshots) and XTest (Input of mouse_probe.py): claim a chunk
with a fresh deed, open the map, click the dimension tabs, the Rights tab and back, the Buy tab, trade 16 iron for
claim credits, then claim a glued and a separate chunk with "Claim here".

Usage (inside xvfb-run, from the repository root):
    xvfb-run -a -s '-screen 0 1920x1080x24' python3.12 modules/simpletweaks/tools/deed_probe.py --out /var/tmp/sb/deed14
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
_spec = importlib.util.spec_from_file_location('mouse_probe', REPO / 'modules/simplebuildingtools/tools/mouse_probe.py')
mp = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(mp)
RUN = mp.RUN


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', required=True)
    ap.add_argument('--scale', type=int, default=2)
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    RUN.mkdir(parents=True, exist_ok=True)
    for f in (mp.STATUS, mp.COMMAND):
        (RUN / f).unlink(missing_ok=True)
    (RUN / mp.REQUEST).write_text(json.dumps({'width': 1280, 'height': 720, 'guiScale': args.scale}))
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
            st = mp.status()
            if st and st.get('screen') == 'none' and 'pos' in st and st['tick'] > 5:
                break
            if game.poll() is not None:
                raise SystemExit('client ended before the world opened (see client.log)')
            time.sleep(1)
        else:
            raise SystemExit('world never opened')
        print(f'world open after {time.time() - started:.0f} s', flush=True)
        io = mp.Input()
        time.sleep(3)

        def chat(c, wait=0.8):
            mp.command('cmd', value=c)
            time.sleep(wait)
            return mp.fresh()

        def snap(name):
            mp.command('shot')
            time.sleep(1.2)
            results.append({'check': 'shot', 'ok': True, 'detail': name})

        def labels(st):
            return [(w['class'], w['label'], w['active']) for w in st['widgets'] if w['visible']]

        def find(st, label, cls=None):
            for w in st['widgets']:
                if w['visible'] and (cls is None or w['class'] == cls or (cls == 'Button' and w['class'] == 'Plain')) and (w['label'] or '') == label:
                    return w
            return None

        def click(label, cls=None, wait=0.8):
            st = mp.fresh()
            w = find(st, label, cls)
            if not w:
                check(f'widget "{label}" visible', False, labels(st))
                return st
            io.click(*mp.centre(st, w))
            time.sleep(wait)
            return mp.fresh()

        def screen(st):
            return st['screen'].rsplit('.', 1)[-1]

        chat('gamemode creative @s')
        chat('clear @s')
        st = mp.fresh()
        y = int(st['pos'].replace(',', ' ').split()[1])
        chat(f'tp @s 4008.5 {y} 4008.5 0 30', 2.0)
        st = mp.fresh()
        io.move(st['windowX'] + 640, st['windowY'] + 360)
        chat('give @s simpletweaks:claim_deed')
        io.key('1')

        # ---------- (1) a fresh deed claims the chunk, then shows the map
        io.right_click()
        st = mp.fresh()
        check('(1) right click with a fresh deed opens the claim sheet', screen(st) == 'DeedScreen', st['screen'])
        st = click('Claim', wait=1.5)
        check('(1) Claim closes the sheet', st['screen'] == 'none', st['screen'])
        io.right_click()
        time.sleep(1.0)
        st = mp.fresh()
        check('(1) right click again opens the map card', screen(st) == 'DeedMapScreen', st['screen'])
        snap('1-map')
        tabs = [w['label'] for w in st['widgets'] if w['class'] == 'SideTab' and w['visible']]
        check('(1) left tabs Map, Buy, Rights', all(t in tabs for t in ('Map', 'Buy', 'Rights')), tabs)
        check('(1) dimension tabs of the modpack (at least Overworld, Nether, End)', len(tabs) >= 6, tabs)
        claim = find(st, 'Claim here', 'Button')
        give = find(st, 'Give up...', 'Button')
        check('(1) Claim here is off on own land, Give up is on for the chosen chunk', claim is not None and not claim['active'] and give is not None and give['active'], labels(st))

        # ---------- (2) dimension tabs, Rights tab and back
        dims = [w for w in st['widgets'] if w['class'] == 'SideTab' and w['visible'] and w['label'] not in ('Map', 'Buy', 'Rights')]
        other = next((w for w in dims if 'nether' in w['label'].lower()), dims[1] if len(dims) > 1 else None)
        if other:
            st = click(other['label'], 'SideTab')
            check(f'(2) dimension tab "{other["label"]}" keeps the map open', screen(st) == 'DeedMapScreen', st['screen'])
            snap('2-other-dimension')
            st = click(dims[0]['label'], 'SideTab')
            check(f'(2) back to "{dims[0]["label"]}"', screen(st) == 'DeedMapScreen', st['screen'])
        st = click('Rights', 'SideTab', 1.2)
        check('(2) Rights tab opens the rights editor', screen(st) == 'DeedScreen', st['screen'])
        snap('2-rights')
        io.key('Escape', 1.0)
        st = mp.fresh()
        check('(2) Escape returns from the rights to the map', screen(st) == 'DeedMapScreen', st['screen'])

        # ---------- (3) Buy tab: trade 16 iron for 3 credits
        st = click('Buy', 'SideTab', 1.2)
        check('(3) Buy tab opens the trade screen', screen(st) == 'DeedTradeScreen', st['screen'])
        trade = find(st, 'Trade', 'Button')
        check('(3) Trade is off without the items', trade is not None and not trade['active'], labels(st))
        chat('give @s minecraft:iron_ingot 16', 1.5)
        st = mp.fresh()
        offer = next((w for w in st['widgets'] if w['visible'] and w['class'] == 'OfferRow' and 'Iron' in (w['label'] or '')), None)
        check('(3) the iron offer is listed', offer is not None, labels(st))
        if offer:
            io.click(*mp.centre(st, offer))
            time.sleep(0.8)
        st = mp.fresh()
        trade = find(st, 'Trade', 'Button')
        check('(3) Trade is on with 16 iron in the inventory', trade is not None and trade['active'], labels(st))
        snap('3-buy-before')
        st = click('Trade', 'Button', 1.5)
        check('(3) after the trade the buy tab stays open', screen(st) == 'DeedTradeScreen', st['screen'])
        trade = find(st, 'Trade', 'Button')
        check('(3) Trade is off again: the server took the 16 iron', trade is not None and not trade['active'], labels(st))
        snap('3-buy-after')
        io.key('Escape', 1.0)
        io.key('e', 1.5)
        st = mp.fresh()
        irons = [s for s in st['slots'] if s['item'] == 'minecraft:iron_ingot']
        check('(3) no iron left in the inventory (server side)', 'InventoryScreen' in st['screen'] and not irons, (st['screen'], irons))
        io.key('Escape', 1.0)
        chat('claim credits', 1.0)
        snap('3-credits-chat')

        # ---------- (4) Claim here: a glued and a separate chunk
        for name, x in (('glued', 4024.5), ('separate', 4072.5)):
            chat(f'tp @s {x} {y} 4008.5 0 30', 6.5)  # past the 100-tick land cooldown
            io.right_click()
            time.sleep(1.0)
            st = mp.fresh()
            claim = find(st, 'Claim here', 'Button')
            check(f'(4) {name}: Claim here is on in a free chunk with credits', screen(st) == 'DeedMapScreen' and claim is not None and claim['active'], labels(st))
            if claim and claim['active']:
                st = click('Claim here', 'Button', 1.5)
                check(f'(4) {name}: the map answers and stays open', screen(st) == 'DeedMapScreen', st['screen'])
                time.sleep(1.0)
                snap(f'4-{name}')
            io.key('Escape', 0.8)
        mp.command('quit')
    finally:
        if game.poll() is None:
            (RUN / mp.COMMAND).write_text(json.dumps({'op': 'quit'}))
        try:
            game.wait(timeout=120)
        except subprocess.TimeoutExpired:
            os.killpg(game.pid, 15)
        log.close()
        for f in (mp.REQUEST, mp.COMMAND):
            (RUN / f).unlink(missing_ok=True)
        shots = sorted(p for p in (RUN / 'screenshots').glob('*.png') if p.stat().st_mtime >= started)
        for i, p in enumerate(shots):
            shutil.copy(p, out / f'{i:02d}-{p.name}')
        ok = ok and any(r['check'] != 'shot' for r in results)
        (out / 'results.json').write_text(json.dumps({'ok': ok, 'results': results}, indent=1))
    sys.exit(0 if ok else 1)


if __name__ == '__main__':
    main()
