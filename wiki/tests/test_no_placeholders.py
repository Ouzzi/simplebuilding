"""N97: no checkerboard placeholders on Blocks & items and in the test centre editor.

Red with a list when an item/block picture of the wiki data or a picture of the editor (data/testcentre/index.json)
- points to no file and is no Vanilla-derived output the dev host writes (wiki/vanilla_textures.py --derived), or
- is a file with the placeholder pattern of tools/textures/vanilla_derived.placeholder_png (grey checker, b = a // 2).
It checks the tree the data was generated from, so ids that only arrive with a later bundle are not in it yet.
The dev host fills the Vanilla-derived files from the jars of the green gates (fill_from_dist, tested below);
live: /root/n74dev/n97_placeholders.cjs.

N113: a picture that is git-ignored (Vanilla-derived texture, sheet copy, 3D render) is missing on the hosts unless their
fill step wrote it. Every such picture the data names must be in wiki/data/local-only.js, so the page does not ask for it
there (the 18 renders of the Textures page, the 249 derived pictures on Production); the loader/page logic is tested by
n113_loader_core.cjs, the production fill (--published-only) below. Live: /root/n74dev/n113_wiki.cjs."""
import io
import json
import shutil
import struct
import subprocess
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
import local_only as lo  # noqa: E402


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


class LocalOnlyTests(unittest.TestCase):
    """N113: what the page must not ask the hosts for."""

    def test_list_is_current(self):
        built = lo.build(WIKI)
        if built is None:
            self.skipTest('no git')
        self.assertEqual(lo.read(WIKI), built, 'wiki/data/local-only.js is stale - python3 wiki/local_only.py (or generate.py)')

    def test_texture_page_renders_listed(self):
        """The case of the 18 broken pictures per texture page: textures.js names a 3D render (icon) git ignores."""
        built = lo.build(WIKI)
        if built is None:
            self.skipTest('no git')
        listed = lo.flat(lo.read(WIKI))
        index = tx.read_js(WIKI / tx.DATA_FILE) or {}
        pics = {e[k] for e in index.get('textures', []) for k in ('icon', 'file') if isinstance(e.get(k), str)}
        ignored = lo.ignored(WIKI, pics) or set()
        renders = {p for p in ignored if '/render/' in p}
        self.assertTrue(renders, 'textures.js names no git-ignored render any more - adjust this test')
        self.assertEqual(sorted(ignored - listed), [], 'git-ignored texture-page pictures missing in local-only.js')
        # every picture that is in no plain checkout and not written by the host fill is in the list
        missing = sorted(p for p in pics if not (WIKI / p).is_file() and p not in listed and p not in derived_files())
        self.assertEqual(missing, [], 'texture-page pictures that exist nowhere')

    def test_minecraft_and_tracked_not_listed(self):
        listed = lo.flat(lo.read(WIKI))
        self.assertFalse([p for p in listed if p.startswith(lo.SKIP_PREFIX)])
        tracked = tx.claimed_copies(WIKI, [])
        if tracked is not None:
            self.assertEqual(sorted(listed & {p for p in tracked if p.startswith('assets/textures/')}), [])

    @unittest.skipUnless(shutil.which('node'), 'node fehlt')
    def test_loader_and_texture_page(self):
        r = subprocess.run(['node', str(Path(__file__).with_name('n113_loader_core.cjs'))], capture_output=True, text=True, timeout=120)
        out = json.loads((r.stdout.strip().splitlines() or ['{}'])[-1])
        self.assertEqual(out.get('failures'), [], r.stderr[-2000:])
        self.assertGreater(out.get('checks', 0), 30)

    def test_published_only_fill(self):
        """Production host: only pictures from the published jars, no placeholders, and the present list names them."""
        real = png(b'\x10\x20\x30\xff')
        layer = tx.SB_LAYERS[-1]
        with tempfile.TemporaryDirectory() as tmp:
            tmp = Path(tmp)
            wiki = tmp / 'wiki'
            (wiki / 'data').mkdir(parents=True)
            entries = [{'id': 'simplebuilding:block/a', 'module': 'simplebuilding', 'file': 'assets/textures/simplebuilding/block/a.png',
                        'source': f'{layer}/assets/simplebuilding/textures/block/a.png', 'w': 2, 'h': 2},
                       {'id': 'simplebuilding:block/b', 'module': 'simplebuilding', 'file': 'assets/textures/simplebuilding/block/b.png',
                        'source': f'{layer}/assets/simplebuilding/textures/block/b.png', 'w': 2, 'h': 2},
                       {'id': 'simplebuilding:block/own', 'module': 'simplebuilding', 'file': 'assets/textures/simplebuilding/block/own.png',
                        'source': 'src/main/resources/assets/simplebuilding/textures/block/own.png', 'w': 2, 'h': 2}]
            (wiki / tx.DATA_FILE).write_text(tx.render_js({'schema': 1, 'modules': [], 'textures': entries}), encoding='utf-8')
            (tmp / 'files').mkdir()
            with zipfile.ZipFile(tmp / 'files/super.jar', 'w') as z:
                z.writestr('assets/simplebuilding/textures/block/a.png', real)
            (tmp / 'index.json').write_text(json.dumps({'builds': [{'date': '2026-11-20', 'files': [
                {'minecraft': '26.3', 'loader': 'fabric', 'url': 'files/super.jar'}]}]}), encoding='utf-8')
            written, left = vt.write_published(tmp, wiki, tmp / 'index.json')
            self.assertEqual((written, left), (1, 1))
            self.assertEqual((wiki / 'assets/textures/simplebuilding/block/a.png').read_bytes(), real)
            self.assertFalse((wiki / 'assets/textures/simplebuilding/block/b.png').exists(), 'no placeholder on production')
            present = json.loads((wiki / vt.PRESENT_FILE).read_text(encoding='utf-8'))
            self.assertEqual(present['files'], ['assets/textures/simplebuilding/block/a.png'])
            self.assertEqual(present['mode'], 'published')


if __name__ == '__main__':
    unittest.main()
