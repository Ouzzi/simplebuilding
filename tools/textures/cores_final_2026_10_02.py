"""Building cores, owner pick 2026-10-02: round-10 no. 4 - the original core with the four diagonal corners one
pixel closer to the middle, everything else unchanged. Applied to all six cores; written into the 26.3 overlay."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposals_v10_2026_10_02 as v10  # noqa: E402
import cores_2026_10_02 as cores  # noqa: E402

OUT = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'item')
CORNERS_IN = dict(v10.PROPOSALS)['Ecken 1 px hinein']

if __name__ == '__main__':
    for name in cores.CORES:
        CORNERS_IN(v10.load(name)).save(os.path.join(OUT, f'{name}_core.png'))
    print('ok')
