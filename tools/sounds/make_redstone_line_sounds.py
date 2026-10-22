"""Own block sounds of the redstone lines Astral, Nihil and Bridge (owner 22.10., Queue SP22-5,
docs/ai/PLAN-B-REDSOUND22-2026-10-22.md). Until then dust, lamps and pistons all used SoundType.AMETHYST.

Usage: python tools/sounds/make_redstone_line_sounds.py [--flat DIR] [--lamp synth|vanilla] [--piston-move vanilla|synth]
       (Python with numpy, scipy and soundfile; howler_dsp.py next to this file)

Synthesised only - no samples, no Vanilla audio - so the files are committed:
mc26_3/overlay/resources/assets/simplebuilding/sounds/block/<block>/ (the lines exist only on 26.3, McVersion.END_SYSTEMS).
The script also writes the events into the 26.3 overlay sounds.json (other entries stay as they are). Event names and
the SoundTypes are com.simplebuilding.util.LineSounds. Mono 44.1 kHz Ogg Vorbis, fixed seeds: a re-run gives the same
audio (only the random Ogg stream serial differs, so do not commit a re-run without a sound change). --flat DIR writes every take (also the variants not built in) to DIR instead, for the preview page.

Block kinds (owner: dust "eher ein Pulver-Sound", lamps may stay amethyst-like but own, pistons like pistons):
  dust   (astral/nihil/bridge_redstone)  powdery trickle of fine grains and a soft puff, like quiet sand/gravel
  lamp   (astralit/nihilith_lamp)        struck crystal-glass with inharmonic partials, own on/off ping
  piston (astral/nihil_piston)           stone knock with a mechanical clack (Vanilla's piston block is stone)
Line colour: Astral brighter and glassier (fine glass ticks, higher partials, longer ring), Nihil duller (lower,
shorter ring, a soft low body), Bridge in between.

Variants (preview A/B, Vanilla events referenced by name in sounds.json, never copied):
  --lamp synth (built in) | vanilla = minecraft:block.amethyst_block.* with an own pitch per line
  --piston-move vanilla (built in) = minecraft:block.piston.extend/contract with an own pitch per line | synth
Levels follow Vanilla 26.3 by active RMS (measured: sand/gravel dig -37/-28 dB, stone dig -20, amethyst break -26,
piston out/in -24/-21, copper bulb toggle -29).
"""
import argparse
import json
import os
import sys

import numpy as np
import soundfile as sf

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from howler_dsp import SR, band, grains, low, master, modal, n_of, room  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, "mc26_3", "overlay", "resources", "assets", "simplebuilding")
SOUNDS_JSON = os.path.join(ASSETS, "sounds.json")
NS = "simplebuilding"


class Tint:
    """The colour of a line: p pitch scale, glass (bright ticks/shimmer) and body (low knock) amounts, lp recording low-pass."""

    def __init__(self, p, glass, body, lp, ring):
        self.p, self.glass, self.body, self.lp, self.ring = p, glass, body, lp, ring


LINES = {
    "astral": Tint(p=1.18, glass=1.0, body=0.0, lp=11000, ring=1.35),
    "bridge": Tint(p=1.0, glass=0.45, body=0.35, lp=8500, ring=1.0),
    "nihil": Tint(p=0.82, glass=0.08, body=0.85, lp=5200, ring=0.6),
}

#: block id -> (kind, line)
BLOCKS = {
    "astral_redstone": ("dust", "astral"),
    "nihil_redstone": ("dust", "nihil"),
    "bridge_redstone": ("dust", "bridge"),
    "astralit_lamp": ("lamp", "astral"),
    "nihilith_lamp": ("lamp", "nihil"),
    "astral_piston": ("piston", "astral"),
    "nihil_piston": ("piston", "nihil"),
}
TAKES = 4
MOVE_TAKES = 3
#: Variant B pitches (Vanilla event referenced in sounds.json).
LAMP_VANILLA_PITCH = {"astral": 1.3, "nihil": 0.75}
PISTON_VANILLA = {"astral": ("minecraft:block.piston.extend", 1.2), "nihil": ("minecraft:block.piston.contract", 0.85)}


class Var:
    """One take: take 0 is the reference, later takes differ a little in pitch and timing, reproducibly."""

    def __init__(self, seed, take):
        self.rng = np.random.default_rng(seed * 7919 + take)
        self.p = 1.0 if take == 0 else self.rng.uniform(0.95, 1.05)
        self.d = 1.0 if take == 0 else self.rng.uniform(0.93, 1.07)


def unit(x):
    """Scaled to RMS 1, so the layers mix by the factors below and not by the chance level of their grains."""
    return x / (np.sqrt(np.mean(x ** 2)) + 1e-12)


def decay(n, tau, at=0.0):
    t = np.arange(n) / SR - at
    return np.where(t < 0, 0.0, np.exp(-np.clip(t, 0, None) / tau))


def glass_ticks(rng, n, count, at_max, tint, amp):
    """Tiny bright glass ticks (Astral's sparkle in the powder)."""
    out = np.zeros(n)
    for _ in range(count):
        f = rng.uniform(4300, 7600) * min(tint.p, 1.15)
        out += modal(rng, n, rng.uniform(0, at_max), (f, f * 1.47, f * 2.09), (0.014, 0.009, 0.006), (1.0, 0.5, 0.3),
                     strike=0.0008, noise_lp=9000) * rng.uniform(0.5, 1.0)
    return out * amp


# --- dust: powder ---------------------------------------------------------------------------------------------------

def dust(seed, tint, v, step):
    rng = v.rng
    n = n_of((0.2 if step else 0.32) * v.d)
    p = tint.p * v.p
    dens = decay(n, 0.035 if step else 0.07)
    if not step:  # the heap settles: a second, smaller trickle
        dens = np.clip(dens + 0.45 * decay(n, 0.05, at=rng.uniform(0.07, 0.1)), 0, 1)
    fine = grains(rng, n, 9000, 1300 * p, 7200 * p, dur=(0.0004, 0.0025), density=dens, amp_sd=0.7)
    coarse = grains(rng, n, 700, 500 * p, 2600 * p, dur=(0.002, 0.006), density=dens, amp_sd=0.5)
    x = unit(fine) + 0.45 * unit(coarse)
    if not step:  # the soft puff of powder
        puff = band(rng.standard_normal(n), 500 * p, 2600 * p) * decay(n, 0.09) * np.clip(np.arange(n) / (0.012 * SR), 0, 1)
        x += 0.5 * unit(puff)
    if tint.glass > 0.1:
        x += unit(glass_ticks(rng, n, 2 if step else 4, 0.12, tint, 1.0)) * 0.55 * tint.glass
    if tint.body:
        x += unit(modal(rng, n, 0.002, (140 * p, 260 * p), (0.025, 0.012), (1.0, 0.4), strike=0.003, noise_lp=600)) \
             * 0.5 * tint.body
    x = room(rng, x, wet=0.08, rt60=0.25)
    return master(x, -31 if step else -30, lp=tint.lp, hp=120, fade_out=0.04)


# --- lamp: crystal glass --------------------------------------------------------------------------------------------

RATIOS = (1.0, 2.32, 4.25, 6.63, 9.38)
AMPS = (1.0, 0.7, 0.45, 0.3, 0.18)
DECAYS = (0.3, 0.19, 0.12, 0.075, 0.05)


def crystal(rng, n, at, f, ring, soft=False):
    """One struck crystal: inharmonic glass partials, ring scales the decays (Astral long, Nihil short)."""
    freqs = [f * r for r in RATIOS]
    keep = [i for i, fr in enumerate(freqs) if fr < SR / 2 - 2000]
    return modal(rng, n, at, [freqs[i] for i in keep], [DECAYS[i] * ring for i in keep], [AMPS[i] for i in keep],
                 strike=0.003 if soft else 0.0012, noise_lp=3000 if soft else 9000)


def lamp(seed, tint, v, step):
    rng = v.rng
    n = n_of((0.42 if step else 0.75) * v.d)
    p = tint.p * v.p
    f = 1400 * p
    x = unit(crystal(rng, n, 0.0, f, tint.ring, soft=step))
    if not step:  # crystal core and glass housing: a second, higher strike just after
        x += 0.55 * unit(crystal(rng, n, rng.uniform(0.018, 0.04), f * rng.uniform(1.17, 1.24), tint.ring * 0.8))
        crack = grains(rng, n, 4000, 3000, 9000, dur=(0.0005, 0.002), density=decay(n, 0.02), amp_sd=0.6)
        x += 0.35 * unit(crack)
    if tint.glass > 0.1:  # Astral shimmer: a few high, slowly fading sines
        t = np.arange(n) / SR
        sh = sum(np.sin(2 * np.pi * rng.uniform(5200, 8800) * t + rng.uniform(0, 6.3)) * decay(n, 0.22, rng.uniform(0.01, 0.08))
                 for _ in range(3))
        x += unit(sh) * 0.18 * tint.glass
    if tint.body:  # Nihil: the crystal sits in stone
        x += unit(modal(rng, n, 0.0, (230 * p, 420 * p), (0.035, 0.02), (1.0, 0.5), strike=0.003, noise_lp=900)) \
             * 0.6 * tint.body
    x = room(rng, x, wet=0.14, rt60=0.5)
    return master(x, -33 if step else -25, lp=tint.lp + 3000, hp=150, fade_out=0.08)


def lamp_toggle(seed, tint, v, on):
    """Own on/off ping instead of the copper bulb: a click and two crystal tones, rising (on) or falling (off)."""
    rng = v.rng
    n = n_of(0.24)
    f = (2100 if tint.glass > 0.5 else 1150) * (1.0 if on else 0.8)
    a, b = (f, f * 1.5) if on else (f * 1.5, f)
    x = 0.5 * unit(modal(rng, n, 0.0, (3200, 5100), (0.004, 0.003), (1.0, 0.5), strike=0.0008, noise_lp=9000))
    x += unit(crystal(rng, n, 0.004, a, 0.35 * tint.ring)) + 0.8 * unit(crystal(rng, n, 0.05, b, 0.45 * tint.ring))
    x = room(rng, x, wet=0.1, rt60=0.3)
    return master(x, -28, lp=tint.lp + 3000, hp=200, fade_out=0.05)


# --- piston: stone and mechanics ------------------------------------------------------------------------------------

def piston(seed, tint, v, step):
    rng = v.rng
    n = n_of((0.3 if step else 0.45) * v.d)
    p = tint.p * v.p
    crunch = grains(rng, n, 5000 if not step else 2500, 250 * p, 2800 * p, dur=(0.001, 0.006),
                    density=decay(n, 0.03 if step else 0.06), amp_sd=0.6)
    x = unit(crunch)
    x += 1.1 * unit(modal(rng, n, 0.0, (180 * p, 330 * p, 610 * p), (0.05, 0.035, 0.02), (1.0, 0.6, 0.35),
                          strike=0.003, noise_lp=1500))
    clack = modal(rng, n, 0.004, (1300 * p, 2900 * p), (0.012, 0.008), (1.0, 0.5), strike=0.001, noise_lp=6000)
    x += (0.45 if step else 0.75) * unit(clack)
    if tint.glass > 0.1:
        x += unit(glass_ticks(rng, n, 1 if step else 2, 0.03, tint, 1.0)) * 0.3 * tint.glass
    if tint.body:
        x = x + 0.4 * tint.body * low(x, 300, 2)
    x = room(rng, x, wet=0.1, rt60=0.35)
    return master(x, -29 if step else -21, lp=min(tint.lp, 7500), hp=60, fade_out=0.05)


def piston_move(seed, tint, v):
    """Variant A of the move: clack, a pneumatic hiss that sweeps up (Astral pushes) or down (Nihil pulls), a knock."""
    rng = v.rng
    n = n_of(0.55 * v.d)
    p = v.p
    up = tint.glass > 0.5
    x = 0.9 * unit(modal(rng, n, 0.0, (900 * p, 2400 * p), (0.015, 0.008), (1.0, 0.6), strike=0.001, noise_lp=6000))
    noise = rng.standard_normal(n)
    lo_hi = (1200, 4500) if up else (3800, 900)
    hiss = np.zeros(n)
    blk = 512
    for i in range(0, n, blk):  # band that sweeps over the hiss
        fc = lo_hi[0] + (lo_hi[1] - lo_hi[0]) * min(1.0, i / (0.3 * SR))
        hiss[i:i + blk] = band(noise[max(0, i - 2048):i + blk], fc * 0.7 * p, fc * 1.4 * p)[-len(noise[i:i + blk]):]
    t = np.arange(n) / SR
    hiss *= np.clip(t / 0.02, 0, 1) * np.clip((0.33 - t) / 0.12, 0, 1)
    x += 0.8 * unit(hiss)
    knock = modal(rng, n, 0.3, (160 * p, 300 * p, 760 * p), (0.04, 0.025, 0.012), (1.0, 0.6, 0.4), strike=0.002, noise_lp=2500)
    x += unit(knock)
    if up:
        x += unit(glass_ticks(rng, n, 2, 0.32, tint, 1.0)) * 0.25
    x = room(rng, x, wet=0.12, rt60=0.4)
    return master(x, -23, lp=tint.lp, hp=60, fade_out=0.05)


KIND = {"dust": dust, "lamp": lamp, "piston": piston}


def seed_of(name):
    return sum((i + 1) * ord(c) for i, c in enumerate(name)) % 9973


def files():
    """File name ('<block>/<take>') -> zero-argument maker, for every take including variant-A moves."""
    made = {}
    for block, (kind, line) in BLOCKS.items():
        tint, make, base = LINES[line], KIND[kind], seed_of(block)
        for k in range(TAKES):
            made[f"{block}/dig{k + 1}"] = lambda m=make, t=tint, b=base, k=k: m(b, t, Var(b, k), False)
            made[f"{block}/step{k + 1}"] = lambda m=make, t=tint, b=base, k=k: m(b, t, Var(b + 500, k), True)
        if kind == "lamp":
            made[f"{block}/toggle_on"] = lambda t=tint, b=base: lamp_toggle(b, t, Var(b + 900, 0), True)
            made[f"{block}/toggle_off"] = lambda t=tint, b=base: lamp_toggle(b, t, Var(b + 901, 0), False)
        if kind == "piston":
            for k in range(MOVE_TAKES):
                made[f"{block}/move{k + 1}"] = lambda t=tint, b=base, k=k: piston_move(b, t, Var(b + 700, k))
    return made


def ref(name, **extra):
    return dict({"name": name}, **extra)


def events(lamp_variant="synth", move_variant="vanilla"):
    """The sounds.json entries of the lines, as built in (and the files they use)."""
    out = {}
    for block, (kind, line) in BLOCKS.items():
        dig = [f"{NS}:block/{block}/dig{k + 1}" for k in range(TAKES)]
        step = [f"{NS}:block/{block}/step{k + 1}" for k in range(TAKES)]
        sounds = {"break": dig, "place": dig, "step": step, "hit": step, "fall": step}
        subtitle = {"break": "break", "place": "place", "step": "footsteps", "hit": "hit", "fall": "fall"}
        for ev, names in sounds.items():
            if kind == "lamp" and lamp_variant == "vanilla":
                names = [ref(f"minecraft:block.amethyst_block.{ev}", type="event", pitch=LAMP_VANILLA_PITCH[line])]
            out[f"block.{block}.{ev}"] = {"sounds": list(names), "subtitle": f"subtitles.block.generic.{subtitle[ev]}"}
        if kind == "lamp":
            for state in ("on", "off"):
                out[f"block.{block}.turn_{state}"] = {
                    "sounds": [ref(f"{NS}:block/{block}/toggle_{state}", attenuation_distance=6)],
                    "subtitle": f"subtitles.{NS}.block.{block}.turn_{state}"}
        if kind == "piston":
            if move_variant == "vanilla":
                event, pitch = PISTON_VANILLA[line]
                moves = [ref(event, type="event", pitch=pitch)]
            else:
                moves = [ref(f"{NS}:block/{block}/move{k + 1}", attenuation_distance=8) for k in range(MOVE_TAKES)]
            out[f"block.{block}.move"] = {"sounds": moves, "subtitle": f"subtitles.{NS}.block.{block}.move"}
    return out


def used_files(entries):
    """Own files ('<block>/<take>') the entries play; referenced Vanilla events need none."""
    prefix = f"{NS}:block/"
    out = set()
    for spec in entries.values():
        for s in spec["sounds"]:
            if isinstance(s, dict) and s.get("type") == "event":
                continue
            name = s if isinstance(s, str) else s["name"]
            if name.startswith(prefix):
                out.add(name.removeprefix(prefix))
    return sorted(out)


def write(path, x):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sf.write(path, x, SR, format="OGG", subtype="VORBIS")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--flat", help="write every take to this folder (preview), leave the repository alone")
    ap.add_argument("--lamp", choices=("synth", "vanilla"), default="synth")
    ap.add_argument("--piston-move", choices=("vanilla", "synth"), default="vanilla")
    args = ap.parse_args()
    made = files()
    if args.flat:
        for name, make in made.items():
            write(os.path.join(args.flat, name.replace("/", "__") + ".ogg"), make())
        print(f"{len(made)} takes -> {args.flat}")
        return
    entries = events(args.lamp, args.piston_move)
    keep = set(used_files(entries))
    root = os.path.join(ASSETS, "sounds", "block")
    for block in BLOCKS:  # drop takes the chosen variants no longer use
        folder = os.path.join(root, block)
        if os.path.isdir(folder):
            for f in os.listdir(folder):
                if f.endswith(".ogg") and f"{block}/{f[:-4]}" not in keep:
                    os.remove(os.path.join(folder, f))
    for name in sorted(keep):
        x = made[name]()
        write(os.path.join(root, name + ".ogg"), x)
        print(f"{name}.ogg {len(x) / SR:.2f}s peak {20 * np.log10(np.max(np.abs(x))):.1f} dBFS")
    with open(SOUNDS_JSON, encoding="utf-8") as f:
        data = json.load(f)
    data = {k: v for k, v in data.items() if k not in entries} | entries
    with open(SOUNDS_JSON, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"{len(entries)} events in {os.path.relpath(SOUNDS_JSON, REPO)}")


if __name__ == "__main__":
    main()
