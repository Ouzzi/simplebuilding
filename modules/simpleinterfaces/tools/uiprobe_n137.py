"""N137 release polish, mods 57-60 (offen-vor-release.md section 3: Simple Optimization, Simple Opt Render, Simple Opt Memory, Simple
Opt Server). None of the four has a "Szenen" line (FPS on real graphics stays with the owner, W4C-03/W4C-04); the scenes cover what
N137 changed and what only the running game shows:

All four
  n137-config-screens  SimpleLib's generic config screen of each mod (Mod Menu route through the probe 'config' command): every entry
                       name is an English lang text (Opt Render/Memory/Server showed raw paths like "leaf_culling.mode" before N137),
                       every option has a tooltip without raw keys and without a literal "\\n" (the Opt Server activation-range tooltip
                       printed "\\n" before N137); the screen title is the lang title; Opt Render "Leaf culling depth" names
                       "Default: 0.", Opt Server "Target MSPT" names "Default: 45.". Shot per screen + tooltip under the real pointer.
Opt Render / Opt Memory / Opt Server
  n137-opt-runtime     The startup decision tables ("simpleoptrender areas:", "simpleoptmemory areas:", "simpleoptserver areas:") are
                       in client.log with every area (6 + 1 + 2 lines; NOTE prints each line - a rival mod in the probe pack may turn
                       an area off, that is the step-back rule, not a failure). '/simpleoptserver status' lists the areas, activation
                       range "off" (default mode off). '/simpleclient simpleoptrender get areas.leaf_culling.depth' answers 0.
                       A 7x7x7 cube of persistent oak leaves is built 4 blocks ahead: shot from outside (the outside keeps its fancy
                       cutout look, no hole) and one from inside the cube (depth 0: inner faces left out). Look = human; FPS under
                       Xvfb is software GL and not a measurement (NOTE).

Sounds: none of the four has own sound events - no listening page.

Patterns (uiprobe_n122-n135 lessons): position via int(float()) of status 'pos'; difficulty normal in _prep is not needed (no mobs);
chat commands ~1-1.5 s each, output read from client.log after a 'say' mark. Comparisons/assumptions are NOTE prints. No game code
is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe137 --scale 2 \\
        --only n137-config-screens,n137-opt-runtime
"""
import json
import re
import time
from pathlib import Path

TAG = 'N137'
FLOOR = 'minecraft:smooth_stone'
REPO = Path(__file__).resolve().parents[3]


# ---------------------------------------------------------------- helpers
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _log(s):
    p = s.out / 'client.log'
    return p.read_text(errors='replace') if p.exists() else ''


def _mark(s, mark):
    s.chat(f'say {TAG} MARK {mark}', 0.4)


def _log_since(s, mark):
    """client.log after the last line ending with '<TAG> MARK <mark>'."""
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} MARK {mark}')), default=-1)
    return '\n'.join(lines[idx + 1:]) if idx >= 0 else ''


def _prep(s, mode='creative'):
    s.close()
    for k in ('Shift_L', 'space', 'w'):
        s.io.key_up(k)
    s.chat('ride @s dismount', 0.3)
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true', 'random_tick_speed 0'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('effect clear @s', 0.3)
    s.chat('clear @s', 0.3)


def _center_pointer(s):
    st = s.st()
    if 'windowX' in st:
        s.io.move(st['windowX'] + 640, st['windowY'] + 360)
        time.sleep(0.3)


def _done(s):
    s.close()
    _center_pointer(s)
    s.chat('gamerule random_tick_speed 3', 0.3)
    s.chat('gamemode creative @s', 0.3)


def _plain(text):
    return re.sub('§.', '', text or '').strip()


def _lang(mod, loc='en_us'):
    return json.loads((REPO / f'modules/{mod}/shared/resources/assets/{mod}/lang/{loc}.json').read_text(encoding='utf-8'))


# ---------------------------------------------------------------- config screens (all four)
SCREENS = (
    # mod, at least n options, must show, (tooltip of, must contain)
    ('simpleoptimization', 3, 'Enable Simple Opt Render', ('Enable Simple Opt Render', 'Default: On.')),
    ('simpleoptrender', 13, 'Leaf culling depth', ('Leaf culling depth', 'Default: 0.')),
    ('simpleoptmemory', 2, 'Block-state tables mode', ('Block-state tables mode', 'FerriteCore')),
    ('simpleoptserver', 10, 'Target MSPT', ('Target MSPT', 'Default: 45.')),
)


def config_screens(s):
    _prep(s)
    for mod, least, must, tip in SCREENS:
        lang = _lang(mod)
        names = {_plain(v) for k, v in lang.items() if k.startswith(f'text.autoconfig.{mod}.option.') and not k.endswith('@Tooltip')}
        cats = {_plain(v) for k, v in lang.items() if f'text.autoconfig.{mod}.category.' in k or k == f'text.autoconfig.{mod}.title'}
        s.close()
        st = s.open_config(mod, 2.5)
        screen = st.get('screen', 'none')
        if screen == 'none':
            s.check(f'{mod}: config screen opens', False, screen)
            continue
        time.sleep(0.8)
        st = s.st()

        def visible(st):
            # Cloth Config keeps invisible internal entries labelled with a UUID: not shown, not options
            return [e for e in st.get('entries', []) if _plain(e.get('label')).strip()
                    and not re.fullmatch(r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}', _plain(e.get('label')).strip())]

        # the options of each group sit on their own Cloth tab ("General" holds conflictScan, "Areas" the area modes):
        # read every tab, not only the open one
        entries = visible(st)
        for tab in [w for w in s.widgets(st) if 'Tab' in w['class'] and w.get('label')]:
            s.click_xy(tab['x'], tab['y'], st)
            time.sleep(0.6)
            for e in visible(s.st()):
                if _plain(e['label']) not in {_plain(x['label']) for x in entries}:
                    entries.append(e)
        st = s.st()
        shown = [_plain(e['label']) for e in entries]
        options = [n for n in shown if n not in cats]
        bad = [n for n in options if n not in names]
        no_tip = [n for e, n in zip(entries, shown) if n in names and not [x for x in e.get('tooltip', []) if _plain(x)]]
        raw_tip = [f'{n}: {x}' for e, n in zip(entries, shown) for x in e.get('tooltip', [])
                   if 'autoconfig' in x or '@Tooltip' in x or '\\n' in x or re.fullmatch(r'[a-z0-9_.]+\.[a-z0-9_.]+', _plain(x))]
        s.check(f'{mod}: config screen lists at least {least} options', len(options) >= least, f'{screen}: {shown}')
        s.check(f'{mod}: every entry name is an English lang text (no raw path)', options and not bad, bad)
        s.check(f'{mod}: every option has a tooltip, none is a raw key or shows a literal \\n', options and not no_tip and not raw_tip,
                (no_tip, raw_tip[:4]))
        title = _plain(st.get('title', ''))
        s.check(f'{mod}: screen title is the lang title, not a raw key', title and 'autoconfig' not in title, title)
        s.check(f'{mod}: shows "{must}"', must in shown, shown)
        e = next((e for e, n in zip(entries, shown) if n == tip[0]), None)
        text = ' '.join(_plain(x) for x in (e or {}).get('tooltip', []))
        s.check(f'{mod}: tooltip of "{tip[0]}" names "{tip[1]}"', tip[1] in text, text)
        if mod == 'simpleoptserver':
            e = next((e for e, n in zip(entries, shown) if n == 'Activation range mode'), None)
            lines = [_plain(x) for x in (e or {}).get('tooltip', [])]
            s.check('simpleoptserver: "Activation range mode" tooltip on separate lines, no literal \\n',
                    lines and not any('\\n' in x for x in lines) and any('Side effects' in x for x in lines), lines)
        s.shot(f'{mod}')
        s.io.move(*s.abs(st, 380, 120))
        time.sleep(1.2)
        s.shot(f'{mod}-tooltip')
        _center_pointer(s)
    s.close()


# ---------------------------------------------------------------- runtime: decision tables, status, client command, leaves
AREAS = {
    'simpleoptrender': ['leaf_culling', 'lightmap_cache', 'fast_ip_ping', 'no_telemetry', 'more_culling', 'details'],
    'simpleoptmemory': ['state_tables'],
    'simpleoptserver': ['dynamic_distance', 'activation_range'],
}


def opt_runtime(s):
    _prep(s)
    log = _log(s)
    for owner, areas in AREAS.items():
        idx = log.rfind(f'{owner} areas:')
        block = log[idx:idx + 2500].splitlines()[1:1 + len(areas)] if idx >= 0 else []
        seen = [a for a in areas if any(re.search(r'\b' + a + r'\s+mode=', ln) for ln in block)]
        s.check(f'{owner}: startup decision table names every area ({len(areas)})', idx >= 0 and len(seen) == len(areas),
                f'seen {seen}' if idx >= 0 else 'no table in client.log')
        for ln in block:
            print(f'NOTE n137 {owner}: {ln.strip()}', flush=True)
    m = re.search(r'O4 state tables: (\d+) of (\d+) arrays shared, about (\d+) KiB saved', log)
    if not m and re.search(r'state_tables\s+mode=\S+\s+-> off', log):
        print('NOTE n137 state_tables is off in this pack (step-back rule), no O4 line expected', flush=True)
    else:
        s.check('Opt Memory logs the shared block-state tables (O4) on the client', m and int(m.group(1)) > 0, m.group(0) if m else 'no O4 line')

    _mark(s, 'status')
    s.chat('simpleoptserver status', 0.5)
    time.sleep(1.5)
    text = _log_since(s, 'status')
    s.check('/simpleoptserver status lists the server areas', 'dynamic_distance' in text and 'activation_range' in text, text[-400:])
    s.check('/simpleoptserver status: activation range off by default', re.search(r'activation_range: off\b', text), text[-400:])
    s.check('/simpleoptserver status lists the render and memory areas too', 'leaf_culling' in text and 'state_tables' in text, text[-400:])

    _mark(s, 'depth')
    s.chat('simpleclient simpleoptrender get areas.leaf_culling.depth', 0.5)
    time.sleep(1.5)
    text = _log_since(s, 'depth')
    s.check('/simpleclient simpleoptrender get areas.leaf_culling.depth answers 0', re.search(r'depth\D{0,40}\b0\b', text), text[-300:])

    x, y, z = _pos(s)
    z0 = z + 4
    try:
        s.chat(f'fill {x - 6} {y} {z0 - 1} {x + 6} {y + 9} {z0 + 9} minecraft:air', 0.6)
        s.chat(f'fill {x - 6} {y - 1} {z - 2} {x + 6} {y - 1} {z0 + 9} {FLOOR}', 0.6)
        s.chat(f'fill {x - 3} {y} {z0} {x + 3} {y + 6} {z0 + 6} minecraft:oak_leaves[persistent=true]', 0.8)
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 10', 1.5)
        time.sleep(2.0)
        s.shot('leaves-outside')
        s.chat(f'tp @s {x + 0.5} {y + 2} {z0 + 3.5} 0 0', 1.5)
        time.sleep(2.0)
        s.shot('leaves-inside')
        print('NOTE n137 leaf cube shots: outside keeps the cutout look (no hole), inside shows the faces left out (human); '
              'FPS under Xvfb is software GL, real FPS stays with the owner (W4C-03/W4C-04)', flush=True)
    finally:
        s.chat(f'fill {x - 3} {y} {z0} {x + 3} {y + 6} {z0 + 6} minecraft:air', 0.6)
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.2)
        _done(s)


SCENARIOS = {
    'n137-config-screens': config_screens,
    'n137-opt-runtime': opt_runtime,
}
