"""N97: no checkerboard placeholders on Blocks & items and in the test centre editor.

Red with a list when an item/block picture of the wiki data or a picture of the editor (data/testcentre/index.json)
- points to no file and is no Vanilla-derived output the dev host writes (wiki/vanilla_textures.py --derived), or
- is a file with the placeholder pattern of tools/textures/vanilla_derived.placeholder_png (grey checker, b = a // 2).
It checks the tree the data was generated from, so ids that only arrive with a later bundle are not in it yet.
The dev host fills the Vanilla-derived files from the jars of the green gates (fill_from_dist, tested below);
live: /root/n74dev/n97_placeholders.cjs."""
import io
import json
import struct
import sys
import tempfile
import unittest
import zipfile
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WIKI = ROOT / 'wiki'
sys.path.insert(0, str(WIKI))
sys.path.insert(0, str(ROOT / 'tools' / 'textures'))
import textures as tx  # noqa: E402
import vanilla_derived as vd  # noqa: E402
import vanilla_textures as vt  # noqa: E402


def is_placeholder(path: Path) -> bool:
    from PIL import Image
    with Image.open(path) as im:
        colours = im.convert('RGBA').getcolors(4)
    if not colours or len(colours) != 2:
        return False
    a, b = colours[0][1], colours[1][1]

    def half(x, y):
        return x[3] == 255 and y[3] == 255 and all(y[j] == x[j] // 2 for j in range(3))
    return half(a, b) or half(b, a)


def pictures() -> dict[str, list[str]]:
    """wiki path -> where it is used (data file + id, or the editor)."""
    out: dict[str, list[str]] = {}
    for path in sorted((WIKI / 'data').glob('*.json')):
        try:
            data = json.loads(path.read_text(encoding='utf-8'))
        except ValueError:
            continue
        for section in ('items', 'blocks') if isinstance(data, dict) else ():
            for entry in data.get(section) or []:
                for key in ('icon', 'texture') if isinstance(entry, dict) else ():
                    if isinstance(entry.get(key), str):
                        out.setdefault(entry[key], []).append(f'{path.name} {entry.get("id")}')
    index = json.loads((WIKI / 'data' / 'testcentre' / 'index.json').read_text(encoding='utf-8'))
    for ident, pic in (index.get('textures') or {}).items():
        out.setdefault(pic, []).append(f'tzeditor {ident}')
    return out


def derived_files() -> set[str]:
    """Wiki paths the dev host writes from the Vanilla-derived set (git-ignored, absent in a plain checkout)."""
    index = tx.read_js(WIKI / tx.DATA_FILE) or {}
    out = {e['file'] for e in index.get('textures', []) if str(e.get('source', '')).startswith(tx.SB_LAYERS[-1] + '/')}
    return out | {f'assets/textures/render/{key}.png' for key in vd.WIKI_RENDERS}


class NoPlaceholderTests(unittest.TestCase):
    def test_every_picture_resolves(self):
        derived = derived_files()
        missing = sorted(f'{p} <- {", ".join(u[:3])}' for p, u in pictures().items()
                         if not (WIKI / p).is_file() and p not in derived)
        self.assertEqual(missing, [], f'{len(missing)} pictures point to no file:\n' + '\n'.join(missing[:100]))

    def test_no_placeholder_pixels(self):
        try:
            import PIL  # noqa: F401
        except ImportError:
            self.skipTest('no Pillow')
        derived = derived_files()  # built here without the client jar: placeholders by design, the host fills them
        bad = sorted(f'{p} <- {", ".join(u[:3])}' for p, u in pictures().items()
                     if p not in derived and (WIKI / p).is_file() and is_placeholder(WIKI / p))
        self.assertEqual(bad, [], f'{len(bad)} pictures are checkerboard placeholders:\n' + '\n'.join(bad[:100]))


def png(rgba: bytes, w: int = 2, h: int = 2) -> bytes:
    rows = b''.join(b'\x00' + rgba * w for _ in range(h))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xFFFFFFFF)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) + \
        chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')


class FillFromDistTests(unittest.TestCase):
    def test_placeholders_replaced_from_published_jars(self):
        module = next(iter(vd.MODULE_OUTPUTS))
        own = next(iter(vd.OUTPUTS))
        nested_rel = next(iter(vd.MODULE_OUTPUTS[module]))
        real_own, real_nested = png(b'\x10\x20\x30\xff'), png(b'\x40\x50\x60\xff')
        with tempfile.TemporaryDirectory() as tmp:
            tmp = Path(tmp)
            out = tmp / 'out'
            vd.write(out, None, placeholders=True)
            vd.write(out, None, placeholders=True, module=module)
            inner = io.BytesIO()
            with zipfile.ZipFile(inner, 'w') as z:
                z.writestr(f'assets/{module}/textures/{nested_rel}', real_nested)
            (tmp / 'files/b1/fabric/26.3').mkdir(parents=True)
            with zipfile.ZipFile(tmp / 'files/b1/fabric/26.3/super.jar', 'w') as z:
                z.writestr(f'assets/{vd.NS}/textures/{own}', real_own)
                z.writestr(f'META-INF/jars/{module}.jar', inner.getvalue())
            (tmp / 'index.json').write_text(json.dumps({'builds': [{'date': '2026-11-18', 'files': [
                {'minecraft': '26.3', 'loader': 'fabric', 'url': 'files/b1/fabric/26.3/super.jar'}]}]}), encoding='utf-8')
            filled, left = vt.fill_from_dist(out, tmp / 'index.json')
            self.assertEqual(filled, 2)
            self.assertEqual((out / f'assets/{vd.NS}/textures/{own}').read_bytes(), real_own)
            self.assertEqual((out / f'assets/{module}/textures/{nested_rel}').read_bytes(), real_nested)
            self.assertIn(f'assets/{vd.NS}/textures/{next(iter(list(vd.OUTPUTS)[1:]))}', left)
            self.assertEqual(vt.fill_from_dist(out, tmp / 'missing.json')[0], 0)  # no published jars: unchanged


if __name__ == '__main__':
    unittest.main()
