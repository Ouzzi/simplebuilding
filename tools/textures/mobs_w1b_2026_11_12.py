"""a-w1b (12.11.2026, PLAN40 simplemobs-7 and simplemobs-2): Makeshift Spawner cage and the Forbidden Fire item. Own pixels,
no Mojang pixels (Vanilla spawner / iron block / fire charge only as a comparison in the preview, read from the client jar at
preview time). Rounds after PRINZIPIEN A8c/A8d are kept here so the preview can show them; the last round is built in.

Makeshift Spawner (was red in texrate: one flat dark 61 px area, 32 % hard): now a cage like every spawner (the spinning mob of
the egg shows through, model cube_all_inner_faces), but makeshift: uneven bars of the iron ramp of the Spawner Fragment/Chunk
(tools/textures/spawn01_2026_11_01.py), a bent bar, a riveted patch plate, tones that drift every 2-3 px (A8d 2/4).
Forbidden Fire: a held flame (the "fire" of the old chainmail recipe) in the Cursed colours of Simple Lib: red rim, orange and
gold tongues, a dark ember core.

    python3 tools/textures/mobs_w1b_2026_11_12.py               # write the built-in textures (+ .layers.json)
    python3 tools/textures/mobs_w1b_2026_11_12.py --check       # exit 1 when a committed PNG/layer source differs
    python3 tools/textures/mobs_w1b_2026_11_12.py --rounds DIR  # every round's candidate as PNG (preview)
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/textures'

# iron ramp of spawn01 (b-tex03, own values): 0 dark rim .. 8 light
IRON = [(38, 38, 42), (62, 62, 66), (88, 88, 92), (116, 116, 120), (146, 146, 150), (174, 174, 176), (202, 202, 203),
        (226, 226, 226), (244, 244, 244)]
# a slightly warmer, rusty side tone for the patch plate (makeshift: scrap of another iron)
RUST = [(74, 58, 50), (104, 80, 64), (134, 104, 82)]


# ---------------------------------------------------------------- Makeshift Spawner
def _drift(seq: str, i: int) -> int:
    """Tone offset at position i along a bar: '+' +1, '-' -1, '0' 0, read cyclically (runs of 2-3 = A8d 4)."""
    c = seq[i % len(seq)]
    return 1 if c == '+' else -1 if c == '-' else 0


def _cage(r: int):
    """Tone grid (None = see-through) of round r and a list of drawing steps (name, set of (x, y))."""
    g = [[None] * 16 for _ in range(16)]
    steps = []

    def put(x, y, t, rust=False):
        if 0 <= x < 16 and 0 <= y < 16:
            g[y][x] = ('r', max(0, min(2, t))) if rust else max(0, min(8, t))

    def mark(name):
        steps.append((name, [row[:] for row in g]))

    if r == 1:
        # round 1: regular cage (Vanilla rhythm), flat tones per bar
        for i in range(16):
            put(i, 0, 6); put(i, 15, 3); put(0, i, 5); put(15, i, 3)
        for y in (5, 10):
            for x in range(1, 15):
                put(x, y, 5)
        for x in (5, 10):
            for y in range(1, 15):
                put(x, y, 4)
        mark('Gitter')
        return g, steps

    # rounds 2 and 3: uneven makeshift cage
    frame_top, frame_side = '00++0--0+00-', '0+0-00+0-+0'
    for i in range(16):
        put(i, 0, 6 + _drift(frame_top, i))                 # top edge lit
        put(i, 15, 3 + _drift(frame_side, i + 3))            # bottom edge in shadow
        put(0, i, 5 + _drift(frame_side, i))                 # left edge lit
        put(15, i, 3 + _drift(frame_top, i + 5))             # right edge in shadow
    mark('Rahmen')
    # horizontal bars: a 2 px bar at y 5-6 (light top row, darker under side), a thin bar at y 11
    for x in range(1, 15):
        put(x, 5, 6 + _drift('0+00-0+-00', x))
        put(x, 6, 3 + _drift('00-0+00', x))
        put(x, 11, 4 + _drift('0-00+0+0-', x))
    # vertical bars: a 2 px bar at x 4-5, a thin one at x 10 that is bent (x 9 below the thin bar)
    for y in range(1, 15):
        if y in (5, 6, 11):
            continue
        put(4, y, 5 + _drift('0+0-00', y))
        put(5, y, 3 + _drift('00-0+', y))
        put(10 if y < 12 else 9, y, 4 + _drift('+0-00+0', y))
    if r >= 3:
        put(10, 12, 3)                                       # the bend: one shadow pixel where the bar steps over
    mark('Stäbe')
    # crossings: bolts (light top-left, shadow right)
    for (x, y) in ((4, 5), (10, 5), (4, 11), (10, 11)):
        put(x, y, 7)
        put(x + 1, y, 4)
    mark('Bolzen')
    # riveted patch plate over the upper right cell (scrap iron, warmer), corner rivets of the frame
    if r == 2:
        for y in range(1, 5):
            for x in range(11, 15):
                put(x, y, 4)
        put(12, 2, 7); put(13, 3, 2)
    else:
        plate = ['.+0+', '+00-', '0-0-', '--0.']           # 4x4 patch, one corner bent off each way
        for dy, row in enumerate(plate):
            for dx, c in enumerate(row):
                if c != '.':
                    put(11 + dx, 1 + dy, 1 + _drift(c, 0), rust=True)
        put(12, 2, 7); put(13, 3, 1)                         # rivets: a lit head, its shadow
        put(12, 4, 6)
    mark('Flicken')
    for (x, y) in ((0, 0), (15, 0), (0, 15), (15, 15)):
        put(x, y, 7 if x == 0 and y == 0 else 2 if x == 15 and y == 15 else 4)
    if r >= 3:
        # a wire brace through the lower left cell (makeshift), 2 px steps
        for (x, y) in ((1, 14), (2, 13), (2, 12), (3, 12)):
            put(x, y, 3)
        put(1, 13, 5)
    mark('Nieten')
    return g, steps


# round 4: hand-placed, darker and denser (cells of 2-4 px like a spawner, not a fence), uneven columns 0|3|7-8|12->13|15 and
# rows 0|4-5|9|14-15, the right bar bent at the thin bar, a riveted iron plate in one cell, a wire through another.
# digits = IRON tone, '.' = see-through
CAGE_R4 = [
    '7556566556655652',
    '5..4...533434..2',
    '4..4...523624..2',
    '5..3...432213..1',
    '5556556765567652',
    '4223223322323221',
    '5..4...53...4..2',
    '4..4...42...4..2',
    '4..3...53...3..1',
    '6446445644546542',
    '4..4..353....4.2',
    '5..3..342....4.1',
    '4..4.3.43....3.2',
    '4..33..52....4.2',
    '4334333433343431',
    '3222112221122110',
]

# round 5: round 4 with soft steps (neighbours one ramp tone apart, A8b), only bolts and rivets stay hard accents
CAGE_R5 = [
    '6556556556556554',
    '5..5...544544..4',
    '4..4...544634..3',
    '5..4...433334..4',
    '5556555655556554',
    '4445444544445443',
    '5..5...54...5..4',
    '4..4...54...4..3',
    '5..4...43...4..4',
    '5446445654546544',
    '4..4..454....4.3',
    '5..5..443....5.4',
    '4..4.4.54....4.3',
    '4..44..43....4.3',
    '5445444544454543',
    '4334333433343433',
]

# round 6: round 4 (the 3D read stays) with the shadow side one tone lighter: under sides of the bars, the right px of the
# double bar and the bottom frame (hard steps only where a bolt or rivet sits)
CAGE_R6 = [
    '7556566556655652',
    '5..4...533434..2',
    '4..4...533624..2',
    '5..3...432213..1',
    '5556556765567652',
    '5334334433434331',
    '5..4...53...4..2',
    '4..4...43...4..2',
    '4..3...53...3..1',
    '6446445644546542',
    '4..4..353....4.2',
    '5..3..343....4.1',
    '4..4.3.43....3.2',
    '4..33..53....4.2',
    '4334334434344431',
    '4333223332233221',
]


def _rgba(grid) -> bytes:
    out = bytearray()
    for row in grid:
        for t in row:
            if t is None:
                out += bytes(4)
            elif isinstance(t, tuple):
                out += bytes(RUST[t[1]] + (255,))
            else:
                out += bytes(IRON[t] + (255,))
    return bytes(out)


def _hand(rows: list[str]):
    """Steps of a hand-placed cage: frame, bars, then the plate and the light accents (bolts, rivets) on top."""
    full = [[None if c == '.' else int(c) for c in row] for row in rows]
    frame = lambda x, y: x in (0, 15) or y in (0, 14, 15)
    plate = lambda x, y: 9 <= x <= 11 and 1 <= y <= 3
    accent = lambda x, y, t: t is not None and t >= 6
    steps, keep = [], []
    for name, pick in (('Rahmen', lambda x, y, t: frame(x, y) and not accent(x, y, t)),
                       ('Stäbe', lambda x, y, t: not plate(x, y) and not accent(x, y, t)),
                       ('Flicken', lambda x, y, t: not accent(x, y, t)),
                       ('Bolzen und Nieten', lambda x, y, t: True)):
        steps.append((name, [[t if t is not None and pick(x, y, t) else None for x, t in enumerate(row)] for y, row in enumerate(full)]))
    return full, steps


def spawner_doc(r: int) -> dict:
    _, steps = _hand({4: CAGE_R4, 5: CAGE_R5, 6: CAGE_R6}[r]) if r >= 4 else _cage(r)
    return layers.from_steps(16, 16, [(name, _rgba(grid)) for name, grid in steps])


# ---------------------------------------------------------------- Forbidden Fire
# palette: Cursed (Simple Lib: radiant red, orange and gold flames, black shadow) - own values
FIRE = {'K': (40, 10, 14), 'D': (96, 18, 20), 'R': (168, 30, 26), 'r': (214, 58, 30), 'O': (240, 116, 34), 'o': (250, 160, 48),
        'Y': (255, 206, 82), 'W': (255, 240, 176), 'E': (70, 22, 40)}
FIRE_ROUNDS = {
    # round 1: one big tongue, symmetric
    1: ['................', '................', '.......R........', '......RR........', '......RrR.......', '.....RrOR.......',
        '.....RrOrR......', '....RrOoOR......', '....RrOYoR......', '...RrOoYoOR.....', '...RrOYWYoR.....', '...RrOYWYOR.....',
        '....RrOoOR......', '.....DRRRD......', '................', '................'],
    # round 2: three tongues, lean to the right, dark core
    2: ['................', '..........R.....', '.......R..RR....', '......RR.RrR....', '......RrRRrR..R.', '.....RrORrOR.RR.',
        '..R..RrOrROR.RR.', '..RR.RrOoOOrRrR.', '..RrRrOoYoOrROR.', '..RrOrOYYoOOrOR.', '..ROoOYWYYoOOOR.', '..RroYWEWYoOrR..',
        '...ROoYEEYoOR...', '....RDOKKOD.....', '.....DDDDD......', '................'],
}
# round 3 (built in): tongues of 2-3 px, tone changes every 2-3 px along the rim (A8d), ember core instead of a black hole
FIRE_ROUNDS[3] = [
    '................',
    '..........R.....',
    '.......D..Rr....',
    '......Rr.DrR....',
    '......RrRRrO..R.',
    '.....DrORrOR.Rr.',
    '..R..RrOrROo.Rr.',
    '..Rr.ROOoOOrRrD.',
    '..DrRrOoYoOrROR.',
    '..RrOrOYYoOOrOR.',
    '..ROoOYWYYoOOoR.',
    '..DroYWEWYoOrD..',
    '...ROoYEEYoOR...',
    '....DRoEEoRD....',
    '.....DDKKDD.....',
    '................']
# round 4: no dark hole any more (it read as a cave), one main tongue to the top, side tongues of 2 px, rounded base
FIRE_ROUNDS[4] = [
    '................',
    '........D.......',
    '.......RR.......',
    '.......RrR...D..',
    '......RrOR..RR..',
    '..D...ROoR.RrR..',
    '..RR.RrOorRrOR..',
    '..RrRrOoYoOrOR..',
    '..RrOOoYYoOOoR..',
    '..RroOYWYYoOoR..',
    '..ROoYWWYYoOrR..',
    '..RroYYWYYoOrR..',
    '...RoOYYYYoOR...',
    '...DROoYYoOrD...',
    '.....DDRRDD.....',
    '................',
]
# round 5: round 4 with a small crimson ember inside the yellow core (the 'forbidden' fire), dark foot
FIRE_ROUNDS[5] = [
    '................',
    '........D.......',
    '.......RR.......',
    '.......RrR...D..',
    '......RrOR..RR..',
    '..D...ROoR.RrR..',
    '..RR.RrOorRrOR..',
    '..RrRrOoYoOrOR..',
    '..RrOOoYYoOOoR..',
    '..RroOYWYYoOoR..',
    '..ROoYWWYYoOrR..',
    '..RroYYEYYoOrR..',
    '...RoOYEEYoOR...',
    '...DROoEEoOrD...',
    '.....DDKKDD.....',
    '................',
]
# round 6 (built in): round 4, the flat yellow core broken by a deeper glow pocket of 2x2 and a 2 px step (A8d 3)
FIRE_ROUNDS[6] = [
    '................',
    '........D.......',
    '.......RR.......',
    '.......RrR...D..',
    '......RrOR..RR..',
    '..D...ROoR.RrR..',
    '..RR.RrOorRrOR..',
    '..RrRrOoYoOrOR..',
    '..RrOOoYYoOOoR..',
    '..RroOYWYooOoR..',
    '..ROoYWWYYoOrR..',
    '..RroYYooYoOrR..',
    '...RoOYooYoOR...',
    '...DROoYYoOrD...',
    '.....DDRRDD.....',
    '................',
]
FIRE_FINAL = 6
FIRE_GROUPS = [('Rand', 'KDRE'), ('Zungen', 'rOo'), ('Glut', 'YW')]


def fire_doc(r: int) -> dict:
    rows = FIRE_ROUNDS[r]
    steps, keep = [], ''
    for name, chars in FIRE_GROUPS:
        keep += chars
        steps.append((name, layers.grid_rgba(rows, FIRE, keep)))
    return layers.from_steps(16, 16, steps)


SPAWNER_FINAL = 6
BUILT_IN = {'block/makeshift_spawner': lambda: spawner_doc(SPAWNER_FINAL), 'item/forbidden_fire': lambda: fire_doc(FIRE_FINAL)}
ROUNDS = {'makeshift_spawner': (spawner_doc, (1, 2, 3, 4, 5, 6)), 'forbidden_fire': (fire_doc, tuple(sorted(FIRE_ROUNDS)))}


def main(argv: list[str]) -> int:
    if '--rounds' in argv:
        out = Path(argv[argv.index('--rounds') + 1])
        out.mkdir(parents=True, exist_ok=True)
        for name, (fn, rounds) in ROUNDS.items():
            for r in rounds:
                d = fn(r)
                (out / f'{name}_r{r}.png').write_bytes(layers.png_write(d['width'], d['height'], bytes(layers.compose(d))))
        print(f'rounds in {out}')
        return 0
    bad = []
    for name, make in BUILT_IN.items():
        d = make()
        png = TEX / f'{name}.png'
        src = layers.layers_path(png)
        text = layers.dumps(d)
        if '--check' in argv:
            if not src.is_file() or src.read_text(encoding='utf-8') != text or not png.is_file() \
                    or layers.png_read(png.read_bytes())[2] != layers.compose(d):
                bad.append(str(png.relative_to(ROOT)))
            continue
        png.parent.mkdir(parents=True, exist_ok=True)
        src.write_text(text, encoding='utf-8', newline='\n')
        png.write_bytes(layers.png_write(d['width'], d['height'], bytes(layers.compose(d))))
        print(f'wrote {png.relative_to(ROOT)} (+ layers)')
    if bad:
        print('stale:', *bad, sep='\n  ')
        return 1
    if '--check' in argv:
        print('mobs w1b textures current')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
