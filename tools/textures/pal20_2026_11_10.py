"""PAL20 (b-pal20, owner answer PAL16 10.11. ~09:00, N20c): the chiseled textures that got an objection to PAL16.

Owner code PAL16 (word by word): polished_andesite ok, polished_diorite einwand (anderes Motiv), prismarine ok, red_nether_bricks ok,
ice ok, astralit_bricks einwand (alte Textur), nihilith_bricks einwand (altes Motiv), ender_quartz_bricks einwand (altes Motiv),
silver einwand (kreissymmetrisch oder 2-achsig symmetrisch), lead ok, zinc einwand (anderes Motiv), tin ok, brass ok; not named
(= accepted): polished_granite, dark_prismarine, nickel.

    accepted (PAL16 pixels, untouched)  -> pal16_2026_11_10 (simpleblocks(), carve_ice(), metal_texture())
    astralit/nihilith/ender_quartz bricks -> the pre-PAL15 look UNCHANGED: generate_textures.end_palette_chiseled(), which gives exactly
                                           the pixels of 63f787ad4 (hub/b-pal15~1); no technical change was needed (plan section 4)
    polished_diorite, zinc, silver      -> two new motifs each (CAND, owner picks A/B with code PAL20), built: PICK

Masks: '#' raised, 'o' hole in the raised part, '.' field; even widths, centred. Diorite and zinc use the PAL16 role rule (lit from the
top, exact left = right). Silver is mirror-symmetric on BOTH axes (owner): its motif has no top light, the raised parts carry a bright
rim on every side (like the four-fold Vanilla chiseled quartz), the grain is mirrored on both axes; the frame stays the metal family
frame (lit from the top like lead/tin/brass/nickel), so a wall of metals keeps one light direction.

    python3.12 tools/textures/pal20_2026_11_10.py --own              write the 9 own-art textures (End bricks + metals) + .layers.json
    python3.12 tools/textures/pal20_2026_11_10.py --check            committed PNG pixels = generator (and layer sources valid)
    python3.12 tools/textures/pal20_2026_11_10.py --rate             hardness, texrate light, mirror scores of every round/candidate
    python3.12 tools/textures/pal20_2026_11_10.py --preview OUT.html owner page ALT | NEU A | NEU B, picks, code PAL20
"""
from __future__ import annotations

import argparse
import base64
import io
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402
import pal16_2026_11_10 as p16  # noqa: E402

p15 = p16.p15
SB_TEX, ORE_TEX = p16.SB_TEX, p16.ORE_TEX
END_MATS = {'chiseled_astralit_bricks': 'astralit', 'chiseled_nihilith_bricks': 'nihilith',
            'chiseled_ender_quartz_bricks': 'ender_quartz'}
METALS = ('silver', 'lead', 'zinc', 'nickel', 'tin', 'brass')

# ---------------------------------------------------------------- candidate masks per round (verdicts: plan section 5)
MASKS = {
    'R1': {
        'amphora': ['..######..', '...####...', '.#.####.#.', '.#.####.#.', '.########.', '##########', '##oooooo##', '.########.',
                    '...####...', '..######..'],
        'lily': ['....##....', '...####...', '...####...', '.#..##..#.', '##..##..##', '.##.##.##.', '..######..', '##########',
                 '...####...', '..##..##..'],
        'bucket': ['..####..', '.#....#.', '########', '.######.', '.#oooo#.', '.######.', '..####..', '........'],
        'battery': ['...##...', '.######.', '.######.', '.##oo##.', '.#oooo#.', '.##oo##.', '.######.', '.######.'],
        'halo_moon': ['..####..', '.#....#.', '#..##..#', '#.####.#', '#.####.#', '#..##..#', '.#....#.', '..####..'],
        'triple_moon': ['.#....#.', '#......#', '#..##..#', '#.####.#', '#.####.#', '#..##..#', '#......#', '.#....#.'],
    },
}
MASKS['R2'] = {**MASKS['R1'],
    # amphora: R1 handles read as ears, body squat -> handles bend out of the neck and close onto the shoulder, band lower, pointed foot
    'amphora': ['..######..', '...####...', '.##.##.##.', '.#.####.#.', '.#.####.#.', '.########.', '##oooooo##', '##########',
                '.########.', '...####...'],
    # lily: R1 read as a frog/bug -> classic fleur-de-lis: tall middle petal, side petals curling out and down, band, three lower lobes
    'lily': ['....##....', '...####...', '...####...', '.#.####.#.', '##..##..##', '#.#.##.#.#', '..######..', '##########',
             '..#.##.#..', '.##.##.##.'],
    # bucket: R1 handle loose, body read as a barrel -> handle arch closes onto the rim, two ribs, tapered bottom
    'bucket': ['.######.', '.#....#.', '########', '.######.', '.#oooo#.', '.######.', '.#oooo#.', '..####..'],
    # battery: R1 read as a jar/lantern -> the battery-level icon: terminal, case, upper half empty (hole), lower half charged
    'battery': ['...##...', '.######.', '.#oooo#.', '.#oooo#.', '.######.', '.######.', '.######.', '.######.'],
}
MASKS['R3'] = {**MASKS['R2'],
    # amphora: R2 band hole dominated (read as a bowl/face) -> solid bellied body, the relief carries it
    'amphora': ['..######..', '...####...', '.##.##.##.', '.#.####.#.', '.########.', '##########', '##########', '.########.',
                '..######..', '...####...'],
    # diorite B: the lily stayed a bug at 10 px (1-px petals) -> crown (three points, jewel band): bold, reads at 1x
    'crown': ['#...##...#', '#...##...#', '##.####.##', '##########', '##########', '##o####o##', '##########'],
    # bucket: R2 read as a stacked barrel -> handle arch free above the rim, body clearly tapered
    'bucket': ['..####..', '.#....#.', '########', '.######.', '.#oooo#.', '.######.', '..####..', '..####..'],
    # zinc B: the battery read as a bag -> Zinkblume, the crystal flower on galvanised sheet (vertical + four diagonal arms)
    'spangle': ['...##...', '#..##..#', '.#.##.#.', '..####..', '..####..', '.#.##.#.', '#..##..#', '...##...'],
}
MASKS['R4'] = {**MASKS['R3'],
    # bucket: R3 still a barrel with a lid (arch closed by the rib band) -> handle runs down to the rim ends, no rib, stronger taper
    'bucket': ['..####..', '.#....#.', '#......#', '########', '.######.', '.######.', '..####..', '..####..'],
}
ROUND = 'R4'
CAND = {'chiseled_polished_diorite': ('amphora', 'crown'), 'chiseled_zinc': ('bucket', 'spangle'),
        'chiseled_silver': ('halo_moon', 'triple_moon')}
# candidates per round (later rounds replaced a motif that did not read)
_C12 = {**CAND, 'chiseled_polished_diorite': ('amphora', 'lily'), 'chiseled_zinc': ('bucket', 'battery')}
ROUND_CAND = {'R1': _C12, 'R2': _C12}
PICK = {'chiseled_polished_diorite': 'A', 'chiseled_zinc': 'A', 'chiseled_silver': 'A'}   # built until the owner answers PAL20
DE = {'amphora': 'Amphore (Vase mit Henkeln)', 'lily': 'Lilie (Fleur-de-Lis)', 'crown': 'Krone (drei Zacken, Steinband)',
      'spangle': 'Zinkblume (Kristallmuster verzinkter Bleche)', 'bucket': 'Verzinkter Eimer mit Bügel und Rippen',
      'battery': 'Zink-Batterie (Pol oben, Plus auf dem Etikett)', 'halo_moon': 'Vollmond im Hof (kreissymmetrisch)',
      'triple_moon': 'Dreifachmond (zunehmend · voll · abnehmend, 2-achsig)'}

# silver tones (METALS14 ramp index, 0 = brightest): rim = bright edge on every side, face, field, hole
SILVER_TONES = {'R1': {'H': 2, 'M': 3, 'K': 7}, 'R2': {'H': 2, 'M': 3, 'K': 7, 'C': 6}, 'R3': {'H': 2, 'M': 3, 'K': 7, 'C': 6}, 'R4': {'H': 2, 'M': 3, 'K': 7}}   # R4: halo C dropped (hart 15 %, little gain)


def _key(motif: str, rnd: str) -> str:
    """Register the mask in pal16's motif table so its role rule (lit from the top) can be reused unchanged."""
    k = f'pal20_{motif}_{rnd}'
    p16.MOTIFS.setdefault(k, MASKS[rnd][motif])
    return k


def motif(name: str, choice: str | None = None) -> str:
    return CAND[name][0 if (choice or PICK[name]) == 'A' else 1]


# ---------------------------------------------------------------- Simple Blocks (Vanilla-derived, built from the jar)

def diorite(jar, mot: str, rnd: str = ROUND) -> np.ndarray:
    return p16.carve_vanilla(p15.vanilla(jar, 'polished_diorite'), _key(mot, rnd), 'polished_diorite', 'R5')


def simpleblocks(jar, rnd: str = ROUND) -> dict:
    out = p16.simpleblocks(jar)   # accepted PAL16 motifs (+ prismarine PAL15) unchanged
    out['block/chiseled_polished_diorite.png'] = diorite(jar, motif('chiseled_polished_diorite'), rnd)
    return out


# ---------------------------------------------------------------- metals (own art)

def silver_grid(mot: str, rnd: str = ROUND) -> list[str]:
    """Four-fold symmetric roles: H raised rim (any side next to the field), M raised face, K hole, C field ring around the motif."""
    m = MASKS[rnd][mot]
    frame = p16.roles(None, 3, 'R5')
    cell = [['.'] * 16 for _ in range(16)]
    for y in range(8):
        for x in range(8):
            cell[4 + y][4 + x] = m[y][x]
    up = lambda y, x: cell[y][x] == '#'   # noqa: E731
    out = []
    for y in range(16):
        row = ''
        for x in range(16):
            if min(x, y, 15 - x, 15 - y) <= 2:
                row += frame[y][x]
            elif cell[y][x] == '#':
                row += 'M' if all(up(y + dy, x + dx) for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1))) else 'H'
            elif cell[y][x] == 'o':
                row += 'K'
            elif 'C' in SILVER_TONES[rnd] and any(up(y + dy, x + dx) for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                row += 'C'
            else:
                row += '.'
        out.append(row)
    return out


def grain4(rate: float, seed: int) -> np.ndarray:
    q = np.random.default_rng(seed).random((8, 8)) < rate
    half = np.concatenate([q, q[:, ::-1]], axis=1)
    return np.concatenate([half, half[::-1]], axis=0)


def metal_from_grid(metal: str, grid: list[str], tones: dict, gm: np.ndarray) -> Image.Image:
    """pal16.metal_texture (round R5) with a given role grid / motif tones / grain mask (same frame, ramp, grain steps)."""
    import metals14_2026_11_09 as m14
    import ores_2026_10_30 as ores
    ramp = ores.RAMPS[m14.METALS[metal]]
    lo, hi = m14.SPANS[m14.ROUND][metal]
    colour = lambda t: ores.ramp_at(ramp, (lo + (hi - lo) * t / 9.0) / 9.0) + (255,)   # noqa: E731
    p = p16.ROUNDS['R5']
    frame = p16.metal_frame()
    gd, gk = p['grain'], p['metal_grain']
    def g(t, ch, y, x):
        return None if t is None else t - (gd.get(ch, 0) * gk if gm[y, x] else 0)
    def fr(y, x):
        return g(frame[y][x], grid[y][x], y, x) if min(x, y, 15 - x, 15 - y) <= 2 else None
    im = Image.new('RGBA', (16, 16))
    for lname, fn in (('Grundfläche', lambda y, x: g(p['metal']['.'], '.', y, x)), ('Kanten', fr),
                      ('Motiv', lambda y, x: g(tones[grid[y][x]], grid[y][x], y, x) if grid[y][x] in tones and grid[y][x] != '.' else None)):
        for y in range(16):
            for x in range(16):
                t = fn(y, x)
                if t is not None:
                    im.putpixel((x, y), colour(t))
        layers.snap(im, lname)
    return im


def metal_candidate(metal: str, mot: str, rnd: str = ROUND) -> Image.Image:
    seed = p16._seed(metal, 'R5')
    if metal == 'silver':
        return metal_from_grid(metal, silver_grid(mot, rnd), SILVER_TONES[rnd], grain4(p16.ROUNDS['R5']['grain_rate'], seed))
    grid = p16.roles(_key(mot, rnd), 3, 'R5')
    return metal_from_grid(metal, grid, p16.ROUNDS['R5']['metal'], p16.grain(p16.ROUNDS['R5']['grain_rate'], seed))


def metal_texture(metal: str, rnd: str = ROUND) -> Image.Image:
    name = f'chiseled_{metal}'
    return metal_candidate(metal, motif(name), rnd) if name in CAND else p16.metal_texture(metal)


def end_texture(name: str) -> Image.Image:
    import generate_textures as gt   # the pre-PAL15 generator: exactly the pixels of 63f787ad4
    return gt.end_palette_chiseled(END_MATS[name])


def own() -> dict:
    out = {SB_TEX / f'{n}.png': end_texture(n) for n in END_MATS}
    out.update({ORE_TEX / f'chiseled_{m}.png': metal_texture(m) for m in METALS})
    return out


def check() -> int:
    bad = []
    for path, im in own().items():
        cur = np.array(Image.open(path).convert('RGBA'))
        if not np.array_equal(cur, np.array(im.convert('RGBA'))):
            bad.append(f'{path.relative_to(p16.ROOT)}: pixels differ from the generator')
        lj = path.with_suffix('.layers.json')
        bad += layers.check_file(lj) if lj.exists() else [f'{lj.relative_to(p16.ROOT)}: missing']
    for name, mats in END_MATS.items():   # owner: the pre-PAL15 texture exactly
        if np.array(Image.open(SB_TEX / f'{name}.png').convert('RGBA')).tobytes() != _old(name).tobytes():
            bad.append(f'{name}: not the pre-PAL15 pixels (63f787ad4)')
    print('\n'.join(bad) if bad else f'pal20 --check OK: {len(END_MATS) + len(METALS)} own textures = generator, End bricks = pre-PAL15')
    return 1 if bad else 0


def _old(name: str) -> np.ndarray:
    return np.array(p16._git_png(f'src/main/resources/assets/simplebuilding/textures/block/{name}.png'))


# ---------------------------------------------------------------- measurement / preview

def mirror(im, lo: int = 3, hi: int = 13) -> tuple[float, float]:
    f = p16.lum(np.array(im.convert('RGBA')))[lo:hi, lo:hi]
    r = max(1.0, f.max() - f.min())
    return 1 - float(np.abs(f - f[:, ::-1]).mean()) / r, 1 - float(np.abs(f - f[::-1]).mean()) / r


def candidates(jar, rnd: str = ROUND) -> dict:
    """(block, 'A'|'B') -> image."""
    out = {}
    for name, mots in ROUND_CAND.get(rnd, CAND).items():
        for ch, mot in zip('AB', mots):
            out[(name, ch)] = (Image.fromarray(diorite(jar, mot, rnd), 'RGBA') if name == 'chiseled_polished_diorite'
                               else metal_candidate(name[len('chiseled_'):], mot, rnd))
    return out


def rate_all(jar) -> list[tuple]:
    import texrate
    rows = []
    for rnd in MASKS:
        for (name, ch), im in candidates(jar, rnd).items():
            m = texrate.metrics(im)
            rows.append((rnd, name, ch, m['hard'], texrate.rate(m)[0], *mirror(im)))
    return rows


def _uri(im: Image.Image, k: int) -> str:
    buf = io.BytesIO()
    im.convert('RGBA').resize((im.width * k, im.height * k), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def _tile(im: Image.Image) -> Image.Image:
    t = Image.new('RGBA', (48, 48))
    for ty in range(3):
        for tx in range(3):
            t.paste(im.convert('RGBA'), (tx * 16, ty * 16))
    return t


ORDER = ['chiseled_polished_diorite', 'chiseled_zinc', 'chiseled_silver', 'chiseled_astralit_bricks', 'chiseled_nihilith_bricks',
         'chiseled_ender_quartz_bricks']
OWNER = {'polished_diorite': 'Einwand: anderes Motiv', 'zinc': 'Einwand: anderes Motiv',
         'silver': 'Einwand: kreissymmetrisch oder 2-achsig symmetrisch', 'astralit_bricks': 'Einwand: alte Textur',
         'nihilith_bricks': 'Einwand: altes Motiv', 'ender_quartz_bricks': 'Einwand: altes Motiv'}
VANILLA = {'chiseled_polished_diorite': 'chiseled_quartz_block', 'chiseled_zinc': 'chiseled_copper', 'chiseled_silver': 'chiseled_quartz_block'}
WHY = {
    'chiseled_polished_diorite': 'Zwei neue Motive (kein Fossil, kein Ammonit): A Amphore – heller polierter Stein wie Marmor, klassische Steinmetzarbeit; '
                                 'B Lilie – klassisches Wappen-Ornament. Beide links = rechts, Licht von oben wie die abgenommenen Blöcke.',
    'chiseled_zinc': 'Zwei neue Motive (kein Blitz, kein Schild): A verzinkter Eimer (Zink schützt Eisen vor Rost); B Zink-Batterie '
                     '(Zink-Kohle-Zelle). Links = rechts, Licht von oben wie Blei/Zinn/Messing/Nickel.',
    'chiseled_silver': 'Zwei Mond-Motive (Silber = Mond), Motivfeld pixelgenau links = rechts UND oben = unten: A Vollmond im Hof '
                       '(kreissymmetrisch), B Dreifachmond. Erhabene Teile mit hellem Rand ringsum statt Licht von oben (sonst wäre oben ≠ unten); '
                       'der Rahmen bleibt der Metall-Rahmen der Familie.',
    'chiseled_astralit_bricks': 'Alte Textur (vor PAL15) unverändert zurück – Pixel identisch mit Commit 63f787ad4.',
    'chiseled_nihilith_bricks': 'Altes Motiv (vor PAL15) zurück – Pixel identisch; keine technische Anpassung nötig.',
    'chiseled_ender_quartz_bricks': 'Altes Motiv (vor PAL15) zurück – Pixel identisch; keine technische Anpassung nötig.',
}
ROUND_NOTES = {}   # filled from the plan (section 5) by preview()


def preview(jar: Path, path: Path, notes: dict) -> None:
    rows = rate_all(jar)
    rate = {(r, n, c): (h, li, lr, tb) for r, n, c, h, li, lr, tb in rows}
    cand = {r: candidates(jar, r) for r in MASKS}
    pal16 = p16.set_new(jar)
    secs = []
    for name in ORDER:
        short = name[len('chiseled_'):]
        figs = []
        if name in CAND:
            figs.append(f'<figure><img class=b src="{_uri(pal16[name], 12)}" alt=""><figcaption>ALT (PAL16, Einwand)</figcaption></figure>')
            for ch, mot in zip('AB', CAND[name]):
                im = cand[ROUND][(name, ch)]
                h, li, lr, tb = rate[(ROUND, name, ch)]
                figs.append(f'<figure><img class=b src="{_uri(im, 12)}" alt=""><figcaption><b>{ch}</b>: {DE[mot]}<br>hart {h:.0f} % · {li} · '
                            f'Spiegel L/R {lr:.2f} O/U {tb:.2f}</figcaption></figure>'
                            f'<figure><img class=t src="{_uri(_tile(im), 3)}" alt=""><figcaption>{ch} 3×3 · 1×: '
                            f'<img class=one src="{_uri(im, 1)}" alt=""></figcaption></figure>')
            van = Image.fromarray(p15.vanilla(jar, VANILLA[name]), 'RGBA')
            figs.append(f'<figure><img class=v src="{_uri(van, 4)}" alt=""><figcaption>Vanilla<br>{VANILLA[name]}</figcaption></figure>')
            rounds = ''.join(f'<figure><img class=s src="{_uri(cand[r][(name, ch)], 5)}" alt=""><figcaption>{r} {ch} · hart '
                             f'{rate[(r, name, ch)][0]:.0f} %</figcaption></figure>' for r in MASKS for ch in 'AB')
            picks = '<button onclick="pick(this,\'A\')">A</button><button onclick="pick(this,\'B\')">B</button>'
            extra = f'<details><summary>Runden</summary><div class=row>{rounds}</div></details>'
        else:
            new = end_texture(name)
            figs.append(f'<figure><img class=b src="{_uri(pal16[name], 12)}" alt=""><figcaption>ALT (PAL16, Einwand)</figcaption></figure>')
            figs.append(f'<figure><img class=b src="{_uri(new, 12)}" alt=""><figcaption>NEU = Fassung vor PAL15</figcaption></figure>'
                        f'<figure><img class=t src="{_uri(_tile(new), 3)}" alt=""><figcaption>3×3 · 1×: <img class=one src="{_uri(new, 1)}" alt="">'
                        f'</figcaption></figure>')
            picks = '<button onclick="pick(this,\'ok\')">ok</button>'
            extra = ''
        secs.append(f'''<section data-id="{short}"><h3>{short.replace('_', ' ')} <small>– Besitzer PAL16: {OWNER[short]}</small></h3>
<p class=why>{WHY[name]}</p><div class=row>{''.join(figs)}</div>{extra}
<div class=pick>{picks}<button onclick="pick(this,'einwand')">Einwand</button><input placeholder="Anmerkung (bei Einwand)" oninput="upd()"></div></section>''')
    nl = ''.join(f'<li><b>{r}</b>: {t}</li>' for r, t in notes.items())
    html = f"""<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>b-pal20: Meißel-Motive (PAL20)</title>
<style>:root{{--bg:#f2efe6;--fg:#1c1e21;--card:#fff;--ac:#2f6b3a;--mut:#555}}@media(prefers-color-scheme:dark){{:root{{--bg:#1d1f22;--fg:#e8e6df;--card:#2a2d31;--ac:#7fc08a;--mut:#b8b5ad}}}}
body{{font:16px/1.5 system-ui,sans-serif;margin:0;padding:1rem;background:var(--bg);color:var(--fg);max-width:1200px}}h1{{font-size:1.4rem}}
section{{background:var(--card);padding:.8rem 1rem;border-radius:8px;margin-bottom:1rem}}h3{{margin:.2rem 0;font-size:1.05rem}}small{{color:var(--mut);font-weight:normal}}
.row{{display:flex;flex-wrap:wrap;gap:.6rem;align-items:flex-end}}figure{{margin:0;text-align:center}}img{{image-rendering:pixelated;display:block}}
img.b{{width:192px;height:192px}}img.t{{width:144px;height:144px}}img.s{{width:80px;height:80px}}img.v{{width:64px;height:64px;margin:auto}}
img.one{{width:16px;height:16px;display:inline-block;vertical-align:middle}}
figcaption{{font-size:.75rem;max-width:200px;color:var(--mut)}}.why{{margin:.2rem 0 .5rem;font-size:.9rem}}
button{{margin:.4rem .4rem 0 0;padding:.5rem .9rem;font-size:1rem;border:2px solid var(--ac);border-radius:6px;background:transparent;color:var(--fg);cursor:pointer}}
button.on{{background:var(--ac);color:var(--bg)}}input{{padding:.45rem;font-size:.95rem;min-width:260px;border-radius:6px;border:1px solid var(--mut);background:var(--card);color:var(--fg)}}
#code{{position:sticky;bottom:0;background:var(--card);padding:.7rem 1rem;border-radius:8px;border:2px solid var(--ac);word-break:break-all}}</style>
<h1>b-pal20: Meißel-Motive nach PAL16 (PAL20)</h1>
<p>Nur die Blöcke mit Einwand aus PAL16. Abgenommen und nicht angefasst: polierter Andesit, Prismarin, rote Netherziegel, Eis, Blei, Zinn, Messing,
polierter Granit, dunkler Prismarin, Nickel. Diorit, Zink, Silber: je zwei neue Motive A/B – bitte wählen (eingebaut ist vorläufig A).
Astralit/Nihilith/Enderquarz: die Fassung vor PAL15 ist pixelgenau zurück. „hart“ = harte Übergänge (A8b ~7 %), Ampel = texrate (A8d),
„Spiegel“ = Spiegelwert des Motivfelds (1,00 = exakt; Vanilla gemeißelter Quarz L/R 0,85 O/U 0,86, Kupfer 0,95/0,82).</p>
<h2>Runden</h2><ul>{nl}</ul>
{''.join(secs)}
<p id=code>Antwortcode: <code id=c>PAL20</code> <button onclick="cp()">Kopieren</button></p>
<script>function pick(b,v){{var s=b.closest('section');s.querySelectorAll('.pick button').forEach(x=>x.classList.remove('on'));b.classList.add('on');s.dataset.v=v;upd()}}
function upd(){{var p=[];document.querySelectorAll('section').forEach(s=>{{if(!s.dataset.v)return;var t=s.querySelector('input').value.trim();p.push(s.dataset.id+'='+s.dataset.v+(t?'('+t+')':''))}});
document.getElementById('c').textContent='PAL20:'+(p.length?' '+p.join(' '):'')}}
function cp(){{var c=document.getElementById('c').textContent;if(navigator.clipboard)navigator.clipboard.writeText(c);else{{var t=document.createElement('textarea');t.value=c;document.body.appendChild(t);t.select();document.execCommand('copy');t.remove()}}}}</script></html>"""
    path.write_text(html, encoding='utf-8')
    print(f'wrote {path}')


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('--own', action='store_true')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--rate', action='store_true')
    ap.add_argument('--preview')
    ap.add_argument('--jar')
    a = ap.parse_args()
    rc = 0
    if a.own:
        for p, im in own().items():
            layers.save(im, p)
        print(f'pal20: {len(END_MATS) + len(METALS)} own textures + layers written')
    if a.check:
        rc = check()
    if a.rate:
        for rnd, name, ch, h, li, lr, tb in rate_all(p15.find_jar(a.jar)):
            print(f'{rnd} {name:28s} {ch} hart {h:5.1f} %  {li:6s} L/R {lr:.2f} O/U {tb:.2f}')
    if a.preview:
        preview(p15.find_jar(a.jar), Path(a.preview), _plan_notes())
    return rc


def _plan_notes() -> dict:
    """Round verdicts from the plan table (single source: docs/ai/PLAN-B-PAL20-2026-11-10.md section 5)."""
    plan = (p16.ROOT / 'docs/ai/PLAN-B-PAL20-2026-11-10.md').read_text(encoding='utf-8')
    out = {}
    for line in plan.splitlines():
        cells = [c.strip() for c in line.strip().strip('|').split('|')]
        if len(cells) >= 3 and cells[0].split(' ')[0] in MASKS:
            out[cells[0]] = f'{cells[1]} → {cells[2]}'
    return out


if __name__ == '__main__':
    sys.exit(main())
