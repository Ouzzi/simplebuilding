"""a-shelftex (12.11.2026, docs/ai/PLAN-A-SHELFTEX-2026-11-12.md): where do a module's committed textures come from?

Counts only (rule A8: no Mojang pixel in the repository) - nothing is written into the repository. Every committed PNG of the
module (git ls-files modules/<module>/**.png) is compared with the 26.3 client jar (assets/minecraft/textures/**.png) and with
the module's own Vanilla-derived build outputs (tools/textures/vanilla_derived.MODULE_OUTPUTS, e.g. the End planks):

  exact  pixels equal 1:1 to a reference of the same size (8 orientations), opaque in both, minus the number expected by
         chance (own colours shuffled; a texture drawn in the same palette shares some pixels anyway)
  swap   palette-swap fit beyond chance: f = sum over the reference's colours of (pixels of the best own colour - 1) divided by
         sum of (pixels - 1), corrected for chance as (f - p) / (1 - p), p = share of the own texture's most common colour -
         1.0 is a pure recolour, unrelated textures stay well below; only reference colours of >= 4 pixels count, at least 3
         of them, they have to cover half of the picture, and the alpha masks have to agree (IoU >= 0.9)
  part   the same swap fit for the halves and quadrants (a recoloured Vanilla frame or half of a sprite)
  mask   alpha-mask IoU with a Vanilla sprite (only for textures with 10-90 % transparency): the silhouette itself
  runs   horizontal 8-px runs (opaque, >= 3 colours) that occur 1:1 anywhere in a reference up to 128x128 (copies at any
         offset or size, e.g. a frame row of a recoloured plank)

A file is a finding (Mojang pixels) when exact >= 16 px, swap >= 0.90 (a-mojang08's threshold) or runs >= 2; it is to be looked
at when part >= 0.90 or mask >= 0.98. Every finding and look is checked by eye.

    python3.12 tools/textures/shelftex12_2026_11_12.py --scan [--module simpleend] [--json OUT] [--files PNG...] [--all]
"""
from __future__ import annotations

import argparse
import io
import json
import subprocess
import sys
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))

RUN = 8
RUN_MAX_AREA = 128 * 128
MIN_CLASS = 4
CHUNK = 1 << 22                      # elements per vectorised block (memory: sb-agent is small)
EXACT_MIN, SWAP_MIN, RUNS_MIN = 16, 0.90, 2
PART_MIN, MASK_MIN = 0.90, 0.98
HASH_MUL = np.array([(0x9E3779B97F4A7C15 * (i + 1) * 2 + 1) & 0xFFFFFFFFFFFFFFFF for i in range(RUN)], dtype=np.uint64)


def codes(im: Image.Image) -> np.ndarray:
    """RGBA -> one uint32 per pixel; fully transparent pixels are 0 (their colour does not matter)."""
    a = np.asarray(im.convert('RGBA'), dtype=np.uint32)
    c = (a[..., 0] << 24) | (a[..., 1] << 16) | (a[..., 2] << 8) | a[..., 3]
    c[a[..., 3] == 0] = 0
    return c


def orientations(a: np.ndarray):
    for k in range(4):
        r = np.rot90(a, k)
        yield f'rot{90 * k}', r
        yield f'rot{90 * k}+flip', r[:, ::-1]


def references(jar: Path, module: str) -> dict:
    """name -> code image: every Vanilla texture and the module's Vanilla-derived outputs."""
    refs = {}
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            if n.startswith('assets/minecraft/textures/') and n.endswith('.png'):
                try:
                    refs['minecraft:' + n[len('assets/minecraft/textures/'):-4]] = codes(Image.open(io.BytesIO(z.read(n))))
                except Exception:  # noqa: BLE001 - a broken sprite is no reference
                    pass
    import vanilla_derived
    if module in vanilla_derived.MODULE_OUTPUTS:
        for rel, im in vanilla_derived.module_images(jar, module).items():
            refs[f'derived {module}:{rel[:-4]}'] = codes(im)
    return refs


# ---------------------------------------------------------------- runs (copies at any offset)

def run_hashes(c: np.ndarray):
    """Hash, y, x of every horizontal RUN-px run of c that is opaque and has >= 3 colours."""
    h, w = c.shape
    if w < RUN:
        return np.zeros(0, np.uint64), np.zeros(0, int), np.zeros(0, int)
    win = np.lib.stride_tricks.sliding_window_view(c, RUN, axis=1)          # (h, w-RUN+1, RUN)
    s = np.sort(win, axis=-1)
    ok = (s[..., 0] != 0) & ((np.diff(s, axis=-1) != 0).sum(-1) >= 2)
    hs = (win.astype(np.uint64) * HASH_MUL).sum(-1, dtype=np.uint64)
    ys, xs = np.nonzero(ok)
    return hs[ok], ys, xs


class RunIndex:
    def __init__(self, refs: dict):
        self.names = [n for n, c in refs.items() if c.size <= RUN_MAX_AREA]
        hs, ids = [], []
        for i, n in enumerate(self.names):
            for flip in (False, True):
                h, _, _ = run_hashes(refs[n][:, ::-1] if flip else refs[n])
                hs.append(h)
                ids.append(np.full(h.size, 2 * i + flip, np.int32))
        h = np.concatenate(hs)
        order = np.argsort(h, kind='stable')
        self.h, self.ids = h[order], np.concatenate(ids)[order]

    def hits(self, c: np.ndarray):
        hs, ys, xs = run_hashes(c)
        if not hs.size:
            return 0, ''
        pos = np.minimum(np.searchsorted(self.h, hs), self.h.size - 1)
        found = self.h[pos] == hs
        if not found.any():
            return 0, ''
        k = int(np.nonzero(found)[0][0])
        ref = int(self.ids[pos[k]])
        return int(found.sum()), f"{self.names[ref // 2]}{' (flip)' if ref % 2 else ''} <- own x{xs[k]} y{ys[k]}"


# ---------------------------------------------------------------- same size (exact, swap, mask)

def stack(refs: dict, shape) -> tuple:
    """Every reference in every orientation that has the given shape: names, codes (n, p) uint32, idx (n, p) int16."""
    names, cs, ix = [], [], []
    for name, c in refs.items():
        if sorted(c.shape) != sorted(shape):
            continue
        pal, inv = np.unique(c, return_inverse=True)
        inv = inv.reshape(c.shape).astype(np.int16)
        if (pal == 0).any():
            inv = np.where(c == 0, -1, inv).astype(np.int16)
        for (oname, oc), (_, oi) in zip(orientations(c), orientations(inv)):
            if oc.shape == tuple(shape):
                names.append(f'{name} {oname}')
                cs.append(oc.ravel())
                ix.append(oi.ravel())
    if not names:
        return [], None, None
    return names, np.stack(cs), np.stack(ix)


def swap_fit(own_idx: np.ndarray, ref_idx: np.ndarray, valid: np.ndarray):
    """Per reference row: sums over its colours (>= MIN_CLASS valid pixels) of (max count of one own colour - 1), of
    (count - 1), and the pixels in those colours."""
    n, p = ref_idx.shape
    k = int(own_idx.max()) + 2
    big = np.int64(1) << 40
    key = ref_idx.astype(np.int64) * k + own_idx[None, :]
    key = np.where(valid, key, big - 1)
    key.sort(axis=1)
    flat = (key + np.arange(n, dtype=np.int64)[:, None] * big).ravel()
    del key
    starts = np.r_[0, np.nonzero(np.diff(flat))[0] + 1]
    lengths = np.diff(np.r_[starts, flat.size])
    vals = flat[starts]
    row, local = vals // big, vals % big
    keep = local != big - 1
    row, local, lengths = row[keep], local[keep], lengths[keep]
    if not lengths.size:
        return np.zeros(n), np.zeros(n), np.zeros(n), np.zeros(n)
    cls = row * (big // k + 1) + local // k
    gstart = np.r_[0, np.nonzero(np.diff(cls))[0] + 1]
    gmax = np.maximum.reduceat(lengths, gstart)
    gsum = np.add.reduceat(lengths, gstart)
    grow = row[gstart]
    big_cls = gsum >= MIN_CLASS  # tiny colour classes (painted, nearly unique colours) fit anything that is smooth nearby
    num = np.bincount(grow, weights=np.where(big_cls, gmax - 1, 0), minlength=n)
    den = np.bincount(grow, weights=np.where(big_cls, gsum - 1, 0), minlength=n)
    cov = np.bincount(grow, weights=np.where(big_cls, gsum, 0), minlength=n)
    ncls = np.bincount(grow, weights=big_cls, minlength=n)
    return num, den, cov, ncls


def chance_fit(own_idx, valid, num, den, cov, ncls, own_n):
    """(f - p) / (1 - p) per row; 0 where the repeated colours cover less than half of the valid pixels, den < 32, fewer
    than 3 such colours (a two-tone mask - banner pattern, frame - matches any picture with a frame or a flat sky) or the
    reference covers less than half of the own opaque pixels of the region (own_n)."""
    nvalid = valid.sum(1)
    k = int(own_idx.max()) + 1
    onehot = np.zeros((own_idx.size, k), np.int32)
    onehot[np.arange(own_idx.size), own_idx] = 1
    p = (valid.astype(np.int32) @ onehot).max(1) / np.maximum(nvalid, 1)
    f = num / np.maximum(den, 1)
    ok = (den >= 32) & (cov >= 0.5 * nvalid) & (ncls >= 3) & (nvalid >= 0.5 * own_n) & (p < 1)
    return np.where(ok, (f - p) / np.maximum(1 - p, 1e-9), 0)


def chance_exact(own, ref, both):
    """Expected equal pixels if the own colours were shuffled over the shared opaque pixels."""
    o, r = own[both], ref[both]
    if not o.size:
        return 0.0
    oc, on = np.unique(o, return_counts=True)
    rc, rn = np.unique(r, return_counts=True)
    common, oi, ri = np.intersect1d(oc, rc, return_indices=True)
    return float((on[oi] * rn[ri]).sum() / o.size)


def regions(h, w):
    """Whole picture, halves and quadrants (part copies: a recoloured Vanilla frame or half of a sprite)."""
    yy, xx = np.mgrid[0:h, 0:w]
    out = {'ganz': np.ones((h, w), bool)}
    if h >= 16 and w >= 16:
        out.update({'oben': yy < h // 2, 'unten': yy >= h // 2, 'links': xx < w // 2, 'rechts': xx >= w // 2})
    if h >= 32 and w >= 32:  # quadrants of 16x16 and more (8x8 corners of a framed picture all look alike)
        out.update({'oben links': (yy < h // 2) & (xx < w // 2), 'oben rechts': (yy < h // 2) & (xx >= w // 2),
                    'unten links': (yy >= h // 2) & (xx < w // 2), 'unten rechts': (yy >= h // 2) & (xx >= w // 2)})
    return {k: v.ravel() for k, v in out.items()}


def score(c: np.ndarray, rel: str, group: tuple, runs: RunIndex) -> dict:
    """The counts of one texture (code image) against the stacked references of its shape."""
    rec = {'file': rel, 'size': list(c.shape[::-1]), 'exact': 0, 'exact_ref': '', 'swap': 0.0, 'swap_ref': '',
           'swap_part': 0.0, 'swap_part_ref': '', 'mask': 0.0, 'mask_ref': '', 'runs': 0, 'runs_ref': ''}
    names, gcodes, gidx = group
    if names:
        own = c.ravel()
        opaque_own = own != 0
        _, own_idx = np.unique(own, return_inverse=True)
        own_idx = own_idx.ravel().astype(np.int64)
        clear = 1 - opaque_own.mean()
        regs = regions(*c.shape)
        best = {'exact': (-1, ''), 'mask': (-1.0, ''), 'ganz': (-1.0, ''), 'part': (-1.0, '')}
        step = max(1, CHUNK // own.size)
        for a in range(0, len(names), step):
            cc, ii = gcodes[a:a + step], gidx[a:a + step]
            opaque_ref = cc != 0
            both = opaque_ref & opaque_own[None, :]
            ex = ((cc == own[None, :]) & both).sum(1)
            union = (opaque_ref | opaque_own[None, :]).sum(1)
            iou = np.where(union > 0, both.sum(1) / np.maximum(union, 1), 0)
            # beyond chance: a texture in the same palette (the End planks' own target colours) shares pixels by chance
            exp = np.zeros(len(cc))
            for r in np.nonzero(ex >= EXACT_MIN // 2)[0]:
                exp[r] = chance_exact(own, cc[r], both[r])
            ex = ex - exp
            j = int(ex.argmax())
            if ex[j] > best['exact'][0]:
                best['exact'] = (float(ex[j]), f'{names[a + j]} ({int(ex[j] + exp[j])} gleich, Zufall {exp[j]:.0f})')
            if 0.1 <= clear <= 0.9:  # a real silhouette: the alpha mask itself can be a copy (rule A8 "Maske")
                j = int(iou.argmax())
                if iou[j] > best['mask'][0]:
                    best['mask'] = (float(iou[j]), names[a + j])
            for name, region in regs.items():
                valid = both & (ii >= 0) & region[None, :]
                fit = chance_fit(own_idx, valid, *swap_fit(own_idx, ii, valid), int((opaque_own & region).sum()))
                if name == 'ganz':
                    fit = np.where(iou >= 0.9, fit, 0)
                j = int(fit.argmax())
                slot = 'ganz' if name == 'ganz' else 'part'
                if fit[j] > best[slot][0]:
                    best[slot] = (float(fit[j]), names[a + j] if name == 'ganz' else f'{name} <- {names[a + j]}')
        rec['exact'], rec['exact_ref'] = int(round(max(0, best['exact'][0]))), best['exact'][1]
        rec['mask'], rec['mask_ref'] = round(max(0.0, best['mask'][0]), 3), best['mask'][1]
        rec['swap'], rec['swap_ref'] = round(max(0.0, best['ganz'][0]), 3), best['ganz'][1]
        rec['swap_part'], rec['swap_part_ref'] = round(max(0.0, best['part'][0]), 3), best['part'][1]
    rec['runs'], rec['runs_ref'] = runs.hits(c)
    rec['finding'] = rec['exact'] >= EXACT_MIN or rec['swap'] >= SWAP_MIN or rec['runs'] >= RUNS_MIN
    rec['look'] = not rec['finding'] and (rec['swap_part'] >= PART_MIN or rec['mask'] >= MASK_MIN)
    return rec


def own_files(module: str) -> list:
    out = subprocess.run(['git', 'ls-files', f'modules/{module}'], cwd=ROOT, capture_output=True, text=True, check=True).stdout
    return [ROOT / p for p in out.split() if p.endswith('.png')]


def scan(module: str, jar: Path, files: list | None = None) -> list:
    """files: PNG paths (default: the module's committed PNGs). One shape at a time (the stacks of one shape only)."""
    refs = references(jar, module)
    committed = {f.relative_to(ROOT).as_posix().split('/textures/', 1)[-1][:-4] for f in own_files(module)}
    for rel in committed:  # a build output that is still committed under the same path would only find itself
        refs.pop(f'derived {module}:{rel}', None)
    runs = RunIndex(refs)
    todo = {}
    for f in (own_files(module) if files is None else files):
        f = Path(f).resolve()
        c = codes(Image.open(f))
        todo.setdefault(c.shape, []).append((f.relative_to(ROOT).as_posix() if f.is_relative_to(ROOT) else str(f), c))
    rows = []
    for shape, items in todo.items():
        group = stack(refs, shape)
        rows += [score(c, rel, group, runs) for rel, c in items]
        del group
    return rows


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--scan', action='store_true')
    ap.add_argument('--module', default='simpleend')
    ap.add_argument('--jar')
    ap.add_argument('--json', type=Path)
    ap.add_argument('--files', nargs='*', help='score these PNGs instead of the committed ones')
    ap.add_argument('--all', action='store_true', help='print every file, not only findings')
    a = ap.parse_args()
    if not a.scan:
        print(__doc__)
        return
    import vanilla_derived
    jar = vanilla_derived.find_jar(a.jar)
    if jar is None:
        sys.exit('shelftex12: no 26.3 client jar')
    rows = scan(a.module, jar, a.files)
    if a.json:
        a.json.write_text(json.dumps(rows, indent=1), encoding='utf-8')
    found = [r for r in rows if r['finding']]
    look = [r for r in rows if r['look']]
    print(f'shelftex12 scan {a.module}: {len(rows)} PNGs, {len(found)} findings (Mojang pixels), {len(look)} to look at (part/mask)')
    for r in sorted(rows, key=lambda r: (-r['finding'], -r['look'], -r['runs'], -r['swap'], -r['exact'], -r['swap_part'])):
        if not (r['finding'] or r['look'] or a.all):
            continue
        tag = 'FINDING' if r['finding'] else ('LOOK   ' if r['look'] else '       ')
        print(f"{tag} exact {r['exact']:4d} swap {r['swap']:.2f} part {r['swap_part']:.2f} mask {r['mask']:.2f} runs {r['runs']:3d}  "
              f"{r['file']} | exact<-{r['exact_ref']} | swap<-{r['swap_ref']} | part<-{r['swap_part_ref']} | mask<-{r['mask_ref']}"
              f" | runs<-{r['runs_ref']}")


if __name__ == '__main__':
    main()
