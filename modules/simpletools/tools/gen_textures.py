"""Block textures of the Death Vault (own pixel art, no Vanilla sheet): an obsidian shrine with bevelled edges, an arched
niche holding a soul flame on every side, a soul-sand offering bowl with a flame on top and plain obsidian tiles below.
python3 modules/simpletools/tools/gen_textures.py [--check] [--preview DIR]"""
import argparse
import sys
from pathlib import Path
from PIL import Image

MODULE = Path(__file__).resolve().parents[1]
OUT = MODULE / 'shared/resources/assets/simpletools/textures/block'

C = {
    'k': (0x0E, 0x09, 0x16), 'o': (0x1C, 0x13, 0x2A), 'p': (0x2A, 0x1E, 0x3E), 'q': (0x3C, 0x2D, 0x57), 'r': (0x56, 0x45, 0x78),
    's': (0x5A, 0x45, 0x36), 't': (0x3E, 0x2E, 0x24), 'u': (0x72, 0x5A, 0x46),
    'e': (0x15, 0x5C, 0x6C), 'd': (0x24, 0xA2, 0xB4), 'c': (0x5C, 0xE4, 0xEE), 'w': (0xC6, 0xFB, 0xFF),
}

TEXTURES = {
    'death_vault_side': [
        'rqqqqqqqqqqqqqqp',
        'qpppopppppppoppk',
        'qpoooooooooooopk',
        'qpopppqqqqppppok',
        'qpoppqkkkkqpppok',
        'qpopqkkkkkkqppok',
        'qpopqkkekkkqopok',
        'qpopqkkkekkqppok',
        'qpopqkkcdkkqppok',
        'qpopqkkcckkqpopk',
        'qpopqkcwcdkqppok',
        'qpopqkdccdkqppok',
        'qpopqeddddeqppok',
        'qpoqrrrrrrqoopok',
        'qppppppppopppppk',
        'pkkkkkkkkkkkkkkk',
    ],
    'death_vault_top': [
        'rqqqqqqqqqqqqqqp',
        'qppppppopppppppk',
        'qpoooooooooooopk',
        'qpoqqqqqqqqqqopk',
        'qpoqttssssttqopk',
        'qpoqtsuuuustqopk',
        'qpoqsuekkeusqopk',
        'qpoqsukcdkusqopk',
        'qpoqsukwckusqopk',
        'qpoqsuedceusqopk',
        'qpoqtsuuuustqopk',
        'qpoqttssssttqopk',
        'qpokkkkkkkkkkopk',
        'qpoooooooooooopk',
        'qppppopppppppppk',
        'pkkkkkkkkkkkkkkk',
    ],
    'death_vault_bottom': [
        'qppppppkqppppppk',
        'pooooopkpoooopok',
        'popooookpooooook',
        'poooooqkpooqoook',
        'pooooookpoooooko',
        'poqooookpooooook',
        'pooooookpooopook',
        'kkkkkkkkkkkkkkkk',
        'qppppppkqppppppk',
        'pooopookpoooooqk',
        'pooooookpoooooko',
        'pooooqokpopooook',
        'pooooookpoooooko',
        'popooookpooooook',
        'pooooookpooooqok',
        'kkkkkkkkkkkkkkkk',
    ],
}


def grid(rows):
    assert len(rows) == 16, len(rows)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row, len(row))
        for x, ch in enumerate(row):
            img.putpixel((x, y), C[ch] + (255,))
    return img


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--preview')
    args = ap.parse_args()
    stale = []
    for name, rows in TEXTURES.items():
        img = grid(rows)
        path = OUT / f'{name}.png'
        if args.check:
            if not path.is_file() or list(Image.open(path).convert('RGBA').getdata()) != list(img.getdata()):
                stale.append(name)
        else:
            OUT.mkdir(parents=True, exist_ok=True)
            img.save(path)
        if args.preview:
            Path(args.preview).mkdir(parents=True, exist_ok=True)
            img.resize((256, 256), Image.NEAREST).save(Path(args.preview) / f'{name}_16x.png')
    if stale:
        print('stale:', ', '.join(stale))
        sys.exit(1)
    print('death vault textures ok' if args.check else 'death vault textures written')


if __name__ == '__main__':
    main()
