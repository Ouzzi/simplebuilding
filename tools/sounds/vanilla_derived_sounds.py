"""Hollow Howler voice, variant B "aus Vanilla abgeleitet": built at build time, never committed.

Like the Vanilla-derived textures (tools/textures/vanilla_derived.py, AGENTS.md rule 7): audio that comes from a
Mojang sound must not live in the public repository. This script layers, pitches, stretches, reverses and filters
Vanilla mob sounds (Warden, Ravager, Hoglin/Zoglin, Enderman, Skeleton, Slime) from the local Minecraft asset store
into the same 18 file names as variant A (make_howler_sounds.py), so sounds.json stays the same.

Selected with the Gradle property -PsbHowlerSounds=vanilla (or SB_HOWLER_SOUNDS=vanilla); see
gradle/module-vanilla-derived-sounds.gradle. Default (no property) = variant A, the committed synthesis.

Sounds are not in the client jar: they are hashed objects of the asset index the loaders download. Search order:
--assets DIR, SB_MC_ASSETS, then under GRADLE_USER_HOME (default ~/.gradle) the Fabric Loom and NeoForm caches,
then ~/.minecraft/assets. The newest 26.3 index is used (any index as a fallback).

Run: python3.12 tools/sounds/vanilla_derived_sounds.py --out DIR [--module simplemobs] [--assets DIR]
     python3.12 tools/sounds/vanilla_derived_sounds.py --list
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
MODULE_OUTPUTS = {'simplemobs': 'entity/hollow_howler'}   # module -> sound folder under assets/<module>/sounds/
NAMES = ('heartbeat', 'ambient1', 'ambient2', 'ambient_hunger1', 'ambient_hunger2', 'ambient_rage1', 'ambient_rage2',
         'hurt1', 'hurt2', 'hurt3', 'death', 'step1', 'step2', 'step3', 'notice', 'sniff', 'roar', 'roar_rage')
PROBE = 'minecraft/sounds/mob/warden/roar_1.ogg'
CACHE_ASSETS = (('caches', 'fabric-loom', 'assets'), ('caches', 'neoformruntime', 'assets'))


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
    """File name -> function building it. Each layer: Vanilla sound, pitch (tape ratio), level, offset."""
    import numpy as np
    from howler_dsp import (SR, band, fit, high, low, master, n_of, norm, pitch, place, room, stretch, swell)

    def lay(n, *layers):
        out = np.zeros(n)
        for key, ratio, gain, at, *fx in layers:
            x = v(key)
            for f in fx:
                x = f(x)
            if ratio != 1:
                x = pitch(x, ratio)
            place(out, norm(x) * gain, at)
        return out

    rev = lambda x: x[::-1]  # noqa: E731
    lp = lambda hz: (lambda x: low(x, hz, 3))  # noqa: E731
    hp = lambda hz: (lambda x: high(x, hz, 2))  # noqa: E731
    st = lambda f: (lambda x: stretch(x, f))  # noqa: E731
    head = lambda d: (lambda x: fit(x, n_of(d)))  # noqa: E731
    rng = np.random.default_rng(7)

    def heartbeat():
        n = n_of(0.55)
        x = lay(n, ('mob/warden/heartbeat_1', 0.88, 1.0, 0.0, head(0.5)),
                ('mob/ravager/step1', 0.45, 0.35, 0.0, lp(260), head(0.4)))
        t = np.arange(n) / SR
        x += 0.06 * (np.sin(2 * np.pi * 40 * t) + np.sin(2 * np.pi * 57 * t)) * np.exp(-t / 0.2) * np.clip(t / 0.03, 0, 1)
        return master(x, -17.5, lp=1800)

    def ambient(i):
        n = n_of(3.0)
        x = lay(n, (f'mob/warden/ambient_{(4, 9)[i]}', 0.86, 1.0, 0.0),
                (f'mob/endermen/idle{(2, 4)[i]}', 0.55, 0.22, 1.0, rev, lp(1500)),
                (f'mob/skeleton/step{(1, 3)[i]}', 0.8, 0.12, 1.6 + 0.2 * i, hp(400)))
        return master(room(rng, x, 0.1), -21.0, lp=4500)

    def hunger(i):
        n = n_of(3.0)
        x = lay(n, (f'mob/warden/agitated_{(2, 5)[i]}', 0.88, 1.0, 0.0),
                (f'mob/hoglin/idle{(3, 7)[i]}', 0.62, 0.45, 0.5 + 0.3 * i, lp(2500)),
                (f'mob/slime/small{(1, 3)[i]}', 0.7, 0.25, 1.2, band_fn(300, 3000)),
                (f'mob/skeleton/step{(2, 4)[i]}', 0.95, 0.18, 1.9, hp(400)))
        return master(room(rng, x, 0.1), -19.5, lp=4500)

    def band_fn(lo, hi):
        return lambda x: band(x, lo, hi)

    def rage(i):
        n = n_of(3.2)
        x = lay(n, (f'mob/warden/angry_{(2, 4)[i]}', 0.85, 1.0, 0.0),
                (f'mob/ravager/idle{(3, 6)[i]}', 0.66, 0.55, 0.15, st(1.3)),
                (f'mob/zoglin/angry{(1, 3)[i]}', 0.78, 0.4, 0.8 + 0.2 * i, lp(3000)))
        return master(room(rng, x, 0.1), -17.0, lp=5000)

    def hurt(i):
        n = n_of(0.85)
        x = lay(n, (f'mob/warden/hurt_{i + 1}', 0.92, 1.0, 0.0, head(0.85)),
                (f'mob/ravager/hurt{i + 1}', 0.78, 0.55, 0.0, head(0.7)),
                (f'mob/skeleton/hurt{i + 1}', 0.75, 0.25, 0.0, hp(500), head(0.3)))
        return master(x, -15.5, lp=6000)

    def death():
        n = n_of(4.6)
        x = lay(n, ('mob/warden/death_2', 0.85, 1.0, 0.0),
                ('mob/ravager/death2', 0.6, 0.55, 0.1, st(1.4)),
                ('mob/warden/heartbeat_2', 0.85, 0.5, 0.0, head(0.9)),
                ('mob/skeleton/death', 0.7, 0.2, 2.2, hp(300)))
        return master(room(rng, x, 0.08, 0.7), -19.0, lp=5000, fade_out=0.3)

    def step(i):
        n = n_of(0.75)
        x = lay(n, (f'mob/warden/step_{i + 1}', 0.85, 1.0, 0.0, head(0.75)),
                (f'mob/ravager/step{i + 2}', 0.62, 0.45, 0.0, lp(1200), head(0.6)))
        return master(x, -24.5, lp=5000)

    def notice():
        n = n_of(1.8)
        x = lay(n, ('mob/warden/listening_angry_2', 0.9, 1.0, 0.0),
                ('mob/endermen/idle3', 0.62, 0.3, 0.25, lp(2000)),
                ('mob/skeleton/step2', 1.1, 0.15, 0.02, hp(800), head(0.25)))
        return master(fit(x, n), -19.0, lp=5500)

    def sniff():
        n = n_of(1.4)
        x = lay(n, ('mob/warden/sniff_1', 0.92, 1.0, 0.0, hp(250), head(1.4)),
                ('mob/slime/small2', 0.8, 0.1, 0.3, band_fn(800, 4000)))
        return master(x, -21.5, lp=6500)

    def roar(rage_):
        if rage_:
            n = n_of(3.8)
            x = lay(n, ('mob/warden/roar_3', 0.86, 1.0, 0.0),
                    ('mob/ravager/roar2', 0.68, 0.6, 0.12, st(1.15)),
                    ('mob/endermen/scream3', 0.5, 0.12, 0.2, rev, lp(1800)),
                    ('mob/hoglin/angry2', 0.6, 0.3, 0.9, lp(2500)))
            t = np.arange(n) / SR
            x += 0.05 * (np.sin(2 * np.pi * 40 * t) + np.sin(2 * np.pi * 57 * t)) * swell(n, 0.4, 0.9)
            return master(room(rng, x, 0.12, 0.8), -14.5, lp=5500, fade_out=0.12)
        n = n_of(3.0)
        x = lay(n, ('mob/ravager/roar1', 0.74, 1.0, 0.0),
                ('mob/warden/roar_2', 0.9, 0.65, 0.05),
                ('mob/hoglin/angry4', 0.65, 0.3, 0.3, lp(2500)))
        return master(room(rng, x, 0.12, 0.7), -15.5, lp=5500, fade_out=0.12)

    return {'heartbeat': heartbeat, 'ambient1': lambda: ambient(0), 'ambient2': lambda: ambient(1),
            'ambient_hunger1': lambda: hunger(0), 'ambient_hunger2': lambda: hunger(1),
            'ambient_rage1': lambda: rage(0), 'ambient_rage2': lambda: rage(1),
            'hurt1': lambda: hurt(0), 'hurt2': lambda: hurt(1), 'hurt3': lambda: hurt(2), 'death': death,
            'step1': lambda: step(0), 'step2': lambda: step(1), 'step3': lambda: step(2),
            'notice': notice, 'sniff': sniff, 'roar': lambda: roar(False), 'roar_rage': lambda: roar(True)}


def build(target: Path, assets) -> int:
    import soundfile as sf
    from howler_dsp import SR
    made = recipes(Vanilla(*assets))
    assert tuple(made) == NAMES, 'NAMES and recipes() disagree'
    target.mkdir(parents=True, exist_ok=True)
    for name, make in made.items():
        sf.write(str(target / f'{name}.ogg'), make(), SR, format='OGG', subtype='VORBIS')
    return len(made)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', type=Path, help='resource root (writes assets/<module>/sounds/...)')
    ap.add_argument('--flat', type=Path, help='write the bare .ogg files into this folder (preview)')
    ap.add_argument('--module', choices=sorted(MODULE_OUTPUTS), default='simplemobs')
    ap.add_argument('--assets', help='a Minecraft assets folder (with indexes/ and objects/)')
    ap.add_argument('--list', action='store_true')
    a = ap.parse_args()
    if a.list or not (a.out or a.flat):
        for name in NAMES:
            print(f'assets/simplemobs/sounds/{MODULE_OUTPUTS["simplemobs"]}/{name}.ogg')
        return
    assets = find_assets(a.assets)
    if assets is None:
        tried = '\n  '.join(asset_candidates(a.assets, os.environ, str(Path.home())))
        raise SystemExit('vanilla_derived_sounds: no Minecraft asset store with the Warden sounds. Searched:\n  '
                         + tried + '\nRun any 26.3 client once (e.g. gradlew :mc26_3:fabric:runClient) or pass --assets.')
    if str(HERE) not in sys.path:
        sys.path.insert(0, str(HERE))
    target = a.flat or a.out / 'assets' / a.module / 'sounds' / MODULE_OUTPUTS[a.module]
    count = build(target, assets)
    print(f'vanilla_derived_sounds: {count} sounds (Vanilla assets {assets[0].parent}) -> {target}')


if __name__ == '__main__':
    main()
