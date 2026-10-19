"""Hollow Howler voice, variant B "aus Vanilla abgeleitet": built at build time, never committed.

Like the Vanilla-derived textures (tools/textures/vanilla_derived.py, AGENTS.md rule 7): audio that comes from a
Mojang sound must not live in the public repository. This script pitches, stretches, cuts and filters Vanilla mob
sounds from the local Minecraft asset store into the same file names as variant A (make_howler_sounds.py; the list of
takes per event is sounds.json), so sounds.json stays the same for A, B and every mix.

Vanilla-like first (owner 18.10.): every event rests on ONE Vanilla source family that leads clearly (the player should
recognise the family), each take on a different Vanilla take of that family (Vanilla's own variety), with only a
little support from a second family and light processing (pitch 0.8-0.9, gentle filter, short room):
  heartbeat, ambient, ambient_hunger, ambient_rage, step, sniff, roar   -> Warden
  roar_rage -> Warden + Ravager roar (+ a reversed Enderman scream far back)
  notice    -> Enderman (idle 'vwoop' pitched down over a slice of the stare drone, Warden tendril clicks)
  hurt, death -> Ravager (+ Hoglin, a little Warden)
Concept docs/ai/KONZEPT-HOLLOW-HOWLER-GRUSEL-2026-10-15.md, section 10.

Selected per event with -PsbHowlerSounds=vanilla (all events) or =mix (the events tools/sounds/howler_sounds.json maps
to "vanilla"); see gradle/module-vanilla-derived-sounds.gradle. Default = variant A, the committed synthesis.

Sounds are not in the client jar: they are hashed objects of the asset index the loaders download. Search order:
--assets DIR, SB_MC_ASSETS, then under GRADLE_USER_HOME (default ~/.gradle) the Fabric Loom and NeoForm caches,
then ~/.minecraft/assets. The newest 26.3 index is used (any index as a fallback).

Run: python3.12 tools/sounds/vanilla_derived_sounds.py --out DIR [--mode vanilla|mix] [--module simplemobs] [--assets DIR]
     python3.12 tools/sounds/vanilla_derived_sounds.py --list [--mode mix]
Needs numpy, scipy and soundfile. --out DIR writes DIR/assets/simplemobs/sounds/entity/hollow_howler/*.ogg
(a resource root), --flat DIR the bare files (preview).
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
MODULE_OUTPUTS = {'simplemobs': 'entity/hollow_howler'}   # module -> sound folder under assets/<module>/sounds/
SOUNDS_JSON = REPO / 'modules/simplemobs/shared/resources/assets/simplemobs/sounds.json'
MAPPING = HERE / 'howler_sounds.json'
PREFIX = 'simplemobs:entity/hollow_howler/'
EVENT = 'entity.hollow_howler.'
PROBE = 'minecraft/sounds/mob/warden/roar_1.ogg'
CACHE_ASSETS = (('caches', 'fabric-loom', 'assets'), ('caches', 'neoformruntime', 'assets'))


def takes(sounds_json: Path = SOUNDS_JSON) -> dict:
    """Event -> file names of its takes, in sounds.json order."""
    data = json.loads(sounds_json.read_text(encoding='utf-8'))
    return {k.removeprefix(EVENT): [(s if isinstance(s, str) else s['name']).removeprefix(PREFIX) for s in v['sounds']]
            for k, v in data.items()}


def selected(mode: str, mapping: Path = MAPPING, sounds_json: Path = SOUNDS_JSON) -> list:
    """Events variant B builds for a mode: all for 'vanilla', the mapping's 'vanilla' events for 'mix', none for 'synth'."""
    events = list(takes(sounds_json))
    if mode == 'synth':
        return []
    if mode == 'vanilla':
        return events
    mix = json.loads(mapping.read_text(encoding='utf-8'))['mix']
    assert set(mix) == set(events), f'howler_sounds.json mix must name every event exactly: {sorted(set(mix) ^ set(events))}'
    bad = {e: m for e, m in mix.items() if m not in ('synth', 'vanilla')}
    assert not bad, f'mix values must be synth or vanilla: {bad}'
    return [e for e in events if mix[e] == 'vanilla']


def asset_candidates(explicit: str | None, env: dict, home_dir: str, join=os.path.join) -> list[str]:
    home = env.get('GRADLE_USER_HOME') or join(home_dir, '.gradle')
    return [c for c in (explicit, env.get('SB_MC_ASSETS')) if c] + [join(home, *p) for p in CACHE_ASSETS] + \
        [join(home_dir, '.minecraft', 'assets')]


def find_assets(explicit: str | None = None):
    """(objects dir, index dict) of the first asset store whose index has the Warden roar and its object file."""
    for c in asset_candidates(explicit, os.environ, str(Path.home())):
        idx_dir = Path(c) / 'indexes'
        if not idx_dir.is_dir():
            continue
        idxs = sorted(idx_dir.glob('*.json'), key=lambda p: (p.name.startswith('26.3'), p.stat().st_mtime), reverse=True)
        for idx in idxs:
            try:
                objs = json.loads(idx.read_text(encoding='utf-8'))['objects']
                h = objs[PROBE]['hash']
            except (OSError, KeyError, ValueError):
                continue
            if (Path(c) / 'objects' / h[:2] / h).is_file():
                return Path(c) / 'objects', objs
    return None


class Vanilla:
    def __init__(self, objects: Path, index: dict):
        self.objects, self.index = objects, index

    def __call__(self, key: str):
        """Mono float at the generator rate for 'mob/warden/roar_1'."""
        import numpy as np
        import soundfile as sf
        from howler_dsp import to_sr
        h = self.index[f'minecraft/sounds/{key}.ogg']['hash']
        x, sr = sf.read(str(self.objects / h[:2] / h), always_2d=True)
        return to_sr(np.asarray(x.mean(1), dtype=float), sr)


def recipes(v: Vanilla) -> dict:
    """Event -> function(take index) building that take. Each layer: Vanilla sound, pitch (tape ratio), level, offset."""
    import numpy as np
    from howler_dsp import SR, band, fit, high, low, master, n_of, norm, pitch, place, room, stretch, swell

    def lay(cap, *layers):
        """Mixes the layers; the file is as long as the longest layer, at most cap seconds (then faded)."""
        done = []
        for key, ratio, gain, at, *fx in layers:
            x = v(key)
            for f in fx:
                x = f(x)
            if ratio != 1:
                x = pitch(x, ratio)
            done.append((norm(x) * gain, at))
        n = min(n_of(cap), max(int(at * SR) + len(x) for x, at in done))
        out = np.zeros(n)
        for x, at in done:
            place(out, x, at)
        return fit(out, n) if n == n_of(cap) else out

    rev = lambda x: x[::-1]  # noqa: E731
    lp = lambda hz: (lambda x: low(x, hz, 3))  # noqa: E731
    hp = lambda hz: (lambda x: high(x, hz, 2))  # noqa: E731
    bp = lambda lo, hi: (lambda x: band(x, lo, hi))  # noqa: E731
    st = lambda f: (lambda x: stretch(x, f))  # noqa: E731
    head = lambda d: (lambda x: fit(x, n_of(d)))  # noqa: E731

    def seg(a, d):  # a slice of a long sound with soft edges
        def f(x):
            y = x[n_of(a):n_of(a + d)]
            return y * swell(len(y), 0.15, 0.3)
        return f

    def infra(n, amp, env):
        t = np.arange(n) / SR
        return amp * (np.sin(2 * np.pi * 40 * t) + np.sin(2 * np.pi * 57 * t)) * env

    def rng(i):
        return np.random.default_rng(700 + i)

    def heartbeat(i):  # Warden heartbeat, one beat of each Vanilla take (the code plays lub-dub), a little lower
        x = lay(0.6, (f'mob/warden/heartbeat_{i + 1}', (0.88, 0.86, 0.9, 0.87)[i], 1.0, 0.0, head(0.55)),
                (f'mob/ravager/step{i + 1}', 0.45, 0.25, 0.0, lp(260), head(0.4)))
        t = np.arange(len(x)) / SR
        x += infra(len(x), 0.05, np.exp(-t / 0.2) * np.clip(t / 0.03, 0, 1))
        return master(x, -17.5, lp=1800, fade_out=0.08)

    def ambient(i):  # Warden lurking breath, deeper; a reversed Enderman idle far back as the 'hollow' air
        x = lay(3.6, (f'mob/warden/ambient_{(1, 3, 5, 9)[i]}', (0.86, 0.84, 0.87, 0.85)[i], 1.0, 0.0),
                (f'mob/endermen/idle{(2, 4, 5, 3)[i]}', 0.55, 0.15, 1.0 + 0.15 * i, rev, lp(1500)))
        return master(room(rng(i), x, 0.08), -21.0, lp=4500, fade_out=0.2)

    def hunger(i):  # Warden agitated, with a Hoglin grunt under it
        x = lay(3.6, (f'mob/warden/agitated_{(1, 2, 4, 6)[i]}', (0.88, 0.86, 0.89, 0.87)[i], 1.0, 0.0),
                (f'mob/hoglin/idle{(3, 7, 5, 9)[i]}', 0.62, 0.3, 0.5 + 0.2 * i, lp(2500)),
                (f'mob/slime/small{(1, 3, 2, 4)[i]}', 0.7, 0.12, 1.2, bp(300, 3000)))
        return master(room(rng(i), x, 0.08), -19.5, lp=4500, fade_out=0.2)

    def rage(i):  # Warden angry growl over a stretched Ravager breath
        x = lay(3.8, (f'mob/warden/angry_{(1, 2, 4, 6)[i]}', (0.85, 0.83, 0.86, 0.84)[i], 1.0, 0.0),
                (f'mob/ravager/idle{(3, 6, 2, 7)[i]}', 0.66, 0.4, 0.15, st(1.3)),
                (f'mob/zoglin/angry{(1, 2, 3, 1)[i]}', 0.78, 0.25, 0.8 + 0.15 * i, lp(3000)))
        return master(room(rng(i), x, 0.08), -17.0, lp=5000, fade_out=0.25)

    def hurt(i):  # Ravager hurt leads, Hoglin squeal and a Warden grunt under it
        x = lay(1.1, (f'mob/ravager/hurt{i + 1}', (0.82, 0.8, 0.84, 0.81)[i], 1.0, 0.0),
                (f'mob/hoglin/hurt{i + 1}', 0.7, 0.4, 0.0),
                (f'mob/warden/hurt_{i + 1}', 0.92, 0.3, 0.0, head(0.8)))
        return master(x, -15.5, lp=6000, fade_out=0.08)

    def death(i):  # Ravager death drawn out, a Hoglin death and the Warden's last heartbeats
        x = lay(4.6, (f'mob/ravager/death{i + 1}', (0.78, 0.76, 0.8)[i], 1.0, 0.0, st(1.2)),
                (f'mob/hoglin/death{(3, 1, 2)[i]}', 0.65, 0.35, 0.15),
                (f'mob/warden/death_{(2, 1, 2)[i]}', 0.85, 0.35, 0.3),
                (f'mob/warden/heartbeat_{i + 2}', 0.85, 0.4, 0.0, head(0.9)))
        return master(room(rng(i), x, 0.08, 0.7), -19.0, lp=5000, fade_out=0.3)

    def step(i):  # Warden step, a Ravager footfall for weight
        x = lay(0.9, (f'mob/warden/step_{(1, 2, 3, 4, 1)[i]}', (0.85, 0.84, 0.86, 0.85, 0.8)[i], 1.0, 0.0, head(0.85)),
                (f'mob/ravager/step{(2, 3, 4, 5, 1)[i]}', 0.62, 0.4, 0.0, lp(1200), head(0.6)))
        return master(x, -24.5, lp=5000, fade_out=0.1)

    def notice(i):  # Enderman: the idle 'vwoop' deep down over a slice of the stare drone, Warden tendril clicks
        x = lay(1.9, (f'mob/endermen/idle{(1, 3, 5)[i]}', (0.58, 0.6, 0.56)[i], 1.0, 0.2),
                ('mob/endermen/stare', 0.7, 0.35, 0.0, seg((2.0, 5.5, 9.0)[i], 1.3), lp(1800)),
                (f'mob/warden/tendril_clicks_{(1, 3, 5)[i]}', 0.9, 0.3, 0.0, head(0.6)))
        return master(room(rng(i), x, 0.1), -19.0, lp=5500, fade_out=0.2)

    def sniff(i):  # Warden sniff, the first part of each Vanilla take
        x = lay(1.8, (f'mob/warden/sniff_{i + 1}', (0.92, 0.9, 0.93, 0.91)[i], 1.0, 0.0, hp(250), head(1.8)))
        return master(x, -21.5, lp=6500, fade_out=0.15)

    def roar(i):  # Warden roar, a Ravager roar under it
        x = lay(3.4, (f'mob/warden/roar_{(2, 4, 5)[i]}', (0.9, 0.88, 0.91)[i], 1.0, 0.0),
                (f'mob/ravager/roar{(1, 3, 4)[i]}', 0.74, 0.5, 0.05),
                (f'mob/hoglin/angry{(4, 2, 6)[i]}', 0.65, 0.2, 0.3, lp(2500)))
        return master(room(rng(i), x, 0.1, 0.7), -15.5, lp=5500, fade_out=0.15)

    def roar_rage(i):  # the longest: Warden + stretched Ravager roar, a reversed Enderman scream far back, infrasound
        x = lay(4.0, (f'mob/warden/roar_{(1, 3, 5)[i]}', (0.86, 0.84, 0.87)[i], 1.0, 0.0),
                (f'mob/ravager/roar{(2, 4, 3)[i]}', 0.68, 0.55, 0.12, st(1.15)),
                (f'mob/endermen/scream{(3, 1, 2)[i]}', 0.5, 0.1, 0.2, rev, lp(1800)))
        x += infra(len(x), 0.05, swell(len(x), 0.4, 0.9))
        return master(room(rng(i), x, 0.1, 0.8), -14.5, lp=5500, fade_out=0.15)

    return {'heartbeat': heartbeat, 'ambient': ambient, 'ambient_hunger': hunger, 'ambient_rage': rage, 'hurt': hurt,
            'death': death, 'step': step, 'notice': notice, 'sniff': sniff, 'roar': roar, 'roar_rage': roar_rage}


def build(target: Path, assets, events) -> int:
    import soundfile as sf
    from howler_dsp import SR
    made = recipes(Vanilla(*assets))
    all_takes = takes()
    assert set(made) == set(all_takes), f'recipes() and sounds.json disagree: {sorted(set(made) ^ set(all_takes))}'
    target.mkdir(parents=True, exist_ok=True)
    count = 0
    for ev in events:
        for i, name in enumerate(all_takes[ev]):
            sf.write(str(target / f'{name}.ogg'), made[ev](i), SR, format='OGG', subtype='VORBIS')
            count += 1
    return count


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', type=Path, help='resource root (writes assets/<module>/sounds/...)')
    ap.add_argument('--flat', type=Path, help='write the bare .ogg files into this folder (preview)')
    ap.add_argument('--module', choices=sorted(MODULE_OUTPUTS), default='simplemobs')
    ap.add_argument('--mode', choices=('vanilla', 'mix'), default='vanilla',
                    help="vanilla: every event; mix: the events howler_sounds.json maps to 'vanilla'")
    ap.add_argument('--mapping', type=Path, default=MAPPING)
    ap.add_argument('--assets', help='a Minecraft assets folder (with indexes/ and objects/)')
    ap.add_argument('--list', action='store_true')
    a = ap.parse_args()
    events = selected(a.mode, a.mapping)
    if a.list or not (a.out or a.flat):
        all_takes = takes()
        for ev in events:
            for name in all_takes[ev]:
                print(f'assets/simplemobs/sounds/{MODULE_OUTPUTS["simplemobs"]}/{name}.ogg')
        return
    if not events:
        print('vanilla_derived_sounds: no event mapped to vanilla, nothing to build')
        return
    assets = find_assets(a.assets)
    if assets is None:
        tried = '\n  '.join(asset_candidates(a.assets, os.environ, str(Path.home())))
        raise SystemExit('vanilla_derived_sounds: no Minecraft asset store with the Warden sounds. Searched:\n  '
                         + tried + '\nRun any 26.3 client once (e.g. gradlew :mc26_3:fabric:runClient) or pass --assets.')
    if str(HERE) not in sys.path:
        sys.path.insert(0, str(HERE))
    target = a.flat or a.out / 'assets' / a.module / 'sounds' / MODULE_OUTPUTS[a.module]
    count = build(target, assets, events)
    print(f'vanilla_derived_sounds: {count} sounds of {len(events)} events ({a.mode}, Vanilla assets {assets[0].parent})'
          f' -> {target}')


if __name__ == '__main__':
    main()
