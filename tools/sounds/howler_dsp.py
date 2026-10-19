"""Shared signal tools for the Hollow Howler voice (variant A make_howler_sounds.py, variant B vanilla_derived_sounds.py).

Why the first synthesis (claude-howler3) sounded synthetic, measured against Vanilla (Warden, Enderman, Ravager, Zoglin
from the 26.3 asset index, see docs/ai/KONZEPT-HOLLOW-HOWLER-GRUSEL-2026-10-15.md section 9):
- Vanilla mob voices sit low: spectral centroid 130-600 Hz, 95 % of the energy below 0.5-2.4 kHz. The old ambients
  centred at 2.1 kHz (band-passed hiss), the old roars/steps were 96-99 % one sub sine below 150 Hz.
- Vanilla voices are barely periodic (recorded growls: rough source, 7-12 % pitch jitter, subharmonics) and their
  envelopes are slow (0.5-0.9 s swell for ambients). The old voice was a clean saw through band-passes.
- Vanilla is recorded in a room: a short, dark tail glues the layers together. The old files were bone dry.
- Loudness: Warden ambient peaks -5..-9 dBFS with active RMS about -20 dBFS, roars -0.3 dBFS / -15 dBFS. The old
  files were all peak-normalised to -2 dBFS, so the roars were about 4 dB hotter than the Warden's.
The tools here answer each point: pulse-by-pulse glottal source with jitter/shimmer/fry, cascade formant tract,
pitch-synchronous breath, granular wet noise, modal (resonant) impacts, a generated room and loudness by active RMS.
"""
import numpy as np
from scipy.signal import butter, fftconvolve, lfilter, resample_poly, sosfilt

SR = 44100


def band(x, lo, hi, order=2):
    return sosfilt(butter(order, [lo, hi], btype="band", fs=SR, output="sos"), x)


def low(x, hi, order=2):
    return sosfilt(butter(order, hi, btype="low", fs=SR, output="sos"), x)


def high(x, lo, order=2):
    return sosfilt(butter(order, lo, btype="high", fs=SR, output="sos"), x)


def n_of(dur):
    return int(SR * dur)


def curve(points, n):
    """Piecewise-linear control curve over n samples from (position 0..1, value) points."""
    xs, ys = zip(*points)
    return np.interp(np.linspace(0, 1, n), xs, ys)


def drift(rng, n, rate_hz, depth):
    """Slow random wander around 1.0 (smoothed noise, depth = relative std)."""
    k = max(4, int(n / SR * rate_hz) + 4)
    pts = rng.standard_normal(k)
    return 1 + depth * np.interp(np.linspace(0, k - 1, n), np.arange(k), pts)


def swell(n, attack, release, curve_pow=1.0):
    """Organic envelope: raised-cosine attack (s), hold, raised-cosine release (s)."""
    t = np.arange(n) / SR
    d = n / SR
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    r = np.clip((d - t) / max(release, 1e-4), 0, 1)
    return (0.5 - 0.5 * np.cos(np.pi * a)) * (0.5 - 0.5 * np.cos(np.pi * r)) ** curve_pow


def place(buf, sig, at):
    """Adds sig into buf starting at time 'at' (s), clipped to the buffer."""
    i = int(at * SR)
    if i >= len(buf):
        return buf
    k = min(len(sig), len(buf) - i)
    buf[i:i + k] += sig[:k]
    return buf


# --- source -------------------------------------------------------------------------------------------------------

def glottal(rng, f0c, jitter=0.06, shimmer=0.2, fry=0.0, open_q=0.65, skew=0.75):
    """Pulse-by-pulse glottal flow derivative (Rosenberg-like) with per-period jitter, shimmer and fry.

    f0c: f0 per sample. fry 0..1: every other pulse weaker and late (period doubling = the 'growl' of large animals).
    Returns (source, flow) - flow (0..1 per period) drives pitch-synchronous breath noise.
    """
    n = len(f0c)
    src = np.zeros(n)
    flow = np.zeros(n)
    wander = drift(rng, n, 6, jitter * 0.6)
    pos = 0.0
    k = 0
    while pos < n - 2:
        i = int(pos)
        f = max(18.0, f0c[i] * wander[i] * (1 + jitter * rng.standard_normal()))
        T = SR / f
        odd = k % 2 == 1
        if fry and odd:
            T *= 1 + 0.35 * fry * rng.uniform(0.5, 1.0)
        amp = max(0.05, 1 + shimmer * rng.standard_normal()) * ((1 - 0.75 * fry) if odd else 1.0)
        L = int(T)
        tp = max(2, int(L * open_q * skew))
        tn = max(2, int(L * open_q * (1 - skew)))
        g = np.zeros(L)
        g[:tp] = 0.5 * (1 - np.cos(np.pi * np.arange(tp) / tp))
        e = min(L, tp + tn)
        g[tp:e] = np.cos(0.5 * np.pi * np.arange(e - tp) / tn)
        d = np.diff(np.concatenate(([0.0], g)))
        m = min(L, n - i)
        src[i:i + m] += amp * d[:m]
        flow[i:i + m] = np.maximum(flow[i:i + m], g[:m])
        pos += T
        k += 1
    return src / (np.max(np.abs(src)) + 1e-9), flow


def aspiration(rng, flow, amount):
    """Breath noise modulated by the glottal flow (noise bursts inside each period, as in real voices)."""
    noise = high(rng.standard_normal(len(flow)), 300)
    return noise * (0.25 + flow) * amount


# --- tract ----------------------------------------------------------------------------------------------------------

def formant_cascade(x, tracks, bws, block=256):
    """Klatt-style cascade of 2-pole resonators, unity DC gain each, coefficients updated per block.

    tracks: list of per-sample formant frequency arrays (F1..Fk); bws: bandwidths (Hz) per formant.
    """
    y = x.copy()
    n = len(x)
    for f_arr, bw in zip(tracks, bws):
        out = np.zeros(n)
        zi = np.zeros(2)
        r = np.exp(-np.pi * bw / SR)
        for s in range(0, n, block):
            e = min(n, s + block)
            th = 2 * np.pi * np.mean(f_arr[s:e]) / SR
            a1, a2 = -2 * r * np.cos(th), r * r
            out[s:e], zi = lfilter([1 + a1 + a2], [1, a1, a2], y[s:e], zi=zi)
        y = out
    return y


VOWELS = {  # big-creature tract: adult vowels scaled by ~0.62 (a much longer throat); F1..F4
    'u': (190, 470, 1400, 2000), 'o': (280, 520, 1500, 2050), 'a': (440, 760, 1600, 2150),
    'e': (300, 1100, 1650, 2200), 'i': (190, 1350, 1800, 2250), 'ae': (400, 1000, 1550, 2100),
}


def vowel_tracks(rng, names, n, wobble=0.05):
    """Formant tracks morphing through the named vowels with a slow organic wander."""
    pts = np.array([VOWELS[v] for v in names], dtype=float)
    pos = np.linspace(0, len(names) - 1, n)
    out = []
    for f in range(4):
        tr = np.interp(pos, np.arange(len(names)), pts[:, f])
        out.append(tr * drift(rng, n, 2.5, wobble))
    return out


def voice(rng, f0c, vowels, jitter=0.06, shimmer=0.2, fry=0.0, breath=0.35, bw=(90, 110, 170, 240), drive=0.0):
    """A whole voiced/breathy layer: glottal source + aspiration through the formant cascade, gentle lip radiation."""
    n = len(f0c)
    src, flow = glottal(rng, f0c, jitter, shimmer, fry)
    exc = src + aspiration(rng, flow, breath)
    y = formant_cascade(exc, vowel_tracks(rng, vowels, n), bw)
    if drive:
        y = np.tanh(y / (np.max(np.abs(y)) + 1e-9) * (1 + 3 * drive))
    return y / (np.max(np.abs(y)) + 1e-9)


def whisper(rng, n, vowels, bw=(140, 180, 260, 320)):
    """Unvoiced breath through the tract (inhale/exhale), the dry 'air' of a big throat."""
    y = formant_cascade(high(rng.standard_normal(n), 150), vowel_tracks(rng, vowels, n, 0.08), bw)
    return y / (np.max(np.abs(y)) + 1e-9)


# --- texture --------------------------------------------------------------------------------------------------------

def grains(rng, n, rate, lo, hi, dur=(0.002, 0.012), density=None, amp_sd=0.6):
    """Granular noise: short Hann-windowed band-noise grains at Poisson times (rate per second, scaled by density)."""
    out = np.zeros(n)
    dens = np.ones(n) if density is None else density
    t = 0.0
    while True:
        t += rng.exponential(1 / rate)
        i = int(t * SR)
        if i >= n:
            break
        if rng.random() > dens[i]:
            continue
        m = max(8, int(SR * rng.uniform(*dur)))
        fc = np.exp(rng.uniform(np.log(lo), np.log(hi)))
        g = band(rng.standard_normal(m + 64), fc * 0.7, min(fc * 1.4, SR / 2 - 100))[64:] * np.hanning(m)
        place(out, g * np.exp(amp_sd * rng.standard_normal()), t)
    return out


def bubbles(rng, n, rate, density=None):
    """Wet gurgle: tiny damped sines that rise in pitch (Minnaert-like bubbles)."""
    out = np.zeros(n)
    dens = np.ones(n) if density is None else density
    t = 0.0
    while True:
        t += rng.exponential(1 / rate)
        i = int(t * SR)
        if i >= n:
            break
        if rng.random() > dens[i]:
            continue
        d = rng.uniform(0.008, 0.03)
        m = int(SR * d)
        tt = np.arange(m) / SR
        f = rng.uniform(220, 900) * (1 + 1.5 * tt / d)
        s = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt / (d * 0.35))
        place(out, s * rng.uniform(0.3, 1.0), t)
    return out


def modal(rng, n, at, freqs, decays, amps, strike=0.004, noise_lp=1200):
    """Resonant impact: a short noise strike exciting damped modes (bodies, bone, ground) - not a sine thump."""
    m = int(SR * (max(decays) * 6 + strike))
    tt = np.arange(m) / SR
    exc = low(rng.standard_normal(m), noise_lp) * np.exp(-tt / strike)
    out = np.zeros(m)
    for f, d, a in zip(freqs, decays, amps):
        f = f * rng.uniform(0.96, 1.04)
        r = np.exp(-1 / (d * SR))
        th = 2 * np.pi * f / SR
        out += a * lfilter([1 - r], [1, -2 * r * np.cos(th), r * r], exc)
    buf = np.zeros(n)
    return place(buf, out / (np.max(np.abs(out)) + 1e-9), at)


def bone(rng, n, at, amp=1.0):
    """A bone tick/clack: bright strike + two short wooden modes + a little crackle."""
    f = rng.uniform(900, 1700)
    x = modal(rng, n, at, (f, f * 2.31, f * 0.57), (0.009, 0.005, 0.014), (1.0, 0.5, 0.4), strike=0.0015,
              noise_lp=7000)
    x += 0.35 * grains(rng, n, 400, 2500, 6000, (0.0008, 0.002), density=_window(n, at, 0.012))
    return x * amp


def _window(n, at, dur):
    d = np.zeros(n)
    i = int(at * SR)
    d[i:i + int(dur * SR)] = 1
    return d


# --- space and level ------------------------------------------------------------------------------------------------

def room_ir(rng, rt60=0.55, early=8, predelay=0.006):
    """A short dark cave room: a few early reflections + a noise tail whose highs die faster than its lows."""
    m = int(SR * rt60 * 1.2)
    t = np.arange(m) / SR
    tail = np.zeros(m)
    for (lo, hi), rt in (((40, 400), rt60), ((400, 2000), rt60 * 0.7), ((2000, 9000), rt60 * 0.4)):
        tail += band(rng.standard_normal(m), lo, hi) * np.exp(-6.9 * t / rt)
    tail *= np.clip((t - predelay) / 0.02, 0, 1)
    ir = tail * 0.35
    for _ in range(early):
        place(ir, np.array([rng.uniform(0.3, 0.8) * rng.choice((-1, 1))]), predelay + rng.uniform(0.003, 0.045))
    ir[0] = 0
    return ir / (np.sqrt(np.sum(ir ** 2)) + 1e-9)


def room(rng, x, wet=0.18, rt60=0.55):
    """Dry + generated room, length kept (the tail fades inside the file)."""
    w = fftconvolve(x, room_ir(rng, rt60))[:len(x)]
    w *= np.sqrt(np.mean(x ** 2) + 1e-12) / (np.sqrt(np.mean(w ** 2)) + 1e-12)
    return x * (1 - wet) + w * wet


def active_rms_db(x, rel=0.01):
    fr = 1024
    k = len(x) // fr
    f = x[:k * fr].reshape(k, fr)
    e = np.sqrt((f ** 2).mean(1))
    act = e > np.max(np.abs(x)) * rel
    return 20 * np.log10(np.sqrt((f[act] ** 2).mean()) + 1e-12)


def master(x, target_db, peak_db=-1.0, lp=6500, hp=32, fade_in=0.004, fade_out=0.03, body=0.0):
    """Band-limit like a recording, set the loudness by active RMS (Vanilla-like), soft-limit the peak, fade.
    body: extra chest weight below ~180 Hz (Vanilla's big mobs carry 60-85 % of their energy there)."""
    x = x - np.mean(x)
    if body:
        x = x + body * low(x, 180, 2)
    x = high(low(x, lp, 2), hp, 2)
    x = x * 10 ** ((target_db - active_rms_db(x)) / 20)
    lim = 10 ** (peak_db / 20)
    over = np.abs(x) > lim * 0.7
    if over.any():  # soft knee above 70 % of the ceiling
        k = lim * 0.7
        x = np.where(over, np.sign(x) * (k + (lim - k) * np.tanh((np.abs(x) - k) / (lim - k))), x)
    a, b = int(SR * fade_in), int(SR * fade_out)
    x[:a] *= np.linspace(0, 1, a)
    x[-b:] *= np.linspace(1, 0, b) ** 2
    return x.astype(np.float32)


# --- resampling / stretching (variant B) ------------------------------------------------------------------------------

def to_sr(x, sr):
    if sr == SR:
        return x
    from math import gcd
    g = gcd(SR, sr)
    return resample_poly(x, SR // g, sr // g)


def pitch(x, ratio):
    """Tape-style pitch change (ratio < 1 = lower and longer), as Vanilla's own pitch field does."""
    up, down = int(round(1000 / ratio)), 1000
    from math import gcd
    g = gcd(up, down)
    return resample_poly(x, up // g, down // g)


def stretch(x, factor, win=2048, hop=512):
    """Phase-vocoder time stretch (factor > 1 = longer) without changing the pitch."""
    w = np.hanning(win)
    frames = range(0, max(1, len(x) - win), hop)
    spec = np.array([np.fft.rfft(w * x[i:i + win]) for i in frames])
    if len(spec) < 2:
        return x
    steps = np.arange(0, len(spec) - 1, 1 / factor)
    steps = steps[steps < len(spec) - 1]  # float arange can overshoot by one frame
    phase = np.angle(spec[0])
    expected = 2 * np.pi * hop * np.arange(win // 2 + 1) / win
    out = np.zeros(int(len(steps) * hop + win))
    for k, s in enumerate(steps):
        i = int(s)
        frac = s - i
        mag = (1 - frac) * np.abs(spec[i]) + frac * np.abs(spec[i + 1])
        frame = np.fft.irfft(mag * np.exp(1j * phase))
        out[k * hop:k * hop + win] += w * frame
        dphi = np.angle(spec[i + 1]) - np.angle(spec[i]) - expected
        dphi -= 2 * np.pi * np.round(dphi / (2 * np.pi))
        phase += expected + dphi
    return norm(out) * np.max(np.abs(x))


def fit(x, n):
    """Pads or cuts to n samples (cut with a short fade)."""
    if len(x) >= n:
        y = x[:n].copy()
        m = min(n, int(SR * 0.05))
        y[-m:] *= np.linspace(1, 0, m)
        return y
    return np.concatenate((x, np.zeros(n - len(x))))


def norm(x):
    return x / (np.max(np.abs(x)) + 1e-9)
